/*
 * $Id: TrackControl.java,v 1.69 2007/08/21 11:32:08 jeffmc Exp $
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


package ucar.unidata.idv.control.storm;


import ucar.unidata.data.storm.*;


import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;

import ucar.visad.display.*;
import ucar.unidata.util.LogUtil;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import visad.*;


/**
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class WayDisplayState {

    private StormDisplayState stormDisplayState;

    private JCheckBox visibilityCbx;


    /** _more_          */
    private Way way;

    /** _more_          */
    private boolean visible = true;

    /** _more_          */
    List<Displayable> displayables = new ArrayList<Displayable>();
    private List<StormTrack> tracks = new ArrayList<StormTrack>();
    private List<FieldImpl> fields = new ArrayList<FieldImpl>();
    private List<DateTime> times = new ArrayList<DateTime>();
    private List<PointOb> pointObs = new ArrayList<PointOb>();

    private Color color;

    public WayDisplayState() {
    }






    /**
     * _more_
     *
     * @param way _more_
     */
    public WayDisplayState(StormDisplayState stormDisplayState, Way way) {
        this.stormDisplayState = stormDisplayState;
        this.way = way;
    }

    public void deactivate() {
        displayables = new ArrayList<Displayable>();
        tracks = new ArrayList<StormTrack>();
        fields = new ArrayList<FieldImpl>();
        times = new ArrayList<DateTime>();
        pointObs = new ArrayList<PointOb>();
    }
    




    public JCheckBox getVisiblityCheckBox() {
        if(visibilityCbx==null) {
            visibilityCbx = new JCheckBox((way.isObservation()?"Show Observation Track":way.toString()), getVisible());
            visibilityCbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        setVisible(visibilityCbx.isSelected());
                    } catch(Exception exc) {
                        LogUtil.logException("Toggling way visibility", exc);
                    }
                }
            });


        }
        return visibilityCbx;
    }


    public List<PointOb> getPointObs() {
        return pointObs;
    }

    private static TextType textType;
    public void addTrack(StormTrack track, FieldImpl field) throws Exception {
        tracks.add(track);
        times.add(track.getTrackStartTime());
        fields.add(field);

        boolean isObservation = way.isObservation();
        DateTime startTime = track.getTrackStartTime();
        List<StormTrackPoint> locs     = track.getTrackPoints();
        //        return makePointOb(el,dt, new RealTuple(new Real[] { new Real(0) }));
        if(textType == null) {
            textType = new TextType("label");
        }

        for(int i=0;i<locs.size();i++) {
            StormTrackPoint stp = locs.get(i);
            DateTime time  =startTime;
            String label = "";
            if(isObservation) {
                time = stp.getTrackPointTime();
            } else {
                if(i==0) {
                    label = way +": "+track.getTrackStartTime();
                }  else {
                    label = ""+stp.getForecastHour()+"H";
                }
            }
            Tuple tuple = new Tuple(new Data[]{new visad.Text(textType,label)});
            pointObs.add(PointObFactory.makePointOb(stp.getTrackPointLocation(), time,tuple));
        }


    }


    public List getFields() {
        return fields;
    }
    public List<StormTrack> getTracks() {
        return tracks;
    }

    public List<DateTime> getTimes() {
        return times;
    }

    /**
     * _more_
     *
     * @param displayable _more_
     */
    public void addDisplayable(Displayable displayable) throws Exception {
        displayables.add(displayable);
        if(way.isObservation()) {
            displayable.setVisible(getVisible());
        } else {
            displayable.setVisible(getVisible() && stormDisplayState.getForecastVisible());
        }
    }



    /**
     * Set the Way property.
     *
     * @param value The new value for Way
     */
    public void setWay(Way value) {
        way = value;
    }

    /**
     * Get the Way property.
     *
     * @return The Way
     */
    public Way getWay() {
        return way;
    }


    /**
     * Set the Visible property.
     *
     * @param value The new value for Visible
     */
    public void setVisible(boolean value) throws Exception {
        this.visible = value;
        for(Displayable displayable: displayables) {
            if(way.isObservation()) {
                displayable.setVisible(getVisible());
            } else {
                displayable.setVisible(getVisible() && stormDisplayState.getForecastVisible());
            }
        }
    }

    /**
     * Get the Visible property.
     *
     * @return The Visible
     */
    public boolean getVisible() {
        return visible;
    }

/**
Set the Color property.

@param value The new value for Color
**/
public void setColor (Color value) {
	color = value;
}

/**
Get the Color property.

@return The Color
**/
public Color getColor () {
	return color;
}


/**
Set the StormDisplayState property.

@param value The new value for StormDisplayState
**/
public void setStormDisplayState (StormDisplayState value) {
	stormDisplayState = value;
}

/**
Get the StormDisplayState property.

@return The StormDisplayState
**/
public StormDisplayState getStormDisplayState () {
	return stormDisplayState;
}



}

