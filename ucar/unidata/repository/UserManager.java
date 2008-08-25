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


import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;



import java.io.UnsupportedEncodingException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;




/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class UserManager extends RepositoryManager {

    /** _more_ */
    public static final String COOKIE_NAME = "repositorysession";

    /** _more_ */
    public static final String ROLE_ANY = "any";

    /** _more_ */
    public static final String ROLE_NONE = "none";


    /** _more_ */
    private Hashtable<String, Session> sessionMap = new Hashtable<String,
                                                        Session>();



    /** _more_ */
    protected RequestUrl[] userUrls = { getRepositoryBase().URL_USER_SETTINGS,
                                        getRepositoryBase().URL_USER_CART };



    /** _more_ */
    private Hashtable<String, User> userMap = new Hashtable<String, User>();

    /** _more_ */
    private Hashtable userCart = new Hashtable();

    /** _more_ */
    private List ipUserList = new ArrayList();

    /** _more_          */
    public final User localFileUser = new User("localuser", false);

    /**
     * _more_
     *
     * @param repository _more_
     */
    public UserManager(Repository repository) {
        super(repository);
        //        ipUserList.add("128.117.156.*");
        //        ipUserList.add("jeffmc");

    }


    /**
     * _more_
     *
     * @param password _more_
     *
     * @return _more_
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(password.getBytes("UTF-8"));
            return XmlUtil.encodeBase64(md.digest()).trim();
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae.getMessage());
        } catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException(uee.getMessage());
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    protected void checkSession(Request request) throws Exception {

        User         user    = request.getUser();
        List<String> cookies = getCookies(request);

        for (String cookieValue : cookies) {
            request.setSessionId(cookieValue);
            if (user == null) {
                Session session = sessionMap.get(request.getSessionId());
                if (session != null) {
                    session.lastActivity = new Date();
                    user                 = session.user;
                    break;
                }
            }
        }


        if ((user == null) && request.hasParameter(ARG_SESSIONID)) {
            Session session =
                sessionMap.get(request.getString(ARG_SESSIONID));
            if (session != null) {
                session.lastActivity = new Date();
                user                 = session.user;
            }
        }

        //Check for url auth
        if ((user == null) && request.exists(ARG_AUTH_USER)
                && request.exists(ARG_AUTH_PASSWORD)) {
            String userId   = request.getString(ARG_AUTH_USER, "");
            String password = request.getString(ARG_AUTH_PASSWORD, "");
            user = findUser(userId, false);
            if (user == null) {
                throw new IllegalArgumentException(msgLabel("Unknown user")
                        + userId);
            }
            if ( !user.getPassword().equals(hashPassword(password))) {
                throw new IllegalArgumentException(msg("Incorrect password"));
            }
            setUserSession(request, user);
        }


        //Check for basic auth
        if (user == null) {
            String auth =
                (String) request.getHttpHeaderArgs().get("Authorization");
            if (auth == null) {
                auth = (String) request.getHttpHeaderArgs().get(
                    "authorization");
            }

            if (auth != null) {
                auth = auth.trim();
                //Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
                if (auth.startsWith("Basic")) {
                    auth = new String(
                        XmlUtil.decodeBase64(auth.substring(5).trim()));
                    String[] toks = StringUtil.split(auth, ":", 2);
                    if (toks.length == 2) {
                        user = findUser(toks[0], false);
                        if (user == null) {
                            throw new Repository.AccessException(
                                msgLabel("Unknown user") + toks[0]);
                        }
                        if ( !user.getPassword().equals(
                                hashPassword(toks[1]))) {
                            throw new Repository.AccessException(
                                msg("Incorrect password"));
                        }
                    }
                    setUserSession(request, user);
                }
            }
        }

        if (user == null) {
            String requestIp = request.getIp();
            if (requestIp != null) {
                for (int i = 0; i < ipUserList.size(); i += 2) {
                    String ip       = (String) ipUserList.get(i);
                    String userName = (String) ipUserList.get(i + 1);
                    if (requestIp.matches(ip)) {
                        user = findUser(userName, false);
                        if (user == null) {
                            user = new User(userName, false);
                            makeOrUpdateUser(user, false);
                        }
                    }
                }
            }
        }


        if (request.getSessionId() == null) {
            //            request.setSessionId(getSessionId());
        }


        if (user == null) {
            user = getUserManager().getAnonymousUser();
        }

        request.setUser(user);

    }

    /**
     * _more_
     *
     * @return _more_
     */
    private List<Session> getSessions() {
        List<Session> sessions = new ArrayList<Session>();
        for (Enumeration keys = sessionMap.keys(); keys.hasMoreElements(); ) {
            sessions.add(sessionMap.get((String) keys.nextElement()));
        }
        return sessions;
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
    private List<String> getCookies(Request request) throws Exception {
        List<String> cookies = new ArrayList<String>();
        String       cookie  = request.getHeaderArg("Cookie");
        if (cookie == null) {
            return cookies;
        }
        request.tmp.append("cookie from header:" + cookie + "<p>");

        List toks = StringUtil.split(cookie, ";", true, true);
        for (int i = 0; i < toks.size(); i++) {
            String tok     = (String) toks.get(i);
            List   subtoks = StringUtil.split(tok, "=", true, true);
            if (subtoks.size() != 2) {
                continue;
            }
            String cookieName  = (String) subtoks.get(0);
            String cookieValue = (String) subtoks.get(1);
            if (cookieName.equals(COOKIE_NAME)) {
                cookies.add(cookieValue);
            }
        }
        request.tmp.append("cookies:" + cookies + "<p>");
        return cookies;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected String getSessionId() {
        return getRepository().getGUID() + "_" + Math.random();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     *
     * @throws Exception _more_
     */
    protected void setUserSession(Request request, User user)
            throws Exception {
        if (request.getSessionId() == null) {
            request.setSessionId(getSessionId());
        }
        sessionMap.put(request.getSessionId(),
                       new Session(request.getSessionId(), user, new Date()));
        request.setUser(user);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    protected void removeUserSession(Request request) throws Exception {
        if (request.getSessionId() != null) {
            sessionMap.remove(request.getSessionId());
        }
        List<String> cookies = getCookies(request);
        for (String cookieValue : cookies) {
            sessionMap.remove(cookieValue);
        }
        request.setUser(getUserManager().getAnonymousUser());
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    protected boolean isRequestOk(Request request) {
        if (getProperty(PROP_ACCESS_ADMINONLY, false)
                && !request.getUser().getAdmin()) {
            if ( !request.getRequestPath().startsWith(
                    getRepository().getUrlBase() + "/user/")) {
                return false;
            }
        }

        if (getProperty(PROP_ACCESS_REQUIRELOGIN, false)
                && request.getUser().getAnonymous()) {
            if ( !request.getRequestPath().startsWith(
                    getRepository().getUrlBase() + "/user/")) {
                return false;
            }
        }
        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String makeLoginForm(Request request) {
        return makeLoginForm(request, "");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public String makeLoginForm(Request request, String extra) {
        StringBuffer sb = new StringBuffer();
        sb.append(header(msg("Please login")));
        String id = request.getString(ARG_USER_ID, "");
        if (getRepository().isSSLEnabled()) {
            //sb.append(HtmlUtil.form(getRepositoryBase().URL_USER_LOGIN.getHttpsUrl("")));
            sb.append(
                HtmlUtil.form(getRepositoryBase().URL_USER_LOGIN.toString()));
        } else {
            sb.append(
                HtmlUtil.form(getRepositoryBase().URL_USER_LOGIN.toString()));
        }
        if (request.defined(ARG_REDIRECT)) {
            sb.append(HtmlUtil.hidden(ARG_REDIRECT,
                                      request.getUnsafeString(ARG_REDIRECT,
                                          "")));
        }
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry(msgLabel("User"),
                                     HtmlUtil.input(ARG_USER_ID, id)));
        sb.append(HtmlUtil.formEntry(msgLabel("Password"),
                                     HtmlUtil.password(ARG_USER_PASSWORD)));
        sb.append(extra);

        sb.append(HtmlUtil.formEntry("", HtmlUtil.submit(msg("Login"))));

        sb.append(HtmlUtil.formTableClose());
        sb.append(HtmlUtil.formClose());
        return sb.toString();
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User getDefaultUser() throws Exception {
        makeUserIfNeeded(new User("default", "Default User", false));
        return findUser("default");
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User getAnonymousUser() throws Exception {
        return new User();
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
        return findUser(id, false);
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param userDefaultIfNotFound _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User findUser(String id, boolean userDefaultIfNotFound)
            throws Exception {
        if (id == null) {
            return null;
        }
        User user = userMap.get(id);
        if (user != null) {
            //            System.err.println ("got from user map:" + id +" " + user);
            return user;
        }
        Statement stmt = getDatabaseManager().select(COLUMNS_USERS,
                             TABLE_USERS, Clause.eq(COL_USERS_ID, id));
        ResultSet results = stmt.getResultSet();
        if ( !results.next()) {
            //            throw new IllegalArgumentException ("Could not find  user id:" + id + " sql:" + query);
            if (userDefaultIfNotFound) {
                return getDefaultUser();
            }
            return null;
        } else {
            user = getUser(results);
        }

        userMap.put(user.getId(), user);
        return user;
    }



    /**
     * _more_
     *
     * @param user _more_
     * @param updateIfNeeded _more_
     *
     * @throws Exception _more_
     */
    protected void makeOrUpdateUser(User user, boolean updateIfNeeded)
            throws Exception {
        if (getRepository().tableContains(user.getId(), TABLE_USERS,
                                          COL_USERS_ID)) {
            if ( !updateIfNeeded) {
                throw new IllegalArgumentException(
                    msgLabel("Database already contains user")
                    + user.getId());
            }
            SqlUtil.update(getConnection(), TABLE_USERS, COL_USERS_ID,
                           user.getId(), new String[] {
                COL_USERS_NAME, COL_USERS_PASSWORD, COL_USERS_EMAIL,
                COL_USERS_QUESTION, COL_USERS_ANSWER, COL_USERS_ADMIN,
                COL_USERS_LANGUAGE
            }, new Object[] {
                user.getName(), user.getPassword(), user.getEmail(),
                user.getQuestion(), user.getAnswer(), user.getAdmin()
                        ? new Integer(1)
                        : new Integer(0), user.getLanguage()
            });
            return;
        }

        getDatabaseManager().executeInsert(INSERT_USERS, new Object[] {
            user.getId(), user.getName(), user.getEmail(), user.getQuestion(),
            user.getAnswer(), user.getPassword(),
            new Boolean(user.getAdmin()), user.getLanguage()
        });
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
            makeOrUpdateUser(user, true);
        }
    }

    /**
     * _more_
     *
     * @param user _more_
     *
     * @throws Exception _more_
     */
    protected void deleteUser(User user) throws Exception {
        deleteRoles(user);
        SqlUtil.delete(getConnection(), TABLE_USERS,
                       Clause.eq(COL_USERS_ID, user.getId()));
    }

    /**
     * _more_
     *
     * @param user _more_
     *
     * @throws Exception _more_
     */
    protected void deleteRoles(User user) throws Exception {
        SqlUtil.delete(getConnection(), TABLE_USERROLES,
                       Clause.eq(COL_USERROLES_USER_ID, user.getId()));
    }


    /*
    protected List<String> getRoles(User user) throws Exception {
        if(user.getRoles() == null) {
        }
        }*/


    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     *
     * @return _more_
     */
    private boolean checkPasswords(Request request, User user) {
        String password1 = request.getString(ARG_USER_PASSWORD1, "").trim();
        String password2 = request.getString(ARG_USER_PASSWORD2, "").trim();
        if (password1.length() > 0) {
            if ( !password1.equals(password2)) {
                return false;
            } else {
                user.setPassword(hashPassword(password1));
            }
        }
        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     * @param doAdmin _more_
     *
     * @throws Exception _more_
     */
    private void applyState(Request request, User user, boolean doAdmin)
            throws Exception {
        user.setName(request.getString(ARG_USER_NAME, user.getName()));
        user.setEmail(request.getString(ARG_USER_EMAIL, user.getEmail()));
        user.setLanguage(request.getString(ARG_USER_LANGUAGE,
                                           user.getLanguage()));
        user.setQuestion(request.getString(ARG_USER_QUESTION,
                                           user.getQuestion()));
        user.setAnswer(request.getString(ARG_USER_ANSWER, user.getAnswer()));
        if (doAdmin) {
            if ( !request.defined(ARG_USER_ADMIN)) {
                user.setAdmin(false);
            } else {
                user.setAdmin(request.get(ARG_USER_ADMIN, user.getAdmin()));
            }
            List<String> roles =
                StringUtil.split(request.getString(ARG_USER_ROLES, ""), "\n",
                                 true, true);
            deleteRoles(user);
            for (String role : roles) {
                getDatabaseManager().executeInsert(INSERT_USERROLES,
                        new Object[] { user.getId(),
                                       role });
            }
            user.setRoles(roles);
        }
        makeOrUpdateUser(user, true);
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
    public Result adminUserEdit(Request request) throws Exception {
        String userId = request.getString(ARG_USER_ID, "");
        User   user   = findUser(userId);
        if (user == null) {
            throw new IllegalArgumentException(
                msgLabel("Could not find user") + userId);
        }

        if (request.defined(ARG_USER_DELETE_CONFIRM)) {
            deleteUser(user);
            return new Result(request.url(getRepositoryBase().URL_USER_LIST));
        }


        StringBuffer sb = new StringBuffer();
        if (request.defined(ARG_USER_CHANGE)) {
            boolean okToChangeUser = true;
            okToChangeUser = checkPasswords(request, user);
            if ( !okToChangeUser) {
                sb.append(getRepository().warning("Incorrect passwords"));
            }

            if (okToChangeUser) {
                applyState(request, user, true);
            }
        }


        sb.append(getRepository().header(msgLabel("User") + HtmlUtil.space(1)
                                         + user.getLabel()));
        sb.append(HtmlUtil.p());
        sb.append(request.form(getRepositoryBase().URL_USER_EDIT));
        sb.append(HtmlUtil.hidden(ARG_USER_ID, user.getId()));
        if (request.defined(ARG_USER_DELETE)) {
            sb.append(
                getRepository().question(
                    msg("Are you sure you want to delete the user?"),
                    getRepository().buttons(
                        HtmlUtil.submit(msg("Yes"), ARG_USER_DELETE_CONFIRM),
                        HtmlUtil.submit(msg("Cancel"), ARG_USER_CANCEL))));
        } else {
            String buttons =
                HtmlUtil.submit(msg("Change User"), ARG_USER_CHANGE)
                + HtmlUtil.space(2)
                + HtmlUtil.submit(msg("Delete User"), ARG_USER_DELETE)
                + HtmlUtil.space(2)
                + HtmlUtil.submit(msg("Cancel"), ARG_CANCEL);
            sb.append(buttons);
            makeUserForm(request, user, sb, true);
            sb.append(buttons);
        }
        sb.append(HtmlUtil.formClose());
        Result result = new Result(msgLabel("User") + user.getLabel(), sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request,
                               getAdmin().adminUrls));
        return result;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param user _more_
     * @param sb _more_
     * @param includeAdmin _more_
     *
     * @throws Exception _more_
     */
    private void makeUserForm(Request request, User user, StringBuffer sb,
                              boolean includeAdmin)
            throws Exception {
        //        System.err.println ("User:" + user);
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry(msgLabel("Name"),
                                     HtmlUtil.input(ARG_USER_NAME,
                                         user.getName(), HtmlUtil.SIZE_40)));
        if (includeAdmin) {
            sb.append(HtmlUtil.formEntry(msgLabel("Admin"),
                                         HtmlUtil.checkbox(ARG_USER_ADMIN,
                                             "true", user.getAdmin())));
            String       userRoles = user.getRolesAsString("\n");
            StringBuffer allRoles  = new StringBuffer();
            List         roles     = getRoles();
            allRoles.append(
                "<table border=0 cellspacing=0 cellpadding=0><tr valign=\"top\"><td><b>e.g.:</b></td><td>&nbsp;&nbsp;</td><td>");
            int cnt = 0;
            for (int i = 0; i < roles.size(); i++) {
                if (cnt++ > 4) {
                    allRoles.append("</td><td>&nbsp;&nbsp;</td><td>");
                    cnt = 0;
                }
                allRoles.append("<i>");
                allRoles.append(roles.get(i));
                allRoles.append("</i><br>");
            }
            allRoles.append("</table>\n");

            String roleEntry =
                HtmlUtil.hbox(HtmlUtil.textArea(ARG_USER_ROLES, userRoles, 5,
                    20), allRoles.toString());
            sb.append(HtmlUtil.formEntryTop(msgLabel("Roles"), roleEntry));
        }

        sb.append(HtmlUtil.formEntry(msgLabel("Email"),
                                     HtmlUtil.input(ARG_USER_EMAIL,
                                         user.getEmail(), HtmlUtil.SIZE_40)));

        List languages = new ArrayList(getRepository().getLanguages());
        languages.add(0, new TwoFacedObject("None", ""));
        sb.append(HtmlUtil.formEntry(msgLabel("Language"),
                                     HtmlUtil.select(ARG_USER_LANGUAGE,
                                         languages, user.getLanguage())));

        sb.append(HtmlUtil.formEntry("&nbsp;<p>", ""));

        sb.append(HtmlUtil.formEntry(msgLabel("Password"),
                                     HtmlUtil.password(ARG_USER_PASSWORD1)));

        sb.append(HtmlUtil.formEntry(msgLabel("Password Again"),
                                     HtmlUtil.password(ARG_USER_PASSWORD2)));

        sb.append(HtmlUtil.formTableClose());
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
    public Result adminUserNew(Request request) throws Exception {
        String       id          = "";
        String       name        = "";
        String       email       = "";
        String       password1   = "";
        String       password2   = "";
        boolean      admin       = false;

        StringBuffer sb          = new StringBuffer();

        StringBuffer errorBuffer = new StringBuffer();
        if (request.exists(ARG_USER_ID)) {
            id        = request.getString(ARG_USER_ID, "").trim();
            name      = request.getString(ARG_USER_NAME, name).trim();
            email     = request.getString(ARG_USER_EMAIL, "").trim();
            password1 = request.getString(ARG_USER_PASSWORD1, "").trim();
            password2 = request.getString(ARG_USER_PASSWORD2, "").trim();
            admin     = request.get(ARG_USER_ADMIN, false);
            boolean okToAdd = true;
            if (id.length() == 0) {
                okToAdd = false;
                errorBuffer.append(msg("Please enter an ID"));
                errorBuffer.append(HtmlUtil.br());
            }

            if ((password1.length() == 0) || !password1.equals(password2)) {
                okToAdd = false;
                errorBuffer.append(msg("Invalid password"));
                errorBuffer.append(HtmlUtil.br());
            }

            if (findUser(id) != null) {
                okToAdd = false;
                errorBuffer.append(msg("User with given id already exists"));
                errorBuffer.append(HtmlUtil.br());
            }

            if (okToAdd) {
                makeOrUpdateUser(new User(id, name, email, "", "",
                                          hashPassword(password1), admin,
                                          ""), false);
                String userEditLink =
                    request.url(getRepositoryBase().URL_USER_EDIT,
                                ARG_USER_ID, id);
                return new Result(userEditLink);
            }
        }



        if (errorBuffer.toString().length() > 0) {
            sb.append(getRepository().warning(errorBuffer.toString()));
        }
        sb.append(msgHeader("Create User"));
        sb.append(request.form(getRepositoryBase().URL_USER_NEW));
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry(msgLabel("ID"),
                                     HtmlUtil.input(ARG_USER_ID, id)));
        sb.append(HtmlUtil.formEntry(msgLabel("Name"),
                                     HtmlUtil.input(ARG_USER_NAME, name)));


        sb.append(HtmlUtil.formEntry(msgLabel("Admin"),
                                     HtmlUtil.checkbox(ARG_USER_ADMIN,
                                         "true", admin)));

        sb.append(HtmlUtil.formEntry(msgLabel("Email"),
                                     HtmlUtil.input(ARG_USER_EMAIL, email)));

        sb.append(HtmlUtil.formEntry(msgLabel("Password"),
                                     HtmlUtil.password(ARG_USER_PASSWORD1)));

        sb.append(HtmlUtil.formEntry(msgLabel("Password Again"),
                                     HtmlUtil.password(ARG_USER_PASSWORD2)));

        sb.append(HtmlUtil.formEntry("",
                                     HtmlUtil.submit(msg("Create User"),
                                         ARG_USER_NEW)));
        sb.append("</table>");
        sb.append("\n</form>\n");
        Result result = new Result(msg("New User"), sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request,
                               getAdmin().adminUrls));
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
    public StringBuffer getSessionList(Request request) throws Exception {
        List<Session> sessions    = getSessions();
        StringBuffer  sessionHtml = new StringBuffer(HtmlUtil.formTable());
        sessionHtml.append(msgHeader("Current Sessions"));
        sessionHtml.append(
            HtmlUtil.row(
                HtmlUtil.cols(
                    HtmlUtil.bold(msg("User")), HtmlUtil.bold(msg("Since")),
                    HtmlUtil.bold(msg("Last Activity")))));
        for (Session session : sessions) {
            sessionHtml.append(
                HtmlUtil.row(
                    HtmlUtil.cols(
                        session.user.getLabel(),
                        formatDate(request, session.createDate),
                        formatDate(request, session.lastActivity))));
        }
        sessionHtml.append(HtmlUtil.formTableClose());
        return sessionHtml;
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
    public Result adminUserList(Request request) throws Exception {






        StringBuffer usersHtml = new StringBuffer();


        usersHtml.append(msgHeader("Users"));
        usersHtml.append(request.form(getRepositoryBase().URL_USER_NEW));
        usersHtml.append(HtmlUtil.submit(msg("New User")));
        usersHtml.append("</form>");

        Statement stmt = getDatabaseManager().select(COLUMNS_USERS,
                             TABLE_USERS, new Clause(),
                             " order by " + COL_USERS_ID);

        SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
        ResultSet        results;

        List<User>       users = new ArrayList();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                users.add(getUser(results));
            }
        }

        usersHtml.append("<table>");
        usersHtml.append(
            HtmlUtil.row(
                HtmlUtil.cols(
                    HtmlUtil.bold(msg("ID")) + HtmlUtil.space(2),
                    HtmlUtil.bold(msg("Name")) + HtmlUtil.space(2),
                    HtmlUtil.bold(msg("Roles")) + HtmlUtil.space(2),
                    HtmlUtil.bold(msg("Email")) + HtmlUtil.space(2),
                    HtmlUtil.bold(msg("Admin?")) + HtmlUtil.space(2))));

        for (User user : users) {
            String userEditLink =
                HtmlUtil.href(request.url(getRepositoryBase().URL_USER_EDIT,
                                          ARG_USER_ID,
                                          user.getId()), user.getId());

            String row = (user.getAdmin()
                          ? "<tr valign=\"top\" style=\"background-color:#cccccc;\">"
                          : "<tr valign=\"top\" >") + HtmlUtil.cols(
                              userEditLink, user.getName(),
                              user.getRolesAsString("<br>"), user.getEmail(),
                              "" + user.getAdmin()) + "</tr>";
            usersHtml.append(row);

        }
        usersHtml.append("</table>");



        StringBuffer sb = new StringBuffer();
        sb.append("<table>");
        sb.append(
            HtmlUtil.rowTop(
                HtmlUtil.cols(
                    getSessionList(request).toString(), HtmlUtil.space(5),
                    usersHtml.toString())));
        sb.append("</table>");

        Result result = new Result(msg("Users"), sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getRepository().getSubNavLinks(request,
                               getAdmin().adminUrls));
        return result;
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
        User user = new User(results.getString(col++),
                             results.getString(col++),
                             results.getString(col++),
                             results.getString(col++),
                             results.getString(col++),
                             results.getString(col++),
                             results.getBoolean(col++),
                             results.getString(col++));

        Statement stmt = getDatabaseManager().select(COL_USERROLES_ROLE,
                             TABLE_USERROLES,
                             Clause.eq(COL_USERROLES_USER_ID, user.getId()));

        String[]     array = SqlUtil.readString(stmt, 1);
        List<String> roles = new ArrayList<String>(Misc.toList(array));
        user.setRoles(roles);
        return user;
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    protected List<Entry> getCart(Request request) {
        String sessionId = request.getSessionId();

        if (sessionId == null) {
            return new ArrayList<Entry>();
        }
        List<Entry> cart = (List<Entry>) userCart.get(sessionId);
        if (cart == null) {
            cart = new ArrayList<Entry>();
            userCart.put(sessionId, cart);
        }
        return cart;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    private void addToCart(Request request, List<Entry> entries)
            throws Exception {
        List<Entry> cart = getCart(request);
        for (Entry entry : entries) {
            if ( !cart.contains(entry)) {
                cart.add(entry);
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
    public Result processCart(Request request) throws Exception {
        String       action = request.getString(ARG_ACTION, "");
        StringBuffer sb     = new StringBuffer();
        if (action.equals(ACTION_CLEAR)) {
            getCart(request).clear();
        } else if (action.equals(ACTION_ADD)) {
            Entry entry = getRepository().getEntry(request,
                              request.getId(""));
            if (entry == null) {
                throw new IllegalArgumentException(
                    msgLabel("Could not find entry with id")
                    + request.getId(""));
            }
            if ( !getCart(request).contains(entry)) {
                getCart(request).add(entry);
            }
        }

        return showCart(request);
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
    public Result showCart(Request request) throws Exception {
        StringBuffer sb      = new StringBuffer();
        List<Entry>  entries = getCart(request);
        sb.append(msgHeader("User Cart"));
        if (entries.size() == 0) {
            sb.append(msg("No entries in cart"));
        } else {
            sb.append(
                HtmlUtil.href(
                    request.url(
                        getRepositoryBase().URL_USER_CART, ARG_ACTION,
                        ACTION_CLEAR), msg("Clear Cart")));
            sb.append(HtmlUtil.p());
            boolean haveFrom = request.defined(ARG_FROM);
            if (haveFrom) {
                Entry fromEntry = getRepository().getEntry(request,
                                      request.getString(ARG_FROM, ""));
                sb.append(HtmlUtil.br());
                sb.append(msgLabel("Pick an entry  to associate with")
                          + HtmlUtil.space(1) + fromEntry.getName());
            }


            if ( !haveFrom) {
                sb.append(
                    request.form(
                        repository.URL_GETENTRIES,
                        "name=\"getentries\" method=\"post\""));
                sb.append(HtmlUtil.submit(msg("Get selected"),
                                          "getselected"));
                sb.append(HtmlUtil.submit(msg("Get all"), "getall"));
                sb.append(HtmlUtil.space(1));
                sb.append(msgLabel("As"));
                sb.append(HtmlUtil.space(1));
                List outputList = repository.getOutputTypes(request,
                                      new OutputHandler.State(entries));
                sb.append(HtmlUtil.select(ARG_OUTPUT, outputList));
            }
            //            sb.append("<br>");
            sb.append("<ul style=\"list-style-image : url("
                      + getRepository().fileUrl(ICON_FILE) + ")\">");
            OutputHandler outputHandler =
                getRepository().getOutputHandler(request);
            for (Entry entry : entries) {
                sb.append("<li> ");
                if (haveFrom) {
                    sb.append(
                        HtmlUtil.href(
                            request.url(
                                getRepository().URL_ASSOCIATION_ADD,
                                ARG_FROM, request.getString(ARG_FROM, ""),
                                ARG_TO, entry.getId()), HtmlUtil.img(
                                    getRepository().fileUrl(
                                        ICON_ASSOCIATION), msg(
                                        "Create an association"))));
                } else {
                    String links = HtmlUtil.checkbox("entry_"
                                       + entry.getId(), "true");
                    sb.append(HtmlUtil.hidden("all_" + entry.getId(), "1"));
                    sb.append(links);
                    sb.append(
                        HtmlUtil.href(
                            request.url(
                                getRepositoryBase().URL_USER_CART, ARG_FROM,
                                entry.getId()), HtmlUtil.img(
                                    getRepository().fileUrl(
                                        ICON_ASSOCIATION), msg(
                                        "Create an association"))));
                }
                sb.append(HtmlUtil.space(1));
                sb.append(outputHandler.getEntryLink(request, entry));
            }
            sb.append("</ul>");
            if ( !haveFrom) {
                sb.append("</form>");
            }
        }
        return makeResult(request, "User Cart", sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param title _more_
     * @param sb _more_
     *
     * @return _more_
     */
    private Result makeResult(Request request, String title,
                              StringBuffer sb) {
        return getRepository().makeResult(request, title, sb,
                                          (request.getUser().getAnonymous()
                                           ? null
                                           : userUrls));
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public String getUserLinks(Request request) {
        User   user = request.getUser();
        String userLink;
        String cartEntry =
            HtmlUtil.href(request.url(getRepositoryBase().URL_USER_CART),
                          HtmlUtil.img(getRepository().fileUrl(ICON_CART),
                                       msg("Data Cart")));
        if (user.getAnonymous()) {
            String redirect =
                XmlUtil.encodeBase64(request.getUrl().getBytes());
            userLink =
                HtmlUtil.href(request.url(getRepositoryBase().URL_USER_LOGIN,
                                          ARG_REDIRECT,
                                          redirect), msg("Login"),
                                              " class=\"navlink\" ");
        } else {
            userLink = HtmlUtil.href(
                request.url(getRepositoryBase().URL_USER_LOGOUT),
                msg("Logout"), " class=\"navlink\" ") + HtmlUtil.space(1)
                    + "|" + HtmlUtil.space(1)
                    + HtmlUtil.href(
                        request.url(getRepositoryBase().URL_USER_SETTINGS),
                        user.getLabel(),
                        " class=\"navlink\" ") + HtmlUtil.space(1);
        }
        return cartEntry + HtmlUtil.space(2) + userLink;
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
    public Result processHome(Request request) throws Exception {
        StringBuffer sb   = new StringBuffer();
        User         user = request.getUser();
        if (user.getAnonymous()) {
            if (request.getOutput().equals("xml")) {
                return new Result(XmlUtil.tag(TAG_RESPONSE,
                        XmlUtil.attr(ATTR_CODE, "error"),
                        "No user defined"), MIME_XML);
            }
            String msg = msg("No user defined");
            if (request.exists(ARG_FROMLOGIN)) {
                msg = msg + HtmlUtil.p()
                      + msg("If you had logged in perhaps you have cookies turned off?");
            }
            sb.append(getRepository().warning(msg));
            sb.append(makeLoginForm(request));
        } else if (request.defined(ARG_MESSAGE)) {
            sb.append(
                getRepository().note(
                    request.getUnsafeString(ARG_MESSAGE, "")));
        }
        if (request.getOutput().equals("xml")) {
            return new Result(XmlUtil.tag(TAG_RESPONSE,
                                          XmlUtil.attr(ATTR_CODE, "ok"),
                                          user.getId()), MIME_XML);
        }
        return makeResult(request, "User Home", sb);
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
    public Result processLogin(Request request) throws Exception {
        StringBuffer sb     = new StringBuffer();
        User         user   = null;
        String       output = request.getOutput();
        if (request.exists(ARG_USER_ID)) {
            String name     = request.getString(ARG_USER_ID, "");
            String password = request.getString(ARG_USER_PASSWORD, "");
            password = hashPassword(password);

            Statement stmt = getDatabaseManager().select(COLUMNS_USERS,
                                 TABLE_USERS,
                                 Clause.and(Clause.eq(COL_USERS_ID, name),
                                            Clause.eq(COL_USERS_PASSWORD,
                                                password)));

            ResultSet results = stmt.getResultSet();
            if (results.next()) {
                user = getUser(results);
                setUserSession(request, user);
                if (output.equals("xml")) {
                    return new Result(XmlUtil.tag(TAG_RESPONSE,
                            XmlUtil.attr(ATTR_CODE, "ok"),
                            request.getSessionId()), MIME_XML);
                }
                if (request.exists(ARG_REDIRECT)) {
                    String redirect = new String(
                                          XmlUtil.decodeBase64(
                                              request.getUnsafeString(
                                                  ARG_REDIRECT, "")));
                    return new Result(HtmlUtil.url(redirect, ARG_FROMLOGIN,
                            "true", ARG_MESSAGE, msg("You are logged in")));
                } else {
                    return new Result(
                        request.url(
                            getRepositoryBase().URL_USER_HOME, ARG_FROMLOGIN,
                            "true", ARG_MESSAGE, msg("You are logged in")));
                }
            } else {
                if (output.equals("xml")) {
                    return new Result(XmlUtil.tag(TAG_RESPONSE,
                            XmlUtil.attr(ATTR_CODE, "error"),
                            "Incorrect user name or password"), MIME_XML);
                }
                sb.append(
                    getRepository().warning(
                        msg("Incorrect user name or password")));
            }
            stmt.close();
        }


        if (user == null) {
            sb.append(makeLoginForm(request));
        }
        return new Result(msg("Login"), sb);
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
    public Result processLogout(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        removeUserSession(request);
        request.setSessionId(getSessionId());
        sb.append(getRepository().note(msg("You are logged out")));
        sb.append(makeLoginForm(request));
        Result result = new Result(msg("Logout"), sb);
        return result;
    }


    /** _more_ */
    public static final String OUTPUT_CART = "user.cart";

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initOutputHandlers() throws Exception {
        OutputHandler outputHandler = new OutputHandler(getRepository()) {
            protected void getEntryLinks(Request request, Entry entry,
                                         List<Link> links, boolean forHeader)
                    throws Exception {
                links.add(new Link(request.url(getRepository().URL_USER_CART,
                        ARG_ACTION, ACTION_ADD, ARG_ID,
                        entry.getId()), getRepository().fileUrl(ICON_CART),
                                        msg("Add to cart")));
            }


            public boolean canHandle(String output) {
                return output.equals(OUTPUT_CART);
            }


            protected void addOutputTypes(Request request, State state,
                                          List<OutputType> types)
                    throws Exception {
                if ( !state.forHeader()) {
                    types.add(new OutputType("Cart", OUTPUT_CART));
                }
            }

            public Result outputGroup(Request request, Group group,
                                      List<Group> subGroups,
                                      List<Entry> entries)
                    throws Exception {
                addToCart(request, entries);
                return showCart(request);
            }
        };

        getRepository().addOutputHandler(outputHandler);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> getRoles() throws Exception {
        String[] roleArray = SqlUtil.readString(
                                 getDatabaseManager().select(
                                     SqlUtil.distinct(COL_USERROLES_ROLE),
                                     TABLE_USERROLES, new Clause()), 1);
        List<String> roles = new ArrayList<String>(Misc.toList(roleArray));
        roles.add(0, ROLE_ANY);
        return roles;
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
    public Result processSettings(Request request) throws Exception {
        StringBuffer sb   = new StringBuffer();

        User         user = request.getUser();
        if (user.getAnonymous()) {
            sb.append(
                getRepository().warning(
                    msg("You need to be logged in to change user settings")));
            sb.append(makeLoginForm(request));
            return new Result(msg("User Settings"), sb);
        }

        if (request.exists(ARG_USER_CHANGE)) {
            boolean okToChangeUser = checkPasswords(request, user);
            if ( !okToChangeUser) {
                sb.append(
                    getRepository().warning(msg("Incorrect passwords")));
            } else {
                applyState(request, user, false);
                return new Result(
                    request.url(
                        getRepositoryBase().URL_USER_SETTINGS, ARG_MESSAGE,
                        msg("User settings changed")));
            }
        }

        if (request.defined(ARG_MESSAGE)) {
            sb.append(
                getRepository().note(
                    request.getUnsafeString(ARG_MESSAGE, "")));
        }

        sb.append(request.form(getRepositoryBase().URL_USER_SETTINGS));
        sb.append(HtmlUtil.submit(msg("Change Settings"), ARG_USER_CHANGE));
        makeUserForm(request, user, sb, false);
        sb.append(HtmlUtil.submit(msg("Change Settings"), ARG_USER_CHANGE));
        sb.append(HtmlUtil.formClose());

        String roles = user.getRolesAsString("<br>").trim();
        sb.append(HtmlUtil.formEntry(HtmlUtil.space(1), ""));
        if (roles.length() == 0) {
            roles = "--none--";
        }
        sb.append(HtmlUtil.formEntryTop(msgLabel("Roles"), roles));

        sb.append(HtmlUtil.formTableClose());
        return makeResult(request, msg("User Settings"), sb);
    }


    /**
     * Class Session _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class Session {

        /** _more_ */
        String id;

        /** _more_ */
        User user;

        /** _more_ */
        Date createDate;

        /** _more_ */
        Date lastActivity;

        /**
         * _more_
         *
         * @param id _more_
         * @param user _more_
         * @param createDate _more_
         */
        public Session(String id, User user, Date createDate) {
            this.id         = id;
            this.user       = user;
            this.createDate = createDate;
            lastActivity    = new Date();
        }
    }


}

