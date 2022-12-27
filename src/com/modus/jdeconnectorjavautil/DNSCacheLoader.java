/**
 * Copyright (C) 2016, W Modus LLC. All rights reserved.
 */
package com.modus.jdeconnectorjavautil;

import java.io.IOException; 
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry; 


public class DNSCacheLoader {
 
    private static final String cachePolicyProp = "networkaddress.cache.ttl";
    private HashMap<String, String> ocmServers;

    public static DNSCacheLoader getInstance() {
        return DNSCacheLoaderHolder.INSTANCE;
    }

    private static class DNSCacheLoaderHolder {

        private DNSCacheLoaderHolder() {
        }

        private static final DNSCacheLoader INSTANCE = new DNSCacheLoader();
    }
    
    public void loadServers() throws IOException
    {
        this.ocmServers = new HashMap();
         this.ocmServers.put("jdesrvlogic","129.146.79.101");
         this.ocmServers.put("jdedbs","129.146.122.94"); 
          
    }
    
    public void setOCMServer(String jdeServerName, String fqdnOrIP)
    {
         
        
    }

    public void loadJDEServersInInetAddressCache()
            throws IOException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        if (this.ocmServers == null) {

            System.out.println("Loading OCM Servers in DNS Cache ...");

            String networkaddressCacheTTL = System.getProperty("networkaddress.cache.ttl");

            if (networkaddressCacheTTL != null) {
                System.out.println("Property 'networkaddress.cache.ttl' value is " + networkaddressCacheTTL);
            }

            showPolicy();
 
        }

        if (this.ocmServers != null && !ocmServers.isEmpty()) {

            if (ocmServers.size() > 0) {

                Iterator<Entry<String, String>> serverValues = ocmServers.entrySet()
                        .iterator();

                while (serverValues.hasNext()) {

                    // Get DNS Record

                    Entry<String, String> dsnRecord = serverValues.next();

                    String jdeServerName = dsnRecord.getKey()
                            .trim();

                    String fqdnOrIP = dsnRecord.getValue()
                            .trim();

                    System.out.println("Processing JDE Server Name Mapping: [" + jdeServerName + "/" + fqdnOrIP + "]");

                    // Validate jdeServerName

                    InetAddress ipJDEServer = null;

                    boolean jdeServerValid = false;

                    System.out.println("MULESOFT - DNSCacheLoader: loadOCMServerInDNSCache: Validation JDE Server Name: [" + jdeServerName + "] ... ");

                    try {

                        ipJDEServer = InetAddress.getByName(jdeServerName);

                        byte[] ip = ipJDEServer.getAddress();

                        System.out.println("         JDE Server: [" + jdeServerName + "] has IP [" + getIPString(ip) + "]");

                        jdeServerValid = true;

                    } catch (UnknownHostException e) { // NOSONAR

                        System.out.println("         JDE Server: [" + jdeServerName + "] hasn't IP. Msg:" + e.getMessage());

                    }

                    // get IP address from FQDN or IP

                    if (!jdeServerValid) {

                        System.out.println("         Validation FQDN or IP: [" + fqdnOrIP + "] ... ");

                        try {

                            ipJDEServer = InetAddress.getByName(fqdnOrIP);

                            byte[] ip = ipJDEServer.getAddress();

                            System.out.println("         JDE Server: [" + fqdnOrIP + "] has IP [" + getIPString(ip) + "]");

                            // Adding JDE Server name to DNS Cache

                            System.out.println("         Adding JDE Server name to DNS Cache ...");

                            registerHost(jdeServerName, ipJDEServer);

                            System.out.println("         JDE Server [" + jdeServerName + "] has been added to DNS Cache with this IP: ["
                                    + getIPString(ip) + "]");

                        } catch (UnknownHostException e) { // NOSONAR

                            System.out.println("         JDE Server: [" + fqdnOrIP + "] hasn't IP. Msg:" + e.getMessage());

                        }

                    }

                }

            }

        } else {

            System.out.println("There isn't [OCM_SERVERS] section inside jdeinterop.ini file");

        }

    }

    private void showPolicy() {

        Integer tmp = null;

        try {

            tmp = Integer.valueOf(java.security.AccessController.doPrivileged(
                    new PrivilegedAction<String>() {

                        public String run() {
                            return Security.getProperty(cachePolicyProp);
                        }
                    }));

        } catch (NumberFormatException e) {
            // Ignore
        }

        if (tmp != null) {
            int cachePolicy = tmp.intValue();
            System.out.println("Cache Policy: " + cachePolicy);

        } else {
            System.out.println("MULESOFT - DNSCacheLoader: loadOCMServerInDNSCache: Cache Policy doesn't exist");
        }

    }

    private void registerHost(String host, InetAddress... ip)
            throws NoSuchMethodException, SecurityException, UnknownHostException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Method method = InetAddress.class.getDeclaredMethod("cacheAddresses", String.class, InetAddress[].class, Boolean.TYPE);
        method.setAccessible(true);
        InetAddress someInetAddress = InetAddress.getLocalHost();
        method.invoke(someInetAddress, host, ip, true);

    }

    public Map<String, String> getOcmServers() {
        return ocmServers;
    }

    public void clean() {
        this.ocmServers = null;
    }

    private String getIPString(byte[] ipInBytes) {

        StringBuilder ipSB = new StringBuilder();

        int temp = 0;

        for (int i = 0, j = ipInBytes.length; i < j; i++) {

            temp = (int) (ipInBytes[i] & 255);

            if (i != 3) {
                ipSB.append(temp)
                        .append(".");
            } else {
                ipSB.append(temp);
            }
        }

        return ipSB.toString();
    }

}
