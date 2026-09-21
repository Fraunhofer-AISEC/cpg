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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.ClassTestLanguage
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.SupportsNewParse
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.newFunction
import de.fraunhofer.aisec.cpg.graph.newTranslationUnit
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.file.Files
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IncrementalTestLanguage : TestLanguage() {
    override val fileExtensions: List<String>
        get() = listOf("inc")

    override val frontend: KClass<out IncrementalTestLanguageFrontend>
        get() = IncrementalTestLanguageFrontend::class
}

/**
 * A minimal test frontend that turns the whole file content into the name of a single top-level
 * [de.fraunhofer.aisec.cpg.graph.declarations.Function] declared in a fresh [TranslationUnit]. This
 * is enough to exercise [TranslationManager.addSource] without needing a real parser.
 */
class IncrementalTestLanguageFrontend(
    ctx: TranslationContext = TranslationContext(TranslationConfiguration.builder().build()),
    language: Language<TestLanguageFrontend> = IncrementalTestLanguage(),
) : TestLanguageFrontend(ctx, language), SupportsNewParse {
    override fun parse(file: File): TranslationUnit {
        return parse(file.readText(), file.toPath())
    }

    override fun parse(content: String, path: java.nio.file.Path?): TranslationUnit {
        val tu = newTranslationUnit(path?.fileName?.toString() ?: content)
        scopeManager.resetToGlobal(tu)
        newFunction(content.trim(), holder = tu)
        return tu
    }

    override fun typeOf(type: Any): Type {
        return unknownType()
    }

    override fun codeOf(astNode: Any): String? {
        return null
    }

    override fun locationOf(astNode: Any): PhysicalLocation? {
        return null
    }

    override fun setComment(node: Node, astNode: Any) {}
}

class AddSourceTest {
    private fun tempSource(topLevel: File, fileName: String, content: String): File {
        return File(topLevel, fileName).apply {
            writeText(content)
            deleteOnExit()
        }
    }

    @Test
    fun testAddSourceMergesIntoLiveScopeManager() {
        val topLevel =
            Files.createTempDirectory("cpg-add-source-test").toFile().apply { deleteOnExit() }
        val first = tempSource(topLevel, "first.inc", "firstFunction")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<IncrementalTestLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()

        assertNotNull(result)
        val component = result.components.single()
        assertEquals(1, component.translationUnits.size)

        val second = tempSource(topLevel, "second.inc", "secondFunction")
        val tu = manager.addSource(result, component, second)

        assertEquals(2, component.translationUnits.size)
        assertTrue(component.translationUnits.contains(tu))

        val language = result.finalCtx.availableLanguage<IncrementalTestLanguage>()
        assertNotNull(language)
        val declarations =
            result.finalCtx.scopeManager.lookupSymbolByName(
                de.fraunhofer.aisec.cpg.graph.Name("secondFunction"),
                language,
                startScope = result.finalCtx.scopeManager.globalScope,
            )
        assertTrue(declarations.isNotEmpty(), "Expected the newly added function to be resolvable")
    }

    @Test
    fun testAddSourceWithContentMergesIntoLiveScopeManager() {
        val topLevel =
            Files.createTempDirectory("cpg-add-source-content-test").toFile().apply {
                deleteOnExit()
            }
        val first = tempSource(topLevel, "first.inc", "firstFunction")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<IncrementalTestLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val tu =
            manager.addSource(
                result,
                component,
                "thirdFunction",
                File(topLevel, "third.inc").toPath(),
            )

        assertEquals(2, component.translationUnits.size)
        assertTrue(component.translationUnits.contains(tu))

        val language = result.finalCtx.availableLanguage<IncrementalTestLanguage>()
        assertNotNull(language)
        val declarations =
            result.finalCtx.scopeManager.lookupSymbolByName(
                de.fraunhofer.aisec.cpg.graph.Name("thirdFunction"),
                language,
                startScope = result.finalCtx.scopeManager.globalScope,
            )
        assertTrue(declarations.isNotEmpty(), "Expected the newly added function to be resolvable")
    }

    @Test
    fun testAddSourceRequiresDisableCleanup() {
        val topLevel =
            Files.createTempDirectory("cpg-add-source-cleanup-test").toFile().apply {
                deleteOnExit()
            }
        val first = tempSource(topLevel, "first.inc", "firstFunction")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<IncrementalTestLanguage>()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val second = tempSource(topLevel, "second.inc", "secondFunction")
        assertFailsWith<IllegalStateException> { manager.addSource(result, component, second) }
    }

    @Test
    fun testAddSourceWithContentAndLanguageMergesIntoLiveScopeManager() {
        val topLevel =
            Files.createTempDirectory("cpg-add-source-language-test").toFile().apply {
                deleteOnExit()
            }
        val first = tempSource(topLevel, "first.inc", "firstFunction")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<IncrementalTestLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val language = result.finalCtx.availableLanguage<IncrementalTestLanguage>()
        assertNotNull(language)

        val tu = manager.addSource(result, component, "fourthFunction", language)

        assertEquals(2, component.translationUnits.size)
        assertTrue(component.translationUnits.contains(tu))

        val declarations =
            result.finalCtx.scopeManager.lookupSymbolByName(
                de.fraunhofer.aisec.cpg.graph.Name("fourthFunction"),
                language,
                startScope = result.finalCtx.scopeManager.globalScope,
            )
        assertTrue(declarations.isNotEmpty(), "Expected the newly added function to be resolvable")
    }

    @Test
    fun testAddSourceWithContentAndLanguageRequiresRegisteredLanguageInstance() {
        val topLevel =
            Files.createTempDirectory("cpg-add-source-language-foreign-test").toFile().apply {
                deleteOnExit()
            }
        val first = tempSource(topLevel, "first.inc", "firstFunction")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<IncrementalTestLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        // A separately constructed instance, rather than the canonical one obtained from
        // ctx.availableLanguage(), must be rejected -- otherwise it would end up as a duplicate
        // Language node in the graph.
        val foreignLanguage = IncrementalTestLanguage()

        assertFailsWith<IllegalArgumentException> {
            manager.addSource(result, component, "someFunction", foreignLanguage)
        }
    }

    @Test
    fun testAddSourceWithContentAndLanguageRequiresSupportsNewParse() {
        val topLevel =
            Files.createTempDirectory("cpg-add-source-language-no-parse-test").toFile().apply {
                deleteOnExit()
            }
        val first = tempSource(topLevel, "first.inc", "firstFunction")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<IncrementalTestLanguage>()
                .registerLanguage<ClassTestLanguage>()
                .disableCleanup()
                .failOnError(true)
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val language = result.finalCtx.availableLanguage<ClassTestLanguage>()
        assertNotNull(language)

        assertFailsWith<TranslationException> {
            manager.addSource(result, component, "someFunction", language)
        }
    }

    @Test
    fun testAddSourceThrowsWhenNoFrontendMatches() {
        val topLevel =
            Files.createTempDirectory("cpg-add-source-no-frontend-test").toFile().apply {
                deleteOnExit()
            }
        val first = tempSource(topLevel, "first.inc", "firstFunction")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(first)
                .registerLanguage<IncrementalTestLanguage>()
                .disableCleanup()
                .failOnError(true)
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val unknown = tempSource(topLevel, "unknown.doesnotexist", "content")
        assertFailsWith<TranslationException> { manager.addSource(result, component, unknown) }
    }
}
