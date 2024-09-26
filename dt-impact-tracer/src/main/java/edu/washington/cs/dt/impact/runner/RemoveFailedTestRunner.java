package edu.washington.cs.dt.impact.runner;

import com.google.common.base.Preconditions;
import com.reedoei.eunomia.util.StandardMain;
import edu.washington.cs.dt.RESULT;
import edu.washington.cs.dt.TestExecResults;
import edu.washington.cs.dt.main.ImpactMain;
import edu.washington.cs.dt.runners.FixedOrderRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemoveFailedTestRunner extends StandardMain {
    private RemoveFailedTestRunner(String[] args) {
        super(args);
    }

    public static void main(final String[] args) throws Exception {
        new RemoveFailedTestRunner(args).run();
    }

    @Override
    protected void run() throws Exception {
        final String classPath = getArg("classpath")
                .map(s -> ImpactMain.buildClassPath(s.split(":")))
                .orElse(System.getProperty("java.class.path"));
        Preconditions.checkArgument(Arrays.stream(classPath.split(":")).allMatch(s -> Files.exists(Paths.get(s))),
                "Some directories or files in the classpath do not exist!");

        final List<String> testOrder = Files.readAllLines(Paths.get(getArgRequired("testOrder")));
        final TestExecResults results = new FixedOrderRunner(classPath, testOrder).run();

        final Path outputFile = Paths.get(getArg("output").orElse("failing-tests.txt"));

        final Map<String, RESULT> failingTests = results.getExecutionRecords().get(0).getNameToResultsMap();

        Files.write(outputFile,
                failingTests.entrySet().stream()
                .filter(e -> !e.getValue().equals(RESULT.PASS))
                .map(Map.Entry::getKey)
                .collect(Collectors.joining("\n"))
                .getBytes());
    }
}
