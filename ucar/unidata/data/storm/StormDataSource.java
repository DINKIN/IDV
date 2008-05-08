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

package ucar.unidata.data.storm;


import ucar.unidata.data.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.geoloc.projection.FlatEarth;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.visad.Util;

import visad.*;
import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;


import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 9, 2008
 * Time: 4:57:58 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class StormDataSource extends DataSourceImpl {


    /** _more_          */
    public static final int CATEGORY_DB = 0;  // - disturbance,

    /** _more_          */
    public static final int CATEGORY_TD = 1;  // - tropical depression,

    /** _more_          */
    public static final int CATEGORY_TS = 2;  // - tropical storm,

    /** _more_          */
    public static final int CATEGORY_TY = 3;  // - typhoon,

    /** _more_          */
    public static final int CATEGORY_ST = 4;  // - super typhoon,

    /** _more_          */
    public static final int CATEGORY_TC = 5;  // - tropical cyclone,

    /** _more_          */
    public static final int CATEGORY_HU = 6;  // - hurricane,

    /** _more_          */
    public static final int CATEGORY_SD = 7;  // - subtropical depression,

    /** _more_          */
    public static final int CATEGORY_SS = 8;  // - subtropical storm,

    /** _more_          */
    public static final int CATEGORY_EX = 9;  // - extratropical systems,

    /** _more_          */
    public static final int CATEGORY_IN = 10;  // - inland,

    /** _more_          */
    public static final int CATEGORY_DS = 11;  // - dissipating,

    /** _more_          */
    public static final int CATEGORY_LO = 12;  // - low,

    /** _more_          */
    public static final int CATEGORY_WV = 13;  // - tropical wave,

    /** _more_          */
    public static final int CATEGORY_ET = 14;  // - extrapolated,

    /** _more_          */
    public static final int CATEGORY_XX = 15;  // - unknown.

    /** _more_          */
    public static RealType TYPE_DISTANCEERROR;



    /** _more_          */
    public static final int[] CATEGORY_VALUES = {
        CATEGORY_DB, CATEGORY_TD, CATEGORY_TS, CATEGORY_TY, CATEGORY_ST,
        CATEGORY_TC, CATEGORY_HU, CATEGORY_SD, CATEGORY_SS, CATEGORY_EX,
        CATEGORY_IN, CATEGORY_DS, CATEGORY_LO, CATEGORY_WV, CATEGORY_ET,
        CATEGORY_XX
    };

    /** _more_          */
    public static final String[] CATEGORY_NAMES = {
        "DB", "TD", "TS", "TY", "ST", "TC", "HU", "SD", "SS", "EX", "IN",
        "DS", "LO", "WV", "ET", "XX"
    };

    /** _more_          */
    public static final String ATTR_CATEGORY = "attr.category";


    /** _more_          */
    public static RealType TYPE_STORMCATEGORY;


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public StormDataSource() throws Exception {}

    /**
     * _more_
     *
     * @param descriptor _more_
     * @param name _more_
     * @param description _more_
     * @param properties _more_
     */
    public StormDataSource(DataSourceDescriptor descriptor, String name,
                           String description, Hashtable properties) {
        super(descriptor, name, description, properties);
    }


    protected void initTypes() throws VisADException {
        if(TYPE_STORMCATEGORY == null) {
            TYPE_STORMCATEGORY = ucar.visad.Util.makeRealType("stormcategory",
                                                              "Storm_Category", null);
        }
        if(TYPE_DISTANCEERROR == null) {
              TYPE_DISTANCEERROR =  ucar.visad.Util.makeRealType("forcast location error",
                                             "distance_error",
                                             Util.parseUnit("km"));
        }
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public int getCategory(String name) {
        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            if (name.equals(CATEGORY_NAMES[i])) {
                return CATEGORY_VALUES[i];
            }
        }
        return CATEGORY_XX;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public abstract List<StormInfo> getStormInfos();


    /**
     * _more_
     */
    protected void doMakeDataChoices() {
        List cats = DataCategory.parseCategories("stormtrack", false);
        DataChoice choice = new DirectDataChoice(this, "stormtrack",
                                "Storm Track", "Storm Track", cats,
                                (Hashtable) null);
        addDataChoice(choice);

    }




    /**
     * _more_
     *
     * @param stormInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public abstract StormTrackCollection getTrackCollection(
            StormInfo stormInfo, Hashtable<String,Boolean> waysToUse)
     throws Exception;


    /**
     * _more_
     *
     * @param stormId _more_
     *
     * @return _more_
     */
    public StormInfo getStormInfo(String stormId) {
        List<StormInfo> stormInfos = getStormInfos();
        for (StormInfo sInfo : stormInfos) {
            if (sInfo.getStormId().equals(stormId)) {
                return sInfo;
            }
        }
        return null;
    }


    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public static int getYear(DateTime dttm) throws VisADException {
        GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        cal.setTime(ucar.visad.Util.makeDate(dttm));
        return cal.get(Calendar.YEAR);
    }

    public static void setStormTrackForecastError(StormTrack obsTrack , StormTrack fctTrack)throws VisADException {
        List<StormTrackPoint> obsTrackPoints = obsTrack.getTrackPoints();
        List<StormTrackPoint> fctTrackPoints = fctTrack.getTrackPoints();

        for(StormTrackPoint stp: fctTrackPoints){
            DateTime dt = stp.getTrackPointTime();
            StormTrackPoint stpObs =  getClosestPoint(obsTrackPoints, dt);
            double der = getDistance(stpObs, stp);
            stp.addAttribute(new Real(TYPE_DISTANCEERROR, der));
        }
       
    }
    
    public static StormTrackPoint getClosestPoint(List<StormTrackPoint> aList, DateTime dt){


        int numPoints = aList.size();
        StormTrackPoint stp1 = aList.get(0);
        StormTrackPoint stp2 = aList.get(numPoints-1);

        double pValue = dt.getValue();
        double minDiffLeft = 200000;
        double minDiffRight = 200000;

        for(int i = 0; i < numPoints; i++){
            StormTrackPoint stp11 = aList.get(i);
            StormTrackPoint stp21 = aList.get(numPoints -i -1);

            double p1Value = stp11.getTrackPointTime().getValue();
            double p2Value = stp21.getTrackPointTime().getValue();
            double diff1 = Math.abs(p1Value-pValue);
            double diff2 = Math.abs(p2Value-pValue);

            if(pValue >= p1Value && diff1 < minDiffRight) {
                if(pValue == p1Value) return stp11;
                stp1 = stp11;
                minDiffRight = diff1;

            }

            if(pValue <= p2Value && diff2 < minDiffLeft) {
                if(pValue == p2Value) return stp21;
                stp2 = stp21;
                minDiffLeft = diff2;
            }

        }

        double diff = minDiffLeft + minDiffRight;
        EarthLocation el1 = stp1.getTrackPointLocation();
        EarthLocation el2 = stp1.getTrackPointLocation();

        double lat = ( (diff - minDiffLeft )*el1.getLatitude().getValue()
                    +  (diff - minDiffRight )*el2.getLatitude().getValue() )/diff;
        double lon = ( (diff - minDiffLeft )*el1.getLongitude().getValue()
                     + (diff - minDiffRight )*el2.getLongitude().getValue() )/diff;

        EarthLocation el = new EarthLocationLite(new Real(RealType.Latitude,
                        lat), new Real(RealType.Longitude, lon),
                                   null);

        return  new StormTrackPoint(el, dt , 0, null);


    }

    public static double getDistance(StormTrackPoint p1, StormTrackPoint p2){

        FlatEarth e1    = new FlatEarth();
        FlatEarth e2   = new FlatEarth();

        EarthLocation el1 = p1.getTrackPointLocation();
        EarthLocation el2 = p2.getTrackPointLocation();
        ProjectionPointImpl pp1 = e1.latLonToProj(el1.getLatitude().getValue(),
                                     el1.getLongitude().getValue());
        ProjectionPointImpl pp2 = e2.latLonToProj(el2.getLatitude().getValue(),
                                     el2.getLongitude().getValue());
        return pp1.distance(pp2);

    }
}

