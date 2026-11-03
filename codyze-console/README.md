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

## Using the agent page

The **Agent** page (`/chat`) combines a code editor with the AI chat: you read the code, inspect what the analysis knows about it, and ask the agent about it, all in one place.

### Layout

- **Left:** the files of the selected component (with the number of concepts per file), the outline of the open file (its functions with their callers, external calls, and concepts and operations), and the findings of the requirements.
- **Middle:** the code, with editor tabs and a breadcrumb (component, path, and the enclosing function).
- **Right:** the **Inspector** and the **Agent** tabs. While the agent works, the inspector tab is only marked when you inspect something, so the conversation stays in view.
- **Graph panel:** the dependence graph (PDG) of a statement, opened from the context menu or the command palette.

### Inspecting code

- **Click** anywhere in the code to inspect the innermost node there. The inspector shows its type, location, calls (targets, callers, calls in its body), concepts and operations, direct dataflows (expandable into a tree), and warnings where the analysis is uncertain, e.g. unresolved calls or code that is not part of the analysis. Long lists are limited and show e.g. "200 of 1234".
- **Hover** over the code for a short summary of the node.
- The **code lens** after the first line of a function shows its callers, calls and external calls; each part opens the matching section of the inspector.
- **Right-click** for the context menu: what affects a statement or what it affects (PDG), callers and callees, and asking the agent about it.
- **Layers** in the code header toggle what is shown in the code:

  | Layer | Shows |
  |---|---|
  | ◆ Concepts | Concepts and operations, as icons in the gutter |
  | ↗ External | Calls to code that is not part of the analysis, underlined |
  | ▲ Findings | Places that violate a requirement |
  | → Dataflow | The direct dataflows of the selection, as arcs next to the line numbers |
  | ● Agent | The steps of the active question to the agent, in the bar above the code |

- **Dataflow paths:** following a dataflow in the inspector shows it as a path with numbered steps, which you can step through.

### Asking the agent

- Ask in the chat, or **select code** and press `Ctrl/Cmd+K` (or use the small input next to the selection) to ask about exactly that code.
- The agent cites the nodes its answer relies on. Citations are links into the code, and the evidence of the active question is marked in the code and can be stepped through like in a debugger.
- Under an answer, **trust warnings** list the places its evidence relies on where the analysis is uncertain, e.g. unresolved calls or calls to external code such as `printf`.

### Keyboard

| Shortcut | Action |
|---|---|
| `Ctrl/Cmd+P` | Open a file by name |
| `Ctrl/Cmd+Shift+P` | Command palette |
| `Ctrl/Cmd+K` | Ask the agent about the selected code |
| `Alt+←` / `Alt+→` | Back / forward through the visited locations |
| `Esc` | Close the open popup or panel |

The command palette lists all commands with their shortcuts, e.g. to toggle layers, step through a dataflow path, open the dependence graph, or show and hide the parts of the layout.

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
   |                    |  AIAgent (Koog)    |                   |
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
   |                    |  AIAgent (Koog)    |                   |
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
LLM (for the next iteration) and the frontend. This loop continues until the LLM responds with text instead of tool calls, or
until a limit of iterations is reached. The loop runs on Koog's `AIAgent`, and the history of a conversation is kept per session
with `ChatMemory`. Besides `text` and `tool_result`, the stream contains further events such as `reasoning` and `usage`; see the
[event table of cpg-ai](../cpg-ai/README.md#using-it).

The analysis and the agent share one graph: after an analysis, the console registers its `TranslationResult` as the session of
the project in the MCP server, which runs in the same process. Concepts that the agent applies therefore show up in the UI.
The agent page itself reads the graph through the REST API of the console (e.g. `/api/node/{id}`, `.../node-at`,
`.../functions`, `.../calls`, `.../overlays`, `/api/trust` and the graph endpoints); `Router.kt` documents all endpoints.