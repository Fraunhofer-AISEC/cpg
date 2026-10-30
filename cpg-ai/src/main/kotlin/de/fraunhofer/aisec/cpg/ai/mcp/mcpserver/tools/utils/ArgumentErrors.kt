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
@file:OptIn(ExperimentalSerializationApi::class)

package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val MAX_PROBLEMS = 20
private const val MAX_SENT_CHARS = 120
private const val TOP_LEVEL = "(arguments)"

private val CAUSE_PATH = Regex("""at path: \$\.?(\S*)""")
private val QUALIFIED_SERIAL_NAME = Regex("""'(?:[a-z_][\w$]*\.)+([A-Z][\w$]*)'""")

/** One thing wrong with the arguments. [owner] is the object whose expected shape explains it. */
private data class Problem(val path: String, val message: String, val owner: String)

/**
 * The text returned to the model when a tool's [arguments] do not decode into the type described by
 * [descriptor].
 *
 * kotlinx.serialization stops at the first problem, which makes a model fix one field per call.
 * This walks the whole arguments against [descriptor] - the decoder's own view, so fields with a
 * default count as optional - and lists every missing field, wrong JSON type and illegal null, plus
 * the expected shape of each object that has a problem. [cause] (the decoder's error) is appended
 * if the walk did not report its path, so nothing it found gets lost; maps, enums and polymorphic
 * types are not walked and are only covered that way.
 *
 * Keeps the "Invalid arguments for <tool>:" prefix that the system prompt's retry rule and log
 * analysis rely on.
 */
fun describeInvalidArguments(
    tool: String,
    descriptor: SerialDescriptor,
    arguments: JsonObject?,
    cause: SerializationException,
): String {
    val problems = mutableListOf<Problem>()
    check(descriptor, arguments ?: JsonObject(emptyMap()), "", "", problems)

    val causeMessage = cleanCauseMessage(cause)
    val causePath = cause.message?.let { CAUSE_PATH.find(it)?.groupValues?.get(1) }
    if (problems.isEmpty()) {
        return "Invalid arguments for $tool: $causeMessage"
    }
    val lines = mutableListOf<String>()
    lines +=
        "Invalid arguments for $tool: ${problems.size} problem(s) - fix all of them, then call again."
    problems.forEach { lines += "- ${display(it.path)}: ${it.message}" }
    if (problems.size >= MAX_PROBLEMS) {
        lines += "- (further problems not listed)"
    }
    if (causePath != null && problems.none { it.path == causePath }) {
        lines += "- ${display(causePath)}: $causeMessage"
    }

    val shapes = linkedMapOf<String, SerialDescriptor>()
    problems.forEach { p ->
        descriptorAt(descriptor, p.owner)?.let { shapes.putIfAbsent(generalize(p.owner), it) }
    }
    if (shapes.isNotEmpty()) {
        lines += "Expected shape:"
        shapes.forEach { (path, d) -> lines += "- ${display(path)}: ${shape(d)}" }
    }
    return lines.joinToString("\n")
}

private fun check(
    d: SerialDescriptor,
    value: JsonElement,
    path: String,
    owner: String,
    problems: MutableList<Problem>,
) {
    if (problems.size >= MAX_PROBLEMS) return
    if (value is JsonNull) {
        if (!d.isNullable)
            problems += Problem(path, "must not be null, expected ${typeName(d)}", owner)
        return
    }
    fun mismatch() {
        problems += Problem(path, "expected ${typeName(d)}, got ${describe(value)}", owner)
    }
    when (d.kind) {
        StructureKind.CLASS,
        StructureKind.OBJECT -> {
            if (value !is JsonObject) return mismatch()
            for (i in 0 until d.elementsCount) {
                val name = d.getElementName(i)
                val element = d.getElementDescriptor(i)
                val field = value[name]
                if (field == null) {
                    if (!d.isElementOptional(i)) {
                        problems +=
                            Problem(
                                path,
                                "missing required field \"$name\" (${typeName(element)})",
                                path,
                            )
                    }
                } else {
                    check(element, field, join(path, name), path, problems)
                }
            }
        }
        StructureKind.LIST -> {
            if (value !is JsonArray) return mismatch()
            val item = d.getElementDescriptor(0)
            value.forEachIndexed { i, e -> check(item, e, "$path[$i]", owner, problems) }
        }
        PrimitiveKind.STRING,
        PrimitiveKind.CHAR -> if (value !is JsonPrimitive || !value.isString) mismatch()
        PrimitiveKind.INT,
        PrimitiveKind.LONG,
        PrimitiveKind.SHORT,
        PrimitiveKind.BYTE ->
            if (value !is JsonPrimitive || value.content.toLongOrNull() == null) mismatch()
        PrimitiveKind.FLOAT,
        PrimitiveKind.DOUBLE ->
            if (value !is JsonPrimitive || value.content.toDoubleOrNull() == null) mismatch()
        PrimitiveKind.BOOLEAN ->
            if (value !is JsonPrimitive || value.content.toBooleanStrictOrNull() == null) mismatch()
        else -> Unit // maps, enums, polymorphic: left to the decoder's message
    }
}

/** The class descriptor reached by following [path] (indices ignored) from [root]. */
private fun descriptorAt(root: SerialDescriptor, path: String): SerialDescriptor? {
    var d = root
    for (segment in generalize(path).split('.').filter { it.isNotEmpty() }) {
        val name = segment.removeSuffix("[]")
        val index =
            (0 until d.elementsCount).firstOrNull { d.getElementName(it) == name } ?: return null
        d = d.getElementDescriptor(index)
        while (d.kind == StructureKind.LIST) d = d.getElementDescriptor(0)
    }
    return d.takeIf { it.kind == StructureKind.CLASS || it.kind == StructureKind.OBJECT }
}

private fun typeName(d: SerialDescriptor): String =
    when (d.kind) {
        StructureKind.CLASS,
        StructureKind.OBJECT -> "object"
        StructureKind.LIST -> "array of ${typeName(d.getElementDescriptor(0))}s"
        StructureKind.MAP -> "object"
        PrimitiveKind.STRING,
        PrimitiveKind.CHAR -> "string"
        PrimitiveKind.INT,
        PrimitiveKind.LONG,
        PrimitiveKind.SHORT,
        PrimitiveKind.BYTE -> "integer"
        PrimitiveKind.FLOAT,
        PrimitiveKind.DOUBLE -> "number"
        PrimitiveKind.BOOLEAN -> "boolean"
        else -> "value"
    }

private fun shape(d: SerialDescriptor): String =
    (0 until d.elementsCount).joinToString(", ", "{", "}") { i ->
        val optional = if (d.isElementOptional(i)) " (optional)" else ""
        "\"${d.getElementName(i)}\": ${typeName(d.getElementDescriptor(i))}$optional"
    }

private fun describe(value: JsonElement): String =
    when (value) {
        is JsonObject -> "an object"
        is JsonArray -> "an array"
        is JsonPrimitive ->
            if (value.isString) "a string: ${truncate(value.toString())}"
            else
                "a ${if (value.content.toBooleanStrictOrNull() != null) "boolean" else "number"}: ${value.content}"
    }

private fun cleanCauseMessage(cause: SerializationException): String =
    (cause.message ?: cause::class.simpleName.orEmpty())
        .substringBefore("\nJSON input:")
        .replace(QUALIFIED_SERIAL_NAME, "'$1'")
        .trim()

private fun truncate(s: String) =
    if (s.length <= MAX_SENT_CHARS) s else s.take(MAX_SENT_CHARS) + "..."

private fun join(path: String, name: String) = if (path.isEmpty()) name else "$path.$name"

private fun generalize(path: String) = path.replace(Regex("""\[\d+]"""), "[]")

private fun display(path: String) = path.ifEmpty { TOP_LEVEL }
