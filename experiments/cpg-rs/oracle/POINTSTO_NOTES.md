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

## Decision (2026-11-05): faithful port, completeness first

All results of the Kotlin pass are kept; slower is acceptable. No precision-reducing approximations;
the limits are parameters with the Kotlin defaults (unlimited where Kotlin is) so that precision can
be traded for speed later. Speed only comes from representation, not from computing less.

## Core primitives, as read from the Kotlin code (not ported yet)

`getAddresses(node, startNode)`: a value declaration -> its `MemoryAddress` (created lazily and kept
in `node.memoryAddresses`); a reference -> the addresses of its declaration (a function is its own
address); `PointerReference` -> none; `PointerDereference` -> the *values* of its input (the address of
`*a` is the value of `a`; `a + literal` becomes a subscription); `MemberAccess` -> the field addresses
stored in the base's address (by resolved name); `Subscription` -> field addresses of the base values
named by the index (index `0` -> the base values); `Cast` -> its expression; `BinaryOperator` -> a
memory address of its own; anything else -> itself.

`getValues(node, startNode)`: `PointerReference` -> the addresses of its input; `PointerDereference` ->
the values stored at the values of its input (from the general state if the node is not the start node
or one of its children, else from the declaration state); a value declaration -> the values stored at
its addresses; `MemberAccess` -> the values of its field addresses, else those of the base, else an
`UnknownMemoryValue` (`getFieldValueOrBase`); `Reference` -> from the general state unless it is the
start node (or its child), else the values at its addresses, minus those that come from the assignment
it is part of; globals read the declaration state (and `globalDerefs`); `Cast` -> its expression;
`Subscription` -> field values or the base.

State updates: `push(general)` and `pushToDeclarationsState` only add what is not there yet
(they subtract the existing entries first) and then take the lub (union, in place).

Side tables that live outside of the state: `node.memoryAddresses`, `nodesCreatingUnknownValues`,
`syntheticOrigins`/`summaryNodes` (k-limiting of synthetic chains), `globalDerefs`. The pass creates
synthetic nodes (`MemoryAddress`, `UnknownMemoryValue`, `ParameterMemoryValue`) that are not in the AST.

## Rust design sketch

- The graph arena gets synthetic nodes (kind + name, flag `synthetic`), appended after the AST nodes,
  printed as `ext:<Class>(<name>)` like the oracle does.
- Facts (`NodeWithPropertiesKey`: node + a set of properties such as granularity, short-FS flag, calling
  context chain) are interned to ids, so that equality and hashing are integer operations. Calling
  contexts are interned chains.
- Sets of nodes/facts are sorted vectors or bitsets; the two state maps are persistent (structurally
  shared) so that a state per EOG node is cheap to copy.
- The configuration struct mirrors `PointsToPass.Configuration` with the Kotlin defaults.
- Determinism: insertion order is the iteration order wherever Kotlin iterates identity sets, so that
  the result does not depend on hashing (Kotlin's does).

## Size of the job

Primitives and state fetchers about 1,000 lines of Kotlin, the handlers (assign, expression,
declaration, unary, return, new, delete) about 800, calls (summaries, destinations, calling contexts,
function summaries of the stdlib YAML) about 2,000. The first milestone that is useful on its own is the
intraprocedural part: scalars, fields, pointers, `&`, `*`, arrays, without calls, verified on `pt/*.c`
and the intraprocedural parts of the Kotlin `PointsToPassTest` resources.

## Status (milestone 1: intraprocedural core, `crates/cpg-passes/src/pt.rs`)

Ported on the general/declaration-state model (`DFG_MODE=pointsto2`): state + `push`/`pushToDeclarationsState`,
`getAddresses`, `getValues`, `getLastWrites`, `fetchFieldAddresses`, `fetchValueFromDeclarationState`,
`syntheticNodeFor` (k-limiting), `handleDeclaration` (incl. array initializer lists), `handleAssign`,
`updateValues`, `updateBaseLastWrites`, `handleUnaryOperator`, `handleExpression` (incl. current-deref DFG),
`initializeParameter` (PMVs), `handleReturn` (edge to the function), `drawDataflowEdges`.
Config mirrors the Kotlin defaults (`PtConfig`).

Result against the Kotlin goldens (run with `PART_DETAIL=1` so partial targets print `Class:name`):
`scalar`, `fields`, `structs`, `ptr1`, `ptr2`, `access.c` identical; c-tests 92.9 %, probes 97.4 % (was
77.8 % / 89.2 %). All that is left are calls (call edges with calling contexts `<in:#n>/<out:#n>`, function
summaries, short FS `!fs` edges), `new`/`delete`, for-each, and record-aware initializer lists.
Next: milestone 2 = `handleCall` + function summaries + calling contexts (Kotlin 1360-3470).

## Milestone 2 (calls): what is read, and the plan

Read (Kotlin `PointsToPass.kt`): `handleCall` 2479-2692, `calculatePrevDFGs` 2694, `calculateFunctionSummaries` 2742,
`writeMapEntriesToState` 2853, `addEntryToMap` 2955, `createPropertySet` 3325, `calculateCallDestinations` 3368,
`calculateIncomingCallingContexts` 1999 + `innerCalculateIncomingCallingContexts` 2132, `handleReturn` FS part 1864,
`storeFunctionSummary` 1501, `addParameterInfoToFS` 1360, `mergeFSEntry` 772, `handleEmptyFunction` 1210,
`addDummyFunctionSummary` 1163, `FSEntry` (Function.kt:120; equality ignores lastWrites).

Structure of a call (`handleCall`), per invoked function (header declarations are dropped if the same name+type has a body):
1. `calculateFunctionSummaries`: analyze the callee first (recursion -> dummy summary; no body -> dummy summary: every
   parameter flows into the return / the function itself, entries with the short-FS flag).
2. `calculateIncomingCallingContexts`: arg -> Parameter (edge with `CallingContextIn([call])`), arg deref values -> deref PMVs
   (`derefvalue`, `derefderefvalue`) with the context.
3. per FS entry (depth 0..3, one depth at a time): `calculateCallDestinations` (where is written) + `addEntryToMap`
   (what is written, which last writes) into `mapDstToSrc`; then `writeMapEntriesToState` -> `updateValues(overwrite=false)`
   with `CallingContextOut([call])` appended to the last writes' context chains (not for short-FS entries).
4. short-FS edges: pushed as last writes (flag true) onto the call node.

Properties in the Rust `Props` that are still missing for this: the calling-context chain (In/Out lists of calls) and the
function-summary flag on edges (`!fs`); `DfgEdge` needs `fs` and `ctx` (dump prints `!fs` and `<in:#n>/<out:#n>`).

Open sub-project: the *detailed short FS* in `addParameterInfoToFS` uses `followDFGEdgesUntilHitNodes(Backward(DFG),
OnlyFullDFG+FieldSensitive+ContextSensitive, Interprocedural(maxCallDepth=1, maxSteps=10))`, i.e. the generic traversal engine
(`GraphTraversal.kt` `followXUntilHitNodes`, `AnalysisConfiguration.kt` `Backward.pickNextStep`/`filterEdges`/call-stack ops).
It has to be ported as a small, separate DFG walker in Rust, over the graph with the edges drawn so far.

Suggested order: (a) `DfgEdge` with fs+contexts, dump; (b) FS data structure (`FsEntry`, merge by equality ignoring lastWrites);
(c) bodiless callees + dummy FS; (d) callees with bodies: `storeFunctionSummary` without the detailed short FS,
incoming/outgoing contexts, destinations; (e) the DFG walker and the detailed short FS; (f) recursion, function pointers
(`addDynamicInvokesEdges`), methods/receivers (C has none).
