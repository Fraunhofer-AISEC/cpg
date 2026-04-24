---
name: suggest-concepts
description: Explore the CPG of an analyzed codebase and suggest semantic concepts and operations to tag nodes with what they are and what they do.
---

# Suggest Concepts and Operations for a CPG

Your task is to explore a Code Property Graph (CPG) of an analyzed codebase and suggest semantic
concept and operation overlays to tag the nodes you find. You must answer with the tool
`cpg_suggest_llm_concepts_and_operations` and not with lists, tables or text. 

## About the CPG

The Code Property Graph is a language-agnostic representation of source code. It unifies the
abstract syntax tree (AST), control flow, and data flow of a program into a single graph. Nodes
represent syntactic and semantic elements (functions, calls, variables, records, etc.), and edges
connect them.

On top of the raw graph we layer **overlays** `Concept` and `Operation` nodes, which attach
higher-level meaning to existing CPG nodes.

## Concepts vs. Operations

**Concepts** describe what something *is*.

- Attached to nodes that represent or hold a thing (variables, fields, records, sometimes functions).
- Examples: `user_email → Data`, `api_token → Secret`, an `AuthService` class → `Authentication`.
- A concept can have properties and a set of operations associated with it.

**Operations** describe what something *does*.

- Attached to nodes that perform an action, most often **calls** (function or method
  calls).
- Examples: `requests.post(...) → HttpRequest`, `file.write(...) → FileWrite`, `encrypt(...) →
  Encryption`.
- Every operation belongs to a concept.

Rules:
- A concept can stand on its own. An operation always needs a concept.
- Keep names short and semantic: one word where possible (`Encryption`, `Logging`, `Secret`).

## Workflow

### 1. Load existing concepts and operations

Call `cpg_list_llm_concepts_operations` once, before anything else.
- If the result is non-empty, reuse those concept and operation names and their property schemas
  wherever they semantically fit. Do not invent a duplicate under a different name.
- If the result is empty suggest new ones.

### 2. Explore the code comprehensively

Before suggesting follow a multistep approach by calling other tools to explore the graph, so one listing is rarely enough:
Combine several of:
- `cpg_list_functions`, `cpg_list_records`, `cpg_list_calls`, `cpg_list_calls_to`, etc. for
  overview.
- `cpg_get_node` to inspect a specific node in detail (if needed).

Keep exploring until you have real node IDs for every concept and operation you intend to suggest.

### 3. Suggest via the tool

For each concept, call `cpg_suggest_llm_concepts_and_operations` with:

- The concept's `nodeId` pointing to the node the concept semantically describes (typically a
  record, field, or variable).
- Each operation's `nodeId` pointing to the node where the action is realized.
- Include reasoning so the user can judge the suggestion.

All node IDs must come from prior tool results. Never pass placeholder strings like `"TODO"`,
`"unknown"`, or invented IDs. If you don't have a real ID, go back to step 2.

### 4. Stop

Once you have called the tool for every concept you want to suggest, your turn is done. The user
reviews the suggestions and decides what to accept. Do not apply anything yourself.