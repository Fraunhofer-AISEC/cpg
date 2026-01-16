/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.EOGStarterHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildrenWithOverlays
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.graph.variables
import de.fraunhofer.aisec.cpg.mcp.mcpserver.cpgDescription
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.CpgRunPassPayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.PassInfo
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.runOnCpg
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toNodeInfo
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.utils.toObject
import de.fraunhofer.aisec.cpg.mcp.setupTranslationConfiguration
import de.fraunhofer.aisec.cpg.passes.CXXExtraPass
import de.fraunhofer.aisec.cpg.passes.ComponentPass
import de.fraunhofer.aisec.cpg.passes.CompressLLVMPass
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.DynamicInvokeResolver
import de.fraunhofer.aisec.cpg.passes.EOGStarterPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.GoEvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.GoExtraPass
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import de.fraunhofer.aisec.cpg.passes.JavaExternalTypeHierarchyResolver
import de.fraunhofer.aisec.cpg.passes.JavaExtraPass
import de.fraunhofer.aisec.cpg.passes.JavaImportResolver
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.PrepareSerialization
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.ResolveCallExpressionAmbiguityPass
import de.fraunhofer.aisec.cpg.passes.ResolveMemberExpressionAmbiguityPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.TranslationResultPass
import de.fraunhofer.aisec.cpg.passes.TranslationUnitPass
import de.fraunhofer.aisec.cpg.passes.TypeHierarchyResolver
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileConceptPass
import de.fraunhofer.aisec.cpg.passes.configuration.PassOrderingHelper
import de.fraunhofer.aisec.cpg.passes.consumeTargets
import de.fraunhofer.aisec.cpg.passes.hardDependencies
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import java.io.File
import java.util.IdentityHashMap
import kotlin.String
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.typeOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

var globalAnalysisResult: TranslationResult? = null

var ctx: TranslationContext? = null

val toolDescription =
    """
        Analyze source code using CPG (Code Property Graph).
        
        $cpgDescription
        
        This tool parses source code and creates a comprehensive graph representation 
        containing all nodes, functions, variables, and call expressions.
        
        Example usage:
        - "Analyze this code: print('hello')"
        - "Analyze this uploaded file"
    """
        .trimIndent()

val inputSchema =
    ToolSchema(
        properties =
            buildJsonObject {
                putJsonObject("content") {
                    put("type", "string")
                    put("description", "Source code content to analyze")
                }
                putJsonObject("extension") {
                    put("type", "string")
                    put(
                        "description",
                        "File extension for language detection (e.g., 'py', 'java', 'cpp')",
                    )
                }
            },
        required = listOf(),
    )

fun Server.addCpgAnalyzeTool() {
    this.addTool(name = "cpg_analyze", description = toolDescription, inputSchema = inputSchema) {
        request ->
        try {
            val payload = request.arguments?.toObject<CpgAnalyzePayload>()
            val analysisResult = runCpgAnalyze(payload, true, true)
            val jsonResult = Json.encodeToString(analysisResult)
            CallToolResult(content = listOf(TextContent(jsonResult)))
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent("Error: ${e.message ?: e::class.simpleName}"))
            )
        }
    }
}

/**
 * Translate the given [payload] to the CPG. If there has been another analysis before, it resets
 * the context and cleans up all frontends.
 *
 * If [runPasses] is true, all default passes will be run, otherwise no pass will be run. If
 * [cleanup] is true, we clean up the [TypeManager] memory after analysis.
 */
fun runCpgAnalyze(
    payload: CpgAnalyzePayload?,
    runPasses: Boolean,
    cleanup: Boolean,
): CpgAnalysisResult {
    val file =
        when {
            payload?.content != null -> {
                val extension =
                    if (payload.extension != null) {
                        if (payload.extension.startsWith(".")) payload.extension
                        else ".${payload.extension}"
                    } else {
                        throw IllegalArgumentException(
                            "Extension is required when providing content"
                        )
                    }

                val tempFile = File.createTempFile("cpg_analysis", extension)
                tempFile.writeText(payload.content)
                tempFile.deleteOnExit()
                tempFile
            }

            else -> throw IllegalArgumentException("Must provide content")
        }

    val config =
        setupTranslationConfiguration(
            topLevel = file,
            files = listOf(file.absolutePath),
            includePaths = emptyList(),
            runPasses = runPasses,
        )
    config.disableCleanup = !cleanup

    if (ctx != null) {
        ctx?.executedFrontends?.forEach { frontend ->
            // If there has been another analysis before, reset the context and clean up all
            // frontends.
            frontend.cleanup()
        }

        ctx = null
    }

    val analyzer = TranslationManager.builder().config(config).build()
    ctx = TranslationContext(config)
    val result =
        ctx?.let { ctx -> analyzer.analyze(ctx).get() }
            ?: throw IllegalStateException("Translation context is not initialized")

    // Store the result globally
    globalAnalysisResult = result

    val allNodes = result.nodes
    val functions = result.functions
    val variables = result.variables
    val callExpressions = result.calls

    val nodeInfos = allNodes.map { node: Node -> node.toNodeInfo() }

    return CpgAnalysisResult(
        totalNodes = allNodes.size,
        functions = functions.size,
        variables = variables.size,
        callExpressions = callExpressions.size,
        nodes = nodeInfos,
    )
}

/**
 * From here, we add a simplified version of the CPG analyze tool that only translates the source
 * code into the CPG AST without running any additional passes. These passes can be run in
 * subsequent tools.
 */

/** Translate source code into the AST of the CPG (Code Property Graph). */
fun Server.addCpgTranslate() {
    this.addTool(
        name = "cpg_translate",
        description =
            """
        Translates the source code into the AST of the CPG (Code Property Graph). This serves as a basis for subsequent passes and analyses.
        
        $cpgDescription
        
        This tool parses source code and creates a comprehensive graph representation 
        containing all nodes, functions, variables, and call expressions.
        
        Example usage:
        - "Analyze this code: print('hello')"
        - "Analyze this uploaded file"
    """
                .trimIndent(),
        inputSchema = inputSchema,
    ) { request ->
        try {
            val payload = request.arguments?.toObject<CpgAnalyzePayload>()
            val analysisResult = runCpgAnalyze(payload, false, false)
            val jsonResult = Json.encodeToString(analysisResult)
            CallToolResult(content = listOf(TextContent(jsonResult)))
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent("Error: ${e.message ?: e::class.simpleName}"))
            )
        }
    }
}

/** Provide a list of all passes that can be applied to the CPG. */
fun Server.addListPasses() {
    this.addTool(
        name = "cpg_list_passes",
        description =
            """Provides a list of all available passes that can be applied to the CPG. It also lists dependencies and what kind of node the pass expects."""
                .trimIndent(),
        inputSchema = ToolSchema(properties = buildJsonObject {}, required = listOf()),
    ) { _ ->
        try {

            fun passToInfo(pass: KClass<out Pass<*>>, description: String): PassInfo {
                return PassInfo(
                    fqn = pass.qualifiedName.toString(),
                    description = description,
                    requiredNodeType =
                        pass.supertypes.fold("") { old, it ->
                            old +
                                if (it.classifier == EOGStarterPass::class)
                                    EOGStarterHolder::class.qualifiedName.toString()
                                else if (it.classifier == TranslationUnitPass::class)
                                    TranslationUnitDeclaration::class.qualifiedName.toString()
                                else if (it.classifier == TranslationResultPass::class)
                                    TranslationResult::class.qualifiedName.toString()
                                else if (it.classifier == ComponentPass::class)
                                    Component::class.qualifiedName.toString()
                                else ""
                        },
                    dependsOn = pass.hardDependencies.map { it.qualifiedName.toString() },
                )
            }

            val passesList =
                mutableListOf(
                    passToInfo(
                        PrepareSerialization::class,
                        "Prepares the CPG for serialization by cleaning up unnecessary data and optimizing the graph structure.",
                    ),
                    passToInfo(
                        DynamicInvokeResolver::class,
                        "Resolves dynamic method invocations in the CPG, enhancing the accuracy of call relationships within the graph.",
                    ),
                    passToInfo(
                        ImportResolver::class,
                        "Resolves import statements in the code, linking imported entities to their definitions within the CPG.",
                    ),
                    passToInfo(
                        SymbolResolver::class,
                        "Resolves symbols in the CPG, linking variable and function usages (i.e., refersTo and calledBy/invokes edges) to their respective declarations. Generates the call graph.",
                    ),
                    passToInfo(
                        EvaluationOrderGraphPass::class,
                        "Adds EOG edges to the graph. These represent the execution order of statements or expressions and is similar to a fine-grained version of a control flow graph.",
                    ),
                    passToInfo(
                        DFGPass::class,
                        "Adds DFG edges to the graph. These are a controlflow-insensitive data flow representation.",
                    ),
                    passToInfo(
                        ControlFlowSensitiveDFGPass::class,
                        "Enhances the Data Flow Graph (DFG) by considering control flow information, leading to more accurate (i.e., flow-sensitive) data flow representation.",
                    ),
                    passToInfo(
                        ControlDependenceGraphPass::class,
                        "Adds CDG edges to the graph. These represent control dependence graph and thus show if executing code depends on a condition of a control-flow controlling statement",
                    ),
                    passToInfo(
                        ProgramDependenceGraphPass::class,
                        "Combines the Data Flow Graph (DFG) and Control Dependence Graph (CDG) into a Program Dependence Graph (PDG), providing a comprehensive view of both data and control dependencies within the program.",
                    ),
                    passToInfo(
                        TypeResolver::class,
                        "Resolves and infers types for nodes in the CPG, enhancing the semantic understanding of the code represented in the graph.",
                    ),
                    passToInfo(
                        TypeHierarchyResolver::class,
                        "Builds the type hierarchy within the CPG, establishing relationships between types such as inheritance and interface implementation.",
                    ),
                    passToInfo(
                        ResolveMemberExpressionAmbiguityPass::class,
                        "A translation unit pass that resolves ambiguities in member expressions within a translation unit. This pass checks whether the base or member name in a member expression refers to an import and, if so, replaces the member expression with a reference using the fully qualified name.",
                    ),
                    passToInfo(
                        ResolveCallExpressionAmbiguityPass::class,
                        "Tries to identify and resolve ambiguous CallExpressions that could also be CastExpressions or ConstructExpressions. The initial translation cannot distinguish between these expression types in some languages (having the trait HasCallExpressionAmbiguity in the CPG and try to fix these issues by this pass.",
                    ),
                )

            // Python-specific pass is added only if Python language is available
            passesList +=
                passToInfo(
                    PythonFileConceptPass::class,
                    "Applies Python-specific file concepts to the CPG, enriching the graph with additional semantic information relevant to Python files.",
                )

            // C-specific pass is added only if C language is available
            passesList +=
                passToInfo(
                    CXXExtraPass::class,
                    "This Pass executes certain C++ specific conversions on initializers, that are only possible once we know all the types.",
                )

            // LLVM-specific pass is added only if LLVM language is available
            passesList +=
                passToInfo(
                    CompressLLVMPass::class,
                    "Re-organizes some nodes in the CPG if they originate from LLVM IR code, removing redundant nodes and edges to optimize the graph structure for analysis.",
                )

            // Java-specific pass is added only if Java language is available
            passesList +=
                passToInfo(
                    JavaExternalTypeHierarchyResolver::class,
                    "Adds some java types and their hierarchy information that are not part of the analyzed code (e.g., from the standard library) to the CPG's type hierarchy.",
                )
            passesList +=
                passToInfo(
                    JavaExtraPass::class,
                    "This pass is responsible for handling Java-specific cases that are not covered by the general CPG logic. For example, Java has static member access, which is not modeled as a member expression, but as a reference with an FQN. This pass will convert such member expressions to references with FQNs.",
                )
            passesList += passToInfo(JavaImportResolver::class, "Pass that resolves Java imports.")

            // Go-specific pass is added only if Go language is available
            passesList +=
                passToInfo(
                    GoExtraPass::class,
                    "This pass takes care of several things that we need to clean up, once all translation units are successfully parsed, but before any of the remaining CPG passes, such as call resolving occurs. Adds Type Listeners for Key/Value Variables in For-Each Statements, Infers NamespaceDeclarations for Import Packages, Declares Variables in Short Assignments, Adjust Names of Keys in Key Value Expressions to FQN, and Adds Methods of Embedded Structs to the Record's Scope.",
                )
            passesList +=
                passToInfo(
                    GoEvaluationOrderGraphPass::class,
                    "This pass contains fine-grained improvements to the EOG for the go language.",
                )

            CallToolResult(
                content = passesList.map { passInfo -> TextContent(Json.encodeToString(passInfo)) }
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent("Error: ${e.message ?: e::class.simpleName}"))
            )
        }
    }
}

/**
 * Keeps track of which passes have been run on which nodes to avoid redundant executions.
 */
val nodeToPass = IdentityHashMap<Node, MutableSet<KClass<out Pass<*>>>>()

/** Runs a specified [de.fraunhofer.aisec.cpg.passes.Pass] on a specified [Node]. */
fun Server.addRunPass() {
    this.addTool(
        name = "cpg_run_pass",
        description =
            """Runs a given Pass on a specified Node. If the given node does not meet the type of node the pass operates on, the tool looks for the next matching node. It also triggers passes that the specified pass depends on, if they have not been run yet on the given node."""
                .trimIndent(),
        inputSchema =
            ToolSchema(
                properties =
                    buildJsonObject {
                        putJsonObject("passName") {
                            put("type", "string")
                            put("description", "The FQN of the pass to run")
                        }
                        putJsonObject("nodeId") {
                            put("type", "string")
                            put("description", "The ID of the node on which the pass should be ran")
                        }
                    },
                required = listOf("pass_name", "nodeId"),
            ),
    ) { request ->
        request.runOnCpg { result: TranslationResult, request: CallToolRequest ->
            val payload =
                request.arguments?.toObject<CpgRunPassPayload>()
                    ?: return@runOnCpg CallToolResult(
                        content =
                            listOf(TextContent("Invalid or missing payload for run_pass tool."))
                    )
            val passClass =
                try {
                    (Class.forName(payload.passName).kotlin as? KClass<out Pass<*>>)
                } catch (_: ClassNotFoundException) {
                    null
                }
                    ?: return@runOnCpg CallToolResult(
                        content =
                            listOf(TextContent("Could not find the pass ${payload.passName}."))
                    )

            val node =
                result.nodes.find { it.id.toString() == payload.nodeId }
                    ?: return@runOnCpg CallToolResult(
                        content =
                            listOf(
                                TextContent(
                                    "Could not find any node with the ID ${payload.nodeId}."
                                )
                            )
                    )
            val executedPasses = mutableListOf<TextContent>()

            // Check if all required passes have been run before executing this pass.
            val orderingHelper = PassOrderingHelper(listOf(passClass))
            val orderedPassesToExecute = orderingHelper.order().flatten()
            for (passToExecute in orderedPassesToExecute) {
                // Check if pass has already been executed for the respective node
                if (passToExecute !in nodeToPass.computeIfAbsent(node) { mutableSetOf() }) {
                    // Execute the pass for the node
                    ctx?.let { ctx ->
                        val passResult = runPassForNode(node, passToExecute, ctx)
                        if (passResult.success) {
                            executedPasses.add(TextContent(passResult.message))
                        } else {
                            // Return if there was an error during pass execution
                            return@runOnCpg CallToolResult(
                                content =
                                    listOf(
                                        TextContent(passResult.message),
                                        *executedPasses.toTypedArray(),
                                    )
                            )
                        }
                        // Mark pass as executed
                        nodeToPass[node]?.add(passToExecute)
                    }
                        ?: return@runOnCpg CallToolResult(
                            content =
                                listOf(
                                    TextContent("Cannot run run_pass without translation context.")
                                )
                        )
                }
            }

            CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "Successfully ran ${payload.passName} on node ${payload.nodeId}."
                        ),
                        *executedPasses.toTypedArray(),
                    )
            )
        }
    }
}

data class PassExecutionResult(val success: Boolean, val message: String)

/**
 * Internal helper function that runs the [Pass] of class [passClass] on the given [node] within the
 * provided [TranslationContext] [ctx]. As a [Pass] has to work on one or multiple nodes, one can
 * provide the list of nodes where the pass should start using the [preList]. If [preList] is not
 * provided, it collects nodes of type [T] starting from the given [node] (either the node itself,
 * its first parent of type [T], or all children of type [T]).
 */
inline fun <reified T : Node> runPassForNode(
    node: Node,
    passClass: KClass<out Pass<T>>,
    ctx: TranslationContext,
    preList: List<T>? = null,
): PassExecutionResult {
    val nodesToAnalyze = preList?.toMutableList() ?: listOfNotNull(node as? T).toMutableList()
    if (nodesToAnalyze.isEmpty())
        nodesToAnalyze.addAll(
            node.firstParentOrNull<T>()?.let { listOf(it) } ?: node.allChildrenWithOverlays<T>()
        )
    return if (nodesToAnalyze.isNotEmpty()) {
        consumeTargets(
            cls = passClass,
            ctx = ctx,
            targets =
                nodesToAnalyze.filter {
                    nodeToPass.computeIfAbsent(it) { mutableSetOf() }.add(passClass)
                },
            executedFrontends = ctx.executedFrontends,
        )
        PassExecutionResult(
            true,
            "Ran pass ${passClass.simpleName} on nodes ${nodesToAnalyze.map { it.id.toString() }}.",
        )
    } else {
        PassExecutionResult(
            false,
            "Expected node of type ${typeOf<T>()} for pass ${passClass.simpleName}, but got ${node.javaClass.simpleName}",
        )
    }
}

/**
 * Runs the specified [passClass] on the given [node] within the provided [ctx]
 * (TranslationContext). To determine the appropriate targets for the pass, it checks the type of
 * the pass and collects nodes accordingly:
 * - For [TranslationResultPass], it looks for the nearest [TranslationResult] parent or the node
 *   itself if it is a [TranslationResult].
 * - For [ComponentPass], it checks if the node is a [Component] or searches for the nearest
 *   [Component] parent or all children that are [Component]s.
 * - For [TranslationUnitPass], it checks if the node is a [TranslationUnitDeclaration] or searches
 *   for the nearest [TranslationUnitDeclaration] parent or all children that are
 *   [TranslationUnitDeclaration]s.
 * - For [EOGStarterPass], it checks if the node is an [EOGStarterHolder] or searches for the
 *   nearest [EOGStarterHolder] parent with no previous EOG or all children that are
 *   [EOGStarterHolder]s with no previous EOG.
 *
 * Returns a [CallToolResult] if there was an error during execution, otherwise returns null.
 */
fun runPassForNode(
    node: Node,
    passClass: KClass<out Pass<*>>,
    ctx: TranslationContext,
): PassExecutionResult {
    val prototype =
        passClass.primaryConstructor?.call(ctx)
            ?: return PassExecutionResult(
                false,
                "Could not create the pass ${passClass.simpleName}.",
            )

    return when (prototype) {
        is TranslationResultPass -> {
            runPassForNode<TranslationResult>(node, prototype::class, ctx)
        }
        is ComponentPass -> {
            runPassForNode<Component>(node, prototype::class, ctx)
        }
        is TranslationUnitPass -> {
            runPassForNode<TranslationUnitDeclaration>(node, prototype::class, ctx)
        }
        is EOGStarterPass -> {
            val eogStarters =
                listOfNotNull<Node>((node as? EOGStarterHolder) as? Node).toMutableList()
            if (eogStarters.isEmpty())
                eogStarters.addAll(
                    node
                        .firstParentOrNull<Node>({ it is EOGStarterHolder && it.prevEOG.isEmpty() })
                        ?.let { listOf(it) }
                        ?: node.allChildrenWithOverlays<Node> {
                            it is EOGStarterHolder && it.prevEOG.isEmpty()
                        }
                )
            runPassForNode<Node>(node, prototype::class, ctx, preList = eogStarters)
        }
    }
}
