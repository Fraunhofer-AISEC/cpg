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
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.fqn
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.order.ExecuteBefore

@ExecuteBefore(TypeResolver::class)
open class TypeNormalizer(ctx: TranslationContext) : ComponentPass(ctx) {

    lateinit var walker: SubgraphWalker.ScopedWalker

    override fun accept(component: Component) {
        walker = SubgraphWalker.ScopedWalker(scopeManager)

        walker.registerHandler(::handleNode)
        walker.iterate(component)
    }

    /**
     * Creates the [ObjectType.recordDeclaration] relationship between [ObjectType]s and
     * [RecordDeclaration] with the same [Node.name].
     */
    fun handleNode(record: RecordDeclaration?, parent: Node?, node: Node?) {
        if (node is HasType) {
            handleHasType(node)
        }
    }

    private fun handleHasType(node: HasType) {
        val type = node.type
        if (type !is UnknownType && type is ObjectType && type.recordDeclaration == null) {
            var holderScope = type.scope

            if (!normalizeType(holderScope, type)) {
                // Retry with using prefixes
                var usingScope =
                    scopeManager.firstScopeOrNull(holderScope) { it.using.isNotEmpty() }
                for (prefix in usingScope?.using ?: listOf()) {
                    if (normalizeType(holderScope, type, prefix)) {
                        break
                    }
                }
            }
        }
    }

    private fun normalizeType(holderScope: Scope?, type: Type, prefix: Name? = null): Boolean {
        var holderScope1 = holderScope
        while (holderScope1 != null) {
            val scope = lookupNameScope(prefix.fqn(type.name), holderScope1)
            val name = scope?.name
            if (name != null) {
                log.debug("Normalizing type {} in {} scope to {}", type.name, type.scope, name)
                type.name = name
                return true
            }
            holderScope1 = holderScope1.parent
        }

        return false
    }

    private fun lookupNameScope(name: Name, startScope: Scope): NameScope? {
        val parts = name.parts
        var scope: Scope? = startScope

        for (part in parts) {
            scope = scope?.children?.firstOrNull { it is NameScope && it.name?.localName == part }
            if (scope == null) {
                break
            }
        }

        return scope as? NameScope
    }

    private fun handleRecord(record: RecordDeclaration) {
        for (t in typeManager.firstOrderTypes) {
            if (t.name == record.name && t is ObjectType) {
                // The node is the class of the type t
                t.recordDeclaration = record
            }
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}
