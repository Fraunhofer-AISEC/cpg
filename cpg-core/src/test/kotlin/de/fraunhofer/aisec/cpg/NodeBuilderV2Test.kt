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

import de.fraunhofer.aisec.cpg.example.ExampleLanguageFrontend
import de.fraunhofer.aisec.cpg.example.RawFileNode
import de.fraunhofer.aisec.cpg.example.RawFunctionNode
import de.fraunhofer.aisec.cpg.example.RawParameterNode
import de.fraunhofer.aisec.cpg.example.RawTypeNode
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.plusAssign
import java.io.File
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

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

    @Test
    fun testFrontend() {
        val file =
            RawFileNode(
                "file.example",
                children =
                    listOf(
                        RawFunctionNode(
                            "main",
                            params = listOf(RawParameterNode("argc", type = RawTypeNode("int")))
                        )
                    )
            )

        val frontend = ExampleLanguageFrontend(rawFileNode = file)
        val node = frontend.parse(File(""))
        assertIs<TranslationUnitDeclaration>(node)

        val main = node.functions["main"]
        assertNotNull(main)
    }
}
