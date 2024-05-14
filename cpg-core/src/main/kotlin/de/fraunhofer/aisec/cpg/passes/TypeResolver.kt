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
import de.fraunhofer.aisec.cpg.graph.DeclaresType
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

/**
 * The purpose of this [Pass] is to establish a relationship between [Type] nodes (more specifically
 * [ObjectType]s) and their [RecordDeclaration].
 */
open class TypeResolver(ctx: TranslationContext) : ComponentPass(ctx) {
    override fun accept(component: Component) {

        // Resolve all references
        for (ref in typeManager.references) {
            resolveTypeReference(ref)
        }

        component.accept(
            Strategy::AST_FORWARD,
            object : IVisitor<Node>() {
                /**
                 * Creates the [ObjectType.recordDeclaration] relationship between [ObjectType]s and
                 * [RecordDeclaration] with the same [Node.name].
                 */
                fun visit(record: RecordDeclaration) {
                    for (t in typeManager.firstOrderTypes) {
                        if (t.name == record.name && t is ObjectType) {
                            // The node is the class of the type t
                            t.recordDeclaration = record
                        }
                    }
                }
            }
        )
    }

    private fun resolveTypeReference(type: TypeReference) {
        // TODO(oxisto): merge this somehow with resolveReference
        // Find a list of candidate symbols. Currently, this is only used the in the "next-gen" call
        // resolution, but in future this will also be used in resolving regular references.
        type.candidates = scopeManager.findSymbols(type.name, startScope = type.scope).toSet()

        type.refersTo = type.candidates.filterIsInstance<DeclaresType>().singleOrNull()
    }

    override fun cleanup() {
        // Nothing to do
    }
}
