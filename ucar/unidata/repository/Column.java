/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.Date;

import java.util.Hashtable;
import java.util.List;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;


/**
 */

public class Column implements Tables, Constants {

    public static final String EXPR_EQUALS = "=";
    public static final String EXPR_LE = "<=";
    public static final String EXPR_GE = ">=";
    public static final String EXPR_BETWEEN = "between";
    public static final List EXPR_ITEMS = Misc.newList(new TwoFacedObject("=",EXPR_EQUALS),
                                                       new TwoFacedObject("<=",EXPR_LE),
                                                       new TwoFacedObject(">=",EXPR_GE),
                                                       new TwoFacedObject("between",EXPR_BETWEEN));

    public static final String EXPR_PATTERN = EXPR_EQUALS +"|" +
        EXPR_LE +"|" +
        EXPR_GE +"|" +
        EXPR_BETWEEN;

    /** _more_          */
    public static final String TYPE_STRING = "string";

    /** _more_          */
    public static final String TYPE_INT = "int";

    /** _more_          */
    public static final String TYPE_DOUBLE = "double";

    /** _more_          */
    public static final String TYPE_BOOLEAN = "boolean";

    /** _more_          */
    public static final String TYPE_ENUMERATION = "enumeration";

    /** _more_          */
    public static final String TYPE_TIMESTAMP = "timestamp";

    /** _more_          */
    public static final String TYPE_LATLON = "latlon";

    /** _more_          */
    public static final String SEARCHTYPE_TEXT = "text";

    /** _more_          */
    public static final String SEARCHTYPE_SELECT = "select";


    /** _more_          */
    private static final String ATTR_NAME = "name";
    private static final String ATTR_NAMES = "names";

    /** _more_          */
    private static final String ATTR_LABEL = "label";

    /** _more_          */
    private static final String ATTR_DESCRIPTION = "description";

    /** _more_          */
    private static final String ATTR_TYPE = "type";

    /** _more_          */
    private static final String ATTR_ISINDEX = "isindex";

    /** _more_          */
    private static final String ATTR_CANSEARCH = "cansearch";

    /** _more_          */
    private static final String ATTR_CANLIST = "canlist";

    /** _more_          */
    private static final String ATTR_VALUES = "values";

    /** _more_          */
    private static final String ATTR_DEFAULT = "default";

    /** _more_          */
    private static final String ATTR_SIZE = "size";

    /** _more_          */
    private static final String ATTR_ROWS = "rows";

    /** _more_          */
    private static final String ATTR_COLUMNS = "columns";

    /** _more_          */
    private static final String ATTR_SEARCHTYPE = "searchtype";



    private GenericTypeHandler typeHandler;



    /** _more_          */
    private String name;

    /** _more_          */
    private String label;

    /** _more_          */
    private String description;

    /** _more_          */
    private String type;

    /** _more_          */
    private String searchType = SEARCHTYPE_TEXT;

    /** _more_          */
    private boolean isIndex;

    /** _more_          */
    private boolean canSearch;

    /** _more_          */
    private boolean canList;

    /** _more_          */
    private List values;

    /** _more_          */
    private String dflt;

    /** _more_          */
    private int size = 200;

    /** _more_          */
    private int rows = 1;

    /** _more_          */
    private int columns = 40;

    private String namesFile;

    /**
     * _more_
     *
     * @param table _more_
     * @param element _more_
     */
    public Column(GenericTypeHandler typeHandler, Element element) {
        this.typeHandler = typeHandler;
        name       = XmlUtil.getAttribute(element, ATTR_NAME);
        label      = XmlUtil.getAttribute(element, ATTR_LABEL, name);
        searchType = XmlUtil.getAttribute(element, ATTR_SEARCHTYPE,
                                          searchType);
        namesFile =XmlUtil.getAttribute(element, ATTR_NAMES,(String)null);

        description  = XmlUtil.getAttribute(element, ATTR_DESCRIPTION, label);
        type         = XmlUtil.getAttribute(element, ATTR_TYPE);
        dflt         = XmlUtil.getAttribute(element, ATTR_DEFAULT, "");
        isIndex      = XmlUtil.getAttribute(element, ATTR_ISINDEX, false);
        canSearch = XmlUtil.getAttribute(element, ATTR_CANSEARCH,
                                            false);
        canList = XmlUtil.getAttribute(element, ATTR_CANLIST,
                                            false);
        size    = XmlUtil.getAttribute(element, ATTR_SIZE, size);
        rows    = XmlUtil.getAttribute(element, ATTR_ROWS, rows);
        columns = XmlUtil.getAttribute(element, ATTR_COLUMNS, columns);
        if (type.equals(TYPE_ENUMERATION)) {
            values = StringUtil.split(XmlUtil.getAttribute(element,
                    ATTR_VALUES), ",", true, true);
        }
    }


    private boolean isNumeric() {
        return type.equals(TYPE_INT) || type.equals(TYPE_DOUBLE);
    }

    protected int formatValue(StringBuffer sb, String output, Object []values, int valueIdx) {
        if (type.equals(TYPE_LATLON)) {
            sb.append(values[valueIdx].toString());
            valueIdx++;
            sb.append(",");
            sb.append(values[valueIdx].toString());
            valueIdx++;
        } else {
            sb.append(values[valueIdx].toString());
            valueIdx++;
        }
        return valueIdx;
    }

    protected int setValues(PreparedStatement stmt, Object[]values, int valueIdx) throws Exception {
        if (type.equals(TYPE_INT)) {
            stmt.setInt(valueIdx+2, ((Integer)values[valueIdx]).intValue());
            valueIdx++;
        } else if (type.equals(TYPE_DOUBLE)) {
            stmt.setDouble(valueIdx+2, ((Double)values[valueIdx]).doubleValue());
            valueIdx++;
        } else if (type.equals(TYPE_BOOLEAN)) {
            boolean v = ((Boolean)values[valueIdx]).booleanValue();
            stmt.setInt(valueIdx+2, (v?1:0));
            valueIdx++;
        } else if (type.equals(TYPE_TIMESTAMP)) {
            Date dttm  =(Date) values[valueIdx];
            stmt.setTimestamp(valueIdx+2, new java.sql.Timestamp(dttm.getTime()));
            valueIdx++;
        } else if (type.equals(TYPE_LATLON)) {
            double lat = ((Double) values[valueIdx]).doubleValue();
            stmt.setDouble(valueIdx+2, lat);
            valueIdx++;
            double lon = ((Double) values[valueIdx]).doubleValue();
            stmt.setDouble(valueIdx+2, lon);
            valueIdx++;
        } else {
            stmt.setString(valueIdx+2, values[valueIdx].toString());
            valueIdx++;
        }
        return valueIdx;


    }


    protected int readValues(ResultSet results, Object[]values, int valueIdx) throws Exception {
        if (type.equals(TYPE_INT)) {
            values[valueIdx] = new Integer(results.getInt(valueIdx+2));
            valueIdx++;
        } else if (type.equals(TYPE_DOUBLE)) {
            values[valueIdx] = new Double(results.getDouble(valueIdx+2));
            valueIdx++;
        } else if (type.equals(TYPE_BOOLEAN)) {
            values[valueIdx] = new Boolean(results.getInt(valueIdx+2)==1);
            valueIdx++;
        } else if (type.equals(TYPE_LATLON)) {
            values[valueIdx] = new Double(results.getDouble(valueIdx+2));
            valueIdx++;
            values[valueIdx] = new Double(results.getDouble(valueIdx+2));
            valueIdx++;
        } else {
            values[valueIdx] = results.getString(valueIdx+2);
            valueIdx++;
        }
        return valueIdx;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getSqlCreate() {
        String def = " " + name + " ";
        if (type.equals(TYPE_STRING)) {
            return def + "varchar(" + size + ") ";
        } else if (type.equals(TYPE_ENUMERATION)) {
            return def + "varchar(" + size + ") ";
        } else if (type.equals(TYPE_INT)) {
            return def + "int ";
        } else if (type.equals(TYPE_DOUBLE)) {
            return def + "double ";
        } else if (type.equals(TYPE_BOOLEAN)) {
            return def + "int ";
        } else if (type.equals(TYPE_LATLON)) {
            return " " + name+"_lat double, " + name+"_lon double ";
        } else {
            throw new IllegalArgumentException("Unknown column type:" + type
                    + " for " + name);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSqlIndex() {
        if (isIndex) {
            return "CREATE INDEX " + typeHandler.getTableName() + "_INDEX_" + name + "  ON "
                   +  typeHandler.getTableName() + " (" + name + ");\n";
        } else {
            return "";
        }
    }


    protected void assembleWhereClause(Request request, List where) throws Exception {            
        String id = getFullName();
        if (type.equals(TYPE_LATLON)) {
            double lat1 = request.get(id+"_lat1",Double.NaN);
            double lat2 = request.get(id+"_lat2",Double.NaN);
            double lon1 = request.get(id+"_lon1",Double.NaN);
            double lon2 = request.get(id+"_lon2",Double.NaN);
            if(lat1==lat1 && lat2 == lat2 && lon1==lon1 && lon2 == lon2) {
                where.add(SqlUtil.ge(getFullName()+"_lat",lat1));
                where.add(SqlUtil.le(getFullName()+"_lat",lat2));
                where.add(SqlUtil.le(getFullName()+"_lon",lon1));
                where.add(SqlUtil.ge(getFullName()+"_lon",lon2));
            }
        } else if (isNumeric()) {       
            String expr = request.getCheckedString(id+"_expr",EXPR_EQUALS,EXPR_PATTERN);
            double from = request.get(id+"_from",Double.NaN);
            double to = request.get(id+"_to",Double.NaN);
            double value = request.get(id,Double.NaN);
            if(from==from && to!=to) {
                to = value;
            } else if(from!=from && to==to) {
                from = value;
            } else if(from!=from && to!=to) {
                from = value;
                to = value;
            }  
            if(from==from) {
                if(expr.equals(EXPR_EQUALS)) {
                    where.add(SqlUtil.eq(getFullName(),from));
                } else  if(expr.equals(EXPR_LE)) {
                    where.add(SqlUtil.le(getFullName(),from));
                } else  if(expr.equals(EXPR_GE)) {
                    where.add(SqlUtil.ge(getFullName(),to));
                } else  if(expr.equals(EXPR_BETWEEN)) {
                    where.add(SqlUtil.ge(getFullName(),from));
                    where.add(SqlUtil.le(getFullName(),to));
                } else if(expr.length()>0) {
                    throw new IllegalArgumentException("Unknown expression:" + expr);
                }
                System.err.println ("where:" + where);
            }
        } else if (type.equals(TYPE_BOOLEAN)) {       
            if(request.defined(id)) {
                where.add(SqlUtil.eq(getFullName(),(request.get(id,true)?1:0)));
            }
        } else {
            String value = request.getString(id,"");
            typeHandler.addOr(getFullName(),
                              (String) request.getString(getFullName(),(String)null),
                              where, !(type.equals(TYPE_INT) || type.equals(TYPE_DOUBLE)));
        }

    }

    /**
     * _more_
     *
     * @param typeHandler _more_
     * @param formBuffer _more_
     * @param headerBuffer _more_
     * @param request _more_
     * @param where _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(StringBuffer formBuffer, StringBuffer headerBuffer,
                          Request request, List where)
        throws Exception {
        
        if (!getCanSearch()) {
            return;
        }


        List tmp = new ArrayList(where);
        String widget = "";
        if (type.equals(TYPE_LATLON)) {
            widget = HtmlUtil.makeLatLonBox(getFullName(),"","","","");
        } else  if (type.equals(TYPE_BOOLEAN)) {
            widget =  HtmlUtil.select(getFullName(),Misc.newList(TypeHandler.ALL_OBJECT,
                                                                 "True","False"));
        } else  if (type.equals(TYPE_ENUMERATION)) {
            List tmpValues = Misc.newList(TypeHandler.ALL_OBJECT);
            tmpValues.addAll(values);
            widget = HtmlUtil.select(getFullName(),tmpValues);
        } else if(isNumeric()) {
            String id = getFullName();
            String expr = HtmlUtil.select(id+"_expr",EXPR_ITEMS);
            widget = expr +
                HtmlUtil.input(id+"_from", "", "size=\"10\"") +
                HtmlUtil.input(id+"_to", "", "size=\"10\"");
        } else {
            if (searchType.equals(SEARCHTYPE_SELECT)) {
                long t1  = System.currentTimeMillis();
                Statement stmt =  typeHandler.executeSelect(request,
                                                            SqlUtil.distinct(getFullName()),
                                                            tmp);
                long t2  = System.currentTimeMillis();
                String[] values = SqlUtil.readString(stmt, 1);
                long t3  = System.currentTimeMillis();
                //                System.err.println("TIME:" + (t2-t1) + " " + (t3-t2));
                List<TwoFacedObject> list = new ArrayList();
                for (int i = 0; i < values.length; i++) {
                    if(values[i] == null) continue;
                    list.add(new TwoFacedObject(getLabel(values[i]), values[i]));
                }
                if(list.size()==1) {
                    widget =  HtmlUtil.hidden(getFullName(), (String)list.get(0).getId()) + " " +list.get(0).toString();
                } else {
                    list.add(0, TypeHandler.ALL_OBJECT);
                    widget =  HtmlUtil.select(getFullName(), list);
                }
            } else if (rows > 1) {
                widget = HtmlUtil.textArea(getFullName(), "", rows, columns);
            } else {
                widget = HtmlUtil.input(getFullName(), "", "size=\"" + columns + "\"");
            }
        }
        formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold(getLabel()
                                                            + ":"), widget));
        formBuffer.append("\n");
    }


    protected String getLabel(String value) throws Exception {
        String desc = typeHandler.getRepository().getFieldDescription(value,namesFile);
        if(desc == null) desc = value;
        else {
            if(desc.indexOf("%value%")>=0) {
                desc = desc.replace("%value%", value);
            } 
        }
        return desc;
        
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getFullName() {
        return  typeHandler.getTableName() + "." + name;
    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    public List getColumnNames() {
        List names = new ArrayList();
        if (type.equals(TYPE_LATLON)) {
            names.add(name+"_lat");
            names.add(name+"_lon");
        } else {
            names.add(name);
        }
        return names;
    }


    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the Label property.
     *
     * @param value The new value for Label
     */
    public void setLabel(String value) {
        label = value;
    }

    /**
     * Get the Label property.
     *
     * @return The Label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the IsIndex property.
     *
     * @param value The new value for IsIndex
     */
    public void setIsIndex(boolean value) {
        isIndex = value;
    }

    /**
     * Get the IsIndex property.
     *
     * @return The IsIndex
     */
    public boolean getIsIndex() {
        return isIndex;
    }


    /**
     * Set the IsSearchable property.
     *
     * @param value The new value for IsSearchable
     */
    public void setCanSearch(boolean value) {
        canSearch = value;
    }

    /**
     * Get the IsSearchable property.
     *
     * @return The IsSearchable
     */
    public boolean getCanSearch() {
        return canSearch;
    }

    /**
     * Set the IsListable property.
     *
     * @param value The new value for IsListable
     */
    public void setCanList(boolean value) {
        canList = value;
    }

    /**
     * Get the IsListable property.
     *
     * @return The IsListable
     */
    public boolean getCanList() {
        return canList;
    }



    /**
     * Set the Values property.
     *
     * @param value The new value for Values
     */
    public void setValues(List value) {
        values = value;
    }

    /**
     * Get the Values property.
     *
     * @return The Values
     */
    public List getValues() {
        return values;
    }

    /**
     * Set the Dflt property.
     *
     * @param value The new value for Dflt
     */
    public void setDflt(String value) {
        dflt = value;
    }

    /**
     * Get the Dflt property.
     *
     * @return The Dflt
     */
    public String getDflt() {
        return dflt;
    }


}

