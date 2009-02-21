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
 * along with this library; if not, write to the Free Software Foundastion,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */



package ucar.unidata.repository.monitor;


import ucar.unidata.repository.*;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.xml.XmlUtil;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public abstract class PasswordAction extends MonitorAction {

    /** _more_ */
    private String remoteUserId = "";

    /** _more_ */
    private String password = "";

    /**
     * _more_
     */
    public PasswordAction() {}



    /**
     * _more_
     *
     * @param repository _more_
     * @param user _more_
     * @param remoteUserId _more_
     * @param password _more_
     */
    public PasswordAction(String id, String remoteUserId, String password) {
        super(id);
        this.remoteUserId = remoteUserId;
        this.password     = password;
    }



    /**
     *  Method for encoding to xml the password. This simply obfuscates what is saved to disk
     *
     *  @param value The new value
     */
    public void setTmp(byte[] value) {
        if (value == null) {
            password = null;
        } else {
            password = new String(XmlUtil.decodeBase64(new String(value)));
        }
    }



    /**
     *  Method for encoding to xml the password. This simply obfuscates what is saved to disk
     *
     *  @return The Password
     */
    public byte[] getTmp() {
        if (password == null) {
            return null;
        }
        return XmlUtil.encodeBase64(password.getBytes()).getBytes();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected String getPassword() {
        return password;
    }


    /**
     * Set the RemoteUserId property.
     *
     * @param value The new value for RemoteUserId
     */
    public void setRemoteUserId(String value) {
        remoteUserId = value;
    }

    /**
     * Get the RemoteUserId property.
     *
     * @return The RemoteUserId
     */
    public String getRemoteUserId() {
        return remoteUserId;
    }


}

