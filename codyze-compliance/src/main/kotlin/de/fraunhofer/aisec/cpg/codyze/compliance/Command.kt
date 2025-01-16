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
package de.fraunhofer.aisec.cpg.codyze.compliance

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import kotlin.io.path.Path

/** Options common to all subcommands. */
class ProjectOptions : OptionGroup("Project Options:") {
    val directory by option("--project-dir", help = "The project directory").default(".")
}

/** The main `compliance` command. */
class ComplianceCommand : CliktCommand() {
    override fun run() {}
}

/** The `scan` command. This will scan the project for compliance violations in the future. */
class ScanCommand : CliktCommand() {
    private val projectOptions by ProjectOptions()

    override fun run() {
        TODO()
    }
}

/**
 * The `list-security-goals` command. This will list the names of all security goals in the
 * specified project.
 *
 * This command assumes that the project contains a folder named `security-goals` that contains YAML
 * files with the security goals.
 */
class ListSecurityGoals : CliktCommand() {
    private val projectOptions by ProjectOptions()

    override fun run() {
        val goals = loadSecurityGoals(Path(projectOptions.directory).resolve("security-goals"))
        goals.forEach { echo(it.name.localName) }
    }
}

var Command = ComplianceCommand().subcommands(ScanCommand(), ListSecurityGoals())
