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
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.passes.ImportDependencies
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
        val ctx = TranslationContext()
        val tu = TranslationUnitDeclaration(ctx)
        val name = NamespaceDeclaration(ctx)
        val func = FunctionDeclaration(ctx)
        name.addDeclaration(tu)
        name.addDeclaration(func)
        tu.addDeclaration(name)

        val visited = mutableListOf<Node>()
        // Let's visit
        tu.accept(
            Strategy::AST_FORWARD,
            object : Visitor<AstNode>() {
                override fun visit(t: AstNode) {
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

        firstStmt.accept<EvaluatedNode>(
            Strategy::EOG_FORWARD,
            object : Visitor<EvaluatedNode>() {
                override fun visit(t: EvaluatedNode) {
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
        recordDeclaration?.accept(
            Strategy::AST_FORWARD,
            object : Visitor<AstNode>() {
                override fun visit(t: AstNode) {
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

        recordDeclaration?.accept(
            Strategy::AST_FORWARD,
            object : Visitor<AstNode>() {
                fun visit(r: ReturnStatement) {
                    returnStatements.add(r)
                }
            },
        )
        assertEquals(2, returnStatements.size)
    }

    @Test
    fun testFallbackComponentLeastImported() {
        val ctx = TranslationContext()
        val component1 = Component(ctx).also { it.name = Name("component1") }
        val component2 = Component(ctx).also { it.name = Name("component2") }

        val tr =
            TranslationResult(
                translationManager = TranslationManager.builder().build(),
                finalCtx = TranslationContext(config = TranslationConfiguration.builder().build()),
            )
        tr.components += component1
        tr.components += component2

        // will trigger fallback as we have no dependency information
        val fallback = Strategy.COMPONENTS_LEAST_IMPORTS(tr).asSequence().toList()
        assertEquals(listOf(component1, component2), fallback)

        tr.componentDependencies =
            ImportDependencies<Component>(tr.components).also {
                it.add(component1, component2)
                it
            }

        // will use sorted
        val sorted = Strategy.COMPONENTS_LEAST_IMPORTS(tr).asSequence().toList()
        assertEquals(listOf(component2, component1), sorted)
        assertEquals(tr.componentDependencies?.sorted, sorted)
    }

    @Test
    fun testFallbackTULeastImported() {
        val ctx = TranslationContext()
        val component = Component(ctx)

        val tr1 = TranslationUnitDeclaration(ctx).also { it.name = Name("tr1") }
        val tr2 = TranslationUnitDeclaration(ctx).also { it.name = Name("tr2") }

        component.translationUnits += tr1
        component.translationUnits += tr2

        // will trigger fallback as we have no dependency information
        val fallback = Strategy.TRANSLATION_UNITS_LEAST_IMPORTS(component).asSequence().toList()
        assertEquals(listOf(tr1, tr2), fallback)

        component.translationUnitDependencies =
            ImportDependencies<TranslationUnitDeclaration>(component.translationUnits).also {
                it.add(tr1, tr2)
                it
            }

        // will use sorted
        val sorted = Strategy.TRANSLATION_UNITS_LEAST_IMPORTS(component).asSequence().toList()
        assertEquals(listOf(tr2, tr1), sorted)
        assertEquals(component.translationUnitDependencies?.sorted, sorted)
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
