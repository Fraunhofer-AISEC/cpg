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
package de.fraunhofer.aisec.cpg.concepts

import de.fraunhofer.aisec.cpg.analysis.MultiValueEvaluator
import de.fraunhofer.aisec.cpg.frontends.ini.IniFileLanguage
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.config.LoadConfigurationFile
import de.fraunhofer.aisec.cpg.graph.statements.expressions.SubscriptExpression
import de.fraunhofer.aisec.cpg.passes.concepts.config.ini.IniFileConfigurationPass
import de.fraunhofer.aisec.cpg.passes.concepts.config.python.PythonStdLibConfigurationPass
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ConfigurationPassTest {
    @Test
    fun testPythonLoadIni() {
        val topLevel = File("src/integrationTest/resources/python")
        val result =
            analyze(listOf(), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
                it.registerLanguage<IniFileLanguage>()
                it.registerPass<IniFileConfigurationPass>()
                it.registerPass<PythonStdLibConfigurationPass>()
                it.softwareComponents(
                    mutableMapOf(
                        "conf" to listOf(topLevel.resolve("conf")),
                        "mypackage" to listOf(topLevel.resolve("mypackage")),
                        "stdlib" to listOf(topLevel.resolve("stdlib")),
                    )
                )
                it.topLevels(
                    mapOf(
                        "conf" to topLevel.resolve("conf"),
                        "mypackage" to topLevel.resolve("mypackage"),
                        "stdlib" to topLevel.resolve("stdlib"),
                    )
                )
            }
        assertNotNull(result)

        val subs = result.allChildren<SubscriptExpression>()
        assertEquals(4, subs.size)

        val loadConfig =
            result.operationNodes.filterIsInstance<LoadConfigurationFile>().singleOrNull()
        assertNotNull(loadConfig)

        val port = result.refs["port"]
        assertNotNull(port)
        assertEquals(setOf("80"), port.evaluate(MultiValueEvaluator()))

        val sslEnabled = result.refs["ssl_enabled"]
        assertNotNull(sslEnabled)
        assertEquals(setOf("true"), sslEnabled.evaluate(MultiValueEvaluator()))
    }
}
