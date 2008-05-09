/*
 * $Id: TrackControl.java,v 1.69 2007/08/21 11:32:08 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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






package ucar.unidata.idv.control.storm;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.data.storm.*;


import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.control.DisplayControlImpl;

import ucar.unidata.idv.control.chart.*;




import ucar.unidata.ui.drawing.*;
import ucar.unidata.ui.symbol.*;
import ucar.unidata.util.ColorTable;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.*;

import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.display.*;


import ucar.visad.display.*;
import ucar.visad.display.Animation;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.SelectRangeDisplayable;
import ucar.visad.display.SelectorPoint;
import ucar.visad.display.StationModelDisplayable;
import ucar.visad.display.TrackDisplayable;



import visad.*;

import visad.bom.Radar2DCoordinateSystem;

import visad.georef.EarthLocation;

import visad.georef.EarthLocationLite;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import visad.util.DataUtility;

import java.awt.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;

import java.beans.*;

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.Arrays;


import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;



/**
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class StormDisplayState {

    /** The array of colors we cycle through */
    private static Color[] colors = {
        Color.RED, Color.PINK, Color.MAGENTA, Color.ORANGE, Color.YELLOW,
        Color.GREEN, Color.BLUE, Color.CYAN, Color.GRAY, Color.LIGHT_GRAY
    };

    /** _more_ */
    private static int[] nextColor = { 0 };

    /** _more_          */
    private List<StormTrackChart> charts = new ArrayList<StormTrackChart>();





    /** _more_ */
    private Object MUTEX = new Object();

    /** _more_ */
    private static final Data DUMMY_DATA = new Real(0);



    /** _more_ */
    private CompositeDisplayable holder;



    /** _more_ */
    private StormInfo stormInfo;


    /** _more_ */
    private WayDisplayState forecastState;

    /** _more_ */
    private boolean haveLoadedForecasts = false;

    /** _more_ */
    private boolean changed = false;


    /** _more_ */
    private boolean active = false;


    /** _more_ */
    private StormTrackCollection trackCollection;


    /** _more_ */
    //    private List<StormTrack> tracks;

    /** _more_ */
    private JTable trackTable;

    /** _more_ */
    private AbstractTableModel trackModel;

    /** _more_ */
    private StormTrackControl stormTrackControl;


    /** _more_ */
    private WayDisplayState obsDisplayState;


    /** time holder */
    private DisplayableData timesHolder = null;

    /** _more_ */
    private JComponent contents;

    /** _more_ */
    private JTabbedPane tabbedPane;

    /** _more_ */
    private JComponent originalContents;



    /** _more_ */
    private Hashtable<Way, WayDisplayState> wayDisplayStateMap =
        new Hashtable<Way, WayDisplayState>();



    /**
     * _more_
     */
    public StormDisplayState() {}


    /**
     * _more_
     *
     * @param stormInfo _more_
     *
     * @throws Exception _more_
     */
    public StormDisplayState(StormInfo stormInfo) throws Exception {
        this.stormInfo = stormInfo;
        forecastState  = new WayDisplayState(this, new Way("forecaststate"));
        forecastState.setVisible(false);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent getContents() {
        if (contents == null) {
            contents = doMakeContents();
        }
        return contents;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected List<WayDisplayState> getWayDisplayStates() {
        return (List<WayDisplayState>) Misc.toList(
            wayDisplayStateMap.elements());
    }

    /**
     * _more_
     *
     * @param way _more_
     *
     * @return _more_
     */
    protected WayDisplayState getWayDisplayState(Way way) {
        WayDisplayState wayState = wayDisplayStateMap.get(way);
        if (wayState == null) {
            wayDisplayStateMap.put(way,
                                   wayState = new WayDisplayState(this, way));
            //        "idv.stormtrackcontrol.way.color"
            if (wayState.getColor() == null) {
                wayState.setColor(getNextColor(nextColor));
            }
        }
        return wayState;
    }


    /**
     * _more_
     */
    public void onlyShowSelectedWays() {
        List<Way>             ways             = new ArrayList<Way>();
        List<WayDisplayState> wayDisplayStates = getWayDisplayStates();
        for (WayDisplayState wayDisplayState : wayDisplayStates) {
            if (wayDisplayState.getVisible()) {
                ways.add(wayDisplayState.getWay());
            }
        }
        stormTrackControl.onlyShowTheseWays(ways);
    }


    /**
     * _more_
     */
    protected void reload() {
        if ( !active) {
            return;
        }
        deactivate();
        active = true;
        showStorm();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected StormTrackCollection getTrackCollection() {
        return trackCollection;
    }


    /**
     * _more_
     */
    public void deactivate() {
        try {
            for (StormTrackChart stormTrackChart : charts) {
                stormTrackChart.deactivate();
            }
            trackCollection = null;
            active          = false;
            stormTrackControl.removeDisplayable(holder);
            holder = null;
            contents.removeAll();
            contents.add(BorderLayout.NORTH, originalContents);
            List<WayDisplayState> wayDisplayStates = getWayDisplayStates();
            for (WayDisplayState wayDisplayState : wayDisplayStates) {
                wayDisplayState.deactivate();
            }
            contents.repaint(1);
            stormTrackControl.stormChanged(StormDisplayState.this);

        } catch (Exception exc) {
            stormTrackControl.logException("Deactivating storm", exc);
        }
    }

    /**
     * _more_
     */
    public void loadStorm() {
        if (active) {
            return;
        }
        active = true;
        showStorm();
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private JComponent doMakeContents() {
        JButton loadBtn  = new JButton("Load Tracks:");
        JLabel  topLabel = GuiUtils.cLabel("  " + stormInfo);
        loadBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                loadStorm();
            }
        });

        JComponent top = GuiUtils.hbox(loadBtn, topLabel);
        originalContents = GuiUtils.inset(top, 5);
        JComponent contents = GuiUtils.top(originalContents);
        contents = new JPanel(new BorderLayout());
        contents.add(BorderLayout.NORTH, originalContents);

        return contents;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String xxx() {
        return stormInfo.toString();
    }


    /**
     * _more_
     */
    public void initDone() {
        if (getActive()) {
            showStorm();
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getForecastVisible() {
        return forecastState.getVisible();
    }

    /**
     * _more_
     */
    private void initCenterContents() {

        contents.removeAll();
        JButton unloadBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Cut16.gif", this,
                                     "deactivate");
        unloadBtn.setToolTipText("Remove this storm");

        JComponent top =
            GuiUtils.inset(GuiUtils.leftRight(GuiUtils.lLabel("Storm: "
                + stormInfo), unloadBtn), new Insets(0, 0, 0, 0));



        List<RealType> attributeTypes = new ArrayList<RealType>();
        Hashtable      seenTypes      = new Hashtable();
        Hashtable      seenWays       = new Hashtable();
        for (StormTrack track : trackCollection.getTracks()) {
            if (seenWays.get(track.getWay()) != null) {
                continue;
            }
            seenWays.put(track.getWay(), track.getWay());
            List<RealType> types = track.getTypes();
            for (RealType realType : types) {
                if (seenTypes.get(realType) != null) {
                    continue;
                }
                seenTypes.put(realType, realType);
                attributeTypes.add(realType);
            }
        }
        Vector radiusAttrNames = null;
        Vector attrNames       = null;
        if ((attributeTypes != null) && (attributeTypes.size() > 0)) {
            attrNames = new Vector();
            for (RealType type : attributeTypes) {
                if (Unit.canConvert(type.getDefaultUnit(),
                                    CommonUnit.meter)) {
                    if (radiusAttrNames == null) {
                        radiusAttrNames = new Vector();
                        radiusAttrNames.add(new TwoFacedObject("None", null));
                    }
                    radiusAttrNames.add(new TwoFacedObject(getLabel(type),
                            type));
                }
                attrNames.add(new TwoFacedObject(getLabel(type), type));
            }
        }


        List components = new ArrayList();
        //Sort them by name
        List<Way> ways             = Misc.sort(trackCollection.getWayList());
        boolean   haveDoneForecast = false;
        //        components.add(GuiUtils.italicizeFont(new JLabel("<html><u>Track Type</u></html>")));
        components.add(new JLabel("<html><u><i>Track Type</i></u></html>"));
        components.add(new JLabel("<html><u><i>Visible</i></u></html>"));
        components.add(new JLabel(((radiusAttrNames != null)
                                   ? "<html><u><i>Rings</i></u></html>"
                                   : "")));
        components.add(new JLabel("<html><u><i>Color</i></u></html>"));
        components.add(new JLabel("<html><u><i>Color Field</i></u></html>"));

        attrNames.add(0, new TwoFacedObject("Fixed", null));
        for (Way way : ways) {
            WayDisplayState wayDisplayState = getWayDisplayState(way);
            if ( !stormTrackControl.okToShowWay(wayDisplayState.getWay())) {
                continue;
            }
            JLabel    wayLabel  = new JLabel(way.toString());
            JComboBox radiusBox = ((radiusAttrNames != null)
                                   ? new JComboBox(radiusAttrNames)
                                   : null);


            Component radiusComp;
            if (radiusBox != null) {
                radiusComp = radiusBox;
            } else {
                radiusComp = GuiUtils.filler();
            }

            Vector tmpAttrNames = new Vector(attrNames);
            tmpAttrNames.add(1, new TwoFacedObject("Default", "default"));



            if (way.isObservation()) {
                components.add(0 + 5, wayLabel);
                components.add(
                    1 + 5,
                    GuiUtils.left(wayDisplayState.getVisiblityCheckBox()));
                components.add(2 + 5, radiusComp);
                //                components.add(2+5, GuiUtils.left(wayDisplayState.getRingsVisiblityCheckBox()));
                components.add(
                    3 + 5,
                    GuiUtils.left(
                        GuiUtils.wrap(wayDisplayState.getColorSwatch())));
                components.add(
                    4 + 5, wayDisplayState.getParamComponent(tmpAttrNames));
            } else {
                if ( !haveDoneForecast) {
                    haveDoneForecast = true;
                    components.add(GuiUtils.filler(2, 5));
                    components.add(GuiUtils.filler(2, 5));
                    components.add(GuiUtils.filler(2, 5));
                    components.add(GuiUtils.filler(2, 5));
                    components.add(GuiUtils.filler(2, 5));

                    components.add(GuiUtils.lLabel("Forecasts:"));
                    components.add(
                        GuiUtils.left(forecastState.getVisiblityCheckBox()));
                    components.add(GuiUtils.filler());
                    components.add(GuiUtils.filler());
                    components.add(
                        forecastState.getParamComponent(attrNames));
                }
                components.add(wayLabel);
                components.add(
                    GuiUtils.left(wayDisplayState.getVisiblityCheckBox()));
                components.add(radiusComp);
                //                components.add(GuiUtils.left(wayDisplayState.getRingsVisiblityCheckBox()));
                components.add(
                    GuiUtils.left(
                        GuiUtils.wrap(wayDisplayState.getColorSwatch())));
                components.add(
                    wayDisplayState.getParamComponent(tmpAttrNames));
            }
        }

        GuiUtils.tmpInsets = new Insets(2, 2, 0, 2);
        JComponent wayComp = GuiUtils.topLeft(GuiUtils.doLayout(components,
                                 5, GuiUtils.WT_NNN, GuiUtils.WT_N));
        //Put the list of ways into a scroller if there are lots of them
        if (ways.size() > 10) {
            int width  = 300;
            int height = 400;
            JScrollPane scroller = GuiUtils.makeScrollPane(wayComp, width,
                                       height);
            scroller.setBorder(BorderFactory.createLoweredBevelBorder());
            scroller.setPreferredSize(new Dimension(width, height));
            scroller.setMinimumSize(new Dimension(width, height));
            wayComp = scroller;
        }

        wayComp    = GuiUtils.inset(wayComp, new Insets(0, 5, 0, 0));
        tabbedPane = GuiUtils.getNestedTabbedPane();
        tabbedPane.addTab("Tracks", wayComp);

        if (charts.size() == 0) {
            charts.add(new StormTrackChart(this, "Storm Chart"));
        }
        for (StormTrackChart stormTrackChart : charts) {
            tabbedPane.addTab(stormTrackChart.getName(),
                              stormTrackChart.getContents());
        }



        JComponent inner = GuiUtils.topCenter(top, tabbedPane);
        inner = GuiUtils.inset(inner, 5);
        contents.add(BorderLayout.CENTER, inner);
        contents.invalidate();
        contents.validate();
        contents.repaint();

    }



    /**
     * _more_
     *
     * @param time _more_
     */
    protected void timeChanged(Real time) {
        for (StormTrackChart stormTrackChart : charts) {
            stormTrackChart.timeChanged(time);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected RealType getForecastParamType() {
        return forecastState.getParamType();
    }


    /**
     * _more_
     *
     * @param way _more_
     *
     * @return _more_
     */
    protected boolean canShowWay(Way way) {
        return getWayDisplayState(way).getVisible();
    }

    /**
     * _more_
     *
     * @param stormTrackControl _more_
     */
    protected void setStormTrackControl(StormTrackControl stormTrackControl) {
        this.stormTrackControl = stormTrackControl;
    }

    /**
     * _more_
     */
    protected void showStorm() {
        Misc.run(new Runnable() {
            public void run() {
                DisplayMaster displayMaster =
                    stormTrackControl.getDisplayMaster();
                boolean wasActive = displayMaster.ensureInactive();
                try {
                    synchronized (MUTEX) {
                        showStormInner();
                        stormTrackControl.stormChanged(
                            StormDisplayState.this);
                    }
                } catch (Exception exc) {
                    stormTrackControl.logException("Showing storm", exc);
                } finally {
                    if (wasActive) {
                        try {
                            displayMaster.setActive(true);
                        } catch (Exception exc) {}
                    }
                }

            }
        });
    }


    /**
     * _more_
     *
     * @param displayable _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected void addDisplayable(Displayable displayable)
            throws VisADException, RemoteException {
        holder.addDisplayable(displayable);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected StormTrackControl getStormTrackControl() {
        return stormTrackControl;
    }




    /**
     * _more_
     *
     *
     * @throws Exception _more_
     */
    private void showStormInner() throws Exception {

        //Read the tracks if we haven't
        long t1 = System.currentTimeMillis();
        if (trackCollection == null) {
            contents.removeAll();
            contents.add(
                GuiUtils.top(
                    GuiUtils.inset(new JLabel("Loading Tracks..."), 5)));
            contents.invalidate();
            contents.validate();
            contents.repaint();
            trackCollection =
                stormTrackControl.getStormDataSource().getTrackCollection(
                    stormInfo, stormTrackControl.getOkWays());
            initCenterContents();
            stormTrackControl.addDisplayable(holder =
                new CompositeDisplayable());
            //Add the tracks
            for (StormTrack track : trackCollection.getTracks()) {
                WayDisplayState wayDisplayState =
                    getWayDisplayState(track.getWay());
                wayDisplayState.addTrack(track);
            }
            obsDisplayState = getWayDisplayState(Way.OBSERVATION);
            StormTrack obsTrack = trackCollection.getObsTrack();
            if (obsTrack != null) {

                FieldImpl field = makeField(obsTrack, null);
                obsDisplayState.addTrack(obsTrack, field);

                obsDisplayState.getTrackDisplay().setTrack(field);
                makeRingField(obsTrack, obsDisplayState, null);


                List<DateTime> times = obsTrack.getTrackTimes();
                timesHolder = new LineDrawing("track_time"
                        + stormInfo.getStormId());
                timesHolder.setManipulable(false);
                timesHolder.setVisible(false);
                timesHolder.setData(ucar.visad.Util.makeTimeSet(times));
                holder.addDisplayable(timesHolder);

                StationModelDisplayable dots =
                    new StationModelDisplayable("dots");
                obsDisplayState.addDisplayable(dots);
                dots.setScale(1.0f);
                holder.addDisplayable(dots);
                dots.setStationModel(getObservationStationModel());
                //                dots.setStationData(PointObFactory.makeTimeSequenceOfPointObs( obsDisplayState.getPointObs(),
                //                                                                               24*60,-1));
                dots.setStationData(
                    PointObFactory.makeTimeSequenceOfPointObs(
                        obsDisplayState.getPointObs(), -1, -1));
            }
        }



        //Don't load the forecast tracks until we need to
        if (getForecastVisible() && !haveLoadedForecasts) {
            haveLoadedForecasts = true;
            List<WayDisplayState> wayDisplayStates = getWayDisplayStates();
            for (WayDisplayState wayDisplayState : wayDisplayStates) {
                if ( !stormTrackControl.okToShowWay(
                        wayDisplayState.getWay())) {
                    continue;
                }
                if (wayDisplayState.getWay().isObservation()) {
                    continue;
                }
                wayDisplayState.makeField();
            }
        }

        for (StormTrackChart stormTrackChart : charts) {
            stormTrackChart.updateChart();
        }


        checkVisibility();
        long t2 = System.currentTimeMillis();
        System.err.println("time:" + (t2 - t1));

    }



    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    protected String getLabel(RealType type) {
        return Util.cleanTypeName(type.getName()).replace("_", " ");
    }



    /**
     * _more_
     *
     * @param wayDisplayState _more_
     */
    protected void wayVisibilityChanged(WayDisplayState wayDisplayState) {
        if (forecastState == wayDisplayState) {
            showStorm();
        }
    }



    /**
     * _more_
     *
     * @param wayDisplayState _more_
     *
     * @throws Exception _more_
     */
    protected void wayParamChanged(WayDisplayState wayDisplayState)
            throws Exception {
        if (forecastState == wayDisplayState) {
            List<WayDisplayState> wayDisplayStates = getWayDisplayStates();
            for (WayDisplayState way : wayDisplayStates) {
                if (way.usingDefaultParam()) {
                    way.makeField();
                }
            }
        }
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void checkVisibility() throws Exception {
        List<WayDisplayState> wayDisplayStates = getWayDisplayStates();
        for (WayDisplayState wayDisplayState : wayDisplayStates) {
            wayDisplayState.checkVisibility();
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private StationModel getObservationStationModel() {
        StationModel model       = new StationModel("TrackLocation");
        ShapeSymbol  shapeSymbol = new ShapeSymbol(0, 0);
        shapeSymbol.setShape(ucar.visad.ShapeUtility.FILLED_CIRCLE);
        shapeSymbol.setScale(0.8f);
        shapeSymbol.bounds = new java.awt.Rectangle(-15, -15, 30, 30);
        shapeSymbol.setRectPoint(Glyph.PT_MM);
        model.addSymbol(shapeSymbol);

        return model;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private StationModel getForecastStationModel() {

        /*
          StationModelDisplayable dots  = new StationModelDisplayable("dots");
          wayDisplayState.addDisplayable(dots);
          StationModel model = new StationModel("TrackLocation");
          TextSymbol textSymbol = new TextSymbol("label","the label");
          textSymbol.setScale(1.5f);
          textSymbol.setRectPoint(Glyph.PT_UL);
          textSymbol.bounds = new java.awt.Rectangle(10,0,21,15);
          model.addSymbol(textSymbol);

          ShapeSymbol shapeSymbol = new ShapeSymbol(0, 0);
          shapeSymbol.setScale(0.5f);
          shapeSymbol.setShape(ucar.visad.ShapeUtility.CIRCLE);
          shapeSymbol.bounds = new java.awt.Rectangle(-15, -15, 30, 30);
          shapeSymbol.setRectPoint(Glyph.PT_MM);
          model.addSymbol(shapeSymbol);
          forecastHolder.addDisplayable(dots);
          dots.setScale(1.0f);
          dots.setStationModel(model);
          dots.setStationData(PointObFactory.makeTimeSequenceOfPointObs( wayDisplayState.getPointObs(),
          -1,-1));

        */
        return null;
    }




    //        ucar.visad.Util.makeTimeField(List<Data> ranges, List times)





    /** _more_ */
    private int cnt = 0;

    /**
     * _more_
     *
     * @param track _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected FieldImpl makeField(StormTrack track, RealType type)
            throws Exception {

        List<DateTime>      times     = track.getTrackTimes();
        List<EarthLocation> locations = track.getLocations();
        int                 numPoints = times.size();
        Unit                timeUnit  = ((DateTime) times.get(0)).getUnit();

        RealType dfltRealType = RealType.getRealType("Default_" + (cnt++));
        Real                dfltReal  = new Real(dfltRealType, 1);

        RealType timeType =
            RealType.getRealType(DataUtil.cleanName("track_time" + cnt + "_"
                + timeUnit), timeUnit);
        RealTupleType rangeType    = null;
        double[][]    newRangeVals = new double[2][numPoints];
        float[]       alts         = new float[numPoints];
        float[]       lats         = new float[numPoints];
        float[]       lons         = new float[numPoints];
        Real[]        values       = ((type == null)
                                      ? null
                                      : track.getTrackAttributeValues(type));
        for (int i = 0; i < numPoints; i++) {
            Real value = ((values == null)
                          ? dfltReal
                          : values[i]);
            //Set the dflt so we can use its unit later
            dfltReal = value;
            if (rangeType == null) {
                rangeType =
                    new RealTupleType(RealType.getRealType("trackrange_"
                        + cnt, value.getUnit()), timeType);
            }
            DateTime      dateTime = (DateTime) times.get(i);
            EarthLocation el       = locations.get(i);
            newRangeVals[0][i] = value.getValue();
            newRangeVals[1][i] = dateTime.getValue();
            lats[i]            = (float) el.getLatitude().getValue();
            lons[i]            = (float) el.getLongitude().getValue();
            alts[i]            = 1;
            //            if(Math.abs(lats[i])>90) System.err.println("bad lat:" + lats[i]);
        }
        GriddedSet llaSet = ucar.visad.Util.makeEarthDomainSet(lats, lons,
                                alts);
        Set[] rangeSets = new Set[2];
        rangeSets[0] = new DoubleSet(new SetType(rangeType.getComponent(0)));
        rangeSets[1] = new DoubleSet(new SetType(rangeType.getComponent(1)));
        FunctionType newType =
            new FunctionType(((SetType) llaSet.getType()).getDomain(),
                             rangeType);
        FlatField timeTrack = new FlatField(newType, llaSet,
                                            (CoordinateSystem) null,
                                            rangeSets,
                                            new Unit[] { dfltReal.getUnit(),
                timeUnit });
        timeTrack.setSamples(newRangeVals, false);
        return timeTrack;
    }



    /** Type for Azimuth */
    private final RealType azimuthType = RealType.getRealType("Azimuth",
                                             CommonUnit.degree);

    /**
     * _more_
     *
     * @param track _more_
     * @param wState _more_
     * @param type _more_
     *
     *
     * @throws Exception _more_
     */
    private void makeRingField(StormTrack track, WayDisplayState wState,
                               RealType type)
            throws Exception {
        //TODO: Use the param type
        type = STIStormDataSource.TYPE_RADIUSMODERATEGALE;
        List<EarthLocation> locations    = track.getLocations();
        int                 numPoints    = locations.size();
        List<RingSet>       rings        = new ArrayList<RingSet>();
        double[][]          newRangeVals = new double[2][numPoints];
        //TODO: Use a real type
        Real[] values = track.getTrackAttributeValues(type);
        if (values == null) {
            wState.setRings(null, null);
            return;
        }
        for (int i = 0; i < numPoints; i++) {
            if ((values[i] != null) && !values[i].isMissing()) {
                rings.add(makeRingSet(locations.get(i), values[i]));
            }
        }
        wState.setRings(type, rings);
    }


    /**
     * _more_
     *
     *
     *
     * @param el _more_
     * @param r _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private RingSet makeRingSet(EarthLocation el, Real r)
            throws VisADException, RemoteException {
        double lat = el.getLatitude().getValue();
        double lon = el.getLongitude().getValue();
        Radar2DCoordinateSystem r2Dcs =
            new Radar2DCoordinateSystem((float) lat, (float) lon);
        RealTupleType rtt = new RealTupleType((RealType) r.getType(),
                                azimuthType, r2Dcs, null);
        Color   ringColor = Color.gray;

        RingSet rss       = new RingSet("range rings", rtt, ringColor);
        // set initial spacing etc.
        rss.setRingValues(r, r);
        //        rss.setRingValues(
        //            new Real(rangeType, r, CommonUnit.meter.scale(1000)),
        //            new Real(rangeType, r, CommonUnit.meter.scale(1000)));
        rss.setVisible(true);

        /** width for range rings */
        float radialWidth = 1.f;

        rss.setLineWidth(radialWidth);

        return rss;

    }


    public void addChart() {
        StormTrackChart stormTrackChart = new  StormTrackChart(this, "Storm Chart");
        charts.add(stormTrackChart);
        tabbedPane.addTab(stormTrackChart.getName(),
                          stormTrackChart.getContents());
        stormTrackChart.updateChart();

    }

    /**
     * _more_
     */
    public void doit() {
        trackModel = new AbstractTableModel() {
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            public int getRowCount() {
                if (trackCollection == null) {
                    return 0;
                }
                return trackCollection.getTracks().size();
            }

            public int getColumnCount() {
                return 2;
            }

            public void setValueAt(Object aValue, int rowIndex,
                                   int columnIndex) {}

            public Object getValueAt(int row, int column) {

                if ((trackCollection == null)
                        || (row >= trackCollection.getTracks().size())) {
                    return "";
                }
                StormTrack track = trackCollection.getTracks().get(row);
                if (column == 0) {
                    return track.getWay();
                }
                return track.getTrackStartTime();
            }

            public String getColumnName(int column) {
                if (column == 0) {
                    return stormTrackControl.getWayName();
                }
                return "Date";
            }
        };


        trackTable = new JTable(trackModel);

        int width  = 300;
        int height = 400;
        JScrollPane scroller = GuiUtils.makeScrollPane(trackTable, width,
                                   height);
        scroller.setBorder(BorderFactory.createLoweredBevelBorder());
        scroller.setPreferredSize(new Dimension(width, height));
        scroller.setMinimumSize(new Dimension(width, height));

    }


    /**
     *  Set the StormInfo property.
     *
     *  @param value The new value for StormInfo
     */
    public void setStormInfo(StormInfo value) {
        stormInfo = value;
    }

    /**
     *  Get the StormInfo property.
     *
     *  @return The StormInfo
     */
    public StormInfo getStormInfo() {
        return stormInfo;
    }


    /**
     *  Set the Changed property.
     *
     *  @param value The new value for Changed
     */
    public void setChanged(boolean value) {
        changed = value;
    }

    /**
     *  Get the Changed property.
     *
     *  @return The Changed
     */
    public boolean getChanged() {
        return changed;
    }

    /**
     * Set the Active property.
     *
     * @param value The new value for Active
     */
    public void setActive(boolean value) {
        active = value;
    }

    /**
     * Get the Active property.
     *
     * @return The Active
     */
    public boolean getActive() {
        return active;
    }


    /**
     *  Set the WayDisplayStateMap property.
     *
     *  @param value The new value for WayDisplayStateMap
     */
    public void setWayDisplayStateMap(Hashtable<Way, WayDisplayState> value) {
        wayDisplayStateMap = value;
    }

    /**
     *  Get the WayDisplayStateMap property.
     *
     *  @return The WayDisplayStateMap
     */
    public Hashtable<Way, WayDisplayState> getWayDisplayStateMap() {
        return wayDisplayStateMap;
    }


    /**
     *  Set the ForecastState property.
     *
     *  @param value The new value for ForecastState
     */
    public void setForecastState(WayDisplayState value) {
        forecastState = value;
    }

    /**
     *  Get the ForecastState property.
     *
     *  @return The ForecastState
     */
    public WayDisplayState getForecastState() {
        return forecastState;
    }




    /**
     * Cycle through the color list.
     *
     *
     * @param nextColor _more_
     * @return The next color in the list
     */
    private static Color getNextColor(int[] nextColor) {
        if (nextColor[0] >= colors.length) {
            nextColor[0] = 0;
        }
        return colors[nextColor[0]++];
    }

    /**
     * Set the Charts property.
     *
     * @param value The new value for Charts
     */
    public void setCharts(List<StormTrackChart> value) {
        charts = value;
    }

    /**
     * Get the Charts property.
     *
     * @return The Charts
     */
    public List<StormTrackChart> getCharts() {
        return charts;
    }


}

