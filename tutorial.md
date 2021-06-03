# CPG Console

While the CPG is mostly used as a libary in external tools, we decided to showcase its functionalities with a simple CLI based console that can be used to query the graph and run simple analysis steps.

To launch the console, built it and then run `bin/cpg-console`. You will be greeted by the interactive prompt of our CPG console, which is implemented by the kotlin `ki` interactive shell. The commands on this shell follow the Kotlin language. For more information please see the [Kotlin documentation](https://kotlinlang.org/docs/home.html).

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

## Launching the translation plugin

One such a plugin is the `:translate` command, or `:tr` for short. It allows the translation of source code files into the code property graph. In this example, we will use all files in `src/test/resources` and translate them.

```log
[1] :tr src/test/resources
10:15:27,086 INFO  TranslationManager Parsing /Users/cpg/Repositories/cpg/cpg-console/src/test/resources/array.cpp
10:15:27,231 INFO  CXXLanguageFrontend Parsed 96 bytes corresponding roughly to 1 LoC
10:15:27,231 INFO  Benchmark CXXLanguageFrontend Parsing sourcefile done in 94 ms
10:15:27,273 INFO  Benchmark CXXLanguageFrontend Transform to CPG done in 41 ms
10:15:27,273 INFO  TranslationManager Parsing /Users/cpg/Repositories/cpg/cpg-console/src/test/resources/Array.java
10:15:27,312 INFO  JavaLanguageFrontend Source file root used for type solver: /Users/cpg/Repositories/cpg/cpg-console/src/test/resources
10:15:27,343 ERROR TranslationManager Different frontends are used for multiple files. This will very likely break the following passes.
10:15:27,395 INFO  Benchmark JavaLanguageFrontend Parsing source file done in 51 ms
10:15:27,452 INFO  Benchmark JavaLanguageFrontend Transform to CPG done in 57 ms
10:15:27,452 INFO  Benchmark TranslationManager Frontend done in 373 ms
10:15:27,465 INFO  Benchmark TypeHierarchyResolver Executing Pass done in 12 ms
10:15:27,467 INFO  Benchmark JavaExternalTypeHierarchyResolver Executing Pass done in 2 ms
10:15:27,470 INFO  Benchmark ImportResolver Executing Pass done in 2 ms
10:15:27,482 INFO  Benchmark VariableUsageResolver Executing Pass done in 11 ms
10:15:27,490 INFO  Benchmark CallResolver Executing Pass done in 8 ms
10:15:27,493 INFO  Benchmark EvaluationOrderGraphPass Executing Pass done in 2 ms
10:15:27,495 INFO  Benchmark TypeResolver Executing Pass done in 1 ms
10:15:27,498 INFO  Benchmark ControlFlowSensitiveDFGPass Executing Pass done in 2 ms
10:15:27,499 INFO  Benchmark FilenameMapper Executing Pass done in 1 ms
10:15:27,499 INFO  Benchmark TranslationManager Translation into full graph done in 420 ms
```

After the translation is done, several symbols are available on the console, to query and analyse the result. You can use the `:ls` command to get a quick overview. 

```kotlin
[2] :ls
val config: de.fraunhofer.aisec.cpg.TranslationConfiguration!
val analyzer: de.fraunhofer.aisec.cpg.TranslationManager!
val result: de.fraunhofer.aisec.cpg.TranslationResult!
val tu: de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration!
```

Most interesting for the user are the `result` object which holds the complete translation result, as well as the `tu` object, which is a shortcut to the first translation unit, i.e. the first file that was translated. It is of type `TranslationUnitDeclaration`, which is one of our node type in the graph; the most basic one being a `Node` itself.

## Querying the translation result

In the following, we will use the aforementioned objects to query the source code for interesting patterns. To do so, we will explore several built-in functions that can be used in exploring the graph. The first of these, is the `all` function, it returns a list of all nodes that are direct descendents of a particular node, basicically flattening the hierarchy.

```kotlin
[3] result.all()
res3: List<de.fraunhofer.aisec.cpg.graph.Node> = ...
```

The output here can be quite huge, so additional filtering is needed. The `all` function takes an additional type parameter, which can be used to further filter nodes of a particular type. In this case, we are interested in all `ArraySubscriptionExpression` nodes, i.e. those that represent access to an element of an array. These operations are often prone to out of bounds errors and we want to explore, whether our code is also affected by that.

```kotlin
[4] result.all<ArraySubscriptionExpression>()
res4: List<de.fraunhofer.aisec.cpg.graph.statements.expressions.ArraySubscriptionExpression> = [
    {"location":"array.cpp(6:12-6:18)","type":{"name":"char"}}, 
    {"location":"Array.java(8:18-8:22)","type":{"name":"char"}}
]
```

Much better. We have found two nodes that represent an array access. To see the corresponding source code of our result, we can prefix our previous command with `:code` or `:c`. This shows the raw source code as well as the location of the file where the code is located.

```kotlin
[5] :code result.all<ArraySubscriptionExpression>()
--- src/test/resources/array.cpp:6:12 ---
= c[b]
-----------------------------------------------------------------------------------------------

--- src/test/resources/Array.java:8:18 ---
c[b]
------------------------------------------------------------------------------------------------

res5: Collection<de.fraunhofer.aisec.cpg.graph.Node> = [{"location":"array.cpp(6:12-6:18)","type":{"name":"char"},"possibleSubTypes":["UNKNOWN",{"name":"char"}]}, {"location":"Array.java(8:18-8:22)","type":{"name":"char"},"possibleSubTypes":["UNKNOWN",{"name":"char"}]}]
```

This also demonstrates quite nicely, that queries on the CPG work independently of the programming language. Our test folder contains both Java as well as C++ files and we can analyse them simultaneously.

```kotlin
[24] result.all<ArraySubscriptionExpression>().map { it.subscriptExpression.resolve() }
res21: List<Any?> = [5, 5]
```

TODO: make it simpler to query prev DFG with type
```kotlin
[30] result.all<ArraySubscriptionExpression>().map { 
    Triple(
        it.subscriptExpression.resolve() as Int,
        (it.arrayExpression.prevDFG.first() as? ArrayCreationExpression)?.dimensions?.first()?.resolve() as Int,
        it
    ) }
res29: List<Triple<Int, Int, de.fraunhofer.aisec.cpg.graph.statements.expressions.ArraySubscriptionExpression>> = [(5, 4, {"location":"array.cpp(6:12-6:18)","type":{"name":"char"},"possibleSubTypes":["UNKNOWN",{"name":"char"}]}), (5, 4, {"location":"Array.java(8:18-8:22)","type":{"name":"char"},"possibleSubTypes":["UNKNOWN",{"name":"char"}]})]
```

We use that result to filter those where the resolved index is greater or equal to our dimension.

```kotlin
[31] res39.filter { it.first >= it.second }
res41: List<Triple<Int, Int, de.fraunhofer.aisec.cpg.graph.statements.expressions.ArraySubscriptionExpression>> = [(5, 4, {"location":"array.cpp(6:12-6:18)","type":{"name":"char"},"possibleSubTypes":["UNKNOWN",{"name":"char"}]}), (5, 4, {"location":"Array.java(8:18-8:22)","type":{"name":"char"},"possibleSubTypes":["UNKNOWN",{"name":"char"}]})]
```

```kotlin
[31] :code res39.filter { it.first >= it.second }.map { it.third }
--- /Users/chr55316/Repositories/cpg/cpg-console/src/test/resources/array.cpp:6:12 ---
= c[b]
-----------------------------------------------------------------------------------------------

--- /Users/chr55316/Repositories/cpg/cpg-console/src/test/resources/Array.java:8:18 ---
c[b]
------------------------------------------------------------------------------------------------
```
Of course the same can also be achieved in one, slightly larger query.

```kotlin
[6] result.all<ArraySubscriptionExpression>().filter {
    (it.subscriptExpression.resolve() as Int) >= 
    ((it.arrayExpression.prevDFG.first() as? ArrayCreationExpression)?.dimensions?.first()?.resolve() as Int)
}
```
