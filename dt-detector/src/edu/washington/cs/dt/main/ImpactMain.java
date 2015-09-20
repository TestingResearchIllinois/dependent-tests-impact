/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam
 */
package edu.washington.cs.dt.main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import edu.washington.cs.dt.TestExecResults;
import edu.washington.cs.dt.runners.AbstractTestRunner;
import edu.washington.cs.dt.runners.FixedOrderRunner;

public class ImpactMain {

    public static boolean useTimer = false;

    public static void main(String[] args) {
        // list to parse the arguments
        List<String> argsList = new ArrayList<String>(Arrays.asList(args));

        int inputTestListIndex = argsList.indexOf("-inputTests");
        List<String> tests = new LinkedList<String>();
        if (inputTestListIndex != 1) {
            int inputTestList = inputTestListIndex + 1;
            if (inputTestList >= argsList.size()) {
                System.err.println("Original order argument is specified but a"
                        + " directory path is not. Please use the format: -origOrder afilepath");
                System.exit(0);
            }

            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(argsList.get(inputTestList)));
                String line = br.readLine();

                while (line != null) {
                    if (!line.equals("")) {
                        tests.add(line);
                    }
                    line = br.readLine();
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else {
            System.err
            .println("No input test list is specified. Please use the format"
                    + " -inputTest atestlistfile");
            System.exit(0);
        }


        int timeIndex = argsList.indexOf("-getTime");
        if (timeIndex != -1) {
            useTimer = true;
        }

        AbstractTestRunner runner = new FixedOrderRunner(tests);
        long start = System.nanoTime();
        TestExecResults results = runner.run();
        long total = System.nanoTime() - start;
        System.out.println("Execution time: " + total);
        System.out.println(results);
    }

    public static TestExecResults getResults(List<String> tests) {
        AbstractTestRunner runner = new FixedOrderRunner(tests);
        return runner.run();
    }
}