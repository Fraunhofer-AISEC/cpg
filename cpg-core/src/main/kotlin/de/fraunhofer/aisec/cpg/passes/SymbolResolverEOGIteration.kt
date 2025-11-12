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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
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
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log
import kotlin.collections.toSet
import kotlinx.coroutines.runBlocking

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
fun DeclarationStateElement.pushDeclarationToScope(
    lattice: DeclarationState,
    scope: Scope,
    vararg elements: Declaration,
): DeclarationStateElement {
    return runBlocking {
        lattice.lub(
            this@pushDeclarationToScope,
            DeclarationStateElement(
                ScopeToDeclarationElement(scope to PowersetLatticeDeclarationElement(*elements)),
                NodeToDeclarationElement(),
                NodeToTypeElement(
                    *elements
                        .mapNotNull { it as? HasType }
                        .map { it as Node to PowersetLatticeTypeElement(it.type) }
                        .toTypedArray()
                ),
            ),
            true,
        )
    }
}

fun DeclarationStateElement.pushCandidate(
    lattice: DeclarationState,
    scope: Node,
    vararg elements: Declaration,
): DeclarationStateElement {
    return runBlocking {
        lattice.lub(
            this@pushCandidate,
            DeclarationStateElement(
                ScopeToDeclarationElement(),
                NodeToDeclarationElement(scope to PowersetLatticeDeclarationElement(*elements)),
                NodeToTypeElement(),
            ),
            true,
        )
    }
}

fun DeclarationStateElement.pushType(
    lattice: DeclarationState,
    node: Node,
    vararg elements: Type,
): DeclarationStateElement {
    return runBlocking {
        lattice.lub(
            this@pushType,
            DeclarationStateElement(
                ScopeToDeclarationElement(),
                NodeToDeclarationElement(),
                NodeToTypeElement(node to PowersetLatticeTypeElement(*elements)),
            ),
            true,
        )
    }
}

/**
 * This method is used to resolve symbols in the AST. It uses the EOG (evaluation order graph) to
 * iterate over the nodes in the graph and resolve the symbols.
 *
 * It uses the power of the [Lattice.iterateEOG] to iterate over the nodes in the graph and built a
 * state of:
 * - [ScopeToDeclarationLattice] - a mapping of scopes to declarations
 * - [NodeToDeclarationLattice] - a mapping of nodes to declarations
 * - [NodeToTypeLattice] - a mapping of nodes to types
 *
 * After the iteration, we set the following based on the final state:
 * - [Reference.candidates] - the candidates for the reference
 * - [Reference.refersTo] - the final declaration for the reference
 * - [CallExpression.invokes] - the final declaration for the call expression
 * - [HasType.type] - the type of the node
 * - [HasType.assignedTypes] - the assigned types of the node
 */
fun SymbolResolver.acceptWithIterateEOG(t: Node) {
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
            lattice,
            ctx.scopeManager.globalScope,
            *ctx.scopeManager.globalScope.symbols.values.flatten().toTypedArray(),
        )

    // Push all record symbols (legacy, need to visit EOG in the future)
    ctx.scopeManager
        .filterScopes { it is NameScope }
        .forEach {
            startState =
                startState.pushDeclarationToScope(
                    lattice,
                    it,
                    *it.symbols.values.flatten().toTypedArray(),
                )
        }

    t.scope?.let { startState = startState.pushDeclarationToScope(lattice, it) }
    val finalState =
        runBlocking { lattice.iterateEOG(t.nextEOGEdges, startState, ::transfer) }
            ?: run {
                log.warn("Could not compute final state for function {} (due to timeout)", t.name)
                return@acceptWithIterateEOG
            }

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
                node.refersTo = node.language.bestViableReferenceCandidate(node)
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

/**
 * The state-transfer function for the [SymbolResolver]. It is called for each node in the EOG and
 * is responsible for updating the state based on the node type.
 */
suspend fun SymbolResolver.transfer(
    lattice: Lattice<DeclarationStateElement>,
    currentEdge: EvaluationOrder,
    state: DeclarationStateElement,
): DeclarationStateElement {
    val lattice = lattice as? DeclarationState ?: return state
    val node = currentEdge.end
    return when (node) {
        is Declaration -> handleDeclaration(lattice, state, node)
        is Reference -> handleReference(lattice, node, state)
        is BinaryOperator -> handleBinaryOperator(lattice, node, state)
        is UnaryOperator -> handleUnaryOperator(lattice, node, state)
        else -> state
    }
}

/**
 * Handles a [BinaryOperator] and updates the [state] based on the operator type and the types of
 * the [BinaryOperator.lhs] and [BinaryOperator.rhs].
 */
private fun SymbolResolver.handleBinaryOperator(
    lattice: DeclarationState,
    binOp: BinaryOperator,
    state: DeclarationStateElement,
): DeclarationStateElement {
    val lhsType = state.types[binOp.lhs]?.commonType ?: unknownType()
    val rhsType = state.types[binOp.rhs]?.commonType ?: unknownType()

    val type = binOp.language.propagateTypeOfBinaryOperation(binOp.operatorCode, lhsType, rhsType)

    return state.pushType(lattice, binOp, type)
}

/**
 * Handles a [UnaryOperator] and updates the state based on the operator type and the type of the
 * [UnaryOperator.input].
 */
private fun SymbolResolver.handleUnaryOperator(
    lattice: DeclarationState,
    op: UnaryOperator,
    state: DeclarationStateElement,
): DeclarationStateElement {
    val inputTypeElement = state.types[op.input]
    return if (inputTypeElement != null) {
        state.pushType(
            lattice,
            op,
            *inputTypeElement
                .map { inputType ->
                    op.language.propagateTypeOfUnaryOperation(op.operatorCode, inputType)
                }
                .toTypedArray(),
        )
    } else {
        state
    }
}

/**
 * Handles a [Reference] and updates the state based on the resolution of the symbol used in the
 * reference.
 *
 * We need to differentiate between a simple reference and a member expression.
 * - In the case of a member expression, we need to extract the base type(s) from the
 *   [DeclarationStateElement.types] and lookup the symbol in the scopes of the base type(s).
 * - In the case of a simple reference, we can look up the symbol directly in the scope of the
 *   reference.
 *
 * In both cases, the symbols are taken from [DeclarationStateElement.symbols] and the symbol
 * candidates are pushed to the [DeclarationStateElement.candidates].
 */
private fun SymbolResolver.handleReference(
    lattice: DeclarationState,
    node: Reference,
    state: DeclarationStateElement,
): DeclarationStateElement {
    infoWithFileLocation(node, log, "Resolving reference. {} scopes are active", state.symbols.size)
    var state = state
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

    // Push candidates to state
    state = state.pushCandidate(lattice, node, *candidates.toTypedArray())

    // Push the type information
    state =
        state.pushType(
            lattice,
            node,
            *candidates.mapNotNull { state.types[it]?.toSet() }.flatten().toTypedArray(),
        )

    return state
}

/**
 * Handles a [Declaration] and updates the state, so the declaration is pushed to the visible
 * [DeclarationStateElement.symbols].
 *
 * The [DeclarationStateElement.symbols] element is a mapping of scopes to declarations.
 */
private fun SymbolResolver.handleDeclaration(
    lattice: DeclarationState,
    state: DeclarationStateElement,
    node: Declaration,
): DeclarationStateElement {
    var state = state

    // Push declaration into scope
    state = node.scope?.let { state.pushDeclarationToScope(lattice, it, node) } ?: state

    return state
}

/**
 * Resolves the symbol in the given scope and returns the set of declarations that match the symbol.
 */
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
