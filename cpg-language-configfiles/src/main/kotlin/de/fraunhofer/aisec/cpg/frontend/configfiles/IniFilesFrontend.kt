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
package de.fraunhofer.aisec.cpg.frontend.configfiles

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.io.FileInputStream
import java.net.URI
import org.ini4j.Ini
import org.ini4j.Profile

/**
 * The INI file frontend. This frontend utilizes the [ini4j library](https://ini4j.sourceforge.net/)
 * to parse the config file. The result consists of
 * - a [TranslationUnitDeclaration] wrapping the entire result
 * - a [de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration] wrapping the INI file and
 *   thus preventing collisions with other symbols which might have the same name
 * - a [RecordDeclaration] per `Section` (a section refers to a block of INI values marked with a
 *   line `[SectionName]`)
 * - a [de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration] per entry in a section. The
 *   [de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration.name] matches the `entry`s `name`
 *   field and the [de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration.initializer] is set
 *   to a [statements.expressions.Literal] with the corresponding `entry`s `value`.
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
 * - [setComment] not implemented as this is not used (no
 *   [de.fraunhofer.aisec.cpg.frontends.Handler] pattern implemented)
 * - Comments in general are not supported.
 */
class IniFilesFrontend(language: Language<IniFilesFrontend>, ctx: TranslationContext) :
    LanguageFrontend<Any, Any?>(language, ctx) {

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
        val topLevel = config.topLevel ?: file.parent
        val namespace =
            (topLevel.toString().split("/") + file.nameWithoutExtension).joinToString(
                language.namespaceDelimiter
            ) // TODO: Windows?

        val tud = newTranslationUnitDeclaration(name = file.name, rawNode = ini)
        scopeManager.resetToGlobal(tud)
        val nsd = newNamespaceDeclaration(name = namespace, rawNode = ini)
        scopeManager.addDeclaration(nsd)
        scopeManager.enterScope(nsd)

        ini.values.forEach { handleSection(it) }

        scopeManager.enterScope(nsd)
        return tud
    }

    /**
     * Translates a `Section` into a [RecordDeclaration] and handles all `entries` using
     * [handleEntry].
     */
    private fun handleSection(section: Profile.Section) {
        val record = newRecordDeclaration(name = section.name, kind = "section", rawNode = section)
        scopeManager.addDeclaration(record)
        scopeManager.enterScope(record)
        section.entries.forEach { handleEntry(it) }
        scopeManager.leaveScope(record)
    }

    /**
     * Translates an `MutableEntry` to a new
     * [de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration] with the
     * [de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration.initializer] being set to the
     * `entry`s value.
     */
    private fun handleEntry(entry: MutableMap.MutableEntry<String?, String?>) {
        val field =
            newFieldDeclaration(name = entry.key, type = primitiveType("string"), rawNode = entry)
                .apply { initializer = newLiteral(value = entry.value, rawNode = entry) }
        scopeManager.addDeclaration(field)
    }

    override fun typeOf(type: Any?): Type {
        return primitiveType("string")
    }

    override fun codeOf(astNode: Any): String? {
        return astNode.toString()
    }

    /**
     * Return the entire file as the location of any node. The parsing library in use does not
     * provide more fine granular access to a node's location.
     */
    override fun locationOf(astNode: Any): PhysicalLocation? {
        return PhysicalLocation(
            uri,
            region
        ) // currently, the line number / column cannot be accessed given an Ini object -> we only
        // provide a precise uri
    }

    override fun setComment(node: Node, astNode: Any) {
        return // not used as this function does not implement [Handler]
    }
}
