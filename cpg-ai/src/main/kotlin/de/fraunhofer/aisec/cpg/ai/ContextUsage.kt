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
package de.fraunhofer.aisec.cpg.ai

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.tokenizer.Tokenizer
import de.fraunhofer.aisec.cpg.ai.skills.LIST_DIRECTORY_TOOL_NAME
import de.fraunhofer.aisec.cpg.ai.skills.READ_FILE_TOOL_NAME
import kotlinx.serialization.Serializable

/** A part of the context window, e.g. the tool definitions, with its share of the tokens. */
@Serializable data class ContextCategoryJSON(val name: String, val tokens: Int)

/**
 * How full the context window of the model is: the tokens of the latest request ([usedTokens]) of
 * the [windowTokens] the model can take, split into [categories]. The provider reports only the
 * total, so the split is estimated and scaled to it; without a reported total, the total is
 * estimated as well ([estimated]).
 */
@Serializable
data class ContextUsageJSON(
    val usedTokens: Int,
    val windowTokens: Long,
    val estimated: Boolean,
    val categories: List<ContextCategoryJSON>,
)

/** The tools of the agent that read its skills, rather than the code. */
private val skillTools = setOf(LIST_DIRECTORY_TOOL_NAME, READ_FILE_TOOL_NAME)

/**
 * How the context of a request to the model is used: the system prompt, the skills (their catalog
 * in the system prompt and what the model read of them), the definitions of the MCP tools, their
 * calls and results, and the conversation itself.
 *
 * @param prompt The prompt of the request.
 * @param tools The tools sent with the request; their definitions are part of the context too.
 * @param skillCatalog The part of the system prompt that lists the skills, if any.
 * @param reportedTokens The input tokens of the request as the provider reported them, if it did.
 * @param windowTokens The context window of the model.
 */
internal fun contextUsage(
    prompt: Prompt,
    tools: List<ToolDescriptor>,
    skillCatalog: String?,
    reportedTokens: Int?,
    windowTokens: Long,
    tokenizer: Tokenizer,
): ContextUsageJSON {
    fun count(text: String) = tokenizer.countTokens(text)

    fun count(tool: ToolDescriptor) =
        count(
            (listOf(tool.name, tool.description) +
                    (tool.requiredParameters + tool.optionalParameters).map {
                        "${it.name} ${it.description} ${it.type}"
                    })
                .joinToString(" ")
        )

    val catalog = skillCatalog?.let(::count) ?: 0
    var system = 0
    var skills = catalog
    var toolResults = 0
    var conversation = 0
    for (message in prompt.messages) {
        if (message is Message.System) {
            system += count(message.textContent())
            continue
        }
        for (part in message.parts) {
            when (part) {
                is MessagePart.Tool.Call -> {
                    if (part.tool in skillTools) skills += count(part.args)
                    else toolResults += count(part.args)
                }
                is MessagePart.Tool.Result -> {
                    val text =
                        part.parts.filterIsInstance<MessagePart.Text>().joinToString("\n") {
                            it.text
                        }
                    if (part.tool in skillTools) skills += count(text)
                    else toolResults += count(text)
                }
                is MessagePart.Text -> conversation += count(part.text)
                else -> {}
            }
        }
    }
    system = (system - catalog).coerceAtLeast(0)
    skills += tools.filter { it.name in skillTools }.sumOf(::count)
    val definitions = tools.filter { it.name !in skillTools }.sumOf(::count)

    val estimate =
        listOf(
            "System prompt" to system,
            "Tool definitions" to definitions,
            "Skills" to skills,
            "Tool results" to toolResults,
            "Conversation" to conversation,
        )
    val estimatedTotal = estimate.sumOf { it.second }.coerceAtLeast(1)
    val used = reportedTokens ?: estimatedTotal
    return ContextUsageJSON(
        usedTokens = used,
        windowTokens = windowTokens,
        estimated = reportedTokens == null,
        categories =
            estimate
                .filter { it.second > 0 }
                .map { (name, tokens) ->
                    ContextCategoryJSON(name, (tokens.toLong() * used / estimatedTotal).toInt())
                },
    )
}
