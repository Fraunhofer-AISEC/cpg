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
package de.fraunhofer.aisec.cpg.enhancements.types

import de.fraunhofer.aisec.cpg.InferenceConfiguration.Companion.builder
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.NumericType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.variables
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import java.nio.file.Path
import kotlin.test.*

internal class TypedefTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "typedefs")

    @Test
    @Throws(Exception::class)
    fun testSingle() {
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("typedefs.cpp").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        with(tu) {
            // normal type
            val l1 = tu.variables["l1"]
            val l2 = tu.variables["l2"]
            assertEquals(l1?.type, l2?.type)

            // pointer
            val longptr1 = tu.variables["longptr1"]
            val longptr2 = tu.variables["longptr2"]
            assertEquals(longptr1?.type, longptr2?.type)

            // array
            val arr1 = tu.variables["arr1"]
            val arr2 = tu.variables["arr2"]
            assertEquals(arr1?.type, arr2?.type)

            // function pointer
            val uintfp1 = tu.variables["uintfp1"]
            val uintfp2 = tu.variables["uintfp2"]

            val fpType = uintfp1?.type as? FunctionPointerType
            assertNotNull(fpType)

            val returnType = fpType.returnType as? NumericType
            assertNotNull(returnType)
            assertEquals(NumericType.Modifier.UNSIGNED, returnType.modifier)
            assertEquals(uintfp1.type, uintfp2?.type)

            val type = tu.ctx.scopeManager.typedefFor(Name("test"))
            assertIs<IntegerType>(type)
            assertLocalName("uint8_t", type)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testWithModifier() {
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("typedefs.cpp").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }

        // pointer
        val l1ptr = tu.variables["l1ptr"]
        val l2ptr = tu.variables["l2ptr"]
        val l3ptr = tu.variables["l3ptr"]
        val l4ptr = tu.variables["l4ptr"]
        assertEquals(l1ptr?.type, l2ptr?.type)
        assertEquals(l1ptr?.type, l3ptr?.type)
        assertEquals(l1ptr?.type, l4ptr?.type)
    }

    @Test
    @Throws(Exception::class)
    fun testChained() {
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("typedefs.cpp").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }

        val l1 = tu.variables["l1"]
        val l3 = tu.variables["l3"]
        val l4 = tu.variables["l4"]
        assertEquals(l1?.type, l3?.type)
        assertEquals(l1?.type, l4?.type)
    }

    @Test
    @Throws(Exception::class)
    fun testMultiple() {
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("typedefs.cpp").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        with(tu) {
            // simple type
            val i1 = tu.variables["i1"]
            val i2 = tu.variables["i2"]
            assertEquals(i1?.type, i2?.type)

            // array
            val a1 = tu.variables["a1"]
            val a2 = tu.variables["a2"]
            assertEquals(a1?.type, a2?.type)

            // pointer
            val intPtr1 = tu.variables["intPtr1"]
            val intPtr2 = tu.variables["intPtr2"]
            assertEquals(intPtr1?.type, intPtr2?.type)

            // function pointer
            val fPtr1 = tu.variables["intFptr1"]
            val fPtr2 = tu.variables["intFptr2"]
            assertEquals(fPtr1?.type, fPtr2?.type)

            val type = tu.ctx.scopeManager.typedefFor(Name("type_B"))
            assertLocalName("template_class_A", type)
            assertIs<ObjectType>(type)
            assertEquals(listOf(primitiveType("int"), primitiveType("int")), type.generics)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testStructs() {
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("typedefs.cpp").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }

        val ps1 = tu.variables["ps1"]
        val ps2 = tu.variables["ps2"]
        assertEquals(ps1?.type, ps2?.type)
    }

    @Test
    @Throws(Exception::class)
    fun testArbitraryTypedefLocation() {
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("weird_typedefs.cpp").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }

        val ullong1 = tu.variables["someUllong1"]
        assertNotNull(ullong1)

        val ullong2 = tu.variables["someUllong2"]
        assertNotNull(ullong2)
        assertEquals(ullong1.type, ullong2.type)

        val records = tu.records
        assertEquals(2, records.size)
        assertEquals(listOf("bar", "foo"), records.map { it.name.localName })
    }

    @Test
    @Throws(Exception::class)
    fun testMemberTypeDef() {
        val result =
            analyze(listOf(topLevel.resolve("typedefs.cpp").toFile()), topLevel, true) {
                it.registerLanguage<CPPLanguage>()
            }

        val addConst = result.records["add_const"]
        val typeMember1: ValueDeclaration = findByUniqueName(addConst.fields, "typeMember1")
        val typeMember2: ValueDeclaration = findByUniqueName(addConst.fields, "typeMember2")
        assertEquals(typeMember1.type, typeMember2.type)

        val typeMemberOutside = result.variables["typeMemberOutside"]
        assertNotEquals(typeMemberOutside?.type, typeMember2.type)

        val cptr1 = result.variables["cptr1"]
        val cptr2 = result.variables["cptr2"]
        assertEquals(cptr1?.type, cptr2?.type)
        assertNotEquals(typeMemberOutside?.type, cptr2?.type)
    }

    @Test
    fun testTypedefInClass() {
        val result =
            analyze(listOf(topLevel.resolve("typedef_in_class.cpp").toFile()), topLevel, true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)

        val someDataClass = result.records["SomeDataClass"]
        assertNotNull(someDataClass)

        val baseClass = result.records["BaseClass"]
        assertNotNull(baseClass)

        val sizeField = baseClass.fields["size"]
        assertNotNull(sizeField)
        assertFalse(sizeField.isInferred)

        val size = result.memberExpressions["size"]
        assertNotNull(size)
        assertRefersTo(size, sizeField)
    }

    @Test
    fun testTypedefStructCPP() {
        val file = File("src/test/resources/cxx/typedef_struct.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.inferenceConfiguration(builder().enabled(false).build())
            }
        with(tu) {
            val me = tu.memberExpressions
            me.forEach { assertNotNull(it.refersTo) }

            val test = tu.records.singleOrNull()
            assertNotNull(test)
            assertLocalName("test", test)
        }
    }

    @Test
    fun testTypedefStructC() {
        val file = File("src/test/resources/c/typedef_struct.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.inferenceConfiguration(builder().enabled(false).build())
            }
        with(tu) {
            val me = tu.memberExpressions
            me.forEach { assertNotNull(it.refersTo) }

            val test = tu.records.singleOrNull()
            assertNotNull(test)
            assertLocalName("test", test)
        }
    }
}
