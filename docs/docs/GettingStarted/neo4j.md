---
title: "Using neo4j"
linkTitle: "Using neo4j"
no_list: true
weight: 2
date: 2025-01-10
description: >
  Using neo4j for visualization (cpg-n2o4j)
---

# Neo4J visualisation tool for the Code Property Graph 

A simple tool to export a *code property graph* to a neo4j database.

## Requirements

The application requires Java 17.

## Build

Build (and install) a distribution using Gradle

```
../gradlew installDist
```

Please remember to adjust the `gradle.properties` before building the project.

## Usage

```
./build/install/cpg-neo4j/bin/cpg-neo4j  [--infer-nodes] [--load-includes] [--no-default-passes]
                    [--no-neo4j] [--no-purge-db] [--print-benchmark]
                    [--use-unity-build] [--benchmark-json=<benchmarkJson>]
                    [--custom-pass-list=<customPasses>]
                    [--export-json=<exportJsonFile>] [--host=<host>]
                    [--includes-file=<includesFile>]
                    [--password=<neo4jPassword>] [--port=<port>]
                    [--save-depth=<depth>] [--top-level=<topLevel>]
                    [--user=<neo4jUsername>] ([<files>...] | -S=<String=String>
                    [-S=<String=String>]... |
                    --json-compilation-database=<jsonCompilationDatabase> |
                    --list-passes)
      [<files>...]           The paths to analyze. If module support is
                               enabled, the paths will be looked at if they
                               contain modules
      --benchmark-json=<benchmarkJson>
                             Save benchmark results to json file
      --custom-pass-list=<customPasses>
                             Add custom list of passes (includes
                               --no-default-passes) which is passed as a
                               comma-separated list; give either pass name if
                               pass is in list, or its FQDN (e.g.
                               --custom-pass-list=DFGPass,CallResolver)
      --export-json=<exportJsonFile>
                             Export cpg as json
      --host=<host>          Set the host of the neo4j Database (default:
                               localhost).
      --includes-file=<includesFile>
                             Load includes from file
      --infer-nodes          Create inferred nodes for missing declarations
      --json-compilation-database=<jsonCompilationDatabase>
                             The path to an optional a JSON compilation database
      --list-passes          Prints the list available passes
      --load-includes        Enable TranslationConfiguration option loadIncludes
      --no-default-passes    Do not register default passes [used for debugging]
      --no-neo4j             Do not push cpg into neo4j [used for debugging]
      --no-purge-db          Do no purge neo4j database before pushing the cpg
      --password=<neo4jPassword>
                             Neo4j password (default: password
      --port=<port>          Set the port of the neo4j Database (default: 7687).
      --print-benchmark      Print benchmark result as markdown table
  -S, --softwareComponents=<String=String>
                             Maps the names of software components to their
                               respective files. The files are separated by
                               commas (No whitespace!).
                             Example: -S App1=./file1.c,./file2.c -S App2=.
                               /Main.java,./Class.java
      --save-depth=<depth>   Performance optimisation: Limit recursion depth
                               form neo4j OGM when leaving the AST. -1
                               (default) means no limit is used.
      --top-level=<topLevel> Set top level directory of project structure.
                               Default: Largest common path of all source files
      --use-unity-build      Enable unity build mode for C++ (requires
                               --load-includes)
      --user=<neo4jUsername> Neo4j user name (default: neo4j)
```
You can provide a list of paths of arbitrary length that can contain both file paths and directory paths.

## Json export

It is possible to export the cpg as json file with the `--export-json` option.
The graph is serialized as list of nodes and edges:
```json
{
   "nodes": [...],
   "edges": [...]
}
```
Documentation about the graph schema can be found at:
[https://fraunhofer-aisec.github.io/cpg/CPG/specs/graph](https://fraunhofer-aisec.github.io/cpg/CPG/specs/graph)

Usage example:
```
$ build/install/cpg-neo4j/bin/cpg-neo4j --export-json cpg-export.json --no-neo4j src/test/resources/client.cpp
```

To export the cpg from a neo4j database, you can use the neo4j `apoc` plugin.
There it's also possible to export only parts of the graph.

## Known issues:

- While importing sufficiently large projects with the parameter <code>--save-depth=-1</code> 
        a <code>java.lang.StackOverflowError</code> may occur.
    - This error could be solved by increasing the stack size with the JavaVM option: <code>-Xss4m</code>
    - Otherwise the depth must be limited (e.g. 3 or 5)

- While pushing a constant value larger than 2^63 - 1 a <code>java.lang.IllegalArgumentException</code> occurs.
