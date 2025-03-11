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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.GraphExamples
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull

class MermaidPrinterTest {
    @Test
    fun testPrintDFG() {
        val graph = GraphExamples.getDataflowClass()
        val sc = graph.functions["main"].variables["sc"]
        assertNotNull(sc)

        val p = sc.printDFG()
        println(p)
        assertContains(p, "DFG")
    }

    @Test
    fun testPrintEOG() {
        val graph = GraphExamples.getDataflowClass()
        val sc = graph.functions["main"].variables["sc"]
        assertNotNull(sc)

        val p = sc.printEOG()
        println(p)
        assertContains(p, "EOG")
    }

    @Test
    fun testPrintAST() {
        with(TestLanguageFrontend()) {
            val ref = newReference("foo")
            val call = newCallExpression(ref)
            val lit = newLiteral(1337).also { it.name = Name("1337") }
            call.arguments += lit
            assertNotNull(ref.astParent)

            // We cannot really assert the whole string since the ID of nodes is not static and
            // therefore the hashcode is not static
            val ast = ref.printAST()
            println(ast)
            assertContains(ast, "foo")
            assertContains(ast, "CallExpression")
            assertContains(ast, "Literal")
            assertContains(ast, "1337")
        }
    }
}
