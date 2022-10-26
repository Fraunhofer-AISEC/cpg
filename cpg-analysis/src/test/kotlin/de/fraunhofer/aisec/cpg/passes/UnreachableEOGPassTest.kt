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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.WhileStatement
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnreachableEOGPassTest {
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
                    .registerPass(UnreachableEOGPass())
            }
    }

    @Test
    fun testIfBothPossible() {
        val method = tu.functions["ifBothPossible"]
        assertNotNull(method)

        val ifStatement = method.bodyOrNull<IfStatement>()
        assertNotNull(ifStatement)

        for (edge in ifStatement.nextEOGEdges) {
            assertFalse(edge.getProperty(Properties.UNREACHABLE) as Boolean)
        }
    }

    @Test
    fun testIfTrue() {
        val method = tu.functions["ifTrue"]
        assertNotNull(method)

        val ifStatement = method.bodyOrNull<IfStatement>()
        assertNotNull(ifStatement)

        assertFalse(ifStatement.nextEOGEdges[0].getProperty(Properties.UNREACHABLE) as Boolean)
        assertTrue(ifStatement.nextEOGEdges[1].getProperty(Properties.UNREACHABLE) as Boolean)
    }

    @Test
    fun testIfFalse() {
        val method = tu.functions["ifFalse"]
        assertNotNull(method)

        val ifStatement = method.bodyOrNull<IfStatement>()
        assertNotNull(ifStatement)

        assertFalse(ifStatement.nextEOGEdges[1].getProperty(Properties.UNREACHABLE) as Boolean)
        assertTrue(ifStatement.nextEOGEdges[0].getProperty(Properties.UNREACHABLE) as Boolean)
    }

    @Test
    fun testIfTrueComputed() {
        val method = tu.functions["ifTrueComputed"]
        assertNotNull(method)

        val ifStatement = method.bodyOrNull<IfStatement>()
        assertNotNull(ifStatement)

        assertFalse(ifStatement.nextEOGEdges[0].getProperty(Properties.UNREACHABLE) as Boolean)
        assertTrue(ifStatement.nextEOGEdges[1].getProperty(Properties.UNREACHABLE) as Boolean)
    }

    @Test
    fun testIfFalseComputed() {
        val method = tu.functions["ifFalseComputed"]
        assertNotNull(method)

        val ifStatement = method.bodyOrNull<IfStatement>()
        assertNotNull(ifStatement)

        assertFalse(ifStatement.nextEOGEdges[1].getProperty(Properties.UNREACHABLE) as Boolean)
        assertTrue(ifStatement.nextEOGEdges[0].getProperty(Properties.UNREACHABLE) as Boolean)
    }

    @Test
    fun testWhileTrueEndless() {
        val method = tu.functions["whileTrueEndless"]
        assertNotNull(method)

        val whileStatement = method.bodyOrNull<WhileStatement>()
        assertNotNull(whileStatement)

        assertFalse(whileStatement.nextEOGEdges[0].getProperty(Properties.UNREACHABLE) as Boolean)
        assertTrue(whileStatement.nextEOGEdges[1].getProperty(Properties.UNREACHABLE) as Boolean)
    }

    @Test
    fun testWhileTrue() {
        val method = tu.functions["whileTrue"]
        assertNotNull(method)

        val whileStatement = method.bodyOrNull<WhileStatement>()
        assertNotNull(whileStatement)

        assertFalse(whileStatement.nextEOGEdges[0].getProperty(Properties.UNREACHABLE) as Boolean)
        assertFalse(whileStatement.nextEOGEdges[1].getProperty(Properties.UNREACHABLE) as Boolean)
    }

    @Test
    fun testWhileComputedTrue() {
        val method = tu.functions["whileComputedTrue"]
        assertNotNull(method)

        val whileStatement = method.bodyOrNull<WhileStatement>()
        assertNotNull(whileStatement)

        assertFalse(whileStatement.nextEOGEdges[0].getProperty(Properties.UNREACHABLE) as Boolean)
        assertTrue(whileStatement.nextEOGEdges[1].getProperty(Properties.UNREACHABLE) as Boolean)
    }

    @Test
    fun testWhileFalse() {
        val method = tu.functions["whileFalse"]
        assertNotNull(method)

        val whileStatement = method.bodyOrNull<WhileStatement>()
        assertNotNull(whileStatement)

        assertFalse(whileStatement.nextEOGEdges[1].getProperty(Properties.UNREACHABLE) as Boolean)
        assertTrue(whileStatement.nextEOGEdges[0].getProperty(Properties.UNREACHABLE) as Boolean)
    }

    @Test
    fun testWhileComputedFalse() {
        val method = tu.functions["whileComputedFalse"]
        assertNotNull(method)

        val whileStatement = method.bodyOrNull<WhileStatement>()
        assertNotNull(whileStatement)

        assertFalse(whileStatement.nextEOGEdges[1].getProperty(Properties.UNREACHABLE) as Boolean)
        assertTrue(whileStatement.nextEOGEdges[0].getProperty(Properties.UNREACHABLE) as Boolean)
    }

    @Test
    fun testWhileUnknown() {
        val method = tu.functions["whileUnknown"]
        assertNotNull(method)

        val whileStatement = method.bodyOrNull<WhileStatement>()
        assertNotNull(whileStatement)

        assertFalse(whileStatement.nextEOGEdges[1].getProperty(Properties.UNREACHABLE) as Boolean)
        assertFalse(whileStatement.nextEOGEdges[0].getProperty(Properties.UNREACHABLE) as Boolean)
    }
}
