#!/bin/bash

# Example usage: bash run.sh firstVers/lib t2 secondVers/lib

if [[ $1 == "" ]] || [[ $2 == "" ]] || [[ $3 == "" ]]; then
    echo "arg1 - Path to module that has its dependencies copied, and source and test compiled. This directory should contain the pom.xml and is the version that metadata was collected for the regression testing algorithms."
    echo "arg2 - The algorithm to run. Valid options are T1 (prioritization, statement, absolute), T2 (prioritization, statement, relative), T3 (prioritization, function, absolute), T4 (prioritization, function, relative), S1 (selection, statement, original), S2 (selection, statement, absolute), S3 (selection, statement, relative), S4 (selection, function, original), S5 (selection, function, absolute), S6 (selection, function, relative), P1 (parallelization, original), and P2 (parallelization, time)."
    echo "arg3 - Path to module that has its dependencies copied, and source and test compiled. This directory should contain the pom.xml and is the version to run the regression testing algorithms."
    echo "arg4 (optional) - Number of machines to simulate for parallelization. Valid otpions are 2, 4, 8, and 16."
    exit 1
fi

source shared/set-vars.sh "$1" "$2" "$3" "$4"

ret_code=$?
if [ $ret_code != 0 ]; then
    printf "Failed setting up variables. See error above."
    exit $ret_code
fi

mkdir -p $DT_SCRIPTS/${SUBJ_NAME}-results
echo "Starting running algorithm $ALGO"
if [[ $TECH == "prio" ]]; then
    bash $DT_SCRIPTS/run/run-prio.sh
elif [[ $TECH == "sele" ]]; then
    bash $DT_SCRIPTS/run/run-sele.sh
elif [[ $TECH == "para" ]]; then
    bash $DT_SCRIPTS/run/run-para.sh
else
    echo "Unknown $TECH. Is $ALGO set correctly in shared/set-vars.sh?"
    exit 1
fi
