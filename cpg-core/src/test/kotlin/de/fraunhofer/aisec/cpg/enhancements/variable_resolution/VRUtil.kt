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
package de.fraunhofer.aisec.cpg.enhancements.variable_resolution

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Utility-class to bundle functionality for the both test classes [VariableResolverCppTest] and
 * [VariableResolverJavaTest].
 */
object VRUtil {
    /** Used to flexibly switch between refers to as a collection or a single element */
    var REFERES_TO_IS_A_COLLECTION = true

    /**
     * Currently when variables or fields are used they can be stored in the expression itself, in
     * the future, a reference as usage indicator pointing to the used ValueDeclaration is planed.
     */
    var ENFORCE_REFERENCES = false

    /**
     * Currently there is no unified enforced structure when using fields, this field is used set
     * whether or not the tests enforce the presence of a member expression
     */
    var ENFORCE_MEMBER_EXPRESSION = false

    /**
     * Asserts equality or containing of the expected usedNode in the usingNode. If [ ]
     * [VRUtil.ENFORCE_REFERENCES] is true, `usingNode` must be a [ ] where
     * [DeclaredReferenceExpression.refersTo] is or contains `usedNode`. If this is not the case,
     * usage can also be interpreted as equality of the two.
     *
     * @param usingNode
     * - The node that shows usage of another node.
     * @param usedNode
     * - The node that is expected to be used.
     */
    fun assertUsageOf(usingNode: Node?, usedNode: Node?) {
        assertNotNull(usingNode)
        if (usingNode !is DeclaredReferenceExpression && !ENFORCE_REFERENCES) {
            assertSame(usedNode, usingNode)
        } else {
            assertTrue(usingNode is DeclaredReferenceExpression)
            val reference = usingNode as DeclaredReferenceExpression?
            assertEquals(reference!!.refersTo, usedNode)
        }
    }

    /**
     * Asserts that `usingNode` uses/references the provided `usedBase` and `usedMember`. If
     * [VRUtil.ENFORCE_MEMBER_EXPRESSION] is true, `usingNode` must be a [MemberExpression] where
     * [MemberExpression.base] uses `usedBase` and [ ][MemberExpression.refersTo] uses `usedMember`.
     * Using is checked as preformed per [ ][VRUtil.assertUsageOf]
     *
     * @param usingNode
     * - Node that uses some member
     * @param usedBase
     * - The expected base that is used
     * @param usedMember
     * - THe expected member that is used
     */
    fun assertUsageOfMemberAndBase(usingNode: Node?, usedBase: Node?, usedMember: Node?) {
        assertNotNull(usingNode)
        if (usingNode !is MemberExpression && !ENFORCE_MEMBER_EXPRESSION) {
            // Assumtion here is that the target of the member portion of the expression and not the
            // base
            // is resolved
            assertUsageOf(usingNode, usedMember)
        } else {
            assertTrue(usingNode is MemberExpression)
            val memberExpression = usingNode as MemberExpression?
            assertNotNull(memberExpression)

            val base: Node = memberExpression.base
            assertUsageOf(base, usedBase)
            assertUsageOf(memberExpression.refersTo, usedMember)
        }
    }
}
