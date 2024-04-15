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
import de.fraunhofer.aisec.cpg.graph.types.DeclaresType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
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

        walker = SubgraphWalker.ScopedWalker(scopeManager)
        walker.registerHandler(::handleNode)
        walker.iterate(component)
    }

    private fun refreshNames() {
        for (type in typeManager.secondOrderTypes) {
            type.refreshNames()
        }
    }

    private fun resolveType(type: Type): Boolean {
        // Let's start by looking up the type according to their name and scope. We exclusively
        // filter for nodes that implement DeclaresType, because otherwise we will get a lot of
        // constructor declarations and such with the same name. It seems this is ok since most
        // languages will prefer structs/classes over functions when resolving types.
        var symbols =
            scopeManager.findSymbols(type.name, startScope = type.scope) { it is DeclaresType }

        // We need to have a single match, otherwise we have an ambiguous type and we cannot
        // normalize it.
        // TODO: Maybe we should have a warning in this case?
        var declares = symbols.filterIsInstance<DeclaresType>().singleOrNull()
        var declaredType = declares?.declaredType

        // If we found the "real" declared type, we can normalize the name of our scoped type and
        // set the name to the declared type.
        if (declaredType != null) {
            var name = declaredType.name
            log.debug("Resolving type {} in {} scope to {}", type.name, type.scope, name)
            type.name = name
            type.declaredFrom = declares
            return true
        }

        return false
    }

    private fun handleNode(node: Node?) {
        if (node is RecordDeclaration) {
            for (t in typeManager.firstOrderTypes) {
                if (t.name == node.name && t is ObjectType) {
                    // The node is the class of the type t
                    t.recordDeclaration = node
                }
            }
        }
    }

    override fun cleanup() {
        // Nothing to do
    }

    fun resolveFirstOrderTypes() {
        for (type in typeManager.firstOrderTypes) {
            if (type is ObjectType) {
                resolveType(type)
            }
        }
    }
}