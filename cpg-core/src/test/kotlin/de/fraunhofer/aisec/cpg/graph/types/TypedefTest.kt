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
import de.fraunhofer.aisec.cpg.TestUtils.findByUniquePredicate
import de.fraunhofer.aisec.cpg.graph.SearchModifier.UNIQUE
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.get
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
        val l1 = assertNotNull(variables["l1", UNIQUE])
        val l2 = assertNotNull(variables["l2", UNIQUE])
        assertEquals(l1.type, l2.type)

        // pointer
        val longptr1 = assertNotNull(variables["longptr1", UNIQUE])
        val longptr2 = assertNotNull(variables["longptr2", UNIQUE])
        assertEquals(longptr1.type, longptr2.type)

        // array
        val arr1 = assertNotNull(variables["arr1", UNIQUE])
        val arr2 = assertNotNull(variables["arr2", UNIQUE])
        assertEquals(arr1.type, arr2.type)

        // function pointer
        val uintfp1 = assertNotNull(variables["uintfp1", UNIQUE])
        val uintfp2 = assertNotNull(variables["uintfp2", UNIQUE])

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
        val l1ptr = assertNotNull(variables["l1ptr", UNIQUE])
        val l2ptr = assertNotNull(variables["l2ptr", UNIQUE])
        val l3ptr = assertNotNull(variables["l3ptr", UNIQUE])
        val l4ptr = assertNotNull(variables["l4ptr", UNIQUE])
        assertEquals(l1ptr.type, l2ptr.type)
        assertEquals(l1ptr.type, l3ptr.type)
        assertEquals(l1ptr.type, l4ptr.type)

        // arrays
        val l1arr = assertNotNull(variables["l1arr", UNIQUE])
        val l2arr = assertNotNull(variables["l2arr", UNIQUE])
        val l3arr = assertNotNull(variables["l3arr", UNIQUE])
        val l4arr = assertNotNull(variables["l4arr", UNIQUE])
        assertTrue(TypeManager.getInstance().checkArrayAndPointer(l1arr.type, l2arr.type))
        assertTrue(TypeManager.getInstance().checkArrayAndPointer(l1arr.type, l3arr.type))
        assertTrue(TypeManager.getInstance().checkArrayAndPointer(l1arr.type, l4arr.type))
    }

    @Test
    @Throws(Exception::class)
    fun testChained() {
        val result = analyze("cpp", topLevel, true)
        val variables = result.variables
        val l1 = assertNotNull(variables["l1", UNIQUE])
        val l3 = assertNotNull(variables["l3", UNIQUE])
        val l4 = assertNotNull(variables["l4", UNIQUE])
        assertEquals(l1.type, l3.type)
        assertEquals(l1.type, l4.type)
    }

    @Test
    @Throws(Exception::class)
    fun testMultiple() {
        val result = analyze("cpp", topLevel, true)
        val variables = result.variables

        // simple type
        val i1 = assertNotNull(variables["i1", UNIQUE])
        val i2 = assertNotNull(variables["i2", UNIQUE])
        assertEquals(i1.type, i2.type)

        // array
        val a1 = assertNotNull(variables["a1", UNIQUE])
        val a2 = assertNotNull(variables["a2", UNIQUE])
        assertEquals(a1.type, a2.type)

        // pointer
        val intPtr1 = assertNotNull(variables["intPtr1", UNIQUE])
        val intPtr2 = assertNotNull(variables["intPtr2", UNIQUE])
        assertEquals(intPtr1.type, intPtr2.type)

        // function pointer
        val fPtr1 = assertNotNull(variables["intFptr1", UNIQUE])
        val fPtr2 = assertNotNull(variables["intFptr2", UNIQUE])
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
        val ps1 = assertNotNull(variables["ps1", UNIQUE])
        val ps2 = assertNotNull(variables["ps2", UNIQUE])
        assertEquals(ps1.type, ps2.type)
    }

    @Test
    @Throws(Exception::class)
    fun testArbitraryTypedefLocation() {
        val result = analyze("cpp", topLevel, true)
        val variables = result.variables
        val ullong1 = assertNotNull(variables["someUllong1", UNIQUE])
        val ullong2 = assertNotNull(variables["someUllong2", UNIQUE])
        assertEquals(ullong1.type, ullong2.type)
    }

    @Test
    @Throws(Exception::class)
    fun testMemberTypeDef() {
        val result = analyze("cpp", topLevel, true)
        val variables = result.variables
        val records = result.records
        val addConst = assertNotNull(records["add_const", UNIQUE])
        val typeMember1: ValueDeclaration = assertNotNull(addConst.fields["typeMember1", UNIQUE])
        val typeMember2: ValueDeclaration = assertNotNull(addConst.fields["typeMember2", UNIQUE])
        assertEquals(typeMember1.type, typeMember2.type)

        val typeMemberOutside = assertNotNull(variables["typeMemberOutside", UNIQUE])
        assertNotEquals(typeMemberOutside.type, typeMember2.type)

        val cptr1 = assertNotNull(variables["cptr1", UNIQUE])
        val cptr2 = assertNotNull(variables["cptr2", UNIQUE])
        assertEquals(cptr1.type, cptr2.type)
        assertNotEquals(typeMemberOutside.type, cptr2.type)
    }
}
