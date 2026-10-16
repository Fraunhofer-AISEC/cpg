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
package de.fraunhofer.aisec.cpg.frontends.java

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.declarations.EnumConstant
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.fields
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.methods
import de.fraunhofer.aisec.cpg.graph.records
import de.fraunhofer.aisec.cpg.graph.variables
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StaticCallsTest {
    @Test
    fun testStaticCalls() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/StaticCalls.java"))
                .defaultPasses()
                .registerLanguage<JavaLanguage>()
                .build()
        val result = TranslationManager.builder().config(config).build().analyze().get()

        val asList = result.calls["asList"]
        assertIs<MemberCall>(asList)
        assertTrue(asList.isStatic)
        assertEquals("java.util.Arrays", asList.base?.type?.name.toString())
        assertEquals("java.util.Arrays.asList", asList.invokes.singleOrNull()?.name.toString())
        assertTrue((asList.base as? Reference)?.isStaticAccess == true)

        val doSomething = result.calls["doSomething"]
        assertIs<MemberCall>(doSomething)
        assertTrue(doSomething.isStatic)
        assertEquals("com.example.Util", doSomething.base?.type?.name.toString())
        assertEquals(
            "com.example.Util.doSomething",
            doSomething.invokes.singleOrNull()?.name.toString(),
        )

        val helper = result.calls["helper"]
        assertNotNull(helper)
        assertEquals(result.methods["helper"], helper.invokes.singleOrNull())

        // a call on a variable is not static
        val size = result.calls["size"]
        assertIs<MemberCall>(size)
        assertTrue(!size.isStatic)

        // we must not infer fields for the type names used as scope
        assertTrue(result.fields.none { it.name.localName in listOf("Arrays", "Util") })
    }

    @Test
    fun testStaticFields() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/StaticCalls.java"))
                .defaultPasses()
                .registerLanguage<JavaLanguage>()
                .build()
        val result = TranslationManager.builder().config(config).build().analyze().get()

        // A static field of a type that we only know from its import is a static reference to
        // the field, not a member access on a field with the name of the type
        val constant = result.variables["constant"]?.initializer
        assertIs<Reference>(constant)
        assertEquals("com.example.Util.CONSTANT", constant.name.toString())
        assertTrue(constant.isStaticAccess)

        // The same is true for a type in java.lang, which JavaParser can resolve
        val max = result.variables["max"]?.initializer
        assertIs<Reference>(max)
        assertEquals("java.lang.Integer.MAX_VALUE", max.name.toString())
        assertTrue(max.isStaticAccess)
        assertEquals("int", max.type.name.toString())
        // The field is not part of our graph, so it is inferred, but with the known type
        val maxField = max.refersTo
        assertIs<Field>(maxField)
        assertTrue(maxField.isInferred)
        assertEquals("int", maxField.type.name.toString())

        // A method reference to a static method in java.lang is a static call
        val parseBoolean = result.calls["parseBoolean"]
        assertIs<MemberCall>(parseBoolean)
        assertTrue(parseBoolean.isStatic)
        assertEquals("java.lang.Boolean", parseBoolean.base?.type?.name.toString())

        // An enum constant is a static access to the existing enum constant, and the call on it
        // is resolved on the enum type
        val red = result.calls["name"]?.let { (it as? MemberCall)?.base }
        assertIs<Reference>(red)
        assertTrue(red.isStaticAccess)
        assertIs<EnumConstant>(red.refersTo)
        assertEquals("StaticCalls.Color", red.type.name.toString())

        // We must not infer fields for any of the type names
        val record = result.records["StaticCalls"]
        assertNotNull(record)
        assertTrue(record.fields.none { it.isInferred })
    }
}
