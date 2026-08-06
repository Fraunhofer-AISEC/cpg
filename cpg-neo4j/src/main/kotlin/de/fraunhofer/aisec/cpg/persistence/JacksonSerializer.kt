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
package de.fraunhofer.aisec.cpg.persistence

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.core.StreamWriteConstraints
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.JsonNode as JacksonNode
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer
import com.fasterxml.jackson.databind.deser.SettableBeanProperty
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.module.SimpleKeyDeserializers
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.module.SimpleSerializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildrenWithOverlays
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeCollection
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeSingletonList
import de.fraunhofer.aisec.cpg.graph.edges.edges
import de.fraunhofer.aisec.cpg.graph.parseName
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.persistence.converters.LocationConverter
import de.fraunhofer.aisec.cpg.persistence.converters.NameConverter
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.IOException
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.uuid.Uuid

/**
 * CPG graphs can be nested very deeply (think of long call/EOG chains), so we raise Jackson's
 * default read and write nesting limits well beyond what a "normal" JSON document would need.
 */
private const val MAX_NESTING_DEPTH = 10_000

class NameKeySerializer : JsonSerializer<Name>() {
    override fun serialize(value: Name, gen: JsonGenerator, serializers: SerializerProvider) {
        // Convert key object to string — customize your format here
        gen.writeFieldName(value.delimiter + " " + value.toString())
    }
}

class NameKeyDeserializer : KeyDeserializer() {
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any {
        val fqnName = key.substringAfter(" ")
        val delimiter = key.substringBefore(" ")
        // Parse string back into MyKey
        return parseName(fqnName, delimiter)
    }
}

/**
 * Serializes a [Name] *value* as an object holding its full name and delimiter, in exactly the
 * shape that the existing [NameConverter] reads back (see [readModule]). This is needed because a
 * [Name]'s defining [Name.fullName] is a *private* field: serialized as a plain bean it would be
 * omitted, and [NameConverter] — which reconstructs the name from `fullName` + the delimiter —
 * would produce an empty name. Since [Node.hashCode] (and thus [Node.id]) depends on the name, that
 * would break the round trip.
 */
class NameSerializer : JsonSerializer<Name>() {
    override fun serialize(value: Name, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField(NameConverter.FIELD_FULL_NAME, value.toString())
        gen.writeStringField(NameConverter.FIELD_NAME_DELIMITER, value.delimiter)
        gen.writeEndObject()
    }
}

/**
 * Maps a serialized object id (Jackson's `@id`) back to the [Node] instance that was built for it.
 * The two-phase reader (see [deserializeFromJson]) fills this in phase 1 (one entry per node in the
 * `nodes` array) and consults it in phase 2 to resolve every reference (edge endpoints, edge
 * containers) to the already-built instance.
 */
class NodeRegistry {
    private val nodes = LinkedHashMap<String, Node>()

    fun register(id: String, node: Node) {
        nodes[id] = node
    }

    fun lookup(id: String): Node? = nodes[id]

    val all: Collection<Node>
        get() = nodes.values
}

/**
 * Resolves a [Node] used as a map key from its serialized `@id`. If the id is unknown (e.g. a
 * regenerable cache that referenced a node we did not restore), it keeps deserialization alive by
 * returning the raw id string instead of failing.
 */
class NodeKeyDeserializer(private val registry: NodeRegistry) : KeyDeserializer() {
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any =
        registry.lookup(key) ?: key
}

/**
 * A last-resort [KeyDeserializer] for map-key types that we do not model explicitly. It simply
 * hands the raw key string back.
 *
 * We need this because Jackson requires a key deserializer for *every* non-[String] map-key type
 * (it resolves them when it builds the map deserializer, even for empty maps) and hard-fails
 * otherwise. The maps that trigger this are regenerable caches such as
 * [de.fraunhofer.aisec.cpg.ScopeManager]'s `symbolLookupCache`, whose keys are serialized via their
 * (irreversible) `toString()` anyway. Returning the raw string keeps deserialization alive; the
 * resulting cache entries never match a real lookup and are simply recomputed on demand.
 */
class TolerantKeyDeserializer : KeyDeserializer() {
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any = key
}

class NodeKeyDeserializers(private val registry: NodeRegistry) : SimpleKeyDeserializers() {
    override fun findKeyDeserializer(
        type: JavaType,
        config: DeserializationConfig,
        beanDesc: BeanDescription?,
    ): KeyDeserializer {
        val raw = type.rawClass
        return when {
            Node::class.java.isAssignableFrom(raw) -> NodeKeyDeserializer(registry)
            Name::class.java.isAssignableFrom(raw) -> NameKeyDeserializer()
            Pair::class.java.isAssignableFrom(raw) -> PairKeyDeserializer()
            KClass::class.java.isAssignableFrom(raw) -> KClassKeyDeserializer()
            // Any other (unmodeled) key type: keep deserialization alive instead of hard-failing.
            else -> TolerantKeyDeserializer()
        }
    }
}

class KClassSerializer : StdSerializer<Any>(Any::class.java) {
    override fun serialize(value: Any, gen: JsonGenerator, provider: SerializerProvider) {
        if (value is KClass<*>) {
            gen.writeString(value.qualifiedName ?: value.simpleName ?: "unknown")
        } else {
            throw IllegalArgumentException("Unexpected type: ${value.javaClass}")
        }
    }

    override fun serializeWithType(
        value: Any,
        gen: JsonGenerator,
        serializers: SerializerProvider,
        typeSer: TypeSerializer,
    ) {
        serialize(value, gen, serializers)
        // super.serializeWithType(value, gen, serializers, typeSer)
    }
}

class KClassDeserializer : StdDeserializer<KClass<*>>(KClass::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): KClass<*> {
        val className = p.valueAsString
        return Class.forName(className).kotlin
    }
}

class KClassKeySerializer : JsonSerializer<KClass<*>>() {
    @Throws(IOException::class)
    override fun serialize(value: KClass<*>, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeFieldName(value.qualifiedName)
    }
}

class KClassKeyDeserializer : KeyDeserializer() {
    @Throws(IOException::class)
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any {

        val kclass = Class.forName(key).kotlin

        return kclass
    }
}

// KClass<*>
class PairKeySerializer : JsonSerializer<Pair<*, *>>() {
    @Throws(IOException::class)
    override fun serialize(value: Pair<*, *>, gen: JsonGenerator, serializers: SerializerProvider) {
        // Convert the pair to a string key (ensure uniqueness and reversibility)
        val first = (value.first as? KClass<*>)?.qualifiedName ?: value.first.toString()
        val second = (value.second as? KClass<*>)?.qualifiedName ?: value.second.toString()
        val keyStr = "${first}|${second}"
        gen.writeFieldName(keyStr)
    }
}

class PairKeyDeserializer : KeyDeserializer() {
    @Throws(IOException::class)
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any {
        // Expected format: "com.package.ClassA|com.package.ClassB"
        val parts = key.split("|")
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid key format: $key")
        }

        val kclass1 = Class.forName(parts[0]).kotlin
        val kclass2 = Class.forName(parts[1]).kotlin

        return Pair(kclass1, kclass2)
    }
}

/**
 * Mixin applied to [Node] on the read path only. It neutralizes the polymorphic type info and the
 * object-id handling that [Node] declares for the write path.
 *
 * Phase 1 of [deserializeFromJson] builds each node from its own JSON subtree, whose concrete type
 * we already know (we read the `@class` property ourselves and hand Jackson the exact class).
 * References to *other* nodes appear as bare id strings. We therefore must stop Jackson from (a)
 * demanding a `@class` type id on those bare-id references and (b) trying to resolve the ids inline
 * — both of which fail on the graph's forward references. The references are linked manually in
 * phase 2 instead.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@JsonIdentityInfo(generator = ObjectIdGenerators.None::class, property = "@id")
private interface NodeReadMixin

/**
 * Phase 1 deserializer wrapper for every [Node] subtype. A full node *definition* (a JSON object)
 * is built by the [delegate]; a bare-id *reference* (a JSON string) is left unresolved (returned as
 * `null`) and linked later in phase 2. The same wrapper handles both the root node we deserialize
 * explicitly (an object) and any nested node-typed property (a reference), based purely on the
 * token it is positioned on.
 */
class SkeletonNodeDeserializer(private val delegate: JsonDeserializer<*>) :
    JsonDeserializer<Any?>(), ResolvableDeserializer {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Any? {
        if (p.currentToken?.isScalarValue == true) {
            // A reference to another node; skip it now and link it in phase 2.
            return null
        }
        return delegate.deserialize(p, ctxt)
    }

    override fun resolve(ctxt: DeserializationContext) {
        (delegate as? ResolvableDeserializer)?.resolve(ctxt)
    }

    override fun isCachable(): Boolean = true
}

/**
 * Whether this property holds references to other nodes and must therefore be skipped in phase 1 of
 * [deserializeFromJson] (the graph connectivity is rebuilt from the flat `edges` array in phase 2).
 * This covers the edge containers themselves (their name contains `Edge`, mirroring
 * [SubgraphWalker.getAllEdgeFields]), scalar node-typed references (e.g. `language`, `scope` —
 * deferring these avoids feeding the `null` phase-1 skeleton into a non-null setter), and any
 * collection, array or map of nodes — most notably the unwrapped list views of edge containers
 * (e.g. `components`), whose backing collections reject the `null` skeleton references.
 */
private fun SettableBeanProperty.referencesNodes(): Boolean =
    "Edge" in name ||
        Node::class.java.isAssignableFrom(type.rawClass) ||
        type.contentType?.let { Node::class.java.isAssignableFrom(it.rawClass) } == true

class UuidDeserializer : StdDeserializer<Uuid>(Uuid::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Uuid {
        val uuid = p.codec.readTree<JacksonNode>(p)
        return Uuid.fromLongs(
            uuid.get("mostSignificantBits").asLong(),
            uuid.get("leastSignificantBits").asLong(),
        ) // or Uuid(text), depending on version
    }
}

/**
 * Explicitly deactivating the default behavior to only store references that is annotated in the
 * class header for [Node] and [Edge]. This leads to the nodes and edges in the set being explicitly
 * stored for the first time and a flattening of the graph.
 */
data class CPG(
    @param:JsonIdentityReference(alwaysAsId = false) val nodes: Set<Node> = emptySet(),
    @param:JsonIdentityReference(alwaysAsId = false) val edges: Set<Edge<*>> = emptySet(),
)

/**
 * The write-path module: custom serializers for the complex value and map-key types that Jackson
 * cannot handle out of the box (see [readModule] for their matching deserializers, which keeps the
 * two paths in parity).
 */
private fun writeModule(): SimpleModule =
    SimpleModule().apply {
        setSerializers(
            object : SimpleSerializers() {
                    override fun findSerializer(
                        config: SerializationConfig,
                        type: JavaType,
                        beanDesc: BeanDescription?,
                    ): JsonSerializer<*>? =
                        if (KClass::class.java.isAssignableFrom(type.rawClass)) {
                            KClassSerializer()
                        } else {
                            super.findSerializer(config, type, beanDesc)
                        }
                }
                .apply {
                    addSerializer(
                        PhysicalLocation::class.java,
                        LocationConverter.LocationSerializer(),
                    )
                }
        )

        addSerializer(Name::class.java, NameSerializer())

        addKeySerializer(Name::class.java, NameKeySerializer())
        addKeySerializer(Pair::class.java, PairKeySerializer())
        addKeySerializer(KClass::class.java, KClassKeySerializer())
    }

/**
 * The read-path module used by phase 1 of [deserializeFromJson]. It mirrors [writeModule] with the
 * matching deserializers and, in addition:
 * - wraps every [Node] subtype's deserializer in a [SkeletonNodeDeserializer], so that nested node
 *   *references* (bare ids) are left unresolved and only full node *definitions* are built;
 * - drops every node-reference property (edge containers, their unwrapped list views such as
 *   `components`, and any other collection/map of nodes), so those keep their empty,
 *   constructor-initialized value; the graph's connectivity is rebuilt from the flat `edges` array
 *   in phase 2. Scalar node-typed references (e.g. `scope`) are kept but resolve to `null` in phase
 *   1, and are re-linked in phase 2.
 */
private fun readModule(registry: NodeRegistry): SimpleModule =
    SimpleModule().apply {
        setDeserializerModifier(
            object : BeanDeserializerModifier() {
                override fun modifyDeserializer(
                    config: DeserializationConfig,
                    desc: BeanDescription,
                    deserializer: JsonDeserializer<*>,
                ): JsonDeserializer<*> =
                    if (Node::class.java.isAssignableFrom(desc.beanClass)) {
                        SkeletonNodeDeserializer(deserializer)
                    } else {
                        deserializer
                    }

                override fun updateBuilder(
                    config: DeserializationConfig,
                    desc: BeanDescription,
                    builder: BeanDeserializerBuilder,
                ): BeanDeserializerBuilder {
                    if (Node::class.java.isAssignableFrom(desc.beanClass)) {
                        builder.properties
                            .asSequence()
                            .filter { it.referencesNodes() }
                            .map { it.fullName }
                            // Snapshot before mutating: `removeProperty` mutates
                            // `builder.properties`.
                            .toList()
                            .forEach(builder::removeProperty)
                    }
                    return builder
                }
            }
        )

        addDeserializer(Name::class.java, NameConverter())
        addDeserializer(KClass::class.java, KClassDeserializer())
        addDeserializer(Uuid::class.java, UuidDeserializer())
        addDeserializer(PhysicalLocation::class.java, LocationConverter.LocationDeserializer())

        setKeyDeserializers(NodeKeyDeserializers(registry))
    }

/**
 * A shared [JsonFactory] that raises Jackson's default read and write nesting limits (see
 * [MAX_NESTING_DEPTH]), so that deeply nested CPG graphs can be (de)serialized.
 */
private fun cpgJsonFactory(): JsonFactory =
    JsonFactory.builder()
        .streamWriteConstraints(
            StreamWriteConstraints.builder().maxNestingDepth(MAX_NESTING_DEPTH).build()
        )
        .streamReadConstraints(
            StreamReadConstraints.builder().maxNestingDepth(MAX_NESTING_DEPTH).build()
        )
        .build()

fun serializeToJson(translationResult: TranslationResult): String {
    // The write path uses the Kotlin module and getter-based visibility so that the
    // `@get:`-targeted
    // Jackson annotations on [Node] (e.g. `@get:JsonIgnore`, `@get:JsonMerge`) take effect.
    val objectMapper =
        ObjectMapper(cpgJsonFactory())
            .findAndRegisterModules()
            .registerKotlinModule()
            .registerModule(writeModule())

    val allNodes = translationResult.allChildrenWithOverlays<Node>().toMutableSet()
    val allEdges = mutableSetOf<Edge<*>>()
    var toExplore = allNodes.toSet()

    while (toExplore.isNotEmpty()) {
        // Only explore the current frontier, not the whole set of already-known nodes. Exploring
        // `allNodes` on every iteration would re-walk every node again and again, turning the graph
        // collection into O(n^2). Each node is discovered once, so exploring just the frontier
        // visits every node (and therefore every edge) exactly once.
        val (exploredNodes, exploredEdges) =
            toExplore
                .map { it.explore() }
                .let { pairOfLists ->
                    pairOfLists.flatMap { it.first }.filter { it !in allNodes } to
                        pairOfLists.flatMap { it.second }
                }
        allEdges.addAll(exploredEdges)
        allNodes.addAll(exploredNodes)
        toExplore = exploredNodes.toSet()
    }

    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(CPG(allNodes, allEdges))
}

fun Node.explore(): Pair<Set<Node>, Set<Edge<*>>> {
    val edges = this.edges<Edge<*>>().toSet()
    val nodes = edges.flatMap { setOf(it.start, it.end) }.toMutableSet()

    val kClass = this::class as KClass<Node>
    kClass.memberProperties.forEach { prop ->
        prop.isAccessible = true
        val value =
            try {
                prop.get(this)
            } catch (_: Exception) {
                null
            }
        val toUnwrapp = mutableListOf(value)
        while (toUnwrapp.isNotEmpty()) {
            val current = toUnwrapp.removeFirst()
            when (current) {
                is Node -> nodes.add(current)
                is Iterable<*> -> current.forEach { toUnwrapp.add(it) }
                is Array<*> -> current.forEach { toUnwrapp.add(it) }
                is Map<*, *> -> {
                    current.keys.forEach { toUnwrapp.add(it) }
                    current.values.forEach { toUnwrapp.add(it) }
                }
            }
        }
    }

    return Pair(nodes, edges)
}

/** The serialized Jackson object id (the `@id` property) of this JSON node. */
private val JacksonNode.id: String
    get() = get("@id").asText()

/**
 * Deserializes a [CPG] graph previously produced by [serializeToJson]. Because the graph is
 * serialized flattened — every node appears once as a full object in the top-level `nodes` array
 * and every reference to it is a bare object id — and Jackson's native `@JsonIdentityInfo`
 * resolution cannot handle these forward references, we link the graph manually in two phases:
 * 1. Build one instance per entry in the `nodes` array. Node-typed references are left unresolved
 *    (null) and edge containers are left empty (see [readModule]); only scalar properties (name,
 *    location, ...) are populated. Each instance is registered under its serialized `@id`.
 * 2. Rebuild the connectivity from the flat `edges` array: for every node, for each of its edge
 *    containers, resolve the referenced edges' endpoints to the registered instances and re-add
 *    them, which recreates the edges (and, for AST edges, restores the `astParent` links).
 */
fun deserializeFromJson(json: String): TranslationResult {
    val registry = NodeRegistry()

    // The read path uses field-based visibility and deliberately does *not* register the Kotlin
    // module: kotlin-reflect cannot introspect the Java collection subclasses (e.g. `IdentitySet`)
    // that back many node properties and would otherwise fail with "Cannot infer visibility for
    // inherited fun clone()".
    val objectMapper =
        ObjectMapper(cpgJsonFactory())
            // Bind strictly to backing fields: disable getter/is-getter/setter auto-detection so a
            // property is bound to at most its field. This avoids collisions where two Kotlin
            // members
            // serialize under the same name (e.g. `Field.isDefinition: Boolean` and
            // `Field.definition: Field` both map to `"definition"`) and sidesteps non-null Kotlin
            // setters by assigning fields directly.
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            // The write path serializes via getters and therefore also emits computed, getter-only
            // properties (e.g. `currentScope`). Those have no backing field, so the field-based
            // read
            // path does not know them. They are derived and regenerable, so we simply skip them
            // instead of failing.
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .addMixIn(Node::class.java, NodeReadMixin::class.java)
            .registerModule(readModule(registry))
            // Two kinds of values that Jackson cannot instantiate on the read path are skipped
            // (nulled out) here rather than aborting the whole read:
            // - node references statically typed by a graph *interface* (e.g.
            //   `Reference.resolutionHelper: HasType?`), serialized as a bare id — not caught by
            // the
            //   field-type filter in [readModule] and re-linked in phase 2;
            // - the non-node "object web" (scopes, translation context, ...) that the writer emits
            //   inline as full objects but whose classes have no no-arg constructor. Restoring that
            //   web is out of scope for this graph-only round-trip.
            .addHandler(
                object : DeserializationProblemHandler() {
                    override fun handleMissingInstantiator(
                        ctxt: DeserializationContext,
                        instClass: Class<*>,
                        valueInsta: com.fasterxml.jackson.databind.deser.ValueInstantiator?,
                        p: JsonParser,
                        msg: String,
                    ): Any? {
                        // Consume the (scalar or object) value we are skipping and null it out.
                        if (p.currentToken?.isStructStart == true) {
                            p.skipChildren()
                        }
                        return null
                    }
                }
            )

    val tree = objectMapper.readTree(json)

    // Phase 1: build a skeleton instance for every node and register it under its serialized id.
    // A few `Node` subtypes are not default-constructible (notably the [Scope] hierarchy, whose
    // constructor requires its `astNode`). These are never AST children — they belong to the
    // non-node "object web" that this graph-only round-trip does not restore — so we skip any node
    // type without a no-arg constructor.
    val nodesJson =
        tree.get("nodes") ?: error("Serialized graph has no `nodes` array to deserialize.")
    var skippedNodes = 0
    for (nodeJson in nodesJson) {
        val id = nodeJson.id
        val node =
            try {
                val type = Class.forName(nodeJson.get("@class").asText())
                // Node types without a no-arg constructor (notably the [Scope] hierarchy, whose
                // constructor requires its `astNode`) belong to the non-node "object web" that this
                // graph-only round-trip does not restore, so we skip them.
                if (type.declaredConstructors.none { it.parameterCount == 0 }) null
                else objectMapper.treeToValue(nodeJson, type) as? Node
            } catch (e: Exception) {
                // A single unbuildable node (e.g. its class is not on the reader's classpath
                // because
                // the producing language frontend is absent) must not abort the whole graph read.
                log.warn("Skipping node {} during deserialization: {}", id, e.message)
                null
            }
        if (node == null) {
            skippedNodes++
            continue
        }
        registry.register(id, node)
    }
    if (skippedNodes > 0) {
        log.info(
            "Restored {} of {} nodes; {} were skipped (non-node object web or unbuildable types). " +
                "Edges touching a skipped node are dropped.",
            registry.all.size,
            nodesJson.size(),
            skippedNodes,
        )
    }

    // Phase 2: rebuild the graph's connectivity from the flat `edges` array and the per-node edge
    // containers, restoring each edge's own scalar properties (e.g. the EOG `branch`).
    val edgesById = readEdgesById(tree)
    for (nodeJson in nodesJson) {
        val node = registry.lookup(nodeJson.id) ?: continue
        relinkEdges(node, nodeJson, edgesById, registry, objectMapper)
    }

    // Phase 3: re-assert the persisted names. Relinking edges fires graph observers (e.g. type
    // propagation) that recompute derived names — a `MemberAccess` recomputes its name as
    // `<base type>.<field>` whenever its base's type changes, so wiring up the base while its type
    // is still unrestored would leave an `UNKNOWN.<field>` name behind. The serialized name is
    // authoritative and feeds [Node.id], so we restore it once connectivity is complete.
    for (nodeJson in nodesJson) {
        val node = registry.lookup(nodeJson.id) ?: continue
        val nameJson = nodeJson.get("name") ?: continue
        node.name = objectMapper.treeToValue(nameJson, Name::class.java)
    }

    return registry.all.filterIsInstance<TranslationResult>().firstOrNull()
        ?: error("Deserialized graph contained no TranslationResult to return.")
}

/** Every serialized edge (from the flat `edges` array), indexed by its `@id`. */
private fun readEdgesById(tree: JacksonNode): Map<String, JacksonNode> =
    tree.get("edges")?.associateBy { it.id }.orEmpty()

/**
 * Describes one edge to (re-)create while relinking: the id of its target ("end") node and, when
 * available, the serialized edge object [edgeJson] holding the edge's own properties. For unwrapped
 * views of private containers (see [edgeDescriptorsFor]) only the target id is known, so [edgeJson]
 * is `null` and the edge is recreated with default properties.
 */
private class EdgeDescriptor(val endId: String, val edgeJson: JacksonNode?)

/**
 * Rebuilds the outgoing edges of a single [node] from its serialized [nodeJson]. For every outgoing
 * edge-container field (mirroring [SubgraphWalker.getAllEdgeFields]) we resolve the edges to
 * recreate (see [edgeDescriptorsFor]) and re-add each target to the container, which recreates the
 * correctly-typed edge, then copy the edge's own scalar properties onto it (see
 * [applyEdgeProperties]). Only outgoing containers are processed: their incoming mirror containers
 * are populated automatically when the outgoing side is re-added.
 */
private fun relinkEdges(
    node: Node,
    nodeJson: JacksonNode,
    edgesById: Map<String, JacksonNode>,
    registry: NodeRegistry,
    objectMapper: ObjectMapper,
) {
    // Guards against creating the same serialized edge twice, should it ever be reachable via two
    // fields on the same node (e.g. a private backing field and a public alias).
    val processedEdgeIds = mutableSetOf<String>()

    for (field in SubgraphWalker.getAllEdgeFields(node.javaClass)) {
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val container = field.get(node) as? EdgeCollection<Node, *> ?: continue
        if (!container.outgoing) continue

        for (descriptor in edgeDescriptorsFor(field.name, nodeJson, edgesById)) {
            val edgeId = descriptor.edgeJson?.id
            if (edgeId != null && !processedEdgeIds.add(edgeId)) continue
            val target = registry.lookup(descriptor.endId) ?: continue
            when (container) {
                is EdgeSingletonList<*, *, *> -> {
                    // An outgoing singleton edge is pre-populated at construction with a
                    // placeholder
                    // (typically a `ProblemExpression`), so we cannot `add` to it; `resetTo`
                    // replaces that placeholder with the real target.
                    @Suppress("UNCHECKED_CAST")
                    (container as EdgeSingletonList<Node, Node?, Edge<Node>>).resetTo(target)
                    descriptor.edgeJson?.let {
                        applyEdgeProperties(container.firstOrNull(), it, objectMapper)
                    }
                }
                else ->
                    if (descriptor.edgeJson != null) {
                        // Recreate the edge and set its own properties in the creation `builder`.
                        // Distinct serialized edges keep their own identity here, so parallel edges
                        // between the same pair of nodes are preserved rather than collapsed.
                        @Suppress("UNCHECKED_CAST")
                        (container as EdgeCollection<Node, Edge<Node>>).add(target) {
                            applyEdgeProperties(this, descriptor.edgeJson, objectMapper)
                        }
                    } else if (container.none { it.end === target }) {
                        // Unwrapped view without an edge object: the target may already be linked
                        // via an edge's mirror container, so add it only once.
                        container.add(target)
                    }
            }
        }
    }
}

/**
 * The serialized keys that describe an edge's *structure* rather than a plain scalar property, plus
 * the complex node-referencing properties we do not (yet) restore. [applyEdgeProperties] skips
 * these so that only simple, settable scalar properties are copied.
 */
private val EDGE_STRUCTURAL_KEYS =
    setOf(
        "@id",
        "start",
        "end",
        "labels",
        "assumptions",
        "overlaying",
        "granularity",
        "callingContext",
    )

/**
 * Copies the scalar properties of a recreated [edge] from its serialized [edgeJson] (e.g. the EOG
 * `branch`/`unreachable`/`scc`, or an edge `index`/`name`). Structural keys and the complex
 * node-referencing properties (`granularity`, `callingContext`) are skipped, as are immutable
 * (`val`) properties that are fixed at construction — these keep their default. Every assignment is
 * best-effort: a property we cannot convert or set is skipped rather than failing the whole read.
 */
private fun applyEdgeProperties(edge: Edge<*>?, edgeJson: JacksonNode, objectMapper: ObjectMapper) {
    if (edge == null) return
    val fieldsByName = allFieldsByName(edge.javaClass)
    edgeJson.fields().forEach { (key, valueNode) ->
        if (key in EDGE_STRUCTURAL_KEYS || valueNode.isNull) return@forEach
        val field = fieldsByName[key] ?: return@forEach
        if (Modifier.isFinal(field.modifiers)) return@forEach
        try {
            field.isAccessible = true
            field.set(edge, objectMapper.treeToValue(valueNode, field.type))
        } catch (_: Exception) {
            // Best-effort: skip any property we cannot convert or assign.
        }
    }
}

/** All declared fields of [type] and its supertypes, indexed by name (subclass wins on clashes). */
private fun allFieldsByName(type: Class<*>): Map<String, Field> {
    val fields = HashMap<String, Field>()
    var current: Class<*>? = type
    while (current != null && current != Any::class.java) {
        current.declaredFields.forEach { fields.putIfAbsent(it.name, it) }
        current = current.superclass
    }
    return fields
}

/**
 * Resolves the edges to recreate for the container named [fieldName], from the serialized
 * [nodeJson]. There are two ways a container shows up in the JSON:
 * 1. A *public* edge container is serialized under its own field name as an array of edge ids; we
 *    look each up in [edgesById] to recover both its target and its properties.
 * 2. A *private* edge container (e.g. [de.fraunhofer.aisec.cpg.graph.expressions.Call]'s
 *    `calleeEdge`) is not serialized at all — but its public unwrapped view is (e.g. `callee`), and
 *    holds the target node ids directly (no edge object). We derive that view's name by stripping
 *    the `Edge`/`Edges` suffix (re-pluralizing for collections, mirroring the `unwrapping` naming
 *    convention).
 */
private fun edgeDescriptorsFor(
    fieldName: String,
    nodeJson: JacksonNode,
    edgesById: Map<String, JacksonNode>,
): List<EdgeDescriptor> {
    // Case 1: the edge container itself (its serialized name may drop a leading underscore).
    val edgeIds = nodeJson.get(fieldName) ?: nodeJson.get(fieldName.removePrefix("_"))
    if (edgeIds != null && edgeIds.isArray) {
        return edgeIds.mapNotNull { idNode ->
            val edgeJson = edgesById[idNode.asText()] ?: return@mapNotNull null
            val endId = edgeJson.get("end")?.asText() ?: return@mapNotNull null
            EdgeDescriptor(endId, edgeJson)
        }
    }

    // Case 2: fall back to the unwrapped node-reference view of a non-serialized (private)
    // container.
    val unwrappedName =
        when {
            fieldName.endsWith("Edges") -> fieldName.removeSuffix("Edges") + "s"
            fieldName.endsWith("Edge") -> fieldName.removeSuffix("Edge")
            else -> return emptyList()
        }
    val unwrapped = nodeJson.get(unwrappedName) ?: return emptyList()
    return when {
        unwrapped.isArray -> unwrapped.map { EdgeDescriptor(it.asText(), null) }
        unwrapped.isTextual -> listOf(EdgeDescriptor(unwrapped.asText(), null))
        else -> emptyList()
    }
}
