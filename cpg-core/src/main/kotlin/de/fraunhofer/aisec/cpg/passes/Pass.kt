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
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.LanguageProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.order.*
import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A [TranslationResultPass] is a pass that operates on a [TranslationResult]. If used with
 * [executePassSequential], one [Pass] object is instantiated for the whole [TranslationResult].
 */
abstract class TranslationResultPass(config: TranslationConfiguration, scopeManager: ScopeManager) :
    Pass<TranslationResult>(config, scopeManager)

/**
 * A [ComponentPass] is a pass that operates on a [Component]. If used with [executePassSequential],
 * one [Pass] object is instantiated for each [Component] in a [TranslationResult].
 */
abstract class ComponentPass(config: TranslationConfiguration, scopeManager: ScopeManager) :
    Pass<Component>(config, scopeManager)

/**
 * A [TranslationUnitPass] is a pass that operates on a [TranslationUnitDeclaration]. If used with
 * [executePassSequential], one [Pass] object is instantiated for each [TranslationUnitDeclaration]
 * in a [Component].
 */
abstract class TranslationUnitPass(config: TranslationConfiguration, scopeManager: ScopeManager) :
    Pass<TranslationUnitDeclaration>(config, scopeManager)

/**
 * A pass target is an interface for a [Node] on which a [Pass] can operate, it should only be
 * implemented by [TranslationResult], [Component] and [TranslationUnitDeclaration].
 */
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
sealed class Pass<T : PassTarget>(
    val config: TranslationConfiguration,
    val scopeManager: ScopeManager
) : Consumer<T> {
    var name: String
        protected set

    init {
        name = this.javaClass.name
    }

    abstract fun cleanup()

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

/**
 * Creates a new [Pass] (based on [cls]) and executes it sequentially on the nodes of [result].
 * Depending on the type of pass, this will either execute the pass directly on the overall result,
 * loop through each component or through each translation unit.
 */
fun executePassSequential(
    cls: KClass<out Pass<*>>,
    result: TranslationResult,
    executedFrontends: Collection<LanguageFrontend>
) {
    // This is a bit tricky but actually better than other reflection magic. We are creating a
    // "prototype" instance of our pass class, so we can deduce certain type information more
    // easily.
    val prototype =
        cls.primaryConstructor?.call(result.config, result.scopeManager)
            ?: throw TranslationException("Could not create prototype pass")

    when (prototype) {
        is TranslationResultPass -> {
            executePass(
                (prototype as TranslationResultPass)::class,
                result,
                result.config,
                result.scopeManager,
                executedFrontends
            )
        }
        is ComponentPass -> {
            for (component in result.components) {
                executePass(
                    (prototype as ComponentPass)::class,
                    component,
                    result.config,
                    result.scopeManager,
                    executedFrontends
                )
            }
        }
        is TranslationUnitPass -> {
            for (component in result.components) {
                for (tu in component.translationUnits) {
                    executePass(
                        (prototype as TranslationUnitPass)::class,
                        tu,
                        result.config,
                        result.scopeManager,
                        executedFrontends
                    )
                }
            }
        }
    }
}

inline fun <reified T : PassTarget> executePass(
    cls: KClass<out Pass<T>>,
    target: T,
    config: TranslationConfiguration,
    scopeManager: ScopeManager,
    executedFrontends: Collection<LanguageFrontend>
): Pass<T>? {
    val language =
        if (target is LanguageProvider) {
            target.language
        } else {
            null
        }

    val realClass = checkForReplacement(cls, language, config)

    val pass = realClass.primaryConstructor?.call(config, scopeManager)
    if (pass?.runsWithCurrentFrontend(executedFrontends) == true) {
        pass.accept(target)
        pass.cleanup()
        return pass
    }

    return null
}

/**
 * Checks, whether the specified pass has a replacement configured in [config] for the given
 * [language]. Currently, we only allow replacement on translation unit level, as this is the only
 * level which has a single language set.
 */
fun <T : PassTarget> checkForReplacement(
    cls: KClass<out Pass<T>>,
    language: Language<*>?,
    config: TranslationConfiguration
): KClass<out Pass<T>> {
    val replacements = config.replacedPasses[cls]
    if (replacements != null) {
        val langClass = replacements.first
        if (langClass.isInstance(language)) {
            return replacements.second as KClass<out Pass<T>>
        }
    }

    return cls
}
