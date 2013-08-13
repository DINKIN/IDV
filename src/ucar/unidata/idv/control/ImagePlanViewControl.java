/*
 * Copyright 1997-2013 Unidata Program Center/University Corporation for
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


import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.adde.AddeImageURL;

import ucar.unidata.data.*;
import ucar.unidata.data.grid.DerivedGridFactory;
import ucar.unidata.data.imagery.*;
import ucar.unidata.geoloc.*;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.chooser.adde.AddeImageChooser;
import ucar.unidata.util.*;

import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.data.AreaImageFlatField;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;

import ucar.visad.display.RubberBandBox;

import visad.*;

import visad.data.mcidas.AREACoordinateSystem;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.MapProjection;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


import javax.swing.*;


/**
 * Class for controlling the display of images.  Designed for brightness
 * images with range of 0 to 255.
 *
 * @author IDV Development Group
 */
public class ImagePlanViewControl extends PlanViewControl {



    //  NB: For now, we don't subclass ColorPlanViewControl because we get
    //  the DataRange widget from getControlWidgets.  Might want this in
    //  the future.  It would be simpler if we wanted to include that.

    /**
     * Default constructor.  Sets the attribute flags used by
     * this particular <code>PlanViewControl</code>
     */
    public ImagePlanViewControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DISPLAYUNIT
                          | FLAG_SKIPFACTOR | FLAG_TEXTUREQUALITY);
        setCanDoProgressiveResolution(true);
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
            new Grid2DDisplayable("ImagePlanViewControl_"
                                  + ((datachoice != null)
                                     ? datachoice.toString()
                                     : ""), true);
        gridDisplay.setTextureEnable(true);
        gridDisplay.setCurvedSize(getTextureQuality());
        /* TODO: Find out why this causes redisplays
        if (BaseImageControl.EMPTY_IMAGE != null) {
            gridDisplay.loadData(BaseImageControl.EMPTY_IMAGE);
        }
        */
        //gridDisplay.setUseRGBTypeForSelect(true);
        addAttributedDisplayable(gridDisplay);
        return gridDisplay;
    }

    /**
     *  Use the value of the texture quality to set the value on the display
     *
     * @throws RemoteException  problem with Java RMI
     * @throws VisADException   problem setting attribute on Displayable
     */
    protected void applyTextureQuality()
            throws VisADException, RemoteException {
        if (getGridDisplay() != null) {
            getGridDisplay().setCurvedSize(getTextureQuality());
        }
    }

    /**
     * Called to initialize this control from the given dataChoice;
     * sets levels controls to match data; make data slice at first level;
     * set display's color table and display units.
     *
     * @param dataChoice  choice that describes the data to be loaded.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice dataChoice)
            throws VisADException, RemoteException {
        List dsList = new ArrayList();
        dataChoice.getDataSources(dsList);
        DataSourceImpl dsImpl = (DataSourceImpl) dsList.get(0);
        if (dsImpl instanceof AddeImageDataSource) {
            AddeImageDataSource aImageDS = (AddeImageDataSource) dsImpl;
            AddeImageDataSource.ImagePreviewSelection regionSelection =
                aImageDS.previewSelection;
            AddeImageSelectionPanel advanceSelection =
                aImageDS.advancedSelection;

            ProjectionRect rect =
                regionSelection.display.getNavigatedPanel()
                    .getSelectedRegion();
            if ( !aImageDS.getIsReload()) {
                isProgressiveResolution =
                    advanceSelection.getIsProgressiveResolution();
            }

            String regionOption = regionSelection.getRegionOption();

            if (rect != null) {
                ProjectionImpl projectionImpl =
                    regionSelection.display.getProjectionImpl();
                LatLonRect latLonRect =
                    projectionImpl.getLatLonBoundingBox(rect);
                GeoLocationInfo gInfo;
                if(latLonRect.getHeight() != latLonRect.getHeight()){
                    //conner point outside the earth
                    LatLonPointImpl cImpl = projectionImpl.projToLatLon(rect.x + rect.getWidth()/2, rect.y + rect.getHeight()/2);
                    LatLonPointImpl urImpl = projectionImpl.projToLatLon(rect.x + rect.getWidth(), rect.y + rect.getHeight());
                    LatLonPointImpl ulImpl = projectionImpl.projToLatLon(rect.x, rect.y + rect.getHeight());
                    LatLonPointImpl lrImpl = projectionImpl.projToLatLon(rect.x + rect.getWidth(), rect.y);
                    LatLonPointImpl llImpl = projectionImpl.projToLatLon(rect.x, rect.y);

                    double maxLat = Double.NaN;
                    double minLat = Double.NaN;
                    double maxLon = Double.NaN;
                    double minLon = Double.NaN;
                    if(cImpl.getLatitude() != cImpl.getLatitude()) {
                        //do nothing
                    } else if(lrImpl.getLatitude() == lrImpl.getLatitude()){
                       //upper left conner
                        maxLat = cImpl.getLatitude() + (cImpl.getLatitude() - lrImpl.getLatitude());
                        minLat = lrImpl.getLatitude();
                        maxLon = lrImpl.getLongitude();
                        minLon = cImpl.getLongitude() - (lrImpl.getLongitude() - cImpl.getLongitude());
                    } else if(llImpl.getLatitude() == llImpl.getLatitude()){
                        //upper right conner
                        maxLat = cImpl.getLatitude() + (cImpl.getLatitude() - llImpl.getLatitude());
                        minLat = llImpl.getLatitude();
                        maxLon = cImpl.getLongitude() + (cImpl.getLongitude() - lrImpl.getLongitude());
                        minLon = lrImpl.getLongitude();
                    } else if(urImpl.getLatitude() == urImpl.getLatitude()){
                        // lower left conner
                        maxLat = urImpl.getLatitude();
                        minLat = cImpl.getLatitude() - (urImpl.getLatitude() - cImpl.getLatitude() );
                        maxLon = urImpl.getLongitude();
                        minLon = cImpl.getLongitude() - (urImpl.getLongitude() - cImpl.getLongitude());
                    } else if(ulImpl.getLatitude() == ulImpl.getLatitude()){
                        // lower right conner
                        maxLat = ulImpl.getLatitude();
                        minLat = cImpl.getLatitude() - (ulImpl.getLatitude() - cImpl.getLatitude());
                        maxLon = cImpl.getLongitude() + (cImpl.getLongitude() - ulImpl.getLongitude());
                        minLon = ulImpl.getLongitude();
                    }

                    gInfo = new GeoLocationInfo(maxLat,
                            LatLonPointImpl.lonNormal(minLon), minLat,
                            LatLonPointImpl.lonNormal(maxLon));
                    dataSelection.putProperty(
                            DataSelection.PROP_HASSCONNER,
                            "true");
                } else {
                    gInfo = new GeoLocationInfo(latLonRect);
                }
                GeoSelection    gs    = new GeoSelection(gInfo);
                NavigatedDisplay navDisplay =
                    (NavigatedDisplay) getViewManager().getMaster();
                Rectangle screenBoundRect = navDisplay.getScreenBounds();
                gs.setScreenBound(screenBoundRect);
                gs.setScreenLatLonRect(navDisplay.getLatLonRect());
                if ( !isProgressiveResolution) {
                    gs.setXStride(aImageDS.getEleMag());
                    gs.setYStride(aImageDS.getLineMag());
                }
                dataSelection.setGeoSelection(gs);

            } else {
                GeoSelection gs = new GeoSelection();
                NavigatedDisplay navDisplay =
                    (NavigatedDisplay) getViewManager().getMaster();
                Rectangle screenBoundRect = navDisplay.getScreenBounds();
                gs.setScreenBound(screenBoundRect);
                gs.setScreenLatLonRect(navDisplay.getLatLonRect());
                if (dataSelection.getGeoSelection() != null) {
                    LatLonPoint[] llp0 =
                        dataSelection.getGeoSelection()
                            .getRubberBandBoxPoints();
                    gs.setRubberBandBoxPoints(llp0);
                }
                if (getViewManager() instanceof MapViewManager) {
                    if (regionSelection.getRegionOption().equals(
                            "Use Display Area")) {
                        getViewManager().setProjectionFromData(false);
                        List<TwoFacedObject> coords =
                            navDisplay.getScreenSidesCoordinates();
                        double[]      elid = (double[]) coords.get(1).getId();
                        EarthLocation el   =
                            navDisplay.getEarthLocation(elid);
                        double maxLat =
                            el.getLatLonPoint().getLatitude().getValue();
                        elid = (double[]) coords.get(2).getId();
                        el   = navDisplay.getEarthLocation(elid);
                        double minLat =
                            el.getLatLonPoint().getLatitude().getValue();
                        elid = (double[]) coords.get(3).getId();
                        el   = navDisplay.getEarthLocation(elid);
                        double maxLon =
                            el.getLatLonPoint().getLongitude().getValue();
                        elid = (double[]) coords.get(4).getId();
                        el   = navDisplay.getEarthLocation(elid);
                        double minLon =
                            el.getLatLonPoint().getLongitude().getValue();
                        GeoLocationInfo glInfo =
                            new GeoLocationInfo(maxLat,
                                LatLonPointImpl.lonNormal(minLon), minLat,
                                LatLonPointImpl.lonNormal(maxLon));

                        gs.setBoundingBox(glInfo);
                    }

                }
                dataSelection.setGeoSelection(gs);
            }
            dataSelection.putProperty(
                DataSelection.PROP_PROGRESSIVERESOLUTION,
                isProgressiveResolution);
            dataSelection.putProperty(DataSelection.PROP_REGIONOPTION,
                                      regionOption);
        }

        boolean result = super.setData(dataChoice);
        if ( !result) {
            userMessage("Selected image(s) not available");
        }
        return result;
    }

    /**
     * Get the initial color table for the data
     *
     * @return  intitial color table
     */
    protected ColorTable getInitialColorTable() {
        ColorTable colorTable = super.getInitialColorTable();
        if (colorTable.getName().equalsIgnoreCase("default")) {
            colorTable = getDisplayConventions().getParamColorTable("image");
        }
        return colorTable;
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
     * Get whether this display should allow smoothing
     * @return true if allows smoothing.
     */
    public boolean getAllowSmoothing() {
        return false;
    }


    /**
     * Get the initial range for the data and color table.
     * Optimized for brightness images with range of 0 to 255.
     *
     * @return  initial range
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Range getInitialRange() throws RemoteException, VisADException {

        // WARNING:  Twisty-turny logic below
        // try for the parameter.
        Range range = getDisplayConventions().getParamRange(paramName,
                          getDisplayUnit());

        // see if one is defined for the color table.
        if (range == null) {
            range = getRangeFromColorTable();
            if ((range != null) && (range.getMin() == range.getMax())) {
                range = null;
            }
        }

        // look for the default for "image" - hopefully it never changes
        boolean usingImage = false;
        Range imageRange = getDisplayConventions().getParamRange("image",
                               getDisplayUnit());
        /*
        if (range == null) {
            range = imageRange;
        }
        */
        if ((range != null) && Misc.equals(range, imageRange)) {
            usingImage = true;
        }

        // check to see if the range of the data is outside the range
        // of the default. This will be wrong if someone redefined what image
        // is supposed to be (0-255).
        if ((range != null) && usingImage
                && (getGridDataInstance() != null)) {
            Range dataRange = getDataRangeInColorUnits();
            if (dataRange != null) {
                if ((range.getMin() > dataRange.getMin())
                        || (range.getMax() < dataRange.getMax())) {
                    range = dataRange;
                }
            }
        }
        if (range == null) {
            range = super.getInitialRange();
        }
        return range;
    }

    /**
     * Get the slice for the display
     *
     * @param slice  slice to use
     *
     * @return slice with skip value applied
     *
     * @throws VisADException  problem subsetting the slice
     */
    protected FieldImpl getSliceForDisplay(FieldImpl slice)
            throws VisADException {
        checkImageSize(slice);
        return super.getSliceForDisplay(slice);
    }

    /**
     * Return the label that is to be used for the skip widget
     * This allows derived classes to override this and provide their
     * own name,
     *
     * @return Label used for the line width widget
     */
    public String getSkipWidgetLabel() {
        return "Pixel Sampling";
    }

    /**
     * What label to use for the data projection
     *
     * @return label
     */
    protected String getDataProjectionLabel() {
        return "Use Native Image Projection";
    }

    /**
     * Is this a raster display
     *
     * @return true
     */
    public boolean getIsRaster() {
        return true;
    }


    /**
     * _more_
     *
     * @return _more_
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


}
