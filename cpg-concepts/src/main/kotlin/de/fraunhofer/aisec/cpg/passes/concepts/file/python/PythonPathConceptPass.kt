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
package de.fraunhofer.aisec.cpg.passes.concepts.file.python

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.python.PythonValueEvaluator
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.getArgumentValueByNameOrPosition
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate
import de.fraunhofer.aisec.cpg.passes.configuration.RequiredFrontend
import de.fraunhofer.aisec.cpg.passes.reconstructedImportName

@ExecuteLate
@RequiredFrontend(PythonLanguageFrontend::class)
@ExecuteBefore(PythonFileConceptPass::class)
class PythonPathConceptPass(ctx: TranslationContext) : ConceptPass(ctx) {
    override fun handleNode(node: Node, tu: TranslationUnitDeclaration) {
        when (node) {
            is CallExpression -> handleCall(node)
        }
    }

    internal fun handleCall(call: CallExpression) {
        when (call.reconstructedImportName.toString()) {
            "os.path.join" -> {
                val firstArgValue =
                    call.getArgumentValueByNameOrPosition<String>(
                        name = "a",
                        position = 0,
                        evaluator = PythonValueEvaluator(),
                    )
                val secondArgValue =
                    call.getArgumentValueByNameOrPosition<String>(
                        name = null,
                        position = 1,
                        evaluator = PythonValueEvaluator(),
                    )
                if (call.arguments.size != 2) {
                    Util.errorWithFileLocation(
                        call,
                        log,
                        "Expected exactly two arguments. More not yet implemented. Ignoring the entire call.",
                    )
                    return
                }
                if (firstArgValue == null || secondArgValue == null) {
                    Util.errorWithFileLocation(
                        call,
                        log,
                        "Couldn't determine the first or seconds arguments value. Ignoring the entire call.",
                    )
                    return
                }
                val combined = firstArgValue + "/" + secondArgValue // TODO hardcoded /
            }
            else -> {}
        }
    }
}
