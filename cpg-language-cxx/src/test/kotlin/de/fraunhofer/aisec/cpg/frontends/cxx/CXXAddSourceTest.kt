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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage

/**
 * Tests for [CXXLanguageFrontend]'s [de.fraunhofer.aisec.cpg.frontends.SupportsNewParse]
 * implementation, both in isolation and end-to-end via [TranslationManager.addSource].
 *
 * All tests here use self-contained content without relative `#include`s. As documented on
 * [CXXLanguageFrontend.parse], headers included relative to the *parsed file's own directory*
 * cannot be resolved for content added this way, since there is no real file on disk for CDT to
 * resolve such includes against; only includes resolvable via the component's configured include
 * paths work.
 */
class CXXAddSourceTest {
    @Test
    fun testParseContentDirectly() {
        val ctx = TranslationContext(TranslationConfiguration.builder().build())
        val frontend = CXXLanguageFrontend(ctx, CPPLanguage())

        val tu: TranslationUnit =
            frontend.parse(
                """
                class MyClass {
                public:
                    void myMethod() {}
                };
                """
                    .trimIndent(),
                Path.of("MyClass.cpp"),
            )

        val record = tu.records["MyClass"]
        assertNotNull(record, "Expected a record 'MyClass' to be parsed from the content")
        assertNotNull(record.methods["myMethod"])
    }

    @Test
    fun testParseContentWithoutPath() {
        val ctx = TranslationContext(TranslationConfiguration.builder().build())
        val frontend = CXXLanguageFrontend(ctx, CPPLanguage())

        // A null path is a valid fallback -- no file-based metadata (name, storage) is
        // available, and we default to the C++ dialect, but parsing and CPG construction must
        // still succeed.
        val tu = frontend.parse("class MyClass { public: void myMethod() {} };", path = null)

        val record = tu.records["MyClass"]
        assertNotNull(record)
        assertNotNull(record.methods["myMethod"])
    }

    @Test
    fun testParseCContentSelectsCDialectByExtension() {
        val ctx = TranslationContext(TranslationConfiguration.builder().build())
        val frontend = CXXLanguageFrontend(ctx, CLanguage())

        val tu =
            frontend.parse(
                """
                struct MyStruct {
                    int field;
                };

                void myFunction(struct MyStruct* s) {}
                """
                    .trimIndent(),
                Path.of("myFile.c"),
            )

        assertNotNull(tu.records["MyStruct"])
        assertNotNull(tu.functions["myFunction"])
    }

    /**
     * Regression test: with a null [Path] (as always happens for the `content + language`
     * `addSource` overload), the dialect must be picked from the frontend's own [CLanguage]
     * instance, not defaulted to C++ via a synthetic `.cpp` filename -- otherwise a C-language
     * `addSource` call would silently be parsed as C++.
     */
    @Test
    fun testParseContentWithoutPathSelectsCDialectFromLanguage() {
        val ctx = TranslationContext(TranslationConfiguration.builder().build())
        val frontend = CXXLanguageFrontend(ctx, CLanguage())

        val tu =
            frontend.parse(
                """
                struct MyStruct {
                    int field;
                };

                void myFunction(struct MyStruct* s) {}
                """
                    .trimIndent(),
                path = null,
            )

        assertTrue(frontend.dialect is GCCLanguage, "Expected the C dialect to be selected")
        assertNotNull(tu.records["MyStruct"])
        assertNotNull(tu.functions["myFunction"])
    }

    private fun tempSource(topLevel: File, fileName: String, content: String): File {
        return File(topLevel, fileName).apply {
            writeText(content)
            deleteOnExit()
        }
    }

    @Test
    fun testAddSourceWithContentMergesIntoLiveGraph() {
        val topLevel =
            Files.createTempDirectory("cpg-cxx-add-source-test").toFile().apply { deleteOnExit() }
        val first =
            tempSource(
                topLevel,
                "first.cpp",
                """
                class First {
                public:
                    void run() {}
                };
                """
                    .trimIndent(),
            )

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<CPPLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        assertNotNull(result)

        val component = result.components.single()
        assertEquals(1, component.translationUnits.size)

        val secondContent =
            """
            class Second {
            public:
                void greet() {}
            };
            """
                .trimIndent()

        // Note: "second.cpp" is intentionally not written to disk -- addSource only needs a
        // virtual path here, since the content is self-contained (no relative includes).
        val tu =
            manager.addSource(
                result,
                component,
                secondContent,
                File(topLevel, "second.cpp").toPath(),
            )
        assertNotNull(tu)

        assertEquals(2, component.translationUnits.size)
        assertTrue(component.translationUnits.contains(tu))

        val record = tu.records["Second"]
        assertNotNull(record)
        assertNotNull(record.methods["greet"])

        val language = result.finalCtx.availableLanguage<CPPLanguage>()
        assertNotNull(language)
        val declarations =
            result.finalCtx.scopeManager.lookupSymbolByName(
                Name("Second"),
                language,
                startScope = result.finalCtx.scopeManager.globalScope,
            )
        assertTrue(declarations.isNotEmpty(), "Expected the newly added class to be resolvable")
    }

    @Test
    fun testAddSourceWithContentAndLanguageMergesIntoLiveGraph() {
        val topLevel =
            Files.createTempDirectory("cpg-cxx-add-source-language-test").toFile().apply {
                deleteOnExit()
            }
        val first =
            tempSource(
                topLevel,
                "first.cpp",
                """
                class First {
                public:
                    void run() {}
                };
                """
                    .trimIndent(),
            )

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<CPPLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val language = result.finalCtx.availableLanguage<CPPLanguage>()
        assertNotNull(language)

        val tu =
            manager.addSource(
                result,
                component,
                """
                class Third {
                public:
                    void run() {}
                };
                """
                    .trimIndent(),
                language,
            )
        assertNotNull(tu)

        assertEquals(2, component.translationUnits.size)
        val record = tu.records["Third"]
        assertNotNull(record)
        assertNotNull(record.methods["run"])
    }
}
