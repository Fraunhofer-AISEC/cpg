# Technology Stack

## Core Development
- **Language:** Kotlin (Primary development language for the library and core analysis logic).
- **Build System:** Gradle with Kotlin DSL (`build.gradle.kts`) for multi-module project management.
- **Minimum Java Version:** Target compatibility for modern JVM environments (supporting Java 17+ features where applicable).

## Language Frontends & Parsing
- **C/C++:** Eclipse CDT (Used for its forgiving parsing capabilities for C and C++).
- **Java:** JavaParser (Selected for its ability to handle incomplete or semantically incorrect Java source code).
- **LLVM IR:** javacpp (Provides the interface to the native LLVM libraries for parsing LLVM Intermediate Representation).
- **Experimental Frontends:** Golang, Python, TypeScript, Ruby, JVM, INI.

## Graph Representation & Analysis
- **Internal Model:** Custom Code Property Graph (CPG) implementation consisting of AST, CFG, and DFG.
- **Analysis Passes:** Modular system for graph enrichment (e.g., `CallResolver`, `DFGPass`, `InferencePasses`).

## Persistence & Interoperability
- **Graph Database:** Neo4j (Primary persistence target for large-scale graph analysis and visualization via Cypher).
- **Standards:** SARIF (Static Analysis Results Interchange Format) for interoperability with other security tools.
- **Native Interop:** javacpp for native library interaction (LLVM).

## Infrastructure & Testing
- **CI/CD:** GitHub Actions (defined in `.github/workflows/build.yml`).
- **Code Coverage:** Codecov integration.
- **Testing Framework:** Kotlin Test / JUnit.
