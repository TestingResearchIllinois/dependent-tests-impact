package edu.washington.cs.dt.impact.tools.detectors;

import com.reedoei.eunomia.util.StandardMain;

import com.reedoei.testrunner.configuration.Configuration;
import com.reedoei.testrunner.data.framework.TestFramework;
import com.reedoei.testrunner.data.results.Result;
import com.reedoei.testrunner.data.results.TestResult;
import com.reedoei.testrunner.data.results.TestRunResult;
import com.reedoei.testrunner.runner.Runner;
import com.reedoei.testrunner.runner.RunnerFactory;
import com.reedoei.testrunner.runner.SmartRunner;
import com.reedoei.testrunner.runner.TestInfoStore;

import edu.washington.cs.dt.main.ImpactMain;
import org.apache.commons.io.FileUtils;
import org.apache.maven.project.MavenProject;
import scala.Option;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class FailingTestDetector extends StandardMain {
    private final String classpath;
    private Path testList = null;
    private Path outputPath = null;
    private SmartRunner runner;

    public FailingTestDetector(final String[] args) throws IOException {
        super(args);

        this.classpath = getClasspathArg();
        this.testList = Paths.get(getArgRequired("tests"));
        this.outputPath = Paths.get(getArgRequired("output"));

        this.runner = new SmartRunner(TestFramework.junitTestFramework(), new TestInfoStore(), this.classpath, new HashMap<String, String>(), this.outputPath);
        Configuration.config().setDefault("testplugin.classpath", "");
    }

    public static void main(final String[] args) {
        try {
            new FailingTestDetector(args).run();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    @Override
    protected void run() throws Exception {
        output(notPassingTests(Files.readAllLines(testList, Charset.defaultCharset())));
    }

    private void output(final Set<String> tests) throws IOException {
        final Set<String> allOutput = new HashSet<>(tests);

        // Read the lines so we don't get rid of any tests
        // but don't just append to the file, use a set to keep track so there are no duplicates.
        if (Files.exists(outputPath)) {
            allOutput.addAll(Files.readAllLines(outputPath, Charset.defaultCharset()));
        }

        Files.write(outputPath, String.join(System.lineSeparator(), allOutput).getBytes());
    }

    public FailingTestDetector(String classpath) {
        super(new String[0]);
        this.classpath = classpath;
    }

    public Set<String> notPassingTests(List<String> tests) throws IOException {
        final Set<String> notPassingTests = new HashSet<>();

        ImpactMain.skipMissingTests = true;

        while (!tests.isEmpty()) {
            final TestRunResult result = runner.runListWithCp(classpath, tests).get();

            final int beforeSize = tests.size();
            notPassingTests.addAll(tests.stream().filter(t -> !result.results().get(t).result().equals(Result.PASS)).collect(Collectors.toList()));

            tests.removeAll(notPassingTests);
            final int afterSize = tests.size();

            if (beforeSize == afterSize) {
                break;
            }
        }

        return notPassingTests;
    }

    public static void outputFailedTests(MavenProject p, String testsPath, String outputPath){
        List<String> failedTestList = new ArrayList<>();

        // Runner Factory
        Option<Runner> from = RunnerFactory.from(p);
        if (from.isDefined()) {
            Runner runner = from.get();
            try {
                String encoding = null;
                List<String> tests = FileUtils.readLines(new File(testsPath), encoding);

                // Runner
                if (runner.runList(tests).isSuccess()){
                    TestRunResult result = runner.runList(tests).get();
                    Map<String, TestResult> resultsMap = result.results();

                    // Parse Results
                    for (Map.Entry<String, TestResult> entry : resultsMap.entrySet()) {
                        String key = entry.getKey();
                        TestResult value = entry.getValue();

                        // ERROR || FAILURE
                        if (value.result().equals(Result.ERROR) || value.result().equals(Result.FAILURE)){
                            failedTestList.add(key);
                        }
                    }
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        // Output "ERROR" || "FAILURE" Tests To File
        try {
            // Generates orig-order.txt
            FileWriter writer = new FileWriter(new File(outputPath));
            for(String str: failedTestList) {
                writer.write(str);
                writer.write(System.getProperty("line.separator"));
            }
            writer.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
