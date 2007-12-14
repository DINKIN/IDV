/**
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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




package ucar.unidata.repository;


import org.w3c.dom.*;


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Metadata implements Constants, Tables {
    public static final String TYPE_HTML = "html";
    public static final String TYPE_URL = "html";
    public static final String TYPE_LINK = "link";

    public static final String IDTYPE_ENTRY = "entry";
    public static final String IDTYPE_GROUP = "group";



    private String id;
    private String idType;
    private String name;
    private String metadataType;
    private String content;

    /**
     * _more_
     *
     *
     *
     * @param args _more_
     * @throws Exception _more_
     */
    public Metadata(String id, String idType, String metadataType, String name,  String content) {
        this.id=id;
        this.name=name;
        this.idType=idType;
        this.metadataType=metadataType;
        this.content=content;
    }


/**
Set the Name property.

@param value The new value for Name
**/
public void setName (String value) {
	name = value;
}

/**
Get the Name property.

@return The Name
**/
public String getName () {
	return name;
}


/**
Set the Id property.

@param value The new value for Id
**/
public void setId (String value) {
	id = value;
}

/**
Get the Id property.

@return The Id
**/
public String getId () {
	return id;
}

/**
Set the IdType property.

@param value The new value for IdType
**/
public void setIdType (String value) {
	idType = value;
}

/**
Get the IdType property.

@return The IdType
**/
public String getIdType () {
	return idType;
}

/**
Set the MetadataType property.

@param value The new value for MetadataType
**/
public void setMetadataType (String value) {
	metadataType = value;
}

/**
Get the MetadataType property.

@return The MetadataType
**/
public String getMetadataType () {
	return metadataType;
}

/**
Set the Content property.

@param value The new value for Content
**/
public void setContent (String value) {
	content = value;
}

/**
Get the Content property.

@return The Content
**/
public String getContent () {
	return content;
}






}

