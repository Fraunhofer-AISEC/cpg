/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.PopulatedByPass
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.assumptions.Assumption
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.multiLanguage
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.passes.ImportDependencies
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.io.File
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.Transient

/**
 * A node which presents some kind of complete piece of software, e.g., an application, a library,
 * microservice, ...
 *
 * This node holds all translation units belonging to this software component as well as (potential)
 * entry points or interactions with other software.
 */
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
open class Component : AstNode() {
    @Relationship("TRANSLATION_UNITS") val translationUnitEdges = astEdgesOf<TranslationUnit>()
    /** All translation units belonging to this application. */
    val translationUnits by unwrapping(Component::translationUnitEdges)

    /**
     * The import dependencies of [TranslationUnit] nodes of this component. The preferred way to
     * access this is via [Strategy.TRANSLATION_UNITS_LEAST_IMPORTS].
     */
    @Transient
    @PopulatedByPass(ImportResolver::class)
    var translationUnitDependencies: ImportDependencies<TranslationUnit>? = null

    @Synchronized
    fun addTranslationUnit(tu: TranslationUnit) {
        translationUnits.add(tu)
    }

    /**
     * In contrast to other Nodes we do not add the assumptions collected over the component because
     * we are already the component.
     */
    override fun relevantAssumptions(): Set<Assumption> {
        return assumptions.toSet()
    }

    /**
     * Returns the top-level directory of this component according to
     * [TranslationConfiguration.topLevels]
     */
    context(provider: ContextProvider)
    fun topLevel(): File? {
        return provider.ctx.config.topLevels[this.name.localName]
    }

    /**
     * All points where unknown data may enter this application, e.g., the main method, or other
     * targets such as listeners to external events such as HTTP requests. This also includes the
     * list of possible entry points.
     */
    val incomingInteractions: MutableList<Node> = mutableListOf()

    /** All outgoing interactions such as sending data to the network or some kind of IPC. */
    val outgoingInteractions: MutableList<Node> = mutableListOf()

    override var language: Language<*>
        get() {
            return multiLanguage()
        }
        set(_) {}
}
