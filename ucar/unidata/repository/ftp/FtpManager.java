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


package ucar.unidata.repository.ftp;


import org.apache.ftpserver.*;
import org.apache.ftpserver.ssl.*;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.*;
import org.apache.ftpserver.usermanager.*;
import org.apache.ftpserver.usermanager.impl.*;




import ucar.unidata.repository.*;

import java.io.IOException;
import java.io.File;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;




/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class FtpManager extends RepositoryManager {
    private FtpServer server;
    private int port=-1;

    /**
     * _more_
     *
     * @param repository _more_
     */
    public FtpManager(Repository repository) {
        super(repository);
        try {
	    checkServer();
        } catch (Exception exc) {
            logError("Creating FTP server", exc);
        }
    }


    public void checkServer() throws Exception {
	int newPort = getRepository().getProperty(PROP_FTP_PORT,-1);
	if(newPort<0) {
	    stop();
	    return;
	} 
	if(newPort!=port) {
	    stop();
	}

	port  = newPort;
	if(server == null) {
	    initFtpServer();
	}

    }


    private void stop() {
	if(server!=null) {
	    server.stop();
	}
	server= null;
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void initFtpServer() throws Exception {

        FtpServerFactory serverFactory = new FtpServerFactory();

        Hashtable        ftplets = new Hashtable<java.lang.String, Ftplet>();
        ftplets.put("default", new RepositoryFtplet(this));
        serverFactory.setFtplets(ftplets);
	
        ListenerFactory factory = new ListenerFactory();
        // set the port of the listener
        factory.setPort(port);


        DataConnectionConfigurationFactory dccf = new DataConnectionConfigurationFactory();
        String passive = "44000-44100";
        logInfo("FTP: setting passive ports to:" + passive);
        dccf.setPassivePorts(passive);
        factory.setDataConnectionConfiguration(dccf.createDataConnectionConfiguration());

        File keystore =
            new File(getRepository().getPropertyValue(PROP_SSL_KEYSTORE,
                getRepository().getStorageManager().getRepositoryDir()
                + "/keystore", false));
	
        if (keystore.exists()) {
	    logInfo("FTP: using FTPS");
	    String password = getRepository().getPropertyValue(PROP_SSL_PASSWORD,
							       (String) null, false);
	    String keyPassword = getRepository().getPropertyValue(PROP_SSL_PASSWORD,
								  password, false);

	    SslConfigurationFactory ssl = new SslConfigurationFactory();
	    ssl.setKeystoreFile(keystore);
	    ssl.setKeystorePassword(keyPassword);

	    factory.setSslConfiguration(ssl.createSslConfiguration());
	    factory.setImplicitSsl(true);
	}




        // replace the default listener
        serverFactory.addListener("default", factory.createListener());


        serverFactory.setUserManager(new RepositoryFtpUserManager(this));

        // start the server
        server = serverFactory.createServer();
        server.start();
	logInfo("FTP: starting server on port:" + port);
    }



}

