/**
 * Copyright 2014 University of Washington. All Rights Reserved.
 * @author Wing Lam
 *
 *         Constants used throughout this project.
 */

package edu.washington.cs.dt.impact.util;

import edu.washington.cs.dt.impact.data.TechniqueValues;

public class Constants {
    public static final TechniqueValues<Integer> TECHNIQUE_FIGNUMS =
            new TechniqueValues<>(17, 18, 19);

    public static final TECHNIQUE DEFAULT_TECHNIQUE = TECHNIQUE.PRIORITIZATION;
    public static final String DEFAULT_TEST_DIR = "sootTestOutput";
    public static final String TEST_LINE = "Test: ";
    public static final String EXECUTE_AFTER = "when executed after: ";
    public static final String TEST_SEP = ", ";

    // For Figures 7,8,9
    public static final String FIXED_DTS = "Number of DTs fixed:";
    public static final String NUM_NOT_FIXED_DTS = "Number of DTs not fixed:";
    public static final String NOT_FIXED_DTS = "DTs not fixed are:";

    // For figure showing the time to get precomputed dependences
    public static final String NUM_TESTS_OUT_OF_ORIG = "Number of tests selected out of total in original order:";
    public static final String TIME_INCL_DTF = "Execution time of TLG and its time to find/nullify any DTs for 1 machine (does not include the time to run the tests in the new order):";

    // for Figures 17, 18, and 19
    public static final String ARGUMENT_STRING = "The following arguments were used to generate this output.";
    public static final String ORIG_TEST_RESULTS = "Original order results:";
    public static final String ALL_TEST_RESULTS = "Test order results:";
    public static final String TIME_STRING = "Time each test takes to run in the new order:";
    public static final String COVERAGE_STRING = "Coverage test list:";
    public static final String ORDER_TIME = "Execution time for executing the following testing order:";
    public static final String ORDER_TIME_PARA = "New order time:";
    public static final String APFD_VALUE = "APFD value:";
    public static final String TEST_ORDER_LIST = "Test order list:";
    public static final String DT_LIST = "Dependent test list:";
    public static final String AVG_DEP_FIND_TIME_STRING = "Average time to find one dependency:";

    public static final String MUTANT_LIST = "Mutant list:";
    public static final String ISOLATION_RESULTS = "Isolation results:";
    public static final String ISOLATION_TIMES = "Isolation times:";
    public static final String ORIG_TEST_TIMES = "Original order times:";

    public static String getOutputFileName(COVERAGE coverage, TECHNIQUE technique, ORDER order, String project,
            TEST_TYPE test_type, MACHINES machines, DT_SETTING dt_setting, TD_SETTING td_setting, boolean postProcessDTs) {
        String ret_str;
        if (technique == TECHNIQUE.PARALLELIZATION) {
            ret_str = technique + "-" + test_type + "-" + project.replace(" ", "_").toUpperCase() + "-" + order + "-" + dt_setting + "-" + td_setting
                    + "-" + machines + "-" + postProcessDTs;
        } else {
            ret_str = technique + "-" + test_type + "-" + project.replace(" ", "_").toUpperCase() + "-" + coverage + "-" + order + "-" + dt_setting
                    + "-" + td_setting + "-" + postProcessDTs;
        }
        return ret_str.concat(".txt");
    }

    public static String getDTListFileName(String project, TEST_TYPE test_type) {
        String ret_str = project.replace(" ", "_").toUpperCase() + "-" + test_type + "-DT_LIST.txt";
        return ret_str;
    }

    /**
     * Coverage information that is currently supported.
     */
    public enum COVERAGE {
        STATEMENT, FUNCTION
    }

    /**
     * Testing techniques that are currently supported.
     */
    public enum TECHNIQUE {
        PRIORITIZATION, SELECTION, PARALLELIZATION
    }

    /**
     * Test orders that are currently supported.
     */
    public enum ORDER {
        RANDOM, ABSOLUTE, RELATIVE, ORIGINAL, TIME
    }

    /**
     * Test types that are currently supported.
     */
    public enum TEST_TYPE {
        ORIG, AUTO
    }

    /**
     * Number of machines that are currently supported.
     */
    public enum MACHINES {
        ONE(1), TWO(2), FOUR(4), EIGHT(8), SIXTEEN(16);
        private final int value;

        private MACHINES(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Whether we nullified DTs or not.
     */
    public enum DT_SETTING {
        FIXED_DT, CONTAINS_DT
    }

    /**
     * Whether test dependence was provided.
     */
    public enum TD_SETTING {
        GIVEN_TD, OMITTED_TD
    }

}
