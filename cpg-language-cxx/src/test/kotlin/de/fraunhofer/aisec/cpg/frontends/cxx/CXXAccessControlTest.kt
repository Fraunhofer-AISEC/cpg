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
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests member access control (C++ `public` / `protected` / `private`): the frontend must map the
 * access specifiers onto the canonical [Visibility], and the
 * [de.fraunhofer.aisec.cpg.passes. SymbolResolver] must take that visibility into account when
 * resolving a member by name, dropping candidates that are inaccessible from the point of access.
 */
class CXXAccessControlTest {
    private fun analyze() =
        File("src/test/resources/cxx/access_control.cpp").let { file ->
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        }

    @Test
    fun testAccessSpecifiersMapToVisibility() {
        val tu = analyze()
        assertNotNull(tu)

        val base = tu.records["Base"]
        assertNotNull(base)

        assertEquals(Visibility.PUBLIC, base.methods["publicMethod"]?.visibility)
        assertEquals(Visibility.PROTECTED, base.methods["protectedMethod"]?.visibility)
        assertEquals(Visibility.PRIVATE, base.methods["privateMethod"]?.visibility)
        // `class` members default to private, even without an explicit specifier.
        assertEquals(Visibility.PRIVATE, base.fields["secret"]?.visibility)
    }

    @Test
    fun testPrivateMemberResolvesWithinItsOwnRecord() {
        val tu = analyze()
        assertNotNull(tu)

        val base = tu.records["Base"]
        assertNotNull(base)
        val secret = base.fields["secret"]
        assertNotNull(secret)

        // Inside `Base::privateMethod`, the reference to the private field `secret` must still
        // resolve: access control never hides a member from its own record.
        val privateMethod = base.methods["privateMethod"]
        assertNotNull(privateMethod)
        val ref = privateMethod.refs.single { it.name.localName == "secret" }
        assertEquals(secret, ref.refersTo)
    }

    @Test
    fun testProtectedMemberAccessibleFromSubclass() {
        val tu = analyze()
        assertNotNull(tu)

        val protectedMethod = tu.records["Base"]?.methods["protectedMethod"]
        assertNotNull(protectedMethod)

        // `Derived::useProtected` calls the protected `protectedMethod` inherited from `Base`. A
        // protected member is accessible from a subclass, so the call must resolve to it.
        val call = tu.calls["protectedMethod"]
        assertNotNull(call)
        assertEquals(protectedMethod, call.invokes.singleOrNull())
    }

    @Test
    fun testOutOfLineDefinitionInheritsVisibility() {
        val tu = analyze()
        assertNotNull(tu)

        // `OutOfLine::hidden` is declared `private` in the class and defined out-of-line. Both the
        // in-class declaration and the out-of-line definition must carry PRIVATE visibility, so the
        // resolver treats the member consistently regardless of which node it encounters.
        val hidden = tu.methods.filter { it.name.localName == "hidden" }
        assertEquals(2, hidden.size, "expected an out-of-line declaration and definition")
        assertTrue(hidden.all { it.visibility == Visibility.PRIVATE })
    }

    @Test
    fun testInaccessibleOverloadFilteredInMultipleInheritance() {
        val tu = analyze()
        assertNotNull(tu)

        val speakerPing = tu.records["Speaker"]?.methods["ping"]
        val mutedPing = tu.records["Muted"]?.methods["ping"]
        assertNotNull(speakerPing)
        assertNotNull(mutedPing)

        // `Combined` inherits a public `ping` from `Speaker` and a private one from `Muted`. The
        // external call `c->ping()` must resolve to the accessible `Speaker::ping` only, i.e. the
        // inaccessible private candidate is filtered out.
        val call = tu.calls["ping"]
        assertNotNull(call)
        assertEquals(speakerPing, call.invokes.singleOrNull())
    }
}
