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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression

/**
 * Specifies that a certain node has an initializer. It is a special case of [ArgumentHolder], in
 * which the initializer is treated as the first (and only) argument.
 */
interface HasInitializer : HasType, ArgumentHolder, AssignmentHolder {

    var initializer: Expression?

    override fun addArgument(expression: Expression) {
        this.initializer = expression
    }

    override fun removeArgument(expression: Expression): Boolean {
        return if (this.initializer == expression) {
            this.initializer = null
            true
        } else {
            false
        }
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        this.initializer = new
        return true
    }

    override val assignments: List<Assignment>
        get() {
            return initializer?.let { listOf(Assignment(it, this, this)) } ?: listOf()
        }
}
