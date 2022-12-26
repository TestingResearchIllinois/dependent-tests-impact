package edu.washington.cs.dt.impact.util;

/**
 * Used by {@link #Instrumenter} to record the execution time for each method under tests
 */
public class StopWatch {
    private static long start = 0L;
    private static long end = 0L;
    public static void setStartTime() {
        start = System.nanoTime();
    }
    public static void setEndTime() {
        end = System.nanoTime();
    }
    public static long getTotalTime(){
        return end - start;
    }
    public static void reset() {
        start = 0L;
        end = 0L;
    }
}
