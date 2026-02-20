---
title: "Implementation and Concepts - Symbol Resolution"
linkTitle: "Implementation and Concepts - Symbol Resolution"
weight: 20
no_list: false
menu:
  main:
    weight: 20
description: >
    The CPG library is a language-agnostic graph representation of source code.
---


# Symbol Resolution

This pages describes the main functionality behind symbol resolution in the CPG library. This is mostly done by the `SymbolResolver` pass, in combination with the symbol lookup API (see [Scopes and Symbols](scopes.md#looking-up-symbols)). In addition to the *lookup* of a symbol, the *resolution* takes the input of the lookup and provides a "definite" decision which symbol is used. This mostly referred to symbols / names used in a `Reference` or a `Call` (which also has a reference as its `Call::callee`).

## The `SymbolResolver` Pass

The `SymbolResolver` pass takes care of the heavy lifting of symbol (or rather reference) resolving:
 
* It sets the `Reference::refersTo` property,
* and sets the `Call::invokes` property,
* and finally takes cares of operator overloading (if the language supports it).

In a way, it can be compared to a linker step in a compiler. The pass operates on a single `Component` and starts by identifying EOG starter nodes within the component. These node "start" an EOG sub-graph, i.e., they do not have any previous EOG edges. The symbol resolver uses the `ScopedWalker` with a special set-up that traverses the EOG starting with each EOG starter node until it reaches the end. This ensures that symbols are resolved in the correct order of "evaluation", e.g., that a base of a member expression is resolved before the expression itself. This ensures that necessary type information on the base are available in order to resolve appropriate fields of the member expression.

The symbol resolver itself has gone through many re-writes over the years and there is still some code left that we consider *legacy*. These functions are marked as such, and we aim to remove them slowly.

## Resolving References

The main functionality lies in `ScopeManager::handleReference`. For all `Reference` nodes (that are not `Member` nodes) we use the symbol lookup API to find declaration candidates for the name the reference is referring to. This candidate list is then stored in `Reference::candidates`. If the reference is the `Call::callee` property of a call, we abort here and jump to [Resolve Calls](#resolve-calls).

Otherwise, we currently take the first entry of the candidate list and set the `Reference::refersTo` property to it.

## Resolve Calls

Prerequisite: The `Call::callee` reference must have been resolved (see [Resolving References](#resolving-references)).