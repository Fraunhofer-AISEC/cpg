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
package de.fraunhofer.aisec.cpg.frontends.csharp.expressionhandler

import de.fraunhofer.aisec.cpg.frontends.csharp.CSharpLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.expressions.ExpressionList
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.New
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CollectionInitializerTest : BaseTest() {

    @Test
    fun collectionInitializerTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("CollectionInitializer.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        /**
         * List<int> numbers = new List<int> { 0, 1, 2 };
         *
         * translated into: List<int> __tmp = new List<int>(); __tmp.Add(0); __tmp.Add(1);
         * __tmp.Add(2); List<int> numbers = __tmp;
         */
        val simpleClass = tu.records["CollectionInitializer"]
        assertNotNull(simpleClass)

        val numbersField = simpleClass.fields["numbers"]
        assertNotNull(numbersField)

        val exprList = numbersField.initializer
        assertNotNull(exprList)
        assertIs<ExpressionList>(exprList)
        assertEquals(5, exprList.expressions.size)

        // DeclarationStatement: __tmp = new List<int>()
        val declStmt = exprList.expressions[0]
        assertIs<DeclarationStatement>(declStmt)
        assertTrue(declStmt.isImplicit)
        val tmpVar = declStmt.declarations.firstOrNull()
        assertNotNull(tmpVar)
        assertIs<Variable>(tmpVar)
        val newNode = tmpVar.initializer
        assertNotNull(newNode)
        assertIs<New>(newNode)

        // __tmp.Add(0)
        val add0 = exprList.expressions[1]
        assertIs<MemberCall>(add0)
        assertTrue(add0.isImplicit)
        assertEquals("Add", add0.name.localName)
        val add0Base = (add0.callee as MemberAccess).base
        assertIs<Reference>(add0Base)
        assertRefersTo(add0Base, tmpVar)
        assertEquals(1, add0.arguments.size)
        val arg0 = add0.arguments[0]
        assertIs<Literal<*>>(arg0)
        assertEquals(0, arg0.value)

        // __tmp.Add(1)
        val add1 = exprList.expressions[2]
        assertIs<MemberCall>(add1)
        assertEquals("Add", add1.name.localName)
        assertEquals(1, add1.arguments.size)
        val arg1 = add1.arguments[0]
        assertIs<Literal<*>>(arg1)
        assertEquals(1, arg1.value)

        // __tmp.Add(2)
        val add2 = exprList.expressions[3]
        assertIs<MemberCall>(add2)
        assertEquals("Add", add2.name.localName)
        assertEquals(1, add2.arguments.size)
        val arg2 = add2.arguments[0]
        assertIs<Literal<*>>(arg2)
        assertEquals(2, arg2.value)

        // Reference to __tmp
        val ref = exprList.expressions[4]
        assertIs<Reference>(ref)
        assertRefersTo(ref, tmpVar)
    }

    @Test
    fun complexCollectionInitializerTest() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("CollectionInitializer.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        /**
         * Dictionary<string, int> map = new Dictionary<string, int> { { "a", 1 }, { "b", 2 } };
         *
         * translated into: Dictionary<string, int> __tmp = new Dictionary<string, int>();
         * __tmp.Add("a", 1); __tmp.Add("b", 2); Dictionary<string, int> map = __tmp;
         */
        val dictClass = tu.records["ComplexCollectionInitializer"]
        assertNotNull(dictClass)

        val mapField = dictClass.fields["map"]
        assertNotNull(mapField)

        val exprList = mapField.initializer
        assertNotNull(exprList)
        assertIs<ExpressionList>(exprList)
        assertEquals(4, exprList.expressions.size)

        // DeclarationStatement: __tmp = new Dictionary<string, int>()
        val declStmt = exprList.expressions[0]
        assertIs<DeclarationStatement>(declStmt)
        val tmpVar = declStmt.declarations.firstOrNull()
        assertNotNull(tmpVar)
        assertIs<Variable>(tmpVar)

        // __tmp.Add("a", 1)
        val addA = exprList.expressions[1]
        assertIs<MemberCall>(addA)
        assertEquals("Add", addA.name.localName)
        assertEquals(2, addA.arguments.size)
        val keyA = addA.arguments[0]
        assertIs<Literal<*>>(keyA)
        assertEquals("a", keyA.value)
        val valA = addA.arguments[1]
        assertIs<Literal<*>>(valA)
        assertEquals(1, valA.value)

        // __tmp.Add("b", 2)
        val addB = exprList.expressions[2]
        assertIs<MemberCall>(addB)
        assertEquals("Add", addB.name.localName)
        assertEquals(2, addB.arguments.size)
        val keyB = addB.arguments[0]
        assertIs<Literal<*>>(keyB)
        assertEquals("b", keyB.value)
        val valB = addB.arguments[1]
        assertIs<Literal<*>>(valB)
        assertEquals(2, valB.value)

        // Reference to __tmp
        val ref = exprList.expressions[3]
        assertIs<Reference>(ref)
        assertRefersTo(ref, tmpVar)
    }
}
