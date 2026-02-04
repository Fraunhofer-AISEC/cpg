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
package de.fraunhofer.aisec.cpg.mcp.mcpserver.utils

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.EOGStarterHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.listOverlayClasses
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.mcp.mcpserver.tools.globalAnalysisResult
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
import de.fraunhofer.aisec.cpg.passes.hardDependencies
import de.fraunhofer.aisec.cpg.passes.softDependencies
import de.fraunhofer.aisec.cpg.query.QueryTree
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import java.util.function.BiFunction
import kotlin.reflect.KClass
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

fun Node.toNodeInfo(): NodeInfo {
    return NodeInfo(this)
}

fun <T> QueryTree<T>.toQueryTreeNode(): QueryTreeNode {
    return QueryTreeNode(
        id = this.id.toString(),
        value = this.value.toString(),
        node = this.node?.toNodeInfo(),
        children = this.children.map { it.toQueryTreeNode() },
    )
}

fun Node.toJson() = Json.encodeToString(NodeInfo(this))

fun FunctionDeclaration.toJson() = Json.encodeToString(FunctionInfo(this))

fun FieldDeclaration.toJson() = Json.encodeToString(FieldInfo(this))

fun RecordDeclaration.toJson() = Json.encodeToString(RecordInfo(this))

fun CallExpression.toJson() = Json.encodeToString(CallInfo(this))

fun OverlayNode.toJson() = Json.encodeToString(OverlayInfo(this))

/** Returns all available concrete (non-abstract) concept classes. */
fun getAvailableConcepts(): List<Class<out Concept>> {
    return listOverlayClasses<Concept>()
        .filter { !it.kotlin.isAbstract }
        .filter {
            // TODO: The concept/operation build helper are explicitly checking against underlying
            //  node, which some of our concepts don't have.
            !it.packageName.endsWith(".policy")
        }
}

/** Returns all available concrete (non-abstract) operation classes. */
fun getAvailableOperations(): List<Class<out Operation>> {
    return listOverlayClasses<Operation>()
        .filter { !it.kotlin.isAbstract }
        .filter {
            // TODO: The concept/operation build helper are explicitly checking against underlying
            //  node, which some of our concepts don't have.
            !it.packageName.endsWith(".policy")
        }
}

inline fun <reified T> JsonObject.toObject() = Json.decodeFromString<T>(Json.encodeToString(this))

fun CallToolRequest.runOnCpg(
    query: BiFunction<TranslationResult, CallToolRequest, CallToolResult>
): CallToolResult {
    return try {
        val result =
            globalAnalysisResult
                ?: return CallToolResult(
                    content =
                        listOf(
                            TextContent(
                                "No analysis result available. Please analyze your code first using cpg_analyze."
                            )
                        )
                )
        query.apply(result, this)
    } catch (e: Exception) {
        CallToolResult(
            content =
                listOf(TextContent("Error executing query: ${e.message ?: e::class.simpleName}"))
        )
    }
}

fun ReadResourceRequest.runOnCpg(
    uri: String,
    query: BiFunction<TranslationResult, ReadResourceRequest, ReadResourceResult>,
): ReadResourceResult {
    return try {
        val result =
            globalAnalysisResult
                ?: return ReadResourceResult(
                    contents =
                        listOf(
                            TextResourceContents(
                                "No analysis result available. Please analyze your code first using cpg_analyze.",
                                uri = uri,
                            )
                        )
                )
        query.apply(result, this)
    } catch (e: Exception) {
        ReadResourceResult(
            contents =
                listOf(
                    TextResourceContents(
                        "Error executing query: ${e.message ?: e::class.simpleName}",
                        uri = uri,
                    )
                )
        )
    }
}

fun listPasses(): List<PassInfo> {
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
                            ComponentPass::class -> Component::class.qualifiedName.toString()
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
    optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileConceptPass")
        ?.let { passesList += it }
    optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.PythonAddDeclarationsPass")?.let {
        passesList += it
    }

    // C-specific pass is added only if C language is available
    optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.CXXExtraPass")?.let { passesList += it }

    // LLVM-specific pass is added only if LLVM language is available
    optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.CompressLLVMPass")?.let { passesList += it }

    // Java-specific pass is added only if Java language is available
    optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.JavaExternalTypeHierarchyResolver")?.let {
        passesList += it
    }
    optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.JavaExtraPass")?.let { passesList += it }
    optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.JavaImportResolver")?.let {
        passesList += it
    }

    // Go-specific pass is added only if Go language is available
    optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.GoExtraPass")?.let { passesList += it }
    optionalPassToInfo("de.fraunhofer.aisec.cpg.passes.GoEvaluationOrderGraphPass")?.let {
        passesList += it
    }
    return passesList
}
