---
name: tag-library
description: Analyze a library via CPG, systematically tag specific functions with LLM concepts and operations describing what they do.
# Flag tested in claude code. Not sure if this works with other clients/cli's. It might a different another name. 
user-invocable: true
arguments:
  - name: path
    description: Path to the library source code to analyze
    required: true
# Claude Code specific: run in a subagent context to not overload the main context window
# https://code.claude.com/docs/en/skills
# For other clients, remove `context: fork`
context: fork
---

# Tag Library with CPG Concepts and Operations

You are tasked with analyzing specific functions in a library's source code using the CPG (Code Property Graph) MCP 
tools and tagging them with semantic concepts and operations.

## Inputs

- **Library path**: `$ARGUMENTS.path`
- **Target functions**: Listed in `functions.yaml` (same directory as this skill file)

## Workflow

### Step 1: Read the target function list

Read the file `functions.yaml` located next to this SKILL.md. It contains a list of functions, each with a `name` and `file`.

### Step 2: Analyze the library

Call `cpg_analyze` with the library path to build the CPG.

### Step 3: Load existing concepts

Call `cpg_list_llm_concepts_operations` to check if there are already defined concepts. If yes, reuse 
them where applicable.

### Step 4: Retrieve target functions from CPG

Collect all function names from `functions.yaml` and call `cpg_get_functions_by_name` with the list of names.
Process in batches of ~10 names per call to avoid overly large responses.
Note any functions which couldn't be found in the summary.

### Step 5: Tag matched functions

For each returned function:

1. Review the function's code, parameters, and signature from the `cpg_get_functions_by_name` response
2. Determine what the function does semantically. Think in concepts and operations:
   - **Concepts** = what something IS (e.g., "Encryption", "Logging")
   - **Operations** = what something DOES (e.g., "encrypt", "write")
3. If the concept doesn't exist yet, define it via `cpg_add_or_update_llm_concept` with:
   - A clear name (short, preferably one word)
   - A description explaining what it represents
   - Relevant properties
   - Associated operations
4. Apply the concept and operations to the function node via `cpg_add_llm_concept_and_operations`, using the `nodeId` from the function info

### Step 6: Summarize

After processing all functions, provide a summary table:

| Function | File | Concept | Operations | Status |
|----------|------|---------|------------|--------|

Where Status is one of: tagged, skipped (ambiguous), not found.

## Guidelines

- If a function's purpose is unclear, mark it as "skipped" rather than guessing
