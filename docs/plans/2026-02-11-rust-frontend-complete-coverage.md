# Rust Frontend 100% Coverage Design

## Current State

The Rust CPG frontend (branch `cpg-language-rust`) handles **112 of 160** concrete tree-sitter-rust v0.21.0 node types. Only **10 produce error nodes** (ProblemExpression/ProblemDeclaration). 55 tests pass. Three commits on top of main.

## Scope

This design covers **every remaining gap** to reach 100% coverage of stable Rust constructs within CPG. Items are grouped into 5 tiers by impact-per-effort.

---

## Tier 1: Bug Fixes & Language Traits

**Impact: Critical | Effort: 1-2 hours | Files: RustLanguage.kt, ExpressionHandler.kt, DeclarationHandler.kt**

### 1.1 Fix Integer Literal Parsing (Bug — from code review)

`handleIntegerLiteral` filters with `it.isDigit() || it == '-'`, which breaks hex (`0xFF`→`0`), octal (`0o77`→`077`), and binary (`0b1010`→`01010`).

**Fix:** Parse with prefix awareness:
```kotlin
private fun handleIntegerLiteral(node: TSNode): Literal<Long> {
    val code = frontend.codeOf(node) ?: ""
    val cleaned = code.replace("_", "").removeSuffix("i8").removeSuffix("i16")
        .removeSuffix("i32").removeSuffix("i64").removeSuffix("i128").removeSuffix("isize")
        .removeSuffix("u8").removeSuffix("u16").removeSuffix("u32").removeSuffix("u64")
        .removeSuffix("u128").removeSuffix("usize")
    val value = when {
        cleaned.startsWith("0x") || cleaned.startsWith("0X") ->
            cleaned.removePrefix("0x").removePrefix("0X").toLongOrNull(16) ?: 0L
        cleaned.startsWith("0o") || cleaned.startsWith("0O") ->
            cleaned.removePrefix("0o").removePrefix("0O").toLongOrNull(8) ?: 0L
        cleaned.startsWith("0b") || cleaned.startsWith("0B") ->
            cleaned.removePrefix("0b").removePrefix("0B").toLongOrNull(2) ?: 0L
        else -> cleaned.toLongOrNull() ?: 0L
    }
    // Infer type from suffix if present, default to i32
    val typeName = when {
        code.endsWith("u8") -> "u8"; code.endsWith("u16") -> "u16"
        code.endsWith("u32") -> "u32"; code.endsWith("u64") -> "u64"
        code.endsWith("u128") -> "u128"; code.endsWith("usize") -> "usize"
        code.endsWith("i8") -> "i8"; code.endsWith("i16") -> "i16"
        code.endsWith("i64") -> "i64"; code.endsWith("i128") -> "i128"
        code.endsWith("isize") -> "isize"
        else -> "i32"
    }
    return newLiteral(value, primitiveType(typeName), rawNode = node)
}
```

### 1.2 Remove `HasDefaultArguments` (Bug — from code review)

Rust does **not** have default function arguments. Remove from class declaration.

### 1.3 Add Language Traits

Add to `RustLanguage` class declaration:

| Trait | Required Override | Value |
|---|---|---|
| `HasGenerics` | `startCharacter`, `endCharacter` | `'<'`, `'>'` |
| `HasStructs` | (marker) | — |
| `HasFirstClassFunctions` | (marker) | — |
| `HasGlobalFunctions` | (marker) | — |
| `HasGlobalVariables` | (marker) | — |
| `HasAnonymousIdentifier` | `anonymousIdentifier` | `"_"` |
| `HasFunctionPointers` | (marker) | — |
| `HasFunctionStyleConstruction` | (marker, inherits `HasCallExpressionAmbiguity`) | — |
| `HasOperatorOverloading` | `overloadedOperatorNames` | 30-entry map (see below) |

**Operator overloading map** (from `feature/language-rust` branch, with typo fix):
```kotlin
override val overloadedOperatorNames =
    mapOf(
        UnaryOperator::class of "!" to "Not::not",
        UnaryOperator::class of "*" to "Deref::deref",
        UnaryOperator::class of "-" to "Neg::neg",
        BinaryOperator::class of "!=" to "PartialEq::ne",
        BinaryOperator::class of "%" to "Rem::rem",
        BinaryOperator::class of "%=" to "RemAssign::rem_assign",
        BinaryOperator::class of "&" to "BitAnd::bitand",
        BinaryOperator::class of "&=" to "BitAndAssign::bitand_assign",
        BinaryOperator::class of "*" to "Mul::mul",
        BinaryOperator::class of "*=" to "MulAssign::mul_assign",
        BinaryOperator::class of "+" to "Add::add",
        BinaryOperator::class of "+=" to "AddAssign::add_assign",
        BinaryOperator::class of "-" to "Sub::sub",
        BinaryOperator::class of "-=" to "SubAssign::sub_assign",
        BinaryOperator::class of "/" to "Div::div",
        BinaryOperator::class of "/=" to "DivAssign::div_assign",
        BinaryOperator::class of "<<" to "Shl::shl",
        BinaryOperator::class of "<<=" to "ShlAssign::shl_assign",
        BinaryOperator::class of "<" to "PartialOrd::lt",
        BinaryOperator::class of "<=" to "PartialOrd::le",
        BinaryOperator::class of "==" to "PartialEq::eq",
        BinaryOperator::class of ">" to "PartialOrd::gt",
        BinaryOperator::class of ">=" to "PartialOrd::ge",
        BinaryOperator::class of ">>" to "Shr::shr",
        BinaryOperator::class of ">>=" to "ShrAssign::shr_assign",
        BinaryOperator::class of "^" to "BitXor::bitxor",
        BinaryOperator::class of "^=" to "BitXorAssign::bitxor_assign",
        BinaryOperator::class of "|" to "BitOr::bitor",
        BinaryOperator::class of "|=" to "BitOrAssign::bitor_assign",
    )
```

### 1.4 Add `@SupportsParallelParsing` to RustLanguageFrontend

The frontend creates a fresh `TSParser` per `parse()` call with no shared mutable state.

### 1.5 Fix DeclarationHandler else branch

Change from `ProblemDeclaration("...")` to `newProblemDeclaration("...", rawNode = node)` to preserve source location.

### 1.6 Remove dead code branches

`method_call_expression` and `tuple_index_expression` do not exist in tree-sitter-rust v0.21.0. Remove their `when` branches from ExpressionHandler.

### 1.7 Add `propagateTypeOfBinaryOperation` for string concat

```kotlin
override fun propagateTypeOfBinaryOperation(
    operatorCode: String?, lhsType: Type, rhsType: Type, hint: BinaryOperator?
): Type = when {
    operatorCode == "+" && lhsType is StringType && rhsType is StringType ->
        builtInTypes["String"] as Type
    else -> super.propagateTypeOfBinaryOperation(operatorCode, lhsType, rhsType, hint)
}
```

---

## Tier 2: Type System Fixes

**Impact: High | Effort: 2-3 hours | Files: TypeHandler.kt, ExpressionHandler.kt**

### 2.1 Fix `handleGenericType()` to preserve type arguments

Currently: `Vec<i32>` → `objectType("Vec")` (loses `<i32>`).

**Fix:** Use `objectType(name, generics = ...)`:
```kotlin
private fun handleGenericType(node: TSNode): Type {
    val typeNode = node.getChildByFieldName("type")
    val typeName = typeNode?.let { frontend.codeOf(it) } ?: frontend.codeOf(node) ?: ""
    val typeArgs = node.getChildByFieldName("type_arguments")
    val generics = mutableListOf<Type>()
    if (typeArgs != null && !typeArgs.isNull) {
        for (i in 0 until typeArgs.childCount) {
            val arg = typeArgs.getChild(i)
            if (arg.isNamed) {
                generics += handle(arg)
            }
        }
    }
    return objectType(typeName, generics)
}
```

### 2.2 Fix `handleTupleType()` to use `TupleType`

Currently: `(i32, String)` → `objectType("(i32, String)")`.

**Fix:** Build proper `TupleType`:
```kotlin
private fun handleTupleType(node: TSNode): Type {
    val elementTypes = mutableListOf<Type>()
    for (i in 0 until node.childCount) {
        val child = node.getChild(i)
        if (child.isNamed) {
            elementTypes += handle(child)
        }
    }
    return TupleType(elementTypes)
}
```

### 2.3 Fix `handleFunctionType()` to use `FunctionType`

Currently: `fn(i32) -> bool` → `objectType("fn(i32) -> bool")`.

**Fix:** Use cpg-core's `FunctionType`:
```kotlin
private fun handleFunctionType(node: TSNode): Type {
    val paramTypes = mutableListOf<Type>()
    val params = node.getChildByFieldName("parameters")
    if (params != null && !params.isNull) {
        for (i in 0 until params.childCount) {
            val child = params.getChild(i)
            if (child.isNamed) paramTypes += handle(child)
        }
    }
    val returnNode = node.getChildByFieldName("return_type")
    val returnTypes = if (returnNode != null && !returnNode.isNull) {
        listOf(handle(returnNode))
    } else { listOf(objectType("()")) }
    return FunctionType(frontend.codeOf(node) ?: "fn", paramTypes, returnTypes)
}
```

### 2.4 Handle `bounded_type` (`T + Send + Sync`)

Parse as objectType of the first type, annotated with bounds:
```kotlin
"bounded_type" -> {
    val firstType = node.getNamedChild(0)
    if (firstType != null && !firstType.isNull) handle(firstType)
    else objectType(frontend.codeOf(node) ?: "")
}
```

### 2.5 Handle `qualified_type` (`<Type as Trait>::Item`)

```kotlin
"qualified_type" -> objectType(frontend.codeOf(node) ?: "")
```

### 2.6 Handle `generic_type_with_turbofish` (`Vec::<i32>`)

Delegate to `handleGenericType` — structurally identical.

### 2.7 Fix tuple expressions to use TupleType

In `handleTupleExpression`, build a `TupleType` from element types instead of `objectType("tuple")`.

---

## Tier 3: Missing Declarations

**Impact: Medium-High | Effort: 1-2 hours | Files: DeclarationHandler.kt**

### 3.1 `union_item` → `RecordDeclaration` with kind="union"

Structurally identical to `handleStructItem` but with `kind = "union"`.

### 3.2 `foreign_mod_item` → `NamespaceDeclaration` with extern ABI

`extern "C" { fn foo(); }` — create a `NamespaceDeclaration`, store ABI string as annotation, process children as declarations.

### 3.3 `extern_crate_declaration` → `ImportDeclaration`

`extern crate foo;` — use `newImportDeclaration(name, ImportStyle.IMPORT_NAMESPACE)`.

### 3.4 `inner_attribute_item` → Annotation on TranslationUnit

`#![no_std]` — attach as annotation to the enclosing TU or module.

### 3.5 `empty_statement` → `EmptyStatement`

Trivial: `newEmptyStatement(rawNode = node)`.

### 3.6 Migrate `use_declaration` to `ImportDeclaration` (from code review)

Replace `IncludeDeclaration` with `ImportDeclaration`. Map Rust import styles:
- `use std::io` → `IMPORT_NAMESPACE`
- `use std::io::Read` → `IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE`
- `use std::io::*` → `IMPORT_ALL_SYMBOLS_FROM_NAMESPACE`
- `use std::io as stdio` → `alias = "stdio"`
- `use std::io::{Read, Write}` → expand to multiple `ImportDeclaration`s

---

## Tier 4: Semantic Depth

**Impact: Medium | Effort: 3-4 hours | Files: Various**

### 4.1 Comment extraction (from code review)

Implement `setComment` in `RustLanguageFrontend`. Tree-sitter provides `line_comment`, `block_comment`, and `doc_comment` as named extra nodes. Strategy:
- In `parse()`, after building the TU, iterate all extra nodes from the tree-sitter tree
- For each comment node, call `CommentMatcher.matchCommentToNode(text, region, tu)`
- This attaches comments to the closest CPG node by location heuristic

### 4.2 Enum variant data modeling

Rust enum variants can carry data:
- Unit: `None` — current `EnumConstantDeclaration` suffices
- Tuple: `Some(T)` — model variant fields as constructor parameters
- Struct: `Err { code: i32 }` — model variant fields as nested record

Approach: For tuple/struct variants, create a nested `RecordDeclaration` inside the `EnumDeclaration` to hold variant fields.

### 4.3 `break`/`continue` label propagation

`break 'outer 42` — set `BreakStatement.label = "outer"` and handle the break value expression.

### 4.4 `const_block` expression

`const { 1 + 2 }` — delegate to `handleBlock` with `@const` annotation.

### 4.5 `let_chain` expression

`if let Some(x) = a && let Some(y) = b` — model as chained `let_condition` with `&&` binary operator.

### 4.6 `macro_invocation` at declaration level

When a macro invocation appears at file/module scope (e.g., `include!("other.rs")`), route to `ExpressionHandler.handleMacroInvocation` instead of producing ProblemDeclaration.

---

## Tier 5: Edge Cases & Hardening

**Impact: Low | Effort: 1-2 hours | Files: Various**

### 5.1 Fix `codeOf` UTF-8 bug

`content.substring(startByte, endByte)` uses byte offsets as char indices. For non-ASCII source files, this produces incorrect code snippets. Fix: use `String(content.toByteArray(Charsets.UTF_8), startByte, endByte - startByte, Charsets.UTF_8)`.

### 5.2 Handle nightly/unstable expressions

- `yield_expression` → `ProblemExpression` is acceptable (unstable feature)
- `try_block` → delegate to `handleBlock` with `@try` annotation
- `metavariable` → `newReference("$var")`

### 5.3 TypeHandler edge cases

- `higher_ranked_trait_bound` (`for<'a> Fn(&'a str)`) → `objectType(code)`
- `removed_trait_bound` (`?Sized`) → `objectType(code)`

### 5.4 Add missing `builtInTypes`

Add `"()"` → `TupleType(listOf())` and `"!"` → `ObjectType("Never")` to the builtInTypes map (already handled in TypeHandler dispatch but not in the map).

---

## Verification

```bash
./gradlew :cpg-language-rust:test
```

All existing 55 tests must continue to pass. Each tier adds new tests. Expected final count: ~70+ tests.

## Node Coverage After All Tiers

| Category | Before | After |
|---|---|---|
| Explicitly handled | 112 | 160 |
| Error-producing | 10 | 0 (for stable Rust) |
| TypeHandler fallback (lossy) | 6 | 2 (only HRTB and removed_trait_bound) |
| Dead code branches | 2 | 0 |
