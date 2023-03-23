#!/bin/bash

# Runs the enhanced algorithms on kevinsawicki/http-request (M9).
# Example usage for T2 algorithm: bash example.sh t2
# Example usage for P2 algorithm with 8 machines: bash example.sh p2 8

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
    git clone https://github.com/kevinsawicki/http-request firstVers &> ${scripts_folder}/logs/firstVers-clone-log.txt
    echo "Compiling firstVers"
    cd firstVers
    git checkout 2d62a3e9da726942a93cf16b6e91c0187e6c0136 &> ${scripts_folder}/logs/checkout-firstVers.txt
    mvn install dependency:copy-dependencies -DskipTests &> ${scripts_folder}/logs/install-log-firstVers.txt
    cd ${scripts_folder}
fi
#cd firstVers
#git checkout b19048b72669fc0e96665b1b125dc1fda21f5993 &> ${scripts_folder}/logs/checkout-firstVers.txt
#mvn install dependency:copy-dependencies -DskipTests
#mvn install dependency:copy-dependencies -DskipTests -pl dropwizard-logging/ -am &> ${scripts_folder}/logs/install-log-firstVers.txt
#cd ${scripts_folder}
# Clone the secondVers if it doesn't exist
if [[ ! -d "secondVers" ]]; then
    echo "Cloning secondVers"
    git clone https://github.com/kevinsawicki/http-request secondVers &> ${scripts_folder}/logs/secondVers-clone-log.txt
    echo "Compiling secondVers"
    cd secondVers/lib
    git checkout ef89ec663e6d192c08b77dd1d9b8649975c1419c &> ${scripts_folder}/logs/checkout-secondVers.txt
    mvn install dependency:copy-dependencies -DskipTests &> ${scripts_folder}/logs/install-log-secondVers.txt
    cd ${scripts_folder}
fi
# Clear any existing results
rm -rf lib-results/

#rm -rf lib-results/methodOutput-plugins
#rm -rf lib-results/sootComparedCSV-plugins
#rm -rf lib-results/sootTestOutput-orig
#rm -rf lib-results/sootXML-plugins

echo "Setting up the two versions for regression testing"
bash setup.sh firstVers/lib $algo secondVers/lib &> logs/setup.txt
#echo "Running the unenhanced regression testing algorithm on the secondVers"
#bash run.sh firstVers/lib $algo secondVers/lib "$machines" &> logs/run-unenhanced.txt
#echo "Computing dependencies on the firstVers"
#bash compute-deps.sh firstVers/lib $algo secondVers/lib "$machines" &> logs/compute-deps.txt
#echo "Running the enhanced regression testing algorithm on the secondVers"
#bash run.sh firstVers/lib $algo secondVers/lib "$machines" &> logs/run-enhanced.txt
