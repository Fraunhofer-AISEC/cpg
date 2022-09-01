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
package de.fraunhofer.aisec.cpg.graph.types

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.TestUtils.findByUniquePredicate
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.records
import de.fraunhofer.aisec.cpg.graph.variables
import java.nio.file.Path
import kotlin.test.*

internal class TypedefTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "typedefs")

    @Test
    @Throws(Exception::class)
    fun testSingle() {
        val result = analyze("cpp", topLevel, true)
        val variables = result.variables

        // normal type
        val l1 = findByUniqueName(variables, "l1")
        val l2 = findByUniqueName(variables, "l2")
        assertEquals(l1.type, l2.type)

        // pointer
        val longptr1 = findByUniqueName(variables, "longptr1")
        val longptr2 = findByUniqueName(variables, "longptr2")
        assertEquals(longptr1.type, longptr2.type)

        // array
        val arr1 = findByUniqueName(variables, "arr1")
        val arr2 = findByUniqueName(variables, "arr2")
        assertEquals(arr1.type, arr2.type)

        // function pointer
        val uintfp1 = findByUniqueName(variables, "uintfp1")
        val uintfp2 = findByUniqueName(variables, "uintfp2")

        val fpType = uintfp1.type as? FunctionPointerType
        assertNotNull(fpType)

        val returnType = fpType.returnType as? ObjectType
        assertNotNull(returnType)
        assertEquals(ObjectType.Modifier.UNSIGNED, returnType.modifier)
        assertEquals(uintfp1.type, uintfp2.type)

        val frontend = TypeManager.getInstance().frontend
        assertNotNull(frontend)

        val typedefs = frontend.scopeManager.currentTypedefs
        val def = typedefs.stream().filter { it.alias.name == "test" }.findAny().orElse(null)
        assertNotNull(def)
    }

    @Test
    @Throws(Exception::class)
    fun testWithModifier() {
        val result = analyze("cpp", topLevel, true)
        val variables = result.variables

        // pointer
        val l1ptr = findByUniqueName(variables, "l1ptr")
        val l2ptr = findByUniqueName(variables, "l2ptr")
        val l3ptr = findByUniqueName(variables, "l3ptr")
        val l4ptr = findByUniqueName(variables, "l4ptr")
        assertEquals(l1ptr.type, l2ptr.type)
        assertEquals(l1ptr.type, l3ptr.type)
        assertEquals(l1ptr.type, l4ptr.type)

        // arrays
        val l1arr = findByUniqueName(variables, "l1arr")
        val l2arr = findByUniqueName(variables, "l2arr")
        val l3arr = findByUniqueName(variables, "l3arr")
        val l4arr = findByUniqueName(variables, "l4arr")
        assertTrue(TypeManager.getInstance().checkArrayAndPointer(l1arr.type, l2arr.type))
        assertTrue(TypeManager.getInstance().checkArrayAndPointer(l1arr.type, l3arr.type))
        assertTrue(TypeManager.getInstance().checkArrayAndPointer(l1arr.type, l4arr.type))
    }

    @Test
    @Throws(Exception::class)
    fun testChained() {
        val result = analyze("cpp", topLevel, true)
        val variables = result.variables
        val l1 = findByUniqueName(variables, "l1")
        val l3 = findByUniqueName(variables, "l3")
        val l4 = findByUniqueName(variables, "l4")
        assertEquals(l1.type, l3.type)
        assertEquals(l1.type, l4.type)
    }

    @Test
    @Throws(Exception::class)
    fun testMultiple() {
        val result = analyze("cpp", topLevel, true)
        val variables = result.variables

        // simple type
        val i1 = findByUniqueName(variables, "i1")
        val i2 = findByUniqueName(variables, "i2")
        assertEquals(i1.type, i2.type)

        // array
        val a1 = findByUniqueName(variables, "a1")
        val a2 = findByUniqueName(variables, "a2")
        assertEquals(a1.type, a2.type)

        // pointer
        val intPtr1 = findByUniqueName(variables, "intPtr1")
        val intPtr2 = findByUniqueName(variables, "intPtr2")
        assertEquals(intPtr1.type, intPtr2.type)

        // function pointer
        val fPtr1 = findByUniqueName(variables, "intFptr1")
        val fPtr2 = findByUniqueName(variables, "intFptr2")
        assertEquals(fPtr1.type, fPtr2.type)

        // template, not to be confused with multiple typedef
        val template =
            findByUniquePredicate(result.translationUnits.firstOrNull()?.typedefs ?: listOf()) {
                it.type.typeName == "template_class_A"
            }
        assertEquals(template.alias.typeName, "type_B")
    }

    @Test
    @Throws(Exception::class)
    fun testStructs() {
        val result = analyze("cpp", topLevel, true)
        val variables = result.variables
        val ps1 = findByUniqueName(variables, "ps1")
        val ps2 = findByUniqueName(variables, "ps2")
        assertEquals(ps1.type, ps2.type)
    }

    @Test
    @Throws(Exception::class)
    fun testArbitraryTypedefLocation() {
        val result = analyze("cpp", topLevel, true)
        val variables = result.variables
        val ullong1 = findByUniqueName(variables, "someUllong1")
        val ullong2 = findByUniqueName(variables, "someUllong2")
        assertEquals(ullong1.type, ullong2.type)
    }

    @Test
    @Throws(Exception::class)
    fun testMemberTypeDef() {
        val result = analyze("cpp", topLevel, true)
        val variables = result.variables
        val records = result.records
        val addConst = findByUniqueName(records, "add_const")
        val typeMember1: ValueDeclaration = findByUniqueName(addConst.fields, "typeMember1")
        val typeMember2: ValueDeclaration = findByUniqueName(addConst.fields, "typeMember2")
        assertEquals(typeMember1.type, typeMember2.type)

        val typeMemberOutside = findByUniqueName(variables, "typeMemberOutside")
        assertNotEquals(typeMemberOutside.type, typeMember2.type)

        val cptr1 = findByUniqueName(variables, "cptr1")
        val cptr2 = findByUniqueName(variables, "cptr2")
        assertEquals(cptr1.type, cptr2.type)
        assertNotEquals(typeMemberOutside.type, cptr2.type)
    }
}
