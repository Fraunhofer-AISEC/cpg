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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.TypeExpression
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.graph.types.apply

/**
 * Adds the resolved default template arguments recursively to the templateParameter list of the
 * ConstructExpression until a fixpoint is reached e.g. template&lt;class Type1, class Type2 =
 * Type1&gt;
 *
 * @param constructExpression
 * @param template
 */
fun SymbolResolver.addRecursiveDefaultTemplateArgs(
    constructExpression: ConstructExpression,
    template: RecordTemplateDeclaration,
) {
    var templateParameters: Int
    do {
        // Handle Explicit Template Arguments
        templateParameters = constructExpression.templateArguments.size
        val templateParametersExplicitInitialization = mutableMapOf<Node, AstNode>()
        handleExplicitTemplateParameters(
            constructExpression,
            template,
            templateParametersExplicitInitialization,
        )
        val templateParameterRealDefaultInitialization = mutableMapOf<Node, AstNode?>()

        // Handle defaults of parameters
        handleDefaultTemplateParameters(template, templateParameterRealDefaultInitialization)

        // Add defaults to ConstructDeclaration
        applyMissingParams(
            template,
            constructExpression,
            templateParametersExplicitInitialization,
            templateParameterRealDefaultInitialization,
        )
    } while (templateParameters != constructExpression.templateArguments.size)
}

/**
 * Matches declared template arguments to the explicit instantiation
 *
 * @param constructExpression containing the explicit instantiation
 * @param template containing declared template arguments
 * @param templateParametersExplicitInitialization mapping of the template parameter to the explicit
 *   instantiation
 */
fun handleExplicitTemplateParameters(
    constructExpression: ConstructExpression,
    template: RecordTemplateDeclaration,
    templateParametersExplicitInitialization: MutableMap<Node, AstNode>,
) {
    for (i in constructExpression.templateArguments.indices) {
        val explicit = constructExpression.templateArguments[i]
        if (template.parameters[i] is TypeParameterDeclaration) {
            templateParametersExplicitInitialization[
                (template.parameters[i] as TypeParameterDeclaration).type] = explicit
        } else if (template.parameters[i] is ParameterDeclaration) {
            templateParametersExplicitInitialization[template.parameters[i]] = explicit
        }
    }
}

/**
 * Apply missingParameters (either explicit or defaults) to the ConstructExpression and its type
 *
 * @param template Template which is instantiated by the ConstructExpression
 * @param constructExpression
 * @param templateParametersExplicitInitialization mapping of the template parameter to the explicit
 *   instantiation
 * @param templateParameterRealDefaultInitialization mapping of template parameter to its real
 *   default (no recursive)
 */
fun SymbolResolver.applyMissingParams(
    template: RecordTemplateDeclaration,
    constructExpression: ConstructExpression,
    templateParametersExplicitInitialization: Map<Node, AstNode>,
    templateParameterRealDefaultInitialization: Map<Node, AstNode?>,
) {
    with(constructExpression) {
        val missingParams =
            template.parameterDefaults.subList(
                constructExpression.templateArguments.size,
                template.parameterDefaults.size,
            )
        for (m in missingParams) {
            var missingParam: Node? = m
            if (missingParam is Reference) {
                if (missingParam.refersTo == null) {
                    val currentScope = scopeManager.currentScope
                    scopeManager.jumpTo(missingParam.scope)
                    missingParam.refersTo =
                        scopeManager.lookupSymbolByNodeName(missingParam).singleOrNull()
                    scopeManager.jumpTo(currentScope)
                }
                missingParam = missingParam.refersTo
            } else if (missingParam is TypeExpression) {
                // If the missing parameter is a TypeExpression, we need to get the type
                missingParam = missingParam.type
            }

            if (missingParam in templateParametersExplicitInitialization) {
                // If default is a previously defined template argument that has been explicitly
                // passed
                templateParametersExplicitInitialization[missingParam]?.let {
                    constructExpression.addTemplateParameter(
                        it,
                        TemplateDeclaration.TemplateInitialization.DEFAULT,
                    )
                }

                // If template argument is a type add it as a generic to the type as well
                (templateParametersExplicitInitialization[missingParam] as? TypeExpression)
                    ?.type
                    ?.let {
                        val type = constructExpression.type
                        if (type is ObjectType) {
                            constructExpression.type =
                                objectType(type.name, listOf(it, *type.generics.toTypedArray()))
                        }
                    }
            } else if (missingParam in templateParameterRealDefaultInitialization) {
                // Add default of template parameter to construct declaration
                templateParameterRealDefaultInitialization[missingParam]?.let {
                    constructExpression.addTemplateParameter(
                        it,
                        TemplateDeclaration.TemplateInitialization.DEFAULT,
                    )
                }
                (templateParametersExplicitInitialization[missingParam] as? TypeExpression)
                    ?.type
                    ?.let {
                        constructExpression.type =
                            objectType(constructExpression.type.name, listOf(it))
                    }
            }
        }
    }
}

/**
 * Matches declared template arguments to their defaults (without defaults of a previously defined
 * template argument)
 *
 * @param template containing template arguments
 * @param templateParameterRealDefaultInitialization mapping of template parameter to its real
 *   default (no recursive)
 */
fun handleDefaultTemplateParameters(
    template: RecordTemplateDeclaration,
    templateParameterRealDefaultInitialization: MutableMap<Node, AstNode?>,
) {
    val declaredTemplateTypes = mutableListOf<Type?>()
    val declaredNonTypeTemplate = mutableListOf<ParameterDeclaration>()
    val parametersWithDefaults = template.parametersWithDefaults
    for (declaration in template.parameters) {
        if (declaration is TypeParameterDeclaration) {
            declaredTemplateTypes.add(declaration.type)
            if (
                declaration.default?.type !in declaredTemplateTypes &&
                    declaration in parametersWithDefaults
            ) {
                templateParameterRealDefaultInitialization[declaration.type] = declaration.default
            }
        } else if (declaration is ParameterDeclaration) {
            declaredNonTypeTemplate.add(declaration)
            if (
                declaration in parametersWithDefaults &&
                    (declaration.default !is Reference ||
                        (declaration.default as Reference?)?.refersTo !in declaredNonTypeTemplate)
            ) {
                templateParameterRealDefaultInitialization[declaration] = declaration.default
            }
        }
    }
}

/**
 * This function "realizes" a type that is based on a [ParameterizedType] (a template type), based
 * on the [initializationSignature]. Basically the [incomingType] is either directly a
 * [ParameterizedType], e.g. `T` or a derived type, such as a [PointerType] (e.g. `T*`). If the
 * [initializationSignature] specifies that `T` is initialized with a [TypeExpression] pointing to
 * `int`, we will return a type representing `int` in the first example and `int*` the second
 * example.
 */
internal fun realizeType(
    language: Language<*>?,
    parameterizedTypeResolution: Map<ParameterizedType, TypeParameterDeclaration>,
    incomingType: Type,
    initializationSignature: Map<Declaration?, Node?>,
): Type {
    var type: Type = UnknownType.getUnknownType(language)

    // The root type of our incoming type should be a ParameterizedType. We need to find its
    // matching TypeParameterDeclaration, to find out how the parameter is initialized.
    val typeParamDeclaration = parameterizedTypeResolution[incomingType.root]
    if (typeParamDeclaration != null) {
        val node = initializationSignature[typeParamDeclaration]
        if (node is TypeExpression) {
            // We might need basically exchange the root node, and we can do this using type
            // operations
            val operations = incomingType.typeOperations
            val newType = operations.apply(node.type)

            type = newType
        }
    }
    return type
}
