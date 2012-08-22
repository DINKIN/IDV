/*
 * Copyright 1997-2012 Unidata Program Center/University Corporation for
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

package ucar.unidata.util;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;


/**
 * The tests for the Misc class.
 */
public class MiscTest {


    /**
     * Test the Misc.decodeLatLon method.
     */
    @Test
    public void testDecodeLatLonsString() {

        /** The Constant DELTA. */
        final double                                          DELTA = 0.001;

        @SuppressWarnings("serial") final Map<String, Double> map   =
            new HashMap<String, Double>() {
            {
                put("4:25:W", -4.416666666);
                put("-9E-3", -0.0090);
                put("4:25:E", 4.416666666);
                put("65:02:06", 65.035);
                put("-147:30:06", -147.50166);
                put("31.77", 31.77);
                put("-95.71", -95.71);
                put("53:26:N", 53.4333);
                put("8:20:S", -8.3333);
            }
        };

        for (Map.Entry<String, Double> e : map.entrySet()) {
            assertEquals("Could not properly decode lat / lon",
                         Misc.decodeLatLon(e.getKey()), e.getValue(), DELTA);
        }
    }

    /**
     *     DD:MM:SS      ===>  -34:29:45
     *       (if longitude and use360 ===> 326:29:45)
     *     DDH           ===>   34W     (or 34S if longitude)
     *     DD.d          ===>  -34.5
     *     DD.dddH       ===>   34.496W (or 34.496S if longitude)
     *     DD MM" SS.s'  ===>  -34 29" 45.6'
     *
     */


    /**
     * Test the Misc.formatLatitude method.
     */
    @Test
    public void formatLat() {
        String errorMsg = "Could not properly format lat";

        //Trivial test
        assertEquals(errorMsg, "12", Misc.formatLatitude(12, "DD"));

        //Testing various formats
        assertEquals(errorMsg, "12:00", Misc.formatLatitude(12, "DD:MM"));

        assertEquals(errorMsg, "12:00:00",
                     Misc.formatLatitude(12, "DD:MM:SS"));

        assertEquals(errorMsg, "12.0", Misc.formatLatitude(12, "DD.d"));

        assertEquals(errorMsg, "12.0:00", Misc.formatLatitude(12, "DD.d:MM"));

        assertEquals(errorMsg, "12 30'", Misc.formatLatitude(12.5, "DD MM'"));
        assertEquals(errorMsg, "12 33' 18\"", Misc.formatLatitude(12.555, "DD MM' SS\""));


        //Testing cardinalities
        assertEquals(errorMsg, "12N", Misc.formatLatitude(12, "DDH"));

        assertEquals(errorMsg, "12S", Misc.formatLatitude(-12, "DDH"));

        //Testing negative
        assertEquals(errorMsg, "-12", Misc.formatLatitude(-12, "DD"));

    }

    /**
     * Test the Misc.formatLongitude method.
     */
    @Test
    public void formatLongitude() {
        String errorMsg = "Could not properly format lon";


        //Testing cardinalities
        assertEquals(errorMsg, "12E", Misc.formatLongitude(12, "DDH", true));

        assertEquals(errorMsg, "12W", Misc.formatLongitude(-12, "DDH", true));

        assertEquals(errorMsg, "-12", Misc.formatLongitude(-12, "DDH", true));
    }


}
