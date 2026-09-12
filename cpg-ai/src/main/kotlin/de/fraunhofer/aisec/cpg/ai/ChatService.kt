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

import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.chatMemory.feature.ChatMemoryPreProcessor
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.environment.ReceivedToolResult
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
import ai.koog.skills.discovery.discoverSkills
import ai.koog.skills.model.Skill
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.fraunhofer.aisec.cpg.ai.clients.*
import de.fraunhofer.aisec.cpg.ai.skills.LIST_DIRECTORY_TOOL_NAME
import de.fraunhofer.aisec.cpg.ai.skills.READ_FILE_TOOL_NAME
import de.fraunhofer.aisec.cpg.ai.skills.buildSkillCatalog
import de.fraunhofer.aisec.cpg.ai.skills.buildSkillFileToolRegistry
import de.fraunhofer.aisec.cpg.ai.skills.defaultSkillDirectories
import de.fraunhofer.aisec.cpg.ai.skills.jailedSkillsFileSystem
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

/**
 * Generic, domain-agnostic default for [ChatService]'s `historyCompressionConcepts` - deliberately
 * shaped around any CPG-analysis agent's workflow (explore graph entities, complete/skip targets),
 * not any one consumer's specific skill/workflow vocabulary. A consumer with its own task-specific
 * concepts (e.g. named skills/workflows) should pass its own list instead.
 */
val defaultHistoryCompressionConcepts =
    listOf(
        HistoryCompressionConcept(
            keyword = "CompletedWork",
            description =
                "Targets or entities already fully handled so far, with their outcome/status " +
                    "and any noted properties or prerequisites.",
        ),
        HistoryCompressionConcept(
            keyword = "OpenIssues",
            description =
                "Targets or entities noted as ambiguous, unresolved, or blocked so far, and " +
                    "why - so they aren't silently dropped from the eventual summary.",
        ),
        HistoryCompressionConcept(
            keyword = "ExploredCpgEntities",
            description =
                "Functions, records, or files already looked up via CPG tools so far, and a " +
                    "brief note of what was found, to avoid redundant re-querying.",
        ),
        HistoryCompressionConcept(
            keyword = "SkippedTargets",
            description =
                "Targets explicitly marked as unresolvable or given up on so far, and why - so " +
                    "an already-abandoned target isn't independently re-investigated after " +
                    "history compression.",
        ),
    )

/** ChatService manages LLM client configuration and provides an API for chat interactions. */
class ChatService(
    private val httpClient: HttpClient,
    private val llmProviderConfig: LlmProviderConfig,
    private val mcpServerUrl: String,
    /** Maximum number of tool-calling round trips the agent may take before it must respond. */
    private val maxAgentIterations: Int = 100,
    /**
     * Concepts [chatStrategy]'s [HistoryCompressionStrategy.FactRetrieval] compression extracts as
     * explicit facts (one dedicated LLM call per concept, over the full history-so-far) before
     * falling back to [HistoryCompressionStrategy.FromLastNMessages] for anything not captured by
     * these - so specific, decision-relevant progress survives compression as structured facts
     * instead of depending on how much of it a single generic prose summary happens to retain.
     * Defaults to [defaultHistoryCompressionConcepts]; a caller with its own task/skill vocabulary
     * should pass its own list instead, since [chatStrategy] is built once from this value and
     * shared across every [chat] call on this instance.
     */
    private val historyCompressionConcepts: List<HistoryCompressionConcept> =
        defaultHistoryCompressionConcepts,
) {
    /**
     * In-memory backing store for Koog `ChatMemory`, shared across [chat] calls on this
     * [ChatService] instance. One [ChatService] per DUST batch, so this typically holds a single
     * session; [evictSession] clears it when the batch finishes.
     */
    private val chatHistoryProvider = EvictingChatHistoryProvider()

    /**
     * Safety floor on the number of messages `ChatMemory` will load/store per session. The
     * strategy's own [FactRetrievalHistoryCompressionStrategy] (keeping the last
     * [historyCompressionKeepLastN] messages + extracted facts) is the primary growth control and
     * runs intra-call; this caps the stored history if compression somehow leaves more than this
     * (or never fires), so a runaway single turn can't overflow the context window on the *next*
     * call's load. Generously above [historyCompressionKeepLastN] so it never interferes with
     * normal compression.
     */
    private val chatMemoryWindowSize = 200

    /** Drop the stored ChatMemory history for [sessionId]; a no-op if it was never used. */
    fun evictSession(sessionId: String) {
        chatHistoryProvider.evict(sessionId)
    }

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

        val skillsFs = jailedSkillsFileSystem(defaultSkillDirectories)
        skills =
            discoverSkills(
                skillsFs,
                defaultSkillDirectories.map { it.toAbsolutePath().normalize().toString() },
            )
        skillFileToolRegistry = buildSkillFileToolRegistry(skillsFs)
    }

    /** Discovered by [connect] via Koog's native Agent Skills discovery. */
    private var skills: List<Skill> = emptyList()

    /**
     * The tool registry the model uses to "activate" a discovered skill (list + read its
     * `SKILL.md`), jailed to [defaultSkillDirectories] - built once in [connect] alongside
     * [skills]. See `SkillFileTools.kt` for why this uses Koog's generic file tools rather than a
     * dedicated activation tool.
     */
    private var skillFileToolRegistry: ToolRegistry = ToolRegistry.EMPTY

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
     * trigger below. Set once per instance by [resolveHistoryCompressionTokenLimit] from the same
     * already-resolved value as the [ChatLlm]'s `model.contextLength` (see
     * [LlmProviderConfig.clientFor]/[LlmProviderConfig.resolveContextLength]) - both consumers
     * share one resolution (override → live-detected → generic default) instead of maintaining
     * their own separate fallback. This initial value is only a placeholder until the first [chat]
     * call resolves it for real - matches [LlmProviderConfig]'s own generic fallback constant.
     */
    private var historyCompressionTokenLimit = 128_000L

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
     * model), shared by [tokenizer] (wrapped for the [Message]/[ai.koog.prompt.Prompt]-based
     * compression-trigger checks) and [truncateForLlm] (used directly, for a raw-string
     * estimate - [PromptTokenizer] itself has no such overload).
     */
    private val rawTokenizer = SimpleRegexBasedTokenizer()

    /**
     * Used only to decide *when* to compress, not for anything requiring precision. Wraps
     * [rawTokenizer] in a [CachingTokenizer] since [chatStrategy]'s compression-trigger edge
     * conditions re-check the token count on every [ChatService.chatStrategy] node transition, and
     * most of the conversation's messages don't change between checks.
     */
    private val tokenizer: PromptTokenizer = CachingTokenizer(rawTokenizer)

    /**
     * Maximum fraction of [historyCompressionTokenLimit] a single tool result may occupy in the
     * LLM-facing history before [truncateForLlm] caps it - deliberately much larger than
     * [historyCompressionTokenFraction] (which reacts to *cumulative* history size):
     * [compressHistory] can only ever shrink *older* turns to make room, never the *one new* tool
     * result that just arrived, so a single result already this large would overflow the context
     * regardless of how aggressively everything else gets compressed. This is the actual
     * last-resort safety valve for that specific case, not a routine cap - it should almost never
     * fire in practice.
     */
    private val singleResultTokenFraction = 0.5

    /**
     * Caps [output] (a [ReceivedToolResult.output], identified by [toolName] only for the warning
     * message) if it alone would already occupy more than [singleResultTokenFraction] of
     * [historyCompressionTokenLimit] - see that field's doc for why this, not [chatStrategy]'s
     * history compression, is the only mechanism that can address this case. Logs a warning
     * whenever it actually truncates, since this should be rare and is worth an operator's
     * attention. Takes/returns a plain [String] rather than a [ReceivedToolResult] so this stays
     * directly testable without constructing Koog's internal result type.
     */
    internal fun truncateForLlm(output: String, toolName: String): String {
        val budget = (historyCompressionTokenLimit * singleResultTokenFraction).toInt()
        val tokenCount = rawTokenizer.countTokens(output)
        if (tokenCount <= budget) return output

        val charBudget = (output.length.toLong() * budget / tokenCount).toInt()
        log.warn(
            "Truncating oversized tool result for '{}': ~{} tokens exceeds the {}-token " +
                "single-result budget ({}% of the model's {}-token context window)",
            toolName,
            tokenCount,
            budget,
            (singleResultTokenFraction * 100).toInt(),
            historyCompressionTokenLimit,
        )
        return output.take(charBudget) +
            "\n... [truncated: this tool result alone was too large for the model's context window]"
    }

    /**
     * Sets [historyCompressionTokenLimit] to [contextLength] - the same value
     * [LlmProviderConfig.clientFor] already resolved (override → live-detected → generic default)
     * for the [ChatLlm]'s `model.contextLength` - once per [ChatService] instance. A no-op on
     * subsequent calls, so later [chat] calls in the same instance don't keep re-applying it.
     */
    private fun resolveHistoryCompressionTokenLimit(contextLength: Long) {
        if (resolvedHistoryCompressionTokenLimit) return
        resolvedHistoryCompressionTokenLimit = true
        historyCompressionTokenLimit = contextLength
        log.info("Using context length {} for history-compression budget", contextLength)
    }

    /**
     * Number of most recent messages kept verbatim when [chatStrategy] compresses the history;
     * everything older falls to the [HistoryCompressionStrategy.FromLastNMessages] fallback in
     * [historyCompressionConcepts] once its own dedicated per-concept fact extraction is done.
     */
    private val historyCompressionKeepLastN = 30

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
                        (environment.executeTools(parallelSafe) +
                                sequential.map { environment.executeTool(it) })
                            .map { it.copy(output = truncateForLlm(it.output, it.tool)) }
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
                    concepts =
                        historyCompressionConcepts.map {
                            Concept(
                                keyword = it.keyword,
                                description = it.description,
                                factType = if (it.multiple) FactType.MULTIPLE else FactType.SINGLE,
                            )
                        },
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
            fun validToolNames() =
                tools.map { it.name }.toSet() + LIST_DIRECTORY_TOOL_NAME + READ_FILE_TOOL_NAME

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
            //
            // Two things to handle here:
            // 1) toolChoice=Auto is inherited from the session prompt but requestLLMStructured
            //    sends response_format:json_schema with no tools — vLLM rejects this combination
            //    ("When using tool_choice, tools must be set"). Temporarily clear toolChoice for
            //    the structured request, then restore it so subsequent turns keep tool calling.
            // 2) executeStructured's internal try-catch does NOT cover the underlying execute()
            //    call, so HTTP errors throw instead of returning Result.failure. Wrap in our own
            //    try-catch to guarantee the strategy completes and ChatMemory can store history.
            val requestTaskStatus by
                node<String, Result<StructuredResponse<TaskStatus>>>("requestTaskStatus") { _ ->
                    llm.writeSession {
                        appendPrompt { user(taskStatusRequestMessage) }
                        val savedParams = prompt.params
                        rewritePrompt { it.withParams(it.params.copy(toolChoice = null)) }
                        try {
                            requestLLMStructured<TaskStatus>()
                        } catch (e: Exception) {
                            log.warn("requestLLMStructured failed: {}", e.message)
                            Result.failure(e)
                        } finally {
                            rewritePrompt { it.withParams(savedParams) }
                        }
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

    /**
     * Return the discovered skills' name/description as [SkillInfo] - deliberately not Koog's own
     * [Skill] type, which isn't visible to callers outside this module (see [SkillInfo]'s doc).
     */
    fun getSkills(): List<SkillInfo> = skills.map { SkillInfo(it.name, it.description) }

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
        // clientFor already resolved contextLength (override → live-detected → generic default);
        // the `?:` here is only defensive - LlmProviderConfig.resolveContextLength always returns
        // non-null, this just guards against LLModel.contextLength being nullable by Koog's own
        // type.
        resolveHistoryCompressionTokenLimit(chatLlm.model.contextLength ?: 128_000L)

        try {
            // When ChatMemory is active (sessionId != null), the initial prompt carries only the
            // system message; ChatMemory loads prior history (including the tool-call/tool-result
            // messages that toChatMessageJsonOrNull drops) from the provider and appends it after
            // the system message at strategy start. When sessionId is null, fall back to seeding
            // history from request.messages directly (the pre-ChatMemory behavior).
            val sessionId = request.sessionId
            val history =
                prompt(
                    id = "chat-history",
                    params = LLMParams(toolChoice = LLMParams.ToolChoice.Auto),
                ) {
                    system(buildSystemPrompt(skills))
                    if (sessionId == null) {
                        priorMessages.forEach { msg ->
                            if (msg.content.isNotBlank()) {
                                if (msg.role == "assistant") assistant(msg.content)
                                else user(msg.content)
                            }
                        }
                    }
                }

            val toolRegistry = mcpToolRegistry + skillFileToolRegistry

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
                    if (sessionId != null) {
                        install(ChatMemory.Feature) {
                            chatHistoryProvider(this@ChatService.chatHistoryProvider)
                            windowSize(chatMemoryWindowSize)
                            addPreProcessor(
                                object : ChatMemoryPreProcessor {
                                    override fun preprocess(
                                        messages: List<Message>
                                    ): List<Message> {
                                        log.info(
                                            "ChatMemory session {}: {} messages loaded/stored",
                                            sessionId,
                                            messages.size,
                                        )
                                        return messages
                                    }
                                }
                            )
                        }
                    }
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
                            // Koog 1.1.1 never populates ResponseMetaInfo.modelId (the OpenAI
                            // client's createMetaInfo hardcodes it to null), so fall back to the
                            // configured model id.
                            if (modelId == null) modelId = chatLlm.model.id
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
            val finalResult =
                if (sessionId != null) agent.run(userMessage, sessionId) else agent.run(userMessage)
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
