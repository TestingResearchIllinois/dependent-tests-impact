package edu.washington.cs.dt.impact.figure.generator;

import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.collections.MapUtil;
import com.reedoei.eunomia.collections.PairStream;
import com.reedoei.eunomia.io.files.FileUtil;
import com.reedoei.eunomia.math.Averager;
import com.reedoei.eunomia.string.matching.LineMatch;
import com.reedoei.eunomia.string.searching.Searcher;
import com.reedoei.eunomia.string.searching.StringSearch;
import com.reedoei.eunomia.util.OptUtil;

import com.reedoei.testrunner.data.results.Result;

import edu.washington.cs.dt.impact.data.EnhancedValues;
import edu.washington.cs.dt.impact.data.TestInfo;
import edu.washington.cs.dt.impact.util.Constants;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConfigurationData {
    public boolean load() {
//        System.out.println("[DEBUG] " + Paths.get("").toAbsolutePath().relativize(Paths.get(filename)));
        final Path enhancedPath = Paths.get(filename.replace("~", "GIVEN_TD"));
        final Path unenhancedPath = Paths.get(filename.replaceFirst("~", "OMITTED_TD"));

        final Path originalPath = originalPath(filename);

        try {
            if (Files.exists(enhancedPath) && Files.exists(unenhancedPath) && Files.exists(originalPath)) {
                System.out.println("[INFO] Loading " + Paths.get(filename).getFileName());

                final Data enhanced = parseFile(enhancedPath);
                final Data unenhanced = parseFile(unenhancedPath);

                data = new EnhancedValues<>(unenhanced, enhanced);

                final Data orig = parseFile(originalPath);
                origOrder(orig.testOrders.get(0).testOrder);

                if (enhancedOrderDifferent()) {
                    System.out.println("[INFO] Enhanced order is different for: " + Paths.get(filename).getFileName());

                    if (!unenhanced.containsDTs()) {
                        printCase("a");
                    } else if (unenhanced.containsDTs() && enhanced.containsDTs()) {
                        printCase("b");
                    } else {
                        printCase("c");
                    }

                    unenhanced.showRerunNeeded("unenhanced");
                    enhanced.showRerunNeeded("enhanced");
                }

                unenhanced.showFalsePositive();

                final Averager<Double> origTimeAverager = new Averager<>();
                origTimeAverager.addAll(enhanced.origTimes());
                origTimeAverager.addAll(unenhanced.origTimes());
                origTimeAverager.addAll(orig.origTimes());

                System.out.println("Average " + subjectName().replace(" ", "-") + " "  + testType() + " time: " + origTimeAverager.mean());

                System.out.println("[INFO] Successfully loaded.");

                return true;
            }
        } catch (IOException ignored) {}

        return false;
    }

    private void printCase(final String s) {
        final String techniqueName;

        if (technique == Constants.TECHNIQUE.PRIORITIZATION) {
            techniqueName = "prio";
        } else if (technique == Constants.TECHNIQUE.SELECTION) {
            techniqueName = "sele";
        } else {
            techniqueName = "para";
        }

        System.out.printf("case %s %s %s %s %s\n",
                s, techniqueName, testType(), subjectName(), speedup());
//                data.unenhanced().rerunType(), data.enhanced().rerunType());
    }

    private Path originalPath(final String filename) {
        final Path parent = Paths.get(filename).toAbsolutePath().getParent();

        final String justName = Paths.get(filename).getFileName().toString();
        final List<String> parts = ListUtil.fromArray(justName.split("-"));

        final String testType = parts.get(1);
        final String name = String.join("-", parts.subList(2, parts.size() - 4));

        return parent.resolve("PRIORITIZATION-" + testType + "-" + name + "-STATEMENT-ORIGINAL-CONTAINS_DT-OMITTED_TD.txt");
    }

    private Data parseFile(final Path path) throws IOException {
        final List<TestOrder> testOrders = new ArrayList<>();

        for (final String section : FileUtil.readFile(path).split("--------------------------")) {
            try {
                testOrders.add(parseTestOrder(section));
            } catch (Throwable ignored) {}
        }

        String flagsInFile = FigureGenerator.getFlagsLine(path.toFile(), Constants.ARGUMENT_STRING, false);

        // get rid of square brackets
        flagsInFile = flagsInFile.substring(1, flagsInFile.length() - 1);
        String[] flags = flagsInFile.split(",");
        final List<String> flagsList = Arrays.asList(flags);
        // get rid of whitespaces
        for (int i = 0; i < flagsList.size(); i++) {
            flagsList.set(i, flagsList.get(i).trim());
        }

        int index = flagsList.indexOf("-project");
        final String projectName = flagsList.get(index + 1);

        index = flagsList.indexOf("-testType");
        final String testType = flagsList.get(index + 1);

        return new Data(projectName, testType, testOrders);
    }

    private String nextLine(final StringSearch search, final String keyword) {
        return nextLineOpt(search, keyword).orElseThrow(() -> new IllegalStateException("Could not find: " + keyword));
    }

    private Optional<String> nextLineOpt(final StringSearch search, final String keyword) {
        return search.searchFirst(Searcher.exactly(keyword))
                .flatMap(LineMatch::nextLine)
                .map(LineMatch::toString);
    }

    private TestOrder parseTestOrder(final String section) {
        final StringSearch search = new StringSearch(section);

        final List<String> testOrder = ListUtil.read(nextLine(search, Constants.TEST_ORDER_LIST));
        final List<Long> testTimes = ListUtil.read(Long::parseLong, nextLine(search, Constants.TIME_STRING));

        final Map<String, Long> testTimesMap = PairStream.zip(testOrder, testTimes).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        final Map<String, Result> testResults = MapUtil.valueRead(Result::valueOf, nextLine(search, Constants.ALL_TEST_RESULTS));

        final Map<String, TestInfo> reorderInfo = FigureGenerator.convertMapsToIsolationDataMap(testResults, testTimesMap);

        final Map<String, Result> origResults = MapUtil.valueRead(Result::valueOf, nextLine(search, Constants.ORIG_TEST_RESULTS));
        final Map<String, Long> origTimes = MapUtil.valueRead(Long::parseLong, nextLine(search, Constants.ORIG_TEST_TIMES));

        final Map<String, TestInfo> origInfo = FigureGenerator.convertMapsToIsolationDataMap(origResults, origTimes);

        final Optional<String> isolationResLine = nextLineOpt(search, Constants.ISOLATION_RESULTS);
        final Optional<String> isolationTimeLine = nextLineOpt(search, Constants.ISOLATION_TIMES);

        Map<String, TestInfo> isolationInfo = new HashMap<>();
        OptUtil.ifAllPresent(isolationResLine, isolationTimeLine, (resLine, timeLine) -> {
            final Map<String, Result> results = MapUtil.valueRead(Result::valueOf, resLine);
            final Map<String, Long> times = MapUtil.valueRead(Long::parseLong, timeLine);

            isolationInfo.putAll(FigureGenerator.convertMapsToIsolationDataMap(results, times));
        });

        return new TestOrder(isolationInfo, reorderInfo, origInfo, testOrder);
    }

    private EnhancedValues<Data> data;

    private final Constants.TECHNIQUE technique;
    private List<String> origOrder;

    private final String filename;

    public ConfigurationData(final String filename) {
        this.filename = filename;

        if (filename.contains("PRIORITIZATION")) {
            technique = Constants.TECHNIQUE.PRIORITIZATION;
        } else if (filename.contains("SELECTION")) {
            technique = Constants.TECHNIQUE.SELECTION;
        } else {
            technique = Constants.TECHNIQUE.PARALLELIZATION;
        }
    }

    public Constants.TECHNIQUE technique() {
        return technique;
    }

    public boolean enhancedOrderDifferent() {
        return PairStream.zip(data.unenhanced().testOrders, data.enhanced().testOrders).anyMatch((unen, en) -> !unen.testOrder.equals(en.testOrder));
    }

    public double speedup() {
        final Averager<Double> averager = new Averager<>();

        PairStream.zip(data.unenhanced().testOrders, data.enhanced().testOrders)
                .forEach((unen, en) -> averager.add(1 - en.orderTime() / unen.orderTime()));

        return averager.mean();
    }

    private List<String> origOrder() {
        return origOrder;
    }

    private ConfigurationData origOrder(final List<String> testOrder) {
        origOrder = testOrder;
        return this;
    }

    public String testType() {
        return data.unenhanced().testType;
    }

    public String subjectName() {
        return data.unenhanced().projectName;
    }

    private class TestOrder {
        private Map<String, TestInfo> isolationInfo;
        private Map<String, TestInfo> reorderInfo;
        private Map<String, TestInfo> origInfo;
        private List<String> testOrder;

        public TestOrder(final Map<String, TestInfo> isolationInfo,
                         final Map<String, TestInfo> reorderInfo,
                         final Map<String, TestInfo> origInfo,
                         final List<String> testOrder) {
            this.isolationInfo = isolationInfo;
            this.reorderInfo = reorderInfo;
            this.origInfo = origInfo;
            this.testOrder = testOrder;
        }

        public boolean containsDTs() {
            for (final String testName : reorderInfo.keySet()) {
                if (!reorderInfo.get(testName).getResult().equals(origInfo.get(testName).getResult())) {
                    return true;
                }
            }

            return false;
        }

        public double orderTime() {
            return EnhancedResultsFigureGenerator.shift_by_time(isolationInfo, origInfo, reorderInfo, testOrder, origOrder());
        }

        public List<String> testOrder() {
            return testOrder;
        }

        public int reruns() {
            int i = 0;

            for (final String testName : reorderInfo.keySet()) {
                if (!reorderInfo.get(testName).getResult().equals(origInfo.get(testName).getResult())) {
                    i++;
                }
            }

            return i;
        }

        public double origTime() {
            return origInfo.values().stream().mapToDouble(i -> i.getTime() / 1E9).sum();
        }

        public boolean fullRerunNeeded() {
            for (final String testName : reorderInfo.keySet()) {
                if (!reorderInfo.get(testName).getResult().equals(origInfo.get(testName).getResult())) {
                    if (!isolationInfo.containsKey(testName) ||
                        !isolationInfo.get(testName).getResult().equals(origInfo.get(testName).getResult()))  {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private class Data {
        private final String projectName;
        private final String testType;

        private final List<TestOrder> testOrders;

        public Data(final String projectName, final String testType, final List<TestOrder> testOrders) {
            this.projectName = projectName;
            this.testType = testType;
            this.testOrders = testOrders;
        }

        public boolean containsDTs() {
            return testOrders.stream().anyMatch(TestOrder::containsDTs);
        }

        public void showRerunNeeded(final String configType) {
            for (final TestOrder testOrder : testOrders) {
                if (testOrder.reruns() > 0) {
                    System.out.println(configType + " " + testType() + " rerun");
                } else {
                    System.out.println(configType + " " + testType() + " no-rerun");
                }
            }
        }

        public String projectName() {
            return projectName;
        }

        public List<Double> origTimes() {
            return testOrders.stream().map(TestOrder::origTime).collect(Collectors.toList());
        }

        public void showFalsePositive() {
            if (containsDTs()) {
                System.out.println("contains-false-positives");
            } else {
                System.out.println("no-false-positives");
            }
        }

        public String rerunType() {
            String rerunType = "no-rerun";

            for (final TestOrder testOrder : testOrders) {
                if (testOrder.fullRerunNeeded()) {
                    return "full-rerun-" + testOrder.reruns();
                } else if (testOrder.reruns() > 0) {
                    rerunType = "rerun-" + testOrder.reruns();
                }
            }

            return rerunType;
        }
    }
}
