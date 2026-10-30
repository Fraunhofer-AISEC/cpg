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

import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

/** [LlmProviderConfig] builds one executor per client and model, reuses it, and closes it. */
class ExecutorLifecycleTest {

    // No server behind it: building an executor makes no request, and the context-length lookup
    // just falls back to the default when the server is unreachable.
    private fun config() =
        LlmProviderConfig(
            HttpClient(),
            listOf(
                ClientConfig(
                    name = "local",
                    baseUrl = "http://127.0.0.1:9",
                    apiKey = null,
                    provider = ClientProvider.OPENAI_COMPATIBLE,
                    requiresApiKey = false,
                )
            ),
        )

    @Test
    fun theSameClientAndModelGetTheSameExecutor() = runBlocking {
        val providers = config()
        val first = assertNotNull(providers.clientFor("local", "a"))

        assertSame(first, providers.clientFor("local", "a"))
        assertNotSame(first, providers.clientFor("local", "b"))
        providers.close()
    }

    @Test
    fun concurrentFirstCallsEndUpWithOneExecutor() = runBlocking {
        val providers = config()
        val results = List(8) { async { providers.clientFor("local", "a") } }.awaitAll()

        assertSame(results.first(), results.distinct().single())
        providers.close()
    }

    @Test
    fun aClosedConfigHandsOutNoExecutors() = runBlocking {
        val providers = config()
        providers.clientFor("local", "a")
        providers.close()

        assertFailsWith<IllegalStateException> { providers.clientFor("local", "a") }
        Unit
    }

    @Test
    fun anUnknownClientIsNotCached() = runBlocking {
        val providers = config()
        assertNull(providers.clientFor("unknown", "a"))
        providers.close()
    }
}
