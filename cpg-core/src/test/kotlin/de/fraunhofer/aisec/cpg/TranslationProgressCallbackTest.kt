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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.newTranslationUnit
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.TranslationResultPass
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import java.nio.file.Files
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CallbackTestLanguage : TestLanguage() {
    override val fileExtensions: List<String>
        get() = listOf("cb")

    override val frontend: KClass<out TestLanguageFrontend>
        get() = CallbackTestLanguageFrontend::class
}

class CallbackTestLanguageFrontend(
    ctx: TranslationContext = TranslationContext(TranslationConfiguration.builder().build()),
    language: Language<TestLanguageFrontend> = CallbackTestLanguage(),
) : TestLanguageFrontend(ctx, language) {
    override fun parse(file: File): TranslationUnit {
        return newTranslationUnit(file.name)
    }

    override fun typeOf(type: Any): Type {
        return unknownType()
    }

    override fun codeOf(astNode: Any): String? {
        return null
    }

    override fun locationOf(astNode: Any): PhysicalLocation? {
        return null
    }

    override fun setComment(node: Node, astNode: Any) {}
}

class CallbackRecordingPass(ctx: TranslationContext) : TranslationResultPass(ctx) {
    override fun cleanup() {}

    override fun accept(t: TranslationResult) {
        executionCount++
    }

    companion object {
        var executionCount: Int = 0
    }
}

class TranslationProgressCallbackTest {
    @Test
    fun callbacksAreInvokedAfterFrontendsAndPasses() {
        CallbackRecordingPass.executionCount = 0
        val events = mutableListOf<String>()

        val callback =
            object : TranslationProgressCallback {
                override fun afterFrontends(
                    ctx: TranslationContext,
                    result: TranslationResult,
                    executedFrontends: Set<de.fraunhofer.aisec.cpg.frontends.LanguageFrontend<*, *>>,
                ) {
                    events += "frontends"
                }

                override fun afterPass(
                    pass: kotlin.reflect.KClass<out de.fraunhofer.aisec.cpg.passes.Pass<out Node>>,
                    ctx: TranslationContext,
                    result: TranslationResult,
                ) {
                    events += "pass:${pass.simpleName}"
                }
            }

        val topLevel =
            Files.createTempDirectory("cpg-callback-test").toFile().apply { deleteOnExit() }
        val source =
            File(topLevel, "main.cb").apply {
                writeText("unit test")
                deleteOnExit()
            }

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(source)
                .registerLanguage<CallbackTestLanguage>()
                .registerPass<CallbackRecordingPass>()
                .build()

        val result =
            TranslationManager.builder()
                .config(config)
                .build()
                .analyze(callbacks = listOf(callback))
                .get()

        assertNotNull(result)
        assertEquals(1, CallbackRecordingPass.executionCount)
        assertTrue(events.isNotEmpty())
        assertEquals("frontends", events.first())
        assertTrue(events.contains("pass:CallbackRecordingPass"))
    }

    @Test
    fun callbackFailuresDoNotAbortAnalysis() {
        CallbackRecordingPass.executionCount = 0

        val callback =
            object : TranslationProgressCallback {
                override fun afterFrontends(
                    ctx: TranslationContext,
                    result: TranslationResult,
                    executedFrontends: Set<LanguageFrontend<*, *>>,
                ) {
                    error("frontend callback failed")
                }

                override fun afterPass(
                    pass: KClass<out Pass<out Node>>,
                    ctx: TranslationContext,
                    result: TranslationResult,
                ) {
                    error("pass callback failed")
                }
            }

        val topLevel =
            Files.createTempDirectory("cpg-callback-failure-test").toFile().apply { deleteOnExit() }
        val source =
            File(topLevel, "main.cb").apply {
                writeText("unit test")
                deleteOnExit()
            }

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(source)
                .registerLanguage<CallbackTestLanguage>()
                .registerPass<CallbackRecordingPass>()
                .build()

        val result =
            TranslationManager.builder()
                .config(config)
                .build()
                .analyze(callbacks = listOf(callback))
                .get()

        assertNotNull(result)
        assertEquals(1, CallbackRecordingPass.executionCount)
    }
}
