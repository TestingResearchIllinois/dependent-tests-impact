#!/bin/bash

# Assumes ${DT_SUBJ}/${SUBJ_NAME}-orig-order that all tests must pass

set -e

cd $NEW_DT_SUBJ

CLASSPATH=$NEW_DT_LIBS:$NEW_DT_CLASS:$NEW_DT_TESTS:$DT_TOOLS

dep_file=""
precomputed_deps=""
if [[ -f $DT_SCRIPTS/${SUBJ_NAME}-results/prio-DT_LIST-${SUBJ_NAME}-${i}-${j}.txt ]]; then
    dep_file="-dependentTestFile "
    precomputed_deps="$DT_SCRIPTS/${SUBJ_NAME}-results/prio-DT_LIST-${SUBJ_NAME}-${i}-${j}.txt"
fi

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
  ${dep_file} "${precomputed_deps}"
rm .tmp_file
