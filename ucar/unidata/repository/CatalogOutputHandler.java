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
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class CatalogOutputHandler extends OutputHandler {

    /** _more_ */
    public static final String CATALOG_ATTRS =
        " xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ";

    /** _more_ */
    public static final String OUTPUT_CATALOG = "thredds.catalog";


    /** _more_ */
    public static final String TAG_CATALOG = "catalog";

    /** _more_ */
    public static final String TAG_NAME = "name";

    /** _more_ */
    public static final String TAG_CONTACT = "contact";


    /** _more_ */
    public static final String TAG_GEOSPATIALCOVERAGE = "geospatialCoverage";

    /** _more_ */
    public static final String TAG_TIMECOVERAGE = "timeCoverage";

    /** _more_ */
    public static final String TAG_START = "start";

    /** _more_ */
    public static final String TAG_END = "end";

    /** _more_ */
    public static final String TAG_DATASIZE = "dataSize";

    /** _more_ */
    public static final String TAG_DATE = "date";

    /** _more_ */
    public static final String TAG_METADATA = "metadata";

    /** _more_ */
    public static final String TAG_ACCESS = "access";




    /** _more_ */
    public static final String ATTR_METADATATYPE = "metadataType";


    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_UNITS = "units";

    /** _more_ */
    public static final String TAG_DATASET = "dataset";

    /** _more_ */
    public static final String TAG_SERVICE = "service";

    /** _more_ */
    public static final String TAG_SERVICENAME = "serviceName";


    /** _more_ */
    public static final String ATTR_URLPATH = "urlPath";


    /** _more_ */
    public static final String ATTR_BASE = "base";

    /** _more_ */
    public static final String ATTR_SERVICETYPE = "serviceType";



    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public CatalogOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
    }

    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param metadataList _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public static void collectMetadata(Repository repository,
                                       List<Metadata> metadataList,
                                       Element node)
            throws Exception {
        NodeList elements = XmlUtil.getElements(node);
        List<MetadataHandler> metadataHandlers =
            repository.getMetadataManager().getMetadataHandlers();

        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            String  tag   = child.getTagName();
            if (tag.equals(TAG_METADATA)) {
                if ( !XmlUtil.getAttribute(child, "metadataType",
                                           "THREDDS").equals("THREDDS")) {
                    //                    System.err.println("Skipping: "
                    //                                       + XmlUtil.toString(child));
                    continue;
                }
                if (XmlUtil.hasAttribute(child, "xlink:href")) {
                    String url = XmlUtil.getAttribute(child, "xlink:href");
                    Element root = XmlUtil.getRoot(url,
                                       CatalogOutputHandler.class);
                    collectMetadata(repository, metadataList, root);
                } else {
                    collectMetadata(repository, metadataList, child);
                }
            } else {

                for (MetadataHandler metadataHandler : metadataHandlers) {
                    Metadata metadata =
                        metadataHandler.makeMetadataFromCatalogNode(child);
                    if (metadata != null) {
                        metadataList.add(metadata);
                        break;
                    }
                }

                //                System.err.println ("UNKNOWN:" + tag  + " " + XmlUtil.toString(node).trim());
                //                System.err.println("UNKNOWN:" + tag);
                //                throw new IllegalArgumentException("");
            }
        }
    }




    /**
     * _more_
     *
     *
     * @param output _more_
     *
     * @return _more_
     */
    public boolean canHandle(String output) {
        return output.equals(OUTPUT_CATALOG);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param what _more_
     * @param types _more_
     *
     *
     * @throws Exception _more_
     */
    protected void getOutputTypesFor(Request request, String what, List types)
            throws Exception {
        if (what.equals(WHAT_ENTRIES)) {
            getOutputTypesForEntries(request, new ArrayList(), types);
            return;
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param types _more_
     *
     *
     * @throws Exception _more_
     */
    protected void getOutputTypesForEntries(Request request,
                                            List<Entry> entries, List types)
            throws Exception {
        types.add(new TwoFacedObject("Thredds Catalog", OUTPUT_CATALOG));
    }



    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(String output) {
        if (output.equals(OUTPUT_CATALOG)) {
            return repository.getMimeTypeFromSuffix(".xml");
        } else {
            return super.getMimeType(output);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, Group group,
                              List<Group> subGroups, List<Entry> entries)
            throws Exception {
        String       title = group.getFullName();
        StringBuffer sb    = new StringBuffer();
        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(TAG_CATALOG,
                                  CATALOG_ATTRS
                                  + XmlUtil.attrs(ATTR_NAME, title)));
        sb.append(XmlUtil.openTag(TAG_SERVICE,
                                  XmlUtil.attrs(ATTR_NAME, "all",
                                      ATTR_SERVICETYPE, "Compound",
                                      ATTR_BASE, "")));
        sb.append(XmlUtil.tag(TAG_SERVICE,
                              XmlUtil.attrs(ATTR_NAME, "self",
                                            ATTR_SERVICETYPE, "HTTP",
                                            ATTR_BASE, "")));
        sb.append(XmlUtil.closeTag(TAG_SERVICE));



        sb.append(XmlUtil.openTag(TAG_DATASET,
                                  XmlUtil.attrs(ATTR_NAME, title)));
        sb.append(toCatalogInner(request, subGroups));
        sb.append(toCatalogInner(request, entries));
        sb.append(XmlUtil.closeTag(TAG_DATASET));
        sb.append(XmlUtil.closeTag(TAG_CATALOG));
        return new Result(title, sb, getMimeType(OUTPUT_CATALOG));

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param groups _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroups(Request request, List<Group> groups)
            throws Exception {
        StringBuffer sb    = new StringBuffer();
        String       title = "Groups";
        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(TAG_CATALOG,
                                  CATALOG_ATTRS
                                  + XmlUtil.attrs(ATTR_NAME, title)));
        sb.append(XmlUtil.openTag(TAG_DATASET,
                                  XmlUtil.attrs(ATTR_NAME, title)));
        sb.append(toCatalogInner(request, groups));
        sb.append(XmlUtil.closeTag(TAG_DATASET));
        sb.append(XmlUtil.closeTag(TAG_CATALOG));
        return new Result(title, sb, getMimeType(OUTPUT_CATALOG));
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntries(Request request, List<Entry> entries)
            throws Exception {
        return toCatalog(request, entries, "Entries");
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param objects _more_
     * @param title _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result toCatalog(Request request, List objects, String title)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(TAG_CATALOG,
                                  CATALOG_ATTRS
                                  + XmlUtil.attrs(ATTR_NAME, title)));
        sb.append(XmlUtil.openTag(TAG_DATASET,
                                  XmlUtil.attrs(ATTR_NAME, title)));
        sb.append(toCatalogInner(request, objects));
        sb.append(XmlUtil.closeTag(TAG_DATASET));
        sb.append(XmlUtil.closeTag(TAG_CATALOG));
        Result result = new Result(title, sb, getMimeType(OUTPUT_CATALOG));
        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param objects _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected StringBuffer toCatalogInner(Request request, List objects)
            throws Exception {
        StringBuffer sb      = new StringBuffer();
        List<Entry>  entries = new ArrayList();
        List<Group>  groups  = new ArrayList();
        for (Object obj : objects) {
            if (obj instanceof Entry) {
                entries.add((Entry) obj);
            } else if (obj instanceof Group) {
                groups.add((Group) obj);
            } else {
                throw new IllegalArgumentException("Unknown object type:"
                        + obj.getClass().getName());
            }

        }
        for (Group group : groups) {
            String url =  /* "http://localhost:8080"+*/
                HtmlUtil.url(repository.URL_GROUP_SHOW, ARG_ID,
                             group.getId(), ARG_OUTPUT, OUTPUT_CATALOG);
            sb.append(XmlUtil.tag(TAG_CATALOGREF,
                                  XmlUtil.attrs(ATTR_XLINKTITLE,
                                      group.getName(), ATTR_XLINKHREF, url)));
        }

        EntryGroup entryGroup = new EntryGroup("");
        for (Entry entry : entries) {
            String     typeDesc = entry.getTypeHandler().getLabel();
            EntryGroup subGroup = entryGroup.find(typeDesc);
            subGroup.add(entry);
        }


        generate(request, sb, entryGroup);
        return sb;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param parent _more_
     */
    protected void generate(Request request, StringBuffer sb,
                            EntryGroup parent) {
        for (int i = 0; i < parent.keys().size(); i++) {
            Object     key   = parent.keys().get(i);
            EntryGroup group = (EntryGroup) parent.map.get(key);
            sb.append(XmlUtil.openTag(TAG_DATASET,
                                      XmlUtil.attrs(ATTR_NAME,
                                          group.key.toString())));
            for (int j = 0; j < group.children.size(); j++) {
                Object child = group.children.get(j);
                if (child instanceof EntryGroup) {
                    EntryGroup subGroup = (EntryGroup) child;
                    generate(request, sb, subGroup);
                } else if (child instanceof Entry) {
                    Entry entry = (Entry) child;
                    entry.getTypeHandler().getDatasetTag(sb, entry, request);
                }
            }
            sb.append(XmlUtil.closeTag(TAG_DATASET));
        }
    }

}

