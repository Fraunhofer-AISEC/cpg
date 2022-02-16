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

class TranslatePlugin : Plugin {
    inner class Load(conf: ReplConfiguration) : BaseCommand() {
        override val name: String by conf.get(default = "translate")
        override val short: String by conf.get(default = "tr")
        override val description: String =
            "translates the source code files at the path into the CPG"

        override val params = "<path>"

        override fun execute(line: String): Command.Result {
            val p = line.indexOf(' ')
            val path = line.substring(p + 1).trim()

            return Command.Result.RunSnippets(
                listOf(
                    // basics
                    "import de.fraunhofer.aisec.cpg.TranslationConfiguration",
                    "import de.fraunhofer.aisec.cpg.TranslationManager",
                    // go
                    "import de.fraunhofer.aisec.cpg.frontends.golang.GoLanguageFrontend",
                    // llvm
                    "import de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguageFrontend",
                    // python
                    "import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend",
                    // all the graph nodes
                    "import de.fraunhofer.aisec.cpg.graph.*",
                    "import de.fraunhofer.aisec.cpg.graph.declarations.*",
                    "import de.fraunhofer.aisec.cpg.graph.statements.*",
                    "import de.fraunhofer.aisec.cpg.graph.statements.expressions.*",
                    // helper builtins
                    "import de.fraunhofer.aisec.cpg.graph.evaluate",
                    "import de.fraunhofer.aisec.cpg.graph.all",
                    "import de.fraunhofer.aisec.cpg.graph.ast",
                    "import de.fraunhofer.aisec.cpg.graph.dfgFrom",
                    "import de.fraunhofer.aisec.cpg.graph.byName",
                    "import de.fraunhofer.aisec.cpg.graph.body",
                    "import de.fraunhofer.aisec.cpg.graph.capacity",
                    "import de.fraunhofer.aisec.cpg.console.printCode",
                    // some basic java stuff
                    "import java.io.File",
                    // lets build and analyze
                    "@OptIn(de.fraunhofer.aisec.cpg.ExperimentalGolang::class, de.fraunhofer.aisec.cpg.ExperimentalPython::class) val config =\n" +
                        "                TranslationConfiguration.builder()\n" +
                        "                    .sourceLocations(File(\"" +
                        path +
                        "\"))\n" +
                        "                    .defaultLanguages()\n" +
                        //                      "
                        // .registerLanguage(GoLanguageFrontend::class.java,
                        // GoLanguageFrontend.GOLANG_EXTENSIONS)" +
                        "                    .registerLanguage(LLVMIRLanguageFrontend::class.java, LLVMIRLanguageFrontend.LLVM_EXTENSIONS)" +
                        "                    .registerLanguage(PythonLanguageFrontend::class.java, PythonLanguageFrontend.PY_EXTENSIONS)" +
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
