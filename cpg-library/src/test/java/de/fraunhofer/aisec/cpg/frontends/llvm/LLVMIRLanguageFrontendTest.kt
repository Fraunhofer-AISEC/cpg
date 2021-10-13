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
package de.fraunhofer.aisec.cpg.frontends.llvm

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.Test

class LLVMIRLanguageFrontendTest {
    @Test
    fun test1() {
        val topLevel = Path.of("src", "test", "resources", "llvm")

        val frontend =
            LLVMIRLanguageFrontend(TranslationConfiguration.builder().build(), ScopeManager())
        frontend.parse(topLevel.resolve("main.ll").toFile())
    }

    @Test
    fun test2() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("integer_ops.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertEquals(2, tu.declarations.size)

        val main =
            tu.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(main)
        assertEquals("i32", main.type.name)

        val rand =
            tu.getDeclarationsByName("rand", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(rand)
        assertNull(rand.body)

        val stmt = main.getBodyStatementAs(0, DeclarationStatement::class.java)
        assertNotNull(stmt)

        val decl = stmt.singleDeclaration as? VariableDeclaration
        assertNotNull(decl)
        assertEquals("x", decl.name)

        val call = decl.initializer as? CallExpression
        assertNotNull(call)
        assertEquals("rand", call.name)
        assertTrue(call.invokes.contains(rand))
    }

    @Test
    fun testIdentifiedStruct() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("struct.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val rt =
            tu.getDeclarationsByName("struct.RT", RecordDeclaration::class.java).iterator().next()
        assertNotNull(rt)

        val st =
            tu.getDeclarationsByName("struct.ST", RecordDeclaration::class.java).iterator().next()
        assertNotNull(st)

        assertEquals(3, st.fields.size)

        var field = st.fields.firstOrNull()
        assertNotNull(field)
        assertEquals("i32", field.type.name)

        field = st.fields[1]
        assertNotNull(field)
        assertEquals("double", field.type.name)

        field = st.fields[2]
        assertNotNull(field)
        assertEquals("struct.RT", field.type.name)
        assertSame(rt, (field.type as? ObjectType)?.recordDeclaration)

        val foo = tu.getDeclarationsByName("foo", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(foo)

        val s = foo.parameters.firstOrNull { it.name == "s" }
        assertNotNull(s)

        val arrayidx =
            foo.getBodyStatementAs(0, DeclarationStatement::class.java)?.singleDeclaration as?
                VariableDeclaration
        assertNotNull(arrayidx)

        // arrayidx will be assigned to a chain of the following expressions:
        // &s[1].field2.field1[5][13]
        // we will check them in the reverse order (after the unary operator)

        val unary = arrayidx.initializer as? UnaryOperator
        assertNotNull(unary)
        assertEquals("&", unary.operatorCode)

        var arrayExpr = unary.input as? ArraySubscriptionExpression
        assertNotNull(arrayExpr)
        assertEquals("13", arrayExpr.name)
        assertEquals(
            13L,
            (arrayExpr.subscriptExpression as? Literal<*>)?.value
        ) // should this be integer instead of long?

        arrayExpr = arrayExpr.arrayExpression as? ArraySubscriptionExpression
        assertNotNull(arrayExpr)
        assertEquals("5", arrayExpr.name)
        assertEquals(
            5L,
            (arrayExpr.subscriptExpression as? Literal<*>)?.value
        ) // should this be integer instead of long?

        var memberExpr = arrayExpr.arrayExpression as? MemberExpression
        assertNotNull(memberExpr)
        assertEquals("field_1", memberExpr.name)

        memberExpr = memberExpr.base as? MemberExpression
        assertNotNull(memberExpr)
        assertEquals("field_2", memberExpr.name)

        arrayExpr = memberExpr.base as? ArraySubscriptionExpression
        assertNotNull(arrayExpr)
        assertEquals("1", arrayExpr.name)
        assertEquals(
            1L,
            (arrayExpr.subscriptExpression as? Literal<*>)?.value
        ) // should this be integer instead of long?

        val ref = arrayExpr.arrayExpression as? DeclaredReferenceExpression
        assertNotNull(ref)
        assertEquals("s", ref.name)
        assertSame(s, ref.refersTo)
    }

    @Test
    fun test4() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("switch_case.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }
    }

    @Test
    fun test5() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("br.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }
    }

    @Test
    fun test6() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("atomicrmw.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }
    }

    @Test
    fun testLiteralStruct() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("literal_struct.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)
    }
}
