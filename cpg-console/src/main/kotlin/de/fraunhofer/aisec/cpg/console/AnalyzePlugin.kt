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
package de.fraunhofer.aisec.cpg.console

import org.jetbrains.kotlinx.ki.shell.BaseCommand
import org.jetbrains.kotlinx.ki.shell.Command
import org.jetbrains.kotlinx.ki.shell.Plugin
import org.jetbrains.kotlinx.ki.shell.Shell
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration

class AnalyzePlugin : Plugin {
    inner class Load(conf: ReplConfiguration) : BaseCommand() {
        override val name: String by conf.get(default = "analyze")
        override val short: String by conf.get(default = "a")
        override val description: String = "analyzes the path"

        override val params = "<path>"

        override fun execute(line: String): Command.Result {
            val p = line.indexOf(' ')
            val path = line.substring(p + 1).trim()

            return Command.Result.RunSnippets(
                listOf(
                    // basics
                    "import de.fraunhofer.aisec.cpg.TranslationConfiguration",
                    "import de.fraunhofer.aisec.cpg.TranslationManager",
                    // all the graph nodes
                    "import de.fraunhofer.aisec.cpg.graph.*",
                    "import de.fraunhofer.aisec.cpg.graph.declarations.*",
                    "import de.fraunhofer.aisec.cpg.graph.statements.*",
                    // helper builtins
                    "import de.fraunhofer.aisec.cpg.analysis.resolve",
                    "import de.fraunhofer.aisec.cpg.analysis.byName",
                    "import de.fraunhofer.aisec.cpg.analysis.body",
                    "import de.fraunhofer.aisec.cpg.analysis.printCode",
                    // some basic java stuff
                    "import java.io.File",
                    // lets build and analyze
                    "val config =\n" +
                        "                TranslationConfiguration.builder()\n" +
                        "                    .sourceLocations(File(\"" +
                        path +
                        "\"))\n" +
                        "                    .defaultLanguages()\n" +
                        "                    .defaultPasses()\n" +
                        "                    .build()",
                    "val analyzer = TranslationManager.builder().config(config).build()",
                    "val result = analyzer.analyze().get()",
                    // for convenience
                    "val tu = result.translationUnits.first()"
                )
            )

            // return Command.Result.RunSnippets(listOf(content))
        }
    }

    lateinit var repl: Shell

    override fun init(repl: Shell, config: ReplConfiguration) {
        this.repl = repl

        repl.registerCommand(Load(config))
    }

    override fun cleanUp() {}
}
