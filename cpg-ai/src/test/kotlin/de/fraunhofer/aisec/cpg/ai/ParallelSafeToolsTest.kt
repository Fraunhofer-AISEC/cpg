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
package de.fraunhofer.aisec.cpg.ai

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the invariant behind [ChatService.parallelSafeToolNames]: only read-only tools run in
 * parallel.
 */
class ParallelSafeToolsTest {

    @Test
    fun noGraphOrFileMutatingToolIsRunInParallel() {
        val mutating =
            listOf(
                "cpg_analyze",
                "cpg_translate",
                "cpg_run_pass",
                "cpg_apply_concepts",
                "cpg_add_llm_concept_and_operations",
                "cpg_add_or_update_llm_concept",
            )

        mutating.forEach {
            assertFalse(it in ChatService.parallelSafeToolNames, "$it mutates shared state")
        }
    }

    @Test
    fun readOnlyQueryToolsAreRunInParallel() {
        listOf(
                "cpg_list_concepts_and_operations",
                "cpg_get_last_write",
                "cpg_suggest_llm_concepts_and_operations",
            )
            .forEach { assertTrue(it in ChatService.parallelSafeToolNames, it) }
    }
}
