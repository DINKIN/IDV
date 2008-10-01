/*
 * $Id: PointDataSource.java,v 1.33 2007/06/21 14:44:59 jeffmc Exp $
 *
 * Copyright (c) 1997-2004 Unidata Program Center/University Corporation for
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




package ucar.unidata.data.point;


import ucar.unidata.data.*;
import ucar.unidata.data.grid.GridDataSource;

import ucar.unidata.geoloc.LatLonRect;


import ucar.unidata.ui.TimeLengthField;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.WrapperException;

import visad.*;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * A data source for point data
 *
 * @author Don Murray
 * @version $Revision: 1.33 $ $Date: 2007/06/21 14:44:59 $
 */
public abstract class PointDataSource extends FilesDataSource {

    /** dataselection property          */
    public static final String PROP_GRID_POINTSX = "prop.grid.pointsx";

    /** dataselection property          */
    public static final String PROP_GRID_POINTSY = "prop.grid.pointsy";

    /** dataselection property          */
    public static final String PROP_GRID_NUMITERATIONS =
        "prop.grid.numiterations";


    /** station model name property */
    public static final String PROP_STATIONMODELNAME =
        "prop.stationmodelname";

    /** Identifier for station data */
    public static final String STATION_DATA = "Station Data";

    /** Identifier for point data */
    public static final String POINT_DATA = "Point Data";

    /** Identifier for a station plot */
    public static final String STATION_PLOT = DataCategory.CATEGORY_POINTPLOT;

    /** default categories */
    private List pointCategories = null;


    /**
     *  A cached version of the html description of the fields.
     */
    protected String fieldsDescription;

    /** bind round to factor */
    private double binRoundTo = 0;

    /** time bin width */
    private double binWidth = 0;

    /** for properties dialog */
    private TimeLengthField binWidthField;

    /** for properties dialog */
    private TimeLengthField binRoundToField;

    /** for properties dialog */
    private JComboBox roundToCbx;

    /** for properties dialog */
    private JComboBox widthCbx;

    /** number of points along x for grid         */
    private int gridPointsX = 100;

    /** number of points along y for grid         */
    private int gridPointsY = 100;

    /** Number of barnes iterations       */
    private int numGridIterations = 1;

    /** Do we make grid fields    */
    private boolean makeGridFields = true;


    /** For gui          */
    private JCheckBox makeGridFieldsCbx;

    /** For gui         */
    private GridParameters gridProperties;

    /**
     *
     * Default constructor
     *
     * @throws VisADException  problem creating VisAD data object
     */
    public PointDataSource() throws VisADException {
        init();
    }

    /**
     * Create a PointDataSource
     *
     * @param descriptor    descriptor for the DataSource
     * @param source        file location or URL
     * @param description   description of data
     * @param properties    extra properties
     *
     * @throws VisADException
     *
     */
    public PointDataSource(DataSourceDescriptor descriptor, String source,
                           String description, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.toList(new String[] { source }), description,
             properties);
    }


    /**
     * Create a new PointDataSource
     *
     * @param descriptor    data source descriptor
     * @param sources       List of sources of data (filename/URL)
     * @param name          The name to use
     * @param properties    extra properties for initialization
     *
     * @throws VisADException   problem creating the data
     *
     */
    public PointDataSource(DataSourceDescriptor descriptor, List sources,
                           String name, Hashtable properties)
            throws VisADException {
        super(descriptor, sources, (sources.size() > 1)
                                   ? "Point Data"
                                   : (String) sources.get(0), name,
                                   properties);
        try {
            init();
        } catch (VisADException exc) {
            setInError(true);
            throw exc;
        }
    }

    /**
     * Initialize this object
     *
     * @throws VisADException    problem during initialization
     */
    protected void init() throws VisADException {}



    /**
     * Class GridParameters holds the grid spacing/iterations gui. Used for hte field selector
     * and the properties
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private class GridParameters extends DataSelectionComponent {

        /** gui component         */
        private JCheckBox useDefaultCbx = new JCheckBox("Use Default", true);

        /** gui component         */
        private JTextField gridPointsXFld;

        /** gui component         */
        private JTextField gridPointsYFld;

        /** gui component         */
        private JTextField numGridIterationsFld;

        /** The list of components      */
        private List comps = new ArrayList();

        /** The main component      */
        private JComponent comp;

        /**
         * ctor
         */
        public GridParameters() {
            super("Grid Parameters");
            gridPointsXFld       = new JTextField("" + gridPointsX, 3);
            gridPointsYFld       = new JTextField("" + gridPointsY, 3);
            numGridIterationsFld = new JTextField("" + numGridIterations, 3);
            comps.add(GuiUtils.rLabel("Grid Size:"));
            comps.add(GuiUtils.left(GuiUtils.hbox(new JLabel("X: "),
                    gridPointsXFld, new JLabel("  Y: "), gridPointsYFld)));
            comps.add(GuiUtils.rLabel("Iterations:"));
            comps.add(GuiUtils.left(numGridIterationsFld));
            useDefaultCbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    checkEnable();
                }
            });
        }


        /**
         * enable/disable the component based on the useDefaultCbx value
         */
        public void checkEnable() {
            GuiUtils.enableTree(comp, !useDefaultCbx.isSelected());
        }

        /**
         * Make the gui for the field selector
         *
         * @return gui for field selector
         */
        protected JComponent doMakeContents() {
            GuiUtils.tmpInsets = GuiUtils.INSETS_5;
            comp = GuiUtils.doLayout(comps, 2, GuiUtils.WT_N, GuiUtils.WT_N);
            checkEnable();
            return GuiUtils.topCenter(GuiUtils.right(useDefaultCbx),
                                      GuiUtils.topLeft(comp));
        }


        /**
         * set properties on dataselection
         *
         * @param dataSelection the dataselection
         */
        public void applyToDataSelection(DataSelection dataSelection) {
            if ( !useDefaultCbx.isSelected()) {
                dataSelection.putProperty(PROP_GRID_POINTSX,
                                          new Integer(getGridPointsX()));
                dataSelection.putProperty(PROP_GRID_POINTSY,
                                          new Integer(getGridPointsY()));
                dataSelection.putProperty(
                    PROP_GRID_NUMITERATIONS,
                    new Integer(getNumGridIterations()));
            }
        }

        /**
         * get grid x
         *
         * @return grid x
         */
        public int getGridPointsX() {
            return GuiUtils.getInt(gridPointsXFld);
        }

        /**
         * get grid y
         *
         * @return grid y
         */
        public int getGridPointsY() {
            return GuiUtils.getInt(gridPointsYFld);
        }

        /**
         * get iterations
         *
         * @return iterations
         */
        public int getNumGridIterations() {
            return GuiUtils.getInt(numGridIterationsFld);
        }


    }


    /**
     * Add the GridParameters for the field selector
     *
     * @param components comps
     * @param dataChoice for this data
     */
    protected void initDataSelectionComponents(
            List<DataSelectionComponent> components,
            final DataChoice dataChoice) {

        if ( !(dataChoice.getId() instanceof List)) {
            return;
        }
        components.add(new GridParameters());
    }


    /**
     * not sure what this does
     *
     * @param dataChoice datachoice_
     *
     * @return false
     */
    public boolean canAddCurrentName(DataChoice dataChoice) {
        return false;
    }


    /**
     * add to properties
     *
     * @param comps comps
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);
        binWidthField   = new TimeLengthField("Bin Width", true);
        binRoundToField = new TimeLengthField("Bin Round To", true);
        binWidthField.setTime(binWidth);
        binRoundToField.setTime(binRoundTo);
        List roundToItems = Misc.toList(new Object[] {
            new TwoFacedObject("Change", new Double(0)),
            new TwoFacedObject("On the hour", new Double(60)),
            new TwoFacedObject("5 after", new Double(5)),
            new TwoFacedObject("10 after", new Double(10)),
            new TwoFacedObject("15 after", new Double(15)),
            new TwoFacedObject("20 after", new Double(20)),
            new TwoFacedObject("30 after", new Double(30)),
            new TwoFacedObject("45 after", new Double(45)),
            new TwoFacedObject("10 to", new Double(50)),
            new TwoFacedObject("5 to", new Double(55))
        });

        roundToCbx = GuiUtils.makeComboBox(roundToItems, roundToItems.get(0),
                                           false, this,
                                           "setRoundToFromComboBox");

        List widthItems = Misc.toList(new Object[] {
            new TwoFacedObject("Change", new Double(0)),
            new TwoFacedObject("5 minutes", new Double(5)),
            new TwoFacedObject("10 minutes", new Double(10)),
            new TwoFacedObject("15 minutes", new Double(15)),
            new TwoFacedObject("20 minutes", new Double(20)),
            new TwoFacedObject("30 minutes", new Double(30)),
            new TwoFacedObject("45 minutes", new Double(45)),
            new TwoFacedObject("1 hour", new Double(60)),
            new TwoFacedObject("6 hours", new Double(60 * 6)),
            new TwoFacedObject("12 hours", new Double(60 * 12)),
            new TwoFacedObject("1 day", new Double(60 * 24))
        });



        widthCbx = GuiUtils.makeComboBox(widthItems, widthItems.get(0),
                                         false, this, "setWidthFromComboBox");

        comps.add(GuiUtils.filler());
        comps.add(getPropertiesHeader("Time Binning"));

        comps.add(GuiUtils.rLabel("Bin Size:"));
        comps.add(GuiUtils.left(GuiUtils.hbox(binWidthField.getContents(),
                widthCbx, 5)));
        comps.add(GuiUtils.rLabel("Round To:"));
        comps.add(GuiUtils.left(GuiUtils.hbox(binRoundToField.getContents(),
                roundToCbx, 5)));



    }

    /**
     * Add the Grid Fields component to the properties tab
     *
     * @param tabbedPane properties tab
     */
    public void addPropertiesTabs(JTabbedPane tabbedPane) {
        super.addPropertiesTabs(tabbedPane);
        List comps = new ArrayList();
        gridProperties    = new GridParameters();
        makeGridFieldsCbx = new JCheckBox("Make Grid Fields", makeGridFields);
        comps.add(GuiUtils.filler());
        comps.add(GuiUtils.left(makeGridFieldsCbx));
        comps.addAll(gridProperties.comps);
        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        tabbedPane.addTab("Objective Analysis",
                          GuiUtils.topLeft(GuiUtils.doLayout(comps, 2,
                              GuiUtils.WT_NN, GuiUtils.WT_N)));
    }


    /**
     * Set the property
     *
     * @param tfo value from combo box_
     */
    public void setRoundToFromComboBox(TwoFacedObject tfo) {
        double value = ((Double) tfo.getId()).doubleValue();
        if (value == 0.0) {
            return;
        }
        binRoundToField.setTime(value);
        roundToCbx.setSelectedIndex(0);
    }

    /**
     * set the property
     *
     * @param tfo value_
     */
    public void setWidthFromComboBox(TwoFacedObject tfo) {
        double value = ((Double) tfo.getId()).doubleValue();
        if (value == 0.0) {
            return;
        }
        binWidthField.setTime(value);
        widthCbx.setSelectedIndex(0);
    }



    /**
     * apply the properties
     *
     * @return success
     */
    public boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        boolean changed = false;
        String  what    = "";
        try {
            what = "Bad bin value";
            changed |= (binRoundToField.getTime() != binRoundTo)
                       || (binWidth != binWidthField.getTime());
            binRoundTo = binRoundToField.getTime();
            binWidth   = binWidthField.getTime();

            what       = "Bad grid points X value";
            changed    |= (gridPointsX != gridProperties.getGridPointsX());
            what       = "Bad grid points Y value";
            changed    |= (gridPointsY != gridProperties.getGridPointsY());
            what       = "Bad grid iterations value";
            changed |= (numGridIterations
                        != gridProperties.getNumGridIterations());
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage(what);
            return false;
        }

        gridPointsX       = gridProperties.getGridPointsX();
        gridPointsY       = gridProperties.getGridPointsY();
        numGridIterations = gridProperties.getNumGridIterations();
        if (makeGridFields != makeGridFieldsCbx.isSelected()) {
            makeGridFields = makeGridFieldsCbx.isSelected();
            dataChoices    = null;
            getDataChoices();
            getDataContext().dataSourceChanged(this);
        }

        if (changed) {
            flushCache();
        }

        return true;
    }




    /**
     * Read a sample of the data. e.g., just the first ob
     *
     * @param dataChoice The data choice
     *
     * @return The first ob
     *
     * @throws Exception On badness
     */
    protected FieldImpl getSample(DataChoice dataChoice) throws Exception {
        return null;
    }

    /**
     * Make the <code>DataChoices</code> for this <code>DataSource</code>.
     */
    public void doMakeDataChoices() {
        if (sources == null) {
            return;
        }
        String stationModelName = (String) getProperty(PROP_STATIONMODELNAME);
        Hashtable properties = Misc.newHashtable(DataChoice.PROP_ICON,
                                   "/auxdata/ui/icons/Placemark16.gif");
        if (stationModelName != null) {
            properties.put(PROP_STATIONMODELNAME, stationModelName);
        }
        DataChoice uberChoice = null;
        /*  Might want to do this someday
        uberChoice = new DirectDataChoice(this,
                                            sources, getName(),
                                            getDataName(),
                                            getPointCategories(),
                                            properties);
        */
        if (sources.size() > 1) {
            uberChoice = new CompositeDataChoice(this, sources, getName(),
                    getDataName(), getPointCategories(), properties);
            addDataChoice(uberChoice);
        }


        for (int i = 0; i < sources.size(); i++) {
            DataChoice choice = new DirectDataChoice(this, new Integer(i),
                                    getDescription(), getDataName(),
                                    getPointCategories(), properties);

            /*
              We'd like to create sub choices for each parameter but we don't really
              know the parameters until we read the data and that can be expensive
                          DirectDataChoice subChoice = new DirectDataChoice(this,
                                    (String) sources.get(i),
                                    getDescription(), getDataName(),
                                    getPointCategories(), properties);
                                    choice.addDataChoice(subChoice);*/

            if (sources.size() > 1) {
                ((CompositeDataChoice) uberChoice).addDataChoice(choice);
            } else {
                addDataChoice(choice);
            }
            try {
                FieldImpl sample = (makeGridFields
                                    ? getSample(choice)
                                    : null);
                if (sample != null) {
                    if (ucar.unidata.data.grid.GridUtil.isTimeSequence(
                            sample)) {
                        sample = (FieldImpl) sample.getSample(0);
                    }
                    PointOb             ob = (PointOb) sample.getSample(0);
                    Tuple               tuple = (Tuple) ob.getData();
                    TupleType tupleType = (TupleType) tuple.getType();
                    MathType[]          types = tupleType.getComponents();
                    CompositeDataChoice compositeDataChoice = null;
                    for (int typeIdx = 0; typeIdx < types.length; typeIdx++) {
                        if ( !(types[typeIdx] instanceof RealType)) {
                            continue;
                        }
                        RealType type = (RealType) types[typeIdx];
                        if (type.getDefaultUnit() == null) {
                            continue;
                        }
                        //                        List gridCategories = 
                        //                            DataCategory.parseCategories("OA Fields;GRID-2D-TIME;");
                        List gridCategories =
                            DataCategory.parseCategories("GRID-2D-TIME;",
                                false);
                        if (compositeDataChoice == null) {
                            compositeDataChoice =
                                new CompositeDataChoice(this, "",
                                    "Objective Analysis Derived Grid Fields",
                                    "OA Fields",
                                    Misc.newList(DataCategory.NONE_CATEGORY),
                                    null);
                            addDataChoice(compositeDataChoice);
                        }
                        DataChoice gridChoice =
                            new DirectDataChoice(this,
                                Misc.newList(new Integer(i), type),
                                type.toString(), type.toString(),
                                gridCategories, (Hashtable) null);
                        compositeDataChoice.addDataChoice(gridChoice);
                    }
                }
            } catch (Exception exc) {
                throw new WrapperException("Making grid parameters", exc);
            }


        }

    }


    /**
     * Get the file or url source path from the given data choice.
     * The new version uses an Integer index into the sources list
     * as the id of the data choice. However, this method does handle
     *
     *
     * @param dataChoice The data choice
     *
     * @return The file or url the data choice refers to
     */
    protected String getSource(DataChoice dataChoice) {
        Object id = dataChoice.getId();
        if (id instanceof String) {
            return (String) id;
        } else if (id instanceof Integer) {
            int idx = ((Integer) id).intValue();
            return (String) sources.get(idx);
        }
        return null;
    }


    /**
     * Get the default categories for data from PointDataSource-s
     *
     * @return list of categories
     */
    protected List getPointCategories() {
        if (pointCategories == null) {
            pointCategories =
                DataCategory.parseCategories(DataCategory.CATEGORY_POINT
                                             + ";" + STATION_PLOT, false);
        }
        return pointCategories;
    }

    /**
     * Get the name of this data.
     *
     * @return name of data
     */
    public String getDataName() {
        return POINT_DATA;
    }



    /**
     * Get the data represented by this class.  Calls makeObs, real work
     * needs to be implemented there.
     *
     * @param dataChoice         choice for data
     * @param category           category of data
     * @param dataSelection      subselection properties
     * @param requestProperties  additional selection properties (not used here)
     * @return  Data object representative of the choice
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        Object id = dataChoice.getId();
        //If it is a list then we are doing a grid field
        if (id instanceof List) {
            Integer  i    = (Integer) ((List) id).get(0);
            RealType type = (RealType) ((List) id).get(1);
            DataChoice choice = new DirectDataChoice(this, i, "", "",
                                    dataChoice.getCategories(),
                                    dataChoice.getProperties());
            FieldImpl pointObs = (FieldImpl) getDataInner(choice, category,
                                     dataSelection, requestProperties);
            if (pointObs == null) {
                return null;
            }
            //{ minY, minX, maxY, maxX };
            int     pointsX    = this.gridPointsX;
            int     pointsY    = this.gridPointsY;
            int     iterations = this.numGridIterations;
            Integer tmp;
            tmp = (Integer) dataSelection.getProperty(PROP_GRID_POINTSX);
            if (tmp != null) {
                pointsX = tmp.intValue();
            }
            tmp = (Integer) dataSelection.getProperty(PROP_GRID_POINTSY);
            if (tmp != null) {
                pointsY = tmp.intValue();
            }
            tmp = (Integer) dataSelection.getProperty(
                PROP_GRID_NUMITERATIONS);
            if (tmp != null) {
                iterations = tmp.intValue();
            }
            pointObs = PointObFactory.makeTimeSequenceOfPointObs(pointObs);
            //            System.err.println("spacing:" + pointsX +" " + pointsY);

            double[] bbox  = PointObFactory.getBoundingBox(pointObs);
            float    spanY = (float) Math.abs(bbox[0] - bbox[2]);
            float    spanX = (float) Math.abs(bbox[1] - bbox[3]);
            LogUtil.message("Doing Barnes Analysis");
            return PointObFactory.barnes(pointObs, type, spanX / pointsX,
                                         spanY / pointsY, iterations);
        }


        GeoSelection    geoSelection = ((dataSelection != null)
                                        ? dataSelection.getGeoSelection()
                                        : null);
        GeoLocationInfo bbox         = ((geoSelection == null)
                                        ? null
                                        : geoSelection.getBoundingBox());


        LatLonRect      llr          = ((bbox != null)
                                        ? bbox.getLatLonRect()
                                        : null);


        FieldImpl       retField     = null;
        try {
            //List choices = (List) dataChoice.getId();
            List choices = (dataChoice instanceof CompositeDataChoice)
                           ? ((CompositeDataChoice) dataChoice)
                               .getDataChoices()
                           : Misc.toList(new DataChoice[] { dataChoice });
            List datas = new ArrayList(choices.size());
            for (int i = 0; i < choices.size(); i++) {
                FieldImpl obs = makeObs((DataChoice) choices.get(i),
                                        dataSelection, llr);
                if (obs == null) {
                    return null;
                }
                if (true) {
                    return obs;
                }
                datas.add(obs);
                if ((fieldsDescription == null) && (obs != null)) {
                    makeFieldDescription(obs);
                }
            }
            retField = PointObFactory.mergeData(datas);
        } catch (Exception exc) {
            logException("Creating obs", exc);
        }
        return retField;
    }


    /**
     * Override this method so we don't make any derived data choices from the grid fields
     *
     * @param dataChoices base list of choices
     */
    protected void makeDerivedDataChoices(List dataChoices) {}

    /**
     * Override the base class method to add on the listing of the
     * param names in the point tuple.
     *
     * @return   full description of this datasource for help
     */
    public String getFullDescription() {
        String parentDescription = super.getFullDescription();
        if (fieldsDescription == null) {
            /*
              Don't do this can this can cost us
            try {
                FieldImpl fi =
                    (FieldImpl) getData(getDescriptionDataChoice(), null,
                                        null);
            } catch (Exception exc) {
                logException("getting description", exc);
                return "";
            }
            */
        }
        return parentDescription + "<p>" + ((fieldsDescription != null)
                                            ? fieldsDescription
                                            : "");
    }

    /**
     * Get the data choice to use for the description
     *
     * @return  the data choice
     */
    protected DataChoice getDescriptionDataChoice() {
        return (DataChoice) getDataChoices().get(0);
    }

    /**
     * Create e field description from the field
     *
     * @param fi  field to use
     */
    protected void makeFieldDescription(FieldImpl fi) {
        if (fi == null) {
            fieldsDescription = "Bad data: null";
            return;
        }
        try {
            if (ucar.unidata.data.grid.GridUtil.isTimeSequence(fi)) {
                fi = (FieldImpl) fi.getSample(0);
            }
            PointOb    ob    = (PointOb) fi.getSample(0);
            Tuple      tuple = (Tuple) ob.getData();
            MathType[] comps = ((TupleType) tuple.getType()).getComponents();
            Trace.msg("PointDataSource #vars=" + comps.length);
            StringBuffer params = new StringBuffer(comps.length
                                      + " Fields:<ul>");
            String dataSourceName = getName();
            for (int i = 0; i < comps.length; i++) {
                params.append("<li>");
                String paramName =
                    ucar.visad.Util.cleanTypeName(comps[i].toString());
                DataAlias alias = DataAlias.findAlias(paramName);
                params.append(paramName);
                if (alias != null) {
                    params.append(" --  " + alias.getLabel());
                    DataChoice.addCurrentName(
                        new TwoFacedObject(
                            dataSourceName + ">" + alias.getLabel() + " -- "
                            + paramName, paramName));
                } else {
                    DataChoice.addCurrentName(
                        new TwoFacedObject(
                            dataSourceName + ">" + paramName, paramName));
                }
                Data data = tuple.getComponent(i);
                if (data instanceof Real) {
                    Unit unit = ((Real) data).getUnit();
                    if (unit != null) {
                        params.append("  [" + unit.toString() + "]");
                    }
                }
            }
            fieldsDescription = params.toString();
        } catch (Exception exc) {
            logException("getting description", exc);
        }
    }



    /**
     * Make the observation data
     *
     * @param dataChoice  choice describing the data
     * @param subset subselection (not used)
     * @param bbox The bounding box
     *
     * @return FieldImpl of PointObs
     *
     * @throws Exception problem (VisAD or IO)
     */
    protected abstract FieldImpl makeObs(DataChoice dataChoice,
                                         DataSelection subset,
                                         LatLonRect bbox)
     throws Exception;




    /**
     * Set the source property (filename or URL).  Used by persistence
     *
     * @param value  data source
     */
    public void setSource(String value) {
        setSources(Misc.toList(new String[] { value }));
    }


    /**
     * Set the BinWidth property.
     *
     * @param value The new value for BinWidth
     */
    public void setBinWidth(double value) {
        binWidth = value;
    }

    /**
     * Get the BinWidth property.
     *
     * @return The BinWidth
     */
    public double getBinWidth() {
        return binWidth;
    }

    /**
     * Set the BinRoundTo property.
     *
     * @param value The new value for BinRoundTo
     */
    public void setBinRoundTo(double value) {
        binRoundTo = value;
    }

    /**
     * Get the BinRoundTo property.
     *
     * @return The BinRoundTo
     */
    public double getBinRoundTo() {
        return binRoundTo;
    }

    /**
     *  Set the GridPointsX property.
     *
     *  @param value The new value for GridPointsX
     */
    public void setGridPointsX(int value) {
        gridPointsX = value;
    }

    /**
     *  Get the GridPointsX property.
     *
     *  @return The GridPointsX
     */
    public int getGridPointsX() {
        return gridPointsX;
    }

    /**
     *  Set the GridPointsY property.
     *
     *  @param value The new value for GridPointsY
     */
    public void setGridPointsY(int value) {
        gridPointsY = value;
    }

    /**
     *  Get the GridPointsY property.
     *
     *  @return The GridPointsY
     */
    public int getGridPointsY() {
        return gridPointsY;
    }

    /**
     *  Set the NumGridIterations property.
     *
     *  @param value The new value for NumGridIterations
     */
    public void setNumGridIterations(int value) {
        numGridIterations = value;
    }

    /**
     *  Get the NumGridIterations property.
     *
     *  @return The NumGridIterations
     */
    public int getNumGridIterations() {
        return numGridIterations;
    }

    /**
     * Set the MakeGridFields property.
     *
     * @param value The new value for MakeGridFields
     */
    public void setMakeGridFields(boolean value) {
        makeGridFields = value;
    }

    /**
     * Get the MakeGridFields property.
     *
     * @return The MakeGridFields
     */
    public boolean getMakeGridFields() {
        return makeGridFields;
    }


}

