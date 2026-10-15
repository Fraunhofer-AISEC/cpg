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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for the canonical, language-independent [Visibility] data model. */
class VisibilityTest {
    @Test
    fun testDefaultVisibilityIsUnknown() {
        with(TestLanguageFrontend()) {
            // A freshly-built declaration must not carry any visibility restriction, so that
            // languages which do not model visibility are unaffected.
            val field = newField("field")
            assertEquals(Visibility.UNKNOWN, field.visibility)
            assertFalse(field.hasInternalLinkage)
        }
    }

    @Test
    fun testInternalLinkageHelper() {
        with(TestLanguageFrontend()) {
            val internal = newVariable("internal").apply { visibility = Visibility.INTERNAL }
            val exported = newVariable("exported").apply { visibility = Visibility.PUBLIC }

            assertTrue(internal.hasInternalLinkage)
            assertFalse(exported.hasInternalLinkage)
        }
    }

    /**
     * The static-vs-instance axis is modeled separately from [Visibility]. [newField] must expose
     * `isStatic` just like [newMethod] does, so that a field's static-ness no longer has to be
     * encoded as a `"static"` string in [HasModifiers.modifiers].
     */
    @Test
    fun testFieldIsStaticIsConsistentWithMethod() {
        with(TestLanguageFrontend()) {
            val staticField = newField("counter", isStatic = true)
            val instanceField = newField("name")
            val staticMethod = newMethod("create", isStatic = true)

            assertTrue(staticField.isStatic)
            assertFalse(instanceField.isStatic)
            assertTrue(staticMethod.isStatic)
        }
    }
}
