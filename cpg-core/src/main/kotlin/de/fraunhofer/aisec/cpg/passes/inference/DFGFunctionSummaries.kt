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
package de.fraunhofer.aisec.cpg.passes.inference

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.parseName
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.io.File

class DFGFunctionSummaries {
    private constructor()

    val functionToDFGEntryMap = mutableMapOf<FunctionDeclarationEntry, List<DFGEntry>>()

    /** This function returns a list of [DataflowEntry] from the specified file. */
    private fun addEntriesFromFile(file: File): Map<FunctionDeclarationEntry, List<DFGEntry>> {
        val mapper =
            if (file.extension.lowercase() in listOf("yaml", "yml")) {
                    ObjectMapper(YAMLFactory())
                } else {
                    ObjectMapper(JsonFactory())
                }
                .registerKotlinModule()
        val entries = mapper.readValue<List<DataflowEntry>>(file)
        for (entry in entries) {
            functionToDFGEntryMap[entry.functionDeclaration] = entry.dataFlows
        }
        return functionToDFGEntryMap
    }

    /**
     * Adds the DFG edges to the [functionDeclaration] depending on the function summaries which are
     * kept in this object. If no suitable entry was found, this method returns `false`.
     */
    fun addFlowsToFunctionDeclaration(functionDeclaration: FunctionDeclaration): Boolean {
        val dfgEntries = findFunctionDeclarationEntry(functionDeclaration) ?: return false
        applyDfgEntryToFunctionDeclaration(functionDeclaration, dfgEntries)
        return true
    }

    private fun findFunctionDeclarationEntry(functionDecl: FunctionDeclaration): List<DFGEntry>? {
        if (functionToDFGEntryMap.isEmpty()) return null

        val language = functionDecl.language
        val languageName = language?.javaClass?.name
        val methodName = functionDecl.name
        // The language and the method name have to match. If a signature is specified, it also has
        // to match to the one of the FunctionDeclaration, null indicates that we accept everything.
        val matchingEntries =
            functionToDFGEntryMap.keys.filter {
                it.language == languageName &&
                    methodName.lastPartsMatch(it.methodName) &&
                    (it.signature == null ||
                        functionDecl.hasSignature(
                            it.signature.map { signatureType -> language.objectType(signatureType) }
                        ))
            }
        return if (matchingEntries.size == 1) {
            // Only one entry => We take this one.
            functionToDFGEntryMap[matchingEntries.single()]
        } else if (matchingEntries.filter { it.signature != null }.size == 1) {
            // Only one entry with a matching signature => We take this one.
            functionToDFGEntryMap[matchingEntries.single { it.signature != null }]
        } else if (matchingEntries.isNotEmpty()) {
            /* There are multiple matching entries. We use the following routine:
             * First, we filter for existing signatures.
             * Second, we filter for the most precise class.
             * If there are still multiple options, we take the longest signature and hope it's the most precise one.
             */
            val typeEntryList =
                matchingEntries
                    .filter { it.signature != null }
                    .map {
                        Pair(
                            language.parseName(it.methodName).parent?.let { it1 ->
                                language?.objectType(it1)
                            },
                            it
                        )
                    }
            val mostPreciseClassEntries = mutableListOf<FunctionDeclarationEntry>()
            var mostPreciseType = typeEntryList.first().first
            var superTypes = getAllSupertypes(mostPreciseType)
            for (typeEntry in typeEntryList) {
                if (typeEntry.first == mostPreciseType) {
                    mostPreciseClassEntries.add(typeEntry.second)
                } else if (typeEntry.first in superTypes) {
                    mostPreciseClassEntries.clear()
                    mostPreciseClassEntries.add(typeEntry.second)
                    mostPreciseType = typeEntry.first
                    superTypes = getAllSupertypes(typeEntry.first)
                }
            }
            if (mostPreciseClassEntries.size > 1) {
                mostPreciseClassEntries.sortByDescending { it.signature?.size ?: 0 }
            }
            functionToDFGEntryMap[matchingEntries.first()]
        } else {
            null
        }
    }

    private fun getAllSupertypes(type: Type?): Set<Type> {
        if (type == null) return setOf()
        val superTypes = type.superTypes
        superTypes.addAll(type.superTypes.flatMap { getAllSupertypes(it) })
        return superTypes
    }

    private fun applyDfgEntryToFunctionDeclaration(
        functionDeclaration: FunctionDeclaration,
        dfgEntries: List<DFGEntry>
    ) {
        for (entry in dfgEntries) {
            val from =
                if (entry.from.startsWith("param")) {
                    try {
                        val paramIndex = entry.from.removePrefix("param").toInt()
                        functionDeclaration.parameters[paramIndex]
                    } catch (e: NumberFormatException) {
                        null
                    }
                } else if (entry.from == "base") {
                    (functionDeclaration as? MethodDeclaration)?.receiver
                } else {
                    null
                }
            val to =
                if (entry.to.startsWith("param")) {
                    try {
                        val paramIndex = entry.to.removePrefix("param").toInt()
                        functionDeclaration.parameters[paramIndex]
                    } catch (e: NumberFormatException) {
                        null
                    }
                } else if (entry.to == "base") {
                    (functionDeclaration as? MethodDeclaration)?.receiver
                } else if (entry.to == "return") {
                    functionDeclaration
                } else if (entry.to.startsWith("return")) {
                    val returnIndex = entry.to.removePrefix("param").toInt()
                    // TODO: It would be nice if we could model the index. Not sure how this is done
                    functionDeclaration
                } else {
                    null
                }
            // TODO: It would make sense to model properties here. Could be the index of a return
            // value, full vs. partial flow or whatever comes to our minds in the future
            to?.let { from?.addNextDFG(it) }
        }
    }

    private data class DataflowEntry(
        val functionDeclaration: FunctionDeclarationEntry,
        val dataFlows: List<DFGEntry>
    )

    data class FunctionDeclarationEntry(
        val language: String,
        val methodName: String,
        val signature: List<String>? = null
    )

    data class DFGEntry(val from: String, val to: String, val dfgType: String)

    companion object {
        /** Generates a [DFGFunctionSummaries] object from the given [files]. */
        fun fromFiles(files: List<File>): DFGFunctionSummaries {
            val dfgFunctionSummaries = DFGFunctionSummaries()
            files.forEach { dfgFunctionSummaries.addEntriesFromFile(it) }
            return dfgFunctionSummaries
        }
    }
}
