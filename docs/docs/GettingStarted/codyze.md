---
title: "Using the Codyze CLI"
linkTitle: "Using the Codyze CLI"
no_list: true
weight: 2
date: 2025-03-17
description: >
  Using the Codyze CLI
---

# Codyze - The swiss army knife for the code property graph

Codyze is a command line tool that allows you to analyze source code using the Code Property Graph (CPG). It is a powerful tool for static analysis, vulnerability detection, and code exploration.

## Build

Build (and install) a distribution using Gradle

```
./gradlew :codyze:installDist
```

Please remember to adjust the `gradle.properties` before building the project.

## Usage

You can then use `codyze` from the command line by running the installed `./codyze/build/install/codyze/bin/codyze` binary.

```
Usage: codyze [<options>] <command> [<args>]...

Options:
  -h, --help  Show this message and exit

Commands:
  console
  compliance  
```

### Console Command

The `console` command is used to start an interactive console for exploring the CPG. It allows you to explore the code and see the CPG in a more visual way. In the future we will offer a way to run interactive queries in the console.

<img src="/cpg/assets/img/codyze-analysis-result.png"  alt="Codyze Console - Analysis Result View"/>
<img src="/cpg/assets/img/codyze-component.png"  alt="Codyze Console - Component View"/>
<img src="/cpg/assets/img/codyze-ast-nodes-overlay-tooltip.png"  alt="Codyze Console - AST Nodes Overlay"/>
<img src="/cpg/assets/img/codyze-ast-nodes-table.png"  alt="Codyze Console - AST Nodes Table"/>


### Compliance Command

The `compliance` command is used to check compliance with various coding standards and guidelines. It can be used to analyze code for potential vulnerabilities and coding issues. We are currently working on adding more compliance checks and this documentation will be updated accordingly.

The `compliance` command has several subcommands:
```
Usage: codyze compliance [<options>] <command> [<args>]...

Options:
  -h, --help  Show this message and exit

Commands:
  scan
  list-security-goals
```

With the most important being the `scan` command, which is used to scan a project for compliance with the specified security goals.

```
Usage: codyze compliance scan [<options>]

Project Options:
  --project-dir=<path>  The project directory
  --console=true|false  Starts the Codyze web console after the analysis

CPG Translation Options:
  --sources=<path>             A list of source files. They will be all added to a single component 'app'.
  --components=<text>          The components to analyze. They must be located inside the 'components' folder inside the project directory. The 'components' folder will be taken as the topLevel property for the
                               translation configuration.
  --exclusion-patterns=<text>  A pattern of files to exclude

Options:
  -h, --help  Show this message and exit
```
