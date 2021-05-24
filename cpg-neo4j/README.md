# Neo4J visualisation tool for the Code Property Graph 

A simple tool to export a *code property graph* to a neo4j database.

## Requirements

The application requires Java 11 or higher.

## Build

Build using Gradle

```
./gradlew installDist
```

## Usage

```
./build/install/cpg-neo4j/bin/cpg-neo4j [--host=<host>] [--port=<port>]
                                        [--user=<neo4jUsername>] [--password=<neo4jPassword>]
                                        [--includes-file=<includesFile>] [--save-depth=<depth>] 
                                        <files>...

      <files>...             The paths to analyze. If module support is
                               enabled, the paths will be looked at if they
                               contain modules
      --host=<host>          Set the host of the neo4j Database (default:
                               localhost).
      --load-includes        Enable TranslationConfiguration option loadIncludes
      --includes-file=<includesFile>
                             Load includes from file
      --password=<neo4jPassword>
                             Neo4j password (default: password
      --port=<port>          Set the port of the neo4j Database (default: 7687).
      --save-depth=<depth>   Performance optimisation: Limit recursion depth
                               form neo4j OGM when leaving the AST. -1
                               (default) means no limit is used.
      --user=<neo4jUsername> Neo4j user name (default: neo4j)
```
You can provide a list of paths of arbitrary length that can contain both file paths and directory paths.

## Known issues:

- While importing sufficiently large projects with the parameter <code>--save-depth=-1</code> 
        a <code>java.lang.StackOverflowError</code> may occur.
    - This error could be solved by increasing the stack size with the JavaVM option: <code>-Xss4m</code>

- While pushing a constant value larger than 2^63 - 1 a <code>java.lang.IllegalArgumentException</code> occurs.
