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
import com.github.ajalt.clikt.parameters.types.path
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import io.github.detekt.sarif4k.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

/** Options common to all subcommands dealing projects. */
class ProjectOptions : OptionGroup("Project Options") {
    val directory by
        option("--project-dir", help = "The project directory").path().default(Path("."))
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
data class AnalysisResult(val translationResult: TranslationResult, val sarif: SarifSchema210) {
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
     * The folder where the additional Sources are located. The subfolders are used to create
     * components and the format should be additionalSources/<componentName>/<namespacefolders>. In
     * the case of a stdlib, this would look like additionalSources/stdlib/[os,sys, ...]. In the
     * case of an external library it would look like
     * additionalSources/mylibrary/org/mylibrary/somesubfolder/... if the namespace starts with
     * org.mylibrary. or additionalSources/mylibrary/mylibrary/somesubfolder/... if the namespace
     * starts with mylibrary.
     */
    var additionalSources: Path? = projectDir?.resolve("libraries"),
    /** The translation configuration for the project. */
    var config: TranslationConfiguration,
) {

    /** Analyzes the project and returns the result. */
    fun analyze(
        postProcess: ((TranslationResult) -> Pair<List<ReportingDescriptor>, List<Result>>)? = null
    ): AnalysisResult {
        val tr = TranslationManager.builder().config(config).build().analyze().get()
        val (rules, results) = postProcess?.invoke(tr) ?: Pair(emptyList(), emptyList())

        // Create a new SARIF run, including a tool definition and rules corresponding to the
        // individual security statements
        val run =
            Run(
                tool =
                    Tool(driver = ToolComponent(name = "Codyze", version = "x.x.x", rules = rules)),
                results = results,
            )

        return AnalysisResult(
            translationResult = tr,
            sarif = SarifSchema210(version = Version.The210, runs = listOf(run)),
        )
    }

    companion object {
        /** Builds a translation configuration from the given project directory. */
        fun from(
            projectDir: Path,
            sources: List<Path>? = null,
            components: List<String>? = null,
            exclusionPatterns: List<String>? = null,
            additionalSources: Path? = projectDir.resolve("libraries"),
            configBuilder:
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

            val addSourcesFolder = additionalSources?.toFile()

            if (additionalSources?.isDirectory() == true) {
                builder.loadIncludes(true)
                addSourcesFolder?.listFiles()?.forEach {
                    builder = builder.includePath(it.toPath())
                }
            }

            exclusionPatterns?.forEach { builder = builder.exclusionPatterns(it) }
            configBuilder?.invoke(builder)

            return AnalysisProject(
                config = builder.build(),
                name = projectDir.fileName.toString(),
                additionalSources = additionalSources,
                projectDir = projectDir,
            )
        }

        /** Builds a translation configuration from the given CLI options. */
        fun fromOptions(
            projectOptions: ProjectOptions,
            translationOptions: TranslationOptions,
            configModifier:
                ((TranslationConfiguration.Builder) -> TranslationConfiguration.Builder)? =
                null,
        ): AnalysisProject {
            return from(
                projectOptions.directory,
                translationOptions.sources,
                translationOptions.components,
                translationOptions.exclusionPatterns,
                configBuilder = configModifier,
            )
        }
    }
}
