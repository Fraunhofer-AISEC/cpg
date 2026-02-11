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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustFieldVisibilityTest : BaseTest() {
    @Test
    fun testFieldVisibility() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("field_visibility.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val config = tu.records["Config"]
        assertNotNull(config)

        // "name" field should have pub annotation
        val nameField = config.fields.firstOrNull { it.name.localName == "name" }
        assertNotNull(nameField)
        assertTrue(
            nameField.annotations.any { it.name.localName == "pub" },
            "name field should have pub annotation",
        )

        // "version" field should have pub(crate) annotation
        val versionField = config.fields.firstOrNull { it.name.localName == "version" }
        assertNotNull(versionField)
        assertTrue(
            versionField.annotations.any { it.name.localName.contains("pub") },
            "version field should have pub(crate) annotation",
        )

        // "secret" field should NOT have visibility annotation
        val secretField = config.fields.firstOrNull { it.name.localName == "secret" }
        assertNotNull(secretField)
        assertTrue(
            secretField.annotations.none { it.name.localName.contains("pub") },
            "secret field should not have pub annotation",
        )
    }
}
