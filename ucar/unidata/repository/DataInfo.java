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


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.util.Date;
import java.util.List;


/**
 * Class DataInfo _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class DataInfo {

    private      String name;
    private      String description;


    /** _more_          */
private      Group group;

    /** _more_ */
private      String file;

    /** _more_ */
private      long startDate;

    /** _more_          */
private      long endDate;

private      String type;

    /**
     * _more_
     *
     * @param group _more_
     * @param file _more_
     * @param date _more_
     */
    public DataInfo(String name, String type, Group group, String file, long date) {
        this(name, type, group, file, date, date);
    }



    /**
     * _more_
     *
     *
     * @param group _more_
     * @param file _more_
     * @param station _more_
     * @param product _more_
     * @param date _more_
     * @param startDate _more_
     * @param endDate _more_
     */
    public DataInfo(String name, String type, Group group,  String file, long startDate, long endDate) {
        this.type = type;
        this.name = name;
        this.group     = group;
        this.file      = file;
        this.startDate = startDate;
        this.endDate   = endDate;
    }

    /**
     * Set the File property.
     *
     * @param value The new value for File
     */
    public void setFile(String value) {
        file = value;
    }

    /**
     * Get the File property.
     *
     * @return The File
     */
    public String getFile() {
        return file;
    }

    /**
     * Set the StartDate property.
     *
     * @param value The new value for StartDate
     */
    public void setStartDate(long value) {
        startDate = value;
    }

    /**
     * Get the StartDate property.
     *
     * @return The StartDate
     */
    public long getStartDate() {
        return startDate;
    }

    /**
     * Set the EndDate property.
     *
     * @param value The new value for EndDate
     */
    public void setEndDate(long value) {
        endDate = value;
    }

    /**
     * Get the EndDate property.
     *
     * @return The EndDate
     */
    public long getEndDate() {
        return endDate;
    }



    /**
     * Set the Group property.
     *
     * @param value The new value for Group
     */
    public void setGroup(Group value) {
        group = value;
    }

    /**
     * Get the Group property.
     *
     * @return The Group
     */
    public Group getGroup() {
        return group;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getGroupId() {
        if (group != null) {
            return group.getId();
        }
        return "";
    }
/**
Set the Type property.

@param value The new value for Type
**/
public void setType (String value) {
        type = value;
}

/**
Get the Type property.

@return The Type
**/
public String getType () {
        return type;
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
Set the Description property.

@param value The new value for Description
**/
public void setDescription (String value) {
	description = value;
}

/**
Get the Description property.

@return The Description
**/
public String getDescription () {
	return description;
}


}

