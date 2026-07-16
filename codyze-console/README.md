# Codyze Console

A web application for Codyze with an optional AI chat, which is enhanced by an MCP client that acts as an agent.
The agent uses the tools of the CPG MCP server to analyze code and answer questions.

> [!IMPORTANT]
> codyze-console has a hard build dependency on the `cpg-ai` module, so `enableAIModule=true` must be set in `gradle.properties` (see [AI Chat Features](#ai-chat-features) below) before building or running codyze-console at all - otherwise the build fails.

## Getting Started

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

codyze-console has a hard build dependency on the `cpg-ai` module (MCP server, `ChatService`, skills). The AI chat itself additionally requires a configured LLM provider to actually work at runtime.

### 1. Enable the `cpg-ai` module

`cpg-ai` is optional at the workspace level (like the language frontends), but codyze-console cannot be built without it. Run the configuration script:

```bash
./configure_frontends.sh
```

Or enable it manually by setting `enableAIModule=true` in `gradle.properties`. If you build codyze-console with `cpg-ai` disabled, the build fails with an explicit error telling you to enable it.

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