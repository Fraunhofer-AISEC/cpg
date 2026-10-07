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
package de.fraunhofer.aisec.cpg.frontends.golang.verified

import de.fraunhofer.aisec.cpg.frontends.golang.GoFrontendConfiguration
import de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage
import de.fraunhofer.aisec.cpg.frontends.golang.GoLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.FrontendProvider
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

/**
 * Runs the Go frontend with and without the verified translation on all Go test files and checks
 * that both produce the same expressions.
 */
class VerifiedFrontendTest {

    private class Configuration(override val verifiedTranslationLibrary: File?) :
        GoFrontendConfiguration() {
        context(provider: FrontendProvider<GoLanguageFrontend>)
        override fun doNotParseBody(node: Function): Boolean = false
    }

    @Test
    fun testFrontendWithVerifiedTranslation() {
        val library = VerifiedTranslationTest.libraryFile
        assumeTrue(library.exists(), "native library not found at $library")

        val topLevel = Path.of("src", "test", "resources", "golang")
        val files =
            topLevel
                .toFile()
                .walk()
                .filter { it.isFile && it.extension == "go" }
                .sortedBy { it.path }
                .toList()

        compareFrontend(files) { topLevel }
    }

    /**
     * Compares the frontend with and without the verified translation on some packages of the Go
     * standard library. Skipped if Go is not installed.
     */
    @Test
    fun testStandardLibrary() {
        val library = VerifiedTranslationTest.libraryFile
        assumeTrue(library.exists(), "native library not found at $library")

        val goRoot =
            runCatching {
                    ProcessBuilder("go", "env", "GOROOT")
                        .start()
                        .inputStream
                        .reader()
                        .readText()
                        .trim()
                }
                .getOrNull()
        assumeTrue(!goRoot.isNullOrEmpty(), "Go is not installed")

        val files =
            listOf("strconv", "strings", "bytes", "fmt", "math/big", "go/scanner", "encoding/json")
                .flatMap { pkg ->
                    File(goRoot, "src/$pkg")
                        .listFiles { f -> f.extension == "go" && !f.name.endsWith("_test.go") }
                        .orEmpty()
                        .sortedBy { it.name }
                }
        assumeTrue(files.isNotEmpty(), "Go standard library not found in $goRoot")

        compareFrontend(files) { it.parentFile.toPath() }
    }

    /**
     * Analyzes each of the [files] with and without the verified translation and checks that both
     * produce the same expressions.
     */
    private fun compareFrontend(files: List<File>, topLevel: (File) -> Path) {
        val library = VerifiedTranslationTest.libraryFile
        val before = LeanTranslator.translatedRequests.get()
        var expressions = 0
        var regularTime = 0L
        var verifiedTime = 0L
        for (file in files) {
            var start = System.nanoTime()
            val regular = expressionsOf(file, topLevel(file), null)
            regularTime += System.nanoTime() - start

            start = System.nanoTime()
            val verified = expressionsOf(file, topLevel(file), library)
            verifiedTime += System.nanoTime() - start

            assertEquals(regular, verified, "different expressions in $file")
            expressions += regular.size
        }

        val translated = LeanTranslator.translatedRequests.get() - before
        println(
            "$expressions expressions in ${files.size} files, $translated verified translations, " +
                "${regularTime / 1_000_000} ms regular, ${verifiedTime / 1_000_000} ms verified"
        )
        assertTrue(translated > 0, "the verified translation was not used")
    }

    /** A description of all expressions in [file], in AST order. */
    private fun expressionsOf(file: File, topLevel: Path, library: File?): List<String> {
        val tu =
            analyzeAndGetFirstTU(listOf(file), topLevel, false) {
                it.registerLanguage<GoLanguage>()
                it.configureFrontend(GoLanguageFrontend::class, Configuration(library))
            }

        return tu.allChildren<Expression>().map {
            buildString {
                append(
                    "${it::class.simpleName} ${it.location?.region} '${it.name}' ${it.type.name}"
                )
                when (it) {
                    is Literal<*> ->
                        append(" = ${it.value} (${it.value?.let { v -> v::class.simpleName }})")
                    is BinaryOperator -> append(" ${it.operatorCode}")
                    is UnaryOperator -> append(" ${it.operatorCode}")
                }
            }
        }
    }
}
