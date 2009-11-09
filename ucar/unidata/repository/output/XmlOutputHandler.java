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

package ucar.unidata.repository.output;


import org.w3c.dom.*;

import ucar.unidata.repository.*;
import ucar.unidata.repository.auth.*;
import ucar.unidata.repository.type.*;

import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;

import java.net.*;

import java.sql.ResultSet;
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
public class XmlOutputHandler extends OutputHandler {

    /** _more_ */
    public static final OutputType OUTPUT_XML =
        new OutputType("XML", "xml.xml",
                       OutputType.TYPE_NONHTML | OutputType.TYPE_FORSEARCH,
                       "", ICON_XML);


    /** _more_ */
    public static final OutputType OUTPUT_XMLENTRY =
        new OutputType("XML Entry", "xml.xmlentry",
                       OutputType.TYPE_NONHTML | OutputType.TYPE_FORSEARCH,
                       "", ICON_XML);



    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public XmlOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_XML);
        addType(OUTPUT_XMLENTRY);
    }










    /**
     * _more_
     *
     * @param request _more_
     * @param typeHandlers _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result listTypes(Request request, List<TypeHandler> typeHandlers)
            throws Exception {
        StringBuffer sb     = new StringBuffer();
        OutputType   output = request.getOutput();
        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(TAG_TYPES));
        for (TypeHandler theTypeHandler : typeHandlers) {
            sb.append(XmlUtil.tag(TAG_TYPE,
                                  XmlUtil.attrs(ATTR_TYPE,
                                      theTypeHandler.getType())));
        }
        sb.append(XmlUtil.closeTag(TAG_TYPES));
        return new Result("", sb, repository.getMimeTypeFromSuffix(".xml"));
    }



    /*
    protected Result listTags(Request request, List<Tag> tags)
            throws Exception {
        StringBuffer sb     = new StringBuffer();
        OutputType       output = request.getOutput();
        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(TAG_TAGS));
        request.remove(ARG_OUTPUT);
        int max = -1;
        int min = -1;

        for (Tag tag : tags) {
            if ((max < 0) || (tag.getCount() > max)) {
                max = tag.getCount();
            }
            if ((min < 0) || (tag.getCount() < min)) {
                min = tag.getCount();
            }
        }

        int    diff         = max - min;
        double distribution = diff / 5.0;

        for (Tag tag : tags) {
            sb.append(XmlUtil.tag(TAG_TAG,
                                  XmlUtil.attrs(ATTR_NAME, tag.getName())));
        }

        String pageTitle = "";
        sb.append(XmlUtil.closeTag(TAG_TAGS));
        Result result = new Result(pageTitle, sb, getMimeType(output));
        return result;
    }
    */



    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_XML) || output.equals(OUTPUT_XMLENTRY)) {
            return repository.getMimeTypeFromSuffix(".xml");
        }
        return super.getMimeType(output);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, Entry entry) throws Exception {
        Document     doc  = XmlUtil.makeDocument();
        Element      root = getEntryTag(request, entry, doc, null);
        StringBuffer sb   = new StringBuffer(XmlUtil.toString(root));
        return new Result("", sb, repository.getMimeTypeFromSuffix(".xml"));
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
        OutputType output = request.getOutput();

        if (output.equals(OUTPUT_XMLENTRY)) {
            return outputEntry(request, group);
        }

        Document doc  = XmlUtil.makeDocument();
        Element  root = getGroupTag(request, group, doc, null);
        for (Group subgroup : subGroups) {
            getGroupTag(request, subgroup, doc, root);
        }
        for (Entry entry : entries) {
            getEntryTag(request, entry, doc, root);
        }
        StringBuffer sb = new StringBuffer(XmlUtil.toString(root));
        return new Result("", sb, repository.getMimeTypeFromSuffix(".xml"));
    }




    /**
     * _more_
     *
     * @param entry _more_
     * @param doc _more_
     * @param parent _more_
     *
     * @throws Exception _more_
     */
    private void addMetadata(Entry entry, Document doc, Element parent)
            throws Exception {}


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param doc _more_
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Element getEntryTag(Request request, Entry entry, Document doc,
                                Element parent)
            throws Exception {
        Element node = XmlUtil.create(doc, TAG_ENTRY, parent, new String[] {
            ATTR_ID, entry.getId(), ATTR_NAME, entry.getName(), ATTR_GROUP,
            entry.getParentGroupId(), ATTR_TYPE,
            entry.getTypeHandler().getType(), ATTR_ISGROUP,
            "" + entry.isGroup(), ATTR_FROMDATE,
            getRepository().formatDate(new Date(entry.getStartDate())),
            ATTR_TODATE,
            getRepository().formatDate(new Date(entry.getEndDate())),
            ATTR_CREATEDATE,
            getRepository().formatDate(new Date(entry.getCreateDate()))
        });


        if (entry.hasNorth()) {
            node.setAttribute(ATTR_NORTH, "" + entry.getNorth());
        }
        if (entry.hasSouth()) {
            node.setAttribute(ATTR_SOUTH, "" + entry.getSouth());
        }
        if (entry.hasEast()) {
            node.setAttribute(ATTR_EAST, "" + entry.getEast());
        }
        if (entry.hasWest()) {
            node.setAttribute(ATTR_WEST, "" + entry.getWest());
        }

        if ( !entry.isGroup() && entry.getResource().isDefined()) {
            XmlUtil.setAttributes(node, new String[] { ATTR_RESOURCE,
                    entry.getResource().getPath(), ATTR_RESOURCE_TYPE,
                    entry.getResource().getType() });

            //Add the service nodes
            for (OutputHandler outputHandler : getRepository()
                    .getOutputHandlers()) {
                outputHandler.addToEntryNode(request, entry, node);
            }

            if (getRepository().getAccessManager().canAccessFile(request,
                    entry)) {
                node.setAttribute(ATTR_FILESIZE,
                                  "" + entry.getResource().getFileSize());
                String url =
                    getRepository().getEntryManager().getEntryResourceUrl(
                        request, entry, true);
                Element serviceNode = XmlUtil.create(TAG_SERVICE, node);
                XmlUtil.setAttributes(serviceNode, new String[] { ATTR_TYPE,
                        SERVICE_FILE, ATTR_URL, url });
            }
        }


        if ((entry.getDescription() != null)
                && (entry.getDescription().length() > 0)) {
            XmlUtil.create(doc, TAG_DESCRIPTION, node,
                           entry.getDescription(), null);
        }
        addMetadata(entry, doc, node);
        return node;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param group _more_
     * @param doc _more_
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Element getGroupTag(Request request, Group group, Document doc,
                                Element parent)
            throws Exception {
        Element node = getEntryTag(request, group, doc, parent);
        boolean canDoNew = getAccessManager().canDoAction(request, group,
                               Permission.ACTION_NEW);
        boolean canDoUpload = getAccessManager().canDoAction(request, group,
                                  Permission.ACTION_UPLOAD);
        node.setAttribute(ATTR_CANDONEW, "" + canDoNew);
        node.setAttribute(ATTR_CANDOUPLOAD, "" + canDoUpload);
        return node;

    }


}

