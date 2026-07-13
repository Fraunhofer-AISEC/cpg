/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.mcp.mcpserver.tools

import de.fraunhofer.aisec.cpg.mcp.FUNCTION_SUMMARIES_FILE
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import java.io.File
import kotlinx.serialization.json.buildJsonObject

fun Server.addQueryTools() {
    this.addCpgQueryApiTool()
    this.addCpgNodeTypesTool()
    this.addCpgFunctionSummariesTool()
}

fun Server.addCpgQueryApiTool() {
    this.addTool(
        name = "cpg_query_api",
        description =
            """
            Serves as the api syntax provider of the CPG query dsl.

            Call this before writing a query so you use the correct function names and parameters
            instead of guessing. Use cpg_node_types for the node types and members usable inside a
            predicate, and the source code browsing tools (cpg_list_*) to find the start nodes.
            """
                .trimIndent(),
        inputSchema = ToolSchema(properties = buildJsonObject {}, required = listOf()),
    ) { _ ->
        CallToolResult(
            content =
                listOf(
                    TextContent(
                        """
    # CPG Query DSL

    The queries are written in Kotlin. The analyzed program is available as `result: TranslationResult`.
    All query functions return a `QueryTree<Boolean>`.

    ## dataFlow: "does this value ever reach X?"

    ```kotlin
    fun dataFlow(
        startNode: Node,
        direction: AnalysisDirection = Forward(GraphToFollow.DFG),
        type: AnalysisType = May,
        vararg sensitivities: AnalysisSensitivity = FieldSensitive + ContextSensitive,
        scope: AnalysisScope = Interprocedural(),
        ctx: Context = Context(steps = 0),
        earlyTermination: ((Node) -> Boolean)? = null,
        predicate: (Node) -> Boolean,
    ): QueryTree<Boolean>
    ```

    Follows data-flow (DFG) edges from `startNode` until a node matches `predicate`.

    Example: does the value of `call` ever reach a reference named "sink":
    ```kotlin
    dataFlow(startNode = call) { it is Reference && it.name.localName == "sink" }
    ```

    ## executionPath: "is Y ever/always executed after X?"

    ```kotlin
    fun executionPath(
        startNode: Node,
        direction: AnalysisDirection = Forward(GraphToFollow.EOG),
        type: AnalysisType = May,
        scope: AnalysisScope = Interprocedural(),
        earlyTermination: ((Node) -> Boolean)? = null,
        predicate: (Node) -> Boolean,
    ): QueryTree<Boolean>
    ```

    Follows execution-order (EOG) edges until a node matches `predicate`. Use for
    control-flow questions.

    Example: is a call to "someFunc" ever executed after `call`:
    ```kotlin
    executionPath(startNode = call) { it is Call && it.name.localName == "someFunc" }
    ```

    ## alwaysFlowsTo — "does this data reach X on ALL paths?"

    ```kotlin
    fun Node.alwaysFlowsTo(
        allowOverwritingValue: Boolean = false,
        earlyTermination: ((Node) -> Boolean)? = null,
        identifyCopies: Boolean = true,
        stopIfImpossible: Boolean = true,
        scope: AnalysisScope, // required, no default
        vararg sensitivities: AnalysisSensitivity =
            ContextSensitive + FieldSensitive + FilterUnreachableEOG,
        predicate: (Node) -> Boolean,
    ): QueryTree<Boolean>
    ```

    True if the data in the receiver node flows through a node matching `predicate` on every
    execution path (a "must" guarantee).

    Example: the value of `variable` always reaches a call to "someFunction":
    ```kotlin
    variable.alwaysFlowsTo(scope = Interprocedural()) { 
        it is Call && it.name.localName == "someFunction"
    }
    ```

    ## allExtended / existsExtended: quantifiers over all nodes of a type

    ```kotlin
    inline fun <reified T> Node.allExtended/existsExtended(
        noinline sel: ((T) -> Boolean)? = null,
        noinline mustSatisfy: (T) -> QueryTree<Boolean>,
    ): QueryTree<Boolean>
    ```

    `allExtended`: `mustSatisfy` must hold for all nodes of type `T` below the receiver (use `sel`
    to filter which ones count). `existsExtended`: it must hold for at least one. 
    
    Example: for every call to "someFunction", its first argument flows to a literal:
    ```kotlin
    result.allExtended<Call>({ it.name.localName == "someFunction" }) { call ->
        dataFlow(startNode = call.arguments[0]) { it is Literal<*> }
    }
    ```

    ## follow*UntilHit: "which paths lead from X to Y?"

    All follow functions are wrappers around the same generic graph walker: they walk the
    edges of one subgraph from the receiver node until a node which matches `predicate`, and return the
    paths instead of a QueryTree:

    ```kotlin
    class FulfilledAndFailedPaths(
        val fulfilled: List<NodePath>,                   // paths that reached a matching node
        val failed: List<Pair<FailureReason, NodePath>>, // paths that did not
    )
    ```

    Each NodePath has `nodes: List<Node>` and `edges: List<Edge<Node>>`.

    The DFG and EOG variants take the same direction/sensitivities/scope parameters as dataFlow
    and executionPath:

    ```kotlin
    fun Node.followDFGEdgesUntilHit(
        collectFailedPaths: Boolean = true,
        findAllPossiblePaths: Boolean = true,
        direction: AnalysisDirection = Forward(GraphToFollow.DFG),
        vararg sensitivities: AnalysisSensitivity = FieldSensitive + ContextSensitive,
        scope: AnalysisScope = Interprocedural(),
        ctx: Context = Context(steps = 0),
        earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
        predicate: (Node) -> Boolean,
    ): FulfilledAndFailedPaths
    ```

    `followEOGEdgesUntilHit`: same shape, defaults `Forward(GraphToFollow.EOG)` and
    `FilterUnreachableEOG + ContextSensitive`.
    `followPrevFullDFGEdgesUntilHit` / `followNextFullDFGEdgesUntilHit`: shortcuts fixed to
    `Backward`/`Forward` with `OnlyFullDFG + ContextSensitive`.

    The CDG and PDG variants (prevCDG = the branch conditions this node's execution depends on.
    The PDG combines DFG and CDG):

    ```kotlin
    fun Node.followPrevCDGUntilHit(
        collectFailedPaths: Boolean = true,
        findAllPossiblePaths: Boolean = true,
        interproceduralAnalysis: Boolean = false,
        interproceduralMaxDepth: Int? = null,
        earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
        predicate: (Node) -> Boolean,
    ): FulfilledAndFailedPaths
    ```

    `followNextCDGUntilHit`, `followPrevPDGUntilHit` and `followNextPDGUntilHit` share this
    signature.

    Example: is `node` on every path guarded by a call to "someCheck":
    ```kotlin
    val paths = node.followPrevCDGUntilHit { it is Call && it.name.localName == "someCheck" }
    val guardedOnAllPaths = paths.failed.isEmpty()
    ```

    ## min / max / size: "what values can this expression have at runtime?"

    ```kotlin
    fun min(n: Node?, eval: ValueEvaluator = IntegerIntervalEvaluator()): QueryTree<Number>
    fun max(n: Node?, eval: ValueEvaluator = IntegerIntervalEvaluator()): QueryTree<Number>
    val Expression.size: QueryTree<Int>
    ```

    `min`/`max` evaluate the smallest/largest possible runtime value of an expression (both also
    accept a `List<Node>`), `size` the allocated size of an array or buffer. Compare with the
    infix operators `lt`, `le`, `gt`, `ge` (numbers) and `eq`, `ne` (any value). They accept
    plain values as well as QueryTrees and return a `QueryTree<Boolean>`:

    Example: the first argument of "someFunction" is never negative:
    ```kotlin
    result.allExtended<Call>({ it.name.localName == "someFunction" }) { call ->
        min(call.arguments[0]) ge 0
    }
    ```

    ## Combining results

    `QueryTree<Boolean>` results combine with `not(q)`, `q1 and q2`, `q1 or q2`, `q1 xor q2`.
    A `List<QueryTree<Boolean>>` merges into one result with `.mergeWithAll()` (true if all
    elements are true) or `.mergeWithAny()` (true if at least one is true).

    ## Parameter values

    AnalysisType `type`:
      `Must`: property must hold on all paths. 
      `May`: property holds on at least one path.

    AnalysisDirection `direction`:
      `Forward(GraphToFollow.DFG)` / `Backward(GraphToFollow.DFG)`: data flow.
      `Forward(GraphToFollow.EOG)` / `Backward(GraphToFollow.EOG)`: execution order.

    AnalysisScope `scope`:
      `Interprocedural(maxCallDepth: Int? = null, maxSteps: Int? = null)`: cross function boundaries. 
      `Intraprocedural(maxSteps: Int? = null)`: within one function. 

    AnalysisSensitivity `sensitivities`:
      `FieldSensitive`: treat `obj.a` and `obj.b` (or `arr[0]` and `arr[1]`) as separate values.
      `ContextSensitive`: a flow leaving a function returns only to the call site it came from.
      `FilterUnreachableEOG`: skip EOG edges marked unreachable.
      `OnlyFullDFG`: only follow flows of the whole value, not flows into a part of it (e.g. one field).
      `Implicit`: also count a branch condition depending on the value as a flow (control-flow leaks).

    ## Function summaries (only for functions without body)

    Only relevant for queries about memory or pointer state (points-to analysis) that must
    track data flow through a function whose body is not part of the analyzed code (e.g. free
    from the C standard library). In that case, call cpg_function_summaries to check whether
    its effect is modeled. For all other queries this is not needed.

    A summary can introduce a synthetic value (an UnknownMemoryValue node, see cpg_node_types).
    To retrieve it as a start node for a dataFlow query:
    ```kotlin
    val startNode = result.functions
        .filter { it.name.localName == "someFunction" }
        .flatMap { it.functionSummary.values.flatten() }
        .mapNotNull { it.srcNode as? UnknownMemoryValue }
    ```

    ## Result: QueryTree<Boolean>
    `.value`:  Boolean.
    `.children`: sub-results, including the node paths found.
    `.stringRepresentation`: human-readable description to reason about the result.
    """
                            .trimIndent()
                    )
                )
        )
    }
}

fun Server.addCpgNodeTypesTool() {
    this.addTool(
        name = "cpg_node_types",
        description =
            """
            Returns the CPG node types (Call, Reference, Function, Literal, etc.) and their members
            that can be used inside a query predicate.
            Call this together with cpg_query_api before writing a query.
            """
                .trimIndent(),
        inputSchema = ToolSchema(properties = buildJsonObject {}, required = listOf()),
    ) { _ ->
        CallToolResult(
            content =
                listOf(
                    TextContent(
                        """
    # CPG node types

    ## Node: the base of all nodes

    ```kotlin
    name: Name                  // name.localName = unqualified name; name.toString() = qualified
    code: String?               // the original source code snippet of this node
    location: PhysicalLocation? // file and line
    astParent: AstNode?         // parent in the syntax tree
    prevDFG: List<Node>         // data flows into this node
    nextDFG: List<Node>         // data flows out of this node
    prevFullDFG: List<Node>     // same as prevDFG, but only flows of the whole value
    nextFullDFG: List<Node>
    prevEOG: List<Node>         // executed directly before this node
    nextEOG: List<Node>         // executed directly after this node
    prevEOGEdges / nextEOGEdges // the EOG edges themselves; edge.unreachable == true marks
                                // an edge that can never be executed
    ```

    ## Expressions

    Every expression additionally has `type: Type` (compare via `type.name.localName`) and
    `access: AccessValues` (READ, WRITE or READWRITE). Expressions that denote memory also have
    `memoryAddresses: Set<MemoryAddress>` the memory cell the expression refers to. For example
    expressions alias share a MemoryAddress: `a.memoryAddresses.any { it in b.memoryAddresses }`.

    ### Call: a function call

    ```kotlin
    callee: Expression          // the expression being called
    arguments: List<Expression> // the call arguments
    invokes: List<Function>     // the resolved call target(s)
    ```

    Example predicate: `it is Call && it.name.localName == "malloc"`

    ### MemberCall: a call on an object, e.g. `obj.method()` (extends Call)

    ```kotlin
    base: Expression?           // the object the method is called on
    ```

    ### Reference: a usage of a variable or other symbols

    ```kotlin
    refersTo: Declaration?      // the declaration this reference points to;
    ```

    Example predicate: `(it as? Reference)?.refersTo == (call.arguments[0] as? Reference)?.refersTo`

    ### Literal<T>: a value in the code

    ```kotlin
    value: T?                   // e.g. (it as? Literal<*>)?.value == 0
    ```

    ### Assign: an assignment, e.g. `a = b` or `a += b`

    ```kotlin
    lhs: List<Expression>       // assignment target(s)
    rhs: List<Expression>       // assigned value(s)
    operatorCode: String        // "=", "+=", etc.
    ```

    ### BinaryOperator: e.g. `a + b`, `a == b`

    ```kotlin
    lhs: Expression
    rhs: Expression
    operatorCode: String        // "+", "==", "<", etc.
    ```

    ### UnaryOperator: e.g. `i++`

    ```kotlin
    input: Expression
    operatorCode: String        // "*", "&", "++", etc.
    ```

    ### Subscription: an array access, e.g. `a[i]`

    ```kotlin
    arrayExpression: Expression     // the array, e.g. `a`
    subscriptExpression: Expression // the index, e.g. `i`
    ```

    ### PointerDereference: e.g. `*p` (extends Reference)

    ```kotlin
    input: Expression           // the pointer being dereferenced
    ```

    ### Cast: a cast, e.g. `(char *) p`

    ```kotlin
    expression: Expression      // the value being cast
    ```

    ### Delete: a C++ `delete` expression

    ```kotlin
    operands: List<Expression>  // what is deleted
    ```

    ## Synthetic memory nodes (created by the points-to analysis)

    These represent memory without a syntactic representation in the code:

    ### MemoryAddress: a memory cell

    ```kotlin
    usages: List<Node>          // all expressions reading or writing this cell
    ```

    ### ParameterMemoryValue: the (dereferenced) value of a parameter at function entry

    ### UnknownMemoryValue: a placeholder for a value that has no source in the analyzed code

    The synthetic values introduced by function summaries (see cpg_function_summaries) appear
    in the graph as these nodes. Identify them by their name, e.g.
    `it is UnknownMemoryValue && it.name.localName == "someValue"`.

    ## Declarations

    ### Function: a function declaration

    ```kotlin
    parameters: List<Parameter> // the declared parameters, in order
    body: Expression?           // the function body
    ```

    ### Method: a function belonging to a Record (extends Function)

    ### Variable: a variable declaration

    ```kotlin
    initializer: Expression?    // the initial value, if any
    ```

    ### Parameter: a declared function parameter

    ### Record: a class/struct/type declaration

    ## Finding nodes shortcut collections

    ```kotlin
    result.records: List<Record>
    result.functions: List<Function>
    result.calls: List<Call>
    result.variables: List<Variable>
    result.refs: List<Reference>
    result.literals: List<Literal<*>>
    ```

    Filter with plain Kotlin: `result.calls.filter { it.name.localName == "foo" }`.
    """
                            .trimIndent()
                    )
                )
        )
    }
}

fun Server.addCpgFunctionSummariesTool() {
    this.addTool(
        name = "cpg_function_summaries",
        description =
            """
            Shows the data-flow summaries of functions without a body in the analyzed code
            (e.g. `free`). Only call this for queries about memory or pointer state (points-to
            analysis) that must track data flow through such a function.
            """
                .trimIndent(),
        inputSchema = ToolSchema(properties = buildJsonObject {}, required = listOf()),
    ) { _ ->
        val summaries = File(FUNCTION_SUMMARIES_FILE)
        if (!summaries.exists()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("No function summaries file found."))
            )
        }
        val text =
            """
            Only needed for queries about memory or pointer state (points-to analysis) that track
            data flow through a function whose body is not part of the analyzed code (e.g. `free`
            from the C standard library). These summaries model their effect, e.g. "paramX.deref" is the memory the argument points to. A "from" value
            that is no parameter (e.g. freedMemory) is a synthetic `UnknownMemoryValue` node and can be used as a start node.
            """
                .trimIndent() + "\n\n" + summaries.readText()
        CallToolResult(content = listOf(TextContent(text)))
    }
}
