#!/bin/bash

# Runs commands for "Instructions to setup a subject for test selection" section.

set -e

echo "[DEBUG] Generate the static information needed by test selection for the new version of the subject."
cd $NEW_DT_SUBJ
rm -rf sootOutput/
rm -rf selectionOutput/
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain --soot-cp $NEW_DT_LIBS:$NEW_DT_CLASS:$JAVA_HOME/jre/lib/*: -inputDir $NEW_DT_CLASS -technique selection
rm -rf sootOutput/

rm -rf $DT_SCRIPTS/${SUBJ_NAME}-results/selectionOutput-subseqVers/
mv selectionOutput/ $DT_SCRIPTS/${SUBJ_NAME}-results/selectionOutput-subseqVers/
