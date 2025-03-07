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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import de.fraunhofer.aisec.cpg.TranslationContext.EmptyTranslationContext
import de.fraunhofer.aisec.cpg.TranslationResult.Companion.DEFAULT_APPLICATION_NAME
import de.fraunhofer.aisec.cpg.frontends.CompilationDatabase
import de.fraunhofer.aisec.cpg.frontends.KClassSerializer
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.configuration.*
import de.fraunhofer.aisec.cpg.passes.inference.DFGFunctionSummaries
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.isSubclassOf
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.slf4j.LoggerFactory

/**
 * The configuration for the [TranslationManager] holds all information that is used during the
 * translation.
 */
@DoNotPersist
class TranslationConfiguration
private constructor(
    /** Definition of additional symbols, mostly useful for C++. */
    val symbols: Map<String, String>,
    /** Source code files to parse. */
    val softwareComponents: Map<String, List<File>>,
    val topLevels: MutableMap<String, File?>,
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
    passes: List<List<KClass<out Pass<out Node>>>>,
    /**
     * This map offers the possibility to replace certain passes for specific languages with other
     * passes. It can either be filled with the [Builder.replacePass] or by using the [ReplacePass]
     * annotation on a [LanguageFrontend].
     */
    val replacedPasses:
        Map<Pair<KClass<out Pass<out Node>>, KClass<out Language<*>>>, KClass<out Pass<out Node>>>,
    /** This list contains the files with function summaries which should be considered. */
    val functionSummaries: DFGFunctionSummaries,
    languages: List<KClass<out Language<*>>>,
    codeInNodes: Boolean,
    processAnnotations: Boolean,
    disableCleanup: Boolean,
    useUnityBuild: Boolean,
    useParallelFrontends: Boolean,
    useParallelPasses: Boolean,
    inferenceConfiguration: InferenceConfiguration,
    compilationDatabase: CompilationDatabase?,
    matchCommentsToNodes: Boolean,
    addIncludesToGraph: Boolean,
    passConfigurations: Map<KClass<out Pass<*>>, PassConfiguration>,
    /** A list of exclusion patterns used to filter files and directories. */
    val exclusionPatternsByString: List<String>,
    /** A list of exclusion patterns using regular expressions to filter files and directories. */
    val exclusionPatternsByRegex: List<Regex>,
) {
    /** This list contains all languages which we want to translate. */
    @JsonIgnore val languages: List<KClass<out Language<*>>>

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

    val useParallelPasses: Boolean

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

    /** A list containing a list of passes that run in parallel. */
    @JsonIgnore val registeredPasses: List<List<KClass<out Pass<out Node>>>>

    /**
     * A flattened list of [registeredPasses], mainly used for the JSON representation because
     * Jackson cannot deal with lists of lists very well.
     */
    @get:JsonSerialize(contentUsing = KClassSerializer::class)
    @get:JsonProperty("registeredPasses")
    val flatRegisteredPasses: List<KClass<out Pass<*>>>
        get() {
            return registeredPasses.flatten()
        }

    /** This sub configuration object holds all information about inference and smart-guessing. */
    val inferenceConfiguration: InferenceConfiguration

    val passConfigurations: Map<KClass<out Pass<*>>, PassConfiguration>

    init {
        this.registeredPasses = passes
        this.languages = languages
        // Make sure to init this AFTER sourceLocations has been set
        this.codeInNodes = codeInNodes
        this.processAnnotations = processAnnotations
        this.disableCleanup = disableCleanup
        this.useUnityBuild = useUnityBuild
        this.useParallelFrontends = useParallelFrontends
        this.useParallelPasses = useParallelPasses
        this.inferenceConfiguration = inferenceConfiguration
        this.compilationDatabase = compilationDatabase
        this.matchCommentsToNodes = matchCommentsToNodes
        this.addIncludesToGraph = addIncludesToGraph
        this.passConfigurations = passConfigurations
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
     * ```
     * TranslationManager.builder()
     *   .config(TranslationConfiguration.builder()
     *     .sourceLocations(new File("example.cpp"))
     *     .defaultPasses()
     *     .debugParser(true)
     *     .build())
     *   .build();
     * ```
     */
    class Builder {
        private var softwareComponents: MutableMap<String, List<File>> = HashMap()
        private val languages = mutableListOf<KClass<out Language<*>>>()
        private var topLevels = mutableMapOf<String, File?>()
        private var debugParser = false
        private var failOnError = false
        private var loadIncludes = false
        private var symbols = mapOf<String, String>()
        private val includePaths = mutableListOf<Path>()
        private val includeWhitelist = mutableListOf<Path>()
        private val includeBlocklist = mutableListOf<Path>()
        private val passes = mutableListOf<KClass<out Pass<*>>>()
        private val replacedPasses =
            mutableMapOf<Pair<KClass<out Pass<*>>, KClass<out Language<*>>>, KClass<out Pass<*>>>()
        private val functionSummaries = mutableListOf<File>()
        private var codeInNodes = true
        private var processAnnotations = false
        private var disableCleanup = false
        private var useUnityBuild = false
        private var useParallelFrontends = false
        private var useParallelPasses = false
        private var inferenceConfiguration = InferenceConfiguration.Builder().build()
        private var compilationDatabase: CompilationDatabase? = null
        private var matchCommentsToNodes = false
        private var addIncludesToGraph = true
        private var useDefaultPasses = false
        private var passConfigurations: MutableMap<KClass<out Pass<*>>, PassConfiguration> =
            mutableMapOf()
        private val exclusionPatternsByRegex = mutableListOf<Regex>()
        private val exclusionPatternsByString = mutableListOf<String>()

        fun symbols(symbols: Map<String, String>): Builder {
            this.symbols = symbols
            return this
        }

        /**
         * Files or directories containing the source code to analyze. Generates a [Component] with
         * the name of [DEFAULT_APPLICATION_NAME].
         *
         * @param sourceLocations The files with the source code
         * @return this
         */
        fun sourceLocations(vararg sourceLocations: File): Builder {
            softwareComponents[DEFAULT_APPLICATION_NAME] = sourceLocations.toMutableList()
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
            softwareComponents[DEFAULT_APPLICATION_NAME] = sourceLocations.toMutableList()
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
            this.topLevels[DEFAULT_APPLICATION_NAME] = topLevel
            return this
        }

        fun topLevels(topLevels: Map<String, File?>): Builder {
            this.topLevels.clear()
            this.topLevels += topLevels
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

        inline fun <reified P : Pass<*>> registerPass(): Builder {
            registerPass(P::class)
            return this
        }

        /** Register an additional [Pass]. */
        fun registerPass(passType: KClass<out Pass<*>>): Builder {
            passes.add(passType)
            return this
        }

        inline fun <
            reified OldPass : Pass<*>,
            reified For : Language<*>,
            reified With : Pass<*>,
        > replacePass(): Builder {
            return replacePass(OldPass::class, For::class, With::class)
        }

        fun replacePass(
            passType: KClass<out Pass<*>>,
            forLanguage: KClass<out Language<*>>,
            with: KClass<out Pass<*>>,
        ): Builder {
            replacedPasses[Pair(passType, forLanguage)] = with
            return this
        }

        fun registerFunctionSummaries(vararg functionSummary: File): Builder {
            this.functionSummaries.addAll(functionSummary)
            return this
        }

        /** Registers an additional [Language]. */
        fun registerLanguage(language: Language<*>): Builder {
            throw UnsupportedOperationException("Use registerLanguage(className: String) instead")
        }

        /** Registers an additional [Language] by its [KClass]. */
        fun <T : Language<*>> registerLanguage(clazz: KClass<T>): Builder {
            languages.add(clazz)
            return this
        }

        /** Registers an additional [Language]. */
        inline fun <reified T : Language<*>> registerLanguage(): Builder {
            registerLanguage(T::class)
            return this
        }

        fun <T : Pass<*>> configurePass(clazz: KClass<T>, config: PassConfiguration): Builder {
            this.passConfigurations[clazz] = config
            return this
        }

        inline fun <reified T : Pass<*>> configurePass(config: PassConfiguration): Builder {
            return this.configurePass(T::class, config)
        }

        /**
         * Adds exclusion patterns using regular expressions for filtering files and directories.
         *
         * @param patterns Exclusion patterns. Example:
         * ```
         * exclusionPatterns(Regex(".*test(s)?"))
         * ```
         */
        fun exclusionPatterns(vararg patterns: Regex): Builder {
            exclusionPatternsByRegex.addAll(patterns)
            return this
        }

        /**
         * Adds exclusion patterns for filtering files and directories.
         *
         * @param patterns Exclusion patterns. Example:
         * ```
         * exclusionPatterns("tests")
         * ```
         */
        fun exclusionPatterns(vararg patterns: String): Builder {
            exclusionPatternsByString.addAll(patterns)
            return this
        }

        /**
         * Loads and registers an additional [Language] based on a fully qualified class name (FQN).
         */
        @Throws(ConfigurationException::class)
        fun registerLanguage(className: String): Builder {
            try {
                @Suppress("UNCHECKED_CAST")
                val loadedClass = Class.forName(className).kotlin as? KClass<out Language<*>>
                if (loadedClass != null) {
                    registerLanguage(loadedClass)
                } else
                    throw ConfigurationException(
                        "Failed casting supposed language class '$className'. It does not seem to be an implementation of Language<*>."
                    )
            } catch (_: Exception) {
                throw ConfigurationException(
                    "Failed to load and instantiate class from FQN '$className'. Possible causes of this error:\n" +
                        "- the given class is unavailable in the class path\n"
                )
            }
            return this
        }

        /** Unregisters a registered [de.fraunhofer.aisec.cpg.frontends.Language]. */
        fun unregisterLanguage(language: KClass<out Language<*>>): Builder {
            languages.removeIf { it.isSubclassOf(language) }
            return this
        }

        /**
         * Register all default [Pass]es.
         *
         * This will register
         * - [TypeHierarchyResolver]
         * - [SymbolResolver]
         * - [ImportResolver]
         * - [DFGPass]
         * - [EvaluationOrderGraphPass]
         * - [DynamicInvokeResolver]
         * - [TypeResolver]
         * - [ControlFlowSensitiveDFGPass]
         * - [FilenameMapper]
         * - [ResolveCallExpressionAmbiguityPass]
         * - [ResolveMemberExpressionAmbiguityPass]
         *
         * to be executed in the order specified by their annotations.
         */
        fun defaultPasses(): Builder {
            registerPass<TypeHierarchyResolver>()
            registerPass<SymbolResolver>()
            registerPass<ImportResolver>()
            registerPass<DFGPass>()
            registerPass<DynamicInvokeResolver>()
            registerPass<EvaluationOrderGraphPass>() // creates EOG
            registerPass<TypeResolver>()
            registerPass<ControlFlowSensitiveDFGPass>()
            registerPass<FilenameMapper>()
            registerPass<ResolveCallExpressionAmbiguityPass>()
            registerPass<ResolveMemberExpressionAmbiguityPass>()
            useDefaultPasses = true
            return this
        }

        /**
         * Register extra passes declared by a frontend with [RegisterExtraPass], but only if
         * [useDefaultPasses] is true (which is set to true by invoking [defaultPasses]).
         */
        @Throws(ConfigurationException::class)
        private fun registerExtraFrontendPasses() {
            // We do not want to register any extra passes from the frontends if we are not running
            // the default passes
            if (!useDefaultPasses) {
                return
            }

            for (frontend in languages.map { it.frontend }) {
                val extraPasses = frontend.findAnnotations<RegisterExtraPass>()
                if (extraPasses.isNotEmpty()) {
                    for (p in extraPasses) {
                        registerPass(p.value)
                        log.info(
                            "Registered an extra (frontend dependent) default dependency: {}",
                            p.value,
                        )
                    }
                }
            }
        }

        private fun registerReplacedPasses() {
            for (frontend in languages.map { it.frontend }) {
                val replacedPasses = frontend.findAnnotations<ReplacePass>()
                if (replacedPasses.isNotEmpty()) {
                    for (p in replacedPasses) {
                        replacePass(p.old, p.lang, p.with)
                        log.info(
                            "Registered an extra (frontend dependent) default dependency, which replaced an existing pass: {}",
                            p.old,
                        )
                    }
                }
            }
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
         * further graph enrichment algorithms remain correct.
         *
         * @param b the new value
         */
        fun useParallelFrontends(b: Boolean): Builder {
            useParallelFrontends = b
            return this
        }

        fun useParallelPasses(b: Boolean): Builder {
            useParallelPasses = b
            return this
        }

        fun inferenceConfiguration(configuration: InferenceConfiguration): Builder {
            inferenceConfiguration = configuration
            return this
        }

        @Throws(ConfigurationException::class)
        fun build(): TranslationConfiguration {
            registerExtraFrontendPasses()
            registerReplacedPasses()
            return TranslationConfiguration(
                symbols,
                softwareComponents,
                topLevels,
                debugParser,
                failOnError,
                loadIncludes,
                includePaths,
                includeWhitelist,
                includeBlocklist,
                orderPasses(),
                replacedPasses,
                DFGFunctionSummaries.fromFiles(functionSummaries),
                languages,
                codeInNodes,
                processAnnotations,
                disableCleanup,
                useUnityBuild,
                useParallelFrontends,
                useParallelPasses,
                inferenceConfiguration,
                compilationDatabase,
                matchCommentsToNodes,
                addIncludesToGraph,
                passConfigurations,
                exclusionPatternsByString,
                exclusionPatternsByRegex,
            )
        }

        /** This function reorders passes in order to meet their dependency requirements. */
        @Throws(ConfigurationException::class)
        private fun orderPasses(): List<List<KClass<out Pass<*>>>> {
            log.info("Passes before enforcing order: {}", passes.map { it.simpleName })
            val orderingHelper = PassOrderingHelper(passes)
            log.info(
                "The following mermaid graph represents the pass dependencies: \n${buildMermaid(passes)}"
            )

            return orderingHelper.order()
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

/**
 * Returns the frontend class of a language. Since the [Language.frontend] is a property of a
 * language, we need to create a temporary object of the language class to access it (using
 * [EmptyTranslationContext]).
 */
val KClass<out Language<*>>.frontend: KClass<out LanguageFrontend<*, *>>
    get() {
        // Instantiate a temporary object of the language class
        val instance =
            constructors.firstOrNull()?.call(EmptyTranslationContext)
                ?: throw IllegalArgumentException(
                    "Could not instantiate temporary object of language class ${this.simpleName}"
                )

        return instance.frontend
    }
