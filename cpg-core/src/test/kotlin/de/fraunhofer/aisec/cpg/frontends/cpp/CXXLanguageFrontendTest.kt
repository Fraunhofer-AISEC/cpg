/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.InferenceConfiguration.Companion.builder
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.TestUtils.analyzeWithBuilder
import de.fraunhofer.aisec.cpg.TestUtils.assertInvokes
import de.fraunhofer.aisec.cpg.TestUtils.assertRefersTo
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.assertFullName
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import kotlin.collections.set
import kotlin.test.*

internal class CXXLanguageFrontendTest : BaseTest() {
    @Test
    @Throws(Exception::class)
    fun testForEach() {
        val file = File("src/test/resources/components/foreachstmt.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val main = tu.getDeclarationsByName("main", FunctionDeclaration::class.java)
        assertFalse(main.isEmpty())

        val decl = main.iterator().next()
        val ls = decl.variables["ls"]
        assertNotNull(ls)
        assertEquals(createTypeFrom("std::vector<int>", true), ls.type)
        assertLocalName("ls", ls)

        val forEachStatement = decl.getBodyStatementAs(1, ForEachStatement::class.java)
        assertNotNull(forEachStatement)

        // should loop over ls
        assertEquals(ls, (forEachStatement.iterable as DeclaredReferenceExpression).refersTo)

        // should declare auto i (so far no concrete type inferrable)
        val stmt = forEachStatement.variable
        assertNotNull(stmt)
        assertTrue(stmt is DeclarationStatement)
        assertTrue(stmt.isSingleDeclaration())

        val i = stmt.singleDeclaration as VariableDeclaration
        assertNotNull(i)
        assertLocalName("i", i)
        assertEquals(UnknownType.getUnknownType(CPPLanguage()), i.type)
    }

    @Test
    @Throws(Exception::class)
    fun testTryCatch() {
        val file = File("src/test/resources/components/trystmt.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val main = tu.getDeclarationsByName("main", FunctionDeclaration::class.java)
        assertFalse(main.isEmpty())

        val tryStatement = main.iterator().next().getBodyStatementAs(0, TryStatement::class.java)
        assertNotNull(tryStatement)

        val catchClauses = tryStatement.catchClauses
        // should have 3 catch clauses
        assertEquals(3, catchClauses.size)

        // declared exception variable
        var parameter = catchClauses[0].parameter
        assertNotNull(parameter)
        assertLocalName("e", parameter)
        assertEquals("std::exception&", parameter.type.typeName)

        // anonymous variable (this is not 100% handled correctly but will do for now)
        parameter = catchClauses[1].parameter
        assertNotNull(parameter)
        // this is currently our 'unnamed' parameter
        assertLocalName("", parameter)
        assertEquals("std::exception&", parameter.type.typeName)

        // catch all
        parameter = catchClauses[2].parameter
        assertNull(parameter)
    }

    @Test
    @Throws(Exception::class)
    fun testTypeId() {
        val file = File("src/test/resources/typeidexpr.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val main = tu.getDeclarationsByName("main", FunctionDeclaration::class.java)
        assertNotNull(main)

        val funcDecl = main.iterator().next()
        val i = funcDecl.variables["i"]
        assertNotNull(i)

        val sizeof = i.initializer as? TypeIdExpression
        assertNotNull(sizeof)
        assertLocalName("sizeof", sizeof)
        assertEquals(createTypeFrom("std::size_t", true), sizeof.type)

        val typeInfo = funcDecl.variables["typeInfo"]
        assertNotNull(typeInfo)

        val typeid = typeInfo.initializer as? TypeIdExpression
        assertNotNull(typeid)
        assertLocalName("typeid", typeid)
        assertEquals(createTypeFrom("const std::type_info&", true), typeid.type)

        val j = funcDecl.variables["j"]
        assertNotNull(j)

        val alignOf = j.initializer as? TypeIdExpression
        assertNotNull(sizeof)
        assertNotNull(alignOf)
        assertLocalName("alignof", alignOf)
        assertEquals(createTypeFrom("std::size_t", true), alignOf.type)
    }

    @Test
    @Throws(Exception::class)
    fun testCast() {
        val file = File("src/test/resources/components/castexpr.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val main = tu.getDeclarationAs(0, FunctionDeclaration::class.java)
        val e =
            Objects.requireNonNull(main!!.getBodyStatementAs(0, DeclarationStatement::class.java))
                ?.singleDeclaration as VariableDeclaration
        assertNotNull(e)
        assertEquals(createTypeFrom("ExtendedClass*", true), e.type)

        val b =
            Objects.requireNonNull(main.getBodyStatementAs(1, DeclarationStatement::class.java))
                ?.singleDeclaration as VariableDeclaration
        assertNotNull(b)
        assertEquals(createTypeFrom("BaseClass*", true), b.type)

        // initializer
        var cast = b.initializer as? CastExpression
        assertNotNull(cast)
        assertEquals(createTypeFrom("BaseClass*", true), cast.castType)

        val staticCast = main.getBodyStatementAs(2, BinaryOperator::class.java)
        assertNotNull(staticCast)
        cast = staticCast.rhs as CastExpression
        assertNotNull(cast)
        assertLocalName("static_cast", cast)

        val reinterpretCast = main.getBodyStatementAs(3, BinaryOperator::class.java)
        assertNotNull(reinterpretCast)
        cast = reinterpretCast.rhs as CastExpression
        assertNotNull(cast)
        assertLocalName("reinterpret_cast", cast)

        val d =
            Objects.requireNonNull(main.getBodyStatementAs(4, DeclarationStatement::class.java))
                ?.singleDeclaration as VariableDeclaration
        assertNotNull(d)

        cast = d.initializer as? CastExpression
        assertNotNull(cast)
        assertEquals(createTypeFrom("int", true), cast.castType)
    }

    @Test
    @Throws(Exception::class)
    fun testArrays() {
        val file = File("src/test/resources/cxx/arrays.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)
        assertNotNull(main)

        val statement = main.body as CompoundStatement

        // first statement is the variable declaration
        val x =
            (statement.statements[0] as DeclarationStatement).singleDeclaration
                as VariableDeclaration
        assertNotNull(x)
        assertEquals(createTypeFrom("int[]", true), x.type)

        // initializer is an initializer list expression
        val ile = x.initializer as? InitializerListExpression
        assertNotNull(ile)

        val initializers = ile.initializers
        assertNotNull(initializers)
        assertEquals(3, initializers.size)

        // second statement is an expression directly
        val ase = statement.statements[1] as ArraySubscriptionExpression
        assertNotNull(ase)
        assertEquals(x, (ase.arrayExpression as DeclaredReferenceExpression).refersTo)
        assertEquals(0, (ase.subscriptExpression as Literal<*>).value)

        // third statement declares a pointer to an array
        val a =
            (statement.statements[2] as? DeclarationStatement)?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(a)

        val type = a.type
        assertTrue(type is PointerType && type.pointerOrigin == PointerType.PointerOrigin.POINTER)

        val elementType = (a.type as? PointerType)?.elementType
        assertNotNull(elementType)
        assertTrue(
            elementType is PointerType &&
                elementType.pointerOrigin == PointerType.PointerOrigin.ARRAY
        )
    }

    @Test
    @Throws(Exception::class)
    fun testFunctionDeclaration() {
        val file = File("src/test/resources/cxx/functiondecl.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        // should be seven function nodes
        assertEquals(8, declaration.declarations.size)

        var method = declaration.getDeclarationAs(0, FunctionDeclaration::class.java)
        assertEquals("function0(int)void", method!!.signature)

        method = declaration.getDeclarationAs(1, FunctionDeclaration::class.java)
        assertEquals("function1(int, std::string, SomeType*, AnotherType&)int", method!!.signature)

        val args = method.parameters.map { it.name.localName }
        assertEquals(listOf("arg0", "arg1", "arg2", "arg3"), args)

        method = declaration.getDeclarationAs(2, FunctionDeclaration::class.java)
        assertEquals("function0(int)void", method!!.signature)

        var statements = (method.body as CompoundStatement).statements
        assertFalse(statements.isEmpty())
        assertEquals(2, statements.size)

        // last statement should be an implicit return
        var statement = method.getBodyStatementAs(statements.size - 1, ReturnStatement::class.java)
        assertNotNull(statement)
        assertTrue(statement.isImplicit)

        method = declaration.getDeclarationAs(3, FunctionDeclaration::class.java)
        assertEquals("function2()void*", method!!.signature)

        statements = (method.body as CompoundStatement).statements
        assertFalse(statements.isEmpty())
        assertEquals(1, statements.size)

        // should only contain 1 explicit return statement
        statement = method.getBodyStatementAs(0, ReturnStatement::class.java)
        assertNotNull(statement)
        assertFalse(statement.isImplicit)

        method = declaration.getDeclarationAs(4, FunctionDeclaration::class.java)
        assertNotNull(method)
        assertEquals("function3()UnknownType*", method.signature)

        method = declaration.getDeclarationAs(5, FunctionDeclaration::class.java)
        assertNotNull(method)
        assertEquals("function4(int)void", method.signature)

        method = declaration.getDeclarationAs(6, FunctionDeclaration::class.java)
        assertNotNull(method)
        assertEquals(0, method.parameters.size)
        assertEquals("function5()void", method.signature)

        method = declaration.getDeclarationAs(7, FunctionDeclaration::class.java)
        assertNotNull(method)
        assertEquals(1, method.parameters.size)

        val param = method.parameters.firstOrNull()
        assertNotNull(param)

        val fpType = param.type as? FunctionPointerType
        assertNotNull(fpType)
        assertEquals(1, fpType.parameters.size)
        assertLocalName("void", fpType.returnType)
    }

    @Test
    @Throws(Exception::class)
    fun testCompoundStatement() {
        val file = File("src/test/resources/compoundstmt.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val function = declaration.getDeclarationAs(0, FunctionDeclaration::class.java)
        assertNotNull(function)

        val functionBody = function.body
        assertNotNull(functionBody)

        val statements = (functionBody as CompoundStatement).statements
        assertEquals(1, statements.size)

        val returnStatement = statements[0] as ReturnStatement
        assertNotNull(returnStatement)

        val returnValue = returnStatement.returnValue
        assertTrue(returnValue is Literal<*>)
        assertEquals(1, returnValue.value)
    }

    @Test
    @Throws(Exception::class)
    fun testPostfixExpression() {
        val file = File("src/test/resources/postfixexpression.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val statements =
            declaration.getDeclarationAs(0, FunctionDeclaration::class.java)?.statements
        assertNotNull(statements)
        assertEquals(6, statements.size)

        val callExpression = statements[0] as CallExpression
        assertLocalName("printf", callExpression)

        val arg = callExpression.arguments[0]
        assertTrue(arg is Literal<*>)
        assertEquals("text", arg.value)

        val unaryOperatorPlus = statements[1] as UnaryOperator
        assertEquals(UnaryOperator.OPERATOR_POSTFIX_INCREMENT, unaryOperatorPlus.operatorCode)
        assertTrue(unaryOperatorPlus.isPostfix)

        val unaryOperatorMinus = statements[2] as UnaryOperator
        assertEquals(UnaryOperator.OPERATOR_POSTFIX_DECREMENT, unaryOperatorMinus.operatorCode)
        assertTrue(unaryOperatorMinus.isPostfix)

        // 4th statement is not yet parsed correctly
        val memberCallExpression = statements[4] as MemberCallExpression
        assertLocalName("test", memberCallExpression.base)
        assertLocalName("c_str", memberCallExpression)
    }

    @Test
    @Throws(Exception::class)
    fun testIf() {
        val file = File("src/test/resources/if.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val statements =
            declaration.getDeclarationAs(0, FunctionDeclaration::class.java)?.statements
        assertNotNull(statements)

        val ifStatement = statements[0] as IfStatement
        assertNotNull(ifStatement)
        assertNotNull(ifStatement.condition)
        assertEquals("bool", ifStatement.condition!!.type.typeName)
        assertEquals(true, (ifStatement.condition as Literal<*>).value)
        assertTrue(
            (ifStatement.thenStatement as CompoundStatement).statements[0] is ReturnStatement
        )
        assertTrue(
            (ifStatement.elseStatement as CompoundStatement).statements[0] is ReturnStatement
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSwitch() {
        val file = File("src/test/resources/cfg/switch.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        assertTrue(tu.allChildren<Node>().isNotEmpty())

        val switchStatements = tu.allChildren<SwitchStatement>()
        assertTrue(switchStatements.size == 3)

        val switchStatement = switchStatements[0]
        assertTrue((switchStatement.statement as CompoundStatement).statements.size == 11)

        val caseStatements = switchStatement.allChildren<CaseStatement>()
        assertTrue(caseStatements.size == 4)

        val defaultStatements = switchStatement.allChildren<DefaultStatement>()
        assertTrue(defaultStatements.size == 1)
    }

    @Test
    @Throws(Exception::class)
    fun testDeclarationStatement() {
        val file = File("src/test/resources/cxx/declstmt.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val function = declaration.getDeclarationAs(0, FunctionDeclaration::class.java)
        val statements = function?.statements
        assertNotNull(statements)
        statements.forEach(
            Consumer { node: Statement ->
                log.debug("{}", node)
                assertTrue(
                    node is DeclarationStatement ||
                        statements.indexOf(node) == statements.size - 1 && node is ReturnStatement
                )
            }
        )

        val declFromMultiplicateExpression =
            (statements[0] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        assertEquals(createTypeFrom("SSL_CTX*", true), declFromMultiplicateExpression.type)
        assertLocalName("ptr", declFromMultiplicateExpression)

        val withInitializer =
            (statements[1] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        var initializer = withInitializer.initializer
        assertNotNull(initializer)
        assertTrue(initializer is Literal<*>)
        assertEquals(1, initializer.value)

        val twoDeclarations = statements[2].declarations
        assertEquals(2, twoDeclarations.size)

        val b = twoDeclarations[0] as VariableDeclaration
        assertNotNull(b)
        assertLocalName("b", b)
        assertEquals(createTypeFrom("int*", false), b.type)

        val c = twoDeclarations[1] as VariableDeclaration
        assertNotNull(c)
        assertLocalName("c", c)
        assertEquals(createTypeFrom("int", false), c.type)

        val withoutInitializer =
            (statements[3] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        initializer = withoutInitializer.initializer
        assertEquals(createTypeFrom("int*", true), withoutInitializer.type)
        assertLocalName("d", withoutInitializer)
        assertNull(initializer)

        val qualifiedType =
            (statements[4] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        assertEquals(createTypeFrom("std::string", true), qualifiedType.type)
        assertLocalName("text", qualifiedType)
        assertTrue(qualifiedType.initializer is Literal<*>)
        assertEquals("some text", (qualifiedType.initializer as? Literal<*>)?.value)

        val pointerWithAssign =
            (statements[5] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        assertEquals(createTypeFrom("void*", true), pointerWithAssign.type)
        assertLocalName("ptr2", pointerWithAssign)
        assertLocalName("NULL", pointerWithAssign.initializer)

        val classWithVariable = statements[6].declarations
        assertEquals(2, classWithVariable.size)

        val classA = classWithVariable[0] as RecordDeclaration
        assertNotNull(classA)
        assertLocalName("A", classA)

        val myA = classWithVariable[1] as VariableDeclaration
        assertNotNull(myA)
        assertLocalName("myA", myA)
        assertEquals(classA, (myA.type as ObjectType).recordDeclaration)
    }

    @Test
    @Throws(Exception::class)
    fun testAssignmentExpression() {
        val file = File("src/test/resources/assignmentexpression.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        // just take a look at the second function
        val functionDeclaration = declaration.getDeclarationAs(1, FunctionDeclaration::class.java)
        val statements = functionDeclaration?.statements
        assertNotNull(statements)

        val declareA = statements[0]
        val a = (declareA as DeclarationStatement).singleDeclaration
        val assignA = statements[1]
        assertTrue(assignA is BinaryOperator)

        var lhs = assignA.lhs
        var rhs = assignA.rhs
        assertLocalName("a", lhs)
        assertEquals(2, (rhs as? Literal<*>)?.value)
        assertRefersTo(assignA.lhs, a)

        val declareB = statements[2]
        assertTrue(declareB is DeclarationStatement)

        val b = declareB.singleDeclaration

        // a = b
        val assignB = statements[3]
        assertTrue(assignB is BinaryOperator)

        lhs = assignB.lhs
        rhs = assignB.rhs
        assertLocalName("a", lhs)
        assertTrue(rhs is DeclaredReferenceExpression)
        assertLocalName("b", rhs)
        assertRefersTo(rhs, b)

        val assignBWithFunction = statements[4]
        assertTrue(assignBWithFunction is BinaryOperator)
        assertLocalName("a", assignBWithFunction.lhs)
        assertTrue(assignBWithFunction.rhs is CallExpression)

        val call = assignBWithFunction.rhs as CallExpression
        assertLocalName("someFunction", call)
        assertRefersTo(call.arguments[0], b)
    }

    @Test
    @Throws(Exception::class)
    fun testShiftExpression() {
        val file = File("src/test/resources/shiftexpression.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val functionDeclaration = declaration.getDeclarationAs(0, FunctionDeclaration::class.java)
        val statements = functionDeclaration?.statements
        assertNotNull(statements)
        assertTrue(statements[1] is BinaryOperator)
    }

    @Test
    @Throws(Exception::class)
    fun testUnaryOperator() {
        val file = File("src/test/resources/unaryoperator.cpp")
        val unit = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val statements = unit.getDeclarationAs(0, FunctionDeclaration::class.java)?.statements
        assertNotNull(statements)

        var line = -1

        // int a
        var statement = statements[++line] as DeclarationStatement
        assertNotNull(statement)

        // a++
        val postfix = statements[++line] as UnaryOperator
        var input = postfix.input
        assertLocalName("a", input)
        assertEquals("++", postfix.operatorCode)
        assertTrue(postfix.isPostfix)

        // --a
        val prefix = statements[++line] as UnaryOperator
        input = prefix.input
        assertLocalName("a", input)
        assertEquals("--", prefix.operatorCode)
        assertTrue(prefix.isPrefix)

        // int len = sizeof(a);
        statement = statements[++line] as DeclarationStatement
        var declaration = statement.singleDeclaration as VariableDeclaration
        val sizeof = declaration.initializer as? UnaryOperator
        assertNotNull(sizeof)

        input = sizeof.input
        assertLocalName("a", input)
        assertEquals("sizeof", sizeof.operatorCode)
        assertTrue(sizeof.isPrefix)

        // bool b = !false;
        statement = statements[++line] as DeclarationStatement
        declaration = statement.singleDeclaration as VariableDeclaration
        val negation = declaration.initializer as? UnaryOperator
        assertNotNull(negation)

        input = negation.input
        assertTrue(input is Literal<*>)
        assertEquals(false, input.value)
        assertEquals("!", negation.operatorCode)
        assertTrue(negation.isPrefix)

        // int* ptr = 0;
        statement = statements[++line] as DeclarationStatement
        assertNotNull(statement)

        // b = *ptr;
        val assign = statements[++line] as BinaryOperator
        val dereference = assign.rhs as UnaryOperator
        input = dereference.input
        assertLocalName("ptr", input)
        assertEquals("*", dereference.operatorCode)
        assertTrue(dereference.isPrefix)
    }

    @Test
    @Throws(Exception::class)
    fun testBinaryOperator() {
        val file = File("src/test/resources/binaryoperator.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val statements =
            declaration.getDeclarationAs(0, FunctionDeclaration::class.java)?.statements
        assertNotNull(statements)
        // first two statements are just declarations

        // a = b * 2
        var operator = statements[2] as? BinaryOperator
        assertNotNull(operator)
        assertLocalName("a", operator.lhs)
        assertTrue(operator.rhs is BinaryOperator)

        var rhs = operator.rhs as BinaryOperator
        assertTrue(rhs.lhs is DeclaredReferenceExpression)
        assertLocalName("b", rhs.lhs)
        assertTrue(rhs.rhs is Literal<*>)
        assertEquals(2, (rhs.rhs as Literal<*>).value)

        // a = 1 * 1
        operator = statements[3] as? BinaryOperator
        assertNotNull(operator)
        assertLocalName("a", operator.lhs)
        assertTrue(operator.rhs is BinaryOperator)

        rhs = operator.rhs as BinaryOperator
        assertTrue(rhs.lhs is Literal<*>)
        assertEquals(1, (rhs.lhs as Literal<*>).value)
        assertTrue(rhs.rhs is Literal<*>)
        assertEquals(1, (rhs.rhs as Literal<*>).value)

        // std::string* notMultiplication
        // this is not a multiplication, but a variable declaration with a pointer type, but
        // syntactically no different than the previous ones
        val stmt = statements[4] as DeclarationStatement
        val decl = stmt.singleDeclaration as VariableDeclaration
        assertEquals(createTypeFrom("std::string*", true), decl.type)
        assertLocalName("notMultiplication", decl)
        assertTrue(decl.initializer is BinaryOperator)

        operator = decl.initializer as? BinaryOperator
        assertNotNull(operator)
        assertTrue(operator.lhs is Literal<*>)
        assertEquals(0, (operator.lhs as Literal<*>).value)
        assertTrue(operator.rhs is Literal<*>)
        assertEquals(0, (operator.rhs as Literal<*>).value)
    }

    @Test
    @Throws(Exception::class)
    fun testRecordDeclaration() {
        val file = File("src/test/resources/cxx/recordstmt.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        val recordDeclaration = tu.records.firstOrNull()
        assertNotNull(recordDeclaration)
        assertLocalName("SomeClass", recordDeclaration)
        assertEquals("class", recordDeclaration.kind)
        assertEquals(2, recordDeclaration.fields.size)

        val field = recordDeclaration.fields["field"]
        assertNotNull(field)

        val constant = recordDeclaration.fields["CONSTANT"]
        assertNotNull(constant)
        assertEquals(createTypeFrom("void*", true), field.type)
        assertEquals(3, recordDeclaration.methods.size)

        val method = recordDeclaration.methods[0]
        assertLocalName("method", method)
        assertEquals(0, method.parameters.size)
        assertEquals("()void*", method.type.typeName)
        assertFalse(method.hasBody())

        var definition = method.definition as? MethodDeclaration
        assertNotNull(definition)
        assertLocalName("method", definition)
        assertEquals(0, definition.parameters.size)
        assertTrue(definition.isDefinition)

        val methodWithParam = recordDeclaration.methods[1]
        assertLocalName("method", methodWithParam)
        assertEquals(1, methodWithParam.parameters.size)
        assertEquals(createTypeFrom("int", true), methodWithParam.parameters[0].type)
        assertEquals(
            FunctionType(
                "(int)void*",
                listOf(createTypeFrom("int", true)),
                listOf(createTypeFrom("void*", true)),
                CPPLanguage()
            ),
            methodWithParam.type
        )
        assertFalse(methodWithParam.hasBody())

        definition = methodWithParam.definition as MethodDeclaration
        assertNotNull(definition)
        assertLocalName("method", definition)
        assertEquals(1, definition.parameters.size)
        assertTrue(definition.isDefinition)

        val inlineMethod = recordDeclaration.methods[2]
        assertLocalName("inlineMethod", inlineMethod)
        assertEquals(
            FunctionType("()void*", listOf(), listOf(createTypeFrom("void*", true)), CPPLanguage()),
            inlineMethod.type
        )
        assertTrue(inlineMethod.hasBody())

        val inlineConstructor = recordDeclaration.constructors[0]
        assertEquals(recordDeclaration.name.localName, inlineConstructor.name.localName)
        assertEquals(
            FunctionType(
                "()SomeClass",
                listOf(),
                listOf(createTypeFrom("SomeClass", true)),
                CPPLanguage()
            ),
            inlineConstructor.type
        )
        assertTrue(inlineConstructor.hasBody())

        val constructorDefinition = tu.getDeclarationAs(3, ConstructorDeclaration::class.java)
        assertNotNull(constructorDefinition)
        assertEquals(1, constructorDefinition.parameters.size)
        assertEquals(createTypeFrom("int", true), constructorDefinition.parameters[0].type)
        assertEquals(
            FunctionType(
                "(int)SomeClass",
                listOf(createTypeFrom("int", false)),
                listOf(createTypeFrom("SomeClass", true)),
                CPPLanguage()
            ),
            constructorDefinition.type
        )
        assertTrue(constructorDefinition.hasBody())

        val constructorDeclaration = recordDeclaration.constructors[1]
        assertNotNull(constructorDeclaration)
        assertFalse(constructorDeclaration.isDefinition)
        assertEquals(constructorDefinition, constructorDeclaration.definition)

        val main =
            tu.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(main)

        val methodCallWithConstant = main.getBodyStatementAs(2, CallExpression::class.java)
        assertNotNull(methodCallWithConstant)

        val arg = methodCallWithConstant.arguments[0]
        assertSame(constant, (arg as DeclaredReferenceExpression).refersTo)

        val anotherMethod = tu.methods["anotherMethod"]
        assertNotNull(anotherMethod)
        assertFullName("OtherClass::anotherMethod", anotherMethod)
    }

    @Test
    @Throws(Exception::class)
    fun testLiterals() {
        val file = File("src/test/resources/cxx/literals.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        val s = tu.variables["s"]
        assertNotNull(s)
        assertIs<PointerType>(s.type)
        assertLocalName("char[]", s.type)

        var initializer = s.initializer as? Literal<*>
        assertNotNull(initializer)
        assertEquals("string", initializer.value)

        val i = tu.variables["i"]
        assertNotNull(i)
        assertIs<IntegerType>(i.type)
        assertLocalName("int", i.type)

        initializer = i.initializer as? Literal<*>
        assertNotNull(initializer)
        assertEquals(1, initializer.value)

        val f = tu.variables["f"]
        assertNotNull(f)
        assertIs<FloatingPointType>(f.type)
        assertLocalName("float", f.type)

        initializer = f.initializer as? Literal<*>
        assertNotNull(initializer)
        assertEquals(0.2f, initializer.value)

        val d = tu.variables["d"]
        assertNotNull(d)
        assertIs<FloatingPointType>(d.type)
        assertLocalName("double", d.type)

        initializer = d.initializer as? Literal<*>
        assertNotNull(initializer)
        assertEquals(0.2, initializer.value)

        val b = tu.variables["b"]
        assertNotNull(b)
        assertIs<BooleanType>(b.type)
        assertLocalName("bool", b.type)

        initializer = b.initializer as? Literal<*>
        assertNotNull(initializer)
        assertEquals(false, initializer.value)

        val c = tu.variables["c"]
        assertNotNull(c)
        assertIs<IntegerType>(c.type)
        assertLocalName("char", c.type)

        initializer = c.initializer as? Literal<*>
        assertNotNull(initializer)
        assertEquals('c', initializer.value)

        val hex = tu.variables["hex"]
        assertNotNull(hex)
        assertIs<IntegerType>(hex.type)
        assertLocalName("unsigned long long", hex.type)

        val duration = tu.variables["duration"]
        assertNotNull(duration)
        assertIs<ProblemExpression>(duration.initializer)
    }

    @Test
    @Throws(Exception::class)
    fun testInitListExpression() {
        val file = File("src/test/resources/initlistexpression.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        // x y = { 1, 2 };
        val y = declaration.getDeclarationAs(1, VariableDeclaration::class.java)
        assertNotNull(y)
        assertLocalName("y", y)

        var initializer = y.initializer
        assertNotNull(initializer)
        assertTrue(initializer is InitializerListExpression)

        var listExpression = initializer
        assertEquals(2, listExpression.initializers.size)

        val a = listExpression.initializers[0] as Literal<*>
        val b = listExpression.initializers[1] as Literal<*>
        assertEquals(1, a.value)
        assertEquals(2, b.value)

        // int z[] = { 2, 3, 4 };
        val z = declaration.getDeclarationAs(2, VariableDeclaration::class.java)
        assertEquals(createTypeFrom("int[]", true), z!!.type)

        initializer = z.initializer
        assertNotNull(initializer)
        assertTrue(initializer is InitializerListExpression)

        listExpression = initializer
        assertEquals(3, listExpression.initializers.size)
    }

    @Test
    @Throws(Exception::class)
    fun testObjectCreation() {
        val file = File("src/test/resources/cxx/objcreation.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        assertNotNull(declaration)

        // get the main method
        val main = declaration.getDeclarationAs(3, FunctionDeclaration::class.java)
        val statement = main!!.body as CompoundStatement

        // Integer i
        val i =
            (statement.statements[0] as DeclarationStatement).singleDeclaration
                as VariableDeclaration
        // type should be Integer
        assertEquals(createTypeFrom("Integer", true), i.type)

        // initializer should be a construct expression
        var constructExpression = i.initializer as? ConstructExpression
        assertNotNull(constructExpression)
        // type of the construct expression should also be Integer
        assertEquals(createTypeFrom("Integer", true), constructExpression.type)

        // auto (Integer) m
        val m =
            (statement.statements[6] as DeclarationStatement).singleDeclaration
                as VariableDeclaration
        // type should be Integer*
        assertEquals(createTypeFrom("Integer*", true), m.type)

        val constructor = constructExpression.constructor
        assertNotNull(constructor)
        assertLocalName("Integer", constructor)
        assertFalse(constructor.isImplicit)

        // initializer should be a new expression
        val newExpression = m.initializer as? NewExpression
        assertNotNull(newExpression)
        // type of the new expression should also be Integer*
        assertEquals(createTypeFrom("Integer*", true), newExpression.type)

        // initializer should be a construct expression
        constructExpression = newExpression.initializer as? ConstructExpression
        assertNotNull(constructExpression)
        // type of the construct expression should be Integer
        assertEquals(createTypeFrom("Integer", true), constructExpression.type)

        // argument should be named k and of type m
        val k = constructExpression.arguments[0] as DeclaredReferenceExpression
        assertLocalName("k", k)
        // type of the construct expression should also be Integer
        assertEquals(createTypeFrom("int", true), k.type)
    }

    private val FunctionDeclaration.statements: List<Statement>?
        get() {
            return (this.body as? CompoundStatement)?.statements
        }

    @Test
    @Throws(Exception::class)
    fun testRegionsCfg() {
        val file = File("src/test/resources/cfg.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val fdecl = declaration.getDeclarationAs(0, FunctionDeclaration::class.java)
        val body = fdecl!!.body as CompoundStatement
        val expected: MutableMap<String?, Region> = HashMap()
        expected["cout << \"bla\";"] = Region(4, 3, 4, 17)
        expected["cout << \"blubb\";"] = Region(5, 3, 5, 19)
        expected["return 0;"] = Region(15, 3, 15, 12)

        for (d in body.statements) {
            if (expected.containsKey(d.code)) {
                assertEquals(expected[d.code], d.location?.region, d.code)
                expected.remove(d.code)
            }
        }
        assertTrue(expected.isEmpty(), java.lang.String.join(", ", expected.keys))
    }

    @Test
    @Throws(Exception::class)
    fun testDesignatedInitializer() {
        val file = File("src/test/resources/components/designatedInitializer.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        // should be four method nodes
        assertEquals(2, declaration.declarations.size)

        val method = declaration.getDeclarationAs(1, FunctionDeclaration::class.java)
        assertEquals("main()int", method!!.signature)
        assertTrue(method.body is CompoundStatement)

        val statements = (method.body as CompoundStatement).statements
        assertEquals(4, statements.size)
        assertTrue(statements[0] is DeclarationStatement)
        assertTrue(statements[1] is DeclarationStatement)
        assertTrue(statements[2] is DeclarationStatement)
        assertTrue(statements[3] is ReturnStatement)

        var initializer =
            ((statements[0] as DeclarationStatement).singleDeclaration as VariableDeclaration)
                .initializer
        assertTrue(initializer is InitializerListExpression)
        assertEquals(3, initializer.initializers.size)
        assertTrue(initializer.initializers[0] is DesignatedInitializerExpression)
        assertTrue(initializer.initializers[1] is DesignatedInitializerExpression)
        assertTrue(initializer.initializers[2] is DesignatedInitializerExpression)

        var die = initializer.initializers[0] as DesignatedInitializerExpression
        assertTrue(die.lhs[0] is DeclaredReferenceExpression)
        assertTrue(die.rhs is Literal<*>)
        assertLocalName("y", die.lhs[0])
        assertEquals(0, (die.rhs as Literal<*>).value)

        die = initializer.initializers[1] as DesignatedInitializerExpression
        assertTrue(die.lhs[0] is DeclaredReferenceExpression)
        assertTrue(die.rhs is Literal<*>)
        assertLocalName("z", die.lhs[0])
        assertEquals(1, (die.rhs as Literal<*>).value)

        die = initializer.initializers[2] as DesignatedInitializerExpression
        assertTrue(die.lhs[0] is DeclaredReferenceExpression)
        assertTrue(die.rhs is Literal<*>)
        assertLocalName("x", die.lhs[0])
        assertEquals(2, (die.rhs as Literal<*>).value)

        initializer =
            ((statements[1] as DeclarationStatement).singleDeclaration as VariableDeclaration)
                .initializer
        assertTrue(initializer is InitializerListExpression)
        assertEquals(1, initializer.initializers.size)
        assertTrue(initializer.initializers[0] is DesignatedInitializerExpression)

        die = initializer.initializers[0] as DesignatedInitializerExpression
        assertTrue(die.lhs[0] is DeclaredReferenceExpression)
        assertTrue(die.rhs is Literal<*>)
        assertLocalName("x", die.lhs[0])
        assertEquals(20, (die.rhs as Literal<*>).value)

        initializer =
            ((statements[2] as DeclarationStatement).singleDeclaration as VariableDeclaration)
                .initializer
        assertTrue(initializer is InitializerListExpression)
        assertEquals(2, initializer.initializers.size)
        assertTrue(initializer.initializers[0] is DesignatedInitializerExpression)
        assertTrue(initializer.initializers[1] is DesignatedInitializerExpression)

        die = initializer.initializers[0] as DesignatedInitializerExpression
        assertTrue(die.lhs[0] is Literal<*>)
        assertTrue(die.rhs is Literal<*>)
        assertEquals(3, (die.lhs[0] as Literal<*>).value)
        assertEquals(1, (die.rhs as Literal<*>).value)

        die = initializer.initializers[1] as DesignatedInitializerExpression
        assertTrue(die.lhs[0] is Literal<*>)
        assertTrue(die.rhs is Literal<*>)
        assertEquals(5, (die.lhs[0] as Literal<*>).value)
        assertEquals(2, (die.rhs as Literal<*>).value)
    }

    @Test
    @Throws(Exception::class)
    fun testLocalVariables() {
        val file = File("src/test/resources/variables/local_variables.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        val function =
            declaration.byNameOrNull<FunctionDeclaration>("testExpressionInExpressionList")
        assertEquals("testExpressionInExpressionList()int", function!!.signature)

        val locals = function.body?.locals
        assertNotNull(locals)

        // Expecting x, foo, t
        val localNames = locals.map { it.name.localName }.toSet()
        assertTrue(localNames.contains("x"))
        assertTrue(localNames.contains("foo"))
        assertTrue(localNames.contains("t"))
        // ... and nothing else
        assertEquals(3, localNames.size)
    }

    @Test
    @Throws(Exception::class)
    fun testLocation() {
        val file = File("src/test/resources/components/foreachstmt.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val main = tu.getDeclarationsByName("main", FunctionDeclaration::class.java)
        assertFalse(main.isEmpty())

        val location = main.iterator().next().location
        assertNotNull(location)

        val path = Path.of(location.artifactLocation.uri)
        assertEquals("foreachstmt.cpp", path.fileName.toString())
        assertEquals(Region(4, 1, 8, 2), location.region)
    }

    @Test
    @Throws(Exception::class)
    fun testNamespaces() {
        val file = File("src/test/resources/namespaces.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        assertNotNull(tu)

        val firstNamespace =
            tu.getDeclarationsByName("FirstNamespace", NamespaceDeclaration::class.java)
                .iterator()
                .next()
        assertNotNull(firstNamespace)

        val someClass =
            firstNamespace
                .getDeclarationsByName("FirstNamespace::SomeClass", RecordDeclaration::class.java)
                .iterator()
                .next()
        assertNotNull(someClass)

        val anotherClass =
            tu.getDeclarationsByName("AnotherClass", RecordDeclaration::class.java)
                .iterator()
                .next()
        assertNotNull(anotherClass)
    }

    @Test
    @Throws(Exception::class)
    fun testAttributes() {
        val file = File("src/test/resources/attributes.cpp")
        val declarations =
            analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(listOf(file))
                    .topLevel(file.parentFile)
                    .defaultPasses()
                    .defaultLanguages()
                    .processAnnotations(true)
                    .symbols(
                        mapOf(
                            Pair("PROPERTY_ATTRIBUTE(...)", "[[property_attribute(#__VA_ARGS__)]]")
                        )
                    )
            )
        assertFalse(declarations.isEmpty())

        val tu = declarations[0]
        assertNotNull(tu)

        val main =
            tu.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(main)
        assertLocalName("function_attribute", main.annotations[0])

        val someClass =
            tu.getDeclarationsByName("SomeClass", RecordDeclaration::class.java).iterator().next()
        assertNotNull(someClass)
        assertLocalName("record_attribute", someClass.annotations[0])

        val a = someClass.fields["a"]
        assertNotNull(a)

        var annotation = a.annotations[0]
        assertNotNull(annotation)
        assertLocalName("property_attribute", annotation)
        assertEquals(3, annotation.members.size)
        assertEquals("a", (annotation.members[0].value as Literal<*>).value)

        val b = someClass.fields["b"]
        assertNotNull(b)

        annotation = b.annotations[0]
        assertNotNull(annotation)
        assertLocalName("property_attribute", annotation)
        assertEquals(1, annotation.members.size)
        assertEquals(
            "SomeCategory, SomeOtherThing",
            (annotation.members[0].value as Literal<*>).value
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUnityBuild() {
        val file = File("src/test/resources/unity")
        val declarations =
            analyzeWithBuilder(
                TranslationConfiguration.builder()
                    .sourceLocations(listOf(file))
                    .topLevel(file.parentFile)
                    .useUnityBuild(true)
                    .loadIncludes(true)
                    .defaultPasses()
                    .defaultLanguages()
            )
        assertEquals(1, declarations.size)
        // should contain 3 declarations (2 include and 1 function decl from the include)
        assertEquals(3, declarations[0].declarations.size)
    }

    @Test
    @Throws(Exception::class)
    fun testEOGCompleteness() {
        val file = File("src/test/resources/fix-455/main.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val main =
            tu.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(main)

        val body = main.body as CompoundStatement
        assertNotNull(body)

        val returnStatement = body.statements[body.statements.size - 1]
        assertNotNull(returnStatement)

        // we need to assert, that we have a consistent chain of EOG edges from the first statement
        // to
        // the return statement. otherwise, the EOG chain is somehow broken
        val eogEdges = ArrayList<Node>()
        main.accept(
            { x: Node? -> Strategy.EOG_FORWARD(x!!) },
            object : IVisitor<Node>() {
                override fun visit(n: Node) {
                    println(n)
                    eogEdges.add(n)
                }
            }
        )
        assertTrue(eogEdges.contains(returnStatement))
    }

    @Test
    @Throws(Exception::class)
    fun testParenthesis() {
        val file = File("src/test/resources/cxx/parenthesis.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                config: TranslationConfiguration.Builder ->
                config.inferenceConfiguration(builder().guessCastExpressions(true).build())
            }
        val main =
            tu.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(main)

        val declStatement = main.getBodyStatementAs(0, DeclarationStatement::class.java)
        assertNotNull(declStatement)

        val decl = declStatement.singleDeclaration as VariableDeclaration
        assertNotNull(decl)

        val initializer = decl.initializer
        assertNotNull(initializer)
        assertTrue(initializer is CastExpression)
        assertLocalName("size_t", initializer.castType)
    }

    @Test
    @Throws(Exception::class)
    fun testCppThis() {
        val file = File("src/test/resources/cpp-this-field.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val classT = tu.byNameOrNull<RecordDeclaration>("T")
        assertNotNull(classT)

        val classTFoo = classT.methods.firstOrNull()
        assertNotNull(classTFoo)

        val classTReturn = classTFoo.bodyOrNull<ReturnStatement>()
        assertNotNull(classTReturn)

        val classTReturnMember = classTReturn.returnValue as? MemberExpression
        assertNotNull(classTReturnMember)

        val classTThisExpression = classTReturnMember.base as? DeclaredReferenceExpression
        assertEquals(classTThisExpression?.refersTo, classTFoo.receiver)

        val classS = tu.byNameOrNull<RecordDeclaration>("S")
        assertNotNull(classS)

        val classSFoo = classS.methods.firstOrNull()
        assertNotNull(classSFoo)

        val classSReturn = classSFoo.bodyOrNull<ReturnStatement>()
        assertNotNull(classSReturn)

        val classSReturnMember = classSReturn.returnValue as? MemberExpression
        assertNotNull(classSReturnMember)

        val classSThisExpression = classSReturnMember.base as? DeclaredReferenceExpression
        assertEquals(classSThisExpression?.refersTo, classSFoo.receiver)
        assertNotEquals(classTFoo, classSFoo)
        assertNotEquals(classTFoo.receiver, classSFoo.receiver)
    }

    @Test
    @Throws(Exception::class)
    fun testEnum() {
        val file = File("src/test/resources/c/enum.c")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        // TU should only contains two AST declarations (EnumDeclaration and FunctionDeclaration),
        // but NOT any EnumConstantDeclarations
        assertEquals(2, tu.declarations.size)

        val main =
            tu.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(main)

        val returnStmt = main.bodyOrNull<ReturnStatement>()
        assertNotNull(returnStmt)
        assertNotNull((returnStmt.returnValue as? DeclaredReferenceExpression)?.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testStruct() {
        val file = File("src/test/resources/c/struct.c")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val myStruct = tu.byNameOrNull<RecordDeclaration>("MyStruct")
        assertNotNull(myStruct)

        val field = myStruct.byNameOrNull<FieldDeclaration>("field")
        assertNotNull(field)

        val s = main.bodyOrNull<DeclarationStatement>()?.singleDeclaration as? VariableDeclaration
        assertNotNull(s)

        assertEquals(myStruct, (s.type as? ObjectType)?.recordDeclaration)
    }

    @Test
    @Throws(Exception::class)
    fun testTypedef() {
        val file = File("src/test/resources/c/typedef_in_header/main.c")
        val result = analyze(listOf(file), file.parentFile.toPath(), true)

        val typedefs = result.scopeManager.currentTypedefs
        assertNotNull(typedefs)
        assertTrue(typedefs.isNotEmpty())

        val tu = result.translationUnits.firstOrNull()
        assertNotNull(tu)

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val call = main.bodyOrNull<CallExpression>()
        assertNotNull(call)
        assertTrue(call.invokes.isNotEmpty())

        val func = call.invokes.firstOrNull()
        assertNotNull(func)
        assertFalse(func.isInferred)
    }

    @Test
    @Throws(Exception::class)
    fun testFunctionPointerToClassMethodSimple() {
        val file = File("src/test/resources/cxx/funcptr_class_simple.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        val myClass = tu.records["MyClass"]
        assertNotNull(myClass)

        val targetNoParam =
            myClass.methods[{ it.name.localName == "target" && it.parameters.isEmpty() }]
        assertNotNull(targetNoParam)

        val targetSingleParam =
            myClass.methods[{ it.name.localName == "target" && it.parameters.size == 1 }]
        assertNotNull(targetSingleParam)

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        // three variables (the class object and two function pointers)
        assertEquals(3, main.variables.size)

        val my = main.variables["my"]
        assertNotNull(my)
        assertFullName("MyClass", my.type)

        // ensure that our function pointer variable is connected to the method declaration via DFG
        val noParam = main.variables["no_param"]
        assertNotNull(noParam)
        assertTrue(
            noParam.followPrevDFGEdgesUntilHit { it == targetNoParam }.fulfilled.isNotEmpty()
        )

        // ensure that our function pointer variable is connected to the method declaration via DFG
        val singleParam = main.variables["single_param"]
        assertNotNull(singleParam)
        assertTrue(
            singleParam
                .followPrevDFGEdgesUntilHit { it == targetSingleParam }
                .fulfilled
                .isNotEmpty()
        )

        val noParamCall = main.mcalls[0]
        assertNotNull(noParamCall)
        assertInvokes(noParamCall, targetNoParam)
        assertFullName("MyClass::*no_param", noParamCall)

        var callee = noParamCall.callee as? BinaryOperator
        assertNotNull(callee)
        assertRefersTo(callee.lhs, my)
        assertRefersTo(callee.rhs, noParam)

        val singleParamCall = main.mcalls[1]
        assertNotNull(singleParamCall)
        assertInvokes(singleParamCall, targetSingleParam)
        assertFullName("MyClass::*single_param", singleParamCall)

        callee = singleParamCall.callee as? BinaryOperator
        assertNotNull(callee)
        assertRefersTo(callee.lhs, my)
        assertRefersTo(callee.rhs, singleParam)
    }

    @Test
    @Throws(Exception::class)
    fun testNamespacedFunction() {
        val file = File("src/test/resources/cxx/namespaced_function.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        assertNotNull(tu)

        // everything in the TU should be a function (within a namespace), not a method (except the
        // implicit constructor of ABC::A)
        assertTrue(tu.functions.isNotEmpty())
        assertTrue(tu.methods.none { it !is ConstructorDeclaration })

        var foo = tu.functions["foo"]
        assertNotNull(foo)

        // jump to definition (in case we got the declaration), but they should be connected anyway
        foo = foo.definition

        val a = foo.variables["a"]
        assertNotNull(a)
        assertFullName("ABC::A", a.type)

        val main = tu.functions["main"]
        assertNotNull(main)

        val callFoo = main.calls["ABC::foo"]
        assertNotNull(callFoo)
        assertInvokes(callFoo, foo)
        assertTrue(callFoo.invokes.none { it.isInferred })
    }

    private fun createTypeFrom(typename: String, resolveAlias: Boolean) =
        TypeParser.createFrom(typename, CPPLanguage(), resolveAlias, null)
}
