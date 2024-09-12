/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.HasBase
import de.fraunhofer.aisec.cpg.graph.HasOperatorCode
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.OperatorDeclaration

/**
 * This special call expression is used when an operator (such as a [BinaryOperator]) is overloaded.
 * In this case, we replace the original [BinaryOperator] with an [OperatorCallExpression], which
 * points to its respective [OperatorDeclaration].
 */
class OperatorCallExpression : CallExpression(), HasOperatorCode, HasBase {

    override var operatorCode: String? = null

    override var name: Name
        get() = Name(operatorCode ?: "")
        set(_) {
            // read-only
        }

    /**
     * The base object. This is basically a shortcut to accessing the base of the [callee], if it
     * has one (i.e., if it implements [HasBase]). This is the case for example, if it is a
     * [MemberExpression].
     */
    override val base: Expression?
        get() {
            return (callee as? HasBase)?.base
        }
}
