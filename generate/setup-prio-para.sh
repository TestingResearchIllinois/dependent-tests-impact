#!/bin/bash

# Runs commands for "Instructions to setup a subject for test prioritization" section.

set -e

# 1. Find the human-written tests in the old subject.
cd $DT_SUBJ
echo "[DEBUG] Finding human written tests in old subject."
bash "$DT_SCRIPTS/shared/get-test-order.sh" old

# 2. Instrument the source and test files.
echo "[DEBUG] Instrumenting source and test files for old subject."
rm -rf sootOutput/

#echo "java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_TESTS --soot-cp $DT_LIBS:$DT_CLASS:$DT_TESTS:$JAVA_HOME/jre/lib/*"
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_TESTS --soot-cp $DT_LIBS:$DT_CLASS:$DT_TESTS:$JAVA_HOME/jre/lib/*

#echo "java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_CLASS --soot-cp $DT_LIBS:$DT_CLASS:$JAVA_HOME/jre/lib/*"
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_CLASS --soot-cp $DT_LIBS:$DT_CLASS:$JAVA_HOME/jre/lib/*

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

# 3. Run the instrumented tests.
echo "[DEBUG] Running instrumented tests."
cd $DT_SUBJ_SRC
#echo "[DEBUG] java -cp $DT_TOOLS: edu.washington.cs.dt.impact.Main.RunnerMain -classpath $DT_LIBS:$DT_TOOLS:$DT_SUBJ/sootOutput/: -inputTests $DT_SUBJ/$SUBJ_NAME-orig-order"
java -cp $DT_TOOLS: edu.washington.cs.dt.impact.Main.RunnerMain -classpath $DT_LIBS:$DT_TOOLS:$DT_SUBJ/sootOutput/: -inputTests $DT_SCRIPTS/${SUBJ_NAME}-results/$SUBJ_NAME-orig-order > /dev/null

rm -rf $DT_SCRIPTS/${SUBJ_NAME}-results/sootTestOutput-orig
mv sootTestOutput/ $DT_SCRIPTS/${SUBJ_NAME}-results/sootTestOutput-orig

cd $DT_SUBJ
rm -rf sootOutput/

# 4. Get the time each test took to run.
cd $DT_SUBJ_SRC
echo "[DEBUG] Getting time for orig tests. $DT_SUBJ/$SUBJ_NAME-orig-time.txt"
java -cp $DT_TOOLS: edu.washington.cs.dt.impact.Main.RunnerMain -classpath $DT_LIBS:$DT_TOOLS:$DT_CLASS:$DT_TESTS: -inputTests $DT_SCRIPTS/${SUBJ_NAME}-results/$SUBJ_NAME-orig-order -getTime > $DT_SCRIPTS/${SUBJ_NAME}-results/$SUBJ_NAME-orig-time.txt
