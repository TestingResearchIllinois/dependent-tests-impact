package edu.washington.cs.dt.impact.plugins;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.reedoei.testrunner.mavenplugin.TestPluginPlugin;
import com.reedoei.testrunner.testobjects.TestLocator;
import edu.washington.cs.dt.impact.tools.OutputPrecomputedDependences;
import edu.washington.cs.dt.impact.tools.detectors.FailingTestDetector;
import org.apache.commons.io.FileUtils;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import scala.collection.JavaConverters;

public class PrecomputeDependencies extends Plugins {
    private Mode RunMode = Mode.DEBUG;

    // Args (Re-Declared Upon Each Use)
    private String[] args = {};

    // MavenCli Variables
    private ClassRealm classRealm = (ClassRealm) Thread.currentThread().getContextClassLoader();
    private MavenCli cli = new MavenCli(classRealm.getWorld());

    private String subprocessOutput;

    public void execute(MavenProject project) {
        // Project Setup
        setProject(project);
        setupOldVers(project);
        gatherDependencies(cli, dtSubjectSource);
        gatherTests(project);

        // Setup Test Algorithms
        setupTestPrioritization();
        setupTestSelection();
        setupTestParallelization();

        // Precompute Dependencies
        try {
            new File(dtResults + "/env-files").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String classpath = dtLibs  + ":" + dtTools + ":" + dtTests + ":" + dtClass;
        runTestPrioritization(classpath);
        runTestParallelization(classpath);

        // Extracts dt-lists & Outputs Files To Necessary Locations
        TestPluginPlugin.info("Generating dt-lists");
        args = new String[]{
                "-prioDirectory", prioResults,
                "-paraDirectory", paraResults,
                "-prioOutputDirectory", prioDTLists,
                "-seleOutputDirectory", seleDTLists,
                "-paraOutputDirectory", paraDTLists};
        OutputPrecomputedDependences.main(args);
    }

    private void gatherTests(MavenProject project){
        humanWrittenTests(project);
        handleFailedTests(project);
    }

    private void humanWrittenTests(MavenProject project){
        TestPluginPlugin.info("Finding Human Written Tests (old version, original tests)");
        String origOrderFile = dtResults + "/orig-order.txt";

        if (!new File(origOrderFile).isFile()) {
            try {
                // Generates orig-order.txt
                final List<String> tests = JavaConverters.bufferAsJavaList(TestLocator.tests(project).toBuffer());
                FileWriter writer = new FileWriter(new File(origOrderFile));
                for(String str: tests) {
                    writer.write(str);
                    writer.write(System.getProperty("line.separator"));
                }
                writer.close();
            } catch(Exception e){
                e.printStackTrace();
            }
        } else {
            TestPluginPlugin.info(String.format("The relevant files already exist. " +
                                                        "Skipping finding human written tests.\n" +
                                                        "  %s\n", origOrderFile));
        }
    }

    private void handleFailedTests(MavenProject project){
        // Locate Failed Tests
        TestPluginPlugin.info("Locating Failed Tests");

        String origOrderFile = dtResults + "/orig-order.txt";
        String ignoreOrderFile = dtResults + "/ignore-order.txt";

        if (!new File(origOrderFile).isFile() || !new File(ignoreOrderFile).isFile()) {
            args = new String[]{
                    "--classpath", dtClass + ":" + dtTests + ":" + dtLibs,
                    "--tests", origOrderFile,
                    "--output", ignoreOrderFile};
            FailingTestDetector.outputFailedTests(project, origOrderFile, ignoreOrderFile);

            // Remove Failed Tests
            TestPluginPlugin.info("Removing Failed Tests");
            try {
                List<String> origOrder = Files.readAllLines(new File(origOrderFile).toPath());
                List<String> ignoreOrder = Files.readAllLines(new File(ignoreOrderFile).toPath());
                origOrder.removeAll(ignoreOrder);

                Files.write(new File(origOrderFile).toPath(), origOrder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            TestPluginPlugin.info(String.format("The relevant files already exist. " +
                                                        "Skipping locating and removing failed tests.\n" +
                                                        "  %s\n  %s\n", origOrderFile, ignoreOrderFile));
        }

        // Adjust MACHINES (Less Tests Than Number Of Cores)
        MACHINES = adjustMachineCount(MACHINES, origOrderFile);
    }

    // Setup A Subject For Test Prioritization
    private void setupTestPrioritization(){
        String JAVA_HOME = expandEnvVars("${JAVA_HOME}");

        // Instrument Test Files
        TestPluginPlugin.info("Test Prioritization: Instrumenting Test Files");
        subprocessOutput = null;
        try {
            // Runtime Exec
            String command = "java -cp " + dtTools + ":" + buildClassPath(JAVA_HOME + "/jre/lib/*") + ":" +
                    " edu.washington.cs.dt.impact.Main.InstrumentationMain" +
                    " -inputDir " + dtTests  +
                    " --soot-cp " + dtLibs + ":" + dtClass + ":" + dtTests + ":" + buildClassPath(JAVA_HOME + "/jre/lib/*");
            Process p = Runtime.getRuntime().exec(command);
            printProcessMessages(p, RunMode, subprocessOutput);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Instrument Source Files
        TestPluginPlugin.info("Test Prioritization: Instrumenting Source Files");
        subprocessOutput = null;
        try {
            // Runtime Exec
            String command = "java -cp " + dtTools + ":" + buildClassPath(JAVA_HOME + "/jre/lib/*") + ":" +
                    " edu.washington.cs.dt.impact.Main.InstrumentationMain" +
                    " -inputDir " + dtClass  +
                    " --soot-cp " + dtLibs + ":" + dtClass + ":" + buildClassPath(JAVA_HOME + "/jre/lib/*");
            Process p = Runtime.getRuntime().exec(command);
            printProcessMessages(p, RunMode, subprocessOutput);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: Copy Any Resource Files From dtClass & dtTests (e.g. Configuration Files - Exclude *.class Files)

        // Run Instrumented Files
        TestPluginPlugin.info("Test Prioritization: Running Instrumented Files");
        args = new String[]{
                "-classpath", dtLibs + ":" + dtTools + ":" + dtSubjectSource + "/sootOutput/",
                "-inputTests", dtResults + "/orig-order.txt"};
        runImpactMain(args, dtResults + "/output/prioritization-orig-order.log");

        // Clean Up Files
        TestPluginPlugin.info("Cleaning Up Files");
        try {
            FileUtils.moveDirectory(new File(dtSubjectSource + "/sootTestOutput"), new File(dtResults + "/sootTestOutput-orig"));
            FileUtils.deleteDirectory(new File(dtSubjectSource + "/sootOutput"));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // Setup A Subject For Test Selection
    private void setupTestSelection(){
        String JAVA_HOME = expandEnvVars("${JAVA_HOME}");

        // Instrument Test Files
        TestPluginPlugin.info("Test Selection: Instrumenting Test Files");
        subprocessOutput = null;
        try {
            // Runtime Exec
            String command = "java -cp " + dtTools + ":" + buildClassPath(JAVA_HOME + "/jre/lib/*") + ":" +
                    " edu.washington.cs.dt.impact.Main.InstrumentationMain" +
                    " -inputDir " + dtTests  +
                    " --soot-cp " + dtLibs + ":" + dtClass + ":" + dtTests + ":" + buildClassPath(JAVA_HOME + "/jre/lib/*") +
                    " -technique selection";
            Process p = Runtime.getRuntime().exec(command);
            printProcessMessages(p, RunMode, subprocessOutput);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Instrument Source Files
        TestPluginPlugin.info("Test Selection: Instrumenting Source Files");
        subprocessOutput = null;
        try {
            // Runtime Exec
            String command = "java -cp " + dtTools + ":" + buildClassPath(JAVA_HOME + "/jre/lib/*") + ":" +
                    " edu.washington.cs.dt.impact.Main.InstrumentationMain" +
                    " -inputDir " + dtClass  +
                    " --soot-cp " + dtLibs + ":" + dtClass + ":" + buildClassPath(JAVA_HOME + "/jre/lib/*") +
                    " -technique selection";
            Process p = Runtime.getRuntime().exec(command);
            printProcessMessages(p, RunMode, subprocessOutput);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: Copy Any Resource Files From dtClass & dtTests (e.g. Configuration Files - Exclude *.class Files)

        // Run Instrumented Tests
        TestPluginPlugin.info("Test Selection: Running Instrumented Tests");
        args = new String[]{
                "-classpath", dtLibs + ":" + dtTools + ":" + dtSubjectSource + "/sootOutput/",
                "-inputTests", dtResults + "/orig-order.txt"};
        runImpactMain(args, dtResults + "/output/selection-orig-order.log");

        // Clean Up Files
        TestPluginPlugin.info("Cleaning Up Files");
        try {
            FileUtils.moveDirectory(new File(dtSubjectSource + "/sootTestOutput"), new File(dtResults + "/sootTestOutput-orig-selection"));
            FileUtils.deleteDirectory(new File(dtSubjectSource + "/sootOutput"));
        } catch (Exception e){
            e.printStackTrace();
        }

        try {
            FileUtils.moveDirectory(new File(dtSubjectSource + "/selectionOutput"), new File(dtResults + "/selectionOutput"));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    // Setup A Subject For Test Selection
    private void setupTestParallelization(){

        String origTimeFile = dtResults + "/orig-time.txt";

        if (!new File(origTimeFile).isFile()) {
            // Calculate Runtimes
            TestPluginPlugin.info("Test Parallelization: Calculating Runtimes For Tests");
            args = new String[]{
                    "-classpath", dtLibs + ":" + dtTools + ":" + dtClass + ":" + dtTests,
                    "-inputTests", dtResults + "/orig-order.txt",
                    "-getTime"};

            runImpactMain(args, "orig-time.txt");

            // Clean Up Files
            TestPluginPlugin.info("Cleaning Up Files");
            try {
                FileUtils.moveFile(new File(dtSubjectSource + "/orig-time.txt"), new File(origTimeFile));
            } catch (Exception e){
                e.printStackTrace();
            }
        } else {
            TestPluginPlugin.info(String.format("The relevant files already exist. " +
                                                        "Skipping getting time of tests.\n" +
                                                        "  %s\n", origTimeFile));
        }
    }

    // Run Test Prioritization
    private void runTestPrioritization(String classpath){
        // Generate Precomputed Dependences
        TestPluginPlugin.info("Test Prioritization: Generating Precomputed Dependencies");
        for (String k : TESTTYPES) {
            for (String i : COVERGAES) {
                for (String j : PRIOORDERS) {
                    String fileName = "prioritization-" + projectName + "-" + k + "-" + i + "-" + j + ".txt";
                    String precomputeFlag = prioDTLists + "/" + fileName;
                    String postProcessFlag = "";

                    args = new String[]{
                            "-technique", "prioritization",
                            "-coverage", i,
                            "-order", j,
                            "-origOrder", dtResults + "/" + k + "-order.txt",
                            "-testInputDir", dtResults + "/sootTestOutput-" + k,
                            "-filesToDelete", dtResults + "/env-files",
                            "-getCoverage",
                            "-project", projectName,
                            "-testType", k,
                            "-outputDir", prioResults,
                            "-timeToRun", Integer.toString(MEDIANTIMES),
                            "-classpath", classpath,
                            "-resolveDependences", precomputeFlag,
                            postProcessFlag};
                    runOneConfigurationRunner(args, dtResults + "/output/" + fileName);
                }
            }
        }

        // Cleanup Files
        TestPluginPlugin.info("Cleaning Up Files");
        FileUtils.deleteQuietly(new File(dtSubjectSource + "/tmpfile.txt"));
        FileUtils.deleteQuietly(new File(dtSubjectSource + "/tmptestfiles.txt"));
    }

    // Run Test Parallelization
    private void runTestParallelization(String classpath){
        // Run Precomputed Dependencies
        TestPluginPlugin.info("Test Parallelization: Generating Precomputed Dependencies");
        for (String j : TESTTYPES) {
            for (String k : MACHINES) {
                String precomputeFlag;
                String postProcessFlag;

                // Original Order
                TestPluginPlugin.info("Original Order");
                String origFileName = "parallelization-" + projectName + "-" + j + "-" + k + "-original.txt";
                precomputeFlag =  prioDTLists + "/" + origFileName;
                postProcessFlag = "";
                args = new String[]{
                        "-technique", "parallelization",
                        "-order", "original",
                        "-origOrder", dtResults + "/" + j + "-order.txt",
                        "-testInputDir", dtResults + "/sootTestOutput-" + j,
                        "-filesToDelete", dtResults + "/env-files",
                        "-project", projectName,
                        "-testType", j,
                        "-numOfMachines", k,
                        "-outputDir", paraResults,
                        "-timesToRun", Integer.toString(MEDIANTIMES),
                        "-classpath", classpath,
                        "-resolveDependences", precomputeFlag,
                        postProcessFlag};
                runOneConfigurationRunner(args, dtResults + "/output/" + origFileName);

                // Time Order
                TestPluginPlugin.info("Time Order");
                String timeFileName = "parallelization-" + projectName + "-" + j + "-" + k + "-time.txt";
                precomputeFlag =  prioDTLists + "/" + timeFileName;
                postProcessFlag = "";
                args = new String[]{
                        "-technique", "parallelization",
                        "-order", "time",
                        "-timeOrder", dtResults + "/" + j + "-time.txt",
                        "-origOrder", dtResults + "/" + j + "-order.txt",
                        "-testInputDir", dtResults + "/sootTestOutput-" + j,
                        "-filesToDelete", dtResults + "/env-files",
                        "-project", projectName,
                        "-testType", j,
                        "-numOfMachines", k,
                        "-outputDir", paraResults,
                        "-timesToRun", Integer.toString(MEDIANTIMES),
                        "-classpath", classpath,
                        "-resolveDependences", precomputeFlag,
                        postProcessFlag};
                runOneConfigurationRunner(args, dtResults + "/output/" + timeFileName);
            }
        }

        // Cleanup Files
        TestPluginPlugin.info("Cleaning Up Files");
        FileUtils.deleteQuietly(new File("tmpfile.txt"));
        FileUtils.deleteQuietly(new File("tmptestfiles.txt"));
    }
}
