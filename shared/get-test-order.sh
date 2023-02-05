#!/usr/bin/env bash

# Usage: bash get-test-order.sh <old|new>
# EITHER:
# - Run the setup script before running this script
# OR:
# - Be in the same directory as the pom.xml before running this script.
# Only run for human-written tests

version=$1

if [[ "$version" == "new" ]]; then
    TESTS=$NEW_DT_TESTS
    CLASS=$NEW_DT_CLASS
    LIBS=$NEW_DT_LIBS
    SUBJ=$NEW_DT_SUBJ
    SUBJ_SRC=$NEW_DT_SUBJ_SRC
    ROOT_DIR=$NEW_DT_SUBJ_ROOT
else
    TESTS=$DT_TESTS
    CLASS=$DT_CLASS
    LIBS=$DT_LIBS
    SUBJ=$DT_SUBJ
    SUBJ_SRC=$DT_SUBJ_SRC
    ROOT_DIR=$DT_SUBJ_ROOT
fi

if [[ ! -z "$SUBJ_SRC" ]]; then
    cd "$SUBJ_SRC"
fi

mkdir -p $DT_SCRIPTS/${SUBJ_NAME}-results

output_file_name="test-order"
if [[ ! -z "$SUBJ_NAME" ]]; then
    output_file_name="$DT_SCRIPTS/${SUBJ_NAME}-results/$SUBJ_NAME-orig-order"
fi

# Run the tests, but force to re-compile from top level just in case of needing local dependencies upgraded
(
    cd $ROOT_DIR
    mvn install -Dmavanagaiata.skip=true -Drat.skip=true -Ddependency-check.skip=true -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -Dmaven-source.skip=true -Dcobertura.skip -DskipTests -pl . -am > "install-log.txt"
)
mvn test -Dmavanagaiata.skip=true -Drat.skip=true -Ddependency-check.skip=true -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -Dmaven-source.skip=true -Dcobertura.skip -Dsurefire.runOrder=reversealphabetical > "test-log.txt"
#mvn -X test | grep "Running" | awk '{print $2}'
java -cp $DT_TOOLS: edu.washington.cs.dt.impact.tools.GetOriginalOrder $output_file_name "target/" "test-log.txt"
mv "test-log.txt" $DT_SCRIPTS/${SUBJ_NAME}-results/
mv "install-log.txt" $DT_SCRIPTS/${SUBJ_NAME}-results/

if [[ ! -z "$SUBJ_NAME" ]]; then
    TEST_ORDER="$output_file_name"
    IGNORE_TESTS_LIST="$DT_SCRIPTS/${SUBJ_NAME}-results/${SUBJ_NAME}-ignore-order"

    if [[ -e "$IGNORE_TESTS_LIST" ]]; then
        temp=$(mktemp)
        grep -Fvf "$IGNORE_TESTS_LIST" $TEST_ORDER > $temp
        mv $temp $TEST_ORDER
    fi

    echo "[INFO] Checking for whether there are any tests that just fails in this order"
    java -cp $DT_TOOLS: edu.washington.cs.dt.impact.tools.detectors.FailingTestDetector --classpath "$CLASS:$TESTS:$LIBS:$DT_TOOLS:" --tests "$TEST_ORDER" --output "$IGNORE_TESTS_LIST"

    if [[ -e "$IGNORE_TESTS_LIST" ]]; then
        temp=$(mktemp)
        grep -Fvf "$IGNORE_TESTS_LIST" $TEST_ORDER > $temp
        mv $temp $TEST_ORDER
    fi
fi
