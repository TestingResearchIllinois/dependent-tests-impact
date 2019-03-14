/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam
 * 
 * Takes two file arguments and determines whether the test results
 * in the two files are the same. A file indicating the number of
 * inconsistent tests, missing tests and additional tests in the two
 * files are generated.
 */

package edu.washington.cs.dt.impact.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.reedoei.testrunner.data.results.Result;

public class CrossReferencer {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // list to parse the arguments
        List<String> argsList = new ArrayList<String>(Arrays.asList(args));

        // get directory for the input of text files
        int file1Index = argsList.indexOf("-origOrder");
        File file1 = null;
        if (file1Index != -1) {
            // get index of output directory
            int file1NameIndex = file1Index + 1;
            if (file1NameIndex >= argsList.size()) {
                System.err.println("Original order argument is specified but a file name is not."
                        + " Please use the format: -origOrder aFileName");
                System.exit(0);
            }
            file1 = new File(argsList.get(file1NameIndex));
            if (!file1.isFile()) {
                System.err.println("Original order argument is specified but the file path is"
                        + " invalid. Please check the file path.");
                System.exit(0);
            }
        } else {
            System.err.println("No original order argument is specified."
                    + " Please use the format: -origOrder aFileName");
            System.exit(0);
        }

        // get directory for the input of text files
        int file2Index = argsList.indexOf("-testOrder");
        File file2 = null;
        if (file2Index != -1) {
            // get index of output directory
            int file2NameIndex = file2Index + 1;
            if (file2NameIndex >= argsList.size()) {
                System.err.println("Test order argument is specified but a file name is not."
                        + " Please use the format: -testOrder aFileName");
                System.exit(0);
            }
            file2 = new File(argsList.get(file2NameIndex));
            if (!file2.isFile()) {
                System.err.println("Test order argument is specified"
                        + " but the file path is invalid. Please check the file path.");
                System.exit(0);
            }
        } else {
            System.err.println("No test order argument is specified."
                    + " Please use the format: -testOrder aFileName");
            System.exit(0);
        }

        compareResults(processFile(file1), processFile(file2), true);
    }

    public static Set<String> compareResults(Map<String, Result> origOrderResults,
            Map<String, Result> testOrderResults, boolean printResults) {
        Set<String> changedTests = new HashSet<String>();
        Set<String> origOrderResultsCopy = new HashSet<String>(origOrderResults.keySet());
        Set<String> testOrderOnly = new HashSet<String>();

        int longestKey = 0;
        for (String key : testOrderResults.keySet()) {
            if (origOrderResults.containsKey(key)) {
                origOrderResultsCopy.remove(key);

                if (origOrderResults.get(key) != testOrderResults.get(key)) {
                    changedTests.add(key);

                    if (key.length() > longestKey) {
                        longestKey = key.length();
                    }
                }
            } else {
                testOrderOnly.add(key);
            }
        }

        for (String key : origOrderResultsCopy) {
            if (key.length() > longestKey) {
                longestKey = key.length();
            }
        }

        if (printResults) {
            System.out.println("Number of Inconsistent Tests Found: " + changedTests.size());
            System.out.println("Number of additional tests: " + testOrderOnly.size());
            System.out.println("Number of Tests Missing: " + origOrderResultsCopy.size());

            char[] spaces = new char[longestKey];
            Arrays.fill(spaces, ' ');
            System.out.println(
                    new String(spaces) + "  Original order result:        Test order result:");

            char[] columnSpaces = new char[26];
            Arrays.fill(columnSpaces, ' ');
            String columnSpaceString = new String(columnSpaces);

            for (String key : changedTests) {
                spaces = new char[longestKey - key.length()];
                Arrays.fill(spaces, ' ');
                System.out.println(key + ": " + new String(spaces) + origOrderResults.get(key)
                        + columnSpaceString + testOrderResults.get(key));
            }

            for (String key : testOrderOnly) {
                spaces = new char[longestKey - key.length() + 4 + columnSpaceString.length()];
                Arrays.fill(spaces, ' ');
                System.out.println(key + ": " + new String(spaces) + testOrderResults.get(key));
            }

            for (String key : origOrderResultsCopy) {
                spaces = new char[longestKey - key.length()];
                Arrays.fill(spaces, ' ');
                System.out.println(key + ": " + new String(spaces) + origOrderResults.get(key));
            }
        }
        return changedTests;
    }

    private static Map<String, Result> processFile(File f) {
        if (!f.isFile()) {
            throw new RuntimeException(f.getName() + " file is invalid.");
        }

        Map<String, Result> testsToResults = new HashMap<>();
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(f));
            String line = br.readLine();
            while (line != null) {
                while (line != null
                        && !line.matches("^Pass: [0-9]+, Fail: [0-9]+, Error: [0-9]+.*")) {
                    line = br.readLine();
                }

                if (line == null) {
                    break;
                }

                String nextLine = br.readLine();
                if (nextLine.equals("{}")) {
                    line = nextLine;
                    continue;
                }

                String[] testResults = nextLine.split(", ");
                if (testResults.length >= 1) {
                    testResults[0] = testResults[0].substring(1);
                    String lastTest = testResults[testResults.length - 1];
                    testResults[testResults.length - 1] = lastTest.substring(0,
                            lastTest.length() - 1);

                    for (String s : testResults) {
                        String[] testAndResult = s.split("=");
                        testsToResults.put(testAndResult[0], FileTools.getResultFromString(testAndResult[1]));
                    }
                }
                line = br.readLine();
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return testsToResults;
    }
}
