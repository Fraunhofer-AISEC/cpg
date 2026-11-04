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
package de.fraunhofer.aisec.cpg.frontends.llvm

import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `LLVMIRLanguageFrontend.locationOf` always returns `null` (no location support at all), so
 * `Node.code` interning (see `de.fraunhofer.aisec.cpg.sarif.tryInternCode`) never has a region to
 * even attempt against. Confirms that expectation stays true.
 */
class CodeInterningTest {
    @Test
    fun testCodeIsNeverInterned() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "retdec")
        val file = topLevel.resolve("client.ll").toFile()

        val result =
            analyze(listOf(file), topLevel, false) { it.registerLanguage<LLVMIRLanguage>() }

        assertTrue(result.nodes.any { it.code != null }, "expected some nodes to have code")
        assertTrue(result.nodes.none { it.isCodeInterned }, "expected no node to intern its code")
    }
}
