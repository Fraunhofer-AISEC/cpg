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

import de.fraunhofer.aisec.cpg.frontends.DeclarationContext
import de.fraunhofer.aisec.cpg.frontends.KeywordSemantics
import de.fraunhofer.aisec.cpg.graph.Visibility
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests that the C/C++ languages map declaration keywords to the correct canonical
 * [KeywordSemantics], with special attention to the context-dependence of `static`.
 */
class CLanguageKeywordSemanticsTest {
    @Test
    fun testStaticIsContextDependent() {
        val c = CLanguage()

        // At file/namespace scope `static` means internal linkage, not a static member.
        assertEquals(
            KeywordSemantics(visibility = Visibility.INTERNAL),
            c.interpretKeyword(STATIC, DeclarationContext.GLOBAL),
        )

        // On a record member `static` means a class-level (non-instance) member.
        assertEquals(
            KeywordSemantics(isStatic = true),
            c.interpretKeyword(STATIC, DeclarationContext.RECORD),
        )

        // Inside a function `static` only affects storage duration, which is irrelevant here.
        assertEquals(KeywordSemantics(), c.interpretKeyword(STATIC, DeclarationContext.LOCAL))
    }

    @Test
    fun testAccessSpecifiersMapToVisibilityInCpp() {
        // Access control is a C++ concept; only C++ maps the access specifiers to a visibility.
        val cpp = CPPLanguage()

        assertEquals(
            KeywordSemantics(visibility = Visibility.PUBLIC),
            cpp.interpretKeyword(PUBLIC, DeclarationContext.RECORD),
        )
        assertEquals(
            KeywordSemantics(visibility = Visibility.PROTECTED),
            cpp.interpretKeyword(PROTECTED, DeclarationContext.RECORD),
        )
        assertEquals(
            KeywordSemantics(visibility = Visibility.PRIVATE),
            cpp.interpretKeyword(PRIVATE, DeclarationContext.RECORD),
        )
    }

    @Test
    fun testCDoesNotModelAccessControl() {
        // C has no access control, so the access specifiers carry no canonical semantics there.
        val c = CLanguage()

        assertEquals(KeywordSemantics(), c.interpretKeyword(PUBLIC, DeclarationContext.RECORD))
        assertEquals(KeywordSemantics(), c.interpretKeyword(PROTECTED, DeclarationContext.RECORD))
        assertEquals(KeywordSemantics(), c.interpretKeyword(PRIVATE, DeclarationContext.RECORD))
    }

    @Test
    fun testUnknownKeywordIsEmpty() {
        val c = CLanguage()
        assertEquals(KeywordSemantics(), c.interpretKeyword("volatile", DeclarationContext.LOCAL))
    }

    @Test
    fun testCPPInheritsTheSameMapping() {
        // C++ must interpret `static` exactly like C, since the trait is inherited unchanged.
        val cpp = CPPLanguage()
        assertEquals(
            KeywordSemantics(visibility = Visibility.INTERNAL),
            cpp.interpretKeyword(STATIC, DeclarationContext.GLOBAL),
        )
        assertEquals(
            KeywordSemantics(isStatic = true),
            cpp.interpretKeyword(STATIC, DeclarationContext.RECORD),
        )
    }

    @Test
    fun testMergeAccumulatesOpinions() {
        // Later, non-null opinions win; silent axes leave the previous value untouched.
        val merged =
            KeywordSemantics(visibility = Visibility.INTERNAL)
                .merge(KeywordSemantics(isStatic = true))
        assertEquals(KeywordSemantics(visibility = Visibility.INTERNAL, isStatic = true), merged)
    }
}
