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

import de.fraunhofer.aisec.cpg.frontends.HasClasses
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.TypeExpression
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
 * example, to infer a [MethodDeclaration], the obvious start point would be a [RecordDeclaration].
 * Since this class implements [IsInferredProvider], all nodes that are created using the node
 * builder functions, will automatically have [Node.isInferred] set to true.
 */
class Inference(val start: Node) : LanguageProvider, IsInferredProvider {
    val log: Logger = LoggerFactory.getLogger(Inference::class.java)

    override val language: Language<out LanguageFrontend>?
        get() = start.language

    fun createInferredFunctionDeclaration(
        name: String?,
        code: String?,
        isStatic: Boolean,
        signature: List<Type?>,
        returnType: Type?,
    ): FunctionDeclaration {
        // We assume that the start is either a record or the translation unit
        val record = start as? RecordDeclaration
        val tu = start as? TranslationUnitDeclaration

        // If both are null, we have the wrong type
        if (record == null && tu == null) {
            throw UnsupportedOperationException(
                "Starting inference with the wrong type of start node"
            )
        }

        log.debug(
            "Inferring a new function declaration $name with parameter types ${signature.map { it?.fullName }}"
        )

        if (
            record?.isInferred == true && record.kind == "struct" && record.language is HasClasses
        ) {
            // "upgrade" our struct to a class, if it was inferred by us, since we are calling
            // methods on it
            record.kind = "class"
        }

        val declarationHolder = (record ?: tu)
        val parameters = createInferredParameters(signature)
        val inferred: FunctionDeclaration =
            if (record != null) {
                newMethodDeclaration(name ?: "", code, isStatic, record)
            } else {
                newFunctionDeclaration(name ?: "", code)
            }
        inferred.parameters = parameters

        // TODO: Once, we used inferred.type = returnType and once the two following statements:
        // Why? What's the "right way"?
        returnType?.let { inferred.returnTypes = listOf(it) }
        inferred.type = returnType

        // TODO: Handle multiple return values?
        if (declarationHolder is RecordDeclaration) {
            declarationHolder.addMethod(inferred as MethodDeclaration)
            if (isStatic) {
                declarationHolder.staticImports.add(inferred)
            }
        } else {
            declarationHolder?.addDeclaration(inferred)
        }

        return inferred
    }

    fun createInferredConstructor(signature: List<Type?>): ConstructorDeclaration {
        val inferred =
            newConstructorDeclaration(
                start.fullName.localName,
                "",
                start as? RecordDeclaration,
            )
        inferred.parameters = createInferredParameters(signature)

        (start as? RecordDeclaration)?.addConstructor(inferred)
        return inferred
    }

    fun createInferredParameters(signature: List<Type?>): List<ParamVariableDeclaration> {
        val params: MutableList<ParamVariableDeclaration> = ArrayList()
        for (i in signature.indices) {
            val targetType = signature[i]
            val paramName = generateParamName(i, targetType!!)
            val param = newParamVariableDeclaration(paramName, targetType, false, "")
            param.argumentIndex = i
            params.add(param)
        }
        return params
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
        while (!hierarchy.isEmpty()) {
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

    fun inferNonTypeTemplateParameter(name: String): ParamVariableDeclaration {
        val expr =
            start as? Expression
                ?: throw UnsupportedOperationException(
                    "Starting inference with the wrong type of start node"
                )

        // Non-Type Template Parameter
        return newParamVariableDeclaration(name, expr.type, false, name)
    }

    fun inferTemplateParameter(
        name: String,
    ): TypeParamDeclaration {
        val parameterizedType = ParameterizedType(name, language)
        TypeManager.getInstance()
            .addTypeParameter(start as? FunctionTemplateDeclaration, parameterizedType)

        val decl = newTypeParamDeclaration(name, name)
        decl.type = parameterizedType

        return decl
    }

    /**
     * Create an inferred FunctionTemplateDeclaration if a call to an FunctionTemplate could not be
     * resolved
     *
     * @param containingRecord
     * @param call
     * @return inferred FunctionTemplateDeclaration which can be invoked by the call
     */
    fun createInferredFunctionTemplate(call: CallExpression): FunctionTemplateDeclaration {
        // We assume that the start is either a record or the translation unit
        val record = start as? RecordDeclaration
        val tu = start as? TranslationUnitDeclaration

        // If both are null, we have the wrong type
        if (record == null && tu == null) {
            throw UnsupportedOperationException(
                "Starting inference with the wrong type of start node"
            )
        }

        val name = call.fullName.localName
        val code = call.code
        val inferred = newFunctionTemplateDeclaration(name, code)
        inferred.isInferred = true

        val inferredRealization: FunctionDeclaration =
            if (record != null) {
                record.addDeclaration(inferred)
                record.inferMethod(call)
            } else {
                tu!!.addDeclaration(inferred)
                tu.inferFunction(call)
            }

        inferred.addRealization(inferredRealization)

        var typeCounter = 0
        var nonTypeCounter = 0
        for (node in call.templateParameters) {
            if (node is TypeExpression) {
                // Template Parameter
                val inferredTypeIdentifier = "T$typeCounter"
                val typeParamDeclaration =
                    inferred.startInference().inferTemplateParameter(inferredTypeIdentifier)
                typeCounter++
                inferred.addParameter(typeParamDeclaration)
            } else if (node is Expression) {
                val inferredNonTypeIdentifier = "N$nonTypeCounter"
                val paramVariableDeclaration =
                    node.startInference().inferNonTypeTemplateParameter(inferredNonTypeIdentifier)

                paramVariableDeclaration.addPrevDFG(node)
                node.addNextDFG(paramVariableDeclaration)
                nonTypeCounter++
                inferred.addParameter(paramVariableDeclaration)
            }
        }
        return inferred
    }

    /**
     * Infers a record declaration for the given type. [type] is the object type representing a
     * record that we want to infer, the [recordToUpdate] is either the type's name or the type's
     * root name. The [kind] specifies if we create a class or a struct.
     */
    fun inferRecordDeclaration(
        type: Type,
        currentTU: TranslationUnitDeclaration,
        kind: String = "class"
    ): RecordDeclaration? {
        if (type !is ObjectType) {
            log.error(
                "Trying to infer a record declaration of a non-object type. Not sure what to do? Should we change the type?"
            )
            return null
        }
        log.debug(
            "Encountered an unknown record type ${type.typeName} during a call. We are going to infer that record"
        )

        // This could be a class or a struct. We start with a class and may have to fine-tune this
        // later.
        val declaration = currentTU.newRecordDeclaration(type.typeName, kind, "")
        declaration.isInferred = true

        // update the type
        type.recordDeclaration = declaration

        // add this record declaration to the current TU (this bypasses the scope manager)
        currentTU.addDeclaration(declaration)
        return declaration
    }

    override val isInferred: Boolean
        get() = true
}

/** Provides information about the inference status of a node. */
interface IsInferredProvider : MetadataProvider {
    val isInferred: Boolean
}

/** Returns a new [Inference] object starting from this node. */
fun Node.startInference() = Inference(this)

/** Tries to infer a [FunctionDeclaration] from a [CallExpression]. */
fun TranslationUnitDeclaration.inferFunction(
    call: CallExpression,
    isStatic: Boolean = false
): FunctionDeclaration {
    return Inference(this)
        .createInferredFunctionDeclaration(
            call.fullName.localName,
            call.code,
            isStatic,
            call.signature,
            // TODO: Is the call's type the return value's type?
            call.type
        )
}

/** Tries to infer a [MethodDeclaration] from a [CallExpression]. */
fun RecordDeclaration.inferMethod(
    call: CallExpression,
    isStatic: Boolean = false
): MethodDeclaration {
    return Inference(this)
        .createInferredFunctionDeclaration(
            call.fullName.localName,
            call.code,
            isStatic,
            call.signature,
            // TODO: Is the call's type the return value's type?
            call.type
        ) as MethodDeclaration
}
