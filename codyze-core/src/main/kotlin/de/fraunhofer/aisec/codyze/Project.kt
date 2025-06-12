/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.codyze

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.path
import de.fraunhofer.aisec.codyze.dsl.ProjectBuilder
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.assumptions.Assumption
import de.fraunhofer.aisec.cpg.assumptions.AssumptionStatus
import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.query.QueryTree
import io.github.detekt.sarif4k.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.uuid.Uuid

/** Options common to all subcommands dealing projects. */
class ProjectOptions : OptionGroup("Project Options") {
    val directory by
        option("--project-dir", help = "The project directory").path().default(Path("."))

    val startConsole by
        option("--console", help = "Starts the Codyze web console after the analysis")
            .boolean()
            .default(false)
}

/** Options common to all subcommands dealing with CPG translation. */
class TranslationOptions : OptionGroup("CPG Translation Options") {
    val sources: List<Path>? by
        option(
                "--sources",
                help = "A list of source files. They will be all added to a single component 'app'.",
            )
            .path()
            .multiple()
    val components: List<String>? by
        option(
                "--components",
                help =
                    "The components to analyze. They must be located inside the 'components' folder inside the project directory. The 'components' folder will be taken as the topLevel property for the translation configuration.",
            )
            .multiple()

    val exclusionPatterns: List<String>? by
        option("--exclusion-patterns", help = "A pattern of files to exclude").multiple()
}

/**
 * Represents the result of the analysis.
 *
 * @param translationResult The result of the CPG translation.
 * @param sarif The SARIF object, that contains findings.
 */
data class AnalysisResult(
    val translationResult: TranslationResult,
    val sarif: SarifSchema210 = SarifSchema210(version = Version.The210, runs = listOf()),
    val requirementsResults: Map<String, QueryTree<Boolean>> = mutableMapOf(),
    val project: AnalysisProject,
) : ContextProvider by translationResult {
    fun writeSarifJson(file: File) {
        file.writeText(SarifSerializer.toJson(sarif))
    }
}

/**
 * Represents an analysis project. This class is responsible for translating the project to a CPG
 * and analyzing it.
 *
 * This class is the base for all commands that analyze a project. It can either be represented by a
 * directory structure or as an ad-hoc project, if we only analyze single source files. In any case,
 * the project is the central entity for the analysis.
 */
class AnalysisProject(
    /**
     * The builder for the project. Potentially null if this is a temporary project that was created
     * using [AnalysisProject.temporary].
     */
    var builder: ProjectBuilder? = null,
    /** The project name. */
    var name: String,
    /** The project directory, if it exists on file. Null if the project is an ad-hoc project. */
    var projectDir: Path?,
    /** The folder where the queries are located. */
    var queriesFolder: Path? = projectDir?.resolve("queries"),
    /** The folder where the security goals are located. */
    var securityGoalsFolder: Path? = projectDir?.resolve("security-goals"),
    /** The folder where the components are located. */
    var componentsFolder: Path? = projectDir?.resolve("components"),
    /**
     * The folder where additional libraries can be placed for analysis, these can be stubs
     * containing only type information or entire source code files. The subfolders are used to
     * create components and the format should be libraries/<componentName>/<namespacefolders>. In
     * the case of a stdlib, this would look like libraries/stdlib/[os,sys, ...]. In the case of an
     * external library it would look like libraries/mylibrary/org/mylibrary/somesubfolder/... if
     * the namespace starts with org.mylibrary. or libraries/mylibrary/mylibrary/somesubfolder/...
     * if the namespace starts with mylibrary.
     */
    var librariesPath: Path? = projectDir?.resolve("libraries"),
    var requirementFunctions: Map<String, TranslationResult.() -> QueryTree<Boolean>> = emptyMap(),
    var assumptionStatusFunctions: Map<String, () -> AssumptionStatus> = emptyMap(),
    var suppressedQueryTreeIDs: Map<(QueryTree<*>) -> Boolean, Any> = emptyMap(),
    /** The translation configuration for the project. */
    var config: TranslationConfiguration,
    /**
     * Any post-process steps that can be applied to the analysis result. This can for example be
     * used to fill [AnalysisResult.sarif].
     */
    var postProcess:
        (AnalysisProject.(AnalysisResult) -> Pair<List<ReportingDescriptor>, List<Result>>)? =
        null,
) {

    /** Analyzes the project and returns the result. */
    fun analyze(): AnalysisResult {
        // Propagate assumption status into a translation result
        assumptionStatusFunctions.forEach { (uuid, func) ->
            Assumption.states[Uuid.parse(uuid)] = func()
        }

        // Propagate suppressed query tree IDs into translation result
        QueryTree.suppressions += suppressedQueryTreeIDs

        val tr = TranslationManager.builder().config(config).build().analyze().get()

        // Run requirements
        val requirementsResults =
            requirementFunctions.map { (name, func) -> Pair(name, func(tr)) }.associate { it }

        // Prepare analysis result
        val runs = mutableListOf<Run>()
        val result =
            AnalysisResult(
                translationResult = tr,
                sarif = SarifSchema210(version = Version.The210, runs = runs),
                requirementsResults = requirementsResults,
                project = this,
            )

        // Create a new SARIF run, including a tool definition and rules corresponding to the
        // individual requirements
        val (rules, results) = buildSarif(result)
        val run =
            Run(
                tool =
                    Tool(driver = ToolComponent(name = "Codyze", version = "x.x.x", rules = rules)),
                results = results,
                originalURIBaseIDS =
                    config.topLevels
                        .mapNotNull { Pair(it.key, it.toSarifLocation()) }
                        .associate { it },
            )
        runs += run

        return result
    }

    companion object {
        /**
         * Builds a new [AnalysisProject] from a directory that contains a `project.codyze.kts`
         * file.
         */
        fun fromDirectory(
            projectDir: Path,
            postProcess:
                (AnalysisProject.(AnalysisResult) -> Pair<
                        List<ReportingDescriptor>,
                        List<Result>,
                    >)? =
                null,
            configModifier:
                ((TranslationConfiguration.Builder) -> TranslationConfiguration.Builder)? =
                null,
        ): AnalysisProject? {
            return fromScript(
                projectDir.resolve("project.codyze.kts"),
                postProcess = postProcess,
                configModifier = configModifier,
            )
        }

        /**
         * Builds a new [AnalysisProject] from a `.codyze.kts` file, which represents a
         * [CodyzeScript].
         */
        fun fromScript(
            file: Path,
            postProcess:
                (AnalysisProject.(AnalysisResult) -> Pair<
                        List<ReportingDescriptor>,
                        List<Result>,
                    >)? =
                null,
            configModifier:
                ((TranslationConfiguration.Builder) -> TranslationConfiguration.Builder)? =
                null,
        ): AnalysisProject? {
            // We need to evaluate the script in order to invoke our project builder inside the
            // script
            val script = evaluateScriptAndIncludes(file)
            if (script == null) {
                return null
            }

            return script.projectBuilder.build(
                postProcess = postProcess,
                configModifier = configModifier,
            )
        }

        /**
         * Builds a temporary [AnalysisProject] from the given values without having a `.codyze.kts`
         * file in place.
         */
        fun temporary(
            projectDir: Path,
            sources: List<Path>? = null,
            components: List<String>? = null,
            exclusionPatterns: List<String>? = null,
            librariesPath: Path? = projectDir.resolve("libraries"),
            postProcess:
                (AnalysisProject.(AnalysisResult) -> Pair<
                        List<ReportingDescriptor>,
                        List<Result>,
                    >)? =
                null,
            configModifier:
                ((TranslationConfiguration.Builder) -> TranslationConfiguration.Builder)? =
                null,
        ): AnalysisProject {
            var builder =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage")
                    .optionalLanguage(
                        "de.fraunhofer.aisec.cpg.frontends.typescript.TypeScriptLanguage"
                    )
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.ruby.RubyLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.jvm.JVMLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.ini.IniFileLanguage")

            // We can either have a single source (using --sources) or multiple components (using
            // --components)
            sources?.let {
                builder =
                    builder
                        .sourceLocations(it.map { source -> source.toFile() })
                        .topLevel(projectDir.toFile())
            }

            components?.let {
                val componentDir = projectDir.resolve("components")
                val pairs =
                    it.map { component ->
                        Pair(
                            component,
                            mutableListOf<File>(componentDir.resolve(component).toFile()),
                        )
                    }
                builder =
                    builder
                        .softwareComponents(
                            pairs
                                .groupingBy { it.first }
                                .aggregate { _, accumulator: MutableList<File>?, element, _ ->
                                    if (accumulator != null) {
                                        accumulator.addAll(element.second)
                                        accumulator
                                    } else {
                                        element.second
                                    }
                                }
                                .toMutableMap()
                        )
                        .topLevels(it.associate { Pair(it, componentDir.resolve(it).toFile()) })
            }

            val addSourcesFolder = librariesPath?.toFile()

            if (librariesPath?.isDirectory() == true) {
                builder.loadIncludes(true)
                addSourcesFolder?.listFiles()?.forEach {
                    builder = builder.includePath(it.toPath())
                }
            }

            exclusionPatterns?.forEach { builder = builder.exclusionPatterns(it) }
            configModifier?.invoke(builder)

            return AnalysisProject(
                config = builder.build(),
                name = projectDir.fileName.toString(),
                librariesPath = librariesPath,
                projectDir = projectDir,
                postProcess = postProcess,
            )
        }

        /** Builds a translation configuration from the given CLI options. */
        fun fromOptions(
            projectOptions: ProjectOptions,
            translationOptions: TranslationOptions,
            postProcess:
                (AnalysisProject.(AnalysisResult) -> Pair<
                        List<ReportingDescriptor>,
                        List<Result>,
                    >)? =
                null,
            configModifier:
                ((TranslationConfiguration.Builder) -> TranslationConfiguration.Builder)? =
                null,
        ): AnalysisProject {
            // Try to load a project from the given directory
            val project =
                fromDirectory(
                    projectOptions.directory,
                    postProcess = postProcess,
                    configModifier = configModifier,
                )
            return project
                ?: // If no project was found, we create a temporary project
                // with the given options
                temporary(
                    projectOptions.directory,
                    translationOptions.sources,
                    translationOptions.components,
                    translationOptions.exclusionPatterns,
                    configModifier = configModifier,
                    postProcess = postProcess,
                )
        }
    }
}
