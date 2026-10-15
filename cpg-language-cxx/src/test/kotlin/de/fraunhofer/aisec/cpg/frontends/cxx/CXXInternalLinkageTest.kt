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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests that a declaration with internal linkage (a file-scope `static` in C) is confined to its
 * own translation unit during symbol resolution, while externally-linked declarations still resolve
 * across translation units. See [de.fraunhofer.aisec.cpg.graph.Visibility.INTERNAL].
 */
class CXXInternalLinkageTest {
    @Test
    fun testStaticGlobalsDoNotLeakAcrossTranslationUnits() {
        val topLevel = Path.of("src", "test", "resources", "c", "internal_linkage")
        val result =
            analyze(listOf(File("$topLevel/a.c"), File("$topLevel/b.c")), topLevel, true) {
                it.registerLanguage<CLanguage>()
            }

        // There are two independent, internal-linkage `secret` variables, one per translation unit.
        val secrets = result.variables.filter { it.name.localName == "secret" }
        assertEquals(2, secrets.size)
        assertTrue(secrets.all { it.visibility == Visibility.INTERNAL })

        val secretInA = secrets.single { it.translationUnit?.name.toString().endsWith("a.c") }
        val secretInB = secrets.single { it.translationUnit?.name.toString().endsWith("b.c") }

        // The `secret` reference in each translation unit must resolve to *its own* `secret`, never
        // to the identically-named one in the other translation unit.
        val readSecretA = result.functions["readSecretA"]
        assertNotNull(readSecretA)
        val refInA = readSecretA.refs.single { it.name.localName == "secret" }
        assertEquals(secretInA, refInA.refersTo, "reference in a.c must resolve to a.c's secret")

        val readSecretB = result.functions["readSecretB"]
        assertNotNull(readSecretB)
        val refInB = readSecretB.refs.single { it.name.localName == "secret" }
        assertEquals(secretInB, refInB.refersTo, "reference in b.c must resolve to b.c's secret")
    }

    @Test
    fun testStaticFunctionsDoNotLeakAcrossTranslationUnits() {
        val topLevel = Path.of("src", "test", "resources", "c", "internal_linkage")
        val result =
            analyze(listOf(File("$topLevel/a.c"), File("$topLevel/b.c")), topLevel, true) {
                it.registerLanguage<CLanguage>()
            }

        // There are two independent, internal-linkage `helper` functions, one per translation unit.
        val helpers = result.functions.filter { it.name.localName == "helper" }
        assertEquals(2, helpers.size)
        assertTrue(helpers.all { it.visibility == Visibility.INTERNAL })

        val helperInA = helpers.single { it.translationUnit?.name.toString().endsWith("a.c") }
        val helperInB = helpers.single { it.translationUnit?.name.toString().endsWith("b.c") }

        // The `helper()` call in each translation unit must invoke *its own* `helper`, never the
        // identically-named internal-linkage one in the other translation unit.
        val callHelperA = result.functions["callHelperA"]
        assertNotNull(callHelperA)
        val callInA = callHelperA.calls["helper"]
        assertNotNull(callInA)
        assertEquals(
            listOf(helperInA),
            callInA.invokes,
            "call in a.c must resolve to a.c's helper only",
        )

        val callHelperB = result.functions["callHelperB"]
        assertNotNull(callHelperB)
        val callInB = callHelperB.calls["helper"]
        assertNotNull(callInB)
        assertEquals(
            listOf(helperInB),
            callInB.invokes,
            "call in b.c must resolve to b.c's helper only",
        )
    }

    @Test
    fun testExternalFunctionsStillResolveAcrossTranslationUnits() {
        val topLevel = Path.of("src", "test", "resources", "c", "internal_linkage")
        val result =
            analyze(listOf(File("$topLevel/a.c"), File("$topLevel/b.c")), topLevel, true) {
                it.registerLanguage<CLanguage>()
            }

        // `shared` has external linkage and is defined in a.c; the call in b.c's `useShared` must
        // resolve to it across translation units, i.e. internal-linkage filtering must not affect
        // externally-linked declarations.
        val shared = result.functions["shared"]
        assertNotNull(shared)
        assertEquals(Visibility.UNKNOWN, shared.visibility)

        val call = result.calls["shared"]
        assertNotNull(call)
        assertTrue(shared in call.invokes, "cross-translation-unit call to `shared` must resolve")
    }
}
