/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.*

/**
 * Represents a [CallExpression] to an [Expression], which is a member of an object. For example
 * `obj.toString()`. The type of the [callee] property should be a [MemberExpression] (unless a
 * translation error occurred).
 */
class MemberCallExpression : CallExpression(), HasBase {
    /**
     * The base object. Be aware that for simple calls the implicit "this" base is not part of the
     * original AST, but we treat it as such for better consistency
     */
    @field:SubGraph("AST")
    override var base: Expression? = null
        set(value) {
            field?.unregisterTypeListener(this)
            field = value
            value?.registerTypeListener(this)
        }

    var operatorCode: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MemberCallExpression) {
            return false
        }

        return super.equals(other) && base == other.base
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), base)
    }

    override fun typeChanged(src: HasType, root: List<HasType>, oldType: Type) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }

        if (src === base) {
            // update the name
            updateName()
        } else {
            super.typeChanged(src, root, oldType)
        }
    }

    override fun possibleSubTypesChanged(src: HasType, root: List<HasType>) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        if (src !== base) {
            super.possibleSubTypesChanged(src, root)
        }
    }
}
