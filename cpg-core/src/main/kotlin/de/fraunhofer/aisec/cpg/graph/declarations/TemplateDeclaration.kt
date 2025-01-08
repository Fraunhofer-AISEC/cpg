/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship

/** Abstract class representing the template concept */
@NodeEntity
abstract class TemplateDeclaration : Declaration(), DeclarationHolder {
    enum class TemplateInitialization {
        /**
         * Template Parameter is deduced automatically due to matching type provided in the function
         * signature
         */
        AUTO_DEDUCTION,
        /** Template Parameter uses the default value for instantiation */
        DEFAULT,
        /** Template Parameter is provided explicitly */
        EXPLICIT,
        UNKNOWN,
    }

    /** Parameters the Template requires for instantiation */
    @Relationship(value = "PARAMETERS", direction = Relationship.Direction.OUTGOING)
    var parameterEdges = astEdgesOf<Declaration>()
    val parameters by unwrapping(TemplateDeclaration::parameterEdges)

    val parametersWithDefaults: List<Declaration>
        get() {
            val parametersWithDefaults: MutableList<Declaration> = ArrayList()
            for (declaration in parameters) {
                if (
                    (declaration is TypeParameterDeclaration && declaration.default != null) ||
                        (declaration is ParameterDeclaration && declaration.default != null)
                ) {
                    parametersWithDefaults.add(declaration)
                }
            }
            return parametersWithDefaults
        }

    val parameterDefaults: List<Node?>
        get() {
            val defaults: MutableList<Node?> = ArrayList()
            for (declaration in parameters) {
                if (declaration is TypeParameterDeclaration) {
                    defaults.add(declaration.default)
                } else if (declaration is ParameterDeclaration) {
                    defaults.add(declaration.default)
                }
            }
            return defaults
        }

    @DoNotPersist
    override val declarations: List<Declaration>
        get() {
            val list = ArrayList<Declaration>()
            list.addAll(realizations)
            return list
        }

    fun removeParameter(parameterizedType: TypeParameterDeclaration?) {
        parameterEdges.removeIf { it.end == parameterizedType }
    }

    fun removeParameter(nonTypeTemplateParamDeclaration: ParameterDeclaration?) {
        parameterEdges.removeIf { it.end == nonTypeTemplateParamDeclaration }
    }

    abstract val realizations: List<Declaration>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as TemplateDeclaration
        return parameters == that.parameters &&
            propertyEqualsList(parameterEdges, that.parameterEdges)
    }

    // We can't add anything else here
    override fun hashCode() = super.hashCode()
}
