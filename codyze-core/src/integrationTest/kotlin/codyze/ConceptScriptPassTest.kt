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
package codyze

import de.fraunhofer.aisec.codyze.ConceptScriptPass
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.analyze
import kotlin.io.path.Path
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Ignore
class ConceptScriptPassTest {

    @Test
    fun testConceptScriptPass() {
        val topLevel = Path("src/integrationTest/resources")
        val result =
            analyze(listOf(topLevel.resolve("encrypt.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<ConceptScriptPass>()
                it.configurePass<ConceptScriptPass>(
                    ConceptScriptPass.Configuration(
                        scriptFile = topLevel.resolve("encryption.concept.kts").toFile()
                    )
                )
            }
        assertNotNull(result)

        val encrypt = result.calls("encrypt")
        encrypt.forEach { assertTrue(it.operationNodes.isNotEmpty()) }
    }
}
