# AGENTS.md

This file provides guidance to AI coding agents (Claude Code, Cursor, Codex, Gemini CLI, etc.) working in this repository.

## Project Overview

**CPG** is a code property graph library and analysis platform.

Key modules:
- **cpg-core** – Core library: AST nodes, graph structures, passes, type system
- **cpg-analysis** – Higher-level analyses built on top of cpg-core (dataflow, control flow, call graphs, etc.)
- **cpg-language-\*** – Language frontends, one module per language (e.g., `cpg-language-go`, `cpg-language-python`)
- **cpg-concepts** – Concept and operation definitions
- **cpg-mcp** – MCP server exposing CPG analysis tools (dataflow, symbol analysis, concept application) to LLMs via streamable HTTP
- **codyze-console** – Web-based analysis UI with AI agent chat (see [Architecture](#codyze-console-architecture) below)

## Technology Stack

### Backend
- **Language**: Kotlin (requires Java 21)
- **Build**: Gradle with Kotlin DSL
- **Testing**: JUnit 5, kotlin.test
- **Formatting**: Google Java Style via spotless plugin

### Frontend (`codyze-console/src/main/webapp`)
- **Framework**: Svelte 5 with SvelteKit
- **Styling**: Tailwind CSS
- **Package Manager**: pnpm (not npm)

## Development Commands

### Build

```bash
# Full build (format + test + publish locally)
./gradlew clean spotlessApply build publishToMavenLocal

# Quick build
./gradlew build

# Build a single module
./gradlew :cpg-core:build
./gradlew :codyze-console:build
```

### Test & Quality

```bash
./gradlew test                  # Unit tests
./gradlew integrationTest       # Integration tests
./gradlew performanceTest       # Performance tests
./gradlew spotlessApply         # Auto-format 
./gradlew spotlessCheck         # Check formatting only
```

### Frontend (`codyze-console`)

```bash
cd codyze-console/src/main/webapp
pnpm install        # Install dependencies
pnpm run dev        # Dev server
pnpm run build      # Production build
pnpm run check      # Type check
pnpm run lint       # Lint
pnpm run format     # Format
```

### Backend (`codyze-console`)

```bash
./gradlew :codyze-console:compileKotlin --console=plain
```

### MCP Server (`cpg-mcp`)

```bash
./gradlew :cpg-mcp:installDist           # Build & install
./gradlew :cpg-mcp:run                   # Run (stdio)
./gradlew :cpg-mcp:run --args="--http 8080"  # Run with streamable HTTP on port 8080
```

## Code Conventions

- Follow **Google Java Style** (enforced by spotless – run `./gradlew spotlessApply` before committing)
- Kotlin idioms preferred over Java-style patterns
- Use `kotlin.test` assertions in tests, not JUnit assertions directly
- Frontend: use **Svelte 5 runes** exclusively (no legacy `$:` reactive syntax)

## codyze-console Architecture

codyze-console is a full-stack web application with a Ktor backend and a Svelte 5 SPA frontend. It provides code analysis capabilities and an AI agent chat interface.

### Backend (`codyze-console/src/main/kotlin/.../console/`)

| File / Package | Responsibility |
|---|---|
| `Main.kt` | Ktor/Netty entry point (port 8080), optionally starts MCP server on port 8081 |
| `Router.kt` | REST API routes (`/api/analyze`, `/api/chat`, `/api/querytrees`, etc.) |
| `ConsoleService.kt` | Core business logic: CPG analysis, QueryTree caching, concept management |
| `Nodes.kt` | JSON serialization models and CPG node-to-JSON conversion |
| `ai/ChatClient.kt` | MCP client + agentic tool-calling loop (connects to cpg-mcp via streamable HTTP) |
| `ai/ChatService.kt` | Loads LLM config from HOCON, creates `ChatClient` with configured provider |
| `ai/LlmClient.kt` | Provider-agnostic LLM interface (`sendPrompt` -> `List<ToolCall>`) |
| `ai/OpenAiClient.kt` | OpenAI-compatible client (also works with Ollama, vLLM, MLX) |
| `ai/GeminiClient.kt` | Google Gemini API client |
| `ai/McpServerHelper.kt` | Reflection-based bridge to cpg-mcp; loads module dynamically so it remains an optional dependency |
| `ai/ClientModels.kt` | Data classes for OpenAI/Gemini request/response formats |

#### AI Agent Data Flow

1. Frontend sends chat messages via `POST /api/chat` (SSE stream)
2. `ChatClient` forwards to LLM with MCP tool definitions
3. If LLM returns tool calls, `ChatClient` executes them against cpg-mcp via `mcp.callTool()`
4. Tool results are streamed to the frontend and fed back to the LLM
5. Loop repeats (max 8 iterations) until the LLM produces a text response

#### MCP Integration

codyze-console acts as both **MCP server host** and **MCP client**:
- It starts the cpg-mcp server on port 8081 (via reflection, so cpg-mcp is an optional dependency)
- `ChatClient` connects to it as a client using `StreamableHttpClientTransport`
- After analysis, the global `TranslationResult` is injected into the MCP server so tools can access the CPG

### Frontend (`codyze-console/src/main/webapp/src/`)

| Directory | Content |
|---|---|
| `routes/` | SvelteKit pages: dashboard, components, requirements, chat, new-analysis |
| `lib/components/ai-agent/` | Chat UI: `ChatInterface`, `WelcomeScreen`, `MessageInput`, `McpCapabilitiesModal` |
| `lib/components/analysis/` | Code viewer, node tables, findings list, file tree |
| `lib/components/requirements/` | Requirement cards, charts, QueryTree explorer (lazy-loading) |
| `lib/services/apiService.ts` | SSE streaming utility (`streamPost` for `/api/chat`) |
| `lib/services/llmAgent.ts` | Chat API facade wrapping `apiService` |
| `lib/stores/queryTreeStore.ts` | In-memory cache for QueryTrees with batch fetching and lazy loading |
| `lib/types.ts` | TypeScript interfaces mirroring backend JSON models |

### API Endpoints

**Analysis:** `POST /api/analyze`, `POST /api/reanalyze`, `GET /api/result`, `GET /api/component/{name}`

**Chat & MCP:** `POST /api/chat` (SSE), `GET /api/chat/mcp/capabilities`, `POST /api/chat/mcp/prompts/{name}`, `GET /api/features`

**Requirements:** `GET /api/requirement/{id}`, `GET /api/querytree/{id}`, `POST /api/querytrees` (batch), `GET /api/querytrees/{id}/parents`