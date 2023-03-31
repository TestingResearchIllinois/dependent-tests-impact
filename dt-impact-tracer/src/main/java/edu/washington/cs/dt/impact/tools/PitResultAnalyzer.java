package edu.washington.cs.dt.impact.tools;

import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.collections.PairStream;
import com.reedoei.eunomia.functional.Func;
import com.reedoei.eunomia.string.Context;
import com.reedoei.eunomia.string.matching.LineMatch;
import com.reedoei.eunomia.string.searching.Searcher;
import com.reedoei.eunomia.string.searching.StringSearch;
import com.reedoei.eunomia.io.files.FileUtil;
import com.reedoei.eunomia.util.StandardMain;
import com.reedoei.eunomia.string.StringUtil;
import edu.washington.cs.dt.impact.util.Constants;
import org.dom4j.DocumentException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class PitResultAnalyzer extends StandardMain {
    private Path outputDir;
    private Path resultFilesPath;

    private PitResultAnalyzer(final String[] args) {
        super(args);
    }

    public static void main(final String[] args) throws Exception {
        new PitResultAnalyzer(args).run();
    }

    @Override
    protected void run() throws Exception {
        final Path basePath = Paths.get(getArgRequired("basePath"));

        final Path origMutationsPath = basePath.resolve("target").resolve("pit").resolve("orig");
        final Path autoMutationsPath = basePath.resolve("target").resolve("pit").resolve("auto");

        run(origMutationsPath, autoMutationsPath,
            Paths.get(getArgRequired("resultFiles")),
            Paths.get(getArgRequired("outputDir")));
    }

    private void run(final Path origMutationsPath, final Path autoMutationsPath, final Path resultFilesPath, final Path outputDir)
            throws IOException, DocumentException {
        this.outputDir = outputDir;
        this.resultFilesPath = resultFilesPath;

        System.out.println(averageResults("orig", origMutationsPath).latexString());
        System.out.println(averageResults("auto", autoMutationsPath).latexString());
    }

    private EnhancedResultAverager averageResults(final String origOrAuto, final Path mutationsPath)
            throws IOException, DocumentException {
        final MutantMatrix mutantMatrix = MutantMatrix.fromPath(mutationsPath);

        final List<MutantMatrix.MutationGroup> mutationGroups = mutantMatrix.createMutationGroups();

        generateResults(mutationGroups);

        // TODO: Record this information somewhere if this is what we end up doing.
        System.out.printf("Using %d mutation groups for %s results\n", mutationGroups.size(), origOrAuto);

        final EnhancedResultAverager averager = new EnhancedResultAverager(origOrAuto, resultFilesPath);

        mutationGroups.forEach(m -> {
            try {
                averager.add(outputPath(m));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // To end the line we print out in results()
        System.out.println();
        return averager;
    }

    private Path outputPath(final MutantMatrix.MutationGroup mutationGroup) {
        return outputDir.resolve(Integer.toString(mutationGroup.getIndex()));
    }

    private Optional<List<String>> readTestOrder(final File file) {
        try {
            return new StringSearch(FileUtil.readFile(file.toPath()))
                    .search(Searcher.contains(Constants.TEST_ORDER_LIST))
                    .map(Context.applyNextLine(ListUtil.reader(Function.identity())))
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private void generateResults(final List<MutantMatrix.MutationGroup> mutationGroups)
            throws IOException {
        Files.createDirectories(outputDir);

        final File[] files = resultFilesPath.toFile().listFiles();
        if (files != null) {
            PairStream.product(Arrays.asList(files), mutationGroups).forEach(this::processGroup);
        }
    }

    private List<String> generateResultsForGroup(final File file,
                                                 final List<String> testOrder,
                                                 final MutantMatrix.MutationGroup mutationGroup)
            throws IOException {
        final List<String> mutationInformation = new ArrayList<>();
        mutationInformation.add("\n");
        mutationInformation.add(Constants.MUTANT_LIST);
        mutationInformation.add(mutationGroup.toString());

        final List<String> coverageStr = mutationGroup.showCoverage(testOrder);

        return new StringSearch(FileUtil.readFile(file.toPath()))
                .searchFirst(Searcher.contains(Constants.COVERAGE_STRING))
                .flatMap(LineMatch::nextLine)
                .map(l -> l.map(Func.constant(coverageStr.toString())).insertAfter(mutationInformation))
                .map(LineMatch::getLines)
                .orElseThrow(() -> new RuntimeException("Coverage string not present or no coverage line in: " + file.getAbsolutePath()));
    }

    private void processGroup(final File file, final MutantMatrix.MutationGroup mutationGroup) {
        readTestOrder(file).ifPresent(testOrder -> {
            try {
                final List<String> newLines = generateResultsForGroup(file, testOrder, mutationGroup);

                // Write modified file to MUTANT_GROUP_INDEX/FILENAME
                // Where FILENAME is the original filename.
                final Path outputPath = outputPath(mutationGroup).resolve(file.getName());
                Files.createDirectories(outputPath.getParent());
                Files.write(outputPath, StringUtil.unlines(newLines).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
