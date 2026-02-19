---
title: "Graph Traversal with followXXX Functions"
linkTitle: "Graph Traversal"
weight: 25
no_list: false
menu:
  main:
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

---

## Function reference

### DFG traversal

| Function | Direction | Sensitivities (default) | Notes |
|---|---|---|---|
| `followDFGEdgesUntilHit(...)` | Configurable (default: `Forward`) | `FieldSensitive + ContextSensitive` | Most flexible; exposes all parameters. |
| `followNextFullDFGEdgesUntilHit(...)` | `Forward` | `OnlyFullDFG + ContextSensitive` | Convenience wrapper. |
| `followPrevFullDFGEdgesUntilHit(...)` | `Backward` | `OnlyFullDFG + ContextSensitive` | Convenience wrapper. |

### EOG traversal

| Function | Direction | Sensitivities (default) | Notes |
|---|---|---|---|
| `followEOGEdgesUntilHit(...)` | Configurable (default: `Forward`) | `FilterUnreachableEOG + ContextSensitive` | Most flexible; exposes all parameters. |

### PDG / CDG traversal

| Function | Direction | `interproceduralAnalysis` | Notes |
|---|---|---|---|
| `followNextPDGUntilHit(...)` | Forward (PDG) | Optional (`false` by default) | Follow program-dependence edges forward. |
| `followPrevPDGUntilHit(...)` | Backward (PDG) | Optional + `interproceduralMaxDepth` | Follow program-dependence edges backward. |
| `followNextCDGUntilHit(...)` | Forward (CDG) | Optional (`false` by default) | Follow control-dependence edges forward. |
| `followPrevCDGUntilHit(...)` | Backward (CDG) | Optional + `interproceduralMaxDepth` | Follow control-dependence edges backward. |

### Collecting all reachable paths (no target predicate)

If you just want all nodes reachable from a starting node via a particular sub-graph, use the
`collectAllXXX` helpers, which internally use a never-satisfied predicate so that all paths end up
in `failed` and are returned:

```kotlin
val allNextDFGPaths: List<NodePath> = myNode.collectAllNextDFGPaths()
val allPrevFullDFGPaths: List<NodePath> = myNode.collectAllPrevFullDFGPaths()
val allNextEOGPaths: List<NodePath> = myNode.collectAllNextEOGPaths()
val allPrevEOGPaths: List<NodePath> = myNode.collectAllPrevEOGPaths(interproceduralAnalysis = false)
val allNextPDGPaths: List<NodePath> = myNode.collectAllNextPDGGPaths()
val allPrevPDGPaths: List<NodePath> = myNode.collectAllPrevPDGPaths(interproceduralAnalysis = false)
val allNextCDGPaths: List<NodePath> = myNode.collectAllNextCDGPaths(interproceduralAnalysis = false)
val allPrevCDGPaths: List<NodePath> = myNode.collectAllPrevCDGPaths(interproceduralAnalysis = false)
```

---

## Examples

### Example 1 – Check if a function argument always comes from a literal

```kotlin
// We want to know: does the first argument of every call to "encrypt" always
// originate from a string literal (no variable reassignment)?

val result = translationResult

for (call in result.calls["encrypt"]) {
    val arg = call.arguments[0]

    // Follow DFG backwards from the argument to find its origin(s)
    val (fulfilled, failed) = arg.followDFGEdgesUntilHit(
        direction = Backward(GraphToFollow.DFG),
        sensitivities = FieldSensitive + ContextSensitive,
        scope = Interprocedural(),
    ) { it is Literal<*> }

    if (failed.isEmpty()) {
        println("$call: argument always originates from a literal ✓")
    } else {
        println("$call: some paths do NOT originate from a literal ✗")
        for ((reason, path) in failed) {
            println("  Failed path ($reason): ${path.nodes}")
        }
    }
}
```

---

### Example 2 – Intraprocedural DFG analysis (stay within the current function)

Use `scope = Intraprocedural()` when you want to avoid following data flow across function-call
boundaries:

```kotlin
val (paths, _) = myNode.followDFGEdgesUntilHit(
    scope = Intraprocedural(),
    collectFailedPaths = false,  // we only care whether the flow exists
) { node ->
    node is CallExpression && node.name.localName == "sanitize"
}

if (paths.isNotEmpty()) {
    println("Data reaches sanitize() within the same function.")
}
```

---

### Example 3 – Stop traversal at function borders with `earlyTermination`

`earlyTermination` lets you abandon a path as soon as it would leave the current scope without
needing to set `scope = Intraprocedural()`. This is useful when you want interprocedural traversal
*except* for certain node types:

```kotlin
// Follow DFG forward across calls, but stop if the path ever reaches a
// FunctionDeclaration node (i.e., tries to enter a new function body)
val (paths, failed) = myNode.followDFGEdgesUntilHit(
    scope = Interprocedural(),
    earlyTermination = { nextNode, _ -> nextNode is FunctionDeclaration },
) { it is Literal<*> }

val earlyStops = failed.filter { it.first == FailureReason.HIT_EARLY_TERMINATION }
println("${earlyStops.size} path(s) were stopped at a function border.")
```

---

### Example 4 – Limit call depth

To perform interprocedural analysis but not go deeper than N call levels:

```kotlin
val (paths, _) = myNode.followDFGEdgesUntilHit(
    scope = Interprocedural(maxCallDepth = 3),
) { it is Literal<*> }
```

---

### Example 5 – EOG reachability: does execution always pass through a specific node?

```kotlin
// Does execution always reach a `ReturnStatement` from `startNode`?
val (fulfilled, failed) = startNode.followEOGEdgesUntilHit(
    direction = Forward(GraphToFollow.EOG),
    scope = Intraprocedural(),
) { it is ReturnStatement }

when {
    fulfilled.isNotEmpty() && failed.isEmpty() ->
        println("Execution ALWAYS reaches a return statement.")
    fulfilled.isNotEmpty() ->
        println("Execution CAN reach a return statement, but not on every path.")
    else ->
        println("Execution NEVER reaches a return statement.")
}
```

---

### Example 6 – PDG/CDG: find nodes that control execution of a target

```kotlin
// Which nodes control whether `myNode` is executed (backward CDG)?
val (fulfilled, _) = myNode.followPrevCDGUntilHit(
    interproceduralAnalysis = false,
) { it is IfStatement }

for (path in fulfilled) {
    println("Controlled by if-statement: ${path.nodes.last()}")
}
```

---

### Example 7 – Collecting all reachable DFG paths for further inspection

```kotlin
// Collect every path that data can take from myNode forward through the DFG
val allPaths: List<NodePath> = myNode.collectAllNextDFGPaths()

for (path in allPaths) {
    println("Path: ${path.nodes.joinToString(" -> ")}")
}
```
