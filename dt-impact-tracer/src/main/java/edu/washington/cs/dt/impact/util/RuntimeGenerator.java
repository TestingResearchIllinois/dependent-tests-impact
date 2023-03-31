package edu.washington.cs.dt.impact.util;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.*;

import static com.github.javaparser.utils.Utils.assertNotNull;

/**
 * This class is responsible for creating runtime xml
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

    public static void main(String[] args) throws IOException{
        List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        int inputTestListIndex = argsList.indexOf("-inputFile");
        int inputTestList = inputTestListIndex + 1;
        File theDir = new File("sootXMLOutput");
        String[] paths = argsList.get(inputTestList).split("/");
        System.out.println("File: " + paths[paths.length - 4]);
        tryCreateDirectory(theDir);
        FileWriter fileWriter = null;
        File rootFile = new File(argsList.get(inputTestList) + "sootTracerData/tempTracerData.txt");
        //tryCreateDirectory(rootFile);
        Scanner fileReader = new Scanner(rootFile);
        String path="sootXMLOutput" + File.separator + paths[paths.length - 4] + "-runtime.xml";
        FileWriter xmlFile = new FileWriter(path);
        xmlFile.write("<?xml version=\"1.0\"?>\n");
        xmlFile.write("<testList>");
        //String id="1";
        int parentId=1;
        String dataParent=null;
        String prevData="";
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        xmlMapper.getFactory().getXMLOutputFactory().setProperty("javax.xml.stream.isRepairingNamespaces", false);
        TestClass tc = new TestClass("");
        while (fileReader.hasNextLine())
        {


            if(parentId==1){

                prevData= fileReader.nextLine();
                String[] prevInfo = prevData.split(",");
                tc.setName(prevInfo[1]);
                dataParent=fileReader.nextLine();
            }
            else
            {
                String[] prevInfo = prevData.split(",");
                //tc.setName(prevInfo[1]);
                if(tc.getTestClassByName(prevInfo[1])==null)
                {
                    String xml = xmlMapper.writeValueAsString(tc);
                    assertNotNull(xml);
                    xmlFile.write(xml + "\n");
                    tc = new TestClass(prevInfo[1]);

                }
                dataParent=fileReader.nextLine();
            }
            String[] testInfoParent = dataParent.split(",");

            String parentMethod=testInfoParent[0];
            String nextmethod=testInfoParent[1];
            long prevTime= Long.parseLong(testInfoParent[2]);
            int prev=0;
            long timeDiff=0;
            Method parent = new Method(Integer.toString(parentId),parentMethod,timeDiff,true,false);
            int expectedexceptionflag=0;
            String expectedexc="";
            if(nextmethod.contains("Exception:-expected-"))
            {
                expectedexceptionflag=1;
                expectedexc=nextmethod.replaceAll("Exception:-expected- ", "");
            }
            String childId="0";
            int skipflag=0;
            int exceptionflag=0;
            while (fileReader.hasNextLine())
            {
                String data = fileReader.nextLine();
                prevData=data;
                String[] testInfo = data.split(",");

                String startMethod=testInfo[0];
                String endMethod=testInfo[1];
                long time= Long.parseLong(testInfo[2]);
                if((expectedexceptionflag==1)&&(endMethod.contains(expectedexc)))
                {
                    timeDiff=time-prevTime;
                    prevTime=time;
                    String currentId=parent.addChild(startMethod,childId,endMethod,timeDiff,true);
                    String[] arrOfStr = currentId.split("\\.");
                    childId="";
                    for (int i=1;i<arrOfStr.length;i++)
                    {
                        if(i==arrOfStr.length-1)
                        {
                            childId+=arrOfStr[i];
                        }
                        else {
                            childId+=arrOfStr[i]+".";
                        }
                    }
                    parent.addTime(timeDiff);
                    expectedexceptionflag=2;
                }
                if(endMethod.contains("Exception:"))
                {
                    exceptionflag=1;
                }
                if(Objects.equals(startMethod, "START"))
                {
                    dataParent=data;
                    break;
                }
                else if(skipflag==1)
                {
                    continue;
                }
                else if(Objects.equals(endMethod, "END"))
                {
                    timeDiff=time-prevTime;
                    skipflag=1;
                    parent.addChild(startMethod,childId,endMethod,timeDiff,false);

                }
                else if(Objects.equals(endMethod, "<init>") || Objects.equals(endMethod, " "))
                {
                    continue;
                }
                else if(endMethod.toString().contains("<init>"))
                {
                    continue;
                }
                else{
                    timeDiff=time-prevTime;
                    prevTime=time;
                    String currentId=parent.addChild(startMethod,childId,endMethod,timeDiff,false);
                    String[] arrOfStr = currentId.split("\\.");
                    childId="";
                    for (int i=1;i<arrOfStr.length;i++)
                    {
                        if(i==arrOfStr.length-1)
                        {
                            childId+=arrOfStr[i];
                        }
                        else {
                            childId+=arrOfStr[i]+".";
                        }
                    }
                    parent.addTime(timeDiff);
                }
            }
            if((skipflag==0) &&(exceptionflag==1))
            {
                parent.setThrowException(true);
            }
            if(expectedexceptionflag==2)
            {
                parent.setThrowException(false);
            }

            tc.addMethod(parent);

            parentId++;
        }
        String xml = xmlMapper.writeValueAsString(tc);
        assertNotNull(xml);
        xmlFile.write(xml + "\n");
        xmlFile.write("</testList>");
        xmlFile.flush();
        xmlFile.close();
    }



}
