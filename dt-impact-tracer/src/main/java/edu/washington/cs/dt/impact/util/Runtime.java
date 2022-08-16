import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Stack;
import java.io.FileNotFoundException;
import java.util.Scanner;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;


public class Runtime{
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
	FileWriter x = null;
	BufferedWriter writer = null;
	try {
	    x = new FileWriter("sootCsvOutput" + File.separator + paths[paths.length - 4] + "-test.csv");
	    writer = new BufferedWriter(x);
	    CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
						   .withHeader("Test Name", "Part", "Method Unter Test Name", "Execution Time", "Throw Exception"));
	    try {
		File rootFile = new File(argsList.get(inputTestList)+"sootSeqOutput/test");
		Scanner fileReader = new Scanner(rootFile);
		HashMap<String, Integer> set_up = new HashMap<String, Integer>();
		HashMap<String, Integer> tear_down = new HashMap<String, Integer>();
		while (fileReader.hasNextLine()) {
		    String data = fileReader.nextLine();
		    String[] test_info = data.split(" > ");
		    String initial = test_info[0].trim();
		    if (!initial.equals("Started")){
			continue;
		    }
		    String name_info = test_info[1].trim();
		    String part = name_info.split(" >> ")[0].trim();
		    String name = name_info.split(" >> ")[1].trim();
		    String q_name = test_info[3].trim();
		    String exec_info = test_info[4].trim();
		    String return_from = exec_info.split(" >> ")[0].trim();
		    String exec_time = exec_info.split(" >> ")[1].trim().split(" : ")[1].trim();
		    if (part.contains("body")) {
			String subFile = argsList.get(inputTestList) + "sootTimerOutput/";
			try {
			    File subDir = new File(subFile+name);
			    Scanner subReader = new Scanner(subDir);
			    while(subReader.hasNextLine()){
				String full_method_name = subReader.nextLine();
				String method_name_info = full_method_name.split(" >>> ")[1];
				csvPrinter.printRecord(name, part, method_name_info.split(" : ")[0].trim(), method_name_info.split(" : ")[1].trim(), return_from);
			    }
			    subReader.close();
			} catch (FileNotFoundException e) {
			    
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
				String full_method_name = testReader.nextLine();
				File subDir = new File(subFile+name);
				Scanner subReader = new Scanner(subDir);
				while(subReader.hasNextLine()){
				    String method_name = subReader.nextLine();
				    String sub_method = method_name.split(" >>> ")[1];
				    if(sub_method.split(" : ")[0].trim().equals(full_method_name.trim())){
					csvPrinter.printRecord(name, part, sub_method.split(" : ")[0].trim(), sub_method.split(" : ")[1].trim(), return_from);
				        result.add(sub_method);
					break;
				    }
				}
				subReader.close();
			    }
			    testReader.close();
			    File subDir = new File(subFile+name);
			    Scanner subReader = new Scanner(subDir);
			    while(subReader.hasNextLine()){
				String method_name = subReader.nextLine();
				String sub_method = method_name.split(" >>> ")[1];
				Boolean found = false;
				for (int i = 0; i < result.size(); i++) {
				    if(sub_method.trim().equals(result.get(i).trim())){
					result.remove(i);
					found = true;
					break;
				    }
				}
				if (!found) {
				    writer_2.write(method_name + "\n");
				}
			    }
			    subReader.close();
			    writer_2.close();
			    temp.renameTo(subDir);
			} catch (FileNotFoundException e) {
			    
			}
		    }
		}
		fileReader.close();
	    } catch (FileNotFoundException e) {
		
	    }
            csvPrinter.flush();
	    x.close();
	    writer.close();
        } catch (IOException e){
	    x.close();
	    writer.close();
	}
    }
}
