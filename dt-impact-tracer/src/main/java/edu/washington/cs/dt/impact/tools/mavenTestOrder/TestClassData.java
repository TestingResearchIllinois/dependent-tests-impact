package edu.washington.cs.dt.impact.tools.mavenTestOrder;

import java.util.List;

/**
 * The following class is copied from the version
 * https://github.com/idflakies/iDFlakies/commit/b74d016aafcce12e22bc88ee4e3903ee0b01f2b4
 * Created by winglam on 4/12/19.
 */
public class TestClassData {
    public String className;
    public List<String> testNames;
    public double classTime;

    public TestClassData(String className, List<String> testNames, double classTime) {
        this.className = className;
        this.testNames = testNames;
        this.classTime = classTime;
    }

    public double classTime() {
        return classTime;
    }

    public String className() {
        return className;
    }

    public List<String> testNames() {
        return testNames;
    }
}
