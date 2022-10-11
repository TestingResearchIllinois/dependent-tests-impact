package edu.washington.cs.dt.impact.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

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
        File theDir = new File("sootCsvOutput");
        String[] paths = argsList.get(inputTestList).split("/");
        System.out.println("File: " + paths[paths.length - 4]);
        tryCreateDirectory(theDir);
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            fileWriter = new FileWriter("sootCsvOutput" + File.separator + paths[paths.length - 4] + "-runtime.csv");
            bufferedWriter = new BufferedWriter(fileWriter);
            CSVPrinter csvPrinter = new CSVPrinter(bufferedWriter, CSVFormat.DEFAULT.withHeader("Test Name", "Part", "Method Under Test Name", "Execution Time", "Throw Exception"));
            try {
                File rootFile = new File(argsList.get(inputTestList)+"sootSeqOutput/functionSequence");
                Scanner fileReader = new Scanner(rootFile);
                while (fileReader.hasNextLine()) {
                    String data = fileReader.nextLine();
                    String[] testInfo = data.split(" > ");
                    String initial = testInfo[0].trim();
                    if (!initial.equals("Started")){
                        continue;
                    }
                    String name_info = testInfo[1].trim();
                    String part = name_info.split(" >> ")[0].trim();
                    String name = name_info.split(" >> ")[1].trim();

                    String execInfo = testInfo[4].trim();
                    String return_from = execInfo.split(" >> ")[0].trim();

                    if (part.contains("body")) {
                        String subFile = argsList.get(inputTestList) + "sootTimerOutput/";
                        try {
                            File subDir = new File(subFile+name);
                            Scanner subReader = new Scanner(subDir);
                            while(subReader.hasNextLine()){
                                String fullMethodName = subReader.nextLine();
                                String methodNameInfo = fullMethodName.split(" >>> ")[1];
                                csvPrinter.printRecord(name, part, methodNameInfo.split(" : ")[0].trim(), methodNameInfo.split(" : ")[1].trim(), return_from);
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
                            File temp = new File("new_csv.txt");
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
                                        csvPrinter.printRecord(name, part, subMethod.split(" : ")[0].trim(), subMethod.split(" : ")[1].trim(), return_from);
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
                }
                fileReader.close();
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
            }
            csvPrinter.flush();
        } catch (IOException e){
            System.err.println(e.getMessage());
        } finally {
            fileWriter.close();
            bufferedWriter.close();
        }
    }
}
