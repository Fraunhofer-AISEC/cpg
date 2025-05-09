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

When developing the TypeScript or Svelte frontends within the main `cpg` project, you'll primarily use Gradle commands executed from the root directory of the `cpg` project (e.g., `/Users/andy/Widgetic/Widgetic-AI/code-base/cpg`).

### Common Gradle Commands

Make sure you are in the `cpg` project's root directory before running these commands.

*   **Compile the `cpg-language-typescript` module (including Svelte frontend):**
    ```bash
    ./gradlew :cpg-language-typescript:assemble
    ```
    This command will compile all Kotlin and Java code within the module and prepare its resources, including compiling the Deno parser.

*   **Run all tests in the `cpg-language-typescript` module:**
    ```bash
    ./gradlew :cpg-language-typescript:test
    ```

*   **Run a specific test class (e.g., `SvelteLanguageFrontendTest`):**
    ```bash
    ./gradlew :cpg-language-typescript:test --tests "de.fraunhofer.aisec.cpg.frontends.typescript.SvelteLanguageFrontendTest"
    ```

*   **Run a single specific test method (e.g., `test parsing a simple Svelte component` in `SvelteLanguageFrontendTest`):**
    ```bash
    ./gradlew :cpg-language-typescript:test --tests "de.fraunhofer.aisec.cpg.frontends.typescript.SvelteLanguageFrontendTest.test parsing a simple Svelte component"
    ```

*   **Clean the `cpg-language-typescript` module (remove build artifacts):**
    ```bash
    ./gradlew :cpg-language-typescript:clean
    ```

*   **Build the entire CPG project (including all modules):**
    ```bash
    ./gradlew assemble
    ```

*   **Run all tests in the entire CPG project:**
    ```bash
    ./gradlew test
    ```

### Important Considerations for Development

*   **CPG API Usage:** As highlighted by project maintainers, ensure that your Kotlin code correctly uses the current CPG API. "Unresolved reference" errors are often due to missing imports or incorrect usage of CPG classes and builder functions, rather than classpath issues. Always refer to existing, working frontends or the CPG core codebase for examples of correct API usage. The `TypeManager` and other core components are typically accessed via the `LanguageFrontend`'s context or helper/extension functions provided by the CPG framework.
*   **Deno Parser:** The Deno parser script (`src/main/typescript/src/parser.ts`) is a critical component. If you modify it, ensure it's correctly compiled into the frontend's resources by the Gradle build tasks (e.g., by running `./gradlew :cpg-language-typescript:processResources` or a full `assemble`).
*   **Testing:** Add comprehensive tests for any new functionality. The existing test structure (e.g., `SvelteLanguageFrontendTest.kt`) provides a good starting point.
