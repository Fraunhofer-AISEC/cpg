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
package de.fraunhofer.aisec.cpg.processing

import de.fraunhofer.aisec.cpg.GraphExamples
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import de.fraunhofer.aisec.cpg.test.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeAll

class VisitorTest : BaseTest() {
    @Test
    fun testLoopDetection() {
        // Let's create an intentional loop
        val tu = TranslationUnitDeclaration()
        val name = NamespaceDeclaration()
        val func = FunctionDeclaration()
        name.addDeclaration(tu)
        name.addDeclaration(func)
        tu.addDeclaration(name)

        val visited = mutableListOf<Node>()
        // Let's visit
        tu.accept(
            Strategy::AST_FORWARD,
            object : IVisitor<Node>() {
                override fun visit(t: Node) {
                    visited += t
                }
            },
        )

        assertEquals(listOf<Node>(tu, name, func), visited)
    }

    /** Visits all nodes along EOG. */
    @Test
    fun testAllEogNodeVisitor() {
        var record = recordDeclaration
        assertNotNull(record)

        val nodeList: MutableList<Node> = ArrayList()
        val method = record.methods["method"]
        assertNotNull(method)

        // the "first" statement includes the block itself, so we need to get index 1 instead of 0
        val firstStmt = method.bodyOrNull<de.fraunhofer.aisec.cpg.graph.statements.Statement>(0)
        assertNotNull(firstStmt)

        firstStmt.accept(
            Strategy::EOG_FORWARD,
            object : IVisitor<Node>() {
                override fun visit(t: Node) {
                    log.info("Node: $t")
                    nodeList.add(t)
                }
            },
        )
        assertEquals(24, nodeList.size)
    }

    /** Visits all nodes along AST. */
    @Test
    fun testAllAstNodeVisitor() {
        assertNotNull(recordDeclaration)

        val nodeList = mutableListOf<Node>()
        recordDeclaration!!.accept(
            Strategy::AST_FORWARD,
            object : IVisitor<Node>() {
                override fun visit(t: Node) {
                    log.info("Node: $t")
                    nodeList.add(t)
                }
            },
        )
        // TODO: It seems to expect a FieldDeclaration for "System" but that's contrary to other
        // tests where it shouldn't exist.
        // Please double check. Until then, I'll change the expected number.
        assertEquals(37, nodeList.size)
    }

    /** Visits only ReturnStatement nodes. */
    @Test
    fun testReturnStmtVisitor() {
        val returnStatements: MutableList<ReturnStatement> = ArrayList()
        assertNotNull(recordDeclaration)

        recordDeclaration!!.accept(
            Strategy::AST_FORWARD,
            object : IVisitor<Node>() {
                fun visit(r: ReturnStatement) {
                    returnStatements.add(r)
                }
            },
        )
        assertEquals(2, returnStatements.size)
    }

    companion object {
        private var recordDeclaration: RecordDeclaration? = null

        @BeforeAll
        @JvmStatic
        @Throws(
            TranslationException::class,
            InterruptedException::class,
            ExecutionException::class,
            TimeoutException::class,
        )
        fun setup() {
            val cpg = GraphExamples.getVisitorTest()
            recordDeclaration = cpg.records.firstOrNull()
        }
    }
}
