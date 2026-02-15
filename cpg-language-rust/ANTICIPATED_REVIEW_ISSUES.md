# Anticipated Review Issues for Rust Frontend

Based on review comments from PR #2559 by @oxisto, the following patterns
should be anticipated and fixed proactively.

---

## 1. Missing KDoc on Handler Functions

**Review pattern**: "Can you add some Javadoc here, following the general style of
other handlers (e.g. LLVM or CXX)?"

**Occurrences**:

### ExpressionHandler.kt (functions without KDoc)
- `handleIntegerLiteral` (line 125)
- `handleStringLiteral` (line 175)
- `handleBooleanLiteral` (line 195)
- `handleIdentifier` (line 201)
- `handleUnaryExpression` (line 219)
- `handleAssignmentExpression` (line 231)
- `handleCompoundAssignmentExpression` (line 248)
- `handleTupleExpression` (line 267)
- `handleArrayExpression` (line 278)
- `handleGenericCallExpression` (line 332)
- `handleFieldExpression` (line 370)
- `handleMacroInvocation` (line 490)
- `handleLetCondition` (line 514)
- `handleBreakExpression` (line 528)
- `handleContinueExpression` (line 539)
- `handleAwaitExpression` (line 550)
- `handleIndexExpression` (line 608)
- `handleRangeExpression` (line 621)
- `handleTryExpression` (line 651)
- `handleTypeCastExpression` (line 661)
- `handleNegativeLiteral` (line 728)
- `handleFloatLiteral` (line 742)
- `handleCharLiteral` (line 750)
- `handleScopedIdentifier` (line 756)
- `handleReferenceExpression` (line 762)
- `handleUnsafeBlock` (line 783)
- `handleAsyncBlock` (line 791)
- `handleRawStringLiteral` (line 798)
- `handleGenericFunctionReference` (line 821)

### DeclarationHandler.kt (functions without KDoc)
- `handleWhereClause` (line 161)
- `handleTypeParameters` (line 183)
- `parseTraitBounds` (line 221)
- `handleParameters` (line 233)
- `handleSelfParameter` (line 264)
- `handleTypeItem` (line 598)
- `handleAssociatedType` (line 611)
- `handleMacroDefinition` (line 632)
- `handleConstItem` (line 645)
- `handleStaticItem` (line 666)
- `handleUseDeclaration` (line 695)
- `handleUnionItem` (line 705)
- `handleForeignModItem` (line 736)
- `handleExternCrateDeclaration` (line 768)
- `handleInnerAttributeItem` (line 777)
- `handleMacroInvocationDecl` (line 792)

### StatementHandler.kt (functions without KDoc)
- `handleBlock` (line 72)
- `handleTupleLetDeclaration` (line 186)
- `handleReturnExpression` (line 218)
- `handleWhileExpression` (line 289)
- `handleLoopExpression` (line 330)
- `handleExpressionStatement` (line 553)

### TypeHandler.kt (ALL functions without KDoc)
- `handlePrimitiveType` (line 70)
- `handleTypeIdentifier` (line 74)
- `handleReferenceType` (line 78)
- `handleTupleType` (line 85)
- `handleArrayType` (line 96)
- `handleGenericType` (line 102)
- `handleFunctionType` (line 118)
- `handleAbstractType` (line 142)
- `handleDynamicType` (line 149)
- `handleScopedTypeIdentifier` (line 156)
- `handlePointerType` (line 161)

---

## 2. Use `modifiers` Instead of `newAnnotation()` for Language Keywords

**Review pattern**: "We now have a `modifiers` property on nodes, can we use that
instead of an annotation?"

**Status**: PARTIALLY APPLICABLE. The `modifiers` property only exists on
`ParameterDeclaration` and `FieldDeclaration` in CPG core. It does NOT exist on
`FunctionDeclaration`, `VariableDeclaration`, `Block`, or generic `Declaration`.
Until broader `modifiers` support is added to CPG core, most of these must remain
as annotations.

**Already fixed**: Annotation name normalized from `"Async"` to `"async"` (lowercase)
to match the actual Rust keyword.

**Can use modifiers (FieldDeclaration/ParameterDeclaration)**:

| File | Line | Current Code | Fix |
|------|------|-------------|-----|
| RustLanguageFrontend.kt | 102 | `newAnnotation(visibilityText)` on field | Use `field.modifiers += visibilityText` when target is FieldDeclaration |

**Must remain as annotations (no `modifiers` property on these types)**:

| File | Line | Current Code | Reason |
|------|------|-------------|--------|
| DeclarationHandler.kt | 104, 111 | `newAnnotation("async")` | FunctionDeclaration has no `modifiers` |
| DeclarationHandler.kt | 279 | `newAnnotation("mut")` on self param | ParameterDeclaration — could use modifiers |
| DeclarationHandler.kt | 650 | `newAnnotation("const")` | VariableDeclaration has no `modifiers` |
| DeclarationHandler.kt | 671 | `newAnnotation("static")` | VariableDeclaration has no `modifiers` |
| DeclarationHandler.kt | 676 | `newAnnotation("mut")` | VariableDeclaration has no `modifiers` |
| StatementHandler.kt | 154 | `newAnnotation("mut")` | VariableDeclaration has no `modifiers` |
| ExpressionHandler.kt | 787 | `newAnnotation("unsafe")` | Block has no `modifiers` |
| ExpressionHandler.kt | 794 | `newAnnotation("async")` | Block has no `modifiers` |

---

## 3. Use `node.text()` Instead of `frontend.codeOf(node)`

**Review pattern**: "Can we use text() here?"

**Occurrences**:

| File | Line | Current Code | Fix |
|------|------|-------------|-----|
| TypeHandler.kt | 61 | `frontend.codeOf(node) ?: ""` | `node.text()` |
| TypeHandler.kt | 63 | `frontend.codeOf(node) ?: ""` | `node.text()` |
| TypeHandler.kt | 104 | `frontend.codeOf(node) ?: ""` | `node.text()` |
| TypeHandler.kt | 135 | `frontend.codeOf(node) ?: "fn"` | `node.text().ifEmpty { "fn" }` |
| RustLanguageFrontend.kt | 101 | `codeOf(grandchild) ?: "pub"` | `grandchild.text().ifEmpty { "pub" }` (needs context receiver) |
| RustLanguageFrontend.kt | 117 | `codeOf(child) ?: ""` | Already in frontend, keep as-is |

Note: `text()` is a context receiver extension on `RustHandler`. In `RustLanguageFrontend`
itself, we cannot use it directly — `codeOf()` is correct there.

---

## 4. Use `getOrNull` in Tests Instead of Direct Array Indexing

**Review pattern**: "Can we use getOrNull here? Direct indexing can throw
ArrayIndexOutOfBoundsException in tests, hiding the real failure."

**Occurrences** (direct `[N]` indexing):

| File | Lines |
|------|-------|
| RustFrontendTest.kt | 57, 110, 118, 126, 134, 161-163, 179, 260, 262 |
| RustControlFlowTest.kt | 70, 76, 114, 120 |
| RustEnumTest.kt | 51-53, 60-61, 68-70 |
| RustDestructuringTest.kt | 60-61 |
| RustTraitsTest.kt | 70 |

---

## 5. `handleFunctionSignatureItem` Always Returns MethodDeclaration

**Review pattern**: "Is that always a method? What if it's not in a record scope?"

**Current behavior**: `handleFunctionSignatureItem` (DeclarationHandler.kt:446)
always returns `MethodDeclaration`, even when not inside a record scope (though
in practice it's only called from trait and extern block contexts where a record
scope IS present). The return type is explicitly `MethodDeclaration`.

**Decision**: Keep as-is since function signatures only appear in trait/extern
blocks where a record scope exists. Add a KDoc comment explaining this.

---

## Summary

| Category | Count | Status |
|----------|-------|--------|
| Missing KDoc | ~60 functions | FIXED — KDoc added to all handler functions |
| Annotations -> Modifiers | 9 occurrences | BLOCKED — `modifiers` only on ParameterDeclaration/FieldDeclaration; normalized "Async" to "async" |
| codeOf -> text() | 4 in TypeHandler | FIXED — replaced with `node.text()` |
| getOrNull in tests | ~25 occurrences | TODO — low priority |
| handleFunctionSignatureItem type | 1 | Info (keep as-is) |
