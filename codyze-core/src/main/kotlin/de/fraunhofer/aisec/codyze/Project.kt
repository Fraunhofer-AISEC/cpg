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

/** Options common to all subcommands dealing projects. */
class ProjectOptions : OptionGroup("Project Options:") {
    val directory by
        option("--project-dir", help = "The project directory").path().default(Path("."))
}

/** Options common to all subcommands dealing with CPG translation. */
class TranslationOptions : OptionGroup("CPG Translation Options:") {
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
 * @param run The SARIF run object, that contains findings.
 */
data class AnalysisResult(val translationResult: TranslationResult, val run: Run)

/** Analyzes the project and returns the result. */
fun analyze(config: TranslationConfiguration): AnalysisResult {
    val run =
        Run(
            tool = Tool(driver = ToolComponent(name = "Codyze", version = "x.x.x")),
            results =
                listOf(
                    Result(
                        rule = ReportingDescriptorReference(id = "Rule1"),
                        message = Message(markdown = "This is a **finding**"),
                        level = Level.Note,
                        locations = listOf(),
                    )
                ),
        )

    val result = TranslationManager.builder().config(config).build().analyze().get()

    return AnalysisResult(run = run, translationResult = result)
}

/** Builds a translation configuration from the given CLI options. */
fun buildConfig(
    projectOptions: ProjectOptions,
    translationOptions: TranslationOptions,
): TranslationConfiguration {
    var builder =
        TranslationConfiguration.builder()
            .defaultPasses()
            .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage")

    // We can either have a single source (using --sources) or multiple components (using
    // --components)
    translationOptions.sources?.let {
        builder =
            builder
                .sourceLocations(translationOptions.sources!!.map { it.toFile() })
                .topLevel(projectOptions.directory.toFile())
    }

    translationOptions.components?.let {
        val componentDir = projectOptions.directory.toFile().resolve("components")
        val pairs =
            it.map { component ->
                Pair(component, mutableListOf<File>(componentDir.resolve(component)))
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
                .topLevel(componentDir)
    }

    translationOptions.exclusionPatterns?.forEach {
        builder = builder.exclusionPatterns(it)
    }

    return builder.build()
}
