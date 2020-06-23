#!/bin/bash

# To minimize the time it would take to compute selection dependencies, this script only uses one algorithm (e.g., P1 for S1) to compute the selection dependencies. For our paper, we used all prioritization and parallelization dependencies for all selection algorithms.

set -e

if [[ "$ALGO" == "s1" ]] || [[ "$ALGO" == "s4" ]]; then
    export TECH="para"
    bash $DT_SCRIPTS/compute-deps/compute-deps-para.sh
elif [[ "$ALGO" == "s2" ]] || [[ "$ALGO" == "s3" ]] || [[ "$ALGO" == "s4" ]] || [[ "$ALGO" == "s5" ]] || [[ "$ALGO" == "s6" ]]; then
    export TECH="prio"
    bash $DT_SCRIPTS/compute-deps/compute-deps-prio.sh
else
    echo "Unknown $ALGO selection algorithm given. Cannot compute dependencies."
    exit 1
fi
