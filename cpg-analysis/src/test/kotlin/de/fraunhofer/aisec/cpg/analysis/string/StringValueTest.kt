/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis.string

import de.fraunhofer.aisec.cpg.analysis.abstracteval.DeclarationState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.GeneralState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.GeneralStateElement
import de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleStateElement
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.passes.objectIdentifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Direct unit tests for [StringValue.applyEffect], modelled on [IntegerValueTest][
 * de.fraunhofer.aisec.cpg.analysis.abstracteval.value.IntegerValueTest]. Complements the end-to-end
 * [AbstractStringEvaluatorTest] fixtures, which only ever exercise the `+`/`+=`/`=` happy paths and
 * therefore never reach the "unsupported operator"/"multiple lhs or rhs" fallback branches.
 */
class StringValueTest {
    private val name = Name("testVariable")
    private val innerLattice = StringLattice()
    private val lattice =
        TupleState<Any, StringPattern>(DeclarationState(innerLattice), GeneralState(innerLattice))

    private fun startState() =
        TupleStateElement<Any, StringPattern>(
            DeclarationState.DeclarationStateElement(),
            GeneralStateElement(),
        )

    @Test
    fun testLiteral() {
        val literal = Literal<String>().apply { value = "foo" }
        assertEquals(
            const("foo"),
            StringValue().applyEffect(lattice = lattice, state = startState(), node = literal),
        )
    }

    @Test
    fun testNullLiteralIsBottom() {
        val literal = Literal<String>()
        assertEquals(
            StringPattern.Bottom,
            StringValue().applyEffect(lattice = lattice, state = startState(), node = literal),
        )
    }

    @Test
    fun testDeclarationWithInitializer() {
        val declaration =
            Variable().apply {
                name = this@StringValueTest.name
                initializer = Literal<String>().apply { value = "foo" }
            }
        assertEquals(
            const("foo"),
            StringValue().applyEffect(lattice = lattice, state = startState(), node = declaration),
        )
    }

    @Test
    fun testUninitializedDeclarationIsUnknown() {
        val declaration = Variable().apply { name = this@StringValueTest.name }
        val result =
            StringValue().applyEffect(lattice = lattice, state = startState(), node = declaration)
        assertIs<StringPattern.Unknown>(result)
    }

    @Test
    fun testBinaryOperatorConcatenation() {
        val state = startState()
        val lhs =
            Reference().apply {
                name = this@StringValueTest.name
                refersTo = Variable().apply { name = this@StringValueTest.name }
                state.first[objectIdentifier()] = const("foo")
            }
        val rhs = Literal<String>().apply { value = "bar" }
        state.first[rhs.objectIdentifier()] = const("bar")
        val binaryOperator =
            BinaryOperator().apply {
                operatorCode = "+"
                this.lhs = lhs
                this.rhs = rhs
            }
        assertEquals(
            const("foobar"),
            StringValue().applyEffect(lattice = lattice, state = state, node = binaryOperator),
        )
    }

    /** `-` has no string semantics - a sound, unsupported fallback, not a crash. */
    @Test
    fun testUnsupportedBinaryOperatorIsUnknown() {
        val state = startState()
        val lhs = Literal<String>().apply { value = "foo" }
        val rhs = Literal<String>().apply { value = "bar" }
        state.first[lhs.objectIdentifier()] = const("foo")
        state.first[rhs.objectIdentifier()] = const("bar")
        val binaryOperator =
            BinaryOperator().apply {
                operatorCode = "-"
                this.lhs = lhs
                this.rhs = rhs
            }
        val result =
            StringValue().applyEffect(lattice = lattice, state = state, node = binaryOperator)
        assertIs<StringPattern.Unknown>(result)
    }

    @Test
    fun testAssign() {
        val state = startState()
        val reference =
            Reference().apply {
                name = this@StringValueTest.name
                refersTo = Variable().apply { name = this@StringValueTest.name }
                state.first[objectIdentifier()] = const("old")
            }
        val literal = Literal<String>().apply { value = "new" }
        state.first[literal.objectIdentifier()] = const("new")
        val assign =
            Assign().apply {
                operatorCode = "="
                lhs += reference
                rhs += literal
            }
        assertEquals(
            const("new"),
            StringValue().applyEffect(lattice = lattice, state = state, node = assign),
        )
    }

    @Test
    fun testAssignPlusLiteral() {
        val state = startState()
        val reference =
            Reference().apply {
                name = this@StringValueTest.name
                refersTo = Variable().apply { name = this@StringValueTest.name }
                state.first[objectIdentifier()] = const("foo")
            }
        val literal = Literal<String>().apply { value = "bar" }
        state.first[literal.objectIdentifier()] = const("bar")
        val assign =
            Assign().apply {
                operatorCode = "+="
                lhs += reference
                rhs += literal
            }
        assertEquals(
            const("foobar"),
            StringValue().applyEffect(lattice = lattice, state = state, node = assign),
        )
    }

    /** `-=` has no string semantics - a sound, unsupported fallback, not a crash. */
    @Test
    fun testUnsupportedAssignOperatorIsUnknown() {
        val state = startState()
        val reference =
            Reference().apply {
                name = this@StringValueTest.name
                refersTo = Variable().apply { name = this@StringValueTest.name }
                state.first[objectIdentifier()] = const("foo")
            }
        val literal = Literal<String>().apply { value = "bar" }
        state.first[literal.objectIdentifier()] = const("bar")
        val assign =
            Assign().apply {
                operatorCode = "-="
                lhs += reference
                rhs += literal
            }
        val result = StringValue().applyEffect(lattice = lattice, state = state, node = assign)
        assertIs<StringPattern.Unknown>(result)
    }

    /**
     * A destructuring assignment (`a, b = f()`-shaped, multiple lhs/rhs) is not modelled - the
     * fallback at the end of [StringValue.applyEffect] applies instead of crashing.
     */
    @Test
    fun testMultipleLhsRhsAssignFallsBackToPatternOf() {
        val state = startState()
        val assign =
            Assign().apply {
                operatorCode = "="
                lhs +=
                    Reference().apply {
                        name = this@StringValueTest.name
                        refersTo = Variable().apply { name = this@StringValueTest.name }
                    }
                lhs += Reference().apply { name = Name("other") }
                rhs += Literal<String>().apply { value = "a" }
                rhs += Literal<String>().apply { value = "b" }
            }
        val result = StringValue().applyEffect(lattice = lattice, state = state, node = assign)
        assertIs<StringPattern.Unknown>(result)
    }
}
