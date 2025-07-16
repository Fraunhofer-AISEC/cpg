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

import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.test.*
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
        val result = analyze("java", topLevel, true) { it.registerLanguage<JavaLanguage>() }
        val records = result.dRecords
        val superClass = findByUniqueName(records, "SuperClass")
        val superMethods = superClass.methods
        val superTarget = findByUniqueName(superMethods, "target")
        val subClass = findByUniqueName(records, "SubClass")
        val methods = subClass.methods
        val target = findByUniqueName(methods, "target")
        val calls = target.dCalls
        val superCall = findByUniquePredicate(calls) { "super.target();" == it.code }
        assertInvokes(superCall, superTarget)
    }

    @Test
    @Throws(Exception::class)
    fun testInterfaceCall() {
        val result = analyze("java", topLevel, true) { it.registerLanguage<JavaLanguage>() }
        val records = result.dRecords
        val interface1 = findByUniqueName(records, "Interface1")
        val interface1Methods = interface1.methods
        val interface1Target = findByUniqueName(interface1Methods, "target")
        val interface2 = findByUniqueName(records, "Interface2")
        val interface2Methods = interface2.methods
        val interface2Target = findByUniqueName(interface2Methods, "target")
        val subClass = findByUniqueName(records, "SubClass")
        val methods = subClass.methods
        val target = findByUniqueName(methods, "target")
        val calls = target.dCalls
        val interface1Call =
            findByUniquePredicate(calls) { "Interface1.super.target();" == it.code }
        val interface2Call =
            findByUniquePredicate(calls) { "Interface2.super.target();" == it.code }
        assertInvokes(interface1Call, interface1Target)
        assertInvokes(interface2Call, interface2Target)
    }

    @Test
    @Throws(Exception::class)
    fun testSuperField() {
        val result = analyze("java", topLevel, true) { it.registerLanguage<JavaLanguage>() }
        val records = result.dRecords
        val superClass = findByUniqueName(records, "SuperClass")
        val superField = findByUniqueName(superClass.fields, "field")
        val subClass = findByUniqueName(records, "SubClass")
        val methods = subClass.methods
        val field = findByUniqueName(subClass.fields, "field")
        val getField = findByUniqueName(methods, "getField")
        var refs = getField.dRefs
        val fieldRef = findByUniquePredicate(refs) { "field" == it.code }
        val getSuperField = findByUniqueName(methods, "getSuperField")
        refs = getSuperField.descendants<MemberExpression>()
        val superFieldRef = findByUniquePredicate(refs) { "super.field" == it.code }
        assertEquals(field, fieldRef.refersTo)
        assertTrue(superFieldRef.base is Reference)
        assertRefersTo(superFieldRef.base, getSuperField.receiver)
        assertEquals(superField, superFieldRef.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testInnerCall() {
        val result = analyze("java", topLevel, true) { it.registerLanguage<JavaLanguage>() }
        val records = result.dRecords
        val superClass = findByUniqueName(records, "SuperClass")
        val superMethods = superClass.methods
        val superTarget = findByUniqueName(superMethods, "target")
        val innerClass = findByUniqueName(records, "SubClass.Inner")
        val methods = innerClass.methods
        val target = findByUniqueName(methods, "inner")
        val calls = target.dCalls
        val superCall = findByUniquePredicate(calls) { "SubClass.super.target();" == it.code }
        assertInvokes(superCall, superTarget)
    }

    @Test
    @Throws(Exception::class)
    fun testNoExcessFields() {
        val result = analyze("java", topLevel, true) { it.registerLanguage<JavaLanguage>() }
        val records = result.dRecords

        val superClass = records["SuperClass"]
        assertNotNull(superClass)
        assertEquals(1, superClass.fields.size)
        assertEquals(listOf("field"), superClass.fields.map { it.name.localName })

        val subClass = findByUniqueName(records, "SubClass")
        assertEquals(1, subClass.fields.size)
        assertEquals(listOf("field"), subClass.fields.map { it.name.localName })

        val inner = findByUniqueName(records, "SubClass.Inner")
        assertEquals(1, inner.fields.size)
        assertEquals(
            listOf("SubClass.Inner.this\$SubClass"),
            inner.fields.map { it.name.toString() },
        )
    }
}
