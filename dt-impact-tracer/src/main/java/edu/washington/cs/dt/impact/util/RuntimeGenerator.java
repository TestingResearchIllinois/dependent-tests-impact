package edu.washington.cs.dt.impact.util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * This class is responsible for creating runtime csv
 */
public class RuntimeGenerator {
    private static void tryCreateDirectory(final File theDir) {
        try {
            Files.createDirectory(theDir.toPath());
        } catch (FileAlreadyExistsException ignored) {
            // The directory must have been created in between the check above and our attempt to create it.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) throws IOException {
        List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        int inputTestListIndex = argsList.indexOf("-inputFile");
        int inputTestList = inputTestListIndex + 1;
        File theDir = new File("sootXMLOutput");
        String[] paths = argsList.get(inputTestList).split("/");
        System.out.println("File: " + paths[paths.length - 4]);
        tryCreateDirectory(theDir);
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
            DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder builder=dbf.newDocumentBuilder();
                Document doc=builder.newDocument();
                Element root=doc.createElement("testList");

                File rootFile = new File(argsList.get(inputTestList)+"sootSeqOutput/functionSequence");
                Scanner fileReader = new Scanner(rootFile);
                while (fileReader.hasNextLine()) {
                    Element test=doc.createElement("test");
                    String data = fileReader.nextLine();
                    Element methods=doc.createElement("methods");
                    String[] testInfo = data.split(" > ");
                    String initial = testInfo[0].trim();
                    if (!initial.equals("Started")){
                        continue;
                    }
                    String nameInfo = testInfo[1].trim();
                    String part = nameInfo.split(" >> ")[0].trim();
                    String nameWithTime = nameInfo.split(" >> ")[1].trim();
                    String name = nameWithTime.split("#")[0];
                    String timeStamp = nameWithTime.split("#")[1];

                    String execInfo = testInfo[4].trim();
                    String returnFrom = execInfo.split(" >> ").length == 1 ? "" : execInfo.split(">>")[0].trim();
                    String startTest = "startTest", endTest = "endTest";
                    if (part.contains("before")) {
                        startTest = "startBefore";
                        endTest = "endBefore";
                    } else if (part.contains("after")) {
                        startTest = "startAfter";
                        endTest = "endAfter";
                    }
                    createeElemnthead(doc,test,name,timeStamp);
                    if (part.contains("body")) {

                        String subFile = argsList.get(inputTestList) + "sootTimerOutput/";
                        try {
                            File subDir = new File(subFile+name);
                            Scanner subReader = new Scanner(subDir);
                            while(subReader.hasNextLine()){
                                String fullMethodName = subReader.nextLine();
                                String methodNameInfo = fullMethodName.split(" >>> ")[1];
                                createeElemnt(doc,methods,methodNameInfo.split(" : ")[0].trim(), methodNameInfo.split(" : ")[1].trim(), returnFrom);
                            }
                            subReader.close();
                        } catch (FileNotFoundException e) {
                            System.err.println(e.getMessage());
                        }
                    } else {
                        String testOutput = argsList.get(inputTestList)+"target/testOutput/";
                        String subFile = argsList.get(inputTestList)+"sootTimerOutput/";
                        ArrayList<String> result = new ArrayList<String>();
                        try {
                            File testDir = new File(testOutput+name);
                            Scanner testReader = new Scanner(testDir);
                            File temp = new File("new_xml.txt");
                            FileWriter output = new FileWriter(temp);
                            BufferedWriter writer_2 = new BufferedWriter(output);
                            while(testReader.hasNextLine()){
                                String fullMethodName = testReader.nextLine();
                                File subDir = new File(subFile+name);
                                Scanner subReader = new Scanner(subDir);
                                while(subReader.hasNextLine()){
                                    String method_name = subReader.nextLine();
                                    String subMethod = method_name.split(" >>> ")[1];
                                    if(subMethod.split(" : ")[0].trim().equals(fullMethodName.trim())){
                                        createeElemnt(doc,methods,subMethod.split(" : ")[0].trim(), subMethod.split(" : ")[1].trim(), returnFrom);
                                        result.add(subMethod);
                                        break;
                                    }
                                }
                                subReader.close();
                            }
                            testReader.close();
                            File subDir = new File(subFile+name);
                            Scanner subReader = new Scanner(subDir);
                            while(subReader.hasNextLine()){
                                String methodName = subReader.nextLine();
                                String subMethod = methodName.split(" >>> ")[1];
                                Boolean found = false;
                                for (int i = 0; i < result.size(); i++) {
                                    if(subMethod.trim().equals(result.get(i).trim())){
                                        result.remove(i);
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    writer_2.write(methodName + "\n");
                                }
                            }
                            subReader.close();
                            writer_2.close();
                            temp.renameTo(subDir);
                        } catch (FileNotFoundException e) {
                            System.err.println(e.getMessage());
                        }
                    }
                    createeElemntend(doc,test,methods,testInfo[3].trim().split("#")[1]);
                    root.appendChild(test);
                }

                doc.appendChild(root);
                DOMSource source=new DOMSource(doc);
                String path="sootXMLOutput" + File.separator + paths[paths.length - 4] + "-runtime.xml";
                File f=new File(path);
                Result result=new StreamResult(f);
                TransformerFactory transformerFactory=TransformerFactory.newInstance();
                Transformer transformer=TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"no");
                transformer.setOutputProperty(OutputKeys.INDENT,"yes");
                transformer.transform(source,result);



            } catch (ParserConfigurationException e) {
                System.err.println(e.getMessage());
            } catch (TransformerConfigurationException e) {
                System.err.println(e.getMessage());
            } catch (TransformerException e) {
                System.err.println(e.getMessage());
            }
    }

    private static void createeElemnthead(Document doc,Element test,String test_val,String execution_time_val)
    {
        Element test_name=doc.createElement("testName");
        Element execution_time=doc.createElement("startTime");
        test_val= iffull(test_val);
        execution_time_val= iffull(execution_time_val);
        Text testNamevalue=doc.createTextNode(test_val);
        test_name.appendChild(testNamevalue);
        Text executionvalue=doc.createTextNode(execution_time_val);
        execution_time.appendChild(executionvalue);
        test.appendChild(test_name);
        test.appendChild(execution_time);

    }
    private static void createeElemnt(Document doc,Element methods,String method_under_test_name_val,String execution_time_val,String throw_exception_val)
    {

        Element method_under_test_name=doc.createElement("methodUnderTestName");
        Element execution_time=doc.createElement("executionTime");
        Element throw_exception=doc.createElement("throwException");
        method_under_test_name_val= iffull(method_under_test_name_val);
        execution_time_val= iffull(execution_time_val);
        throw_exception_val= iffull(throw_exception_val);
        Text methodvalue=doc.createTextNode(method_under_test_name_val);
        method_under_test_name.appendChild(methodvalue);
        Text executionvalue=doc.createTextNode(execution_time_val);
        execution_time.appendChild(executionvalue);
        Text exceptionvalue=doc.createTextNode(throw_exception_val);
        throw_exception.appendChild(exceptionvalue);
        methods.appendChild(method_under_test_name);
        methods.appendChild(execution_time);
        methods.appendChild(throw_exception);

    }

    private static void createeElemntend(Document doc,Element test,Element methods,String execution_time_val)
    {
        Element execution_time=doc.createElement("endTime");
        execution_time_val= iffull(execution_time_val);
        Text executionvalue=doc.createTextNode(execution_time_val);
        execution_time.appendChild(executionvalue);
        test.appendChild(methods);
        test.appendChild(execution_time);

    }

    private static String iffull(String val)
    {
        if(val=="")
        {
            val=val.replace(""," ");
        }
        return val;
    }
}
