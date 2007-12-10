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


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



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
public class Repository implements Constants, Tables {

    public static final String CATALOG_ATTRS = " xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ";

    /** _more_          */
    private static final int PAGE_CACHE_LIMIT = 100;

    /** _more_          */
    private Harvester harvester;

    /** _more_          */
    private Properties mimeTypes;

    /** _more_ */
    private Properties productMap;

    /** _more_          */
    private Properties properties = new Properties();

    /** _more_ */
    private String urlBase = "/repository";

    /** _more_ */
    private long baseTime = System.currentTimeMillis();

    /** _more_ */
    private int keyCnt = 0;


    /** _more_ */
    private Connection connection;

    /** _more_ */
    private Hashtable typeHandlersMap = new Hashtable();


    /** _more_ */
    private static String timelineAppletTemplate;

    /** _more_ */
    private static String graphXmlTemplate;

    /** _more_ */
    private static String graphAppletTemplate;


    /** _more_ */
    private Hashtable<String, Group> groupMap = new Hashtable<String,
                                                    Group>();

    /** _more_          */
    private Hashtable<String, User> userMap = new Hashtable<String, User>();



    /**
     * _more_
     *
     *
     *
     * @param args _more_
     * @throws Exception _more_
     */
    public Repository(String[] args) throws Exception {
        properties = new Properties();
        properties.load(
            IOUtil.getInputStream(
                "/ucar/unidata/repository/repository.properties",
                getClass()));
        for (int i = 0; i < args.length; i++) {
            if (args[i].endsWith(".properties")) {
                properties.load(IOUtil.getInputStream(args[i], getClass()));
            }
        }
        Misc.findClass((String) properties.get(PROP_DB_DRIVER));
        urlBase = (String) properties.get(PROP_HTML_URLBASE);
        if (urlBase == null) {
            urlBase = "";
        }
        harvester = new Harvester(this);
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void makeConnection() throws Exception {
        String userName      = (String) properties.get(PROP_DB_USER);
        String password      = (String) properties.get(PROP_DB_PASSWORD);
        String connectionURL = (String) properties.get(PROP_DB_URL);

        System.err.println("db:" + connectionURL);
        if (userName != null) {
            connection = DriverManager.getConnection(connectionURL, userName,
                    password);
        } else {
            connection = DriverManager.getConnection(connectionURL);
        }
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void init() throws Exception {
        makeConnection();
        mimeTypes = new Properties();
        mimeTypes.load(
            IOUtil.getInputStream(
                "/ucar/unidata/repository/mimetypes.properties", getClass()));
        initTable();
        initTypeHandlers();
        initGroups();
        initRequests();
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    protected boolean isAppletEnabled(Request request) {
        if ( !getProperty(PROP_SHOW_APPLET, true)) {
            return false;
        }
        String arg = (String) request.get(ARG_APPLET, "true");
        return Misc.equals(arg, "true");
    }


    /** _more_          */
    List api = new ArrayList();

    /** _more_          */
    Hashtable requestMap = new Hashtable();

    /**
     * _more_
     *
     * @param request _more_
     * @param methodName _more_
     * @param permission _more_
     * @param canCache _more_
     */
    protected void addRequest(String request, String methodName,
                              Permission permission, boolean canCache) {
        Class[] paramTypes = new Class[] { Request.class };
        Method  method = Misc.findMethod(getClass(), methodName, paramTypes);
        if (method == null) {
            throw new IllegalArgumentException("Unknown request method:"
                    + methodName);
        }
        ApiMethod apiMethod = new ApiMethod(request, permission, method,
                                            canCache);
        api.add(apiMethod);
        requestMap.put(request, apiMethod);
        requestMap.put(getUrlBase() + request, apiMethod);
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initRequests() throws Exception {
        Element apiRoot = XmlUtil.getRoot("/ucar/unidata/repository/api.xml",
                                          getClass());
        List children = XmlUtil.findChildren(apiRoot, TAG_METHOD);
        for (int i = 0; i < children.size(); i++) {
            Element node    = (Element) children.get(i);
            String  request = XmlUtil.getAttribute(node, ATTR_API_REQUEST);
            String  method  = XmlUtil.getAttribute(node, ATTR_API_METHOD);
            boolean admin = XmlUtil.getAttribute(node, ATTR_API_ADMIN, true);
            boolean canCache = XmlUtil.getAttribute(node, ATTR_API_CANCACHE,
                                   false);
            addRequest(request, method, new Permission(admin), canCache);
        }
    }

    /** _more_          */
    private Hashtable pageCache = new Hashtable();

    /** _more_          */
    private List pageCacheList = new ArrayList();



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleRequest(Request request) throws Exception {
        String incoming = request.getType().trim();
        if (incoming.endsWith("/")) {
            incoming = incoming.substring(0, incoming.length() - 1);
        }
        //        System.err.println ("incoming:"+incoming+":");
        if (incoming.startsWith(getUrlBase())) {
            incoming = incoming.substring(getUrlBase().length());
        }
        User      user      = request.getRequestContext().getUser();
        ApiMethod apiMethod = (ApiMethod) requestMap.get(incoming);
        if (apiMethod == null) {
            incoming = incoming;
            for (int i = 0; i < api.size(); i++) {
                ApiMethod tmp  = (ApiMethod) api.get(i);
                String    path = tmp.getRequest();
                if (path.endsWith("/*")) {
                    path = path.substring(0, path.length() - 2);
                    //                    System.err.println (path +":"+incoming);
                    if (incoming.startsWith(path)) {
                        apiMethod = tmp;
                        break;
                    }
                }
            }
        }
        Result result = null;
        if (apiMethod != null) {
            if (canCache() && apiMethod.getCanCache()) {
                result = (Result) pageCache.get(request);
                if (result != null) {
                    //                    System.err.println("from cache:" + request);
                    pageCacheList.remove(request);
                    pageCacheList.add(request);
                }
            }
            if (result == null) {
                if ( !apiMethod.getPermission().isRequestOk(request, this)) {
                    result = new Result("Error",
                                        new StringBuffer("Access Violation"));
                } else {
                    if ((connection == null)
                            && !incoming.startsWith("/admin")) {
                        result = new Result(
                            "No Database",
                            new StringBuffer("Database is shutdown"));
                    } else {
                        result = (Result) apiMethod.getMethod().invoke(this,
                                new Object[] { request });
                    }
                }
            }
        } else {
            //            result = new Result("Unknown Request",new StringBuffer("Unknown request:" + request.getType()));
        }
        if (result != null) {
            if (canCache() && apiMethod.getCanCache()) {
                //                System.err.println("caching:" + request);
                pageCache.put(request, result);
                pageCacheList.add(request);
                while (pageCacheList.size() > PAGE_CACHE_LIMIT) {
                    Request tmp = (Request) pageCacheList.remove(0);
                    pageCache.remove(tmp);
                }
            }
            result.putProperty(PROP_NAVLINKS, getNavLinks(request));
        }
        return result;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean canCache() {
        return getProperty(PROP_DB_CANCACHE, true);
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public String getProperty(String name) {
        return (String) properties.get(name);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getProperty(String name, boolean dflt) {
        return Misc.getProperty(properties, name, dflt);
    }




    public Connection getConnection() {
        return connection;
    } 

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initTable() throws Exception {
        System.err.println("making db");
        String sql =
            IOUtil.readContents("/ucar/unidata/repository/makedb.sql",
                                getClass());
        Statement statement = connection.createStatement();
        SqlUtil.loadSql(sql, statement, true);
        Element entriesRoot =
            XmlUtil.getRoot("/ucar/unidata/repository/entries.dbxml",
                            getClass());
        List children = XmlUtil.findChildren(entriesRoot, TAG_DB_ENTRY);

        for (int i = 0; i < children.size(); i++) {
            Element entryNode = (Element) children.get(i);
            Class handlerClass  = Misc.findClass(XmlUtil.getAttribute(entryNode,TAG_DB_HANDLER,"ucar.unidata.repository.GenericTypeHandler"));
            Constructor ctor = Misc.findConstructor(handlerClass,
                                                    new Class[] { Repository.class,Element.class});
            GenericTypeHandler typeHandler = (GenericTypeHandler)  ctor.newInstance(new Object[]{this,entryNode});
            addTypeHandler(typeHandler.getType(), typeHandler);
        }

        makeUserIfNeeded(new User("jdoe", "John Doe", true));
        makeUserIfNeeded(new User("jsmith", "John Smith", false));
        loadTestData();
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void loadTestData() throws Exception {
        ResultSet results = execute("select count(*) from "
                                    + TABLE_ENTRIES).getResultSet();
        results.next();
        if (results.getInt(1) == 0) {
            System.err.println("Adding test data");
            loadTestFiles();
            loadSatelliteFiles();
            loadLevel3RadarFiles();
            loadLevel2RadarFiles();
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
    public Result adminDb(Request request) throws Exception {
        StringBuffer sb   = new StringBuffer();
        sb.append("<h3>Database Administration</h3>");
        String       what = request.get(ARG_ADMIN_WHAT, "nothing");
        if (what.equals("shutdown")) {
            if (connection == null) {
                sb.append("Not connected to database");
            } else {
                connection.close();
                connection = null;
                sb.append("Database is shut down");
            }
        } else if (what.equals("restart")) {
            if (connection != null) {
                sb.append("Already connected to database");
            } else {
                makeConnection();
                sb.append("Database is restarted");
            }
        } 
        sb.append("<p>");
        sb.append(HtmlUtil.form(href("/admin/db"), " name=\"admin\""));
        if (connection == null) {
            sb.append(HtmlUtil.hidden(ARG_ADMIN_WHAT, "restart"));
            sb.append(HtmlUtil.submit("Restart Database"));
        } else {
            sb.append(HtmlUtil.hidden(ARG_ADMIN_WHAT, "shutdown"));
            sb.append(HtmlUtil.submit("Shut Down Database"));
        }
        sb.append("</form>");
        Result result = new Result("Administration", sb);
        result.putProperty(PROP_NAVSUBLINKS, getAdminLinks(request));
        return result;
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
    public Result adminHome(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("<h3>Repository Administration</h3><ul>\n");
        sb.append("<li> ");
        sb.append(href("/admin/db", "Administer Database"));
        sb.append("<li> ");
        sb.append(href("/admin/stats", "Statistics"));
        sb.append("<li> ");
        sb.append(href("/admin/sql", "Execute SQL"));
        sb.append("</ul>");
        Result result = new Result("Administration", sb);
        result.putProperty(PROP_NAVSUBLINKS, getAdminLinks(request));
        return result;
    }

    public int getCount(String table, String where) throws Exception {
        Statement statement =
            execute(SqlUtil.makeSelect("count(*)",
                                       Misc.newList(table), where));

        ResultSet results = statement.getResultSet();
        if(!results.next()) return 0;
        return results.getInt(1);
    }

    public Result adminStats(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("<h3>Repository Statistics</h3>");
        sb.append("<table>\n");
        String []tables = {"Users","Tags","Groups","Associations"};
        for(int i=0;i<tables.length;i++) {
            sb.append("<tr><td>"+ getCount(tables[i],"")+"</td><td>"+tables[i]+"</td></tr>");
        }


        sb.append("<tr><td colspan=\"2\">&nbsp;<p><b>Types:</b></td></tr>\n");
        int total = 0;
        sb.append("<tr><td>"+ getCount(TABLE_ENTRIES,"")+"</td><td>Total entries</td></tr>");
        for (Enumeration keys = typeHandlersMap.keys();
             keys.hasMoreElements(); ) {
            String id = (String) keys.nextElement();
            if(id.equals(TypeHandler.TYPE_ANY)) continue;
            TypeHandler typeHandler = (TypeHandler) typeHandlersMap.get(id);
            int cnt = getCount(TABLE_ENTRIES,"type=" + SqlUtil.quote(id));
            String url = href(HtmlUtil.url("/searchform",ARG_TYPE, id), typeHandler.getDescription());
            sb.append("<tr><td>"+ cnt+"</td><td>"+ url+"</td></tr>");            
        }



        sb.append("</table>\n");

        Result result = new Result("Repository Statistics", sb);
        result.putProperty(PROP_NAVSUBLINKS, getAdminLinks(request));
        return result;
    }





    /**
     * _more_
     *
     * @param args _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result adminSql(Request request) throws Exception {
        String       query = (String) request.get(ARG_QUERY);
        StringBuffer sb    = new StringBuffer();
        sb.append("<H3>SQL</h3>");
        sb.append(HtmlUtil.form(href("/admin/sql")));
        sb.append(HtmlUtil.submit("Execute"));
        sb.append(HtmlUtil.input(ARG_QUERY, query, " size=\"60\" "));
        sb.append("</form>\n");
        sb.append("<table>");
        if (query == null) {
            Result result = new Result("SQL", sb);
            result.putProperty(PROP_NAVSUBLINKS, getAdminLinks(request));
            return result;
        }

        long      t1        = System.currentTimeMillis();

        Statement statement = null;
        try {
            statement = execute(query);
        } catch (Exception exc) {
            exc.printStackTrace();
            throw exc;
        }

        SqlUtil.Iterator iter = SqlUtil.getIterator(statement);
        ResultSet        results;
        int              cnt    = 0;
        Hashtable        map    = new Hashtable();
        int              unique = 0;
        while ((results = iter.next()) != null) {
            ResultSetMetaData rsmd = results.getMetaData();
            while (results.next()) {
                cnt++;
                if (cnt > 1000) {
                    continue;
                }
                int colcnt = 0;
                if (cnt == 1) {
                    sb.append("<table><tr>");
                    for (int i = 0; i < rsmd.getColumnCount(); i++) {
                        sb.append(
                            HtmlUtil.col(
                                HtmlUtil.bold(rsmd.getColumnLabel(i + 1))));
                    }
                    sb.append("</tr>");
                }
                sb.append("<tr>");
                while (colcnt < rsmd.getColumnCount()) {
                    sb.append(HtmlUtil.col(results.getString(++colcnt)));
                }
                sb.append("</tr>\n");
                //                if (cnt++ > 1000) {
                //                    sb.append(HtmlUtil.row("..."));
                //                    break;
                //                }
            }
        }
        sb.append("</table>");
        long t2 = System.currentTimeMillis();
        Result result = new Result("SQL",
                                   new StringBuffer("Fetched:" + cnt
                                       + " rows in: " + (t2 - t1) + "ms <p>"
                                       + sb.toString()));
        result.putProperty(PROP_NAVSUBLINKS, getAdminLinks(request));
        return result;
    }





    /**
     * _more_
     */
    protected void initTypeHandlers() {
        addTypeHandler(TypeHandler.TYPE_ANY,
                       new TypeHandler(this, TypeHandler.TYPE_ANY,
                                       "Any file types"));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected String getGUID() {
        return baseTime + "_" + (keyCnt++);
    }







    /**
     * _more_
     *
     * @param typeName _more_
     * @param typeHandler _more_
     */
    protected void addTypeHandler(String typeName, TypeHandler typeHandler) {
        typeHandlersMap.put(typeName, typeHandler);
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
    protected TypeHandler getTypeHandler(Request request) throws Exception {
        String type = request.get(ARG_TYPE,TypeHandler.TYPE_ANY).trim();
        return getTypeHandler(type);
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
    protected TypeHandler getTypeHandler(String type) throws Exception {
        TypeHandler typeHandler = (TypeHandler) typeHandlersMap.get(type);
        if (typeHandler == null) {
            try {
                Class c = Misc.findClass("ucar.unidata.repository." + type);
                Constructor ctor = Misc.findConstructor(c,
                                       new Class[] { Repository.class,
                        String.class });
                typeHandler = (TypeHandler) ctor.newInstance(new Object[] {
                    this,
                    type });
            } catch (Throwable cnfe) {}
        }

        if (typeHandler == null) {
            typeHandler = new TypeHandler(this, type);
            addTypeHandler(type, typeHandler);
        }
        return typeHandler;
    }

    /**
     * _more_
     *
     * @param sql _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Group> getGroups(String sql) throws Exception {
        Statement statement = execute(sql);
        return getGroups(SqlUtil.readString(statement, 1));
    }

    /**
     * _more_
     *
     * @param groups _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Group> getGroups(String[] groups) throws Exception {
        List<Group> groupList = new ArrayList<Group>();
        for (int i = 0; i < groups.length; i++) {
            Group group = findGroup(groups[i]);
            if (group != null) {
                groupList.add(group);
            }
        }
        return groupList;
    }


    public Result processShowList(Request request) throws Exception {
        StringBuffer sb           = new StringBuffer();
        List links = getListLinks(request, false);
        TypeHandler typeHandler = getTypeHandler(request);
        List<TwoFacedObject> typeList = new ArrayList<TwoFacedObject>();
        List<TwoFacedObject> specialTypes = typeHandler.getListTypes(false);
        if(specialTypes.size()>0) {
            sb.append("<b>" + typeHandler.getDescription()+":</b>");
        }
        typeList.addAll(specialTypes);
        /*
        if(typeList.size()>0) {
            sb.append("<ul>");
            for(TwoFacedObject tfo: typeList) {
                sb.append("<li>");
                sb.append(href("/list/show?what=" +tfo.getId() +"&type=" + typeHandler.getType() , tfo.toString()));
                sb.append("\n");
            }
            sb.append("</ul>");
        }
        sb.append("<p><b>Basic:</b><ul><li>");
        */
        sb.append("<ul><li>");
        sb.append(StringUtil.join("<li>",links));
        sb.append("</ul>");



        Result result = new Result("Lists", sb);
        result.putProperty(PROP_NAVSUBLINKS, getListLinks(request,true));
        return result;

    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @return _more_
     */
    protected List getListLinks(Request request, boolean includeExtra) throws Exception {
        List links = new ArrayList();
        String extra =
            " style=\" font-family: Arial, Helvetica, sans-serif;  font-weight: bold; color:#ffffff;\" class=\"navtitle\"";
        if(!includeExtra) extra = "";
        ///query?what=type

        TypeHandler typeHandler = getTypeHandler(request);
        List<TwoFacedObject> typeList = typeHandler.getListTypes(false);
        if(typeList.size()>0) {
            for(TwoFacedObject tfo: typeList) {
                links.add(href("/list/show?what=" +tfo.getId() +"&type=" + typeHandler.getType() , tfo.toString(),extra));
            }
        }
        String typeAttr = "";
        if(!typeHandler.getType().equals(TypeHandler.TYPE_ANY)) {
            typeAttr = "&type=" +typeHandler.getType();
        }

        links.add(href("/list/show?what=" +WHAT_TYPE+typeAttr, "Types", extra));
        links.add(href("/list/show?what=" +WHAT_GROUP+typeAttr, "Groups", extra));
        links.add(href("/list/show?what=" +WHAT_TAG+typeAttr, "Tags", extra));
        links.add(href("/list/show?what=" +WHAT_ASSOCIATION+typeAttr, "Associations", extra));
        return links;
    }




    /**
     * _more_
     *
     * @param args _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSearchForm(Request request) throws Exception {
        List         where        = assembleWhereClause(request);
        StringBuffer sb           = new StringBuffer();
        StringBuffer headerBuffer = new StringBuffer();
        headerBuffer.append("<h3>Search Form</h3>");
        headerBuffer.append("<table cellpadding=\"5\">");

        sb.append(HtmlUtil.form(href(HtmlUtil.url("/query", "name",WHAT_ENTRIES))));

        TypeHandler typeHandler = getTypeHandler(request);

        String      what        = (String) request.get(ARG_WHAT,"");

        List whatList = Misc.toList(new Object[] {
            new TwoFacedObject("Entries", WHAT_ENTRIES),
            new TwoFacedObject("Data Types", WHAT_TYPE),
            new TwoFacedObject("Groups", WHAT_GROUP),
            new TwoFacedObject("Tags", WHAT_TAG),
            new TwoFacedObject("Associations", WHAT_ASSOCIATION)
        });
        whatList.addAll(typeHandler.getListTypes(true));

        String output = (String) request.get(ARG_OUTPUT,"");
        String outputHtml ="";
        if (output.length()==0) {
            outputHtml =  HtmlUtil.bold("Output Type: ") + HtmlUtil.select(ARG_OUTPUT,
                                                                           Misc.newList(OUTPUT_HTML,
                                                                                        OUTPUT_XML, 
                                                                                        OUTPUT_RSS,
                                                                                        OUTPUT_CSV));
        } else {
            outputHtml = HtmlUtil.bold("Output Type: ") +  output;
            sb.append(HtmlUtil.hidden(output, ARG_OUTPUT));
        }

        if (what.length()==0) {
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Search For:"),
                                          HtmlUtil.select(ARG_WHAT,
                                              whatList)+ "&nbsp;&nbsp;&nbsp;" + outputHtml));

        } else {
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Search For:"),
                                          TwoFacedObject.findLabel(what,
                                              whatList)+ "&nbsp;&nbsp;&nbsp;" + outputHtml));
            sb.append(HtmlUtil.hidden(ARG_WHAT, what));
        }

        typeHandler.addToSearchForm(sb, headerBuffer, request, where);


        sb.append(HtmlUtil.tableEntry("", HtmlUtil.submit("Search")));
        sb.append("<table>");
        sb.append("</form>");
        headerBuffer.append(sb.toString());

        Result result = new Result("Search Form", headerBuffer);
        result.putProperty(PROP_NAVSUBLINKS, getSearchFormLinks(request));
        return result;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @return _more_
     */
    protected List getAdminLinks(Request request) {
        List links = new ArrayList();
        String extra =
            " style=\" font-family: Arial, Helvetica, sans-serif;  font-weight: bold; color:#ffffff;\" class=\"navtitle\"";
        links.add(href("/admin/home", "Home", extra));
        links.add(href("/admin/db", "Database", extra));
        links.add(href("/admin/stats", "Statistics", extra));
        links.add(href("/admin/sql", "SQL", extra));
        return links;
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @return _more_
     */
    protected List getSearchFormLinks(Request request) {
        List links = new ArrayList();
        String extra =
            " style=\" font-family: Arial, Helvetica, sans-serif;  font-weight: bold; color:#ffffff;\" class=\"navtitle\"";
        links.add("<span " + extra+">Search For: </span>" +href(HtmlUtil.url("/searchform", ARG_WHAT, WHAT_ENTRIES), "Entries", extra));
        links.add(href(HtmlUtil.url("/searchform",ARG_WHAT, WHAT_TYPE), "Types", extra));
        links.add(href(HtmlUtil.url("/searchform",ARG_WHAT,WHAT_GROUP), "Groups", extra));
        links.add(href(HtmlUtil.url("/searchform", ARG_WHAT, WHAT_TAG), "Tags", extra));
        links.add(href(HtmlUtil.url("/searchform", ARG_WHAT, WHAT_ASSOCIATION), "Associations", extra));

        return links;
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @return _more_
     */
    protected List getNavLinks(Request request) {
        List links = new ArrayList();
        String extra =
            " style=\" font-family: Arial, Helvetica, sans-serif;  font-weight: bold; color:#ffffff;\" class=\"navtitle\"";
        links.add(href("/showgroup", "Groups", extra));
        links.add(href("/searchform", "Search", extra));
        links.add(href("/list/home", "List", extra));
        RequestContext context = request.getRequestContext();
        User           user    = context.getUser();
        if (user.getAdmin()) {
           links.add(href("/admin/home", "Admin", extra));
        }
        return links;
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public int getMax(Request request) {
        String max = (String) request.get(ARG_MAX,""+MAX_ROWS).trim();
        return new Integer(max).intValue();
    }

    /**
     * _more_
     *
     * @param args _more_
     * @param column _more_
     * @param tag _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processList(Request request) throws Exception {
        String what = request.get(ARG_WHAT, WHAT_TYPE);
        Result result =null;
        if (what.equals(WHAT_GROUP)) {
            result =  listGroups(request);
        } else if (what.equals(WHAT_TAG)) {
            result =  listTags(request);
        } else if (what.equals(WHAT_ASSOCIATION)) {
            result =  listAssociations(request);
        } else if (what.equals(WHAT_TYPE)) {
            result =  listTypes(request);
        } else {
            TypeHandler typeHandler = getTypeHandler(request);
            result = typeHandler.processList(request, what);
        }
        result.putProperty(PROP_NAVSUBLINKS, getListLinks(request,true));
        return result;
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
    public Result processGetEntry(Request request) throws Exception {
        String fileId = (String) request.get(ARG_ID);
        if (fileId == null) {
            throw new IllegalArgumentException("No " + ARG_ID + " given");
        }
        Entry entry = getEntry(fileId, request);
        if (entry == null) {
            throw new IllegalArgumentException("Could not find file with id:" + fileId);
        }
        if(!entry.getTypeHandler().isEntryDownloadable(request, entry)) {
            throw new IllegalArgumentException("Cannot download file with id:" + fileId);
        }
        String contents = IOUtil.readContents(entry.getFile(), getClass());
        return new Result("", new StringBuffer(contents),
                          IOUtil.getFileExtension(entry.getFile()));
    }


    /**
     * _more_
     *
     * @param fileId _more_
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Entry getEntry(String fileId, Request request) throws Exception {
        String query = SqlUtil.makeSelect(COLUMNS_ENTRIES,
                                          Misc.newList(TABLE_ENTRIES),
                                          SqlUtil.eq(COL_ENTRIES_ID,
                                                     SqlUtil.quote(fileId)));
        ResultSet results = execute(query).getResultSet();
        if ( !results.next()) {
            return null;
        }
        TypeHandler typeHandler = getTypeHandler(results.getString(2));
        return filterEntry(request, typeHandler.getEntry(results));
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
    public Result processShowEntry(Request request) throws Exception {
        String fileId = (String) request.get(ARG_ID);
        if (fileId == null) {
            throw new IllegalArgumentException("No " + ARG_ID + " given");
        }
        Entry entry = getEntry(fileId, request);
        if (entry == null) {
            throw new IllegalArgumentException("Could not find file");
        }
        TypeHandler typeHandler = getTypeHandler(entry.getType());
        StringBuffer sb = typeHandler.getEntryContent(entry, request);
        return new Result("Entry: " + entry.getName(), sb,
                          getMimeTypeFromOutput(request.get(ARG_OUTPUT,OUTPUT_HTML)));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry filterEntry(Request request, Entry file) throws Exception {
        //TODO: Check for access
        return file;
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
    public List<Entry> filterEntries(Request request, List<Entry> entries)
            throws Exception {
        List<Entry> filtered = new ArrayList();
        for (Entry entry : entries) {
            entry = filterEntry(request, entry);
            if (entry != null) {
                filtered.add(entry);
            }
        }
        return filtered;
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
    public Result processGetEntries(Request request) throws Exception {
        List<Entry> entries = new ArrayList();
        for (Enumeration keys = request.getParameters().keys();
                keys.hasMoreElements(); ) {
            String id = (String) keys.nextElement();
            if (!Misc.equals(request.get(id), "true")) {
                continue;
            }
            if ( !id.startsWith("file_")) {
                continue;
            }
            id = id.substring("file_".length());
            Entry entry = getEntry(id, request);
            if (entry != null) {
                entries.add(entry);
            }
        }
        String ids = request.get(ARG_IDS);
        if(ids!=null) {
            List<String> idList = StringUtil.split(ids,",",true,true);
            for(String id: idList) {
                Entry entry = getEntry(id, request);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        entries = filterEntries(request, entries);
        String output = request.get(ARG_OUTPUT, OUTPUT_CATALOG);

        if(output.equals(OUTPUT_HTML)) {
            StringBuffer sb = new StringBuffer();
            getEntryHtml(sb,entries,request,true,true);
            return new Result("Entries", sb, getMimeTypeFromOutput(output));
        } else if (output.equals(OUTPUT_CATALOG)) {
            return toCatalog(request, entries, "Entry Listing");
        } else if (output.equals(OUTPUT_ZIP)) {
            return toZip(request, entries);
        } else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected long currentTime() {
        return new Date().getTime();

    }

    protected String[] getBreadCrumbs(Request request, Group group) throws Exception {
        List  breadcrumbs = new ArrayList();
        List  titleList   = new ArrayList();
        Group parent      = group.getParent();
        while (parent != null) {
            titleList.add(0, parent.getName());
            breadcrumbs.add(0, href(HtmlUtil.url("/showgroup", ARG_GROUP,
                                                 parent.getFullName()), parent.getName()));
            parent = parent.getParent();
        }
        breadcrumbs.add(0, href("/showgroup", "Top"));
        titleList.add(group.getName());
        breadcrumbs.add(group.getName() + " "
                        + getGroupLinks(request, group));
        String title = "Group: "
            + StringUtil.join("&nbsp;&gt;&nbsp;", titleList);
        return new String[]{title, StringUtil.join("&nbsp;&gt;&nbsp;", breadcrumbs)};
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
    public Result processShowGroup(Request request) throws Exception {

        Group  theGroup  = null;
        String groupName = (String) request.get(ARG_GROUP);
        if (groupName != null) {
            request.getParameters().remove(ARG_GROUP);
            theGroup = findGroupFromName(groupName);
        }
        List<Group> groups      = new ArrayList<Group>();
        TypeHandler typeHandler = getTypeHandler(request);
        boolean     topLevel    = false;
        String title = "Groups";
        if (theGroup == null) {
            topLevel = true;
            Statement statement = execute(SqlUtil.makeSelect(COL_GROUPS_ID,
                                                             Misc.newList(TABLE_GROUPS),
                                                             COL_GROUPS_PARENT + " IS NULL"));
            groups.addAll(getGroups(SqlUtil.readString(statement, 1)));
        } else {
            groups.add(theGroup);
            title = theGroup.getFullName();
        }


        String       output = request.get(ARG_OUTPUT, OUTPUT_HTML);
        List         where  = typeHandler.assembleWhereClause(request);
        StringBuffer sb     = new StringBuffer();

        if (output.equals(OUTPUT_HTML)) {
            if (topLevel) {
                sb.append("<h3>Top Level Groups</h3>" + "<ul>");
            }
        } else if (output.equals(OUTPUT_CATALOG)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_CATALOG,
                                      CATALOG_ATTRS+
                                      XmlUtil.attrs(ATTR_NAME,
                                                    title)));
            sb.append(XmlUtil.openTag(TAG_DATASET,
                                      XmlUtil.attrs(ATTR_NAME,
                                                    title)));
        }



        if (topLevel) {
            if (output.equals(OUTPUT_CATALOG)) {
                sb.append(toCatalogInner(request, groups));
            } else {
                for (Group group : groups) {
                    if (output.equals(OUTPUT_HTML)) {
                        sb.append(
                                  getGroupLinks(request, group) + " " );
                        sb.append(href(HtmlUtil.url(
                                                    "/showgroup", ARG_GROUP,
                                                    group.getFullName()), group.getFullName()) + "</a> "
                                  );
                        sb.append("\n<br>\n");
                    }
                }
            }
        } else { 
            Group group = groups.get(0);
            List<Group> subGroups = getGroups(
                                              SqlUtil.makeSelect(
                                                                 COL_GROUPS_ID,
                                                                 Misc.newList(TABLE_GROUPS),
                                                                 SqlUtil.eq(
                                                                            COL_GROUPS_PARENT,
                                                                            SqlUtil.quote(
                                                                                          group.getId()))));

            where.add(SqlUtil.eq(COL_ENTRIES_GROUP_ID,
                                 SqlUtil.quote(group.getId())));
            Statement  stmt = typeHandler.executeSelect(request,
                                                        SqlUtil.comma(
                                                                      COL_ENTRIES_ID, COL_ENTRIES_NAME, COL_ENTRIES_TYPE,
                                                                      COL_ENTRIES_FILE),where);
            SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
            ResultSet        results;
            int              cnt = 0;
            List<Entry> entries = new ArrayList();
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    if (cnt++ > 1000) {
                        break;
                    }
                    int    col  = 1;
                    String id   = results.getString(col++);
                    Entry entry = getEntry(id,request);
                    if(entry!=null) entries.add(entry);
                }
            }

            entries = filterEntries(request, entries);
            if (output.equals(OUTPUT_CATALOG)) {
                sb.append(toCatalogInner(request, subGroups));
                sb.append(toCatalogInner(request, entries));
            } else if (output.equals(OUTPUT_HTML)) {
                String[] crumbs = getBreadCrumbs(request, group);
                title = crumbs[0];
                sb.append(HtmlUtil.bold("Group: ")+ crumbs[1]);
                sb.append("<hr>");
                if (subGroups.size() > 0) {
                    sb.append(HtmlUtil.bold("Sub groups:"));
                    sb.append("<ul>");
                    for (Group subGroup : subGroups) {
                        sb.append(getGroupLinks(request, subGroup) + " " 
                                  + href(HtmlUtil
                                         .url("/showgroup", ARG_GROUP,
                                              subGroup.getFullName()), subGroup
                                         .getFullName()) + "</a>");

                        sb.append("\n<br>\n");
                    }
                    sb.append("</ul>");
                }
                if(entries.size()>0) {
                    sb.append("\n");
                    sb.append(HtmlUtil.bold("Entries:"));
                    sb.append("<br>");
                    getEntryHtml(sb,entries,request,true,false);
                }
            }
        }


        if (output.equals(OUTPUT_CATALOG)) {
            sb.append(XmlUtil.closeTag(TAG_DATASET));
            sb.append(XmlUtil.closeTag(TAG_CATALOG));
        }
        return new Result(title, sb, getMimeTypeFromOutput(output));

    }

    public void getEntryHtml(StringBuffer sb, List<Entry> entries, Request request, boolean doForm, boolean dfltSelected) {
        if(doForm) {
            sb.append(HtmlUtil.form("/getentries", "getentries"));
            sb.append(HtmlUtil.submit("Get Selected Entries"));
            List outputList =
                Misc.toList(new Object[] {
                    new TwoFacedObject("As catalog", OUTPUT_XML),
                    new TwoFacedObject("As zip file",
                                       OUTPUT_ZIP) });
            sb.append(HtmlUtil.select(ARG_OUTPUT, outputList));
            sb.append("<p>\n");
            sb.append("<ul>\n");

        }
        for(Entry entry: entries) {
            sb.append(HtmlUtil.checkbox("file_" + entry.getId(), "true",dfltSelected) + " " +
                      entry.getTypeHandler().getEntryLinks(entry,request) + " " +
                      href(HtmlUtil.url("/showentry", ARG_ID, entry.getId()),
                             entry.getName()));
            sb.append("<br>\n");
        }
        if(doForm) {
            sb.append("</ul>");
            sb.append("</form>");
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
    public Result processGraphView(Request request) throws Exception {
        if (true || (graphAppletTemplate == null)) {
            graphAppletTemplate = IOUtil.readContents(
                "/ucar/unidata/repository/graphapplet.html", getClass());
        }

        String type = request.get(ARG_NODETYPE, NODETYPE_GROUP);
        String id   = request.get(ARG_ID, null);

        if ((type == null) || (id == null)) {
            throw new IllegalArgumentException(
                "no type or id argument specified");
        }
        String html = StringUtil.replace(graphAppletTemplate, "%id%", id);
        html = StringUtil.replace(html, "%type%", type);
        return new Result("Graph View", html.getBytes(), Result.TYPE_HTML);
    }


    /**
     * _more_
     *
     * @param results _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getEntryNodeXml(Request request, ResultSet results) throws Exception {
        int    col      = 1;
        String fileId   = results.getString(col++);
        String name     = results.getString(col++);
        String fileType = results.getString(col++);
        String groupId  = results.getString(col++);
        String file     = results.getString(col++);
        TypeHandler typeHandler = getTypeHandler(request);
        String nodeType = typeHandler.getNodeType();
        return XmlUtil.tag(TAG_NODE,
                           XmlUtil.attrs(ATTR_TYPE, nodeType, ATTR_ID,
                                         fileId, ATTR_TITLE, name));
    }


    protected String[]getTags(Request request, String entryId) throws Exception {
        String tagQuery = SqlUtil.makeSelect(COL_TAGS_NAME,
                                             Misc.newList(TABLE_TAGS),
                                             SqlUtil.eq(COL_TAGS_ENTRY_ID,
                                                        SqlUtil.quote(entryId)));
        return SqlUtil.readString(execute(tagQuery), 1);
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
    public Result processGetGraph(Request request) throws Exception {

        if (true || (graphXmlTemplate == null)) {
            graphXmlTemplate = IOUtil.readContents(
                "/ucar/unidata/repository/graphtemplate.xml", getClass());
        }
        String id   = (String) request.get(ARG_ID);
        String originalId   = id;
        String type = (String) request.get(ARG_NODETYPE);
        int skip =  new Integer(request.get(ARG_SKIP,"0")).intValue();

        boolean haveSkip = false;
        if(id.startsWith("skip_")) {
            haveSkip = true;
            //skip_tag_" +(cnt+skip)+"_"+id;
            List toks = StringUtil.split(id,"_",true,true);
            type = (String) toks.get(1);
            skip = new Integer((String) toks.get(2)).intValue();
            toks.remove(0);
            toks.remove(0);
            toks.remove(0);
            id = StringUtil.join("_",toks);
        }

        int MAX_EDGES = 15;
        if (id == null) {
            throw new IllegalArgumentException("Could not find id:"
                    + request);
        }
        if (type == null) {
            type = NODETYPE_GROUP;
        }
        TypeHandler typeHandler = getTypeHandler(request);
        StringBuffer sb = new StringBuffer();
        if (type.equals(TYPE_TAG)) {
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, TYPE_TAG, ATTR_ID,
                                      originalId, ATTR_TITLE, originalId)));
            Statement stmt = typeHandler.executeSelect(request,
                                                       SqlUtil.comma(COL_ENTRIES_ID,
                                                                     COL_ENTRIES_NAME, COL_ENTRIES_TYPE,
                                                                     COL_ENTRIES_GROUP_ID, COL_ENTRIES_FILE),
                                                       Misc.newList(SqlUtil.eq(COL_TAGS_ENTRY_ID,COL_ENTRIES_ID),
                                                                    SqlUtil.eq(COL_TAGS_NAME, SqlUtil.quote(id))));

            SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
            ResultSet        results;
            int  cnt = 0;
            int actualCnt = 0;
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    cnt++;
                    if(cnt<=skip) continue;
                    actualCnt++;
                    sb.append(getEntryNodeXml(request,results));
                    sb.append(XmlUtil.tag(TAG_EDGE,
                                          XmlUtil.attrs(ATTR_TYPE,
                                              "taggedby", ATTR_FROM, originalId,
                                                  ATTR_TO,
                                                      results.getString(1))));

                    if (actualCnt >= MAX_EDGES) {
                        String skipId  = "skip_" + type +"_" +(actualCnt+skip)+"_"+id;
                        sb.append(XmlUtil.tag(TAG_NODE,
                                              XmlUtil.attrs(ATTR_TYPE, "skip", ATTR_ID,
                                                            skipId, ATTR_TITLE, "...")));
                        sb.append(XmlUtil.tag(TAG_EDGE,
                                              XmlUtil.attrs(ATTR_TYPE, "etc",
                                                            ATTR_FROM, originalId,
                                                            ATTR_TO, skipId)));
                        break;
                    }
                }
            }
            String xml = StringUtil.replace(graphXmlTemplate, "%content%",
                                            sb.toString());
            return new Result("", new StringBuffer(xml),
                              getMimeTypeFromOutput(OUTPUT_GRAPH));
        }

        if ( !type.equals(TYPE_GROUP)) {
            Statement stmt = typeHandler.executeSelect(request,
                                                       SqlUtil.comma(COL_ENTRIES_ID,
                                                                     COL_ENTRIES_NAME, COL_ENTRIES_TYPE, COL_ENTRIES_GROUP_ID,
                                                                     COL_ENTRIES_FILE), 
                                                       Misc.newList(SqlUtil.eq(COL_ENTRIES_ID, SqlUtil.quote(id))));

            ResultSet results = stmt.getResultSet();
            if ( !results.next()) {
                throw new IllegalArgumentException("Unknown entry id:" + id);
            }

            sb.append(getEntryNodeXml(request,results));
            Group group = findGroup(results.getString(4));
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, NODETYPE_GROUP, ATTR_ID,
                                      group.getFullName(), ATTR_TITLE,
                                      group.getFullName())));
            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, group.getFullName(),
                                      ATTR_TO, results.getString(1))));

            String[] tags = getTags(request,id);
            for (int i = 0; i < tags.length; i++) {
                sb.append(XmlUtil.tag(TAG_NODE,
                                      XmlUtil.attrs(ATTR_TYPE, TYPE_TAG,
                                          ATTR_ID, tags[i], ATTR_TITLE,
                                          tags[i])));
                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TYPE, "taggedby",
                                          ATTR_FROM, tags[i], ATTR_TO, id)));
            }




            String xml = StringUtil.replace(graphXmlTemplate, "%content%",
                                            sb.toString());
            return new Result("", new StringBuffer(xml),
                              getMimeTypeFromOutput(OUTPUT_GRAPH));
        }

        Group group = findGroupFromName(id);
        if (group == null) {
            throw new IllegalArgumentException("Could not find group:" + id);
        }
        sb.append(XmlUtil.tag(TAG_NODE,
                              XmlUtil.attrs(ATTR_TYPE, NODETYPE_GROUP, ATTR_ID,
                                            group.getFullName(), ATTR_TITLE,
                                            group.getFullName())));
        List<Group> subGroups = getGroups(SqlUtil.makeSelect(COL_GROUPS_ID,
                                    Misc.newList(TABLE_GROUPS),
                                    SqlUtil.eq(COL_GROUPS_PARENT,
                                        SqlUtil.quote(group.getId()))));

        Group parent = group.getParent();
        if (parent != null) {
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, NODETYPE_GROUP, ATTR_ID,
                                      parent.getFullName(), ATTR_TITLE,
                                      parent.getFullName())));
            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, parent.getFullName(),
                                      ATTR_TO, group.getFullName())));
        }


        for (Group subGroup : subGroups) {

            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, NODETYPE_GROUP, ATTR_ID,
                                      subGroup.getFullName(), ATTR_TITLE,
                                      subGroup.getFullName())));

            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, group.getFullName(),
                                      ATTR_TO, subGroup.getFullName())));
        }

        String query = SqlUtil.makeSelect(SqlUtil.comma(COL_ENTRIES_ID,
                           COL_ENTRIES_NAME, COL_ENTRIES_TYPE,
                           COL_ENTRIES_GROUP_ID,
                           COL_ENTRIES_FILE), Misc.newList(TABLE_ENTRIES),
                               SqlUtil.eq(COL_ENTRIES_GROUP_ID,
                                          SqlUtil.quote(group.getId())));
        SqlUtil.Iterator iter = SqlUtil.getIterator(execute(query));
        ResultSet        results;
        int cnt=0;
        int actualCnt = 0;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                cnt++;
                if(cnt<=skip) continue;
                actualCnt++;
                sb.append(getEntryNodeXml(request,results));
                String fileId = results.getString(1);
                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                          ATTR_FROM, (haveSkip?originalId:group.getFullName()),
                                                    ATTR_TO, fileId)));
                sb.append("\n");
                if (actualCnt >= MAX_EDGES) {
                    String skipId  = "skip_" + type +"_" +(actualCnt+skip)+"_"+id;
                    sb.append(XmlUtil.tag(TAG_NODE,
                                          XmlUtil.attrs(ATTR_TYPE, "skip", ATTR_ID,
                                                        skipId, ATTR_TITLE, "...")));
                    sb.append(XmlUtil.tag(TAG_EDGE,
                                          XmlUtil.attrs(ATTR_TYPE, "etc",
                                                        ATTR_FROM, originalId,
                                                        ATTR_TO, skipId)));
                    break;
                }
            }
        }
        String xml = StringUtil.replace(graphXmlTemplate, "%content%",
                                        sb.toString());
        xml = StringUtil.replace(xml, "%root%", urlBase);
        return new Result("", new StringBuffer(xml),
                          getMimeTypeFromOutput(OUTPUT_GRAPH));
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listGroups(Request request) throws Exception {
        TypeHandler typeHandler = getTypeHandler(request);
        Statement    statement =  typeHandler.executeSelect(request,
                                                            SqlUtil.distinct(COL_ENTRIES_GROUP_ID));
        String[]     groups    = SqlUtil.readString(statement, 1);
        StringBuffer sb        = new StringBuffer();
        String       output    = request.get(ARG_OUTPUT, OUTPUT_HTML);
        if (output.equals(OUTPUT_HTML)) {
            sb.append("<h3>Groups</h3>");
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_GROUPS));

        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }

        for (int i = 0; i < groups.length; i++) {
            Group group = findGroup(groups[i]);
            if (group == null) {
                continue;
            }

            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li>" +getGroupLinks(request, group) +" " +group.getFullName());
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(TAG_GROUP,
                                      XmlUtil.attrs(ATTR_NAME,
                                          group.getFullName(), ATTR_ID,
                                          group.getId())));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(group.getFullName(), group.getId()));
                sb.append("\n");
            }

        }
        if (output.equals(OUTPUT_HTML)) {
            sb.append("</ul>\n");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_GROUPS));
        }

        return new Result("", sb, getMimeTypeFromOutput(output));
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
    protected List<TypeHandler> getTypeHandlers(Request request)
            throws Exception {
        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        Statement stmt = typeHandler.executeSelect(request, 
                                                   SqlUtil.distinct(COL_ENTRIES_TYPE),
                                                   where);
        String[]          types        = SqlUtil.readString(stmt, 1);
        List<TypeHandler> typeHandlers = new ArrayList<TypeHandler>();
        for (int i = 0; i < types.length; i++) {
            typeHandlers.add(getTypeHandler(types[i]));
        }
        return typeHandlers;
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
    protected Result listTypes(Request request) throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       output = request.get(ARG_OUTPUT, OUTPUT_HTML);
        if (output.equals(OUTPUT_HTML)) {
            sb.append("<h3>Types</h3>");
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_TYPES));
        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }

        List<TypeHandler> typeHandlers = getTypeHandlers(request);
        for (TypeHandler theTypeHandler : typeHandlers) {
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li>");
                sb.append(
                    href(HtmlUtil.url(
                        "/searchform", ARG_TYPE,
                        theTypeHandler.getType()), HtmlUtil.img(urlBase + "/Search16.gif", "Search in Group")));
                sb.append(" ");
                sb.append(
                    href(HtmlUtil.url(
                        "/list/home", ARG_TYPE,
                        theTypeHandler.getType()), theTypeHandler.getType()));
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(TAG_TYPE,
                                      XmlUtil.attrs(ATTR_TYPE,
                                          theTypeHandler.getType())));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(theTypeHandler.getType(),
                                        theTypeHandler.getDescription()));
                sb.append("\n");
            }

        }
        if (output.equals(OUTPUT_HTML)) {
            sb.append("</ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_TYPES));
        }
        return new Result("", sb, getMimeTypeFromOutput(output));
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
    protected Result listTags(Request request) throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       output = request.get(ARG_OUTPUT, OUTPUT_HTML);
        request.getParameters().remove(ARG_OUTPUT);            
        if (output.equals(OUTPUT_HTML)) {
            request.getParameters().put(ARG_OUTPUT, OUTPUT_CLOUD);
            String other = href("/query"+"?"+request.getUrlArgs(),"Tag Cloud");
            sb.append("<h3>Tag List &nbsp;|&nbsp; " + other +"</h3>");
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_CLOUD)) {
            request.getParameters().put(ARG_OUTPUT, OUTPUT_HTML);
            String other = href("/query"+"?"+request.getUrlArgs(),"Tag List");
            sb.append("<h3>" + other +" &nbsp;|&nbsp; " +"Tag Cloud</h3>");
            sb.append("<p>\n");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_TAGS));
        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }
        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);

        if (where.size() > 0) {
            where.add(0, SqlUtil.eq(COL_TAGS_ENTRY_ID, COL_ENTRIES_ID));
        }


        String[] tags = SqlUtil.readString(typeHandler.executeSelect(request,
                                                                     SqlUtil.distinct(COL_TAGS_NAME),
                                                                     where, " order by " + COL_TAGS_NAME),1);

        List<String>     names  = new ArrayList<String>();
        List<Integer>    counts = new ArrayList<Integer>();
        ResultSet        results;
        int              max  = -1;
        int              min  = -1;
        for(int i=0;i<tags.length;i++) {
            String tag   = tags[i];
            Statement stmt2 = typeHandler.executeSelect(request,
                                                        SqlUtil.count("*"),
                                                        Misc.newList(SqlUtil.eq(COL_TAGS_NAME,SqlUtil.quote(tag))));

            ResultSet results2 = stmt2.getResultSet();
            if(!results2.next()) continue;
            int    count = results2.getInt(1);
            if ((max < 0) || (count > max)) {
                max = count;
            }
            if ((min < 0) || (count < min)) {
                min = count;
            }
            names.add(tag);
            counts.add(new Integer(count));
        }

        int    diff         = max - min;
        double distribution = diff / 5.0;

        for (int i = 0; i < names.size(); i++) {
            String tag   = names.get(i);
            int    count = counts.get(i).intValue();
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li> ");
                sb.append(getTagLinks(request, tag));
                sb.append(" ");
                sb.append(tag);
                sb.append(" (" + count + ")");

            } else if (output.equals(OUTPUT_CLOUD)) {
                double percent = count / distribution;
                int    bin     = (int) (percent * 5);
                String css     = "font-size:" + (12 + bin * 2);
                sb.append("<span style=\"" + css + "\">");
                String extra = XmlUtil.attrs("alt", "Count:" + count,
                                             "title", "Count:" + count);
                sb.append(href(HtmlUtil.url("/graphview", ARG_ID, tag, ARG_NODETYPE,
                                            TYPE_TAG), tag, extra));
                sb.append("</span>");
                sb.append(" &nbsp; ");
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(TAG_TAG,
                                      XmlUtil.attrs(ATTR_NAME, tag)));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(tag);
                sb.append("\n");
            }
        }

        String pageTitle = "";
        if (output.equals(OUTPUT_HTML)) {
            if(tags.length==0)
                sb.append("No tags found");
            pageTitle = "Tags";
            sb.append("</ul>");
        } else if (output.equals(OUTPUT_CLOUD)) {
            if(tags.length==0)
                sb.append("No tags found");
            pageTitle = "Tag Cloud";
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_TAGS));
        }
        return new Result(pageTitle, sb, getMimeTypeFromOutput(output));
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
    protected Result listAssociations(Request request) throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       output = request.get(ARG_OUTPUT, OUTPUT_HTML);
        if (output.equals(OUTPUT_HTML)) {
            sb.append("<h3>Associations</h3>");
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_CLOUD)) {
            sb.append("<h3>Association Cloud</h3>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_ASSOCIATIONS));
        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }
        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        if (where.size() > 0) {
            where.add(0, SqlUtil.eq(COL_ASSOCIATIONS_FROM_ENTRY_ID, COL_ENTRIES_ID));            
            where.add(0, SqlUtil.eq(COL_ASSOCIATIONS_TO_ENTRY_ID, COL_ENTRIES_ID));
        }


        String[] associations = SqlUtil.readString(typeHandler.executeSelect(request,
                                                                             SqlUtil.distinct(COL_ASSOCIATIONS_NAME),
                                                                             where),1);

        List<String>     names  = new ArrayList<String>();
        List<Integer>    counts = new ArrayList<Integer>();
        ResultSet        results;
        int              max  = -1;
        int              min  = -1;
        for(int i=0;i<associations.length;i++) {
            String association   = associations[i];
            Statement stmt2 = typeHandler.executeSelect(request,
                                                        SqlUtil.count("*"),
                                                        Misc.newList(SqlUtil.eq(COL_ASSOCIATIONS_NAME,SqlUtil.quote(association))));

            ResultSet results2 = stmt2.getResultSet();
            if(!results2.next()) continue;
            int    count = results2.getInt(1);
            if ((max < 0) || (count > max)) {
                max = count;
            }
            if ((min < 0) || (count < min)) {
                min = count;
            }
            names.add(association);
            counts.add(new Integer(count));
        }

        int    diff         = max - min;
        double distribution = diff / 5.0;

        for (int i = 0; i < names.size(); i++) {
            String association   = names.get(i);
            int    count = counts.get(i).intValue();
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li> ");
                sb.append(getAssociationLinks(request, association));
                sb.append(" ");
                sb.append(association);
                sb.append(" (" + count + ")");

            } else if (output.equals(OUTPUT_CLOUD)) {
                double percent = count / distribution;
                int    bin     = (int) (percent * 5);
                String css     = "font-size:" + (12 + bin * 2);
                sb.append("<span style=\"" + css + "\">");
                String extra = XmlUtil.attrs("alt", "Count:" + count,
                                             "title", "Count:" + count);
                sb.append(href(HtmlUtil.url("/graphview", ARG_ID, association, ARG_NODETYPE,
                                            TYPE_ASSOCIATION), association, extra));
                sb.append("</span>");
                sb.append(" &nbsp; ");
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(TAG_ASSOCIATION,
                                      XmlUtil.attrs(ATTR_NAME, association)));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(association);
                sb.append("\n");
            }
        }

        String pageTitle = "";
        if (output.equals(OUTPUT_HTML)) {
            pageTitle = "Associations";
            sb.append("</ul>");
        } else if (output.equals(OUTPUT_CLOUD)) {
            pageTitle = "Association Cloud";
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_ASSOCIATIONS));
        }
        return new Result(pageTitle, sb, getMimeTypeFromOutput(output));
    }



    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    protected String getMimeTypeFromOutput(String output) {
        if (output.equals(OUTPUT_CSV)) {
            return getMimeType(".csv");
        } else if (output.equals(OUTPUT_XML)) {
            return getMimeType(".xml");
        } else if (output.equals(OUTPUT_GRAPH)) {
            return getMimeType(".xml");
        } else if (output.equals(OUTPUT_RSS)) {
            return getMimeType(".rss");
        } else if (output.equals(OUTPUT_HTML)) {
            return getMimeType(".html");
        } else if (output.equals(OUTPUT_CLOUD)) {
            return getMimeType(".html");
        } else if (output.equals(OUTPUT_ZIP)) {
            return getMimeType(".zip");
        } else {
            return getMimeType(".txt");
        }
    }


    /**
     * _more_
     *
     * @param suffix _more_
     *
     * @return _more_
     */
    protected String getMimeType(String suffix) {
        String type = (String) mimeTypes.get(suffix);
        if (type == null) {
            if (suffix.startsWith(".")) {
                suffix = suffix.substring(1);
            }
            type = (String) mimeTypes.get(suffix);
        }
        if (type == null) {
            type = "unknown";
        }
        return type;
    }



    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    protected String href(String url) {
        return urlBase + url;
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param label _more_
     *
     * @return _more_
     */
    protected String href(String url, String label) {
        return href(url, label, "");
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param label _more_
     * @param extra _more_
     *
     * @return _more_
     */
    protected String href(String url, String label, String extra) {
        return HtmlUtil.href(urlBase + url, label, extra);
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
    protected List assembleWhereClause(Request request) throws Exception {
        return getTypeHandler(request).assembleWhereClause(request);
    }







    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void initGroups() throws Exception {
        Statement statement =
            execute(SqlUtil.makeSelect(SqlUtil.comma(COL_GROUPS_ID,
                COL_GROUPS_PARENT, COL_GROUPS_NAME,
                COL_GROUPS_DESCRIPTION), Misc.newList(TABLE_GROUPS)));

        ResultSet        results;
        SqlUtil.Iterator iter   = SqlUtil.getIterator(statement);
        List<Group>      groups = new ArrayList<Group>();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col = 1;
                Group group = new Group(results.getString(col++),
                                        findGroup(results.getString(col++)),
                                        results.getString(col++),
                                        results.getString(col++));
                groups.add(group);
                groupMap.put(group.getId(), group);
            }
        }
        for (Group group : groups) {
            if (group.getParentId() != null) {
                group.setParent(groupMap.get(group.getParentId()));
            }
            groupMap.put(group.getFullName(), group);
        }
    }




    /**
     * _more_
     *
     * @param user _more_
     *
     * @throws Exception _more_
     */
    protected void makeUserIfNeeded(User user) throws Exception {
        if (findUser(user.getId()) == null) {
            makeUser(user);
        }
    }

    /**
     * _more_
     *
     * @param user _more_
     *
     * @throws Exception _more_
     */
    protected void makeUser(User user) throws Exception {
        execute(INSERT_USERS, new Object[] { user.getId(), user.getName(),
                                             new Boolean(user.getAdmin()) });
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User findUser(String id) throws Exception {
        if (id == null) {
            return null;
        }
        User user = userMap.get(id);
        if (user != null) {
            return user;
        }
        String query = SqlUtil.makeSelect(COLUMNS_USERS,
                                          Misc.newList(TABLE_USERS),
                                          SqlUtil.eq(COL_USERS_ID,
                                              SqlUtil.quote(id)));
        ResultSet results = execute(query).getResultSet();
        if ( !results.next()) {
            //            throw new IllegalArgumentException ("Could not find  user id:" + id + " sql:" + query);
            return null;
        } else {
            int col = 1;
            user = new User(results.getString(col++),
                            results.getString(col++),
                            results.getBoolean(col++));
        }

        userMap.put(user.getId(), user);
        return user;
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Group findGroup(String id) throws Exception {
        if ((id == null) || (id.length() == 0)) {
            return null;
        }
        Group group = groupMap.get(id);
        if (group != null) {
            return group;
        }
        String query = SqlUtil.makeSelect(COLUMNS_GROUPS,
                                          Misc.newList(TABLE_GROUPS),
                                          SqlUtil.eq(COL_GROUPS_ID,
                                              SqlUtil.quote(id)));
        Statement statement = execute(query);
        //id,parent,name,description
        ResultSet results = statement.getResultSet();
        if (results.next()) {
            group = new Group(results.getString(1),
                              findGroup(results.getString(2)),
                              results.getString(3), results.getString(4));
        } else {
            //????
            return null;
        }
        groupMap.put(id, group);
        return group;
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Group findGroupFromName(String name) throws Exception {
        return findGroupFromName(name, false);
    }

    protected Group findGroupFromName(String name, boolean createIfNeeded) throws Exception {
        //        if(name.indexOf(Group.IDDELIMITER) >=0) Misc.printStack(name,10,null);
        Group group = groupMap.get(name);
        if (group != null) {
            return group;
        }
        List<String> toks = (List<String>) StringUtil.split(name, "/", true,
                                true);
        Group  parent = null;
        String lastName;
        if ((toks.size() == 0) || (toks.size() == 1)) {
            lastName = name;
        } else {
            lastName = toks.get(toks.size() - 1);
            toks.remove(toks.size() - 1);
            parent = findGroupFromName(StringUtil.join("/", toks),createIfNeeded);
            if(parent == null) return null;
        }
        String where = "";
        if (parent != null) {
            where += SqlUtil.eq(COL_GROUPS_PARENT,
                                SqlUtil.quote(parent.getId())) + " AND ";
        } else {
            where += COL_GROUPS_PARENT + " is null AND ";
        }
        where += SqlUtil.eq(COL_GROUPS_NAME, SqlUtil.quote(lastName));

        String query = SqlUtil.makeSelect(COLUMNS_GROUPS,
                                          Misc.newList(TABLE_GROUPS), where);

        Statement statement = execute(query);
        ResultSet results   = statement.getResultSet();
        if (results.next()) {
            group = new Group(results.getString(1), parent,
                              results.getString(3), results.getString(4));
        } else {
            if(!createIfNeeded) return null;
            int baseId = 0;
            String idWhere;
            if(parent==null)
                idWhere =  COL_GROUPS_PARENT + " IS NULL ";
            else 
                idWhere =  SqlUtil.eq(COL_GROUPS_PARENT, SqlUtil.quote(parent.getId()));
            String newId=null;
            while(true) {
                if(parent==null)            
                   newId = ""+baseId;
                else 
                    newId = parent.getId()+Group.IDDELIMITER+baseId;
                ResultSet idResults = execute(SqlUtil.makeSelect(COL_GROUPS_ID,Misc.newList(TABLE_GROUPS), idWhere +" AND " + 
                                                                 SqlUtil.eq(COL_GROUPS_ID, SqlUtil.quote(newId)))).getResultSet();
                
                if(!idResults.next()) break;
                baseId++;
            }
            //            System.err.println ("made id:" + newId);
            execute(INSERT_GROUPS, new Object[] { newId, ((parent != null)
                    ? parent.getId()
                    : null), lastName, lastName });
            group = new Group(newId, parent, lastName, lastName);
        }
        groupMap.put(group.getId(), group);
        groupMap.put(name, group);
        return group;
    }


    /**
     * _more_
     *
     * @param insert _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    protected void execute(String insert, Object[] values) throws Exception {
        PreparedStatement pstmt = connection.prepareStatement(insert);
        for (int i = 0; i < values.length; i++) {
            //Assume null is a string
            if (values[i] == null) {
                pstmt.setNull(i + 1, java.sql.Types.VARCHAR);
            } else {
                pstmt.setObject(i + 1, values[i]);
            }
        }
        pstmt.execute();
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
    protected List<Entry> getEntries(Request request) throws Exception {
        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        Statement        statement = typeHandler.executeSelect(request, 
                                                               COLUMNS_ENTRIES,
                                                               where,
                                                               "order by " + COL_ENTRIES_FROMDATE);
        List<Entry>      entries   = new ArrayList<Entry>();
        ResultSet        results;
        SqlUtil.Iterator iter = SqlUtil.getIterator(statement);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                //id,type,name,desc,group,user,file,createdata,fromdate,todate
                TypeHandler localTypeHandler =
                    getTypeHandler(results.getString(2));
                entries.add(localTypeHandler.getEntry(results));
            }
        }
        return entries;
    }





    /**
     * _more_
     *
     * @param product _more_
     *
     * @return _more_
     */
    protected String getLongName(String product) {
        return getLongName(product, product);
    }

    /**
     * _more_
     *
     * @param product _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    protected String getLongName(String product, String dflt) {
        if (productMap == null) {
            productMap = new Properties();
            try {
                InputStream s =
                    IOUtil.getInputStream(
                        "/ucar/unidata/repository/names.properties",
                        getClass());
                productMap.load(s);
            } catch (Exception exc) {
                System.err.println("err:" + exc);
            }
        }
        String name = (String) productMap.get(product);
        if (name != null) {
            return name;
        }
        //        System.err.println("not there:" + product+":");
        return dflt;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getGroupLinks(Request request, Group group)
            throws Exception {
        String search =
            href(HtmlUtil.url("/searchform", ARG_GROUP,
                              group.getId()), HtmlUtil.img(urlBase
                                      + "/Search16.gif", "Search in Group"));

        String catalog =
            href(HtmlUtil.url("/showgroup", ARG_GROUP,
                              group.getFullName(),
                              ARG_OUTPUT, OUTPUT_CATALOG), HtmlUtil.img(urlBase
                                      + "/catalog.jpg", "Get group catalog"));

        return search + " " + catalog +" " +getGraphLink(request, group);
    }

    protected String getTagLinks(Request request, String tag)
            throws Exception {
        String search =
            href(HtmlUtil.url("/searchform", ARG_TAG,
                              java.net.URLEncoder.encode(tag,
                                  "UTF-8")), HtmlUtil.img(urlBase
                                      + "/Search16.gif", "Search in tag"));

        if (isAppletEnabled(request)) {
            search += href(HtmlUtil.url("/graphview", ARG_ID, tag,
                                        ARG_NODETYPE,
                                        TYPE_TAG), HtmlUtil.img(urlBase + "/tree.gif",
                                                                "Show tag in graph"));
        }
        return search;
    }


    protected String getAssociationLinks(Request request, String association)
            throws Exception {
        String search =
            href(HtmlUtil.url("/searchform", ARG_ASSOCIATION,
                              java.net.URLEncoder.encode(association,
                                  "UTF-8")), HtmlUtil.img(urlBase
                                      + "/Search16.gif", "Search in association"));

        if (isAppletEnabled(request)) {
            search += href(HtmlUtil.url("/graphview", ARG_ID, association,
                                        ARG_NODETYPE,
                                        TYPE_ASSOCIATION), HtmlUtil.img(urlBase + "/tree.gif",
                                                                "Show association in graph"));
        }
        return search;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     *
     * @return _more_
     */
    protected String getGraphLink(Request request, Group group) {
        if ( !isAppletEnabled(request)) {
            return "";
        }
        return href(HtmlUtil.url("/graphview", ARG_ID, group.getFullName(),
                                 ARG_NODETYPE,
                                 NODETYPE_GROUP), HtmlUtil.img(urlBase
                                 + "/tree.gif","Show group in graph"));
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
    public Result processQuery(Request request) throws Exception {

        String what = request.get(ARG_WHAT, WHAT_ENTRIES);
        if ( !what.equals(WHAT_ENTRIES)) {
            Result result = processList(request);
            if(result == null) throw new IllegalArgumentException ("Unknown list request: " + what);
            result.putProperty(PROP_NAVSUBLINKS, getSearchFormLinks(request));
            return result;
        }

        timelineAppletTemplate = IOUtil.readContents(
            "/ucar/unidata/repository/timelineapplet.html", getClass());
        List         times   = new ArrayList();
        List         labels  = new ArrayList();
        List         ids     = new ArrayList();
        List<Entry>  entries = getEntries(request);


        StringBuffer sb      = new StringBuffer();
        String       output  = request.get(ARG_OUTPUT, OUTPUT_HTML);
        if (output.equals(OUTPUT_CATALOG)) {
            return toCatalog(request, entries, "Query Results");
        }


        if (output.equals(OUTPUT_HTML)) {
            sb.append("<h3>Query Results</h3>");
            if (entries.size() == 0) {
                sb.append("<b>Nothing Found</b><p>");
            }
            sb.append("<table>");
        } else if (output.equals(OUTPUT_RSS)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_RSS_RSS,
                                      XmlUtil.attrs(ATTR_RSS_VERSION,
                                          "2.0")));
            sb.append(XmlUtil.openTag(TAG_RSS_CHANNEL));
            sb.append(XmlUtil.tag(TAG_RSS_TITLE, "", "Repository Query"));
        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }

        StringBufferCollection sbc = new StringBufferCollection();
        for (Entry entry : entries) {
            times.add(SqlUtil.format(new Date(entry.getStartDate())));
            labels.add(entry.getName());
            ids.add(entry.getId());
            StringBuffer ssb = sbc.getBuffer(entry.getTypeHandler().getDescription());
            if (output.equals(OUTPUT_HTML)) {
                String links = HtmlUtil.checkbox("file_" + entry.getId(),
                                                 "true") + " " + entry.getTypeHandler().getEntryLinks(entry,request);

                ssb.append(HtmlUtil
                    .row(links + " "
                         + href(HtmlUtil
                             .url("/showentry", ARG_ID, entry.getId()), entry
                             .getName()), "" + new Date(entry
                             .getStartDate())));
            } else if (output.equals(OUTPUT_RSS)) {
                sb.append(XmlUtil.openTag(TAG_RSS_ITEM));
                sb.append(XmlUtil.tag(TAG_RSS_PUBDATE, "",
                                      "" + new Date(entry.getStartDate())));
                sb.append(XmlUtil.tag(TAG_RSS_TITLE, "", entry.getName()));
                sb.append(XmlUtil.tag(TAG_RSS_DESCRIPTION, "",
                                      entry.getDescription()));
                sb.append(XmlUtil.closeTag(TAG_RSS_ITEM));
                //      <link>http://earthquake.usgs.gov/eqcenter/recenteqsww/Quakes/us2007kmae.php</link>
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(entry.getId(), entry.getFile()));
            }
        }


        if (output.equals(OUTPUT_HTML)) {
            if (entries.size() > 0) {
                String tmp = StringUtil.replace(timelineAppletTemplate,
                                 "%times%", StringUtil.join(",", times));
                tmp = StringUtil.replace(tmp, "%labels%",
                                         StringUtil.join(",", labels));
                tmp = StringUtil.replace(tmp, "%ids%",
                                         StringUtil.join(",", ids));
                tmp = StringUtil.replace(tmp, "%loadurl%",
                                         href(HtmlUtil.url("/getentries",ARG_IDS,"%ids%", ARG_OUTPUT,OUTPUT_HTML)));
                tmp = StringUtil.replace(tmp, "%loadtypes%",
                                         SqlUtil.comma(OUTPUT_HTML,OUTPUT_CATALOG,OUTPUT_ZIP));
                if (isAppletEnabled(request)) {
                    sb.append(tmp);
                }
            }
            sb.append(HtmlUtil.form("/getentries", "getentries"));
            if (entries.size() > 0) {
                sb.append(HtmlUtil.submit("Get selected entries"));
                List outputList =
                    Misc.toList(new Object[] {
                        new TwoFacedObject("As catalog", OUTPUT_XML),
                        new TwoFacedObject("As zip file",
                                           OUTPUT_ZIP) });
                sb.append(HtmlUtil.select(ARG_OUTPUT, outputList));
            }
            sb.append("<br>");
        }
        for (int i = 0; i < sbc.getKeys().size(); i++) {
            String       type = (String) sbc.getKeys().get(i);
            StringBuffer ssb  = sbc.getBuffer(type);
            if (output.equals(OUTPUT_HTML)) {
                sb.append(HtmlUtil.row(HtmlUtil.bold("Type:" + type)));
                sb.append(ssb);
            }
        }

        if (output.equals(OUTPUT_HTML)) {
            sb.append("</form>");
            sb.append("</table>");
        } else if (output.equals(OUTPUT_RSS)) {
            sb.append(XmlUtil.closeTag(TAG_RSS_CHANNEL));
            sb.append(XmlUtil.closeTag(TAG_RSS_RSS));
        }
        Result result = new Result("Query Results", sb,
                                   getMimeTypeFromOutput(output));
        result.putProperty(PROP_NAVSUBLINKS, getSearchFormLinks(request));
        return result;

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
    protected Result toZip(Request request, List<Entry> entries)
            throws Exception {
        ByteArrayOutputStream bos  = new ByteArrayOutputStream();
        ZipOutputStream       zos  = new ZipOutputStream(bos);
        Hashtable             seen = new Hashtable();
        for (Entry entry : entries) {
            String path = entry.getFile();
            String name = IOUtil.getFileTail(path);
            int    cnt  = 1;
            while (seen.get(name) != null) {
                name = (cnt++) + "_" + name;
            }
            seen.put(name, name);
            zos.putNextEntry(new ZipEntry(name));
            byte[] bytes = IOUtil.readBytes(IOUtil.getInputStream(path,
                               getClass()));
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
        }
        zos.close();
        bos.close();
        return new Result("", bos.toByteArray(),
                          getMimeTypeFromOutput(OUTPUT_ZIP));
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param title _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result toCatalog(Request request, List objects,
                               String title)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(XmlUtil.XML_HEADER + "\n");
        sb.append(XmlUtil.openTag(TAG_CATALOG,
                                  CATALOG_ATTRS+
                                  XmlUtil.attrs(ATTR_NAME,
                                                title)));
        sb.append(XmlUtil.openTag(TAG_DATASET, XmlUtil.attrs(ATTR_NAME, title)));
        sb.append(toCatalogInner(request, objects));
        sb.append(XmlUtil.closeTag(TAG_DATASET));
        sb.append(XmlUtil.closeTag(TAG_CATALOG));
        Result result = new Result(title, sb,
                                   getMimeTypeFromOutput(OUTPUT_CATALOG));
        return result;
    }


    protected StringBuffer toCatalogInner(Request request, List objects)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        List<Entry> entries  = new ArrayList();
        List<Group> groups = new ArrayList();
        for(Object obj: objects) {
            if(obj instanceof Entry) {
                entries.add((Entry)obj);
            } else if(obj instanceof Group) {
                groups.add((Group)obj);
            } else {
                throw new IllegalArgumentException("Unknown object type:" + obj.getClass().getName());
            }

        }
        for(Group group: groups) {
            String url = /* "http://localhost:8080"+*/href(HtmlUtil.url(
                                             "/showgroup", ARG_GROUP,
                                             group.getFullName(),
                                             ARG_OUTPUT,
                                             OUTPUT_CATALOG));
            sb.append(XmlUtil.tag(TAG_CATALOGREF, XmlUtil.attrs(ATTR_XLINKTITLE, group.getName(),
                                                                ATTR_XLINKHREF,
                                                                url)));
        }

        StringBufferCollection sbc = new StringBufferCollection();
        for (Entry entry : entries) {
            StringBuffer ssb = sbc.getBuffer(entry.getTypeHandler().getDescription());
            ssb.append(entry.getTypeHandler().getDatasetTag(entry,request));
        }

        for (int i = 0; i < sbc.getKeys().size(); i++) {
            String       type = (String) sbc.getKeys().get(i);
            StringBuffer ssb  = sbc.getBuffer(type);
            if(sbc.getKeys().size()>1)
                sb.append(XmlUtil.openTag(TAG_DATASET,
                                          XmlUtil.attrs(ATTR_NAME, type)));
            sb.append(ssb);
            if(sbc.getKeys().size()>1)
                sb.append(XmlUtil.closeTag(TAG_DATASET));
        }
        return sb;

    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param statement _more_
     *
     * @throws Exception _more_
     */
    protected void setStatement(Entry entry, PreparedStatement statement)
            throws Exception {
        int col = 1;
        //id,type,name,desc,group,user,file,createdata,fromdate,todate
        statement.setString(col++, entry.getId());
        statement.setString(col++, entry.getType());
        statement.setString(col++, entry.getName());
        statement.setString(col++, entry.getDescription());
        statement.setString(col++, entry.getGroupId());
        statement.setString(col++, entry.getUser().getId());
        statement.setString(col++, entry.getFile().toString());
        statement.setTimestamp(col++, new java.sql.Timestamp(currentTime()));
        //        System.err.println (entry.getName() + " " + new Date(entry.getStartDate()));
        statement.setTimestamp(col++,
                               new java.sql.Timestamp(entry.getStartDate()));
        statement.setTimestamp(col++,
                               new java.sql.Timestamp(entry.getStartDate()));
    }


    public void insertEntries(TypeHandler typeHandler, List<Entry> entries) throws Exception {
        if(entries.size() == 0) return;
        System.err.println("Inserting:" + entries.size() + " " + typeHandler.getType()+" entries");
        long t1  = System.currentTimeMillis();
        int  cnt = 0;
        PreparedStatement entryInsert =
            connection.prepareStatement(INSERT_ENTRIES);

        String sql = typeHandler.getInsertSql();
        PreparedStatement typeInsert = (sql==null?null:
                                        connection.prepareStatement(sql));
        PreparedStatement tagsInsert =
            connection.prepareStatement(INSERT_TAGS);

        int batchCnt = 0;
        connection.setAutoCommit(false);
        for (Entry entry : entries) {
            if ((++cnt) % 5000 == 0) {
                long   tt2      = System.currentTimeMillis();
                double tseconds = (tt2 - t1) / 1000.0;
                System.err.println("# " + cnt + " rate: "
                                   + ((int) (cnt / tseconds)) + "/s");
            }
            String id = getGUID();
            entry.setId(id);
            setStatement(entry, entryInsert);
            entryInsert.addBatch();

            if(typeInsert!=null) {
                typeHandler.setStatement(entry, typeInsert);
                typeInsert.addBatch();
            }
            List<String> tags = entry.getTags();
            if (tags != null) {
                for (String tag : tags) {
                    tagsInsert.setString(1, tag);
                    tagsInsert.setString(2, entry.getId());
                    batchCnt++;
                    tagsInsert.addBatch();
                }
            }


            batchCnt++;
            if (batchCnt > 100) {
                entryInsert.executeBatch();
                tagsInsert.executeBatch();
                if(typeInsert!=null) {
                    typeInsert.executeBatch();
                }
                batchCnt = 0;
            }
        }
        if (batchCnt > 0) {
            entryInsert.executeBatch();
            tagsInsert.executeBatch();
            if(typeInsert!=null) {
                typeInsert.executeBatch();
            }
        }
        connection.commit();
        connection.setAutoCommit(true);
    }




    /**
     * _more_
     *
     * @param sql _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Statement execute(String sql) throws Exception {
        return execute(sql, -1);
    }

    /**
     * _more_
     *
     * @param sql _more_
     * @param max _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Statement execute(String sql, int max) throws Exception {
        Statement statement = connection.createStatement();
        if (max > 0) {
            statement.setMaxRows(max);
        }
        long t1 = System.currentTimeMillis();
        try {
            //            System.err.println("query:" + sql);
            statement.execute(sql);
        } catch (Exception exc) {
            System.err.println("ERROR:" + sql);
            throw exc;
        }
        long t2 = System.currentTimeMillis();
        if (t2 - t1 > 300) {
            System.err.println("query:" + sql);
            System.err.println("time:" + (t2 - t1));
        }
        return statement;
    }


    /**
     * _more_
     *
     * @param stmt _more_
     * @param sql _more_
     *
     * @throws Exception _more_
     */
    public void eval(String sql) throws Exception {
        Statement statement = execute(sql);
        String[]  results   = SqlUtil.readString(statement, 1);
        for (int i = 0; (i < results.length) && (i < 10); i++) {
            System.err.print(results[i] + " ");
            if (i == 9) {
                System.err.print("...");
            }
        }
    }



    /**
     * Set the UrlBase property.
     *
     * @param value The new value for UrlBase
     */
    public void setUrlBase(String value) {
        urlBase = value;
    }

    /**
     * Get the UrlBase property.
     *
     * @return The UrlBase
     */
    public String getUrlBase() {
        return urlBase;
    }






    /**
     * _more_
     *
     * @param stmt _more_
     * @param table _more_
     *
     * @throws Exception _more_
     */
    public void loadModelFiles() throws Exception {
        File rootDir = new File("/data/ldm/gempak/model");
        TypeHandler typeHandler = getTypeHandler("model");
        List<Entry> entries = harvester.collectModelFiles(rootDir, "IDD/Model",typeHandler);
        insertEntries(typeHandler, entries);
    }


    /**
     * _more_
     *
     * @param stmt _more_
     * @param table _more_
     *
     * @throws Exception _more_
     */
    public void loadSatelliteFiles() throws Exception {
        File rootDir = new File("/data/ldm/gempak/images/sat");
        TypeHandler typeHandler = getTypeHandler("satellite");
        List<Entry> entries = harvester.collectSatelliteFiles(rootDir,
                                                                   "IDD/Satellite",typeHandler);
        insertEntries(typeHandler, entries);
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void loadTestFiles() throws Exception {
        File rootDir =
            new File(
                "c:/cygwin/home/jeffmc/unidata/src/idv/trunk/ucar/unidata");
        if(!rootDir.exists())
            rootDir = new File("/harpo/jeffmc/src/idv/trunk/ucar/unidata");
        TypeHandler typeHandler = getTypeHandler("file");
        List<FileInfo> dirs = new ArrayList();
        List<Entry> entries = harvester.collectFiles(rootDir, "Files",
                                                            typeHandler, dirs);

        //        System.err.println ("dirs:" + dirs.size());
        //        Misc.run(this,"listen", dirs);
        insertEntries(typeHandler,entries);
    }

    public void listen(List<FileInfo> dirs) {
        while(true) {
            for(FileInfo f: dirs) {
                if(f.hasChanged()) {
                    System.err.println ("changed:" + f);
                }
            }
            Misc.sleep(1000);
        }
    }



    /**
     * _more_
     *
     * @param stmt _more_
     * @param table _more_
     *
     * @throws Exception _more_
     */
    public void loadLevel3RadarFiles() throws Exception {
        File rootDir = new File("/data/ldm/gempak/nexrad/NIDS");
        TypeHandler typeHandler = getTypeHandler("level3radar");
        List<Entry> entries =
            harvester.collectLevel3radarFiles(rootDir, "IDD/Level3",
                typeHandler);
        insertEntries(typeHandler, entries);
    }



    /**
     * _more_
     *
     * @param stmt _more_
     * @param table _more_
     *
     * @throws Exception _more_
     */
    public void loadLevel2RadarFiles() throws Exception {
        File rootDir = new File("/data/ldm/gempak/nexrad/craft");
        TypeHandler typeHandler =   getTypeHandler("level2radar");
        List<Entry> entries =
            harvester.collectLevel2radarFiles(rootDir, "IDD/Level2",
                                              typeHandler);
        insertEntries(typeHandler, entries);
    }






}

