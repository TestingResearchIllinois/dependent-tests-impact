/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam
 *
 *         Data class representing a test case.
 */

package edu.washington.cs.dt.impact.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

public class TestFunctionStatement extends Observable implements Comparable<TestFunctionStatement>, Observer {

    private String methodName;
    // all lines that this test method contains
    private final Set<String> allLines;
    // current lines of this test method that have yet to be covered
    private final Set<String> currentLines;

    // lines belonging to this test's and its dependencies'
    private final LinkedHashSet<Set<String>> setOfCurrentLines;
    // tests that this test depends on
    private final Set<TestFunctionStatement> observers;

    private boolean mergeDTCoverage;

    // list of tests that when executed before reveals methodName as a dependent test
    protected Set<TestFunctionStatement> execBefore;
    // list of tests that when executed after reveals methodName as a dependent test
    protected Set<TestFunctionStatement> execAfter;

    // Position of test in original order, to be used in comparing for sorting
    protected int origOrderIndex;

    public TestFunctionStatement(String name) {
        this.methodName = name;
        currentLines = new LinkedHashSet<String>();
        allLines = new LinkedHashSet<String>();
        execBefore = new HashSet<TestFunctionStatement>();
        execAfter = new HashSet<TestFunctionStatement>();
        observers = new HashSet<TestFunctionStatement>();
        setOfCurrentLines = new LinkedHashSet<Set<String>>();
        setOfCurrentLines.add(currentLines);
        mergeDTCoverage = true;
    }

    public void setOrigOrderIndex(int origOrderIndex) {
        this.origOrderIndex = origOrderIndex;
    }

    public int getOrigOrderIndex() {
        return this.origOrderIndex;
    }

    /**
     * @param isBefore
     *            true if tmd reveals this test as a dependent test when executed before this test
     *            false if tmd reveals this test as a dependent test when executed after this test.
     */
    public void addDependentTest(TestFunctionStatement tmd, boolean isBefore) {
        if (tmd != null) {
            tmd.customAddObserver(this);
            setChanged();
            notifyObservers(new HashSet<Object>());
            if (isBefore) {
                execBefore.add(tmd);
            } else {
                execAfter.add(tmd);
                // Only update setOfCurrentLines that modifies weight for after,
                // since later the logic only uses after (for ease)
                setOfCurrentLines.addAll(tmd.getTests());
            }
        }
    }

    public void customAddObserver(TestFunctionStatement tmd) {
        observers.add(tmd);
        this.addObserver(tmd);
    }

    public Set<TestFunctionStatement> getObservers() {
        return observers;
    }

    public Set<TestFunctionStatement> getDependentTests(boolean isBefore) {
        if (isBefore) {
            return execBefore;
        } else {
            return execAfter;
        }
    }

    public long getLineCount() {
        if (mergeDTCoverage) {
            Set<String> lines = new HashSet<String>();
            for (Set<String> s : setOfCurrentLines) {
                lines.addAll(s);
            }
            return lines.size();
        } else {
            return setOfCurrentLines.iterator().next().size();
        }
    }

    public String getName() {
        return methodName;
    }

    public void addLine(String line) {
        allLines.add(line);
    }

    public void reset() {
        currentLines.clear();
        currentLines.addAll(allLines);
    }

    public void removeLines(Set<String> lines) {
        currentLines.removeAll(lines);
    }

    public void retainLines(Set<String> lines) {
        // currentLines.retainAll(lines);
        Set<String> newLines = new HashSet<String>();
        for (String line : currentLines) {
            if (lines.contains(line)) {
                newLines.add(line);
            }
        }
        currentLines.clear();
        currentLines.addAll(newLines);
    }

    public Set<String> getLines() {
        if (mergeDTCoverage) {
            Set<String> lines = new HashSet<String>();
            for (Set<String> s : setOfCurrentLines) {
                lines.addAll(s);
            }
            return Collections.unmodifiableSet(lines);
        } else {
            return setOfCurrentLines.iterator().next();
        }
    }

    public Set<Set<String>> getTests() {
        return setOfCurrentLines;
    }

    public void setName(String name) {
        this.methodName = name;
    }

    public void setMergeDTCoverage(boolean mergeDTCoverage) {
        this.mergeDTCoverage = mergeDTCoverage;
    }

    @Override
    public boolean equals(Object obj) {
	   if (obj == null) {
	        return false;
	    }
	    if (!TestFunctionStatement.class.isAssignableFrom(obj.getClass())) {
	        return false;
	    }
	    final TestFunctionStatement other = (TestFunctionStatement) obj;
	    if ((this.getName() == null) ? (other.getName() != null) : !this.getName().equals(other.getName())) {
	        return false;
	    }
	    return true;
    }

    @Override
    public String toString() {
        return getName() + " : " + getLineCount();
    }

    @Override
    public int compareTo(TestFunctionStatement o) {
        long mLineCount = getLineCount();
        long oLineCount = o.getLineCount();

        if (mLineCount < oLineCount) {
            return 1;
        } else if (mLineCount > oLineCount) {
            return -1;
        } else {
            // Sort based on their position in the original order
            // Subtract the two indices to get right sign of value
            return getOrigOrderIndex() - o.getOrigOrderIndex();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update(Observable o, Object arg) {
        if (!(arg instanceof HashSet<?>)) {
            throw new RuntimeException("Observer received incompatible arg.");
        }

        if (!(o instanceof TestFunctionStatement)) {
            throw new RuntimeException("Observer received incompatible o.");
        }

        Set<Object> newArgs = (HashSet<Object>) arg;
        TestFunctionStatement tmd = (TestFunctionStatement) o;
        if (!newArgs.contains(o)) {
            setOfCurrentLines.addAll(tmd.getTests());
            newArgs.add(o);
            setChanged();
            notifyObservers(newArgs);
        }
    }
}
