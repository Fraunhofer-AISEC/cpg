---
name: analysis-patterns
description: Use for questions about how something can happen in the code - how a statement is reached or triggered, under which conditions code runs, whether a value can reach a place, what influences or is influenced by code. Translates such questions into dataflow and dependence queries on the CPG.
---

# Analysis patterns: from a question to the graph

Questions about code are often asked as tasks ("how can I make X happen?", "can Y end up in Z?").
The CPG answers them, but only once the question is translated into a question about the graph:
**which nodes** (the target, the source), **which relation** (data or control) and **which
direction** (backward to the causes, forward to the effects). Do this translation first, then call
the tools. Do not answer from reading the code alone when a tool can show the relation.

## The graphs and the tools

The **program dependence graph (PDG)** connects statements by their dependences. It has two kinds
of edges, and each of them is a graph of its own:

- **Data dependence (DFG)**: a statement uses a value another statement computed. The label of
  the edge names the variable. Answers "where does a value come from / go to?".
- **Control dependence (CDG)**: a condition (an `if`, a loop, a `switch`) decides whether a
  statement runs. The label of the edge is the branch, `true` or `false`, the statement is on.
  Answers "under which condition does this run?".
- **PDG**: both kinds together. Answers "what does this depend on / affect at all?", but gives
  larger results. Use the narrowest graph that answers the question.

Two kinds of tools follow these edges:

- **`cpg_slice`** returns the statements around a node and how they depend on each other:
  - `graph`: `PDG`, `DFG` or `CDG`, see above.
  - `direction`: `backward` for what affects the node, `forward` for what it affects.
  - `hops`: how many dependences away the slice reaches, 2 by default. A statement with
    `more > 0` has further dependences that were cut off; raise `hops` (at most 5) or slice
    again from that statement if they matter for the question.
  - `interprocedural`: by default the slice follows dependences into called functions and back
    to the calls of a function. Set it to `false` to stay inside one function.
  - In the result, `depth` is the distance of a statement from the node, `kind` is `BRANCH` for
    conditions and loops and `STUB` for code that is not part of the analysis, and `external`
    or `unresolved` mark statements whose calls the analysis cannot follow. `truncated` means the
    slice hit its size limit.
- **`cpg_dfg_backward`** and **`cpg_dfg_forward`** return exact dataflow paths node by node, also
  across functions. Use them to prove that a value flows from A to B; use `cpg_slice` to find the
  conditions and the context around a statement.

## Step 1: find the nodes the question is about

- A function or call by its name: `cpg_list_calls_to` (calls of a function), `cpg_list_functions`,
  `cpg_list_calls`.
- Something by its meaning (logging, encryption, secrets, network, files, ...): first look at
  `cpg_list_concepts_and_operations`, the analysis may already have tagged it. Fall back to names.
- If the question names several candidates, handle each of them; say which ones you found.

## Step 2: pick the pattern

### "How can X happen / be triggered / be reached?", "Under which conditions does X run?"

Whether X runs is decided by control dependences, the values of the conditions by data
dependences.

1. Find the call or statement X (step 1).
2. `cpg_slice` with `graph=CDG`, `direction=backward` on X: the conditions that decide whether X
   runs, with the branch (`true`/`false`) X is on.
3. For each condition: where do its values come from? `cpg_dfg_backward` on the condition, or
   `cpg_slice` with `graph=DFG`, `direction=backward`. Follow them to their origins: parameters,
   return values of calls, input, configuration, constants.
4. If X is in a function that is not an entry point, find its callers with `cpg_list_calls_to`
   and repeat step 2 for the call, so that the conditions along the way are known.
5. Answer with the chain: what has to be true (or false) for X to run, and what decides it. Name
   origins outside of the analysed code (`STUB`, external calls) as such: the analysis cannot see
   behind them.

### "Can A reach B?", "Does A leak into B?", "Is A ever passed to B?"

A flow from A to B is a dataflow path; whether it happens at runtime is decided by the conditions
on the way.

1. Find the source A and the sink B (step 1).
2. `cpg_dfg_forward` from A; check whether a path ends at (an argument of) B.
3. If no path reaches B, say so, and name where the dataflow ends or leaves the analysed code.
4. If a path exists, check the conditions on the path (pattern above, `graph=CDG` on B) to say
   when the flow happens.

### "Where does the value of X come from?", "Who can influence X?"

Only data dependences matter here.

1. `cpg_dfg_backward` on X for the exact paths, or `cpg_slice` with `graph=DFG`,
   `direction=backward` for the statements around it.
2. Name the origins and whether they are outside of the analysed code.

### "What does X depend on?", "Why does X behave like this?"

Both values and conditions can explain X, so start with the whole PDG.

1. `cpg_slice` with `graph=PDG`, `direction=backward` on X: the statements whose values X uses and
   the conditions that decide whether X runs, in one view.
2. Follow the dependences that matter for the question with the patterns above (DFG for values,
   CDG for conditions) instead of describing every statement of the slice.

### "What does X affect?", "What happens if X changes?"

X affects the statements that use its value and the statements it decides on.

1. `cpg_slice` with `graph=PDG`, `direction=forward` on X.
2. Name the statements that matter for the question, e.g. calls that send, store or log a value.

## Step 3: answer with evidence

- Cite every statement of the chain with `[[node:<id>]]`, using IDs from the tool results.
- Keep the order of the chain: from the cause to the effect.
- Say where the evidence is incomplete: external calls, unresolved calls, truncated results.
