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

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.XmlUtil;

import org.w3c.dom.*;

import java.io.File;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Admin  extends RepositoryManager {

    /** _more_ */
    public RequestUrl URL_ADMIN_SQL = new RequestUrl(this, "/admin/sql", "SQL");

    /** _more_          */
    public RequestUrl URL_ADMIN_IMPORT_CATALOG =
        new RequestUrl(this, "/admin/import/catalog", "Import Catalog");

    /** _more_ */
    public RequestUrl URL_ADMIN_CLEANUP = new RequestUrl(this, "/admin/cleanup", "Cleanup");

    /** _more_ */
    public RequestUrl URL_ADMIN_HOME = new RequestUrl(this, "/admin/home", "Home");

    /** _more_ */
    public RequestUrl URL_ADMIN_STARTSTOP = new RequestUrl(this, "/admin/startstop",
                                           "Database");

    /** _more_ */
    public RequestUrl URL_ADMIN_TABLES = new RequestUrl(this, "/admin/tables", "Tables");

    /** _more_ */
    public RequestUrl URL_ADMIN_STATS = new RequestUrl(this, "/admin/stats", "Statistics");

    /** _more_ */
    public RequestUrl URL_ADMIN_HARVESTERS = new RequestUrl(this, "/admin/harvesters",
                                            "Harvesters");

    /** _more_ */
    protected RequestUrl[] adminUrls = {
        URL_ADMIN_HOME, URL_ADMIN_STARTSTOP, URL_ADMIN_TABLES,
        URL_ADMIN_STATS, getUserManager().URL_USER_LIST, URL_ADMIN_HARVESTERS,
        URL_ADMIN_SQL, URL_ADMIN_CLEANUP
    };


    /** _more_ */
    int cleanupTimeStamp = 0;

    /** _more_ */
    boolean runningCleanup = false;

    /** _more_ */
    StringBuffer cleanupStatus = new StringBuffer();



    /** _more_ */
    private List<Harvester> harvesters = new ArrayList();

    public Admin(Repository repository) {
        super(repository);
    }



    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    protected Harvester findHarvester(String id) {
        for (Harvester harvester : harvesters) {
            if (harvester.getId().equals(id)) {
                return harvester;
            }
        }
        return null;
    }




    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected StringBuffer getDbMetaData() throws Exception {
        StringBuffer     sb       = new StringBuffer();
        DatabaseMetaData dbmd     = getRepository().getConnection().getMetaData();
        ResultSet        catalogs = dbmd.getCatalogs();

        System.err.println("catalogs");
        ResultSet tables = dbmd.getTables(null, null, null, null);
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            String tableType = tables.getString("TABLE_TYPE");
            if ((tableType != null) && tableType.startsWith("SYSTEM")) {
                continue;
            }
            ResultSet columns = dbmd.getColumns(null, null, tableName, null);
            String encoded = new String(XmlUtil.encodeBase64(("text:?"
                                 + tableName).getBytes()));
            int cnt = getRepository().getCount(tableName, "");
            sb.append("Table:" + tableName + " (#" + cnt + ")");
            sb.append("<ul>");
            while (columns.next()) {
                String colName = columns.getString("COLUMN_NAME");
                sb.append("<li>");
                sb.append(colName + " (" + columns.getString("TYPE_NAME")
                          + ")");
            }
            sb.append("</ul>");
        }
        return sb;
    }




    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initHarvesters() throws Exception {
        List<String> harvesterFiles =
            StringUtil.split(getRepository().getProperty(PROP_HARVESTERS_FILE), ";", true,
                             true);
        boolean okToStart = getRepository().getProperty(PROP_HARVESTERS_ACTIVE, true);
        try {
            harvesters = new ArrayList<Harvester>();
            for (String file : harvesterFiles) {
                Element root = XmlUtil.getRoot(file, getClass());
                harvesters.addAll(Harvester.createHarvesters(getRepository(), root));
            }
        } catch (Exception exc) {
            System.err.println("Error loading harvester file");
            throw exc;
        }
        for (Harvester harvester : harvesters) {
            File rootDir = harvester.getRootDir();
            if (rootDir != null) {
                getRepository().addDownloadPrefix(rootDir.toString().replace("\\", "/")
                                     + "/");
            }
            if ( !okToStart) {
                harvester.setActive(false);
            } else if (harvester.getActive()) {
                Misc.run(harvester, "run");
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
    public Result adminDbStartStop(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(header("Database Administration"));
        String what = request.getString(ARG_ADMIN_WHAT, "nothing");
        if (what.equals("shutdown")) {
            if (getRepository().getConnection() == null) {
                sb.append("Not connected to database");
            } else {
                getRepository().closeConnection();
                sb.append("Database is shut down");
            }
        } else if (what.equals("restart")) {
            if (getRepository().getConnection() != null) {
                sb.append("Already connected to database");
            } else {
                getRepository().makeConnection();
                sb.append("Database is restarted");
            }
        }
        sb.append("<p>");
        sb.append(HtmlUtil.form(URL_ADMIN_STARTSTOP, " name=\"admin\""));
        if (repository.getConnection() == null) {
            sb.append(HtmlUtil.hidden(ARG_ADMIN_WHAT, "restart"));
            sb.append(HtmlUtil.submit("Restart Database"));
        } else {
            sb.append(HtmlUtil.hidden(ARG_ADMIN_WHAT, "shutdown"));
            sb.append(HtmlUtil.submit("Shut Down Database"));
        }
        sb.append("</form>");
        Result result = new Result("Administration", sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request, adminUrls));
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
    public Result adminDbTables(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(header("Database Tables"));
        sb.append(getDbMetaData());
        Result result = new Result("Administration", sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request, adminUrls));
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
        sb.append(HtmlUtil.href(URL_ADMIN_STARTSTOP, "Administer Database"));
        sb.append("<li> ");
        sb.append(HtmlUtil.href(URL_ADMIN_TABLES, "Show Tables"));
        sb.append("<li> ");
        sb.append(HtmlUtil.href(URL_ADMIN_STATS, "Statistics"));
        sb.append("<li> ");
        sb.append(HtmlUtil.href(URL_ADMIN_SQL, "Execute SQL"));
        sb.append("</ul>");
        Result result = new Result("Administration", sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request, adminUrls));
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
        if (request.defined(ARG_ACTION)) {
            String    action    = request.getString(ARG_ACTION, "");
            Harvester harvester = findHarvester(request.getString(ARG_ID,
                                      ""));
            if (action.equals(ACTION_STOP)) {
                harvester.setActive(false);
            } else if (action.equals(ACTION_REMOVE)) {
                harvester.setActive(false);
                harvesters.remove(harvester);
            } else if (action.equals(ACTION_START)) {
                if ( !harvester.getActive()) {
                    harvester.setActive(true);
                    Misc.run(harvester, "run");
                }
            }
            return new Result(URL_ADMIN_HARVESTERS.toString());
        }


        sb.append(header("Harvesters"));
        sb.append("<table cellspacing=\"5\">");
        sb.append(HtmlUtil.row(HtmlUtil.cols(HtmlUtil.bold("Name"),
                                             HtmlUtil.bold("State"),
                                             HtmlUtil.bold("Action"), "",
                                             "")));

        int cnt = 0;
        for (Harvester harvester : harvesters) {
            String remove = HtmlUtil.href(HtmlUtil.url(URL_ADMIN_HARVESTERS,
                                ARG_ACTION, ACTION_REMOVE, ARG_ID,
                                harvester.getId()), "Remove");
            String run;
            if (harvester.getActive()) {
                run = HtmlUtil.href(HtmlUtil.url(URL_ADMIN_HARVESTERS,
                        ARG_ACTION, ACTION_STOP, ARG_ID,
                        harvester.getId()), "Stop");
            } else {
                run = HtmlUtil.href(HtmlUtil.url(URL_ADMIN_HARVESTERS,
                        ARG_ACTION, ACTION_START, ARG_ID,
                        harvester.getId()), "Start");
            }
            cnt++;
            sb.append("<tr valign=\"top\">");
            sb.append(HtmlUtil.cols(harvester.getName(),
                                    (harvester.getActive()
                                     ? "Active"
                                     : "Stopped") + HtmlUtil.space(2), run,
                                     remove, harvester.getExtraInfo()));
            sb.append("</tr>\n");
        }
        sb.append("</table>");

        Result result = new Result("Harvesters", sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request, adminUrls));
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
        String[] names  = { "Users", "Tags", "Associations" };
        String[] tables = { TABLE_USERS, TABLE_TAGS, TABLE_ASSOCIATIONS };
        for (int i = 0; i < tables.length; i++) {
            sb.append(HtmlUtil.row(HtmlUtil.cols(""
                    + getRepository().getCount(tables[i].toLowerCase(), ""), names[i])));
        }


        sb.append(HtmlUtil.row("<td colspan=\"2\">&nbsp;<p>"
                               + HtmlUtil.bold("Types:") + "</td>"));
        int total = 0;
        sb.append(HtmlUtil.row(HtmlUtil.cols("" + getRepository().getCount(TABLE_ENTRIES,
                ""), "Total entries")));
        for (TypeHandler typeHandler: getRepository().getTypeHandlers()) {
            if (typeHandler.isType(TypeHandler.TYPE_ANY)) {
                continue;
            }
            int cnt = getRepository().getCount(TABLE_ENTRIES, "type=" + SqlUtil.quote(typeHandler.getType()));

            String url = HtmlUtil.href(HtmlUtil.url(getRepository().URL_ENTRY_SEARCHFORM,
                             ARG_TYPE, typeHandler.getType()), typeHandler.getLabel());
            sb.append(HtmlUtil.row(HtmlUtil.cols("" + cnt, url)));
        }



        sb.append("</table>\n");

        Result result = new Result("Repository Statistics", sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request, adminUrls));
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
    public Result adminSql(Request request) throws Exception {
        String query = (String) request.getUnsafeString(ARG_QUERY,
                           (String) null);
        StringBuffer sb = new StringBuffer();
        sb.append(header("SQL"));
        sb.append(HtmlUtil.form(URL_ADMIN_SQL));
        sb.append(HtmlUtil.submit("Execute"));
        sb.append(HtmlUtil.textArea(ARG_QUERY, query==null?"":query, 10,75));
        sb.append("</form>\n");
        sb.append("<table>");
        if (query == null) {
            Result result = new Result("SQL", sb);
            result.putProperty(PROP_NAVSUBLINKS,
                               getRepository().getSubNavLinks(request, adminUrls));
            return result;
        }

        long      t1        = System.currentTimeMillis();

        Statement statement = null;
        try {
            statement = getRepository().execute(query);
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
                    colcnt++;
                    if(rsmd.getColumnType(colcnt) == java.sql.Types.TIMESTAMP) {
                        Date dttm = results.getTimestamp(colcnt , Repository.calendar );
                        sb.append(HtmlUtil.col(Repository.fmt(dttm)));
                    } else {
                        sb.append(HtmlUtil.col(results.getString(colcnt)));
                    }
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
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request, adminUrls));
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
    public Result adminCleanup(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(HtmlUtil.form(URL_ADMIN_CLEANUP));
        if (request.defined(ACTION_STOP)) {
            runningCleanup = false;
            cleanupTimeStamp++;
            return new Result(URL_ADMIN_CLEANUP.toString());
        } else if (request.defined(ACTION_START)) {
            Misc.run(this, "runDatabaseCleanUp", request);
            return new Result(URL_ADMIN_CLEANUP.toString());
        }
        String status = cleanupStatus.toString();
        if (runningCleanup) {
            sb.append("Database clean up is running<p>");
            sb.append(HtmlUtil.submit("Stop cleanup", ACTION_STOP));
        } else {
            sb.append(
                "Cleanup allows you to remove all file entries from the repository database that do not exist on the local file system<p>");
            sb.append(HtmlUtil.submit("Start cleanup", ACTION_START));


        }
        sb.append("</form>");
        if (status.length() > 0) {
            sb.append("<h3>Cleanup Status</h3>");
            sb.append(status);
        }
        //        sb.append(cnt +" files do not exist in " + (t2-t1) );
        Result result = new Result("Cleanup", sb,
                                   getRepository().getMimeTypeFromSuffix(".html"));
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request, adminUrls));
        return result;
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void runDatabaseCleanUp(Request request) throws Exception {
        if (runningCleanup) {
            return;
        }
        runningCleanup = true;
        cleanupStatus  = new StringBuffer();
        int myTimeStamp = ++cleanupTimeStamp;
        try {
            String query = SqlUtil.makeSelect(
                               SqlUtil.comma(
                                   COL_ENTRIES_ID, COL_ENTRIES_RESOURCE,
                                   COL_ENTRIES_TYPE), Misc.newList(
                                       TABLE_ENTRIES), SqlUtil.eq(
                                       COL_ENTRIES_RESOURCE_TYPE,
                                       SqlUtil.quote(Resource.TYPE_FILE)));

            SqlUtil.Iterator iter = SqlUtil.getIterator(getRepository().execute(query));
            ResultSet        results;
            int              cnt       = 0;
            int              deleteCnt = 0;
            long             t1        = System.currentTimeMillis();
            List<Entry>      entries   = new ArrayList<Entry>();
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    if ((cleanupTimeStamp != myTimeStamp)
                            || !runningCleanup) {
                        runningCleanup = false;
                        break;
                    }
                    int    col      = 1;
                    String id       = results.getString(col++);
                    String resource = results.getString(col++);
                    Entry entry = getRepository().getTypeHandler(
                                      results.getString(col++)).createEntry(
                                      id);
                    File f = new File(resource);
                    if (f.exists()) {
                        continue;
                    }
                    //TODO: differentiate the entries that are not files
                    entries.add(entry);
                    if (entries.size() % 1000 == 0) {
                        System.err.print(".");
                    }
                    if (entries.size() > 1000) {
                        getRepository().deleteEntries(request, entries);
                        entries   = new ArrayList<Entry>();
                        deleteCnt += 1000;
                        cleanupStatus = new StringBuffer("Removed "
                                + deleteCnt + " entries from database");
                    }
                }
                if ((cleanupTimeStamp != myTimeStamp) || !runningCleanup) {
                    runningCleanup = false;
                    break;
                }
            }
            if (runningCleanup) {
                getRepository().deleteEntries(request, entries);
                deleteCnt += entries.size();
                cleanupStatus =
                    new StringBuffer("Done running cleanup<br>Removed "
                                     + deleteCnt + " entries from database");
            }
        } catch (Exception exc) {
            log("Running cleanup", exc);
            cleanupStatus.append("An error occurred running cleanup<pre>");
            cleanupStatus.append(LogUtil.getStackTrace(exc));
            cleanupStatus.append("</pre>");
        }
        runningCleanup = false;
        long t2 = System.currentTimeMillis();
    }



    /** _more_          */
    int ccnt = 0;


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result adminImportCatalog(Request request) throws Exception {
        Group        group   = getRepository().findGroup(request, false);
        boolean      recurse = request.get(ARG_RECURSE, false);
        StringBuffer sb      = new StringBuffer();
        sb.append(getRepository().makeGroupHeader(request, group));
        sb.append("<p>");
        String catalog = request.getString(ARG_CATALOG, "").trim();
        sb.append(HtmlUtil.form(URL_ADMIN_IMPORT_CATALOG.toString()));
        sb.append(HtmlUtil.hidden(ARG_GROUP, group.getFullName()));
        sb.append(HtmlUtil.submit("Import catalog:"));
        sb.append(HtmlUtil.space(1));
        sb.append(HtmlUtil.input(ARG_CATALOG, catalog, " size=\"75\""));
        sb.append(HtmlUtil.checkbox(ARG_RECURSE, "true", recurse));
        sb.append(" Recurse");
        sb.append("</form>");
        if (catalog.length() > 0) {
            CatalogHarvester harvester =
                new CatalogHarvester(getRepository(), group, catalog,
                                     request.getRequestContext().getUser(),
                                     recurse);
            harvesters.add(harvester);
            Misc.run(harvester, "run");
        }

        Result result = new Result(URL_ADMIN_HARVESTERS.toString());
        return result;
    }




}

