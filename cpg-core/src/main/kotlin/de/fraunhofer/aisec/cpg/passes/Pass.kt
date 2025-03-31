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
import de.fraunhofer.aisec.cpg.frontends.LanguageTrait
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteFirst
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLast
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate
import de.fraunhofer.aisec.cpg.passes.configuration.RequiredFrontend
import de.fraunhofer.aisec.cpg.passes.configuration.RequiresLanguageTrait
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import org.apache.commons.lang3.builder.ToStringBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A [TranslationResultPass] is a pass that operates on a [TranslationResult]. If used with
 * [executePass], one [Pass] object is instantiated for the whole [TranslationResult].
 */
abstract class TranslationResultPass(
    ctx: TranslationContext,
    sort: Sorter<TranslationResult> = TranslationResultSorter,
) : Pass<TranslationResult>(ctx, sort)

/**
 * A [ComponentPass] is a pass that operates on a [Component]. If used with [executePass], one
 * [Pass] object is instantiated for each [Component] in a [TranslationResult].
 */
abstract class ComponentPass(
    ctx: TranslationContext,
    sort: Sorter<Component> = LeastImportComponentSorter,
) : Pass<Component>(ctx, sort)

/**
 * A [TranslationUnitPass] is a pass that operates on a [TranslationUnitDeclaration]. If used with
 * [executePass], one [Pass] object is instantiated for each [TranslationUnitDeclaration] in a
 * [Component].
 */
abstract class TranslationUnitPass(
    ctx: TranslationContext,
    sort: Sorter<TranslationUnitDeclaration> = LeastImportTranslationUnitSorter,
) : Pass<TranslationUnitDeclaration>(ctx, sort)

/**
 * A [EOGStarterPass] is a pass that operates on nodes that are contained in a [EOGStarterHolder].
 * If used with [executePass], one [Pass] object is instantiated for each [Node] in a
 * [EOGStarterHolder] in each [TranslationUnitDeclaration] in each [Component].
 */
abstract class EOGStarterPass(ctx: TranslationContext, sort: Sorter<Node> = EOGStarterSorter) :
    Pass<Node>(ctx, sort)

open class PassConfiguration

/** Implementations of this abstract class sort nodes before they are passed to the [Pass]es. */
abstract class Sorter<T : Node> : (TranslationResult) -> List<T>

/**
 * This class is only used to provide a common interface also for [TranslationResultPass]es. It will
 * return a list containing only the [TranslationResult].
 */
object TranslationResultSorter : Sorter<TranslationResult>() {
    override fun invoke(result: TranslationResult): List<TranslationResult> = listOf(result)
}

/**
 * Execute the [Component]s in the "sorted" order (if available). They are sorted based on least
 * import dependencies.
 */
object LeastImportComponentSorter : Sorter<Component>() {
    override fun invoke(result: TranslationResult): List<Component> =
        (Strategy::COMPONENTS_LEAST_IMPORTS)(result).asSequence().toList()
}

/**
 * Execute the [TranslationUnitDeclaration]s in the "sorted" order (if available) w.r.t. least
 * import dependencies. To do so, it first sorts the [Component]s using the
 * [LeastImportComponentSorter] and then decides on their [TranslationUnitDeclaration]s.
 */
object LeastImportTranslationUnitSorter : Sorter<TranslationUnitDeclaration>() {
    override fun invoke(result: TranslationResult): List<TranslationUnitDeclaration> =
        LeastImportComponentSorter.invoke(result)
            .flatMap { (Strategy::TRANSLATION_UNITS_LEAST_IMPORTS)(it).asSequence() }
            .toList()
}

/**
 * First, sorts the [TranslationUnitDeclaration]s with the [LeastImportTranslationUnitSorter] and
 * then gathers all resolution EOG starters; and make sure they really do not have a predecessor,
 * otherwise we might analyze a node multiple times. Note that the [EOGStarterHolder]s are not
 * sorted.
 */
object EOGStarterSorter : Sorter<Node>() {
    override fun invoke(result: TranslationResult): List<Node> =
        LeastImportTranslationUnitSorter.invoke(result)
            .flatMap { it.allEOGStarters.filter { it.prevEOGEdges.isEmpty() } }
            .toList()
}

/**
 * Represents an abstract class that enhances the graph before it is persisted. Passes can exist at
 * different levels:
 * - the overall [TranslationResult]
 * - a [Component],
 * - a [TranslationUnitDeclaration], and
 * - a [EOGStarterHolder].
 *
 * A level should be chosen as granular as possible, to allow for the (future) parallel execution of
 * passes. Instead of directly subclassing this type, one of the types [TranslationResultPass],
 * [ComponentPass] or [TranslationUnitPass] must be used.
 *
 * [sort] can be used to specify the order in which the Pass will visit/process the nodes.
 */
sealed class Pass<T : Node>(final override val ctx: TranslationContext, val sort: Sorter<T>) :
    Consumer<T>, ContextProvider, RawNodeTypeProvider<Nothing>, ScopeProvider {
    var name: String
        protected set

    val config: TranslationConfiguration = ctx.config
    val scopeManager: ScopeManager = ctx.scopeManager
    val typeManager: TypeManager = ctx.typeManager

    init {
        name = this.javaClass.name
    }

    /**
     * The current [Scope] of the [scopeManager]. Please note, that each pass is responsible for
     * actually setting the correct scope within the [scopeManager], e.g., by using the
     * [ScopedWalker].
     */
    override val scope: Scope?
        get() = scopeManager.currentScope

    abstract fun cleanup()

    /**
     * Check if the pass requires a specific language frontend and if that frontend has been
     * executed.
     *
     * @return true, if the pass does not require a specific language frontend or if it matches the
     *   [RequiredFrontend]
     */
    fun runsWithCurrentFrontend(usedFrontends: Collection<LanguageFrontend<*, *>>): Boolean {
        val requiredFrontend = this::class.findAnnotation<RequiredFrontend>() ?: return true
        for (used in usedFrontends) {
            if (used::class == requiredFrontend.value) return true
        }
        return false
    }

    /**
     * Checks, if the pass requires a specific [LanguageTrait] and if the current target of the pass
     * has this trait.
     *
     * @return true, if the pass does not require a specific language trait or if it matches the
     *   [RequiresLanguageTrait].
     */
    fun runsWithLanguageTrait(language: Language<*>?): Boolean {
        if (language == null) {
            return true
        }

        val requiresLanguageTraits = this::class.findAnnotations<RequiresLanguageTrait>()
        for (requiresLanguageTrait in requiresLanguageTraits) {
            if (!language::class.isSubclassOf(requiresLanguageTrait.value)) {
                return false
            }
        }

        return true
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(Pass::class.java)
    }

    fun <T : PassConfiguration> passConfig(): T? {
        return this.config.passConfigurations[this::class] as? T
    }

    override fun toString(): String {
        val builder =
            ToStringBuilder(this, Node.TO_STRING_STYLE).append("pass", this::class.simpleName)

        if (this::class.softDependencies.isNotEmpty()) {
            builder.append("soft dependencies:", this::class.softDependencies.map { it.simpleName })
        }

        if (this::class.hardDependencies.isNotEmpty()) {
            builder.append("hard dependencies:", this::class.hardDependencies.map { it.simpleName })
        }

        if (this::class.softExecuteBefore.isNotEmpty()) {
            builder.append(
                "execute before (soft): ",
                this::class.softExecuteBefore.map { it.simpleName },
            )
        }

        if (this::class.hardExecuteBefore.isNotEmpty()) {
            builder.append(
                "execute before (hard): ",
                this::class.hardExecuteBefore.map { it.simpleName },
            )
        }

        if (this::class.isFirstPass) {
            builder.append("firstPass")
        }

        if (this::class.isLastPass) {
            builder.append("lastPass")
        }

        if (this::class.isLatePass) {
            builder.append("latePass")
        }

        return builder.toString()
    }
}

fun executePassesInParallel(
    classes: List<KClass<out Pass<*>>>,
    ctx: TranslationContext,
    result: TranslationResult,
    executedFrontends: Collection<LanguageFrontend<*, *>>,
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
            result,
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
    executedFrontends: Collection<LanguageFrontend<*, *>>,
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
                prototype.sort(result),
                executedFrontends,
            )
        is ComponentPass ->
            consumeTargets(
                (prototype as ComponentPass)::class,
                ctx,
                prototype.sort(result),
                executedFrontends,
            )
        is TranslationUnitPass ->
            consumeTargets(
                (prototype as TranslationUnitPass)::class,
                ctx,
                // Execute them in the "sorted" order (if available)
                prototype.sort(result),
                executedFrontends,
            )
        is EOGStarterPass -> {
            consumeTargets(
                (prototype as EOGStarterPass)::class,
                ctx,
                prototype.sort(result),
                executedFrontends,
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
    targets: Collection<T>,
    executedFrontends: Collection<LanguageFrontend<*, *>>,
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
    executedFrontends: Collection<LanguageFrontend<*, *>>,
): Pass<T>? {
    val language = target.language

    val realClass = checkForReplacement(cls, language, ctx.config)

    val pass = realClass.primaryConstructor?.call(ctx)
    if (
        pass?.runsWithCurrentFrontend(executedFrontends) == true &&
            pass.runsWithLanguageTrait(language)
    ) {
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
    config: TranslationConfiguration,
): KClass<out Pass<T>> {
    if (language == null) {
        return cls
    }

    @Suppress("UNCHECKED_CAST")
    return config.replacedPasses[Pair(cls, language::class)] as? KClass<out Pass<T>> ?: cls
}

val KClass<out Pass<*>>.isFirstPass: Boolean
    get() {
        return this.hasAnnotation<ExecuteFirst>()
    }

val KClass<out Pass<*>>.isLastPass: Boolean
    get() {
        return this.hasAnnotation<ExecuteLast>()
    }

val KClass<out Pass<*>>.isLatePass: Boolean
    get() {
        return this.hasAnnotation<ExecuteLate>()
    }

val KClass<out Pass<*>>.softDependencies: Set<KClass<out Pass<*>>>
    get() {
        return this.findAnnotations<DependsOn>()
            .filter { it.softDependency == true }
            .map { it.value }
            .toSet()
    }

val KClass<out Pass<*>>.hardDependencies: Set<KClass<out Pass<*>>>
    get() {
        return this.findAnnotations<DependsOn>()
            .filter { it.softDependency == false }
            .map { it.value }
            .toSet()
    }

val KClass<out Pass<*>>.softExecuteBefore: Set<KClass<out Pass<*>>>
    get() {
        return this.findAnnotations<ExecuteBefore>()
            .filter { it.softDependency == true }
            .map { it.other }
            .toSet()
    }

val KClass<out Pass<*>>.hardExecuteBefore: Set<KClass<out Pass<*>>>
    get() {
        return this.findAnnotations<ExecuteBefore>()
            .filter { it.softDependency == false }
            .map { it.other }
            .toSet()
    }
