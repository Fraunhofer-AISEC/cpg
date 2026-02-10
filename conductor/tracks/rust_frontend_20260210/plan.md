# Implementation Plan - Rust Frontend

## Phase 1: Module Setup & Infrastructure [checkpoint: ca8bfad]
- [x] Task: Create `cpg-language-rust` module structure (cacf7e1)
    - [ ] Create directory structure `cpg-language-rust/src/main/kotlin`, `cpg-language-rust/src/test/kotlin`, etc.
    - [ ] Create `build.gradle.kts` for the module with necessary dependencies (including tree-sitter bindings).
    - [ ] Register the new module in the root `settings.gradle.kts`.
    - [ ] Update root `build.gradle.kts` to handle the new module (e.g., in documentation or distribution tasks).
    - [ ] Add configuration flag to `gradle.properties` (e.g., `enableRustFrontend`) and `configure_frontends.sh`.
- [x] Task: Conductor - User Manual Verification 'Module Setup & Infrastructure' (Protocol in workflow.md)

## Phase 2: Tree-sitter Integration [checkpoint: 69772dd]
- [x] Task: Integrate `tree-sitter-rust` (e01f55e)
    - [ ] Research and select the appropriate Java/Kotlin binding for Tree-sitter (e.g., existing project bindings or a new wrapper).
    - [ ] Implement a basic test to verify that `tree-sitter-rust` can successfully parse a simple "Hello, World!" Rust file.
- [x] Task: Conductor - User Manual Verification 'Tree-sitter Integration' (Protocol in workflow.md)

## Phase 3: Basic AST Translation (Structure & Functions) [checkpoint: 41e0be3]
- [x] Task: Implement `RustLanguageFrontend` class (e01f55e)
    - [ ] Create the main class extending `LanguageFrontend`.
    - [ ] Implement the `parse()` method to trigger Tree-sitter parsing.
- [x] Task: Implement `RustHandler` and `DeclarationHandler` (62c192f)
    - [ ] Create `RustHandler` to dispatch Tree-sitter nodes to specific handlers.
    - [ ] Implement `DeclarationHandler` to handle `TranslationUnit` (file root).
    - [ ] Implement support for basic function declarations (`fn main() {}`).
    - [ ] Write tests: Verify that a file with an empty function produces the correct `TranslationUnitDeclaration` and `FunctionDeclaration` nodes.
- [x] Task: Conductor - User Manual Verification 'Basic AST Translation (Structure & Functions)' (Protocol in workflow.md)

## Phase 4: Statements & Control Flow [checkpoint: 239e02a]
- [x] Task: Implement `StatementHandler` (51d07a7)
    - [ ] Support block statements `{ ... }`.
    - [ ] Support variable declarations (`let x = ...`).
    - [ ] Support `return` statements.
    - [ ] Support `if/else` expressions (Rust treats these as expressions, but CPG might view them as statements/expressions depending on context).
    - [ ] Write tests: Verify CFG construction for basic blocks and conditionals.
- [x] Task: Conductor - User Manual Verification 'Statements & Control Flow' (Protocol in workflow.md)

## Phase 5: Expressions & Types [checkpoint: 1f2bb3b]
- [x] Task: Implement `ExpressionHandler` (6ba4cd6)
    - [x] Support literals (integers, strings, booleans).
    - [x] Support binary operations (`+`, `-`, `*`, etc.).
    - [x] Support function calls.
- [x] Task: Implement `TypeHandler` (6ba4cd6)
    - [x] Map basic Rust types (`i32`, `bool`, `String`, `&str`) to CPG types.
    - [x] Handle type inference where explicit types are missing (basic cases).
    - [x] Write tests: Verify DFG for variable assignments and usage.
- [x] Task: Conductor - User Manual Verification 'Expressions & Types' (Protocol in workflow.md)

## Phase 6: Rust Specifics (Structs, Enums, Traits) [checkpoint: 7bbe316]
- [x] Task: Support Compound Types (18ca831)
    - [x] Map `struct` definitions to `RecordDeclaration`.
    - [x] Map `enum` definitions.
    - [x] Support `impl` blocks (associating methods with structs).
- [x] Task: Support Matching (18ca831)
    - [x] Map `match` expressions to appropriate CPG control flow structures (likely `SwitchStatement` or equivalent).
- [x] Task: Conductor - User Manual Verification 'Rust Specifics (Structs, Enums, Traits)' (Protocol in workflow.md)

## Phase 7: Final Polish & Verification
- [ ] Task: Comprehensive Testing
    - [ ] Expand test suite to cover >90% of the new module.
    - [ ] Run integration tests with larger Rust samples.
- [ ] Task: Documentation
    - [ ] Add KDoc for all new classes.
    - [ ] Update project `README.md` or `tech-stack.md` to list Rust as an experimental frontend.
- [ ] Task: Conductor - User Manual Verification 'Final Polish & Verification' (Protocol in workflow.md)
