/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam
 *
 *         Contains methods and fields used by all test techniques.
 */

package edu.washington.cs.dt.impact.technique;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.washington.cs.dt.impact.data.TestFunctionStatement;
import edu.washington.cs.dt.impact.order.Standard;
import edu.washington.cs.dt.impact.tools.FileTools;
import edu.washington.cs.dt.impact.util.Constants;
import edu.washington.cs.dt.impact.util.Constants.COVERAGE;

public class Test {

    // all lines specific to the type of COVERAGE specified
    protected Set<String> allCoverageLines;
    protected List<TestFunctionStatement> methodList;
    protected List<TestFunctionStatement> allMethodList;
    protected Standard orderObj;
    private final Map<String, Set<String>> testToAllLines;
    protected List<String> origOrderList = null;

    public Test(File folder, COVERAGE coverage, File dependentTestsFile, File origOrder, boolean mergeDTsCoverage) {
        this(folder, coverage, dependentTestsFile, FileTools.parseFileToList(origOrder), mergeDTsCoverage);
    }

    public Test(File folder, COVERAGE coverage, File dependentTestsFile, List<String> origOrder, boolean mergeDTsCoverage) {
        this(folder, coverage, dependentTestsFile, null, origOrder, mergeDTsCoverage);
    }

    public Test(File folder, COVERAGE coverage, List<String> allDTList, List<String> origOrder, boolean mergeDTsCoverage) {
        this(folder, coverage, null, allDTList, origOrder, mergeDTsCoverage);
    }

    // Private constructor to help reduce redundancy of logic, which only depends on if dependentTestsFile or allDTList
    private Test(File folder, COVERAGE coverage, File dependentTestsFile, List<String> allDTList, List<String> origOrder, boolean mergeDTsCoverage) {
        origOrderList = origOrder;
        allCoverageLines = new HashSet<String>();
        testToAllLines = new HashMap<>();
        if (allMethodList == null) {
            setAllLines(folder);
            allMethodList = listFilesForFolder(coverage);
        }
        // Set merge configuration for each method and set each one's orig order index
        for (TestFunctionStatement tfs : allMethodList) {
            tfs.setMergeDTCoverage(mergeDTsCoverage);
            tfs.setOrigOrderIndex(origOrderList.indexOf(tfs.getName()));
        }
        methodList = new ArrayList<>(allMethodList);
        // Need one of these to be not null
        if (dependentTestsFile != null || allDTList != null) {
            processDependentTests(dependentTestsFile, allDTList, allMethodList);
        }

        // After processing the dependent tests, convert the befores format into the afters format
        // The only reason to keep the befores format around is to help with determining later by technique what tests must be included
        for (TestFunctionStatement test : methodList) {
            for (TestFunctionStatement before : test.getDependentTests(true)) {
                // Find the test in the list that is the before and add the current test to that one's after
                for (TestFunctionStatement other : methodList) {
                    if (other.getName().equals(before.getName())) {
                        other.addDependentTest(test, false);
                    }
                }
            }
        }
    }

    public void resetDTList(List<String> allDTList) {
        if (allDTList != null) {
            processDependentTests(null, allDTList, allMethodList);
        }
    }

    protected void processDependentTests(File dependentTestsFile, List<String> allDTList, List<TestFunctionStatement> dtMethodList) {
        // list of tests that when executed before reveals the dependent test
        Map<String, List<String>> execBefore = new HashMap<String, List<String>>();
        // list of tests that when executed after reveals the dependent test
        Map<String, List<String>> execAfter = new HashMap<String, List<String>>();

        Map<String, TestFunctionStatement> nameToMethodData = new HashMap<String, TestFunctionStatement>();
        for (TestFunctionStatement methodData : dtMethodList) {
            nameToMethodData.put(methodData.getName(), methodData);
        }

        if (dependentTestsFile != null && dependentTestsFile.isFile()) {
            parseDependentTestsFile(dependentTestsFile, execBefore, execAfter);
        } else {
            parseDependentTestsList(allDTList, execBefore, execAfter);
        }

        for (String testName : execBefore.keySet()) {
            if (nameToMethodData.get(testName) == null && origOrderList.contains(testName)) {
                TestFunctionStatement tmd = new TestFunctionStatement(testName);
                nameToMethodData.put(testName, tmd);
            }
            for (String dtTest : execBefore.get(testName)) {
                TestFunctionStatement tmd = nameToMethodData.get(dtTest);
                if (tmd == null && origOrderList.contains(dtTest)) {
                    tmd = new TestFunctionStatement(dtTest);
                    nameToMethodData.put(dtTest, tmd);
                }
                if (nameToMethodData.get(testName) != null && tmd != null) {
                    nameToMethodData.get(testName).addDependentTest(tmd, true);
                }
            }
            if (nameToMethodData.get(testName) != null) {
                nameToMethodData.get(testName).reset();            	
            }
        }

        for (String testName : execAfter.keySet()) {
            if (nameToMethodData.get(testName) == null && origOrderList.contains(testName)) {
                TestFunctionStatement tmd = new TestFunctionStatement(testName);
                nameToMethodData.put(testName, tmd);
            }
            for (String dtTest : execAfter.get(testName)) {
                TestFunctionStatement tmd = nameToMethodData.get(dtTest);
                if (tmd == null && origOrderList.contains(dtTest)) {
                    tmd = new TestFunctionStatement(dtTest);
                    nameToMethodData.put(dtTest, tmd);
                }
                if (nameToMethodData.get(testName) != null && tmd != null) {
                    nameToMethodData.get(testName).addDependentTest(tmd, false);                	
                }
            }
            if (nameToMethodData.get(testName) != null) {
                nameToMethodData.get(testName).reset();            	
            }
        }
    }

    private void parseDependentTestsList(List<String> allDTList, Map<String, List<String>> execBefore,
            Map<String, List<String>> execAfter) {
        for (int j = 0; j < allDTList.size();) {
            String line = allDTList.get(j);
            if (line.length() == 0) {
                j++;
                continue;
            }
            String testName = line.split(Constants.TEST_LINE)[1];
            // revealed behavior line
            j += 2;

            // tests reveal dependence when executed after testName
            line = allDTList.get(j);
            String afterTestsStr = line.split(Constants.EXECUTE_AFTER)[1];
            if (afterTestsStr.length() > 2) {
                afterTestsStr = afterTestsStr.substring(1, afterTestsStr.length() - 1);
                if (!execAfter.containsKey(testName)) {
                    execAfter.put(testName, new ArrayList<String>());
                }
                execAfter.get(testName).addAll(Arrays.asList(afterTestsStr.split(Constants.TEST_SEP)));
            }
            // revealed behavior line
            j += 2;

            // tests reveal dependence when executed before testName
            line = allDTList.get(j);
            String beforeTestsStr = line.split(Constants.EXECUTE_AFTER)[1];
            if (beforeTestsStr.length() > 2) {
                beforeTestsStr = beforeTestsStr.substring(1, beforeTestsStr.length() - 1);
                if (!execBefore.containsKey(testName)) {
                    execBefore.put(testName, new ArrayList<String>());
                }
                execBefore.get(testName).addAll(Arrays.asList(beforeTestsStr.split(Constants.TEST_SEP)));
            }
            j += 1;
        }
    }

    private void parseDependentTestsFile(File dependentTestsFile, Map<String, List<String>> execBefore,
            Map<String, List<String>> execAfter) {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(dependentTestsFile));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }

                String testName = line.split(Constants.TEST_LINE)[1];

                // intended behavior line
                br.readLine();

                // tests reveal dependence when executed after testName
                line = br.readLine();
                String afterTestsStr = line.split(Constants.EXECUTE_AFTER)[1];
                if (afterTestsStr.length() > 2) {
                    afterTestsStr = afterTestsStr.substring(1, afterTestsStr.length() - 1);
                    if (!execAfter.containsKey(testName)) {
                        execAfter.put(testName, new ArrayList<String>());
                    }
                    execAfter.get(testName).addAll(Arrays.asList(afterTestsStr.split(Constants.TEST_SEP)));
                }

                // revealed behavior line
                br.readLine();

                // tests reveal dependence when executed before testName
                line = br.readLine();
                String beforeTestsStr = line.split(Constants.EXECUTE_AFTER)[1];
                if (beforeTestsStr.length() > 2) {
                    beforeTestsStr = beforeTestsStr.substring(1, beforeTestsStr.length() - 1);
                    if (!execBefore.containsKey(testName)) {
                        execBefore.put(testName, new ArrayList<String>());
                    }
                    execBefore.get(testName).addAll(Arrays.asList(beforeTestsStr.split(Constants.TEST_SEP)));
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<TestFunctionStatement> listFilesForFolder(final COVERAGE coverage) {
        List<TestFunctionStatement> methodList = new LinkedList<TestFunctionStatement>();
        for (String testName : testToAllLines.keySet()) {
            TestFunctionStatement methodData = new TestFunctionStatement(testName);
            for (String line : testToAllLines.get(testName)) {
                if (coverage == COVERAGE.STATEMENT) {
                    allCoverageLines.add(line);
                    methodData.addLine(line);
                } else if (coverage == COVERAGE.FUNCTION) {
                    String functionName = line.split(" ")[0];
                    allCoverageLines.add(functionName);
                    methodData.addLine(functionName);
                }
            }
            methodData.reset();
            methodList.add(methodData);
        }
        return methodList;
    }

    private void setAllLines(final File inputTestFolder) {
        if (inputTestFolder == null) {
            throw new RuntimeException("sootOutput is missing some required classes.");
        }
        for (final File fileEntry : inputTestFolder.listFiles()) {
            if (fileEntry.isFile() && !fileEntry.getName().startsWith(".") && !fileEntry.isHidden()) {
                BufferedReader br;
                try {
                    if (origOrderList.contains(fileEntry.getName())) {
                        br = new BufferedReader(new FileReader(fileEntry));
                        Set<String> lines = new HashSet<String>();

                        String line;
                        while ((line = br.readLine()) != null) {
                            lines.add(line);
                        }
                        testToAllLines.put(fileEntry.getName(), lines);                    	
                        br.close();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                continue;
            }
        }
    }

    protected void parseOrigOrderListToMethodList(List<String> origOrder,
            Map<String, TestFunctionStatement> nameToMethodData) {
        methodList.clear();
        for (String line : origOrder) {
            if (nameToMethodData.containsKey(line)) {
                methodList.add(nameToMethodData.get(line));
            }
        }
    }

    protected Map<String, TestFunctionStatement> getNameToMethodData(List<TestFunctionStatement> methodList) {
        Map<String, TestFunctionStatement> nameToMethodData = new HashMap<String, TestFunctionStatement>();
        for (TestFunctionStatement methodData : methodList) {
            nameToMethodData.put(methodData.getName(), methodData);
        }
        return nameToMethodData;
    }

    public void printResults() {
        orderObj.applyDeps();
        orderObj.printResults();
    }

    public List<TestFunctionStatement> getResults(int machine) {
        orderObj.applyDeps();
        return orderObj.getMethodList();
    }

    public List<TestFunctionStatement> getCoverage(int machine) {
        orderObj.applyDeps();
        return orderObj.getCoverage(false);
    }
}
