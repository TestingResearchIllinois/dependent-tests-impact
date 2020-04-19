/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam
 *
 *         Creates a list of tests that is ordered with test selection based on the parameters
 *         specified to the constructor.
 */

package edu.washington.cs.dt.impact.technique;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.washington.cs.dt.impact.data.TestFunctionStatement;
import edu.washington.cs.dt.impact.order.Relative;
import edu.washington.cs.dt.impact.order.Standard;
import edu.washington.cs.dt.impact.tools.FileTools;
import edu.washington.cs.dt.impact.util.Constants.COVERAGE;
import edu.washington.cs.dt.impact.util.Constants.ORDER;

public class Selection extends Test {

    /**
     *
     * @param order the order in which to order the tests with
     * @param outputFileName the name of the output file
     * @param inputTestFolder folder containing all test cases
     * @param coverage the coverage to consider when processing the test cases
     * @param selectionOutput1 contains the coverage information of each test case
     * @param selectionOutput2 contains the coverage information of each test case.
     *            The coverage information here should differ from selectionOutput1
     * @param origOrder the original order in which the test cases are ran in
     * @param dependentTestsFile the file containing the information
     *            regarding which test depends on which
     */
    public Selection(ORDER order, String outputFileName, File inputTestFolder, COVERAGE coverage, File selectionOutput1,
            File selectionOutput2, File origOrder, File dependentTestsFile, boolean getCoverage, boolean mergeDTsCoverage) {
        this(order, outputFileName, inputTestFolder, coverage, selectionOutput1, selectionOutput2, FileTools.parseFileToList(origOrder), FileTools.parseFileToList(dependentTestsFile), getCoverage, mergeDTsCoverage);
    }

    public Selection(ORDER order, String outputFileName, File inputTestFolder, COVERAGE coverage, File selectionOutput1,
            File selectionOutput2, List<String> origOrder, List<String> allDTList, boolean getCoverage, boolean mergeDTsCoverage) {
        super(inputTestFolder, coverage, allDTList, origOrder, mergeDTsCoverage);
        select(order, outputFileName, coverage, selectionOutput1, selectionOutput2, origOrder, getCoverage);
    }

    public Selection(ORDER order, String outputFileName, File inputTestFolder, COVERAGE coverage, File selectionOutput1,
            File selectionOutput2, List<String> origOrder, Map<String, List<String>> execBefore, Map<String, List<String>> execAfter,
            boolean getCoverage, boolean mergeDTsCoverage) {
        super(inputTestFolder, coverage, execBefore, execAfter, origOrder, mergeDTsCoverage);
        select(order, outputFileName, coverage, selectionOutput1, selectionOutput2, origOrder, getCoverage);
    }

    // Helper method for selecting
    private void select(ORDER order, String outputFileName, COVERAGE coverage, File selectionOutput1, File selectionOutput2,
            List<String> origOrder, boolean getCoverage) {
        Set<String> changedCoverage = findCoverage(selectionOutput1, selectionOutput2, coverage);

        // removes all tests from consideration if they don't execute
        // any of the methods that has been changed
        Map<String, TestFunctionStatement> nameToMethodData = new HashMap<String, TestFunctionStatement>();
        for (TestFunctionStatement methodData : methodList) {
            methodData.retainLines(changedCoverage);
        }
        for (TestFunctionStatement methodData : methodList) {
            if (methodData.getLineCount() != 0) {
                nameToMethodData.put(methodData.getName(), methodData);
            }
        }

        // go through the methodList, find those that need to run before any that are in
        // the changed list, and include those as well
        for (TestFunctionStatement methodData : methodList) {
            for (TestFunctionStatement beforeData : methodData.getDependentTests(true)) {
                // one of the ones that this test must run before is in the changed list,
                // so ensure that the test is in the methodList, if not in there already
                if (nameToMethodData.containsKey(beforeData.getName())) {
                    if (!nameToMethodData.containsKey(methodData.getName())) {
                        nameToMethodData.put(methodData.getName(), methodData);
                    }
                }
            }
        }

        if (order == ORDER.RELATIVE) {
            methodList.retainAll(nameToMethodData.values());
            allCoverageLines.retainAll(changedCoverage);
            Collections.sort(methodList);
            orderObj = new Relative(outputFileName, methodList, getCoverage, allCoverageLines);
        } else {
            if (order == ORDER.ABSOLUTE) {
                methodList.retainAll(nameToMethodData.values());
                Collections.sort(methodList);
            } else if (order == ORDER.RANDOM) {
                methodList.retainAll(nameToMethodData.values());
                Collections.shuffle(methodList);
            } else if (order == ORDER.ORIGINAL) {
            	parseOrigOrderListToMethodList(origOrderList, nameToMethodData);
            } else {
                System.err.println("Test selection is specified with an incompatible order."
                        + " Compatible orders are: absolute, relative, random and original.");
                System.exit(0);
            }
            orderObj = new Standard(outputFileName, methodList, getCoverage, allCoverageLines);
        }
    }

    private Set<String> findCoverage(File selectionOutput1, File selectionOutput2, final COVERAGE coverage) {
        Map<String, List<String>> oldVersMap = coverageCount(selectionOutput1);
        Map<String, List<String>> newVersMap = coverageCount(selectionOutput2);
        Set<String> changedCoverage = new HashSet<String>();

        // !oldVersMap.get(key).equals(newVersMap.get(key)) does not work well.
        // Ex. selectionOuputs for org.joda.time.tz.ZoneInfoProvider.loadZoneInfoMap:
        /*
         * org.joda.time.tz.ZoneInfoProvider.loadZoneInfoMap : staticinvoke <org.joda.time.tz.ZoneInfoProvider: void readZoneInfoMap(java.io.DataInputStream,java.util.Map)>(r3, r2) >>>>>>>> org.joda.time.tz.ZoneInfoProvider.loadZoneInfoMap : $r10 := @caughtexception
         * org.joda.time.tz.ZoneInfoProvider.loadZoneInfoMap : staticinvoke <org.joda.time.tz.ZoneInfoProvider: void readZoneInfoMap(java.io.DataInputStream,java.util.Map)>(r3, r2) >>>>>>>> org.joda.time.tz.ZoneInfoProvider.loadZoneInfoMap : $r9 := @caughtexception
         */
        // While another may output the following.
        /*
		 * org.joda.time.tz.ZoneInfoProvider.loadZoneInfoMap : staticinvoke <org.joda.time.tz.ZoneInfoProvider: void readZoneInfoMap(java.io.DataInputStream,java.util.Map)>(r3, r2) >>>>>>>> org.joda.time.tz.ZoneInfoProvider.loadZoneInfoMap : $r9 := @caughtexception
		 * org.joda.time.tz.ZoneInfoProvider.loadZoneInfoMap : staticinvoke <org.joda.time.tz.ZoneInfoProvider: void readZoneInfoMap(java.io.DataInputStream,java.util.Map)>(r3, r2) >>>>>>>> org.joda.time.tz.ZoneInfoProvider.loadZoneInfoMap : $r10 := @caughtexception
         */

        if (coverage == COVERAGE.FUNCTION) {
            // generate the list of methods that has been changed
            for (String key : oldVersMap.keySet()) {
                if (!newVersMap.containsKey(key) || !oldVersMap.get(key).equals(newVersMap.get(key))) {
                    changedCoverage.add(key);
                }
            }
        } else if (coverage == COVERAGE.STATEMENT) {
            // generate the list of statements that has been changed
            for (String key : oldVersMap.keySet()) {
                if (!newVersMap.containsKey(key)) {
                    changedCoverage.addAll(oldVersMap.get(key));
                } else if (!oldVersMap.get(key).equals(newVersMap.get(key))) {
                    oldVersMap.get(key).removeAll(newVersMap.get(key));
                    changedCoverage.addAll(oldVersMap.get(key));
                }
            }
        }

        return changedCoverage;
    }

    private Map<String, List<String>> coverageCount(final File folder) {
        Map<String, List<String>> retMap = new HashMap<String, List<String>>();

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isFile()) {
                BufferedReader br;
                try {
                    br = new BufferedReader(new FileReader(fileEntry));
                    String line;
                    List<String> lines = new LinkedList<String>();
                    while ((line = br.readLine()) != null) {
                        lines.add(line);
                    }
                    br.close();
                    retMap.put(fileEntry.getName(), lines);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {

                    e.printStackTrace();
                }
            } else {
                continue;
            }
        }

        return retMap;
    }
}
