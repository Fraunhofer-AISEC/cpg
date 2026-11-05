#!/bin/bash
# Dumps the Kotlin CPG of a directory of C files in the oracle format.
#
#   CPG_ROOT=<checkout of the Kotlin CPG> ./run_kotlin_dump.sh <input dir> <output dir> [passes]
#
# passes (comma separated, default: none): dfg = add the DFG pass, pointsto = add the PointsToPass.
# Environment: DUMP_ACCESS=1 prints access values, DUMP_CTX=1 prints calling contexts and names of
# memory nodes. The oracle test is copied into the checkout for the run and removed again.
set -e
: "${CPG_ROOT:?set CPG_ROOT to a checkout of the Kotlin CPG}"
in=$(cd "$1" && pwd); out="$2"; export DUMP_PASSES="${3:-}"
here=$(cd "$(dirname "$0")" && pwd)
test_file="$CPG_ROOT/cpg-language-cxx/src/test/kotlin/de/fraunhofer/aisec/cpg/frontends/cxx/TmpDump.kt"
cp "$here/OracleDump.kt.txt" "$test_file"
trap 'rm -f "$test_file"' EXIT
rm -rf "$out"
export DUMP_DIR="$in" DUMP_OUT_DIR="$(mkdir -p "$out" && cd "$out" && pwd)"
cd "$CPG_ROOT"
./gradlew :cpg-language-cxx:test --rerun --tests "*TmpDump*" \
  -PenableGoFrontend=false -PenablePythonFrontend=false -PenableLLVMFrontend=false \
  -PenableTypeScriptFrontend=false -PenableRubyFrontend=false -PenableJVMFrontend=false \
  -PenableINIFrontend=false -PenablePHPFrontend=false -PenableRustFrontend=false \
  -PenableJavaFrontend=false > /tmp/run_kotlin_dump.log 2>&1 || { tail -20 /tmp/run_kotlin_dump.log; exit 1; }
ls "$DUMP_OUT_DIR" | wc -l | xargs echo "dumped files:"
