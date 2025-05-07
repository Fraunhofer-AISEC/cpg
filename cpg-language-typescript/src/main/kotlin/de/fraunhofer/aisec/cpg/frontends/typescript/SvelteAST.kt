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
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore properties we haven't explicitly defined
interface SvelteNode {
    val type: String
    val start: Int?
    val end: Int?
}

/** Represents the root of a parsed Svelte component. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteProgram(
    override val type: String =
        "Root", // svelte.parse() doesn't give a type for the root, make one up
    val html:
        SvelteElement, // Or a list of top-level SvelteNodes like SvelteElement, SvelteText etc.
    // Assuming svelte.parse() returns a structure like this.
    // Adjust based on actual svelte.parse() output.
    val css: SvelteStyleNode? = null, // Assuming a potential CSS node
    val instance: SvelteScript? = null,
    val module: SvelteScript? = null,
    override val start: Int?, // Typically 0 for the root
    override val end: Int?, // Typically the length of the file content
) : SvelteNode

/** Represents an HTML element node in the template. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteElement(
    override val type: String, // e.g., "Element", "Text", "MustacheTag"
    val name: String? = null, // For Element, e.g., "div", "p"
    val data: String? = null, // For Text node
    val children: List<SvelteNode>? = listOf(),
    val attributes: List<SvelteAttributeOrBinding>? = listOf(),
    override val start: Int?,
    override val end: Int?,
    // Other properties depending on the type...
) : SvelteNode

/** Represents a plain text node in the template. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteText(
    override val type: String, // "Text"
    val data: String,
    override val start: Int?,
    override val end: Int?,
) : SvelteNode

/** Represents a Mustache tag (e.g., `{expression}`). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteMustacheTag(
    override val type: String, // "MustacheTag"
    val expression: EsTreeNode, // The expression inside { }
    override val start: Int?,
    override val end: Int?,
) : SvelteNode

/** Represents a `<script>` block. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteScript(
    override val type: String, // "Script"
    val context: String?, // "default", "module"
    @JsonProperty("content") val ast: EsTreeProgram, // Changed from content: String
    override val start: Int?,
    override val end: Int?,
    val attributes: List<SvelteAttributeOrBinding>? = listOf(),
) : SvelteNode

/** Represents a `<style>` block. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteStyleNode( // Placeholder for <style> blocks
    override val type: String, // "Style"
    val attributes: List<SvelteAttributeOrBinding>? = listOf(),
    val children: List<SvelteNode>? = listOf(), // Style content, typically SvelteText with CSS
    val content: SvelteStyleContent,
    override val start: Int?,
    override val end: Int?,
) : SvelteNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteStyleContent(
    val styles: String, // Raw CSS string
    val start: Int?,
    val end: Int?,
    // Potentially add more structure if svelte.parse provides it (e.g., parsed CSS AST)
)

// ESTree specific nodes for JavaScript/TypeScript within <script> tags
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type", // This is the discriminator field in JSON
)
@JsonSubTypes(
    JsonSubTypes.Type(value = EsTreeProgram::class, name = "Program"),
    JsonSubTypes.Type(value = EsTreeVariableDeclaration::class, name = "VariableDeclaration"),
    JsonSubTypes.Type(value = EsTreeVariableDeclarator::class, name = "VariableDeclarator"),
    JsonSubTypes.Type(value = EsTreeIdentifier::class, name = "Identifier"),
    JsonSubTypes.Type(value = EsTreeLiteral::class, name = "Literal"),
    // Add other EsTreeNode types here as they are defined, e.g., ExpressionStatement
)
@JsonIgnoreProperties(ignoreUnknown = true)
interface EsTreeNode {
    // The 'type' property is implicitly handled by Jackson due to @JsonTypeInfo
    val start: Int?
    val end: Int?
}

interface EsTreeStatement : EsTreeNode

interface EsTreeExpression : EsTreeNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeProgram(
    val type: String, // Should be "Program"
    val body: List<EsTreeNode>,
    val sourceType: String, // "script" or "module"
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeVariableDeclaration(
    val type: String, // Should be "VariableDeclaration"
    val declarations: List<EsTreeVariableDeclarator>,
    val kind: String, // "var", "let", or "const"
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeStatement

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeVariableDeclarator(
    val type: String, // Should be "VariableDeclarator"
    val id: EsTreeIdentifier,
    val init: EsTreeNode?, // Actually an EsTreeExpression
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeIdentifier(
    val type: String, // Should be "Identifier"
    val name: String,
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeLiteral(
    val type: String, // Should be "Literal"
    val value: Any?,
    val raw: String,
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

// --- Placeholder Interfaces/Classes for nested structures ---

/** Base for attributes and directives. */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = SvelteAttribute::class, name = "Attribute"),
    JsonSubTypes.Type(value = SvelteEventHandler::class, name = "EventHandler"),
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
    val value: List<SvelteNode>? = null, // Attribute value can be complex
) : SvelteAttributeLike

/** Represents an `on:` directive (event handler). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteEventHandler(
    override val type: String, // Should be "EventHandler"
    override val start: Int,
    override val end: Int,
    val name: String, // The event name (e.g., "click")
    val expression: SvelteExpression? = null, // The handler expression/function
    // Modifiers might be here too
) : SvelteAttributeLike

/** Placeholder for different types of expressions within Svelte templates or scripts. */
@JsonIgnoreProperties(ignoreUnknown = true) interface SvelteExpression : SvelteNode

// We can add concrete expression types later (Identifier, Literal, BinaryExpression, CallExpression
// etc.)
// matching the structure produced by Svelte/ESTree within the AST.

@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteAttributeOrBinding(
    override val type: String, // e.g., "Attribute", "Binding"
    val name: String,
    // Attributes can have complex values (Text, MustacheTag, etc.)
    // For simplicity, assuming value is a list of SvelteNodes or simple string for now.
    // This might need to be List<SvelteNode> if attributes can contain tags.
    val value: Any? =
        null, // Could be Boolean for shorthand, or List<SvelteNode> for complex values
    override val start: Int?,
    override val end: Int?,
) : SvelteNode
