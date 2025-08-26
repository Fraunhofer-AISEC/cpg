# CPG MCP Server

## Available Tools

### Analysis Tools
- **`cpg_analyze`** : Parse code files to build the CPG
- **`cpg_llm_analyze`** : Generate prompt asking LLM to suggest concepts/operations
- **`cpg_apply_concepts`** : Apply the suggested concept/operations to specific nodes
- **`cpg_dataflow`** : Perform dataflow analysis

### Query and Listing Tools
- **`list_functions`** : List all function declarations
- **`list_records`** : List all record declarations (classes, structs, etc.)
- **`list_calls`** : List all call expressions
- **`list_calls_to`** : List all calls to a specific function or method
- **`list_available_concepts`** : List all available concepts
- **`list_available_operations`** : List all available operations
- **`list_concepts_and_operations`** : List all applied concepts and operations on nodes
- **`get_all_args`** : Get all arguments passed to function calls
- **`get_arg_by_index_or_name`** : Get specific argument by index or parameter name

## Setup

The MCP server can be used via two transport types:

- **Standard I/O Mode** and
- **Server-Sent Events (SSE)**.

The current implementation uses stdio since Claude Desktop only supports this transport type.

```bash
./gradlew :cpg-mcp:installDist
```

1. Open Claude Desktop
2. Go to Settings -> Developer -> Edit Config
3. Add the following configuration to the `mcpServers` section:

```json
    {
  "mcpServers": {
    "cpg": {
      "command": "/path/to/cpg-mcp/build/install/cpg-mcp/bin/cpg-mcp"
    }
  }
}
```

4. If you're navigating to the config file outside the app:
    - On Linux, it is usually located at `~/.config/claude-desktop/config.json`.
    - On macOS, it is typically at `~/Library/Application Support/Claude Desktop/config.json`.
5. Open the file in a text editor
6. Paste the configuration above into the `mcpServers` section
7. Save the file and restart Claude Desktop

## Usage

### Step 1: Parse Your Code (`cpg_analyze`)

First, parse the source code to build the Code Property Graph.
You can either provide an upload a file or paste code directly.

**Example:**

```json
{
  "tool": "cpg_analyze",
  "arguments": {
    "content": "def read_user_data():\n    with open('/etc/passwd') as f:\n        return f.read()",
    "fileName": "security_check.py"
  }
}
```

**Response:**

```json
{
  "fileName": "security_check.py",
  "totalNodes": 15,
  "functions": 3,
  "variables": 5,
  "callExpressions": 2,
  "nodes": [
    {
      "nodeId": "12345",
      "name": "open",
      "code": "open('/etc/passwd')",
      "fileName": "security_check.py",
      "startLine": 2,
      "endLine": 2,
      "startColumn": 10,
      "endColumn": 26
    }
  ]
}
```

### Step 2: Generate Prompt (`cpg_llm_analyze`)

This tool creates a prompt that asks the LLM to act as a software engineer with expertise in software security.
The LLM is provided with background information of the CPG and with examples of how to classify nodes into concepts and
operations.
Furthermore, it obtains a list of all existing concepts/operations and a list of all nodes in the current CPG analysis.

**LLM Response Format Expected:**

```json
{
  "overlaySuggestions": [
    {
      "nodeId": "1234",
      "overlay": "fully.qualified.class.Name",
      "overlayType": "Concept | Operation",
      "conceptNodeId": "string (REQUIRED for operations)",
      "reasoning": "Security reasoning for this classification",
      "securityImpact": "Potential security implications"
    }
  ]
}
```