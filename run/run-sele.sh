#!/bin/bash

set -e

cd $NEW_DT_SUBJ

CLASSPATH=$NEW_DT_LIBS:$NEW_DT_CLASS:$NEW_DT_TESTS:$DT_TOOLS

# Assume computed dependencies exist
dep_file="-dependentTestFile "
if [[ "$ALGO" == "s1" ]] || [[ "$ALGO" == "s4" ]]; then
    precomputed_deps="$DT_SCRIPTS/${SUBJ_NAME}-results/para-DT_LIST-${SUBJ_NAME}-${j}-${MACHINES}.txt"
elif [[ "$ALGO" == "s2" ]] || [[ "$ALGO" == "s3" ]] || [[ "$ALGO" == "s4" ]] || [[ "$ALGO" == "s5" ]] || [[ "$ALGO" == "s6" ]]; then
    precomputed_deps="$DT_SCRIPTS/${SUBJ_NAME}-results/prio-DT_LIST-${SUBJ_NAME}-${i}-${j}.txt"
else
    echo "Unknown $ALGO selection algorithm given. Cannot compute dependencies."
    exit 1
fi

# Reset variables if computed dependencies do not exist
if [[ ! -f ${precomputed_deps} ]]; then
    dep_file=""
    precomputed_deps=""
fi

echo "" > .tmp_file
java -cp $DT_TOOLS: edu.washington.cs.dt.impact.runner.OneConfigurationRunner \
  -technique selection \
  -coverage $i \
  -order $j \
  -origOrder $DT_SCRIPTS/${SUBJ_NAME}-results/${SUBJ_NAME}-orig-order \
  -testInputDir $DT_SCRIPTS/${SUBJ_NAME}-results/sootTestOutput-orig \
  -filesToDelete .tmp_file \
  -project "$SUBJ_NAME" \
  -testType orig \
  -oldVersCFG $DT_SCRIPTS/${SUBJ_NAME}-results/selectionOutput-firstVers \
  -newVersCFG $DT_SCRIPTS/${SUBJ_NAME}-results/selectionOutput-subseqVers \
  -getCoverage \
  -outputDir $DT_SCRIPTS/${SUBJ_NAME}-results/ \
  -timesToRun $medianTimes \
  -classpath "$CLASSPATH" \
  -postProcessDTs \
  ${dep_file} "${precomputed_deps}"
rm .tmp_file
