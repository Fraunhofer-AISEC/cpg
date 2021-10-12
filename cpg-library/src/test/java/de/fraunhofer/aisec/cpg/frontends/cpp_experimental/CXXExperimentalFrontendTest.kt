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
package de.fraunhofer.aisec.cpg.frontends.cpp_experimental

import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import org.junit.jupiter.api.Test

class CXXExperimentalFrontendTest {
    @Test
    fun testSimple() {
        val file = File("src/test/resources/simple.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXExperimentalFrontend::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        assertNotNull(tu)
        assertEquals(2, tu.declarations.size)

        val someFunc =
            tu.getDeclarationsByName("someFunc", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(someFunc)
        assertEquals("someFunc", someFunc.name)
        assertEquals(1, someFunc.parameters.size)

        val a = someFunc.parameters.firstOrNull { it.name == "a" }
        assertNotNull(a)
        assertEquals("int", a.type.typeName)

        val r = someFunc.getBodyStatementAs(0, ReturnStatement::class.java)
        assertNotNull(r)

        val binOp = r.returnValue as? BinaryOperator
        assertNotNull(binOp)

        val ref = binOp.lhs as? DeclaredReferenceExpression
        assertNotNull(ref)
        assertEquals("a", ref.name)
        assertSame(a, ref.refersTo)

        val literal = binOp.rhs as? Literal<*>
        assertNotNull(literal)
        assertEquals(1, literal.value)
    }
}
