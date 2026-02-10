# Specification: Comprehensive Rust Language Frontend

## 1. Overview
This track aims to elevate the experimental `cpg-language-rust` frontend to a "full and complete" status. It focuses on implementing support for Rust's most advanced and unique features, ensuring the Code Property Graph (CPG) accurately reflects the semantics of idiomatic Rust code.

## 2. Goals
- **Full Macro Support:** Implement robust handling for macro expansion, including `macro_rules!` and procedural macros.
- **Advanced Type System:** Support Rust's trait system, generics (without monomorphization), and associated types.
- **Memory & Lifetimes:** Explicitly model ownership, borrowing, and lifetime annotations within the graph.
- **Modern Control Flow:** Support `async`/`await` patterns and advanced pattern matching guards.

## 3. Requirements

### 3.1. Macros & Metaprogramming
- **Expansion Mechanism:** Implement a mechanism to expand `macro_rules!` invocations *before* or *during* AST traversal to ensure the CPG reflects the expanded code.
- **Procedural Macros:** Provide a strategy for handling standard procedural macros (e.g., `#[derive]`).
- **Source Mapping:** Ensure nodes generated from macros link back to their original source location where possible.

### 3.2. Traits & Generics
- **Generic Representation:** Model generic functions and structs as `TemplateDeclaration` nodes.
- **No Monomorphization:** Do *not* generate concrete instances for every type usage. Instead, use usage edges (`instantiates` or similar) to link call sites to the generic definition, preserving the generic nature of the graph.
- **Trait Bounds:** Capture `where` clauses and trait bounds in the CPG, likely as properties of the `Type` or `TypeParameter` nodes.
- **Associated Types:** Model associated types within traits.

### 3.3. Memory, Ownership & Lifetimes
- **Lifetime Nodes:** Introduce nodes or properties to represent lifetime annotations (`'a`, `'static`).
- **Ownership Edges:** Where explicit, model ownership transfer (moves) and borrowing (references) in the Data Flow Graph (DFG).
- **Borrow Checking Support:** Ensure sufficient data is extracted (mutability, references, lifetimes) to enable a subsequent "Borrow Checker" analysis pass.

### 3.4. Advanced Control Flow
- **Async/Await:** Model `async` functions and `.await` calls. Consider representing the underlying state machine state or treating them as special control flow edges.
- **Pattern Matching:** Extend current `match` support to handle "Match Guards" (`match x { y if y > 5 => ... }`) and destructuring patterns.
- **Control Flow:** Support `if let`, `while let`, and loop labels.

## 4. Architecture
- **Extensions:**
    - Extend `RustLanguageFrontend` to integrate a macro expansion phase.
    - Enhance `TypeHandler` for complex trait bounds and lifetime syntax.
    - Update `StatementHandler` for async/await constructs.
- **New Nodes (Potential):**
    - `LifetimeDeclaration` (if not already present).
    - `MacroCallExpression` (for unexpanded or declarative representation).

## 5. Constraints
- **Graph Size:** Avoid explosion of nodes due to macro expansion; keep the graph optimized.
- **Performance:** Macro expansion should be efficient.
