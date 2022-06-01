/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.enhancements.calls

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.TestUtils.assertInvokes
import de.fraunhofer.aisec.cpg.TestUtils.assertRefersTo
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.TestUtils.findByUniquePredicate
import de.fraunhofer.aisec.cpg.TestUtils.flattenIsInstance
import de.fraunhofer.aisec.cpg.TestUtils.flattenListIsInstance
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SuperCallTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "superCalls")

    @Test
    @Throws(Exception::class)
    fun testSimpleCall() {
        val result = analyze("java", topLevel, true)
        val records = flattenListIsInstance<RecordDeclaration>(result)
        val superClass = findByUniqueName(records, "SuperClass")
        val superMethods = flattenIsInstance<MethodDeclaration>(superClass)
        val superTarget = findByUniqueName(superMethods, "target")
        val subClass = findByUniqueName(records, "SubClass")
        val methods = flattenIsInstance<MethodDeclaration>(subClass)
        val target = findByUniqueName(methods, "target")
        val calls = flattenIsInstance<CallExpression>(target)
        val superCall = findByUniquePredicate(calls) { "super.target();" == it.code }
        assertEquals(listOf(superTarget), superCall.invokes)
    }

    @Test
    @Throws(Exception::class)
    fun testInterfaceCall() {
        val result = analyze("java", topLevel, true)
        val records = flattenListIsInstance<RecordDeclaration>(result)
        val interface1 = findByUniqueName(records, "Interface1")
        val interface1Methods = flattenIsInstance<MethodDeclaration>(interface1)
        val interface1Target = findByUniqueName(interface1Methods, "target")
        val interface2 = findByUniqueName(records, "Interface2")
        val interface2Methods = flattenIsInstance<MethodDeclaration>(interface2)
        val interface2Target = findByUniqueName(interface2Methods, "target")
        val subClass = findByUniqueName(records, "SubClass")
        val methods = flattenIsInstance<MethodDeclaration>(subClass)
        val target = findByUniqueName(methods, "target")
        val calls = flattenIsInstance<CallExpression>(target)
        val interface1Call =
            findByUniquePredicate(calls) { "Interface1.super.target();" == it.code }
        val interface2Call =
            findByUniquePredicate(calls) { "Interface2.super.target();" == it.code }
        assertEquals(listOf(interface1Target), interface1Call.invokes)
        assertEquals(listOf(interface2Target), interface2Call.invokes)
    }

    @Test
    @Throws(Exception::class)
    fun testSuperField() {
        val result = analyze("java", topLevel, true)
        val records = flattenListIsInstance<RecordDeclaration>(result)
        val superClass = findByUniqueName(records, "SuperClass")
        val superField = findByUniqueName(superClass.fields, "field")
        val subClass = findByUniqueName(records, "SubClass")
        val methods = flattenIsInstance<MethodDeclaration>(subClass)
        val field = findByUniqueName(subClass.fields, "field")
        val getField = findByUniqueName(methods, "getField")
        var refs = flattenIsInstance<MemberExpression>(getField)
        val fieldRef = findByUniquePredicate(refs) { "field" == it.code }
        val getSuperField = findByUniqueName(methods, "getSuperField")
        refs = flattenIsInstance(getSuperField)
        val superFieldRef = findByUniquePredicate(refs) { "super.field" == it.code }
        assertTrue(fieldRef.base is DeclaredReferenceExpression)
        assertRefersTo(fieldRef.base, subClass.`this`)
        assertEquals(field, fieldRef.refersTo)
        assertTrue(superFieldRef.base is DeclaredReferenceExpression)
        assertRefersTo(superFieldRef.base, superClass.`this`)
        assertEquals(superField, superFieldRef.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testInnerCall() {
        val result = analyze("java", topLevel, true)
        val records = flattenListIsInstance<RecordDeclaration>(result)
        val superClass = findByUniqueName(records, "SuperClass")
        val superMethods = flattenIsInstance<MethodDeclaration>(superClass)
        val superTarget = findByUniqueName(superMethods, "target")
        val innerClass = findByUniqueName(records, "SubClass.Inner")
        val methods = flattenIsInstance<MethodDeclaration>(innerClass)
        val target = findByUniqueName(methods, "inner")
        val calls = flattenIsInstance<CallExpression>(target)
        val superCall = findByUniquePredicate(calls) { "SubClass.super.target();" == it.code }
        assertInvokes(superCall, superTarget)
    }

    @Test
    @Throws(Exception::class)
    fun testNoExcessFields() {
        val result = analyze("java", topLevel, true)
        val records = flattenListIsInstance<RecordDeclaration>(result)
        val superClass = findByUniqueName(records, "SuperClass")
        assertEquals(2, superClass.fields.size)
        assertEquals(
            mutableSetOf("this", "field"),
            superClass.fields.stream().map(Node::name).collect(Collectors.toSet())
        )

        val subClass = findByUniqueName(records, "SubClass")
        assertEquals(2, subClass.fields.size)
        assertEquals(
            mutableSetOf("this", "field"),
            subClass.fields.stream().map(Node::name).collect(Collectors.toSet())
        )

        val inner = findByUniqueName(records, "SubClass.Inner")
        assertEquals(1, inner.fields.size)
        assertEquals(
            mutableSetOf("this"),
            inner.fields.stream().map(Node::name).collect(Collectors.toSet())
        )
    }
}
