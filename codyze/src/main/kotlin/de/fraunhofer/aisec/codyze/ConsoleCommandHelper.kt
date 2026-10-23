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
package de.fraunhofer.aisec.codyze

import com.github.ajalt.clikt.core.CliktCommand
import org.slf4j.LoggerFactory

/**
 * Helper object to make the `console` subcommand from the (optional) `codyze-console` module
 * conditionally available. When the module is available in the build, this loads the actual command
 * via reflection.
 */
object ConsoleCommandHelper {
    private val log = LoggerFactory.getLogger(ConsoleCommandHelper::class.java)

    /** Check if the codyze-console module is enabled. */
    val isEnabled: Boolean by lazy {
        try {
            Class.forName("de.fraunhofer.aisec.codyze.console.CommandKt")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    /** Returns the `console` subcommand, or `null` if codyze-console is not enabled. */
    fun consoleCommand(): CliktCommand? {
        if (!isEnabled) {
            return null
        }

        return try {
            val commandKt = Class.forName("de.fraunhofer.aisec.codyze.console.CommandKt")
            commandKt.getMethod("getCommand").invoke(null) as? CliktCommand
        } catch (e: Exception) {
            log.error("Failed to load codyze-console command: {}", e.message, e)
            null
        }
    }
}
