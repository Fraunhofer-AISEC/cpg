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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordTemplateDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TypeParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.TypeExpression
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * Adds the resolved default template arguments recursively to the templateParameter list of the
 * ConstructExpression until a fixpoint is reached e.g. template&lt;class Type1, class Type2 =
 * Type1&gt;
 *
 * @param constructExpr
 * @param template
 */
fun addRecursiveDefaultTemplateArgs(
    constructExpr: ConstructExpression,
    template: RecordTemplateDeclaration
) {
    var templateParameters: Int
    do {
        // Handle Explicit Template Arguments
        templateParameters = constructExpr.templateParameters.size
        val templateParametersExplicitInitialization = mutableMapOf<Node, Node>()
        handleExplicitTemplateParameters(
            constructExpr,
            template,
            templateParametersExplicitInitialization
        )
        val templateParameterRealDefaultInitialization = mutableMapOf<Node, Node?>()

        // Handle defaults of parameters
        handleDefaultTemplateParameters(template, templateParameterRealDefaultInitialization)

        // Add defaults to ConstructDeclaration
        applyMissingParams(
            template,
            constructExpr,
            templateParametersExplicitInitialization,
            templateParameterRealDefaultInitialization
        )
    } while (templateParameters != constructExpr.templateParameters.size)
}

/**
 * Matches declared template arguments to the explicit instantiation
 *
 * @param constructExpr containing the explicit instantiation
 * @param template containing declared template arguments
 * @param templateParametersExplicitInitialization mapping of the template parameter to the explicit
 *   instantiation
 */
fun handleExplicitTemplateParameters(
    constructExpr: ConstructExpression,
    template: RecordTemplateDeclaration,
    templateParametersExplicitInitialization: MutableMap<Node, Node>
) {
    for (i in constructExpr.templateParameters.indices) {
        val explicit = constructExpr.templateParameters[i]
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
 * @param constructExpr
 * @param templateParametersExplicitInitialization mapping of the template parameter to the explicit
 *   instantiation
 * @param templateParameterRealDefaultInitialization mapping of template parameter to its real
 *   default (no recursive)
 */
fun applyMissingParams(
    template: RecordTemplateDeclaration,
    constructExpr: ConstructExpression,
    templateParametersExplicitInitialization: Map<Node, Node>,
    templateParameterRealDefaultInitialization: Map<Node, Node?>
) {
    val missingParams: List<Node?> =
        template.parameterDefaults.subList(
            constructExpr.templateParameters.size,
            template.parameterDefaults.size
        )
    for (m in missingParams) {
        var missingParam = m
        if (missingParam is Reference) {
            missingParam = missingParam.refersTo
        }
        if (missingParam in templateParametersExplicitInitialization) {
            // If default is a previously defined template argument that has been explicitly
            // passed
            templateParametersExplicitInitialization[missingParam]?.let {
                constructExpr.addTemplateParameter(
                    it,
                    TemplateDeclaration.TemplateInitialization.DEFAULT
                )
            }
            // If template argument is a type add it as a generic to the type as well
            (templateParametersExplicitInitialization[missingParam] as? TypeExpression)?.type?.let {
                (constructExpr.type as ObjectType).addGeneric(it)
            }
        } else if (missingParam in templateParameterRealDefaultInitialization) {
            // Add default of template parameter to construct declaration
            templateParameterRealDefaultInitialization[missingParam]?.let {
                constructExpr.addTemplateParameter(
                    it,
                    TemplateDeclaration.TemplateInitialization.DEFAULT
                )
            }
            (templateParametersExplicitInitialization[missingParam] as? TypeExpression)?.type?.let {
                (constructExpr.type as ObjectType).addGeneric(it)
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
    templateParameterRealDefaultInitialization: MutableMap<Node, Node?>
) {
    val declaredTemplateTypes = mutableListOf<Type?>()
    val declaredNonTypeTemplate = mutableListOf<ParameterDeclaration>()
    val parametersWithDefaults = template.parametersWithDefaults
    for (declaration in template.parameters) {
        if (declaration is TypeParameterDeclaration) {
            declaredTemplateTypes.add(declaration.type)
            if (
                declaration.default !in declaredTemplateTypes &&
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
