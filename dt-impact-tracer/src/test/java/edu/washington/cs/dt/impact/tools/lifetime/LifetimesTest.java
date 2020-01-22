package edu.washington.cs.dt.impact.tools.lifetime;

import com.reedoei.eunomia.collections.ListUtil;
import com.reedoei.eunomia.math.Averager;
import com.reedoei.eunomia.util.DateUtil;
import edu.washington.cs.dt.impact.data.TechniqueValues;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import soot.jimple.spark.ondemand.genericutil.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.HashSet;

import static org.junit.Assert.*;

public class LifetimesTest {
    private static final Path BASE_PATH =
            Paths.get("src").resolve("test").resolve("resources").resolve("precomputed-lifetime");

    private static final String V1 = "src/test/resources/precomputed-lifetime/sample-2018-05-04-04-17-45-v1";
    private static final String V2 = "src/test/resources/precomputed-lifetime/sample-2018-05-06-04-17-45-v2";
    private static final String V6 = "src/test/resources/precomputed-lifetime/sample-2018-05-16-04-45-45-v6";
    private static final String V7 = "src/test/resources/precomputed-lifetime/sample-2018-05-17-10-15-01-v7";
    private static final String V10 = "src/test/resources/precomputed-lifetime/sample-2018-06-09-17-15-23-v10";
    private static final String V11 = "src/test/resources/precomputed-lifetime/sample-2018-06-23-15-01-23-v11";
    private static final String V12 = "src/test/resources/precomputed-lifetime/sample-2018-07-01-01-15-01-v12";

    @BeforeClass
    public static void beforeClass() {
        for (final String path : ListUtil.fromArray(V1, V2, V6, V7, V10, V11, V12)) {
            if (!Files.exists(Paths.get(path))) {
                try {
                    Files.createDirectories(Paths.get(path));
                } catch (IOException e) {
                    Assume.assumeNoException(e);
                }
            }
        }
    }

    @Before
    public void setUp() {
        Assume.assumeTrue(Files.exists(BASE_PATH));
    }

    @Test
    public void testSplitCutoffs() throws Exception {
        final Averager<Integer> prioAverager = new Averager<>(ListUtil.fromArray(8, 2, 39, 4, 40));
        final Averager<Integer> seleAverager = new Averager<>(ListUtil.fromArray(5, 8, 12, 14, 25));
        final Averager<Integer> paraAverager = new Averager<>(ListUtil.fromArray(1, 1, 3, 2, 13));

        final Lifetimes lifetimes = new Lifetimes(DateUtil.makeDate(2018, 5, 1), prioAverager, seleAverager, paraAverager);

        final TechniqueValues<Pair<List<Path>, List<Path>>> values = lifetimes.splitAtCutoff(BASE_PATH);

        final List<Path> prioBefore = Util.map(ListUtil.fromArray(V7, V1, V6, V2), Paths::get);
        final List<Path> prioAfter = Util.map(ListUtil.fromArray(V12, V11, V10), Paths::get);

        final List<Path> seleBefore = Util.map(ListUtil.fromArray(V1, V2), Paths::get);
        final List<Path> seleAfter = Util.map(ListUtil.fromArray(V12, V7, V6, V11, V10), Paths::get);

        final List<Path> paraBefore = Util.map(ListUtil.fromArray(V7, V1, V6, V2), Paths::get);
        final List<Path> paraAfter = Util.map(ListUtil.fromArray(V12, V11, V10), Paths::get);

        assertEquals(new HashSet(prioBefore), new HashSet(values.prio().getLeft()));
        assertEquals(new HashSet(prioAfter), new HashSet(values.prio().getRight()));
        assertEquals(new HashSet(seleBefore), new HashSet(values.sele().getLeft()));
        assertEquals(new HashSet(seleAfter), new HashSet(values.sele().getRight()));
        assertEquals(new HashSet(paraBefore), new HashSet(values.para().getLeft()));
        assertEquals(new HashSet(paraAfter), new HashSet(values.para().getRight()));

    }
}
