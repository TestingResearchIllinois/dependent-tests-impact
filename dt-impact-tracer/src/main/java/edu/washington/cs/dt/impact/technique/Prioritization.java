/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam
 *         Creates a list of tests that is ordered with test prioritization based on the parameters
 *         specified to the constructor.
 */

package edu.washington.cs.dt.impact.technique;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.washington.cs.dt.impact.data.TestFunctionStatement;
import edu.washington.cs.dt.impact.order.Relative;
import edu.washington.cs.dt.impact.order.Standard;
import edu.washington.cs.dt.impact.tools.FileTools;
import edu.washington.cs.dt.impact.util.Constants.COVERAGE;
import edu.washington.cs.dt.impact.util.Constants.ORDER;

public class Prioritization extends Test {

    /**
     *
     * @param order the order in which to order the tests with
     * @param inputTestFolder folder containing all test cases
     * @param coverage the coverage to consider when processing the test cases
     * @param dependentTestsFile the file containing the information
     *            regarding which test depends on which
     * @param getCoverage if true the coverage percentage of each test is printed
     *            if false, the execution result will be printed
     */
    public Prioritization(ORDER order, String outputFilename, File inputTestFolder, COVERAGE coverage,
            File dependentTestsFile, boolean getCoverage, File origOrder, boolean mergeDTsCoverage) {
        this(order, outputFilename, inputTestFolder, coverage, dependentTestsFile, getCoverage, origOrder,
                0, mergeDTsCoverage);
        for (TestFunctionStatement testFunctionStatement : methodList) {
            System.out.println(testFunctionStatement.getName() + "\t" + testFunctionStatement.getLineCount());
        }
    }

    public Prioritization(ORDER order, String outputFilename, File inputTestFolder, COVERAGE coverage,
            File dependentTestsFile, boolean getCoverage, File origOrder, int seed, boolean mergeDTsCoverage) {
        this(order, outputFilename, inputTestFolder, coverage, FileTools.parseFileToList(dependentTestsFile),
                getCoverage, FileTools.parseFileToList(origOrder), seed, false, mergeDTsCoverage);
    }

    public Prioritization(ORDER order, String outputFilename, File inputTestFolder, COVERAGE coverage,
            List<String> allDTList, boolean getCoverage, List<String> origOrder, int seed, boolean randomizeOriginal,
                          boolean mergeDTsCoverage) {
        super(inputTestFolder, coverage, allDTList, origOrder, mergeDTsCoverage);
        prioritize(order, outputFilename, getCoverage, origOrder, seed, randomizeOriginal);
    }

    public Prioritization(ORDER order, String outputFilename, File inputTestFolder, COVERAGE coverage,
            Map<String, List<String>> execBefore, Map<String, List<String>> execAfter,
            boolean getCoverage, List<String> origOrder, int seed, boolean randomizeOriginal, boolean mergeDTsCoverage) {
        super(inputTestFolder, coverage, execBefore, execAfter, origOrder, mergeDTsCoverage);
        prioritize(order, outputFilename, getCoverage, origOrder, seed, randomizeOriginal);
    }

    // Helper method for prioritizing
    private void prioritize(ORDER order, String outputFilename, boolean getCoverage, List<String> origOrder,
            int seed, boolean randomizeOriginal) {
        if (order == ORDER.ABSOLUTE || order == ORDER.RELATIVE) {
            Collections.sort(methodList);
            if (order == ORDER.RELATIVE) {
                methodList = new Relative(outputFilename, methodList, getCoverage, allCoverageLines).getMethodList();
            }
            orderObj = new Standard(outputFilename, methodList, getCoverage, allCoverageLines);
        } else if (order == ORDER.RANDOM) {
            Collections.shuffle(methodList, new Random(seed));
            orderObj = new Standard(outputFilename, methodList);
        } else if (order == ORDER.ORIGINAL) {
            parseOrigOrderListToMethodList(origOrder, getNameToMethodData(methodList));
            if (randomizeOriginal) {
                Collections.shuffle(methodList, new Random(seed));
            }
            orderObj = new Standard(outputFilename, methodList, getCoverage, allCoverageLines);
        } else {
            System.err.println("Test prioritization is specified with an incompatible order."
                    + " Compatible orders are: absolute, relative and random.");
            System.exit(0);
        }
    }

    public Standard getOrderObj() {
        return orderObj;
    }
}
