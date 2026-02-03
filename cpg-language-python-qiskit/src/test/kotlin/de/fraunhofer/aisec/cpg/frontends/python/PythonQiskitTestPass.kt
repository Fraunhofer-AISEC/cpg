/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass
import de.fraunhofer.aisec.cpg.passes.QiskitPass
import de.fraunhofer.aisec.cpg.passes.quantumcpg.QuantumEOGPass
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import java.nio.file.Path
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

class PythonQiskitTestPass : BaseTest() {
    // TODO ensure gradle doesn't remove those classes
    private val dummyRegion = Region()
    private val dummyPhysicalLocation = PhysicalLocation(URI(""), dummyRegion)

    @Test
    fun testIntro1() {
        val topLevel = Path.of("src", "test", "resources", "python", "qiskit")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("intro_tutorial_1.py").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<QuantumEOGPass>()
                it.registerPass<QiskitPass>()
            }

        assertNotNull(tu)
    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources", "python", "qiskit")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("if.py").toFile()),
                topLevel,
                true,
            ) {
                it.registerPass<EdgeCachePass>()
                it.registerLanguage<PythonLanguage>()
                it.registerPass<QiskitPass>()
                it.registerPass<QuantumEOGPass>()
            }

        assertNotNull(tu)
    }
}
