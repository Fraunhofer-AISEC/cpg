/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import de.fraunhofer.aisec.cpg.CallResolutionResult
import de.fraunhofer.aisec.cpg.SignatureResult
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.ancestors
import de.fraunhofer.aisec.cpg.evaluation.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.component
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.ast.TemplateArguments
import de.fraunhofer.aisec.cpg.graph.pointer
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.helpers.Util.errorWithFileLocation
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.inference.Inference
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import org.neo4j.ogm.annotation.Transient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * [CastResult] is the result of the function [Language.tryCast] and describes whether a cast of one
 * type into another is successful according to the logic of the [Language].
 */
sealed class CastResult(
    /**
     * The "distance" from the base type to the target type that is needed when this object needs to
     * be cast. For example, if the types are the same, the distance is 0. An implicit cast is 1.
     * The distance from a derived class to its direct super class is also 1. It increments for each
     * super class in the hierarchy.
     */
    open var depthDistance: Int
)

data object CastNotPossible : CastResult(-1)

data object DirectMatch : CastResult(0)

data class ImplicitCast(override var depthDistance: Int) : CastResult(depthDistance) {
    companion object : CastResult(1)
}

/**
 * Represents a programming language. When creating new languages in the CPG, one must derive custom
 * class from this and override the necessary fields and methods.
 *
 * Furthermore, since this also implements [Node], one node for each programming language used is
 * persisted in the final graph (database) and each node links to its corresponding language using
 * the [Node.language] property.
 */
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
abstract class Language<T : LanguageFrontend<*, *>>() : Node() {

    /** The file extensions without the dot */
    abstract val fileExtensions: List<String>

    /** The namespace delimiter used by the language. Often, this is "." */
    open val namespaceDelimiter: String = "."

    @get:JsonSerialize(using = KClassSerializer::class)
    /** The class of the frontend which is used to parse files of this language. */
    abstract val frontend: KClass<out T>

    /** The primitive type names of this language. */
    @get:JsonIgnore
    val primitiveTypeNames: Set<String>
        get() = builtInTypes.keys

    /** The built-in types of this language. */
    @get:JsonIgnore abstract val builtInTypes: Map<String, Type>

    /** The access modifiers of this programming language */
    open val accessModifiers: Set<String>
        get() = setOf("public", "protected", "private")

    /** The arithmetic operations of this language */
    open val arithmeticOperations: Set<String>
        get() = setOf("+", "-", "*", "/", "%", "<<", ">>")

    /** All operators which perform and assignment and an operation using lhs and rhs. */
    abstract val compoundAssignmentOperators: Set<String>

    /** All operators which perform a simple assignment from the rhs to the lhs. */
    open val simpleAssignmentOperators: Set<String> = setOf("=")

    /** The standard evaluator to be used with this language. */
    @Transient @DoNotPersist open val evaluator: ValueEvaluator = ValueEvaluator()

    init {
        this.language = this
        this.name = Name(this::class.simpleName ?: EMPTY_NAME)
    }

    /**
     * Creates a new [LanguageFrontend] object to parse the language. It requires the
     * [TranslationContext], which holds the necessary managers.
     */
    open fun newFrontend(ctx: TranslationContext): T {
        return this.frontend.primaryConstructor?.call(ctx, this)
            ?: throw TranslationException("could not instantiate language frontend")
    }

    /**
     * Returns the type conforming to the given [typeString]. If no matching type is found in the
     * [builtInTypes] map, it returns null. The [typeString] must precisely match the key in the
     * map.
     */
    fun getSimpleTypeOf(typeString: CharSequence) = builtInTypes[typeString.toString()]

    /** Returns true if the [file] can be handled by the frontend of this language. */
    fun handlesFile(file: File): Boolean {
        return file.extension in fileExtensions
    }

    override fun equals(other: Any?): Boolean {
        return other?.javaClass == this.javaClass
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + fileExtensions.hashCode()
        result = 31 * result + namespaceDelimiter.hashCode()
        result = 31 * result + frontend.hashCode()
        result = 31 * result + primitiveTypeNames.hashCode()
        result = 31 * result + accessModifiers.hashCode()
        return result
    }

    private fun arithmeticOpTypePropagation(lhsType: Type, rhsType: Type): Type {
        return when {
            lhsType is FloatingPointType &&
                rhsType !is FloatingPointType &&
                rhsType is NumericType -> lhsType
            lhsType !is FloatingPointType &&
                lhsType is NumericType &&
                rhsType is FloatingPointType -> rhsType
            lhsType is FloatingPointType && rhsType is FloatingPointType ||
                lhsType is IntegerType && rhsType is IntegerType ->
                // We take the one with the bigger bit-width
                if ((lhsType.bitWidth ?: 0) >= (rhsType.bitWidth ?: 0)) {
                    lhsType
                } else {
                    rhsType
                }
            lhsType is BooleanType && rhsType is BooleanType -> lhsType
            else -> unknownType()
        }
    }

    /**
     * Determines how to propagate types across binary operations since these may differ among the
     * programming languages. This intentionally uses the [Type] of the left-hand side and
     * right-hand side to determine the type of the binary operation instead of the [BinaryOperator]
     * node, because we want to use it in our new symbol resolver that operates on a state that
     * contains the type, rather than the [HasType.type] directly.
     *
     * Optionally, a [BinaryOperator] can be passed as a hint to the function. This is useful for
     * languages that need access to additional information from the [BinaryOperator] node.
     * Implementors who override this function must ensure that they do NOT use the [HasType.type]
     * of the [BinaryOperator.lhs] / [BinaryOperator.rhs] property of [hint], but use the [lhsType]
     * and [rhsType] instead.
     *
     * @param operatorCode The [BinaryOperator.operatorCode]
     * @param lhsType The type of the left-hand side ([BinaryOperator.lhs])
     * @param rhsType The type of the right-hand side ([BinaryOperator.rhs])
     * @param hint The [BinaryOperator] node that is used as a hint for the language to determine
     *   the type of the binary operation. This is optional and can be null.
     */
    open fun propagateTypeOfBinaryOperation(
        operatorCode: String?,
        lhsType: Type,
        rhsType: Type,
        hint: BinaryOperator? = null,
    ): Type {
        return when (operatorCode) {
            "<",
            "=<",
            ">",
            "<=",
            "==",
            "===" ->
                // A comparison, so we return the type "boolean"
                this.builtInTypes.values.firstOrNull { it is BooleanType }
                    ?: this.builtInTypes.values.firstOrNull { it.name.localName.startsWith("bool") }
                    ?: unknownType()
            "+" ->
                when {
                    lhsType is StringType -> {
                        // string + anything => string
                        lhsType
                    }
                    rhsType is StringType -> {
                        // anything + string => string
                        rhsType
                    }
                    else -> {
                        arithmeticOpTypePropagation(lhsType, rhsType)
                    }
                }
            "-",
            "*",
            "/",
            "%",
            "&",
            "&&",
            "|",
            "||",
            "^" -> arithmeticOpTypePropagation(lhsType, rhsType)
            "<<",
            ">>",
            ">>>" ->
                if (lhsType.isPrimitive && rhsType.isPrimitive) {
                    // primitive type 1 OP primitive type 2 => primitive type 1
                    lhsType
                } else {
                    unknownType()
                }
            else -> unknownType() // We don't know what is this thing
        }
    }

    /**
     * Determines how to propagate types across unary operations since these may differ among the
     * programming languages.
     *
     * @param operatorCode The [UnaryOperator.operatorCode]
     * @param inputType The type of the input ([UnaryOperator.input])
     */
    open fun propagateTypeOfUnaryOperation(operatorCode: String?, inputType: Type): Type {
        return when (operatorCode) {
            "*" -> inputType.dereference()
            "&" -> inputType.pointer()
            else -> inputType
        }
    }

    /**
     * When propagating [HasType.assignedTypes] from one node to another, we might want to propagate
     * only certain types. A common example is to truncate [NumericType]s, when they are not "big"
     * enough.
     *
     * @param existingType The existing type of the node that should be updated
     * @param newType The new type that should be propagated
     * @param hint The node that is used as a hint for the language to determine the type of the
     *   node that should be updated
     */
    open fun shouldPropagateType(existingType: Type, newType: Type, hint: HasType): Boolean {
        return when {
            // We only want to add certain types, in case we have a numeric type
            existingType is NumericType -> {
                // We do not allow to propagate non-numeric types into numeric types
                if (newType !is NumericType) {
                    false
                } else {
                    val srcWidth = newType.bitWidth
                    val lhsWidth = existingType.bitWidth
                    // Do not propagate anything if the new type is too big for the current type.
                    return !(lhsWidth != null && srcWidth != null && lhsWidth < srcWidth)
                }
            }
            // We do not want to propagate a dynamic type
            newType is DynamicType -> {
                false
            }
            else -> {
                true
            }
        }
    }

    /**
     * This function checks, if [type] can be cast into [targetType]. Note, this also takes the
     * "type" of the type into account, which means that pointer types of derived types will not
     * match with a non-pointer type of its base type. But, if both are pointer types, they will
     * match.
     *
     * Optionally, the nodes that hold the respective type can be supplied as [hint] and
     * [targetHint].
     */
    open fun tryCast(
        type: Type,
        targetType: Type,
        hint: HasType? = null,
        targetHint: HasType? = null,
    ): CastResult {
        // We can take a shortcut if it is the same type
        if (type == targetType) {
            return DirectMatch
        }

        // We can also take a shortcut: if they are not of the same subclass, they will never
        // match
        if (type::class != targetType::class) {
            return CastNotPossible
        }

        // Retrieve all ancestor types of our type (more concretely of the root type)
        val root = type.root
        val ancestors = root.ancestors
        val superTypes = ancestors.map(Type.Ancestor::type)

        return if (targetType.root in superTypes) {
            // Find depth
            val depth = ancestors.firstOrNull { it.type == targetType.root }?.depth
            if (depth == null) {
                // This should not happen
                CastNotPossible
            } else {
                ImplicitCast(depth)
            }
        } else {
            CastNotPossible
        }
    }

    /**
     * This functions gives the language a chance to refine the results of a
     * [SymbolResolver.resolveWithArguments] by choosing the best viable function(s) out of the set
     * of viable functions. It can also influence the [CallResolutionResult.SuccessKind] of the
     * resolution, e.g., if the result is ambiguous.
     *
     * The default implementation will follow the following heuristic:
     * - If the list of [CallResolutionResult.viableFunctions] is empty, we can directly return.
     * - If we have only one item in [CallResolutionResult.viableFunctions], we can take it.
     * - Next, we can check for direct matches, meaning that they have a [SignatureResult] that only
     *   has [DirectMatch] casts.
     * - Lastly, if we have no direct matches, we need to sort the viable functions using a simple
     *   ranking. The function(s) will the best (lowest) [SignatureResult.ranking] will be chosen as
     *   the best. The ranking is determined by the [CastResult.depthDistance] of all cast results
     *   in the signature results.
     */
    context(provider: ContextProvider)
    open fun bestViableResolution(
        result: CallResolutionResult
    ): Pair<Set<FunctionDeclaration>, CallResolutionResult.SuccessKind> {
        // Check for direct matches. Let's hope there is only one, otherwise we have an ambiguous
        // result
        val directMatches = result.signatureResults.entries.filter { it.value.isDirectMatch }
        if (directMatches.size > 1) {
            // This is an ambiguous result. Let's return all direct matches
            return Pair(
                directMatches.map { it.key }.toSet(),
                CallResolutionResult.SuccessKind.AMBIGUOUS,
            )
        } else if (directMatches.size == 1) {
            // Let's return the single direct match
            return Pair(
                setOf(directMatches.first().key),
                CallResolutionResult.SuccessKind.SUCCESSFUL,
            )
        }

        // No direct match yet, let's continue with some casting...

        // TODO: Move this code somewhere else once we have a proper template expansion pass
        // We need to check, whether this language has special handling of templates. In this
        // case, we need to check, whether a template matches directly after we have no direct
        // matches
        val source = result.source
        if (this is HasTemplates && source is CallExpression) {
            source.templateArgumentEdges = TemplateArguments(source)
            val (ok, candidates) =
                this.handleTemplateFunctionCalls(
                    null,
                    source,
                    false,
                    provider.ctx,
                    null,
                    needsExactMatch = true,
                )
            if (ok) {
                return Pair(candidates.toSet(), CallResolutionResult.SuccessKind.SUCCESSFUL)
            }

            source.templateArgumentEdges = null
        }

        // If the list of viable functions is still empty at this point, the call is unresolved
        if (result.viableFunctions.isEmpty()) {
            return Pair(setOf(), CallResolutionResult.SuccessKind.UNRESOLVED)
        }

        // Otherwise, sort it according to a simple ranking based on the total number of
        // conversions needed. This might not necessarily be the best idea and this is
        // also not really optimized.
        val rankings = result.signatureResults.entries.map { Pair(it.value.ranking, it.key) }

        // Find the best (lowest) rank and find functions with the specific rank
        val bestRanking = rankings.minBy { it.first }.first
        val list = rankings.filter { it.first == bestRanking }.map { it.second }
        return if (list.size > 1) {
            // Return the list of best-ranked (hopefully only one). If one then more result has
            // the same ranking, this result is ambiguous
            Pair(list.toSet(), CallResolutionResult.SuccessKind.AMBIGUOUS)
        } else {
            Pair(list.toSet(), CallResolutionResult.SuccessKind.SUCCESSFUL)
        }
    }

    /**
     * This function returns the best viable declaration when resolving a [Reference]. The
     * candidates to chose from are stored in [Reference.candidates] In most cases the languages can
     * keep the default implementation, which only returns a declaration, if the list contains one
     * single item. Otherwise, we have an ambiguous result and cannot determine the result with
     * certainty.
     *
     * If we encounter an ambiguous result, a warning is issued.
     */
    open fun bestViableReferenceCandidate(ref: Reference): Declaration? {
        return if (ref.candidates.size > 1) {
            Util.warnWithFileLocation(
                ref,
                log,
                "Resolution of reference {} was ambiguous, cannot set refersTo correctly, " +
                    "will be set to null.",
                ref.name,
            )
            null
        } else {
            ref.candidates.singleOrNull()
        }
    }

    /**
     * There are some cases where our [Inference] system needs to place declarations, e.g., a
     * [NamespaceDeclaration] in the [GlobalScope]. The issue with that is that the [Scope.astNode]
     * of the global scope is always the last parsed [TranslationUnitDeclaration] and we might end
     * up adding the declaration to some random translation unit, where it does not really belong.
     *
     * Therefore, we give the language a chance to return a [TranslationUnitDeclaration] where the
     * declaration should be placed. If the language does not override this function, the default
     * implementation will return the first [TranslationUnitDeclaration] in the [Component] of
     * [source].
     *
     * But languages might choose to take the information of [TypeToInfer] and [source] and create a
     * specific [TranslationUnitDeclaration], e.g., for each namespace that is inferred globally or
     * try to put all inferred declarations into one specific (inferred) new translation unit.
     *
     * @param TypeToInfer the type of the node that should be inferred
     * @param source the source that was responsible for the inference
     */
    context(provider: ContextProvider)
    fun <TypeToInfer : Node> translationUnitForInference(source: Node): TranslationUnitDeclaration {
        // The easiest way to identify the current component would be traversing the AST, but that
        // does not work for types. But types have a scope and the scope (should) have the
        // connection to the AST. We add several fallbacks here to make sure that we have a
        // component.
        val component =
            if (source !is Type) {
                source.component
                    ?: provider.ctx.currentComponent
                    ?: source.scope?.astNode?.component
            } else {
                provider.ctx.currentComponent ?: source.scope?.astNode?.component
            }
        if (component == null) {
            val msg =
                "No suitable component found that should be used for inference. " +
                    "That should not happen and it seems that there is a serious problem with handling this node"
            errorWithFileLocation(source, log, msg)
            throw TranslationException(msg)
        }

        // We should also make sure that the language matches
        val tu = component.translationUnits.firstOrNull { it.language == this }
        if (tu == null) {
            val msg = "No suitable translation unit found that should be used for inference"
            errorWithFileLocation(source, log, msg)
            throw TranslationException(msg)
        }

        return tu
    }

    companion object {
        @JvmStatic protected val log: Logger = LoggerFactory.getLogger(Language::class.java)
    }
}

/**
 * We need to bring our own serializer for [KClass] until
 * https://github.com/FasterXML/jackson-module-kotlin/issues/361 is resolved.
 */
internal class KClassSerializer : JsonSerializer<KClass<*>>() {
    override fun serialize(value: KClass<*>, gen: JsonGenerator, provider: SerializerProvider) {
        // Write the fully qualified name as a string
        gen.writeString(value.qualifiedName)
    }
}

/**
 * Represents a language definition with no known implementation or specifics. The class is used as
 * a placeholder or to handle cases where the language is not explicitly defined or supported.
 */
object UnknownLanguage : Language<Nothing>() {
    override val fileExtensions: List<String>
        get() = listOf()

    override val frontend: KClass<out Nothing> = Nothing::class
    override val builtInTypes: Map<String, Type> = mapOf()
    override val compoundAssignmentOperators: Set<String> = setOf()
}

/**
 * Represents a "language" that is not really a language. The class is used in cases where the
 * language is not explicitly defined or supported, for example in an [OverlayNode].
 */
object NoLanguage : Language<Nothing>() {
    override val fileExtensions = listOf<String>()
    override val frontend: KClass<out Nothing> = Nothing::class
    override val builtInTypes: Map<String, Type> = mapOf()
    override val compoundAssignmentOperators: Set<String> = setOf()
}

/**
 * Represents a composite language definition composed of multiple languages.
 *
 * @property languages A list of languages that are part of this composite language definition.
 */
class MultipleLanguages(val languages: Set<Language<*>>) : Language<Nothing>() {
    override val fileExtensions = languages.flatMap { it.fileExtensions }
    override val frontend: KClass<out Nothing> = Nothing::class
    override val builtInTypes: Map<String, Type> = mapOf()
    override val compoundAssignmentOperators: Set<String> = setOf()
}

/**
 * Returns the single language of a node and its children. If the node has multiple children with
 * different languages, it returns a [MultipleLanguages] object.
 */
fun Node.multiLanguage(): Language<*> {
    val languages = astChildren.map { it.language }.toSet()
    return if (languages.size == 1) {
        languages.single()
    } else if (languages.size > 1) {
        MultipleLanguages(languages = languages)
    } else {
        UnknownLanguage
    }
}
