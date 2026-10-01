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
package de.fraunhofer.aisec.cpg.ai.clients

import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import de.fraunhofer.aisec.cpg.ai.GenericChatParams
import de.fraunhofer.aisec.cpg.ai.OpenAiCompatibleChatParams
import de.fraunhofer.aisec.cpg.ai.toGenerationParams
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigParsingTest {

    private fun clients(hocon: String) =
        ConfigFactory.parseString(hocon).toLlmProviderConfig(HttpClient(CIO)).clients

    @Test
    fun theShippedExampleConfigParses() {
        val text =
            checkNotNull(javaClass.getResource("/application.conf.example")) {
                    "application.conf.example is not on the classpath"
                }
                .readText()
        val config = ConfigFactory.parseString(text)

        assertEquals(
            setOf("ollama", "vLLM", "mlx", "openai", "gemini"),
            config.toLlmProviderConfig(HttpClient(CIO)).clients.map { it.name }.toSet(),
        )
        assertEquals(GenericChatParams() to null, config.toGenerationParams())
    }

    @Test
    fun clientContextLengthAndRequestTimeoutAreRead() {
        val client =
            clients(
                    """llm.clients.vLLM { baseUrl = "http://x", contextLength = 1000, requestTimeoutMillis = 5000 }"""
                )
                .single()

        assertEquals(1000L, client.contextLengthOverride)
        assertEquals(5000L, client.requestTimeoutMillis)
    }

    @Test
    fun clientOptionsDefaultToUnset() {
        val client = clients("""llm.clients.vLLM { baseUrl = "http://x" }""").single()

        assertNull(client.contextLengthOverride)
        assertNull(client.requestTimeoutMillis)
    }

    @Test
    fun noGenerationBlockMeansDefaults() {
        val config = ConfigFactory.parseString("""llm.clients.vLLM { baseUrl = "http://x" }""")

        assertEquals(GenericChatParams() to null, config.toGenerationParams())
    }

    @Test
    fun genericGenerationParamsAloneDoNotCreateOpenAiParams() {
        val config =
            ConfigFactory.parseString("llm.generation { temperature = 0.3, maxTokens = 512 }")

        assertEquals(
            GenericChatParams(temperature = 0.3, maxTokens = 512) to null,
            config.toGenerationParams(),
        )
    }

    @Test
    fun everyGenerationKeyIsRead() {
        val config =
            ConfigFactory.parseString(
                """
                llm.generation {
                  temperature = 0.2
                  maxTokens = 4096
                  reasoningEffort = "low"
                  frequencyPenalty = 0.1
                  presencePenalty = 0.2
                  topP = 0.9
                  stop = ["###", "END"]
                }
                """
            )

        assertEquals(
            GenericChatParams(temperature = 0.2, maxTokens = 4096) to
                OpenAiCompatibleChatParams(
                    reasoningEffort = "low",
                    frequencyPenalty = 0.1,
                    presencePenalty = 0.2,
                    topP = 0.9,
                    stop = listOf("###", "END"),
                ),
            config.toGenerationParams(),
        )
    }

    @Test
    fun anInvalidReasoningEffortFailsNamingTheAcceptedValues() {
        val config = ConfigFactory.parseString("""llm.generation { reasoningEffort = "extreme" }""")

        val failure = assertFailsWith<IllegalArgumentException> { config.toGenerationParams() }

        assertTrue(
            "high" in failure.message.orEmpty() && "extreme" in failure.message.orEmpty(),
            failure.message,
        )
    }

    @Test
    fun aWronglyTypedGenerationValueIsRejected() {
        val config = ConfigFactory.parseString("""llm.generation { temperature = "hot" }""")

        assertFailsWith<ConfigException> { config.toGenerationParams() }
    }
}
