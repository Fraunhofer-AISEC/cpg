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
package de.fraunhofer.aisec.cpg.analysis.abstracteval.value

import de.fraunhofer.aisec.cpg.analysis.abstracteval.*
import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.analysis.abstracteval.NewIntervalLattice
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewArrayExpression
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayValueTest {
    private val name = Name("testVariable")
    private val current = LatticeInterval.Bounded(1, 1)

    @Test
    fun applyDeclarationTest() {
        val correctDeclaration = run {
            val decl = VariableDeclaration()
            val init = NewArrayExpression()
            val lit = Literal<Int>()
            lit.value = 5
            init.dimensions = mutableListOf(lit)
            decl.name = name
            decl.initializer = init
            decl
        }
        assertEquals(
            LatticeInterval.Bounded(5, 5),
            ArrayValue()
                .applyEffect(
                    current,
                    TupleState(
                        DeclarationState(NewIntervalLattice()),
                        NewIntervalState(NewIntervalLattice()),
                    ),
                    de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleStateElement(
                        DeclarationStateElement(),
                        NewIntervalStateElement(),
                    ),
                    correctDeclaration,
                    null,
                    name.localName,
                ),
        )

        val wrongNameDeclaration = run {
            val decl = VariableDeclaration()
            val init = Literal<Int>()
            init.value = 5
            decl.name = Name("otherVariable")
            decl.initializer = init
            decl
        }
        assertEquals(
            LatticeInterval.Bounded(1, 1),
            ArrayValue()
                .applyEffect(
                    current,
                    TupleState(
                        DeclarationState(NewIntervalLattice()),
                        NewIntervalState(NewIntervalLattice()),
                    ),
                    de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleStateElement(
                        DeclarationStateElement(),
                        NewIntervalStateElement(),
                    ),
                    wrongNameDeclaration,
                    null,
                    name.localName,
                ),
        )

        val noInitializerDeclaration = run {
            val decl = VariableDeclaration()
            decl.name = name
            decl
        }
        assertEquals(
            LatticeInterval.Bounded(1, 1),
            ArrayValue()
                .applyEffect(
                    current,
                    TupleState(
                        DeclarationState(NewIntervalLattice()),
                        NewIntervalState(NewIntervalLattice()),
                    ),
                    de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleStateElement(
                        DeclarationStateElement(),
                        NewIntervalStateElement(),
                    ),
                    noInitializerDeclaration,
                    null,
                    name.localName,
                ),
        )
    }
}
