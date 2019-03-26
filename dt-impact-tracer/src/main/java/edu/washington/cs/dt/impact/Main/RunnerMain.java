package edu.washington.cs.dt.impact.Main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.reedoei.testrunner.data.framework.TestFramework;
import com.reedoei.testrunner.data.results.Result;
import com.reedoei.testrunner.data.results.TestRunResult;
import com.reedoei.testrunner.runner.SmartRunner;
import com.reedoei.testrunner.runner.TestInfoStore;

import edu.washington.cs.dt.main.ImpactMain;

/*
 * A main function class that is meant to replace ImpactMain and allow commandline calls using testrunner
 * Most of the logic is taken directly from ImpactMain, mainly the argument parsing; main difference is using the testrunner
 */
public class RunnerMain {

    public static boolean useTimer = false;
    public static boolean skipMissingTests = false;
    public static boolean runSeparately = false;

    public static void main(String[] args) {
        // list to parse the arguments
        List<String> argsList = new ArrayList<String>(Arrays.asList(args));

        int inputTestListIndex = argsList.indexOf("-inputTests");
        List<String> tests = new LinkedList<String>();
        if (inputTestListIndex != -1) {
            int inputTestList = inputTestListIndex + 1;
            if (inputTestList >= argsList.size()) {
                System.err.println("Input test list argument is specified but a"
                        + " file path is not. Please use the format: -inputTests afilepath");
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
            System.err.println("No input test list is specified. Please use the format" + " -inputTests atestlistfile");
            System.exit(0);
        }

        int timeIndex = argsList.indexOf("-getTime");
        if (timeIndex != -1) {
            useTimer = true;
        }

        boolean randomize = argsList.contains("-randomize");
        skipMissingTests = argsList.contains("-skipMissingTests");
        runSeparately = argsList.contains("-separate");

        int classpathIndex = argsList.indexOf("-classpath");
        String classpath = System.getProperty("java.class.path");
        if (classpathIndex != -1) {
            if (classpathIndex + 1 < argsList.size()) {
                classpath = ImpactMain.buildClassPath(argsList.get(classpathIndex + 1).split(":"));
            }
        }

        // TODO: Allow handling of randomize option; for now just run in fixed order
        SmartRunner runner = new SmartRunner(TestFramework.junitTestFramework(), new TestInfoStore(), classpath, new HashMap<String, String>(), Paths.get("/dev/null"));
        Configuration.config().setDefault("testplugin.classpath", "");
        /*if (randomize) {
            runner = new RandomOrderRunner(classpath, tests);
        } else {
            runner = new FixedOrderRunner(classpath, tests);
        }*/

        long start = System.nanoTime();
        //TestExecResults results = runner.run();
        TestRunResult result = runner.runListWithCp(classpath, tests).get();
        long total = System.nanoTime() - start;
        System.out.println("Total execution time: " + total);

        // Print out the result in the format as expected by running ImpactMain
        // Assume only one run in one JVM of all tests
        StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("line.separator"));
        // Compute passing, failing, error, skipped, and ignored tests
        int passing = 0;
        int failing = 0;
        int error = 0;
        int skipped = 0;
        int ignored = 0;
        for (String test : result.results().keySet()) {
            switch (result.results().get(test).result()) {
                case PASS:
                    passing++;
                    break;
                case FAILURE:
                    failing++;
                    break;
                case ERROR:
                    error++;
                    break;
                case SKIPPED:
                    skipped++;
                    break;
            }
        }
        sb.append("Pass: " + passing + ", Fail: " + failing + ", Error: " + error + ", Skipped: " + skipped + ", Ignored: " + ignored);
        sb.append(System.getProperty("line.separator"));
        sb.append(result.toString());
        sb.append(System.getProperty("line.separator"));
        System.out.println(sb.toString());
    }
}
