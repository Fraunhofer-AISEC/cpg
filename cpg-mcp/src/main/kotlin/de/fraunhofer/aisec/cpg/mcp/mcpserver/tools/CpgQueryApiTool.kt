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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.mcp.FUNCTION_SUMMARIES_FILE
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgQueryScript
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.runOnCpg
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toUnmodeledInfo
import de.fraunhofer.aisec.cpg.passes.inference.DFGFunctionSummaries
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import java.io.File
import java.time.LocalDate
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.JvmScriptCompiler
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

fun Server.addQueryTools() {
    this.addCpgQueryApiTool()
    this.addCpgNodeTypesTool()
    this.addCpgFunctionSummariesTool()
    this.addCpgAddFunctionSummaryTool()
    this.addCpgListUnmodeledFunctionsTool()
    this.addCpgValidateQueryTool()
}

fun Server.addCpgValidateQueryTool() {
    this.addTool(
        name = "cpg_validate_query",
        description =
            """
            Compiles a CPG query to check for errors without executing it. This includes syntax errors, unresolved
            names, wrong function names/parameters, and type errors. Call this after writing a query to catch errors 
            and to receive helpful tips. A successful compile does not mean the query is logically correct, only that it 
            compiles without errors.
            """
                .trimIndent(),
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        putJsonObject("queryCode") {
                            put("type", "string")
                            put(
                                "description",
                                "The Kotlin CPG query script to compile. The analyzed source " +
                                    "code is available as `result: TranslationResult`, as " +
                                    "described by cpg_query_api.",
                            )
                        }
                    },
                required = listOf("queryCode"),
            ),
    ) { request ->
        val queryCode = (request.arguments?.get("queryCode") as? JsonPrimitive)?.contentOrNull
        if (queryCode.isNullOrBlank()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Missing parameter queryCode."))
            )
        }

        val compilationConfiguration =
            createJvmCompilationConfigurationFromTemplate<CpgQueryScript>()
        val compileResult =
            JvmScriptCompiler().invoke(queryCode.toScriptSource(), compilationConfiguration)

        when (compileResult) {
            is ResultWithDiagnostics.Success ->
                CallToolResult(content = listOf(TextContent("Query compiles successfully.")))
            is ResultWithDiagnostics.Failure -> {
                val errors =
                    compileResult.reports.filter { it.severity >= ScriptDiagnostic.Severity.ERROR }
                CallToolResult(
                    content =
                        listOf(
                            TextContent(
                                "Compilation error: " + errors.joinToString("; ") { it.message }
                            )
                        )
                )
            }
        }
    }
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
            Once you've written a query, call cpg_validate_query to catch compile errors before
            running it.
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
    
    The queries are written in Kotlin. The analyzed source code is available as `result: TranslationResult`.
    All query functions return a `QueryTree<Boolean>`.

    A query is typically built in three steps:
    1. Starting point: quantify over the nodes to check with `allExtended` / `existsExtended`,
       or pick concrete start nodes from the shortcut collections (see cpg_node_types).
    2. Inside `mustSatisfy`, state the property using the flow functions (`dataFlow`,
       `executionPath`, `alwaysFlowsTo`) or the value functions (`min`, `max`, `size`).
    3. Combine sub-results with `and` / `or` / `not(...)` into one `QueryTree<Boolean>`.

    ## Starting point: allExtended / existsExtended — "does this hold for all / for at least one?"

    ```kotlin
    inline fun <reified T> Node.allExtended/existsExtended(
        noinline sel: ((T) -> Boolean)? = null,
        noinline mustSatisfy: (T) -> QueryTree<Boolean>,
    ): QueryTree<Boolean>
    ```

    `allExtended`: `mustSatisfy` must hold for all nodes of type `T` below the receiver (use `sel`
    to filter). `existsExtended`: it must hold for at least one.

    Example: no value returned by a call to "sourceFunc" ever reaches a call to "sinkFunc":
    ```kotlin
    result.allExtended<Call>({ it.name.localName == "sourceFunc" }) { call ->
        not(dataFlow(startNode = call) { it is Call && it.name.localName == "sinkFunc" })
    }
    ```

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

    Follows data-flow (DFG) edges from `startNode` until a node matches `predicate`. It only
    reasons about existing DFG paths. For the guarantee that data always reaches something,
    use alwaysFlowsTo. To check that a flow can never happen, wrap it in `not(...)`.

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

    ## alwaysFlowsTo: "does this data reach X on ALL paths?"

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
    elements are true) or `.mergeWithAny()` (true if at least one is true). If a step yields a
    plain Boolean (e.g. after aggregating with Kotlin's `map`/`any`), wrap it with
    `QueryTree(value)` to use it as a result.

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

    For a function whose body isn't part of the analyzed code (e.g. `free` from the C
    standard library), the graph doesn't know what it actually does to its arguments, so a
    `dataFlow`/`alwaysFlowsTo`/`executionPath` check just treats the call as if nothing
    happened, even when the function invalidates a pointer or changes what it points to. A
    query meant to catch a bug pattern in general, not just one example of it, needs these
    effects registered, not only the functions the query happens to name.

    Example: without a summary for `free`, a fresh allocation still looks reachable from an
    earlier one that was already freed and replaced:
    ```
    p = malloc(10);   // firstMalloc
    free(p);
    p = malloc(10);   // a new, unrelated allocation
    use(p);
    ```
    ```kotlin
    dataFlow(startNode = firstMalloc) { it == useOfP }  // true, even though p was reassigned
    ```
    Registering free's summary (`from: freedMemory, to: param0.deref`, see
    cpg_add_function_summary) tells the graph that free() changes what the pointer points
    to, so this becomes `false`.

    Call cpg_list_unmodeled_functions before writing such a query, and register a summary
    with cpg_add_function_summary for every function whose real effect, allocating, freeing,
    copying, or otherwise changing what a pointer points to, your check needs to reflect.

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
    `access: AccessValues` (READ, WRITE or READWRITE). Expressions that mark memory also have
    `memoryAddresses: Set<MemoryAddress>`.

    For a `Reference` (and `PointerDereference`, which extends it), this is the address of the
    *declaration's storage slot* it refers to (`refersTo.memoryAddresses`) -- not the value/pointee
    currently stored there. It is fixed for the life of the declaration, so reassigning the variable
    to a new allocation does not change it: `a.memoryAddresses.any { it in b.memoryAddresses }`
    across two References to the same variable answers "same variable?", not "same allocation?" --
    it stays true even across an intervening `free` and reassignment to a fresh allocation.

    To check whether a pointer still holds a specific allocation (e.g. one freed earlier), follow
    `dataFlow`/`prevFullDFG` from the allocation site instead of comparing `memoryAddresses` on
    References.

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

// TODO: add an example with memcopy
fun Server.addCpgFunctionSummariesTool() {
    this.addTool(
        name = "cpg_function_summaries",
        description =
            """
            Shows the registered data-flow summaries. A summary models what a function whose
            body is not part of the analyzed code (e.g. `free` from the C standard library) influenced the data dereferenced 
            by the arguments. 
            Call this together with cpg_list_unmodeled_functions for any query
            about memory, pointer, or tainted-data state, see cpg_query_api's "Function
            summaries" section for why this matters even when the query never calls the
            function out by name. Register missing summaries with 'cpg_add_function_summary'.
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
        var text =
            """
            Only needed for queries that track data flow through a function whose body is not
            part of the analyzed code (e.g. `free` from the C standard library). Each entry
            models the effect of one function on its arguments, e.g. "paramX.deref" is the
            memory the argument points to. A "from" value that is not a parameter is a
            synthetic `UnknownMemoryValue` node that marks data coming out of the function,
            so that queries can use it as a start node.
            """
                .trimIndent()
        val content = summaries.readText()
        if (content.isBlank()) {
            text += "\n\n(no summaries registered yet)"
        } else {
            text += "\n\n" + content
        }
        CallToolResult(content = listOf(TextContent(text)))
    }
}

fun Server.addCpgListUnmodeledFunctionsTool() {
    this.addTool(
        name = "cpg_list_unmodeled_functions",
        description =
            """
            Lists functions for that the points-to analysis could not compute a real data-flow summary
            (no function body available in the analyzed files), i.e.
            data does not flow through calls to them unless you register a summary with
            cpg_add_function_summary. Check this for any query about memory, pointer, or
            tainted-data state, even if the query's predicate never names the function,
            see cpg_query_api's "Function summaries" section for why an unmodeled call can
            silently break such a query without ever appearing in it.

            For each one this also reports where it was declared, if anywhere: a file path
            outside the files you analyzed suggests an external library function; 
            no path at all means the call could not be resolved to any declaration.
            """
                .trimIndent(),
        inputSchema = ToolSchema(properties = buildJsonObject {}, required = listOf()),
    ) { request ->
        request.runOnCpg { result: TranslationResult, _ ->
            val unmodeled =
                result.functions.filter { func ->
                    func.functionSummary.keys.any {
                        (it as? Literal<*>)?.name?.localName == "dummy"
                    } || func.functionSummary.values.flatten().any { it.isDummy }
                }
            if (unmodeled.isEmpty()) {
                CallToolResult(content = listOf(TextContent("(no unmodeled functions found)")))
            } else {
                CallToolResult(
                    content =
                        unmodeled.map { TextContent(Json.encodeToString(it.toUnmodeledInfo())) }
                )
            }
        }
    }
}

fun Server.addCpgAddFunctionSummaryTool() {
    this.addTool(
        name = "cpg_add_function_summary",
        description =
            """
            Registers a data-flow summary for a function whose body is not part of the analyzed
            code (e.g. a C standard library function). Without a summary, queries cannot rely on
            what the function actually does with its arguments, most importantly writes into the
            memory an argument points to (points-to analysis), but also e.g. a flow from one
            argument into another or into the base object.

            Register one for any function that mutates, invalidates, or copies its arguments
            and whose real behavior a memory-, pointer-, or taint-related query needs to
            reflect, e.g. free invalidating the pointer it's given, strcpy copying one
            argument's data into another, or malloc returning a fresh block of memory.
            Without a summary, the graph has no model of what the function actually does, so
            it cannot represent that a pointer became dangling or that memory changed, a
            query then keeps treating the data as unchanged straight through the call, even
            if the query's predicate never names that function (see cpg_query_api's
            "Function summaries" section for a worked example).

            Step by step:
            1. Starting point: call cpg_list_unmodeled_functions to find functions with no
               reliable data-flow summary that a query start node's value could reach.
            2. Call cpg_function_summaries to see which of them are already registered.
            3. Register the documented effect of each missing function with this tool, one
               call per function, before relying on any dataFlow/executionPath/alwaysFlowsTo
               result that involves memory, pointer, or tainted-data state.

            The entry must be one YAML list item in exactly this format:

            ```yaml
            - functionDeclaration:
                language: CLanguage        # language class name; CLanguage entries also match C++ code
                methodName: someFunction   # fully qualified for methods, e.g. SomeClass::write
                signature: [char*, int]    # optional, only to pick one overload (parameter type names)
              dataFlows:                   # one item per flow the function causes
                - from: param1
                  to: param0.deref
                  dfgType: full            # full: the value flows entirely; partial: only a part of the destination is written
                - from: param0.deref
                  to: return
                  dfgType: full
            ```

            `from` is one of:
            - `paramX`: the argument at index X
            - `paramX.deref`: the memory the argument points to
            - `base`: the object a method is called on
            - `function`: the function itself
            - `NewMemoryAddressX`: a newly allocated memory address, created fresh for every call
              (for allocation functions; X is a number to tell several allocations apart)
            - any other word: a synthetic marker value (`UnknownMemoryValue`) with that name,
              e.g. to mark data the function writes as secret; queries can use it as a start node

            `to` is one of: `paramX`, `paramX.deref`, `paramX.address` (the argument's address),
            `base`, `return`.
            """
                .trimIndent(),
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        putJsonObject("yamlEntry") {
                            put("type", "string")
                            put(
                                "description",
                                "One YAML list item in the format described by the tool.",
                            )
                        }
                    },
                required = listOf("yamlEntry"),
            ),
    ) { request ->
        val yamlEntry = (request.arguments?.get("yamlEntry") as? JsonPrimitive)?.contentOrNull
        if (yamlEntry.isNullOrBlank()) {
            return@addTool CallToolResult(
                content = listOf(TextContent("Missing parameter yamlEntry."))
            )
        }
        val summaries = File(FUNCTION_SUMMARIES_FILE)
        if (!summaries.exists()) {
            return@addTool CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "Function summaries file not found at $FUNCTION_SUMMARIES_FILE. " +
                                "The MCP server must be started from the repository root, " +
                                "otherwise summaries cannot be registered."
                        )
                    )
            )
        }

        val oldText = summaries.readText()
        val entry = "# agent-added ${LocalDate.now()}\n" + yamlEntry.trimIndent().trim() + "\n"
        val newText =
            if (oldText.isBlank()) {
                entry
            } else {
                oldText.trimEnd() + "\n\n" + entry
            }
        summaries.writeText(newText)
        try {
            DFGFunctionSummaries.fromFiles(listOf(summaries))
        } catch (e: Exception) {
            summaries.writeText(oldText)
            return@addTool CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "The entry does not parse and was rejected: ${e.message}. " +
                                "Fix the entry and try again. The registered summaries are unchanged."
                        )
                    )
            )
        }
        CallToolResult(content = listOf(TextContent("Summary registered")))
    }
}
