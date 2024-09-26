package edu.washington.cs.dt.impact.tools;

import com.reedoei.eunomia.io.capture.CapturedOutput;
import com.reedoei.eunomia.io.capture.CaptureOutStream;
import edu.washington.cs.dt.TestExecResult;
import edu.washington.cs.dt.runners.FixedOrderRunner;

import java.util.List;

public class SilentRunner {
    private final String classpath;
    private final List<String> tests;

    public SilentRunner(final String classpath, final List<String> tests) {
        this.classpath = classpath;
        this.tests = tests;
    }

    public TestExecResult run() {
        final CapturedOutput<TestExecResult> run =
                new CaptureOutStream<>(() -> new FixedOrderRunner(classpath, tests).run().getExecutionRecords().get(0)).run();

        if (run.hadError()) {
            System.out.println("\r[ERROR] Encountered an error while running an order. Full log is shown below:");
            System.out.println(run.stringOutput());
            throw new RuntimeException(run.error());
        } else {
            return run.valueRequired();
        }
    }
}
