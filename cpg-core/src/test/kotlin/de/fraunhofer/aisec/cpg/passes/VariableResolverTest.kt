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
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.TestUtils.flattenIsInstance
import de.fraunhofer.aisec.cpg.TestUtils.flattenListIsInstance
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.test.*

internal class VariableResolverTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "variables")

    @Test
    @Throws(Exception::class)
    fun testFields() {
        val result = analyze("java", topLevel, true)
        val methods = flattenListIsInstance<MethodDeclaration>(result)
        val fields = flattenListIsInstance<FieldDeclaration>(result)
        val field = findByUniqueName(fields, "field")
        val getField = findByUniqueName(methods, "getField")
        var returnStatement = flattenIsInstance<ReturnStatement>(getField).firstOrNull()
        assertNotNull(returnStatement)
        assertEquals(field, (returnStatement.returnValue as MemberExpression).refersTo)

        val noShadow = findByUniqueName(methods, "getField")

        returnStatement = flattenIsInstance<ReturnStatement>(noShadow).firstOrNull()
        assertNotNull(returnStatement)
        assertEquals(field, (returnStatement.returnValue as MemberExpression).refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testLocalVars() {
        val result = analyze("java", topLevel, true)
        val methods = flattenListIsInstance<MethodDeclaration>(result)
        val fields = flattenListIsInstance<FieldDeclaration>(result)
        val field = findByUniqueName(fields, "field")
        val getLocal = findByUniqueName(methods, "getLocal")
        var returnStatement = flattenIsInstance<ReturnStatement>(getLocal).firstOrNull()
        assertNotNull(returnStatement)

        var local = flattenIsInstance<VariableDeclaration>(getLocal).firstOrNull()

        var returnValue = returnStatement.returnValue as DeclaredReferenceExpression
        assertNotEquals(field, returnValue.refersTo)
        assertEquals(local, returnValue.refersTo)

        val getShadow = findByUniqueName(methods, "getShadow")

        returnStatement = flattenIsInstance<ReturnStatement>(getShadow).firstOrNull()
        assertNotNull(returnStatement)

        local = flattenIsInstance<VariableDeclaration>(getShadow).firstOrNull()

        returnValue = returnStatement.returnValue as DeclaredReferenceExpression
        assertNotEquals(field, returnValue.refersTo)
        assertEquals(local, returnValue.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testLocalVarsCpp() {
        val tu = analyze("cpp", topLevel, true).firstOrNull()
        assertNotNull(tu)

        val function = tu.getDeclarationAs(2, FunctionDeclaration::class.java)
        assertEquals("testExpressionInExpressionList()int", function!!.signature)

        val locals = function.body.locals

        // Expecting x, foo, t
        val localNames =
            locals.stream().map { l: VariableDeclaration -> l.name }.collect(Collectors.toSet())
        assertTrue(localNames.contains("x"))
        assertTrue(localNames.contains("foo"))
        assertTrue(localNames.contains("t"))
        // ... and nothing else
        assertEquals(3, localNames.size)

        // Class "Test" has only one (virtual) field "this"
        val clazz = tu.getDeclarationAs(0, RecordDeclaration::class.java)
        for (f in clazz!!.fields) {
            if (f == null) {
                println("NULL")
                continue
            }
            println(f.name + " " + f.initializer)
        }
        // FIXME Fails. Actually has "this", "a" and "foo"
        assertEquals(1, clazz.fields.size)
    }
}
