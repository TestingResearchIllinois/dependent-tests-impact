#!/bin/bash

# Assumes ${DT_SUBJ}/${SUBJ_NAME}-orig-order that all tests must pass

set -e

cd $DT_SUBJ

PRECOMPUTE_FLAG="-resolveDependences $DT_SCRIPTS/${SUBJ_NAME}-results/prio-DT_LIST-${SUBJ_NAME}-${i}-${j}.txt"
CLASSPATH=$DT_LIBS:$DT_CLASS:$DT_TESTS:$DT_TOOLS

echo "" > .tmp_file
java -cp $DT_TOOLS: edu.washington.cs.dt.impact.runner.OneConfigurationRunner \
  -technique prioritization \
  -coverage $i \
  -order $j \
  -origOrder $DT_SCRIPTS/${SUBJ_NAME}-results/${SUBJ_NAME}-orig-order \
  -testInputDir $DT_SCRIPTS/${SUBJ_NAME}-results/sootTestOutput-orig \
  -filesToDelete .tmp_file \
  -getCoverage \
  -project "$SUBJ_NAME" \
  -testType "orig" \
  -outputDir $DT_SCRIPTS/${SUBJ_NAME}-results/ \
  -timesToRun $medianTimes \
  -classpath "$CLASSPATH" \
  -postProcessDTs \
  $PRECOMPUTE_FLAG
rm .tmp_file
