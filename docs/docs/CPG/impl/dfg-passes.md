# DFG Passes

The current implementation differentiates between three `Pass`es which are responsible for adding the data flow edges.
At first, one may be surprised why we need three Passes for one thing but the simple answer is: Performance.

## `DFGPass`

This is the most basic pass of all three and it draws DFG edges which are flow- and field-insensitive.

It constructs the DFG edges by iterating the abstract syntax tree exactly once. This is quite fast, it does not have to keep track of large states or remember information which are not already encoded in the graph, it does not even require the EOG. The only thing it needs is the AST and resolved symbols (well, the pass resolving symbols needs the EOG, so... bad luck).

Due to its simplicity, the pass is also a bit limited in its capabilities. It mostly draws DFG edges between a `Node` and its AST-children or its parent. But for almost all nodes in the graph, this is sufficient, so using the other passes for the same thing is just a massive waste of resources.

One thing has changed though: Originally, the `DFGPass` would draw all possible DFG edges as an over-approximation and other passes (i.e., the `ControlFlowSensitiveDFGPass`) would remove edges between `Reference`s and local variables to replace them with flow-sensitive versions. This is now no longer the case. Instead, the `DFGPass` checks if the `ControlFlowSensitiveDFGPass` or the `PointsToPass` will be running, and, if so, does not even draw these data flow edge. If none of the passes runs, it will draw additional data flow edges:
1. `Reference`s are connected to the local `Variable`s as an over-approximation.
2. Inter-procedural edges are added between `Parameter`s and arguments.

## `ControlFlowSensitiveDFGPass`

The `ControlFlowSensitiveDFGPass`, as the name suggests, draws DFG edges in a control-flow-sensitive way. In its core, this affects `Reference`s and local variables. To do so, it keeps a state of the last write access to a `Variable` and connects subsequent read accesses with it. It operates with a fixed-point-iteration and this means that it keeps some rather large states and it iterates a sub-graph multiple times. This pass only runs on EOGStarters.

In addition, for calls to functions which already have a function summary, it uses that summary to connect the last write to a parameter (or receiver) inside the invoked function to the corresponding argument (or base) at the call site, labelled with the calling context.

If the `PointsToPass` is used, this pass does not have any benefit and should be omitted. The two passes are meant to be run exclusively of one another: they compute overlapping, redundant results for the same features (flow-sensitive `Reference` edges and function-summary-based interprocedural edges), with the `ControlFlowSensitiveDFGPass` being the older, less precise implementation and the `PointsToPass` its successor. This exclusivity is currently a convention rather than something enforced by the pass infrastructure.

## `PointsToPass`

The `PointsToPass` is a more advanced version of the `ControlFlowSensitiveDFGPass`. Not only does it populate flow-sensitive data flow edges and add the inter-procedural edges between parameters of the function and arguments of the call of a function (as well as the propagation to modified arguments and return values), it also models pointer arithmetics to a certain extent.
In particular, it differentiates between pointer references and dereferences, and it understands if two variables may point to the same address (i.e., they are aliases and read/write the same data).
To provide an improved version of the interprocedural data flow analysis, it keeps track of function summaries. Unlike the other two passes, it also handles calls to functions for which no function summary is available (e.g., functions without a body): it falls back to a conservative dummy summary which assumes that every parameter may affect the return value, so that some interprocedural data flow is still modeled even in the absence of a real summary.

While this pass offers the most precise results, it is also the heaviest one among the three options.

## Feature ownership

The table below is an attempt at defining, per feature, which pass is strictly responsible for it ("Owner"), and which pass only contributes it as a fallback when its actual owner(s) are not registered ("Fallback"). This does not mean the passes never overlap in practice — where two passes are both marked "Owner", they compute the same feature redundantly and are meant to be run exclusively of one another (see above), rather than being combined.

| Feature | `DFGPass` | `ControlFlowSensitiveDFGPass` | `PointsToPass` | Notes |
|---|---|---|---|---|
| AST-structural edges (`Assign`, `Call` args→param, etc.) | Owner | | | Always runs |
| Local `Reference` flow-insensitive edges | Fallback | | | Only drawn if neither of the other two passes is registered |
| Local `Reference` flow-sensitive edges | | Owner | Owner | Mutually exclusive at runtime; not enforced by the pass infrastructure |
| Interprocedural param↔arg / return-value / modified-argument edges via an existing function summary | Fallback | Owner | Owner | |
| Interprocedural edges for functions without any function summary (e.g., no body) | | | Owner | Not handled by `DFGPass` or `ControlFlowSensitiveDFGPass`; `PointsToPass` falls back to a conservative dummy summary |
| Pointer reference/dereference, aliasing | | | Owner | Not modeled by the other two passes |
| Function summary computation | | | Owner | |

