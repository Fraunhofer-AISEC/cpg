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
package de.fraunhofer.aisec.cpg.frontends.jvm

import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * This frontend derives `code` from SootUp's re-serialization of the loaded bytecode
 * (`JVMLanguageFrontend.codeOf`: `SootMethod.body.toString()` / `astNode.toString()`), never a
 * substring of any file on disk, so `Node.code` interning (see
 * `de.fraunhofer.aisec.cpg.sarif.tryInternCode`) never has anything to verify against. Confirms
 * that expectation stays true (i.e. it never silently starts "interning" against wrong content).
 */
class CodeInterningTest {
    @Test
    fun testCodeIsNeverInterned() {
        // This will be our classpath
        val topLevel = Path.of("src", "test", "resources", "jar", "literals")
        val jar = topLevel.resolve("literals.jar").toFile()

        val result = analyze(listOf(jar), topLevel, false) { it.registerLanguage<JVMLanguage>() }

        assertTrue(result.nodes.any { it.code != null }, "expected some nodes to have code")
        assertTrue(result.nodes.none { it.isCodeInterned }, "expected no node to intern its code")
    }
}
