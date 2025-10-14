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
package de.fraunhofer.aisec.cpg.assumptions

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.newLiteral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid

class AssumptionTest {
    @Test
    fun testAssumptionStatus() {
        with(TestLanguageFrontend()) {
            Assumption.states[{ it.id == Uuid.parse("00000000-0000-0000-ffff-ffff9d93bc7d") }] =
                AssumptionStatus.Accepted

            val lit = newLiteral(1).assume(AssumptionType.SoundnessAssumption, "We assume 1 is 1")
            assertEquals(-1650614200, lit.hashCode())
            assertEquals("00000000-0000-0000-ffff-ffff9d9da048", lit.id.toString())

            val assumption = lit.assumptions.firstOrNull()
            assertNotNull(assumption)
            assertEquals("We assume 1 is 1", assumption.message)
            assertEquals(AssumptionType.SoundnessAssumption, assumption.assumptionType)
            assertEquals(null, assumption.edge)
            assertEquals(
                "de.fraunhofer.aisec.cpg.assumptions.Assumption",
                assumption.javaClass.name,
            )
            assertEquals(-1651262339, assumption.hashCode())
            assertEquals("00000000-0000-0000-ffff-ffff9d93bc7d", assumption.id.toString())
            assertEquals(AssumptionStatus.Accepted, assumption.status)
        }
    }
}
