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
 * Tests for [JavaLanguageFrontend]'s [de.fraunhofer.aisec.cpg.frontends.SupportsNewParse]
 * implementation, both in isolation and end-to-end via [TranslationManager.addSource].
 */
class JavaAddSourceTest {
    @Test
    fun testParseContentDirectly() {
        val ctx = TranslationContext(TranslationConfiguration.builder().build())
        val frontend = JavaLanguageFrontend(ctx, JavaLanguage())

        val tu: TranslationUnit =
            frontend.parse(
                """
                package foo;

                class MyClass {
                    void myMethod() {}
                }
                """
                    .trimIndent(),
                Path.of("MyClass.java"),
            )

        val record = tu.records["foo.MyClass"]
        assertNotNull(record, "Expected a record 'foo.MyClass' to be parsed from the content")
        assertNotNull(record.methods["myMethod"])
    }

    @Test
    fun testParseContentWithoutPath() {
        val ctx = TranslationContext(TranslationConfiguration.builder().build())
        val frontend = JavaLanguageFrontend(ctx, JavaLanguage())

        // A null path is a valid fallback -- no file-based metadata (name, storage) is available,
        // but parsing and CPG construction must still succeed.
        val tu = frontend.parse("class MyClass {}", path = null)

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
    fun testAddSourceWithContentMergesIntoLiveGraph() {
        val topLevel =
            Files.createTempDirectory("cpg-java-add-source-test").toFile().apply { deleteOnExit() }
        val first =
            tempSource(
                topLevel,
                "First.java",
                """
                public class First {
                    public static void main(String[] args) {}
                }
                """
                    .trimIndent(),
            )

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<JavaLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        assertNotNull(result)

        val component = result.components.single()
        assertEquals(1, component.translationUnits.size)

        val secondContent =
            """
            public class Second {
                public void greet() {}
            }
            """
                .trimIndent()

        val tu =
            manager.addSource(
                result,
                component,
                secondContent,
                File(topLevel, "Second.java").toPath(),
            )
        assertNotNull(tu)

        assertEquals(2, component.translationUnits.size)
        assertTrue(component.translationUnits.contains(tu))

        val record = tu.records["Second"]
        assertNotNull(record)
        assertNotNull(record.methods["greet"])

        val language = result.finalCtx.availableLanguage<JavaLanguage>()
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
            Files.createTempDirectory("cpg-java-add-source-language-test").toFile().apply {
                deleteOnExit()
            }
        val first =
            tempSource(
                topLevel,
                "First.java",
                """
                public class First {
                    public static void main(String[] args) {}
                }
                """
                    .trimIndent(),
            )

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<JavaLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val language = result.finalCtx.availableLanguage<JavaLanguage>()
        assertNotNull(language)

        val tu =
            manager.addSource(
                result,
                component,
                """
                public class Third {
                    public void run() {}
                }
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
