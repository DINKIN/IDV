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
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

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



import java.util.regex.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Request implements Constants {

    /** _more_ */
    public static final String CALL_QUERY = "/query";

    /** _more_          */
    public static final String CALL_FETCH = "/fetch";

    /** _more_ */
    public static final String CALL_SQL = "/sql";

    /** _more_ */
    public static final String CALL_SEARCHFORM = "/searchform";

    /** _more_ */
    public static final String CALL_LIST = "/list";

    /** _more_ */
    public static final String CALL_SHOWGROUP = "/showgroup";

    /** _more_ */
    public static final String CALL_SHOWFILE = "/showfile";

    /** _more_ */
    public static final String CALL_GRAPH = "/graph";

    /** _more_ */
    public static final String CALL_GRAPHVIEW = "/graphview";


    /** _more_ */
    private String type;

    /** _more_ */
    private RequestContext requestContext;

    /** _more_ */
    private Hashtable parameters;

    private Hashtable originalParameters;

    private Repository repository;

    public String toString() {
        return type + " params:" + parameters;
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param requestContext _more_
     * @param parameters _more_
     */
    public Request(Repository repository, String type, RequestContext requestContext,
                   Hashtable parameters) {
        this.repository     = repository;
        this.type           = type;
        this.requestContext = requestContext;
        this.parameters     = parameters;
        this.originalParameters= new Hashtable();
        originalParameters.putAll(parameters);
    }

    public String getUrlArgs() {
        StringBuffer sb = new StringBuffer();
        int cnt = 0;
        for (Enumeration keys = parameters.keys();
                keys.hasMoreElements(); ) {
            String arg = (String) keys.nextElement();
            String value = (String) parameters.get(arg);
            if(value.length()==0) continue;
            if(cnt++>0)
                sb.append("&");
            sb.append(arg+"="+value);
        }
        return sb.toString();
    }


    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !o.getClass().equals(getClass())) {
            return false;
        }
        Request that = (Request) o;
        return this.type.equals(that.type)
               && this.requestContext.equals(that.requestContext)
               && this.originalParameters.equals(that.originalParameters);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return type.hashCode() ^ requestContext.hashCode()
            ^ originalParameters.hashCode();
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public boolean hasSetParameter(String key) {
        String v = (String) parameters.get(key);
        if ((v == null) || (v.trim().length() == 0)) {
            return false;
        }
        return true;
    }


    public void remove(String key) {
        parameters.remove(key);
    }

    public void put(String key, String value) {
        parameters.put(key,value);
    }


    public boolean defined(String key) {
        String result = (String) get(key,(String)null);
        if (result == null) {
            return false;
        }
        if(result.trim().length()==0) return false;
        return true;
    }

    public String getUnsafeString(String key, String dflt) {
        String result = (String)get(key,(String)null);
        if (result == null) {
            return dflt;
        }
        return result;
    }


    private static Pattern checker;

    public String getCheckedString(String key, String dflt, String patternString) {
        return getCheckedString(key,dflt, Pattern.compile(patternString));
    }


    public String getCheckedString(String key, String dflt, Pattern pattern) {
        String v = (String)get(key,(String)null);
        if (v == null) {
            return dflt;
        }
        Matcher matcher = pattern.matcher(v);
        if(!matcher.find()) {
            throw new BadInputException("Incorrect input for:" + key+" value:" + v+":");
        }
        //TODO:Check the value
        return v;
    }


    public String getString(String key, String dflt) {
        if(checker==null) {
            checker = Pattern.compile(repository.getProperty(PROP_DB_PATTERN));
        }
        return getCheckedString(key,dflt, checker);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private String get(String key, String dflt) {
        String result = (String)parameters.get(key);
        if (result == null) {
            return dflt;
        }
        return result;
    }

    public String getOutput() {
        return getOutput(OutputHandler.OUTPUT_HTML);
    }

    public String getOutput(String dflt) {
        return  getString(ARG_OUTPUT,dflt);
    }


    public String getId(String dflt) {
        return  getString(ARG_ID,dflt);
    }


    public String getIds(String dflt) {
        return  getString(ARG_IDS,dflt);
    }

    public String getDateSelect(String name, String dflt) {
        String v =  getUnsafeString(name,(String)null);
        if(v==null) return dflt;
        //TODO:Check value
        return v;
    }

    public String getWhat(String dflt) {
        return   getString(ARG_WHAT,dflt);
    }

    public String getType(String dflt) {
        return  getString(ARG_TYPE,dflt);
    }


    public String getUser() {
        return   getString(ARG_USER,(String)null);
    }



    public int get(String key, int dflt) {
        String result = (String)get(key,(String)null);
        if (result == null || result.trim().length()==0) {
            return dflt;
        }
        return new Integer(result).intValue();
    }

    public double get(String key, double dflt) {
        String result = (String)get(key,(String)null);
        if (result == null || result.trim().length()==0) {
            return dflt;
        }
        return new Double(result).doubleValue();
    }


    public Date get(String key, Date dflt) throws java.text.ParseException {
        String result = (String)get(key,(String)null);
        if (result == null || result.trim().length()==0) {
            return dflt;
        }
        return  DateUtil.parse(result);
    }


    public boolean get(String key, boolean dflt) {
        String result =(String) get(key,(String)null);
        if (result == null || result.trim().length()==0) {
            return dflt;
        }
        return new Boolean(result).booleanValue();
    }


    public Enumeration keys() {
        return parameters.keys();
    }

    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the RequestContext property.
     *
     * @param value The new value for RequestContext
     */
    public void setRequestContext(RequestContext value) {
        requestContext = value;
    }

    /**
     * Get the RequestContext property.
     *
     * @return The RequestContext
     */
    public RequestContext getRequestContext() {
        return requestContext;
    }


    public static class BadInputException extends RuntimeException {
        public BadInputException(String msg) {
            super(msg);
        }
    }


}

