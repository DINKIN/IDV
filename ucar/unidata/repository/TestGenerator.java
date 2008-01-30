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
public class TestGenerator extends Harvester {

    /** _more_          */
    private TypeHandler typeHandler;


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     *
     * @throws Exception _more_
     */
    public TestGenerator(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        this.typeHandler = repository.getTypeHandler(TypeHandler.TYPE_FILE);
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
        List<Entry> entries = new ArrayList<Entry>();
        final User  user    = repository.getUserManager().getDefaultUser();
        int         cnt     = 0;
        List        groups  = new ArrayList();
        for (int j = 0; j < 100; j++) {
            Group group = repository.findGroupFromName("Test/Generated/"
                              + "Group" + j, user, true);
            groups.add(group);
        }

        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 100; j++) {
                Group group = (Group) groups.get(j);
                for (int k = 0; k < 10; k++) {
                    Date createDate = new Date();
                    Entry entry =
                        typeHandler.createEntry(repository.getGUID());
                    entry.init("test_" + i + "_" + j + "_" + k, "", group,
                               user, new Resource("", Resource.TYPE_UNKNOWN),
                               createDate.getTime(), createDate.getTime(),
                               createDate.getTime(), null);
                    entries.add(entry);
                    typeHandler.initializeNewEntry(entry);
                    cnt++;
                    if ( !getActive()) {
                        return;
                    }
                    if (entries.size() > 5000) {
                        repository.insertEntries(entries, true, true);
                        entries = new ArrayList<Entry>();
                    }
                }
            }
            //            System.err.println("  Added:" + cnt);
        }


    }





}

