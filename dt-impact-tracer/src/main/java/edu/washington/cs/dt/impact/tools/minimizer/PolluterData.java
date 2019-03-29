package edu.washington.cs.dt.impact.tools.minimizer;

import java.util.ArrayList;
import java.util.List;

public class PolluterData {
    private final int index;            // The index of when this polluter was found (0 is first)
    private final List<String> deps;

    public PolluterData(final int index, final List<String> deps) {
        this.index = index;
        this.deps = deps;
    }

    public int index() {
        return index;
    }

    public List<String> deps() {
        return deps;
    }

    public List<String> withDeps(final String dependentTest) {
        final List<String> order = new ArrayList<>(deps);
        if (!order.contains(dependentTest)) {
            order.add(dependentTest);
        }
        return order;
    }
}
