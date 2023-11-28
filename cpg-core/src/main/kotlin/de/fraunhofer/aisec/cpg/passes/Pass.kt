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
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.passes.order.RequiredFrontend
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A [TranslationResultPass] is a pass that operates on a [TranslationResult]. If used with
 * [executePass], one [Pass] object is instantiated for the whole [TranslationResult].
 */
abstract class TranslationResultPass(ctx: TranslationContext) : Pass<TranslationResult>(ctx)

/**
 * A [ComponentPass] is a pass that operates on a [Component]. If used with [executePass], one
 * [Pass] object is instantiated for each [Component] in a [TranslationResult].
 */
abstract class ComponentPass(ctx: TranslationContext) : Pass<Component>(ctx)

/**
 * A [TranslationUnitPass] is a pass that operates on a [TranslationUnitDeclaration]. If used with
 * [executePass], one [Pass] object is instantiated for each [TranslationUnitDeclaration] in a
 * [Component].
 */
abstract class TranslationUnitPass(ctx: TranslationContext) : Pass<TranslationUnitDeclaration>(ctx)

/**
 * A [EOGStarterPass] is a pass that operates on nodes that are contained in a [EOGStarterHolder].
 * If used with [executePass], one [Pass] object is instantiated for each [Node] in a
 * [EOGStarterHolder] in each [TranslationUnitDeclaration] in each [Component].
 */
abstract class EOGStarterPass(ctx: TranslationContext) : Pass<Node>(ctx)

open class PassConfiguration {}

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
sealed class Pass<T : Node>(final override val ctx: TranslationContext) :
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

    fun <T : PassConfiguration> passConfig(): T? {
        return this.config.passConfigurations[this::class] as? T
    }
}

fun executePassesInParallel(
    classes: List<KClass<out Pass<*>>>,
    ctx: TranslationContext,
    result: TranslationResult,
    executedFrontends: Collection<LanguageFrontend<*, *>>
) {
    // Execute a single pass directly sequentially and return
    val pass = classes.singleOrNull()
    if (pass != null) {
        executePass(pass, ctx, result, executedFrontends)
        return
    }

    // Otherwise, we build futures out of the list
    val bench =
        Benchmark(
            TranslationManager::class.java,
            "Executing Passes [${classes.map { it.simpleName }}] in parallel",
            false,
            result
        )

    val futures =
        classes.map {
            CompletableFuture.supplyAsync { executePass(it, ctx, result, executedFrontends) }
        }

    futures.map(CompletableFuture<Unit>::join)
    bench.stop()
}

/**
 * Creates a new [Pass] (based on [cls]) and executes it sequentially on all target nodes of
 * [result].
 *
 * Depending on the type of pass, this will either execute the pass directly on the overall result
 * (in case of a [TranslationUnitPass]) or loop through each component or through each translation
 * unit. The individual loop elements become the "target" of the execution of [consumeTarget].
 */
@Suppress("USELESS_CAST")
fun executePass(
    cls: KClass<out Pass<out Node>>,
    ctx: TranslationContext,
    result: TranslationResult,
    executedFrontends: Collection<LanguageFrontend<*, *>>
) {
    val bench = Benchmark(cls.java, "Executing Pass", false, result)

    // This is a bit tricky but actually better than other reflection magic. We are creating a
    // "prototype" instance of our pass class, so we can deduce certain type information more
    // easily.
    val prototype =
        cls.primaryConstructor?.call(ctx)
            ?: throw TranslationException("Could not create prototype pass")

    // Collect our "targets" based on the type and granularity of the pass and consume them by the
    // pass.
    when (prototype) {
        is TranslationResultPass ->
            consumeTargets(
                (prototype as TranslationResultPass)::class,
                ctx,
                listOf(result),
                executedFrontends
            )
        is ComponentPass ->
            consumeTargets(
                (prototype as ComponentPass)::class,
                ctx,
                result.components,
                executedFrontends
            )
        is TranslationUnitPass ->
            consumeTargets(
                (prototype as TranslationUnitPass)::class,
                ctx,
                result.components.flatMap { it.translationUnits },
                executedFrontends
            )
        is EOGStarterPass -> {
            consumeTargets(
                (prototype as EOGStarterPass)::class,
                ctx,
                result.allEOGStarters,
                executedFrontends
            )
        }
    }

    bench.stop()
}

/**
 * This function is a wrapper around [consumeTarget] to apply it to all [targets]. This is primarily
 * needed because of very delicate type inference work of the Kotlin compiler.
 *
 * Depending on the configuration of [TranslationConfiguration.useParallelPasses], the individual
 * targets will either be consumed sequentially or in parallel.
 */
private inline fun <reified T : Node> consumeTargets(
    cls: KClass<out Pass<T>>,
    ctx: TranslationContext,
    targets: List<T>,
    executedFrontends: Collection<LanguageFrontend<*, *>>
) {
    if (ctx.config.useParallelPasses) {
        val futures =
            targets.map {
                CompletableFuture.supplyAsync { consumeTarget(cls, ctx, it, executedFrontends) }
            }
        futures.forEach(CompletableFuture<Pass<T>?>::join)
    } else {
        targets.forEach { consumeTarget(cls, ctx, it, executedFrontends) }
    }
}

/**
 * This function creates a new [Pass] object, based on the class specified in [cls] and consumes the
 * [target] with the pass. The target type depends on the type of pass, e.g., a
 * [TranslationUnitDeclaration] or a whole [Component]. When passes are executed in parallel,
 * different instances of the same [Pass] class are executed at the same time (on different [target]
 * nodes) using this function.
 */
private inline fun <reified T : Node> consumeTarget(
    cls: KClass<out Pass<T>>,
    ctx: TranslationContext,
    target: T,
    executedFrontends: Collection<LanguageFrontend<*, *>>
): Pass<T>? {
    val language = target.language

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
fun <T : Node> checkForReplacement(
    cls: KClass<out Pass<T>>,
    language: Language<*>?,
    config: TranslationConfiguration
): KClass<out Pass<T>> {
    if (language == null) {
        return cls
    }

    @Suppress("UNCHECKED_CAST")
    return config.replacedPasses[Pair(cls, language::class)] as? KClass<out Pass<T>> ?: cls
}
