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

import de.fraunhofer.aisec.cpg.graph.AST
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration

/**
 * This expression denotes the usage of an anonymous / lambda function. It connects the inner
 * anonymous function to the user of a lambda function with an expression.
 */
class LambdaExpression : Expression() {

    /**
     * If [areVariablesMutable] is false, only the (outer) variables in this list can be modified
     * inside the lambda.
     */
    val mutableVariables: MutableList<ValueDeclaration> = mutableListOf()

    /** Determines if we can modify variables declared outside the lambda from inside the lambda */
    var areVariablesMutable: Boolean = true

    @AST
    var function: FunctionDeclaration? = null
        set(value) {
            if (value != null) {}
            field = value
        }
}
