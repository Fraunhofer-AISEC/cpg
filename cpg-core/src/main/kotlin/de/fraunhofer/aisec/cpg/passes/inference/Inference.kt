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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.HasClasses
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpr
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.TypeExpr
import de.fraunhofer.aisec.cpg.graph.types.*
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class contains different kinds of helper that *infer* certain [Node]s that are not present
 * in the source code but that we assure are present in the overall program (e.g., if because we
 * only see parts of it).
 *
 * Each inference has a certain [start] point, which might depend on the type of inference. For
 * example, to infer a [MethodDecl], the obvious start point would be a [RecordDecl]. Since this
 * class implements [IsInferredProvider], all nodes that are created using the node builder
 * functions, will automatically have [Node.isInferred] set to true.
 */
class Inference(val start: Node, override val ctx: TranslationContext) :
    LanguageProvider, ScopeProvider, IsInferredProvider, ContextProvider {

    override val language: Language<*>?
        get() = start.language

    override val isInferred: Boolean
        get() = true

    val scopeManager = ctx.scopeManager
    val typeManager = ctx.typeManager

    override val scope: Scope?
        get() = scopeManager.currentScope

    fun createInferredFunctionDeclaration(
        name: CharSequence?,
        code: String?,
        isStatic: Boolean,
        signature: List<Type?>,
        returnType: Type?,
    ): FunctionDecl {
        // We assume that the start is either a record, a namespace or the translation unit
        val record = start as? RecordDecl
        val namespace = start as? NamespaceDecl
        val tu = start as? TranslationUnitDecl

        // If all are null, we have the wrong type
        if (record == null && namespace == null && tu == null) {
            throw UnsupportedOperationException(
                "Starting inference with the wrong type of start node"
            )
        }

        return inferInScopeOf(start) {
            val inferred: FunctionDecl =
                if (record != null) {
                    newMethodDecl(name ?: "", code, isStatic, record)
                } else {
                    newFunctionDecl(name ?: "", code)
                }

            Companion.log.debug(
                "Inferred a new {} declaration {} with parameter types {}",
                if (inferred is MethodDecl) "method" else "function",
                inferred.name,
                signature.map { it?.name }
            )

            createInferredParameters(inferred, signature)

            // Set the type and return type(s)
            returnType?.let { inferred.returnTypes = listOf(it) }
            inferred.type = FunctionType.computeType(inferred)

            // Add it to the scope
            scopeManager.addDeclaration(inferred)

            // Some magic that adds it to static imports. Not sure if this really needed

            if (record != null) {
                if (isStatic) {
                    record.staticImports.add(inferred)
                }
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

    fun createInferredConstructor(signature: List<Type?>): ConstructorDecl {
        return inferInScopeOf(start) {
            val inferred =
                newConstructorDecl(
                    start.name.localName,
                    "",
                    start as? RecordDecl,
                )
            createInferredParameters(inferred, signature)

            scopeManager.addDeclaration(inferred)

            inferred
        }
    }

    /**
     * This wrapper should be used around any kind of inference code that actually creates a
     * [Declaration]. It takes cares of "jumping" to the appropriate scope of the [start] node,
     * executing the commands in [init] (which needs to create an inferred node of [T]) as well as
     * restoring the previous scope.
     */
    private fun <T : Declaration> inferInScopeOf(start: Node, init: () -> T): T {
        return scopeManager.withScope(scopeManager.lookupScope(start), init)
    }

    private fun createInferredParameters(function: FunctionDecl, signature: List<Type?>) {
        // To save some unnecessary scopes, we only want to "enter" the function if it is necessary,
        // e.g., if we need to create parameters
        if (signature.isNotEmpty()) {
            scopeManager.enterScope(function)

            for (i in signature.indices) {
                val targetType = signature[i] ?: UnknownType.getUnknownType(function.language)
                val paramName = generateParamName(i, targetType)
                val param = newParameterDecl(paramName, targetType, false, "")
                param.argumentIndex = i

                scopeManager.addDeclaration(param)
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

    private fun inferNonTypeTemplateParameter(name: String): ParameterDecl {
        val expr =
            start as? Expression
                ?: throw UnsupportedOperationException(
                    "Starting inference with the wrong type of start node"
                )

        // Non-Type Template Parameter
        return newParameterDecl(name, expr.type, false, name)
    }

    private fun inferTemplateParameter(
        name: String,
    ): TypeParamDecl {
        val parameterizedType = ParameterizedType(name, language)
        typeManager.addTypeParameter(start as FunctionTemplateDecl, parameterizedType)

        val decl = newTypeParamDecl(name, name)
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
    fun createInferredFunctionTemplate(call: CallExpr): FunctionTemplateDecl {
        // We assume that the start is either a record or the translation unit
        val record = start as? RecordDecl
        val tu = start as? TranslationUnitDecl

        // If both are null, we have the wrong type
        if (record == null && tu == null) {
            throw UnsupportedOperationException(
                "Starting inference with the wrong type of start node"
            )
        }

        val name = call.name.localName
        val code = call.code
        val inferred = newFunctionTemplateDecl(name, code)
        inferred.isInferred = true

        val inferredRealization =
            if (record != null) {
                record.addDeclaration(inferred)
                record.inferMethod(call, ctx = ctx)
            } else {
                tu?.addDeclaration(inferred)
                tu?.inferFunction(call, ctx = ctx)
            }

        inferredRealization?.let { inferred.addRealization(it) }

        var typeCounter = 0
        var nonTypeCounter = 0
        for (node in call.templateParameters) {
            if (node is TypeExpr) {
                // Template Parameter
                val inferredTypeIdentifier = "T$typeCounter"
                val typeParamDeclaration =
                    inferred.startInference(ctx).inferTemplateParameter(inferredTypeIdentifier)
                typeCounter++
                inferred.addParameter(typeParamDeclaration)
            } else if (node is Expression) {
                val inferredNonTypeIdentifier = "N$nonTypeCounter"
                val paramVariableDeclaration =
                    node
                        .startInference(ctx)
                        .inferNonTypeTemplateParameter(inferredNonTypeIdentifier)
                node.addNextDFG(paramVariableDeclaration)
                nonTypeCounter++
                inferred.addParameter(paramVariableDeclaration)
            }
        }
        return inferred
    }

    /**
     * Infers a record declaration for the given type. [type] is the object type representing a
     * record that we want to infer. The [kind] specifies if we create a class or a struct.
     */
    fun inferRecordDeclaration(
        type: Type,
        currentTU: TranslationUnitDecl,
        kind: String = "class"
    ): RecordDecl? {
        if (type !is ObjectType) {
            Companion.log.error(
                "Trying to infer a record declaration of a non-object type. Not sure what to do? Should we change the type?"
            )
            return null
        }
        Companion.log.debug(
            "Encountered an unknown record type ${type.typeName} during a call. We are going to infer that record"
        )

        // This could be a class or a struct. We start with a class and may have to fine-tune this
        // later.
        val declaration = currentTU.newRecordDecl(type.typeName, kind, "")
        declaration.isInferred = true

        // update the type
        type.recordDecl = declaration

        // add this record declaration to the current TU (this bypasses the scope manager)
        currentTU.addDeclaration(declaration)
        return declaration
    }

    fun createInferredNamespaceDeclaration(name: Name, path: String?): NamespaceDecl {
        // Here be dragons. Jump to the scope that the node defines directly, so that we can
        // delegate further operations to the scope manager. We also save the old scope so we can
        // restore it.
        return inferInScopeOf(start) {
            Companion.log.debug(
                "Inferring a new namespace declaration {} {}",
                name,
                if (path != null) {
                    "with path '$path'"
                } else {
                    ""
                }
            )

            val inferred = newNamespaceDecl(name)
            inferred.path = path

            scopeManager.addDeclaration(inferred)

            // We need to "enter" the scope to make it known to the scope map of the ScopeManager
            scopeManager.enterScope(inferred)
            scopeManager.leaveScope(inferred)
            inferred
        }
    }

    /**
     * This class implements a [HasType.TypeObserver] and uses the observed type to set the
     * [ValueDecl.declaredType] of a [ValueDecl], based on the types we see. It can be registered on
     * objects that are used to "start" an inference, for example a [MemberExpression], which infers
     * a [FieldDecl]. Once the type of the member expression becomes known, we can use this
     * information to set the type of the field.
     *
     * For now, this implementation uses the first type that we "see" and once the type of our
     * [declaration] is known, we ignore further updates. In a future implementation, we could try
     * to fine-tune this, e.g. by finding a common type (such as an interface) that is more
     * probable, if multiple types are assigned.
     */
    class TypeInferenceObserver(var declaration: ValueDecl) : HasType.TypeObserver {
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
                        type.name
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
}

/** Provides information about the inference status of a node. */
interface IsInferredProvider : MetadataProvider {
    val isInferred: Boolean
}

/** Returns a new [Inference] object starting from this node. */
fun Node.startInference(ctx: TranslationContext) = Inference(this, ctx)

/** Tries to infer a [FunctionDecl] from a [CallExpr]. */
fun TranslationUnitDecl.inferFunction(
    call: CallExpr,
    isStatic: Boolean = false,
    ctx: TranslationContext
): FunctionDecl {
    return Inference(this, ctx)
        .createInferredFunctionDeclaration(
            call.name.localName,
            call.code,
            isStatic,
            call.signature,
            // TODO: Is the call's type the return value's type?
            call.type
        )
}

/** Tries to infer a [FunctionDecl] from a [CallExpr]. */
fun NamespaceDecl.inferFunction(
    call: CallExpr,
    isStatic: Boolean = false,
    ctx: TranslationContext
): FunctionDecl {
    return Inference(this, ctx)
        .createInferredFunctionDeclaration(
            call.name,
            call.code,
            isStatic,
            call.signature,
            // TODO: Is the call's type the return value's type?
            call.type
        )
}

/** Tries to infer a [MethodDecl] from a [CallExpr]. */
fun RecordDecl.inferMethod(
    call: CallExpr,
    isStatic: Boolean = false,
    ctx: TranslationContext
): MethodDecl {
    return Inference(this, ctx)
        .createInferredFunctionDeclaration(
            call.name.localName,
            call.code,
            isStatic,
            call.signature,
            // TODO: Is the call's type the return value's type?
            call.type
        ) as MethodDecl
}
