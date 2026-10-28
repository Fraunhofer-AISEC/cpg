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
import ai.koog.prompt.executor.clients.openai.OpenAIChatParams
import ai.koog.prompt.executor.clients.openai.base.models.ReasoningEffort
import ai.koog.prompt.llm.LLModel
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
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

/**
 * Generic, domain-agnostic default for [ChatService]'s `historyCompressionConcepts` - deliberately
 * shaped around any CPG-analysis agent's workflow (explore graph entities, complete/skip targets),
 * not any one consumer's specific skill/workflow vocabulary. A consumer with its own task-specific
 * concepts (e.g. named skills/workflows) should pass its own list instead.
 */
val defaultHistoryCompressionConcepts: List<Concept> =
    listOf(
        Concept(
            keyword = "CompletedWork",
            description =
                "Targets or entities already fully handled so far, with their outcome/status " +
                    "and any noted properties or prerequisites.",
            factType = FactType.MULTIPLE,
        ),
        Concept(
            keyword = "OpenIssues",
            description =
                "Targets or entities noted as ambiguous, unresolved, or blocked so far, and " +
                    "why - so they aren't silently dropped from the eventual summary.",
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
            keyword = "SkippedTargets",
            description =
                "Targets explicitly marked as unresolvable or given up on so far, and why - so " +
                    "an already-abandoned target isn't independently re-investigated after " +
                    "history compression.",
            factType = FactType.MULTIPLE,
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
    private val historyCompressionConcepts: List<Concept> = defaultHistoryCompressionConcepts,
    /**
     * Generation parameters sent on every [chat] call. Pass an [OpenAIChatParams] to set the
     * OpenAI-specific fields (e.g. `reasoningEffort`) for an OpenAI-compatible client; a plain
     * [LLMParams] is valid for any client. Its `toolChoice` is overridden with
     * [LLMParams.ToolChoice.Auto]: [chatStrategy] relies on the model being free to choose whether
     * to call a tool.
     */
    generationParams: LLMParams = LLMParams(),
) {
    /** [generationParams] with tool choice fixed to auto, reused for every [chat] call. */
    private val chatParams: LLMParams =
        generationParams.copy(toolChoice = LLMParams.ToolChoice.Auto)

    /**
     * In-memory backing store for Koog `ChatMemory`, shared across [chat] calls on this
     * [ChatService] instance. One [ChatService] per host-application batch/processing run, so this
     * typically holds a single session; [evictSession] clears it when the batch finishes.
     */
    private val chatHistoryProvider = SessionChatHistoryProvider()

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

    /** A new id for [ChatRequestJSON.sessionId], to start a conversation with fresh memory. */
    fun createSession(): String = UUID.randomUUID().toString()

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

    // @Volatile: connect() writes these once, chat()/getMcpCapabilities() read them on every
    // call. Marking volatile (rather than holding them in one immutable snapshot) is sufficient
    // for the connect-once-at-startup contract; a re-connect publishes all fields individually,
    // but each read sees a consistent reference for that one field.
    @Volatile private var tools: List<Tool> = emptyList()

    /**
     * Names of the MCP tools that announce `readOnlyHint`: only these are run concurrently by
     * [chatStrategy] within one turn. A tool without the hint (e.g. from a host's own tools) stays
     * sequential, which is always safe, just not maximally fast.
     */
    @Volatile private var readOnlyToolNames: Set<String> = emptySet()
    @Volatile private var prompts: List<Prompt> = emptyList()
    @Volatile private var resources: List<Resource> = emptyList()

    /** The MCP tools, wrapped as a Koog [ToolRegistry] for use by the agent in [chat]. */
    @Volatile private var mcpToolRegistry: ToolRegistry = ToolRegistry.EMPTY

    /** Connect to the MCP server via streamable HTTP. */
    suspend fun connect() {
        val transport = StreamableHttpClientTransport(url = mcpServerUrl, client = httpClient)
        mcp.connect(transport)
        tools = mcp.listTools().tools
        readOnlyToolNames =
            tools.filter { it.annotations?.readOnlyHint == true }.map { it.name }.toSet()
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
    @Volatile private var skills: List<Skill> = emptyList()

    /**
     * The tool registry the model uses to "activate" a discovered skill (list + read its
     * `SKILL.md`), jailed to [defaultSkillDirectories] - built once in [connect] alongside
     * [skills]. See `SkillFileTools.kt` for why this uses Koog's generic file tools rather than a
     * dedicated activation tool.
     */
    @Volatile private var skillFileToolRegistry: ToolRegistry = ToolRegistry.EMPTY

    /**
     * Once the running prompt grows beyond this many messages, [chatStrategy] compresses the
     * history before continuing the tool-calling loop. With up to [maxAgentIterations] round trips
     * per [chat] call, a single oversized tool result (or many moderate ones accumulating over
     * iterations) can otherwise grow the prompt past the LLM provider's context window and cause a
     * 400 error. This alone is a weak proxy for actual size though - see
     * [contextLengthOf]/[historyCompressionTokenFraction], which catch the case this misses (few
     * messages, but individually huge).
     */
    private val historyCompressionThreshold = 100

    /**
     * The context window, in tokens, of the model a request runs on: the budget of [chatStrategy]'s
     * token-based compression trigger and of [truncateForLlm]. Read per request from the model the
     * agent is running with, since every [ChatRequestJSON] picks its own client and model and one
     * service may serve models with different windows (also concurrently).
     * [LlmProviderConfig.clientFor] always sets it (override, live-detected or generic default);
     * the fallback here only covers [LLModel.contextLength] being nullable in Koog's type.
     */
    private fun contextLengthOf(model: LLModel): Long =
        model.contextLength ?: DEFAULT_CONTEXT_LENGTH

    /**
     * Once [tokenizer]'s estimated token count for the running prompt exceeds this fraction of the
     * model's context window ([contextLengthOf]), [chatStrategy] compresses the history -
     * independently of [historyCompressionThreshold], so a handful of huge tool results trigger
     * compression just as reliably as many moderate ones.
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
     * Maximum fraction of the model's context window a single tool result may occupy in the
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
     * [contextLength], the window of the model the request runs on - see
     * [singleResultTokenFraction]'s doc for why this, not [chatStrategy]'s history compression, is
     * the only mechanism that can address this case. Logs a warning whenever it actually truncates,
     * since this should be rare and is worth an operator's attention. Takes/returns a plain
     * [String] rather than a [ReceivedToolResult] so this stays directly testable without
     * constructing Koog's internal result type.
     */
    internal fun truncateForLlm(output: String, toolName: String, contextLength: Long): String {
        val budget = (contextLength * singleResultTokenFraction).toInt()
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
            contextLength,
        )
        return output.take(charBudget) +
            "\n... [truncated: this tool result alone was too large for the model's context window]"
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
     * Follow-up message sent once [chatStrategy] believes a turn is finished, requesting a
     * [TaskStatus] via structured output (see `requestTaskStatus`/`finishWithTaskStatus` in
     * [chatStrategy]) - a more reliable completion signal for callers than inferring "done" from
     * whatever the skill happened to persist to disk. The model's own final answer is already in
     * its history by this point (appended automatically when it was produced), so this only needs
     * to ask for the status, not restate the question.
     */
    private val taskStatusRequestMessage =
        "Report your current task status now via the structured response format requested."

    /** Messages added only to steer a run; not kept in the stored history. */
    private val controlMessages = setOf(continueNudgeMessage, taskStatusRequestMessage)

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
     *   messages, or [tokenizer]'s estimate exceeds [historyCompressionTokenFraction] of the
     *   request model's context window (see [contextLengthOf]), so long tool-calling loops don't
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
                    frames.toList().assembleStreamedAnswer("collectRequestLlmStream").also {
                        logTokenUsage(it)
                    }
                }
            // Not a plain nodeExecuteTools(): that node is all-or-nothing (every call in the
            // turn parallel, or every call sequential). This runs each maximal run of
            // readOnlyToolNames calls concurrently (via environment.executeTools) and every
            // other call one at a time, *in the order the model gave them*: a readOnly call that
            // follows a mutating one runs after it, so it observes the mutation. Splitting the
            // turn into parallel batches and sequential calls in place preserves the model's
            // intended order across the read/write boundary, not only within the sequential part.
            val executeTool by
                node<ToolCalls, ReceivedToolResults>("executeToolsPartiallyParallel") { toolCalls ->
                    val contextLength = llm.readSession { contextLengthOf(model) }
                    val results = mutableListOf<ReceivedToolResult>()
                    val parallelBatch = mutableListOf<MessagePart.Tool.Call>()

                    suspend fun flushParallel() {
                        if (parallelBatch.isEmpty()) return
                        results += environment.executeTools(parallelBatch)
                        parallelBatch.clear()
                    }

                    for (call in toolCalls.toolCalls) {
                        if (call.tool in readOnlyToolNames) {
                            parallelBatch.add(call)
                        } else {
                            flushParallel()
                            results += environment.executeTool(call)
                        }
                    }
                    flushParallel()
                    ReceivedToolResults(
                        results.map {
                            it.copy(output = truncateForLlm(it.output, it.tool, contextLength))
                        }
                    )
                }
            val sendToolResultStream by nodeLLMSendToolResultsStreaming()
            val sendToolResult by
                node<Flow<StreamFrame>, Message.Assistant>("collectSendToolResultStream") { frames
                    ->
                    frames.toList().assembleStreamedAnswer("collectSendToolResultStream").also {
                        logTokenUsage(it)
                    }
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
                    frames.toList().assembleStreamedAnswer("collectNudgeRequestStream").also {
                        logTokenUsage(it)
                    }
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
                        } catch (e: CancellationException) {
                            throw e
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
            edge(requestLlm forwardTo buildNudge onTextMessage { true })
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
                                    contextLengthOf(model) * historyCompressionTokenFraction
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
                                    contextLengthOf(model) * historyCompressionTokenFraction
                        }
                    }
            )
            edge(sendToolResultStream forwardTo sendToolResult)
            edge(sendToolResult forwardTo executeTool onToolCalls { true })
            edge(sendToolResult forwardTo buildNudge onTextMessage { true })
            // Same empty-response fallback as after requestLlm above.
            edge(
                sendToolResult forwardTo
                    emptyResponseToNudge onCondition
                    { message ->
                        message.parts.isEmpty()
                    }
            )

            edge(emptyResponseToNudge forwardTo buildNudge)

            edge(buildNudge forwardTo nudgeRequestStream)
            edge(nudgeRequestStream forwardTo nudgeRequest)
            edge(nudgeRequest forwardTo executeTool onToolCalls { true })
            edge(nudgeRequest forwardTo requestTaskStatus onTextMessage { true })
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

            edge(emptyResponseToFinish forwardTo requestTaskStatus)
            edge(requestTaskStatus forwardTo finishWithTaskStatus)
            edge(finishWithTaskStatus forwardTo nodeFinish)
        }

    /** The skills discovered by [connect], as reported by Koog's Agent Skills discovery. */
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
        log.debug(
            "Using context length {} of {} for this request's compression budget",
            contextLengthOf(chatLlm.model),
            chatLlm.model.id,
        )

        try {
            // Stored session history is loaded by ChatMemory; only seed from request.messages if
            // there is none yet, otherwise the history would appear twice.
            val sessionId = request.sessionId
            val seedFromRequest = sessionId == null || !chatHistoryProvider.contains(sessionId)
            val history =
                prompt(id = "chat-history", params = chatParams) {
                    system(buildSystemPrompt(skills))
                    if (seedFromRequest) {
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
                            addPreProcessor(
                                object : ChatMemoryPreProcessor {
                                    override fun preprocess(
                                        messages: List<Message>
                                    ): List<Message> {
                                        val result =
                                            windowKeepingSystem(
                                                dropTrailingControlMessages(
                                                    messages,
                                                    controlMessages,
                                                ),
                                                chatMemoryWindowSize,
                                            )
                                        log.info(
                                            "ChatMemory session {}: {} of {} messages kept",
                                            sessionId,
                                            result.size,
                                            messages.size,
                                        )
                                        return result
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
                            val content =
                                ctx.toolResult?.toKotlinxJsonElement()?.let(::unwrapMcpToolResult)
                                    ?: JsonNull
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
        } catch (e: CancellationException) {
            throw e
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
     * The payload of an MCP tool result as it reaches the frontend.
     *
     * Koog's `McpTool` hands the whole serialized [CallToolResult] to the event handlers, while the
     * frontend's widgets (code items, data flow) match on the payload itself. This decodes [result]
     * back into a [CallToolResult] (like `McpTool.decodeResult`) and parses its [TextContent] items
     * with [parseToolResultContent], which is what the frontend got before the tool calls went
     * through Koog. A result that is not a [CallToolResult] (e.g. from a file tool) is returned
     * unchanged.
     */
    fun unwrapMcpToolResult(result: JsonElement): JsonElement {
        val callToolResult =
            try {
                Json.decodeFromJsonElement(CallToolResult.serializer(), result)
            } catch (_: SerializationException) {
                return result
            }
        return parseToolResultContent(
            callToolResult.content.filterIsInstance<TextContent>().map { it.text }
        )
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

    /**
     * Closes the [httpClient] and [llmProviderConfig] this service was given (and with it every
     * executor built for its chats). A host that shares one [LlmProviderConfig] between several
     * services closes it through the last of them.
     */
    fun close() {
        httpClient.close()
        llmProviderConfig.close()
    }

    companion object {
        private val log = LoggerFactory.getLogger(ChatService::class.java)

        /**
         * Drops the trailing control messages ([controlMessages], e.g. the nudge and the task
         * status request, plus the replies to them) from [messages], so they are not kept in the
         * stored history. The model still sees them during the run. Only the end is trimmed, so the
         * history keeps ending on the message before them and roles still alternate.
         */
        internal fun dropTrailingControlMessages(
            messages: List<Message>,
            controlMessages: Set<String>,
        ): List<Message> {
            fun Message.isControl() =
                this is Message.User &&
                    parts.all { it is MessagePart.Text } &&
                    textContent() in controlMessages

            var end = messages.size
            while (end > 0) {
                val last = messages[end - 1]
                end =
                    when {
                        last.isControl() -> end - 1
                        last is Message.Assistant &&
                            last.parts.none { it is MessagePart.Tool.Call } &&
                            end >= 2 &&
                            messages[end - 2].isControl() -> end - 2
                        else -> break
                    }
            }
            return messages.subList(0, end)
        }

        /**
         * Keeps the last [windowSize] messages of [messages], but always the system message (it
         * holds the skill catalog) and starting on a user message without a tool result, so no tool
         * call/result pair is cut in half.
         */
        internal fun windowKeepingSystem(messages: List<Message>, windowSize: Int): List<Message> {
            if (messages.size <= windowSize) return messages
            val system = messages.firstOrNull() as? Message.System
            val window =
                messages.takeLast(if (system != null) windowSize - 1 else windowSize).dropWhile {
                    !(it is Message.User &&
                        it.parts.none { part -> part is MessagePart.Tool.Result })
                }
            return listOfNotNull(system) + window
        }

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
                generationParams = config.toGenerationParams(),
            )
        }
    }
}

/**
 * Reads the optional `llm.generation` block into the [LLMParams] [ChatService] sends with every
 * request: `temperature` and `maxTokens` apply to every client, while `reasoningEffort`,
 * `frequencyPenalty`, `presencePenalty`, `topP` and `stop` only mean something to an
 * OpenAI-compatible one, so an [OpenAIChatParams] is built only if at least one of them is set. A
 * `reasoningEffort` that is not one of Koog's levels fails here, with the accepted values, rather
 * than as an opaque enum error when the service is built.
 */
internal fun Config.toGenerationParams(): LLMParams {
    if (!hasPath("llm.generation")) return LLMParams()
    val generation = getConfig("llm.generation")

    fun double(key: String) = if (generation.hasPath(key)) generation.getDouble(key) else null

    val temperature = double("temperature")
    val maxTokens = if (generation.hasPath("maxTokens")) generation.getInt("maxTokens") else null

    val reasoningEffort =
        if (generation.hasPath("reasoningEffort")) generation.getString("reasoningEffort") else null
    val allowedEfforts = ReasoningEffort.entries.map { it.name.lowercase() }
    require(reasoningEffort == null || reasoningEffort.lowercase() in allowedEfforts) {
        "llm.generation.reasoningEffort must be one of $allowedEfforts, but was \"$reasoningEffort\""
    }
    val frequencyPenalty = double("frequencyPenalty")
    val presencePenalty = double("presencePenalty")
    val topP = double("topP")
    val stop = if (generation.hasPath("stop")) generation.getStringList("stop") else null

    val openAiSpecific =
        listOf(reasoningEffort, frequencyPenalty, presencePenalty, topP, stop).any { it != null }
    return if (openAiSpecific) {
        OpenAIChatParams(
            temperature = temperature,
            maxTokens = maxTokens,
            reasoningEffort = reasoningEffort?.let { ReasoningEffort.valueOf(it.uppercase()) },
            frequencyPenalty = frequencyPenalty,
            presencePenalty = presencePenalty,
            topP = topP,
            stop = stop,
        )
    } else {
        LLMParams(temperature = temperature, maxTokens = maxTokens)
    }
}
