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
        Document doc = XmlUtil.makeDocument();
        Element root = XmlUtil.create(doc,TAG_CATALOG,null,new String[]{
            "xmlns","http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0",
            "xmlns:xlink","http://www.w3.org/1999/xlink",
            ATTR_NAME,title});


        Element service = XmlUtil.create(doc,TAG_SERVICE,root,new String[]{
            ATTR_NAME,"all",
            ATTR_SERVICETYPE, "Compound",
            ATTR_BASE,""});

        Element self = XmlUtil.create(doc,TAG_SERVICE, service, new String[]{
            ATTR_NAME,"self",
            ATTR_SERVICETYPE, "HTTP",
            ATTR_BASE, ""});

        Element dataset = XmlUtil.create(doc,TAG_DATASET, root,new String[]{
            ATTR_NAME, title});

        toCatalogInner(request, subGroups,doc,dataset);
        toCatalogInner(request, entries,doc,dataset);
        StringBuffer sb    = new StringBuffer(XmlUtil.XML_HEADER);
        sb.append(XmlUtil.toString(root));
        return new Result(title, sb, "text/xml");
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
    protected void toCatalogInner(Request request, List entryList, Document doc, Element parent)
            throws Exception {

        List<Entry>  entries = new ArrayList();
        List<Group>  groups  = new ArrayList();
        for (int i=0;i<entryList.size();i++) {
            Entry entry = (Entry) entryList.get(i);
            if (entry.isGroup()) {
                groups.add((Group) entry);
            } else {
                entries.add(entry);
            } 
        }
        for (Group group : groups) {
            String url =  /* "http://localhost:8080"+*/
                HtmlUtil.url(repository.URL_GROUP_SHOW, ARG_ID,
                             group.getId(), ARG_OUTPUT, OUTPUT_CATALOG);
            
            Element ref = XmlUtil.create(doc,TAG_CATALOGREF, parent, new String[]{
                ATTR_XLINKTITLE, group.getName(),
                ATTR_XLINKHREF, url});
        }

        EntryGroup entryGroup = new EntryGroup("");
        for (Entry entry : entries) {
            String     typeDesc = entry.getTypeHandler().getLabel();
            EntryGroup subGroup = entryGroup.find(typeDesc);
            subGroup.add(entry);
        }


        generate(request,  entryGroup, doc, parent);

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param parent _more_
     */
    protected void generate(Request request, EntryGroup parent,
                            Document doc, Element datasetNode) throws Exception {


        for (int i = 0; i < parent.keys().size(); i++) {
            Object     key   = parent.keys().get(i);
            EntryGroup group = (EntryGroup) parent.map.get(key);

            Element dataset = XmlUtil.create(doc,TAG_DATASET,datasetNode, new String[]{
                ATTR_NAME, group.key.toString()});

            for (int j = 0; j < group.children.size(); j++) {
                Object child = group.children.get(j);
                if (child instanceof EntryGroup) {
                    EntryGroup subGroup = (EntryGroup) child;
                    generate(request,  subGroup, doc, dataset);
                } else if (child instanceof Entry) {
                    Entry entry = (Entry) child;
                    entry.getTypeHandler().getDatasetTag(entry, request, doc, dataset);
                }
            }
        }
    }

}

