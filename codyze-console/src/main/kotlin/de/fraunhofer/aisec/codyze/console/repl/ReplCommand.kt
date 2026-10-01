/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.codyze.console.repl

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import de.fraunhofer.aisec.codyze.console.AnalyzeRequestJSON
import de.fraunhofer.aisec.codyze.console.ConsoleService
import kotlinx.coroutines.runBlocking

/**
 * `codyze repl` — start an interactive Kotlin REPL with semantic completion against the analyzed
 * CPG.
 *
 * Two ways to load a project:
 * * positional `<source-dir>` (with optional `--include`, `--top-level`, `--concepts`) for an
 *   ad-hoc analysis;
 * * `--project <dir>` to load a saved Codyze project directory (with a `codyze.yaml` or similar).
 *
 * If neither is given, the REPL starts empty and the user can `:reload <path>` to analyze on
 * demand.
 */
class ReplCommand : CliktCommand(name = "repl") {

    private val sourceDir by argument("source-dir", help = "Source directory to analyze").optional()

    private val includeDir by
        option("--include", help = "Include path passed to the translation configuration").path()

    private val topLevel by
        option("--top-level", help = "Top-level directory of the project").path()

    private val conceptsFile by
        option("--concepts", help = "Path to a persisted concepts YAML file").path()

    override fun run() {
        // Print the banner up-front so it isn't buried under the analyzer's log output.
        // Analysis can take several seconds; the banner is a clear "you're in the right
        // place, wait" signal during that gap.
        ReplLoop.printStartupBanner(System.out, sourceDir)
        System.out.flush()

        val consoleService = ConsoleService()
        if (sourceDir != null) {
            val request =
                AnalyzeRequestJSON(
                    sourceDir = sourceDir!!,
                    includeDir = includeDir?.toString(),
                    topLevel = topLevel?.toString(),
                    conceptsFile = conceptsFile?.toString(),
                )
            runBlocking { consoleService.analyze(request) }
        }

        val replService = ReplService(consoleService)
        // Pre-warm the Kotlin scripting compiler with a no-op snippet so the first real
        // query the user types feels instant. The cold start (compiler init, classpath
        // scan, IDE-services bootstrap) costs ~2–4s on a modern laptop; we'd rather
        // pay that cost here while the user is still reading the banner than make them
        // wait at the first `cpg>` prompt.
        if (replService.translationResult != null) {
            try {
                replService.eval("0")
            } catch (_: Throwable) {
                // pre-warm is best-effort; failures shouldn't block REPL startup
            }
        }
        ReplLoop(consoleService, replService).run()
    }
}

/** Subcommand instance registered by the codyze CLI. */
val Command = ReplCommand()
