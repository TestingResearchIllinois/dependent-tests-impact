package edu.washington.cs.dt.impact.tools.minimizer;

import com.google.gson.Gson;
import com.reedoei.eunomia.collections.ListEx;
import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.data.caching.FileCache;
import com.reedoei.eunomia.io.files.FileUtil;
import com.reedoei.eunomia.util.RuntimeThrower;
import com.reedoei.eunomia.util.Util;
import edu.illinois.cs.testrunner.data.results.Result;
import edu.illinois.cs.testrunner.data.results.TestRunResult;
import edu.illinois.cs.testrunner.mavenplugin.TestPluginPlugin;
import edu.illinois.cs.testrunner.runner.SmartRunner;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestMinimizer {
    protected final List<String> testOrder;
    protected final String dependentTest;
    protected final Result expected;
    protected final Result isolationResult;
    protected final SmartRunner runner;

    protected TestRunResult expectedRun;

    public TestMinimizer(final List<String> testOrder, final SmartRunner runner, final String dependentTest) {
        // Only take the tests that come before the dependent test
        this.testOrder = testOrder.contains(dependentTest) ? ListUtil.before(testOrder, dependentTest) : testOrder;
        this.dependentTest = dependentTest;

        this.runner = runner;

        // Run in given order to determine what the result should be.
        this.expectedRun = runResult(testOrder);
        this.expected = expectedRun.results().get(dependentTest).result();
        this.isolationResult = result(Collections.singletonList(dependentTest));
    }

    public Result expected() {
        return expected;
    }

    private TestRunResult runResult(final List<String> order) {
        final List<String> actualOrder = new ArrayList<>(order);

        if (!actualOrder.contains(dependentTest)) {
            actualOrder.add(dependentTest);
        }

        return runner.runList(actualOrder).get();
    }

    private Result result(final List<String> order) {
        return runResult(order).results().get(dependentTest).result();
    }

    public MinimizeTestsResult run() {
        final List<String> order =
                testOrder.contains(dependentTest) ? ListUtil.beforeInc(testOrder, dependentTest) : new ArrayList<>(testOrder);

        // Run the single test 10 times to be more confident that the test is deterministic in its result
        Result isolationResult = result(Collections.singletonList(dependentTest));
        for (int i = 0; i < 9; i++) {
            final Result r = result(Collections.singletonList(dependentTest));
            // Result does not match, so return a form that is NOD
            if (r != isolationResult) {
                return new MinimizeTestsResult(expectedRun, expected, dependentTest, new ArrayList<PolluterData>(), FlakyClass.NOD);
            }
        }

        // Keep going as long as there are tests besides dependent test to run
        List<PolluterData> polluters = new ArrayList<>();
        int index = 0;
        while (!order.isEmpty()) {
            // First need to check if remaining tests in order still lead to expected value
            if (result(order) != expected) {
                break;
            }

            final List<String> deps = run(new ArrayList<>(order));
            if (deps.isEmpty()) {
                break;
            }

            polluters.add(new PolluterData(index, deps));

            order.removeAll(deps);  // Look for other deps besides the ones already found
            index++;
        }
        /*for (int i = 0; i < order.size(); i++) {
            String test = order.get(i);
            final Result r = result(Collections.singletonList(test));
            if (r == expected) {
                polluters.add(new PolluterData(index, Collections.singletonList(test)));
                index++;
            }
        }*/

        final MinimizeTestsResult minimizedResult =
                new MinimizeTestsResult(expectedRun, expected, dependentTest, polluters, FlakyClass.OD);

        // If the verifying does not work, then mark this test as NOD
        /*boolean verifyStatus = minimizedResult.verify(runner);
        if (verifyStatus) {
            return minimizedResult;
        } else {
            return new MinimizeTestsResult(expectedRun, expected, dependentTest, polluters, FlakyClass.NOD);
        }*/
        return minimizedResult;
    }

    private List<String> deltaDebug(final List<String> deps, int n) {
        // If n granularity is greater than number of tests, then finished, simply return passed in tests
        if (deps.size() < n) {
            return deps;
        }

        // Cut the tests into n equal chunks and try each chunk
        int chunkSize = (int)Math.round((double)(deps.size()) / n);
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < deps.size(); i += chunkSize) {
            List<String> chunk = new ArrayList<>();
            List<String> otherChunk = new ArrayList<>();
            // Create chunk starting at this iteration
            int endpoint = Math.min(deps.size(), i + chunkSize);
            chunk.addAll(deps.subList(i, endpoint));

            // Complement chunk are tests before and after this current chunk
            otherChunk.addAll(deps.subList(0, i));
            otherChunk.addAll(deps.subList(endpoint, deps.size()));

            // Try to other, complement chunk first, with theory that polluter is close to victim
            if (this.expected == result(otherChunk)) {
                return deltaDebug(otherChunk, 2);   // If works, then delta debug some more the complement chunk
            }
            // Check if running this chunk works
            if (this.expected == result(chunk)) {
                return deltaDebug(chunk, 2); // If works, then delta debug some more this chunk
            }
        }
        // If size is equal to number of ochunks, we are finished, cannot go down more
        if (deps.size() == n) {
            return deps;
        }
        // If not chunk/complement work, increase granularity and try again
        if (deps.size() < n * 2) {
            return deltaDebug(deps, deps.size());
        } else {
            return deltaDebug(deps, n * 2);
        }
    }

    private List<String> run(List<String> order) {
        final List<String> deps = new ArrayList<>();

        if (order.isEmpty()) {
            return deps;
        }

        deps.addAll(deltaDebug(order, 2));

        return deps;
    }

    private boolean tryIsolated(final List<String> deps, final List<String> order) {
        if (isolationResult.equals(expected)) {
            deps.clear();
            return true;
        }

        for (int i = 0; i < order.size(); i++) {
            String test = order.get(i);

            final Result r = result(Collections.singletonList(test));

            // Found an order where we get the expected result with just one test, can't be more
            // minimal than this.
            if (r == expected) {
                deps.add(test);
                return true;
            }
        }

        return false;
    }

    private List<String> runSequential(final List<String> deps, final List<String> testOrder) {
        final List<String> remainingTests = new ArrayList<>(testOrder);

        while (!remainingTests.isEmpty()) {
            final String current = remainingTests.remove(0);

            final List<String> order = Util.prependAll(deps, remainingTests);
            final Result r = result(order);

            if (r != expected) {
                deps.add(current);
            }
        }

        return deps;
    }

    public String getDependentTest() {
        return dependentTest;
    }
}
