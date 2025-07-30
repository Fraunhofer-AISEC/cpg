/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/** Pass with some graph transformations useful when doing serialization. */
@ExecuteLate
class PrepareSerialization(ctx: TranslationContext) : TranslationUnitPass(ctx) {
    private val nodeNameField =
        Node::class
            .memberProperties
            .first { it.name == "name" }
            .javaField
            .also { it?.isAccessible = true }

    override fun cleanup() {
        // nothing to do
    }

    override fun accept(tr: TranslationUnitDeclaration) {
        tr.allChildren<AstNode>().map { node ->
            // Add explicit AST edge
            node.astChildren = SubgraphWalker.getAstChildren(node)
            // CallExpression overwrites name property and must be copied to JvmField
            // to be visible by Neo4jOGM
            if (node is CallExpression) nodeNameField?.set(node, node.name)
        }
    }
}
