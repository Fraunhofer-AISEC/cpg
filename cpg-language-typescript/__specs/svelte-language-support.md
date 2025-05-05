# Svelte Language Support Integration Plan

This document tracks the progress of integrating Svelte language support into the `cpg-language-typescript` module.

## Plan (Systematic Approach)

1.  **Revert & Branch:** Revert `cpg-language-typescript` to baseline (main branch state). Create new branch `feature/svelte-support`. (DONE)
2.  **Verify Baseline:** Confirm the original module builds correctly (ignoring test setup issues like Kover). (DONE - `./gradlew :cpg-language-typescript:clean :cpg-language-typescript:assemble` succeeded).
3.  **Add Svelte Kotlin Stubs:** Create minimal `SvelteLanguage.kt`, `SvelteAST.kt`, `SvelteLanguageFrontend.kt`. (DONE)
4.  **Build Step 1:** Run `compileKotlin` to ensure stubs are syntactically correct. Commit. (Current Step)
    *   Purpose: Quickly check basic Kotlin syntax, class structure, inheritance, and imports for the new stub files before adding complex logic. Confirms the stubs themselves don't break compilation.
5.  **Add Svelte Parser Script & Build Logic:** Decide between Deno or Node.js for the `svelte.parse` script. Add parser script and necessary build tasks (`build.gradle.kts`). Run `assemble`. Commit.
6.  **Integrate Parser Execution:** Add logic to `SvelteLanguageFrontend.kt` to run the parser and read JSON output.
7.  **Build Step 2:** Run `compileKotlin`. Fix process/IO/JSON errors. Commit.
8.  **Integrate Basic CPG Nodes:** Add code to create `TranslationUnitDeclaration` and placeholder `RecordDeclaration` from AST.
9.  **Build Step 3:** Run `compileKotlin`. Analyze and fix core CPG integration errors carefully. Commit.
10. **Add Dispatch Logic:** Re-introduce Svelte dispatch logic in `TypeScriptLanguageFrontend.kt`.
11. **Build Step 4:** Run `compileKotlin`. Fix. Commit.
12. **Add Tests & Refine:** Implement tests and detailed CPG node handling.

## Progress Notes

*   **Strategy Shift:** Decided to integrate Svelte support directly into `cpg-language-typescript` instead of a separate module, based on maintainer feedback.
*   **Kotlin Stubs:** Created initial Kotlin classes within `cpg-language-typescript`:
    *   `SvelteLanguage.kt`: Defines the language properties.
    *   `SvelteAST.kt`: Placeholder interface for AST nodes.
    *   `SvelteLanguageFrontend.kt`: Stub implementation for the frontend, including basic `parse` method structure.
*   **Parser Setup:**
    *   Modified the existing Deno-based parser script (`src/main/typescript/parser.ts`) to include `svelte.parse()` for handling `.svelte` files.
    *   The build process in `cpg-language-typescript/build.gradle.kts` will need to be adjusted to handle Deno execution for the combined parser (specific tasks to be defined).
*   **Frontend Integration:** Modified `TypeScriptLanguageFrontend.kt`'s `parse` method to detect `.svelte` files and delegate to `SvelteLanguageFrontend` (when instantiated).
*   **Build Status (Current):** Successfully added Kotlin stubs. Next step is to resolve Kotlin compilation errors in the new Svelte files and attempt a build (`./gradlew :cpg-language-typescript:build`).
