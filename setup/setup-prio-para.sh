#!/bin/bash

# Runs commands for "Instructions to setup a subject for test prioritization" section.
set -e

cd $DT_SUBJ
echo "[DEBUG] Removing existing sootTimer output"
cd ..
rm -rf sootTimerOutput/
rm -rf sootSeqOutput/
rm -rf sootException/
rm -rf sootTracerData/
rm -rf sootXMLOutput/
#rm -rf sootCsvOutput/
#echo "[DEBUG] checking vars"
#echo $DT_TESTS
#echo $DT_CLASS
#echo $DT_LIBS
#echo $DT_TOOLS
#echo "[DEBUG] checking 2nd vars"

#echo $NEW_DT_CLASS
#echo $NEW_DT_TESTS
#echo $NEW_DT_LIBS
#echo $NEW_DT_SUBJ_ROOT
echo $fullyQualifiedMethodNames
# 1. Find the human-written tests in the old subject.
cd $DT_SUBJ
echo "[DEBUG] Finding human written tests in old subject."
bash "$DT_SCRIPTS/shared/get-test-order.sh" old

# 2. Instrument the source and test files.
echo "[DEBUG] Instrumenting source and test files for old subject."
rm -rf sootOutput/
rm -rf methodOutput/
#echo "java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_TESTS --soot-cp $DT_LIBS:$DT_CLASS:$DT_TESTS:$JAVA_HOME/jre/lib/*"
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_TESTS --soot-cp $DT_LIBS:$DT_CLASS:$DT_TESTS:$JAVA_HOME/jre/lib/*

#echo "java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_CLASS --soot-cp $DT_LIBS:$DT_CLASS:$JAVA_HOME/jre/lib/*"
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $DT_CLASS --soot-cp $DT_LIBS:$DT_CLASS:$JAVA_HOME/jre/lib/*

rm -rf $DT_SCRIPTS/${SUBJ_NAME}-results/methodOutput-${VER_NAME}/
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

# 3. Run the instrumented tests.
echo "[DEBUG] Running instrumented tests."
cd $DT_SUBJ_SRC
echo "[DEBUG] java -cp $DT_TOOLS: edu.washington.cs.dt.impact.Main.RunnerMain -classpath $DT_LIBS:$DT_TOOLS:$DT_SUBJ/sootOutput/: -inputTests $DT_SCRIPTS/${SUBJ_NAME}-results/$SUBJ_NAME-orig-order"
java -cp $DT_TOOLS: edu.washington.cs.dt.impact.Main.RunnerMain -classpath $DT_LIBS:$DT_TOOLS:$DT_SUBJ/sootOutput/: -inputTests $DT_SCRIPTS/${SUBJ_NAME}-results/$SUBJ_NAME-orig-order> /home/pious/Documents/work/dependent-tests-impact/runneroutput
# -Djava.security.manager -Djava.security.policy=all_permissions.policy
echo "[DEBUG] Generating runtime for each test method under test cases"
java -cp $DT_TOOLS: edu.washington.cs.dt.impact.util.RuntimeGenerator -inputFile $DT_SUBJ/../ -inputName $DT_SUBJ



rm -rf $DT_SCRIPTS/${SUBJ_NAME}-results/sootTestOutput-orig
mv sootTestOutput/ $DT_SCRIPTS/${SUBJ_NAME}-results/sootTestOutput-orig
mv sootXMLOutput/ $DT_SCRIPTS/${SUBJ_NAME}-results/sootXML-${VER_NAME}/

echo "[DEBUG] Generating first-var vs second-var xml"
java -cp $DT_TOOLS:$JAVA_HOME/jre/lib/*: edu.washington.cs.dt.impact.Main.InstrumentationMain -inputDir $NEW_DT_TESTS --soot-cp $NEW_DT_LIBS:$NEW_DT_CLASS:$NEW_DT_TESTS:$JAVA_HOME/jre/lib/* -compare -outputPath $DT_SCRIPTS/${SUBJ_NAME}-results/ -changedFiles $fullyQualifiedMethodNames

echo "[DEBUG] Generating report for surefire vs our test result"
java -cp $DT_TOOLS: edu.washington.cs.dt.impact.util.RuntimeComparator -inputFile $DT_SUBJ/../ -inputName $DT_SCRIPTS/${SUBJ_NAME}-results/
mv sootCsvOutput/ $DT_SCRIPTS/${SUBJ_NAME}-results/sootComparedCSV-${VER_NAME}/


cd $DT_SUBJ
rm -rf sootOutput/

# 4. Get the time each test took to run.
cd $DT_SUBJ_SRC
echo "[DEBUG] Getting time for orig tests. $DT_SUBJ/$SUBJ_NAME-orig-time.txt"
java -cp $DT_TOOLS: edu.washington.cs.dt.impact.Main.RunnerMain -classpath $DT_LIBS:$DT_TOOLS:$DT_CLASS:$DT_TESTS: -inputTests $DT_SCRIPTS/${SUBJ_NAME}-results/$SUBJ_NAME-orig-order -getTime > $DT_SCRIPTS/${SUBJ_NAME}-results/$SUBJ_NAME-orig-time.txt
