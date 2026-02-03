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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.test.analyzeWithBuilder
import de.fraunhofer.aisec.cpg.test.assertInvokes
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class IntegrationTest {

    @Test
    fun testProject() {
        val project =
            Project.Companion.buildProject(
                "src/test/resources/golang/integration",
                "darwin",
                "arm64",
            )

        val app = project.components[TranslationResult.Companion.DEFAULT_APPLICATION_NAME]
        assertNotNull(app)
        assertNotNull(app.firstOrNull { it.endsWith("main.go") })
        assertNotNull(app.firstOrNull { it.endsWith("func_darwin.go") })
        assertNotNull(app.firstOrNull { it.endsWith("func_darwin_arm64.go") })
        assertNull(app.firstOrNull { it.endsWith("func_darwin_ios.go") })
        assertNull(app.firstOrNull { it.endsWith("func_linux_arm64.go") })
        assertNotNull(app.firstOrNull { it.endsWith("fmt/print.go") })

        val tus =
            analyzeWithBuilder(
                TranslationConfiguration.Companion.builder()
                    .softwareComponents(project.components)
                    .symbols(project.symbols)
                    .includePath(project.includePaths.first().path)
                    .registerLanguage<GoLanguage>()
                    .defaultPasses()
            )
        assertNotNull(tus)

        val printTU = tus.firstOrNull { it.name.endsWith("fmt/print.go") }
        assertNotNull(printTU)

        val printf = printTU.functions["Printf"]
        assertNotNull(printf)

        val mainTU = tus.firstOrNull { it.name.endsWith("main.go") }
        assertNotNull(mainTU)

        val printfCall = mainTU.calls["fmt.Printf"]
        assertNotNull(printfCall)
        assertInvokes(printfCall, printf)
    }
}
