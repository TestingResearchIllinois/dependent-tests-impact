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
        xmlFile.write("<MethodList>");
        //String id="1";
        int parentId=1;
        String dataParent=null;

        while (fileReader.hasNextLine())
        {

            if(parentId==1){
                dataParent= fileReader.nextLine();
            }
            String[] testInfoParent = dataParent.split(",");

            String parentMethod=testInfoParent[1];
            String prevm=testInfoParent[0];
            long prevTime= Long.parseLong(testInfoParent[2]);
            int prev=0;
            long timeDiff=0;
            /*if(Objects.equals(parentMethod, "START"))
            {
                continue;
            }*/
            Method parent = new Method(Integer.toString(parentId),parentMethod,timeDiff,false,false);
            String childId="0";

            while (fileReader.hasNextLine())
            {
                String data = fileReader.nextLine();
                String[] testInfo = data.split(",");

                String startMethod=testInfo[0];
                String endMethod=testInfo[1];
                long time= Long.parseLong(testInfo[2]);
                if(Objects.equals(startMethod, "START"))
                {
                    dataParent=data;
                    break;
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
                    //parent.addTime(timeDiff);


                }
            }
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            xmlMapper.getFactory().getXMLOutputFactory().setProperty("javax.xml.stream.isRepairingNamespaces", false);
            String xml = xmlMapper.writeValueAsString(parent);
            assertNotNull(xml);
            xmlFile.write(xml + "\n");
            parentId++;
        }

        xmlFile.write("</MethodList>");
        xmlFile.flush();
        xmlFile.close();
    }



}
