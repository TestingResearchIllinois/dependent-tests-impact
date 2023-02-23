#!/bin/bash

if [[ "$JAVA_HOME" == "" ]]; then
    echo "ERROR: JAVA_HOME is not set"
    exit 1
fi

# ================ firstVers variables

export DT_SUBJ_ROOT=$(realpath $1)

if [[ ! -d "$DT_SUBJ_ROOT" ]]; then
    echo "Path to DT_SUBJ_ROOT given as argument 1 does not exist. DT_SUBJ_ROOT: $DT_SUBJ_ROOT"
    exit 1
fi

export DT_SUBJ_SRC="$DT_SUBJ_ROOT"
export DT_SUBJ="$DT_SUBJ_ROOT/target"
if [[ ! -d "$DT_SUBJ" ]]; then
    echo "Path to DT_SUBJ does not exist. You must run mvn mvn install -DskipTests in $DT_SUBJ first."
    exit 1
fi

# Directory for libraries of the old subject
libs_dir=$DT_SUBJ/dependency
if [[ ! -d "$libs_dir" ]]; then
    echo "Path to DT_LIBS does not exist. You must run mvn dependency:copy-dependencies in $DT_SUBJ_ROOT first."
    exit 1
fi
LIBS=$(find "$DT_SUBJ/dependency/" -name "*.jar" -not -name "junit*.jar")
export DT_LIBS=$(echo $LIBS | sed -E "s/ /:/g")

# Compiled class files of the old subject
export DT_CLASS=$DT_SUBJ/classes
if [[ ! -d "$DT_CLASS" ]]; then
    echo "Path to DT_CLASS does not exist. You must run mvn mvn install -DskipTests in $DT_SUBJ_ROOT first."
    exit 1
fi

# Compiled human-written test files of the old subject
export DT_TESTS=$DT_SUBJ/test-classes
if [[ ! -d "$DT_TESTS" ]]; then
    echo "Path to DT_TESTS does not exist. You must run mvn mvn install -DskipTests in $DT_SUBJ_ROOT first."
    exit 1
fi

# ================ subseqVers variables
export NEW_DT_SUBJ_ROOT=$(realpath $3)

if [[ ! -d "$NEW_DT_SUBJ_ROOT" ]]; then
    echo "Path to NEW_DT_SUBJ_ROOT given as argument 1 does not exist. NEW_DT_SUBJ_ROOT: $NEW_DT_SUBJ_ROOT"
    exit 1
fi

export NEW_DT_SUBJ_SRC="$NEW_DT_SUBJ_ROOT"
export NEW_DT_SUBJ="$NEW_DT_SUBJ_ROOT/target"
if [[ ! -d "$NEW_DT_SUBJ" ]]; then
    echo "Path to NEW_DT_SUBJ does not exist. You must run mvn mvn install -DskipTests in $NEW_DT_SUBJ_ROOT first."
    exit 1
fi

# Directory for libraries of the new subject
libs_dir=$NEW_DT_SUBJ/dependency
if [[ ! -d "$libs_dir" ]]; then
    echo "Path to NEW_DT_LIBS does not exist. You must run mvn dependency:copy-dependencies in $NEW_DT_SUBJ_ROOT first."
    exit 1
fi
LIBS=$(find "$NEW_DT_SUBJ/dependency/" -name "*.jar" -not -name "junit*.jar")
export NEW_DT_LIBS=$(echo $LIBS | sed -E "s/ /:/g")

# Compiled class files of the new subject
export NEW_DT_CLASS=$NEW_DT_SUBJ/classes
if [[ ! -d "$NEW_DT_CLASS" ]]; then
    echo "Path to NEW_DT_CLASS does not exist. You must run mvn mvn install -DskipTests in $NEW_DT_SUBJ_ROOT first."
    exit 1
fi

# Compiled human-written test files of the new subject
export NEW_DT_TESTS=$NEW_DT_SUBJ/test-classes
if [[ ! -d "$NEW_DT_TESTS" ]]; then
    echo "Path to NEW_DT_TESTS does not exist. You must run mvn mvn install -DskipTests in $NEW_DT_SUBJ_ROOT first."
    exit 1
fi

# ================ DT_SCRIPTS variables

export DT_SCRIPTS="$(cd "$(dirname $BASH_SOURCE)"; pwd)/../"
if [[ ! -d "$DT_SCRIPTS" ]]; then
    echo "Path to DT_SCRIPTS does not exist. DT_SCRIPTS: $DT_SCRIPTS"
    exit 1
fi

tools_dir=$DT_SCRIPTS/shared/impact-tools
if [[ ! -d "$tools_dir" ]]; then
    echo "Path to DT_TOOLS does not exist. Please ensure that you cloned $DT_SCRIPTS with the impact-tools/ directory."
    exit 1
fi
TOOLS=$(find "$DT_SCRIPTS/shared/impact-tools/" -name "*.jar" -not -name "randoop.jar")
export DT_TOOLS=$(echo $TOOLS | sed -E "s/ /:/g")

export SUBJ_NAME="$(echo $DT_SUBJ_ROOT | rev | cut -d'/' -f1 | rev)"
export VER_NAME="$(echo $DT_SUBJ_ROOT | rev | cut -d'/' -f2 | rev)"

export ALGO=$(echo "$2" | tr '[:upper:]' '[:lower:]')
if [[ "$ALGO" == "t1" ]]; then
    export i="statement"
    export j="absolute"
    export TECH="prio"
elif [[ "$ALGO" == "t2" ]]; then
    export i="statement"
    export j="relative"
    export TECH="prio"
elif [[ "$ALGO" == "t3" ]]; then
    export i="function"
    export j="absolute"
    export TECH="prio"
elif [[ "$ALGO" == "t4" ]]; then
    export i="function"
    export j="relative"
    export TECH="prio"
elif [[ "$ALGO" == "s1" ]]; then
    export i="statement"
    export j="original"
    export TECH="sele"
elif [[ "$ALGO" == "s2" ]]; then
    export i="statement"
    export j="absolute"
    export TECH="sele"
elif [[ "$ALGO" == "s3" ]]; then
    export i="statement"
    export j="relative"
    export TECH="sele"
elif [[ "$ALGO" == "s4" ]]; then
    export i="function"
    export j="original"
    export TECH="sele"
elif [[ "$ALGO" == "s5" ]]; then
    export i="function"
    export j="absolute"
    export TECH="sele"
elif [[ "$ALGO" == "s6" ]]; then
    export i="function"
    export j="relative"
    export TECH="sele"
elif [[ "$ALGO" == "p1" ]]; then
    export j="original"
    export TECH="para"
elif [[ "$ALGO" == "p2" ]]; then
    export j="time"
    export TECH="para"
else
    echo "[ERROR] Unknown prioritization label provided. Valid options are T1 (prioritization, statement, absolute), T2 (prioritization, statement, relative), T3 (prioritization, function, absolute), T4 (prioritization, function, relative), S1 (selection, statement, original), S2 (selection, statement, absolute), S3 (selection, statement, relative), S4 (selection, function, original), S5 (selection, function, absolute), S6 (selection, function, relative), P1 (parallelization, original), and P2 (parallelization, time)."
    exit 1
fi

export MACHINES="$4"
if [[ "$MACHINES" != "2" ]] && [[ "$MACHINES" != "4" ]] && [[ "$MACHINES" != "8" ]] && [[ "$MACHINES" != "16" ]]; then
    export MACHINES="16"
fi

export medianTimes=1
