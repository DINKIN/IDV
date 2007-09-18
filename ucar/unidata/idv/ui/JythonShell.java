/*
 * $Id: DataTree.java,v 1.50 2007/08/21 12:15:45 jeffmc Exp $
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





package ucar.unidata.idv.ui;


import org.python.core.*;
import org.python.util.*;

import ucar.unidata.data.*;

import ucar.unidata.idv.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.awt.*;
import java.awt.event.*;


import java.io.*;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.*;


import javax.swing.tree.*;



/**
 * This class provides  an interactive shell for running JYthon
 *
 * @author IDV development team
 * @version $Revision: 1.50 $Date: 2007/08/21 12:15:45 $
 */
public class JythonShell {

    /** _more_ */
    private IntegratedDataViewer idv;

    /** _more_ */
    private PythonInterpreter interp;

    /** _more_ */
    private JFrame frame;

    /** _more_ */
    private JTextField commandFld;

    /** _more_ */
    private JTextArea commandArea;

    /** _more_ */
    private JButton flipBtn;

    /** _more_ */
    private GuiUtils.CardLayoutPanel cardLayoutPanel;

    /** _more_ */
    private JEditorPane editorPane;

    /** _more_ */
    private StringBuffer sb = new StringBuffer();

    /** _more_ */
    private List history = new ArrayList();

    /** _more_ */
    private int historyIdx = -1;




    /**
     * _more_
     *
     * @param theIdv _more_
     */
    public JythonShell(IntegratedDataViewer theIdv) {

        this.idv   = theIdv;
        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        createInterpreter();
        JScrollPane scroller = GuiUtils.makeScrollPane(editorPane, 400, 300);
        scroller.setPreferredSize(new Dimension(400, 300));
        commandFld = new JTextField();
        GuiUtils.setFixedWidthFont(commandFld);
        commandFld.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e, commandFld);
            }
        });
        commandFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                eval();
            }
        });
        commandFld.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showProcedurePopup(commandFld);
                }
            }
        });

        commandArea = new JTextArea("", 4, 30);
        GuiUtils.setFixedWidthFont(commandArea);
        commandArea.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e, commandArea);
            }
        });
        commandArea.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showProcedurePopup(commandArea);
                }
            }
        });
        cardLayoutPanel = new GuiUtils.CardLayoutPanel();
        cardLayoutPanel.addCard(GuiUtils.top(commandFld));
        cardLayoutPanel.addCard(GuiUtils.makeScrollPane(commandArea, 200,
                100));
        flipBtn = GuiUtils.makeImageButton("/auxdata/ui/icons/DownDown.gif",
                                           this, "flipField");
        JButton evalBtn = GuiUtils.makeButton("Evaluate:", this, "eval");
        JComponent bottom = GuiUtils.leftCenterRight(GuiUtils.top(evalBtn),
                                cardLayoutPanel, GuiUtils.top(flipBtn));
        JComponent contents = GuiUtils.centerBottom(scroller, bottom);
        contents = GuiUtils.inset(contents, 5);

        JMenuBar menuBar = new JMenuBar();
        List     items   = new ArrayList();
        items.add(GuiUtils.makeMenuItem("Export Commands", this,
                                        "exportHistory"));
        menuBar.add(GuiUtils.makeMenu("File", items));


        items = new ArrayList();
        List      displayMenuItems = new ArrayList();


        List      cds              = idv.getControlDescriptors();
        Hashtable catMenus         = new Hashtable();
        for (int i = 0; i < cds.size(); i++) {
            ControlDescriptor cd = (ControlDescriptor) cds.get(i);
            JMenu catMenu = (JMenu) catMenus.get(cd.getDisplayCategory());
            if (catMenu == null) {
                catMenu = new JMenu(cd.getDisplayCategory());
                catMenus.put(cd.getDisplayCategory(), catMenu);
                displayMenuItems.add(catMenu);
            }
            catMenu.add(GuiUtils.makeMenuItem(cd.getDescription(), this,
                    "insert", "'" + cd.getControlId() + "'"));
        }


        items.add(GuiUtils.makeMenuItem("Clear", this, "clear"));
        items.add(GuiUtils.makeMenu("Insert Display Id", displayMenuItems));
        menuBar.add(GuiUtils.makeMenu("Edit", items));


        items = new ArrayList();
        items.add(GuiUtils.makeMenuItem("Help", this, "showHelp"));
        menuBar.add(GuiUtils.makeMenu("Help", items));


        contents = GuiUtils.topCenter(menuBar, contents);
        frame    = new JFrame("Jython Shell");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                idv.getJythonManager().removeInterpreter(interp);
            }
        });
        frame.getContentPane().add(contents);
        frame.pack();
        frame.setLocation(100, 100);
        frame.setVisible(true);


    }


    /**
     * _more_
     */
    public void toFront() {
        GuiUtils.toFront(frame);
    }

    /**
     * _more_
     */
    public void flipField() {
        cardLayoutPanel.flip();
        if (getCommandFld() instanceof JTextArea) {
            flipBtn.setIcon(
                GuiUtils.getImageIcon("/auxdata/ui/icons/UpUp.gif"));
        } else {
            flipBtn.setIcon(
                GuiUtils.getImageIcon("/auxdata/ui/icons/DownDown.gif"));
        }

    }

    /**
     * _more_
     *
     * @param t _more_
     */
    public void appendText(String t) {
        GuiUtils.insertText(getCommandFld(), t);
    }

    /**
     * _more_
     *
     * @param cmdFld _more_
     */
    public void showProcedurePopup(JTextComponent cmdFld) {
        String t = cmdFld.getText();
        /*
          int pos = cmdFld.getCaretPosition();
          t = t.substring(0, pos);
          String tmp = "";
          for(int i=t.length()-1;i>=0;i--) {
          char c = t.charAt(i);
          if(!Character.isJavaIdentifierPart(c)) break;
          tmp = c+tmp;
          }
          t=tmp;
          if(t.length()==0) {
          t = null;
          }
          //            System.err.println(t);
          */
        t = null;

        JPopupMenu popup = GuiUtils.makePopupMenu(
                               idv.getJythonManager().makeProcedureMenu(
                                   this, "appendText", t));
        if (popup != null) {
            popup.show(cmdFld, 0, (int) cmdFld.getBounds().getHeight());
        }

    }


    /**
     * _more_
     *
     * @param e _more_
     * @param cmdFld _more_
     */
    private void handleKeyPress(KeyEvent e, JTextComponent cmdFld) {
        if ((e.getKeyCode() == e.VK_M) && e.isControlDown()) {
            showProcedurePopup(cmdFld);
            return;
        }


        if ((e.getKeyCode() == e.VK_B) && e.isControlDown()) {
            if (cmdFld.getCaretPosition() > 0) {
                cmdFld.setCaretPosition(cmdFld.getCaretPosition() - 1);
            }
        }
        if ((e.getKeyCode() == e.VK_F) && e.isControlDown()) {
            if (cmdFld.getCaretPosition() < cmdFld.getText().length()) {
                cmdFld.setCaretPosition(cmdFld.getCaretPosition() + 1);
            }
        }
        if (((e.getKeyCode() == e.VK_UP)
                || ((e.getKeyCode() == e.VK_P)
                    && e.isControlDown())) && (history.size() > 0)) {
            if ((historyIdx < 0) || (historyIdx >= history.size())) {
                historyIdx = history.size() - 1;
            } else {
                historyIdx--;
                if (historyIdx < 0) {
                    historyIdx = 0;
                }
            }
            if ((historyIdx >= 0) && (historyIdx < history.size())) {
                cmdFld.setText((String) history.get(historyIdx));
            }
        }
        if (((e.getKeyCode() == e.VK_DOWN)
                || ((e.getKeyCode() == e.VK_N)
                    && e.isControlDown())) && (history.size() > 0)) {
            if ((historyIdx < 0) || (historyIdx >= history.size())) {
                historyIdx = history.size() - 1;
            } else {
                historyIdx++;
                if (historyIdx >= history.size()) {
                    historyIdx = history.size() - 1;
                }
            }
            if ((historyIdx >= 0) && (historyIdx < history.size())) {
                cmdFld.setText((String) history.get(historyIdx));
            }
        }

    }


    /**
     * _more_
     *
     * @param s _more_
     */
    public void insert(String s) {
        String t   = getCommandFld().getText();
        int    pos = getCommandFld().getCaretPosition();
        t = t.substring(0, pos) + s + t.substring(pos);
        getCommandFld().setText(t);
        getCommandFld().setCaretPosition(pos + s.length());
    }


    /**
     * _more_
     */
    public void showHelp() {
        idv.getIdvUIManager().showHelp("idv.tools.jythonshell");
    }

    /**
     * _more_
     */
    public void exportHistory() {
        if (history.size() == 0) {
            LogUtil.userMessage("There are no commands to export");
            return;
        }
        String procedureName =
            GuiUtils.getInput("Enter optional procedure name",
                              "Procedure name: ", "",
                              " (Leave blank for no procedure)");
        if (procedureName == null) {
            return;
        }
        String s;
        if (procedureName.trim().length() == 0) {
            s = StringUtil.join("\n", history);
        } else {
            s = "def " + procedureName + "():\n" + "    "
                + StringUtil.join("\n    ", history);
        }
        s = "#From shell\n" + s + "\n\n";
        idv.getJythonManager().appendJython(s);
    }



    /**
     * _more_
     */
    private void createInterpreter() {
        if (interp != null) {
            idv.getJythonManager().removeInterpreter(interp);
        }
        interp = idv.getJythonManager().createInterpreter();
        interp.set("shell", this);
        OutputStream os = new OutputStream() {
            public void write(int b) {
                //                    output(new String(b));
            }
            public void write(byte[] b, int off, int len) {
                output(new String(b, off, len) + "<br>");
            }
        };

        interp.setOut(os);
        interp.setErr(os);
    }


    /**
     * _more_
     */
    public void clear() {
        try {
            historyIdx = -1;
            history    = new ArrayList();
            sb         = new StringBuffer();
            createInterpreter();
            editorPane.setText("");
        } catch (Exception exc) {
            LogUtil.logException(
                "An error occurred clearing the Jython shell", exc);
        }
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private JTextComponent getCommandFld() {
        if (cardLayoutPanel.getVisibleIndex() == 0) {
            return commandFld;
        }
        return commandArea;
    }


    /**
     * _more_
     */
    public void eval() {
        JTextComponent cmdFld = getCommandFld();
        String         cmd    = cmdFld.getText();
        cmdFld.setText("");
        eval(cmd);
        history.add(cmd);
        historyIdx = -1;
    }

    /**
     * _more_
     *
     * @param m _more_
     */
    private void output(String m) {
        sb.append(m);
        editorPane.setText(sb.toString());
        editorPane.repaint();
        editorPane.scrollRectToVisible(new Rectangle(0, 10000, 1, 1));
    }

    /**
     * _more_
     *
     * @param jython _more_
     */
    private void eval(String jython) {
        Misc.run(this, "evalInThread", jython);
    }

    /**
     * _more_
     *
     * @param jython _more_
     */
    public void evalInThread(String jython) {
        try {
            String html = StringUtil.replace(jython.trim(), "\n", "<br>");
            html = StringUtil.replace(html, " ", "&nbsp;");
            html = StringUtil.replace(html, "\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
            output("<div style=\"margin:0; margin-bottom:1; background-color:#cccccc; \">"
                   + html + "</div>");
            interp.exec(jython);
        } catch (PyException pse) {
            output("<font color=\"red\">Error: " + pse.toString()
                   + "</font><br>");
        } catch (Exception exc) {
            output("<font color=\"red\">Error: " + exc + "</font><br>");
        }
    }

}

