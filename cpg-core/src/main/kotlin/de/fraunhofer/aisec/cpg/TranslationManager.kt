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
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.*
import java.io.File
import java.io.PrintWriter
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.name
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

        // Without extracting the sources from the include paths to the external sources the feature
        // is turned off
        if (ctx.config.loadIncludes) {
            ctx.config.includePaths.forEach {
                ctx.externalSources.addAll(extractConfiguredSources(it))
            }
        }

        var useParallelFrontends = ctx.config.useParallelFrontends

        for (sc in ctx.config.softwareComponents.keys) {
            val component = Component()
            component.ctx = ctx
            component.name = Name(sc)
            result.addComponent(component)

            // Every Component needs to reprocess the sources
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
        }
        ctx.externalSources
            .firstOrNull {
                it.language?.isBuiltinsFile(
                    it.relativeTo(Util.getRootPath(it, ctx.config.includePaths).toFile())
                ) ?: false
            }
            ?.let { ctx.importedSources.add(it) }

        // A set of processed files from external sources that is used as negative to the worklist
        // in ctx.importedSources
        // it is used to filter out files that were already processed and to detect if new files
        // were analyzed.
        val processedExternalSources: MutableList<File> = mutableListOf()

        do {
            val oldProcessedSize = processedExternalSources.size
            // Distribute all files by their root path prefix, parse them in individual component
            // named
            // like their rootPath local name
            ctx.config.includePaths.forEach { includePath ->
                val unprocessedFilesInIncludePath =
                    ctx.importedSources
                        .filter { !processedExternalSources.contains(it) }
                        .filter {
                            it.path.removePrefix(includePath.toString()) != it.path.toString()
                        }
                if (unprocessedFilesInIncludePath.isNotEmpty()) {
                    val compName = Name(includePath.name)
                    var component = result.components.firstOrNull { it.name == compName }
                    if (component == null) {
                        component = Component()
                        component.ctx = ctx
                        component.name = compName
                        result.addComponent(component)
                        ctx.config.topLevels.put(includePath.name, includePath.toFile())
                    }

                    usedFrontends.addAll(
                        if (useParallelFrontends) {
                            parseParallel(component, result, ctx, unprocessedFilesInIncludePath)
                        } else {
                            parseSequentially(component, result, ctx, unprocessedFilesInIncludePath)
                        }
                    )
                    processedExternalSources.addAll(unprocessedFilesInIncludePath)
                }
            }
            // If the last run added files to the processed list, we do another run
        } while (processedExternalSources.size > oldProcessedSize)

        return usedFrontends
    }

    private fun extractConfiguredSources(path: Path): MutableList<File> {
        val rootFile = path.toFile()
        return if (rootFile.exists()) {
            if (rootFile.isDirectory) {
                rootFile.walkTopDown().toMutableList()
            } else {
                mutableListOf(rootFile)
            }
        } else {
            mutableListOf()
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

        var b =
            Benchmark(
                TranslationManager::class.java,
                "Merging type and scope information to final context",
            )

        // We want to merge everything into the final scope manager of the result
        globalCtx.scopeManager.mergeFrom(parallelContexts.map { it.scopeManager })

        // We also need to update all types that point to one of the "old" global scopes
        // TODO(oxisto): This is really messy and instead we should have ONE global scope
        //  and individual file scopes beneath it
        var newGlobalScope = globalCtx.scopeManager.globalScope
        globalCtx.typeManager.firstOrderTypes.updateGlobalScope(newGlobalScope)
        globalCtx.typeManager.secondOrderTypes.updateGlobalScope(newGlobalScope)

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
    }
}
