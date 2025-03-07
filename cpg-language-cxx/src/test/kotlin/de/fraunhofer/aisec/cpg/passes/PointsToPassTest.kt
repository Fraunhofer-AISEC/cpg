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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edges.flows.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.PointerDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertLocalName
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse

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
        val iRefLine6 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 6 && it.location?.region?.startColumn == 11
                }
                .first()
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
        assertLocalName("i", iDecl.memoryAddresses.singleOrNull())
        assertEquals(literal0, iDecl.prevFullDFG.singleOrNull())
        assertEquals(literal0, iDecl.memoryValues.singleOrNull())

        // Line 5
        assertLocalName("j", jDecl.memoryAddresses.singleOrNull())
        assertEquals(literal1, jDecl.prevFullDFG.singleOrNull())
        assertEquals(literal1, jDecl.memoryValues.singleOrNull())

        // Line 6
        assertLocalName("a", aDecl.memoryAddresses.singleOrNull())
        assertEquals(iPointerRef, aDecl.prevDFG.singleOrNull())
        assertEquals(iDecl.memoryAddresses.singleOrNull(), aDecl.memoryValues.singleOrNull())

        assertEquals(iDecl, iRefLine6.prevDFG.singleOrNull())

        assertTrue(iPointerRef.memoryAddresses.isEmpty())
        //        assertEquals(?, iPointerRef.prevDFG.singleOrNull())
        assertEquals(
            iPointerRef.memoryValues.first() as MemoryAddress?,
            iDecl.memoryAddresses.singleOrNull(),
        )

        // Line 7
        assertLocalName("b", bDecl.memoryAddresses.singleOrNull())
        assertEquals(1, bDecl.prevFullDFG.size)
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            bDecl.prevFullDFG.first() as MemoryAddress?,
        )

        // Line 8
        assertEquals(1, iRefLine8.memoryAddresses.size)
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            iRefLine8.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, iRefLine8.prevFullDFG.size)
        assertEquals(literal0, iRefLine8.prevFullDFG.first())

        // Line 9
        assertEquals(1, iRefLine9.memoryAddresses.size)
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            iRefLine9.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, iRefLine9.prevFullDFG.size)
        assertEquals(literal2, iRefLine9.prevFullDFG.filterIsInstance<Literal<*>>().first())

        // Line 10
        assertEquals(1, iRefLine10.memoryAddresses.size)
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            iRefLine10.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, iRefLine10.prevFullDFG.size)
        assertEquals(literal2, iRefLine10.prevFullDFG.first())

        // Line 11
        assertEquals(1, iRefLine11.memoryAddresses.size)
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            iRefLine11.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, iRefLine11.prevFullDFG.size)
        assertTrue(iRefLine11.prevFullDFG.filterIsInstance<BinaryOperator>().isNotEmpty())

        // Line 12
        assertEquals(1, aPointerDerefLine12.memoryAddresses.size)
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            aPointerDerefLine12.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, aPointerDerefLine12.prevFullDFG.size)
        assertTrue(aPointerDerefLine12.prevFullDFG.filterIsInstance<BinaryOperator>().isNotEmpty())

        // Line 13 should only update the DeclarationState, not much here to test
        // Line 14
        assertEquals(1, aPointerDerefLine14.memoryAddresses.size)
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            aPointerDerefLine14.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, aPointerDerefLine14.prevFullDFG.size)
        assertEquals(iUO.input, aPointerDerefLine14.prevFullDFG.first())

        // Line 15
        assertTrue(jPointerRef.memoryAddresses.isEmpty())
        assertEquals(1, jPointerRef.prevFullDFG.size)
        assertEquals(
            jDecl.memoryAddresses.singleOrNull(),
            jPointerRef.prevFullDFG.first() as MemoryAddress?,
        )
        assertEquals(1, aRefLine15.memoryAddresses.size)
        assertEquals(
            aDecl.memoryAddresses.singleOrNull(),
            aRefLine15.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, aRefLine15.prevFullDFG.size)
        assertEquals(
            jDecl.memoryAddresses.singleOrNull(),
            aRefLine15.prevFullDFG.first() as MemoryAddress?,
        )

        // Line 16
        assertEquals(1, aPointerDerefLine16.memoryAddresses.size)
        assertEquals(
            jDecl.memoryAddresses.singleOrNull(),
            aPointerDerefLine16.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, aPointerDerefLine16.prevFullDFG.size)
        assertEquals(literal1, aPointerDerefLine16.prevFullDFG.first())

        // Line 17
        assertEquals(1, aPointerDerefLine17.memoryAddresses.size)
        assertEquals(
            jDecl.memoryAddresses.singleOrNull(),
            aPointerDerefLine17.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, aPointerDerefLine17.prevFullDFG.size)
        assertEquals(literal3, aPointerDerefLine17.prevFullDFG.first())

        // Line 18
        assertEquals(1, bPointerDerefLine18.memoryAddresses.size)
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            bPointerDerefLine18.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, bPointerDerefLine18.prevFullDFG.size)
        assertEquals(iUO.input, bPointerDerefLine18.prevFullDFG.first())
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
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            aPointerDerefLine27.memoryAddresses.firstOrNull(),
        )
        assertEquals(1, aPointerDerefLine27.prevFullDFG.size)
        assertEquals(iDecl.prevFullDFG.first(), aPointerDerefLine27.prevFullDFG.first())

        // Line 30
        assertEquals(
            jDecl.memoryAddresses.singleOrNull(),
            aPointerDerefLine30.memoryAddresses.firstOrNull(),
        )
        assertEquals(1, aPointerDerefLine30.prevFullDFG.size)
        assertEquals(jDecl.prevFullDFG.first(), aPointerDerefLine30.prevFullDFG.first())

        // Line 32
        assertEquals(2, aPointerDerefLine32.memoryAddresses.size)
        assertNotNull(iDecl.memoryAddresses.singleOrNull())
        assertNotNull(jDecl.memoryAddresses.singleOrNull())
        assertTrue(
            aPointerDerefLine32.memoryAddresses.containsAll(
                setOf(iDecl.memoryAddresses.single(), jDecl.memoryAddresses.single())
            )
        )
        assertEquals(2, aPointerDerefLine32.prevFullDFG.size)
        assertTrue(aPointerDerefLine32.prevFullDFG.contains(iDecl.prevFullDFG.first()))
        assertTrue(aPointerDerefLine32.prevFullDFG.contains(jDecl.prevFullDFG.first()))

        // Line 37
        assertEquals(2, aPointerDerefLine37.memoryAddresses.size)
        assertTrue(
            aPointerDerefLine37.memoryAddresses.containsAll(
                setOf(iDecl.memoryAddresses.single(), jDecl.memoryAddresses.single())
            )
        )
        assertEquals(3, aPointerDerefLine37.prevFullDFG.size)
        assertTrue(aPointerDerefLine37.prevFullDFG.contains(iDecl.prevFullDFG.first()))
        assertTrue(aPointerDerefLine37.prevFullDFG.contains(jDecl.prevFullDFG.first()))
        assertTrue(aPointerDerefLine37.prevFullDFG.contains(iUO.input))
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
        assertEquals(1, saLine51.memoryAddresses.size)
        assertEquals(1, saLine51.prevFullDFG.size)
        assertEquals(literal1, saLine51.prevFullDFG.firstOrNull())

        // Line 52
        assertEquals(1, sbLine52.memoryAddresses.size)
        assertEquals(1, sbLine52.prevFullDFG.size)
        assertEquals(literal2, sbLine52.prevFullDFG.firstOrNull())

        // Line 53
        assertEquals(1, saLine53.memoryAddresses.size)
        assertEquals(1, saLine53.prevFullDFG.size)
        assertEquals(literal1, saLine53.prevFullDFG.firstOrNull())

        assertEquals(1, sbLine53.memoryAddresses.size)
        assertEquals(1, sbLine53.prevFullDFG.size)
        assertEquals(literal2, sbLine53.prevFullDFG.firstOrNull())

        // Line 55
        assertEquals(1, paLine55.memoryAddresses.size)
        assertEquals(saLine51.memoryAddresses.first(), paLine55.memoryAddresses.first())
        assertEquals(1, paLine55.prevFullDFG.size)
        assertEquals(literal1, paLine55.prevFullDFG.first())

        assertEquals(1, pbLine55.memoryAddresses.size)
        assertEquals(sbLine52.memoryAddresses.first(), pbLine55.memoryAddresses.first())
        assertEquals(1, pbLine55.prevFullDFG.size)
        assertEquals(literal2, pbLine55.prevFullDFG.first())

        // Line 56
        assertEquals(1, paLine56.memoryAddresses.size)
        assertEquals(saLine51.memoryAddresses.first(), paLine56.memoryAddresses.first())
        assertEquals(1, paLine56.prevFullDFG.size)
        assertEquals(literal3, paLine56.prevFullDFG.first())

        // Line 57
        assertEquals(1, pbLine57.memoryAddresses.size)
        assertEquals(sbLine52.memoryAddresses.first(), pbLine57.memoryAddresses.first())
        assertEquals(1, pbLine57.prevFullDFG.size)
        assertEquals(literal4, pbLine57.prevFullDFG.first())

        // Line 59
        assertEquals(1, paLine59.memoryAddresses.size)
        assertEquals(saLine51.memoryAddresses.first(), paLine59.memoryAddresses.first())
        assertEquals(1, paLine59.prevFullDFG.size)
        assertEquals(literal3, paLine59.prevFullDFG.first())

        assertEquals(1, pbLine59.memoryAddresses.size)
        assertEquals(sbLine52.memoryAddresses.first(), pbLine59.memoryAddresses.first())
        assertEquals(1, pbLine59.prevFullDFG.size)
        assertEquals(literal4, pbLine59.prevFullDFG.first())
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
        // TODO
        assertEquals(1, n0Line66.memoryAddresses.size)
        /*        assertEquals(
            n0Line66.arrayExpression.prevFullDFG.first().fieldAddresses.get("0")?.first() as Node,
            n0Line66.memoryAddresses.first()
        )*/
        assertEquals(1, n0Line66.prevFullDFG.size)
        assertTrue(n0Line66.prevFullDFG.first() is UnknownMemoryValue)

        // Line 67
        assertEquals(1, n0Line67.memoryAddresses.size)
        // TODO
        /*        assertEquals(
            n0Line67.base.prevFullDFG.first(),
            (n0Line67.memoryAddresses.first() as MemoryAddress?)?.memoryParent
        )*/
        assertEquals(1, n0Line67.prevFullDFG.size)
        assertEquals(literal1, n0Line67.prevFullDFG.firstOrNull())

        // Line 68
        assertEquals(1, n0Line68.memoryAddresses.size)
        // TODO
        /*        assertEquals(
            n0Line68.base.prevFullDFG.first(),
            (n0Line68.memoryAddresses.first() as MemoryAddress?)?.memoryParent
        )*/
        assertEquals(1, n0Line68.prevFullDFG.size)
        assertEquals(literal1, n0Line68.prevFullDFG.firstOrNull())

        // Line 71
        assertEquals(1, niLine71.memoryAddresses.size)
        // TODO
        /*        assertEquals(
            niLine71.base.prevFullDFG.first(),
            (niLine71.memoryAddresses.first() as MemoryAddress?)?.memoryParent
        )*/
        assertEquals(1, niLine71.prevFullDFG.size)
        assertEquals(exprLine71, niLine71.prevFullDFG.firstOrNull())

        // Line 75
        assertEquals(1, njLine75.memoryAddresses.size)
        // TODO
        /*        assertEquals(
            njLine75.base.prevFullDFG.first(),
            (njLine75.memoryAddresses.first() as MemoryAddress?)?.memoryParent
        )*/
        assertEquals(1, njLine75.prevFullDFG.size)
        assertTrue(njLine75.prevFullDFG.first() is UnknownMemoryValue)
        assertLocalName("j", njLine75.prevFullDFG.first())
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
        assertEquals(1, bRef.memoryAddresses.size)
        assertEquals(
            bDecl.memoryAddresses.singleOrNull(),
            bRef.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, bRef.prevFullDFG.size)
        assertEquals(aDecl.prevFullDFG.first(), bRef.prevFullDFG.first())

        assertEquals(1, pbPointerDeref.memoryAddresses.size)
        assertEquals(
            bDecl.memoryAddresses.singleOrNull(),
            pbPointerDeref.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, pbPointerDeref.prevFullDFG.size)
        assertEquals(aDecl.prevFullDFG.first(), pbPointerDeref.prevFullDFG.first())

        // Result of memcpy in Line 115
        assertEquals(1, cRef.memoryAddresses.size)
        assertEquals(
            cDecl.memoryAddresses.singleOrNull(),
            cRef.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, cRef.prevFullDFG.size)
        assertEquals(aDecl.prevFullDFG.first(), cRef.prevFullDFG.first())

        assertEquals(1, pcPointerDeref.memoryAddresses.size)
        assertEquals(
            cDecl.memoryAddresses.singleOrNull(),
            pcPointerDeref.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, pcPointerDeref.prevFullDFG.size)
        assertEquals(aDecl.prevFullDFG.first(), pcPointerDeref.prevFullDFG.first())

        // Result of memcpy in Line 118
        assertEquals(1, dRef.memoryAddresses.size)
        assertEquals(
            dDecl.memoryAddresses.singleOrNull(),
            dRef.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, dRef.prevFullDFG.size)
        assertEquals(aDecl.prevFullDFG.first(), dRef.prevFullDFG.first())

        assertEquals(1, pdPointerDeref.memoryAddresses.size)
        assertEquals(
            dDecl.memoryAddresses.singleOrNull(),
            pdPointerDeref.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, pdPointerDeref.prevFullDFG.size)
        assertEquals(aDecl.prevFullDFG.first(), pdPointerDeref.prevFullDFG.first())

        // Result of memcpy in Line 121
        assertEquals(1, eRef.memoryAddresses.size)
        assertEquals(
            eDecl.memoryAddresses.singleOrNull(),
            eRef.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, eRef.prevFullDFG.size)
        assertEquals(aDecl.prevFullDFG.first(), eRef.prevFullDFG.first())

        assertEquals(1, pePointerDeref.memoryAddresses.size)
        assertEquals(
            eDecl.memoryAddresses.singleOrNull(),
            pePointerDeref.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, pePointerDeref.prevFullDFG.size)
        assertEquals(aDecl.prevFullDFG.first(), pePointerDeref.prevFullDFG.first())

        // Result of memcpy in Line 125
        assertEquals(1, fRef.memoryAddresses.size)
        assertEquals(
            fDecl.memoryAddresses.singleOrNull(),
            fRef.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, fRef.prevFullDFG.size)
        assertEquals(fDecl.prevFullDFG.first(), fRef.prevFullDFG.first())

        assertEquals(1, pfPointerDeref.memoryAddresses.size)
        assertEquals(
            aDecl.memoryAddresses.singleOrNull(),
            pfPointerDeref.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, pfPointerDeref.prevFullDFG.size)
        assertEquals(aDecl.prevFullDFG.first(), pfPointerDeref.prevFullDFG.first())
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
        assertEquals(1, aRefLine138.memoryAddresses.size)
        assertEquals(
            aDecl.memoryAddresses.singleOrNull(),
            aRefLine138.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, aRefLine138.prevFullDFG.size)
        assertEquals(literal10, aRefLine138.prevFullDFG.first())

        assertEquals(1, bRefLine138.memoryAddresses.size)
        assertEquals(
            bDecl.memoryAddresses.singleOrNull(),
            bRefLine138.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, bRefLine138.prevFullDFG.size)
        assertEquals(
            aDecl.memoryAddresses.singleOrNull(),
            bRefLine138.prevFullDFG.first() as MemoryAddress?,
        )

        assertEquals(1, bPointerDerefLine138.memoryAddresses.size)
        assertEquals(
            aDecl.memoryAddresses.singleOrNull(),
            bPointerDerefLine138.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, bPointerDerefLine138.prevFullDFG.size)
        assertEquals(literal10, bPointerDerefLine138.prevFullDFG.first())

        // Line 139
        assertEquals(1, bRefLine139.memoryAddresses.size)
        assertEquals(
            bDecl.memoryAddresses.singleOrNull(),
            bRefLine139.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, bRefLine139.prevFullDFG.size)
        assertEquals(
            aDecl.memoryAddresses.singleOrNull(),
            bRefLine139.prevFullDFG.first() as MemoryAddress?,
        )

        assertEquals(1, cRefLine139.memoryAddresses.size)
        assertEquals(
            cDecl.memoryAddresses.singleOrNull(),
            cRefLine139.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, cRefLine139.prevFullDFG.size)
        assertEquals(
            bDecl.memoryAddresses.singleOrNull(),
            cRefLine139.prevFullDFG.first() as MemoryAddress?,
        )

        assertEquals(1, cPointerDerefLine139.memoryAddresses.size)
        assertEquals(
            bDecl.memoryAddresses.singleOrNull(),
            cPointerDerefLine139.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, cPointerDerefLine139.prevFullDFG.size)
        assertEquals(
            aDecl.memoryAddresses.singleOrNull(),
            cPointerDerefLine139.prevFullDFG.first() as MemoryAddress?,
        )

        // Line 140
        assertEquals(1, cPointerDerefLine140.memoryAddresses.size)
        assertEquals(
            aDecl.memoryAddresses.singleOrNull(),
            cPointerDerefLine140.memoryAddresses.first() as MemoryAddress?,
        )
        assertEquals(1, cPointerDerefLine140.prevFullDFG.size)
        assertEquals(literal10, cPointerDerefLine140.prevFullDFG.first())
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

        val param_1DerefLine201 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 201 && it.name.localName == "param_1"
                }
                .first()
        assertNotNull(param_1DerefLine201)

        val param_1SSELine202 =
            tu.allChildren<SubscriptExpression> { it.location?.region?.startLine == 202 }.first()
        assertNotNull(param_1SSELine202)

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

        // SubscriptExpressions
        val sseLine181 =
            tu.allChildren<SubscriptExpression> { it.location?.region?.startLine == 181 }.first()
        assertNotNull(sseLine181)

        // FunctionSummaries
        val fsecallkeytoout =
            tu.allChildren<FunctionDeclaration> { it.name.localName == "ecall_key_to_out" }
                .first()
                .functionSummary
        assertNotNull(fsecallkeytoout)

        val fssgxecallkeytoout =
            tu.allChildren<FunctionDeclaration> { it.name.localName == "sgx_ecall_key_to_out" }
                .first()
                .functionSummary
        assertNotNull(fssgxecallkeytoout)

        // Line 159
        assertEquals(1, local_20Line159.prevFullDFG.size)
        assertEquals(1, param_1Line159.prevFullDFG.size)
        assertEquals(param_1Line159.prevFullDFG.first(), local_20Line159.prevFullDFG.first())

        // Effect from Line 160
        assertEquals(1, local_30Line165.prevFullDFG.size)
        assertTrue(local_30Line165.prevFullDFG.first() is ParameterMemoryValue)
        assertEquals(
            "param_1.derefvalue",
            local_30Line165.prevFullDFG.firstOrNull()?.name.toString(),
        )

        // Line 165
        assertEquals(1, local_18Line165.prevFullDFG.size)
        assertTrue(local_18Line165.prevFullDFG.first() is ParameterMemoryValue)
        assertEquals(
            "param_1.derefvalue",
            local_18Line165.prevFullDFG.firstOrNull()?.name.toString(),
        )

        // Line 167
        assertEquals(1, local_28Line167.prevFullDFG.size)
        assertEquals(literal0Line167, local_28Line167.prevFullDFG.firstOrNull())

        // Line 172
        assertEquals(1, local_28Line172.prevFullDFG.size)
        assertEquals(ceLine172, local_28Line172.prevFullDFG.firstOrNull())

        // Line 179
        assertEquals(2, local_28Line179.prevFullDFG.size)
        assertTrue(local_28Line179.prevFullDFG.contains(literal0Line167))
        assertTrue(local_28Line179.prevFullDFG.contains(ceLine172))

        assertEquals(2, local_28DerefLine179.prevFullDFG.size)
        assertTrue(local_28DerefLine179.prevFullDFG.contains(literal0Line177))
        assertEquals(
            1,
            local_28DerefLine179.prevDFG
                .filterIsInstance<UnknownMemoryValue>()
                .filter { it.name.localName == "0" }
                .size,
        )
        assertTrue(local_28DerefLine179.prevFullDFG.contains(literal0Line177))
        assertEquals(
            2,
            local_28DerefLine179.memoryValues.size,
        ) // TODO: We used memoryAddresses here. Why?
        assertTrue(local_28DerefLine179.memoryValues.contains(literal0Line167))
        assertTrue(local_28DerefLine179.memoryValues.contains(ceLine172))

        // Line 180
        assertEquals(2, local_28Line180.prevFullDFG.size)
        assertTrue(local_28Line180.prevFullDFG.contains(literal0Line167))
        assertTrue(local_28Line180.prevFullDFG.contains(ceLine172))

        // Line 181
        assertEquals(2, local_28DerefLine181.prevFullDFG.size)
        assertTrue(local_28DerefLine181.prevFullDFG.contains(ceLine201))
        assertEquals(
            1,
            local_28DerefLine181.prevDFG
                .filter { it is UnknownMemoryValue && it.name.localName == "DAT_0011b1c8" }
                .size,
        )
        assertEquals(1, sseLine181.prevFullDFG.size)
        assertEquals(
            1,
            sseLine181.prevDFG
                .filter { it is UnknownMemoryValue && it.name.localName == "DAT_0011b1c8" }
                .size,
        )

        // Line 190
        assertEquals(1, local_18DerefLine190.memoryAddresses.size)
        assertTrue(local_18DerefLine190.memoryAddresses.firstOrNull() is ParameterMemoryValue)
        assertLocalName("derefvalue", local_18DerefLine190.memoryAddresses.firstOrNull())
        assertEquals(3, local_18DerefLine190.prevFullDFG.size)
        assertTrue(local_18DerefLine190.prevFullDFG.contains(ceLine201))
        assertTrue(
            local_18DerefLine190.prevFullDFG.any {
                it is UnknownMemoryValue && it.name.localName == "DAT_0011b1c8"
            }
        )
        assertEquals(
            1,
            local_18DerefLine190.prevDFG
                .filter { it is ParameterMemoryValue && it.name.localName == "derefderefvalue" }
                .size,
        )

        // Line 201
        assertEquals(1, param_1DerefLine201.prevFullDFG.size)
        assertEquals(ceLine201, param_1DerefLine201.prevFullDFG.firstOrNull())

        // Line 202
        assertEquals(1, param_1SSELine202.prevFullDFG.size)
        assertTrue(
            param_1SSELine202.prevFullDFG.any {
                it is UnknownMemoryValue && it.name.localName == "DAT_0011b1c8"
            }
        )

        // FunctionSummary of ecall_key_to_out
        assertEquals(1, fsecallkeytoout.size)
        assertTrue(fsecallkeytoout.entries.firstOrNull()?.key is ParameterDeclaration)
        assertLocalName("param_1", fsecallkeytoout.entries.firstOrNull()?.key)
        assertTrue(
            fsecallkeytoout.entries.firstOrNull()?.value?.any { it.srcNode == ceLine201 } == true
        )
        assertTrue(
            fsecallkeytoout.entries.firstOrNull()?.value?.any {
                it.srcNode is UnknownMemoryValue && it.srcNode.name.localName == "DAT_0011b1c8"
            } == true
        )

        // FunctionSummary of sgx_ecall_key_to_out
        assertEquals(1, fssgxecallkeytoout.filter { it.key !is ReturnStatement }.size)
        assertTrue(
            fssgxecallkeytoout.filter { it.key !is ReturnStatement }.entries.firstOrNull()?.key
                is ParameterDeclaration
        )
        assertLocalName(
            "param_1",
            fssgxecallkeytoout.filter { it.key !is ReturnStatement }.entries.firstOrNull()?.key,
        )
        assertEquals(
            1,
            fssgxecallkeytoout
                .filter { it.key !is ReturnStatement }
                .entries
                .firstOrNull()
                ?.value
                ?.filter { it.srcNode == ceLine201 }
                ?.size,
        )
        assertEquals(
            1,
            fssgxecallkeytoout
                .filter { it.key !is ReturnStatement }
                .entries
                .firstOrNull()
                ?.value
                ?.filter {
                    it.srcNode is UnknownMemoryValue && it.srcNode.name.localName == "DAT_0011b1c8"
                }
                ?.size,
        )
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

        val ceLine233 =
            tu.allChildren<CallExpression> { it.location?.region?.startLine == 233 }.firstOrNull()
        assertNotNull(ceLine233)

        val ceLine236 =
            tu.allChildren<CallExpression> { it.location?.region?.startLine == 236 }.firstOrNull()
        assertNotNull(ceLine236)

        val ceLine239 =
            tu.allChildren<CallExpression> { it.location?.region?.startLine == 239 }.firstOrNull()
        assertNotNull(ceLine239)

        val ceLine242 =
            tu.allChildren<CallExpression> { it.location?.region?.startLine == 242 }.firstOrNull()
        assertNotNull(ceLine242)

        // Line 230
        assertEquals(1, ceLine230.prevFullDFG.size)
        assertEquals(binOpLine207, ceLine230.prevFullDFG.firstOrNull())
        assertEquals(1, iRefLine230Left.prevFullDFG.size)
        assertEquals(ceLine230, iRefLine230Left.prevFullDFG.firstOrNull())
        assertEquals(1, iRefLine230Right.nextDFG.size)
        assertTrue(iRefLine230Right.nextDFG.firstOrNull() is ParameterMemoryValue)
        assertLocalName("value", iRefLine230Right.nextDFG.firstOrNull())
        assertEquals("i", iRefLine230Right.nextDFG.firstOrNull()?.name?.parent?.localName)

        // Line 231
        assertEquals(1, iRefLine231.prevFullDFG.size)
        assertEquals(ceLine230, iRefLine231.prevFullDFG.first())
        assertEquals(1, pDerefLine231.prevFullDFG.size)
        assertEquals(ceLine230, pDerefLine231.prevFullDFG.first())
        assertEquals(1, pDerefLine231.memoryAddresses.size)
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            pDerefLine231.memoryAddresses.first() as MemoryAddress?,
        )

        // Line 234
        assertEquals(1, pDerefLine234.memoryAddresses.size)
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            pDerefLine234.memoryAddresses.firstOrNull(),
        )
        assertEquals(1, iRefLine234.prevFullDFG.size)
        assertTrue(iRefLine234.prevFullDFG.contains(binOpLine212))
        assertEquals(2, iRefLine234.prevFunctionSummaryDFG.size)
        assertTrue(iRefLine234.prevFunctionSummaryDFG.contains(ceLine233.arguments[0]))
        assertTrue(iRefLine234.prevFunctionSummaryDFG.contains(ceLine233.invokes.first()))
        assertEquals(1, pDerefLine234.prevFullDFG.size)
        assertTrue(pDerefLine234.prevFullDFG.contains(binOpLine212))
        assertEquals(2, pDerefLine234.prevFunctionSummaryDFG.size)
        assertTrue(pDerefLine234.prevFunctionSummaryDFG.contains(ceLine233.arguments[0]))
        assertTrue(pDerefLine234.prevFunctionSummaryDFG.contains(ceLine233.invokes.first()))
        assertEquals(1, pDerefLine234.memoryAddresses.size)
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            pDerefLine234.memoryAddresses.first() as MemoryAddress?,
        )

        // Line 237
        assertEquals(1, pDerefLine237.memoryAddresses.size)
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            pDerefLine237.memoryAddresses.firstOrNull(),
        )
        assertEquals(1, pDerefLine237.prevFullDFG.size)
        assertTrue(pDerefLine237.prevFullDFG.contains(jDecl.prevFullDFG.firstOrNull()))
        assertEquals(2, pDerefLine237.prevFunctionSummaryDFG.size)
        assertTrue(pDerefLine237.prevFunctionSummaryDFG.contains(ceLine236.arguments[0]))
        assertTrue(pDerefLine237.prevFunctionSummaryDFG.contains(ceLine236.invokes.first()))

        // Line 240
        assertEquals(1, pDerefLine240.memoryAddresses.size)
        assertEquals(
            iDecl.memoryAddresses.singleOrNull(),
            pDerefLine240.memoryAddresses.firstOrNull(),
        )
        assertEquals(1, pDerefLine240.prevFullDFG.size)
        assertTrue(pDerefLine240.prevFullDFG.contains(binOpLine212))
        assertEquals(2, pDerefLine240.prevFunctionSummaryDFG.size)
        assertTrue(pDerefLine240.prevFunctionSummaryDFG.contains(ceLine239.arguments[0]))
        assertTrue(pDerefLine240.prevFunctionSummaryDFG.contains(ceLine239.invokes.first()))
        assertEquals(1, iRefLine240.prevFullDFG.size)
        assertTrue(iRefLine240.prevFullDFG.contains(binOpLine212))
        assertEquals(2, iRefLine240.prevFunctionSummaryDFG.size)
        assertTrue(iRefLine240.prevFunctionSummaryDFG.contains(ceLine239.arguments[0]))
        assertTrue(iRefLine240.prevFunctionSummaryDFG.contains(ceLine239.invokes.first()))

        // Line 242
        assertEquals(1, iRefLine242Left.prevFullDFG.size)
        assertEquals(ceLine242, iRefLine242Left.prevFullDFG.firstOrNull())
        assertEquals(2, ceLine242.prevFullDFG.size)
        assertTrue(ceLine242.prevFullDFG.contains(iDecl.memoryAddresses.single()))
        assertTrue(ceLine242.prevFullDFG.contains(binOpLine212))
        assertEquals(2, ceLine242.prevFunctionSummaryDFG.size)
        assertTrue(ceLine242.prevFunctionSummaryDFG.contains(ceLine239.invokes.first()))
        assertTrue(ceLine242.prevFunctionSummaryDFG.contains(ceLine239.arguments.first()))
    }

    @Test
    fun testSubstructs() {
        val file = File("src/test/resources/c/dataflow/tls.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
                it.configurePass<PointsToPass>(PointsToPass.Configuration(addressLength = 64))
            }
        assertNotNull(tu)

        // FunctionSummaries
        val FSread =
            tu.allChildren<FunctionDeclaration> { it.name.localName == "mbedtls_ssl_read" }
                .first()
                .functionSummary
        assertNotNull(FSread)

        val FSinnerrenegotiate =
            tu.allChildren<FunctionDeclaration> { it.name.localName == "inner_renegotiate" }
                .first()
                .functionSummary
        assertNotNull(FSinnerrenegotiate)

        val FSrenegotiate =
            tu.allChildren<FunctionDeclaration> { it.name.localName == "renegotiate" }
                .first()
                .functionSummary
        assertNotNull(FSrenegotiate)

        // Literals
        val literal1 =
            tu.allChildren<Literal<*>> { it.location?.region?.startLine == 26 }.firstOrNull()
        assertNotNull(literal1)

        val literal2 =
            tu.allChildren<Literal<*>> { it.location?.region?.startLine == 27 }.firstOrNull()
        assertNotNull(literal2)

        val literal3 =
            tu.allChildren<Literal<*>> { it.location?.region?.startLine == 28 }.firstOrNull()
        assertNotNull(literal3)

        val literal4 =
            tu.allChildren<Literal<*>> { it.location?.region?.startLine == 29 }.firstOrNull()
        assertNotNull(literal4)

        val literal5 =
            tu.allChildren<Literal<*>> { it.location?.region?.startLine == 20 }.firstOrNull()
        assertNotNull(literal5)

        val literal6 =
            tu.allChildren<Literal<*>> { it.location?.region?.startLine == 15 }.firstOrNull()
        assertNotNull(literal6)

        val literal7 =
            tu.allChildren<Literal<*>> { it.location?.region?.startLine == 16 }.firstOrNull()
        assertNotNull(literal7)

        // References
        val ctxLine43 =
            tu.allChildren<Reference> { it.location?.region?.startLine == 43 }.firstOrNull()
        assertNotNull(ctxLine43)

        // Initial State
        println(ctxLine43)

        // Function Summary for mbedtls_ssl_read
        assertEquals(1, FSread.size)
        assertEquals(5, FSread.entries.firstOrNull()?.value?.size)
        assertTrue(
            FSread.entries.firstOrNull()?.value?.any {
                it.srcNode == literal1 && it.subAccessName == "i"
            } == true
        )
        assertTrue(
            FSread.entries.firstOrNull()?.value?.any {
                it.srcNode == literal2 && it.subAccessName == "j"
            } == true
        )
        assertTrue(
            FSread.entries.firstOrNull()?.value?.any {
                it.srcNode == literal3 && it.subAccessName == "k"
            } == true
        )
        assertTrue(
            FSread.entries.firstOrNull()?.value?.any {
                it.srcNode == literal4 && it.subAccessName == "session.l"
            } == true
        )
        assertTrue(
            FSread.entries.firstOrNull()?.value?.any {
                it.srcNode is UnknownMemoryValue && it.subAccessName == "session"
            } == true
        )

        // Function Summary for inner_renegotiate
        assertEquals(1, FSinnerrenegotiate.size)
        assertEquals(
            2,
            FSinnerrenegotiate.entries
                .firstOrNull()
                ?.value
                ?.filter { !it.shortFunctionSummary }
                ?.size,
        )
        assertTrue(
            FSinnerrenegotiate.entries
                .firstOrNull()
                ?.value
                ?.filter { !it.shortFunctionSummary }
                ?.any { it.srcNode == literal6 && it.subAccessName == "j" } == true
        )
        assertTrue(
            FSinnerrenegotiate.entries
                .firstOrNull()
                ?.value
                ?.filter { !it.shortFunctionSummary }
                ?.any { it.srcNode == literal7 && it.subAccessName == "session.l" } == true
        )

        // Function Summary for renegotiate
        assertEquals(1, FSrenegotiate.size)
        assertEquals(3, FSrenegotiate.entries.firstOrNull()?.value?.size)
        assertTrue(
            FSrenegotiate.entries.firstOrNull()?.value?.any {
                it.srcNode == literal5 && it.subAccessName == "i"
            } == true
        )
        assertTrue(
            FSrenegotiate.entries.firstOrNull()?.value?.any {
                it.srcNode == literal6 && it.subAccessName == "j"
            } == true
        )
        assertTrue(
            FSrenegotiate.entries.firstOrNull()?.value?.any {
                it.srcNode == literal7 && it.subAccessName == "session.l"
            } == true
        )
    }

    @Test
    fun testPointerToPointerFunctionSummaries() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
            }
        assertNotNull(tu)

        // FunctionSummaries
        val changepointerFS =
            tu.functions.first { it.name.localName == "changepointer" }.functionSummary

        // References
        val p2pLine262 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 262 &&
                        it.location?.region?.startColumn == 101 &&
                        it.name.localName == "p2p"
                }
                .first()
        assertNotNull(p2pLine262)
        val p2pLine264 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 264 &&
                        it.location?.region?.startColumn == 101 &&
                        it.name.localName == "p2p"
                }
                .first()
        assertNotNull(p2pLine264)

        // Declarations
        val oldvalDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 256 }.first()
        assertNotNull(oldvalDecl)
        val newvalDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 259 }.first()
        assertNotNull(newvalDecl)
        val p_oldvalDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 257 }.first()
        assertNotNull(p_oldvalDecl)
        val p_newvalDecl =
            tu.allChildren<Declaration> { it.location?.region?.startLine == 260 }.first()
        assertNotNull(p_newvalDecl)
        val p2pDecl = tu.allChildren<Declaration> { it.location?.region?.startLine == 258 }.first()
        assertNotNull(p2pDecl)

        // Literals
        val literal1 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 256 }.first()
        assertNotNull(literal1)
        val literal2 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 259 }.first()
        assertNotNull(literal2)

        // Test the FS of changepointer
        assertEquals(
            2,
            changepointerFS.entries
                .first { it.key.name.localName == "p" }
                .value
                .filter { !it.shortFunctionSummary }
                .size,
        )
        // TODO: it should be one less, we have two entries from the newp to p with different depths
        assertEquals(
            5,
            changepointerFS.entries
                .first { it.key.name.localName == "p" }
                .value
                .filter { it.shortFunctionSummary }
                .size,
        )
        assertEquals(
            1,
            changepointerFS.entries
                .first { it.key.name.localName == "newp" }
                .value
                .filter { !it.shortFunctionSummary }
                .size,
        )
        assertEquals(
            2,
            changepointerFS.entries
                .first { it.key.name.localName == "newp" }
                .value
                .filter { it.shortFunctionSummary }
                .size,
        )

        // p2p before the call in Line 262
        assertEquals(3, p2pLine262.prevDFGEdges.size)
        assertEquals(1, p2pLine262.prevFullDFG.size)
        assertEquals(
            p_oldvalDecl.memoryAddresses.singleOrNull(),
            p2pLine262.prevFullDFG.first() as MemoryAddress?,
        )
        assertEquals(
            1,
            p2pLine262.prevDFGEdges
                .filter {
                    it.granularity is PointerDataflowGranularity &&
                        (it.granularity as PointerDataflowGranularity).pointerTarget ==
                            PointerAccess.currentDerefValue
                }
                .size,
        )
        assertEquals(
            oldvalDecl.memoryAddresses.singleOrNull(),
            p2pLine262.prevDFGEdges
                .first {
                    it.granularity is PointerDataflowGranularity &&
                        (it.granularity as PointerDataflowGranularity).pointerTarget ==
                            PointerAccess.currentDerefValue
                }
                .start as MemoryAddress?,
        )
        assertEquals(
            1,
            p2pLine262.prevDFGEdges
                .filter {
                    it.granularity is PointerDataflowGranularity &&
                        (it.granularity as PointerDataflowGranularity).pointerTarget ==
                            PointerAccess.currentDerefDerefValue
                }
                .size,
        )
        assertEquals(
            literal1,
            p2pLine262.prevDFGEdges
                .first {
                    it.granularity is PointerDataflowGranularity &&
                        (it.granularity as PointerDataflowGranularity).pointerTarget ==
                            PointerAccess.currentDerefDerefValue
                }
                .start,
        )

        // Test the result on p2p
        assertEquals(3, p2pLine264.prevDFGEdges.size)
        assertEquals(1, p2pLine264.prevFullDFG.size)
        assertEquals(
            p_oldvalDecl.memoryAddresses.singleOrNull(),
            p2pLine264.prevFullDFG.first() as MemoryAddress?,
        )
        assertEquals(
            1,
            p2pLine264.prevDFGEdges
                .filter {
                    it.granularity is PointerDataflowGranularity &&
                        (it.granularity as PointerDataflowGranularity).pointerTarget ==
                            PointerAccess.currentDerefValue
                }
                .size,
        )
        assertEquals(
            newvalDecl.memoryAddresses.singleOrNull(),
            p2pLine264.prevDFGEdges
                .first {
                    it.granularity is PointerDataflowGranularity &&
                        (it.granularity as PointerDataflowGranularity).pointerTarget ==
                            PointerAccess.currentDerefValue
                }
                .start as MemoryAddress?,
        )
        assertEquals(
            1,
            p2pLine264.prevDFGEdges
                .filter {
                    it.granularity is PointerDataflowGranularity &&
                        (it.granularity as PointerDataflowGranularity).pointerTarget ==
                            PointerAccess.currentDerefDerefValue
                }
                .size,
        )
        assertEquals(
            literal2,
            p2pLine264.prevDFGEdges
                .first {
                    it.granularity is PointerDataflowGranularity &&
                        (it.granularity as PointerDataflowGranularity).pointerTarget ==
                            PointerAccess.currentDerefDerefValue
                }
                .start,
        )
    }

    @Test
    fun testGlobalVariables() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
                it.configurePass<PointsToPass>(PointsToPass.Configuration(addressLength = 64))
            }
        assertNotNull(tu)

        // Declarations
        val keyDecl = tu.allChildren<Declaration> { it.location?.region?.startLine == 267 }.first()
        assertNotNull(keyDecl)

        // PointerReferences
        val keyPointerRefLine314 =
            tu.allChildren<PointerReference> {
                    it.name.localName == "key" && it.location?.region?.startLine == 314
                }
                .first()
        assertNotNull(keyPointerRefLine314)

        val keyPointerRefLine318 =
            tu.allChildren<PointerReference> {
                    it.name.localName == "key" && it.location?.region?.startLine == 318
                }
                .first()
        assertNotNull(keyPointerRefLine318)

        val keyPrevDFGs =
            tu.allChildren<Reference> {
                    it !is PointerDereference &&
                        it !is PointerReference &&
                        it.name.localName == "key"
                }
                .flatMap { it.prevDFG }
                .toIdentitySet()

        // Ensure that all key-references point to keyDecl as prevDFG
        assertEquals(2, keyPrevDFGs.size)
        assertEquals(2, keyPrevDFGs.filterIsInstance<UnknownMemoryValue>().size)

        assertTrue(keyPrevDFGs.any { it.name.localName == "key" })
        assertTrue(keyPrevDFGs.any { it.name.localName == "sgx_get_key.secret" })

        assertTrue(
            (keyPrevDFGs.first { it.name.localName == "key" } as? UnknownMemoryValue)?.isGlobal
                ?: false
        )
        assertFalse(
            (keyPrevDFGs.first { it.name.localName == "sgx_get_key.secret" } as? UnknownMemoryValue)
                ?.isGlobal ?: true
        )
    }

    @Test
    fun testCallingContexts() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
            }
        assertNotNull(tu)

        // CallExpressions
        val ceLine380 =
            tu.allChildren<CallExpression> { it.location?.region?.startLine == 380 }.first()
        assertNotNull(ceLine380)

        val ceLine384 =
            tu.allChildren<CallExpression> { it.location?.region?.startLine == 384 }.first()
        assertNotNull(ceLine384)

        val ceLine386 =
            tu.allChildren<CallExpression> { it.location?.region?.startLine == 386 }.first()
        assertNotNull(ceLine386)

        val ceLine390 =
            tu.allChildren<CallExpression> { it.location?.region?.startLine == 390 }.first()
        assertNotNull(ceLine390)

        // arguments
        val iArgLine380 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 380 && it.location?.region?.startColumn == 11
                }
                .first()
        assertNotNull(iArgLine380)

        val iArgLine384 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 384 && it.location?.region?.startColumn == 11
                }
                .first()
        assertNotNull(iArgLine384)

        val pArgLine386 =
            tu.allChildren<Reference> { it.location?.region?.startLine == 386 }.first()
        assertNotNull(pArgLine386)

        // BinaryOperators
        val binOpLine207 =
            tu.allChildren<BinaryOperator> { it.location?.region?.startLine == 207 }.first()
        assertNotNull(binOpLine207)

        val binOpLine212 =
            tu.allChildren<BinaryOperator> { it.location?.region?.startLine == 212 }.first()
        assertNotNull(binOpLine212)

        // References
        val iRefLeftLine380 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 380 && it.location?.region?.startColumn == 5
                }
                .first()
        assertNotNull(iRefLeftLine380)

        val iRefLeftLine384 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 384 && it.location?.region?.startColumn == 5
                }
                .first()
        assertNotNull(iRefLeftLine384)

        val jLine388 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 388 && it.name.localName == "j"
                }
                .first()
        assertNotNull(jLine388)

        val pDerefLine388 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 388 && it.name.localName == "p"
                }
                .first()
        assertNotNull(pDerefLine388)

        val jLine394 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 394 && it.name.localName == "j"
                }
                .first()
        assertNotNull(jLine394)

        val pDerefLine394 =
            tu.allChildren<PointerDereference> {
                    it.location?.region?.startLine == 394 && it.name.localName == "p"
                }
                .first()
        assertNotNull(pDerefLine394)

        // FunctionDeclarations
        val incpFD = tu.functions.firstOrNull { it.name.localName == "incp" }
        assertNotNull(incpFD)

        // ParameterMemoryValues
        val incpDerefValue = incpFD.parameters.first().memoryValue?.memoryValues?.singleOrNull()
        assertNotNull(incpDerefValue)

        // Literals
        val literal0 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 376 }.first()
        assertNotNull(literal0)

        val literal1 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 377 }.first()
        assertNotNull(literal1)

        val literal2 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 392 }.first()
        assertNotNull(literal2)

        // CallExpression in Line 380
        assertEquals(1, iArgLine380.nextDFGEdges.size)
        assertTrue(iArgLine380.nextDFGEdges.first().end is ParameterMemoryValue)
        assertLocalName("value", iArgLine380.nextDFGEdges.first().end)
        assertEquals(
            ceLine380,
            ((iArgLine380.nextDFGEdges.first() as ContextSensitiveDataflow).callingContext
                    as CallingContextIn)
                .call,
        )

        assertEquals(1, ceLine380.prevDFGEdges.size)
        assertEquals(
            ceLine380,
            ((ceLine380.prevDFGEdges.first() as ContextSensitiveDataflow).callingContext
                    as CallingContextOut)
                .call,
        )

        // CallExpression in Line 384
        assertEquals(1, iArgLine384.nextDFGEdges.size)
        assertTrue(iArgLine384.nextDFGEdges.first().end is ParameterMemoryValue)
        assertLocalName("value", iArgLine384.nextDFGEdges.first().end)
        assertEquals(
            ceLine384,
            ((iArgLine384.nextDFGEdges.first() as ContextSensitiveDataflow).callingContext
                    as CallingContextIn)
                .call,
        )

        assertEquals(1, ceLine384.prevDFGEdges.size)
        assertEquals(
            ceLine384,
            ((ceLine384.prevDFGEdges.first() as ContextSensitiveDataflow).callingContext
                    as CallingContextOut)
                .call,
        )

        // CallExpression in Line 386
        assertEquals(1, pArgLine386.nextDFGEdges.filter { !it.functionSummary }.size)
        assertEquals(
            ceLine386,
            ((pArgLine386.nextDFGEdges.first() as ContextSensitiveDataflow).callingContext
                    as CallingContextIn)
                .call,
        )
        assertEquals(1, incpDerefValue.prevDFGEdges.filter { it.start == literal1 }.size)
        assertEquals(
            ceLine386,
            ((incpDerefValue.prevDFGEdges.first { it.start == literal1 }
                        as ContextSensitiveDataflow)
                    .callingContext as CallingContextIn)
                .call,
        )

        // print Line 388
        assertEquals(1, jLine388.prevDFGEdges.size)
        assertEquals(
            ceLine386,
            ((jLine388.prevDFGEdges.first() as ContextSensitiveDataflow).callingContext
                    as CallingContextOut)
                .call,
        )
        assertEquals(1, pDerefLine388.prevDFGEdges.size)
        assertEquals(
            ceLine386,
            ((pDerefLine388.prevDFGEdges.first() as ContextSensitiveDataflow).callingContext
                    as CallingContextOut)
                .call,
        )

        // print Line 394
        assertEquals(1, jLine394.prevDFGEdges.size)
        assertTrue(jLine394.prevDFGEdges.first() !is ContextSensitiveDataflow)
        assertEquals(literal2, jLine394.prevDFG.first())
        assertEquals(1, pDerefLine394.prevDFGEdges.size)
        assertTrue(pDerefLine394.prevDFGEdges.first() !is ContextSensitiveDataflow)
        assertEquals(literal2, pDerefLine394.prevDFG.first())
    }

    @Test
    fun testUnaryOps() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
            }
        assertNotNull(tu)

        // References
        val iLine405 =
            tu.allChildren<Reference> {
                    it.name.localName == "i" && it.location?.region?.startLine == 405
                }
                .first()
        assertNotNull(iLine405)

        val iLine409 =
            tu.allChildren<Reference> {
                    it.name.localName == "i" && it.location?.region?.startLine == 409
                }
                .first()
        assertNotNull(iLine409)

        // UnaryOperators
        val uOPLine403 =
            tu.allChildren<UnaryOperator> { it.location?.region?.startLine == 403 }.first()
        assertNotNull(uOPLine403)

        val uOPLine407 =
            tu.allChildren<UnaryOperator> { it.location?.region?.startLine == 407 }.first()
        assertNotNull(uOPLine407)

        // Literals
        val literal1 = tu.allChildren<Literal<*>> { it.location?.region?.startLine == 400 }.first()
        assertNotNull(literal1)

        // printf in Line 405
        assertEquals(2, iLine405.prevDFG.size)
        assertEquals(mutableSetOf<Node>(literal1, uOPLine403), iLine405.prevDFG)

        // printf in Line 409
        assertEquals(1, iLine409.prevDFG.size)
        assertEquals(mutableSetOf<Node>(uOPLine407.input), iLine409.prevDFG)
    }

    @Test
    fun testShortFS() {
        val file = File("src/test/resources/pointsto.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
            }
        assertNotNull(tu)

        // Function
        val func = tu.functions["testShortFS"]
        assertNotNull(func)

        // Literals
        val literal0 = func.literals.first { it.value as? Int == 0 }
        assertNotNull(literal0)

        val literal3 = tu.functions["set"].literals.first { it.value as? Int == 3 }
        assertNotNull(literal3)

        // Declarations
        val iDecl = func.variables["i"]
        assertNotNull(iDecl)

        // References
        val iLine424 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 424 && it.name.localName == "i"
                }
                .first()
        assertNotNull(iLine424)

        val jLine424 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 424 && it.name.localName == "j"
                }
                .first()
        assertNotNull(jLine424)

        val iLine428 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 428 && it.name.localName == "i"
                }
                .first()
        assertNotNull(iLine428)

        val iLine432 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 432 && it.name.localName == "i"
                }
                .first()
        assertNotNull(iLine432)

        val iLine436 =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 436 && it.name.localName == "i"
                }
                .first()
        assertNotNull(iLine436)

        // UnaryOperator
        val binOP = tu.allChildren<BinaryOperator> { it.location?.region?.startLine == 413 }.first()
        assertNotNull(binOP)

        // CallExpressions
        val addCE1 = func.calls("add")[0]
        assertNotNull(addCE1)

        val addCE2 = func.calls("add")[1]
        assertNotNull(addCE2)

        val addCE3 = func.calls("add")[2]
        assertNotNull(addCE3)

        val setCE = func.calls("set")[0]
        assertNotNull(setCE)

        // Line 422
        assertEquals(1, addCE1.arguments[0].prevFullDFG.size)
        assertTrue(addCE1.arguments[0].prevFullDFG.contains(iDecl.memoryAddresses.single()))
        assertEquals(2, addCE1.arguments[0].prevFunctionSummaryDFG.size)
        assertTrue(addCE1.arguments[0].prevFunctionSummaryDFG.contains(literal0))
        assertTrue(addCE1.arguments[0].prevFunctionSummaryDFG.contains(addCE1.arguments[0]))

        // Line 424
        assertEquals(1, iLine424.prevFullDFG.size)
        assertTrue(iLine424.prevFullDFG.contains(binOP))
        assertEquals(2, iLine424.prevFunctionSummaryDFG.size)
        assertTrue(iLine424.prevFunctionSummaryDFG.contains(addCE1.arguments[0]))
        assertEquals(1, addCE1.invokes.size)
        assertTrue(iLine424.prevFunctionSummaryDFG.contains(addCE1.invokes.first()))

        // Line 426
        assertEquals(1, addCE2.arguments[0].prevFullDFG.size)
        assertTrue(addCE2.arguments[0].prevFullDFG.contains(iDecl.memoryAddresses.single()))
        assertEquals(3, addCE2.arguments[0].prevFunctionSummaryDFG.size)
        assertTrue(addCE2.arguments[0].prevFunctionSummaryDFG.contains(addCE2.arguments[0]))
        assertTrue(addCE2.arguments[0].prevFunctionSummaryDFG.contains(addCE2.arguments[1]))
        assertEquals(1, addCE2.invokes.size)
        assertTrue(addCE2.arguments[0].prevFunctionSummaryDFG.contains(addCE2.invokes.first()))

        // Line 428
        assertEquals(1, iLine428.prevFullDFG.size)
        assertTrue(iLine428.prevFullDFG.contains(binOP))
        assertEquals(2, iLine428.prevFunctionSummaryDFG.size)
        assertTrue(iLine428.prevFunctionSummaryDFG.contains(addCE2.arguments[0]))
        assertTrue(iLine428.prevFunctionSummaryDFG.contains(addCE2.invokes.first()))

        // Line 430
        assertEquals(1, addCE3.arguments[0].prevFullDFG.size)
        assertTrue(addCE3.arguments[0].prevFullDFG.contains(iDecl.memoryAddresses.single()))
        assertEquals(3, addCE3.arguments[0].prevFunctionSummaryDFG.size)
        assertTrue(addCE3.arguments[0].prevFunctionSummaryDFG.contains(addCE3.arguments[0]))
        assertTrue(addCE3.arguments[0].prevFunctionSummaryDFG.contains(addCE3.arguments[1]))
        assertEquals(1, addCE3.invokes.size)
        assertTrue(addCE3.arguments[0].prevFunctionSummaryDFG.contains(addCE3.invokes.first()))

        // Line 432
        assertEquals(1, iLine432.prevFullDFG.size)
        assertTrue(iLine432.prevFullDFG.contains(binOP))
        assertEquals(2, iLine432.prevFunctionSummaryDFG.size)
        assertTrue(iLine432.prevFunctionSummaryDFG.contains(addCE3.arguments[0]))
        assertTrue(iLine432.prevFunctionSummaryDFG.contains(addCE3.invokes.first()))

        // Line 434
        assertEquals(1, setCE.arguments[0].prevFullDFG.size)
        assertTrue(setCE.arguments[0].prevFullDFG.contains(iDecl.memoryAddresses.single()))
        assertEquals(0, setCE.arguments[0].prevFunctionSummaryDFG.size)

        // Line 436
        assertEquals(1, iLine436.prevFullDFG.size)
        assertTrue(iLine436.prevFullDFG.contains(literal3))
        assertEquals(1, iLine436.prevFunctionSummaryDFG.size)
        assertEquals(1, setCE.invokes.size)
        assertTrue(iLine436.prevFunctionSummaryDFG.contains(setCE.invokes.first()))
    }

    @Test
    fun testFoo() {
        val file =
            File(
                "/home/moe/projects/cpg-attestation/sgx-examples/write_secret_to_outside/Enclave/Enclave.cpp"
            )
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<PointsToPass>()
                it.registerFunctionSummaries(File("src/test/resources/hardcodedDFGedges.yml"))
            }
        assertNotNull(tu)
    }
}
