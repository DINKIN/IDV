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
 * WITHOUT ANY WARRANTYP; without even the implied warranty of
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
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.sql.Statement;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class AdminMetadataHandler extends MetadataHandler {


    /** _more_ */
    public static Metadata.Type TYPE_TEMPLATE =
        new Metadata.Type("admin.template", "Page Template");

    /** _more_ */
    public static Metadata.Type TYPE_CONTENTTEMPLATE =
        new Metadata.Type("admin.contenttemplate", "Content Template");

    /** _more_ */
    public static Metadata.Type TYPE_LOCALFILE_PATTERN =
        new Metadata.Type("admin.localfile.pattern", "Local File Pattern");

    //TODO:

    /** _more_          */
    public static Metadata.Type TYPE_ANONYMOUS_UPLOAD =
        new Metadata.Type("admin.anonymousupload", "Anonymous Upload");



    /**
     * _more_
     *
     * @param repository _more_
     * @param node _more_
     * @throws Exception _more_
     */
    public AdminMetadataHandler(Repository repository, Element node)
            throws Exception {
        super(repository, node);
        addType(TYPE_TEMPLATE);
        nonLocalTypes.add(TYPE_TEMPLATE);
        addType(TYPE_LOCALFILE_PATTERN);
        addType(TYPE_ANONYMOUS_UPLOAD);

        //        addType(TYPE_CONTENTTEMPLATE);
    }


    /** _more_ */
    private List<Metadata.Type> dummyTypeList =
        new ArrayList<Metadata.Type>();

    /** _more_ */
    private List<Metadata.Type> nonLocalTypes =
        new ArrayList<Metadata.Type>();

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public List<Metadata.Type> getTypes(Request request, Entry entry) {
        if (request.getUser().getAdmin()) {
            if (entry.getIsLocalFile()) {
                return super.getTypes(request, entry);
            }
            return nonLocalTypes;
        }
        return dummyTypeList;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected String getHandlerGroupName() {
        return "Admin";
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     */
    public String[] getHtml(Request request, Entry entry, Metadata metadata) {
        Metadata.Type type = getType(metadata.getType());
        String        lbl  = msgLabel(type.getLabel());
        if (type.equals(TYPE_TEMPLATE) || type.equals(TYPE_CONTENTTEMPLATE)) {
            return new String[] { lbl, "Has template" };
        }

        if (type.equals(TYPE_ANONYMOUS_UPLOAD)) {
            return new String[] { lbl,
                                  "From:" + metadata.getAttr1() + " IP: "
                                  + metadata.getAttr2() };
        }

        if (type.equals(TYPE_LOCALFILE_PATTERN)) {
            return new String[] { lbl, "Local File Pattern" };
        }

        String content = metadata.getAttr1();
        //        return new String[] { lbl, content };
        return null;
    }




    /**
     * _more_
     *
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param forEdit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getForm(Request request, Entry entry, Metadata metadata,
                            boolean forEdit)
            throws Exception {
        Metadata.Type type   = getType(metadata.getType());
        String        lbl    = msgLabel(type.getLabel());
        String        id     = metadata.getId();
        String        suffix = "";
        if (id.length() > 0) {
            suffix = "." + id;
        }
        String submit = (forEdit
                         ? ""
                         : HtmlUtil.submit(msg("Add") + HtmlUtil.space(1)
                                           + lbl));
        String cancel  = (forEdit
                          ? ""
                          : HtmlUtil.submit(msg("Cancel"), ARG_CANCEL));
        String arg1    = ARG_ATTR1 + suffix;
        String content = "";
        if (type.equals(TYPE_TEMPLATE)) {
            String value = metadata.getAttr1();
            if ( !forEdit) {
                value = getRepository().getResource(PROP_HTML_TEMPLATE);
            }
            value = value.replace("<", "&lt;");
            value = value.replace(">", "&gt;");
            value = value.replace("$", "&#36;");
            String textarea = HtmlUtil.textArea(arg1, value, 20, 80);
            content =
                HtmlUtil.row(HtmlUtil.colspan(submit, 2))
                + HtmlUtil.formEntry(lbl,
                                     "Note: must contain macro ${content}"
                                     + "<br>" + textarea);
        }
        if (type.equals(TYPE_LOCALFILE_PATTERN)) {
            if ((metadata.getEntry() == null)
                    || !metadata.getEntry().getIsLocalFile()) {
                return null;
            }
            String value = metadata.getAttr1();
            String input = HtmlUtil.input(arg1, value);
            content = HtmlUtil.row(HtmlUtil.colspan(submit, 2))
                      + HtmlUtil.formEntry(lbl, input);
        }
        if (type.equals(TYPE_ANONYMOUS_UPLOAD)) {
            content = "From:" + metadata.getAttr1() + " IP: "
                      + metadata.getAttr2();
        }


        if ( !forEdit) {
            content = content + HtmlUtil.row(HtmlUtil.colspan(cancel, 2));
        }
        String argtype = ARG_TYPE + suffix;
        String argid   = ARG_METADATAID + suffix;
        content = content + HtmlUtil.hidden(argtype, type.getType())
                  + HtmlUtil.hidden(argid, metadata.getId());
        return new String[] { lbl, content };
    }






}

