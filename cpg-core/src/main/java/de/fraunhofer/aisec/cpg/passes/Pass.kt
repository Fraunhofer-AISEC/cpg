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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.passes.order.*
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import java.util.function.Consumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Represents an abstract class that enhances the graph before it is persisted.
 *
 * Passes are expected to mutate the [TranslationResult].
 */
abstract class Pass protected constructor() : Consumer<TranslationResult> {
    var name: String
        protected set

    /**
     * Dependencies which, if present, have to be executed before this pass. Note: Dependencies
     * registered here will not be added automatically to the list of active passes. Use
     * [hardDependencies] to add them automatically.
     */
    internal val softDependencies: MutableSet<Class<out Pass>>

    /**
     * Dependencies which have to be executed before this pass. Note: Dependencies registered here
     * will be added to the list of active passes automatically. Use [softDependencies] if this is
     * not desired.
     */
    internal val hardDependencies: MutableSet<Class<out Pass>>
    internal val executeBefore: MutableSet<Class<out Pass>>

    fun addSoftDependency(toAdd: Class<out Pass?>) {
        softDependencies.add(toAdd)
    }

    lateinit var scopeManager: ScopeManager
    protected var config: TranslationConfiguration? = null

    init {
        name = this.javaClass.name
        hardDependencies = HashSet()
        softDependencies = HashSet()
        executeBefore = HashSet()

        // collect all dependencies added by [DependsOn] annotations.
        if (this.javaClass.getAnnotationsByType(DependsOn::class.java).isNotEmpty()) {
            val dependencies = this.javaClass.getAnnotationsByType(DependsOn::class.java)
            for (d in dependencies) {
                if (d.softDependency) {
                    softDependencies.add(d.value.java)
                } else {
                    hardDependencies.add(d.value.java)
                }
            }
        }
        if (this.javaClass.getAnnotationsByType(ExecuteBefore::class.java).isNotEmpty()) {
            val dependencies = this.javaClass.getAnnotationsByType(ExecuteBefore::class.java)
            for (d in dependencies) {
                executeBefore.add(d.other.java)
            }
        }
    }

    abstract fun cleanup()

    /**
     * Specifies, whether this pass supports this particular language. This defaults to `true ` *
     * and needs to be overridden if a different behaviour is wanted.
     *
     * @param language the language
     * @return truw by default
     */
    fun supportsLanguage(language: Language<out LanguageFrontend>): Boolean {
        return true
    }

    val isLastPass: Boolean
        get() =
            try {
                this.javaClass.isAnnotationPresent(ExecuteLast::class.java)
            } catch (e: Exception) {
                false
            }
    val isFirstPass: Boolean
        get() =
            try {
                this.javaClass.isAnnotationPresent(ExecuteFirst::class.java)
            } catch (e: Exception) {
                false
            }

    /**
     * Check if the pass requires a specific language frontend and if that frontend has been
     * executed.
     *
     * @return true, if the pass does not require a specific language frontend or if it matches the
     * [RequiredFrontend]
     */
    fun runsWithCurrentFrontend(usedFrontends: Collection<LanguageFrontend>): Boolean {
        if (!this.javaClass.isAnnotationPresent(RequiredFrontend::class.java)) return true
        val requiredFrontend = this.javaClass.getAnnotation(RequiredFrontend::class.java).value
        for (used in usedFrontends) {
            if (used.javaClass == requiredFrontend.java) return true
        }
        return false
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(Pass::class.java)
    }
}
