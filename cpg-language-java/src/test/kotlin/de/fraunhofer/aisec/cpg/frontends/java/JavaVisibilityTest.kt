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
package de.fraunhofer.aisec.cpg.frontends.java

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class JavaVisibilityTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "java", "visibility")

    private fun analyzeFixture() =
        analyze(listOf(topLevel.resolve("Visibility.java").toFile()), topLevel, true) {
            it.registerLanguage<JavaLanguage>()
        }

    private fun recordByLocalName(result: List<Record>, name: String): Record {
        val record = result.singleOrNull { it.name.localName == name }
        assertNotNull(record, "expected exactly one record named $name")
        return record
    }

    @Test
    fun testFieldVisibility() {
        val result = analyzeFixture()
        val visibility = recordByLocalName(result.records, "Visibility")

        assertEquals(Visibility.PUBLIC, visibility.fields["publicField"]?.visibility)
        assertEquals(Visibility.PROTECTED, visibility.fields["protectedField"]?.visibility)
        assertEquals(Visibility.PRIVATE, visibility.fields["privateField"]?.visibility)
        // The tricky default: a field without an access modifier is package-private.
        assertEquals(Visibility.PACKAGE, visibility.fields["packageField"]?.visibility)
    }

    @Test
    fun testStaticFieldVisibility() {
        val result = analyzeFixture()
        val visibility = recordByLocalName(result.records, "Visibility")

        val publicStatic = visibility.fields["publicStaticField"]
        assertNotNull(publicStatic)
        assertEquals(Visibility.PUBLIC, publicStatic.visibility)
        assertEquals(true, publicStatic.isStatic)
        assertContains(publicStatic.modifiers, "static")

        // No access modifier, but `static`: still package-private and static.
        val packageStatic = visibility.fields["packageStaticField"]
        assertNotNull(packageStatic)
        assertEquals(Visibility.PACKAGE, packageStatic.visibility)
        assertEquals(true, packageStatic.isStatic)
    }

    @Test
    fun testMethodVisibility() {
        val result = analyzeFixture()
        val visibility = recordByLocalName(result.records, "Visibility")

        assertEquals(Visibility.PUBLIC, visibility.methods["publicMethod"]?.visibility)
        assertEquals(Visibility.PROTECTED, visibility.methods["protectedMethod"]?.visibility)
        assertEquals(Visibility.PRIVATE, visibility.methods["privateMethod"]?.visibility)
        assertEquals(Visibility.PACKAGE, visibility.methods["packageMethod"]?.visibility)

        val publicStatic = visibility.methods["publicStaticMethod"]
        assertNotNull(publicStatic)
        assertEquals(Visibility.PUBLIC, publicStatic.visibility)
        assertEquals(true, publicStatic.isStatic)

        val packageStatic = visibility.methods["packageStaticMethod"]
        assertNotNull(packageStatic)
        assertEquals(Visibility.PACKAGE, packageStatic.visibility)
        assertEquals(true, packageStatic.isStatic)
    }

    @Test
    fun testConstructorVisibility() {
        val result = analyzeFixture()
        val visibility = recordByLocalName(result.records, "Visibility")

        val publicCtor = visibility.constructors.singleOrNull { it.parameters.isEmpty() }
        assertNotNull(publicCtor)
        assertEquals(Visibility.PUBLIC, publicCtor.visibility)

        // The second, one-argument constructor has no access modifier -> package-private.
        val packageCtor = visibility.constructors.singleOrNull { it.parameters.size == 1 }
        assertNotNull(packageCtor)
        assertEquals(Visibility.PACKAGE, packageCtor.visibility)
    }

    @Test
    fun testNestedTypeVisibility() {
        val result = analyzeFixture()

        assertEquals(Visibility.PUBLIC, recordByLocalName(result.records, "PublicInner").visibility)
        assertEquals(
            Visibility.PROTECTED,
            recordByLocalName(result.records, "ProtectedInner").visibility,
        )
        assertEquals(
            Visibility.PRIVATE,
            recordByLocalName(result.records, "PrivateInner").visibility,
        )
        assertEquals(
            Visibility.PACKAGE,
            recordByLocalName(result.records, "PackageInner").visibility,
        )
        assertEquals(Visibility.PUBLIC, recordByLocalName(result.records, "PublicEnum").visibility)
        assertEquals(
            Visibility.PACKAGE,
            recordByLocalName(result.records, "PackageEnum").visibility,
        )
    }

    @Test
    fun testTopLevelTypeVisibility() {
        val result = analyzeFixture()

        // A public top-level class.
        assertEquals(Visibility.PUBLIC, recordByLocalName(result.records, "Visibility").visibility)

        // A top-level class without an access modifier is package-private, not public.
        assertEquals(
            Visibility.PACKAGE,
            recordByLocalName(result.records, "PackagePrivateTopLevel").visibility,
        )
    }
}
