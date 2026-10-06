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

import com.github.javaparser.ParserConfiguration
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.problems
import de.fraunhofer.aisec.cpg.graph.records
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.graph.variables
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClasspathTest {
    /** The jar of JavaParser itself, which we use as an external library in our test. */
    private val javaParserJar =
        Path.of(ParserConfiguration::class.java.protectionDomain.codeSource.location.toURI())

    private fun analyze(configuration: JavaFrontendConfiguration?): TranslationResult {
        val builder =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/Classpath.java"))
                .defaultPasses()
                .registerLanguage<JavaLanguage>()
        configuration?.let { builder.configureFrontend<JavaLanguageFrontend>(it) }
        return TranslationManager.builder().config(builder.build()).build().analyze().get()
    }

    @Test
    fun testClasspath() {
        // Without the classpath, we cannot know the return type of the external method
        val withoutClasspath = analyze(null)
        val call = withoutClasspath.calls["getLanguageLevel"]
        assertNotNull(call)
        assertIs<UnknownType>(call.type)

        val withClasspath = analyze(JavaFrontendConfiguration(classpath = listOf(javaParserJar)))
        val resolvedCall = withClasspath.calls["getLanguageLevel"]
        assertNotNull(resolvedCall)
        assertEquals(
            "com.github.javaparser.ParserConfiguration.LanguageLevel",
            resolvedCall.type.name.toString(),
        )
    }

    @Test
    fun testModernSyntax() {
        val result = analyze(JavaFrontendConfiguration(classpath = listOf(javaParserJar)))
        assertTrue(
            result.problems.none { "not supported" in it.problem },
            "unexpected problems: ${result.problems.map { it.problem }}",
        )
        assertNotNull(result.records["Point"])
        assertNotNull(result.variables["p"])
    }
}
