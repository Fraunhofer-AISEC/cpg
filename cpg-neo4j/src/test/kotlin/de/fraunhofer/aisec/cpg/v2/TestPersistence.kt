/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.v2

import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg_vis_neo4j.createTranslationResult
import java.nio.file.Path
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integration")
class TestPersistence {
    @Test
    fun testPersist() {
        val result = createTranslationResult()

        neo4jSession.executeWrite { tx -> tx.run("MATCH (n) DETACH DELETE n").consume() }
        result.second.persist()
    }

    @Test
    fun testPersistGlance() {
        val topLevel =
            Path.of("/Users/chr55316/Repositories/openstack-checker/targets/projects/glance")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("glance").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
                it.exclusionPatterns("tests")
                it.useParallelFrontends(false)
                it.failOnError(false)
            }

        val bench = Benchmark(this.javaClass, "Persist")
        neo4jSession.executeWrite { tx -> tx.run("MATCH (n) DETACH DELETE n").consume() }
        result.persist()
        bench.stop()
    }
}
