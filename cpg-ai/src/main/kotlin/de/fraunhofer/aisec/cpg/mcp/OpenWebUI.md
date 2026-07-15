# Running CPG MCP Server with OpenWebUI

## Quick Setup

1. Install the MCP-to-OpenAPI proxy:
```bash
pip install mcpo
```

2. Build the CPG MCP server:
```bash
./gradlew :cpg-ai:installDist
```

3. Start the proxy server:
```bash
uvx mcpo --port 8000 -- /path/to/cpg-ai/build/install/cpg-ai/bin/cpg-ai
```

5. Add to OpenWebUI:
   - Go to Settings -> Tools -> Add Connection 
   - Add API Base URL, e.g., `http://localhost:8000`

If its properly configured, the available tools should appear in the chat interface.