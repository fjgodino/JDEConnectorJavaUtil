/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdeconnectorjavautil;

import com.jdedwards.services.objectlookup.ObjectLookupService;
import com.jdedwards.system.connector.dynamic.ServerFailureException;
import com.jdedwards.system.connector.dynamic.UserSession;
import com.jdedwards.system.connector.dynamic.callmethod.ExecutableMethod;
import com.jdedwards.system.connector.dynamic.spec.SpecFailureException;
import com.jdedwards.system.connector.dynamic.spec.source.BSFNMethod;
import com.jdedwards.system.connector.dynamic.spec.source.BSFNParameter;
import com.jdedwards.system.connector.dynamic.spec.source.OneworldBSFNSpecSource;
import com.jdedwards.system.kernel.CallObjectErrorList;
import com.jdedwards.database.base.JDBDatabaseAccess;
import com.jdedwards.database.base.JDBException;
import com.jdedwards.database.base.JDBField;
import com.jdedwards.database.base.JDBFieldMap;
import com.jdedwards.database.base.JDBResultSet;
import com.jdedwards.database.jdb.JDBSystem;
import com.jdedwards.database.services.serviceobj.F9862;
import com.jdedwards.services.ServiceException;
import com.jdedwards.services.objectlookup.ObjectLookupService;
import com.jdedwards.services.objectlookup.ObjectLookupServiceLoader;
import com.jdedwards.system.connector.dynamic.Connector;
import com.jdedwards.system.connector.dynamic.spec.dbservices.BSFNLookupFailureException;
import com.jdedwards.system.security.UserOCMContextSession;
import com.jdedwards.system.xml.XMLRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author jgodino
 */
public class JDEConnectorJavaUtil {

    private String user;
    private String password;
    private String environment;
    private String role;

    private Integer sleeptime;
    private Integer calls;

    private int iSessionID = 0;

    List<String> filesListInDir = new ArrayList<String>();

    /**
     * This method populates all the files in a directory to a List
     *
     * @param dir
     * @throws IOException
     */
    private void populateFilesList(File dir) throws IOException {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                filesListInDir.add(file.getAbsolutePath());
            } else {
                populateFilesList(file);
            }
        }
    }

    private void zipDirectory(File dir, String zipDirName) {
        try {
            populateFilesList(dir);
            //now zip files one by one
            //create ZipOutputStream to write to the zip file
            FileOutputStream fos = new FileOutputStream(zipDirName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            for (String filePath : filesListInDir) {

                if (filePath.endsWith(".log")) {
                    System.out.println("Zipping " + filePath);
                    //for ZipEntry we need to keep only relative file path, so we used substring on absolute path
                    ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length() + 1, filePath.length()));
                    zos.putNextEntry(ze);
                    //read the file and write to ZipOutputStream
                    FileInputStream fis = new FileInputStream(filePath);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                    fis.close();
                }
            }
            zos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getMetadata(String user, String pwd, String environment, String role) throws ServerFailureException, InterruptedException, ServiceException, JDBException {

        System.out.println("=============== Parameters ================== ");
        System.out.println("User: " + user);
        System.out.println("Enviroment: " + environment);
        System.out.println("Role: " + role);
        System.out.println("=============== ********** ================== ");

        this.user = user;
        this.password = pwd;
        this.environment = environment;
        this.role = role;

        login();

        // ================
        // Init Object
        // ================
        ObjectLookupService objectLookupService;

        // ================
        // Get User Session
        // ================
        Integer localInteger = Integer.valueOf(this.iSessionID);

        UserSession localUserSession = Connector.getInstance()
                .getUserSession(localInteger);

        if (!localUserSession.isSbfConnectorMode()) {

            Properties localProperties = new Properties();

            UserOCMContextSession localUserOCMContextSession = new UserOCMContextSession(localUserSession);

            localProperties.put("session", localUserOCMContextSession);

            objectLookupService = ObjectLookupServiceLoader.findService(localProperties);

            JDBDatabaseAccess localJDBDatabaseAccess = JDBSystem.connect(localUserOCMContextSession, objectLookupService);

            localUserSession.setUserDBAccess(localJDBDatabaseAccess);

        }

        // ================
        // Get Functions
        // ================
        //
        System.out.println("MULESOFT - SpecsGenerator: BussinessFunctionList() Reading Functions from F9862");

        JDBResultSet localJDBResultSet = null;

        JDBDatabaseAccess localJDBDatabaseAccess = localUserSession.getUserDBAccess();

        localJDBResultSet = localJDBDatabaseAccess.select(F9862.getInstance(), new JDBField[]{
            F9862.FCTNM_FIELD,
            F9862.MD_FIELD
        }, null, null);

        JDBFieldMap arrayOfJDBFieldMap = localJDBResultSet.fetchNext();

        int i = 0;

        while (arrayOfJDBFieldMap != null) {

            String functionName = arrayOfJDBFieldMap.getString(F9862.FCTNM_FIELD)
                    .trim();

            System.out.println(functionName);

            arrayOfJDBFieldMap = localJDBResultSet.fetchNext();

            i++;

        }

        localJDBResultSet.close();

        if (localJDBResultSet != null) {

            localJDBResultSet.close();

            System.out.println("MULESOFT - SpecsGenerator: BussinessFunctionList() Connections closed");

        }

        System.out.println("MULESOFT - SpecsGenerator: BussinessFunctionList() Returning " + Integer.toString(i) + " functions");

        if (iSessionID != 0) {

            System.out.println("Logoff from JDE ...");

            com.jdedwards.system.connector.dynamic.Connector.getInstance()
                    .logoff(iSessionID);

            System.out.println("ShutDown Connections ...");

            com.jdedwards.system.connector.dynamic.Connector.getInstance()
                    .shutDown();

        }

        System.out.println("-----------------------------------------------");
        System.out.println("Done");
        System.out.println("-----------------------------------------------");

    }

    public void testUBE(String user, String pwd, String environment, String role, String ubeXMLFile) throws ParserConfigurationException, SAXException, IOException, SpecFailureException {

        System.out.println("=============== SUBMIT UBE ================== ");

        DocumentBuilderFactory dbFactory;
        DocumentBuilder dBuilder;

        dbFactory = DocumentBuilderFactory.newInstance();

        dBuilder = dbFactory.newDocumentBuilder();

        Document docRequest = dBuilder.parse(new File(ubeXMLFile));

        docRequest.getDocumentElement()
                .normalize();

        String xmlToSubmit = convertDocRequestToString(docRequest);
        
        xmlToSubmit = xmlToSubmit.replace("%USER%", user);
        xmlToSubmit = xmlToSubmit.replace("%PWD%", pwd);
        xmlToSubmit = xmlToSubmit.replace("%ENV%", environment);
        xmlToSubmit = xmlToSubmit.replace("%ROL%", role);
 

        String serverName = "";

        int serverPort = 0;

        XMLRequest xmlRequest = new XMLRequest(serverName, serverPort, xmlToSubmit);

        String szDocRequestResult = "";

        if (xmlRequest != null) {

            System.out.print("Request: ");
            System.out.println(xmlToSubmit);

            szDocRequestResult = xmlRequest.execute();

            System.out.print("Response: ");
            System.out.println(szDocRequestResult);

        }

        // ===================================================
        // Convert Response to Document
        // ===================================================
        InputSource is;

        Document docRequestResult = null;

        if (!szDocRequestResult.isEmpty()) {

            dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();

            is = new InputSource(new StringReader(szDocRequestResult));

            docRequestResult = dBuilder.parse(is);

            if (docRequestResult != null) {

                docRequestResult.getDocumentElement()
                        .normalize();

            } else {

                throw new SpecFailureException("Error Generating XML Document from Response", null);

            }

        }

        String errorReturnValue = "";

        Node nodejdeJDE = docRequestResult.getElementsByTagName("ERROR")
                .item(0);

        NamedNodeMap attr = null;
        Node nodeAttr = null;

        if (nodejdeJDE != null) {

            attr = nodejdeJDE.getAttributes();

            if (attr != null && attr.getNamedItem("VALUE") != null) {

                nodeAttr = attr.getNamedItem("VALUE");

                errorReturnValue = nodeAttr.getTextContent();

            }

        } else {
            System.out.println("There is not ERROR");
        }

        // ===================================================
        // Set Job ID
        // ===================================================
        boolean bXMLFormatValid = false;

        long jobId = 0;

        nodejdeJDE = docRequestResult.getElementsByTagName("JOBID")
                .item(0);

        if (nodejdeJDE != null) {

            attr = nodejdeJDE.getAttributes();

            if (attr != null && attr.getNamedItem("VALUE") != null) {

                nodeAttr = attr.getNamedItem("VALUE");

                jobId = Long.parseLong(nodeAttr.getTextContent());

                bXMLFormatValid = true;

            }

        }

        if (!bXMLFormatValid) {
            System.out.println("Error Submit UBE Version - Invalid Job Number");
            throw new SpecFailureException("Error Submit UBE Version - Invalid Job Number", null);
        }

        System.out.println("Job ID: " + jobId);

        System.out.println("------------------------------------------------------- ");

        // ===================================================
        // INVOKE XML CREATE LIST Request
        // ===================================================
        System.out.println("=============== CREATE LIST ================== ");

        String request = "<?xml version=\"1.0\"?>\n"
                + "<jdeRequest type='list' user='%USER%' pwd='%PWD%' environment='%ENV%' role='%ROL%'>\n"
                + "  <!-- sessionid and sessionidle above are optional and an existing sessionid could be passed in -->\n"
                + "  <ACTION TYPE='CreateList'>\n"
                + "    <TABLE_NAME VALUE='F986110'/>\n"
                + "    <TABLE_TYPE VALUE='OWTABLE'/> \n"
                + "    <LIST_TYPE VALUE='JDB'/>\n"
                + "        <RUNTIME_OPTIONS>\n"
                + "			<COLUMN_SELECTION>\n"
                + "				<COLUMN ALIAS='JOBNBR'/>\n"
                + "    	        <COLUMN ALIAS='EXEHOST'/>  \n"
                + "				<COLUMN ALIAS='JOBSTS'/>\n"
                + "			</COLUMN_SELECTION>\n"
                + "			<DATA_SELECTION>\n"
                + "                <CLAUSE TYPE='WHERE'>\n"
                + "					<COLUMN INSTANCE='0' TABLE='F986110' ALIAS='EXEHOST' NAME='JCEXEHOST' />\n"
                + "					<OPERATOR TYPE='EQ'/>\n"
                + "					<OPERAND>\n"
                + "						<LITERAL VALUE='%1%'/>\n"
                + "					</OPERAND>				\n"
                + "				</CLAUSE>\n"
                + "	            <CLAUSE TYPE='AND'>\n"
                + "					<COLUMN INSTANCE='0' TABLE='F986110' ALIAS='JOBNBR' NAME ='JCJOBNBR'/>\n"
                + "					<OPERATOR TYPE='EQ'/>\n"
                + "					<OPERAND>\n"
                + "						<LITERAL VALUE='%2%'/>\n"
                + "					</OPERAND>\n"
                + "                </CLAUSE>\n"
                + "			</DATA_SELECTION>\n"
                + "		</RUNTIME_OPTIONS>\n"
                + "    </ACTION> \n"
                + "</jdeRequest>";

        dbFactory = DocumentBuilderFactory.newInstance();

        dBuilder = dbFactory.newDocumentBuilder();
        docRequest = dBuilder.parse(new InputSource(new StringReader(request)));
        docRequest.getDocumentElement()
                .normalize();

        xmlToSubmit = convertDocRequestToString(docRequest);

        xmlToSubmit = xmlToSubmit.replace("%1%", serverName);
        xmlToSubmit = xmlToSubmit.replace("%2%", Long.toString(jobId));
        xmlToSubmit = xmlToSubmit.replace("%USER%", user);
        xmlToSubmit = xmlToSubmit.replace("%PWD%", pwd);
        xmlToSubmit = xmlToSubmit.replace("%ENV%", environment);
        xmlToSubmit = xmlToSubmit.replace("%ROL%", role);

        xmlRequest = new XMLRequest(serverName, serverPort, xmlToSubmit);

        szDocRequestResult = "";

        if (xmlRequest != null) {

            System.out.print("Request: ");
            System.out.println(xmlToSubmit);

            szDocRequestResult = xmlRequest.execute();

            System.out.print("Response: ");
            System.out.println(szDocRequestResult);

        }

        docRequestResult = null;

        if (!szDocRequestResult.isEmpty()) {

            dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();

            is = new InputSource(new StringReader(szDocRequestResult));

            docRequestResult = dBuilder.parse(is);

            if (docRequestResult != null) {

                docRequestResult.getDocumentElement()
                        .normalize();

            } else {

                throw new SpecFailureException("Error Generating XML Document from Response", null);

            }

        }

        errorReturnValue = "";

        nodejdeJDE = docRequestResult.getElementsByTagName("ERROR")
                .item(0);

        attr = null;
        nodeAttr = null;

        if (nodejdeJDE != null) {

            attr = nodejdeJDE.getAttributes();

            if (attr != null && attr.getNamedItem("VALUE") != null) {

                nodeAttr = attr.getNamedItem("VALUE");

                errorReturnValue = nodeAttr.getTextContent();

            }

        } else {
            System.out.println("There is not ERROR");
        }

        bXMLFormatValid = false;

        String sessionID = "";

        nodejdeJDE = docRequestResult.getElementsByTagName("jdeResponse")
                .item(0);

        if (nodejdeJDE != null) {

            attr = nodejdeJDE.getAttributes();

            if (attr != null && attr.getNamedItem("session") != null) {

                nodeAttr = attr.getNamedItem("session");

                sessionID = nodeAttr.getTextContent();

                bXMLFormatValid = true;

            }

        }

        if (!bXMLFormatValid) {
            throw new SpecFailureException("Error Submit UBE Version - Invalid Job Number", null);
        }

        System.out.println("Session ID: " + sessionID);

        bXMLFormatValid = false;

        String handleFromXMLList = "";

        nodejdeJDE = docRequestResult.getElementsByTagName("HANDLE")
                .item(0);

        if (nodejdeJDE != null) {

            handleFromXMLList = nodejdeJDE.getTextContent();

            bXMLFormatValid = true;

        }

        if (!bXMLFormatValid) {
            throw new SpecFailureException("Error Submit UBE Version - Invalid Job Number", null);
        }

        System.out.println("Handle ID: " + handleFromXMLList);

        System.out.println("------------------------------------------------------- ");

        // ===================================================
        // INVOKE GET GROUP Request
        // ===================================================
        System.out.println("=============== CREATE GROUP ================== ");

        request = "<?xml version=\"1.0\"?>\n"
                + "<jdeRequest type='list' user='%USER%' pwd='%PWD%' environment='%ENV%' role='%ROL%'>\n"
                + " <ACTION TYPE=\"GetGroup\">\n"
                + "  <HANDLE VALUE=\"%1%\"/>\n"
                + " </ACTION>\n"
                + "</jdeRequest>";

        dbFactory = DocumentBuilderFactory.newInstance();

        dBuilder = dbFactory.newDocumentBuilder();
        docRequest = dBuilder.parse(new InputSource(new StringReader(request)));
        docRequest.getDocumentElement()
                .normalize();

        xmlToSubmit = convertDocRequestToString(docRequest);

        xmlToSubmit = xmlToSubmit.replace("%1%", handleFromXMLList);
        xmlToSubmit = xmlToSubmit.replace("%USER%", user);
        xmlToSubmit = xmlToSubmit.replace("%PWD%", pwd);
        xmlToSubmit = xmlToSubmit.replace("%ENV%", environment);
        xmlToSubmit = xmlToSubmit.replace("%ROL%", role);

        xmlRequest = new XMLRequest(serverName, serverPort, xmlToSubmit);

        szDocRequestResult = "";

        if (xmlRequest != null) {

            System.out.print("Request: ");
            System.out.println(xmlToSubmit);

            szDocRequestResult = xmlRequest.execute();

            System.out.print("Response: ");
            System.out.println(szDocRequestResult);

        }

        docRequestResult = null;

        if (!szDocRequestResult.isEmpty()) {

            dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();

            is = new InputSource(new StringReader(szDocRequestResult));

            docRequestResult = dBuilder.parse(is);

            if (docRequestResult != null) {

                docRequestResult.getDocumentElement()
                        .normalize();

            } else {

                throw new SpecFailureException("Error Generating XML Document from Response", null);

            }

        }

        errorReturnValue = "";

        nodejdeJDE = docRequestResult.getElementsByTagName("ERROR")
                .item(0);

        attr = null;
        nodeAttr = null;

        if (nodejdeJDE != null) {

            attr = nodejdeJDE.getAttributes();

            if (attr != null && attr.getNamedItem("VALUE") != null) {

                nodeAttr = attr.getNamedItem("VALUE");

                errorReturnValue = nodeAttr.getTextContent();

            }

        } else {
            System.out.println("There is not ERROR");
        }

        bXMLFormatValid = false;

        int fetchedRows = 0;

        nodejdeJDE = docRequestResult.getElementsByTagName("HANDLE")
                .item(0);

        if (nodejdeJDE != null) {

            attr = nodejdeJDE.getAttributes();

            if (attr != null && attr.getNamedItem("FETCHEDROWS") != null) {

                nodeAttr = attr.getNamedItem("FETCHEDROWS");

                fetchedRows = Integer.parseInt(nodeAttr.getTextContent());

                bXMLFormatValid = true;

            }

        }

        if (fetchedRows == 0) {
            throw new SpecFailureException("Error Submit UBE Version - Invalid Job Number", null);
        }

        System.out.println("Fetched Rows: " + fetchedRows);

        System.out.println("------------------------------------------------------- ");

        // ===================================================
        // INVOKE DELETE LIST Request
        // ===================================================
        System.out.println("=============== DELETE LIST ================== ");

        request = "<?xml version=\"1.0\"?>\n"
                + "<jdeRequest type='list' user='%USER%' pwd='%PWD%' environment='%ENV%' role='%ROL%'>\n"
                + " <ACTION TYPE=\"DeleteList\">\n"
                + "   <HANDLE VALUE=\"%1%\"/>\n"
                + " </ACTION>\n"
                + "</jdeRequest>";

        dbFactory = DocumentBuilderFactory.newInstance();

        dBuilder = dbFactory.newDocumentBuilder();
        docRequest = dBuilder.parse(new InputSource(new StringReader(request)));
        docRequest.getDocumentElement()
                .normalize();

        xmlToSubmit = convertDocRequestToString(docRequest);

        xmlToSubmit = xmlToSubmit.replace("%1%", handleFromXMLList);
        xmlToSubmit = xmlToSubmit.replace("%USER%", user);
        xmlToSubmit = xmlToSubmit.replace("%PWD%", pwd);
        xmlToSubmit = xmlToSubmit.replace("%ENV%", environment);
        xmlToSubmit = xmlToSubmit.replace("%ROL%", role);

        xmlRequest = new XMLRequest(serverName, serverPort, xmlToSubmit);

        szDocRequestResult = "";

        if (xmlRequest != null) {

            System.out.print("Request: ");
            System.out.println(xmlToSubmit);

            szDocRequestResult = xmlRequest.execute();

            System.out.print("Response: ");
            System.out.println(szDocRequestResult);

        }

        docRequestResult = null;

        if (!szDocRequestResult.isEmpty()) {

            dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();

            is = new InputSource(new StringReader(szDocRequestResult));

            docRequestResult = dBuilder.parse(is);

            if (docRequestResult != null) {

                docRequestResult.getDocumentElement()
                        .normalize();

            } else {

                throw new SpecFailureException("Error Generating XML Document from Response", null);

            }

        }

        errorReturnValue = "";

        nodejdeJDE = docRequestResult.getElementsByTagName("ERROR")
                .item(0);

        attr = null;
        nodeAttr = null;

        if (nodejdeJDE != null) {

            attr = nodejdeJDE.getAttributes();

            if (attr != null && attr.getNamedItem("VALUE") != null) {

                nodeAttr = attr.getNamedItem("VALUE");

                errorReturnValue = nodeAttr.getTextContent();

            }

        } else {
            System.out.println("There is not ERROR");
        }
        
        System.out.println("=============== Zipping Logs ================== ");

        File dir = new File("/tmp/jdelog");
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        String zipDirName = "log_" + ubeXMLFile + '_' + dateFormat.format(date) + ".zip";
         
        File zipFile = new File("/tmp/jdelog.zip");
        zipFile.delete();
        zipDirectory(dir, zipDirName);

        System.out.println("Zip " + zipDirName + " has been created");

        System.out.println("=============== ************ ================== ");

        System.out.println("-----------------------------------------------");
        System.out.println("LOGS: ");
        System.out.println("          JDE Logs: " + zipDirName);

    }

    private String convertDocRequestToString(Document docRequest) throws SpecFailureException {

        // ===================================================
        // Initialize Output Var
        // ===================================================
        String szDocRequest = "";

        // ===================================================
        // Initialize Working Variable
        // ===================================================
        TransformerFactory transformerFactory;
        Transformer transformer;
        DOMSource source;
        StringWriter outWriter;
        StreamResult result;

        // ===================================================
        // Convert Doc Request
        // ===================================================
        try {

            transformerFactory = TransformerFactory.newInstance();
            transformer = transformerFactory.newTransformer();
            source = new DOMSource(docRequest);
            outWriter = new StringWriter();
            result = new StreamResult(outWriter);
            transformer.transform(source, result);
            szDocRequest = outWriter.getBuffer()
                    .toString();

        } catch (Exception e) {

            throw new SpecFailureException(e.getMessage(), e);

        }

        return szDocRequest;
    }

    public String testCallBSFN(String user, String pwd, String environment, String role, String sleeptime, String calls) throws ServerFailureException, InterruptedException, IOException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        DNSCacheLoader.getInstance().loadServers();

        DNSCacheLoader.getInstance().loadJDEServersInInetAddressCache();

        System.out.println("=============== Parameters ================== ");
        System.out.println("User: " + user);
        System.out.println("Enviroment: " + environment);
        System.out.println("Role: " + role);
        System.out.println("How many BSFN calls to execute: " + calls);
        System.out.println("Sleep Time between Call BSFN: " + sleeptime + " seconds");
        System.out.println("=============== ********** ================== ");
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        this.user = user;
        this.password = pwd;
        this.environment = environment;
        this.role = role;
        this.sleeptime = Integer.parseInt(sleeptime);
        this.calls = Integer.parseInt(calls);

        for (int i = 0; i < this.calls; i++) {

            boolean userConnected = false;

            login();

            CallBSFN();
            //CallBSFNGetNextNumber();
//            CallBSFNInventory();

            System.out.println("Sleep for " + this.sleeptime + " minutes");

            if (i < this.calls - 1) {

                int currentTime = this.sleeptime;
                while (currentTime > 0) {
                    System.out.print(" . ");
                    Thread.sleep(60000);
                    currentTime = currentTime - 1;
                }

                System.out.println(" . ");

            }

        }

        if (iSessionID != 0) {

            System.out.println("Logoff from JDE ...");

            com.jdedwards.system.connector.dynamic.Connector.getInstance()
                    .logoff(iSessionID);

            System.out.println("ShutDown Connections ...");

            com.jdedwards.system.connector.dynamic.Connector.getInstance()
                    .shutDown();

        }

        System.out.println("=============== Zipping Logs ================== ");

        File dir = new File("/tmp/jdelog");
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        String zipDirName = "log_" + dateFormat.format(date) + ".zip";
        zipDirName = "jdelog.zip";
        File zipFile = new File("/tmp/jdelog.zip");
        zipFile.delete();
        zipDirectory(dir, zipDirName);

        System.out.println("Zip " + zipDirName + " has been created");

        System.out.println("=============== ************ ================== ");

        System.out.println("-----------------------------------------------");
        System.out.println("Done");
        System.out.println("-----------------------------------------------");

        return zipDirName;

    }
    
    public String testDNSLogin(String user, String pwd, String environment, String role, String sleeptime, String calls) throws ServerFailureException, InterruptedException, IOException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
 

        System.out.println("=============== Parameters ================== ");
        System.out.println("User: " + user);
        System.out.println("Enviroment: " + environment);
        System.out.println("Role: " + role); 
        System.out.println("=============== ********** ================== ");
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        this.user = user;
        this.password = pwd;
        this.environment = environment;
        this.role = role;
        
        login();
         

        if (iSessionID != 0) {

            System.out.println("Logoff from JDE ...");

            com.jdedwards.system.connector.dynamic.Connector.getInstance()
                    .logoff(iSessionID);

            System.out.println("ShutDown Connections ...");

            com.jdedwards.system.connector.dynamic.Connector.getInstance()
                    .shutDown();

        }

        System.out.println("=============== Zipping Logs ================== ");

        File dir = new File("/tmp/jdelog");
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        String zipDirName = "log_" + dateFormat.format(date) + ".zip";
        zipDirName = "jdelog.zip";
        File zipFile = new File("/tmp/jdelog.zip");
        zipFile.delete();
        zipDirectory(dir, zipDirName);

        System.out.println("Zip " + zipDirName + " has been created");

        System.out.println("=============== ************ ================== ");

        System.out.println("-----------------------------------------------");
        System.out.println("Done");
        System.out.println("-----------------------------------------------");

        return zipDirName;

    }
    

    private void login() throws ServerFailureException, InterruptedException {
        System.out.println("Connecting to JDE ..");

        boolean userConnected = false;

        if (iSessionID != 0) {

            System.out.println("MULESOFT - JDEConnectorService:  Previously connected to JDE. Id:" + Integer.toString(iSessionID));

            userConnected = com.jdedwards.system.connector.dynamic.Connector.getInstance()
                    .isLoggedIn(iSessionID);

            System.out.println("MULESOFT - JDEConnectorService:  is connected to JDE? = " + userConnected);

            if (!userConnected) {

                iSessionID = 0;

                System.out.println("The connections has been reseted");

            } else {
                System.out.println("Current user is connected");
            }

        }

        if (iSessionID == 0) {

            iSessionID = com.jdedwards.system.connector.dynamic.Connector.getInstance()
                    .login(this.user, this.password, this.environment, this.role);

            System.out.println("      User Connected with Id: " + Integer.toString(iSessionID));

            // ==============================================================
            // Validate is there is another session opened with the same user
            // ==============================================================
            Map<?, ?> connectionsOpen = com.jdedwards.system.connector.dynamic.Connector.getInstance()
                    .getUserSessions();

            Iterator<?> userSessions = connectionsOpen.values()
                    .iterator();

            System.out.println("Current user logged: ");

            while (userSessions.hasNext()) {

                UserSession sessionOpen = (UserSession) userSessions.next();

                System.out.println("Current user logged: ");
                System.out.println("      User: " + sessionOpen.getUserName());
                System.out.println("      Role: " + sessionOpen.getUserRole());
                System.out.println("      Environment: " + sessionOpen.getUserEnvironment());

                if (sessionOpen.getUserName()
                        .compareTo(user) == 0 && sessionOpen.getUserRole()
                        .compareTo(role) == 0
                        && sessionOpen.getUserEnvironment()
                                .compareTo(environment) == 0) {

                    iSessionID = (int) sessionOpen.getSessionID();

                    System.out.println("      User Connected with Id: " + Integer.toString(iSessionID));

                    userConnected = true;
                }

            }

            // ==============================================================
            // Validate is there is another session opened with the same user
            // ==============================================================
            if (userConnected) {

                userConnected = com.jdedwards.system.connector.dynamic.Connector.getInstance()
                        .isLoggedIn(iSessionID);

            }

            // ==============================================================
            // Login
            // ==============================================================
            if (!userConnected) {

                System.out.println("Connecting to JDE ..");

                iSessionID = com.jdedwards.system.connector.dynamic.Connector.getInstance()
                        .login(user, password, environment, role);

                System.out.println("      User Connected with Id: " + Integer.toString(iSessionID));
            }

        }

        Thread.sleep(5000);

    }

    private void CallBSFN() throws ServerFailureException, InterruptedException {

        System.out.println("Getting specs ...");

        OneworldBSFNSpecSource specSource = null;
        BSFNMethod method = null;
        ExecutableMethod callobject = null;
        CallObjectErrorList bsfnListError = null;

        try {
            specSource = new OneworldBSFNSpecSource(iSessionID);
        } catch (SpecFailureException e) {
            System.out.println("Unexpected exception:" + e.getMessage());
        }

        if (specSource != null) {

            try {

                System.out.println("Getting specs for GetCurrentUTime ...");

                method = specSource.getBSFNMethod("GetCurrentUTime");

                System.out.println("Creating Executable ...");

                callobject = method.createExecutable();

                System.out.println("Executing BSFN ...");

                bsfnListError = callobject.executeBSFN(iSessionID);

                if (bsfnListError.getBSFNErrorCode() > 0) {

                    System.out.println("ERROR Calling BSFN OMWGetNextNumber ");
                }

                for (BSFNParameter parameter : method.getParameters()) {

                    System.out.println("callBSFN() Returning Parameter: " + parameter.getName() + " value: "
                            + callobject.getValue(parameter.getName()));
                }

                System.out.println("callBSFN() Ready.");

            } catch (Exception ex) {
                System.out.println("nUnexpected exception:" + ex.getMessage());
            }

        }

    }
    
    
    private void CallBSFNInventory() throws ServerFailureException, InterruptedException {

        System.out.println("Getting specs ...");

        OneworldBSFNSpecSource specSource = null;
        BSFNMethod method = null;
        ExecutableMethod callobject = null;
        CallObjectErrorList bsfnListError = null;

        try {
            specSource = new OneworldBSFNSpecSource(iSessionID);
        } catch (SpecFailureException e) {
            System.out.println("Unexpected exception:" + e.getMessage());
        }

        if (specSource != null) {

            try {

                System.out.println("Getting specs for GetNextUniqueKeyID ...");

                 method = specSource.getBSFNMethod("InventoryAvailableAllItems"); 
                 
                System.out.println("Creating Executable ...");

                callobject = method.createExecutable();

                callobject.setValue("szObjectName", "MULETEST");
                callobject.setValue("mnUniqueKeyID", "0");

                System.out.println("Executing BSFN ...");

                bsfnListError = callobject.executeBSFN(iSessionID);

                if (bsfnListError.getBSFNErrorCode() > 0) {

                    System.out.println("ERROR Calling BSFN GetNextUniqueKeyID ");
                }

                for (BSFNParameter parameter : method.getParameters()) {

                    System.out.println("callBSFN() Returning Parameter: " + parameter.getName() + " value: "
                            + callobject.getValue(parameter.getName()));
                }

                System.out.println("callBSFN() Ready.");

            } catch (Exception ex) {
                System.out.println("nUnexpected exception:" + ex.getMessage());
                ex.printStackTrace();
            }

        }

    }

    private void CallBSFNGetNextNumber() throws ServerFailureException, InterruptedException {

        System.out.println("Getting specs ...");

        OneworldBSFNSpecSource specSource = null;
        BSFNMethod method = null;
        ExecutableMethod callobject = null;
        CallObjectErrorList bsfnListError = null;

        try {
            specSource = new OneworldBSFNSpecSource(iSessionID);
        } catch (SpecFailureException e) {
            System.out.println("Unexpected exception:" + e.getMessage());
        }

        if (specSource != null) {

            try {

                System.out.println("Getting specs for GetNextUniqueKeyID ...");

                method = specSource.getBSFNMethod("GetNextUniqueKeyID");

                System.out.println("Creating Executable ...");

                callobject = method.createExecutable();

                callobject.setValue("szObjectName", "MULETEST");
                callobject.setValue("mnUniqueKeyID", "0");

                System.out.println("Executing BSFN ...");

                bsfnListError = callobject.executeBSFN(iSessionID);

                if (bsfnListError.getBSFNErrorCode() > 0) {

                    System.out.println("ERROR Calling BSFN GetNextUniqueKeyID ");
                }

                for (BSFNParameter parameter : method.getParameters()) {

                    System.out.println("callBSFN() Returning Parameter: " + parameter.getName() + " value: "
                            + callobject.getValue(parameter.getName()));
                }

                System.out.println("callBSFN() Ready.");

            } catch (Exception ex) {
                System.out.println("nUnexpected exception:" + ex.getMessage());
            }

        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ParserConfigurationException, SAXException {

        boolean testUBE = false;
        boolean testCALL = false;
        boolean testMETA = false;
        boolean testDNS = true;

        // create the command line parser
        CommandLineParser parser = new DefaultParser();

        // create the Options
        Options options = new Options();

        options.addOption("u", "jde_user", true, "Enter JDE User");
        options.addOption("p", "jde_pwd", true, "Enter JDE Password");
        options.addOption("r", "jde_role", true, "Enter JDE Role");
        options.addOption("e", "jde_environment", true, "Enter JDE Environment");
        options.addOption("c", "calls", true, "How many BSFN calls to execute");
        options.addOption("s", "sleep_time", true, "Sleep Time in seconds");
        options.addOption("b", "ube_file_name", true, "UBE XML request file name");

        System.out.println("JDEConnectorJavaUtil Test Call BSFN Version: 1.0.0");

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            if (line != null) {

                if (testMETA) // validate that block-size has been set
                {
                    if (    line.hasOption("jde_user") && 
                            line.hasOption("jde_pwd") && 
                            line.hasOption("jde_role") && 
                            line.hasOption("jde_environment")) {

                        System.out.println(" jde_user: " + line.getOptionValue("jde_user"));
                        System.out.println(" jde_pwd: " + line.getOptionValue("jde_pwd"));
                        System.out.println(" jde_role: " + line.getOptionValue("jde_role"));
                        System.out.println(" jde_environment: " + line.getOptionValue("jde_environment"));

                        JDEConnectorJavaUtil util = new JDEConnectorJavaUtil();

                        try {
                            if (testMETA) {
                                util.getMetadata(line.getOptionValue("jde_user"), line.getOptionValue("jde_pwd"), line.getOptionValue("jde_environment"), line.getOptionValue("jde_role"));
                            }
                        } catch (ServerFailureException ex) {
                            System.out.println("Unexpected exception:" + ex.getMessage());
                        } catch (InterruptedException ex) {
                            System.out.println("Unexpected exception:" + ex.getMessage());
                        } catch (ServiceException ex) {
                            System.out.println("Unexpected exception:" + ex.getMessage());
                        } catch (JDBException ex) {
                            System.out.println("Unexpected exception:" + ex.getMessage());
                        }

                    } else {
                        HelpFormatter formatter = new HelpFormatter();
                        formatter.printHelp("java -jar JDEConnectorJavaUtil.jar", options);
                    }
                }

                if (testUBE) {
                    
                    if (    line.hasOption("jde_user") && 
                            line.hasOption("jde_pwd") && 
                            line.hasOption("jde_role") && 
                            line.hasOption("jde_environment") && 
                            line.hasOption("ube_file_name")) {

                        System.out.println(" jde_user: " + line.getOptionValue("jde_user"));
                        System.out.println(" jde_pwd: " + line.getOptionValue("jde_pwd"));
                        System.out.println(" jde_role: " + line.getOptionValue("jde_role"));
                        System.out.println(" jde_environment: " + line.getOptionValue("jde_environment"));
                        System.out.println(" ube_file_name: " + line.getOptionValue("ube_file_name"));

                        JDEConnectorJavaUtil util = new JDEConnectorJavaUtil();

                        try {

                            util.testUBE(line.getOptionValue("jde_user"), 
                                         line.getOptionValue("jde_pwd"), 
                                         line.getOptionValue("jde_environment"), 
                                         line.getOptionValue("jde_role"), 
                                         line.getOptionValue("ube_file_name"));
                                
                            
                        } catch (SpecFailureException ex) {
                            Logger.getLogger(JDEConnectorJavaUtil.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(JDEConnectorJavaUtil.class.getName()).log(Level.SEVERE, null, ex);
                        }
 

                    } else {
                        HelpFormatter formatter = new HelpFormatter();
                        formatter.printHelp("java -jar JDEConnectorJavaUtil.jar", options);
                    }
                }

                if (testCALL) {

                    if (line.hasOption("jde_user") && line.hasOption("jde_pwd") && line.hasOption("jde_role") && line.hasOption("jde_environment") && line.hasOption("sleep_time") && line.hasOption("calls")) {

                        System.out.println(" jde_user: " + line.getOptionValue("jde_user"));
                        System.out.println(" jde_pwd: " + line.getOptionValue("jde_pwd"));
                        System.out.println(" jde_role: " + line.getOptionValue("jde_role"));
                        System.out.println(" jde_environment: " + line.getOptionValue("jde_environment"));
                        System.out.println(" sleep_time: " + line.getOptionValue("sleep_time"));
                        System.out.println(" calls: " + line.getOptionValue("calls"));

                        JDEConnectorJavaUtil util = new JDEConnectorJavaUtil();

                        try {

                            util.testCallBSFN(line.getOptionValue("jde_user"), line.getOptionValue("jde_pwd"), line.getOptionValue("jde_environment"), line.getOptionValue("jde_role"), line.getOptionValue("sleep_time"), line.getOptionValue("calls"));
                            
                            

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
                
                if (testDNS) { 
                    
                    String[] versionElements = System.getProperty("java.version").split("\\.");
         
                    System.out.println(Arrays.toString(versionElements));
                    
                    
                    System.out.println("-------------------------------------------------------");
                    System.out.println("Get By Name 01 - JDE-ALPHA-ENT");
                    System.out.println("-------------------------------------------------------");
                    
                    try {
            
                        InetAddress ipJDEServer = InetAddress.getByName("JDE-ALPHA-ENT");
                        byte[] ip = ipJDEServer.getAddress();

                        System.out.println(getIPString(ip));


                    } catch (UnknownHostException ex) {
                         System.out.println("Unexpected exception: " + ex.getMessage());
                    }
                    
                    System.out.println("-------------------------------------------------------");
                    System.out.println("Get By Name 02 - JDE-ALPHA-ENT");
                    System.out.println("-------------------------------------------------------");
                    
                    try {
            
                        InetAddress ipJDEServer = InetAddress.getByName("JDE-ALPHA-ENT");
                        byte[] ip = ipJDEServer.getAddress();

                        System.out.println(getIPString(ip));


                    } catch (UnknownHostException ex) {
                         System.out.println("Unexpected exception: " + ex.getMessage());
                    }
                    
                    System.out.println("-------------------------------------------------------");
                    System.out.println("Register SQL Server JDE-ALPHA-SQL 65.52.119.187");
                    System.out.println("-------------------------------------------------------");
                     
                    try {
                        
                        InetAddress ipJDEServerTmp = InetAddress.getByName("65.52.119.187");
                        
                        registerHostOpenJDK11("JDE-ALPHA-SQL", ipJDEServerTmp);
                    
                    } catch (UnknownHostException ex) {
                        System.out.println("Unexpected exception: " + ex.getMessage());
                    } catch (IOException ex) {
                        Logger.getLogger(JDEConnectorJavaUtil.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    System.out.println("-------------------------------------------------------");
                    System.out.println("Get By Name 03 - JDE-ALPHA-SQL");
                    System.out.println("-------------------------------------------------------");
                    
                    try {
            
                        InetAddress ipJDEServer = InetAddress.getByName("JDE-ALPHA-SQL");
                        byte[] ip = ipJDEServer.getAddress();

                        System.out.println(getIPString(ip));


                    } catch (UnknownHostException ex) {
                         System.out.println("Unexpected exception: " + ex.getMessage());
                    }
                    
                    
                    System.out.println("-------------------------------------------------------");
                    System.out.println("Get By IP 01");
                    System.out.println("-------------------------------------------------------");
        
                    try {

                        InetAddress ipJDEServer = InetAddress.getByName("138.91.73.161");
                        byte[] ip = ipJDEServer.getAddress();

                        System.out.println(getIPString(ip));


                    } catch (UnknownHostException ex) {
                        System.out.println("Unexpected exception: " + ex.getMessage());
                    }
                    
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

        } catch (ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
        }

    }
    
    private static void registerHostOpenJDK11(String host, InetAddress ip) throws IOException {

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
    
    private static String getIPString(byte[] ipInBytes) {

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
