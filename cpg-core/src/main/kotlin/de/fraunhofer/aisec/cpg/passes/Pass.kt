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

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.order.*
import java.util.function.BiConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class ComponentPass : Pass<Component>()

abstract class TranslationResultPass : Pass<TranslationResult>()

abstract class TranslationUnitPass : Pass<TranslationUnitDeclaration>()

interface PassTarget

/**
 * Represents an abstract class that enhances the graph before it is persisted. Passes can exist at
 * three different levels:
 * - the overall [TranslationResult]
 * - a [Component], and
 * - a [TranslationUnitDeclaration].
 *
 * A level should be chosen as granular as possible, to allow for the (future) parallel execution of
 * passes. Instead of directly subclassing this type, one of the types [TranslationResultPass],
 * [ComponentPass] or [TranslationUnitPass] must be used.
 */
sealed class Pass<T : PassTarget> : BiConsumer<T, TranslationResult> {
    var name: String
        protected set

    /**
     * Dependencies which, if present, have to be executed before this pass. Note: Dependencies
     * registered here will not be added automatically to the list of active passes. Use
     * [hardDependencies] to add them automatically.
     */
    internal val softDependencies: MutableSet<Class<out Pass<*>>>

    /**
     * Dependencies which have to be executed before this pass. Note: Dependencies registered here
     * will be added to the list of active passes automatically. Use [softDependencies] if this is
     * not desired.
     */
    internal val hardDependencies: MutableSet<Class<out Pass<*>>>
    internal val executeBefore: MutableSet<Class<out Pass<*>>>

    fun addSoftDependency(toAdd: Class<out Pass<*>>) {
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
                    softDependencies.add(d.value.java as Class<out Pass<*>>)
                } else {
                    hardDependencies.add(d.value.java as Class<out Pass<*>>)
                }
            }
        }
        if (this.javaClass.getAnnotationsByType(ExecuteBefore::class.java).isNotEmpty()) {
            val dependencies = this.javaClass.getAnnotationsByType(ExecuteBefore::class.java)
            for (d in dependencies) {
                executeBefore.add(d.other.java as Class<out Pass<*>>)
            }
        }
    }

    abstract fun cleanup()

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
     *   [RequiredFrontend]
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

fun executePassSequential(pass: Pass<*>, result: TranslationResult) {
    when (pass) {
        is ComponentPass -> {
            for (component in result.components) {
                pass.accept(component, result)
            }
        }
        is TranslationResultPass -> {
            pass.accept(result, result)
        }
        is TranslationUnitPass -> {
            for (component in result.components) {
                for (tu in component.translationUnits) {
                    val realPass = checkForReplacement(pass, tu.language, result.config)
                    realPass.accept(tu, result)
                }
            }
        }
    }
}

/**
 * Checks, whether the specified pass has a replacement configured in [config] for the given
 * [language]. Currently, we only allow replacement on translation unit level, as this is the only
 * level which has a single language set.
 */
fun checkForReplacement(
    pass: TranslationUnitPass,
    language: Language<*>?,
    config: TranslationConfiguration
): TranslationUnitPass {
    val replacements = config.replacedPasses[pass::class]
    return if (replacements != null) {
        val langClass = replacements.first
        if (langClass.isInstance(language) && replacements.second is TranslationUnitPass) {
            replacements.second as TranslationUnitPass
        } else {
            pass
        }
    } else {
        pass
    }
}
