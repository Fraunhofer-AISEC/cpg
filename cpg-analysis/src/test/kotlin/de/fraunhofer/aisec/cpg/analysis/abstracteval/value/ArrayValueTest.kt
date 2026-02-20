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
import de.fraunhofer.aisec.cpg.analysis.abstracteval.IntervalLattice
import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.graph.array
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewArray
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayValueTest {
    private val lattice =
        TupleState<Any>(DeclarationState(IntervalLattice()), NewIntervalState(IntervalLattice()))

    @Test
    fun applyDeclarationTest() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )

        val correctDeclaration =
            Variable().apply {
                this.name = name
                this.type = IntegerType(language = TestLanguage()).array()
                this.initializer =
                    NewArray().apply { this.dimensions += Literal<Int>().apply { this.value = 5 } }
            }

        assertEquals(
            LatticeInterval.Bounded(5, 5),
            ArrayValue()
                .applyEffect(lattice = lattice, state = startState, node = correctDeclaration),
        )
    }

    @Test
    fun applyReferenceTest() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )

        val decl =
            Variable().apply {
                this.name = name
                this.type = IntegerType(language = TestLanguage()).array()
                this.initializer =
                    NewArray().apply { this.dimensions += Literal<Int>().apply { this.value = 5 } }
            }
        lattice.pushToDeclarationState(startState, decl, LatticeInterval.Bounded(5, 5))
        val reference =
            Reference().apply {
                this.name = name
                this.refersTo = decl
            }

        assertEquals(
            LatticeInterval.Bounded(5, 5),
            ArrayValue().applyEffect(lattice = lattice, state = startState, node = reference),
        )
    }

    @Test
    fun applyDeclarationWithoutInitializerTest() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val noInitializerDeclaration =
            Variable().apply {
                this.name = name
                this.type = IntegerType(language = TestLanguage()).array()
            }

        assertEquals(
            LatticeInterval.TOP,
            ArrayValue()
                .applyEffect(lattice = lattice, state = startState, node = noInitializerDeclaration),
        )
    }
}
