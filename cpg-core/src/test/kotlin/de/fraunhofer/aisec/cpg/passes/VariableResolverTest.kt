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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.GraphExamples
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.fields
import de.fraunhofer.aisec.cpg.graph.methods
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStmt
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpr
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.variables
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

internal class VariableResolverTest : BaseTest() {

    @Test
    @Throws(Exception::class)
    fun testFields() {
        val result = GraphExamples.getVariables()
        val methods = result.methods
        val fields = result.fields
        val field = findByUniqueName(fields, "field")
        val getField = findByUniqueName(methods, "getField")
        var returnStmt = getField.allChildren<ReturnStmt>().firstOrNull()
        assertNotNull(returnStmt)
        assertEquals(field, (returnStmt.returnValue as MemberExpr).refersTo)

        val noShadow = findByUniqueName(methods, "getField")

        returnStmt = noShadow.allChildren<ReturnStmt>().firstOrNull()
        assertNotNull(returnStmt)
        assertEquals(field, (returnStmt.returnValue as MemberExpr).refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testLocalVars() {
        val result = GraphExamples.getVariables()
        val methods = result.methods
        val fields = result.fields
        val field = findByUniqueName(fields, "field")
        val getLocal = findByUniqueName(methods, "getLocal")
        var returnStmt = getLocal.allChildren<ReturnStmt>().firstOrNull()
        assertNotNull(returnStmt)

        var local = getLocal.variables.firstOrNull { it.name.localName != "this" }

        var returnValue = returnStmt.returnValue as Reference
        assertNotEquals(field, returnValue.refersTo)
        assertEquals(local, returnValue.refersTo)

        val getShadow = findByUniqueName(methods, "getShadow")

        returnStmt = getShadow.allChildren<ReturnStmt>().firstOrNull()
        assertNotNull(returnStmt)

        local = getShadow.variables.firstOrNull { it.name.localName != "this" }

        returnValue = returnStmt.returnValue as Reference
        assertNotEquals(field, returnValue.refersTo)
        assertEquals(local, returnValue.refersTo)
    }
}
