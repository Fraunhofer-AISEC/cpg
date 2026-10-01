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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgIdPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.CpgListPayload
import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.toPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ToolPayloadTest {

    @Test
    fun missingArgumentsFallBackToDefaultsWhenEveryFieldIsOptional() {
        assertEquals(CpgListPayload(), (null as JsonObject?).toPayload<CpgListPayload>())
        assertEquals(CpgListPayload(), JsonObject(emptyMap()).toPayload<CpgListPayload>())
    }

    @Test
    fun missingArgumentsNameTheRequiredFieldTheyLack() {
        val failure =
            assertFailsWith<SerializationException> {
                (null as JsonObject?).toPayload<CpgIdPayload>()
            }

        assertTrue("id" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun givenArgumentsAreDecoded() {
        val payload = JsonObject(mapOf("id" to JsonPrimitive("42"))).toPayload<CpgIdPayload>()

        assertEquals(CpgIdPayload("42"), payload)
    }
}
