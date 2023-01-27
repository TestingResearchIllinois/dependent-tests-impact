package edu.washington.cs.dt.impact.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class RuntimeComparator {
    private static void tryCreateDirectory(final File theDir) {
        try {
            Files.createDirectory(theDir.toPath());
        } catch (FileAlreadyExistsException ignored) {
            // The directory must have been created in between the check above and our attempt to create it.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        int inputTestListIndex = argsList.indexOf("-inputFile");
        int inputTestList = inputTestListIndex + 1;
        File theDir = new File("sootCsvOutput");
        tryCreateDirectory(theDir);
        String testOutput = argsList.get(inputTestList)+"target/surefire-reports/";

        String currpath=System.getProperty("user.dir");
        String projectPath=currpath.substring(0, currpath.length() - 13);
        String SootXml=projectPath+"lib-results/sootXML-firstVers/firstVers-runtime.xml";
        File xmlfile= new File(SootXml);
        DocumentBuilderFactory dbfxml= DocumentBuilderFactory.newInstance();
        DocumentBuilder dbxml = dbfxml.newDocumentBuilder();
        Document docxml = dbxml.parse(xmlfile);
        docxml.getDocumentElement().normalize();

        //System.out.println(SootXml);
        File folder = new File(testOutput);
        String[] paths = argsList.get(inputTestList).split("/");
        File[] listOfFiles = folder.listFiles();
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        fileWriter = new FileWriter("sootCsvOutput" + File.separator + paths[paths.length - 4] + "-comparedResult.csv");
        bufferedWriter = new BufferedWriter(fileWriter);
        CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.DEFAULT.withHeader("Test Name", "Method Under Test Name", "Surefire result","Test result","Surefire reported execution time(msec.)", "Tested execution time(msec)"));
        for(int i = 0; i < listOfFiles.length; i++){
            String filename = listOfFiles[i].getName();
            if(filename.endsWith(".xml")||filename.endsWith(".XML")) {

                File file = new File(testOutput+filename);
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(file);
                doc.getDocumentElement().normalize();

                //System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
                NodeList nList = doc.getElementsByTagName("testcase");


                for (int temp = 0; temp < nList.getLength(); temp++) {
                    Node nNode = nList.item(temp);
                    //System.out.println("\nCurrent Element :" + nNode.getNodeName());
                    //System.out.println("\n");
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
                        /*System.out.println("Test name: "
                                + eElement.getAttribute("classname"));
                        System.out.println("Method name: "
                                + eElement.getAttribute("name"));
                        System.out.println("Execution Time: "
                                + eElement.getAttribute("time"));*/
                        String surefireTestResult="";
                        if(!eElement.getAttribute("failure").isEmpty())
                        {
                            surefireTestResult="failure";
                        }
                        else if(!eElement.getAttribute("error").isEmpty())
                        {
                            surefireTestResult="failed";
                        }else {
                            surefireTestResult="pass";
                        }

                        NodeList nListxml= docxml.getElementsByTagName("method");

                        String time="";
                        String testResult="pass";
                        for (int tempxml = 0; tempxml < nListxml.getLength(); tempxml++) {
                            Node nNodexml = nListxml.item(tempxml);
                            try {
                                if (nNodexml.getNodeType() == nNodexml.ELEMENT_NODE) {
                                    Element eElementxml = (Element) nNodexml;

                                    if((Objects.equals(eElementxml.getAttribute("name"), eElement.getAttribute("classname")+"."+eElement.getAttribute("name"))) && (eElementxml.getAttribute("testType").equals("true")))
                                    {
                                        time=eElementxml.getAttribute("time");
                                        if((eElementxml.getAttribute("throwException").equals("true")))
                                        {
                                            testResult="failed";
                                        }
                                        break;
                                    }
                                }
                            }catch (NullPointerException e)
                            {
                                System.err.println(e.getMessage());
                            }



                        }
                        float surefireTime=Float.parseFloat(eElement.getAttribute("time"))*1000;
                        csvPrinter.printRecord(eElement.getAttribute("classname"),eElement.getAttribute("name"),surefireTestResult,testResult,(int)surefireTime,time);


                    }
                }
                csvPrinter.flush();
            }
        }


    }
}
