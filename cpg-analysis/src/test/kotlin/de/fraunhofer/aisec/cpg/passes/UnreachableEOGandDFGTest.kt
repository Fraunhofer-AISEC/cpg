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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.testcases.Passes
import kotlin.test.Ignore
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
        tu = Passes.getUnreachability().components.first().translationUnits.first()
    }

    @Test
    @Ignore
    fun testIfTrue() {
        val method = tu.allFunctions["ifTrue"]
        assertNotNull(method)

        val ifStatement = method.bodyOrNull<IfStatement>()
        assertNotNull(ifStatement)
        val thenY = ifStatement.thenStatement.allRefs["y"]
        assertNotNull(thenY)

        val argY = method.allCalls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)
        // There's only one DFG to the y++ in the then branch.
        assertEquals(setOf<Node>(thenY), argY.prevDFG)
    }

    @Test
    @Ignore
    fun testIfFalse() {
        val method = tu.allFunctions["ifFalse"]
        assertNotNull(method)

        val ifStatement = method.bodyOrNull<IfStatement>()
        assertNotNull(ifStatement)
        val elseY = ifStatement.elseStatement.allRefs["y"]
        assertNotNull(elseY)

        val argY = method.allCalls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)
        // There's only one DFG to the y++ in the then branch.
        assertEquals(setOf<Node>(elseY), argY.prevDFG)
    }

    @Test
    @Ignore
    fun testIfTrueComputed() {
        val method = tu.allFunctions["ifTrueComputed"]
        assertNotNull(method)

        val ifStatement = method.bodyOrNull<IfStatement>()
        assertNotNull(ifStatement)
        val thenY = ifStatement.thenStatement.allRefs["y"]
        assertNotNull(thenY)
        val argY = method.allCalls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)

        // Still only the y in the then branch
        assertEquals(setOf<Node>(thenY), argY.prevDFG)
    }

    @Test
    @Ignore
    fun testIfFalseComputed() {
        val method = tu.allFunctions["ifFalseComputed"]
        assertNotNull(method)

        val ifStatement = method.bodyOrNull<IfStatement>()
        assertNotNull(ifStatement)
        val elseY = ifStatement.elseStatement.allRefs["y"]
        assertNotNull(elseY)
        val argY = method.allCalls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)

        // Still only the y in the then branch
        assertEquals(setOf<Node>(elseY), argY.prevDFG)
    }

    @Test
    @Ignore
    fun testIfTrueComputedHard() {
        val method = tu.allFunctions["ifTrueComputedHard"]
        assertNotNull(method)

        val ifStatement = method.bodyOrNull<IfStatement>()
        assertNotNull(ifStatement)
        val thenY = ifStatement.thenStatement.allRefs["y"]
        assertNotNull(thenY)
        val argY = method.allCalls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)

        // Still only the y in the then branch
        assertEquals(setOf<Node>(thenY), argY.prevDFG)
    }

    @Test
    @Ignore
    fun testIfFalseComputedHard() {
        val method = tu.allFunctions["ifFalseComputedHard"]
        assertNotNull(method)

        val ifStatement = method.bodyOrNull<IfStatement>()
        assertNotNull(ifStatement)
        val elseY = ifStatement.elseStatement.allRefs["y"]
        assertNotNull(elseY)
        val argY = method.allCalls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)

        // Still only the y in the then branch
        assertEquals(setOf<Node>(elseY), argY.prevDFG)
    }

    @Test
    @Ignore
    fun testIfBothPossible() {
        val method = tu.allFunctions["ifBothPossible"]
        assertNotNull(method)

        val ifStatement = method.bodyOrNull<IfStatement>()
        assertNotNull(ifStatement)
        val thenY = ifStatement.thenStatement.allRefs["y"]
        assertNotNull(thenY)
        val elseY = ifStatement.elseStatement.allRefs["y"]
        assertNotNull(elseY)
        val argY = method.allCalls["println"]?.arguments?.firstOrNull()
        assertNotNull(argY)

        // Still only the y in the then branch
        assertEquals(setOf<Node>(thenY, elseY), argY.prevDFG)
    }
}
