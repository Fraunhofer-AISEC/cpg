/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.commonType
import de.fraunhofer.aisec.cpg.frontends.HasImplicitReceiver
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.firstScopeParentOrNull
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.LatticeElement
import de.fraunhofer.aisec.cpg.helpers.Util.infoWithFileLocation
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.MapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.helpers.functional.TripleLattice
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import kotlin.collections.firstOrNull
import kotlin.collections.toSet

/**
 * Implements the [LatticeElement] for a lattice over a set of nodes. The lattice itself is
 * constructed by the powerset.
 */
typealias PowersetLatticeDeclarationLattice = PowersetLattice<Declaration>

typealias PowersetLatticeDeclarationElement = PowersetLattice.Element<Declaration>

typealias PowersetLatticeTypeLattice = PowersetLattice<Type>

typealias PowersetLatticeTypeElement = PowersetLattice.Element<Type>

typealias ScopeToDeclarationLattice = MapLattice<Scope, PowersetLatticeDeclarationElement>

typealias ScopeToDeclarationElement = MapLattice.Element<Scope, PowersetLatticeDeclarationElement>

typealias NodeToDeclarationLattice = MapLattice<Node, PowersetLatticeDeclarationElement>

typealias NodeToDeclarationElement = MapLattice.Element<Node, PowersetLatticeDeclarationElement>

typealias NodeToTypeLattice = MapLattice<Node, PowersetLatticeTypeElement>

typealias NodeToTypeElement = MapLattice.Element<Node, PowersetLatticeTypeElement>

typealias DeclarationStateElement =
    TripleLattice.Element<ScopeToDeclarationElement, NodeToDeclarationElement, NodeToTypeElement>

typealias DeclarationState =
    TripleLattice<ScopeToDeclarationElement, NodeToDeclarationElement, NodeToTypeElement>

val DeclarationStateElement.symbols
    get() = this.first

val DeclarationStateElement.candidates
    get() = this.second

val DeclarationStateElement.types
    get() = this.third

/**
 * Pushes the [Declaration] to the [DeclarationStateElement.symbols] and returns a new
 * [DeclarationState].
 *
 * If the [Declaration] is a [HasType], the type is also pushed to the
 * [DeclarationStateElement.types].
 */
// TODO: alex is lub'ing
fun DeclarationStateElement.pushDeclarationToScope(
    scope: Scope,
    vararg elements: Declaration,
): DeclarationStateElement {
    val newSymbols = this.symbols.duplicate()
    newSymbols.computeIfAbsent(scope) { PowersetLatticeDeclarationElement() }.addAll(elements)

    val newTypes = this.types.duplicate()
    elements.forEach { decl ->
        if (decl is HasType) {
            newTypes.computeIfAbsent(decl) { PowersetLatticeTypeElement() }.add(decl.type)
        }
    }

    return DeclarationStateElement(newSymbols, this.candidates.duplicate(), newTypes)
}

// TODO: alex is lub'ing
fun DeclarationStateElement.pushCandidate(
    scope: Node,
    vararg elements: Declaration,
): DeclarationStateElement {
    val newCandidates = this.candidates.duplicate()
    newCandidates.computeIfAbsent(scope) { PowersetLatticeDeclarationElement() }.addAll(elements)
    return DeclarationStateElement(this.symbols.duplicate(), newCandidates, this.types.duplicate())
}

// TODO: alex is lub'ing
fun DeclarationStateElement.pushType(node: Node, vararg elements: Type): DeclarationStateElement {
    val newTypes = this.types.duplicate()
    newTypes.computeIfAbsent(node) { PowersetLatticeTypeElement() }.addAll(elements)
    return DeclarationStateElement(this.symbols.duplicate(), this.candidates.duplicate(), newTypes)
}

@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(TypeResolver::class)
class SymbolResolverEOGIteration(ctx: TranslationContext) : EOGStarterPass(ctx) {
    override fun cleanup() {
        // Nothing to clean up
    }

    override fun accept(t: Node) {
        if (t !is FunctionDeclaration) {
            return
        }

        val lattice =
            DeclarationState(
                ScopeToDeclarationLattice(PowersetLatticeDeclarationLattice()),
                NodeToDeclarationLattice(PowersetLatticeDeclarationLattice()),
                NodeToTypeLattice(PowersetLatticeTypeLattice()),
            )

        var startState =
            DeclarationStateElement(
                ScopeToDeclarationElement(),
                NodeToDeclarationElement(),
                NodeToTypeElement(),
            )

        // Push all global symbols (sort of legacy, in the future they should also go through the
        // EOG)
        startState =
            startState.pushDeclarationToScope(
                ctx.scopeManager.globalScope,
                *ctx.scopeManager.globalScope.symbols.values.flatten().toTypedArray(),
            )

        // Push all record symbols (legacy, need to visit EOG in the future)
        ctx.scopeManager
            .filterScopes { it is NameScope }
            .forEach {
                startState =
                    startState.pushDeclarationToScope(
                        it,
                        *it.symbols.values.flatten().toTypedArray(),
                    )
            }

        t.scope?.let { startState = startState.pushDeclarationToScope(it) }
        val finalState = lattice.iterateEOG(t.nextEOGEdges, startState, ::transfer)

        finalState.candidates.forEach { node, candidates ->
            if (node is Reference) {
                node.candidates = candidates

                // Now it's getting interesting! We need to make the final decision based on whether
                // this a simple reference to a variable or if we are the callee of a call
                // expression
                val call = node.astParent as? CallExpression
                if (call != null) {
                    decideInvokesBasedOnCandidates(node, call)
                } else {
                    // Reference to a variable
                    node.refersTo = candidates.firstOrNull()
                }
            }
        }

        finalState.types.forEach { node, types ->
            if (node is HasType) {
                node.type = types.commonType ?: unknownType()
                node.assignedTypes = types.toSet()
            }
        }
    }

    protected open fun transfer(
        lattice: Lattice<DeclarationStateElement>,
        currentEdge: EvaluationOrder,
        state: DeclarationStateElement,
    ): DeclarationStateElement {

        val node = currentEdge.end
        return when (node) {
            is Declaration -> {
                handleDeclaration(state, node)
            }
            is Reference -> {
                handleReference(node, state)
            }
            is BinaryOperator -> {
                handleBinaryOperator(node, state)
            }
            else -> state
        }
    }

    private fun handleBinaryOperator(
        binOp: BinaryOperator,
        state: DeclarationStateElement,
    ): DeclarationStateElement {
        val lhsType = state.types[binOp.lhs]?.commonType ?: unknownType()
        val rhsType = state.types[binOp.rhs]?.commonType ?: unknownType()

        val type =
            binOp.language.propagateTypeOfBinaryOperation(binOp.operatorCode, lhsType, rhsType)

        return state.pushType(binOp, type)
    }

    private fun handleReference(
        node: Reference,
        state: DeclarationStateElement,
    ): DeclarationStateElement {
        infoWithFileLocation(
            node,
            log,
            "Resolving reference. {} scopes are active",
            state.symbols.size,
        )
        var state = state
        // Lookup symbol here or after the final state?
        var candidates =
            if (node is MemberExpression) {
                // We need to extract the scope from the base type(s) and then do a qualified
                // lookup
                val baseTypes = state.types[node.base] ?: identitySetOf()
                val scopes = baseTypes.mapNotNull { ctx.scopeManager.lookupScope(it.root.name) }
                scopes.flatMap { scope ->
                    state.symbols.resolveSymbol(
                        symbol = node.name.localName,
                        startScope = scope,
                        language = node.language,
                        qualifiedLookup = true,
                    )
                }
            } else {
                state.symbols.resolveSymbol(
                    symbol = node.name.localName,
                    startScope = node.scope!!,
                    language = node.language,
                    qualifiedLookup = false,
                )
            }
        println(candidates)

        // Let's set it here for now, but also to the final state, maybe it's helpful for
        // later
        state = state.pushCandidate(node, *candidates.toTypedArray())

        // Push the type information
        state =
            state.pushType(
                node,
                *candidates.mapNotNull { state.types[it]?.toSet() }.flatten().toTypedArray(),
            )

        return state
    }

    private fun handleDeclaration(
        state: DeclarationStateElement,
        node: Declaration,
    ): DeclarationStateElement {
        var state = state

        // Push declaration into scope
        state = node.scope?.let { state.pushDeclarationToScope(it, node) } ?: state

        // Push type of declaration
        /*if (node is HasType) {
            state = state.pushType(node, node.type)
        }*/

        return state
    }
}

// TODO: we could do this easier if we would have a lattice that combines the symbols across the
//  scopes and doing the shadowing
private fun ScopeToDeclarationElement.resolveSymbol(
    symbol: Symbol,
    language: Language<*>,
    startScope: Scope,
    qualifiedLookup: Boolean,
): IdentitySet<Declaration> {
    val symbols = identitySetOf<Declaration>()
    var scope: Scope? = startScope

    // TODO: support modified scope
    val modifiedScoped = null

    while (scope != null) {
        var scopeSymbols = this[scope]
        scopeSymbols?.filterTo(symbols) { it.name.localName == symbol }
        if (symbols.isNotEmpty() == true) {
            return symbols
        }

        // If we didn't find the symbol in the current scope, we need to check the parent scope
        scope =
            if (qualifiedLookup || modifiedScoped != null) {
                break
            } else {
                // If our language needs explicit lookup for fields (and other class members),
                // we need to skip record scopes unless we are in a qualified lookup
                if (language !is HasImplicitReceiver && scope.parent is RecordScope) {
                    scope.firstScopeParentOrNull { it !is RecordScope }
                } else {
                    // Otherwise, we can just go to the next parent
                    scope.parent
                }
            }
    }

    return symbols
}
