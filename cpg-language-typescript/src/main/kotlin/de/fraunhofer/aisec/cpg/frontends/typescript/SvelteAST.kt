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
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls

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
    JsonSubTypes.Type(value = SvelteEachBlock::class, name = "EachBlock"),
    JsonSubTypes.Type(value = SvelteComment::class, name = "Comment"),
    JsonSubTypes.Type(value = SvelteConstTag::class, name = "ConstTag"),
    // CSS Nodes
    JsonSubTypes.Type(value = SvelteRule::class, name = "Rule"),
    JsonSubTypes.Type(value = SvelteSelectorList::class, name = "SelectorList"),
    JsonSubTypes.Type(value = SvelteSelector::class, name = "Selector"),
    JsonSubTypes.Type(value = SvelteTypeSelector::class, name = "TypeSelector"),
    JsonSubTypes.Type(value = SvelteClassSelector::class, name = "ClassSelector"),
    JsonSubTypes.Type(value = SveltePseudoClassSelector::class, name = "PseudoClassSelector"),
    JsonSubTypes.Type(value = SveltePseudoElementSelector::class, name = "PseudoElementSelector"),
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

/** Represents a Svelte each block (e.g., {#each items as item}...{/each}). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteEachBlock(
    override val start: Int?,
    override val end: Int?,
    val expression: EsTreeNode,  // The collection being iterated (e.g., items)
    val context: EsTreeNode,     // Changed from String to EsTreeNode for Identifier (e.g., "item")
    val index: String? = null,   // Optional index variable name (e.g., "i") - this is just a string!
    val children: List<SvelteNode> = emptyList(),  // Content inside the each block
    val elseBlock: SvelteElseBlock? = null         // Optional {:else} block for empty collections
) : SvelteNode

/** Represents a Svelte/HTML comment (e.g., `<!-- comment -->`). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteComment(
    val data: String, // The comment text content
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
    JsonSubTypes.Type(value = EsTreeLogicalExpression::class, name = "LogicalExpression"),
    JsonSubTypes.Type(value = EsTreeUnaryExpression::class, name = "UnaryExpression"),
    JsonSubTypes.Type(value = EsTreeArrowFunctionExpression::class, name = "ArrowFunctionExpression"),
    JsonSubTypes.Type(value = EsTreeMemberExpression::class, name = "MemberExpression"),
    JsonSubTypes.Type(value = EsTreeImportDeclaration::class, name = "ImportDeclaration"),
    JsonSubTypes.Type(value = EsTreeImportSpecifier::class, name = "ImportSpecifier"),
    JsonSubTypes.Type(value = EsTreeImportDefaultSpecifier::class, name = "ImportDefaultSpecifier"),
    JsonSubTypes.Type(value = EsTreeObjectExpression::class, name = "ObjectExpression"),
    JsonSubTypes.Type(value = EsTreeArrayExpression::class, name = "ArrayExpression"),
    JsonSubTypes.Type(value = EsTreeExportSpecifier::class, name = "ExportSpecifier"),
    JsonSubTypes.Type(value = EsTreeIfStatement::class, name = "IfStatement"),
    JsonSubTypes.Type(value = EsTreeTSAsExpression::class, name = "TSAsExpression"),
    JsonSubTypes.Type(value = EsTreeTSTypeReference::class, name = "TSTypeReference"),
    JsonSubTypes.Type(value = EsTreeSpreadElement::class, name = "SpreadElement"),
    JsonSubTypes.Type(value = EsTreeChainExpression::class, name = "ChainExpression"),
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
    JsonSubTypes.Type(value = SvelteClassDirective::class, name = "Class"),
    JsonSubTypes.Type(value = SvelteBinding::class, name = "Binding"),
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
    val value: Any? = null, // Attribute value can be Boolean for shorthand, or List<SvelteNode> for complex values
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

/** Represents a Svelte class directive (e.g., class:active={isActive}). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteClassDirective(
    override val start: Int?,
    override val end: Int?,
    val name: String, // e.g., "active" for class:active
    val expression: EsTreeNode?, // The condition expression, can be null for shorthand
) : SvelteNode, SvelteAttributeLike

/** Represents a Svelte binding directive (e.g., bind:value={myValue}). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteBinding(
    override val start: Int?,
    override val end: Int?,
    val name: String, // e.g., "value" for bind:value
    val expression: EsTreeNode?, // The variable being bound to, can be null for shorthand
) : SvelteNode, SvelteAttributeLike

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
data class SvelteClassSelector(
    override val start: Int?,
    override val end: Int?,
    val name: String // The class name without the dot, e.g., "my-class" for ".my-class"
) : SvelteNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class SveltePseudoClassSelector(
    override val start: Int?,
    override val end: Int?,
    val name: String // The pseudo-class name, e.g., ":hover"
) : SvelteNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class SveltePseudoElementSelector(
    override val start: Int?,
    override val end: Int?,
    val name: String // The pseudo-element name, e.g., "::before"
) : SvelteNode

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
    val properties: List<EsTreeNode>, // Changed from List<EsTreeProperty> to List<EsTreeNode> to support both Property and SpreadElement
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

// --- New class for LogicalExpression support ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeLogicalExpression(
    val operator: String, // Logical operator: "&&", "||", "??"
    val left: EsTreeNode, // Left operand expression
    val right: EsTreeNode, // Right operand expression
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

// --- New class for UnaryExpression support ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeUnaryExpression(
    val operator: String, // Unary operator: "+", "-", "!", "~", "typeof", "void", "delete", etc.
    val argument: EsTreeNode, // The expression being operated on
    val prefix: Boolean = true, // true for prefix operators (most unary ops), false for postfix
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

// --- New class for ArrowFunctionExpression support ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeArrowFunctionExpression(
    val params: List<EsTreeNode>, // Function parameters (Identifier, Pattern, etc.)
    val body: EsTreeNode, // Function body (BlockStatement or Expression)
    val async: Boolean = false, // true for async arrow functions
    val expression: Boolean = true, // true if body is an expression, false if BlockStatement
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

// --- New class for MemberExpression support ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeMemberExpression(
    @JsonProperty("object") val objectNode: EsTreeNode, // The object being accessed (renamed to avoid keyword conflict)
    val property: EsTreeNode, // The property being accessed
    val computed: Boolean = false, // true for computed access like array[index], false for dot notation
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeImportDeclaration(
    val specifiers: List<EsTreeNode>,
    val source: EsTreeLiteral,
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode

// --- New class for ImportSpecifier support ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeImportSpecifier(
    val imported: EsTreeNode, // The name being imported from the module (usually Identifier)
    val local: EsTreeNode, // The local name it's bound to (usually Identifier, can be different for aliases)
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode

// --- New class for ImportDefaultSpecifier support ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeImportDefaultSpecifier(
    val local: EsTreeNode, // The local name it's bound to (usually Identifier)
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode

// --- New class for ObjectExpression support ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeObjectExpression(
    val properties: List<EsTreeNode>, // Changed from List<EsTreeProperty> to List<EsTreeNode> to support both Property and SpreadElement
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

// --- New class for ArrayExpression support ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeArrayExpression(
    val elements: List<EsTreeNode>, // List of array elements (can be null for sparse arrays)
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

// --- New class for ExportSpecifier support ---
@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeExportSpecifier(
    val exported: EsTreeNode, // The name being exported (usually Identifier)
    val local: EsTreeNode, // The local name being exported (usually Identifier, can be different for aliases)
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeIfStatement(
    val test: EsTreeNode, // Changed from 'condition' to 'test' to match standard ESTree naming
    val consequent: EsTreeNode, // Changed from 'then' to 'consequent' to match standard ESTree naming  
    val alternate: EsTreeNode?, // Changed from 'else' to 'alternate' to match standard ESTree naming
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeStatement

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeTSAsExpression(
    val typeAnnotation: EsTreeNode?, // Changed from 'type' to 'typeAnnotation' and made nullable
    val expression: EsTreeNode,
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeTSTypeReference(
    val typeName: EsTreeNode, // Changed from String to EsTreeNode for Identifier
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeSpreadElement(
    val argument: EsTreeNode, // The expression being spread (e.g., obj in ...obj)
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

@JsonIgnoreProperties(ignoreUnknown = true)
data class EsTreeChainExpression(
    val expression: EsTreeNode, // The expression being chained
    override val start: Int?,
    override val end: Int?,
) : EsTreeNode, EsTreeExpression

/** Represents a Svelte const tag (e.g., {@const value = expression}). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SvelteConstTag(
    override val start: Int?,
    override val end: Int?,
    val expression: EsTreeNode, // The assignment expression (e.g., value = computed())
) : SvelteNode
