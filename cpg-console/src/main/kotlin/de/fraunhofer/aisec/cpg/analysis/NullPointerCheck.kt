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
package de.fraunhofer.aisec.cpg.analysis

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.HasBase
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation.locationLink
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NullPointerCheck {
    private val log: Logger
        get() = LoggerFactory.getLogger(OutOfBoundsCheck::class.java)

    fun run(result: TranslationResult) {
        for (tu in result.translationUnits) {
            tu.accept(
                Strategy::AST_FORWARD,
                object : IVisitor<Node?>() {
                    fun visit(v: MemberCallExpression) {
                        handleHasBase(v)
                    }

                    fun visit(v: CallExpression) {
                        handleHasBase(v)
                    }

                    fun visit(v: MemberExpression) {
                        handleHasBase(v)
                    }

                    fun visit(v: ArraySubscriptionExpression) {
                        handleHasBase(v)
                    }
                }
            )
        }
    }

    fun handleHasBase(node: HasBase) {
        // check for all incoming DFG branches
        node.base.prevDFG.forEach {
            var resolved: Any? = CouldNotResolve()
            if (it is Expression) {
                // try to resolve them
                resolved = it.resolve()
            } else if (it is Declaration) {
                // try to resolve them
                resolved = it.resolve()
            }

            if (resolved == null) {
                // TODO(oxisto): would be nice to have the complete resolution path
                Util.errorWithFileLocation(
                    node as Node,
                    log,
                    "Null pointer detected in branch. Relevant point that set it was here: ${locationLink(it.location)}"
                )
            }
        }
    }
}
