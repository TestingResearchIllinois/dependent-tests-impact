package edu.washington.cs.dt.impact.tools.detectors;

import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.io.capture.CaptureOutStream;
import com.reedoei.eunomia.io.capture.CapturedOutput;
import com.reedoei.eunomia.io.files.FileUtil;
import edu.washington.cs.dt.TestExecResultsDelta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class Detector {
    protected final String classpath;
    protected List<String> tests;

    private int rounds;

    public Detector(final String classpath, final List<String> tests, final int rounds) {
        this.classpath = classpath;
        this.tests = tests;
        this.rounds = rounds;
    }

    public abstract List<TestExecResultsDelta> detectionRound();

    public List<String> newTests(final List<TestExecResultsDelta> roundResult) {
//        for (final TestExecResultsDelta delta : roundResult) {
//            tests.remove(delta.testName);
//        }

        return tests;
    }

    public List<TestExecResultsDelta> detect() {
        final List<TestExecResultsDelta> result = new ArrayList<>();

        final long origStartTimeMs = System.currentTimeMillis();
        long startTimeMs = System.currentTimeMillis();

        int i = 0;
        while (i < rounds) {
            final CapturedOutput<List<TestExecResultsDelta>> run = new CaptureOutStream<>(this::detectionRound).run();

            if (run.hadError()) {
                System.out.printf("\rRun %d had an error. Full log is shown below:\n", i);
                System.out.println(run.stringOutput());
            } else {
                final List<TestExecResultsDelta> currentRoundResult = run.valueRequired();
                // Make sure we don't get duplicates.
                for (final TestExecResultsDelta delta : result) {
                    currentRoundResult.removeIf(curDelta -> delta.testName.equals(curDelta.testName));

                    if (currentRoundResult.isEmpty()) {
                        break;
                    }
                }

                final double elapsed = System.currentTimeMillis() - startTimeMs;
                final double totalElapsed = (System.currentTimeMillis() - origStartTimeMs) / 1000.0;
                final long estimate = (long) (elapsed / (i + 1) * (rounds - i - 1) / 1000);

                if (!currentRoundResult.isEmpty()) {
                    System.out.printf("\r[INFO] Found %d tests in round %d of %d (%.1f seconds elapsed (%.1f total), %d seconds remaining).\n", currentRoundResult.size(), i, rounds, elapsed / 1000, totalElapsed, estimate);
                    result.addAll(currentRoundResult);
                    i = 0;
                    startTimeMs = System.currentTimeMillis();
                } else {
                    System.out.printf("\r[INFO] Found %d tests in round %d of %d (%.1f seconds elapsed (%.1f total), %d seconds remaining)", currentRoundResult.size(), i, rounds, elapsed / 1000, totalElapsed, estimate);
                    i++;
                }

                tests = newTests(currentRoundResult);
            }
        }
        System.out.println();

        return result;
    }

    public void writeTo(final Path dir) throws IOException {
        FileUtil.makeDirectoryDestructive(dir);

        final Path listPath = dir.resolve("list.txt");
        final Path dtListPath = dir.resolve("dt-lists.txt");

        final List<TestExecResultsDelta> detect = detect();

        System.out.printf("[INFO] Found %d tests, writing list to %s and dt lists to %s\n", detect.size(), listPath, dtListPath);

        Files.write(dtListPath, String.join(System.lineSeparator(), ListUtil.map(TestExecResultsDelta::toString, detect)).getBytes());
        Files.write(listPath, String.join(System.lineSeparator(), ListUtil.map(t -> t.testName, detect)).getBytes());
    }
}
