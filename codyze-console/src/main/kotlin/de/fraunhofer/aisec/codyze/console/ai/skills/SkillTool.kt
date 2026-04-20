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
package de.fraunhofer.aisec.codyze.console.ai.skills

import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

const val ACTIVATE_SKILL_TOOL_NAME = "activate_skill"

/**
 * Build a synthetic tool the LLM can call to activate a discovered skill. Returns null when no
 * skills are available, i.e. the tool is not registered.
 *
 * The catalog (name + description per skill) lives in the system prompt. This tool only exposes the
 * activation mechanism itself, with an `enum` over the valid skill names so the LLM cannot
 * hallucinate unknown ones.
 *
 * See
 * [Model-driven activation](https://agentskills.io/client-implementation/adding-skills-support#model-driven-activation).
 */
fun buildActivateSkillTool(skills: List<Skill>): Tool? {
    if (skills.isEmpty()) return null

    val properties = buildJsonObject {
        putJsonObject("name") {
            put("type", "string")
            put("description", "The name of the skill to activate.")
            putJsonArray("enum") { skills.forEach { add(JsonPrimitive(it.name)) } }
        }
    }

    return Tool(
        name = ACTIVATE_SKILL_TOOL_NAME,
        description =
            "Activate a skill to load its full instructions. " +
                "Use this when the user's task matches one of the skills listed in the system prompt.",
        inputSchema = ToolSchema(properties = properties, required = listOf("name")),
    )
}

/**
 * Build the skill-catalog section that is appended to the system prompt. See
 * [Behavioral instructions](https://agentskills.io/client-implementation/adding-skills-support#behavioral-instructions)
 */
fun buildSkillCatalog(skills: List<Skill>): String? {
    if (skills.isEmpty()) return null
    val entries = skills.joinToString("\n") { "- ${it.name}: ${it.description}" }
    return """
        The following skills provide specialized instructions for specific tasks.
        When a task matches a skill's description, call the `$ACTIVATE_SKILL_TOOL_NAME`
        tool with the skill's name to load its full instructions.

        Available skills:
        $entries
    """
        .trimIndent()
}

/**
 * Wrap a skill's body so that the LLM can identify skill content in its conversation context.
 *
 * See
 * [Structured wrapping](https://agentskills.io/client-implementation/adding-skills-support#structured-wrapping)
 */
fun wrapActivatedSkill(skill: Skill): String =
    "<skill_content name=\"${skill.name}\">\n${skill.body}\n</skill_content>"
