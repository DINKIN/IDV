/*
 * $Id: FrontDataSource.java,v 1.15 2007/04/17 22:22:52 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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





package ucar.unidata.data.text;


import edu.wisc.ssec.mcidas.adde.AddeTextReader;

import ucar.unidata.data.BadDataException;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;

import ucar.unidata.data.DirectDataChoice;


import ucar.unidata.data.FilesDataSource;

import ucar.unidata.idv.control.DrawingControl;
import ucar.unidata.idv.control.drawing.DrawingGlyph;
import ucar.unidata.idv.control.drawing.FrontGlyph;
import ucar.unidata.idv.control.drawing.HighLowGlyph;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;


import ucar.visad.display.FrontDrawer;



import visad.*;

import java.io.ByteArrayInputStream;



import java.io.File;
import java.io.FileInputStream;

import java.rmi.RemoteException;



import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;




/**
 * A class for handling text (and HTML) classes
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */
public abstract class TextProductDataSource extends FilesDataSource {

    /**
     * Default bean constructor; does nothing.
     *
     */
    public TextProductDataSource() {}


    /**
     * Ctor
     *
     * @param descriptor The descriptor
     * @param name The name
     * @param description The long name
     * @param properties properties
     */
    public TextProductDataSource(DataSourceDescriptor descriptor, String name,
                           String description, Hashtable properties) {
        super(descriptor, name, description, properties);
    }


    /**
     * Ctor
     *
     * @param descriptor The descriptor
     * @param newSources List of files or urls
     * @param description The long name
     * @param properties properties
     */
    public TextProductDataSource(DataSourceDescriptor descriptor, List newSources,
                           String description, Hashtable properties) {
        super(descriptor, newSources, description, properties);
    }


    /**
     * Create a TrackDataSource from the specification given.
     *
     * @param descriptor    data source descriptor
     * @param newSources       List of sources of data (filename/URL)
     * @param name my name
     * @param description   description of the data
     * @param properties    extra properties for initialization
     */
    public TextProductDataSource(DataSourceDescriptor descriptor, List newSources,
                           String name, String description,
                           Hashtable properties) {
        super(descriptor, newSources, name, description, properties);
    }




    /**
     * _more_
     *
     * @param productType _more_
     *
     * @return _more_
     */
    public abstract String readProduct(ProductType productType);

    /**
     * _more_
     *
     * @param product _more_
     *
     * @return _more_
     */
    public abstract NamedStationTable getStations(ProductType productType) throws Exception;



    /**
     * _more_
     *
     * @return _more_
     */
    public abstract List<ProductGroup> getProductGroups();


    /**
     * _more_
     */
    protected void doMakeDataChoices() {
        String category = "textproducts";
        String docName  = getName();
        addDataChoice(
            new DirectDataChoice(
                this, docName, docName, docName,
                DataCategory.parseCategories(category, false)));
    }



}

