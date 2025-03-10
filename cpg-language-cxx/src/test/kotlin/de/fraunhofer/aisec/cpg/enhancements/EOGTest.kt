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
package de.fraunhofer.aisec.cpg.enhancements

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.SearchModifier.UNIQUE
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.helpers.Util.Connect
import de.fraunhofer.aisec.cpg.processing.Visitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.test.*

/**
 * Tests correct path building for EOG focusing on loops, conditions, breaks ect.
 *
 * @author konrad.weiss@aisec.fraunhofer.de
 */
internal class EOGTest : BaseTest() {
    @Test
    @Throws(Exception::class)
    fun testCppIf() {
        testIf("src/test/resources/cfg/if.cpp", REFNODESTRINGCXX)
    }

    /**
     * Tests EOG building in the presence of if/else statements.
     *
     * @param relPath
     * @param refNodeString
     * - Exact string of reference nodes, do not change/insert nodes in the test file.
     */
    @Throws(Exception::class)
    fun testIf(relPath: String, refNodeString: String) {
        val nodes = translateToNodes(relPath)

        // All BinaryOperators (including If conditions) have only one successor
        val binops = nodes.filterIsInstance<BinaryOperator>()
        for (binop in binops) {
            val binopEOG = SubgraphWalker.getEOGPathEdges(binop)
            assertEquals(1, binopEOG.exits.size)
        }
        val ifs = nodes.filterIsInstance<IfStatement>()
        assertEquals(2, ifs.size)
        ifs.forEach { assertNotNull(it.thenStatement) }
        assertTrue(ifs.any { it.elseStatement == null } && ifs.any { it.elseStatement != null })
        val ifSimple = ifs[0]
        val ifBranched = ifs[1]
        val prints = nodes.filter { it.code == refNodeString }
        val ifEOG = SubgraphWalker.getEOGPathEdges(ifSimple)
        var conditionEOG = SubgraphWalker.getEOGPathEdges(ifSimple.condition)
        var thenEOG = SubgraphWalker.getEOGPathEdges(ifSimple.thenStatement)

        // IfStmt has 2 outgoing EOG edges (for true and false branch)
        assertEquals(2, ifEOG.exits.size)

        // Assert: Only single entry and exit NODE per block
        assertTrue(conditionEOG.entries.size == 1 && conditionEOG.exits.size == 1)
        assertTrue(thenEOG.entries.size == 1 && thenEOG.exits.size == 1)

        // Assert: Condition of simple if is preceded by print
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.ENTRIES,
                n = ifSimple.condition,
                refs = listOf(prints[0]),
            )
        )

        // Assert: All EOGs going into the then branch (=the 2nd print stmt) come from the
        // IfStatement
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.ENTRIES,
                n = ifSimple.thenStatement,
                cr = Connect.NODE,
                predicate = { it.branch == true },
                refs = listOf(ifSimple),
            )
        )
        // Assert: The EOGs going into the second print come either from the then branch or the
        // IfStatement
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = ifSimple,
                refs = listOf(prints[1]),
            )
        )
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = ifSimple,
                predicate = { it.branch == true },
                refs = listOf(ifSimple.thenStatement),
            )
        )
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = ifSimple.thenStatement,
                refs = listOf(prints[1]),
            )
        )
        conditionEOG = SubgraphWalker.getEOGPathEdges(ifBranched.condition)
        thenEOG = SubgraphWalker.getEOGPathEdges(ifBranched.thenStatement)
        val elseEOG = SubgraphWalker.getEOGPathEdges(ifBranched.elseStatement)

        // Assert: Only single entry and exit NODE per block
        assertTrue(conditionEOG.entries.size == 1 && conditionEOG.exits.size == 1)
        assertTrue(thenEOG.entries.size == 1 && thenEOG.exits.size == 1)
        assertTrue(elseEOG.entries.size == 1 && elseEOG.exits.size == 1)

        // Assert: Branched if is preceded by the second print
        assertTrue(
            Util.eogConnect(en = Util.Edge.ENTRIES, n = ifBranched, refs = listOf(prints[1]))
        )

        // IfStatement has exactly 2 outgoing EOGS: true (then) and false (else) branch
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = ifBranched,
                predicate = { it.branch == true },
                refs = listOf(ifBranched.thenStatement),
            )
        )
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = ifBranched,
                predicate = { it.branch == false },
                refs = listOf(ifBranched.elseStatement),
            )
        )
        val ifBranchedEOG = SubgraphWalker.getEOGPathEdges(ifBranched)
        assertEquals(2, ifBranchedEOG.exits.size)

        // Assert: EOG going into then branch comes from the condition branch
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.ENTRIES,
                n = ifBranched.thenStatement,
                cr = Connect.NODE,
                predicate = { it.branch == true },
                refs = listOf(ifBranched),
            )
        )

        // Assert: EOG going into else branch comes from the condition branch
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.ENTRIES,
                n = ifBranched.elseStatement,
                cr = Connect.NODE,
                predicate = { it.branch == false },
                refs = listOf(ifBranched),
            )
        )

        // Assert: EOG edges going into the third print either come from the then or else branch
        assertTrue(
            Util.eogConnect(
                cn = Connect.SUBTREE,
                en = Util.Edge.EXITS,
                n = ifBranched,
                refs = listOf(prints[2]),
            )
        )
        // Assert: EOG edges going into the branch root node either come from the then or else
        // branch
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.ENTRIES,
                n = ifBranched,
                refs = listOf(ifBranched.condition),
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testCPPFor() {
        val nodes = translateToNodes("src/test/resources/cfg/forloop.cpp")
        val prints = nodes.filter { it.code == REFNODESTRINGCXX }
        val fstat = nodes.filterIsInstance<ForStatement>()
        var fs = fstat[0]
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = prints[0],
                cr = Connect.SUBTREE,
                refs = listOf(fs),
            )
        )
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = prints[0],
                cr = Connect.SUBTREE,
                refs = listOfNotNull(fs.initializerStatement),
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = fs.initializerStatement,
                cr = Connect.SUBTREE,
                refs = listOfNotNull(fs.conditionDeclaration),
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = fs.conditionDeclaration,
                cr = Connect.NODE,
                refs = listOf(fs),
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = fs.statement,
                cr = Connect.SUBTREE,
                refs = listOfNotNull(fs.iterationStatement),
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = fs.iterationStatement,
                cr = Connect.SUBTREE,
                refs = listOfNotNull(fs.conditionDeclaration),
            )
        )
        assertTrue(
            Util.eogConnect(
                cn = Connect.SUBTREE,
                en = Util.Edge.EXITS,
                n = fs,
                cr = Connect.SUBTREE,
                predicate = { it.branch == false },
                refs = listOf(prints[1]),
            )
        )
        fs = fstat[1]
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = prints[1],
                cr = Connect.SUBTREE,
                refs = listOf(fs),
            )
        )
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = prints[1],
                cr = Connect.SUBTREE,
                refs = listOfNotNull(fs.initializerStatement),
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = fs.initializerStatement,
                cr = Connect.SUBTREE,
                refs = listOfNotNull(fs.condition),
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = fs.condition,
                cr = Connect.NODE,
                refs = listOf(fs),
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = fs.statement,
                cr = Connect.SUBTREE,
                refs = listOfNotNull(fs.iterationStatement),
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = fs.iterationStatement,
                cr = Connect.SUBTREE,
                refs = listOfNotNull(fs.condition),
            )
        )
        assertTrue(
            Util.eogConnect(
                cn = Connect.SUBTREE,
                en = Util.Edge.EXITS,
                n = fs,
                cr = Connect.SUBTREE,
                predicate = { it.branch == false },
                refs = listOf(prints[2]),
            )
        )
        fs = fstat[2]
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = prints[3],
                cr = Connect.SUBTREE,
                refs = listOf(fs),
            )
        )
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = prints[3],
                cr = Connect.SUBTREE,
                refs = listOfNotNull(fs.initializerStatement),
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = fs.initializerStatement,
                cr = Connect.SUBTREE,
                refs = listOfNotNull(fs.condition),
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = fs.condition,
                cr = Connect.NODE,
                refs = listOf(fs),
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = fs.statement,
                cr = Connect.SUBTREE,
                refs = listOfNotNull(fs.iterationStatement),
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = fs.iterationStatement,
                cr = Connect.SUBTREE,
                refs = listOfNotNull(fs.condition),
            )
        )
        assertTrue(
            Util.eogConnect(
                cn = Connect.SUBTREE,
                en = Util.Edge.EXITS,
                n = fs,
                cr = Connect.SUBTREE,
                predicate = { it.branch == false },
                refs = listOf(prints[4]),
            )
        )
    }

    /** Test function (not method) calls. */
    @Test
    @Throws(Exception::class)
    fun testCPPCallGraph() {
        val nodes = translateToNodes("src/test/resources/cg.cpp")
        val calls = nodes.filterIsInstance<CallExpression>()
        val functions = nodes.filterIsInstance<FunctionDeclaration>()
        val first = findByUniqueName(calls, "first")
        assertNotNull(first)

        var target = functions["first", UNIQUE]
        assertEquals(listOf(target), first.invokes)

        val second = calls["second", UNIQUE]
        assertNotNull(second)

        target = findByUniqueName(functions, "second")
        assertEquals(listOf(target), second.invokes)

        val third = findByUniqueName(calls, "third")
        assertNotNull(third)

        target = functions[{ it.name.localName == "third" && it.parameters.size == 2 }, UNIQUE]
        assertEquals(listOf(target), third.invokes)

        val fourth = findByUniqueName(calls, "fourth")
        assertNotNull(fourth)

        target = findByUniqueName(functions, "fourth")
        assertEquals(listOf(target), fourth.invokes)
    }

    @Test
    @Throws(Exception::class)
    fun testCppLoops() {
        testLoops("src/test/resources/cfg/loops.cpp", "printf(\"\\n\");")
    }

    /**
     * Tests EOG building in the presence of while-, do-while-loop statements.
     *
     * @param relPath
     * @param refNodeString
     * - Exact string of reference nodes, do not change/insert nodes in the test file.
     */
    @Throws(Exception::class)
    fun testLoops(relPath: String, refNodeString: String?) {
        val nodes = translateToNodes(relPath)
        val prints = nodes.filter { it.code == refNodeString }
        assertEquals(1, nodes.filterIsInstance<WhileStatement>().count())

        val wstat = nodes.filterIsInstance<WhileStatement>().firstOrNull()
        assertNotNull(wstat)

        var conditionEOG = SubgraphWalker.getEOGPathEdges(wstat.condition)
        var blockEOG = SubgraphWalker.getEOGPathEdges(wstat.statement)

        // Print EOG edges for debugging
        nodes[0].accept(
            Strategy::EOG_BACKWARD,
            object : Visitor<EvaluatedNode>() {
                override fun visit(t: EvaluatedNode) {
                    println(
                        PhysicalLocation.locationLink(t.location) +
                            " -> " +
                            PhysicalLocation.locationLink(t.location)
                    )
                }
            },
        )

        // Assert: Only single entry and exit NODE per block
        assertTrue(conditionEOG.entries.size == 1 && conditionEOG.exits.size == 1)
        assertTrue(blockEOG.entries.size == 1 && blockEOG.exits.size == 1)

        // Assert: While is preceded by a specific printf("\n")
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.ENTRIES,
                n = wstat,
                refs = listOfNotNull(wstat.condition),
            )
        )
        // Assert: Condition is preceded by print or block of the loop itself
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.ENTRIES,
                n = wstat.condition,
                refs = listOfNotNull(prints[0], wstat.statement),
            )
        )

        // Assert: All EOGs going into the loop branch come from the condition
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.ENTRIES,
                n = wstat.statement,
                cr = Connect.NODE,
                predicate = { it.branch == true },
                refs = listOf(wstat),
            )
        )

        // Assert: The EOGs going into the second print come either from the then branch or the
        // condition
        assertTrue(
            Util.eogConnect(
                cn = Connect.SUBTREE,
                en = Util.Edge.EXITS,
                n = wstat,
                predicate = { it.branch == false },
                refs = listOf(prints[1]),
            )
        )
        val dostat = nodes.filterIsInstance<DoStatement>().firstOrNull()
        assertNotNull(dostat)

        conditionEOG = SubgraphWalker.getEOGPathEdges(dostat.condition)
        blockEOG = SubgraphWalker.getEOGPathEdges(dostat.statement)

        // Assert: Only single entry and exit NODE per block
        assertTrue(conditionEOG.entries.size == 1 && conditionEOG.exits.size == 1)
        assertTrue(blockEOG.entries.size == 1 && blockEOG.exits.size == 1)

        // Assert: do is preceded by its condition
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.ENTRIES,
                n = dostat,
                refs = listOf(dostat.condition),
            )
        )
        // Assert: All EOGs going into the loop branch come from the condition
        assertTrue(
            Util.eogConnect(en = Util.Edge.EXITS, n = prints[1], refs = listOf(dostat.statement))
        )
        assertTrue(
            Util.eogConnect(
                q = Util.Quantifier.ANY,
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = dostat,
                predicate = { it.branch == true },
                refs = listOf(dostat.statement),
            )
        )

        // Assert: Condition is preceded by the loop branch
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.ENTRIES,
                n = dostat.condition,
                refs = listOf(dostat.statement),
            )
        )

        // Assert: The EOGs going into the second print come either from the then branch or the
        // condition
        assertTrue(
            Util.eogConnect(
                cn = Connect.SUBTREE,
                en = Util.Edge.EXITS,
                n = dostat,
                predicate = { it.branch == false },
                refs = listOf(prints[2]),
            )
        )
    }

    @Throws(Exception::class)
    fun testSwitch(relPath: String, refNodeString: String) {
        val nodes = translateToNodes(relPath)
        val functions =
            nodes.filterIsInstance<FunctionDeclaration>().filter { it !is ConstructorDeclaration }

        // main()
        var swch = functions[0].allChildren<SwitchStatement>()[0]
        var prints = Util.subnodesOfCode(functions[0], refNodeString)
        var cases = swch.allChildren<CaseStatement>()
        var defaults = swch.allChildren<DefaultStatement>()
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = prints[0],
                cr = Connect.SUBTREE,
                refs = listOf(swch.selector),
            )
        )
        assertTrue(
            Util.eogConnect(
                cn = Connect.SUBTREE,
                en = Util.Edge.EXITS,
                n = swch,
                refs = listOf(prints[1]),
            )
        )

        // Assert: Selector exits connect to the switch root node
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = swch,
                refs = cases + defaults,
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = swch.selector,
                cr = Connect.NODE,
                refs = listOf(swch),
            )
        )

        // Assert: Entries of case statements have one edge to switch root
        for (s in
            Stream.of(cases, defaults)
                .flatMap { n: List<Statement> -> n.stream() }
                .collect(Collectors.toList())) {
            assertTrue(
                Util.eogConnect(
                    q = Util.Quantifier.ANY,
                    en = Util.Edge.ENTRIES,
                    n = s,
                    cr = Connect.NODE,
                    refs = listOf(swch),
                )
            )
        }

        // Assert: All breaks inside of switch connect to the switch root node
        for (b in swch.allChildren<BreakStatement>()) assertTrue(
            Util.eogConnect(
                Util.Quantifier.ALL,
                Connect.SUBTREE,
                Util.Edge.EXITS,
                b,
                Connect.SUBTREE,
                refs = listOf(prints[1]),
            )
        )

        // whileswitch
        swch = functions[1].allChildren<SwitchStatement>()[0]
        prints = Util.subnodesOfCode(functions[1], refNodeString)
        cases = swch.allChildren<CaseStatement>()
        defaults = swch.allChildren<DefaultStatement>()
        val wstat = functions[1].allChildren<WhileStatement>().firstOrNull()
        assertNotNull(wstat)
        assertTrue(Util.eogConnect(en = Util.Edge.EXITS, n = prints[0], refs = listOf(wstat)))
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = wstat,
                refs = listOf(prints[2]),
            )
        )
        // Assert: switch root node exits connect to either case or default statements entries
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = swch,
                refs = cases + defaults,
            )
        )
        // Assert: Selector exits connect to the switch root node
        assertTrue(
            Util.eogConnect(
                cn = Connect.SUBTREE,
                en = Util.Edge.EXITS,
                n = swch.selector,
                cr = Connect.NODE,
                refs = listOf(swch),
            )
        )

        // switch-while
        swch = functions[2].allChildren<SwitchStatement>()[0]
        prints = Util.subnodesOfCode(functions[2], refNodeString)
        assertTrue(Util.eogConnect(en = Util.Edge.EXITS, n = prints[0], refs = listOf(swch)))
        assertTrue(Util.eogConnect(en = Util.Edge.EXITS, n = swch, refs = listOf(prints[2])))
        // Assert: Selector exits connect to either case or default statements entries
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = swch.selector,
                cr = Connect.NODE,
                refs = listOf(swch),
            )
        )
        swch = functions[1].allChildren<SwitchStatement>()[0]
        prints = Util.subnodesOfCode(functions[1], refNodeString)
        var breaks = swch.allChildren<BreakStatement>()

        // Assert: while-switch, all breaks inside the switch connect to the containing switch
        // unless it has a label which connects the break to the  while
        for (b in breaks) {
            if (b.label != null && b.label!!.isNotEmpty()) {
                assertTrue(
                    Util.eogConnect(
                        en = Util.Edge.EXITS,
                        n = b,
                        cr = Connect.SUBTREE,
                        refs = listOf(prints[2]),
                    )
                )
            } else {
                assertTrue(
                    Util.eogConnect(
                        en = Util.Edge.EXITS,
                        n = b,
                        cr = Connect.SUBTREE,
                        refs = listOf(prints[1]),
                    )
                )
            }
        }
        prints = Util.subnodesOfCode(functions[2], refNodeString)
        val whiles = functions[2].allChildren<WhileStatement>()[0]
        breaks = whiles.allChildren<BreakStatement>()

        // Assert: switch-while, all breaks inside the while connect to the containing while unless
        // it has a label which connects the break to the switch
        for (b in breaks) if (b.label != null && b.label!!.isNotEmpty())
            assertTrue(
                Util.eogConnect(
                    en = Util.Edge.EXITS,
                    n = b,
                    cr = Connect.SUBTREE,
                    refs = listOf(prints[2]),
                )
            )
        else
            assertTrue(
                Util.eogConnect(
                    en = Util.Edge.EXITS,
                    n = b,
                    cr = Connect.SUBTREE,
                    refs = listOf(prints[1]),
                )
            )
    }

    @Test
    @Throws(Exception::class)
    fun testCppSwitch() {
        testSwitch("src/test/resources/cfg/switch.cpp", REFNODESTRINGCXX)
    }

    @Test
    @Throws(Exception::class)
    fun testCppBreakContinue() {
        testBreakContinue("src/test/resources/cfg/break_continue.cpp", "printf(\"\\n\");")
    }

    /**
     * Tests EOG building in the presence of break-,continue- statements in loops.
     *
     * @param relPath
     * @param refNodeString
     * - Exact string of reference nodes, do not change/insert nodes in the test file.
     */
    @Throws(Exception::class)
    fun testBreakContinue(relPath: String, refNodeString: String) {
        val nodes = translateToNodes(relPath)
        val prints = nodes.filter { it.code == refNodeString }
        assertEquals(1, nodes.filterIsInstance<WhileStatement>().count())
        val breaks = nodes.filterIsInstance<BreakStatement>()
        val continues = nodes.filterIsInstance<ContinueStatement>()
        val wstat = nodes.filterIsInstance<WhileStatement>().firstOrNull()
        assertNotNull(wstat)
        var conditionEOG = SubgraphWalker.getEOGPathEdges(wstat.condition)
        var blockEOG = SubgraphWalker.getEOGPathEdges(wstat.statement)

        // Assert: Only single entry and two exit NODEs per block
        assertTrue(conditionEOG.entries.size == 1 && conditionEOG.exits.size == 1)
        assertTrue(blockEOG.entries.size == 1 && blockEOG.exits.size == 3)

        // Assert: Print is only followed by first nodes in condition
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = prints[0],
                refs = listOfNotNull(wstat.condition),
            )
        )

        // Assert: condition nodes are preceded by either continue, last nodes in block or last
        // nodes in print
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.ENTRIES,
                n = wstat.condition,
                refs = listOfNotNull(prints[0], wstat.statement),
            ) ||
                Util.eogConnect(
                    cn = Connect.NODE,
                    en = Util.Edge.EXITS,
                    n = continues[0],
                    refs = listOfNotNull(wstat.condition),
                )
        )

        // Assert: All EOGs going into the loop branch come from the Loop root node
        assertTrue(
            Util.eogConnect(en = Util.Edge.ENTRIES, n = wstat.statement, refs = listOf(wstat))
        )

        // Assert: The EOGs going into the second print come either from the while root or break
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = wstat,
                refs = listOf(prints[1]),
            ) ||
                Util.eogConnect(
                    cn = Connect.NODE,
                    en = Util.Edge.EXITS,
                    n = breaks[0],
                    refs = listOf(prints[1]),
                )
        )
        val dostat = nodes.filterIsInstance<DoStatement>().firstOrNull()
        assertNotNull(dostat)

        conditionEOG = SubgraphWalker.getEOGPathEdges(dostat.condition)
        blockEOG = SubgraphWalker.getEOGPathEdges(dostat.statement)

        // Assert: Only single entry and two exit NODEs per block
        assertTrue(conditionEOG.entries.size == 1 && conditionEOG.exits.size == 1)
        assertTrue(blockEOG.entries.size == 1 && blockEOG.exits.size == 3)

        // Assert: All EOGs going into the loop branch come from the condition
        assertTrue(
            Util.eogConnect(en = Util.Edge.EXITS, n = prints[1], refs = listOf(dostat.statement))
        )
        assertTrue(
            Util.eogConnect(
                Util.Quantifier.ANY,
                Connect.NODE,
                Util.Edge.EXITS,
                dostat,
                Connect.SUBTREE,
                refs = listOf(dostat.statement),
            )
        )
        assertTrue(
            Util.eogConnect(en = Util.Edge.EXITS, n = prints[1], refs = listOf(dostat.statement))
        )
        assertTrue(
            Util.eogConnect(
                Util.Quantifier.ANY,
                Connect.NODE,
                Util.Edge.EXITS,
                dostat,
                refs = listOf(dostat.statement),
            )
        )

        // Assert: Condition is preceded by the loop branch
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.ENTRIES,
                n = dostat.condition,
                refs = listOf(dostat.statement),
            ) ||
                Util.eogConnect(
                    cn = Connect.NODE,
                    en = Util.Edge.EXITS,
                    n = continues[1],
                    refs = listOf(dostat.condition),
                )
        )

        // Assert: The EOGs going into the third print come  from the loop root
        assertTrue(
            Util.eogConnect(
                cn = Connect.NODE,
                en = Util.Edge.EXITS,
                n = dostat,
                refs = listOf(prints[2]),
            )
        )
    }

    @Test
    fun testLambdaExpression() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .registerLanguage<CPPLanguage>()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda2"]
        assertNotNull(function)

        val lambdaVar = function.variables["this_is_a_lambda"]
        assertNotNull(lambdaVar)
        val lambda = lambdaVar.initializer as? LambdaExpression
        assertNotNull(lambda)

        // The "outer" EOG is assembled correctly.
        assertTrue(lambda in lambdaVar.nextEOG)
        val printFunctionCall = function.calls["print_function"]
        assertNotNull(printFunctionCall)
        assertTrue(printFunctionCall in lambdaVar.prevEOG)

        // The "inner" EOG is assembled correctly.
        val body = (lambda.function?.body as? Block)
        assertNotNull(body)
        assertEquals(1, lambda.function?.nextEOG?.size)
        assertEquals("std::cout", (lambda.function?.nextEOG?.get(0) as? Reference)?.name.toString())

        val cout = lambda.function?.nextEOG?.get(0) as? Reference
        assertNotNull(cout)
        assertEquals(1, cout.nextEOG.size)
        assertEquals("Hello ", (cout.nextEOG[0] as? Literal<*>)?.value.toString())

        val hello = cout.nextEOG[0] as? Literal<*>
        assertNotNull(hello)
        assertEquals(1, hello.nextEOG.size)
        assertEquals("<<", (hello.nextEOG[0] as? BinaryOperator)?.operatorCode)

        val binOpLeft = hello.nextEOG[0] as? BinaryOperator
        assertNotNull(binOpLeft)
        assertEquals(1, binOpLeft.nextEOG.size)
        assertEquals("number", (binOpLeft.nextEOG[0] as? Reference)?.name.toString())

        val number = binOpLeft.nextEOG[0] as? Reference
        assertNotNull(number)
        assertEquals(1, number.nextEOG.size)
        assertEquals("<<", (number.nextEOG[0] as? BinaryOperator)?.operatorCode)

        val binOpCenter = (number.nextEOG[0] as? BinaryOperator)
        assertNotNull(binOpCenter)
        assertEquals(1, binOpCenter.nextEOG.size)
        assertEquals("std::endl", (binOpCenter.nextEOG[0] as? Reference)?.name.toString())

        val endl = (binOpCenter.nextEOG[0] as? Reference)
        assertNotNull(endl)
        assertEquals(1, endl.nextEOG.size)
        assertEquals("<<", (endl.nextEOG[0] as? BinaryOperator)?.operatorCode)

        val binOpRight = (endl.nextEOG[0] as? BinaryOperator)
        assertNotNull(binOpRight)
        assertEquals(1, binOpRight.nextEOG.size)
        assertTrue(binOpRight.nextEOG.firstOrNull() is Block)

        assertEquals(0, (binOpRight.nextEOG.firstOrNull() as? Block)?.nextEOG?.size)
    }

    /**
     * Translates the given file into CPG and returns the graph. Extracted to reduce code duplicates
     *
     * @param path
     * - path for the file to test.
     */
    @Throws(Exception::class)
    private fun translateToNodes(path: String): List<AstNode> {
        val toTranslate = File(path)
        val topLevel = toTranslate.parentFile.toPath()
        val tu =
            analyzeAndGetFirstTU(listOf(toTranslate), topLevel, true) {
                it.registerLanguage<CPPLanguage>()
            }
        var nodes = SubgraphWalker.flattenAST(tu)
        // TODO: until explicitly added Return Statements are either removed again or code and
        // region set properly
        nodes = nodes.filter { it.code != null }
        return nodes
    }

    companion object {
        var REFNODESTRINGJAVA = "System.out.println();"
        var REFNODESTRINGCXX = "printf(\"\\n\");"
    }
}
