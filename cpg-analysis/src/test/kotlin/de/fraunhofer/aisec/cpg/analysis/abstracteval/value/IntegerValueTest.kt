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
import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.Bound.*
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.passes.objectIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals

class IntegerValueTest {
    private val name = Name("testVariable")
    private val current = LatticeInterval.Bounded(1, 1)
    val lattice =
        TupleState<Any>(DeclarationState(IntervalLattice()), NewIntervalState(IntervalLattice()))

    @Test
    fun applyDeclarationTest() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val declaration =
            VariableDeclaration().apply {
                name = this@IntegerValueTest.name
                initializer =
                    Literal<Int>().apply {
                        value = 5
                        this.value?.let { value ->
                            startState.first[this.objectIdentifier()] =
                                IntervalLattice.Element(LatticeInterval.Bounded(value, value))
                        }
                    }
            }
        assertEquals(
            LatticeInterval.Bounded(5, 5),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = declaration),
        )
    }

    @Test
    fun applyUninitializedDeclarationTest() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val declaration = VariableDeclaration().apply { name = this@IntegerValueTest.name }
        assertEquals(
            LatticeInterval.Bounded(NEGATIVE_INFINITE, INFINITE),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = declaration),
        )
    }

    @Test
    fun applyPrefixIncrement() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val reference =
            Reference().apply {
                name = this@IntegerValueTest.name
                refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                startState.first[objectIdentifier()] = IntervalLattice.Element(current)
            }
        val unaryOperator =
            UnaryOperator().apply {
                isPrefix = true
                operatorCode = "++"
                input = reference
            }
        assertEquals(
            LatticeInterval.Bounded(2, 2),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = unaryOperator),
        )
    }

    @Test
    fun applyPostfixIncrement() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val reference =
            Reference().apply {
                name = this@IntegerValueTest.name
                refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                startState.first[objectIdentifier()] = IntervalLattice.Element(current)
            }
        val unaryOperator =
            UnaryOperator().apply {
                isPrefix = false
                operatorCode = "++"
                input = reference
            }
        assertEquals(
            LatticeInterval.Bounded(2, 2),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = unaryOperator),
        )
    }

    @Test
    fun applyPrefixDecrement() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val reference =
            Reference().apply {
                name = this@IntegerValueTest.name
                refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                startState.first[objectIdentifier()] = IntervalLattice.Element(current)
            }
        val unaryOperator =
            UnaryOperator().apply {
                isPrefix = true
                operatorCode = "--"
                input = reference
            }
        assertEquals(
            LatticeInterval.Bounded(0, 0),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = unaryOperator),
        )
    }

    @Test
    fun applyPostfixIncrementation() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val reference =
            Reference().apply {
                name = this@IntegerValueTest.name
                refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                startState.first[objectIdentifier()] = IntervalLattice.Element(current)
            }
        val unaryOperator =
            UnaryOperator().apply {
                isPrefix = false
                operatorCode = "--"
                input = reference
            }
        assertEquals(
            LatticeInterval.Bounded(0, 0),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = unaryOperator),
        )
    }

    @Test
    fun applyUnaryStar() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val reference =
            Reference().apply {
                name = this@IntegerValueTest.name
                refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                startState.first[objectIdentifier()] = IntervalLattice.Element(current)
            }
        val unaryOperator =
            UnaryOperator().apply {
                operatorCode = "*"
                input = reference
            }
        assertEquals(
            LatticeInterval.Bounded(1, 1),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = unaryOperator),
        )
    }

    @Test
    fun applyAssignExpression() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val assignment =
            AssignExpression().apply {
                operatorCode = "="
                lhs +=
                    Reference().apply {
                        name = this@IntegerValueTest.name
                        refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                        startState.first[objectIdentifier()] = IntervalLattice.Element(current)
                    }
                rhs +=
                    Literal<Int>().apply {
                        value = 3
                        this.value?.let { value ->
                            startState.first[this.objectIdentifier()] =
                                IntervalLattice.Element(LatticeInterval.Bounded(value, value))
                        }
                    }
            }
        assertEquals(
            LatticeInterval.Bounded(3, 3),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = assignment),
        )
    }

    @Test
    fun testAssignUnresolved() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val assignment =
            AssignExpression().apply {
                operatorCode = "="
                lhs +=
                    Reference().apply {
                        name = this@IntegerValueTest.name
                        refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                        startState.first[objectIdentifier()] = IntervalLattice.Element(current)
                    }
            }
        assertEquals(
            LatticeInterval.Bounded(NEGATIVE_INFINITE, INFINITE),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = assignment),
        )
    }

    @Test
    fun testAssignPlusLiteral() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val assignment =
            AssignExpression().apply {
                operatorCode = "+="
                lhs +=
                    Reference().apply {
                        name = this@IntegerValueTest.name
                        refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                        startState.first[objectIdentifier()] = IntervalLattice.Element(current)
                    }
                rhs +=
                    Literal<Int>().apply {
                        value = 3
                        this.value?.let { value ->
                            startState.first[this.objectIdentifier()] =
                                IntervalLattice.Element(LatticeInterval.Bounded(value, value))
                        }
                    }
            }
        assertEquals(
            LatticeInterval.Bounded(4, 4),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = assignment),
        )
    }

    @Test
    fun testAssignPlusUnresolved() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val assignment =
            AssignExpression().apply {
                operatorCode = "+="
                lhs +=
                    Reference().apply {
                        name = this@IntegerValueTest.name
                        refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                        startState.first[objectIdentifier()] = IntervalLattice.Element(current)
                    }
            }
        assertEquals(
            LatticeInterval.Bounded(NEGATIVE_INFINITE, INFINITE),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = assignment),
        )
    }

    @Test
    fun testAssignMinusLiteral() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val assignment =
            AssignExpression().apply {
                operatorCode = "-="
                lhs +=
                    Reference().apply {
                        name = this@IntegerValueTest.name
                        refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                        startState.first[objectIdentifier()] = IntervalLattice.Element(current)
                    }
                rhs +=
                    Literal<Int>().apply {
                        value = 3
                        this.value?.let { value ->
                            startState.first[this.objectIdentifier()] =
                                IntervalLattice.Element(LatticeInterval.Bounded(value, value))
                        }
                    }
            }
        assertEquals(
            LatticeInterval.Bounded(-2, -2),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = assignment),
        )
    }

    @Test
    fun testAssignMinusUnresolved() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val assignment =
            AssignExpression().apply {
                operatorCode = "-="
                lhs +=
                    Reference().apply {
                        name = this@IntegerValueTest.name
                        refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                        startState.first[objectIdentifier()] = IntervalLattice.Element(current)
                    }
            }
        assertEquals(
            LatticeInterval.Bounded(NEGATIVE_INFINITE, INFINITE),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = assignment),
        )
    }

    @Test
    fun testAssignTimesLiteral() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val assignment =
            AssignExpression().apply {
                operatorCode = "*="
                lhs +=
                    Reference().apply {
                        name = this@IntegerValueTest.name
                        refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                        startState.first[objectIdentifier()] = IntervalLattice.Element(current)
                    }
                rhs +=
                    Literal<Int>().apply {
                        value = 3
                        this.value?.let { value ->
                            startState.first[this.objectIdentifier()] =
                                IntervalLattice.Element(LatticeInterval.Bounded(value, value))
                        }
                    }
            }
        assertEquals(
            LatticeInterval.Bounded(3, 3),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = assignment),
        )
    }

    @Test
    fun testAssignTimesUnresolved() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val assignment =
            AssignExpression().apply {
                operatorCode = "*="
                lhs +=
                    Reference().apply {
                        name = this@IntegerValueTest.name
                        refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                        startState.first[objectIdentifier()] = IntervalLattice.Element(current)
                    }
            }
        assertEquals(
            LatticeInterval.Bounded(NEGATIVE_INFINITE, INFINITE),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = assignment),
        )
    }

    @Test
    fun testAssignDivLiteral() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val assignment =
            AssignExpression().apply {
                operatorCode = "/="
                lhs +=
                    Reference().apply {
                        name = this@IntegerValueTest.name
                        refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                        startState.first[objectIdentifier()] = IntervalLattice.Element(current)
                    }
                rhs +=
                    Literal<Int>().apply {
                        value = 3
                        this.value?.let { value ->
                            startState.first[this.objectIdentifier()] =
                                IntervalLattice.Element(LatticeInterval.Bounded(value, value))
                        }
                    }
            }
        assertEquals(
            LatticeInterval.Bounded(0, 0),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = assignment),
        )
    }

    @Test
    fun testAssignDivUnresolved() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val assignment =
            AssignExpression().apply {
                operatorCode = "/="
                lhs +=
                    Reference().apply {
                        name = this@IntegerValueTest.name
                        refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                        startState.first[objectIdentifier()] = IntervalLattice.Element(current)
                    }
            }
        assertEquals(
            LatticeInterval.Bounded(NEGATIVE_INFINITE, INFINITE),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = assignment),
        )
    }

    @Test
    fun testAssignModLiteral() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val assignment =
            AssignExpression().apply {
                operatorCode = "%="
                lhs +=
                    Reference().apply {
                        name = this@IntegerValueTest.name
                        refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                        startState.first[objectIdentifier()] = IntervalLattice.Element(current)
                    }
                rhs +=
                    Literal<Int>().apply {
                        value = 3
                        this.value?.let { value ->
                            startState.first[this.objectIdentifier()] =
                                IntervalLattice.Element(LatticeInterval.Bounded(value, value))
                        }
                    }
            }
        assertEquals(
            LatticeInterval.Bounded(1, 1),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = assignment),
        )
    }

    @Test
    fun testAssignModUnresolved() {
        val startState =
            TupleStateElement<Any>(
                DeclarationState.DeclarationStateElement(),
                NewIntervalStateElement(),
            )
        val assignment =
            AssignExpression().apply {
                operatorCode = "%="
                lhs +=
                    Reference().apply {
                        name = this@IntegerValueTest.name
                        refersTo = VariableDeclaration().apply { name = this@IntegerValueTest.name }
                        startState.first[objectIdentifier()] = IntervalLattice.Element(current)
                    }
            }
        assertEquals(
            LatticeInterval.Bounded(NEGATIVE_INFINITE, INFINITE),
            IntegerValue().applyEffect(lattice = lattice, state = startState, node = assignment),
        )
    }
}
