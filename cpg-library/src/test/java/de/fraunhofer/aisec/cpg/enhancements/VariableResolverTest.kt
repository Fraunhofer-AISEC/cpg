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
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.TestUtils.subnodesOfType
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.test.*
import org.junit.jupiter.api.Test

internal class VariableResolverTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "variables")

    @Test
    @Throws(Exception::class)
    fun testFields() {
        val result = analyze("java", topLevel, true)
        val methods = subnodesOfType(result, MethodDeclaration::class.java)
        val fields = subnodesOfType(result, FieldDeclaration::class.java)
        val field = findByUniqueName(fields, "field")
        val getField = findByUniqueName(methods, "getField")
        var returnStatement = subnodesOfType(getField, ReturnStatement::class.java)[0]
        assertEquals(field, (returnStatement.returnValue as MemberExpression).refersTo)

        val noShadow = findByUniqueName(methods, "getField")
        returnStatement = subnodesOfType(noShadow, ReturnStatement::class.java)[0]
        assertEquals(field, (returnStatement.returnValue as MemberExpression).refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testLocalVars() {
        val result = analyze("java", topLevel, true)
        val methods = subnodesOfType(result, MethodDeclaration::class.java)
        val fields = subnodesOfType(result, FieldDeclaration::class.java)
        val field = findByUniqueName(fields, "field")
        val getLocal = findByUniqueName(methods, "getLocal")
        var returnStatement = subnodesOfType(getLocal, ReturnStatement::class.java)[0]
        var local = subnodesOfType(getLocal, VariableDeclaration::class.java)[0]
        var returnValue = returnStatement.returnValue as DeclaredReferenceExpression
        assertNotEquals(field, returnValue.refersTo)
        assertEquals(local, returnValue.refersTo)

        val getShadow = findByUniqueName(methods, "getShadow")
        returnStatement = subnodesOfType(getShadow, ReturnStatement::class.java)[0]
        local = subnodesOfType(getShadow, VariableDeclaration::class.java)[0]
        returnValue = returnStatement.returnValue as DeclaredReferenceExpression

        assertNotEquals(field, returnValue.refersTo)
        assertEquals(local, returnValue.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testLocalVarsCpp() {
        val tu = analyze("cpp", topLevel, true)
        val function = tu[0].getDeclarationAs(2, FunctionDeclaration::class.java)
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

        // Class should not have any fields
        val clazz = tu[0].getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(clazz)
        assertEquals(0, clazz.fields.size)

        // however, the call method should have a proper "this" receiver
        val call = clazz.methods.firstOrNull { it.name == "call" }
        assertNotNull(call)

        val `this` = call.receiver
        assertNotNull(`this`)
        assertTrue(`this`.isImplicit)
        assertEquals("this", `this`.name)
        assertSame(clazz, (`this`.type as? ObjectType)?.recordDeclaration)
    }
}
