/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.codyze.console

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import io.github.detekt.sarif4k.ArtifactLocation
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodesTest {
    @Test
    fun testAbsolutePath() {
        val config =
            TranslationConfiguration.builder()
                .topLevels(mapOf("application" to File("app-path")))
                .build()
        val result =
            TranslationResult(
                translationManager = TranslationManager.builder().config(config).build(),
                finalCtx = TranslationContext(config),
            )
        result.components += Component().also { it.name = Name("application") }

        var location = ArtifactLocation(uri = null)
        var absolutePath = with(result) { location.absolutePath }
        assertNull(absolutePath)

        location = ArtifactLocation(uri = "relative.file", uriBaseID = "application")
        absolutePath = with(result) { location.absolutePath }
        assertNotNull(absolutePath)
        assertTrue(absolutePath.isAbsolute)

        location = ArtifactLocation(uri = "file:/absolute.file")
        absolutePath = with(result) { location.absolutePath }
        assertNotNull(absolutePath)
        assertTrue(absolutePath.isAbsolute)

        location = ArtifactLocation(uri = "file:///absolute.file")
        absolutePath = with(result) { location.absolutePath }
        assertNotNull(absolutePath)
        assertTrue(absolutePath.isAbsolute)
    }
}
