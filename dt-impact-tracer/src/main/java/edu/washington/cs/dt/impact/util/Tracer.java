/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam
 * 
 * Used by the Instrumenter to record the statements a test case covers.
 */

package edu.washington.cs.dt.impact.util;

import edu.washington.cs.dt.impact.util.StopWatch;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Stack;

public class Tracer {
    private static Map<String, Set<String>> statements = new ConcurrentHashMap<>();
    private static final List<String> selectionStatements = Collections.synchronizedList(new LinkedList<>());
    private static Set<String> duplicates = ConcurrentHashMap.newKeySet();
    private static boolean printLastElement = true;
    private static StopWatch timer = new StopWatch();
    private static StopWatch totalExecutionTime = new StopWatch();

    public static void trace(String str, String methodName) {
        if (statements.containsKey(methodName)) {
            Set<String> currSet = statements.get(methodName);
            currSet.add(str);
            statements.put(methodName, currSet);
        } else {
            final Set<String> base = ConcurrentHashMap.newKeySet();
            base.add(str);
            statements.put(methodName, base);
        }
    }

    public static void output(String packageMethodName) {
        File theDir = new File("sootTestOutput");
        // if the directory does not exist, create it
        tryCreateDirectory(theDir);

        FileWriter output = null;
        BufferedWriter writer = null;
        try {
            output = new FileWriter("sootTestOutput" + File.separator + packageMethodName);
            writer = new BufferedWriter(output);

            for (String key : statements.keySet()) {
                for (String s : statements.get(key)) {
                    writer.write(key + " : " + s + "\n");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                // Ignore issues during closing
            }
        }
    }

    private static void tryCreateDirectory(final File theDir) {
        try {
            Files.createDirectory(theDir.toPath());
        } catch (FileAlreadyExistsException ignored) {
            // The directory must have been created in between the check above and our attempt to create it.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void reset() {
        statements.clear();
        selectionStatements.clear();
        duplicates.clear();
    }

    public static void selectionTrace(String str, String packageMethodName) {
        String s = packageMethodName + " : " + str.split(" goto")[0];
        /*if (str.contains("staticinvoke <edu.washington.cs.dt.impact.util.Tracer:"
                + " void selectionTrace(java.lang.String,java.lang.String)>")
                || duplicates.contains(s)) {
            return;
        }*/

        // duplicates.add(s);
        selectionStatements.add(s);
        printLastElement = true;
    }

    // used for the instrumentation of test selection test files
    public static void selectionOutput(String packageMethodName) {
        if (!printLastElement) {
            return;
        }

        File theDir = new File("sootTestOutput");
        tryCreateDirectory(theDir);

        FileWriter output = null;
        BufferedWriter writer = null;
        try {
            output = new FileWriter("sootTestOutput" + File.separator + packageMethodName, true);
            writer = new BufferedWriter(output);

            synchronized (selectionStatements) {

		if (selectionStatements.size() > 1) {
                    String prev = selectionStatements.get(0);
                    for (int i = 1; i < selectionStatements.size(); i++) {
                        writer.write(prev + " >>>>>>>> " + selectionStatements.get(i) + "\n");
                        prev = selectionStatements.get(i);
                    }
                } else if (selectionStatements.size() == 1) {
                    writer.write(selectionStatements.get(0) + "\n");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                // Ignore issues during closing
            }
        }
    }

    public static void startTimer (){
	timer.setStartTime();
    }
    public static void endTimer(){
	timer.setEndTime();
    }

    public static void exceptionMessage(String packageMethodName, String prefix){
	endExecution(packageMethodName, prefix, "Exception");
	File theDir = new File("sootException");
        // if the directory does not exist, create it
        tryCreateDirectory(theDir);
        FileWriter output = null;
        BufferedWriter writer = null;
        try {
            output = new FileWriter("sootException" + File.separator + "test", true);
            writer = new BufferedWriter(output);
	    writer.write("Caught Execption in : " + packageMethodName + "\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                // Ignore issues during closing
            }
        }
    }

    public static void timerOutput(String packageMethodName, String methodName, String declaringClass){
	File theDir = new File("sootTimerOutput");
        // if the directory does not exist, create it
        tryCreateDirectory(theDir);

        FileWriter output = null;
        BufferedWriter writer = null;
	long elapsedTime = timer.getTotalTime();
	timer.reset();
        try {
            output = new FileWriter("sootTimerOutput" + File.separator + packageMethodName, true);
            writer = new BufferedWriter(output);
	    writer.write(packageMethodName + " >>> " + declaringClass + "." + methodName + " : " + elapsedTime + "\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                // Ignore issues during closing
            }
        }
    }

    public static void startExecution(String packageMethodName, String prefix){
	File theDir = new File("sootSeqOutput");
        // if the directory does not exist, create it
        tryCreateDirectory(theDir);

        FileWriter output = null;
        BufferedWriter writer = null;
        totalExecutionTime.setStartTime();
        try {
            output = new FileWriter("sootSeqOutput" + File.separator + "test", true);
            writer = new BufferedWriter(output);
	    writer.write("Started > " + prefix + " >> " + packageMethodName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                // Ignore issues during closing
            }
        }
    }

    public static void endExecution(String packageMethodName, String prefix, String endMethod){
	File theDir = new File("sootSeqOutput");
        // if the directory does not exist, create it
        tryCreateDirectory(theDir);

        FileWriter output = null;
        BufferedWriter writer = null;
	totalExecutionTime.setEndTime();
	long elapsedTime = totalExecutionTime.getTotalTime();
        totalExecutionTime.reset();
        try {
            output = new FileWriter("sootSeqOutput" + File.separator + "test", true);
            writer = new BufferedWriter(output);
	    writer.write(" > Ended > " + packageMethodName + " > " + endMethod + " >> " + "Time : " + elapsedTime + " " +  "\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                // Ignore issues during closing
            }
        }
    }
}
