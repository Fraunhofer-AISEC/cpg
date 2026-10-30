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

import ai.koog.agents.features.eventHandler.feature.EventHandlerConfig
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame

/** Token usage summed over the LLM responses of one agent run (see [countTokenUsage]). */
internal class TokenUsage {
    var input = 0
        private set

    var output = 0
        private set

    var total = 0
        private set

    /** The last model id a response reported, if any. */
    var modelId: String? = null
        private set

    fun add(metaInfo: ResponseMetaInfo) {
        input += metaInfo.inputTokensCount ?: 0
        output += metaInfo.outputTokensCount ?: 0
        total += metaInfo.totalTokensCount ?: 0
        metaInfo.modelId?.let { modelId = it }
    }
}

/**
 * Adds the usage of every LLM response of the run to [usage], as the responses arrive: a streamed
 * response's usage comes with its [StreamFrame.End], a non-streamed one's with `onLLMCallCompleted`
 * (which Koog fires only for non-streamed calls).
 *
 * Counting at arrival, not by scanning the session's messages at the end, also covers responses
 * that never become part of the session: those of the history compression, and a structured
 * response that fails to decode (Koog drops it).
 */
internal fun EventHandlerConfig.countTokenUsage(usage: TokenUsage) {
    onLLMStreamingFrameReceived { ctx ->
        (ctx.streamFrame as? StreamFrame.End)?.let { usage.add(it.metaInfo) }
    }
    onLLMCallCompleted { ctx -> ctx.response?.let { usage.add(it.metaInfo) } }
}
