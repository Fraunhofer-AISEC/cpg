/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg_vis_neo4j

import com.fasterxml.jackson.databind.ObjectMapper
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.Persistable
import de.fraunhofer.aisec.cpg.persistence.*
import io.github.classgraph.ClassGraph
import java.io.File
import java.io.PrintWriter
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.javaType

class Schema {

    /** Schema definition for node entities that is persisted to the neo4j database */
    data class SchemaNode(
        val name: String,
        val isAbstract: Boolean,
        val labels: Set<String>,
        val childLabels: Set<String>,
        val relationships: Set<SchemaRelationship>,
        val properties: Set<SchemaProperty>,
    )

    /**
     * Definition of a CPG relationship with relationship label, the targeted node and whether the
     * node has multiple or a single relationship edge of the declared type.
     */
    data class SchemaRelationship(
        val label: String,
        val targetNode: String,
        val multiplicity: Char,
        val inherited: Boolean,
    )

    /**
     * Key-value pair defining a node property and if the property was inherited from a parent node
     * entity type or newly introduced in this node entity.
     */
    data class SchemaProperty(val name: String, val valueType: String, val inherited: Boolean)

    /** Output format of the CPG Schema description. */
    enum class Format {
        MARKDOWN,
        JSON,
    }

    private val styling =
        "<style>" +
            ".superclassLabel{background:#dddddd;border-radius:5%;line-height:26px;display:inline-block;text-align:center;margin-bottom:10px;padding-left:10px;padding-right:10px;}" +
            ".classLabel{background:#aabbff;border-radius:5%;line-height:26px;display:inline-block;text-align:center;margin-bottom:10px;padding-left:10px;padding-right:10px;}" +
            ".child{background:#dddddd;border-radius:5%;line-height:26px;display:inline-block;text-align:center;margin-bottom:10px;padding-left:10px;padding-right:10px;}" +
            ".relationship{background:#aaffbb;border-radius:5%;line-height:26px;display:inline-block;text-align:center;margin-bottom:10px;padding-left:10px;padding-right:10px;}" +
            ".inherited-relationship{background:#dddddd;border-radius:5%;line-height:26px;display:inline-block;text-align:center;margin-bottom:10px;padding-left:10px;padding-right:10px;}" +
            "</style>\n"

    private val header =
        styling +
            "\n" +
            "# CPG Schema\n" +
            "This file shows all node labels and relationships between them that are persisted from the in memory CPG to the Neo4j database. " +
            "The specification is generated automatically and always up to date."

    // Contains the class hierarchy with the root Node.
    private val hierarchy:
        MutableMap<KClass<out Node>, Pair<KClass<out Node>?, List<KClass<out Node>>>> =
        mutableMapOf()

    /**
     * Map of entities and the relationships they can have, including relationships defined in
     * subclasses. The pair saves the field name, and the relationship name. Saves
     * MutableMap<EntityName,Set<Pair<FieldName, RelationshipName>>>
     */
    private val allRels: MutableMap<String, Set<Pair<String, String>>> = mutableMapOf()

    /**
     * Relationships newly defined in this specific entity. Saves
     * MutableMap<EntityName,Set<Pair<FieldName, RelationshipName>>>
     */
    private val inherentRels: MutableMap<String, Set<Pair<String, String>>> = mutableMapOf()

    /**
     * Relationships inherited from a parent in the inheritance hierarchy. A node with this label
     * can have this relationship if it is non-nullable. Saves
     * MutableMap<EntityName,Set<Pair<FieldName, RelationshipName>>>
     */
    private val inheritedRels: MutableMap<String, Set<Pair<String, String>>> = mutableMapOf()

    /**
     * Relationships newly defined in this specific entity. Saves
     * MutableMap<EntityName,Set<Pair<Type-Name, PropertyName>>>
     */
    private val inherentProperties: MutableMap<String, MutableSet<Pair<String, String>>> =
        mutableMapOf()

    /**
     * Relationships inherited from a parent in the inheritance hierarchy. A node with this label
     * can have this relationship if it is non-nullable. Saves
     * MutableMap<EntityName,Set<Pair<Type-Name, PropertyName>>>
     */
    private val inheritedProperties: MutableMap<String, MutableSet<Pair<String, String>>> =
        mutableMapOf()

    /**
     * Relationships defined by children in the inheritance hierarchy. A node with this label can
     * have this relationship also has the label of the defining child entity. Saves
     * MutableMap<EntityName,Set<Pair<FieldName, RelationshipName>>>
     */
    private val childrenRels: MutableMap<String, Set<Pair<String, String>>> = mutableMapOf()

    /**
     * Stores a mapping from class information in combination with a relationship name, to the field
     * information that contains the relationship.
     */
    private val relationshipFields:
        MutableMap<Pair<KClass<out Node>, String>, KProperty1<out Persistable, *>> =
        mutableMapOf()

    /**
     * Extracts information on the nodes and edges that can be persisted to the neo4 database using
     * the new persistence schema.
     */
    fun extractSchema() {
        // Use ClassGraph to find all Node subclasses
        val scanResult =
            ClassGraph().acceptPackages(Node::class.java.packageName).enableClassInfo().scan()

        val nodeClass = Node::class
        val allSubclasses = scanResult.getSubclasses(Node::class.java).loadClasses()

        // Include both abstract and concrete classes for hierarchy building
        val allEntities =
            allSubclasses
                .map { it.kotlin }
                .filter { !it.java.isInterface }
                .filterIsInstance<KClass<out Node>>()
                .toMutableList()

        // Also keep track of just the concrete entities for schema output
        val entities =
            allSubclasses
                .map { it.kotlin }
                .filter { !it.java.isInterface && !Modifier.isAbstract(it.java.modifiers) }
                .filterIsInstance<KClass<out Node>>()
                .toMutableList()

        // Add the Node class itself
        allEntities.add(0, nodeClass)
        entities.add(0, nodeClass)

        // Build hierarchy (use all entities including abstract classes)
        allEntities.forEach { entity ->
            val superC =
                entity.superclasses
                    .firstOrNull { it.isSubclassOf(Node::class) && it != Any::class }
                    ?.let { it as? KClass<out Node> }

            val children =
                allEntities.filter { child ->
                    child.superclasses.firstOrNull {
                        it.isSubclassOf(Node::class) && it != Any::class
                    } == entity
                }

            hierarchy[entity] = Pair(superC, children)
        }

        // Extract relationships for each entity
        entities.forEach { entity ->
            val name = entity.qualifiedName ?: entity.simpleName ?: ""
            val relationships = entity.schemaRelationships

            allRels[name] =
                relationships
                    .map { (propertyName, property) ->
                        Pair(propertyName, property.relationshipName)
                    }
                    .toSet()
        }

        // Complements the hierarchy and relationship information for abstract classes
        completeSchema(allRels, hierarchy, nodeClass)

        // Searches for all relationships and properties backed by a class field to know which
        // of them are newly defined in the entity class
        entities.forEach { entity ->
            val name = entity.qualifiedName ?: entity.simpleName ?: ""
            val relationships = entity.schemaRelationships

            // Get relationships declared directly in this class
            val declaredProps = entity.memberProperties.map { it.name }.toSet()
            relationships.forEach { (propName, property) ->
                relationshipFields[Pair(entity, propName)] = property
            }

            allRels[name]?.let { relationPair ->
                inherentRels[name] =
                    relationPair
                        .filter { rel -> relationships.keys.any { it == rel.first } }
                        .toSet()
            }

            // Extracting the key-value pairs that are persisted as node properties
            entity.schemaProperties.forEach { (propertyName, property) ->
                val propertyType = property.returnType.javaType.typeName
                val persistedField = listOf(Pair(propertyType, propertyName))

                // Check if property is declared in this class or inherited
                if (declaredProps.contains(propertyName)) {
                    inherentProperties
                        .computeIfAbsent(name) { mutableSetOf() }
                        .addAll(persistedField)
                } else {
                    inheritedProperties
                        .computeIfAbsent(name) { mutableSetOf() }
                        .addAll(persistedField)
                }
            }
        }

        //  Determines the relationships an entity inherits by propagating the relationships from
        // parent to child
        val entityRoots = hierarchy.filter { it.value.first == null }.map { it.key }.toMutableList()
        entityRoots.forEach {
            inheritedRels[it.qualifiedName ?: it.simpleName ?: ""] = mutableSetOf()
        }
        entityRoots.forEach { extractFieldInformationFromHierarchy(it) }

        allRels.forEach {
            childrenRels[it.key] =
                it.value
                    .subtract(inheritedRels[it.key] ?: emptySet())
                    .subtract(inherentRels[it.key] ?: emptySet())
        }
        println()
    }

    /** Extracts the field information for every entity and relationship. */
    private fun extractFieldInformationFromHierarchy(kClass: KClass<out Node>) {
        val fields: MutableSet<Pair<String, String>> = mutableSetOf()
        val name = kClass.qualifiedName ?: kClass.simpleName ?: ""
        inherentRels[name]?.let { fields.addAll(it) }
        inheritedRels[name]?.let { fields.addAll(it) }

        hierarchy[kClass]?.second?.forEach {
            val childName = it.qualifiedName ?: it.simpleName ?: ""
            inheritedRels[childName] = fields
            extractFieldInformationFromHierarchy(it)
        }
    }

    /**
     * Complements the hierarchy and relationship information for abstract classes to not have empty
     * entities.
     */
    private fun completeSchema(
        relCanHave: MutableMap<String, Set<Pair<String, String>>>,
        hierarchy: MutableMap<KClass<out Node>, Pair<KClass<out Node>?, List<KClass<out Node>>>>,
        root: KClass<out Node>,
    ) {
        hierarchy[root]?.second?.forEach { completeSchema(relCanHave, hierarchy, it) }

        hierarchy.keys
            .filter { !relCanHave.contains(it.qualifiedName ?: it.simpleName ?: "") }
            .forEach { kClass ->
                val name = kClass.qualifiedName ?: kClass.simpleName ?: ""
                relCanHave[name] =
                    hierarchy[kClass]
                        ?.second
                        ?.flatMap { childClass ->
                            relCanHave[childClass.qualifiedName ?: childClass.simpleName ?: ""]
                                ?: setOf()
                        }
                        ?.toSet() ?: setOf()
            }
    }

    /**
     * Depending on the specified output format the Neo4j Schema for the CPG is printed to the
     * specified file.
     */
    fun printToFile(fileName: String, format: Format) {
        val fileExtension = if (Format.MARKDOWN == format) ".md" else ".json"
        val file =
            File(if (fileName.endsWith(fileExtension)) fileName else fileName + fileExtension)
        file.parentFile.mkdirs()
        file.createNewFile()
        file.printWriter().use { out ->
            val entityRoots: MutableList<KClass<out Node>> =
                hierarchy.filter { it.value.first == null }.map { it.key }.toMutableList()
            if (format == Format.MARKDOWN) {
                out.println(header)
                entityRoots.forEach { printEntitiesToMarkdown(it, out) }
            } else {
                entityRoots.forEach {
                    val objectMapper = ObjectMapper()
                    objectMapper.writeValue(out, entitiesToJson(it))
                }
            }
        }
    }

    /**
     * Prints a section for every entity with a list of labels (e.g. superclasses), a list of
     * relationships, a dropdown with inherited relationships, a list of properties and a dropdown
     * with inherited properties.
     *
     * Generates links between the boxes.
     */
    private fun printEntitiesToMarkdown(kClass: KClass<out Node>, out: PrintWriter) {

        val className = kClass.qualifiedName ?: kClass.simpleName ?: ""
        val entityLabel = toLabel(kClass)

        out.println("## $entityLabel<a id=\"${toAnchorLink("e${entityLabel}")}\"></a>")

        if (hierarchy[kClass]?.first != null) {
            out.print("**Labels**:")

            // Use the labels extension property from Common.kt
            kClass.labels.forEach { label ->
                out.print(
                    getBoxWithClass("superclassLabel", "[${label}](#${toAnchorLink("e${label}")})")
                )
            }
            out.println()
        }
        if (hierarchy[kClass]?.second?.isNotEmpty() == true) {
            out.println("### Children")

            hierarchy[kClass]?.second?.let {
                if (it.isNotEmpty()) {
                    it.forEach { childClass ->
                        out.print(
                            getBoxWithClass(
                                "child",
                                "[${toLabel(childClass)}](#${toAnchorLink("e"+toLabel(childClass))})",
                            )
                        )
                    }
                    out.println()
                }
            }
        }

        if (inherentRels.isNotEmpty() && inheritedRels.isNotEmpty()) {
            out.println("### Relationships")

            removeLabelDuplicates(inherentRels[className])?.forEach {
                out.print(
                    getBoxWithClass(
                        "relationship",
                        "[${it.second}](#${ toLabel(kClass) + it.second})",
                    )
                )
            }

            if (inheritedRels[className]?.isNotEmpty() == true) {
                out.println("<div class=\"papers\" markdown>")
                out.println("??? info \"Inherited Relationships\"")
                out.println()
                removeLabelDuplicates(inheritedRels[className])?.forEach { inherited ->
                    var current = kClass
                    var baseClass: KClass<out Node>? = null
                    while (baseClass == null) {
                        val currentName = current.qualifiedName ?: current.simpleName ?: ""
                        inherentRels[currentName]?.let { rels ->
                            if (rels.any { it.second == inherited.second }) {
                                baseClass = current
                            }
                        }
                        hierarchy[current]?.first?.let { current = it }
                    }
                    out.print("    ")
                    out.println(
                        getBoxWithClass(
                            "inherited-relationship",
                            "[${inherited.second}](#${toConcatName(toLabel(baseClass) + inherited.second)})",
                        )
                    )
                }
                out.println("</div>")
                out.println()
            }

            removeLabelDuplicates(inherentRels[className])?.forEach {
                printRelationshipsToMarkdown(kClass, it, out)
            }
        }

        if (inherentProperties.isNotEmpty() && inheritedProperties.isNotEmpty()) {
            out.println("### Properties")

            removeLabelDuplicates(inherentProperties[className])?.forEach {
                out.println("${it.second} : ${it.first}")
                out.println()
            }
            if (inheritedProperties[className]?.isNotEmpty() == true) {
                out.println("<div class=\"papers\" markdown>")
                out.println("??? info \"Inherited Properties\"")
                removeLabelDuplicates(inheritedProperties[className])?.forEach {
                    out.println("    ${it.second} : ${it.first}")
                    out.println()
                }
                out.println("</div>")
                out.println()
            }
        }

        hierarchy[kClass]?.second?.forEach { printEntitiesToMarkdown(it, out) }
    }

    /**
     * Prints a section for every entity with a list of labels (e.g. superclasses), a list of
     * relationships, a dropdown with inherited relationships, a list of properties and a dropdown
     * with inherited properties.
     *
     * Generates links between the boxes.
     */
    private fun entitiesToJson(kClass: KClass<out Node>): MutableList<SchemaNode> {

        val className = kClass.qualifiedName ?: kClass.simpleName ?: ""
        val entityLabel = toLabel(kClass)

        // Use the labels extension property from Common.kt
        val labels: Set<String> = kClass.labels

        val childLabels: MutableSet<String> = mutableSetOf()
        if (hierarchy[kClass]?.second?.isNotEmpty() == true) {

            hierarchy[kClass]?.second?.let {
                if (it.isNotEmpty()) {
                    it.forEach { childClass -> childLabels.add(toLabel(childClass)) }
                }
            }
        }

        val relationships: MutableSet<SchemaRelationship> = mutableSetOf()
        if (inherentRels.isNotEmpty() && inheritedRels.isNotEmpty()) {

            if (inheritedRels[className]?.isNotEmpty() == true) {
                removeLabelDuplicates(inheritedRels[className])?.forEach { inheritedRel ->
                    var current = kClass
                    var baseClass: KClass<out Node>? = null
                    while (baseClass == null) {
                        val currentName = current.qualifiedName ?: current.simpleName ?: ""
                        inherentRels[currentName]?.let { rels ->
                            if (rels.any { it.second == inheritedRel.second }) {
                                baseClass = current
                            }
                        }
                        hierarchy[current]?.first?.let { current = it }
                    }
                    relationships.add(relationshipToJson(baseClass, inheritedRel, true))
                }
            }

            removeLabelDuplicates(inherentRels[className])?.forEach {
                relationships.add(relationshipToJson(kClass, it, false))
            }
        }

        val properties = mutableSetOf<SchemaProperty>()
        if (inherentProperties.isNotEmpty() && inheritedProperties.isNotEmpty()) {

            removeLabelDuplicates(inherentProperties[className])?.forEach {
                properties.add(SchemaProperty(it.second, it.first, false))
            }
            if (inheritedProperties[className]?.isNotEmpty() == true) {
                removeLabelDuplicates(inheritedProperties[className])?.forEach {
                    properties.add(SchemaProperty(it.second, it.first, true))
                }
            }
        }
        val entityNodes =
            hierarchy[kClass]?.second?.flatMap { entitiesToJson(it) }?.toMutableList()
                ?: mutableListOf()
        entityNodes.add(
            0,
            SchemaNode(
                entityLabel,
                Modifier.isAbstract(kClass.java.modifiers),
                labels,
                childLabels,
                relationships,
                properties,
            ),
        )

        return entityNodes
    }

    private fun removeLabelDuplicates(
        list: Set<Pair<String, String>>?
    ): Set<Pair<String, String>>? {
        if (list == null) return null
        return list
            .map { it.second }
            .distinct()
            .map { label -> list.first { it.second == label } }
            .toSet()
    }

    private fun toLabel(kClass: KClass<out Node>?): String {
        if (kClass == null) {
            return "Node"
        }
        return kClass.simpleName ?: kClass.qualifiedName ?: "Node"
    }

    /** Creates a unique markdown anchor to make navigation unambiguous. */
    private fun toAnchorLink(entityName: String): String {
        return toConcatName(entityName).lowercase(Locale.getDefault())
    }

    private fun toConcatName(entityName: String): String {
        return entityName.replace(" ", "-")
    }

    private fun openMermaid(out: PrintWriter) {
        out.println(
            "```mermaid\n" +
                "flowchart LR\n" +
                "  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;" +
                "  classDef special fill:#afa,stroke:#5a5,stroke-dasharray:5 5;"
        )
    }

    private fun closeMermaid(out: PrintWriter) {
        out.println("```")
    }

    /**
     * By specifying a field that constitutes a relationship, this function returns information on
     * the multiplicity and the target class entity.
     */
    private fun getTargetInfo(
        property: KProperty1<out Persistable, *>
    ): Pair<Boolean, KClass<out Node>?> {
        val type = property.returnType.javaType
        val baseClass: Type? = getNestedBaseType(type)
        val multiplicity = getNestedMultiplicity(type)

        var targetClass: KClass<out Node>? = null
        if (baseClass != null) {
            targetClass =
                hierarchy.keys.firstOrNull { it.qualifiedName in baseClass.typeName.split(" ") }
        }

        return Pair(multiplicity, targetClass)
    }

    private fun getNestedBaseType(type: Type): Type? {
        if (type is ParameterizedType) {
            return type.actualTypeArguments.map { getNestedBaseType(it) }.firstOrNull()
        }
        return type
    }

    private fun getNestedMultiplicity(type: Type): Boolean {
        if (type is ParameterizedType) {
            return if (type.rawType.typeName.substringBeforeLast(".") == "java.util") {
                true
            } else {
                type.actualTypeArguments.any { getNestedMultiplicity(it) }
            }
        }
        return false
    }

    private fun getBoxWithClass(cssClass: String, text: String): String {
        return "<span class=\"${cssClass}\">${text}</span>\n"
    }

    private fun printRelationshipsToMarkdown(
        kClass: KClass<out Node>,
        relationshipLabel: Pair<String, String>,
        out: PrintWriter,
    ) {
        val property = relationshipFields[Pair(kClass, relationshipLabel.first)]
        val targetInfo = if (property != null) getTargetInfo(property) else Pair(false, null)
        val multiplicity = if (targetInfo.first) "*" else "¹"
        out.println(
            "#### ${relationshipLabel.second}<a id=\"${toLabel(kClass)+relationshipLabel.second}\"></a>"
        )
        openMermaid(out)
        out.println(
            "${toLabel(kClass)}--\"${relationshipLabel.second}${multiplicity}\"-->${toLabel(kClass)}${relationshipLabel.second}[<a href='#${toAnchorLink("e" + toLabel(targetInfo.second))}'>${toLabel(targetInfo.second)}</a>]:::outer"
        )
        closeMermaid(out)
    }

    private fun relationshipToJson(
        kClass: KClass<out Node>?,
        relationshipLabel: Pair<String, String>,
        inherited: Boolean,
    ): SchemaRelationship {
        val property =
            if (kClass != null) relationshipFields[Pair(kClass, relationshipLabel.first)] else null
        val targetInfo = if (property != null) getTargetInfo(property) else Pair(false, null)
        val multiplicity = if (targetInfo.first) '*' else '1'
        return SchemaRelationship(
            relationshipLabel.second,
            toLabel(kClass),
            multiplicity,
            inherited,
        )
    }
}
