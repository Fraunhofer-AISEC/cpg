/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.ini

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.io.FileInputStream
import java.net.URI
import org.ini4j.Ini
import org.ini4j.Profile.Section

/**
 * The INI file frontend. This frontend utilizes the [ini4j library](https://ini4j.sourceforge.net/)
 * to parse the config file. The result consists of
 * - a [TranslationUnitDeclaration] wrapping the entire result
 * - a [NamespaceDeclaration] wrapping the INI file and thus preventing collisions with other
 *   symbols which might have the same name
 * - a [RecordDeclaration] per `Section` (a section refers to a block of INI values marked with a
 *   line `[SectionName]`)
 * - a [FieldDeclaration] per entry in a section. The [FieldDeclaration.name] matches the `entry`s
 *   `name` field and the [FieldDeclaration.initializer] is set to a [Literal] with the
 *   corresponding `entry`s `value`.
 *
 * Note:
 * - the "ini4j" library does not provide any super type for all nodes. Thus, the frontend accepts
 *   `Any`
 * - [typeOf] has to be implemented, but as there are no types always returns the builtin `string`
 *   type
 * - [codeOf] has to accept `Any` (because of the limitations stated above) and simply returns
 *   `.toString()`
 * - [locationOf] always returns `null` as the "ini4j" library does not provide any means of getting
 *   a location given a node
 * - [setComment] not implemented as this is not used (no [Handler] pattern implemented)
 * - Comments in general are not supported.
 */
class IniFileFrontend(ctx: TranslationContext, language: Language<IniFileFrontend>) :
    LanguageFrontend<Any, Any?>(ctx, language) {

    private lateinit var uri: URI
    private lateinit var region: Region

    override fun parse(file: File): TranslationUnitDeclaration {
        uri = file.toURI()
        region = Region()

        val ini = Ini()
        try {
            ini.load(FileInputStream(file))
        } catch (ex: Exception) {
            throw TranslationException("Parsing failed with exception: $ex")
        }

        /*
         * build a namespace name relative to the configured
         * [de.fraunhofer.aisec.cpg.TranslationConfiguration.topLevel] using
         * [Language.namespaceDelimiter] as a separator
         */
        val topLevel = ctx.currentComponent?.topLevel()?.let { file.relativeToOrNull(it) } ?: file
        val parentDir = topLevel.parent

        val namespace =
            if (parentDir != null) {
                val pathSegments = parentDir.toString().split(File.separator)
                (pathSegments + file.nameWithoutExtension).joinToString(language.namespaceDelimiter)
            } else {
                file.nameWithoutExtension
            }

        val tud = newTranslationUnitDeclaration(name = file.name, rawNode = ini)
        scopeManager.resetToGlobal(tud)

        val nsd = newNamespaceDeclaration(name = namespace, rawNode = ini)
        scopeManager.addDeclaration(nsd)
        tud.addDeclaration(nsd)

        scopeManager.enterScope(nsd)

        ini.values.forEach {
            val record = handleSection(it)
            scopeManager.addDeclaration(record)
            nsd.addDeclaration(record)
        }

        scopeManager.leaveScope(nsd)
        return tud
    }

    /**
     * Translates a `Section` into a [RecordDeclaration] and handles all `entries` using
     * [handleEntry].
     */
    private fun handleSection(section: Section): RecordDeclaration {
        val record = newRecordDeclaration(name = section.name, kind = "section", rawNode = section)
        scopeManager.enterScope(record)
        section.entries.forEach {
            val field = handleEntry(it)
            scopeManager.addDeclaration(field)
            record.fields += field
        }
        scopeManager.leaveScope(record)

        return record
    }

    /**
     * Translates an `MutableEntry` to a new [FieldDeclaration] with the
     * [FieldDeclaration.initializer] being set to the `entry`s value.
     */
    private fun handleEntry(entry: MutableMap.MutableEntry<String?, String?>): FieldDeclaration {
        val field =
            newFieldDeclaration(name = entry.key, type = primitiveType("string"), rawNode = entry)
                .apply { initializer = newLiteral(value = entry.value, rawNode = entry) }

        return field
    }

    override fun typeOf(type: Any?): Type {
        return primitiveType("string")
    }

    /**
     * Returns an approximation of the original code by re-creating (parts of) the INI file given
     * the parsed results provided by ini4j. This is not a perfect representation of the original
     * code (comments, order, ...), however re-parsing it should result in the same
     * CPG-representation.
     */
    override fun codeOf(astNode: Any): String? {
        return when (astNode) {
            is Ini -> codeOfIni(astNode)
            is Section -> codeOfSection(astNode)
            is Map.Entry<*, *> -> codeOfEntry(astNode)
            else -> null
        }
    }

    /**
     * Returns an approximation of the original code by re-creating (parts of) the INI file given
     * the parsed results provided by ini4j. This is not a perfect representation of the original
     * code (comments, order, ...), however re-parsing it should result in the same
     * CPG-representation.
     */
    private fun codeOfIni(ini: Ini): String {
        return ini.values.joinToString(System.lineSeparator()) { codeOfSection(it) }
    }

    /**
     * Returns an approximation of the original code by re-creating (parts of) the INI file given
     * the parsed results provided by ini4j. This is not a perfect representation of the original
     * code (comments, order, ...), however re-parsing it should result in the same
     * CPG-representation.
     */
    private fun codeOfEntry(entry: Map.Entry<*, *>): String {
        return "${entry.key} = ${entry.value}"
    }

    /**
     * Returns an approximation of the original code by re-creating (parts of) the INI file given
     * the parsed results provided by ini4j. This is not a perfect representation of the original
     * code (comments, order, ...), however re-parsing it should result in the same
     * CPG-representation.
     */
    private fun codeOfSection(section: Section): String {
        return "[" +
            section.name +
            "]" +
            System.lineSeparator() +
            section.entries.joinToString(System.lineSeparator()) { codeOfEntry(it) }
    }

    /**
     * Return the entire file as the location of any node. The parsing library in use does not
     * provide more fine granular access to a node's location.
     */
    override fun locationOf(astNode: Any): PhysicalLocation? {
        return PhysicalLocation(
            uri,
            region,
        ) // currently, the line number / column cannot be accessed given an Ini object -> we only
        // provide a precise uri
    }

    override fun setComment(node: Node, astNode: Any) {
        return // not used as this function does not implement [Handler]
    }
}
