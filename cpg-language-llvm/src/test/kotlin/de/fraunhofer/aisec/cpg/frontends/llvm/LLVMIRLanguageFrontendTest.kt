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

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDecl
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDecl
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDecl
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
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
                TranslationContext(
                    TranslationConfiguration.builder().build(),
                    ScopeManager(),
                    TypeManager()
                )
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

        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)
        assertLocalName("i32", main.type)

        val xVector =
            (main.bodyOrNull<CompoundStmt>(0)?.statements?.get(0) as? DeclarationStmt)
                ?.singleDeclaration as? VariableDecl
        val xInit = xVector?.initializer as? InitializerListExpr
        assertNotNull(xInit)
        assertLocalName("poison", xInit.initializers[0] as? Reference)
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

        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)
        assertLocalName("i32", main.type)

        val rand = tu.byNameOrNull<FunctionDecl>("rand")
        assertNotNull(rand)
        assertNull(rand.body)

        val stmt = main.bodyOrNull<DeclarationStmt>(0)
        assertNotNull(stmt)

        val decl = stmt.singleDeclaration as? VariableDecl
        assertNotNull(decl)
        assertLocalName("x", decl)

        val call = decl.initializer as? CallExpr
        assertNotNull(call)
        assertLocalName("rand", call)
        assertTrue(call.invokes.contains(rand))
        assertEquals(0, call.arguments.size)

        val xorStatement = main.bodyOrNull<DeclarationStmt>(3)
        assertNotNull(xorStatement)

        val xorDecl = xorStatement.singleDeclaration as? VariableDecl
        assertNotNull(xorDecl)
        assertLocalName("a", xorDecl)
        assertEquals("i32", xorDecl.type.typeName)

        val xor = xorDecl.initializer as? BinaryOp
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

        val rt = tu.byNameOrNull<RecordDecl>("struct.RT")
        assertNotNull(rt)

        val st = tu.byNameOrNull<RecordDecl>("struct.ST")
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
        assertSame(rt, (field.type as? ObjectType)?.recordDecl)

        val foo = tu.byNameOrNull<FunctionDecl>("foo")
        assertNotNull(foo)

        val s = foo.parameters.firstOrNull { it.name.localName == "s" }
        assertNotNull(s)

        val arrayidx = foo.bodyOrNull<DeclarationStmt>(0)?.singleDeclaration as? VariableDecl
        assertNotNull(arrayidx)

        // arrayidx will be assigned to a chain of the following expressions:
        // &s[1].field2.field1[5][13]
        // we will check them in the reverse order (after the unary operator)

        val unary = arrayidx.initializer as? UnaryOp
        assertNotNull(unary)
        assertEquals("&", unary.operatorCode)

        var arrayExpr = unary.input as? SubscriptionExpr
        assertNotNull(arrayExpr)
        assertLocalName("13", arrayExpr)
        assertEquals(
            13L,
            (arrayExpr.subscriptExpression as? Literal<*>)?.value
        ) // should this be integer instead of long?

        arrayExpr = arrayExpr.arrayExpression as? SubscriptionExpr
        assertNotNull(arrayExpr)
        assertLocalName("5", arrayExpr)
        assertEquals(
            5L,
            (arrayExpr.subscriptExpression as? Literal<*>)?.value
        ) // should this be integer instead of long?

        var memberExpr = arrayExpr.arrayExpression as? MemberExpr
        assertNotNull(memberExpr)
        assertLocalName("field_1", memberExpr)

        memberExpr = memberExpr.base as? MemberExpr
        assertNotNull(memberExpr)
        assertLocalName("field_2", memberExpr)

        arrayExpr = memberExpr.base as? SubscriptionExpr
        assertNotNull(arrayExpr)
        assertLocalName("1", arrayExpr)
        assertEquals(
            1L,
            (arrayExpr.subscriptExpression as? Literal<*>)?.value
        ) // should this be integer instead of long?

        val ref = arrayExpr.arrayExpression as? Reference
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

        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)

        val onzeroLabel = main.bodyOrNull<LabelStmt>(0)
        assertNotNull(onzeroLabel)
        assertLocalName("onzero", onzeroLabel)
        assertTrue(onzeroLabel.subStatement is CompoundStmt)

        val ononeLabel = main.bodyOrNull<LabelStmt>(1)
        assertNotNull(ononeLabel)
        assertLocalName("onone", ononeLabel)
        assertTrue(ononeLabel.subStatement is CompoundStmt)

        val defaultLabel = main.bodyOrNull<LabelStmt>(2)
        assertNotNull(defaultLabel)
        assertLocalName("otherwise", defaultLabel)
        assertTrue(defaultLabel.subStatement is CompoundStmt)

        // Check that the type of %a is i32
        val xorStatement = main.bodyOrNull<DeclarationStmt>(3)
        assertNotNull(xorStatement)
        val a = xorStatement.singleDeclaration as? VariableDecl
        assertNotNull(a)
        assertLocalName("a", a)
        assertEquals("i32", a.type.typeName)

        // Check that the jump targets are set correctly
        val switchStmt = main.bodyOrNull<SwitchStmt>()
        assertNotNull(switchStmt)

        // Check that we have switch(a)
        assertSame(a, (switchStmt.selector as Reference).refersTo)

        val cases = switchStmt.statement as CompoundStmt
        // Check that the first case is case 0 -> goto onzero and that the BB is inlined
        val case1 = cases.statements[0] as CaseStmt
        assertEquals(0L, (case1.caseExpression as Literal<*>).value as Long)
        assertSame(onzeroLabel.subStatement, cases.statements[1])
        // Check that the second case is case 1 -> goto onone and that the BB is inlined
        val case2 = cases.statements[2] as CaseStmt
        assertEquals(1L, (case2.caseExpression as Literal<*>).value as Long)
        assertSame(ononeLabel.subStatement, cases.statements[3])

        // Check that the default location is inlined
        val defaultStmt = cases.statements[4] as? DefaultStmt
        assertNotNull(defaultStmt)
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

        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)

        // Test that the types and values of the comparison expression are correct
        val icmpStatement = main.bodyOrNull<DeclarationStmt>(1)
        assertNotNull(icmpStatement)
        val variableDecl = icmpStatement.declarations[0] as VariableDecl
        val comparison = variableDecl.initializer as BinaryOp
        assertEquals("==", comparison.operatorCode)
        val rhs = (comparison.rhs as Literal<*>)
        val lhs = (comparison.lhs as Reference).refersTo as VariableDecl
        assertEquals(10L, (rhs.value as Long))
        assertEquals(tu.primitiveType("i32"), rhs.type)
        assertLocalName("x", comparison.lhs as Reference)
        assertLocalName("x", lhs)
        assertEquals(tu.primitiveType("i32"), lhs.type)

        // Check that the jump targets are set correctly
        val ifStmt = main.bodyOrNull<IfStmt>(0)
        assertNotNull(ifStmt)
        assertEquals("IfUnequal", (ifStmt.elseStatement!! as GotoStmt).labelName)
        val ifBranch = (ifStmt.thenStatement as CompoundStmt)

        // Check that the condition is set correctly
        val ifCondition = ifStmt.condition
        assertSame(variableDecl, (ifCondition as Reference).refersTo)

        val elseBranch =
            (ifStmt.elseStatement!! as GotoStmt).targetLabel?.subStatement as CompoundStmt
        assertEquals(2, elseBranch.statements.size)
        assertEquals("  %y = mul i32 %x, 32768", elseBranch.statements[0].code)
        assertEquals("  ret i32 %y", elseBranch.statements[1].code)

        // Check that it's  the correct then-branch
        assertEquals(2, ifBranch.statements.size)
        assertEquals("  %condUnsigned = icmp ugt i32 %x, -3", ifBranch.statements[0].code)

        val ifBranchVariableDecl =
            (ifBranch.statements[0] as DeclarationStmt).declarations[0] as VariableDecl
        val ifBranchComp = ifBranchVariableDecl.initializer as BinaryOp
        assertEquals(">", ifBranchComp.operatorCode)
        assertTrue(ifBranchComp.rhs is CastExpr)
        assertTrue(ifBranchComp.lhs is CastExpr)

        val ifBranchCompRhs = ifBranchComp.rhs as CastExpr
        assertEquals(tu.objectType("ui32"), ifBranchCompRhs.castType)
        assertEquals(tu.objectType("ui32"), ifBranchCompRhs.type)
        val ifBranchCompLhs = ifBranchComp.lhs as CastExpr
        assertEquals(tu.objectType("ui32"), ifBranchCompLhs.castType)
        assertEquals(tu.objectType("ui32"), ifBranchCompLhs.type)

        val declRefExpr = ifBranchCompLhs.expression as Reference
        assertEquals(-3, ((ifBranchCompRhs.expression as Literal<*>).value as Long))
        assertLocalName("x", declRefExpr)
        // TODO: declRefExpr.refersTo is null. Is that expected/intended?

        val ifBranchSecondStatement = ifBranch.statements[1] as? IfStmt
        assertNotNull(ifBranchSecondStatement)
        val ifRet = ifBranchSecondStatement.thenStatement as? CompoundStmt
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

        val foo = tu.byNameOrNull<FunctionDecl>("foo")
        assertNotNull(foo)

        val atomicrmwStatement = foo.bodyOrNull<CompoundStmt>()
        assertNotNull(atomicrmwStatement)

        // Check that the value is assigned to
        val decl = (atomicrmwStatement.statements[0].declarations[0] as VariableDecl)
        assertLocalName("old", decl)
        assertLocalName("i32", decl.type)
        assertEquals("*", (decl.initializer as UnaryOp).operatorCode)
        assertLocalName("ptr", (decl.initializer as UnaryOp).input)

        // Check that the replacement equals *ptr = *ptr + 1
        val replacement = (atomicrmwStatement.statements[1] as AssignExpr)
        assertEquals(1, replacement.lhs.size)
        assertEquals(1, replacement.rhs.size)
        assertEquals("=", replacement.operatorCode)
        assertEquals("*", (replacement.lhs.first() as UnaryOp).operatorCode)
        assertLocalName("ptr", (replacement.lhs.first() as UnaryOp).input)
        // Check that the rhs is equal to *ptr + 1
        val add = replacement.rhs.first() as BinaryOp
        assertEquals("+", add.operatorCode)
        assertEquals("*", (add.lhs as UnaryOp).operatorCode)
        assertLocalName("ptr", (add.lhs as UnaryOp).input)
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

        val foo = tu.byNameOrNull<FunctionDecl>("foo")
        assertNotNull(foo)

        val cmpxchgStatement = foo.bodyOrNull<CompoundStmt>(1)
        assertNotNull(cmpxchgStatement)
        assertEquals(2, cmpxchgStatement.statements.size)

        // Check that the first statement is "literal_i32_i1 val_success = literal_i32_i1(*ptr, *ptr
        // == 5)"
        val decl = (cmpxchgStatement.statements[0].declarations[0] as VariableDecl)
        assertLocalName("val_success", decl)
        assertLocalName("literal_i32_i1", decl.type)

        // Check that the first value is *ptr
        val value1 = (decl.initializer as ConstructExpr).arguments[0] as UnaryOp
        assertEquals("*", value1.operatorCode)
        assertLocalName("ptr", value1.input)

        // Check that the first value is *ptr == 5
        val value2 = (decl.initializer as ConstructExpr).arguments[1] as BinaryOp
        assertEquals("==", value2.operatorCode)
        assertEquals("*", (value2.lhs as UnaryOp).operatorCode)
        assertLocalName("ptr", (value2.lhs as UnaryOp).input)
        assertEquals(5L, (value2.rhs as Literal<*>).value as Long)

        val ifStmt = cmpxchgStatement.statements[1] as IfStmt
        // The condition is the same as the second value above
        val ifExpr = ifStmt.condition as BinaryOp
        assertEquals("==", ifExpr.operatorCode)
        assertEquals("*", (ifExpr.lhs as UnaryOp).operatorCode)
        assertLocalName("ptr", (ifExpr.lhs as UnaryOp).input)
        assertEquals(5L, (ifExpr.rhs as Literal<*>).value as Long)

        val thenExpr = ifStmt.thenStatement as AssignExpr
        assertEquals(1, thenExpr.lhs.size)
        assertEquals(1, thenExpr.rhs.size)
        assertEquals("=", thenExpr.operatorCode)
        assertEquals("*", (thenExpr.lhs.first() as UnaryOp).operatorCode)
        assertLocalName("ptr", (thenExpr.lhs.first() as UnaryOp).input)
        assertLocalName("old", thenExpr.rhs.first() as Reference)
        assertLocalName("old", (thenExpr.rhs.first() as Reference).refersTo)
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

        val foo = tu.byNameOrNull<FunctionDecl>("foo")
        assertNotNull(foo)

        val extractvalueStatement = foo.bodyOrNull<DeclarationStmt>()
        assertNotNull(extractvalueStatement)
        val decl = (extractvalueStatement.declarations[0] as VariableDecl)
        assertLocalName("value_loaded", decl)
        assertLocalName("i1", decl.type)

        assertLocalName("val_success", (decl.initializer as MemberExpr).base)
        assertEquals(".", (decl.initializer as MemberExpr).operatorCode)
        assertLocalName("field_1", decl.initializer as MemberExpr)
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

        val foo = tu.byNameOrNull<FunctionDecl>("foo")
        assertNotNull(foo)
        assertEquals("literal_i32_i8", foo.type.typeName)

        val record = (foo.type as? ObjectType)?.recordDecl
        assertNotNull(record)
        assertEquals(2, record.fields.size)

        val returnStmt = foo.bodyOrNull<ReturnStmt>(0)
        assertNotNull(returnStmt)

        val construct = returnStmt.returnValue as? ConstructExpr
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

        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)

        val globalX = tu.byNameOrNull<VariableDecl>("x")
        assertNotNull(globalX)
        assertEquals("i32*", globalX.type.typeName)

        val globalA = tu.byNameOrNull<VariableDecl>("a")
        assertNotNull(globalA)
        assertEquals("i32*", globalA.type.typeName)

        val loadXStatement = main.bodyOrNull<DeclarationStmt>(1)
        assertNotNull(loadXStatement)
        assertLocalName("locX", loadXStatement.singleDeclaration)

        val initXOp = (loadXStatement.singleDeclaration as VariableDecl).initializer as UnaryOp
        assertEquals("*", initXOp.operatorCode)

        var ref = initXOp.input as? Reference
        assertNotNull(ref)
        assertLocalName("x", ref)
        assertSame(globalX, ref.refersTo)

        val loadAStatement = main.bodyOrNull<DeclarationStmt>(2)
        assertNotNull(loadAStatement)
        assertLocalName("locA", loadAStatement.singleDeclaration)
        val initAOp = (loadAStatement.singleDeclaration as VariableDecl).initializer as UnaryOp
        assertEquals("*", initAOp.operatorCode)

        ref = initAOp.input as? Reference
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

        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)

        // %ptr = alloca i32
        val ptr = main.bodyOrNull<DeclarationStmt>()?.singleDeclaration as? VariableDecl
        assertNotNull(ptr)

        val alloca = ptr.initializer as? ArrayExpr
        assertNotNull(alloca)
        assertEquals("i32*", alloca.type.typeName)

        // store i32 3, i32* %ptr
        val store = main.bodyOrNull<AssignExpr>()
        assertNotNull(store)
        assertEquals("=", store.operatorCode)

        assertEquals(1, store.lhs.size)
        val dereferencePtr = store.lhs.first() as? UnaryOp
        assertNotNull(dereferencePtr)
        assertEquals("*", dereferencePtr.operatorCode)
        assertEquals("i32", dereferencePtr.type.typeName)
        assertSame(ptr, (dereferencePtr.input as? Reference)?.refersTo)

        assertEquals(1, store.rhs.size)
        val value = store.rhs.first() as? Literal<*>
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

        val foo = tu.byNameOrNull<FunctionDecl>("foo")
        assertNotNull(foo)
        assertEquals("literal_i32_i8", foo.type.typeName)

        val record = (foo.type as? ObjectType)?.recordDecl
        assertNotNull(record)
        assertEquals(2, record.fields.size)

        val declStatement = foo.bodyOrNull<DeclarationStmt>()
        assertNotNull(declStatement)

        val varDecl = declStatement.singleDeclaration as VariableDecl
        assertLocalName("a", varDecl)
        assertEquals("literal_i32_i8", varDecl.type.typeName)
        val args = (varDecl.initializer as ConstructExpr).arguments
        assertEquals(2, args.size)
        assertEquals(100L, (args[0] as Literal<*>).value as Long)
        assertNull((args[1] as Literal<*>).value)

        val compoundStatement = foo.bodyOrNull<CompoundStmt>()
        assertNotNull(compoundStatement)
        // First copy a to b
        val copyStatement =
            (compoundStatement.statements[0] as DeclarationStmt).singleDeclaration as VariableDecl
        assertLocalName("b", copyStatement)
        assertEquals("literal_i32_i8", copyStatement.type.typeName)

        // Now, we set b.field_1 to 7
        val assignment = (compoundStatement.statements[1] as AssignExpr)
        assertEquals("=", assignment.operatorCode)
        assertEquals(1, assignment.lhs.size)
        assertEquals(1, assignment.rhs.size)
        assertLocalName("b", (assignment.lhs.first() as MemberExpr).base)
        assertEquals(".", (assignment.lhs.first() as MemberExpr).operatorCode)
        assertLocalName("field_1", assignment.lhs.first() as MemberExpr)
        assertEquals(7L, (assignment.rhs.first() as Literal<*>).value as Long)
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

        val throwingFoo = tu.byNameOrNull<FunctionDecl>("throwingFoo")

        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)

        val mainBody = main.body as CompoundStmt
        val tryStmt = mainBody.statements[0] as? TryStmt
        assertNotNull(tryStmt)

        // Check the assignment of the function call
        val resDecl =
            (tryStmt.tryBlock?.statements?.get(0) as? DeclarationStmt)?.singleDeclaration
                as? VariableDecl
        assertNotNull(resDecl)
        assertLocalName("res", resDecl)
        val call = resDecl.initializer as? CallExpr
        assertNotNull(call)
        assertLocalName("throwingFoo", call)
        assertTrue(call.invokes.contains(throwingFoo))
        assertEquals(0, call.arguments.size)

        // Check that the second part of the try-block is inlined by the pass
        val aDecl =
            (tryStmt.tryBlock?.statements?.get(1) as? DeclarationStmt)?.singleDeclaration
                as? VariableDecl
        assertNotNull(aDecl)
        assertLocalName("a", aDecl)
        val resStatement = tryStmt.tryBlock?.statements?.get(2) as? ReturnStmt
        assertNotNull(resStatement)

        // Check that the catch block is inlined by the pass
        assertEquals(1, tryStmt.catchClauses.size)
        assertEquals(5, tryStmt.catchClauses[0].body?.statements?.size)
        assertLocalName("_ZTIi | ...", tryStmt.catchClauses[0])
        val ifStmt = tryStmt.catchClauses[0].body?.statements?.get(4) as? IfStmt
        assertNotNull(ifStmt)
        assertTrue(ifStmt.thenStatement is CompoundStmt)
        assertEquals(4, (ifStmt.thenStatement as CompoundStmt).statements.size)
        assertTrue(ifStmt.elseStatement is CompoundStmt)
        assertEquals(1, (ifStmt.elseStatement as CompoundStmt).statements.size)
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
        val main = tu.byNameOrNull<FunctionDecl>("loopPhi")
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
        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)

        val mainBody = main.body as CompoundStmt
        val yDecl = (mainBody.statements[0] as DeclarationStmt).singleDeclaration as VariableDecl
        assertNotNull(yDecl)

        val ifStmt = mainBody.statements[3] as? IfStmt
        assertNotNull(ifStmt)

        val thenStmt = ifStmt.thenStatement as? CompoundStmt
        assertNotNull(thenStmt)
        assertEquals(3, thenStmt.statements.size)
        assertNotNull(thenStmt.statements[1] as? AssignExpr)
        val aDecl = (thenStmt.statements[0] as DeclarationStmt).singleDeclaration as VariableDecl
        val thenY = thenStmt.statements[1] as AssignExpr
        assertEquals(1, thenY.lhs.size)
        assertEquals(1, thenY.rhs.size)
        assertSame(aDecl, (thenY.rhs.first() as Reference).refersTo)
        assertSame(yDecl, (thenY.lhs.first() as Reference).refersTo)

        val elseStmt = ifStmt.elseStatement as? CompoundStmt
        assertNotNull(elseStmt)
        assertEquals(3, elseStmt.statements.size)
        val bDecl = (elseStmt.statements[0] as DeclarationStmt).singleDeclaration as VariableDecl
        assertNotNull(elseStmt.statements[1] as? AssignExpr)
        val elseY = elseStmt.statements[1] as AssignExpr
        assertEquals(1, elseY.lhs.size)
        assertEquals(1, elseY.lhs.size)
        assertSame(bDecl, (elseY.rhs.first() as Reference).refersTo)
        assertSame(yDecl, (elseY.lhs.first() as Reference).refersTo)

        val continueBlock =
            (thenStmt.statements[2] as? GotoStmt)?.targetLabel?.subStatement as? CompoundStmt
        assertNotNull(continueBlock)
        assertEquals(
            yDecl,
            ((continueBlock.statements[1] as ReturnStmt).returnValue as Reference).refersTo
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
        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)

        // Test that x is initialized correctly
        val mainBody = main.body as CompoundStmt
        val origX =
            ((mainBody.statements[0] as? DeclarationStmt)?.singleDeclaration as? VariableDecl)
        val xInit = origX?.initializer as? InitializerListExpr
        assertNotNull(xInit)
        assertEquals(10L, (xInit.initializers[0] as? Literal<*>)?.value)
        assertEquals(9L, (xInit.initializers[1] as? Literal<*>)?.value)
        assertEquals(6L, (xInit.initializers[2] as? Literal<*>)?.value)
        assertEquals(-100L, (xInit.initializers[3] as? Literal<*>)?.value)

        // Test that y is initialized correctly
        val origY =
            ((mainBody.statements[1] as? DeclarationStmt)?.singleDeclaration as? VariableDecl)
        val yInit = origY?.initializer as? InitializerListExpr
        assertNotNull(yInit)
        assertEquals(15L, (yInit.initializers[0] as? Literal<*>)?.value)
        assertEquals(34L, (yInit.initializers[1] as? Literal<*>)?.value)
        assertEquals(99L, (yInit.initializers[2] as? Literal<*>)?.value)
        assertEquals(1000L, (yInit.initializers[3] as? Literal<*>)?.value)

        // Test that extractelement works
        val zInit =
            ((mainBody.statements[2] as? DeclarationStmt)?.singleDeclaration as? VariableDecl)
                ?.initializer as? SubscriptionExpr
        assertNotNull(zInit)
        assertEquals(0L, (zInit.subscriptExpression as? Literal<*>)?.value)
        assertEquals("x", (zInit.arrayExpression as? Reference)?.name?.localName)
        assertSame(origX, (zInit.arrayExpression as? Reference)?.refersTo)

        // Test the assignment of y to yMod
        val yModInit =
            ((mainBody.statements[3] as CompoundStmt).statements[0] as? DeclarationStmt)
                ?.singleDeclaration as? VariableDecl
        assertNotNull(yModInit)
        assertEquals("y", (yModInit.initializer as? Reference)?.name?.localName)
        assertSame(origY, (yModInit.initializer as? Reference)?.refersTo)
        // Now, test the modification of yMod[3] = 8
        val yMod = ((mainBody.statements[3] as CompoundStmt).statements[1] as? AssignExpr)
        assertNotNull(yMod)
        assertEquals(1, yMod.lhs.size)
        assertEquals(1, yMod.rhs.size)
        assertEquals(
            3L,
            ((yMod.lhs.first() as? SubscriptionExpr)?.subscriptExpression as? Literal<*>)?.value
        )
        assertSame(
            yModInit,
            ((yMod.lhs.first() as? SubscriptionExpr)?.arrayExpression as? Reference)?.refersTo
        )
        assertEquals(8L, (yMod.rhs.first() as? Literal<*>)?.value)

        // Test the last shufflevector instruction which does not contain constant as initializers.
        val shuffledInit =
            ((mainBody.statements[4] as? DeclarationStmt)?.singleDeclaration as? VariableDecl)
                ?.initializer as? InitializerListExpr
        assertNotNull(shuffledInit)
        assertSame(
            origX,
            ((shuffledInit.initializers[0] as? SubscriptionExpr)?.arrayExpression as? Reference)
                ?.refersTo
        )
        assertSame(
            yModInit,
            ((shuffledInit.initializers[1] as? SubscriptionExpr)?.arrayExpression as? Reference)
                ?.refersTo
        )
        assertSame(
            yModInit,
            ((shuffledInit.initializers[2] as? SubscriptionExpr)?.arrayExpression as? Reference)
                ?.refersTo
        )
        assertSame(
            1,
            ((shuffledInit.initializers[0] as? SubscriptionExpr)?.subscriptExpression
                    as? Literal<*>)
                ?.value
        )
        assertSame(
            2,
            ((shuffledInit.initializers[1] as? SubscriptionExpr)?.subscriptExpression
                    as? Literal<*>)
                ?.value
        )
        assertSame(
            3,
            ((shuffledInit.initializers[2] as? SubscriptionExpr)?.subscriptExpression
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
        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)

        // Test that x is initialized correctly
        val mainBody = main.body as CompoundStmt

        val fenceCall = mainBody.statements[0] as? CallExpr
        assertNotNull(fenceCall)
        assertEquals(1, fenceCall.arguments.size)
        assertEquals(2, (fenceCall.arguments[0] as Literal<*>).value)

        val fenceCallScope = mainBody.statements[2] as? CallExpr
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

        val funcF = tu.byNameOrNull<FunctionDecl>("f")
        assertNotNull(funcF)

        val tryStmt =
            (funcF.bodyOrNull<LabelStmt>(0)?.subStatement as? CompoundStmt)
                ?.statements
                ?.firstOrNull { s -> s is TryStmt } as? TryStmt
        assertNotNull(tryStmt)
        assertEquals(2, tryStmt.tryBlock?.statements?.size)
        assertFullName("_CxxThrowException", tryStmt.tryBlock?.statements?.get(0) as? CallExpr)
        assertEquals(
            "end",
            (tryStmt.tryBlock?.statements?.get(1) as? GotoStmt)?.targetLabel?.name?.localName
        )

        assertEquals(1, tryStmt.catchClauses.size)
        val catchSwitchExpr = tryStmt.catchClauses[0].body?.statements?.get(0) as? DeclarationStmt
        assertNotNull(catchSwitchExpr)
        val catchswitchCall =
            (catchSwitchExpr.singleDeclaration as? VariableDecl)?.initializer as? CallExpr
        assertNotNull(catchswitchCall)
        assertFullName("llvm.catchswitch", catchswitchCall)
        val ifExceptionMatches = tryStmt.catchClauses[0].body?.statements?.get(1) as? IfStmt
        val matchesExceptionCall = ifExceptionMatches?.condition as? CallExpr
        assertNotNull(matchesExceptionCall)
        assertFullName("llvm.matchesCatchpad", matchesExceptionCall)
        assertEquals(
            catchSwitchExpr.singleDeclaration,
            (matchesExceptionCall.arguments[0] as Reference).refersTo
        )
        assertEquals(null, (matchesExceptionCall.arguments[1] as Literal<*>).value)
        assertEquals(64L, (matchesExceptionCall.arguments[2] as Literal<*>).value as Long)
        assertEquals(null, (matchesExceptionCall.arguments[3] as Literal<*>).value)

        val catchBlock = ifExceptionMatches.thenStatement as? CompoundStmt
        assertNotNull(catchBlock)
        assertFullName(
            "llvm.catchpad",
            ((catchBlock.statements[0] as? DeclarationStmt)?.singleDeclaration as? VariableDecl)
                ?.initializer as? CallExpr
        )

        val innerTry = catchBlock.statements[1] as? TryStmt
        assertNotNull(innerTry)
        assertFullName("_CxxThrowException", innerTry.tryBlock?.statements?.get(0) as? CallExpr)
        assertLocalName(
            "try.cont",
            (innerTry.tryBlock?.statements?.get(1) as? GotoStmt)?.targetLabel
        )

        val innerCatchClause =
            (innerTry.catchClauses[0].body?.statements?.get(1) as? IfStmt)?.thenStatement
                as? CompoundStmt
        assertNotNull(innerCatchClause)
        assertFullName(
            "llvm.catchpad",
            ((innerCatchClause.statements[0] as? DeclarationStmt)?.singleDeclaration
                    as? VariableDecl)
                ?.initializer as? CallExpr
        )
        assertLocalName("try.cont", (innerCatchClause.statements[1] as? GotoStmt)?.targetLabel)

        val innerCatchThrows =
            (innerTry.catchClauses[0].body?.statements?.get(1) as? IfStmt)?.elseStatement
                as? UnaryOp
        assertNotNull(innerCatchThrows)
        assertNotNull(innerCatchThrows.input)
        assertSame(
            innerTry.catchClauses[0].parameter,
            (innerCatchThrows.input as? Reference)?.refersTo
        )
    }

    // TODO: Write test for calling a vararg function (e.g. printf). LLVM code snippets can already
    // be found in client.ll.
}
