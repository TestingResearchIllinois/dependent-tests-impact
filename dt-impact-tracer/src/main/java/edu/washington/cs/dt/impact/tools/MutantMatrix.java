package edu.washington.cs.dt.impact.tools;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.functional.Func;
import com.reedoei.eunomia.util.Util;
import edu.washington.cs.dt.impact.order.Standard;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MutantMatrix {
    private static final int MUTANT_GROUP_SIZE = 5;
    private static final int MUTANT_GROUP_COUNT = 100;

    private final List<Mutation> mutations;

    public static MutantMatrix fromPath(final Path mutationsPath) throws IOException, DocumentException {
        if (mutationsPath.endsWith("xml")) {
            return new MutantMatrix(mutationsPath);
        } else {
            return new MutantMatrix(getMutationsPath(mutationsPath));
        }
    }

    private static Path getMutationsPath(final Path parent) throws IOException {
        return Files.list(parent)
                .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                .map(p -> p.resolve("mutations.xml"))
                .orElseThrow(() -> new IOException("No mutations.xml found in path: " + parent));
    }

    private MutantMatrix(final Path mutationsPath)
            throws MalformedURLException, DocumentException {
        this(Util.readXmlDoc(mutationsPath.toFile()));
    }

    public MutantMatrix(final Document pitOutput) {
        this.mutations =
                pitOutput.getRootElement().elements("mutation").stream()
                .filter(m -> m.attribute("status").getValue().equals("KILLED"))
                .map(Mutation::new)
                .collect(Collectors.toList());
    }

    public List<MutationGroup> createMutationGroups() {
        return createMutationGroups(MUTANT_GROUP_SIZE);
    }

    public List<MutationGroup> createMutationGroups(final int groupSize) {
        return createMutationGroups(groupSize, MUTANT_GROUP_COUNT);
    }

    public List<MutationGroup> createMutationGroups(final int groupSize, final int groupCount) {
        // Select the groups randomly
        Collections.shuffle(mutations);

        final List<List<Mutation>> groups = Lists.partition(mutations, groupSize);

        final List<MutationGroup> mutationGroups =
                ListUtil.mapWithIndex(
                        MutationGroup::new,
                        groups.subList(0, Math.min(groupCount, groups.size())));
        Preconditions.checkState(!mutationGroups.isEmpty(), "No mutation groups generated!");
        return mutationGroups;
    }

    /**
     * @param s The fully qualified name in the format package.class.method(package.class)
     * @return The fully qualified name in the format package.class.method
     */
    private static String formatQName(final String s) {
        final String[] parts = s.split("\\(");
        return parts[0];
    }

    public static class MutationGroup {
        private final int index;
        private final List<Mutation> mutations;

        public MutationGroup(final int index, final List<Mutation> mutations) {
            this.index = index;
            this.mutations = mutations;
        }

        public Optional<String> firstKillingTest(final List<String> tests) {
            return tests.stream().filter(this::coveredBy).findFirst();
        }

        public boolean coveredBy(final String test) {
            return mutations.stream().anyMatch(m -> m.coveredBy(test));
        }

        private List<Double> coverage(final List<String> testOrder) {
            final List<Mutation> mutationListCopy = new ArrayList<>(mutations);

            final List<Double> result = new ArrayList<>();

            for (final String test : testOrder) {
                final int sizeBefore = mutationListCopy.size();
                mutationListCopy.removeIf(m -> m.coveredBy(test));
                final int sizeAfter = mutationListCopy.size();

                final double coverage = (sizeBefore - sizeAfter) / (double)mutations.size();
                result.add(coverage);
            }

            return result;
        }

        public List<String> showCoverage(final List<String> testOrder) {
            return coverage(testOrder).stream()
                    .map(Standard::formatPercent)
                    .collect(Collectors.toList());
        }

        public int getIndex() {
            return index;
        }

        @Override
        public String toString() {
            return mutations.toString();
        }
    }

    public static class Mutation {
        private final String mutator;
        private final String lineNumber;
        private final String index;
        private final List<String> killingTests;

        public Mutation(final Element mutation) {
            this.mutator = mutation.element("mutator").getText();
            this.lineNumber = mutation.element("lineNumber").getText();
            this.index = mutation.element("index").getText();

            this.killingTests =
                    Arrays.stream(mutation.element("killingTest").getText().split(","))
                    .map(MutantMatrix::formatQName)
                    .collect(Collectors.toList());
        }

        public Mutation(final String mutator,
                        final String lineNumber,
                        final String index,
                        final List<String> killingTests) {
            this.mutator = mutator;
            this.lineNumber = lineNumber;
            this.index = index;
            this.killingTests = killingTests;
        }

        @Override
        public String toString() {
            return mutator + "-" + lineNumber + "-" + index;
        }

        public boolean coveredBy(final String test) {
            return killingTests.contains(test);
        }
    }
}
