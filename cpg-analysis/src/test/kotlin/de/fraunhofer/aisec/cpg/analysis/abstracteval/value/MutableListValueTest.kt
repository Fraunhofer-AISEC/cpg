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
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Member
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import kotlin.test.Test
import kotlin.test.assertEquals

class MutableListValueTest {
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
                this.initializer =
                    MemberCall().apply {
                        this.arguments += Literal<Int>().apply { this.value = 5 }
                        this.arguments += Literal<Int>().apply { this.value = 5 }
                    }
            }

        assertEquals(
            LatticeInterval.Bounded(2, 2),
            MutableListSize()
                .applyEffect(lattice = lattice, state = startState, node = correctDeclaration),
        )
    }

    @Test
    fun applyDeclarationNoInitializerTest() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val noInitializerDeclaration = Variable().apply { this.name = name }

        assertEquals(
            LatticeInterval.BOTTOM,
            MutableListSize()
                .applyEffect(lattice = lattice, state = startState, node = noInitializerDeclaration),
        )
    }

    @Test
    fun applyAddTest() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val decl = Variable().apply { this.name = name }
        lattice.pushToDeclarationState(startState, decl, LatticeInterval.Bounded(1, 1))

        val add =
            MemberCall().apply {
                this.name = Name("add")
                callee =
                    Member().apply {
                        this.name = Name("add")
                        base =
                            Reference().apply {
                                this.name = name
                                this.refersTo = decl
                            }
                    }
                this.arguments += Literal<Int>().apply { this.value = 5 }
            }

        assertEquals(
            LatticeInterval.Bounded(2, 2),
            MutableListSize().applyEffect(lattice = lattice, state = startState, node = add),
        )
    }

    @Test
    fun applyAddAllTest() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val decl = Variable().apply { this.name = name }
        lattice.pushToDeclarationState(startState, decl, LatticeInterval.Bounded(1, 1))

        val addAll =
            MemberCall().apply {
                this.name = Name("addAll")
                callee =
                    Member().apply {
                        this.name = Name("addAll")
                        base =
                            Reference().apply {
                                this.name = name
                                this.refersTo = decl
                            }
                    }
                this.arguments += Literal<Int>().apply { this.value = 5 }
                this.arguments += Literal<Int>().apply { this.value = 10 }
            }

        assertEquals(
            LatticeInterval.Bounded(3, 3),
            MutableListSize().applyEffect(lattice = lattice, state = startState, node = addAll),
        )
    }

    @Test
    fun applyClearTest() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val decl = Variable().apply { this.name = name }
        lattice.pushToDeclarationState(startState, decl, LatticeInterval.Bounded(3, 3))

        val clear =
            MemberCall().apply {
                this.name = Name("clear")
                callee =
                    Member().apply {
                        this.name = Name("clear")
                        base =
                            Reference().apply {
                                this.name = name
                                this.refersTo = decl
                            }
                    }
            }

        assertEquals(
            LatticeInterval.Bounded(0, 0),
            MutableListSize().applyEffect(lattice = lattice, state = startState, node = clear),
        )
    }

    @Test
    fun applyRemoveIndexedTest() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val decl = Variable().apply { this.name = name }
        lattice.pushToDeclarationState(startState, decl, LatticeInterval.Bounded(3, 3))

        val removeInt =
            MemberCall().apply {
                this.name = Name("remove")
                callee =
                    Member().apply {
                        this.name = Name("remove")
                        base =
                            Reference().apply {
                                this.name = name
                                this.refersTo = decl
                            }
                    }
                this.arguments +=
                    Literal<Int>().apply {
                        this.value = 5
                        this.type = IntegerType(language = TestLanguage())
                    }
            }

        assertEquals(
            LatticeInterval.Bounded(2, 2),
            MutableListSize().applyEffect(lattice = lattice, state = startState, node = removeInt),
        )
    }

    @Test
    fun applyRemoveObjectTest() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val decl = Variable().apply { this.name = name }
        lattice.pushToDeclarationState(startState, decl, LatticeInterval.Bounded(3, 3))

        val removeObject =
            MemberCall().apply {
                this.name = Name("remove")
                callee =
                    Member().apply {
                        this.name = Name("remove")
                        base =
                            Reference().apply {
                                this.name = name
                                this.refersTo = decl
                            }
                    }
                this.arguments += Literal<Int>().apply { this.value = 5 }
            }

        assertEquals(
            LatticeInterval.Bounded(2, 3),
            MutableListSize()
                .applyEffect(lattice = lattice, state = startState, node = removeObject),
        )
    }

    @Test
    fun applyRemoveAllTest() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val decl = Variable().apply { this.name = name }
        lattice.pushToDeclarationState(startState, decl, LatticeInterval.Bounded(3, 3))

        val removeAll =
            MemberCall().apply {
                this.name = Name("removeAll")
                callee =
                    Member().apply {
                        this.name = Name("removeAll")
                        base =
                            Reference().apply {
                                this.name = name
                                this.refersTo = decl
                            }
                    }
                this.arguments += Literal<Int>().apply { this.value = 5 }
                this.arguments += Literal<Int>().apply { this.value = 10 }
            }
        assertEquals(
            LatticeInterval.Bounded(0, 3),
            MutableListSize().applyEffect(lattice = lattice, state = startState, node = removeAll),
        )
    }
}
