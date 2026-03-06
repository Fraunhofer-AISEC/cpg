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
package de.fraunhofer.aisec.cpg.mcp.prompts

import de.fraunhofer.aisec.cpg.mcp.utils.withMcpServer
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequest
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CpgSuggestConceptsPromptTest {

    @Test
    fun suggestConceptsNoDescription() = withMcpServer { _, client ->
        val result =
            client.getPrompt(GetPromptRequest(GetPromptRequestParams(name = "suggest_concepts")))

        assertNotNull(result)
        val text = (result.messages.firstOrNull()?.content as? TextContent)?.text
        assertNotNull(text, "Prompt message should have text content")
        assertFalse(
            "## Additional Context" in text,
            "Result should not contain 'Additional Context' when no description is given",
        )
    }

    @Test
    fun suggestConceptsWithDescription() = withMcpServer { _, client ->
        val result =
            client.getPrompt(
                GetPromptRequest(
                    GetPromptRequestParams(
                        name = "suggest_concepts",
                        arguments = mapOf("description" to "We have some additional context here."),
                    )
                )
            )

        assertNotNull(result)
        val text = (result.messages.firstOrNull()?.content as? TextContent)?.text
        assertNotNull(text, "Prompt message should have text content")
        assertTrue(
            "## Additional Context" in text,
            "Result should contain 'Additional Context' when description is provided",
        )
    }
}
