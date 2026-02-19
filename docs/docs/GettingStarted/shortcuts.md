---
title: "Shortcuts to Explore the Graph"
linkTitle: "Shortcuts to Explore the Graph"
weight: 20
no_list: false
menu:
  main:
    weight: 20
description: >
    The CPG library is a language-agnostic graph representation of source code.
---

# Shortcuts to Explore the Graph

When analyzing software, there are some information which are interesting to
explore. To facilitate accessing the information even without in-depth knowledge
about the graph and the graph model, we provide a number of shortcuts which can
be used on all nodes to find the nodes you're looking for.

All you have to do to use this functionality is to add the `import
de.fraunhofer.aisec.cpg.graph.*`.

## AST subtree traversal

It is often useful to find nodes which are in the AST subtree of another node.
We provide the following shortcuts to gain a quick overview of relevant types of
nodes:

Starting from node `n`...

* ...get all function/method calls with `n.calls`
* ...get all member calls (i.e., calls which are called on an object or class)
  with `n.mcalls`
* ...get all method declarations with `n.methods`
* ...get all function (and method) declarations with `n.functions`
* ...get all field declarations with `n.fields`
* ...get all parameters with `n.parameters`
* ...get all record declarations (e.g. classes, structs) with `n.records`
* ...get all namespaces with `n.namespaces`
* ...get all variables with `n.variables`
* ...get all literals with `n.literals`
* ...get all references to variables, fields, functions, etc. with `n.refs`
* ...get all assignments with `n.assignments`

## Filtering the results

The lists you get here can be quite long, and it's a good idea to filter them. To
do so, we provide different operators:

* To retrieve a single element, you can use the `[]` (get) operator and specify
  your criterion inside the brackets.
* To retrieve a single element and get an exception if there are multiple
  options, add the `SearchModifiers.UNIQUE` to the query.
* To retrieve a list of nodes, you can use the `()` (invokes) operator to
  specify your criterion.

Both notations allow you to quickly filter for the name by providing the
respective string or by accessing the fields and writing conditions on them.

Examples:
```kotlin
import de.fraunhofer.aisec.cpg.graph.*

// returns the first variable in the graph which has the name "a"
var a = result.variables["a"]

// returns the only variable with the name "a" or an exception otherwise
var theOnlyA = result.variables["a", SearchModifiers.UNIQUE]

// returns the first variable in the graph which does have an initializer
var anyWithInitializer = result.variables[{ it.initializer != null }]

// returns the only variable in the graph which does not have an initializer or throws an exception
var uniqueWithInitializer = result.variables[{ it.initializer != null }, SearchModifiers.UNIQUE]

// returns a list of all VariableDeclarations in the graph with the name "a"
var aList = result.variables("a")

// returns a list of FunctionDeclarations that have no parameter
var noArgs = result.functions { it.parameters.isEmpty() }
```

## More information needed?

In some cases, the AST-based traversals won't suffice to filter the nodes that
you're interested in. For this reason, there are a number of additional methods
which search for other patterns in the graph. Note that these are often less
stable than the information from above!

* The size of an array is evaluated using
  `SubscriptExpression.arraySize`. Unfortunately, this only works if the
  size is given in the initialization. Updates are not considered.
* Control dependencies are currently available via the extensions
  `Node.controlledBy()` and `IfStatement.controls()`.
*  `Node.eogDistanceTo(to: Node)` calculates the number of EOG edges between
   this node and `to`.
* `FunctionDeclaration.get(n: Int)`: Returns the n-th statement of the body of
  this function.
* `FunctionDeclaration.callees`: Returns the functions which are called from
  this function.
* `TranslationResult.callersOf(function: FunctionDeclaration)` determines which
  functions call the specified function.
* The methods
  ```kotlin
  Node.followEOGEdgesUntilHit(...)
  Node.followDFGEdgesUntilHit(...)
  ```
  and the related `followNextFullDFGEdgesUntilHit`, `followPrevFullDFGEdgesUntilHit`,
  `followNextPDGUntilHit`, `followPrevPDGUntilHit`, `followNextCDGUntilHit`, and
  `followPrevCDGUntilHit` enable fine-grained graph traversal. They return a
  `FulfilledAndFailedPaths` object containing all paths that reached a target node
  (`fulfilled`) and all paths that did not (`failed`). For a full description of all
  parameters (`direction`, `scope`, `sensitivities`, `earlyTermination`,
  `collectFailedPaths`, `findAllPossiblePaths`, `predicate`) and concrete usage
  examples, see the dedicated **[Graph Traversal (followXXX)](graph-traversal/index.md)** page.
* If you're interested in all nodes reachable via one of the sub-graphs from
  a certain node, the methods
  `Node.collectAllPrevFullDFGPaths()`,
  `Node.collectAllNextFullDFGPaths()`,
  `Node.collectAllPrevEOGPaths()`,
  `Node.collectAllNextEOGPaths()`,
  `Node.collectAllPrevCDGPaths(interproceduralAnalysis: Boolean)`,
  `Node.collectAllNextCDGPaths(interproceduralAnalysis: Boolean)`,
  `Node.collectAllPrevPDGPaths(interproceduralAnalysis: Boolean)`,
  `Node.collectAllNextPDGGPaths(interproceduralAnalysis: Boolean)`
  can be used.

