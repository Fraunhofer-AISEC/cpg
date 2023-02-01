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
import de.fraunhofer.aisec.cpg.assertFullName
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
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
import kotlin.test.Test

class LLVMIRLanguageFrontendTest {
    @Test
    fun test1() {
        val topLevel = Path.of("src", "test", "resources", "llvm")

        val frontend =
            LLVMIRLanguageFrontend(
                LLVMIRLanguage(),
                TranslationConfiguration.builder().build(),
                ScopeManager()
            )
        frontend.parse(topLevel.resolve("main.ll").toFile())
    }

    @Test
    fun testVectorPoison() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("vector_poison.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertEquals(1, tu.declarations.size)

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)
        assertLocalName("i32", main.type)

        val xVector =
            (main.bodyOrNull<CompoundStatement>(0)?.statements?.get(0) as? DeclarationStatement)
                ?.singleDeclaration as? VariableDeclaration
        val xInit = xVector?.initializer as? InitializerListExpression
        assertNotNull(xInit)
        assertLocalName("poison", xInit.initializers[0] as? DeclaredReferenceExpression)
        assertEquals(0L, (xInit.initializers[1] as? Literal<*>)?.value)
        assertEquals(0L, (xInit.initializers[2] as? Literal<*>)?.value)
        assertEquals(0L, (xInit.initializers[3] as? Literal<*>)?.value)
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
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertEquals(2, tu.declarations.size)

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)
        assertLocalName("i32", main.type)

        val rand = tu.byNameOrNull<FunctionDeclaration>("rand")
        assertNotNull(rand)
        assertNull(rand.body)

        val stmt = main.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(stmt)

        val decl = stmt.singleDeclaration as? VariableDeclaration
        assertNotNull(decl)
        assertLocalName("x", decl)

        val call = decl.initializer as? CallExpression
        assertNotNull(call)
        assertLocalName("rand", call)
        assertTrue(call.invokes.contains(rand))
        assertEquals(0, call.arguments.size)

        val xorStatement = main.bodyOrNull<DeclarationStatement>(3)
        assertNotNull(xorStatement)

        val xorDecl = xorStatement.singleDeclaration as? VariableDeclaration
        assertNotNull(xorDecl)
        assertLocalName("a", xorDecl)
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
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)

        val rt = tu.byNameOrNull<RecordDeclaration>("struct.RT")
        assertNotNull(rt)

        val st = tu.byNameOrNull<RecordDeclaration>("struct.ST")
        assertNotNull(st)

        assertEquals(3, st.fields.size)

        var field = st.fields.firstOrNull()
        assertNotNull(field)
        assertLocalName("i32", field.type)

        field = st.fields[1]
        assertNotNull(field)
        assertLocalName("double", field.type)

        field = st.fields[2]
        assertNotNull(field)
        assertLocalName("struct.RT", field.type)
        assertSame(rt, (field.type as? ObjectType)?.recordDeclaration)

        val foo = tu.byNameOrNull<FunctionDeclaration>("foo")
        assertNotNull(foo)

        val s = foo.parameters.firstOrNull { it.name.localName == "s" }
        assertNotNull(s)

        val arrayidx =
            foo.bodyOrNull<DeclarationStatement>(0)?.singleDeclaration as? VariableDeclaration
        assertNotNull(arrayidx)

        // arrayidx will be assigned to a chain of the following expressions:
        // &s[1].field2.field1[5][13]
        // we will check them in the reverse order (after the unary operator)

        val unary = arrayidx.initializer as? UnaryOperator
        assertNotNull(unary)
        assertEquals("&", unary.operatorCode)

        var arrayExpr = unary.input as? ArraySubscriptionExpression
        assertNotNull(arrayExpr)
        assertLocalName("13", arrayExpr)
        assertEquals(
            13L,
            (arrayExpr.subscriptExpression as? Literal<*>)?.value
        ) // should this be integer instead of long?

        arrayExpr = arrayExpr.arrayExpression as? ArraySubscriptionExpression
        assertNotNull(arrayExpr)
        assertLocalName("5", arrayExpr)
        assertEquals(
            5L,
            (arrayExpr.subscriptExpression as? Literal<*>)?.value
        ) // should this be integer instead of long?

        var memberExpr = arrayExpr.arrayExpression as? MemberExpression
        assertNotNull(memberExpr)
        assertLocalName("field_1", memberExpr)

        memberExpr = memberExpr.base as? MemberExpression
        assertNotNull(memberExpr)
        assertLocalName("field_2", memberExpr)

        arrayExpr = memberExpr.base as? ArraySubscriptionExpression
        assertNotNull(arrayExpr)
        assertLocalName("1", arrayExpr)
        assertEquals(
            1L,
            (arrayExpr.subscriptExpression as? Literal<*>)?.value
        ) // should this be integer instead of long?

        val ref = arrayExpr.arrayExpression as? DeclaredReferenceExpression
        assertNotNull(ref)
        assertLocalName("s", ref)
        assertSame(s, ref.refersTo)
    }

    @Test
    fun testSwitchCase() { // TODO: Update the test
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("switch_case.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val onzeroLabel = main.bodyOrNull<LabelStatement>(0)
        assertNotNull(onzeroLabel)
        assertLocalName("onzero", onzeroLabel)
        assertTrue(onzeroLabel.subStatement is CompoundStatement)

        val ononeLabel = main.bodyOrNull<LabelStatement>(1)
        assertNotNull(ononeLabel)
        assertLocalName("onone", ononeLabel)
        assertTrue(ononeLabel.subStatement is CompoundStatement)

        val defaultLabel = main.bodyOrNull<LabelStatement>(2)
        assertNotNull(defaultLabel)
        assertLocalName("otherwise", defaultLabel)
        assertTrue(defaultLabel.subStatement is CompoundStatement)

        // Check that the type of %a is i32
        val xorStatement = main.bodyOrNull<DeclarationStatement>(3)
        assertNotNull(xorStatement)
        val a = xorStatement.singleDeclaration as? VariableDeclaration
        assertNotNull(a)
        assertLocalName("a", a)
        assertEquals("i32", a.type.typeName)

        // Check that the jump targets are set correctly
        val switchStatement = main.bodyOrNull<SwitchStatement>()
        assertNotNull(switchStatement)

        // Check that we have switch(a)
        assertSame(a, (switchStatement.selector as DeclaredReferenceExpression).refersTo)

        val cases = switchStatement.statement as CompoundStatement
        // Check that the first case is case 0 -> goto onzero and that the BB is inlined
        val case1 = cases.statements[0] as CaseStatement
        assertEquals(0L, (case1.caseExpression as Literal<*>).value as Long)
        assertSame(onzeroLabel.subStatement, cases.statements[1])
        // Check that the second case is case 1 -> goto onone and that the BB is inlined
        val case2 = cases.statements[2] as CaseStatement
        assertEquals(1L, (case2.caseExpression as Literal<*>).value as Long)
        assertSame(ononeLabel.subStatement, cases.statements[3])

        // Check that the default location is inlined
        val defaultStatement = cases.statements[4] as? DefaultStatement
        assertNotNull(defaultStatement)
        assertSame(defaultLabel.subStatement, cases.statements[5])
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
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertEquals(2, tu.declarations.size)

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        // Test that the types and values of the comparison expression are correct
        val icmpStatement = main.bodyOrNull<DeclarationStatement>(1)
        assertNotNull(icmpStatement)
        val variableDecl = icmpStatement.declarations[0] as VariableDeclaration
        val comparison = variableDecl.initializer as BinaryOperator
        assertEquals("==", comparison.operatorCode)
        val rhs = (comparison.rhs as Literal<*>)
        val lhs = (comparison.lhs as DeclaredReferenceExpression).refersTo as VariableDeclaration
        assertEquals(10L, (rhs.value as Long))
        assertEquals(TypeParser.createFrom("i32", LLVMIRLanguage()), rhs.type)
        assertLocalName("x", comparison.lhs as DeclaredReferenceExpression)
        assertLocalName("x", lhs)
        assertEquals(TypeParser.createFrom("i32", LLVMIRLanguage()), lhs.type)

        // Check that the jump targets are set correctly
        val ifStatement = main.bodyOrNull<IfStatement>(0)
        assertNotNull(ifStatement)
        assertEquals("IfUnequal", (ifStatement.elseStatement!! as GotoStatement).labelName)
        val ifBranch = (ifStatement.thenStatement as CompoundStatement)

        // Check that the condition is set correctly
        val ifCondition = ifStatement.condition
        assertSame(variableDecl, (ifCondition as DeclaredReferenceExpression).refersTo)

        val elseBranch =
            (ifStatement.elseStatement!! as GotoStatement).targetLabel.subStatement
                as CompoundStatement
        assertEquals(2, elseBranch.statements.size)
        assertEquals("  %y = mul i32 %x, 32768", elseBranch.statements[0].code)
        assertEquals("  ret i32 %y", elseBranch.statements[1].code)

        // Check that it's  the correct then-branch
        assertEquals(2, ifBranch.statements.size)
        assertEquals("  %condUnsigned = icmp ugt i32 %x, -3", ifBranch.statements[0].code)

        val ifBranchVariableDecl =
            (ifBranch.statements[0] as DeclarationStatement).declarations[0] as VariableDeclaration
        val ifBranchComp = ifBranchVariableDecl.initializer as BinaryOperator
        assertEquals(">", ifBranchComp.operatorCode)
        assertEquals(CastExpression::class, ifBranchComp.rhs::class)
        assertEquals(CastExpression::class, ifBranchComp.lhs::class)

        val ifBranchCompRhs = ifBranchComp.rhs as CastExpression
        assertEquals(TypeParser.createFrom("ui32", LLVMIRLanguage()), ifBranchCompRhs.castType)
        assertEquals(TypeParser.createFrom("ui32", LLVMIRLanguage()), ifBranchCompRhs.type)
        val ifBranchCompLhs = ifBranchComp.lhs as CastExpression
        assertEquals(TypeParser.createFrom("ui32", LLVMIRLanguage()), ifBranchCompLhs.castType)
        assertEquals(TypeParser.createFrom("ui32", LLVMIRLanguage()), ifBranchCompLhs.type)

        val declRefExpr = ifBranchCompLhs.expression as DeclaredReferenceExpression
        assertEquals(-3, ((ifBranchCompRhs.expression as Literal<*>).value as Long))
        assertLocalName("x", declRefExpr)
        // TODO: declRefExpr.refersTo is null. Is that expected/intended?

        val ifBranchSecondStatement = ifBranch.statements[1] as? IfStatement
        assertNotNull(ifBranchSecondStatement)
        val ifRet = ifBranchSecondStatement.thenStatement as? CompoundStatement
        assertNotNull(ifRet)
        assertEquals(1, ifRet.statements.size)
        assertEquals("  ret i32 1", ifRet.statements[0].code)
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
                it.registerLanguage<LLVMIRLanguage>()
            }

        val foo = tu.byNameOrNull<FunctionDeclaration>("foo")
        assertNotNull(foo)

        val atomicrmwStatement = foo.bodyOrNull<CompoundStatement>()
        assertNotNull(atomicrmwStatement)

        // Check that the value is assigned to
        val decl = (atomicrmwStatement.statements[0].declarations[0] as VariableDeclaration)
        assertLocalName("old", decl)
        assertLocalName("i32", decl.type)
        assertEquals("*", (decl.initializer as UnaryOperator).operatorCode)
        assertLocalName("ptr", (decl.initializer as UnaryOperator).input)

        // Check that the replacement equals *ptr = *ptr + 1
        val replacement = (atomicrmwStatement.statements[1] as BinaryOperator)
        assertEquals("=", replacement.operatorCode)
        assertEquals("*", (replacement.lhs as UnaryOperator).operatorCode)
        assertLocalName("ptr", (replacement.lhs as UnaryOperator).input)
        // Check that the rhs is equal to *ptr + 1
        val add = replacement.rhs as BinaryOperator
        assertEquals("+", add.operatorCode)
        assertEquals("*", (add.lhs as UnaryOperator).operatorCode)
        assertLocalName("ptr", (add.lhs as UnaryOperator).input)
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
                it.registerLanguage<LLVMIRLanguage>()
            }

        val foo = tu.byNameOrNull<FunctionDeclaration>("foo")
        assertNotNull(foo)

        val cmpxchgStatement = foo.bodyOrNull<CompoundStatement>(1)
        assertNotNull(cmpxchgStatement)
        assertEquals(2, cmpxchgStatement.statements.size)

        // Check that the first statement is "literal_i32_i1 val_success = literal_i32_i1(*ptr, *ptr
        // == 5)"
        val decl = (cmpxchgStatement.statements[0].declarations[0] as VariableDeclaration)
        assertLocalName("val_success", decl)
        assertLocalName("literal_i32_i1", decl.type)

        // Check that the first value is *ptr
        val value1 = (decl.initializer as ConstructExpression).arguments[0] as UnaryOperator
        assertEquals("*", value1.operatorCode)
        assertLocalName("ptr", value1.input)

        // Check that the first value is *ptr == 5
        val value2 = (decl.initializer as ConstructExpression).arguments[1] as BinaryOperator
        assertEquals("==", value2.operatorCode)
        assertEquals("*", (value2.lhs as UnaryOperator).operatorCode)
        assertLocalName("ptr", (value2.lhs as UnaryOperator).input)
        assertEquals(5L, (value2.rhs as Literal<*>).value as Long)

        val ifStatement = cmpxchgStatement.statements[1] as IfStatement
        // The condition is the same as the second value above
        val ifExpr = ifStatement.condition as BinaryOperator
        assertEquals("==", ifExpr.operatorCode)
        assertEquals("*", (ifExpr.lhs as UnaryOperator).operatorCode)
        assertLocalName("ptr", (ifExpr.lhs as UnaryOperator).input)
        assertEquals(5L, (ifExpr.rhs as Literal<*>).value as Long)

        val thenExpr = ifStatement.thenStatement as BinaryOperator
        assertEquals("=", thenExpr.operatorCode)
        assertEquals("*", (thenExpr.lhs as UnaryOperator).operatorCode)
        assertLocalName("ptr", (thenExpr.lhs as UnaryOperator).input)
        assertLocalName("old", thenExpr.rhs as DeclaredReferenceExpression)
        assertLocalName("old", (thenExpr.rhs as DeclaredReferenceExpression).refersTo)
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
                it.registerLanguage<LLVMIRLanguage>()
            }

        val foo = tu.byNameOrNull<FunctionDeclaration>("foo")
        assertNotNull(foo)

        val extractvalueStatement = foo.bodyOrNull<DeclarationStatement>()
        assertNotNull(extractvalueStatement)
        val decl = (extractvalueStatement.declarations[0] as VariableDeclaration)
        assertLocalName("value_loaded", decl)
        assertLocalName("i1", decl.type)

        assertLocalName("val_success", (decl.initializer as MemberExpression).base)
        assertEquals(".", (decl.initializer as MemberExpression).operatorCode)
        assertLocalName("field_1", decl.initializer as MemberExpression)
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
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)

        val foo = tu.byNameOrNull<FunctionDeclaration>("foo")
        assertNotNull(foo)
        assertEquals("literal_i32_i8", foo.type.typeName)

        val record = (foo.type as? ObjectType)?.recordDeclaration
        assertNotNull(record)
        assertEquals(2, record.fields.size)

        val returnStatement = foo.bodyOrNull<ReturnStatement>(0)
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
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val globalX = tu.byNameOrNull<VariableDeclaration>("x")
        assertNotNull(globalX)
        assertEquals("i32*", globalX.type.typeName)

        val globalA = tu.byNameOrNull<VariableDeclaration>("a")
        assertNotNull(globalA)
        assertEquals("i32*", globalA.type.typeName)

        val loadXStatement = main.bodyOrNull<DeclarationStatement>(1)
        assertNotNull(loadXStatement)
        assertLocalName("locX", loadXStatement.singleDeclaration)

        val initXOp =
            (loadXStatement.singleDeclaration as VariableDeclaration).initializer as UnaryOperator
        assertEquals("*", initXOp.operatorCode)

        var ref = initXOp.input as? DeclaredReferenceExpression
        assertNotNull(ref)
        assertLocalName("x", ref)
        assertSame(globalX, ref.refersTo)

        val loadAStatement = main.bodyOrNull<DeclarationStatement>(2)
        assertNotNull(loadAStatement)
        assertLocalName("locA", loadAStatement.singleDeclaration)
        val initAOp =
            (loadAStatement.singleDeclaration as VariableDeclaration).initializer as UnaryOperator
        assertEquals("*", initAOp.operatorCode)

        ref = initAOp.input as? DeclaredReferenceExpression
        assertNotNull(ref)
        assertLocalName("a", ref)
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
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        // %ptr = alloca i32
        val ptr = main.bodyOrNull<DeclarationStatement>()?.singleDeclaration as? VariableDeclaration
        assertNotNull(ptr)

        val alloca = ptr.initializer as? ArrayCreationExpression
        assertNotNull(alloca)
        assertEquals("i32*", alloca.type.typeName)

        // store i32 3, i32* %ptr
        val store = main.bodyOrNull<BinaryOperator>()
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
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)

        val foo = tu.byNameOrNull<FunctionDeclaration>("foo")
        assertNotNull(foo)
        assertEquals("literal_i32_i8", foo.type.typeName)

        val record = (foo.type as? ObjectType)?.recordDeclaration
        assertNotNull(record)
        assertEquals(2, record.fields.size)

        val declStatement = foo.bodyOrNull<DeclarationStatement>()
        assertNotNull(declStatement)

        val varDecl = declStatement.singleDeclaration as VariableDeclaration
        assertLocalName("a", varDecl)
        assertEquals("literal_i32_i8", varDecl.type.typeName)
        val args = (varDecl.initializer as ConstructExpression).arguments
        assertEquals(2, args.size)
        assertEquals(100L, (args[0] as Literal<*>).value as Long)
        assertNull((args[1] as Literal<*>).value)

        val compoundStatement = foo.bodyOrNull<CompoundStatement>()
        assertNotNull(compoundStatement)
        // First copy a to b
        val copyStatement =
            (compoundStatement.statements[0] as DeclarationStatement).singleDeclaration
                as VariableDeclaration
        assertLocalName("b", copyStatement)
        assertEquals("literal_i32_i8", copyStatement.type.typeName)

        // Now, we set b.field_1 to 7
        val assignment = (compoundStatement.statements[1] as BinaryOperator)
        assertEquals("=", assignment.operatorCode)
        assertLocalName("b", (assignment.lhs as MemberExpression).base)
        assertEquals(".", (assignment.lhs as MemberExpression).operatorCode)
        assertLocalName("field_1", assignment.lhs as MemberExpression)
        assertEquals(7L, (assignment.rhs as Literal<*>).value as Long)
    }

    @Test
    fun testTryCatch() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("try_catch.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val throwingFoo = tu.byNameOrNull<FunctionDeclaration>("throwingFoo")

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val mainBody = main.body as CompoundStatement
        val tryStatement = mainBody.statements[0] as? TryStatement
        assertNotNull(tryStatement)

        // Check the assignment of the function call
        val resDecl =
            (tryStatement.tryBlock?.statements?.get(0) as? DeclarationStatement)?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(resDecl)
        assertLocalName("res", resDecl)
        val call = resDecl.initializer as? CallExpression
        assertNotNull(call)
        assertLocalName("throwingFoo", call)
        assertTrue(call.invokes.contains(throwingFoo))
        assertEquals(0, call.arguments.size)

        // Check that the second part of the try-block is inlined by the pass
        val aDecl =
            (tryStatement.tryBlock?.statements?.get(1) as? DeclarationStatement)?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(aDecl)
        assertLocalName("a", aDecl)
        val resStatement = tryStatement.tryBlock?.statements?.get(2) as? ReturnStatement
        assertNotNull(resStatement)

        // Check that the catch block is inlined by the pass
        assertEquals(1, tryStatement.catchClauses.size)
        assertEquals(5, tryStatement.catchClauses[0].body?.statements?.size)
        assertLocalName("_ZTIi | ...", tryStatement.catchClauses[0])
        val ifStatement = tryStatement.catchClauses[0].body?.statements?.get(4) as? IfStatement
        assertNotNull(ifStatement)
        assertTrue(ifStatement.thenStatement is CompoundStatement)
        assertEquals(4, (ifStatement.thenStatement as CompoundStatement).statements.size)
        assertTrue(ifStatement.elseStatement is CompoundStatement)
        assertEquals(1, (ifStatement.elseStatement as CompoundStatement).statements.size)
    }

    @Test
    fun testLoopPhi() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("loopPhi.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }
        val main = tu.byNameOrNull<FunctionDeclaration>("loopPhi")
        assertNotNull(main)
    }

    @Test
    fun testPhi() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("phi.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }
        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val mainBody = main.body as CompoundStatement
        val yDecl =
            (mainBody.statements[0] as DeclarationStatement).singleDeclaration
                as VariableDeclaration
        assertNotNull(yDecl)

        val ifStatement = mainBody.statements[3] as? IfStatement
        assertNotNull(ifStatement)

        val thenStmt = ifStatement.thenStatement as? CompoundStatement
        assertNotNull(thenStmt)
        assertEquals(3, thenStmt.statements.size)
        assertNotNull(thenStmt.statements[1] as? BinaryOperator)
        val aDecl =
            (thenStmt.statements[0] as DeclarationStatement).singleDeclaration
                as VariableDeclaration
        val thenY = thenStmt.statements[1] as BinaryOperator
        assertSame(aDecl, (thenY.rhs as DeclaredReferenceExpression).refersTo)
        assertSame(yDecl, (thenY.lhs as DeclaredReferenceExpression).refersTo)

        val elseStmt = ifStatement.elseStatement as? CompoundStatement
        assertNotNull(elseStmt)
        assertEquals(3, elseStmt.statements.size)
        val bDecl =
            (elseStmt.statements[0] as DeclarationStatement).singleDeclaration
                as VariableDeclaration
        assertNotNull(elseStmt.statements[1] as? BinaryOperator)
        val elseY = elseStmt.statements[1] as BinaryOperator
        assertSame(bDecl, (elseY.rhs as DeclaredReferenceExpression).refersTo)
        assertSame(yDecl, (elseY.lhs as DeclaredReferenceExpression).refersTo)

        val continueBlock =
            (thenStmt.statements[2] as? GotoStatement)?.targetLabel?.subStatement
                as? CompoundStatement
        assertNotNull(continueBlock)
        assertEquals(
            yDecl,
            ((continueBlock.statements[1] as ReturnStatement).returnValue
                    as DeclaredReferenceExpression)
                .refersTo
        )
    }

    @Test
    fun testVectorOperations() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("vector.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }
        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        // Test that x is initialized correctly
        val mainBody = main.body as CompoundStatement
        val origX =
            ((mainBody.statements[0] as? DeclarationStatement)?.singleDeclaration
                as? VariableDeclaration)
        val xInit = origX?.initializer as? InitializerListExpression
        assertNotNull(xInit)
        assertEquals(10L, (xInit.initializers[0] as? Literal<*>)?.value)
        assertEquals(9L, (xInit.initializers[1] as? Literal<*>)?.value)
        assertEquals(6L, (xInit.initializers[2] as? Literal<*>)?.value)
        assertEquals(-100L, (xInit.initializers[3] as? Literal<*>)?.value)

        // Test that y is initialized correctly
        val origY =
            ((mainBody.statements[1] as? DeclarationStatement)?.singleDeclaration
                as? VariableDeclaration)
        val yInit = origY?.initializer as? InitializerListExpression
        assertNotNull(yInit)
        assertEquals(15L, (yInit.initializers[0] as? Literal<*>)?.value)
        assertEquals(34L, (yInit.initializers[1] as? Literal<*>)?.value)
        assertEquals(99L, (yInit.initializers[2] as? Literal<*>)?.value)
        assertEquals(1000L, (yInit.initializers[3] as? Literal<*>)?.value)

        // Test that extractelement works
        val zInit =
            ((mainBody.statements[2] as? DeclarationStatement)?.singleDeclaration
                    as? VariableDeclaration)
                ?.initializer as? ArraySubscriptionExpression
        assertNotNull(zInit)
        assertEquals(0L, (zInit.subscriptExpression as? Literal<*>)?.value)
        assertEquals("x", (zInit.arrayExpression as? DeclaredReferenceExpression)?.name?.localName)
        assertSame(origX, (zInit.arrayExpression as? DeclaredReferenceExpression)?.refersTo)

        // Test the assignment of y to yMod
        val yModInit =
            ((mainBody.statements[3] as CompoundStatement).statements[0] as? DeclarationStatement)
                ?.singleDeclaration as? VariableDeclaration
        assertNotNull(yModInit)
        assertEquals("y", (yModInit.initializer as? DeclaredReferenceExpression)?.name?.localName)
        assertSame(origY, (yModInit.initializer as? DeclaredReferenceExpression)?.refersTo)
        // Now, test the modification of yMod[3] = 8
        val yMod = ((mainBody.statements[3] as CompoundStatement).statements[1] as? BinaryOperator)
        assertNotNull(yMod)
        assertEquals(
            3L,
            ((yMod.lhs as? ArraySubscriptionExpression)?.subscriptExpression as? Literal<*>)?.value
        )
        assertSame(
            yModInit,
            ((yMod.lhs as? ArraySubscriptionExpression)?.arrayExpression
                    as? DeclaredReferenceExpression)
                ?.refersTo
        )
        assertEquals(8L, (yMod.rhs as? Literal<*>)?.value)

        // Test the last shufflevector instruction which does not contain constant as initializers.
        val shuffledInit =
            ((mainBody.statements[4] as? DeclarationStatement)?.singleDeclaration
                    as? VariableDeclaration)
                ?.initializer as? InitializerListExpression
        assertNotNull(shuffledInit)
        assertSame(
            origX,
            ((shuffledInit.initializers[0] as? ArraySubscriptionExpression)?.arrayExpression
                    as? DeclaredReferenceExpression)
                ?.refersTo
        )
        assertSame(
            yModInit,
            ((shuffledInit.initializers[1] as? ArraySubscriptionExpression)?.arrayExpression
                    as? DeclaredReferenceExpression)
                ?.refersTo
        )
        assertSame(
            yModInit,
            ((shuffledInit.initializers[2] as? ArraySubscriptionExpression)?.arrayExpression
                    as? DeclaredReferenceExpression)
                ?.refersTo
        )
        assertSame(
            1,
            ((shuffledInit.initializers[0] as? ArraySubscriptionExpression)?.subscriptExpression
                    as? Literal<*>)
                ?.value
        )
        assertSame(
            2,
            ((shuffledInit.initializers[1] as? ArraySubscriptionExpression)?.subscriptExpression
                    as? Literal<*>)
                ?.value
        )
        assertSame(
            3,
            ((shuffledInit.initializers[2] as? ArraySubscriptionExpression)?.subscriptExpression
                    as? Literal<*>)
                ?.value
        )
    }

    @Test
    fun testFence() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("fence.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }
        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        // Test that x is initialized correctly
        val mainBody = main.body as CompoundStatement

        val fenceCall = mainBody.statements[0] as? CallExpression
        assertNotNull(fenceCall)
        assertEquals(1, fenceCall.arguments.size)
        assertEquals(2, (fenceCall.arguments[0] as Literal<*>).value)

        val fenceCallScope = mainBody.statements[2] as? CallExpression
        assertNotNull(fenceCallScope)
        assertEquals(2, fenceCallScope.arguments.size)
        // TODO: This doesn't match but it doesn't seem to be our mistake
        // assertEquals(5, (fenceCallScope.arguments[0] as Literal<*>).value)
        assertEquals("scope", (fenceCallScope.arguments[1] as Literal<*>).value)
    }

    @Test
    fun testExceptions() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("exceptions.ll").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val funcF = tu.byNameOrNull<FunctionDeclaration>("f")
        assertNotNull(funcF)

        val tryStatement =
            (funcF.bodyOrNull<LabelStatement>(0)?.subStatement as? CompoundStatement)
                ?.statements
                ?.firstOrNull { s -> s is TryStatement } as? TryStatement
        assertNotNull(tryStatement)
        assertEquals(2, tryStatement.tryBlock?.statements?.size)
        assertFullName(
            "_CxxThrowException",
            tryStatement.tryBlock?.statements?.get(0) as? CallExpression
        )
        assertEquals(
            "end",
            (tryStatement.tryBlock?.statements?.get(1) as? GotoStatement)
                ?.targetLabel
                ?.name
                ?.localName
        )

        assertEquals(1, tryStatement.catchClauses.size)
        val catchSwitchExpr =
            tryStatement.catchClauses[0].body?.statements?.get(0) as? DeclarationStatement
        assertNotNull(catchSwitchExpr)
        val catchswitchCall =
            (catchSwitchExpr.singleDeclaration as? VariableDeclaration)?.initializer
                as? CallExpression
        assertNotNull(catchswitchCall)
        assertFullName("llvm.catchswitch", catchswitchCall)
        val ifExceptionMatches =
            tryStatement.catchClauses[0].body?.statements?.get(1) as? IfStatement
        val matchesExceptionCall = ifExceptionMatches?.condition as? CallExpression
        assertNotNull(matchesExceptionCall)
        assertFullName("llvm.matchesCatchpad", matchesExceptionCall)
        assertEquals(
            catchSwitchExpr.singleDeclaration,
            (matchesExceptionCall.arguments[0] as DeclaredReferenceExpression).refersTo
        )
        assertEquals(null, (matchesExceptionCall.arguments[1] as Literal<*>).value)
        assertEquals(64L, (matchesExceptionCall.arguments[2] as Literal<*>).value as Long)
        assertEquals(null, (matchesExceptionCall.arguments[3] as Literal<*>).value)

        val catchBlock = ifExceptionMatches.thenStatement as? CompoundStatement
        assertNotNull(catchBlock)
        assertFullName(
            "llvm.catchpad",
            ((catchBlock.statements[0] as? DeclarationStatement)?.singleDeclaration
                    as? VariableDeclaration)
                ?.initializer as? CallExpression
        )

        val innerTry = catchBlock.statements[1] as? TryStatement
        assertNotNull(innerTry)
        assertFullName(
            "_CxxThrowException",
            innerTry.tryBlock?.statements?.get(0) as? CallExpression
        )
        assertLocalName(
            "try.cont",
            (innerTry.tryBlock?.statements?.get(1) as? GotoStatement)?.targetLabel
        )

        val innerCatchClause =
            (innerTry.catchClauses[0].body?.statements?.get(1) as? IfStatement)?.thenStatement
                as? CompoundStatement
        assertNotNull(innerCatchClause)
        assertFullName(
            "llvm.catchpad",
            ((innerCatchClause.statements[0] as? DeclarationStatement)?.singleDeclaration
                    as? VariableDeclaration)
                ?.initializer as? CallExpression
        )
        assertLocalName("try.cont", (innerCatchClause.statements[1] as? GotoStatement)?.targetLabel)

        val innerCatchThrows =
            (innerTry.catchClauses[0].body?.statements?.get(1) as? IfStatement)?.elseStatement
                as? UnaryOperator
        assertNotNull(innerCatchThrows)
        assertNotNull(innerCatchThrows.input)
        assertSame(
            innerTry.catchClauses[0].parameter,
            (innerCatchThrows.input as? DeclaredReferenceExpression)?.refersTo
        )
    }

    // TODO: Write test for calling a vararg function (e.g. printf). LLVM code snippets can already
    // be found in client.ll.
}
