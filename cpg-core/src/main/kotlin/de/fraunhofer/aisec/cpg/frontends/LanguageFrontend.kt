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

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.ast.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.util.*
import org.slf4j.LoggerFactory

/**
 * The main task of the language frontend is to translate the programming language-specific files to
 * the common CPG nodes. It further fills the scopeManager. The language frontend must not be used
 * after having processed the files, i.e., it won't be available in passes.
 *
 * More information can be found in the
 * [GitHub wiki page](https://github.com/Fraunhofer-AISEC/cpg/wiki/Language-Frontends).
 */
abstract class LanguageFrontend<AstNode, TypeNode>(
    /**
     * The translation context, which contains all necessary managers used in this frontend parsing
     * process. Note, that different contexts could be passed to frontends, e.g., in parallel
     * parsing to supply different managers to different frontends.
     */
    final override var ctx: TranslationContext,

    /** The language this frontend works for. */
    override val language: Language<out LanguageFrontend<AstNode, TypeNode>>,
) :
    ProcessedListener(),
    CodeAndLocationProvider<AstNode>,
    LanguageProvider,
    ContextProvider,
    ScopeProvider,
    NamespaceProvider,
    RawNodeTypeProvider<AstNode> {
    val scopeManager: ScopeManager = ctx.scopeManager
    val typeManager: TypeManager = ctx.typeManager
    val config: TranslationConfiguration = ctx.config

    var currentTU: TranslationUnitDeclaration? = null

    @Throws(TranslationException::class)
    fun parseAll(): List<TranslationUnitDeclaration> {
        val units = ArrayList<TranslationUnitDeclaration>()
        for (componentFiles in config.softwareComponents.values) {
            for (sourceFile in componentFiles) {
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
    fun build(init: LanguageFrontend<*, *>.() -> TranslationResult): TranslationResult {
        return init(this)
    }

    /**
     * This function serves as an entry-point to type parsing in the language frontend. It needs to
     * return a [Type] object based on the ast type object used by the language frontend, e.g., the
     * parser.
     *
     * A language frontend will usually de-construct the ast type object, e.g., in case of pointer
     * or array types and then either recursively call this function or call other helper functions
     * similar to this one. Ideally, they should share the [typeOf] name, but have different method
     * signatures.
     */
    abstract fun typeOf(type: TypeNode): Type

    /**
     * Returns the raw code of the ast node, generic for java or c++ ast nodes.
     *
     * @param <T> the raw ast type
     * @param astNode the ast node
     * @return the source code </T>
     */
    abstract override fun codeOf(astNode: AstNode): String?

    /**
     * Returns the [Region] of the code with line and column, index starting at 1, generic for java
     * or c++ ast nodes.
     *
     * @param <T> the raw ast type
     * @param astNode the ast node
     * @return the location </T>
     */
    abstract override fun locationOf(astNode: AstNode): PhysicalLocation?

    open fun cleanup() {
        clearProcessed()
    }

    abstract fun setComment(node: Node, astNode: AstNode)

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
