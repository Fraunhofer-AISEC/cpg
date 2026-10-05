# AGENTS.md

Guidance for AI coding agents (Claude Code, Copilot, Cursor, Codex, Gemini CLI, etc.) working in this repository. `CLAUDE.md` is a symlink to this file.

## Project Overview

**CPG** is a code property graph library and analysis platform, written in Kotlin (Java 21, Gradle Kotlin DSL).

| Module | Purpose |
|---|---|
| `cpg-core` | AST nodes, graph structures, passes, type system |
| `cpg-analysis` | Higher-level analyses (dataflow, control flow, call graphs, ...) |
| `cpg-concepts` | Concept and operation definitions |
| `cpg-language-*` | One language frontend per module (e.g. `cpg-language-go`, `cpg-language-python`) |
| `cpg-serialization` | JSON serialization for CPG nodes |
| `cpg-neo4j` | Export of the CPG to Neo4j |
| `cpg-ai` | MCP server exposing CPG analysis tools to LLMs, plus chat/skills integration |
| `codyze-core` | Codyze analysis core (project API, queries, results) |
| `codyze-compliance` | Compliance checking and reporting |
| `codyze-console` | Web UI (Ktor backend + Svelte 5 frontend) with AI agent chat |

## Commands

Prefer the narrowest task that verifies your change. Full builds are slow.

```bash
./gradlew :cpg-core:compileKotlin                     # Compile a single module
./gradlew :cpg-core:test --tests "*SomeTest*"          # Run specific tests
./gradlew :cpg-core:test                              # Unit tests of a module
./gradlew :cpg-core:integrationTest                   # Integration tests (also: performanceTest)
./gradlew spotlessApply                               # Format + license headers (run before committing)
./gradlew build                                       # Full build (slow)
```

Language frontends can be disabled in `gradle.properties` (`enableGoFrontend=false`, etc.) to speed up builds. Do not commit such changes.

### Frontend (`codyze-console/src/main/webapp`)

Run all of these from that directory. Use **pnpm**, never npm.

```bash
pnpm install
pnpm run check      # Type check
pnpm run lint       # Prettier + ESLint
pnpm run format     # Auto-format
pnpm run build
```

### Running things

- **Do not start the codyze-console backend yourself.** Ask the user to do it.
- MCP server: `./gradlew :cpg-ai:run` (stdio) or `./gradlew :cpg-ai:run --args="--http 8080"` (streamable HTTP).

## Code Conventions

### Kotlin

- Formatting is done by spotless with **ktfmt (kotlinlang style)**. Run `./gradlew spotlessApply` instead of hand-formatting.
- Every source file needs the Apache 2.0 license header. `spotlessApply` adds it to new files.
- Prefer Kotlin idioms over Java-style patterns. Write KDoc for public APIs.
- Tests: JUnit 5 with `kotlin.test` assertions (not JUnit assertions directly).

### CPG node patterns (see `CONTRIBUTING.md` for details)

- Property edges: name the edge property `<singular>Edges` (e.g. `parameterEdges`) and expose the nodes via `var parameters by unwrapping(Function::parameterEdges)`.
- Required properties are non-nullable and default to a problem node, e.g. `var base: Expression = newProblemExpression("...")`.
- In `equals`, compare edge lists with `propertyEqualsList(...)`. `hashCode` must include every property compared in `equals`.

### Frontend

- Svelte 5 **runes only**: `$state`, `$derived`, `$effect`, `$props`. No legacy `$:` or `export let`.
- Svelte 5 event syntax (`onclick`, not `on:click`). Use `<button>` for interactive elements, not clickable `<div>`s.
- Styling with Tailwind CSS. Keep the design clean and minimal.
- Page data is loaded in `+page.ts` and read in `+page.svelte` via `let { data }: PageProps = $props()`.

## Git & Pull Requests

- Use [Conventional Commits](https://www.conventionalcommits.org/) for commit messages **and PR titles**. PRs are squash-merged, so the PR title becomes the commit on `main`.
  - Format: `<type>(<scope>): <description>`. Types: `feat`, `fix`, `perf`, `refactor`, `test`, `docs`, `build`, `ci`, `chore`.
  - Scope is optional and should be the short module name: drop the `cpg-` / `cpg-language-` prefix (`core`, `analysis`, `concepts`, `ai`, `go`, `python`, `cxx`, ...). Codyze modules keep their full name (`codyze-console`, `codyze-core`).
  - Examples: `feat(core): add UnknownMemoryValue`, `fix(go): resolve scopes via AST lookup`, `perf(codyze-console): virtualize node tables`.
  - Mark breaking changes with `!`, e.g. `refactor(core)!: remove fluent DSL`.
- Keep PRs small and focused: one concern per PR. Split unrelated changes into separate PRs.
- Include tests with changes whenever possible.
- PRs that change the graph or analysis interfaces need a changelog section in the PR description (format in `CONTRIBUTING.md`).
- Never commit stray local files (scratch tests, patches, archives, build outputs).
- **Never push directly to `main`.** All changes go through a feature branch and a pull request.
- **Never reply on GitHub on behalf of a human.** Do not post comments, reviews, or answers to issues, PRs, or discussions. Communication on GitHub is done by humans. Draft a reply locally if asked, and let the user post it.

## codyze-console Architecture

Ktor backend (`codyze-console/src/main/kotlin/.../console/`) plus a Svelte 5 SPA (`src/main/webapp/src/`). The backend serves on port 8080 and starts the `cpg-ai` MCP server on port 8081.

- `Router.kt` holds the REST API (`/api/*`), `ConsoleService.kt` the business logic (analysis, QueryTree caching, concepts), and `Nodes.kt` the JSON models.
- The AI chat lives in **`cpg-ai`**, not codyze-console: `ChatService` (LLM config from HOCON, agentic tool-calling loop, max 50 iterations), `clients/` (OpenAI-compatible and Gemini), `skills/`, `mcp/`.
- Chat flow: frontend `POST /api/chat` (SSE). `ChatService` sends MCP tool definitions to the LLM, executes the returned tool calls against the local MCP server (`StreamableHttpClientTransport`), streams results back, and loops until the LLM answers with text.
- After analysis, the `TranslationResult` is injected into the MCP server via `globalAnalysisResult` (`cpg.ai.mcp.mcpserver.tools`).
- codyze-console depends on `cpg-ai` directly. Enabling codyze-console force-enables `cpg-ai` (`enableAIModule` in `settings.gradle.kts`).
- Frontend: `lib/types.ts` mirrors the backend JSON models, so keep them in sync with `Nodes.kt`.
