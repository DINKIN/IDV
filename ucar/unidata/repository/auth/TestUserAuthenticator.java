/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

import ucar.unidata.repository.Repository;
import ucar.unidata.repository.Request;
import ucar.unidata.repository.auth.User;

import ucar.unidata.repository.auth.UserAuthenticatorImpl;




import java.util.ArrayList;
import java.util.List;


/**
 * This is an example of an implementation of the UserAuthenticator interface.
 * This allows one to plugin their own user authentication. To use this make you 
 * own class just like this one and implement the below methods. Put the compiled .class
 * files into your RAMADDA home plugins directory and restart RAMADDA. (Or make a jar file of
 * the classes and put the jar in the plugins dir.). RAMADDA will instantiate of of these
 * classes and use it to do user authentication and management.
 *
 *
 * @author Jeff McWhirter
 */
public class TestUserAuthenticator extends UserAuthenticatorImpl {


    /**
     * _more_
     */
    public TestUserAuthenticator() {}

    /**
     * this gets called when we want to just get a User object from the ID.
     * return null if user is unknown
     *
     * @param repository _more_
     * @param userId _more_
     *
     * @return _more_
     */
    public User findUser(Repository repository, String userId) {
        //Create the xxx user
        if (userId.equals("xxx")) {
            //The false says this is not an administrator
            User user = new User(userId, "Some name for user xxx", false);
            user.setEmail("xxx@foo.bar");
            ArrayList<String> roles = new ArrayList<String>();
            roles.add("some role1");
            roles.add("some role2");
            user.setRoles(roles);
            return user;
        }
        return null;
    }

    /**
     * this gets called when we want to  autheticate the given user/password
     * return null if user/password is unknown or incorrect
     *
     * @param repository _more_
     * @param request _more_
     * @param extraLoginForm _more_
     * @param userId _more_
     * @param password _more_
     *
     * @return _more_
     */
    public User authenticateUser(Repository repository, Request request,
                                 StringBuffer extraLoginForm, String userId,
                                 String password) {
        System.err.println("test authenticator authenticateUser: " + userId);
        //This example will create a User for userid=xxx password=yyy
        if (userId.equals("xxx") && password.equals("yyy")) {
            return findUser(repository, userId);
        }
        return null;
    }


    /**
     * This is used to list out the roles in the access pages
     *
     * @return _more_
     */
    public List<String> getAllRoles() {
        ArrayList<String> roles = new ArrayList<String>();
        roles.add("some role1");
        roles.add("some role2");
        return roles;
    }

    /**
     * this can be used to list out all of the users and display them
     * in RAMADDA
     * It is not used by RAMADDA right now
     *
     * @return _more_
     */
    public List<User> getAllUsers() {
        return new ArrayList<User>();
    }


    /**
     * This will be used to allow this authenticator to add options
     * to the admin config form
     * Its not used right now
     *
     * @param repository _more_
     * @param sb _more_
     */
    public void addToConfigurationForm(Repository repository,
                                       StringBuffer sb) {}

    /**
     * This will be used to allow this authenticator to set the options from the config form
     * to the admin config form
     * Its not used right now
     *
     * @param repository _more_
     * @param request _more_
     */
    public void applyConfigurationForm(Repository repository,
                                       Request request) {}

}
