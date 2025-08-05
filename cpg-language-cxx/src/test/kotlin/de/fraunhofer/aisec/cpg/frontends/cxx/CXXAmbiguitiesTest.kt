/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.ProblemDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class CXXAmbiguitiesTest {
    /**
     * This test is somewhat tricky. CDT thinks that certain call expressions are function
     * declarations in function declarations (which is not possible, with the exception of lambdas).
     * The issue is that we cannot currently solve this ambiguity, but rather we can recognize it as
     * a [ProblemDeclaration] and make sure that it is contained to the local function and the rest
     * of the AST and its scope are not affected by it (too much).
     *
     * If we ever fix the ambiguity, this test will probably FAIL and needs to be adjusted.
     */
    @Test
    fun testCallVsFunctionDeclaration() {
        val file = File("src/test/resources/call_me_crazy.h")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        // we have 3 (record) declarations in our TU now. 1 of the original MyClass and two because
        // CDT thinks that "call" is the return type and "crazy" the type of the parameter. We infer
        // record declarations for all types, so we end up with 3 declarations here.
        assertEquals(3, tu.declarations.size)

        val myClass = tu.records["MyClass"]
        assertNotNull(myClass)

        val someFunction = myClass.innerMethods["someFunction"]
        assertNotNull(someFunction)

        // CDT now (incorrectly) thinks the first line is a declaration statement, when in reality
        // it should be a CallExpression. But we cannot fix that at the moment
        val crazy = someFunction.allChildren<DeclarationStatement>().firstOrNull()
        assertNotNull(crazy) // if we ever fix it, this will FAIL

        val problem = crazy.singleDeclaration as? ProblemDeclaration
        assertNotNull(problem)
        assertContains(problem.problem, "CDT")
    }

    /**
     * In CXX there is an ambiguity with the statement: "(A)(B);" 1) If A is a function pointer,
     * this is a [CallExpression] 2) If A is a type, this is a [CastExpression]
     */
    @Test
    fun testFunctionCallOrTypeCast() {
        val file = File("src/test/resources/function_ptr_or_type_cast.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        val mainFunc = tu.functions["main"]
        assertNotNull(mainFunc)

        val fooFunc = tu.functions["foo"]
        assertNotNull(fooFunc)

        val body = mainFunc.body
        assertIs<Block>(body)

        // First two Statements after declaration statement are CallExpressions
        val s1 = body.statements.getOrNull(1)
        assertIs<CallExpression>(s1)
        assertInvokes(s1, fooFunc)

        val s2 = body.statements.getOrNull(2)
        assertIs<CallExpression>(s2)
        assertInvokes(s2, fooFunc)

        // Last two Statements are CastExpressions
        val s3 = body.statements.getOrNull(3)
        assertIs<CastExpression>(s3)

        val s4 = body.statements.getOrNull(4)
        assertIs<CastExpression>(s4)
    }

    /**
     * In CXX there is an ambiguity with the statement: "(A.B)(C);" 1) If B is a method, this is a
     * [MemberCallExpression] 2) if B is a function pointer, this is a [CallExpression].
     *
     * Function pointer as a struct member are currently not supported in the cpg. This test case
     * will just ensure that there will be no crash when parsing such a statement. When adding this
     * functionality in the cpg, this test case must be adapted accordingly.
     */
    @Test
    fun testMethodOrFunction() {
        val file = File("src/test/resources/method_or_function_call.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        val mainFunc = tu.functions["main"]
        assertNotNull(mainFunc)

        val classA = tu.records["A"]
        assertNotNull(classA)

        val structB = tu.records["B"]
        assertNotNull(structB)
    }
}
