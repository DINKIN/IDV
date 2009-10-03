/*
 * $Id: ViewManager.java,v 1.401 2007/08/16 14:05:04 jeffmc Exp $
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
 * This library is distributed in the hope that it will be2 useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */




package ucar.unidata.idv;


import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ThermometerPlot;
import org.jfree.data.general.DefaultValueDataset;


import org.w3c.dom.*;

import org.w3c.dom.*;

import ucar.unidata.collab.*;


import ucar.unidata.data.gis.KmlUtil;

import ucar.unidata.idv.control.ReadoutInfo;
import ucar.unidata.idv.ui.CursorReadoutWindow;
import ucar.unidata.ui.LatLonWidget;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.*;
import ucar.unidata.view.geoloc.*;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.ShapeUtility;

import ucar.visad.Util;

import ucar.visad.display.*;

import visad.*;

import visad.georef.*;

import visad.java3d.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.media.j3d.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import javax.vecmath.*;

import javax.vecmath.*;


/**
 *
 * @author IDV development team
 */

public class Flythrough extends SharableImpl implements PropertyChangeListener {

    /** _more_          */
    public static final String ORIENT_POINT = "point";

    /** _more_          */
    public static final String ORIENT_UP = "up";

    /** _more_          */
    public static final String ORIENT_DOWN = "down";

    /** _more_          */
    public static final String ORIENT_LEFT = "left";

    /** _more_          */
    public static final String ORIENT_RIGHT = "right";

    /** _more_ */
    public static final int COL_LAT = 0;

    /** _more_ */
    public static final int COL_LON = 1;

    /** _more_ */
    public static final int COL_ALT = 2;

    /** _more_ */
    public static final int COL_DATE = 3;

    /** _more_ */
    private JTextField clipFld;

    /** _more_ */
    private JTextField[] cflds = { null, null, null, null };

    /** _more_ */
    public static final String TAG_FLYTHROUGH = "flythrough";

    /** _more_ */
    public static final String TAG_DESCRIPTION = "description";

    /** _more_ */
    public static final String TAG_POINT = "point";

    /** _more_ */
    public static final String ATTR_DATE = "date";

    /** _more_ */
    public static final String ATTR_LAT = "lat";

    /** _more_ */
    public static final String ATTR_LON = "lon";

    /** _more_ */
    public static final String ATTR_ALT = "alt";

    /** _more_ */
    public static final String[] ATTR_TILT = { "tiltx", "tilty", "tiltz" };


    /** _more_ */
    public static final String ATTR_ZOOM = "zoom";

    /** _more_ */
    public static final String ATTR_MATRIX = "matrix";


    /** _more_ */
    private SimpleDateFormat sdf;

    /** _more_ */
    private MapViewManager viewManager;

    /** _more_ */
    private List<FlythroughPoint> points = new ArrayList<FlythroughPoint>();


    /** _more_ */
    private double[] tilt = { 0.0, 0.0, 0.0 };

    /** _more_          */
    JSlider[] tiltSliders = { null, null, null };

    /** _more_          */
    JLabel[] tiltLabels = { null, null, null };


    /** _more_ */
    private double zoom = 1.0;

    /** _more_ */
    private boolean changeViewpoint = true;

    /** _more_ */
    private boolean animate = false;

    /** _more_          */
    private int animationSpeed = 50;

    /** _more_ */
    private JCheckBox animateCbx;

    /** _more_ */
    private JCheckBox clipCbx;


    /** _more_ */
    private boolean showLine = true;

    /** _more_          */
    private boolean showMarker = true;


    /** _more_ */
    private boolean showReadout = true;


    /** Animation info */
    private AnimationInfo animationInfo;

    /** _more_ */
    private Animation animation;

    /** The anim widget */
    private AnimationWidget animationWidget;

    /** _more_ */
    private boolean hasTimes = false;

    /** _more_ */
    private boolean showTimes = false;

    /** _more_ */
    private String orientation = ORIENT_POINT;

    /** _more_ */
    private boolean clip = false;

    /** _more_ */
    private JCheckBox showTimesCbx;

    /** _more_ */
    private FlythroughPoint currentPoint;

    /** _more_ */
    private CursorReadoutWindow readout;

    /** _more_ */
    private JLabel readoutLabel;

    /** _more_          */
    private JComponent readoutDisplay;


    /** _more_ */
    private JCheckBox showReadoutCbx;

    /** _more_ */
    private JCheckBox changeViewpointCbx;

    /** _more_ */
    private JCheckBox overheadCbx;

    /** _more_ */
    private JTextField zoomFld;

    /** _more_ */
    private JComboBox orientBox;

    /** _more_          */
    private Vector<TwoFacedObject> orients = new Vector<TwoFacedObject>();


    /** _more_          */
    private JCheckBox fixedZCbx;


    /** _more_ */
    private JTextField tiltxFld;

    /** _more_ */
    private JTextField tiltyFld;

    /** _more_ */
    private JTextField tiltzFld;

    /** _more_ */
    private JRadioButton backBtn;


    /** _more_ */
    private JFrame frame;

    /** The line from the origin to the point */
    private LineDrawing locationLine;

    /** _more_ */
    private SelectorPoint locationMarker;

    /** _more_ */
    private JTable pointTable;

    /** _more_ */
    private AbstractTableModel pointTableModel;

    /** _more_ */
    private JEditorPane htmlView;


    /** _more_ */
    private JComponent contents;

    /** _more_ */
    private boolean shown = false;

    /** _more_          */
    private boolean useFixedZ = true;

    /** _more_          */
    private int currentIndex = 0;

    /**
     * _more_
     */
    public Flythrough() {
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z");
        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
    }


    /**
     * _more_
     *
     * @param viewManager _more_
     */
    public Flythrough(MapViewManager viewManager) {
        this();
        this.viewManager = viewManager;
    }


    /**
     * _more_
     *
     * @param that _more_
     */

    public void initWith(Flythrough that) {
        if (this == that) {
            return;
        }
        this.points          = new ArrayList<FlythroughPoint>(that.points);
        this.animationInfo   = that.animationInfo;

        this.tilt = new double[] { that.tilt[0], that.tilt[1], that.tilt[2] };
        this.zoom            = that.zoom;
        this.changeViewpoint = that.changeViewpoint;
        this.showLine        = that.showLine;
        this.showMarker      = that.showMarker;
        this.showReadout     = that.showReadout;
        this.showTimes       = that.showTimes;
        setAnimationTimes();
    }


    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void destroy() throws VisADException, RemoteException {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }

        if ((locationMarker != null) && (viewManager != null)) {
            viewManager.getMaster().removeDisplayable(locationMarker);
            viewManager.getMaster().removeDisplayable(locationLine);
        }


        if (animationWidget != null) {
            animationWidget.destroy();
            animationWidget = null;
        }
        viewManager = null;
    }



    /**
     * Set the AnimationInfo property.
     *
     * @param value The new value for AnimationInfo
     */
    public void setAnimationInfo(AnimationInfo value) {
        animationInfo = value;
    }


    /**
     * Get the AnimationInfo property.
     *
     * @return The AnimationInfo
     */
    public AnimationInfo getAnimationInfo() {
        if (animationWidget != null) {
            animationInfo = animationWidget.getAnimationInfo();
        }
        return animationInfo;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public AnimationWidget getAnimationWidget() {
        return animationWidget;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Animation getAnimation() {
        return animation;
    }



    /**
     * _more_
     *
     * @param ld _more_
     * @param x1 _more_
     * @param x2 _more_
     * @param y1 _more_
     * @param y2 _more_
     * @param z1 _more_
     * @param z2 _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private void setPts(LineDrawing ld, float x1, float x2, float y1,
                        float y2, float z1, float z2)
            throws VisADException, RemoteException {
        MathType  mathType = RealTupleType.SpatialCartesian3DTuple;
        float[][] pts      = new float[][] {
            { x1, x2 }, { y1, y2 }, { z1, z2 }
        };
        ld.setData(new Gridded3DSet(mathType, pts, 2));
    }




    /**
     * _more_
     *
     * @param pts _more_
     */
    public void flythrough(final float[][] pts) {
        List<FlythroughPoint> points     = new ArrayList<FlythroughPoint>();
        NavigatedDisplay      navDisplay = viewManager.getNavigatedDisplay();
        for (int i = 0; i < pts[0].length; i++) {
            EarthLocation el = navDisplay.getEarthLocation(pts[0][i],
                                   pts[1][i], pts[2][i], false);
            points.add(new FlythroughPoint(el));
        }
        this.points = points;
        setAnimationTimes();
        show();
    }



    /**
     * tmp
     *
     *
     * @param newPoints _more_
     *
     */
    public void flythrough(List<FlythroughPoint> newPoints) {
        while (newPoints.size() > 1000) {
            ArrayList<FlythroughPoint> tmp = new ArrayList<FlythroughPoint>();
            for (int i = 0; i < newPoints.size(); i++) {
                if (i % 2 == 0) {
                    tmp.add(newPoints.get(i));
                }
            }
            newPoints = tmp;
        }

        this.points = new ArrayList<FlythroughPoint>(newPoints);
        setAnimationTimes();
        show();
    }



    /**
     * _more_
     *
     * @param fld _more_
     *
     * @return _more_
     */
    private float parsef(JTextField fld) {
        return (float) parse(fld, 0);
    }

    /**
     * _more_
     *
     * @param fld _more_
     * @param d _more_
     *
     * @return _more_
     */
    private double parse(JTextField fld, double d) {
        String t = fld.getText().trim();
        if (t.length() == 0) {
            return d;
        }
        if (t.equals("-")) {
            return d;
        }
        try {
            return new Double(t).doubleValue();
        } catch (NumberFormatException nfe) {
            animationWidget.setRunning(false);
            viewManager.logException("Parse error:" + t, nfe);
            return d;
        }
    }

    /**
     * _more_
     */
    private void setAnimationTimes() {
        hasTimes = false;
        if (animationWidget == null) {
            return;
        }
        try {
            Set                   set       = null;
            List<FlythroughPoint> thePoints = this.points;
            if ((thePoints != null) && !thePoints.isEmpty()) {
                DateTime[] timeArray = new DateTime[thePoints.size()];
                for (int i = 0; i < thePoints.size(); i++) {
                    DateTime dttm = thePoints.get(i).getDateTime();
                    if (dttm == null) {
                        dttm = new DateTime(new Date(i * 1000 * 60 * 60
                                * 24));
                    } else {
                        hasTimes = true;
                    }
                    timeArray[i] = dttm;
                }
                set = DateTime.makeTimeSet(timeArray);
            }
            if (showTimesCbx != null) {
                showTimesCbx.setEnabled(hasTimes);
            }
            animationWidget.showDateBox(hasTimes);
            animationWidget.setBaseTimes(set);
            if (pointTableModel != null) {
                pointTableModel.fireTableStructureChanged();
            }
        } catch (Exception exc) {
            viewManager.logException("Setting flythrough", exc);
        }
    }



    /**
     * _more_
     */
    public void goToCurrent() {
        if (animation != null) {
            try {
                doStep(animation.getCurrent());
            } catch (Exception exc) {
                viewManager.logException("Setting flythrough", exc);
            }
        }
    }

    /**
     * _more_
     *
     * @param evt _more_
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(Animation.ANI_VALUE)) {
            goToCurrent();
        }
    }

    /** _more_ */
    private static int cnt = 0;


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void makeWidgets() throws Exception {

        boolean doGlobe     = viewManager.getUseGlobeDisplay();

        JSlider speedSlider = new JSlider(1, 200, 200 - animationSpeed);
        speedSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider) e.getSource();
                if ( !slider.getValueIsAdjusting()) {
                    animationSpeed = 200 - slider.getValue() + 1;
                }
            }
        });

        showTimesCbx = new JCheckBox("Set Animation Time", showTimes);


        clipCbx      = GuiUtils.makeCheckbox("Clip Viewpoint", this, "clip");
        showTimesCbx = new JCheckBox("Set Animation Time", showTimes);
        showTimesCbx.setEnabled(hasTimes);
        animateCbx = new JCheckBox("Animated", animate);
        changeViewpointCbx = new JCheckBox("Change Viewpoint",
                                           changeViewpoint);


        orients = new Vector<TwoFacedObject>();
        orients.add(new TwoFacedObject("Towards Point", ORIENT_POINT));
        orients.add(new TwoFacedObject("Up", ORIENT_UP));
        orients.add(new TwoFacedObject("Down", ORIENT_DOWN));
        orients.add(new TwoFacedObject("Left", ORIENT_LEFT));
        orients.add(new TwoFacedObject("Right", ORIENT_RIGHT));
        orientBox = new JComboBox(orients);
        orientBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                orientation =
                    TwoFacedObject.getIdString(orientBox.getSelectedItem());
                goToCurrent();
            }
        });

        showReadoutCbx = new JCheckBox("Show Readout", showReadout);
        readoutLabel =
            GuiUtils.getFixedWidthLabel("<html><br><br><br></html>");
        readoutLabel.setVerticalAlignment(SwingConstants.TOP);

        readoutDisplay = new JPanel();
        readoutDisplay.setLayout(new BorderLayout());



        if (animationInfo == null) {
            animationInfo = new AnimationInfo();
            animationInfo.setShareIndex(true);
        }


        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Misc.run(Flythrough.this, "goToCurrent");
            }
        };


        htmlView = new JEditorPane();
        htmlView.setPreferredSize(new Dimension(300, 400));
        htmlView.setEditable(false);
        htmlView.setContentType("text/html");


        animationWidget = new AnimationWidget(null, null, animationInfo);

        if ((getShareGroup() == null)
                || getShareGroup().equals(SharableManager.GROUP_ALL)) {
            setShareGroup("flythrough");
        }
        animationWidget.setShareGroup(getShareGroup());
        animationWidget.setSharing(getSharing());


        animation = new Animation();
        animation.setAnimationInfo(animationInfo);
        animation.addPropertyChangeListener(this);
        animationWidget.setAnimation(animation);
        locationLine = new LineDrawing("flythroughpoint.line");
        locationMarker =
            new SelectorPoint(
                "flythrough.point",
                ShapeUtility.setSize(
                    ShapeUtility.createShape(ShapeUtility.CROSS)[0],
                    .04f), new RealTuple(
                        RealTupleType.SpatialCartesian3DTuple,
                        new double[] { 0,
                                       0, 0 }));

        locationMarker.setAutoSize(true);
        locationMarker.setManipulable(false);
        locationMarker.setColor(Color.green);

        locationLine.setVisible(false);
        locationMarker.setVisible(false);
        locationLine.setLineWidth(2);
        locationLine.setColor(Color.green);
        viewManager.getMaster().addDisplayable(locationMarker);
        viewManager.getMaster().addDisplayable(locationLine);

        readout = new CursorReadoutWindow(viewManager);


        clipFld = new JTextField("0", 5);
        clipFld.addActionListener(listener);
        for (int ci = 0; ci < cflds.length; ci++) {
            cflds[ci] = new JTextField("0.0");
            cflds[ci].addActionListener(listener);
        }


        zoomFld = new JTextField(zoom + "", 5);

        for (int i = 0; i < tilt.length; i++) {
            final int theIndex = i;
            tiltSliders[i] = new JSlider(-90, 90, (int) tilt[i]);
            tiltSliders[i].setToolTipText("Control-R: reset");
            tiltSliders[i].addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if ((e.getKeyCode() == KeyEvent.VK_R)
                            && e.isControlDown()) {
                        tilt[theIndex] = 0;
                        tiltLabels[theIndex].setText(""
                                + (int) tilt[theIndex]);
                        tiltSliders[theIndex].setValue(0);
                        goToCurrent();
                    }
                }
            });

            tiltLabels[i] = new JLabel("" + (int) tilt[i]);
            tiltSliders[i].addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    JSlider slider = (JSlider) e.getSource();
                    tiltLabels[theIndex].setText("" + slider.getValue());
                    if ( !slider.getValueIsAdjusting()) {
                        tilt[theIndex] = slider.getValue();
                        goToCurrent();
                    }
                }
            });
        }
        /*
        tiltxFld = new JTextField("" + tiltX, 4);
        tiltyFld = new JTextField("" + tiltY, 4);
        tiltzFld = new JTextField("" + tiltZ, 4);
        tiltxFld.addActionListener(listener);
        tiltyFld.addActionListener(listener);
        tiltzFld.addActionListener(listener);
        */



        zoomFld.addActionListener(listener);

        pointTableModel = new AbstractTableModel() {
            public int getRowCount() {
                return points.size();
            }

            public int getColumnCount() {
                return 4;
            }
            public void setValueAt(Object aValue, int rowIndex,
                                   int columnIndex) {
                List<FlythroughPoint> thePoints = points;
                FlythroughPoint       pt        = thePoints.get(rowIndex);
                if (aValue == null) {
                    pt.setDateTime(null);
                } else if (aValue instanceof DateTime) {
                    pt.setDateTime((DateTime) aValue);
                } else {
                    //??
                }
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == COL_DATE;
            }

            public Object getValueAt(int row, int column) {
                List<FlythroughPoint> thePoints = points;
                if (row >= thePoints.size()) {
                    return "n/a";
                }
                FlythroughPoint pt = thePoints.get(row);
                if (column == COL_LAT) {
                    if (pt.getMatrix() != null) {
                        return "matrix";
                    }
                    return pt.getEarthLocation().getLatitude();
                }
                if (column == COL_LON) {
                    if (pt.getMatrix() != null) {
                        return "";
                    }
                    return pt.getEarthLocation().getLongitude();
                }
                if (column == COL_ALT) {
                    if (pt.getMatrix() != null) {
                        return "";
                    }
                    return pt.getEarthLocation().getAltitude();
                }
                if (column == COL_DATE) {
                    return pt.getDateTime();
                }
                return "";
            }

            public String getColumnName(int column) {
                switch (column) {

                  case COL_LAT :
                      return "Latitude";

                  case COL_LON :
                      return "Longitude";

                  case COL_ALT :
                      return "Altitude";

                  case COL_DATE :
                      return "Date/Time";
                }
                return "";
            }
        };

        pointTable = new JTable(pointTableModel);
        pointTable.setToolTipText(
            "Double click: view; Control-P: Show point properties; Delete: delete point");


        pointTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == KeyEvent.VK_P) && e.isControlDown()) {
                    List<FlythroughPoint> newPoints =
                        new ArrayList<FlythroughPoint>();
                    int[]                 rows = pointTable.getSelectedRows();
                    List<FlythroughPoint> oldPoints = points;
                    for (int j = 0; j < rows.length; j++) {
                        FlythroughPoint pt = oldPoints.get(rows[j]);
                        if ( !showProperties(pt)) {
                            break;
                        }
                        pointTable.repaint();
                    }
                }

                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    List<FlythroughPoint> newPoints =
                        new ArrayList<FlythroughPoint>();
                    int[]                 rows = pointTable.getSelectedRows();
                    List<FlythroughPoint> oldPoints = points;
                    for (int i = 0; i < oldPoints.size(); i++) {
                        boolean good = true;
                        for (int j = 0; j < rows.length; j++) {
                            if (i == rows[j]) {
                                good = false;
                                break;
                            }
                        }
                        if (good) {
                            newPoints.add(oldPoints.get(i));
                        }
                    }
                    flythrough(newPoints);
                }
            }
        });

        pointTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                final int row = pointTable.rowAtPoint(e.getPoint());
                if ((row < 0) || (row >= points.size())) {
                    return;
                }
                if (e.getClickCount() > 1) {
                    animation.setCurrent(row);
                }
            }
        });

        fixedZCbx = GuiUtils.makeCheckbox("Use fixed Z", this, "useFixedZ");
        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent orientationComp = GuiUtils.formLayout(new Component[] {
            GuiUtils.filler(), GuiUtils.left(changeViewpointCbx),
            GuiUtils.rLabel("Orientation:"),
            GuiUtils.left(GuiUtils.hbox(orientBox, (doGlobe
                    ? GuiUtils.filler()
                    : (JComponent) fixedZCbx))), GuiUtils.rLabel("Zoom:"),
            GuiUtils.left(zoomFld),
            //            GuiUtils.rLabel("Tilt:"),
            //            GuiUtils.left(GuiUtils.hbox(tiltxFld, tiltyFld, tiltzFld)),

            GuiUtils.rLabel("Tilt Down/Up:"),
            GuiUtils.centerRight(tiltSliders[0], tiltLabels[0]),
            GuiUtils.rLabel("Tilt Left/Right:"),
            GuiUtils.centerRight(tiltSliders[1], tiltLabels[1]),
            //            GuiUtils.rLabel("Tilt Down/Up:"), GuiUtils.centerRight(tiltSliders[2],tiltLabels[2]),

            GuiUtils.rLabel("Transitions:"),
            GuiUtils.vbox(GuiUtils.left(animateCbx),
                          GuiUtils.leftCenter(new JLabel("Speed"),
                              speedSlider)),
            GuiUtils.filler(), GuiUtils.left(showTimesCbx),
            //            GuiUtils.filler(), GuiUtils.left(clipCbx),
            //            GuiUtils.rLabel("Clip Distance:"), GuiUtils.left(clipFld),
            //            GuiUtils.Label("Clip:"), GuiUtils.hbox(cflds), GuiUtils.filler(),
            GuiUtils.filler(),
            GuiUtils.left(GuiUtils.hbox(GuiUtils.makeCheckbox("Show Line",
                this, "showLine"), GuiUtils.makeCheckbox("Show Marker", this,
                    "showMarker")))
        });



        JScrollPane scrollPane = new JScrollPane(pointTable);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        JScrollPane htmlScrollPane = new JScrollPane(htmlView);
        htmlScrollPane.setPreferredSize(new Dimension(400, 300));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("View", GuiUtils.top(orientationComp));
        JTabbedPane readoutTab = new JTabbedPane();
        tabbedPane.addTab("Readout",
                          GuiUtils.topCenter(GuiUtils.left(showReadoutCbx),
                                             readoutTab));
        readoutTab.addTab("Values", readoutLabel);
        readoutTab.addTab("Gauges", readoutDisplay);

        tabbedPane.addTab("Points", scrollPane);
        tabbedPane.addTab("Description", htmlScrollPane);

        JComponent innerContents =
            GuiUtils.topCenter(animationWidget.getContents(), tabbedPane);
        JMenuBar menuBar  = new JMenuBar();
        JMenu    fileMenu = new JMenu("File");
        JMenu editMenu = GuiUtils.makeDynamicMenu("Edit", this,
                             "initEditMenu");
        fileMenu.add(GuiUtils.makeMenuItem("Export", this, "doExport"));
        fileMenu.add(GuiUtils.makeMenuItem("Import", this, "doImport"));




        menuBar.add(fileMenu);
        menuBar.add(editMenu);


        innerContents = GuiUtils.inset(innerContents, 5);
        contents      = GuiUtils.topCenter(menuBar, innerContents);

        animation.setCurrent(currentIndex);


    }


    /**
     * _more_
     *
     * @param editMenu _more_
     */
    public void initEditMenu(JMenu editMenu) {
        editMenu.add(GuiUtils.makeMenuItem("Add Point", this,
                                           "addPointWithoutTime"));
        editMenu.add(GuiUtils.makeMenuItem("Add Point with Time", this,
                                           "addPointWithTime"));

        editMenu.add(GuiUtils.makeCheckboxMenuItem("Sharing On", this,
                "sharing", null));

        editMenu.add(GuiUtils.makeMenuItem("Set Share Group", this,
                                           "showSharableDialog"));

    }


    /**
     * _more_
     *
     * @param sharing _more_
     */
    public void setSharing(boolean sharing) {
        super.setSharing(sharing);
        if (animationWidget != null) {
            animationWidget.setSharing(sharing);
            animationInfo.setShared(true);
        }
    }

    /**
     * _more_
     *
     * @param shareGroup _more_
     */
    public void setShareGroup(Object shareGroup) {
        super.setShareGroup(shareGroup);
        if (animationWidget != null) {
            animationWidget.setShareGroup(shareGroup);
        }
    }



    /**
     * _more_
     *
     * @param pt _more_
     *
     * @return _more_
     */
    private boolean showProperties(FlythroughPoint pt) {
        try {
            DateTime[] times     =
                viewManager.getAnimationWidget().getTimes();
            JComboBox  timeBox   = null;
            JLabel     timeLabel = GuiUtils.rLabel("Time:");
            Vector     timesList = new Vector();
            timesList.add(0, new TwoFacedObject("None", null));
            if ((times != null) && (times.length > 0)) {
                timesList.addAll(Misc.toList(times));
            }
            if ((pt.getDateTime() != null)
                    && !timesList.contains(pt.getDateTime())) {
                timesList.add(pt.getDateTime());
            }
            timeBox = new JComboBox(timesList);
            if (pt.getDateTime() != null) {
                timeBox.setSelectedItem(pt.getDateTime());
            }

            LatLonWidget llw = new LatLonWidget("Latitude: ", "Longitude: ",
                                   "Altitude: ", null) {
                protected String formatLatLonString(String latOrLon) {
                    return latOrLon;
                }
            };

            EarthLocation el = pt.getEarthLocation();
            llw.setLatLon(el.getLatitude().getValue(CommonUnit.degree),
                          el.getLongitude().getValue(CommonUnit.degree));
            llw.setAlt(el.getAltitude().getValue(CommonUnit.meter));

            JTextArea textArea = new JTextArea("", 5, 100);
            if (pt.getDescription() != null) {
                textArea.setText(pt.getDescription());
            }
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));
            JComponent contents = GuiUtils.formLayout(new Component[] {
                GuiUtils.rLabel("Location:"), llw, timeLabel,
                GuiUtils.left(timeBox), GuiUtils.rLabel("Description:"),
                scrollPane
            });
            if ( !GuiUtils.showOkCancelDialog(frame, "Point Properties",
                    contents, null)) {
                return false;
            }
            pt.setDescription(textArea.getText());
            pt.setEarthLocation(makePoint(llw.getLat(), llw.getLon(),
                                          llw.getAlt()));
            Object selectedDate = timeBox.getSelectedItem();
            if (selectedDate instanceof TwoFacedObject) {
                pt.setDateTime(null);
            } else {
                pt.setDateTime((DateTime) selectedDate);
            }


            return true;
        } catch (Exception exc) {
            viewManager.logException("Showing point properties", exc);
            return false;
        }

    }


    /**
     * _more_
     *
     *
     * @param force _more_
     * @throws Exception _more_
     */
    private synchronized void doMakeContents(boolean force) throws Exception {

        if ( !force && (contents != null)) {
            return;
        }

        if (contents == null) {
            makeWidgets();
        }

        animation.setAnimationInfo(animationInfo);
        showTimesCbx.setSelected(showTimes);
        showTimesCbx.setEnabled(hasTimes);
        animateCbx.setSelected(animate);
        changeViewpointCbx.setSelected(changeViewpoint);

        orientBox.setSelectedItem(TwoFacedObject.findId(orientation,
                orients));
        showReadoutCbx.setSelected(showReadout);
        zoomFld.setText(zoom + "");
        for (int i = 0; i < tilt.length; i++) {
            tiltSliders[i].setValue((int) tilt[i]);
            tiltLabels[i].setText("" + (int) tilt[i]);
        }
        //        tiltxFld.setText("" + tiltX);
        //        tiltyFld.setText("" + tiltY);
        //        tiltzFld.setText("" + tiltZ);



        setAnimationTimes();
        boolean hadFrame = true;
        if (frame == null) {
            frame = new JFrame(GuiUtils.getApplicationTitle() + "Flythrough");
            frame.setIconImage(
                GuiUtils.getImage("/auxdata/ui/icons/plane.png"));
            hadFrame = false;
        }
        frame.getContentPane().removeAll();
        frame.getContentPane().add(contents);
        frame.pack();
        if ( !hadFrame) {
            frame.setLocation(400, 400);
        }
        GuiUtils.toFront(frame);

    }




    /**
     * _more_
     *
     * @param latitude _more_
     * @param longitude _more_
     * @param alt _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private EarthLocation makePoint(String latitude, String longitude,
                                    String alt)
            throws VisADException, RemoteException {
        return makePoint(((latitude == null)
                          ? 0
                          : new Double(
                              latitude.trim()).doubleValue()), ((longitude
                                  == null)
                ? 0
                : new Double(longitude.trim()).doubleValue()), ((alt == null)
                ? 0
                : new Double(alt.trim()).doubleValue()));
    }



    /**
     * _more_
     *
     * @param latitude _more_
     * @param longitude _more_
     * @param alt _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private EarthLocation makePoint(double latitude, double longitude,
                                    double alt)
            throws VisADException, RemoteException {
        Real altReal = new Real(RealType.Altitude, alt);
        return new EarthLocationLite(new Real(RealType.Latitude, latitude),
                                     new Real(RealType.Longitude, longitude),
                                     altReal);
    }


    /**
     * _more_
     *
     * @param root _more_
     */
    private void importKml(Element root) {
        try {
            List tourNodes = XmlUtil.findDescendants(root, KmlUtil.TAG_TOUR);
            if (tourNodes.size() == 0) {
                LogUtil.userMessage("Could not find any tours");
                return;
            }
            Element               tourNode  = (Element) tourNodes.get(0);
            List<FlythroughPoint> thePoints =
                new ArrayList<FlythroughPoint>();
            Element playListNode = XmlUtil.findChild(tourNode,
                                       KmlUtil.TAG_PLAYLIST);
            if (playListNode == null) {
                LogUtil.userMessage("Could not find playlist");
                return;
            }

            NodeList elements = XmlUtil.getElements(playListNode);
            for (int i = 0; i < elements.getLength(); i++) {
                Element child = (Element) elements.item(i);
                if (child.getTagName().equals(KmlUtil.TAG_FLYTO)) {
                    Element cameraNode = XmlUtil.findChild(child,
                                             KmlUtil.TAG_CAMERA);
                    /*        <Camera>
          <longitude>170.157</longitude>
          <latitude>-43.671</latitude>
          <altitude>9700</altitude>
          <heading>-6.333</heading>
          <tilt>33.5</tilt>
          </Camera>*/
                    if (cameraNode == null) {
                        cameraNode = XmlUtil.findChild(child,
                                KmlUtil.TAG_LOOKAT);
                    }

                    if (cameraNode == null) {
                        //                        System.err.println ("no camera:" + XmlUtil.toString(child));
                        continue;
                    }
                    FlythroughPoint pt =
                        new FlythroughPoint(makePoint(XmlUtil
                            .getGrandChildText(cameraNode,
                                KmlUtil.TAG_LATITUDE), XmlUtil
                                    .getGrandChildText(cameraNode,
                                        KmlUtil.TAG_LONGITUDE), XmlUtil
                                            .getGrandChildText(cameraNode,
                                                KmlUtil.TAG_ALTITUDE)));

                    pt.setTiltX(
                        -new Double(
                            XmlUtil.getGrandChildText(
                                cameraNode, KmlUtil.TAG_TILT,
                                "0")).doubleValue());
                    pt.setTiltY(
                        new Double(
                            XmlUtil.getGrandChildText(
                                cameraNode, KmlUtil.TAG_HEADING,
                                "0")).doubleValue());
                    pt.setTiltZ(
                        new Double(
                            XmlUtil.getGrandChildText(
                                cameraNode, KmlUtil.TAG_ROLL,
                                "0")).doubleValue());

                    thePoints.add(pt);

                } else if (child.getTagName().equals(KmlUtil.TAG_WAIT)) {}
                else {}

            }


            this.points = thePoints;
            doMakeContents(true);
            setAnimationTimes();
        } catch (Exception exc) {
            viewManager.logException("Importing kml", exc);
        }
    }

    /**
     * _more_
     */
    public void doImport() {
        try {
            String filename = FileManager.getReadFile(FileManager.FILTER_XML);
            if (filename == null) {
                return;
            }
            Element root = XmlUtil.getRoot(filename, getClass());
            if (root.getTagName().equals(KmlUtil.TAG_KML)) {
                importKml(root);
                return;
            }


            if ( !root.getTagName().equals(TAG_FLYTHROUGH)) {
                throw new IllegalStateException("Unknown tag:"
                        + root.getTagName());
            }
            for (int i = 0; i < tilt.length; i++) {
                tilt[i] = XmlUtil.getAttribute(root, ATTR_TILT[i], tilt[i]);
            }
            zoom = XmlUtil.getAttribute(root, ATTR_ZOOM, zoom);

            List<FlythroughPoint> thePoints =
                new ArrayList<FlythroughPoint>();
            NodeList elements = XmlUtil.getElements(root);
            for (int i = 0; i < elements.getLength(); i++) {
                Element child = (Element) elements.item(i);

                if ( !child.getTagName().equals(TAG_POINT)) {
                    throw new IllegalStateException("Unknown tag:"
                            + child.getTagName());
                }
                FlythroughPoint pt = new FlythroughPoint();
                pt.setDescription(XmlUtil.getGrandChildText(child,
                        TAG_DESCRIPTION));

                pt.setEarthLocation(makePoint(XmlUtil.getAttribute(child,
                        ATTR_LAT, 0.0), XmlUtil.getAttribute(child, ATTR_LON,
                            0.0), XmlUtil.getAttribute(child, ATTR_ALT,
                                0.0)));

                if (XmlUtil.hasAttribute(child, ATTR_DATE)) {
                    pt.setDateTime(parseDate(XmlUtil.getAttribute(child,
                            ATTR_DATE)));
                }
                pt.setTiltX(XmlUtil.getAttribute(child, ATTR_TILT[0],
                        Double.NaN));
                pt.setTiltY(XmlUtil.getAttribute(child, ATTR_TILT[1],
                        Double.NaN));
                pt.setTiltZ(XmlUtil.getAttribute(child, ATTR_TILT[2],
                        Double.NaN));
                pt.setZoom(XmlUtil.getAttribute(child, ATTR_ZOOM,
                        Double.NaN));
                String matrixS = XmlUtil.getAttribute(child, ATTR_MATRIX,
                                     (String) null);
                if (matrixS != null) {
                    List<String> toks =
                        (List<String>) StringUtil.split(matrixS, ",", true,
                            true);
                    double[] m = new double[toks.size()];
                    for (int tokIdx = 0; tokIdx < m.length; tokIdx++) {
                        m[tokIdx] =
                            new Double(toks.get(tokIdx)).doubleValue();
                    }
                    pt.setMatrix(m);
                }
                thePoints.add(pt);
            }
            this.points = thePoints;
            doMakeContents(true);
            setAnimationTimes();
        } catch (Exception exc) {
            viewManager.logException("Initializing flythrough", exc);
        }
    }

    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String formatDate(DateTime dttm) throws Exception {
        return sdf.format(ucar.visad.Util.makeDate(dttm));
    }


    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private DateTime parseDate(String dttm) throws Exception {
        return new DateTime(sdf.parse(dttm));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public FlythroughPoint addPointWithoutTime() {
        return addPoint(false);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public FlythroughPoint addPointWithTime() {
        return addPoint(true);
    }


    /**
     * _more_
     *
     * @param includeTime _more_
     *
     * @return _more_
     */
    public FlythroughPoint addPoint(boolean includeTime) {
        try {
            FlythroughPoint pt = new FlythroughPoint();
            pt.setEarthLocation(makePoint(0, 0, 0));
            NavigatedDisplay navDisplay    =
                viewManager.getNavigatedDisplay();
            double[]         currentMatrix = navDisplay.getProjectionMatrix();
            if (includeTime) {
                Real dttm =
                    viewManager.getAnimation().getCurrentAnimationValue();
                if (dttm != null) {
                    pt.setDateTime(new DateTime(dttm));
                }
            }
            pt.setMatrix(currentMatrix);
            points.add(pt);
            flythrough(points);
            return pt;
        } catch (Exception exc) {
            viewManager.logException("Adding point", exc);
            return null;
        }
    }

    /**
     * _more_
     */
    public void doExport() {
        try {
            String filename =
                FileManager.getWriteFile(FileManager.FILTER_XML,
                                         FileManager.SUFFIX_XML);
            if (filename == null) {
                return;
            }

            Document doc  = XmlUtil.makeDocument();
            Element  root = doc.createElement(TAG_FLYTHROUGH);
            for (int i = 0; i < tilt.length; i++) {
                root.setAttribute(ATTR_TILT[i], "" + tilt[i]);
            }
            root.setAttribute(ATTR_ZOOM, "" + getZoom());

            List<FlythroughPoint> thePoints = this.points;
            for (FlythroughPoint pt : thePoints) {
                Element ptNode = XmlUtil.create(TAG_POINT, root);

                if (pt.getDescription() != null) {
                    XmlUtil.create(root.getOwnerDocument(), TAG_DESCRIPTION,
                                   ptNode, pt.getDescription());
                }

                EarthLocation el = pt.getEarthLocation();
                if (pt.getDateTime() != null) {
                    ptNode.setAttribute(ATTR_DATE,
                                        formatDate(pt.getDateTime()));
                }
                ptNode.setAttribute(
                    ATTR_LAT,
                    "" + el.getLatitude().getValue(CommonUnit.degree));
                ptNode.setAttribute(
                    ATTR_LON,
                    "" + el.getLongitude().getValue(CommonUnit.degree));
                ptNode.setAttribute(
                    ATTR_ALT,
                    "" + el.getAltitude().getValue(CommonUnit.meter));
                if (pt.hasTiltX()) {
                    ptNode.setAttribute(ATTR_TILT[0], "" + pt.getTiltX());
                }
                if (pt.hasTiltY()) {
                    ptNode.setAttribute(ATTR_TILT[1], "" + pt.getTiltY());
                }
                if (pt.hasTiltZ()) {
                    ptNode.setAttribute(ATTR_TILT[2], "" + pt.getTiltZ());
                }
                if (pt.hasZoom()) {
                    ptNode.setAttribute(ATTR_ZOOM, "" + pt.getZoom());
                }

                double[] m = pt.getMatrix();
                if (m != null) {
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < m.length; i++) {
                        if (i > 0) {
                            sb.append(",");
                        }
                        sb.append(m[i]);
                    }
                    ptNode.setAttribute(ATTR_MATRIX, sb.toString());
                }

            }
            String xml = XmlUtil.toString(root);
            IOUtil.writeFile(filename, xml);
        } catch (Exception exc) {
            viewManager.logException("Exporting flythrough", exc);
        }

    }

    /**
     * _more_
     */
    public void show() {
        if (frame == null) {
            try {
                doMakeContents(false);
            } catch (Exception exc) {
                viewManager.logException("Showing flythrough", exc);
            }
        }
        setAnimationTimes();
        if (frame != null) {
            frame.show();
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isActive() {
        return viewManager != null;
    }


    /**
     * fly to the given point
     *
     * @param index _more_
     *
     * @throws Exception _more_
     */
    private void doStep(int index) throws Exception {

        if ((points.size() == 0) || !isActive()) {
            return;
        }



        MapViewManager viewManager = this.viewManager;
        if (viewManager == null) {
            return;
        }
        boolean               doGlobe       =
            viewManager.getUseGlobeDisplay();
        List<FlythroughPoint> thePoints     = this.points;
        NavigatedDisplay      navDisplay = viewManager.getNavigatedDisplay();
        MouseBehavior         mouseBehavior = navDisplay.getMouseBehavior();
        double[]              currentMatrix =
            navDisplay.getProjectionMatrix();
        double[]              aspect        = navDisplay.getDisplayAspect();
        double[]              xyz1          = { 0, 0, 0 };
        double[]              xyz2          = { 0, 0, 0 };

        Vector3d              upVector;

        if (index >= thePoints.size()) {
            index = 0;
        } else if (index < 0) {
            index = thePoints.size() - 1;
        }
        if (pointTable != null) {
            pointTable.getSelectionModel().setSelectionInterval(index, index);
            pointTable.repaint();
        }

        int     index1 = index;
        int     index2 = index + 1;
        boolean atEnd  = false;
        if (index2 >= thePoints.size()) {
            index2 = 0;
            atEnd  = true;
        }


        try {
            FlythroughPoint pt1 = thePoints.get(index1);
            FlythroughPoint pt2 = thePoints.get(index2);

            if (htmlView != null) {
                if (pt1.getDescription() != null) {
                    htmlView.setText(pt1.getDescription());
                } else {
                    htmlView.setText("");
                }
            }


            xyz1 = navDisplay.getSpatialCoordinates(pt1.getEarthLocation(),
                    xyz1);
            xyz2 = navDisplay.getSpatialCoordinates(pt2.getEarthLocation(),
                    xyz2);
            processReadout(pt1);




            float x1 = (float) xyz1[0];
            float y1 = (float) xyz1[1];
            float z1 = (float) xyz1[2];

            if (atEnd && (thePoints.size() > 1) && (index1 > 0)) {
                FlythroughPoint prevPt = thePoints.get(index1 - 1);

                double[] xyz3 = navDisplay.getSpatialCoordinates(
                                    prevPt.getEarthLocation(), null);
                xyz2[0] = x1 + (x1 - xyz3[0]);
                xyz2[1] = y1 + (y1 - xyz3[1]);
                xyz2[2] = z1 + (z1 - xyz3[2]);
            }


            float  x2   = (float) xyz2[0];
            float  y2   = (float) xyz2[1];
            float  z2   = (float) xyz2[2];

            double zoom = (pt1.hasZoom()
                           ? pt1.getZoom()
                           : getZoom());
            if (zoom == 0) {
                zoom = 0.1;
            }

            double tiltx = (pt1.hasTiltX()
                            ? pt1.getTiltX()
                            : tilt[0]);
            double tilty = (pt1.hasTiltY()
                            ? pt1.getTiltY()
                            : tilt[1]);
            double tiltz = (pt1.hasTiltZ()
                            ? pt1.getTiltZ()
                            : tilt[2]);



            //Check for nans
            if ((x2 != x2) || (y2 != y2) || (z2 != z2)) {
                return;
            }
            if ((x1 != x1) || (y1 != y1) || (z1 != z1)) {
                return;
            }

            double[] m = pt1.getMatrix();
            if (m == null) {
                m = new double[16];

                Transform3D t = new Transform3D();
                if (orientation.equals(ORIENT_UP)) {
                    y2 = y1 + 100;
                    x2 = x1;
                } else if (orientation.equals(ORIENT_DOWN)) {
                    y2 = y1 - 100;
                    x2 = x1;
                } else if (orientation.equals(ORIENT_LEFT)) {
                    x2 = x1 - 100;
                    y2 = y1;
                } else if (orientation.equals(ORIENT_RIGHT)) {
                    x2 = x1 + 100;
                    y2 = y1;
                }

                if ((x1 == x2) && (y1 == y2) && (z1 == z2)) {
                    return;
                }

                if (doGlobe) {
                    upVector = new Vector3d(x1, y1, z1);
                } else {
                    upVector = new Vector3d(0, 0, 1);
                }

                //Keep flat in z for non globe
                t.lookAt(new Point3d(x1, y1, z1),
                         new Point3d(x2, y2, (( !getUseFixedZ() || doGlobe)
                        ? z2
                        : z1)), upVector);
                t.get(m);

                double[] tiltMatrix = mouseBehavior.make_matrix(tiltx, tilty,
                                          tiltz, 1.0, 1.0, 1.0, 0.0, 0.0,
                                          0.0);
                m = mouseBehavior.multiply_matrix(tiltMatrix, m);
                if (aspect != null) {
                    //                double[] aspectMatrix = mouseBehavior.make_matrix(0.0, 0.0,
                    //                                                             0.0, aspect[0],aspect[1], aspect[2], 0.0, 0.0,0.0);                    
                    //                m = mouseBehavior.multiply_matrix(aspectMatrix, m);
                }

                double[] scaleMatrix = mouseBehavior.make_matrix(0.0, 0.0,
                                           0.0, zoom, 0.0, 0.0, 0.0);

                m = mouseBehavior.multiply_matrix(scaleMatrix, m);
            }

            currentPoint = pt1;

            if (doGlobe) {
                setPts(locationLine, 0, x1 * 2, 0, y1 * 2, 0, z1 * 2);
            } else {
                setPts(locationLine, x1, x1, y1, y1, 1, -1);
            }
            locationMarker.setPoint(
                new RealTuple(
                    RealTupleType.SpatialCartesian3DTuple, new double[] { x1,
                    y1, z1 }));

            locationLine.setVisible(showLine);
            locationMarker.setVisible(showMarker);



            DisplayRendererJ3D dr =
                (DisplayRendererJ3D) navDisplay.getDisplay()
                    .getDisplayRenderer();
            /*
            dr.setClip(0, true,
                       parsef(cflds[0]),
                       parsef(cflds[1]),
                       parsef(cflds[2]),
                       parsef(cflds[3]));*/


            /*            Transform3D t3 = new Transform3D();
            t3.rotX(Math.toRadians(90));
            Point3d toPt  = new Point3d(x2,y2,z2);
            t3.transform(toPt);*/

            float  coeff = 1;
            double value = y1;
            dr.setClip(0, getClip(), 0.f, (float) coeff, 0.f,
                       (float) ((-coeff * (value + coeff * 0.01f))));



            if (false && (navDisplay instanceof MapProjectionDisplayJ3D)) {
                double clipDistance = parse(clipFld, 0.0);
                System.err.println("Clip distance:" + clipDistance);
                ((MapProjectionDisplayJ3D) navDisplay).getView()
                    .setFrontClipDistance(clipDistance);
            }

            //            System.err.println("x/y:" + x1 +"/" + y1 + "  " + x2 +"/" + y2);

            if (hasTimes && getShowTimes()) {
                DateTime dttm = pt1.getDateTime();
                if (dttm != null) {
                    viewManager.getAnimationWidget().setTimeFromUser(dttm);
                }

            }


            if (changeViewpointCbx.isSelected()) {
                if (getAnimate()) {
                    navDisplay.animateMatrix(m, animationSpeed);
                } else {
                    navDisplay.setProjectionMatrix(m);
                }
            }



        } catch (NumberFormatException exc) {
            viewManager.logException("Error parsing number:" + exc, exc);
        } catch (javax.media.j3d.BadTransformException bte) {
            try {
                navDisplay.setProjectionMatrix(currentMatrix);
            } catch (Exception ignore) {}
        } catch (Exception exc) {
            viewManager.logException("Error", exc);
            animationWidget.setRunning(false);
            return;
        }

    }


    /**
     * _more_
     */
    protected void animationTimeChanged() {
        FlythroughPoint pt1 = getCurrentPoint();
        if (pt1 != null) {
            try {
                processReadout(pt1);
            } catch (Exception exc) {
                viewManager.logException("Setting readout", exc);
            }
        }

    }


    /**
     * _more_
     *
     * @param pt1 _more_
     *
     * @throws Exception _more_
     */
    protected void processReadout(FlythroughPoint pt1) throws Exception {
        List<ReadoutInfo> samples = new ArrayList<ReadoutInfo>();
        readoutLabel.setText(readout.getReadout(pt1.getEarthLocation(),
                showReadoutCbx.isSelected(), true, samples));

        if ( !showReadoutCbx.isSelected()) {
            return;
        }


        List comps  = new ArrayList();
        List labels = new ArrayList();
        for (ReadoutInfo info : samples) {
            Real r = info.getReal();
            if (r == null) {
                continue;
            }
            String name = ucar.visad.Util.cleanTypeName(r.getType());
            if (name.toLowerCase().indexOf("precipitation") >= 0) {
                double v = r.getValue();
                String html;
                if (r.isMissing() || (v < 0.01)) {
                    html = "<html><img  src=idvresource:/ucar/unidata/idv/control/images/sunny.png></html>";
                } else {
                    html = "<html><img  src=idvresource:/ucar/unidata/idv/control/images/rainy.png></html>";
                }
                String labelString = name.replace("_", " ") + "<br>"
                                     + Misc.format(v);
                if (r.getUnit() != null) {
                    labelString += " [" + r.getUnit() + "]";
                }
                labels.add(new JLabel("<html>" + labelString));
                comps.add(new JLabel(html));
            } else if (name.toLowerCase().indexOf("temperature") >= 0) {
                if (r.isMissing()) {
                    continue;
                }
                double v =
                    r.getValue(ucar.visad.quantities.CommonUnits.CELSIUS);
                labels.add(new JLabel("<html>" + name.replace("_", " ")
                                      + "<br>" + Misc.format(v) + " [C]"));
                DefaultValueDataset dataset =
                    new DefaultValueDataset(new Double(v));
                ThermometerPlot plot = new ThermometerPlot(dataset);
                JFreeChart chart = new JFreeChart("",
                                       JFreeChart.DEFAULT_TITLE_FONT, plot,
                                       false);
                ChartPanel chartPanel = new ChartPanel(chart);
                //                chartPanel.setPreferredSize(new Dimension(100,300));
                comps.add(chartPanel);
            }
        }

        List allComps = new ArrayList(labels);
        allComps.addAll(comps);
        readoutDisplay.removeAll();
        readoutDisplay.add("Center",
                           GuiUtils.doLayout(allComps, labels.size(),
                                             GuiUtils.WT_Y, GuiUtils.WT_NY));

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public FlythroughPoint getCurrentPoint() {
        return currentPoint;
    }


    /**
     *  Set the Points property.
     *
     *  @param value The new value for Points
     */
    public void setPoints(List<FlythroughPoint> value) {
        this.points = value;
    }

    /**
     *  Get the Points property.
     *
     *  @return The Points
     */
    public List<FlythroughPoint> getPoints() {
        return this.points;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasPoints() {
        return points.size() > 0;
    }


    /**
     *  Set the Tilt property.
     *
     *  @param value The new value for Tilt
     */
    public void setTiltX(double value) {}

    /**
     *  Set the Tilt property.
     *
     *  @param value The new value for Tilt
     */
    public void setTiltY(double value) {}



    /**
     *  Set the Tilt propertz.
     *
     *  @param value The new value for Tilt
     */
    public void setTiltZ(double value) {}




    /**
     * Set the Tilt property.
     *
     * @param value The new value for Tilt
     */
    public void setTilt(double[] value) {
        this.tilt = value;
    }

    /**
     * Get the Tilt property.
     *
     * @return The Tilt
     */
    public double[] getTilt() {
        return this.tilt;
    }



    /**
     *  Set the Zoom property.
     *
     *  @param value The new value for Zoom
     */
    public void setZoom(double value) {
        this.zoom = value;
    }

    /**
     *  Get the Zoom property.
     *
     *  @return The Zoom
     */
    public double getZoom() {
        if (zoomFld != null) {
            this.zoom = parse(zoomFld, zoom);
        }
        return this.zoom;
    }



    /**
     *  Set the ViewManager property.
     *
     *  @param value The new value for ViewManager
     */
    public void setViewManager(MapViewManager value) {
        this.viewManager = value;
    }


    /**
     *  Set the ChangeViewpoint property.
     *
     *  @param value The new value for ChangeViewpoint
     */
    public void setChangeViewpoint(boolean value) {
        this.changeViewpoint = value;
    }

    /**
     *  Get the ChangeViewpoint property.
     *
     *  @return The ChangeViewpoint
     */
    public boolean getChangeViewpoint() {
        if (changeViewpointCbx != null) {
            return changeViewpointCbx.isSelected();
        }
        return this.changeViewpoint;
    }

    /**
     *  Set the ShowReadout property.
     *
     *  @param value The new value for ShowReadout
     */
    public void setShowReadout(boolean value) {
        this.showReadout = value;
    }

    /**
     *  Get the ShowReadout property.
     *
     *  @return The ShowReadout
     */
    public boolean getShowReadout() {
        if (showReadoutCbx != null) {
            return showReadoutCbx.isSelected();
        }
        return this.showReadout;
    }


    /**
     * Set the ShowTimes property.
     *
     * @param value The new value for ShowTimes
     */
    public void setShowTimes(boolean value) {
        showTimes = value;
    }

    /**
     * Get the ShowTimes property.
     *
     * @return The ShowTimes
     */
    public boolean getShowTimes() {
        if (showTimesCbx != null) {
            showTimes = showTimesCbx.isSelected();
        }
        return showTimes;
    }


    /**
     *  Set the ShowLine property.
     *
     *  @param value The new value for ShowLine
     */
    public void setShowLine(boolean value) {
        showLine = value;
        if (locationLine != null) {
            try {
                locationLine.setVisible(value);
            } catch (Exception ignore) {}
        }
    }

    /**
     * Get the ShowLine property.
     *
     * @return The ShowLine
     */
    public boolean getShowLine() {
        return showLine;
    }



    /**
     *  Set the ShowMarker property.
     *
     *  @param value The new value for ShowMarker
     */
    public void setShowMarker(boolean value) {
        showMarker = value;
        if (locationMarker != null) {
            try {
                locationMarker.setVisible(value);
            } catch (Exception ignore) {}
        }
    }

    /**
     * Get the ShowMarker property.
     *
     * @return The ShowMarker
     */
    public boolean getShowMarker() {
        return showMarker;
    }


    /**
     * Set the Animate property.
     *
     * @param value The new value for Animate
     */
    public void setAnimate(boolean value) {
        animate = value;
    }

    /**
     * Get the Animate property.
     *
     * @return The Animate
     */
    public boolean getAnimate() {
        if (animateCbx != null) {
            animate = animateCbx.isSelected();
        }
        return animate;
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void setRelativeOrientation(boolean value) {}

    /**
     * Set the Orientation property.
     *
     * @param value The new value for Orientation
     */
    public void setOrientation(String value) {
        this.orientation = value;
    }

    /**
     * Get the Orientation property.
     *
     * @return The Orientation
     */
    public String getOrientation() {
        return this.orientation;
    }


    /**
     * Set the Shown property.
     *
     * @param value The new value for Shown
     */
    public void setShown(boolean value) {
        this.shown = value;
    }

    /**
     * Get the Shown property.
     *
     * @return The Shown
     */
    public boolean getShown() {
        if (frame != null) {
            return frame.isShowing();
        }
        return this.shown;
    }

    /**
     *  Set the Clip property.
     *
     *  @param value The new value for Clip
     *
     * @throws Exception _more_
     */
    public void setClip(boolean value) throws Exception {
        if (clip != value) {
            clip = value;
            if (isActive() && (animationWidget != null)) {
                doStep(animation.getCurrent());
            }
        }
    }

    /**
     *  Get the Clip property.
     *
     *  @return The Clip
     */
    public boolean getClip() {
        if (clipCbx != null) {
            clip = clipCbx.isSelected();
        }
        return clip;
    }



    /**
     *  Set the UseFixedZ property.
     *
     *  @param value The new value for UseFixedZ
     */
    public void setUseFixedZ(boolean value) {
        useFixedZ = value;
    }

    /**
     *  Get the UseFixedZ property.
     *
     *  @return The UseFixedZ
     */
    public boolean getUseFixedZ() {
        return useFixedZ;
    }


    /**
     *  Set the CurrentIndex property.
     *
     *  @param value The new value for CurrentIndex
     */
    public void setCurrentIndex(int value) {
        currentIndex = value;
    }

    /**
     *  Get the CurrentIndex property.
     *
     *  @return The CurrentIndex
     */
    public int getCurrentIndex() {
        if (animation != null) {
            try {
                currentIndex = animation.getCurrent();
            } catch (Exception ignore) {}
        }
        return currentIndex;
    }

    /**
     *  Set the AnimateSpeed property.
     *
     *  @param value The new value for AnimateSpeed
     */
    public void setAnimateSpeed(long value) {}



    /**
     * Set the AnimationSpeed property.
     *
     * @param value The new value for AnimationSpeed
     */
    public void setAnimationSpeed(int value) {
        this.animationSpeed = value;
    }

    /**
     * Get the AnimationSpeed property.
     *
     * @return The AnimationSpeed
     */
    public int getAnimationSpeed() {
        return this.animationSpeed;
    }





}

