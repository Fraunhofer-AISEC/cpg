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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.*

/**
 * A deferred call runs on every return of its function, so the EOG has to connect it to each return
 * reachable from the defer.
 *
 * The number of *paths* from a defer to those returns grows exponentially with the number of
 * branches in between, while the number of returns stays linear. Enumerating the paths therefore
 * makes an ordinary function -- a defer followed by a handful of conditionals -- take effectively
 * forever, which is what stalled several projects of the corpus inside the EOG pass.
 */
class DeferReachableReturnsTest {

    @Test
    fun testDeferConnectsToEveryReachableReturn() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val result =
            analyze(listOf(topLevel.resolve("defer_many_returns.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(result)

        val work = result.functions["work"]
        assertNotNull(work, "work should be parsed")

        val defer = work.allChildren<UnaryOperator> { it.name.localName == "defer" }.firstOrNull()
        assertNotNull(defer, "the defer statement should be parsed")

        val returns = work.allChildren<Return>()
        assertTrue(
            returns.size >= 7,
            "the fixture should have at least 7 returns, got ${returns.size}",
        )

        // The deferred call runs after each return, so each return precedes it in the EOG.
        val connected = defer.input.prevEOG.filterIsInstance<Return>().toSet()
        assertEquals(
            returns.toSet(),
            connected,
            "the deferred call must be connected to every reachable return",
        )
    }
}
