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
package de.fraunhofer.aisec.codyze.console

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import kotlin.test.*

class NodeDetailsTest {
    private val uri = URI("file:/details.c")

    private fun at(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int) =
        PhysicalLocation(uri, Region(startLine, startColumn, endLine, endColumn))

    // void main() {        line 1
    //   puts(x);           line 2, the call spans the columns 3 to 11, its callee 3 to 7
    // }                    line 3
    private val callee =
        Reference().apply {
            name = Name("puts")
            location = at(2, 3, 2, 7)
        }
    private val argument =
        Reference().apply {
            name = Name("x")
            location = at(2, 8, 2, 9)
        }
    private val call =
        Call().apply {
            name = Name("puts")
            location = at(2, 3, 2, 11)
            this.callee = this@NodeDetailsTest.callee
            arguments += argument
        }
    private val body =
        Block().apply {
            location = at(1, 13, 3, 2)
            statements += call
        }
    private val tu =
        TranslationUnit().apply {
            declarations +=
                Function().apply {
                    name = Name("main")
                    location = at(1, 1, 3, 2)
                    body = this@NodeDetailsTest.body
                }
        }

    @Test
    fun testInnermostNode() {
        assertSame(callee, tu.nodeAt(2, 4))
        assertSame(argument, tu.nodeAt(2, 8))
        // Between the argument and the closing parenthesis, only the call contains the position
        assertSame(call, tu.nodeAt(2, 10))
        assertSame(body, tu.nodeAt(3, 1))
    }

    @Test
    fun testEndColumnIsExclusive() {
        assertSame(body, tu.nodeAt(2, 11))
    }

    @Test
    fun testNoNodeOutsideOfTheCode() {
        assertNull(tu.nodeAt(10, 1))
    }

    @Test
    fun testExplicitNodeIsPreferredOverImplicitOne() {
        // An implicit node with the same region as the call, e.g. an inserted conversion
        call.arguments +=
            Literal<Int>().apply {
                isImplicit = true
                location = at(2, 3, 2, 11)
            }
        assertSame(call, tu.nodeAt(2, 10))
    }

    @Test
    fun testDeeperNodeIsPreferredWithTheSameRegion() {
        // A callee that spans the whole call, e.g. a call without parentheses
        callee.location = at(2, 3, 2, 11)
        assertSame(callee, tu.nodeAt(2, 10))
    }
}
