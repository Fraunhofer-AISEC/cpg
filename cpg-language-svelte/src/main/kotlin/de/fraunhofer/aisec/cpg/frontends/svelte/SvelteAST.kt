package de.fraunhofer.aisec.cpg.frontends.svelte

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Base interface for Svelte AST nodes, containing common properties.
 */
interface SvelteNode {
    val type: String
    val start: Int
    val end: Int
}

/**
 * Represents the root of a parsed Svelte component.
 */
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore fields not explicitly defined
data class Root(
    override val type: String = "Root",
    override val start: Int,
    override val end: Int,
    val options: SvelteOptions?,
    val fragment: Fragment,
    val css: Style? = null, // Renamed from CSS.StyleSheet for clarity, map later if needed
    val instance: Script? = null,
    val module: Script? = null
) : SvelteNode

/**
 * Represents compile options potentially defined within <svelte:options>.
 * Add properties as needed based on documentation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteOptions(
    val start: Int,
    val end: Int,
    val runes: Boolean? = null,
    val immutable: Boolean? = null,
    val accessors: Boolean? = null,
    val preserveWhitespace: Boolean? = null,
    val namespace: String? = null,
    val css: String? = null, // 'injected'
    // Add other options like customElement if needed
    // val attributes: List<Attribute>? = null // Requires Attribute definition
)

/**
 * Represents a <script> tag (either instance or module).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Script(
    override val type: String = "Script", // Should match AST type if different
    override val start: Int,
    override val end: Int,
    val context: String, // 'default' for instance, 'module' for module script
    val content: String, // The raw JS/TS code content
    // Add attributes if needed: val attributes: List<Attribute>? = null
) : SvelteNode

/**
 * Represents the main template structure.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Fragment(
    override val type: String = "Fragment",
    override val start: Int, // Note: Fragment itself might not have start/end in Svelte 5 AST, adjust if needed
    override val end: Int,
    @JsonProperty("nodes") // Explicitly map 'nodes' property
    val children: List<SvelteNode> // List of child nodes (Text, Element, ExpressionTag, etc.)
) : SvelteNode

/**
 * Represents a <style> tag.
 * Define CSS node types more specifically if needed.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Style(
    override val type: String = "Style", // Assuming type is 'Style' or similar
    override val start: Int,
    override val end: Int,
    val content: StyleContent // Contains the raw CSS string
    // Add attributes if needed: val attributes: List<Attribute>? = null
) : SvelteNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class StyleContent(
    val start: Int,
    val end: Int,
    val styles: String // Raw CSS content
)

/**
 * Represents static text content in the template.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Text(
    override val type: String = "Text",
    override val start: Int,
    override val end: Int,
    val data: String, // Decoded text
    val raw: String // Raw text with entities
) : SvelteNode

/**
 * Represents a template expression like {expression}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ExpressionTag(
    override val type: String = "ExpressionTag",
    override val start: Int,
    override val end: Int,
    val expression: ExpressionNode // Represents the JS/TS expression inside
) : SvelteNode

/**
 * Represents a comment in the template.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Comment(
    override val type: String = "Comment",
    override val start: Int,
    override val end: Int,
    val data: String // Comment content
) : SvelteNode

// --- Placeholder for more complex types needed later --- 

/** Placeholder for different types of expressions within tags or scripts */
@JsonIgnoreProperties(ignoreUnknown = true)
// Add specific expression types later, e.g., Identifier, Literal, BinaryExpression
interface ExpressionNode : SvelteNode

/** Represents an HTML element or Svelte Component in the template */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Element(
    override val type: String = "Element", // Or Component, SvelteElement, etc. - Check AST output
    override val start: Int,
    override val end: Int,
    val name: String, // Tag name (e.g., "h1", "div", "MyComponent")
    val attributes: List<Attribute>, // List of attributes/directives
    @JsonProperty("nodes") // Map 'nodes' for children
    val children: List<SvelteNode> // Child nodes
) : SvelteNode // ElementLike removed as Element is now concrete

/** Placeholder for Blocks like #if, #each */
@JsonIgnoreProperties(ignoreUnknown = true)
interface Block : SvelteNode

/** Represents an attribute or directive on an element */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Attribute(
    override val type: String = "Attribute", // Or specific types like SpreadAttribute, Binding, EventHandler
    override val start: Int,
    override val end: Int,
    val name: String, // Attribute name
    val value: List<SvelteNode>, // Attribute value can be complex (Text, ExpressionTag)
    // Add other fields like 'modifiers' for directives if needed
) : SvelteNode

// --- Type resolution for polymorphism --- 

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true // Make 'type' property accessible
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Text::class, name = "Text"),
    JsonSubTypes.Type(value = ExpressionTag::class, name = "ExpressionTag"),
    JsonSubTypes.Type(value = Comment::class, name = "Comment"),
    // Add Element type
    JsonSubTypes.Type(value = Element::class, name = "Element"),
    // JsonSubTypes.Type(value = Element::class, name = "Component"), // Add alias if type name varies
    // JsonSubTypes.Type(value = Element::class, name = "SvelteElement"), // Add alias if type name varies
    JsonSubTypes.Type(value = Fragment::class, name = "Fragment"),
    // Add Attribute (might not be needed directly in Fragment's children list, but good to have)
    JsonSubTypes.Type(value = Attribute::class, name = "Attribute")
    // Add Block subtypes, etc.
)
interface SvelteNodeMixin // Mixin interface for Jackson annotations

// We need to tell Jackson to apply the Mixin annotations to our base interface
// This is usually done when configuring the ObjectMapper, but we can try applying 
// it broadly here for simplicity, though less standard.
// Alternatively, configure mapper in SvelteLanguageFrontend.kt
// @com.fasterxml.jackson.databind.annotation.JsonDeserialize(as = SvelteNodeMixin::class)
// interface SvelteNode // Add annotation here? Requires testing. 