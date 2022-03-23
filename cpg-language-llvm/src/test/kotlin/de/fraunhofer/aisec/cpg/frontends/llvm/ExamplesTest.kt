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
         *  - memchr-f368e2194464f0ec.ll => (3), liegt nicht an CompressLLVMPass
         *  - miniz_oxide-bdfdbcfc5f7f7f1b.ll => (3), liegt nicht an CompressLLVMPass
         *  - rustc_demangle-0523e76fb0a24ded.ll => (3), liegt nicht an CompressLLVMPass
         *  - alloc-04ee48fceb10d7c0.ll => (2), liegt nicht an CompressLLVMPass
         *  - compiler_builtins-f8373ef78ecdac9a.ll => (3), liegt nicht an CompressLLVMPass, Issue 742
         *  - gimli-42844e93de3eb724.ll => (3), liegt nicht an CompressLLVMPass, Issue 742
         *  - core-e4cbdb9a079d6d85.ll => OutOfMemory
         *  - proc_macro-ad55da585703b268.ll -> OutOfMemoryError
         *  - std-b98b422506f4d0f3.ll -> OutOfMemoryError
         *
         * (2) java.lang.StackOverflowError
               at java.base/java.util.Arrays.hashCode(Arrays.java:4685)
               at java.base/java.util.Objects.hash(Objects.java:146)
               at de.fraunhofer.aisec.cpg.graph.Node.hashCode(Node.kt:274)
               at de.fraunhofer.aisec.cpg.graph.statements.Statement.hashCode(Statement.java:86)
               at de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression.hashCode(Expression.java:273)
               at de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression.hashCode(DeclaredReferenceExpression.java:214)
               at java.base/java.util.HashMap.hash(HashMap.java:340)
               at java.base/java.util.HashMap.containsKey(HashMap.java:592)
               at java.base/java.util.HashSet.contains(HashSet.java:204)
               at de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator.getsDataFromInput(UnaryOperator.java:102)
               at de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator.lambda$getsDataFromInput$0(UnaryOperator.java:113)
               at java.base/java.util.stream.MatchOps$1MatchSink.accept(MatchOps.java:90)
               at java.base/java.util.HashMap$KeySpliterator.tryAdvance(HashMap.java:1642)
               at java.base/java.util.stream.ReferencePipeline.forEachWithCancel(ReferencePipeline.java:127)
               at java.base/java.util.stream.AbstractPipeline.copyIntoWithCancel(AbstractPipeline.java:502)
               at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:488)
               at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:474)
               at java.base/java.util.stream.MatchOps$MatchOp.evaluateSequential(MatchOps.java:230)
               at java.base/java.util.stream.MatchOps$MatchOp.evaluateSequential(MatchOps.java:196)
               at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
               at java.base/java.util.stream.ReferencePipeline.anyMatch(ReferencePipeline.java:528)
               at de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator.getsDataFromInput(UnaryOperator.java:113)
         *
         *
         * (3) java.lang.StackOverflowError
        at java.base/java.util.Arrays.hashCode(Arrays.java:4685)
        at java.base/java.util.Objects.hash(Objects.java:146)
        at de.fraunhofer.aisec.cpg.graph.types.Type.hashCode(Type.java:357)
        at de.fraunhofer.aisec.cpg.graph.types.ObjectType.hashCode(ObjectType.java:189)
        at java.base/java.util.Arrays.hashCode(Arrays.java:4685)
        at java.base/java.util.Objects.hash(Objects.java:146)
        at de.fraunhofer.aisec.cpg.graph.types.PointerType.hashCode(PointerType.java:159)
        at java.base/java.util.Arrays.hashCode(Arrays.java:4685)
        at java.base/java.util.Objects.hash(Objects.java:146)
        at de.fraunhofer.aisec.cpg.graph.types.PointerType.hashCode(PointerType.java:159)
         * ...
         * at de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator.possibleSubTypesChanged(BinaryOperator.java:218)
        at de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression.lambda$setPossibleSubTypes$7(Expression.java:198)
        at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:183)
        at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:177)
        at java.base/java.util.HashMap$KeySpliterator.forEachRemaining(HashMap.java:1621)
        at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:484)
        at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:474)
        at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(ForEachOps.java:150)
        at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(ForEachOps.java:173)
        at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
        at java.base/java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:497)
        at de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression.setPossibleSubTypes(Expression.java:198)
        at de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression.possibleSubTypesChanged(DeclaredReferenceExpression.java:158)
        at de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression.lambda$setPossibleSubTypes$7(Expression.java:198)
        at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:183)
        at java.base/java.util.stream.ReferencePipeline$2$1.accept(ReferencePipeline.java:177)
        at java.base/java.util.HashMap$KeySpliterator.forEachRemaining(HashMap.java:1621)
        at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:484)
        at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:474)
        at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateSequential(ForEachOps.java:150)
        at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateSequential(ForEachOps.java:173)
        at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
        at java.base/java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:497)
        at de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression.setPossibleSubTypes(Expression.java:198)
        at de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator.possibleSubTypesChanged(BinaryOperator.java:220)
        at de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression.lambda$setPossibleSubTypes$7(Expression.java:198)
         */
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("memchr-f368e2194464f0ec.ll").toFile()),
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
