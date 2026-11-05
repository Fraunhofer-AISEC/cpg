# Porting the PointsToPass: what is known

Status (see `git log`): stages 1 and 2 are ported and identical to the Kotlin CPG on the probes
`pt/scalar.c`, `pt/fields.c`, `pt/structs.c`. Overall match against the Kotlin goldens of all C tests
and probes (DFG edges, `DFG_MODE=pointsto`): 77.8 % / 89.2 % of the nodes. The rest needs pointers and
calls.

## How to compare

```bash
cd experiments/cpg-rs && cargo build --release
cd oracle
# Kotlin goldens (needs a checkout of the Kotlin CPG; DUMP_CTX prints calling contexts and memory nodes)
CPG_ROOT=<checkout> DUMP_ACCESS=1 DUMP_CTX=1 ./run_kotlin_dump.sh <dir with .c> <out dir> pointsto
# compare
DFG_MODE=pointsto SRC=<dir with .c> GOLD=<golden dir> python3 diff.py cls,name,loc,dfg [file.c]
```

## What the Kotlin pass computes (decoded from the goldens)

Flow sensitive reads and writes over the EOG, a fixpoint per function, defs take effect at the
`Assign` / `++` / `--` node (after the right hand side). The DFG pass only does *global* references
when the pass is registered.

- **Scalars**: reaching definitions. Defs: a `Variable` with its declaration, a `Parameter` at entry,
  a written `Reference`. A read gets an edge from every reaching def. `Return` -> `Function`.
  Implicit constructors: `this` -> `Constructor`.
- **Fields** (`p.x`): a write defines the *path* (`Field`), and every prefix of the written path is a
  partial definition of its parent (the base reference / inner member access, granularity
  `Part(Field:<name>)`). A read of the whole variable gets the declaration plus the top level partial
  definitions. A member read with no defs (and no partial defs for its sub-fields) gets the *field
  declaration* as its value. A whole assignment `q = p` does not copy field defs: it replaces
  everything known about `q`.
- **Pointers** (`pt/ptr1.c`, `pt/ptr2.c`, goldens in `goldens/pointsto/probes`, not ported yet):
  - `&x` gets `MemoryAddress(x)` (a synthetic node) and the current defs of `x`.
  - A write through `*p` is a def of what `p` points to: a later read of `x` gets that write
    (aliasing), not the initialization. A read `*p` gets the last write through `*p`.
  - A read of a pointer variable `p` gets its defs *and* the defs written through it (`#11` in ptr1).
  - A pointer parameter `q` points to synthetic `ParameterMemoryValue` nodes (`q.derefvalue`,
    `q.derefderefvalue`); reads of `q` and `*q` get them. A local read can also list such a node if
    its def's value came from one (`a = *q` -> `a` lists `derefderefvalue`).

## The Kotlin implementation (cpg-core `passes/PointsToPass.kt`, ~5,300 lines)

State = `PointsToState(generalState: Node -> (addresses, memoryValues, lastWrites),
declarationState: address -> (addresses, values (node, shortFS), lastWrites))`, all powerset lattices.

| Part | Lines |
|---|---|
| configuration (timeout, limits, k of the call strings) | ~550-740 |
| `accept` / `acceptInternal` (per function, summaries, timeouts) | 831-1160 |
| `handleAssign`, `updateBaseLastWrites` | 3547-3660 |
| `handleExpression` | 3663-3800 |
| `handleDeclaration` | 3803-4170 |
| `handleUnaryOperator` | 3483-3545 |
| `calculatePrevDFGs`, `addEntryToMap` | 2694-2740, 2955-3100 |
| `getFromDecl`, `fetch*`, `getLastWrites`, `getValues`, `getAddresses`, `getNestedValues` | 4222-5269 |
| calls: `handleCall`, `calculateFunctionSummaries`, `calculateCallDestinations`, `addParameterInfoToFS` | 2479-3470, 1360-1700 |
| `handleReturn`, calling contexts | 1864-2480 |

## Plan

1. Re-found the Rust analysis on the same general/declaration state (addresses, values, last writes)
   and keep `scalar`, `fields`, `structs` as regression tests.
2. Pointers (address-of, dereference, parameter memory values), then arrays.
3. Calls: summaries (also the YAML ones of the stdlib profile), calling contexts.
4. Limits (state size, summary size, k-limiting) from the start of step 3: in the Kotlin profile of
   sqlite3.c 399 functions (callers high in the call graph) hit the 1 s timeout and cost ~440 s.
