/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jdeconnectorjavautil;

import com.jdedwards.system.connector.dynamic.ServerFailureException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

/**
 *
 * @author jgodi
 */
public class JDETestDNSJava11 {
    
    public static void test(CommandLine line,Options options)  {
        
        String[] versionElements = System.getProperty("java.version").split("\\.");
        
        
                    System.out.println("-------------------------------------------------------");
                    System.out.println("Register Enterprise Server JDE-ALPHA-ENT 138.91.73.161");
                    System.out.println("-------------------------------------------------------");
                     
                    try {
                        
                        InetAddress ipJDEServerTmp = InetAddress.getByName("138.91.73.161");
                        
                        DNSCacheLoaderJava11.registerHostOpenJDK11("JDE-ALPHA-ENT", ipJDEServerTmp);
                        
                        System.out.println("Server JDE-ALPHA-ENT registered with 138.91.73.161");
                    
                    } catch (UnknownHostException ex) {
                        System.out.println("Unexpected exception: " + ex.getMessage());
                    } catch (IOException ex) {
                        System.out.println("Unexpected exception: " + ex.getMessage());
                    }
                    
         
                    System.out.println(Arrays.toString(versionElements));
                     
                    System.out.println("-------------------------------------------------------");
                    System.out.println("Get Server By Name - JDE-ALPHA-ENT");
                    System.out.println("-------------------------------------------------------");
                    
                    try {
            
                        InetAddress ipJDEServer = InetAddress.getByName("JDE-ALPHA-ENT");
                        
                        byte[] ip = ipJDEServer.getAddress(); 
                        
                        System.out.println("Server JDE-ALPHA-ENT IP: " + DNSCacheLoaderJava11.getIPString(ip));


                    } catch (UnknownHostException ex) {
                         System.out.println("Unexpected exception: " + ex.getMessage());
                    }
                     
                    
                    System.out.println("-------------------------------------------------------");
                    System.out.println("Register SQL Server JDE-ALPHA-SQL 65.52.119.187");
                    System.out.println("-------------------------------------------------------");
                     
                    try {
                        
                        InetAddress ipJDEServerTmp = InetAddress.getByName("65.52.119.187");
                        
                        DNSCacheLoaderJava11.registerHostOpenJDK11("JDE-ALPHA-SQL", ipJDEServerTmp);
                        
                        System.out.println("Server JDE-ALPHA-SQL registered with 65.52.119.187");
                    
                    } catch (UnknownHostException ex) {
                        System.out.println("Unexpected exception: " + ex.getMessage());
                    } catch (IOException ex) {
                        System.out.println("Unexpected exception: " + ex.getMessage());
                    }
                    
                    System.out.println("-------------------------------------------------------");
                    System.out.println("Get Server By Name  - JDE-ALPHA-SQL");
                    System.out.println("-------------------------------------------------------");
                    
                    try {
            
                        InetAddress ipJDEServer = InetAddress.getByName("JDE-ALPHA-SQL");
                        byte[] ip = ipJDEServer.getAddress(); 
                        System.out.println("Server JDE-ALPHA-SQL IP: " + DNSCacheLoaderJava11.getIPString(ip));
 
                    } catch (UnknownHostException ex) {
                         System.out.println("Unexpected exception: " + ex.getMessage());
                    }
                    
                    
                    System.out.println("-------------------------------------------------------");
                    System.out.println("JDE LOGIN");
                    System.out.println("-------------------------------------------------------");
         
                    
                    if (line.hasOption("jde_user") && line.hasOption("jde_pwd") && line.hasOption("jde_role") && line.hasOption("jde_environment") && line.hasOption("sleep_time") && line.hasOption("calls")) {

                        System.out.println(" jde_user: " + line.getOptionValue("jde_user"));
                        System.out.println(" jde_pwd: " + line.getOptionValue("jde_pwd"));
                        System.out.println(" jde_role: " + line.getOptionValue("jde_role"));
                        System.out.println(" jde_environment: " + line.getOptionValue("jde_environment"));
                        System.out.println(" sleep_time: " + line.getOptionValue("sleep_time"));
                        System.out.println(" calls: " + line.getOptionValue("calls"));

                        JDEConnectorJavaUtil util = new JDEConnectorJavaUtil();

                        try {

                            util.testDNSLogin(line.getOptionValue("jde_user"), line.getOptionValue("jde_pwd"), line.getOptionValue("jde_environment"), line.getOptionValue("jde_role"), line.getOptionValue("sleep_time"), line.getOptionValue("calls"));
                             

                        } catch (ServerFailureException ex) {
                            System.out.println("Unexpected exception:" + ex.getMessage());
                        } catch (InterruptedException ex) {
                            System.out.println("Unexpected exception:" + ex.getMessage());
                        } catch (IOException ex) {
                            System.out.println("Unexpected exception:" + ex.getMessage());
                        } catch (NoSuchMethodException ex) {
                            System.out.println("Unexpected exception:" + ex.getMessage());
                        } catch (SecurityException ex) {
                            System.out.println("Unexpected exception:" + ex.getMessage());
                        } catch (IllegalAccessException ex) {
                            System.out.println("Unexpected exception:" + ex.getMessage());
                        } catch (IllegalArgumentException ex) {
                            System.out.println("Unexpected exception:" + ex.getMessage());
                        } catch (InvocationTargetException ex) {
                            System.out.println("Unexpected exception:" + ex.getMessage());
                        }
 
                    } else {
                        HelpFormatter formatter = new HelpFormatter();
                        formatter.printHelp("java -jar JDEConnectorJavaUtil.jar", options);
                    }
        
    }
}
