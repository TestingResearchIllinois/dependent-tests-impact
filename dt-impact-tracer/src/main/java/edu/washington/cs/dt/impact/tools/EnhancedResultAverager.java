package edu.washington.cs.dt.impact.tools;

import com.google.common.base.Preconditions;
import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.functional.Func;
import com.reedoei.eunomia.io.files.FileUtil;
import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.math.Averager;
import com.reedoei.eunomia.util.Util;
import edu.washington.cs.dt.impact.figure.generator.EnhancedResults;
import edu.washington.cs.dt.impact.figure.generator.EnhancedResultsFigureGenerator;
import edu.washington.cs.dt.impact.util.Constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class EnhancedResultAverager {
    private final List<Averager<Double>> averagers = new ArrayList<>();

    private final String origOrAuto;
    private final Constants.TECHNIQUE technique;

    private final List<Path> resultFilesPaths = new ArrayList<>();
    private final List<EnhancedResults> enhancedResults = new ArrayList<>();

    public EnhancedResultAverager(final String origOrAuto, final Path resultFilesParent) throws IOException {
        this(origOrAuto, EnhancedResults.getTechnique(resultFilesParent));
    }

    public EnhancedResultAverager(final String origOrAuto, final Constants.TECHNIQUE technique) {
        this.origOrAuto = origOrAuto;
        this.technique = technique;
    }

    public EnhancedResultAverager add(final Path resultFilesPath) throws IOException {
        resultFilesPaths.add(resultFilesPath);

        final EnhancedResults results = getResults(resultFilesPath);
        enhancedResults.add(results);

        System.out.printf("[INFO] Generating %s results for %s\n", origOrAuto, Paths.get("").toAbsolutePath().relativize(resultFilesPath));

//        addValues(getValues(resultFilesPath));
        addValues(results.values(origOrAuto));

        return this;
    }

    private List<Double> getValues(final Path resultFilesPath) throws IOException {
        final Path outputFile = Paths.get("cached")
                .resolve(resultFilesPath.toString().replace("/", "-").substring(1)
                        + "-" + techniqueStr()
                        + "-" + origOrAuto);

        try {
            if (Files.exists(outputFile)) {
                return ListUtil.read(Double::parseDouble, FileUtil.readFile(outputFile));
            }
        } catch (IOException ignored) {}

        final List<Double> values = getResults(resultFilesPath).values(origOrAuto);

        try {
            Files.write(outputFile, values.toString().getBytes());
        } catch (IOException ignored) {}
        return values;
    }

    private String techniqueStr() {
        if (technique == Constants.TECHNIQUE.PRIORITIZATION) {
            return "prio";
        } else if (technique == Constants.TECHNIQUE.SELECTION) {
            return "sele";
        } else {
            return "para";
        }
    }

    public List<EnhancedResults> enhancedResults() {
        return enhancedResults;
    }

    private EnhancedResults getResults(final Path resultFilesPath) throws IOException {
        final Path outputPath = Files.createTempDirectory("output");

        return EnhancedResultsFigureGenerator.setup(true, origOrAuto, resultFilesPath, outputPath);
    }

    private void ensureAveragerCapacity(final int size) {
        for (int i = averagers.size(); i < size; i++) {
            averagers.add(new Averager<>());
        }
    }

    private void addValues(final List<Double> values) {
        ensureAveragerCapacity(values.size());
        Util.zipWith(Averager::add, averagers, values);
    }

    public String latexString() throws IOException {
        Preconditions.checkState(!resultFilesPaths.isEmpty(), "Must have added at least one result file path BEFORE generating latex string!");

        final Path outputPath = Files.createTempDirectory("output");
        final EnhancedResults results =
                // We only need the results files paths for the subject name, so we can just use the first one.
                EnhancedResultsFigureGenerator.setup(true, origOrAuto, resultFilesPaths.get(0), outputPath);

        final List<Double> values = ListUtil.map(Averager::mean, averagers);

        switch (technique) {
            case PRIORITIZATION:
                return results.generatePrioString(origOrAuto, values);

            case SELECTION:
                return results.generateSeleString(origOrAuto, values);

            case PARALLELIZATION:
                return results.generateParaString(origOrAuto, values);

            default:
                throw new IllegalArgumentException("Unhandled technique: " + technique);
        }
    }

    public List<Double> means() {
        return ListUtil.map(Averager::mean, averagers);
    }

    public List<Averager<Double>> averagers() {
        return averagers;
    }
}
