/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.project

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.TranslationResult.Companion.DEFAULT_APPLICATION_NAME
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.passes.Pass
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.reflect.KClass
import org.slf4j.LoggerFactory

/**
 * Defines a single component of a [Project], e.g., an application, a service or a library that is
 * analyzed together with the rest of the project but represented as an individual
 * [de.fraunhofer.aisec.cpg.graph.Component] in the resulting graph.
 */
class ComponentDefinition(
    /** The name of the component. */
    val name: String,
    /** The top-level directory of the component. */
    val root: Path,
    /** The source files or directories of this component. Defaults to the whole [root]. */
    val sources: List<Path> = listOf(root),
)

/**
 * A [Project] is the primary, high-level entry point for analyzing source code with the CPG. It
 * describes *what* is analyzed (a single file or a whole repository, optionally split into
 * components and filtered), and the [TargetEnvironment] it is assumed to run on. From this, the
 * lower-level [TranslationConfiguration] is derived automatically.
 *
 * The simplest usage is a fully automatic analysis — an empty block is enough:
 * ```kotlin
 * val result = project(Path("/path/to/repo")) { }.analyze()
 * ```
 *
 * This **auto-mode** uses all default languages available on the classpath, all default passes, and
 * runs every registered [ComponentDetector] and [ProjectDetector] to auto-detect the project
 * structure. Specifying a block *overrides* the respective defaults:
 * ```kotlin
 * val project =
 *     project(Path("/path/to/repo")) {
 *         // Only Go is registered; no other language is considered.
 *         languages { use<GoLanguage>() }
 *
 *         // Default languages plus Go (when Go is not already in the defaults).
 *         languages { default(); use<GoLanguage>() }
 *
 *         // No default passes; only the explicitly listed ones run.
 *         passes { use<SymbolResolver>() }
 *
 *         // Auto-detect components AND add an extra explicit one.
 *         components { default(); component("extra", root = Path("extra")) }
 *     }
 * val result = project.analyze()
 * ```
 *
 * When a project is created from a directory, all registered languages that implement
 * [ComponentDetector] or [ProjectDetector] are asked to auto-detect the project structure
 * (components such as Go modules or compilation database targets) and project-wide settings
 * (symbols, include paths, a compilation database). The outcome is recorded in [components] and
 * [detectionResults] so that it can be inspected before the analysis is started.
 */
class Project
internal constructor(
    /** The name of the project. */
    val name: String,
    /** The project directory. Null, if this is an ad-hoc project for a single file. */
    val directory: Path?,
    /** The components of this project. */
    val components: List<ComponentDefinition>,
    /** The external environment this project is assumed to run on. */
    val environment: TargetEnvironment,
    /** The results of the project auto-detection, mainly for diagnostic purposes. */
    val detectionResults: List<DetectionResult>,
    /** The derived low-level translation configuration. */
    val config: TranslationConfiguration,
) {
    /** Translates the project into a CPG and returns the result. */
    fun analyze(): TranslationResult {
        return TranslationManager.builder().config(config).build().analyze().get()
    }

    companion object {
        /**
         * Fully qualified class names of all known languages. They are registered if they are
         * available on the classpath and if no language was explicitly registered with
         * [LanguagesBuilder.use].
         */
        val defaultLanguages =
            listOf(
                "de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage",
                "de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage",
                "de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage",
                "de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage",
                "de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguage",
                "de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage",
                "de.fraunhofer.aisec.cpg.frontends.typescript.TypeScriptLanguage",
                "de.fraunhofer.aisec.cpg.frontends.ruby.RubyLanguage",
                "de.fraunhofer.aisec.cpg.frontends.jvm.JVMLanguage",
                "de.fraunhofer.aisec.cpg.frontends.ini.IniFileLanguage",
            )

        /**
         * Creates a [Project] from a [path], which can either point to a single source file or to a
         * directory (e.g., a repository). Additional configuration can be supplied with the
         * [configure] block, see [ProjectBuilder].
         */
        fun from(path: Path, configure: ProjectBuilder.() -> Unit = {}): Project {
            val builder = ProjectBuilder(path)
            builder.configure()
            return builder.resolve()
        }
    }
}

/** Creates a [Project] from [path]. This is a DSL-style alias for [Project.from]. */
fun project(path: Path, configure: ProjectBuilder.() -> Unit = {}): Project {
    return Project.from(path, configure)
}

/**
 * Controls which languages are registered for a [Project].
 *
 * **Auto-mode** (no `languages {}` block): all languages from [Project.defaultLanguages] that are
 * present on the classpath are used.
 *
 * **Explicit block**: only the languages added with [use] are registered, unless [default] is also
 * called to include the defaults.
 *
 * ```kotlin
 * // Only Go — no other language.
 * languages { use<GoLanguage>() }
 *
 * // Default languages plus Go.
 * languages { default(); use<GoLanguage>() }
 * ```
 */
class LanguagesBuilder {
    @PublishedApi internal val explicit = mutableSetOf<KClass<out Language<*>>>()
    internal var includeDefaults = false

    /** Includes all languages from [Project.defaultLanguages] that are on the classpath. */
    fun default() {
        includeDefaults = true
    }

    /** Registers language [T]. */
    inline fun <reified T : Language<*>> use() {
        explicit += T::class
    }

    /** Registers language [clazz]. */
    fun use(clazz: KClass<out Language<*>>) {
        explicit += clazz
    }
}

/**
 * Controls which passes are registered for a [Project].
 *
 * **Auto-mode** (no `passes {}` block): the default passes (see
 * [TranslationConfiguration.Builder.defaultPasses]) are registered.
 *
 * **Explicit block**: only the passes added with [use] are registered, unless [default] is also
 * called to include the defaults.
 *
 * ```kotlin
 * // Only SymbolResolver — no other pass.
 * passes { use<SymbolResolver>() }
 *
 * // Default passes plus a custom one.
 * passes { default(); use<MyCustomPass>() }
 * ```
 */
class PassesBuilder {
    internal var includeDefaults = false
    @PublishedApi internal val explicit = mutableListOf<KClass<out Pass<*>>>()

    /** Includes all default passes (see [TranslationConfiguration.Builder.defaultPasses]). */
    fun default() {
        includeDefaults = true
    }

    /** Registers pass [T]. */
    inline fun <reified T : Pass<*>> use() {
        explicit += T::class
    }

    /** Registers pass [clazz]. */
    fun use(clazz: KClass<out Pass<*>>) {
        explicit += clazz
    }
}

/**
 * Controls how the components of a [Project] are determined.
 *
 * **Auto-mode** (no `components {}` block): [ComponentDetector]s and [ProjectDetector]s from all
 * registered languages are run automatically. If no detector finds anything, a single default
 * component spanning the whole project directory is used.
 *
 * **Explicit block**: only the components added with [component] are used; detectors do **not** run
 * unless [default] is called. [default] and [detector] can be combined with explicit [component]
 * definitions.
 *
 * ```kotlin
 * // Only an explicit "backend" component; no auto-detection.
 * components { component("backend", root = Path("services/backend")) }
 *
 * // Auto-detect everything and additionally add an explicit component.
 * components { default(); component("extra", root = Path("extra")) }
 *
 * // Auto-detect using a custom standalone detector.
 * components { detector(DirectoryComponentDetector("services")) }
 * ```
 */
class ComponentsBuilder {
    internal val explicit = mutableListOf<ComponentDefinition>()
    internal val detectors = mutableListOf<Detector>()
    internal var includeAutoDetect = false

    /**
     * Enables auto-detection: runs [ComponentDetector]s and [ProjectDetector]s from all registered
     * languages and any [detector]s added to this block.
     */
    fun default() {
        includeAutoDetect = true
    }

    /**
     * Adds a standalone [Detector] to this block. Also enables auto-detection ([default] is implied
     * when a detector is added).
     */
    fun detector(detector: Detector) {
        detectors += detector
        includeAutoDetect = true
    }

    /** Adds an explicit component definition. */
    fun component(name: String, root: Path, sources: List<Path> = listOf(root)) {
        explicit += ComponentDefinition(name, root, sources)
    }
}

/**
 * Collects all user-supplied configuration for a [Project] and resolves it into an immutable
 * [Project], including the derived [TranslationConfiguration].
 *
 * Leaving a block uncalled enables **auto-mode** for that aspect of the project:
 * - No `languages {}` → all [Project.defaultLanguages] present on the classpath.
 * - No `passes {}` → the default pass pipeline.
 * - No `components {}` → detectors from all registered languages run automatically.
 */
class ProjectBuilder(
    /** The path to the project: either a single source file or a directory. */
    val path: Path
) {
    /** The name of the project. Defaults to the file or directory name of [path]. */
    var name: String = path.fileName?.toString() ?: DEFAULT_APPLICATION_NAME

    private var languagesBuilder: LanguagesBuilder? = null
    private var passesBuilder: PassesBuilder? = null
    private var componentsBuilder: ComponentsBuilder? = null
    private val standaloneDetectors = mutableListOf<Detector>()

    /**
     * Overrides the set of candidate languages used by [detectLanguages]. Setting this is only
     * intended for tests that need to inject specific language classes without depending on the
     * classpath contents of [Project.defaultLanguages].
     */
    internal var defaultLanguagesOverride: Set<KClass<out Language<*>>>? = null
    private val excludesByString = mutableListOf<String>()
    private val excludesByRegex = mutableListOf<Regex>()
    private val configModifiers = mutableListOf<(TranslationConfiguration.Builder) -> Unit>()
    private var environment = TargetEnvironment.host()

    /** Configures the [TargetEnvironment] this project is assumed to run on. */
    fun environment(init: TargetEnvironmentBuilder.() -> Unit) {
        val builder = TargetEnvironmentBuilder()
        builder.init()
        environment = builder.build()
    }

    /**
     * Configures which languages are registered. Calling this block switches from auto-mode (all
     * default languages) to explicit mode; use [LanguagesBuilder.default] to re-include the
     * defaults.
     */
    fun languages(init: LanguagesBuilder.() -> Unit) {
        val builder = languagesBuilder ?: LanguagesBuilder().also { languagesBuilder = it }
        builder.init()
    }

    /**
     * Configures which passes are registered. Calling this block switches from auto-mode (default
     * pass pipeline) to explicit mode; use [PassesBuilder.default] to re-include the defaults.
     */
    fun passes(init: PassesBuilder.() -> Unit) {
        val builder = passesBuilder ?: PassesBuilder().also { passesBuilder = it }
        builder.init()
    }

    /**
     * Configures how components are determined. Calling this block switches from auto-mode (run all
     * language-based detectors) to explicit mode; use [ComponentsBuilder.default] to re-enable
     * auto-detection.
     */
    fun components(init: ComponentsBuilder.() -> Unit) {
        val builder = componentsBuilder ?: ComponentsBuilder().also { componentsBuilder = it }
        builder.init()
    }

    /** Excludes files and directories matching the given [patterns] from the analysis. */
    fun exclude(vararg patterns: String) {
        excludesByString += patterns
    }

    /** Excludes files and directories matching the given regex [patterns] from the analysis. */
    fun exclude(vararg patterns: Regex) {
        excludesByRegex += patterns
    }

    /**
     * Adds a standalone [Detector] (a [ComponentDetector] and/or [ProjectDetector]) that is not
     * tied to a [Language], such as the [DirectoryComponentDetector]. Standalone detectors run
     * before the language-based ones, so they take precedence in case of conflicts.
     *
     * This is equivalent to `components { detector(detector) }`.
     */
    fun detector(detector: Detector) {
        standaloneDetectors += detector
    }

    /**
     * Explicitly registers a [Language]. Equivalent to `languages { use(clazz) }`.
     *
     * Calling this method implicitly switches languages from auto-mode to explicit mode, so only
     * the registered languages (plus any loaded via [LanguagesBuilder.default]) are used.
     */
    fun registerLanguage(clazz: KClass<out Language<*>>) {
        languages { use(clazz) }
    }

    /** Explicitly registers a [Language]. Equivalent to `languages { use<T>() }`. */
    inline fun <reified T : Language<*>> registerLanguage() {
        registerLanguage(T::class)
    }

    /**
     * Adds an explicit component definition. Equivalent to `components { component(...) }`.
     *
     * Calling this method implicitly switches components from auto-mode to explicit mode. Combine
     * with `components { default() }` to keep auto-detection alongside the explicit component.
     */
    fun component(name: String, root: Path = path, sources: List<Path> = listOf(root)) {
        components { component(name, root, sources) }
    }

    /**
     * An escape hatch for options that are not (yet) exposed through the project API. The
     * [modifier] is applied to the underlying [TranslationConfiguration.Builder] after all
     * project-level configuration, so it can override any derived option.
     */
    fun translation(modifier: (TranslationConfiguration.Builder) -> Unit) {
        configModifiers += modifier
    }

    /** Resolves this builder into a [Project], running project auto-detection if enabled. */
    internal fun resolve(): Project {
        val languages = resolveLanguages()

        // Determine whether language-based detectors should run.
        val cb = componentsBuilder
        val languageAutoDetect = path.isDirectory() && (cb == null || cb.includeAutoDetect)

        // Collect detectors: standalone (always) + language-based (only in auto-detect mode).
        val allDetectors = buildList {
            if (path.isDirectory()) {
                // Standalone detectors from the top-level builder and from the components block run
                // regardless of auto-mode, since the user explicitly added them.
                addAll(standaloneDetectors)
                addAll(cb?.detectors ?: emptyList())
            }
            if (languageAutoDetect) {
                addAll(languages.mapNotNull { instantiate(it) }.filterIsInstance<Detector>())
            }
        }

        val detectionResults = detectSettings(allDetectors.filterIsInstance<ProjectDetector>())

        // Explicit components win over detected ones. If neither exist, use a single default
        // component spanning the whole path.
        val explicitComponents = cb?.explicit ?: emptyList()
        val components =
            explicitComponents.ifEmpty {
                detectComponents(allDetectors.filterIsInstance<ComponentDetector>()).ifEmpty {
                    defaultComponents()
                }
            }

        val builder = TranslationConfiguration.builder().targetEnvironment(environment)

        resolvePasses(builder)

        languages.forEach { builder.registerLanguage(it) }

        builder.softwareComponents(
            components.associate { it.name to it.sources.map(Path::toFile) }.toMutableMap()
        )
        builder.topLevels(components.associate { it.name to it.root.toFile() })

        builder.exclusionPatterns(*excludesByString.toTypedArray())
        builder.exclusionPatterns(*excludesByRegex.toTypedArray())

        val symbols = detectionResults.flatMap { it.symbols.entries }.associate { it.toPair() }
        if (symbols.isNotEmpty()) {
            builder.symbols(symbols)
        }
        detectionResults.flatMap { it.includePaths }.forEach { builder.includePath(it) }
        detectionResults
            .firstNotNullOfOrNull { it.compilationDatabase }
            ?.let { builder.useCompilationDatabase(it) }

        configModifiers.forEach { it(builder) }

        return Project(
            name = name,
            directory = if (path.isDirectory()) path else null,
            components = components,
            environment = environment,
            detectionResults = detectionResults,
            config = builder.build(),
        )
    }

    private fun resolveLanguages(): Set<KClass<out Language<*>>> {
        return when (val lb = languagesBuilder) {
            null -> detectLanguages()
            else ->
                buildSet {
                    if (lb.includeDefaults) addAll(detectLanguages())
                    addAll(lb.explicit)
                }
        }
    }

    private fun resolvePasses(builder: TranslationConfiguration.Builder) {
        when (val pb = passesBuilder) {
            null -> builder.defaultPasses()
            else -> {
                if (pb.includeDefaults) builder.defaultPasses()
                pb.explicit.forEach { builder.registerPass(it) }
            }
        }
    }

    /**
     * Runs all [ProjectDetector]s on the project directory. If several detectors report a result
     * with the same [DetectionResult.detector] name (e.g., two related languages sharing detection
     * logic), only the first result is kept.
     */
    private fun detectSettings(detectors: List<ProjectDetector>): List<DetectionResult> {
        return detectors
            .mapNotNull { detector ->
                val result = detector.detect(path, environment)
                if (result != null) {
                    log.info("Project detection ({}): {}", result.detector, result)
                }
                result
            }
            .distinctBy { it.detector }
    }

    /**
     * Walks the project directory tree and asks each [ComponentDetector] for components rooted in
     * the visited directories. Hidden directories and common dependency folders (such as `vendor`
     * or `node_modules`) are skipped. If several detectors report components with the same name,
     * only the first one is kept.
     */
    private fun detectComponents(detectors: List<ComponentDetector>): List<ComponentDefinition> {
        if (detectors.isEmpty()) {
            return listOf()
        }

        val components =
            path
                .toFile()
                .walkTopDown()
                .onEnter { !it.name.startsWith(".") && it.name !in skippedDirectories }
                .filter { it.isDirectory }
                .flatMap { dir ->
                    detectors.flatMap { it.detectComponents(dir.toPath(), environment) }
                }
                .distinctBy { it.name }
                .toList()

        components.forEach { log.info("Detected component '{}' rooted at {}", it.name, it.root) }

        return components
    }

    private fun defaultComponents(): List<ComponentDefinition> {
        return if (path.isDirectory()) {
            listOf(ComponentDefinition(DEFAULT_APPLICATION_NAME, root = path))
        } else {
            // An ad-hoc project for a single file; its parent directory serves as top-level
            listOf(
                ComponentDefinition(
                    DEFAULT_APPLICATION_NAME,
                    root = path.toAbsolutePath().parent,
                    sources = listOf(path),
                )
            )
        }
    }

    /**
     * Loads all languages from [Project.defaultLanguages] that are on the classpath, then filters
     * them down to those that can actually handle files in [path]:
     * - A language with no declared [Language.fileExtensions] is always included (it uses its own
     *   detection logic, e.g. [ComponentDetector]).
     * - A language with declared extensions is only included when at least one file with a matching
     *   extension exists anywhere under [path].
     *
     * Falls back to loading all available languages when [path] is not a directory (single-file
     * projects) or when the directory walk produces no extensions at all.
     */
    private fun detectLanguages(): Set<KClass<out Language<*>>> {
        val allAvailable = loadDefaultLanguages()
        if (!path.isDirectory()) return allAvailable

        val presentExtensions = scanExtensions()
        if (presentExtensions.isEmpty()) return allAvailable

        return allAvailable
            .filter { clazz ->
                val lang = instantiate(clazz) ?: return@filter false
                // No declared extensions → always activate (language uses its own detection)
                lang.fileExtensions.isEmpty() ||
                    lang.fileExtensions.any { it.lowercase() in presentExtensions }
            }
            .toSet()
    }

    /** Collects every unique (lowercased) file extension found under [path]. */
    private fun scanExtensions(): Set<String> {
        return path
            .toFile()
            .walkTopDown()
            .onEnter { !it.name.startsWith(".") && it.name !in skippedDirectories }
            .filter { it.isFile }
            .mapNotNullTo(mutableSetOf()) { file ->
                file.extension.lowercase().takeIf { it.isNotEmpty() }
            }
    }

    private fun loadDefaultLanguages(): Set<KClass<out Language<*>>> {
        return defaultLanguagesOverride
            ?: Project.defaultLanguages
                .mapNotNull {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        Class.forName(it).kotlin as? KClass<out Language<*>>
                    } catch (_: ClassNotFoundException) {
                        null
                    }
                }
                .toSet()
    }

    private fun instantiate(clazz: KClass<out Language<*>>): Language<*>? {
        return try {
            clazz.constructors.firstOrNull()?.call()
        } catch (e: Exception) {
            log.warn("Could not instantiate language {} for project detection", clazz.simpleName)
            null
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProjectBuilder::class.java)

        /** Directory names that are never entered during component detection. */
        private val skippedDirectories = setOf("vendor", "node_modules", "testdata")
    }
}
