---
title: "Using the Interactive CLI"
linkTitle: "Using the Interactive CLI"
no_list: true
weight: 2
date: 2020-01-30
description: >
  Using the Interactive CLI (cpg-console)
---

# The Interactive CLI

If you want to explore the graph from the command line, we provide an interactive interface for this.

To build the interface from source, simply type `./gradlew
:cpg-console:installDist` from the root of the repository. You can then start
the CLI with `cpg-console/build/install/cpg-console/bin/cpg-console`.

The CLI comes with only few basic commands:
* `:tr <path>` or `:translate <path>` translates the file(s) under the given
  path.
* `:c <node>` or `:code <node>` prints the code of the given node
* `:e neo4j <username> <password>` exports the graph to neo4j
* `:translateCompilationDatabase <path>` or `:trdb <path>` translates the source
  code files using the provided compilation database into the CPG
* `:r` or `:run` runs two pre-defined analyzers: a null pointer check or an
  out-of-bounds check
* `:help` prints a help text with available commands
* `:q` quits the session

After translating a file/project, the translation result will be kept in
`result`. You can now explore all edges and nodes in the graph as in any kotlin
project. You can also use the [shortcuts](shortcuts.md) or the analyses provided
by the [Query API](query.md).

Example:
```kotlin
[0] :tr  cpg-analysis/src/test/resources/value_evaluation/size.java
  ...
[10] val mainFun = result.functions["main"]
[20] :code mainFun!!
  2: public static void main(String[] args) {
  3:         int[] array = new int[3];
  4:         for(int i = 0; i < array.length; i++) {
  5:             array[i] = i;
  6:         }
  7:         System.out.println(array[1]);
  8:
  9:         String str = "abcde";
  10:         System.out.println(str);
  11:         return 0;
  12:     }

[21] sizeof(mainFun.calls["println"]!!.arguments[0]).value
res21: Int = 3
[22] :q

Bye!
```
