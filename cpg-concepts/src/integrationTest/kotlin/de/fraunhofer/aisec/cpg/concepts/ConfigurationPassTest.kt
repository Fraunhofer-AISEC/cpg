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
import de.fraunhofer.aisec.cpg.graph.concepts.config.Configuration
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationGroup
import de.fraunhofer.aisec.cpg.graph.concepts.config.ConfigurationOption
import de.fraunhofer.aisec.cpg.graph.concepts.config.LoadConfiguration
import de.fraunhofer.aisec.cpg.graph.concepts.config.ReadConfigurationGroup
import de.fraunhofer.aisec.cpg.graph.concepts.config.ReadConfigurationOption
import de.fraunhofer.aisec.cpg.graph.concepts.config.RegisterConfigurationGroup
import de.fraunhofer.aisec.cpg.graph.concepts.config.RegisterConfigurationOption
import de.fraunhofer.aisec.cpg.graph.statements.expressions.SubscriptExpression
import de.fraunhofer.aisec.cpg.passes.concepts.config.ini.IniFileConfigurationPass
import de.fraunhofer.aisec.cpg.passes.concepts.config.python.PythonStdLibConfigurationPass
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

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

        val conf = result.conceptNodes.filterIsInstance<Configuration>().singleOrNull()
        assertNotNull(conf, "There should be a single configuration node")

        val loadConfig = result.operationNodes.filterIsInstance<LoadConfiguration>().singleOrNull()
        assertNotNull(loadConfig)
        assertSame(conf, loadConfig.concept)

        val groups = result.conceptNodes.filterIsInstance<ConfigurationGroup>()
        assertEquals(2, groups.size)

        val defaultGroup = groups["DEFAULT"]
        assertNotNull(defaultGroup)

        val sslGroup = groups["ssl"]
        assertNotNull(sslGroup)

        val readGroupOps = result.operationNodes.filterIsInstance<ReadConfigurationGroup>()
        assertEquals(
            mapOf("DEFAULT" to defaultGroup, "ssl" to sslGroup),
            readGroupOps.associate { Pair(it.name.toString(), it.group) },
        )

        val registerGroupOps = result.operationNodes.filterIsInstance<RegisterConfigurationGroup>()
        assertEquals(
            mapOf("DEFAULT" to defaultGroup, "ssl" to sslGroup),
            registerGroupOps.associate { Pair(it.name.toString(), it.group) },
        )

        val options = result.conceptNodes.filterIsInstance<ConfigurationOption>()
        assertEquals(2, options.size)

        val portOption = options["DEFAULT.port"]
        assertNotNull(portOption)

        val sslEnabledOption = options["ssl.enabled"]
        assertNotNull(sslEnabledOption)

        val readOptionOps = result.operationNodes.filterIsInstance<ReadConfigurationOption>()
        assertEquals(
            mapOf("DEFAULT.port" to portOption, "ssl.enabled" to sslEnabledOption),
            readOptionOps.associate { Pair(it.name.toString(), it.option) },
        )

        val registerOptionOps =
            result.operationNodes.filterIsInstance<RegisterConfigurationOption>()
        assertEquals(
            mapOf("DEFAULT.port" to portOption, "ssl.enabled" to sslEnabledOption),
            registerOptionOps.associate { Pair(it.name.toString(), it.option) },
        )

        val subs = result.allChildren<SubscriptExpression>()
        assertEquals(6, subs.size)

        val port = result.refs["port"]
        assertNotNull(port)
        assertEquals(setOf("80", "8080"), port.evaluate(MultiValueEvaluator()))

        val sslEnabled = result.refs["ssl_enabled"]
        assertNotNull(sslEnabled)
        assertEquals(setOf("true", "false"), sslEnabled.evaluate(MultiValueEvaluator()))
    }
}
