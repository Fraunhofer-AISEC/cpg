/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLast
import de.fraunhofer.aisec.cpg.processing.Visitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

@ExecuteLast
class FilenameMapper(ctx: TranslationContext) : TranslationUnitPass(ctx) {
    override fun accept(tu: TranslationUnitDeclaration) {
        val file = tu.name.toString()
        tu.file = file
        handle(tu, file)
    }

    protected fun handle(node: AstNode, file: String) {
        // Using a visitor to avoid loops in the AST
        node.accept(
            Strategy::AST_FORWARD,
            object : Visitor<AstNode>() {
                override fun visit(t: AstNode) {
                    t.file = file
                }
            },
        )
    }

    override fun cleanup() {
        // nothing to do
    }
}
