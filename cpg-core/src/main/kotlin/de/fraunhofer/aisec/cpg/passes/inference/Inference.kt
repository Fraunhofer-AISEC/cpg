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
package de.fraunhofer.aisec.cpg.passes.inference

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TypeManager
import de.fraunhofer.aisec.cpg.frontends.HasClasses
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.TypeExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.helpers.Util.debugWithFileLocation
import de.fraunhofer.aisec.cpg.helpers.Util.errorWithFileLocation
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class contains different kinds of helper that *infer* certain [Node]s that are not present
 * in the source code but that we assure are present in the overall program (e.g., if because we
 * only see parts of it).
 *
 * Each inference has a certain [start] point, which might depend on the type of inference. For
 * example, to infer a [MethodDeclaration], the obvious start point would be a [RecordDeclaration].
 * Since this class implements [IsInferredProvider], all nodes that are created using the node
 * builder functions, will automatically have [Node.isInferred] set to true.
 */
class Inference internal constructor(val start: Node, override val ctx: TranslationContext) :
    LanguageProvider,
    ScopeProvider,
    IsInferredProvider,
    ContextProvider,
    RawNodeTypeProvider<Nothing> {

    override val language: Language<*>
        get() = start.language

    override val isInferred: Boolean
        get() = true

    val scopeManager: ScopeManager
    val typeManager: TypeManager

    override val scope: Scope?
        get() = scopeManager.currentScope

    fun inferFunctionDeclaration(
        name: CharSequence?,
        code: String?,
        isStatic: Boolean,
        signature: List<Type?>,
        incomingReturnType: Type?,
        hint: CallExpression? = null,
    ): FunctionDeclaration? {
        if (!ctx.config.inferenceConfiguration.inferFunctions) {
            return null
        }

        // We assume that the start is either a record, a namespace or the translation unit
        val record = start as? RecordDeclaration
        val namespace = start as? NamespaceDeclaration
        val tu = start as? TranslationUnitDeclaration

        // If all are null, we have the wrong type
        if (record == null && namespace == null && tu == null) {
            throw UnsupportedOperationException(
                "Starting inference with the wrong type of start node"
            )
        }

        return inferInScopeOf(start) {
            val inferred: FunctionDeclaration =
                if (record != null) {
                    newMethodDeclaration(name ?: "", isStatic, record)
                } else {
                    newFunctionDeclaration(name ?: "")
                }
            inferred.code = code

            // Create parameter declarations and receiver (only for methods).
            if (inferred is MethodDeclaration) {
                createInferredReceiver(inferred, record)
            }
            createInferredParameters(inferred, signature)

            // Set the type and return type(s)
            var returnType =
                if (
                    ctx.config.inferenceConfiguration.inferReturnTypes &&
                        incomingReturnType is UnknownType &&
                        hint != null
                ) {
                    inferReturnType(hint) ?: unknownType()
                } else {
                    incomingReturnType
                }

            if (returnType is TupleType) {
                inferred.returnTypes = returnType.types
            } else if (returnType != null) {
                inferred.returnTypes = listOf(returnType)
            }

            inferred.type = computeType(inferred)

            debugWithFileLocation(
                hint,
                log,
                "Inferred a new {} declaration {} with parameter types {} and return types {} in {}",
                if (inferred is MethodDeclaration) "method" else "function",
                inferred.name,
                signature.map { it?.name },
                inferred.returnTypes.map { it.name },
                it,
            )

            // Add it to the scope
            scopeManager.addDeclaration(inferred)
            start.addDeclaration(inferred)

            // Some magic that adds it to static imports. Not sure if this really needed
            if (record != null && isStatic) {
                record.staticImports.add(inferred)
            }

            // Some more magic, that adds it to the AST. Note: this might not be 100 % compliant
            // with the language, since in some languages the AST of a method declaration could be
            // outside of a method, but this will do for now
            if (record != null && inferred is MethodDeclaration) {
                record.addMethod(inferred)
            }

            // "upgrade" our struct to a class, if it was inferred by us, since we are calling
            // methods on it. But only if the language supports classes in the first place.
            if (
                record?.isInferred == true &&
                    record.kind == "struct" &&
                    record.language is HasClasses
            ) {
                record.kind = "class"
            }

            inferred
        }
    }

    fun createInferredConstructor(signature: List<Type?>): ConstructorDeclaration {
        return inferInScopeOf(start) {
            val record = start as? RecordDeclaration
            val inferred = newConstructorDeclaration(start.name.localName, record)
            createInferredParameters(inferred, signature)

            scopeManager.addDeclaration(inferred)
            record?.addConstructor(inferred)

            inferred
        }
    }

    /**
     * This wrapper should be used around any kind of inference code that actually creates a
     * [Declaration]. It takes cares of "jumping" to the appropriate scope of the [start] node,
     * executing the commands in [init] (which needs to create an inferred node of [T]) as well as
     * restoring the previous scope.
     */
    private fun <T : Declaration> inferInScopeOf(start: Node, init: (scope: Scope?) -> T): T {
        return scopeManager.withScope(scopeManager.lookupScope(start), init)
    }

    /**
     * This function creates a [VariableDeclaration], which acts as the
     * [MethodDeclaration.receiver], in order to hold all data flows to the object instance of this
     * particular [method].
     */
    private fun createInferredReceiver(method: MethodDeclaration, record: RecordDeclaration?) {
        // We do not really know, how a receiver is called in a particular language, but we will
        // probably not do anything wrong by calling it "this".
        val receiver = newVariableDeclaration("this", record?.toType() ?: unknownType())
        method.receiver = receiver
    }

    /**
     * This function creates a [ParameterDeclaration] for each parameter in the [function]'s
     * [signature].
     */
    private fun createInferredParameters(function: FunctionDeclaration, signature: List<Type?>) {
        // To save some unnecessary scopes, we only want to "enter" the function if it is necessary,
        // e.g., if we need to create parameters
        if (signature.isNotEmpty()) {
            scopeManager.enterScope(function)

            for (i in signature.indices) {
                val targetType = signature[i] ?: UnknownType.getUnknownType(function.language)
                val paramName = generateParamName(i, targetType)
                val param = newParameterDeclaration(paramName, targetType, false)
                param.argumentIndex = i

                scopeManager.addDeclaration(param)
                function.parameters += param
            }

            scopeManager.leaveScope(function)
        }
    }

    /** Generates a name for an inferred function parameter based on the type. */
    private fun generateParamName(i: Int, targetType: Type): String {
        val hierarchy: Deque<String> = ArrayDeque()
        var currLevel: Type? = targetType
        while (currLevel != null) {
            currLevel =
                when (currLevel) {
                    is FunctionPointerType -> {
                        hierarchy.push("Fptr")
                        null
                    }
                    is PointerType -> {
                        hierarchy.push("Ptr")
                        currLevel.elementType
                    }
                    is ReferenceType -> {
                        hierarchy.push("Ref")
                        currLevel.elementType
                    }
                    else -> {
                        hierarchy.push(currLevel.typeName)
                        null
                    }
                }
        }
        val paramName = StringBuilder()
        while (hierarchy.isNotEmpty()) {
            val part = hierarchy.pop()
            if (part.isEmpty()) {
                continue
            }
            if (paramName.isNotEmpty()) {
                paramName.append(part.substring(0, 1).uppercase(Locale.getDefault()))
                if (part.length >= 2) {
                    paramName.append(part.substring(1))
                }
            } else {
                paramName.append(part.lowercase(Locale.getDefault()))
            }
        }
        paramName.append(i)
        return paramName.toString()
    }

    private fun inferNonTypeTemplateParameter(name: String): ParameterDeclaration {
        val expr =
            start as? Expression
                ?: throw UnsupportedOperationException(
                    "Starting inference with the wrong type of start node"
                )

        // Non-Type Template Parameter
        val param = newParameterDeclaration(name, expr.type, false)
        param.code = name
        return param
    }

    private fun inferTemplateParameter(name: String): TypeParameterDeclaration {
        val parameterizedType = ParameterizedType(name, language)
        typeManager.addTypeParameter(start as FunctionTemplateDeclaration, parameterizedType)

        val decl = newTypeParameterDeclaration(name)
        decl.code = name
        decl.type = parameterizedType

        return decl
    }

    /**
     * Create an inferred FunctionTemplateDeclaration if a call to an FunctionTemplate could not be
     * resolved
     *
     * @param call
     * @return inferred FunctionTemplateDeclaration which can be invoked by the call
     */
    fun inferFunctionTemplate(call: CallExpression): FunctionTemplateDeclaration {
        // We assume that the start is either a record or the translation unit
        val record = start as? RecordDeclaration
        val tu = start as? TranslationUnitDeclaration

        // If both are null, we have the wrong type
        if (record == null && tu == null) {
            throw UnsupportedOperationException(
                "Starting inference with the wrong type of start node"
            )
        }

        val name = call.name.localName
        val code = call.code
        val inferred = newFunctionTemplateDeclaration(name)
        inferred.code = code
        inferred.isInferred = true

        val inferredRealization =
            if (record != null) {
                record.addDeclaration(inferred)
                record.inferMethod(call, ctx = ctx)
            } else {
                tu?.addDeclaration(inferred)
                tu?.inferFunction(call, ctx = ctx)
            }

        inferredRealization?.let { inferred.realization += it }

        var typeCounter = 0
        var nonTypeCounter = 0
        for (node in call.templateArguments) {
            if (node is TypeExpression) {
                // Template Parameter
                val inferredTypeIdentifier = "T$typeCounter"
                val typeParamDeclaration =
                    inferred.startInference(ctx)?.inferTemplateParameter(inferredTypeIdentifier)
                typeCounter++
                if (typeParamDeclaration != null) {
                    inferred.parameters += typeParamDeclaration
                }
            } else if (node is Expression) {
                val inferredNonTypeIdentifier = "N$nonTypeCounter"
                val paramVariableDeclaration =
                    node
                        .startInference(ctx)
                        ?.inferNonTypeTemplateParameter(inferredNonTypeIdentifier)
                if (paramVariableDeclaration != null) {
                    node.nextDFGEdges += paramVariableDeclaration
                }
                nonTypeCounter++
                if (paramVariableDeclaration != null) {
                    inferred.parameters += paramVariableDeclaration
                }
            }
        }
        return inferred
    }

    /**
     * Infers a record declaration for the given type. [type] is the object type representing a
     * record that we want to infer. The [kind] specifies if we create a class or a struct.
     *
     * Since [Type] does not contain a location, a separate node that contains a location can
     * optionally be specified in [locationHint]. This could for example be a call expression that
     * contained the reference to a class method.
     */
    fun inferRecordDeclaration(
        type: Type,
        kind: String = "class",
        locationHint: Node? = null,
    ): RecordDeclaration? {
        if (!ctx.config.inferenceConfiguration.inferRecords) {
            return null
        }

        // We assume that the start is either a record, a namespace or the translation unit
        val record = start as? RecordDeclaration
        val namespace = start as? NamespaceDeclaration
        val tu = start as? TranslationUnitDeclaration

        // If all are null, we have the wrong type
        if (record == null && namespace == null && tu == null) {
            throw UnsupportedOperationException(
                "Starting inference with the wrong type of start node"
            )
        }

        if (type !is ObjectType) {
            errorWithFileLocation(
                locationHint,
                log,
                "Trying to infer a record declaration of a non-object type. Not sure what to do? Should we change the type?",
            )
            return null
        }

        return inferInScopeOf(start) {
            // This could be a class or a struct. We start with a class and may have to fine-tune
            // this later.
            val declaration = newRecordDeclaration(type.typeName, kind)
            declaration.isInferred = true

            debugWithFileLocation(
                locationHint,
                log,
                "Inferred a new record declaration ${declaration.name} (${declaration.kind}) in $it",
            )

            // Update the type
            type.recordDeclaration = declaration

            // Make sure the record is registered as a scope itself
            scopeManager.enterScope(declaration)
            scopeManager.leaveScope(declaration)

            scopeManager.addDeclaration(declaration)
            start.addDeclaration(declaration)
            declaration
        }
    }

    /**
     * This infers a [VariableDeclaration] based on an unresolved [Reference], which is supplied as
     * a [hint]. Currently, this is only used to infer global variables. In the future, we might
     * also infer static variables in namespaces.
     */
    fun inferVariableDeclaration(hint: Reference): VariableDeclaration? {
        if (!ctx.config.inferenceConfiguration.inferVariables) {
            return null
        }

        return inferInScopeOf(start) {
            // Build a new variable declaration from the reference. Maybe we are even lucky and the
            // reference has a type -- some language frontends provide us one -- but most likely
            // this type will be unknown.
            val inferred = newVariableDeclaration(hint.name, hint.type)

            debugWithFileLocation(
                hint,
                log,
                "Inferred a new variable declaration {} with type {} in $it",
                inferred.name,
                inferred.type,
            )

            // In any case, we will observe the type of our reference and update our new variable
            // declaration accordingly.
            hint.typeObservers += TypeInferenceObserver(inferred)

            // Add it to the scope
            scopeManager.addDeclaration(inferred)
            (start as? DeclarationHolder)?.addDeclaration(inferred)

            inferred
        }
    }

    fun inferNamespaceDeclaration(name: Name, path: String?, origin: Node?): NamespaceDeclaration? {
        if (!ctx.config.inferenceConfiguration.inferNamespaces) {
            return null
        }

        return inferInScopeOf(start) {
            debugWithFileLocation(
                origin,
                log,
                "Inferring a new namespace declaration {} (path: {}) in $it",
                name,
                if (path != null) {
                    "with path '$path'"
                } else {
                    ""
                },
            )

            val inferred = newNamespaceDeclaration(name)
            inferred.path = path

            scopeManager.addDeclaration(inferred)
            (start as? DeclarationHolder)?.addDeclaration(inferred)

            // We need to "enter" the scope to make it known to the scope map of the ScopeManager
            scopeManager.enterScope(inferred)
            scopeManager.leaveScope(inferred)
            inferred
        }
    }

    /**
     * This class implements a [HasType.TypeObserver] and uses the observed type to set the
     * [ValueDeclaration.type] of a [ValueDeclaration], based on the types we see. It can be
     * registered on objects that are used to "start" an inference, for example a
     * [MemberExpression], which infers a [FieldDeclaration]. Once the type of the member expression
     * becomes known, we can use this information to set the type of the field.
     *
     * For now, this implementation uses the first type that we "see" and once the type of our
     * [declaration] is known, we ignore further updates. In a future implementation, we could try
     * to fine-tune this, e.g. by finding a common type (such as an interface) that is more
     * probable, if multiple types are assigned.
     */
    class TypeInferenceObserver(var declaration: ValueDeclaration) : HasType.TypeObserver {
        override fun typeChanged(newType: Type, src: HasType) {
            // Only set a new type, if it is unknown for now
            if (declaration.type is UnknownType) {
                declaration.type = newType
            } else {
                // TODO(oxisto): We could "refine" the type here based on further type
                //  observations
            }
        }

        override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
            // Only set a new type, if it is unknown for now
            if (declaration.type is UnknownType) {
                // For now, just set it if there is only one type
                if (assignedTypes.size == 1) {
                    val type = assignedTypes.single()
                    log.debug(
                        "Inferring type of declaration {} to be {}",
                        declaration.name,
                        type.name,
                    )

                    declaration.type = type
                }
            } else {
                // TODO(oxisto): We could "refine" the type here based on further type
                //  observations
            }
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(Inference::class.java)
    }

    init {
        this.scopeManager = ctx.scopeManager
        this.typeManager = ctx.typeManager
    }

    /**
     * This function tries to infer a return type for an inferred [FunctionDeclaration] based the
     * original [CallExpression] (as the [hint]) parameter that was used to infer the function.
     */
    fun inferReturnType(hint: CallExpression): Type? {
        // Try to find out, if the supplied hint is part of an assignment. If yes, we can use their
        // type as the return type of the function
        var targetType =
            ctx.currentComponent.assignments.singleOrNull { it.value == hint }?.target?.type
        if (targetType != null && targetType !is UnknownType) {
            return targetType
        }

        // Look for an "argument holder". These can be different kind of nodes
        val holder =
            ctx.currentComponent.allChildren<ArgumentHolder> { it.hasArgument(hint) }.singleOrNull()
        when (holder) {
            is UnaryOperator -> {
                // If it's a boolean operator, the return type is probably a boolean
                if (holder.operatorCode == "!") {
                    return hint.language.builtInTypes.values.firstOrNull { it is BooleanType }
                }
                // If it's a numeric operator, return the largest numeric type that we have; we
                // prefer integers to floats
                if (holder.operatorCode in listOf("+", "-", "++", "--")) {
                    val numericTypes =
                        hint.language.builtInTypes.values
                            .filterIsInstance<NumericType>()
                            .sortedWith(
                                compareBy<NumericType> { it.bitWidth }
                                    .then { a, b -> preferIntegerType(a, b) }
                            )

                    return numericTypes.lastOrNull()
                }
            }
            is ConstructExpression -> {
                return holder.type
            }
            is BinaryOperator -> {
                // If it is on the right side, it's probably the same as on the left-side (and
                // vice versa)
                if (hint == holder.rhs) {
                    return holder.lhs.type
                } else if (hint == holder.lhs) {
                    return holder.rhs.type
                }
            }
            is ReturnStatement -> {
                // If this is part of a return statement, we can take the return type
                val func = hint.firstParentOrNull<FunctionDeclaration>()
                val returnTypes = func?.returnTypes

                return if (returnTypes != null && returnTypes.size > 1) {
                    TupleType(returnTypes)
                } else {
                    returnTypes?.singleOrNull()
                }
            }
        }

        return null
    }
}

/** Provides information about the inference status of a node. */
interface IsInferredProvider : MetadataProvider {
    val isInferred: Boolean
}

/** Provides information about the implicit status of a node. */
interface IsImplicitProvider : MetadataProvider {
    val isImplicit: Boolean
}

/**
 * Returns a new [Inference] object starting from this node. This will check, whether inference is
 * enabled at all (using [InferenceConfiguration.enabled]). Otherwise null, will be returned.
 */
fun Node.startInference(ctx: TranslationContext): Inference? {
    if (!ctx.config.inferenceConfiguration.enabled) {
        return null
    }

    return Inference(this, ctx)
}

/** Tries to infer a [FunctionDeclaration] from a [CallExpression]. */
fun TranslationUnitDeclaration.inferFunction(
    call: CallExpression,
    isStatic: Boolean = false,
    ctx: TranslationContext,
): FunctionDeclaration? {
    return startInference(ctx)
        ?.inferFunctionDeclaration(
            call.name.localName,
            call.code,
            isStatic,
            call.signature,
            // TODO: Is the call's type the return value's type?
            call.type,
            call,
        )
}

/** Tries to infer a [FunctionDeclaration] from a [CallExpression]. */
fun NamespaceDeclaration.inferFunction(
    call: CallExpression,
    isStatic: Boolean = false,
    ctx: TranslationContext,
): FunctionDeclaration? {
    return startInference(ctx)
        ?.inferFunctionDeclaration(
            call.name,
            call.code,
            isStatic,
            call.signature,
            // TODO: Is the call's type the return value's type?
            call.type,
            call,
        )
}

/** Tries to infer a [MethodDeclaration] from a [CallExpression]. */
fun RecordDeclaration.inferMethod(
    call: CallExpression,
    isStatic: Boolean = false,
    ctx: TranslationContext,
): MethodDeclaration? {
    return startInference(ctx)
        ?.inferFunctionDeclaration(
            call.name.localName,
            call.code,
            isStatic,
            call.signature,
            // TODO: Is the call's type the return value's type?
            call.type,
            call,
        ) as? MethodDeclaration
}

/** A small helper function that prefers [IntegerType] when comparing two [NumericType] types. */
fun preferIntegerType(a: NumericType, b: NumericType): Int {
    return when {
        a is IntegerType && b is IntegerType -> 0
        a is IntegerType && b !is IntegerType -> 1
        else -> -1
    }
}
