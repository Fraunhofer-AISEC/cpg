/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguageWithColon
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.FieldDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.PartialDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.test.*
import de.fraunhofer.aisec.cpg.test.GraphExamples
import kotlin.test.*

class ControlFlowSensitiveDFGPassTest {
    @Test
    fun testConfiguration() {
        val result = getForEachTest()
        assertNotNull(result)
    }

    /**
     * This test asserts the dataflow that occurs with a simple field access using a [MemberAccess].
     */
    @Test
    fun testSimpleFieldDataflow() {
        val test = GraphExamples.getSimpleFieldDataflow()
        assertNotNull(test)

        val field1 = test.records["myStruct"].fields["field1"]
        assertNotNull(field1)

        val i = test.functions["doSomething"].parameters["i"]
        assertNotNull(i)

        val main = test.functions["main"]
        assertNotNull(main)

        val s1 = main.variables["s1"]
        assertNotNull(s1)
        val s2 = main.variables["s2"]
        assertNotNull(s2)

        // Just some debugging output
        println(s1.printDFG())

        // Gather all calls of doSomething (in order: line 11, 15, 16)
        val calls = main.calls("doSomething")
        assertEquals(listOf(11, 15, 16), calls.map { it.location?.region?.startLine })

        with(s1) {
            // The DFG from the variable declaration of s1 should go to base of the member
            // expression s1.field1 of the first doSomething call (line 11) as well as the s1.field1
            // in the assignment (line 13) and the base of the second doSomething call (line 15)
            val next = this.nextDFGEdges.sortedBy { it.end.location?.region?.startLine }
            assertEquals(3, next.size)

            val baseOfMemberRead11 = assertNotNull(next.firstOrNull()?.end)
            assertEquals(11, baseOfMemberRead11.location?.region?.startLine)
            assertIs<Reference>(baseOfMemberRead11)
            assertIs<MemberAccess>(baseOfMemberRead11.astParent)
            assertEquals(AccessValues.READ, baseOfMemberRead11.access)

            val baseOfMemberWrite13 = assertNotNull(next.getOrNull(1)?.end)
            assertEquals(13, baseOfMemberWrite13.location?.region?.startLine)
            assertIs<Reference>(baseOfMemberWrite13)
            assertIs<MemberAccess>(baseOfMemberWrite13.astParent)
            assertEquals(
                AccessValues.READ,
                baseOfMemberWrite13.access,
            ) // the base of the member write is still a read, since the READ is only applying to
            // "FULL" flows

            // In the first case `doSomething(s1.field1)`, which is a READ, we should have a partial
            // flow from the base to the member expression. Since this is a READ, the "full" flow
            // will stop here
            with(baseOfMemberRead11) {
                val nextEdges = this.nextDFGEdges.toList()
                val partialFlow = assertNotNull(nextEdges.singleOrNull())
                val granularity = assertIs<FieldDataflowGranularity>(partialFlow.granularity)
                assertSame(field1, granularity.partialTarget)

                // The target of this partial flow should be our member expression in line 11
                val me = assertIs<MemberAccess>(partialFlow.end)
                assertEquals(11, me.location?.region?.startLine)
                // Which in turn should only have an incoming FULL DFG edge from the field
                // declaration as its "initializer"
                assertEquals(
                    mutableListOf<Node>(field1),
                    me.prevDFGEdges
                        .filter { it.granularity is FullDataflowGranularity }
                        .map(Dataflow::start),
                )
                // ... and only have one outgoing Full DFG edge to the "i" parameter of doSomething
                assertEquals(listOf<Node>(i), me.nextFullDFG)
                // ... plus the shortFSEdge back to the call
                assertEquals(listOf<Node>(calls[0]), me.nextFunctionSummaryDFG)
            }

            // Back to the second case (the member write).
            with(baseOfMemberWrite13) {
                // There should be an incoming PARTIAL dfg edge to this base from the member
                // expression which writes the field. It should also be the only partial DFG edge.
                // The partial target of this edge is our field declaration.
                // And the originating node should be our member expression that does the member
                // write in line 13
                val memberWrite13 = assertSinglePartialEdgeTo<MemberAccess>(this, field1)
                assertEquals(13, memberWrite13.location?.region?.startLine)

                // The sole incoming DFG edge to the memberWrite member expression should be a
                // literal with the value 1
                assertLiteralValue(1, memberWrite13.prevDFG.singleOrNull() as? Literal<*>)

                // To double-check, the baseOfMemberWrite node should only have two incoming DFG
                // edges at all: the partial DFG from the member write and the full DFG from its
                // declaration
                assertEquals(mutableSetOf<Node>(memberWrite13, s1), baseOfMemberWrite13.prevDFG)

                // Even though this was a partial write, we consider this to be the last point of
                // write for s1 in terms of control-flow sensitivity. Therefore, the next read
                // reference to s1 in line 15, which is another `doSomething(s1.field1)` will
                // originate from this point here, instead of the variable declaration.
                val baseOfMemberRead15 =
                    assertIs<Reference>(baseOfMemberWrite13.nextDFG.singleOrNull())

                // Once again, we have a partial flow to the member expression, which reads the
                // field in line 15
                val memberRead15 =
                    assertSinglePartialEdgeFrom<MemberAccess>(
                        baseOfMemberRead15,
                        partialTarget = field1,
                    )
                assertEquals(15, memberRead15.location?.region?.startLine)

                // This finally flows to "i"
                assertEquals(listOf<Node>(i), memberRead15.nextFullDFG)
                // And the shortFS directly to the CallExpression
                assertEquals(calls[1], memberRead15.nextFunctionSummaryDFG.singleOrNull())

                // We should also have a full flow between the member write and the member read.
                // This is a FULL flow because both occasions are only referring to the field.
                assertEquals(
                    listOf<Node>(memberWrite13),
                    memberRead15.prevDFGEdges
                        .filter { it.granularity is FullDataflowGranularity }
                        .map(Dataflow::start),
                )
            }
        }

        // The DFG from the variable declaration of s2 should go the base of s2.field1 of the
        // assignment (line 14) and to the base of argument of the call doSomething (line 16)
        with(s2) {
            val next = this.nextDFGEdges.sortedBy { it.end.location?.region?.startLine }
            assertEquals(2, next.size)

            val baseOfMemberWrite14 = assertNotNull(next.getOrNull(0)?.end)
            assertEquals(14, baseOfMemberWrite14.location?.region?.startLine)
            assertIs<Reference>(baseOfMemberWrite14)
            assertIs<MemberAccess>(baseOfMemberWrite14.astParent)
            assertEquals(AccessValues.READ, baseOfMemberWrite14.access)

            val baseOfArg16 = assertNotNull(next.getOrNull(1)?.end)
            assertEquals(16, baseOfArg16.location?.region?.startLine)
            assertIs<Reference>(baseOfArg16)
            assertIs<MemberAccess>(baseOfArg16.astParent)
            assertEquals(AccessValues.READ, baseOfMemberWrite14.access)

            assertEquals(
                mutableSetOf<Node>(baseOfMemberWrite14, baseOfArg16),
                next.mapTo(mutableSetOf()) { it.end },
            )

            // The rest should be the same as s1, so we can probably skip the rest of the asserts
        }
    }

    @Test
    fun testNestedFieldFlow() {
        val test = GraphExamples.getNestedFieldDataflow()
        assertNotNull(test)

        val `in` = test.records["outer"].fields["in"]
        assertNotNull(`in`)

        val field = test.records["inner"].fields["field"]
        assertNotNull(field)

        val i = test.functions["doSomething"].parameters["i"]
        assertNotNull(i)

        val main = test.functions["main"]
        assertNotNull(main)

        val o = main.variables["o"]
        assertNotNull(o)

        val meFields = main.memberExpressions("field")
        assertEquals(2, meFields.size)

        val meIn = main.memberExpressions("in")
        assertEquals(2, meIn.size)

        val refO = main.refs("o")
        assertEquals(2, refO.size)

        // There should be a full flow from the field to the argument (since this is the only one
        // that is fully written) and partial flows between the middle MemberAccess and the second
        // as well as for the ref
        assertFullEdgeBetween(meFields[0], meFields[1])
        assertEquals(
            meIn[1],
            meIn[0]
                .nextDFGEdges
                .singleOrNull {
                    ((it.granularity as? PartialDataflowGranularity<*>)?.partialTarget as? Field)
                        ?.name
                        ?.toString() == "inner.field"
                }
                ?.end,
        )
        assertEquals(
            refO[1],
            refO[0]
                .nextDFGEdges
                .singleOrNull {
                    ((it.granularity as? PartialDataflowGranularity<*>)?.partialTarget as? Field)
                        ?.name
                        ?.toString() == "outer.in"
                }
                ?.end,
        )

        // There should be a partial flow from the first '.field' which writes to the first '.in'
        assertPartialEdgeBetween(meFields[0], meIn[0], field)
        // And also a partial flow from '.in' to 'o'
        assertPartialEdgeBetween(meIn[0], refO[0], `in`)
    }

    private fun assertPartialEdgeBetween(from: Node, to: Node, partialTarget: Declaration?) {
        val edge =
            from.nextDFGEdges
                .filter { it.granularity is PartialDataflowGranularity<*> }
                .firstOrNull { it.end == to }
        assertNotNull(edge)
        assertEquals(
            partialTarget,
            (edge.granularity as? PartialDataflowGranularity<*>)?.partialTarget,
        )
    }

    private fun assertFullEdgeBetween(from: Node, to: Node) {
        assertContains(
            from.nextDFGEdges
                .filter { it.granularity is FullDataflowGranularity }
                .map(Dataflow::end),
            to,
        )
    }

    private inline fun <reified T : Node> assertSinglePartialEdgeFrom(
        from: Node,
        partialTarget: Declaration?,
    ): T {
        val partialEdge =
            assertNotNull(
                from.nextDFGEdges.singleOrNull { it.granularity is PartialDataflowGranularity<*> }
            )
        assertEquals(
            partialTarget,
            (partialEdge.granularity as? PartialDataflowGranularity<*>)?.partialTarget,
        )
        return assertIs<T>(partialEdge.end)
    }

    private inline fun <reified T : Node> assertSinglePartialEdgeTo(
        to: Node,
        partialTarget: Declaration?,
    ): T {
        val partialEdge =
            assertNotNull(
                to.prevDFGEdges.singleOrNull { it.granularity is PartialDataflowGranularity<*> }
            )
        assertEquals(
            partialTarget,
            (partialEdge.granularity as? PartialDataflowGranularity<*>)?.partialTarget,
        )
        return assertIs<T>(partialEdge.start)
    }

    fun getForEachTest() =
        testFrontend(
                TranslationConfiguration.builder()
                    .registerLanguage<TestLanguageWithColon>()
                    .defaultPasses()
                    .registerPass<ControlFlowSensitiveDFGPass>()
                    .configurePass<ControlFlowSensitiveDFGPass>(
                        ControlFlowSensitiveDFGPass.Configuration(maxComplexity = 0)
                    )
                    .build()
            )
            .build {
                val tu = newTranslationUnit("forEach.cpp")
                scopeManager.resetToGlobal(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val declStmt = newDeclarationStatement()
                            val i =
                                newVariable("i", objectType("int")).also {
                                    it.initializer = newLiteral(0, objectType("int"))
                                }
                            declStmt.declarations += i
                            scopeManager.addDeclaration(i)
                            block += declStmt

                            // Note: Fluent's "forEachStmt" never enters a new scope for the
                            // ForEach node itself, and "declare"/"call" inside its block attach to
                            // ForEach's own (generic) StatementHolder.statements -- NOT its
                            // dedicated .variable/.iterable properties, since those aren't used
                            // here. Faithfully reproduced.
                            val forEach = newForEach()
                            val loopVarDeclStmt = newDeclarationStatement()
                            val loopVar = newVariable("loopVar", objectType("string"))
                            loopVarDeclStmt.declarations += loopVar
                            scopeManager.addDeclaration(loopVar)
                            forEach.statements += loopVarDeclStmt
                            forEach.statements += newCall(newReference("magicFunction"))
                            forEach.statement =
                                newBlock(enterScope = true) { loopBody ->
                                    val printfCall = newCall(newReference("printf"))
                                    printfCall.addArgument(
                                        newLiteral("loop: \${}\n", objectType("string"))
                                    )
                                    printfCall.addArgument(newReference("loopVar"))
                                    loopBody += printfCall
                                }
                            block += forEach

                            val printfCall2 = newCall(newReference("printf"))
                            printfCall2.addArgument(newLiteral("1\n", objectType("string")))
                            block += printfCall2

                            val returnStmt = newReturn()
                            returnStmt.returnValue = newReference("i")
                            block += returnStmt
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
}
