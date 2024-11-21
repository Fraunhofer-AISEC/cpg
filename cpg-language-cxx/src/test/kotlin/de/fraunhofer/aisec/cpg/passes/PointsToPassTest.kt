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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertLocalName
import java.io.File
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PointsToPassTest {
    companion object {
        private val topLevel = java.nio.file.Path.of("src", "test", "resources")
    }

    @Test
    fun testBasics() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
            }
        assertNotNull(tu)

        // Declarations
        val iDecl =
            tu.allChildren<VariableDeclaration> { it.location?.region?.startLine == 4 }.first()
        val jDecl =
            tu.allChildren<VariableDeclaration> { it.location?.region?.startLine == 5 }.first()
        val aDecl =
            tu.allChildren<VariableDeclaration> { it.location?.region?.startLine == 6 }.first()
        val bDecl =
            tu.allChildren<VariableDeclaration> { it.location?.region?.startLine == 7 }.first()

        // Literals
        val literal0 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 4 }.first()
        val literal1 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 5 }.first()
        val literal2 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 9 }.first()
        val literal3 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 17 }.first()

        // PointerReferences
        val iPointerRef =
            tu.allChildren<PointerReference> { it.location?.region?.startLine == 6 }.first()
        val jPointerRef =
            tu.allChildren<PointerReference> { it.location?.region?.startLine == 15 }.first()

        // PointerDeReferences
        val aPointerDerefLine12 =
            tu.allChildren<PointerDereference> { it.location?.region?.startLine == 12 }.first()
        val aPointerDerefLine14 =
            tu.allChildren<PointerDereference> { it.location?.region?.startLine == 14 }.first()
        val aPointerDerefLine16 =
            tu.allChildren<PointerDereference> { it.location?.region?.startLine == 16 }.first()
        val aPointerDerefLine17 =
            tu.allChildren<PointerDereference> { it.location?.region?.startLine == 17 }.first()
        val bPointerDerefLine18 =
            tu.allChildren<PointerDereference> { it.location?.region?.startLine == 18 }.first()

        // References
        val iRefLine8 = tu.allChildren<Reference> { it.location?.region?.startLine == 8 }.first()
        val iRefLine9 = tu.allChildren<Reference> { it.location?.region?.startLine == 9 }.first()
        val iRefLine10 = tu.allChildren<Reference> { it.location?.region?.startLine == 10 }.first()
        val iRefLine11 = tu.allChildren<Reference> { it.location?.region?.startLine == 11 }.first()
        val aRefLine15 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 15 && it.name.localName == "a"
                }
                .first()

        // UnaryOperators
        val iUO = tu.allChildren<UnaryOperator> { it.location?.region?.startLine == 13 }.first()

        // Line 4
        assertLocalName("i", iDecl.memoryAddress)
        assertEquals(1, iDecl.memoryValue.size)
        assertEquals(literal0, iDecl.memoryValue.first())

        // Line 5
        assertLocalName("j", jDecl.memoryAddress)
        assertEquals(1, jDecl.memoryValue.size)
        assertEquals(literal1, jDecl.memoryValue.first())

        // Line 6
        assertLocalName("a", aDecl.memoryAddress)
        assertEquals(1, aDecl.memoryValue.size)
        assertEquals(aDecl.memoryValue.first(), iDecl.memoryAddress)
        assertTrue(iPointerRef.memoryAddress.isEmpty())
        assertEquals(1, iPointerRef.memoryValue.size)
        assertEquals(iPointerRef.memoryValue.first(), iDecl.memoryAddress)

        // Line 7
        assertLocalName("b", bDecl.memoryAddress)
        assertEquals(1, bDecl.memoryValue.size)
        assertEquals(iDecl.memoryAddress, bDecl.memoryValue.first())

        // Line 8
        assertEquals(1, iRefLine8.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, iRefLine8.memoryAddress.first())
        assertEquals(1, iRefLine8.memoryValue.size)
        assertEquals(literal0, iRefLine8.memoryValue.first())

        // Line 9
        assertEquals(1, iRefLine9.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, iRefLine9.memoryAddress.first())
        assertEquals(1, iRefLine9.memoryValue.size)
        assertEquals(literal2, iRefLine9.memoryValue.filterIsInstance<Literal<*>>().first())

        // Line 10
        assertEquals(1, iRefLine10.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, iRefLine10.memoryAddress.first())
        assertEquals(1, iRefLine10.memoryValue.size)
        assertEquals(literal2, iRefLine10.memoryValue.first())

        // Line 11
        assertEquals(1, iRefLine11.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, iRefLine11.memoryAddress.first())
        assertEquals(1, iRefLine11.memoryValue.size)
        assertTrue(iRefLine11.memoryValue.filterIsInstance<BinaryOperator>().isNotEmpty())

        // Line 12
        assertEquals(1, aPointerDerefLine12.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, aPointerDerefLine12.memoryAddress.first())
        assertEquals(1, aPointerDerefLine12.memoryValue.size)
        assertTrue(aPointerDerefLine12.memoryValue.filterIsInstance<BinaryOperator>().isNotEmpty())

        // Line 13 should only update the DeclarationState, not much here to test
        // Line 14
        assertEquals(1, aPointerDerefLine14.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, aPointerDerefLine14.memoryAddress.first())
        assertEquals(1, aPointerDerefLine14.memoryValue.size)
        assertEquals(iUO, aPointerDerefLine14.memoryValue.first())

        // Line 15
        assertTrue(jPointerRef.memoryAddress.isEmpty())
        assertEquals(1, jPointerRef.memoryValue.size)
        assertEquals(jDecl.memoryAddress, jPointerRef.memoryValue.first())
        assertEquals(1, aRefLine15.memoryAddress.size)
        assertEquals(aDecl.memoryAddress, aRefLine15.memoryAddress.first())
        assertEquals(1, aRefLine15.memoryValue.size)
        assertEquals(jDecl.memoryAddress, aRefLine15.memoryValue.first())

        // Line 16
        assertEquals(1, aPointerDerefLine16.memoryAddress.size)
        assertEquals(jDecl.memoryAddress, aPointerDerefLine16.memoryAddress.first())
        assertEquals(1, aPointerDerefLine16.memoryValue.size)
        assertEquals(literal1, aPointerDerefLine16.memoryValue.first())

        // Line 17
        assertEquals(1, aPointerDerefLine17.memoryAddress.size)
        assertEquals(jDecl.memoryAddress, aPointerDerefLine17.memoryAddress.first())
        assertEquals(1, aPointerDerefLine17.memoryValue.size)
        assertEquals(literal3, aPointerDerefLine17.memoryValue.first())

        // Line 18
        assertEquals(1, bPointerDerefLine18.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, bPointerDerefLine18.memoryAddress.first())
        assertEquals(1, bPointerDerefLine18.memoryValue.size)
        assertEquals(iUO, bPointerDerefLine18.memoryValue.first())
    }

    @Test
    fun testConditions() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
            }
        assertNotNull(tu)

        // Declarations
        val iDecl = tu.allChildren<Declaration> { it.location?.region?.startLine == 22 }.first()
        val jDecl = tu.allChildren<Declaration> { it.location?.region?.startLine == 23 }.first()
        val aDecl = tu.allChildren<Declaration> { it.location?.region?.startLine == 24 }.first()

        // PointerDerefs
        val aPointerDerefLine27 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 27 && it.name.localName == "a"
                }
                .first()
        val aPointerDerefLine30 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 30 && it.name.localName == "a"
                }
                .first()
        val aPointerDerefLine32 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 32 && it.name.localName == "a"
                }
                .first()
        val aPointerDerefLine37 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 37 && it.name.localName == "a"
                }
                .first()

        // UnaryOperator
        val iUO = tu.allChildren<UnaryOperator> { it.location?.region?.startLine == 35 }.first()

        // Line 27
        assertEquals(iDecl.memoryAddress, aPointerDerefLine27.memoryAddress.firstOrNull())
        assertEquals(iDecl.memoryValue, aPointerDerefLine27.memoryValue)

        // Line 30
        assertEquals(jDecl.memoryAddress, aPointerDerefLine30.memoryAddress.firstOrNull())
        assertEquals(jDecl.memoryValue, aPointerDerefLine30.memoryValue)

        // Line 32
        assertEquals(2, aPointerDerefLine32.memoryAddress.size)
        assertTrue(
            aPointerDerefLine32.memoryAddress.containsAll(
                setOf(iDecl.memoryAddress, jDecl.memoryAddress)
            )
        )
        assertEquals(2, aPointerDerefLine32.memoryValue.size)
        assertTrue(
            aPointerDerefLine32.memoryValue.containsAll(
                setOf(iDecl.memoryValue.first(), jDecl.memoryValue.first())
            )
        )

        // Line 37
        assertEquals(2, aPointerDerefLine37.memoryAddress.size)
        assertTrue(
            aPointerDerefLine37.memoryAddress.containsAll(
                setOf(iDecl.memoryAddress, jDecl.memoryAddress)
            )
        )
        assertEquals(3, aPointerDerefLine37.memoryValue.size)
        assertTrue(
            aPointerDerefLine37.memoryValue.containsAll(
                setOf(iDecl.memoryValue.first(), jDecl.memoryValue.first(), iUO)
            )
        )
    }

    @Test
    fun testStructs() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
            }
        assertNotNull(tu)
    }
}
