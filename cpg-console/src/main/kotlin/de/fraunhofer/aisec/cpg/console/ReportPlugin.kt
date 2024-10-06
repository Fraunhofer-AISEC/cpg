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

class ReportPlugin : Plugin {
    inner class Load(conf: ReplConfiguration) : BaseCommand() {
        override val name: String by conf.get(default = "report")
        override val short: String by conf.get(default = "rp")
        override val description: String =
            "creates a report for the current result of a :run command. " +
                "Optionally provide a path to write the report to and a flag to minify the output." +
                "requires a `reporter` and `rules` to be set, e.g. by running the :loadReporter and :runRule commands."

        override val params = "[path minify]"

        override fun execute(line: String): Command.Result {
            // println(line.split(" ")) // [":report", "<path>", "<minify>"]
            var path = ""
            var minify = ""
            line.split(" ").let {
                when (it.size) {
                    3 -> {
                        path = it[1]
                        minify = it[2]
                    }
                    2 -> {
                        if (it[1].toBoolean()) {
                            minify = it[1]
                        } else {
                            path = it[1]
                        }
                    }
                    else -> {
                        // nothing to do
                    }
                }
            }
            var toRun =
                "reporter.toFile(reporter.report(rules, $minify), kotlin.io.path.Path(\"$path\"))"
            // println(toRun)
            return Command.Result.RunSnippets(listOf(toRun))
        }
    }

    lateinit var repl: Shell

    override fun init(repl: Shell, config: ReplConfiguration) {
        this.repl = repl

        repl.registerCommand(Load(config))
    }

    override fun cleanUp() {
        // nothing to do
    }
}
