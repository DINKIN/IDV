/*
 * $Id: IdvUIManager.java,v 1.353 2007/08/22 11:59:17 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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


package ucar.unidata.idv.ui;


import org.python.core.*;
import org.python.util.*;

import org.w3c.dom.*;

import ucar.unidata.collab.*;
import ucar.unidata.data.*;
import ucar.unidata.gis.maps.*;
import ucar.unidata.idv.*;

import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.collab.CollabManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.MapDisplayControl;
import ucar.unidata.idv.publish.IdvPublisher;

import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.ui.FineLineBorder;
import ucar.unidata.ui.Help;
import ucar.unidata.ui.HelpTipDialog;
import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.ui.MultiFrame;
import ucar.unidata.ui.RovingProgress;
import ucar.unidata.ui.XmlUi;
import ucar.unidata.ui.colortable.ColorTableEditor;
import ucar.unidata.ui.symbol.StationModel;
import ucar.unidata.ui.symbol.StationModelManager;

import ucar.unidata.util.ColorTable;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.MemoryMonitor;
import ucar.unidata.util.Misc;

import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectArray;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.Resource;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlPersistable;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import ucar.unidata.idv.control.DisplaySettingsDialog;

import ucar.visad.VisADPersistence;
import ucar.visad.display.DisplayMaster;

import visad.Data;
import visad.VisADException;

import visad.python.*;


import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.*;

import java.lang.reflect.Method;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;



/**
 * Manages the user interface for the IDV
 *
 *
 * @author IDV development team
 */
public class IdvUIManager extends IdvManager {


    /** property for support form */
    public static final String PROP_SUPPORT_PACKAGE = "idv.support.package";

    /** property for support form */
    public static final String PROP_SUPPORT_MESSAGE = "idv.support.message";

    /** property for support form */
    public static final String PROP_HELP_EMAIL = "idv.help.email";

    /** property for support form */
    public static final String PROP_HELP_ORG = "idv.help.org";

    /** property for support form */
    public static final String PROP_HELP_NAME = "idv.help.name";


    /** Xml tag for the actions resource */
    public static final String TAG_ACTION = "action";

    /** Xml attribute name for the  in the actions resource */
    public static final String ATTR_ID = "id";

    /** Xml attribute name for skin mainwindow attribute */
    public static final String ATTR_MAINWINDOW = "mainwindow";

    /** Xml attribute name for the  description in the actions resource */
    public static final String ATTR_DESCRIPTION = "description";

    /** Xml attribute name for the  action in the actions resource */
    public static final String ATTR_ACTION = "action";

    /** Xml attribute name for the  image in the actions resource */
    public static final String ATTR_IMAGE = "image";


    /** The identifier of the  toolbar component */
    public static final String COMP_FAVORITESBAR = "idv.favoritesbar";

    /** Help tag for xmlui */
    public static final String COMP_HELP = "idv.help";

    /** The identifier of the  menubar component */
    public static final String COMP_MENUBAR = "idv.menubar";

    /** The identifier of the  toolbar component */
    public static final String COMP_TOOLBAR = "idv.toolbar";


    /** The identifier of the  data selector component */
    public static final String COMP_DATASELECTOR = "idv.dataselector";


    /** The identifier of the  toolbar component */
    public static final String COMP_MEMORYMONITOR = "idv.memorymonitor";

    /** The identifier of the  wait label */
    public static final String COMP_WAITLABEL = "idv.waitlabel";

    /** The identifier of the  streaming indicator */
    public static final String COMP_STREAMINGLABEL = "idv.streaminglabel";

    /** The identifier of the  wait label */
    public static final String COMP_READINGLABEL = "idv.readinglabel";

    /** The identifier of the  progress bar */
    public static final String COMP_PROGRESSBAR = "idv.progressbar";

    /** Xml tag name for datatree */
    public static final String COMP_DATATREE = "idv.datatree";

    /** Xml tag name for the panel that holds all of the views */
    public static final String COMP_VIEWPANEL = "idv.viewpanel";

    /** Xml tag name for messagelogger */
    public static final String COMP_MESSAGELOGGER = "idv.messagelogger";

    /** Xml tag name for messagelabel */
    public static final String COMP_MESSAGELABEL = "idv.messagelabel";

    /** Xml tag name for map view manager */
    public static final String COMP_MAPVIEW = "idv.mapview";

    /** Xml tag name for any view manager */
    public static final String COMP_VIEW = "idv.view";

    /** Xml tag name for status label */
    public static final String COMP_STATUSBAR = "idv.statusbar";

    /**
     * Xml tag name for the chooser tag.
     * We overwrite the XmlUi component  factory
     * method to handle  these special tags
     */
    public static final String COMP_CHOOSER = "chooser";


    /** tag for xmlui to show choosers */
    public static final String COMP_CHOOSERS = "idv.choosers";



    /**
     *  The identifier of the "Data" menu held in the xml file that defines
     *  the menu bar (e.g., resources/defaultmenu.xml). We use this identifier
     *  to find the actual JMenu that is created from the xml so we can dink with it.
     */
    public static final String MENU_DATA = "data";

    /** identifier for maps menu */
    public static final String MENU_MAPS = "maps";

    /** identifier for special menu */
    public static final String MENU_SPECIAL = "special";

    /** identifier for locations menu */
    public static final String MENU_LOCATIONS = "locations";

    /** Help menu */
    public static final String MENU_HELP = "help";


    /** ID for the View menu */
    public static final String MENU_VIEW = "view";


    /** Menu id in the menu xml for the delete views menu */
    public static final String MENU_DELETEVIEWS = "edit.deleteviews";

    /** the edit formulas menu */
    public static final String MENU_EDITFORMULAS = "edit.formulas";


    /**
     *  The identifier of the "New views" menu item held in the xml file that defines
     *  the menu bar (e.g., resources/defaultmenu.xml). We use this identifier
     *  to find the actual JMenuItem that is created from the xml so we can dis/enable it.
     */
    public static final String MENU_NEWVIEWS = "file.new.views";


    /** id for the windows menu */
    public static final String MENU_WINDOWS = "menu.windows";

    /**
     *  The identifier of the "Data" menu held in the xml file that defines
     *  the menu bar (e.g., resources/defaultmenu.xml). We use this identifier
     *  to find the actual JMenu that is created from the xml so we can dink with it.
     */
    public static final String MENU_PUBLISH = "publish";

    /** Menu id in the menu xml for the publis/configure menu */
    public static final String MENU_PUBLISH_CONFIG = "publish.config";


    /**
     *  The identifier of the "History" menu held in the xml file that defines
     *  the menu bar (e.g., resources/defaultmenu.xml).
     */
    public static final String MENU_HISTORY = "menu.history";


    /**
     *  The identifier of the "Deletehistory" menu held in the xml file that defines
     *  the menu bar (e.g., resources/defaultmenu.xml).
     */
    public static final String MENU_DELETEHISTORY = "menu.deletehistory";

    /**
     *  The identifier of the "Displays" menu held in the xml file that defines
     *  the menu bar (e.g., resources/defaultmenu.xml). We use this identifier
     *  to find the actual JMenu that is created from the xml so we can dink with it.
     */
    public static final String MENU_DISPLAYS = "displays";


    /** Id of the "New Display" menu item for the file menu */
    public static final String MENU_NEWDISPLAY = "file.new.display";


    /** Have we done the initDone */
    private boolean haveInitialized = false;


    /**
     * A mapping from a JMenu (the data menu) to the list of initial components in the menu
     */
    private Hashtable fixedDataMenuItems = new Hashtable();

    /** Maps favorite type to the BundleTree that shows the Manage window for the type */
    private Hashtable bundleTrees = new Hashtable();


    /** MUTEX for doing the set cursor calls */
    private Object CURSOR_MUTEX = new Object();

    /** The splash screen */
    private IdvSplash splash;

    /** Maps aciton id to xml element */
    private Hashtable actionMap;

    /** List of all action ids */
    private List actionList;


    /** A cache of the operand name to value for the user choices */
    private Hashtable operandCache;

    /**
     * Keep track of the last window that the user has moused in
     * We do this so when we are adding a DisplayControl into one of the
     * main windows we add it into the last active window.
     */
    protected IdvWindow lastActiveFrame = null;


    /** How many showWaitCursor/showNormalCursor calls have been made. */
    private int waitCursorCount = 0;


    /** List of the {@link DataControlDialog}s currently active */
    List dcdWindows = new ArrayList();


    /** THe help tip dialog */
    private HelpTipDialog helpTipDialog;


    /**
     *  Holds a list of windows are created during initialization
     *  and that need to be shown when initialization is complete
     */
    private List windowsToBeShown;


    /**
     *  A list of things that show data source. Use to notify them
     *  when new data sources have changed.
     */
    List dataSourceHolders = new ArrayList();

    /** List of the {@link DataSourceHolder}s currently active */
    Hashtable dataSourceHolderWindows = new Hashtable();

    /** Holds the default window sizes for the data source holders */
    Hashtable defaultHolderBounds;


    /** The toolbar editor */
    private ToolbarEditor toolbarEditor;

    /** The view panel */
    private ViewPanel viewPanel;

    /** The control tabs */
    private JTabbedPane legendTabs;

    /** Maps ViewManager to the JComponent in the legend tab */
    Hashtable legendTabMap = new Hashtable();


    /** the desktop pane */
    private JDesktopPane desktopPane;

    /** the desktop frame */
    private JFrame desktopFrame;


    /** list of the screens */
    private GraphicsDevice[] screens;

    /** list of the rectangles for the screens */
    private Rectangle[] screenRects;

    /** default screen */
    private GraphicsDevice defaultScreen;

    /**
     * Create me with the IDV
     *
     * @param idv The IDV
     */
    public IdvUIManager(IntegratedDataViewer idv) {
        super(idv);
    }

    /**
     * Load in the look and feel
     */
    public void loadLookAndFeel() {
        String lookAndFeel = getStore().get(PREF_LOOKANDFEEL, (String) null);
        if (lookAndFeel != null) {
            try {
                UIManager.setLookAndFeel(lookAndFeel);
            } catch (Exception exc) {
                System.err.println("Unknown look and feel:" + lookAndFeel);
            }
        }
    }


    /**
     * Called by the IDV when its initialization is done.
     * This method closes the splash window and
     * opens any windows that need to be open,
     */
    public void init() {
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            screens = ge.getScreenDevices();
            int ns = screens.length;
            screenRects = new Rectangle[ns];
            for (int j = 0; j < ns; j++) {
                GraphicsDevice screen = screens[j];
                screenRects[j] = screen.getDefaultConfiguration().getBounds();
                //System.out.println("screen:" + screen + " size "
                //                   + screenRects[j]);
            }
            defaultScreen = ge.getDefaultScreenDevice();
        } catch (HeadlessException he) {}
        //System.out.println("default screen = " + defaultScreen);

        ucar.visad.display.AnimationWidget.bigIcon =
            getProperty("idv.animation.bigicon", false);


        if (getStateManager().getProperty(PROP_UI_DESKTOP, false)) {
            desktopPane = new JDesktopPane();
            desktopPane.setPreferredSize(new Dimension(700, 500));
            desktopFrame = new JFrame(getStateManager().getTitle());
            desktopFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    if (getIdv().quit()) {}
                }
            });
            desktopFrame.getContentPane().add(desktopPane);
            desktopFrame.pack();
            desktopFrame.setLocation(new Point(50, 50));
            desktopFrame.setDefaultCloseOperation(
                WindowConstants.DO_NOTHING_ON_CLOSE);
            MultiFrame.useDesktopPane(desktopPane);
        }

        if (getIdv().getStateManager().getShowDashboardOnStart()) {
            showBasicWindow(true);
            //            doMakeBasicWindows();
        }





    }


    /**
     * Create the first display window
     *
     */
    public void doMakeInitialGui() {
        createNewWindow(new ArrayList(), false);
    }


    /**
     * Create the basic windows. This gets called at start up and if the user
     * presses "show dashboard" and there isn't any windows available
     */
    public void doMakeBasicWindows() {
        splashMsg("Creating User Interface");
        List skins = StringUtil.split(
                         getStateManager().getProperty(
                             "idv.ui.initskins", ""), ";", true, true);
        for (int i = 0; i < skins.size(); i++) {
            String skin = (String) skins.get(i);
            try {
                createNewWindow(new ArrayList(), skin);
            } catch (Throwable exc) {
                logException("Creating UI from skin:" + skin, exc);
            }
        }
    }


    /**
     * Find the component that is in an IdvWindow with the id
     *
     * @param id id
     *
     * @return component
     */
    private Object findComponent(String id) {
        List comps = findComponents(id);
        if (comps.size() > 0) {
            return comps.get(0);
        }
        return null;
    }


    /**
     * This gets all of the objects that are associated with the given
     * group for all active windows. We use this for finding the active
     * choosers.
     *
     * @param group  The group id
     *
     * @return List of objects in the given group
     */
    public List getWindowGroup(Object group) {
        List      groupList;
        List      comps  = new ArrayList();
        IdvWindow active = IdvWindow.getActiveWindow();
        if (active != null) {
            groupList = active.getGroup(group);
            if (groupList != null) {
                comps.addAll(groupList);
            }
        }
        List windows = new ArrayList(IdvWindow.getWindows());
        for (int i = 0; i < windows.size(); i++) {
            IdvWindow window = (IdvWindow) windows.get(i);
            if (active == window) {
                continue;
            }
            groupList = window.getGroup(group);
            if (groupList != null) {
                comps.addAll(groupList);
            }
        }
        return comps;
    }


    /**
     * Get all components in all windows
     *
     * @return components
     */
    public List getComponents() {
        List      comps  = new ArrayList();
        IdvWindow active = IdvWindow.getActiveWindow();
        if (active != null) {
            List windowComps = active.getComponents();
            if (windowComps != null) {
                System.err.println("window comps:" + windowComps.size());
                comps.addAll(windowComps);
            }
        }
        List windows = new ArrayList(IdvWindow.getWindows());
        for (int i = 0; i < windows.size(); i++) {
            IdvWindow window = (IdvWindow) windows.get(i);
            if (active == window) {
                continue;
            }
            List windowComps = window.getComponents();
            if (windowComps != null) {
                comps.addAll(windowComps);
            }
        }
        return comps;
    }


    /**
     * find the components with the given id in the idv windows
     *
     * @param id id
     *
     * @return list of components
     */
    private List findComponents(String id) {
        Object    comp   = null;
        List      comps  = new ArrayList();
        IdvWindow active = IdvWindow.getActiveWindow();
        if (active != null) {
            comp = IdvWindow.getActiveWindow().getComponent(id);
            if (comp != null) {
                comps.add(comp);
                return comps;
            }
        }
        List windows = new ArrayList(IdvWindow.getWindows());
        for (int i = 0; i < windows.size(); i++) {
            IdvWindow window = (IdvWindow) windows.get(i);
            if (active == window) {
                continue;
            }
            comp = window.getComponent(id);
            if (comp != null) {
                comps.add(comp);
            }
        }
        return comps;
    }





    /**
     *  return the screen containing the point, null if none do
     *
     * @param p  point in the display
     *
     * @return  the GraphicsDevice for that point
     */
    public GraphicsDevice getScreen(Point p) {
        if ((p == null) || (screens == null)) {
            return null;
        }

        GraphicsDevice screen = null;
        int            i,
                       n      = screenRects.length;
        for (i = 0; i < n; i++) {
            if (screenRects[i].contains(p)) {
                screen = screens[i];
                break;
            }
        }
        // System.out.println("found screen " + screen + " for point " + p);
        return screen;
    }


    /**
     * Close the currently active window.
     */
    public void closeCurrentWindow() {
        IdvWindow window = IdvWindow.getActiveWindow();
        if (window != null) {
            window.doClose();
        }
    }




    /**
     * get the view panel, the one that holds the displays.
     * If it doesn't exist then create it
     *
     * @return ViewPanel
     */
    public ViewPanel getViewPanel() {
        if (viewPanel == null) {
            viewPanel = new ViewPanel(getIdv());
            viewPanel.getContents();
        }
        return viewPanel;
    }


    /**
     * This gets called by the view manager to see if the uimanager should do
     * something with the side legend, whether the side legend is embedded in some
     * window.
     *
     * @param viewManager The view manager
     * @param sideLegend The side legend
     *
     * @return true if this UIManager will embed the legend. False if the ViewManager should deal
     * with the legend as usual.
     */
    public boolean handleSideLegend(final ViewManager viewManager,
                                    JComponent sideLegend) {
        if (getArgsManager().getIsOffScreen()) {
            return false;
        }

        if (legendTabs == null) {
            return false;
        }
        JComponent wrapper;
        JButton showBtn = GuiUtils.makeButton("Show Window", viewManager,
                              "toFront");
        wrapper = GuiUtils.topCenter(GuiUtils.left(GuiUtils.inset(showBtn,
                2)), sideLegend);
        wrapper = GuiUtils.left(wrapper);
        legendTabMap.put(viewManager, wrapper);
        legendTabs.add(getViewManagerTabLabel(viewManager, legendTabs),
                       wrapper);
        legendTabs.setSelectedIndex(legendTabs.getTabCount() - 1);
        return true;
    }


    /**
     * Be notified of the addition of a VM
     *
     * @param viewManager The VM
     */
    public void viewManagerAdded(ViewManager viewManager) {
        if (getArgsManager().getIsOffScreen()) {
            return;
        }
        if (getViewPanel() != null) {
            getViewPanel().viewManagerAdded(viewManager);
        }
    }


    /**
     * Called when the ViewManager is removed. If we are showing legends in a
     * window then we remove the tab
     *
     * @param viewManager The ViewManager that was destroyed
     */
    public void viewManagerDestroyed(ViewManager viewManager) {
        if (getArgsManager().getIsOffScreen()) {
            return;
        }
        if (getViewPanel() != null) {
            getViewPanel().viewManagerDestroyed(viewManager);
        }

        JComponent wrapper = (JComponent) legendTabMap.get(viewManager);
        if ((wrapper != null) && (legendTabs != null)) {
            legendTabMap.remove(viewManager);
            legendTabs.remove(wrapper);
        }
    }


    /**
     * The  active state of the  view manager has changed
     *
     * @param viewManager The view manager
     */
    public void viewManagerActiveChanged(ViewManager viewManager) {
        if (getViewPanel() != null) {
            getViewPanel().viewManagerChanged(viewManager);
        }
    }


    /**
     * Called when the ViewManager is changed. If we are showing legends in a
     * window then we update the tab label
     *
     * @param viewManager The ViewManager that was changed
     */
    public void viewManagerChanged(ViewManager viewManager) {
        if (getArgsManager().getIsOffScreen()) {
            return;
        }
        if (getViewPanel() != null) {
            getViewPanel().viewManagerChanged(viewManager);
        }

        JComponent wrapper = (JComponent) legendTabMap.get(viewManager);
        if ((wrapper != null) && (legendTabs != null)) {
            legendTabs.setTitleAt(legendTabs.indexOfComponent(wrapper),
                                  getViewManagerTabLabel(viewManager,
                                      legendTabs));
        }

    }




    /**
     * Create the tab label for the embedded legend tabs
     *
     * @param viewManager View manager in the tab
     * @param tabs The tabs
     *
     * @return Label to use
     */
    protected String getViewManagerTabLabel(ViewManager viewManager,
                                            JTabbedPane tabs) {
        String name = viewManager.getName();
        if ((name == null) || (name.trim().length() == 0)) {
            name = "View " + (tabs.getTabCount() + 1);
        }
        return /*viewManager.getTypeName() + ": " +*/ name;
        //        return "<html><table><tr><td>" + viewManager.getTypeName()+": " + name + "</td></tr></table></html>";
    }

    /**
     * Called by the IDV when its initialization is done.
     * This method closes the splash window and
     * opens any windows that need to be open,
     */
    public void initDone() {
        if (haveInitialized) {
            return;
        }
        haveInitialized = true;
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);


        if (getIdv().getStateManager().getShowDashboardOnStart()) {
            //            showBasicWindow(true);
            //            doMakeBasicWindows();
        }


        //Only  make the default gui if we have no main windows
        if ( !getArgsManager().getIsOffScreen()
                && (IdvWindow.getMainWindows().size() == 0)) {
            if ( !getArgsManager().getNoGui()) {
                doMakeInitialGui();
            }
        }

        splashClose();
        if (desktopFrame != null) {
            desktopFrame.setSize(new Dimension(700, 500));
            desktopFrame.setVisible(true);
        }

        if (windowsToBeShown != null) {
            for (int i = 0; i < windowsToBeShown.size(); i++) {
                IdvWindow idvWindow = (IdvWindow) windowsToBeShown.get(i);
                idvWindow.setVisible(true);
                idvWindow.show();
            }
            windowsToBeShown = null;
        }

        List vms = getVMManager().getViewManagers();
        for (int i = 0; i < vms.size(); i++) {
            ((ViewManager) vms.get(i)).guiInitializationDone();
        }

        /**
         * for (int i = 0; i < dataSourceHolders.size(); i++) {
         *   DataSourceHolder holder =
         *       (DataSourceHolder) dataSourceHolders.get(i);
         *   holder.show();
         * }
         */
        showAllWindows();
        initHelpTips(true);
        GuiUtils.toFrontModalDialogs();
    }



    /**
     * Read in the actions xml
     */
    private void initActions() {
        if (actionMap != null) {
            return;
        }
        actionMap  = new Hashtable();
        actionList = new ArrayList();
        XmlResourceCollection xrc = getResourceManager().getXmlResources(
                                        getResourceManager().RSC_ACTIONS);

        for (int i = 0; i < xrc.size(); i++) {
            Element root = xrc.getRoot(i);
            if (root == null) {
                continue;
            }
            List children = XmlUtil.findChildren(root, TAG_ACTION);
            for (int actionIdx = 0; actionIdx < children.size();
                    actionIdx++) {
                Element actionNode = (Element) children.get(actionIdx);
                String  id         = XmlUtil.getAttribute(actionNode,
                                         ATTR_ID);
                actionMap.put(id, actionNode);
                actionList.add(id);
                //                System.out.println("<li><b>" + id +"</b><br>" + XmlUtil.getAttribute(actionNode, ATTR_DESCRIPTION));
            }

        }
    }

    /**
     * Is the given id an action. Does it start with action:
     *
     * @param id The id
     *
     * @return Is it an action
     */
    public boolean isAction(String id) {
        return id.startsWith("action:");
    }


    /**
     * Strip any &quot;action:& string from the beginning.
     *
     * @param id The action id
     *
     * @return The id stripped of any action:
     */
    public String stripAction(String id) {
        if (id.startsWith("action:")) {
            id = id.substring(7);
        }
        return id;
    }

    /**
     * Find the xml element for the given action
     *
     * @param id The action id. May start with &quot;action:&quot;
     *
     * @return The xml element that describes the action
     */
    public Element getActionNode(String id) {
        initActions();
        return (Element) actionMap.get(stripAction(id));
    }

    /**
     * Get the given named attribute from the xml element that represents the
     * action id
     *
     * @param id The action
     * @param attr The attr name
     *
     * @return The attr value or null if none found
     */
    public String getActionAttr(String id, String attr) {
        initActions();
        Element node = getActionNode(stripAction(id));
        if (node == null) {
            return null;
        }
        return XmlUtil.getAttribute(node, attr, (String) null);
    }


    /**
     * Get the action action for the given action id
     *
     * @param id The action id
     *
     * @return The aciton to invoke_
     */
    public String getAction(String id) {
        return getActionAttr(id, ATTR_ACTION);
    }

    /**
     * Get the image for the given action
     *
     * @param id The action id
     *
     * @return The image
     */
    public String getActionImage(String id) {
        return getActionAttr(id, ATTR_IMAGE);
    }

    /**
     * Get the description for the given action
     *
     * @param id The action id
     *
     * @return The description
     */

    public String getActionDescription(String id) {
        return getActionAttr(id, ATTR_DESCRIPTION);
    }

    /**
     * Get the list of (String) action ids.
     *
     * @return List of actions
     */
    public List getActions() {
        initActions();
        return actionList;
    }



    /**
     *  Implementation of the ControlContext method.
     *  If the idv has been initialized then this simply shows the
     *  window. If not yet fully initialized then we place this window
     *  in a list of windows to be displayed after initialization is done.
     *
     * @param control The new DisplayControl
     * @param window Its window
     */
    public void showWindow(DisplayControl control, IdvWindow window) {
        if (getIdv().getHaveInitialized()) {
            window.setVisible(true);
        } else {
            if (windowsToBeShown == null) {
                windowsToBeShown = new ArrayList();
            }
            windowsToBeShown.add(window);
        }
    }



    /**
     * Create the splash screen if needed
     */
    public void initSplash() {
        //Create and show the splash screen (if ok)
        if (getProperty(PROP_SHOWSPLASH, true)
                && !getArgsManager().getNoGui()
                && !getArgsManager().getIsOffScreen()
                && !getArgsManager().testMode) {
            splash = new IdvSplash(getIdv());
            splashMsg("Loading Programs");
        } else {
            //            splash = new IdvSplash (this);
        }
    }

    /**
     * Return the number of open wait cursor calls
     *
     * @return number of wait cursor calls pending
     */
    public int getWaitCursorCount() {
        return waitCursorCount;
    }

    /**
     *  Increment the waitCursorCount and set the wait cursor on the last active frame
     */
    public void showWaitCursor() {
        synchronized (CURSOR_MUTEX) {
            waitCursorCount++;
            if (waitCursorCount == 1) {
                //              System.err.println ("UI.setWaitCursor");
                long tmp = System.currentTimeMillis();
                //              Trace.msg("*** wait on time since last wait:" + (tmp-timeSinceLastWait));
                timeSinceLastWait = tmp;
                setCursor(true, GuiUtils.waitCursor);
            }
        }
    }


    /** For timing debugs */
    public static long timeSinceLastWait = 0;

    /** For timing debugs */
    public static long startTime = 0;

    /**
     *  Helper method to show the "normal" cursor. Actually will decrement the waitCursorCount
     *  and only show the normal cursor when waitCursorCount <= 0. This enables a bunch of
     *  code to show the wait cursor and keep showing it until all is done.
     */
    public void showNormalCursor() {
        synchronized (CURSOR_MUTEX) {
            waitCursorCount = Math.max(0, waitCursorCount - 1);
            //      System.err.println ("normal:" + waitCursorCount);
            if (waitCursorCount == 0) {
                //              System.err.println ("UI.setNormalCursor");
                long tmp = System.currentTimeMillis();
                //          Trace.msg("*** wait off time since last wait:" + (tmp-timeSinceLastWait));
                //                Trace.msg("*** wait off time since start:"   + (tmp - startTime));
                timeSinceLastWait = tmp;
                setCursor(false, GuiUtils.normalCursor);
            }
        }
    }


    /**
     *  Helper method to clear any outstanding wait cursor calls.
     */
    public void clearWaitCursor() {
        synchronized (CURSOR_MUTEX) {
            waitCursorCount = 0;
            setCursor(false, GuiUtils.normalCursor);
        }
    }

    /**
     * Start reading
     *
     * @param source  the object to read
     */
    public void startReading(Object source) {}

    /**
     * Stop reading
     *
     * @param source  the object to stop reading
     */
    public void stopReading(Object source) {}




    /**
     *  Set the given cursor on all DataControlDialog-s, DataTree windows,
     *  DisplayControl windows. Call startWait/endWait on all IdvWindow-s
     *
     * @param waiting Is waiting or normal
     * @param cursor The cursor
     */
    //    static int cnt = 0;
    //    StringBuffer sb = new StringBuffer();
    private void setCursor(boolean waiting, Cursor cursor) {

        for (int i = 0; i < dcdWindows.size(); i++) {
            DataControlDialog dcd = (DataControlDialog) dcdWindows.get(i);
            dcd.setCursor(cursor);
        }



        IdvWindow.setWaitState(waiting);
        List windows = new ArrayList(IdvWindow.getWindows());
        for (int i = 0; i < windows.size(); i++) {
            IdvWindow window = (IdvWindow) windows.get(i);
            try {
                window.setCursor(cursor);
                if (waiting) {
                    window.startWait();
                } else {
                    window.endWait();
                }
            } catch (Exception exc) {
                System.err.println("Error in setCursor-windows:" + exc);
                exc.printStackTrace();
            }
        }

        for (int i = 0; i < dataSourceHolders.size(); i++) {
            DataSourceHolder holder =
                (DataSourceHolder) dataSourceHolders.get(i);
            IdvWindow window = holder.getFrame();
            if (window != null) {
                try {
                    window.setCursor(cursor);
                } catch (Exception exc) {
                    System.err.println(
                        "Error in setCursor-data source holders:" + exc);
                }
            }
        }

        List displayControls = getIdv().getDisplayControls();
        for (int i = 0; i < displayControls.size(); i++) {
            JFrame window =
                ((DisplayControl) displayControls.get(i)).getWindow();
            if (window != null) {
                try {
                    window.setCursor(cursor);
                } catch (Exception exc) {
                    System.err.println("Error in setCursor-displays:" + exc);
                }
            }
        }
    }



    /**
     * Run through all windows and call show
     */
    public void showAllWindows() {
        //Any windows  that have been created at initialization time
        //are not shown until the end.
        List windows = new ArrayList(IdvWindow.getWindows());
        //Don't do this for now since the controls have windows
        for (int i = 0; i < windows.size(); i++) {
            //            ((IdvWindow) windows.get(i)).setVisible(true);
            //            ((IdvWindow) windows.get(i)).show();
        }

        List displayControls = getIdv().getDisplayControls();
        for (int i = 0; i < displayControls.size(); i++) {
            ((DisplayControl) displayControls.get(i)).toFront();
        }

    }

    /**
     * Run through all windows and call show
     */
    public void toFrontMainWindows() {
        List mainWindows = IdvWindow.getMainWindows();
        for (int i = 0; i < mainWindows.size(); i++) {
            ((IdvWindow) mainWindows.get(i)).show();
            ((IdvWindow) mainWindows.get(i)).toFront();
        }
    }


    /**
     * Associate the given view manager with the given window. We do this
     * so we can find a view manager from its window. We also listen
     * for window closing events to notify the view manager of the closing
     *
     * @param window The window
     * @param viewManagers List of ViewManager-s to associate
     *
     */
    private void associateWindowWithViewManagers(final IdvWindow window,
            final List viewManagers) {
        window.setTheViewManagers(viewManagers);
        if (viewManagers.size() == 0) {
            return;
        }
        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager viewManager = (ViewManager) viewManagers.get(i);
            viewManager.setWindow(window);
        }

        final WindowAdapter[] wa = { null };

        window.addWindowListener(wa[0] = new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                if (lastActiveFrame == window) {
                    return;
                }
                lastActiveFrame = window;
                if ( !getStateManager().isLoadingXml()
                        && getIdv().getHaveInitialized()) {
                    handleWindowActivated(window);
                }
            }

            public void windowClosed(WindowEvent e) {
                removeWindow(window);
                window.removeWindowListener(wa[0]);
            }
        });
    }


    /**
     * Update the last active view manager.
     */
    public void resetLastActiveViewManager() {
        if ((lastActiveFrame != null)
                && (lastActiveFrame.getViewManagers() != null)
                && (lastActiveFrame.getViewManagers().size() > 0)) {
            handleWindowActivated(lastActiveFrame);
        } else {
            getVMManager().setLastActiveViewManager(null);
            List windows = IdvWindow.getMainWindows();
            for (int i = 0; i < windows.size(); i++) {
                handleWindowActivated((IdvWindow) windows.get(i));
            }
        }
    }

    /**
     * Handle when the window is activated. Set the last active view manager
     *
     * @param window The window
     */
    public void handleWindowActivated(IdvWindow window) {
        List        viewManagers            = window.getViewManagers();
        ViewManager viewManagerToMakeActive = null;
        long        lastActivatedTime       = -1;
        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager viewManager = (ViewManager) viewManagers.get(i);
            if (viewManager.getContents() == null) {
                continue;
            }
            if ( !viewManager.getContents().isVisible()) {
                continue;
            }
            lastActiveFrame = window;
            if (viewManager.getLastTimeActivated() > lastActivatedTime) {
                viewManagerToMakeActive = viewManager;
                lastActivatedTime       = viewManager.getLastTimeActivated();
            }
        }

        if (viewManagerToMakeActive != null) {
            getVMManager().setLastActiveViewManager(viewManagerToMakeActive);
            return;
        }
        getVMManager().setLastActiveViewManager(null);
    }

    /**
     * Remove the window from the list of windows
     *
     * @param window The window to remove
     */
    public void removeWindow(IdvWindow window) {
        if (lastActiveFrame == window) {
            lastActiveFrame = null;
            List windows = IdvWindow.getMainWindows();
        }
        //This updates any ViewManager displays
        getVMManager().setLastActiveViewManager(
            getVMManager().getLastActiveViewManager());
        window.destroy();
    }


    /**
     * Update history links
     */
    public void updateHistoryLinks() {}


    /**
     *  Adds the given DataControlDialog to the dcdWindows list. We keep this list
     *  around so we can set the cursor on a  showWaitCursor call.
     *
     * @param dcd The dialog
     * @return The dcd argument
     */
    public DataControlDialog addDCD(DataControlDialog dcd) {
        dcdWindows.add(dcd);
        return dcd;
    }

    /**
     *  Removes the given DataControlDialog from the dcdWindows list. We keep this list
     *  around so we can set the cursor on a  showWaitCursor call.
     *
     * @param dcd The dialog
     */
    public void removeDCD(DataControlDialog dcd) {
        dcdWindows.remove(dcd);
    }


    /**
     * Handle (polymorphically) the {@link ucar.unidata.idv.ui.DataControlDialog}.
     * This dialog is used to either select a display control to create
     * or is used to set the timers used for a {@link ucar.unidata.data.DataSource}.
     *
     * @param dcd The dialog
     */
    public void processDialog(DataControlDialog dcd) {
        DataChoice dataChoice = dcd.getDataChoice();
        DataSource dataSource = dcd.getDataSource();
        if (dataChoice != null) {
            Object[]      selectedControls = dcd.getSelectedControls();
            for (int i = 0; i < selectedControls.length; i++) {
                ControlDescriptor cd =
                    (ControlDescriptor) selectedControls[i];
                DataSelection dataSelection = dcd.getDataSelectionWidget().createDataSelection(cd.doesLevels());
                Hashtable props = new Hashtable();
                List settings = dcd.getDataSelectionWidget().getSelectedSettings();
                if(settings!=null && settings.size()>0) {
                    props.put("initialSettings", settings);
                }
                getIdv().doMakeControl(Misc.newList(dataChoice), cd, props, 
                                       dataSelection);
            }
        } else if (dataSource != null) {
            dataSource.setDateTimeSelection(dcd.getDataSelectionWidget().getSelectedDateTimes());
            dataSourceTimeChanged(dataSource);
        }
    }


    /**
     * Initialize the given menu before it is shown
     *
     * @param id Id of the menu
     * @param menu The menu
     */
    protected void handleMenuSelected(String id, JMenu menu) {
        if (id.equals(MENU_WINDOWS)) {
            menu.removeAll();
            makeWindowsMenu(menu);
        } else if (id.equals("file.newdata") || id.equals("data.newdata")) {
            menu.removeAll();
            GuiUtils.makeMenu(
                menu,
                getIdvChooserManager().makeChooserMenus(new ArrayList()));
        } else if (id.equals(MENU_NEWVIEWS)) {
            menu.removeAll();
            makeViewStateMenu(menu);
        } else if (id.equals(MENU_HISTORY)) {
            historyMenuSelected(menu);

        } else if (id.equals(MENU_EDITFORMULAS)) {
            editFormulasMenuSelected(menu);
        } else if (id.equals(MENU_DELETEHISTORY)) {
            deleteHistoryMenuSelected(menu);
        } else if (id.equals(MENU_DELETEVIEWS)) {
            menu.removeAll();
            makeDeleteViewsMenu(menu);
        } else if (id.equals(MENU_DISPLAYS)) {
            menu.removeAll();
            initializeDisplayMenu(menu);
        } else if (id.equals(MENU_MAPS)) {
            if (menu.getItemCount() == 0) {
                processMapMenu(menu, false);
            }
        } else if (id.equals(MENU_LOCATIONS)) {
            if (menu.getItemCount() == 0) {
                Msg.addDontComponent(menu);
                processStationMenu(menu, false);
            }
        } else if (id.equals(MENU_SPECIAL)) {
            if (menu.getItemCount() == 0) {
                processStandAloneMenu(menu, false);
            }
        } else if (id.equals(MENU_VIEW)) {
            //            menu.removeAll();
            //            initializeViewMenu(menu);
        } else if (id.equals(MENU_DATA)) {
            updateDataMenu(menu);
        }
    }



    /**
     * DeInitialize the given menu before it is shown
     *
     * @param id Id of the menu
     * @param menu The menu
     */
    protected void handleMenuDeSelected(String id, JMenu menu) {
        if (id.equals(MENU_DISPLAYS)) {
            menu.removeAll();
        } else if (id.equals(MENU_DATA)) {
            menu.removeAll();
        }
    }


    /** The different menu ids */
    private Hashtable menuIds;

    /**
     * Get the map of menu ids
     *
     * @return menus
     */
    public Hashtable getMenuIds() {
        return menuIds;
    }


    /**
     * Make the menu bar and menus for the given IdvWindow. Use the set of xml menu files
     * defined by the menubarResources member
     *
     * @return The menu bar we just created
     */
    public JMenuBar doMakeMenuBar() {

        Hashtable menuMap = new Hashtable();
        menuIds = menuMap;
        JMenuBar menuBar = null;
        XmlResourceCollection xrc = getResourceManager().getXmlResources(
                                        getResourceManager().RSC_MENUBAR);
        menuBar = new JMenuBar();
        for (int i = 0; i < xrc.size(); i++) {
            GuiUtils.processXmlMenuBar(xrc.getRoot(i), menuBar, getIdv(),
                                       menuMap);
        }

        JMenu helpMenu = (JMenu) menuMap.get(MENU_HELP);
        //Move to end
        if (helpMenu != null) {
            menuBar.remove(helpMenu);
            menuBar.add(helpMenu);
        }

        //TODO: Perhaps we will put the different skins in the menu?
        JMenuItem newDisplayMenu = (JMenuItem) menuMap.get(MENU_NEWDISPLAY);
        if (newDisplayMenu != null) {
            final XmlResourceCollection skins =
                getResourceManager().getXmlResources(
                    getResourceManager().RSC_SKIN);

            Hashtable menus = new Hashtable();
            for (int i = 0; i < skins.size(); i++) {
                final Element root = skins.getRoot(i);
                if (root == null) {
                    continue;
                }
                final int skinIndex = i;
                List names = StringUtil.split(skins.getShortName(i), ">",
                                 true, true);
                JMenuItem theMenu = newDisplayMenu;
                String    path    = "";
                for (int nameIdx = 0; nameIdx < names.size() - 1; nameIdx++) {
                    String catName = (String) names.get(nameIdx);
                    path = path + ">" + catName;
                    JMenu tmpMenu = (JMenu) menus.get(path);
                    if (tmpMenu == null) {
                        tmpMenu = new JMenu(catName);
                        theMenu.add(tmpMenu);
                        menus.put(path, tmpMenu);
                    }
                    theMenu = tmpMenu;
                }
                final String name = (String) names.get(names.size() - 1);
                JMenuItem    mi   = new JMenuItem(name);
                theMenu.add(mi);
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        createNewWindow(null, true,
                                        getWindowTitleFromSkin(skinIndex),
                                        skins.get(skinIndex).toString(),
                                        skins.getRoot(skinIndex, false));
                    }
                });
            }
        }


        /*        JMenu newData = (JMenu) menuMap.get("file.newdata");
        if (newData != null) {
            GuiUtils.makeMenu(
                newData,
                getIdvChooserManager().makeChooserMenus(new ArrayList()));
        }
        newData = (JMenu) menuMap.get("data.newdata");
        if (newData != null) {
            List items = new ArrayList();
            getIdvChooserManager().makeChooserMenus(items);
            GuiUtils.makeMenu(newData, items);
            }*/



        final JMenu publishMenu = (JMenu) menuMap.get(MENU_PUBLISH);
        final JMenu publishConfigMenu =
            (JMenu) menuMap.get(MENU_PUBLISH_CONFIG);
        if (publishConfigMenu != null) {
            if ( !getPublishManager().isPublishingEnabled()) {
                publishConfigMenu.getParent().remove(publishConfigMenu);
            } else {
                for (int i = 0;
                        i < getPublishManager().getPublishers().size(); i++) {
                    IdvPublisher p =
                        (IdvPublisher) getPublishManager().getPublishers()
                            .get(i);
                    JMenuItem mi = new JMenuItem(p.getName());
                    publishConfigMenu.add(mi);
                    mi.addActionListener(new ObjectListener(p) {
                        public void actionPerformed(ActionEvent ae) {
                            ((IdvPublisher) theObject).configure();
                        }
                    });
                }

            }
        }

        if (publishMenu != null) {
            if ( !getPublishManager().isPublishingEnabled()) {
                publishMenu.getParent().remove(publishMenu);
            }
        }


        for (Enumeration keys = menuMap.keys(); keys.hasMoreElements(); ) {
            final String menuId = (String) keys.nextElement();
            if ( !(menuMap.get(menuId) instanceof JMenu)) {
                continue;
            }
            final JMenu menu = (JMenu) menuMap.get(menuId);
            menu.addMenuListener(new MenuListener() {
                public void menuCanceled(MenuEvent e) {}

                public void menuDeselected(MenuEvent e) {
                    handleMenuDeSelected(menuId, menu);
                }

                public void menuSelected(MenuEvent e) {
                    handleMenuSelected(menuId, menu);
                }
            });
        }

        return menuBar;
    }


    /**
     * Get the skin's HTML
     *
     * @return  the skin's name
     */
    public String getSkinHtml() {
        StringBuffer html =
            new StringBuffer("<html><body><h2>Create New Windows</h2><ul>");
        final XmlResourceCollection skins =
            getResourceManager().getXmlResources(
                getResourceManager().RSC_SKIN);
        List      cats  = new ArrayList();
        Hashtable buffs = new Hashtable();
        for (int i = 0; i < skins.size(); i++) {
            final Element root = skins.getRoot(i);
            if (root == null) {
                continue;
            }
            List names = StringUtil.split(skins.getShortName(i), ">", true,
                                          true);
            String path = "";
            for (int nameIdx = 0; nameIdx < names.size() - 1; nameIdx++) {
                String catName = (String) names.get(nameIdx);
                if (path.length() > 0) {
                    path = path + "&gt;";
                }
                path = path + catName;
            }
            StringBuffer sb = (StringBuffer) buffs.get(path);
            if (sb == null) {
                sb = new StringBuffer();
                buffs.put(path, sb);
                cats.add(path);
            }
            String name = (String) names.get(names.size() - 1);
            String action = "jython:idv.getIdvUIManager().loadSkinByIndex("
                            + i + ")";
            sb.append("<li> <A href=\"" + action + "\"> " + name + "</a>\n");
        }
        for (int i = 0; i < cats.size(); i++) {
            html.append("<li> " + cats.get(i)
                        + " <ul style=\"margin-top:0;\">");
            html.append(buffs.get(cats.get(i)));
            html.append("</ul>");
        }

        return html.toString();
    }


    /**
     * Load the skin by index
     *
     * @param skinIndex  index for the skin
     */
    public void loadSkinByIndex(int skinIndex) {
        XmlResourceCollection skins = getResourceManager().getXmlResources(
                                          getResourceManager().RSC_SKIN);
        Element root = skins.getRoot(skinIndex, false);
        createNewWindow(null, true, getWindowTitleFromSkin(skinIndex),
                        skins.get(skinIndex).toString(),
                        skins.getRoot(skinIndex, false));
    }


    /**
     * Make the windows menu. This lists all of the current windows
     * and allows the user to show them.
     *
     * @param menu windows menu
     */
    public void makeWindowsMenu(JMenu menu) {
        IdvWindow activeWindow = IdvWindow.getActiveWindow();
        if (activeWindow != null) {
            //            menu.add(M
            //            menu.addSeparator();
        }

        List windows = IdvWindow.getWindows();
        for (int i = 0; i < windows.size(); i++) {
            IdvWindow window = (IdvWindow) windows.get(i);
            menu.add(GuiUtils.makeMenuItem(window.getTitle(), window,
                                           "show"));
        }
    }


    /**
     *  This adds to the given menu a set of MenuItems, one for each saved viewmanager
     *  in the vmState list.
     *
     * @param menu The menu
     */
    public void makeViewStateMenu(JMenu menu) {
        makeViewStateMenu(menu, null);
    }

    /**
     *  This adds to the given menu a set of MenuItems, one for each saved viewmanager
     *  in the vmState list. If the ViewManager parameter vm is non-null
     *  then  the result of the selection will be to apply the selected ViewManager
     *  state to the given vm. Else a new window will be created with a new ViewManager.
     *
     * @param menu The menu
     * @param vm The view manager
     */
    public void makeViewStateMenu(JMenu menu, final ViewManager vm) {
        List vms = getVMManager().getVMState();
        if (vms.size() == 0) {
            menu.add(new JMenuItem(Msg.msg("No Saved Views")));
        }
        for (int i = 0; i < vms.size(); i++) {
            TwoFacedObject tfo = (TwoFacedObject) vms.get(i);
            JMenuItem      mi  = new JMenuItem(tfo.getLabel().toString());
            menu.add(mi);
            mi.addActionListener(new ObjectListener(tfo.getId()) {
                public void actionPerformed(ActionEvent ae) {
                    if (vm == null) {
                        ViewManager otherView = (ViewManager) theObject;
                        //Create a new VM in its own window (thus the true, true)
                        //TODO: Do something with this
                        /*ViewManager newVM = getVMManager().xgetViewManager(
                          otherView.getViewDescriptor(),
                          true, true, null);*/
                        //                        newVM.initWith(otherView);
                    } else {
                        vm.initWith((ViewManager) theObject, true);
                    }

                }
            });
        }
    }



    /**
     *  This adds to the given menu a set of MenuItems, one for each saved ViewManager, for
     *  deleting the selected saved ViewManager.
     *
     * @param menu The menu
     */
    public void makeDeleteViewsMenu(JMenu menu) {
        List vms = getVMManager().getVMState();
        if (vms.size() == 0) {
            menu.add(new JMenuItem(Msg.msg("No Saved Views")));
        }
        for (int i = 0; i < vms.size(); i++) {
            TwoFacedObject tfo = (TwoFacedObject) vms.get(i);
            JMenuItem mi = new JMenuItem("Delete "
                                         + tfo.getLabel().toString());
            menu.add(mi);
            mi.addActionListener(new ObjectListener(tfo) {
                public void actionPerformed(ActionEvent ae) {
                    if ( !GuiUtils.askYesNo(
                            "Delete Saved View",
                            "Are you sure you want to delete the saved view: "
                            + theObject + " ?")) {
                        return;
                    }
                    getIdv().getVMManager().getVMState().remove(theObject);
                    getIdv().getVMManager().writeVMState();
                }
            });
        }
    }


    public void editDisplaySettings() {
        DisplaySettingsDialog dsd = new DisplaySettingsDialog(getIdv());
    }

    /**
     * Add in the dynamic menu for editing formulas
     *
     * @param menu edit menu to add to
     */
    public void editFormulasMenuSelected(JMenu menu) {
        menu.removeAll();
        GuiUtils.makeMenu(menu, getJythonManager().doMakeEditMenuItems());
    }




    /**
     * User just clicked on the file-history menu. Add in the items
     *
     * @param fileMenu The menu to fill
     */
    public void historyMenuSelected(JMenu fileMenu) {
        fileMenu.removeAll();
        //Make sure we read in the preference list  of past files
        List historyList = getIdv().getHistory();
        if ((historyList != null) && (historyList.size() > 0)) {
            for (int i = 0; i < historyList.size(); i++) {
                //the triple list holds (type, name, id, properties);
                History   history = (History) historyList.get(i);
                JMenuItem mi      = new JMenuItem(history.toString());
                fileMenu.add(mi);
                mi.addActionListener(new ObjectListener(history) {
                    public void actionPerformed(ActionEvent ae) {
                        Misc.run(new Runnable() {
                            public void run() {
                                try {
                                    showWaitCursor();
                                    ((History) theObject).process(getIdv());
                                    showNormalCursor();
                                } catch (Throwable exc) {
                                    logException(
                                        "Creating data source from history",
                                        exc);
                                }
                            }
                        });
                    }
                });
            }
        } else {
            fileMenu.add(new JMenuItem("No Files"));
        }
    }




    /**
     * Update all of the data menus that may exist
     *
     * @param dataMenu The menu
     */
    private void updateDataMenu(JMenu dataMenu) {
        JMenuItem mi;
        JMenu     fixedMenu  = null;
        List      fixedComps = (List) fixedDataMenuItems.get(dataMenu);
        if (fixedComps == null) {
            fixedComps = new ArrayList();
            for (int i = 0; i < dataMenu.getMenuComponentCount(); i++) {
                fixedComps.add(dataMenu.getMenuComponent(i));
            }
            fixedDataMenuItems.put(dataMenu, fixedComps);
        } else {
            dataMenu.removeAll();
            for (int i = 0; i < fixedComps.size(); i++) {
                dataMenu.add((Component) fixedComps.get(i));
            }
        }



        //        dataMenu.add(mi = new JMenuItem("New Data Source..."));
        //mi.setActionCommand("jython:idv.showChooser();");
        //        mi.addActionListener(getIdv());
        fixedMenu =
            getIdv().getIdvChooserManager().addUserChooserToMenu(dataMenu,
                fixedMenu);

        processBundleMenu(dataMenu, IdvPersistenceManager.BUNDLES_DATA);
        // dataMenu.addSeparator();

        List dataSources = new ArrayList(getIdv().getDataSources());
        if (getIdv().getJythonManager().getDescriptorDataSource() != null) {
            dataSources.add(
                0, getIdv().getJythonManager().getDescriptorDataSource());
        }


        for (int i = 0; i < dataSources.size(); i++) {
            DataSource dataSource = (DataSource) dataSources.get(i);
            JMenu dataSourceMenu =
                new JMenu(DataSelector.getNameForDataSource(dataSource));
            dataSourceMenu.setToolTipText(dataSource.toString());
            dataMenu.add(dataSourceMenu);

            JMenu editMenu = GuiUtils.makeMenu("Edit",
                                 doMakeDataSourceMenuItems(dataSource, null));
            dataSourceMenu.add(editMenu);
            dataSourceMenu.addSeparator();


            /*
        if ( !DataManager.isFormulaDataSource(dataSource)) {
            mi = new JMenuItem("Remove");
            mi.addActionListener(new ObjectListener(dataSource) {
                public void actionPerformed(ActionEvent e) {
                    getIdv().removeDataSource((DataSource) theObject);
                    //Clear for mem. leaks
                    theObject = null;
                }
            });
            dataSourceMenu.add(mi);
            dataSourceMenu.addSeparator();
        } else {
            mi = new JMenuItem("Create Formula");
            mi.addActionListener(new ObjectListener(dataSource) {
                public void actionPerformed(ActionEvent e) {
                    getIdv().getJythonManager().showFormulaDialog();
                }
            });
            dataSourceMenu.add(mi);
            dataSourceMenu.addSeparator();
        }
            */
            addChoicesToMenu(dataSource, dataSourceMenu, dataMenu);
        }

    }





    /**
     * Fill in the delete history    menu
     *
     * @param fileMenu the menu to fill
     */
    public void deleteHistoryMenuSelected(JMenu fileMenu) {
        fileMenu.removeAll();
        //Make sure we read in the preference list  of past files
        List      historyList = getIdv().getHistory();
        JMenuItem mi;
        if ((historyList != null) && (historyList.size() > 0)) {
            mi = new JMenuItem("Remove All");
            fileMenu.add(mi);
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    getIdv().clearHistoryList();
                }
            });
            fileMenu.addSeparator();

            for (int i = 0; i < historyList.size(); i++) {
                Object history = historyList.get(i);

                JMenu  menu    = new JMenu(history.toString());
                fileMenu.add(menu);
                mi = new JMenuItem("Remove");
                menu.add(mi);
                mi.addActionListener(new ObjectListener(new Integer(i)) {
                    public void actionPerformed(ActionEvent ae) {
                        int  index       = ((Integer) theObject).intValue();
                        List historyList = getIdv().getHistory();
                        historyList.remove(index);
                        getIdv().writeHistoryList();
                    }
                });

                mi = new JMenuItem("Set Alias");
                menu.add(mi);
                mi.addActionListener(new ObjectListener(history) {
                    public void actionPerformed(ActionEvent ae) {
                        getIdv().getHistory();
                        History history = (History) theObject;
                        String alias = GuiUtils.getInput(" ", "Alias: ",
                                           history.getAlias());
                        if (alias != null) {
                            history.setAlias(alias);
                        }
                        getIdv().writeHistoryList();
                    }
                });


            }
        } else {
            fileMenu.add(new JMenuItem("No History"));
        }
    }



    /**
     * Add the menu items for dealing with the
     * {@link ucar.unidata.data.DataChoice}s from the
     * given dataSource into the given menu.
     *
     * @param dataSource The data source
     * @param dataSourceMenu Its menu
     * @param dataMenu  The menu to hold the data choice items
     */
    public void addChoicesToMenu(DataSource dataSource, JMenu dataSourceMenu,
                                 JMenu dataMenu) {
        List      choices   = dataSource.getDataChoices();
        Hashtable catToMenu = new Hashtable();
        for (int i = 0; i < choices.size(); i++) {
            DataChoice choice = (DataChoice) choices.get(i);
            if ( !choice.getForUser()) {
                continue;
            }
            DataCategory topCategory = choice.getDisplayCategory();
            JMenu        parent      = dataSourceMenu;
            String       catPath     = null;
            while (topCategory != null) {
                String catName = topCategory.getName();
                if ( !catName.equals("skip")) {
                    if (catPath == null) {
                        catPath = catName;
                    } else {
                        catPath = catPath + "." + catName;
                    }
                    JMenu nextMenu = (JMenu) catToMenu.get(catPath);
                    if (nextMenu == null) {
                        nextMenu = new JMenu(catName);
                        parent.add(nextMenu);
                        catToMenu.put(catPath, nextMenu);
                    }
                    parent = nextMenu;
                }
                topCategory = topCategory.getChild();
            }
            createDataChoiceMenuItem(choice, parent, dataMenu);
        }
    }




    /**
     * Create the menu items for dealing with the given data choice
     *
     * @param choice The data choice
     * @param parentMenu The parent menu
     * @param dataMenu Where to put the items
     */
    public void createDataChoiceMenuItem(DataChoice choice, JMenu parentMenu,
                                         final JMenu dataMenu) {
        boolean showIcons = getIdv().getProperty("idv.ui.datatree.showicons",
                                true);

        boolean   isComposite = (choice instanceof CompositeDataChoice);
        boolean   isDerived   = (choice instanceof DerivedDataChoice);

        ImageIcon icon        = null;
        if (showIcons) {
            if (isDerived) {
                icon = DataTree.getDerivedIcon();
            } else {
                String iconPath = choice.getProperty(DataChoice.PROP_ICON,
                                      (String) null);
                if (iconPath != null) {
                    icon = GuiUtils.getImageIcon(iconPath, true);
                }
            }
        }


        JMenuItem choiceMenuItem = ((icon != null)
                                    ? new JMenuItem(choice.toString(), icon)
                                    : new JMenuItem(choice.toString()));
        choiceMenuItem.addActionListener(new ObjectListener(choice) {
            public void actionPerformed(ActionEvent e) {
                Point loc = dataMenu.getLocationOnScreen();
                //This is modal
                new DataControlDialog(getIdv(), (DataChoice) theObject,
                                      loc.x, loc.y);
            }
        });

        int itemCount = parentMenu.getItemCount();
        while (itemCount > 15) {
            JMenu moreMenu = null;
            for (int itemIdx = 0; (moreMenu == null) && (itemIdx < itemCount);
                    itemIdx++) {
                JMenuItem item = parentMenu.getItem(itemIdx);
                if (item == null) {
                    continue;
                }
                if (item.getText().equals("More")
                        && (item instanceof JMenu)) {
                    moreMenu = (JMenu) item;
                }
            }
            if (moreMenu == null) {
                moreMenu = new JMenu("More");
                parentMenu.add(moreMenu);
            }
            parentMenu = moreMenu;
            itemCount  = parentMenu.getItemCount();
        }



        parentMenu.add(choiceMenuItem);
        if (isComposite) {
            JMenu compositeMenu = new JMenu("Sub: " + choice.toString());
            parentMenu.add(compositeMenu);
            List children = ((CompositeDataChoice) choice).getDataChoices();
            for (int i = 0; i < children.size(); i++) {
                DataChoice theChild = (DataChoice) children.get(i);
                createDataChoiceMenuItem(theChild, compositeMenu, dataMenu);
            }
        }
    }


    /**
     * Pass through to the idv to load in the given bundle
     *
     * @param bundle The bundle to create
     */
    public void processBundle(SavedBundle bundle) {
        showWaitCursor();
        LogUtil.message("Loading bundle: " + bundle.getName());
        getPersistenceManager().decodeXmlFile(bundle.getUrl(),
                bundle.getName(), true);
        LogUtil.message("");
        showNormalCursor();
    }



    /**
     * Create the bundle menu for the given list of bundles
     *
     * @param displayMenu The menu to add the bundle menu into
     * @param bundleType Is this for the favorites or the display templates
     */
    protected void processBundleMenu(JMenu displayMenu,
                                     final int bundleType) {
        final List bundles = getPersistenceManager().getBundles(bundleType);
        if (bundles.size() == 0) {
            return;
        }
        final String title =
            getPersistenceManager().getBundleTitle(bundleType);
        final String bundleDir =
            getPersistenceManager().getBundleDirectory(bundleType);

        JMenu bundleMenu = new JMenu(title);
        bundleMenu.setMnemonic(GuiUtils.charToKeyCode(title));
        JMenuItem mi;

        getPersistenceManager().initBundleMenu(bundleType, bundleMenu);

        mi = new JMenuItem("Manage...");
        mi.setMnemonic(GuiUtils.charToKeyCode("M"));
        bundleMenu.add(mi);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showBundleDialog(bundleType);
            }
        });
        bundleMenu.addSeparator();


        Hashtable catMenus = new Hashtable();
        displayMenu.add(bundleMenu);
        for (int i = 0; i < bundles.size(); i++) {
            SavedBundle bundle       = (SavedBundle) bundles.get(i);
            List        categories   = bundle.getCategories();
            JMenu       catMenu      = bundleMenu;
            String      mainCategory = "";
            for (int catIdx = 0; catIdx < categories.size(); catIdx++) {
                String category = (String) categories.get(catIdx);
                mainCategory += "." + category;
                JMenu tmpMenu = (JMenu) catMenus.get(mainCategory);
                if (tmpMenu == null) {
                    tmpMenu = new JMenu(category);
                    catMenu.add(tmpMenu);
                    catMenus.put(mainCategory, tmpMenu);
                }
                catMenu = tmpMenu;

            }

            final SavedBundle theBundle = bundle;
            mi = new JMenuItem(bundle.getName());
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    //Do it in a thread
                    Misc.run(IdvUIManager.this, "processBundle", theBundle);
                }
            });
            catMenu.add(mi);
        }
    }


    /**
     * Add the map menu into the display menu
     *
     * @param displayMenu The display menu
     * @param makeNew  if true, make a new menu
     */
    protected void processMapMenu(JMenu displayMenu, boolean makeNew) {
        JMenuItem mi;
        JMenu     mapMenu;
        if (makeNew) {
            mapMenu = new JMenu("Maps and Backgrounds");
            displayMenu.add(mapMenu);
        } else {
            mapMenu = displayMenu;
        }

        List maps = getResourceManager().getMaps();


        mi = new JMenuItem("Add Your Own Map...");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                addMap(null);
            }
        });
        mapMenu.add(mi);

        mi = new JMenuItem("Add Default Maps");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                addDefaultMap();
            }
        });
        mapMenu.add(mi);
        JMenu systemMenu = new JMenu("Add System Map");
        mapMenu.add(systemMenu);


        for (int i = 0; i < maps.size(); i++) {
            final MapData mapData = (MapData) maps.get(i);
            mi = new JMenuItem(mapData.getDescription());
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    addMap(mapData);
                }
            });
            systemMenu.add(mi);
        }

        mi = new JMenuItem("Add Background Image");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                getIdv().doMakeBackgroundImage();
            }
        });
        mapMenu.add(mi);


    }


    /**
     * Add the station menu into the display menu
     *
     * @param displayMenu The display menu
     * @param makeNew  if true, make a new menu
     */
    protected void processStationMenu(JMenu displayMenu, boolean makeNew) {
        ControlDescriptor locationDescriptor =
            getIdv().getControlDescriptor("locationcontrol");
        if (locationDescriptor != null) {
            List           stations = getIdv().getLocationList();
            ObjectListener listener = new ObjectListener(locationDescriptor) {
                public void actionPerformed(ActionEvent ae, Object obj) {
                    addStationDisplay((NamedStationTable) obj,
                                      (ControlDescriptor) theObject);
                }
            };
            List menuItems = NamedStationTable.makeMenuItems(stations,
                                 listener);
            if (makeNew) {
                displayMenu.add(GuiUtils.makeMenu("Locations", menuItems));
            } else {
                GuiUtils.makeMenu(displayMenu, menuItems);
            }
        }
    }


    /**
     * Add the stand alone displays into the display menu
     *
     * @return List of ControlDescriptor-s that can stand alone.
     */
    public List getStandAloneControlDescriptors() {
        List result             = new ArrayList();
        List controlDescriptors = getIdv().getControlDescriptors();
        for (int i = 0; i < controlDescriptors.size(); i++) {
            ControlDescriptor cd =
                (ControlDescriptor) controlDescriptors.get(i);
            if (cd.canStandAlone()) {
                result.add(cd);
            }
        }
        return result;
    }

    /**
     * Add the stand alone displays into the display menu
     *
     * @param displayMenu The display menu
     * @param makeNew  if true, make a new menu
     */
    protected void processStandAloneMenu(JMenu displayMenu, boolean makeNew) {
        JMenuItem mi;
        JMenu     standAloneMenu;
        if (makeNew) {
            standAloneMenu = new JMenu("Special");
            displayMenu.add(standAloneMenu);
        } else {
            standAloneMenu = displayMenu;
        }

        List controlDescriptors = getStandAloneControlDescriptors();
        for (int i = 0; i < controlDescriptors.size(); i++) {
            ControlDescriptor cd =
                (ControlDescriptor) controlDescriptors.get(i);
            mi = new JMenuItem(cd.getLabel());
            mi.addActionListener(new ObjectListener(cd) {
                public void actionPerformed(ActionEvent ev) {
                    getIdv().doMakeControl(new ArrayList(),
                                           (ControlDescriptor) theObject);
                }
            });
            standAloneMenu.add(mi);
        }
    }



    /**
     * Add the instances into the display menu
     *
     * @param displayMenu The display menu
     */
    protected void processInstanceMenu(JMenu displayMenu) {
        JMenuItem mi;
        List      displayControls = getIdv().getDisplayControls();
        displayMenu.addSeparator();



        JMenu viewMenu = new JMenu("Current View");
        initializeViewMenu(viewMenu);
        displayMenu.add(viewMenu);




        JMenu instanceMenu = null;
        for (int i = 0; i < displayControls.size(); i++) {
            DisplayControl c = (DisplayControl) displayControls.get(i);
            mi = new JMenuItem(c.getMenuLabel());
            mi.addActionListener(new ObjectListener(c) {
                public void actionPerformed(ActionEvent ev) {
                    ((DisplayControl) theObject).show();
                    ((DisplayControl) theObject).toFront();
                }
            });
            if (instanceMenu == null) {
                instanceMenu = new JMenu("Current Displays");
                displayMenu.add(instanceMenu);
            }
            instanceMenu.add(mi);
        }




    }

    /**
     * Add in the menu items for the given view menu
     *
     * @param viewMenu The view menu
     */
    protected void initializeViewMenu(JMenu viewMenu) {
        ViewManager vm = getIdv().getVMManager().getLastActiveViewManager();
        //      vm = null;
        if (vm == null) {
            viewMenu.add(new JMenuItem("No active views"));
            return;
        }
        List menus = vm.doMakeMenuList();
        for (int i = 0; i < menus.size(); i++) {
            viewMenu.add((JMenuItem) menus.get(i));
        }
    }



    /**
     * Add in the menu items for the given display menu
     *
     * @param displayMenu The display menu
     */
    protected void initializeDisplayMenu(JMenu displayMenu) {
        processBundleMenu(displayMenu,
                          IdvPersistenceManager.BUNDLES_FAVORITES);
        processBundleMenu(displayMenu, IdvPersistenceManager.BUNDLES_DISPLAY);

        processMapMenu(displayMenu, true);
        processStationMenu(displayMenu, true);
        processStandAloneMenu(displayMenu, true);
        processInstanceMenu(displayMenu);
        Msg.translateTree(displayMenu);
    }



    /**
     * Popup the favorites manage dialog
     */
    public void showBundleDialog() {
        showBundleDialog(IdvPersistenceManager.BUNDLES_FAVORITES);
    }


    /**
     * Popup the dialog that edits the list of bundles
     *
     * @param bundleType What is the type of the bundle we are showing an edit dialog for
     */
    public void showBundleDialog(final int bundleType) {
        Object     key  = new Integer(bundleType);
        BundleTree tree = (BundleTree) bundleTrees.get(key);
        if (tree == null) {
            tree = new BundleTree(this, bundleType);
            bundleTrees.put(key, tree);
        }
        tree.show();
    }



    /**
     * Use this to notify when any of the display templates changed.
     * This passes through the calls to the data source holders and
     * recreates the display menus
     */
    public void displayTemplatesChanged() {
        displayControlsChanged();
        for (int i = 0; i < dataSourceHolders.size(); i++) {
            ((DataSourceHolder) dataSourceHolders.get(
                i)).displayTemplatesChanged();
        }
        updateToolbars();
        //Update the bundle trees
        for (Enumeration keys =
                bundleTrees.keys(); keys.hasMoreElements(); ) {
            ((BundleTree) bundleTrees.get(keys.nextElement())).loadBundles();
        }
    }

    /**
     * Use this to notify when any of the favorite bundles changed.
     * This updates the toolbars and bundle trees
     */
    public void favoriteBundlesChanged() {
        updateToolbars();
        //Update the bundle trees
        for (Enumeration keys =
                bundleTrees.keys(); keys.hasMoreElements(); ) {
            ((BundleTree) bundleTrees.get(keys.nextElement())).loadBundles();
        }
    }


    /**
     * Called by the IDV when there has been a change to the display controls.
     */
    public void displayControlsChanged() {}

    /**
     * The display changed
     *
     * @param displayControl display that changed
     */
    public void displayControlChanged(DisplayControl displayControl) {
        if (getViewPanel() != null) {
            getViewPanel().displayControlChanged(displayControl);
        }
    }



    /**
     * A utility method to add the given menu item into either the JPopupMenu or the JMenu
     *
     * @param menu The JPopupMenu or the JMenu
     * @param mi The item to add
     */
    private void addToMenu(JComponent menu, JMenuItem mi) {
        if (menu instanceof JPopupMenu) {
            ((JPopupMenu) menu).add(mi);
        } else {
            ((JMenu) menu).add(mi);
        }
    }

    /**
     * This will someday be used to put in the Toolbar favorites into  a Gui toolbar
     */
    private void updateToolbars() {

        ImageIcon fileIcon =
            GuiUtils.getImageIcon("/auxdata/ui/icons/File.gif");
        ImageIcon catIcon =
            GuiUtils.getImageIcon("/auxdata/ui/icons/Folder.gif");
        List windows = IdvWindow.getWindows();
        List bundles = getPersistenceManager().getBundles(
                           IdvPersistenceManager.BUNDLES_FAVORITES);
        List toolbarBundles = new ArrayList();
        for (int bundleIdx = 0; bundleIdx < bundles.size(); bundleIdx++) {
            SavedBundle bundle = (SavedBundle) bundles.get(bundleIdx);
            if ((bundle.getCategories().size() == 0)
                    || !bundle.getCategories().get(0).equals(
                        IdvPersistenceManager.CAT_TOOLBAR)) {
                continue;
            }
            toolbarBundles.add(bundle);
        }
        ImageIcon separatorIcon =
            GuiUtils.getImageIcon("/auxdata/ui/icons/Separator.gif");



        for (int i = 0; i < windows.size(); i++) {
            JPanel toolbar = (JPanel) ((IdvWindow) windows.get(
                                 i)).getComponent(COMP_FAVORITESBAR);
            if (toolbar == null) {
                continue;
            }

            toolbar.removeAll();
            toolbar.setVisible(false);
            List      comps = new ArrayList();
            Hashtable menus = new Hashtable();
            //            JMenuBar menuBar = new JMenuBar();
            for (int bundleIdx = 0; bundleIdx < toolbarBundles.size();
                    bundleIdx++) {
                final SavedBundle bundle =
                    (SavedBundle) toolbarBundles.get(bundleIdx);
                List           categories = bundle.getCategories();
                ActionListener listener   = new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        getPersistenceManager().open(bundle);
                    }
                };
                if (comps.size() == 0) {
                    comps.add(new JLabel(separatorIcon));
                }


                if (categories.size() == 1) {
                    JButton btn = new JButton(bundle.getName(), fileIcon);
                    btn.setContentAreaFilled(false);
                    btn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2,
                            2));
                    btn.setToolTipText("Click to open favorite: "
                                       + bundle.getName());
                    btn.addActionListener(listener);
                    comps.add(btn);
                    //menuBar.add(btn);
                } else {
                    String     catSoFar   = "";
                    JComponent parentMenu = null;
                    for (int catIdx = 1; catIdx < categories.size();
                            catIdx++) {
                        String category = categories.get(catIdx).toString();
                        catSoFar += "-" + category;
                        JComponent catMenu = (JComponent) menus.get(catSoFar);
                        if (catMenu != null) {
                            parentMenu = catMenu;
                        } else {
                            catMenu = ((parentMenu != null)
                                       ? (JComponent) new JMenu(category)
                                       : (JComponent) new JPopupMenu());
                            menus.put(catSoFar, catMenu);
                            if (parentMenu != null) {
                                addToMenu(parentMenu, (JMenuItem) catMenu);
                            } else {
                                final JPopupMenu popup = (JPopupMenu) catMenu;
                                final JButton b = new JButton(category,
                                                      catIcon);
                                b.setContentAreaFilled(false);
                                b.setToolTipText(
                                    "Click to show favorites category: "
                                    + category);
                                b.setBorder(
                                    BorderFactory.createEmptyBorder(
                                        2, 2, 2, 2));
                                b.addActionListener(new ActionListener() {
                                    public void actionPerformed(
                                            ActionEvent ae) {
                                        popup.show(b, 0, b.getHeight());
                                    }
                                });
                                comps.add(b);
                                //menuBar.add(b);
                            }
                            parentMenu = catMenu;
                        }
                    }
                    if (parentMenu == null) {
                        continue;
                    }
                    JMenuItem mi = new JMenuItem(bundle.getName(), fileIcon);
                    mi.setToolTipText("Click to open favorite: "
                                      + bundle.getName());
                    mi.addActionListener(listener);
                    addToMenu(parentMenu, (JMenuItem) mi);
                }
            }
            //            toolbar.add(menuBar, BorderLayout.WEST);
            if (comps.size() > 1) {
                toolbar.setLayout(new BorderLayout());
                toolbar.add(GuiUtils.inset(GuiUtils.hbox(comps, 4),
                                           new Insets(4, 0, 0,
                                               0)), BorderLayout.WEST);
                toolbar.setVisible(true);
            }
            toolbar.validate();
        }
    }


    /**
     * Add in te default map display
     */
    private void addDefaultMap() {
        ControlDescriptor mapDescriptor =
            getIdv().getControlDescriptor("mapdisplay");
        if (mapDescriptor == null) {
            return;
        }
        String attrs =
            "initializeAsDefault=true;displayName=Default Background Maps;";
        getIdv().doMakeControl(new ArrayList(), mapDescriptor, attrs, null);
    }


    /**
     * Create a MapDisplayControl
     *
     *
     * @param mapData Holds the map  to create
     */
    private void addMap(MapData mapData) {
        ControlDescriptor mapDescriptor =
            getIdv().getControlDescriptor("mapdisplay");
        if (mapDescriptor == null) {
            return;
        }
        Hashtable properties = new Hashtable(mapDescriptor.getProperties());
        properties.put("initialMap", "");
        properties.put("initialMapDescription", "");
        mapDescriptor.initControl(new MapDisplayControl(mapData),
                                  new ArrayList(), getIdv(), properties,
                                  null);
    }



    /**
     * Create the station table display for the given station table
     *
     * @param stationTable The station table
     * @param cd The display control descriptor for the station location display
     */
    public void addStationDisplay(NamedStationTable stationTable,
                                  ControlDescriptor cd) {
        getIdv().doMakeControl(new ArrayList(), cd,
                               "stationTableName="
                               + stationTable.getFullName() + ";", null);
    }




    /**
     *  Return the list of menu items to use when the user has clicked on a DataSource.
     *
     * @param dataSource The data source
     * @param src Where this menu pops up
     * @return List of menu items for dealing with the given dataSource
     */
    public List doMakeDataSourceMenuItems(final DataSource dataSource,
                                          final Component src) {
        List      menuItems = new ArrayList();
        JMenuItem mi;

        if (DataManager.isFormulaDataSource(dataSource)) {
            return getIdv().getJythonManager()
                .doMakeFormulaDataSourceMenuItems(dataSource);
        }

        mi = new JMenuItem("Remove This Data Source");
        mi.addActionListener(new ObjectListener(null) {
            public void actionPerformed(ActionEvent ev) {
                getIdv().removeDataSource(dataSource);
            }
        });
        menuItems.add(mi);
        List l = dataSource.getActions();
        for (int i = 0; i < l.size(); i++) {
            Action a = (Action) l.get(i);
            mi = new JMenuItem((String) a.getValue(a.NAME));
            mi.addActionListener(a);
            menuItems.add(mi);
        }
        menuItems.add(GuiUtils.MENU_SEPARATOR);
        menuItems.add(GuiUtils.makeMenuItem("Save As Favorite",
                                            getPersistenceManager(),
                                            "saveDataSource", dataSource));
        menuItems.add(GuiUtils.makeMenuItem("Properties", dataSource,
                                            "showPropertiesDialog"));


        return menuItems;
    }


    /**
     * This prompts the  user for the alias name for the given dataSource and
     * sets the alias on the data source.
     *
     * @param dataSource The data source to set an alias on
     */
    public void setAlias(DataSource dataSource) {
        String alias = GuiUtils.getInput(" ", "Data source alias: ",
                                         dataSource.getAlias());
        if (alias != null) {
            dataSource.setAlias(alias);
        }
    }




    /**
     * Show the  html description of the given data source.
     *
     * @param dataSource The data source to show details for
     */
    public void showDataSourceDetails(DataSource dataSource) {
        showWaitCursor();
        try {
            String html = dataSource.getFullDescription();
            GuiUtils.showHtmlDialog(html, getIdv());
        } catch (Throwable exc) {
            logException("Getting data source details", exc);
        }
        showNormalCursor();
    }



    /**
     * Show the {@link DataControlDialog} for selecting times
     * for the given dataSource
     *
     * @param dataSource The data source
     * @param src Where to popup the dialog
     * @deprecated Don't use this anymore
     */
    public void showTimeSelection(DataSource dataSource, Component src) {}



    /**
     * Make the menu for the given data choice
     *
     * @param dataChoice The data choice
     * @return The menu
     */
    public JMenu doMakeDataChoiceMenu(DataChoice dataChoice) {
        return GuiUtils.makeMenu("Displays",
                                 doMakeDataChoiceMenuItems(dataChoice, false,
                                     true));
    }

    /**
     * Make the menu items for the given data choice
     *
     * @param dataChoice The data choice
     * @return A list of menu items
     */
    public List doMakeDataChoiceMenus(DataChoice dataChoice) {
        return doMakeDataChoiceMenuItems(dataChoice, false, true);
    }


    /**
     * Make the menu items for the given data choice
     *
     * @param dataChoice The data choice
     * @param isUserFormula Is this data choice a user formula
     * @param showAll If true then add in the items for creating applicable
     * displays
     * @return List of menu items
     */
    public List doMakeDataChoiceMenuItems(final DataChoice dataChoice,
                                          boolean isUserFormula,
                                          boolean showAll) {
        List items = new ArrayList();
        if (isUserFormula) {
            getIdv().getJythonManager().doMakeDataChoiceMenuItems(dataChoice,
                    items);
        }


        if (showAll) {
            if ( !items.isEmpty()) {
                items.add(GuiUtils.MENU_SEPARATOR);
            }

            List l = ControlDescriptor.getApplicableControlDescriptors(
                         dataChoice.getCategories(),
                         getIdv().getControlDescriptors());
            for (int i = 0; i < l.size(); i++) {
                ControlDescriptor dd = (ControlDescriptor) l.get(i);
                JMenuItem         mi = new JMenuItem(dd.getLabel());
                mi.addActionListener(new ObjectListener(dd) {
                    public void actionPerformed(ActionEvent ev) {
                        getIdv().doMakeControl(dataChoice,
                                (ControlDescriptor) theObject, NULL_STRING);
                    }
                });
                items.add(mi);

            }
        }
        return items;
    }





    /**
     * Creates the icon buttons for creating display controls
     *
     * @param vertical Alignment is vertical
     * @return Panel holding the icon buttons
     */
    public JPanel doMakeControlButtons(boolean vertical) {
        List buttons            = new ArrayList();
        List controlDescriptors = getIdv().getControlDescriptors();
        for (int i = 0; i < controlDescriptors.size(); i++) {
            ControlDescriptor dd =
                (ControlDescriptor) controlDescriptors.get(i);
            if (dd.getIcon() == null) {
                continue;
            }
            JButton btn =
                GuiUtils.getImageButton(GuiUtils.getImageIcon(dd.getIcon(),
                    getIdvClass()));
            buttons.add(btn);
            btn.setToolTipText(dd.getDescription());
            btn.addActionListener(new ObjectListener(dd) {
                public void actionPerformed(ActionEvent event) {
                    getIdv().doMakeControl(
                        selectDataChoice((ControlDescriptor) theObject),
                        (ControlDescriptor) theObject, NULL_STRING);
                }
            });
        }
        if (vertical) {
            return GuiUtils.vbox(buttons);
        }
        return GuiUtils.hbox(buttons);
    }





    /**
     *  Popup a menu for a datachoice object over the tree component at (x,y)
     *
     * @param dataTree The data tree to show a menu for
     * @param event  The click
     * @param showFullMenu Should show the full menu
     */
    public void showDataTreeMenu(DataTree dataTree, MouseEvent event,
                                 boolean showFullMenu) {
        Object object = dataTree.getObjectAt(event.getX(), event.getY());
        if (object == null) {
            return;
        }
        List items = null;
        if (object instanceof DataChoice) {
            DataChoice choice = (DataChoice) object;
            items = doMakeDataChoiceMenuItems(choice,
                    choice.isEndUserFormula(), showFullMenu);
        } else if (object instanceof DataSource) {
            if (DataManager.isFormulaDataSource(object)) {
                items = getIdv().getJythonManager()
                    .doMakeFormulaDataSourceMenuItems((DataSource) object);
            } else {
                items = doMakeDataSourceMenuItems((DataSource) object,
                        dataTree.getContents());
            }
        }

        if ((items != null) && (items.size() > 0)) {
            JPopupMenu menu = GuiUtils.makePopupMenu(items);
            Msg.translateTree(menu);
            if (menu != null) {
                menu.show(dataTree.getTree(), event.getX(), event.getY());
            }
        }

    }


    /**
     *  Create (if null)  and show the HelpTipDialog. If checkPrefs is true
     *  then only create the dialog if the PREF_HELPTIPSHOW preference is true.
     *
     * @param checkPrefs Should the user preferences be checked
     */
    public void initHelpTips(boolean checkPrefs) {
        try {
            if (getIdv().getArgsManager().getIsOffScreen()) {
                return;
            }


            if (checkPrefs) {
                if ( !getStore().get(HelpTipDialog.PREF_HELPTIPSHOW, true)) {
                    return;
                }
            }
            if (helpTipDialog == null) {
                IdvResourceManager resourceManager = getResourceManager();
                helpTipDialog = new HelpTipDialog(
                    resourceManager.getXmlResources(
                        resourceManager.RSC_HELPTIPS), getIdv(), getStore(),
                            getIdvClass(),
                            getStore().get(
                                HelpTipDialog.PREF_HELPTIPSHOW, true));
            }
            helpTipDialog.setVisible(true);
            GuiUtils.toFront(helpTipDialog);
        } catch (Throwable excp) {
            logException("Reading help tips", excp);
        }
    }




    /**
     *  If created, close the HelpTipDialog window.
     */
    public void closeHelpTips() {
        if (helpTipDialog != null) {
            helpTipDialog.setVisible(false);
        }
    }

    /**
     *  Create (if null)  and show the HelpTipDialog
     */
    public void showHelpTips() {
        initHelpTips(false);
    }

    /**
     *  Show a message in the splash screen (if it exists)
     *
     * @param m The message to show
     */
    public void splashMsg(String m) {
        if (splash != null) {
            splash.splashMsg(m);
        }
    }

    /**
     *  Close and dispose of the splash window (if it has been created).
     */
    public void splashClose() {
        if (splash != null) {
            splash.doClose();
        }
    }



    /**
     *  Popup the about dialog. Show the title and the version.
     */
    public void about() {

        JLabel iconLbl = new JLabel(
                             GuiUtils.getImageIcon(
                                 getIdv().getProperty(PROP_SPLASHICON, "")));
        StringBuffer buf = new StringBuffer();
        buf.append("<h1>");
        buf.append(getStateManager().getTitle());
        buf.append("</h1>");
        buf.append(getStateManager().getVersionAbout());
        String      text   = buf.toString();
        JEditorPane editor = new JEditorPane();
        editor.setEditable(false);
        editor.setContentType("text/html");
        editor.setText(text);
        JPanel tmp = new JPanel();
        editor.setBackground(tmp.getBackground());
        editor.addHyperlinkListener(getIdv());

        JTabbedPane tab = new JTabbedPane();


        JPanel contents = GuiUtils.leftCenter(iconLbl,
                              GuiUtils.inset(editor, 5));
        tab.add("About", contents);

        StringBuffer javaInfo = new StringBuffer();

        StringBuffer props    = new StringBuffer();
        props.append("<b>Operating System</b>");
        props.append("<br> ");
        props.append("Name: ");
        props.append(System.getProperty("os.name"));
        props.append("<br>");
        props.append("Version: ");
        props.append(System.getProperty("os.version"));
        props.append("<br>");
        props.append("Architecture: ");
        props.append(System.getProperty("os.arch"));
        props.append("<br> ");
        props.append("<b>Java</b>");
        props.append("<br> ");
        props.append("Version: ");
        props.append(System.getProperty("java.version"));
        props.append("<br> ");
        props.append("Vendor: ");
        props.append(System.getProperty("java.vendor"));
        props.append("<br> ");
        props.append("Location: ");
        props.append(System.getProperty("java.home"));
        props.append("<br> ");
        props.append("Java 3D Version: ");
        // look for java3d
        Class c = null;
        try {
            c = Class.forName("javax.media.j3d.VirtualUniverse");
            Method method = Misc.findMethod(c, "getProperties",
                                            new Class[] {});
            if (method == null) {
                props.append("<1.3");
            } else {
                try {
                    Map m = (Map) method.invoke(c, new Object[] {});
                    props.append(m.get("j3d.version"));
                    props.append("<br> ");
                    props.append("Java 3D Vendor: ");
                    props.append(System.getProperty("java.vendor"));
                    props.append(m.get("j3d.vendor"));
                    props.append("<br> ");
                    props.append("Java 3D Renderer: ");
                    props.append(m.get("j3d.renderer"));
                } catch (Exception exc) {
                    props.append("unknown");
                }
            }
        } catch (ClassNotFoundException exc) {
            props.append("none");
        }

        JEditorPane propsLbl = new JEditorPane();
        propsLbl.setEditable(false);
        propsLbl.setContentType("text/html");
        propsLbl.setText(props.toString());
        propsLbl.setBackground(tmp.getBackground());

        tab.add("System",
                GuiUtils.setSize(new JScrollPane(propsLbl), 300, 200));
        propsLbl.setBackground(Color.white);

        String[] argv = getArgsManager().getOriginalArgs();
        if (argv.length > 0) {
            StringBuffer args = new StringBuffer("Command Line Arguments<p>");
            for (int i = 0; i < argv.length; i++) {
                args.append("<b> arg " + i + "</b> = " + argv[i] + "<br>");
            }
            JEditorPane argsLbl = new JEditorPane();
            argsLbl.setEditable(false);
            argsLbl.setContentType("text/html");
            argsLbl.setText(args.toString());
            argsLbl.setBackground(tmp.getBackground());

            tab.add("Command Line Arguments",
                    GuiUtils.setSize(new JScrollPane(argsLbl), 400, 300));
            argsLbl.setBackground(Color.white);
        }



        GuiUtils.makeDialog(getFrame(),
                            "About " + getStateManager().getTitle(), tab,
                            null, new String[] { "Close" });

    }


    /**
     * Handle when the delete key is pressed in the data tree
     *
     * @param dataTree The data tree
     */
    public void deleteKeyPressed(DataTree dataTree) {
        DataChoice choice = dataTree.getSelectedDataChoice();
        if (choice == null) {
            return;
        }
        getJythonManager().deleteKeyPressed(choice);
    }



    /**
     * The data tree was clicked. Either show the data tree menu
     * or popup the control dialog, depending on whether it
     * was  right click or a double click
     *
     * @param dataTree The {@link DataTree} that was clicked
     * @param event The <code>MouseEvent</code>
     */
    public void dataTreeClick(DataTree dataTree, MouseEvent event) {
        String function = "";
        if (SwingUtilities.isRightMouseButton(event)) {
            function = getIdv().getProperty("datatree.rightmouse",
                                            "showTreeMenu");
        } else if (event.getClickCount() > 1) {
            function = getIdv().getProperty("datatree.doubleclick",
                                            "showControlDialog");
        }
        function = function.trim();
        if (function.equals("showTreeMenu")) {
            showDataTreeMenu(dataTree, event, true);
        } else if (function.equals("showControlDialog")) {
            showControlDialog(dataTree, event);
        }
    }

    /**
     * Create and show a {@link DataControlDialog} for the {@link ucar.unidata.data.DataChoice}
     * in the DataTree at the given mouse x and y (if there is a data choice there).
     *
     * @param dataTree The DataTree
     * @param event The <code>MouseEvent</code>
     */
    public void showControlDialog(DataTree dataTree, MouseEvent event) {
        int    x      = event.getX();
        int    y      = event.getY();
        Object object = dataTree.getObjectAt(x, y);
        if ((object == null) || !(object instanceof DataChoice)) {
            return;
        }
        Point sl = dataTree.getLocationOnScreen();
        sl.x += x;
        sl.y += y;
        addDCD(new DataControlDialog(getIdv(), (DataChoice) object, sl.x,
                                     sl.y));
    }


    /**
     * Make the status bar for the window. This contains a
     * {@link ucar.unidata.ui.MemoryMonitor} and a message label.
     * This does not add the status bar to the frame though.
     *
     * @param window The window
     * @return The status bar
     */
    public JPanel doMakeStatusBar(IdvWindow window) {
        JLabel msgLabel = new JLabel("                         ");
        LogUtil.addMessageLogger(msgLabel);
        window.setComponent(COMP_MESSAGELABEL, msgLabel);
        IdvXmlUi xmlUI = window.getXmlUI();
        if (xmlUI != null) {
            xmlUI.addComponent(COMP_MESSAGELABEL, msgLabel);
        }


        //        final JLabel waitLabel = new JLabel(window.getNormalIcon());
        JLabel waitLabel = new JLabel(window.getNormalIcon());
        waitLabel.addMouseListener(new ObjectListener(null) {
            public void mouseClicked(MouseEvent e) {
                getIdv().clearWaitCursor();
            }
        });

        window.setComponent(COMP_WAITLABEL, waitLabel);

        RovingProgress progress = doMakeRovingProgressBar();
        window.setComponent(COMP_PROGRESSBAR, progress);


        //TODO: turn off the memory monitor  when this window is closed.
        MemoryMonitor mm = new MemoryMonitor();
        //      mm.setLabelFont (DisplayConventions.getWindowLabelFont ());
        Border paddedBorder =
            BorderFactory.createCompoundBorder(getStatusBorder(),
                BorderFactory.createEmptyBorder(0, 2, 0, 2));
        mm.setBorder(paddedBorder);
        progress.setBorder(paddedBorder);
        waitLabel.setBorder(getStatusBorder());
        msgLabel.setBorder(paddedBorder);
        JPanel msgBar    = GuiUtils.leftCenter(mm, msgLabel);

        JPanel statusBar = GuiUtils.centerRight(msgBar, progress);
        //                                                GuiUtils.hbox(progress,
        //                                                    waitLabel));
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        return statusBar;
    }


    /**
     * Make the roving progress bar
     *
     * @return Roving progress bar
     */
    public RovingProgress doMakeRovingProgressBar() {
        Color          c        = Color.lightGray.darker();
        RovingProgress progress = new RovingProgress(c) {
            private Font labelFont;
            public boolean drawFilledSquare() {
                return false;
            }

            public void paintInner(Graphics g) {
                //Catch if we're not in a wait state
                if ( !IdvWindow.getWaitState() && super.isRunning()) {
                    super.stop();
                    return;
                }
                if ( !super.isRunning()) {
                    super.paintInner(g);
                    return;
                }
                super.paintInner(g);
            }

            public void paintLabel(Graphics g, Rectangle bounds) {
                if (labelFont == null) {
                    labelFont = g.getFont();
                    labelFont = labelFont.deriveFont(Font.BOLD);
                }
                g.setFont(labelFont);
                //                String label = "";
                g.setColor(Color.black);
                if (DataSourceImpl.getOutstandingGetDataCalls() > 0) {
                    g.drawString("Reading data", 5, bounds.height - 4);
                } else {
                    //                    g.drawString("Building display", 5, bounds.height - 4);
                }

            }
        };
        progress.setPreferredSize(new Dimension(130, 10));
        return progress;
    }


    /**
     * Get the border used for the status bar
     *
     * @return The border
     */
    public Border getStatusBorder() {
        return new FineLineBorder(BevelBorder.LOWERED);
    }



    /**
     *  If there is a lastActiveFrame then return that.
     *  Else return the first window in the list of windows
     *
     * @return Some JFrame
     */
    public JFrame getFrame() {
        if (lastActiveFrame != null) {
            //TODO            return lastActiveFrame;
        }
        List windows = IdvWindow.getMainWindows();
        if (windows.size() > 0) {
            IdvWindow idvWindow = (IdvWindow) windows.get(0);
            return (JFrame) idvWindow.getFrame();
        }
        return null;
    }


    /**
     * This is the first window popped up. Set its size, etc.
     *
     * @param frame The window
     */
    private void positionWindow(JFrame frame) {
        if (getIdv().getProperty(PROP_WINDOW_USESCREENSIZE, false)) {
            Dimension screenSize =
                Toolkit.getDefaultToolkit().getScreenSize();
            int offset = getIdv().getProperty(PROP_WINDOW_SCREENSIZEOFFSET,
                             20);
            frame.setLocation(offset, offset);
            frame.setSize(screenSize.width - offset * 2,
                          screenSize.height - offset * 2);
        } else {
            int w = getIdv().getProperty(PROP_WINDOW_SIZEWIDTH, -1);
            int h = getIdv().getProperty(PROP_WINDOW_SIZEHEIGHT, -1);
            if ((w != -1) && (h != -1)) {
                frame.setSize(w, h);
            }
        }
    }



    /**
     * Do basic initialization
     */
    public void doBasicInitialization() {
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        setDateFormat();
    }



    /**
     * Find the window with the specified skin
     *
     * @param windows  the list of windows
     * @param skinPath  the skin's path
     *
     * @return  the window for that skin or null
     */
    private IdvWindow findWindowWithSkin(List windows, String skinPath) {
        for (int windowIdx = 0; windowIdx < windows.size(); windowIdx++) {
            IdvWindow window = (IdvWindow) windows.get(windowIdx);
            if ( !Misc.equals(fixSkinPath(window.getSkinPath()),
                              fixSkinPath(skinPath))) {
                continue;
            }
            return window;
        }
        return null;
    }

    /**
     * Find the window that matches the window info
     *
     * @param currentWindows  list of windows
     * @param windowInfo  the info to search for
     *
     * @return the matching window or null
     */
    protected IdvWindow findWindowThatMatches(List currentWindows,
            WindowInfo windowInfo) {
        String skinPath = windowInfo.getSkinPath();
        List   newVms   = windowInfo.getViewManagers();
        for (int oldWindowIdx = 0; oldWindowIdx < currentWindows.size();
                oldWindowIdx++) {
            IdvWindow currentWindow =
                (IdvWindow) currentWindows.get(oldWindowIdx);
            if ( !Misc.equals(fixSkinPath(currentWindow.getSkinPath()),
                              fixSkinPath(skinPath))) {
                continue;
            }
            List origVms = currentWindow.getViewManagers();
            if (newVms.size() != origVms.size()) {
                continue;
            }
            boolean windowOk = true;
            for (int vmIdx = 0; vmIdx < newVms.size(); vmIdx++) {
                ViewManager newViewManager  = (ViewManager) newVms.get(vmIdx);
                ViewManager origViewManager =
                    (ViewManager) origVms.get(vmIdx);
                if ( !origViewManager.canBe(newViewManager)) {
                    windowOk = false;
                    break;
                }
            }
            if ( !windowOk) {
                continue;
            }
            return currentWindow;
        }
        return null;
    }


    /**
     * Handle the list of IdvWindow-s from the bundle
     *
     * @param windows The windows form the bundle.
     * @param newViewManagers List of the new view managers. We remove any ones we init
     * @param okToMerge  true if okay to merge
     * @param fromCollab From collaboration
     * @param didRemoveAll  true if remove all was done
     */
    public void unpersistWindowInfo(List windows, List newViewManagers,
                                    boolean okToMerge, boolean fromCollab,
                                    boolean didRemoveAll) {
        if (newViewManagers == null) {
            newViewManagers = new ArrayList();
        }
        //        List currentWindows = new ArrayList(IdvWindow.getMainWindows());
        List currentWindows = new ArrayList(IdvWindow.getWindows());
        for (int newWindowIdx = 0; newWindowIdx < windows.size();
                newWindowIdx++) {
            WindowInfo windowInfo = (WindowInfo) windows.get(newWindowIdx);
            List       newVms     = windowInfo.getViewManagers();
            newViewManagers.removeAll(newVms);

            boolean createANewOne = true;
            if (okToMerge) {
                IdvWindow currentWindow =
                    findWindowThatMatches(currentWindows, windowInfo);
                if (currentWindow != null) {
                    List origVms = currentWindow.getViewManagers();
                    createANewOne = false;
                    for (int vmIdx = 0;
                            (vmIdx < newVms.size())
                            && (vmIdx < origVms.size());
                            vmIdx++) {
                        ViewManager newViewManager =
                            (ViewManager) newVms.get(vmIdx);
                        ViewManager origViewManager =
                            (ViewManager) origVms.get(vmIdx);
                        origViewManager.initWith(newViewManager, fromCollab);
                    }
                    currentWindows.remove(currentWindow);
                    currentWindow.setIsAMainWindow(
                        windowInfo.getIsAMainWindow());
                    currentWindow.setBounds(windowInfo.getBounds());
                }
            }
            if (createANewOne) {
                getVMManager().addViewManagers(newVms);
                String skinPath = windowInfo.getSkinPath();
                IdvWindow window = createNewWindow(newVms, skinPath,
                                       windowInfo.getTitle());
                window.setBounds(windowInfo.getBounds());
            }
        }

        /**
         * If the user said to remove all displays and data then we go through
         *   the list of existing windows that are not being used in the just loaded
         *   bundle and get rid of them
         */
        if (okToMerge && didRemoveAll) {
            //            System.err.println ("disposing of:" + currentWindows);
            for (int windowIdx = 0; windowIdx < currentWindows.size();
                    windowIdx++) {
                IdvWindow idvWindow =
                    (IdvWindow) currentWindows.get(windowIdx);
                if (idvWindow.getIsAMainWindow()) {
                    idvWindow.dispose();
                }
            }
        }


    }


    /**
     * Create a new window containing a new {@link MapViewManager}
     *
     * @return The new window
     */
    public IdvWindow createNewWindow() {
        return createNewWindow(new ArrayList(), false);
    }




    /**
     * Create a new IdvWindow for the given viewManager. Put the
     * contents of the viewManager into the window
     *
     * @param viewManagers The view managers
     * @return The new window
     */
    public IdvWindow createNewWindow(List viewManagers) {
        return createNewWindow(viewManagers, true);
    }


    /**
     * Get the window title from the skin
     *
     * @param index  the skin index
     *
     * @return  the title
     */
    private String getWindowTitleFromSkin(int index) {
        XmlResourceCollection skins = getResourceManager().getXmlResources(
                                          getResourceManager().RSC_SKIN);
        List names = StringUtil.split(skins.getShortName(index), ">", true,
                                      true);

        String title = getStateManager().getTitle();

        if (names.size() > 0) {
            title = title + " - " + StringUtil.join(" - ", names);
        }
        return title;
    }

    /**
     * Create a new IdvWindow for the given viewManager. Put the
     * contents of the viewManager into the window
     *
     * @param viewManagers The view managers
     * @param notifyCollab Should the {@link ucar.unidata.idv.collab.CollabManager}
     * be notified
     *
     * @return The new window
     */
    public IdvWindow createNewWindow(List viewManagers,
                                     boolean notifyCollab) {

        XmlResourceCollection skins = getResourceManager().getXmlResources(
                                          getResourceManager().RSC_SKIN);
        Element root     = null;
        String  path     = null;
        String  skinName = null;

        //First try to find the the default skin
        for (int i = 0; (i < skins.size()) && (root == null); i++) {
            if (Misc.equals(skins.getProperty("default", i), "true")) {
                root     = skins.getRoot(i);
                path     = skins.get(i).toString();
                skinName = getWindowTitleFromSkin(i);
            }
        }

        if (root == null) {
            for (int i = 0; (i < skins.size()) && (root == null); i++) {
                root     = skins.getRoot(i);
                path     = skins.get(i).toString();
                skinName = getWindowTitleFromSkin(i);
            }
        }
        return createNewWindow(viewManagers, notifyCollab, skinName, path,
                               root);
    }

    /**
     * Create a new IdvWindow with the lsit of ViewManager-s and the xml skin.
     *
     * @param viewManagers The view managers to put in the window.
     * @param skinPath The skin
     *
     * @return The window
     */
    public IdvWindow createNewWindow(List viewManagers, String skinPath) {
        return createNewWindow(viewManagers, skinPath, null);
    }



    /**
     * Create a new IdvWindow with the lsit of ViewManager-s and the xml skin.
     *
     * @param viewManagers The view managers to put in the window.
     * @param skinPath The skin
     * @param  windowTitle title
     *
     * @return The window
     */
    public IdvWindow createNewWindow(List viewManagers, String skinPath,
                                     String windowTitle) {
        try {
            Element skinRoot = XmlUtil.getRoot(fixSkinPath(skinPath),
                                   getClass());
            return createNewWindow(viewManagers, false, windowTitle,
                                   skinPath, skinRoot);
        } catch (Throwable excp) {
            logException("createNewWindow", excp);
            return null;
        }
    }


    /**
     * A hack because we moved the skins
     *
     * @param skinPath original path
     * @return fixed path
     */
    private String fixSkinPath(String skinPath) {
        if (skinPath == null) {
            return null;
        }
        if (StringUtil.stringMatch(
                skinPath, "^/ucar/unidata/idv/resources/[^/]+\\.xml")) {
            skinPath =
                StringUtil.replace(skinPath, "/ucar/unidata/idv/resources/",
                                   "/ucar/unidata/idv/resources/skins/");
        }
        return skinPath;
    }

    /**
     * Create a new IdvWindow
     *
     * @param viewManagers The view managers to put in the window.
     * @param notifyCollab Should we tell the collab facility
     * @param skinPath The skin. May be null.
     * @param skinRoot Root of the skin xml. May be null.
     *
     * @return The window.
     */
    public IdvWindow createNewWindow(List viewManagers, boolean notifyCollab,
                                     String skinPath, Element skinRoot) {
        return createNewWindow(viewManagers, notifyCollab, null, skinPath,
                               skinRoot);
    }

    /**
     * Create a new window
     *
     * @param viewManagers The view managers to put in the window.
     * @param notifyCollab Should we tell the collab facility
     * @param title        The title
     * @param skinPath The skin. May be null.
     * @param skinRoot Root of the skin xml. May be null.
     *
     * @return The window.
     */
    public IdvWindow createNewWindow(List viewManagers, boolean notifyCollab,
                                     String title, String skinPath,
                                     Element skinRoot) {
        return createNewWindow(viewManagers, notifyCollab, title, skinPath,
                               skinRoot, true);
    }

    /**
     * Create a new window
     *
     * @param viewManagers The view managers to put in the window.
     * @param notifyCollab Should we tell the collab facility
     * @param title        The title
     * @param skinPath The skin. May be null.
     * @param skinRoot Root of the skin xml. May be null.
     * @param show If true show the window once created, otherwise leave
     *             it to the caller.
     *
     * @return The window.
     */
    public IdvWindow createNewWindow(List viewManagers, boolean notifyCollab,
                                     String title, String skinPath,
                                     Element skinRoot, boolean show) {

        try {
            if (title == null) {
                title = getStateManager().getTitle();
            }
            boolean isMainWindow = true;
            String  windowType   = "";
            if (skinRoot != null) {
                windowType = XmlUtil.getAttribute(skinRoot, "windowtype",
                        windowType);
                if (XmlUtil.hasAttribute(skinRoot, ATTR_MAINWINDOW)) {
                    isMainWindow = XmlUtil.getAttribute(skinRoot,
                            ATTR_MAINWINDOW, isMainWindow);
                }
            }

            if ((skinRoot != null)
                    && XmlUtil.hasAttribute(skinRoot, "windowtitle")) {
                title = XmlUtil.getAttribute(skinRoot, "windowtitle", "");
            }

            IdvWindow window = new IdvWindow(title, getIdv(), isMainWindow);
            window.setType(windowType);
            window.setVisible(false);
            ImageIcon icon =
                GuiUtils.getImageIcon(getIdv().getProperty("idv.splash.icon",
                    ""));
            if (icon != null) {
                window.setIconImage(icon.getImage());
            }

            JComponent contents;

            //If we have a skin then use it
            if (viewManagers == null) {
                viewManagers = new ArrayList();
            }
            if (skinRoot != null) {
                IdvXmlUi xmlUI = doMakeIdvXmlUi(window, viewManagers,
                                     skinRoot);
                window.setSkinPath(skinPath);
                String iconProp = xmlUI.getProperty("icon.wait.normal");
                if (iconProp != null) {
                    IdvWindow.setNormalIcon(iconProp);
                }
                iconProp = xmlUI.getProperty("icon.wait.wait");
                if (iconProp != null) {
                    IdvWindow.setWaitIcon(iconProp);
                }
                contents = (JComponent) xmlUI.getContents();
                window.setXmlUI(xmlUI);
                viewManagers = xmlUI.getViewManagers();
            } else {
                //Else call out to make the gui
                if (viewManagers.size() == 0) {
                    viewManagers.add(
                        getIdv().getViewManager(
                            ViewDescriptor.LASTACTIVE, false,
                            getIdv().getViewManagerProperties()));
                }
                contents = doMakeDefaultContents(window,
                        (ViewManager) viewManagers.get(0));
                JMenuBar menuBar = doMakeMenuBar();
                if (menuBar != null) {
                    window.setJMenuBar(menuBar);
                }
            }
            updateToolbars();
            Msg.translateTree(contents);
            window.setContents(contents);
            if (viewManagers.size() > 0) {
                associateWindowWithViewManagers(window, viewManagers);
            }

            if (getIdv().okToShowWindows() && show) {
                window.show();
                GuiUtils.toFrontModalDialogs();
            }

            if (notifyCollab) {
                //TODO:                getIdv().getCollabManager().newWindow(viewManager);
            }
            return window;
        } catch (Throwable excp) {
            logException("createNewWindow-1", excp);
        }
        return null;
    }


    /**
     * Get the list of IdvWindows that should be saved in a bundle
     *
     * @return List of windows to persist
     */
    public List getWindowsToPersist() {
        List windows    = new ArrayList();
        List allWindows = IdvWindow.getWindows();
        for (int windowIdx = 0; windowIdx < allWindows.size(); windowIdx++) {
            IdvWindow window = (IdvWindow) allWindows.get(windowIdx);
            //            if (window.hasViewManagers()) {
            if (window.getIsAMainWindow()) {
                windows.add(new WindowInfo(window));
            }
        }
        return windows;
    }

    /**
     * Create a gui component of the toolbar
     *
     * @return the toolbar
     */
    public JComponent getToolbarUI() {
        Element toolbarRoot = getToolbarRoot();
        if (toolbarRoot == null) {
            return new JPanel();
        }
        IdvXmlUi.processToolbarXml(toolbarRoot, this);
        return (JComponent) new XmlUi(toolbarRoot, getIdv()).getContents();
    }

    /**
     * Factory method to create an xmlui
     *
     * @param window The window
     * @param viewManagers The view manager
     * @param skinRoot The skin xml
     *
     * @return The xmlui
     */
    protected IdvXmlUi doMakeIdvXmlUi(IdvWindow window, List viewManagers,
                                      Element skinRoot) {
        return new IdvXmlUi(window, viewManagers, getIdv(), skinRoot);
    }


    protected JComponent doMakeToolbar() {
        Element toolbarRoot = getToolbarRoot();
        if (toolbarRoot == null) {
            return new JPanel();
        }
        IdvXmlUi.processToolbarXml(toolbarRoot, getIdvUIManager());
        XmlUi xmlui = new XmlUi(toolbarRoot,getIdv());
        JComponent toolbar = (JComponent) xmlui.getContents();
        return toolbar;
    }



    public void reloadToolbarResources() {
        XmlResourceCollection xrc = getResourceManager().getXmlResources(
                                                                         getResourceManager().RSC_TOOLBAR);
        xrc.clearCache();
    }


    public void updateIconBar() {
        reloadToolbarResources();
        List toolbarComps = getWindowGroup(
                                           IdvWindow.GROUP_TOOLBARS);
        for(int i=0;i<toolbarComps.size();i++) {
            JComponent comp = (JComponent)toolbarComps.get(i);
            JComponent toolbar = doMakeToolbar();
            comp.removeAll();
            comp.add(BorderLayout.CENTER, toolbar);
            comp.repaint();
        }
    }


    /**
     * If we don't have a UI skin then this method is called to create the window contents
     *
     * @param window The window
     * @param viewManager  The ViewManager being shown in the window
     *
     * @return The GUI contents
     */
    public JComponent doMakeDefaultContents(IdvWindow window,
                                            ViewManager viewManager) {
        JPanel statusBar = doMakeStatusBar(window);
        JPanel toolbar   = new JPanel();
        window.setComponent(COMP_FAVORITESBAR, toolbar);
        return GuiUtils.topCenterBottom(toolbar, viewManager.getContents(),
                                        statusBar);
    }




    /**
     * Get the root of the toolbar xml
     *
     * @return The toolbar xml root
     */
    public Element getToolbarRoot() {
        try {
            XmlResourceCollection xrc = getResourceManager().getXmlResources(
                                            getResourceManager().RSC_TOOLBAR);
            Element root = null;
            for (int i = 0; (i < xrc.size()) && (root == null); i++) {
                root = xrc.getRoot(i);
            }
            return root;
        } catch (Exception exc) {
            logException("Creating icon bar", exc);
        }
        return null;
    }



    /**
     * Display any errors that are contained in the given results.
     * The results are from one or more data source create calls.
     *
     * @param results The results to show
     */
    public void showResults(DataSourceResults results) {
        if ( !results.anyFailed()) {
            return;
        }
        List failedData = Misc.toVector(
                              StringUtil.listToStringArray(
                                  results.getFailedData()));
        List       exceptions = results.getExceptions();
        JComponent topLabel;

        String     errorMsg = null;
        if (exceptions.size() > 1) {
            errorMsg = "There were errors loading the data:";
        } else {
            errorMsg = "There was an error loading the data:";
        }
        if (results.anyOk()) {
            if (exceptions.size() > 1) {
                errorMsg = "But there were errors loading some of the data:";
            } else {
                errorMsg = "But there was an error loading some of the data:";
            }
            List success = results.getSuccessData();
            List comps   = new ArrayList();
            for (int i = 0; i < success.size(); i++) {
                Object o = success.get(i);
                String msg;
                if ( !(o instanceof List)) {
                    o = Misc.newList(o);
                }
                List dataList = (List) o;
                for (int j = 0; j < dataList.size(); j++) {
                    comps.add(GuiUtils.inset(new JLabel("    "
                            + dataList.get(j).toString()), 2));
                }
            }
            topLabel = GuiUtils
                .vbox(new JLabel(
                    "Some of the data files were loaded successfully:"), GuiUtils
                        .vbox(comps));
        } else {
            topLabel = new JLabel(" ");
        }
        JPanel content;
        if (exceptions.size() > 1) {
            content =
                GuiUtils.topCenter(new JLabel(errorMsg),
                                   LogUtil.getMultiExceptionsPanel(null,
                                       exceptions));
            GuiUtils.showDialog("Data loading errors",
                                GuiUtils.topCenter(topLabel,
                                    GuiUtils.inset(content, 4)));

        } else if (exceptions.size() == 1) {
            LogUtil.logException(errorMsg, (Exception) exceptions.get(0));
        }
    }




    /**
     *  Remove the given data tree from the list of data trees
     *
     * @param holder The holder to remove
     */
    public void removeDataSourceHolder(DataSourceHolder holder) {
        dataSourceHolders.remove(holder);
    }

    /**
     * Add the given data source holder to the list of data source holders
     *
     * @param holder The holder to initialize
     */
    public void addDataSourceHolder(DataSourceHolder holder) {
        dataSourceHolders.add(holder);
    }



    /**
     * Add the set of data sources to the given holder.
     * Put it in a window if inWindow is true.
     *
     * @param holder The holder to initialize
     * @param inWindow Put the holder in a window?
     */
    public void initDataSourceHolder(final DataSourceHolder holder,
                                     boolean inWindow) {
        if (inWindow) {
            IdvWindow treeWindow = holder.doMakeFrame();
            JMenuBar  menuBar    = doMakeMenuBar();
            if (menuBar != null) {
                treeWindow.setJMenuBar(menuBar);
            }
            if (defaultHolderBounds != null) {
                String className = holder.getClass().getName();
                Rectangle defaultBounds =
                    (Rectangle) defaultHolderBounds.get(className);
                if (defaultBounds != null) {
                    treeWindow.setBounds(defaultBounds);
                    defaultHolderBounds.remove(className);
                }
            }
            if (getIdv().getHaveInitialized()) {
                holder.show();
            }
            Msg.translateTree(treeWindow.getContents());
        }
        addDataSourceHolder(holder);
        List dataSources = getDataSourcesForGui();
        if (dataSources != null) {
            for (int i = 0; i < dataSources.size(); i++) {
                holder.addDataSource((DataSource) dataSources.get(i));
            }
        }

    }



    /**
     *  Find and return all of the current DataSource-s that do not have
     *  the DataManager.SHOW_IN_TREE property set to false.
     *
     * @return List of data sources to show in the gui
     */
    private List getDataSourcesForGui() {
        List all = getIdv().getDataSources();
        if (all == null) {
            return null;
        }
        List subset = new ArrayList();
        for (int i = 0; i < all.size(); i++) {
            DataSource dataSource = (DataSource) all.get(i);
            if (getIdv().getDataManager().getProperty(
                    dataSource.getTypeName(), DataManager.PROP_SHOW_IN_TREE,
                    true)) {
                subset.add(dataSource);
            }
        }
        return subset;
    }


    /**
     * Notify the data source holders of a new display control.
     * Redo any menus
     *
     * @param control The new control
     */
    public void addDisplayControl(DisplayControl control) {
        if (getStateManager().getShowControlsInTree()) {
            DataChoice choice = control.getDataChoice();
            for (int i = 0; i < dataSourceHolders.size(); i++) {
                ((DataSourceHolder) dataSourceHolders.get(
                    i)).addDisplayControl(control, choice);
            }
        }
        if ((getViewPanel() != null)
                && !getIdv().getArgsManager().getIsOffScreen()) {
            getViewPanel().addDisplayControl(control);
        }
        displayControlsChanged();
    }

    /**
     * This tries to show one of the main gui windows
     */
    public void showDashboard() {
        showBasicWindow(true);
    }


    /**
     * Do we have a basic window
     *
     * @return  true if the window is a basic window
     */
    public boolean haveBasicWindow() {
        List windows = new ArrayList(IdvWindow.getWindows());
        for (int i = 0; i < windows.size(); i++) {
            IdvWindow window = (IdvWindow) windows.get(i);
            if ( !window.hasViewManagers()) {
                return true;
            }
        }
        return false;
    }

    /**
     * This tries to show one of the main gui windows
     *
     * @param createThemIfNotThere If true then, if there isn't any
     * non view containing windows then call  doMakeBasicWindows()
     *
     * @return Were there any found
     */
    public boolean showBasicWindow(boolean createThemIfNotThere) {
        List windows = new ArrayList(IdvWindow.getWindows());
        for (int i = 0; i < windows.size(); i++) {
            IdvWindow window = (IdvWindow) windows.get(i);
            if (window.getSkinPath()!=null &&  !window.hasViewManagers()) {
                //got one
                window.show();
                return true;
            }
        }
        if (createThemIfNotThere) {
            showWaitCursor();
            doMakeBasicWindows();
            showNormalCursor();
        }
        return false;
    }





    /**
     * Gets called by the display controls. We add the 'embed in tabs' menu items
     *
     * @param control The control
     * @param items List of menu items
     */
    public void addViewMenuItems(DisplayControl control, List items) {
        if (getViewPanel() != null) {
            getViewPanel().addViewMenuItems(control, items);
        }
    }


    /**
     * Do we do control tabs
     *
     * @return Do we do control tabs
     */
    protected boolean getShowControlsInTab() {
        return getStore().get(PREF_CONTROLSINTABS, true);
    }




    /**
     * Notify the data source holders of a removed display control.
     * Redo any menus
     *
     * @param control The removed control
     */
    public void removeDisplayControl(DisplayControl control) {
        for (int i = 0; i < dataSourceHolders.size(); i++) {
            ((DataSourceHolder) dataSourceHolders.get(
                i)).removeDisplayControl(control);
        }
        if (getViewPanel() != null) {
            getViewPanel().removeDisplayControl(control);
        }
        displayControlsChanged();
    }


    /**
     *  Remove all data sources from the data source holders.
     *  Update any menus.
     */
    public void removeAllDataSources() {
        for (int i = 0; i < dataSourceHolders.size(); i++) {
            ((DataSourceHolder) dataSourceHolders.get(
                i)).removeAllDataSources();
        }
    }


    /**
     *  Remove the data source the data source holders.
     *  Update the data menu in all menu bars.
     *
     * @param dataSource The removed data source
     */
    public void removeDataSource(DataSource dataSource) {
        for (int i = 0; i < dataSourceHolders.size(); i++) {
            ((DataSourceHolder) dataSourceHolders.get(i)).removeDataSource(
                dataSource);
        }
    }

    /**
     * Tell the data source holders of the change.
     *  Update any menus.
     *
     * @param source The data source that changed.
     */
    public void dataSourceChanged(DataSource source) {
        for (int i = 0; i < dataSourceHolders.size(); i++) {
            DataSourceHolder holder =
                (DataSourceHolder) dataSourceHolders.get(i);
            holder.dataSourceChanged(source);
        }
    }



    /**
     * Tell the data source holders of the change.
     *  Update any menus.
     *
     * @param source The data source that changed.
     */
    public void dataSourceTimeChanged(DataSource source) {
        for (int i = 0; i < dataSourceHolders.size(); i++) {
            DataSourceHolder holder =
                (DataSourceHolder) dataSourceHolders.get(i);
            holder.dataSourceTimeChanged(source);
        }
    }


    /**
     * Tell the data source holders of the change.
     *  Update any menus.
     *
     * @param dataSource The new data source
     */
    public void addDataSource(DataSource dataSource) {
        for (int i = 0; i < dataSourceHolders.size(); i++) {
            ((DataSourceHolder) dataSourceHolders.get(i)).addDataSource(
                dataSource);

        }
    }


    /**
     * Add any UI state to the bundle. Example: commands to show color table
     * editor, station model editor, etc.
     *
     * @param data Where to put the state.
     */
    public void addStateToBundle(Hashtable data) {
        List commandsToRun = new ArrayList();
        if (commandsToRun.size() > 0) {
            data.put(ID_COMMANDSTORUN, commandsToRun);
        }

        addDataHolderState(data);
    }



    /**
     * Add any UI state concerning the data holders (e.g., window size)
     * to the bundle.
     *
     * @param data Where to put the state.
     */
    public void addDataHolderState(Hashtable data) {
        DataSourceHolder windowedTreeHolder     = null;
        DataSourceHolder windowedSelectorHolder = null;
        boolean          didOne                 = false;
        Hashtable        bounds                 = null;
        for (int i = 0; i < dataSourceHolders.size(); i++) {
            DataSourceHolder holder =
                (DataSourceHolder) dataSourceHolders.get(i);
            if (holder.getFrame() == null) {
                continue;
            }
            if (bounds == null) {
                bounds = new Hashtable();
            }
            String className = holder.getClass().getName();
            if (bounds.get(className) != null) {
                continue;
            }
            bounds.put(className, holder.getFrame().getBounds());
        }

        if (bounds != null) {
            Hashtable misc = new Hashtable();
            misc.put(PROP_DATAHOLDERBOUNDS, bounds);
            data.put(ID_MISCHASHTABLE, misc);
        }
    }

    /**
     * misc contains state that was from a bundle. Apply it to the data source holders.
     *
     * @param misc The state
     */
    public void applyDataHolderState(Hashtable misc) {
        if (misc == null) {
            return;
        }
        defaultHolderBounds = (Hashtable) misc.get(PROP_DATAHOLDERBOUNDS);
        if (defaultHolderBounds == null) {
            return;
        }
        for (int i = 0; i < dataSourceHolders.size(); i++) {
            DataSourceHolder holder =
                (DataSourceHolder) dataSourceHolders.get(i);
            Rectangle bounds = (Rectangle) defaultHolderBounds.get(
                                   holder.getClass().getName());
            if ((bounds != null) && (holder.getFrame() != null)) {
                holder.getFrame().setBounds(bounds);
            }
        }
    }


    /**
     * Get the list of DataSource holders
     *
     * @return  the list of holders
     */
    public List getDataSourceHolders() {
        return new ArrayList(dataSourceHolders);
    }


    /**
     * Find the children of the component. If its a jmu only use the menu items
     *
     * @param comp The component
     *
     * @return Its children
     */
    private Component[] findChildren(JComponent comp) {
        if (comp instanceof JMenu) {
            return ((JMenu) comp).getMenuComponents();
        }
        return comp.getComponents();
    }

    /**
     * This automatically pops up the menu identified by the colon delimited string of menu names
     *
     * @param s colon delimited list of menu names
     */
    public void showMenu(String s) {
        Misc.run(this, "showMenuInThread", s);
    }


    /**
     * This automatically pops up the menu identified by the colon delimited string of menu names
     *
     * @param s colon delimited list of menu names
     */
    public void showMenuInThread(String s) {
        IdvWindow idvWindow = IdvWindow.getActiveWindow();
        if (idvWindow == null) {
            return;
        }
        JMenuBar menuBar = null;
        List     comps   = idvWindow.getComponents();
        if (comps == null) {
            return;
        }
        for (int i = 0; i < comps.size(); i++) {
            Object o = comps.get(i);
            if (o instanceof JMenuBar) {
                menuBar = (JMenuBar) o;
                break;
            }
        }

        if (menuBar == null) {
            return;
        }
        GuiUtils.showComponentInTabs(menuBar);
        List       toks  = StringUtil.split(s, ":", true, true);
        JComponent comp  = menuBar;
        List       menus = new ArrayList();
        for (int tokIdx = 0; tokIdx < toks.size(); tokIdx++) {
            String tok = (String) toks.get(tokIdx);
            tok = tok.toLowerCase().trim();
            boolean     foundIt  = false;
            Component[] children = findChildren(comp);
            for (int i = 0; (i < children.length) && !foundIt; i++) {
                Component child = children[i];
                if ( !(child instanceof JMenuItem)) {
                    continue;
                }
                String text = ((JMenuItem) child).getText();
                text    = text.toLowerCase().trim();
                foundIt = Misc.equals(tok, text);
                if (foundIt) {
                    comp = (JComponent) child;
                }
            }
            if ( !foundIt) {
                System.err.println("Couldn't find menu:" + tok);
                return;
            }
            JMenuItem mi = (JMenuItem) comp;
            mi.setArmed(true);
            Misc.sleep(1000);
            if (mi instanceof JMenu) {
                mi.doClick();
                menus.add(0, mi);
            } else {
                mi.doClick();
                mi.setArmed(false);
                break;
            }
            Misc.sleep(1000);
        }
        for (int i = 0; i < menus.size(); i++) {
            JMenu menu = (JMenu) menus.get(i);
            menu.setArmed(false);
            menu.setPopupMenuVisible(false);
        }

    }


    /**
     * Show the help identified by the given target
     *
     * @param target The target in the help system
     */
    public void showHelp(String target) {
        showHelp(null, target);
    }



    /**
     * Show the javahelp with the given base url and help target
     *
     * @param url The base url. May be null, if so use the StateManager's getDefaultHelpUrl
     * @param target The javahelp target. May be null, if so use the StateManager's getDefaultHelpTarget
     */
    public void showHelp(String url, String target) {
        if (url == null) {
            url = getIdv().getStateManager().getDefaultHelpUrl();
        }
        if (target == null) {
            target = getIdv().getStateManager().getDefaultHelpTarget();
        }

        List comps = findComponents("idv.help");
        for (int i = 0; i < comps.size(); i++) {
            Object comp = comps.get(i);
            if ( !(comp instanceof IdvHelp)) {
                continue;
            }
            IdvHelp idvHelp = (IdvHelp) comp;
            if (idvHelp.getHelpSet().getCombinedMap().isValidID(target,
                    idvHelp.getHelpSet())) {
                idvHelp.setCurrentID(target);
                GuiUtils.showComponentInTabs(idvHelp);
                return;
            }
        }


        Help.setTopDir(url);
        if ( !Help.getDefaultHelp().getDefaultHelp().isValidID(target)) {
            LogUtil.userMessage("Unknown help id: " + target);
            Help.getDefaultHelp().gotoTarget(
                getIdv().getStateManager().getDefaultHelpTarget());
        } else {
            Help.getDefaultHelp().gotoTarget(target);
        }
    }



    /**
     *  Create a {@link DataTree} object. Put it in its own window
     *  if the parameter inOwnWindow is true.
     *
     * @param inOwnWindow Put it in its own window
     * @return The new {@link DataTree}
     */
    public DataTree createDataTree(boolean inOwnWindow) {
        DataTree dataTree =
            new DataTree(getIdv(),
                         getIdv().getProperty(PROP_SHOWFORMULAS, true)
                         ? getIdv().getJythonManager()
                             .getDescriptorDataSource()
                         : null);
        initDataTree(dataTree);
        initDataSourceHolder(dataTree, inOwnWindow);
        return dataTree;
    }




    /**
     *  Create a {@link DataTree} object that is  in its own window
     *
     * @return  The new {@link DataTree}
     */
    public DataTree createDataTreeWindow() {
        return createDataTree(true);
    }

    /**
     *  Create a {@link DataTree} object that is  <em>not</em> in its own window
     *
     * @return The new {@link DataTree}
     */
    public DataTree createDataTree() {
        return createDataTree(false);
    }




    /**
     *  When a new {@link DataTree} is created this method is called.
     *  It adds a mouseListener on the tree to listen for mouseClick events
     *  which it then calls the dataTreeClick method.
     *
     * @param dataTree The {@link DataTree} to initialize
     */
    public void initDataTree(final DataTree dataTree) {
        if (dataTree == null) {
            return;
        }
        dataTree.getTree().addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                dataTreeClick(dataTree, me);
            }
        });
    }



    /**
     * Create a {@link DataSelector} window.
     * Put it in its own window  if the parameter inOwnWindow is true.
     * @return The new  {@link DataSelector}
     */
    public DataSelector createDataSelector() {
        return createDataSelector(false, true);
    }



    /**
     * Should we embed the selector in the dashboard
     *
     * @return embed the selector in the dashboard
     */
    public boolean embedFieldSelectorInDashboard() {
        return getIdv().getStore().get(PREF_EMBEDFIELDSELECTORINDASHBOARD,
                                       true);
    }

    /**
     * Should we embed the quick links in the dashboard
     *
     * @return embed the quick links in the dashboard
     */
    public boolean embedQuickLinksInDashboard() {
        return getIdv().getStore().get(
            PREF_EMBEDQUICKLINKSINDASHBOARD,
            getIdv().getProperty("idv.quicklinks.show", true));
    }

    /**
     * Should we embed the chooser in the dashboard
     *
     * @return embed the chooser in the dashboard
     */
    public boolean embedDataChooserInDashboard() {
        return getIdv().getStore().get(PREF_EMBEDDATACHOOSERINDASHBOARD,
                                       true);
    }

    /**
     * Should we embed the legends in the dashboard
     *
     * @return embed legends in the dashboard
     */
    public boolean embedLegendInDashboard() {
        return getIdv().getStore().get(PREF_EMBEDLEGENDINDASHBOARD, false);
    }


    /**
     * If there is a data selector window around then show it. Else
     * create a new one.
     *
     * @deprecated Moving away from hard-coded windows
     */
    public void showOrCreateDataSelector() {
        if ( !dataSelectorToFront()) {
            createDataSelector();
        }
    }

    /**
     * Show the data selector window if its ok with the user preference
     * @deprecated Moving away from hard-coded windows
     */
    public void dataSelectorToFrontIfOk() {
        dataSelectorToFront();
    }


    /**
     * Show data selector.  Called by reflection, public
     * by implementation.  Do not call directly
     *
     * @return Was there a data selector shown
     */
    public boolean showDataSelector() {
        Object comp = findComponent("idv.dataselector");
        if (comp != null) {
            GuiUtils.showComponentInTabs((JComponent) comp);
            return true;
        }
        return false;
    }



    /**
     * If there is a data selector window around then show it.
     *
     * @return true if successful
     */
    public boolean dataSelectorToFront() {
        return showDataSelector();
    }



    /**
     * Create the first data selector.
     * @deprecated not used
     */
    public void createInitialDataSelector() {
        //NOOP

    }



    /**
     *  Create a {@link DataSelector} window.
     * Put it in its own window  if the parameter inOwnWindow is true.
     *
     * @param inOwnWindow Should the  data selector be in its own window.
     * @return The new  {@link DataSelector}
     */
    public DataSelector createDataSelector(boolean inOwnWindow) {
        return createDataSelector( !inOwnWindow, inOwnWindow);
    }


    /**
     * Create the data selector
     *
     * @param horizontalOrientation Do we have all 4 components horizontal
     * or do we stack the 'Displays' and 'Times' component
     * @param inOwnWindow Should we popup a window
     *
     * @return The selector
     */
    public DataSelector createDataSelector(boolean horizontalOrientation,
                                           boolean inOwnWindow) {
        DataSelector selector =
        //If its in its  own window we do the normal vertical orientation
        //Else we do a hor. orientation
        new DataSelector(getIdv(), horizontalOrientation,
                         getIdv().getProperty(PROP_SHOWFORMULAS, true)
                         ? getIdv().getJythonManager()
                             .getDescriptorDataSource()
                         : null);
        initDataSourceHolder(selector, inOwnWindow);
        return selector;
    }


    /**
     * Popup a {@link DataTreeDialog} to let the user select a
     * {@link ucar.unidata.data.DataChoice} that the given control
     * descriptor is applicable to.
     *
     * @param descriptor The control descriptor
     * @return The selected data choice
     */
    public DataChoice selectDataChoice(ControlDescriptor descriptor) {
        List dataSourcesForTree = getDataSourcesForGui();
        if (dataSourcesForTree.size() == 0) {
            return null;
        }
        DataTreeDialog dataDialog =
            new DataTreeDialog(getIdv(), getFrame(), null,
                               Misc.newList(descriptor.getCategories()),
                               Misc.newList("Please select a data choice:"),
                               false, dataSourcesForTree);
        List selected = dataDialog.getSelected();
        dataDialog.dispose();
        if ((selected == null) || selected.isEmpty()) {
            return null;
        }
        return (DataChoice) selected.get(0);
    }

    /**
     *  Popup a dialog containing a DataTree for each operand in the given operands list
     *  Return a List of DataChoice's the user selects or null if they canceled.
     *
     * @param operands List of param names
     * @return List of {@link ucar.unidata.data.DataChoice}s
     */
    public List selectDataChoices(List operands) {
        if (operands.size() == 0) {
            return new ArrayList();
        }
        Hashtable choicesWeAlreadyHave = new Hashtable();
        List      labels               = new ArrayList();
        List      categories           = new ArrayList();
        List      patterns             = new ArrayList();

        //First go thru the list and see if there are any operands of the 
        //form <data source description>:<param description>
        //or [<data source description>:<param description>]
        //Where <data source description> could be:
        //#N -> get the Nth data source
        //alias -> lookup the alias
        //url or filename -> Load this as needed
        //and <param description> could be:
        //#N -> Get the Nth data choice from the given data source
        //name or canonical name 
        for (int i = 0; i < operands.size(); i++) {
            DataOperand operand = (DataOperand) operands.get(i);
            DataChoice  choice  = null;
            //Check if we have a datasource:paramname
            String dataSourceName = operand.getDataSourceName();
            if (dataSourceName != null) {
                DataSource newDataSource = null;
                //Look for the datasource
                if (dataSourceName.startsWith("#")) {
                    //It is an index into the list
                    int index =
                        new Integer(dataSourceName.substring(1)).intValue()
                        - 1;
                    List dataSources = getDataSourcesForGui();
                    if ((index < dataSources.size()) && (index >= 0)) {
                        newDataSource = (DataSource) dataSources.get(index);
                    }
                } else {
                    //It is either an alias or an url.filename
                    DataSourceResults results =
                        getIdv().createDataSource(dataSourceName, null, null,
                            true);
                    if (results.anyFailed()) {
                        showResults(results);
                        return null;
                    }
                    //Can this happen?
                    if ( !results.anyOk()) {
                        return null;
                    }
                    newDataSource =
                        (DataSource) results.getDataSources().get(0);
                }
                if (newDataSource != null) {
                    choice =
                        newDataSource.findDataChoice(operand.getParamName());
                }
            }

            if (choice != null) {
                choicesWeAlreadyHave.put(operand, choice);
            } else {
                patterns.add(operand.getProperty("pattern"));
                labels.add(operand.getLabel());
                categories.add(operand.getCategories());
            }
        }


        List dataSourcesForTree = getIdv().getAllDataSources();
        if (dataSourcesForTree.size() == 0) {
            LogUtil.userMessage("No available data sources");
            return null;
        }

        List selected = new ArrayList();

        //If we still have unfound choices then popup the dialog
        if (labels.size() > 0) {
            DataTreeDialog dataDialog = new DataTreeDialog(getIdv(),
                                            getFrame(), null, categories,
                                            labels, patterns, false,
                                            dataSourcesForTree);


            selected = dataDialog.getSelected();
            dataDialog.dispose();
            if (selected == null) {
                //System.err.println ("selected null" + labels);
                return null;
            }
            if (selected.size() == 0) {
                //System.err.println ("selected ==0" + labels);
                return null;
            }
        }

        int  selectedIdx  = 0;
        List finalChoices = new ArrayList();
        for (int i = 0; i < operands.size(); i++) {
            DataOperand operand   = (DataOperand) operands.get(i);
            String      paramName = operand.getParamName();
            DataChoice choice =
                (DataChoice) choicesWeAlreadyHave.get(operand);
            if (choice == null) {
                choice = (DataChoice) selected.get(selectedIdx);
                selectedIdx++;
            }
            List times = operand.getTimeIndices();
            if (times != null) {
                choice.setTimeSelection(times);
            }
            finalChoices.add(choice);
        }
        return finalChoices;
    }



    /**
     * Popup a JTextField containing dialog that allows  the user
     * to enter text values, one for each name in the userChoices List.
     * This strips off any leading &quot;user_&quot; and converts
     * any underscores into spaces in the userChoices list.
     *
     * @param msg The message to display in the GUI
     * @param userOperands List of DataOperand-s, one for each value
     * @return List of Strings the user entered or null if they cancelled
     */
    public List selectUserChoices(String msg, List userOperands) {
        if (operandCache == null) {
            operandCache =
                (Hashtable) getStore().getEncodedFile("operandcache.xml");
            if (operandCache == null) {
                operandCache = new Hashtable();
            }
        }
        List fields     = new ArrayList();
        List components = new ArrayList();
        for (int i = 0; i < userOperands.size(); i++) {
            DataOperand operand       = (DataOperand) userOperands.get(i);
            String      label         = operand.getLabel();
            String      dflt          = operand.getUserDefault();
            String      cachedOperand = (String) operandCache.get(label);
            if (cachedOperand != null) {
                dflt = cachedOperand;
            }

            JTextField f = new JTextField(8);
            fields.add(f);
            if (dflt != null) {
                f.setText(dflt);
            }
            label = StringUtil.replace(label, "_", " ");
            components.add(new JLabel(label, SwingConstants.RIGHT));
            components.add(f);
        }
        GuiUtils.tmpColFills = new int[] { GridBagConstraints.HORIZONTAL,
                                           GridBagConstraints.NONE,
                                           GridBagConstraints.NONE };
        Component contents = GuiUtils.topCenter(new JLabel(msg),
                                 GuiUtils.doLayout(components, 2, 6, 2));
        if ( !GuiUtils.showOkCancelDialog(getFrame(), "Select input",
                                          contents, null, fields)) {
            return null;
        }
        List values = new ArrayList();
        for (int i = 0; i < userOperands.size(); i++) {
            DataOperand operand = (DataOperand) userOperands.get(i);
            String      label   = operand.getLabel();
            String      value = ((JTextField) fields.get(i)).getText().trim();
            operandCache.put(label, value);
            values.add(value);
        }
        getStore().putEncodedFile("operandcache.xml", operandCache);
        return values;
    }






    /** Just some haiku stuff */
    List haikus;

    /** Just some haiku stuff */
    int haikuCnt = 0;

    /** Just some haiku stuff */
    boolean doingHaiku = false;

    /** Just some haiku stuff */
    Font[] fonts;

    /** Just some haiku stuff */
    List haikuUsers;

    /** Just some haiku stuff */
    List haikuActions;

    /** Just some haiku stuff */
    JLabel haikuL1;

    /** Just some haiku stuff */
    JLabel haikuL2;

    /** Just some haiku stuff */
    JLabel haikuL3;



    /**
     * Just some haiku stuff
     */
    private void runHaiku() {
        while (doingHaiku) {
            if (haikuCnt >= haikus.size()) {
                haikuCnt = 0;
            }
            List haiku = (List) haikus.get(haikuCnt);
            while (haiku.size() < 3) {
                haiku.add("");
            }
            haikuL1.setText(haiku.get(0).toString());
            haikuL2.setText(haiku.get(1).toString());
            haikuL3.setText(haiku.get(2).toString());
            haikuCnt++;
            try {
                Misc.sleep(6000);
            } catch (Exception exc) {}
        }

    }

    /**
     * Just some haiku stuff
     */
    private void doHaiku() {
        if (haikus == null) {
            haikus = new ArrayList();
            int cnt = 1;
            while (true) {
                String list = getIdv().getProperty("haiku" + cnt,
                                  (String) null);
                cnt++;
                if (list == null) {
                    break;
                }
                haikus.add(StringUtil.split(list, ";", true, true));
            }
        }
        doingHaiku = true;
        final Window f = new Window(getFrame());
        haikuL1 = new JLabel("  ");
        haikuL2 = new JLabel("  ");
        haikuL3 = new JLabel("  ");
        Font font = new Font("Dialog", Font.BOLD, 50);
        haikuL1.setFont(font);
        haikuL2.setFont(font);
        haikuL3.setFont(font);
        JPanel p = GuiUtils.leftCenter(new JLabel("             "),
                                       GuiUtils.topCenter(null,
                                           GuiUtils.vbox(haikuL1, haikuL2,
                                               haikuL3)));
        GuiUtils.setBackgroundOnTree(p, Color.blue);
        //   f.getContentPane ().add (p);
        p.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                doingHaiku = false;
                f.setVisible(false);

            }
        });
        Misc.runInABit(2000, new Runnable() {
            public void run() {
                runHaiku();
            }
        });

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        screenSize = new Dimension(screenSize.width + 20,
                                   screenSize.height + 20);
        p.setMinimumSize(screenSize);
        p.setPreferredSize(screenSize);
        f.add(p);
        f.pack();
        f.setLocation(-10, -10);
        f.setVisible(true);
        //f.toFront();   // this brings the getFrame() frame to the front
    }


    /** Just some haiku stuff */
    boolean haikuUserOk = true;

    /**
     * Just some haiku stuff
     *
     * @param action some haiku stuff
     * @return some haiku stuff
     */
    public boolean checkHaiku(String action) {
        if ( !haikuUserOk) {
            return false;
        }
        boolean didHaiku = getStore().get("didhaiku", false);
        if (didHaiku) {
            return false;
        }

        // didHaiku = false;
        if ( !didHaiku) {
            if (haikuUsers == null) {
                haikuUsers =
                    StringUtil.split(getIdv().getProperty("haiku.users", ""),
                                     ";", true, true);
                haikuActions =
                    StringUtil.split(getIdv().getProperty("haiku.actions",
                        ""), ";", true, true);
            }
            haikuUserOk =
                haikuUsers.contains(getStateManager().getUserName());
            Object actionOk = StringUtil.findMatch(action, haikuActions,
                                  null);
            if ((actionOk != null) && haikuUserOk) {
                getStore().put("didhaiku", true);
                getStore().save();
                doHaiku();
                return true;
            }
        }
        return false;
    }



    /**
     * Capture an image from the first active view managers
     *
     * @param filename The image filename
     * @deprecated Use ImageGenerator.captureImage
     */
    public void captureImage(String filename) {
        getImageGenerator().captureImage(filename);
    }


    /**
     * Capture a movie from the first view manager
     *
     * @param filename The movie  filename
     * @deprecated Use ImageGenerator.captureMovie
     */
    public synchronized void captureMovie(String filename) {
        getImageGenerator().captureMovie(filename);
    }



    /**
     * Show the support request form
     */
    public void showSupportForm() {
        showSupportForm("", "");
    }


    /**
     * Append a string and object to the buffer
     *
     * @param sb  StringBuffer to append to
     * @param name  Name of the object
     * @param value  the object value
     */
    private void append(StringBuffer sb, String name, Object value) {
        sb.append("<b>" + name + "</b>: " + value + "<br>");
    }

    /**
     * Show the support request form
     *
     * @param description Default value for the description form entry
     * @param stackTrace The stack trace that caused this error.
     */
    public void showSupportForm(String description, String stackTrace) {
        showSupportForm(description, stackTrace, null);
    }


    /**
     * Show the support request form
     *
     * @param description Default value for the description form entry
     * @param stackTrace The stack trace that caused this error.
     * @param dialog The dialog to put the gui in, if non-null.
     */
    public void showSupportForm(final String description,
                                final String stackTrace,
                                final JDialog dialog) {
        //Must do this in a non-swing thread
        Misc.run(new Runnable() {
            public void run() {
                showSupportFormInThread(description, stackTrace, dialog);
            }
        });
    }




    /**
     * Show the support request form in a non-swing thread. We do this because we cannot
     * call the HttpFormEntry.showUI from a swing thread
     *
     * @param description Default value for the description form entry
     * @param stackTrace The stack trace that caused this error.
     * @param dialog The dialog to put the gui in, if non-null.
     */

    private void showSupportFormInThread(String description,
                                         String stackTrace, JDialog dialog) {

        List         entries = new ArrayList();

        StringBuffer extra   = new StringBuffer("<h3>OS</h3>\n");
        append(extra, "os.name", System.getProperty("os.name"));
        append(extra, "os.arch", System.getProperty("os.arch"));
        append(extra, "os.version", System.getProperty("os.version"));

        extra.append("<h3>Java</h3>\n");

        append(extra, "java.vendor", System.getProperty("java.vendor"));
        append(extra, "java.version", System.getProperty("java.version"));
        append(extra, "java.home", System.getProperty("java.home"));

        StringBuffer javaInfo = new StringBuffer();
        javaInfo.append("Java: home: " + System.getProperty("java.home"));
        javaInfo.append(" version: " + System.getProperty("java.version"));


        Class c = null;
        try {
            c = Class.forName("javax.media.j3d.VirtualUniverse");
            Method method = Misc.findMethod(c, "getProperties",
                                            new Class[] {});
            if (method == null) {
                javaInfo.append("j3d <1.3");
            } else {
                try {
                    Map m = (Map) method.invoke(c, new Object[] {});
                    javaInfo.append(" j3d:" + m.get("j3d.version"));
                    append(extra, "j3d.version", m.get("j3d.version"));
                    append(extra, "j3d.vendor", m.get("j3d.vendor"));
                    append(extra, "j3d.renderer", m.get("j3d.renderer"));
                } catch (Exception exc) {
                    javaInfo.append(" j3d:" + "unknown");
                }
            }
        } catch (ClassNotFoundException exc) {
            append(extra, "j3d", "none");
        }



        HttpFormEntry descriptionEntry;
        HttpFormEntry nameEntry;
        HttpFormEntry emailEntry;
        HttpFormEntry orgEntry;

        entries.add(nameEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "form_data[fromName]", "Name:",
                getStore().get(PROP_HELP_NAME, (String) null)));
        entries.add(emailEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "form_data[email]", "Your Email:",
                getStore().get(PROP_HELP_EMAIL, (String) null)));
        entries.add(orgEntry = new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                "form_data[organization]", "Organization:",
                getStore().get(PROP_HELP_ORG, (String) null)));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_INPUT,
                                      "form_data[subject]", "Subject:"));

        entries.add(
            new HttpFormEntry(
                HttpFormEntry.TYPE_LABEL, "",
                "<html>Please provide a <i>thorough</i> description of the problem you encountered:</html>"));
        entries.add(descriptionEntry =
            new HttpFormEntry(HttpFormEntry.TYPE_AREA,
                              "form_data[description]", "Description:",
                              description, 5, 30, true));

        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_FILE,
                                      "form_data[att2]", "Attachment 1:", "",
                                      false));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_FILE,
                                      "form_data[att3]", "Attachment 2:", "",
                                      false));

        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN,
                                      "form_data[submit]", "", "Send Email"));
        entries.add(
            new HttpFormEntry(
                HttpFormEntry.TYPE_HIDDEN, "form_data[package]", "",
                getStateManager().getProperty(PROP_SUPPORT_PACKAGE, "idv")));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN,
                                      "form_data[p_version]", "",
                                      getStateManager().getVersion()
                                      + " build date:"
                                      + getStateManager().getBuildDate()));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN,
                                      "form_data[opsys]", "",
                                      System.getProperty("os.name")));
        entries.add(new HttpFormEntry(HttpFormEntry.TYPE_HIDDEN,
                                      "form_data[hardware]", "",
                                      javaInfo.toString()));

        JLabel topLabel =
            new JLabel("<html>"
                       + getStateManager().getProperty(PROP_SUPPORT_MESSAGE,
                           "") + "<br>" + "</html>");



        JCheckBox includeBundleCbx =
            new JCheckBox("Include Current State as Bundle", false);

        boolean alreadyHaveDialog = true;
        if (dialog == null) {
            dialog = GuiUtils.createDialog(LogUtil.getCurrentWindow(),
                                           "Support Request Form", true);
            alreadyHaveDialog = false;
        }

        JLabel statusLabel = GuiUtils.cLabel(" ");
        JComponent bottom = GuiUtils.vbox(GuiUtils.left(includeBundleCbx),
                                          statusLabel);

        while (true) {
            //Show form. Check if user pressed cancel.
            statusLabel.setText(" ");
            if ( !HttpFormEntry.showUI(entries, GuiUtils.inset(topLabel, 10),
                                       bottom, dialog, alreadyHaveDialog)) {
                break;
            }
            statusLabel.setText("Posting support request...");

            //Save persistent state
            getStore().put(PROP_HELP_NAME, nameEntry.getValue());
            getStore().put(PROP_HELP_ORG, orgEntry.getValue());
            getStore().put(PROP_HELP_EMAIL, emailEntry.getValue());
            getStore().save();

            List entriesToPost = new ArrayList(entries);

            if ((stackTrace != null) && (stackTrace.length() > 0)) {
                entriesToPost.remove(descriptionEntry);
                String newDescription =
                    descriptionEntry.getValue()
                    + "\n\n******************\nStack trace:\n" + stackTrace;
                entriesToPost.add(
                    new HttpFormEntry(
                        HttpFormEntry.TYPE_HIDDEN, "form_data[description]",
                        "Description:", newDescription, 5, 30, true));
            }

            try {
                extra.append(getIdv().getPluginManager().getPluginHtml());
                extra.append(getResourceManager().getHtmlView());

                entriesToPost.add(new HttpFormEntry("form_data[att_one]",
                        "extra.html", extra.toString().getBytes()));

                if (includeBundleCbx.isSelected()) {
                    entriesToPost.add(
                        new HttpFormEntry(
                            "form_data[att_two]", "bundle.xidv",
                            getIdv().getPersistenceManager().getBundleXml(
                                true).getBytes()));
                }

                String[] results =
                    HttpFormEntry.doPost(
                        entriesToPost,
                        "http://www.unidata.ucar.edu/support/email_support.php");
                if (results[0] != null) {
                    GuiUtils.showHtmlDialog(
                        results[0], "Support Request Response - Error",
                        "Support Request Response - Error", null, true);
                    continue;
                }
                String html = results[1];
                if (html.toLowerCase().indexOf("your email has been sent")
                        >= 0) {
                    LogUtil.userMessage("Your support request has been sent");
                    break;
                } else if (html.toLowerCase().indexOf("required fields")
                           >= 0) {
                    LogUtil.userErrorMessage(
                        "<html>There was a problem submitting your request. <br>Is your email correct?</html>");
                } else {
                    GuiUtils.showHtmlDialog(
                        html, "Unknown Support Request Response",
                        "Unknown Support Request Response", null, true);
                }
            } catch (Exception exc) {
                LogUtil.logException("Doing support request form", exc);
            }
        }
        dialog.dispose();

    }



    /**
     * Create the toolbar preference panel
     *
     * @param preferenceManager The preference manager
     */
    public void addToolbarPreferences(
            IdvPreferenceManager preferenceManager) {
        if (toolbarEditor == null) {
            toolbarEditor = new ToolbarEditor(this);
        }

        PreferenceManager toolbarManager = new PreferenceManager() {
            public void applyPreference(XmlObjectStore theStore,
                                        Object data) {
                if (toolbarEditor.anyChanges()) {
                    toolbarEditor.doApply();
                    updateIconBar();
                }
            }
        };

        preferenceManager.add("Toolbar", "Toolbar icons", toolbarManager,
                              toolbarEditor.getContents(), toolbarEditor);

    }

    /**
     *  Set the date format from the preferences
     */
    public void setDateFormat() {
        TimeZone tz = getIdv().getPreferenceManager().getDefaultTimeZone();
        String dateFormat =
            getIdv().getPreferenceManager().getDefaultDateFormat();
        ucar.unidata.ui.DateTimePicker.setDefaultTimeZone(tz);
        ucar.unidata.ui.Timeline.setDateFormat(dateFormat);
        ucar.unidata.ui.Timeline.setTimeZone(tz);
        if ( !(tz.equals(visad.DateTime.DEFAULT_TIMEZONE)
                && dateFormat.equals(DEFAULT_DATE_FORMAT))) {
            visad.DateTime.setFormatTimeZone(tz);
            visad.DateTime.setFormatPattern(dateFormat);
        } else {
            visad.DateTime.resetFormat();
        }
    }




    /**
     * Sort the list if TwoFacedObjects and string param names.
     * Do this case insensitive
     *
     * @param listToSort incoming list
     *
     * @return sorted list
     */
    private static List sortParamNames(List listToSort) {
        Comparator comparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                String s1 = o1.toString().toLowerCase();
                String s2 = o2.toString().toLowerCase();
                return s1.compareTo(s2);

            }
        };
        Object[] array = listToSort.toArray();
        Arrays.sort(array, comparator);
        return Arrays.asList(array);
    }

    /**
     * A utility method to make a list of menu items of the current parameters
     * and the aliases. This sets the text of the given JTextComponent
     * if delimiter is null. Else it appends the param name to the component
     * with the delimiter
     *
     * @param fld fld to set
     * @param delimiter delimiter to use. If null then do fld.setText
     * @param includeAliases Do we include the list of aliases
     *
     * @return List of menu items
     */
    public static List getParamsMenuItems(final JTextComponent fld,
                                          final String delimiter,
                                          final boolean includeAliases) {

        List names = DataChoice.getCurrentNames();
        names = sortParamNames(names);
        List      topItems = new ArrayList();
        List      items    = new ArrayList();
        Hashtable catMenus = new Hashtable();
        for (int i = 0; i < names.size(); i++) {
            final Object obj     = names.get(i);
            String       name    = obj.toString();
            String       label   = name;

            JMenu        subMenu = null;
            List         toks    = StringUtil.split(name, ">", true, true);
            if (toks.size() == 2) {
                String group = (String) toks.get(0);
                label   = (String) toks.get(1);
                subMenu = (JMenu) catMenus.get(group);
                if (subMenu == null) {
                    subMenu = new JMenu(group);
                    catMenus.put(group, subMenu);
                    items.add(subMenu);
                }
            }
            JMenuItem mi = new JMenuItem(label);
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    String name;
                    if (obj instanceof TwoFacedObject) {
                        name = ((TwoFacedObject) obj).getId().toString();
                    } else {
                        name = obj.toString();
                    }
                    if (delimiter != null) {
                        GuiUtils.appendText(fld, name, delimiter);
                    } else {
                        fld.setText(name);
                    }
                    ActionListener[] listeners = null;
                    if (fld instanceof JTextField) {
                        listeners = ((JTextField) fld).getActionListeners();
                    }
                    ActionEvent action = new ActionEvent(fld, 0, "changed");
                    if (listeners != null) {
                        for (int i = 0; i < listeners.length; i++) {
                            listeners[i].actionPerformed(action);
                        }
                    }
                }
            });
            if (subMenu == null) {
                items.add(mi);
            } else {
                subMenu.add(mi);
            }
        }
        if (items.size() > 0) {
            JMenu namesMenu = GuiUtils.makeMenu("Current Fields", items);
            topItems.add(namesMenu);
        }

        if (includeAliases) {
            items = new ArrayList();
            List aliases = DataAlias.getDataAliasList();
            for (int i = 0; i < aliases.size(); i++) {
                DataAlias    dataAlias = (DataAlias) aliases.get(i);
                final String name      = dataAlias.getName();
                String       lbl       = dataAlias.getName();
                if ((dataAlias.getLabel() != null)
                        && (dataAlias.getLabel().length() > 0)) {
                    lbl = dataAlias.getLabel() + " (" + lbl + ")";
                }
                JMenuItem mi = new JMenuItem(lbl);
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        if (delimiter != null) {
                            GuiUtils.appendText(fld, name, delimiter);
                        } else {
                            fld.setText(name);
                        }
                        ActionListener[] listeners = null;
                        if (fld instanceof JTextField) {
                            listeners =
                                ((JTextField) fld).getActionListeners();
                        }
                        ActionEvent action = new ActionEvent(fld, 0,
                                                 "changed");
                        if (listeners != null) {
                            for (int i = 0; i < listeners.length; i++) {
                                listeners[i].actionPerformed(action);
                            }
                        }
                    }
                });
                items.add(mi);
            }
            topItems.add(GuiUtils.makeMenu("Aliases", items));
        }
        JMenu dummy = GuiUtils.makeMenu("", topItems);
        items = new ArrayList();
        GuiUtils.limitMenuSize(dummy, "Group #", 20);
        for (int i = 0; i < dummy.getItemCount(); i++) {
            items.add(dummy.getMenuComponent(i));
        }

        if (items.size() == 0) {
            items.add(new JMenuItem("No parameters"));
        }
        return items;

    }


    /**
     * A utility method to popup a menu listing the current parameters
     * and the aliases. This sets the text of the given JTextComponent
     * if delimiter is null. Else it appends the param name to the component
     * with the delimiter
     *
     * @param fld fld to set
     * @param e  mouse event
     * @param delimiter delimiter to use. If null then do fld.setText
     * @param includeAliases Do we include the list of aliases
     */
    public static void showParamsPopup(final JTextComponent fld,
                                       MouseEvent e, final String delimiter,
                                       final boolean includeAliases) {
        List       items = getParamsMenuItems(fld, delimiter, includeAliases);
        JPopupMenu popup = GuiUtils.makePopupMenu(items);
        popup.show(fld, e.getX(), e.getY());
    }


    /**
     * Make a JTextField that is for entering a parameter name. This adds
     * a mouselistener to popup the params menu above.
     *
     * @param delimiter If non-null then we append the name from the popup menu,
     * else we set the text on the field.
     * @param includeAliases Include the list of aliases in the popup
     *
     * @return The field
     */
    public static JTextField doMakeParamField(final String delimiter,
            final boolean includeAliases) {
        final JTextField fld = new JTextField("", 20);
        if (delimiter == null) {
            fld.setToolTipText(
                "<html>Parameter name<br>Right mouse to set parameter name</html>");
        } else {
            fld.setToolTipText(
                "<html>Comma separated list of parameter names<br>Right mouse to add parameter names</html>");
        }

        fld.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showParamsPopup(fld, e, delimiter, includeAliases);
                }
            }
        });
        return fld;
    }


}

