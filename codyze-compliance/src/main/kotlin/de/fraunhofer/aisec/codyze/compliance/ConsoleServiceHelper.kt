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
package de.fraunhofer.aisec.codyze.compliance

import de.fraunhofer.aisec.codyze.AnalysisResult
import org.slf4j.LoggerFactory

/**
 * Helper object to make the web console from the (optional) `codyze-console` module conditionally
 * available. When the module is available in the build, this starts the actual console via
 * reflection.
 */
object ConsoleServiceHelper {
    private val log = LoggerFactory.getLogger(ConsoleServiceHelper::class.java)

    /** Check if the codyze-console module is enabled. */
    val isEnabled: Boolean by lazy {
        try {
            Class.forName("de.fraunhofer.aisec.codyze.console.ConsoleService")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    /** Starts the web console for the given [result], if codyze-console is enabled. */
    fun startConsole(result: AnalysisResult) {
        if (!isEnabled) {
            log.warn(
                "The --console option was set, but the codyze-console module is not enabled in " +
                    "this build (set enableCodyzeConsole=true in gradle.properties)."
            )
            return
        }

        try {
            val consoleServiceClass =
                Class.forName("de.fraunhofer.aisec.codyze.console.ConsoleService")
            val companion = consoleServiceClass.getField("Companion").get(null)
            val service =
                companion.javaClass
                    .getMethod("fromAnalysisResult", AnalysisResult::class.java)
                    .invoke(companion, result)

            val mainKt = Class.forName("de.fraunhofer.aisec.codyze.console.MainKt")
            val startConsole =
                mainKt.methods.first { it.name == "startConsole" && it.parameterCount == 3 }
            startConsole.invoke(null, service, "localhost", 8080)
        } catch (e: Exception) {
            log.error("Failed to start web console: {}", e.message, e)
        }
    }
}
