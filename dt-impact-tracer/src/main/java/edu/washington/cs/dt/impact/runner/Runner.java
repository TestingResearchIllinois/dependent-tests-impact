package edu.washington.cs.dt.impact.runner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.reedoei.testrunner.data.results.TestRunResult;
import com.reedoei.testrunner.runner.SmartRunner;

import edu.washington.cs.dt.TestExecResult;
import edu.washington.cs.dt.TestExecResults;
import edu.washington.cs.dt.impact.data.TestFunctionStatement;
import edu.washington.cs.dt.impact.data.WrapperTestList;
import edu.washington.cs.dt.impact.figure.generator.FigureGenerator;
import edu.washington.cs.dt.impact.technique.Test;
import edu.washington.cs.dt.impact.tools.DependentTestFinder;
import edu.washington.cs.dt.impact.tools.FileTools;
import edu.washington.cs.dt.impact.util.Constants;
import edu.washington.cs.dt.impact.util.Constants.COVERAGE;
import edu.washington.cs.dt.impact.util.Constants.DT_SETTING;
import edu.washington.cs.dt.impact.util.Constants.MACHINES;
import edu.washington.cs.dt.impact.util.Constants.ORDER;
import edu.washington.cs.dt.impact.util.Constants.TD_SETTING;
import edu.washington.cs.dt.impact.util.Constants.TECHNIQUE;
import edu.washington.cs.dt.impact.util.Constants.TEST_TYPE;
import edu.washington.cs.dt.main.ImpactMain;

public abstract class Runner {
    protected TECHNIQUE techniqueName = null;
    protected COVERAGE coverage = null;
    protected ORDER order = null;
    protected File testInputDir = new File(Constants.DEFAULT_TEST_DIR);
    protected File outputDir = new File(".");
    protected String outputFileName = null;
    protected File origOrder = null;
    protected File selectionOutput1 = null;
    protected File selectionOutput2 = null;
    protected Constants.MACHINES numOfMachines = MACHINES.ONE;
    protected int timesToRun = 1;
    protected int timesToRunCheckTestOrderNOD = 3;
    protected String project = "CRYSTAL";
    protected Constants.TEST_TYPE testType = TEST_TYPE.ORIG;
    protected List<String> allDTList;
    protected File dependentTestFile = null;
    protected File resolveDependences = null;
    protected String filesToDeleteStr = null;
    protected boolean getCoverage = false;
    protected List<String> filesToDelete = null;
    protected List<String> argsList = null;
    protected double TLGTime = 0.0;
    protected List<WrapperTestList> listTestList = new ArrayList<>();
    protected List<String> origOrderTestList = null;
    protected File timeOrder = null;
    protected String classPath = System.getProperty("java.class.path");
    protected boolean postProcessDTs = false;

    private static final int MAX_ARRAY_SIZE_TO_WRITE = 5000; // 5,000

    protected SmartRunner runner;

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    public void setRunner(SmartRunner runner) {
        this.runner = runner;
    }

    public void parseArgs(String[] args) {
        argsList = new ArrayList<String>(Arrays.asList(args));
        if (argsList.contains("-help")) {
            System.out.println("Main class that relies on program arguments to generate a regression testing "
                    + "execution order. The following options are supported:\n"
                    + "-technique - prioritization, selection, parallelization\n" + "-coverage - statement, function\n"
                    + "-order - absolute, relative, random, original, time (if technique is parallelization)\n"
                    + "-resolveDependences - when specified the output will not be affected " + "by dependent tests\n"
                    + "-numMachines - integer value (only valid when technique is parallelization\n"
                    + "-selectionOldVers - path to directory to older version of program's selectionOutput "
                    + "(only valid when technique is selection)\n"
                    + "-selectionNewVers - path to directory to newer version of program's selectionOutput "
                    + "(only valid when technique is selection)\n"
                    + "-origOrder - path to file containing the original order the tests are executed in\n"
                    + "-testInputDir - path to directory to sootTestOutput\n"
                    + "-dependentTestFile - path to file containing existing known dependent tests\n"
                    + "-filesToDelete - path to file containing list of files to delete to clean the environment "
                    + "each time the tests are executed\n"
                    + "-outputFile - path to file to output the regression test order, dependent test list and "
                    + "execution time, if unspecified the output will be sent to System.out\n"
                    + "-help - display this message\n");
            System.exit(0);
        }

        // get the technique
        int techniqueIndex = argsList.indexOf("-technique");
        if (techniqueIndex != -1) {
            // get index of technique name
            int techniqueNameIndex = techniqueIndex + 1;
            if (techniqueNameIndex >= argsList.size()) {
                System.err.println("Technique argument is specified but technique name is not."
                        + " Please use the format: -technique aTechniqueName");
                System.exit(0);
            }

            String techniqueStr = argsList.get(techniqueNameIndex).toLowerCase().trim();
            if (techniqueStr.equals("prioritization")) {
                techniqueName = TECHNIQUE.PRIORITIZATION;
            } else if (techniqueStr.equals("selection")) {
                techniqueName = TECHNIQUE.SELECTION;
            } else if (techniqueStr.equals("parallelization")) {
                techniqueName = TECHNIQUE.PARALLELIZATION;
            } else {
                System.err.println("Technique name is invalid. Try \"prioritization\","
                        + " \"selection\" or \"parallelization\".");
                System.exit(0);
            }
        } else {
            System.err.println(
                    "No technique argument is specified." + " Please use the format: -technique aTechniqueName");
            System.exit(0);
        }

        // get the coverage
        int coverageIndex = argsList.indexOf("-coverage");
        if (coverageIndex != -1) {
            int coverageNameIndex = coverageIndex + 1;
            if (coverageNameIndex >= argsList.size()) {
                System.err.println("Coverage argument is specified but valid coverage was not."
                        + " Please use the format: -coverage aCoverageName");
                System.exit(0);
            }
            String coverageStr = argsList.get(coverageNameIndex).trim().toLowerCase();
            if (coverageStr.equals("statement")) {
                coverage = COVERAGE.STATEMENT;
            } else if (coverageStr.equals("function")) {
                coverage = COVERAGE.FUNCTION;
            } else {
                System.err.println("Coverage is invalid. Try \"statement\", \"branch\" or \"function\".");
                System.exit(0);
            }
        } else if (techniqueName != TECHNIQUE.PARALLELIZATION) {
            System.err
                    .println("No coverage argument is specified." + " Please use the format: -coverage aCoverageName");
            System.exit(0);
        }

        // get the order
        int orderIndex = argsList.indexOf("-order");
        if (orderIndex != -1) {
            int orderNameIndex = orderIndex + 1;
            if (orderNameIndex >= argsList.size()) {
                System.err.println("Order argument is specified but valid order was not."
                        + " Please use the format: -order aOrderName");
                System.exit(0);
            }
            String orderStr = argsList.get(orderNameIndex).trim().toLowerCase();
            if (orderStr.equals("absolute")) {
                order = ORDER.ABSOLUTE;
            } else if (orderStr.equals("relative")) {
                order = ORDER.RELATIVE;
            } else if (orderStr.equals("random")) {
                order = ORDER.RANDOM;
            } else if (orderStr.equals("original")) {
                order = ORDER.ORIGINAL;
            } else if (orderStr.equals("time") && techniqueName == TECHNIQUE.PARALLELIZATION) {
                int timeOrderIndex = argsList.indexOf("-timeOrder");
                if (timeOrderIndex != -1) {
                    // get file for the time each test took
                    int timeOrderNameIndex = timeOrderIndex + 1;
                    if (timeOrderNameIndex >= argsList.size()) {
                        System.err.println("Time order argument is specified but a directory path"
                                + " is not. Please use the format: -timeOrder aFilePath");
                        System.exit(0);
                    }
                    timeOrder = new File(argsList.get(timeOrderNameIndex));
                    if (!timeOrder.isFile()) {
                        System.err.println("Time order argument is specified but the file path"
                                + " is invalid. Please check the file path.");
                        System.exit(0);
                    }
                    order = ORDER.TIME;
                } else {
                    System.err.println("Time is specified as the order but no -timeOrder is "
                            + " supplied. Please add -timeOrder aFilePath argument.");
                    System.exit(0);
                }
            } else {
                System.err.println("Order is invalid. Please input a valid order.\n"
                        + "Try \"absolute\", \"relative\", \"random\", or \"original\".\n"
                        + "If your technique is parallelization then \"time\" is also valid.");
                System.exit(0);
            }
        } else {
            System.err.println("No order argument is specified." + " Please use the format: -order aOrderName");
            System.exit(0);
        }

        // get directory for the input of test files, the default is sootTestOutput
        int testInputDirIndex = argsList.indexOf("-testInputDir");
        if (testInputDirIndex != -1) {
            int testInputDirNameIndex = testInputDirIndex + 1;
            if (testInputDirNameIndex >= argsList.size()) {
                System.err.println("Test input directory argument is specified but a directory name is not."
                        + " Please use the format: -testInputDir aDirName");
                System.exit(0);
            }
            testInputDir = new File(argsList.get(testInputDirNameIndex));
        } else {
            System.err.println("No test input directory argument is specified."
                    + " Please use the format: -testInputDir aDirectoryPath");
            System.exit(0);
        }

        // get directory to output the files
        int outputDirIndex = argsList.indexOf("-outputDir");
        if (outputDirIndex != -1) {
            int outputDirNameIndex = outputDirIndex + 1;
            if (outputDirNameIndex >= argsList.size()) {
                System.err.println("Output directory argument is specified but a directory name is not."
                        + " Please use the format: -outputDir aDirName");
                System.exit(0);
            }
            outputDir = new File(argsList.get(outputDirNameIndex));
        }

        // get the classpath to run tests with
        int classPathIndex = argsList.indexOf("-classpath");
        if (classPathIndex != -1) {
            int classPathValIndex = classPathIndex + 1;
            if (classPathValIndex >= argsList.size()) {
                System.err.println("Classpath argument is specified but a value is not."
                        + " Please use the format: -classpath classpath");
                System.exit(0);
            }

            classPath = ImpactMain.buildClassPath(argsList.get(classPathValIndex).split(":"));
        }

        // if specified, the output is saved to the file name instead of printed to console
        int outputFile = argsList.indexOf("-outputFile");
        if (outputFile != -1) {
            // get index of output file
            int outputFileNameIndex = outputFile + 1;
            if (outputFileNameIndex >= argsList.size()) {
                System.err.println("Output file argument is specified but a file name is not."
                        + " Please use the format: -outputFile aFileName");
                System.exit(0);
            }
            outputFileName = argsList.get(outputFileNameIndex);
        }

        // get file for the original order in which the tests should be ordered
        int origOrderIndex = argsList.indexOf("-origOrder");
        if (origOrderIndex != -1) {
            int origOrderNameIndex = origOrderIndex + 1;
            if (origOrderNameIndex >= argsList.size()) {
                System.err.println("Original order argument is specified but a directory"
                        + " path is not. Please use the format: -origOrder aFilePath");
                System.exit(0);
            }
            origOrder = new File(argsList.get(origOrderNameIndex));
            if (!origOrder.isFile()) {
                System.err.println("Original order argument is specified but the file"
                        + " path is invalid. Please check the file path. Path is: " + argsList.get(origOrderNameIndex));
                System.exit(0);
            }
        } else {
            System.err.println(
                    "No original order argument is specified." + " Please use the format: -origOrder aFileName");
            System.exit(0);
        }

        if (techniqueName == TECHNIQUE.SELECTION) {
            // get directory of old version's selection output
            int oldVersCFGIndex = argsList.indexOf("-oldVersCFG");
            if (oldVersCFGIndex != -1) {
                int oldVersCFGNameIndex = oldVersCFGIndex + 1;
                if (oldVersCFGNameIndex >= argsList.size()) {
                    System.err.println("Old version CFG argument is specified but a directory"
                            + " path is not. Please use the format: -oldVersCFG aDirPath");
                    System.exit(0);
                }
                selectionOutput1 = new File(argsList.get(oldVersCFGNameIndex));
                if (!selectionOutput1.isDirectory()) {
                    System.err.println("Old version CFG argument is specified but the directory"
                            + " path is invalid. Please check the directory path. The input path was: "
                            + selectionOutput1);
                    System.exit(0);
                }
            } else {
                System.err.println(
                        "No old version CFG argument is specified." + " Please use the format: -oldVersCFG aDirPath");
                System.exit(0);
            }

            // get directory of new version's selection output
            int newVersCFGIndex = argsList.indexOf("-newVersCFG");
            if (newVersCFGIndex != -1) {
                int newVersCFGNameIndex = newVersCFGIndex + 1;
                if (newVersCFGNameIndex >= argsList.size()) {
                    System.err.println("New version CFG argument is specified but a directory"
                            + " path is not. Please use the format: -newVersCFG aDirPath");
                    System.exit(0);
                }
                selectionOutput2 = new File(argsList.get(newVersCFGNameIndex));
                if (!selectionOutput2.isDirectory()) {
                    System.err.println("New version CFG argument is specified but the directory"
                            + " path is invalid. Please check the directory path. The input path was: "
                            + selectionOutput2);
                    System.exit(0);
                }
            } else {
                System.err.println(
                        "No new version CFG argument is specified." + " Please use the format: -newVersCFG aDirPath");
                System.exit(0);
            }
        }

        if (techniqueName == TECHNIQUE.PARALLELIZATION) {
            int numOfMachinesIndex = argsList.indexOf("-numOfMachines");
            if (numOfMachinesIndex != -1) {
                int numOfMachinesIntIndex = numOfMachinesIndex + 1;
                if (numOfMachinesIntIndex >= argsList.size()) {
                    System.err.println("Number of machines argument is specified but a integer"
                            + " is not. Please use the format: -numOfMachines aInteger");
                    System.exit(0);
                }
                int machines = Integer.parseInt(argsList.get(numOfMachinesIntIndex));
                if (machines == 2) {
                    numOfMachines = MACHINES.TWO;
                } else if (machines == 4) {
                    numOfMachines = MACHINES.FOUR;
                } else if (machines == 8) {
                    numOfMachines = MACHINES.EIGHT;
                } else if (machines == 16) {
                    numOfMachines = MACHINES.SIXTEEN;
                } else {
                    System.err.println("Number of machines argument is specified but the integer"
                            + " value provided is invalid. Please input either 2, 4, 8 or 16.");
                    System.exit(0);
                }
            }
        }

        int timesToRunIndex = argsList.indexOf("-timesToRun");
        if (timesToRunIndex != -1) {
            int timesToRunIntIndex = timesToRunIndex + 1;
            if (timesToRunIntIndex >= argsList.size()) {
                System.err.println("Times to run argument is specified but a integer"
                        + " is not. Please use the format: -timesToRun aInteger");
                System.exit(0);
            }
            timesToRun = Integer.parseInt(argsList.get(timesToRunIntIndex));
            if (timesToRun < 1) {
                System.err.println("Times to run argument is specified but the integer"
                        + " value provided is invalid. Please check the integer value.");
                System.exit(0);
            }
        }

        int timesToRunCheckTestOrderNODIndex = argsList.indexOf("-timesToRunCheckTestOrderNOD");
        if (timesToRunCheckTestOrderNODIndex != -1) {
            int timesToRunCheckTestOrderNODIntIndex = timesToRunCheckTestOrderNODIndex + 1;
            if (timesToRunCheckTestOrderNODIntIndex >= argsList.size()) {
                System.err.println("Times to run to check test order for NODs argument is specified but a integer"
                                           + " is not. Please use the format: -timesToRun aInteger");
                System.exit(0);
            }
            timesToRunCheckTestOrderNOD = Integer.parseInt(argsList.get(timesToRunCheckTestOrderNODIntIndex));
            if (timesToRunCheckTestOrderNOD < 1) {
                System.err.println("Times to run to check test order for NODs is specified but the integer"
                                           + " value provided is invalid. Please check the integer value.");
                System.exit(0);
            }
        }

        int projectIndex = argsList.indexOf("-project");
        if (projectIndex != -1) {
            int projectStrIndex = projectIndex + 1;
            if (projectStrIndex >= argsList.size()) {
                System.err.println("Project argument is specified but a project name"
                        + " is not. Please use the format: -project aProjectName");
                System.exit(0);
            }
            project = argsList.get(projectStrIndex);
        } else {
            System.err.println("No project argument is specified." + " Please use the format: -project aProjectName");
            System.exit(0);
        }

        int testTypeIndex = argsList.indexOf("-testType");
        if (testTypeIndex != -1) {
            int testTypeStrIndex = testTypeIndex + 1;
            if (testTypeStrIndex >= argsList.size()) {
                System.err.println("Test type argument is specified but a test type name"
                        + " is not. Please use the format: -testType aTestTypeName");
                System.exit(0);
            }
            String testTypeName = argsList.get(testTypeStrIndex);
            if (testTypeName.equals("orig")) {
                testType = TEST_TYPE.ORIG;
            } else if (testTypeName.equals("auto")) {
                testType = TEST_TYPE.AUTO;
            } else {
                System.err.println("Test type argument is specified but the test type name"
                        + " value provided is invalid. Please use either orig or auto.");
                System.exit(0);
            }
        } else {
            System.err
                    .println("No test type argument is specified." + " Please use the format: -testType aTestTypeName");
            System.exit(0);
        }

        // if specified, the test list generated will consider the dependencies in this file
        int dependentFileIndex = argsList.indexOf("-dependentTestFile");
        if (dependentFileIndex != -1) {
            // get index of output file
            int dependentFileNameIndex = dependentFileIndex + 1;
            if (dependentFileNameIndex >= argsList.size()) {
                System.err.println("Dependent test file argument is specified but a file name is not."
                        + " Please use the format: -dependentTestFile aFileName");
                System.exit(0);
            }
            dependentTestFile = new File(argsList.get(dependentFileNameIndex));
            if (dependentTestFile.isDirectory()) {
                try {
                    dependentTestFile = new File(dependentTestFile.getCanonicalPath()
                            + System.getProperty("file.separator") + Constants.getDTListFileName(project, testType));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            allDTList = FileTools.parseFileToList(dependentTestFile);
        } else {
            allDTList = null;
        }

        int resolveDependencesIndex = argsList.indexOf("-resolveDependences");
        if (resolveDependencesIndex != -1) {
        	int resolveDependencesFileIndex = resolveDependencesIndex + 1;
        	if (resolveDependencesFileIndex >= argsList.size()) {
        		FigureGenerator.exitWithError("resolveDependences file argument is specified but a file name is not."
                        + " Please use the format: -resolveDependences aFileName");
        	}
        	resolveDependences = new File(argsList.get(resolveDependencesFileIndex));
        	int counter = 1;
        	while (resolveDependences.isFile()) {
        		try {
					resolveDependences = new File(resolveDependences.getCanonicalPath() + counter);
				} catch (IOException e) {
					FigureGenerator.exitWithError("Error creating unique resolveDependences file.");
				}
        	}
        }

        // file containing list of files that should be deleted before the test suite is executed
        // again
        int filesToDeleteIndex = argsList.indexOf("-filesToDelete");
        if (filesToDeleteIndex != -1) {
            int filesToDeleteFileIndex = filesToDeleteIndex + 1;
            if (filesToDeleteFileIndex >= argsList.size()) {
                System.err.println("Files to delete argument is specified but a file name is not."
                        + " Please use the format: -filesToDelete aFileName");
                System.exit(0);
            }
            filesToDeleteStr = argsList.get(filesToDeleteFileIndex);
        }

        getCoverage = argsList.contains("-getCoverage");

        // double totalTimeOrigOrder = 1.0;
        // if (techniqueName == TECHNIQUE.PARALLELIZATION) {
        // WrapperTestList origOrderTestListData = new WrapperTestList();
        // setTestListMedianTime(timesToRun, filesToDelete, origOrderTestList, origOrderTestListData);
        // totalTimeOrigOrder = origOrderTestListData.getNewOrderTime();
        // }

        postProcessDTs = argsList.contains("-postProcessDTs");

        if (outputFileName == null) {
            outputFileName = Constants.getOutputFileName(coverage, techniqueName, order, project, testType,
                    numOfMachines, resolveDependences != null ? DT_SETTING.FIXED_DT : DT_SETTING.CONTAINS_DT,
                    allDTList == null ? TD_SETTING.OMITTED_TD : TD_SETTING.GIVEN_TD, postProcessDTs);
        }

        filesToDelete = FileTools.parseFileToList(new File(filesToDeleteStr));
        origOrderTestList = FileTools.parseFileToList(origOrder);

        List<String> extraFiles = new ArrayList<String>(origOrderTestList);

        try {
            Files.list(testInputDir.toPath()).forEach(path -> {
                final File fileEntry = path.toFile();
                if (fileEntry.isFile() && !fileEntry.getName().startsWith(".") && !fileEntry.isHidden()) {
                    extraFiles.remove(fileEntry.getName());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        origOrderTestList.removeAll(extraFiles);
    }

    protected void output(boolean outputDTListSeparately) {
        output(outputDTListSeparately, null, null);
    }

    protected void output(boolean outputDTListSeparately, String withNIterationTime,
            String withoutNIterationTime) {
        FileTools.clearEnv(filesToDelete);
        double totalTime = TLGTime;
        double maxTime = Double.MIN_VALUE;
        double testListTime;
        int numTests = 0;
        List<String> outputArr = new ArrayList<>();
        outputArr.add(Constants.ARGUMENT_STRING + "\n");
        outputArr.add(argsList + "\n\n");

        int i = 0;
        while (i < listTestList.size()) {
            for (; i < listTestList.size(); i++) {
                WrapperTestList testList = listTestList.get(i);

                testListTime = testList.getNewOrderTime();
                totalTime += testListTime;
                maxTime = Math.max(maxTime, testListTime);

                if (testList.getTestList() != null) {
                    numTests += testList.getTestList().size();
                }

                outputArr.addAll(testList.output(getCoverage, origOrderTestList));

                // Size of the output array is getting too big. We will write this to a file first then continue4
                if (outputArr.size() > MAX_ARRAY_SIZE_TO_WRITE) {
                    break;
                }
            }

            if (i < listTestList.size()) {
                writeToFile(outputArr, false);
                outputArr.clear();
            }
        }

        outputArr.add("Total time (of all machines and iterations plus initial TestListGenerator): "
                + millisecondToSecond(totalTime));
        if (withNIterationTime != null && withoutNIterationTime != null) {
            outputArr.add(
                    "\nRuntime to generate the dependent test list including nIterations time: " + withNIterationTime);
            outputArr.add(
                    "\nRuntime to generate the dependent test list without nIterations time: " + withoutNIterationTime);
        }
        if (techniqueName == TECHNIQUE.PARALLELIZATION) {
            outputArr.add("\n" + Constants.ORDER_TIME_PARA + " " + millisecondToSecond(maxTime));
            outputArr.add("\nTotal number of tests executed in all machines out of total in original order: " + numTests
                    + " / " + origOrderTestList.size());
        }
        writeToFile(outputArr, outputDTListSeparately);
        
        if (resolveDependences != null) {
            if (allDTList.isEmpty()) {
                FileWriter output = null;
                BufferedWriter writer = null;
                try {
                    output = new FileWriter(resolveDependences);
                    writer = new BufferedWriter(output);
                    writer.write("");
                    writer.close();
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (writer != null) {
                            writer.close();
                        }
                        if (output != null) {
                            output.close();
                        }
                    } catch (IOException e) {
                    }
                }
            } else {
                DependentTestFinder.printDependenceHelper(allDTList, resolveDependences);
            }
        }
    }

    private void writeToFile(List<String> outputArr, boolean outputDTListSeparately) {
        FileWriter output = null;
        BufferedWriter writer = null;
        try {
            String dirPath = outputDir.getCanonicalPath() + System.getProperty("file.separator"); // / or \
            String filePathName = dirPath + outputFileName;
            File outputFile = new File(filePathName);
            System.out.println("Writing results to " + filePathName);

            // Loop until outputFile is one that does not exist already
            /*int k = 1;
            while (outputFile.isFile()) {
                outputFile = new File(filePathName + k);
                k += 1;
            }*/

            output = new FileWriter(outputFile);
            writer = new BufferedWriter(output);
            for (String line : outputArr) {
                writer.write(line);
            }
            writer.close();
            output.close();

            if (outputDTListSeparately && allDTList != null && !allDTList.isEmpty()) {
                output = new FileWriter(dirPath + Constants.getDTListFileName(project.replace(" ", "_").toUpperCase(), testType));
                writer = new BufferedWriter(output);

                for (int j = 0; j < allDTList.size();) {
                    for (int i = 0; i < 5; j++) {
                        writer.write(allDTList.get(j) + "\n");
                        i++;
                    }
                    writer.write("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
            }
        }
    }

    protected Map<Double, List<Double>> setTestListMedianTime(int timesToRun, List<String> filesToDelete,
            List<String> currentOrderTestList, WrapperTestList testList, boolean printTestLists) {
        // Get time each test took
        Map<Double, List<Double>> totalTimeToCumulTime = new TreeMap<>();
        Map<Double, List<String>> totalTimeToTimeEachTest = new TreeMap<>();
        for (int j = 0; j < timesToRun; j++) {
            System.out.println("Getting median in iteration: " + (j + 1) + " / " + timesToRun);
            FileTools.clearEnv(filesToDelete);
            List<String> timeEachTest = new ArrayList<String>();
            TestRunResult result = runner.runListWithCp(classPath, currentOrderTestList).get();
            for (String test : result.results().keySet()) {
                timeEachTest.add(String.valueOf((long)(result.results().get(test).time() * 1000)));
            }
            List<Double> cumulTime = getCumulList(timeEachTest);
            double totalTimeNewOrder = getSumStr(timeEachTest);
            totalTimeToCumulTime.put(totalTimeNewOrder, cumulTime);
            totalTimeToTimeEachTest.put(totalTimeNewOrder, timeEachTest);
        }
        Double[] keys = totalTimeToCumulTime.keySet().toArray(new Double[totalTimeToCumulTime.keySet().size()]);
        double median = keys[keys.length / 2];
        if (printTestLists) {
            testList.setTimeEachTest(totalTimeToTimeEachTest.get(median).toString());
        }
        testList.setNewOrderTime(median);
        return totalTimeToCumulTime;
    }

    protected double getSumStr(List<String> list) {
        List<Double> doubleList = new ArrayList<>();
        for (String val : list) {
            doubleList.add(Double.valueOf(val));
        }
        return getSum(doubleList);
    }

    protected TestRunResult getCurrentOrderTestListResults(List<String> currentOrderTestList,
                                                           List<String> filesToDelete) {
        // ImpactMain
        FileTools.clearEnv(filesToDelete);
        TestRunResult result = runner.runListWithCp(classPath, currentOrderTestList).get();
        return result;
    }

    protected List<String> getCurrentTestList(Test testObj, int numOfMachines) {
        return getCurrentTestList(testObj, numOfMachines, true);
    }

    protected List<String> getCurrentTestList(Test testObj, int numOfMachines, boolean mergeDTCoverage) {
        // TestListGenerator
        List<String> currentOrderTestList = new LinkedList<String>();
        for (TestFunctionStatement tfs : testObj.getResults(numOfMachines)) {
            currentOrderTestList.add(tfs.getName());
        }
        return currentOrderTestList;
    }

    protected List<String> getCurrentCoverage(Test testObj, int numOfMachines) {
        List<String> currentOrderTestList = new LinkedList<String>();
        for (TestFunctionStatement tfs : testObj.getCoverage(numOfMachines)) {
            currentOrderTestList.add(tfs.getName());
        }
        return currentOrderTestList;
    }

    // Helper static methods

    public static String millisecondToSecond(double milliseconds) {
        double sec = milliseconds / 1E3;
        return String.format("%.3f", sec);
    }

    public static double getAPFD(List<Double> cumulTime, List<Double> cumulCoverage) {
        if (cumulTime.size() < 2 || cumulCoverage.size() < 1) {
            throw new IllegalArgumentException("cumulTime or cumulCoverage is too small to get APFD.\ncumulTime is: "
                    + cumulTime + "\ncumulCoverage is: " + cumulCoverage + ". Your configuration seems to have only selected one test.");
        }

        List<Double> testAPFD = new ArrayList<>();
        testAPFD.add((cumulTime.get(1) - cumulTime.get(0)) * cumulCoverage.get(0));
        for (int i = 2; i < cumulTime.size(); i++) {
            testAPFD.add((cumulTime.get(i) - cumulTime.get(i - 1)) * cumulCoverage.get(i - 1));
        }
        return getSum(testAPFD) / (cumulTime.get(cumulTime.size() - 1) * cumulCoverage.get(cumulCoverage.size() - 1));
    }

    protected static double getSum(List<Double> list) {
        double sum = 0.0;
        for (Double val : list) {
            sum += val;
        }
        return sum;
    }

    public static List<Double> getCumulList(List<String> list) {
        if (list == null || list.size() < 1) {
            throw new IllegalArgumentException("getCumulList received argument: " + list + ". Are you using test selection?"
            		+ " If so, are you sure the subjects you are selected have enough differences for tests to be selected?");
        }
        List<Double> cumulList = new ArrayList<>();
        cumulList.add(Double.valueOf(list.get(0)));
        for (int i = 1; i < list.size(); i++) {
            cumulList.add(Double.valueOf(list.get(i)) + cumulList.get(i - 1));
        }
        return cumulList;
    }

    public static List<Double> getCumulListDouble(List<Double> list) {
        if (list == null || list.size() < 1) {
            throw new IllegalArgumentException("getCumulList received argument: " + list);
        }
        List<Double> cumulList = new ArrayList<>();
        cumulList.add(list.get(0));
        for (int i = 1; i < list.size(); i++) {
            cumulList.add(list.get(i) + cumulList.get(i - 1));
        }
        return cumulList;
    }

    public String classPath() {
        return this.classPath;
    }

    public String outputFileName() {
        return this.outputFileName;
    }
}
