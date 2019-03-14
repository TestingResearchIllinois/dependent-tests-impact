/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam
 *
 *         Determines and outputs what tests needs to run before and
 *         after a particular test in order for that particular test to
 *         attain the same specified result in a new test execution order.
 */
package edu.washington.cs.dt.impact.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.reedoei.testrunner.data.results.Result;

import edu.washington.cs.dt.RESULT;
import edu.washington.cs.dt.TestExecResult;
import edu.washington.cs.dt.TestExecResults;
import edu.washington.cs.dt.impact.util.Constants;
import edu.washington.cs.dt.runners.AbstractTestRunner;
import edu.washington.cs.dt.runners.FixedOrderRunner;

public class DependentTestFinder {

    private static File DT_FILE;
    private static List<String> FILES_TO_DELETE;
    private static List<String> ORIGINAL_ORDER_TESTS;
    private static List<String> CURRENT_ORDER_TESTS;
    private static Result DEPENDENT_TEST_RESULT;
    private static List<String> ALL_DT_LIST;
    private static String CLASSPATH;

    public static void main(String[] args) {
        List<String> argsList = new ArrayList<String>(Arrays.asList(args));

        String dependentTestName = null;
        int dependentTestIndex = argsList.indexOf("-dependentTestName");
        if (dependentTestIndex != -1) {
            // get index of output file
            int dependentTestNameIndex = dependentTestIndex + 1;
            if (dependentTestNameIndex >= argsList.size()) {
                System.err.println("Dependent test name argument is specified but a test name is not."
                        + " Please use the format: -dependentTestName aTestName");
                System.exit(0);
            }
            dependentTestName = argsList.get(dependentTestNameIndex);
        } else {
            System.err.println("No dependent test name argument is specified."
                    + " Please use the format: -dependentTestName aTestName");
            System.exit(0);
        }

        Result dependentTestResult = null;
        int dependentTestResultIndex = argsList.indexOf("-dependentTestResult");
        if (dependentTestResultIndex != -1) {
            // get index of output file
            int dependentTestNameIndex = dependentTestResultIndex + 1;
            if (dependentTestNameIndex >= argsList.size()) {
                System.err.println("Dependent test result argument is specified but a test name is not."
                        + " Please use the format: -dependentTestResult aTestResult");
                System.exit(0);
            }
            String dependentTestResultString = argsList.get(dependentTestNameIndex);
            dependentTestResultString = dependentTestResultString.toLowerCase();

            if (dependentTestResultString.equals("pass")) {
                dependentTestResult = Result.PASS;
            } else if (dependentTestResultString.equals("error")) {
                dependentTestResult = Result.ERROR;
            } else if (dependentTestResultString.equals("fail")) {
                dependentTestResult = Result.FAILURE;
            } else {
                System.err.println(
                        "No such test result argument exist." + " Possible options are: pass, error and fail.");
                System.exit(0);
            }
        } else {
            System.err.println("No dependent test result argument is specified."
                    + " Please use the format: -dependentTestResult aTestResult");
            System.exit(0);
        }

        File currentOrderFile = null;
        int currentOrderIndex = argsList.indexOf("-currentOrderFile");
        if (currentOrderIndex != -1) {
            // get index of output file
            int currentOrderFileIndex = currentOrderIndex + 1;
            if (currentOrderFileIndex >= argsList.size()) {
                System.err.println("Current order file argument is specified but a file name is not."
                        + " Please use the format: -currentOrderFile aFileName");
                System.exit(0);
            }
            currentOrderFile = new File(argsList.get(currentOrderFileIndex));
        } else {
            System.err.println("No current order file argument is specified."
                    + " Please use the format: -currentOrderFile aFileName");
            System.exit(0);
        }

        File originalOrderFile = null;
        int originalOrderIndex = argsList.indexOf("-originalOrderFile");
        if (originalOrderIndex != -1) {
            // get index of output file
            int originalOrderFileIndex = originalOrderIndex + 1;
            if (originalOrderFileIndex >= argsList.size()) {
                System.err.println("Original order file argument is specified but a file name is not."
                        + " Please use the format: -originalOrderFile aFileName");
                System.exit(0);
            }
            originalOrderFile = new File(argsList.get(originalOrderFileIndex));
        } else {
            System.err.println("No original order file argument is specified."
                    + " Please use the format: -originalOrderFile aFileName");
            System.exit(0);
        }

        File filesToDelete = null;
        int filesToDeleteIndex = argsList.indexOf("-filesToDelete");
        if (filesToDeleteIndex != -1) {
            // get index of output file
            int filesToDeleteFileIndex = filesToDeleteIndex + 1;
            if (filesToDeleteFileIndex >= argsList.size()) {
                System.err.println("Files to delete argument is specified but a file name is not."
                        + " Please use the format: -filesToDelete aFileName");
                System.exit(0);
            }
            filesToDelete = new File(argsList.get(filesToDeleteFileIndex));
        } else {
            System.err.println(
                    "No files to delete argument is specified." + " Please use the format: -filesToDelete aFileName");
            System.exit(0);
        }

        int dtListIndex = argsList.indexOf("-dtFile");
        File dtFile = null;
        if (dtListIndex != -1) {
            // get index of output file
            int dtListFileIndex = dtListIndex + 1;
            if (dtListFileIndex >= argsList.size()) {
                System.err.println("DT file argument is specified but a file name is not."
                        + " Please use the format: -dtFile aFileName");
                System.exit(0);
            }
            dtFile = new File(argsList.get(dtListFileIndex));
        } else {
            System.err.println("No DT file argument is specified." + " Please use the format: -dtFile aFileName");
            System.exit(0);
        }

        DT_FILE = dtFile;
        runDTF(dependentTestName, dependentTestResult, FileTools.parseFileToList(currentOrderFile),
                FileTools.parseFileToList(originalOrderFile), FileTools.parseFileToList(filesToDelete),
                FileTools.parseFileToList(DT_FILE), System.getProperty("java.class.path"));
    }

    public static void runDTF(String dependentTestName, Result dependentTestResult, List<String> currentOrderTests,
            List<String> originalOrderTests, List<String> filesToDelete, List<String> allDTList,
                              String CLASSPATH) {
        DependentTestFinder.CLASSPATH = CLASSPATH;
        DEPENDENT_TEST_RESULT = dependentTestResult;
        CURRENT_ORDER_TESTS = currentOrderTests;
        ORIGINAL_ORDER_TESTS = originalOrderTests;
        FILES_TO_DELETE = filesToDelete;

        if (allDTList == null) {
            ALL_DT_LIST = new ArrayList<String>();
        } else {
            ALL_DT_LIST = allDTList;
        }
        boolean isOriginalOrder = isTestResultDifferent(dependentTestName, Arrays.asList(dependentTestName));
        List<String> fullTests;
        if (isOriginalOrder) {
            fullTests = ORIGINAL_ORDER_TESTS;
        } else {
            fullTests = CURRENT_ORDER_TESTS;
        }

        List<String> tests = new LinkedList<String>(fullTests);
        if (tests == null || !tests.contains(dependentTestName)) {
            throw new RuntimeException("Dependent test missing from test order.");
        }

        List<String> beforeTests = new ArrayList<String>();
        List<String> afterTests = new ArrayList<String>();
        int testNameIndex = ALL_DT_LIST.indexOf(Constants.TEST_LINE + dependentTestName);
        if (testNameIndex != -1) {
            isOriginalOrder = determineSubLists(fullTests, testNameIndex, dependentTestName, isOriginalOrder, tests,
                    beforeTests, afterTests);
        } else {
            tests = fullTests.subList(0, fullTests.indexOf(dependentTestName));
            dependentTestSolver(tests, dependentTestName, isOriginalOrder, beforeTests, afterTests,
                    new ArrayList<String>(), new ArrayList<String>(), false);
        }

        checkOrder(isOriginalOrder, dependentTestName, beforeTests, afterTests);
    }

    public static List<String> getAllDTs() {
        return ALL_DT_LIST;
    }

    // determines a sublist to look for the test(s) dependentTest depends on
    // returns true if the sublist is part of ORIGINAL_ORDER_TESTS,
    // false if the sublist is part of CURRENT_ORDER_TESTS
    private static boolean determineSubLists(List<String> fullTests, int testNameIndex, String dependentTestName,
            boolean isOriginalOrder, List<String> tests, List<String> beforeTests, List<String> afterTests) {
        // go up dependentTest's chain and use either a portion of the original order tests
        // or a portion of current order tests to get dependent test to pass
        List<String> botTests = new ArrayList<String>();
        String currentTest =
                ALL_DT_LIST.get(testNameIndex + 2).substring(22, ALL_DT_LIST.get(testNameIndex + 2).length() - 1);
        botTests.add(currentTest);
        int startIndex = fullTests.indexOf(currentTest) + 1;
        int independentTestIndex = ALL_DT_LIST.indexOf(Constants.TEST_LINE + currentTest);

        // get all the tests that dependentTestName depends on to come before it
        while (independentTestIndex != -1) {
            currentTest = ALL_DT_LIST.get(independentTestIndex + 2).substring(22,
                    ALL_DT_LIST.get(independentTestIndex + 2).length() - 1);
            botTests.add(currentTest);
            independentTestIndex = ALL_DT_LIST.indexOf(Constants.TEST_LINE + currentTest);
        }

        // add dependentTestName to the list of tests that must
        // come before it and sort it in ascending order
        botTests.add(dependentTestName);
        List<String> descendingOrder = sortTestsDescendingOrder(botTests);
        botTests.clear();
        for (int i = descendingOrder.size() - 1; i >= 0; i--) {
            botTests.add(descendingOrder.get(i));
        }

        // run just dependentTestName and the tests that must come before it
        int chainIndex = botTests.size() - 2;
        boolean chainTestResult = isTestResultDifferent(dependentTestName, botTests);
        if (chainTestResult) {
            // just the chain causes dependentTestName to attain a different result
            // look inside the original order for what it is missing
            fullTests = ORIGINAL_ORDER_TESTS;
            isOriginalOrder = true;
            List<String> fullChainTests = new ArrayList<String>();

            String currentKey = botTests.get(0);
            Map<String, List<String>> testToChunk = new HashMap<String, List<String>>();
            for (int j = 1; j < botTests.size(); j++) {
                int fromIndex = fullTests.indexOf(currentKey);
                int toIndex = fullTests.indexOf(botTests.get(j));
                testToChunk.put(currentKey, new ArrayList<String>(fullTests.subList(fromIndex, toIndex)));
                currentKey = botTests.get(j);
            }

            // inject tests from the original order into the chain to determine
            // which part contains the test we need
            // TODO #1. This is incomplete. If the dependent test requires A and B to run
            // before it and A and B are in different sections of the chain
            // this will be unable to identify it.
            // We need to do delta debugging on the different sections of the chain
            // identify which sections are relevant to the dependent test.
            // Then use the depednentTestSolver and change the topAddOn and botAddOn
            // tests to pin down the tests in each section that is relevant to the dependent test.
            for (; chainIndex >= 0; chainIndex--) {
                fullChainTests.addAll(botTests.subList(0, chainIndex));
                fullChainTests.addAll(testToChunk.get(botTests.get(chainIndex)));
                fullChainTests.addAll(botTests.subList(chainIndex + 1, botTests.size()));

                boolean fullChainResult = isTestResultDifferent(dependentTestName, fullChainTests);
                if (!fullChainResult) {
                    // this current chunk of fullTests contains the test that will prevent
                    // the dependent test from manifesting
                    break;
                }
                fullChainTests.clear();
            }
        } else {
            // the chain does not cause dependentTestName to attain a different result
            // but something between the chain or before it is causing it to
            fullTests = CURRENT_ORDER_TESTS;
            isOriginalOrder = false;
            List<String> fullChainTests = new ArrayList<String>();

            String currentKey = botTests.get(0);
            Map<String, List<String>> testToChunk = new HashMap<String, List<String>>();
            for (int j = 1; j < botTests.size(); j++) {
                int toIndex = fullTests.indexOf(botTests.get(j));
                if (toIndex == -1) {
                	continue;
                }
                int fromIndex = fullTests.indexOf(currentKey);
                testToChunk.put(currentKey, new ArrayList<String>(fullTests.subList(fromIndex, toIndex)));
                currentKey = botTests.get(j);
            }

            // inject tests from the current order into the chain to determine
            // which part contains the test we need
            // TODO #2. This is incomplete. If the dependent test requires A and B to run
            // before it and A and B are in different sections of the chain
            // this will be unable to identify it.
            // We need to do delta debugging on the different sections of the chain
            // identify which sections are relevant to the dependent test.
            // Then use the depednentTestSolver and change the topAddOn and botAddOn
            // tests to pin down the tests in each section that is relevant to the dependent test.
            for (; chainIndex >= 0; chainIndex--) {
                fullChainTests.addAll(botTests.subList(0, chainIndex));
                if (testToChunk.get(botTests.get(chainIndex)) != null) {
                    fullChainTests.addAll(testToChunk.get(botTests.get(chainIndex)));                	
                }
                fullChainTests.addAll(botTests.subList(chainIndex + 1, botTests.size()));

                boolean fullChainResult = isTestResultDifferent(dependentTestName, fullChainTests);
                if (fullChainResult) {
                    // this current chunk of fullTests contains the test that will cause
                    // the dependent test to manifest
                    break;
                }
                fullChainTests.clear();
            }
        }

        int endIndex = -1;
        boolean useTopBot = true;
        List<String> topTests = new ArrayList<String>();
        if (chainIndex == -1) {
            // the relevant test is outside of the dependence chain,
            // use the entire fullTests order to the start of the chain
            startIndex = 0;
            endIndex = fullTests.indexOf(botTests.get(0));
            botTests = new ArrayList<String>(
                    fullTests.subList(fullTests.indexOf(botTests.get(0)), fullTests.indexOf(dependentTestName)));
            topTests = new ArrayList<String>();
        } else {
            // the relevant test is in between two tests in the dependence chain
            startIndex = fullTests.indexOf(botTests.get(chainIndex)) + 1;
            endIndex = fullTests.indexOf(botTests.get(chainIndex + 1));
            topTests = new ArrayList<String>(botTests.subList(0, botTests.indexOf(botTests.get(chainIndex)) + 1));
            botTests = new ArrayList<String>(botTests.subList(chainIndex + 1, botTests.size() - 1));
        }

        if (startIndex > fullTests.indexOf(dependentTestName)) {
            throw new RuntimeException(currentTest + " is marked to come before " + dependentTestName
                    + " but is currently coming after it.");
        }

        tests = fullTests.subList(startIndex, endIndex);
        dependentTestSolver(tests, dependentTestName, isOriginalOrder, beforeTests, afterTests, topTests, botTests,
                useTopBot);
        return isOriginalOrder;
    }

    // recursive method that adds a test to beforeTests or afterTests each call. The test
    // that is added is deemed to be depended on by dependentTestName
    private static void dependentTestSolver(List<String> tests, String dependentTestName, boolean isOriginalOrder,
            List<String> beforeTests, List<String> afterTests, List<String> topAddOnTests, List<String> botAddOnTests,
            boolean useAddOnTests) {
        tests.removeAll(topAddOnTests);
        tests.removeAll(botAddOnTests);
        List<String> topHalf = new LinkedList<String>(tests.subList(0, tests.size() / 2));
        List<String> botHalf = new LinkedList<String>(tests.subList(tests.size() / 2, tests.size()));

        while (tests.size() > 1) {

            if (useAddOnTests) {
                topHalf.addAll(0, topAddOnTests);
                botHalf.addAll(0, topAddOnTests);

                topHalf.addAll(topHalf.size(), botAddOnTests);
                botHalf.addAll(botHalf.size(), botAddOnTests);
            }

            topHalf.add(dependentTestName);
            botHalf.add(dependentTestName);

            FileTools.clearEnv(FILES_TO_DELETE);
            AbstractTestRunner runner = new FixedOrderRunner(CLASSPATH, topHalf);
            boolean topResults =
                    checkTestMatch(isOriginalOrder, runner.run().getExecutionRecords().get(0), dependentTestName);

            FileTools.clearEnv(FILES_TO_DELETE);
            runner = new FixedOrderRunner(CLASSPATH, botHalf);
            boolean botResults =
                    checkTestMatch(isOriginalOrder, runner.run().getExecutionRecords().get(0), dependentTestName);

            // dependent test depends on more than one test in tests
            if (topResults == botResults) {

                // if splitting the tests no longer reveals the dependent test behavior,
                // then use both sides in the recursive call
                useAddOnTests = !topResults || useAddOnTests;

                List<String> newBeforeTests = new ArrayList<String>(beforeTests);
                List<String> newAfterTests = new ArrayList<String>(afterTests);
                List<String> newTopList = new ArrayList<String>(topHalf);
                List<String> newBotList = new ArrayList<String>(botHalf);

                // find the test in the bottom half of our current list
                newTopList.removeAll(topAddOnTests);
                newTopList.removeAll(botAddOnTests);
                newTopList.remove(newTopList.size() - 1);
                newBotList.removeAll(topAddOnTests);
                newBotList.remove(newBotList.size() - 1);
                if (topResults) {
                    // A or B causes the dependent test to manifest
                    // recursive call with a truncated test list to identify A first
                    dependentTestSolver(newTopList, dependentTestName, isOriginalOrder, beforeTests, afterTests,
                            topAddOnTests, botAddOnTests, useAddOnTests);
                } else {
                    // A and B causes the dependent test to manifest
                    // recursive call with the bottom half of tests fixed
                    dependentTestSolver(newTopList, dependentTestName, isOriginalOrder, beforeTests, afterTests,
                            topAddOnTests, newBotList, useAddOnTests);
                }

                if (!beforeTests.equals(newBeforeTests)) {
                    List<String> tempBeforeTests = new ArrayList<String>(beforeTests);
                    beforeTests.removeAll(newBeforeTests);
                    if (!topAddOnTests.containsAll(beforeTests)) {
                        beforeTests.removeAll(topAddOnTests);
                        topAddOnTests.addAll(beforeTests);
                    }
                    beforeTests.clear();
                    beforeTests.addAll(tempBeforeTests);
                }

                if (!afterTests.equals(newAfterTests)) {
                    List<String> tempAfterTests = new ArrayList<String>(afterTests);
                    afterTests.removeAll(newAfterTests);
                    if (!topAddOnTests.containsAll(afterTests)) {
                        afterTests.removeAll(topAddOnTests);
                        topAddOnTests.addAll(afterTests);
                    }
                    afterTests.clear();
                    afterTests.addAll(tempAfterTests);
                }

                List<String> orderedTests = new ArrayList<String>(topAddOnTests);
                orderedTests.addAll(botAddOnTests);
                orderedTests.add(dependentTestName);
                boolean testResult = isTestResultDifferent(dependentTestName, orderedTests);
                if (!((!isOriginalOrder && testResult) || (isOriginalOrder && !testResult))) {
                    // case of A and B needs to come before C
                    dependentTestSolver(newBotList, dependentTestName, isOriginalOrder, beforeTests, afterTests,
                            topAddOnTests, botAddOnTests, useAddOnTests);
                } else if (testResult && isOriginalOrder) {
                    dependentTestSolver(newBotList, dependentTestName, isOriginalOrder, beforeTests, afterTests,
                            topAddOnTests, botAddOnTests, false);
                }
                return;
            }

            if (topResults) {
                tests = topHalf;
            } else {
                tests = botHalf;
            }

            if (useAddOnTests) {
                tests.removeAll(topAddOnTests);
                tests.removeAll(botAddOnTests);
            }
            tests.remove(tests.size() - 1);
            topHalf = new LinkedList<String>(tests.subList(0, tests.size() / 2));
            botHalf = new LinkedList<String>(tests.subList(tests.size() / 2, tests.size()));
        }

        if (!tests.isEmpty() && !beforeTests.contains(tests.get(0)) && !afterTests.contains(tests.get(0))) {
            if (isOriginalOrder) {
                beforeTests.add(tests.get(0));
            } else {
                afterTests.add(tests.get(0));
            }
        }
    }

    // Will be used to fix TODO #1 and TODO #2.
    // Uses delta debugging to identify which sections of a
    // dependence chain is relevant to the dependent test.
    private static void dependentTestSolverChunks(List<String> keys, List<String> allTests,
            Map<String, List<String>> testToChunk, String dependentTestName, boolean isOriginalOrder,
            List<String> beforeTests, List<String> afterTests, List<String> topAddOnTests, List<String> botAddOnTests,
            boolean useAddOnTests) {

        if (keys.size() == 0) {
            throw new RuntimeException("There are no chunks to delta debug.");
        }

        while (keys.size() > 1) {
            List<String> topHalfKeys = new LinkedList<String>(keys.subList(0, keys.size() / 2));
            List<String> botHalfKeys = new LinkedList<String>(keys.subList(keys.size() / 2, keys.size()));

            List<String> topHalf = new ArrayList<String>();
            List<String> botHalf = new ArrayList<String>();
            for (String key : topHalfKeys) {
                topHalf.addAll(testToChunk.get(key));
            }
            for (String key : botHalfKeys) {
                botHalf.addAll(testToChunk.get(key));
            }

            if (useAddOnTests) {
                topHalf.addAll(0, topAddOnTests);
                botHalf.addAll(0, topAddOnTests);

                topHalf.addAll(topHalf.size(), botAddOnTests);
                botHalf.addAll(botHalf.size(), botAddOnTests);
            }

            topHalf.add(dependentTestName);
            botHalf.add(dependentTestName);

            FileTools.clearEnv(FILES_TO_DELETE);
            AbstractTestRunner runner = new FixedOrderRunner(CLASSPATH, topHalf);
            boolean topResults =
                    checkTestMatch(isOriginalOrder, runner.run().getExecutionRecords().get(0), dependentTestName);

            FileTools.clearEnv(FILES_TO_DELETE);
            runner = new FixedOrderRunner(CLASSPATH, botHalf);
            boolean botResults =
                    checkTestMatch(isOriginalOrder, runner.run().getExecutionRecords().get(0), dependentTestName);

            // dependent test depends on more than one test in tests
            if (topResults == botResults) {

                // if splitting the tests no longer reveals the dependent test behavior,
                // then use both sides in the recursive call
                useAddOnTests = !topResults || useAddOnTests;

                List<String> newBeforeTests = new ArrayList<String>(beforeTests);
                List<String> newAfterTests = new ArrayList<String>(afterTests);
                List<String> newBotList = new ArrayList<String>(botHalf);

                // find the test in the bottom half of our current list
                newBotList.removeAll(topAddOnTests);
                newBotList.remove(newBotList.size() - 1);
                if (topResults) {
                    // A or B causes the dependent test to manifest
                    // recursive call with a truncated test list to identify A first
                    dependentTestSolverChunks(topHalfKeys, allTests, testToChunk, dependentTestName, isOriginalOrder,
                            beforeTests, afterTests, topAddOnTests, botAddOnTests, useAddOnTests);
                } else {
                    // A and B causes the dependent test to manifest
                    // recursive call with the bottom half of tests fixed
                    dependentTestSolverChunks(topHalfKeys, allTests, testToChunk, dependentTestName, isOriginalOrder,
                            beforeTests, afterTests, topAddOnTests, newBotList, useAddOnTests);
                }

                // if we found new dependent tests add them to our top fixed tests
                // so we don't find them again
                if (!beforeTests.equals(newBeforeTests)) {
                    List<String> tempBeforeTests = new ArrayList<String>(beforeTests);
                    beforeTests.removeAll(newBeforeTests);
                    if (!topAddOnTests.containsAll(beforeTests)) {
                        beforeTests.removeAll(topAddOnTests);
                        for (String key : beforeTests) {
                            topAddOnTests.addAll(testToChunk.get(key));
                        }
                    }
                    beforeTests.clear();
                    beforeTests.addAll(tempBeforeTests);
                }
                if (!afterTests.equals(newAfterTests)) {
                    List<String> tempAfterTests = new ArrayList<String>(afterTests);
                    afterTests.removeAll(newAfterTests);
                    if (!topAddOnTests.containsAll(afterTests)) {
                        afterTests.removeAll(topAddOnTests);
                        for (String key : afterTests) {
                            topAddOnTests.addAll(testToChunk.get(key));
                        }
                    }
                    afterTests.clear();
                    afterTests.addAll(tempAfterTests);
                }

                // we have found A, now look for B for A and B needs to come before C or
                // A or B needs to come before C
                List<String> orderedTests = new ArrayList<String>(topAddOnTests);
                orderedTests.addAll(botAddOnTests);
                orderedTests.add(dependentTestName);
                boolean testResult = isTestResultDifferent(dependentTestName, orderedTests);
                if (!((!isOriginalOrder && testResult) || (isOriginalOrder && !testResult))) {
                    // case of A and B needs to come before C
                    dependentTestSolverChunks(botHalfKeys, allTests, testToChunk, dependentTestName, isOriginalOrder,
                            beforeTests, afterTests, topAddOnTests, botAddOnTests, useAddOnTests);
                } else if (testResult && isOriginalOrder) {
                    dependentTestSolverChunks(botHalfKeys, allTests, testToChunk, dependentTestName, isOriginalOrder,
                            beforeTests, afterTests, topAddOnTests, botAddOnTests, false);
                }
                return;
            }

            if (topResults) {
                keys = topHalfKeys;
            } else {
                keys = botHalfKeys;
            }
        }

        if (!beforeTests.contains(keys.get(0)) && !afterTests.contains(keys.get(0))) {
            if (isOriginalOrder) {
                beforeTests.add(keys.get(0));
            } else {
                afterTests.add(keys.get(0));
            }
        }
    }

    // check whether the before and after tests that dependentTestName depends on are
    // come before or after it (respectively)
    private static void checkOrder(boolean isOriginalOrder, String dependentTestName, List<String> beforeTests,
            List<String> afterTests) {
        beforeTests = sortTestsDescendingOrder(beforeTests);
        afterTests = sortTestsDescendingOrder(afterTests);

        if (beforeTests.size() != 0) {
            // there is a test that comes after dependentTestName in beforeTests
            // change that test to be the dependentTestName instead
            if (ORIGINAL_ORDER_TESTS.indexOf(beforeTests.get(0)) > ORIGINAL_ORDER_TESTS.indexOf(dependentTestName)) {
                beforeTests.add(dependentTestName);
                dependentTestName = beforeTests.get(0);
                beforeTests.remove(0);
                beforeTests = sortTestsDescendingOrder(beforeTests);
            }

            String highestIndexTest = beforeTests.get(0);
            checkDependentTest(dependentTestName, highestIndexTest);

            for (int i = 1; i < beforeTests.size(); i++) {
                checkDependentTest(highestIndexTest, beforeTests.get(i));
                highestIndexTest = beforeTests.get(i);
            }
        }

        if (afterTests.size() != 0) {
            // there is a test that comes before dependentTestName in afterTests
            // change that test to be the dependentTestName instead
            if (ORIGINAL_ORDER_TESTS.indexOf(afterTests.get(afterTests.size() - 1)) < ORIGINAL_ORDER_TESTS
                    .indexOf(dependentTestName)) {
                afterTests.add(dependentTestName);
                dependentTestName = afterTests.get(afterTests.size() - 2);
                afterTests.remove(afterTests.size() - 2);
                afterTests = sortTestsDescendingOrder(afterTests);
            }

            String highestIndexTest = afterTests.get(0);
            for (int i = 1; i < afterTests.size(); i++) {
                checkDependentTest(highestIndexTest, afterTests.get(i));
                highestIndexTest = afterTests.get(i);
            }
            checkDependentTest(highestIndexTest, dependentTestName);
        }
    }

    // checks whether the dependentTest and the independentTest is valid to print
    // if not tests in the ALL_DT_LIST is rearranged before printing
    private static void checkDependentTest(String dependentTest, String independentTest) {
        int dependentTestIndex = -1;
        int testNameIndex = ALL_DT_LIST.indexOf("Test: " + dependentTest);
        if (testNameIndex != -1) {
            dependentTestIndex = testNameIndex + 2;
            String tests =
                    ALL_DT_LIST.get(dependentTestIndex).substring(22, ALL_DT_LIST.get(dependentTestIndex).length() - 1);

            if (ORIGINAL_ORDER_TESTS.indexOf(tests) < ORIGINAL_ORDER_TESTS.indexOf(independentTest)) {
                int independentTestIndex = ALL_DT_LIST.indexOf("when executed after: [" + independentTest + "]");
                if (independentTestIndex == -1) {
                    ALL_DT_LIST.remove(dependentTestIndex);
                    ALL_DT_LIST.add(dependentTestIndex, "when executed after: [" + independentTest + "]");
                } else {
                    // independentTest is already declared. find a test that is after it
                    // that no other test needs to come after it
                    mergeChains(independentTest, dependentTest);
                }
                checkDependentTest(independentTest, tests);
                printDependenceHelper(ALL_DT_LIST, DT_FILE);
            } else if (ORIGINAL_ORDER_TESTS.indexOf(tests) > ORIGINAL_ORDER_TESTS.indexOf(independentTest)) {
                checkDependentTest(tests, independentTest);
            }
        } else {
            printDependence(independentTest, dependentTest);
        }
    }

    // checks whether the independentTest is already declared to come before a test in ALL_DT_LIST
    // if it is declared then a merge is necessary.
    // if not then proceed to output the dependentTest and independentTest
    private static void printDependence(String independentTest, String dependentTest) {
        int independentTestIndex = ALL_DT_LIST.indexOf("when executed after: [" + independentTest + "]");
        if (independentTestIndex != -1) {
            mergeChains(independentTest, dependentTest);
        } else {
            int testNameIndex = ALL_DT_LIST.indexOf(Constants.TEST_LINE + dependentTest);
            if (testNameIndex == -1) {
                ALL_DT_LIST.add(Constants.TEST_LINE + dependentTest);
                ALL_DT_LIST.add("Intended behavior: FAILURE");
                ALL_DT_LIST.add("when executed after: [" + independentTest + "]");
                ALL_DT_LIST.add("The revealed different behavior: PASS");
                ALL_DT_LIST.add("when executed after: []");
            }
        }
        printDependenceHelper(ALL_DT_LIST, DT_FILE);
    }

    // sorts tests based on the opposite order as they would appear on the original order list
    // C -> B -> A if original order was ABC
    private static List<String> sortTestsDescendingOrder(List<String> tests) {
        if (tests == null) {
            throw new RuntimeException("Unable to sort empty test list.");
        }

        List<String> sortedTests = new ArrayList<String>();
        while (tests.size() != 0) {
            int testListIndex = 0;
            int orderListIndex = ORIGINAL_ORDER_TESTS.indexOf(tests.get(0));

            for (int i = 1; i < tests.size(); i++) {
                int currentTestIndex = ORIGINAL_ORDER_TESTS.indexOf(tests.get(i));
                if (currentTestIndex > orderListIndex) {
                    orderListIndex = currentTestIndex;
                    testListIndex = i;
                }
            }
            sortedTests.add(tests.get(testListIndex));
            tests.remove(testListIndex);
        }
        return sortedTests;
    }

    // traverses the ALL_DT_LIST to determine what independentTest dependentTest can depend on
    // to come before it so that independentTest is guaranteed to come before it
    private static void mergeChains(String independentTest, String dependentTest) {
        int independentTestIndex = ALL_DT_LIST.indexOf("when executed after: [" + independentTest + "]");
        if (independentTestIndex == -1) {
            throw new RuntimeException(independentTest + " does not currently exist in the DT list.");
        }
        String newIndependentTest = ALL_DT_LIST.get(independentTestIndex - 2).substring(Constants.TEST_LINE.length());

        // find the test with the highest index in the original order where the test is
        // in between independentTest and dependentTest and is part of independentTest's chain
        while (ORIGINAL_ORDER_TESTS.indexOf(dependentTest) > ORIGINAL_ORDER_TESTS.indexOf(newIndependentTest)) {
            independentTest = newIndependentTest;
            independentTestIndex = ALL_DT_LIST.indexOf("when executed after: [" + newIndependentTest + "]");
            if (independentTestIndex == -1) {
                break;
            }
            String newTest = ALL_DT_LIST.get(independentTestIndex - 2).substring(Constants.TEST_LINE.length());
            if (newTest.equals(newIndependentTest)) {
            	throw new RuntimeException(newIndependentTest + " appears numerous times in DT file!");
            }
            newIndependentTest = newTest;
        }

        // if dependentTestName isn't already in the ALL_DT_LIST, add it
        // fixes the bottom half of a chain when injecting dependentTest
        int testNameIndex = ALL_DT_LIST.indexOf(Constants.TEST_LINE + dependentTest);
        if (testNameIndex == -1) {
            ALL_DT_LIST.add(Constants.TEST_LINE + dependentTest);
            ALL_DT_LIST.add("Intended behavior: FAILURE");
            ALL_DT_LIST.add("when executed after: [" + independentTest + "]");
            ALL_DT_LIST.add("The revealed different behavior: PASS");
            ALL_DT_LIST.add("when executed after: []");
        }

        // if independentTest already exists in ALL_DT_LIST then delete it
        // fixes the top half of a chain when injecting dependentTest to the chain
        if (independentTestIndex != -1) {
            int dependentTestIndex = ALL_DT_LIST.indexOf("when executed after: [" + dependentTest + "]");
            ALL_DT_LIST.remove(independentTestIndex);
            ALL_DT_LIST.add(independentTestIndex, "when executed after: [" + dependentTest + "]");

            // now that we injected dependentTest into another chain,
            // check whether we ruined dependentTest's chain or not
            while (dependentTestIndex != -1) {
                independentTest = dependentTest;
                dependentTest = ALL_DT_LIST.get(dependentTestIndex - 2).substring(Constants.TEST_LINE.length());
                while (ORIGINAL_ORDER_TESTS.indexOf(dependentTest) > ORIGINAL_ORDER_TESTS.indexOf(newIndependentTest)) {
                    independentTest = newIndependentTest;
                    independentTestIndex = ALL_DT_LIST.indexOf("when executed after: [" + newIndependentTest + "]");
                    if (independentTestIndex == -1) {
                        break;
                    }
                    newIndependentTest =
                            ALL_DT_LIST.get(independentTestIndex - 2).substring(Constants.TEST_LINE.length());
                }

                if (independentTestIndex != -1) {
                    ALL_DT_LIST.remove(dependentTestIndex);
                    ALL_DT_LIST.add(dependentTestIndex, "when executed after: [" + independentTest + "]");
                    dependentTestIndex = ALL_DT_LIST.indexOf("when executed after: [" + dependentTest + "]");
                    ALL_DT_LIST.remove(independentTestIndex);
                    ALL_DT_LIST.add(independentTestIndex, "when executed after: [" + dependentTest + "]");
                } else {
                    ALL_DT_LIST.remove(dependentTestIndex);
                    ALL_DT_LIST.add(dependentTestIndex, "when executed after: [" + independentTest + "]");
                    break;
                }
            }
        }
    }

    // outputs the ALL_DT_LIST to DT_FILE. With every 5 lines, a line break is inserted
    public static void printDependenceHelper(List<String> allDTList, File outputFile) {
        if (outputFile != null) {
            FileWriter output = null;
            BufferedWriter writer = null;
            try {
                output = new FileWriter(outputFile);
                writer = new BufferedWriter(output);

                for (int j = 0; j < allDTList.size();) {
                    for (int i = 0; i < 5; j++) {
                        writer.write(allDTList.get(j) + "\n");
                        i++;
                    }
                    writer.write("\n");
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
    }

    // runs orderedTests and determine whether dependentTestName
    // in the orderTests matches DEPEDENT_TEST_RESULT
    // returns true if dependentTestName's result in orderedTest
    // does not match DEPENDENT_TEST_RESULT, false otherwise
    private static boolean isTestResultDifferent(String dependentTestName, List<String> orderedTests) {
        FileTools.clearEnv(FILES_TO_DELETE);
        AbstractTestRunner runner = new FixedOrderRunner(CLASSPATH, orderedTests);
        TestExecResults results = runner.run();

        RESULT dtIsolateResult = null;
        TestExecResult result = results.getExecutionRecords().get(0);
        if (result.isTestPassed(dependentTestName)) {
            dtIsolateResult = RESULT.PASS;
        } else if (result.isTestError(dependentTestName)) {
            dtIsolateResult = RESULT.ERROR;
        } else {
            dtIsolateResult = RESULT.FAILURE;
        }

        return !dtIsolateResult.equals(DEPENDENT_TEST_RESULT);
    }

    // returns true if dependentTestName's result in results is the same as
    // DEPENDENT_TEST_RESULT and if it is original order, false otherwise
    private static boolean checkTestMatch(boolean isOriginalOrder, TestExecResult results, String dependentTestName) {
        boolean testResult;
        if (DEPENDENT_TEST_RESULT.equals(RESULT.PASS)) {
            testResult = results.isTestPassed(dependentTestName);
        } else if (DEPENDENT_TEST_RESULT.equals(RESULT.ERROR)) {
            testResult = results.isTestError(dependentTestName);
        } else {
            testResult = results.isTestFailed(dependentTestName);
        }

        return testResult == isOriginalOrder;
    }
}
