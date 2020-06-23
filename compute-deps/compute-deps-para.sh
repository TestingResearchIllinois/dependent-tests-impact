#!/bin/bash

# Assumes ${DT_SUBJ}/${SUBJ_NAME}-orig-order that all tests must pass

set -e

cd $DT_SUBJ

PRECOMPUTE_FLAG="-resolveDependences $DT_SCRIPTS/${SUBJ_NAME}-results/para-DT_LIST-${SUBJ_NAME}-${j}-${MACHINES}.txt"
CLASSPATH=$DT_LIBS:$DT_CLASS:$DT_TESTS:$DT_TOOLS

time_order=""
time_file=""
if [[ "$ALGO" == "p2" ]]; then
    time_order="-timeOrder "
    time_file="$DT_SCRIPTS/${SUBJ_NAME}-results/$SUBJ_NAME-orig-time.txt"
fi

echo "" > .tmp_file
java -cp $DT_TOOLS: edu.washington.cs.dt.impact.runner.OneConfigurationRunner \
  -technique parallelization \
  ${time_order} "${time_file}" \
  -order $j \
  -origOrder $DT_SCRIPTS/${SUBJ_NAME}-results/${SUBJ_NAME}-orig-order \
  -testInputDir $DT_SCRIPTS/${SUBJ_NAME}-results/sootTestOutput-orig \
  -filesToDelete .tmp_file \
  -project "$SUBJ_NAME" \
  -testType "orig" \
  -numOfMachines $MACHINES \
  -outputDir $DT_SCRIPTS/${SUBJ_NAME}-results/ \
  -timesToRun $medianTimes \
  -classpath "$CLASSPATH" \
  -postProcessDTs \
  $PRECOMPUTE_FLAG
rm .tmp_file
