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
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.graph.array
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.ArrayConstruction
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayValueTest {
    private val lattice =
        TupleState<Any>(
            DeclarationState(NewIntervalLattice()),
            NewIntervalState(NewIntervalLattice()),
        )

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
                    ArrayConstruction().apply {
                        this.dimensions += Literal<Int>().apply { this.value = 5 }
                    }
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
                    ArrayConstruction().apply {
                        this.dimensions += Literal<Int>().apply { this.value = 5 }
                    }
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

    /**
     * An [ArrayConstruction] with no [dimensions] (and no initializer) used to crash inside
     * `getSize` because `reduce` throws on an empty list. It must now return BOTTOM gracefully —
     * `ArraySizeEvaluator` calls `getSize` on arbitrary nodes, so any crash here propagates.
     */
    @Test
    fun testEmptyDimensions() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val emptyDimensionsDeclaration =
            Variable().apply {
                this.name = name
                this.type = IntegerType(language = TestLanguage()).array()
                this.initializer = ArrayConstruction()
            }

        assertEquals(
            LatticeInterval.BOTTOM,
            ArrayValue()
                .applyEffect(
                    lattice = lattice,
                    state = startState,
                    node = emptyDimensionsDeclaration,
                ),
        )
    }

    /**
     * An [ArrayConstruction] whose dimension constant-evaluates to a non-[Number] (e.g. a string
     * literal — happens when the dimension expression can't be resolved to an integer) used to
     * crash with `ClassCastException` because the cast was `as Number` rather than `as? Number`. It
     * must now return BOTTOM gracefully.
     */
    @Test
    fun testNonNumberDimension() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val nonNumberDimensionDeclaration =
            Variable().apply {
                this.name = name
                this.type = IntegerType(language = TestLanguage()).array()
                this.initializer =
                    ArrayConstruction().apply {
                        this.dimensions += Literal<String>().apply { this.value = "not a number" }
                    }
            }

        assertEquals(
            LatticeInterval.BOTTOM,
            ArrayValue()
                .applyEffect(
                    lattice = lattice,
                    state = startState,
                    node = nonNumberDimensionDeclaration,
                ),
        )
    }
}
