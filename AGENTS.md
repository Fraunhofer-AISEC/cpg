# AGENTS.md

This file provides guidance to AI coding agents (Claude Code, Cursor, Codex, Gemini CLI, etc.) working in this repository.

## Project Overview

**CPG** is a code property graph library and analysis platform.

Key modules:
- **cpg-core** – Core library: AST nodes, graph structures, passes, type system
- **cpg-analysis** – Higher-level analyses built on top of cpg-core (dataflow, control flow, call graphs, etc.)
- **cpg-language-\*** – Language frontends, one module per language (e.g., `cpg-language-go`, `cpg-language-python`)
- **cpg-concepts** – Concept and operation definitions
- **cpg-mcp** – MCP (Model Context Protocol) server exposing CPG analysis tools to LLMs
- **codyze-console** – Web-based analysis UI: Kotlin backend + Svelte 5 frontend

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