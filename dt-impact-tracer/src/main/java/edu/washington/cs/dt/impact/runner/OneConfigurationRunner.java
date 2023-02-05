/**
 * Copyright 2015 University of Washington. All Rights Reserved.
 * @author Wing Lam
 *
 *         Main class that relies on program arguments to generate a regression testing
 *         execution order. The following options are supported:
 *         -technique - prioritization, selection, parallelization
 *         -coverage - statement, function
 *         -order - absolute, relative, random, original, time (if technique is parallelization)
 *         -resolveDependences - when specified the output test order will not be affected
 *         by dependent tests
 *         -numMachines - integer value (only valid when technique is parallelization
 *         -selectionOldVers - path to directory to older version of program's selectionOutput
 *         (only valid when technique is selection)
 *         -selectionNewVers - path to directory to newer version of program's selectionOutput
 *         (only valid when technique is selection)
 *         -origOrder - path to file containing the original order the tests are executed in
 *         -testInputDir - path to directory to sootTestOutput
 *         -dependentTestFile - path to file containing existing known dependent tests
 *         -filesToDelete - path to file containing list of files to delete to clean the environment
 *         each time the tests are executed
 *         -outputFile - path to file to output the regression test order, dependent test list and
 *         execution time, if unspecified the output will be sent to System.out
 *         -help - display this message
 */

package edu.washington.cs.dt.impact.runner;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.illinois.cs.testrunner.configuration.Configuration;
import edu.illinois.cs.testrunner.data.framework.TestFramework;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.runner.SmartRunner;
import edu.illinois.cs.testrunner.runner.TestInfoStore;

import edu.washington.cs.dt.TestExecResult;
import edu.washington.cs.dt.TestExecResults;
import edu.washington.cs.dt.impact.data.WrapperTestList;
import edu.washington.cs.dt.impact.technique.Parallelization;
import edu.washington.cs.dt.impact.technique.Prioritization;
import edu.washington.cs.dt.impact.technique.Selection;
import edu.washington.cs.dt.impact.technique.Test;
import edu.washington.cs.dt.impact.tools.CrossReferencer;
import edu.washington.cs.dt.impact.tools.minimizer.FlakyClass;
import edu.washington.cs.dt.impact.tools.minimizer.MinimizeTestsResult;
import edu.washington.cs.dt.impact.tools.minimizer.PolluterData;
import edu.washington.cs.dt.impact.tools.minimizer.TestMinimizer;
import edu.washington.cs.dt.impact.util.Constants;
import edu.washington.cs.dt.impact.util.Constants.TECHNIQUE;

public class OneConfigurationRunner extends Runner {

    public Set<String> checkForNODs(TestRunResult origResult, List<String> currentOrderTestList, int timesToCheck) {
        Set<String> nods = new HashSet<>();

        Map<String, Result> nameToOrigResult = getNameToResult(origResult);
        for (int i = 0; i < timesToCheck; i++) {
            TestRunResult newResult = getCurrentOrderTestListResults(currentOrderTestList, filesToDelete);
            nods.addAll(CrossReferencer.compareResults(nameToOrigResult, getNameToResult(newResult), false));
        }

        return nods;
    }

    private Map<String, Result> getNameToResult(TestRunResult result) {
        Map<String, Result> nameToOrigResults = new HashMap<>();
        for (String test : result.results().keySet()) {
            nameToOrigResults.put(test, result.results().get(test).result());
        }
        return nameToOrigResults;
    }

    public void execute() {
        TestRunResult origResults = getCurrentOrderTestListResults(origOrderTestList, filesToDelete);
        Map<String, Result> nameToOrigResults = getNameToResult(origResults);

        // capture start time
        double start = System.nanoTime();

        boolean hasDependentTest = false;

        // TestListGenerator
        Test testObj = null;
        if (techniqueName == TECHNIQUE.PRIORITIZATION) {
            testObj = new Prioritization(order, outputFileName, testInputDir, coverage, dependentTestFile, false,
                    origOrder, !postProcessDTs);
        } else if (techniqueName == TECHNIQUE.SELECTION) {
            testObj = new Selection(order, outputFileName, testInputDir, coverage, selectionOutput1, selectionOutput2,
                    origOrder, dependentTestFile, getCoverage, !postProcessDTs);
        } else if (techniqueName == TECHNIQUE.PARALLELIZATION) {
            testObj = new Parallelization(order, outputFileName, testInputDir, coverage, dependentTestFile,
                    numOfMachines.getValue(), origOrder, timeOrder, getCoverage, origOrderTestList, !postProcessDTs);
        } else {
            System.err.println("The regression testing technique '" + techniqueName + "' is invalid. Please restart the"
                    + " program and try again.");
            System.exit(0);
        }

        TLGTime = System.nanoTime() - start;

        listTestList = new ArrayList<>();
        for (int i = 0; i < numOfMachines.getValue(); i++) {
            start = System.nanoTime();

            WrapperTestList testList = new WrapperTestList(runner);
            List<String> currentOrderTestList = getCurrentTestList(testObj, i);

            // ImpactMain
            TestRunResult result = getCurrentOrderTestListResults(currentOrderTestList, filesToDelete);
            Map<String, Result> nameToTestResults = getNameToResult(result);

            testList.setOrigOrderResults(origResults);
            testList.setTestOrderResults(nameToTestResults);

            // CrossReferencer
            Set<String> changedTests = CrossReferencer.compareResults(nameToOrigResults, nameToTestResults, false);

            // Compare current test order results with running that same test order multiple times, to see if any difference in result
            Set<String> NODs = new HashSet<>();
            NODs.addAll(checkForNODs(result, currentOrderTestList, timesToRunCheckTestOrderNOD));
            changedTests.removeAll(NODs);   // Remove from consideration any tests that are NOD

            // Run the dependent tests in isolation to get the time/stuff for those
            for (final String changedTest : changedTests) {
                testList.isolationResult(changedTest, classPath);
                Result isoResult = testList.getIsolationResults().get(changedTest);
                // Run a few more times to ensure that the isolation result is consistent, otherwise it is NOD
                for (int j = 0; j < timesToRunCheckTestOrderNOD; j++) {
                    testList.isolationResult(changedTest, classPath);
                    if (testList.getIsolationResults().get(changedTest) != isoResult) {
                        NODs.add(changedTest);
                        break;
                    }
                }
            }
            changedTests.removeAll(NODs);   // Remove any additional NODs found from isolation reruns

            // Only check for dependent tests once removing changed tests from consideration
            if (!changedTests.isEmpty()) {
                hasDependentTest = true;
            }

            int counter = 0;

            double totalDepTime = 0;
            Set<String> fixedDT = new HashSet<>();
            if (resolveDependences != null) {
                Set<String> dtToFix = new HashSet<String>();
                for (String test : changedTests) {
                    if (currentOrderTestList.contains(test)) {
                        dtToFix.add(test);
                    }
                }

                // TODO there may be a bug in the generation of parallelization orders where tests that needs to come
                // before a dependent test from the ALL_DT_LIST is not actually putting all tests before the dependent
                // test. Print botTests in determineSubLists to know for sure if all tests in there are in the
                // ALL_DT_LIST and is in the current test order.
                Set<String> notFixable = new HashSet<String>(); // Keep track of those that somehow cannot be addressed by minimizer
                while (!dtToFix.isEmpty()) {
                    String testName = dtToFix.iterator().next();

                    System.out.println("Nullifying DTs iteration number / possible iterations left: " + counter + " / "
                            + dtToFix.size());
                    counter += 1;

                    // Minimizer
                    final long startDepFind = System.nanoTime();

                    // If we don't have isolation result from before, that means this is new exposed one, so we need to rerun and check for NOD
                    if (testList.getIsolationResults().get(testName) == null) {
                        testList.isolationResult(testName, classPath);
                        Result isolationResult = testList.getIsolationResults().get(testName);
                        boolean foundNOD = false;
                        for (int j = 0; j < timesToRunCheckTestOrderNOD; j++) {
                            testList.isolationResult(testName, classPath);
                            if (testList.getIsolationResults().get(testName) != isolationResult) {
                                NODs.add(testName);
                                foundNOD = true;
                                break;
                            }
                        }
                        // Found it as NOD, so skip
                        if (foundNOD) {
                            continue;
                        }
                    }

                    // If isolated result of test is PASS, then is victim, so minimize on failing order and put after
                    List<String> orderTestList;
                    Result isolationResult = testList.getIsolationResults().get(testName);
                    if (isolationResult == Result.PASS) {
                        orderTestList = currentOrderTestList;
                    } else {
                        orderTestList = origOrderTestList;
                    }
                    TestMinimizer minimizer = new TestMinimizer(orderTestList, runner, testName);
                    MinimizeTestsResult minimizerResult = minimizer.run();
                    final long endDepFind = System.nanoTime();

                    totalDepTime += endDepFind - startDepFind;

                    // If the result is NOD, then skip this dependent test, but record in list of NODs
                    if (minimizerResult.flakyClass() == FlakyClass.NOD) {
                        dtToFix.remove(testName);
                        NODs.add(testName);
                        continue;
                    }

                    if (allDTList == null) {
                        allDTList = new ArrayList<>();
                    }
                    // Get the results of the TestMinimizer, force it into format
                    for (PolluterData pd : minimizerResult.polluters()) {
                        // Encode dependencies as positive (P->) or negative (N->) dependencies
                        String delim;
                        if (isolationResult == Result.PASS) {   // If isolation result is PASS, then is victim, so finding negative dependencies
                            delim = " N-> ";
                        } else {
                            delim = " P-> ";
                        }
                        allDTList.add(pd.deps().get(0).toString() + delim + testName);
                    }

                    // Make new test object, but make sure to use new instances that use in memory data objects (not files)
                    if (techniqueName == TECHNIQUE.PRIORITIZATION) {
                        testObj = new Prioritization(order, outputFileName, testInputDir, coverage, allDTList, false,
                                origOrderTestList, 0, false, !postProcessDTs);
                    } else if (techniqueName == TECHNIQUE.SELECTION) {
                        testObj = new Selection(order, outputFileName, testInputDir, coverage, selectionOutput1, selectionOutput2,
                                origOrderTestList, allDTList, getCoverage, !postProcessDTs);
                    } else {
                        testObj = new Parallelization(order, outputFileName, testInputDir, coverage, allDTList,
                                numOfMachines.getValue(), origOrderTestList, timeOrder, getCoverage, origOrderTestList, !postProcessDTs);
                    }
                    currentOrderTestList = getCurrentTestList(testObj, i);
                    // ImpactMain
                    result = getCurrentOrderTestListResults(currentOrderTestList, filesToDelete);
                    nameToTestResults = getNameToResult(result);
                    // Cross Referencer
                    changedTests = CrossReferencer.compareResults(nameToOrigResults, nameToTestResults, false);

                    // If the test is still not fixed, then include it in the not fixed ones
                    if (changedTests.contains(testName)) {
                        notFixable.add(testName);
                    } else {
                        // The test has been fixed
                        fixedDT.add(testName);
                    }

                    Set<String> oldDtToFix = new HashSet<>(dtToFix);    // Keep track of what was before, if entirely the same, then we could be looping
                    dtToFix.clear();
                    for (String test : changedTests) {
                        if (currentOrderTestList.contains(test)) {
                            dtToFix.add(test);
                        }
                    }
                    // Remove any tests that are not fixable
                    dtToFix.removeAll(notFixable);
                    if (dtToFix.equals(oldDtToFix)) {
                        System.out.println("DID NOT REMOVE ANY OLD DTs, LIKELY CANNOT REDUCE ANY MORE");
                        break;
                    }
                }
            }

            // capture end time
            double runTotal = System.nanoTime() - start;
            testList.setNullifyDTTime(runTotal);
            testList.setNumNotFixedDT(changedTests);
            testList.setNumFixedDT(fixedDT.size());
            testList.setNODs(NODs);
            testList.setTestList(currentOrderTestList);
            testList.setTestListSize(currentOrderTestList.size());

            if (counter != 0) {
                testList.setAvgDepFindTime(totalDepTime / counter / 1E9); // Convert to seconds
                System.out.println("Average time was: " + testList.getAvgDepFindTime());
            }

            Map<Double, List<Double>> totalTimeToCumulTime =
                    setTestListMedianTime(timesToRun, filesToDelete, currentOrderTestList, testList, true);
            if (allDTList != null) {
                testList.setDtList(allDTList.toString());
            }

            if (getCoverage) {
                // Get coverage each test achieved
                List<String> coverageEachTest = getCurrentCoverage(testObj, i);
                testList.setCoverage(coverageEachTest);
                List<Double> cumulCoverage = getCumulList(coverageEachTest);

                try {
                    testList.setAPFD(getAPFD(totalTimeToCumulTime.get(testList.getNewOrderTime()), cumulCoverage));
                } catch (IllegalArgumentException e) {
                    // This will occur when there are too few tests selected.
                    // Currently, the APFD is not used, so this is not important. If it is, this should be
                    // handled (TODO) properly in the results methods.
                    testList.setAPFD(-1.0);
                }
            }
            listTestList.add(testList);
        }

        if (allDTList == null && !hasDependentTest) {
            System.out.println("No dependent tests found and allDTList is null, setting to empty.");
            allDTList = new ArrayList<>();
        }

        output(false);
    }

    public static void main(String[] args) {
        OneConfigurationRunner runner = new OneConfigurationRunner();
        runner.parseArgs(args);
        runner.setRunner(new SmartRunner(TestFramework.junitTestFramework(), new TestInfoStore(), runner.classPath(), new HashMap<String, String>(), Paths.get(runner.outputFileName())));
        Configuration.config().setDefault("testplugin.classpath", "");
        runner.execute();
    }
}
