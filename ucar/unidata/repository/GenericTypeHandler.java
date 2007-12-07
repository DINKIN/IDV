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


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

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

    /** _more_          */
    List<Column> columns;


    /**
     * _more_
     *
     * @param repository _more_
     * @param type _more_
     * @param description _more_
     * @param columns _more_
     */
    public GenericTypeHandler(Repository repository, String type,
                              String description, List<Column> columns) {
        super(repository, type, description);
        this.columns = columns;
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
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List assembleWhereClause(Request request) throws Exception {
        List    where        = super.assembleWhereClause(request);
        boolean hadBaseWhere = where.size() > 0;
        boolean didOne       = false;
        for (Column column : columns) {
            if ( !column.getIsSearchable()) {
                continue;
            }
            didOne |= addOr(column.getFullName(),
                            (String) request.get(column.getFullName()),
                            where, true);
        }
        if (didOne && hadBaseWhere) {
            where.add(SqlUtil.eq(COL_ENTRIES_ID, getTableName() + ".id"));
        }
        return where;
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
            if ( !column.getIsSearchable()) {
                continue;
            }
            String value = request.get(column.getFullName(), "");
            if (value.trim().length() > 0) {
                initTables.add(getTableName());
                break;
            }
        }
        return initTables;
    }

    /**
     * _more_
     *
     * @param formBuffer _more_
     * @param headerBuffer _more_
     * @param request _more_
     * @param where _more_
     *
     * @throws Exception _more_
     */
    public void addToForm(StringBuffer formBuffer, StringBuffer headerBuffer,
                          Request request, List where)
            throws Exception {
        super.addToForm(formBuffer, headerBuffer, request, where);
        for (Column column : columns) {
            column.addToForm(this, formBuffer, headerBuffer, request, where);
        }
    }


}

