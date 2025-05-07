# TypeScript and Svelte Language Frontends

This module (`cpg-language-typescript`) provides CPG (Code Property Graph) language frontends for both TypeScript (including JavaScript, JSX, TSX) and Svelte.

## Unified Deno-Based Parser

A single, unified Deno-based parser script is responsible for parsing both TypeScript/JavaScript and Svelte source files. This script is located at `cpg-language-typescript/src/main/typescript/src/parser.ts` within this submodule.

The parser leverages:
- The official TypeScript Compiler API for parsing TypeScript, JavaScript, JSX, and TSX.
- The `svelte.parse()` API (from the `svelte/compiler`) for parsing Svelte components.

The behavior of the Deno parser (i.e., which language to parse) is controlled by a `--language` command-line flag passed to it by the respective CPG frontend. The parser outputs the Abstract Syntax Tree (AST) as a JSON string.

## Frontend Implementations

### 1. `TypeScriptLanguageFrontend.kt`
-   **Handles**: `.ts`, `.js`, `.tsx`, `.jsx` files.
-   **Mechanism**: Invokes the unified Deno parser script (`parser.ts`) with the `--language=typescript` flag. It then processes the JSON AST output (which conforms to the TypeScript Compiler API's AST structure) to construct the CPG.

### 2. `SvelteLanguageFrontend.kt`
-   **Handles**: `.svelte` files.
-   **Mechanism**: Invokes the *same* unified Deno parser script (`parser.ts`) but with the `--language=svelte` flag. It receives a JSON AST (based on the structure output by `svelte.parse()`, which includes ESTree-compliant AST for `<script>` blocks). The `SvelteLanguageFrontend` then processes this Svelte-specific AST to construct the CPG, including handling the script content, HTML-like template, and style blocks.

This approach allows for a shared parsing infrastructure (the Deno script) while maintaining separate, specialized CPG construction logic within each Kotlin-based language frontend also located in this submodule. 


## How to develop on localhost inside the cpg project
