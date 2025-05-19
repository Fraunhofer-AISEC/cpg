# Svelte Language Support Integration Plan

This document tracks the progress of integrating Svelte language support into the `cpg-language-typescript` module.


## What we're building(purpose) and how parsing works

### Overall Goal: 
Our main objective is to enable the CPG (Code Property Graph) framework to understand and analyze Svelte files (.svelte), in addition to its existing capabilities for TypeScript, JavaScript, JSX, and TSX files.
This involves parsing these files and converting their structure and code into a graph representation (the CPG) that can then be queried and analyzed for patterns, vulnerabilities, etc.

### Parsing Process & Frontend Roles:
* We have a single Deno-based parser script: cpg-language-typescript/src/main/typescript/src/parser.ts. 
This script is the workhorse for the initial parsing step.
* For TypeScript/JavaScript/JSX/TSX files:
    * The TypeScriptLanguageFrontend.kt is responsible.
    * It calls the Deno parser.ts script with a --language=typescript flag.
    * Inside the Deno script, the TypeScript Compiler API (e.g., ts.createSourceFile) is used to parse the code and generate an Abstract Syntax Tree (AST).
    * This AST is sent back to TypeScriptLanguageFrontend.kt as a JSON string.
    * The TypeScriptLanguageFrontend.kt then converts this JSON AST into CPG nodes.
    * This part was largely pre-existing in the module.
* For Svelte files (.svelte):
    * The SvelteLanguageFrontend.kt (which we are working on) is responsible.
    * It calls the same Deno parser.ts script but with a --language=svelte flag.
    * Inside the Deno script, the svelte.parse() function (from the official svelte/compiler library) is used to parse the Svelte file. This function generates a Svelte-specific AST. For the JavaScript/TypeScript code within <script> tags, this AST is ESTree-compliant.
    * This Svelte AST is sent back to SvelteLanguageFrontend.kt as a JSON string.
    * The SvelteLanguageFrontend.kt then uses helper Kotlin data classes (defined in SvelteAST.kt) to understand this JSON and converts it into CPG nodes.

### Role and Status of SvelteAST.kt:
* SvelteAST.kt defines a set of Kotlin data classes (like SvelteProgram, SvelteScript, SvelteHtmlElement, and also the EsTreeNode hierarchy for script content). These classes are structured to match the JSON output of svelte.parse().
* Its main purpose is to allow the Jackson JSON library (used in SvelteLanguageFrontend.kt) to take the raw JSON string from the Deno parser and turn it into a tree of usable Kotlin objects.
* Is it "finished"? It's developed enough to handle the Svelte AST structures we've encountered so far, especially for deserializing script content and basic HTML/CSS structures, which fixed earlier Jackson deserialization errors. However, if svelte.parse() outputs new or different AST node types in the future (e.g., with new Svelte features), we might need to add or modify classes in SvelteAST.kt to match. So, it's functionally adequate for our current test cases but could need expansion.

### AST Creation for regular JS/TS files:
As mentioned above, for regular .js, .ts, .jsx, and .tsx files, the AST is created by the TypeScript Compiler API within the Deno parser.ts script. This was an existing capability of the cpg-language-typescript module before we started adding Svelte support.


### Using Tree-sitter for Svelte files
You're right, the web search results show that Tree-sitter grammars for Svelte do exist (like Himujjal/tree-sitter-svelte at https://github.com/Himujjal/tree-sitter-svelte and tree-sitter-grammars/tree-sitter-svelte at https://github.com/tree-sitter-grammars/tree-sitter-svelte).
* Current Approach (using svelte.parse() via Deno):
    * Pros: We're using the official Svelte compiler's parser, which guarantees the most accurate understanding of Svelte syntax and its evolution. The AST for script blocks is already ESTree-compliant, which is a standard format the CPG can work with more easily for JavaScript parts.
    * Cons: It involves an external Deno process call, which adds some overhead. The full Svelte AST structure is specific to the Svelte compiler and requires the mapping we've done in SvelteAST.kt.
* Potential Tree-sitter Approach:
    * Pros: Tree-sitter is generally very fast and can offer good error recovery. If we used Java/Kotlin bindings for Tree-sitter directly, we might avoid the Deno process call. It offers a more universal way to define language grammars.
    * Cons: We would be relying on a third-party grammar. Its accuracy, completeness, and maintenance compared to the official Svelte parser would need careful evaluation. The AST structure produced by Tree-sitter would likely be different from what svelte.parse() produces, meaning SvelteAST.kt would need a significant rewrite, and the logic in SvelteLanguageFrontend.kt to transform that AST into CPG nodes would also need to be redone. Integrating Tree-sitter (especially if it means managing native libraries from Kotlin/Java) could add complexity to the build and deployment.
* Recommendation:
For now, continuing with the current approach (using svelte.parse() via the Deno script) seems more practical. We've made significant progress, and the primary known issues seem to be within our Kotlin frontend's handling of the AST rather than the parsing itself. The CPG project already uses this Deno-based parsing mechanism for TypeScript, so there's an established pattern.
Switching to Tree-sitter would be a substantial architectural change and a new research task. It might be something to consider in the future if the current approach hits insurmountable limitations (e.g., performance bottlenecks that can't be optimized), but it's probably best to get the current method fully working and stable first.


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

### 1. Add svelte support in current typescript modules
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

### 2. Refactor: Unified AST Handling with `GenericAstNode`

**What changed:**  
We introduced a new interface, `GenericAstNode`, as a base for all Svelte and ESTree AST node data classes in `SvelteAST.kt`. This interface provides common properties (`start`, `end`) and allows the Kotlin frontend to process all AST nodes in a unified way.

**Why:**  
Previously, Svelte-specific and ESTree nodes were handled separately, leading to code duplication and making it harder to extend support for new node types. By introducing `GenericAstNode`, we can write generic processing logic in `SvelteLanguageFrontend.kt` that works for both Svelte and ESTree nodes.

**How it works:**  
- All relevant AST node data classes now implement `GenericAstNode`.
- The frontend logic (e.g., code extraction, location mapping) can operate on `GenericAstNode` without needing to know the specific node type.
- This makes the codebase more maintainable and easier to extend as Svelte or ESTree evolve.

**Impact:**  
- Reduces code duplication in the frontend.
- Simplifies future support for new Svelte/ESTree AST node types.
- Prepares the codebase for more advanced Svelte features and better error handling.

