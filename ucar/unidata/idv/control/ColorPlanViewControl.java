/*
 * $Id: ColorPlanViewControl.java,v 1.41 2007/08/09 18:38:39 dmurray Exp $
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


import ucar.unidata.data.DataChoice;

import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;


import visad.*;
import visad.VisADException;


import java.rmi.RemoteException;

import java.util.List;
import java.awt.event.*;

import javax.swing.JCheckBox;



/**
 * Class for controlling the display of color shaded plan views of
 * gridded data.
 * @author Jeff McWhirter
 * @version $Revision: 1.41 $
 */
public class ColorPlanViewControl extends PlanViewControl {

    /** flag for smoothing */
    boolean isSmoothed = false;

    /** flag for allowing smoothing */
    boolean allowSmoothing = true;

    /**
     * Default constructor.  Sets the attribute flags used by
     * this particular <code>PlanViewControl</code>
     */
    public ColorPlanViewControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DISPLAYUNIT
                          | FLAG_SELECTRANGE);
    }

    /**
     * Return the color display used by this object.  A wrapper
     * around {@link #getPlanDisplay()}.
     * @return this instance's Grid2Ddisplayable.
     * @see #createPlanDisplay()
     */
    Grid2DDisplayable getGridDisplay() {
        return (Grid2DDisplayable) getPlanDisplay();
    }

    /**
     * Method to create the particular <code>DisplayableData</code> that
     * this this instance uses for data depictions.
     * @return Contour2DDisplayable for this instance.
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
     */
    protected DisplayableData createPlanDisplay()
            throws VisADException, RemoteException {
        Grid2DDisplayable gridDisplay =
            new Grid2DDisplayable("ColorPlanViewControl_"
                                  + ((datachoice != null)
                                     ? datachoice.toString()
                                     : ""), true);
        gridDisplay.setTextureEnable( !isSmoothed);
        addAttributedDisplayable(gridDisplay);
        gridDisplay.setUseRGBTypeForSelect(true);
        return gridDisplay;
    }

    /**
     * Add in any special control widgets to the current list of widgets.
     * @param controlWidgets  list of control widgets
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);
        if (getAllowSmoothing()) {
            JCheckBox toggle = new JCheckBox("", isSmoothed);
            toggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        isSmoothed = ((JCheckBox) e.getSource()).isSelected();
                        // textured == not smoothed
                        getGridDisplay().setTextureEnable( !isSmoothed);

                    } catch (Exception ve) {
                        logException("setSmoothed", ve);
                    }
                }
            });
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Shade Colors:"),
                    GuiUtils.leftCenter(toggle, GuiUtils.filler())));
        }
    }


    /**
     * Set whether this display should be smoothed colors or blocky. Used
     * by XML persistence (bundles) for the most part.
     * @param v  true if smoothed.
     */
    public void setSmoothed(boolean v) {
        isSmoothed = v;
    }

    /**
     * Get whether this display should be smoothed colors or
     * blocky.
     * @return true if smoothed.
     */
    public boolean getSmoothed() {
        return isSmoothed;
    }

    /**
     * Set whether this display should allow smoothed colors or blocky. Used
     * by XML persistence (bundles) for the most part.
     * @param v  true to allowing smoothing.
     */
    public void setAllowSmoothing(boolean v) {
        allowSmoothing = v;
    }

    /**
     * Get whether this display should allow smoothing
     * @return true if allows smoothing.
     */
    public boolean getAllowSmoothing() {
        return allowSmoothing;
    }

}
