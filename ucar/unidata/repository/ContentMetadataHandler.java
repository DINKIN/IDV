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

import ucar.unidata.repository.data.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.awt.Image;


import java.io.*;
import java.io.File;
import java.io.FileInputStream;

import java.net.URL;
import java.net.URLConnection;


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
public class ContentMetadataHandler extends MetadataHandler {


    /** _more_ */
    public static final String  TYPE_THUMBNAIL ="content.thumbnail";

    /** _more_ */
    public static final String  TYPE_ATTACHMENT = "content.attachment";

    /** _more_ */
    public static final String TYPE_CONTACT ="content.contact";

    /** _more_          */
    public static final String TYPE_SORT = "content.sort";


    public ContentMetadataHandler(Repository repository)
            throws Exception {
        super(repository);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected String getHandlerGroupName() {
        return "Attachments and Contact";
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param doc _more_
     * @param datasetNode _more_
     *
     * @throws Exception _more_
     */
    public void addMetadataToCatalog(Request request, Entry entry,
                                     Metadata metadata, Document doc,
                                     Element datasetNode)
            throws Exception {
        if (metadata.getType().equals(TYPE_THUMBNAIL)) {
            XmlUtil.create(
                doc,
                ThreddsMetadataHandler.getTag(
                    ThreddsMetadataHandler.TYPE_PROPERTY), datasetNode,
                        new String[] { ThreddsMetadataHandler.ATTR_NAME,
                                       "thumbnail", ThreddsMetadataHandler
                                           .ATTR_VALUE, getRepository()
                                           .absoluteUrl(request
                                               .url(getRepository()
                                                   .getMetadataManager()
                                                       .URL_METADATA_VIEW, ARG_ENTRYID, metadata
                                                           .getEntryId(), ARG_METADATA_ID, metadata
                                                               .getId() /*,ARG_THUMBNAIL,"true"*/)) });
        }
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param node _more_
     * @param fileMap _more_
     * @param internal _more_
     *
     * @throws Exception _more_
     */
    public void processMetadataXml(Entry entry, Element node,
                                   Hashtable fileMap, boolean internal)
            throws Exception {
        MetadataType type = findType(XmlUtil.getAttribute(node, ATTR_TYPE));
        if (type.isType(TYPE_THUMBNAIL)
                || type.isType(TYPE_ATTACHMENT)) {
            String fileArg  = XmlUtil.getAttribute(node, ATTR_ATTR1, "");
            String fileName = null;

            if (internal) {
                fileName = fileArg;
            } else {
                String tmpFile = (String) fileMap.get(fileArg);
                if (tmpFile == null) {
                    getRepository().getLogManager().logError(
                        "No attachment uploaded file:" + fileArg);
                    getRepository().getLogManager().logError(
                        "available files: " + fileMap);
                    return;
                }
                File file = new File(tmpFile);
                fileName =
                    getRepository().getStorageManager().copyToEntryDir(entry,
                        file).getName();
            }

            Metadata metadata = new Metadata(getRepository().getGUID(),
                                             entry.getId(), type,
                                             XmlUtil.getAttribute(node,
                                                 ATTR_INHERITED,
                                                 DFLT_INHERITED), fileName,
                                                     "", "", "");
            entry.addMetadata(metadata);
        } else {
            super.processMetadataXml(entry, node, fileMap, internal);
        }

    }


    /**
     * _more_
     *
     * @param metadata _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void newEntry(Metadata metadata, Entry entry) throws Exception {
        if (metadata.getType().equals(TYPE_THUMBNAIL) || metadata.getType().equals(TYPE_ATTACHMENT)) {
            String fileArg = metadata.getAttr1();
            if ( !entry.getIsLocalFile()) {
                fileArg =
                    getRepository().getStorageManager().copyToEntryDir(entry,
                        new File(fileArg)).getName();
            }
            metadata.setAttr1(fileArg);
        }
        super.newEntry(metadata, entry);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param sb _more_
     * @param metadata _more_
     * @param forLink _more_
     *
     * @throws Exception _more_
     */
    public void decorateEntry(Request request, Entry entry, StringBuffer sb,
                              Metadata metadata, boolean forLink)
            throws Exception {
        if (metadata.getType().equals(TYPE_THUMBNAIL)) {
            String html = getFileHtml(request, entry, metadata, forLink);
            if (html != null) {
                sb.append(HtmlUtil.space(1));
                sb.append(html);
                sb.append(HtmlUtil.space(1));
            }
        }

        if ( !forLink && metadata.getType().equals(TYPE_ATTACHMENT)) {
            String html = getFileHtml(request, entry, metadata, false);
            if (html != null) {
                sb.append(HtmlUtil.space(1));
                sb.append(html);
                sb.append(HtmlUtil.space(1));
            }
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     * @param forLink _more_
     *
     * @return _more_
     */
    public String getFileHtml(Request request, Entry entry,
                              Metadata metadata, boolean forLink) {
        File          f    = getImageFile(entry, metadata);
        if (f == null) {
            return null;
        }

        String extra = (forLink
                        ? " "
                        : "");
        if (ImageUtils.isImage(f.toString())) {
            String img =
                HtmlUtil
                    .img(request
                        .url(getRepository().getMetadataManager()
                            .URL_METADATA_VIEW, ARG_ENTRYID,
                                metadata.getEntryId(), ARG_METADATA_ID,
                                metadata.getId(), ARG_THUMBNAIL,
                                "" + forLink), msg("Click to enlarge"),
                                    extra);

            if (forLink) {
                String bigimg =
                    HtmlUtil
                        .img(request
                            .url(getRepository().getMetadataManager()
                                .URL_METADATA_VIEW, ARG_ENTRYID,
                                    metadata.getEntryId(), ARG_METADATA_ID,
                                    metadata.getId()), "thumbnail", "");


                String imgUrl =
                    HtmlUtil
                        .url(getRepository().getMetadataManager()
                            .URL_METADATA_VIEW + "/"
                                + getRepository().getStorageManager()
                                    .getFileTail(f.toString()), ARG_ENTRYID,
                                        metadata.getEntryId(),
                                        ARG_METADATA_ID, metadata.getId());


                //                System.err.println(imgUrl);
                //img =  HtmlUtil.href(imgUrl,img," dojoType=\"dojox.image.Lightbox\" ");
                img = getRepository().makePopupLink(img, bigimg, true, false);
            }
            return img;
        } else if (f.exists()) {
            String name =
                getRepository().getStorageManager().getFileTail(f.getName());
            return HtmlUtil.href(
                request.url(
                    getRepository().getMetadataManager().URL_METADATA_VIEW,
                    ARG_ENTRYID, metadata.getEntryId(), ARG_METADATA_ID,
                    metadata.getId()), name);
        }
        return "";
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     */
    private File getImageFile(Entry entry, Metadata metadata) {
        File f;
        if ( !entry.getIsLocalFile()) {
            f = new File(
                IOUtil.joinDir(
                    getRepository().getStorageManager().getEntryDir(
                        metadata.getEntryId(), false), metadata.getAttr1()));
        } else {
            f = new File(metadata.getAttr1());
        }
        if ( !f.exists()) {
            return null;
        }
        return f;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param metadata _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processView(Request request, Entry entry, Metadata metadata)
            throws Exception {

        if (metadata.getType().equals(TYPE_THUMBNAIL) || metadata.getType().equals(TYPE_ATTACHMENT)) {
            File f = getImageFile(entry, metadata);
            if (f == null) {
                return new Result("", "Thumbnail does not exist");
            }
            String mimeType = getRepository().getMimeTypeFromSuffix(
                                  IOUtil.getFileExtension(f.toString()));
            if (request.get(ARG_THUMBNAIL, false)) {
                File thumb = getStorageManager().getTmpFile(request,
                                 IOUtil.getFileTail(f.toString()));
                if ( !thumb.exists()) {
                    Image image = ImageUtils.readImage(f.toString());
                    image = ImageUtils.resize(image, 100, -1);
                    ImageUtils.waitOnImage(image);
                    ImageUtils.writeImageToFile(image, thumb.toString());
                }
                f = thumb;
            }

            Result result =
                new Result("thumbnail",
                           IOUtil.readBytes(new FileInputStream(f), null,
                                            true), mimeType);
            result.setShouldDecorate(false);
            return result;
        }
        return new Result("", "Cannot process view");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param id _more_
     * @param suffix _more_
     * @param metadataList _more_
     * @param newMetadata _more_
     *
     * @throws Exception _more_
     */
    public void handleForm(Request request, Entry entry, String id,
                           String suffix, List<Metadata> metadataList,
                           boolean newMetadata)
            throws Exception {
        String type =  request.getString(ARG_TYPE + suffix);
        if (type.equals(TYPE_THUMBNAIL) || type.equals(TYPE_ATTACHMENT)) {
            if ( !newMetadata) {
                //TODO: delete the old thumbs file
            }
            String url     = request.getString(ARG_ATTR2 + suffix, "");
            String theFile = null;
            if (url.length() > 0) {
                String tail    = IOUtil.getFileTail(url);
                File   tmpFile = getStorageManager().getTmpFile(request,
                                     tail);
                RepositoryUtil.checkFilePath(tmpFile.toString());
                URL              fromUrl    = new URL(url);
                URLConnection    connection = fromUrl.openConnection();
                InputStream      fromStream = connection.getInputStream();
                FileOutputStream toStream   = new FileOutputStream(tmpFile);
                try {
                    int bytes = IOUtil.writeTo(fromStream, toStream);
                    if (bytes < 0) {
                        throw new IllegalArgumentException(
                            "Could not download url:" + url);
                    }
                } catch (Exception ioe) {
                    throw new IllegalArgumentException(
                        "Could not download url:" + url);
                } finally {
                    try {
                        toStream.close();
                        fromStream.close();
                    } catch (Exception exc) {}
                }
                theFile = tmpFile.toString();
            } else {
                String fileArg = request.getUploadedFile(ARG_ATTR1 + suffix);
                if (fileArg == null) {
                    return;
                }

                theFile = fileArg;
            }

            theFile =
                getRepository().getStorageManager().moveToEntryDir(entry,
                    new File(theFile)).getName();
            metadataList.add(new Metadata(id, entry.getId(), type, false,
                                          theFile, "", "", ""));
            return;
        }
        super.handleForm(request, entry, id, suffix, metadataList,
                         newMetadata);
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
        String[] result = super.getForm(request, entry, metadata, forEdit);
        if (result != null) {
            return result;
        }

        MetadataType type = findType(metadata.getType());
        if(type == null) return null;
        String        lbl     = msgLabel(type.getLabel());
        String        content = null;
        String        id      = metadata.getId();
        String        suffix  = "";
        if (id.length() > 0) {
            suffix = "." + id;
        }

        String submit = HtmlUtil.submit(msg("Add") + HtmlUtil.space(1)
                                        + lbl);
        String cancel = HtmlUtil.submit(msg("Cancel"), ARG_CANCEL);
        if (forEdit) {
            submit = "";
            cancel = "";
        }
        String arg1 = ARG_ATTR1 + suffix;
        String arg2 = ARG_ATTR2 + suffix;
        String size = HtmlUtil.SIZE_70;
        if (type.isType(TYPE_THUMBNAIL) || type.isType(TYPE_ATTACHMENT)) {
            String image = (forEdit
                            ? getFileHtml(request, entry, metadata, false)
                            : "");
            if (image == null) {
                image = "";
            } else {
                image = "<br>" + image;
            }
            content = formEntry(new String[] { submit,
                    msgLabel((type.isType(TYPE_THUMBNAIL)
                              ? "Thumbnail"
                              : "Attachment")), HtmlUtil.fileInput(arg1,
                              size) + image,
                    msgLabel("Or download URL"),
                    HtmlUtil.input(arg2, "", size) });
        }

        if (content == null) {
            return null;
        }
        String argtype = ARG_TYPE + suffix;
        String argid   = ARG_METADATAID + suffix;
        content = content + HtmlUtil.hidden(argtype, type)
                  + HtmlUtil.hidden(argid, metadata.getId());
        if (cancel.length() > 0) {
            content = content + HtmlUtil.row(HtmlUtil.colspan(cancel, 2));
        }
        return new String[] { lbl, content };
    }






}

