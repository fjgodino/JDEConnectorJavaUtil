/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Singleton.java to edit this template
 */
package jdeconnectorjavautil;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author jgodi
 */
public class DNSCacheLoaderJava11 {
    
    public static void registerHostOpenJDK11(String host, InetAddress ip) throws IOException {

        try {

            // -------------------------------------------------------
            // Create NameServiceAddresses using Reflection
            // -------------------------------------------------------
 
            Class<?> naClass = Class.forName("java.net.InetAddress$CachedAddresses");
           
            Constructor<?> naConstructor = naClass.getDeclaredConstructor(String.class, InetAddress[].class, Long.TYPE);
            naConstructor.setAccessible(true);
            System.out.println("         java.net.InetAddress$CachedAddresses constructor accessible: " + naConstructor.toString());
             
            InetAddress ips[] = new InetAddress[1];
            ips[0] = ip;

            Object naObject = naConstructor.newInstance(host, ips, 100000L); 
            System.out.println("         java.net.InetAddress$NameServiceAddresses instance: " + naObject.toString());
            

            // -------------------------------------------------------
            // Loading Cache
            // -------------------------------------------------------

            Class<?> inetAdressClass = Class.forName("java.net.InetAddress");
            System.out.println("         java.net.InetAddress loaded ");
             
            Field field = inetAdressClass.getDeclaredField("cache"); 
            System.out.println("         java.net.InetAddressloaded - cache field loaded");

            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ConcurrentMap<String, Object> result = (ConcurrentMap<String, Object>)field.get(null);
            System.out.println("         java.net.InetAddressloaded - cache field has " + result.size() + " elements.");
             
            // -------------------------------------------------------
            // Cache new host
            // -------------------------------------------------------

            result.putIfAbsent(host, naObject);
            System.out.println("         InetAddress " + host + " Added " + ip.toString()); 

        } catch (Exception e) { // NOSONAR

            StringBuffer sb = new StringBuffer(500);
            StackTraceElement[] st = e.getStackTrace();
            sb.append(e.getClass().getName() + ": " + e.getMessage() + "\n");
            for (int i = 0; i < st.length; i++) {
                sb.append("\t at " + st[i].toString() + "\n");
            }

            System.out.println("         InetAddress getDeclaredMethod Error: " + e.getMessage()); 

            System.out.println("         InetAddress getDeclaredMethod Trace: " + sb.toString()); 
              

        }

    } 
    
    public static String getIPString(byte[] ipInBytes) {

        StringBuilder ipSB = new StringBuilder();

        int temp = 0;

        for (int i = 0, j = ipInBytes.length; i < j; i++) {

            temp = (int)(ipInBytes[i] & 255);

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
