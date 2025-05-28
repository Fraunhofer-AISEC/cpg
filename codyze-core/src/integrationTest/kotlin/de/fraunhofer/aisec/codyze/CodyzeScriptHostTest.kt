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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.Secret
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.query.Failed
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.query.allExtended
import de.fraunhofer.aisec.cpg.query.eq
import de.fraunhofer.aisec.cpg.test.assertInvokes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

fun goodCryptoFunc(result: TranslationResult): QueryTree<Boolean> {
    return result.allExtended<CallExpression> { it.name eq "encrypt" }
}

fun goodArgumentSize(result: TranslationResult): QueryTree<Boolean> {
    return result.allExtended<CallExpression> { it.arguments.size eq 2 }
}

class CodyzeExecutorTest {
    @Test
    fun testEvaluate() {
        val project =
            AnalysisProject.fromScript("src/integrationTest/resources/example/project.codyze.kts")
        assertNotNull(project)

        val result = project.analyze()
        assertNotNull(result)

        val foo = result.translationResult.functions["foo"]
        assertNotNull(foo)

        val encrypt = result.translationResult.functions["encrypt"]
        assertNotNull(encrypt)

        val myFunc = result.translationResult.functions["my_func"]
        assertNotNull(myFunc)
        assertFalse(myFunc.isInferred)

        assertEquals(2, result.requirementsResults.size)
        assertEquals(result.requirementsResults["RQ-ENCRYPTION-01"]?.value, Failed)

        val myFuncCall = result.translationResult.calls["my_func"]
        assertNotNull(myFuncCall)
        assertInvokes(myFuncCall, myFunc)

        val getSecretCall = result.translationResult.calls["get_secret_from_server"]
        assertNotNull(getSecretCall)

        val secret = getSecretCall.conceptNodes.filterIsInstance<Secret>().singleOrNull()
        assertNotNull(secret)
    }
}
