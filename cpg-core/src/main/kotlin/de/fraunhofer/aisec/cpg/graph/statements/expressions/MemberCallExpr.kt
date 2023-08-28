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
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDecl
import java.util.*

/**
 * Represents a [CallExpr] to something which is a member of an object (the [base]). For example
 * `obj.toString()`. The type of the [callee] property should be a [MemberExpr] (unless a
 * translation error occurred). One notable exception are function pointer calls to class methods in
 * C++, in which the callee is a [BinaryOp] with a `.*` operator.
 *
 * While this node implements [HasBase], this is basically just a shortcut to access the base of the
 * underlying [callee] property, if appropriate.
 */
class MemberCallExpr : CallExpr(), HasBase, HasOperatorCode {
    /**
     * The base object. This is basically a shortcut to accessing the base of the [callee], if it
     * has one (i.e., if it implements [HasBase]). This is the case for example, if it is a
     * [MemberExpr].
     */
    override val base: Expression?
        get() {
            return (callee as? HasBase)?.base
        }

    /**
     * The operator code to access the base object. This is basically a shortcut to accessing the
     * base of the [callee], if it has one (i.e., if it implements [HasBase]). This is the case for
     * example, if it is a [MemberExpr].
     */
    override val operatorCode: String?
        get() {
            return (callee as? HasBase)?.operatorCode
        }

    /**
     * Needs to be set to true, if this call is a static call, i.e., a call to a static member of a
     * [RecordDecl]. In this case the [callee] is most likely a [MemberExpr] in which
     * [MemberExpr.base] refers directly to the [RecordDecl] instead of an object.
     */
    var isStatic: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemberCallExpr) return false

        return super.equals(other) && base == other.base
    }

    override fun hashCode() = Objects.hash(super.hashCode(), base)
}
