# Specification: Data Flow Graph

The Data Flow Graph (DFG) is built as edges between nodes. Each node has a set of incoming data flows (`prevDFG`) and outgoing data flows (`nextDFG`). In the following, we summarize how different types of nodes construct the respective data flows.


## CallExpression

Interesting fields:
* `invokes: List<FunctionDeclaration>`: A list of the functions which are called
* `arguments: List<Expression>`: The arguments which are used in the function call

A call expressions calls another function. We differentiate two types of call expressions: 1) the called function is implemented in the program (and we can analyze the code) and 2) the called function cannot be analyzed (e.g., this is the case for library/API functions). For the first case, the `invokes` list contains values, in the second case, the list is empty.

### Case 1: Known function

For each function in the `invokes` list, the arguments of the call expression flow to the function's parameters. The value of the function declaration flows to the call.

Scheme:
* `argument[i]` -- DFG --> `invokes[j].parameter[i]` for all i, j
* `invokes[j]` -- DFG --> `CallExpression` for all j

### Case 2: Unknown function

The base and all arguments flow to the call expression.

Scheme:
* `argument[i]` -- DFG --> `CallExpression` for all i
* `base` -- DFG --> `CallExpression`

## CastExpression

Interesting fields:
* `expression: Expression`: The inner expression which has to be casted

The value of the `expression` flows to the cast expression.


## BinaryOperator

Interesting fields:
* `operatorCode: String`: String representation of the operator
* `lhs: Expression`: The left-hand side of the operation
* `rhs: Expression`: The right-hand side of the operation

We have to differentiate between the operators. We can group them into three categories: 1) Assignment, 2) Assignment with a Computation and 3) Computation

### Case 1: Assignment (`operatorCode: =`)

The `rhs` flows to `lhs`.

Scheme:
* `rhs` -- DFG --> `lhs`

### Case 2: Assignment with a Computation (`operatorCode: *=, /=, %=, +=, -=, <<=, >>=, &=, ^=, |=` )

The `lhs` and the `rhs` flow to the binary operator expression, the binary operator flows to the `lhs`.

Scheme:
* `lhs` -- DFG --> `(lhs operatorCode rhs)`
* `rhs` -- DFG --> `(lhs operatorCode rhs)`
* `(lhs operatorCode rhs)` -- DFG --> `lhs`

*Dangerous: We have to ensure that the first two operations are performed before the last one*


### Case 3: Computation

The `lhs` and the `rhs` flow to the binary operator expression.

Scheme:
* `lhs` -- DFG --> `(lhs operatorCode rhs)`
* `rhs` -- DFG --> `(lhs operatorCode rhs)`


## ArrayCreationExpression

Interesting fields:
* `initializer: Expression`: The initialization values of the array.

The `initializer` flows to the array creation expression.

Scheme:
* `initializer` -- DFG --> `ArrayCreationExpression`


## ArraySubscriptionExpression

Interesting fields:
* `arrayExpression: Expression`: The array which is accessed
* `subscriptExpression: Expression`: The index which is accessed

The `arrayExpression` flows to the subscription expression. This means, we do not differentiate between the field which is accessed.

Scheme:
* `arrayExpression` -- DFG --> `ArraySubscriptionExpression`


## ConditionalExpression

Interesting fields:
* `condition: Expression`: The condition which is evaluated
* `thenExpr: Expression`: The expression which is executed if the condition holds
* `elseExpr: Expression`: The expression which is executed if the condition does not hold

The `thenExpr` and the `elseExpr` flow to the `ConditionalExpression`. This means that implicit data flows are not considered.

Scheme:
* `thenExpr` -- DFG --> `ConditionalExpression`
* `elseExpr` -- DFG --> `ConditionalExpression`

## DeclaredReferenceExpression

Interesting fields:
* `refersTo: Declaration`: The declaration e.g. of the variable or symbol
* `access: AccessValues`: Determines if the value is read from, written to or both

The value flows from the declaration to the expression for read access.

For write access, data flow from the expression to the declaration.

For readwrite access, both flows are present.

*This is very very dangerous and is completely changed in the ControlFlowSensitiveDFGPass! Update and fix!*

## ExpressionList

Interesting fields:
* `expressions: List<Statement>`

The data of the last statement in `expressions` flows to the expression.

## InitializerListExpression

Interesting fields:
* `initializers: List<Expression>`: The list of expressions which initialize the values.

The data of all initializers flow to this expression.

Scheme:
* `initializers[i]` -- DFG --> `ConditionalExpression` for all i


## KeyValueExpression

Interesting fields:
* `value: Expression`: The value which is assigned.

The value flows to this expression.

Scheme:
* `value` -- DFG --> `KeyValueExpression`


## LambdaExpression

Interesting fields:
* `function: FunctionDeclaration`: The usage of a lambda

The data flow from the function representing the lambda to the expression.

Scheme:
* `function` -- DFG --> `LambdaExpression`


## UnaryOperator

Interesting fields:
* `input: Expression`: The inner expression
* `operatorCode: String`: A string representation of the operation

The data flow from the input to this node and, in case of the operatorCodes ++ and -- also back from the node to the input.

*Dangerous: We have to ensure that the first operation is performed before the last one (if applicable)*


## ReturnStatement

Interesting fields:
* `returnValue: Expression`: The value which is returned

The return value flows to the whole statement.


## FunctionDeclaration

Interesting fields:
* `body: Expression`: The body (i.e., all statements) of the function implementation

The values of all return expressions in the body flow to the function declaration.

Scheme:
* `ReturnExpression` -- DFG --> `FieldDeclaration` for all returns


## FieldDeclaration

Interesting fields:
* `initializer: Expression?`: The value which is used to initialize a field (if applicable).

The value of the initializer flows to the whole field.

In addition, all writes to a reference to the field (via a `DeclaredReferenceExpression`) flow to the field, for all reads, data flow to the reference.

Scheme:
* `initializer` -- DFG --> field

## VariableDeclaration

Interesting fields:
* `initializer: Expression?`: The value which is used to initialize a variable (if applicable).

The value of the initializer flows to the variable declaration. The value of the variable declarations flows to all `DeclaredReferenceExpressions` which read the value before the value of the variable is written to through another reference to the variable.

Scheme:
* `initializer` -- DFG --> `VariableDeclaration`
* `VariableDeclaration` -- DFG -->  `ref` where `ref.access == AccessValues.READ` and if there is a path between `ref` and `VariableDeclaration` where no write access to the variable takes place


## Assignment

Interesting fields:
* `value: Expression`: The rhs of the assignment
* `target: AssignmentTarget`: The lhs of the assignment

This should already be covered by the declarations and binary operator "=". If not, the `value` flows to the `target`

