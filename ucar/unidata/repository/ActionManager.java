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
public class ActionManager extends RepositoryManager {


    public RequestUrl URL_STATUS = new RequestUrl(this, "/status");


    private Hashtable<Object,ActionInfo> actions = new Hashtable<Object,ActionInfo>();


    /**
     * _more_
     *
     * @param repository _more_
     */
    public ActionManager(Repository repository) {
        super(repository);
    }



    public Result processStatus(Request request) throws Exception {

        String id = request.getString(ARG_ACTION_ID,"");
        ActionInfo action = getAction(id);

        StringBuffer sb = new StringBuffer();
        if(action == null) {
            sb.append("No action found");
        } else {
            sb.append("<h3>Action: " + action.getName() +"</h3>");
            if(action.getError()!=null) {
                sb.append("Completed<br>");
                sb.append(action.getError());
            } else   if(!action.getRunning()) {
                sb.append("Completed<br>");
                sb.append(action.getContinueHtml());
                actions.remove(id);
            } else {
                sb.append("In progress<br>");
                sb.append(action.getMessage());
            }
        }
        return new Result("Status",  sb);
    }



    protected ActionInfo getAction(Object id) {
        if(id ==null) return null;
        return  actions.get(id);
    }


    protected boolean getActionOk(Object id) {
        ActionInfo action = getAction(id);
        if(action == null) return false;
        return action.getRunning();
    }


    protected void setActionMessage(Object id, String msg) {
        ActionInfo action = getAction(id);
        if(action == null) return;
        action.setMessage(msg);
    }


    protected void actionComplete(Object id) {
        ActionInfo action = getAction(id);
        if(action == null) return;
        action.setRunning(false);
    }

    protected void handleError(Object actionId, Exception exc) {
        ActionInfo action = getAction(actionId);
        if(action == null) return;
        action.setError("An error has occurred:" + exc);
    }

    protected Object addAction(String msg, String continueHtml) {
        String id = getRepository().getGUID();
        actions.put(id, new ActionInfo(msg, continueHtml));
        return id;
    }


    protected Object runAction(final Action runnable, String name, String continueHtml) {
        final Object actionId = addAction(name, continueHtml);
        Misc.run(new Runnable() {
                public void run() {
                    try {
                        runnable.run(actionId);
                    } catch(Exception exc) {
                        //TODO: handle the error better
                        handleError(actionId, exc);
                        return;
                    }
                    actionComplete(actionId);
                }
            });
        return actionId;
    }


    public static interface Action {
        public void run(Object actionId) throws Exception;
    }


    public static class ActionInfo {

    private String name;
    private boolean running=true;
    private String message="";
    private String continueHtml;
        private String error=null;


    public ActionInfo(String name, String continueHtml) {
        this.name = name;
        this.continueHtml = continueHtml;
    }


    /**
       Set the Name property.

       @param value The new value for Name
    **/
    public void setName (String value) {
	name = value;
    }

    /**
       Get the Name property.

       @return The Name
    **/
    public String getName () {
	return name;
    }

    /**
       Set the Running property.

       @param value The new value for Running
    **/
    public void setRunning (boolean value) {
	running = value;
    }

    /**
       Get the Running property.

       @return The Running
    **/
    public boolean getRunning () {
	return running;
    }


    
/**
Set the Message property.

@param value The new value for Message
**/
public void setMessage (String value) {
	message = value;
}

/**
Get the Message property.

@return The Message
**/
public String getMessage () {
	return message;
}


/**
Set the ContinueHtml property.

@param value The new value for ContinueHtml
**/
public void setContinueHtml (String value) {
	continueHtml = value;
}

/**
Get the ContinueHtml property.

@return The ContinueHtml
**/
public String getContinueHtml () {
	return continueHtml;
}

/**
Set the Error property.

@param value The new value for Error
**/
public void setError (String value) {
	error = value;
}

/**
Get the Error property.

@return The Error
**/
public String getError () {
	return error;
}




}



}

