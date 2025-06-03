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

/** Base interface for all Svelte and ESTree AST nodes, providing common properties. */
@JsonIgnoreProperties(ignoreUnknown = true)
interface GenericAstNode {
    val start: Int?
    val end: Int?
}

/**
 * Base interface for Svelte AST nodes parsed from the svelte parser. We will define concrete data
 * classes inheriting from this later.
 */
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore properties we haven't explicitly defined
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    visible = true, // Make the 'type' property accessible if needed
)
@JsonSubTypes(
    JsonSubTypes.Type(value = SvelteFragment::class, name = "Fragment"),
    JsonSubTypes.Type(value = SvelteElement::class, name = "Element"),
    JsonSubTypes.Type(value = SvelteText::class, name = "Text"),
    JsonSubTypes.Type(value = SvelteMustacheTag::class, name = "MustacheTag"),
    JsonSubTypes.Type(value = SvelteScript::class, name = "Script"),
    JsonSubTypes.Type(value = SvelteStyleNode::class, name = "Style"),
    JsonSubTypes.Type(value = SvelteEventHandler::class, name = "EventHandler"),
    JsonSubTypes.Type(value = SvelteInlineComponent::class, name = "InlineComponent"),
    JsonSubTypes.Type(value = SvelteIfBlock::class, name = "IfBlock"),
    JsonSubTypes.Type(value = SvelteElseBlock::class, name = "ElseBlock"),
    // CSS Nodes
    JsonSubTypes.Type(value = SvelteRule::class, name = "Rule"),
    JsonSubTypes.Type(value = SvelteSelectorList::class, name = "SelectorList"),
    JsonSubTypes.Type(value = SvelteSelector::class, name = "Selector"),
    JsonSubTypes.Type(value = SvelteTypeSelector::class, name = "TypeSelector"),
    JsonSubTypes.Type(value = SvelteBlock::class, name = "Block"),
    JsonSubTypes.Type(value = SvelteDeclaration::class, name = "Declaration"),
    // TODO: Add other Svelte node types here as needed (e.g., Comment, Block, etc.)
)
interface SvelteNode : GenericAstNode {
    // 'type' property handled by Jackson
    // start and end are now inherited from GenericAstNode
}

/** Represents the root of a parsed Svelte component. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteProgram(
    // 'type' is not present in the root of svelte.parse() output
    val html: SvelteFragment, // Changed from SvelteElement to SvelteFragment
    val css: SvelteStyleNode? = null,
    val instance: SvelteScript? = null,
    val module: SvelteScript? = null,
    // Root itself doesn't have start/end in the svelte.parse output
) // Removed SvelteNode implementation as root doesn't conform

/** Represents the top-level Fragment wrapper for HTML content */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteFragment(
    override val start: Int?,
    override val end: Int?,
    val children: List<SvelteNode> = listOf(),
) : SvelteNode

/** Represents an HTML element node in the template. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteElement(
    val name: String? = null, // For Element, e.g., "div", "p"
    val data: String? = null, // For Text node
    val children: List<SvelteNode>? = listOf(),
    val attributes: List<SvelteAttributeLike>? = listOf(),
    override val start: Int?,
    override val end: Int?,
    // Other properties depending on the type...
) : SvelteNode

/** Represents a plain text node in the template. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteText(val data: String, override val start: Int?, override val end: Int?) :
    SvelteNode

/** Represents a Mustache tag (e.g., `{expression}`). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteMustacheTag(
    val expression: EsTreeNode, // The expression inside { }
    override val start: Int?,
    override val end: Int?,
) : SvelteNode

/** Represents an inline Svelte component (e.g., `<CustomComponent />`). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteInlineComponent(
    val name: String, // Component name, e.g., "CustomComponent"
    val attributes: List<SvelteAttributeLike>? = listOf(), // Props and directives
    val children: List<SvelteNode>? = listOf(), // Child content
    override val start: Int?,
    override val end: Int?,
) : SvelteNode

/** Represents a Svelte conditional block (e.g., `{#if condition}...{/if}`). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteIfBlock(
    val expression: EsTreeNode, // The condition expression
    val children: List<SvelteNode>? = listOf(), // Content when condition is true
    @JsonProperty("else") val elseBlock: SvelteElseBlock? = null, // Optional else block
    override val start: Int?,
    override val end: Int?,
) : SvelteNode

/** Represents a Svelte else block (part of if/else chain). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteElseBlock(
    val children: List<SvelteNode>? = listOf(), // Content for else case
    override val start: Int?,
    override val end: Int?,
) : SvelteNode

/** Represents a `<script>` block. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteScript(
    val context: String?, // "default", "module"
    @JsonProperty("content") val ast: EsTreeProgram, // Changed from content: String
    override val start: Int?,
    override val end: Int?,
    val attributes: List<SvelteAttributeOrBinding>? = listOf(),
) : SvelteNode

/** Represents a `<style>` block. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteStyleNode( // Placeholder for <style> blocks
    override val start: Int?,
    override val end: Int?,
    val attributes: List<SvelteAttributeLike>? = listOf(),
    val children: List<SvelteNode>? = listOf(), // Changed to SvelteNode, contains SvelteRule
    val content: SvelteStyleContent,
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
    JsonSubTypes.Type(value = EsTreeFunctionDeclaration::class, name = "FunctionDeclaration"),
    JsonSubTypes.Type(value = EsTreeBlockStatement::class, name = "BlockStatement"),
    JsonSubTypes.Type(value = EsTreeExportNamedDeclaration::class, name = "ExportNamedDeclaration"),
    // Add other EsTreeNode types here as they are defined, e.g., ExpressionStatement
    // Added types found in HTML mustache tags
    JsonSubTypes.Type(value = EsTreeConditionalExpression::class, name = "ConditionalExpression"),
    JsonSubTypes.Type(value = EsTreeBinaryExpression::class, name = "BinaryExpression"),
    // Added types found in function bodies
    JsonSubTypes.Type(value = EsTreeExpressionStatement::class, name = "ExpressionStatement"),
    JsonSubTypes.Type(value = EsTreeAssignmentExpression::class, name = "AssignmentExpression"),
    JsonSubTypes.Type(value = EsTreeUpdateExpression::class, name = "UpdateExpression"),
    JsonSubTypes.Type(value = EsTreeReturnStatement::class, name = "ReturnStatement"),
    // Added TemplateLiteral support
    JsonSubTypes.Type(value = EsTreeTemplateLiteral::class, name = "TemplateLiteral"),
    JsonSubTypes.Type(value = EsTreeTemplateElement::class, name = "TemplateElement"),
    // Added ObjectPattern support for ES6 destructuring
    JsonSubTypes.Type(value = EsTreeObjectPattern::class, name = "ObjectPattern"),
    JsonSubTypes.Type(value = EsTreeProperty::class, name = "Property"),
    JsonSubTypes.Type(value = EsTreeAssignmentPattern::class, name = "AssignmentPattern"),
    JsonSubTypes.Type(value = EsTreeCallExpression::class, name = "CallExpression"),
)
@JsonIgnoreProperties(ignoreUnknown = true)
interface EsTreeNode : GenericAstNode {
    // The 'type' property is implicitly handled by Jackson due to @JsonTypeInfo
    // start and end are now inherited from GenericAstNode
}

interface EsTreeStatement : EsTreeNode

interface EsTreeExpression : EsTreeNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeProgram(
    val body: List<EsTreeNode>,
    val sourceType: String, // "script" or "module"
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeVariableDeclaration(
    val declarations: List<EsTreeVariableDeclarator>,
    val kind: String, // "var", "let", or "const"
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeStatement

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeVariableDeclarator(
    val id: EsTreeNode, // Changed from EsTreeIdentifier to EsTreeNode to support ObjectPattern
    val init: EsTreeNode?, // Actually an EsTreeExpression
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeIdentifier(val name: String, override val start: Int?, override val end: Int?) :
    EsTreeNode, EsTreeExpression

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeLiteral(
    val value: Any?,
    val raw: String,
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

// New class definitions
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeFunctionDeclaration(
    val id: EsTreeIdentifier?, // Function name, null for anonymous functions in some contexts
    val params: List<EsTreeNode>, // List of EsTreeIdentifier or other patterns
    val body: EsTreeBlockStatement,
    val async: Boolean = false,
    val generator: Boolean = false,
    // 'expression' field is typically for ArrowFunctionExpression, not FunctionDeclaration
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeStatement

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeBlockStatement(
    val body: List<EsTreeNode>, // List of statements
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeStatement

// New class definition for ExportNamedDeclaration
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeExportNamedDeclaration(
    val declaration:
        EsTreeNode?, // Can be VariableDeclaration, FunctionDeclaration, ClassDeclaration
    val specifiers:
        List<EsTreeNode>, // For re-exports like export { name }; List<EsTreeExportSpecifier>
    val source: EsTreeLiteral?, // For re-exports like export * from 'module';
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeStatement

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
    override val start: Int,
    override val end: Int,
    val name: String,
    val value: List<SvelteNode>? = null, // Attribute value can be complex
) : SvelteAttributeLike

/** Represents an EventHandler attribute (e.g., on:click). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteEventHandler(
    override val start: Int?,
    override val end: Int?,
    val name: String, // e.g., "click"
    val expression: EsTreeNode?, // Can be null for shorthand like on:click
    val modifiers: List<String> = listOf(),
) : SvelteNode, SvelteAttributeLike // Implement both

/** Placeholder for different types of expressions within Svelte templates or scripts. */
@JsonIgnoreProperties(ignoreUnknown = true) interface SvelteExpression : SvelteNode

// We can add concrete expression types later (Identifier, Literal, BinaryExpression, CallExpression
// etc.)
// matching the structure produced by Svelte/ESTree within the AST.

@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteAttributeOrBinding(
    val name: String,
    // Attributes can have complex values (Text, MustacheTag, etc.)
    // For simplicity, assuming value is a list of SvelteNodes or simple string for now.
    // This might need to be List<SvelteNode> if attributes can contain tags.
    val value: Any? =
        null, // Could be Boolean for shorthand, or List<SvelteNode> for complex values
    override val start: Int?,
    override val end: Int?,
) : SvelteNode

// --- New CSS Node Definitions ---

@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteRule(
    override val start: Int?,
    override val end: Int?,
    val prelude: SvelteSelectorList,
    val block: SvelteBlock,
) : SvelteNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteSelectorList(
    override val start: Int?,
    override val end: Int?,
    val children: List<SvelteSelector> = listOf(),
) : SvelteNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteSelector(
    override val start: Int?,
    override val end: Int?,
    val children: List<SvelteNode> = listOf(), // Contains TypeSelector, etc.
) : SvelteNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteTypeSelector(override val start: Int?, override val end: Int?, val name: String) :
    SvelteNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteBlock(
    override val start: Int?,
    override val end: Int?,
    val children: List<SvelteDeclaration> = listOf(),
) : SvelteNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteDeclaration(
    override val start: Int?,
    override val end: Int?,
    val property: String,
    val value: String, // Simple string value based on AST
) : SvelteNode

// --- New classes for expressions found in template ---

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeConditionalExpression(
    val test: EsTreeNode, // The condition expression
    val consequent: EsTreeNode, // Expression if true
    val alternate: EsTreeNode, // Expression if false
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeBinaryExpression(
    val operator: String, // e.g., "===", "+", "-"
    val left: EsTreeNode,
    val right: EsTreeNode,
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

// --- New classes for statements/expressions found in function body ---

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeExpressionStatement(
    val expression: EsTreeNode, // The actual expression being executed
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeStatement

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeAssignmentExpression(
    val operator: String, // e.g., "=", "+=", "-="
    val left: EsTreeNode, // Usually an Identifier or MemberExpression
    val right: EsTreeNode, // The value being assigned
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

// --- New class for UpdateExpression ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeUpdateExpression(
    val operator: String, // "++" or "--"
    val argument: EsTreeNode, // The identifier being updated
    val prefix: Boolean, // true if prefix (++x), false if postfix (x++)
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

// --- New class for ReturnStatement ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeReturnStatement(
    val argument: EsTreeNode?, // The expression being returned, can be null
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeStatement // ReturnStatement is a Statement

// --- New classes for TemplateLiteral support ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeTemplateLiteral(
    val quasis: List<EsTreeTemplateElement>, // Template literal parts (strings)
    val expressions: List<EsTreeNode>, // Expressions within ${} 
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeTemplateElement(
    val value: TemplateElementValue,
    val tail: Boolean, // true if this is the final element
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class TemplateElementValue(
    val raw: String, // Raw string content including escape sequences
    val cooked: String? // Processed string content (null if contains invalid escape sequences)
)

// --- New classes for ObjectPattern support (ES6 destructuring) ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeObjectPattern(
    val properties: List<EsTreeProperty>, // List of property patterns
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeProperty(
    val key: EsTreeNode, // Property key (usually Identifier)
    val value: EsTreeNode, // Property value pattern (Identifier, default value, etc.)
    val kind: String = "init", // "init", "get", "set"
    val method: Boolean = false, // true if method
    val shorthand: Boolean = false, // true if shorthand property ({x} instead of {x: x})
    val computed: Boolean = false, // true if computed property ([key]: value)
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeAssignmentPattern(
    val left: EsTreeNode, // The pattern being assigned to (usually Identifier)
    val right: EsTreeNode, // The default value expression
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

// --- New class for CallExpression support ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeCallExpression(
    val callee: EsTreeNode, // The function being called (Identifier, MemberExpression, etc.)
    val arguments: List<EsTreeNode>, // List of argument expressions
    val optional: Boolean = false, // true for optional chaining (func?.())
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression
