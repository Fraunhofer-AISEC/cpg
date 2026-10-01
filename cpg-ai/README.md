# cpg-ai

AI components for the [Code Property Graph](../README.md). `cpg-ai` lets an LLM explore a CPG, tag
code with *concepts* and *operations*, and query data flows - either from an MCP client of your
choice or from the built-in, Koog-based agent.

| Component | Where | What it does |
|---|---|---|
| **MCP server** | `ai.mcp` | Exposes the CPG as MCP tools (analyze, list, inspect, tag, query data flow) and one prompt. |
| **Chat agent** | `ai.ChatService` | An LLM tool-calling loop (built on [Koog](https://github.com/JetBrains/koog)) that talks to the MCP server, with history compression, session memory, and skills. |
| **LLM providers** | `ai.clients` | Resolves `application.conf` into a Koog executor: any OpenAI-compatible endpoint (vLLM, Ollama, mlx, OpenAI, ...) or Gemini. |
| **Skills** | `ai.skills` | [Agent Skills](https://agentskills.io) discovery; skill files are read through a file tool jailed to `.agents/skills/`. |

You can use the MCP server on its own (most common), the chat agent on top of it, or embed both in
your own application (that is how `codyze-console` uses it).

## Quick start

The module is enabled by `enableAIModule=true` in `gradle.properties` (see
`gradle.properties.example`). Which languages can be analyzed depends on which `cpg-language-*`
modules are enabled in the same file when you build.

```bash
./gradlew :cpg-ai:installDist
```

This produces `cpg-ai/build/install/cpg-mcp/bin/cpg-mcp` (the start script sets `-Xss515m -Xmx8g`).

| Transport | Command | Endpoint |
|---|---|---|
| stdio (default) | `cpg-mcp` | stdin/stdout |
| Streamable HTTP | `cpg-mcp --http 8080` | `http://localhost:8080/mcp` |
| SSE | `cpg-mcp --sse 8080` | `http://localhost:8080` (usable with the MCP inspector) |

`--http` and `--sse` are mutually exclusive. `--host <ip>` sets the bind address.

> **Security:** the server binds to `0.0.0.0` by default and has **no authentication**. Any client
> that can reach it can make it read any file the process can read (`cpg_analyze` takes a `path`,
> `cpg_get_node` returns code). Use `--host 127.0.0.1`, or put it behind something that
> authenticates, unless you are on a trusted network.

### Connecting an MCP client

For clients that launch a stdio server (e.g. Claude Desktop), add to the client's `mcpServers`:

```json
{
  "mcpServers": {
    "cpg": { "command": "/path/to/cpg-ai/build/install/cpg-mcp/bin/cpg-mcp" }
  }
}
```

For OpenWebUI, see [OpenWebUI.md](src/main/kotlin/de/fraunhofer/aisec/cpg/ai/mcp/OpenWebUI.md).

## A typical session

1. **Analyze.** `cpg_analyze` with either `path` (file or project directory; the project layout is
   detected automatically) or `content` + `extension` (a snippet). This replaces any previously
   analyzed graph.
2. **Explore.** `cpg_list_functions`, `cpg_list_records`, `cpg_list_calls` and
   `cpg_list_concepts_and_operations` return compact summaries, 20 per page by default - use
   `limit`/`offset` for more. `cpg_get_functions_by_name` is cheaper than listing when you already
   know the names. `cpg_get_node` returns the full details (including code) of one node id.
3. **Tag.** Attach concepts ("what something *is*") and operations ("what something *does*") to
   nodes, see [Concepts and operations](#concepts-and-operations).
4. **Query.** `cpg_dataflow` (is there a flow from concept A to concept B?), `cpg_dfg_backward`
   (where does this value come from?), `cpg_get_last_write` (which writes may have produced this
   value?).

Prefer finer control over what is computed? `cpg_translate` builds only the AST, `cpg_list_passes`
shows the available passes with their dependencies, and `cpg_run_pass` runs one pass (plus whatever
it depends on) for a single node.

## Tool reference

All node ids come from earlier tool results; the model must never invent them. "Writes" means the
tool changes shared server state (see [Concurrency](#concurrency)).

| Tool | Purpose | Writes |
|---|---|---|
| `cpg_analyze` | Parse code and run the default passes (plus CDG/PDG). Arguments: `path` or `content`+`extension`. | graph (replaces it) |
| `cpg_translate` | Parse code into the AST only, no passes. Same arguments. | graph (replaces it) |
| `cpg_list_passes` | List available passes, their dependencies and expected node type. | - |
| `cpg_run_pass` | Run a pass (`passName` = FQN) on a node (`nodeId`), including unmet dependencies. | graph |
| `cpg_list_functions` | Minimal `nodeId` + signature index of all functions. Paginated. | - |
| `cpg_list_records` | Classes/structs as compact summaries. Paginated. | - |
| `cpg_list_calls` | All calls as compact summaries. Paginated. | - |
| `cpg_list_calls_to` | Calls to the function/method with the given `name`. Not paginated. | - |
| `cpg_list_call_args` | Arguments of the call with the given `id`. | - |
| `cpg_list_call_arg_by_name_or_index` | One argument of a call, by `argumentName` or `index`. | - |
| `cpg_get_node` | Complete information (including code) for one node `id`. | - |
| `cpg_get_functions_by_name` | Batch lookup of functions by local name; `includeCode=false` omits bodies. | - |
| `cpg_list_concepts_and_operations` | Concepts/operations already applied in the graph. Paginated. | - |
| `cpg_list_available_concepts` / `cpg_list_available_operations` | The built-in concept/operation catalog (FQNs). | - |
| `cpg_apply_concepts` | Apply built-in concepts/operations (by FQN) to nodes. | graph |
| `cpg_list_llm_concepts_operations` | The persisted, LLM-defined concept schemas (`concepts.yaml`). | - |
| `cpg_add_or_update_llm_concept` | Declare/replace an LLM-defined concept schema (matched by name). | `concepts.yaml` |
| `cpg_suggest_llm_concepts_and_operations` | Validate a concept proposal's node ids without applying it. | - |
| `cpg_add_llm_concept_and_operations` | Apply LLM-defined concepts and their operations to nodes; also records their schemas. | graph, `concepts.yaml` |
| `cpg_dataflow` | Forward, intra-procedural, may-flow query between two applied concepts (`from`, `to`). | - |
| `cpg_dfg_backward` | All backward DFG paths from a node. | - |
| `cpg_get_last_write` | One-hop reaching writes of a node. | - |

Prompt: `suggest_concepts` (optional argument `description` to focus the analysis) guides a model
through exploring the CPG and proposing overlays for security-relevant nodes. Fetch it with
`ChatService.getPrompt` or your client's prompt picker and use its messages as the opening turn.

## Concepts and operations

There are two ways to tag nodes:

- **Built-in catalog** - concepts/operations that exist as classes in `cpg-concepts`. List them with
  `cpg_list_available_*`, apply them with `cpg_apply_concepts` by fully qualified class name.
- **LLM-defined (generic)** - the model invents the vocabulary. It first declares a schema with
  `cpg_add_or_update_llm_concept` (concept, operations, and typed properties with descriptions), then
  applies instances with `cpg_add_llm_concept_and_operations`. Schemas are persisted to a YAML file
  (`concepts.yaml` in the working directory by default; embedders choose the path) so later calls and
  later runs reuse them.

A schema property can declare a `fixedValue`, which overrides whatever the model supplies when
applied. A property of type `NodeReference` holds a real node id, which is resolved to the node
itself (and fails the application if the node does not exist). Every applied concept/operation also
participates in the data-flow graph.

## The chat agent

`ChatService` runs an LLM tool-calling loop against the MCP server. It is a library class, not a
CLI - embed it in a host application.

### Configuration

`ChatService.createIfConfigExist()` reads `application.conf` (standard
[Typesafe Config](https://github.com/lightbend/config) lookup, e.g. on the classpath or via
`-Dconfig.file=...`); copy `src/main/resources/application.conf.example` to start:

```hocon
llm.clients {
  vLLM   { baseUrl = "http://localhost:8000", contextLength = 262144 }   # contextLength optional
  openai { baseUrl = "https://api.openai.com", apiKeyEnv = "CODYZE_OPENAI_API_KEY" }
  gemini { baseUrl = "https://generativelanguage.googleapis.com/v1beta", apiKeyEnv = "CODYZE_GEMINI_API_KEY" }
}
mcp.serverUrl = "http://localhost:8081/mcp"
```

- A client named `gemini` uses the Gemini provider; every other name is treated as OpenAI-compatible.
- API keys are never stored in the file: `apiKeyEnv` names the environment variable to read.
- The model's context window is `contextLength` if set, else `max_model_len` reported by the server's
  `/v1/models` (vLLM), else a conservative 128k default. It drives history compression and
  oversized-result truncation, so set it explicitly for servers that do not report it.
- Generation parameters (temperature, max tokens, reasoning effort, ...) are constructor arguments of
  `ChatService`, not config-file keys.

### Using it

```kotlin
val chat = ChatService.createIfConfigExist() ?: error("no llm.clients configured")
chat.connect()                       // connects to mcp.serverUrl, discovers tools and skills

chat.chat(
    ChatRequestJSON(
        messages = listOf(ChatMessageJSON("user", "Which functions touch the network?")),
        client = "vLLM",
        model = "my-model",
        sessionId = "batch-1",       // keep tool-call history across calls; null = stateless
    )
).collect { event -> println(event) }   // JSON events, see below

chat.evictSession("batch-1")         // free the session's history when you are done
chat.close()
```

`chat()` returns a `Flow<String>` of JSON events, each with a `type`:

| `type` | Meaning |
|---|---|
| `text` | Assistant text. |
| `reasoning` | Model reasoning content, if the model produces it. |
| `tool_result` | A tool was called: `toolName`, `args`, `content`. |
| `task_status` | Structured completion signal: `done`, `resolvedItems`. |
| `usage` | Token usage summed over the whole call. |
| `final_history` | The conversation after in-call compression, for callers that resend history (only needed when `sessionId` is null). |
| `keepalive` | Sent first, covers a slow model cold start. Ignore it. |

Other useful methods: `getSkills()`, `getPrompt(name, arguments)`, `callTool(name, arguments)`,
`listAvailableProviders()`.

### What happens inside a call

- **Loop:** the model calls tools until it answers in plain text; at most `maxAgentIterations`
  (default 100) round trips. A text-only reply gets one nudge to continue before it counts as done,
  then the model is asked for a structured `task_status`. Tool calls that a weaker model writes as
  fenced JSON or `<tool_call>` text instead of using function calling are recognized as well.
- **History compression:** once the prompt exceeds 100 messages, or about a third of the context
  window, older turns are compressed into extracted facts (`defaultHistoryCompressionConcepts`; pass
  your own for a different task vocabulary) while the last 30 messages stay verbatim.
- **Oversized results:** as a last resort, a single tool result larger than half the context window is
  truncated, with a warning in the log. Pagination on the list tools usually keeps results well below
  that.
- **Parallel tools:** when the model requests several tool calls in one turn, only an explicit
  allowlist of read-only tools (`parallelSafeToolNames`) runs concurrently; everything else runs
  sequentially. A new tool is therefore sequential until you add it.

## Skills

A skill is a directory `.agents/skills/<name>/SKILL.md` (relative to the working directory) with
YAML frontmatter (`name`, `description`) followed by Markdown instructions; `name` must be
lower-case words joined by hyphens. `connect()` discovers them and adds a catalog to the system
prompt. The model "activates" a skill by reading its `SKILL.md` with `__read_file__`
(`__list_directory__` is also available).

Those two file tools are the **only** file access the model gets, and they are jailed to
`.agents/skills/`: any other path - including `../` escapes - is rejected. The agent can otherwise
only reason through the CPG.

## Extending and embedding

Run the server inside your own process:

```kotlin
val server = configureServer()                          // all default tools and the prompt
val http = runHttpMcpServerUsingKtorPlugin(port = 8081, host = "127.0.0.1", server = server)
globalAnalysisResult = translationResult                // the graph every tool will operate on
```

`configureServer { ... }` takes a lambda (a `Server.() -> Server`, so end it with `this`) that
registers the tools *instead of* the default set, so you can pick a subset or add your own. A custom
tool gets its JSON schema generated from a Kotlin payload class (non-nullable property = required,
`@Description` = parameter description):

```kotlin
fun Server.myTool() =
    addTool<MyPayload>(name = "my_tool", description = "...") { result: TranslationResult, payload ->
        CallToolResult(content = listOf(TextContent("...")))
    }
```

Rules for tools: the handler is not `suspend`, and the description of the payload's parameters is
appended to the tool description automatically, so do not repeat it. Pass `mutating = true` to
`addTool` if the handler changes the graph or does a read-modify-write on a file shared with other
tools (see [Concurrency](#concurrency)); forgetting it is the one way to get this wrong, because
the default is the cheap, shared one. Embedders that run `cpg_run_pass` must also set `ctx` (the
`TranslationContext`) next to `globalAnalysisResult`.

## Concurrency

The server holds **one CPG per process** in `globalAnalysisResult`; every tool operates on it. Keep
that in mind before sharing a server between independent users or runs.

The graph is not thread-safe, so tool calls go through `CpgLock`, a read/write lock:

- Tools registered through `addTool` run under the **read** lock by default, so any number of
  queries run in parallel.
- Tools registered with `mutating = true` run under the **write** lock, alone: `cpg_apply_concepts`,
  `cpg_run_pass` and `cpg_add_llm_concept_and_operations`. `cpg_analyze` and `cpg_translate` take it
  for the whole analysis, since they replace the graph. These are the tools marked *graph* in the
  *Writes* column above. While one runs, every other call waits.
- `cpg_add_or_update_llm_concept` only writes `concepts.yaml` (atomically, behind its own small lock)
  and does not wait for graph readers.

Both sides are reentrant, and a write holder may read. A tool that already holds the read lock cannot
take the write lock (it would deadlock), so `CpgLock.write` throws instead - register such a tool with
`mutating = true`. Code that touches the graph outside an MCP call, e.g. a host assigning
`globalAnalysisResult` while the server is running, can use `CpgLock.read { }` / `CpgLock.write { }`
itself.

## Development

```bash
./gradlew :cpg-ai:test               # unit tests
./gradlew :cpg-ai:integrationTest    # end-to-end MCP tests (need the language frontends)
./gradlew spotlessApply              # format before committing
```
