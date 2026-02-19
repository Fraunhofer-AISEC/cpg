# Unified TypeScript/Svelte Parser (Deno)

This directory contains the source code for the unified parser used by the `cpg-language-typescript` frontend, capable of handling both TypeScript/JavaScript and Svelte files.

## Implementation

The parser is implemented in TypeScript (`src/parser.ts`) and executed using **Deno**.
It leverages:

*   **Deno Runtime:** Provides the execution environment and APIs (like `Deno.args`, `Deno.readTextFile`).
*   **TypeScript Compiler API (`typescript` npm package):** Used internally by the script to parse standard TypeScript and JavaScript files (`.ts`, `.js`, `.tsx`, `.jsx`). This dependency is managed via `deno.json`.
*   **Svelte Compiler (`svelte/compiler` npm package):** Used internally by the script to parse Svelte component files (`.svelte`) using `svelte.parse()`. This dependency is also managed via `deno.json`.

The script accepts a `--language` flag (`typescript` or `svelte`) to determine which internal parser to use and outputs the resulting Abstract Syntax Tree (AST) as a JSON string to standard output.

## Build Process

The Gradle build process (`cpg-language-typescript/build.gradle.kts`) uses the `deno-gradle-plugin` to:

1.  **Compile the Parser Script:** Runs `deno compile` on `src/main/typescript/src/parser.ts` for various target platforms (Linux, macOS, Windows, x86_64, aarch64).
    *   Deno bundles the script and its dependencies (including the TS and Svelte compilers fetched via `deno.json`) into standalone executables.
2.  **Resource Copying:** Copies these compiled parser executables into the build resources (`build/resources/main/typescript/`).

## Frontend Usage

The `TypeScriptLanguageFrontend.kt` invokes the appropriate compiled Deno executable based on the host OS/architecture for `.ts` and `.js` files, passing the `--language=typescript` flag. It receives the TS AST JSON, deserializes it into `TypeScriptNode` objects, and processes it using its internal handlers.

The `SvelteLanguageFrontend.kt` (when invoked by the CPG TranslationManager for `.svelte` files) similarly invokes the same compiled Deno executable. It passes the `--language=svelte` flag, receives the Svelte AST JSON, deserializes it into `SvelteNode` objects (defined in `SvelteAST.kt`), and then directly constructs the CPG within its `parse()` method.
