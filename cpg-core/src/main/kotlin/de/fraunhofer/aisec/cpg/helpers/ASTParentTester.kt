/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import org.slf4j.LoggerFactory

object ASTParentTester {
    private val LOGGER = LoggerFactory.getLogger(SubgraphWalker::class.java)

    /**
     * This helper functions verifies that every "forward" AST edge has exactly one reverse edge to
     * the AST "parent".
     */
    fun checkASTParents(result: TranslationResult): Boolean {
        var allParentsGood = true
        val flatAST = SubgraphWalker.flattenAST(result)
        for (parent in flatAST) {
            when (parent) {
                is TranslationResult,
                is Component,
                is TranslationUnitDeclaration -> continue // ignore
                else -> {}
            }
            for (child in parent.astChildren) {
                if (parent !== child.astParent) {
                    allParentsGood = false
                    LOGGER.error("AST parent mismatch for nodes child $child and parent $parent")
                }
            }
        }
        return allParentsGood
    }
}
