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

import de.fraunhofer.aisec.cpg.graph.SubGraph

/**
 * Represents a key / value pair, often found in languages that allow associative arrays or objects,
 * such as Python, Golang or JavaScript.
 *
 * Most often used in combination with an
 * [de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression] to represent the
 * creation of an array.
 */
class KeyValueExpression : Expression() {

    /**
     * The key of this pair. It is usually a literal, but some languages even allow references to
     * variables as a key.
     */
    @field:SubGraph("AST") var key: Expression? = null

    /** The value of this pair. It can be any expression */
    @field:SubGraph("AST")
    var value: Expression? = null
        set(value) {
            if (this.value != null) {
                this.removePrevDFG(this.value)
            }
            value?.let { this.addPrevDFG(value) }
            field = value
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is KeyValueExpression) {
            return false
        }

        return super.equals(other) && name == other.name && value == other.value
    }
}
