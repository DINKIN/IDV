/*
 * $Id: StationModelDisplayable.java,v 1.99 2007/07/06 17:56:06 jeffmc Exp $
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


package ucar.visad.display;


import org.python.core.*;

import org.python.util.*;

import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.point.PointOb;

import ucar.unidata.idv.JythonManager;
import ucar.unidata.ui.drawing.Glyph;

import ucar.unidata.ui.symbol.*;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.WrapperException;

import ucar.visad.ShapeUtility;
import ucar.visad.Util;
import ucar.visad.WindBarb;



import visad.*;


import visad.data.units.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.NamedLocationTuple;

import visad.meteorology.WeatherSymbols;

import java.awt.Color;

import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;



/**
 * Class for displaying a station model plot
 *
 * @author IDV Development Team
 * @version $Revision: 1.99 $
 */
public class StationModelDisplayable extends DisplayableData {


    /** Special index value */
    private static int INDEX_LAT = -1000;

    /** Special index value */
    private static int INDEX_LON = -1001;

    /** Special index value */
    private static int INDEX_ALT = -1002;

    /** Special index value */
    private static int INDEX_TIME = -1003;

    /** Scale for offset */
    public static final float OFFSET_SCALE = 20.f;

    /** Mapping of param name to the index */
    private Hashtable nameToIndex;

    /** Should we use altitude */
    private boolean shouldUseAltitude = true;

    /** Keep around for makeShapes */
    private Point2D workPoint = new Point2D.Double();

    /** Keep around for makeShapes */
    private Rectangle2D workRect = new Rectangle2D.Float();

    /** Work object */
    private Rectangle2D workShapeBounds = new Rectangle2D.Float();

    /** Work object */
    private float[] workOffsetArray = { 0.0f, 0.0f, 0.0f };

    /** Work object */
    private Data[] workDataArray = { null, null };

    /** Work object */
    private float[] workUV = { 0.0f, 0.0f };

    /** Work object */
    float[][] workFlowValues = {
        { 0.0f }, { 0.0f }, { 0.0f }
    };

    /** Work object */
    float[][] workSpatialValues = {
        { 0.0f }, { 0.0f }, { 0.0f }
    };

    /** range select variable */
    private boolean[][] workRangeSelect = {
        { true }, { true }, { true }
    };


    /** Mapping between comma separated param names and the parsed list */
    private Hashtable namesToList = new Hashtable();

    /** Keeps trakc of when we have  printed out a missing param message */
    private Hashtable haveNotified = null;

    /** Should we try to merge the shapes. This gets set to false if we have an error in merging */
    private boolean tryMerge = true;

    /** Hashtable for jython codes to operands */
    private Hashtable codeToOperands = new Hashtable();

    /** code for conversion */
    private String convertCode = null;

    /** code for formatting */
    private String fmtCode = null;

    /** Our interperter */
    PythonInterpreter interp;

    /** A cache of shapes */
    private Hashtable shapeCache = new Hashtable();


    /**
     * The {@link ucar.unidata.idv.JythonManager} to use for accessing
     * jython  interpreters
     */
    private JythonManager jythonManager;

    /** ScalarMap for the weather symbol shapes */
    ScalarMap wxMap = null;

    /** RealType used for the weather symbols */
    RealType wxType = null;

    /** ShapeControl for the weather symbol shapes */
    ShapeControl shapeControl = null;

    /** ScalarMap for the time selection */
    ScalarMap timeSelectMap = null;

    /** RealType used for the time selection */
    RealType timeSelectType = null;

    /** Control for select range */
    private RangeControl timeSelectControl;

    /** low range for select */
    private double lowSelectedRange = Double.NaN;  // low range for scalarmap

    /** high range for select */
    private double highSelectedRange = Double.NaN;  // high range for scalarmap

    /** low range for select map */
    private double minSelect = Double.NaN;  // low range for scalarmap

    /** high range for select map */
    private double maxSelect = Double.NaN;  // high range for scalarmap

    /** The weather symbol shapes */
    VisADGeometryArray[] shapes = null;


    /** static count for incrementing the RealTypes */
    private static int count = 0;

    /** StationModel for laying out the shapes */
    private StationModel stationModel;

    /** The data to use to make the shapes */
    private FieldImpl stationData = null;

    /** index for the shape */
    private int shapeIndex;

    /** Vector of shapes */
    private List shapeList;

    /** scaling factor */
    private float scale = 1.0f;

    /** This is the scale factor to unsquash squashed aspect ratios from the display */
    private double[] displayScaleFactor;

    /** variable indices */
    private int[] varIndices;

    /** instance */
    private static int instance = 0;

    /** mutex for locking */
    private static Object INSTANCE_MUTEX = new Object();

    /** mutex for locking when creating shapes */
    private static Object SHAPES_MUTEX = new Object();

    /** flag for a time sequence */
    private boolean isTimeSequence = false;

    /**
     * Default constructor;
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable() throws VisADException, RemoteException {
        this("Station Model");
    }


    /**
     * Construct a StationModelDisplayable with the specified name
     * @param name  name of displayable and StationModel
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable(String name)
            throws VisADException, RemoteException {
        this(new StationModel(name));
    }


    /**
     * Construct a StationModelDisplayable with the specified name
     * @param name  name of displayable and StationModel
     * @param jythonManager The JythonManager for evaluating embedded expressions
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable(String name, JythonManager jythonManager)
            throws VisADException, RemoteException {
        this(name, new StationModel(name), jythonManager);
    }



    /**
     * Construct a StationModelDisplayable using the specified model.
     * @param model  StationModel to use for data depiction
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable(StationModel model)
            throws VisADException, RemoteException {
        this(model, null);
    }


    /**
     * Construct a StationModelDisplayable using the specified model.
     * @param model  StationModel to use for data depiction
     * @param jythonManager The JythonManager for evaluating embedded expressions
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable(StationModel model,
                                   JythonManager jythonManager)
            throws VisADException, RemoteException {
        this(model.getName(), model, jythonManager);
    }


    /**
     * Construct a StationModelDisplayable using the specified model and
     * name.
     * @param name  name of for this displayable data reference
     * @param stationModel  StationModel to use for data depiction
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable(String name, StationModel stationModel)
            throws VisADException, RemoteException {
        this(name, stationModel, null);
    }


    /**
     * Construct a StationModelDisplayable using the specified model and
     * name.
     * @param name  name of for this displayable data reference
     * @param stationModel  StationModel to use for data depiction
     * @param jythonManager The JythonManager for evaluating embedded expressions
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public StationModelDisplayable(String name, StationModel stationModel,
                                   JythonManager jythonManager)
            throws VisADException, RemoteException {
        super(name);
        this.jythonManager = jythonManager;
        this.stationModel  = stationModel;
        setUpScalarMaps();
    }

    /**
     * Clone constructor to create another instance.
     * @param that  object to clone from.
     *
     * @throws RemoteException  a remote error
     * @throws VisADException   a VisAD error
     */
    protected StationModelDisplayable(StationModelDisplayable that)
            throws VisADException, RemoteException {
        super(that);
        this.stationModel = that.stationModel;
    }



    /**
     * Set the station data to display using the StationModel.
     * @param  stationData Field of station observations.
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    public void setStationData(FieldImpl stationData)
            throws VisADException, RemoteException {

        setDisplayInactive();
        Data d = makeNewDataWithShapes(stationData);
        if ((d != null) && !d.isMissing()) {
            setData(d);
        } else {
            setData(new Real(0));
        }
        this.stationData = stationData;  // hold around for posterity
        setDisplayActive();
    }



    /**
     * Set up the ScalarMaps for this Displayable
     *
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    private void setUpScalarMaps() throws VisADException, RemoteException {
        int myInstance;
        synchronized (INSTANCE_MUTEX) {
            myInstance = instance++;
        }
        wxType = RealType.getRealType("Station_Model_" + myInstance);
        wxMap  = new ScalarMap(wxType, Display.Shape);
        wxMap.addScalarMapListener(new ScalarMapListener() {
            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {
                int id = event.getId();
                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    shapeControl = (ShapeControl) wxMap.getControl();
                    if (shapeControl != null) {
                        setShapesInControl(shapes);
                        shapeControl.setAutoScale(false);
                        shapeControl.setScale(scale);
                        shapeControl.setAutoScale(true);
                    }
                }
            }

            public void mapChanged(ScalarMapEvent event)
                    throws RemoteException, VisADException {}
        });
        addScalarMap(wxMap);
        timeSelectType = RealType.getRealType("Station_Model_Time"
                + myInstance, CommonUnit.secondsSinceTheEpoch);
        timeSelectMap = new ScalarMap(timeSelectType, Display.SelectRange);
        timeSelectMap.addScalarMapListener(new ScalarMapListener() {
            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {
                int id = event.getId();
                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    timeSelectControl =
                        (RangeControl) timeSelectMap.getControl();
                    if (hasSelectedRange() && (timeSelectControl != null)) {
                        timeSelectControl.setRange(new double[] {
                            lowSelectedRange,
                            highSelectedRange });
                    }
                }
            }

            public void mapChanged(ScalarMapEvent event)
                    throws RemoteException, VisADException {
                if ((event.getId() == event.AUTO_SCALE)
                        && hasSelectMinMax()) {
                    timeSelectMap.setRange(minSelect, maxSelect);
                }
            }
        });
        addScalarMap(timeSelectMap);
    }


    /**
     * set the shapes in the control
     *
     * @param shapes  shapes to use
     * n
     * @throws VisADException unable to create specified VisAD objects
     * @throws RemoteException unable to create specified remote objects
     */
    private void setShapesInControl(VisADGeometryArray[] shapes)
            throws VisADException, RemoteException {
        if (shapeControl != null) {
            if ((shapes == null) || (shapes.length == 0)) {
                shapes = ShapeUtility.createShape(ShapeUtility.NONE);
            }
            shapeControl.setShapeSet(new Integer1DSet(shapes.length));
            shapeControl.setShapes(shapes);
        }

    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()  // revise
            throws RemoteException, VisADException {
        return new StationModelDisplayable(this);
    }

    /**
     * Set the station ob model for this object
     * @param model StationModel to use
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setStationModel(StationModel model)
            throws VisADException, RemoteException {
        setStationModel(model, true);
    }

    /**
     * Set the station ob model for this object
     * @param model StationModel to use
     * @param update update data using new model
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setStationModel(StationModel model, boolean update)
            throws VisADException, RemoteException {
        tryMerge     = true;
        stationModel = model;
        if ((stationData != null) && update) {
            makeNewDataWithShapes(stationData);
        }
    }

    /**
     * Get the station model used by this displayable.
     * @return the station model
     */
    public StationModel getStationModel() {
        return stationModel;
    }



    /**
     * make the shapes and set the data
     *
     * @param data The data  to create shapes with
     * @return The new field
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private FieldImpl makeNewDataWithShapes(FieldImpl data)
            throws VisADException, RemoteException {

        if (data == null) {
            return null;
        }

        DisplayMaster master = getDisplayMaster();
        displayScaleFactor = null;
        if (master != null) {
            double[] aspect = master.getDisplayAspect();
            if ((aspect != null) && (aspect.length > 2)) {
                displayScaleFactor = new double[] { 1.0 / aspect[0],
                        1.0 / aspect[1], 1.0 / aspect[2] };
            }
        }

        FieldImpl newFI = null;
        synchronized (SHAPES_MUTEX) {
            haveNotified = null;
            isTimeSequence =
                ucar.unidata.data.grid.GridUtil.isTimeSequence(data);
            int numTimes = 0;
            shapeIndex  = 0;
            shapeList   = new Vector();
            nameToIndex = new Hashtable();
            try {
                if (isTimeSequence) {
                    boolean haveChecked = false;
                    Set     timeSet     = data.getDomainSet();
                    for (int i = 0; i < timeSet.getLength(); i++) {
                        FieldImpl shapeFI = makeShapesFromPointObsField(
                                                (FieldImpl) data.getSample(
                                                    i));
                        if (shapeFI == null) {
                            continue;
                        }
                        if (newFI == null) {  // first time through
                            FunctionType functionType =
                                new FunctionType(((FunctionType) data
                                    .getType()).getDomain(), shapeFI
                                        .getType());
                            newFI = new FieldImpl(functionType, timeSet);
                        }
                        newFI.setSample(i, shapeFI, false, !haveChecked);
                        if ( !haveChecked) {
                            haveChecked = true;
                        }
                    }  // end isSequence
                } else {
                    newFI = makeShapesFromPointObsField(data);
                }      // end single time 
            } catch (Exception exc) {
                logException("making shapes", exc);
                return null;
            }

            try {
                shapes = new VisADGeometryArray[shapeList.size()];
                shapes = (VisADGeometryArray[]) shapeList.toArray(shapes);
                setShapesInControl(shapes);
            } catch (Exception t) {
                throw new VisADException(
                    "Unable to covert vector to VisADGeometry array");
            }
        }
        return newFI;
    }



    /**
     * create shapes for an individual time step.
     *
     * @param data
     * @return
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private FieldImpl makeShapesFromPointObsField(FieldImpl data)
            throws VisADException, RemoteException {

        Set  set = data.getDomainSet();
        Data tmp = data.getSample(0);
        if (tmp.isMissing()) {
           return null;
        }

        PointOb       firstOb   = (PointOb) data.getSample(0);
        FunctionType  fieldType = (FunctionType) data.getType();
        RealTupleType domain    = fieldType.getDomain();
        MathType      llType;
        boolean       useAltitude = getShouldUseAltitude();
        if (useAltitude) {
            llType = firstOb.getEarthLocation().getType();
        } else {
            llType = RealTupleType.LatitudeLongitudeTuple;
        }
        //      System.err.println(" usealt:" + useAltitude +" llType:" + llType);
        TupleType tt = new TupleType(new MathType[] { wxType, llType,
                timeSelectType });
        FunctionType retType          = new FunctionType(domain, tt);
        List         dataList         = new Vector();

        List         symbols          = new ArrayList();
        List         pointOnSymbols   = new ArrayList();
        List         offsetFlipPoints = new ArrayList();
        for (Iterator iter = stationModel.iterator(); iter.hasNext(); ) {
            MetSymbol metSymbol = (MetSymbol) iter.next();
            if ( !metSymbol.getActive()) {
                continue;
            }
            symbols.add(metSymbol);
            Rectangle symbolBounds = metSymbol.getBounds();
            String    symbolPoint  = metSymbol.getRectPoint();
            Point2D pointOnSymbol = Glyph.getPointOnRect(symbolPoint,
                                        symbolBounds);
            pointOnSymbol.setLocation(pointOnSymbol.getX() / OFFSET_SCALE,
                                      pointOnSymbol.getY() / OFFSET_SCALE);

            pointOnSymbols.add(pointOnSymbol);
            offsetFlipPoints.add(Glyph.flipY(symbolPoint));
        }
        int  total = 0;
        long t1    = System.currentTimeMillis();
        Trace.call1("SMD.makeShapes loop",
                    " #obs:" + set.getLength() + " #vars:"
                    + ((Tuple) firstOb.getData()).getLength());
        String[]  typeNames     = null;
        int       length        = set.getLength();
        TupleType dataTupleType = null;
        for (int obIdx = 0; obIdx < length; obIdx++) {
            PointOb ob = (PointOb) data.getSample(obIdx);
            if (typeNames == null) {
                Tuple     obData = (Tuple) ob.getData();
                TupleType tType  = (TupleType) obData.getType();
                typeNames = getTypeNames(tType);
            }

            List obShapes = makeShapes(ob, typeNames, symbols,
                                       pointOnSymbols, offsetFlipPoints);
            if (obShapes == null) {
                continue;
            }
            for (int j = 0; j < obShapes.size(); j++) {
                Data     location = (useAltitude
                                     ? ob.getEarthLocation()
                                     : ob.getEarthLocation()
                                         .getLatLonPoint());
                DateTime obTime   = ob.getDateTime();
                Real time = new Real(
                                timeSelectType,
                                obTime.getValue(
                                    timeSelectType.getDefaultUnit()));
                Data[] dataArray = new Data[] {
                                       new Real(wxType, shapeIndex++),
                                       location, time };
                if (dataTupleType == null) {
                    dataTupleType = Tuple.buildTupleType(dataArray);
                }
                Tuple t = new Tuple(dataTupleType, dataArray, false, false);
                dataList.add(t);
                shapeList.add(obShapes.get(j));
            }
        }
        Trace.call2("SMD.makeShapes loop");

        long t2 = System.currentTimeMillis();
        //      System.err.println ("ob shape time:" +(t2-t1));

        FieldImpl fi = null;
        if ( !dataList.isEmpty()) {
            Data[] dArray = new Data[dataList.size()];
            try {
                dArray = (Data[]) dataList.toArray(dArray);
            } catch (Exception t) {
                throw new VisADException(
                    "Unable to convert vector to data array");
            }
            Integer1DSet index = new Integer1DSet(domain, dArray.length);
            //      System.out.println ("# vertices:" + total + "\nindex:" + index);
            fi = new FieldImpl(retType, index);
            fi.setSamples(dArray, false, false);
        } else {  // return a missing object
            fi = new FieldImpl(retType, new Integer1DSet(domain, 1));
            fi.setSample(0, new Tuple(tt), false);
        }

        return fi;
    }



    /**
     * Make shape from an observation based on the StationModel
     *
     * @param ob  a single point ob to turn into a shape
     * @param typeNames list of type names
     * @param symbols List of the MetSymbols to use
     * @param pointOnSymbols List of the rectangle point on the symbols
     * @param offsetFlipPoints List of the flipper points
     * @return  corresponding shape
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private List makeShapes(PointOb ob, String[] typeNames, List symbols,
                            List pointOnSymbols, List offsetFlipPoints)
            throws VisADException, RemoteException {

        List      lineShapes     = null;
        List      triangleShapes = null;
        List      quadShapes     = null;
        Tuple     data           = (Tuple) ob.getData();
        TupleType tType          = (TupleType) data.getType();

        MetSymbol metSymbol      = null;
        workDataArray[0] = null;
        workDataArray[1] = null;
        //The workDataArray should never be more than size 2
        try {
            for (int symbolIdx = 0; symbolIdx < symbols.size(); symbolIdx++) {
                metSymbol = (MetSymbol) symbols.get(symbolIdx);
                Point2D pointOnSymbol =
                    (Point2D) pointOnSymbols.get(symbolIdx);
                float    shapeScaleFactor = .05f;
                String[] paramIds         = metSymbol.getParamIds();
                boolean  ok               = true;
                if ( !(metSymbol instanceof LabelSymbol)
                        && !metSymbol.doAllObs()) {
                    int max = Math.min(workDataArray.length, paramIds.length);
                    for (int paramIdx = 0; paramIdx < max; paramIdx++) {
                        String paramId = paramIds[paramIdx];
                        if (paramId.startsWith("=")) {
                            //Is a jython formula
                            workDataArray[paramIdx] = evaluateCode(ob,
                                    paramId.substring(1), tType, typeNames,
                                    data, metSymbol,
                                    metSymbol.getDisplayUnit());
                        } else if (paramId.startsWith("value:")) {
                            String tok = paramId.substring(6);
                            workDataArray[paramIdx] = Util.toReal(tok);
                        } else {
                            workDataArray[paramIdx] = getComponent(ob, data,
                                    tType, typeNames, paramId);
                        }
                        if (workDataArray[paramIdx] == null) {
                            ok = false;
                            break;
                        }
                    }
                }
                if ( !ok) {
                    continue;
                }

                Rectangle            symbolBounds = metSymbol.getBounds();
                VisADGeometryArray[] shapes       = null;
                VisADGeometryArray   shape        = null;


                if (metSymbol.doAllObs()) {
                    shapes = metSymbol.makeShapes(ob);
                } else if (metSymbol instanceof TextSymbol) {
                    TextSymbol textSymbol  = (TextSymbol) metSymbol;
                    Font       font        = textSymbol.getFont();
                    String     stringValue = null;
                    Scalar     scalar      = (Scalar) workDataArray[0];
                    //Perhaps cache on value,font,number format and display unit
                    //              Object theKey = new Object[]{font,scalar};
                    //shape = (VisADGeometryArray) shapeCache.get(key);
                    if ((scalar != null) && (scalar instanceof Real)
                            && (metSymbol instanceof ValueSymbol)) {
                        ValueSymbol vs = (ValueSymbol) metSymbol;
                        try {
                            double value = ((vs.getDisplayUnit() == null)
                                            || ((Real) scalar).getUnit()
                                               == null)
                                           ? ((Real) scalar).getValue()
                                           : ((Real) scalar).getValue(
                                               vs.getDisplayUnit());
                            stringValue = Double.isNaN(value)
                                          ? null
                                          : vs.formatNumber(value);
                        } catch (Exception ue) {
                            throw new WrapperException("Incompatible units "
                                    + ((Real) scalar).getUnit() + " & "
                                    + vs.getDisplayUnit(), ue);
                        }
                    } else if (metSymbol instanceof LabelSymbol) {
                        stringValue = ((LabelSymbol) metSymbol).getText();
                    } else if (metSymbol instanceof TextSymbol) {
                        stringValue = (scalar instanceof Text)
                                      ? ((Text) scalar).getValue()
                                      : ((TextSymbol) metSymbol).formatNumber(
                                          ((Real) scalar).getValue());
                    }
                    if (stringValue != null) {
                        if (font != null) {
                            shapeScaleFactor = 0.1f;
                        }
                        int    fontSize = textSymbol.getFontSize();
                        Object key = font + "_" + fontSize + "_"
                                     + stringValue;
                        shape = (VisADGeometryArray) shapeCache.get(key);
                        if (shape == null) {
                            shape = (font == null)
                                    ? ShapeUtility.shapeText(stringValue,
                                    fontSize, true)
                                    : ShapeUtility.shapeFont(stringValue,
                                    font, true);
                            if (shape != null) {
                                shapeCache.put(key, shape.clone());
                            }
                        } else {
                            shape = (VisADGeometryArray) shape.clone();
                        }
                    }
                } else if (metSymbol instanceof WeatherSymbol) {
                    double value;
                    if(workDataArray[0] instanceof Text) {
                        value = new Double(workDataArray[0].toString()).doubleValue();
                        //                        ((Real) workDataArray[0]).getValue();
                    } else {
                        value = ((Real) workDataArray[0]).getValue();
                    }
                    if ( !Double.isNaN(value)) {
                        shape =
                            ((WeatherSymbol) metSymbol).getLines((int) value);
                    }
                } else if (metSymbol instanceof WindBarbSymbol) {
                    boolean isNorth =
                        (ob.getEarthLocation().getLatitude().getValue(
                            CommonUnit.degree) < 0.0);
                    Object key = "wind_" + pointOnSymbol + "_" + isNorth
                                 + "_" + workDataArray[0] + "_"
                                 + workDataArray[1];
                    shapes = (VisADGeometryArray[]) shapeCache.get(key);
                    if (shapes != null) {
                        shapes = ShapeUtility.clone(shapes);
                    } else {
                        float speed;
                        float direction;
                        try {
                            speed =
                                (float) ((Real) workDataArray[0]).getValue(
                                    CommonUnit.meterPerSecond);
                        } catch (Exception e) {
                            speed =
                                (float) ((Real) workDataArray[0]).getValue();
                        }
                        // could be speed or v component.  default to speed
                        Real vOrSpeed = (Real) workDataArray[1];
                        if (Unit.canConvert(vOrSpeed.getUnit(),
                                            CommonUnit.meterPerSecond)) {
                            workUV[0] = speed;
                            workUV[1] = (float) vOrSpeed.getValue(
                                CommonUnit.meterPerSecond);
                        } else {
                            try {
                                direction = (float) vOrSpeed.getValue(
                                    CommonUnit.degree);
                            } catch (Exception e) {
                                direction = (float) vOrSpeed.getValue();
                            }
                            windVector(speed, direction, workUV);
                        }
                        workFlowValues[0][0] = workUV[0];
                        workFlowValues[1][0] = workUV[1];
                        workSpatialValues[0][0] =
                            (float) (pointOnSymbol.getX());
                        workSpatialValues[1][0] =
                            (float) (-pointOnSymbol.getY());
                        try {
                            shapes = WindBarb.staticMakeFlow(workFlowValues,
                                    2.5f, workSpatialValues, (byte[][]) null,  //color_values, 
                                    workRangeSelect, isNorth);
                            shapeCache.put(key, ShapeUtility.clone(shapes));

                        } catch (Exception excp) {
                            System.out.println("speed = " + speed);
                            System.out.println("dir = " + speed);
                            Misc.printArray("workUV", workUV);
                        }  // bad winds
                    }

                } else {
                    //Default is to ask the symbol to make the shapes
                    shapes = metSymbol.makeShapes(workDataArray, ob);
                }

                if (shape != null) {
                    shapes = new VisADGeometryArray[] { shape };
                } else if (shapes == null) {
                    continue;
                }

                String scaleParam = metSymbol.getScaleParam();
                if (scaleParam != null) {
                    Data scaleData = getComponent(ob, data, tType, typeNames,
                                         scaleParam);
                    if (scaleData != null) {
                        boolean valueOk = true;
                        double  value   = 0;
                        if (scaleData instanceof Real) {
                            Unit scaleUnit = metSymbol.getScaleUnit();
                            value = ((scaleUnit != null)
                                     ? ((Real) scaleData).getValue(scaleUnit)
                                     : ((Real) scaleData).getValue());
                        } else {
                            try {
                                value =
                                    Double.parseDouble(scaleData.toString());
                            } catch (NumberFormatException nfe) {
                                valueOk = false;
                            }
                        }
                        if (valueOk) {
                            Range  dataRange  = metSymbol.getScaleDataRange();
                            Range  scaleRange = metSymbol.getScaleRange();
                            double percent    = dataRange.getPercent(value);
                            if (percent < 0.0) {
                                percent = 0.0;
                            } else if (percent > 1.0) {
                                percent = 1.0;
                            }
                            shapeScaleFactor *=
                                scaleRange.getValueOfPercent(percent);
                        }
                    }
                }
                shapeScaleFactor *= metSymbol.getScale();

                Rectangle2D shapeBounds = null;
                for (int shapeIndex = 0; shapeIndex < shapes.length;
                        shapeIndex++) {
                    if (shapes[shapeIndex] == null) {
                        continue;
                    }
                    for (int i = 0; i < RotateInfo.TYPES.length; i++) {
                        RotateInfo info =
                            metSymbol.getRotateInfo(RotateInfo.TYPES[i]);
                        String rotateParam = info.getParam();
                        if (rotateParam == null) {
                            continue;
                        }
                        double angle = 0.0;
                        if (rotateParam.startsWith("angle:")) {
                            angle = Math.toRadians(
                                Double.parseDouble(rotateParam.substring(6)));
                        } else {
                            Data rotateData = getComponent(ob, data, tType,
                                                  typeNames, rotateParam);
                            if ((rotateData == null)
                                    || !(rotateData instanceof Real)) {
                                continue;
                            }
                            Unit   rotateUnit = info.getUnit();
                            double value      = ((rotateUnit != null)
                                    ? ((Real) rotateData).getValue(rotateUnit)
                                    : ((Real) rotateData).getValue());
                            Range rotateRange     = info.getRange();
                            Range rotateDataRange = info.getDataRange();
                            double percent =
                                Math.min(
                                    1.0,
                                    Math.max(
                                        0.0,
                                        rotateDataRange.getPercent(value)));
                            angle = -Math.toRadians(
                                rotateRange.getValueOfPercent(percent));

                        }
                        if (RotateInfo.TYPES[i] == RotateInfo.TYPE_Z) {
                            ShapeUtility.rotateZ(shapes[shapeIndex],
                                    (float) angle);
                        } else if (RotateInfo.TYPES[i] == RotateInfo.TYPE_X) {
                            ShapeUtility.rotateX(shapes[shapeIndex],
                                    (float) angle);
                        } else {
                            ShapeUtility.rotateY(shapes[shapeIndex],
                                    (float) angle);
                        }
                    }



                    shapeBounds = ShapeUtility.bounds2d(shapes[shapeIndex],
                            workRect);
                    double tmpScale = shapeScaleFactor;
                    if (metSymbol.shouldScaleShape()) {
                        float size = (metSymbol instanceof TextSymbol)
                                     ? ((TextSymbol) metSymbol).getFontSize()
                                       / 12.f
                                     : Math.min((float) ((symbolBounds.width
                                         / OFFSET_SCALE) / shapeBounds
                                             .getWidth()), (float) ((symbolBounds
                                                 .height / OFFSET_SCALE) / shapeBounds
                                                     .getHeight()));
                        tmpScale *= size;
                    }

                    if (displayScaleFactor != null) {
                        ShapeUtility.reScale(shapes[shapeIndex],
                                             displayScaleFactor,
                                             shapeScaleFactor);
                    } else {
                        ShapeUtility.reScale(shapes[shapeIndex],
                                             shapeScaleFactor);
                    }
                    if (metSymbol.shouldScaleShape()) {
                        shapeBounds =
                            ShapeUtility.bounds2d(shapes[shapeIndex],
                                shapeBounds);
                    }
                    if (shapeIndex == 0) {
                        workShapeBounds.setRect(shapeBounds);
                    } else {
                        workShapeBounds =
                            shapeBounds.createUnion(workShapeBounds);
                    }
                }

                shapeBounds = workShapeBounds;
                for (int s = 0; s < shapes.length; s++) {
                    if (shapes[s] == null) {
                        continue;
                    }
                    if (metSymbol.shouldOffsetShape()) {
                        Point2D fromPoint = Glyph.getPointOnRect(
                                                (String) offsetFlipPoints.get(
                                                    symbolIdx), shapeBounds,
                                                        workPoint);
                        workOffsetArray[0] = (float) (pointOnSymbol.getX()
                                * shapeScaleFactor - fromPoint.getX());
                        workOffsetArray[1] = (float) (-pointOnSymbol.getY()
                                * shapeScaleFactor - fromPoint.getY());
                        ShapeUtility.offset(shapes[s], workOffsetArray);
                    }

                    //Bump it a bit up
                    ShapeUtility.offset(shapes[s], 0.0f, 0.0f, 0.002f);

                    ColorTable ct         = metSymbol.getColorTable();
                    String     colorParam = metSymbol.getColorParam();
                    String     ctParam    = metSymbol.getColorTableParam();
                    //                    System.err.println("colorParam:" + colorParam + ": ctParam:" + ctParam+":");
                    if ((colorParam != null) && (colorParam.length() > 0)) {
                        if (metSymbol.shouldBeColored()) {
                            Data colorData = getComponent(ob, data, tType,
                                                 typeNames, colorParam);
                            if (colorData != null) {
                                List   mappings =
                                    metSymbol.getColorMappings();
                                Color  theColor    =
                                    metSymbol.getForeground();
                                String colorString = colorData.toString();
                                if ((mappings != null)
                                        && (mappings.size() > 0)) {
                                    for (int i = 0; i < mappings.size();
                                            i++) {
                                        ColorMap colorMap =
                                            (ColorMap) mappings.get(i);
                                        if (colorMap.match(colorString)) {
                                            Color color = colorMap.getColor();
                                            if (color == null) {
                                                continue;
                                            }
                                            theColor = color;
                                            break;
                                        }
                                    }
                                } else {
                                    theColor =
                                        ucar.unidata.util.GuiUtils
                                            .decodeColor(colorString, null);
                                }
                                if (theColor != null) {
                                    ShapeUtility.setColor(shapes[s],
                                            theColor);
                                }
                            }
                        }
                    } else if ((ct == null) || (ctParam == null)
                               || (ctParam.length() == 0)) {
                        if (metSymbol.shouldBeColored()) {
                            Color theColor = metSymbol.getForeground();
                            if (theColor != null) {
                                ShapeUtility.setColor(shapes[s], theColor);
                            }
                        }
                    } else {
                        Data ctData = getComponent(ob, data, tType,
                                          typeNames, ctParam);
                        if (ctData != null) {
                            Unit    ctUnit  = metSymbol.getColorTableUnit();
                            double  value   = 0.0;
                            boolean valueOk = true;
                            if (ctData instanceof Real) {
                                value = ((ctUnit != null)
                                         ? ((Real) ctData).getValue(ctUnit)
                                         : ((Real) ctData).getValue());
                            } else {
                                try {
                                    value =
                                        Double.parseDouble(ctData.toString());
                                } catch (Exception exc) {
                                    valueOk = false;
                                    System.err.println(ctData.toString());
                                }
                            }
                            if (valueOk) {
                                Range  r = metSymbol.getColorTableRange();
                                double percent = r.getPercent(value);
                                if (percent < 0.0) {
                                    percent = 0.0;
                                } else if (percent > 1.0) {
                                    percent = 1.0;
                                }
                                List colors = ct.getColorList();
                                int index = (int) (colors.size() * percent)
                                            - 1;
                                if (index < 0) {
                                    index = 0;
                                }
                                ShapeUtility.setColor(shapes[s],
                                        (Color) colors.get(index));
                            }

                        }
                    }


                    if (metSymbol.getBackground() != null) {
                        Rectangle2D tmp = ShapeUtility.bounds2d(shapes[s],
                                              workRect);
                        Rectangle2D.Float bgb =
                            new Rectangle2D.Float((float) tmp.getX(),
                                (float) tmp.getY(), (float) tmp.getWidth(),
                                (float) tmp.getHeight());


                        bgb.x      = bgb.x - bgb.width * 0.05f;
                        bgb.y      = bgb.y - bgb.height * 0.05f;
                        bgb.width  += bgb.width * 0.1f;
                        bgb.height += bgb.height * 0.1f;

                        VisADQuadArray bgshape = new VisADQuadArray();
                        bgshape.coordinates = new float[] {
                            bgb.x, bgb.y, 0.0f, bgb.x, bgb.y + bgb.height,
                            0.0f, bgb.x + bgb.width, bgb.y + bgb.height, 0.0f,
                            bgb.x + bgb.width, bgb.y, 0.0f
                        };
                        bgshape.vertexCount = bgshape.coordinates.length / 3;
                        bgshape.normals     = new float[12];
                        for (int i = 0; i < 12; i += 3) {
                            bgshape.normals[i]     = 0.0f;
                            bgshape.normals[i + 1] = 0.0f;
                            bgshape.normals[i + 2] = 1.0f;
                        }
                        //Bump it a bit up
                        ShapeUtility.offset(bgshape, 0.0f, 0.0f, 0.001f);

                        ShapeUtility.setColor(bgshape,
                                metSymbol.getBackground());
                        quadShapes = add(quadShapes, bgshape);
                    }

                    if (shapes[s] instanceof VisADLineArray) {
                        lineShapes = add(lineShapes, shapes[s]);
                    } else if (shapes[s] instanceof VisADQuadArray) {
                        quadShapes = add(quadShapes, shapes[s]);
                    } else {
                        triangleShapes = add(triangleShapes, shapes[s]);
                    }
                }
            }
        } catch (Exception e) {
            throw new WrapperException("Error generating symbol: "
                                       + ((metSymbol != null)
                                          ? metSymbol.getName()
                                          : ""), e);
        }

        List allShapes = new ArrayList();
        //Try to merge them. But, if any of the  Visad arrays have different
        //state (normals,colors, etc.) there will be an error. We track that with the 
        //tryMerge flag.
        if (lineShapes != null) {
            if ((lineShapes.size() > 1) && tryMerge) {
                try {
                    VisADLineArray[] la =
                        new VisADLineArray[lineShapes.size()];
                    la = (VisADLineArray[]) lineShapes.toArray(la);
                    allShapes.add(VisADLineArray.merge(la));
                } catch (visad.DisplayException exc) {
                    tryMerge = false;
                    allShapes.addAll(lineShapes);
                }
            } else {
                allShapes.addAll(lineShapes);
            }
        }
        if (triangleShapes != null) {
            if ((triangleShapes.size() > 1) && tryMerge) {
                try {
                    VisADTriangleArray[] ta =
                        new VisADTriangleArray[triangleShapes.size()];
                    ta = (VisADTriangleArray[]) triangleShapes.toArray(ta);
                    allShapes.add(VisADTriangleArray.merge(ta));
                } catch (visad.DisplayException exc) {
                    tryMerge = false;
                    allShapes.addAll(triangleShapes);
                }
            } else {
                allShapes.addAll(triangleShapes);
            }
        }
        if (quadShapes != null) {
            allShapes.addAll(quadShapes);
        }
        return allShapes;
    }



    /**
     * Utility to add to the given list. If null then create it
     *
     * @param l The list
     * @param o The object to add
     *
     * @return The given list if non-null, else the newly created list.
     */
    private List add(List l, Object o) {
        if (l == null) {
            l = new ArrayList();
        }
        l.add(o);
        return l;
    }


    /*
     * Get the jython interpreter to use for evaluating embedded expressions
     *
     * @return The jython interpreter to use
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
        return jythonManager.getDerivedDataInterpreter();
        }
     */


    /**
     * Called when the displayable is removed from a display master
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void destroy() throws RemoteException, VisADException {
        if (interp != null) {
            jythonManager.removeInterpreter(interp);
            interp = null;
        }
        super.destroy();
    }


    /** Track how much time is spent evaluating code */
    private long codeTime = 0;

    /**
     * Evaluate some Jython code
     *
     *
     * @param ob           The point ob
     * @param code         Jython code
     * @param tType        tuple type of all symbols
     * @param typeNames    list of type names
     * @param data         corresponding data
     * @param formatter    formatter for code
     * @param displayUnit  unit for display
     * @return
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private Data evaluateCode(PointOb ob, String code, TupleType tType,
                              String[] typeNames, Tuple data,
                              Object formatter, Unit displayUnit)
            throws VisADException, RemoteException {
        //Find the operands in the code
        List operands = (List) codeToOperands.get(code);
        if (operands == null) {
            operands = DerivedDataChoice.parseOperands(code);
            codeToOperands.put(code, operands);
        }

        if (interp == null) {
            interp      = jythonManager.createInterpreter();
            convertCode = "from ucar.visad.Util import *\n\n";
            convertCode += "def format(v):\n";
            convertCode += "    if formatter is None:\n";
            convertCode += "        return str(v);\n";
            convertCode += "    return formatter.format(v)\n";
            convertCode += "\n\n";
            convertCode += "def convert(v):\n";
            convertCode += "    if displayUnit is None:\n";
            convertCode += "        return v\n";
            convertCode += "    return v.getValue (displayUnit)\n";
            convertCode += "\n\n";
            convertCode += "def formatDate(v,format):\n";
            convertCode += "    return formatUtcDate(v,format)\n";
            convertCode += "\n\n";
            interp.exec(convertCode);
        }

        interp.set("displayUnit", displayUnit);
        interp.set("formatter", formatter);

        //Bind the operands
        for (int opIdx = 0; opIdx < operands.size(); opIdx++) {
            String op     = (String) operands.get(opIdx).toString();
            Data   opData = getComponent(ob, data, tType, typeNames, op);
            if (opData == null) {
                return null;
            }
            interp.set(op, opData);
        }

        /**
         *       if(codeTime==0) {
         *           String testCode = "def p(op):\n";
         *           testCode += "   return op\n\n\n";
         *           interp.exec(testCode);
         *           testCode = "";
         *           for(int i=0;i<1000;i++) {
         *               interp.set("o"+i, "x"+i);
         *               testCode +="r"+i+"=p(o"+i+")\n";
         *           }
         *           long testt1 = System.currentTimeMillis();
         *           interp.exec(testCode);
         *           long testt2 = System.currentTimeMillis();
         *           System.err.println ("test:" + (testt2-testt1));
         *           for(int i=0;i<1000;i++) {
         *               Object obj = interp.get("r"+i,Object.class);
         *               System.err.print(obj+",");
         *           }
         *           System.err.print("");
         *       }
         */

        long t1 = System.currentTimeMillis();
        //Evaluate the code
        PyObject pyResult = interp.eval(code);
        long     t2       = System.currentTimeMillis();
        codeTime += (t2 - t1);



        //Get the result
        Object resultObject = pyResult.__tojava__(visad.Data.class);
        if ((resultObject == null) || !(resultObject instanceof visad.Data)) {
            resultObject = pyResult.__tojava__(Object.class);
        }

        //Make sure we have the right kind of return value
        if ( !(resultObject instanceof Data)) {
            resultObject = new visad.Text(resultObject.toString());
            //            throw new IllegalArgumentException ("Unknown return value type:" + resultObject.getClass().getName () + "\n Value=" + resultObject +"\nCode:" + code);
        }

        //Now clear out the bindings
        for (int opIdx = 0; opIdx < operands.size(); opIdx++) {
            interp.set((String) operands.get(opIdx).toString(), null);
        }
        return (Data) resultObject;

    }


    /**
     * Debug to print out a message
     *
     * @param msg  message to print
     */
    void pr(String msg) {
        System.err.println(msg);
    }

    /**
     * Debug to print out a message and the bounds of a Rectangle2D
     *
     * @param msg message to print out
     * @param r   rectangle
     */
    void pr(String msg, Rectangle2D r) {
        System.err.println(msg + " y1 =" + " " + r.getY() + " cy= "
                           + (r.getY() + r.getHeight() / 2) + " y2="
                           + (r.getY() + r.getHeight()) + " height= "
                           + r.getHeight());
    }

    /**
     * Debug to print out a message and a point
     *
     * @param msg  message
     * @param r    point
     */
    void pr(String msg, Point2D r) {
        System.err.println(msg + " pt.y= " + r.getY());
    }

    /**
     * Set the scale of the ShapeControl.  Usually done to set
     * the initial scale.
     * @param newScale  scale to use.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setScale(float newScale)
            throws VisADException, RemoteException {
        if (shapeControl != null) {
            //We set auto scale false here so the shape control 
            //clears out its controllistener which was keeping around the old initial scale

            shapeControl.setAutoScale(false);
            shapeControl.setScale(newScale);
            shapeControl.setAutoScale(true);
        }
        scale = newScale;
    }

    /**
     * Get the scale of the ShapeControl.
     * @return current scale;
     */
    public float getScale() {
        return (shapeControl != null)
               ? shapeControl.getScale()
               : scale;
    }


    /**
     * Create u and v components from speed and direction.
     * @param speed  wind speed (m/s)
     * @param direction  wind direction (degrees)
     * @param uv uv work array
     */
    private void windVector(float speed, float direction, float[] uv) {
        uv[0] = (float) (-1 * speed
                         * Math.sin(direction * Data.DEGREES_TO_RADIANS));
        uv[1] = (float) (-1 * speed
                         * Math.cos(direction * Data.DEGREES_TO_RADIANS));
    }



    /**
     * Get the index of any of a list of comma separated names in
     * that match the names of ScalarTypes in a TupleType.
     *
     * @param typeNames names
     * @param names List of param names
     * @return the index if found.
     */
    private int getIndex(String[] typeNames, List names) {
        for (int i = 0; i < names.size(); i++) {
            String name = (String) names.get(i);

            if (name.equalsIgnoreCase(PointOb.PARAM_LAT)
                    || name.equalsIgnoreCase("latitude")) {
                return INDEX_LAT;
            }
            if (name.equalsIgnoreCase(PointOb.PARAM_LON)
                    || name.equalsIgnoreCase("longitude")) {
                return INDEX_LON;
            }
            if (name.equalsIgnoreCase(PointOb.PARAM_ALT)
                    || name.equalsIgnoreCase("altitude")) {
                return INDEX_ALT;
            }
            if (name.equalsIgnoreCase(PointOb.PARAM_TIME)
                    || name.equalsIgnoreCase("dttm")) {
                return INDEX_TIME;
            }

            // first check to see if the name is good before aliases
            //            int index = getIndex(tType,ScalarType.getScalarTypeByName(name));
            int index = getIndex(typeNames, name);
            if (index != PointOb.BAD_INDEX) {
                return index;
            }
            List aliases = DataAlias.getAliasesOf(name);
            if ((aliases == null) || aliases.isEmpty()) {
                continue;
            }
            for (int aliasIdx = 0; aliasIdx < aliases.size(); aliasIdx++) {
                String alias = (String) aliases.get(aliasIdx);
                //                index = tType.getIndex(ScalarType.getScalarTypeByName(alias));
                index = getIndex(typeNames, alias);
                if (index != PointOb.BAD_INDEX) {
                    return index;
                }
            }
        }

        return PointOb.BAD_INDEX;
    }


    /**
     * Find the index in the TupleType of the MathType whose name matches
     * lookingFor. If the math type name ends with '[unit:...]' then strip
     * that off
     *
     * @param names   list of names
     * @param lookingFor  pattern to look for
     *
     * @return index or bad index
     */
    private int getIndex(String[] names, String lookingFor) {
        if (lookingFor.equals("*")) {
            if (names.length > 0) {
                return 0;
            }
            return PointOb.BAD_INDEX;
        }


        if (lookingFor.startsWith("#")) {
            int index = new Integer(lookingFor.substring(1)).intValue();
            if (index < names.length) {
                return index;
            }
            return PointOb.BAD_INDEX;

        }


        boolean not = false;
        if (lookingFor.startsWith("!")) {
            lookingFor = lookingFor.substring(1);
            not        = true;
        }

        if (StringUtil.containsRegExp(lookingFor)) {
            for (int i = 0; i < names.length; i++) {
                if (StringUtil.stringMatch(names[i], lookingFor)) {
                    if (not) {
                        continue;
                    }
                    return i;
                } else if (not) {
                    return i;
                }
            }
        }



        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(lookingFor)) {
                if (not) {
                    continue;
                }
                return i;
            } else if (not) {
                return i;
            }
        }
        return PointOb.BAD_INDEX;
    }


    /**
     *
     * @param tType  get the list of type names
     *
     * @return the list
     */
    private String[] getTypeNames(TupleType tType) {
        MathType[] comps = tType.getComponents();
        String[]   names = new String[comps.length];
        for (int i = 0; i < comps.length; i++) {
            String name = Util.cleanTypeName(comps[i]);
            names[i] = name;
        }
        return names;
    }




    /**
     * Find and return the Data component in the given Tuple that matches one of
     * the param names in the given argument.
     *
     *
     * @param ob  The point ob
     * @param data The tuple to look into.
     * @param tType  <code>TupleType</code> to check.
     * @param typeNames list of type names
     * @param commaSeparatedNames  a string containing a comma separated
     *                             list of names for a data alias.
     *
     * @return the Data if found.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     *
     */
    private Data getComponent(PointOb ob, Tuple data, TupleType tType,
                              String[] typeNames, String commaSeparatedNames)
            throws VisADException, RemoteException {

        List names = (List) namesToList.get(commaSeparatedNames);
        if (names == null) {
            names = StringUtil.split(commaSeparatedNames, ",", true, true);
            namesToList.put(commaSeparatedNames, names);
        }
        int     index;
        Integer cachedIndex = (Integer) nameToIndex.get(commaSeparatedNames);
        if (cachedIndex != null) {
            index = cachedIndex.intValue();
        } else {
            index = getIndex(typeNames, names);
            nameToIndex.put(commaSeparatedNames, new Integer(index));
        }

        if (index == INDEX_LAT) {
            return ob.getEarthLocation().getLatitude();
        }
        if (index == INDEX_LON) {
            return ob.getEarthLocation().getLongitude();
        }
        if (index == INDEX_ALT) {
            return ob.getEarthLocation().getAltitude();
        }
        if (index == INDEX_TIME) {
            return ob.getDateTime();
        }

        if (index == PointOb.BAD_INDEX) {
            if (haveNotified == null) {
                haveNotified = new Hashtable();
            }
            if (haveNotified.get(commaSeparatedNames) == null) {
                haveNotified.put(commaSeparatedNames, commaSeparatedNames);
                LogUtil.consoleMessage("Unknown field name:"
                                       + commaSeparatedNames);
            }
            return null;
        }
        return data.getComponent(index);
    }



    /**
     * Set the ShouldUseAltitude property.
     *
     * @param value The new value for ShouldUseAltitude
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setShouldUseAltitude(boolean value)
            throws VisADException, RemoteException {
        shouldUseAltitude = value;
        FieldImpl tmp = stationData;
        //        setDisplayInactive();
        setStationData(null);
        setStationData(tmp);
        //        setDisplayActive();
    }

    /**
     * Get the ShouldUseAltitude property.
     *
     * @return The ShouldUseAltitude
     */
    public boolean getShouldUseAltitude() {
        return shouldUseAltitude;
    }

    /**
     * Set selected range with the range for select
     *
     * @param low  low select value
     * @param hi   hi select value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setSelectedRange(double low, double hi)
            throws VisADException, RemoteException {

        lowSelectedRange  = low;
        highSelectedRange = hi;
        if ((timeSelectControl != null) && hasSelectedRange()) {
            timeSelectControl.setRange(new double[] { low, hi });
        }

    }

    /**
     * Set the upper and lower limit of the range values associated
     * with a color table.
     *
     * @param low    the minimun value
     * @param hi     the maximum value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setRangeForSelect(double low, double hi)
            throws VisADException, RemoteException {

        minSelect = low;
        maxSelect = hi;
        if ((timeSelectMap != null) && hasSelectMinMax()) {
            timeSelectMap.setRange(low, hi);
        }
    }

    /**
     * Check to see if the range has been set for the select
     *
     * @return true if it has
     */
    private boolean hasSelectMinMax() {
        return ( !Double.isNaN(minSelect) && !Double.isNaN(maxSelect));
    }

    /**
     * Returns whether this Displayable has a valid range
     * (i.e., lowSelectedRange and highSelectedRange are both not NaN's
     *
     * @return true if range has been set
     */
    public boolean hasSelectedRange() {
        return ( !Double.isNaN(lowSelectedRange)
                 && !Double.isNaN(highSelectedRange));
    }

}

