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
package de.fraunhofer.aisec.cpg.analysis.abstracteval

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.testcases.AbstractEvaluationTests
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AbstractEvaluatorTest {
    private lateinit var tu: TranslationUnitDeclaration

    @BeforeAll
    fun beforeAll() {
        tu = AbstractEvaluationTests.getIntegerExample().components.first().translationUnits.first()
    }

    /*
       Bar f = new Bar();
       int a = 5;

       a = 5;
       a -= 2;
       a += 3;

       b.f(a);
    */
    @Test
    fun testSimpleInteger() {
        val mainClass = tu.records["Foo"]
        assertNotNull(mainClass)
        val f1 = mainClass.methods["f1"]
        assertNotNull(f1)

        val refA = f1.bodyOrNull<MemberCallExpression>(5)!!.arguments.first()
        assertNotNull(refA)

        val evaluator = AbstractIntervalEvaluator()
        val value = evaluator.evaluate(refA)
        assertEquals(LatticeInterval.Bounded(1, 1), value)
    }

    /*
       Bar f = new Bar();
       int a = 5;

       a = 3;
       a++;
       ++a;
       a -= 2;
       a += 3;
       a--;
       --a;
       a *= 4;
       a /= 2;
       a %= 3;

       b.f(a);
    */
    @Test
    fun testIntegerOperations() {
        val mainClass = tu.records["Foo"]
        assertNotNull(mainClass)
        val f1 = mainClass.methods["f2"]
        assertNotNull(f1)

        val refA = f1.bodyOrNull<MemberCallExpression>(12)!!.arguments.first()
        assertNotNull(refA)

        val evaluator = AbstractIntervalEvaluator()
        val value = evaluator.evaluate(refA)
        assertEquals(LatticeInterval.Bounded(2, 2), value)
    }

    /*
       Bar b = new Bar();
       int a = 5;

       if (new Random().nextBoolean()) {
           a -= 1;
       }

       b.f(a);
    */
    @Test
    fun testBranch1Integer() {
        val mainClass = tu.records["Foo"]
        assertNotNull(mainClass)
        val f1 = mainClass.methods["f3"]
        assertNotNull(f1)

        val refA = f1.bodyOrNull<MemberCallExpression>(3)!!.arguments.first()
        assertNotNull(refA)

        val evaluator = AbstractIntervalEvaluator()
        val value = evaluator.evaluate(refA)
        assertEquals(LatticeInterval.Bounded(4, 5), value)
    }

    /*
       Bar b = new Bar();
       int a = 5;

       if (new Random().nextBoolean()) {
           a -= 1;
       } else {
           a = 3;
       }

       b.f(a);
    */
    @Test
    fun testBranch2Integer() {
        val mainClass = tu.records["Foo"]
        assertNotNull(mainClass)
        val f1 = mainClass.methods["f4"]
        assertNotNull(f1)

        val refA = f1.bodyOrNull<MemberCallExpression>(3)!!.arguments.first()
        assertNotNull(refA)

        val evaluator = AbstractIntervalEvaluator()
        val value = evaluator.evaluate(refA)
        assertEquals(LatticeInterval.Bounded(3, 4), value)
    }

    /*
       Bar b = new Bar();
       int a = 5;

       for (int i = 0; i < 5; i++) {
           a += 1;
           println(i);
       }

       b.f(a);
    */
    @Test
    fun testLoopInteger() {
        val mainClass = tu.records["Foo"]
        assertNotNull(mainClass)
        val f5 = mainClass.methods["f5"]
        assertNotNull(f5)

        val refI = f5.calls["println"]?.arguments?.singleOrNull()
        assertNotNull(refI)

        val evaluatorI = AbstractIntervalEvaluator()
        val valueI = evaluatorI.evaluate(refI)
        assertEquals(LatticeInterval.Bounded(0, 4), valueI)

        val refA = f5.bodyOrNull<MemberCallExpression>(6)!!.arguments.first()
        assertNotNull(refA)

        val evaluator = AbstractIntervalEvaluator()
        val value = evaluator.evaluate(refA)
        assertEquals(LatticeInterval.Bounded(5, LatticeInterval.Bound.INFINITE), value)
    }
}
