#!/bin/bash

# Example usage: bash generate.sh firstVers/lib t2 secondVers/lib

if [[ $1 == "" ]] || [[ $2 == "" ]] || [[ $3 == "" ]]; then
    echo "arg1 - Path to module that has its dependencies copied, and source and test compiled. This directory should contain the pom.xml and is the version to collect metadata for regression testing algorithms."
    echo "arg2 - The algorithm to run. Valid options are T1 (prioritization, statement, absolute), T2 (prioritization, statement, relative), T3 (prioritization, function, absolute), and T4 (prioritization, function, relative)."
    echo "arg3 - Path to module that has its dependencies copied, and source and test compiled. This directory should contain the pom.xml and is the version to run the regression testing algorithms."
    exit 1
fi

source shared/set-vars.sh "$1" "$2" "$3"

ret_code=$?
if [ $ret_code != 0 ]; then
    printf "Failed setting up variables. See error above."
    exit $ret_code
fi

mkdir -p $DT_SCRIPTS/${SUBJ_NAME}-results
echo "Starting setup for $TECH"
if [[ $TECH == "prio" ]] || [[ $TECH == "para" ]]; then
    bash $DT_SCRIPTS/generate/setup-prio-para.sh
elif [[ $TECH == "sele" ]]; then
    bash $DT_SCRIPTS/generate/setup-sele-firstVers.sh
    bash $DT_SCRIPTS/generate/setup-sele-subseqVers.sh
else
    echo "Unknown $TECH. Is $ALGO set correctly in shared/set-vars.sh?"
    exit 1
fi
