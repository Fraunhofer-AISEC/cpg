/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.metadata.McpServerInfo
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.streaming.toMessageResponse
import ai.koog.prompt.structure.StructuredResponse
import ai.koog.prompt.tokenizer.CachingTokenizer
import ai.koog.prompt.tokenizer.PromptTokenizer
import ai.koog.prompt.tokenizer.SimpleRegexBasedTokenizer
import ai.koog.serialization.kotlinx.toKotlinxJsonElement
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.fraunhofer.aisec.cpg.ai.clients.*
import de.fraunhofer.aisec.cpg.ai.skills.ACTIVATE_SKILL_TOOL_NAME
import de.fraunhofer.aisec.cpg.ai.skills.SkillLoader
import de.fraunhofer.aisec.cpg.ai.skills.buildActivateSkillToolRegistry
import de.fraunhofer.aisec.cpg.ai.skills.buildSkillCatalog
import de.fraunhofer.aisec.cpg.ai.skills.defaultSkillDirectories
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

/** ChatService manages LLM client configuration and provides an API for chat interactions. */
class ChatService(
    private val httpClient: HttpClient,
    private val llmProviderConfig: LlmProviderConfig,
    private val mcpServerUrl: String,
    /** Maximum number of tool-calling round trips the agent may take before it must respond. */
    private val maxAgentIterations: Int = 100,
) {
    suspend fun listAvailableProviders(): List<LlmProviderWithModels> =
        llmProviderConfig.listAvailableProviders()

    /**
     * Raw MCP client used for capability discovery (prompts/resources/tool schemas) and the direct,
     * non-agentic [getPrompt]/[callTool] endpoints. Koog's MCP integration
     * ([McpToolRegistryProvider]) only models MCP *tools*, so it can't replace this for
     * prompts/resources listing. The LLM-driven tool-calling loop in [chat], however, goes through
     * [mcpToolRegistry] below, which wraps this very same connected client.
     */
    private val mcp: Client =
        Client(
            clientInfo = Implementation(name = "codyze-client", version = "1.0.0"),
            options = ClientOptions(),
        )

    private var tools: List<Tool> = emptyList()
    private var prompts: List<Prompt> = emptyList()
    private var resources: List<Resource> = emptyList()

    /** The MCP tools, wrapped as a Koog [ToolRegistry] for use by the agent in [chat]. */
    private var mcpToolRegistry: ToolRegistry = ToolRegistry.EMPTY

    /** Connect to the MCP server via streamable HTTP. */
    suspend fun connect() {
        val transport = StreamableHttpClientTransport(url = mcpServerUrl, client = httpClient)
        mcp.connect(transport)
        tools = mcp.listTools().tools
        prompts = mcp.listPrompts().prompts
        resources = mcp.listResources().resources
        mcpToolRegistry =
            McpToolRegistryProvider.fromClient(
                mcpClient = mcp,
                serverInfo = McpServerInfo(url = mcpServerUrl),
            )
    }

    private val skillLoader = SkillLoader(defaultSkillDirectories)
    private var skills: List<Skill> = skillLoader.discoverSkills()

    /**
     * Once the running prompt grows beyond this many messages, [chatStrategy] compresses the
     * history before continuing the tool-calling loop. With up to [maxAgentIterations] round trips
     * per [chat] call, a single oversized tool result (or many moderate ones accumulating over
     * iterations) can otherwise grow the prompt past the LLM provider's context window and cause a
     * 400 error. This alone is a weak proxy for actual size though - see
     * [historyCompressionTokenLimit]/[historyCompressionTokenFraction], which catch the case this
     * misses (few messages, but individually huge).
     */
    private val historyCompressionThreshold = 100

    /**
     * The model's real context window, in tokens, used by [chatStrategy]'s token-based compression
     * trigger below. [chat] tries to resolve this dynamically (see
     * [resolveHistoryCompressionTokenLimit]) and only keeps this fallback if that fails. 262144 is
     * Qwen 3.6's context length, used as a reasonable general-purpose default.
     */
    private var historyCompressionTokenLimit = 262_144L

    /** Whether [resolveHistoryCompressionTokenLimit] has already run once for this instance. */
    private var resolvedHistoryCompressionTokenLimit = false

    /**
     * Once [tokenizer]'s estimated token count for the running prompt exceeds this fraction of
     * [historyCompressionTokenLimit], [chatStrategy] compresses the history - independently of
     * [historyCompressionThreshold], so a handful of huge tool results trigger compression just as
     * reliably as many moderate ones.
     */
    private val historyCompressionTokenFraction = 0.33

    /**
     * Rough, dependency-free token estimate (regex-based, not a real tokenizer for any specific
     * model) used only to decide *when* to compress, not for anything requiring precision. Wraps it
     * in a [CachingTokenizer] since [chatStrategy]'s compression-trigger edge conditions re-check
     * the token count on every [ChatService.chatStrategy] node transition, and most of the
     * conversation's messages don't change between checks.
     */
    private val tokenizer: PromptTokenizer = CachingTokenizer(SimpleRegexBasedTokenizer())

    /**
     * Tries [LlmProviderConfig.contextLengthFor] once per [ChatService] instance to replace the
     * hardcoded [historyCompressionTokenLimit] fallback with the real value for [clientName]'s
     * [model], if the server reports it. A no-op (including on failure) once already attempted, so
     * a slow/unreachable server only costs one extra request per instance, not one per [chat] call.
     */
    private suspend fun resolveHistoryCompressionTokenLimit(clientName: String, model: String) {
        if (resolvedHistoryCompressionTokenLimit) return
        resolvedHistoryCompressionTokenLimit = true
        llmProviderConfig.contextLengthFor(clientName, model)?.let {
            log.info("Resolved real context length for {}/{}: {} tokens", clientName, model, it)
            historyCompressionTokenLimit = it
        }
    }

    /**
     * Number of most recent messages kept verbatim when [chatStrategy] compresses the history;
     * everything older falls to the [HistoryCompressionStrategy.FromLastNMessages] fallback in
     * [historyCompressionConcepts] once its own dedicated per-concept fact extraction is done.
     */
    private val historyCompressionKeepLastN = 30

    /**
     * Concepts [chatStrategy]'s [HistoryCompressionStrategy.FactRetrieval] compression extracts as
     * explicit facts (one dedicated LLM call per concept, over the full history-so-far) before
     * falling back to [HistoryCompressionStrategy.FromLastNMessages] for anything not captured by
     * these - so specific, decision-relevant progress survives compression as structured facts
     * instead of depending on how much of it a single generic prose summary happens to retain.
     * Deliberately skill-agnostic (tag-library vs. match-library) since [chatStrategy] is built
     * once and shared across every [chat] call.
     */
    private val historyCompressionConcepts =
        listOf(
            Concept(
                keyword = "CompletedWork",
                description =
                    "Functions or concepts/operations already tagged (tag-library) or matched to " +
                        "a substitute (match-library) so far, with their outcome/status and any " +
                        "noted properties or prerequisites.",
                factType = FactType.MULTIPLE,
            ),
            Concept(
                keyword = "OpenIssues",
                description =
                    "Functions, concepts, or operations noted as ambiguous, unmatched, or blocked " +
                        "so far, and why - so they aren't silently dropped from the eventual " +
                        "summary.",
                factType = FactType.MULTIPLE,
            ),
            Concept(
                keyword = "ExploredCpgEntities",
                description =
                    "Functions, records, or files already looked up via CPG tools so far, and a " +
                        "brief note of what was found, to avoid redundant re-querying.",
                factType = FactType.MULTIPLE,
            ),
            Concept(
                keyword = "SkippedFunctions",
                description =
                    "Functions or entries explicitly marked as unresolvable or given up on so " +
                        "far (e.g. via a skip/give-up tool call), and why - so a function already " +
                        "confirmed unresolvable isn't independently re-investigated and " +
                        "re-skipped after history compression.",
                factType = FactType.MULTIPLE,
            ),
        )

    /**
     * Nudge sent to the model when it replies with plain text instead of calling a tool, to
     * distinguish "narrating the next step" from a genuine final answer (see the `buildNudge` node
     * in [chatStrategy]).
     */
    private val continueNudgeMessage =
        "If your task is not yet complete, call the appropriate tool now instead of describing " +
            "what you would do. If you are done, just confirm that no further tool calls are " +
            "needed."

    /**
     * Tool names safe to execute concurrently with each other in [chatStrategy]'s `executeTool`
     * node - i.e. read-only CPG queries with no shared mutable state. Deliberately an explicit
     * allowlist rather than "everything except a known mutating list": several mutating tools
     * (`cpg_persist_semantic_nodes`, `cpg_skip_function`, `cpg_persist_analysis_summary` in DUST's
     * `CpgPersistConceptsTool.kt`/ `CpgPersistAnalysisSummaryTool.kt`) do an unsynchronized
     * read-modify-write on a shared YAML/markdown file, so two concurrent calls could race and
     * silently drop a write. A new tool not added here simply stays sequential, which is always
     * safe, just not maximally fast - the reverse (a new mutating tool accidentally inheriting
     * parallelism) would not be.
     */
    private val parallelSafeToolNames =
        setOf(
            "cpg_dataflow",
            "cpg_dfg_backward",
            "cpg_list_functions",
            "cpg_list_records",
            "cpg_list_calls",
            "cpg_list_calls_to",
            "cpg_list_call_args",
            "cpg_list_call_arg_by_name_or_index",
            "cpg_get_functions_by_name",
            "cpg_get_node",
            "cpg_list_llm_concepts_operations",
        )

    /**
     * Follow-up message sent once [chatStrategy] believes a turn is finished, requesting a
     * [TaskStatus] via structured output (see `requestTaskStatus`/`finishWithTaskStatus` in
     * [chatStrategy]) - a more reliable completion signal for callers than inferring "done" from
     * whatever the skill happened to persist to disk. The model's own final answer is already in
     * its history by this point (appended automatically when it was produced), so this only needs
     * to ask for the status, not restate the question.
     */
    private val taskStatusRequestMessage =
        "Report your current task status now via the structured response format requested."

    /**
     * Converts one history [Message] to a [ChatMessageJSON] for [Events.finalHistory], or `null` to
     * omit it: system messages (a caller resending this history gets a fresh one via
     * [buildSystemPrompt] anyway) and any message with no text part (pure tool-call/tool-result
     * turns - not representable in the plain user/assistant transcript [ChatRequestJSON] uses, so
     * dropping them is strictly no worse than [chat]'s own current behavior of never returning them
     * at all).
     */
    private fun Message.toChatMessageJsonOrNull(): ChatMessageJSON? {
        if (this is Message.System) return null
        val text = textContent()
        if (text.isBlank()) return null
        return ChatMessageJSON(
            role = if (this is Message.Assistant) "assistant" else "user",
            content = text,
        )
    }

    /** Logs token usage reported by the LLM provider for one response, if any was reported. */
    private fun logTokenUsage(message: Message.Assistant) {
        val usage = message.metaInfo
        log.info(
            "LLM usage: model={} input={} output={} total={}",
            usage.modelId,
            usage.inputTokensCount,
            usage.outputTokensCount,
            usage.totalTokensCount,
        )
    }

    /**
     * Extracts a tool call from [text] when the model attempted one via free-form text instead of a
     * real structured `tool_calls` response - e.g. because the LLM provider's tool-call parser
     * doesn't recognize this particular model's native tool-call format (observed with some
     * self-hosted models/servers). Rather than special-casing any one model's native syntax (which
     * varies across model families and even across attempts by the same model), this looks for a
     * JSON object - optionally inside a fenced code block or a `<tool_call>` tag, both common
     * conventions - with a name-like key matching one of [validToolNames] and an arguments-like
     * key. Returns null if no such match is found, which is the common case (real tool calls and
     * genuine final answers never match).
     */
    private fun extractFallbackToolCall(
        text: String,
        validToolNames: Set<String>,
    ): MessagePart.Tool.Call? {
        val candidates = buildList {
            FENCED_CODE_BLOCK_REGEX.findAll(text).forEach { add(it.groupValues[1]) }
            TOOL_CALL_TAG_REGEX.findAll(text).forEach { add(it.groupValues[1]) }
            add(text)
        }

        for (candidate in candidates) {
            for (jsonText in findJsonObjects(candidate)) {
                val obj =
                    runCatching { Json.parseToJsonElement(jsonText).jsonObject }.getOrNull()
                        ?: continue
                val name =
                    NAME_KEYS.firstNotNullOfOrNull { key -> obj[key]?.jsonPrimitive?.contentOrNull }
                if (name == null || name !in validToolNames) continue
                val args = ARGUMENT_KEYS.firstNotNullOfOrNull { key -> obj[key]?.jsonObject }
                return MessagePart.Tool.Call(tool = name, args = args ?: JsonObject(emptyMap()))
            }
        }
        return null
    }

    /** Finds all top-level, brace-balanced `{...}` substrings in [text]. */
    private fun findJsonObjects(text: String): List<String> {
        val results = mutableListOf<String>()
        var depth = 0
        var start = -1
        for ((i, c) in text.withIndex()) {
            when (c) {
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    if (depth > 0) {
                        depth--
                        if (depth == 0 && start >= 0) {
                            results.add(text.substring(start, i + 1))
                            start = -1
                        }
                    }
                }
            }
        }
        return results
    }

    /**
     * The agent's tool-calling loop: request the LLM, and if it calls tool(s), execute them and
     * send the results back, repeating until the LLM responds with text. This mirrors Koog's
     * built-in single-run strategy shape, extended with:
     * - streaming LLM request/send-tool-result nodes ([nodeLLMRequestStreaming],
     *   [nodeLLMSendToolResultsStreaming]) so [chat]'s `onLLMStreamingFrameReceived` handler
     *   actually receives frames - the plain (non-streaming) node variants never call the streaming
     *   client path at all, so that handler would otherwise never fire and no text would ever reach
     *   the frontend. Each is paired with a small collector node
     *   (`collectRequestLlmStream`/`collectSendToolResultStream`) that only *drains* the frame
     *   [Flow] (`onLLMStreamingFrameReceived` fires as a side effect of collection, driven by
     *   Koog's `ContextualPromptExecutor` - the collector must not re-emit frames itself, or every
     *   token would reach the frontend twice) and reduces it back to a [Message.Assistant] via
     *   [toMessageResponse], so every downstream edge below is unchanged from the non-streaming
     *   version.
     * - a history-compression node that fires once the prompt exceeds [historyCompressionThreshold]
     *   messages, or [tokenizer]'s estimate exceeds [historyCompressionTokenFraction] of
     *   [historyCompressionTokenLimit] (see class docs above), so long tool-calling loops don't
     *   blow the LLM's context window either - whichever of the two fires first.
     *
     * Built once and reused across [chat] calls, since the graph itself carries no per-request
     * state.
     */
    private val chatStrategy =
        strategy<String, String>("chat-with-history-compression") {
            val requestLlmStream by nodeLLMRequestStreaming()
            val requestLlm by
                node<Flow<StreamFrame>, Message.Assistant>("collectRequestLlmStream") { frames ->
                    frames.toList().toMessageResponse().also { logTokenUsage(it) }
                }
            // Not a plain nodeExecuteTools(): that node is all-or-nothing (every call in the
            // turn parallel, or every call sequential). This splits a turn's tool calls into
            // parallelSafeToolNames (executed concurrently via environment.executeTools) and
            // everything else (executed one at a time, in order, via environment.executeTool) -
            // see parallelSafeToolNames doc for why this can't just be "parallel = true".
            val executeTool by
                node<ToolCalls, ReceivedToolResults>("executeToolsPartiallyParallel") { toolCalls ->
                    val (parallelSafe, sequential) =
                        toolCalls.toolCalls.partition { it.tool in parallelSafeToolNames }
                    ReceivedToolResults(
                        environment.executeTools(parallelSafe) +
                            sequential.map { environment.executeTool(it) }
                    )
                }
            val sendToolResultStream by nodeLLMSendToolResultsStreaming()
            val sendToolResult by
                node<Flow<StreamFrame>, Message.Assistant>("collectSendToolResultStream") { frames
                    ->
                    frames.toList().toMessageResponse().also { logTokenUsage(it) }
                }
            val compressionStrategy =
                FactRetrievalHistoryCompressionStrategy(
                    concepts = historyCompressionConcepts,
                    fallback =
                        HistoryCompressionStrategy.FromLastNMessages(historyCompressionKeepLastN),
                )
            val compressHistory by
                nodeLLMCompressHistory<ReceivedToolResults>(strategy = compressionStrategy)
            // Some models (esp. smaller/local ones) narrate their next step in plain text instead
            // of calling a tool in the same turn (e.g. "Let me check what functions are
            // available:" with no accompanying tool call). Rather than accepting that prose as the
            // final answer, give the model one nudge to actually continue; only if it replies with
            // text *again* do we treat it as truly final. This costs one extra round trip on every
            // genuinely-final answer too, but avoids silently truncating still-in-progress work.
            val buildNudge by node<String, String>("buildNudge") { _ -> continueNudgeMessage }
            // onToolCalls/onTextMessage below only match a response with tool calls or a text
            // part; a response with neither (parts=[], finishReason=stop - seen from some
            // providers) would otherwise leave Koog unable to route the message anywhere, throwing
            // AIAgentStuckInTheNodeException. These two transform nodes let such an empty response
            // join the existing buildNudge/nodeFinish paths, which both expect a String input.
            val emptyResponseToNudge by
                node<Message.Assistant, String>("emptyResponseToNudge") { _ -> "" }
            val emptyResponseToFinish by
                node<Message.Assistant, String>("emptyResponseToFinish") { _ -> "" }
            val nudgeRequestStream by nodeLLMRequestStreaming("nudgeRequestStream")
            val nudgeRequest by
                node<Flow<StreamFrame>, Message.Assistant>("collectNudgeRequestStream") { frames ->
                    frames.toList().toMessageResponse().also { logTokenUsage(it) }
                }

            // Some models attempt a tool call as free-form text instead of a real structured
            // tool_calls response (see extractFallbackToolCall doc). Both "first attempt" and
            // "after the nudge" responses get this same check before falling through to the
            // nudge/finish behavior above, since a text-only reply can happen at either point.
            fun validToolNames() = tools.map { it.name }.toSet() + ACTIVATE_SKILL_TOOL_NAME

            val detectFallbackToolCall by
                node<String, Pair<String, MessagePart.Tool.Call?>>("detectFallbackToolCall") { text
                    ->
                    text to extractFallbackToolCall(text, validToolNames())
                }
            val fallbackToolCallDetected by
                node<Pair<String, MessagePart.Tool.Call?>, ToolCalls>("fallbackToolCallDetected") {
                    (_, call) ->
                    ToolCalls(listOf(requireNotNull(call)))
                }
            val fallbackNoToolCallDetected by
                node<Pair<String, MessagePart.Tool.Call?>, String>("fallbackNoToolCallDetected") {
                    (text, _) ->
                    text
                }

            val detectFallbackToolCallAfterNudge by
                node<String, Pair<String, MessagePart.Tool.Call?>>(
                    "detectFallbackToolCallAfterNudge"
                ) { text ->
                    text to extractFallbackToolCall(text, validToolNames())
                }
            val fallbackToolCallDetectedAfterNudge by
                node<Pair<String, MessagePart.Tool.Call?>, ToolCalls>(
                    "fallbackToolCallDetectedAfterNudge"
                ) { (_, call) ->
                    ToolCalls(listOf(requireNotNull(call)))
                }
            val fallbackNoToolCallDetectedAfterNudge by
                node<Pair<String, MessagePart.Tool.Call?>, String>(
                    "fallbackNoToolCallDetectedAfterNudge"
                ) { (text, _) ->
                    text
                }

            // Once the model believes a turn is finished (both paths above that used to go
            // straight to nodeFinish), ask once more for a structured TaskStatus instead of
            // letting callers infer completion by re-parsing whatever the skill persisted to
            // disk. The model's final answer is already in its history (appended automatically
            // when produced) and was already streamed out via onLLMStreamingFrameReceived before
            // this point, so the text input here is intentionally discarded - only the request
            // for status matters.
            val requestTaskStatus by
                node<String, Result<StructuredResponse<TaskStatus>>>("requestTaskStatus") { _ ->
                    llm.writeSession {
                        appendPrompt { user(taskStatusRequestMessage) }
                        requestLLMStructured<TaskStatus>()
                    }
                }
            // Encodes the TaskStatus as this strategy's overall String output on success ([chat]
            // decodes it back to emit a task_status event), or an empty string if the structured
            // request itself failed - this return value isn't otherwise displayed (see [chat]'s
            // doc on agent.run's return value), so degrading to "no status" here is safe and
            // doesn't affect the text already streamed to the caller.
            val finishWithTaskStatus by
                node<Result<StructuredResponse<TaskStatus>>, String>("finishWithTaskStatus") {
                    result ->
                    result.fold(onSuccess = { Json.encodeToString(it.data) }, onFailure = { "" })
                }

            edge(nodeStart forwardTo requestLlmStream)
            edge(requestLlmStream forwardTo requestLlm)
            // onToolCalls is checked before onTextMessage (matching Koog's own singleRunStrategy
            // convention): some providers' streaming responses include a harmless empty text part
            // (e.g. vLLM sends an explicit `content: ""` on the role-establishing and
            // finish-reason chunks) alongside a real tool call in the same reconstructed message.
            // Since these edge predicates aren't mutually exclusive (onTextMessage just checks
            // "any Text part present", regardless of tool calls), checking onTextMessage first
            // would let that empty text part win the race and silently skip the tool call.
            edge(requestLlm forwardTo executeTool onToolCalls { true })
            edge(requestLlm forwardTo detectFallbackToolCall onTextMessage { true })
            // See emptyResponseToNudge/emptyResponseToFinish docs above: treat a genuinely empty
            // response the same as unhelpful text - nudge the model to actually continue.
            edge(
                requestLlm forwardTo
                    emptyResponseToNudge onCondition
                    { message ->
                        message.parts.isEmpty()
                    }
            )
            // If the history has grown too large - by message count or estimated token size,
            // whichever fires first - compress it before sending the tool result.
            edge(
                executeTool forwardTo
                    compressHistory onCondition
                    { _ ->
                        llm.readSession {
                            prompt.messages.size > historyCompressionThreshold ||
                                tokenizer.tokenCountFor(prompt) >
                                    historyCompressionTokenLimit * historyCompressionTokenFraction
                        }
                    }
            )
            edge(compressHistory forwardTo sendToolResultStream)
            // Otherwise, send the tool result directly.
            edge(
                executeTool forwardTo
                    sendToolResultStream onCondition
                    { _ ->
                        llm.readSession {
                            prompt.messages.size <= historyCompressionThreshold &&
                                tokenizer.tokenCountFor(prompt) <=
                                    historyCompressionTokenLimit * historyCompressionTokenFraction
                        }
                    }
            )
            edge(sendToolResultStream forwardTo sendToolResult)
            edge(sendToolResult forwardTo executeTool onToolCalls { true })
            edge(sendToolResult forwardTo detectFallbackToolCall onTextMessage { true })
            // Same empty-response fallback as after requestLlm above.
            edge(
                sendToolResult forwardTo
                    emptyResponseToNudge onCondition
                    { message ->
                        message.parts.isEmpty()
                    }
            )

            edge(
                detectFallbackToolCall forwardTo
                    fallbackToolCallDetected onCondition
                    { (_, call) ->
                        call != null
                    }
            )
            edge(fallbackToolCallDetected forwardTo executeTool)
            edge(
                detectFallbackToolCall forwardTo
                    fallbackNoToolCallDetected onCondition
                    { (_, call) ->
                        call == null
                    }
            )
            edge(fallbackNoToolCallDetected forwardTo buildNudge)
            edge(emptyResponseToNudge forwardTo buildNudge)

            edge(buildNudge forwardTo nudgeRequestStream)
            edge(nudgeRequestStream forwardTo nudgeRequest)
            edge(nudgeRequest forwardTo executeTool onToolCalls { true })
            edge(nudgeRequest forwardTo detectFallbackToolCallAfterNudge onTextMessage { true })
            // Same empty-response case as above, but this is already the nudge response - there's
            // no further nudge to give, so end the turn, matching how unhelpful text after the
            // nudge already ends at nodeFinish below.
            edge(
                nudgeRequest forwardTo
                    emptyResponseToFinish onCondition
                    { message ->
                        message.parts.isEmpty()
                    }
            )

            edge(
                detectFallbackToolCallAfterNudge forwardTo
                    fallbackToolCallDetectedAfterNudge onCondition
                    { (_, call) ->
                        call != null
                    }
            )
            edge(fallbackToolCallDetectedAfterNudge forwardTo executeTool)
            edge(
                detectFallbackToolCallAfterNudge forwardTo
                    fallbackNoToolCallDetectedAfterNudge onCondition
                    { (_, call) ->
                        call == null
                    }
            )
            edge(fallbackNoToolCallDetectedAfterNudge forwardTo requestTaskStatus)
            edge(emptyResponseToFinish forwardTo requestTaskStatus)
            edge(requestTaskStatus forwardTo finishWithTaskStatus)
            edge(finishWithTaskStatus forwardTo nodeFinish)
        }

    /** Return the discovered skills. */
    fun getSkills(): List<Skill> = skills

    /** Process a chat query using the LLM with MCP tool support */
    fun chat(request: ChatRequestJSON): Flow<String> = channelFlow {
        // Used if the LLM needs more time for a "cold-start"
        send(Events.keepalive())

        val userMessage = request.messages.lastOrNull()?.content ?: ""
        val priorMessages = request.messages.dropLast(1)

        val chatLlm =
            llmProviderConfig.clientFor(request.client, request.model)
                ?: run {
                    send(Events.text("Unknown or unavailable LLM client"))
                    return@channelFlow
                }
        resolveHistoryCompressionTokenLimit(request.client, request.model)

        try {
            // Re-derive the full conversation as the agent's initial history: the frontend sends
            // the
            // complete message list on every request (ChatService itself is stateless across
            // calls),
            // so a fresh AIAgent/prompt is built per request, mirroring the old per-request
            // LlmClient.
            val history =
                prompt(
                    id = "chat-history",
                    params = LLMParams(toolChoice = LLMParams.ToolChoice.Auto),
                ) {
                    system(buildSystemPrompt(skills))
                    priorMessages.forEach { msg ->
                        if (msg.content.isNotBlank()) {
                            if (msg.role == "assistant") assistant(msg.content)
                            else user(msg.content)
                        }
                    }
                }

            val toolRegistry = mcpToolRegistry + buildActivateSkillToolRegistry(skills)

            val agent =
                AIAgent(
                    promptExecutor = chatLlm.executor,
                    agentConfig =
                        AIAgentConfig(
                            prompt = history,
                            model = chatLlm.model,
                            maxAgentIterations = maxAgentIterations,
                        ),
                    strategy = chatStrategy,
                    toolRegistry = toolRegistry,
                ) {
                    handleEvents {
                        onLLMStreamingFrameReceived { ctx ->
                            when (val frame = ctx.streamFrame) {
                                is StreamFrame.TextDelta -> send(Events.text(frame.text))
                                is StreamFrame.ReasoningDelta ->
                                    frame.text?.let { send(Events.reasoning(it)) }
                                else -> {}
                            }
                        }
                        onToolCallCompleted { ctx ->
                            val args = ctx.toolArgs.toKotlinxJsonElement()
                            val content = ctx.toolResult?.toKotlinxJsonElement() ?: JsonNull
                            send(Events.toolResult(ctx.toolName, args, content))
                        }
                        onToolCallFailed { ctx -> send(Events.text("Tool failed: ${ctx.message}")) }
                        onAgentCompleted { ctx ->
                            val allMessages = ctx.context.llm.readSession { prompt.messages }
                            val assistantMessages =
                                allMessages.filterIsInstance<Message.Assistant>()
                            var inputTokens = 0
                            var outputTokens = 0
                            var totalTokens = 0
                            var modelId: String? = null
                            for (message in assistantMessages) {
                                val usage = message.metaInfo
                                inputTokens += usage.inputTokensCount ?: 0
                                outputTokens += usage.outputTokensCount ?: 0
                                totalTokens += usage.totalTokensCount ?: 0
                                usage.modelId?.let { modelId = it }
                            }
                            send(Events.usage(modelId, inputTokens, outputTokens, totalTokens))
                            send(
                                Events.finalHistory(
                                    allMessages.mapNotNull { it.toChatMessageJsonOrNull() }
                                )
                            )
                        }
                    }
                }

            // The final display text is already streamed out via onLLMStreamingFrameReceived
            // above - agent.run's return value is chatStrategy's finishWithTaskStatus output
            // (a TaskStatus encoded as JSON, or "" if that structured request failed/never
            // fired - e.g. the turn ended via a tool call rather than a finished text answer), not
            // display text. Emit it as its own event if present; callers that don't care about
            // structured completion can simply ignore this event type.
            val finalResult = agent.run(userMessage)
            runCatching { Json.decodeFromString<TaskStatus>(finalResult) }
                .getOrNull()
                ?.let { send(Events.taskStatus(it.done, it.resolvedItems)) }
        } catch (e: Exception) {
            log.error("Chat error: {}", e.message, e)
            send(Events.text("Error: ${e.message}"))
        }
    }

    /**
     * Compose the system prompt sent to the LLM: the base prompt followed by the skill catalog when
     * skills are available.
     */
    private fun buildSystemPrompt(skills: List<Skill>): String {
        val catalog = buildSkillCatalog(skills) ?: return SYSTEM_PROMPT
        return "$SYSTEM_PROMPT\n\n$catalog"
    }

    /** Return the MCP capabilities: tools, prompts, and resources. */
    fun getMcpCapabilities(): McpCapabilitiesJSON =
        McpCapabilitiesJSON(
            serverName = mcp.serverVersion?.name ?: "MCP Server",
            serverVersion = mcp.serverVersion?.version ?: "",
            tools =
                tools.map { tool ->
                    McpToolJSON(
                        name = tool.name,
                        description = tool.description,
                        inputSchema =
                            ToolSchemaJSON(
                                properties = tool.inputSchema.properties,
                                required = tool.inputSchema.required,
                            ),
                    )
                },
            prompts =
                prompts.map { prompt ->
                    McpPromptJSON(
                        name = prompt.name,
                        description = prompt.description,
                        arguments =
                            prompt.arguments?.map { arg ->
                                PromptArgumentJSON(
                                    name = arg.name,
                                    description = arg.description,
                                    required = arg.required,
                                )
                            },
                    )
                },
            resources =
                resources.map { resource ->
                    McpResourceJSON(
                        uri = resource.uri,
                        name = resource.name,
                        description = resource.description,
                        mimeType = resource.mimeType,
                    )
                },
        )

    /** Resolve an MCP prompt and return its messages as [ChatMessageJSON]. */
    suspend fun getPrompt(
        name: String,
        arguments: Map<String, String> = emptyMap(),
    ): List<ChatMessageJSON> {
        val result =
            mcp.getPrompt(
                GetPromptRequest(
                    GetPromptRequestParams(name = name, arguments = arguments.ifEmpty { null })
                )
            )
        return result.messages.map { msg ->
            ChatMessageJSON(
                role = if (msg.role == Role.User) "user" else "assistant",
                content = (msg.content as? TextContent)?.text ?: "",
            )
        }
    }

    /**
     * Parse a list of text content items from an MCP tool result into a [JsonElement]. JSON strings
     * are parsed into their structured form; plain text is wrapped as [JsonPrimitive]. A single
     * item is returned directly; multiple items are wrapped in a [JsonArray].
     */
    fun parseToolResultContent(contentTexts: List<String>): JsonElement {
        if (contentTexts.isEmpty()) {
            return JsonArray(emptyList())
        }
        val parsedItems =
            contentTexts.map { text ->
                try {
                    Json.parseToJsonElement(text)
                } catch (_: Exception) {
                    JsonPrimitive(text)
                }
            }
        return if (parsedItems.size == 1) parsedItems[0] else JsonArray(parsedItems)
    }

    /** Call an MCP tool directly and return the result as a parsed JSON element. */
    suspend fun callTool(name: String, arguments: JsonObject): JsonElement {
        val result = mcp.callTool(name = name, arguments = arguments)
        val contentTexts = result.content.mapNotNull { (it as? TextContent)?.text }
        return parseToolResultContent(contentTexts)
    }

    fun close() {
        httpClient.close()
    }

    companion object {
        private val log = LoggerFactory.getLogger(ChatService::class.java)

        /** Matches fenced code blocks, e.g. ` ```json ... ``` ` (see [extractFallbackToolCall]). */
        private val FENCED_CODE_BLOCK_REGEX = Regex("```(?:\\w+)?\\s*([\\s\\S]*?)```")

        /**
         * Matches the common `<tool_call>...</tool_call>` convention (see
         * [extractFallbackToolCall]).
         */
        private val TOOL_CALL_TAG_REGEX =
            Regex("<tool_call>([\\s\\S]*?)</tool_call>", RegexOption.IGNORE_CASE)

        /** Candidate JSON keys for a tool's name, in order of preference. */
        private val NAME_KEYS = listOf("name", "tool", "tool_name", "function")

        /** Candidate JSON keys for a tool's arguments, in order of preference. */
        private val ARGUMENT_KEYS = listOf("arguments", "parameters", "input")

        fun createIfConfigExist(): ChatService? {
            val config = ConfigFactory.load()
            if (!config.hasPath("llm.clients")) {
                log.warn(
                    "No application.conf found, AI chat features disabled. " +
                        "Copy application.conf.example to application.conf to enable them."
                )
                return null
            }
            return fromConfig(config)
        }

        private fun fromConfig(config: Config): ChatService {
            val mcpServerUrl = config.getString("mcp.serverUrl")

            val httpClient =
                HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                            }
                        )
                    }
                    install(SSE)
                    install(HttpTimeout) {
                        requestTimeoutMillis = 600_000
                        connectTimeoutMillis = 30_000
                        socketTimeoutMillis = 600_000
                    }
                }

            return ChatService(
                httpClient = httpClient,
                llmProviderConfig = config.toLlmProviderConfig(httpClient),
                mcpServerUrl = mcpServerUrl,
            )
        }
    }
}
