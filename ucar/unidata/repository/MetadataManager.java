/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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



import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.sql.Clause;

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.geoloc.NavigatedMapPanel;
import ucar.unidata.xml.XmlUtil;


import java.awt.*;
import java.awt.Image;

import java.io.*;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
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
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import java.util.regex.*;
import java.util.zip.*;


import javax.swing.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class MetadataManager extends RepositoryManager {

    /** _more_ */
    private static final String SUFFIX_SELECT = ".select.";


    /** _more_ */
    private Object MUTEX_METADATA = new Object();


    /** _more_ */
    public RequestUrl URL_METADATA_FORM = new RequestUrl(getRepository(),
                                              "/metadata/form",
                                              "Edit Metadata");

    /** _more_ */
    public RequestUrl URL_METADATA_ADDFORM = new RequestUrl(getRepository(),
                                                 "/metadata/addform",
                                                 "Add Metadata");

    /** _more_ */
    public RequestUrl URL_METADATA_ADD = new RequestUrl(getRepository(),
                                             "/metadata/add");

    /** _more_ */
    public RequestUrl URL_METADATA_CHANGE = new RequestUrl(getRepository(),
                                                "/metadata/change");




    /** _more_ */
    protected Hashtable distinctMap = new Hashtable();

    /** _more_ */
    private List<MetadataHandler> metadataHandlers =
        new ArrayList<MetadataHandler>();




    /**
     * _more_
     *
     *
     * @param repository _more_
     *
     */
    public MetadataManager(Repository repository) {
        super(repository);
    }


    /** _more_ */
    MetadataHandler dfltMetadataHandler;


    /**
     * _more_
     *
     * @param entry _more_
     * @param type _more_
     * @param checkInherited _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Metadata findMetadata(Entry entry, Metadata.Type type,
                                 boolean checkInherited)
            throws Exception {
        if (entry == null) {
            return null;
        }
        for (Metadata metadata : getMetadata(entry)) {
            if (metadata.getType().equals(type.getType())) {
                return metadata;
            }
        }
        if (checkInherited) {
            return findMetadata(entry.getParentGroup(), type, checkInherited);
        }
        return null;
    }


    /**
     * _more_
     *
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Metadata> getMetadata(Entry entry) throws Exception {
        if (entry.isDummy()) {
            return new ArrayList<Metadata>();
        }
        List<Metadata> metadataList = entry.getMetadata();
        if (metadataList != null) {
            return metadataList;
        }


        Statement stmt = getDatabaseManager().select(COLUMNS_METADATA,
                             TABLE_METADATA,
                             Clause.eq(COL_METADATA_ENTRY_ID, entry.getId()),
                             " order by " + COL_METADATA_TYPE);
        SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
        ResultSet        results;
        metadataList = new ArrayList();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int             col     = 1;
                String          type    = results.getString(3);
                MetadataHandler handler = findMetadataHandler(type);
                metadataList.add(
                    handler.makeMetadata(
                        results.getString(col++), results.getString(col++),
                        results.getString(col++), results.getInt(col++)==1,results.getString(col++),
                        results.getString(col++), results.getString(col++),
                        results.getString(col++)));
            }
        }
        entry.setMetadata(metadataList);
        return metadataList;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    protected List<MetadataHandler> getMetadataHandlers() {
        return metadataHandlers;
    }




    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public MetadataHandler findMetadataHandler(Metadata metadata)
            throws Exception {
        for (MetadataHandler handler : metadataHandlers) {
            if (handler.canHandle(metadata)) {
                return handler;
            }
        }
        if (dfltMetadataHandler == null) {
            dfltMetadataHandler = new MetadataHandler(getRepository(), null);
        }
        return dfltMetadataHandler;
    }



    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public MetadataHandler findMetadataHandler(String type) throws Exception {
        for (MetadataHandler handler : metadataHandlers) {
            if (handler.canHandle(type)) {
                return handler;
            }
        }
        if (dfltMetadataHandler == null) {
            dfltMetadataHandler = new MetadataHandler(getRepository(), null);
        }
        return dfltMetadataHandler;
    }


    /**
     * _more_
     *
     *
     * @param metadataDefFiles _more_
     * @throws Exception _more_
     */
    protected void initMetadataHandlers(List<String> metadataDefFiles)
            throws Exception {
        for (String file : metadataDefFiles) {
            try {
                file = getStorageManager().localizePath(file);
                Element root = XmlUtil.getRoot(file, getClass());
                if (root == null) {
                    continue;
                }
                List children = XmlUtil.findChildren(root,
                                    TAG_METADATAHANDLER);
                for (int i = 0; i < children.size(); i++) {
                    Element node = (Element) children.get(i);
                    Class c = Misc.findClass(XmlUtil.getAttribute(node,
                                  ATTR_CLASS));
                    Constructor ctor = Misc.findConstructor(c,
                                           new Class[] { Repository.class,
                            Element.class });
                    metadataHandlers.add(
                        (MetadataHandler) ctor.newInstance(
                            new Object[] { getRepository(),
                                           node }));
                }
            } catch (Exception exc) {
                getRepository().log("Error loading metadata handler file:"
                                    + file, exc);
                throw exc;
            }

        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public StringBuffer addToSearchForm(Request request, StringBuffer sb)
            throws Exception {
        for (MetadataHandler handler : metadataHandlers) {
            handler.addToSearchForm(request, sb);
        }
        return sb;
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processMetadataChange(Request request) throws Exception {
        synchronized (MUTEX_METADATA) {
            Entry entry = getRepository().getEntry(request);

            if (request.exists(ARG_DELETE)) {
                Hashtable args = request.getArgs();
                for (Enumeration keys =
                        args.keys(); keys.hasMoreElements(); ) {
                    String arg = (String) keys.nextElement();
                    if ( !arg.startsWith(ARG_METADATA_ID + SUFFIX_SELECT)) {
                        continue;
                    }
                    SqlUtil.delete(getConnection(), TABLE_METADATA,
                                   Clause.eq(COL_METADATA_ID,
                                             request.getString(arg, BLANK)));
                }
            } else {
                List<Metadata> newMetadata = new ArrayList<Metadata>();
                for (MetadataHandler handler : metadataHandlers) {
                    handler.handleFormSubmit(request, entry, newMetadata);
                }

                for (Metadata metadata : newMetadata) {
                    SqlUtil.delete(getConnection(), TABLE_METADATA,
                                   Clause.eq(COL_METADATA_ID,
                                             metadata.getId()));
                    insertMetadata(metadata);
                }
            }
            entry.setMetadata(null);
            return new Result(request.url(URL_METADATA_FORM, ARG_ID,
                                          entry.getId()));
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processMetadataForm(Request request) throws Exception {
        StringBuffer sb    = new StringBuffer();
        Entry        entry = getRepository().getEntry(request);

        sb.append(getRepository().makeEntryHeader(request, entry));

        List<Metadata> metadataList = getMetadata(entry);
        sb.append(HtmlUtil.p());
        if (metadataList.size() == 0) {
            sb.append(
                getRepository().note(msg("No metadata defined for entry")));
        } else {
            sb.append(HtmlUtil.formPost(request.url(URL_METADATA_CHANGE)));
            sb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
            sb.append(HtmlUtil.submit(msg("Change")));
            sb.append(HtmlUtil.space(2));
            sb.append(HtmlUtil.submit(msg("Delete selected"), ARG_DELETE));
            sb.append(HtmlUtil.formTable());
            for (Metadata metadata : metadataList) {
                metadata.setEntry(entry);
                MetadataHandler metadataHandler =
                    findMetadataHandler(metadata);
                if (metadataHandler == null) {
                    continue;
                }
                String[] html = metadataHandler.getForm(request, metadata,
                                    true);
                if (html == null) {
                    continue;
                }
                String cbx = HtmlUtil.checkbox(ARG_METADATA_ID
                                 + SUFFIX_SELECT
                                 + metadata.getId(), metadata.getId(), false);
                sb.append(HtmlUtil.rowTop(HtmlUtil.cols(cbx + html[0],
                        html[1])));
            }
            sb.append(HtmlUtil.formTableClose());
            sb.append(HtmlUtil.submit(msg("Change")));
            sb.append(HtmlUtil.space(2));
            sb.append(HtmlUtil.submit(msg("Delete Selected"), ARG_DELETE));
            sb.append(HtmlUtil.formClose());
        }

        return getRepository().makeEntryEditResult(request, entry,
                msg("Edit Metadata"), sb);

    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processMetadataAddForm(Request request) throws Exception {
        StringBuffer sb    = new StringBuffer();
        Entry        entry = getRepository().getEntry(request);
        sb.append(getRepository().makeEntryHeader(request, entry));
        sb.append(HtmlUtil.p());
        if ( !request.exists(ARG_TYPE)) {
            List<String> groups   = new ArrayList<String>();
            Hashtable    groupMap = new Hashtable();

            for (MetadataHandler handler : metadataHandlers) {
                String       name    = handler.getHandlerGroupName();
                StringBuffer groupSB = null;
                for (Metadata.Type type : handler.getTypes(request, entry)) {
                    if (groupSB == null) {
                        groupSB = (StringBuffer) groupMap.get(name);
                        if (groupSB == null) {
                            groupMap.put(name, groupSB = new StringBuffer());
                            groups.add(name);
                        }
                    }
                    groupSB.append(request.form(URL_METADATA_ADDFORM));
                    groupSB.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
                    groupSB.append(HtmlUtil.hidden(ARG_TYPE, type.getType()));
                    groupSB.append(HtmlUtil.submit(msg("Add")));
                    groupSB.append(HtmlUtil.space(1)
                                   + HtmlUtil.bold(type.getLabel()));
                    groupSB.append(HtmlUtil.formClose());
                    groupSB.append(HtmlUtil.p());
                    groupSB.append(NEWLINE);
                }
            }
            for (String name : groups) {
                sb.append(header(name));
                sb.append("<ul>");
                sb.append(groupMap.get(name));
                sb.append("</ul>");
            }
        } else {
            String type = request.getString(ARG_TYPE, BLANK);
            sb.append(HtmlUtil.formTable());
            for (MetadataHandler handler : metadataHandlers) {
                if (handler.canHandle(type)) {
                    handler.makeAddForm(request, entry,
                                        handler.findType(type), sb);
                    break;
                }
            }
            sb.append(HtmlUtil.formTableClose());
        }
        return getRepository().makeEntryEditResult(request, entry,
                msg("Add Metadata"), sb);
    }





    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processMetadataAdd(Request request) throws Exception {
        synchronized (MUTEX_METADATA) {
            Entry entry = getRepository().getEntry(request);
            if (request.exists(ARG_CANCEL)) {
                return new Result(request.url(URL_METADATA_ADDFORM, ARG_ID,
                        entry.getId()));
            }
            List<Metadata> newMetadata = new ArrayList<Metadata>();
            for (MetadataHandler handler : metadataHandlers) {
                handler.handleAddSubmit(request, entry, newMetadata);
            }

            for (Metadata metadata : newMetadata) {
                insertMetadata(metadata);
            }
            entry.setMetadata(null);
            return new Result(request.url(URL_METADATA_FORM, ARG_ID,
                                          entry.getId()));

        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param handler _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getDistinctValues(Request request,
                                      MetadataHandler handler,
                                      Metadata.Type type)
            throws Exception {
        if (distinctMap == null) {
            distinctMap = new Hashtable();
        }
        String[] values = (String[]) distinctMap.get(type.getType());

        if (values == null) {
            Statement stmt = getDatabaseManager().select(
                                 SqlUtil.distinct(COL_METADATA_ATTR1),
                                 TABLE_METADATA,
                                 Clause.eq(
                                     COL_METADATA_TYPE, type.getType()));
            values = SqlUtil.readString(stmt, 1);
            distinctMap.put(type.getType(), values);
        }
        return values;
    }


    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @throws Exception _more_
     */
    public void insertMetadata(Metadata metadata) throws Exception {
        distinctMap = null;
        getDatabaseManager().executeInsert(INSERT_METADATA, new Object[] {
            metadata.getId(), metadata.getEntryId(), metadata.getType(),
            new Integer(metadata.getInherited()?1:0),
            metadata.getAttr1(), metadata.getAttr2(), metadata.getAttr3(),
            metadata.getAttr4()
        });
    }




}

