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

import de.fraunhofer.aisec.cpg.TestUtils
import java.nio.file.Path
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("llvm-examples")
class ExamplesTest {
    @Test
    fun testRust() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("rust_sample.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)
    }

    @Test
    fun testRust2() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "failed")

        /* Failing:
                *  - miniz_oxide-bdfdbcfc5f7f7f1b.ll => (4) in ControlFlowSensitiveDFGPass?
                *  - rustc_demangle-0523e76fb0a24ded.ll => OutOfMemory in ControlFlowSensitiveDFGPass?
                *  - gimli-42844e93de3eb724.ll => OutOfMemory in ControlFlowSensitiveDFGPass?
                *  - core-e4cbdb9a079d6d85.ll => OutOfMemory
                *  - proc_macro-ad55da585703b268.ll -> OutOfMemory
                *  - std-b98b422506f4d0f3.ll -> OutOfMemory
                *
                *
                * (4) Caused by: java.lang.StackOverflowError
        at java.base/java.util.Objects.equals(Objects.java:77)
        at de.fraunhofer.aisec.cpg.graph.statements.expressions.CastExpression.equals(CastExpression.java:135)
        at java.base/java.util.HashMap$TreeNode.find(HashMap.java:1932)
        at java.base/java.util.HashMap$TreeNode.find(HashMap.java:1942)
        at java.base/java.util.HashMap$TreeNode.find(HashMap.java:1942)
        at java.base/java.util.HashMap$TreeNode.find(HashMap.java:1942)
        at java.base/java.util.HashMap$TreeNode.find(HashMap.java:1942)
        at java.base/java.util.HashMap$TreeNode.find(HashMap.java:1942)
        at java.base/java.util.HashMap$TreeNode.find(HashMap.java:1942)
        at java.base/java.util.HashMap$TreeNode.getTreeNode(HashMap.java:1954)
        at java.base/java.util.HashMap.getNode(HashMap.java:572)
        at java.base/java.util.HashMap.containsKey(HashMap.java:592)
        at de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass$FunctionLevelFixpointIterator.iterateTillFixpoint(ControlFlowSensitiveDFGPass.java:266)
        at de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass$FunctionLevelFixpointIterator.iterateTillFixpoint(ControlFlowSensitiveDFGPass.java:303)
        at de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass$FunctionLevelFixpointIterator.iterateTillFixpoint(ControlFlowSensitiveDFGPass.java:303)
        at de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass$FunctionLevelFixpointIterator.iterateTillFixpoint(ControlFlowSensitiveDFGPass.java:303)
        at de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass$FunctionLevelFixpointIterator.iterateTillFixpoint(ControlFlowSensitiveDFGPass.java:303)
        at de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass$FunctionLevelFixpointIterator.iterateTillFixpoint(ControlFlowSensitiveDFGPass.java:303)
        at de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass$FunctionLevelFixpointIterator.iterateTillFixpoint(ControlFlowSensitiveDFGPass.java:303)
        at de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass$FunctionLevelFixpointIterator.iterateTillFixpoint(ControlFlowSensitiveDFGPass.java:303)
        at de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass$FunctionLevelFixpointIterator.iterateTillFixpoint(ControlFlowSensitiveDFGPass.java:303)
        at de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass$FunctionLevelFixpointIterator.iterateTillFixpoint(ControlFlowSensitiveDFGPass.java:303)
        at de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass$FunctionLevelFixpointIterator.iterateTillFixpoint(ControlFlowSensitiveDFGPass.java:303)
        at de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass$FunctionLevelFixpointIterator.iterateTillFixpoint(ControlFlowSensitiveDFGPass.java:303)
                */
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("proc_macro-ad55da585703b268.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)
    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "llvm")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("client.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)
    }

    @Test
    fun testLiftedClient() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "retdec")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("client.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)
    }

    @Test
    fun testLiftedIf() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "retdec")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("if.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)
    }

    @Test
    fun testLiftedMain() {
        val topLevel = Path.of("src", "test", "resources", "llvm", "examples", "retdec")

        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("main.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)
    }
}
