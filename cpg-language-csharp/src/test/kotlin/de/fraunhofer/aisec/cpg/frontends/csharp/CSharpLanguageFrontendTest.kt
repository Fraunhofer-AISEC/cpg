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
package de.fraunhofer.aisec.cpg.frontends.csharp

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CSharpLanguageFrontendTest : BaseTest() {

    @Test
    fun testNamespaces() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("namespace.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.namespaces["Foo"]
        assertNotNull(foo)

        val bar = foo.namespaces["Bar"]
        assertNotNull(bar)

        val baz = bar.records["Baz"]
        assertNotNull(baz)

        val dottedNameSpace = tu.namespaces["Dotted.NameSpace"]
        assertNotNull(dottedNameSpace)
    }

    @Test
    fun testFileScopedNamespace() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("fileScoped_namespace.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val ns = tu.namespaces["HelloWorld"]
        assertNotNull(ns)

        val foo = ns.records["Foo"]
        assertNotNull(foo)

        assertEquals("bar", foo.fields["bar"]?.name?.localName)
    }

    @Test
    fun testFieldDeclarations() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("fields.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)
    }
}
