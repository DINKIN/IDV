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
import ucar.unidata.xml.XmlUtil;


import java.awt.*;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.*;

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

import javax.swing.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Repository implements Constants, Tables, RequestHandler {

    /** _more_ */
    public String URL_SHOWGROUP = "/showgroup";

    /** _more_ */
    public String URL_SEARCHFORM = "/searchform";

    /** _more_ */
    public String URL_LIST_HOME = "/list/home";

    /** _more_ */
    public String URL_QUERY = "/query";

    /** _more_ */
    public String URL_LIST_SHOW = "/list/show";

    /** _more_ */
    public String URL_GRAPHVIEW = "/graphview";

    /** _more_ */
    public String URL_GETGRAPH = "/getgraph";

    /** _more_ */
    public String URL_SHOWENTRY = "/showentry";

    /** _more_ */
    public String URL_GETENTRIES = "/getentries";

    /** _more_ */
    public String URL_GETENTRY = "/getentry";

    /** _more_ */
    public String URL_ADMIN_SQL = "/admin/sql";

    /** _more_ */
    public String URL_ADMIN_HOME = "/admin/home";

    /** _more_ */
    public String URL_ADMIN_DB = "/admin/db";

    /** _more_ */
    public String URL_ADMIN_STATS = "/admin/stats";

    /** _more_ */
    public String URL_ADMIN_USERS = "/admin/users";

    /** _more_ */
    public String URL_ADMIN_HARVESTERS = "/admin/harvesters";


    /** _more_ */
    private static final int PAGE_CACHE_LIMIT = 100;

    /** _more_ */
    private Harvester harvester;

    /** _more_ */
    private List<Harvester> harvesters = new ArrayList();

    /** _more_ */
    private Properties mimeTypes;

    /** _more_ */
    private Properties productMap;

    /** _more_ */
    private String repositoryDir;

    /** _more_ */
    private String tmpDir;

    /** _more_ */
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
    private List<OutputHandler> outputHandlers = new ArrayList();


    /** _more_ */
    private Hashtable resources = new Hashtable();





    /** _more_ */
    private Hashtable<String, Group> groupMap = new Hashtable<String,
                                                    Group>();

    /** _more_ */
    private Hashtable<String, User> userMap = new Hashtable<String, User>();


    /** _more_ */
    List<String> entryDefFiles;

    /** _more_ */
    List<String> apiDefFiles;

    /** _more_ */
    List<String> outputDefFiles;

    /** _more_ */
    String[] args;

    /** _more_ */
    private Hashtable pageCache = new Hashtable();

    /** _more_ */
    private List pageCacheList = new ArrayList();

    /** _more_ */
    private List<User> cmdLineUsers = new ArrayList();

    /**
     * _more_
     */
    protected void clearPageCache() {
        pageCache     = new Hashtable();
        pageCacheList = new ArrayList();
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public Repository(String[] args) throws Exception {
        this.args = args;
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void init() throws Exception {
        initProperties();
        initUrls();
        makeConnection();
        mimeTypes = new Properties();
        for (String mimeFile : StringUtil.split(
                getProperty(PROP_HTML_MIMEPROPERTIES), ";", true, true)) {
            mimeTypes.load(IOUtil.getInputStream(mimeFile, getClass()));
        }
        initTable();
        initTypeHandlers();
        initOutputHandlers();
        initApi();
        initUsers();
        initGroups();
        initHarvesters();
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initProperties() throws Exception {
        properties = new Properties();
        properties.load(
            IOUtil.getInputStream(
                "/ucar/unidata/repository/resources/repository.properties",
                getClass()));
        List<String> argEntryDefFiles  = new ArrayList();
        List<String> argApiDefFiles    = new ArrayList();
        List<String> argOutputDefFiles = new ArrayList();

        for (int i = 0; i < args.length; i++) {
            if (args[i].endsWith(".properties")) {
                properties.load(IOUtil.getInputStream(args[i], getClass()));
            } else if (args[i].indexOf("api.xml") >= 0) {
                argApiDefFiles.add(args[i]);
            } else if (args[i].indexOf(".dbxml") >= 0) {
                argEntryDefFiles.add(args[i]);
            } else if (args[i].indexOf("outputhandlers.xml") >= 0) {
                argOutputDefFiles.add(args[i]);
            } else if (args[i].equals("-admin")) {
                cmdLineUsers.add(new User(args[i + 1], args[i + 1], true));
                i++;
            } else if (args[i].startsWith("-D")) {
                String       s    = args[i].substring(2);
                List<String> toks = StringUtil.split(s, "=", true, true);
                if (toks.size() != 2) {
                    throw new IllegalArgumentException("Bad argument:"
                            + args[i]);
                }
                properties.put(toks.get(0), toks.get(1));
            } else {
                throw new IllegalArgumentException("Unknown argument: "
                        + args[i]);
            }
        }
        apiDefFiles = StringUtil.split(getProperty(PROP_API));
        apiDefFiles.addAll(argApiDefFiles);

        entryDefFiles = StringUtil.split(getProperty(PROP_DB_ENTRIES));
        entryDefFiles.addAll(argEntryDefFiles);

        outputDefFiles = StringUtil.split(getProperty(PROP_OUTPUT_FILES));
        outputDefFiles.addAll(argOutputDefFiles);

        urlBase = (String) properties.get(PROP_HTML_URLBASE);
        if (urlBase == null) {
            urlBase = "";
        }

        repositoryDir = IOUtil.joinDir(Misc.getSystemProperty("user.home",
                "."), IOUtil.joinDir(".unidata", "repository"));
        tmpDir = IOUtil.joinDir(repositoryDir, "tmp");
        IOUtil.makeDirRecursive(new File(repositoryDir));
        IOUtil.makeDirRecursive(new File(tmpDir));


        String derbyHome = (String) properties.get(PROP_DB_DERBY_HOME);
        if (derbyHome != null) {
            derbyHome = derbyHome.replace("%userhome%",
                                          Misc.getSystemProperty("user.home",
                                              "."));
            File dir = new File(derbyHome);
            IOUtil.makeDirRecursive(dir);
            System.setProperty("derby.system.home", derbyHome);
        }

        harvester = new Harvester(this);
        Misc.findClass((String) properties.get(PROP_DB_DRIVER));

    }


    /**
     * _more_
     */
    protected void initUrls() {
        URL_SHOWGROUP        = getUrlBase() + URL_SHOWGROUP;
        URL_SEARCHFORM       = getUrlBase() + URL_SEARCHFORM;
        URL_LIST_HOME        = getUrlBase() + URL_LIST_HOME;
        URL_QUERY            = getUrlBase() + URL_QUERY;
        URL_LIST_SHOW        = getUrlBase() + URL_LIST_SHOW;
        URL_GRAPHVIEW        = getUrlBase() + URL_GRAPHVIEW;
        URL_GETGRAPH         = getUrlBase() + URL_GETGRAPH;
        URL_SHOWENTRY        = getUrlBase() + URL_SHOWENTRY;
        URL_GETENTRIES       = getUrlBase() + URL_GETENTRIES;
        URL_GETENTRY         = getUrlBase() + URL_GETENTRY;
        URL_ADMIN_SQL        = getUrlBase() + URL_ADMIN_SQL;
        URL_ADMIN_HOME       = getUrlBase() + URL_ADMIN_HOME;
        URL_ADMIN_DB         = getUrlBase() + URL_ADMIN_DB;
        URL_ADMIN_STATS      = getUrlBase() + URL_ADMIN_STATS;
        URL_ADMIN_USERS      = getUrlBase() + URL_ADMIN_USERS;
        URL_ADMIN_HARVESTERS = getUrlBase() + URL_ADMIN_HARVESTERS;
    }


    /**
     * _more_
     *
     * @param message _more_
     * @param exc _more_
     */
    protected void log(String message, Throwable exc) {
        System.err.println(message);
        Throwable thr = LogUtil.getInnerException(exc);
        thr.printStackTrace();
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    protected void log(String message) {
        System.err.println(message);
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initUsers() throws Exception {
        for (User user : cmdLineUsers) {
            makeUser(user, true);
        }
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
     * @param request _more_
     *
     * @return _more_
     */
    protected boolean isAppletEnabled(Request request) {
        if ( !getProperty(PROP_SHOW_APPLET, true)) {
            return false;
        }
        if (request != null) {
            return request.get(ARG_APPLET, true);
        }
        return true;
    }


    /** _more_ */
    Hashtable<String, ApiMethod> requestMap = new Hashtable();

    /** _more_ */
    ApiMethod homeApi;

    /** _more_ */
    ArrayList<ApiMethod> apiMethods = new ArrayList();

    /** _more_ */
    ArrayList<ApiMethod> topLevelMethods = new ArrayList();


    /**
     * _more_
     *
     * @param node _more_
     *
     * @throws Exception _more_
     */
    protected void addRequest(Element node) throws Exception {

        String  request = XmlUtil.getAttribute(node, ApiMethod.ATTR_REQUEST);
        String  methodName = XmlUtil.getAttribute(node,
                                 ApiMethod.ATTR_METHOD);
        boolean admin = XmlUtil.getAttribute(node, ApiMethod.ATTR_ADMIN,
                                             true);

        Permission     permission = new Permission(admin);

        RequestHandler handler    = this;
        if (XmlUtil.hasAttribute(node, ApiMethod.ATTR_HANDLER)) {
            Class c = Misc.findClass(XmlUtil.getAttribute(node,
                          ApiMethod.ATTR_HANDLER));
            Constructor ctor = Misc.findConstructor(c,
                                   new Class[] { Repository.class,
                    Element.class });
            handler = (RequestHandler) ctor.newInstance(new Object[] { this,
                    node });
        }

        String    url       = getUrlBase() + request;
        ApiMethod oldMethod = requestMap.get(url);
        if (oldMethod != null) {
            requestMap.remove(url);
        }


        Class[] paramTypes = new Class[] { Request.class };
        Method method = Misc.findMethod(handler.getClass(), methodName,
                                        paramTypes);
        if (method == null) {
            throw new IllegalArgumentException("Unknown request method:"
                    + methodName);
        }
        ApiMethod apiMethod = new ApiMethod(
                                  handler, request,
                                  XmlUtil.getAttribute(
                                      node, ApiMethod.ATTR_NAME,
                                      request), permission, method,
                                          XmlUtil.getAttribute(
                                              node, ApiMethod.ATTR_CANCACHE,
                                              false), XmlUtil.getAttribute(
                                                  node,
                                                  ApiMethod.ATTR_TOPLEVEL,
                                                  false));
        if (XmlUtil.getAttribute(node, ApiMethod.ATTR_ISHOME, false)) {
            homeApi = apiMethod;
        }
        requestMap.put(url, apiMethod);
        if (oldMethod != null) {
            int index = apiMethods.indexOf(oldMethod);
            apiMethods.remove(index);
            apiMethods.add(index, apiMethod);
        } else {
            apiMethods.add(apiMethod);
        }
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initApi() throws Exception {
        for (String file : apiDefFiles) {
            Element apiRoot  = XmlUtil.getRoot(file, getClass());
            List    children = XmlUtil.findChildren(apiRoot, TAG_METHOD);
            for (int i = 0; i < children.size(); i++) {
                Element node = (Element) children.get(i);
                addRequest(node);
            }
        }
        for (ApiMethod apiMethod : apiMethods) {
            if (apiMethod.getIsTopLevel()) {
                topLevelMethods.add(apiMethod);
            }
        }


    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initHarvesters() throws Exception {
        try {
            Element root =
                XmlUtil.getRoot(
                    "/ucar/unidata/repository/resources/harvesters.xml",
                    getClass());
            List children = XmlUtil.findChildren(root, TAG_HARVESTER);
            for (int i = 0; i < children.size(); i++) {
                Element node = (Element) children.get(i);
                Class c = Misc.findClass(XmlUtil.getAttribute(node,
                              ATTR_CLASS));
                Constructor ctor = Misc.findConstructor(c,
                                       new Class[] { Repository.class,
                        Element.class });
                harvesters.add((Harvester) ctor.newInstance(new Object[] {
                    this,
                    node }));
            }
        } catch (Exception exc) {
            System.err.println("Error loading harvester file");
            throw exc;
        }
        for (Harvester harvester : harvesters) {
            if (harvester.getActive()) {
                Misc.run(harvester, "run");
            }
        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initOutputHandlers() throws Exception {
        for (String file : outputDefFiles) {
            try {
                Element root  = XmlUtil.getRoot(file, getClass());
                List children = XmlUtil.findChildren(root, TAG_OUTPUTHANDLER);
                for (int i = 0; i < children.size(); i++) {
                    Element node = (Element) children.get(i);
                    Class c = Misc.findClass(XmlUtil.getAttribute(node,
                                  ATTR_CLASS));
                    Constructor ctor = Misc.findConstructor(c,
                                           new Class[] { Repository.class,
                            Element.class });
                    outputHandlers.add(
                        (OutputHandler) ctor.newInstance(new Object[] { this,
                            node }));
                }
            } catch (Exception exc) {
                System.err.println("Error loading output handler file:"
                                   + file);
                throw exc;
            }

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
    public Result handleRequest(Request request) throws Exception {
        long   t1       = System.currentTimeMillis();

        Result result = getResult(request);
        if(result != null &&
           result.getInputStream()==null &&
           result.isHtml() && result.getShouldDecorate()) {
            result.putProperty(PROP_NAVLINKS, getNavLinks(request));
            decorateResult(result);
        }


        long t2 = System.currentTimeMillis();
        if ((result != null) && (t2 != t1)
                && (true || request.get("debug", false))) {
            System.err.println("Time:" + request.getType() + " " + (t2 - t1));
        }
        return result;


    }

    protected Result getResult(Request request) throws Exception {
        String incoming = request.getType().trim();
        if (incoming.endsWith("/")) {
            incoming = incoming.substring(0, incoming.length() - 1);
        }
        ApiMethod apiMethod = (ApiMethod) requestMap.get(incoming);
        if (apiMethod == null) {
            for (ApiMethod tmp : apiMethods) {
                String path = tmp.getRequest();
                if (path.endsWith("/*")) {
                    path = path.substring(0, path.length() - 2);
                    if (incoming.startsWith(getUrlBase() + path)) {
                        apiMethod = tmp;
                        break;
                    }
                }
            }
        }
        if ((apiMethod == null) && incoming.equals(getUrlBase())) {
            apiMethod = homeApi;
        }
        if (apiMethod == null) {
            return getHtdocsFile(request);
        }

        Result result = null;
        if (canCache() && apiMethod.getCanCache()) {
            result = (Result) pageCache.get(request);
            if (result != null) {
                pageCacheList.remove(request);
                pageCacheList.add(request);
                result.setShouldDecorate(false);
                return result;
            }
        }

        boolean cachingOk = canCache();

        if (!apiMethod.getPermission().isRequestOk(request, this)) {
            cachingOk = false;
            result = new Result("Error",
                                new StringBuffer("Access Violation"));
        } else {
            if ((connection == null)
                && !incoming.startsWith("/admin")) {
                cachingOk = false;
                result = new Result(
                                    "No Database",
                                    new StringBuffer("Database is shutdown"));
            } else {
                result = (Result) apiMethod.invoke(request);
            }
        }
        if(result ==null) return null;

        if (result.getInputStream()== null &&
            cachingOk &&
            apiMethod.getCanCache()) {
            pageCache.put(request, result);
            pageCacheList.add(request);
            while (pageCacheList.size() > PAGE_CACHE_LIMIT) {
                Request tmp = (Request) pageCacheList.remove(0);
                pageCache.remove(tmp);
            }
        }

        return result;
    }

    protected Result getHtdocsFile(Request request) throws Exception {
        String path= request.getType();
        String type = getMimeTypeFromSuffix(
                                                       IOUtil.getFileExtension(path));
        path = StringUtil.replace(path,
                                  getUrlBase(), "");
        if ((path.trim().length() == 0) || path.equals("/")) {
            return new Result("Error",
                              new StringBuffer("Unknown request:"
                                               + path));
        }

        try {
            InputStream is =
                IOUtil.getInputStream(
                                      "/ucar/unidata/repository/htdocs" + path,
                                      getClass());
            Result result = new Result("", is, type);
            result.setCacheOk(true);
            return result;
        } catch (IOException fnfe) {
            return  new Result("Error",
                               new StringBuffer("Unknown request:"
                                                + path));
        }
    }

    protected void decorateResult(Result result) throws Exception {
        String template = getResource(PROP_HTML_TEMPLATE);
        String html = StringUtil.replace(template, "${content}",
                                         new String(result.getContent()));
        html = StringUtil.replace(html, "${title}", result.getTitle());
        html = StringUtil.replace(html, "${root}",
                                  getUrlBase());
        List   links     = (List) result.getProperty(PROP_NAVLINKS);
        String linksHtml = "&nbsp;";
        if (links != null) {
            linksHtml = StringUtil.join("&nbsp;|&nbsp;", links);
        }
        List   sublinks     = (List) result.getProperty(PROP_NAVSUBLINKS);
        String sublinksHtml = "";
        if (sublinks != null) {
            sublinksHtml = StringUtil.join("\n&nbsp;|&nbsp;\n", sublinks);
        }

        html = StringUtil.replace(html, "${links}", linksHtml);
        if (sublinksHtml.length() > 0) {
            html = StringUtil.replace(html, "${sublinks}",
                                      "<div class=\"subnav\">"
                                      + sublinksHtml + "</div>");
        } else {
            html = StringUtil.replace(html, "${sublinks}", "");
        }
        result.setContent(html.getBytes());
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean canCache() {
        if (true) {
            return false;
        }
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
    public String getProperty(String name, String dflt) {
        return Misc.getProperty(properties, name, dflt);
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




    /**
     * _more_
     *
     * @return _more_
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     *
     */
    protected void initTable() throws Exception {
        String sql = IOUtil.readContents(getProperty(PROP_DB_SCRIPT),
                                         getClass());
        Statement statement = connection.createStatement();
        SqlUtil.loadSql(sql, statement, true);
        for (String file : entryDefFiles) {
            Element entriesRoot = XmlUtil.getRoot(file, getClass());
            List    children = XmlUtil.findChildren(entriesRoot,
                                   TAG_DB_ENTRY);
            for (int i = 0; i < children.size(); i++) {
                Element entryNode = (Element) children.get(i);
                Class handlerClass =
                    Misc.findClass(XmlUtil.getAttribute(entryNode,
                        TAG_DB_HANDLER,
                        "ucar.unidata.repository.GenericTypeHandler"));
                Constructor ctor = Misc.findConstructor(handlerClass,
                                       new Class[] { Repository.class,
                        Element.class });
                GenericTypeHandler typeHandler =
                    (GenericTypeHandler) ctor.newInstance(new Object[] { this,
                        entryNode });
                addTypeHandler(typeHandler.getType(), typeHandler);
            }
        }

        makeUserIfNeeded(new User("jdoe", "John Doe", true));
        makeUserIfNeeded(new User("anonymous", "Anonymous", false));
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
        File rootDir =
            new File(
                "c:/cygwin/home/jeffmc/unidata/src/idv/trunk/ucar/unidata");
        if ( !rootDir.exists()) {
            rootDir = new File("/harpo/jeffmc/src/idv/trunk/ucar/unidata");
        }
        TypeHandler typeHandler = getTypeHandler("file");
        if (false && (results.getInt(1) == 0)) {
            System.err.println("Adding test data");
            //            loadTestFiles();
            loadModelFiles();
            loadSatelliteFiles();
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
        StringBuffer sb = new StringBuffer();
        sb.append(header("Database Administration"));
        String what = request.getString(ARG_ADMIN_WHAT, "nothing");
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
        sb.append(HtmlUtil.form(URL_ADMIN_DB, " name=\"admin\""));
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
        sb.append(header("Repository Administration"));
        sb.append("<ul>\n");
        sb.append("<li> ");
        sb.append(HtmlUtil.href(URL_ADMIN_DB, "Administer Database"));
        sb.append("<li> ");
        sb.append(HtmlUtil.href(URL_ADMIN_STATS, "Statistics"));
        sb.append("<li> ");
        sb.append(HtmlUtil.href(URL_ADMIN_SQL, "Execute SQL"));
        sb.append("</ul>");
        Result result = new Result("Administration", sb);
        result.putProperty(PROP_NAVSUBLINKS, getAdminLinks(request));
        return result;
    }

    /**
     * _more_
     *
     * @param table _more_
     * @param where _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public int getCount(String table, String where) throws Exception {
        Statement statement = execute(SqlUtil.makeSelect("count(*)",
                                  Misc.newList(table), where));

        ResultSet results = statement.getResultSet();
        if ( !results.next()) {
            return 0;
        }
        return results.getInt(1);
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
    protected User getUser(ResultSet results) throws Exception {
        int col = 1;
        return new User(results.getString(col++), results.getString(col++),
                        results.getBoolean(col++));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result adminUsers(Request request, User user) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(header("User: " + user.getName()));
        sb.append("User admin tasks here");
        Result result = new Result("User:" + user.getName(), sb);
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
    public Result adminHarvesters(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(header("Harvesters"));
        sb.append("<table>");
        sb.append(HtmlUtil.row(HtmlUtil.cols(HtmlUtil.bold("Name"),
                                             HtmlUtil.bold("Active"), "")));
        if(request.defined(ARG_WHAT)) {
            String what = request.getString(ARG_WHAT,"");
            int id = request.get(ARG_ID,0);
            Harvester harvester = harvesters.get(id);
            if(what.equals("stop")) {
                harvester.setActive(false);
            } else if(what.equals("start")) {
                if(!harvester.getActive()) 
                    Misc.run(harvester,"run");
            }
        }


        int cnt = 0;
        for (Harvester harvester : harvesters) {
            String run;
            if(harvester.getActive()) {
                run = HtmlUtil.href(HtmlUtil.url(URL_ADMIN_HARVESTERS,ARG_WHAT,"stop", ARG_ID,""+cnt),"Stop");
            } else {
                run = HtmlUtil.href(HtmlUtil.url(URL_ADMIN_HARVESTERS,ARG_WHAT,"start", ARG_ID,""+cnt),"Start");
            }
            cnt++;
            sb.append(HtmlUtil.row(HtmlUtil.cols(harvester.getName(),
                    harvester.getActive() + " " + run, harvester.getExtraInfo())));
        }
        sb.append("</table>");

        Result result = new Result("Harvesters", sb);
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
    public Result adminUsers(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(header("Users"));
        String userId = request.getUser();
        if (userId != null) {
            User user = findUser(userId);
            if (user == null) {
                throw new IllegalArgumentException("Could not find user:"
                        + userId);
            }
            return adminUsers(request, user);
        } else {
            String query = SqlUtil.makeSelect(COLUMNS_USERS,
                               Misc.newList(TABLE_USERS));

            SqlUtil.Iterator iter = SqlUtil.getIterator(execute(query));
            ResultSet        results;

            List<User>       users = new ArrayList();
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    users.add(getUser(results));
                }
            }
            sb.append("<table>");
            sb.append(HtmlUtil.row(HtmlUtil.cols(HtmlUtil.bold("ID"),
                    HtmlUtil.bold("Name"), HtmlUtil.bold("Admin?"))));
            for (User user : users) {
                sb.append(HtmlUtil
                    .row(HtmlUtil
                        .cols(HtmlUtil
                            .href(HtmlUtil
                                .url(URL_ADMIN_USERS, ARG_USER,
                                     user.getId()), user.getId()), user
                                         .getName(), "" + user.getAdmin())));
            }
            sb.append("</table>");
        }
        Result result = new Result("Users", sb);
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
    public Result adminStats(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(header("Repository Statistics"));
        sb.append("<table>\n");
        String[] names = { "Users", "Tags", "Groups", "Associations" };
        String[] tables = { TABLE_USERS, TABLE_TAGS, TABLE_GROUPS,
                            TABLE_ASSOCIATIONS };
        for (int i = 0; i < tables.length; i++) {
            sb.append(HtmlUtil.row(HtmlUtil.cols(""
                    + getCount(tables[i].toLowerCase(), ""), names[i])));
        }


        sb.append(
            HtmlUtil.row("<td colspan=\"2\">&nbsp;<p><b>Types:</b></td>"));
        int total = 0;
        sb.append(HtmlUtil.row(HtmlUtil.cols("" + getCount(TABLE_ENTRIES,
                ""), "Total entries")));
        for (Enumeration keys = typeHandlersMap.keys();
                keys.hasMoreElements(); ) {
            String id = (String) keys.nextElement();
            if (id.equals(TypeHandler.TYPE_ANY)) {
                continue;
            }
            TypeHandler typeHandler = (TypeHandler) typeHandlersMap.get(id);
            int cnt = getCount(TABLE_ENTRIES, "type=" + SqlUtil.quote(id));
            String url = HtmlUtil.href(HtmlUtil.url(URL_SEARCHFORM, ARG_TYPE,
                             id), typeHandler.getDescription());
            sb.append(HtmlUtil.row(HtmlUtil.cols("" + cnt, url)));
        }



        sb.append("</table>\n");

        Result result = new Result("Repository Statistics", sb);
        result.putProperty(PROP_NAVSUBLINKS, getAdminLinks(request));
        return result;
    }




    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    protected String header(String h) {
        return "<H3>" + h + "</h3>";
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
    public Result adminSql(Request request) throws Exception {
        String query = (String) request.getUnsafeString(ARG_QUERY,
                           (String) null);
        StringBuffer sb = new StringBuffer();
        sb.append(header("SQL"));
        sb.append(HtmlUtil.form(URL_ADMIN_SQL));
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
     *
     * @param request _more_
     * @param what _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getOutputTypesFor(Request request, String what)
            throws Exception {
        List list = new ArrayList();
        for (OutputHandler outputHandler : outputHandlers) {
            list.addAll(outputHandler.getOutputTypesFor(request, what));
        }
        return list;
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
    protected List getOutputTypesForEntries(Request request)
            throws Exception {
        List list = new ArrayList();
        for (OutputHandler outputHandler : outputHandlers) {
            list.addAll(outputHandler.getOutputTypesForEntries(request));
        }
        return list;
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
    protected OutputHandler getOutputHandler(Request request)
            throws Exception {
        for (OutputHandler outputHandler : outputHandlers) {
            if (outputHandler.canHandle(request)) {
                return outputHandler;
            }
        }
        throw new IllegalArgumentException(
            "Could not find output handler for: " + request.getOutput());
    }

    /**
     * _more_
     */
    protected void initTypeHandlers() {
        addTypeHandler(TypeHandler.TYPE_ANY,
                       new TypeHandler(this, TypeHandler.TYPE_ANY,
                                       "Any file types"));
        addTypeHandler("file", new TypeHandler(this, "file", "Files"));
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
        String type = request.getType(TypeHandler.TYPE_ANY).trim();
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


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processShowList(Request request) throws Exception {
        StringBuffer         sb           = new StringBuffer();
        List                 links        = getListLinks(request, "", false);
        TypeHandler          typeHandler  = getTypeHandler(request);
        List<TwoFacedObject> typeList     = new ArrayList<TwoFacedObject>();
        List<TwoFacedObject> specialTypes = typeHandler.getListTypes(false);
        if (specialTypes.size() > 0) {
            sb.append("<b>" + typeHandler.getDescription() + ":</b>");
        }
        typeList.addAll(specialTypes);
        /*
        if(typeList.size()>0) {
            sb.append("<ul>");
            for(TwoFacedObject tfo: typeList) {
                sb.append("<li>");
                sb.append(HtmlUtil.href(HtmlUtil.url(URL_LIST_SHOW,ARG_WHAT, tfo.getId(),ARG_TYPE,,typeHandler.getType()) , tfo.toString())));
                sb.append("\n");
            }
            sb.append("</ul>");
        }
        sb.append("<p><b>Basic:</b><ul><li>");
        */
        sb.append("<ul><li>");
        sb.append(StringUtil.join("<li>", links));
        sb.append("</ul>");



        Result result = new Result("Lists", sb);
        result.putProperty(PROP_NAVSUBLINKS, getListLinks(request, "", true));
        return result;

    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param what _more_
     * @param includeExtra _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getListLinks(Request request, String what,
                                boolean includeExtra)
            throws Exception {
        List                 links       = new ArrayList();
        TypeHandler          typeHandler = getTypeHandler(request);
        List<TwoFacedObject> typeList    = typeHandler.getListTypes(false);
        String               extra1      = " class=subnavnolink ";
        String               extra2      = " class=subnavlink ";
        if ( !includeExtra) {
            extra1 = "";
            extra2 = "";
        }
        if (typeList.size() > 0) {
            for (TwoFacedObject tfo : typeList) {
                if (what.equals(tfo.getId())) {
                    links.add(HtmlUtil.span(tfo.toString(), extra1));
                } else {
                    links.add(HtmlUtil.href(HtmlUtil.url(URL_LIST_SHOW,
                            ARG_WHAT, (String) tfo.getId(), ARG_TYPE,
                            (String) typeHandler.getType()), tfo.toString(),
                                extra2));
                }
            }
        }
        String typeAttr = "";
        if ( !typeHandler.getType().equals(TypeHandler.TYPE_ANY)) {
            typeAttr = "&type=" + typeHandler.getType();
        }


        String[] whats = { WHAT_TYPE, WHAT_GROUP, WHAT_TAG,
                           WHAT_ASSOCIATION };
        String[] names = { "Types", "Groups", "Tags", "Associations" };
        for (int i = 0; i < whats.length; i++) {
            if (what.equals(whats[i])) {
                links.add(HtmlUtil.span(names[i], extra1));
            } else {
                links.add(HtmlUtil.href(HtmlUtil.url(URL_LIST_SHOW, ARG_WHAT,
                        whats[i]) + typeAttr, names[i], extra2));
            }
        }

        return links;
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
    public Result processSearchForm(Request request) throws Exception {
        return processSearchForm(request, false);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param typeSpecific _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSearchForm(Request request, boolean typeSpecific)
            throws Exception {
        boolean basicForm = request.get(ARG_FORM_BASIC,true);
        List         where        = assembleWhereClause(request);
        StringBuffer sb           = new StringBuffer();
        StringBuffer headerBuffer = new StringBuffer();
        //        headerBuffer.append(header("Search Form"));
        request.remove(ARG_FORM_BASIC);
        String urlArgs = request.getUrlArgs();

        headerBuffer.append("<table cellpadding=\"5\">");
        if(basicForm) {
            headerBuffer.append(HtmlUtil.tableEntry("",HtmlUtil.href(URL_SEARCHFORM+"?" + ARG_FORM_BASIC+"=false&" + urlArgs, "Advanced Search")));
        } else {
            headerBuffer.append(HtmlUtil.tableEntry("",HtmlUtil.href(URL_SEARCHFORM+"?" + ARG_FORM_BASIC+"=true&" + urlArgs, "Basic Search")));
        }

        sb.append(HtmlUtil.form(HtmlUtil.url(URL_QUERY, ARG_NAME,
                                             WHAT_ENTRIES)));

        sb.append(HtmlUtil.hidden(ARG_FORM_BASIC,basicForm+""));

        //Put in an empty submit button so when the user presses return 
        //it acts like a regular submit (not a submit to change the type)
        sb.append(HtmlUtil.submitImage(getUrlBase() + "/blank.gif",
                                       "submit"));
        TypeHandler typeHandler = getTypeHandler(request);

        String      what        = (String) request.getWhat("");
        if (what.length() == 0) {
            what = WHAT_ENTRIES;
        }

        List whatList = Misc.toList(new Object[] {
                            new TwoFacedObject("Entries", WHAT_ENTRIES),
                            new TwoFacedObject("Data Types", WHAT_TYPE),
                            new TwoFacedObject("Groups", WHAT_GROUP),
                            new TwoFacedObject("Tags", WHAT_TAG),
                            new TwoFacedObject("Associations",
                                WHAT_ASSOCIATION) });
        whatList.addAll(typeHandler.getListTypes(true));

        String output     = (String) request.getOutput("");
        String outputHtml = "";
        if(!basicForm) {
            if (output.length() == 0) {
                outputHtml = HtmlUtil.bold("Output Type: ")
                    + HtmlUtil.select(ARG_OUTPUT,
                                      getOutputTypesFor(request, what));
            } else {
                outputHtml = HtmlUtil.bold("Output Type: ") + output;
                sb.append(HtmlUtil.hidden(ARG_OUTPUT,output));
            }
        }


        if (what.length() == 0) {
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Search For:"),
                                          HtmlUtil.select(ARG_WHAT, whatList)
                                          + "&nbsp;&nbsp;&nbsp;"
                                          + outputHtml));

        } else {
            String label = TwoFacedObject.findLabel(what, whatList);
            label = StringUtil.padRight(label, 40, "&nbsp;");
            sb.append(HtmlUtil.tableEntry(HtmlUtil.bold("Search For:"),
                                          label + "&nbsp;&nbsp;"
                                          + outputHtml));
            sb.append(HtmlUtil.hidden(ARG_WHAT, what));
        }


        typeHandler.addToSearchForm(sb, headerBuffer, request, where, basicForm);


        sb.append(HtmlUtil.tableEntry("",
                                      HtmlUtil.submit("Search", "submit")
                                      + " "
                                      + HtmlUtil.submit("Search Subset",
                                          "submit_subset")));
        sb.append("</table>");
        sb.append("</form>");
        headerBuffer.append(sb.toString());

        Result result = new Result("Search Form", headerBuffer);
        result.putProperty(PROP_NAVSUBLINKS,
                           getSearchFormLinks(request, what));
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
        List   links = new ArrayList();
        String extra = " class=\"navlink\" ";
        links.add(HtmlUtil.href(URL_ADMIN_HOME, "Home", extra));
        links.add(HtmlUtil.href(URL_ADMIN_DB, "Database", extra));
        links.add(HtmlUtil.href(URL_ADMIN_STATS, "Statistics", extra));
        links.add(HtmlUtil.href(URL_ADMIN_USERS, "Users", extra));
        links.add(HtmlUtil.href(URL_ADMIN_HARVESTERS, "Harvesters", extra));
        links.add(HtmlUtil.href(URL_ADMIN_SQL, "SQL", extra));

        return links;
    }






    /**
     * _more_
     *
     *
     * @param request _more_
     * @param what _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getSearchFormLinks(Request request, String what)
            throws Exception {
        TypeHandler typeHandler = getTypeHandler(request);
        List        links       = new ArrayList();
        String      extra1      = " class=subnavnolink ";
        String      extra2      = " class=subnavlink ";
        String[] whats = { WHAT_ENTRIES, WHAT_GROUP, WHAT_TAG,
                           WHAT_ASSOCIATION };
        String[] names = { "Entries", "Groups", "Tags", "Associations" };

        for (int i = 0; i < whats.length; i++) {
            String item;
            if (what.equals(whats[i])) {
                item = HtmlUtil.span(names[i], extra1);
            } else {
                item = HtmlUtil.href(HtmlUtil.url(URL_SEARCHFORM, ARG_WHAT,
                        whats[i]), names[i], extra2);
            }
            if (i == 0) {
                item = "<span " + extra1
                       + ">Search For:&nbsp;&nbsp;&nbsp; </span>" + item;
            }
            links.add(item);
        }

        List<TwoFacedObject> whatList = typeHandler.getListTypes(false);
        for (TwoFacedObject tfo : whatList) {
            if (tfo.getId().equals(what)) {
                links.add(HtmlUtil.span(tfo.toString(), extra1));
            } else {
                links.add(HtmlUtil.href(HtmlUtil.url(URL_SEARCHFORM,
                        ARG_WHAT, "" + tfo.getId(), ARG_TYPE,
                        typeHandler.getType()), tfo.toString(), extra2));
            }
        }

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
        List    links   = new ArrayList();
        String  extra   = " class=navlink ";
        boolean isAdmin = false;
        if (request != null) {
            RequestContext context = request.getRequestContext();
            User           user    = context.getUser();
            isAdmin = user.getAdmin();
        }

        for (ApiMethod apiMethod : topLevelMethods) {
            if (apiMethod.getPermission().getMustBeAdmin() && !isAdmin) {
                continue;
            }
            links.add(HtmlUtil.href(fileUrl(apiMethod.getRequest()),
                                    apiMethod.getName(), extra));
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
        return request.get(ARG_MAX, MAX_ROWS);
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
    public Result processList(Request request) throws Exception {
        String what   = request.getWhat(WHAT_TYPE);
        Result result = null;
        if (what.equals(WHAT_GROUP)) {
            result = listGroups(request);
        } else if (what.equals(WHAT_TAG)) {
            result = listTags(request);
        } else if (what.equals(WHAT_ASSOCIATION)) {
            result = listAssociations(request);
        } else if (what.equals(WHAT_TYPE)) {
            result = listTypes(request);
        } else {
            TypeHandler typeHandler = getTypeHandler(request);
            result = typeHandler.processList(request, what);
        }
        result.putProperty(PROP_NAVSUBLINKS,
                           getListLinks(request, what, true));
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
        String entryId = (String) request.getId((String) null);
        if (entryId == null) {
            throw new IllegalArgumentException("No " + ARG_ID + " given");
        }
        Entry entry = getEntry(entryId, request);
        if (entry == null) {
            throw new IllegalArgumentException("Could not find entry with id:"
                    + entryId);
        }



        if ( !entry.getTypeHandler().isEntryDownloadable(request, entry)) {
            throw new IllegalArgumentException(
                "Cannot download file with id:" + entryId);
        }

        byte[] bytes;
        //        System.err.println("request:" + request);
        if (request.defined(ARG_IMAGEWIDTH)
                && ImageUtils.isImage(entry.getFile())) {
            int width = request.get(ARG_IMAGEWIDTH, 75);
            String thumb = IOUtil.joinDir(tmpDir,
                                          entry.getId() + "_" + width
                                          + ".jpg");
            Image image = ImageUtils.readImage(entry.getFile());
            Image resizedImage = image.getScaledInstance(width, -1,
                                     Image.SCALE_AREA_AVERAGING);
            ImageUtils.waitOnImage(resizedImage);
            System.err.println("thumb:" + thumb);
            //            GuiUtils.showOkCancelDialog(null,"",new JLabel(new ImageIcon(resizedImage)),null);
            //            System.err.println ("AFTER");
            ImageUtils.writeImageToFile(resizedImage, thumb);
            bytes = IOUtil.readBytes(IOUtil.getInputStream(thumb,
                    getClass()));
            return new Result("", bytes,
                              IOUtil.getFileExtension(entry.getFile()));
        } else {
            return new Result(
                "", IOUtil.getInputStream(entry.getFile(), getClass()),
                IOUtil.getFileExtension(entry.getFile()));
        }

    }

    /** _more_ */
    PreparedStatement entryStmt;

    /**
     * _more_
     *
     *
     * @param entryId _more_
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Entry getEntry(String entryId, Request request)
            throws Exception {
        if (entryStmt == null) {
            String query = SqlUtil.makeSelect(COLUMNS_ENTRIES,
                               Misc.newList(TABLE_ENTRIES),
                               SqlUtil.eq(COL_ENTRIES_ID, "?"));
            entryStmt = connection.prepareStatement(query);

        }
        /*
        String query = SqlUtil.makeSelect(COLUMNS_ENTRIES,
                                          Misc.newList(TABLE_ENTRIES),
                                          SqlUtil.eq(COL_ENTRIES_ID,
                                          SqlUtil.quote(entryId)));*/
        //        ResultSet results = execute(query).getResultSet();
        entryStmt.setString(1, entryId);
        entryStmt.execute();
        ResultSet results = entryStmt.getResultSet();
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
        String entryId = (String) request.getId((String) null);
        if (entryId == null) {
            throw new IllegalArgumentException("No " + ARG_ID + " given");
        }
        Entry entry = getEntry(entryId, request);
        if (entry == null) {
            throw new IllegalArgumentException("Could not find entry");
        }

        System.err.println (request);
        if(request.get(ARG_NEXT,false) || request.get(ARG_PREVIOUS,false)) {
            boolean next = request.get(ARG_NEXT,false);
            String[]ids = getEntryIdsInGroup(request, entry.getGroup(),  new ArrayList());
            String nextId = null;
            for(int i=0;i<ids.length && nextId ==null;i++) {
                if(ids[i].equals(entryId)) {
                    if(next) {
                        if(i==ids.length-1) nextId = ids[0];
                        else nextId = ids[i+1];
                    } else {
                        if(i==0) nextId = ids[ids.length-1];
                        else nextId = ids[i-1];
                    }
                }
            }
            //Do a redirect
            if(nextId!=null) {
                return new Result(HtmlUtil.url(URL_SHOWENTRY,ARG_ID, nextId,ARG_OUTPUT, request.getString(ARG_OUTPUT,OutputHandler.OUTPUT_HTML)));
            }
        }
        return getOutputHandler(request).processShowEntry(request, entry);
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
    public Entry filterEntry(Request request, Entry entry) throws Exception {
        //TODO: Check for access
        return entry;
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
        List<Entry> entries    = new ArrayList();
        boolean     doAll      = request.defined("getall");
        boolean     doSelected = request.defined("getselected");
        String      prefix     = (doAll
                                  ? "all_"
                                  : "entry_");

        for (Enumeration keys = request.keys(); keys.hasMoreElements(); ) {
            String id = (String) keys.nextElement();
            if (doSelected) {
                if ( !request.get(id, false)) {
                    continue;
                }
            }
            if ( !id.startsWith(prefix)) {
                continue;
            }
            id = id.substring(prefix.length());
            Entry entry = getEntry(id, request);
            if (entry != null) {
                entries.add(entry);
            }
        }
        String ids = request.getIds((String) null);
        if (ids != null) {
            List<String> idList = StringUtil.split(ids, ",", true, true);
            for (String id : idList) {
                Entry entry = getEntry(id, request);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        entries = filterEntries(request, entries);
        return getOutputHandler(request).processEntries(request, entries);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    protected long currentTime() {
        return new Date().getTime();

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
        Group group = null;
        String groupName = (String) request.getString(ARG_GROUP,
                               (String) null);
        if (groupName != null) {
            group = findGroupFromName(groupName);
        }
        OutputHandler outputHandler = getOutputHandler(request);
        if (group == null) {
            Statement stmt = execute(SqlUtil.makeSelect(COL_GROUPS_ID,
                                 Misc.newList(TABLE_GROUPS),
                                 COL_GROUPS_PARENT + " IS NULL"));
            return outputHandler.processShowGroups(request,
                    getGroups(SqlUtil.readString(stmt, 1)));
        }

        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        List<Group> subGroups = getGroups(SqlUtil.makeSelect(COL_GROUPS_ID,
                                    Misc.newList(TABLE_GROUPS),
                                    SqlUtil.eq(COL_GROUPS_PARENT,
                                        SqlUtil.quote(group.getId()))));


        String[]ids = getEntryIdsInGroup(request, group,  where);

        List<Entry> entries = new ArrayList();
        for(int i=0;i<ids.length;i++) {
            Entry  entry = getEntry(ids[i], request);
            if (entry != null) {
                entries.add(entry);
            }
        }
        entries = filterEntries(request, entries);
        return outputHandler.processShowGroup(request, group, subGroups,
                entries);
    }


    protected String[] getEntryIdsInGroup(Request request, Group group, List where) throws Exception {
        where = new ArrayList(where);
        where.add(SqlUtil.eq(COL_ENTRIES_GROUP_ID,
                             SqlUtil.quote(group.getId())));
        TypeHandler typeHandler = getTypeHandler(request);
        Statement stmt = typeHandler.executeSelect(request,
                                                   COL_ENTRIES_ID, where);

        return SqlUtil.readString(stmt,1);
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
    public String getResource(String id) throws Exception {
        String resource = (String) resources.get(id);
        if (resource != null) {
            return resource;
        }
        String fromProperties = getProperty(id);
        if (fromProperties != null) {
            resource = IOUtil.readContents(fromProperties, getClass());
        } else {
            resource = IOUtil.readContents(id, getClass());
        }
        if (resource != null) {
            //            resources.put(id,resource);
        }
        return resource;
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
        String graphAppletTemplate = getResource(PROP_HTML_GRAPHAPPLET);
        String type = request.getString(ARG_NODETYPE, NODETYPE_GROUP);
        String id                  = request.getId((String) null);

        if ((type == null) || (id == null)) {
            throw new IllegalArgumentException(
                "no type or id argument specified");
        }
        String html = StringUtil.replace(graphAppletTemplate, "${id}",
                                         encode(id));
        html = StringUtil.replace(html, "${root}", getUrlBase());
        html = StringUtil.replace(html, "${type}", encode(type));
        return new Result("Graph View", html.getBytes(), Result.TYPE_HTML);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param results _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getEntryNodeXml(Request request, ResultSet results)
            throws Exception {
        int         col         = 1;
        String      entryId      = results.getString(col++);
        String      name        = results.getString(col++);
        String      fileType    = results.getString(col++);
        String      groupId     = results.getString(col++);
        String      file        = results.getString(col++);
        TypeHandler typeHandler = getTypeHandler(request);
        String      nodeType    = typeHandler.getNodeType();
        if (ImageUtils.isImage(file)) {
            nodeType = "imageentry";
        }
        String attrs = XmlUtil.attrs(ATTR_TYPE, nodeType, ATTR_ID, entryId,
                                     ATTR_TITLE, name);
        if (ImageUtils.isImage(file)) {
            String imageUrl =
                HtmlUtil.url(URL_GETENTRY + entryId
                             + IOUtil.getFileExtension(file), ARG_ID, entryId,
                                 ARG_IMAGEWIDTH, "75");
            attrs = attrs + " " + XmlUtil.attr("image", imageUrl);
        }
        //        System.err.println (XmlUtil.tag(TAG_NODE,attrs));
        return XmlUtil.tag(TAG_NODE, attrs);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entryId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<Tag> getTags(Request request, String entryId)
            throws Exception {
        String tagQuery = SqlUtil.makeSelect(COL_TAGS_NAME,
                                             Misc.newList(TABLE_TAGS),
                                             SqlUtil.eq(COL_TAGS_ENTRY_ID,
                                                 SqlUtil.quote(entryId)));
        String[]  tags    = SqlUtil.readString(execute(tagQuery), 1);
        List<Tag> tagList = new ArrayList();
        for (int i = 0; i < tags.length; i++) {
            tagList.add(new Tag(tags[i]));
        }
        return tagList;
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

        String  graphXmlTemplate = getResource(PROP_HTML_GRAPHTEMPLATE);
        String  id               = (String) request.getId((String) null);
        String  originalId       = id;
        String  type = (String) request.getString(ARG_NODETYPE,
                           (String) null);
        int     skip             = request.get(ARG_SKIP, 0);
        boolean haveSkip         = false;
        if (id.startsWith("skip_")) {
            haveSkip = true;
            //skip_tag_" +(cnt+skip)+"_"+id;
            List toks = StringUtil.split(id, "_", true, true);
            type = (String) toks.get(1);
            skip = new Integer((String) toks.get(2)).intValue();
            toks.remove(0);
            toks.remove(0);
            toks.remove(0);
            id = StringUtil.join("_", toks);
        }

        int MAX_EDGES = 15;
        if (id == null) {
            throw new IllegalArgumentException("Could not find id:"
                    + request);
        }
        if (type == null) {
            type = NODETYPE_GROUP;
        }
        TypeHandler  typeHandler = getTypeHandler(request);
        StringBuffer sb          = new StringBuffer();
        if (type.equals(TYPE_TAG)) {
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, TYPE_TAG, ATTR_ID,
                                      originalId, ATTR_TITLE, originalId)));

            Statement stmt =
                typeHandler.executeSelect(
                    request,
                    SqlUtil.comma(
                        COL_ENTRIES_ID, COL_ENTRIES_NAME, COL_ENTRIES_TYPE,
                        COL_ENTRIES_GROUP_ID, COL_ENTRIES_FILE), Misc.newList(
                            SqlUtil.eq(COL_TAGS_ENTRY_ID, COL_ENTRIES_ID),
                            SqlUtil.eq(
                                COL_TAGS_NAME,
                                SqlUtil.quote(id))), " order by " + COL_ENTRIES_FROMDATE);

            SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
            ResultSet        results;
            int              cnt       = 0;
            int              actualCnt = 0;
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    cnt++;
                    if (cnt <= skip) {
                        continue;
                    }
                    actualCnt++;
                    sb.append(getEntryNodeXml(request, results));
                    sb.append(XmlUtil.tag(TAG_EDGE,
                                          XmlUtil.attrs(ATTR_TYPE,
                                              "taggedby", ATTR_FROM,
                                                  originalId, ATTR_TO,
                                                      results.getString(1))));

                    if (actualCnt >= MAX_EDGES) {
                        String skipId = "skip_" + type + "_"
                                        + (actualCnt + skip) + "_" + id;
                        sb.append(XmlUtil.tag(TAG_NODE,
                                XmlUtil.attrs(ATTR_TYPE, "skip", ATTR_ID,
                                    skipId, ATTR_TITLE, "...")));
                        sb.append(XmlUtil.tag(TAG_EDGE,
                                XmlUtil.attrs(ATTR_TYPE, "etc", ATTR_FROM,
                                    originalId, ATTR_TO, skipId)));
                        break;
                    }
                }
            }
            String xml = StringUtil.replace(graphXmlTemplate, "${content}",
                                            sb.toString());
            xml = StringUtil.replace(xml, "${root}", getUrlBase());
            return new Result("", new StringBuffer(xml),
                              getMimeTypeFromSuffix(".xml"));
        }


        if ( !type.equals(TYPE_GROUP)) {
            Statement stmt = typeHandler.executeSelect(
                                 request,
                                 SqlUtil.comma(
                                     COL_ENTRIES_ID, COL_ENTRIES_NAME,
                                     COL_ENTRIES_TYPE, COL_ENTRIES_GROUP_ID,
                                     COL_ENTRIES_FILE), Misc.newList(
                                         SqlUtil.eq(
                                             COL_ENTRIES_ID,
                                             SqlUtil.quote(id))));

            ResultSet results = stmt.getResultSet();
            if ( !results.next()) {
                throw new IllegalArgumentException("Unknown entry id:" + id);
            }

            sb.append(getEntryNodeXml(request, results));

            List<Association> associations = getAssociations(request, id);
            for (Association association : associations) {
                Entry   other  = null;
                boolean isTail = true;
                if (association.getFromId().equals(id)) {
                    other  = getEntry(association.getToId(), request);
                    isTail = true;
                } else {
                    other  = getEntry(association.getFromId(), request);
                    isTail = false;
                }

                if (other != null) {
                    sb.append(
                        XmlUtil.tag(
                            TAG_NODE,
                            XmlUtil.attrs(
                                ATTR_TYPE,
                                other.getTypeHandler().getNodeType(),
                                ATTR_ID, other.getId(), ATTR_TITLE,
                                other.getName())));
                    sb.append(XmlUtil.tag(TAG_EDGE,
                                          XmlUtil.attrs(ATTR_TYPE,
                                              "association", ATTR_FROM,
                                                  (isTail
                            ? id
                            : other.getId()), ATTR_TO, (isTail
                            ? other.getId()
                            : id))));
                }
            }



            Group group = findGroup(results.getString(4));
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, NODETYPE_GROUP,
                                      ATTR_ID, group.getFullName(),
                                      ATTR_TITLE, group.getFullName())));
            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, group.getFullName(),
                                      ATTR_TO, results.getString(1))));

            for (Tag tag : getTags(request, id)) {
                sb.append(XmlUtil.tag(TAG_NODE,
                                      XmlUtil.attrs(ATTR_TYPE, TYPE_TAG,
                                          ATTR_ID, tag.getName(), ATTR_TITLE,
                                          tag.getName())));
                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TYPE, "taggedby",
                                          ATTR_FROM, tag.getName(), ATTR_TO,
                                          id)));
            }




            String xml = StringUtil.replace(graphXmlTemplate, "${content}",
                                            sb.toString());

            xml = StringUtil.replace(xml, "${root}", getUrlBase());
            return new Result("", new StringBuffer(xml),
                              getMimeTypeFromSuffix(".xml"));
        }

        Group group = findGroupFromName(id);
        if (group == null) {
            throw new IllegalArgumentException("Could not find group:" + id);
        }
        sb.append(XmlUtil.tag(TAG_NODE,
                              XmlUtil.attrs(ATTR_TYPE, NODETYPE_GROUP,
                                            ATTR_ID, group.getFullName(),
                                            ATTR_TITLE,
                                            group.getFullName())));
        List<Group> subGroups = getGroups(SqlUtil.makeSelect(COL_GROUPS_ID,
                                    Misc.newList(TABLE_GROUPS),
                                    SqlUtil.eq(COL_GROUPS_PARENT,
                                        SqlUtil.quote(group.getId()))));

        Group parent = group.getParent();
        if (parent != null) {
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, NODETYPE_GROUP,
                                      ATTR_ID, parent.getFullName(),
                                      ATTR_TITLE, parent.getFullName())));
            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, parent.getFullName(),
                                      ATTR_TO, group.getFullName())));
        }


        for (Group subGroup : subGroups) {

            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, NODETYPE_GROUP,
                                      ATTR_ID, subGroup.getFullName(),
                                      ATTR_TITLE, subGroup.getFullName())));

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
        int              cnt       = 0;
        int              actualCnt = 0;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                cnt++;
                if (cnt <= skip) {
                    continue;
                }
                actualCnt++;
                sb.append(getEntryNodeXml(request, results));
                String entryId = results.getString(1);
                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                          ATTR_FROM, (haveSkip
                        ? originalId
                        : group.getFullName()), ATTR_TO, entryId)));
                sb.append("\n");
                if (actualCnt >= MAX_EDGES) {
                    String skipId = "skip_" + type + "_" + (actualCnt + skip)
                                    + "_" + id;
                    sb.append(XmlUtil.tag(TAG_NODE,
                                          XmlUtil.attrs(ATTR_TYPE, "skip",
                                              ATTR_ID, skipId, ATTR_TITLE,
                                                  "...")));
                    sb.append(XmlUtil.tag(TAG_EDGE,
                                          XmlUtil.attrs(ATTR_TYPE, "etc",
                                              ATTR_FROM, originalId, ATTR_TO,
                                                  skipId)));
                    break;
                }
            }
        }
        String xml = StringUtil.replace(graphXmlTemplate, "${content}",
                                        sb.toString());
        xml = StringUtil.replace(xml, "${root}", getUrlBase());
        return new Result("", new StringBuffer(xml),
                          getMimeTypeFromSuffix(".xml"));

    }



    /**
     * _more_
     *
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listGroups(Request request) throws Exception {
        TypeHandler typeHandler = getTypeHandler(request);
        Statement statement = typeHandler.executeSelect(request,
                                  SqlUtil.distinct(COL_ENTRIES_GROUP_ID));
        String[]    groups    = SqlUtil.readString(statement, 1);
        List<Group> groupList = new ArrayList();
        for (int i = 0; i < groups.length; i++) {
            Group group = findGroup(groups[i]);
            if (group == null) {
                continue;
            }
            groupList.add(group);
        }
        return getOutputHandler(request).processShowGroups(request,
                                groupList);
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
                             SqlUtil.distinct(COL_ENTRIES_TYPE), where);
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
        List<TypeHandler> typeHandlers = getTypeHandlers(request);
        return getOutputHandler(request).listTypes(request, typeHandlers);
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
        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        System.err.println ("where:" + where);
        if (where.size() > 0) {
            where.add(0, SqlUtil.eq(COL_TAGS_ENTRY_ID, COL_ENTRIES_ID));
        }

        String[] tags = SqlUtil.readString(typeHandler.executeSelect(request,
                            SqlUtil.distinct(COL_TAGS_NAME), where,
                            " order by " + COL_TAGS_NAME), 1);

        List<Tag>     tagList = new ArrayList();
        List<String>  names   = new ArrayList<String>();
        List<Integer> counts  = new ArrayList<Integer>();
        ResultSet     results;
        int           max = -1;
        int           min = -1;
        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i];
            Statement stmt2 = typeHandler.executeSelect(request,
                                  SqlUtil.count("*"),
                                  Misc.newList(SqlUtil.eq(COL_TAGS_NAME,
                                      SqlUtil.quote(tag))));

            ResultSet results2 = stmt2.getResultSet();
            if ( !results2.next()) {
                continue;
            }
            int count = results2.getInt(1);
            if ((max < 0) || (count > max)) {
                max = count;
            }
            if ((min < 0) || (count < min)) {
                min = count;
            }
            tagList.add(new Tag(tag, count));
        }

        return getOutputHandler(request).listTags(request, tagList);
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
        return getOutputHandler(request).listAssociations(request);
    }




    /**
     * _more_
     *
     * @param suffix _more_
     *
     * @return _more_
     */
    protected String getMimeTypeFromSuffix(String suffix) {
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
            makeUser(user, true);
        }
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param tableName _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected boolean tableContains(String id, String tableName,
                                    String column)
            throws Exception {
        String query = SqlUtil.makeSelect(column, Misc.newList(tableName),
                                          SqlUtil.eq(column,
                                              SqlUtil.quote(id)));
        ResultSet results = execute(query).getResultSet();
        return results.next();
    }


    /**
     * _more_
     *
     * @param user _more_
     *
     * @throws Exception _more_
     */
    protected void deleteUser(User user) throws Exception {
        String query = SqlUtil.makeDelete(TABLE_USERS, COL_USERS_ID,
                                          SqlUtil.quote(user.getId()));
        execute(query);
    }

    /**
     * _more_
     *
     * @param user _more_
     * @param updateIfNeeded _more_
     *
     * @throws Exception _more_
     */
    protected void makeUser(User user, boolean updateIfNeeded)
            throws Exception {
        if (tableContains(user.getId(), TABLE_USERS, COL_USERS_ID)) {
            if ( !updateIfNeeded) {
                throw new IllegalArgumentException(
                    "Database already contains user:" + user.getId());
            }
            String query = SqlUtil.makeUpdate(TABLE_USERS, COL_USERS_ID,
                               SqlUtil.quote(user.getId()),
                               new String[] { COL_USERS_NAME,
                    COL_USERS_ADMIN }, new String[] {
                        SqlUtil.quote(user.getName()), (user.getAdmin()
                    ? "1"
                    : "0") });
            execute(query);
            return;
        }

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

    /**
     * _more_
     *
     * @param name _more_
     * @param createIfNeeded _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Group findGroupFromName(String name, boolean createIfNeeded)
            throws Exception {
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
            parent = findGroupFromName(StringUtil.join("/", toks),
                                       createIfNeeded);
            if (parent == null) {
                return null;
            }
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
            if ( !createIfNeeded) {
                return null;
            }
            int    baseId = 0;
            String idWhere;
            if (parent == null) {
                idWhere = COL_GROUPS_PARENT + " IS NULL ";
            } else {
                idWhere = SqlUtil.eq(COL_GROUPS_PARENT,
                                     SqlUtil.quote(parent.getId()));
            }
            String newId = null;
            while (true) {
                if (parent == null) {
                    newId = "" + baseId;
                } else {
                    newId = parent.getId() + Group.IDDELIMITER + baseId;
                }
                ResultSet idResults =
                    execute(SqlUtil.makeSelect(COL_GROUPS_ID,
                        Misc.newList(TABLE_GROUPS),
                        idWhere + " AND "
                        + SqlUtil.eq(COL_GROUPS_ID,
                                     SqlUtil.quote(newId)))).getResultSet();

                if ( !idResults.next()) {
                    break;
                }
                baseId++;
            }
            //            System.err.println ("made id:" + newId);
            //            System.err.println ("last name" + lastName);
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
        Statement statement = typeHandler.executeSelect(request,
                                  COLUMNS_ENTRIES, where,
                                  "order by " + COL_ENTRIES_FROMDATE);
        List<Entry>      entries = new ArrayList<Entry>();
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




    /** _more_ */
    private Hashtable namesHolder = new Hashtable();


    /**
     * _more_
     *
     * @param fieldValue _more_
     * @param namesFile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getFieldDescription(String fieldValue, String namesFile) 
            throws Exception {
        return getFieldDescription(fieldValue, namesFile, null);
    }

    protected String getFieldDescription(String fieldValue, String namesFile, String dflt)
            throws Exception {
        if (namesFile == null) {
            return getLongName(fieldValue);
        }
        Properties names = (Properties) namesHolder.get(namesFile);
        if (names == null) {
            try {
                names = new Properties();
                InputStream s = IOUtil.getInputStream(namesFile, getClass());
                names.load(s);
                namesHolder.put(namesFile, names);
            } catch (Exception exc) {
                System.err.println("err:" + exc);
                throw exc;
            }
        }
        return (String) names.get(fieldValue);
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
                        "/ucar/unidata/repository/resources/names.properties",
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
     * @param s _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String encode(String s) throws Exception {
        return java.net.URLEncoder.encode(s, "UTF-8");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param tag _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getTagLinks(Request request, String tag)
            throws Exception {
        String search = HtmlUtil.href(
                            HtmlUtil.url(
                                URL_SEARCHFORM, ARG_TAG,
                                java.net.URLEncoder.encode(
                                    tag, "UTF-8")), HtmlUtil.img(
                                        fileUrl("/Search16.gif"),
                                        "Search in tag"));

        if (isAppletEnabled(request)) {
            search += HtmlUtil.href(HtmlUtil.url(URL_GRAPHVIEW, ARG_ID, tag,
                    ARG_NODETYPE,
                    TYPE_TAG), HtmlUtil.img(fileUrl("/tree.gif"),
                                            "Show tag in graph"));
        }
        return search;
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getEntryUrl(Entry entry) {
        return HtmlUtil.href(HtmlUtil.url(URL_SHOWENTRY, ARG_ID,
                                          entry.getId()), entry.getName());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entryId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<Association> getAssociations(Request request,
            String entryId)
            throws Exception {
        String query = SqlUtil.makeSelect(
                           COLUMNS_ASSOCIATIONS,
                           Misc.newList(TABLE_ASSOCIATIONS),
                           SqlUtil.eq(
                               COL_ASSOCIATIONS_FROM_ENTRY_ID,
                               SqlUtil.quote(entryId)) + " OR "
                                   + SqlUtil.eq(
                                       COL_ASSOCIATIONS_TO_ENTRY_ID,
                                       SqlUtil.quote(entryId)));
        List<Association> associations = new ArrayList();
        SqlUtil.Iterator  iter         = SqlUtil.getIterator(execute(query));
        ResultSet         results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                associations.add(new Association(results.getString(1),
                        results.getString(2), results.getString(3)));
            }
        }
        return associations;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param association _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getAssociationLinks(Request request, String association)
            throws Exception {
        if (true) {
            return "";
        }
        String search = HtmlUtil.href(
                            HtmlUtil.url(
                                URL_SEARCHFORM, ARG_ASSOCIATION,
                                encode(association)), HtmlUtil.img(
                                    fileUrl("/Search16.gif"),
                                    "Search in association"));

        return search;
    }



    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    public String fileUrl(String f) {
        return urlBase + f;
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
        return HtmlUtil
            .href(HtmlUtil
                .url(URL_GRAPHVIEW, ARG_ID, group.getFullName(),
                     ARG_NODETYPE, NODETYPE_GROUP), HtmlUtil
                         .img(fileUrl("/tree.gif"), "Show group in graph"));
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
        //        System.err.println("submit:" + request.getString("submit","YYY"));
        if (request.defined("submit_type.x")) {
            //            System.err.println("request:" + request.getString("submit_type.x","XXX"));
            request.remove(ARG_OUTPUT);
            return processSearchForm(request);
        }
        if (request.defined("submit_subset")) {
            //            System.err.println("request:" + request.getString("submit_type.x","XXX"));
            request.remove(ARG_OUTPUT);
            return processSearchForm(request);
        }

        String what = request.getWhat(WHAT_ENTRIES);
        if ( !what.equals(WHAT_ENTRIES)) {
            Result result = processList(request);
            if (result == null) {
                throw new IllegalArgumentException("Unknown list request: "
                        + what);
            }
            result.putProperty(PROP_NAVSUBLINKS,
                               getSearchFormLinks(request, what));
            return result;
        }

        List<Entry> entries = getEntries(request);
        return getOutputHandler(request).processEntries(request, entries);
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
        statement.setDouble(col++, entry.getMinLat());
        statement.setDouble(col++, entry.getMaxLat());
        statement.setDouble(col++, entry.getMinLon());
        statement.setDouble(col++, entry.getMaxLon());
    }


    /**
     * _more_
     *
     * @param group _more_
     * @param type _more_
     * @param name _more_
     * @param content _more_
     *
     * @throws Exception _more_
     */
    public void insertMetadata(Group group, String type, String name,
                               String content)
            throws Exception {
        insertMetadata(new Metadata(group.getId(), Metadata.IDTYPE_GROUP,
                                    type, name, content));
    }

    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @throws Exception _more_
     */
    public void insertMetadata(Metadata metadata) throws Exception {
        PreparedStatement metadataInsert =
            connection.prepareStatement(INSERT_METADATA);
        int col = 1;
        metadataInsert.setString(col++, metadata.getId());
        metadataInsert.setString(col++, metadata.getIdType());
        metadataInsert.setString(col++, metadata.getMetadataType());
        metadataInsert.setString(col++, metadata.getName());
        metadataInsert.setString(col++, metadata.getContent());
        metadataInsert.execute();
    }

    /**
     * _more_
     *
     * @param typeHandler _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    public void insertEntries(TypeHandler typeHandler, List<Entry> entries)
            throws Exception {
        if (entries.size() == 0) {
            return;
        }
        clearPageCache();
        System.err.println("Inserting:" + entries.size() + " "
                           + typeHandler.getType() + " entries");
        long t1  = System.currentTimeMillis();
        int  cnt = 0;
        PreparedStatement entryInsert =
            connection.prepareStatement(INSERT_ENTRIES);

        String            sql        = typeHandler.getInsertSql();
        PreparedStatement typeInsert = ((sql == null)
                                        ? null
                                        : connection.prepareStatement(sql));
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
            setStatement(entry, entryInsert);
            entryInsert.addBatch();

            if (typeInsert != null) {
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
                if (typeInsert != null) {
                    typeInsert.executeBatch();
                }
                batchCnt = 0;
            }
            for (Metadata metadata : entry.getMetadata()) {
                insertMetadata(metadata);
            }
        }
        if (batchCnt > 0) {
            entryInsert.executeBatch();
            tagsInsert.executeBatch();
            if (typeInsert != null) {
                typeInsert.executeBatch();
            }
        }
        connection.commit();
        connection.setAutoCommit(true);
        System.err.println("done");
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
            System.err.println("query time:" + (t2 - t1));
        }
        return statement;
    }


    /**
     * _more_
     *
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
     *
     * @throws Exception _more_
     */
    public void loadModelFiles() throws Exception {
        File        rootDir     = new File("/data/ldm/gempak/model");
        TypeHandler typeHandler = getTypeHandler("model");
        List<Entry> entries = harvester.collectModelFiles(rootDir,
                                  "IDD/Model", typeHandler);
        insertEntries(typeHandler, entries);
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void loadSatelliteFiles() throws Exception {
        File        rootDir     = new File("/data/ldm/gempak/images/sat");
        TypeHandler typeHandler = getTypeHandler("satellite");
        List<Entry> entries = harvester.collectSatelliteFiles(rootDir,
                                  "IDD/Satellite", typeHandler);
        insertEntries(typeHandler, entries);
    }



    /**
     * _more_
     *
     * @param harvester _more_
     * @param typeHandler _more_
     * @param entries _more_
     *
     * @return _more_
     */
    public boolean processEntries(Harvester harvester,
                                  TypeHandler typeHandler,
                                  List<Entry> entries) {
        String query = "";
        try {
            if (entries.size() == 0) {
                return true;
            }
            //            long  tt1 = System.currentTimeMillis();
            //            String allQuery = SqlUtil.makeSelect(COL_ENTRIES_FILE,
            //                                                 Misc.newList(TABLE_ENTRIES));
            //            String[] files =  SqlUtil.readString(execute(allQuery), 1);
            //            long  tt2 = System.currentTimeMillis();
            //            System.err.println ("tt:"+ (tt2-tt1) + " #files:" + files.length );


            PreparedStatement select =
                connection.prepareStatement(query =
                    SqlUtil.makeSelect("count(" + COL_ENTRIES_ID + ")",
                                       Misc.newList(TABLE_ENTRIES),
                                       SqlUtil.eq(COL_ENTRIES_FILE, "?")));
            long        t1        = System.currentTimeMillis();
            List<Entry> needToAdd = new ArrayList();
            for (Entry entry : entries) {
                select.setString(1, entry.getFile());
                //                select.addBatch();
                ResultSet results = select.executeQuery();
                if (results.next()) {
                    int found = results.getInt(1);
                    if (found == 0) {
                        needToAdd.add(entry);
                        //                        System.err.println ("adding: " + entry.getFile());
                    }
                }
            }
            /*
            ResultSet results = select.executeBatch();
            int cnt = 0;
            while(results.next()) {
                int found = results.getInt(1);
                if(found ==0) {
                    needToAdd.add(entries.get(cnt));
                    System.err.println ("adding: " + entries.get(cnt).getFile());
                }
                cnt++;
            }
            System.err.println ("#results:" + cnt);
            */
            long t2 = System.currentTimeMillis();
            insertEntries(typeHandler, needToAdd);
            System.err.println("Took:" + (t2 - t1) + "ms to check: "
                               + entries.size());
        } catch (Exception exc) {
            log("Processing:" + query, exc);
            return false;
        }
        return true;
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
        if ( !rootDir.exists()) {
            rootDir = new File("/harpo/jeffmc/src/idv/trunk/ucar/unidata");
        }
        TypeHandler    typeHandler = getTypeHandler("file");
        List<FileInfo> dirs        = new ArrayList();
        List<Entry> entries = harvester.collectFiles(rootDir, "Files",
                                  typeHandler, dirs);
        insertEntries(typeHandler, entries);
        Hashtable javaFiles = new Hashtable();
        for (Entry entry : entries) {
            if (entry.getFile().endsWith(".java")) {
                javaFiles.put(entry.getFile(), entry);
            }
        }
        PreparedStatement associationInsert =
            connection.prepareStatement(INSERT_ASSOCIATIONS);
        for (Entry entry : entries) {
            String f = entry.getFile();
            if (f.endsWith(".class")) {
                int idx = f.indexOf("$");
                if (idx >= 0) {
                    f = f.substring(0, idx) + ".java";
                } else {
                    idx = f.indexOf(".class");
                    f   = f.substring(0, idx) + ".java";
                }
                Entry javaEntry = (Entry) javaFiles.get(f);
                if (javaEntry == null) {
                    //                    System.err.println ("??:" + f);                    
                } else {
                    int col = 1;
                    associationInsert.setString(col++, "compiledfrom");
                    associationInsert.setString(col++, javaEntry.getId());
                    associationInsert.setString(col++, entry.getId());
                    associationInsert.execute();
                }
            }

        }



    }

    /**
     * _more_
     *
     * @param dirs _more_
     */
    public void listen(List<FileInfo> dirs) {
        while (true) {
            for (FileInfo f : dirs) {
                if (f.hasChanged()) {
                    System.err.println("changed:" + f);
                }
            }
            Misc.sleep(1000);
        }
    }







    /**
     * _more_
     *
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Metadata> getMetadata(Group group) throws Exception {
        return getMetadata(group.getId(), Metadata.IDTYPE_GROUP);
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Metadata> getMetadata(Entry entry) throws Exception {
        return getMetadata(entry.getId(), Metadata.IDTYPE_ENTRY);
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Metadata> getMetadata(String id, String type)
            throws Exception {
        String query = SqlUtil.makeSelect(
                           COLUMNS_METADATA, Misc.newList(TABLE_METADATA),
                           SqlUtil.makeAnd(
                               Misc.newList(
                                   SqlUtil.eq(
                                       COL_METADATA_ID,
                                       SqlUtil.quote(id)), SqlUtil.eq(
                                           COL_METADATA_ID_TYPE,
                                           SqlUtil.quote(type)))));

        System.err.println("query: " + query);
        SqlUtil.Iterator iter = SqlUtil.getIterator(execute(query));
        ResultSet        results;
        List<Metadata>   metadata = new ArrayList();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col = 1;
                metadata.add(new Metadata(results.getString(col++),
                                          results.getString(col++),
                                          results.getString(col++),
                                          results.getString(col++),
                                          results.getString(col++)));
            }
        }
        return metadata;
    }






}

