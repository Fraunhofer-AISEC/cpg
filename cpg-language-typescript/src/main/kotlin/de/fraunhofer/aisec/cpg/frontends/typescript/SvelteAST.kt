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
package de.fraunhofer.aisec.cpg.frontends.typescript

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Base interface for Svelte AST nodes parsed from the svelte parser. We will define concrete data
 * classes inheriting from this later.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type", // The property in JSON that determines the subtype
    visible = true // Make the 'type' property accessible in the Kotlin class
)
@JsonSubTypes(
    // Register known node types here. Add more as needed.
    JsonSubTypes.Type(value = SvelteElement::class, name = "Element"),
    JsonSubTypes.Type(value = SvelteText::class, name = "Text"),
    JsonSubTypes.Type(value = SvelteMustacheTag::class, name = "MustacheTag"),
    JsonSubTypes.Type(value = SvelteScript::class, name = "Script"),
    JsonSubTypes.Type(value = SvelteStyle::class, name = "Style")
    // Add other types like IfBlock, EachBlock, Attribute, Directive, etc.
)
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore properties we haven't explicitly defined
interface SvelteNode {
    val type: String
    val start: Int
    val end: Int
}

/** Represents the root of a parsed Svelte component. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteProgram(
    // Note: The root itself doesn't have a type/start/end from svelte.parse
    val html: SvelteFragment? = null, // Represents the template markup
    val css: SvelteStyle? = null, // Represents the <style> block
    val instance: SvelteScript? = null, // Represents the regular <script> block
    val module: SvelteScript? = null // Represents the <script context="module"> block
)

/** Represents a fragment of the Svelte template (e.g., the content of html). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteFragment(
    override val type: String = "Fragment", // Type might be inferred or fixed
    override val start: Int,
    override val end: Int,
    val children: List<SvelteNode>? = null
) : SvelteNode

/** Represents an HTML element node in the template. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteElement(
    override val type: String, // Should be "Element"
    override val start: Int,
    override val end: Int,
    val name: String, // The tag name (e.g., "h1", "p", "button")
    val attributes: List<SvelteAttributeLike> = emptyList(),
    val children: List<SvelteNode>? = null
) : SvelteNode

/** Represents a plain text node in the template. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteText(
    override val type: String, // Should be "Text"
    override val start: Int,
    override val end: Int,
    val data: String, // The raw text content
    @JsonProperty("raw") // Sometimes the parser uses "raw" instead of "data"
    val rawData: String? = null
) : SvelteNode {
    val text: String
        get() = data ?: rawData ?: ""
}

/** Represents a Mustache tag (e.g., `{expression}`). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteMustacheTag(
    override val type: String, // Should be "MustacheTag"
    override val start: Int,
    override val end: Int,
    val expression: SvelteExpression? = null // Placeholder for expression nodes
) : SvelteNode

/** Represents a `<script>` block. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteScript(
    override val type: String, // Should be "Script"
    override val start: Int,
    override val end: Int,
    val context: String? = null, // e.g., "module"
    val content: String, // Raw script content
    val attributes: List<SvelteAttributeLike> = emptyList()
    // TODO: Potentially add a parsed representation of the script content (e.g., using TS parser)
) : SvelteNode

/** Represents a `<style>` block. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteStyle(
    override val type: String, // Should be "Style"
    override val start: Int,
    override val end: Int,
    val attributes: List<SvelteAttributeLike> = emptyList(),
    val children: List<SvelteNode>? = null, // Style content might be parsed as nodes
    val content: SvelteStyleContent? = null // Contains raw content and rules
) : SvelteNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteStyleContent(
    val start: Int,
    val end: Int,
    val styles: String, // Raw CSS content
    val attributes: List<SvelteAttributeLike> = emptyList()
    // TODO: Potentially add parsed CSS rules if needed
)

// --- Placeholder Interfaces/Classes for nested structures --- 

/** Base for attributes and directives. */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = SvelteAttribute::class, name = "Attribute"),
    JsonSubTypes.Type(value = SvelteEventHandler::class, name = "EventHandler")
    // Add other types like Binding, ClassList, Spread, etc.
)
@JsonIgnoreProperties(ignoreUnknown = true)
interface SvelteAttributeLike : SvelteNode

/** Represents a standard HTML attribute. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteAttribute(
    override val type: String, // Should be "Attribute"
    override val start: Int,
    override val end: Int,
    val name: String,
    val value: List<SvelteNode>? = null // Attribute value can be complex
) : SvelteAttributeLike

/** Represents an `on:` directive (event handler). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteEventHandler(
    override val type: String, // Should be "EventHandler"
    override val start: Int,
    override val end: Int,
    val name: String, // The event name (e.g., "click")
    val expression: SvelteExpression? = null // The handler expression/function
    // Modifiers might be here too
) : SvelteAttributeLike

/** Placeholder for different types of expressions within Svelte templates or scripts. */
@JsonIgnoreProperties(ignoreUnknown = true)
interface SvelteExpression : SvelteNode

// We can add concrete expression types later (Identifier, Literal, BinaryExpression, CallExpression etc.)
// matching the structure produced by Svelte/ESTree within the AST.
