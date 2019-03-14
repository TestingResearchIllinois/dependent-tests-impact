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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestRunResult;

import edu.washington.cs.dt.RESULT;
import edu.washington.cs.dt.TestExecResult;
import edu.washington.cs.dt.TestExecResults;
import edu.washington.cs.dt.impact.data.WrapperTestList;
import edu.washington.cs.dt.impact.technique.Parallelization;
import edu.washington.cs.dt.impact.technique.Prioritization;
import edu.washington.cs.dt.impact.technique.Selection;
import edu.washington.cs.dt.impact.technique.Test;
import edu.washington.cs.dt.impact.tools.CrossReferencer;
import edu.washington.cs.dt.impact.tools.DependentTestFinder;
import edu.washington.cs.dt.impact.util.Constants.TECHNIQUE;
import edu.washington.cs.dt.runners.FixedOrderRunner;

public class OneConfigurationRunner extends Runner {

    public void execute() {
        TestRunResult origResults = getCurrentOrderTestListResults(origOrderTestList, filesToDelete);
        Map<String, Result> nameToOrigResults = new HashMap<>();
        for (String test : origResults.results().keySet()) {
            nameToOrigResults.put(test, origResults.results().get(test).result());
        }

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
            TestRunResult result =  getCurrentOrderTestListResults(currentOrderTestList, filesToDelete);
            Map<String, Result> nameToTestResults = new HashMap<>();
            for (String test : result.results().keySet()) {
                nameToTestResults.put(test, result.results().get(test).result());
            }

            testList.setOrigOrderResults(origResults);
            testList.setTestOrderResults(nameToTestResults);

            // CrossReferencer
            Set<String> changedTests = CrossReferencer.compareResults(nameToOrigResults, nameToTestResults, false);

            if (!changedTests.isEmpty()) {
                hasDependentTest = true;
            }

            // Run the dependent tests in isolation to get the time/stuff for those
            for (final String changedTest : changedTests) {
                testList.isolationResult(changedTest, classPath);
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
                while (!dtToFix.isEmpty()) {
                    String testName = dtToFix.iterator().next();

                    System.out.println("Nullifying DTs iteration number / possible iterations left: " + counter + " / "
                            + dtToFix.size());
                    counter += 1;

                    fixedDT.add(testName);

                    // DependentTestFinder
                    final long startDepFind = System.nanoTime();
                    DependentTestFinder.runDTF(testName, nameToOrigResults.get(testName), currentOrderTestList,
                            origOrderTestList, filesToDelete, allDTList, classPath);
                    final long endDepFind = System.nanoTime();

                    totalDepTime += endDepFind - startDepFind;

                    allDTList = DependentTestFinder.getAllDTs();
                    // TestListGenerator
                    testObj.resetDTList(allDTList);
                    currentOrderTestList = getCurrentTestList(testObj, i);
                    // ImpactMain
                    nameToTestResults = new HashMap<>();
                    result = getCurrentOrderTestListResults(currentOrderTestList, filesToDelete);
                    for (String test : result.results().keySet()) {
                        nameToTestResults.put(test, result.results().get(test).result());
                    }
                    // Cross Referencer
                    changedTests = CrossReferencer.compareResults(nameToOrigResults, nameToTestResults, false);

                    dtToFix.clear();
                    for (String test : changedTests) {
                        if (currentOrderTestList.contains(test)) {
                            dtToFix.add(test);
                        }
                    }
                }
            }

            // capture end time
            double runTotal = System.nanoTime() - start;
            testList.setNullifyDTTime(runTotal);
            testList.setNumNotFixedDT(changedTests);
            testList.setNumFixedDT(fixedDT.size());
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
        runner.execute();
    }
}
