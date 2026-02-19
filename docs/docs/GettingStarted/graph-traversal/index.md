---
title: "Graph Traversal – Overview & Parameters"
linkTitle: "Graph Traversal"
weight: 25
description: >
    How to traverse DFG, EOG, PDG and CDG edges with the followXXX family of functions.
---

# Graph Traversal with `followXXX` Functions

The CPG library provides a powerful family of functions to traverse the various sub-graphs (DFG,
EOG, PDG, CDG) between nodes. All of them share a common design: you start from a node, configure
**how** you want to follow edges, and get back all paths that either reached a target node
(the *fulfilled* paths) or did not (the *failed* paths).

All functions reside in `de.fraunhofer.aisec.cpg.graph` and are available as extension functions on
`Node`. Make sure to add the following import to your analysis code:

```kotlin
import de.fraunhofer.aisec.cpg.graph.*
```

For a quick overview of all available functions, see the
[Function Reference](reference.md). For annotated end-to-end examples with C source code, see
[Examples](examples.md).

---

## Return type: `FulfilledAndFailedPaths`

Every `followXXX` function returns a `FulfilledAndFailedPaths` instance. It holds two lists:

| Property | Type | Description |
|---|---|---|
| `fulfilled` | `List<NodePath>` | Paths that ended at a node satisfying `predicate`. |
| `failed` | `List<Pair<FailureReason, NodePath>>` | Paths that ended without satisfying `predicate`. |

The `FailureReason` enum distinguishes why a path was not fulfilled:

| Value | Meaning |
|---|---|
| `PATH_ENDED` | No further edges could be followed (dead end or loop detected). |
| `STEPS_EXCEEDED` | The traversal exceeded the configured `maxSteps` limit. |
| `HIT_EARLY_TERMINATION` | The `earlyTermination` predicate returned `true` for a node on the path. |

`FulfilledAndFailedPaths` supports destructuring for convenience:

```kotlin
val (fulfilled, failed) = someNode.followDFGEdgesUntilHit { it is Literal<*> }
```

It also supports the `+` operator to merge results from several starting nodes:

```kotlin
val combined = result1 + result2
```

!!! tip "May vs. Must analysis"
    The two lists together enable both *may* and *must* reasoning:

    - If `fulfilled` is **non-empty** → a path to the target *can* exist (may analysis).
    - If `failed` is **empty** (and `fulfilled` is non-empty) → the target is reached on **every**
      path (must analysis / mandatory flow).

---

## Common parameters

All `followXXX` functions accept a subset of the parameters described below.

### `predicate`

```kotlin
predicate: (Node) -> Boolean
```

The only *required* parameter (it is passed as the last trailing lambda). It defines the **target**:
when a node satisfies this predicate, the path leading to it is added to `fulfilled` and traversal
along that branch stops.

```kotlin
// Find the nearest literal reachable via DFG from `myNode`
val (paths, _) = myNode.followDFGEdgesUntilHit { it is Literal<*> }
```

---

### `collectFailedPaths`

```kotlin
collectFailedPaths: Boolean = true
```

When `true` (the default), paths that do not reach a node satisfying `predicate` are collected in
`failed`. This is useful to check **whether every possible execution path** reaches the target (if
`failed` is empty, the flow is *mandatory*).

Set to `false` when you are only interested in whether a flow *exists at all* – this avoids the
overhead of collecting dead-end paths:

```kotlin
// We only care about fulfilled paths, skip collecting failed ones
val (paths, _) = myNode.followDFGEdgesUntilHit(collectFailedPaths = false) {
    it is Literal<*>
}
if (paths.isNotEmpty()) {
    println("Data can flow to a literal!")
}
```

---

### `findAllPossiblePaths`

```kotlin
findAllPossiblePaths: Boolean = true
```

When `true` (the default), the traversal explores every possible path through the graph, even if a
node has already been visited via a different path. This gives the most complete picture but can be
slow for large graphs.

Set to `false` to visit each `(Node, Context)` pair at most once. This is faster but may miss some
paths in the presence of loops or complex call chains:

```kotlin
// Fast single-visit traversal – may miss paths but sufficient for many analyses
val (paths, _) = myNode.followDFGEdgesUntilHit(findAllPossiblePaths = false) {
    it is Literal<*>
}
```

---

### `direction`

```kotlin
direction: AnalysisDirection = Forward(GraphToFollow.DFG)  // for followDFGEdgesUntilHit
direction: AnalysisDirection = Forward(GraphToFollow.EOG)  // for followEOGEdgesUntilHit
```

Controls the **direction** in which edges are traversed. Two concrete classes are available:

| Class | Effect |
|---|---|
| `Forward(GraphToFollow.DFG)` | Follow DFG edges from definitions to uses. |
| `Backward(GraphToFollow.DFG)` | Follow DFG edges from uses back to definitions. |
| `Forward(GraphToFollow.EOG)` | Follow EOG edges in execution order. |
| `Backward(GraphToFollow.EOG)` | Follow EOG edges against execution order. |

```kotlin
// Backward DFG: find all possible definitions that could reach `useNode`
val (paths, _) = useNode.followDFGEdgesUntilHit(
    direction = Backward(GraphToFollow.DFG),
) { it is VariableDeclaration }
```

The convenience wrappers `followPrevFullDFGEdgesUntilHit` and `followNextFullDFGEdgesUntilHit`
already set the direction for you.

---

### `sensitivities`

```kotlin
vararg sensitivities: AnalysisSensitivity = FieldSensitive + ContextSensitive  // DFG default
vararg sensitivities: AnalysisSensitivity = FilterUnreachableEOG + ContextSensitive  // EOG default
```

One or more `AnalysisSensitivity` objects that act as additional **edge filters** on top of the
scope. Multiple sensitivities are combined with `+`. All of them must agree for an edge to be
followed.

| Sensitivity | Graph | Effect |
|---|---|---|
| `FieldSensitive` | DFG | Tracks individual fields, known keys and indices of aggregates (e.g. array elements). |
| `ContextSensitive` | DFG / EOG | Respects the call stack, preventing data from "leaking" back through the wrong call site. |
| `OnlyFullDFG` | DFG | Skips partial-granularity DFG edges; only follows edges with `FullDataflowGranularity`. |
| `FilterUnreachableEOG` | EOG | Skips EOG edges that are statically unreachable (e.g. dead code after `return`). |
| `Implicit` | DFG | Also follows implicit information flows via PDG edges (e.g. control dependencies). |

```kotlin
// Follow only full DFG edges, context-sensitive, no field sensitivity
val (paths, _) = myNode.followDFGEdgesUntilHit(
    sensitivities = OnlyFullDFG + ContextSensitive,
) { it is Literal<*> }

// Include implicit flows via the PDG
val (paths, _) = myNode.followDFGEdgesUntilHit(
    sensitivities = FieldSensitive + ContextSensitive + Implicit,
) { it is Literal<*> }
```

---

### `scope`

```kotlin
scope: AnalysisScope = Interprocedural()
```

Controls **how far** the traversal follows edges across function boundaries.

| Class | Effect |
|---|---|
| `Intraprocedural(maxSteps?)` | Stays within the current function; never follows call/return edges. |
| `Interprocedural(maxCallDepth?, maxSteps?)` | Follows call/return edges across function boundaries. |
| `InterproceduralWithDfgTermination(maxCallDepth?, maxSteps?, allReachableNodes)` | Like `Interprocedural`, but stops entering a callee if none of the `allReachableNodes` are reachable in that callee's scope. |

Both `maxSteps` and `maxCallDepth` can be `null` (the default), meaning unlimited.

```kotlin
// Restrict the analysis to the current function body
val (paths, _) = myNode.followDFGEdgesUntilHit(
    scope = Intraprocedural(),
) { it is Literal<*> }

// Follow at most 2 call levels deep
val (paths, _) = myNode.followDFGEdgesUntilHit(
    scope = Interprocedural(maxCallDepth = 2),
) { it is Literal<*> }
```

---

### `earlyTermination`

```kotlin
earlyTermination: (Node, Context) -> Boolean = { _, _ -> false }
```

Called on each *candidate next node* (and the current `Context`) **before** it is explored further.
If it returns `true`, the path is immediately added to `failed` with reason
`FailureReason.HIT_EARLY_TERMINATION` and that branch is abandoned.

This is the idiomatic way to **stop traversal at a specific graph boundary without changing the
scope**. The `Context` parameter gives access to the current call stack depth, index stack, and step
counter, enabling dynamic decisions.

```kotlin
// Stop the DFG traversal whenever the path would enter a new function declaration
val (paths, _) = myNode.followDFGEdgesUntilHit(
    earlyTermination = { nextNode, _ -> nextNode is FunctionDeclaration },
) { it is Literal<*> }

// Stop after the path has visited more than 10 steps
val (paths, _) = myNode.followDFGEdgesUntilHit(
    earlyTermination = { _, ctx -> ctx.steps > 10 },
) { it is Literal<*> }
```

!!! note
    `scope = Intraprocedural()` is usually sufficient to stay within one function. Use
    `earlyTermination` when you need more fine-grained control, such as stopping at a specific node
    type or at a dynamic condition derived from the `Context`.

---

### `ctx` (advanced)

```kotlin
ctx: Context = Context(steps = 0)
```

The initial traversal context, holding the index stack, call stack, and step counter. The default
is usually sufficient. Supply a custom context when you want the analysis to start inside a
pre-defined call chain (e.g. you already know which `CallExpression` invoked the function where
`myNode` lives):

```kotlin
val context = Context.ofCallStack(callExpr1, callExpr2)
val (paths, _) = myNode.followDFGEdgesUntilHit(ctx = context) { it is Literal<*> }
```
