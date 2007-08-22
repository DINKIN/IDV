/*
 * $Id: RadarQuery.java,v 1.2 2007/07/03 18:31:07 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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



package ucar.unidata.data.radar;


import ucar.unidata.util.DateSelection;


/**
 * @author IDV Development Team
 * @version $Revision: 1.2 $
 */
public class RadarQuery {

    /** ur to the radar collection */
    private String collectionUrl;

    /** The station */
    private String station;

    /** The list of dates */
    private DateSelection dateSelection;


    /**
     * ctor for encoding
     */
    public RadarQuery() {}

    /**
     * ctor
     *
     * @param collectionUrl the collection url
     * @param station the station
     * @param dateSelection the date selection_
     */
    public RadarQuery(String collectionUrl, String station,
                      DateSelection dateSelection) {
        this.collectionUrl = collectionUrl;
        this.station       = station;
        this.dateSelection = dateSelection;
    }


    /**
     * Set the CollectionUrl property.
     *
     * @param value The new value for CollectionUrl
     */
    public void setCollectionUrl(String value) {
        collectionUrl = value;
    }

    /**
     * Get the CollectionUrl property.
     *
     * @return The CollectionUrl
     */
    public String getCollectionUrl() {
        return collectionUrl;
    }


    /**
     * Set the Station property.
     *
     * @param value The new value for Station
     */
    public void setStation(String value) {
        station = value;
    }

    /**
     * Get the Station property.
     *
     * @return The Station
     */
    public String getStation() {
        return station;
    }

    /**
     *  Set the DateSelection property.
     *
     *  @param value The new value for DateSelection
     */
    public void setDateSelection(DateSelection value) {
        dateSelection = value;
    }

    /**
     *  Get the DateSelection property.
     *
     *  @return The DateSelection
     */
    public DateSelection getDateSelection() {
        return dateSelection;
    }


}

