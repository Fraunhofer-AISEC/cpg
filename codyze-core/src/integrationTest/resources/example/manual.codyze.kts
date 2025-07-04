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
package example

project {
    manualAssessment {
        of("SEC-TARGET") {
            val expectedValue = 2
            val actualValue = 3

            /* Yes, I really checked that 6 is greater than 5 */
            val result = actualValue gt expectedValue
            result.assume(
                AssumptionType.SoundnessAssumption,
                "We assume that mathematical principles are sound",
            )
        }

        of("THIRD-PARTY-LIBRARY") {
            /* Yes, the rumors are true. */
            true
        }

        of("SOMETHING-ELSE") {
            /* Hmm. I am not sure about this one. */
            NotYetEvaluated
        }
    }
}
