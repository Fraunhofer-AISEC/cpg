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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.ProblemExpression
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArgumentHolderTest {
    @Test
    fun testHasArgument() {
        with(TestLanguageFrontend()) {
            var ref = newReference("test")
            var list =
                listOf(
                    newBinaryOperator("?"),
                    newUnaryOperator("?", false, false),
                    newCallExpression(),
                    newCastExpression(),
                    newIfStatement(),
                    newReturnStatement(),
                    newConditionalExpression(newLiteral(true)),
                    newDoStatement(),
                    newInitializerListExpression(),
                    newKeyValueExpression(key = ProblemExpression(), value = ProblemExpression()),
                    newSubscriptExpression(),
                    newWhileStatement(),
                    newAssignExpression(),
                    newVariableDeclaration("test"),
                )

            for (node in list) {
                assertFalse(node.hasArgument(ref), "hasArgument failed for ${node::class}")
            }

            for (node in list) {
                node += ref
                assertTrue(node.hasArgument(ref), "hasArgument failed for ${node::class}")
            }
        }
    }
}
