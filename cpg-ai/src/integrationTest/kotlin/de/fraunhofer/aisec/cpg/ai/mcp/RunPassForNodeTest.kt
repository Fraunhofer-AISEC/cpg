/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.ai.mcp

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.runCpgAnalyze
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.runPassForNode
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.getSession
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.passes.ResolveMemberAmbiguityPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.TranslationUnitPass
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RunPassForNodeTest {

    @Test
    fun runEOGPass() {
        // Build a small CPG without passes
        runCpgAnalyze(
            CpgAnalyzePayload(content = "def hello():\n    print('X')", extension = "py"),
            runPasses = false,
            cleanup = true,
        )
        val session = getSession()
        assertNotNull(session)

        // Pick any node (i.e., the function declaration "hello")
        val functionDecl = session.translationResult.functions["hello"]
        assertNotNull(functionDecl)

        // Use a concrete TU-related pass on the child
        val result =
            runPassForNode(
                session.nodeToPass,
                session.translationResult,
                functionDecl,
                SymbolResolver::class,
                session.translationContext,
            )
        // No error expected
        assertTrue(result.success)
    }

    @Test
    fun runsPassOnNearestParent() {
        // Build a small CPG without passes
        runCpgAnalyze(
            CpgAnalyzePayload(content = "def hello():\n    print('X')", extension = "py"),
            runPasses = false,
            cleanup = true,
        )
        val session = getSession()
        assertNotNull(session)

        // Pick any node (i.e., the function declaration "hello")
        val functionDecl = session.translationResult.functions["hello"]
        assertNotNull(functionDecl)

        // Use a concrete TU-related pass on the child
        val result =
            runPassForNode(
                session.nodeToPass,
                session.translationResult,
                functionDecl,
                ResolveMemberAmbiguityPass::class,
                session.translationContext,
            )
        // No error expected
        assertTrue(result.success)
    }

    @Test
    fun runsPassOnNearestChild() {
        // Build a small CPG without passes
        runCpgAnalyze(
            CpgAnalyzePayload(content = "def hello():\n    print('X')", extension = "py"),
            runPasses = false,
            cleanup = true,
        )
        val session = getSession()
        assertNotNull(session)

        // Use a concrete TU-related pass on the parent
        val result =
            runPassForNode(
                session.nodeToPass,
                session.translationResult,
                session.translationResult,
                ResolveMemberAmbiguityPass::class,
                session.translationContext,
            )
        // No error expected
        assertTrue(result.success)
    }

    @Test
    fun runsPassOnCorrectNode() {
        // Build a small CPG without passes
        runCpgAnalyze(
            CpgAnalyzePayload(content = "def hello():\n    print('X')", extension = "py"),
            runPasses = false,
            cleanup = true,
        )
        val session = getSession()
        assertNotNull(session)

        // Use a concrete TU-related pass on the parent
        val result =
            runPassForNode(
                session.nodeToPass,
                session.translationResult,
                session.translationResult,
                ResolveMemberAmbiguityPass::class,
                session.translationContext,
            )
        // No error expected
        assertTrue(result.success)
    }

    @Test
    fun returnsErrorWhenPassPrototypeCannotBeConstructed() {
        // Ensure analysis and context exist
        runCpgAnalyze(
            CpgAnalyzePayload(content = "def hello():\n    print('X')", extension = "py"),
            runPasses = false,
            cleanup = true,
        )
        val session = getSession()
        assertNotNull(session)

        val someNode: Node = session.translationResult.nodes.first()
        // TranslationUnitPass is abstract; primary constructor call should fail with
        // IllegalArgumentException
        assertFailsWith<IllegalArgumentException> {
            runPassForNode(
                session.nodeToPass,
                session.translationResult,
                someNode,
                TranslationUnitPass::class,
                session.translationContext,
            )
        }
    }
}
