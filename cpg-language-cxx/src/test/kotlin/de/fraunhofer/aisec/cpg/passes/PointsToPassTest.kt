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
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertLocalName
import java.io.File
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
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
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
        assertEquals(1, iDecl.prevDFG.size)
        assertEquals(literal0, iDecl.prevDFG.first())

        // Line 5
        assertLocalName("j", jDecl.memoryAddress)
        assertEquals(1, jDecl.prevDFG.size)
        assertEquals(literal1, jDecl.prevDFG.first())

        // Line 6
        assertLocalName("a", aDecl.memoryAddress)
        assertEquals(1, aDecl.prevDFG.size)
        assertEquals(aDecl.prevDFG.first(), iDecl.memoryAddress)
        assertTrue(iPointerRef.memoryAddress.isEmpty())
        assertEquals(1, iPointerRef.prevDFG.size)
        assertEquals(iPointerRef.prevDFG.first(), iDecl.memoryAddress)

        // Line 7
        assertLocalName("b", bDecl.memoryAddress)
        assertEquals(1, bDecl.prevDFG.size)
        assertEquals(iDecl.memoryAddress, bDecl.prevDFG.first())

        // Line 8
        assertEquals(1, iRefLine8.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, iRefLine8.memoryAddress.first())
        assertEquals(1, iRefLine8.prevDFG.size)
        assertEquals(literal0, iRefLine8.prevDFG.first())

        // Line 9
        assertEquals(1, iRefLine9.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, iRefLine9.memoryAddress.first())
        assertEquals(1, iRefLine9.prevDFG.size)
        assertEquals(literal2, iRefLine9.prevDFG.filterIsInstance<Literal<*>>().first())

        // Line 10
        assertEquals(1, iRefLine10.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, iRefLine10.memoryAddress.first())
        assertEquals(1, iRefLine10.prevDFG.size)
        assertEquals(literal2, iRefLine10.prevDFG.first())

        // Line 11
        assertEquals(1, iRefLine11.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, iRefLine11.memoryAddress.first())
        assertEquals(1, iRefLine11.prevDFG.size)
        assertTrue(iRefLine11.prevDFG.filterIsInstance<BinaryOperator>().isNotEmpty())

        // Line 12
        assertEquals(1, aPointerDerefLine12.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, aPointerDerefLine12.memoryAddress.first())
        assertEquals(1, aPointerDerefLine12.prevDFG.size)
        assertTrue(aPointerDerefLine12.prevDFG.filterIsInstance<BinaryOperator>().isNotEmpty())

        // Line 13 should only update the DeclarationState, not much here to test
        // Line 14
        assertEquals(1, aPointerDerefLine14.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, aPointerDerefLine14.memoryAddress.first())
        assertEquals(1, aPointerDerefLine14.prevDFG.size)
        assertEquals(iUO, aPointerDerefLine14.prevDFG.first())

        // Line 15
        assertTrue(jPointerRef.memoryAddress.isEmpty())
        assertEquals(1, jPointerRef.prevDFG.size)
        assertEquals(jDecl.memoryAddress, jPointerRef.prevDFG.first())
        assertEquals(1, aRefLine15.memoryAddress.size)
        assertEquals(aDecl.memoryAddress, aRefLine15.memoryAddress.first())
        assertEquals(1, aRefLine15.prevDFG.size)
        assertEquals(jDecl.memoryAddress, aRefLine15.prevDFG.first())

        // Line 16
        assertEquals(1, aPointerDerefLine16.memoryAddress.size)
        assertEquals(jDecl.memoryAddress, aPointerDerefLine16.memoryAddress.first())
        assertEquals(1, aPointerDerefLine16.prevDFG.size)
        assertEquals(literal1, aPointerDerefLine16.prevDFG.first())

        // Line 17
        assertEquals(1, aPointerDerefLine17.memoryAddress.size)
        assertEquals(jDecl.memoryAddress, aPointerDerefLine17.memoryAddress.first())
        assertEquals(1, aPointerDerefLine17.prevDFG.size)
        assertEquals(literal3, aPointerDerefLine17.prevDFG.first())

        // Line 18
        assertEquals(1, bPointerDerefLine18.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, bPointerDerefLine18.memoryAddress.first())
        assertEquals(1, bPointerDerefLine18.prevDFG.size)
        assertEquals(iUO, bPointerDerefLine18.prevDFG.first())
    }

    @Test
    fun testConditions() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
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
        assertEquals(1, aPointerDerefLine27.prevDFG.size)
        assertEquals(iDecl.prevDFG.first(), aPointerDerefLine27.prevDFG.first())

        // Line 30
        assertEquals(jDecl.memoryAddress, aPointerDerefLine30.memoryAddress.firstOrNull())
        assertEquals(1, aPointerDerefLine30.prevDFG.size)
        assertEquals(jDecl.prevDFG.first(), aPointerDerefLine30.prevDFG.first())

        // Line 32
        assertEquals(2, aPointerDerefLine32.memoryAddress.size)
        aPointerDerefLine32.memoryAddress.containsAll(
            setOf(iDecl.memoryAddress, jDecl.memoryAddress)
        )
        assertEquals(2, aPointerDerefLine32.prevDFG.size)
        assertTrue(aPointerDerefLine32.prevDFG.contains(iDecl.prevDFG.first()))
        assertTrue(aPointerDerefLine32.prevDFG.contains(jDecl.prevDFG.first()))

        // Line 37
        assertEquals(2, aPointerDerefLine37.memoryAddress.size)
        assertTrue(
            aPointerDerefLine37.memoryAddress.containsAll(
                setOf(iDecl.memoryAddress, jDecl.memoryAddress)
            )
        )
        assertEquals(3, aPointerDerefLine37.prevDFG.size)
        assertTrue(aPointerDerefLine37.prevDFG.contains(iDecl.prevDFG.first()))
        assertTrue(aPointerDerefLine37.prevDFG.contains(jDecl.prevDFG.first()))
        assertTrue(aPointerDerefLine37.prevDFG.contains(iUO))
    }

    @Test
    fun testStructs() {
        val file = File("src/test/resources/pointsto.cpp")
        // val file = File("/tmp/pointsto.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                // it.registerLanguage<CLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
            }
        assertNotNull(tu)

        // References
        val saLine51 =
            tu.allChildren<MemberExpression> { it.location?.region?.startLine == 51 }.first()
        val sbLine52 =
            tu.allChildren<MemberExpression> { it.location?.region?.startLine == 52 }.first()
        val saLine53 =
            tu.allChildren<MemberExpression> {
                    it.location?.region?.startLine == 53 && it.name.localName == "a"
                }
                .first()
        val sbLine53 =
            tu.allChildren<MemberExpression> {
                    it.location?.region?.startLine == 53 && it.name.localName == "b"
                }
                .first()
        val paLine55 =
            tu.allChildren<MemberExpression> {
                    it.location?.region?.startLine == 55 && it.name.localName == "a"
                }
                .first()
        val pbLine55 =
            tu.allChildren<MemberExpression> {
                    it.location?.region?.startLine == 55 && it.name.localName == "b"
                }
                .first()
        val paLine56 =
            tu.allChildren<MemberExpression> { it.location?.region?.startLine == 56 }.first()
        val pbLine57 =
            tu.allChildren<MemberExpression> { it.location?.region?.startLine == 57 }.first()
        val paLine59 =
            tu.allChildren<MemberExpression> {
                    it.location?.region?.startLine == 59 && it.name.localName == "a"
                }
                .first()
        val pbLine59 =
            tu.allChildren<MemberExpression> {
                    it.location?.region?.startLine == 59 && it.name.localName == "b"
                }
                .first()

        // Literals
        val literal1 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 51 }.first()
        val literal2 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 52 }.first()
        val literal3 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 56 }.first()
        val literal4 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 57 }.first()

        // Line 51
        assertEquals(1, saLine51.memoryAddress.size)
        assertEquals(
            ((saLine51.base as? Reference)?.memoryAddress?.firstOrNull() as? MemoryAddress)
                ?.fieldAddresses
                ?.filter { it.key == saLine51.refersTo?.name?.localName }
                ?.entries
                ?.firstOrNull()
                ?.value
                ?.firstOrNull(),
            saLine51.memoryAddress.firstOrNull()
        )
        assertEquals(1, saLine51.prevDFG.size)
        assertEquals(literal1, saLine51.prevDFG.firstOrNull())

        // Line 52
        assertEquals(1, sbLine52.memoryAddress.size)
        assertEquals(
            ((sbLine52.base as? Reference)?.memoryAddress?.firstOrNull() as? MemoryAddress)
                ?.fieldAddresses
                ?.filter { it.key == sbLine52.refersTo?.name?.localName }
                ?.entries
                ?.firstOrNull()
                ?.value
                ?.firstOrNull(),
            sbLine52.memoryAddress.firstOrNull()
        )
        assertEquals(1, sbLine52.prevDFG.size)
        assertEquals(literal2, sbLine52.prevDFG.firstOrNull())

        // Line 53
        assertEquals(1, saLine53.memoryAddress.size)
        assertEquals(
            ((saLine53.base as? Reference)?.memoryAddress?.firstOrNull() as? MemoryAddress)
                ?.fieldAddresses
                ?.filter { it.key == saLine53.refersTo?.name?.localName }
                ?.entries
                ?.firstOrNull()
                ?.value
                ?.firstOrNull(),
            saLine53.memoryAddress.firstOrNull()
        )
        assertEquals(1, saLine53.prevDFG.size)
        assertEquals(literal1, saLine53.prevDFG.firstOrNull())

        assertEquals(1, sbLine53.memoryAddress.size)
        assertEquals(
            ((sbLine53.base as? Reference)?.memoryAddress?.firstOrNull() as? MemoryAddress)
                ?.fieldAddresses
                ?.filter { it.key == sbLine53.refersTo?.name?.localName }
                ?.entries
                ?.firstOrNull()
                ?.value
                ?.firstOrNull(),
            sbLine53.memoryAddress.firstOrNull()
        )
        assertEquals(1, sbLine53.prevDFG.size)
        assertEquals(literal2, sbLine53.prevDFG.firstOrNull())

        // Line 55
        assertEquals(1, paLine55.memoryAddress.size)
        assertEquals(saLine51.memoryAddress.first(), paLine55.memoryAddress.first())
        assertEquals(1, paLine55.prevDFG.size)
        assertEquals(literal1, paLine55.prevDFG.first())

        assertEquals(1, pbLine55.memoryAddress.size)
        assertEquals(sbLine52.memoryAddress.first(), pbLine55.memoryAddress.first())
        assertEquals(1, pbLine55.prevDFG.size)
        assertEquals(literal2, pbLine55.prevDFG.first())

        // Line 56
        assertEquals(1, paLine56.memoryAddress.size)
        assertEquals(saLine51.memoryAddress.first(), paLine56.memoryAddress.first())
        assertEquals(1, paLine56.prevDFG.size)
        assertEquals(literal3, paLine56.prevDFG.first())

        // Line 57
        assertEquals(1, pbLine57.memoryAddress.size)
        assertEquals(sbLine52.memoryAddress.first(), pbLine57.memoryAddress.first())
        assertEquals(1, pbLine57.prevDFG.size)
        assertEquals(literal4, pbLine57.prevDFG.first())

        // Line 59
        assertEquals(1, paLine59.memoryAddress.size)
        assertEquals(saLine51.memoryAddress.first(), paLine59.memoryAddress.first())
        assertEquals(1, paLine59.prevDFG.size)
        assertEquals(literal3, paLine59.prevDFG.first())

        assertEquals(1, pbLine59.memoryAddress.size)
        assertEquals(sbLine52.memoryAddress.first(), pbLine59.memoryAddress.first())
        assertEquals(1, pbLine59.prevDFG.size)
        assertEquals(literal4, pbLine59.prevDFG.first())
    }

    @Test
    fun testArrays() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
            }
        assertNotNull(tu)

        // References
        val n0Line66 =
            tu.allChildren<SubscriptExpression> { it.location?.region?.startLine == 66 }.first()
        val n0Line67 =
            tu.allChildren<SubscriptExpression> { it.location?.region?.startLine == 67 }.first()
        val n0Line68 =
            tu.allChildren<SubscriptExpression> { it.location?.region?.startLine == 68 }.first()
        val niLine71 =
            tu.allChildren<SubscriptExpression> { it.location?.region?.startLine == 71 }.first()
        val njLine75 =
            tu.allChildren<SubscriptExpression> { it.location?.region?.startLine == 75 }.first()

        // Literals
        val literal1 =
            tu.allChildren<Literal<*>> { it.location?.region?.startLine == 67 && it.value == 1 }
                .first()

        // Expressions
        val exprLine71 =
            tu.allChildren<BinaryOperator> { it.location?.region?.startLine == 71 }.first()

        // Line 66
        assertEquals(1, n0Line66.memoryAddress.size)
        assertTrue(n0Line66.arrayExpression.memoryAddress.first() is MemoryAddress)
        assertEquals(
            (n0Line66.arrayExpression.memoryAddress
                .filterIsInstance<MemoryAddress>()
                .firstOrNull()
                ?.fieldAddresses
                ?.get("0")
                ?.first() as MemoryAddress),
            n0Line66.memoryAddress.first()
        )
        assertEquals(1, n0Line66.prevDFG.size)
        assertTrue(n0Line66.prevDFG.first() is UnknownMemoryValue)

        // Line 67
        assertEquals(1, n0Line67.memoryAddress.size)
        assertEquals(
            (n0Line67.arrayExpression.memoryAddress
                .filterIsInstance<MemoryAddress>()
                .firstOrNull()
                ?.fieldAddresses
                ?.get("0")
                ?.first() as MemoryAddress),
            n0Line67.memoryAddress.first()
        )
        assertEquals(1, n0Line67.prevDFG.size)
        assertEquals(literal1, n0Line67.prevDFG.firstOrNull())

        // Line 68
        assertEquals(1, n0Line68.memoryAddress.size)
        assertEquals(
            (n0Line68.arrayExpression.memoryAddress
                .filterIsInstance<MemoryAddress>()
                .firstOrNull()
                ?.fieldAddresses
                ?.get("0")
                ?.first() as MemoryAddress),
            n0Line68.memoryAddress.first()
        )
        assertEquals(1, n0Line68.prevDFG.size)
        assertEquals(literal1, n0Line68.prevDFG.firstOrNull())

        // Line 71
        assertEquals(1, niLine71.memoryAddress.size)
        assertEquals(
            (niLine71.arrayExpression.memoryAddress
                .filterIsInstance<MemoryAddress>()
                .firstOrNull()
                ?.fieldAddresses
                ?.get("i")
                ?.first() as MemoryAddress),
            niLine71.memoryAddress.first()
        )
        assertEquals(1, niLine71.prevDFG.size)
        assertEquals(exprLine71, niLine71.prevDFG.firstOrNull())

        // Line 75
        assertEquals(1, njLine75.memoryAddress.size)
        assertEquals(
            (njLine75.arrayExpression.memoryAddress
                .filterIsInstance<MemoryAddress>()
                .firstOrNull()
                ?.fieldAddresses
                ?.get("j")
                ?.first() as MemoryAddress),
            njLine75.memoryAddress.first()
        )
        // TODO: What are our expections for njLine75.prevDFG? I think null is fine, since we
        // never defined that
        assertEquals(1, njLine75.prevDFG.size)
        assertTrue(njLine75.prevDFG.first() is UnknownMemoryValue)
    }

    @Test
    fun testMemcpy() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
            }
        assertNotNull(tu)

        // Declarations
        val aDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 89 }.firstOrNull()
        assertNotNull(aDecl)
        val bDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 90 }.firstOrNull()
        assertNotNull(bDecl)
        val cDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 91 }.firstOrNull()
        assertNotNull(cDecl)
        val caddrDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 92 }.firstOrNull()
        assertNotNull(caddrDecl)
        val dDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 93 }.firstOrNull()
        assertNotNull(dDecl)
        val eDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 94 }.firstOrNull()
        assertNotNull(eDecl)
        val fDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 95 }.firstOrNull()
        assertNotNull(fDecl)
        val gDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 96 }.firstOrNull()
        assertNotNull(gDecl)
        val hDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 97 }.firstOrNull()
        assertNotNull(hDecl)

        val paDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 99 }.firstOrNull()
        assertNotNull(paDecl)
        val pbDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 100 }.firstOrNull()
        assertNotNull(pbDecl)
        val pcDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 101 }.firstOrNull()
        assertNotNull(pcDecl)
        val pdDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 102 }.firstOrNull()
        assertNotNull(pdDecl)
        val peDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 103 }.firstOrNull()
        assertNotNull(peDecl)
        val pfDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 104 }.firstOrNull()
        assertNotNull(pfDecl)
        val pgDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 105 }.firstOrNull()
        assertNotNull(pgDecl)
        val phDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 106 }.firstOrNull()
        assertNotNull(phDecl)

        // References
        val aRef =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 128 &&
                        it.location?.region?.startColumn == 86 &&
                        it.name.localName == "a"
                }
                .firstOrNull()
        assertNotNull(aRef)
        val bRef =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 128 &&
                        it.location?.region?.startColumn == 93 &&
                        it.name.localName == "b"
                }
                .firstOrNull()
        assertNotNull(bRef)
        val cRef =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 128 &&
                        it.location?.region?.startColumn == 100 &&
                        it.name.localName == "c"
                }
                .firstOrNull()
        assertNotNull(cRef)
        val dRef =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 128 &&
                        it.location?.region?.startColumn == 107 &&
                        it.name.localName == "d"
                }
                .firstOrNull()
        assertNotNull(dRef)
        val eRef =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 128 &&
                        it.location?.region?.startColumn == 114 &&
                        it.name.localName == "e"
                }
                .firstOrNull()
        assertNotNull(eRef)
        val fRef =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 128 &&
                        it.location?.region?.startColumn == 121 &&
                        it.name.localName == "f"
                }
                .firstOrNull()
        assertNotNull(fRef)
        val paRef =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 129 &&
                        it.location?.region?.startColumn == 91 &&
                        it.name.localName == "pa"
                }
                .firstOrNull()
        assertNotNull(paRef)
        val pbRef =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 129 &&
                        it.location?.region?.startColumn == 100 &&
                        it.name.localName == "pb"
                }
                .firstOrNull()
        assertNotNull(pbRef)
        val pcRef =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 129 &&
                        it.location?.region?.startColumn == 109 &&
                        it.name.localName == "pc"
                }
                .firstOrNull()
        assertNotNull(pcRef)
        val pdRef =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 129 &&
                        it.location?.region?.startColumn == 118 &&
                        it.name.localName == "pd"
                }
                .firstOrNull()
        assertNotNull(pdRef)
        val peRef =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 129 &&
                        it.location?.region?.startColumn == 127 &&
                        it.name.localName == "pe"
                }
                .firstOrNull()
        assertNotNull(peRef)
        val pfRef =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 129 &&
                        it.location?.region?.startColumn == 136 &&
                        it.name.localName == "pf"
                }
                .firstOrNull()
        assertNotNull(pfRef)

        val aPointerRef =
            tu.allChildren<PointerReference> {
                    it.location?.region?.startLine == 128 && it.name.localName == "a"
                }
                .firstOrNull()
        assertNotNull(aPointerRef)
        val bPointerRef =
            tu.allChildren<PointerReference> {
                    it.location?.region?.startLine == 128 && it.name.localName == "b"
                }
                .firstOrNull()
        assertNotNull(bPointerRef)
        val cPointerRef =
            tu.allChildren<PointerReference> {
                    it.location?.region?.startLine == 128 && it.name.localName == "c"
                }
                .firstOrNull()
        assertNotNull(cPointerRef)
        val dPointerRef =
            tu.allChildren<PointerReference> {
                    it.location?.region?.startLine == 128 && it.name.localName == "d"
                }
                .firstOrNull()
        assertNotNull(dPointerRef)
        val ePointerRef =
            tu.allChildren<PointerReference> {
                    it.location?.region?.startLine == 128 && it.name.localName == "e"
                }
                .firstOrNull()
        assertNotNull(ePointerRef)
        val fPointerRef =
            tu.allChildren<PointerReference> {
                    it.location?.region?.startLine == 128 && it.name.localName == "f"
                }
                .firstOrNull()
        assertNotNull(fPointerRef)

        // PointerDerefs
        val paPointerDeref =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 129 && it.name.localName == "pa"
                }
                .firstOrNull()
        assertNotNull(paPointerDeref)
        val pbPointerDeref =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 129 && it.name.localName == "pb"
                }
                .firstOrNull()
        assertNotNull(pbPointerDeref)
        val pcPointerDeref =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 129 && it.name.localName == "pc"
                }
                .firstOrNull()
        assertNotNull(pcPointerDeref)
        val pdPointerDeref =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 129 && it.name.localName == "pd"
                }
                .firstOrNull()
        assertNotNull(pdPointerDeref)
        val pePointerDeref =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 129 && it.name.localName == "pe"
                }
                .firstOrNull()
        assertNotNull(pePointerDeref)
        val pfPointerDeref =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 129 && it.name.localName == "pf"
                }
                .firstOrNull()
        assertNotNull(pfPointerDeref)

        // Result of memcpy in Line 112
        assertEquals(1, bRef.memoryAddress.size)
        assertEquals(bDecl.memoryAddress, bRef.memoryAddress.first())
        assertEquals(1, bRef.prevDFG.size)
        assertEquals(aDecl.prevDFG.first(), bRef.prevDFG.first())

        assertEquals(1, pbPointerDeref.memoryAddress.size)
        assertEquals(bDecl.memoryAddress, pbPointerDeref.memoryAddress.first())
        assertEquals(1, pbPointerDeref.prevDFG.size)
        assertEquals(aDecl.prevDFG.first(), pbPointerDeref.prevDFG.first())

        // Result of memcpy in Line 115
        assertEquals(1, cRef.memoryAddress.size)
        assertEquals(cDecl.memoryAddress, cRef.memoryAddress.first())
        assertEquals(1, cRef.prevDFG.size)
        assertEquals(aDecl.prevDFG.first(), cRef.prevDFG.first())

        assertEquals(1, pcPointerDeref.memoryAddress.size)
        assertEquals(cDecl.memoryAddress, pcPointerDeref.memoryAddress.first())
        assertEquals(1, pcPointerDeref.prevDFG.size)
        assertEquals(aDecl.prevDFG.first(), pcPointerDeref.prevDFG.first())

        // Result of memcpy in Line 118
        assertEquals(1, dRef.memoryAddress.size)
        assertEquals(dDecl.memoryAddress, dRef.memoryAddress.first())
        assertEquals(1, dRef.prevDFG.size)
        assertEquals(aDecl.prevDFG.first(), dRef.prevDFG.first())

        assertEquals(1, pdPointerDeref.memoryAddress.size)
        assertEquals(dDecl.memoryAddress, pdPointerDeref.memoryAddress.first())
        assertEquals(1, pdPointerDeref.prevDFG.size)
        assertEquals(aDecl.prevDFG.first(), pdPointerDeref.prevDFG.first())

        // Result of memcpy in Line 121
        assertEquals(1, eRef.memoryAddress.size)
        assertEquals(eDecl.memoryAddress, eRef.memoryAddress.first())
        assertEquals(1, eRef.prevDFG.size)
        assertEquals(aDecl.prevDFG.first(), eRef.prevDFG.first())

        assertEquals(1, pePointerDeref.memoryAddress.size)
        assertEquals(eDecl.memoryAddress, pePointerDeref.memoryAddress.first())
        assertEquals(1, pePointerDeref.prevDFG.size)
        assertEquals(aDecl.prevDFG.first(), pePointerDeref.prevDFG.first())

        // Result of memcpy in Line 125
        assertEquals(1, fRef.memoryAddress.size)
        assertEquals(fDecl.memoryAddress, fRef.memoryAddress.first())
        assertEquals(1, fRef.prevDFG.size)
        assertEquals(fDecl.prevDFG.first(), fRef.prevDFG.first())

        assertEquals(1, pfPointerDeref.memoryAddress.size)
        assertEquals(aDecl.memoryAddress, pfPointerDeref.memoryAddress.first())
        assertEquals(1, pfPointerDeref.prevDFG.size)
        assertEquals(aDecl.prevDFG.first(), pfPointerDeref.prevDFG.first())
    }

    @Test
    fun testPointerToPointer() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
            }
        assertNotNull(tu)

        // Declarations
        val aDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 134 }.firstOrNull()
        assertNotNull(aDecl)
        val bDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 135 }.firstOrNull()
        assertNotNull(bDecl)
        val cDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 136 }.firstOrNull()
        assertNotNull(cDecl)

        // References
        val aRefLine138 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 138 && it.name.localName == "a"
                }
                .firstOrNull()
        assertNotNull(aRefLine138)
        val bRefLine138 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 138 &&
                        it.name.localName == "b" &&
                        it.location?.region?.startColumn == 65
                }
                .firstOrNull()
        assertNotNull(bRefLine138)
        val bRefLine139 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 139 && it.name.localName == "b"
                }
                .firstOrNull()
        assertNotNull(bRefLine139)
        val cRefLine139 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 139 &&
                        it.name.localName == "c" &&
                        it.location?.region?.startColumn == 68
                }
                .firstOrNull()
        assertNotNull(cRefLine139)

        // PointerDereferences
        val bPointerDerefLine138 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 138 && it.name.localName == "b"
                }
                .firstOrNull()
        assertNotNull(bPointerDerefLine138)
        val cPointerDerefLine139 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 139 && it.name.localName == "c"
                }
                .firstOrNull()
        assertNotNull(cPointerDerefLine139)
        val cPointerDerefLine140 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 140 &&
                        it.name.localName == "c" &&
                        it.input is PointerDereference
                }
                .firstOrNull()

        // Literals
        val literal10 =
            tu.allChildren<Literal<*>> { it.location?.region?.startLine == 134 }.firstOrNull()
        assertNotNull(literal10)

        assertNotNull(cPointerDerefLine140)

        // Line 138
        assertEquals(1, aRefLine138.memoryAddress.size)
        assertEquals(aDecl.memoryAddress, aRefLine138.memoryAddress.first())
        assertEquals(1, aRefLine138.prevDFG.size)
        assertEquals(literal10, aRefLine138.prevDFG.first())

        assertEquals(1, bRefLine138.memoryAddress.size)
        assertEquals(bDecl.memoryAddress, bRefLine138.memoryAddress.first())
        assertEquals(1, bRefLine138.prevDFG.size)
        assertEquals(aDecl.memoryAddress, bRefLine138.prevDFG.first())

        assertEquals(1, bPointerDerefLine138.memoryAddress.size)
        assertEquals(aDecl.memoryAddress, bPointerDerefLine138.memoryAddress.first())
        assertEquals(1, bPointerDerefLine138.prevDFG.size)
        assertEquals(literal10, bPointerDerefLine138.prevDFG.first())

        // Line 139
        assertEquals(1, bRefLine139.memoryAddress.size)
        assertEquals(bDecl.memoryAddress, bRefLine139.memoryAddress.first())
        assertEquals(1, bRefLine139.prevDFG.size)
        assertEquals(aDecl.memoryAddress, bRefLine139.prevDFG.first())

        assertEquals(1, cRefLine139.memoryAddress.size)
        assertEquals(cDecl.memoryAddress, cRefLine139.memoryAddress.first())
        assertEquals(1, cRefLine139.prevDFG.size)
        assertEquals(bDecl.memoryAddress, cRefLine139.prevDFG.first())

        assertEquals(1, cPointerDerefLine139.memoryAddress.size)
        assertEquals(bDecl.memoryAddress, cPointerDerefLine139.memoryAddress.first())
        assertEquals(1, cPointerDerefLine139.prevDFG.size)
        assertEquals(aDecl.memoryAddress, cPointerDerefLine139.prevDFG.first())

        // Line 140
        assertEquals(1, cPointerDerefLine140.memoryAddress.size)
        assertEquals(aDecl.memoryAddress, cPointerDerefLine140.memoryAddress.first())
        assertEquals(1, cPointerDerefLine140.prevDFG.size)
        assertEquals(literal10, cPointerDerefLine140.prevDFG.first())
    }

    @Test
    fun testGhidraCode() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
            }
        assertNotNull(tu)

        // ParameterDeclaration
        val param_1Line145 =
            tu.allChildren<ParameterDeclaration> { it.location?.region?.startLine == 145 }.first()
        assertNotNull(param_1Line145)

        val param_1Line193 =
            tu.allChildren<ParameterDeclaration> { it.location?.region?.startLine == 193 }.first()
        assertNotNull(param_1Line193)

        // References
        val local_20Line159 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 159 && it.name.localName == "local_20"
                }
                .first()
        assertNotNull(local_20Line159)
        val param_1Line159 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 159 && it.name.localName == "param_1"
                }
                .first()
        assertNotNull(param_1Line159)

        val local_30Line160 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 160 && it.name.localName == "local_30"
                }
                .first()
        assertNotNull(local_30Line160)
        val param_1Line160 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 160 && it.name.localName == "param_1"
                }
                .first()
        assertNotNull(param_1Line160)

        val local_30Line165 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 165 && it.name.localName == "local_30"
                }
                .first()
        assertNotNull(local_30Line165)

        val local_18Line165 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 165 && it.name.localName == "local_18"
                }
                .first()
        assertNotNull(local_18Line165)

        val local_10Line166 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 166 && it.name.localName == "local_10"
                }
                .first()
        assertNotNull(local_10Line166)

        val local_28Line167 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 167 && it.name.localName == "local_28"
                }
                .first()
        assertNotNull(local_28Line167)

        val local_28Line172 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 172 && it.name.localName == "local_28"
                }
                .first()
        assertNotNull(local_28Line172)

        val local_10Line172 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 172 && it.name.localName == "local_10"
                }
                .first()
        assertNotNull(local_10Line172)

        val local_28Line177 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 177 && it.name.localName == "local_28"
                }
                .first()
        assertNotNull(local_28Line177)

        val local_10Line177 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 177 && it.name.localName == "local_10"
                }
                .first()
        assertNotNull(local_10Line177)

        val local_28Line179 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 179 &&
                        it.name.localName == "local_28" &&
                        it.location?.region?.startColumn == 19
                }
                .first()
        assertNotNull(local_28Line179)

        val local_28Line180 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 180 && it.name.localName == "local_28"
                }
                .first()
        assertNotNull(local_28Line180)

        val local_28DerefLine181 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 181 && it.name.localName == "local_28"
                }
                .first()
        assertNotNull(local_28DerefLine181)

        val local_28Line182 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 182 && it.name.localName == "local_28"
                }
                .first()
        assertNotNull(local_28Line182)

        // PointerDereferences
        val local_28DerefLine179 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 179 && it.name.localName == "local_28"
                }
                .first()
        assertNotNull(local_28DerefLine179)

        val local_18DerefLine190 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 190 && it.name.localName == "local_18"
                }
                .first()
        assertNotNull(local_18DerefLine190)

        val param_1DerefLine190 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 190 && it.name.localName == "param_1"
                }
                .first()
        assertNotNull(param_1DerefLine190)

        // Literals
        val literal10Line166 =
            tu.allChildren<Literal<*>> { it.location?.region?.startLine == 166 }.first()
        assertNotNull(literal10Line166)

        val literal0Line167 =
            tu.allChildren<Literal<*>> { it.location?.region?.startLine == 167 }.first()
        assertNotNull(literal0Line167)

        val literal0Line177 =
            tu.allChildren<Literal<*>> { it.location?.region?.startLine == 177 }.first()
        assertNotNull(literal0Line177)

        // MemberExpressions
        val meLine201 =
            tu.allChildren<MemberExpression> { it.location?.region?.startLine == 201 }.first()
        assertNotNull(meLine201)

        // CallExpressions
        val ceLine172 =
            tu.allChildren<CallExpression> { it.location?.region?.startLine == 172 }.first()
        assertNotNull(ceLine172)

        val ceLine201 =
            tu.allChildren<CallExpression> { it.location?.region?.startLine == 201 }.first()
        assertNotNull(ceLine201)

        // Line 159
        assertEquals(1, local_20Line159.prevDFG.size)
        assertEquals(1, param_1Line159.prevDFG.size)
        assertEquals(param_1Line159.prevDFG.first(), local_20Line159.prevDFG.first())

        // Effect from Line 160
        assertEquals(1, local_30Line165.prevDFG.size)
        assertTrue(local_30Line165.prevDFG.first() is ParameterMemoryValue)
        assertEquals("param_1.derefvalue", local_30Line165.prevDFG.firstOrNull()?.name.toString())

        // Line 165
        assertEquals(1, local_18Line165.prevDFG.size)
        assertTrue(local_18Line165.prevDFG.first() is ParameterMemoryValue)
        assertEquals("param_1.derefvalue", local_18Line165.prevDFG.firstOrNull()?.name.toString())

        // Line 167
        assertEquals(1, local_28Line167.prevDFG.size)
        assertEquals(literal0Line167, local_28Line167.prevDFG.firstOrNull())

        // Line 172
        assertEquals(1, local_28Line172.prevDFG.size)
        assertEquals(ceLine172, local_28Line172.prevDFG.firstOrNull())

        // Line 179
        assertEquals(2, local_28Line179.prevDFG.size)
        assertTrue(local_28Line179.prevDFG.contains(literal0Line167))
        assertTrue(local_28Line179.prevDFG.contains(ceLine172))

        assertEquals(2, local_28DerefLine179.prevDFG.size)
        assertTrue(local_28DerefLine179.prevDFG.contains(literal0Line177))
        assertEquals(
            1,
            local_28DerefLine179.prevDFG
                .filterIsInstance<UnknownMemoryValue>()
                .filter { it.name.localName == "0" }
                .size
        )
        assertTrue(local_28DerefLine179.prevDFG.contains(literal0Line177))
        assertEquals(2, local_28DerefLine179.memoryAddress.size)
        assertTrue(local_28DerefLine179.memoryAddress.contains(literal0Line167))
        assertTrue(local_28DerefLine179.memoryAddress.contains(ceLine172))

        // Line 180
        assertEquals(2, local_28Line180.prevDFG.size)
        assertTrue(local_28Line180.prevDFG.contains(literal0Line167))
        assertTrue(local_28Line180.prevDFG.contains(ceLine172))

        // Line 181
        assertEquals(2, local_28DerefLine181.prevDFG.size)
        assertTrue(local_28DerefLine181.prevDFG.contains(ceLine201))
        assertTrue(
            local_28DerefLine181.prevDFG.any {
                it is UnknownMemoryValue && it.name.localName == "DAT_0011b1c8"
            }
        )

        // Line 190
        // TODO: verify the memcpy in Line 183
        assertEquals(1, local_18DerefLine190.memoryAddress.size)
        assertEquals(param_1Line145.memoryValue, local_18DerefLine190.prevDFG.firstOrNull())
        assertEquals(2, local_18DerefLine190.prevDFG.size)

        // Verify the functionSummary for sgx_ecall_key_to_out
    }

    @Test
    fun testFunctionSummaries() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
            }
        assertNotNull(tu)

        // Declarations
        val iDecl =
            tu.allChildren<VariableDeclaration> {
                    it.location?.region?.startLine == 224 && it.name.localName == "i"
                }
                .firstOrNull()
        assertNotNull(iDecl)

        val jDecl =
            tu.allChildren<VariableDeclaration> {
                    it.location?.region?.startLine == 225 && it.name.localName == "j"
                }
                .firstOrNull()
        assertNotNull(jDecl)

        // References
        val iRefLine208 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 208 && it.name.localName == "i"
                }
                .firstOrNull()
        assertNotNull(iRefLine208)

        val iRefLine230Left =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 230 &&
                        it.name.localName == "i" &&
                        it.location?.region?.startColumn == 3
                }
                .firstOrNull()
        assertNotNull(iRefLine230Left)

        val iRefLine230Right =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 230 &&
                        it.name.localName == "i" &&
                        it.location?.region?.startColumn == 9
                }
                .firstOrNull()
        assertNotNull(iRefLine230Right)

        val iRefLine231 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 231 && it.name.localName == "i"
                }
                .firstOrNull()
        assertNotNull(iRefLine231)

        val iRefLine234 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 234 && it.name.localName == "i"
                }
                .firstOrNull()
        assertNotNull(iRefLine234)

        val iRefLine237 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 237 && it.name.localName == "i"
                }
                .firstOrNull()
        assertNotNull(iRefLine237)

        val iRefLine240 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 240 && it.name.localName == "i"
                }
                .firstOrNull()
        assertNotNull(iRefLine240)

        val iRefLine242Left =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 242 &&
                        it.name.localName == "i" &&
                        it.location?.region?.startColumn == 3
                }
                .firstOrNull()
        assertNotNull(iRefLine242Left)

        val iRefLine242Right =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 242 &&
                        it.name.localName == "i" &&
                        it.location?.region?.startColumn == 19
                }
                .firstOrNull()
        assertNotNull(iRefLine242Right)

        val pRefLine242 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 242 && it.name.localName == "p"
                }
                .firstOrNull()
        assertNotNull(pRefLine242)

        // Dereferences
        val pDerefLine231 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 231 && it.name.localName == "p"
                }
                .firstOrNull()
        assertNotNull(pDerefLine231)

        val pDerefLine234 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 234 && it.name.localName == "p"
                }
                .firstOrNull()
        assertNotNull(pDerefLine234)

        val pDerefLine237 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 237 && it.name.localName == "p"
                }
                .firstOrNull()
        assertNotNull(pDerefLine237)

        val pDerefLine240 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 240 && it.name.localName == "p"
                }
                .firstOrNull()
        assertNotNull(pDerefLine240)

        // BinaryOperators
        val binOpLine207 =
            tu.allChildren<BinaryOperator> { it.location?.region?.startLine == 207 }.firstOrNull()
        assertNotNull(binOpLine207)

        val binOpLine212 =
            tu.allChildren<BinaryOperator> { it.location?.region?.startLine == 212 }.firstOrNull()
        assertNotNull(binOpLine212)

        // CallExpressions
        val ceLine230 =
            tu.allChildren<CallExpression> { it.location?.region?.startLine == 230 }.firstOrNull()
        assertNotNull(ceLine230)

        val ceLine242 =
            tu.allChildren<CallExpression> { it.location?.region?.startLine == 242 }.firstOrNull()
        assertNotNull(ceLine242)

        // Line 230
        assertEquals(1, ceLine230.prevDFG.size)
        assertEquals(binOpLine207, ceLine230.prevDFG.firstOrNull())
        assertEquals(1, iRefLine230Left.prevDFG.size)
        assertEquals(ceLine230, iRefLine230Left.prevDFG.firstOrNull())
        assertEquals(1, iRefLine230Right.nextDFG.size)
        assertTrue(iRefLine230Right.nextDFG.firstOrNull() is ParameterMemoryValue)
        assertLocalName("value", iRefLine230Right.nextDFG.firstOrNull())
        assertEquals("i", iRefLine230Right.nextDFG.firstOrNull()?.name?.parent?.localName)

        // Line 231
        assertEquals(1, iRefLine231.prevDFG.size)
        assertEquals(ceLine230, iRefLine231.prevDFG.first())
        assertEquals(1, pDerefLine231.prevDFG.size)
        assertEquals(ceLine230, pDerefLine231.prevDFG.first())
        assertEquals(1, pDerefLine231.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, pDerefLine231.memoryAddress.first())

        // Line 234
        assertEquals(1, pDerefLine234.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, pDerefLine234.memoryAddress.firstOrNull())
        assertEquals(1, iRefLine234.prevDFG.size)
        assertEquals(binOpLine212, iRefLine234.prevDFG.firstOrNull())
        assertEquals(1, pDerefLine234.prevDFG.size)
        assertEquals(binOpLine212, pDerefLine234.prevDFG.firstOrNull())
        assertEquals(1, pDerefLine234.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, pDerefLine234.memoryAddress.first())

        // Line 237
        assertEquals(1, pDerefLine237.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, pDerefLine237.memoryAddress.firstOrNull())
        assertEquals(1, pDerefLine237.prevDFG.size)
        assertEquals(jDecl.prevDFG.firstOrNull(), pDerefLine237.prevDFG.firstOrNull())

        // Line 240
        assertEquals(1, pDerefLine240.memoryAddress.size)
        assertEquals(iDecl.memoryAddress, pDerefLine240.memoryAddress.firstOrNull())
        assertEquals(1, pDerefLine240.prevDFG.size)
        assertEquals(binOpLine212, pDerefLine240.prevDFG.firstOrNull())
        assertEquals(1, iRefLine240.prevDFG.size)
        assertEquals(binOpLine212, iRefLine240.prevDFG.firstOrNull())

        // Line 242
        assertEquals(1, iRefLine242Left.prevDFG.size)
        assertEquals(ceLine242, iRefLine242Left.prevDFG.firstOrNull())
        assertEquals(2, ceLine242.prevDFG.size)
        assertTrue(ceLine242.prevDFG.contains(iRefLine242Right))
        assertTrue(ceLine242.prevDFG.contains(pRefLine242))
    }
}
