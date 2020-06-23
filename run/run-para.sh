#!/bin/bash

# Assumes ${DT_SUBJ}/${SUBJ_NAME}-orig-order that all tests must pass

set -e

cd $NEW_DT_SUBJ

CLASSPATH=$NEW_DT_LIBS:$NEW_DT_CLASS:$NEW_DT_TESTS:$DT_TOOLS

dep_file=""
precomputed_deps=""
if [[ -f $DT_SCRIPTS/${SUBJ_NAME}-results/para-DT_LIST-${SUBJ_NAME}-${j}-${MACHINES}.txt ]]; then
    dep_file="-dependentTestFile "
    precomputed_deps="$DT_SCRIPTS/${SUBJ_NAME}-results/para-DT_LIST-${SUBJ_NAME}-${j}-${MACHINES}.txt"
fi

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
  ${dep_file} "${precomputed_deps}"
rm .tmp_file
