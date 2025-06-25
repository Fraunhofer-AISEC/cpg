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
import de.fraunhofer.aisec.cpg.IncompatibleSignature
import de.fraunhofer.aisec.cpg.SignatureMatches
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.CastNotPossible
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.applyMetadata
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.PartialDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.default
import de.fraunhofer.aisec.cpg.graph.newFunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.newParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.parseName
import de.fraunhofer.aisec.cpg.graph.returns
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemoryAddress
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnknownMemoryValue
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.helpers.functional.equalLinkedHashSetOf
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.matchesSignature
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.tryCast
import java.io.File
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * If the user of the library registers one or multiple DFG-function summary files (via
 * [TranslationConfiguration.Builder.registerFunctionSummaries]), this class is responsible for
 * parsing the files, caching the result and adding the respective DFG summaries to the
 * [FunctionDeclaration].
 */
class DFGFunctionSummaries {
    private constructor()

    /** Caches a mapping of the [FunctionDeclarationEntry] to a list of its [DFGEntry]. */
    val functionToDFGEntryMap = mutableMapOf<FunctionDeclarationEntry, List<DFGEntry>>()

    fun hasSummary(functionDeclaration: FunctionDeclaration) =
        functionDeclaration.functionSummary.isNotEmpty()

    fun getLastWrites(
        functionDeclaration: FunctionDeclaration
    ): Map<Node, Set<FunctionDeclaration.FSEntry>> = functionDeclaration.functionSummary

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
    fun DFGPass.addFlowsToFunctionDeclaration(functionDeclaration: FunctionDeclaration): Boolean {
        val dfgEntries = findFunctionDeclarationEntry(functionDeclaration) ?: return false
        applyDfgEntryToFunctionDeclaration(functionDeclaration, dfgEntries)
        return true
    }

    fun generateFunctionForEntry(
        contextProvider: ContextProvider,
        language: Language<*>,
        declEntry: FunctionDeclarationEntry,
        summary: List<DFGEntry>,
    ): FunctionDeclaration {
        val inferredFunction =
            contextProvider.newFunctionDeclaration(language.parseName(declEntry.methodName))
        declEntry.signature?.forEachIndexed { i, typeName ->
            val type =
                if (contextProvider.ctx.typeManager.typeExists(typeName)) {
                    contextProvider.ctx.typeManager.lookupResolvedType(typeName)
                } else {
                    val type = ObjectType(typeName, listOf(), false, language)
                    // Apply our usual metadata, such as scope, code, location, if we have any. Make
                    // sure only to refer by the local name because we will treat types as sort of
                    // references when creating them and resolve them later.
                    type.applyMetadata(contextProvider, typeName, doNotPrependNamespace = true)

                    // Piping it through register type will ensure that we know the type and can
                    // resolve it later
                    // contextProvider.ctx.typeManager.registerType(type)
                    type
                } ?: language.unknownType()
            inferredFunction.parameters +=
                contextProvider.newParameterDeclaration(Name("param$i"), type)
        }
        applyDfgEntryToFunctionDeclaration(inferredFunction, summary)
        return inferredFunction
    }

    /**
     * It identifies the "best match" of all [FunctionDeclarationEntry]s stored in the
     * [functionToDFGEntryMap] for the given [functionDecl]. It therefore checks that
     * 1) The languages match
     * 2) The method/function names match
     * 3) If there are multiple entries with different signatures, the signature has to match. If
     *    none of the entries with a signature matches, we take the "default" entry without a
     *    signature.
     * 4) If it's a method (i.e., invoked on an object), we also consider which type of the
     *    receiver/base is the most precise one
     *
     * This method returns the list of [DFGEntry] for the "best match" or `null` if no entry
     * matches.
     */
    private fun DFGPass.findFunctionDeclarationEntry(
        functionDecl: FunctionDeclaration
    ): List<DFGEntry>? {
        if (functionToDFGEntryMap.isEmpty()) return null

        val language = functionDecl.language
        val languageName = language.javaClass.name
        val methodName = functionDecl.name

        // The language and the method name have to match. If a signature is specified, it also has
        // to match to the one of the FunctionDeclaration, null indicates that we accept everything.
        val matchingEntries =
            functionToDFGEntryMap.keys.filter {
                // The language has to match otherwise the remaining comparison is useless
                if (languageName.endsWith(it.language)) {
                    // Split the name if we have a FQN
                    val entryMethodName = language.parseName(it.methodName)
                    val entryRecord =
                        entryMethodName.parent?.let {
                            typeManager.lookupResolvedType(
                                entryMethodName.parent,
                                language = language,
                            )
                        }
                    methodName.lastPartsMatch(
                        entryMethodName.localName
                    ) && // The local name has to match
                        // If it's a method, the record declaration has to be compatible with the
                        // type of the entry's record declaration. We take the type of the method
                        // name's parent and generate a type from it. We then check if this type is
                        // a supertype
                        (entryRecord == null ||
                            (functionDecl as? MethodDeclaration)
                                ?.recordDeclaration
                                ?.toType()
                                ?.tryCast(entryRecord) != CastNotPossible) &&
                        // The parameter types have to match
                        (it.signature == null ||
                            functionDecl.matchesSignature(
                                it.signature.map { signatureType ->
                                    typeManager.lookupResolvedType(
                                        signatureType,
                                        language = language,
                                    ) ?: functionDecl.unknownType()
                                }
                            ) != IncompatibleSignature)
                } else {
                    false
                }
            }

        log.debug(
            "Found {} matching entries for function declaration {} with parameter types {} in {}",
            matchingEntries.size,
            functionDecl.name,
            functionDecl.signatureTypes.map(Node::name),
            functionDecl.scope,
        )

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
             * If there are still multiple options, we take the longest signature.
             * If this also didn't help to get a precise result, we iterate through the parameters and take the most precise one. We start with index 0 and count upwards, so if param0 leads to a single result, we're done and other entries won't be considered even if all the remaining parameters are more precise or whatever.
             * If nothing helped to get a unique entry, we pick the first remaining entry and hope it's the most precise one.
             */
            val typeEntryList =
                matchingEntries
                    .filter { it.signature != null }
                    .map {
                        Pair(
                            language.parseName(it.methodName).parent?.let { it1 ->
                                typeManager.lookupResolvedType(it1, language = language)
                            } ?: language.unknownType(),
                            it,
                        )
                    }
            val uniqueTypes = typeEntryList.map { it.first }.distinct()
            val targetType =
                language.parseName(functionDecl.name).parent?.let { it1 ->
                    typeManager.lookupResolvedType(it1, language = language)
                } ?: language.unknownType()

            val mostPreciseType =
                uniqueTypes
                    .map { Pair(it, language.tryCast(targetType, it)) }
                    .minByOrNull { it.second.depthDistance }
                    ?.first

            val mostPreciseClassEntries =
                typeEntryList.filter { it.first == mostPreciseType }.map { it.second }

            val signatureResults =
                mostPreciseClassEntries
                    .map {
                        Pair(
                            it,
                            functionDecl.matchesSignature(
                                it.signature?.map {
                                    typeManager.lookupResolvedType(it, language = language)
                                        ?: language.unknownType()
                                } ?: listOf()
                            ),
                        )
                    }
                    .filter { it.second is SignatureMatches }
                    .associate { it }

            val rankings = signatureResults.entries.map { Pair(it.value.ranking, it.key) }

            // Find the best (lowest) rank and find functions with the specific rank
            val bestRanking = rankings.minBy { it.first }.first
            val list = rankings.filter { it.first == bestRanking }.map { it.second }

            functionToDFGEntryMap[list.first()]
        } else {
            null
        }
    }

    private val functionDeclarationToMemAddrMap =
        mutableMapOf<FunctionDeclaration, MutableMap<Name, MemoryAddress>>()

    /**
     * This method parses the [DFGEntry] entries in [dfgEntries] and adds the respective DFG edges
     * between the parameters, receiver and potentially the [functionDeclaration] itself.
     */
    fun applyDfgEntryToFunctionDeclaration(
        functionDeclaration: FunctionDeclaration,
        dfgEntries: List<DFGEntry>,
    ) {
        for (entry in dfgEntries) {
            var srcValueDepth = 1
            var destValueDepth = 1
            val granularity =
                if (entry.dfgType == "partial") PartialDataflowGranularity("hardcoded")
                else default()
            val destNodes: MutableSet<Node> = mutableSetOf()
            val from: Node? =
                if (entry.from.startsWith("param")) {
                    try {
                        val e = entry.from.split(".")
                        val paramIndex = e.getOrNull(0)?.removePrefix("param")?.toInt()
                        if (paramIndex != null) {
                            val foo = functionDeclaration.parameters.getOrNull(paramIndex)
                            if (e.getOrNull(1) == "deref") srcValueDepth = 2
                            foo
                        } else null
                    } catch (_: NumberFormatException) {
                        null
                    }
                } else if (entry.from.startsWith("NewMemoryAddress")) {
                    val memAddrName = Name(entry.from, functionDeclaration.name)
                    functionDeclarationToMemAddrMap
                        .computeIfAbsent(functionDeclaration) { mutableMapOf() }
                        .computeIfAbsent(memAddrName) { MemoryAddress(memAddrName) }
                } else if (entry.from == "base") {
                    (functionDeclaration as? MethodDeclaration)?.receiver
                } else {
                    UnknownMemoryValue(Name(entry.from))
                }
            val to =
                if (entry.to.startsWith("param")) {
                    try {
                        val e = entry.to.split(".")
                        val paramIndex = e.getOrNull(0)?.removePrefix("param")?.toInt()
                        if (e.getOrNull(1) == "deref") destValueDepth = 2
                        else if (e.getOrNull(1) == "address") destValueDepth = 0
                        val paramTo =
                            paramIndex?.let { functionDeclaration.parameters.getOrNull(it) }
                        if (paramTo != null) {
                            destNodes.add(paramTo)
                        }
                        paramTo
                    } catch (_: NumberFormatException) {
                        null
                    }
                } else if (entry.to == "base") {
                    val receiver = (functionDeclaration as? MethodDeclaration)?.receiver
                    if (from != null) {
                        if (receiver != null) {
                            destNodes.add(receiver)
                            TODO() // Make sure that this makes sense
                            /*functionDeclaration.functionSummary
                            .computeIfAbsent(receiver) { identitySetOf() }
                            .add(
                                FunctionDeclaration.FSEntry(
                                    destValueDepth,
                                    from,
                                    srcValueDepth,
                                    "",
                                    equalLinkedHashSetOf(
                                        Pair(functionDeclaration, equalLinkedHashSetOf())
                                    ),
                                    equalLinkedHashSetOf(false),
                                )
                            )*/
                        }
                    }
                    receiver
                } else if (entry.to == "return") {
                    if (functionDeclaration.returns.isNotEmpty())
                        destNodes.addAll(functionDeclaration.returns)
                    else destNodes.add(functionDeclaration)
                    functionDeclaration
                } else if (entry.to.startsWith("return")) {
                    val returnIndex = entry.to.removePrefix("return").toInt()
                    // TODO: It would be nice if we could model the index. Not sure how this is done
                    destNodes.addAll(functionDeclaration.returns)
                    functionDeclaration
                } else {
                    null
                }

            if (from != null && destNodes.isNotEmpty()) {
                destNodes.forEach { destNode ->
                    functionDeclaration.functionSummary
                        .computeIfAbsent(destNode) { identitySetOf() }
                        .add(
                            FunctionDeclaration.FSEntry(
                                destValueDepth,
                                from,
                                srcValueDepth,
                                "",
                                equalLinkedHashSetOf(Pair(destNode, equalLinkedHashSetOf())),
                                equalLinkedHashSetOf(granularity, false),
                            )
                        )
                }
            }

            // TODO: It would make sense to model properties here. Could be the index of a return
            // value, full vs. partial flow or whatever comes to our minds in the future
            // TODO: Unsure if we still need this. We currently draw the edges between the
            // ParameterMemoryValues. We can't do this here because we don't yet have them, so we do
            // this in handleCallExpression in the pointsToPass
            to?.let { from?.nextDFGEdges += Dataflow(from, it, granularity = granularity) }
        }
    }

    /**
     * This class summarizes a data flow entry. Consists of the [functionDeclaration] for which it
     * is relevant and a list [dataFlows] of data flow summaries.
     */
    private data class DataflowEntry(
        val functionDeclaration: FunctionDeclarationEntry,
        val dataFlows: List<DFGEntry>,
    )

    /**
     * This class is used to identify the [FunctionDeclaration] of interest for the specified flows.
     */
    data class FunctionDeclarationEntry(
        /** The FQN of the [Language] for which this flow is relevant. */
        val language: String,
        /** The FQN of the [FunctionDeclaration] or [MethodDeclaration]. */
        val methodName: String,
        /**
         * The signature of the [FunctionDeclaration]. We use a list of the FQN of the [Type]s of
         * parameter. This is optional and if not specified, we perform the matching only based on
         * the [methodName].
         */
        val signature: List<String>? = null,
    )

    /** Represents a data flow entry. */
    data class DFGEntry(
        /**
         * The start of the DFG edge. Can be a parameter (`paramX`, where X is a number), or `base`.
         */
        val from: String,
        /**
         * The end of the DFG edge. Can be a parameter (`paramX`, where X is a number), `base`, or
         * the return value (`returnX`, where X is optional and a number indicating an index).
         */
        val to: String,
        /**
         * A property which can give us more information. Currently, it's ignored, but it would make
         * sense to add e.g. partial flows based on PR 1421.
         */
        val dfgType: String,
    )

    companion object {
        /** Generates a [DFGFunctionSummaries] object from the given [files]. */
        fun fromFiles(files: List<File>): DFGFunctionSummaries {
            val dfgFunctionSummaries = DFGFunctionSummaries()
            files.forEach { dfgFunctionSummaries.addEntriesFromFile(it) }
            return dfgFunctionSummaries
        }

        val log: Logger = LoggerFactory.getLogger(DFGFunctionSummaries::class.java)
    }
}
