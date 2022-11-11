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
package de.fraunhofer.aisec.cpg

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIdentityReference
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import de.fraunhofer.aisec.cpg.frontends.CompilationDatabase
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.cpp.CLanguage
import de.fraunhofer.aisec.cpg.frontends.cpp.CPPLanguage
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.order.*
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.primaryConstructor
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.slf4j.LoggerFactory

/**
 * The configuration for the [TranslationManager] holds all information that is used during the
 * translation.
 */
class TranslationConfiguration
private constructor(
    /** Definition of additional symbols, mostly useful for C++. */
    val symbols: Map<String, String>,
    /** Source code files to parse. */
    val softwareComponents: Map<String, List<File>>,
    val topLevel: File?,
    /** Set to true to generate debug output for the parser. */
    val debugParser: Boolean,
    /**
     * Should parser/translation fail on parse/resolving errors (true) or try to continue in a
     * best-effort manner (false).
     */
    val failOnError: Boolean,
    /**
     * Set to true to transitively load include files into the CPG.
     *
     * If this value is set to false but includePaths are given, the parser will resolve
     * symbols/templates from these include, but do not load the parse tree into the CPG
     */
    val loadIncludes: Boolean,
    /**
     * Paths to look for include files.
     *
     * It is recommended to set proper include paths as otherwise unresolved symbols/templates will
     * result in subsequent parser mistakes, such as treating "&lt;" as a BinaryOperator in the
     * following example: &lt;code&gt; std::unique_ptr&lt;Botan::Cipher_Mode&gt; bla; &lt;/code&gt;
     *
     * As long as loadIncludes is set to false, include files will only be parsed, but not loaded
     * into the CPG. *
     */
    val includePaths: List<Path>,
    /**
     * This acts as a white list for include files, if the array is not empty. Only the specified
     * includes files will be parsed and processed in the CPG, unless it is a port of the blacklist,
     * in which it will be ignored.
     */
    val includeWhitelist: List<Path>,
    /**
     * This acts as a block list for include files, if the array is not empty. The specified include
     * files will be excluded from being parsed and processed in the CPG. The blocklist entries
     * always take priority over those in the whitelist.
     */
    val includeBlocklist: List<Path>,
    passes: List<Pass>,
    languages: List<Language<out LanguageFrontend>>,
    codeInNodes: Boolean,
    processAnnotations: Boolean,
    disableCleanup: Boolean,
    useUnityBuild: Boolean,
    useParallelFrontends: Boolean,
    typeSystemActiveInFrontend: Boolean,
    inferenceConfiguration: InferenceConfiguration,
    compilationDatabase: CompilationDatabase?,
    matchCommentsToNodes: Boolean,
    addIncludesToGraph: Boolean
) {
    /** This list contains all languages which we want to translate. */
    val languages: List<Language<out LanguageFrontend>>

    /**
     * Switch off cleaning up TypeManager memory after analysis.
     *
     * Set this to `true` only for testing.
     */
    var disableCleanup = false

    /** should the code of a node be shown as parameter in the node * */
    @JvmField val codeInNodes: Boolean

    /** Set to true to process annotations or annotation-like elements. */
    val processAnnotations: Boolean

    /**
     * Only relevant for C++. A unity build refers to a build that consolidates all translation
     * units into a single one, which has the advantage that header files are only processed once,
     * adding far less duplicate nodes to the graph
     */
    val useUnityBuild: Boolean

    /**
     * If true, the ASTs for the source files are parsed in parallel, but the passes afterwards will
     * still run in a single thread. This speeds up initial parsing but makes sure that further
     * graph enrichment algorithms remain correct.
     */
    val useParallelFrontends: Boolean

    /**
     * If false, the type listener system is only activated once the frontends are done building the
     * initial AST structure. This avoids errors where the type of a node may depend on the order in
     * which the source files have been parsed.
     */
    val typeSystemActiveInFrontend: Boolean

    /**
     * This is the data structure for storing the compilation database. It stores a mapping from the
     * File to the list of files that have to be included to their path, specified by the parameter
     * in the compilation database. This is currently only used by the [CXXLanguageFrontend].
     *
     * [[CompilationDatabase.Companion.fromFile] can be used to construct a new compilation database
     * from a file.
     */
    val compilationDatabase: CompilationDatabase?

    /**
     * If true the frontend shall use a heuristic matching of comments found in the source file to
     * match them to the closest AST node and save it in the comment property.
     */
    val matchCommentsToNodes: Boolean

    /** If true the (cpp) frontend connects a node to required includes. */
    val addIncludesToGraph: Boolean

    @get:JsonIdentityReference(alwaysAsId = true)
    @get:JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator::class,
        property = "name"
    )
    val registeredPasses: List<Pass>

    /** This sub configuration object holds all information about inference and smart-guessing. */
    val inferenceConfiguration: InferenceConfiguration

    init {
        registeredPasses = passes
        this.languages = languages
        // Make sure to init this AFTER sourceLocations has been set
        this.codeInNodes = codeInNodes
        this.processAnnotations = processAnnotations
        this.disableCleanup = disableCleanup
        this.useUnityBuild = useUnityBuild
        this.useParallelFrontends = useParallelFrontends
        this.typeSystemActiveInFrontend = typeSystemActiveInFrontend
        this.inferenceConfiguration = inferenceConfiguration
        this.compilationDatabase = compilationDatabase
        this.matchCommentsToNodes = matchCommentsToNodes
        this.addIncludesToGraph = addIncludesToGraph
    }

    /** Returns a list of all analyzed files. */
    val sourceLocations: List<File>
        get() {
            val sourceLocations: MutableList<File> = ArrayList()
            for ((_, value) in softwareComponents) {
                sourceLocations.addAll(value)
            }
            return sourceLocations
        }

    /**
     * Builds a [TranslationConfiguration].
     *
     * Example:
     *
     * <pre>`TranslationManager.builder() .config( TranslationConfiguration.builder()
     * .sourceLocations(new File("example.cpp")) .defaultPasses() .debugParser(true) .build())
     * .build(); `</pre> *
     */
    class Builder {
        private var softwareComponents: MutableMap<String, List<File>> = HashMap()
        private val languages = mutableListOf<Language<out LanguageFrontend>>()
        private var topLevel: File? = null
        private var debugParser = false
        private var failOnError = false
        private var loadIncludes = false
        private var symbols = mapOf<String, String>()
        private val includePaths = mutableListOf<Path>()
        private val includeWhitelist = mutableListOf<Path>()
        private val includeBlocklist = mutableListOf<Path>()
        private val passes = mutableListOf<Pass>()
        private var codeInNodes = true
        private var processAnnotations = false
        private var disableCleanup = false
        private var useUnityBuild = false
        private var useParallelFrontends = false
        private var typeSystemActiveInFrontend = true
        private var inferenceConfiguration = InferenceConfiguration.Builder().build()
        private var compilationDatabase: CompilationDatabase? = null
        private var matchCommentsToNodes = false
        private var addIncludesToGraph = true
        fun symbols(symbols: Map<String, String>): Builder {
            this.symbols = symbols
            return this
        }

        /**
         * Files or directories containing the source code to analyze. Generates a dummy software
         * component called "application".
         *
         * @param sourceLocations The files with the source code
         * @return this
         */
        fun sourceLocations(vararg sourceLocations: File): Builder {
            softwareComponents["application"] = sourceLocations.toMutableList()
            return this
        }

        /**
         * Files or directories containing the source code to analyze. Generates a dummy software
         * component called "application".
         *
         * @param sourceLocations The files with the source code
         * @return this
         */
        fun sourceLocations(sourceLocations: List<File>): Builder {
            softwareComponents["application"] = sourceLocations.toMutableList()
            return this
        }

        /**
         * Files or directories containing the source code to analyze organized by different
         * components
         *
         * @param softwareComponents A map holding the different components with their files
         * @return this
         */
        fun softwareComponents(softwareComponents: MutableMap<String, List<File>>): Builder {
            this.softwareComponents = softwareComponents
            return this
        }

        fun useCompilationDatabase(compilationDatabase: CompilationDatabase?): Builder {
            this.compilationDatabase = compilationDatabase
            return this
        }

        fun topLevel(topLevel: File?): Builder {
            this.topLevel = topLevel
            return this
        }

        /** Dump parser debug output to the logs (Caution: this will generate a lot of output). */
        fun debugParser(debugParser: Boolean): Builder {
            this.debugParser = debugParser
            return this
        }

        /** Match comments found in source files to nodes according to a heuristic. */
        fun matchCommentsToNodes(matchCommentsToNodes: Boolean): Builder {
            this.matchCommentsToNodes = matchCommentsToNodes
            return this
        }

        /** Adds all required includes. */
        fun addIncludesToGraph(addIncludesToGraph: Boolean): Builder {
            this.addIncludesToGraph = addIncludesToGraph
            return this
        }

        /** Fail analysis on first error. Try to continue otherwise. */
        fun failOnError(failOnError: Boolean): Builder {
            this.failOnError = failOnError
            return this
        }

        /**
         * Load C/C++ include headers before the analysis.
         *
         * Required for macro expansion.
         */
        fun loadIncludes(loadIncludes: Boolean): Builder {
            this.loadIncludes = loadIncludes
            return this
        }

        /** Directory containing include headers. */
        fun includePath(includePath: String): Builder {
            includePaths.add(Path.of(includePath))
            return this
        }

        /** Directory containing include headers. */
        fun includePath(includePath: Path): Builder {
            includePaths.add(includePath)
            return this
        }

        /**
         * Adds the specified file to the include whitelist. Relative and absolute paths are
         * supported.
         */
        fun includeWhitelist(includeFile: String): Builder {
            includeWhitelist.add(Path.of(includeFile))
            return this
        }

        /**
         * Adds the specified file to the include whitelist. Relative and absolute paths are
         * supported.
         */
        fun includeWhitelist(includeFile: Path): Builder {
            includeWhitelist.add(includeFile)
            return this
        }

        fun disableCleanup(): Builder {
            disableCleanup = true
            return this
        }

        /**
         * Adds the specified file to the include blocklist. Relative and absolute paths are
         * supported.
         */
        fun includeBlocklist(includeFile: String): Builder {
            includeBlocklist.add(Path.of(includeFile))
            return this
        }

        /**
         * Adds the specified file to the include blocklist. Relative and absolute paths are
         * supported.
         */
        fun includeBlocklist(includeFile: Path): Builder {
            includeBlocklist.add(includeFile)
            return this
        }

        /** Register an additional [Pass]. */
        fun registerPass(pass: Pass): Builder {
            passes.add(pass)
            return this
        }

        /** Registers an additional [Language]. */
        fun registerLanguage(language: Language<out LanguageFrontend>): Builder {
            languages.add(language)
            log.info(
                "Registered language frontend '${language::class.simpleName}' for following file types: ${language.fileExtensions}"
            )
            return this
        }

        /** Registers an additional [Language]. */
        inline fun <reified T : Language<out LanguageFrontend>> registerLanguage(): Builder {
            T::class.primaryConstructor?.call()?.let { registerLanguage(it) }
            return this
        }

        /**
         * Loads and registers an additional [Language] based on a fully qualified class name (FQN).
         */
        @Throws(ConfigurationException::class)
        fun registerLanguage(className: String): Builder {
            try {
                val loadedClass = Class.forName(className).kotlin.createInstance() as? Language<*>
                if (loadedClass != null) {
                    registerLanguage(loadedClass)
                } else
                    throw ConfigurationException(
                        "Failed casting supposed language class '$className'. It does not seem to be an implementation of Language<*>."
                    )
            } catch (e: Exception) {
                throw ConfigurationException(
                    "Failed to load and instantiate class from FQN '$className'. Possible causes of this error:\n" +
                        "- the given class is unavailable in the class path\n" +
                        "- the given class does not have a single no-arg constructor\n"
                )
            }
            return this
        }

        /** Unregisters a registered [de.fraunhofer.aisec.cpg.frontends.Language]. */
        fun unregisterLanguage(language: Class<out Language<out LanguageFrontend>?>): Builder {
            languages.removeIf { obj: Language<out LanguageFrontend>? -> language.isInstance(obj) }
            return this
        }

        /**
         * Register all default [Pass]es.
         *
         * This will register
         *
         * - [TypeHierarchyResolver]
         * - [JavaExternalTypeHierarchyResolver]
         * - [ImportResolver]
         * - [VariableUsageResolver]
         * - [CallResolver]
         * - [DFGPass]
         * - [FunctionPointerCallResolver]
         * - [EvaluationOrderGraphPass]
         * - [TypeResolver]
         * - [ControlFlowSensitiveDFGPass]
         * - [FilenameMapper]
         *
         * to be executed in the order specified by their annotations.
         */
        fun defaultPasses(): Builder {
            registerPass(TypeHierarchyResolver())
            registerPass(JavaExternalTypeHierarchyResolver())
            registerPass(ImportResolver())
            registerPass(VariableUsageResolver())
            registerPass(CallResolver()) // creates CG
            registerPass(DFGPass())
            registerPass(FunctionPointerCallResolver())
            registerPass(EvaluationOrderGraphPass()) // creates EOG
            registerPass(TypeResolver())
            registerPass(ControlFlowSensitiveDFGPass())
            registerPass(FilenameMapper())
            return this
        }

        /** Register extra passes declared by a frontend with [RegisterExtraPass] */
        @Throws(ConfigurationException::class)
        private fun registerExtraFrontendPasses() {
            for (frontend in languages.map(Language<out LanguageFrontend>::frontend)) {
                val extraPasses = frontend.findAnnotations<RegisterExtraPass>()

                if (extraPasses.isNotEmpty()) {
                    for (p in extraPasses) {
                        val pass = p.value.primaryConstructor?.call()
                        if (pass != null) {
                            registerPass(pass)

                            log.info(
                                "Registered an extra (frontend dependent) default dependency: {}",
                                p.value
                            )
                        } else {
                            throw ConfigurationException(
                                "Failed to load frontend because we could not register required pass dependency: ${frontend.simpleName}"
                            )
                        }
                    }
                }
            }
        }

        /** Register all default languages. */
        fun defaultLanguages(): Builder {
            registerLanguage(CLanguage())
            registerLanguage(CPPLanguage())
            registerLanguage(JavaLanguage())

            return this
        }

        /**
         * Safely register an additional [Language] from a class name. If the [Language] given by
         * the class name could not be loaded or instantiated, no [Language] is registered and no
         * error is thrown. Please have a look at [registerLanguage] if an error should be thrown in
         * case the language could not be registered.
         *
         * @param className Fully qualified class name (FQN) of a [Language] class
         * @see [registerLanguage]
         */
        fun optionalLanguage(className: String) =
            try {
                registerLanguage(className)
            } catch (e: ConfigurationException) {
                this
            }

        fun codeInNodes(b: Boolean): Builder {
            codeInNodes = b
            return this
        }

        /**
         * Specifies, whether annotations should be process or not. By default, they are not
         * processed, since they might populate the graph too much.
         *
         * @param b the new value
         */
        fun processAnnotations(b: Boolean): Builder {
            processAnnotations = b
            return this
        }

        /**
         * Only relevant for C++. A unity build refers to a build that consolidates all translation
         * units into a single one, which has the advantage that header files are only processed
         * once, adding far less duplicate nodes to the graph
         *
         * @param b the new value
         */
        fun useUnityBuild(b: Boolean): Builder {
            useUnityBuild = b
            return this
        }

        /**
         * If true, the ASTs for the source files are parsed in parallel, but the passes afterwards
         * will still run in a single thread. This speeds up initial parsing but makes sure that
         * further graph enrichment algorithms remain correct. Please make sure to also set [
         * ][.typeSystemActiveInFrontend] to false to avoid probabilistic errors that appear
         * depending on the parsing order.
         *
         * @param b the new value
         */
        fun useParallelFrontends(b: Boolean): Builder {
            useParallelFrontends = b
            return this
        }

        /**
         * If false, the type system is only activated once the frontends are done building the
         * initial AST structure. This avoids errors where the type of a node may depend on the
         * order in which the source files have been parsed.
         *
         * @param b the new value
         */
        fun typeSystemActiveInFrontend(b: Boolean): Builder {
            typeSystemActiveInFrontend = b
            return this
        }

        fun inferenceConfiguration(configuration: InferenceConfiguration): Builder {
            inferenceConfiguration = configuration
            return this
        }

        @Throws(ConfigurationException::class)
        fun build(): TranslationConfiguration {
            if (useParallelFrontends && typeSystemActiveInFrontend) {
                log.warn(
                    "Not disabling the type system during the frontend " +
                        "phase is not recommended when using the parallel frontends feature! " +
                        "This may result in erroneous results."
                )
            }
            registerExtraFrontendPasses()
            return TranslationConfiguration(
                symbols,
                softwareComponents,
                topLevel,
                debugParser,
                failOnError,
                loadIncludes,
                includePaths,
                includeWhitelist,
                includeBlocklist,
                orderPasses(),
                languages,
                codeInNodes,
                processAnnotations,
                disableCleanup,
                useUnityBuild,
                useParallelFrontends,
                typeSystemActiveInFrontend,
                inferenceConfiguration,
                compilationDatabase,
                matchCommentsToNodes,
                addIncludesToGraph
            )
        }

        /**
         * Collects the requested passes stored in [registeredPasses] and generates a
         * [PassWithDepsContainer] consisting of pairs of passes and their dependencies.
         *
         * @return A populated [PassWithDepsContainer] derived from [registeredPasses].
         */
        private fun collectInitialPasses(): PassWithDepsContainer {
            val workingList = PassWithDepsContainer()

            // Add the "execute before" dependencies.
            for (p in passes) {
                val executeBefore = p.executeBefore
                for (eb in executeBefore) {
                    passes
                        .filter { eb.isInstance(it) }
                        .forEach { it.addSoftDependency(p.javaClass) }
                }
            }
            for (p in passes) {
                var passFound = false
                for ((pass) in workingList.getWorkingList()) {
                    if (pass.javaClass == p.javaClass) {
                        passFound = true
                        break
                    }
                }
                if (!passFound) {
                    val deps: MutableSet<Class<out Pass>> = HashSet()
                    deps.addAll(p.hardDependencies)
                    deps.addAll(p.softDependencies)
                    workingList.addToWorkingList(PassWithDependencies(p, deps))
                }
            }
            return workingList
        }

        /**
         * This function reorders passes in order to meet their dependency requirements.
         *
         * * soft dependencies [DependsOn] with `softDependency == true`: all passes registered as
         * soft dependency will be executed before the current pass if they are registered
         * * hard dependencies [DependsOn] with `softDependency == false (default)`: all passes
         * registered as hard dependency will be executed before the current pass (hard dependencies
         * will be registered even if the user did not register them)
         * * first pass [ExecuteFirst]: a pass registered as first pass will be executed in the
         * beginning
         * * last pass [ExecuteLast]: a pass registered as last pass will be executed at the end
         *
         * This function uses a very simple (and inefficient) logic to meet the requirements above:
         *
         * 1. A list of all registered passes and their dependencies is build
         * [PassWithDepsContainer.workingList]
         * 1. All missing hard dependencies [DependsOn] are added to the
         * [PassWithDepsContainer.workingList]
         * 1. The first pass [ExecuteFirst] is added to the result and removed from the other passes
         * dependencies
         * 1. The first pass in the [workingList] without dependencies is added to the result and it
         * is removed from the other passes dependencies
         * 1. The above step is repeated until all passes are added to the result
         *
         * @return a sorted list of passes
         */
        @Throws(ConfigurationException::class)
        private fun orderPasses(): List<Pass> {
            log.info("Passes before enforcing order: {}", passes)
            val result = mutableListOf<Pass>()

            // Create a local copy of all passes and their "current" dependencies without possible
            // duplicates
            val workingList = collectInitialPasses()
            log.debug("Working list after initial scan: {}", workingList)
            workingList.addMissingDependencies()
            log.debug("Working list after adding missing dependencies: {}", workingList)
            if (workingList.getFirstPasses().size > 1) {
                log.error(
                    "Too many passes require to be executed as first pass: {}",
                    workingList.getWorkingList()
                )
                throw ConfigurationException(
                    "Too many passes require to be executed as first pass."
                )
            }
            if (workingList.getLastPasses().size > 1) {
                log.error(
                    "Too many passes require to be executed as last pass: {}",
                    workingList.getLastPasses()
                )
                throw ConfigurationException("Too many passes require to be executed as last pass.")
            }
            val firstPass = workingList.getAndRemoveFirstPass()
            if (firstPass != null) {
                result.add(firstPass)
            }
            while (!workingList.isEmpty) {
                val p = workingList.getAndRemoveFirstPassWithoutDependencies()
                if (p != null) {
                    result.add(p)
                } else {
                    // failed to find a pass that can be added to the result -> deadlock :(
                    throw ConfigurationException("Failed to satisfy ordering requirements.")
                }
            }
            log.info("Passes after enforcing order: {}", result)
            return result
        }
    }

    override fun toString(): String {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE)
    }

    companion object {
        private val log = LoggerFactory.getLogger(TranslationConfiguration::class.java)
        fun builder(): Builder {
            return Builder()
        }
    }
}
