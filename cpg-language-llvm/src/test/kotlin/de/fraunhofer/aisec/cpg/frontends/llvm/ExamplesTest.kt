/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Tag

@Tag("llvm-examples")
class ExamplesTest {
    @Test
    fun testRust() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples")

        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("rust_sample.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)
    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "llvm")

        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("client.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)
    }

    @Test
    fun testLiftedClient() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "retdec")

        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("client.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)
    }

    @Test
    fun testLiftedIf() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "retdec")

        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("if.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)
    }

    @Test
    fun testLiftedMain() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "retdec")

        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("main.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)
    }
}
