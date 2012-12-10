/*
 * Copyright 1997-2012 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.idv.control;


import ucar.nc2.units.SimpleUnit;

import ucar.unidata.collab.Sharable;

import ucar.unidata.data.*;


import ucar.unidata.data.gis.MapMaker;
import ucar.unidata.data.grid.DerivedGridFactory;
import ucar.unidata.data.grid.GeoGridDataSource;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.control.drawing.*;
import ucar.unidata.ui.FineLineBorder;

import ucar.unidata.ui.colortable.ColorTableDefaults;
import ucar.unidata.util.*;

import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.unidata.xml.XmlUtil;


import ucar.visad.Util;
import ucar.visad.display.*;


import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;
import visad.georef.LatLonPoint;

import visad.util.DataUtility;


import java.awt.*;
import java.awt.event.*;


import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;



/**
 * A MetApps Display Control for drawing lines on a navigated
 * display.
 *
 * @author MetApps development team
 * @version $Revision: 1.145 $
 */

public class GridTrajectoryControl extends DrawingControl {


    /** Controls the disabled state */
    protected JCheckBox enabledCbx;

    /** The title we get from the xml */
    private String editorTitle = null;

    /** Top level displayable */
    CompositeDisplayable displayHolder0;

    /** If we get our glyphs from a datachoice this is set to true */
    private boolean displayOnly = true;

    /** command */
    public static final DrawingCommand CMD_REMOVE =
        new DrawingCommand("Remove graphic", "remove all shape graphics",
                           "/auxdata/ui/icons/Reshape16.gif");

    /** _more_ */
    public static final String CMD_SETLEVELS = "cmd.setlevels";

    /** _more_ */
    DataChoice dataChoice;


    /** _more_ */
    private JButton levelUpBtn;

    /** _more_ */
    private JButton levelDownBtn;

    /** _more_ */
    private JComboBox levelBox;

    /** _more_ */
    private JLabel levelLabel;

    /** _more_ */
    protected Object currentLevel;

    /** _more_ */
    protected Object[] currentLevels;

    /** _more_ */
    private boolean levelEnabled = false;

    /** _more_ */
    private Unit zunit;

    /** _more_ */
    private Unit newZunit = CommonUnit.meter;

    /** _more_ */
    private Range lastRange;

    /** _more_ */
    private static final Data DUMMY_DATA = new Real(0);


    /** _more_ */
    CoordinateSystem pressToHeightCS;

    /** streamlines button */
    private JRadioButton pointsBtn;

    /** vector/barb button */
    private JRadioButton rectangleBtn;

    /** flag for streamlines */
    boolean isPoints = true;

    /** _more_ */
    private JButton createTrjBtn;

    /** _more_ */
    JPanel controlPane;

    /** _more_ */
    private MyTrackControl gridTrackControl;

    /** _more_ */
    FieldImpl u;

    /** _more_ */
    FieldImpl v;

    /** _more_ */
    FieldImpl pw;

    /** _more_ */
    FieldImpl s;

    /**
     * Create a new Drawing Control; set attributes.
     */
    public GridTrajectoryControl() {
        setCoordType(DrawingGlyph.COORD_LATLON);
        setLineWidth(2);
        setAttributeFlags(FLAG_DISPLAYUNIT);
    }


    /**
     * Class MyRadarSweepControl _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class MyTrackControl extends TrackControl {

        /** _more_ */
        private DisplayableData timesHolder = null;

        /** _more_ */
        private StationModelDisplayable indicator = null;

        /** _more_ */
        private double timeDeclutterMinutes = 1;

        /** _more_ */
        private boolean useTrackTimes = true;

        /** _more_ */
        private boolean timeDeclutterEnabled = true;

        /** _more_ */
        private EarthLocationLite lastIndicatorPosition;

        /** _more_ */
        private float markerScale = 1.0f;

        /** _more_ */
        private Range lastRange;

        /**
         * _more_
         */
        public MyTrackControl() {
            setAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL
                              | FLAG_DISPLAYUNIT | FLAG_TIMERANGE
                              | FLAG_SELECTRANGE);
        }

        /**
         * _more_
         */
        protected void addToControlContext() {}

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getShowInLegend() {
            return false;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getUseTrackTimes() {
            return useTrackTimes;
        }

        /**
         * _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        private void setTrackTimes() throws VisADException, RemoteException {
            if ((trackDisplay == null)) {
                return;
            }
            Data d = trackDisplay.getData();
            if (d.equals(DUMMY_DATA)) {
                return;
            }
            if ( !getUseTrackTimes()) {
                timesHolder.setData(DUMMY_DATA);
                return;
            }
            FlatField f;
            try {
                f = (FlatField) ((FieldImpl) d).getSample(0);
            } catch (ClassCastException e) {
                f = (FlatField) d;
            }

            //System.out.println(f.getType());
            double[][] samples  = f.getValues(false);
            int        numTimes = samples[1].length;
            if ( !getTimeDeclutterEnabled()) {
                if ( !getAskedUserToDeclutterTime() && (numTimes > 1000)) {
                    int success =
                        GuiUtils
                            .showYesNoCancelDialog(getWindow(), "<html>There are "
                                + numTimes
                                + " time steps in the data.<br>Do you want to show them all?</html>", "Time Declutter", GuiUtils
                                    .CMD_NO);
                    if (success == JOptionPane.CANCEL_OPTION) {
                        return;
                    } else {
                        setAskedUserToDeclutterTime(true);
                        setTimeDeclutterEnabled(success
                                == JOptionPane.NO_OPTION);
                    }
                }
            }

            double[] times = samples[1];
            if ( !Util.isStrictlySorted(times)) {
                int[] indexes = Util.strictlySortedIndexes(times, true);
                times = Util.take(times, indexes);

            }
            if (getTimeDeclutterEnabled()) {
                LogUtil.message("Track display: subsetting times");
                Trace.call1("declutterTime");
                times = declutterTime(times);
                Trace.call2("declutterTime");
                LogUtil.message("");
            }
            Unit[] units = f.getDefaultRangeUnits();
            Gridded1DDoubleSet timeSet =
                new Gridded1DDoubleSet(RealTupleType.Time1DTuple,
                                       new double[][] {
                times
            }, times.length, (CoordinateSystem) null,
               new Unit[] { units[1] }, (ErrorEstimate[]) null,
               false /*don't copy*/);
            if (timeSet != null) {
                timesHolder.setData(timeSet);
            }
        }

        /**
         * _more_
         *
         * @param times _more_
         *
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        private double[] declutterTime(double[] times)
                throws VisADException, RemoteException {
            int numTimes = times.length;
            int seconds  = (int) (timeDeclutterMinutes * 60);
            if (seconds == 0) {
                seconds = 1;
            }
            double[]  tmpTimes = new double[times.length];
            int       numFound = 0;
            Hashtable seenTime = new Hashtable();
            for (int timeIdx = 0; timeIdx < numTimes; timeIdx++) {
                Integer timeKey = new Integer((int) (times[timeIdx]
                                      / seconds));
                if ((timeIdx < numTimes - 1)
                        && (seenTime.get(timeKey) != null)) {
                    continue;
                }
                seenTime.put(timeKey, timeKey);
                tmpTimes[numFound++] = times[timeIdx];
            }
            double[] newTimes = new double[numFound];
            System.arraycopy(tmpTimes, 0, newTimes, 0, numFound);
            return newTimes;
        }

        /**
         * _more_
         *
         * @param fi _more_
         *
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        protected boolean setData(FieldImpl fi)
                throws VisADException, RemoteException {
            if (trackDisplay == null) {
                return true;
            }

            Unit newUnit = getDisplayUnit();
            //TODO: use the right index
            if ((newUnit != null) && !newUnit.equals(getDisplayUnit())
                    && Unit.canConvert(newUnit, getRawDataUnit())) {
                trackDisplay.setDisplayUnit(newUnit);
                selectRangeDisplay.setDisplayUnit(newUnit);


            }

            FlatField ff   = null;
            FieldImpl grid = null;

            if (trackDisplay != null) {
                trackDisplay.setData(DUMMY_DATA);
                indicator.setVisible(false);
                timesHolder.setData(DUMMY_DATA);
            }


            if (indicator != null) {
                indicator.setVisible(getMarkerVisible());
            }
            int len = fi.getLength();
            /*  for(int i = 0; i< len; i++) {
            FieldImpl fii = (FieldImpl)fi.getSample(i) ;

            trackDisplay.setTrack(fii);
        }    */
            updateTimeSelectRange();
            ff = (FlatField) fi.getSample(0, false);
            trackDisplay.setTrack(ff);


            setTrackTimes();
            applyTimeRange();

            return true;
        }

        /**
         * _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        /*  private void updateIndicator() {
           if (indicator != null) {
               try {
                   lastIndicatorPosition = null;
                   indicator.setStationModel( getMarkerLayout());
                   indicator.setVisible( getMarkerVisible());
                   setScaleOnMarker();
                   applyTimeRange();
               } catch (Exception exc) {
                   logException("Updating indicator", exc);
               }
           }
       } */

        /**
         * _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        private void setScaleOnMarker()
                throws RemoteException, VisADException {
            setScaleOnMarker(getDisplayScale() * markerScale);
        }

        /**
         * Get the TimeDeclutterEnabled property.
         *
         * @return The TimeDeclutterEnabled
         */
        public boolean getTimeDeclutterEnabled() {
            return timeDeclutterEnabled;
        }

        /**
         *  A utility to set the scale on the marker dislayable
         *
         * @param f The new scale value
         *
         * @throws RemoteException When bad things happen
         * @throws VisADException When bad things happen
         */
        private void setScaleOnMarker(float f)
                throws RemoteException, VisADException {
            if (indicator != null) {
                indicator.setScale(f);
            }
        }

        /**
         * _more_
         */
        private void updateIndicator() {
            if (indicator != null) {
                try {
                    lastIndicatorPosition = null;
                    indicator.setStationModel(getMarkerLayout());
                    indicator.setVisible(getMarkerVisible());
                    setScaleOnMarker();
                    // applyTimeRange();
                } catch (Exception exc) {
                    logException("Updating indicator", exc);
                }
            }
        }

        /**
         * _more_
         */
        public void applyTimeRange() {
            try {
                DataTimeRange    dataTimeRange    = getDataTimeRange(true);
                GridDataInstance gridDataInstance = getGridDataInstance();
                Unit             dataTimeUnit;
                DateTime[]       dts = gridDataInstance.getDateTimes();
                dataTimeUnit = dts[0].getUnit();
                int size = dts.length;
                // Range    r                = getRangeForTimeSelect();
                // RealType dataTimeRealType = Util.getRealType(dataTimeUnit);
                Real      startReal = dts[0].getReal();
                Real      endReal   = dts[size - 1].getReal();


                Animation anime     = getViewAnimation();
                Real      aniValue  = ((anime != null)
                                       ? anime.getAniValue()
                                       : null);

                Real[] startEnd = getDataTimeRange().getTimeRange(startReal,
                                      endReal, aniValue);


                double startDate = startEnd[0].getValue(dataTimeUnit);
                double endDate   = startEnd[1].getValue(dataTimeUnit);
                if ( !Misc.equals(lastRange, new Range(startDate, endDate))) {
                    lastRange = new Range(startDate, endDate);
                    if (trackDisplay != null) {
                        trackDisplay.setSelectedRange(startDate, endDate);
                    }
                }
                // set the position of the marker at the animation time
                double aniDate = ((aniValue != null)
                                  && (aniValue instanceof Real))
                                 ? ((Real) aniValue).getValue(dataTimeUnit)
                                 : endDate;
                DataTimeRange dtr = getDataTimeRange();
                if ((dtr != null) && (trackDisplay != null)
                        && useTrackTimes) {
                    dtr.setEndMode(dtr.MODE_ANIMATION);
                    trackDisplay.setSelectedRange(startDate, aniDate);
                }


            } catch (Exception e) {
                logException("applyTimeRange", e);
            }
        }

        /**
         * _more_
         */
        public void applyTimeRange1() {
            try {
                DataTimeRange    dataTimeRange    = getDataTimeRange(true);
                GridDataInstance gridDataInstance = getGridDataInstance();
                Unit             dataTimeUnit;
                Data             d = trackDisplay.getData();
                FlatField        f;
                try {
                    if (d != null) {
                        f = (FlatField) ((FieldImpl) d).getSample(0);
                    } else {
                        f = gridDataInstance.getFlatField();
                    }
                } catch (ClassCastException e) {
                    f = (FlatField) d;
                }

                //System.out.println(f.getType());
                double[][] samples  = f.getValues(false);
                int        numTimes = samples[1].length;
                DateTime   d0       = new DateTime(samples[1][0]);
                DateTime   d1       = new DateTime(samples[1][numTimes - 1]);
                //DateTime[]       dts = gridDataInstance.getDateTimes();
                dataTimeUnit = d0.getUnit();
                int size = numTimes;  //dts.length;
                // Range    r                = getRangeForTimeSelect();
                // RealType dataTimeRealType = Util.getRealType(dataTimeUnit);
                Real      startReal = d0;  //dts[0].getReal();
                Real      endReal   = d1;  //dts[size - 1].getReal();


                Animation anime     = getViewAnimation();
                Real      aniValue  = ((anime != null)
                                       ? anime.getAniValue()
                                       : null);

                Real[] startEnd = getDataTimeRange().getTimeRange(startReal,
                                      endReal, aniValue);


                double startDate = startEnd[0].getValue(dataTimeUnit);
                double endDate   = startEnd[1].getValue(dataTimeUnit);
                if ( !Misc.equals(lastRange, new Range(startDate, endDate))) {
                    lastRange = new Range(startDate, endDate);
                    if (trackDisplay != null) {
                        trackDisplay.setSelectedRange(startDate, endDate);
                    }
                }
                // set the position of the marker at the animation time
                double aniDate = ((aniValue != null)
                                  && (aniValue instanceof Real))
                                 ? ((Real) aniValue).getValue(dataTimeUnit)
                                 : endDate;
                DataTimeRange dtr = getDataTimeRange();
                if ((dtr != null) && (trackDisplay != null)
                        && useTrackTimes) {
                    dtr.setEndMode(dtr.MODE_ANIMATION);
                    trackDisplay.setSelectedRange(startDate, aniDate);
                }


            } catch (Exception e) {
                logException("applyTimeRange", e);
            }
        }

        /**
         * Respond to a timeChange event
         *
         * @param time new time
         */
        protected void timeChanged(Real time) {

            super.timeChanged(time);
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        private boolean trackDataOk() throws VisADException, RemoteException {

            DataInstance dataInstance = getDataInstance();
            if ((dataInstance == null) || !dataInstance.dataOk()) {
                return false;
            }
            return true;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isInitDone() {
            return true;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        protected boolean haveMultipleFields() {
            return false;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        protected int getColorRangeIndex() {
            return 0;
        }

        /**
         * _more_
         */
        private void updateTimeSelectRange() {
            try {
                Range r = getRangeForTimeSelect();
                if (r == null) {
                    return;
                }
                if (trackDisplay != null) {
                    trackDisplay.setRangeForSelect(r.getMin(), r.getMax());
                }
            } catch (Exception e) {
                logException("updateTimeSelectRange", e);
            }
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        private Range getRangeForTimeSelect()
                throws VisADException, RemoteException {
            Range            range = getRange();
            GridDataInstance gdi   = getGridDataInstance();
            if ((gdi != null) && (gdi.getNumRealTypes() > 1)) {
                range = gdi.getRange(1);
            }
            return range;
        }
    }

    /**
     * _more_
     *
     * @param time _more_
     */
    protected void timeChanged(Real time) {

        gridTrackControl.timeChanged(time);
    }

    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     * @return true if everything is okay
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */

    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        super.init((DataChoice) null);
        gridTrackControl = new MyTrackControl();
        // super.init(dataChoice);
        this.dataChoice = dataChoice;
        DataInstance      di      = getDataInstance();
        DerivedDataChoice ddc     = (DerivedDataChoice) dataChoice;


        Hashtable         choices = ddc.getUserSelectedChoices();
        DirectDataChoice udc =
            (DirectDataChoice) choices.get(new String("D1"));
        DirectDataChoice vdc =
            (DirectDataChoice) choices.get(new String("D2"));
        DirectDataChoice wdc =
            (DirectDataChoice) choices.get(new String("D3"));
        DirectDataChoice sdc =
            (DirectDataChoice) choices.get(new String("scaler"));
        addDataChoice(udc);
        addDataChoice(vdc);
        addDataChoice(wdc);


        u  = (FieldImpl) udc.getData(null);
        v  = (FieldImpl) vdc.getData(null);
        pw = (FieldImpl) wdc.getData(null);
        s  = (FieldImpl) sdc.getData(null);
        doMakeDataInstance(sdc);


        GridDataInstance gdi = new GridDataInstance(sdc, getDataSelection(),
                                   getRequestProperties());

        gridTrackControl.controlContext = getControlContext();
        gridTrackControl.updateGridDataInstance(gdi);
        setDisplayUnit(gdi.getRawUnit(0));
        initDisplayUnit();

        // level widget init
        levelBox = gridTrackControl.doMakeLevelControl(null);
        levelBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String cmd = event.getActionCommand();
                if (cmd.equals(CMD_SETLEVELS)) {
                    TwoFacedObject select =
                        (TwoFacedObject) ((JComboBox) event.getSource())
                            .getSelectedItem();
                    setLevel(select);
                }
            }
        });
        ImageIcon upIcon =
            GuiUtils.getImageIcon(
                "/ucar/unidata/idv/control/images/LevelUp.gif");
        levelUpBtn = new JButton(upIcon);
        levelUpBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        levelUpBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                moveUpDown(-1);
            }
        });

        ImageIcon downIcon =
            GuiUtils.getImageIcon(
                "/ucar/unidata/idv/control/images/LevelDown.gif");
        levelDownBtn = new JButton(downIcon);
        levelDownBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        levelDownBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                moveUpDown(1);
            }
        });

        //        levelLabel = GuiUtils.rLabel("<html><u>L</u>evels:");
        levelLabel = GuiUtils.rLabel(getLevelsLabel());
        levelLabel.setDisplayedMnemonic(GuiUtils.charToKeyCode("L"));
        levelLabel.setLabelFor(levelBox);

        DataSelection tmpSelection = new DataSelection(0);
        //tmpSelection.setFromLevel(null);
        //tmpSelection.setToLevel(null);

        List     levelsList = sdc.getAllLevels(tmpSelection);
        Object[] levels     = null;
        if ((levelsList != null) && (levelsList.size() > 0)) {
            levels =
                (Object[]) levelsList.toArray(new Object[levelsList.size()]);
            SampledSet ss = GridUtil.getSpatialDomain(gdi.getGrid());
            zunit = ss.getSetUnits()[2];
        }


        if (levels == null) {
            GridUtil.getSpatialDomain(gdi.getGrid());
            levels = ((GridDataInstance) getDataInstance()).getLevels();
            zunit  = ((GridDataInstance) getDataInstance()).getZUnit();
        }

        if (currentLevel == null) {
            currentLevel = getDataSelection().getFromLevel();
        }
        if ((levels != null) && (levels.length > 0)
                && (currentLevel == null)) {
            currentLevel = levels[0];
        }

        setLevels(levels);

        // the control for the track
        setDisplayActive();

        if ( !gridTrackControl.trackDataOk()) {
            List dlist = new ArrayList();
            dlist.add(sdc);
            gridTrackControl.appendDataChoices(dlist);
            if ( !gridTrackControl.trackDataOk()) {
                return false;
            }
        }
        gridTrackControl.trackDisplay = new TrackDisplayable("track"
                + dataChoice);
        setLineWidth(gridTrackControl.trackWidth);
        addDisplayable(gridTrackControl.trackDisplay, getAttributeFlags());
        gridTrackControl.selectRangeDisplay = new SelectRangeDisplayable();
        addDisplayable(gridTrackControl.selectRangeDisplay,
                       FLAG_DISPLAYUNIT | FLAG_SELECTRANGE);
        getViewAnimation();
        gridTrackControl.indicator = new StationModelDisplayable("indicator");
        gridTrackControl.indicator.setScale(gridTrackControl.markerScale);
        gridTrackControl.indicator.setShouldUseAltitude(true);
        gridTrackControl.updateIndicator();
        addDisplayable(gridTrackControl.indicator);
        gridTrackControl.timesHolder = new LineDrawing("track_time"
                + dataChoice);
        gridTrackControl.timesHolder.setManipulable(false);
        gridTrackControl.timesHolder.setVisible(false);
        addDisplayable(gridTrackControl.timesHolder);
        // return setData(dataChoice);

        return true;


    }


    /**
     * current level
     *
     * @param levels _more_
     */

    public void setLevels(Object[] levels) {
        setOkToFireEvents(false);
        currentLevels = levels;
        levelEnabled  = (levels != null);

        if (levelBox == null) {
            return;
        }
        levelBox.setEnabled(levelEnabled);
        levelUpBtn.setEnabled(levelEnabled);
        levelDownBtn.setEnabled(levelEnabled);
        levelLabel.setEnabled(levelEnabled);




        GuiUtils.setListData(levelBox, formatLevels(levels));
        if (currentLevel != null) {
            levelBox.setSelectedItem(currentLevel);
        }

        setOkToFireEvents(true);
    }

    /**
     * _more_
     *
     * @param r _more_
     */
    public void setLevel(Object r) {
        currentLevel = r;
    }

    /**
     * move up/down levels by the delta
     *
     * @param delta   delta between levels
     */
    private void moveUpDown(int delta) {
        int selected = levelBox.getSelectedIndex();
        if (selected >= 0) {
            selected += delta;
            int max = levelBox.getItemCount();
            if (selected >= max) {
                selected = max - 1;
            }
        }
        if (selected < 0) {
            selected = 0;
        }
        levelBox.setSelectedIndex(selected);
    }

    /**
     * Get the label for the levels box.
     * @return the label
     */
    public String getLevelsLabel() {
        return "Levels:";
    }



    /**
     * Initialize the display unit
     */
    protected void initDisplayUnit() {
        if (getDisplayUnit() == null) {
            setDisplayUnit(getDefaultDistanceUnit());
        }
    }

    /**
     * Signal base class to add this as a display listener
     *
     * @return Add as display listener
     */

    protected boolean shouldAddDisplayListener() {
        return true;
    }




    /**
     * Signal base class to add this as a control listener
     *
     * @return Add as control listener
     */
    protected boolean shouldAddControlListener() {
        return true;
    }


    /**
     * Remove this DisplayControl from the system.  Nulls out any
     * objects for garbage collection
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void doRemove() throws VisADException, RemoteException {
        clearCursor();
        if (glyphs != null) {
            for (int i = 0; i < glyphs.size(); i++) {
                ((DrawingGlyph) glyphs.get(i)).setBeenRemoved(true);
            }
        }
        glyphs         = null;
        selectedGlyphs = null;
        displayHolder  = null;
        super.doRemove();
    }


    /**
     * Overwrite the legend labels method to use the editor title if there is one.
     *
     * @param labels List of labels
     * @param legendType Side or bottom
     */
    protected void getLegendLabels(List labels, int legendType) {
        if ((editorTitle != null) && (editorTitle.length() > 0)) {
            labels.add(editorTitle);
        } else {
            super.getLegendLabels(labels, legendType);
        }
    }


    /**
     * Remove the glyph from the drawing
     *
     * @param glyph The glyph to remove
     */
    public void removeGlyph(DrawingGlyph glyph) {
        glyph.setBeenRemoved(true);
        glyphs.remove(glyph);
        selectedGlyphs.remove(glyph);

        try {
            displayHolder.removeDisplayable(glyph.getDisplayable());
        } catch (Exception exc) {
            logException("Removing glyph", exc);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected String getColorParamName() {

        return paramName;
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    void createTrajectoryControl()
            throws VisADException, RemoteException, Exception {

        //  FieldImpl u = this.dataChoice;
        //   super.init(dataChoice0);


        Unit dUnit = ((FlatField) s.getSample(0)).getRangeUnits()[0][0];
        gridTrackControl.setDisplayUnit(dUnit);
        final Unit rgUnit =
            ((FlatField) pw.getSample(0)).getRangeUnits()[0][0];
        FieldImpl w;
        if (Unit.canConvert(rgUnit, CommonUnits.METERS_PER_SECOND)) {
            w = pw;
        } else {
            FieldImpl pFI = DerivedGridFactory.createPressureGridFromDomain(
                                (FlatField) pw.getSample(0));
            FieldImpl hPI = DerivedGridFactory.convertPressureToHeight(pFI);
            w = DerivedGridFactory.convertPressureVelocityToHeightVelocity(
                pw, hPI, null);
        }

        final Set timeSet  = s.getDomainSet();
        int       numTimes = timeSet.getLength();
        Unit      timeUnit = timeSet.getSetUnits()[0];
        final Unit paramUnit =
            ((FlatField) s.getSample(0)).getRangeUnits()[0][0];
        FunctionType rt =
            (FunctionType) ((FlatField) s.getSample(0)).getType();
        final String paramName =
            rt.getFlatRange().getRealComponents()[0].getName();

        double[]   timeVals     = timeSet.getDoubles()[0];

        SampledSet domain0      = GridUtil.getSpatialDomain(s);

        double[]   ttts         = timeSet.getDoubles()[0];
        boolean    normalizeLon = true;

        boolean    isLatLon     = GridUtil.isLatLonOrder(domain0);
        int        latIndex     = isLatLon
                                  ? 0
                                  : 1;
        int        lonIndex     = isLatLon
                                  ? 1
                                  : 0;
        float[][]  geoVals      = getEarthLocationPoints(latIndex, lonIndex);
        int        numPoints    = geoVals[0].length;
        //first step  init  u,v, w, and s at all initial points
        List<DerivedGridFactory.TrajInfo> tj =
            DerivedGridFactory.calculateTrackPoints(u, v, w, s, ttts,
                geoVals, numPoints, numTimes, latIndex, lonIndex, true,
                normalizeLon);

        int numParcels = numPoints;  //10;
        final FunctionType ft = new FunctionType(
                                    RealType.Generic,
                                    new FunctionType(
                                        RealTupleType.SpatialEarth3DTuple,
                                        RealType.getRealType(paramName)));

        List tracks;

        tracks = DerivedGridFactory.createTracks(paramName, tj, timeSet, ft,
                paramUnit, numParcels);
        FlatField mergedTracks = DerivedGridFactory.mergeTracks(tracks);

        FunctionType fiType = new FunctionType(RealType.Time,
                                  mergedTracks.getType());

        DateTime endTime = new DateTime(timeVals[numTimes - 1], timeUnit);

        FieldImpl fi =
            new FieldImpl(fiType,
                          new SingletonSet(new RealTuple(new Real[] {
                              endTime })));
        fi.setSample(0, mergedTracks, false);

        //super.init(fi)

        gridTrackControl.setData(fi);
        Range range = gridTrackControl.getGridDataInstance().getRange(
                          gridTrackControl.getColorRangeIndex());  //GridUtil.getMinMax(fi)[0];
        gridTrackControl.setRange(range);
        Set[]         rset = mergedTracks.getRangeSets();
        DoubleSet     ds   = (DoubleSet) rset[0];

        SetType       st   = (SetType) ds.getType();
        RealTupleType rtt  = st.getDomain();

        RealType      rt0  = (RealType) rtt.getRealComponents()[0];
        super.setDataInstance(getDataInstance());
        gridTrackControl.selectRangeDisplay.setSelectRealType(rt0);
        //super.initializationDone = true;
        super.paramName = paramName;
        controlPane.setVisible(true);
        controlPane.add(gridTrackControl.doMakeContents());

    }





    /**
     * _more_
     *
     *
     * @param latIndex _more_
     * @param lonIndex _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public float[][] getEarthLocationPoints(int latIndex, int lonIndex)
            throws Exception {
        double clevel = 0;
        if (currentLevel instanceof Real) {
            clevel = ((Real) currentLevel).getValue();
        } else if (currentLevel instanceof TwoFacedObject) {
            Object oj = ((TwoFacedObject) currentLevel).getId();
            clevel = ((Real) oj).getValue();
        }

        if (pressToHeightCS == null) {
            pressToHeightCS =
                DataUtil.getPressureToHeightCS(DataUtil.STD_ATMOSPHERE);
        }
        double[][] hVals = pressToHeightCS.toReference(new double[][] {
            new double[] { clevel }
        }, new Unit[] { zunit });

        float      z     = (float) hVals[0][0];
        if (currentCmd.getLabel().equals(
                GlyphCreatorCommand.CMD_SYMBOL.getLabel())) {
            int       pointNum = glyphs.size();

            float[][] points   = new float[3][pointNum];

            for (int i = 0; i < pointNum; i++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
                points[latIndex][i] = glyph.getLatLons()[0][0];
                points[lonIndex][i] = (float) LatLonPointImpl.lonNormal(
                    glyph.getLatLons()[1][0]);
                points[2][i] = z;
            }
            return points;
        } else {

            if (glyphs.size() == 0) {
                return null;
            }
            Gridded3DSet domain =
                gridTrackControl.getGridDataInstance().getDomainSet3D();
            Unit[]   du       = domain.getSetUnits();
            MapMaker mapMaker = new MapMaker();
            for (DrawingGlyph glyph : (List<DrawingGlyph>) glyphs) {
                float[][] lls = glyph.getLatLons();
                float[][] tmp = glyph.getLatLons();
                if (du[lonIndex].isConvertible(CommonUnit.radian)) {
                    for (int i = 0; i < lls[1].length; i++) {
                        lls[0][i] =
                            (float) GridUtil.normalizeLongitude(domain,
                                tmp[0][i], du[lonIndex]);
                    }
                } else if (du[lonIndex].isConvertible(
                        CommonUnits.KILOMETER)) {
                    for (int i = 0; i < lls[1].length; i++) {
                        lls[1][i] =
                            (float) LatLonPointImpl.lonNormal(lls[1][i]);
                    }
                }
                mapMaker.addMap(lls);
            }

            float[][][] latlons = GridUtil.findContainedLatLons(domain,
                                      mapMaker.getMaps());
            int       num    = latlons[0][0].length;
            float[][] points = new float[3][num];

            for (int i = 0; i < num; i++) {

                points[latIndex][i] = latlons[0][0][i];
                points[lonIndex][i] =
                    (float) LatLonPointImpl.lonNormal(latlons[0][1][i]);
                points[2][i] = z;
            }

            return points;
        }
    }


    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */

    protected Container doMakeContents()
            throws VisADException, RemoteException {
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.add("Controls", GuiUtils.top(doMakeControlsPanel()));

        return GuiUtils.centerBottom(tabbedPane, new JLabel(""));
    }


    /**
     * Make the main tabbed pane
     *
     * @return Controls panel
     */
    protected JComponent doMakeControlsPanel() {
        controlPane = new JPanel();
        controlPane.setPreferredSize(new Dimension(300, 180));
        JComponent controlHolder = GuiUtils.topCenter(new JLabel("Result:"),
                                       controlPane);


        List widgets = new ArrayList();
        addControlWidgets(widgets);
        GuiUtils.tmpInsets = new Insets(4, 4, 0, 4);
        JPanel comps = GuiUtils.doLayout(widgets, 2, GuiUtils.WT_NY,
                                         GuiUtils.WT_N);


        return GuiUtils.top(GuiUtils.topCenter(comps, controlPane));
    }

    /**
     * Add the widgets into the controls panel
     *
     * @param widgets List to add to. Add in pairs (label, widget)
     */
    protected void addControlWidgets(List widgets) {
        JPanel levelUpDown = GuiUtils.doLayout(new Component[] { levelUpBtn,
                levelDownBtn }, 1, GuiUtils.WT_N, GuiUtils.WT_N);
        JPanel levelSelector = GuiUtils.doLayout(new Component[] { levelBox,
                levelUpDown }, 2, GuiUtils.WT_N, GuiUtils.WT_N);

        JComponent widgets0 = GuiUtils.formLayout(new Component[] {
                                  levelLabel,
                                  GuiUtils.left(levelSelector) });
        JButton unloadBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Cut16.gif", this,
                                     "removeAllGlyphs");
        unloadBtn.setToolTipText("Remove all glyphs");

        msgLabel  = new JLabel();
        pointsBtn = new JRadioButton("Points:", isPoints);
        setCurrentCommand(GlyphCreatorCommand.CMD_SYMBOL);
        rectangleBtn = new JRadioButton("Rectangle:", !isPoints);
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JRadioButton source = (JRadioButton) e.getSource();
                if (source == pointsBtn) {
                    setCurrentCommand(GlyphCreatorCommand.CMD_SYMBOL);
                    removeAllGlyphs();
                } else {
                    setCurrentCommand(GlyphCreatorCommand.CMD_RECTANGLE);
                    removeAllGlyphs();
                }
            }
        };
        pointsBtn.addActionListener(listener);
        rectangleBtn.addActionListener(listener);
        GuiUtils.buttonGroup(pointsBtn, rectangleBtn);
        createTrjBtn = new JButton("Create Trajectory");
        createTrjBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    createTrajectoryControl();
                } catch (VisADException ee) {}
                catch (RemoteException er) {}
                catch (Exception exr) {}
            }
        });

        JComponent rightComp = GuiUtils.vbox(GuiUtils.left(pointsBtn),
                                             GuiUtils.left(rectangleBtn),
                                             GuiUtils.left(createTrjBtn));
        JLabel showLabel = GuiUtils.rLabel("Trajectory Initial:");
        showLabel.setVerticalTextPosition(JLabel.TOP);

        widgets.add(
            GuiUtils.topBottom(
                widgets0,
                GuiUtils.leftRight(
                    GuiUtils.top(
                        GuiUtils.inset(
                            showLabel,
                            new Insets(10, 0, 0, 0))), GuiUtils.centerRight(
                                GuiUtils.top(rightComp),
                                GuiUtils.right(unloadBtn)))));

    }




    /**
     * Should we show the locatio  widgets
     *
     * @return  show the locatio  widgets
     */
    protected boolean showLocationWidgets() {
        return true;
    }


    /**
     * Apply the current color to all glyphs
     */
    public void applyColorToAll() {
        for (int i = 0; i < selectedGlyphs.size(); i++) {
            ((DrawingGlyph) selectedGlyphs.get(i)).setColor(getColor());
        }
    }





    /**
     * Remove em all.
     */
    public void removeAllGlyphs() {
        try {
            while (glyphs.size() > 0) {
                removeGlyph((DrawingGlyph) glyphs.get(0));
            }
            while (controlPane.getComponentCount() > 0) {
                controlPane.remove(0);
                controlPane.setVisible(false);
                if (gridTrackControl.trackDisplay != null) {
                    gridTrackControl.trackDisplay.setData(DUMMY_DATA);
                    gridTrackControl.indicator.setVisible(false);
                    gridTrackControl.timesHolder.setData(DUMMY_DATA);
                }
            }
        } catch (Exception exc) {
            logException("Removing drawings", exc);
        }

    }



    /**
     * Clear the cursor in the main display
     */
    private void clearCursor() {
        setCursor(null);
    }

    /**
     * Set the cursor in the main display
     *
     * @param c  The cursor id
     */
    private void setCursor(int c) {
        setCursor(Cursor.getPredefinedCursor(c));
    }

    /**
     * Set the cursor in the main display
     *
     * @param c The cursor
     */
    private void setCursor(Cursor c) {
        getViewManager().setCursorInDisplay(c);
    }




}
