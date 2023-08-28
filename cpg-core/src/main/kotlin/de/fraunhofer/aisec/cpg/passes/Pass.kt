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

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDecl
import de.fraunhofer.aisec.cpg.passes.order.RequiredFrontend
import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A [TranslationResultPass] is a pass that operates on a [TranslationResult]. If used with
 * [executePassSequential], one [Pass] object is instantiated for the whole [TranslationResult].
 */
abstract class TranslationResultPass(ctx: TranslationContext) : Pass<TranslationResult>(ctx)

/**
 * A [ComponentPass] is a pass that operates on a [Component]. If used with [executePassSequential],
 * one [Pass] object is instantiated for each [Component] in a [TranslationResult].
 */
abstract class ComponentPass(ctx: TranslationContext) : Pass<Component>(ctx)

/**
 * A [TranslationUnitPass] is a pass that operates on a [TranslationUnitDecl]. If used with
 * [executePassSequential], one [Pass] object is instantiated for each [TranslationUnitDecl] in a
 * [Component].
 */
abstract class TranslationUnitPass(ctx: TranslationContext) : Pass<TranslationUnitDecl>(ctx)

/**
 * A pass target is an interface for a [Node] on which a [Pass] can operate, it should only be
 * implemented by [TranslationResult], [Component] and [TranslationUnitDecl].
 */
interface PassTarget

/**
 * Represents an abstract class that enhances the graph before it is persisted. Passes can exist at
 * three different levels:
 * - the overall [TranslationResult]
 * - a [Component], and
 * - a [TranslationUnitDecl].
 *
 * A level should be chosen as granular as possible, to allow for the (future) parallel execution of
 * passes. Instead of directly subclassing this type, one of the types [TranslationResultPass],
 * [ComponentPass] or [TranslationUnitPass] must be used.
 */
sealed class Pass<T : PassTarget>(final override val ctx: TranslationContext) :
    Consumer<T>, ContextProvider {
    var name: String
        protected set

    val config: TranslationConfiguration = ctx.config
    val scopeManager: ScopeManager = ctx.scopeManager
    val typeManager: TypeManager = ctx.typeManager

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
    fun runsWithCurrentFrontend(usedFrontends: Collection<LanguageFrontend<*, *>>): Boolean {
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
    ctx: TranslationContext,
    result: TranslationResult,
    executedFrontends: Collection<LanguageFrontend<*, *>>
) {
    // This is a bit tricky but actually better than other reflection magic. We are creating a
    // "prototype" instance of our pass class, so we can deduce certain type information more
    // easily.
    val prototype =
        cls.primaryConstructor?.call(ctx)
            ?: throw TranslationException("Could not create prototype pass")

    when (prototype) {
        is TranslationResultPass -> {
            executePass((prototype as TranslationResultPass)::class, ctx, result, executedFrontends)
        }
        is ComponentPass -> {
            for (component in result.components) {
                executePass((prototype as ComponentPass)::class, ctx, component, executedFrontends)
            }
        }
        is TranslationUnitPass -> {
            for (component in result.components) {
                for (tu in component.translationUnits) {
                    executePass(
                        (prototype as TranslationUnitPass)::class,
                        ctx,
                        tu,
                        executedFrontends
                    )
                }
            }
        }
    }
}

inline fun <reified T : PassTarget> executePass(
    cls: KClass<out Pass<T>>,
    ctx: TranslationContext,
    target: T,
    executedFrontends: Collection<LanguageFrontend<*, *>>
): Pass<T>? {
    val language =
        if (target is LanguageProvider) {
            target.language
        } else {
            null
        }

    val realClass = checkForReplacement(cls, language, ctx.config)

    val pass = realClass.primaryConstructor?.call(ctx)
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
    if (language == null) {
        return cls
    }

    return config.replacedPasses[Pair(cls, language::class)] as? KClass<out Pass<T>> ?: cls
}
