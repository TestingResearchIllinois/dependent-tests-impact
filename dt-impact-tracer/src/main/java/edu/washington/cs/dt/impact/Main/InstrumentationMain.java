/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam
 *
 * Main class to instrument a project.
 */

package edu.washington.cs.dt.impact.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import edu.washington.cs.dt.impact.tools.FailedTestRemover;
import edu.washington.cs.dt.impact.util.InstrumenterXML;
import soot.*;
import edu.washington.cs.dt.impact.util.Constants;
import edu.washington.cs.dt.impact.util.Constants.TECHNIQUE;
import edu.washington.cs.dt.impact.util.Instrumenter;
import soot.PackManager;
import soot.Transform;
import soot.options.Options;

public class InstrumentationMain {
    public static void main(String[] args) throws Exception {
        /* check the arguments */
        if (args.length == 0) {
            System.err.println("Usage: java InstrumentationMain [options] classname");
            System.exit(0);}

        // list to parse the arguments
        List<String> argsList = new ArrayList<String>(Arrays.asList(args));

        // get list of files to instrument
        int inputDirIndex = argsList.indexOf("-inputDir");
        if (inputDirIndex != -1) {
            // get index of input directory
            int inputDirNameIndex = inputDirIndex + 1;
            if (inputDirNameIndex >= argsList.size()) {
                System.err.println("Input directory argument is specified but a directory"
                        + " path is not. Please use the format: -inputDir adirpath");
                System.exit(0);
            }
            String inputDirName = argsList.get(inputDirNameIndex);
            File f = new File(inputDirName);
            if (!f.isDirectory()) {
                System.err.println("Input directory argument is specified but the directory"
                        + " path is invalid. Please check the directory path.");
                System.exit(0);
            }
            argsList.remove(inputDirNameIndex);
            argsList.remove(inputDirIndex);
            argsList.add("-process-path");
            argsList.add(inputDirName);
        } else {
            System.err.println("No input directory argument is specified."
                    + " Please use the format: -inputDir adirpath");
            System.exit(0);
        }

        // get the technique, the default is absolute
        TECHNIQUE techniqueName = Constants.DEFAULT_TECHNIQUE;
        int techniqueIndex = argsList.indexOf("-technique");
        if (techniqueIndex != -1) {
            // get index of technique name
            int techniqueNameIndex = techniqueIndex + 1;
            if (techniqueNameIndex >= argsList.size()) {
                System.err
                        .println("Technique argument is specified but technique name is not."
                                + " Please use the format: -technique aTechniqueName");
                System.exit(0);
            }

            String techniqueStr = argsList.get(techniqueNameIndex).toLowerCase().trim();
            if (techniqueStr.equals("prioritization")) {
                techniqueName = TECHNIQUE.PRIORITIZATION;
            } else if (techniqueStr.equals("selection")) {
                techniqueName = TECHNIQUE.SELECTION;
            } else if (techniqueStr.equals("parallelization")) {
                techniqueName = TECHNIQUE.PARALLELIZATION;
            } else {
                System.err
                        .println("Technique name is invalid. Try \"prioritization-absolute\","
                                + " \"prioritization-relative\", \"random\" or \"selection\".");
                System.exit(0);
            }
            argsList.remove(techniqueNameIndex);
            argsList.remove(techniqueIndex);
        }

        int sootClasspathIndex = argsList.indexOf("--soot-cp");
        String sootClasspath = System.getProperty("java.class.path");
        if (sootClasspathIndex != -1) {
            sootClasspath = FailedTestRemover.buildClassPath(argsList.get(sootClasspathIndex + 1).split(":"));
            argsList.remove(sootClasspathIndex + 1);
            argsList.remove(sootClasspathIndex);

        }
        int outputPathindex= argsList.indexOf("-outputPath");
        String outputPath="";
        if(outputPathindex!=-1)
        {
            int outputPathNameindex = outputPathindex + 1;
            outputPath= argsList.get(outputPathNameindex);
            argsList.remove(outputPathNameindex);
            argsList.remove(outputPathindex);
        }
        int inputModeIndex= argsList.indexOf("-compare");
        if (inputModeIndex != -1)
        {
            argsList.remove(inputModeIndex);
            Pack wjtp = PackManager.v().getPack("wjtp");
            List<String> targetTestMethodNames = new ArrayList<String>();
            //targetTestMethodNames.add("com.github.kevinsawicki.http.EncodeTest.encode");

            int changedMethodIndex= argsList.indexOf("-changedFiles");
            if(changedMethodIndex!=-1)
            {
                int changedMethodNameIndex = changedMethodIndex + 1;
                for (String methodname : argsList.get(changedMethodNameIndex).split(",")) {
                    targetTestMethodNames.add(methodname);
                }
                argsList.remove(changedMethodNameIndex);
                argsList.remove(changedMethodIndex);
            }
            int ifelselimit=0;
            InstrumenterXML instrumenter = new InstrumenterXML(techniqueName, targetTestMethodNames,ifelselimit);
            wjtp.add(new Transform("wjtp.instrumenter", instrumenter));

            Scene.v().setSootClassPath(sootClasspath);

            Options.v().set_output_format(Options.output_format_class);

            // Set the output directory for the instrumented .class files
            Options.v().set_output_dir("/home/pious/Documents/work/dependent-tests-impact/testgui");

            argsList.add("-w");
            argsList.add("-p");
            argsList.add("cg");
            argsList.add("all-reachable:true");
            argsList.add("-keep-line-number");
            argsList.add("-pp");
            argsList.add("-allow-phantom-refs");
            argsList.add("-p");
            argsList.add("jb");
            argsList.add("use-original-names:true");
            // enable loop unrolling optimization pack and set unroll depth to 4
            /*argsList.add("-p");
            argsList.add("jb.ls:unroll-depth=4");

// enable loop peeling optimization pack and set peel depth to 2
            argsList.add("-p");
            argsList.add("jb.ulp:peel-depth=2");*/
            /**/
//            argsList.add("-p");
//            argsList.add("wjtp.tnlp");
//            argsList.add("enabled:false");
            /*argsList.add("--no-bodies-for-excluded");
            argsList.add("--loop-opts-enabled");*/
            /*argsList.add("--num-loop-detection");
            argsList.add("3");*/


            int inputDirNameIndex = inputDirIndex + 1;
            String inputDirName = argsList.get(inputDirNameIndex);

            List<String> classNames = getClassesFromDirectory(new File(inputDirName));

            for (String className : classNames) {
                SootClass clazz = Scene.v().forceResolve(className, SootClass.BODIES);
                clazz.setApplicationClass();
            }

            for (SootClass sc : Scene.v().getClasses()) {
                Scene.v().addBasicClass(sc.getName(), SootClass.BODIES);
            }


            String[] sootArgs = argsList.toArray(new String[0]);

            soot.Main.main(sootArgs);
            instrumenter.generateXML(outputPath);
        }
        else {

            Pack jtp = PackManager.v().getPack("jtp");
            jtp.add(new Transform("jtp.instrumenter",
                    new Instrumenter(techniqueName)));

            Scene.v().setSootClassPath(sootClasspath);

            argsList.add("-keep-line-number");
            argsList.add("-pp");
            argsList.add("-allow-phantom-refs");
            String[] sootArgs = argsList.toArray(new String[0]);



            /**Give control to Soot to process all options,
             * Instrumenter.internalTransform will get called.*/

            soot.Main.main(sootArgs);

        }

    }
    public static List<String> getClassesFromDirectory(File directory) {
        List<String> classNames = new ArrayList<>();
        if (directory.exists() && directory.isDirectory()) {
            try {
                Path basePath = directory.toPath();
                Files.walk(basePath).forEach(path -> {
                    if (Files.isRegularFile(path) && path.toString().endsWith(".class")) {
                        String className = basePath.relativize(path).toString().replace(File.separator, ".");
                        className = className.substring(0, className.length() - ".class".length());
                        classNames.add(className);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("Error getting classes from directory", e);
            }
        }
        return classNames;
    }
    public static String getFullyQualifiedMethodName(String filePath, String methodName) {
        String packageName = extractPackageName(filePath);
        String className = extractClassName(filePath);
        return packageName + "." + className + "." + methodName;
    }

    private static String extractPackageName(String filePath) {
        String packagePath = filePath.substring(filePath.indexOf("java") + 5, filePath.lastIndexOf(File.separator));
        return packagePath.replace(File.separator, ".");
    }

    private static String extractClassName(String filePath) {
        String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
        return fileName.replace(".java", "");
    }
}
