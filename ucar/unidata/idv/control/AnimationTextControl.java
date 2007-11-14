/*
 * $Id: AnimationTextControl.java,v 1.87 2006/12/01 20:16:36 jeffmc Exp $
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


package ucar.unidata.idv.control;



import ucar.unidata.data.DataChoice;
import ucar.unidata.util.DatedObject;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.visad.Util;
import ucar.visad.display.*;

import visad.*;



import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;


/**
 *
 * @author Jeff McWhirter
 * @version  $Revision: 1.87 $
 */
public class AnimationTextControl extends DisplayControlImpl implements HyperlinkListener {

    /** _more_          */
    private DateTime currentTime;

    /** _more_          */
    private List textList = new ArrayList();

    /** _more_          */
    private JEditorPane editor;

    /** _more_          */
    private JTextField subjectFld;

    /** _more_          */
    private JComboBox timeBox;

    /** _more_          */
    private boolean ignoreBoxEvents = false;

    /** _more_          */
    private JCheckBox listenCbx;

    /** _more_          */
    private boolean listenOnAnimation = true;

    /**
     * Default Constructor. Set the flags to tell that this display
     * control wants a color widget.
     */
    public AnimationTextControl() {}


    /**
     * Called to make this kind of Display Control; also calls code to
     * made its Displayable, the line.
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment -
     *                   not used yet; can be null.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        CompositeDisplayable holder = new CompositeDisplayable();
        addDisplayable(holder);
        return true;
    }

    /**
     * _more_
     */
    public void initDone() {
        super.initDone();
        try {
            Animation animation = getAnimation();
            if (animation != null) {
                setTime(animation.getAniValue());
            }
        } catch (Exception exc) {
            logException("initDone", exc);
        }

    }

    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    private DatedText findDatedText(DateTime dttm) throws VisADException {
        if (dttm == null) {
            return null;
        }
        Date date = Util.makeDate(dttm);
        for (int i = 0; i < textList.size(); i++) {
            DatedText datedObject = (DatedText) textList.get(i);
            if (datedObject.getDate().equals(date)) {
                return datedObject;
            }
        }
        return null;
    }


    /**
     * _more_
     *
     * @param dt _more_
     */
    private void setDatedText(DatedText dt) {
        try {
            ignoreBoxEvents = true;
            DatedText oldDT = findDatedText(currentTime);
            if (oldDT != null) {
                oldDT.setText(editor.getText());
                oldDT.setSubject(subjectFld.getText());
            }

            if (dt == null) {
                editor.setText("");
                subjectFld.setText("");
                timeBox.setSelectedItem("none");
                currentTime = null;
            } else {
                editor.setText(dt.getText());
                subjectFld.setText(dt.getSubject());
                timeBox.setSelectedItem(dt);
                try {
                    currentTime = dt.getDateTime();
                } catch (Exception exc) {
                    logException("Time changed", exc);
                }
            }
        } catch (Exception exc) {
            logException("Changing times", exc);
        }
        ignoreBoxEvents = false;
    }


    /**
     * _more_
     *
     * @param time _more_
     */
    private void setTime(Real time) {
        try {
            if (Misc.equals(time, currentTime)) {
                return;
            }
            if ((time == null)
                    || ((time != null) && !(time instanceof DateTime))) {
                setDatedText(null);
                return;
            }
            DatedText newDT = findDatedText((DateTime) time);
            if (newDT == null) {
                newDT = new DatedText(Util.makeDate((DateTime) time));
                textList.add(newDT);
                textList        = DatedObject.sort(textList, true);
                ignoreBoxEvents = true;
                GuiUtils.setListData(timeBox, textList);
                ignoreBoxEvents = false;
            }
            setDatedText(newDT);
        } catch (Exception exc) {
            logException("Time changed", exc);
        }
    }

    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        if (getListenOnAnimation()) {
            setTime(time);
        }
        super.timeChanged(time);
    }


    /**
     * _more_
     *
     * @param e _more_
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {}

    /**
     * Make the UI contents for this control.
     *
     * @return  UI container
     */
    public Container doMakeContents() {
        subjectFld = new JTextField();
        editor     = new JEditorPane();
        editor.setEditable(true);
        editor.addHyperlinkListener(this);
        editor.setContentType("text/html");
        listenCbx = new JCheckBox("Synchronize with animation",
                                  listenOnAnimation);
        listenCbx.setToolTipText(
            "Change text display when animation changes");
        JScrollPane scroller = GuiUtils.makeScrollPane(editor, 300, 200);
        scroller.setPreferredSize(new Dimension(300, 200));
        timeBox = new JComboBox();
        timeBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (ignoreBoxEvents) {
                    return;
                }
                Object item = timeBox.getSelectedItem();
                if ((item == null) || !(item instanceof DatedText)) {
                    item = null;
                }
                setDatedText((DatedText) item);
            }
        });

        ignoreBoxEvents = true;
        GuiUtils.setListData(timeBox, textList);
        ignoreBoxEvents = false;
        JButton deleteBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Delete16.gif", this,
                                     "deleteCurrent");
        deleteBtn.setToolTipText("Delete current time step");
        JComponent footer = GuiUtils.leftRight(
                                GuiUtils.hbox(
                                    deleteBtn,
                                    GuiUtils.label(
                                        "Time: ", timeBox)), GuiUtils.inset(
                                            listenCbx,
                                            new Insets(0, 20, 0, 0)));
        return GuiUtils.topCenterBottom(GuiUtils.label("Subject: ",
                subjectFld), scroller, GuiUtils.inset(footer, 3));
    }

    /**
     * _more_
     */
    public void deleteCurrent() {
        try {
            DatedText oldDT = findDatedText(currentTime);
            if (oldDT != null) {
                textList.remove(oldDT);
                ignoreBoxEvents = true;
                GuiUtils.setListData(timeBox, textList);
                ignoreBoxEvents = false;
                if (textList.size() > 0) {
                    setDatedText((DatedText) textList.get(0));
                } else {
                    setDatedText(null);
                }

            }
        } catch (Exception exc) {
            logException("Deleting time step", exc);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean shouldAddAnimationListener() {
        return true;
    }

    /**
     *  Set the TextList property.
     *
     *  @param value The new value for TextList
     */
    public void setTextList(List value) {
        textList = value;
    }

    /**
     *  Get the TextList property.
     *
     *  @return The TextList
     */
    public List getTextList() {
        try {
            DatedText oldDT = findDatedText(currentTime);
            if (oldDT != null) {
                oldDT.setText(editor.getText());
                oldDT.setSubject(subjectFld.getText());
            }
        } catch (Exception exc) {}
        return textList;
    }

    /**
     * Class DatedText _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class DatedText extends DatedObject {

        /** _more_          */
        private String subject = "";

        /**
         * _more_
         */
        public DatedText() {}

        /**
         * _more_
         *
         * @param date _more_
         */
        public DatedText(Date date) {
            super(date, "");
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getText() {
            return (String) getObject();
        }

        /**
         * _more_
         *
         * @param text _more_
         */
        protected void setText(String text) {
            super.setObject(text);

        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws VisADException _more_
         */
        public DateTime getDateTime() throws VisADException {
            return new DateTime(getDate());
        }

        /**
         *  Set the Subject property.
         *
         *  @param value The new value for Subject
         */
        public void setSubject(String value) {
            subject = value;
        }

        /**
         *  Get the Subject property.
         *
         *  @return The Subject
         */
        public String getSubject() {
            return subject;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return getDate() + "";
        }

    }


    /**
     * Set the ListOnAnimation property.
     *
     * @param value The new value for ListOnAnimation
     */
    public void setListenOnAnimation(boolean value) {
        listenOnAnimation = value;
        if (listenCbx != null) {
            listenCbx.setSelected(value);
        }
    }

    /**
     * Get the ListenOnAnimation property.
     *
     * @return The ListenOnAnimation
     */
    public boolean getListenOnAnimation() {
        if (listenCbx != null) {
            return listenCbx.isSelected();
        }
        return listenOnAnimation;
    }


}

