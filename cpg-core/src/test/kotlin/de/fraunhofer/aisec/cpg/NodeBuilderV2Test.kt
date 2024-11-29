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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ProblemDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import kotlin.test.Test
import kotlin.test.assertNotNull

class RawFileDeclarationNode(var children: List<RawDeclarationNode>) : RawDeclarationNode()

class RawFunctionDeclarationNode : RawDeclarationNode()

open class RawDeclarationNode : RawNode()

open class RawNode

private class TestDeclarationHandler(frontend: TestLanguageFrontend) :
    Handler<Declaration, RawDeclarationNode, TestLanguageFrontend>(::ProblemDeclaration, frontend) {
    override fun handle(node: RawDeclarationNode): Declaration {
        when (node) {
            is RawFunctionDeclarationNode -> handleFunction(node)
            is RawFileDeclarationNode -> handleFile(node)
        }

        return ProblemDeclaration()
    }

    private fun handleFile(node: RawFileDeclarationNode): TranslationUnitDeclaration {
        return translationUnitDeclaration("test.file").globalScope {
            for (child in node.children) {
                this += handle(child)
            }
        }
    }

    private fun handleFunction(node: RawFunctionDeclarationNode): FunctionDeclaration {
        return functionDeclaration("main", rawNode = node).withScope {
            val param = parameterDeclaration("argc", objectType("int"))

            this += param
        }
    }
}

class NodeBuilderV2Test {
    @Test
    fun testBuild() {
        val frontend = TestLanguageFrontend()
        val tu =
            with(frontend) {
                translationUnitDeclaration("test.file").globalScope {
                    val func =
                        functionDeclaration("main").withScope {
                            val param = parameterDeclaration("argc", objectType("int"))

                            this += param
                        }

                    this += func
                }
            }

        assertNotNull(tu)

        val func = tu.functions["main"]
        assertNotNull(func)

        val param = func.parameters["argc"]
        assertNotNull(param)
    }
}
