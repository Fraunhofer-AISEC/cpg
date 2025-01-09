/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.frontends.SupportsParallelParsing
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.passes.*
import java.io.File
import java.io.PrintWriter
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.full.findAnnotation
import org.slf4j.LoggerFactory

/** Main entry point for all source code translation for all language front-ends. */
class TranslationManager
private constructor(
    /**
     * Returns the current (immutable) configuration of this TranslationManager.
     *
     * @return the configuration
     */
    val config: TranslationConfiguration
) {
    private val isCancelled = AtomicBoolean(false)

    /**
     * Kicks off the analysis.
     *
     * This method orchestrates all passes that will do the main work.
     *
     * @return a [CompletableFuture] with the [TranslationResult].
     */
    fun analyze(): CompletableFuture<TranslationResult> {
        // We wrap the analysis in a CompletableFuture, i.e. in an async task.
        return CompletableFuture.supplyAsync { analyzeNonAsync() }
    }

    private fun analyzeNonAsync(): TranslationResult {
        var executedFrontends = setOf<LanguageFrontend<*, *>>()

        // Build a new global translation context
        val ctx = TranslationContext(config, ScopeManager(), TypeManager())

        // Build a new translation result
        val result = TranslationResult(this, ctx)

        val outerBench =
            Benchmark(TranslationManager::class.java, "Translation into full graph", false, result)

        try {
            // Parse Java/C/CPP files
            val bench = Benchmark(this.javaClass, "Executing Language Frontend", false, result)
            executedFrontends = runFrontends(ctx, result)
            bench.addMeasurement()

            if (config.useParallelPasses) {
                // Execute list of parallel passes together in parallel
                for (list in config.registeredPasses) {
                    executePassesInParallel(list, ctx, result, executedFrontends)
                    if (result.isCancelled) {
                        log.warn("Analysis interrupted, stopping Pass evaluation")
                    }
                }
            } else {
                // Execute all passes in sequence
                for (pass in config.registeredPasses.flatten()) {
                    executePass(pass, ctx, result, executedFrontends)
                    if (result.isCancelled) {
                        log.warn("Analysis interrupted, stopping Pass evaluation")
                    }
                }
            }
        } catch (ex: TranslationException) {
            throw CompletionException(ex)
        } finally {
            outerBench.addMeasurement()
            if (!config.disableCleanup) {
                log.debug("Cleaning up {} Frontends", executedFrontends.size)

                executedFrontends.forEach { it.cleanup() }
            }
        }

        return result
    }

    fun isCancelled(): Boolean {
        return isCancelled.get()
    }

    /**
     * Parses all language files using the respective [LanguageFrontend] and creates the initial set
     * of AST nodes.
     *
     * @param result the translation result that is being mutated
     * @param ctx the translation context
     * @throws TranslationException if the language front-end runs into an error and
     *   [TranslationConfiguration.failOnError]
     * * is `true`.
     */
    @Throws(TranslationException::class)
    private fun runFrontends(
        ctx: TranslationContext,
        result: TranslationResult,
    ): Set<LanguageFrontend<*, *>> {
        val usedFrontends = mutableSetOf<LanguageFrontend<*, *>>()
        for (sc in ctx.config.softwareComponents.keys) {
            val component = Component()
            component.name = Name(sc)
            result.addComponent(component)

            var sourceLocations: List<File> = ctx.config.softwareComponents[sc] ?: listOf()

            var useParallelFrontends = ctx.config.useParallelFrontends

            val list =
                sourceLocations.flatMap { file ->
                    if (file.isDirectory) {
                        val files =
                            file
                                .walkTopDown()
                                .onEnter { !it.name.startsWith(".") }
                                .filter { it.isFile && !it.name.startsWith(".") }
                                .filter {
                                    ctx.config.exclusionPatternsByString.none { pattern ->
                                        it.absolutePath.contains(pattern)
                                    }
                                }
                                .filter {
                                    ctx.config.exclusionPatternsByRegex.none { pattern ->
                                        pattern.containsMatchIn(it.absolutePath)
                                    }
                                }
                                .toList()
                        files
                    } else {
                        val frontendClass = file.language?.frontend
                        val supportsParallelParsing =
                            file.language
                                ?.frontend
                                ?.findAnnotation<SupportsParallelParsing>()
                                ?.supported ?: true
                        // By default, the frontends support parallel parsing. But the
                        // SupportsParallelParsing annotation can be set to false and force
                        // to disable it.
                        if (useParallelFrontends && !supportsParallelParsing) {
                            log.warn(
                                "Parallel frontends are not yet supported for the language frontend ${frontendClass?.simpleName}"
                            )
                            useParallelFrontends = false
                        }
                        listOf(file)
                    }
                }
            if (ctx.config.useUnityBuild) {
                // If we only have C files in the list, we need to make sure that our unity build is
                // also a .c file
                val cFiles = list.filter { it.extension == "c" }
                val cOnly = cFiles.size == list.size

                val tmpFile =
                    Files.createTempFile(
                            "compile",
                            if (cOnly) {
                                ".c"
                            } else {
                                ".cpp"
                            },
                        )
                        .toFile()
                tmpFile.deleteOnExit()

                PrintWriter(tmpFile).use { writer ->
                    list.forEach {
                        val cxxExtensions = listOf("c", "cpp", "cc", "cxx")
                        if (cxxExtensions.contains(it.extension)) {
                            if (ctx.config.topLevel != null) {
                                val topLevel = ctx.config.topLevel.toPath()
                                writer.write(
                                    """
#include "${topLevel.relativize(it.toPath())}"

"""
                                        .trimIndent()
                                )
                            } else {
                                writer.write(
                                    """
#include "${it.absolutePath}"

"""
                                        .trimIndent()
                                )
                            }
                        }
                    }
                }

                sourceLocations = listOf(tmpFile)
                if (ctx.config.compilationDatabase != null) {
                    // merge include paths from all translation units
                    ctx.config.compilationDatabase.addIncludePath(
                        tmpFile,
                        ctx.config.compilationDatabase.allIncludePaths,
                    )
                }
            } else {
                sourceLocations = list
            }

            usedFrontends.addAll(
                if (useParallelFrontends) {
                    parseParallel(component, result, ctx, sourceLocations)
                } else {
                    parseSequentially(component, result, ctx, sourceLocations)
                }
            )
        }

        return usedFrontends
    }

    private fun parseParallel(
        component: Component,
        result: TranslationResult,
        globalCtx: TranslationContext,
        sourceLocations: Collection<File>,
    ): Set<LanguageFrontend<*, *>> {
        val usedFrontends = mutableSetOf<LanguageFrontend<*, *>>()

        log.info("Parallel parsing started")
        val futures = mutableListOf<CompletableFuture<LanguageFrontend<*, *>?>>()
        val parallelContexts = mutableListOf<TranslationContext>()

        val futureToFile: MutableMap<CompletableFuture<LanguageFrontend<*, *>?>, File> =
            IdentityHashMap()

        for (sourceLocation in sourceLocations) {
            // Build a new translation context for this parallel parsing process. We need to do this
            // until we can use a single scope manager concurrently. We can re-use the global
            // configuration and type manager.
            val ctx =
                TranslationContext(
                    globalCtx.config,
                    ScopeManager(),
                    globalCtx.typeManager,
                    component,
                )
            parallelContexts.add(ctx)

            val future =
                CompletableFuture.supplyAsync {
                    try {
                        return@supplyAsync parse(component, ctx, sourceLocation)
                    } catch (e: TranslationException) {
                        throw RuntimeException("Error parsing $sourceLocation", e)
                    }
                }

            futures.add(future)
            futureToFile[future] = sourceLocation
        }

        for (future in futures) {
            try {
                val f = future.get()
                if (f != null) {
                    handleCompletion(result, usedFrontends, futureToFile[future], f)
                }
            } catch (e: InterruptedException) {
                log.error("Error parsing ${futureToFile[future]}", e)
                Thread.currentThread().interrupt()
            } catch (e: ExecutionException) {
                log.error("Error parsing ${futureToFile[future]}", e)
                // We previously called Thread.currentThread().interrupt here, however
                // it is unsure, why. Therefore, instead of just removing this line, we
                // "disabled" it and left this comment here for future generations. If
                // we see that it is really not needed we can remove it completely at some
                // point.
                // Thread.currentThread().interrupt()
            }
        }

        // We want to merge everything into the final scope manager of the result
        globalCtx.scopeManager.mergeFrom(parallelContexts.map { it.scopeManager })

        // We also need to update all types that point to one of the "old" global scopes
        // TODO(oxisto): This is really messy and instead we should have ONE global scope
        //  and individual file scopes beneath it
        var newGlobalScope = globalCtx.scopeManager.globalScope
        var types =
            globalCtx.typeManager.firstOrderTypes.union(globalCtx.typeManager.secondOrderTypes)
        types.forEach {
            if (it.scope is GlobalScope) {
                it.scope = newGlobalScope
            }
        }

        log.info("Parallel parsing completed")

        return usedFrontends
    }

    @Throws(TranslationException::class)
    private fun parseSequentially(
        component: Component,
        result: TranslationResult,
        ctx: TranslationContext,
        sourceLocations: Collection<File>,
    ): Set<LanguageFrontend<*, *>> {
        val usedFrontends = mutableSetOf<LanguageFrontend<*, *>>()

        for (sourceLocation in sourceLocations) {
            ctx.currentComponent = component
            val f = parse(component, ctx, sourceLocation)
            if (f != null) {
                handleCompletion(result, usedFrontends, sourceLocation, f)
            }
        }

        return usedFrontends
    }

    private fun handleCompletion(
        result: TranslationResult,
        usedFrontends: MutableSet<LanguageFrontend<*, *>>,
        sourceLocation: File?,
        f: LanguageFrontend<*, *>,
    ) {
        usedFrontends.add(f)

        // remember which frontend parsed each file
        val sfToFe =
            result.scratch.computeIfAbsent(TranslationResult.SOURCE_LOCATIONS_TO_FRONTEND) {
                mutableMapOf<String, String>()
            } as MutableMap<String, String>
        sourceLocation?.name?.let { sfToFe[it] = f.javaClass.simpleName }
    }

    @Throws(TranslationException::class)
    private fun parse(
        component: Component,
        ctx: TranslationContext,
        sourceLocation: File,
    ): LanguageFrontend<*, *>? {
        log.info("Parsing {}", sourceLocation.absolutePath)

        var frontend: LanguageFrontend<*, *>? = null
        try {
            frontend = getFrontend(sourceLocation, ctx)

            if (frontend == null) {
                log.error("Found no parser frontend for ${sourceLocation.name}")

                if (config.failOnError) {
                    throw TranslationException(
                        "Found no parser frontend for ${sourceLocation.name}"
                    )
                }
                return null
            }
            component.addTranslationUnit(frontend.parse(sourceLocation))
        } catch (ex: TranslationException) {
            log.error("An error occurred during parsing of ${sourceLocation.name}: ${ex.message}")
            if (config.failOnError) {
                throw ex
            }
        }
        return frontend
    }

    private fun getFrontend(file: File, ctx: TranslationContext): LanguageFrontend<*, *>? {
        val language = file.language

        return if (language != null) {
            try {
                // Make sure, that our simple types are also known to the type manager
                language.builtInTypes.values.forEach { ctx.typeManager.registerType(it) }

                // Return a new language frontend
                language.newFrontend(ctx)
            } catch (e: Exception) {
                when (e) {
                    is InstantiationException,
                    is IllegalAccessException,
                    is InvocationTargetException,
                    is NoSuchMethodException -> {
                        log.error(
                            "Could not instantiate language frontend {}",
                            language.frontend.simpleName,
                            e,
                        )
                        null
                    }
                    else -> throw e
                }
            }
        } else null
    }

    /**
     * This extension function returns an appropriate [Language] for this [File] based on the
     * registered file extensions of [TranslationConfiguration.languages]. It will emit a warning if
     * multiple languages are registered for the same extension (and the first one that was
     * registered will be returned).
     */
    private val File.language: Language<*>?
        get() {
            val languages = config.languages.filter { it.handlesFile(this) }
            if (languages.size > 1) {
                log.warn(
                    "Multiple languages match for file extension ${this.extension}, the first registered language will be used."
                )
            }
            return languages.firstOrNull()
        }

    class Builder {
        private var config: TranslationConfiguration? = null

        fun config(config: TranslationConfiguration): Builder {
            this.config = config
            return this
        }

        fun build(): TranslationManager {
            return TranslationManager(config ?: TranslationConfiguration.builder().build())
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TranslationManager::class.java)

        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}
