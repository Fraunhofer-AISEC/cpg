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

import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.helpers.MeasurementHolder
import de.fraunhofer.aisec.cpg.helpers.StatisticsHolder
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

/**
 * The global (intermediate) result of the translation. A [ ] will initially populate it and a [ ]
 * can extend it.
 */
class TranslationResult(
    val translationManager: TranslationManager,
    /**
     * The scope manager which comprises the complete translation result. In case of sequential
     * parsing, this scope manager is passed to the individual frontends one after another. In case
     * of sequential parsing, individual scope managers will be spawned by each language frontend
     * and then finally merged into this one.
     */
    val scopeManager: ScopeManager
) : Node(), StatisticsHolder {
    /**
     * Entry points to the CPG: "SoftwareComponent" refer to programs, application, other "bundles"
     * of software.
     */
    @SubGraph("AST")
    val components: MutableList<Component> = Collections.synchronizedList(mutableListOf())

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
    val additionalNodes: Set<Node> = HashSet()
    val isCancelled: Boolean
        get() = translationManager.isCancelled()

    /**
     * Checks if only a single software component has been analyzed and returns its translation
     * units. For multiple software components, it aggregates the results.
     *
     * @return the list of all translation units.
     */
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

    override val translatedFiles: List<String>
        get() {
            return components.flatMap { it.translationUnits }.map { it.name.toString() }
        }
    override val benchmarks: MutableSet<MeasurementHolder> = mutableSetOf()
    override val config: TranslationConfiguration
        get() = translationManager.config

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
            // the
            // component we created and add it there or create a new one.
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
        swc.translationUnits.add(tu!!)
    }

    override fun addBenchmark(b: MeasurementHolder) {
        benchmarks.add(b)
    }

    companion object {
        const val SOURCE_LOCATIONS_TO_FRONTEND = "sourceLocationsToFrontend"
        const val APPLICATION_LOCAL_NAME = "application"
    }
}
