import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.apache.commons.io.FileUtils;

public class FileCompare{

    public static long filesCompareByLine(String str_path1, String str_path2) throws IOException {
	Path path1 = Paths.get(str_path1);
	Path path2 = Paths.get(str_path2);
	try (BufferedReader bf1 = Files.newBufferedReader(path1);
         BufferedReader bf2 = Files.newBufferedReader(path2)) {
	    long lineNumber = 1;
	    String line1 = "", line2 = "";
	    while ((line1 = bf1.readLine()) != null) {
		line2 = bf2.readLine();
		if (line2 == null || !line1.equals(line2)) {
		    return lineNumber;
		}
		lineNumber++;
	    }
	    if (bf2.readLine() == null) {
		return -1;
	    }
	    else {
		return lineNumber;
	    }
	} catch (FileNotFoundException e){
	    System.out.println("Not Found");
	    return -500; 
	}
    }

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
        int firstFileIndex = argsList.indexOf("-firstFile");
	int secondFileIndex = argsList.indexOf("-secondFile");
	String first_file = argsList.get(firstFileIndex+1);
	String second_file = argsList.get(secondFileIndex+1);
	File folder = new File(first_file);
	File[] listOfFiles = folder.listFiles();
        File theDir = new File("matchingOutput");
        // if the directory does not exist, create it
        tryCreateDirectory(theDir);
	FileWriter output = null;
        BufferedWriter writer = null;
	try {
	    output = new FileWriter("matchingOutput" + File.separator + "nomatch", true);
            writer = new BufferedWriter(output);
	    for (int i = 0; i < listOfFiles.length; i++) {
		if (listOfFiles[i].isFile()) {
		    String input_file =  listOfFiles[i].getName();
		    try {
			String primary_file = first_file+File.separator+input_file;
			String secondary_file = second_file+File.separator+input_file;
			long x = filesCompareByLine(primary_file, secondary_file);
			if (x!= -1 && x != -500) {
			    writer.write(input_file);
			    System.out.println("Files: " + input_file);
			}
		    } catch (Exception e) {
			System.out.println("File Not Found in Second List" + listOfFiles[i].getName());
		    }
		}
	    }
	} catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                // Ignore issues during closing
            }
        }
	/*try {
	    File rootFile = new File(argsList.get(inputTestList)+"sootSeqOutput/test");
	    Scanner fileReader = new Scanner(rootFile);
	    while (fileReader.hasNextLine()) {
		String data = fileReader.nextLine();
	    }
	} catch (FileNotFoundException e) {
	}*/
    }
}
