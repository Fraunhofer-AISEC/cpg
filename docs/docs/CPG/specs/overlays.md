---
title: "Overlay Graph"
linkTitle: "Overlay Graph"
no_list: true
weight: 1
date: 2025-01-10
description: >
    Overlay Graph
---

# Overlay Graph

The CPG represents the code of a program as a graph of nodes $N_{CPG}$
and edges $E$.

Our basic version of the CPG only considers nodes that are part
of the CPG's immediate representation of the program's AST (we denote
these nodes as $N_{AST} \subseteq N_{CPG}$).

The edges $E$ represent various graph structures like the abstract
syntax tree (AST), data flow graph (DFG), the execution order (EOG),
call graph, and further dependencies among code fragments. Each of
the edges can have a predefined set of properties which is specified
by our graph schema.

However, this version of the CPG does not include any information
about the semantics of the code or consider expert knowledge on
certain framework or libraries.  This is, however, crucial information
for in-depth semantic analyses. To account for this, we introduce
the concept of an **Overlay Graph** which allows us to extend the graph
with expert knowledge or any other information which may not be directly
visible in the code.

We define an overlay graph as a set of nodes $N_O \subseteq N_{CPG}$,
where $\forall n_O \in N_O: n_O \not\in N_{AST}$. This means, we add nodes
which are not part of the CPG's AST. These overlay nodes are denoted by
extending the interface `de.fraunhofer.aisec.cpg.graph.OverlayNode` and
are connected via an edge to the  nodes in $N_{AST}$. The overlay nodes
may have additional edges and can fill  all known except from the AST edge.

## Concepts and Operations

One generic extension of the CPG can include **concepts** and
**operations** for which we provide the two classes
`de.fraunhofer.aisec.cpg.graph.concepts.Concept` and
`de.fraunhofer.aisec.cpg.graph.concepts.Operation` which can be extended.
We will incrementally add some nodes to the library within a dedicated
module.

Each concept aims to represent a certain "interesting" type of
behavior or somehow relevant information and can contain multiple
operations or interesting properties related to the same concept.
Operations always have to represent some sort of program behavior.

Typically, it makes sense to register custom passes which use the
information  provided by the plain version of the CPG and generate
new instances of a concept or operation when the pass identifies certain
patterns. This pattern may be a call of a specific function, a sequence
of functions, it may consider the values passed as arguments, or it may
also be a known sequence of operations.
