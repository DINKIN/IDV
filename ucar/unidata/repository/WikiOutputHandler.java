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


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.WikiUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.util.regex.*;
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
public class WikiOutputHandler extends OutputHandler implements WikiUtil.WikiPageHandler {



    /** _more_ */
    public static final OutputType OUTPUT_WIKI = new OutputType("Wiki",
                                                                "wiki",true);


    public static final String PROP_ENTRY = "entry";
    public static final String PROP_REQUEST = "request";


    public static final String WIKIPROP_IMPORT = "import";

    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public WikiOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_WIKI);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param state _more_
     * @param types _more_
     *
     *
     * @throws Exception _more_
     */
    protected void addOutputTypes(Request request, State state,
                                  List<OutputType> types)
            throws Exception {

        if (state.entry == null) {
            return;
        }
        if(state.entry.getType().equals(WikiPageTypeHandler.TYPE_WIKIPAGE)) {
            types.add(OUTPUT_WIKI);
        }
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


        String desc = entry.getDescription();
        StringBuffer sb  = new StringBuffer();

        if(request.exists(ARG_WIKI_CREATE)) {
            //            return wikiPageCreate(request, entry);
        }

        WikiUtil wikiUtil = new WikiUtil(Misc.newHashtable(new Object[]{PROP_REQUEST,request,PROP_ENTRY,entry}));
        sb.append(wikiUtil.wikify(desc,this));
        return makeLinksResult(request, msg("Wiki"), sb, new State(entry));
    }

    public Result wikiPageCreate(Request request, Entry entry) throws Exception {
        return null;
    }

    public Result outputGroup(Request request, Group group,
                              List<Group> subGroups, List<Entry> entries)
            throws Exception {
        return outputEntry(request, group);
    }

    
    public String makeShowHideBlock(WikiUtil wikiUtil, String title, String contents) {
        return Repository.makeShowHideBlock(title, new StringBuffer(contents),
                                            true," class=\"wiki=tocheader\"",
                                            " class=\"wiki-toc\" ");
    }

    public String getPropertyValue(WikiUtil wikiUtil, String property) {
        property  =property.trim();
        if(property.startsWith(WIKIPROP_IMPORT+":")) {
            return handleImport(wikiUtil,property.substring(WIKIPROP_IMPORT.length()+1));
        }
        return wikiUtil.getPropertyValue(property);
    }

    public String handleImport(WikiUtil wikiUtil, String property) {
        try {
            //{{import:the id|output type}}
            Entry entry = (Entry) wikiUtil.getProperty(PROP_ENTRY);
            Request request = (Request) wikiUtil.getProperty(PROP_REQUEST);
            Group parent = entry.getParentGroup();
            OutputType outputType = OutputHandler.OUTPUT_HTML;
            String entryName = property;
            String []pair  = StringUtil.split(property,"|",2);
            if(pair !=null) {
                entryName = pair[0].trim();
                outputType = new OutputType(pair[1].trim(),false);
            }

            //Check for metadata, header, etc.
            if(outputType.equals("metadata")) {
                
            }

            OutputHandler handler = getRepository().getOutputHandler(outputType);
            if(handler == null) {
                return "Error:Could not find output handler for type: " + outputType;
            }
            outputType = handler.findOutputType(outputType.getId());

            Entry importEntry = findEntry(request, entryName,parent);
            if(importEntry==null) {
                return "Error:Could not find entry: " + entryName;
            }

            if(importEntry.equals(entry)) {
                return "Error:Circular wiki reference: " + entryName;
            }


            String originalOutput = request.getString(ARG_OUTPUT, (String) "");
            String originalId = request.getString(ARG_ENTRYID, (String) "");
            request.put(ARG_ENTRYID, importEntry.getId());
            request.put(ARG_OUTPUT, outputType.getId());
            request.put(ARG_EMBEDDED,"true");

            String propertyValue;
            if(!outputType.getIsHtml()) {
                String url =  request.entryUrl(getRepository().URL_ENTRY_SHOW,importEntry, ARG_OUTPUT, outputType.getId());
                String label  = importEntry.getName() +" - " + outputType.getLabel();
                propertyValue =  getEntryManager().getAjaxLink(request, importEntry,
                                                     label,url, false);
            } else {
                Result result = getEntryManager().processEntryShow(request, importEntry);
                propertyValue =new String(result.getContent());
            }

            request.put(ARG_OUTPUT, originalOutput);
            request.put(ARG_ENTRYID, originalId);
            request.remove(ARG_EMBEDDED);
            return propertyValue;
        } catch(Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    public Entry findEntry(Request request, String name,Group parent) throws Exception {
        name = name.trim();
        Entry theEntry=null;
        theEntry = getEntryManager().getEntry(request, name);
        if(theEntry == null) {
            for(Entry child:  getEntryManager().getChildren(request, parent)) {
                if(child.getName().equals(name)) {
                    theEntry = child;
                    break;
                }
            }
        }
        return theEntry;
    }

    public String makeLink(WikiUtil wikiUtil, String name, String label) {
        try {
        Entry entry = (Entry) wikiUtil.getProperty(PROP_ENTRY);
        Request request = (Request) wikiUtil.getProperty(PROP_REQUEST);
        Group parent = entry.getParentGroup();
        Entry theEntry=findEntry(request, name,parent);

        if(theEntry != null) {
            if(label.trim().length()==0) label = theEntry.getName();
            if(theEntry.getType().equals(WikiPageTypeHandler.TYPE_WIKIPAGE)) {
                String url = request.entryUrl(getRepository().URL_ENTRY_SHOW, theEntry, ARG_OUTPUT,OUTPUT_WIKI);
                return getEntryManager().getAjaxLink(request, theEntry,
                                                     label,url, false);

            } else {
                return getEntryManager().getAjaxLink(request, theEntry,
                                                     label,false);
            }
        }

        String url = request.entryUrl(getRepository().URL_ENTRY_SHOW, entry, ARG_OUTPUT,OUTPUT_WIKI,ARG_WIKI_CREATE, name);
        return HtmlUtil.href(url,name,HtmlUtil.cssClass("wiki-link-noexist"));
        } catch(Exception exc) {
            throw new RuntimeException(exc);
        }
    }

}

