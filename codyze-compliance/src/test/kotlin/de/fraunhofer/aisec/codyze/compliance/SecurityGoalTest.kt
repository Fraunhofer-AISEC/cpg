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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Name
import kotlin.io.path.Path
import kotlin.test.*

class SecurityGoalTest {
    @Test
    fun testLoadSecurityGoals() {
        val goals = loadSecurityGoals(Path("src/test/resources/security-goals"))
        val goal1 = goals.firstOrNull()
        assertNotNull(goal1)
        assertEquals("Goal1", goal1.name.localName)
        assertEquals("Make it very secure", goal1.description)

        val objective1 = goal1.objectives.firstOrNull()
        assertNotNull(objective1)
        assertEquals("Good encryption", objective1.name.localName)
        assertEquals("Encryption used is very good", objective1.description)
    }

    @Test
    fun testLoadSecurityGoal() {
        val stream = this::class.java.getResourceAsStream("/security-goals/goal1.yaml")
        assertNotNull(stream)

        val goal1 = loadSecurityGoal(stream)
        assertNotNull(goal1)
        assertEquals("Goal1", goal1.name.localName)
        assertEquals("Make it very secure", goal1.description)

        val objective1 = goal1.objectives.firstOrNull()
        assertNotNull(objective1)
        assertEquals("Good encryption", objective1.name.localName)
        assertEquals("Encryption used is very good", objective1.description)
    }

    @Test
    fun testLoadSecurityGoalWithResult() {
        val stream = this::class.java.getResourceAsStream("/security-goals/goal1.yaml")
        assertNotNull(stream)

        val result =
            TranslationResult(
                translationManager = TranslationManager.builder().build(),
                TranslationContext(config = TranslationConfiguration.builder().build()),
            )
        val auth = Component(result.finalCtx).also { it.name = Name("auth") }
        result.components += auth
        val webserver = Component(result.finalCtx).also { it.name = Name("webserver") }
        result.components += webserver

        val goal1 = loadSecurityGoal(stream, result)
        assertNotNull(goal1)
        assertEquals(listOf(auth, webserver), goal1.components)

        val objective1 = goal1.objectives.firstOrNull()
        assertNotNull(objective1)
        assertEquals(listOf(auth), objective1.components)
    }
}
