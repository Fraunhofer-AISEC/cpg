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

    fun handleIdentPat(identPat: RsIdentPat): Expression {
        val raw = RsAst.RustPat(RsPat.IdentPat(identPat))

        val variable =
            frontend.scopeManager.currentScope.symbols[identPat.name]
                ?.filterIsInstance<Variable>()
                ?.firstOrNull()

        variable?.let {
            val lhsRef =
                newReference(identPat.name, rawNode = raw).also { it.access = AccessValues.WRITE }
            // If identPat has a nested pattern, translate it as an assignment
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

    fun handleBoxPat(boxPat: RsBoxPat): Expression {
        val raw = RsAst.RustPat(RsPat.BoxPat(boxPat))

        val box = newObjectDeconstruction(raw)

        // Todo add type according to box pattern

        boxPat.pat.firstOrNull()?.let { box.components += handleNode(it) }

        return box
    }

    fun handleConstBlockPat(constBlockPat: RsConstBlockPat): Expression {
        val raw = RsAst.RustPat(RsPat.ConstBlockPat(constBlockPat))

        constBlockPat.blockExpr?.let {
            return frontend.expressionHandler.handleNode(RsExpr.BlockExpr(it))
        }

        return newProblemExpression("ConstBlockPat does not contain a handleable block expression")
    }

    fun handleLiteralPat(literalPat: RsLiteralPat): Expression {
        val raw = RsAst.RustPat(RsPat.LiteralPat(literalPat))

        literalPat.literal?.let {
            return frontend.expressionHandler.handleNode(RsExpr.Literal(it))
        }

        return newProblemExpression("RsLiteralPat does not contain a handleable literal")
    }

    fun handleMacroPat(macroPat: RsMacroPat): Expression {
        val raw = RsAst.RustPat(RsPat.MacroPat(macroPat))

        return newProblemExpression("MacroPat need to be resolved before translation")
    }

    fun handleOrPat(orPat: RsOrPat): Expression {
        val raw = RsAst.RustPat(RsPat.OrPat(orPat))

        val alternative = newAlternativeDeconstruction(raw)

        orPat.pats.forEach { alternative.alternatives += handleNode(it) }

        return alternative
    }

    fun handleParenPat(parenPat: RsParenPat): Expression {
        val raw = RsAst.RustPat(RsPat.ParenPat(parenPat))

        parenPat.pat.firstOrNull()?.let {
            return handleNode(it)
        }

        return newProblemExpression("ParenPat does not contain a valid subpattern")
    }

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

    fun handleRefPat(refPat: RsRefPat): Expression {
        val raw = RsAst.RustPat(RsPat.RefPat(refPat))

        refPat.pat.firstOrNull()?.let {
            val contained = handleNode(it)
            if (refPat.isRef) {
                val objectDeconstruction = newObjectDeconstruction(raw)
                objectDeconstruction.components += contained
                // Todo handle type as this behaves like a deref
                return objectDeconstruction
            } else {
                return contained
            }
        }

        return newProblemExpression("RefPat is not supported yet")
    }

    fun handleRestPat(restPat: RsRestPat): Expression {
        val raw = RsAst.RustPat(RsPat.RestPat(restPat))
        return newEmpty(rawNode = raw).also { it.usedAsExpression = true }
    }

    fun handleSlicePat(slicePat: RsSlicePat): Expression {
        val raw = RsAst.RustPat(RsPat.SlicePat(slicePat))

        return newObjectDeconstruction(raw).also { oDec ->
            slicePat.pats.forEach { oDec.components += handleNode(it) }
        }
    }

    fun handleTuplePat(tuplePat: RsTuplePat): Expression {
        val raw = RsAst.RustPat(RsPat.TuplePat(tuplePat))

        return newObjectDeconstruction(raw).also { oDec ->
            tuplePat.fields.forEach { oDec.components += handleNode(it) }
        }
    }

    fun handleTupleStructPat(tupleStructPat: RsTupleStructPat): Expression {
        val raw = RsAst.RustPat(RsPat.TupleStructPat(tupleStructPat))

        return newObjectDeconstruction(raw).also { oDec ->
            tupleStructPat.fields.forEach { oDec.components += handleNode(it) }
        }
    }

    fun handleWildcardPat(wildcardPat: RsWildcardPat): Expression {
        val raw = RsAst.RustPat(RsPat.WildcardPat(wildcardPat))
        return newEmpty(rawNode = raw).also { it.usedAsExpression = true }
    }

    fun handleRecordPatField(recordPatField: RsRecordPatField): Expression {
        val raw = RsAst.RustPat(RsPat.RecordPatField(recordPatField))

        recordPatField.pat.firstOrNull()?.let {
            return newNamedDeconstruction(raw).also { namedDec ->
                namedDec.value = handleNode(it)

                namedDec.name =
                    recordPatField.name?.let { name -> newName(name.text) } ?: namedDec.value.name
            }
        }

        return newProblemExpression("RecordPatField does not contain a valid pattern")
    }
}
