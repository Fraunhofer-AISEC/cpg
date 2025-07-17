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
package de.fraunhofer.aisec.codyze

import de.fraunhofer.aisec.cpg.graph.allCalls
import de.fraunhofer.aisec.cpg.graph.allFunctions
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Secret
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.query.*
import de.fraunhofer.aisec.cpg.test.assertInvokes
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class CodyzeExecutorTest {
    @Test
    fun testEvaluate() {
        val project =
            AnalysisProject.fromScript(
                Path("src/integrationTest/resources/example/project.codyze.kts")
            )
        assertNotNull(project)

        val result = project.analyze()
        assertNotNull(result)

        val foo = result.translationResult.allFunctions["foo"]
        assertNotNull(foo)

        val encrypt = result.translationResult.allFunctions["encrypt"]
        assertNotNull(encrypt)

        val specialFunc = result.translationResult.allFunctions["special_func"]
        assertNotNull(specialFunc)
        assertFalse(specialFunc.isInferred)

        assertEquals(3, result.requirementsResults.size)
        assertEquals(false, result.requirementsResults["RQ-ENCRYPTION-001"]?.value)
        assertEquals(UndecidedResult, result.requirementsResults["RQ-ENCRYPTION-001"]?.confidence)
        assertEquals(true, result.requirementsResults["RQ-ENCRYPTION-002"]?.value)
        assertEquals(AcceptedResult, result.requirementsResults["RQ-ENCRYPTION-002"]?.confidence)

        val myFuncCall = result.translationResult.allCalls["special_func"]
        assertNotNull(myFuncCall)
        assertInvokes(myFuncCall, specialFunc)

        val getSecretCall = result.translationResult.allCalls["get_secret_from_server"]
        assertNotNull(getSecretCall)

        val secret = getSecretCall.conceptNodes.filterIsInstance<Secret>().singleOrNull()
        assertNotNull(secret)
    }
}
