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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.assumptions.AssumptionStatus
import de.fraunhofer.aisec.cpg.assumptions.AssumptionType
import de.fraunhofer.aisec.cpg.assumptions.assume
import java.io.File
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class DecisionTest {

    private val assumptionIDCounter = 1

    fun QueryTree<Boolean>.testAssumption(
        assumptionStatus: AssumptionStatus = AssumptionStatus.Undecided
    ): QueryTree<Boolean> {
        this.assume(AssumptionType.ExceptionsAssumption, "TestAssumption $assumptionIDCounter")
            .also { it.assumptions.map { it.status = assumptionStatus } }
        return this
    }

    private val trueQ = true.toQueryTree()
    private val falseQ = false.toQueryTree()

    private val trueUndecided = true.toQueryTree().testAssumption(AssumptionStatus.Undecided)
    private val falseUndecided = false.toQueryTree().testAssumption(AssumptionStatus.Undecided)

    private val trueAccepted = true.toQueryTree().testAssumption(AssumptionStatus.Accepted)
    private val falseAccepted = false.toQueryTree().testAssumption(AssumptionStatus.Accepted)

    private val trueRejected = true.toQueryTree().testAssumption(AssumptionStatus.Rejected)
    private val falseRejected = false.toQueryTree().testAssumption(AssumptionStatus.Rejected)

    context(TranslationResult)
    private fun assertDecide(decisionState: DecisionState, queryTree: QueryTree<Boolean>) {
        assertEquals(decisionState, queryTree.decide().value)
    }

    @Test
    fun testDecideAnd() {

        val config =
            TranslationConfiguration.builder().topLevels(mapOf("test" to File("test-path"))).build()
        val result =
            TranslationResult(
                translationManager = TranslationManager.builder().config(config).build(),
                finalCtx = TranslationContext(config),
            )

        with(result) {
            assertDecide(Succeeded, trueQ and trueQ)
            assertDecide(Succeeded, trueAccepted and trueQ)

            assertDecide(Undecided, trueUndecided and trueQ)
            assertDecide(Undecided, trueUndecided and trueUndecided)

            assertDecide(Failed, trueRejected and trueQ)
            assertDecide(Failed, falseQ and trueQ)
        }
    }

    @Test
    fun testDecideOr() {

        val config =
            TranslationConfiguration.builder().topLevels(mapOf("test" to File("test-path"))).build()
        val result =
            TranslationResult(
                translationManager = TranslationManager.builder().config(config).build(),
                finalCtx = TranslationContext(config),
            )

        with(result) {
            assertDecide(Succeeded, trueQ or falseQ)
            assertDecide(Succeeded, trueQ or trueUndecided)
            assertDecide(Succeeded, trueQ or trueRejected)
            assertDecide(Succeeded, trueAccepted or falseQ)

            assertDecide(Undecided, trueUndecided or falseUndecided)
            assertDecide(Undecided, trueUndecided or falseAccepted)
            assertDecide(Undecided, trueUndecided or falseRejected)

            assertDecide(Failed, trueRejected or falseQ)
            assertDecide(Failed, falseAccepted or falseQ)
        }
    }

    @Test
    fun testDecideXOR() {

        val config =
            TranslationConfiguration.builder().topLevels(mapOf("test" to File("test-path"))).build()
        val result =
            TranslationResult(
                translationManager = TranslationManager.builder().config(config).build(),
                finalCtx = TranslationContext(config),
            )

        with(result) {
            assertDecide(Succeeded, trueQ xor falseQ)
            assertDecide(Succeeded, trueQ xor trueRejected)
            assertDecide(Succeeded, trueAccepted xor falseQ)
            assertDecide(Succeeded, trueAccepted xor trueRejected)

            assertDecide(Undecided, trueUndecided xor falseQ)
            assertDecide(Undecided, trueUndecided xor trueRejected)
            assertDecide(Undecided, trueUndecided xor trueQ)

            assertDecide(Failed, falseQ xor falseQ)
            assertDecide(Failed, trueQ xor trueQ)

            assertDecide(Failed, trueAccepted xor trueAccepted)
            assertDecide(Failed, trueAccepted xor trueQ)

            assertDecide(Failed, falseAccepted xor falseAccepted)
            assertDecide(Failed, falseAccepted xor falseQ)
        }
    }

    @Test
    fun testDecideImplies() {

        val config =
            TranslationConfiguration.builder().topLevels(mapOf("test" to File("test-path"))).build()
        val result =
            TranslationResult(
                translationManager = TranslationManager.builder().config(config).build(),
                finalCtx = TranslationContext(config),
            )

        with(result) {
            assertDecide(Succeeded, trueQ implies trueQ)
            assertDecide(Succeeded, trueQ implies trueAccepted)
            assertDecide(Succeeded, trueAccepted implies trueQ)
            assertDecide(Succeeded, trueAccepted implies trueAccepted)
            assertDecide(Succeeded, falseQ implies falseQ)
            assertDecide(Succeeded, falseAccepted implies falseQ)
            assertDecide(Succeeded, trueRejected implies falseQ)
            assertDecide(Succeeded, falseUndecided implies trueQ)
            assertDecide(Succeeded, falseUndecided implies falseQ)

            assertDecide(Undecided, trueQ implies trueUndecided)
            assertDecide(Undecided, trueAccepted implies trueUndecided)

            assertDecide(Failed, trueQ implies falseQ)
            assertDecide(Failed, trueQ implies falseAccepted)
            assertDecide(Failed, trueQ implies trueRejected)
            assertDecide(Failed, trueAccepted implies falseQ)
            assertDecide(Failed, trueAccepted implies falseAccepted)
            assertDecide(Failed, trueAccepted implies trueRejected)
        }
    }

    @Test
    fun testDecideNot() {

        val config =
            TranslationConfiguration.builder().topLevels(mapOf("test" to File("test-path"))).build()
        val result =
            TranslationResult(
                translationManager = TranslationManager.builder().config(config).build(),
                finalCtx = TranslationContext(config),
            )

        with(result) {
            assertDecide(Succeeded, not(falseQ))
            assertDecide(Succeeded, not(trueRejected))
            assertDecide(Succeeded, not(falseRejected))
            assertDecide(Succeeded, not(falseUndecided))

            assertDecide(Undecided, not(trueUndecided))

            assertDecide(Failed, not(trueQ))
        }
    }
}
