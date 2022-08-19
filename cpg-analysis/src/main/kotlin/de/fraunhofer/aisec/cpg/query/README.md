# The Query API

The Query API provides a way validate if nodes in the graph fulfil certain requirements. It is a mixture of typical logical expressions (e.g. and, or, xor, implies), quantors (e.g. forall, exists), comparisons (e.g. <, >, ==, !=), some special operations (e.g., `in` to check for collections or `is` for types) and a couple of operations.

## Operation modes
The Query API has two modes of operations which determine the depth of the output:
1. The detailed mode reasons about every single step performed to check if the query is fulfilled.
2. The less detailed mode only provides the final output (true, false) and the nodes which serve as input.

To use the detailed mode, it is necessary to use specific operators in a textual representation whereas the other modes relies on the operators as known from any programming language.

The following example output from the test case `testMemcpyTooLargeQuery2` shows the difference:

**Less detailed:**
```
[CallExpression[name=memcpy,location=vulnerable.cpp(3:5-3:38),type=UNKNOWN,base=<null>]]
```
**Detailed mode:**
```
all (==> false)
--------
    Starting at CallExpression[name=memcpy,location=vulnerable.cpp(3:5-3:38),type=UNKNOWN,base=<null>]: 5 > 11 (==> false)
------------------------
        sizeof(DeclaredReferenceExpression[DeclaredReferenceExpression[name=array,location=vulnerable.cpp(3:12-3:17),type=PointerType[name=char[]]],refersTo=VariableDeclaration[name=array,location=vulnerable.cpp(2:10-2:28),initializer=Literal[location=vulnerable.cpp(2:21-2:28),type=PointerType[name=char[]],value=hello]]]) (==> 5)
----------------------------------------
------------------------
        sizeof(Literal[location=vulnerable.cpp(3:19-3:32),type=PointerType[name=char[]],value=Hello world]) (==> 11)
----------------------------------------
------------------------
--------
```

## Operators of the detailed mode
Numerous methods allow to evaluate the queries while keeping track of all the steps. Currently, the following operations are supported:
- **eq**: Equality of two values.
- **ne**: Inequality of two values.
- **IN**: Checks if a value is contained in a [Collection]
- **IS**: Checks if a value implements a type ([Class]).

Additionally, some functions are available only for certain types of values.

For boolean values:
- **and**: Logical and operation (&&)
- **or**: Logical or operation (||)
- **xor**: Logical exclusive or operation (xor)
- **implies**: Logical implication

For numeric values:
- **gt**: Grater than (>)
- **ge**: Grater than or equal (>=)
- **lt**: Less than (<)
- **le**: Less than or equal (<=)

**Note:** The detailed mode and its operators require the user to take care of the correct order. I.e., the user has to put the brackets!

## Operators of the less detailed mode
Numerous methods allow to evaluate the queries:
- **==**: Equality of two values.
- **!=**: Inequality of two values.
- **in** : Checks if a value is contained in a [Collection]. The value of a query tree has to be accessed by the property `value`.
- **is**: Checks if a value implements a type ([Class]). The value of a query tree has to be accessed by the property `value`.
- **&&**: Logical and operation
- **||**: Logical or operation
- **xor**: Logical exclusive or operation
- **>**: Grater than
- **>=**: Grater than or equal
- **<**: Less than
- **<=**: Less than or equal

## Functions of the Query API
Since these operators cannot cover all interesting values, we provide an initial set of analyses and functions to use them. These are:
- **min(n: Node)**: Minimal value of a node
- **max(n: Node)**: Maximal value of a node
- **sizeof(n: Node)**: The length of an array or string
- **dataFlow(from: Node, to: Node)**: Checks if a data flow is possible between the nodes `from` as a source and `to` as sink.
- **executionPath(from: Node, to: Node)**: Checks if a path of execution flow is possible between the nodes `from` and `to`.
- **executionPath(from: Node, predicate: (Node) -> Boolean)**: Checks if a path of execution flow is possible starting at node `from` and fulfilling the requirement specified in `predicate`.

## Running a query
The query can use any of these operators and functions and additionally operate on the fields of a node. To simplify the generation of queries, we provide an initial set of extensions for certain nodes.

An example for such a query could look as follows for the detailed mode:
```kotlin
val memcpyTooLargeQuery = { node: CallExpression ->
    sizeof(node.arguments[0]) gt sizeof(node.arguments[1])
}
```
The same query in the less detailed mode:
```kotlin
val memcpyTooLargeQuery = { node: CallExpression ->
    sizeof(node.arguments[0]) > sizeof(node.arguments[1])
}
```

After assembling a query of the respective operators and functions, we want to run it for a subset of nodes in the graph. We therefore provide two operators: `all` (or `allExtended` for the detailed output) and `exists` (or `existsExtended` for the detailed output). Both are used in a similar way.
They enable the user to optionally specify conditions to determine on which nodes we want to run a query (e.g., only on `CallExpression`s which call a function called "memcpy").

The following snippets use the queries from above to run them on all calls of the function "memcpy" contained in the `TranslationResult` `result`:
```kotlin
val queryTreeResult =
    result.allExtended<CallExpression>(
        { it.name == "memcpy" },
        { sizeof(it.arguments[0]) gt sizeof(it.arguments[1]) }
    )
```
Less detailled:
```kotlin
val queryTreeResult =
    result.all<CallExpression>(
        { it.name == "memcpy" },
        { sizeof(it.arguments[0]) > sizeof(it.arguments[1]) }
    )
```