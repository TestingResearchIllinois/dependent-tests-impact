package edu.washington.cs.dt.impact.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.washington.cs.dt.impact.util.Constants;

import static edu.washington.cs.dt.impact.figure.generator.FigureGenerator.mustGetArgName;

/**
 * Created by winglam on 5/2/19.
 */
public class PradetToMinimizerFormat {

    public static void main(String[] args) {
        List<String> argsList = new ArrayList<String>(Arrays.asList(args));

        // name of directory where files should be outputted
        String projectName = mustGetArgName(argsList,
                                            "-projectName");

        String origOrAuto = mustGetArgName(argsList,
                                           "-origOrAuto");
        Constants.TEST_TYPE type;
        if (origOrAuto.equalsIgnoreCase("orig")) {
            type = Constants.TEST_TYPE.ORIG;
        } else {
            type = Constants.TEST_TYPE.AUTO;
        }

        File pradetFile = new File(mustGetArgName(argsList,
                                                  "-pradetFile"));
        String priorOutputDirectoryName = mustGetArgName(argsList,
                                                         "-prioOutputDirectory");
        String seleOutputDirectoryName = mustGetArgName(argsList,
                                                        "-seleOutputDirectory");
        String paraOutputDirectoryName = mustGetArgName(argsList,
                                                        "-paraOutputDirectory");

        List<String> pradetDeps = convertPradetToMinimizer(pradetFile);

        for (Constants.TECHNIQUE technique : Constants.TECHNIQUE.values()) {
            if (technique == Constants.TECHNIQUE.PRIORITIZATION) {
                for (Constants.COVERAGE coverage : Constants.COVERAGE.values()) {
                    for (Constants.ORDER order : Constants.ORDER.values()) {
                        OutputPrecomputedDependences.outputDependences("prioritization",
                                                                       order.name().toLowerCase(),
                                                                       coverage.name().toLowerCase(),
                                                                       type.name().toLowerCase(),
                                                                       projectName,
                                                                       -1,
                                                                       null,
                                                                       priorOutputDirectoryName,
                                                                       OutputPrecomputedDependences.getDTFile(pradetDeps));
                    }
                }
            } else if (technique == Constants.TECHNIQUE.PARALLELIZATION) {
                for (Constants.MACHINES machines : Constants.MACHINES.values()) {
                    for (Constants.ORDER order : Constants.ORDER.values()) {
                        OutputPrecomputedDependences.outputDependences("parallelization",
                                                                       order.name().toLowerCase(),
                                                                       null,
                                                                       type.name().toLowerCase(),
                                                                       projectName,
                                                                       machines.getValue(),
                                                                       null,
                                                                       paraOutputDirectoryName,
                                                                       OutputPrecomputedDependences.getDTFile(pradetDeps));
                    }
                }
            } else {
                for (Constants.COVERAGE coverage : Constants.COVERAGE.values()) {
                    for (Constants.ORDER order : Constants.ORDER.values()) {
                        OutputPrecomputedDependences.outputDependences("selection",
                                                                       order.name().toLowerCase(),
                                                                       coverage.name().toLowerCase(),
                                                                       type.name().toLowerCase(),
                                                                       projectName,
                                                                       -1,
                                                                       null,
                                                                       seleOutputDirectoryName,
                                                                       OutputPrecomputedDependences.getDTFile(pradetDeps));
                    }
                }
            }
        }
    }

    private static List<String> convertPradetToMinimizer(File pradetFile) {
        List<String> sb = new ArrayList<>();
        List<String> fileContents = FileTools.parseFileToList(pradetFile);

        for (String s : fileContents) {
            String[] splitComma = s.split(",");
            String brittle = splitComma[0];
            String stateSetter = splitComma[1];

            // At some point we may want to extend this method to run brittle in isolation to confirm that it is indeed a brittle and never a victim
            sb.add(Constants.TEST_LINE + stateSetter);
            sb.add("Intended behavior: PASS"); // Assume result is PASS
            sb.add(Constants.EXECUTE_AFTER + "[]");
            sb.add("The revealed different behavior: PASS"); // Assume result is PASS
            sb.add(Constants.EXECUTE_AFTER + "[" + brittle + "]");
        }

        return sb;
    }
}
