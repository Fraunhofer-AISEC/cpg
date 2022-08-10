# Code Property Graph

Ensuring the correct behavior of software is crucial to avoid security issues stemming from incorrect implementations. In this video, we present our CPG tool, a language-independent analysis platform for source code based on an adaption of a code property graph. Our platform has support for multiple passes that can extend the analysis after the graph is constructed and it currently supports C/C++, Java and has experimental support for Golang and Python.

The whole process is also documented in a short video.

[![CPG video](https://img.youtube.com/vi/ngKtH6kYxx0/0.jpg)](https://www.youtube.com/watch?v=ngKtH6kYxx0)

## What is a Code Property Graph?

A code property graph (CPG) is a representation of source code in form of a labelled directed multigraph. Think of it as a graph where each node and edge is assigned a set of key-value pairs, called properties. This representation is supported by a range of graph databases and can be used to store source code of a program in a searchable data structure. Thus, the code property graph allows to use existing graph query languages in order to either manually navigate through interesting parts of the source code or to automatically find "interesting" patterns.

## CPG Console

While the CPG tool is mostly used as a libary in external tools, such as [Codyze](http://github.com/Fraunhofer-AISEC/codyze), we decided to showcase its functionalities with a simple CLI based console that can be used to query the graph and run simple analysis steps.

To launch the console, first build it according to the instructions in our `README.md` and then run `bin/cpg-console`. You will be greeted by the interactive prompt of our console, which is implemented by the kotlin `ki` interactive shell. The commands on this shell follow the syntax of the Kotlin language. For more information please see the [Kotlin documentation](https://kotlinlang.org/docs/home.html). 

In addition to that, commands prefixed by a `:` are plugin commands. To get a list of all available plugins use the `:h` or `:help` command.

```
ki-shell 0.3/1.4.32
type :h for help
[0] :h
:quit or q                           quit the shell
:load or l <path>                    load file and evaluate
:type or t <expr>                    display the type of an expression without evaluating it
:list or ls                          list defined symbols
:help or h [command]                 print this summary or command-specific help
```

### Launching the translation plugin

One such a plugin is the `:translate` command, or `:tr` for short. It allows the translation of source code files into the code property graph. In this example, we will use all files in `src/test/resources` and translate them.

```kotlin
[1] :tr src/test/resources
18:29:01,138 INFO  TranslationManager Parsing src/test/resources/array.go
18:29:01,151 INFO  TranslationManager Parsing src/test/resources/nullptr.cpp
18:29:01,183 ERROR TranslationManager Different frontends are used for multiple files. This will very likely break the following passes.
18:29:01,284 INFO  CXXLanguageFrontend Parsed 114 bytes corresponding roughly to 2 LoC
18:29:01,284 INFO  Benchmark CXXLanguageFrontend Parsing sourcefile done in 98 ms
18:29:01,307 INFO  Benchmark CXXLanguageFrontend Transform to CPG done in 22 ms
18:29:01,307 INFO  TranslationManager Parsing src/test/resources/array.cpp
18:29:01,308 ERROR TranslationManager Different frontends are used for multiple files. This will very likely break the following passes.
18:29:01,310 INFO  CXXLanguageFrontend Parsed 174 bytes corresponding roughly to 3 LoC
18:29:01,310 INFO  Benchmark CXXLanguageFrontend Parsing sourcefile done in 2 ms
18:29:01,326 INFO  Benchmark CXXLanguageFrontend Transform to CPG done in 16 ms
18:29:01,327 INFO  TranslationManager Parsing src/test/resources/Array.java
18:29:01,347 INFO  JavaLanguageFrontend Source file root used for type solver: src/test/resources
18:29:01,373 ERROR TranslationManager Different frontends are used for multiple files. This will very likely break the following passes.
18:29:01,373 ERROR TranslationManager Different frontends are used for multiple files. This will very likely break the following passes.
18:29:01,373 ERROR TranslationManager Different frontends are used for multiple files. This will very likely break the following passes.
18:29:01,421 INFO  Benchmark JavaLanguageFrontend Parsing source file done in 47 ms
18:29:01,472 INFO  Benchmark JavaLanguageFrontend Transform to CPG done in 50 ms
18:29:01,472 INFO  Benchmark TranslationManager Frontend done in 338 ms
18:29:01,486 INFO  Benchmark TypeHierarchyResolver Executing Pass done in 13 ms
18:29:01,488 INFO  Benchmark JavaExternalTypeHierarchyResolver Executing Pass done in 1 ms
18:29:01,490 INFO  Benchmark ImportResolver Executing Pass done in 1 ms
18:29:01,508 WARN  Util src/test/resources/struct.go:6:6: Did not find a declaration for nil
18:29:01,509 WARN  Util src/test/resources/nullptr.cpp:5:9: Did not find a declaration for null
18:29:01,513 INFO  Benchmark VariableUsageResolver Executing Pass done in 22 ms
18:29:01,521 INFO  Benchmark CallResolver Executing Pass done in 7 ms
18:29:01,525 INFO  Benchmark EvaluationOrderGraphPass Executing Pass done in 3 ms
18:29:01,527 INFO  Benchmark TypeResolver Executing Pass done in 2 ms
18:29:01,531 INFO  Benchmark ControlFlowSensitiveDFGPass Executing Pass done in 4 ms
18:29:01,533 INFO  Benchmark FilenameMapper Executing Pass done in 1 ms
18:29:01,533 INFO  Benchmark TranslationManager Translation into full graph done in 399 ms
```

After the translation is done, several symbols are available on the console, to query and analyse the translation result. You can use the `:ls` command to get a quick overview. 

```kotlin
[2] :ls
val config: de.fraunhofer.aisec.cpg.TranslationConfiguration!
val analyzer: de.fraunhofer.aisec.cpg.TranslationManager!
val result: de.fraunhofer.aisec.cpg.TranslationResult!
val tu: de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration!
```

Most interesting for the user is the `result` object which holds the complete translation result, as well as the `tu` object, which is a shortcut to the first translation unit, i.e. the first file that was translated. It is of type `TranslationUnitDeclaration`, which is one of our node types in the graph; the most basic one being a `Node` itself.

### Querying the translation result

In the following, we will use the aforementioned objects to query the source code for interesting patterns. To do so, we will explore several built-in functions that can be used in exploring the graph. The first of these, is the `all` function, it returns a list of all nodes that are direct descendents of a particular node, basicically flattening the hierarchy.

```kotlin
[3] result.all()
res3: List<de.fraunhofer.aisec.cpg.graph.Node> = ...
```

The output here can be quite verbose, so additional filtering is needed. The `all` function takes an additional type parameter, which can be used to further filter nodes of a particular type. In this case, we are interested in all `ArraySubscriptionExpression` nodes, i.e. those that represent access to an element of an array. These operations are often prone to out of bounds errors and we want to explore, whether our code is also affected by that.

```kotlin
[4] result.all<ArraySubscriptionExpression>()
res4: List<de.fraunhofer.aisec.cpg.graph.statements.expressions.ArraySubscriptionExpression> = [
    {"@type":"ArraySubscriptionExpression","location":"array.go(6:2-6:7)","type":{"@type":"ObjectType","name":"int"}},
    {"@type":"ArraySubscriptionExpression","location":"array.cpp(6:12-6:18)","type":{"@type":"ObjectType","name":"char"}},
    {"@type":"ArraySubscriptionExpression","location":"Array.java(8:18-8:22)","type":{"@type":"ObjectType","name":"char"}}, 
    {"@type":"ArraySubscriptionExpression","location":"array.cpp(12:12-12:16)","type":{"@type":"ObjectType","name":"char"}}
]
```

Much better. We have found four nodes that represent an array access. To see the corresponding source code of our result, we can prefix our previous command with `:code` or `:c`. This shows the raw source code as well as the location of the file where the code is located.

```kotlin
[5] :code result.all<ArraySubscriptionExpression>()
--- src/test/resources/array.go:6:2 ---
  6: a[11]
------------------------------------------------

--- array.cpp:6:12 ---
  6: = c[b]
-----------------------------------------------------------------------------------------------

--- Array.java:8:18 ---
  8: c[b]
------------------------------------------------------------------------------------------------

--- array.cpp:12:12 ---
 12: c[0]
------------------------------------------------------------------------------------------------
```

This also demonstrates quite nicely, that queries on the CPG work independently of the programming language. Our test folder contains Java, Go and C++ files and we can analyse all of them simultaneously.

### Looking for software errors

In a next step, we want to identify, which of those expression are accessing an array index that is greater than its capacity, thus leading to an error. From the code output we have seen before we can already identify two array indicies: `0` and `11`. But the other two are using a variable `b` as the index. Using the `evaluate` function, we can try to evaluate the variable `b`, to check if it has a constant value.

```kotlin
[6] result.all<ArraySubscriptionExpression>().map { it.subscriptExpression.evaluate() }
res6: List<Any?> = [11, 5, 5, 0]
```

In this case we are in luck and we see that, next to the `0` and `11` we already know, the other two expression were evaluated to `5`.

In a next step, we want to check to capacity of the array the access is referring to. We can make use of two helper functions `dfgFrom` and `capacity` to quickly check this, using the built-in data flow analysis.

```kotlin
[7] var expr = result.all<ArraySubscriptionExpression>().map { Triple(
        it.subscriptExpression.evaluate() as Int,
        it.arrayExpression.dfgFrom<ArrayCreationExpression>().first().capacity,
        it
    ) }
[8]: expr    
res8: List<Triple<Int, Int, de.fraunhofer.aisec.cpg.graph.statements.expressions.ArraySubscriptionExpression>> = [
    (11, 10, {"@type":"ArraySubscriptionExpression","location":"array.go(6:2-6:7)","type":{"@type":"ObjectType","name":"int"}}),
    (5, 4, {"@type":"ArraySubscriptionExpression","location":"array.cpp(6:12-6:18)","type":{"@type":"ObjectType","name":"char"}}),
    (5, 4, {"@type":"ArraySubscriptionExpression","location":"Array.java(8:18-8:22)","type":{"@type":"ObjectType","name":"char"}}),
    (0, 100, {"@type":"ArraySubscriptionExpression","location":"array.cpp(12:12-12:16)","type":{"@type":"ObjectType","name":"char"}})
]
```

This gives us a triple of the array index, the array capacity and a reference to the node in the graph.

Lastly, we can make use of the `filter` function to return only those nodes where the evaluated index is greater or equal to the capacity, leading to an out of bounds error, and a possible program crash.

```kotlin
[9] expr.filter { it.first >= it.second }
res8: List<Triple<Int, Int, de.fraunhofer.aisec.cpg.graph.statements.expressions.ArraySubscriptionExpression>> = [
    (11, 10, {"@type":"ArraySubscriptionExpression","location":"array.go(6:2-6:7)","type":{"@type":"ObjectType","name":"int"}}), 
    (5, 4, {"@type":"ArraySubscriptionExpression","location":"array.cpp(6:12-6:18)","type":{"@type":"ObjectType","name":"char"}}), 
    (5, 4, {"@type":"ArraySubscriptionExpression","location":"Array.java(8:18-8:22)","type":{"@type":"ObjectType","name":"char"}})
]
```

Using the already known `:code` command, we can also show the relevant code locations.

```kotlin
[10] :code expr.filter { it.first >= it.second }.map { it.third }
--- src/test/resources/array.go:6:2 ---
  6: a[11]
------------------------------------------------

--- src/test/resources/array.cpp:6:12 ---
  6: = c[b]
-----------------------------------------------------------------------------------------------

--- src/test/resources/Array.java:8:18 ---
  8: c[b]
------------------------------------------------------------------------------------------------
```

### Futher analysis

Because the manual analyis we have shown can be quite tedious, we already included several example analyis steps that can be performed on the currently loaded graph. They can be executed by running the `:run` command. This includes the aforementioned check for out of bounds as well as check for null pointers and will be extended in the future.

```kotlin
[11] :run

--- FINDING: Out of bounds access in ArrayCreationExpression when accessing index 11 of a, an array of length 10 ---
src/test/resources/array.go:6:2: a[11]

The following path was discovered that leads to 11 being 11:
src/test/resources/array.go:6:4: 11

--- FINDING: Out of bounds access in ArrayCreationExpression when accessing index 5 of c, an array of length 4 ---
src/test/resources/array.cpp:6:12: = c[b]

The following path was discovered that leads to b being 5:
src/test/resources/array.cpp:6:16: b
src/test/resources/array.cpp:4:5: int b = a + 1;
src/test/resources/array.cpp:4:11: = a + 1
src/test/resources/array.cpp:4:13: a
src/test/resources/array.cpp:3:5: int a = 4;
src/test/resources/array.cpp:3:11: = 4
src/test/resources/array.cpp:4:17: 1
```

Lastly, it is also possible to export the complete graph structure to a graph database, such as Neo4J with a simple `:export` command.

```kotlin
[12] :export neo4j
19:26:41,642 INFO  Application Using import depth: -1
19:26:41,643 INFO  Application Count base nodes to save: 4
Jun 08, 2021 7:26:41 PM org.neo4j.driver.internal.logging.JULogger info
INFO: Direct driver instance 1156771703 created for server address localhost:7687
19:26:42,006 INFO  DomainInfo Starting Post-processing phase
19:26:42,006 INFO  DomainInfo Building byLabel lookup maps
19:26:42,006 INFO  DomainInfo Building interface class map for 106 classes
19:26:42,027 INFO  DomainInfo Post-processing complete
19:26:44,471 INFO  BoltDriver Shutting down Bolt driver org.neo4j.driver.internal.InternalDriver@44f2ef77
Jun 08, 2021 7:26:44 PM org.neo4j.driver.internal.logging.JULogger info
INFO: Closing driver instance 1156771703
Jun 08, 2021 7:26:44 PM org.neo4j.driver.internal.logging.JULogger info
INFO: Closing connection pool towards localhost:7687
```

Then, additional tools, such as the Neo4j browser can be used to further explore the graph.

(TODO: screenshot of neo4j browser)

## Conclusion

In conclusion, the CPG tool can be used to translate source code of different programming languages to a uniform, language-independed represetation in the form of a code property graph. It can either be used as a library, in which it forms the underlying basis of the [Codyze](http://github.com/Fraunhofer-AISEC/codyze) analyizer or it's console can be used to quickly explore source code and find weaknesses.

It is available as open source on GitHub: https://github.com/Fraunhofer-AISEC/cpg
