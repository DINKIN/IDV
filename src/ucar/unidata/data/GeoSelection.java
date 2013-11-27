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

package ucar.unidata.data;


import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.HashCodeUtils;
import ucar.unidata.util.Misc;
import visad.Real;


/**
 * Holds geo-location information  - lat/lon bounding box, image size, etc.
 * This is used to pass information from a chooser into a datasource.
 */
public class GeoSelection {

    /** The default bounding box to use for all */
    private static GeoLocationInfo defaultBoundingBox = null;

    /** No stride value */
    public static final int STRIDE_NONE = 0;

    /** No stride value */
    public static final int STRIDE_BASE = 1;

    /** The bounding box */
    private GeoLocationInfo boundingBox;

    /** A flag to note that even though the bounds are null here we really want to use the full spatial bounds */
    private boolean useFullBounds = false;

    /** X stride */
    private int xStride = STRIDE_NONE;

    /** Y stride */
    private int yStride = STRIDE_NONE;

    /** Z stride */
    private int zStride = STRIDE_NONE;

    /** The level */
    private Real level;

    /* Screen Bound*/

    /** _more_ */
    Rectangle screenBoundRect;

    /** _more_ */
    LatLonRect screenLatLonRect;

    /** _more_ */
    //LatLonPoint[] rubberBandBoxPoints;

    /**
     * ctor
     */
    public GeoSelection() {}

    /**
     * ctor
     *
     * @param level The level to use
     */
    public GeoSelection(Real level) {
        this();
        this.level = level;
    }

    /**
     * Copy ctor
     *
     * @param that The  object to copy
     */
    public GeoSelection(GeoSelection that) {
        this();
        this.useFullBounds = that.useFullBounds;
        if (that.boundingBox != null) {
            this.boundingBox = new GeoLocationInfo(that.boundingBox);
        }
        this.xStride = that.xStride;
        this.yStride = that.yStride;
        this.zStride = that.zStride;
        this.level   = that.level;
        if (that.screenBoundRect != null) {
            this.screenBoundRect = that.screenBoundRect;
        }
        //if (that.rubberBandBoxPoints != null) {
        //    this.rubberBandBoxPoints = that.rubberBandBoxPoints;
        //}
        if (that.screenLatLonRect != null) {
            this.screenLatLonRect = that.screenLatLonRect;
        }
    }




    /**
     * ctor
     *
     * @param boundingBox The bounding box. May be null.
     */
    public GeoSelection(GeoLocationInfo boundingBox) {
        this(boundingBox, STRIDE_NONE);
    }


    /**
     * ctor
     *
     * @param boundingBox The bounding box. May be null.
     * @param stride The stride for  x, y and z
     */
    public GeoSelection(GeoLocationInfo boundingBox, int stride) {
        this(boundingBox, stride, stride, stride);
    }



    /**
     * ctor.
     *
     * @param boundingBox The bounding box. May be null.
     * @param xStride X stride
     * @param yStride Y stride
     * @param zStride Z stride
     */
    public GeoSelection(GeoLocationInfo boundingBox, int xStride,
                        int yStride, int zStride) {
        this(boundingBox, xStride, yStride, zStride, null);
    }

    /**
     * ctor.
     *
     * @param boundingBox The bounding box. May be null.
     * @param xStride X stride
     * @param yStride Y stride
     * @param zStride Z stride
     * @param level The level to use
     */
    public GeoSelection(GeoLocationInfo boundingBox, int xStride,
                        int yStride, int zStride, Real level) {
        this(boundingBox, false, xStride, yStride, zStride, level);
    }




    /**
     * ctor.
     *
     * @param boundingBox The bounding box. May be null.
     * @param useFullBounds Use full bounds
     * @param xStride X stride
     * @param yStride Y stride
     * @param zStride Z stride
     * @param level The level to use
     */
    public GeoSelection(GeoLocationInfo boundingBox, boolean useFullBounds,
                        int xStride, int yStride, int zStride, Real level) {

        this.boundingBox         = boundingBox;
        this.useFullBounds       = useFullBounds;
        this.xStride             = xStride;
        this.yStride             = yStride;
        this.zStride             = zStride;
        this.level               = level;
        this.screenBoundRect     = null;
        //this.rubberBandBoxPoints = null;
        this.screenLatLonRect    = null;
    }



    /**
     * Construct a GeoSelection
     *
     * @param boundingBox _more_
     * @param useFullBounds _more_
     * @param xStride _more_
     * @param yStride _more_
     * @param zStride _more_
     * @param level _more_
     * @param screenBoundRect _more_
     * @param screenLatLonRect _more_
     */
    public GeoSelection(GeoLocationInfo boundingBox, boolean useFullBounds,
                        int xStride, int yStride, int zStride, Real level,
                        Rectangle screenBoundRect,
                        //LatLonPoint[] rubberBandBoxPoints,
                        LatLonRect screenLatLonRect) {

        this.boundingBox         = boundingBox;
        this.useFullBounds       = useFullBounds;
        this.xStride             = xStride;
        this.yStride             = yStride;
        this.zStride             = zStride;
        this.level               = level;
        this.screenBoundRect     = screenBoundRect;
        //this.rubberBandBoxPoints = rubberBandBoxPoints;
        this.screenLatLonRect    = screenLatLonRect;
    }

    /**
     * Get the default bbox
     *
     * @return default bbox
     */
    public static GeoLocationInfo getDefaultBoundingBox() {
        if (defaultBoundingBox != null) {
            return new GeoLocationInfo(defaultBoundingBox);
        }
        return null;
    }


    /**
     * Define the global bounding box that is used as the default
     *
     * @param defaultBox default box
     */
    public static void setDefaultBoundingBox(GeoLocationInfo defaultBox) {
        defaultBoundingBox = defaultBox;
    }


    /**
     * Set the bounds using the rectangle
     *
     * @param rect  rect
     */
    public void setLatLonRect(Rectangle2D rect) {
        this.boundingBox = new GeoLocationInfo(rect);
    }




    /**
     * Utility to get a lat lon rect if we have one.
     *
     * @return latlon rectangle
     */
    public LatLonRect getLatLonRect() {
        if (boundingBox == null) {
            return null;
        }
        LatLonPoint ul = new LatLonPointImpl(boundingBox.getMaxLat(),
                                             boundingBox.getMinLon());
        LatLonPoint lr = new LatLonPointImpl(boundingBox.getMinLat(),
                                             boundingBox.getMaxLon());
        return new LatLonRect(ul, lr);
    }


    /**
     * Do we have either a bounding box or any valid strides
     *
     * @return Has anything to subset
     */
    public boolean getHasValidState() {
        return hasSpatialSubset() || hasStride();
    }


    /**
     * Is there a spatial subset defined
     *
     * @return Has spatial subset
     */
    public boolean hasSpatialSubset() {
        return useFullBounds || (boundingBox != null);
    }

    /**
     * Does this selection have any stride other than NONE and BASE
     *
     * @return has a stride defined other than NONE or BASE
     */
    public boolean getHasNonOneStride() {
        return hasStride()
               && (((xStride != STRIDE_NONE) && (xStride != STRIDE_BASE))
                   || ((yStride != STRIDE_NONE) && (yStride != STRIDE_BASE))
                   || ((zStride != STRIDE_NONE) && (zStride != STRIDE_BASE)));

    }



    /**
     * Create a new GeoSelection by merging the two subsets.
     *
     * @param highPriority Take state from this first if defined
     * @param lowPriority Use this state
     *
     * @return The merged subset
     */
    public static GeoSelection merge(GeoSelection highPriority,
                                     GeoSelection lowPriority) {
        if ((highPriority == null) && (lowPriority == null)) {
            return null;
        }

        if (highPriority == null) {
            return new GeoSelection(lowPriority);
        }
        if (lowPriority == null) {
            return new GeoSelection(highPriority);
        }
        int             xStride         = highPriority.hasXStride()
                                          ? highPriority.xStride
                                          : lowPriority.xStride;
        int             yStride         = highPriority.hasYStride()
                                          ? highPriority.yStride
                                          : lowPriority.yStride;
        int             zStride         = highPriority.hasZStride()
                                          ? highPriority.zStride
                                          : lowPriority.zStride;
        GeoLocationInfo bbox            = ((highPriority.boundingBox != null)
                                           ? highPriority.boundingBox
                                           : lowPriority.boundingBox);

        Rectangle       screenBoundRect = highPriority.screenBoundRect;
        // ((highPriority.screenBoundRect != null)
        //  ? highPriority.screenBoundRect
        //  : lowPriority.screenBoundRect);

        //LatLonPoint[] rubberBandBoxPoints = highPriority.rubberBandBoxPoints;
        //  ((highPriority.rubberBandBoxPoints != null)
        //  ? highPriority.rubberBandBoxPoints
        //   : lowPriority.rubberBandBoxPoints);

        LatLonRect screenLatLonRect = highPriority.screenLatLonRect;
        // ((highPriority.screenLatLonRect != null)
        //  ? highPriority.screenLatLonRect
        //  : lowPriority.screenLatLonRect);

        /*   if ((highPriority.boundingBox == null)
                   && (highPriority.rubberBandBoxPoints == null)) {
               rubberBandBoxPoints = null;
           }  */

        if (highPriority.getUseFullBounds()
                && (highPriority.boundingBox == null)) {
            bbox = null;
        }

        Real level = ((highPriority.level != null)
                      ? highPriority.level
                      : lowPriority.level);
        GeoSelection newOne = new GeoSelection(bbox,
                                  highPriority.getUseFullBounds(), xStride,
                                  yStride, zStride, level, screenBoundRect,
                                  screenLatLonRect);

        return newOne;
    }



    /**
     * Set the Stride property.
     *
     * @param value The new value for Stride
     */
    public void setStride(int value) {
        xStride = value;
        yStride = value;
        zStride = value;
    }


    /**
     * Clear the stride settings
     */
    public void clearStride() {
        xStride = STRIDE_NONE;
        yStride = STRIDE_NONE;
        zStride = STRIDE_NONE;
    }


    /**
     * Do we have a stride defined
     *
     * @return x stride defined
     */
    public boolean hasXStride() {
        return xStride != STRIDE_NONE;
    }

    /**
     * Do we have a stride defined
     *
     * @return y stride defined
     */
    public boolean hasYStride() {
        return yStride != STRIDE_NONE;
    }

    /**
     * Do we have a stride defined
     *
     * @return z stride defined
     */
    public boolean hasZStride() {
        return zStride != STRIDE_NONE;
    }

    /**
     * Do we have any stride defined
     *
     * @return any stride defined
     */
    public boolean hasStride() {
        return hasXStride() || hasYStride() || hasZStride();
    }

    /**
     * Set the XStride property.
     *
     * @param value The new value for XStride
     */
    public void setXStride(int value) {
        xStride = value;
    }

    /**
     * Get the XStride property.
     *
     * @return The XStride
     */
    public int getXStride() {
        return xStride;
    }

    /**
     * Get the XStride property if it is valid. Else return the base stride.
     *
     * @return The x stride to use
     */
    public int getXStrideToUse() {
        return (hasXStride()
                ? xStride
                : STRIDE_BASE);
    }

    /**
     * Set the YStride property.
     *
     * @param value The new value for YStride
     */
    public void setYStride(int value) {
        yStride = value;
    }

    /**
     * Get the YStride property.
     *
     * @return The YStride
     */
    public int getYStride() {
        return yStride;
    }


    /**
     * Get the YStride property if it is valid. Else return the base stride.
     *
     * @return The y stride to use
     */
    public int getYStrideToUse() {
        return (hasYStride()
                ? yStride
                : STRIDE_BASE);
    }



    /**
     * Set the ZStride property.
     *
     * @param value The new value for ZStride
     */
    public void setZStride(int value) {
        zStride = value;
    }

    /**
     * Get the ZStride property.
     *
     * @return The ZStride
     */
    public int getZStride() {
        return zStride;
    }


    /**
     * Get the ZStride property if it is valid. Else return the base stride.
     *
     * @return The z stride to use
     */
    public int getZStrideToUse() {
        return (hasZStride()
                ? zStride
                : STRIDE_BASE);
    }





    /**
     * Set the BoundingBox property.
     *
     * @param value The new value for BoundingBox
     */
    public void setBoundingBox(GeoLocationInfo value) {
        boundingBox = value;
    }

    /**
     * Get the BoundingBox property.
     *
     * @return The BoundingBox
     */
    public GeoLocationInfo getBoundingBox() {
        return boundingBox;
    }



    /**
     * tostring
     *
     * @return tostring
     */
    public String toString() {
        return "x:" + xStride + " " + "y:" + yStride + " " + "z:" + zStride
               + " bbox:" + boundingBox;
    }


    /**
     * Set the Level property.
     *
     * @param value The new value for Level
     */
    public void setLevel(Real value) {
        level = value;
    }

    /**
     * Get the Level property.
     *
     * @return The Level
     */
    public Real getLevel() {
        return level;
    }


    /**
     * hash me
     *
     * @return my hash code
     */
    public int hashCode() {
        int seed = ((boundingBox != null)
                    ? boundingBox.hashCode()
                    : HashCodeUtils.SEED);
        return HashCodeUtils.hash(HashCodeUtils.hash(HashCodeUtils.hash(seed,
                xStride), yStride), zStride);
    }



    /**
     * equals
     *
     * @param obj obj
     *
     * @return is equals
     */
    public boolean equals(Object obj) {
        if ( !(obj instanceof GeoSelection)) {
            return false;
        }
        GeoSelection that = (GeoSelection) obj;
        return (this.xStride == that.xStride)
               && (this.yStride == that.yStride)
               && (this.zStride == that.zStride)
               && Misc.equals(this.boundingBox, that.boundingBox)
               && Misc.equals(this.level, that.level);
    }


    /**
     *  Set the UseFullBounds property.
     *
     *  @param value The new value for UseFullBounds
     */
    public void setUseFullBounds(boolean value) {
        useFullBounds = value;
    }

    /**
     *  Get the UseFullBounds property.
     *
     *  @return The UseFullBounds
     */
    public boolean getUseFullBounds() {
        return useFullBounds;
    }


    /**
     * _more_
     *
     * @param screenBoundRect _more_
     */
    public void setScreenBound(Rectangle screenBoundRect) {
        this.screenBoundRect = screenBoundRect;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Rectangle getScreenBound() {
        return this.screenBoundRect;
    }

    /**
     * _more_
     *
     * @param screenLatLonRect _more_
     */
    public void setScreenLatLonRect(LatLonRect screenLatLonRect) {
        this.screenLatLonRect = screenLatLonRect;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public LatLonRect getScreenLatLonRect() {
        return this.screenLatLonRect;
    }

}
