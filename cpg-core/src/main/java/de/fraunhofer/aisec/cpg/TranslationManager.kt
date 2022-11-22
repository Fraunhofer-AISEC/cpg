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
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import java.io.File
import java.io.PrintWriter
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors
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
        val result = TranslationResult(this, ScopeManager())

        // We wrap the analysis in a CompletableFuture, i.e. in an async task.
        return CompletableFuture.supplyAsync {
            val outerBench =
                Benchmark(
                    TranslationManager::class.java,
                    "Translation into full graph",
                    false,
                    result
                )
            val executedPasses = mutableSetOf<Pass>()
            var executedFrontends = setOf<LanguageFrontend>()

            try {
                // Parse Java/C/CPP files
                var bench = Benchmark(this.javaClass, "Executing Language Frontend", false, result)
                executedFrontends = runFrontends(result, config)
                bench.addMeasurement()

                // Apply passes
                for (pass in config.registeredPasses) {
                    bench = Benchmark(pass.javaClass, "Executing Pass", false, result)
                    if (pass.runsWithCurrentFrontend(executedFrontends)) {
                        executedPasses.add(pass)
                        pass.accept(result)
                    }
                    bench.addMeasurement()
                    if (result.isCancelled) {
                        log.warn("Analysis interrupted, stopping Pass evaluation")
                    }
                }
            } catch (ex: TranslationException) {
                throw CompletionException(ex)
            } finally {
                outerBench.addMeasurement()
                if (!config.disableCleanup) {
                    log.debug("Cleaning up {} Passes", executedPasses.size)

                    executedPasses.forEach { it.cleanup() }

                    log.debug("Cleaning up {} Frontends", executedFrontends.size)

                    executedFrontends.forEach { it.cleanup() }
                    TypeManager.getInstance().cleanup()
                }
            }
            result
        }
    }

    val passes: List<Pass>
        get() = config.registeredPasses

    fun isCancelled(): Boolean {
        return isCancelled.get()
    }

    /**
     * Parses all language files using the respective [LanguageFrontend] and creates the initial set
     * of AST nodes.
     *
     * @param result the translation result that is being mutated
     * @param config the translation configuration
     * @throws TranslationException if the language front-end runs into an error and
     * [TranslationConfiguration.failOnError]
     * * is `true`.
     */
    @Throws(TranslationException::class)
    private fun runFrontends(
        result: TranslationResult,
        config: TranslationConfiguration,
    ): Set<LanguageFrontend> {
        val usedFrontends = mutableSetOf<LanguageFrontend>()
        for (sc in this.config.softwareComponents.keys) {
            val component = Component()
            component.name = sc
            result.addComponent(component)

            var sourceLocations: List<File> = this.config.softwareComponents[sc]!!

            var useParallelFrontends = config.useParallelFrontends

            val list =
                sourceLocations.flatMap { file ->
                    if (file.isDirectory) {
                        Files.find(
                                file.toPath(),
                                999,
                                { _: Path?, fileAttr: BasicFileAttributes ->
                                    fileAttr.isRegularFile
                                }
                            )
                            .map { it.toFile() }
                            .collect(Collectors.toList())
                    } else {
                        val frontendClass = file.language?.frontend
                        val supportsParallelParsing =
                            file.language
                                ?.frontend
                                ?.findAnnotation<SupportsParallelParsing>()
                                ?.supported
                                ?: true
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
            if (config.useUnityBuild) {
                val tmpFile = Files.createTempFile("compile", ".cpp").toFile()
                tmpFile.deleteOnExit()

                PrintWriter(tmpFile).use { writer ->
                    list.forEach {
                        if (CXXLanguageFrontend.CXX_EXTENSIONS.contains(Util.getExtension(it))) {
                            if (config.topLevel != null) {
                                val topLevel = config.topLevel.toPath()
                                writer.write(
                                    """
#include "${topLevel.relativize(it.toPath())}"

""".trimIndent(
                                    )
                                )
                            } else {
                                writer.write("""
#include "${it.absolutePath}"

""".trimIndent())
                            }
                        }
                    }
                }

                sourceLocations = listOf(tmpFile)
            } else {
                sourceLocations = list
            }

            TypeManager.setTypeSystemActive(config.typeSystemActiveInFrontend)

            usedFrontends.addAll(
                if (useParallelFrontends) {
                    parseParallel(component, result, sourceLocations)
                } else {
                    parseSequentially(component, result, sourceLocations)
                }
            )

            if (!config.typeSystemActiveInFrontend) {
                TypeManager.setTypeSystemActive(true)

                result.components.forEach { s ->
                    s.translationUnits.forEach {
                        val bench =
                            Benchmark(this.javaClass, "Activating types for ${it.name}", true)
                        result.scopeManager.activateTypes(it)
                        bench.stop()
                    }
                }
            }
        }

        return usedFrontends
    }

    private fun parseParallel(
        component: Component,
        result: TranslationResult,
        sourceLocations: Collection<File>
    ): Set<LanguageFrontend> {
        val usedFrontends = mutableSetOf<LanguageFrontend>()

        log.info("Parallel parsing started")
        val futures = mutableListOf<CompletableFuture<Optional<LanguageFrontend>>>()
        val parallelScopeManagers = mutableListOf<ScopeManager>()

        val futureToFile: MutableMap<CompletableFuture<Optional<LanguageFrontend>>, File> =
            IdentityHashMap()

        for (sourceLocation in sourceLocations) {
            val scopeManager = ScopeManager()
            parallelScopeManagers.add(scopeManager)

            val future =
                CompletableFuture.supplyAsync {
                    try {
                        return@supplyAsync parse(component, scopeManager, sourceLocation)
                    } catch (e: TranslationException) {
                        throw RuntimeException("Error parsing $sourceLocation", e)
                    }
                }

            futures.add(future)
            futureToFile[future] = sourceLocation
        }

        for (future in futures) {
            try {
                future.get().ifPresent { f: LanguageFrontend ->
                    handleCompletion(result, usedFrontends, futureToFile[future], f)
                }
            } catch (e: InterruptedException) {
                log.error("Error parsing ${futureToFile[future]}", e)
                Thread.currentThread().interrupt()
            } catch (e: ExecutionException) {
                log.error("Error parsing ${futureToFile[future]}", e)
                Thread.currentThread().interrupt()
            }
        }

        // We want to merge everything into the final scope manager of the result
        result.scopeManager.mergeFrom(parallelScopeManagers)

        log.info("Parallel parsing completed")

        return usedFrontends
    }

    @Throws(TranslationException::class)
    private fun parseSequentially(
        component: Component,
        result: TranslationResult,
        sourceLocations: Collection<File>
    ): Set<LanguageFrontend> {
        val usedFrontends = mutableSetOf<LanguageFrontend>()

        for (sourceLocation in sourceLocations) {
            log.info("Parsing {}", sourceLocation.absolutePath)

            parse(component, result.scopeManager, sourceLocation).ifPresent { f: LanguageFrontend ->
                handleCompletion(result, usedFrontends, sourceLocation, f)
            }
        }

        return usedFrontends
    }

    private fun handleCompletion(
        result: TranslationResult,
        usedFrontends: MutableSet<LanguageFrontend>,
        sourceLocation: File?,
        f: LanguageFrontend
    ) {
        usedFrontends.add(f)

        // remember which frontend parsed each file
        val sfToFe =
            result.scratch.computeIfAbsent(TranslationResult.SOURCE_LOCATIONS_TO_FRONTEND) {
                mutableMapOf<String, String>()
            } as MutableMap<String, String>
        sfToFe[sourceLocation!!.name] = f.javaClass.simpleName
    }

    @Throws(TranslationException::class)
    private fun parse(
        component: Component,
        scopeManager: ScopeManager,
        sourceLocation: File
    ): Optional<LanguageFrontend> {
        var frontend: LanguageFrontend? = null
        try {
            frontend = getFrontend(sourceLocation, scopeManager)

            if (frontend == null) {
                log.error("Found no parser frontend for ${sourceLocation.name}")

                if (config.failOnError) {
                    throw TranslationException(
                        "Found no parser frontend for ${sourceLocation.name}"
                    )
                }
                return Optional.empty()
            }
            component.translationUnits.add(frontend.parse(sourceLocation))
        } catch (ex: TranslationException) {
            log.error("An error occurred during parsing of ${sourceLocation.name}: ${ex.message}")
            if (config.failOnError) {
                throw ex
            }
        }
        return Optional.ofNullable(frontend)
    }

    private fun getFrontend(file: File, scopeManager: ScopeManager): LanguageFrontend? {
        val language = file.language

        return if (language != null) {
            try {
                language.newFrontend(config, scopeManager)
            } catch (e: Exception) {
                when (e) {
                    is InstantiationException,
                    is IllegalAccessException,
                    is InvocationTargetException,
                    is NoSuchMethodException -> {
                        log.error(
                            "Could not instantiate language frontend {}",
                            language.frontend.simpleName,
                            e
                        )
                        null
                    }
                    else -> throw e
                }
            }
        } else null
    }

    private val File.language: Language<*>?
        get() {
            return config.languages.firstOrNull { it.handlesFile(this) }
        }

    class Builder {
        private var config: TranslationConfiguration = TranslationConfiguration.builder().build()

        fun config(config: TranslationConfiguration): Builder {
            this.config = config
            return this
        }

        fun build(): TranslationManager {
            return TranslationManager(config)
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
