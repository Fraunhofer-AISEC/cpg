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
package de.fraunhofer.aisec.cpg.graph2

import de.fraunhofer.aisec.cpg.passes2.scopes.ScopeManager2
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class Graph2test {
    @Test
    fun testImplicitDeclaration() {
        val a1 = Assignment(Reference("a"), Literal(2))
        println(a1.toPseudoCode())

        val a2 = Assignment(Reference("a"), BinaryOperation(Literal(3), "+", Literal(4)))
        println(a2.toPseudoCode())

        val b = Block(listOf(a1, a2))

        val ref = a2.lhs
        assertEquals(a2, ref.parent)

        val i = ImplicitDeclarator()
        i.doPass(b)

        // collect all references
        val refs = collect<Reference>(b)
        assertEquals(2, refs.size)
    }

    @Test
    fun testExplicitDeclaration() {
        val scope = ScopeManager2()

        val b = Block()
        scope.use(b) {
            val a = Variable("a", "int")

            it += a

            val stmt = DeclarationStatement(listOf(a))

            val a1 = Assignment(Reference("a"), Literal(2))

            b += stmt
            b += a1
        }

        // collect all variables
        val vars = collect<Variable>(b)
        assertEquals(1, vars.size)
    }
}
