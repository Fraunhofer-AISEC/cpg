# Implementation Plan - Comprehensive Rust Frontend

## Phase 1: Advanced Control Flow & Pattern Matching
- [x] Task: Support `if let` and `while let` constructs 928d3f1
    - [ ] Update `StatementHandler` to handle `if let` expressions as a combination of a conditional check and a declaration.
    - [ ] Update `StatementHandler` to handle `while let` loops.
    - [ ] Test with `if let Some(x) = opt` and `while let Ok(x) = iter.next()`.
- [x] Task: Support Match Guards 468f881
    - [ ] Extend `ExpressionHandler` or `StatementHandler` to parse match arms with `if` guards.
    - [ ] Map the guard condition to the CFG correctly (must be true for the arm to execute).
    - [ ] Test with pattern matching including guards.
- [x] Task: Support Loop Labels 228bbae
    - [ ] Update `StatementHandler` to capture labels on loops (`'label: loop { ... }`).
    - [ ] Update `break` and `continue` handling to resolve labelled targets.
    - [ ] Test nested loops with labelled breaks.
- [x] Task: Support Async/Await dbb0af9
    - [ ] Handle `async fn` declarations (mark function as async).
    - [ ] Handle `.await` expressions (model as a specific unary operator or call).
    - [ ] Test basic async function definition and execution.
- [ ] Task: Conductor - User Manual Verification 'Advanced Control Flow & Pattern Matching' (Protocol in workflow.md)

## Phase 2: Traits & Generics (Generic Graph)
- [ ] Task: Enhanced Trait Support
    - [ ] Update `DeclarationHandler` to parse `trait` definitions fully, including default methods.
    - [ ] Handle `impl Trait for Type` blocks, linking implementation methods to the trait definition.
    - [ ] Test basic trait definition and implementation.
- [ ] Task: Trait Bounds & Where Clauses
    - [ ] Update `TypeHandler` and `DeclarationHandler` to parse `where` clauses and inline trait bounds (`T: Display`).
    - [ ] Store these bounds in the `TypeParameterDeclaration`.
    - [ ] Test generic functions with simple and complex trait bounds.
- [ ] Task: Associated Types
    - [ ] Support `type Item = ...` inside `impl` blocks.
    - [ ] Model these as `TypedefDeclaration` or similar nested within the record/namespace.
    - [ ] Test iterator implementation with associated types.
- [ ] Task: Conductor - User Manual Verification 'Traits & Generics (Generic Graph)' (Protocol in workflow.md)

## Phase 3: Macros & Metaprogramming
- [ ] Task: Macro Expansion Architecture
    - [ ] Research and design a strategy for macro expansion (e.g., using `rustc --pretty expanded` or an internal simple expander for basic cases).
    - [ ] Decide: For this track, we will likely implement a *basic internal expander* or rely on parsing the *source* as provided, potentially adding a pre-processing hook. *Note: Full `macro_rules` engine is huge; we might need to scope this to "Structure Preservation" for now.*
    - *Decision:* We will model macro calls as `CallExpression` nodes initially, but add a `MacroCall` node type if needed to preserve the unexpanded form.
- [ ] Task: Procedural Macros (Derive)
    - [ ] Handle `#[derive(...)]` attributes.
    - [ ] Potentially generate implicit methods (like `clone()`, `fmt()`) in the CPG if they are derived.
- [ ] Task: Conductor - User Manual Verification 'Macros & Metaprogramming' (Protocol in workflow.md)

## Phase 4: Memory, Ownership & Lifetimes
- [ ] Task: Lifetime Annotations
    - [ ] Update grammar/handler to parse lifetime annotations (`'a`).
    - [ ] Create a mechanism to store these on `Type` nodes (e.g., `ReferenceType` should have a `lifetime` property).
    - [ ] Test function signatures with explicit lifetimes.
- [ ] Task: Ownership Semantics
    - [ ] Review how `Move` vs `Copy` semantics can be represented.
    - [ ] Ensure `VariableDeclaration` nodes allow marking mutability (`mut`).
    - [ ] Test strict ownership transfer scenarios (though full analysis is a separate pass, the graph must support it).
- [ ] Task: Conductor - User Manual Verification 'Memory, Ownership & Lifetimes' (Protocol in workflow.md)

## Phase 5: Final Verification & Integration
- [ ] Task: Comprehensive Integration Test
    - [ ] Create a "kitchen sink" Rust file containing all supported features.
    - [ ] Verify the CPG structure contains all expected nodes and edges.
- [ ] Task: Documentation Update
    - [ ] Update `GEMINI.md` or language documentation to reflect full Rust support.
- [ ] Task: Conductor - User Manual Verification 'Final Verification & Integration' (Protocol in workflow.md)
