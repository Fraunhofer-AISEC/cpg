# Specification: Evaluation Order Graph

The Evaluation Order Graph (EOG) is built as edges between AST nodes after the initial translation of the code to the CPG.
Its purpose is to follow the order in which code is executed, similar to a CFG, and additionally differentiate on a finer level of granularity in which order expressions and subexpressions are evaluated.
Every node points to a set of previously evaluated nodes (`prevEOG`) and nodes that are evaluated after (`nextEOG`).
The EOG edges are intraprocedural and thus differentiate from INVOKES edges.
In the following, we summarize in which order the root node representing a language construct and its descendants in the AST tree are connected.

An EOG always starts at root node representing a method/function or record that holds executable code and ends in the node representing the corresponding code or multiple return statements.
An implicit return statement with a code location of (-1,-1) is used if the actual source code does not have an explicit return statement.

A distinct EOG is drawn for any declared component that can contain code, currently: `NamespaceDeclaration`, `TranslationUnitDeclaration`, `RecordDeclaration` and any subclass of `FunctionDeclaration`.
 
The EOG is similar to a CFG which connects basic blocks of statements, but there are some subtle differences:

* For methods without explicit return statement, the EOG will have an edge to a virtual return node  with line number -1 which does not exist in the original code. 
  A CFG will always end with the last reachable statement(s) and not insert any virtual return statements.
* The EOG considers an opening blocking (`CompoundStatement`, indicated by a `{`) as a separate node.
  A CFG will rather use the first actual executable statement within the block.
* For IF statements, the EOG treats the `if` keyword and the condition as separate nodes.
  A CFG treats this as one `if` statement.
* The EOG considers a method header as a node.
  A CFG will consider the first executable statement of the methods as a node.

## General Structure

The graphs in this specification abstract the representation of the handled graph, to formally specify how EOG edges are drawn between a parent node and the subgraphs rooted by its children.
Therefore, a collection of AST children are represented as abstract nodes showing the multiplicity of the node with an indicator (n), in case of sets, or as several nodes showing how the position in a list can impact the construction of an EOG, e.g., nodes (i - 1) to i.
The EOG is constructed as postorder of the AST traversal.
When building the EOG for the expression a + b, the entire expression is considered evaluated after the subexpression a and the subexpression b is evaluated, therefore EOG edges connect nodes of (a) and (b) before reaching the parent node (+).

Note: Nodes describing the titled programing construct will be drawn round, while the rectangular nodes represent their abstract children, that can be atomic leaf nodes or deep AST subtrees.
EOG edges to these abstract nodes always mean that a subtree expansion would be necessary to connect the target of the EOG edge to the right node in the subtree.

```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> lhs
  node --EOG--> next:::outer
  node([+]) -.-> lhs["a"]
  node -.-> rhs["b"]
  lhs --EOG--> rhs
  rhs --EOG--> node
```

Whether a subgraph (a) or (b) is connected first, depends on the exact construct and sometimes the language that is translated into a CPG.
Note that, in the following graphics we will often draw an EOG edge to an abstract child node of a language construct that is an AST subtree.
The EOG path through that subtree will depend on the node types of that tree and mostly start connecting one of the AST leaf nodes.

## FunctionDeclaration
A function declaration is the start of an intraprocedural EOG and contains its end. Therefore there is no incoming or outgoing edge to `previous` or `next` eog nodes that are not in its AST subtree. The EOG connects the code body, as well as the default values of parameters if they exist.

Interesting fields:

* `parameters: List<ParamVariableDeclaration>`: The parameters of the function.
* `defaultValue: Expression`: Optional default values of the parameters that have to be evaluated before executing the function's body.
* `body: Statement`: One or multiple statements executed when this function is called.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  parent(["FunctionDeclaration"]) --EOG-->child1["defaultValue(i-1)"]
  child1 --EOG-->child2["defaultValue(i)"]
  child2  --EOG--> body
  parent -.-> body
  parent -."parameters(n)".->child3["parameter(i-1)"] -.->child1 
  parent -."parameters(n)".->child4["parameter(i)"] -.->child2
```
  
## StatementHolder
StatementHolder is an interface for any node that is not a function and contains code that should be connected with an EOG. The following classes implement this interface: `NamespaceDeclaration`, `TranslationUnitDeclaration`, `RecordDeclaration` and `CompoundStatement`. The Node implementing the interface is the start of one or multiple EOGs. Note that code inside such a holder can be static or non static (bound to an instance of a record). Therefore, two separate EOGs may be built. 

Interesting fields:

* `statements: List<Statement>`: The code inside a holder. The individual elements are distinguished by a property marking them as `staticBlock` if they are a `CompoundStatement`.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  holder([StatementHolder])-."statements(n)".->sblock1["StaticStatement(i-1)"]
  holder([StatementHolder])-."statements(n)".->sblock2["StaticStatement(i)"]
  holder-."statements(n)".->nblock1["NonStaticStatement(i-1)"]
  holder-."statements(n)".->nblock2["NonStaticStatement(i)"]
  holder--EOG-->sblock1
  sblock1--EOG-->sblock2
  holder--EOG-->nblock1
  nblock1--EOG-->nblock2
```

## VariableDeclaration
Represents the declaration of a local variable.

Interesting fields:

* `initializer: Expression`: The result of evaluation will initialize the variable.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child
  parent(["VariableDeclaration"]) --EOG--> next:::outer
  parent -.-> child["initializer"]
  child --EOG--> parent

```

## CallExpression
Represents any type of call in a program.

Interesting fields:

* `base: Expression`: The base in a `MemberCallExpression` or a function pointer in a `CallExpression` evaluated to determine the call target.
* `arguments: List<Expression>`: Mapped to the parameters of the call target but evaluated before the call happens.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child["base"]
  parent(["CallExpression"]) --EOG--> next:::outer
  child --EOG--> arg1["Argument(i-1)"]
  arg1--EOG--> arg2["Argument(i)"]
  arg2["Argument(i)"] --EOG--> parent
  parent -.-> child
  parent -."arguments(n)".-> arg1
  parent -."arguments(n)".-> arg2
```

## MemberExpression
Access to the field in a `RecordDeclaration`.

Interesting fields:

* `base: Expression`: The base evaluated to determine whose field we want to access.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child
  parent(["MemberExpression"]) --EOG--> next:::outer
  parent -.-> child["base"]
  child --EOG--> parent
```

## ArraySubscriptionExpression
Array access in the form of `arrayExpression[subscriptExpression]`.

Interesting fields:

* `arrayExpression: Expression`: The array to be accessed.
* `subscriptExpression: Expression`: The index in the array.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child
  child --EOG--> child2["subscriptExpression"]
  parent(["ArraySubscriptionExpression"]) --EOG--> next:::outer
  parent -.-> child["arrayExpression"]
  parent -.-> child2
  child2 --EOG--> parent
```

## ArrayCreationExpression
Interesting fields:

* `dimensions: List<Expression>`: Multiple expressions that define the array's dimensions.
* `initializer: Expression`: The expression for array initialization.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child1["dimension(i-1)"]
  child1 --EOG--> child2["dimension(i)"]
  child2 --EOG--> initializer
  parent(["ArrayCreationExpression"]) --EOG--> next:::outer
  parent -.-> child1
  parent -.-> child2
  parent -.-> initializer
  initializer --EOG--> parent
```

## KeyValueExpression
Represents a key / value pair that could be used in associative arrays, among others.

Interesting fields:

* `key: Expression`: The key used for later accessing this pair.
* `value: Expression`: The value of the pair.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child
  child --EOG--> child2["value"]
  parent(["KeyValueExpression"]) --EOG--> next:::outer
  parent -.-> child["key"]
  parent -.-> child2
  child2 --EOG--> parent
```

## DeclarationStatement

Here, the EOG is only drawn to the child component if that component is a VariableDeclaration, not if it is a FunctionDeclaration.

```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child
  parent(["DeclarationStatement"]) --EOG--> next:::outer
  parent -.-> child(["VariableDeclaration"])
  child --EOG--> parent

```
## ReturnStatement
This forms the end of an EOG as this is the last statement to be executed in the function.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child
  child["returnValue"] --EOG--> parent(["ReturnStatement"])
  parent -.-> child
```

## BinaryOperator

Interesting fields:

* `lhs: Expression`: Left hand side of a binary operation.
* `rhs: Expression`: Right hand side of a binary operation.
* `operatorCode: String`: The operation.

We differentiate between two cases based on the `operatorCode`.

### Short-circuit evaluation

The operations `&&` and `||` have a short-circuit evaluation. This means that the expression can terminate early if the `lhs` is false (for `&&`) or `true` (for `||`). This affects the EOG by adding an EOG edge from `lhs` to the BinaryOperator.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> lhs
  node --EOG--> next:::outer
  node([BinaryOperator]) -.-> lhs
  node -.-> rhs
  lhs --EOG--> rhs
  lhs --EOG--> node
  rhs --EOG--> node
```
### Default case

For the other binary operations like `+`, `-` but also assignments `=` and `+=` we follow the left before right order. The `lhs` is evaluated before the `rhs` as we assume left to right evaluation.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> lhs
  node --EOG--> next:::outer
  node([BinaryOperator]) -.-> lhs
  node -.-> rhs
  lhs --EOG--> rhs
  rhs --EOG--> node
```


## CompoundStatement

Represents an explicit block of statements.

Interesting fields:

* `statements:List<Statement>`: Statements in a block of code that are evaluated sequentially.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child1["statement(i-1)"]
  child1 --EOG-->child2["statement(i)"]
  parent(["CompoundStatement"]) --EOG--> next:::outer
  parent -."statements(n)".-> child1
  parent -."statements(n)".-> child2
  child2 --EOG--> parent

```

## UnaryOperator
For unary operations like `!` but also writing operations: `++` and `--`.

Interesting fields:

* `input:Expression`: Wrapped by the unary operation.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child["input"]
  child --EOG-->parent
  parent(["UnaryOperator"]) --EOG--> next:::outer
  parent -."statements(n)".-> child

```


### UnaryOperator for exception throws
Throwing of exceptions is modelled as unary operation. The EOG continues at an exception catching structure or a function that does a re-throw. 

Interesting fields:
* `input: Expression`: Exception to be thrown for exception handling.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child["input"]
  child --EOG-->parent
  parent(["throw"]) --EOG--> catchingContext:::outer
  parent -."statements(n)".-> child

```


## AssertStatement
Statement that evaluates a condition and if the condition is false, evaluates a message, this message is generalized to a `Statement` to hold everything 
from a single String, to an Exception construction.

Interesting fields:

* `condition: Expression` Its evaluation leads to evaluation of message and EOG termination or to the regular evaluation of the parent `AssertStatement`.
* `message: Statement`: A String message or Exception evaluated only if the assertion fails. 

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child1["condition"]
  child1 --"EOG:false"-->child2["message"]
  child1 --"EOG:true"-->parent
  parent([AssertStatement]) --EOG--> next:::outer
  parent -.-> child1
  parent -.-> child2

```

## TryStatement

After the execution of the statement the control flow only proceeds with the next statement if all exceptions were handled. If not, execution is relayed to the next outer exception handling context.

Interesting fields:

* `resources:List<Statement>`: Initialization of values needed in the block or special objects needing cleanup.
* `tryBlock:CompoundStatement`: The code that should be tried, exceptions inside lead to an eog edge to the catch clauses.
* `finallyBlock:CompoundStatement`: All EOG paths inside the `tryBlock` or the `catch` blocks will finally reach this block and evaluate it.
* `catchBlocks:List<CompoundStatementt>`: Children of `CatchClause` (omitted here), evaluated when the exception matches the clauses condition.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer -- EOG --> resource["resource(i-1)"];
  resource -.- parent
  resource -- EOG --> resourceI["resource(i)"]
  resourceI -.- parent
  resourceI -- EOG --> try["tryBlock"]
  try -.- parent
  throws:::outer -- EOG --> catch["catchBlock(i)"]
  try -- EOG --> finally["finallyBlock"]
  parent([TryStatement]) --EOG--> next:::outer
  parent --EOG--> catchingContext:::outer
  catch -- EOG --> finally
  finally -- EOG --> parent
  finally -.- parent
  catch -.- parent
```

## ContinueStatement
The execution continues at the `condition` of a node associated to a `Continuable` scope, e.g. `WhileStatement`. This is not necessarily the closest enclosing node of this type, the `ContinueStatement` may contain a label specifying the exact outer node.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> parent
  parent(["ContinueStatement"]) --EOG--> conditionInContinuableContext:::outer

```
## BreakStatement
The execution continues after a node associated to a `Breakable` scope, e.g. `WhileStatement`or `SwitchStatement`. This is not necessarily the closest enclosing node of this type, the `BreakStatement` may contain a label specifying the exact outer node.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> parent
  parent(["BreakStatement"]) --EOG--> nextAfterBreakableContext:::outer
```

## DeleteExpression
Deletion of a specific object freeing memory or calling the destructor.

Interesting fields:

* `operand: Expression`: The result of the evaluation is the object to be deleted.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child["operand"]
  child --EOG--> parent
  parent(["DeleteExpression"]) --EOG--> next:::outer
  parent -.-> child
```

## LabelStatement
The `LabelStatement` itself is not added to the EOG. EOG construction is directly forwarded to the labeled statement in the `subStatement`.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child["subStatement"]
  child --EOG--> next:::outer
  parent(["LabelStatement"]) -.-> child
```

## GotoStatement
Models a `goto`statement and an EOG-Edge is created to the appropriate `LabelStatement`.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child(["GotoStatement"])
  child --EOG--> labeledStatement:::outer
```

## NewExpression
Creates a new object, which is either an array or an instantiation of a `RecordDeclaration`. The initializer has to be evaluated to create the object.

Interesting fields:

* `initializer: Expression`: To be evaluated before creating a new object.

* Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child["initializer"]
  child --EOG--> parent
  parent(["NewExpression"]) --EOG--> next:::outer
  parent -.-> child
```

## CastExpression
Interesting fields:

* `expression: Expression`: An expression of a specific compile time type, cast to a specified other compile time type.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child["expression"]
  child --EOG--> parent
  parent(["CastExpression"]) --EOG--> next:::outer
  parent -.-> child
```

## ExpressionList
List of several expressions that aer evaluated sequentially. The resulting value is the last evaluated expression.

Interesting fields:

* `expressions: List<Expression>`: Several expressions in sequential order.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child1["expression(i-1)"]
  child1 --EOG--> child2["expression(i)"]
  child2 --EOG--> parent
  parent(["ExpressionList"]) --EOG--> next:::outer
  parent -."expressions(n)".-> child1
  parent -."expressions(n)".-> child2
```

## InitializerListExpression
This expression initializes multiple variables or an object of multiple elements, e.g. arrays, lists.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child1["initializer(i-1)"]
  child1 --EOG--> child2["initializer(i)"]
  child2 --EOG--> parent
  parent(["InitializerListExpression"]) --EOG--> next:::outer
  parent -."initializers(n)".-> child1
  parent -."initializers(n)".-> child2
```

## ConstructExpression
A ConstructExpression creates an object.

Interesting fields:

* `arguments: List<Expression>`: Arguments to the construction, e.g. arguments for a call to a constructor.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child1["argument(i-1)"]
  child1 --EOG--> child2["argument(i)"]
  child2 --EOG--> parent
  parent(["ConstructExpression"]) --EOG--> next:::outer
  parent -."arguments(n)".-> child1
  parent -."arguments(n)".-> child2
```

## SynchronizedStatement
The placement of the root node between expression and executed block is such that algorithms can be evaluated the expression and then encountering the information that this expression is used for synchronization.

Interesting fields:

* `expression: Expression`: Its evaluation returns an object that acts as a lock for synchronization.
* `blockStatement: CompoundStatement`: Code executed while the object evaluated from `expression` is locked.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child1["expression"]
  child1 --EOG--> parent
  parent --EOG--> child2["blockStatement"]
  child2 --EOG--> next:::outer
  parent -.-> child1
  parent -.-> child2
```

## ConditionalExpression
A conditional evaluation of two expression, realizing the branching pattern of an `IfStatement` on the expression level.

Interesting fields:

* `condition:Expression`: Executed first to decide the branch of evaluation.
* `thenExpr:Expression`: Evaluated if `condition` evaluates to `true.`
* `elseExpr:Expression`: Evaluated if `condition` evaluates to `false.`

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child1["condition"]
  child1 --EOG--> parent(["ConditionalExpression"])
  parent --EOG:true--> child2["thenExpr"]
  parent --EOG:false--> child3["elseExpr"]
  child2 --EOG--> next:::outer
  child3 --EOG--> next:::outer
  parent -.-> child1
  parent -.-> child2
  parent -.-> child3
```

## WhileStatement
This is a classic while loop where the condition is evaluated before every loop iteration.

Note: The condition may be enclosed in a declaration, in that case the EOG will not contain a `condition` but rather a declaration of a variable where the `initializer` serves as loop condition. Uses of one or the other are currently mutually exclusive.

Interesting fields:

* `condition: Expression`: The condition for the loop.
* `conditionDeclaration: Declaration`: The declaration of a variable with condition as initializer.
* `statement: Statement`: The body of the loop to be iterated over.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child1["condition|conditionDeclaration"]
  child1 --EOG--> parent
  parent --EOG:false--> next:::outer
  parent(["WhileStatement"]) --EOG:true--> child3["statement"]
  child3 --EOG--> child1
  parent -.-> child1
  parent -.-> child3
```

## DoStatement
This is a classic do while loop where the condition is evaluated after every loop iteration.

Interesting fields:

* `condition: Expression`: The condition of the loop.
* `statement: Statement`: The body of the loop to be iterated over.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child1["statement"];
  child1 --EOG--> child2["condition"];
  child2 --EOG--> parent(["DoStatement"]);
  parent --EOG:false--> next:::outer
  parent --EOG:true--> child1
  parent -.-> child1
  parent -.-> child2
```

## ForEachStatement
This is a loop that iterates over all elements in a multi-element `iterable` with the single elements bound to the declaration of `variable` while evaluating `statement`.

Interesting fields:

* `iterable: Statement`: Elements of this iterable will trigger a loop iteration.
* `variable: Statement`: Variable declaring Statement that binds elements to a name.
* `statement: Statement`: Loop body to be iterated over.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child1["iterable"]
  child1 --EOG--> child2["variable"]
  child2 --EOG--> parent
  parent --EOG:false--> next:::outer
  parent(["ForEachStatement"]) --EOG:true--> child3["statement"]
  child3 --EOG--> child1
  parent -.-> child2
  parent -.-> child1
  parent -.-> child3
```

## ForStatement
This is a classic for-loop where a statement is executed before the loop run, a condition is evaluated before every loop iteration, and a post iteration statement can be declared.

Note: The condition may be enclosed in a declaration. In this case, the EOG will not contain a `condition` but rather a declaration of a variable where the `initializer` serves as loop condition. Uses of one or the other are currently mutually exclusive.

Interesting fields:

* `initializerStatement:Statement`: Statement run once, before the loop starts.
* `condition: Expression`: The condition of the loop.
* `conditionDeclaration: Declaration`: The declaration of a variable with the condition as initializer.
* `statement: Statement`: The body of the loop to be iterated over.
* `iterationStatement: Statement`: The statement to be executed after each loop iteration.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  iteration --EOG--> condition
  statement --EOG--> iteration["iterationStatement"]
  prev:::outer --EOG--> initializer["initializerStatement"]
  parent --EOG:false--> next:::outer
  initializer --EOG--> condition["condition|conditionDeclaration"]
  condition --EOG--> parent
  parent(["ForStatement"]) --EOG:true--> statement["statement"]
```

## IfStatement
This is a branching statement where the evaluation of a `condition` leads to the execution of one optional, or two mutually exclusive blocks of code.

Note: The condition may be enclosed in a declaration, in that case the EOG will not contain a `condition` but rather a declaration of a variable where the `initializer` serves as branching condition. Uses of one or the other are currently mutually exclusive.

Interesting fields:

* `condition: Expression`: The condition of the branching decision.
* `conditionDeclaration: Declaration`: The declaration of a variable with condition as initializer.
* `thenStatement: Statement`: The body of the mandatory block that is evaluated if the `condition` evaluates to `true`.
* `elseStatement: Statement`: The body of an optional block that is evaluated if the `condition` evaluates to `false`.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child1["initializerStatement"]
  child1 --EOG--> child2["condition|conditionDeclaration"]
  child2 --EOG--> parent
  parent(["IfStatement"]) --EOG:true--> child4["thenStatement"]
  parent --EOG:false--> child5["elseStatement"]
  parent --EOG--> next:::outer
  child4 --EOG--> next:::outer
  child5 --EOG--> next:::outer
```

## SwitchStatement
This is a switch statement where the evaluation of a `selector` decides the entry point in a large block of code. `CaseStatements` serve as entry points and `BreakStatements` are needed to prevent all cases after the entry to be evaluated.

Note: The `selector` may be enclosed in a declaration. In this case, the EOG will not contain a selector but rather a declaration of a variable where the `initializer` serves as switch selector. Uses of one or the other are currently mutually exclusive.

Interesting fields:

* `selector: Expression`: The evaluated selector which needs to match the expression evaluation of the expression in a `caseStatement` or the entry will be the `defaultStatement`.
* `selectorDeclaration: Declaration`: The declarations `initializer` serves as `selector`.
* `statement: Statement`: The body containing all entry points and statements to be executed.
* `caseStatement: Statement`: The entry point into the evaluation of the switch body if the `selector` matches its `caseExpression`.
* `defaultStatement: Statement`: The default entry point if no `caseExpression` matched the selector.

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child1["initializerStatement"]
  child1 --EOG--> child2["selector|selectorDeclaration"]
  child2 --EOG--> parent
  parent(["SwitchStatement"]) --EOG--> child4["caseStatement"]
  parent --EOG--> child5["defaultStatement"]
  child7["statement(n-1)"] --EOG--> child6["statement"]
  parent -.->child6
  child6 -."statements(n)".-> child4
  child6 -."statements(n)".-> child5
  child6 -."statements(n)".-> child7
  child6 --EOG--> next:::outer
```

## CaseStatement

Serves as an entry point inside a `SwitchStatement`, the statements executed after entry are not children of this structure but can be found on the same AST hierarchy level. 

Interesting fields:

* `caseExpression: Expression`: serves as an entry point if its evaluation matches the `selector` evaluation in `SwitchStatement`

Scheme:
```mermaid
flowchart LR
  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;
  prev:::outer --EOG--> child["caseExpression"]
  child --EOG--> parent
  parent(["CaseStatement"]) --EOG--> next:::outer
  parent -.-> child

```




  

