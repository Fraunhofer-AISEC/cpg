/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.util.stream.Collectors
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test

class IncrementalConstructionTest : BaseTest() {

    fun analyze(
        fileExtension: String,
        topLevel: Path,
        usePasses: Boolean,
        previousResult: TranslationResult? = null
    ): TranslationResult {
        val files =
            Files.walk(topLevel, Int.MAX_VALUE)
                .map { obj: Path -> obj.toFile() }
                .filter { obj: File -> obj.isFile }
                .filter { f: File -> f.name.endsWith(fileExtension) }
                .sorted()
                .collect(Collectors.toList())
        val builder =
            TranslationConfiguration.builder()
                .sourceLocations(files)
                .topLevel(topLevel.toFile())
                .loadIncludes(true)
                .disableCleanup()
                .debugParser(true)
                .failOnError(true)
                .typeSystemActiveInFrontend(false)
                .useParallelFrontends(true)
                .defaultLanguages()
        if (usePasses) {
            builder.defaultPasses()
        }

        val config = builder.build()
        val analyzer = TranslationManager.builder().config(config).build()
        return if (previousResult == null) {
            analyzer.analyze().get()
        } else {
            analyzer.analyze(previousResult)
        }
    }

    @Test
    fun testChangedFiles() {
        val original = Paths.get("src/test/resources/incremental/original")
        val changed = Paths.get("src/test/resources/incremental/changed")
        val tempDir = Files.createTempDirectory("incrementalCPG_")
        val topLevel = tempDir.resolve("topLevel")

        FileUtils.copyDirectory(changed.toFile(), topLevel.toFile())

        var start = Instant.now()
        val conventionalResult = analyze("java", topLevel, true)
        val conventionalTime = Duration.between(start, Instant.now()).toMillis()
        FileUtils.deleteDirectory(topLevel.toFile())

        FileUtils.copyDirectory(original.toFile(), topLevel.toFile())
        start = Instant.now()
        val previousResult = analyze("java", topLevel, true)
        var incrementalTime = Duration.between(start, Instant.now()).toMillis()
        FileUtils.deleteDirectory(topLevel.toFile())

        FileUtils.copyDirectory(changed.toFile(), topLevel.toFile())
        start = Instant.now()
        val incrementalResult = analyze("java", topLevel, true, previousResult)
        incrementalTime += Duration.between(start, Instant.now()).toMillis()
        FileUtils.deleteDirectory(tempDir.toFile())

        println("Conventional time: $conventionalTime ms, incremental time: $incrementalTime ms")

        TestUtils.assertGraphsAreEqual(
            conventionalResult.translationUnits,
            incrementalResult.translationUnits
        )
    }
}
