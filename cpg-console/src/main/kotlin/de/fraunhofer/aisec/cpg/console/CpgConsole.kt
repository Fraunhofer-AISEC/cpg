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

import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm
import org.jetbrains.kotlinx.ki.shell.KotlinShell
import org.jetbrains.kotlinx.ki.shell.Plugin
import org.jetbrains.kotlinx.ki.shell.Shell
import org.jetbrains.kotlinx.ki.shell.configuration.CachedInstance
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfiguration
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfigurationBase

object CpgConsole {
    @JvmStatic
    fun main(args: Array<String>) {
        val repl =
            Shell(
                configuration(),
                defaultJvmScriptingHostConfiguration,
                ScriptCompilationConfiguration {
                    jvm {
                        dependenciesFromClassloader(
                            classLoader = KotlinShell::class.java.classLoader,
                            wholeClasspath = true
                        )
                    }
                },
                ScriptEvaluationConfiguration {
                    jvm { baseClassLoader(Shell::class.java.classLoader) }
                }
            )

        Runtime.getRuntime()
            .addShutdownHook(
                Thread {
                    println("\nBye!")
                    repl.cleanUp()
                }
            )

        repl.doRun()
    }

    private fun configuration(): ReplConfiguration {
        val instance = CachedInstance<ReplConfiguration>()
        val klassName: String? = System.getProperty("config.class")

        return if (klassName != null) {
            instance.load(klassName, ReplConfiguration::class)
        } else {
            instance.get {
                object : ReplConfigurationBase() {
                    override fun plugins(): Iterator<Plugin> {
                        val list = super.plugins().asSequence().toList().toMutableList()
                        list += AnalyzePlugin()
                        list += Neo4jPlugin()
                        list += ShowCodePlugin()
                        list += RunPlugin()

                        return list.listIterator()
                    }
                }
            }
        }
    }
}
