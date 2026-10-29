/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.csharp

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.declarations.Import
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.*

class CSharpLanguageTest : BaseTest() {

    private fun translate(file: String) =
        Path.of("src", "test", "resources", "csharp").let { topLevel ->
            analyzeAndGetFirstTU(listOf(topLevel.resolve(file).toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        }

    @Test
    fun testFieldVisibility() {
        val tu = translate("Modifiers.cs")
        val record = tu.records["Modifiers"]
        assertNotNull(record)

        fun field(name: String): Field {
            val field = record.fields[name]
            assertNotNull(field, "no field $name")
            return field
        }

        // Without an access modifier, a member is private
        assertEquals(Visibility.PRIVATE, field("implicitPrivate").visibility)
        assertEquals(Visibility.PUBLIC, field("pub").visibility)
        assertEquals(Visibility.PROTECTED, field("prot").visibility)
        // `internal` maps to PACKAGE, not to INTERNAL
        assertEquals(Visibility.PACKAGE, field("intern").visibility)
        assertEquals(Visibility.PACKAGE, field("protIntern").visibility)
        assertEquals(Visibility.PROTECTED, field("privProt").visibility)

        // The record itself is public, an interface member is public by default and a type
        // without a modifier is internal
        assertEquals(Visibility.PUBLIC, record.visibility)
        val shape = tu.records["IShape"]
        assertNotNull(shape)
        assertEquals(Visibility.PUBLIC, shape.methods["Area"]?.visibility)
        assertEquals(Visibility.PACKAGE, tu.records["InternalByDefault"]?.visibility)
    }

    @Test
    fun testStatic() {
        val tu = translate("Modifiers.cs")
        val record = tu.records["Modifiers"]
        assertNotNull(record)

        assertTrue(record.fields["stat"]?.isStatic == true)
        // `const` is implicitly static
        assertTrue(record.fields["Const"]?.isStatic == true)
        assertTrue(record.fields["StaticProp"]?.isStatic == true)
        assertFalse(record.fields["pub"]?.isStatic == true)

        val stat = record.methods["Stat"]
        assertNotNull(stat)
        assertTrue(stat.isStatic)
        // A static method has no instance, so no receiver
        assertNull(stat.receiver)

        val inst = record.methods["Inst"]
        assertNotNull(inst)
        assertFalse(inst.isStatic)
        assertNotNull(inst.receiver)
    }

    @Test
    fun testAccessorKeepsPropertyStaticAndNarrowsAccess() {
        val tu = translate("Modifiers.cs")
        val record = tu.records["Modifiers"]
        assertNotNull(record)

        val staticGetter =
            record.methods.firstOrNull { it.name.localName.contains("get_StaticProp") }
        assertNotNull(staticGetter)
        assertTrue(staticGetter.isStatic)

        val getter = record.methods.firstOrNull { it.name.localName.contains("get_Prop") }
        val setter = record.methods.firstOrNull { it.name.localName.contains("set_Prop") }
        assertNotNull(getter)
        assertNotNull(setter)
        assertEquals(Visibility.PUBLIC, getter.visibility)
        // `private set` narrows the access of the property
        assertEquals(Visibility.PRIVATE, setter.visibility)
    }

    @Test
    fun testConstructorHasReceiver() {
        val tu = translate("Modifiers.cs")
        val constructor = tu.records["Modifiers"]?.constructors?.singleOrNull()
        assertNotNull(constructor)
        val receiver = constructor.receiver
        assertNotNull(receiver)

        // `this.pub = x` must refer to the receiver and to the declared field, not to an
        // inferred one
        val access = constructor.nodes.filterIsInstance<MemberAccess>().single()
        val base = access.base
        assertIs<Reference>(base)
        assertRefersTo(base, receiver)
        assertEquals(tu.records["Modifiers"]?.fields["pub"], access.refersTo)
    }

    @Test
    fun testStaticUsing() {
        val tu = translate("Modifiers.cs")
        val imports = tu.imports
        val system = imports.single { it.import.toString() == "System" }
        val math = imports.single { it.import.toString() == "System.Math" }
        assertFalse(system.isStatic)
        assertTrue(math.isStatic)
        assertIs<Import>(math)
    }

    @Test
    fun testVarHasUnknownTypeAndResolvesOverload() {
        val tu = translate("Modifiers.cs")
        val call = tu.records["Modifiers"]?.methods["Call"]
        assertNotNull(call)

        val v = call.variables["v"]
        assertNotNull(v)
        // `var` takes the type of its initializer
        assertLocalName("int", v.type)

        // `Over(v)` resolves to the declared `Over(int)`, not to an inferred overload
        val invoked = call.calls["Over"]?.invokes?.singleOrNull()
        assertIs<Method>(invoked)
        assertFalse(invoked.isInferred)
        assertEquals("int", invoked.parameters.single().type.name.localName)
    }

    @Test
    fun testIntIs32Bit() {
        val tu = translate("Modifiers.cs")
        val add = tu.records["Modifiers"]?.methods["Add"]
        assertNotNull(add)
        val sum = add.nodes.filterIsInstance<BinaryOperator>().single()
        // int + long is a long
        assertLocalName("long", sum.type)
    }

    @Test
    fun testShortCircuitOperators() {
        val tu = translate("Modifiers.cs")
        val method = tu.records["Modifiers"]?.methods["ShortCircuit"]
        assertNotNull(method)

        // o != null && s == null || s != null
        val ret = method.body.let { it as Block }
        val or = (ret.statements.single() as Return).returnValue
        assertIs<ShortCircuitOperator>(or)
        assertEquals("||", or.operatorCode)
        val and = or.lhs
        assertIs<ShortCircuitOperator>(and)
        assertEquals("&&", and.operatorCode)
        // the comparisons are not short-circuiting
        val comparison = and.lhs
        assertIs<BinaryOperator>(comparison)
        assertFalse(comparison is ShortCircuitOperator)
    }

    @Test
    fun testNullCoalescingIsShortCircuiting() {
        val tu = translate("Modifiers.cs")
        val method = tu.records["Modifiers"]?.methods["Coalesce"]
        assertNotNull(method)
        val coalesce = method.nodes.filterIsInstance<BinaryOperator>().single()
        assertIs<ShortCircuitOperator>(coalesce)
        assertEquals("??", coalesce.operatorCode)
    }
}
