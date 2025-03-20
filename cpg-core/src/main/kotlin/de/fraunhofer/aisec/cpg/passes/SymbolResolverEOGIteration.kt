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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.LatticeElement
import de.fraunhofer.aisec.cpg.helpers.Util.infoWithFileLocation
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.MapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.helpers.functional.TupleLattice
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn

/**
 * Implements the [LatticeElement] for a lattice over a set of nodes. The lattice itself is
 * constructed by the powerset.
 */
typealias PowersetLatticeDeclarationLattice = PowersetLattice<Declaration>

typealias PowersetLatticeDeclarationElement = PowersetLattice.Element<Declaration>

typealias ScopeToDeclarationLattice = MapLattice<Scope, PowersetLatticeDeclarationElement>

typealias ScopeToDeclarationElement = MapLattice.Element<Scope, PowersetLatticeDeclarationElement>

typealias NodeToDeclarationLattice = MapLattice<Node, PowersetLatticeDeclarationElement>

typealias NodeToDeclarationElement = MapLattice.Element<Node, PowersetLatticeDeclarationElement>

typealias DeclarationStateElement =
    TupleLattice.Element<ScopeToDeclarationElement, NodeToDeclarationElement>

typealias DeclarationState = TupleLattice<ScopeToDeclarationElement, NodeToDeclarationElement>

val DeclarationStateElement.symbols
    get() = this.first

val DeclarationStateElement.candidates
    get() = this.second

fun DeclarationStateElement.pushScope(
    scope: Scope,
    vararg elements: Declaration,
): DeclarationStateElement {
    val newSymbols = this.symbols.duplicate()
    newSymbols.computeIfAbsent(scope) { PowersetLatticeDeclarationElement() }.addAll(elements)
    return DeclarationStateElement(newSymbols, this.candidates.duplicate())
}

fun DeclarationStateElement.pushCandidate(
    scope: Node,
    vararg elements: Declaration,
): DeclarationStateElement {
    val newCandidates = this.candidates.duplicate()
    newCandidates.computeIfAbsent(scope) { PowersetLatticeDeclarationElement() }.addAll(elements)
    return DeclarationStateElement(this.symbols.duplicate(), newCandidates)
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
            )

        val startState =
            DeclarationStateElement(ScopeToDeclarationElement(), NodeToDeclarationElement())

        // Push all global symbols (sort of legacy, in the future they should also go through the
        // EOG)
        startState.pushScope(
            ctx.scopeManager.globalScope,
            *ctx.scopeManager.globalScope.symbols.values.flatten().toTypedArray(),
        )

        // Push all record symbols (legacy, need to visit EOG in the future)
        ctx.scopeManager
            .filterScopes { it is NameScope }
            .forEach { startState.pushScope(it, *it.symbols.values.flatten().toTypedArray()) }

        t.scope?.let { startState.pushScope(it) }
        val finalState = lattice.iterateEOG(t.nextEOGEdges, startState, ::transfer)

        finalState.symbols.forEach { println(it) }
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
            else -> state
        }
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
                // We need to extract the scope from the base type and then do a qualified
                // lookup
                // TODO: lookup based on assigned types as well
                val baseType = node.base.type
                val scope = ctx.scopeManager.lookupScope(baseType.root.name)
                state.symbols.resolveSymbol(
                    symbol = node.name.localName,
                    startScope = scope!!,
                    language = node.language,
                    qualifiedLookup = true,
                )
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

        return state
    }

    private fun handleDeclaration(
        state: DeclarationStateElement,
        node: Declaration,
    ): DeclarationStateElement {
        // Push declaration into scope
        return node.scope?.let { state.pushScope(it, node) } ?: state
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
