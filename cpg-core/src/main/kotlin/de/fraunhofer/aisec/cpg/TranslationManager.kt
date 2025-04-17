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

import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.executePass
import de.fraunhofer.aisec.cpg.passes.executePassesInParallel
import de.fraunhofer.aisec.cpg.sarif.toLocation
import java.io.File
import java.io.PrintWriter
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayDeque
import kotlin.io.path.name
import kotlin.reflect.KClass
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
        val ctx = TranslationContext(config)

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
                // Execute all passes in sequence. First convert the list of passes to a queue
                val queue = ArrayDeque<KClass<out Pass<out Node>>>()
                queue.addAll(config.registeredPasses.flatten())
                while (queue.isNotEmpty()) {
                    // Get the next pass from the queue
                    val pass = queue.removeFirst()
                    executePass(pass, ctx, result, executedFrontends)

                    // After each pass execution, identify "dirty" nodes and identify which passes
                    // should be run afterward
                    var scheduledPasses = result.dirtyNodes.values.flatten()
                    for (scheduledPass in scheduledPasses) {
                        // If the pass is already in the queue, ignore it
                        if (scheduledPass in queue) {
                            continue
                        }

                        // Otherwise, add it to the queue
                        queue.addFirst(scheduledPass)
                    }

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

        // If loadIncludes is active, the files stored in the include paths are made available for
        // conditional analysis by providing them to the frontends over the
        // [TranslationContext.additionalSources] list.
        if (ctx.config.loadIncludes) {
            ctx.config.includePaths.forEach {
                ctx.additionalSources.addAll(extractAdditionalSources(it.toFile()))
            }
        }

        var useParallelFrontends = ctx.config.useParallelFrontends

        for (sc in ctx.config.softwareComponents.keys) {
            val component = Component()
            component.name = Name(sc)
            component.location = with(ctx) { component.topLevel()?.toPath()?.toLocation() }
            result.addComponent(component)

            var sourceLocations: List<File> = ctx.config.softwareComponents[sc] ?: listOf()

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
                        // Retrieve the file's language based on the available languages of the
                        // result
                        val language = with(ctx) { file.language }
                        val frontendClass = language?.frontend
                        val supportsParallelParsing =
                            frontendClass?.findAnnotation<SupportsParallelParsing>()?.supported !=
                                false

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
                    list.forEach { file ->
                        val cxxExtensions = listOf("c", "cpp", "cc", "cxx")
                        if (cxxExtensions.contains(file.extension)) {
                            ctx.config.topLevels[sc]?.let {
                                val topLevel = it.toPath()
                                writer.write(
                                    """
#include "${topLevel.relativize(file.toPath())}"

"""
                                        .trimIndent()
                                )
                            }
                                ?: run {
                                    writer.write(
                                        """
#include "${file.absolutePath}"

"""
                                            .trimIndent()
                                    )
                                }
                        }
                    }
                }

                sourceLocations = listOf(tmpFile)
                ctx.config.compilationDatabase?.addIncludePath(
                    tmpFile,
                    ctx.config.compilationDatabase.allIncludePaths,
                )
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

            // Collects all used languages used in the main analysis code
            result.usedLanguages.addAll(
                sourceLocations.mapNotNull { with(ctx) { it.language } }.toSet()
            )
        }

        // Adds all languages provided as additional sources that may be relevant in the main code
        result.usedLanguages.addAll(
            ctx.additionalSources.mapNotNull { with(ctx) { it.relative.language } }.toSet()
        )

        result.usedLanguages.filterIsInstance<HasBuiltins>().forEach { hasBuiltins ->
            // Includes a file in the analysis, if relative to its rootpath it matches the name of
            // a builtins file candidate.
            val builtinsCandidates = hasBuiltins.builtinsFileCandidates
            ctx.additionalSources
                .filter { builtinsCandidates.contains(it.relative) }
                .forEach { ctx.importedSources.add(it) }
        }

        // A set of processed files from [TranslationContext.additionalSources] that is used as
        // negative to the
        // worklist in ctx.importedSources it is used to filter out files that were already
        // processed and to
        // detect if new files were analyzed.
        val processedAdditionalSources: MutableList<AdditionalSource> = mutableListOf()

        do {
            val oldProcessedSize = processedAdditionalSources.size

            // Distribute all files by their root path prefix, parse them in individual component
            // named like their rootPath local name
            ctx.config.includePaths.forEach { includePath ->
                val unprocessedFilesInIncludePath =
                    ctx.importedSources
                        .filter { !processedAdditionalSources.contains(it) }
                        .filter { it.includePath == includePath.toFile().canonicalFile }
                if (unprocessedFilesInIncludePath.isNotEmpty()) {
                    val compName = Name(includePath.name)
                    var component = result.components.firstOrNull { it.name == compName }
                    if (component == null) {
                        component = Component()
                        component.name = compName
                        component.location = includePath.toLocation()
                        result.addComponent(component)
                        ctx.config.topLevels.put(includePath.name, includePath.toFile())
                    }

                    usedFrontends.addAll(
                        if (useParallelFrontends) {
                            parseParallel(
                                component,
                                result,
                                ctx,
                                unprocessedFilesInIncludePath.map { it.absolute },
                            )
                        } else {
                            parseSequentially(
                                component,
                                result,
                                ctx,
                                unprocessedFilesInIncludePath.map { it.absolute },
                            )
                        }
                    )
                    processedAdditionalSources.addAll(unprocessedFilesInIncludePath)
                }
            }
            // If the last run added files to the processed list, we do another run
        } while (processedAdditionalSources.size > oldProcessedSize)

        return usedFrontends
    }

    /**
     * Extracts all files from the given include path as an [AdditionalSource]. If the path is a
     * directory, all files in the directory are returned. If the path is a single file, the file
     * itself is returned.
     */
    private fun extractAdditionalSources(includePath: File): List<AdditionalSource> {
        return when {
            !includePath.exists() -> listOf()
            includePath.isDirectory ->
                includePath.walkTopDown().toList().map {
                    AdditionalSource(it.relativeTo(includePath), includePath.canonicalFile)
                }
            else ->
                listOf(
                    AdditionalSource(includePath.relativeTo(includePath), includePath.canonicalFile)
                )
        }
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
            val ctx = TranslationContext(globalCtx.config, globalCtx.typeManager, component)
            parallelContexts.add(ctx)

            val future =
                CompletableFuture.supplyAsync {
                    try {
                        return@supplyAsync parse(component, ctx, globalCtx, sourceLocation)
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

        var b =
            Benchmark(
                TranslationManager::class.java,
                "Merging type and scope information to final context",
            )

        // We want to merge everything into the final scope manager of the result
        globalCtx.scopeManager.mergeFrom(parallelContexts.map { it.scopeManager })
        b.stop()

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
            val f = parse(component, ctx, ctx, sourceLocation)
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
        globalCtx: TranslationContext,
        sourceLocation: File,
    ): LanguageFrontend<*, *>? {
        log.info("Parsing {}", sourceLocation.absolutePath)

        var frontend: LanguageFrontend<*, *>? = null
        try {
            frontend = getFrontend(sourceLocation, ctx, globalCtx)

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

    private fun getFrontend(
        file: File,
        ctx: TranslationContext,
        globalCtx: TranslationContext,
    ): LanguageFrontend<*, *>? {
        // Retrieve the languages based on the global ctx, so that all frontends share the same
        // language instances.
        //
        // Once we address https://github.com/Fraunhofer-AISEC/cpg/issues/2109 we can remove the
        // globalCtx parameter
        val language = with(globalCtx) { file.language }

        return if (language != null) {
            try {
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
     * An additional source file that was originally part of [TranslationConfiguration.includePaths]
     * and that is potentially included in the analysis.
     *
     * To make it easier for language frontends to match specific patterns on this file, e.g.,
     * whether its path is corresponding to a package structure, we provide a path (relative to the
     * original include path).
     */
    data class AdditionalSource(val relative: File, val includePath: File) {
        /**
         * Returns the absolute path of this [AdditionalSource] by resolving the relative path
         * against the include path.
         */
        val absolute: File
            get() = includePath.resolve(relative).canonicalFile
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
        internal val log = LoggerFactory.getLogger(TranslationManager::class.java)

        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}

/**
 * This function loops through the list of [Type] nodes and updates the [Type.scope] of all nodes
 * that have a [GlobalScope] as their scope to [newGlobalScope].
 *
 * This is needed because we currently have multiple global scopes (one per [ScopeManager]) and we
 * need to update all types with the merged global scope.
 */
private fun MutableList<Type>.updateGlobalScope(newGlobalScope: GlobalScope?) {
    for (type in this) {
        if (type.scope is GlobalScope) {
            type.scope = newGlobalScope
        }

        type.secondOrderTypes.updateGlobalScope(newGlobalScope)
    }
}
