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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ProblemDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

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
        val tu = TestUtils.analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        assertNotNull(tu)

        // make sure we still have only one declaration in the file (the record)
        assertEquals(1, tu.declarations.size)

        val myClass = tu.byNameOrNull<RecordDeclaration>("MyClass")

        assertNotNull(myClass)

        val someFunction = myClass.byNameOrNull<MethodDeclaration>("someFunction")

        assertNotNull(someFunction)

        // CDT now (incorrectly) thinks the first line is a declaration statement, when in reality
        // it should be a CallExpression. But we cannot fix that at the moment
        val crazy = someFunction.bodyOrNull<DeclarationStatement>(0)

        assertNotNull(crazy) // if we ever fix it, this will FAIL

        val problem = crazy.singleDeclaration as? ProblemDeclaration
        assertNotNull(problem)
        assertContains(problem.problem, "CDT")
    }
}
