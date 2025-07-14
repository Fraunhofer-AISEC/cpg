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
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import kotlin.test.Test
import kotlin.test.assertEquals

class MutableListValueTest {
    private val name = Name("testVariable")
    private val current = LatticeInterval.Bounded(1, 1)

    @Test
    fun applyDeclarationTest() {
        val correctDeclaration = run {
            val decl = VariableDeclaration()
            val init = MemberCallExpression()
            val lit = Literal<Int>()
            lit.value = 5
            init.arguments = mutableListOf(lit, lit)
            decl.name = name
            decl.initializer = init
            decl
        }
        assertEquals(
            LatticeInterval.Bounded(2, 2),
            MutableListValue()
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
            MutableListValue()
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
            MutableListValue()
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
    fun applyDirectCallTest() {
        val add = run {
            val expr = MemberCallExpression()
            val member = MemberExpression()
            member.base.code = name.localName
            member.name = Name("add")
            expr.callee = member
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(2, 2),
            MutableListValue()
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
                    add,
                    name.localName,
                ),
        )

        val addAll = run {
            val expr = MemberCallExpression()
            val member = MemberExpression()
            member.base.code = name.localName
            member.name = Name("addAll")
            expr.callee = member
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(1, INFINITE),
            MutableListValue()
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
                    addAll,
                    name.localName,
                ),
        )

        val clear = run {
            val expr = MemberCallExpression()
            val member = MemberExpression()
            member.base.code = name.localName
            member.name = Name("clear")
            expr.callee = member
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(0, 0),
            MutableListValue()
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
                    clear,
                    name.localName,
                ),
        )

        val removeInt = run {
            val expr = MemberCallExpression()
            val lit = Literal<Int>()
            val member = MemberExpression()
            member.base.code = name.localName
            member.name = Name("remove")
            lit.type = IntegerType(language = TestLanguage())
            expr.callee = member
            expr.arguments = mutableListOf(lit)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(0, 0),
            MutableListValue()
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
                    removeInt,
                    name.localName,
                ),
        )

        val removeObject = run {
            val expr = MemberCallExpression()
            val lit = Literal<Any>()
            val member = MemberExpression()
            member.base.code = name.localName
            member.name = Name("remove")
            expr.callee = member
            expr.arguments = mutableListOf(lit)
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(0, 1),
            MutableListValue()
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
                    removeObject,
                    name.localName,
                ),
        )

        val removeAll = run {
            val expr = MemberCallExpression()
            val member = MemberExpression()
            member.base.code = name.localName
            member.name = Name("removeAll")
            expr.callee = member
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(0, 1),
            MutableListValue()
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
                    removeAll,
                    name.localName,
                ),
        )

        val wrongName = run {
            val expr = MemberCallExpression()
            val member = MemberExpression()
            member.name = Name("add")
            expr.callee = member
            expr
        }
        assertEquals(
            LatticeInterval.Bounded(1, 1),
            MutableListValue()
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
    }
}
