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

import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.streaming.toMessageResponse
import kotlinx.serialization.SerializationException
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("de.fraunhofer.aisec.cpg.ai.StreamDiagnostics")

/** Tool-call arguments longer than this are cut in the log. */
private const val MAX_LOGGED_ARGUMENTS = 2000

/**
 * The streamed [frames] assembled into one assistant message, like [toMessageResponse]. If that
 * fails to parse, the frames are logged ([describeFrames]) before the exception is rethrown, so the
 * log shows what the model actually sent - e.g. a tool call with empty or cut-off arguments, as
 * opposed to a stream that broke off. [node] names the strategy node for the log.
 */
internal fun List<StreamFrame>.toMessageResponseLoggingFailures(node: String): Message.Assistant =
    try {
        toMessageResponse()
    } catch (e: SerializationException) {
        log.warn(
            "Could not assemble the streamed answer in {}: {}. Streamed frames: {}",
            node,
            e.message,
            describeFrames(this),
        )
        throw e
    }

/**
 * A compact description of [frames]: how many of each frame type, plus every tool-call frame in
 * full (id, name, argument string). Text and reasoning are only counted - they are large and
 * already printed as they stream.
 */
internal fun describeFrames(frames: List<StreamFrame>): String {
    val counts = frames.groupingBy { it::class.simpleName ?: "unknown" }.eachCount()
    val toolCalls =
        frames.mapNotNull { frame ->
            when (frame) {
                is StreamFrame.ToolCallComplete ->
                    "complete(id=${frame.id}, name=${frame.name}, arguments=${quote(frame.content)})"
                is StreamFrame.ToolCallDelta ->
                    "delta(id=${frame.id}, name=${frame.name}, arguments=${quote(frame.content)})"
                else -> null
            }
        }
    return "counts=$counts, tool calls=$toolCalls"
}

private fun quote(text: String?): String {
    if (text == null) return "null"
    val shown =
        if (text.length > MAX_LOGGED_ARGUMENTS) text.take(MAX_LOGGED_ARGUMENTS) + "..." else text
    return "\"" + shown.replace("\"", "\\\"") + "\" (${text.length} chars)"
}
