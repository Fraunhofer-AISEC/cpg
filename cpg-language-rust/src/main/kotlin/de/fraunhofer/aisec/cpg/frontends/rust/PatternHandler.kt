/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.newAlternativeDeconstruction
import de.fraunhofer.aisec.cpg.graph.newAssign
import de.fraunhofer.aisec.cpg.graph.newDeclarationStatement
import de.fraunhofer.aisec.cpg.graph.newEmpty
import de.fraunhofer.aisec.cpg.graph.newName
import de.fraunhofer.aisec.cpg.graph.newNamedDeconstruction
import de.fraunhofer.aisec.cpg.graph.newObjectDeconstruction
import de.fraunhofer.aisec.cpg.graph.newProblemExpression
import de.fraunhofer.aisec.cpg.graph.newRange
import de.fraunhofer.aisec.cpg.graph.newReference
import de.fraunhofer.aisec.cpg.graph.newVariable
import uniffi.rustast.RsAst
import uniffi.rustast.RsBoxPat
import uniffi.rustast.RsConstBlockPat
import uniffi.rustast.RsExpr
import uniffi.rustast.RsIdentPat
import uniffi.rustast.RsLiteralPat
import uniffi.rustast.RsMacroPat
import uniffi.rustast.RsOrPat
import uniffi.rustast.RsParenPat
import uniffi.rustast.RsPat
import uniffi.rustast.RsPathPat
import uniffi.rustast.RsRangePat
import uniffi.rustast.RsRecordPat
import uniffi.rustast.RsRecordPatField
import uniffi.rustast.RsRefPat
import uniffi.rustast.RsRestPat
import uniffi.rustast.RsSlicePat
import uniffi.rustast.RsTuplePat
import uniffi.rustast.RsTupleStructPat
import uniffi.rustast.RsWildcardPat

class PatternHandler(frontend: RustLanguageFrontend) :
    RustHandler<Expression, RsAst.RustPat>(::ProblemExpression, frontend) {

    override fun handleNode(node: RsAst.RustPat): Expression {
        val unwrapped = node.v1
        return handleNode(unwrapped)
    }

    fun handleNode(node: RsPat): Expression {
        return when (node) {
            is RsPat.BoxPat -> handleBoxPat(node.v1)
            is RsPat.ConstBlockPat -> handleConstBlockPat(node.v1)
            is RsPat.IdentPat -> handleIdentPat(node.v1)
            is RsPat.LiteralPat -> handleLiteralPat(node.v1)
            is RsPat.MacroPat -> handleMacroPat(node.v1)
            is RsPat.OrPat -> handleOrPat(node.v1)
            is RsPat.ParenPat -> handleParenPat(node.v1)
            is RsPat.PathPat -> handlePathPat(node.v1)
            is RsPat.RangePat -> handleRangePat(node.v1)
            is RsPat.RecordPat -> handleRecordPat(node.v1)
            is RsPat.RefPat -> handleRefPat(node.v1)
            is RsPat.RestPat -> handleRestPat(node.v1)
            is RsPat.SlicePat -> handleSlicePat(node.v1)
            is RsPat.TuplePat -> handleTuplePat(node.v1)
            is RsPat.TupleStructPat -> handleTupleStructPat(node.v1)
            is RsPat.WildcardPat -> handleWildcardPat(node.v1)
            is RsPat.RecordPatField -> handleRecordPatField(node.v1)
        }
    }

    /**
     * Handles an identifier (binding) pattern: `name`, `ref mut name`, or `name @ subpattern`. If
     * the name already resolves to a [Variable] in scope (e.g. reused as a loop/match binding
     * inside the same scope), it produces a [Reference] to it (or an [Assign] if there is a nested
     * `@` subpattern); otherwise it declares a fresh [Variable].
     *
     * AST:
     * [ast::IdentPat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.IdentPat.html)
     *
     * Reference:
     * [Identifier patterns](https://doc.rust-lang.org/reference/patterns.html#identifier-patterns)
     */
    fun handleIdentPat(identPat: RsIdentPat): Expression {
        val raw = RsAst.RustPat(RsPat.IdentPat(identPat))

        val variable =
            identPat.name?.let {
                frontend.scopeManager
                    .lookupSymbolByName(
                        newName(
                            it,
                            doNotPrependNamespace = false,
                            frontend.scopeManager.currentNamespace,
                        ),
                        language,
                    )
                    .filterIsInstance<Variable>()
                    .firstOrNull()
            }

        variable?.let { existing ->
            val lhsRef =
                newReference(identPat.name, rawNode = raw).also {
                    it.access = AccessValues.WRITE
                    it.refersTo = existing
                }
            identPat.pat.firstOrNull()?.let { nestedPat ->
                val rhs = handleNode(nestedPat)
                return newAssign("=", listOf(lhsRef), listOf(rhs), rawNode = raw)
            }

            return lhsRef
        }

        return newDeclarationStatement(rawNode = raw).also { declaration ->
            declaration.usedAsExpression = true
            val variable = newVariable(rawNode = raw, name = identPat.name)
            declaration.declarations += variable

            // If the pattern is empty we use an empty expression as initializer, it forwards dfgs
            // that are pointing to it
            // during deconstruction
            variable.initializer =
                identPat.pat.firstOrNull()?.let { handleNode(it) }
                    ?: newEmpty(raw).also { it.usedAsExpression = true }
            frontend.scopeManager.addDeclaration(variable)
        }
    }

    /**
     * Handles a (nightly-only) box pattern `box subpattern`, used to match through a `Box<T>`, into
     * an [ObjectDeconstruction] wrapping the inner pattern.
     *
     * AST: [ast::BoxPat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.BoxPat.html)
     *
     * Reference: not part of stable Rust; see the general
     * [patterns](https://doc.rust-lang.org/reference/patterns.html) overview.
     */
    fun handleBoxPat(boxPat: RsBoxPat): Expression {
        val raw = RsAst.RustPat(RsPat.BoxPat(boxPat))

        val box = newObjectDeconstruction(raw)

        boxPat.pat.firstOrNull()?.let { box.components += handleNode(it) }

        return box
    }

    /**
     * Handles a `const { ... }` block used in pattern position, delegating to the block expression
     * handler for its inner const context.
     *
     * AST:
     * [ast::ConstBlockPat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.ConstBlockPat.html)
     *
     * Reference:
     * [Inline const blocks](https://doc.rust-lang.org/reference/expressions/block-expr.html#inline-const-blocks)
     */
    fun handleConstBlockPat(constBlockPat: RsConstBlockPat): Expression {
        val raw = RsAst.RustPat(RsPat.ConstBlockPat(constBlockPat))

        constBlockPat.blockExpr?.let {
            return frontend.expressionHandler.handleNode(RsExpr.BlockExpr(it))
        }

        return newProblemExpression("ConstBlockPat does not contain a handleable block expression")
    }

    /**
     * Handles a literal pattern (matching an exact literal value, e.g. `0`, `"foo"`, `true`) by
     * delegating to the literal expression handler.
     *
     * AST:
     * [ast::LiteralPat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.LiteralPat.html)
     *
     * Reference:
     * [Literal patterns](https://doc.rust-lang.org/reference/patterns.html#literal-patterns)
     */
    fun handleLiteralPat(literalPat: RsLiteralPat): Expression {
        val raw = RsAst.RustPat(RsPat.LiteralPat(literalPat))

        literalPat.literal?.let {
            return frontend.expressionHandler.handleNode(RsExpr.Literal(it))
        }

        return newProblemExpression("RsLiteralPat does not contain a handleable literal")
    }

    /**
     * Handles a macro invocation used in pattern position; not currently supported, macro expansion
     * would need to happen before translation.
     *
     * AST:
     * [ast::MacroPat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.MacroPat.html)
     *
     * Reference: [Macros](https://doc.rust-lang.org/reference/macros.html)
     */
    fun handleMacroPat(macroPat: RsMacroPat): Expression {
        val raw = RsAst.RustPat(RsPat.MacroPat(macroPat))

        return newProblemExpression("MacroPat need to be resolved before translation")
    }

    /**
     * Handles an or-pattern `pat1 | pat2 | ...` into an [AlternativeDeconstruction] listing each
     * alternative.
     *
     * AST: [ast::OrPat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.OrPat.html)
     *
     * Reference: [Or-patterns](https://doc.rust-lang.org/reference/patterns.html#or-patterns)
     */
    fun handleOrPat(orPat: RsOrPat): Expression {
        val raw = RsAst.RustPat(RsPat.OrPat(orPat))

        val alternative = newAlternativeDeconstruction(raw)

        orPat.pats.forEach { alternative.alternatives += handleNode(it) }

        return alternative
    }

    /**
     * Handles a parenthesized pattern `(pat)`, unwrapped and translated directly (parentheses carry
     * no separate CPG representation).
     *
     * AST:
     * [ast::ParenPat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.ParenPat.html)
     *
     * Reference:
     * [Grouped patterns](https://doc.rust-lang.org/reference/patterns.html#grouped-patterns)
     */
    fun handleParenPat(parenPat: RsParenPat): Expression {
        val raw = RsAst.RustPat(RsPat.ParenPat(parenPat))

        parenPat.pat.firstOrNull()?.let {
            return handleNode(it)
        }

        return newProblemExpression("ParenPat does not contain a valid subpattern")
    }

    /**
     * Handles a path pattern referring to a unit struct, enum variant, or constant by path (e.g.
     * `None`, `MyEnum::Variant`), into a [Reference].
     *
     * AST: [ast::PathPat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.PathPat.html)
     *
     * Reference: [Path patterns](https://doc.rust-lang.org/reference/patterns.html#path-patterns)
     */
    fun handlePathPat(pathPat: RsPathPat): Expression {
        val raw = RsAst.RustPat(RsPat.PathPat(pathPat))

        pathPat.path?.let { rsPath ->
            return newReference(
                frontend.handleKeywordsInNames(frontend.handlePathForRef(rsPath) ?: newName("")),
                rawNode = raw,
            )
        }

        return newProblemExpression("RsPathPat cannot be parsed properly")
    }

    /**
     * Handles a range pattern (`a..=b`, `a..b`, `..=b`, ...) into a [Range] with floor/ceiling and
     * the range operator preserved.
     *
     * AST:
     * [ast::RangePat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.RangePat.html)
     *
     * Reference: [Range patterns](https://doc.rust-lang.org/reference/patterns.html#range-patterns)
     */
    fun handleRangePat(rangePat: RsRangePat): Expression {
        val raw = RsAst.RustPat(RsPat.RangePat(rangePat))

        val range = newRange(rawNode = raw)

        rangePat.patterns.getOrNull(0)?.let {
            range.floor = frontend.patternHandler.handle(RsAst.RustPat(it))
        }

        rangePat.patterns.getOrNull(1)?.let {
            range.ceiling = frontend.patternHandler.handle(RsAst.RustPat(it))
        }

        range.operatorCode = rangePat.operator

        return range
    }

    /**
     * Handles a struct/record pattern with named fields (`Path { field, other: pat, .. }`) into an
     * [ObjectDeconstruction] typed to the matched path, with each field handled by
     * [handleRecordPatField].
     *
     * AST:
     * [ast::RecordPat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.RecordPat.html)
     *
     * Reference:
     * [Struct patterns](https://doc.rust-lang.org/reference/patterns.html#struct-patterns)
     */
    fun handleRecordPat(recordPat: RsRecordPat): Expression {
        val raw = RsAst.RustPat(RsPat.RecordPat(recordPat))

        val objectDeconstruction = newObjectDeconstruction(raw)

        recordPat.path?.let { rsPath ->
            // Todo If I set a type base on a name, shouldn't the resolution then use the scope
            objectDeconstruction.type =
                frontend.typeOf(
                    frontend
                        .handleKeywordsInNames(frontend.handlePathForRef(rsPath) ?: newName(""))
                        .toString()
                )
        }

        recordPat.fields.forEach { field ->
            objectDeconstruction.components += handleRecordPatField(field)
        }

        return objectDeconstruction
    }

    /**
     * Handles a reference pattern (`&pat`, `&mut pat`) by wrapping the inner pattern in an
     * [ObjectDeconstruction] when `&` is present, or passing it through unwrapped otherwise.
     *
     * AST: [ast::RefPat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.RefPat.html)
     *
     * Reference:
     * [Reference patterns](https://doc.rust-lang.org/reference/patterns.html#reference-patterns)
     */
    fun handleRefPat(refPat: RsRefPat): Expression {
        val raw = RsAst.RustPat(RsPat.RefPat(refPat))

        refPat.pat.firstOrNull()?.let {
            val contained = handleNode(it)
            return if (refPat.isRef) {
                val objectDeconstruction = newObjectDeconstruction(raw)
                objectDeconstruction.components += contained
                objectDeconstruction
            } else {
                contained
            }
        }

        return newProblemExpression("RefPat is not supported yet")
    }

    /**
     * Handles the rest pattern `..` inside a tuple/slice/struct pattern into an [Empty] expression,
     * since it binds nothing and only skips remaining elements/fields.
     *
     * AST: [ast::RestPat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.RestPat.html)
     *
     * Reference: [Rest patterns](https://doc.rust-lang.org/reference/patterns.html#rest-patterns)
     */
    fun handleRestPat(restPat: RsRestPat): Expression {
        val raw = RsAst.RustPat(RsPat.RestPat(restPat))
        return newEmpty(rawNode = raw).also { it.usedAsExpression = true }
    }

    /**
     * Handles a slice/array pattern (`[a, b, ..rest]`) into an [ObjectDeconstruction] whose
     * components are the translated element patterns.
     *
     * AST:
     * [ast::SlicePat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.SlicePat.html)
     *
     * Reference: [Slice patterns](https://doc.rust-lang.org/reference/patterns.html#slice-patterns)
     */
    fun handleSlicePat(slicePat: RsSlicePat): Expression {
        val raw = RsAst.RustPat(RsPat.SlicePat(slicePat))

        return newObjectDeconstruction(raw).also { oDec ->
            slicePat.pats.forEach { oDec.components += handleNode(it) }
        }
    }

    /**
     * Handles a tuple pattern (`(a, b, c)`) into an [ObjectDeconstruction] whose components are the
     * translated element patterns.
     *
     * AST:
     * [ast::TuplePat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.TuplePat.html)
     *
     * Reference: [Tuple patterns](https://doc.rust-lang.org/reference/patterns.html#tuple-patterns)
     */
    fun handleTuplePat(tuplePat: RsTuplePat): Expression {
        val raw = RsAst.RustPat(RsPat.TuplePat(tuplePat))

        return newObjectDeconstruction(raw).also { oDec ->
            tuplePat.fields.forEach { oDec.components += handleNode(it) }
        }
    }

    /**
     * Handles a tuple-struct/enum-variant pattern (`Some(x)`, `Point(x, y)`) into an
     * [ObjectDeconstruction] typed to the matched path, with components from the positional
     * sub-patterns.
     *
     * AST:
     * [ast::TupleStructPat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.TupleStructPat.html)
     *
     * Reference:
     * [Tuple struct patterns](https://doc.rust-lang.org/reference/patterns.html#tuple-struct-patterns)
     */
    fun handleTupleStructPat(tupleStructPat: RsTupleStructPat): Expression {
        val raw = RsAst.RustPat(RsPat.TupleStructPat(tupleStructPat))

        val objectDeconstruction = newObjectDeconstruction(raw)

        tupleStructPat.path?.let { rsPath ->
            objectDeconstruction.type =
                frontend.typeOf(
                    frontend
                        .handleKeywordsInNames(frontend.handlePathForRef(rsPath) ?: newName(""))
                        .toString()
                )
        }

        tupleStructPat.fields.forEach { objectDeconstruction.components += handleNode(it) }

        return objectDeconstruction
    }

    /**
     * Handles the wildcard pattern `_`, which matches any value and binds nothing, into an [Empty]
     * expression.
     *
     * AST:
     * [ast::WildcardPat](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.WildcardPat.html)
     *
     * Reference:
     * [Wildcard pattern](https://doc.rust-lang.org/reference/patterns.html#wildcard-pattern)
     */
    fun handleWildcardPat(wildcardPat: RsWildcardPat): Expression {
        val raw = RsAst.RustPat(RsPat.WildcardPat(wildcardPat))
        return newEmpty(rawNode = raw).also { it.usedAsExpression = true }
    }

    /**
     * Handles a single field inside a struct pattern (`name: pat` or the `name` shorthand) into a
     * [NamedDeconstruction].
     *
     * AST:
     * [ast::RecordPatField](https://docs.rs/ra_ap_syntax/latest/ra_ap_syntax/ast/struct.RecordPatField.html)
     *
     * Reference: part of
     * [Struct patterns](https://doc.rust-lang.org/reference/patterns.html#struct-patterns)
     */
    fun handleRecordPatField(recordPatField: RsRecordPatField): Expression {
        val raw = RsAst.RustPat(RsPat.RecordPatField(recordPatField))

        recordPatField.pat.firstOrNull()?.let {
            return newNamedDeconstruction(raw).also { namedDec ->
                namedDec.value = handleNode(it)

                namedDec.name =
                    recordPatField.name?.let { name -> newName(name.text) } ?: namedDec.value.name
            }
        }

        return newProblemExpression("RecordPatField missing valid pattern", rawNode = raw)
    }
}
