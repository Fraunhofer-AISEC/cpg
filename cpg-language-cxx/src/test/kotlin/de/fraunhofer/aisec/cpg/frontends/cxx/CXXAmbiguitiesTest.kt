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

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDecl
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDecl
import de.fraunhofer.aisec.cpg.graph.declarations.ProblemDecl
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDecl
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStmt
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpr
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CastExpr
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpr
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CXXAmbiguitiesTest {
    /**
     * This test is somewhat tricky. CDT thinks that certain call expressions are function
     * declarations in function declarations (which is not possible, with the exception of lambdas).
     * The issue is that we cannot currently solve this ambiguity, but rather we can recognize it as
     * a [ProblemDecl] and make sure that it is contained to the local function and the rest of the
     * AST and its scope are not affected by it (too much).
     *
     * If we ever fix the ambiguity, this test will probably FAIL and needs to be adjusted.
     */
    @Test
    fun testCallVsFunctionDeclaration() {
        val file = File("src/test/resources/call_me_crazy.h")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        assertNotNull(tu)

        // make sure we still have only one declaration in the file (the record)
        assertEquals(1, tu.declarations.size)

        val myClass = tu.byNameOrNull<RecordDecl>("MyClass")

        assertNotNull(myClass)

        val someFunction = myClass.byNameOrNull<MethodDecl>("someFunction")

        assertNotNull(someFunction)

        // CDT now (incorrectly) thinks the first line is a declaration statement, when in reality
        // it should be a CallExpression. But we cannot fix that at the moment
        val crazy = someFunction.bodyOrNull<DeclarationStmt>(0)

        assertNotNull(crazy) // if we ever fix it, this will FAIL

        val problem = crazy.singleDeclaration as? ProblemDecl
        assertNotNull(problem)
        assertContains(problem.problem, "CDT")
    }

    /**
     * In CXX there is an ambiguity with the statement: "(A)(B);" 1) If A is a function pointer,
     * this is a [CallExpr] 2) If A is a type, this is a [CastExpr]
     */
    @Test
    fun testFunctionCallOrTypeCast() {
        val file = File("src/test/resources/function_ptr_or_type_cast.c")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        assertNotNull(tu)

        val mainFunc = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(mainFunc)

        val fooFunc = tu.byNameOrNull<FunctionDecl>("foo")
        assertNotNull(fooFunc)

        // First two Statements are CallExpressions
        val s1 = mainFunc.getBodyStatementAs(1, CallExpr::class.java)
        assertNotNull(s1)
        assertEquals(fooFunc, s1.invokes.iterator().next())

        val s2 = mainFunc.getBodyStatementAs(2, CallExpr::class.java)
        assertNotNull(s2)
        assertEquals(fooFunc, s2.invokes.iterator().next())

        // Last two Statements are CastExpressions
        val s3 = mainFunc.getBodyStatementAs(3, CastExpr::class.java)
        assertNotNull(s3)

        val s4 = mainFunc.getBodyStatementAs(4, CastExpr::class.java)
        assertNotNull(s4)
    }

    /**
     * In CXX there is an ambiguity with the statement: "(A.B)(C);" 1) If B is a method, this is a
     * [MemberCallExpr] 2) if B is a function pointer, this is a [CallExpr].
     *
     * Function pointer as a struct member are currently not supported in the cpg. This test case
     * will just ensure that there will be no crash when parsing such a statement. When adding this
     * functionality in the cpg, this test case must be adapted accordingly.
     */
    @Test
    fun testMethodOrFunction() {
        val file = File("src/test/resources/method_or_function_call.cpp")
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        assertNotNull(tu)

        val mainFunc = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(mainFunc)

        val classA = tu.byNameOrNull<RecordDecl>("A")
        assertNotNull(classA)

        val structB = tu.byNameOrNull<RecordDecl>("B")
        assertNotNull(structB)
    }
}
