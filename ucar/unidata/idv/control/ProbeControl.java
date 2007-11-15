/*
 * $Id: ProbeControl.java,v 1.203 2007/08/13 21:37:40 dmurray Exp $
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





package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;

import ucar.unidata.geoloc.Bearing;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.ControlContext;

import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.control.chart.LineState;

import ucar.unidata.idv.control.chart.TimeSeriesChart;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.LatLonWidget;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.MidiProperties;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.ThreeDSize;

import ucar.unidata.util.TwoFacedObject;

import ucar.visad.ShapeUtility;

import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.PointProbe;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.SelectorPoint;

import visad.*;

import visad.data.units.Parser;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;



/**
 * A widget to display data values at one point in the 2d or 3d data field.
 * Can show several parameters' values at the point. Can choose method of
 * field sampling - nearest grid point value or weighted average.
 * Can change levels.
 *
 * @author Unidata IDV developers
 * @version $Revision: 1.203 $
 */
public class ProbeControl extends DisplayControlImpl {

    /** ID for sharing position */
    public static final String SHARE_POSITION = "ProbeControl.SHARE_POSITION";

    /** ID for sharing choices */
    public static final String SHARE_CHOICES = "ProbeControl.SHARE_CHOICES";

    /** ID for sharing sampling methods */
    public static final String SHARE_METHODS = "ProbeControl.SHARE_METHODS";

    /** ID for sharing levels */
    public static final String SHARE_LEVELS = "ProbeControl.SHARE_LEVELS";


    /** Column name property */
    public static final int COL_NAME = 0;

    /** Column value property */
    public static final int COL_VALUE = 1;

    /** Column value property */
    public static final int COL_EXTRA = 2;

    /** Column level property */
    public static final int COL_LEVEL = 3;

    /** Column sampling property */
    public static final int COL_SAMPLING = 4;

    /** number of columns */
    public static final int NUM_COLS = 5;

    /** The latlon widget */
    private LatLonWidget latLonWidget;

    /** The animation widget */
    private JComponent aniWidget;

    /** Is the axis fixed */
    private boolean xFixed = false;

    /** Is the axis fixed */
    private boolean yFixed = false;

    /** Is the axis fixed */
    private boolean zFixed = false;


    /** time label */
    private JLabel timeLabel;

    /** Last altitude we probed on */
    private Real lastProbeAltitude;

    /** list of infos */
    private List<ProbeRowInfo> infos = new ArrayList();

    /** list of levels */
    private List _levels;

    /** list of display units */
    private List _units;

    /** list of altitudes */
    private List _altitudes;

    /** list of VisAD sampling methods */
    private List _methods;

    /** list of sound properties */
    private List _sounds;


    /** list of times */
    private List times = new ArrayList();

    /** table for output */
    private JTable paramsTable;

    /** Holds the table */
    private JComponent tablePanel;

    /** Panel */
    JPanel panel;

    /** Are we currently exporting the table */
    private boolean amExporting = false;

    /** table model */
    private AbstractTableModel tableModel;

    /** the probe */
    private PointProbe probe;

    /** Keep around for the label macros */
    private String positionText;

    /** initial probe position */
    private RealTuple initPosition;

    /** initial location */
    private EarthLocation initLocation;

    /** the time data holder */
    private DisplayableData timeData;

    /** labels for sampling selections */
    private String[] samplingLabels = { WEIGHTED_AVERAGE, NEAREST_NEIGHBOR };

    /** How we display data */
    private String dataTemplate;

    /** The label to show the readout in the side legend */
    private JLabel sideLegendReadout;



    /** The point size */
    private float pointSize = 1.0f;

    /** The shape for the probe point */
    private String marker;


    /** time series chart */
    private TimeSeriesChart timeSeries;

    /** Show the table */
    private boolean showTable = true;

    /** Show the table in the legend */
    private boolean showTableInLegend = true;

    /** _more_          */
    private boolean showSunriseSunset = false;


    /**
     * Cstr; sets flags; see init() for creation actions.
     * needed for bean persistence
     */
    public ProbeControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_DATACONTROL);
    }

    /**
     * Set up new probe.
     *
     * @param choices a list of DataChoices
     * @return boolean true if succeeded
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(List choices) throws VisADException, RemoteException {
        //        System.err.println("probe init " + choices);
        if ((_levels != null) && (infos.size() == 0)) {
            //We have legacy muli-list table state
            for (int i = 0; i < _levels.size(); i++) {
                Real   level     = (Real) Misc.safeGet(_levels, i);
                Real   alt       = (Real) Misc.safeGet(_altitudes, i);
                Object method    = Misc.safeGet(_methods, i);
                int    theMethod = getDefaultSamplingModeValue();
                if (method != null) {
                    if (method instanceof TwoFacedObject) {
                        method = ((TwoFacedObject) method).getId();
                    }
                    theMethod = ((Integer) method).intValue();
                }
                Unit unit = (Unit) Misc.safeGet(_units, i);
                MidiProperties sound = (MidiProperties) Misc.safeGet(_sounds,
                                           i);
                infos.add(new ProbeRowInfo(level, alt, theMethod, unit,
                                           sound));
            }

        }




        ActionListener llListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                handleLatLonWidgetChange();
            }
        };
        latLonWidget = new LatLonWidget("Lat: ", "Lon: ", "Alt: ",
                                        llListener);


        timeLabel = new JLabel("   ");
        getAnimation(true);
        aniWidget = getAnimationWidget().getContents();

        probe     = new PointProbe(0.0, 0.0, 0.0);
        if (marker != null) {
            probe.setMarker(
                SelectorPoint.reduce(ShapeUtility.makeShape(marker)));
        }

        probe.setFixed(xFixed, yFixed, zFixed);
        probe.setAutoSize(true);
        probe.setVisible(true);
        probe.addPropertyChangeListener(this);
        if (initPosition != null) {
            probe.setPosition(initPosition);
        }
        if (initLocation != null) {
            setEarthLocation(initLocation);
        }
        addDisplayable(probe, FLAG_COLOR);
        setContents(doMakeContents());
        //        probe.setPointSize(pointSize);
        if (pointSize != 1.0f) {
            probe.setPointSize(pointSize);
        } else {
            probe.setPointSize(getDisplayScale());
        }
        return true;
    }



    /**
     * Called after init.
     */
    public void initDone() {
        try {
            super.initDone();
            if ((initPosition == null) && (initLocation == null)) {
                double[] screenCenter = getScreenCenter();
                probe.setPosition(
                    new RealTuple(
                        RealTupleType.SpatialCartesian3DTuple,
                        new double[] { screenCenter[0],
                                       screenCenter[1], 0.0 }));


            }
            setTimesForAnimation();
            updatePosition();
            doMoveProbe();
        } catch (Exception exc) {
            logException("In init done", exc);
        }
    }

    /**
     * Return the label that is to be used for the color widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the color widget
     */
    public String getColorWidgetLabel() {
        return "Probe Color";
    }

    /**
     * Make the view menu items
     *
     * @param items List of menu items
     * @param forMenuBar  forMenuBar
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        super.getViewMenuItems(items, forMenuBar);
        items.add(GuiUtils.MENU_SEPARATOR);

        List paramItems = new ArrayList();
        paramItems.add(GuiUtils.makeCheckboxMenuItem("Show Parameter Table",
                this, "showTable", null));
        paramItems.add(
            GuiUtils.makeCheckboxMenuItem(
                "Show Readout In Legend", this, "showTableInLegend", null));
        paramItems.add(doMakeChangeParameterMenuItem());
        List choices = getDataChoices();
        for (int i = 0; i < choices.size(); i++) {
            paramItems.addAll(getParameterMenuItems(i));
        }


        items.add(GuiUtils.makeMenu("Parameters", paramItems));

        JMenu chartMenu = new JMenu("Chart");
        items.add(chartMenu);

        chartMenu.add(
            GuiUtils.makeCheckboxMenuItem(
                "Show Thumbnail in Legend", getChart(), "showThumb", null));
        chartMenu.add(
            GuiUtils.makeCheckboxMenuItem(
                "Show Sunrise/Sunset Times", this, "showSunriseSunset",
                null));
        List chartMenuItems = new ArrayList();
        getChart().addViewMenuItems(chartMenuItems);
        GuiUtils.makeMenu(chartMenu, chartMenuItems);

        JMenu probeMenu = new JMenu("Probe");
        items.add(probeMenu);
        JMenu posMenu = new JMenu("Position");
        probeMenu.add(posMenu);
        posMenu.add(GuiUtils.makeMenuItem("Reset Probe Position", this,
                                          "resetProbePosition"));
        posMenu.add(GuiUtils.makeCheckboxMenuItem("Lock X Axis", this,
                "xFixed", null));
        posMenu.add(GuiUtils.makeCheckboxMenuItem("Lock Y Axis", this,
                "yFixed", null));
        posMenu.add(GuiUtils.makeCheckboxMenuItem("Lock Z Axis", this,
                "zFixed", null));
        probeMenu.add(doMakeChangeColorMenu("Color"));

        JMenu sizeMenu = new JMenu("Size");
        probeMenu.add(sizeMenu);

        sizeMenu.add(GuiUtils.makeMenuItem("Increase", this,
                                           "increaseProbeSize"));
        sizeMenu.add(GuiUtils.makeMenuItem("Decrease", this,
                                           "decreaseProbeSize"));

        JMenu shapeMenu = new JMenu("Probe Shape");
        probeMenu.add(shapeMenu);
        for (int i = 0; i < ShapeUtility.SHAPES.length; i++) {
            TwoFacedObject tof = ShapeUtility.SHAPES[i];
            String         lbl = tof.toString();
            if (Misc.equals(tof.getId(), marker)) {
                lbl = ">" + lbl;
            }
            JMenuItem mi = GuiUtils.makeMenuItem(lbl, this, "setMarker",
                               tof.getId());
            shapeMenu.add(mi);
        }
        GuiUtils.limitMenuSize(shapeMenu, "Shape Group ", 10);

    }


    /**
     * A hook to allow derived classes to tell us to add this
     * as an animation listener
     *
     * @return Add as animation listener
     */
    protected boolean shouldAddAnimationListener() {
        return true;
    }


    /**
     * Set the earth location
     *
     * @param el  the earth location
     */
    public void setEarthLocation(EarthLocation el) {
        try {
            if (probe == null) {
                initLocation = el;
                return;
            }
            double[] xyz = earthToBox(el);
            resetProbePosition(xyz[0], xyz[1], xyz[2]);
        } catch (Exception exc) {
            logException("Error setting probe position", exc);
        }
    }



    /**
     * Add display settings for this control
     *
     * @param dsd  dialog to add to
     */
    protected void addDisplaySettings(DisplaySettingsDialog dsd) {
        try {
            dsd.addPropertyValue(new Boolean(getChart().getShowThumb()),
                                 "showThumbNail", "Show Thumbnail", "Probe");

            dsd.addPropertyValue(getEarthLocationFromWidget(),
                                 "earthLocation", "Probe Position", "Probe");

            dsd.addPropertyValue(getInfos(), "infos", "Probe parameters",
                                 "Probe");

        } catch (Exception exc) {
            logException("Error getting location", exc);
        }
        super.addDisplaySettings(dsd);
    }



    /**
     * Get earth location from the lat/lon widget
     *
     * @return the earth location
     *
     * @throws RemoteException  problem getting remote data
     * @throws VisADException   problem getting local data
     */
    private EarthLocation getEarthLocationFromWidget()
            throws VisADException, RemoteException {
        double lat = latLonWidget.getLat();
        double lon = latLonWidget.getLon();
        double alt = latLonWidget.getAlt();
        return makeEarthLocation(lat, lon, alt);
    }

    /**
     * Update the lat/lon widget with the specified earth location
     *
     * @param elt the new earth location
     */
    private void updateLatLonWidget(EarthLocation elt) {
        if (latLonWidget == null) {
            return;
        }
        LatLonPoint llp = elt.getLatLonPoint();
        latLonWidget.setLat(
            getDisplayConventions().formatLatLon(
                llp.getLatitude().getValue()));
        latLonWidget.setLon(
            getDisplayConventions().formatLatLon(
                llp.getLongitude().getValue()));
        latLonWidget.setAlt(
            getDisplayConventions().formatAltitude(elt.getAltitude()));
    }


    /**
     * Handle the user pressing return
     */
    private void handleLatLonWidgetChange() {
        try {
            setEarthLocation(getEarthLocationFromWidget());
        } catch (Exception exc) {
            logException("Error setting lat/lon", exc);
        }
    }



    /**
     * Reset the position of the probe to the center.
     */
    public void resetProbePosition() {
        resetProbePosition(0.0, 0.0, 0.0);
    }

    /**
     * Reset the position of the probe to the center.
     *
     * @param lat lat
     * @param lon lon
     * @param alt alt
     */
    public void resetProbePosition(double lat, double lon, double alt) {
        try {
            if (probe == null) {
                return;
            }
            probe.setPosition(
                new RealTuple(
                    RealTupleType.SpatialCartesian3DTuple, new double[] { lat,
                    lon, alt }));
        } catch (Exception exc) {
            logException("Resetting probe position", exc);
        }
    }


    /**
     * Get edit menu items
     *
     * @param items      list of menu items
     * @param forMenuBar  true if for the menu bar
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        JMenuItem mi;
        items.add(mi = new JMenuItem("Change Display Format..."));
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String newFormat =
                    GuiUtils.getInput(
                        "Enter a new value readout format:", " Format: ",
                        dataTemplate, null,
                        " (Use HTML with %value%, %unit%, %rawvalue%, %rawunit%)",
                        "Change Display Format");
                if (newFormat != null) {
                    dataTemplate = newFormat;
                    doMoveProbe();
                }
            }
        });

        super.getEditMenuItems(items, forMenuBar);
    }





    /**
     * Method called by other classes that share the state
     *
     * @param from  other class.
     * @param dataId  type of sharing
     * @param data  Array of data being shared.  In this case, the first
     *              (and only?) object in the array is the level
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        if ((probe != null) && dataId.equals(SHARE_POSITION)) {
            try {
                if (data[0] instanceof EarthLocationTuple) {
                    EarthLocationTuple elt = (EarthLocationTuple) data[0];
                    data[0] = earthToBoxTuple(elt);
                }
                probe.setPosition((RealTuple) data[0]);
            } catch (Exception exc) {
                logException("receiveShareData.position", exc);
            }
            return;
        }

        if (dataId.equals(SHARE_CHOICES)) {
            try {
                processNewData((List) data[0]);
            } catch (Exception exc) {
                logException("receiveShareData.data", exc);
            }
            return;
        }


        if (dataId.equals(SHARE_METHODS)) {
            try {
                //                methods = new ArrayList((List) data[0]);
                doMoveProbe();
                fireStructureChanged();
            } catch (Exception exc) {
                logException("receiveShareData.data", exc);
            }
            return;
        }


        if (dataId.equals(SHARE_LEVELS)) {
            try {
                doMoveProbe();
                fireStructureChanged();
            } catch (Exception exc) {
                logException("receiveShareData.data", exc);
            }
            return;
        }

        super.receiveShareData(from, dataId, data);
    }





    /**
     * Override base class method to use the list of data choices to
     * get the long parameter name
     *
     * @return The String to be used for the long parameter name
     */

    protected String getLongParamName() {
        String paramName = "Params: ";

        List   choices   = getDataChoices();
        if (choices == null) {
            System.err.println("Probe data choices are null");
            return "";
        }
        for (int i = 0; i < choices.size(); i++) {
            ProbeRowInfo rowInfo = getRowInfo(i);
            if (i > 0) {
                paramName += ", ";
            }
            paramName += rowInfo.getDataInstance().getDataChoice().getName();
            if (i > 3) {
                break;
            }
        }
        if (choices.size() > 3) {
            paramName += ", ...";
        }
        return paramName;
    }

    /**
     * Respond to a change in the display's projection.  In this case
     * we resample at the new location. (move probe)
     */
    public void projectionChanged() {
        super.projectionChanged();
        doMoveProbe();
    }

    /**
     * Add any macro name/label pairs
     *
     * @param names List of macro names
     * @param labels List of macro labels
     */
    protected void getMacroNames(List names, List labels) {
        super.getMacroNames(names, labels);
        names.addAll(Misc.newList(MACRO_POSITION));
        labels.addAll(Misc.newList("Probe Position"));
    }

    /**
     * Add any macro name/value pairs.
     *
     *
     * @param template template
     * @param patterns The macro names
     * @param values The macro values
     */
    protected void addLabelMacros(String template, List patterns,
                                  List values) {
        super.addLabelMacros(template, patterns, values);
        patterns.add(MACRO_POSITION);
        values.add(positionText);
    }



    /**
     * Override base class method to just trigger a redisplay of the data.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void resetData() throws VisADException, RemoteException {
        clearCachedSamples();
        updateLegendLabel();
        setTimesForAnimation();
        doMoveProbe();
        fireStructureChanged();
    }


    /**
     * Set the times for animation
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setTimesForAnimation()
            throws VisADException, RemoteException {
        Set myTimes = calculateTimeSet();
        if (myTimes == null) {
            return;
        }
        /*  We used to merge with main display, not just have our own
        Animation animation = getAnimation();
        Set       aniSet    = animation.getSet();
        if (aniSet == null) {
            animation.setSet(myTimes);
        } else if ( !aniSet.equals(myTimes)) {
            myTimes = aniSet.merge1DSets(myTimes);
            animation.setSet(myTimes);
        }
        */
        getAnimationWidget().setBaseTimes(myTimes);
    }

    /**
     * Override base class method which is called when the user has selected
     * new data choices.
     *
     * @param newChoices  new list of choices
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void addNewData(List newChoices)
            throws VisADException, RemoteException {
        processNewData(newChoices);
        doShare(SHARE_CHOICES, newChoices);
    }



    /**
     * Copy the data choice at the given row.
     *
     * @param row The table row that holds the parameter to copy
     */
    private void copyParameter(int row) {
        try {
            DataChoice dc = (DataChoice) getDataChoiceAtRow(row);
            if (dc == null) {
                return;
            }
            ProbeRowInfo oldInfo = getRowInfo(row);
            dc = dc.createClone();
            appendDataChoices(Misc.newList(dc));
            List choices = getDataChoices();
            //This should force the creation of a new one
            getRowInfo(choices.size() - 1).initWith(oldInfo);
            resetData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Override base class method which is called when the user has selected
     * new data choices.
     *
     * @param newChoices    new list of choices
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void processNewData(List newChoices)
            throws VisADException, RemoteException {
        List<ProbeRowInfo> newInfos = new ArrayList<ProbeRowInfo>();
        showWaitCursor();
        for (int i = 0; i < newChoices.size(); i++) {
            ProbeRowInfo info = new ProbeRowInfo(this);
            newInfos.add(info);
            DataChoice dc = (DataChoice) newChoices.get(i);
            initRowInfo(info, dc);
        }
        showNormalCursor();
        appendDataChoices(newChoices);
        infos.addAll(newInfos);
        resetData();
    }

    /**
     * Assume that any display controls that have a color table widget
     * will want the color table to show up in the legend.
     *
     * @param  legendType  type of legend
     * @return The extra JComponent to use in legend
     */
    protected JComponent getExtraLegendComponent(int legendType) {
        JComponent parentComp = super.getExtraLegendComponent(legendType);
        if (legendType == BOTTOM_LEGEND) {
            return parentComp;
        }
        if (sideLegendReadout == null) {
            sideLegendReadout = new JLabel("<html><br></html>");
        }
        return GuiUtils.vbox(parentComp, sideLegendReadout,
                             getChart().getThumb());
    }

    /**
     * Append any label information to the list of labels.
     *
     * @param labels   in/out list of labels
     * @param legendType The type of legend, BOTTOM_LEGEND or SIDE_LEGEND
     */
    public void getLegendLabels(List labels, int legendType) {
        super.getLegendLabels(labels, legendType);
        labels.add(positionText);
    }

    /**
     * Create a merged time set from the DataChoices.
     *
     * @return merged set or null
     */
    private Set calculateTimeSet() {
        List choices = getDataChoices();
        if (choices.isEmpty()) {
            return null;
        }
        Set newSet = null;
        for (int i = 0; i < choices.size(); i++) {
            try {
                ProbeRowInfo info = getRowInfo(i);
                /*                Data         data = info.getDataInstance().getData();
                if ( !(data instanceof FieldImpl)) {
                    continue;
                }
                Set set = GridUtil.getTimeSet((FieldImpl) data);*/
                Set set = info.getTimeSet();
                if (set != null) {
                    if (newSet == null) {
                        newSet = set;
                    } else {
                        newSet = newSet.merge1DSets(set);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //System.out.println("merged time set = " + newSet);
        return newSet;
    }

    /**
     * Set the probe position property; used by XML persistence.
     *
     * @param p    probe position
     */
    public void setPosition(RealTuple p) {
        initPosition = p;
    }

    /**
     * Set the probe position property; used by XML persistence.
     *
     * @return  probe position - may be <code>null</code>.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public RealTuple getPosition() throws VisADException, RemoteException {
        return ((probe != null)
                ? probe.getPosition()
                : null);
    }

    /**
     * Remove this display.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doRemove() throws RemoteException, VisADException {
        probe      = null;
        tableModel = null;
        super.doRemove();
    }

    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        try {
            updateTime();
            getChart().timeChanged();
        } catch (Exception exc) {
            logException("changePosition", exc);
        }
        super.timeChanged(time);
    }




    /**
     * get the image for what
     *
     * @param what  the thing to get
     *
     * @return an image
     *
     * @throws Exception problem (can this be more specific?)
     */
    public Image getImage(String what) throws Exception {
        if ((what != null) && what.equals("chart")) {
            setMainPanelDimensions();
            return ImageUtils.getImage(getChart().getContents());
        }
        return super.getImage(what);
    }




    /** Not used for now */
    private boolean updatePending = false;


    /**
     * Property change method.
     *
     * @param evt   event to act on
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ( !getHaveInitialized() || !getActive()) {
            return;
        }
        if (evt.getPropertyName().equals(
                SelectorDisplayable.PROPERTY_POSITION)) {
            try {
                RealTuple position = getPosition();
                if (updatePending) {
                    return;
                }
                updatePending = true;
                updatePosition();
                //                updatePosition(position);
                //                Misc.runInABit(1000,this,"updatePosition",null);
                doShare(SHARE_POSITION, position);
            } catch (Exception exc) {
                logException("Probe moved", exc);
            }
        } else {
            super.propertyChange(evt);
        }
    }


    /**
     * Get the list of levels for a particular parameter (row)
     *
     * @param row  row for parameter
     * @return   list of levels
     */
    public Real[] getLevelsAtRow(int row) {
        ProbeRowInfo rowInfo = getRowInfo(row);
        if ( !rowInfo.isGrid()) {
            return null;
        }
        GridDataInstance gdi = rowInfo.getGridDataInstance();
        return gdi.getLevels();
    }


    /**
     * see if there are levels assigned to this row (parameter);
     * some data is 2d and has no levels.
     *
     * @param row   row to check
     * @return true if there are levels assigned to this row (parameter)
     */
    public boolean haveLevelsAtRow(int row) {
        Real[] levelArray = getLevelsAtRow(row);
        if ((levelArray == null) || (levelArray.length <= 1)) {
            return false;
        }
        return true;
    }



    /**
     * _more_
     *
     * @param param _more_
     */
    public void changePointParameter(Object[] param) {
        try {
            ProbeRowInfo rowInfo = (ProbeRowInfo) param[0];
            String       name    = (String) param[1];
            rowInfo.setPointParameter(name);
            doMoveProbe();
        } catch (Exception exc) {
            logException("Changing parameter", exc);
        }
    }

    /**
     * Get the menu items for the given row
     *
     * @param row the row
     *
     * @return menu items
     */
    private List getParameterMenuItems(final int row) {
        List               items   = new ArrayList();
        final ProbeRowInfo rowInfo = getRowInfo(row);

        JMenu paramMenu = new JMenu("Parameter " + getFieldName(row));
        items.add(paramMenu);
        JMenuItem jmi;

        if ( !rowInfo.isGrid()) {
            try {
                TupleType t = rowInfo.getTupleType();
                if (t != null) {
                    List subItems = new ArrayList();
                    for (int i = 0; i < t.getDimension(); i++) {
                        if ( !(t.getComponent(i) instanceof RealType)) {
                            continue;
                        }
                        String name = t.getComponent(i).toString();
                        subItems.add(GuiUtils.makeMenuItem(name, this,
                                "changePointParameter",
                                new Object[] { rowInfo,
                                name }));
                    }
                    if (subItems.size() > 0) {
                        paramMenu.add(GuiUtils.makeMenu("Point Parameter",
                                subItems));
                    }
                }
            } catch (Exception exc) {
                logException("Changing parameter", exc);
            }
        }



        jmi = new JMenuItem("Copy");
        paramMenu.add(jmi);
        jmi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                copyParameter(row);
            }
        });

        paramMenu.add(GuiUtils.makeMenuItem("Chart Properties",
                                            ProbeControl.this,
                                            "showLineProperties", rowInfo));


        // change unit choice
        jmi = new JMenuItem("Change Unit...");
        paramMenu.add(jmi);
        jmi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Unit newUnit = getDisplayConventions().selectUnit(
                                   getRowInfo(row).getUnit(), null);
                if (newUnit != null) {
                    getRowInfo(row).setUnit(newUnit);
                    try {
                        updatePosition();
                    } catch (Exception exc) {
                        logException("After changing units", exc);
                    }
                }

            }
        });


        // change unit choice
        paramMenu.add(GuiUtils.makeMenuItem("Set Sound...",
                                            ProbeControl.this,
                                            "showSoundDialog",
                                            getRowInfo(row)));

        // Remove this parameter
        jmi = new JMenuItem("Remove");
        paramMenu.add(jmi);
        jmi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                removeField(row);
                updateLegendLabel();
            }
        });

        return items;
    }


    /**
     * Make the UI for this display control.
     *
     * @return  UI contents.
     */
    public Container doMakeContents() {

        tableModel = new AbstractTableModel() {

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                ProbeRowInfo rowInfo = getRowInfo(rowIndex);
                if (columnIndex == COL_LEVEL) {
                    return haveLevelsAtRow(rowIndex);
                }
                if (columnIndex == COL_SAMPLING) {
                    return true;
                }
                return false;
            }

            public int getRowCount() {
                List dataChoices = getDataChoices();
                if (dataChoices == null) {
                    return 0;
                }
                return dataChoices.size();
            }

            public int getColumnCount() {
                return NUM_COLS;
            }

            public void setValueAt(Object aValue, int rowIndex,
                                   int columnIndex) {
                ProbeRowInfo rowInfo = getRowInfo(rowIndex);
                if (columnIndex == COL_LEVEL) {
                    if ( !rowInfo.isGrid()) {
                        return;
                    }
                    Real r = null;
                    if (aValue instanceof Real) {
                        r = (Real) aValue;
                    } else if (aValue instanceof TwoFacedObject) {
                        r = (Real) ((TwoFacedObject) aValue).getId();
                    }
                    rowInfo.setLevel(r);
                    if (r != null) {
                        rowInfo.setAltitude(
                            getAltitudeAtLevel(
                                rowInfo.getGridDataInstance(), r));
                    } else {
                        rowInfo.setAltitude(null);
                    }

                    try {
                        updatePosition();
                    } catch (Exception exc) {
                        logException("After changing levels", exc);
                    }
                    //doShare(SHARE_LEVELS, new Object[]{ levels, altitudes });
                    return;
                }
                if (columnIndex == COL_NAME) {
                    rowInfo.setPointParameter(aValue.toString());
                    return;
                }

                if (columnIndex == COL_SAMPLING) {
                    rowInfo.setSamplingMode(
                        getSamplingModeValue(aValue.toString()));
                    doMoveProbe();
                    //                    doShare(SHARE_METHODS, methods);
                    return;
                }
            }

            public Object getValueAt(int row, int column) {
                if (column == COL_NAME) {
                    return getFieldName(row);
                }
                if (column == COL_VALUE) {
                    if (row < getDataChoices().size()) {
                        if (amExporting) {
                            Data raw = getRowInfo(row).getTimeSample();
                            if (raw == null) {
                                return "missing";
                            }
                            if (raw instanceof Real) {
                                return raw;
                            }
                            RealTuple rt = (RealTuple) raw;
                            try {
                                return rt.getComponent(0);
                            } catch (Exception exc) {
                                return null;
                            }
                        } else {
                            return getRowInfo(row).getDisplayValue();
                        }
                    }
                }
                if (column == COL_EXTRA) {
                    return getRowInfo(row).getExtra();
                }
                if (column == COL_LEVEL) {
                    if ( !haveLevelsAtRow(row)) {
                        return "--";
                    }
                    Real level = getRowInfo(row).getLevel();
                    Real alt   = getRowInfo(row).getAltitude();
                    if ((level == null) || (alt == null)) {
                        if (lastProbeAltitude != null) {
                            return (amExporting
                                    ? ""
                                    : "Probe: ") + getDisplayConventions()
                                        .formatAltitude(lastProbeAltitude);
                        } else {
                            return "";
                        }
                    }
                    return Util.labeledReal(level);
                }
                if (column == COL_SAMPLING) {
                    return getSamplingModeName(
                        getRowInfo(row).getSamplingMode());
                }
                return "";
            }

            public String getColumnName(int column) {

                switch (column) {

                  case COL_NAME :
                      return "Parameter";

                  case COL_VALUE :
                      return "Value";

                  case COL_EXTRA :
                      return "Min/Max/Avg";

                  case COL_LEVEL :
                      return "Level";

                  case COL_SAMPLING :
                      return "Sampling";
                }

                return "";
            }
        };



        paramsTable = new JTable(tableModel);


        paramsTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {

                    removeField(paramsTable.getSelectedRow());
                }
            }
        });


        paramsTable.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {

                if ( !SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                final int row = paramsTable.rowAtPoint(e.getPoint());
                if ((row < 0) || (row >= getDataChoices().size())) {
                    return;
                }



                List       choices   = getDataChoices();
                JPopupMenu popupMenu = new JPopupMenu();
                JMenuItem  jmi       = doMakeChangeParameterMenuItem();
                popupMenu.add(jmi);
                popupMenu.addSeparator();
                List items = getParameterMenuItems(row);
                GuiUtils.makePopupMenu(popupMenu, items);
                /*
                JMenu moveMenu = JMenu("Order");
                popupMenu.add(moveMenu);
                if(row!=0) {
                    JMenuItem mi = new JMenuItem("Move Up");
                    moveMenu.add(mi);
                    mi.addActionListener(new ObjectListener(new Integer(row)) {
                    public void actionPerformed(ActionEvent ev) {
                        Object o = infos.get(row);
                        infos.remove(row);
                        infos.infos.add(row-1,o);
                    }
                });

                }
                if(row<choices.size()-1) {
                    JMenuItem mi = new JMenuItem("Move Down");
                    moveMenu.add(mi);

                }
                */


                // Display choices
                JMenu dataChoiceMenu =
                    getControlContext().doMakeDataChoiceMenu(
                        getDataChoiceAtRow(row));
                popupMenu.add(dataChoiceMenu);
                popupMenu.show(paramsTable, e.getX(), e.getY());

            }

        });
        paramsTable.setToolTipText("Right click to edit");

        JScrollPane scrollPane = new JScrollPane(paramsTable);

        paramsTable.getColumnModel().getColumn(COL_LEVEL).setCellEditor(
            new LevelEditor());

        paramsTable.getColumnModel().getColumn(COL_SAMPLING).setCellEditor(
            new SamplingEditor());

        DefaultTableCellRenderer cellRenderer =
            new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        paramsTable.getColumnModel().getColumn(COL_VALUE).setCellRenderer(
            cellRenderer);
        paramsTable.getColumnModel().getColumn(COL_EXTRA).setCellRenderer(
            cellRenderer);
        paramsTable.getColumnModel().getColumn(COL_LEVEL).setCellRenderer(
            cellRenderer);


        //        paramsTable.setPreferredSize(new Dimension(450, 100));
        scrollPane.setPreferredSize(new Dimension(450, 100));

        JTableHeader header = paramsTable.getTableHeader();
        tablePanel = new JPanel();
        tablePanel.setVisible(showTable);
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.add(scrollPane);
        scrollPane.setPreferredSize(new Dimension(300, 100));

        if (timeSeries != null) {
            timeSeries.setControl(this);
        }
        //getChart().setEmptyChartLabel("Right click on observation in table to add to chart");


        JTabbedPane tab         = new JTabbedPane();
        List        bottomComps = new ArrayList();
        JComponent  bottomPanel = GuiUtils.leftRight(aniWidget, latLonWidget);

        bottomPanel = GuiUtils.inset(bottomPanel, 5);
        JComponent bottom = GuiUtils.centerBottom(tablePanel, bottomPanel);


        //        JSplitPane split = GuiUtils.vsplit(getChart().getContents(), bottom,
        //                                           0.75);
        //        split.setOneTouchExpandable(true);
        //        return split;
        return GuiUtils.centerBottom(getChart().getContents(), bottom);

        //        return GuiUtils.centerBottom(getChart().getContents(), bottom);
    }  // end domakecontents



    /**
     * Popup the data dialog; override superclass to allow multiple
     * selections.
     *
     * @param dialogMessage the dialog message
     * @param from   component to latch on to
     * @param multiples  true to support multiple selections
     * @param categories  data categories of params to show
     */
    protected void popupDataDialog(final String dialogMessage,
                                   Component from, boolean multiples,
                                   List categories) {
        super.popupDataDialog(dialogMessage, from, true, categories);
    }



    /**
     * Show the properties dialog for the chart line
     *
     * @param rowInfo The chrt entry to show properties for
     */
    public void showLineProperties(ProbeRowInfo rowInfo) {
        PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    ProbeControl.this.updatePosition();
                } catch (Exception exc) {
                    logException("Updating position", exc);
                }
            }
        };
        LineState lineState = rowInfo.getLineState();
        lineState.showPropertiesDialog(listener, getChart().getPlotNames(),
                                       getChart().getCurrentRanges());
    }


    /**
     * Show the sound dialog for the row
     *
     * @param info the row
     */
    public void showSoundDialog(ProbeRowInfo info) {
        try {
            info.showSoundDialog(this);
        } catch (Exception exc) {
            logException("shoing sound dialog", exc);
        }
    }


    /**
     * Class LevelEditor, used for selecting levels in table column
     */
    public class LevelEditor extends DefaultCellEditor {

        /**
         * New editor, create as a combo box
         */
        public LevelEditor() {
            super(new JComboBox());
        }

        /**
         * Get the component for editing the levels
         *
         * @param table           the JTable
         * @param value           the value
         * @param isSelected      flag for selection
         * @param rowIndex        row index
         * @param vColIndex       column index.
         * @return   the editing component
         */
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int rowIndex,
                int vColIndex) {
            JComboBox box    = (JComboBox) getComponent();
            Object[]  levels =
                formatLevels((Real[]) getLevelsAtRow(rowIndex));
            List      ll     = Misc.toList(levels);
            ll.add(0, "Probe's");
            GuiUtils.setListData(box, ll.toArray());
            if (value instanceof Real) {
                value = Util.labeledReal((Real) value);
            }
            box.setSelectedItem(value);
            return box;
        }
    }


    /**
     * If user clicks on the "sampling" column, a popup menu appears
     * with choices for the grid value sampling method.
     *
     */
    public class SamplingEditor extends DefaultCellEditor {

        /**
         * The sampling mode editor
         *
         */
        public SamplingEditor() {
            super(new JComboBox());
        }

        /**
         * Get the component for editing the sampling methods
         *
         * @param table           the JTable
         * @param value           the value
         * @param isSelected      flag for selection
         * @param rowIndex        row index
         * @param vColIndex       column index.
         * @return   the editing component
         */
        public Component getTableCellEditorComponent(JTable table,
                Object value, boolean isSelected, int rowIndex,
                int vColIndex) {
            JComboBox box = (JComboBox) getComponent();
            GuiUtils.setListData(box, samplingLabels);
            box.setSelectedItem(value);
            return box;
        }
    }


    /**
     * Remove a parameter
     *
     * @param row  row to remove
     */
    private void removeField(int row) {
        if (row < 0) {
            return;
        }
        ProbeRowInfo info = getRowInfo(row);
        DataInstance di   = info.getDataInstance();
        if (di != null) {
            removeDataChoice(di.getDataChoice());
        }
        infos.remove(row);
        try {
            setTimesForAnimation();
        } catch (Exception e) {
            logException("Error updating times: ", e);
        }
        fireStructureChanged();
        doMoveProbe();  // update the side legend label if needed
    }

    /**
     * Called to reset the table structure after a change
     */
    private void fireStructureChanged() {

        tableModel.fireTableStructureChanged();
        paramsTable.getColumnModel().getColumn(COL_SAMPLING).setCellEditor(
            new SamplingEditor());
        paramsTable.getColumnModel().getColumn(COL_LEVEL).setCellEditor(
            new LevelEditor());

        DefaultTableCellRenderer cellRenderer =
            new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        paramsTable.getColumnModel().getColumn(COL_VALUE).setCellRenderer(
            cellRenderer);
        paramsTable.getColumnModel().getColumn(COL_EXTRA).setCellRenderer(
            cellRenderer);
        paramsTable.getColumnModel().getColumn(COL_LEVEL).setCellRenderer(
            cellRenderer);
    }

    /**
     * Get the DataChoice associated with the parameter at a particular
     * row.
     *
     * @param row    row index
     * @return   the associated DataChoice
     */
    public DataChoice getDataChoiceAtRow(int row) {
        List choices = getDataChoices();
        if (row >= choices.size()) {
            return null;
        }
        return (DataChoice) choices.get(row);
    }


    /**
     * Get the field name (parameter) at a particular row
     *
     * @param row  row index
     * @return   name of the parameter
     */
    String getFieldName(int row) {
        ProbeRowInfo rowInfo = getRowInfo(row);
        if (rowInfo.isPoint()) {
            return rowInfo.getPointParameter() + "@"
                   + rowInfo.getStationName();
        }
        return rowInfo.getDataInstance().getDataChoice().getName();
    }


    /**
     * Return the appropriate label text for the menu.
     * @return  the label text
     */
    protected String getChangeParameterLabel() {
        return "Add Parameter...";
    }

    /**
     *  Gets called when the user has moved with the display.
     *  This is a wrapper around updatePosition, catching any exceptions.
     */
    private void doMoveProbe() {
        if ( !getHaveInitialized()) {
            return;
        }
        try {
            updatePosition();
        } catch (Exception exc) {
            logException("changePosition", exc);
        }
    }

    /**
     * Make new values in data probe display (readout table)
     * to match chages in location or sampling of data.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void updatePosition() throws VisADException, RemoteException {
        updatePosition(probe.getPosition());
    }

    /**
     * Apply the preferences.  Used to pick up the date format changes.
     */
    public void applyPreferences() {
        super.applyPreferences();
        try {
            dataTemplate = null;
            updatePosition();
        } catch (Exception exc) {
            logException("applyPreferences", exc);
        }
    }


    /**
     * Populate the units array with units
     *
     * @param reals List of real data values
     */
    private void initRowUnits(List reals) {
        /*
        for (int row = 0; row < reals.size(); row++) {
            Real real = (Real) reals.get(row);
            GridDataInstance dataInstance =
                (GridDataInstance) dataInstances.get(row);

        }
        */
    }


    /**
     * Make new values in data probe display (readout table)
     * to match chages in location or sampling of data.
     *
     * @param position   probe position
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void updatePosition(RealTuple position)
            throws VisADException, RemoteException {

        updatePending = false;
        if ( !getHaveInitialized()) {
            return;
        }

        double[] positionValues = position.getValues();
        EarthLocationTuple elt =
            (EarthLocationTuple) boxToEarth(new double[] { positionValues[0],
                positionValues[1], positionValues[2] }, false);
        LatLonPoint llp = elt.getLatLonPoint();
        lastProbeAltitude = elt.getAltitude();

        // set location labels
        if (llp != null) {
            positionText = getDisplayConventions().formatLatLonPoint(llp);
            if (latLonWidget != null) {
                updateLatLonWidget(elt);
            }
        }

        updateTime();
        List<ProbeRowInfo> rowInfos = new ArrayList();
        List               choices  = getDataChoices();
        for (int i = 0; i < choices.size(); i++) {
            rowInfos.add(getRowInfo(i));
        }
        if (showSunriseSunset) {
            getChart().setLocation(ucar.visad.Util.toLatLonPoint(llp));
        } else {
            getChart().setLocation(null);
        }
        getChart().setProbeSamples(rowInfos);
    }


    /**
     * Resample at the current time
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void updateTime() throws VisADException, RemoteException {

        if ( !getHaveInitialized() || !getActive()) {
            return;
        }
        Animation animation = getAnimation(true);
        int       step      = (animation != null)
                              ? animation.getCurrent()
                              : 0;
        Real      aniValue  = ((animation != null)
                               ? animation.getAniValue()
                               : null);

        try {
            setData(aniValue, step);
        } catch (Exception exc) {
            logException("Updating time", exc);
        }

        StringBuffer sideText = new StringBuffer();
        if (dataTemplate == null) {
            dataTemplate = getObjectStore().get(PREF_PROBEFORMAT,
                    DEFAULT_PROBEFORMAT);
        }
        dataTemplate = dataTemplate.trim();
        if (dataTemplate.equals("")) {
            dataTemplate = "%value%";
        }

        List choices = getDataChoices();
        for (int i = 0; i < choices.size(); i++) {
            Real         theValue     = null;
            ProbeRowInfo info         = getRowInfo(i);
            DataInstance dataInstance = info.getDataInstance();
            List         reals        = new ArrayList();
            Real         theReal      = info.getRealValue();
            if (theReal != null) {
                reals.add(theReal);
            }

            if ((i > 0) && (i < 8)) {
                sideText.append("<br>");
            }
            if (i < 8) {
                sideText.append(dataInstance.getDataChoice().getName());
                String levString = null;
                if (haveLevelsAtRow(i)) {
                    Real level = getRowInfo(i).getLevel();
                    Real alt   = getRowInfo(i).getAltitude();
                    if ((level == null) || (alt == null)) {
                        if (lastProbeAltitude != null) {
                            levString =
                                getDisplayConventions().formatAltitude(
                                    lastProbeAltitude);
                        }
                    } else {
                        levString = Util.formatReal(level);
                    }
                }
                if ( !(levString == null)) {
                    sideText.append("(" + levString + ")");
                }
                sideText.append(": ");
            }
            if (reals.size() == 0) {
                info.setDisplayValue("missing");
                if (i < 8) {
                    sideText.append("missing");
                }
                continue;
            }

            Unit rowUnit = info.getUnit();
            try {
                String valueStr = null;
                if (rowUnit != null) {
                    try {
                        valueStr = "";
                        for (int realsIdx = 0; realsIdx < reals.size();
                                realsIdx++) {
                            Real real = (Real) reals.get(realsIdx);
                            if (theValue == null) {
                                theValue = real;
                            }
                            String value;
                            String unit;
                            if (Unit.canConvert(rowUnit, real.getUnit())) {
                                value = getDisplayConventions().format(
                                    real.getValue(rowUnit));
                                unit = rowUnit.toString();
                            } else {
                                value = getDisplayConventions().format(
                                    real.getValue());
                                unit = "" + real.getUnit();
                            }
                            String tmp = StringUtil.replace(dataTemplate,
                                             "%value%", value);
                            tmp = StringUtil.replace(tmp, "%unit%", unit);
                            tmp = StringUtil.replace(tmp, "%rawvalue%",
                                    "" + real.getValue());
                            tmp = StringUtil.replace(tmp, "%rawunit%",
                                    "" + real.getUnit());
                            if (realsIdx > 0) {
                                valueStr = valueStr + ", ";
                            }
                            valueStr = valueStr + tmp;
                        }
                        if (i < 8) {
                            sideText.append(valueStr);
                        }
                        valueStr = "<html>" + valueStr + "</html>";
                        // probably not needed anymore since we check units above
                    } catch (visad.UnitException ue) {
                        userMessage("Bad unit: " + rowUnit);
                    }
                }
                if (valueStr == null) {
                    valueStr = "";
                    for (int realsIdx = 0; realsIdx < reals.size();
                            realsIdx++) {
                        Real real = (Real) reals.get(realsIdx);
                        if (theValue == null) {
                            theValue = real;
                        }
                        String tmp =
                            getDisplayConventions().format(real.getValue());
                        if (realsIdx > 0) {
                            valueStr = valueStr + ", ";
                        }
                        valueStr = valueStr + tmp;
                    }
                    if (i < 8) {
                        sideText.append(valueStr);
                    }
                }

                //                System.err.println ("value str:" + valueStr);
                info.setDisplayValue(valueStr);
                if (theValue != null) {
                    info.playSound(theValue.getValue());
                }
            } catch (Exception exc) {
                logException("Setting values", exc);
            }
        }

        if (sideLegendReadout == null) {
            sideLegendReadout = new JLabel();
        }

        if (getShowTableInLegend()) {
            sideLegendReadout.setText("<html>" + sideText.toString()
                                      + "</html>");
        } else {
            sideLegendReadout = new JLabel("<html><br></html>");
        }

        paramsTable.repaint();
        if (animation != null) {
            Set timeSet = animation.getSet();
            if (timeSet != null) {
                RealTuple timeTuple =
                    visad.util.DataUtility.getSample(timeSet, step);
                if (timeLabel != null) {
                    timeLabel.setText("" + (Real) timeTuple.getComponent(0));
                }
            }
        }
        updateLegendLabel();
    }


    /**
     * This method is called  to update the legend labels when
     * some state has changed in this control that is reflected in the labels.
     */
    protected void updateLegendLabel() {
        super.updateLegendLabel();
        // if the display label has the position, we'll update the list also
        String template = getDisplayListTemplate();
        if (template.contains(MACRO_POSITION)) {
            updateDisplayList();
        }
    }

    /**
     * Create and initialize a new ProbeRowInfo if needed. Return it.
     *
     * @param row The row
     *
     * @return The info
     */
    private ProbeRowInfo getRowInfo(int row) {
        while (row >= infos.size()) {
            ProbeRowInfo info = new ProbeRowInfo(this);
            infos.add(info);
        }
        ProbeRowInfo info = (ProbeRowInfo) infos.get(row);
        if (info.getDataInstance() == null) {
            List choices = getDataChoices();
            try {
                DataChoice dc = (DataChoice) choices.get(row);
                showWaitCursor();
                initRowInfo(info, dc);
                showNormalCursor();
            } catch (VisADException exc) {}
            catch (RemoteException exc) {}
        }
        return info;
    }

    /**
     * Initialize the row info from the given data choice
     *
     * @param rowInfo row info to initialize
     * @param dc The data choice
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void initRowInfo(ProbeRowInfo rowInfo, DataChoice dc)
            throws VisADException, RemoteException {
        rowInfo.setDataInstance(createDataInstance(dc));
        if ( !rowInfo.isGrid() && (rowInfo.getPointParameter() == null)) {}
    }

    /**
     * Create the data instance for the given data choice. This may be a grid or a point
     *
     * @param dc data choice
     *
     * @return The data instance to use
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private DataInstance createDataInstance(DataChoice dc)
            throws VisADException, RemoteException {
        Data data = dc.getData(getDataSelection(), getRequestProperties());
        if ((data instanceof FieldImpl)
                && GridUtil.isGrid((FieldImpl) data)) {
            return new GridDataInstance(dc, getDataSelection(),
                                        getRequestProperties(), data);
        } else {
            return new DataInstance(dc, getDataSelection(),
                                    getRequestProperties(), data);
        }
    }



    /**
     * Get the location of the probe
     *
     * @return probe location
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private EarthLocationTuple getProbeLocation()
            throws VisADException, RemoteException {
        RealTuple position       = probe.getPosition();
        double[]  positionValues = position.getValues();
        EarthLocationTuple elt =
            (EarthLocationTuple) boxToEarth(new double[] { positionValues[0],
                positionValues[1], positionValues[2] }, false);
        return elt;
    }



    /**
     * This clears out the cached data
     */
    private void clearCachedSamples() {
        for (int rowIdx = 0; rowIdx < infos.size(); rowIdx++) {
            ProbeRowInfo info = infos.get(rowIdx);
            info.clearCachedSamples();
        }
    }


    /**
     * Sample the data held by the info at the given point
     *
     * @param info the info
     * @param elt point
     * @param llp point again
     * @param useRowInfoCache _more_
     *
     * @return sample
     *
     *
     * @throws Exception _more_
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private FieldImpl getSampleAtPoint(ProbeRowInfo info, EarthLocation elt,
                                       boolean useRowInfoCache)
            throws VisADException, RemoteException, Exception {

        LatLonPoint llp    = elt.getLatLonPoint();

        FieldImpl   sample = null;
        if (useRowInfoCache) {
            sample = info.getPointSample(elt);
            if (sample != null) {
                return sample;
            }
        }

        if (info.isPoint()) {
            FieldImpl pointObs = (FieldImpl) info.getDataInstance().getData();
            if (pointObs == null) {
                return null;
            }
            int     numObs      = pointObs.getDomainSet().getLength();
            List    obs         = new ArrayList();

            PointOb closest     = null;
            double  minDistance = 0;

            for (int i = 0; i < numObs; i++) {
                PointOb ob = (PointOb) pointObs.getSample(i);
                double distance =
                    ucar.visad.Util.bearingDistance(ob.getEarthLocation(),
                        elt).getValue();
                if ((closest == null) || (distance < minDistance)) {
                    closest     = ob;
                    minDistance = distance;
                }
            }
            if (closest == null) {
                return null;
            }

            EarthLocation closestEL = closest.getEarthLocation();
            for (int i = 0; i < numObs; i++) {
                PointOb ob = (PointOb) pointObs.getSample(i);
                if (ob.getEarthLocation().equals(closestEL)) {
                    obs.add(ob);
                }
            }
            sample = PointObFactory.makeTimeSequenceOfPointObs(obs, 0,
                    info.getPointIndex());
            if (useRowInfoCache) {
                info.setStationName((PointOb) obs.get(0));
                info.setPointSample(sample, elt);
                setTimesForAnimation();
            }
            return sample;
        }


        //        System.out.println("getting sample for:" + info + " at:" + elt);
        FieldImpl workingGrid = info.getWorkingGrid();
        if (workingGrid == null) {
            workingGrid = info.getGridDataInstance().getGrid();
            if (GridUtil.is3D(workingGrid)
                    && !GridUtil.isVolume(workingGrid)) {
                workingGrid = GridUtil.make2DGridFromSlice(workingGrid,
                        false);
            }
        }
        if (GridUtil.isVolume(workingGrid)) {
            if (info.getAltitude() == null) {
                sample = GridUtil.sample(workingGrid, elt,
                                         info.getSamplingMode());
            } else {
                sample = GridUtil.sample(
                    workingGrid,
                    new EarthLocationTuple(llp, info.getAltitude()),
                    info.getSamplingMode());
            }
        } else {
            sample = GridUtil.sample(workingGrid, llp,
                                     info.getSamplingMode());
        }
        if (useRowInfoCache) {
            info.setWorkingGrid(workingGrid);
            info.setPointSample(sample, elt);
            setTimesForAnimation();
        }
        return sample;
    }

    /**
     * _more_
     *
     * @param elt _more_
     * @param animationValue _more_
     * @param animationStep _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List getCursorReadoutInner(EarthLocation elt, Real animationValue,
                                 int animationStep)
            throws Exception {
        List l = new ArrayList();
        for (int rowIdx = 0; rowIdx < infos.size(); rowIdx++) {
            ProbeRowInfo info = infos.get(rowIdx);
            Data[] d = getSampleAt(info, elt, animationValue, animationStep,
                                   false);
            if (d == null) {
                continue;
            }
            Data rt = d[1];
            Real r = info.getRealValue(rt);
            if (r == null|| r.isMissing()) {
                continue;
            }
            if (l.size() == 0) {
                l.add("<tr><td>"+getMenuLabel() + ":" +"</td><td></td></tr>");
            }

            Unit unit = info.getUnit();
            double value = (unit!=null?r.getValue(unit):r.getValue());
            if(unit == null) unit = r.getUnit();
            l.add("<tr><td>&nbsp;&nbsp;&nbsp;" + info.toString() + ":</td><td align=\"right\">"
                  + Misc.format(value) + "[" + unit + "]</td></tr>");
        }
        return l;
    }



    /**
     * _more_
     *
     * @param info _more_
     * @param elt _more_
     * @param aniValue _more_
     * @param step _more_
     * @param useRowInfoCache _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Data[] getSampleAt(ProbeRowInfo info, EarthLocation elt,
                               Real aniValue, int step,
                               boolean useRowInfoCache)
            throws Exception {
        FieldImpl sample = getSampleAtPoint(info, elt, useRowInfoCache);
        Data      rt     = null;
        if (sample != null) {
            if ((aniValue != null) && !aniValue.isMissing()) {
                // can't use this because it uses floats
                rt = sample.evaluate(aniValue, info.getSamplingMode(),
                                     Data.NO_ERRORS);
            } else {
                rt = sample.getSample(step);
            }
        }
        if (rt == null) {
            return null;
        }
        return new Data[] { sample, rt };
    }



    /**
     * Get the real values for the given time step
     *
     * @param aniValue The time
     * @param step The time step
     *
     *
     * @throws Exception _more_
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void setData(Real aniValue, int step)
            throws VisADException, RemoteException, Exception {
        EarthLocationTuple elt     = getProbeLocation();
        List               choices = getDataChoices();
        for (int i = 0; i < choices.size(); i++) {
            ProbeRowInfo info = getRowInfo(i);
            Data[]       d    = getSampleAt(info, elt, aniValue, step, true);
            if (d == null) {
                continue;
            }
            FieldImpl sample = (FieldImpl) d[0];
            Data      rt     = d[1];
            if (rt == null) {
                continue;
            }

            info.setTimeSample(rt);
            Unit realUnit  = null;
            Real realValue = info.getRealValue();
            //            System.err.println("rt:" + rt.getType());
            //            System.err.println("realValue:" + realValue);
            if (realValue != null) {
                realUnit = realValue.getUnit();
            }
            Unit rowUnit = info.getUnit();
            if (rowUnit == null) {  // is null or hasn't been set
                if (info.isGrid()) {
                    String name = info.getDataInstance().getParamName();
                    rowUnit = getDisplayConventions().selectDisplayUnit(name,
                            realUnit);
                } else {
                    Real r = info.getRealValue();
                    if (r != null) {
                        rowUnit = r.getUnit();
                    }
                }
                info.setUnit(rowUnit);
            }




            if (info.isGrid()) {
                info.setExtra("");
                float values[][] = sample.getFloats(false);
                if ((values.length > 0) && (values[0].length > 1)) {
                    float min = 0.0f;
                    float max = 0.0f;
                    float avg = 0.0f;
                    for (int valueIdx = 0; valueIdx < values[0].length;
                            valueIdx++) {
                        float value = values[0][valueIdx];
                        if ((valueIdx == 0) || (value < min)) {
                            min = value;
                        }
                        if ((valueIdx == 0) || (value > max)) {
                            max = value;
                        }
                        avg += value;
                    }
                    avg = avg / values[0].length;

                    if ((rowUnit != null) && (realUnit != null)) {
                        min = (float) rowUnit.toThis(min, realUnit);
                        max = (float) rowUnit.toThis(max, realUnit);
                        avg = (float) rowUnit.toThis(avg, realUnit);
                    }
                    info.setExtra(getDisplayConventions().format(min) + "/"
                                  + getDisplayConventions().format(max) + "/"
                                  + getDisplayConventions().format(avg));
                }
            }

        }
    }


    /**
     * Get the altitude at a particular level
     *
     * @param gdi     grid data instance
     * @param level   level
     * @return  altitude at the given level
     */
    private Real getAltitudeAtLevel(GridDataInstance gdi, Real level) {
        Real altitude = null;
        try {
            altitude = GridUtil.getAltitude(gdi.getGrid(), level);
        } catch (VisADException ve) {
            altitude = new Real(RealType.Altitude);
        }
        return altitude;
    }


    /**
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getSaveMenuItems(List items, boolean forMenuBar) {
        super.getSaveMenuItems(items, forMenuBar);

        items.add(GuiUtils.makeMenuItem("Save Chart Image...", getChart(),
                                        "saveImage"));
        items.add(GuiUtils.makeMenuItem("Export Current Time as CSV...",
                                        this, "exportCsv"));
        items.add(GuiUtils.makeMenuItem("Export All Times as CSV...", this,
                                        "exportCsvAllTimes"));

    }


    /** _more_ */
    private JCheckBox columnsCbx;

    /**
     * Export the current time as csv
     */
    public void exportCsv() {
        try {
            Animation animation = getAnimation(true);
            int       step      = animation.getCurrent();
            Set       aniSet    = getAnimation(true).getSet();
            Real[]    times     = Animation.getDateTimeArray(aniSet);
            if (times.length == 0) {
                return;
            }
            exportToCsv(new Real[] { times[step] });
            //        GuiUtils.exportAsCsv(tableModel);
            paramsTable.repaint();
        } catch (Exception exc) {
            logException("Exporting to csv", exc);
        }
    }



    /**
     * _more_
     */
    public void exportCsvAllTimes() {
        try {
            Set    aniSet = getAnimation(true).getSet();
            Real[] times  = Animation.getDateTimeArray(aniSet);
            exportToCsv(times);
            paramsTable.repaint();
        } catch (Exception exc) {
            logException("Exporting to csv", exc);
        }
    }


    /**
     * Export all times as csv
     *
     * @param times _more_
     */
    public void exportToCsv(Real[] times) {
        try {
            String filename =
                FileManager.getWriteFile(Misc.newList(FileManager.FILTER_CSV,
                    FileManager.FILTER_XLS), FileManager.SUFFIX_CSV);
            if (filename == null) {
                return;
            }
            amExporting = true;
            List choices = getDataChoices();

            if (times.length == 0) {
                LogUtil.userMessage("No times to export");
                return;
            }

            //Force the sampling. This sets the sample at the current location, time set, etc.
            setData(times[0], 0);
            List rows = new ArrayList();
            List cols;
            cols = Misc.newList("Time");
            for (int row = 0; row < choices.size(); row++) {
                ProbeRowInfo info = getRowInfo(row);
                cols.add(getFieldName(row));
            }
            rows.add(cols);

            for (int timeIdx = 0; timeIdx < times.length; timeIdx++) {
                Real aniValue = times[timeIdx];
                cols = Misc.newList("" + aniValue);
                rows.add(cols);
            }

            for (int timeIdx = 0; timeIdx < times.length; timeIdx++) {
                Real aniValue = times[timeIdx];
                for (int row = 0; row < choices.size(); row++) {
                    cols = (List) rows.get(timeIdx + 1);
                    ProbeRowInfo info    = getRowInfo(row);
                    Set          timeSet = info.getTimeSet();
                    Data         rt      = null;
                    FieldImpl    sample  = info.getPointSample();
                    if ((sample != null) && (timeSet != null)) {
                        rt = sample.evaluate(aniValue,
                                             info.getSamplingMode(),
                                             Data.NO_ERRORS);
                    } else {
                        rt = info.getPointSample().getSample(0);
                    }

                    if (rt == null) {
                        cols.add("missing");
                    } else {
                        if (info.getUnit() != null) {
                            Real real = null;
                            if (rt instanceof Real) {
                                real = (Real) rt;
                            } else {
                                real = (Real) ((RealTuple) rt).getComponent(
                                    0);
                            }
                            cols.add(real.getValue(info.getUnit()));
                        } else {
                            cols.add(rt.toString());
                        }
                    }
                }
            }
            DataUtil.writeCsv(filename, rows);
        } catch (Exception exc) {
            logException("Exporting to csv", exc);
        }
        amExporting = false;
    }


    /**
     *  Set the DataTemplate property.
     *
     *  @param value The new value for DataTemplate
     */
    public void setDataTemplate(String value) {
        dataTemplate = value;
    }

    /**
     *  Get the DataTemplate property.
     *
     *  @return The DataTemplate
     */
    public String getDataTemplate() {
        return dataTemplate;
    }


    /**
     * Set the XFixed property.
     *
     * @param value The new value for XFixed
     */
    public void setXFixed(boolean value) {
        xFixed = value;
        if ((probe != null) && getHaveInitialized()) {
            probe.setFixed(xFixed, yFixed, zFixed);
        }
    }

    /**
     * Get the XFixed property.
     *
     * @return The XFixed
     */
    public boolean getXFixed() {
        return xFixed;
    }

    /**
     * Set the YFixed property.
     *
     * @param value The new value for YFixed
     */
    public void setYFixed(boolean value) {
        yFixed = value;
        if ((probe != null) && getHaveInitialized()) {
            probe.setFixed(xFixed, yFixed, zFixed);
        }
    }

    /**
     * Get the YFixed property.
     *
     * @return The YFixed
     */
    public boolean getYFixed() {
        return yFixed;
    }


    /**
     * Set the ZFixed property.
     *
     * @param value The new value for ZFixed
     */
    public void setZFixed(boolean value) {
        zFixed = value;
        if ((probe != null) && getHaveInitialized()) {
            probe.setFixed(xFixed, yFixed, zFixed);
        }
    }

    /**
     * Get the ZFixed property.
     *
     * @return The ZFixed
     */
    public boolean getZFixed() {
        return zFixed;
    }



    /**
     * Set the Infos property.
     *
     * @param value The new value for Infos
     */
    public void setInfos(List<ProbeRowInfo> value) {
        infos = value;
    }

    /**
     * Get the Infos property.
     *
     * @return The Infos
     */
    public List<ProbeRowInfo> getInfos() {
        return infos;
    }







    /**
     * Set the altitudes property, use for persistence
     *
     * @param l   list of altitudes
     * @deprecated Keep around for legacy bundles
     */
    public void setAltitudes(List l) {
        _altitudes = l;
    }


    /**
     * Get the list of levels; use by persistence
     *
     * @param l   list of levels for parameters
     * @deprecated Keep around for legacy bundles
     */
    public void setLevels(List l) {
        _levels = l;
    }



    /**
     * Get the list of sampling methods for each of the parameters.
     * Used for persistence
     *
     * @param l  list of sampling methods
     * @deprecated Keep around for legacy bundles
     */
    public void setMethods(List l) {
        _methods = l;
    }


    /**
     * Set the list of display units for each parameter. Used by persistence
     *
     * @param l   list of units
     * @deprecated Keep around for legacy bundles
     */
    public void setUnits(List l) {
        _units = l;
    }


    /**
     * Set the list of display sounds for each parameter. Used by persistence
     *
     * @param l   list of sounds
     * @deprecated Keep around for legacy bundles
     */
    public void setSounds(List l) {
        _sounds = l;
    }


    /**
     * Increase the probe size
     */
    public void increaseProbeSize() {
        if (probe == null) {
            return;
        }
        pointSize = probe.getPointScale();
        setPointSize(pointSize + pointSize * 0.5f);
    }


    /**
     * Decrease the probe size
     */
    public void decreaseProbeSize() {
        if (probe == null) {
            return;
        }
        pointSize = probe.getPointScale();
        pointSize = pointSize - pointSize * 0.5f;
        if (pointSize < 0.1f) {
            pointSize = 0.1f;
        }
        setPointSize(pointSize);
    }



    /**
     *  Set the PointSize property.
     *
     *  @param value The new value for PointSize
     */
    public void setPointSize(float value) {
        pointSize = value;
        if (probe != null) {
            try {
                probe.setAutoSize(false);
                probe.setPointSize(pointSize);
                probe.setAutoSize(true);
            } catch (Exception exc) {
                logException("Increasing probe size", exc);
            }
        }
    }

    /**
     *  Get the PointSize property.
     *
     *  @return The PointSize
     */
    public float getPointSize() {
        return pointSize;
    }


    /**
     * Set the Marker property.
     *
     * @param value The new value for Marker
     */
    public void setMarker(String value) {
        marker = value;
        if ((probe != null) && (marker != null)) {
            try {
                probe.setAutoSize(false);
                probe.setMarker(
                    SelectorPoint.reduce(ShapeUtility.makeShape(marker)));
                probe.setAutoSize(true);
            } catch (Exception exc) {
                logException("Setting marker", exc);
            }
        }
    }

    /**
     * Get the Marker property.
     *
     * @return The Marker
     */
    public String getMarker() {
        return marker;
    }

    /**
     * Set the TimeSeries property.
     *
     * @param value The new value for TimeSeries
     */
    public void setTimeSeries(TimeSeriesChart value) {
        timeSeries = value;
    }

    /**
     * Get the TimeSeries property.
     *
     * @return The TimeSeries
     */
    public TimeSeriesChart getTimeSeries() {
        return timeSeries;
    }


    /**
     * Get the chart
     *
     * @return The chart_
     */
    public TimeSeriesChart getChart() {
        if (timeSeries == null) {
            timeSeries = new TimeSeriesChart(this, "Data Probe");
            timeSeries.showAnimationTime(true);
        }
        return timeSeries;
    }


    /**
     * Set the ShowThumbNail property.
     *
     * @param value The new value for ShowThumbNail
     */
    public void setShowThumbNail(boolean value) {
        getChart().setShowThumb(value);
    }


    /**
     *  Set the ShowTable property.
     *
     *  @param value The new value for ShowTable
     */
    public void setShowTable(boolean value) {
        showTable = value;
        if (tablePanel != null) {
            tablePanel.setVisible(showTable);
            tablePanel.invalidate();
            tablePanel.validate();
        }
    }

    /**
     *  Get the ShowTable property.
     *
     *  @return The ShowTable
     */
    public boolean getShowTable() {
        return showTable;
    }


    /**
     *  Set the ShowTableInLegend property.
     *
     *  @param value The new value for ShowTable
     */
    public void setShowTableInLegend(boolean value) {
        showTableInLegend = value;
        if (sideLegendReadout != null) {
            sideLegendReadout.setVisible(value);
        }
        doMoveProbe();
    }

    /**
     *  Get the ShowTableInLegend property.
     *
     *  @return The ShowTableInLegend
     */
    public boolean getShowTableInLegend() {
        return showTableInLegend;
    }

    /**
     *  Set the ShowSunriseSunset property.
     *
     *  @param value The new value for ShowSunriseSunset
     */
    public void setShowSunriseSunset(boolean value) {
        showSunriseSunset = value;
        doMoveProbe();
    }

    /**
     *  Get the ShowSunriseSunset property.
     *
     *  @return The ShowSunriseSunset
     */
    public boolean getShowSunriseSunset() {
        return showSunriseSunset;
    }



}

