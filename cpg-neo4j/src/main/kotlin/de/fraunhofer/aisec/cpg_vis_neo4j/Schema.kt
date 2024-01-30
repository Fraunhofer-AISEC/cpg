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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.helpers.neo4j.CpgCompositeConverter
import java.io.File
import java.io.PrintWriter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import org.neo4j.ogm.metadata.ClassInfo
import org.neo4j.ogm.metadata.FieldInfo
import org.neo4j.ogm.metadata.MetaData

class Schema {

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
    private val hierarchy: MutableMap<ClassInfo, Pair<ClassInfo?, List<ClassInfo>>> = mutableMapOf()

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
    private val childrensRels: MutableMap<String, Set<Pair<String, String>>> = mutableMapOf()

    /**
     * Stores a mapping from class information in combination with a relationship name, to the field
     * information that contains the relationship.
     */
    private val relationshipFields: MutableMap<Pair<ClassInfo, String>, FieldInfo> = mutableMapOf()

    fun extractSchema() {
        val meta = MetaData(Node.javaClass.packageName)
        val nodeClassInfo =
            meta.persistentEntities().first { it.underlyingClass == Node::class.java }
        val entities =
            meta.persistentEntities().filter {
                Node::class.java.isAssignableFrom(it.underlyingClass) && !it.isRelationshipEntity
            } // Node to filter for, filter out what is not explicitly a

        entities.forEach { entity ->
            val superC = entity.directSuperclass()

            hierarchy[entity] =
                Pair(
                    if (superC in entities) superC else null,
                    entity
                        .directSubclasses()
                        .filter { it in entities }
                        .distinct() // Filter out duplicates
                )
        }

        // node in neo4j

        entities.forEach { classInfo ->
            val key = meta.schema.findNode(classInfo.neo4jName())
            allRels[classInfo.neo4jName() ?: classInfo.underlyingClass.simpleName] =
                key.relationships().entries.map { Pair(it.key, it.value.type()) }.toSet()
        }

        // Complements the hierarchy and relationship information for abstract classes
        completeSchema(allRels, hierarchy, nodeClassInfo)
        // Searches for all relationships and properties backed by a class field to know which
        // of them are newly defined in the entity class
        entities.forEach { entity ->
            val fields =
                entity.relationshipFields().filter {
                    it.field.declaringClass == entity.underlyingClass
                }
            fields.forEach { relationshipFields.put(Pair(entity, it.name), it) }
            val name = entity.neo4jName() ?: entity.underlyingClass.simpleName
            allRels[name]?.let { relationPair ->
                inherentRels[name] =
                    relationPair.filter { rel -> fields.any { it.name == rel.first } }.toSet()
            }

            entity.propertyFields().forEach { property ->
                val persistedField =
                    if (
                        property.hasCompositeConverter() &&
                            property.compositeConverter is CpgCompositeConverter
                    ) {
                        (property.compositeConverter as CpgCompositeConverter).graphSchema
                    } else {
                        listOf<Pair<String, String>>(
                            Pair(property.field.type.simpleName, property.name)
                        )
                    }

                if (property.field.declaringClass == entity.underlyingClass) {
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
        val entityRoots: MutableList<ClassInfo> =
            hierarchy.filter { it.value.first == null }.map { it.key }.toMutableList()
        entityRoots.forEach {
            inheritedRels[it.neo4jName() ?: it.underlyingClass.simpleName] = mutableSetOf()
        }
        entityRoots.forEach { buildInheritedFields(it) }

        allRels.forEach {
            childrensRels[it.key] =
                it.value
                    .subtract(inheritedRels[it.key] ?: emptySet())
                    .subtract(inherentRels[it.key] ?: emptySet())
        }
        println()
    }

    private fun buildInheritedFields(classInfo: ClassInfo) {
        val fields: MutableSet<Pair<String, String>> = mutableSetOf()
        inherentRels[classInfo.neo4jName() ?: classInfo.underlyingClass.simpleName]?.let {
            fields.addAll(it)
        }
        inheritedRels[classInfo.neo4jName() ?: classInfo.underlyingClass.simpleName]?.let {
            fields.addAll(it)
        }

        hierarchy[classInfo]?.second?.forEach {
            inheritedRels[it.neo4jName() ?: it.underlyingClass.simpleName] = fields
            buildInheritedFields(it)
        }
    }

    private fun completeSchema(
        relCanHave: MutableMap<String, Set<Pair<String, String>>>,
        hierarchy: MutableMap<ClassInfo, Pair<ClassInfo?, List<ClassInfo>>>,
        root: ClassInfo
    ) {
        hierarchy[root]?.second?.forEach { completeSchema(relCanHave, hierarchy, it) }

        hierarchy.keys
            .filter { !relCanHave.contains(it.neo4jName() ?: it.underlyingClass.simpleName) }
            .forEach {
                relCanHave.put(
                    it.neo4jName() ?: it.underlyingClass.simpleName,
                    hierarchy[it]
                        ?.second
                        ?.flatMap { classInfo ->
                            relCanHave[
                                classInfo.neo4jName() ?: classInfo.underlyingClass.simpleName]
                                ?: setOf()
                        }
                        ?.toSet()
                        ?: setOf()
                )
            }
    }

    fun printToFile(fileName: String) {
        val file = File(fileName)
        file.parentFile.mkdirs()
        file.createNewFile()
        file.printWriter().use { out ->
            out.println(header)
            val entityRoots: MutableList<ClassInfo> =
                hierarchy.filter { it.value.first == null }.map { it.key }.toMutableList()
            entityRoots.forEach { printEntities(it, out) }
        }
    }

    /**
     * Prints a section for every entity with a list of labels (e.g. superclasses), a list of
     * relationships, a dropdown with inherited relationships, a list of properties and a dropdown
     * with inherited properties.
     *
     * Generates links between the boxes.
     */
    private fun printEntities(classInfo: ClassInfo, out: PrintWriter) {
        val entityLabel = toLabel(classInfo)

        out.println("## $entityLabel<a id=\"${toAnchorLink("e${entityLabel}")}\"></a>")

        // Todo print entity description
        if (hierarchy[classInfo]?.first != null) {
            out.print("**Labels**:")
            // Todo Print markdown with hierarchy, and not inherent relationships

            hierarchy[classInfo]?.first?.let {
                getHierarchy(it).forEach {
                    out.print(
                        getBoxWithClass(
                            "superclassLabel",
                            "[${toLabel(it)}](#${toAnchorLink("e"+toLabel(it))})"
                        )
                    )
                }
            }
            out.print(
                getBoxWithClass("classLabel", "[${entityLabel}](#${toAnchorLink("e$entityLabel")})")
            )
            out.println()
        }
        if (hierarchy[classInfo]?.second?.isNotEmpty() == true) {
            out.println("### Children")

            hierarchy[classInfo]?.second?.let {
                if (it.isNotEmpty()) {
                    it.forEach { classInfo ->
                        out.print(
                            getBoxWithClass(
                                "child",
                                "[${toLabel(classInfo)}](#${toAnchorLink("e"+toLabel(classInfo))})"
                            )
                        )
                    }
                    out.println()
                }
            }
        }

        if (inherentRels.isNotEmpty() && inheritedRels.isNotEmpty()) {
            out.println("### Relationships")

            removeLabelDuplicates(inherentRels[entityLabel])?.forEach {
                out.print(
                    getBoxWithClass(
                        "relationship",
                        "[${it.second}](#${ toLabel(classInfo) + it.second})"
                    )
                )
            }

            if (inheritedRels[entityLabel]?.isNotEmpty() == true) {
                out.println("<details markdown><summary>Inherited Relationships</summary>")
                out.println()
                removeLabelDuplicates(inheritedRels[entityLabel])?.forEach { inherited ->
                    var current = classInfo
                    var baseClass: ClassInfo? = null
                    while (baseClass == null) {
                        inherentRels[toLabel(current)]?.let { rels ->
                            if (rels.any { it.second == inherited.second }) {
                                baseClass = current
                            }
                        }
                        hierarchy[current]?.first?.let { current = it }
                    }
                    out.println(
                        getBoxWithClass(
                            "inherited-relationship",
                            "[${inherited.second}](#${toConcatName(toLabel(baseClass) + inherited.second)})"
                        )
                    )
                }
                out.println("</details>")
                out.println()
            }

            removeLabelDuplicates(inherentRels[entityLabel])?.forEach {
                printRelationships(classInfo, it, out)
            }
        }

        if (inherentProperties.isNotEmpty() && inheritedProperties.isNotEmpty()) {
            out.println("### Properties")

            removeLabelDuplicates(inherentProperties[entityLabel])?.forEach {
                out.println("${it.second} : ${it.first}")
                out.println()
            }
            if (inheritedProperties[entityLabel]?.isNotEmpty() == true) {
                out.println("<details markdown><summary>Inherited Properties</summary>")
                removeLabelDuplicates(inheritedProperties[entityLabel])?.forEach {
                    out.println("${it.second} : ${it.first}")
                    out.println()
                }
                out.println("</details>")
                out.println()
            }
        }

        hierarchy[classInfo]?.second?.forEach { printEntities(it, out) }
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

    private fun toLabel(classInfo: ClassInfo?): String {
        if (classInfo == null) {
            return "Node"
        }
        return classInfo.neo4jName() ?: classInfo.underlyingClass.simpleName
    }

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

    private fun getHierarchy(classInfo: ClassInfo): MutableList<ClassInfo> {
        val inheritance: MutableList<ClassInfo> = mutableListOf()
        hierarchy[classInfo]?.first?.let { inheritance.addAll(getHierarchy(it)) }
        inheritance.add(classInfo)
        return inheritance
    }

    private fun getTargetInfo(fInfo: FieldInfo): Pair<Boolean, ClassInfo?> {
        val type = fInfo.field.genericType
        relationshipFields
            .map { it.value.field.genericType }
            .filterIsInstance<ParameterizedType>()
            .map { it.rawType }
        val baseClass: Type? = getNestedBaseType(type)
        val multiplicity = getNestedMultiplicity(type)

        var targetClassInfo: ClassInfo? = null
        if (baseClass != null) {
            targetClassInfo =
                hierarchy
                    .map { it.key }
                    .firstOrNull {
                        it.underlyingClass.canonicalName in baseClass.typeName.split(" ")
                    }
        }

        return Pair(multiplicity, targetClassInfo)
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

    private fun printRelationships(
        classInfo: ClassInfo,
        relationshipLabel: Pair<String, String>,
        out: PrintWriter
    ) {
        val fieldInfo: FieldInfo = classInfo.getFieldInfo(relationshipLabel.first)
        val targetInfo = getTargetInfo(fieldInfo)
        val multiplicity = if (targetInfo.first) "*" else "¹"
        out.println(
            "#### ${relationshipLabel.second}<a id=\"${toLabel(classInfo)+relationshipLabel.second}\"></a>"
        )
        openMermaid(out)
        out.println(
            "${toLabel(classInfo)}--\"${relationshipLabel.second}${multiplicity}\"-->${toLabel(classInfo)}${relationshipLabel.second}[<a href='#${toAnchorLink("e" + toLabel(targetInfo.second))}'>${toLabel(targetInfo.second)}</a>]:::outer"
        )
        closeMermaid(out)
    }
}
