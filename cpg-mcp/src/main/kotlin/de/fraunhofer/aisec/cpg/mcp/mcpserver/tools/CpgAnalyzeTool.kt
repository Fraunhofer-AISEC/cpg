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
@file:Suppress("UNCHECKED_CAST")

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
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.CpgAnalysisResult
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.CpgAnalyzePayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.CpgRunPassPayload
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.PassInfo
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.runOnCpg
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.toNodeInfo
import de.fraunhofer.aisec.cpg.mcp.mcpserver.utils.toObject
import de.fraunhofer.aisec.cpg.mcp.setupTranslationConfiguration
import de.fraunhofer.aisec.cpg.passes.BasicBlockCollectorPass
import de.fraunhofer.aisec.cpg.passes.ComponentPass
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.DynamicInvokeResolver
import de.fraunhofer.aisec.cpg.passes.EOGStarterPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.PrepareSerialization
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.ResolveCallExpressionAmbiguityPass
import de.fraunhofer.aisec.cpg.passes.ResolveMemberExpressionAmbiguityPass
import de.fraunhofer.aisec.cpg.passes.SccPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.TranslationResultPass
import de.fraunhofer.aisec.cpg.passes.TranslationUnitPass
import de.fraunhofer.aisec.cpg.passes.TypeHierarchyResolver
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import de.fraunhofer.aisec.cpg.passes.briefDescription
import de.fraunhofer.aisec.cpg.passes.configuration.PassOrderingHelper
import de.fraunhofer.aisec.cpg.passes.configuration.ReplacePass
import de.fraunhofer.aisec.cpg.passes.consumeTargets
import de.fraunhofer.aisec.cpg.passes.hardDependencies
import de.fraunhofer.aisec.cpg.passes.softDependencies
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import java.io.File
import java.util.IdentityHashMap
import kotlin.String
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations
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
            val analysisResult = runCpgAnalyze(payload, runPasses = true, cleanup = true)
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
            val analysisResult = runCpgAnalyze(payload, runPasses = false, cleanup = false)
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
            fun passToInfo(pass: KClass<out Pass<*>>): PassInfo {
                return PassInfo(
                    fqn = pass.qualifiedName.toString(),
                    description = pass.briefDescription,
                    requiredNodeType =
                        pass.supertypes.fold("") { old, it ->
                            old +
                                when (it.classifier) {
                                    EOGStarterPass::class ->
                                        EOGStarterHolder::class.qualifiedName.toString()
                                    TranslationUnitPass::class ->
                                        TranslationUnitDeclaration::class.qualifiedName.toString()
                                    TranslationResultPass::class ->
                                        TranslationResult::class.qualifiedName.toString()
                                    ComponentPass::class ->
                                        Component::class.qualifiedName.toString()
                                    else -> ""
                                }
                        },
                    dependsOn = pass.hardDependencies.map { it.qualifiedName.toString() },
                    softDependencies = pass.softDependencies.map { it.qualifiedName.toString() },
                )
            }

            fun optionalPassToInfo(passName: String): PassInfo? {
                return try {
                    (Class.forName(passName).kotlin as? KClass<out Pass<*>>)?.let { passToInfo(it) }
                } catch (_: ClassNotFoundException) {
                    null
                }
            }

            val passesList =
                mutableListOf(
                    passToInfo(PrepareSerialization::class),
                    passToInfo(DynamicInvokeResolver::class),
                    passToInfo(ImportResolver::class),
                    passToInfo(SymbolResolver::class),
                    passToInfo(EvaluationOrderGraphPass::class),
                    passToInfo(DFGPass::class),
                    passToInfo(ControlFlowSensitiveDFGPass::class),
                    passToInfo(ControlDependenceGraphPass::class),
                    passToInfo(ProgramDependenceGraphPass::class),
                    passToInfo(TypeResolver::class),
                    passToInfo(TypeHierarchyResolver::class),
                    passToInfo(ResolveMemberExpressionAmbiguityPass::class),
                    passToInfo(ResolveCallExpressionAmbiguityPass::class),
                    passToInfo(SccPass::class),
                    passToInfo(BasicBlockCollectorPass::class),
                )

            // Python-specific passes are added only if Python language is available
            optionalPassToInfo(
                    "de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileConceptPass"
                )
                ?.let { passesList += it }
            optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.PythonAddDeclarationsPass")?.let {
                passesList += it
            }

            // C-specific pass is added only if C language is available
            optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.CXXExtraPass")?.let {
                passesList += it
            }

            // LLVM-specific pass is added only if LLVM language is available
            optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.CompressLLVMPass")?.let {
                passesList += it
            }

            // Java-specific pass is added only if Java language is available
            optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.JavaExternalTypeHierarchyResolver")
                ?.let { passesList += it }
            optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.JavaExtraPass")?.let {
                passesList += it
            }
            optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.JavaImportResolver")?.let {
                passesList += it
            }

            // Go-specific pass is added only if Go language is available
            optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.GoExtraPass")?.let {
                passesList += it
            }
            optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.GoEvaluationOrderGraphPass")?.let {
                passesList += it
            }

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

/** Keeps track of which passes have been run on which nodes to avoid redundant executions. */
val nodeToPass = IdentityHashMap<Node, MutableSet<KClass<out Pass<*>>>>()

/**
 * Registers a tool which runs a [Pass] on a specified [Node] or the closest suitable node(s) for
 * the pass by first searching upwards and then (in case no suitable node was found) downwards the
 * AST. The tool further takes care of dependencies between the passes.
 */
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
                required = listOf("passName", "nodeId"),
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
                        ?: return@runOnCpg CallToolResult(
                            content =
                                listOf(TextContent("Could not find the pass ${payload.passName}."))
                        )
                } catch (_: ClassNotFoundException) {
                    return@runOnCpg CallToolResult(
                        content =
                            listOf(TextContent("Could not find the pass ${payload.passName}."))
                    )
                }

            val nodes = result.nodes.filter { it.id.toString() == payload.nodeId }

            if (nodes.isEmpty())
                return@runOnCpg CallToolResult(
                    content =
                        listOf(
                            TextContent("Could not find any node with the ID ${payload.nodeId}.")
                        )
                )
            val executedPasses = mutableListOf<TextContent>()
            // Check if all required passes have been run before executing this pass.
            val orderingHelper = PassOrderingHelper(listOf(passClass))
            val orderedPassesToExecute =
                try {
                    orderingHelper.order().flatten()
                } catch (_: ConfigurationException) {
                    // There was an exception while ordering the passes (e.g., cyclic dependency).
                    // We just add the requested pass and hope that the AI knows what it is doing.
                    // Note: We do not log this error because it has led to problems with the MCP
                    // server via stdio in the past.
                    listOf(passClass)
                }

            for (node in nodes) {
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
                                        TextContent(
                                            "Cannot run run_pass without translation context."
                                        )
                                    )
                            )
                    }
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
        val messages = mutableListOf<String>()
        for ((language, nodes) in nodesToAnalyze.groupBy { it.language }) {
            // Check if we have to replace the pass for this language
            val actualClass =
                (language::class.findAnnotations<ReplacePass>().find { it.old == passClass }?.with
                    as? KClass<out Pass<T>>) ?: passClass

            consumeTargets(
                cls = actualClass,
                ctx = ctx,
                targets =
                    nodes.filter {
                        nodeToPass.computeIfAbsent(it) { mutableSetOf() }.add(actualClass)
                    },
                executedFrontends = ctx.executedFrontends,
            )
            messages +=
                "Ran pass ${actualClass.simpleName} on nodes ${nodesToAnalyze.map { it.id.toString() }}."
        }
        PassExecutionResult(true, messages.joinToString(","))
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
            val eogStarters = listOfNotNull((node as? EOGStarterHolder) as? Node).toMutableList()
            if (eogStarters.isEmpty())
                eogStarters.addAll(
                    node
                        .firstParentOrNull<Node> { it is EOGStarterHolder && it.prevEOG.isEmpty() }
                        ?.let { listOf(it) }
                        ?: node.allChildrenWithOverlays<Node> {
                            it is EOGStarterHolder && it.prevEOG.isEmpty()
                        }
                )
            runPassForNode<Node>(node, prototype::class, ctx, preList = eogStarters)
        }
    }
}
