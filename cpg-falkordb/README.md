# FalkorDB persistence tool for the Code Property Graph

A simple tool to export a *code property graph* to a [FalkorDB](https://www.falkordb.com/) graph database.

FalkorDB is a Redis module that implements a sparse-matrix based property graph and speaks
openCypher. Compared to the [cpg-neo4j](../cpg-neo4j) tool it needs no server-side plugin, starts in
well under a second and keeps the whole graph in memory, which makes it a good fit for short-lived
analysis runs and CI pipelines.

## Requirements

The application requires Java 21.

No server-side plugin is required. For example using docker:

```
docker run -p 127.0.0.1:6379:6379 -d falkordb/falkordb:latest
```

FalkorDB ships a graph browser on port 3000 if you expose it (`-p 127.0.0.1:3000:3000`), which can be
used to visually explore the persisted CPG.

## Build

Build (and install) a distribution using Gradle

```
../gradlew installDist
```

Please remember to adjust the `gradle.properties` before building the project, in order to enable the
language frontends you are interested in.

## Usage

```
./build/install/cpg-falkordb/bin/cpg-falkordb [--infer-nodes] [--load-includes]
                    [--no-default-passes] [--no-falkordb] [--no-purge-db]
                    [--print-benchmark] [--use-unity-build]
                    [--benchmark-json=<benchmarkJson>]
                    [--custom-pass-list=<customPasses>]
                    [--export-json=<exportJsonFile>] [--graph=<graphName>]
                    [--host=<host>] [--includes-file=<includesFile>]
                    [--max-complexity-cf-dfg=<maxComplexity>]
                    [--password=<falkorDbPassword>] [--port=<port>]
                    [--top-level=<topLevel>] [--user=<falkorDbUsername>]
                    [--exclusion-patterns=<exclusionPatterns>]...
                    [-IP=<includePaths>]... ([<files>...] | -S=<String=String>
                    [-S=<String=String>]... |
                    --json-compilation-database=<jsonCompilationDatabase> |
                    --list-passes)
      [<files>...]          The paths to analyze. If module support is enabled,
                              the paths will be looked at if they contain
                              modules
      --benchmark-json=<benchmarkJson>
                            Save benchmark results to json file
      --custom-pass-list=<customPasses>
                            Add custom list of passes (might be used additional
                              to --no-default-passes) which is passed as a
                              comma-separated list; give either pass name if
                              pass is in list, or its FQDN (e.g.
                              --custom-pass-list=DFGPass,CallResolver)
      --exclusion-patterns=<exclusionPatterns>
                            Configures an exclusion pattern for files or
                              directories that should not be parsed
      --export-json=<exportJsonFile>
                            Export cpg as json
      --graph=<graphName>   Set the name of the graph to store the cpg in
                              (default: cpg).
                            A single FalkorDB instance can hold several
                              independent graphs.
      --host=<host>         Set the host of the FalkorDB instance (default:
                              localhost).
      --includes-file=<includesFile>
                            Load includes from file
      --infer-nodes         Create inferred nodes for missing declarations
      -IP, --include-paths=<includePaths>
                            Directories containing additional headers and
                              implementations for imported code.
      --json-compilation-database=<jsonCompilationDatabase>
                            The path to an optional a JSON compilation
                              database. Please note, that the JSON compilation
                              database always describes a single component.
      --list-passes         Prints the list available passes
      --load-includes       Enable TranslationConfig option
      --max-complexity-cf-dfg=<maxComplexity>
                            Performance optimisation: Limit the
                              ControlFlowSensitiveDFGPass to functions with a
                              complexity less than what is specified here. -1
                              (default) means no limit is used.
      --no-default-passes   Do not register default passes [used for debugging]
      --no-falkordb         Do not push cpg into FalkorDB [used for debugging]
      --no-purge-db         Do not purge the graph before pushing the cpg
      --password=<falkorDbPassword>
                            FalkorDB password (default: no authentication)
      --port=<port>         Set the port of the FalkorDB instance (default:
                              6379).
      --print-benchmark     Print benchmark result as markdown table
  -S, --softwareComponents=<String=String>
                            Maps the names of software components to their
                              respective files. The files are separated by
                              commas (No whitespace!).
                            Example: -S App1=./file1.c,./file2.c -S App2=./Main.
                              java,./Class.java
      --top-level=<topLevel>
                            Set top level directory of project structure.
                              Default: Largest common path of all source files
      --use-unity-build     Enable unity build mode for C++ (requires
                              --load-includes)
      --user=<falkorDbUsername>
                            FalkorDB user name (default: no authentication)
```

You can provide a list of paths of arbitrary length that can contain both file paths and directory
paths.

Usage example:

```
$ ./build/install/cpg-falkordb/bin/cpg-falkordb --graph my-project src/main.c
```

Afterwards the graph can be queried with any Redis client, e.g.:

```
$ redis-cli GRAPH.QUERY my-project "MATCH (f:Function)-[:BODY]->(b) RETURN f.name, b.code"
```

## Graph schema

The persisted schema is identical to the one used by the [cpg-neo4j](../cpg-neo4j) tool: every node
carries the labels of its whole class hierarchy (e.g. a call expression is labelled
`:Node:AstNode:Expression:Call`) and every edge is persisted with its edge properties.

Documentation about the graph schema can be found at:
[https://fraunhofer-aisec.github.io/cpg/CPG/specs/graph](https://fraunhofer-aisec.github.io/cpg/CPG/specs/graph)

## Json export

It is possible to export the cpg as json file with the `--export-json` option. The graph is
serialized as list of nodes and edges:

```json
{
   "nodes": [...],
   "edges": [...]
}
```

Usage example:

```
$ ./build/install/cpg-falkordb/bin/cpg-falkordb --export-json cpg-export.json --no-falkordb src/main.c
```

## Tests

The unit tests do not need a running database:

```
../gradlew :cpg-falkordb:test
```

The integration tests require a FalkorDB instance. Its location can be configured with the
`FALKORDB_HOST`, `FALKORDB_PORT` and `FALKORDB_GRAPH` environment variables:

```
../gradlew :cpg-falkordb:integrationTest
```
