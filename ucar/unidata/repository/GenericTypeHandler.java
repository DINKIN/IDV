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


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.util.WrapperException;
import ucar.unidata.xml.XmlUtil;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;


import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GenericTypeHandler extends TypeHandler {

    /** _more_ */
    public static final String TAG_COLUMN = "column";

    /** _more_          */
    public static final String TAG_PROPERTY = "property";

    /** _more_          */
    public static final String ATTR_NAME = "name";

    /** _more_          */
    public static final String ATTR_VALUE = "value";

    /** _more_ */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String TAG_HANDLER = "handler";




    /** _more_ */
    public static final String COL_ID = "id";

    /** _more_ */
    List<Column> columns;

    /** _more_ */
    List colNames;

    /** _more_ */
    Hashtable nameMap = new Hashtable();

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public GenericTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository);
        init(entryNode);
    }


    /**
     * _more_
     *
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    private void init(Element entryNode) throws Exception {
        type = XmlUtil.getAttribute(entryNode, ATTR_DB_NAME);
        setDescription(XmlUtil.getAttribute(entryNode, ATTR_DB_DESCRIPTION,
                                            getType()));


        List propertyNodes = XmlUtil.findChildren(entryNode, TAG_PROPERTY);
        for (int propIdx = 0; propIdx < propertyNodes.size(); propIdx++) {
            Element propertyNode = (Element) propertyNodes.get(propIdx);
            if (XmlUtil.hasAttribute(propertyNode, ATTR_VALUE)) {
                putProperty(XmlUtil.getAttribute(propertyNode, ATTR_NAME),
                            XmlUtil.getAttribute(propertyNode, ATTR_VALUE));
            } else {
                putProperty(XmlUtil.getAttribute(propertyNode, ATTR_NAME),
                            XmlUtil.getChildText(propertyNode));
            }
        }



        this.columns = new ArrayList<Column>();
        colNames     = new ArrayList();

        List columnNodes = XmlUtil.findChildren(entryNode, TAG_COLUMN);
        if (columnNodes.size() == 0) {
            return;
        }


        colNames.add(COL_ID);
        StringBuffer tableDef = new StringBuffer("create table "
                                    + getTableName() + " (\n");
        StringBuffer indexDef = new StringBuffer();
        tableDef.append(COL_ID + " varchar(200)");
        indexDef.append("CREATE INDEX " + getTableName() + "_INDEX_" + COL_ID
                        + "  ON " + getTableName() + " (" + COL_ID + ");\n");
        for (int colIdx = 0; colIdx < columnNodes.size(); colIdx++) {
            Element columnNode = (Element) columnNodes.get(colIdx);
            Column  column = new Column(this, columnNode,
                                        colNames.size() - 1);
            columns.add(column);
            colNames.addAll(column.getColumnNames());
            tableDef.append(",\n");
            tableDef.append(column.getSqlCreate());
            indexDef.append(column.getSqlIndex());
        }
        tableDef.append(")");
        //            System.err.println("table:" + tableDef);
        //            System.err.println("index:" + indexDef);
        Statement statement =
            getRepository().getConnection().createStatement();
        try {
            statement.execute(tableDef.toString());
        } catch (Throwable exc) {
            if (exc.toString().indexOf("already exists") < 0) {
                //TODO:
                //                throw new WrapperException(exc);
            }
        }
        try {
            SqlUtil.loadSql(indexDef.toString(), statement, false);
        } catch (Throwable exc) {
            //TODO:
            //            throw new WrapperException(exc);
        }


    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntry(Request request, Entry entry)
            throws Exception {
        super.initializeEntry(request, entry);
        if (colNames.size() <= 1) {
            return;
        }
        Object[] values = new Object[colNames.size()];
        for (Column column : columns) {
            column.setValue(request, values);
        }
        entry.setValues(values);
    }




    /**
     * _more_
     *
     * @param arg _more_
     * @param value _more_
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public int matchValue(String arg, Object value, Request request,
                          Entry entry) {
        for (Column column : columns) {
            int match = column.matchValue(arg, value, request, entry);
            if (match == MATCH_FALSE) {
                return MATCH_FALSE;
            }
            if (match == MATCH_TRUE) {
                return MATCH_TRUE;
            }
        }
        return MATCH_UNKNOWN;
    }


    /**
     * _more_
     *
     * @param longName _more_
     *
     * @return _more_
     */
    public List<TwoFacedObject> getListTypes(boolean longName) {
        List<TwoFacedObject> list = super.getListTypes(longName);
        for (Column column : columns) {
            if (column.getCanList()) {
                list.add(new TwoFacedObject((longName
                                             ? (getDescription() + " - ")
                                             : "") + column
                                             .getDescription(), column
                                             .getFullName()));
            }
        }
        return list;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param what _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processList(Request request, String what) throws Exception {
        Column theColumn = null;
        for (Column column : columns) {
            if (column.getCanList() && column.getFullName().equals(what)) {
                theColumn = column;
                break;
            }
        }

        if (theColumn == null) {
            return super.processList(request, what);
        }

        String column = theColumn.getFullName();
        String tag    = theColumn.getName();
        String title  = theColumn.getDescription();
        List   where  = assembleWhereClause(request);
        Statement statement = executeSelect(request,
                                            SqlUtil.distinct(column), where);

        String[]     values = SqlUtil.readString(statement, 1);
        StringBuffer sb     = new StringBuffer();
        String       output = request.getOutput();
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            sb.append(repository.header(title));
            sb.append("<ul>");
        } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(tag + "s"));
        }

        Properties properties =
            repository.getFieldProperties(theColumn.getPropertiesFile());
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                continue;
            }
            String longName = theColumn.getLabel(values[i]);
            if (output.equals(OutputHandler.OUTPUT_HTML)) {
                sb.append("<li>");
                sb.append(longName);
            } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {
                String attrs = XmlUtil.attrs(ATTR_ID, values[i]);
                if (properties != null) {
                    for (Enumeration keys = properties.keys();
                            keys.hasMoreElements(); ) {
                        String key = (String) keys.nextElement();
                        if (key.startsWith(values[i] + ".")) {
                            String value = (String) properties.get(key);
                            value = value.replace("${value}", values[i]);
                            key   = key.substring((values[i] + ".").length());
                            attrs = attrs + XmlUtil.attr(key, value);
                        }
                    }
                }
                sb.append(XmlUtil.tag(tag, attrs));
            } else if (output.equals(CsvOutputHandler.OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(values[i], longName));
                sb.append("\n");
            }
        }
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            sb.append("</ul>");
        } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(tag + "s"));
        }
        return new Result(
            title, sb,
            repository.getOutputHandler(request).getMimeType(output));
    }




    /**
     * _more_
     *
     * @param obj _more_
     *
     * @return _more_
     */
    public boolean equals(Object obj) {
        if ( !super.equals(obj)) {
            return false;
        }
        //TODO
        return true;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTableName() {
        return type;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param statement _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntry(Request request, Statement statement, Entry entry)
            throws Exception {
        super.deleteEntry(request, statement, entry);
        //        statement.setString(1, getTableName());
        //        statement.setString(2, entry.getId());
        String query = SqlUtil.makeDelete(getTableName(), COL_ID,
                                          SqlUtil.quote(entry.getId()));
        statement.execute(query);
        //        statement.addBatch();
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List assembleWhereClause(Request request) throws Exception {
        List where        = super.assembleWhereClause(request);
        int  originalSize = where.size();
        for (Column column : columns) {
            if ( !column.getCanSearch()) {
                continue;
            }
            column.assembleWhereClause(request, where);
        }
        if ((originalSize != where.size()) && (originalSize > 0)) {
            where.add(SqlUtil.eq(COL_ENTRIES_ID, getTableName() + ".id"));
        }
        return where;
    }


    /**
     * _more_
     *
     *
     * @param isNew _more_
     * @return _more_
     */
    public String getInsertSql(boolean isNew) {
        if (colNames.size() == 0) {
            return null;
        }
        if (isNew) {
            return SqlUtil.makeInsert(
                getTableName(), SqlUtil.comma(colNames),
                SqlUtil.getQuestionMarks(colNames.size()));
        } else {

            return SqlUtil.makeUpdate(getTableName(), COL_ID, colNames);
        }
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param stmt _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    public void setStatement(Entry entry, PreparedStatement stmt,
                             boolean isNew)
            throws Exception {
        int stmtIdx = 1;
        stmt.setString(stmtIdx++, entry.getId());
        Object[] values = entry.getValues();
        if (values != null) {
            for (Column column : columns) {
                stmtIdx = column.setValues(stmt, values, stmtIdx);
            }
        }
        if ( !isNew) {
            stmt.setString(stmtIdx, entry.getId());
        }
    }


    /**
     * _more_
     *
     * @param results _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry getEntry(ResultSet results) throws Exception {
        Entry entry = super.getEntry(results);
        if (colNames.size() == 0) {
            return entry;
        }
        Object[] values = new Object[colNames.size()];
        String query = SqlUtil.makeSelect(SqlUtil.comma(colNames),
                                          Misc.newList(getTableName()),
                                          SqlUtil.eq(COL_ID,
                                              SqlUtil.quote(entry.getId())));
        ResultSet results2 = getRepository().execute(query).getResultSet();
        if (results2.next()) {
            int valueIdx = 0;
            for (Column column : columns) {
                valueIdx = column.readValues(results2, values, valueIdx);
            }
        }
        entry.setValues(values);
        return entry;
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param request _more_
     * @param output _more_
     * @param showResource _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StringBuffer getInnerEntryContent(Entry entry, Request request,
                                             String output,
                                             boolean showResource)
            throws Exception {
        StringBuffer sb = super.getInnerEntryContent(entry, request, output,
                              showResource);
        if (output.equals(OutputHandler.OUTPUT_HTML)) {
            int      valueIdx = 0;
            Object[] values   = entry.getValues();
            if (values != null) {
                for (Column column : columns) {
                    StringBuffer tmpSb = new StringBuffer();
                    valueIdx = column.formatValue(tmpSb, output, values,
                            valueIdx);
                    sb.append(HtmlUtil.formEntry(column.getLabel() + ":",
                            tmpSb.toString()));
                }

            }
        } else if (output.equals(XmlOutputHandler.OUTPUT_XML)) {}
        return sb;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param initTables _more_
     *
     * @return _more_
     */
    protected List getTablesForQuery(Request request, List initTables) {
        super.getTablesForQuery(request, initTables);
        for (Column column : columns) {
            if ( !column.getCanSearch()) {
                continue;
            }
            if (request.defined(column.getFullName())) {
                initTables.add(getTableName());
                break;
            }
        }
        return initTables;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryForm(Request request, StringBuffer formBuffer,
                               Entry entry)
            throws Exception {
        for (Column column : columns) {
            column.addToEntryForm(request, formBuffer, entry);
        }

    }


    /**
     * _more_
     *
     * @param formBuffer _more_
     * @param request _more_
     * @param where _more_
     * @param simpleForm _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, StringBuffer formBuffer,
                                List where, boolean simpleForm)
            throws Exception {
        super.addToSearchForm(request, formBuffer, where, simpleForm);
        for (Column column : columns) {
            column.addToSearchForm(request, formBuffer, where);
        }
    }


}

