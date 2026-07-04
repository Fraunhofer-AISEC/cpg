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
 * The simplest usages are:
 * ```kotlin
 * val result = Project.from(Path("main.cpp")).analyze()
 * val result = Project.from(Path("/path/to/repo")).analyze()
 * ```
 *
 * More control is available through the builder DSL:
 * ```kotlin
 * val project =
 *     project(Path("/path/to/repo")) {
 *         exclude("tests")
 *         environment {
 *             os = OperatingSystem.LINUX
 *             architecture = Architecture.ARM64
 *         }
 *         component("backend", root = Path("/path/to/repo/services/backend"))
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
         * [ProjectBuilder.registerLanguage].
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
 * Collects all user-supplied configuration for a [Project] and resolves it into an immutable
 * [Project], including the derived [TranslationConfiguration].
 */
class ProjectBuilder(
    /** The path to the project: either a single source file or a directory. */
    val path: Path
) {
    /** The name of the project. Defaults to the file or directory name of [path]. */
    var name: String = path.fileName?.toString() ?: DEFAULT_APPLICATION_NAME

    /**
     * Whether [ComponentDetector]s and [ProjectDetector]s should auto-detect the project structure
     * and settings. Only applies if [path] is a directory.
     */
    var autoDetect: Boolean = true

    /**
     * Whether the default passes (see [TranslationConfiguration.Builder.defaultPasses]) should be
     * registered. Disable this only if you want to work with the raw AST or register a completely
     * custom set of passes with [translation].
     */
    var defaultPasses: Boolean = true

    private val components = mutableListOf<ComponentDefinition>()
    private val excludesByString = mutableListOf<String>()
    private val excludesByRegex = mutableListOf<Regex>()
    private val languages = mutableSetOf<KClass<out Language<*>>>()
    private val detectors = mutableListOf<Detector>()
    private val configModifiers = mutableListOf<(TranslationConfiguration.Builder) -> Unit>()
    private var environment = TargetEnvironment.host()

    /** Configures the [TargetEnvironment] this project is assumed to run on. */
    fun environment(init: TargetEnvironmentBuilder.() -> Unit) {
        val builder = TargetEnvironmentBuilder()
        builder.init()
        environment = builder.build()
    }

    /**
     * Adds a component to the project. If no component is defined (and none is auto-detected), a
     * single default component spanning the whole [path] is created.
     */
    fun component(name: String, root: Path = path, sources: List<Path> = listOf(root)) {
        components += ComponentDefinition(name, root, sources)
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
     * Explicitly registers a [Language]. If no language is registered explicitly, all languages in
     * [Project.defaultLanguages] that are available on the classpath are used.
     */
    fun registerLanguage(clazz: KClass<out Language<*>>) {
        languages += clazz
    }

    /** Explicitly registers a [Language]. */
    inline fun <reified T : Language<*>> registerLanguage() {
        registerLanguage(T::class)
    }

    /**
     * Adds a standalone [Detector] (a [ComponentDetector] and/or [ProjectDetector]) that is not
     * tied to a [Language], such as the [DirectoryComponentDetector]. Standalone detectors run
     * before the language-based ones, so they take precedence in case of conflicts.
     */
    fun detector(detector: Detector) {
        detectors += detector
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
        val languages = languages.ifEmpty { loadDefaultLanguages() }

        // Standalone detectors run before language-based ones, so they win in case of conflicts
        val allDetectors =
            if (path.isDirectory() && autoDetect) {
                detectors + languages.mapNotNull { instantiate(it) }.filterIsInstance<Detector>()
            } else {
                listOf()
            }

        val detectionResults = detectSettings(allDetectors.filterIsInstance<ProjectDetector>())

        // Explicitly configured components always win over detected ones. If neither exist, we
        // create a single default component spanning the whole path.
        val components =
            components.ifEmpty {
                detectComponents(allDetectors.filterIsInstance<ComponentDetector>()).ifEmpty {
                    defaultComponents()
                }
            }

        val builder = TranslationConfiguration.builder().targetEnvironment(environment)
        if (defaultPasses) {
            builder.defaultPasses()
        }
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

    private fun loadDefaultLanguages(): Set<KClass<out Language<*>>> {
        return Project.defaultLanguages
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
