/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jdeconnectorjavautil;

import com.jdedwards.base.logging.E1Logger;
import com.jdedwards.base.logging.JdeLog;
import com.jdedwards.database.base.JDBErrorCode;
import com.jdedwards.database.base.JDBException;
import com.jdedwards.database.impl.config.JDBConfig;
import com.jdedwards.database.impl.physical.JDBConnectionManager; 
import driver.JDEIniFile;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Enumeration; 
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author jgodi
 */
public class SQLDriverCloudJava11 implements Driver {
    
    private static final String DEFAULT_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String DRIVER_LOADED = "SQLServerDriver"; 
    private static final String DRIVER_PARAMETER_IN_JDBJ = "BASE.SQLSERVER"; 
    private static E1Logger sE1Logger = JdeLog.getE1Logger(JDBConnectionManager.class);
    private static JDBConfig sConfig;
    private String originalDriver;
    private Driver localDriver;
    private Class localClass; 
    private Map<String, String> ocmServers;

    public SQLDriverCloudJava11() throws JDBException {

        sE1Logger.debug(23, this.getClass().getName() + "=============================================================== ", null, null, null);

        sE1Logger.debug(23, this.getClass().getName() + ":    Showing current Drivers Loaded using SQLDriverCloudJava11...", null, null, null);
        
        // Show
        Enumeration<Driver> driversCurrent = DriverManager.getDrivers();

        boolean driversLoaded = false;

        while (driversCurrent.hasMoreElements()) {
            Driver driver = driversCurrent.nextElement();

            sE1Logger.debug(23, this.getClass().getName()  + ":         Driver: " + driver.toString(), null, null, null);
            
            driversLoaded = true;

        }
        
        if(!driversLoaded)
        {
            sE1Logger.debug(23, this.getClass().getName()  + ":         There is not drivers loaded.", null, null, null);
        }

        sE1Logger.debug(23, "-------------------------------------------------------------------------------- ", null, null, null);

        sE1Logger.debug(23, this.getClass().getName()  + ":    Getting current JDE JDBJ configuration...", null, null, null);
        
        sConfig = JDBConfig.getInstance();

        if (sConfig.ifSectionExists("JDBj-JDBC BASE DRIVERS")) {
            
            originalDriver = sConfig.getConfigValue("JDBj-JDBC BASE DRIVERS", DRIVER_PARAMETER_IN_JDBJ);
             
            sE1Logger.debug(23, this.getClass().getName()  + ": Current Driver defined in jdbj.ini [JDBj-JDBC BASE DRIVERS]:  ", null, null, null);
            
            sE1Logger.debug(23, this.getClass().getName()  + ":                                        " + DRIVER_PARAMETER_IN_JDBJ + ":  " + originalDriver, null, null, null);
            
        } else {
            
            originalDriver = DEFAULT_DRIVER;
 
            sE1Logger.debug(23, this.getClass().getName()  + ": There is not [JDBj-JDBC BASE DRIVERS] section defined. :  ", null, null, null);
            
            sE1Logger.debug(23, this.getClass().getName()  + ":                                        Default Driver:  " + originalDriver, null, null, null);
            
            
        }

        sE1Logger.debug(23, "-------------------------------------------------------------------------------- ", null, null, null);
        
        try {

            sE1Logger.debug(23, this.getClass().getName()  + ": Loading [JDBj-JDBC BASE DRIVERS] or default Driver...", null, null, null);

            localClass = Class.forName(originalDriver);

            sE1Logger.debug(23, this.getClass().getName()  + ": [JDBj-JDBC BASE DRIVERS] or default Driver Loaded: " + originalDriver, null, null, null);

        } catch (ClassNotFoundException localClassNotFoundException) {

            sE1Logger.severe(23, this.getClass().getName() + ": ERROR ClassNotFoundException while trying to load  " + originalDriver + ", this JDBC Driver is not in the Classpath" + ". Not all of the supported JDBC Drivers have been installed. Processing will continue though.", localClassNotFoundException.getMessage(), localClassNotFoundException);

            throw new JDBException(JDBErrorCode.CONNECTION_MANAGER_BOOT_ERROR, new Throwable());

        }
        
        sE1Logger.debug(23, "-------------------------------------------------------------------------------- ", null, null, null);

        try {

            sE1Logger.debug(23, this.getClass().getName()  + ": Instancing [JDBj-JDBC BASE DRIVERS] or default driver: " + originalDriver, null, null, null);

            localDriver = (Driver) localClass.newInstance();

            sE1Logger.debug(23, this.getClass().getName()  + ": JDBj-JDBC BASE DRIVERS] or default driver instanced.", null, null, null);

        } catch (InstantiationException localInstantiationException) {

            sE1Logger.severe(23, this.getClass().getName()  +  ": ERROR: Exception occured while registering JDBC Driver " + originalDriver + ". Please ensure the " + "JDBj-JDBC DRIVERS" + " is configured correctly, processing will continue though.", localInstantiationException.getMessage(), localInstantiationException);

            throw new JDBException(JDBErrorCode.CONNECTION_MANAGER_BOOT_ERROR, new Throwable());

        } catch (IllegalAccessException localIllegalAccessException) {

            sE1Logger.severe(23, this.getClass().getName()  +  ": ERROR. Exception occured while registering JDBC Driver " + originalDriver + ". Please ensure the " + "JDBj-JDBC DRIVERS" + " is configured correctly, processing will continue though.", localIllegalAccessException.getMessage(), localIllegalAccessException);

            throw new JDBException(JDBErrorCode.CONNECTION_MANAGER_BOOT_ERROR, new Throwable());

        }

        sE1Logger.debug(23, "-------------------------------------------------------------------------------- ", null, null, null);
        
        try {

            sE1Logger.debug(23, this.getClass().getName()  + ": De-Register [JDBj-JDBC BASE DRIVERS] or default driver: " + localDriver.toString(), null, null, null);
  
            Enumeration<Driver> drivers = DriverManager.getDrivers();

            while (drivers.hasMoreElements()) {
                
                Driver driver = drivers.nextElement();

                sE1Logger.debug(23, this.getClass().getName()  + ":                 Existing Driver loaded: " + driver.toString(), null, null, null);

                if (driver.toString().contains(DRIVER_LOADED)) {
                    
                    sE1Logger.debug(23, this.getClass().getName()  + ":                 De-registering Driver: : " + driver.toString(), null, null, null);

                    DriverManager.deregisterDriver(driver);

                    sE1Logger.debug(23, this.getClass().getName()  + ":                 Driver: : " + driver.toString() + " De-registered!", null, null, null);

                }  

            }

            sE1Logger.debug(23, "-------------------------------------------------------------------------------- ", null, null, null);
            
            sE1Logger.debug(23, this.getClass().getName()  + ": Registering this driver: " + this.toString(), null, null, null);

            DriverManager.registerDriver(this);
            
            sE1Logger.debug(23, this.getClass().getName()  + ": Driver: " + this.toString() + " registered", null, null, null);

        } catch (SQLException ex) {

            sE1Logger.severe(23, this.getClass().getName()  + ": ERROR. Exception occured while registering JDBC Driver " + this.toString() + ". Please ensure the " + "JDBj-JDBC DRIVERS" + " is configured correctly, processing will continue though.", ex.getMessage(), ex);

            throw new JDBException(JDBErrorCode.CONNECTION_MANAGER_BOOT_ERROR, new Throwable());
        }
        
        sE1Logger.debug(23, "-------------------------------------------------------------------------------- ", null, null, null);

        // Show
        Enumeration<Driver> drivers = DriverManager.getDrivers();

        driversLoaded = false;
        
        sE1Logger.debug(23, this.getClass().getName() + ":    Showing current Drivers Loaded...", null, null, null);
  
        while (drivers.hasMoreElements()) {
            
            Driver driver = drivers.nextElement();

            sE1Logger.debug(23, this.getClass().getName()  + ":         Driver: " + driver.toString(), null, null, null);
            
            driversLoaded = true;

        }
        
        if(!driversLoaded)
        {
            sE1Logger.debug(23, this.getClass().getName()  + ":         There is not drivers loaded.", null, null, null);
        }
 
        sE1Logger.debug(23, "-------------------------------------------------------------------------------- ", null, null, null);
        
        sE1Logger.debug(23, this.getClass().getName() + ":    Showing current OCM Servers defined in jdeinterop.ini file...", null, null, null);
        
        String defaultPath = System.getProperty("default_path");
        
        sE1Logger.debug(23, this.getClass().getName() + ":          Reading Interop.ini File in path: " + defaultPath, null, null, null); 
        
        ocmServers = JDEIniFile.getInstance().getOCMServers(defaultPath);
          
        sE1Logger.debug(23, "=================================================================== ", null, null, null);

    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {

        sE1Logger.debug(23, this.getClass().getName() + ": Connecting to URL: " + url, null, null, null);

        String newUrl = JdbcUrlSplitter(url);

        sE1Logger.debug(23, this.getClass().getName() + ": Connecting to new URL: " + newUrl, null, null, null);

        return localDriver.connect(newUrl, info);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return localDriver.acceptsURL(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return localDriver.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return localDriver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return localDriver.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return localDriver.jdbcCompliant();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return localDriver.getParentLogger();
    }

    private String JdbcUrlSplitter(String url)   {

        String newURL = url;
         
        Iterator simpleServers = ocmServers.keySet().iterator();
        
        while(simpleServers.hasNext())
        {
            String host = (String) simpleServers.next();
            
            String hostFull = "//" + host + ":";
            
            sE1Logger.debug(23, this.getClass().getName()  + ":                Searching " + host + " in " + url, null, null, null);
            
            if(url.contains(hostFull))
            {
                sE1Logger.debug(23, this.getClass().getName()  + ":                Found " + host + " in " + url, null, null, null);
                
                String newHost = (String) ocmServers.get(host);
                
                sE1Logger.debug(23, this.getClass().getName()  + ":                 New Host " + newHost, null, null, null);
                
                if (!newHost.equals(host)) {
                    
                    newURL = newURL.replaceAll(host, newHost); 
                    
                    sE1Logger.debug(23, this.getClass().getName()  + ":                 New URL " + newURL, null, null, null);
                    
                    break;
                    
                }
                
            }
            else
            {
                sE1Logger.debug(23, this.getClass().getName()  + ":                Not Found " + host + " in " + url, null, null, null);
            }
              
        }
        
        return newURL;

    } 
}
