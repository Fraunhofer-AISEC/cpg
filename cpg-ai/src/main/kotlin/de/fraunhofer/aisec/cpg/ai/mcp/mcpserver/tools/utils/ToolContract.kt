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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.server.RegisteredTool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * For tests of a host's tool set: every registered tool that breaks the conventions of [addTool] /
 * [addToolWithoutCpg], as "<tool>: <what is wrong>". Empty means fine.
 *
 * Checks that each tool carries the [TYPED_ARGUMENTS_META_KEY] marker (i.e. was not registered
 * through the SDK's own `addTool`), and that a call whose every argument has the wrong JSON type is
 * answered with the "Invalid arguments for <tool>:" problem list. The second check invokes the
 * handlers in-process; decoding fails before any handler code runs, so it has no side effects for
 * tools that follow the conventions. Tools without parameters are only checked for the marker.
 */
fun Server.toolRegistrationProblems(): List<String> =
    tools.values
        .sortedBy { it.tool.name }
        .flatMap { registered ->
            val name = registered.tool.name
            buildList {
                if (registered.tool.meta?.get(TYPED_ARGUMENTS_META_KEY) != JsonPrimitive(true)) {
                    add("$name: registered without addTool/addToolWithoutCpg")
                }
                val wrong = wronglyTypedArguments(registered)
                if (wrong.isNotEmpty()) {
                    val answer = call(registered, JsonObject(wrong))
                    if (!answer.startsWith("Invalid arguments for $name:")) {
                        add(
                            "$name: wrongly typed arguments were not reported, the tool answered: ${answer.take(200)}"
                        )
                    }
                }
            }
        }

/** One value per declared parameter, of a JSON type the parameter does not accept. */
private fun wronglyTypedArguments(registered: RegisteredTool): Map<String, JsonElement> =
    registered.tool.inputSchema.properties
        .orEmpty()
        .mapNotNull { (param, schema) ->
            val type = schema.jsonObject["type"]?.jsonPrimitive?.content
            val wrong: JsonElement? =
                when (type) {
                    "string" -> JsonPrimitive(12345)
                    "integer",
                    "number",
                    "boolean",
                    "array",
                    "object" -> JsonPrimitive("not a $type")
                    else -> null
                }
            wrong?.let { param to it }
        }
        .toMap()

/** The handlers registered through the helpers never use the connection. */
private val unusedConnection: ClientConnection =
    Proxy.newProxyInstance(
        ClientConnection::class.java.classLoader,
        arrayOf(ClientConnection::class.java),
    ) { _, method, _ ->
        error("toolRegistrationProblems: a tool handler used ClientConnection.${method.name}")
    } as? ClientConnection ?: error("proxy does not implement ClientConnection")

private fun call(registered: RegisteredTool, arguments: JsonObject): String {
    val request =
        CallToolRequest(CallToolRequestParams(name = registered.tool.name, arguments = arguments))
    val result =
        try {
            runBlocking { registered.handler(unusedConnection, request) }
        } catch (e: Exception) {
            return "exception ${e::class.simpleName}: ${e.message}"
        }
    return result.content.filterIsInstance<TextContent>().joinToString("\n") { it.text.orEmpty() }
}
