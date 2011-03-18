/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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

/**
 * DataSource for Grid files.
 *
 * @author Metapps development team
 * @version $Revision: 1.18 $ $Date: 2007/04/20 13:54:08 $
 */

package ucar.unidata.data.grid;


import ucar.unidata.data.*;

import ucar.unidata.util.Misc;


import ucar.unidata.xml.XmlEncoder;

import visad.VisADException;

import java.rmi.RemoteException;



import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;


/**
 *  An abstract  class that provides a list of 2d and 3d DataCategory objects
 *  for   grid data sources.
 */
public abstract class GridDataSource extends FilesDataSource {

    /** _more_          */
    public static final String ATTR_NORTH = "north";

    /** _more_          */
    public static final String ATTR_SOUTH = "south";

    /** _more_          */
    public static final String ATTR_EAST = "east";

    /** _more_          */
    public static final String ATTR_WEST = "west";

    /** _more_          */
    public static final String ATTR_X = "x";

    /** _more_          */
    public static final String ATTR_Y = "y";

    /** _more_          */
    public static final String ATTR_Z = "z";


    /** List of 2D categories for grids */
    private List twoDCategories;

    /** List of 3D categories for grids */
    private List threeDCategories;

    /** List of 2D categories for time series of grids */
    private List twoDTimeSeriesCategories;

    /** List of 2D ensemble categories for time series of grids */
    private List twoDEnsTimeSeriesCategories;

    /** List of 3D categories for time series of grids */
    private List threeDTimeSeriesCategories;

    /** List of 3D ensemble categories for time series of grids */
    private List threeDEnsTimeSeriesCategories;

    /** List of ens categories for grids */
    private DataCategory ensDCategory;

    /**
     * Default constructor; initializes data categories
     */
    public GridDataSource() {
        initCategories();
    }



    /**
     * _more_
     *
     * @param descriptor _more_
     */
    public GridDataSource(DataSourceDescriptor descriptor) {
        super(descriptor);
        initCategories();
    }

    /**
     * Create a GridDataSource from the specification given.
     *
     * @param descriptor       data source descriptor
     * @param source of file   filename or URL
     * @param name             name of this data source
     * @param properties       extra initialization properties
     */
    public GridDataSource(DataSourceDescriptor descriptor, String source,
                          String name, Hashtable properties) {
        super(descriptor, Misc.newList(source), source, name, properties);
        initCategories();
    }


    /**
     * Create a GridDataSource from the specification given.
     *
     * @param descriptor       data source descriptor
     * @param sources          List of files or URLS
     * @param name             name of this data source
     * @param properties       extra initialization properties
     */
    public GridDataSource(DataSourceDescriptor descriptor, List sources,
                          String name, Hashtable properties) {
        super(descriptor, sources, name, properties);
        initCategories();
    }



    /**
     * Initialize the data categories
     */
    public void initCategories() {
        if (twoDTimeSeriesCategories == null) {
            twoDTimeSeriesCategories =
                DataCategory.parseCategories("2D grid;GRID-2D-TIME;");
            twoDEnsTimeSeriesCategories = DataCategory.parseCategories(
                "2D grid;GRID-2D-TIME;ENSEMBLE;");
            twoDCategories = DataCategory.parseCategories("2D grid;GRID-2D;");
            threeDTimeSeriesCategories =
                DataCategory.parseCategories("3D grid;GRID-3D-TIME;");
            threeDEnsTimeSeriesCategories = DataCategory.parseCategories(
                "3D grid;GRID-3D-TIME;ENSEMBLE;");
            threeDCategories =
                DataCategory.parseCategories("3D grid;GRID-3D;");
            ensDCategory = DataCategory.parseCategory("ENSEMBLE", true);
        }
    }

    /**
     * Get the ensemble data categories
     * @return   list of categories
     */
    public DataCategory getEnsDCategory() {
        return ensDCategory;
    }


    /**
     * Get the 2D data categories
     * @return   list of categories
     */
    public List getTwoDCategories() {
        return twoDCategories;
    }

    /**
     * Get the 3D data categories
     * @return   list of categories
     */
    public List getThreeDCategories() {
        return threeDCategories;
    }


    /**
     * Get the list of 2D time series categories
     * @return   list of categories
     */
    public List getTwoDTimeSeriesCategories() {
        return twoDTimeSeriesCategories;
    }

    /**
     * Get the list of 2D time series ensemble categories
     * @return   list of categories
     */
    public List getTwoDEnsTimeSeriesCategories() {
        return twoDEnsTimeSeriesCategories;
    }

    /**
     * Get the list of 3D time series categories
     * @return   list of categories
     */
    public List getThreeDTimeSeriesCategories() {
        return threeDTimeSeriesCategories;
    }

    /**
     * Get the list of 3D time series ensemble categories
     * @return   list of categories
     */
    public List getThreeDEnsTimeSeriesCategories() {
        return threeDEnsTimeSeriesCategories;
    }

}
