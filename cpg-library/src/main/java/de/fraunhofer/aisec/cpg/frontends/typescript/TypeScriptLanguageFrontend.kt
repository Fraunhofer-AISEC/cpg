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
package de.fraunhofer.aisec.cpg.frontends.typescript

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend.log
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.frontends.typescript.tsserver.*
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.*
import java.nio.CharBuffer
import org.apache.commons.io.input.TeeInputStream
import org.checkerframework.checker.nullness.qual.NonNull

class TypeScriptLanguageFrontend(
    config: @NonNull TranslationConfiguration,
    scopeManager: ScopeManager?
) : LanguageFrontend(config, scopeManager, ".") {

    companion object {
        @kotlin.jvm.JvmField var GOLANG_EXTENSIONS: List<String> = listOf(".ts")
    }

    override fun parse(file: File): TranslationUnitDeclaration {
        var p =
            Runtime.getRuntime()
                .exec(
                    arrayOf(
                        "node",
                        "/Applications/Visual Studio Code.app/Contents/Resources/app/extensions/node_modules/typescript/lib/tsserver.js"
                    )
                )

        var teeOutput = TeeInputStream(p.inputStream, System.out)

        var reader = BufferedReader(InputStreamReader(teeOutput))
        var writer = BufferedWriter(OutputStreamWriter(p.outputStream))

        var client = RpcClient(reader, writer, file)

        var welcome = client.parseNextMessage()
        // ignore it

        client.sendOpen() // there should not be a response (which is kind of annoying)
        var response =
            client.requestNavTree() ?: throw TranslationException("no response to navtree")

        return handleRootNavTree(response, file)
    }

    private fun handleRootNavTree(
        response: NavTreeResponse,
        file: File
    ): TranslationUnitDeclaration {
        var tu = NodeBuilder.newTranslationUnitDeclaration(file.name, null)

        this.scopeManager.resetToGlobal(tu)

        // loop through elements in the tree
        for (tree in response.body?.childItems ?: emptyList()) {
            val decl = handleNavTree(tree)

            decl?.let { this.scopeManager.addDeclaration(decl) }
        }

        return tu
    }

    private fun handleNavTree(tree: NavigationTree): Declaration? {
        if (tree.kind == "function") {
            var func = NodeBuilder.newFunctionDeclaration(tree.text, null)

            // TOOD: handle body

            return func
        }

        return null
    }

    override fun <T : Any?> getCodeFromRawNode(astNode: T): String? {
        return null
    }

    override fun <T : Any?> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        return null
    }

    override fun <S : Any?, T : Any?> setComment(s: S, ctx: T) {}
}

class RpcClient(var reader: BufferedReader, var writer: BufferedWriter, var file: File) {
    var mapper: ObjectMapper

    init {
        mapper =
            jacksonObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    fun parseNextMessage(): Message? {
        var line = reader.readLine()

        if (line.startsWith("Content-Length:")) {
            // its the header
            var length = Integer.parseInt(line.substringAfter("Content-Length: "))
            LanguageFrontend.log.debug("Got RPC header. Length: $length. Trying to read payload")

            // read one more line
            // TODO: assert, that its just \n
            reader.readLine()

            val buf: CharBuffer = CharBuffer.allocate(length)

            // read the content
            var read = reader.read(buf)
            if (read != length) {
                LanguageFrontend.log.error("Could not read the whole message")
                // TODO: fail
            }

            buf.position(0)
            var s = buf.toString()

            // TODO: de-serialize JSON
            LanguageFrontend.log.info("Read JSON: $s")

            // first, de-serialize it as a hashmap because of Jackson shortcomings (see
            // https://github.com/FasterXML/jackson-databind/issues/2000)
            val header = mapper.readValue(s, HashMap::class.java)

            val message =
                when {
                    header["type"] == "event" -> mapper.readValue(s, Event::class.java)
                    header["type"] == "response" -> mapper.readValue(s, Response::class.java)
                    else -> null
                }

            log.error("Could not parse incoming message: $s")

            return message
        }

        return null
    }

    fun sendOpen() {
        var message = OpenRequest(OpenRequestArgs(null, "ts", null, file.absolutePath, null))

        send(message)
    }

    fun requestNavTree(): NavTreeResponse? {
        val message = NavTreeRequest(FileRequestArgs(file.absolutePath))

        send(message)

        return parseNextMessage() as? NavTreeResponse
    }

    fun sendEncodedSemanticClassifications() {
        val message = EncodedSemanticClassificationsRequest(FileRequestArgs(file.absolutePath))

        send(message)
    }

    fun send(message: Message) {
        val s = mapper.writeValueAsString(message)

        log.info("Sending {}", s)

        writer.write("$s\n")
        writer.flush()
    }
}
