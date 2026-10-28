/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.ai.clients

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SystemPromptTest {

    @Test
    fun aFailedToolCallWithABadArgumentMayBeCorrectedAndRetriedAFewTimes() {
        assertTrue("correct that argument and call the tool again" in SYSTEM_PROMPT)
        assertTrue("at most twice" in SYSTEM_PROMPT)
        assertTrue("never with identical arguments" in SYSTEM_PROMPT)
    }

    @Test
    fun otherFailuresAreNotRepeated() {
        assertTrue("do not repeat the call" in SYSTEM_PROMPT)
    }

    @Test
    fun theBlanketNoRetryRuleIsGone() {
        assertFalse("do not retry it" in SYSTEM_PROMPT)
    }
}
