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
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
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
    fun testIntegerOps() {
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

        val xorStatement = main.getBodyStatementAs(3, DeclarationStatement::class.java)
        assertNotNull(xorStatement)

        val xorDecl = xorStatement.singleDeclaration as? VariableDeclaration
        assertNotNull(xorDecl)
        assertEquals("a", xorDecl.name)
        assertEquals("i32", xorDecl.type.typeName)

        val xor = xorDecl.initializer as? BinaryOperator
        assertNotNull(xor)
        assertEquals("^", xor.operatorCode)
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
    fun testSwitchCase() {
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

        val main =
            tu.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(main)

        // Check that the type of %a is i32
        val xorStatement = main.getBodyStatementAs(3, DeclarationStatement::class.java)
        assertNotNull(xorStatement)
        val xorDecl = xorStatement.singleDeclaration as? VariableDeclaration
        assertNotNull(xorDecl)
        assertEquals("a", xorDecl.name)
        assertEquals("i32", xorDecl.type.typeName)

        // Check that the jump targets are set correctly
        val brStatement = main.getBodyStatementAs(4, ConditionalBranchStatement::class.java)
        assertNotNull(brStatement)
        assertEquals("otherwise", brStatement.defaultTargetLabel.name)

        var onzeroLabel = brStatement.conditionalTargets[0].second
        assertEquals("onzero", onzeroLabel.name)

        assertEquals("onzero", brStatement.conditionalTargets[0].second.name)
        assertEquals("onone", brStatement.conditionalTargets[1].second.name)

        // Check that the condition is set correctly
        val brCondition1 = brStatement.conditionalTargets[0].first
        assertEquals(BinaryOperator::class, brCondition1::class)
        assertEquals("==", (brCondition1 as BinaryOperator).operatorCode)
        assertEquals(0L, (brCondition1.rhs as Literal<*>).value as Long)
        assertEquals("a", (brCondition1.lhs as DeclaredReferenceExpression).name)
        assertEquals("i32", (brCondition1.lhs as DeclaredReferenceExpression).type.typeName)
        // TODO: brCondition1.lhs.refersTo is null. Is that expected/intended?

        // Check that the condition is set correctly
        val brCondition2 = brStatement.conditionalTargets[1].first
        assertEquals(BinaryOperator::class, brCondition1::class)
        assertEquals("==", (brCondition2 as BinaryOperator).operatorCode)
        assertEquals(1L, (brCondition2.rhs as Literal<*>).value as Long)
        assertEquals("a", (brCondition2.lhs as DeclaredReferenceExpression).name)
        // TODO: brCondition2.lhs.refersTo is null. Is that expected/intended?
    }

    @Test
    fun testBrStatements() {
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

        assertEquals(2, tu.declarations.size)

        val main =
            tu.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(main)

        // Test that the types and values of the comparison expression are correct
        val icmpStatement = main.getBodyStatementAs(1, DeclarationStatement::class.java)
        assertNotNull(icmpStatement)
        val variableDecl = icmpStatement.declarations[0] as VariableDeclaration
        val comparison = variableDecl.initializer as BinaryOperator
        assertEquals("==", comparison.operatorCode)
        val rhs = (comparison.rhs as Literal<*>)
        val lhs = (comparison.lhs as DeclaredReferenceExpression).refersTo as VariableDeclaration
        assertEquals(10L, (rhs.value as Long))
        assertEquals(TypeParser.createFrom("i32", true), rhs.type)
        assertEquals("x", (comparison.lhs as DeclaredReferenceExpression).name)
        assertEquals("x", lhs.name)
        assertEquals(TypeParser.createFrom("i32", true), lhs.type)

        // Check that the jump targets are set correctly
        val brStatement = main.getBodyStatementAs(2, ConditionalBranchStatement::class.java)
        assertNotNull(brStatement)
        assertEquals("IfUnequal", brStatement.defaultTargetLabel.name)
        assertEquals("IfEqual", brStatement.conditionalTargets[0].second.name)

        // Check that the condition is set correctly
        val brCondition = brStatement.conditionalTargets[0].first
        assertEquals("cond", brCondition.name)
        assertEquals("i1", brCondition.type.typeName)

        val elseBranch = brStatement.defaultTargetLabel.subStatement as CompoundStatement
        assertEquals(2, elseBranch.statements.size)

        val ifBranch = brStatement.conditionalTargets[0].second.subStatement as CompoundStatement
        assertEquals(2, ifBranch.statements.size)

        val ifBranchVariableDecl =
            (ifBranch.statements[0] as DeclarationStatement).declarations[0] as VariableDeclaration
        val ifBranchComp = ifBranchVariableDecl.initializer as BinaryOperator
        assertEquals(">", ifBranchComp.operatorCode)
        assertEquals(CastExpression::class, ifBranchComp.rhs::class)
        assertEquals(CastExpression::class, ifBranchComp.lhs::class)

        val ifBranchCompRhs = ifBranchComp.rhs as CastExpression
        assertEquals(TypeParser.createFrom("ui32", true), ifBranchCompRhs.castType)
        assertEquals(TypeParser.createFrom("ui32", true), ifBranchCompRhs.type)
        val ifBranchCompLhs = ifBranchComp.lhs as CastExpression
        assertEquals(TypeParser.createFrom("ui32", true), ifBranchCompLhs.castType)
        assertEquals(TypeParser.createFrom("ui32", true), ifBranchCompLhs.type)

        val declRefExpr = ifBranchCompLhs.expression as DeclaredReferenceExpression
        assertEquals(-3, ((ifBranchCompRhs.expression as Literal<*>).value as Long))
        assertEquals("x", declRefExpr.name)
        // TODO: declRefExpr.refersTo is null. Is that expected/intended?
    }

    @Test
    fun testAtomicrmw() {
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

        val foo = tu.getDeclarationsByName("foo", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(foo)

        val atomicrmwStatement = foo.getBodyStatementAs(0, CompoundStatement::class.java)
        assertNotNull(atomicrmwStatement)

        // Check that the value is assigned to
        val decl = (atomicrmwStatement.statements[0].declarations[0] as VariableDeclaration)
        assertEquals("old", decl.name)
        assertEquals("i32", decl.type.typeName)
        assertEquals("*", (decl.initializer as UnaryOperator).operatorCode)
        assertEquals("ptr", (decl.initializer as UnaryOperator).input.name)

        // Check that the replacement equals *ptr = *ptr + 1
        val replacement = (atomicrmwStatement.statements[1] as BinaryOperator)
        assertEquals("=", replacement.operatorCode)
        assertEquals("*", (replacement.lhs as UnaryOperator).operatorCode)
        assertEquals("ptr", (replacement.lhs as UnaryOperator).input.name)
        // Check that the rhs is equal to *ptr + 1
        val add = replacement.rhs as BinaryOperator
        assertEquals("+", add.operatorCode)
        assertEquals("*", (add.lhs as UnaryOperator).operatorCode)
        assertEquals("ptr", (add.lhs as UnaryOperator).input.name)
        assertEquals(1L, (add.rhs as Literal<*>).value as Long)
    }

    @Test
    fun testCmpxchg() {
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

        val foo = tu.getDeclarationsByName("foo", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(foo)

        val cmpxchgStatement = foo.getBodyStatementAs(1, CompoundStatement::class.java)
        assertNotNull(cmpxchgStatement)
        assertEquals(2, cmpxchgStatement.statements.size)

        // Check that the first statement is "literal_i32_i1 val_success = literal_i32_i1(*ptr, *ptr
        // == 5)"
        val decl = (cmpxchgStatement.statements[0].declarations[0] as VariableDeclaration)
        assertEquals("val_success", decl.name)
        assertEquals("literal_i32_i1", decl.type.typeName)

        // Check that the first value is *ptr
        val value1 = (decl.initializer as ConstructExpression).arguments[0] as UnaryOperator
        assertEquals("*", value1.operatorCode)
        assertEquals("ptr", value1.input.name)

        // Check that the first value is *ptr == 5
        val value2 = (decl.initializer as ConstructExpression).arguments[1] as BinaryOperator
        assertEquals("==", value2.operatorCode)
        assertEquals("*", (value2.lhs as UnaryOperator).operatorCode)
        assertEquals("ptr", (value2.lhs as UnaryOperator).input.name)
        assertEquals(5L, (value2.rhs as Literal<*>).value as Long)

        val ifStatement = cmpxchgStatement.statements[1] as IfStatement
        // The condition is the same as the second value above
        val ifExpr = ifStatement.condition as BinaryOperator
        assertEquals("==", ifExpr.operatorCode)
        assertEquals("*", (ifExpr.lhs as UnaryOperator).operatorCode)
        assertEquals("ptr", (ifExpr.lhs as UnaryOperator).input.name)
        assertEquals(5L, (ifExpr.rhs as Literal<*>).value as Long)

        val thenExpr = ifStatement.thenStatement as BinaryOperator
        assertEquals("=", thenExpr.operatorCode)
        assertEquals("*", (thenExpr.lhs as UnaryOperator).operatorCode)
        assertEquals("ptr", (thenExpr.lhs as UnaryOperator).input.name)
        assertEquals("old", (thenExpr.rhs as DeclaredReferenceExpression).name)
        assertEquals("old", (thenExpr.rhs as DeclaredReferenceExpression).refersTo!!.name)
    }

    @Test
    fun testExtractvalue() {
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

        val foo = tu.getDeclarationsByName("foo", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(foo)

        val extractvalueStatement = foo.getBodyStatementAs(2, DeclarationStatement::class.java)
        assertNotNull(extractvalueStatement)
        val decl = (extractvalueStatement.declarations[0] as VariableDeclaration)
        assertEquals("value_loaded", decl.name)
        assertEquals("i1", decl.type.typeName)

        assertEquals("val_success", (decl.initializer as MemberExpression).base.name)
        assertEquals(".", (decl.initializer as MemberExpression).operatorCode)
        assertEquals("field_1", (decl.initializer as MemberExpression).name)
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

        val foo = tu.getDeclarationsByName("foo", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(foo)
        assertEquals("literal_i32_i8", foo.type.typeName)

        val record = (foo.type as? ObjectType)?.recordDeclaration
        assertNotNull(record)
        assertEquals(2, record.fields.size)

        val returnStatement = foo.getBodyStatementAs(0, ReturnStatement::class.java)
        assertNotNull(returnStatement)

        val construct = returnStatement.returnValue as? ConstructExpression
        assertNotNull(construct)
        assertEquals(2, construct.arguments.size)

        var arg = construct.arguments.getOrNull(0) as? Literal<*>
        assertNotNull(arg)
        assertEquals("i32", arg.type.typeName)
        assertEquals(4L, arg.value)

        arg = construct.arguments.getOrNull(1) as? Literal<*>
        assertNotNull(arg)
        assertEquals("i8", arg.type.typeName)
        assertEquals(2L, arg.value)
    }

    @Test
    fun testVariableScope() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("global_local_var.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val main =
            tu.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(main)

        val globalX =
            tu.getDeclarationsByName("x", VariableDeclaration::class.java).iterator().next()
        assertNotNull(globalX)
        assertEquals("i32*", globalX.type.typeName)

        val globalA =
            tu.getDeclarationsByName("a", VariableDeclaration::class.java).iterator().next()
        assertNotNull(globalA)
        assertEquals("i32*", globalA.type.typeName)

        val loadXStatement = main.getBodyStatementAs(1, DeclarationStatement::class.java)
        assertNotNull(loadXStatement)
        assertEquals("locX", loadXStatement.singleDeclaration.name)

        val initXOp =
            (loadXStatement.singleDeclaration as VariableDeclaration).initializer as UnaryOperator
        assertEquals("*", initXOp.operatorCode)

        var ref = initXOp.input as? DeclaredReferenceExpression
        assertNotNull(ref)
        assertEquals("x", ref.name)
        assertSame(globalX, ref.refersTo)

        val loadAStatement = main.getBodyStatementAs(2, DeclarationStatement::class.java)
        assertNotNull(loadAStatement)
        assertEquals("locA", loadAStatement.singleDeclaration.name)
        val initAOp =
            (loadAStatement.singleDeclaration as VariableDeclaration).initializer as UnaryOperator
        assertEquals("*", initAOp.operatorCode)

        ref = initAOp.input as? DeclaredReferenceExpression
        assertNotNull(ref)
        assertEquals("a", ref.name)
        assertSame(globalA, ref.refersTo)
    }

    @Test
    fun testAlloca() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("alloca.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val main =
            tu.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(main)

        // %ptr = alloca i32
        val ptr =
            main.getBodyStatementAs(0, DeclarationStatement::class.java)?.singleDeclaration as?
                VariableDeclaration
        assertNotNull(ptr)

        val alloca = ptr.initializer as? ArrayCreationExpression
        assertNotNull(alloca)
        assertEquals("i32*", alloca.type.typeName)

        // store i32 3, i32* %ptr
        val store = main.getBodyStatementAs(1, BinaryOperator::class.java)
        assertNotNull(store)
        assertEquals("=", store.operatorCode)

        val dereferencePtr = store.lhs as? UnaryOperator
        assertNotNull(dereferencePtr)
        assertEquals("*", dereferencePtr.operatorCode)
        assertEquals("i32", dereferencePtr.type.typeName)
        assertSame(ptr, (dereferencePtr.input as? DeclaredReferenceExpression)?.refersTo)

        val value = store.rhs as? Literal<*>
        assertNotNull(value)
        assertEquals(3L, value.value)
        assertEquals("i32", value.type.typeName)
    }

    @Test
    fun testUndefInsertvalue() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("undef_insertvalue.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    LLVMIRLanguageFrontend::class.java,
                    LLVMIRLanguageFrontend.LLVM_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val foo = tu.getDeclarationsByName("foo", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(foo)
        assertEquals("literal_i32_i8", foo.type.typeName)

        val record = (foo.type as? ObjectType)?.recordDeclaration
        assertNotNull(record)
        assertEquals(2, record.fields.size)

        val declStatement = foo.getBodyStatementAs(0, DeclarationStatement::class.java)
        assertNotNull(declStatement)

        val varDecl = declStatement.singleDeclaration as VariableDeclaration
        assertEquals("a", varDecl.name)
        assertEquals("literal_i32_i8", varDecl.type.typeName)
        val args = (varDecl.initializer as ConstructExpression).arguments
        assertEquals(2, args.size)
        assertEquals(100L, (args[0] as Literal<*>).value as Long)
        assertNull((args[1] as Literal<*>).value)

        val compoundStatement = foo.getBodyStatementAs(1, CompoundStatement::class.java)
        assertNotNull(compoundStatement)
        // First copy a to b
        val copyStatement =
            (compoundStatement.statements[0] as DeclarationStatement).singleDeclaration as
                VariableDeclaration
        assertEquals("b", copyStatement.name)
        assertEquals("literal_i32_i8", copyStatement.type.typeName)

        // Now, we set b.field_1 to 7
        val assignment = (compoundStatement.statements[1] as BinaryOperator)
        assertEquals("=", assignment.operatorCode)
        assertEquals("b", (assignment.lhs as MemberExpression).base.name)
        assertEquals(".", (assignment.lhs as MemberExpression).operatorCode)
        assertEquals("field_1", (assignment.lhs as MemberExpression).name)
        assertEquals(7L, (assignment.rhs as Literal<*>).value as Long)
    }
}
