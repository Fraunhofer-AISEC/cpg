# cpg-rs: a Rust port of the C frontend and EOG (experiment)

An experiment, not part of the Gradle build. It re-implements, in Rust, what the Kotlin CPG does
for C: preprocessing, translation of a tree-sitter-c syntax tree into CPG nodes, symbol/member/call
resolution with expression types, and the evaluation order graph (EOG). The Kotlin implementation is
the **oracle**: every behaviour (including quirks) is compared node by node, and the differences are
either a gap in the port or a bug in the Kotlin code (see `oracle/FINDINGS.md`).

Motivation: for `sqlite3.c` (270 kLOC) the Kotlin C frontend needs ~45 s, of which CDT parsing is
only ~0.5-1 s; the rest is CPG node construction. The Rust port builds the same graph (all passes
listed below) in well under a second on one thread. These numbers are for one file and one machine.

## Layout

| Crate | Purpose |
|---|---|
| `cpg-core` | `Kind`, `Node`, `Graph` (arena), EOG edges, the dump format. No parser dependency. |
| `cpg-pp` | Offset-preserving preprocessor: conditionals, safe object-like macros, macros from a `Profile.symbols()`-style map (`NAME` / `NAME(a,b)` -> body). |
| `cpg-c` | tree-sitter-c frontend, split like the Kotlin handlers (`decl`, `expr`, `stmt`, `types`, `translate`). |
| `cpg-passes` | `eog` (port of `EvaluationOrderGraphPass`), `resolve` (scopes), `typing` (expression types, members, call resolution). |
| `cpg-ffi` | C ABI + flat AST export (for trying FFM from Java). |
| `cpg-tools` | `cpgdump` (prints the graph in the oracle format), `cpgbench`, `ppbench`, `errs`, `flatbench`. |

## Build and try

```bash
cd experiments/cpg-rs
cargo build --release
cargo test --release                       # preprocessor tests
./target/release/cpgdump path/to/file.c    # graph in the oracle format
./target/release/cpgbench path/to/big.c    # timings per phase
```

## Compare against the Kotlin CPG

`oracle/OracleDump.kt.txt` is a (throwaway) test that dumps the Kotlin CPG of a directory of C files
after the frontend plus `ImportResolver`, `TypeResolver`, `TypeHierarchyResolver`,
`EvaluationOrderGraphPass` and `SymbolResolver`, with inference disabled. To regenerate the goldens:

```bash
cp experiments/cpg-rs/oracle/OracleDump.kt.txt \
   cpg-language-cxx/src/test/kotlin/de/fraunhofer/aisec/cpg/frontends/cxx/TmpDump.kt
DUMP_DIR=cpg-language-cxx/src/test/resources/c \
DUMP_OUT_DIR=experiments/cpg-rs/oracle/goldens/c-tests/run1 \
  ./gradlew :cpg-language-cxx:test --rerun --tests "*TmpDump*"
rm cpg-language-cxx/src/test/kotlin/de/fraunhofer/aisec/cpg/frontends/cxx/TmpDump.kt   # never commit it
```

Then `python3 experiments/cpg-rs/oracle/diff.py cls,name,loc,type,refersTo,invokes,eog` compares
class, name, location, type and the `refersTo`, `invokes` and EOG edges (targets compared by their
class/name/location, so ids do not matter). Several golden runs can be given (`GOLD=a:b:c`): the
Kotlin output is not deterministic when a call has several candidate functions (finding #23), and
the best matching run is used per file. `oracle/probes/` holds extra C files for constructs the
test resources do not cover (control flow, goto, ternary, sizeof, ...).

## State

On the 27 top-level C files of `cpg-language-cxx/src/test/resources/c` all compared fields match
for 99.5 % of the Kotlin nodes (precision 99.7 %); on the probes 99.4 %. The known differences are
gaps of tree-sitter-c (implicit `int` return type, `(type)*p` cast-vs-multiplication ambiguity)
and the non-determinism above. Not ported: the DFG and later passes, C++, include handling.
