/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdeconnectorjavautil;

import com.jdedwards.system.connector.dynamic.ServerFailureException;
import com.jdedwards.system.connector.dynamic.callmethod.ExecutableMethod;
import com.jdedwards.system.connector.dynamic.spec.SpecFailureException;
import com.jdedwards.system.connector.dynamic.spec.source.BSFNMethod;
import com.jdedwards.system.connector.dynamic.spec.source.BSFNParameter;
import com.jdedwards.system.connector.dynamic.spec.source.OneworldBSFNSpecSource;
import com.jdedwards.system.kernel.CallObjectErrorList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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

                if (!filePath.endsWith(".lck")) {
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

    private void testCallBSFN(String user, String pwd, String environment, String role, String sleeptime) throws ServerFailureException, InterruptedException {

        System.out.println("=============== Parameters ================== ");
        System.out.println("User: " + user);
        System.out.println("Enviroment: " + environment);
        System.out.println("Role: " + role);
        System.out.println("Sleep Time between Call BSFN: " + sleeptime + " seconds");
        System.out.println("=============== ********** ================== ");

        this.user = user;
        this.password = pwd;
        this.environment = environment;
        this.role = role;
        this.sleeptime = Integer.parseInt(sleeptime);

        System.out.println("Connecting to JDE ..");

        iSessionID = com.jdedwards.system.connector.dynamic.Connector.getInstance()
                .login(this.user, this.password, this.environment, this.role);

        System.out.println("      User Connected with Id: " + Integer.toString(iSessionID));

        Thread.sleep(5000);

        System.out.println("\\nGetting specs ...");

        OneworldBSFNSpecSource specSource = null;
        BSFNMethod method = null;
        ExecutableMethod callobject = null;
        CallObjectErrorList bsfnListError = null;

        try {
            specSource = new OneworldBSFNSpecSource(iSessionID);
        } catch (SpecFailureException e) {
            System.out.println("\\nUnexpected exception:" + e.getMessage());
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

                    System.out.println("ERROR Calling BSFN GetCurrentUTime ");
                }

                for (BSFNParameter parameter : method.getParameters()) {

                    System.out.println("callBSFN() Returning Parameter: " + parameter.getName() + " value: "
                            + callobject.getValue(parameter.getName()));
                }

                System.out.println("callBSFN() Ready.");

                System.out.println("Sleep for " + this.sleeptime + " seconds");

                int currentTime = this.sleeptime;
                while (currentTime > 0) {
                    System.out.print(" . ");
                    Thread.sleep(10000);
                    currentTime = currentTime - 10;
                }

                System.out.println(" ");

                System.out.println("Executing BSFN ...");

                bsfnListError = callobject.executeBSFN(iSessionID);

                if (bsfnListError.getBSFNErrorCode() > 0) {

                    System.out.println("ERROR Calling BSFN GetCurrentUTime ");
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

        if (iSessionID != 0) {

            System.out.println("Logoff from JDE ...");

            com.jdedwards.system.connector.dynamic.Connector.getInstance()
                    .logoff(iSessionID);

            System.out.println("ShutDown Connections ...");

            com.jdedwards.system.connector.dynamic.Connector.getInstance()
                    .shutDown();

        }
        
        System.out.println("=============== Zipping Logs ================== ");

        File dir = new File("target");
        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss") ;
        String zipDirName = "log_"+ dateFormat.format(date) + ".zip";
        zipDirectory(dir, zipDirName);
        
        System.out.println("Zip " + zipDirName + " has been created");
        
         System.out.println("=============== ************ ================== ");


        System.out.println("-----------------------------------------------");
        System.out.println("Done");
        System.out.println("-----------------------------------------------");

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // create the command line parser
        CommandLineParser parser = new DefaultParser();
         
        // create the Options
        Options options = new Options();

        options.addOption("u", "jde_user", true, "Enter JDE User");
        options.addOption("p", "jde_pwd", true, "Enter JDE Password");
        options.addOption("r", "jde_role", true, "Enter JDE Role");
        options.addOption("e", "jde_environment", true, "Enter JDE Environment");
        options.addOption("s", "sleep_time", true, "Sleep Time in seconds");

        System.out.println("JDEConnectorJavaUtil Test Call BSFN Version: 1.0.0");

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            if (line != null) {

                // validate that block-size has been set
                if (line.hasOption("jde_user") && line.hasOption("jde_pwd") && line.hasOption("jde_role") && line.hasOption("jde_environment") && line.hasOption("sleep_time")) {

                    JDEConnectorJavaUtil util = new JDEConnectorJavaUtil();
                    try {
                        util.testCallBSFN(line.getOptionValue("jde_user"), line.getOptionValue("jde_pwd"), line.getOptionValue("jde_environment"), line.getOptionValue("jde_role"), line.getOptionValue("sleep_time"));
                    } catch (ServerFailureException ex) {
                        System.out.println("Unexpected exception:" + ex.getMessage());
                    } catch (InterruptedException ex) {
                        System.out.println("Unexpected exception:" + ex.getMessage());
                    }

                } else {
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp("java -jar JDEConnectorJavaUtil.jar", options);
                }

            }

        } catch (ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
        }

    }

}
