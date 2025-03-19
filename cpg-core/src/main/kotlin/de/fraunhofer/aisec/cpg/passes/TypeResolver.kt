/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.types.DeclaresType
import de.fraunhofer.aisec.cpg.graph.types.HasSecondaryTypeEdge
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.recordDeclaration
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.inference.tryRecordInference
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import kotlin.collections.plusAssign

/**
 * The purpose of this [Pass] is to establish a relationship between [Type] nodes (more specifically
 * [ObjectType]s) and their [RecordDeclaration].
 */
@DependsOn(ImportResolver::class)
open class TypeResolver(ctx: TranslationContext) : ComponentPass(ctx) {

    lateinit var walker: SubgraphWalker.ScopedWalker

    override fun accept(component: Component) {
        ctx.currentComponent = component
        walker = SubgraphWalker.ScopedWalker(scopeManager, strategy = Strategy::AST_FORWARD)
        walker.registerHandler { handleNode(it) }
        walker.iterate(component)
    }

    /**
     * This function is called for each [Node] in the component. It checks if the node has a type or
     * declares a type. If so, it tries to resolve the type using [resolveType]. It also checks for
     * secondary type edges (see [HasSecondaryTypeEdge] and resolves them as well.
     *
     * @param node The node to handle.
     */
    private fun handleNode(node: Node) {
        if (node is HasType) {
            var type = node.type.root
            handleType(type)
            node.assignedTypes.forEach { handleType(it.root) }
        } else if (node is DeclaresType) {
            handleType(node.declaredType)
        }

        if (node is HasSecondaryTypeEdge) {
            node.secondaryTypes.forEach { handleType(it) }
        }
    }

    /**
     * This function is called for each [Type] in the component. It checks if the type is
     * unresolved. If so, it tries to resolve the type using [resolveType]. It also checks for
     * secondary type edges (see [HasSecondaryTypeEdge] and resolves them as well.
     *
     * @param type The type to handle.
     */
    private fun handleType(type: Type) {
        if (
            type is ObjectType && type.typeOrigin == Type.Origin.UNRESOLVED ||
                type.typeOrigin == Type.Origin.GUESSED
        ) {
            resolveType(type)
        }

        if (type is HasSecondaryTypeEdge) {
            type.secondaryTypes.filter { it.root != type }.forEach { handleType(it.root) }
        }
    }

    /**
     * This function tries to "resolve" a [Type] back to the original declaration that declared it
     * (see [DeclaresType]). More specifically, it harmonises the type's name to the FQN of the
     * declared type and sets the [Type.declaredFrom] (and [ObjectType.recordDeclaration]) property.
     * It also sets [Type.typeOrigin] to [Type.Origin.RESOLVED] to mark it as resolved.
     *
     * The high-level approach looks like the following:
     * - First, we check if this type refers to a typedef (see [ScopeManager.typedefFor]). If yes,
     *   we need to make sure that the target type is resolved and then resolve the type to the
     *   target type's declaration.
     * - If no typedef is used, [ScopeManager.lookupSymbolByName] is used to look up declarations by
     *   the type's name, starting at its [Type.scope]. Depending on the type, this can be
     *   unqualified or qualified. We filter exclusively for declarations that implement
     *   [DeclaresType].
     * - If this yields no declaration, we try to infer a record declaration using
     *   [tryRecordInference].
     * - Finally, we set the type's name to the resolved type, set [Type.declaredFrom],
     *   [ObjectType.recordDeclaration], sync [Type.superTypes] with the declaration and set
     *   [Type.typeOrigin] to [Type.Origin.RESOLVED].
     */
    fun resolveType(type: Type): Boolean {
        // Because we still have multiple "global scopes" (one per parallel context), we need to
        // make sure they all point to the final global scope
        type.updateGlobalScope()

        // Check for a possible typedef
        var target = scopeManager.typedefFor(type.name, type.scope)
        if (target != null) {
            if (target.typeOrigin == Type.Origin.UNRESOLVED && type != target) {
                // Make sure our typedef target is resolved
                resolveType(target)
            }

            var originDeclares = target.recordDeclaration
            var name = target.name
            log.trace("Aliasing type {} in {} scope to {}", type.name, type.scope, name)
            type.declaredFrom = originDeclares
            type.recordDeclaration = originDeclares
            type.typeOrigin = Type.Origin.RESOLVED
            typeManager.resolvedTypes += type

            return true
        }

        // Let's start by looking up the type according to their name and scope. We exclusively
        // filter for nodes that implement DeclaresType, because otherwise we will get a lot of
        // constructor declarations and such with the same name. It seems this is ok since most
        // languages will prefer structs/classes over functions when resolving types.
        var declares = scopeManager.lookupTypeSymbolByName(type.name, type.language, type.scope)

        // If we did not find any declaration, we can try to infer a record declaration for it
        if (declares == null) {
            declares = tryRecordInference(type, source = type)
        }

        // If we found the "real" declared type, we can normalize the name of our scoped type
        // and set the name to the declared type.
        if (declares != null) {
            var declaredType = declares.declaredType
            log.trace(
                "Resolving type {} in {} scope to {}",
                type.name,
                type.scope,
                declaredType.name,
            )
            type.name = declaredType.name
            type.refreshNames()
            type.declaredFrom = declares
            type.recordDeclaration = declares as? RecordDeclaration
            type.typeOrigin = Type.Origin.RESOLVED
            typeManager.resolvedTypes += type

            if (declaredType.superTypes.contains(type))
                log.warn(
                    "Removing type {} from the list of its own supertypes. This would create a type cycle that is not allowed.",
                    type,
                )
            type.superTypes.addAll(
                declaredType.superTypes.filter {
                    if (it == this) {
                        log.warn(
                            "Removing type {} from the list of its own supertypes. This would create a type cycle that is not allowed.",
                            this,
                        )
                        false
                    } else {
                        true
                    }
                }
            )

            return true
        }

        return false
    }

    override fun cleanup() {
        // Nothing to do
    }
}

/**
 * This helper function sets the [Type.scope] to the current [ScopeManager.globalScope] if it has a
 * [GlobalScope]. This is necessary because the parallel parsing introduces multiple global scopes.
 */
context(ContextProvider)
private fun Type.updateGlobalScope() {
    if (scope is GlobalScope) {
        scope = ctx.scopeManager.globalScope
        secondOrderTypes.forEach { it.updateGlobalScope() }
    }
}
