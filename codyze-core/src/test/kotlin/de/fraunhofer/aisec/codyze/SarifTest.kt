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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.query.Failed
import de.fraunhofer.aisec.cpg.query.NotYetEvaluated
import de.fraunhofer.aisec.cpg.query.Succeeded
import de.fraunhofer.aisec.cpg.query.Undecided
import de.fraunhofer.aisec.cpg.query.decide
import de.fraunhofer.aisec.cpg.query.toQueryTree
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SarifTest {

    @Test
    fun testUndecide() {
        with(
            TranslationResult(
                translationManager = TranslationManager.builder().build(),
                finalCtx = TranslationContext(),
            )
        ) {
            val trueQ = true.toQueryTree()
            val falseQ = false.toQueryTree()
            val succeededQ = Succeeded.toQueryTree()
            val failedQ = Failed.toQueryTree()
            val undecidedQ = Undecided.toQueryTree()
            val notYetEvaluatedQ = NotYetEvaluated.toQueryTree()
            val succeededFromTrueQ = trueQ.decide()
            val failedFromFalseQ = falseQ.decide()

            // Let's undecide!
            assertSame(
                trueQ,
                succeededFromTrueQ.toBooleanQueryTree(),
                "Expected decision coming from a boolean query tree to be converted back to itself",
            )
            assertSame(
                falseQ,
                failedFromFalseQ.toBooleanQueryTree(),
                "Expected decision coming from a boolean query tree to be converted back to itself",
            )
            assertTrue(
                succeededQ.toBooleanQueryTree()?.value == true,
                "Expected Succeeded decision to be converted as true",
            )
            assertFalse(
                failedQ.toBooleanQueryTree()?.value == true,
                "Expected Failed decision to be converted as false",
            )
            assertFalse(
                undecidedQ.toBooleanQueryTree()?.value == true,
                "Expected Undecided decision to be converted as false",
            )
            assertNull(
                notYetEvaluatedQ.toBooleanQueryTree(),
                "Expected NotYetEvaluated decision to be converted as null",
            )
        }
    }
}
