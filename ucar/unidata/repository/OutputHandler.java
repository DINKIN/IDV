/**
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


import ucar.unidata.repository.collab.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.WikiUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;



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

import java.util.zip.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class OutputHandler extends RepositoryManager implements WikiUtil
    .WikiPageHandler {

    public static final String LABEL_LINKS = "View &amp; Edit";

    /** _more_ */
    public static final OutputType OUTPUT_HTML = new OutputType("Entry",
                                                     "default.html",
                                                     OutputType.TYPE_HTML,
                                                     "", ICON_INFORMATION);


    /** _more_ */
    private String name;

    /** _more_ */
    private List<OutputType> types = new ArrayList<OutputType>();


    /**
     * _more_
     *
     * @param repository _more_
     * @param name _more_
     *
     * @throws Exception _more_
     */
    public OutputHandler(Repository repository, String name)
            throws Exception {
        super(repository);
        this.name = name;
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public OutputType findOutputType(String id) {
        int idx = types.indexOf(new OutputType(id, OutputType.TYPE_HTML));
        if (idx >= 0) {
            return types.get(idx);
        }
        return null;
    }


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public OutputHandler(Repository repository, Element element)
            throws Exception {
        this(repository,
             XmlUtil.getAttribute(element, ATTR_NAME, (String) null));
    }

    /**
     * _more_
     */
    public void init() {}


    /**
     * _more_
     */
    public void clearCache() {}

    /**
     * _more_
     *
     * @param type _more_
     */
    protected void addType(OutputType type) {
        type.setGroupName(name);
        types.add(type);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<OutputType> getTypes() {
        return types;
    }


    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }


    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        if (name == null) {
            name = Misc.getClassName(getClass());
        }
        return name;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param subGroups _more_
     * @param entries _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    public void showNext(Request request, List<Group> subGroups,
                         List<Entry> entries, StringBuffer sb)
            throws Exception {
        int cnt = subGroups.size() + entries.size();
        int max = request.get(ARG_MAX, Repository.MAX_ROWS);
        //        System.err.println ("cnt:" + cnt + " " + max);

        if ((cnt > 0) && ((cnt == max) || request.defined(ARG_SKIP))) {
            int skip = Math.max(0, request.get(ARG_SKIP, 0));
            sb.append(msgLabel("Results") + (skip + 1) + "-" + (skip + cnt));
            sb.append(HtmlUtil.space(4));
            if (skip > 0) {
                sb.append(HtmlUtil.href(request.getUrl(ARG_SKIP) + "&"
                                        + ARG_SKIP + "="
                                        + (skip - max), msg("Previous")));
                sb.append(HtmlUtil.space(1));
            }
            if (cnt >= max) {
                sb.append(HtmlUtil.href(request.getUrl(ARG_SKIP) + "&"
                                        + ARG_SKIP + "="
                                        + (skip + max), msg("Next")));
            }
        }

    }




    /**
     * _more_
     *
     * @param request _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public boolean canHandleOutput(OutputType output) {
        for (OutputType type : types) {
            if (type.equals(output)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Class State _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class State {

        /** _more_ */
        public static final int FOR_UNKNOWN = 0;

        /** _more_ */
        public static final int FOR_HEADER = 1;

        /** _more_ */
        public int forWhat = FOR_UNKNOWN;

        /** _more_ */
        public Entry entry;

        /** _more_ */
        public Group group;

        /** _more_ */
        public List<Group> subGroups;

        /** _more_ */
        public List<Entry> entries;

        /** _more_ */
        public List<Entry> allEntries;

        /**
         * _more_
         *
         * @param entry _more_
         */
        public State(Entry entry) {
            if (entry != null) {
                if (entry.isGroup()) {
                    group          = (Group) entry;
                    this.subGroups = group.getSubGroups();
                    this.entries   = group.getSubEntries();
                } else {
                    this.entry = entry;
                }
            }

        }

        /**
         * _more_
         *
         * @param group _more_
         * @param subGroups _more_
         * @param entries _more_
         */
        public State(Group group, List<Group> subGroups,
                     List<Entry> entries) {
            this.group     = group;
            this.entries   = entries;
            this.subGroups = subGroups;
        }


        /**
         * _more_
         *
         *
         * @param group _more_
         * @param entries _more_
         */
        public State(Group group, List<Entry> entries) {
            this.group   = group;
            this.entries = entries;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isDummyGroup() {
            Entry entry = getEntry();
            if (entry == null) {
                return false;
            }
            if ( !entry.isGroup()) {
                return false;
            }
            return entry.isDummy();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean forHeader() {
            return forWhat == FOR_HEADER;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public List<Entry> getAllEntries() {
            if (allEntries == null) {
                allEntries = new ArrayList();
                if (subGroups != null) {
                    allEntries.addAll(subGroups);
                }
                if (entries != null) {
                    allEntries.addAll(entries);
                }
                if (entry != null) {
                    allEntries.add(entry);
                }
            }
            return (List<Entry>) allEntries;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Entry getEntry() {
            if (entry != null) {
                return entry;
            }
            return group;
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param title _more_
     * @param sb _more_
     * @param state _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result makeLinksResult(Request request, String title,
                                     StringBuffer sb, State state)
            throws Exception {
        Result result = new Result(title, sb);
        addLinks(request, result, state);
        return result;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param result _more_
     * @param state _more_
     *
     * @throws Exception _more_
     */
    protected void addLinks(Request request, Result result, State state)
            throws Exception {
        state.forWhat = State.FOR_HEADER;
        if (state.getEntry().getDescription().indexOf("<nolinks>") >= 0) {
            return;
        }
        /*        result.putProperty(
            PROP_NAVSUBLINKS,
            getHeader(
                request, request.getOutput(),
                getRepository().getLinksForHeader(request, state)));
        */
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param state _more_
     * @param links _more_
     * @param forHeader _more_
     *
     * @throws Exception _more_
     */
    protected void getEntryLinks(Request request, State state,
                                 List<Link> links)
            throws Exception {}



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param outputType _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Link makeLink(Request request, Entry entry,
                            OutputType outputType)
            throws Exception {
        return makeLink(request, entry, outputType, "");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param outputType _more_
     * @param suffix _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Link makeLink(Request request, Entry entry,
                            OutputType outputType, String suffix)
            throws Exception {
        String url;
        if (entry == null) {
            url = HtmlUtil.url(getRepository().URL_ENTRY_SHOW + suffix,
                               ARG_OUTPUT, outputType.toString());
        } else {
            url = request.getEntryUrl(getRepository().URL_ENTRY_SHOW
                                      + suffix, entry);
            url = HtmlUtil.url(url, ARG_ENTRYID, entry.getId(), ARG_OUTPUT,
                               outputType.toString());
        }
        int linkType = OutputType.TYPE_ACTION;
        return new Link(url, (outputType.getIcon() == null)
                             ? null
                             : iconUrl(outputType.getIcon()), outputType
                                 .getLabel(), outputType);

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param links _more_
     * @param type _more_
     *
     * @throws Exception _more_
     */
    protected void addOutputLink(Request request, Entry entry,
                                 List<Link> links, OutputType type)
            throws Exception {
        links.add(new Link(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                            entry, ARG_OUTPUT,
                                            type), iconUrl(type.getIcon()),
                                                type.getLabel(), type));

    }

    /**
     * _more_
     *
     *
     * @param method _more_
     * @return _more_
     */
    private Result notImplemented(String method) {
        throw new IllegalArgumentException("Method: " + method
                                           + " not implemented");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, Entry entry) throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        return outputGroup(request, getEntryManager().getDummyGroup(),
                           new ArrayList<Group>(), entries);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, Group group,
                              List<Group> subGroups, List<Entry> entries)
            throws Exception {
        return notImplemented("outputGroup");
    }




    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        return null;
    }






    /**
     * _more_
     *
     * @param request _more_
     * @param elementId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getGroupSelect(Request request, String elementId)
            throws Exception {
        return getSelect(request, elementId, "Select", false, "");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param elementId _more_
     * @param label _more_
     * @param allEntries _more_
     * @param append _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getSelect(Request request, String elementId,
                                   String label, boolean allEntries,
                                   String type)
            throws Exception {
        String event = HtmlUtil.call("selectInitialClick",
                                     "event," + HtmlUtil.squote(elementId)
                                     + "," + HtmlUtil.squote("" + allEntries)
                                     + "," + HtmlUtil.squote(type));
        return HtmlUtil.mouseClickHref(event, msg(label),
                                       HtmlUtil.id(elementId
                                           + ".selectlink"));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param entry _more_
     * @param target _more_
     * @param allEntries _more_
     * @param selectType _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getSelectLink(Request request, Entry entry, String target) 
        throws Exception {
        String       linkText = entry.getLabel();
        StringBuffer sb       = new StringBuffer();
        String       entryId  = entry.getId();
        String       icon     = getEntryManager().getIconUrl(request, entry);
        String       event;
        String       uid = "link_" + HtmlUtil.blockCnt++;
        String folderClickUrl = 
            request.entryUrl(getRepository().URL_ENTRY_SHOW,entry)+
            "&" + HtmlUtil.args(new String[]{
                ARG_OUTPUT, request.getString(ARG_OUTPUT,"selectxml"),
                ATTR_TARGET, target,
                ARG_ALLENTRIES, request.getString(ARG_ALLENTRIES,"true"),
                ARG_SELECTTYPE,request.getString(ARG_SELECTTYPE,"")});

        event = HtmlUtil.onMouseClick(HtmlUtil.call("folderClick",
                                                    HtmlUtil.squote(uid)
                                                    + "," +
                                                    HtmlUtil.squote(folderClickUrl)));
        String img = HtmlUtil.img(icon, (entry.isGroup()
                                         ? "Click to open group; "
                                         : ""), HtmlUtil.id("img_" + uid)
                                             + event);
        sb.append(img);
        sb.append(HtmlUtil.space(1));

        String type      = request.getString(ARG_SELECTTYPE, "");
        String elementId = entry.getId();
        String value     = (entry.isGroup()
                            ? ((Group) entry).getFullName()
                            : entry.getName());
        sb.append(HtmlUtil.mouseClickHref(HtmlUtil.call("selectClick",
                                                        HtmlUtil.comma(
                                                            HtmlUtil.squote(target),
                                                            HtmlUtil.squote(entry.getId()),
                                                            HtmlUtil.squote(value),
                                                            HtmlUtil.squote(type))), linkText));

        sb.append(HtmlUtil.br());
        sb.append(HtmlUtil.div("",
                               HtmlUtil.attrs(HtmlUtil.ATTR_STYLE,
                                   "display:none;visibility:hidden",
                                   HtmlUtil.ATTR_CLASS, "folderblock",
                                   HtmlUtil.ATTR_ID, uid)));
        return sb.toString();
    }


    public Result makeAjaxResult(Request request, String contents) {
        StringBuffer xml = new StringBuffer("<content>\n");
        XmlUtil.appendCdata(xml,contents);
        xml.append("\n</content>");
        return new Result("", xml, "text/xml");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param output _more_
     *
     * @return _more_
     */
    public List<Link> getNextPrevLinks(Request request, Entry entry,
                                       OutputType output) {
        Link       link;
        List<Link> links = new ArrayList<Link>();

        link = new Link(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                         entry, ARG_OUTPUT, output,
                                         ARG_PREVIOUS,
                                         "true"), iconUrl(ICON_LEFT),
                                             msg("View Previous Entry"));

        link.setLinkType(OutputType.TYPE_NONHTML);
        links.add(link);
        link = new Link(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                         entry, ARG_OUTPUT, output, ARG_NEXT,
                                         "true"), iconUrl(ICON_RIGHT),
                                             msg("View Next Entry"));
        link.setLinkType(OutputType.TYPE_NONHTML);
        links.add(link);
        return links;
    }



    /**
     * _more_
     *
     * @param buffer _more_
     */
    public void addToSettingsForm(StringBuffer buffer) {}

    /**
     * _more_
     *
     * @param request _more_
     *
     * @throws Exception _more_
     */
    public void applySettings(Request request) throws Exception {}


    /** _more_ */
    public static int entryCnt = 0;



    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getEntryFormStart(Request request, List entries)
            throws Exception {
        String       base   = "toggleentry" + (entryCnt++);
        StringBuffer formSB = new StringBuffer();

        formSB.append(request.formPost(getRepository().URL_ENTRY_GETENTRIES,
                                       "getentries"));

        List<Link> links = getRepository().getOutputLinks(request,
                               new State(getEntryManager().getDummyGroup(),
                                         entries));

        List<TwoFacedObject> tfos = new ArrayList<TwoFacedObject>();
        for (Link link : links) {
            OutputType outputType = link.getOutputType();
            if (outputType == null) {
                continue;
            }
            tfos.add(new TwoFacedObject(outputType.getLabel(),
                                        outputType.getId()));
        }
        StringBuffer selectSB = new StringBuffer();
        selectSB.append(HtmlUtil.space(4));
        selectSB.append(msgLabel("View As"));
        selectSB.append(HtmlUtil.select(ARG_OUTPUT, tfos));
        selectSB.append(HtmlUtil.submit(msg("Selected"), "getselected"));
        selectSB.append(HtmlUtil.submit(msg("All"), "getall"));

        String arrowImg =
            HtmlUtil.img(getRepository().iconUrl(ICON_DOWNARROW),
                         msg("Show/Hide Form"), HtmlUtil.id(base + "img"));
        String link = HtmlUtil.space(2)
                      + HtmlUtil.jsLink(HtmlUtil.onMouseClick(base
                          + ".groupToggleVisibility()"), arrowImg);
        formSB.append(HtmlUtil.span(selectSB.toString(),
                                    HtmlUtil.id(base + "select")));
        formSB.append(
            HtmlUtil.script(
                base + " = new VisibilityGroup("
                + HtmlUtil.squote(base + "img") + ");\n"
                + HtmlUtil.call(
                    base + ".groupAddEntry",
                    HtmlUtil.squote(base + "select"))));
        return new String[] { link, base, formSB.toString() };
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param base _more_
     *
     * @return _more_
     */
    public String getEntryFormEnd(Request request, String base) {
        StringBuffer sb = new StringBuffer();
        sb.append(HtmlUtil.formClose());
        sb.append(HtmlUtil.script(base + ".groupToggleVisibility();"));
        return sb.toString();
    }



    /**
     * _more_
     *
     * @param sb _more_
     * @param entries _more_
     * @param request _more_
     * @param doForm _more_
     * @param dfltSelected _more_
     * @param showCrumbs _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public String getEntriesList(StringBuffer sb, List entries,
                                 Request request, boolean doForm,
                                 boolean dfltSelected, boolean showCrumbs)
            throws Exception {

        String link = "";
        String base = "";
        if (doForm) {
            String[] tuple = getEntryFormStart(request, entries);
            link = tuple[0];
            base = tuple[1];
            sb.append(tuple[2]);
        }
        sb.append(HtmlUtil.open(HtmlUtil.TAG_DIV, HtmlUtil.cssClass("folderblock")));
        int          cnt  = 0;
        StringBuffer jsSB = new StringBuffer();
        for (Entry entry : (List<Entry>) entries) {
            StringBuffer cbxSB = new StringBuffer();
            String rowId = base + (cnt++);
            String cbxId = "entry_" + entry.getId();
            String cbxWrapperId = base + (cnt++);
            jsSB.append(HtmlUtil.callln("addEntryRow",HtmlUtil.comma(HtmlUtil.squote(rowId),HtmlUtil.squote(cbxId),HtmlUtil.squote(cbxWrapperId),base)));
            if (doForm) {
                cbxSB.append(HtmlUtil.hidden("all_" + entry.getId(), "1"));
                String cbx = HtmlUtil.checkbox(cbxId,
                                               "true", dfltSelected,HtmlUtil.id(cbxId)+" " +
                                               HtmlUtil.attr(HtmlUtil.ATTR_TITLE,msg("Shift-click: select range; Control-click: toggle all"))+
                                               HtmlUtil.attr(HtmlUtil.ATTR_ONCLICK,HtmlUtil.call("entryRowCheckboxClicked",
                                                                                                 HtmlUtil.comma("event",HtmlUtil.squote(rowId)))));
                

                cbxSB.append(HtmlUtil.span(cbx, HtmlUtil.id(cbxWrapperId)));
            }

            if (showCrumbs) {
                cbxSB.append(HtmlUtil.img(getEntryManager().getIconUrl(request,entry)));
                cbxSB.append(HtmlUtil.space(1));
                cbxSB.append(getEntryManager().getBreadCrumbs(request, entry));
                sb.append(cbxSB);
                sb.append(HtmlUtil.br());
            } else {
                EntryLink entryLink  = getEntryManager().getAjaxLink(request, entry, entry.getLabel());
                entryLink.setLink(cbxSB + entryLink.getLink());
                decorateEntryRow(request, entry, sb,entryLink,rowId);
            }
        }
        sb.append(HtmlUtil.close(HtmlUtil.TAG_UL));
        sb.append("\n\n");
        sb.append(HtmlUtil.script(jsSB.toString()));
        sb.append("\n\n");
        if (doForm) {
            sb.append(getEntryFormEnd(request, base));
        }
        return link;
    }


    protected void decorateEntryRow(Request request, Entry entry, StringBuffer sb, EntryLink link,String rowId) {
        if(rowId==null) {
            rowId = "entryrow_" + (HtmlUtil.blockCnt++);
        }
        sb.append(HtmlUtil.open(HtmlUtil.TAG_DIV,HtmlUtil.id(rowId) +HtmlUtil.cssClass("entryrow")+ 
                                HtmlUtil.onMouseOver(HtmlUtil.call("entryRowOver",HtmlUtil.squote(rowId))) +
                                HtmlUtil.onMouseOut(HtmlUtil.call("entryRowOut",HtmlUtil.squote(rowId)))));
        sb.append("<table border=\"0\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\"><tr ><td>");
        sb.append(link.getLink());
        sb.append("</td><td align=right class=entryrowlabel>");
        if(entry.getResource().isFile()) {
            File   f    = entry.getResource().getFile();
            //            sb.append(formatFileLength(f.length()));
        }
        sb.append(getRepository().formatDateShort(request, new Date(entry.getStartDate())));
        String userLabel;
        if(Misc.equals(request.getUser(), entry.getUser())) {
            userLabel="me";
        } else {
            userLabel=entry.getUser().getId();
        }
        sb.append("</td><td width=\"1%\" align=right class=entryrowlabel>");
        sb.append(HtmlUtil.space(1));
            String userSearchLink =
                HtmlUtil.href(
                    HtmlUtil.url(
                        request.url(getRepository().URL_USER_PROFILE),
                        ARG_USER_ID,
                        entry.getUser().getId()), userLabel,
                            "title=\"View user profile\"");

        sb.append(userSearchLink);
        sb.append("</td></tr></table>");
        sb.append(HtmlUtil.close(HtmlUtil.TAG_DIV));
        sb.append(link.getFolderBlock());

    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getEntryLink(Request request, Entry entry)
            throws Exception {
        return getEntryManager().getTooltipLink(request, entry,
                                                entry.getLabel(), null);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param output _more_
     * @param outputTypes _more_
     * @param links _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getHeader(Request request, OutputType output,
                             List<Link> links)
            throws Exception {

        List   items          = new ArrayList();
        Object initialMessage = request.remove(ARG_MESSAGE);
        String onLinkTemplate = getRepository().getTemplateProperty(request,
                                    "ramadda.template.sublink.on", "");
        String offLinkTemplate = getRepository().getTemplateProperty(request,
                                     "ramadda.template.sublink.off", "");
        for (Link link : links) {
            OutputType outputType = link.getOutputType();
            String     url        = link.getUrl();
            String     template;
            if (Misc.equals(outputType, output)) {
                template = onLinkTemplate;
            } else {
                template = offLinkTemplate;
            }
            String html = template.replace("${label}", link.getLabel());
            html = html.replace("${url}", url);
            html = html.replace("${root}", getRepository().getUrlBase());
            items.add(html);
        }
        if (initialMessage != null) {
            request.put(ARG_MESSAGE, initialMessage);
        }
        return items;
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param typeHandlers _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listTypes(Request request,
                               List<TypeHandler> typeHandlers)
            throws Exception {
        return notImplemented("listTypes");
    }





    /**
     * protected Result listTags(Request request, List<Tag> tags)
     *       throws Exception {
     *   return notImplemented("listTags");
     * }
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listAssociations(Request request) throws Exception {
        return notImplemented("listAssociations");
    }





    /** _more_ */
    public static final String RESOURCE_ENTRYTEMPLATE = "entrytemplate.txt";

    /** _more_ */
    public static final String RESOURCE_GROUPTEMPLATE = "grouptemplate.txt";


    /** _more_ */
    public static final String PROP_ENTRY = "entry";

    /** _more_ */
    public static final String PROP_REQUEST = "request";

    /** _more_ */
    public static final String WIKIPROP_IMPORT = "import";

    /** _more_          */
    public static final String WIKIPROP_COMMENTS = "comments";

    /** _more_ */
    public static final String WIKIPROP_TOOLBAR = "toolbar";

    /** _more_ */
    public static final String WIKIPROP_BREADCRUMBS = "breadcrumbs";

    /** _more_ */
    public static final String WIKIPROP_INFORMATION = "information";

    /** _more_ */
    public static final String WIKIPROP_IMAGE = "image";

    /** _more_ */
    public static final String WIKIPROP_NAME = "name";

    /** _more_ */
    public static final String WIKIPROP_DESCRIPTION = "description";

    /** _more_ */
    public static final String WIKIPROP_LINKS = "links";

    /** _more_ */
    public static final String WIKIPROP_ = "";

    /** _more_ */
    public static final String WIKIPROP_CHILDREN_GROUPS = "subgroups";

    /** _more_ */
    public static final String WIKIPROP_CHILDREN_ENTRIES = "subentries";

    /** _more_ */
    public static final String WIKIPROP_CHILDREN = "children";

    //        WIKIPROP_IMPORT = "import";

    /** _more_ */
    public static final String[] WIKIPROPS = {
        WIKIPROP_INFORMATION, WIKIPROP_NAME, WIKIPROP_DESCRIPTION,
        WIKIPROP_COMMENTS, WIKIPROP_BREADCRUMBS, WIKIPROP_TOOLBAR,
        WIKIPROP_IMAGE, WIKIPROP_LINKS  /*,
                          WIKIPROP_CHILDREN_GROUPS,
                          WIKIPROP_CHILDREN_ENTRIES,
                          WIKIPROP_CHILDREN*/
    };




    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param property _more_
     *
     * @return _more_
     */
    public String getWikiPropertyValue(WikiUtil wikiUtil, String property) {
        try {
            /*
              {{type name="value" ...}}
              {{import "entry identifier" type name="value"}}
             */
            Entry   entry   = (Entry) wikiUtil.getProperty(PROP_ENTRY);
            Request request = (Request) wikiUtil.getProperty(PROP_REQUEST);
            //Check for infinite loop
            property = property.trim();
            if (property.length() == 0) {
                return "";
            }
            if (request.getExtraProperty(property) != null) {
                return "<b>Detected circular wiki import:" + property
                       + "</b>";
            }
            request.putExtraProperty(property, property);

            List<String> toks      = StringUtil.splitUpTo(property, " ", 2);
            String       tag       = toks.get(0);
            String       remainder = "";
            if (toks.size() > 1) {
                remainder = toks.get(1);
            }
            Entry theEntry = entry;
            if (tag.equals(WIKIPROP_IMPORT)) {
                toks = StringUtil.splitUpTo(remainder, " ", 3);
                if (toks.size() < 2) {
                    return "<b>Incorrect import specification:" + property
                           + "</b>";
                }
                String id = toks.get(0).trim();
                tag = toks.get(1).trim();
                if (toks.size() == 3) {
                    remainder = toks.get(2);
                } else {
                    remainder = "";
                }
                theEntry = findWikiEntry(request, wikiUtil, id, entry);
                if (theEntry == null) {
                    return "<b>Could not find entry&lt;" + id + "&gt;</b>";
                }
            }
            Hashtable props = new Hashtable();
            props = StringUtil.parseHtmlProperties(remainder);
            addWikiLink(wikiUtil, theEntry);
            String include = handleWikiImport(wikiUtil, request, theEntry,
                                 tag, props);
            if (include != null) {
                return include;
            }
            return wikiUtil.getPropertyValue(property);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String getImageUrl(Request request, Entry entry) {
        if ( !entry.getResource().isImage()) {
            if (true) {
                return null;
            }
            if (entry.hasAreaDefined()) {
                return request.url(repository.URL_GETMAP, ARG_SOUTH,
                                   "" + entry.getSouth(), ARG_WEST,
                                   "" + entry.getWest(), ARG_NORTH,
                                   "" + entry.getNorth(), ARG_EAST,
                                   "" + entry.getEast());
            }
            return null;
        }

        return HtmlUtil.url(request.url(repository.URL_ENTRY_GET) + "/"
                            + entry.getName(), ARG_ENTRYID, entry.getId());
    }


    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param entry _more_
     * @param include _more_
     * @param props _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getWikiInclude(WikiUtil wikiUtil, Request request,
                                 Entry entry, String include, Hashtable props)
            throws Exception {

        boolean open         = Misc.getProperty(props, "open", true);
        boolean inBlock      = Misc.getProperty(props, "showhide", true);
        String  blockContent = null;
        String  blockTitle   = "";
        boolean doBG         = true;

        if (include.equals(WIKIPROP_INFORMATION)) {
            blockContent =
                getRepository().getHtmlOutputHandler().getInformationTabs(
                    request, entry, true);
            blockTitle = Misc.getProperty(props, "title", msg("Information"));
        } else if (include.equals(WIKIPROP_IMAGE)) {
            if ( !entry.getResource().isImage()) {
                return msg("Not an image");
            }
            return HtmlUtil.img(getImageUrl(request, entry), entry.getName());
        } else if (include.equals(WIKIPROP_LINKS)) {
            blockTitle = Misc.getProperty(props, "title", msg(LABEL_LINKS));
            blockContent = getEntryManager().getEntryActionsTable(request,
                    entry, OutputType.TYPE_ALL);
        } else if (include.equals(WIKIPROP_COMMENTS)) {
            return getCommentBlock(request, entry).toString();
        } else if (include.equals(WIKIPROP_TOOLBAR)) {
            return getEntryManager().getEntryToolbar(request, entry, false);
        } else if (include.equals(WIKIPROP_BREADCRUMBS)) {
            return getEntryManager().getBreadCrumbs(request, entry);
        } else if (include.equals(WIKIPROP_DESCRIPTION)) {
            return entry.getDescription();
        } else if (include.equals(WIKIPROP_NAME)) {
            return entry.getName();
        } else if (include.equals(WIKIPROP_CHILDREN_GROUPS)) {
            doBG = false;
            List<Entry> children =
                (List<Entry>) wikiUtil.getProperty(entry.getId()
                    + "_subgroups");
            if (children == null) {
                children = getEntryManager().getChildrenGroups(request,
                        entry);
            }
            if (children.size() == 0) {
                return "";
            }
            StringBuffer sb = new StringBuffer();
            String link = getEntriesList(sb, children, request, true, false,
                                         false);
            blockContent = sb.toString();
            blockTitle = Misc.getProperty(props, "title", msg("Groups"))
                         + link;
        } else if (include.equals(WIKIPROP_CHILDREN_ENTRIES)) {
            doBG = false;
            List<Entry> children =
                (List<Entry>) wikiUtil.getProperty(entry.getId()
                    + "_subentries");
            if (children == null) {
                children = getEntryManager().getChildrenEntries(request,
                        entry);
            }
            if (children.size() == 0) {
                return "";
            }

            StringBuffer sb = new StringBuffer();
            String link = getEntriesList(sb, children, request, true, false,
                                         false);
            blockContent = sb.toString();
            blockTitle = Misc.getProperty(props, "title", msg("Entries"))
                         + link;
            blockContent = sb.toString();
            blockTitle = Misc.getProperty(props, "title", msg("Groups"))
                         + link;
        } else if (include.equals(WIKIPROP_CHILDREN)) {
            doBG = false;
            StringBuffer sb = new StringBuffer();
            List<Entry> children =
                (List<Entry>) wikiUtil.getProperty(entry.getId()
                    + "_children");
            if (children == null) {
                children = getEntryManager().getChildren(request, entry);
            }

            if (children.size() == 0) {
                return "";
            }
            String link = getEntriesList(sb, children, request, true, false,
                                         false);
            blockContent = sb.toString();
            blockTitle = Misc.getProperty(props, "title", msg("Children"))
                         + link;
        } else {
            return null;
        }

        if ( !inBlock) {
            return blockContent;
        }
        if (doBG) {
            return HtmlUtil.makeShowHideBlock(blockTitle, blockContent, open,
                    HtmlUtil.cssClass("wiki-tocheader"),
                    HtmlUtil.cssClass("wiki-toc"));
        } else {
            return HtmlUtil.makeShowHideBlock(blockTitle, blockContent, open);
        }


    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    public StringBuffer getCommentBlock(Request request, Entry entry)
            throws Exception {
        StringBuffer  sb       = new StringBuffer();
        List<Comment> comments = getEntryManager().getComments(request,
                                     entry);
        if (comments.size() > 0) {
            sb.append(getEntryManager().getCommentHtml(request, entry));
        }
        return sb;

    }




    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param request _more_
     * @param importEntry _more_
     * @param property _more_
     * @param tag _more_
     * @param props _more_
     *
     * @return _more_
     */
    public String handleWikiImport(WikiUtil wikiUtil, final Request request,
                                   Entry importEntry, String tag,
                                   Hashtable props) {
        try {
            Request myRequest =
                new Request(getRepository(), request.getUser(),
                            getRepository().URL_ENTRY_SHOW.toString()) {
                public void putExtraProperty(Object key, Object value) {
                    request.putExtraProperty(key, value);
                }
                public Object getExtraProperty(Object key) {
                    return request.getExtraProperty(key);
                }

            };



            for (Enumeration keys = props.keys(); keys.hasMoreElements(); ) {
                Object key = keys.nextElement();
                myRequest.put(key, props.get(key));
            }



            String include = getWikiInclude(wikiUtil, request, importEntry,
                                            tag, props);
            if (include != null) {
                return include;
            }

            OutputHandler handler = getRepository().getOutputHandler(tag);
            if (handler == null) {
                return null;
            }
            OutputType outputType = handler.findOutputType(tag);

            String originalOutput = request.getString(ARG_OUTPUT,
                                        (String) "");
            String originalId = request.getString(ARG_ENTRYID, (String) "");
            myRequest.put(ARG_ENTRYID, importEntry.getId());
            myRequest.put(ARG_OUTPUT, outputType.getId());
            myRequest.put(ARG_EMBEDDED, "true");

            String title = null;
            String propertyValue;
            if ( !outputType.getIsHtml()) {
                List<Link> links = new ArrayList<Link>();
                handler.getEntryLinks(myRequest, new State(importEntry),
                                      links);
                Link theLink = null;
                for (Link link : links) {
                    if (Misc.equals(outputType, link.getOutputType())) {
                        theLink = link;
                        break;
                    }
                }

                String url = ((theLink != null)
                              ? theLink.getUrl()
                              : myRequest.entryUrl(
                                  getRepository().URL_ENTRY_SHOW,
                                  importEntry, ARG_OUTPUT,
                                  outputType.getId()));
                String label = importEntry.getName() + " - "
                               + ((theLink != null)
                                  ? theLink.getLabel()
                                  : outputType.getLabel());
                propertyValue = getEntryManager().getTooltipLink(myRequest,
                                                                 importEntry, label, url);
            } else {
                Result result = getEntryManager().processEntryShow(myRequest,
                                    importEntry);
                propertyValue = new String(result.getContent());
                title         = result.getTitle();
                title         = Misc.getProperty(props, "title", title);
            }

            boolean open = Misc.getProperty(props, "open", true);
            if (title != null) {
                return HtmlUtil.makeShowHideBlock(title, propertyValue, open,
                        HtmlUtil.cssClass("wiki-tocheader"),
                        HtmlUtil.cssClass("wiki-toc"));
            }
            return propertyValue;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param wikiUtil _more_
     * @param name _more_
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findWikiEntry(Request request, WikiUtil wikiUtil,
                               String name, Entry parent)
            throws Exception {
        name = name.trim();
        Entry theEntry = null;
        theEntry = getEntryManager().getEntry(request, name);
        if ((theEntry == null) && parent.isGroup()) {
            for (Entry child : getEntryManager().getChildren(request,
                    (Group) parent)) {
                if (child.getName().trim().equalsIgnoreCase(name)) {
                    theEntry = child;
                    break;
                }
            }
        }
        return theEntry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param textAreaId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeWikiEditBar(Request request, Entry entry,
                                  String textAreaId)
            throws Exception {

        String select = OutputHandler.getSelect(request, textAreaId,
                            "Add link", true, "wikilink") + HtmlUtil.space(1)
                                + OutputHandler.getSelect(request,
                                    textAreaId, "Add import entry", true,
                                    "entryid");

        StringBuffer buttons = new StringBuffer();
        buttons.append(addWikiEditButton(textAreaId, "button_bold.png",
                                         "Bold text", "\\'\\'\\'",
                                         "\\'\\'\\'", "Bold text",
                                         "mw-editbutton-bold"));
        buttons.append(addWikiEditButton(textAreaId, "button_italic.png",
                                         "Italic text", "\\'\\'", "\\'\\'",
                                         "Italic text",
                                         "mw-editbutton-italic"));
        buttons.append(addWikiEditButton(textAreaId, "button_link.png",
                                         "Internal link", "[[", "]]",
                                         "Link title", "mw-editbutton-link"));
        buttons.append(
            addWikiEditButton(
                textAreaId, "button_extlink.png",
                "External link (remember http:// prefix)", "[", "]",
                "http://www.example.com link title",
                "mw-editbutton-extlink"));
        buttons.append(addWikiEditButton(textAreaId, "button_headline.png",
                                         "Level 2 headline", "\\n== ",
                                         " ==\\n", "Headline text",
                                         "mw-editbutton-headline"));
        buttons.append(addWikiEditButton(textAreaId, "button_linebreak.png",
                                         "Line break", "<br>", "", "",
                                         "mw-editbutton-headline"));
        buttons.append(addWikiEditButton(textAreaId, "button_strike.png",
                                         "Strike Through", "<s>", "</s>",
                                         "Strike-through text",
                                         "mw-editbutton-headline"));
        buttons.append(addWikiEditButton(textAreaId,
                                         "button_upper_letter.png",
                                         "Super Script", "<sup>", "</sup>",
                                         "Super script text",
                                         "mw-editbutton-headline"));
        buttons.append(addWikiEditButton(textAreaId,
                                         "button_lower_letter.png",
                                         "Sub Script", "<sub>", "</sub>",
                                         "Subscript script text",
                                         "mw-editbutton-headline"));
        buttons.append(addWikiEditButton(textAreaId, "button_small.png",
                                         "Small text", "<small>", "</small>",
                                         "Small text",
                                         "mw-editbutton-headline"));
        buttons.append(addWikiEditButton(textAreaId, "button_blockquote.png",
                                         "Insert block quote",
                                         "<blockquote>", "</blockquote>",
                                         "Quoted text",
                                         "mw-editbutton-headline"));
        //        buttons.append(addWikiEditButton(textAreaId,"button_image.png","Embedded file","[[File:","]]","Example.jpg","mw-editbutton-image"));
        //        buttons.append(addWikiEditButton(textAreaId,"button_media.png","File link","[[Media:","]]","Example.ogg","mw-editbutton-media"));
        //        buttons.append(addWikiEditButton(textAreaId,"button_nowiki.png","Ignore wiki formatting","\\x3cnowiki\\x3e","\\x3c/nowiki\\x3e","Insert non-formatted text here","mw-editbutton-nowiki"));
        //        buttons.append(addWikiEditButton(textAreaId,"button_sig.png","Your signature with timestamp","--~~~~","","","mw-editbutton-signature"));
        buttons.append(addWikiEditButton(textAreaId, "button_hr.png",
                                         "Horizontal line (use sparingly)",
                                         "\\n----\\n", "", "",
                                         "mw-editbutton-hr"));

        StringBuffer propertyMenu = new StringBuffer();
        StringBuffer importMenu   = new StringBuffer();
        for (int i = 0; i < OutputHandler.WIKIPROPS.length; i++) {
            String prop = OutputHandler.WIKIPROPS[i];
            String js = "javascript:insertTags("
                        + HtmlUtil.squote(textAreaId) + ","
                        + HtmlUtil.squote("{{") + "," + HtmlUtil.squote("}}")
                        + "," + HtmlUtil.squote(prop) + ");";
            propertyMenu.append(HtmlUtil.href(js, prop));
            propertyMenu.append(HtmlUtil.br());

            String js2 = "javascript:insertTags("
                         + HtmlUtil.squote(textAreaId) + ","
                         + HtmlUtil.squote("{{import ") + ","
                         + HtmlUtil.squote(" " + prop + "}}") + ","
                         + HtmlUtil.squote(" entryid ") + ");";
            importMenu.append(HtmlUtil.href(js2, prop));
            importMenu.append(HtmlUtil.br());
        }

        List<Link> links = getRepository().getOutputLinks(request,
                               new OutputHandler.State(entry));


        propertyMenu.append("<hr>");
        for (Link link : links) {
            if (link.getOutputType() == null) {
                continue;
            }

            String prop = link.getOutputType().getId();
            String js = "javascript:insertTags("
                        + HtmlUtil.squote(textAreaId) + ","
                        + HtmlUtil.squote("{{") + "," + HtmlUtil.squote("}}")
                        + "," + HtmlUtil.squote(prop) + ");";
            propertyMenu.append(HtmlUtil.href(js, link.getLabel()));
            propertyMenu.append(HtmlUtil.br());
        }



        StringBuffer importOutputMenu = new StringBuffer();
        /*
                List<OutputType> allTypes = getRepository().getOutputTypes();
                //        importMenu.append("<hr>");
                for(OutputType type: allTypes) {
                    String prop = type.getId();
                    String js = "javascript:insertTags(" + HtmlUtil.squote(textAreaId)+"," +
                        HtmlUtil.squote("{{import ") +","+
                        HtmlUtil.squote(" " + type.getId()+" }}") +","+
                        HtmlUtil.squote("entryid")+");";
                    importOutputMenu.append(HtmlUtil.href(js,type.getLabel()));
                    importOutputMenu.append(HtmlUtil.br());
                }
        */


        String propertyMenuLabel =
            HtmlUtil.img(iconUrl("/icons/wiki/button_property.png"),
                         "Add Entry Property");
        String propertyButton =
            getRepository().makePopupLink(propertyMenuLabel,
                                          propertyMenu.toString());
        buttons.append(propertyButton);
        String importMenuLabel =
            HtmlUtil.img(iconUrl("/icons/wiki/button_import.png"),
                         "Import Entry Property");
        String importButton = getRepository().makePopupLink(importMenuLabel,
                                                            HtmlUtil.hbox(importMenu.toString(),
                                                                          importOutputMenu.toString()));
        buttons.append(importButton);
        buttons.append(HtmlUtil.space(2));
        buttons.append(select);

        return buttons.toString();
    }


    /**
     * _more_
     *
     *
     * @param textAreaId _more_
     * @param icon _more_
     * @param label _more_
     * @param prefix _more_
     * @param suffix _more_
     * @param example _more_
     * @param huh _more_
     *
     * @return _more_
     */
    private String addWikiEditButton(String textAreaId, String icon,
                                     String label, String prefix,
                                     String suffix, String example,
                                     String huh) {
        String prop = prefix + example + suffix;
        String js;
        if (suffix.length() == 0) {
            js = "javascript:insertText(" + HtmlUtil.squote(textAreaId) + ","
                 + HtmlUtil.squote(prop) + ");";
        } else {
            js = "javascript:insertTags(" + HtmlUtil.squote(textAreaId) + ","
                 + HtmlUtil.squote(prefix) + "," + HtmlUtil.squote(suffix)
                 + "," + HtmlUtil.squote(example) + ");";
        }
        return HtmlUtil.href(js,
                             HtmlUtil.img(iconUrl("/icons/wiki/" + icon),
                                          label));

    }




    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param name _more_
     * @param label _more_
     *
     * @return _more_
     */
    public String getWikiLink(WikiUtil wikiUtil, String name, String label) {
        try {
            Entry   entry   = (Entry) wikiUtil.getProperty(PROP_ENTRY);
            Request request = (Request) wikiUtil.getProperty(PROP_REQUEST);
            Group   parent  = entry.getParentGroup();


            name = name.trim();
            if (name.startsWith("Category:")) {
                String category = name.substring("Category:".length());
                String url = request.url(getRepository().URL_ENTRY_SEARCH,
                                         ARG_METADATA_TYPE + ".wikicategory",
                                         "wikicategory",
                                         ARG_METADATA_ATTR1
                                         + ".wikicategory", category);
                wikiUtil.addCategoryLink(HtmlUtil.href(url, category));
                List categories =
                    (List) wikiUtil.getProperty("wikicategories");
                if (categories == null) {
                    wikiUtil.putProperty("wikicategories",
                                         categories = new ArrayList());
                }
                categories.add(category);
                return "";
            }

            Entry theEntry = null;
            //If the entry is a group first check its children.
            if (entry.isGroup()) {
                theEntry = findWikiEntry(request, wikiUtil, name,
                                         (Group) entry);
            }
            if (theEntry == null) {
                theEntry = findWikiEntry(request, wikiUtil, name, parent);
            }

            if (theEntry != null) {
                addWikiLink(wikiUtil, theEntry);
                if (label.trim().length() == 0) {
                    label = theEntry.getName();
                }
                if (theEntry.getType().equals(
                        WikiPageTypeHandler.TYPE_WIKIPAGE)) {
                    String url =
                        request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                         theEntry, ARG_OUTPUT,
                                         WikiPageOutputHandler.OUTPUT_WIKI);
                    return getEntryManager().getTooltipLink(request, theEntry,
                                                            label, url);

                } else {
                    return getEntryManager().getTooltipLink(request, theEntry,
                                                            label,null);
                }
            }


            String url = request.url(getRepository().URL_ENTRY_FORM,
                                     ARG_NAME, name, ARG_GROUP,
                                     (entry.isGroup()
                                      ? entry.getId()
                                      : parent.getId()), ARG_TYPE,
                                          WikiPageTypeHandler.TYPE_WIKIPAGE);

            return HtmlUtil.href(url, name,
                                 HtmlUtil.cssClass("wiki-link-noexist"));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param wikiContent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String wikifyEntry(Request request, Entry entry,
                              String wikiContent)
            throws Exception {
        return wikifyEntry(request, entry, wikiContent, null, null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param wikiContent _more_
     * @param subGroups _more_
     * @param subEntries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String wikifyEntry(Request request, Entry entry,
                              String wikiContent, List<Group> subGroups,
                              List<Entry> subEntries)
            throws Exception {
        WikiUtil wikiUtil = new WikiUtil(Misc.newHashtable(new Object[] {
                                PROP_REQUEST,
                                request, PROP_ENTRY, entry }));
        return wikifyEntry(request, entry, wikiUtil, wikiContent, subGroups,
                           subEntries);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param wikiUtil _more_
     * @param wikiContent _more_
     * @param subGroups _more_
     * @param subEntries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String wikifyEntry(Request request, Entry entry,
                              WikiUtil wikiUtil, String wikiContent,
                              List<Group> subGroups, List<Entry> subEntries)
            throws Exception {
        List children = new ArrayList();
        if (subGroups != null) {
            wikiUtil.putProperty(entry.getId() + "_subgroups", subGroups);
            children.addAll(subGroups);
        }

        if (subEntries != null) {
            wikiUtil.putProperty(entry.getId() + "_subentries", subEntries);
            children.addAll(subEntries);
        }

        wikiUtil.putProperty(entry.getId() + "_children", children);


        //TODO: We need to keep track of what is getting called so we prevent
        //infinite loops
        String content = wikiUtil.wikify(wikiContent, this);
        return HtmlUtil.div(content, HtmlUtil.cssClass("wikicontent"));

    }

    /**
     * _more_
     *
     * @param wikiUtil _more_
     * @param toEntry _more_
     */
    protected void addWikiLink(WikiUtil wikiUtil, Entry toEntry) {
        Hashtable links = (Hashtable) wikiUtil.getProperty("wikilinks");
        if (links == null) {
            wikiUtil.putProperty("wikilinks", links = new Hashtable());
        }
        links.put(toEntry, toEntry);
    }


}

