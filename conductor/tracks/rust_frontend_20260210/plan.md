# Implementation Plan - Rust Frontend

## Phase 1: Module Setup & Infrastructure
- [x] Task: Create `cpg-language-rust` module structure (cacf7e1)
    - [ ] Create directory structure `cpg-language-rust/src/main/kotlin`, `cpg-language-rust/src/test/kotlin`, etc.
    - [ ] Create `build.gradle.kts` for the module with necessary dependencies (including tree-sitter bindings).
    - [ ] Register the new module in the root `settings.gradle.kts`.
    - [ ] Update root `build.gradle.kts` to handle the new module (e.g., in documentation or distribution tasks).
    - [ ] Add configuration flag to `gradle.properties` (e.g., `enableRustFrontend`) and `configure_frontends.sh`.
- [x] Task: Conductor - User Manual Verification 'Module Setup & Infrastructure' (Protocol in workflow.md)

## Phase 2: Tree-sitter Integration
- [ ] Task: Integrate `tree-sitter-rust`
    - [ ] Research and select the appropriate Java/Kotlin binding for Tree-sitter (e.g., existing project bindings or a new wrapper).
    - [ ] Implement a basic test to verify that `tree-sitter-rust` can successfully parse a simple "Hello, World!" Rust file.
- [ ] Task: Conductor - User Manual Verification 'Tree-sitter Integration' (Protocol in workflow.md)

## Phase 3: Basic AST Translation (Structure & Functions)
- [ ] Task: Implement `RustLanguageFrontend` class
    - [ ] Create the main class extending `LanguageFrontend`.
    - [ ] Implement the `parse()` method to trigger Tree-sitter parsing.
- [ ] Task: Implement `RustHandler` and `DeclarationHandler`
    - [ ] Create `RustHandler` to dispatch Tree-sitter nodes to specific handlers.
    - [ ] Implement `DeclarationHandler` to handle `TranslationUnit` (file root).
    - [ ] Implement support for basic function declarations (`fn main() {}`).
    - [ ] Write tests: Verify that a file with an empty function produces the correct `TranslationUnitDeclaration` and `FunctionDeclaration` nodes.
- [ ] Task: Conductor - User Manual Verification 'Basic AST Translation (Structure & Functions)' (Protocol in workflow.md)

## Phase 4: Statements & Control Flow
- [ ] Task: Implement `StatementHandler`
    - [ ] Support block statements `{ ... }`.
    - [ ] Support variable declarations (`let x = ...`).
    - [ ] Support `return` statements.
    - [ ] Support `if/else` expressions (Rust treats these as expressions, but CPG might view them as statements/expressions depending on context).
    - [ ] Write tests: Verify CFG construction for basic blocks and conditionals.
- [ ] Task: Conductor - User Manual Verification 'Statements & Control Flow' (Protocol in workflow.md)

## Phase 5: Expressions & Types
- [ ] Task: Implement `ExpressionHandler`
    - [ ] Support literals (integers, strings, booleans).
    - [ ] Support binary operations (`+`, `-`, `*`, etc.).
    - [ ] Support function calls.
- [ ] Task: Implement `TypeHandler`
    - [ ] Map basic Rust types (`i32`, `bool`, `String`, `&str`) to CPG types.
    - [ ] Handle type inference where explicit types are missing (basic cases).
    - [ ] Write tests: Verify DFG for variable assignments and usage.
- [ ] Task: Conductor - User Manual Verification 'Expressions & Types' (Protocol in workflow.md)

## Phase 6: Rust Specifics (Structs, Enums, Traits)
- [ ] Task: Support Compound Types
    - [ ] Map `struct` definitions to `RecordDeclaration`.
    - [ ] Map `enum` definitions.
    - [ ] Support `impl` blocks (associating methods with structs).
- [ ] Task: Support Matching
    - [ ] Map `match` expressions to appropriate CPG control flow structures (likely `SwitchStatement` or equivalent).
- [ ] Task: Conductor - User Manual Verification 'Rust Specifics (Structs, Enums, Traits)' (Protocol in workflow.md)

## Phase 7: Final Polish & Verification
- [ ] Task: Comprehensive Testing
    - [ ] Expand test suite to cover >90% of the new module.
    - [ ] Run integration tests with larger Rust samples.
- [ ] Task: Documentation
    - [ ] Add KDoc for all new classes.
    - [ ] Update project `README.md` or `tech-stack.md` to list Rust as an experimental frontend.
- [ ] Task: Conductor - User Manual Verification 'Final Polish & Verification' (Protocol in workflow.md)
