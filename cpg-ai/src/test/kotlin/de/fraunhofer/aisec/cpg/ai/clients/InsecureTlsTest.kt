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

import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Runs `/v1/models` on an HTTPS server with a self-signed certificate for `localhost`, which the
 * JVM does not trust, and checks whether the client's context length could be read from it.
 */
class InsecureTlsTest {

    private val password = "changeit".toCharArray()

    /** A keystore with a self-signed certificate for `localhost`, made with the JDK's keytool. */
    private fun selfSignedKeyStore(dir: File): KeyStore {
        val file = File(dir, "server.p12")
        val keytool = File(System.getProperty("java.home"), "bin/keytool").path
        val process =
            ProcessBuilder(
                    keytool,
                    "-genkeypair",
                    "-alias",
                    "server",
                    "-keyalg",
                    "RSA",
                    "-keysize",
                    "2048",
                    "-validity",
                    "1",
                    "-dname",
                    "CN=localhost",
                    "-ext",
                    "san=dns:localhost",
                    "-storetype",
                    "PKCS12",
                    "-keystore",
                    file.path,
                    "-storepass",
                    String(password),
                )
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "keytool failed: $output" }
        return KeyStore.getInstance("PKCS12").apply {
            file.inputStream().use { load(it, password) }
        }
    }

    private fun withSelfSignedModelsServer(body: (baseUrl: String) -> Unit) {
        val dir = Files.createTempDirectory("insecure-tls").toFile()
        try {
            val keyManagers =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                    .apply { init(selfSignedKeyStore(dir), password) }
                    .keyManagers
            val sslContext = SSLContext.getInstance("TLS").apply { init(keyManagers, null, null) }
            val server = HttpsServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            server.httpsConfigurator = HttpsConfigurator(sslContext)
            server.createContext("/v1/models") { exchange ->
                val listing = """{"data":[{"id":"m","max_model_len":4096}]}""".toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, listing.size.toLong())
                exchange.responseBody.use { it.write(listing) }
            }
            server.start()
            try {
                body("https://localhost:${server.address.port}")
            } finally {
                server.stop(0)
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun contextLengthOf(baseUrl: String, insecureTls: Boolean): Long? {
        val providers =
            LlmProviderConfig(
                HttpClient(CIO) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
                listOf(
                    ClientConfig(
                        name = "vLLM",
                        baseUrl = baseUrl,
                        apiKey = null,
                        provider = ClientProvider.OPENAI_COMPATIBLE,
                        requiresApiKey = false,
                        insecureTls = insecureTls,
                    )
                ),
            )
        return providers.use { runBlocking { it.clientFor("vLLM", "m")?.model?.contextLength } }
    }

    @Test
    fun anUntrustedCertificateIsRejectedByDefault() {
        withSelfSignedModelsServer { baseUrl ->
            assertEquals(DEFAULT_CONTEXT_LENGTH, contextLengthOf(baseUrl, insecureTls = false))
        }
    }

    @Test
    fun insecureTlsAcceptsAnUntrustedCertificate() {
        withSelfSignedModelsServer { baseUrl ->
            assertEquals(4096L, contextLengthOf(baseUrl, insecureTls = true))
        }
    }
}
