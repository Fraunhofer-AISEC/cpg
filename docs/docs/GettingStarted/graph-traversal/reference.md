---
title: "Graph Traversal – Function Reference"
linkTitle: "Function Reference"
weight: 26
description: >
    Quick reference for all followXXX and collectAllXXX graph traversal functions.
---

# Function Reference

This page lists all graph traversal functions available in
`de.fraunhofer.aisec.cpg.graph`. See the [Overview & Parameters](index.md) page for a detailed
description of each parameter, or the [Examples](examples.md) page for annotated end-to-end
queries.

---

## DFG traversal

### `followDFGEdgesUntilHit` – most flexible

```kotlin
fun Node.followDFGEdgesUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    direction: AnalysisDirection = Forward(GraphToFollow.DFG),
    vararg sensitivities: AnalysisSensitivity = FieldSensitive + ContextSensitive,
    scope: AnalysisScope = Interprocedural(),
    ctx: Context = Context(steps = 0),
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths
```

The most flexible DFG traversal. All parameters can be freely configured; in particular, `direction`
and `sensitivities` are exposed so both forward/backward analysis and custom edge filters are
possible.

---

### `followNextFullDFGEdgesUntilHit` – convenience wrapper (forward, full edges)

```kotlin
fun Node.followNextFullDFGEdgesUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths
```

Equivalent to calling `followDFGEdgesUntilHit` with:

- `direction = Forward(GraphToFollow.DFG)`
- `sensitivities = OnlyFullDFG + ContextSensitive`
- `scope = Interprocedural()`

Use when you want forward DFG traversal that only follows full-granularity edges.

---

### `followPrevFullDFGEdgesUntilHit` – convenience wrapper (backward, full edges)

```kotlin
fun Node.followPrevFullDFGEdgesUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths
```

Equivalent to calling `followDFGEdgesUntilHit` with:

- `direction = Backward(GraphToFollow.DFG)`
- `sensitivities = OnlyFullDFG + ContextSensitive`
- `scope = Interprocedural()`

Use when you want backward DFG traversal (from a use back to its definition(s)) over full-granularity
edges only.

---

### Summary table – DFG

| Function | Direction | Sensitivities | Scope |
|---|---|---|---|
| `followDFGEdgesUntilHit` | Configurable (default: `Forward`) | `FieldSensitive + ContextSensitive` | `Interprocedural()` |
| `followNextFullDFGEdgesUntilHit` | `Forward` | `OnlyFullDFG + ContextSensitive` | `Interprocedural()` |
| `followPrevFullDFGEdgesUntilHit` | `Backward` | `OnlyFullDFG + ContextSensitive` | `Interprocedural()` |

---

## EOG traversal

### `followEOGEdgesUntilHit` – most flexible

```kotlin
fun Node.followEOGEdgesUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    direction: AnalysisDirection = Forward(GraphToFollow.EOG),
    vararg sensitivities: AnalysisSensitivity = FilterUnreachableEOG + ContextSensitive,
    scope: AnalysisScope = Interprocedural(),
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths
```

Traverses EOG edges. By default, unreachable EOG edges (e.g. dead code after an unconditional
`return`) are filtered out via the `FilterUnreachableEOG` sensitivity.

---

## PDG traversal

### `followNextPDGUntilHit` – forward, program-dependence edges

```kotlin
fun Node.followNextPDGUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    interproceduralAnalysis: Boolean = false,
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths
```

Follows program-dependence (PDG) edges *forward* from `this`. When `interproceduralAnalysis` is
`true`, the traversal also follows `CallExpression` invocations into called functions.

---

### `followPrevPDGUntilHit` – backward, program-dependence edges

```kotlin
fun Node.followPrevPDGUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    interproceduralAnalysis: Boolean = false,
    interproceduralMaxDepth: Int? = null,
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths
```

Follows program-dependence (PDG) edges *backward* from `this`. When `interproceduralAnalysis` is
`true`, the traversal also follows `FunctionDeclaration` usages back into callers, up to
`interproceduralMaxDepth` call levels (unlimited when `null`).

---

## CDG traversal

### `followNextCDGUntilHit` – forward, control-dependence edges

```kotlin
fun Node.followNextCDGUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    interproceduralAnalysis: Boolean = false,
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths
```

Follows control-dependence (CDG) edges *forward* from `this` to find nodes whose execution is
controlled by `this`.

---

### `followPrevCDGUntilHit` – backward, control-dependence edges

```kotlin
fun Node.followPrevCDGUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    interproceduralAnalysis: Boolean = false,
    interproceduralMaxDepth: Int? = null,
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths
```

Follows control-dependence (CDG) edges *backward* from `this` to find nodes that *control* whether
`this` is executed (e.g. the conditions of `if` statements).

---

### Summary table – PDG and CDG

| Function | Graph | Direction | `interproceduralAnalysis` | `interproceduralMaxDepth` |
|---|---|---|---|---|
| `followNextPDGUntilHit` | PDG | Forward | Optional (default `false`) | – |
| `followPrevPDGUntilHit` | PDG | Backward | Optional (default `false`) | Optional (default unlimited) |
| `followNextCDGUntilHit` | CDG | Forward | Optional (default `false`) | – |
| `followPrevCDGUntilHit` | CDG | Backward | Optional (default `false`) | Optional (default unlimited) |

---

## `collectAllXXX` helpers

When you want **all** nodes reachable from a starting node via a particular sub-graph (with no
target predicate), use the `collectAllXXX` helpers. They internally use a never-satisfied predicate
so that every explored path ends up in `failed` and is returned:

```kotlin
val allNextDFGPaths: List<NodePath>  = myNode.collectAllNextDFGPaths()
val allPrevDFGPaths: List<NodePath>  = myNode.collectAllPrevDFGPaths()
val allNextFullDFG:  List<NodePath>  = myNode.collectAllNextFullDFGPaths()
val allPrevFullDFG:  List<NodePath>  = myNode.collectAllPrevFullDFGPaths()
val allNextEOG:      List<NodePath>  = myNode.collectAllNextEOGPaths(interproceduralAnalysis = true)
val allPrevEOG:      List<NodePath>  = myNode.collectAllPrevEOGPaths(interproceduralAnalysis = false)
val allNextPDG:      List<NodePath>  = myNode.collectAllNextPDGGPaths()
val allPrevPDG:      List<NodePath>  = myNode.collectAllPrevPDGPaths(interproceduralAnalysis = false)
val allNextCDG:      List<NodePath>  = myNode.collectAllNextCDGPaths(interproceduralAnalysis = false)
val allPrevCDG:      List<NodePath>  = myNode.collectAllPrevCDGPaths(interproceduralAnalysis = false)
```

| Helper | Sub-graph | Direction |
|---|---|---|
| `collectAllNextDFGPaths` | DFG | Forward |
| `collectAllPrevDFGPaths` | DFG | Backward |
| `collectAllNextFullDFGPaths` | DFG (full edges only) | Forward |
| `collectAllPrevFullDFGPaths` | DFG (full edges only) | Backward |
| `collectAllNextEOGPaths(interproceduralAnalysis)` | EOG | Forward |
| `collectAllPrevEOGPaths(interproceduralAnalysis)` | EOG | Backward |
| `collectAllNextPDGGPaths` | PDG | Forward |
| `collectAllPrevPDGPaths(interproceduralAnalysis)` | PDG | Backward |
| `collectAllNextCDGPaths(interproceduralAnalysis)` | CDG | Forward |
| `collectAllPrevCDGPaths(interproceduralAnalysis)` | CDG | Backward |
