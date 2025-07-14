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
import de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.Bound.*
import de.fraunhofer.aisec.cpg.analysis.abstracteval.NewIntervalLattice
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import kotlin.test.Test
import kotlin.test.assertEquals

class IntegerValueTest {
    private val name = Name("testVariable")
    private val current = LatticeInterval.Bounded(1, 1)

    @Test
    fun applyDeclarationTest() {
        val correctDeclaration = run {
            val decl = VariableDeclaration()
            val init = Literal<Int>()
            init.value = 5
            decl.name = name
            decl.initializer = init
            decl
        }
        assertEquals(
            LatticeInterval.Bounded(5, 5),
            IntegerValue()
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
            IntegerValue()
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
            IntegerValue()
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
                    name.localName,
                ),
        )
    }

    @Test
    fun applyUnaryOperator() {
        val preInc = run {
            val op = UnaryOperator()
            op.isPrefix = true
            op.operatorCode = "++"
            op.input.code = name.localName
            op
        }
        assertEquals(
            LatticeInterval.Bounded(2, 2),
            IntegerValue()
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
                    preInc,
                    name.localName,
                ),
        )

        val postInc = run {
            val op = UnaryOperator()
            op.isPrefix = false
            op.operatorCode = "++"
            op.input.code = name.localName
            op
        }
        assertEquals(
            LatticeInterval.Bounded(2, 2),
            IntegerValue()
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
                    postInc,
                    name.localName,
                ),
        )

        val preDec = run {
            val op = UnaryOperator()
            op.isPrefix = true
            op.operatorCode = "--"
            op.input.code = name.localName
            op
        }
        assertEquals(
            LatticeInterval.Bounded(0, 0),
            IntegerValue()
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
                    preDec,
                    name.localName,
                ),
        )

        val postDec = run {
            val op = UnaryOperator()
            op.isPrefix = false
            op.operatorCode = "--"
            op.input.code = name.localName
            op
        }
        assertEquals(
            LatticeInterval.Bounded(0, 0),
            IntegerValue()
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
                    postDec,
                    name.localName,
                ),
        )

        val wrongName = run {
            val op = UnaryOperator()
            op.isPrefix = false
            op.operatorCode = "--"
            op.input.code = "otherVariable"
            op
        }
        assertEquals(
            LatticeInterval.Bounded(1, 1),
            IntegerValue()
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
                    wrongName,
                    name.localName,
                ),
        )

        val wrongCode = run {
            val op = UnaryOperator()
            op.isPrefix = false
            op.operatorCode = "+-"
            op.input.code = name.localName
            op
        }
        assertEquals(
            LatticeInterval.Bounded(1, 1),
            IntegerValue()
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
                    wrongCode,
                    name.localName,
                ),
        )
    }

    @Test
    fun applyAssignExpression() {
        val assignLiteral = run {
            val expr = AssignExpression()
            val ref = Reference()
            val lit = Literal<Int>()
            lit.value = 3
            ref.code = name.localName
            expr.operatorCode = "="
            expr.lhs.add(0, ref)
            expr.rhs.add(0, lit)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(3, 3),
            IntegerValue()
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
                    assignLiteral,
                    name.localName,
                ),
        )

        val assignFallback = run {
            val expr = AssignExpression()
            val ref = Reference()
            ref.code = name.localName
            expr.operatorCode = "="
            expr.lhs.add(0, ref)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(NEGATIVE_INFINITE, INFINITE),
            IntegerValue()
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
                    assignFallback,
                    name.localName,
                ),
        )

        val assignPlusLiteral = run {
            val expr = AssignExpression()
            val ref = Reference()
            val lit = Literal<Int>()
            lit.value = 3
            ref.code = name.localName
            expr.operatorCode = "+="
            expr.lhs.add(0, ref)
            expr.rhs.add(0, lit)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(4, 4),
            IntegerValue()
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
                    assignPlusLiteral,
                    name.localName,
                ),
        )

        val assignPlusFallback = run {
            val expr = AssignExpression()
            val ref = Reference()
            ref.code = name.localName
            expr.operatorCode = "+="
            expr.lhs.add(0, ref)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(NEGATIVE_INFINITE, INFINITE),
            IntegerValue()
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
                    assignPlusFallback,
                    name.localName,
                ),
        )

        val assignMinusLiteral = run {
            val expr = AssignExpression()
            val ref = Reference()
            val lit = Literal<Int>()
            lit.value = 3
            ref.code = name.localName
            expr.operatorCode = "-="
            expr.lhs.add(0, ref)
            expr.rhs.add(0, lit)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(-2, -2),
            IntegerValue()
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
                    assignMinusLiteral,
                    name.localName,
                ),
        )

        val assignMinusFallback = run {
            val expr = AssignExpression()
            val ref = Reference()
            ref.code = name.localName
            expr.operatorCode = "-="
            expr.lhs.add(0, ref)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(NEGATIVE_INFINITE, INFINITE),
            IntegerValue()
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
                    assignMinusFallback,
                    name.localName,
                ),
        )

        val assignTimesLiteral = run {
            val expr = AssignExpression()
            val ref = Reference()
            val lit = Literal<Int>()
            lit.value = 3
            ref.code = name.localName
            expr.operatorCode = "*="
            expr.lhs.add(0, ref)
            expr.rhs.add(0, lit)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(3, 3),
            IntegerValue()
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
                    assignTimesLiteral,
                    name.localName,
                ),
        )

        val assignTimesFallback = run {
            val expr = AssignExpression()
            val ref = Reference()
            ref.code = name.localName
            expr.operatorCode = "*="
            expr.lhs.add(0, ref)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(NEGATIVE_INFINITE, INFINITE),
            IntegerValue()
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
                    assignTimesFallback,
                    name.localName,
                ),
        )

        val assignDivLiteral = run {
            val expr = AssignExpression()
            val ref = Reference()
            val lit = Literal<Int>()
            lit.value = 3
            ref.code = name.localName
            expr.operatorCode = "/="
            expr.lhs.add(0, ref)
            expr.rhs.add(0, lit)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(0, 0),
            IntegerValue()
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
                    assignDivLiteral,
                    name.localName,
                ),
        )

        val assignDivFallback = run {
            val expr = AssignExpression()
            val ref = Reference()
            ref.code = name.localName
            expr.operatorCode = "/="
            expr.lhs.add(0, ref)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(NEGATIVE_INFINITE, INFINITE),
            IntegerValue()
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
                    assignDivFallback,
                    name.localName,
                ),
        )

        val assignModLiteral = run {
            val expr = AssignExpression()
            val ref = Reference()
            val lit = Literal<Int>()
            lit.value = 3
            ref.code = name.localName
            expr.operatorCode = "%="
            expr.lhs.add(0, ref)
            expr.rhs.add(0, lit)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(1, 1),
            IntegerValue()
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
                    assignModLiteral,
                    name.localName,
                ),
        )

        val assignModFallback = run {
            val expr = AssignExpression()
            val ref = Reference()
            ref.code = name.localName
            expr.operatorCode = "%="
            expr.lhs.add(0, ref)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(NEGATIVE_INFINITE, INFINITE),
            IntegerValue()
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
                    assignModFallback,
                    name.localName,
                ),
        )
    }
}
