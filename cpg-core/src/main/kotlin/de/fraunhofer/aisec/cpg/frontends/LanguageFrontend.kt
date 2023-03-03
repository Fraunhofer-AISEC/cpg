/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.util.*
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

/**
 * The main task of the language frontend is to translate the programming language-specific files to
 * the common CPG nodes. It further fills the scopeManager. The language frontend must not be used
 * after having processed the files, i.e., it won't be available in passes.
 *
 * More information can be found in the
 * [github wiki page](https://github.com/Fraunhofer-AISEC/cpg/wiki/Language-Frontends).
 */
abstract class LanguageFrontend(
    override val language: Language<out LanguageFrontend>,
    val config: TranslationConfiguration,
    scopeManager: ScopeManager,
) :
    ProcessedListener(),
    CodeAndLocationProvider,
    LanguageProvider,
    ScopeProvider,
    NamespaceProvider {
    var scopeManager: ScopeManager = scopeManager
        set(scopeManager) {
            field = scopeManager
            field.lang = this
        }

    init {
        // this.scopeManager = scopeManager
        this.scopeManager.lang = this
    }

    var currentTU: TranslationUnitDeclaration? = null

    @Throws(TranslationException::class)
    fun parseAll(): List<TranslationUnitDeclaration> {
        val units = ArrayList<TranslationUnitDeclaration>()
        for (component in config.softwareComponents.keys) {
            for (sourceFile in config.softwareComponents[component]!!) {
                units.add(parse(sourceFile))
            }
        }
        return units
    }

    @Throws(TranslationException::class) abstract fun parse(file: File): TranslationUnitDeclaration

    /**
     * This function returns a [TranslationResult], but rather than parsing source code, the
     * function [init] is used to build nodes in the Node Fluent DSL.
     */
    fun build(init: LanguageFrontend.() -> TranslationResult): TranslationResult {
        return init(this)
    }

    /**
     * Returns the raw code of the ast node, generic for java or c++ ast nodes.
     *
     * @param <T> the raw ast type
     * @param astNode the ast node
     * @return the source code </T>
     */
    abstract fun <T> getCodeFromRawNode(astNode: T): String?

    /**
     * Returns the [Region] of the code with line and column, index starting at 1, generic for java
     * or c++ ast nodes.
     *
     * @param <T> the raw ast type
     * @param astNode the ast node
     * @return the location </T>
     */
    abstract fun <T> getLocationFromRawNode(astNode: T): PhysicalLocation?
    override fun <N, S> setCodeAndLocation(cpgNode: N, astNode: S?) {
        if (cpgNode is Node && astNode != null) {
            if (config.codeInNodes) {
                // only set code, if it's not already set or empty
                val code = getCodeFromRawNode<S?>(astNode)
                if (code != null) {
                    (cpgNode as Node).code = code
                } else {
                    log.warn("Unexpected: No code for node {}", astNode)
                }
            }
            (cpgNode as Node).location = getLocationFromRawNode<S?>(astNode)
        }
    }

    /**
     * To prevent issues with different newline types and formatting.
     *
     * @param node
     * - The newline type is extracted from the nodes code.
     *
     * @return the String of the newline
     */
    fun getNewLineType(node: Node): String {
        val nls = listOf("\n\r", "\r\n", "\n")
        for (nl in nls) {
            if (node.toString().endsWith(nl)) {
                return nl
            }
        }
        log.debug("Could not determine newline type. Assuming \\n. {}", node)
        return "\n"
    }

    /**
     * Returns the code represented by the subregion extracted from the parent node and its region.
     *
     * @param node
     * - The parent node of the subregion
     *
     * @param nodeRegion
     * - region needs to be precomputed.
     *
     * @param subRegion
     * - precomputed subregion
     *
     * @return the code of the subregion.
     */
    fun getCodeOfSubregion(node: Node, nodeRegion: Region, subRegion: Region): String {
        val code = node.code ?: return ""
        val nlType = getNewLineType(node)
        val start =
            if (subRegion.startLine == nodeRegion.startLine) {
                subRegion.startColumn - nodeRegion.startColumn
            } else {
                (StringUtils.ordinalIndexOf(
                    code,
                    nlType,
                    subRegion.startLine - nodeRegion.startLine
                ) + subRegion.startColumn)
            }
        val end =
            if (subRegion.endLine == nodeRegion.startLine) {
                subRegion.endColumn - nodeRegion.startColumn
            } else {
                (StringUtils.ordinalIndexOf(
                    code,
                    nlType,
                    subRegion.endLine - nodeRegion.startLine
                ) + subRegion.endColumn)
            }
        return code.substring(start, end)
    }

    /**
     * Merges two regions. The new region contains both and is the minimal region to do so.
     *
     * @param regionOne the first region
     * @param regionTwo the second region
     * @return the merged region
     */
    fun mergeRegions(regionOne: Region, regionTwo: Region): Region {
        val ret = Region()
        if (
            regionOne.startLine < regionTwo.startLine ||
                regionOne.startLine == regionTwo.startLine &&
                    regionOne.startColumn < regionTwo.startColumn
        ) {
            ret.startLine = regionOne.startLine
            ret.startColumn = regionOne.startColumn
        } else {
            ret.startLine = regionTwo.startLine
            ret.startColumn = regionTwo.startColumn
        }
        if (
            regionOne.endLine > regionTwo.endLine ||
                regionOne.endLine == regionTwo.endLine && regionOne.endColumn > regionTwo.endColumn
        ) {
            ret.endLine = regionOne.endLine
            ret.endColumn = regionOne.startColumn
        } else {
            ret.endLine = regionTwo.endLine
            ret.endColumn = regionTwo.endColumn
        }
        return ret
    }

    open fun cleanup() {
        clearProcessed()
    }

    abstract fun <S, T> setComment(s: S, ctx: T)

    companion object {
        // Allow non-Java frontends to access the logger (i.e. jep)
        val log = LoggerFactory.getLogger(LanguageFrontend::class.java)
    }

    override val scope: Scope?
        get() {
            return this.scopeManager.currentScope
        }

    override val namespace: Name?
        get() {
            return this.scopeManager.currentNamespace
        }
}
