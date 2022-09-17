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
import de.fraunhofer.aisec.cpg.TestUtils.assertRefersTo
import de.fraunhofer.aisec.cpg.TranslationConfiguration
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
import java.util.stream.Collectors
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
        assertEquals(TypeParser.createFrom("std::vector<int>", true), ls.type)
        assertEquals("ls", ls.name)

        val forEachStatement = decl.getBodyStatementAs(1, ForEachStatement::class.java)
        assertNotNull(forEachStatement)

        // should loop over ls
        assertEquals(ls, (forEachStatement.iterable as DeclaredReferenceExpression).refersTo)

        // should declare auto i (so far no concrete type inferrable)
        val stmt = forEachStatement.variable
        assertNotNull(stmt)
        assertTrue(stmt is DeclarationStatement)
        assertTrue(stmt.isSingleDeclaration)

        val i = stmt.singleDeclaration as VariableDeclaration
        assertNotNull(i)
        assertEquals("i", i.name)
        assertEquals(UnknownType.getUnknownType(), i.type)
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
        assertEquals("e", parameter.name)
        assertEquals("std.exception&", parameter.type.typeName)
        assertTrue(parameter.type.qualifier.isConst)

        // anonymous variable (this is not 100% handled correctly but will do for now)
        parameter = catchClauses[1].parameter
        assertNotNull(parameter)
        // this is currently our 'unnamed' parameter
        assertEquals("", parameter.name)
        assertEquals("std.exception&", parameter.type.typeName)
        assertTrue(parameter.type.qualifier.isConst)

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
        assertEquals("sizeof", sizeof.name)
        assertEquals(TypeParser.createFrom("std::size_t", true), sizeof.type)

        val typeInfo = funcDecl.variables["typeInfo"]
        assertNotNull(typeInfo)

        val typeid = typeInfo.initializer as? TypeIdExpression
        assertNotNull(typeid)
        assertEquals("typeid", typeid.name)
        assertEquals(TypeParser.createFrom("const std::type_info&", true), typeid.type)

        val j = funcDecl.variables["j"]
        assertNotNull(j)

        val alignOf = j.initializer as? TypeIdExpression
        assertNotNull(sizeof)
        assertNotNull(alignOf)
        assertEquals("alignof", alignOf.name)
        assertEquals(TypeParser.createFrom("std::size_t", true), alignOf.type)
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
        assertEquals(TypeParser.createFrom("ExtendedClass*", true), e.type)

        val b =
            Objects.requireNonNull(main.getBodyStatementAs(1, DeclarationStatement::class.java))
                ?.singleDeclaration as VariableDeclaration
        assertNotNull(b)
        assertEquals(TypeParser.createFrom("BaseClass*", true), b.type)

        // initializer
        var cast = b.initializer as? CastExpression
        assertNotNull(cast)
        assertEquals(TypeParser.createFrom("BaseClass*", true), cast.castType)

        val staticCast = main.getBodyStatementAs(2, BinaryOperator::class.java)
        assertNotNull(staticCast)
        cast = staticCast.rhs as CastExpression
        assertNotNull(cast)
        assertEquals("static_cast", cast.name)

        val reinterpretCast = main.getBodyStatementAs(3, BinaryOperator::class.java)
        assertNotNull(reinterpretCast)
        cast = reinterpretCast.rhs as CastExpression
        assertNotNull(cast)
        assertEquals("reinterpret_cast", cast.name)

        val d =
            Objects.requireNonNull(main.getBodyStatementAs(4, DeclarationStatement::class.java))
                ?.singleDeclaration as VariableDeclaration
        assertNotNull(d)

        cast = d.initializer as? CastExpression
        assertNotNull(cast)
        assertEquals(TypeParser.createFrom("int", true), cast.castType)
    }

    @Test
    @Throws(Exception::class)
    fun testArrays() {
        val file = File("src/test/resources/cxx/arrays.cpp")
        val tu = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val statement = main.body as CompoundStatement

        // first statement is the variable declaration
        val x =
            (statement.statements[0] as DeclarationStatement).singleDeclaration
                as VariableDeclaration
        assertNotNull(x)
        assertEquals(TypeParser.createFrom("int[]", true), x.type)

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
        assertEquals("function1(int, std.string, SomeType*, AnotherType&)int", method!!.signature)

        val args = method.parameters.stream().map(Node::name).collect(Collectors.toList())
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
        assertEquals("void", fpType.returnType.name)
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
        assertEquals("printf", callExpression.name)

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
        assertEquals("test", memberCallExpression.base?.name)
        assertEquals("c_str", memberCallExpression.name)
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
        assertEquals("bool", ifStatement.condition.type.typeName)
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
        assertEquals(TypeParser.createFrom("SSL_CTX*", true), declFromMultiplicateExpression.type)
        assertEquals("ptr", declFromMultiplicateExpression.name)

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
        assertEquals("b", b.name)
        assertEquals(TypeParser.createFrom("int*", false), b.type)

        val c = twoDeclarations[1] as VariableDeclaration
        assertNotNull(c)
        assertEquals("c", c.name)
        assertEquals(TypeParser.createFrom("int", false), c.type)

        val withoutInitializer =
            (statements[3] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        initializer = withoutInitializer.initializer
        assertEquals(TypeParser.createFrom("int*", true), withoutInitializer.type)
        assertEquals("d", withoutInitializer.name)
        assertNull(initializer)

        val qualifiedType =
            (statements[4] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        assertEquals(TypeParser.createFrom("std.string", true), qualifiedType.type)
        assertEquals("text", qualifiedType.name)
        assertTrue(qualifiedType.initializer is Literal<*>)
        assertEquals("some text", (qualifiedType.initializer as? Literal<*>)?.value)

        val pointerWithAssign =
            (statements[5] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        assertEquals(TypeParser.createFrom("void*", true), pointerWithAssign.type)
        assertEquals("ptr2", pointerWithAssign.name)
        assertEquals("NULL", pointerWithAssign.initializer?.name)

        val classWithVariable = statements[6].declarations
        assertEquals(2, classWithVariable.size)

        val classA = classWithVariable[0] as RecordDeclaration
        assertNotNull(classA)
        assertEquals("A", classA.name)

        val myA = classWithVariable[1] as VariableDeclaration
        assertNotNull(myA)
        assertEquals("myA", myA.name)
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
        assertEquals("a", lhs.name)
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
        assertEquals("a", lhs.name)
        assertTrue(rhs is DeclaredReferenceExpression)
        assertEquals("b", rhs.name)
        assertRefersTo(rhs, b)

        val assignBWithFunction = statements[4]
        assertTrue(assignBWithFunction is BinaryOperator)
        assertEquals("a", assignBWithFunction.lhs.name)
        assertTrue(assignBWithFunction.rhs is CallExpression)

        val call = assignBWithFunction.rhs as CallExpression
        assertEquals("someFunction", call.name)
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
        assertEquals("a", input.name)
        assertEquals("++", postfix.operatorCode)
        assertTrue(postfix.isPostfix)

        // --a
        val prefix = statements[++line] as UnaryOperator
        input = prefix.input
        assertEquals("a", input.name)
        assertEquals("--", prefix.operatorCode)
        assertTrue(prefix.isPrefix)

        // int len = sizeof(a);
        statement = statements[++line] as DeclarationStatement
        var declaration = statement.singleDeclaration as VariableDeclaration
        val sizeof = declaration.initializer as? UnaryOperator
        assertNotNull(sizeof)

        input = sizeof.input
        assertEquals("a", input.name)
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
        assertEquals("ptr", input.name)
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
        assertEquals("a", operator.lhs.name)
        assertTrue(operator.rhs is BinaryOperator)

        var rhs = operator.rhs as BinaryOperator
        assertTrue(rhs.lhs is DeclaredReferenceExpression)
        assertEquals("b", rhs.lhs.name)
        assertTrue(rhs.rhs is Literal<*>)
        assertEquals(2, (rhs.rhs as Literal<*>).value)

        // a = 1 * 1
        operator = statements[3] as? BinaryOperator
        assertNotNull(operator)
        assertEquals("a", operator.lhs.name)
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
        assertEquals(TypeParser.createFrom("std.string*", true), decl.type)
        assertEquals("notMultiplication", decl.name)
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
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val recordDeclaration = declaration.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(recordDeclaration)
        assertEquals("SomeClass", recordDeclaration.name)
        assertEquals("class", recordDeclaration.kind)
        assertEquals(2, recordDeclaration.fields.size)

        val field = recordDeclaration.fields["field"]
        assertNotNull(field)

        val constant = recordDeclaration.fields["CONSTANT"]
        assertNotNull(constant)
        assertEquals(TypeParser.createFrom("void*", true), field.type)
        assertEquals(3, recordDeclaration.methods.size)

        val method = recordDeclaration.methods[0]
        assertEquals("method", method.name)
        assertEquals(0, method.parameters.size)
        assertEquals("()void*", method.type.typeName)
        assertFalse(method.hasBody())

        var definition = method.definition as? MethodDeclaration
        assertNotNull(definition)
        assertEquals("method", definition.name)
        assertEquals(0, definition.parameters.size)
        assertTrue(definition.isDefinition)

        val methodWithParam = recordDeclaration.methods[1]
        assertEquals("method", methodWithParam.name)
        assertEquals(1, methodWithParam.parameters.size)
        assertEquals(TypeParser.createFrom("int", true), methodWithParam.parameters[0].type)
        assertEquals(
            FunctionType(
                "(int)void*",
                listOf(TypeParser.createFrom("int", true)),
                listOf(TypeParser.createFrom("void*", true))
            ),
            methodWithParam.type
        )
        assertFalse(methodWithParam.hasBody())

        definition = methodWithParam.definition as MethodDeclaration
        assertNotNull(definition)
        assertEquals("method", definition.name)
        assertEquals(1, definition.parameters.size)
        assertTrue(definition.isDefinition)

        val inlineMethod = recordDeclaration.methods[2]
        assertEquals("inlineMethod", inlineMethod.name)
        assertEquals(
            FunctionType("()void*", listOf(), listOf(TypeParser.createFrom("void*", true))),
            inlineMethod.type
        )
        assertTrue(inlineMethod.hasBody())

        val inlineConstructor = recordDeclaration.constructors[0]
        assertEquals(recordDeclaration.name, inlineConstructor.name)
        assertEquals(
            FunctionType("()SomeClass", listOf(), listOf(TypeParser.createFrom("SomeClass", true))),
            inlineConstructor.type
        )
        assertTrue(inlineConstructor.hasBody())

        val constructorDefinition =
            declaration.getDeclarationAs(3, ConstructorDeclaration::class.java)
        assertNotNull(constructorDefinition)
        assertEquals(1, constructorDefinition.parameters.size)
        assertEquals(TypeParser.createFrom("int", true), constructorDefinition.parameters[0].type)
        assertEquals(
            FunctionType(
                "(int)SomeClass",
                listOf(TypeParser.createFrom("int", false)),
                listOf(TypeParser.createFrom("SomeClass", true))
            ),
            constructorDefinition.type
        )
        assertTrue(constructorDefinition.hasBody())

        val constructorDeclaration = recordDeclaration.constructors[1]
        assertNotNull(constructorDeclaration)
        assertFalse(constructorDeclaration.isDefinition)
        assertEquals(constructorDefinition, constructorDeclaration.definition)

        val main =
            declaration
                .getDeclarationsByName("main", FunctionDeclaration::class.java)
                .iterator()
                .next()
        assertNotNull(main)

        val methodCallWithConstant = main.getBodyStatementAs(2, CallExpression::class.java)
        assertNotNull(methodCallWithConstant)

        val arg = methodCallWithConstant.arguments[0]
        assertSame(constant, (arg as DeclaredReferenceExpression).refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testLiterals() {
        val file = File("src/test/resources/literals.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)
        val s = declaration.getDeclarationAs(0, VariableDeclaration::class.java)
        assertEquals(TypeParser.createFrom("char[]", true), s!!.type)
        assertEquals("s", s.name)

        var initializer = s.initializer as? Literal<*>
        assertNotNull(initializer)
        assertEquals("string", initializer.value)

        val i = declaration.getDeclarationAs(1, VariableDeclaration::class.java)
        assertEquals(TypeParser.createFrom("int", true), i!!.type)
        assertEquals("i", i.name)

        initializer = i.initializer as? Literal<*>
        assertNotNull(initializer)
        assertEquals(1, initializer.value)

        val f = declaration.getDeclarationAs(2, VariableDeclaration::class.java)
        assertEquals(TypeParser.createFrom("float", true), f!!.type)
        assertEquals("f", f.name)

        initializer = f.initializer as? Literal<*>
        assertNotNull(initializer)
        assertEquals(0.2f, initializer.value)

        val d = declaration.getDeclarationAs(3, VariableDeclaration::class.java)
        assertEquals(TypeParser.createFrom("double", true), d!!.type)
        assertEquals("d", d.name)

        initializer = d.initializer as? Literal<*>
        assertNotNull(initializer)
        assertEquals(0.2, initializer.value)

        val b = declaration.getDeclarationAs(4, VariableDeclaration::class.java)
        assertEquals(TypeParser.createFrom("bool", true), b!!.type)
        assertEquals("b", b.name)

        initializer = b.initializer as? Literal<*>
        assertNotNull(initializer)
        assertEquals(false, initializer.value)

        val c = declaration.getDeclarationAs(5, VariableDeclaration::class.java)
        assertEquals(TypeParser.createFrom("char", true), c!!.type)
        assertEquals("c", c.name)

        initializer = c.initializer as? Literal<*>
        assertNotNull(initializer)
        assertEquals('c', initializer.value)
    }

    @Test
    @Throws(Exception::class)
    fun testInitListExpression() {
        val file = File("src/test/resources/initlistexpression.cpp")
        val declaration = analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true)

        // x y = { 1, 2 };
        val y = declaration.getDeclarationAs(1, VariableDeclaration::class.java)
        assertEquals("y", y!!.name)

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
        assertEquals(TypeParser.createFrom("int[]", true), z!!.type)

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
        assertEquals(TypeParser.createFrom("Integer", true), i.type)

        // initializer should be a construct expression
        var constructExpression = i.initializer as? ConstructExpression
        assertNotNull(constructExpression)
        // type of the construct expression should also be Integer
        assertEquals(TypeParser.createFrom("Integer", true), constructExpression.type)

        // auto (Integer) m
        val m =
            (statement.statements[6] as DeclarationStatement).singleDeclaration
                as VariableDeclaration
        // type should be Integer*
        assertEquals(TypeParser.createFrom("Integer*", true), m.type)

        val constructor = constructExpression.constructor
        assertNotNull(constructor)
        assertEquals("Integer", constructor.name)
        assertFalse(constructor.isImplicit)

        // initializer should be a new expression
        val newExpression = m.initializer as? NewExpression
        assertNotNull(newExpression)
        // type of the new expression should also be Integer*
        assertEquals(TypeParser.createFrom("Integer*", true), newExpression.type)

        // initializer should be a construct expression
        constructExpression = newExpression.initializer as? ConstructExpression
        assertNotNull(constructExpression)
        // type of the construct expression should be Integer
        assertEquals(TypeParser.createFrom("Integer", true), constructExpression.type)

        // argument should be named k and of type m
        val k = constructExpression.arguments[0] as DeclaredReferenceExpression
        assertEquals("k", k.name)
        // type of the construct expression should also be Integer
        assertEquals(TypeParser.createFrom("int", true), k.type)
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
        assertEquals("y", die.lhs[0].name)
        assertEquals(0, (die.rhs as Literal<*>).value)

        die = initializer.initializers[1] as DesignatedInitializerExpression
        assertTrue(die.lhs[0] is DeclaredReferenceExpression)
        assertTrue(die.rhs is Literal<*>)
        assertEquals("z", die.lhs[0].name)
        assertEquals(1, (die.rhs as Literal<*>).value)

        die = initializer.initializers[2] as DesignatedInitializerExpression
        assertTrue(die.lhs[0] is DeclaredReferenceExpression)
        assertTrue(die.rhs is Literal<*>)
        assertEquals("x", die.lhs[0].name)
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
        assertEquals("x", die.lhs[0].name)
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
        val localNames = locals.map(Node::name).toSet()
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
        assertEquals("function_attribute", main.annotations[0].name)

        val someClass =
            tu.getDeclarationsByName("SomeClass", RecordDeclaration::class.java).iterator().next()
        assertNotNull(someClass)
        assertEquals("record_attribute", someClass.annotations[0].name)

        val a =
            someClass.fields
                .stream()
                .filter { f: FieldDeclaration -> f.name == "a" }
                .findAny()
                .orElse(null)
        assertNotNull(a)

        var annotation = a.annotations[0]
        assertNotNull(annotation)
        assertEquals("property_attribute", annotation.name)
        assertEquals(3, annotation.members.size)
        assertEquals("a", (annotation.members[0].value as Literal<*>).value)

        val b =
            someClass.fields
                .stream()
                .filter { f: FieldDeclaration -> f.name == "b" }
                .findAny()
                .orElse(null)
        assertNotNull(a)

        annotation = b.annotations[0]
        assertNotNull(annotation)
        assertEquals("property_attribute", annotation.name)
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
        assertEquals("size_t", initializer.castType.name)
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

        val typedefs = TypeManager.getInstance().frontend?.scopeManager?.currentTypedefs
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
}
