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
package de.fraunhofer.aisec.codyze.compliance

import de.fraunhofer.aisec.codyze.AnalysisProject
import de.fraunhofer.aisec.cpg.graph.*
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SarifTest {
    @Test
    fun testSarifFindings() {
        val project =
            AnalysisProject.fromDirectory(
                projectDir = "src/integrationTest/resources/demo-app",
                postProcess = AnalysisProject::buildSarif,
            )
        assertNotNull(project)

        val result = project.analyze()
        val tr = result.translationResult
        val webappMain = tr.namespaces["webapp.main"]
        assertNotNull(webappMain)

        val tmpFile = createTempFile(prefix = "findings", suffix = ".sarif").toFile()
        result.writeSarifJson(tmpFile)

        assertTrue(tmpFile.length() > 0)
        tmpFile.delete()
    }
}
