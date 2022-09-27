#!/bin/bash

if [[ $1 == "" ]]; then
    echo "arg1 - The algorithm to run. Valid options are T1 (prioritization, statement, absolute), T2 (prioritization, statement, relative), T3 (prioritization, function, absolute), T4 (prioritization, function, relative), S1 (selection, statement, original), S2 (selection, statement, absolute), S3 (selection, statement, relative), S4 (selection, function, original), S5 (selection, function, absolute), S6 (selection, function, relative), P1 (parallelization, original), and P2 (parallelization, time)."
    echo "arg2 (optional) - Number of machines to simulate for parallelization. Valid otpions are 2, 4, 8, and 16."
    exit 1
fi

scripts_folder=$(cd "$(dirname $BASH_SOURCE)"; pwd)
algo=$1
machines=$2

# Clear any existing logs
rm -rf ${scripts_folder}/logs/
mkdir -p ${scripts_folder}/logs/

# Clone the firstVers if it doesn't exist
if [[ ! -d "firstVers" ]]; then
    echo "Cloning firstVers"
    git clone https://github.com/kevinsawicki/http-request.git firstVers &> ${scripts_folder}/logs/firstVers-clone-log.txt
    echo "Compiling firstVers"
    cd firstVers/lib
    git checkout d0ba95cf3c621c74a023887814e8c1f73b5da1b2 &> ${scripts_folder}/logs/checkout-firstVers.txt
    mvn install dependency:copy-dependencies -DskipTests &> ${scripts_folder}/logs/install-log-firstVers.txt
    cd ${scripts_folder}
fi

# update dt-impact-tracer

cd dt-impact-tracer/
mvn install -DskipTests &> ../logs/dt-impact-tracer-install.log
cd ..
mv dt-impact-tracer/target/dt-impact-tracer-1.0.5.3.jar shared/impact-tools/dt-impact-tracer-1.0.5.3.jar

mv dt-impact-tracer/target/dt-impact-tracer-1.0.5.3-jar-with-dependencies.jar shared/impact-tools/dt-impact-tracer-1.0.5.3-jar-with-dependencies.jar

# Clear any existing results
rm -rf lib-results/
rm -rf firstVers/lib/target/

cd firstVers/lib
git checkout d0ba95cf3c621c74a023887814e8c1f73b5da1b2 &> ${scripts_folder}/logs/checkout-firstVers.txt
git log -2 --pretty=format:"%h" &> ${scripts_folder}/logs/commitlist-firstVers.txt
mvn install dependency:copy-dependencies -DskipTests &> ${scripts_folder}/logs/install-log-firstVers.txt

echo -e "" >>  ${scripts_folder}/logs/commitlist-firstVers.txt

cd ${scripts_folder}

declare -A commitArray
num=-1

echo "Setting up the two versions for data generation"

while read line; do  
    #Reading each line  
    commitArray[$((++num))]=$line
    cd ${scripts_folder}
    cd firstVers/lib
    git checkout $line &> ${scripts_folder}/logs/checkout-$line-firstVers.txt
    mvn install dependency:copy-dependencies -DskipTests &> ${scripts_folder}/logs/install-log-$line.txt
    cd ${scripts_folder}
    rm -rf $line
    cp -r firstVers $line
    bash generate.sh $line/lib $algo firstVers/lib &> logs/setup.txt
done < ${scripts_folder}/logs/commitlist-firstVers.txt

p=${commitArray[0]}/lib
q=${commitArray[1]}/lib

source shared/set-vars.sh "$p" "$algo" "$q"

cd $DT_SUBJ_SRC

git diff ${commitArray[0]} ${commitArray[1]} --name-only &> ${scripts_folder}/logs/diff.txt
echo -e "" >>  ${scripts_folder}/logs/diff.txt

while read c_file; do
    java -cp $DT_TOOLS: GeneratingAST \
	 -inputFile $c_file \
	&> ${scripts_folder}/logs/generate_ast.txt
done < ${scripts_folder}/logs/diff.txt


java -cp $DT_TOOLS: FileCompare \
     -firstFile $DT_SCRIPTS/${SUBJ_NAME}-results/selectionOutput-${commitArray[0]}/ \
     -secondFile $DT_SCRIPTS/${SUBJ_NAME}-results/selectionOutput-${commitArray[1]}/ \
    &> ${scripts_folder}/logs/compare.txt

cd ${scripts_folder}

# echo "Two versions"

#bash setup.sh firstVers/lib $algo secondVers/lib &> logs/setup.txt
#echo "Running the unenhanced regression testing algorithm on the secondVers"
#bash run.sh firstVers/lib $algo secondVers/lib "$machines" &> logs/run-unenhanced.txt
#echo "Computing dependencies on the firstVers"
#bash compute-deps.sh firstVers/lib $algo secondVers/lib "$machines" &> logs/compute-deps.txt
#echo "Running the enhanced regression testing algorithm on the secondVers"
#bash run.sh firstVers/lib $algo secondVers/lib "$machines" &> logs/run-enhanced.txt
