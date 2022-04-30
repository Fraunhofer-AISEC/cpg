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

/**
 * Represents a [CallExpression] to a function, which is a member of an object. For example
 * `obj.toString()`. The type of the [callee] property is always a [MemberExpression] (unless a
 * translation error occurred).
 */
class MemberCallExpression : CallExpression(), HasBase {
    val operatorCode: String?
        get() {
            return (callee as? MemberCallExpression)?.operatorCode
        }

    override val base: Expression?
        get() = (callee as? MemberExpression)?.base

    override fun typeChanged(src: HasType, root: List<HasType>, oldType: Type) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        /*if (src === base) {
            fqn = src.getType().root.typeName + "." + name
        }*/
        super.typeChanged(src, root, oldType)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MemberCallExpression) {
            return false
        }
        if (!super.equals(other)) {
            return false
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
