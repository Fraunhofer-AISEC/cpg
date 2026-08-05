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
 * while keeping the raw modifiers losslessly. It also pins the known limitation for *nested*
 * classes, whose real accessibility is not recoverable from their own ClassFile access flags.
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

        // A top-level class without any access flag is package-private -> PACKAGE. This is a
        // separate top-level class (its own PackagePrivate.class), not a nested class.
        val packagePrivate = tu.records["mypackage.PackagePrivate"]
        assertNotNull(packagePrivate)
        assertEquals(Visibility.PACKAGE, packagePrivate.visibility)
        assertFalse(packagePrivate.modifiers.contains(PUBLIC))

        // Nested classes: SootUp only reads a class' own ClassFile access flags, which never carry
        // ACC_PRIVATE/ACC_PROTECTED/ACC_STATIC -- a nested class' real accessibility lives in the
        // enclosing class' InnerClasses attribute, which SootUp does not surface. We therefore
        // cannot recover it and pin the resulting (limited) behavior here (see
        // DeclarationHandler.applyAccessFlags).

        // Real accessibility is `private`, but javac writes no access flag into the nested class'
        // own ClassFile, so it is reported as package-private instead of PRIVATE.
        val nestedPrivate = tu.records["mypackage.Visibility\$NestedPrivate"]
        assertNotNull(nestedPrivate)
        assertEquals(Visibility.PACKAGE, nestedPrivate.visibility)

        // Real accessibility is `protected`, but javac marks the nested class' own ClassFile
        // ACC_PUBLIC, so it is reported as PUBLIC instead of PROTECTED.
        val nestedProtected = tu.records["mypackage.Visibility\$NestedProtected"]
        assertNotNull(nestedProtected)
        assertEquals(Visibility.PUBLIC, nestedProtected.visibility)

        // A public nested class is the one case that survives: ACC_PUBLIC is present on its own
        // ClassFile, so it maps to PUBLIC correctly.
        val nestedStatic = tu.records["mypackage.Visibility\$NestedStatic"]
        assertNotNull(nestedStatic)
        assertEquals(Visibility.PUBLIC, nestedStatic.visibility)

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
