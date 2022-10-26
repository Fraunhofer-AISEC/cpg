/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnreachableEOGandDFGTest {

    private lateinit var tu: TranslationUnitDeclaration

    @BeforeAll
    fun beforeAll() {
        val topLevel = Path.of("src", "test", "resources", "passes", "unreachable")
        TranslationManager.builder().build().analyze()
        tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("Unreachability.java").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                        JavaLanguageFrontend::class.java,
                        JavaLanguageFrontend.JAVA_EXTENSIONS
                    )
                    .registerPass(UnreachableEOGPassBeforeDFG())
            }
    }

    @Test
    fun testIfTrue() {
        val method =
            tu.getDeclarationsByName("TestClass", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "ifTrue" } as FunctionDeclaration?

        assertNotNull(method)

        val ifStatement = (method.body as CompoundStatement).statements[2] as? IfStatement
        assertNotNull(ifStatement)
        val thenY = ifStatement.thenStatement.refs["y"]
        assertNotNull(thenY)

        val argY = method.calls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)
        // There's only one DFG to the y++ in the then branch.
        assertEquals(setOf<Node>(thenY), argY.prevDFG)
    }

    @Test
    fun testIfFalse() {
        val method =
            tu.getDeclarationsByName("TestClass", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "ifFalse" } as FunctionDeclaration?

        assertNotNull(method)

        val ifStatement = (method.body as CompoundStatement).statements[2] as? IfStatement
        assertNotNull(ifStatement)
        val elseY = ifStatement.elseStatement.refs["y"]
        assertNotNull(elseY)

        val argY = method.calls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)
        // There's only one DFG to the y++ in the then branch.
        assertEquals(setOf<Node>(elseY), argY.prevDFG)
    }

    @Test
    fun testIfTrueComputed() {
        val method =
            tu.getDeclarationsByName("TestClass", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "ifTrueComputed" } as FunctionDeclaration?

        assertNotNull(method)

        val ifStatement = (method.body as CompoundStatement).statements[2] as? IfStatement
        assertNotNull(ifStatement)
        val thenY = ifStatement.thenStatement.refs["y"]
        assertNotNull(thenY)
        val argY = method.calls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)

        // Still only the y in the then branch
        assertEquals(setOf<Node>(thenY), argY.prevDFG)
    }

    @Test
    fun testIfFalseComputed() {
        val method =
            tu.getDeclarationsByName("TestClass", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "ifFalseComputed" } as FunctionDeclaration?

        assertNotNull(method)

        val ifStatement = (method.body as CompoundStatement).statements[2] as? IfStatement
        assertNotNull(ifStatement)
        val elseY = ifStatement.elseStatement.refs["y"]
        assertNotNull(elseY)
        val argY = method.calls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)

        // Still only the y in the then branch
        assertEquals(setOf<Node>(elseY), argY.prevDFG)
    }

    @Test
    fun testIfTrueComputedHard() {
        val method =
            tu.getDeclarationsByName("TestClass", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "ifTrueComputedHard" } as FunctionDeclaration?

        assertNotNull(method)

        val ifStatement = (method.body as CompoundStatement).statements[3] as? IfStatement
        assertNotNull(ifStatement)
        val thenY = ifStatement.thenStatement.refs["y"]
        assertNotNull(thenY)
        val argY = method.calls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)

        // Still only the y in the then branch
        assertEquals(setOf<Node>(thenY), argY.prevDFG)
    }

    @Test
    fun testIfFalseComputedHard() {
        val method =
            tu.getDeclarationsByName("TestClass", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "ifFalseComputedHard" } as FunctionDeclaration?

        assertNotNull(method)

        val ifStatement = (method.body as CompoundStatement).statements[3] as? IfStatement
        assertNotNull(ifStatement)
        val elseY = ifStatement.elseStatement.refs["y"]
        assertNotNull(elseY)
        val argY = method.calls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)

        // Still only the y in the then branch
        assertEquals(setOf<Node>(elseY), argY.prevDFG)
    }

    @Test
    fun testIfBothPossible() {
        val method =
            tu.getDeclarationsByName("TestClass", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "ifBothPossible" } as FunctionDeclaration?

        assertNotNull(method)

        val ifStatement = (method.body as CompoundStatement).statements[2] as? IfStatement
        assertNotNull(ifStatement)
        val thenY = ifStatement.thenStatement.refs["y"]
        assertNotNull(thenY)
        val elseY = ifStatement.elseStatement.refs["y"]
        assertNotNull(elseY)
        val argY = method.calls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)

        // Still only the y in the then branch
        assertEquals(setOf<Node>(thenY, elseY), argY.prevDFG)
    }
}
