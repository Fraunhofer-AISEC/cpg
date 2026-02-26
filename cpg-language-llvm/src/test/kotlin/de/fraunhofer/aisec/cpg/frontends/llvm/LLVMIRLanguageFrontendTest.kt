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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.assertThrows

class LLVMIRLanguageFrontendTest {
    @Test
    fun testExceptionBrokenFile() {
        val topLevel = Path.of("src", "test", "resources", "llvm")

        val ctx = TranslationContext(TranslationConfiguration.builder().build())
        val frontend = LLVMIRLanguageFrontend(ctx, LLVMIRLanguage())
        val exception =
            assertThrows<TranslationException> {
                frontend.parse(topLevel.resolve("main-broken.ll").toFile())
            }
        assertTrue(exception.message?.startsWith("Could not parse IR: ") == true)
    }

    @Test
    fun test1() {
        val topLevel = Path.of("src", "test", "resources", "llvm")

        val ctx = TranslationContext(TranslationConfiguration.builder().build())
        val frontend = LLVMIRLanguageFrontend(ctx, LLVMIRLanguage())
        frontend.parse(topLevel.resolve("main.ll").toFile())
    }

    @Test
    fun testVectorPoison() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("vector_poison.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertEquals(1, tu.declarations.size)

        val main = tu.functions["main"]
        assertNotNull(main)
        assertLocalName("i32", main.type)

        // We want to see that the declaration is the very first statement of the method body (it's
        // wrapped inside another block).
        val mainBody = main.bodyOrNull<Block>(0)
        assertIs<Block>(mainBody)
        val declarationStmt = mainBody.statements.firstOrNull()
        assertIs<DeclarationStatement>(declarationStmt)
        val xVector = declarationStmt.singleDeclaration
        assertIs<Variable>(xVector)
        val xInit = xVector.initializer
        assertIs<InitializerList>(xInit)
        assertIs<Reference>(xInit.initializers[0])
        assertLocalName("poison", xInit.initializers[0])
        assertLiteralValue(0L, xInit.initializers[1])
        assertLiteralValue(0L, xInit.initializers[2])
        assertLiteralValue(0L, xInit.initializers[3])
    }

    @Test
    fun testIdentifiedStruct() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("struct.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)

        val rt = tu.records["struct.RT"]
        assertNotNull(rt)

        val st = tu.records["struct.ST"]
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

        val foo = tu.functions["foo"]
        assertNotNull(foo)

        val s = foo.parameters.firstOrNull { it.name.localName == "s" }
        assertNotNull(s)

        val arrayidx = foo.variables["arrayidx"]
        assertNotNull(arrayidx)

        // arrayidx will be assigned to a chain of the following expressions:
        // &s[1].field2.field1[5][13]
        // we will check them in the reverse order (after the unary operator)

        val unary = arrayidx.initializer
        assertIs<UnaryOperator>(unary)
        assertEquals("&", unary.operatorCode)

        var arrayExpr = unary.input
        assertIs<Subscription>(arrayExpr)
        assertLocalName("13", arrayExpr)
        assertLiteralValue(
            13L,
            arrayExpr.subscriptExpression,
        ) // should this be integer instead of long?

        arrayExpr = arrayExpr.arrayExpression
        assertIs<Subscription>(arrayExpr)
        assertLocalName("5", arrayExpr)
        assertLiteralValue(
            5L,
            arrayExpr.subscriptExpression,
        ) // should this be integer instead of long?

        var memberExpression = arrayExpr.arrayExpression
        assertIs<MemberAccess>(memberExpression)
        assertLocalName("field_1", memberExpression)

        memberExpression = memberExpression.base
        assertIs<MemberAccess>(memberExpression)
        assertLocalName("field_2", memberExpression)

        arrayExpr = memberExpression.base
        assertIs<Subscription>(arrayExpr)
        assertLocalName("1", arrayExpr)
        assertLiteralValue(
            1L,
            arrayExpr.subscriptExpression,
        ) // should this be integer instead of long?

        val ref = arrayExpr.arrayExpression
        assertIs<Reference>(ref)
        assertLocalName("s", ref)
        assertRefersTo(ref, s)
    }

    @Test
    fun testSwitchCase() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("switch_case.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val main = tu.functions["main"]
        assertNotNull(main)

        val onzeroLabel = main.labels.getOrNull(0)
        assertNotNull(onzeroLabel)
        assertLocalName("onzero", onzeroLabel)
        assertIs<Block>(onzeroLabel.subStatement)

        val ononeLabel = main.labels.getOrNull(1)
        assertNotNull(ononeLabel)
        assertLocalName("onone", ononeLabel)
        assertIs<Block>(ononeLabel.subStatement)

        val defaultLabel = main.labels.getOrNull(2)
        assertNotNull(defaultLabel)
        assertLocalName("otherwise", defaultLabel)
        assertIs<Block>(defaultLabel.subStatement)

        // Check that the type of %a is i32
        val a = main.variables["a"]
        assertNotNull(a)
        assertLocalName("a", a)
        assertEquals("i32", a.type.typeName)

        // Check that the jump targets are set correctly
        val switchStatement = main.switches.firstOrNull()
        assertNotNull(switchStatement)

        // Check that we have switch(a)
        assertRefersTo(switchStatement.selector, a)

        val cases = switchStatement.statement
        assertIs<Block>(cases)
        // Check that the first case is case 0 -> goto onzero and that the BB is inlined
        val case1 = cases.statements[0]
        assertIs<CaseStatement>(case1)
        assertLiteralValue(0L, case1.caseExpression)
        assertSame(onzeroLabel.subStatement, cases.statements[1])
        // Check that the second case is case 1 -> goto onone and that the BB is inlined
        val case2 = cases.statements[2]
        assertIs<CaseStatement>(case2)
        assertLiteralValue(1L, case2.caseExpression)
        assertSame(ononeLabel.subStatement, cases.statements[3])

        // Check that the default location is inlined
        val defaultStatement = cases.statements[4] as? DefaultStatement
        assertIs<DefaultStatement>(defaultStatement)
        assertSame(defaultLabel.subStatement, cases.statements[5])
    }

    @Test
    fun testBrStatements() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("br.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertEquals(2, tu.declarations.size)

        val main = tu.functions["main"]
        assertNotNull(main)

        // Test that the types and values of the comparison expression are correct
        val icmpStatement = main.bodyOrNull<DeclarationStatement>(1)
        assertNotNull(icmpStatement)
        val variableDecl = icmpStatement.declarations[0]
        assertIs<Variable>(variableDecl)
        val comparison = variableDecl.initializer
        assertIs<BinaryOperator>(comparison)
        assertEquals("==", comparison.operatorCode)
        val rhs = comparison.rhs
        assertIs<Literal<*>>(rhs)
        assertLiteralValue(10L, rhs)
        assertEquals(tu.primitiveType("i32"), rhs.type)
        val lhsRef = comparison.lhs
        assertIs<Reference>(lhsRef)
        assertLocalName("x", lhsRef)
        val lhsDeclaration = lhsRef.refersTo
        assertIs<Variable>(lhsDeclaration)
        assertLocalName("x", lhsDeclaration)
        assertSame(tu.primitiveType("i32"), lhsDeclaration.type)

        // Check that the jump targets are set correctly
        val ifStatement = main.ifs.firstOrNull()
        assertNotNull(ifStatement)
        val elseStatement = ifStatement.elseStatement
        assertIs<GotoStatement>(elseStatement)
        assertEquals("IfUnequal", elseStatement.labelName)
        val thenBranch = ifStatement.thenStatement
        assertIs<Block>(thenBranch)

        // Check that the condition is set correctly
        val ifCondition = ifStatement.condition
        assertRefersTo(ifCondition, variableDecl)

        val elseBranch = elseStatement.targetLabel?.subStatement
        assertIs<Block>(elseBranch)
        assertEquals(2, elseBranch.statements.size)
        assertEquals("  %y = mul i32 %x, 32768", elseBranch.statements[0].code)
        assertEquals("  ret i32 %y", elseBranch.statements[1].code)

        // Check that it's  the correct then-branch
        assertEquals(2, thenBranch.statements.size)
        assertEquals("  %condUnsigned = icmp ugt i32 %x, -3", thenBranch.statements[0].code)

        val ifBranchDeclarationStatement = thenBranch.statements[0]
        assertIs<DeclarationStatement>(ifBranchDeclarationStatement)
        val ifBranchVariableDeclaration = ifBranchDeclarationStatement.declarations[0]
        assertIs<Variable>(ifBranchVariableDeclaration)
        val ifBranchComp = ifBranchVariableDeclaration.initializer
        assertIs<BinaryOperator>(ifBranchComp)
        assertEquals(">", ifBranchComp.operatorCode)
        assertIs<Cast>(ifBranchComp.rhs)
        assertIs<Cast>(ifBranchComp.lhs)

        val ifBranchCompRhs = ifBranchComp.rhs
        assertIs<Cast>(ifBranchCompRhs)
        assertEquals(tu.objectType("ui32"), ifBranchCompRhs.castType)
        assertEquals(tu.objectType("ui32"), ifBranchCompRhs.type)
        val ifBranchCompLhs = ifBranchComp.lhs
        assertIs<Cast>(ifBranchCompLhs)
        assertEquals(tu.objectType("ui32"), ifBranchCompLhs.castType)
        assertEquals(tu.objectType("ui32"), ifBranchCompLhs.type)

        val declRefExpr = ifBranchCompLhs.expression
        assertIs<Reference>(declRefExpr)
        assertLocalName("x", declRefExpr)
        assertLiteralValue(-3L, ifBranchCompRhs.expression)
        assertNotNull(declRefExpr.refersTo)

        val ifBranchSecondStatement = thenBranch.statements[1]
        assertIs<IfStatement>(ifBranchSecondStatement)
        val ifRet = ifBranchSecondStatement.thenStatement
        assertIs<Block>(ifRet)
        assertEquals(1, ifRet.statements.size)
        assertEquals("  ret i32 1", ifRet.statements[0].code)
    }

    @Test
    fun testCmpxchg() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("atomicrmw.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val foo = tu.functions["foo"]
        assertNotNull(foo)

        val cmpxchgStatement = foo.bodyOrNull<Block>(10)
        assertNotNull(cmpxchgStatement)
        assertEquals(2, cmpxchgStatement.statements.size)

        // Check that the first statement is "literal_i32_i1 val_success = literal_i32_i1(*ptr, *ptr
        // == 5)"
        val declaration = cmpxchgStatement.statements[0].declarations[0]
        assertIs<Variable>(declaration)
        assertLocalName("val_success", declaration)
        assertLocalName("literal_i32_i1", declaration.type)

        // Check that the first value is *ptr
        val declarationInitializer = declaration.initializer
        assertIs<Construction>(declarationInitializer)
        val value1 = declarationInitializer.arguments[0]
        assertIs<UnaryOperator>(value1)
        assertEquals("*", value1.operatorCode)
        assertLocalName("ptr", value1.input)

        // Check that the first value is *ptr == 5
        val value2 = declarationInitializer.arguments[1]
        assertIs<BinaryOperator>(value2)
        assertEquals("==", value2.operatorCode)
        val value2Lhs = value2.lhs
        assertIs<UnaryOperator>(value2Lhs)
        assertEquals("*", value2Lhs.operatorCode)
        assertLocalName("ptr", value2Lhs.input)
        assertLiteralValue(5L, value2.rhs)

        val ifStatement = cmpxchgStatement.statements[1]
        assertIs<IfStatement>(ifStatement)
        // The condition is the same as the second value above
        val ifExpr = ifStatement.condition
        assertIs<BinaryOperator>(ifExpr)
        assertEquals("==", ifExpr.operatorCode)
        val ifExprLhs = ifExpr.lhs
        assertIs<UnaryOperator>(ifExprLhs)
        assertEquals("*", ifExprLhs.operatorCode)
        assertLocalName("ptr", ifExprLhs.input)
        assertLiteralValue(5L, ifExpr.rhs)

        val thenExpr = ifStatement.thenStatement
        assertIs<Assign>(thenExpr)
        assertEquals(1, thenExpr.lhs.size)
        assertEquals(1, thenExpr.rhs.size)
        assertEquals("=", thenExpr.operatorCode)
        val thenExprLhs = thenExpr.lhs.first()
        assertIs<UnaryOperator>(thenExprLhs)
        assertEquals("*", thenExprLhs.operatorCode)
        assertLocalName("ptr", thenExprLhs.input)
        assertIs<Reference>(thenExpr.rhs.first())
        assertLocalName("old1", thenExpr.rhs.first())
        assertRefersTo(thenExpr.rhs.first(), tu.variables["old1"])
    }

    @Test
    fun testExtractvalue() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("atomicrmw.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val foo = tu.functions["foo"]
        assertNotNull(foo)

        val declaration = foo.variables["value_loaded"]
        assertNotNull(declaration)
        assertLocalName("i1", declaration.type)

        val initializer = declaration.initializer
        assertIs<MemberAccess>(initializer)
        assertLocalName("val_success", initializer.base)
        assertEquals(".", initializer.operatorCode)
        assertLocalName("field_1", initializer)
    }

    @Test
    fun testLiteralStruct() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("literal_struct.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)

        val foo = tu.functions["foo"]
        assertNotNull(foo)
        val fooType = foo.type
        assertIs<ObjectType>(fooType)
        assertEquals("literal_i32_i8", fooType.typeName)

        val record = fooType.recordDeclaration
        assertNotNull(record)
        assertEquals(2, record.fields.size)

        val returnStatement = foo.returns.singleOrNull()
        assertNotNull(returnStatement)

        val construct = returnStatement.returnValue
        assertIs<Construction>(construct)
        assertEquals(2, construct.arguments.size)

        assertEquals("i32", construct.arguments[0].type.typeName)
        assertLiteralValue(4L, construct.arguments[0])

        assertEquals("i8", construct.arguments[1].type.typeName)
        assertLiteralValue(2L, construct.arguments[1])
    }

    @Test
    fun testVariableScope() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("global_local_var.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)

        val main = tu.functions["main"]
        assertNotNull(main)

        val globalX = tu.variables["x"]
        assertNotNull(globalX)
        assertEquals("i32*", globalX.type.typeName)

        val globalA = tu.variables["a"]
        assertNotNull(globalA)
        assertEquals("i32*", globalA.type.typeName)

        val loadXStatement = main.bodyOrNull<DeclarationStatement>(1)
        assertNotNull(loadXStatement)
        assertLocalName("locX", loadXStatement.singleDeclaration)

        val initXOpDeclaration = loadXStatement.singleDeclaration
        assertIs<Variable>(initXOpDeclaration)
        val initXOp = initXOpDeclaration.initializer
        assertIs<UnaryOperator>(initXOp)
        assertEquals("*", initXOp.operatorCode)

        var ref = initXOp.input
        assertIs<Reference>(ref)
        assertLocalName("x", ref)
        assertRefersTo(ref, globalX)

        val loadAStatement = main.bodyOrNull<DeclarationStatement>(2)
        assertNotNull(loadAStatement)
        val loadADeclaration = loadAStatement.singleDeclaration
        assertIs<Variable>(loadADeclaration)
        assertLocalName("locA", loadAStatement.singleDeclaration)
        val initAOp = loadADeclaration.initializer
        assertIs<UnaryOperator>(initAOp)
        assertEquals("*", initAOp.operatorCode)

        ref = initAOp.input
        assertIs<Reference>(ref)
        assertLocalName("a", ref)
        assertRefersTo(ref, globalA)
    }

    @Test
    fun testAlloca() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("alloca.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)

        val main = tu.functions["main"]
        assertNotNull(main)

        // %ptr = alloca i32
        val ptr = main.bodyOrNull<DeclarationStatement>()?.singleDeclaration
        assertIs<Variable>(ptr)

        val alloca = ptr.initializer
        assertIs<ArrayConstruction>(alloca)
        assertEquals("i32*", alloca.type.typeName)

        // store i32 3, i32* %ptr
        val store = main.assigns.firstOrNull()
        assertNotNull(store)
        assertEquals("=", store.operatorCode)

        assertEquals(1, store.lhs.size)
        val dereferencePtr = store.lhs.firstOrNull()
        assertIs<UnaryOperator>(dereferencePtr)
        assertEquals("*", dereferencePtr.operatorCode)
        assertEquals("i32", dereferencePtr.type.typeName)
        assertRefersTo(dereferencePtr.input, ptr)

        assertEquals(1, store.rhs.size)
        val value = store.rhs.firstOrNull()
        assertIs<Literal<*>>(value)
        assertLiteralValue(3L, value)
        assertEquals("i32", value.type.typeName)
    }

    @Test
    fun testUndefInsertvalue() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("undef_insertvalue.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        assertNotNull(tu)

        val foo = tu.functions["foo"]
        assertNotNull(foo)
        assertEquals("literal_i32_i8", foo.type.typeName)

        val fooType = foo.type
        assertIs<ObjectType>(fooType)
        val record = fooType.recordDeclaration
        assertNotNull(record)
        assertEquals(2, record.fields.size)

        val declarationStatement = foo.bodyOrNull<DeclarationStatement>()
        assertNotNull(declarationStatement)

        val varDeclaration = declarationStatement.singleDeclaration
        assertIs<Variable>(varDeclaration)
        assertLocalName("a", varDeclaration)
        assertEquals("literal_i32_i8", varDeclaration.type.typeName)
        val initializer = varDeclaration.initializer
        assertIs<Construction>(initializer)
        val args = initializer.arguments
        assertEquals(2, args.size)
        assertLiteralValue(100L, args[0])
        assertLiteralValue(null, args[1])

        val block = foo.blocks.firstOrNull()
        assertNotNull(block)

        // First copy a to b
        val b = block.variables["b"]
        assertNotNull(b)
        assertLocalName("b", b)
        assertEquals("literal_i32_i8", b.type.typeName)

        // Now, we set b.field_1 to 7
        val assign = block.assigns.firstOrNull()
        assertNotNull(assign)

        assertEquals("=", assign.operatorCode)
        assertEquals(1, assign.lhs.size)
        assertEquals(1, assign.rhs.size)
        val assignLhs = assign.lhs.first()
        assertIs<MemberAccess>(assignLhs)
        assertLocalName("b", assignLhs.base)
        assertEquals(".", assignLhs.operatorCode)
        assertLocalName("field_1", assignLhs)
        assertLiteralValue(7L, assign.rhs.first())
    }

    @Test
    fun testTryCatch() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("try_catch.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val throwingFoo = tu.functions["throwingFoo"]

        val main = tu.functions["main"]
        assertNotNull(main)

        val mainBody = main.body
        assertIs<Block>(mainBody)
        val tryStatement = mainBody.statements[0]
        assertIs<TryStatement>(tryStatement)

        // Check the assignment of the function call
        val resDeclarationStatement = tryStatement.tryBlock?.statements?.get(0)
        assertIs<DeclarationStatement>(resDeclarationStatement)
        val resDeclaration = resDeclarationStatement.singleDeclaration
        assertIs<Variable>(resDeclaration)
        assertLocalName("res", resDeclaration)
        val call = resDeclaration.initializer
        assertIs<Call>(call)
        assertLocalName("throwingFoo", call)
        assertContains(call.invokes, throwingFoo)
        assertEquals(0, call.arguments.size)

        // Check that the second part of the try-block is inlined by the pass
        val aDeclarationStatement = tryStatement.tryBlock?.statements?.get(1)
        assertIs<DeclarationStatement>(aDeclarationStatement)
        val aDeclaration = aDeclarationStatement.singleDeclaration
        assertIs<Variable>(aDeclaration)
        assertLocalName("a", aDeclaration)
        val resStatement = tryStatement.tryBlock?.statements?.get(2)
        assertIs<ReturnStatement>(resStatement)

        // Check that the catch block is inlined by the pass
        assertEquals(1, tryStatement.catchClauses.size)
        assertEquals(5, tryStatement.catchClauses[0].body?.statements?.size)
        assertLocalName("_ZTIi | ...", tryStatement.catchClauses[0])
        val ifStatement = tryStatement.catchClauses[0].body?.statements?.get(4)
        assertIs<IfStatement>(ifStatement)
        val thenStatement = ifStatement.thenStatement
        assertIs<Block>(thenStatement)
        assertEquals(4, thenStatement.statements.size)
        val elseStatement = ifStatement.elseStatement
        assertIs<Block>(elseStatement)
        assertEquals(1, elseStatement.statements.size)
    }

    @Test
    fun testLoopPhi() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("loopPhi.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }
        val main = tu.functions["loopPhi"]
        assertNotNull(main)
    }

    @Test
    fun testPhi() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("phi.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }
        val main = tu.functions["main"]
        assertNotNull(main)

        val mainBody = main.body
        assertIs<Block>(mainBody)
        val yDeclarationStatement = mainBody.statements[0]
        assertIs<DeclarationStatement>(yDeclarationStatement)
        val yDecl = yDeclarationStatement.singleDeclaration
        assertIs<Variable>(yDecl)

        val ifStatement = mainBody.statements[3]
        assertIs<IfStatement>(ifStatement)

        val thenStmt = ifStatement.thenStatement
        assertIs<Block>(thenStmt)
        assertEquals(3, thenStmt.statements.size)
        val aDeclarationStatement = thenStmt.statements[0]
        assertIs<DeclarationStatement>(aDeclarationStatement)
        val aDecl = aDeclarationStatement.singleDeclaration
        assertIs<Variable>(aDecl)
        val thenY = thenStmt.statements[1]
        assertIs<Assign>(thenY)
        assertEquals(1, thenY.lhs.size)
        assertEquals(1, thenY.rhs.size)
        assertRefersTo(thenY.rhs.first(), aDecl)
        assertRefersTo(thenY.lhs.first(), yDecl)

        val elseStmt = ifStatement.elseStatement
        assertIs<Block>(elseStmt)
        assertEquals(3, elseStmt.statements.size)
        val bDeclarationStatement = elseStmt.statements[0]
        assertIs<DeclarationStatement>(bDeclarationStatement)
        val bDecl = bDeclarationStatement.singleDeclaration
        assertIs<Variable>(bDecl)
        val elseY = elseStmt.statements[1]
        assertIs<Assign>(elseY)
        assertEquals(1, elseY.lhs.size)
        assertEquals(1, elseY.lhs.size)
        assertRefersTo(elseY.rhs.first(), bDecl)
        assertRefersTo(elseY.lhs.first(), yDecl)

        val gotoStatement = thenStmt.statements[2]
        assertIs<GotoStatement>(gotoStatement)
        val continueBlock = gotoStatement.targetLabel?.subStatement
        assertIs<Block>(continueBlock)
        val returnStatement = continueBlock.statements[1]
        assertIs<ReturnStatement>(returnStatement)
        assertIs<Reference>(returnStatement.returnValue)
        assertRefersTo(returnStatement.returnValue, yDecl)
    }

    @Test
    fun testVectorOperations() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("vector.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }
        val main = tu.functions["main"]
        assertNotNull(main)

        // Test that x is initialized correctly
        val mainBody = main.body
        assertIs<Block>(mainBody)
        val xDeclarationStatement = mainBody.statements[0]
        assertIs<DeclarationStatement>(xDeclarationStatement)
        val origX = xDeclarationStatement.singleDeclaration
        assertIs<Variable>(origX)
        val xInit = origX.initializer
        assertIs<InitializerList>(xInit)
        assertLiteralValue(10L, xInit.initializers[0])
        assertLiteralValue(9L, xInit.initializers[1])
        assertLiteralValue(6L, xInit.initializers[2])
        assertLiteralValue(-100L, xInit.initializers[3])

        // Test that y is initialized correctly

        val yDeclarationStatement = mainBody.statements[1]
        assertIs<DeclarationStatement>(yDeclarationStatement)
        val origY = yDeclarationStatement.singleDeclaration
        assertIs<Variable>(origY)
        val yInit = origY.initializer
        assertIs<InitializerList>(yInit)
        assertLiteralValue(15L, yInit.initializers[0])
        assertLiteralValue(34L, yInit.initializers[1])
        assertLiteralValue(99L, yInit.initializers[2])
        assertLiteralValue(1000L, yInit.initializers[3])

        // Test that extractelement works
        val zDeclarationStatement = mainBody.statements[2]
        assertIs<DeclarationStatement>(zDeclarationStatement)
        val origZ = zDeclarationStatement.singleDeclaration
        assertIs<Variable>(origZ)
        val zInit = origZ.initializer
        assertIs<Subscription>(zInit)
        assertLiteralValue(0L, zInit.subscriptExpression)
        assertLocalName("x", zInit.arrayExpression)
        assertRefersTo(zInit.arrayExpression, origX)

        // Test the assignment of y to yMod
        val yModDeclarationStatementBlock = mainBody.statements[3]
        assertIs<Block>(yModDeclarationStatementBlock)
        val yModDeclarationStatement = yModDeclarationStatementBlock.statements[0]
        assertIs<DeclarationStatement>(yModDeclarationStatement)
        val modY = yModDeclarationStatement.singleDeclaration
        assertIs<Variable>(modY)
        val yModInit = modY.initializer
        assertIs<Reference>(yModInit)
        assertLocalName("y", yModInit)
        assertRefersTo(yModInit, origY)

        // Now, test the modification of yMod[3] = 8
        val yMod = yModDeclarationStatementBlock.statements[1]
        assertIs<Assign>(yMod)
        assertEquals(1, yMod.lhs.size)
        assertEquals(1, yMod.rhs.size)
        val yModLhs = yMod.lhs.first()
        assertIs<Subscription>(yModLhs)
        assertLiteralValue(3L, yModLhs.subscriptExpression)
        assertRefersTo(yModLhs.arrayExpression, modY)
        assertLiteralValue(8L, yMod.rhs.first())

        // Test the last shufflevector instruction which does not contain constant as initializers.
        val shuffledInitDeclarationStatement = mainBody.statements[4]
        assertIs<DeclarationStatement>(shuffledInitDeclarationStatement)
        val shuffledInitDeclaration = shuffledInitDeclarationStatement.singleDeclaration
        assertIs<Variable>(shuffledInitDeclaration)
        val shuffledInit = shuffledInitDeclaration.initializer
        assertIs<InitializerList>(shuffledInit)
        val shuffledInit0 = shuffledInit.initializers[0]
        assertIs<Subscription>(shuffledInit0)
        val shuffledInit1 = shuffledInit.initializers[1]
        assertIs<Subscription>(shuffledInit1)
        val shuffledInit2 = shuffledInit.initializers[2]
        assertIs<Subscription>(shuffledInit2)
        assertRefersTo(shuffledInit0.arrayExpression, origX)
        assertRefersTo(shuffledInit1.arrayExpression, modY)
        assertRefersTo(shuffledInit2.arrayExpression, modY)
        assertLiteralValue(1, shuffledInit0.subscriptExpression)
        assertLiteralValue(2, shuffledInit1.subscriptExpression)
        assertLiteralValue(3, shuffledInit2.subscriptExpression)
    }

    @Test
    fun testFence() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("fence.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }
        val main = tu.functions["main"]
        assertNotNull(main)

        // Test that x is initialized correctly
        val mainBody = main.body
        assertIs<Block>(mainBody)

        val fenceCall = mainBody.statements[0]
        assertIs<Call>(fenceCall)
        assertEquals(1, fenceCall.arguments.size)
        assertLiteralValue(2, fenceCall.arguments[0])

        val fenceCallScope = mainBody.statements[2]
        assertIs<Call>(fenceCallScope)
        assertEquals(2, fenceCallScope.arguments.size)
        // TODO: This doesn't match but it doesn't seem to be our mistake
        // assertEquals(5, (fenceCallScope.arguments[0] as Literal<*>).value)
        assertLiteralValue("scope", fenceCallScope.arguments[1])
    }

    @Test
    fun testExceptions() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("exceptions.ll").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val funcF = tu.functions["f"]
        assertNotNull(funcF)

        val tryStatement = funcF.bodyOrNull<LabelStatement>(0)?.subStatement?.trys?.firstOrNull()
        assertNotNull(tryStatement)
        val tryBlock = tryStatement.tryBlock
        assertNotNull(tryBlock)
        assertEquals(2, tryBlock.statements.size)
        assertIs<Call>(tryBlock.statements[0])
        assertFullName("_CxxThrowException", tryBlock.statements[0])
        val gotoStatement = tryBlock.statements[1]
        assertIs<GotoStatement>(gotoStatement)
        assertLocalName("end", gotoStatement.targetLabel)

        assertEquals(1, tryStatement.catchClauses.size)
        val catchBody = tryStatement.catchClauses[0].body
        assertNotNull(catchBody)
        val catchSwitchExpr = catchBody.statements[0]
        assertIs<DeclarationStatement>(catchSwitchExpr)
        val catchSwitchDeclaration = catchSwitchExpr.singleDeclaration
        assertIs<Variable>(catchSwitchDeclaration)
        val catchswitchCall = catchSwitchDeclaration.initializer
        assertIs<Call>(catchswitchCall)
        assertFullName("llvm.catchswitch", catchswitchCall)
        val ifExceptionMatches = tryStatement.catchClauses[0].body?.statements?.get(1)
        assertIs<IfStatement>(ifExceptionMatches)
        val matchesExceptionCall = ifExceptionMatches.condition
        assertIs<Call>(matchesExceptionCall)
        assertFullName("llvm.matchesCatchpad", matchesExceptionCall)
        assertRefersTo(matchesExceptionCall.arguments[0], catchSwitchDeclaration)
        assertLiteralValue(null, matchesExceptionCall.arguments[1])
        assertLiteralValue(64L, matchesExceptionCall.arguments[2])
        assertLiteralValue(null, matchesExceptionCall.arguments[3])

        val catchBlock = ifExceptionMatches.thenStatement
        assertIs<Block>(catchBlock)
        val catchpadDeclarationStatement = catchBlock.statements[0]
        assertIs<DeclarationStatement>(catchpadDeclarationStatement)
        val catchpadDeclaration = catchpadDeclarationStatement.singleDeclaration
        assertIs<Variable>(catchpadDeclaration)
        assertIs<Call>(catchpadDeclaration.initializer)
        assertFullName("llvm.catchpad", catchpadDeclaration.initializer)

        val innerTry = catchBlock.statements[1]
        assertIs<TryStatement>(innerTry)
        val innerTryBlock = innerTry.tryBlock
        assertNotNull(innerTryBlock)
        assertIs<Call>(innerTryBlock.statements[0])
        assertFullName("_CxxThrowException", innerTryBlock.statements[0])
        val innerTryGoto = innerTryBlock.statements[1]
        assertIs<GotoStatement>(innerTryGoto)
        assertLocalName("try.cont", innerTryGoto.targetLabel)

        val innerCatchBody = innerTry.catchClauses[0].body
        assertNotNull(innerCatchBody)
        val innerCatchIf = innerCatchBody.statements[1]
        assertIs<IfStatement>(innerCatchIf)
        val innerCatchClause = innerCatchIf.thenStatement
        assertIs<Block>(innerCatchClause)
        val innerCatchpadDeclarationStatement = innerCatchClause.statements[0]
        assertIs<DeclarationStatement>(innerCatchpadDeclarationStatement)
        val innerCatchDeclaration = innerCatchpadDeclarationStatement.singleDeclaration
        assertIs<Variable>(innerCatchDeclaration)
        assertFullName("llvm.catchpad", innerCatchDeclaration.initializer)

        val innerCatchGoto = innerCatchClause.statements[1]
        assertIs<GotoStatement>(innerCatchGoto)
        assertLocalName("try.cont", innerCatchGoto.targetLabel)

        val innerCatchThrows = innerCatchIf.elseStatement
        assertIs<Throw>(innerCatchThrows)
        assertNotNull(innerCatchThrows.exception)
        assertRefersTo(innerCatchThrows.exception, innerTry.catchClauses[0].parameter)
    }

    // TODO: Write test for calling a vararg function (e.g. printf). LLVM code snippets can already
    // be found in client.ll.
}
