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
import java.io.File
import java.io.PrintWriter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import org.neo4j.ogm.metadata.ClassInfo
import org.neo4j.ogm.metadata.FieldInfo
import org.neo4j.ogm.metadata.MetaData

class Schema {

    val lightGreen = "#aaffbb"
    val lightBlue = "#aabbff"
    val lightGray = "#dddddd"

    val header =
        "# CPG Schema\n" +
            "This file shows all node labels and relationships between them that are persisted from the in memory CPG to the Neo4j database. " +
            "The specification is generated automatically and always up to date."

    // Contains the class hierarchy with the root Node.
    val hierarchy: MutableMap<ClassInfo, Pair<ClassInfo?, List<ClassInfo>>> = mutableMapOf()

    /**
     * Set of fields that are translated into a relationship. The pair saves the field name, and the
     * relationship name. Saves MutableMap<EntityName,Set<Pair<FieldName, RelationshipName>>>
     */
    val relCanHave: MutableMap<String, Set<Pair<String, String>>> = mutableMapOf()

    /**
     * Relationships newly defined in this specific entity. Saves
     * MutableMap<EntityName,Set<Pair<FieldName, RelationshipName>>>
     */
    val inherentFields: MutableMap<String, Set<Pair<String, String>>> = mutableMapOf()

    /**
     * Relationships inherited from a parent in the inheritance hierarchy. A node with this label
     * can have this relationship if it is non-nullable. Saves
     * MutableMap<EntityName,Set<Pair<FieldName, RelationshipName>>>
     */
    val inheritedFields: MutableMap<String, Set<Pair<String, String>>> = mutableMapOf()
    /**
     * Relationships defined by children in the inheritance hierarchy. A node with this label can
     * have this relationship also has the label of the defining child entity. Saves
     * MutableMap<EntityName,Set<Pair<FieldName, RelationshipName>>>
     */
    val childrenFields: MutableMap<String, Set<Pair<String, String>>> = mutableMapOf()

    val relationshipFields: MutableMap<Pair<ClassInfo, String>, FieldInfo> = mutableMapOf()

    fun extractSchema() {
        val meta: MetaData = MetaData(Node.javaClass.packageName)
        val nodeClassInfo =
            meta
                .persistentEntities()
                .filter { it.underlyingClass == Node::class.java }
                .firstOrNull()!!
        val entities =
            meta.persistentEntities().filter {
                Node::class.java.isAssignableFrom(it.underlyingClass) && !it.isRelationshipEntity
            } // Node to filter for, filter out what is not explicitly a

        entities.forEach {
            if (it in entities) {
                val superC = it.directSuperclass()

                hierarchy.put(
                    it,
                    Pair(
                        if (superC in entities) superC else null,
                        it.directSubclasses()
                            .filter { it in entities }
                            .distinct() // Filter out duplicates
                    )
                )
            }
        }

        // node in neo4j

        entities.forEach {
            val key = meta.schema.findNode(it.neo4jName())
            relCanHave.put(
                it.neo4jName() ?: it.underlyingClass.simpleName,
                key.relationships().entries.map { Pair(it.key, it.value.type()) }.toSet(),
            )
        }

        // Complements the hierarchy and relationship information for abstract classes
        completeSchema(relCanHave, hierarchy, nodeClassInfo)
        // Searches for all relationships backed by a class field to know which relationships are
        // newly defined in the
        // entity class
        entities.forEach {
            val entity = it
            val fields =
                entity.relationshipFields().filter {
                    it.field.declaringClass == entity.underlyingClass
                }
            fields.forEach { relationshipFields.put(Pair(entity, it.name), it) }
            val name = it.neo4jName() ?: it.underlyingClass.simpleName
            relCanHave[name]?.let {
                inherentFields.put(
                    name,
                    it.filter {
                            val rel = it.first
                            fields.any { it.name.equals(rel) }
                        }
                        .toSet()
                )
            }
        }

        //  Determines the relationships an entity inherits by propagating the relationships from
        // parent to child
        val entityRoots: MutableList<ClassInfo> =
            hierarchy.filter { it.value.first == null }.map { it.key }.toMutableList()
        entityRoots.forEach {
            inheritedFields[it.neo4jName() ?: it.underlyingClass.simpleName] = mutableSetOf()
        }
        entityRoots.forEach { buildInheritedFields(it) }

        relCanHave.forEach {
            childrenFields[it.key] =
                it.value
                    .subtract(inheritedFields[it.key] ?: emptyList())
                    .subtract(inherentFields[it.key] ?: emptyList())
        }
        println()
    }

    private fun buildInheritedFields(classInfo: ClassInfo) {
        val fields: MutableSet<Pair<String, String>> = mutableSetOf()
        inherentFields[classInfo.neo4jName() ?: classInfo.underlyingClass.simpleName]?.let {
            fields.addAll(it)
        }
        inheritedFields[classInfo.neo4jName() ?: classInfo.underlyingClass.simpleName]?.let {
            fields.addAll(it)
        }

        hierarchy[classInfo]?.second?.forEach {
            inheritedFields[it.neo4jName() ?: it.underlyingClass.simpleName] = fields
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
                        ?.flatMap {
                            relCanHave[it.neo4jName() ?: it.underlyingClass.simpleName] ?: setOf()
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

    fun printEntities(classInfo: ClassInfo, out: PrintWriter) {
        // TODO print a section for every entity. List of relationships not inherent. List of rel
        // inherent with result node. try to get links into relationship and target.
        // TODO subsection with inherent relationships.
        val entityLabel = toLabel(classInfo)

        out.println("# $entityLabel<a id=\"e${entityLabel}\"></a>")

        // Todo print entity description
        if (hierarchy[classInfo]?.first != null) {
            out.print("**Labels**:")
            // Todo Print markdown with hierarchy, and not inherent relationships

            hierarchy[classInfo]?.first?.let {
                getHierarchy(it).forEach {
                    out.print(
                        "${getBoxWithColor(lightGray,"[${toLabel(it)}](#${toAnchorLink("e"+toLabel(it))})")}\t"
                    )
                }
            }
            out.print(
                "${getBoxWithColor(lightBlue,"[${entityLabel}](#${toAnchorLink("e"+entityLabel)})")}\t"
            )
            out.println()
        }
        if (hierarchy[classInfo]?.second?.isNotEmpty() ?: false) {
            out.println("## Children")

            hierarchy[classInfo]?.second?.let {
                if (it.isNotEmpty()) {
                    it.forEach {
                        out.print(
                            "${getBoxWithColor(lightGray,"[${toLabel(it)}](#${toAnchorLink("e"+toLabel(it))})")}\t"
                        )
                        // out.println("click ${toLabel(it)} href
                        // \"#${toAnchorLink(toLabel(it))}\"")
                    }
                    out.println()
                }
            }
        }

        if (inherentFields.isNotEmpty() && inheritedFields.isNotEmpty()) {
            out.println("## Relationships")

            noLabelDups(inherentFields[entityLabel])?.forEach {
                out.println(
                    "${getBoxWithColor(lightGreen,"[${it.second}](#${ toLabel(classInfo) + it.second})")}\t"
                )
            }
            noLabelDups(inheritedFields[entityLabel])?.forEach {
                var inherited = it
                var current = classInfo
                var baseClass: ClassInfo? = null
                while (baseClass == null) {
                    inherentFields[toLabel(current)]?.let {
                        if (it.any { it.second.equals(inherited.second) }) {
                            baseClass = current
                        }
                    }
                    hierarchy[current]?.first?.let { current = it }
                }
                out.println(
                    "${getBoxWithColor(lightGray,"[${it.second}](#${toConcatName(toLabel(baseClass)+it.second)})")}\t"
                )
            }

            noLabelDups(inherentFields[entityLabel])?.forEach {
                printRelationships(classInfo, it, out)
            }
        }

        hierarchy[classInfo]?.second?.forEach { printEntities(it, out) }
    }

    fun noLabelDups(list: Set<Pair<String, String>>?): Set<Pair<String, String>>? {
        if (list == null) return null
        return list
            .map { it.second }
            .distinct()
            .map {
                val label = it
                list.first { it.second == label }
            }
            .toSet()
    }

    fun toLabel(classInfo: ClassInfo?): String {
        if (classInfo == null) {
            return "Node"
        }
        return classInfo.neo4jName() ?: classInfo.underlyingClass.simpleName
    }

    fun toAnchorLink(entityName: String): String {
        return toConcatName(entityName).lowercase(Locale.getDefault())
    }

    fun toConcatName(entityName: String): String {
        return entityName.replace(" ", "-")
    }

    fun openMermaid(out: PrintWriter) {
        out.println(
            "```mermaid\n" +
                "flowchart LR\n" +
                "  classDef outer fill:#fff,stroke:#ddd,stroke-dasharray:5 5;" +
                "  classDef special fill:#afa,stroke:#5a5,stroke-dasharray:5 5;"
        )
    }
    fun closeMermaid(out: PrintWriter) {
        out.println("```")
    }

    fun getHierarchy(classInfo: ClassInfo): MutableList<ClassInfo> {
        val inheritance: MutableList<ClassInfo> = mutableListOf()
        hierarchy[classInfo]?.first?.let { inheritance.addAll(getHierarchy(it)) }
        inheritance.add(classInfo)
        return inheritance
    }

    fun getTargetInfo(fInfo: FieldInfo): Pair<Boolean, ClassInfo?> {
        val type = fInfo.field.genericType
        relationshipFields
            .map { it.value.field.genericType }
            .filterIsInstance<ParameterizedType>()
            .map { it.rawType }
        val baseClass: Type? = getNestedBaseType(type)
        var multiplicity = getNestedMultiplicity(type)

        var targetClassInfo: ClassInfo? = null
        if (baseClass != null) {
            targetClassInfo =
                hierarchy
                    .map { it.key }
                    .filter {
                        baseClass.typeName.split(" ").contains(it.underlyingClass.canonicalName)
                    }
                    .firstOrNull()
        }

        return Pair(multiplicity, targetClassInfo)
    }

    fun getNestedBaseType(type: Type): Type? {
        if (type is ParameterizedType) {
            return type.actualTypeArguments.map { getNestedBaseType(it) }.firstOrNull()
        }
        return type
    }

    fun getNestedMultiplicity(type: Type): Boolean {
        if (type is ParameterizedType) {
            if (
                type.rawType.typeName.substringBeforeLast(".").equals("java.util")
            ) { // listOf(List::class).contains(type.rawType)
                return true
            } else {
                return type.actualTypeArguments.any { getNestedMultiplicity(it) }
            }
        }
        return false
    }

    fun getBoxWithColor(color: String, text: String): String {
        return "<span style=\"background:${color};\n" +
            "    border-radius:5%;\n" +
            "    line-height: 26px;\n" +
            "    display: inline-block;\n" +
            "    text-align: center;\n" +
            "    margin-bottom: 10px;\n" +
            "    padding-left: 10px;\n" +
            "    padding-right: 10px;\">${text}</span>"
    }

    fun printRelationships(
        classInfo: ClassInfo,
        relationshipLabel: Pair<String, String>,
        out: PrintWriter
    ) {
        val fieldInfo: FieldInfo = classInfo.getFieldInfo(relationshipLabel.first)
        val targetInfo = getTargetInfo(fieldInfo)
        val multiplicity = if (targetInfo.first) "*" else "¹"
        out.println(
            "### ${relationshipLabel.second}<a id=\"${toLabel(classInfo)+relationshipLabel.second}\"></a>"
        )
        openMermaid(out)
        out.println(
            "${toLabel(classInfo)}--\"${relationshipLabel.second}${multiplicity}\"-->${toLabel(classInfo)}${relationshipLabel.second}[<a href='#${toAnchorLink("e" + toLabel(targetInfo.second))}'>${toLabel(targetInfo.second)}</a>]:::outer"
        )
        closeMermaid(out)
    }
}
