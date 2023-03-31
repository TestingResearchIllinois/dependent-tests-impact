package edu.washington.cs.dt.impact.tools.lifetime;

import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.functional.Func;
import com.reedoei.eunomia.math.Averager;
import com.reedoei.eunomia.util.DateUtil;
import edu.washington.cs.dt.impact.data.TechniqueValues;
import edu.washington.cs.dt.impact.util.Constants;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Lifetimes {
    public static Date dateFromDir(final Path path) {
        final String dirName = path.getFileName().toString();
        final List<String> parts = ListUtil.fromArray(dirName.split("-"));
        // Folders are in format: subj-name-yyyy-mm-dd-H-M-S-commit so we want just the date part.
        final List<Integer> dateParts = ListUtil.map(Integer::parseInt, parts.subList(parts.size() - 7, parts.size() - 1));

        return DateUtil.makeDate(dateParts.get(0), dateParts.get(1), dateParts.get(2), dateParts.get(3), dateParts.get(4), dateParts.get(5));
    }

    private final Date startDate;

    private final Averager<Integer> prioAverager;
    private final Averager<Integer> seleAverager;
    private final Averager<Integer> paraAverager;

    public Lifetimes(final Date startDate,
                     final Averager<Integer> prioAverager,
                     final Averager<Integer> seleAverager,
                     final Averager<Integer> paraAverager) {
        this.startDate = startDate;

        this.prioAverager = prioAverager;
        this.seleAverager = seleAverager;
        this.paraAverager = paraAverager;
    }

    public Averager<Integer> prioAverager() {
        return prioAverager;
    }

    public Averager<Integer> seleAverager() {
        return seleAverager;
    }

    public Averager<Integer> paraAverager() {
        return paraAverager;
    }

    public TechniqueValues<Averager<Integer>> techniqueAveragers() {
        return new TechniqueValues<>(prioAverager(), seleAverager(), paraAverager());
    }

    public double prioLifetime() {
        return prioAverager.mean();
    }

    public double seleLifetime() {
        return seleAverager.mean();
    }

    public double paraLifetime() {
        return paraAverager.mean();
    }

    private Date cutoff(final Averager<Integer> averager) {
        return Date.from(startDate.toInstant().plus(Duration.ofDays((long) averager.mean())));
    }

    private Date cutoff(final Constants.TECHNIQUE technique) {
        switch (technique) {
            case PRIORITIZATION:
                return prioCutoff();
            case SELECTION:
                return seleCutoff();
            case PARALLELIZATION:
                return prioCutoff();
        }

        // NOTE: Unreachable (because of switch)
        return startDate;
    }

    public Date prioCutoff() {
        return cutoff(prioAverager);
    }

    public Date seleCutoff() {
        return cutoff(seleAverager);
    }

    public Date paraCutoff() {
        return cutoff(paraAverager);
    }

    public TechniqueValues<Pair<List<Path>, List<Path>>> splitAtCutoff(final Path basePath) throws IOException {
        final TechniqueValues<Pair<List<Path>, List<Path>>> values =
                new TechniqueValues<>(() -> ImmutablePair.of(new ArrayList<>(), new ArrayList<>()));

        Files.list(basePath).forEach(path -> {
            for (final Constants.TECHNIQUE technique : Constants.TECHNIQUE.values()) {
                final Date cutoff = cutoff(technique);

                if (dateFromDir(path).before(cutoff)) {
                    values.technique(technique).getLeft().add(path);
                } else {
                    values.technique(technique).getRight().add(path);
                }
            }
        });

        values.forEach((t, split) -> {
            System.out.println("Before cutoff versions: " + split.getLeft());
            System.out.println("After cutoff versions: " + split.getRight());
        });

        return values;
    }
}
