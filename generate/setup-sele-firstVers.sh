#!/bin/bash

# Runs commands for "Instructions to setup a subject for test selection" section.

set -e

cd $DT_SUBJ
cd ..
rm -rf sootTimerOutput/
rm -rf sootSeqOutput/

cd $DT_SUBJ
echo "[DEBUG] Finding human written tests in old subject."
bash "$DT_SCRIPTS/shared/get-test-order.sh" old

# 1. Generate the static information needed by test selection for the old version of the subject.
cd $DT_SUBJ
rm -rf sootOutput/
rm -rf selectionOutput/
rm -rf methodOutput/

echo "[DEBUG] Instrumenting subject for selection (\$DT_TESTS)."
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_TESTS --soot-cp $DT_LIBS:$DT_CLASS:$DT_TESTS:$JAVA_HOME/jre/lib/*: -technique selection
echo "[DEBUG] Instrumenting subject for selection (\$DT_CLASS)."
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_CLASS --soot-cp $DT_LIBS:$DT_CLASS:$JAVA_HOME/jre/lib/*: -technique selection

rm -rf $DT_SCRIPTS/${SUBJ_NAME}-results/selectionOutput-${VER_NAME}/
rm -rf $DT_SCRIPTS/${SUBJ_NAME}-results/methodOutput-${VER_NAME}/
mv selectionOutput/ $DT_SCRIPTS/${SUBJ_NAME}-results/selectionOutput-${VER_NAME}/
mv methodOutput/ $DT_SCRIPTS/${SUBJ_NAME}-results/methodOutput-${VER_NAME}/

# Copy over any resource files from the classes/ and test-classes/ directories (e.g. configuration files).
# Make sure we don't copy any .class files though.
cd classes/
find . -iname "*.class" -printf "%P\n" > ../exclude-list.txt
cd ..
rsync -av classes/ sootOutput/ --exclude-from=exclude-list.txt

cd test-classes/
find . -name "*.class" -printf "%P\n" > ../exclude-list.txt
cd ..
rsync -av test-classes/ sootOutput/ --exclude-from=exclude-list.txt

# 2. Run the instrumented tests.
cd $DT_SUBJ_SRC
echo "[DEBUG] Running the instrumented human-written tests."

echo "[DEBUG] java -cp $DT_TOOLS: edu.washington.cs.dt.impact.Main.RunnerMain -classpath $DT_LIBS:$DT_TOOLS:$DT_SUBJ/sootOutput/: -inputTests $DT_SUBJ/$SUBJ_NAME-orig-order"

java -cp $DT_TOOLS: edu.washington.cs.dt.impact.Main.RunnerMain -classpath $DT_LIBS:$DT_TOOLS:$DT_SUBJ/sootOutput/: -inputTests $DT_SCRIPTS/${SUBJ_NAME}-results/$SUBJ_NAME-orig-order 
 

java -cp $DT_TOOLS: Runtime -inputFile $DT_SUBJ/../ -inputName $DT_SUBJ

rm -rf $DT_SCRIPTS/${SUBJ_NAME}-results/sootTestOutput-orig-selection
mv sootTestOutput/ $DT_SCRIPTS/${SUBJ_NAME}-results/sootTestOutput-orig
mv sootCsvOutput/ $DT_SCRIPTS/${SUBJ_NAME}-results/sootCSV-${VER_NAME}/

cd $DT_SUBJ
# rm -rf sootOutput/
