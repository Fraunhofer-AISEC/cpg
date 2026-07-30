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

import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.project.Architecture
import de.fraunhofer.aisec.cpg.project.OperatingSystem
import de.fraunhofer.aisec.cpg.project.Project
import de.fraunhofer.aisec.cpg.test.assertInvokes
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class IntegrationTest {

    @Test
    fun testProject() {
        val project =
            Project.from(Path("src/test/resources/golang/integration")) {
                registerLanguage<GoLanguage>()
                environment {
                    os = OperatingSystem.MACOS
                    architecture = Architecture.ARM64
                }
                detector(GoBuildDetector())
            }

        // The component is named after the Go module and its sources are resolved (and
        // pre-filtered by build constraints) using the Go toolchain
        val app = project.components.singleOrNull()
        assertNotNull(app)
        assertEquals("integration", app.name)

        val sources = app.sources.map { it.toString() }
        assertNotNull(sources.firstOrNull { it.endsWith("main.go") })
        assertNotNull(sources.firstOrNull { it.endsWith("func_darwin.go") })
        assertNotNull(sources.firstOrNull { it.endsWith("func_darwin_arm64.go") })
        assertNull(sources.firstOrNull { it.endsWith("func_darwin_ios.go") })
        assertNull(sources.firstOrNull { it.endsWith("func_linux_arm64.go") })
        assertNotNull(sources.firstOrNull { it.endsWith("fmt/print.go") })

        // GOOS/GOARCH are derived from the target environment
        assertEquals("darwin", project.config.symbols["GOOS"])
        assertEquals("arm64", project.config.symbols["GOARCH"])

        val result = project.analyze()
        val tus = result.components.flatMap { it.translationUnits }

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
