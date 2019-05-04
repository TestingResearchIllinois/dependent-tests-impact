/**
 * This class takes as input the prioritization-results and 
 * the parallelization-results directory from executing the 
 * OneConfigurationRunner on a subject with the -resolveDependences
 * flag (to generate the precomputed dependences). The purpose
 * of this class is to parse those results and output the 
 * precomputed dependences to the appropriate locations.
 */

package edu.washington.cs.dt.impact.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.google.common.collect.Streams;

import edu.washington.cs.dt.impact.data.Project;
import edu.washington.cs.dt.impact.figure.generator.FigureGenerator;
import edu.washington.cs.dt.impact.util.Constants;

public class OutputPrecomputedDependences extends FigureGenerator {

	private static String priorOutputDirectoryName = null;
	private static String seleOutputDirectoryName = null;
	private static String paraOutputDirectoryName = null;

	private int maxMachines = 0;

	public static void main(String[] args) {
		List<String> argsList = new ArrayList<String>(Arrays.asList(args));

		// name of directory where files should be outputted
		priorOutputDirectoryName = mustGetArgName(argsList, "-prioOutputDirectory");
		seleOutputDirectoryName = mustGetArgName(argsList, "-seleOutputDirectory");
		paraOutputDirectoryName = mustGetArgName(argsList, "-paraOutputDirectory");

		String priorDirectoryName = mustGetArgName(argsList, "-prioDirectory");
		String paraDirectoryName = mustGetArgName(argsList, "-paraDirectory");

        File[] prioDirList = new File(priorDirectoryName).listFiles();
        File[] paraDirList = new File(paraDirectoryName).listFiles();

        // create a list of project Objects that each have a diff project name
        List<Project> proj_orig_arrayList = new ArrayList<Project>();
        List<Project> proj_auto_arrayList = new ArrayList<Project>();

        // Call super's parse file method and let it parse the files for information and
        // then call doParaCalculations, doSeleCalculations, or doPrioCalculations for each file
        parseFiles(prioDirList, new OutputPrecomputedDependences(), false, proj_orig_arrayList, proj_auto_arrayList);
        parseFiles(paraDirList, new OutputPrecomputedDependences(), false, proj_orig_arrayList, proj_auto_arrayList);
	}

	@Override
	public void doParaCalculations() {
		int index = flagsList.indexOf("-numOfMachines");
		String numMachines_string = flagsList.get(index + 1);
		int numMachines = Integer.parseInt(numMachines_string);

		outputDependences(techniqueName, orderName, coverageName, testType, projectName, numMachines,
                          null, paraOutputDirectoryName,
                          getDTFile(parseFileForDTs(file, Constants.DT_LIST, true)));
		if (numMachines > maxMachines && orderName.equals("original")) {
			outputDependences("selection", orderName, coverageName, testType, projectName, numMachines,
                              "function", seleOutputDirectoryName,
                              getDTFile(parseFileForDTs(file, Constants.DT_LIST, true)));
			outputDependences("selection", orderName, coverageName, testType, projectName, numMachines,
                              "statement", seleOutputDirectoryName,
                              getDTFile(parseFileForDTs(file, Constants.DT_LIST, true)));
			maxMachines = numMachines;
		}
	}

	@Override
	public void doPrioCalculations() {
		outputDependences(techniqueName, orderName, coverageName, testType, projectName, -1,
                          null, priorOutputDirectoryName,
                          getDTFile(parseFileForDTs(file, Constants.DT_LIST, true)));
		outputDependences("selection", orderName, coverageName, testType, projectName,
                          -1, null, seleOutputDirectoryName,
                          getDTFile(parseFileForDTs(file, Constants.DT_LIST, true)));
	}

	public static void outputDependences(String techniqueName, String orderName, String coverageName,
										 String testType, String projectName, int numMachines,
										 String customCoverage, String outputDirStr, String fileContents) {
		StringBuilder fileName = new StringBuilder();
		fileName.append(techniqueName);
		fileName.append("-");
		fileName.append(projectName);
		fileName.append("-");
		fileName.append(testType);
		fileName.append("-");
		if (techniqueName.equals("parallelization")) {
			fileName.append(numMachines);
		} else {
			if (customCoverage == null) {				
				fileName.append(coverageName);
			} else {
				fileName.append(customCoverage);
			}
		}
		fileName.append("-");
		fileName.append(orderName);
		fileName.append(".txt");

        File directory = new File(outputDirStr);
        if (! directory.exists()){
            directory.mkdir();
        }

		writeToLatexFile(fileContents, outputDirStr + File.separator + fileName.toString(), false);
	}

	public static String getDTFile(List<String> dtList) {
        StringBuilder fileContents = new StringBuilder();
        for (int j = 0; j < dtList.size();) {
            for (int i = 0; i < 5; j++) {
                fileContents.append(dtList.get(j) + "\n");
                i++;
            }
            fileContents.append("\n");
        }
        return fileContents.toString();
    }

	@Override
	public void doSeleCalculations() {
		// doNothing
	}
}