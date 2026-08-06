# Codyze Console

A web application for Codyze with an optional AI chat, which is enhanced by an MCP client that acts as an agent.
The agent uses the tools of the CPG MCP server to analyze code and answer questions.

> [!IMPORTANT]
> codyze-console has a hard, unconditional build dependency on the `cpg-ai` module (see [AI Chat Features](#ai-chat-features) below). Enabling `enableCodyzeConsole=true` in `gradle.properties` always enables `cpg-ai` too - no separate step needed, and even an explicit `enableAIModule=false` is overridden while `enableCodyzeConsole=true`.

## Getting Started

Codyze Console is an optional module. Enable it by setting `enableCodyzeConsole=true` in `gradle.properties` (or via `./configure_frontends.sh`), then rerun/resync Gradle.

The easiest way to get started is by using the predefined IntelliJ run configurations in the `.run/` directory:
- *Codyze Console* (standalone)
- *Codyze Compliance Scan (with Console and Example)* (with an example project).

Alternatively, starting the application from the command line:

```bash
# Start the console only
./gradlew :codyze:run --args="console"

# With an analysis of a project
./gradlew :codyze:run --args="compliance scan --project-dir <path> --console=true"
```

The web console is available at `http://localhost:8080`.

## AI Chat Features

codyze-console has a hard, unconditional build dependency on the `cpg-ai` module (MCP server, `ChatService`, skills), which is always enabled together with `enableCodyzeConsole=true` as described above - no separate step needed. The AI chat itself additionally requires a configured LLM provider to actually work at runtime.

### 1. Enable the `cpg-ai` module independently (optional)

`cpg-ai` is optional at the workspace level (like the language frontends), but codyze-console cannot be built without it, so it's always enabled alongside `enableCodyzeConsole=true`. If you want `cpg-ai` enabled without `codyze-console` (e.g. just the MCP server), use the configuration script:

```bash
./configure_frontends.sh
```

Or set `enableAIModule=true` manually in `gradle.properties`. Note this only matters independently of `codyze-console`: an explicit `enableAIModule=false` has no effect while `enableCodyzeConsole=true`, since that dependency is unconditional.

### 2. Configure your LLM provider

Copy the example configuration:

```bash
cp cpg-ai/src/main/resources/application.conf.example cpg-ai/src/main/resources/application.conf
```

Then edit `application.conf` and configure the clients you want to use under `llm.clients`:

```hocon
llm {
  clients {
    ollama {
      baseUrl = "http://localhost:11434"
    }

    openai {
      baseUrl = "https://api.openai.com"
      apiKeyEnv = "CODYZE_OPENAI_API_KEY"
    }

    gemini {
      baseUrl = "https://generativelanguage.googleapis.com/v1beta"
      apiKeyEnv = "CODYZE_GEMINI_API_KEY"
    }
  }
}
```

Each entry defines a `baseUrl` and, if the provider requires authentication, an `apiKeyEnv` that names the environment variable holding the key. The model itself is no longer set in the config, instead it can be selected in the chat UI.

Currently, only Gemini and OpenAI-compatible endpoints are supported.

### 3. MCP Server

The MCP server is automatically started on port `8081` whenever codyze-console starts. The AI chat connects to it as an MCP client to access the CPG tools (e.g., listing functions, records, and calls).

## Architecture

The following diagram shows the interaction between the main components during a chat request:

```
Frontend            Backend              LLM               MCP Server
(Svelte)           (ChatService)      (Gemini/OpenAI)       (cpg-ai) 
   |                    |                    |                   |
   | POST /api/chat     |                    |                   |
   | {messages}         |                    |                   |
   |------------------->|                    |                   |
   |                    |                    |                   |
   |                    |  sendPrompt()      |                   |
   |                    |  (messages + tools)|                   |
   |                    |------------------->|                   |
   |                    |                    |                   |
   |                    |   "call tool X     |                   |
   |                    |    with args Y"    |                   |
   |                    |<-------------------|                   |
   |                    |                    |                   |
   |                    |  mcp.callTool(X, Y)                    |
   |                    |--------------------------------------> |
   |                    |                    |                   |
   |                    |                         tool result    |
   |                    |<-------------------------------------- |
   |                    |                    |                   |
   |   tool_result      |                    |                   |
   |<-------------------|                    |                   |
   |                    |                    |                   |
   |                    |  sendPrompt()      |                   |
   |                    |  (+ tool results)  |                   |
   |                    |------------------->|                   |
   |                    |                    |                   |
   |                    |   text response    |                   |
   |                    |<-------------------|                   |
   |                    |                    |                   |
   |       text         |                    |                   |
   |<-------------------|                    |                   |
```

The LLM decides which tools to call and the backend executes the tool calls on the MCP server, and streams results back to both the 
LLM (for the next iteration) and the frontend. This loop continues until the LLM responds with text instead of tool calls.