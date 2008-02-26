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


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public interface Tables {



    /*
      For each of the tables in the database we have the following defs.
      The TABLE_<TABLE NAME> ... is the name of the table.
      The COL_<TABLE NAME>_<COLUMN NAME> is the name of the column in the table
      The ARRAY_<TABLE NAME> is the array of column names
      The COLUMNS_<TABLE NAME> is the comma separated list of columns in the table
      The J- and J+ turn off jindent formatting
     */

//J+
/** begin generated table definitions**/

public static final String TABLE_ASSOCIATIONS  = "associations";
public static final String COL_ASSOCIATIONS_NAME = TABLE_ASSOCIATIONS+".name";
public static final String COL_ASSOCIATIONS_FROM_ENTRY_ID = TABLE_ASSOCIATIONS+".from_entry_id";
public static final String COL_ASSOCIATIONS_TO_ENTRY_ID = TABLE_ASSOCIATIONS+".to_entry_id";

public static final String []ARRAY_ASSOCIATIONS= new String[]{
COL_ASSOCIATIONS_NAME,
COL_ASSOCIATIONS_FROM_ENTRY_ID,
COL_ASSOCIATIONS_TO_ENTRY_ID};

public static final String COLUMNS_ASSOCIATIONS = SqlUtil.comma(ARRAY_ASSOCIATIONS);
public static final String NODOT_COLUMNS_ASSOCIATIONS = SqlUtil.commaNoDot(ARRAY_ASSOCIATIONS);
public static final String INSERT_ASSOCIATIONS=
SqlUtil.makeInsert(
TABLE_ASSOCIATIONS,
NODOT_COLUMNS_ASSOCIATIONS,
SqlUtil.getQuestionMarks(ARRAY_ASSOCIATIONS.length));



public static final String TABLE_BOREHOLE  = "borehole";
public static final String COL_BOREHOLE_ID = TABLE_BOREHOLE+".id";
public static final String COL_BOREHOLE_MODELGROUP = TABLE_BOREHOLE+".modelgroup";
public static final String COL_BOREHOLE_MODELRUN = TABLE_BOREHOLE+".modelrun";

public static final String []ARRAY_BOREHOLE= new String[]{
COL_BOREHOLE_ID,
COL_BOREHOLE_MODELGROUP,
COL_BOREHOLE_MODELRUN};

public static final String COLUMNS_BOREHOLE = SqlUtil.comma(ARRAY_BOREHOLE);
public static final String NODOT_COLUMNS_BOREHOLE = SqlUtil.commaNoDot(ARRAY_BOREHOLE);
public static final String INSERT_BOREHOLE=
SqlUtil.makeInsert(
TABLE_BOREHOLE,
NODOT_COLUMNS_BOREHOLE,
SqlUtil.getQuestionMarks(ARRAY_BOREHOLE.length));



public static final String TABLE_COMMENTS  = "comments";
public static final String COL_COMMENTS_ID = TABLE_COMMENTS+".id";
public static final String COL_COMMENTS_ENTRY_ID = TABLE_COMMENTS+".entry_id";
public static final String COL_COMMENTS_USER_ID = TABLE_COMMENTS+".user_id";
public static final String COL_COMMENTS_DATE = TABLE_COMMENTS+".date";
public static final String COL_COMMENTS_SUBJECT = TABLE_COMMENTS+".subject";
public static final String COL_COMMENTS_COMMENT = TABLE_COMMENTS+".comment";

public static final String []ARRAY_COMMENTS= new String[]{
COL_COMMENTS_ID,
COL_COMMENTS_ENTRY_ID,
COL_COMMENTS_USER_ID,
COL_COMMENTS_DATE,
COL_COMMENTS_SUBJECT,
COL_COMMENTS_COMMENT};

public static final String COLUMNS_COMMENTS = SqlUtil.comma(ARRAY_COMMENTS);
public static final String NODOT_COLUMNS_COMMENTS = SqlUtil.commaNoDot(ARRAY_COMMENTS);
public static final String INSERT_COMMENTS=
SqlUtil.makeInsert(
TABLE_COMMENTS,
NODOT_COLUMNS_COMMENTS,
SqlUtil.getQuestionMarks(ARRAY_COMMENTS.length));



public static final String TABLE_DUMMY  = "dummy";
public static final String COL_DUMMY_NAME = TABLE_DUMMY+".name";

public static final String []ARRAY_DUMMY= new String[]{
COL_DUMMY_NAME};

public static final String COLUMNS_DUMMY = SqlUtil.comma(ARRAY_DUMMY);
public static final String NODOT_COLUMNS_DUMMY = SqlUtil.commaNoDot(ARRAY_DUMMY);
public static final String INSERT_DUMMY=
SqlUtil.makeInsert(
TABLE_DUMMY,
NODOT_COLUMNS_DUMMY,
SqlUtil.getQuestionMarks(ARRAY_DUMMY.length));



public static final String TABLE_ENTRIES  = "entries";
public static final String COL_ENTRIES_ID = TABLE_ENTRIES+".id";
public static final String COL_ENTRIES_TYPE = TABLE_ENTRIES+".type";
public static final String COL_ENTRIES_NAME = TABLE_ENTRIES+".name";
public static final String COL_ENTRIES_DESCRIPTION = TABLE_ENTRIES+".description";
public static final String COL_ENTRIES_PARENT_GROUP_ID = TABLE_ENTRIES+".parent_group_id";
public static final String COL_ENTRIES_USER_ID = TABLE_ENTRIES+".user_id";
public static final String COL_ENTRIES_RESOURCE = TABLE_ENTRIES+".resource";
public static final String COL_ENTRIES_RESOURCE_TYPE = TABLE_ENTRIES+".resource_type";
public static final String COL_ENTRIES_CREATEDATE = TABLE_ENTRIES+".createdate";
public static final String COL_ENTRIES_FROMDATE = TABLE_ENTRIES+".fromdate";
public static final String COL_ENTRIES_TODATE = TABLE_ENTRIES+".todate";
public static final String COL_ENTRIES_SOUTH = TABLE_ENTRIES+".south";
public static final String COL_ENTRIES_NORTH = TABLE_ENTRIES+".north";
public static final String COL_ENTRIES_EAST = TABLE_ENTRIES+".east";
public static final String COL_ENTRIES_WEST = TABLE_ENTRIES+".west";

public static final String []ARRAY_ENTRIES= new String[]{
COL_ENTRIES_ID,
COL_ENTRIES_TYPE,
COL_ENTRIES_NAME,
COL_ENTRIES_DESCRIPTION,
COL_ENTRIES_PARENT_GROUP_ID,
COL_ENTRIES_USER_ID,
COL_ENTRIES_RESOURCE,
COL_ENTRIES_RESOURCE_TYPE,
COL_ENTRIES_CREATEDATE,
COL_ENTRIES_FROMDATE,
COL_ENTRIES_TODATE,
COL_ENTRIES_SOUTH,
COL_ENTRIES_NORTH,
COL_ENTRIES_EAST,
COL_ENTRIES_WEST};

public static final String COLUMNS_ENTRIES = SqlUtil.comma(ARRAY_ENTRIES);
public static final String NODOT_COLUMNS_ENTRIES = SqlUtil.commaNoDot(ARRAY_ENTRIES);
public static final String INSERT_ENTRIES=
SqlUtil.makeInsert(
TABLE_ENTRIES,
NODOT_COLUMNS_ENTRIES,
SqlUtil.getQuestionMarks(ARRAY_ENTRIES.length));

public static final String UPDATE_ENTRIES =
SqlUtil.makeUpdate(
TABLE_ENTRIES,
COL_ENTRIES_ID,
ARRAY_ENTRIES);


public static final String TABLE_GLOBALS  = "globals";
public static final String COL_GLOBALS_NAME = TABLE_GLOBALS+".name";
public static final String COL_GLOBALS_VALUE = TABLE_GLOBALS+".value";

public static final String []ARRAY_GLOBALS= new String[]{
COL_GLOBALS_NAME,
COL_GLOBALS_VALUE};

public static final String COLUMNS_GLOBALS = SqlUtil.comma(ARRAY_GLOBALS);
public static final String NODOT_COLUMNS_GLOBALS = SqlUtil.commaNoDot(ARRAY_GLOBALS);
public static final String INSERT_GLOBALS=
SqlUtil.makeInsert(
TABLE_GLOBALS,
NODOT_COLUMNS_GLOBALS,
SqlUtil.getQuestionMarks(ARRAY_GLOBALS.length));



public static final String TABLE_HARVESTERS  = "harvesters";
public static final String COL_HARVESTERS_ID = TABLE_HARVESTERS+".id";
public static final String COL_HARVESTERS_CLASS = TABLE_HARVESTERS+".class";
public static final String COL_HARVESTERS_CONTENT = TABLE_HARVESTERS+".content";

public static final String []ARRAY_HARVESTERS= new String[]{
COL_HARVESTERS_ID,
COL_HARVESTERS_CLASS,
COL_HARVESTERS_CONTENT};

public static final String COLUMNS_HARVESTERS = SqlUtil.comma(ARRAY_HARVESTERS);
public static final String NODOT_COLUMNS_HARVESTERS = SqlUtil.commaNoDot(ARRAY_HARVESTERS);
public static final String INSERT_HARVESTERS=
SqlUtil.makeInsert(
TABLE_HARVESTERS,
NODOT_COLUMNS_HARVESTERS,
SqlUtil.getQuestionMarks(ARRAY_HARVESTERS.length));



public static final String TABLE_METADATA  = "metadata";
public static final String COL_METADATA_ID = TABLE_METADATA+".id";
public static final String COL_METADATA_ENTRY_ID = TABLE_METADATA+".entry_id";
public static final String COL_METADATA_TYPE = TABLE_METADATA+".type";
public static final String COL_METADATA_ATTR1 = TABLE_METADATA+".attr1";
public static final String COL_METADATA_ATTR2 = TABLE_METADATA+".attr2";
public static final String COL_METADATA_ATTR3 = TABLE_METADATA+".attr3";
public static final String COL_METADATA_ATTR4 = TABLE_METADATA+".attr4";

public static final String []ARRAY_METADATA= new String[]{
COL_METADATA_ID,
COL_METADATA_ENTRY_ID,
COL_METADATA_TYPE,
COL_METADATA_ATTR1,
COL_METADATA_ATTR2,
COL_METADATA_ATTR3,
COL_METADATA_ATTR4};

public static final String COLUMNS_METADATA = SqlUtil.comma(ARRAY_METADATA);
public static final String NODOT_COLUMNS_METADATA = SqlUtil.commaNoDot(ARRAY_METADATA);
public static final String INSERT_METADATA=
SqlUtil.makeInsert(
TABLE_METADATA,
NODOT_COLUMNS_METADATA,
SqlUtil.getQuestionMarks(ARRAY_METADATA.length));



public static final String TABLE_PERMISSIONS  = "permissions";
public static final String COL_PERMISSIONS_ENTRY_ID = TABLE_PERMISSIONS+".entry_id";
public static final String COL_PERMISSIONS_ACTION = TABLE_PERMISSIONS+".action";
public static final String COL_PERMISSIONS_ROLE = TABLE_PERMISSIONS+".role";

public static final String []ARRAY_PERMISSIONS= new String[]{
COL_PERMISSIONS_ENTRY_ID,
COL_PERMISSIONS_ACTION,
COL_PERMISSIONS_ROLE};

public static final String COLUMNS_PERMISSIONS = SqlUtil.comma(ARRAY_PERMISSIONS);
public static final String NODOT_COLUMNS_PERMISSIONS = SqlUtil.commaNoDot(ARRAY_PERMISSIONS);
public static final String INSERT_PERMISSIONS=
SqlUtil.makeInsert(
TABLE_PERMISSIONS,
NODOT_COLUMNS_PERMISSIONS,
SqlUtil.getQuestionMarks(ARRAY_PERMISSIONS.length));



public static final String TABLE_USER_ROLES  = "user_roles";
public static final String COL_USER_ROLES_USER_ID = TABLE_USER_ROLES+".user_id";
public static final String COL_USER_ROLES_ROLE = TABLE_USER_ROLES+".role";

public static final String []ARRAY_USER_ROLES= new String[]{
COL_USER_ROLES_USER_ID,
COL_USER_ROLES_ROLE};

public static final String COLUMNS_USER_ROLES = SqlUtil.comma(ARRAY_USER_ROLES);
public static final String NODOT_COLUMNS_USER_ROLES = SqlUtil.commaNoDot(ARRAY_USER_ROLES);
public static final String INSERT_USER_ROLES=
SqlUtil.makeInsert(
TABLE_USER_ROLES,
NODOT_COLUMNS_USER_ROLES,
SqlUtil.getQuestionMarks(ARRAY_USER_ROLES.length));



public static final String TABLE_USERROLES  = "userroles";
public static final String COL_USERROLES_USER_ID = TABLE_USERROLES+".user_id";
public static final String COL_USERROLES_ROLE = TABLE_USERROLES+".role";

public static final String []ARRAY_USERROLES= new String[]{
COL_USERROLES_USER_ID,
COL_USERROLES_ROLE};

public static final String COLUMNS_USERROLES = SqlUtil.comma(ARRAY_USERROLES);
public static final String NODOT_COLUMNS_USERROLES = SqlUtil.commaNoDot(ARRAY_USERROLES);
public static final String INSERT_USERROLES=
SqlUtil.makeInsert(
TABLE_USERROLES,
NODOT_COLUMNS_USERROLES,
SqlUtil.getQuestionMarks(ARRAY_USERROLES.length));



public static final String TABLE_USERS  = "users";
public static final String COL_USERS_ID = TABLE_USERS+".id";
public static final String COL_USERS_NAME = TABLE_USERS+".name";
public static final String COL_USERS_EMAIL = TABLE_USERS+".email";
public static final String COL_USERS_QUESTION = TABLE_USERS+".question";
public static final String COL_USERS_ANSWER = TABLE_USERS+".answer";
public static final String COL_USERS_PASSWORD = TABLE_USERS+".password";
public static final String COL_USERS_ADMIN = TABLE_USERS+".admin";
public static final String COL_USERS_LANGUAGE = TABLE_USERS+".language";

public static final String []ARRAY_USERS= new String[]{
COL_USERS_ID,
COL_USERS_NAME,
COL_USERS_EMAIL,
COL_USERS_QUESTION,
COL_USERS_ANSWER,
COL_USERS_PASSWORD,
COL_USERS_ADMIN,
COL_USERS_LANGUAGE};

public static final String COLUMNS_USERS = SqlUtil.comma(ARRAY_USERS);
public static final String NODOT_COLUMNS_USERS = SqlUtil.commaNoDot(ARRAY_USERS);
public static final String INSERT_USERS=
SqlUtil.makeInsert(
TABLE_USERS,
NODOT_COLUMNS_USERS,
SqlUtil.getQuestionMarks(ARRAY_USERS.length));





/** end generated table definitions**/

//J+

}

