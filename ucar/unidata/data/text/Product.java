/*
 * $Id: FrontDataSource.java,v 1.15 2007/04/17 22:22:52 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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



package ucar.unidata.data.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a named product
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */

public class Product {
    private String name;
    private String id;

    public Product(String name, String id) {
        this.name  = name;
        this.id = id;
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
       Set the Id property.

       @param value The new value for Id
    **/
    public void setId (String value) {
        id = value;
    }

    /**
       Get the Id property.

       @return The Id
    **/
    public String getId () {
        return id;
    }


    public String toString() {
        return name;
    }

}
