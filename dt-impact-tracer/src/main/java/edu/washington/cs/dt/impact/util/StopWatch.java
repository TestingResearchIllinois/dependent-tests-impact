/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Ridwan
 * 
 * Used by the Instrumenter to record the execution time for each method under tests.
 */

package edu.washington.cs.dt.impact.util;


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

public class StopWatch {
    private static  long start = 0L;
    private static long end = 0L;
    public static void setStartTime() {
	start = System.nanoTime();
    }
    public static void setEndTime() {
	end = System.nanoTime();
    }
    public static Long getTotalTime(){
	return end - start;
    }
    public static void reset() {
	start = 0L;
	end = 0L;
    }
}

