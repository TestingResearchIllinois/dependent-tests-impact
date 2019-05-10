/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam
 *
 *         Reorders a list of TestFunctionStatement based on its dependent tests if specified to do so.
 *         Prints the ordered list of tests.
 */

package edu.washington.cs.dt.impact.order;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.washington.cs.dt.impact.data.TestFunctionStatement;

public class Standard {
    protected String filename;
    protected List<TestFunctionStatement> methodList;
    protected boolean getCoverage;
    protected Set<String> allLines;

    public Standard(String outputFileName, List<TestFunctionStatement> methodList, boolean getCoverage,
            Set<String> allLines) {
        this.filename = outputFileName;
        this.methodList = methodList;
        this.getCoverage = getCoverage;
        this.allLines = allLines;
    }

    public Standard(String outputFileName, List<TestFunctionStatement> methodList) {
        this(outputFileName, methodList, false);
    }

    public Standard(String outputFileName, List<TestFunctionStatement> methodList, boolean getCoverage) {
        this(outputFileName, methodList, getCoverage, null);
    }

    public void applyDeps() {
        List<TestFunctionStatement> orderedListOfTests = new LinkedList<TestFunctionStatement>();
        Set<String> alreadyAddedTests = new HashSet<String>();
        System.out.println(methodList);

        // Go through list and sort it out (now everything should follow the afters format
        for (TestFunctionStatement test : methodList) {
            if (test == null) {
                continue;
            }
            System.out.println(test.getName() + ": " + test.getDependentTests(true) + " " + test.getDependentTests(false));
            applyDepsHelper(orderedListOfTests, test, alreadyAddedTests);
        }
        methodList = orderedListOfTests;
        System.out.println(methodList);
    }

    // If newTest is not in orderedListOfTests, adds newTest as the last
    // element, and may also add, before newTest, dependee tests that newTest
    // depends on.
    // Precondition: orderedListOfTests satisfies all test dependences: for
    // every test in the list, all of its dependees appear before it.
    // Postcondition: orderedListOfTests contains newTest and satisfies all
    // test dependences
    private void applyDepsHelper(List<TestFunctionStatement> orderedListOfTests, TestFunctionStatement newTest, Set<String> alreadyAddedTests) {
        if (!orderedListOfTests.contains(newTest)) {
            List<TestFunctionStatement> testsThatNeedToExecuteBeforeNewTest = new LinkedList<>(newTest.getDependentTests(false));
            testsThatNeedToExecuteBeforeNewTest.retainAll(methodList);  // Only use the ones that are in methodList, the selected ones
            Collections.sort(testsThatNeedToExecuteBeforeNewTest);  // Sort list of tests that come before based on their weights
            // Add all tests that needs to come before newTest to the orderedListOfTests
            for (TestFunctionStatement positiveDependee : testsThatNeedToExecuteBeforeNewTest) {
                applyDepsHelper(orderedListOfTests, positiveDependee, alreadyAddedTests);
            }

            // Add newTest to orderedListOfTests
            if (!alreadyAddedTests.contains(newTest.getName())) {
                orderedListOfTests.add(newTest);
                alreadyAddedTests.add(newTest.getName());
            }
        }
    }

    // record the percent of coverage as xx.xx%
    public static String formatPercent(final double value) {
        long displayPercent = (long) (value * 10000);

        char remainder = String.valueOf(displayPercent % 100).charAt(0);
        displayPercent = displayPercent / 100;

        return displayPercent + "." + remainder;
    }

    // Used to get the percent coverage each test is responsible for based on their current
    // order in methodList
    public List<TestFunctionStatement> getCoverage(boolean includeName) {
        // TODO does not work with parallelization, not sure why.
        Set<String> allLinesCpy = new HashSet<>(allLines);
        List<TestFunctionStatement> coverageList = new LinkedList<>();
        while (methodList.size() > 0) {
            TestFunctionStatement tfs = methodList.remove(0);
            tfs.reset();
            int beforeSize = allLinesCpy.size();
            allLinesCpy.removeAll(tfs.getLines());
            int afterSize = beforeSize - allLinesCpy.size();

            // record the percent of coverage as xx.xx%
            final String displayPercent = formatPercent((double)afterSize / allLines.size());

            if (includeName) {
                tfs.setName((tfs.getName() + " : " + displayPercent));
            } else {
                tfs.setName(displayPercent);
            }
            coverageList.add(tfs);
        }
        return coverageList;
    }

    public List<TestFunctionStatement> getMethodList() {
        return methodList;
    }

    public void printResults() {
        if (getCoverage) {
            methodList = getCoverage(true);
        }

        if (filename == null) {
            for (TestFunctionStatement methodData : methodList) {
                System.out.println(methodData.getName());
            }
        } else {
            FileWriter output = null;
            BufferedWriter writer = null;
            try {
                output = new FileWriter(filename);
                writer = new BufferedWriter(output);

                for (TestFunctionStatement methodData : methodList) {
                    writer.write(methodData.getName() + "\n");
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
        }
    }
}
