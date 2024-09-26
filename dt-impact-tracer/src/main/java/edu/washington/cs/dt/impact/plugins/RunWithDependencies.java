package edu.washington.cs.dt.impact.plugins;

import edu.illinois.cs.testrunner.data.framework.TestFramework;
import edu.illinois.cs.testrunner.mavenplugin.TestPluginPlugin;
import edu.illinois.cs.testrunner.testobjects.TestLocator;
import org.apache.commons.io.FileUtils;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import scala.collection.JavaConverters;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class RunWithDependencies extends Plugins {

    // Args (Re-Declared Upon Each Use)
    private String[] args = {};

    // MavenCli Variables
    private ClassRealm classRealm = (ClassRealm) Thread.currentThread().getContextClassLoader();
    private MavenCli cli = new MavenCli(classRealm.getWorld());

    private Mode RunMode = Mode.DEBUG;

    public void execute(MavenProject project) {
        // Project Setup
        setProject(project);
        setupNewVers(project);
        gatherDependencies(cli, newDTSubjectSource);
        gatherTests(project);

        // Setup Test Algorithms
        setupTestSelection();

        // Run With Dependencies
        try {
            new File(newDTResults + "/env-files").createNewFile();

            // TODO: -dependentTestFile is static, so it doesn't get reset between runs -> dependent tests alway re-ordered even without dependent test file
            new File(newDTResults + "/emptyDependentTestFile.txt").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String classpath = newDTLibs  + ":" + newDTTools + ":" + newDTTests + ":" + newDTClass;
        runTestPrioritization(classpath);
        runTestSelection(classpath);
        runTestParallelization(classpath);
    }

    // Gather Tests
    private void gatherTests(MavenProject project) {
        // Move Test Info From Old Version To The New Version
        TestPluginPlugin.info("Moving Test Info From The Old Version To The New Version");
        try {
            FileUtils.copyFile(new File(dtResults + "/orig-order.txt"), new File(newDTResults + "/orig-order.txt"));
            FileUtils.copyFile(new File(dtResults + "/ignore-order.txt"), new File(newDTResults + "/ignore-order.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Remove Failed Tests
        TestPluginPlugin.info("Removing Failed Tests");
        try {
            List<String> origOrder = Files.readAllLines(new File(newDTResults + "/orig-order.txt").toPath());
            List<String> ignoreOrder = Files.readAllLines(new File(newDTResults + "/ignore-order.txt").toPath());
            origOrder.removeAll(ignoreOrder);

            Files.write(new File(newDTResults + "/orig-order.txt").toPath(), origOrder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Remove Tests In The Old Version But Not The New Version
        TestPluginPlugin.info("Removing Missing Tests");
        humanWrittenTests(project);
        try {
            List<String> origOrder = Files.readAllLines(new File(newDTResults + "/orig-order.txt").toPath());
            List<String> newOrigOrder = Files.readAllLines(new File(newDTResults + "/new-orig-order.txt").toPath());

            List<String> missingTests = new ArrayList<>(origOrder);
            missingTests.removeAll(newOrigOrder);
            origOrder.removeAll(missingTests);

            Files.write(new File(newDTResults + "/orig-order.txt").toPath(), origOrder);
            FileUtils.deleteQuietly(new File(newDTResults + "/new-orig-order.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Adjust MACHINES (Less Tests Than Number Of Cores)
        MACHINES = adjustMachineCount(MACHINES, newDTResults + "/orig-order.txt");
    }

    private void humanWrittenTests(MavenProject project){
        TestPluginPlugin.info("Finding Human Written Tests (new version, original tests)");
        try {
            // Generates orig-order.txt
            final List<String> tests = JavaConverters.bufferAsJavaList(TestLocator.tests(project, TestFramework.junitTestFramework()).toBuffer());
            FileWriter writer = new FileWriter(new File(newDTResults + "/new-orig-order.txt"));
            for(String str: tests) {
                writer.write(str);
                writer.write(System.getProperty("line.separator"));
            }
            writer.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    // Setup Test Selection On The New Version
    private void setupTestSelection() {
        String JAVA_HOME = expandEnvVars("${JAVA_HOME}");

        // Instrument Source Files
        TestPluginPlugin.info("Test Selection: Instrumenting Files");
        try {
            // Runtime Exec
            String command = "java -cp " + newDTTools + ":" + buildClassPath(JAVA_HOME + "/jre/lib/*") + ":" +
                    " edu.washington.cs.dt.impact.Main.InstrumentationMain" +
                    " -inputDir " + newDTClass +
                    " --soot-cp " + newDTLibs + ":" + newDTClass + ":" + buildClassPath(JAVA_HOME + "/jre/lib/*") +
                    " -technique selection" +
                    " --java-version 1.8";
            Process p = Runtime.getRuntime().exec(command);
            printProcessMessages(p, RunMode, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Clean Up Files
        try {
            FileUtils.moveDirectory(new File(newDTSubjectSource + "/sootOutput"), new File(newDTResults + "/sootOutput"));
            FileUtils.moveDirectory(new File(newDTSubjectSource + "/selectionOutput"), new File(newDTResults + "/selectionOutput"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Run Test Prioritization With Dependencies
    private void runTestPrioritization(String classpath){
        // Run Technique With Dependencies
        TestPluginPlugin.info("Test Prioritization: Running With Dependencies");
        for (String k : TESTTYPES) {
            String postProcessFlag = "";

            TestPluginPlugin.info("Running Prioritization For: " + k + "Test Type");
            args = new String[]{
                    "-technique", "prioritization",
                    "-coverage", "statement",
                    "-order", "original",
                    "-origOrder", newDTResults + "/" + k + "-order.txt",
                    "-testInputDir", dtResults + "/sootTestOutput-" + k,
                    "-filesToDelete", newDTResults + "/env-files",
                    "-project", projectName,
                    "-testType", k,
                    "-outputDir", newPrioResults,
                    "-timeToRun", Integer.toString(MEDIANTIMES),
                    "-classpath", classpath,
                    "-getCoverage",
                    postProcessFlag};
            runOneConfigurationRunner(args, dtResults + "/output/priorization-" + k + "-run.log");

            for (String i : COVERGAES) {
                for (String j : PRIOORDERS) {
                    // TODO: -dependentTestFile is static, so it doesn't get reset between runs -> dependent tests always re-ordered even without dependent test file
                    TestPluginPlugin.info("Running Prioritization Without Dependent Test File");
                    String fileName = "prioritization-" + projectName + "-" + k + "-" + i + "-" + j;
                    args = new String[]{
                            "-technique", "prioritization",
                            "-coverage", i,
                            "-order", j,
                            "-origOrder", newDTResults + "/" + k + "-order.txt",
                            "-testInputDir", dtResults + "/sootTestOutput-" + k,
                            "-filesToDelete", newDTResults + "/env-files",
                            "-getCoverage",
                            "-project", projectName,
                            "-testType", k,
                            "-outputDir", newPrioResults,
                            "-timeToRun", Integer.toString(MEDIANTIMES),
                            "-classpath", classpath,
                            "-dependentTestFile", newDTResults + "/emptyDependentTestFile.txt",
                            postProcessFlag};
                    runOneConfigurationRunner(args, dtResults + "/output/" + fileName + "-woDependencies.log");

                    TestPluginPlugin.info("Running Prioritization With Dependent Test File");
                    args = new String[]{
                            "-technique", "prioritization",
                            "-coverage", i,
                            "-order", j,
                            "-origOrder", newDTResults + "/" + k + "-order.txt",
                            "-testInputDir", dtResults + "/sootTestOutput-" + k,
                            "-filesToDelete", newDTResults + "/env-files",
                            "-getCoverage",
                            "-project", projectName,
                            "-testType", k,
                            "-outputDir", newPrioResults,
                            "-timeToRun", Integer.toString(MEDIANTIMES),
                            "-classpath", classpath,
                            "-dependentTestFile", prioDTLists + "/" + fileName + ".txt",
                            postProcessFlag};

                    runOneConfigurationRunner(args, dtResults + "/output/" + fileName + "-wDependencies.log");
                }
            }
        }

        // Cleanup Files
        TestPluginPlugin.info("Cleaning Up Files");
        FileUtils.deleteQuietly(new File(newDTSubjectSource + "/tmpfile.txt"));
        FileUtils.deleteQuietly(new File(newDTSubjectSource + "/tmptestfiles.txt"));
    }

    // Run Test Selection With Dependencies
    private void runTestSelection(String classpath){
        // Run Technique With Dependencies
        TestPluginPlugin.info("Test Selection: Running With Dependencies");
        for (String k : TESTTYPES) {
            String postProcessFlag = "";

            TestPluginPlugin.info("Running Selection For: " + k + "Test Type");
            args = new String[]{
                    "-technique", "prioritization",
                    "-coverage", "statement",
                    "-order", "original",
                    "-origOrder", newDTResults + "/" + k + "-order.txt",
                    "-testInputDir", dtResults + "/sootTestOutput-" + k + "-selection",
                    "-filesToDelete", newDTResults + "/env-files",
                    "-project", projectName,
                    "-testType", k,
                    "-outputDir", newSeleResults,
                    "-timeToRun", Integer.toString(MEDIANTIMES),
                    "-classpath", classpath,
                    "-getCoverage",
                    postProcessFlag};
            runOneConfigurationRunner(args, dtResults + "/output/selection-priorization-" + k + "-run.log");

            for (String i : COVERGAES) {
                for (String j : SELEORDERS) {
                    // TODO: -dependentTestFile is static, so it doesn't get reset between runs -> dependent tests always re-ordered even without dependent test file
                    TestPluginPlugin.info("Running Selection Without Dependent Test File");
                    String fileName = "selection-" + projectName + "-" + k + "-" + i + "-" + j;
                    args = new String[]{
                            "-technique", "selection",
                            "-coverage", i,
                            "-order", j,
                            "-origOrder", newDTResults + "/" + k + "-order.txt",
                            "-testInputDir", dtResults + "/sootTestOutput-" + k + "-selection",
                            "-filesToDelete", newDTResults + "/env-files",
                            "-project", projectName,
                            "-testType", k,
                            "-oldVersCFG", dtResults + "/selectionOutput",
                            "-newVersCFG", newDTResults + "/selectionOutput",
                            "-getCoverage",
                            "-outputDir", newSeleResults,
                            "-timeToRun", Integer.toString(MEDIANTIMES),
                            "-classpath", classpath,
                            "-dependentTestFile", newDTResults + "/emptyDependentTestFile.txt",
                            postProcessFlag};
                    runOneConfigurationRunner(args, dtResults + "/output/" + fileName + "-woDependencies.log");

                    TestPluginPlugin.info("Running Selection With Dependent Test File");
                    args = new String[]{
                            "-technique", "selection",
                            "-coverage", i,
                            "-order", j,
                            "-origOrder", newDTResults + "/" + k + "-order.txt",
                            "-testInputDir", dtResults + "/sootTestOutput-" + k + "-selection",
                            "-filesToDelete", newDTResults + "/env-files",
                            "-project", projectName,
                            "-testType", k,
                            "-oldVersCFG", dtResults + "/selectionOutput",
                            "-newVersCFG", newDTResults + "/selectionOutput",
                            "-getCoverage",
                            "-outputDir", newSeleResults,
                            "-timeToRun", Integer.toString(MEDIANTIMES),
                            "-classpath", classpath,
                            "-dependentTestFile", seleDTLists + "/" + fileName + ".txt",
                            postProcessFlag};
                    runOneConfigurationRunner(args, dtResults + "/output/" + fileName + "-wDependencies.log");
                }
            }
        }
    }

    // Run Test Parallelization With Dependencies
    private void runTestParallelization(String classpath){
        // Run Technique With Dependencies
        TestPluginPlugin.info("Test Parallelization: Running With Dependencies");
        for (String j : TESTTYPES) {
            String postProcessFlag = "";

            TestPluginPlugin.info("Running Parallelization For: " + j + "Test Type");
            args = new String[]{
                    "-technique", "prioritization",
                    "-coverage", "statement",
                    "-order", "original",
                    "-origOrder", newDTResults + "/" + j + "-order.txt",
                    "-testInputDir", dtResults + "/sootTestOutput-" + j,
                    "-filesToDelete", newDTResults + "/env-files",
                    "-project", projectName,
                    "-testType", j,
                    "-outputDir", newParaResults,
                    "-timeToRun", Integer.toString(MEDIANTIMES),
                    "-classpath", classpath,
                    "-getCoverage",
                    postProcessFlag};
            runOneConfigurationRunner(args, dtResults + "/output/parallelization-priorization-" + j + "-run.log");

            String[] paraOrders = { "original", "time" };
            for (String k : MACHINES) {
                for (String order : paraOrders) {
                    String timeFlag = "";
                    if (order.equals("time")){
                        timeFlag = dtResults + "/" + j + "-time.txt";
                    }

                    // TODO: -dependentTestFile is static, so it doesn't get reset between runs -> dependent tests always re-ordered even without dependent test file
                    TestPluginPlugin.info("Running Parallelization Without Dependent Test File");
                    String timeFileName = "parallelization-" + "" + "-" + j + "-" + k + "-" + order;
                    args = new String[]{
                            "-technique", "parallelization",
                            "-order", order,
                            "-timeOrder", timeFlag,
                            "-origOrder", newDTResults + "/" + j + "-order.txt",
                            "-testInputDir", dtResults + "/sootTestOutput-" + j,
                            "-filesToDelete", newDTResults + "/env-files",
                            "-project", projectName,
                            "-testType", j,
                            "-numOfMachines", k,
                            "-outputDir", newParaResults,
                            "-timeToRun", Integer.toString(MEDIANTIMES),
                            "-classpath", classpath,
                            "-dependentTestFile", newDTResults + "/emptyDependentTestFile.txt",
                            postProcessFlag};
                    runOneConfigurationRunner(args, dtResults + "/output/" + timeFileName + "-woDependencies.log");

                    TestPluginPlugin.info("Running Parallelization With Dependent Test File");
                    args = new String[]{
                            "-technique", "parallelization",
                            "-order", order,
                            "-timeOrder", timeFlag,
                            "-origOrder", newDTResults + "/" + j + "-order.txt",
                            "-testInputDir", dtResults + "/sootTestOutput-" + j,
                            "-filesToDelete", newDTResults + "/env-files",
                            "-project", projectName,
                            "-testType", j,
                            "-numOfMachines", k,
                            "-outputDir", newParaResults,
                            "-timeToRun", Integer.toString(MEDIANTIMES),
                            "-classpath", classpath,
                            "-dependentTestFile", paraDTLists + "/" + timeFileName + ".txt",
                            postProcessFlag};
                    runOneConfigurationRunner(args, dtResults + "/output/" + timeFileName + "-wDependencies.log");
                }
            }
        }
    }
}