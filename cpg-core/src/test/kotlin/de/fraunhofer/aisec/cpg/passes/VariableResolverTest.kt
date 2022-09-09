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
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.SearchModifier.UNIQUE
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

internal class VariableResolverTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "variables")

    @Test
    @Throws(Exception::class)
    fun testFields() {
        val result = analyze("java", topLevel, true)
        val methods = result.methods
        val fields = result.fields
        val field = assertNotNull(fields["field", UNIQUE])
        val getField = assertNotNull(methods["getField", UNIQUE])
        var returnStatement = getField.allChildren<ReturnStatement>().firstOrNull()
        assertNotNull(returnStatement)
        assertEquals(field, (returnStatement.returnValue as MemberExpression).refersTo)

        val noShadow = assertNotNull(methods["getField", UNIQUE])

        returnStatement = noShadow.allChildren<ReturnStatement>().firstOrNull()
        assertNotNull(returnStatement)
        assertEquals(field, (returnStatement.returnValue as MemberExpression).refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testLocalVars() {
        val result = analyze("java", topLevel, true)
        val methods = result.methods
        val fields = result.fields
        val field = assertNotNull(fields["field", UNIQUE])
        val getLocal = assertNotNull(methods["getLocal", UNIQUE])
        var returnStatement = getLocal.allChildren<ReturnStatement>().firstOrNull()
        assertNotNull(returnStatement)

        var local = getLocal.variables.firstOrNull()

        var returnValue = returnStatement.returnValue as DeclaredReferenceExpression
        assertNotEquals(field, returnValue.refersTo)
        assertEquals(local, returnValue.refersTo)

        val getShadow = assertNotNull(methods["getShadow", UNIQUE])

        returnStatement = getShadow.allChildren<ReturnStatement>().firstOrNull()
        assertNotNull(returnStatement)

        local = getShadow.variables.firstOrNull()

        returnValue = returnStatement.returnValue as DeclaredReferenceExpression
        assertNotEquals(field, returnValue.refersTo)
        assertEquals(local, returnValue.refersTo)
    }
}
