/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis

import de.fraunhofer.aisec.cpg.console.Neo4jPlugin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.kotlinx.ki.shell.Command
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfigurationBase

class Neo4jPluginTest {
    object TestConfig : ReplConfigurationBase()

    @Test
    fun testExecute() {
        val plugin = Neo4jPlugin().Load(TestConfig)

        // not enough parameters
        var result = plugin.execute(":export")
        assertTrue(result is Command.Result.Failure)

        // with default parameters
        result = plugin.execute(":export neo4j")
        assertTrue(result is Command.Result.RunSnippets)
        assertEquals(3, result.snippetsToRun.toList().size)

        // with specified username/password
        result = plugin.execute(":export neo4j neo4j mypassword")
        assertTrue(result is Command.Result.RunSnippets)
        assertEquals(5, result.snippetsToRun.toList().size)
    }
}
