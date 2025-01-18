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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.multiLanguage
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.helpers.MeasurementHolder
import de.fraunhofer.aisec.cpg.helpers.StatisticsHolder
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import org.neo4j.ogm.annotation.Relationship

/**
 * The global (intermediate) result of the translation. A [LanguageFrontend] will initially populate
 * it and a [Pass] can extend it.
 */
class TranslationResult(
    /** A reference to our [TranslationManager]. */
    private val translationManager: TranslationManager,
    /**
     * The final [TranslationContext] of this translation result. Currently, for parallel
     * processing, we are creating one translation context for each parsed file (containing a
     * dedicated [ScopeManager] each). This property will contain the final, merged context.
     */
    var finalCtx: TranslationContext,
) : Node(), StatisticsHolder {

    @Relationship("COMPONENTS") val componentEdges = astEdgesOf<Component>()
    /**
     * Entry points to the CPG: "SoftwareComponent" refer to programs, application, other "bundles"
     * of software.
     */
    val components by unwrapping(TranslationResult::componentEdges)

    /**
     * Scratch storage that can be used by passes to store additional information in this result.
     * Callers must ensure that keys are unique. It is recommended to prefix them with the class
     * name of the Pass.
     *
     * @return the scratch storage
     */
    /** A free-for-use HashMap where passes can store whatever they want. */
    val scratch: MutableMap<String, Any> = ConcurrentHashMap()

    /**
     * A free-for-use collection of unique nodes. Nodes stored here will be exported to Neo4j, too.
     */
    val additionalNodes = mutableSetOf<Node>()
    override val benchmarks: MutableSet<MeasurementHolder> = LinkedHashSet()

    val isCancelled: Boolean
        get() = translationManager.isCancelled()

    override var ctx: TranslationContext? = null
        get() {
            return finalCtx
        }

    /**
     * Checks if only a single software component has been analyzed and returns its translation
     * units. For multiple software components, it aggregates the results.
     *
     * @return the list of all translation units.
     */
    @Deprecated(message = "translation units of individual components should be accessed instead")
    @DoNotPersist
    val translationUnits: List<TranslationUnitDeclaration>
        get() {
            if (components.size == 1) {
                return Collections.unmodifiableList(components[0].translationUnits)
            }
            val result: MutableList<TranslationUnitDeclaration> = ArrayList()
            for (sc in components) {
                result.addAll(sc.translationUnits)
            }
            return result
        }

    /**
     * If no component exists, it generates a [Component] called "application" and adds [tu]. If a
     * component already exists, adds the tu to this component.
     *
     * @param tu The translation unit to add.
     */
    @Deprecated(
        """This should not be used anymore. Instead, the corresponding component should be
        selected and the translation unit should be added there."""
    )
    @Synchronized
    fun addTranslationUnit(tu: TranslationUnitDeclaration?) {
        var swc: Component? = null
        if (components.size == 1) {
            // Only one component exists, so we take this one
            swc = components[0]
        } else if (components.isEmpty()) {
            // No component exists, so we create the new dummy component.
            swc = Component()
            swc.name = Name(APPLICATION_LOCAL_NAME, null, "")
            components.add(swc)
        } else {
            // Multiple components exist. As we don't know where to put the tu, we check if we have
            // the component we created and add it there or create a new one.
            for (component in components) {
                if (component.name.localName == APPLICATION_LOCAL_NAME) {
                    swc = component
                    break
                }
            }
            if (swc == null) {
                swc = Component()
                swc.name = Name(APPLICATION_LOCAL_NAME, null, "")
                components.add(swc)
            }
        }
        tu?.let { swc.addTranslationUnit(it) }
    }

    /**
     * Add a [Component] to this translation result in a thread safe way.
     *
     * @param sc The software component to add
     */
    @Synchronized
    fun addComponent(sc: Component) {
        components.add(sc)
    }

    override fun addBenchmark(b: MeasurementHolder) {
        benchmarks.add(b)
    }

    override val translatedFiles: List<String>
        get() {
            val result: MutableList<String> = ArrayList()
            components.forEach { sc: Component ->
                result.addAll(
                    sc.translationUnits.map(TranslationUnitDeclaration::name).map(Name::toString)
                )
            }
            return result
        }

    override val resolutionCacheResults: Int
        get() = finalCtx.scopeManager.symbolCache.results.size

    override val config: TranslationConfiguration
        get() = finalCtx.config

    override var language: Language<*>
        get() {
            return multiLanguage()
        }
        set(_) {}

    companion object {
        const val SOURCE_LOCATIONS_TO_FRONTEND = "sourceLocationsToFrontend"
        const val APPLICATION_LOCAL_NAME = "application"
    }
}
