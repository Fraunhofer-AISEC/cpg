# Codyze Console

A web application for Codyze with an optional AI chat, which is enhanced by an MCP client that acts as an agent.
The agent uses the tools of the CPG MCP server to analyze code and answer questions.

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

The AI chat requires the `cpg-mcp` module to be enabled and a configured LLM provider.

### 1. Enable the `cpg-mcp` module

Run the configuration script:

```bash
./configure_frontends.sh
```

Or enable it manually by setting `enableMCPModule=true` in `gradle.properties`.

### 2. Configure your LLM provider

Copy the example configuration:

```bash
cp codyze-console/src/main/resources/application.conf.example codyze-console/src/main/resources/application.conf
```

Then edit `application.conf` and set the `client` field of the provider:

```hocon
llm {
  client = "ollama"

  ollama {
    baseUrl = "http://localhost:11434"
    model = "llama3"
  }

  # ... other providers are preconfigured as placeholders.
}
```

Currently, only Gemini and OpenAI-compatible endpoints are supported. The predefined clients (`ollama`, `vLLM`, `mlx`, etc.) use all the same OpenAI-compatible client internally. They are intended for testing and development and allows to switch between different server URLs without reconfiguring every time.

### 3. MCP Server

When `cpg-mcp` is enabled, the MCP server is automatically started on port `8081`. The AI chat connects to it as an MCP client to access the CPG tools (e.g., listing functions, records, and calls).