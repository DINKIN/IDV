/*
 * Copyright 2010 UNAVCO, 6350 Nautilus Drive, Boulder, CO 80301
 * http://www.unavco.org
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
 * 
 */

package ucar.unidata.data.point;


import ucar.unidata.data.*;
import ucar.unidata.geoloc.*;

import ucar.unidata.geoloc.projection.UtmProjection;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.Util;
import ucar.visad.display.RGBDisplayable;

import ucar.visad.display.VolumeDisplayable;

import visad.*;

import visad.georef.MapProjection;


import visad.georef.TrivialMapProjection;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.geom.Rectangle2D;


import java.io.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;




/**
 * A display control for volume rendering of a 3D grid
 *
 * @author Unidata IDV Development Team
 * @version $Revision: 1.11 $
 */

public class PointCloudDataSource extends FilesDataSource {

    /** _more_          */
    public static final float GRID_MISSING = -99999.9f;

    /** _more_ */
    public static final int INDEX_ALT = 0;

    /** _more_ */
    public static final int INDEX_LON = 1;

    /** _more_ */
    public static final int INDEX_LAT = 2;


    /** _more_ */
    private int skip = 10;

    /** _more_ */
    private JTextField skipFld;

    /** _more_ */
    private String delimiter = ",";

    /** _more_ */
    private JTextField delimiterFld;

    /** _more_ */
    private JTextField colorByIndexFld;

    /** _more_ */
    private int colorByIndex = 3;

    /** _more_ */
    UtmInfo utmInfo = new UtmInfo();


    /** _more_ */
    private String header;

    /** _more_ */
    private String fieldName;


    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(PointCloudDataSource.class.getName());


    /**
     *
     * Default constructor
     *
     */
    public PointCloudDataSource() {}


    /**
     * _more_
     *
     * @param descriptor _more_
     * @param filename _more_
     * @param properties _more_
     */
    public PointCloudDataSource(DataSourceDescriptor descriptor,
                                String filename, Hashtable properties) {
        super(descriptor, filename, "Point cloud data source", properties);
        String tmp = (String) getProperty("delimiter");
        if (tmp != null) {
            delimiter = tmp;
        }
    }



    /**
     * Create a PointDataSource
     *
     * @param descriptor    descriptor for the DataSource
     * @param sources _more_
     * @param name _more_
     * @param properties    extra properties
     *
     * @throws VisADException
     *
     */
    public PointCloudDataSource(DataSourceDescriptor descriptor,
                                List sources, String name,
                                Hashtable properties)
            throws VisADException {
        super(descriptor, sources, sources.get(0).toString(), properties);
    }



    /**
     * _more_
     *
     * @param comps _more_
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);
        String filePath = getFilePath();
        File   f        = new File(filePath);
        if ( !f.exists()) {
            if (filePath.indexOf("${skip}") < 0) {
                if (filePath.indexOf("%24%7Bskip%7D") < 0) {
                    return;
                }
            }
        }
        skipFld         = new JTextField("" + skip, 5);
        delimiterFld    = new JTextField(delimiter, 3);
        colorByIndexFld = new JTextField("" + colorByIndex, 3);
        if (header != null) {
            comps.add(GuiUtils.rLabel("Header:"));
            comps.add(GuiUtils.lLabel(header));
        }

        comps.add(GuiUtils.rLabel("Skip:"));
        comps.add(GuiUtils.left(GuiUtils.hbox(skipFld,
                new JLabel("-1 = default"))));
        comps.add(GuiUtils.rLabel("Delimiter:"));
        comps.add(GuiUtils.left(delimiterFld));
        comps.add(GuiUtils.rLabel("Color by index:"));
        comps.add(GuiUtils.left(colorByIndexFld));
        utmInfo.getPropertiesComponents(comps);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        utmInfo.applyProperties();
        if (skipFld == null) {
            return true;
        }
        skip = new Integer(skipFld.getText().trim()).intValue();
        colorByIndex =
            new Integer(colorByIndexFld.getText().trim()).intValue();

        delimiter = delimiterFld.getText().trim();
        if (delimiter.length() == 0) {
            delimiter = " ";
        }

        flushCache();
        return true;
    }

    /** _more_ */
    private static int typeCnt = 0;

    /**
     * _more_
     *
     * @param dataChoice _more_
     * @param category _more_
     * @param dataSelection _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        try {
            float[][] pts       = null;
            String    filePath  = getFilePath();
            File      f         = new File(filePath);
            int       skipToUse = skip;
            if ( !f.exists()) {
                if (skip >= 0) {
                    if ((filePath.indexOf("${skip}") >= 0)
                            || (filePath.indexOf("%24%7Bskip%7D") >= 0)) {
                        filePath = filePath.replace("${skip}", "" + skip);
                        filePath = filePath.replace("%24%7Bskip%7D",
                                "" + skip);
                        skipToUse = 0;
                    }
                }
            }

            BufferedReader reader;
            if (f.exists()) {
                reader = new BufferedReader(new FileReader(f));
            } else {
                InputStream inputStream = IOUtil.getInputStream(filePath,
                                              getClass());
                //                inputStream = new BufferedInputStream(inputStream, 10000);
                reader =
                    new BufferedReader(new InputStreamReader(inputStream));
            }


            int     pointCnt  = 0;
            int     numFields = 3;
            boolean latLon    = true;
            //check the order
            if (Misc.equals(getProperty("latlon"), "false")) {
                latLon = false;
            }

            int  skipCnt = 0;


            long t1      = System.currentTimeMillis();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                //Check for header
                if (pointCnt == 0) {
                    if (line.toLowerCase().indexOf("latitude") >= 0) {
                        header = line;
                        continue;
                    }
                    if (line.toLowerCase().indexOf("x") >= 0) {
                        header = line;
                        continue;
                    }
                }

                if (line.startsWith("#")) {
                    continue;
                }
                List<String> toks = StringUtil.split(line, delimiter, true,
                                        true);
                if (toks.size() < 3) {
                    System.err.println("Bad line:" + line);
                    continue;
                }

                if (skipToUse > 0) {
                    if (skipCnt > 0) {
                        skipCnt--;
                        continue;
                    }
                    skipCnt = skipToUse;
                }

                if (pts == null) {
                    numFields = Math.min(4, toks.size());
                    numFields = 3;
                    pts       = new float[numFields][10000];
                } else if (pointCnt >= pts[0].length) {
                    pts = Misc.expand(pts);
                }

                //            if(pointCnt>50) break;
                float v1  = Float.parseFloat(toks.get(0).toString());
                float v2  = Float.parseFloat(toks.get(1).toString());
                float v3  = Float.parseFloat(toks.get(2).toString());

                float lat = latLon
                            ? v1
                            : v2;
                float lon = latLon
                            ? v2
                            : v1;
                float alt = v3;
                if (utmInfo.getIsUtm() && utmInfo.getIsUtmMeters()) {
                    lon = lon / 1000;
                    lat = lat / 1000;
                }
                pts[INDEX_ALT][pointCnt] = alt;
                pts[INDEX_LON][pointCnt] = lon;
                pts[INDEX_LAT][pointCnt] = lat;
                //            System.err.println(lat +"/" + lon +"/" + alt);

                if (numFields == 4) {
                    pts[3][pointCnt] =
                        Float.parseFloat(toks.get(colorByIndex).toString());
                }

                pointCnt++;
            }
            if (pts == null) {
                throw new BadDataException(
                    "No points were read. Bad delimiter?");
            }
            long t2 = System.currentTimeMillis();
            System.err.println("cnt:" + pointCnt + " time:"
                               + (t2 - t1) / 1000);
            pts = Misc.copy(pts, pointCnt);

            if (utmInfo.getIsUtm()) {
                UtmProjection utm = new UtmProjection(utmInfo.getUtmZone(),
                                        utmInfo.getIsUtmNorth());
                float[]   lats   = pts[INDEX_LAT];
                float[]   lons   = pts[INDEX_LON];
                float[][] result = utm.projToLatLon(new float[][] {
                    lats, lons
                }, new float[][] {
                    lats, lons
                });

                pts[INDEX_LAT] = lats;
                pts[INDEX_LON] = lons;
            }


            if (header != null) {
                List<String> tmpToks = StringUtil.split(header, delimiter,
                                           true, true);
                if ((pts.length > 3) && (tmpToks.size() > 3)) {
                    fieldName = tmpToks.get(3);
                }
            }


            RealType rt = null;
            if (pts.length > 3) {
                rt = Util.makeRealType(((fieldName != null)
                                        ? fieldName
                                        : "field") + "_" + (typeCnt++), null);
            }
            System.err.println("Alt:" + RealType.Altitude.getDefaultUnit());
            MathType type = (pts.length == 3)
                            ? new RealTupleType(RealType.Altitude,
                                RealType.Longitude, RealType.Latitude)
                            : new RealTupleType(RealType.Altitude,
                                RealType.Longitude, RealType.Latitude, rt);

            if (dataChoice.getId().equals("altitudegrid")) {
                return makeGrid(pts, rt);
            }
            return makeField(type, pts);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @param pts _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private FieldImpl makeGrid(float[][] pts, RealType type)
            throws Exception {

        boolean   fillMissing = true;
        int       numCols     = 800;
        int       numRows     = 800;

        float     west        = Float.POSITIVE_INFINITY;
        float     south       = Float.POSITIVE_INFINITY;
        float     east        = Float.NEGATIVE_INFINITY;
        float     north       = Float.NEGATIVE_INFINITY;

        float[][] latLonGrid  = new float[numRows][numCols];
        int[][]   cntGrid     = new int[numRows][numCols];
        for (int x = 0; x < numCols; x++) {
            for (int y = 0; y < numRows; y++) {
                latLonGrid[y][x] = Float.NaN;
                cntGrid[y][x]    = 0;
            }
        }

        for (int i = 0; i < pts[0].length; i++) {
            double lat = pts[INDEX_LAT][i];
            double lon = pts[INDEX_LON][i];
            west  = (float) Math.min(west, lon);
            east  = (float) Math.max(east, lon);
            north = (float) Math.max(north, lat);
            south = (float) Math.min(south, lat);
        }

        double gridWidth  = east - west;
        double gridHeight = north - south;
        for (int i = 0; i < pts[0].length; i++) {
            double altitude = pts[INDEX_ALT][i];
            double lat      = pts[INDEX_LAT][i];
            double lon      = pts[INDEX_LON][i];
            int latIndex = (numRows - 1)
                           - (int) ((numRows - 1) * (lat - south)
                                    / gridHeight);
            int lonIndex = (int) ((numCols - 1) * (lon - west) / gridWidth);
            if (latLonGrid[latIndex][lonIndex]
                    != latLonGrid[latIndex][lonIndex]) {
                latLonGrid[latIndex][lonIndex] = 0;
            }
            latLonGrid[latIndex][lonIndex] += (float) altitude;
            cntGrid[latIndex][lonIndex]++;
        }

        for (int x = 0; x < numCols; x++) {
            for (int y = 0; y < numRows; y++) {
                if (latLonGrid[y][x] == latLonGrid[y][x]) {
                    latLonGrid[y][x] = latLonGrid[y][x] / cntGrid[y][x];
                }
            }
        }

        if (fillMissing) {
            for (int x = 0; x < numCols; x++) {
                for (int y = 0; y < numRows; y++) {
                    if (latLonGrid[y][x] != latLonGrid[y][x]) {
                        int     delta                 = numCols / 100;
                        boolean foundNonMissingNearby = false;
                        for (int dx = -delta; dx < delta; dx++) {
                            for (int dy = -delta; dy < delta; dy++) {
                                int nx = x + dx;
                                int ny = y + dy;
                                if ((nx >= 0) && (nx < latLonGrid[0].length)
                                        && (ny >= 0)
                                        && (ny < latLonGrid.length)) {
                                    if ((latLonGrid[ny][nx]
                                            == latLonGrid[ny][nx]) && (latLonGrid[ny][nx]
                                                != GRID_MISSING)) {
                                        foundNonMissingNearby = true;
                                    }
                                }
                            }
                        }
                        if ( !foundNonMissingNearby) {
                            latLonGrid[y][x] = GRID_MISSING;
                        }
                    }
                }
            }

            for (int pass = 0; pass < 1; pass++) {
                boolean anyMissing = false;
                for (int x = 0; x < numCols; x++) {
                    for (int y = 0; y < numRows; y++) {
                        if (fillMissingFromNeighbors(latLonGrid, x, y)) {
                            anyMissing = true;
                        }
                    }
                }
                if (anyMissing) {
                    for (int y = 0; y < numRows; y++) {
                        for (int x = 0; x < numCols; x++) {
                            if (fillMissingFromNeighbors(latLonGrid, x, y)) {
                                anyMissing = true;
                            }
                        }
                    }
                }
                if (anyMissing) {
                    for (int y = numRows - 1; y >= 0; y--) {
                        for (int x = numCols - 1; x >= 0; x--) {
                            if (fillMissingFromNeighbors(latLonGrid, x, y)) {
                                anyMissing = true;
                            }
                        }
                    }
                }
                if (anyMissing) {
                    for (int x = numCols - 1; x >= 0; x--) {
                        for (int y = numRows - 1; y >= 0; y--) {
                            if (fillMissingFromNeighbors(latLonGrid, x, y)) {
                                anyMissing = true;
                            }
                        }
                    }
                }
                if ( !anyMissing) {
                    break;
                }
            }
        }


        float[][] gridValues = new float[1][numCols * numRows];

        int       m          = 0;
        for (int j = 0; j < numRows; j++) {
            for (int i = 0; i < numCols; i++) {
                float value = (float) latLonGrid[j][i];
                if (value == GRID_MISSING) {
                    value = Float.NaN;
                }
                gridValues[0][m] = value;
                m++;
            }
        }

        type = RealType.Altitude;
        Linear1DSet xSet = new Linear1DSet(RealType.Longitude, west, east,
                                           numCols);
        Linear1DSet ySet = new Linear1DSet(RealType.Latitude, north, south,
                                           numRows);
        System.err.println(xSet);
        System.err.println(ySet);
        GriddedSet gdsSet =
            new LinearLatLonSet(RealTupleType.SpatialEarth2DTuple,
                                new Linear1DSet[] { xSet,
                ySet }, (CoordinateSystem) null, (Unit[]) null,
                        (ErrorEstimate[]) null, true);
        FunctionType ftLatLon2Param =
            new FunctionType(((SetType) gdsSet.getType()).getDomain(),
                             new RealTupleType(type));
        Unit outputUnits = type.getDefaultUnit();
        FlatField retData = new FlatField(ftLatLon2Param, gdsSet,
                                          (CoordinateSystem) null,
                                          (Set[]) null,
                                          new Unit[] { outputUnits });
        retData.setSamples(gridValues, false);
        return retData;

    }


    /**
     * _more_
     *
     * @param grid _more_
     * @param x _more_
     * @param y _more_
     *
     * @return _more_
     */
    private static boolean fillMissingFromNeighbors(float[][] grid, int x,
            int y) {
        if (grid[y][x] == grid[y][x]) {
            return false;
        }
        if (grid[y][x] == GRID_MISSING) {
            return false;
        }
        float sum = 0;
        int   cnt = 0;
        for (int dx = -1; dx < 2; dx++) {
            for (int dy = -1; dy < 2; dy++) {
                int nx = x + dx;
                int ny = y + dy;
                if ((nx >= 0) && (nx < grid[0].length) && (ny >= 0)
                        && (ny < grid.length)) {
                    if ((grid[ny][nx] == grid[ny][nx])
                            && (grid[ny][nx] != GRID_MISSING)) {
                        sum += grid[ny][nx];
                        cnt++;
                    }
                }
            }
        }
        if (cnt > 0) {
            grid[y][x] = sum / cnt;
        }
        return true;
    }



    /**
     * _more_
     *
     * @param rangeType _more_
     * @param pts _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public static FlatField makeField(MathType rangeType, float[][] pts)
            throws VisADException, RemoteException {
        RealType     index  = RealType.getRealType("index");
        Integer1DSet domain = new Integer1DSet(index, pts[0].length);
        FunctionType ft     = new FunctionType(index, rangeType);
        FlatField    field  = new FlatField(ft, domain);
        field.setSamples(pts, false);
        return field;
    }



    /**
     * _more_
     */
    public void doMakeDataChoices() {
        addDataChoice(
            new DirectDataChoice(
                this, "pointcloud", "pointcloud", "Point Cloud Data",
                DataCategory.parseCategories("pointcloud", false),
                new Hashtable()));

        addDataChoice(
            new DirectDataChoice(
                this, "altitudegrid", "altitude", "Altitude Grid",
                DataCategory.parseCategories("GRID-2D;", false),
                new Hashtable()));



    }



    /**
     * Set the UtmInfo property.
     *
     * @param value The new value for UtmInfo
     */
    public void setUtmInfo(UtmInfo value) {
        this.utmInfo = value;
    }

    /**
     * Get the UtmInfo property.
     *
     * @return The UtmInfo
     */
    public UtmInfo getUtmInfo() {
        return this.utmInfo;
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            for (int cnt = 0; cnt < 5; cnt++) {
                //                InputStream inputStream = new FileInputStream(arg);
                //                inputStream = new BufferedInputStream(inputStream, 10000);
                //                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                BufferedReader reader =
                    new BufferedReader(new FileReader(arg));
                long t1 = System.currentTimeMillis();
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    List<String> toks = StringUtil.split(line, " ", true,
                                            true);
                }
                long t2 = System.currentTimeMillis();
                System.err.println("Time:" + (t2 - t1) / 1000);
            }
        }
    }

    /**
     * Set the Delimiter property.
     *
     * @param value The new value for Delimiter
     */
    public void setDelimiter(String value) {
        this.delimiter = value;
    }

    /**
     * Get the Delimiter property.
     *
     * @return The Delimiter
     */
    public String getDelimiter() {
        return this.delimiter;
    }



    /**
     * Set the ColorByIndex property.
     *
     * @param value The new value for ColorByIndex
     */
    public void setColorByIndex(int value) {
        this.colorByIndex = value;
    }

    /**
     * Get the ColorByIndex property.
     *
     * @return The ColorByIndex
     */
    public int getColorByIndex() {
        return this.colorByIndex;
    }

    /**
     *  Set the Skip property.
     *
     *  @param value The new value for Skip
     */
    public void setSkip(int value) {
        this.skip = value;
    }

    /**
     *  Get the Skip property.
     *
     *  @return The Skip
     */
    public int getSkip() {
        return this.skip;
    }


    /**
     *  Set the Header property.
     *
     *  @param value The new value for Header
     */
    public void setHeader(String value) {
        this.header = value;
    }

    /**
     *  Get the Header property.
     *
     *  @return The Header
     */
    public String getHeader() {
        return this.header;
    }




}
