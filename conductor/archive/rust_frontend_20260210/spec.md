# Specification: Rust Language Frontend

## 1. Overview
This feature introduces `cpg-language-rust`, a new experimental language frontend for the Code Property Graph (CPG) library. It will enable the analysis of Rust source code by translating it into the CPG's unified graph representation. The frontend will utilize `tree-sitter-rust` for parsing and AST generation, taking design inspiration from the existing C++, Python, and Go frontends to ensure architectural consistency.

## 2. Goals
- **Enable Rust Analysis:** Provide the capability to parse Rust code and generate a CPG.
- **Tree-sitter Integration:** Successfully integrate `tree-sitter-rust` (via Java bindings or a Kotlin wrapper) to generate the initial AST.
- **Idiomatic Translation:** Accurately map Rust-specific constructs (e.g., ownership, borrowing, lifetimes, match expressions, traits) to CPG nodes, preserving as much semantic information as possible.
- **Extensibility:** Design the frontend to be easily extensible for future Rust language updates.

## 3. Requirements

### 3.1. Parsing & AST
- Use `tree-sitter-rust` as the underlying parser.
- Ensure robust handling of standard Rust syntax as defined in the [Rust Reference](https://doc.rust-lang.org/reference/).

### 3.2. CPG Mapping
- **Structural Nodes:** Map files, namespaces (crates/modules), and functions.
- **Type System:** Map basic Rust types, structs, enums, and traits to CPG type nodes.
- **Control Flow:** Translate control flow constructs (if/else, loops, match) to CPG Control Flow Graph (CFG) edges.
- **Data Flow:** Model variable declarations, assignments, and usage to support Data Flow Graph (DFG) construction.
- **Rust Specifics:** Capture ownership and lifetime information where possible within the existing CPG schema, or propose extensions if necessary.

### 3.3. Integration
- Create a new Gradle module `cpg-language-rust`.
- Integrate the module into the main build lifecycle (`settings.gradle.kts`, `build.gradle.kts`).
- Ensure the frontend can be enabled/disabled via `gradle.properties` (similar to other experimental frontends).

### 3.4. Testing
- Achieve >90% code coverage for the new module.
- comprehensive unit tests for individual constructs.
- Integration tests using sample Rust code snippets.

## 4. Architecture
- **Module:** `cpg-language-rust`
- **Frontend Class:** `RustLanguageFrontend` (extends `LanguageFrontend`)
- **Handler Classes:**
    - `RustHandler` (main entry point for AST traversal)
    - `DeclarationHandler` (handles structs, enums, fn declarations)
    - `StatementHandler` (handles statements within blocks)
    - `ExpressionHandler` (handles expressions)
    - `TypeHandler` (handles type resolution)

## 5. Constraints
- Must align with the existing CPG node hierarchy.
- Performance should be comparable to other Tree-sitter-based frontends (e.g., Python).
