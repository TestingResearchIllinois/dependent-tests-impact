package edu.washington.cs.dt.impact.data;

import com.reedoei.testrunner.data.results.Result;

/**
 * Created by winglam on 7/1/18.
 *
 * This class is meant to contain the time a dependent test ran in isolation and the test execution result of a
 * dependent test in isolation.
 */
public class TestInfo {
    private final long time;
    private final Result result;

    public TestInfo(long time, Result result) {
        this.result = result;
        this.time = time;
    }

    public long getTime() {
        return time;
    }
    public Result getResult() {
        return result;
    }
}
