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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.recordDeclaration
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn

/**
 * The purpose of this [Pass] is to establish a relationship between [Type] nodes (more specifically
 * [ObjectType]s) and their [RecordDeclaration].
 */
@DependsOn(ImportResolver::class)
open class TypeResolver(ctx: TranslationContext) : ComponentPass(ctx) {

    lateinit var walker: SubgraphWalker.ScopedWalker

    override fun accept(component: Component) {
        resolveFirstOrderTypes()
        refreshNames()
    }

    private fun refreshNames() {
        for (type in typeManager.secondOrderTypes) {
            type.refreshNames()
        }
    }

    companion object {
        context(ContextProvider)
        fun resolveType(type: Type): Boolean {
            // Let's start by looking up the type according to their name and scope. We exclusively
            // filter for nodes that implement DeclaresType, because otherwise we will get a lot of
            // constructor declarations and such with the same name. It seems this is ok since most
            // languages will prefer structs/classes over functions when resolving types.
            var declares = ctx?.scopeManager?.findTypeDeclaration(type.name, type.scope)

            // Check for a possible typedef
            var target = ctx?.scopeManager?.typedefFor(type.name, type.scope)
            if (target != null) {
                if (
                    target.resolutionState == Type.ResolutionState.UNRESOLVED &&
                        type != target &&
                        target is ObjectType
                ) {
                    // Make sure our typedef target is resolved
                    resolveType(target)
                }

                var originDeclares = target.recordDeclaration
                var name = target.name
                log.debug("Aliasing type {} in {} scope to {}", type.name, type.scope, name)
                type.declaredFrom = originDeclares
                type.recordDeclaration = originDeclares
                ctx?.typeManager?.markAsResolved(type)
                return true
            }

            if (declares == null) {
                declares = ctx?.tryRecordInference(type, locationHint = type)
            }

            // If we found the "real" declared type, we can normalize the name of our scoped type
            // and set the name to the declared type.
            if (declares != null) {
                var declaredType = declares.declaredType
                log.debug(
                    "Resolving type {} in {} scope to {}",
                    type.name,
                    type.scope,
                    declaredType.name
                )
                type.name = declaredType.name
                type.declaredFrom = declares
                type.recordDeclaration = declares as? RecordDeclaration
                type.superTypes.addAll(declaredType.superTypes)
                ctx?.typeManager?.markAsResolved(type)
                return true
            }

            return false
        }
    }

    override fun cleanup() {
        // Nothing to do
    }

    fun resolveFirstOrderTypes() {
        var allTypes = typeManager.firstOrderTypesMap.values.flatten().sortedBy { it.name }

        log.info("Resolving {} first order type objects", allTypes.size)

        for (type in allTypes) {
            if (
                type is ObjectType && type.resolutionState == Type.ResolutionState.UNRESOLVED ||
                    type.resolutionState == Type.ResolutionState.GUESSED
            ) {
                // Try to resolve all UNRESOLVED types. Also try to resolve GUESSED types. GUESSED
                // is not really used anymore, except in the java frontend, which needs a more or
                // less complete type-rewrite. Once that is done, we can remove the GUESSED state.
                resolveType(type)
            } else if (type.resolutionState == Type.ResolutionState.RESOLVED) {
                // This will most likely only affect built-in types. They are resolved by default,
                // and we want to make sure that they end up in the resolve first order list
                ctx.typeManager.markAsResolved(type)
            }
        }
    }
}
