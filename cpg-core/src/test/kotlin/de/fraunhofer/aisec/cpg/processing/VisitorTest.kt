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

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
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
                override fun visit(n: Node) {
                    visited += n
                }
            }
        )

        assertEquals(listOf<Node>(tu, name, func), visited)
    }

    /** Visits all nodes along EOG. */
    @Test
    fun testAllEogNodeVisitor() {
        val nodeList: MutableList<Node> = ArrayList()
        val recordDeclaration = namespace?.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(recordDeclaration)

        val method = recordDeclaration.byNameOrNull<MethodDeclaration>("method")
        assertNotNull(method)

        val firstStmt = method.bodyOrNull<Statement>()
        assertNotNull(firstStmt)

        firstStmt.accept(
            Strategy::EOG_FORWARD,
            object : IVisitor<Node>() {
                override fun visit(n: Node) {
                    log.info("Node: $n")
                    nodeList.add(n)
                }
            }
        )
        assertEquals(23, nodeList.size)
    }

    /** Visits all nodes along AST. */
    @Test
    fun testAllAstNodeVisitor() {
        val recordDeclaration = namespace?.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(recordDeclaration)

        val nodeList = mutableListOf<Node>()
        recordDeclaration.accept(
            Strategy::AST_FORWARD,
            object : IVisitor<Node>() {
                override fun visit(n: Node) {
                    log.info("Node: $n")
                    nodeList.add(n)
                }
            }
        )
        assertEquals(38, nodeList.size)
    }

    /** Visits only ReturnStatement nodes. */
    @Test
    fun testReturnStmtVisitor() {
        val returnStmts: MutableList<ReturnStatement> = ArrayList()
        val recordDeclaration = namespace?.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(recordDeclaration)

        recordDeclaration.accept(
            Strategy::AST_FORWARD,
            object : IVisitor<Node>() {
                fun visit(r: ReturnStatement) {
                    returnStmts.add(r)
                }
            }
        )
        assertEquals(2, returnStmts.size)
    }

    companion object {
        private var namespace: NamespaceDeclaration? = null
        @BeforeAll
        @JvmStatic
        @Throws(
            TranslationException::class,
            InterruptedException::class,
            ExecutionException::class,
            TimeoutException::class
        )
        fun setup() {
            val file = File("src/test/resources/compiling/RecordDeclaration.java")
            val config =
                TranslationConfiguration.builder()
                    .sourceLocations(file)
                    .defaultPasses()
                    .defaultLanguages()
                    .build()
            val result =
                TranslationManager.builder().config(config).build().analyze()[20, TimeUnit.SECONDS]
            val tu = result.translationUnits.firstOrNull()
            assertNotNull(tu)

            namespace = tu.declarations.firstOrNull() as NamespaceDeclaration
        }
    }
}
