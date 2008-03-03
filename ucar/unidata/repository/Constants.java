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


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

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


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public interface Constants {

    /** _more_ */
    public static final int MAX_ROWS = 100;


    /** _more_ */
    public static final String ICON_CART = "/cart.gif";

    /** _more_ */
    public static final String ICON_COMMENTS = "/comments.gif";

    public static final String ICON_PLUS = "/plus.gif";

    public static final String ICON_MINUS = "/minus.gif";

    /** _more_ */
    public static final String ICON_DELETE = "/delete.gif";

    /** _more_ */
    public static final String ICON_FETCH = "/fetch.gif";

    /** _more_ */
    public static final String ICON_SEARCH = "/search.gif";

    /** _more_ */
    public static final String ICON_RIGHT = "/right.gif";

    /** _more_ */
    public static final String ICON_ASSOCIATION = "/association.gif";

    /** _more_ */
    public static final String ICON_GRAPH = "/graph.gif";

    /** _more_ */
    public static final String ICON_EDIT = "/edit.gif";

    /** _more_ */
    public static final String ICON_NEW = "/new.gif";

    /** _more_ */
    public static final String ICON_ARROW = "/arrow.gif";


    /** _more_ */
    public static final String ICON_BLANK = "/blank.gif";

    /** _more_ */
    public static final String ICON_LEFT = "/left.gif";


    /** _more_ */
    public static final String ATTR_ID = "id";


    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_TYPE = "type";


    /** _more_ */
    public static final String TAG_NODE = "node";

    /** _more_ */
    public static final String TAG_EDGE = "edge";

    /** _more_ */
    public static final String TAG_TAG = "tag";

    /** _more_ */
    public static final String TAG_ASSOCIATION = "association";

    /** _more_ */
    public static final String TAG_TAGS = "tags";

    /** _more_ */
    public static final String TAG_ASSOCIATIONS = "associations";


    /** _more_ */
    public static final String ATTR_RSS_VERSION = "version";

    /** _more_ */
    public static final String TAG_RSS_RSS = "rss";

    /** _more_ */
    public static final String TAG_RSS_LINK = "link";

    /** _more_ */
    public static final String TAG_RSS_GUID = "guid";

    /** _more_ */
    public static final String TAG_RSS_CHANNEL = "channel";

    /** _more_ */
    public static final String TAG_RSS_ITEM = "item";

    /** _more_ */
    public static final String TAG_RSS_TITLE = "title";

    /** _more_ */
    public static final String TAG_RSS_PUBDATE = "pubDate";

    /** _more_ */
    public static final String TAG_RSS_DESCRIPTION = "description";


    /** _more_ */
    public static final String ATTR_FROM = "from";

    /** _more_ */
    public static final String ATTR_TO = "to";


    /** _more_ */
    public static final String ATTR_TITLE = "title";

    /** _more_ */
    public static final String ATTR_TOOLTIP = "tooltip";



    /** _more_ */
    public static final String TAG_TYPES = "types";

    /** _more_ */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String ARG_TYPE = "type";

    /** _more_ */
    public static final String ARG_TYPE_EXCLUDE_GROUP = "excludegroup";



    /** _more_ */
    public static final String ARG_RECURSE = "recurse";

    /** _more_ */
    public static final String ARG_REDIRECT = "redirect";

    /** _more_ */
    public static final String ARG_CATALOG = "catalog";

    /** _more_ */
    public static final String ARG_REQUIRED = "required";

    /** _more_ */
    public static final String ARG_COMMENT = "comment";

    /** _more_ */
    public static final String ARG_SUBJECT = "subject";



    /** _more_ */
    public static final String ARG_SOUTH = "south";

    /** _more_ */
    public static final String ARG_NORTH = "north";

    /** _more_ */
    public static final String ARG_EAST = "east";

    /** _more_ */
    public static final String ARG_WEST = "west";

    /** _more_ */
    public static final String ARG_WAIT = "wait";

    /** _more_ */
    public static final String ARG_SKIP = "skip";

    /** _more_ */
    public static final String ARG_ORDERBY = "orderby";

    /** _more_ */
    public static final String ARG_ASCENDING = "ascending";

    /** _more_ */
    public static final String ARG_AREA = "area";

    /** _more_ */
    public static final String ARG_DATE = "date";

    /** _more_ */
    public static final String ARG_DATE_PATTERN = "date.pattern";

    /** _more_ */
    public static final String ARG_FORM_TYPE = "form.type";

    /** _more_ */
    public static final String ARG_FORM_ADVANCED = "form.advanced";

    /** _more_ */
    public static final String ARG_FORM_METADATA = "form.metadata";

    /** _more_ */
    public static final String ARG_CHANGE = "change";

    /** _more_ */
    public static final String ARG_ADMIN = "admin";



    /** _more_ */
    public static final String ARG_DELETE = "delete";

    /** _more_ */
    public static final String ARG_ADD = "add";

    /** _more_ */
    public static final String ARG_NEW = "new";

    /** _more_ */
    public static final String ARG_DELETE_CONFIRM = "delete.confirm";

    /** _more_ */
    public static final String ARG_CANCEL = "cancel";

    public static final String ARG_CANCEL_DELETE = "canceldelete";

    /** _more_ */
    public static final String ARG_MESSAGE = "message";

    /** _more_ */
    public static final String ARG_INCLUDENONGEO = "includenongeo";


    /** _more_ */
    public static final String ARG_NEXT = "next";

    /** _more_ */
    public static final String ARG_PREVIOUS = "previous";

    /** _more_ */
    public static final String ARG_APPLET = "applet";


    /** _more_ */
    public static final String ARG_AUTH_USER = "auth.user";

    /** _more_ */
    public static final String ARG_AUTH_PASSWORD = "auth.password";


    /** _more_ */
    public static final String ARG_USER_ID = "user.id";

    /** _more_ */
    public static final String ARG_ASSOCIATION = "association";

    /** _more_ */
    public static final String ARG_WHAT = "what";

    /** _more_ */
    public static final String ARG_ACTION_ID = "actionid";


    /** _more_ */
    public static final String ARG_ACTION = "action";

    /** _more_ */
    public static final String ARG_ROLES = "roles";

    /** _more_ */
    public static final String WHAT_ENTRIES = "entries";



    /** _more_ */
    public static final String WHAT_TYPE = "type";

    /** _more_ */
    public static final String WHAT_TAG = "tag";


    /** _more_ */
    public static final String WHAT_METADATA = "metadata";

    /** _more_ */
    public static final String WHAT_ASSOCIATION = "association";

    /** _more_ */
    public static final String WHAT_USER = "user";


    /** _more_ */
    public static final String ARG_MAX = "max";

    /** _more_ */
    public static final String ARG_SHOWMETADATA = "showmetadata";

    /** _more_ */
    public static final String ARG_COMMENTS = "showcomments";

    /** _more_ */
    public static final String ARG_OUTPUT = "output";

    /** _more_ */
    public static final String ARG_NAME = "name";

    /** _more_ */
    public static final String ARG_SEARCHMETADATA = "searchmetadata";

    /** _more_ */
    public static final String ARG_ID = "id";

    /** _more_ */
    public static final String ARG_METADATA_ID = "metadata.id";

    /** _more_ */
    public static final String ARG_METADATA_INHERITED = "metadata.inherited";

    /** _more_ */
    public static final String ARG_METADATA_ATTR1 = "metadata.attr1";

    /** _more_ */
    public static final String ARG_METADATA_ATTR2 = "metadata.attr2";

    /** _more_ */
    public static final String ARG_METADATA_ATTR3 = "metadata.attr3";

    /** _more_ */
    public static final String ARG_METADATA_ATTR4 = "metadata.attr4";

    /** _more_ */
    public static final String ARG_METADATA_TYPE = "metadatatype";

    /** _more_ */
    public static final String ARG_EDIT_METADATA = "edit.metadata";

    /** _more_ */
    public static final String ARG_COMMENT_ID = "comment_id";

    /** _more_ */
    public static final String ARG_FROM = "from";

    /** _more_ */
    public static final String ARG_TO = "to";


    /** _more_ */
    public static final String ARG_IDS = "ids";

    /** _more_ */
    public static final String ARG_GROUP = "group";

    /** _more_ */
    public static final String ARG_GROUPID = "groupid";

    /** _more_ */
    public static final String ARG_GROUP_CHILDREN = "group_children";

    /** _more_ */
    public static final String ARG_CREATEDATE = "createdate";

    /** _more_ */
    public static final String ARG_QUERY = "query";

    /** _more_ */
    public static final String ARG_TODATE = "todate";

    /** _more_ */
    public static final String ARG_RELATIVEDATE = "relativedate";

    /** _more_ */
    public static final String ARG_FROMDATE = "fromdate";

    /** _more_ */
    public static final String ARG_PRODUCT = "product";

    /** _more_ */
    public static final String ARG_STATION = "station";




    /** _more_ */
    public static final String TYPE_TAG = "tag";

    /** _more_ */
    public static final String TYPE_ASSOCIATION = "association";

    /** _more_ */
    public static final String TYPE_GROUP = "group";



    /** _more_ */
    public static final String PROP_DOWNLOAD_OK = "jdms.download.ok";

    /** _more_ */
    public static final String PROP_REPOSITORY_HOME = "jdms.home";


    /** _more_ */
    public static final String PROP_DEBUG = "jdms.debug";

    /** _more_ */
    public static final String PROP_DATEFORMAT = "jdms.dateformat";


    /** _more_ */
    public static final String PROP_ACCESS_REQUIRELOGIN =
        "jdms.access.requirelogin";

    /** _more_ */
    public static final String PROP_ACCESS_ADMINONLY =
        "jdms.access.adminonly";

    /** _more_ */
    public static final String PROP_LANGUAGE = "jdms.language";


    /** _more_ */
    public static final String PROP_HTML_FOOTER = "jdms.html.footer";

    /** _more_ */
    public static final String PROP_REQUEST_PATTERN = "jdms.request.pattern";

    /** _more_ */
    public static final String PROP_REPOSITORY_NAME = "jdms.repository.name";


    /** _more_ */
    public static final String PROP_DB_CANCACHE = "jdms.db.cancache";

    /** _more_ */
    public static final String PROP_DB = "jdms.db";


    /** _more_ */
    public static final String PROP_DB_DERBY_HOME = "jdms.db.derby.home";

    /** _more_ */
    public static final String PROP_DB_DRIVER = "jdms.db.${db}.driver";

    /** _more_ */
    public static final String PROP_DB_URL = "jdms.db.${db}.url";

    /** _more_ */
    public static final String PROP_DB_USER = "jdms.db.${db}.user";

    /** _more_ */
    public static final String PROP_DB_PASSWORD = "jdms.db.${db}.password";





    /** _more_ */
    public static final String PROP_HARVESTERS_ACTIVE =
        "jdms.harvesters.active";

    /** _more_ */
    public static final String PROP_HARVESTERS = "jdms.harvesters";

    /** _more_ */
    public static final String PROP_DB_SCRIPT = "jdms.db.script";

    /** _more_ */
    public static final String PROP_TYPES = "jdms.types";

    /** _more_ */
    public static final String PROP_OUTPUTHANDLERS = "jdms.outputhandlers";

    /** _more_ */
    public static final String PROP_METADATAHANDLERS =
        "jdms.metadatahandlers";

    /** _more_ */
    public static final String PROP_NAVLINKS = "jdms.navlinks";

    /** _more_ */
    public static final String PROP_API = "jdms.api";



    /** _more_ */
    public static final String PROP_NAVSUBLINKS = "jdms.navsublinks";

    /** _more_ */
    public static final String PROP_SHOW_APPLET = "jdms.html.showapplet";

    /** _more_ */
    public static final String PROP_HTML_URLBASE = "jdms.html.urlbase";

    /** _more_ */
    public static final String PROP_HTML_TEMPLATE = "jdms.html.template";

    /** _more_ */
    public static final String PROP_HTML_IMAGEPLAYER =
        "jdms.html.imageplayer";

    /** _more_ */
    public static final String PROP_HTML_SLIDESHOW = "jdms.html.slideshow";

    /** _more_ */
    public static final String PROP_HTML_TIMELINEAPPLET =
        "jdms.html.timelineapplet";

    /** _more_ */
    public static final String PROP_HTML_MIMEPROPERTIES =
        "jdms.html.mimeproperties";

    /** _more_ */
    public static final String PROP_HTML_GRAPHAPPLET =
        "jdms.html.graphapplet";

    /** _more_ */
    public static final String PROP_HTML_GRAPHTEMPLATE =
        "jdms.html.graphtemplate";



    /** _more_ */
    public static final String PROP_DOWNLOAD_ASFILES =
        "jdms.download.asfiles";




    /** _more_ */
    public static final String TAG_OUTPUTHANDLER = "outputhandler";

    /** _more_ */
    public static final String TAG_METADATAHANDLER = "metadatahandler";


    /** _more_ */
    public static final String ATTR_DB_NAME = "name";

    /** _more_ */
    public static final String ATTR_CLASS = "class";

    /** _more_ */
    public static final String ATTR_DB_DESCRIPTION = "description";





    /** _more_ */
    public static final String ARG_NODETYPE = "nodetype";

    /** _more_ */
    public static final String ARG_IMAGEWIDTH = "imagewidth";

    /** _more_ */
    public static final String ARG_IMAGEHEIGHT = "imageheight";

    /** _more_ */
    public static final String NODETYPE_ENTRY = "entry";

    /** _more_ */
    public static final String NODETYPE_GROUP = "group";

    /** _more_ */
    public static final String TAG_CATALOGREF = "catalogRef";

    /** _more_ */
    public static final String ATTR_XLINKTITLE = "xlink:title";

    /** _more_ */
    public static final String ATTR_XLINKHREF = "xlink:href";


    /** _more_ */
    public static final String ARG_WIDTH = "width";

    /** _more_ */
    public static final String ARG_RESOURCE = "resource";

    /** _more_ */
    public static final String ARG_FILE = "file";

    /** _more_ */
    public static final String ARG_FILE_UNZIP = "file.unzip";

    /** _more_ */
    public static final String ARG_DESCRIPTION = "description";


    /** _more_ */
    public static final String ARG_ADMIN_HAVECREATED = "admin.havecreated";


    /** _more_ */
    public static final String ARG_ADMIN_WHAT = "what";


    /** _more_ */
    public static final String ACTION_EDIT = "action.edit";
    public static final String ACTION_COPY = "action.copy";
    public static final String ACTION_MOVE = "action.move";

    /** _more_ */
    public static final String ACTION_CLEAR = "action.clear";

    /** _more_ */
    public static final String ACTION_DELETE_ASK = "action.delete.ask";

    /** _more_ */
    public static final String ACTION_DELETE_DOIT = "action.delete.doit";

    /** _more_ */
    public static final String ACTION_START = "action.start";

    /** _more_ */
    public static final String ACTION_STOP = "action.stop";

    /** _more_ */
    public static final String ACTION_ADD = "action.add";

    /** _more_ */
    public static final String ACTION_REMOVE = "action.remove";





    /** _more_ */
    public static final String NEWLINE = "\n";

    /** _more_ */
    public static final String BR = "<br>";

    /** _more_ */
    public static final String HR = "<hr>";

    /** _more_ */
    public static final String BLANK = "";





}

