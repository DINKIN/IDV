/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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

package ucar.unidata.repository;


import org.w3c.dom.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class PatternHarvester extends Harvester {

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_DATEFORMAT = "dateformat";

    /** _more_ */
    public static final String ATTR_FILEPATTERN = "filepattern";


    /** _more_ */
    public static final String ATTR_BASEGROUP = "basegroup";

    /** _more_ */
    public static final String ATTR_MOVETOSTORAGE = "movetostorage";


    /** _more_ */
    private String dateFormat = "yyyyMMdd_HHmm";

    /** _more_ */
    private SimpleDateFormat sdf;
    //SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");


    /** _more_ */
    private List<String> patternNames = new ArrayList<String>();


    /** _more_ */
    private String filePatternString = ".*";


    /** _more_ */
    private Pattern filePattern;


    /** _more_ */
    private String baseGroupName = "";


    /** _more_ */
    private boolean moveToStorage = false;

    /** _more_ */
    private List<FileInfo> dirs;


    /** _more_ */
    private Hashtable dirMap = new Hashtable();


    /** _more_ */
    User user;



    /** _more_ */
    private int entryCnt = 0;

    /** _more_ */
    private int newEntryCnt = 0;


    /**
     * _more_
     *
     * @param repository _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public PatternHarvester(Repository repository, String id)
            throws Exception {
        super(repository, id);
        if (groupTemplate.length() == 0) {
            groupTemplate = "${dirgroup}";
        }
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public PatternHarvester(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        if (groupTemplate.length() == 0) {
            groupTemplate = "${dirgroup}";
        }
        init();
    }




    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    protected void init(Element element) throws Exception {
        super.init(element);
        rootDir = new File(XmlUtil.getAttribute(element, ATTR_ROOTDIR, ""));

        moveToStorage = XmlUtil.getAttribute(element, ATTR_MOVETOSTORAGE,
                                             moveToStorage);
        filePatternString = XmlUtil.getAttribute(element, ATTR_FILEPATTERN,
                filePatternString);

        this.baseGroupName = XmlUtil.getAttribute(element, ATTR_BASEGROUP,
                "");

        filePattern = null;
        sdf         = null;
        init();
        dateFormat = XmlUtil.getAttribute(element, ATTR_DATEFORMAT,
                                          dateFormat);
        sdf = null;

    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected User getUser() throws Exception {
        if (user == null) {
            user = repository.getUserManager().getDefaultUser();
        }
        return user;
    }

    /**
     * _more_
     *
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public void applyState(Element element) throws Exception {
        super.applyState(element);
        element.setAttribute(ATTR_FILEPATTERN, filePatternString);
        element.setAttribute(ATTR_MOVETOSTORAGE, "" + moveToStorage);
        element.setAttribute(ATTR_BASEGROUP, baseGroupName);
        element.setAttribute(ATTR_DATEFORMAT, dateFormat);
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applyEditForm(Request request) throws Exception {
        super.applyEditForm(request);
        rootDir = new File(request.getUnsafeString(ATTR_ROOTDIR,
                (rootDir != null)
                ? rootDir.toString()
                : ""));
        filePatternString = request.getUnsafeString(ATTR_FILEPATTERN,
                filePatternString);
        filePattern = null;
        sdf         = null;
        init();
        baseGroupName = request.getUnsafeString(ATTR_BASEGROUP,
                baseGroupName);
        dateFormat = request.getUnsafeString(ATTR_DATEFORMAT, dateFormat);
        if (request.exists(ATTR_MOVETOSTORAGE)) {
            moveToStorage = request.get(ATTR_MOVETOSTORAGE, moveToStorage);
        } else {
            moveToStorage = false;
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void createEditForm(Request request, StringBuffer sb)
            throws Exception {
        super.createEditForm(request, sb);
        String root = (rootDir != null)
                      ? rootDir.toString()
                      : "";
        root = root.replace("\\", "/");
        String extraLabel = "";
        if ((rootDir != null) && !rootDir.exists()) {
            extraLabel = HtmlUtil.space(2)
                         + HtmlUtil.bold("Directory does not exist");
        }
        sb.append(RepositoryManager.tableSubHeader("Look for files"));
        sb.append(HtmlUtil.formEntry(msgLabel("In directory"),
                                     HtmlUtil.input(ATTR_ROOTDIR, root,
                                         HtmlUtil.SIZE_60) + extraLabel));
        sb.append(HtmlUtil.formEntry(msgLabel("That match pattern"),
                                     HtmlUtil.input(ATTR_FILEPATTERN,
                                         filePatternString,
                                         HtmlUtil.SIZE_60)));

        sb.append(
            RepositoryManager.tableSubHeader("Then create an entry with"));


        //        sb.append(
        //HtmlUtil.formEntry("",
        //msgLabel("Then create an entry with")));

        sb.append(HtmlUtil.formEntry(msgLabel("Name template"),
                                     HtmlUtil.input(ATTR_NAMETEMPLATE,
                                         nameTemplate, HtmlUtil.SIZE_60)));
        sb.append(HtmlUtil.formEntry(msgLabel("Description template"),
                                     HtmlUtil.input(ATTR_DESCTEMPLATE,
                                         descTemplate, HtmlUtil.SIZE_60)));

        if (baseGroupName.length() > 0) {
            sb.append(HtmlUtil.formEntry(msgLabel("Base group"),
                                         HtmlUtil.input(ATTR_BASEGROUP,
                                             baseGroupName,
                                             HtmlUtil.SIZE_60)));
        }
        sb.append(HtmlUtil.formEntry(msgLabel("Group template"),
                                     HtmlUtil.input(ATTR_GROUPTEMPLATE,
                                         groupTemplate, HtmlUtil.SIZE_60)));

        sb.append(HtmlUtil.formEntry(msgLabel("Date format"),
                                     HtmlUtil.input(ATTR_DATEFORMAT,
                                         dateFormat, HtmlUtil.SIZE_30)));


        sb.append(HtmlUtil.formEntry(msgLabel("Move file to storage"),
                                     HtmlUtil.checkbox(ATTR_MOVETOSTORAGE,
                                         "true", moveToStorage)));

        sb.append(HtmlUtil.formEntry(msgLabel("Add Metadata"),
                                     HtmlUtil.checkbox(ATTR_ADDMETADATA,
                                         "true", getAddMetadata())));

    }




    /**
     * _more_
     *
     * @return _more_
     */
    private SimpleDateFormat getSDF() {
        if (sdf == null) {
            if ((dateFormat != null) && (dateFormat.length() > 0)) {
                sdf = new SimpleDateFormat(dateFormat);
            } else {
                sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
            }
            sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        }
        return sdf;

    }


    /**
     * _more_
     */
    private void init() {
        if ((filePattern == null) && (filePatternString != null)
                && (filePatternString.length() > 0)) {
            String       tmp     = filePatternString;
            StringBuffer pattern = new StringBuffer();
            patternNames = new ArrayList<String>();
            while (true) {
                int idx1 = tmp.indexOf("(");
                if (idx1 < 0) {
                    pattern.append(tmp);
                    break;
                }
                int idx2 = tmp.indexOf(":");
                if (idx2 < 0) {
                    throw new IllegalArgumentException("bad pattern:"
                            + filePatternString);
                }
                pattern.append(tmp.substring(0, idx1 + 1));
                String name = tmp.substring(idx1 + 1, idx2);
                patternNames.add(name);
                tmp = tmp.substring(idx2 + 1);
            }
            filePattern = Pattern.compile(pattern.toString());
            //            System.err.println("pattern:" + this + "  " + filePatternString);
            //            System.err.println("pattern names:" + patternNames);
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
        String error = getError();
        if (error != null) {
            return super.getExtraInfo();
        }
        String dirMsg = "";
        if (dirs != null) {
            if (dirs.size() == 0) {
                dirMsg = "No directories found<br>";
            } else {
                dirMsg = "Scanning:" + dirs.size() + " directories<br>";
            }
        }

        String entryMsg = "";
        if (entryCnt > 0) {
            entryMsg = "Found " + entryCnt + " file" + ((entryCnt == 1)
                    ? ""
                    : "s") + "<br>" + "Found " + newEntryCnt + " new file"
                           + ((newEntryCnt == 1)
                              ? ""
                              : "s") + "<br>";

        }
        return "Directory:" + rootDir + "<br>" + dirMsg + entryMsg + status;
    }

    /**
     * _more_
     *
     * @param dir _more_
     */
    private void removeDir(FileInfo dir) {
        dirs.remove(dir);
        dirMap.remove(dir.getFile());
    }

    /**
     * _more_
     *
     * @param dir _more_
     *
     * @return _more_
     */
    private FileInfo addDir(File dir) {
        FileInfo fileInfo = new FileInfo(dir, true);
        dirs.add(fileInfo);
        dirMap.put(dir, dir);
        return fileInfo;
    }

    /**
     * _more_
     *
     * @param dir _more_
     *
     * @return _more_
     */
    private boolean hasDir(File dir) {
        return dirMap.get(dir) != null;
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void runInner() throws Exception {
        if ( !getActive()) {
            return;
        }
        entryCnt    = 0;
        newEntryCnt = 0;
        status = new StringBuffer("Looking for initial directory listing");
        long tt1 = System.currentTimeMillis();
        dirs = new ArrayList<FileInfo>();
        dirs.add(new FileInfo(rootDir));
        dirs.addAll(FileInfo.collectDirs(rootDir));
        long tt2 = System.currentTimeMillis();
        status = new StringBuffer("");
        //        System.err.println("took:" + (tt2 - tt1) + " to find initial dirs:"
        //                           + dirs.size());

        for (FileInfo dir : dirs) {
            dirMap.put(dir.getFile(), dir);
        }

        int cnt = 0;
        while (getActive()) {
            long t1 = System.currentTimeMillis();
            collectEntries((cnt == 0));
            long t2 = System.currentTimeMillis();
            cnt++;
            //            System.err.println("found:" + entries.size() + " files in:"
            //                               + (t2 - t1) + "ms");
            if ( !getMonitor()) {
                status.append("Done<br>");
                break;
            }

            status.append("Done... sleeping for " + getSleepMinutes()
                          + " minutes<br>");
            Misc.sleep((long) (getSleepMinutes() * 60 * 1000));
            status = new StringBuffer();
        }
    }





    /**
     * _more_
     *
     * @param firstTime _more_
     *
     *
     * @throws Exception _more_
     */
    public void collectEntries(boolean firstTime) throws Exception {

        long           t1        = System.currentTimeMillis();
        List<Entry>    entries   = new ArrayList<Entry>();
        List<Entry>    needToAdd = new ArrayList<Entry>();
        List<FileInfo> tmpDirs   = new ArrayList<FileInfo>(dirs);
        entryCnt    = 0;
        newEntryCnt = 0;
        for (int dirIdx = 0; dirIdx < tmpDirs.size(); dirIdx++) {
            FileInfo fileInfo = tmpDirs.get(dirIdx);
            if ( !fileInfo.exists()) {
                removeDir(fileInfo);
                continue;
            }
            if ( !firstTime && !fileInfo.hasChanged()) {
                continue;
            }
            File[] files = fileInfo.getFile().listFiles();
            if (files == null) {
                continue;

            }

            for (int fileIdx = 0; fileIdx < files.length; fileIdx++) {
                File f = files[fileIdx];
                if (f.isDirectory()) {
                    //If this is a directory then check if we already have it 
                    //in the list. If not then add it to the main list and the local list
                    if ( !hasDir(f)) {
                        FileInfo newFileInfo = addDir(f);
                        tmpDirs.add(newFileInfo);
                    }
                    continue;
                }
                Entry entry = processFile(f);
                if (entry != null) {
                    entries.add(entry);
                }

                entryCnt++;
                if (entries.size() > 1000) {
                    List uniqueEntries = repository.getUniqueEntries(entries);
                    newEntryCnt += uniqueEntries.size();
                    needToAdd.addAll(uniqueEntries);
                    entries = new ArrayList();
                }
                if (needToAdd.size() > 1000) {
                    if (getAddMetadata()) {
                        getRepository().addInitialMetadata(null, needToAdd);
                    }
                    repository.insertEntries(needToAdd, true, true);
                    needToAdd = new ArrayList<Entry>();
                }

                //                if(true) break;
                if ( !getActive()) {
                    return;
                }
            }
        }

        needToAdd.addAll(repository.getUniqueEntries(entries));
        if (needToAdd.size() > 0) {
            if (getAddMetadata()) {
                getRepository().addInitialMetadata(null, needToAdd);
            }
            repository.insertEntries(needToAdd, true, true);
        }
    }



    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry processFile(File f) throws Exception {

        //check if its a hidden file
        if (f.getName().startsWith(".")) {
            return null;
        }

        String fileName = f.toString();
        fileName = fileName.replace("\\", "/");
        String dirPath    = f.getParent().toString();
        int    rootStrLen = rootDir.toString().length();
        //        System.err.println("root:" + rootDir + " " + rootStrLen);
        dirPath = dirPath.substring(rootStrLen);
        dirPath = SqlUtil.cleanUp(dirPath);
        dirPath = dirPath.replace("\\", "/");


        init();

        Matcher matcher = filePattern.matcher(fileName);
        //        System.err.println("file:" + fileName + " " +matcher.find());
        if ( !matcher.find()) {
            return null;
        }


        Hashtable map       = new Hashtable();
        Date      fromDate  = null;
        Date      toDate    = null;
        String    tag       = tagTemplate;
        String    groupName = groupTemplate;
        String    name      = nameTemplate;
        String    desc      = descTemplate;


        //        System.err.println("pattern names:" + patternNames);
        for (int dataIdx = 0; dataIdx < patternNames.size(); dataIdx++) {
            String dataName = patternNames.get(dataIdx);
            Object value    = matcher.group(dataIdx + 1);
            if (dataName.equals("fromdate")) {
                value = fromDate = getSDF().parse((String) value);
            } else if (dataName.equals("todate")) {
                value = toDate = getSDF().parse((String) value);
            } else {
                value = typeHandler.convert(dataName, (String) value);
                groupName = groupName.replace("${" + dataName + "}",
                        value.toString());
                name = name.replace("${" + dataName + "}", value.toString());
                desc = desc.replace("${" + dataName + "}", value.toString());
                map.put(dataName, value);
            }
        }
        //        System.err.println("values:");
        //        System.err.println("map:" + map);
        Object[] values = typeHandler.makeValues(map);
        //        for(int i=0;i<values.length;i++) {
        //            System.err.println("   value[" + i +"] = " + values[i]);
        //        }

        //        System.err.println(fileName + " " + toDate);
        Date createDate = new Date();
        if (fromDate == null) {
            fromDate = toDate;
        }
        if (toDate == null) {
            toDate = fromDate;
        }
        if (fromDate == null) {
            fromDate = createDate;
        }
        if (toDate == null) {
            toDate = createDate;
        }


        List   dirToks  = StringUtil.split(dirPath, "/", true, true);
        String dirGroup = StringUtil.join("/", dirToks);


        String ext      = IOUtil.getFileExtension(fileName);
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        tag       = tag.replace("${extension}", ext);

        groupName = groupName.replace("${dirgroup}", dirGroup);

        groupName = groupName.replace("${fromdate}",
                                      getRepository().formatDate(fromDate));
        groupName = groupName.replace("${todate}",
                                      getRepository().formatDate(toDate));

        name = name.replace("${filename}", f.getName());
        name = name.replace("${fromdate}",
                            getRepository().formatDate(fromDate));

        name = name.replace("${todate}", getRepository().formatDate(toDate));

        desc = desc.replace("${fromdate}",
                            getRepository().formatDate(fromDate));
        desc = desc.replace("${todate}", getRepository().formatDate(toDate));
        desc = desc.replace("${name}", name);


        if ((baseGroupName != null) && (baseGroupName.length() > 0)) {
            groupName = baseGroupName + "/" + groupName;
        }
        Group group = repository.findGroupFromName(groupName, getUser(),
                          true);
        Entry    entry = typeHandler.createEntry(repository.getGUID());
        Resource resource;
        if (moveToStorage) {
            File newFile = getStorageManager().moveToStorage(null,
                               new File(fileName),
                               getRepository().getGUID() + "_");
            resource = new Resource(newFile.toString(),
                                    Resource.TYPE_STOREDFILE);
        } else {
            resource = new Resource(fileName, Resource.TYPE_FILE);
        }
        entry.initEntry(name, desc, group, group.getCollectionGroupId(),
                        getUser(), resource, "", createDate.getTime(),
                        fromDate.getTime(), toDate.getTime(), values);
        if (tag.length() > 0) {
            List tags = StringUtil.split(tag, ",", true, true);
            for (int i = 0; i < tags.size(); i++) {
                entry.addMetadata(new Metadata(repository.getGUID(),
                        entry.getId(), EnumeratedMetadataHandler.TYPE_TAG,
                        DFLT_INHERITED, (String) tags.get(i), "", "", ""));
            }

        }
        typeHandler.initializeNewEntry(entry);


        return entry;

    }

    /**
     * _more_
     *
     * @param type _more_
     * @param filepath _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry processFile(TypeHandler type, String filepath)
            throws Exception {
        if ( !this.typeHandler.equals(type)) {
            return null;
        }
        return processFile(new File(filepath));
    }




}

