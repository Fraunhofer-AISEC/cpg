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

class Neo4jPlugin : Plugin {
    inner class Load(conf: ReplConfiguration) : BaseCommand() {
        override val name: String by conf.get(default = "export")
        override val short: String by conf.get(default = "e")
        override val description: String = "exports the graph into neo4j"

        override val params = "neo4j (<username> <password>)"

        override fun execute(line: String): Command.Result {
            val input = line.split(" ")

            if (input.size < 2) {
                return Command.Result.Failure("not enough arguments")
            }

            val commands =
                mutableListOf(
                    "import de.fraunhofer.aisec.cpg_vis_neo4j.Application",
                    "val neo4j = Application()"
                )

            if (input.size == 4) {
                // configure neo4j username and password
                commands += "neo4j.neo4jPassword = ${input[2]}"
                commands += "neo4j.neo4jUsername = ${input[3]}"
            }

            commands += "neo4j.pushToNeo4j(result)"

            return Command.Result.RunSnippets(commands)
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
