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

import ucar.unidata.ui.InteractiveShell;

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

import visad.*;
import visad.jmet.DumpType;


/**
 * This class provides  an interactive shell for running JYthon
 *
 * @author IDV development team
 * @version $Revision: 1.50 $Date: 2007/08/21 12:15:45 $
 */
public class JythonShell extends InteractiveShell {

    public static final String PROP_JYTHON_SHELL_HISTORY = "prop.jython.shell.history";

    /** _more_ */
    private IntegratedDataViewer idv;

    /** _more_ */
    private PythonInterpreter interp;

    private  OutputStream   outputStream;


    /**
     * _more_
     *
     * @param theIdv _more_
     */
    public JythonShell(IntegratedDataViewer theIdv) {
        super("Jython Shell");
        this.idv = theIdv;
        List oldHistory = (List)idv.getStore().get(PROP_JYTHON_SHELL_HISTORY);
        if(oldHistory!=null) {
            history = new ArrayList(oldHistory);
        }
        createInterpreter();
        //Create the gui
        init();
    }


    public void listHistory() {
        for(int i=0;i<history.size();i++) {
            super.eval((String) history.get(i));
        }
    }

    public void saveHistory() {
        idv.getStore().put(PROP_JYTHON_SHELL_HISTORY,history);
        idv.getStore().save();
    }

 
    /**
     * This gets called by the base class to make the frame.
     * If you don't want this to popup then make this method a noop
     * You can access the GUI contents with the member contents
     *
     * @param contents
     */
    protected void makeFrame() {
        super.makeFrame();
        //When the window closes remove the interpreter
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (interp != null) {
                    idv.getJythonManager().removeInterpreter(interp);
                }
            }
        });
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private PythonInterpreter getInterpreter() {
        if (interp == null) {
            createInterpreter();
        }
        return interp;
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

        List items = new ArrayList();
        if(history.size()>0) {
            List historyItems = new ArrayList();
            for(int i=history.size()-1;i>=0;i--) {
                historyItems.add(GuiUtils.makeMenuItem(history.get(i).toString(), this,"eval",
                                                       history.get(i)));
            }
            items.add(GuiUtils.makeMenu("History",historyItems));
                                        
        }

        items.add(GuiUtils.makeMenu("Insert Display Type", getDisplayMenuItems()));
        items.add(GuiUtils.makeMenu("Insert Procedure Call",idv.getJythonManager().makeProcedureMenu(
                                                                                                      this, "insertText", t)));

        items.add(GuiUtils.makeMenu("Insert Idv Action",idv.getIdvUIManager().makeActionMenu(this,"insertText",true)));

        JPopupMenu popup = GuiUtils.makePopupMenu(items);
        if (popup != null) {
            popup.show(cmdFld, 0, (int) cmdFld.getBounds().getHeight());
        }
    }


    public void listVars() {
        PyStringMap seq   = (PyStringMap) getInterpreter().getLocals();
        PyList      items = seq.items();
        StringBuffer sb = new StringBuffer("Variables:<br>");
        for (int itemIdx = 0; itemIdx < items.__len__(); itemIdx++) {
            PyTuple pair = (PyTuple) items.__finditem__(itemIdx);
            Object obj = pair.__finditem__(1);
            String name = pair.__finditem__(0).toString();
            if (obj instanceof PyFunction || 
                obj instanceof PyReflectedFunction || 
                obj instanceof PyJavaClass || 
                obj instanceof PyJavaPackage || 
                obj instanceof PySystemState || 
                obj instanceof PyJavaPackage || name.startsWith("__") || name.equals("JyVars")) {
                continue;
            }
            sb.append ("&nbsp;&nbsp;&nbsp;" + name+"<br>");
        }
        output(sb.toString());
    }


    public void insertAction(String action) {
        insertText("idv.handleAction('action:" + action +"')");
    }

    /**
     * _more_
     *
     * @param e _more_
     * @param cmdFld _more_
     */
    protected void handleKeyPress(KeyEvent e, JTextComponent cmdFld) {
        super.handleKeyPress(e, cmdFld);
        if ((e.getKeyCode() == e.VK_M) && e.isControlDown()) {
            showProcedurePopup(cmdFld);
            return;
        }
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
        outputStream = new OutputStream() {
            public void write(int b) {
                //                    output(new String(b));
            }
            public void write(byte[] b, int off, int len) {
                String s = new String(new String(b, off, len));
                s = StringUtil.replace(s,"\n","<br>");
                s = StringUtil.replace(s," ","&nbsp;");
                s = StringUtil.replace(s,"\t","&nbsp;&nbsp;&nbsp;&nbsp;");
                output(s);
            }
        };


        interp.setOut(outputStream);
        interp.setErr(outputStream);
    }


    /**
     * _more_
     */
    public void clear() {
        try {
            super.clear();
            createInterpreter();
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
    protected JMenuBar doMakeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        List     items   = new ArrayList();
        items.add(GuiUtils.makeMenuItem("Export Commands", this,
                                        "exportHistory"));
        items.add(GuiUtils.makeMenuItem("Save History", this,
                                        "saveHistory"));
        items.add(GuiUtils.makeMenuItem("List History", this,
                                        "listHistory"));
        items.add(GuiUtils.makeMenuItem("List Variables", this,
                                        "listVars"));
        menuBar.add(GuiUtils.makeMenu("File", items));


        items = new ArrayList();
        items.add(GuiUtils.makeMenuItem("Clear All", this, "clear"));
        items.add(GuiUtils.makeMenuItem("Clear Output", this, "clearOutput"));
        //        items.add(GuiUtils.makeMenu("Insert Display Type", getDisplayMenuItems()));
        menuBar.add(GuiUtils.makeMenu("Edit", items));

        items = new ArrayList();
        items.add(GuiUtils.makeMenuItem("Help", this, "showHelp"));
        menuBar.add(GuiUtils.makeMenu("Help", items));
        return menuBar;
    }


    protected List getDisplayMenuItems() {
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
        return displayMenuItems;
    }



    /**
     * _more_
     *
     * @param commandFld _more_
     * @param e _more_
     */
    protected void handleRightMouseClick(JTextComponent commandFld,
                                         MouseEvent e) {
        showProcedurePopup(commandFld);
    }


    /**
     * _more_
     *
     * @param code _more_
     *
     * @return _more_
     */
    protected String formatCode(String code) {
        String html = StringUtil.replace(code.trim(), "\n", "<br>");
        html = StringUtil.replace(html, " ", "&nbsp;");
        html = StringUtil.replace(html, "\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
        return html;
    }


    /**
     * _more_
     *
     * @param jython _more_
     */
    public void eval(String jython) {
        try {
            if(jython.trim().length()==0) return;
            super.eval(jython);
            StringBuffer sb = new StringBuffer();
            List lines = StringUtil.split(jython,"\n",false,false);
            for(int i=0;i<lines.size();i++) {
                String line = (String)lines.get(i);
                if(line.trim().startsWith("?")) {
                    while(!line.startsWith("?")) {
                        sb.append(line.substring(0,1));
                        line = line.substring(1);
                    }
                    line = "print " + line.trim().substring(1);
                }
                sb.append (line);
                sb.append ("\n");
            }
            getInterpreter().exec(sb.toString());
        } catch (PyException pse) {
            output("<font color=\"red\">Error: " + pse.toString()
                   + "</font><br>");
        } catch (Exception exc) {
            output("<font color=\"red\">Error: " + exc + "</font><br>");
        }
    }


    public void printType(Data d) {
        try {
            startBufferingOutput();
            MathType t = d.getType();
            visad.jmet.DumpType.dumpMathType(t,outputStream);
            output("<hr>DataType analysis...");
            visad.jmet.DumpType.dumpDataType(d,outputStream);
        } catch (Exception exc) {
            LogUtil.logException(
                "An error occurred printing types", exc);
        }
        endBufferingOutput();
    }




}

