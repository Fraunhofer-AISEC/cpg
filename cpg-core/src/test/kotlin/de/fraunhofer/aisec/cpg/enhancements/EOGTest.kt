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

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.TestUtils.findByUniquePredicate
import de.fraunhofer.aisec.cpg.TestUtils.flattenIsInstance
import de.fraunhofer.aisec.cpg.TestUtils.flattenListIsInstance
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.helpers.Util.Connect
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.file.Path
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests correct path building for EOG focusing on loops, conditions, breaks ect.
 *
 * @author konrad.weiss@aisec.fraunhofer.de
 */
internal class EOGTest : BaseTest() {
    @Test
    @Throws(Exception::class)
    fun testJavaIf() {
        testIf("src/test/resources/cfg/If.java", REFNODESTRINGJAVA)
    }

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
        val binops = Util.filterCast(nodes, BinaryOperator::class.java)
        for (binop in binops) {
            val binopEOG = SubgraphWalker.getEOGPathEdges(binop)
            assertEquals(1, binopEOG.exits.size)
        }
        val ifs = Util.filterCast(nodes, IfStatement::class.java)
        assertEquals(2, ifs.size)
        ifs.forEach(Consumer { ifnode: IfStatement -> assertNotNull(ifnode.thenStatement) })
        assertTrue(
            ifs.stream().anyMatch { node: IfStatement -> node.elseStatement == null } &&
                ifs.stream().anyMatch { node: IfStatement -> node.elseStatement != null }
        )
        val ifSimple = ifs[0]
        val ifBranched = ifs[1]
        val prints =
            nodes
                .stream()
                .filter { node: Node -> node.code == refNodeString }
                .collect(Collectors.toList())
        val ifEOG = SubgraphWalker.getEOGPathEdges(ifSimple)
        var conditionEOG = SubgraphWalker.getEOGPathEdges(ifSimple.condition)
        var thenEOG = SubgraphWalker.getEOGPathEdges(ifSimple.thenStatement)

        // IfStmt has 2 outgoing EOG edges (for true and false branch)
        assertEquals(2, ifEOG.exits.size)

        // Assert: Only single entry and exit NODE per block
        assertTrue(conditionEOG.entries.size == 1 && conditionEOG.exits.size == 1)
        assertTrue(thenEOG.entries.size == 1 && thenEOG.exits.size == 1)

        // Assert: Condition of simple if is preceded by print
        assertTrue(Util.eogConnect(Util.Edge.ENTRIES, ifSimple.condition, prints[0]))

        // Assert: All EOGs going into the then branch (=the 2nd print stmt) come from the
        // IfStatement
        assertTrue(
            Util.eogConnect(Util.Edge.ENTRIES, ifSimple.thenStatement, Connect.NODE, ifSimple)
        )
        // Assert: The EOGs going into the second print come either from the then branch or the
        // IfStatement
        assertTrue(Util.eogConnect(Connect.NODE, Util.Edge.EXITS, ifSimple, prints[1]))
        assertTrue(Util.eogConnect(Connect.NODE, Util.Edge.EXITS, ifSimple, ifSimple.thenStatement))
        assertTrue(
            Util.eogConnect(Connect.NODE, Util.Edge.EXITS, ifSimple.thenStatement, prints[1])
        )
        conditionEOG = SubgraphWalker.getEOGPathEdges(ifBranched.condition)
        thenEOG = SubgraphWalker.getEOGPathEdges(ifBranched.thenStatement)
        val elseEOG = SubgraphWalker.getEOGPathEdges(ifBranched.elseStatement)

        // Assert: Only single entry and exit NODE per block
        assertTrue(conditionEOG.entries.size == 1 && conditionEOG.exits.size == 1)
        assertTrue(thenEOG.entries.size == 1 && thenEOG.exits.size == 1)
        assertTrue(elseEOG.entries.size == 1 && elseEOG.exits.size == 1)

        // Assert: Branched if is preceded by the second print
        assertTrue(Util.eogConnect(Util.Edge.ENTRIES, ifBranched, prints[1]))

        // IfStatement has exactly 2 outgoing EOGS: true (then) and false (else) branch
        assertTrue(
            Util.eogConnect(Connect.NODE, Util.Edge.EXITS, ifBranched, ifBranched.thenStatement)
        )
        assertTrue(
            Util.eogConnect(Connect.NODE, Util.Edge.EXITS, ifBranched, ifBranched.elseStatement)
        )
        val ifBranchedEOG = SubgraphWalker.getEOGPathEdges(ifBranched)
        assertEquals(2, ifBranchedEOG.exits.size)

        // Assert: EOG going into then branch comes from the condition branch
        assertTrue(
            Util.eogConnect(Util.Edge.ENTRIES, ifBranched.thenStatement, Connect.NODE, ifBranched)
        )

        // Assert: EOG going into else branch comes from the condition branch
        assertTrue(
            Util.eogConnect(Util.Edge.ENTRIES, ifBranched.elseStatement, Connect.NODE, ifBranched)
        )

        // Assert: EOG edges going into the third print either come from the then or else branch
        assertTrue(Util.eogConnect(Connect.SUBTREE, Util.Edge.EXITS, ifBranched, prints[2]))
        // Assert: EOG edges going into the branch root node either come from the then or else
        // branch
        assertTrue(
            Util.eogConnect(Connect.NODE, Util.Edge.ENTRIES, ifBranched, ifBranched.condition)
        )
    }

    @Test
    @Throws(Exception::class)
    fun testConditionShortCircuit() {
        val nodes = translateToNodes("src/test/resources/cfg/ShortCircuit.java")
        val binaryOperators =
            Util.filterCast(nodes, BinaryOperator::class.java)
                .stream()
                .filter { bo: BinaryOperator -> bo.operatorCode == "&&" || bo.operatorCode == "||" }
                .collect(Collectors.toList())
        for (bo in binaryOperators) {
            assertTrue(
                Util.eogConnect(
                    Util.Quantifier.ALL,
                    Connect.SUBTREE,
                    Util.Edge.EXITS,
                    bo.lhs,
                    Connect.SUBTREE,
                    bo.rhs
                )
            )
            assertTrue(
                Util.eogConnect(
                    Util.Quantifier.ALL,
                    Connect.SUBTREE,
                    Util.Edge.EXITS,
                    bo.lhs,
                    Connect.NODE,
                    bo
                )
            )
            assertTrue(bo.lhs.nextEOG.size == 2)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testJavaFor() {
        val nodes = translateToNodes("src/test/resources/cfg/ForLoop.java")
        val prints =
            nodes
                .stream()
                .filter { node: Node -> node.code == REFNODESTRINGJAVA }
                .collect(Collectors.toList())
        val fstat = Util.filterCast(nodes, ForStatement::class.java)
        var fs = fstat[0]
        assertTrue(Util.eogConnect(Connect.NODE, Util.Edge.EXITS, prints[0], Connect.SUBTREE, fs))
        assertTrue(
            Util.eogConnect(
                Connect.NODE,
                Util.Edge.EXITS,
                prints[0],
                Connect.SUBTREE,
                fs.initializerStatement
            )
        )
        assertTrue(
            Util.eogConnect(Util.Edge.EXITS, fs.initializerStatement, Connect.SUBTREE, fs.condition)
        )
        assertTrue(Util.eogConnect(Util.Edge.EXITS, fs.condition, Connect.NODE, fs))
        assertTrue(
            Util.eogConnect(
                Connect.NODE,
                Util.Edge.EXITS,
                fs,
                Connect.SUBTREE,
                fs.statement,
                prints[1]
            )
        )
        assertTrue(
            Util.eogConnect(Util.Edge.EXITS, fs.statement, Connect.SUBTREE, fs.iterationStatement)
        )
        assertTrue(
            Util.eogConnect(Util.Edge.EXITS, fs.iterationStatement, Connect.SUBTREE, fs.condition)
        )
        fs = fstat[1]
        assertTrue(Util.eogConnect(Connect.NODE, Util.Edge.EXITS, prints[1], Connect.SUBTREE, fs))
        assertTrue(
            Util.eogConnect(
                Connect.NODE,
                Util.Edge.EXITS,
                prints[1],
                Connect.SUBTREE,
                fs.initializerStatement
            )
        )
        assertTrue(
            Util.eogConnect(Util.Edge.EXITS, fs.initializerStatement, Connect.SUBTREE, fs.condition)
        )
        assertTrue(Util.eogConnect(Util.Edge.EXITS, fs.condition, Connect.NODE, fs))
        assertTrue(
            Util.eogConnect(Util.Edge.EXITS, fs.statement, Connect.SUBTREE, fs.iterationStatement)
        )
        assertTrue(
            Util.eogConnect(Util.Edge.EXITS, fs.iterationStatement, Connect.SUBTREE, fs.condition)
        )
        assertTrue(
            Util.eogConnect(Connect.SUBTREE, Util.Edge.EXITS, fs, Connect.SUBTREE, prints[2])
        )
    }

    @Test
    @Throws(Exception::class)
    fun testCPPFor() {
        val nodes = translateToNodes("src/test/resources/cfg/forloop.cpp")
        val prints =
            nodes
                .stream()
                .filter { node: Node -> node.code == REFNODESTRINGCXX }
                .collect(Collectors.toList())
        val fstat = Util.filterCast(nodes, ForStatement::class.java)
        var fs = fstat[0]
        assertTrue(Util.eogConnect(Connect.NODE, Util.Edge.EXITS, prints[0], Connect.SUBTREE, fs))
        assertTrue(
            Util.eogConnect(
                Connect.NODE,
                Util.Edge.EXITS,
                prints[0],
                Connect.SUBTREE,
                fs.initializerStatement
            )
        )
        assertTrue(
            Util.eogConnect(
                Util.Edge.EXITS,
                fs.initializerStatement,
                Connect.SUBTREE,
                fs.conditionDeclaration
            )
        )
        assertTrue(Util.eogConnect(Util.Edge.EXITS, fs.conditionDeclaration, Connect.NODE, fs))
        assertTrue(
            Util.eogConnect(Util.Edge.EXITS, fs.statement, Connect.SUBTREE, fs.iterationStatement)
        )
        assertTrue(
            Util.eogConnect(
                Util.Edge.EXITS,
                fs.iterationStatement,
                Connect.SUBTREE,
                fs.conditionDeclaration
            )
        )
        assertTrue(
            Util.eogConnect(Connect.SUBTREE, Util.Edge.EXITS, fs, Connect.SUBTREE, prints[1])
        )
        fs = fstat[1]
        assertTrue(Util.eogConnect(Connect.NODE, Util.Edge.EXITS, prints[1], Connect.SUBTREE, fs))
        assertTrue(
            Util.eogConnect(
                Connect.NODE,
                Util.Edge.EXITS,
                prints[1],
                Connect.SUBTREE,
                fs.initializerStatement
            )
        )
        assertTrue(
            Util.eogConnect(Util.Edge.EXITS, fs.initializerStatement, Connect.SUBTREE, fs.condition)
        )
        assertTrue(Util.eogConnect(Util.Edge.EXITS, fs.condition, Connect.NODE, fs))
        assertTrue(
            Util.eogConnect(Util.Edge.EXITS, fs.statement, Connect.SUBTREE, fs.iterationStatement)
        )
        assertTrue(
            Util.eogConnect(Util.Edge.EXITS, fs.iterationStatement, Connect.SUBTREE, fs.condition)
        )
        assertTrue(
            Util.eogConnect(Connect.SUBTREE, Util.Edge.EXITS, fs, Connect.SUBTREE, prints[2])
        )
        fs = fstat[2]
        assertTrue(Util.eogConnect(Connect.NODE, Util.Edge.EXITS, prints[3], Connect.SUBTREE, fs))
        assertTrue(
            Util.eogConnect(
                Connect.NODE,
                Util.Edge.EXITS,
                prints[3],
                Connect.SUBTREE,
                fs.initializerStatement
            )
        )
        assertTrue(
            Util.eogConnect(Util.Edge.EXITS, fs.initializerStatement, Connect.SUBTREE, fs.condition)
        )
        assertTrue(Util.eogConnect(Util.Edge.EXITS, fs.condition, Connect.NODE, fs))
        assertTrue(
            Util.eogConnect(Util.Edge.EXITS, fs.statement, Connect.SUBTREE, fs.iterationStatement)
        )
        assertTrue(
            Util.eogConnect(Util.Edge.EXITS, fs.iterationStatement, Connect.SUBTREE, fs.condition)
        )
        assertTrue(
            Util.eogConnect(Connect.SUBTREE, Util.Edge.EXITS, fs, Connect.SUBTREE, prints[4])
        )
    }

    /** Test function (not method) calls. */
    @Test
    @Throws(Exception::class)
    fun testCPPCallGraph() {
        val nodes = translateToNodes("src/test/resources/cg.cpp")
        val calls = flattenListIsInstance<CallExpression>(nodes)
        val functions = flattenListIsInstance<FunctionDeclaration>(nodes)
        val first = findByUniqueName(calls, "first")
        assertNotNull(first)

        var target = findByUniqueName(functions, "first")
        assertEquals(listOf(target), first.invokes)

        val second = findByUniqueName(calls, "second")
        assertNotNull(second)

        target = findByUniqueName(functions, "second")
        assertEquals(listOf(target), second.invokes)

        val third = findByUniqueName(calls, "third")
        assertNotNull(third)

        target = findByUniquePredicate(functions) { it.name == "third" && it.parameters.size == 2 }
        assertEquals(listOf(target), third.invokes)

        val fourth = findByUniqueName(calls, "fourth")
        assertNotNull(fourth)

        target = findByUniqueName(functions, "fourth")
        assertEquals(listOf(target), fourth.invokes)
    }

    @Test
    @Throws(Exception::class)
    fun testJavaLoops() {
        testLoops("src/test/resources/cfg/Loops.java", "System.out.println();")
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
        val prints =
            nodes
                .stream()
                .filter { node: Node -> node.code == refNodeString }
                .collect(Collectors.toList())
        assertEquals(1, nodes.stream().filter { node: Node? -> node is WhileStatement }.count())
        val wstat = Util.filterCast(nodes, WhileStatement::class.java)[0]
        var conditionEOG = SubgraphWalker.getEOGPathEdges(wstat.condition)
        var blockEOG = SubgraphWalker.getEOGPathEdges(wstat.statement)

        // Print EOG edges for debugging
        nodes[0].accept(
            Strategy::EOG_BACKWARD,
            object : IVisitor<Node>() {
                override fun visit(n: Node) {
                    println(
                        PhysicalLocation.locationLink(n.location) +
                            " -> " +
                            PhysicalLocation.locationLink(n.location)
                    )
                }
            }
        )

        // Assert: Only single entry and exit NODE per block
        assertTrue(conditionEOG.entries.size == 1 && conditionEOG.exits.size == 1)
        assertTrue(blockEOG.entries.size == 1 && blockEOG.exits.size == 1)

        // Assert: While is preceded by a specific printf("\n")
        assertTrue(Util.eogConnect(Connect.NODE, Util.Edge.ENTRIES, wstat, wstat.condition))
        // Assert: Condition is preceded by print or block of the loop itself
        assertTrue(Util.eogConnect(Util.Edge.ENTRIES, wstat.condition, prints[0], wstat.statement))

        // Assert: All EOGs going into the loop branch come from the condition
        assertTrue(Util.eogConnect(Util.Edge.ENTRIES, wstat.statement, Connect.NODE, wstat))

        // Assert: The EOGs going into the second print come either from the then branch or the
        // condition
        assertTrue(Util.eogConnect(Connect.SUBTREE, Util.Edge.EXITS, wstat, prints[1]))
        val dostat = Util.filterCast(nodes, DoStatement::class.java)[0]
        conditionEOG = SubgraphWalker.getEOGPathEdges(dostat.condition)
        blockEOG = SubgraphWalker.getEOGPathEdges(dostat.statement)

        // Assert: Only single entry and exit NODE per block
        assertTrue(conditionEOG.entries.size == 1 && conditionEOG.exits.size == 1)
        assertTrue(blockEOG.entries.size == 1 && blockEOG.exits.size == 1)

        // Assert: do is preceded by print
        assertTrue(Util.eogConnect(Connect.NODE, Util.Edge.ENTRIES, dostat, dostat.condition))
        // Assert: All EOGs going into the loop branch come from the condition
        assertTrue(Util.eogConnect(Util.Edge.EXITS, prints[1], dostat.statement))
        assertTrue(
            Util.eogConnect(
                Util.Quantifier.ANY,
                Connect.NODE,
                Util.Edge.EXITS,
                dostat,
                dostat.statement
            )
        )

        // Assert: Condition is preceded by the loop branch
        assertTrue(Util.eogConnect(Util.Edge.ENTRIES, dostat.condition, dostat.statement))

        // Assert: The EOGs going into the second print come either from the then branch or the
        // condition
        assertTrue(Util.eogConnect(Connect.SUBTREE, Util.Edge.EXITS, dostat, prints[2]))
    }

    @Throws(Exception::class)
    fun testSwitch(relPath: String, refNodeString: String?) {
        val nodes = translateToNodes(relPath)
        val functions =
            Util.filterCast(nodes, FunctionDeclaration::class.java)
                .stream()
                .filter { f: FunctionDeclaration? -> f !is ConstructorDeclaration }
                .collect(Collectors.toList())

        // main()
        var swch = flattenIsInstance<SwitchStatement>(functions[0])[0]
        var prints = Util.subnodesOfCode(functions[0], refNodeString)
        var cases = flattenIsInstance<CaseStatement>(swch)
        var defaults = flattenIsInstance<DefaultStatement>(swch)
        assertTrue(Util.eogConnect(Util.Edge.EXITS, prints[0], Connect.SUBTREE, swch.getSelector()))
        assertTrue(Util.eogConnect(Connect.SUBTREE, Util.Edge.EXITS, swch, prints[1]))

        // Assert: Selector exits connect to the switch root node
        assertTrue(
            Util.eogConnect(
                Connect.NODE,
                Util.Edge.EXITS,
                swch,
                *Stream.of(cases, defaults)
                    .flatMap { l: List<Statement> -> l.stream() }
                    .toArray { size: Int -> arrayOfNulls(size) }
            )
        )
        assertTrue(Util.eogConnect(Util.Edge.EXITS, swch.getSelector(), Connect.NODE, swch))

        // Assert: Entries of case statements have one edge to switch root
        for (s in
            Stream.of(cases, defaults)
                .flatMap { n: List<Statement> -> n.stream() }
                .collect(Collectors.toList())) {
            assertTrue(
                Util.eogConnect(Util.Quantifier.ANY, Util.Edge.ENTRIES, s, Connect.NODE, swch)
            )
        }

        // Assert: All breaks inside of switch connect to the switch root node
        for (b in flattenIsInstance<BreakStatement>(swch)) assertTrue(
            Util.eogConnect(
                Util.Quantifier.ALL,
                Connect.SUBTREE,
                Util.Edge.EXITS,
                b,
                Connect.SUBTREE,
                prints[1]
            )
        )

        // whileswitch
        swch = flattenIsInstance<SwitchStatement>(functions[1])[0]
        prints = Util.subnodesOfCode(functions[1], refNodeString)
        cases = flattenIsInstance(swch)
        defaults = flattenIsInstance(swch)
        var wstat = flattenIsInstance<WhileStatement>(functions[1]).firstOrNull()
        assertTrue(Util.eogConnect(Util.Edge.EXITS, prints[0], wstat))
        assertTrue(Util.eogConnect(Connect.NODE, Util.Edge.EXITS, wstat, prints[2]))
        // Assert: switch root node exits connect to either case or default statements entries
        assertTrue(
            Util.eogConnect(
                Connect.NODE,
                Util.Edge.EXITS,
                swch,
                *Stream.of(cases, defaults)
                    .flatMap { l: List<Statement> -> l.stream() }
                    .toArray { size: Int -> arrayOfNulls(size) }
            )
        )
        // Assert: Selector exits connect to the switch root node
        assertTrue(
            Util.eogConnect(
                Connect.SUBTREE,
                Util.Edge.EXITS,
                swch.getSelector(),
                Connect.NODE,
                swch
            )
        )

        // switch-while
        swch = flattenIsInstance<SwitchStatement>(functions[2])[0]
        prints = Util.subnodesOfCode(functions[2], refNodeString)
        wstat = flattenIsInstance<WhileStatement>(functions[2])[0]
        cases = flattenIsInstance(swch)
        defaults = flattenIsInstance(swch)
        assertTrue(Util.eogConnect(Util.Edge.EXITS, prints[0], swch))
        assertTrue(Util.eogConnect(Util.Edge.EXITS, swch, prints[2]))
        // Assert: Selector exits connect to either case or default statements entries
        assertTrue(Util.eogConnect(Util.Edge.EXITS, swch.getSelector(), Connect.NODE, swch))
        swch = flattenIsInstance<SwitchStatement>(functions[1])[0]
        prints = Util.subnodesOfCode(functions[1], refNodeString)
        var breaks = flattenIsInstance<BreakStatement>(swch)

        // Assert: while-switch, all breaks inside the switch connect to the containing switch
        // unless it has a label which connects the break to the  while
        for (b in breaks) {
            if (b.label != null && b.label.isNotEmpty()) {
                assertTrue(Util.eogConnect(Util.Edge.EXITS, b, Connect.SUBTREE, prints[2]))
            } else {
                assertTrue(Util.eogConnect(Util.Edge.EXITS, b, Connect.SUBTREE, prints[1]))
            }
        }
        swch = flattenIsInstance<SwitchStatement>(functions[2])[0]
        prints = Util.subnodesOfCode(functions[2], refNodeString)
        val whiles = flattenIsInstance<WhileStatement>(functions[2])[0]
        breaks = flattenIsInstance(whiles)

        // Assert: switch-while, all breaks inside the while connect to the containing while unless
        // it has a label which connects the break to the switch
        for (b in breaks) if (b.label != null && b.label.isNotEmpty())
            assertTrue(Util.eogConnect(Util.Edge.EXITS, b, Connect.SUBTREE, prints[2]))
        else assertTrue(Util.eogConnect(Util.Edge.EXITS, b, Connect.SUBTREE, prints[1]))
    }

    @Test
    @Throws(Exception::class)
    fun testCppSwitch() {
        testSwitch("src/test/resources/cfg/switch.cpp", REFNODESTRINGCXX)
    }

    @Test
    @Throws(Exception::class)
    fun testJavaSwitch() {
        testSwitch("src/test/resources/cfg/Switch.java", REFNODESTRINGJAVA)
    }

    @Test
    @Throws(Exception::class)
    fun testJavaBreakContinue() {
        testBreakContinue("src/test/resources/cfg/BreakContinue.java", "System.out.println();")
    }

    @Test
    @Throws(Exception::class)
    fun testCppBreakContinue() {
        testBreakContinue("src/test/resources/cfg/break_continue.cpp", "printf(\"\\n\");")
    }

    /**
     * Tests EOG branch edge property in if/else if/else construct
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testBranchProperty() {
        val topLevel = Path.of("src", "test", "resources", "eog")
        val result = analyze(listOf(topLevel.resolve("EOG.java").toFile()), topLevel, true)

        // Test If-Block
        val firstIf: IfStatement =
            flattenListIsInstance<IfStatement>(result)
                .filter { l -> l.location?.region?.startLine == 6 }[0]
        val a: DeclaredReferenceExpression =
            flattenListIsInstance<DeclaredReferenceExpression>(result)
                .filter { l: DeclaredReferenceExpression ->
                    l.location?.region?.startLine == 8 && l.name == "a"
                }[0]
        val b: DeclaredReferenceExpression =
            flattenListIsInstance<DeclaredReferenceExpression>(result)
                .filter { l: DeclaredReferenceExpression ->
                    l.location?.region?.startLine == 7 && l.name == "b"
                }[0]
        var nextEOG: List<PropertyEdge<Node>> = firstIf.nextEOGEdges
        assertEquals(2, nextEOG.size)
        for (edge in nextEOG) {
            assertEquals(firstIf, edge.start)
            if (edge.end == b) {
                assertEquals(true, edge.getProperty(Properties.BRANCH))
                assertEquals(0, edge.getProperty(Properties.INDEX))
            } else {
                assertEquals(a, edge.end)
                assertEquals(false, edge.getProperty(Properties.BRANCH))
                assertEquals(1, edge.getProperty(Properties.INDEX))
            }
        }
        val elseIf: IfStatement =
            flattenListIsInstance<IfStatement>(result)
                .filter { l: IfStatement -> l.location?.region?.startLine == 8 }[0]
        assertEquals(elseIf, firstIf.elseStatement)
        val b2: DeclaredReferenceExpression =
            flattenListIsInstance<DeclaredReferenceExpression>(result)
                .filter { l: DeclaredReferenceExpression ->
                    l.location?.region?.startLine == 9 && l.name == "b"
                }[0]
        val x: DeclaredReferenceExpression =
            flattenListIsInstance<DeclaredReferenceExpression>(result)
                .filter { l: DeclaredReferenceExpression ->
                    l.location?.region?.startLine == 11 && l.name == "x"
                }[0]
        nextEOG = elseIf.nextEOGEdges
        assertEquals(2, nextEOG.size)
        for (edge in nextEOG) {
            assertEquals(elseIf, edge.start)
            if (edge.end == b2) {
                assertEquals(true, edge.getProperty(Properties.BRANCH))
                assertEquals(0, edge.getProperty(Properties.INDEX))
            } else {
                assertEquals(x, edge.end)
                assertEquals(false, edge.getProperty(Properties.BRANCH))
                assertEquals(1, edge.getProperty(Properties.INDEX))
            }
        }
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
        val prints =
            nodes
                .stream()
                .filter { node: Node -> node.code == refNodeString }
                .collect(Collectors.toList())
        assertEquals(1, nodes.stream().filter { node: Node? -> node is WhileStatement }.count())
        val breaks = Util.filterCast(nodes, BreakStatement::class.java)
        val continues = Util.filterCast(nodes, ContinueStatement::class.java)
        val wstat = Util.filterCast(nodes, WhileStatement::class.java)[0]
        var conditionEOG = SubgraphWalker.getEOGPathEdges(wstat.condition)
        var blockEOG = SubgraphWalker.getEOGPathEdges(wstat.statement)

        // Assert: Only single entry and two exit NODEs per block
        assertTrue(conditionEOG.entries.size == 1 && conditionEOG.exits.size == 1)
        assertTrue(blockEOG.entries.size == 1 && blockEOG.exits.size == 3)

        // Assert: Print is only followed by first nodes in condition
        assertTrue(Util.eogConnect(Util.Edge.EXITS, prints[0], wstat.condition))

        // Assert: condition nodes are preceded by either continue, last nodes in block or last
        // nodes in
        // print
        assertTrue(
            Util.eogConnect(Util.Edge.ENTRIES, wstat.condition, prints[0], wstat.statement) ||
                Util.eogConnect(Connect.NODE, Util.Edge.EXITS, continues[0], wstat.condition)
        )

        // Assert: All EOGs going into the loop branch come from the Loop root node
        assertTrue(Util.eogConnect(Util.Edge.ENTRIES, wstat.statement, wstat))

        // Assert: The EOGs going into the second print come either from the while root or break
        assertTrue(
            Util.eogConnect(Connect.NODE, Util.Edge.EXITS, wstat, prints[1]) ||
                Util.eogConnect(Connect.NODE, Util.Edge.EXITS, breaks[0], prints[1])
        )
        val dostat = Util.filterCast(nodes, DoStatement::class.java)[0]
        conditionEOG = SubgraphWalker.getEOGPathEdges(dostat.condition)
        blockEOG = SubgraphWalker.getEOGPathEdges(dostat.statement)

        // Assert: Only single entry and two exit NODEs per block
        assertTrue(conditionEOG.entries.size == 1 && conditionEOG.exits.size == 1)
        assertTrue(blockEOG.entries.size == 1 && blockEOG.exits.size == 3)

        // Assert: All EOGs going into the loop branch come from the condition
        assertTrue(Util.eogConnect(Util.Edge.EXITS, prints[1], dostat.statement))
        assertTrue(
            Util.eogConnect(
                Util.Quantifier.ANY,
                Connect.NODE,
                Util.Edge.EXITS,
                dostat,
                Connect.SUBTREE,
                dostat.statement
            )
        )
        assertTrue(Util.eogConnect(Util.Edge.EXITS, prints[1], dostat.statement))
        assertTrue(
            Util.eogConnect(
                Util.Quantifier.ANY,
                Connect.NODE,
                Util.Edge.EXITS,
                dostat,
                dostat.statement
            )
        )

        // Assert: Condition is preceded by the loop branch
        assertTrue(
            Util.eogConnect(Util.Edge.ENTRIES, dostat.condition, dostat.statement) ||
                Util.eogConnect(Connect.NODE, Util.Edge.EXITS, continues[1], dostat.condition)
        )

        // Assert: The EOGs going into the third print come  from the loop root
        assertTrue(Util.eogConnect(Connect.NODE, Util.Edge.EXITS, dostat, prints[2]))
    }

    @Test
    @Throws(Exception::class)
    fun testEOGInvariant() {
        val file = File("src/main/java/de/fraunhofer/aisec/cpg/passes/CallResolver.java")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        assertTrue(EvaluationOrderGraphPass.checkEOGInvariant(tu))
    }

    /**
     * Translates the given file into CPG and returns the graph. Extracted to reduce code duplicates
     *
     * @param path
     * - path for the file to test.
     */
    @Throws(Exception::class)
    private fun translateToNodes(path: String): List<Node> {
        val toTranslate = File(path)
        val topLevel = toTranslate.parentFile.toPath()
        val tu = analyzeAndGetFirstTU(listOf(toTranslate), topLevel, true)
        var nodes = SubgraphWalker.flattenAST(tu)
        // TODO: until explicitly added Return Statements are either removed again or code and
        // region
        //  set properly
        nodes =
            nodes.stream().filter { node: Node -> node.code != null }.collect(Collectors.toList())
        return nodes
    }

    companion object {
        var REFNODESTRINGJAVA = "System.out.println();"
        var REFNODESTRINGCXX = "printf(\"\\n\");"
    }
}
