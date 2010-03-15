/**
 * $Id: ,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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

package ucar.unidata.repository.data;


import org.w3c.dom.*;

import ucar.unidata.repository.*;
import ucar.unidata.repository.harvester.*;
import ucar.unidata.repository.auth.*;
import ucar.unidata.repository.data.*;
import ucar.unidata.repository.metadata.*;
import ucar.unidata.repository.type.*;

import ucar.unidata.sql.SqlUtil;


import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;



import java.io.*;



import java.net.*;



import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;




/**
 * Class CatalogHarvester _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class CatalogHarvester extends Harvester {

    /** _more_ */
    Group topGroup;

    /** _more_ */
    boolean recurse = false;

    /** _more_ */
    boolean download = false;

    /** _more_ */
    Hashtable seen = new Hashtable();

    /** _more_ */
    List groups = new ArrayList();

    /** _more_ */
    int catalogCnt = 0;

    /** _more_ */
    int entryCnt = 0;

    /** _more_ */
    int groupCnt = 0;

    /** _more_ */
    User user;

    /** _more_ */
    String topUrl;

    /** _more_ */
    List<Entry> entries = new ArrayList<Entry>();

    /**
     * _more_
     *
     * @param repository _more_
     * @param group _more_
     * @param url _more_
     * @param user _more_
     * @param recurse _more_
     * @param download _more_
     */
    public CatalogHarvester(Repository repository, Group group, String url,
                            User user, boolean recurse, boolean download) {
        super(repository);
        setName("Catalog harvester");
        this.recurse  = recurse;
        this.download = download;
        this.topGroup = group;
        this.topUrl   = url;
        this.user     = user;
    }



    /**
     * _more_
     *
     *
     * @param timestamp _more_
     * @throws Exception _more_
     */
    protected void runInner(int timestamp) throws Exception {
        groups = new ArrayList();
        importCatalog(topUrl, topGroup, 0, timestamp);
        //getEntryManager().processEntries(this, null, entries,false);
        if (entries.size() > 0) {
            getEntryManager().processEntries(this, null, entries, false);
        }
        entries = new ArrayList<Entry>();
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param parent _more_
     * @param depth _more_
     * @param timestamp _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean importCatalog(String url, Group parent, int depth,
                                  int timestamp)
            throws Exception {
        if ( !canContinueRunning(timestamp)) {
            return true;
        }
        if (seen.get(url) != null) {
            return true;
        }
        if (depth > 10) {
            System.err.println("Catalogs go too deep:" + url);
            return true;
        }
        if (url.indexOf("hyrax/LBA") >= 0) {
            //            System.err.println ("Skipping bad one");
            return true;
        }


        catalogCnt++;
        if (catalogCnt % 10 == 0) {
            System.err.print(".");
        }
        seen.put(url, url);
        try {
            Element root = XmlUtil.getRoot(url, getClass());
            if (root == null) {
                System.err.println("Could not load catalog:" + url);
                System.err.println("xml:" + IOUtil.readContents(url, getClass()));
                return true;
            }
            //                System.err.println("loaded:" + url);
            NodeList children    = XmlUtil.getElements(root);
            int      cnt         = 0;
            Element  datasetNode = null;
            for (int i = 0; i < children.getLength(); i++) {
                Element child = (Element) children.item(i);
                if (XmlUtil.isTag(child,CatalogUtil.TAG_DATASET)
                    || XmlUtil.isTag(child,CatalogUtil.TAG_CATALOGREF)) {
                    if (XmlUtil.isTag(child,CatalogUtil.TAG_DATASET)) {
                        datasetNode = (Element) child;
                    }
                    cnt++;
                }
            }

            //If there is just one top-level dataset node then just load that
            if ((cnt == 1) && (datasetNode != null)) {
                recurseCatalog((Element) datasetNode, parent, url, 0, depth,
                               timestamp);
            } else {
                recurseCatalog((Element) root, parent, url, 0, depth,
                               timestamp);
            }
            return true;
        } catch (Exception exc) {
            exc.printStackTrace();
            //            log("",exc);
            return false;
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param metadataList _more_
     */
    private void insertMetadata(Entry entry, List<Metadata> metadataList) {
        for (Metadata metadata : metadataList) {
            metadata.setEntryId(entry.getId());
            try {
                if (metadata.getAttr1().length() > 10000) {
                    repository.getLogManager().logError("Too long metadata:"
                            + metadata.getAttr1().substring(0, 100) + "...");
                    continue;
                }
                getMetadataManager().insertMetadata(metadata);
            } catch (Exception exc) {
                repository.getLogManager().logError("Bad metadata", exc);
                System.err.println("metadata attr1" + metadata.getAttr1());
                System.err.println("metadata attr2" + metadata.getAttr2());
                System.err.println("metadata attr3" + metadata.getAttr3());
                System.err.println("metadata attr4" + metadata.getAttr4());

            }
        }

    }

    /**
     * _more_
     *
     * @param node _more_
     * @param parent _more_
     * @param catalogUrlPath _more_
     * @param xmlDepth _more_
     * @param recurseDepth _more_
     * @param timestamp _more_
     *
     * @throws Exception _more_
     */
    private void recurseCatalog(Element node, Group parent,
                                String catalogUrlPath, int xmlDepth,
                                int recurseDepth, int timestamp)
            throws Exception {

        String tab = "";
        for (int i = 0; i < xmlDepth; i++) {
            tab = tab + "  ";
        }
        if ( !canContinueRunning(timestamp)) {
            return;
        }
        URL catalogUrl = new URL(catalogUrlPath);
        String name =
            XmlUtil.getAttribute(node, ATTR_NAME,
                                 IOUtil.getFileTail(catalogUrlPath));
        NodeList elements = XmlUtil.getElements(node);
        String urlPath = XmlUtil.getAttribute(node, CatalogUtil.ATTR_URLPATH,
                             (String) null);
        if (urlPath == null) {
            Element accessNode = XmlUtil.findChild(node,
                                     CatalogUtil.TAG_ACCESS);
            if (accessNode != null) {
                urlPath = XmlUtil.getAttribute(accessNode,
                        CatalogUtil.ATTR_URLPATH);
            }
        }


        boolean haveChildDatasets = false;
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            if (XmlUtil.isTag(child,CatalogUtil.TAG_DATASET)) {
                haveChildDatasets = true;
                break;
            }
        }

        //        System.err.println(tab+"name:" + name+"  #children:" + elements.getLength() +" depth:" + xmlDepth + " " + urlPath +" " + haveChildDatasets);

        boolean madeEntry = false;
        if ( !haveChildDatasets && (xmlDepth > 0) && (urlPath != null)) {
            Element serviceNode = CatalogUtil.findServiceNodeForDataset(node,
                                      false, null);

            boolean isOpendap = false;
            if (serviceNode != null) {
                String path = XmlUtil.getAttribute(serviceNode, "base");
                urlPath = new URL(catalogUrl, path + urlPath).toString();
                String serviceType = XmlUtil.getAttribute(serviceNode,
                                         CatalogUtil.ATTR_SERVICETYPE,
                                         "").toLowerCase();
                isOpendap = serviceType.equals("opendap")
                            || serviceType.equals("dods");
            }

            TypeHandler typeHandler = repository.getTypeHandler((isOpendap
                    ? TypeHandler.TYPE_OPENDAPLINK
                    : TypeHandler.TYPE_FILE));
            entryCnt++;
            Entry  entry      = typeHandler.createEntry(repository.getGUID());
            Date   createDate = new Date();
            String ext        = IOUtil.getFileExtension(urlPath);
            if (ext.startsWith(".")) {
                ext = ext.substring(1);
            }
            if (ext.length() > 0) {
                entry.addMetadata(new Metadata(repository.getGUID(),
                        entry.getId(), EnumeratedMetadataHandler.TYPE_TAG,
                        DFLT_INHERITED, ext, Metadata.DFLT_ATTR,
                        Metadata.DFLT_ATTR, Metadata.DFLT_ATTR,
                        Metadata.DFLT_EXTRA));
            }

            Resource resource = null;
            if (download
                    && (urlPath.startsWith("http:")
                        || urlPath.startsWith("https:")
                        || urlPath.startsWith("ftp:"))) {
                String tail    = IOUtil.getFileTail(urlPath);
                File   newFile = getStorageManager().getTmpFile(null, tail);
                try {
                    URL           fromUrl    = new URL(urlPath);
                    URLConnection connection = fromUrl.openConnection();
                    InputStream   fromStream = connection.getInputStream();
                    OutputStream toStream =
                        getStorageManager().getFileOutputStream(newFile);
                    int bytes = IOUtil.writeTo(fromStream, toStream);
                    toStream.close();
                    fromStream.close();
                    if (bytes > 0) {
                        String theFile =
                            getStorageManager().moveToStorage((Request) null,
                                newFile).toString();
                        resource = new Resource(new File(theFile),
                                Resource.TYPE_STOREDFILE);
                    }
                } catch (Exception ignore) {
                    System.err.println("error " + ignore);
                    ignore.printStackTrace();
                }
            }
            if (resource == null) {
                resource = new Resource(urlPath, Resource.TYPE_URL);
            }

            entry.initEntry(name, "", parent, user, resource, "",
                            createDate.getTime(), createDate.getTime(),
                            createDate.getTime(), null);
            entries.add(entry);
            madeEntry  =true;

            typeHandler.initializeNewEntry(entry);

            List<Metadata> metadataList = new ArrayList<Metadata>();
            CatalogOutputHandler.collectMetadata(repository, metadataList,
                    node);
            metadataList.add(new Metadata(repository.getGUID(),
                                          entry.getId(),
                                          ThreddsMetadataHandler.TYPE_LINK,
                                          DFLT_INHERITED,
                                          "Imported from catalog",
                                          catalogUrlPath, Metadata.DFLT_ATTR,
                                          Metadata.DFLT_ATTR,
                                          Metadata.DFLT_EXTRA));
            for (Metadata metadata : metadataList) {
                metadata.setEntryId(entry.getId());
                entry.addMetadata(metadata);
            }


            if (isOpendap && (getAddMetadata()||getAddShortMetadata())) {
                getEntryManager().addInitialMetadata(null,
                                                     (List<Entry>) Misc.newList(entry), getAddMetadata(), getAddShortMetadata());

            }


            if (entries.size() > 100) {
                getEntryManager().processEntries(this, null, entries, false);
                entries = new ArrayList<Entry>();
            }
        }

        if(!madeEntry) {
            name = name.replace(Group.IDDELIMITER, "--");
            name = name.replace("'", "");
            Group group = null;
            Entry newGroup = getEntryManager().findEntryWithName(null, parent,
                                                                 name);
            if ((newGroup != null) && newGroup.isGroup()) {
                group = (Group) newGroup;
            }
            if (group == null) {
                //                System.err.println(tab+"Making new group:" + name);
                group = getEntryManager().makeNewGroup(parent, name, user);
                List<Metadata> metadataList = new ArrayList<Metadata>();
                CatalogOutputHandler.collectMetadata(repository, metadataList,
                                                     node);
                metadataList.add(new Metadata(repository.getGUID(),
                                              group.getId(),
                                              ThreddsMetadataHandler.TYPE_LINK,
                                              DFLT_INHERITED,
                                              "Imported from catalog",
                                              catalogUrlPath, Metadata.DFLT_ATTR,
                                              Metadata.DFLT_ATTR,
                                              Metadata.DFLT_EXTRA));

                insertMetadata(group, metadataList);
                String crumbs = getEntryManager().getBreadCrumbs(null, group,
                                                                 true, topGroup)[1];
                crumbs = crumbs.replace("class=", "xclass=");
                groups.add(crumbs);
                groupCnt++;
                if (groups.size() > 100) {
                    groups = new ArrayList();
                }
            }


            //xxxx
            for (int i = 0; i < elements.getLength(); i++) {
                Element child = (Element) elements.item(i);
                String  tag   = XmlUtil.getLocalName(child);
                if (tag.equals(CatalogUtil.TAG_DATASET)) {
                    recurseCatalog(child, group, catalogUrlPath, xmlDepth + 1,
                                   recurseDepth, timestamp);
                } else if (tag.equals(CatalogUtil.TAG_CATALOGREF)) {
                    if ( !recurse) {
                        continue;
                    }
                    String url = XmlUtil.getAttribute(child, "xlink:href");
                    URL    newCatalogUrl = new URL(catalogUrl, url);
                    //                System.err.println("url:" + newCatalogUrl);
                    if ( !importCatalog(newCatalogUrl.toString(), group,
                                        recurseDepth + 1, timestamp)) {
                        System.err.println("Could not load catalog:" + url);
                        System.err.println("Base catalog:" + catalogUrl);
                        System.err.println("Base URL:"
                                           + XmlUtil.getAttribute(child,
                                                                  "xlink:href"));
                    }
                }
            }
        }
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getExtraInfo() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("Catalog: " + topUrl + "<br>");
        sb.append("Loaded " + catalogCnt + " catalogs<br>");
        sb.append("Created " + entryCnt + " entries<br>");
        sb.append("Created " + groupCnt + " groups");

        StringBuffer groupSB = new StringBuffer();
        groupSB.append("<div class=\"scrollablediv\"><ul>");
        for (int i = 0; i < groups.size(); i++) {
            String groupLine = (String) groups.get(i);
            groupSB.append("<li>");
            groupSB.append(groupLine);
        }
        groupSB.append("</ul></div>");
        sb.append(HtmlUtil.makeShowHideBlock("Entries", groupSB.toString(),
                                             false));
        return sb.toString();
    }


}

