/*
 * $Id: IDV-Style.xjs,v 1.1 2006/05/03 21:43:47 dmurray Exp $
 *
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.chooser;


import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.w3c.dom.Element;
import thredds.catalog.XMLEntityResolver;
import ucar.nc2.dt.StationImpl;
import ucar.nc2.thredds.TDSRadarDatasetCollection;
import ucar.nc2.units.DateUnit;
import ucar.unidata.data.radar.RadarQuery;
import ucar.unidata.metdata.NamedStation;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.util.*;
import visad.CommonUnit;
import visad.DateTime;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Jan 16, 2008
 * Time: 11:17:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class TDSRadarChooser extends TimesChooser {

    /**
     *  Holds the main gui contents. We keep this around so we can replace it with an error message
     *  when the connection to the service fails.
     */
    private JComponent outerContents;


    /** The collection */
    private TDSRadarDatasetCollection collection;


    /** The currently selected station */
    private NamedStation selectedStation;

    /** Those urls we connect to */
    //"http://motherlode.ucar.edu:8080/thredds/radarServer/catalog.xml";
    private String serverUrl;

    /** Each dataset collection URL         */
    //"http://motherlode.ucar.edu:8080/thredds/radarServer/level2/idd/dataset.xml";
    private String collectionUrl;

    /** id --> collectionUrl */
    private HashMap collectionHMap;

    /** _more_          */
    private JComboBox collectionSelector;



    /**
     * Create the RadarChooser
     *
     * @param mgr The <code>IdvChooserManager</code>
     * @param root  The xml root that defines this chooser
     *
     */
    public TDSRadarChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }



    /**
     * Handle the update event. Just pass it through to the imageChooser
     */
    public void doUpdate() {
        if ((serverUrl == null) || (datasetList == null)
                || (datasetList.size() == 0)) {
            if (urlBox != null) {
                setServer((String) urlBox.getSelectedItem());
            }
            return;
        }
        Misc.run(this, "stationChanged");
    }



    /**
     * Update the status of the gui
     */
    protected void updateStatus() {
        super.updateStatus();
        if (selectedStation == null) {
            setHaveData(false);
            setStatus("Please select a station", "stationmap");
            return;
        }
        boolean haveTimesSelected;
        if (getDoAbsoluteTimes()) {
            haveTimesSelected = getSelectedAbsoluteTimes().size() > 0;
        } else {
            haveTimesSelected = true;
        }
        setHaveData(haveTimesSelected);
        if (haveTimesSelected) {
            setStatus("Press \"" + CMD_LOAD
                      + "\" to load the selected radar data", "buttons");
        } else {
            setStatus("Please select times", "times");
        }
    }



    /**
     * Handle when there are newly selected stations
     *
     * @param stations list of newly selected stations
     */
    protected void newSelectedStations(List stations) {
        super.newSelectedStations(stations);
        if ((stations == null) || (stations.size() == 0)) {
            selectedStation = null;
        } else {
            NamedStation newStation = (NamedStation) stations.get(0);
            if (Misc.equals(newStation, selectedStation)) {
                return;
            }
            selectedStation = newStation;
        }
        Misc.run(TDSRadarChooser.this, "stationChanged");
    }

    /**
     * Make the GUI
     *
     * @return The GUI
     */
    private PreferenceList urlListHandler;

    /** _more_          */
    private JComboBox urlBox;

    /** _more_          */
    private boolean okToDoUrlListEvents = true;

    /** _more_          */
    private List datasetList;

    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent doMakeContents() {

        //Get the list of catalogs but remove the old catalog.xml entry
        urlListHandler = getPreferenceList(PREF_TDSRADARSERVER);

        ActionListener catListListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if ( !okToDoUrlListEvents) {
                    return;
                }
                setServer((String) urlBox.getSelectedItem());
            }
        };
        urlBox = urlListHandler.createComboBox(GuiUtils.CMD_UPDATE,
                catListListener, true);
        //serverUrl = (String) urlBox.getSelectedItem();
        //GuiUtils.setPreferredWidth(urlBox, 250);


        collectionSelector = new JComboBox();
        collectionSelector.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                String collectionID =
                    (String) collectionSelector.getSelectedItem();
                collectionUrl = (String) collectionHMap.get(collectionID);
                setCollection(collectionUrl);

            }

        });

        collectionSelector.getEditor().getEditorComponent().addMouseListener(
            new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if ( !SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                List       items = new ArrayList();

                JPopupMenu popup = GuiUtils.makePopupMenu(items);
                popup.show(collectionSelector, e.getX(), e.getY());
            }
        });

        JComponent stationMap = getStationMap();
        JComponent buttons    = getDefaultButtons();
        JComponent timesPanel = makeTimesPanel(true, true);
        GuiUtils.tmpInsets = GRID_INSETS;
        JPanel top = GuiUtils.doLayout(new Component[] {
                           GuiUtils.rLabel("Servers:"),
                           urlBox, GuiUtils.rLabel("Collections:"),
                           collectionSelector }, 4, GuiUtils.WT_NYNY,
                               GuiUtils.WT_N);
        GuiUtils.tmpInsets = new Insets(0,2,0,2);;
        stationMap.setPreferredSize(new Dimension(230, 200));
        stationMap = registerStatusComp("stations", stationMap);
        timesPanel = registerStatusComp("timepanel", timesPanel);
        addServerComp(stationMap);
        addServerComp(timesPanel);
        JComponent contents = GuiUtils.doLayout(new Component[] { top,
              //  stationMap, timesPanel }, 1, GuiUtils.WT_YYY, ,GuiUtils.WT_NYY);
               stationMap, timesPanel }, 1, GuiUtils.WT_Y, new double[] {0.5,  4.0, 1.0 });

        contents = GuiUtils.inset(contents, 5);
        GuiUtils.enableComponents(compsThatNeedServer, false);
        //  Misc.run(this, "initializeCollection");
        outerContents =
            GuiUtils.center(GuiUtils.topCenterBottom(getStatusComponent(),
                contents, buttons));
        return outerContents;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean shouldDoUpdateOnFirstDisplay() {
        return true;
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    private void setServer(String s) {
        datasetList    = new ArrayList();
        serverUrl = s;
        collectionHMap = getRadarCollections(serverUrl);
        Iterator it = collectionHMap.keySet().iterator();
        while (it.hasNext()) {
            datasetList.add(it.next());
        }

        GuiUtils.setListData(collectionSelector, datasetList);
    }

    /**
     * _more_
     *
     * @param s _more_
     */
    private void setCollection(String s) {
        GuiUtils.enableComponents(compsThatNeedServer, true);
        Misc.run(this, "initializeCollection");
    }

    /** _more_          */
    private List compsThatNeedServer = new ArrayList();

    /**
     * _more_
     *
     * @param comp _more_
     *
     * @return _more_
     */
    protected JComponent addServerComp(JComponent comp) {
        compsThatNeedServer.add(comp);
        return comp;
    }

    /** Command for connecting */
    protected static final String CMD_CONNECT = "cmd.connect";

    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent getConnectButton() {
        JButton connectBtn = new JButton("Connect");
        connectBtn.setActionCommand(CMD_CONNECT);
        connectBtn.addActionListener(this);
        return registerStatusComp("connect", connectBtn);
        //        return connectBtn;
    }

    /**
     * _more_
     *
     * @param radarServerURL _more_
     *
     * @return _more_
     */
    public HashMap getRadarCollections(String radarServerURL) {
        SAXBuilder        builder;
        Document          doc  = null;
        XMLEntityResolver jaxp = new XMLEntityResolver(true);
        builder = jaxp.getSAXBuilder();
        HashMap collections = new HashMap();

        try {
            doc = builder.build(radarServerURL);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        org.jdom.Element rootElem = doc.getRootElement();
        org.jdom.Element dsElem   = readElements(rootElem, "dataset");
        String           naming   = "catalogRef";
        Namespace        nss      = rootElem.getNamespace("xlink");
        java.util.List   children = dsElem.getChildren();
        for (int j = 0; j < children.size(); j++) {
            org.jdom.Element child     = (org.jdom.Element) children.get(j);
            String           childName = child.getName();
            if (childName.equals(naming)) {
                String         id     = child.getAttributeValue("ID");
                String         desc   = child.getAttributeValue("title", nss);
               // TwoFacedObject twoObj = new TwoFacedObject(desc, id);
                String       c        = radarServerURL.replaceFirst("catalog.xml","");
                String ul = c + id + "/dataset.xml";
                if(!desc.contains("NIDS"))
                    collections.put(desc, ul);
            }

        }

        return collections;
    }

    /**
     * _more_
     *
     * @param elem _more_
     * @param eleName _more_
     *
     * @return _more_
     */
    public org.jdom.Element readElements(org.jdom.Element elem,
                                         String eleName) {
        java.util.List children = elem.getChildren();
        for (int j = 0; j < children.size(); j++) {
            org.jdom.Element child     = (org.jdom.Element) children.get(j);
            String           childName = child.getName();
            if (childName.equals(eleName)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Make the collection. If there is an error then blow away the GUI and show a text area showing the error message
     */
    public void initializeCollection() {

        List stations = new ArrayList();
        try {
            StringBuffer errlog = new StringBuffer();
            try {
                collection = TDSRadarDatasetCollection.factory("test",
                        collectionUrl, errlog);
            } catch (Exception exc) {
                JTextArea lbl =
                    new JTextArea(
                        "There was an error connecting to the radar collection:\n"
                        + collectionUrl + "\n" + exc + "\n" + errlog);
                outerContents.removeAll();
                outerContents.add(BorderLayout.CENTER, lbl);
                return;
            }
            List tdsStations = collection.getRadarStations();
            for (int i = 0; i < tdsStations.size(); i++) {
                StationImpl stn = (StationImpl) tdsStations.get(i);
                // thredds.catalog.query.Location loc = stn.getLocation();
                //TODO: need better station  need to switch lat lon
                NamedStationImpl station =
                    new NamedStationImpl(stn.getName(), stn.getName(),
                                         stn.getLatitude(),
                                         stn.getLongitude(),
                                          0,
                                         CommonUnit.meter);
                stations.add(station);

            }

            getStationMap().setStations(stations);
        } catch (Exception exc) {
            JTextArea lbl =
                new JTextArea(
                    "There was an error connecting to the radar collection:\n"
                    + collectionUrl + "\n" + exc);
            outerContents.removeAll();
            outerContents.add(BorderLayout.CENTER, lbl);
            outerContents.layout();

        }
    }


    /**
     * Handle when the user has selected a new station
     */
    public void stationChanged() {
        Vector times = new Vector();
        setHaveData(false);
        if (selectedStation != null) {
            Date toDate = new Date(System.currentTimeMillis()
                                   + DateUtil.daysToMillis(1));
            //Go back 10 years (or so)
            Date fromDate = new Date(System.currentTimeMillis()
                                     - DateUtil.daysToMillis(365 * 10));
            try {
                showWaitCursor();
                setAbsoluteTimes(new ArrayList());
                setStatus("Reading times for station: " + selectedStation,
                          "");
                //                LogUtil.message("Reading times for station: "
                //                                + selectedStation);
                List allTimes =
                    collection.getRadarStationTimes(selectedStation.getID(),
                        fromDate, toDate);
                for (int timeIdx = 0; timeIdx < allTimes.size(); timeIdx++) {
                    Object timeObj = allTimes.get(timeIdx);
                    Date   date;
                    if (timeObj instanceof Date) {
                        date = (Date) timeObj;
                    } else {
                        date = DateUnit.getStandardOrISO(timeObj.toString());
                    }
                    times.add(new DateTime(date));
                }
                //                LogUtil.message("");
                showNormalCursor();
            } catch (Exception exc) {
                logException("Getting times for station: " + selectedStation,
                             exc);
                setStatus("", "");
            }
        }
        setAbsoluteTimes(times);
        updateStatus();
    }





    /**
     * Load the data
     */
    public void doLoadInThread() {
        // to the CDMRadarDataSource
        Hashtable ht = new Hashtable();

        try {
            DateSelection dateSelection = new DateSelection();
            RadarQuery radarQuery = new RadarQuery(collectionUrl,
                                        selectedStation.getID(),
                                        dateSelection);

            List urls = new ArrayList();

            if (getDoAbsoluteTimes()) {
                List times    = new ArrayList();
                List selected = makeDatedObjects(getSelectedAbsoluteTimes());
                for (int i = 0; i < selected.size(); i++) {
                    DatedThing datedThing = (DatedThing) selected.get(i);
                    Date       date       = datedThing.getDate();
                    times.add(date);
                    URI uri = collection.getRadarDatasetURI(
                                  selectedStation.getID(), date);
                    urls.add(uri.toString());
                }
                if (urls.size() == 0) {
                    LogUtil.userMessage("No times selected");
                    return;
                }
                dateSelection.setTimes(times);
            } else {
                int count = getRelativeTimesList().getSelectedIndex() + 1;
                if (count == 0) {
                    LogUtil.userMessage("No relative times selected");
                    return;
                }
                Date toDate = new Date();
                //Go back 10 years (or so)
                Date fromDate = new Date(System.currentTimeMillis()
                                         - DateUtil.daysToMillis(365 * 10));

                dateSelection.setStartFixedTime(fromDate);
                dateSelection.setEndFixedTime(toDate);
                dateSelection.setCount(count);
            }
            makeDataSource(radarQuery, "FILE.RADAR", ht);
        } catch (Exception exc) {
            logException("Loading radar data", exc);
        }
    }


}

