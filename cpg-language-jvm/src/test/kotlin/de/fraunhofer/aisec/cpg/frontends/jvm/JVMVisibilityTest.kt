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
package de.fraunhofer.aisec.cpg.frontends.jvm

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests that the JVM frontend maps bytecode access flags (`ACC_PUBLIC`, `ACC_PROTECTED`,
 * `ACC_PRIVATE`, the absence of all three, and `ACC_STATIC`) onto the canonical [Visibility] model,
 * while keeping the raw modifiers losslessly.
 */
class JVMVisibilityTest {
    @Test
    fun testAccessFlagsToVisibility() {
        val topLevel = Path.of("src", "test", "resources", "class", "visibility")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("mypackage/Visibility.class").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(tu)
        assertEquals(0, tu.problems.size)

        // A public top-level class maps to PUBLIC and keeps its raw "public" modifier.
        val visibility = tu.records["mypackage.Visibility"]
        assertNotNull(visibility)
        assertEquals(Visibility.PUBLIC, visibility.visibility)
        assertContains(visibility.modifiers, PUBLIC)

        // A top-level class without any access flag is package-private -> PACKAGE.
        val packagePrivate = tu.records["mypackage.PackagePrivate"]
        assertNotNull(packagePrivate)
        assertEquals(Visibility.PACKAGE, packagePrivate.visibility)
        assertFalse(packagePrivate.modifiers.contains(PUBLIC))

        // Fields: cover every access-control value plus the static flag.
        val publicField = visibility.fields["publicField"]
        assertNotNull(publicField)
        assertEquals(Visibility.PUBLIC, publicField.visibility)
        assertFalse(publicField.isStatic)

        val protectedField = visibility.fields["protectedField"]
        assertNotNull(protectedField)
        assertEquals(Visibility.PROTECTED, protectedField.visibility)

        val privateField = visibility.fields["privateField"]
        assertNotNull(privateField)
        assertEquals(Visibility.PRIVATE, privateField.visibility)

        // The tricky default: no access flag -> PACKAGE.
        val packageField = visibility.fields["packageField"]
        assertNotNull(packageField)
        assertEquals(Visibility.PACKAGE, packageField.visibility)

        val staticField = visibility.fields["staticField"]
        assertNotNull(staticField)
        assertEquals(Visibility.PUBLIC, staticField.visibility)
        assertTrue(staticField.isStatic)
        assertContains(staticField.modifiers, STATIC)

        // Methods: cover every access-control value plus the static flag.
        val publicMethod = visibility.methods["publicMethod"]
        assertNotNull(publicMethod)
        assertEquals(Visibility.PUBLIC, publicMethod.visibility)
        assertFalse(publicMethod.isStatic)

        val protectedMethod = visibility.methods["protectedMethod"]
        assertNotNull(protectedMethod)
        assertEquals(Visibility.PROTECTED, protectedMethod.visibility)

        val privateMethod = visibility.methods["privateMethod"]
        assertNotNull(privateMethod)
        assertEquals(Visibility.PRIVATE, privateMethod.visibility)

        val packageMethod = visibility.methods["packageMethod"]
        assertNotNull(packageMethod)
        assertEquals(Visibility.PACKAGE, packageMethod.visibility)

        val staticMethod = visibility.methods["staticMethod"]
        assertNotNull(staticMethod)
        assertEquals(Visibility.PUBLIC, staticMethod.visibility)
        assertTrue(staticMethod.isStatic)
        assertContains(staticMethod.modifiers, STATIC)
    }
}
