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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.neo4j.ogm.annotation.Relationship

/**
 * This expression denotes the usage of an anonymous / lambda function. It connects the inner
 * anonymous function to the user of a lambda function with an expression.
 */
class LambdaExpression : Expression(), HasType.TypeObserver {

    /**
     * If [areVariablesMutable] is false, only the (outer) variables in this list can be modified
     * inside the lambda.
     */
    val mutableVariables: MutableList<ValueDeclaration> = mutableListOf()

    /** Determines if we can modify variables declared outside the lambda from inside the lambda */
    var areVariablesMutable: Boolean = true

    @Relationship("FUNCTION")
    var functionEdge =
        astOptionalEdgeOf<Function>(onChanged = ::exchangeTypeObserverWithAccessPropagation)
    var function by unwrapping(LambdaExpression::functionEdge)

    override fun typeChanged(newType: Type, src: HasType) {
        // Make sure our src is the function
        if (src != function) {
            return
        }

        // We should only propagate a function type, coming from our declared function
        if (newType is FunctionType) {
            // TODO(oxisto): We should discuss at some point, whether we should actually return
            //  a FunctionType instead of a FunctionPointerType
            // Propagate a pointer reference to the function
            this.type = newType.pointer()
        }
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // Nothing to do
    }
}
