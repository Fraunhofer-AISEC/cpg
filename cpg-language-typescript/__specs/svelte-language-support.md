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

**Further Refinement & Progress (Post Initial `GenericAstNode` Introduction):**
- **Problem Encountered:** Initial tests with `SvelteLanguageFrontendTest` failed to identify exported variables (e.g., `export let name...`). Debugging revealed a `ClassCastException` when the CPG core attempted to get code/location information for `EsTreeVariableDeclarator` nodes (which implement `GenericAstNode`). The core was trying to cast these to `SvelteProgram` (the frontend's original `AstNode` type).
- **Solution:** The `SvelteLanguageFrontend` was refactored to use `GenericAstNode` as its primary `AstNode` type parameter (i.e., `LanguageFrontend<GenericAstNode, GenericAstNode>`).
    - The `codeOf(astNode: AstNode)`, `locationOf(astNode: AstNode)`, and `setComment(node: Node, astNode: AstNode)` methods were updated to directly use `GenericAstNode` in their signatures.
    - Specific logic for `SvelteProgram` instances (e.g., for getting the code of the entire file) was integrated within these overridden `GenericAstNode`-based methods.
- **Outcome:** This refactoring resolved the `ClassCastException` and allowed the CPG node builders to correctly interact with the frontend for all `GenericAstNode` subtypes. The `SvelteLanguageFrontendTest` (testing `SimpleComponent.svelte`) now passes, confirming that top-level variable declarations (including exported ones like `name`) and function declarations (like `handleClick` and `count`) in `<script>` blocks are correctly parsed and represented as CPG nodes.


Let's summarize where we stand with Svelte support:
What Was Working (and should still work if we revert recent changes):
1. Svelte File Parsing: The SvelteLanguageFrontend successfully invokes the Deno-based parser (parser.ts with --language=svelte).
2. AST Deserialization: The JSON AST output by the Deno parser (which uses svelte.parse()) is correctly deserialized into Kotlin objects using the SvelteAST.kt data classes. This includes the ESTree-compliant AST for the content within <script> tags.
3. CPG Node Creation for Script Signatures & Top-Level Variables:
* Top-level variable declarations in the <script> block (e.g., export let name: string = "World";, let count: number = 0;) are parsed, and corresponding VariableDeclaration CPG nodes are created.
* Function declarations in the <script> block (e.g., function handleClick()) are parsed, and FunctionDeclaration CPG nodes representing their signatures are created.
* This level of parsing was sufficient for SvelteLanguageFrontendTest to pass, as it checks for the existence of these CPG nodes.
Current State & What We Were Working On (Leading to Build Errors):
* Function Body Details (TODO 3): The current "Unresolved reference" errors (newExpressionStatement, ProblemNode.ProblemType.PARSER, addStatement) emerged when we started to implement the detailed parsing of function bodies. This involves:
* Creating ParameterDeclaration CPG nodes for function parameters.
* Creating a CompoundStatement (a block) for the function's body.
* Recursively calling handleScriptStatement to process statements inside the function body and adding them to this CompoundStatement.
* It's this new, more complex logic within handleScriptStatement (specifically for EsTreeFunctionDeclaration and EsTreeExportNamedDeclaration containing a function) that is encountering issues with resolving certain CPG builder functions.
What Has NOT Been Implemented Yet:
* HTML Structure Parsing (TODO 1): We have not yet started implementing the logic to parse the Svelte HTML-like template structure (e.g., <h1>Hello {name}!</h1>, <button on:click={handleClick}>). This involves creating CPG nodes for HTML elements, attributes, Svelte-specific directives (on:click, bind:value, {#if}, {#each}), and linking them to the script logic. This is represented by program.html in the Svelte AST.
* CSS Parsing (TODO 2): Similarly, parsing the content of <style> blocks (represented by program.css) has not been addressed.
* Other Script-Related TODOs: Several other TODOs related to richer script analysis (export details, type resolution beyond unknownType()) were planned after getting the basic function bodies working.
In essence: The frontend can parse .svelte files and understand the top-level structure of the <script> tag, including variable and function declarations. The current errors are preventing us from correctly parsing the implementation details inside those functions. The HTML and CSS aspects are still pending.

### 3. Basic Svelte Parsing Implementation Complete ✅

**Status:** Successfully implemented basic Svelte file parsing with working CPG node generation for script content.

**What Works:**
- **Svelte File Processing:** The `SvelteLanguageFrontend` successfully parses `.svelte` files using the Deno-based parser with `svelte.parse()`.
- **AST Deserialization:** JSON AST output is correctly deserialized into Kotlin objects using `SvelteAST.kt` data classes.
- **Script Block Parsing:** Successfully extracts and processes JavaScript/TypeScript code from `<script>` tags.
- **CPG Node Creation:** Creates proper CPG nodes for:
  - Top-level variable declarations (including exported variables like `export let name: string = "World"`)
  - Function declarations (signatures)
  - Basic variable types and initializers

**Test Results:**
- `SvelteLanguageFrontendTest` passes successfully
- Correctly identifies variables: `name`, `count` 
- Correctly identifies function: `handleClick`
- Parser execution: ~2 seconds for simple component
- Log output shows proper detection: "Declarations in TU after Svelte parse: name (VariableDeclaration), count (VariableDeclaration), handleClick (FunctionDeclaration)"

**Current Implementation Scope:**
- Parses `SimpleComponent.svelte` test file containing:
  - TypeScript script block with exported variables
  - HTML template with Svelte expressions (`{name}`, `{count}`)
  - CSS style block
  - Event handlers (`on:click={handleClick}`)

**Next Steps Required:**
1. **Function Body Implementation:** Complete parsing of function implementation details (statements, expressions within function bodies)
2. **HTML Template Parsing:** Implement CPG nodes for HTML elements, Svelte directives, and template expressions
3. **CSS Block Parsing:** Add support for style block content
4. **Export/Import Analysis:** Enhanced handling of Svelte component exports and imports
5. **JSON Output Testing:** Add tests for CPG-to-JSON conversion for visualization tools

**Technical Notes:**
- Uses `GenericAstNode` interface for unified AST handling
- Resolved `ClassCastException` issues with frontend type parameters
- All compilation passes without errors
- Ready for next phase of implementation

### 4. Current Status & Next Immediate Actions ⚡

**Current State (May 2025):**
✅ **COMPLETED:**
- Basic Svelte parsing infrastructure is fully working
- Script block parsing with proper CPG node generation
- Variable and function declaration extraction
- Test passes (`SvelteLanguageFrontendTest`)
- Enhanced test with JSON output capability for cpg-wrapper-service

🔄 **IN PROGRESS:**
- JSON output enhancement for visualization tools
- Property access fixes in test (resolved compilation errors)

📋 **IMMEDIATE NEXT TODOS:**
[✅] 1. **Run Enhanced Test** - Execute the updated test to verify JSON output for cpg-wrapper-service ✅ **COMPLETED**
   - JSON file successfully generated: `build/test-results/svelte/SimpleComponent-cpg.json`
   - Contains proper CPG structure for variables, functions, types, and locations
   - Ready for cpg-wrapper-service integration
[✅] 2. **Function Body Implementation** - Complete parsing of function internals (statements, expressions) ✅ **COMPLETED**
   - Assignment expressions working: `count += 1;` correctly parsed as `AssignExpression` with operator `"+="`
   - Variable references working: `count` identified as `Reference` type
   - Literals working: `1` identified as `Literal` type
   - Function body compound statements working: 1 statement correctly detected in `handleClick()`
[✅] 3. **HTML Template Parsing** - Begin implementing CPG nodes for Svelte template syntax ✅ **COMPLETED**
   - Successfully parses HTML elements: `<h1>`, `<p>`, `<button>` → `RecordDeclaration` nodes
   - Text nodes working: "Hello", "You've clicked the button" → `Literal` nodes
   - Svelte expressions working: `{name}`, `{count}` → `Reference` nodes to script variables
   - Event handlers working: `on:click={handleClick}` → `FieldDeclaration` with handler linkage
   - Template structure: 7 children processed including mixed content (text, elements, expressions)
   - **Debug logs confirm**: "Processing HTML element: h1/p/button", "Processing Svelte expression: EsTreeIdentifier" → "Reference"
[ ] 4. **Integration Testing** - Test with cpg-wrapper-service visualizer to ensure graph compatibility

**Technical Debt/TODOs:**
- CSS block parsing (not started) - **NEXT PRIORITY**
- Advanced Svelte directives (`{#if}`, `{#each}`, `bind:value`, etc.) → CPG representation  
- Enhanced type resolution beyond `unknownType()`
- Cross-references between HTML event handlers and script functions
- Complete ConditionalExpression handling (currently creates ProblemExpression)
- Parent-child relationships in HTML element hierarchy




