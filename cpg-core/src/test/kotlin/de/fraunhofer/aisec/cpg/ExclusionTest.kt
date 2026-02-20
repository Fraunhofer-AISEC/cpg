/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.newTranslationUnit
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestFileLanguage() : TestLanguage() {
    override val fileExtensions: List<String>
        get() = listOf("file")

    override val frontend: KClass<out TestLanguageFrontend>
        get() = TestFileLanguageFrontend::class
}

/** Just a test frontend that "reads" a file and returns an empty [TranslationUnit]. */
class TestFileLanguageFrontend(
    ctx: TranslationContext = TranslationContext(TranslationConfiguration.builder().build()),
    language: Language<TestLanguageFrontend> = TestFileLanguage(),
) : TestLanguageFrontend(ctx, language) {
    override fun parse(file: File): TranslationUnit {
        return newTranslationUnit(file.name)
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

class ExclusionTest {
    @Test
    fun testExclusionPatternStringDirectory() {
        val topLevel = File("src/test/resources/exclusion")
        val result =
            TranslationManager.builder()
                .config(
                    TranslationConfiguration.builder()
                        .topLevel(topLevel)
                        .sourceLocations(topLevel)
                        .defaultPasses()
                        .exclusionPatterns("tests")
                        .registerLanguage<TestFileLanguage>()
                        .build()
                )
                .build()
                .analyze()
                .get()

        val tus = result.translationUnits
        assertNotNull(tus)
        assertEquals(1, tus.size)
    }

    @Test
    fun testExclusionPatternStringFile() {
        val topLevel = File("src/test/resources/exclusion")
        val result =
            TranslationManager.builder()
                .config(
                    TranslationConfiguration.builder()
                        .topLevel(topLevel)
                        .sourceLocations(topLevel)
                        .defaultPasses()
                        .exclusionPatterns("test.file")
                        .registerLanguage<TestFileLanguage>()
                        .build()
                )
                .build()
                .analyze()
                .get()

        val tus = result.translationUnits
        assertNotNull(tus)
        assertEquals(1, tus.size)
    }

    @Test
    fun testExclusionPatternRegex() {
        val topLevel = File("src/test/resources/exclusion")
        val result =
            TranslationManager.builder()
                .config(
                    TranslationConfiguration.builder()
                        .topLevel(topLevel)
                        .sourceLocations(topLevel)
                        .defaultPasses()
                        .exclusionPatterns("""(.*)est.file""".toRegex())
                        .registerLanguage<TestFileLanguage>()
                        .build()
                )
                .build()
                .analyze()
                .get()

        val tus = result.translationUnits
        assertNotNull(tus)
        assertEquals(1, tus.size)
    }
}
