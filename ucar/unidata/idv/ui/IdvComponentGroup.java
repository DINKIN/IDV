/*
 * $Id: IdvXmlUi.java,v 1.54 2007/08/16 14:08:22 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
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


import ucar.unidata.idv.*;

import ucar.unidata.idv.control.*;

import ucar.unidata.ui.ComponentGroup;
import ucar.unidata.ui.HtmlComponent;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.xml.XmlUtil;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.LogUtil;

import java.util.ArrayList;
import java.util.List;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.CDATASection;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 */

public class IdvComponentGroup extends ComponentGroup {

    /** _more_          */
    IntegratedDataViewer idv;

    /**
     * _more_
     */
    public IdvComponentGroup() {}


    /**
     * _more_
     *
     * @param idv _more_
     * @param name _more_
     */
    public IdvComponentGroup(IntegratedDataViewer idv, String name) {
        super(name);
        this.idv = idv;
    }


    public void writeSkin() {
        try {
        String filename  = FileManager.getWriteFile(FileManager.FILTER_XML,
                                         FileManager.SUFFIX_XML);
        if (filename == null) {
            return;
        }
        Element root = createXmlNode(XmlUtil.makeDocument());
        setState(root);
        String xml  = XmlUtil.toString(root);
        String templatePath = idv.getProperty("idv.ui.skin.template",(String)null);
        System.err.println (templatePath);
        if(templatePath!=null) {
            String template = IOUtil.readContents(templatePath, getClass(), (String) null);
            if(template!=null) {
                xml = StringUtil.replace(template,"%contents%", xml);
            }
        }
        IOUtil.writeFile(filename, xml);
        } catch(Exception exc) {
            LogUtil.logException("Error writing skin file", exc);
        }
    }

    public Element createXmlNode(Document doc) {
        Element node = doc.createElement(IdvUIManager.COMP_COMPONENT_GROUP);
        List displayComponents = getDisplayComponents();
        for (int i = 0; i < displayComponents.size(); i++) {
            ComponentHolder comp = (ComponentHolder) displayComponents.get(i);
            Element child = null;
            if(comp instanceof IdvComponentHolder) {
                child = ((IdvComponentHolder)comp).createXmlNode(doc);
            } else if(comp instanceof IdvComponentGroup) {
                child = ((IdvComponentGroup)comp).createXmlNode(doc);
            } else if(comp instanceof HtmlComponent) {
                child = doc.createElement(IdvUIManager.COMP_COMPONENT_HTML);
                child.appendChild(XmlUtil.makeCDataNode(doc,((HtmlComponent)comp).getText()));
            }
            if(child!=null) {
                comp.setState(child);
                node.appendChild(child);
            }
        }

        return node;
    }


    /**
     * _more_
     *
     * @param items _more_
     *
     * @return _more_
     */
    protected List getPopupMenuItems(List items) {
        super.getPopupMenuItems(items);
        List newItems = new ArrayList();
        newItems.add(GuiUtils.makeMenuItem("Map View", this, "makeNew",
                                           IdvUIManager.COMP_MAPVIEW));
        newItems.add(GuiUtils.makeMenuItem("Globe View", this, "makeNew",
                                           IdvUIManager.COMP_GLOBEVIEW));
        newItems.add(GuiUtils.makeMenuItem("Transect View", this, "makeNew",
                                           IdvUIManager.COMP_TRANSECTVIEW));
        newItems.add(GuiUtils.makeMenuItem("Field Selector", this, "makeNew",
                                           IdvUIManager.COMP_DATASELECTOR));
        newItems.add(GuiUtils.makeMenuItem("Tab Group", this, "makeNew",
                                           IdvUIManager.COMP_COMPONENT_GROUP));
        newItems.add(GuiUtils.makeMenuItem("Html Text", this, "makeNew",
                                           IdvUIManager.COMP_COMPONENT_HTML));


        items.add(GuiUtils.makeMenu("New", newItems));
        items.add(GuiUtils.makeMenuItem("Write Skin", this,"writeSkin"));


        List importItems = new ArrayList();
        List vms         = idv.getVMManager().getViewManagers();
        for (int vmIdx = 0; vmIdx < vms.size(); vmIdx++) {
            ViewManager vm              = (ViewManager) vms.get(vmIdx);
            List        viewItems       = new ArrayList();
            List        displayControls = vm.getControls();
            if (displayControls.size() > 0) {
                viewItems.add(GuiUtils.makeMenuItem("Import All", this,
                        "importAllDisplayControls", displayControls));
                viewItems.add(GuiUtils.MENU_SEPARATOR);
            }
            for (int i = 0; i < displayControls.size(); i++) {
                DisplayControlImpl dc =
                    (DisplayControlImpl) displayControls.get(i);
                if(dc.getComponentHolder()!=null && dc.getComponentHolder().getParent()==this) {
                    continue;
                }
                viewItems.add(GuiUtils.makeMenuItem(dc.getLabel(), this,
                        "importDisplayControl", dc));
            }
            if (viewItems.size() > 0) {
                String name = vm.getName();
                if ((name == null) || (name.trim().length() == 0)) {
                    name = "View " + (vmIdx + 1);
                }
                importItems.add(GuiUtils.makeMenu(name, viewItems));
            }
        }
        items.add(GuiUtils.makeMenu("Import Display", importItems));
        return items;
    }

    /**
     * _more_
     *
     * @param displayControls _more_
     */
    public void importAllDisplayControls(List displayControls) {
        for (int i = 0; i < displayControls.size(); i++) {
            DisplayControlImpl dc =
                (DisplayControlImpl) displayControls.get(i);
            if(dc.getComponentHolder()!=null && dc.getComponentHolder().getParent()==this) {
                continue;
            }
            importDisplayControl(dc);
        }
    }


    /**
     * _more_
     *
     * @param dc _more_
     */
    public void importDisplayControl(DisplayControlImpl dc) {
        if (dc.getComponentHolder() != null) {
            dc.getComponentHolder().removeDisplayControl(dc);
        }
        idv.getIdvUIManager().getViewPanel().removeDisplayControl(dc);
        dc.guiImported();
        addComponent(new IdvComponentHolder(idv, dc));
    }

    /**
     * _more_
     *
     * @param what _more_
     */
    public void makeNew(String what) {
        try {
            if (what.equals(IdvUIManager.COMP_MAPVIEW)) {
                ViewManager vm = new MapViewManager(idv,
                                     new ViewDescriptor(),
                                     "showControlLegend=false");
                idv.getVMManager().addViewManager(vm);
                addComponent(new IdvComponentHolder(idv, vm));
            } else if (what.equals(IdvUIManager.COMP_GLOBEVIEW)) {
                MapViewManager vm = new MapViewManager(idv,
                                        new ViewDescriptor(),
                                        "showControlLegend=false");
                vm.setUseGlobeDisplay(true);
                idv.getVMManager().addViewManager(vm);
                addComponent(new IdvComponentHolder(idv, vm));
            } else if (what.equals(IdvUIManager.COMP_TRANSECTVIEW)) {
                ViewManager vm = new TransectViewManager(idv,
                                     new ViewDescriptor(),
                                     "showControlLegend=false");
                idv.getVMManager().addViewManager(vm);
                addComponent(new IdvComponentHolder(idv, vm));
            } else if (what.equals(IdvUIManager.COMP_DATASELECTOR)) {
                addComponent(new IdvComponentHolder(idv,
                        idv.getIdvUIManager().createDataSelector(false,
                            false)));
            } else if (what.equals(IdvUIManager.COMP_COMPONENT_GROUP)) {
                String name = GuiUtils.getInput("Enter name for tab group",
                                  "Name: ", "Group");
                if (name == null) {
                    return;
                }
                IdvComponentGroup group = new IdvComponentGroup(idv, name);
                group.setLayout(group.LAYOUT_TABS);
                addComponent(group);

            } else if (what.equals(IdvUIManager.COMP_COMPONENT_HTML)) {
                String text = GuiUtils.getInput("Enter html",
                                  "Html: ", "");
                if (text == null) {
                    return;
                }
                ComponentHolder comp = new HtmlComponent("Html Text",text);
                comp.setShowLabel(false);
                addComponent(comp);
            }
        } catch (Exception exc) {
            LogUtil.logException("Error making new " + what, exc);
        }
    }



    /**
     *  Set the Idv property.
     *
     *  @param value The new value for Idv
     */
    public void setIdv(IntegratedDataViewer value) {
        idv = value;
    }

    /**
     *  Get the Idv property.
     *
     *  @return The Idv
     */
    public IntegratedDataViewer getIdv() {
        return idv;
    }



}

