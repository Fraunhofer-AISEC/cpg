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
package de.fraunhofer.aisec.cpg.enhancements.calls

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.TestUtils.findByUniquePredicate
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.SearchModifier.UNIQUE
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.variables
import java.nio.file.Path
import kotlin.test.*

internal class ConstructorsTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "constructors")

    @Test
    @Throws(Exception::class)
    fun testJava() {
        val result = TestUtils.analyze("java", topLevel, true)
        val constructors = result.allChildren<ConstructorDeclaration>()
        val noArg = findByUniquePredicate(constructors) { it.parameters.size == 0 }
        val singleArg = findByUniquePredicate(constructors) { it.parameters.size == 1 }
        val twoArgs = findByUniquePredicate(constructors) { it.parameters.size == 2 }
        val variables = result.variables
        val a1 = assertNotNull(variables["a1", UNIQUE])
        assertNotNull(a1)
        assertTrue(a1.initializer is NewExpression)
        assertTrue((a1.initializer as? NewExpression)?.initializer is ConstructExpression)

        val a1Initializer = (a1.initializer as NewExpression).initializer as ConstructExpression
        assertEquals(noArg, a1Initializer.constructor)

        val a2 = assertNotNull(variables["a2", UNIQUE])
        assertNotNull(a2)
        assertTrue(a2.initializer is NewExpression)
        assertTrue((a2.initializer as? NewExpression)?.initializer is ConstructExpression)

        val a2Initializer = (a2.initializer as NewExpression).initializer as ConstructExpression
        assertEquals(singleArg, a2Initializer.constructor)

        val a3 = assertNotNull(variables["a3", UNIQUE])
        assertNotNull(a3)
        assertTrue(a3.initializer is NewExpression)
        assertTrue((a3.initializer as NewExpression).initializer is ConstructExpression)

        val a3Initializer = (a3.initializer as NewExpression).initializer as ConstructExpression
        assertEquals(twoArgs, a3Initializer.constructor)

        val a4 = assertNotNull(variables["a4", UNIQUE])
        assertNotNull(a4)
        assertTrue(a4.initializer is UninitializedValue)
    }

    @Test
    @Throws(Exception::class)
    fun testCPP() {
        val result = TestUtils.analyze("cpp", topLevel, true)
        val constructors = result.allChildren<ConstructorDeclaration>()
        val noArg =
            findByUniquePredicate(constructors) { it.parameters.size == 0 && it.name == "A" }
        val singleArg =
            findByUniquePredicate(constructors) { it.parameters.size == 1 && it.name == "A" }
        val twoArgs =
            findByUniquePredicate(constructors) { it.parameters.size == 2 && it.name == "A" }
        val variables = result.variables
        val a1 = assertNotNull(variables["a1", UNIQUE])
        assertNotNull(a1)
        assertTrue(a1.initializer is ConstructExpression)

        val a1Initializer = a1.initializer as ConstructExpression
        assertEquals(noArg, a1Initializer.constructor)

        val a2 = assertNotNull(variables["a2", UNIQUE])
        assertNotNull(a2)
        assertTrue(a2.initializer is ConstructExpression)

        val a2Initializer = a2.initializer as ConstructExpression
        assertEquals(singleArg, a2Initializer.constructor)

        val a3 = assertNotNull(variables["a3", UNIQUE])
        assertNotNull(a3)
        assertTrue(a3.initializer is ConstructExpression)

        val a3Initializer = a3.initializer as ConstructExpression
        assertEquals(twoArgs, a3Initializer.constructor)

        val a4 = assertNotNull(variables["a4", UNIQUE])
        assertNotNull(a4)
        assertTrue(a4.initializer is ConstructExpression)

        val a4Initializer = a4.initializer as ConstructExpression
        assertEquals(noArg, a4Initializer.constructor)

        val a5 = assertNotNull(variables["a5", UNIQUE])
        assertNotNull(a5)
        assertTrue(a5.initializer is ConstructExpression)

        val a5Initializer = a5.initializer as ConstructExpression
        assertEquals(singleArg, a5Initializer.constructor)

        val a6 = assertNotNull(variables["a6", UNIQUE])
        assertNotNull(a6)
        assertTrue(a6.initializer is ConstructExpression)

        val a6Initializer = a6.initializer as ConstructExpression
        assertEquals(twoArgs, a6Initializer.constructor)

        val a7 = assertNotNull(variables["a7", UNIQUE])
        assertNotNull(a7)
        assertTrue(a7.initializer is NewExpression)
        assertTrue((a7.initializer as NewExpression).initializer is ConstructExpression)

        val a7Initializer = (a7.initializer as NewExpression).initializer as ConstructExpression
        assertEquals(noArg, a7Initializer.constructor)

        val a8 = assertNotNull(variables["a8", UNIQUE])
        assertNotNull(a8)
        assertTrue(a8.initializer is NewExpression)
        assertTrue((a8.initializer as NewExpression).initializer is ConstructExpression)

        val a8Initializer = (a8.initializer as NewExpression).initializer as ConstructExpression
        assertEquals(noArg, a8Initializer.constructor)

        val a9 = assertNotNull(variables["a9", UNIQUE])
        assertNotNull(a9)
        assertTrue(a9.initializer is NewExpression)
        assertTrue((a9.initializer as NewExpression).initializer is ConstructExpression)

        val a9Initializer = (a9.initializer as NewExpression).initializer as ConstructExpression
        assertEquals(singleArg, a9Initializer.constructor)

        val a10 = assertNotNull(variables["a10", UNIQUE])
        assertNotNull(a10)
        assertTrue(a10.initializer is NewExpression)
        assertTrue((a10.initializer as NewExpression).initializer is ConstructExpression)

        val a10Initializer = (a10.initializer as NewExpression).initializer as ConstructExpression
        assertEquals(twoArgs, a10Initializer.constructor)
    }

    @Test
    @Throws(Exception::class)
    fun testCPPFullDefault() {
        val result =
            TestUtils.analyze(
                listOf(
                    Path.of(topLevel.toString(), "defaultarg", "constructorDefault.cpp").toFile()
                ),
                topLevel,
                true
            )
        val constructors = result.allChildren<ConstructorDeclaration>()
        val variables = result.variables
        val twoDefaultArg =
            findByUniquePredicate(constructors) { it.defaultParameters.size == 2 && it.name == "D" }
        assertNotNull(twoDefaultArg)

        val literal0 = findByUniquePredicate(result.literals) { it.value == 0 }
        val literal1 = findByUniquePredicate(result.literals) { it.value == 1 }
        val d1 = assertNotNull(variables["d1", UNIQUE])
        assertNotNull(d1)
        assertTrue(d1.initializer is ConstructExpression)

        val d1Initializer = d1.initializer as ConstructExpression
        assertEquals(twoDefaultArg, d1Initializer.constructor)
        assertEquals(0, d1Initializer.arguments.size)
        assertTrue(twoDefaultArg.nextEOG.contains(literal0))
        assertTrue(twoDefaultArg.nextEOG.contains(literal1))
        assertTrue(literal0.nextEOG.contains(literal1))

        for (node in twoDefaultArg.nextEOG) {
            if (!(node == literal0 || node == literal1)) {
                assertTrue(literal1.nextEOG.contains(node))
            }
        }
        val d2 = assertNotNull(variables["d2", UNIQUE])
        assertTrue(d2.initializer is ConstructExpression)

        val d2Initializer = d2.initializer as ConstructExpression
        assertEquals(twoDefaultArg, d2Initializer.constructor)
        assertEquals(1, d2Initializer.arguments.size)
        assertEquals(2, (d2Initializer.arguments[0] as Literal<*>).value)

        val d3 = assertNotNull(variables["d3", UNIQUE])
        assertTrue(d3.initializer is ConstructExpression)

        val d3Initializer = d3.initializer as ConstructExpression
        assertEquals(twoDefaultArg, d3Initializer.constructor)
        assertEquals(2, d3Initializer.arguments.size)
        assertEquals(3, (d3Initializer.arguments[0] as Literal<*>).value)
        assertEquals(4, (d3Initializer.arguments[1] as Literal<*>).value)
    }

    @Test
    @Throws(Exception::class)
    fun testCPPPartialDefault() {
        val result =
            TestUtils.analyze(
                listOf(
                    Path.of(topLevel.toString(), "defaultarg", "constructorDefault.cpp").toFile()
                ),
                topLevel,
                true
            )
        val constructors = result.allChildren<ConstructorDeclaration>()
        val variables = result.variables
        val singleDefaultArg =
            findByUniquePredicate(constructors) { c: ConstructorDeclaration ->
                c.parameters.size == 2 && c.name == "E"
            }
        val literal10 = findByUniquePredicate(result.literals) { it.value == 10 }
        val e1 = assertNotNull(variables["e1", UNIQUE])
        assertTrue(e1.initializer is ConstructExpression)
        val e1Initializer = e1.initializer as ConstructExpression
        assertTrue(e1Initializer.constructor!!.isInferred)
        assertEquals(0, e1Initializer.arguments.size)
        val e2 = assertNotNull(variables["e2", UNIQUE])
        assertTrue(e2.initializer is ConstructExpression)
        val e2Initializer = e2.initializer as ConstructExpression
        assertEquals(singleDefaultArg, e2Initializer.constructor)
        assertEquals(1, e2Initializer.arguments.size)
        assertEquals(5, (e2Initializer.arguments[0] as Literal<*>).value)
        assertTrue(singleDefaultArg.nextEOG.contains(literal10))
        for (node in singleDefaultArg.nextEOG) {
            if (node != literal10) {
                assertTrue(literal10.nextEOG.contains(node))
            }
        }
        val e3 = assertNotNull(variables["e3", UNIQUE])
        assertTrue(e3.initializer is ConstructExpression)
        val e3Initializer = e3.initializer as ConstructExpression
        assertEquals(singleDefaultArg, e3Initializer.constructor)
        assertEquals(2, e3Initializer.arguments.size)
        assertEquals(6, (e3Initializer.arguments[0] as Literal<*>).value)
        assertEquals(7, (e3Initializer.arguments[1] as Literal<*>).value)
    }

    @Test
    @Throws(Exception::class)
    fun testCPPImplicitCast() {
        val result =
            TestUtils.analyze(
                listOf(
                    Path.of(topLevel.toString(), "implicitcastarg", "constructorImplicit.cpp")
                        .toFile()
                ),
                topLevel,
                true
            )
        val constructors = result.allChildren<ConstructorDeclaration>()
        val variables = result.variables
        val implicitConstructor =
            findByUniquePredicate(constructors) { c: ConstructorDeclaration -> c.name == "I" }
        val literal10 = findByUniquePredicate(result.literals) { it.value == 10 }
        val i1 = assertNotNull(variables["i1", UNIQUE])
        assertTrue(i1.initializer is ConstructExpression)

        val i1Constructor = i1.initializer as ConstructExpression
        assertFalse(i1Constructor.isImplicit)
        assertEquals(implicitConstructor, i1Constructor.constructor)
        assertEquals(1, i1Constructor.arguments.size)
        assertTrue(i1Constructor.arguments[0] is CastExpression)

        val i1ConstructorArgument = i1Constructor.arguments[0] as CastExpression
        assertEquals("int", i1ConstructorArgument.castType.name)
        assertEquals("1.0", i1ConstructorArgument.expression.code)
        assertEquals("double", i1ConstructorArgument.expression.type.name)

        val implicitConstructorWithDefault =
            findByUniquePredicate(constructors) { c: ConstructorDeclaration -> c.name == "H" }
        val h1 = assertNotNull(variables["h1", UNIQUE])
        assertTrue(h1.initializer is ConstructExpression)

        val h1Constructor = h1.initializer as ConstructExpression
        assertFalse(h1Constructor.isImplicit)
        assertEquals(implicitConstructorWithDefault, h1Constructor.constructor)
        assertEquals(1, h1Constructor.arguments.size)
        assertTrue(h1Constructor.arguments[0] is CastExpression)

        val h1ConstructorArgument1 = h1Constructor.arguments[0] as CastExpression
        assertEquals("int", h1ConstructorArgument1.castType.name)
        assertEquals("2.0", h1ConstructorArgument1.expression.code)
        assertEquals("double", h1ConstructorArgument1.expression.type.name)
        assertTrue(implicitConstructorWithDefault.nextEOG.contains(literal10))
        for (node in implicitConstructorWithDefault.nextEOG) {
            if (node != literal10) {
                assertTrue(literal10.nextEOG.contains(node))
            }
        }
    }
}
