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
package de.fraunhofer.aisec.cpg.serialization

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.component
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.translationUnit
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import kotlin.uuid.Uuid
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * Custom serializer for [Uuid] to convert it to and from a string representation. This is used for
 * serialization and deserialization of [Uuid] in the JSON data classes.
 */
object UuidSerializer : KSerializer<Uuid> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Uuid", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uuid) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uuid {
        return Uuid.parse(decoder.decodeString())
    }
}

/** JSON data class for an [Edge]. */
@Serializable
data class EdgeJSON(
    var label: String,
    @Serializable(with = UuidSerializer::class) var start: Uuid,
    @Serializable(with = UuidSerializer::class) var end: Uuid,
)

/**
 * JSON data class for a [Node].
 *
 * @property isInferred Whether the node was inferred by the analysis instead of being parsed from
 *   source code, see [Node.isInferred].
 * @property isImplicit Whether the node is not written in the source code, see [Node.isImplicit].
 * @property typeName For a node with a type: the name of its type, see [Type.typeName].
 */
@Serializable
data class NodeJSON(
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    val type: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val code: String,
    val name: String,
    //        val astChildren: List<NodeJSON>,
    val prevDFG: List<EdgeJSON> = emptyList(),
    val nextDFG: List<EdgeJSON> = emptyList(),
    @Serializable(with = UuidSerializer::class) val translationUnitId: Uuid? = null,
    val componentName: String? = null,
    val fileName: String? = null,
    val isInferred: Boolean = false,
    val isImplicit: Boolean = false,
    val typeName: String? = null,
)

/**
 * Converts a [Node] into its JSON representation.
 *
 * @param noEdges Whether to omit the DFG edges of the node.
 * @param maxCodeLength If set, [NodeJSON.code] only contains the first line of the code (trimmed),
 *   shortened to this many characters followed by `…`. This keeps the JSON small when nodes are
 *   only referenced, e.g. in lists, where the full code of a function would be too much.
 */
fun Node.toJSON(noEdges: Boolean = false, maxCodeLength: Int? = null): NodeJSON {
    return NodeJSON(
        id = this.id,
        type = this.javaClass.simpleName,
        startLine = location?.region?.startLine ?: -1,
        startColumn = location?.region?.startColumn ?: -1,
        endLine = location?.region?.endLine ?: -1,
        endColumn = location?.region?.endColumn ?: -1,
        code =
            (this.code ?: "").let { if (maxCodeLength != null) it.snippet(maxCodeLength) else it },
        name = this.name.toString(),
        fileName =
            this.location?.artifactLocation?.uri?.let { uri ->
                // Extract filename from URI
                val path = uri.toString()
                path.substringAfterLast('/').substringAfterLast('\\')
            },
        //                astChildren =
        //                    if (noEdges) emptyList()
        //                    else (this as? AstNode)?.astChildren?.map { it.toJSON() } ?:
        // emptyList(),
        prevDFG = if (noEdges) emptyList() else this.prevDFGEdges.map { it.toJSON() },
        nextDFG = if (noEdges) emptyList() else this.nextDFGEdges.map { it.toJSON() },
        translationUnitId = this.translationUnit?.id,
        componentName = this.component?.name?.toString(),
        isInferred = this.isInferred,
        isImplicit = this.isImplicit,
        typeName = (this as? HasType)?.type?.typeName,
    )
}

/** Upper bound for the code of a node that is only referenced, see [toRefJSON]. */
private const val MAX_SNIPPET_LENGTH = 120

/**
 * This node as [NodeJSON] for a reference to it, e.g. in a list: without its DFG edges and with
 * only the first line of its code, so that hub nodes and large functions do not blow up the
 * response.
 */
fun Node.toRefJSON(): NodeJSON = toJSON(noEdges = true, maxCodeLength = MAX_SNIPPET_LENGTH)

/** The first line of this code, trimmed and shortened to at most [maxLength] characters. */
private fun String.snippet(maxLength: Int): String {
    val firstLine = lineSequence().firstOrNull()?.trim().orEmpty()
    return if (firstLine.length > maxLength) firstLine.take(maxLength) + "…" else firstLine
}

/** Converts an [Edge] into its JSON representation. */
fun Edge<*>.toJSON(): EdgeJSON {
    return EdgeJSON(
        label = this.labels.firstOrNull() ?: "",
        start = this.start.id,
        end = this.end.id,
    )
}

/** A compact reference to a [Node] with its location, e.g. for the results of the MCP tools. */
@Serializable
data class NodeInfo(
    val nodeId: String,
    val name: String,
    val code: String?,
    val type: String?,
    val fileName: String?,
    val startLine: Int?,
    val endLine: Int?,
) {
    constructor(
        node: Node
    ) : this(
        nodeId = node.id.toString(),
        name = node.name.localName,
        code = node.code,
        type = node::class.simpleName,
        fileName = node.location?.artifactLocation?.fileName,
        startLine = node.location?.region?.startLine,
        endLine = node.location?.region?.endLine,
    )
}

/**
 * Paths through the graph, e.g. the dataflows from or to a node. Tools that find paths return this
 * format, so that the LLM gets their structure and the console can show them as a path or a graph.
 *
 * @property kind What the paths follow, e.g. `dataflow`.
 * @property description What the paths show, e.g. "where the value of `key` comes from".
 * @property start The node the paths were searched from.
 * @property paths The paths, each from its first to its last node in the direction of the flow
 *   (from the source to the sink, also when the search went backwards).
 * @property truncated Whether paths are missing because of the limits of the search.
 */
@Serializable
data class NodePaths(
    val kind: String,
    val description: String,
    val start: NodeInfo,
    val paths: List<List<NodeInfo>>,
    val truncated: Boolean,
)

/**
 * A concept or operation and the node it is attached to.
 *
 * @property concept For an operation: the concept it belongs to. `null` for a concept.
 */
@Serializable
data class OverlayJSON(
    val overlay: NodeJSON,
    val underlyingNode: NodeJSON?,
    val concept: NodeJSON?,
)

/** This node as [OverlayJSON], or `null` if it is neither a [Concept] nor an [Operation]. */
fun Node.toOverlayJSON(): OverlayJSON? {
    if (this !is Concept && this !is Operation) return null
    return OverlayJSON(
        overlay = toRefJSON(),
        underlyingNode = this.underlyingNode?.toRefJSON(),
        concept = (this as? Operation)?.concept?.toRefJSON(),
    )
}

/** How well the analysis knows what a [Call] does. */
enum class CallStatus {
    /**
     * At least one target has a body, i.e., is part of the analysed code. For a declaration, the
     * body of its definition counts, e.g. of a function declared in a header and defined in another
     * file.
     */
    RESOLVED,
    /**
     * The targets are known, but none of them has a body, e.g. functions that are only declared in
     * a header or a library, or that were inferred by the analysis.
     */
    EXTERNAL,
    /** No target could be determined at all. */
    UNRESOLVED,
}

/**
 * How well the analysis knows the targets of this call, see [CallStatus].
 *
 * A single target with a body is enough for [CallStatus.RESOLVED], since the targets of a virtual
 * call can include declarations without a body (e.g. a pure virtual method) next to its overrides.
 */
val Call.status: CallStatus
    get() =
        when {
            invokes.isEmpty() -> CallStatus.UNRESOLVED
            invokes.none { (it.definition ?: it).hasBody() } -> CallStatus.EXTERNAL
            else -> CallStatus.RESOLVED
        }
