# Initial Concept
The Code Property Graph (CPG) is a simple library to extract a *code property graph* out of source code. It supports multiple passes that can extend the analysis after the graph is constructed. It currently supports C/C++ (C17), Java (Java 13) and has experimental support for Golang, Python, TypeScript and Rust. Furthermore, it has support for the LLVM IR.

# Product Definition

## Vision
To provide a unified, searchable, and extensible representation of source code that enables advanced security analysis, architectural discovery, and automated refactoring across multiple programming languages.

## Target Audience
- **Security Researchers:** To identify complex vulnerability patterns (e.g., data flow from untrusted sources to sensitive sinks).
- **Software Architects:** To visualize and analyze large-scale system dependencies and architectural patterns.
- **Compiler & Tools Engineers:** To build program analysis tools on top of a "forgiving" and language-agnostic graph representation.

## Core Value Proposition
- **Multi-Language Support:** Analyzes C/C++, Java, Go, Python, TypeScript, and Rust using a unified graph model.
- **Forgiving Parsing:** Handles incomplete or semantically incorrect code, making it ideal for real-world analysis where full build environments are often unavailable.
- **Extensible Analysis:** A modular "Pass" system allows users to implement custom analysis logic (e.g., call graph construction, data flow analysis) on top of the base graph.
- **Graph Database Integration:** Seamlessly exports to Neo4j and other graph databases to leverage powerful query languages like Cypher for code exploration.

## Key Features
- **Graph Construction:** Extracts AST, CFG, and DFG into a unified Code Property Graph.
- **Modular Architecture:** Language frontends and analysis passes are decoupled for easy extension.
- **Persistence:** Support for Neo4j and SARIF output.
- **Inference Engine:** Automatically infers missing information (e.g., record declarations, call targets) to provide a more complete graph.
