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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.GraphExamples
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.test.*
import kotlin.test.*

internal class SubgraphWalkerTest : BaseTest() {
    @Test
    fun testLoopDetection() {
        // Let's create an intentional loop
        val tu = TranslationUnitDeclaration()
        val name = NamespaceDeclaration()
        val func = FunctionDeclaration()
        name.addDeclaration(tu)
        name.addDeclaration(func)
        tu.addDeclaration(name)

        val flat = SubgraphWalker.flattenAST(tu)

        assertEquals(listOf<Node>(tu, name, func), flat)
    }

    @Test
    @Throws(Exception::class)
    fun testASTChildrenGetter() {
        val tu =
            GraphExamples.getVisitorTest(
                    TranslationConfiguration.builder()
                        .loadIncludes(true)
                        .disableCleanup()
                        .debugParser(true)
                        .failOnError(true)
                        .useParallelFrontends(true)
                        .registerLanguage<TestLanguage>()
                        .defaultPasses()
                        .build()
                )
                .components
                .first()
                .translationUnits
                .first()
        val namespace = tu.namespaces["compiling"]
        assertNotNull(namespace)

        val recordDeclaration = namespace.records["compiling.SimpleClass"]
        assertNotNull(recordDeclaration)

        // This calls SubgraphWalker.getAstChildren()
        val ast = recordDeclaration.astChildren
        assertFalse(ast.isEmpty())

        // should contain 3 AST nodes, 1 field, 1 method, 1 constructor
        assertEquals(3, ast.size)
    }
}
