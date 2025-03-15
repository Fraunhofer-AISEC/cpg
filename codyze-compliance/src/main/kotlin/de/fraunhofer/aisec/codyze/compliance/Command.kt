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
package de.fraunhofer.aisec.codyze.compliance

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import de.fraunhofer.aisec.codyze.*
import de.fraunhofer.aisec.codyze.console.ConsoleService
import de.fraunhofer.aisec.codyze.console.startServer
import java.io.File

/** The main `compliance` command. */
class ComplianceCommand : CliktCommand() {
    override fun run() {}
}

/**
 * A command that operates on a project. This class provides the common options and functions for
 * all commands.
 */
abstract class ProjectCommand : CliktCommand() {
    protected val projectOptions by ProjectOptions()
    protected val translationOptions by TranslationOptions()
}

/** The `scan` command. This will scan the project for compliance violations in the future. */
open class ScanCommand : ProjectCommand() {
    override fun run() {
        val project =
            AnalysisProject.fromOptions(
                projectOptions,
                translationOptions,
                postProcess = AnalysisProject::executeSecurityGoalsQueries,
            ) {
                // just to show that we can use a config build here
                it
            }
        val result = project.analyze()
        result.writeSarifJson(File("findings.json"))

        result.sarif.runs.forEach { run ->
            run.results?.forEach { result -> echo(result.message.toString()) }
        }

        if (projectOptions.startServer) {
            ConsoleService.fromAnalysisResult(result).startServer()
        }
    }
}

/**
 * The `list-security-goals` command. This will list the names of all security goals in the
 * specified project.
 *
 * This command assumes that the project contains a folder named `security-goals` that contains YAML
 * files with the security goals.
 */
class ListSecurityGoals : ProjectCommand() {
    override fun run() {
        val project = AnalysisProject.fromOptions(projectOptions, translationOptions)
        val goals = project.loadSecurityGoals()

        // Print the name of each security goal
        goals.forEach { echo(it.name.localName) }
    }
}

var Command = ComplianceCommand().subcommands(ScanCommand(), ListSecurityGoals())
