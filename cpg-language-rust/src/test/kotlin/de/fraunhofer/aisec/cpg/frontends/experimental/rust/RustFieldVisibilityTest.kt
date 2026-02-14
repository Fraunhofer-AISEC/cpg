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
package de.fraunhofer.aisec.cpg.frontends.experimental.rust

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

        // "name" field should have pub modifier
        val nameField = config.fields.firstOrNull { it.name.localName == "name" }
        assertNotNull(nameField)
        assertTrue(nameField.modifiers.any { it == "pub" }, "name field should have pub modifier")

        // "version" field should have pub(crate) modifier
        val versionField = config.fields.firstOrNull { it.name.localName == "version" }
        assertNotNull(versionField)
        assertTrue(
            versionField.modifiers.any { it.contains("pub") },
            "version field should have pub(crate) modifier",
        )

        // "secret" field should NOT have visibility modifier
        val secretField = config.fields.firstOrNull { it.name.localName == "secret" }
        assertNotNull(secretField)
        assertTrue(
            secretField.modifiers.none { it.contains("pub") },
            "secret field should not have pub modifier",
        )
    }
}
