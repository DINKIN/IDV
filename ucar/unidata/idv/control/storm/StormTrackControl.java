/**
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


import org.w3c.dom.*;


import ucar.unidata.data.*;
import ucar.unidata.data.DataUtil;

import ucar.unidata.data.gis.KmlUtil;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.point.*;
import ucar.unidata.data.storm.*;
import ucar.unidata.data.storm.StormInfo;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.TreePanel;
import ucar.unidata.ui.TwoListPanel;

import ucar.unidata.ui.drawing.*;
import ucar.unidata.ui.symbol.*;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.MenuUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.Util;

import ucar.visad.display.*;

import visad.*;
import visad.Set;

import visad.georef.EarthLocation;
import visad.georef.MapProjection;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.rmi.RemoteException;

import java.util.*;
import java.util.List;
import java.text.SimpleDateFormat;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


/**
 * A MetApps Display Control with Displayable and controls for
 * displaying a track (balloon sounding or aircraft track)
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class StormTrackControl extends DisplayControlImpl {


    /** _more_ */
    private final static String PREF_STORMDISPLAYSTATE =
        "pref.stormdisplaystate";

    /** _more_ */
    private final static String PREF_OKWAYS = "pref.okways";

    /** _more_ */
    private final static String PREF_OKPARAMS = "pref.okparams";

    /** _more_ */
    private static int cnt = 0;



    /** _more_ */
    final ImageIcon ICON_ON =
        GuiUtils.getImageIcon("/ucar/unidata/idv/control/storm/dot.gif");

    /** _more_ */
    final ImageIcon ICON_OFF =
        GuiUtils.getImageIcon("/ucar/unidata/idv/control/storm/blank.gif");



    /** _more_          */
    private StormDisplayState localStormDisplayState;

    /** _more_ */
    private Hashtable preferences;


    /** _more_ */
    private Hashtable<String, Boolean> okWays;

    /** _more_ */
    private Hashtable<String, Boolean> okParams;


    /** _more_ */
    private String startTime;

    /** _more_ */
    private String endTime;

    /** _more_ */
    private CompositeDisplayable placeHolder;

    /** _more_ */
    private StormDataSource stormDataSource;


    /** _more_ */
    private List<StormInfo> stormInfos;


    /** Holds the EarthLocation of the last point clicked */
    private EarthLocation lastEarthLocation = null;


    /** _more_ */
    private Hashtable<StormInfo, StormDisplayState> stormDisplayStateMap =
        new Hashtable<StormInfo, StormDisplayState>();


    /** _more_ */
    private List<StormDisplayState> activeStorms;

    /** _more_ */
    private TreePanel treePanel;

    private Hashtable<Integer,YearDisplayState> yearDisplayStateMap = new Hashtable<Integer,YearDisplayState>();

    private Hashtable yearData=new Hashtable();


    /** _more_          */
    private JCheckBox obsCbx;

    /** _more_          */
    private JCheckBox forecastCbx;

    /** _more_          */
    private JCheckBox mostRecentCbx;


    /**
     * Create a new Track Control; set the attribute flags
     */
    public StormTrackControl() {
        setAttributeFlags(FLAG_COLORTABLE);
    }




    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        placeHolder = new CompositeDisplayable("Place holder");
        addDisplayable(placeHolder);

        List dataSources = new ArrayList();
        dataChoice.getDataSources(dataSources);

        if (dataSources.size() != 1) {
            userMessage("Could not find Storm Data Source");
            return false;
        }


        if ( !(dataSources.get(0) instanceof StormDataSource)) {
            userMessage("Could not find Storm Data Source");
            return false;
        }

        getColorTableWidget(getRangeForColorTable());
        stormDataSource = (StormDataSource) dataSources.get(0);

        if (okWays == null) {
            okWays = (Hashtable<String,
                                Boolean>) getPreferences().get(PREF_OKWAYS);
        }
        if (okWays == null) {
            okWays = new Hashtable<String, Boolean>();
        }

        if (okParams == null) {
            okParams =
                (Hashtable<String,
                           Boolean>) getPreferences().get(PREF_OKPARAMS);
        }
        if (okParams == null) {
            okParams = new Hashtable<String, Boolean>();
        }


        return true;
    }





    private Hashtable rangeTypes = new Hashtable();

    /**
     * _more_
     *
     * @param track _more_
     *
     * @param param _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected FieldImpl makeTrackField(StormTrack track, StormParam param)
            throws Exception {

        List<StormTrackPoint> points    = track.getTrackPoints();
        int                   numPoints = points.size();
        RealType rangeType    = null;
        double[][]    newRangeVals = new double[1][numPoints];
        float[]       alts         = new float[numPoints];
        float[]       lats         = new float[numPoints];
        float[]       lons         = new float[numPoints];
        Real[]        values       = ((param == null)
                                      ? null
                                      : track.getTrackAttributeValues(param));
        Unit unit = (param!=null?param.getUnit():null);
        for (int pointIdx = 0; pointIdx < numPoints; pointIdx++) {
            StormTrackPoint stp   = points.get(pointIdx);
            Real            value = ((values == null)
                                     ? null
                                     : values[pointIdx]);

            //Set the dflt so we can use its unit later
            if (rangeType == null) {
                String key = track.getWay() +"_" + param;
                rangeType = (RealType) rangeTypes.get(key);
                if(rangeType == null) {
                    cnt++;
                    rangeType =Util.makeRealType("trackrange_" + track.getWay() +"_"  +cnt, unit);
                    rangeTypes.put(key, rangeType);
                }
            }
            EarthLocation el       = stp.getLocation();
            newRangeVals[0][pointIdx] = (value!=null?value.getValue():0);
            lats[pointIdx]            = (float) el.getLatitude().getValue();
            lons[pointIdx]            = (float) el.getLongitude().getValue();
            alts[pointIdx] = 1;
            //            if(Math.abs(lats[i])>90) System.err.println("bad lat:" + lats[i]);
        }
        GriddedSet llaSet = ucar.visad.Util.makeEarthDomainSet(lats, lons,
                                alts);
        Set[] rangeSets = new Set[]{
            new DoubleSet(new SetType(rangeType))
        };
        FunctionType newType =
            new FunctionType(((SetType) llaSet.getType()).getDomain(),
                             rangeType);
        FlatField trackField = new FlatField(newType, llaSet,
                                        (CoordinateSystem) null,
                                        rangeSets,
                                             new Unit[] { unit});
        trackField.setSamples(newRangeVals, false);
        return trackField;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public DisplayMaster getDisplayMaster() {
        return getDisplayMaster(placeHolder);
    }



    /**
     * _more_
     *
     * @param way _more_
     *
     * @return _more_
     */
    protected boolean okToShowWay(Way way) {
        if (way.isObservation()) {
            return true;
        }
        if (okWays == null) {
            return true;
        }
        if ((okWays.size() > 0) && (okWays.get(way.getId()) == null)) {
            return false;
        }
        return true;
    }

    /**
     * _more_
     *
     * @param realType _more_
     *
     * @return _more_
     */
    protected boolean okToShowParam(RealType realType) {
        if (okParams == null) {
            return true;
        }
        if ((okParams.size() > 0) && (okParams.get(realType) == null)) {
            return false;
        }
        return true;
    }


    /**
     * _more_
     */
    public void showWaySelectDialog() {
        List checkBoxes = new ArrayList();
        List useWays    = new ArrayList();
        List allWays    = new ArrayList<Way>();
        for (Way way : stormDataSource.getWays()) {
            if (way.isObservation()) {
                continue;
            }
            allWays.add(way);
            if (okToShowWay(way)) {
                useWays.add(way);
            }
        }
        useWays = Misc.sort(useWays);
        allWays = Misc.sort(allWays);
        JCheckBox writeAsPreferenceCbx = new JCheckBox("Save as preference",
                                             false);
        TwoListPanel tlp = new TwoListPanel(allWays, "Don't Use", useWays,
                                            "Use", null, false);
        JComponent contents = GuiUtils.centerBottom(tlp,
                                  GuiUtils.left(writeAsPreferenceCbx));
        if ( !GuiUtils.showOkCancelDialog(null, getWayName() + " Selection",
                                          contents, null)) {
            return;
        }
        List only = tlp.getCurrentEntries();

        if (only.size() == allWays.size()) {
            onlyShowTheseWays(new ArrayList<Way>(),
                              writeAsPreferenceCbx.isSelected());
        } else {
            onlyShowTheseWays((List<Way>) only,
                              writeAsPreferenceCbx.isSelected());
        }

    }




    /**
     * _more_
     *
     * @return _more_
     */
    public StormDisplayState getCurrentStormDisplayState() {
        if (localStormDisplayState != null) {
            return localStormDisplayState;
        }
        if (treePanel == null) {
            return null;
        }

        Component comp = treePanel.getVisibleComponent();
        if (comp == null) {
            return null;
        }
        for (int i = stormInfos.size() - 1; i >= 0; i--) {
            StormInfo stormInfo = stormInfos.get(i);
            StormDisplayState stormDisplayState =
                getStormDisplayState(stormInfo);
            if (stormDisplayState.getContents() == comp) {
                return stormDisplayState;
            }
        }
        return null;
    }



    /**
     * _more_
     *
     * @param ways _more_
     * @param writeAsPreference _more_
     */
    protected void onlyShowTheseWays(List<Way> ways,
                                     boolean writeAsPreference) {
        okWays = new Hashtable();
        for (Way way : ways) {
            okWays.put(way.getId(), new Boolean(true));
        }
        for (int i = stormInfos.size() - 1; i >= 0; i--) {
            StormInfo stormInfo = stormInfos.get(i);
            StormDisplayState stormDisplayState =
                getStormDisplayState(stormInfo);
            stormDisplayState.reload();
        }
        if (writeAsPreference) {
            putPreference(PREF_OKWAYS, okWays);
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public StormDataSource getStormDataSource() {
        return stormDataSource;
    }


    /**
     * _more_
     *
     * @param stormDisplayState _more_
     */
    public void viewStorm(StormDisplayState stormDisplayState) {
        if (treePanel != null) {
            treePanel.show(stormDisplayState.getContents());
        }
    }

    /**
     * _more_
     */
    public void unloadAllTracks() {
        for (int i = stormInfos.size() - 1; i >= 0; i--) {
            StormInfo stormInfo = stormInfos.get(i);
            StormDisplayState stormDisplayState =
                getStormDisplayState(stormInfo);
            if (stormDisplayState.getActive()) {
                stormDisplayState.deactivate();
            }
        }
    }


    /**
     * _more_
     *
     * @param items _more_
     * @param forMenuBar _more_
     */
    protected void getSaveMenuItems(List items, boolean forMenuBar) {
        StormDisplayState current = getCurrentStormDisplayState();
        if ((current != null) && current.getActive()) {
            items.add(GuiUtils.makeMenuItem("Export to Spreadsheet", current,
                                            "writeToXls"));

        }
        items.add(GuiUtils.makeMenuItem("Export to Google Earth", this,
                                        "writeToKml"));
        super.getSaveMenuItems(items, forMenuBar);
    }






    /**
     * _more_
     *
     * @param items _more_
     * @param forMenuBar _more_
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        StormDisplayState current = getCurrentStormDisplayState();
        if ((current != null) && current.getActive()) {
            items.add(GuiUtils.makeMenuItem("Add Forecast Time Chart",
                                            current, "addForecastTimeChart"));
            items.add(GuiUtils.makeMenuItem("Add Forecast Hour Chart",
                                            current, "addForecastHourChart"));
            items.add(GuiUtils.makeMenuItem("Select " + getWaysName()
                                            + " To Use", this,
                                                "showWaySelectDialog"));
            items.add(
                GuiUtils.makeMenuItem(
                    "Save Storm Display as Preference", this,
                    "saveStormDisplayState"));
        }
        if (getPreferences().get(PREF_STORMDISPLAYSTATE) != null) {
            items.add(
                GuiUtils.makeMenuItem(
                    "Remove Storm Display Preference", this,
                    "deleteStormDisplayState"));
        }
        super.getEditMenuItems(items, forMenuBar);
    }


    /**
     * _more_
     *
     * @param items _more_
     * @param forMenuBar _more_
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        try {
            List subMenus = new ArrayList();
            GregorianCalendar cal =
                new GregorianCalendar(DateUtil.TIMEZONE_GMT);
            Hashtable menus       = new Hashtable();
            List      activeItems = new ArrayList();
            for (int i = stormInfos.size() - 1; i >= 0; i--) {
                StormInfo stormInfo = stormInfos.get(i);
                cal.setTime(
                    ucar.visad.Util.makeDate(stormInfo.getStartTime()));
                int   year     = cal.get(Calendar.YEAR);
                JMenu yearMenu = (JMenu) menus.get("" + year);
                if (yearMenu == null) {
                    yearMenu = new JMenu("" + year);
                    menus.put("" + year, yearMenu);
                    subMenus.add(yearMenu);
                }
                StormDisplayState stormDisplayState =
                    getStormDisplayState(stormInfo);
                if (stormDisplayState.getActive()) {
                    activeItems.add(
                        MenuUtil.makeMenuItem(
                            stormInfo.toString(), this, "viewStorm",
                            stormDisplayState));
                }
                if (stormInfo.getBasin() != null) {
                    JMenu basinMenu = (JMenu) menus.get(year + "Basin:"
                                          + stormInfo.getBasin());
                    if (basinMenu == null) {
                        basinMenu = new JMenu("Basin:"
                                + stormInfo.getBasin());
                        menus.put(year + "Basin:" + stormInfo.getBasin(),
                                  basinMenu);
                        yearMenu.add(basinMenu);
                    }
                    yearMenu = basinMenu;
                }
                yearMenu.add(GuiUtils.makeMenuItem(stormInfo.toString(),
                        this, "viewStorm", stormDisplayState));
            }

            JMenu trackMenu = GuiUtils.makeMenu("Storm Tracks", subMenus);
            GuiUtils.limitMenuSize(trackMenu, "Tracks:", 30);



            if (activeItems.size() > 0) {
                activeItems.add(0, GuiUtils.MENU_SEPARATOR);
                activeItems.add(0, GuiUtils.makeMenuItem("Unload all tracks",
                        this, "unloadAllTracks", null));
                trackMenu.insert(GuiUtils.makeMenu("Active Tracks",
                        activeItems), 0);
            }

            items.add(trackMenu);
            super.getViewMenuItems(items, forMenuBar);
        } catch (Exception exc) {
            logException("Making track menu", exc);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getWayName() {
        return stormDataSource.getWayName();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getWaysName() {
        return stormDataSource.getWaysName();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected String getDataProjectionLabel() {
        return "Use Projection From Tracks";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public MapProjection getDataProjection() {
        return null;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasMapProjection() {
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public MapProjection getDataProjectionForMenu() {
        try {
            double minLon = Double.POSITIVE_INFINITY;
            double maxLon = Double.NEGATIVE_INFINITY;
            double minLat = Double.POSITIVE_INFINITY;
            double maxLat = Double.NEGATIVE_INFINITY;
            List<StormDisplayState> stormDisplayStates =
                getStormDisplayStates();
            boolean didone = false;
            for (StormDisplayState stormDisplayState : getActiveStorms()) {
                LatLonRect bbox = stormDisplayState.getBoundingBox();
                if (bbox == null) {
                    continue;
                }
                minLon = Math.min(minLon, bbox.getLonMin());
                maxLon = Math.max(maxLon, bbox.getLonMax());
                minLat = Math.min(minLat, bbox.getLatMin());
                maxLat = Math.max(maxLat, bbox.getLatMax());
                didone = true;
            }

            for(YearDisplayState yearDisplayState: getYearDisplayStates()) {
                if(!yearDisplayState.getActive()) continue;
                List<StormTrack> yearTracks = yearDisplayState.getStormTracks();
                for (StormTrack track : yearTracks) {
                    LatLonRect bbox = track.getBoundingBox();
                    if (bbox == null) {
                        continue;
                    }
                    minLon = Math.min(minLon, bbox.getLonMin());
                    maxLon = Math.max(maxLon, bbox.getLonMax());
                    minLat = Math.min(minLat, bbox.getLatMin());
                    maxLat = Math.max(maxLat, bbox.getLatMax());
                    didone = true;
                }
            }


            if ( !didone) {
                return null;
            }
            return ucar.visad.Util.makeMapProjection(minLat, minLon, maxLat,
                    maxLon);
        } catch (Exception exc) {
            logException("Error making projection from tracks", exc);
            return null;
        }

    }



    /**
     * _more_
     *
     * @return _more_
     */
    private List<StormDisplayState> getActiveStorms() {
        if (activeStorms == null) {
            List<StormDisplayState> tmpList =
                new ArrayList<StormDisplayState>();
            List<StormDisplayState> stormDisplayStates =
                getStormDisplayStates();
            for (StormDisplayState stormDisplayState : stormDisplayStates) {
                if (stormDisplayState.getActive()) {
                    tmpList.add(stormDisplayState);
                }
            }
            activeStorms = tmpList;
        }
        return activeStorms;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private Hashtable getPreferences() {
        if (preferences == null) {
            String path = stormDataSource.getClass().getName()
                          + ".StormTrackControl.xml";
            preferences =
                (Hashtable) getIdv().getStore().getEncodedFile(path);
            if (preferences == null) {
                preferences = new Hashtable();
            }
        }
        return preferences;
    }



    /**
     * _more_
     */
    public void deleteStormDisplayState() {
        String template =
            (String) getPreferences().get(PREF_STORMDISPLAYSTATE);
        if (template != null) {
            getPreferences().remove(PREF_STORMDISPLAYSTATE);
            writePreferences();
        }
    }

    /**
     * _more_
     */
    public void saveStormDisplayState() {
        try {
            StormDisplayState current = getCurrentStormDisplayState();
            if (current == null) {
                return;
            }
            boolean wasActive = current.getActive();
            current.setActive(false);
            current.setStormTrackControl(null);
            String xml = getIdv().encodeObject(current, false);
            current.setStormTrackControl(this);
            current.setActive(wasActive);
            putPreference(PREF_STORMDISPLAYSTATE, xml);
            userMessage(
                "<html>Preference saved. <br>Note: This will take effect for new display controls</html>");
        } catch (Exception exc) {
            logException("Saving storm display", exc);
        }

    }

    /**
     * _more_
     */
    private void writePreferences() {
        String path = stormDataSource.getClass().getName()
                      + ".StormTrackControl.xml";
        getIdv().getStore().putEncodedFile(path, preferences);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param object _more_
     */
    private void putPreference(String key, Object object) {
        getPreferences().put(key, object);
        writePreferences();
    }


    /**
     * _more_
     *
     * @param stormInfo _more_
     *
     * @return _more_
     */
    private StormDisplayState getStormDisplayState(StormInfo stormInfo) {
        StormDisplayState stormDisplayState =
            stormDisplayStateMap.get(stormInfo);
        try {
            if (stormDisplayState == null) {
                String template =
                    (String) getPreferences().get(PREF_STORMDISPLAYSTATE);
                if (template != null) {
                    try {
                        stormDisplayState =
                            (StormDisplayState) getIdv().decodeObject(
                                template);
                        stormDisplayState.setStormInfo(stormInfo);
                    } catch (Exception exc) {
                        logException("Creating storm display", exc);
                        System.err.println("Error decoding preference:"
                                           + exc);
                        //noop
                    }
                }
            }
            if (stormDisplayState == null) {
                stormDisplayState = new StormDisplayState(stormInfo);
            }

            stormDisplayState.setStormTrackControl(this);
            stormDisplayStateMap.put(stormInfo, stormDisplayState);
        } catch (Exception exc) {
            logException("Creating storm display", exc);
        }

        return stormDisplayState;
    }


    /**
     * _more_
     */
    public void initDone() {
        super.initDone();
        try {
            for (Enumeration keys = stormDisplayStateMap.keys();
                    keys.hasMoreElements(); ) {
                StormInfo key = (StormInfo) keys.nextElement();
                StormDisplayState stormDisplayState =
                    stormDisplayStateMap.get(key);
                stormDisplayState.setStormTrackControl(this);
                stormDisplayState.initDone();

                MapProjection mapProjection = getDataProjectionForMenu();
                if (mapProjection != null) {
                    MapViewManager mvm = getMapViewManager();
                    if (mvm != null) {
                        mvm.setMapProjection(
                            mapProjection, true,
                            getDisplayConventions().getMapProjectionLabel(
                                mapProjection, this), true);
                    }
                }

            }
        } catch (Exception exc) {
            logException("Setting new storm info", exc);
        }
        Misc.run(this,"initYears");
    }


    public void initYears() {
        List<YearDisplayState> ydss = getYearDisplayStates();
        for (YearDisplayState yds : ydss) {
            if(!yds.getActive()) continue;
            try {
                yds.setState(yds.STATE_LOADING);
                loadYearInner(yds);
            } catch (Exception exc) {
                logException("Loading year", exc);
                return;
            }
        }
    }






    public void loadYear(final YearDisplayState yds) {
        Misc.run(new Runnable() {
            public void run() {
                try {
                    yds.setState(yds.STATE_LOADING);
                    loadYearInner(yds);
                } catch (Exception exc) {
                    logException("Loading year", exc);
                }
            }
        });

    }


    public void loadYearInner(YearDisplayState yds) throws Exception {
        TextType      textType = TextType.getTextType("ID");
        List fields = new ArrayList();
        List times  = new ArrayList();
        List<StormTrack> obsTracks = new ArrayList<StormTrack>();
        List<PointOb> pointObs = new ArrayList<PointOb>();

        JWindow errorWindow=null;
        JLabel errorLabel=null;


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        GregorianCalendar cal =
            new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        Hashtable<String, Boolean> obsWays = new Hashtable<String,
            Boolean>();
        obsWays.put(Way.OBSERVATION.toString(), new Boolean(true));
        String currentMessage = "";
        String errors = "";
        boolean doYearTime = false;
        for (int i = stormInfos.size() - 1; i >= 0; i--) {
            if(yds.getState()!=yds.STATE_LOADING) {
                yds.setState(YearDisplayState.STATE_INACTIVE);
                yds.setStatus("");
                if(errorWindow!=null) {
                    errorWindow.setVisible(false);
                }
                return;
            }
            StormInfo stormInfo = stormInfos.get(i);
            cal.setTime(
                        ucar.visad.Util.makeDate(stormInfo.getStartTime()));
            int stormYear = cal.get(Calendar.YEAR);
            if (stormYear != yds.getYear()) {
                continue;
            }

            Object     key      = yds.getYear() + "_" + stormInfo.getStormId();
            StormTrack obsTrack = (StormTrack) yearData.get(key);
            if (obsTrack == null) {
                yds.setStatus("Loading " + stormInfo + "...");
                currentMessage = "Loading " + stormInfo;
                try {
                    StormTrackCollection tracks =
                        stormDataSource.getTrackCollection(stormInfo,
                                                           obsWays);
                    obsTrack = tracks.getObsTrack();
                    if (obsTrack == null) {
                        continue;
                    }
                    obsTrack = new StormTrack(obsTrack);
                    obsTrack.setWay(new Way(obsTrack.getWay() + "_year" + yds.getYear()));
                    yearData.put(key, obsTrack);
                } catch (BadDataException bde) {
                    if(errorWindow==null) {
                        Window parent = GuiUtils.getWindow(yds.getButton());
                        errorWindow = new JWindow(parent);
                        errorWindow.getContentPane().add( errorLabel = new JLabel(" "));
                        errorLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
                        errorWindow.pack();
                        Point     loc = yds.getButton().getLocationOnScreen();
                        errorWindow.setLocation(
                                                (int) loc.getX(),
                                                (int) (loc.getY() +yds.getButton().bounds().height));
                        errorWindow.show();
                    }
                    errors = errors + "Error " + currentMessage  + "<br>";
                    yds.setStatus("Error:" + currentMessage);
                    errorLabel.setText("<html><i>"
                                       + errors + "</i></html>");
                    errorWindow.pack();
                }
            }

            if(obsTrack!=null) {
                FieldImpl       field = makeTrackField(obsTrack, null);
                StormTrackPoint stp   = obsTrack.getTrackPoints().get(0);
                DateTime dttm = new DateTime(sdf.parse(""+yds.getYear()));
                if(!doYearTime) {
                    dttm = stormInfo.getStartTime();
                }
                obsTracks.add(obsTrack);
                times.add(dttm);
                fields.add(field);
                Tuple tuple = new Tuple(new Data[] {
                    new visad.Text(textType,
                                   stormInfo.toString()) });
                pointObs.add(
                             PointObFactory.makePointOb(
                                                        stp.getLocation(), dttm,
                                                        tuple));
            }

        }
        if(errorWindow!=null) {
            errorWindow.setVisible(false);
        }
        yds.setData(obsTracks, times, fields, pointObs);
        yds.setState(YearDisplayState.STATE_ACTIVE);
        yds.setStatus("");
    }


            /**
     * _more_
     */
    public void writeToKml() {
        if (obsCbx == null) {
            obsCbx        = new JCheckBox("Observation", true);
            forecastCbx   = new JCheckBox("Forecast", true);
            mostRecentCbx = new JCheckBox("Most Recent Forecasts", false);
        }
        JComponent accessory = GuiUtils.top(GuiUtils.vbox(obsCbx,
                                   forecastCbx, mostRecentCbx));

        String filename =
            FileManager.getWriteFile(Misc.newList(FileManager.FILTER_KML),
                                     FileManager.SUFFIX_KML, accessory);
        if (filename == null) {
            return;
        }

        try {
            writeToKml(filename, obsCbx.isSelected(),
                       forecastCbx.isSelected(), mostRecentCbx.isSelected());
        } catch (Exception exc) {
            logException("Writing KML", exc);
        }
    }

    /**
     * _more_
     *
     * @param filename _more_
     * @param doObs _more_
     * @param doForecast _more_
     * @param mostRecent _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void writeToKml(String filename, boolean doObs,
                           boolean doForecast, boolean mostRecent)
            throws VisADException, RemoteException {
        try {
            Element kmlNode = KmlUtil.kml("");
            Element docNode = KmlUtil.document(kmlNode, "");
            KmlUtil.iconstyle(
                docNode, "hurricaneicon",
                "http://www.unidata.ucar.edu/software/idv/kml/images/hurricane.png");
            Hashtable state = new Hashtable();
            for (StormDisplayState stormDisplayState : getActiveStorms()) {
                stormDisplayState.writeToKml(docNode, state, doObs,
                                             doForecast, mostRecent);
            }

            List<YearDisplayState> ydss = getYearDisplayStates();
            for (YearDisplayState yds : ydss) {
                if(!yds.getActive()) continue;
                Element yearNode = KmlUtil.folder(docNode,
                                                  "Year:" + yds.getYear());
                for(StormTrack track: yds.getStormTracks()) {
                    writeToGE(docNode, state, yearNode, track, yds.getColor());
                }
            }

            FileOutputStream fileOut = new FileOutputStream(filename);
            IOUtil.writeBytes(new File(filename),
                              XmlUtil.toString(kmlNode).getBytes());

        } catch (Exception exc) {
            logException("Writing KML", exc);
        }
    }



    /**
     * _more_
     *
     *
     * @param docNode _more_
     * @param state _more_
     * @param parent _more_
     * @param track _more_
     *
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected void writeToGE(Element docNode, Hashtable state,
                             Element parent, StormTrack track, Color color)
            throws VisADException, RemoteException {
        Element placemark = KmlUtil.placemark(parent, "Track",
                                "<html>" + getWayName()
                                + ":" + track.getWay() + "<br>" + ""
                                + track.getStartTime() + "</html>");

        int cnt = 0;
        String dateString =
            track.getStartTime().formattedString("yyyy-MM-dd hhmm",
                DateUtil.TIMEZONE_GMT);
        String           sheetName = track.getWay() + " - " + dateString;
        int              rowCnt    = 0;
        List<StormParam> params    = track.getParams();
        StringBuffer     sb        = new StringBuffer();
        for (StormTrackPoint stp : track.getTrackPoints()) {
            EarthLocation el = stp.getLocation();
            if (track.getWay().isObservation()) {
                Element icon = KmlUtil.placemark(
                                   parent, "Time:" + stp.getTime(),
                                   "<html><table>"
                                   + formatStormTrackPoint(
                                       track, stp) + "</table></html>", el,
                                           "#hurricaneicon");
                KmlUtil.timestamp(icon, stp.getTime());
            }

            sb.append(el.getLongitude().getValue());
            sb.append(",");
            sb.append(el.getLatitude().getValue());
            sb.append(",");
            sb.append(el.getAltitude().getValue());
            sb.append("\n");
        }

        String styleUrl = "linestyle" + track.getWay();
        if (state.get(styleUrl) == null) {
            Element style =
                KmlUtil.linestyle(
                    docNode, styleUrl,
                    color,
                    track.getWay().isObservation()
                    ? 3
                    : 2);
            state.put(styleUrl, style);
        }
        KmlUtil.styleurl(placemark, "#" + styleUrl);
        Element linestring = KmlUtil.linestring(placemark, false, false,
                                 sb.toString());
        //        KmlUtil.timestamp(linestring, track.getStartTime());
        if ( !track.getWay().isObservation()) {
            KmlUtil.timestamp(placemark, track.getStartTime());
        } else {}
    }



    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {


        //Get the storm infos and sort them
        stormInfos =
            (List<StormInfo>) Misc.sort(stormDataSource.getStormInfos());

        if (stormInfos.size() == 1) {
            try {
                if (localStormDisplayState == null) {
                    localStormDisplayState =
                        new StormDisplayState(stormInfos.get(0));
                }
                stormDisplayStateMap = new Hashtable<StormInfo,
                        StormDisplayState>();
                localStormDisplayState.setStormTrackControl(this);
                stormDisplayStateMap.put(stormInfos.get(0),
                                         localStormDisplayState);
                localStormDisplayState.setIsOnlyChild(true);
                JComponent comp = localStormDisplayState.getContents();
                localStormDisplayState.loadStorm();
                return comp;
            } catch (Exception exc) {
                logException("Creating storm display", exc);
                return new JLabel("Error");
            }
        }
        localStormDisplayState = null;
        treePanel              = new TreePanel(true, 150);
        Hashtable         years                  = new Hashtable();
        JComponent        firstComponent         = null;
        JComponent        firstSelectedComponent = null;
        GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);

        List yearComps = new ArrayList();
        for (int i = stormInfos.size() - 1; i >= 0; i--) {
            StormInfo stormInfo = stormInfos.get(i);
            cal.setTime(ucar.visad.Util.makeDate(stormInfo.getStartTime()));
            int year = cal.get(Calendar.YEAR);
            if (years.get(new Integer(year)) == null) {
                YearDisplayState yds = getYearDisplayState(year);
                yearComps.add(new JLabel(""+year));
                yearComps.add(yds.getButton());
                yearComps.add(GuiUtils.wrap(yds.getColorSwatch()));
                yearComps.add(yds.getLabel());
                years.put(new Integer(year),"");
            }
        }
        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent yearComponent = GuiUtils.doLayout(yearComps, 4, GuiUtils.WT_NNNY, GuiUtils.WT_N);
        if(yearComps.size()>1) {
            int width  = 300;
            int height = 400;
            JScrollPane scroller = GuiUtils.makeScrollPane(GuiUtils.top(yearComponent), width,
                                       height);
            scroller.setBorder(BorderFactory.createLoweredBevelBorder());
            scroller.setPreferredSize(new Dimension(width, height));
            scroller.setMinimumSize(new Dimension(width, height));
            yearComponent = scroller;
        }
        treePanel.addComponent(GuiUtils.top(yearComponent), null, "Yearly Tracks",null);

        years                  = new Hashtable();

        //Go in reverse order so we get the latest first
        for (int i = stormInfos.size() - 1; i >= 0; i--) {
            StormInfo stormInfo = stormInfos.get(i);
            cal.setTime(ucar.visad.Util.makeDate(stormInfo.getStartTime()));
            int year = cal.get(Calendar.YEAR);
            StormDisplayState stormDisplayState =
                getStormDisplayState(stormInfo);

            String category = "" + year;
            JComponent panelContents = stormDisplayState.getContents();
            if (stormInfo.getBasin() != null) {
                category = category + TreePanel.CATEGORY_DELIMITER + "Basin:"
                           + stormInfo.getBasin();
            }
            treePanel.addComponent(panelContents, category,
                                   stormInfo.toString(),
                                   stormDisplayState.getActive()
                                   ? ICON_ON
                                   : ICON_OFF);

            if (stormDisplayState.getActive()
                    && (firstSelectedComponent == null)) {
                firstSelectedComponent = panelContents;
            }
            if (firstComponent == null) {
                firstComponent = panelContents;
            }
        }

        //Show the first selected component or the first component
        if (firstSelectedComponent != null) {
            treePanel.show(firstSelectedComponent);
        } else if (firstComponent != null) {
            treePanel.show(firstComponent);
        }

        treePanel.setPreferredSize(new Dimension(500, 400));
        JComponent contents = treePanel;

        //        JComponent contents = GuiUtils.topCenter(GuiUtils.left(box),
        //                                  scroller);
        contents.setPreferredSize(new Dimension(500, 400));


        if ((startTime != null) && (endTime != null)) {
            try {

                Date[] range = DateUtil.getDateRange(startTime, endTime,
                                   new Date());
                double fromDate = range[0].getTime();
                double toDate   = range[1].getTime();
                for (StormInfo stormInfo : stormInfos) {
                    double date =
                        Util.makeDate(stormInfo.getStartTime()).getTime();
                    StormDisplayState stormDisplayState =
                        getStormDisplayState(stormInfo);
                    if ((date >= fromDate) && (date <= toDate)) {
                        stormDisplayState.loadStorm();
                    } else if (stormDisplayState.getActive()) {
                        stormDisplayState.deactivate();
                    }
                }
            } catch (java.text.ParseException pe) {
                logException("Error parsing start/end dates:" + startTime
                             + " " + endTime, pe);
            }
        }



        return contents;
    }

    /**
     * _more_
     *
     * @param stormDisplayState _more_
     */
    public void stormChanged(StormDisplayState stormDisplayState) {
        activeStorms = null;
        if (treePanel != null) {
            treePanel.setIcon(stormDisplayState.getContents(),
                              stormDisplayState.getActive()
                              ? ICON_ON
                              : ICON_OFF);
        }
    }




    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        try {
            List<StormDisplayState> active = getActiveStorms();
            for (StormDisplayState stormDisplayState : active) {
                stormDisplayState.timeChanged(time);
            }
        } catch (Exception exc) {
            logException("changePosition", exc);
        }
        super.timeChanged(time);
    }




    /**
     *  Set the StormDisplayStates property.
     *
     *  @param value The new value for StormDisplayStates
     */
    public void setStormDisplayStates(List<StormDisplayState> value) {
        if (value != null) {
            for (StormDisplayState stormDisplayState : value) {
                stormDisplayStateMap.put(stormDisplayState.getStormInfo(),
                                         stormDisplayState);
            }
        }
    }


    /**
     *  Get the StormDisplayStates property.
     *
     *  @return The StormDisplayStates
     */
    public List<StormDisplayState> getStormDisplayStates() {
        List<StormDisplayState> stormDisplayStates =
            new ArrayList<StormDisplayState>();
        for (Enumeration keys = stormDisplayStateMap.keys();
                keys.hasMoreElements(); ) {
            StormInfo key = (StormInfo) keys.nextElement();
            StormDisplayState stormDisplayState =
                stormDisplayStateMap.get(key);
            //TODO: We don't want to add every state, just the ones that have been changed
            //            if(stormDisplayState.getChanged()) {
            if (stormDisplayState.getActive()) {
                stormDisplayStates.add(stormDisplayState);
            }
        }
        return stormDisplayStates;
    }



    public YearDisplayState getYearDisplayState(int year) {
        YearDisplayState yearDisplayState = yearDisplayStateMap.get(new Integer(year));
        if(yearDisplayState==null) {
            yearDisplayState = new YearDisplayState(this, year);
            yearDisplayStateMap.put(new Integer(year), yearDisplayState);
        }
        return yearDisplayState;
    }


    /**
     *  Set the YearDisplayStates property.
     *
     *  @param value The new value for YearDisplayStates
     */
    public void setYearDisplayStates(List<YearDisplayState> value) {
        if (value != null) {
            yearDisplayStateMap = new Hashtable<Integer, YearDisplayState>();
            for (YearDisplayState yearDisplayState : value) {
                yearDisplayStateMap.put(new Integer(yearDisplayState.getYear()),
                                         yearDisplayState);
            }
        }
    }


    /**
     *  Get the YearDisplayStates property.
     *
     *  @return The YearDisplayStates
     */
    public List<YearDisplayState> getYearDisplayStates() {
        List<YearDisplayState> yearDisplayStates =
            new ArrayList<YearDisplayState>();
        for (Enumeration keys = yearDisplayStateMap.keys();
                keys.hasMoreElements(); ) {
            Object key =  keys.nextElement();
            YearDisplayState yearDisplayState =
                yearDisplayStateMap.get(key);
            if (yearDisplayState.getActive()) {
                yearDisplayStates.add(yearDisplayState);
            }
        }
        return yearDisplayStates;
    }



    /**
     * _more_
     *
     * @param el _more_
     * @param animationValue _more_
     * @param animationStep _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getCursorReadoutInner(EarthLocation el,
                                         Real animationValue,
                                         int animationStep)
            throws Exception {

        StormTrackPoint ob             = null;

        List            result         = new ArrayList();
        List            theStormStates = getStormDisplayStates();
        if (theStormStates != null) {
            Object[] pair = findClosestPoint(el, theStormStates,
                                             animationValue, 20);
            if (pair != null) {
                StormTrack      closestTrack = (StormTrack) pair[0];
                StormTrackPoint closestOb    = (StormTrackPoint) pair[1];
                result.add("<tr><td>" + "Way: " + closestTrack.getWay()
                           + "</td></tr> "
                           + formatStormTrackPoint(closestTrack, closestOb));

            }
        }

        return result;
    }


    /**
     * _more_
     *
     *
     * @param stormTrack _more_
     * @param stp _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected String formatStormTrackPoint(StormTrack stormTrack,
                                           StormTrackPoint stp)
            throws VisADException, RemoteException {
        Unit   displayUnit = getDisplayUnit();
        double value;
        if (stp == null) {
            return "";
        }
        List<StormParam> params = stormTrack.getParams();
        //            result = "<tr><td>" + "Storm: "
        //                     + stp.toString() + "</td></tr>";
        String result = "<tr><td>" + "Track Point Time:</td><td align=right>"
                        + stp.getTime() + "</td></tr>";
        for (StormParam param : params) {
            Real r = stp.getAttribute(param);
            if (r == null) {
                continue;
            }
            Unit unit = param.getUnit();
            result = result + "<tr><td>" + param.toString()
                     + ":</td><td align=right>" + Misc.format(r.getValue())
                     + ((unit != null)
                        ? ("[" + unit + "]")
                        : "") + "</td></tr>";
        }

        int length = result.length();
        return StringUtil.padLeft(result, 5 * (20 - length), "&nbsp;");
    }



    /**
     * This finds the StormTrack and StormTrackPoint that is closest to the given location
     *
     *
     * @param el _more_
     * @param theStates _more_
     * @param animationValue _more_
     * @param distanceThresholdPixels _more_
     * @return A 2-tuple. First element is the StormTrack. Second element is the ob.
     *      Or null if none found
     *
     * @throws Exception _more_
     */
    protected Object[] findClosestPoint(EarthLocation el,
                                        List<StormDisplayState> theStates,
                                        Real animationValue,
                                        int distanceThresholdPixels)
            throws Exception {
        if ((el == null) || (theStates == null)) {
            return null;
        }

        int             numStates    = theStates.size();
        StormTrackPoint closestOb    = null;
        StormTrack      closestTrack = null;

        int[]           clickPt      = boxToScreen(earthToBox(el));
        double          minDistance  = distanceThresholdPixels;
        //        System.err.println ("click:" + clickPt[0]+"/"+clickPt[1] + " " +minDistance);

        for (int i = 0; i < numStates; i++) {
            StormDisplayState sds   = theStates.get(i);
            StormInfo         sinfo = sds.getStormInfo();
            HashMap<Way, List> wayToTracksMap =
                sds.getTrackCollection().getWayToTracksHashMap();
            // Way obsWay = new Way(Way.OBSERVATION);
            java.util.Set<Way> ways = wayToTracksMap.keySet();

            for (Way way : ways) {
                StormTrack track = null;
                if (way.equals(Way.OBSERVATION)) {
                    //  WayDisplayState       trackWDS   = wayToTracksMap.get(way); //get(Way.OBSERVATION);
                    List<StormTrack> tracks = wayToTracksMap.get(way);
                    if (tracks.size() > 0) {
                        track = tracks.get(0);
                    }
                } else {
                    WayDisplayState trackWDS = sds.getWayDisplayState(way);  //get(Way.OBSERVATION);
                    boolean visible = checkTracksVisible(animationValue,
                                          trackWDS);
                    if (visible) {
                        List<StormTrack> tracks = wayToTracksMap.get(way);
                        track = getClosestTimeForecastTrack(tracks,
                                animationValue);
                    }
                }

                if (track == null) {
                    continue;
                }
                //System.err.println(way + " track time is: " + track.getStartTime());
                List<StormTrackPoint> stpList = track.getTrackPoints();
                int                   size    = stpList.size();
                for (int j = 0; j < size; j++) {
                    StormTrackPoint stp      = stpList.get(j);
                    EarthLocation   stpLoc   = stp.getLocation();
                    int[]           obScreen =
                        boxToScreen(earthToBox(stpLoc));
                    double distance = GuiUtils.distance(obScreen, clickPt);
                    if (distance < minDistance) {
                        closestOb    = stp;
                        minDistance  = distance;
                        closestTrack = track;
                    }
                }
            }
            //            System.err.println ("\t" + obScreen[0]+"/"+obScreen[1] + " d:" + distance);

        }

        if (closestOb != null) {
            return new Object[] { closestTrack, closestOb };
        }

        return null;
    }




    /**
     * _more_
     *
     * @param currentAnimationTime _more_
     * @param wds _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean checkTracksVisible(Real currentAnimationTime,
                                       WayDisplayState wds)
            throws Exception {
        if ((currentAnimationTime == null)
                || currentAnimationTime.isMissing()) {
            return false;
        }
        // Iterate way display states
        boolean visible = false;
        if (wds.shouldShowTrack() && wds.hasTrackDisplay()) {
            FieldImpl field = (FieldImpl) wds.getTrackDisplay().getData();
            if (field == null) {
                return false;
            }
            Set timeSet = GridUtil.getTimeSet(field);
            if (timeSet == null) {
                return false;
            }
            if (timeSet.getLength() == 1) {
                return true;
            } else {
                //Else work the visad magic
                float timeValueFloat = (float) currentAnimationTime.getValue(
                                           timeSet.getSetUnits()[0]);
                //            System.err.println("multiple times:" + timeValueFloat);
                float[][] value = {
                    { timeValueFloat }
                };
                int[]     index = timeSet.valueToIndex(value);
                //            System.err.println("index:" + index[0]);
                return visible = (index[0] >= 0);
            }

        }
        return visible;
    }

    /**
     * _more_
     *
     * @param tracks _more_
     * @param pTime _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    private StormTrack getClosestTimeForecastTrack(List<StormTrack> tracks,
            Real pTime)
            throws VisADException {

        DateTime dt            = new DateTime(pTime);  // pTime.
        double   timeToLookFor = dt.getValue();
        int      numPoints     = tracks.size();
        double   lastTime      = -1;

        //  for(StormTrack track: tracks){
        //      if(track.getTrackStartTime().equals(dt))
        //         return track;
        //  }
        for (int i = 0; i < numPoints; i++) {
            StormTrack st          = tracks.get(i);
            double     currentTime = st.getStartTime().getValue();
            if (timeToLookFor == currentTime) {
                return st;
            }
            if (timeToLookFor < currentTime) {
                if (i == 0) {
                    return null;
                }
                if (timeToLookFor > lastTime) {
                    return tracks.get(i - 1);
                }
            }
            lastTime = currentTime;
        }
        return null;
    }


    /**
     * Set the OkWays property.
     *
     * @param value The new value for OkWays
     */
    public void setOkWays(Hashtable<String, Boolean> value) {
        okWays = value;
    }

    /**
     * Get the OkWays property.
     *
     * @return The OkWays
     */
    public Hashtable<String, Boolean> getOkWays() {
        return okWays;
    }


    /**
     * Set the OkParams property.
     *
     * @param value The new value for OkParams
     */
    public void setOkParams(Hashtable<String, Boolean> value) {
        okParams = value;
    }

    /**
     * Get the OkParams property.
     *
     * @return The OkParams
     */
    public Hashtable<String, Boolean> getOkParams() {
        return okParams;
    }





    /**
     * Set the StartTime property.
     *
     * @param value The new value for StartTime
     */
    public void setStartTime(String value) {
        startTime = value;
    }

    /**
     * Get the StartTime property.
     *
     * @return The StartTime
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * Set the EndTime property.
     *
     * @param value The new value for EndTime
     */
    public void setEndTime(String value) {
        endTime = value;
    }

    /**
     * Get the EndTime property.
     *
     * @return The EndTime
     */
    public String getEndTime() {
        return endTime;
    }



    /**
     * Set the LocalStormDisplayState property.
     *
     * @param value The new value for LocalStormDisplayState
     */
    public void setLocalStormDisplayState(StormDisplayState value) {
        localStormDisplayState = value;
    }

    /**
     * Get the LocalStormDisplayState property.
     *
     * @return The LocalStormDisplayState
     */
    public StormDisplayState getLocalStormDisplayState() {
        return localStormDisplayState;
    }



}

