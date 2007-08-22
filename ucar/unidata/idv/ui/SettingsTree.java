/*
 * $Id: SettingsTree.java,v 1.1 2007/08/20 20:54:30 jeffmc Exp $
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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.data.GeoSelectionPanel;


import ucar.unidata.idv.*;


import ucar.unidata.idv.control.DisplaySetting;

import ucar.unidata.ui.DndTree;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.Util;



import visad.VisADException;




import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.BoxLayout;
import javax.swing.event.*;
import javax.swing.tree.*;




/**
 * This class is a sortof polymorphic dialog/window that manages  selection
 * of times for a datasource, displays/times for a datachoice and (sometime)
 * a window showing a DataTree, list of displays and times.
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.1 $
 */

public class SettingsTree extends DndTree {

    /** _more_ */
    private static ImageIcon settingsIcon;

    /** Reference to the idv */
    private IntegratedDataViewer idv;

    /** _more_ */
    private long lastSettingsTimestamp = -1;

    /** _more_ */
    private DefaultMutableTreeNode settingsRoot;

    /** _more_ */
    private DefaultTreeModel settingsModel;

    /** _more_ */
    private JScrollPane settingsSP;


    /**
     * _more_
     *
     * @param dialog _more_
     * @param theIdv _more_
     */
    public SettingsTree(DataControlDialog dialog,
                        IntegratedDataViewer theIdv) {
        this.idv      = theIdv;
        settingsRoot  = new DefaultMutableTreeNode("Settings");
        settingsModel = new DefaultTreeModel(settingsRoot);
        this.setModel(settingsModel);
        this.setRootVisible(false);
        this.setShowsRootHandles(true);

        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if ( !SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                List selected = getSelectedSettings();
                if (selected.size() == 0) {
                    return;
                }

                List items = new ArrayList();
                if (selected.size() == 1) {
                    items.add(GuiUtils.makeMenuItem("Change Name",
                            SettingsTree.this, "changeName",
                            selected.get(0)));
                }
                items.add(GuiUtils.makeMenuItem("Write Selected to Plugin",
                        idv.getPluginManager(), "addObject", selected));
                items.add(GuiUtils.makeMenuItem("Delete Selected Settings",
                        SettingsTree.this, "deleteSettings", selected));


                JPopupMenu popup = GuiUtils.makePopupMenu(items);
                popup.show(SettingsTree.this, e.getX(), e.getY());

            }
        });



        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                List selected = getSelectedSettings();
                if (e.getKeyCode() == e.VK_DELETE) {
                    deleteSettings(selected);
                } else if ((e.getKeyCode() == e.VK_S) && e.isControlDown()
                           && (selected.size() > 0)) {
                    idv.getPluginManager().addObject(selected);
                }
            }
        });

        this.setToolTipText(
            "<html>Right click to show popu menu;<br>Control-S to save selected to plugin;<br>Delete key to delete selected settings</html>");
        settingsSP = new JScrollPane(this);

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree theTree,
                    Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {

                super.getTreeCellRendererComponent(theTree, value, sel,
                        expanded, leaf, row, hasFocus);
                if (leaf) {
                    if (settingsIcon == null) {
                        settingsIcon = GuiUtils.getImageIcon(
                            "/auxdata/ui/icons/Settings16.png", getClass());
                    }
                    setIcon(settingsIcon);
                } else {
                    setIcon(null);
                }
                return this;
            }
        };
        this.setCellRenderer(renderer);
        updateSettings();
    }


    /**
     * _more_
     *
     * @param displaySetting _more_
     */
    public void changeName(DisplaySetting displaySetting) {
        String name    = displaySetting.getNameWithoutCategory();
        String cat     = displaySetting.getCategory();
        String newName = DisplaySetting.getNewName(idv, null, cat, name);
        if (newName == null) {
            return;
        }
        if (newName.equals(displaySetting.getName())) {
            return;
        }
        DisplaySetting existing =
            idv.getResourceManager().findDisplaySetting(newName);
        if (existing != null) {
            idv.getResourceManager().removeDisplaySetting(existing);
        }

        displaySetting.setName(newName);
        idv.getResourceManager().displaySettingChanged(displaySetting);
        updateSettings();
    }


    /**
     * _more_
     *
     * @param selected _more_
     */
    public void deleteSettings(List selected) {
        idv.getResourceManager().removeDisplaySettings(selected);
        updateSettings();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent getContents() {
        return settingsSP;
    }

    /**
     * _more_
     *
     * @param g _more_
     */
    public void paint(Graphics g) {
        super.paint(g);
        updateSettings();
    }


    /**
     * _more_
     *
     */
    private void updateSettings() {
        //        System.err.println("updateSettings " +lastSettingsTimestamp+"  " + idv.getResourceManager().getDisplaySettingsTimestamp());
        //Check if we need to update
        if (lastSettingsTimestamp
                == idv.getResourceManager().getDisplaySettingsTimestamp()) {
            return;
        }
        lastSettingsTimestamp =
            idv.getResourceManager().getDisplaySettingsTimestamp();

        List settings = idv.getResourceManager().getDisplaySettings();
        Hashtable paths = GuiUtils.initializeExpandedPathsBeforeChange(this,
                              settingsRoot);

        settingsRoot.removeAllChildren();
        for (int i = 0; i < settings.size(); i++) {
            DisplaySetting setting = (DisplaySetting) settings.get(i);
            List cats = StringUtil.split(setting.getName(), ">", true, true);
            if (cats.size() == 0) {
                cats.add("");
            }

            DefaultMutableTreeNode parent   = settingsRoot;
            String                 catSoFar = "";

            for (int catIdx = 0; catIdx < cats.size() - 1; catIdx++) {
                String cat = (String) cats.get(catIdx);
                if (catSoFar.length() == 0) {
                    catSoFar = cat;
                } else {
                    catSoFar = catSoFar + ">" + cat;
                }
                DefaultMutableTreeNode nextParent = null;
                for (int nodeIdx = 0; nodeIdx < parent.getChildCount();
                        nodeIdx++) {
                    DefaultMutableTreeNode child =
                        (DefaultMutableTreeNode) parent.getChildAt(nodeIdx);
                    if (child.getUserObject().toString().equals(cat)) {
                        nextParent = child;
                        break;
                    }
                }
                if (nextParent == null) {
                    nextParent =
                        new DefaultMutableTreeNode(new TwoFacedObject(cat,
                            catSoFar));
                    parent.add(nextParent);
                }
                parent = nextParent;
            }
            String name = cats.get(cats.size() - 1).toString();
            if (setting.getIsLocal()) {
                name = name + " <local>";
            }
            DefaultMutableTreeNode node =
                new DefaultMutableTreeNode(new TwoFacedObject(name, setting));
            parent.add(node);
        }
        settingsModel.nodeStructureChanged(settingsRoot);
        GuiUtils.expandPathsAfterChange(this, paths, settingsRoot);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected List getSelectedSettings() {
        TreePath[] paths    = getSelectionPaths();
        List       settings = new ArrayList();
        if (paths == null) {
            return settings;
        }
        for (int i = 0; i < paths.length; i++) {
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) paths[i].getLastPathComponent();
            if (node.getUserObject() instanceof TwoFacedObject) {
                settings.add(((TwoFacedObject) node.getUserObject()).getId());
            }
        }
        return settings;
    }


    /**
     * _more_
     *
     * @param sourceNode _more_
     *
     * @return _more_
     */
    protected boolean okToDrag(DefaultMutableTreeNode sourceNode) {
        if ( !(sourceNode.getUserObject() instanceof TwoFacedObject)) {
            return false;
        }
        TwoFacedObject tfo = (TwoFacedObject) sourceNode.getUserObject();
        return tfo.getId() instanceof DisplaySetting;
    }

    /**
     * _more_
     *
     * @param sourceNode _more_
     * @param destNode _more_
     *
     * @return _more_
     */
    protected boolean okToDrop(DefaultMutableTreeNode sourceNode,
                               DefaultMutableTreeNode destNode) {
        if ( !(destNode.getUserObject() instanceof TwoFacedObject)) {
            return false;
        }
        TwoFacedObject tfo = (TwoFacedObject) destNode.getUserObject();
        return !(tfo.getId() instanceof DisplaySetting);
    }

    /**
     * _more_
     *
     * @param sourceNode _more_
     * @param destNode _more_
     */
    protected void doDrop(DefaultMutableTreeNode sourceNode,
                          DefaultMutableTreeNode destNode) {
        DisplaySetting displaySetting =
            (DisplaySetting) ((TwoFacedObject) sourceNode.getUserObject())
                .getId();
        String cat =
            (String) ((TwoFacedObject) destNode.getUserObject()).getId();
        String name    = displaySetting.getNameWithoutCategory();
        String newName = DisplaySetting.cleanName(cat + ">" + name);

        DisplaySetting existing =
            idv.getResourceManager().findDisplaySetting(newName);
        if ((existing != null) && (existing != displaySetting)) {
            if ( !GuiUtils.askYesNo(
                    "Overwrite Display Setting",
                    "<html>A display setting with the given category & name already exists.<br>Do you want to overwrite it?</html>")) {
                return;
            }
            idv.getResourceManager().removeDisplaySetting(existing);
        }

        displaySetting.setName(newName);
        idv.getResourceManager().displaySettingChanged(displaySetting);
        updateSettings();
    }



}

