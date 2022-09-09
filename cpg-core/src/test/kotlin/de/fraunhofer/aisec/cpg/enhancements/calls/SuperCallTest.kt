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
import de.fraunhofer.aisec.cpg.TestUtils.findByUniquePredicate
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.SearchModifier.UNIQUE
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class SuperCallTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "superCalls")

    @Test
    @Throws(Exception::class)
    fun testSimpleCall() {
        val result = analyze("java", topLevel, true)
        val records = result.records
        val superClass = assertNotNull(records["SuperClass", UNIQUE])
        val superMethods = superClass.methods
        val superTarget = assertNotNull(superMethods["target", UNIQUE])
        val subClass = assertNotNull(records["SubClass", UNIQUE])
        val methods = subClass.methods
        val target = assertNotNull(methods["target", UNIQUE])
        val calls = target.calls
        val superCall = findByUniquePredicate(calls) { "super.target();" == it.code }
        assertEquals(listOf(superTarget), superCall.invokes)
    }

    @Test
    @Throws(Exception::class)
    fun testInterfaceCall() {
        val result = analyze("java", topLevel, true)
        val records = result.records
        val interface1 = assertNotNull(records["Interface1", UNIQUE])
        val interface1Methods = interface1.methods
        val interface1Target = assertNotNull(interface1Methods["target", UNIQUE])
        val interface2 = assertNotNull(records["Interface2", UNIQUE])
        val interface2Methods = interface2.methods
        val interface2Target = assertNotNull(interface2Methods["target", UNIQUE])
        val subClass = assertNotNull(records["SubClass", UNIQUE])
        val methods = subClass.methods
        val target = assertNotNull(methods["target", UNIQUE])
        val calls = target.calls
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
        val records = result.records
        val superClass = assertNotNull(records["SuperClass", UNIQUE])
        val superField = assertNotNull(superClass.fields["field", UNIQUE])
        val subClass = assertNotNull(records["SubClass", UNIQUE])
        val methods = subClass.methods
        val field = assertNotNull(subClass.fields["field", UNIQUE])
        val getField = assertNotNull(methods["getField", UNIQUE])
        var refs = getField.allChildren<MemberExpression>()
        val fieldRef = findByUniquePredicate(refs) { "field" == it.code }
        val getSuperField = assertNotNull(methods["getSuperField", UNIQUE])
        refs = getSuperField.allChildren<MemberExpression>()
        val superFieldRef = findByUniquePredicate(refs) { "super.field" == it.code }
        assertTrue(fieldRef.base is DeclaredReferenceExpression)
        assertRefersTo(fieldRef.base, getField.receiver)
        assertEquals(field, fieldRef.refersTo)
        assertTrue(superFieldRef.base is DeclaredReferenceExpression)
        assertRefersTo(superFieldRef.base, getSuperField.receiver)
        assertEquals(superField, superFieldRef.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testInnerCall() {
        val result = analyze("java", topLevel, true)
        val records = result.records
        val superClass = assertNotNull(records["SuperClass", UNIQUE])
        val superMethods = superClass.methods
        val superTarget = assertNotNull(superMethods["target", UNIQUE])
        val innerClass = assertNotNull(records["SubClass.Inner", UNIQUE])
        val methods = innerClass.methods
        val target = assertNotNull(methods["inner", UNIQUE])
        val calls = target.calls
        val superCall = findByUniquePredicate(calls) { "SubClass.super.target();" == it.code }
        assertInvokes(superCall, superTarget)
    }

    @Test
    @Throws(Exception::class)
    fun testNoExcessFields() {
        val result = analyze("java", topLevel, true)
        val records = result.records

        val superClass = records["SuperClass"]
        assertNotNull(superClass)
        assertEquals(1, superClass.fields.size)
        assertEquals(listOf("field"), superClass.fields.map(Node::name))

        val subClass = assertNotNull(records["SubClass", UNIQUE])
        assertEquals(1, subClass.fields.size)
        assertEquals(listOf("field"), subClass.fields.map(Node::name))

        val inner = assertNotNull(records["SubClass.Inner", UNIQUE])
        assertEquals(1, inner.fields.size)
        assertEquals(listOf("SubClass.this"), inner.fields.map(Node::name))
    }
}
