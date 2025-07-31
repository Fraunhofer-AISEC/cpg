# CPG MCP Server

## Available Tools

- **`cpg_analyze`** : Parse code files to build the CPG
- **`cpg_llm_analyze`** : Generate security analysis prompts asking LLM to suggest concepts/operations
- **`cpg_apply_concepts`** : Apply concept overlays to specific code nodes
- **`cpg_dataflow`** : Perform dataflow analysis

## Setup

The MCP server can be used via two transport types:

- **Standard I/O Mode** and
- **Server-Sent Events (SSE)**.

The current implementation uses stdio since Claude Desktop only supports this transport type.

```bash
./gradlew :cpg-mcp:installDist
```

Add to your Claude Desktop config:

```json
{
  "mcpServers": {
    "cpg": {
      "command": "/path/to/cpg-mcp/build/install/cpg-mcp/bin/cpg-mcp"
    }
  }
}
```

## Usage

### Step 1: Parse Your Code (`cpg_analyze`)

First, parse the source code to build the Code Property Graph.
You can either provide a file path or paste code directly.

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

### Step 2: Generate Security Analysis Prompt (`cpg_llm_analyze`)

This tool creates a security analysis prompt that asks the LLM to act as a security officer.
The LLM is instructed to research the CPG documentation and suggest appropriate security concepts.

**Example:**

```json
{
  "tool": "cpg_llm_analyze",
  "arguments": {
    "description": "Focus on authentication and data access patterns in this Python web application"
  }
}
```

**Response:** A comprehensive security analysis prompt that includes:

- Instructions for the LLM to take on the role of a security officer
- Background examples (e.g., `open('/etc/passwd', 'r')` → 'Data' concept)
- Direction to research CPG repository and documentation
- All analyzed nodes from the current CPG analysis
- Request for JSON response with security findings and concept suggestions

**LLM Response Format Expected:**
```json
{
  "analysis_summary": "Brief overview of security findings",
  "concept_suggestions": [
    {
      "nodeId": "node_123",
      "conceptType": "Data|ReadData|Authentication|HttpRequest|etc",
      "reasoning": "Detailed security reasoning for this classification",
      "security_impact": "Potential security implications"
    }
  ],
  "additional_recommendations": "Any additional security recommendations"
}
```

### WIP: Step 3: Apply the Concepts (`cpg_apply_concepts`)


**Example:**

```json
{
  "tool": "cpg_apply_concepts",
  "arguments": {
    "applications": [
      {
        "nodeId": "12345",
        "conceptType": "Data"
      },
      {
        "nodeId": "12346",
        "conceptType": "ReadData"
      }
    ]
  }
}
```

**Result:**

```json
[
  "Applied Data to node 12345 (CallExpression)",
  "Applied ReadData to node 12346 (FunctionDeclaration)"
]
```

### WIP: Step 4: Analyze Data Flow (`cpg_dataflow`)

**Example:**

```json
{
  "tool": "cpg_dataflow",
  "arguments": {
    "from": "Data",
    "to": "ReadData"
  }
}
```

**Response:**

```json
{
  "fromConcept": "Data",
  "toConcept": "ReadData",
  "foundPaths": [
    {
      "id": "flow-123",
      "value": "true",
      "node": {
        "nodeId": "12345",
        "name": "open",
        "code": "open('/etc/passwd')",
        "fileName": "security_check.py",
        "startLine": 2
      },
      "children": [
        {
          "id": "flow-124",
          "value": "true",
          "node": {
            "nodeId": "12346",
            "name": "f",
            "code": "f.read()",
            "fileName": "security_check.py",
            "startLine": 3
          },
          "children": []
        }
      ]
    }
  ]
}
```