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
package de.fraunhofer.aisec.cpg.frontends.python

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

/**
 * Tests for [PythonLanguageFrontend]'s [de.fraunhofer.aisec.cpg.frontends.SupportsNewParse]
 * implementation, both in isolation and end-to-end via [TranslationManager.addSource]; and for the
 * file-based [TranslationManager.addSource] overload, which works for any frontend without
 * requiring [de.fraunhofer.aisec.cpg.frontends.SupportsNewParse].
 */
class PythonAddSourceTest {
    @Test
    fun testParseContentDirectly() {
        val ctx = TranslationContext(TranslationConfiguration.builder().build())
        val frontend = PythonLanguageFrontend(ctx, PythonLanguage())

        val tu: TranslationUnit =
            frontend.parse(
                """
                class MyClass:
                    def my_method(self):
                        pass
                """
                    .trimIndent(),
                Path.of("MyClass.py"),
            )

        val record = tu.records["MyClass"]
        assertNotNull(record, "Expected a record 'MyClass' to be parsed from the content")
        assertNotNull(record.methods["my_method"])
    }

    @Test
    fun testParseContentWithoutPath() {
        val ctx = TranslationContext(TranslationConfiguration.builder().build())
        val frontend = PythonLanguageFrontend(ctx, PythonLanguage())

        // A null path is a valid fallback -- no file-based metadata (name, storage) is available,
        // but parsing and CPG construction must still succeed.
        val tu = frontend.parse("class MyClass:\n    pass\n", path = null)

        val record = tu.records["MyClass"]
        assertNotNull(record)
    }

    private fun tempSource(topLevel: File, fileName: String, content: String): File {
        return File(topLevel, fileName).apply {
            writeText(content)
            deleteOnExit()
        }
    }

    @Test
    fun testAddSourceWithFileMergesIntoLiveGraph() {
        val topLevel =
            Files.createTempDirectory("cpg-python-add-source-file-test").toFile().apply {
                deleteOnExit()
            }
        val first =
            tempSource(
                topLevel,
                "first.py",
                """
                class First:
                    def run(self):
                        pass
                """
                    .trimIndent(),
            )

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<PythonLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        assertNotNull(result)

        val component = result.components.single()
        assertEquals(1, component.translationUnits.size)

        val second =
            tempSource(
                topLevel,
                "second.py",
                """
                class Second:
                    def greet(self):
                        pass
                """
                    .trimIndent(),
            )

        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        assertEquals(2, component.translationUnits.size)
        assertTrue(component.translationUnits.contains(tu))

        val record = tu.records["Second"]
        assertNotNull(record)
        assertNotNull(record.methods["greet"])

        val language = result.finalCtx.availableLanguage<PythonLanguage>()
        assertNotNull(language)
        // Python wraps each top-level file in a module namespace, so the class is only visible
        // under its fully qualified name (e.g. "second.Second"), not as a bare "Second" from the
        // global scope.
        val declarations =
            result.finalCtx.scopeManager.lookupSymbolByName(
                record.name,
                language,
                startScope = result.finalCtx.scopeManager.globalScope,
            )
        assertTrue(declarations.isNotEmpty(), "Expected the newly added class to be resolvable")
    }

    @Test
    fun testAddSourceWithContentMergesIntoLiveGraph() {
        val topLevel =
            Files.createTempDirectory("cpg-python-add-source-test").toFile().apply {
                deleteOnExit()
            }
        val first =
            tempSource(
                topLevel,
                "first.py",
                """
                class First:
                    def run(self):
                        pass
                """
                    .trimIndent(),
            )

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<PythonLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        assertNotNull(result)

        val component = result.components.single()
        assertEquals(1, component.translationUnits.size)

        val secondContent =
            """
            class Second:
                def greet(self):
                    pass
            """
                .trimIndent()

        val tu =
            manager.addSource(
                result,
                component,
                secondContent,
                File(topLevel, "second.py").toPath(),
            )
        assertNotNull(tu)

        assertEquals(2, component.translationUnits.size)
        assertTrue(component.translationUnits.contains(tu))

        val record = tu.records["Second"]
        assertNotNull(record)
        assertNotNull(record.methods["greet"])

        val language = result.finalCtx.availableLanguage<PythonLanguage>()
        assertNotNull(language)
        // Python wraps each top-level file in a module namespace, so the class is only visible
        // under its fully qualified name (e.g. "second.Second"), not as a bare "Second" from the
        // global scope.
        val declarations =
            result.finalCtx.scopeManager.lookupSymbolByName(
                record.name,
                language,
                startScope = result.finalCtx.scopeManager.globalScope,
            )
        assertTrue(declarations.isNotEmpty(), "Expected the newly added class to be resolvable")
    }

    @Test
    fun testAddSourceWithContentAndLanguageMergesIntoLiveGraph() {
        val topLevel =
            Files.createTempDirectory("cpg-python-add-source-language-test").toFile().apply {
                deleteOnExit()
            }
        val first =
            tempSource(
                topLevel,
                "first.py",
                """
                class First:
                    def run(self):
                        pass
                """
                    .trimIndent(),
            )

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<PythonLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val language = result.finalCtx.availableLanguage<PythonLanguage>()
        assertNotNull(language)

        val tu =
            manager.addSource(
                result,
                component,
                """
                class Third:
                    def run(self):
                        pass
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
