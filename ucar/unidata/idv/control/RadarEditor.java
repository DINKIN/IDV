/*
 * $Id: TransectDrawingControl.java,v 1.41 2006/12/28 19:50:59 jeffmc Exp $
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

package ucar.unidata.idv.control;


import org.python.core.*;
import org.python.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.collab.Sharable;

import ucar.unidata.data.radar.RadarConstants;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.gis.Transect;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.TransectViewManager;


import ucar.unidata.idv.VMManager;

import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.control.drawing.*;
import ucar.unidata.ui.FineLineBorder;

import ucar.unidata.ui.colortable.ColorTableDefaults;
import ucar.unidata.util.ColorTable;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.display.*;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;


import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;


/**
 * A MetApps Display Control for drawing lines on a navigated
 * display.
 *
 * @author MetApps development team
 * @version $Revision: 1.41 $
 */

public class RadarEditor extends DrawingControl {

    private RadarSweepControl radarSweepControl;


    /**
     * Create a new Drawing Control; set attributes.
     */
    public RadarEditor() {
        setCoordType(DrawingGlyph.COORD_LATLON);
        setLineWidth(2);
    }


    public static class MyRadarSweepControl extends RadarSweepControl {
        protected void addToControlContext() {
        }

        public boolean getShowInLegend() {
            return false;
        }

    }

    public void doRemove() throws RemoteException, VisADException {
        if(radarSweepControl!=null) {
            radarSweepControl.doRemove();
        }
        super.doRemove();
    }

    public boolean init(List choices) throws VisADException, RemoteException {
        super.init((DataChoice)null);
        radarSweepControl = new MyRadarSweepControl();
        radarSweepControl.setUse3D(false);
        radarSweepControl.init(getDisplayId(), getCategories(), choices,
                               getControlContext(), "", getDataSelection());
        return true;
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
        tabbedPane.add("Regions", doMakeShapesPanel());
        tabbedPane.add("Radar Display", radarSweepControl.doMakeContents());
        return GuiUtils.centerBottom(tabbedPane, msgLabel);
    }




    /**
     * Overwrite base class method to not show the filled cbx
     *
     * @return false
     */
    protected boolean showFilledCbx() {
        return false;
    }

    /**
     * Overwrite base class method to make the mode panel
     *
     * @param widgets List of panel widgets to add to
     */
    protected void makeModePanel(List widgets) {

        List        commands = Misc.newList(CMD_SELECT, CMD_MOVE,
                                            CMD_STRETCH);
        List shapes = new ArrayList();
        shapes.add(GlyphCreatorCommand.CMD_CLOSEDPOLYGON);
        //        shapes.add(GlyphCreatorCommand.CMD_POLYGON);
        //        shapes.add(GlyphCreatorCommand.CMD_LINE);
        shapes.add(GlyphCreatorCommand.CMD_RECTANGLE);
        ButtonGroup bg       = new ButtonGroup();
        widgets.add(GuiUtils.rLabel("Mode:"));
        widgets.add(GuiUtils.left(GuiUtils.hbox(makeButtonPanel(shapes, bg),
                makeButtonPanel(commands, bg), enabledCbx)));

    }



    /**
     * Don't show time widgets
     *
     * @return false
     */
    protected boolean showTimeWidgets() {
        return false;
    }

    /**
     * Don't show location widgets
     *
     * @return false
     */
    protected boolean showLocationWidgets() {
        return false;
    }



}

