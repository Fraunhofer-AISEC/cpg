# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Building the Project
```bash
# Full build with formatting and tests
./gradlew clean spotlessApply build publishToMavenLocal

# Quick build
./gradlew build

# Build specific module
./gradlew :cpg-core:build
./gradlew :codyze-console:build
```

### Code Quality and Testing  
```bash
# Run all tests
./gradlew test

# Run integration tests  
./gradlew integrationTest

# Run performance tests
./gradlew performanceTest

# Apply code formatting (Google Java Style)
./gradlew spotlessApply

# Check code formatting
./gradlew spotlessCheck

# Generate test coverage report
./gradlew jacocoTestReport
```

### Frontend Development (codyze-console)
```bash
# Navigate to webapp directory first
cd codyze-console/src/main/webapp

# Install dependencies
pnpm install

# Development server
pnpm run dev

# Build frontend
pnpm run build

# Type checking
pnpm run check

# Lint and format
pnpm run lint
pnpm run format
```

### Backend Development (codyze-console)
```bash
# Compile Kotlin backend
./gradlew :codyze-console:compileKotlin --console=plain
```

### MCP Server Development (cpg-mcp)
```bash
# Build and install MCP server
./gradlew :cpg-mcp:installDist

# Run MCP server (stdio mode for Claude Desktop)
./gradlew :cpg-mcp:run

# Run MCP server with SSE mode on port 8080
./gradlew :cpg-mcp:run --args="--sse 8080"
```

## Project Architecture

### Core Structure
- **cpg-mcp**: MCP (Model Context Protocol) server providing CPG analysis tools for LLM integration
- **codyze-console**: Kotlin backend with Svelte 5 frontend for web-based analysis
- **cpg-core**: Core library containing AST nodes, graph structures, passes, and type system
- **cpg-concepts**: Concept and operation definitions

### Technology Stack

#### Backend (cpg-core, codyze modules)
- **Language**: Kotlin (requires Java 21)
- **Build**: Gradle with Kotlin DSL
- **Testing**: JUnit 5, kotlin.test, Mockk
- **Formatting**: Google Java Style (spotless plugin)

#### Frontend (codyze-console webapp)  
- **Framework**: Svelte 5 with SvelteKit
- **Styling**: Tailwind CSS
- **Package Manager**: pnpm (required, not npm)
- **Charts**: Chart.js

### Key Development Patterns

#### Svelte 5 Runes (codyze-console frontend)
```javascript
// Reactive state
let count = $state(0);

// Derived values  
const doubled = $derived(count * 2);

// Effects
$effect(() => {
  console.log('Count changed:', count);
});

// Component props
let { items }: Props = $props();
```

**Available MCP Tools:**
- `cpg_analyze` - Parse code files to build CPG
- `cpg_llm_analyze` - Generate LLM prompts for concept/operation suggestions  
- `cpg_apply_concepts` - Apply suggested concepts/operations to nodes
- `cpg_dataflow` - Perform dataflow analysis
- `list_functions` - List function declarations
- `list_records` - List record declarations (classes, structs)
- `list_calls` - List call expressions
- `list_calls_to` - List calls to specific functions
- `list_available_concepts` - List available concepts
- `list_available_operations` - List available operations