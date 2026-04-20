---
name: suggest-concepts
description: Explore the CPG of an analyzed codebase and suggest semantic concepts and operations that describe what nodes are and what they do.
---

# Suggest Concepts and Operations for a CPG

Your task is to explore a Code Property Graph (CPG) of an analyzed codebase and suggest semantic
concept and operation overlays for the nodes you find.

## About the CPG

The Code Property Graph is a language-agnostic representation of source code. It unifies the
abstract syntax tree (AST), control flow, and data flow of a program into a single graph. Nodes
represent syntactic and semantic elements (functions, calls, variables, records, etc.), and edges connect them.

On top of the raw graph we layer **overlays** — `Concept` and `Operation` nodes, which attach
higher-level meaning to existing CPG nodes.

## Concepts vs. Operations

**Concepts** describe what something *is*.

- Attached to nodes that represent or hold a thing (variables, fields, records, sometimes functions).
- Examples: `user_email → Data`, `api_token → Secret`, an `AuthService` class → `Authentication`.
- A concept can have properties and a set of operations associated with it.

**Operations** describe what something *does*.

- Attached to nodes that perform an action, most often **call sites** (function calls, method
  calls).
- Examples: `requests.post(...) → HttpRequest`, `file.write(...) → FileWrite`, `encrypt(...) →
  Encrypt`.
- Every operation belongs to a concept.

Rules:
- A single node can carry a concept; a concept can stand on its own. An operation always needs a
  concept.
- Keep names short and semantic: one word where possible (`Encryption`, `Logging`, `Secret`).

## Workflow

### 1. Load existing concepts and operations

Call `cpg_list_llm_concepts_operations` first. If concepts are already defined from previous runs,
**reuse their names and property schemas** where they semantically fit. Only invent a new concept
when nothing existing matches.

### 2. Explore the code comprehensively

Before suggesting follow a multistep approach by calling other tools to explore the graph, so one listing is rarely enough:

- Listing tools to get an overview, e.g. `cpg_list_functions`, `cpg_list_records`, `cpg_list_calls`, etc.
- Or call `cpg_get_node` to retrieve complete details of a node by ID when you need 

### 3. Define any missing concepts

For each concept you want to use that doesn't already exist in the list from step 1, call
`cpg_add_or_update_llm_concept` with:

- a clear, short name,
- a description of what it represents,
- relevant properties (if any),
- the operations you expect to associate with it.

Reuse matches existing concepts whenever possible don't create redundant ones.

### 4. Suggest concepts and operations 

Call `cpg_suggest_llm_concepts_and_operations` with **node IDs** that you obtained from the
exploration tools. For each suggestion:

- The concept's `nodeId` points to the node the concept semantically describes (e.g. the record or
  field that embodies the concept).
- Each operation's `nodeId` points to the node where the action is realized (typically a call
  site).
- Include reasoning so the user can judge the suggestion.

Never pass placeholder strings like `"TODO"` or `"unknown"`. If you don't yet have a ID, go back to step 2.

### 5. Wait for user approval

After you've made your suggestions, stop. Do **not** immediately apply them. The user reviews the
suggestions and decides what to accept.