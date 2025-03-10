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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.InferenceConfiguration.Companion.builder
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin.ARRAY
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin.POINTER
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import de.fraunhofer.aisec.cpg.sarif.Region
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import java.nio.file.Path
import java.util.function.Consumer
import kotlin.Throws
import kotlin.test.*

internal class CXXLanguageFrontendTest : BaseTest() {
    @Test
    @Throws(Exception::class)
    fun testForEach() {
        val file = File("src/test/resources/cxx/foreachstmt.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        with(tu) {
            val main = tu.functions["main"]
            assertNotNull(main)

            val decl = main
            val ls = decl.variables["ls"]
            assertNotNull(ls)
            assertEquals(assertResolvedType("std::vector"), ls.type)
            assertLocalName("ls", ls)

            val forEachStatement = decl.forEachLoops.firstOrNull()
            assertNotNull(forEachStatement)

            // should loop over ls
            assertEquals(ls, (forEachStatement.iterable as Reference).refersTo)

            // should declare auto i (so far no concrete type inferrable)
            val stmt = forEachStatement.variable
            assertNotNull(stmt)
            assertTrue(stmt is DeclarationStatement)
            assertTrue(stmt.isSingleDeclaration())

            val i = stmt.singleDeclaration as VariableDeclaration
            assertNotNull(i)
            assertLocalName("i", i)
            assertIs<AutoType>(i.type)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testTryCatch() {
        val file = File("src/test/resources/components/trystmt.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val main = tu.functions["main"]
        assertNotNull(main)

        val tryStatement = main.trys.firstOrNull()
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
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val main = tu.functions["main"]
        with(tu) {
            assertNotNull(main)

            val funcDecl = main
            val i = funcDecl.variables["i"]
            assertNotNull(i)

            val sizeof = i.initializer as? TypeIdExpression
            assertNotNull(sizeof)
            assertLocalName("sizeof", sizeof)
            assertEquals(assertResolvedType("std::size_t"), sizeof.type)

            val typeInfo = funcDecl.variables["typeInfo"]
            assertNotNull(typeInfo)

            val typeid = typeInfo.initializer as? TypeIdExpression
            assertNotNull(typeid)
            assertLocalName("typeid", typeid)

            assertEquals(assertResolvedType("std::type_info").ref(), typeid.type)

            val j = funcDecl.variables["j"]
            assertNotNull(j)

            val alignOf = j.initializer as? TypeIdExpression
            assertNotNull(sizeof)
            assertNotNull(alignOf)
            assertLocalName("alignof", alignOf)
            assertEquals(assertResolvedType("std::size_t"), alignOf.type)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testCast() {
        val file = File("src/test/resources/cxx/castexpr.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        with(tu) {
            val main = tu.functions["main"]
            assertNotNull(main)

            val e = main.variables["e"]
            assertNotNull(e)
            assertEquals(assertResolvedType("ExtendedClass").pointer(), e.type)

            val b = main.variables["b"]
            assertNotNull(b)
            assertEquals(assertResolvedType("BaseClass").pointer(), b.type)

            // initializer
            var cast = b.initializer as? CastExpression
            assertNotNull(cast)
            assertEquals(assertResolvedType("BaseClass").pointer(), cast.castType)

            val staticCast = main.assigns.getOrNull(0)
            assertNotNull(staticCast)
            cast = staticCast.rhs<CastExpression>()
            assertNotNull(cast)
            assertLocalName("BaseClass*", cast)

            val reinterpretCast = main.assigns.getOrNull(0)
            assertNotNull(reinterpretCast)
            cast = reinterpretCast.rhs<CastExpression>()
            assertNotNull(cast)
            assertLocalName("BaseClass*", cast)

            val d = main.variables["d"]
            assertNotNull(d)

            cast = d.initializer as? CastExpression
            assertNotNull(cast)
            assertEquals(primitiveType("int"), cast.castType)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testArrays() {
        val file = File("src/test/resources/cxx/arrays.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val main = tu.functions["main"]
        with(tu) {
            assertNotNull(main)

            val statement = main.body as Block

            // first statement is the variable declaration
            val x =
                (statement.statements[0] as DeclarationStatement).singleDeclaration
                    as VariableDeclaration
            assertNotNull(x)
            assertEquals(primitiveType("int").array(), x.type)

            // initializer is an initializer list expression
            val ile = x.initializer as? InitializerListExpression
            assertNotNull(ile)

            val initializers = ile.initializers
            assertNotNull(initializers)
            assertEquals(3, initializers.size)

            // second statement is an expression directly
            val ase = statement.statements[1] as SubscriptExpression
            assertNotNull(ase)
            assertEquals(x, (ase.arrayExpression as Reference).refersTo)
            assertEquals(0, (ase.subscriptExpression as Literal<*>).value)

            // third statement declares a pointer to an array
            val a =
                (statement.statements[2] as? DeclarationStatement)?.singleDeclaration
                    as? VariableDeclaration
            assertNotNull(a)

            val type = a.type
            assertTrue(
                type is PointerType && type.pointerOrigin == PointerType.PointerOrigin.POINTER
            )

            val elementType = (a.type as? PointerType)?.elementType
            assertNotNull(elementType)
            assertTrue(elementType is PointerType && elementType.pointerOrigin == ARRAY)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testBlock() {
        val file = File("src/test/resources/compoundstmt.cpp")
        val declaration =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val function = declaration.declarations<FunctionDeclaration>(0)
        assertNotNull(function)

        val functionBody = function.body
        assertNotNull(functionBody)

        val statements = (functionBody as Block).statements
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
        val declaration =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val statements = declaration.declarations<FunctionDeclaration>(0)?.statements
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
        val memberCallExpr = statements[4] as MemberCallExpression
        assertLocalName("test", memberCallExpr.base)
        assertLocalName("c_str", memberCallExpr)
    }

    @Test
    @Throws(Exception::class)
    fun testIf() {
        val file = File("src/test/resources/if.cpp")
        val declaration =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val statements = declaration.declarations<FunctionDeclaration>(0)?.statements
        assertNotNull(statements)

        val ifStatement = statements[0] as IfStatement
        assertNotNull(ifStatement)
        assertNotNull(ifStatement.condition)
        assertEquals("bool", ifStatement.condition!!.type.typeName)
        assertEquals(true, (ifStatement.condition as Literal<*>).value)
        assertTrue((ifStatement.thenStatement as Block).statements[0] is ReturnStatement)
        assertTrue((ifStatement.elseStatement as Block).statements[0] is ReturnStatement)
    }

    @Test
    @Throws(Exception::class)
    fun testSwitch() {
        val file = File("src/test/resources/cfg/switch.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }

        assertTrue(tu.allChildren<Node>().isNotEmpty())

        val switchStatements = tu.allChildren<SwitchStatement>()
        assertTrue(switchStatements.size == 3)

        val switchStatement = switchStatements[0]
        assertTrue((switchStatement.statement as Block).statements.size == 11)

        val caseStatements = switchStatement.allChildren<CaseStatement>()
        assertTrue(caseStatements.size == 4)

        val defaultStatements = switchStatement.allChildren<DefaultStatement>()
        assertTrue(defaultStatements.size == 1)
    }

    @Test
    @Throws(Exception::class)
    fun testDeclarationStatement() {
        val file = File("src/test/resources/cxx/declstmt.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        with(tu) {
            val function = tu.declarations<FunctionDeclaration>(0)
            val statements = function?.statements
            assertNotNull(statements)
            statements.forEach(
                Consumer { node: Statement ->
                    log.debug("{}", node)
                    assertTrue(
                        node is DeclarationStatement ||
                            statements.indexOf(node) == statements.size - 1 &&
                                node is ReturnStatement
                    )
                }
            )

            val declFromMultiplicateExpression =
                (statements[0] as DeclarationStatement).getSingleDeclarationAs(
                    VariableDeclaration::class.java
                )
            assertEquals(
                assertResolvedType("SSL_CTX").pointer(),
                declFromMultiplicateExpression.type,
            )
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
            assertEquals(primitiveType("int").reference(POINTER), b.type)

            val c = twoDeclarations[1] as VariableDeclaration
            assertNotNull(c)
            assertLocalName("c", c)
            assertEquals(primitiveType("int"), c.type)

            val withoutInitializer =
                (statements[3] as DeclarationStatement).getSingleDeclarationAs(
                    VariableDeclaration::class.java
                )
            initializer = withoutInitializer.initializer
            assertEquals(primitiveType("int").reference(POINTER), withoutInitializer.type)
            assertLocalName("d", withoutInitializer)
            assertNull(initializer)

            val qualifiedType =
                (statements[4] as DeclarationStatement).getSingleDeclarationAs(
                    VariableDeclaration::class.java
                )
            assertEquals(objectType("std::string"), qualifiedType.type)
            assertLocalName("text", qualifiedType)
            assertTrue(qualifiedType.initializer is Literal<*>)
            assertEquals("some text", (qualifiedType.initializer as? Literal<*>)?.value)

            val pointerWithAssign =
                (statements[5] as DeclarationStatement).getSingleDeclarationAs(
                    VariableDeclaration::class.java
                )
            assertEquals(incompleteType().reference(POINTER), pointerWithAssign.type)
            assertLocalName("ptr2", pointerWithAssign)
            assertLiteralValue(null, pointerWithAssign.initializer)

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
    }

    @Test
    @Throws(Exception::class)
    fun testAssignmentExpression() {
        val file = File("src/test/resources/cxx/assignmentexpression.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }

        // just take a look at the second function
        val main = tu.functions["main"]
        assertNotNull(main)

        val statements = main.statements
        assertNotNull(statements)

        val a = main.variables["a"]
        val assignA = statements[1]
        assertTrue(assignA is AssignExpression)

        var lhs = assignA.lhs<Expression>()
        var rhs = assignA.rhs<Expression>()
        assertLocalName("a", lhs)
        assertEquals(2, (rhs as? Literal<*>)?.value)
        assertRefersTo(lhs, a)

        val b = main.variables["b"]

        // a = b
        val assignB = statements[3]
        assertTrue(assignB is AssignExpression)

        lhs = assignB.lhs()
        rhs = assignB.rhs()
        assertLocalName("a", lhs)
        assertTrue(rhs is Reference)
        assertLocalName("b", rhs)
        assertRefersTo(rhs, b)

        val assignBWithFunction = statements[4]
        assertTrue(assignBWithFunction is AssignExpression)
        assertLocalName("a", assignBWithFunction.lhs())

        val call = assignBWithFunction.rhs<CallExpression>()
        assertNotNull(call)
        assertLocalName("someFunction", call)
        assertRefersTo(call.arguments[0], b)
    }

    @Test
    @Throws(Exception::class)
    fun testShiftExpression() {
        val file = File("src/test/resources/shiftexpression.cpp")
        val declaration =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val functionDecl = declaration.declarations<FunctionDeclaration>(0)
        val statements = functionDecl?.statements
        assertNotNull(statements)
        assertTrue(statements[1] is BinaryOperator)
    }

    @Test
    @Throws(Exception::class)
    fun testUnaryOperator() {
        val file = File("src/test/resources/unaryoperator.cpp")
        val unit =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val statements = unit.declarations<FunctionDeclaration>(0)?.statements
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
        val assign = statements[++line] as AssignExpression

        val dereference = assign.rhs<UnaryOperator>()
        assertNotNull(dereference)
        input = dereference.input
        assertLocalName("ptr", input)
        assertEquals("*", dereference.operatorCode)
        assertTrue(dereference.isPrefix)
    }

    @Test
    @Throws(Exception::class)
    fun testBinaryOperator() {
        val file = File("src/test/resources/cxx/binaryoperator.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }

        val main = tu.functions["main"]
        assertNotNull(main)

        val statements = main.statements
        assertNotNull(statements)
        // first two statements are just declarations

        // a = b * 2
        var assign = statements[2] as? AssignExpression
        assertNotNull(assign)

        var ref = assign.lhs<Reference>()
        assertNotNull(ref)
        assertLocalName("a", ref)

        var binOp = assign.rhs<BinaryOperator>()
        assertNotNull(binOp)

        assertTrue(binOp.lhs is Reference)
        assertLocalName("b", binOp.lhs)
        assertTrue(binOp.rhs is Literal<*>)
        assertEquals(2, (binOp.rhs as Literal<*>).value)

        // a = 1 * 1
        assign = statements[3] as? AssignExpression
        assertNotNull(assign)

        ref = assign.lhs<Reference>()
        assertNotNull(ref)
        assertLocalName("a", ref)

        binOp = assign.rhs<BinaryOperator>()
        assertNotNull(binOp)

        assertTrue(binOp.lhs is Literal<*>)
        assertEquals(1, (binOp.lhs as Literal<*>).value)
        assertTrue(binOp.rhs is Literal<*>)
        assertEquals(1, (binOp.rhs as Literal<*>).value)

        // std::string* notMultiplication
        // this is not a multiplication, but a variable declaration with a pointer type, but
        // syntactically no different from the previous ones
        val stmt = statements[4] as DeclarationStatement
        val decl = stmt.singleDeclaration as VariableDeclaration
        with(tu) { assertEquals(objectType("std::string").pointer(), decl.type) }
        assertLocalName("notMultiplication", decl)
        assertTrue(decl.initializer is BinaryOperator)

        binOp = decl.initializer as? BinaryOperator
        assertNotNull(binOp)
        assertTrue(binOp.lhs is Literal<*>)
        assertEquals(0, (binOp.lhs as Literal<*>).value)
        assertTrue(binOp.rhs is Literal<*>)
        assertEquals(0, (binOp.rhs as Literal<*>).value)
    }

    @Test
    @Throws(Exception::class)
    fun testRecordDeclaration() {
        val file = File("src/test/resources/cxx/recordstmt.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val language = tu.ctx?.availableLanguage<CPPLanguage>()
        assertNotNull(language)
        assertEquals(language, tu.language)

        val recordDeclaration = tu.records.firstOrNull()
        assertNotNull(recordDeclaration)
        assertLocalName("SomeClass", recordDeclaration)
        assertEquals("class", recordDeclaration.kind)
        assertEquals(2, recordDeclaration.fields.size)

        val field = recordDeclaration.fields["field"]
        assertNotNull(field)

        val constant = recordDeclaration.fields["CONSTANT"]
        assertNotNull(constant)
        assertEquals(tu.incompleteType().reference(POINTER), field.type)
        assertEquals(4, recordDeclaration.methods.size)

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
        assertEquals(tu.primitiveType("int"), methodWithParam.parameters[0].type)
        assertEquals(
            FunctionType(
                "(int)void*",
                listOf(tu.primitiveType("int")),
                listOf(tu.incompleteType().reference(POINTER)),
                language,
            ),
            methodWithParam.type,
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
            FunctionType(
                "()void*",
                listOf(),
                listOf(tu.incompleteType().reference(POINTER)),
                language,
            ),
            inlineMethod.type,
        )
        assertTrue(inlineMethod.hasBody())

        val inlineConstructor = recordDeclaration.constructors[0]
        assertEquals(recordDeclaration.name.localName, inlineConstructor.name.localName)
        assertEquals(
            FunctionType("()SomeClass", listOf(), listOf(tu.objectType("SomeClass")), language),
            inlineConstructor.type,
        )
        assertTrue(inlineConstructor.hasBody())

        val constructorDefinition = tu.declarations<ConstructorDeclaration>(3)
        assertNotNull(constructorDefinition)
        assertEquals(1, constructorDefinition.parameters.size)
        assertEquals(tu.primitiveType("int"), constructorDefinition.parameters[0].type)
        assertEquals(
            FunctionType(
                "(int)SomeClass",
                listOf(tu.primitiveType("int")),
                listOf(tu.objectType("SomeClass")),
                language,
            ),
            constructorDefinition.type,
        )
        assertTrue(constructorDefinition.hasBody())

        val constructorDeclaration = recordDeclaration.constructors[1]
        assertNotNull(constructorDeclaration)
        assertFalse(constructorDeclaration.isDefinition)
        assertEquals(constructorDefinition, constructorDeclaration.definition)

        val main = tu.functions["main"]
        assertNotNull(main)

        val methodCallWithConstant = main.calls("method").getOrNull(1)
        assertNotNull(methodCallWithConstant)

        val arg = methodCallWithConstant.arguments[0]
        assertSame(constant, (arg as Reference).refersTo)

        val anotherMethod = tu.methods["anotherMethod"]
        assertNotNull(anotherMethod)
        assertFullName("OtherClass::anotherMethod", anotherMethod)
    }

    @Test
    @Throws(Exception::class)
    fun testLiterals() {
        val file = File("src/test/resources/cxx/literals.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }

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
        assertLocalName("unsigned long long int", hex.type)

        val duration_ms = tu.variables["duration_ms"]
        assertNotNull(duration_ms)
        assertIs<ProblemExpression>(duration_ms.initializer)

        val duration_s = tu.variables["duration_s"]
        assertNotNull(duration_s)
        assertIs<ProblemExpression>(duration_s.initializer)
    }

    @Test
    @Throws(Exception::class)
    fun testInitListExpression() {
        val file = File("src/test/resources/initlistexpression.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }

        // x y = { 1, 2 };
        val y = tu.declarations<VariableDeclaration>(1)
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
        val z = tu.declarations<VariableDeclaration>(2)
        assertNotNull(z)
        with(tu) { assertEquals(primitiveType("int").array(), z.type) }

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
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)
        with(tu) {
            // get the main method
            val main = tu.declarations<FunctionDeclaration>(3)
            val statement = main!!.body as Block

            // Integer i
            val i =
                (statement.statements[0] as DeclarationStatement).singleDeclaration
                    as VariableDeclaration
            // type should be Integer
            assertEquals(assertResolvedType("Integer"), i.type)

            // initializer should be a construct expression
            var constructExpr = i.initializer as? ConstructExpression
            assertNotNull(constructExpr)
            // type of the construct expression should also be Integer
            assertEquals(assertResolvedType("Integer"), constructExpr.type)

            // auto (Integer) m
            val m =
                (statement.statements[6] as DeclarationStatement).singleDeclaration
                    as VariableDeclaration
            // type should be Integer*
            assertEquals(assertResolvedType("Integer").pointer(), m.type)

            val constructor = constructExpr.constructor
            assertNotNull(constructor)
            assertLocalName("Integer", constructor)
            assertFalse(constructor.isImplicit)

            // initializer should be a new expression
            val newExpression = m.initializer as? NewExpression
            assertNotNull(newExpression)
            // type of the new expression should also be Integer*
            assertEquals(assertResolvedType("Integer").pointer(), newExpression.type)

            // initializer should be a construct expression
            constructExpr = newExpression.initializer as? ConstructExpression
            assertNotNull(constructExpr)
            // type of the construct expression should be Integer
            assertEquals(assertResolvedType("Integer"), constructExpr.type)

            // argument should be named k and of type m
            val k = constructExpr.arguments[0] as Reference
            assertLocalName("k", k)
            // type of the construct expression should also be Integer
            assertEquals(assertResolvedType("int"), k.type)
        }
    }

    private val FunctionDeclaration.statements: List<Statement>?
        get() {
            return (this.body as? Block)?.statements
        }

    @Test
    @Throws(Exception::class)
    fun testRegionsCfg() {
        val file = File("src/test/resources/cfg.cpp")
        val declaration =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val fdecl = declaration.declarations<FunctionDeclaration>(0)
        val body = fdecl!!.body as Block
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
    fun testCDesignatedInitializer() {
        val file = File("src/test/resources/c/designated.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }

        val foo3 = tu.variables["foo3"]
        assertNotNull(foo3)

        val init = foo3.initializer
        assertIs<InitializerListExpression>(init)

        val assign = init.initializers.firstOrNull()
        assertIs<AssignExpression>(assign)

        val lhs = assign.lhs<SubscriptExpression>(0)
        assertNotNull(lhs)
    }

    @Test
    @Throws(Exception::class)
    fun testCPPDesignatedInitializer() {
        val file = File("src/test/resources/cxx/designated.cpp")
        val declaration =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }

        val method = declaration.functions["main"]
        assertNotNull(method)
        assertEquals("main()int", method.signature)
        assertTrue(method.body is Block)

        val statements = (method.body as Block).statements
        assertEquals(5, statements.size)
        assertTrue(statements[0] is DeclarationStatement)
        assertTrue(statements[1] is DeclarationStatement)
        assertTrue(statements[2] is DeclarationStatement)
        assertTrue(statements[3] is DeclarationStatement)
        assertTrue(statements[4] is ReturnStatement)

        var initializer =
            ((statements[0] as DeclarationStatement).singleDeclaration as VariableDeclaration)
                .initializer
        assertTrue(initializer is InitializerListExpression)
        assertEquals(3, initializer.initializers.size)
        assertTrue(initializer.initializers[0] is AssignExpression)
        assertTrue(initializer.initializers[1] is AssignExpression)
        assertTrue(initializer.initializers[2] is AssignExpression)

        var die = initializer.initializers[0] as AssignExpression
        assertTrue(die.lhs[0] is Reference)
        assertTrue(die.rhs[0] is Literal<*>)
        assertLocalName("y", die.lhs[0])
        assertEquals(0, (die.rhs[0] as Literal<*>).value)

        die = initializer.initializers[1] as AssignExpression
        assertTrue(die.lhs[0] is Reference)
        assertTrue(die.rhs[0] is Literal<*>)
        assertLocalName("z", die.lhs[0])
        assertEquals(1, (die.rhs[0] as Literal<*>).value)

        die = initializer.initializers[2] as AssignExpression
        assertTrue(die.lhs[0] is Reference)
        assertTrue(die.rhs[0] is Literal<*>)
        assertLocalName("x", die.lhs[0])
        assertEquals(2, (die.rhs[0] as Literal<*>).value)

        initializer =
            ((statements[1] as DeclarationStatement).singleDeclaration as VariableDeclaration)
                .initializer
        assertTrue(initializer is InitializerListExpression)
        assertEquals(1, initializer.initializers.size)
        assertTrue(initializer.initializers[0] is AssignExpression)

        die = initializer.initializers[0] as AssignExpression
        assertTrue(die.lhs[0] is Reference)
        assertTrue(die.rhs[0] is Literal<*>)
        assertLocalName("x", die.lhs[0])
        assertEquals(20, (die.rhs[0] as Literal<*>).value)

        initializer =
            ((statements[3] as DeclarationStatement).singleDeclaration as VariableDeclaration)
                .initializer
        assertTrue(initializer is InitializerListExpression)
        assertEquals(2, initializer.initializers.size)
        assertTrue(initializer.initializers[0] is AssignExpression)
        assertTrue(initializer.initializers[1] is AssignExpression)

        die = initializer.initializers[0] as AssignExpression
        assertLiteralValue(3, (die.lhs[0] as SubscriptExpression).subscriptExpression)
        assertLiteralValue(1, die.rhs[0])

        die = initializer.initializers[1] as AssignExpression
        assertLiteralValue(5, (die.lhs[0] as SubscriptExpression).subscriptExpression)
        assertLiteralValue(2, die.rhs[0])

        val o = declaration.variables["o"]
        assertNotNull(o)
    }

    @Test
    @Throws(Exception::class)
    fun testLocalVariables() {
        val file = File("src/test/resources/variables/local_variables.cpp")
        val declaration =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }

        val function = declaration.functions["testExpressionInExpressionList"]
        assertNotNull(function)
        assertEquals("testExpressionInExpressionList()int", function.signature)

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
        val file = File("src/test/resources/cxx/foreachstmt.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val main = tu.functions["main"]
        assertNotNull(main)

        val location = main.location
        assertNotNull(location)

        val path = Path.of(location.artifactLocation.uri)
        assertEquals("foreachstmt.cpp", path.fileName.toString())
        assertEquals(Region(4, 1, 8, 2), location.region)
    }

    @Test
    @Throws(Exception::class)
    fun testNamespaces() {
        val file = File("src/test/resources/namespaces.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        val firstNamespace = tu.namespaces["FirstNamespace"]
        assertNotNull(firstNamespace)

        val someClass = firstNamespace.records["SomeClass"]
        assertNotNull(someClass)

        val anotherClass = tu.records["AnotherClass"]
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
                    .registerLanguage<CPPLanguage>()
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

        val main = tu.functions["main"]
        assertNotNull(main)
        assertLocalName("function_attribute", main.annotations[0])

        val someClass = tu.records["SomeClass"]
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
            (annotation.members[0].value as Literal<*>).value,
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
                    .registerLanguage<CPPLanguage>()
            )
        assertEquals(1, declarations.size)
        // should contain 3 declarations (2 include and 1 function decl from the include)
        assertEquals(3, declarations[0].declarations.size)
    }

    @Test
    @Throws(Exception::class)
    fun testEOGCompleteness() {
        val file = File("src/test/resources/fix-455/main.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val main = tu.functions["main"]
        assertNotNull(main)

        val body = main.body as Block
        assertNotNull(body)

        val returnStatement = body.statements[body.statements.size - 1]
        assertNotNull(returnStatement)

        // we need to assert, that we have a consistent chain of EOG edges from the first statement
        // to the return statement. otherwise, the EOG chain is somehow broken
        val eogEdges = ArrayList<Node>()
        main.accept(
            Strategy::EOG_FORWARD,
            object : IVisitor<Node>() {
                override fun visit(t: Node) {
                    println(t)
                    eogEdges.add(t)
                }
            },
        )
        assertTrue(eogEdges.contains(returnStatement))
    }

    @Test
    @Throws(Exception::class)
    fun testParenthesis() {
        val file = File("src/test/resources/cxx/parenthesis.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val main = tu.functions["main"]
        assertNotNull(main)

        val count = tu.variables["count"]
        assertNotNull(count)

        var cast = count.initializer
        assertIs<CastExpression>(cast)
        assertLocalName("int", cast.castType)
        assertLiteralValue(42, cast.expression)

        val addr = tu.variables["addr"]
        assertNotNull(addr)

        cast = addr.initializer
        assertIs<CastExpression>(cast)
        assertLocalName("int64_t", cast.castType)

        val unary = cast.expression
        assertIs<UnaryOperator>(unary)

        val refCount = unary.input
        assertIs<Reference>(refCount)
        assertRefersTo(refCount, count)

        var paths = addr.followPrevFullDFGEdgesUntilHit { it == refCount }
        assertTrue(paths.fulfilled.isNotEmpty())
        assertTrue(paths.failed.isEmpty())

        val refKey = tu.refs["key"]
        assertNotNull(refKey)

        val assign = tu.assignments.firstOrNull { it.value is UnaryOperator }
        assertNotNull(assign)
        paths = assign.value.followPrevFullDFGEdgesUntilHit { it == refKey }
        assertTrue(paths.fulfilled.isNotEmpty())
        assertTrue(paths.failed.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testCppThis() {
        val file = File("src/test/resources/cpp-this-field.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        val main = tu.functions["main"]
        assertNotNull(main)

        val classT = tu.records["T"]
        assertNotNull(classT)

        val classTFoo = classT.methods.firstOrNull()
        assertNotNull(classTFoo)

        val classTReturn = classTFoo.returns.firstOrNull()
        assertNotNull(classTReturn)

        val classTReturnMemberExpression = classTReturn.returnValue as? MemberExpression
        assertNotNull(classTReturnMemberExpression)

        val classTThisExpression = classTReturnMemberExpression.base as? Reference
        assertEquals(classTThisExpression?.refersTo, classTFoo.receiver)

        val classS = tu.records["S"]
        assertNotNull(classS)

        val classSFoo = classS.methods.firstOrNull()
        assertNotNull(classSFoo)

        val classSReturn = classSFoo.bodyOrNull<ReturnStatement>()
        assertNotNull(classSReturn)

        val classSReturnMemberExpression = classSReturn.returnValue as? MemberExpression
        assertNotNull(classSReturnMemberExpression)

        val classSThisExpression = classSReturnMemberExpression.base as? Reference
        assertEquals(classSThisExpression?.refersTo, classSFoo.receiver)
        assertNotEquals(classTFoo, classSFoo)
        assertNotEquals(classTFoo.receiver, classSFoo.receiver)
    }

    @Test
    @Throws(Exception::class)
    fun testEnum() {
        val file = File("src/test/resources/c/enum.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        // TU should only contain three AST declarations (EnumDeclaration, FunctionDeclaration,
        // TypedefDeclaration),
        // but NOT any EnumConstantDeclarations
        assertEquals(3, tu.declarations.size)

        val main = tu.functions["main"]
        assertNotNull(main)

        val returnStatement = main.bodyOrNull<ReturnStatement>()
        assertNotNull(returnStatement)
        assertNotNull((returnStatement.returnValue as? Reference)?.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testEnumCPP() {
        val file = File("src/test/resources/cxx/enum.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        // TU should only contain two AST declarations (EnumDeclaration and FunctionDeclaration),
        // but NOT any EnumConstantDeclarations
        assertEquals(2, tu.declarations.size)

        val main = tu.functions["main"]
        assertNotNull(main)

        val returnStatement = main.bodyOrNull<ReturnStatement>()
        assertNotNull(returnStatement)
        assertNotNull((returnStatement.returnValue as? Reference)?.refersTo)
    }

    @Test
    @Throws(Exception::class)
    fun testStruct() {
        val file = File("src/test/resources/c/struct.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }

        val main = tu.functions["main"]
        assertNotNull(main)

        val myStruct = tu.records["MyStruct"]
        assertNotNull(myStruct)

        val field = myStruct.fields["field"]
        assertNotNull(field)

        val s = main.variables["s"]
        assertNotNull(s)

        assertEquals(myStruct, (s.type as? ObjectType)?.recordDeclaration)
    }

    @Test
    @Throws(Exception::class)
    fun testTypedef() {
        val file = File("src/test/resources/c/typedef_in_header/main.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        with(tu) {
            val typedefs = tu.ctx?.scopeManager?.typedefFor(Name("MyStruct"))
            assertLocalName("__myStruct", typedefs)

            val main = tu.functions["main"]
            assertNotNull(main)

            val call = main.calls.firstOrNull()
            assertNotNull(call)
            assertTrue(call.invokes.isNotEmpty())

            val func = call.invokes.firstOrNull()
            assertNotNull(func)
            assertFalse(func.isInferred)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testFunctionPointerToClassMethodSimple() {
        val file = File("src/test/resources/cxx/funcptr_class_simple.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }

        val myClass = tu.records["MyClass"]
        assertNotNull(myClass)

        val targetNoParam =
            myClass.methods[{ it.name.localName == "target" && it.parameters.isEmpty() }]
        assertNotNull(targetNoParam)

        val targetSingleParam =
            myClass.methods[{ it.name.localName == "target" && it.parameters.size == 1 }]
        assertNotNull(targetSingleParam)

        val main = tu.functions["main"]
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
            noParam.followPrevFullDFGEdgesUntilHit { it == targetNoParam }.fulfilled.isNotEmpty()
        )

        // ensure that our function pointer variable is connected to the method declaration via DFG
        val singleParam = main.variables["single_param"]
        assertNotNull(singleParam)
        assertTrue(
            singleParam
                .followPrevFullDFGEdgesUntilHit { it == targetSingleParam }
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
    fun testFunctionPointerCallWithCDFG() {
        val file = File("src/test/resources/c/func_ptr_call.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), false) {
                it.registerLanguage<CLanguage>()
                it.registerPass<TypeHierarchyResolver>()
                it.registerPass<CXXExtraPass>()
                it.registerPass<SymbolResolver>()
                it.registerPass<DFGPass>()
                it.registerPass<EvaluationOrderGraphPass>() // creates EOG
                it.registerPass<TypeResolver>()
                it.registerPass<ControlFlowSensitiveDFGPass>()
                it.registerPass<DynamicInvokeResolver>()
                it.registerPass<FilenameMapper>()
            }

        val target = tu.functions["target"]
        assertNotNull(target)

        val main = tu.functions["main"]
        assertNotNull(main)

        // We do not want any inferred functions
        assertTrue(tu.functions.none { it.isInferred })

        val noParamPointerCall = tu.calls("no_param").firstOrNull { it.callee is UnaryOperator }
        assertInvokes(assertNotNull(noParamPointerCall), target)

        val noParamNoInitPointerCall =
            tu.calls("no_param_uninitialized").firstOrNull { it.callee is UnaryOperator }
        assertInvokes(assertNotNull(noParamNoInitPointerCall), target)

        val noParamCall = tu.calls("no_param").firstOrNull { it.callee is Reference }
        assertInvokes(assertNotNull(noParamCall), target)

        val noParamNoInitCall =
            tu.calls("no_param_uninitialized").firstOrNull { it.callee is Reference }
        assertInvokes(assertNotNull(noParamNoInitCall), target)

        val targetCall = tu.calls["target"]
        assertInvokes(assertNotNull(targetCall), target)
    }

    @Test
    @Throws(Exception::class)
    fun testFunctionPointerCallWithNormalDFG() {
        val file = File("src/test/resources/c/func_ptr_call.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), false) {
                it.registerLanguage<CLanguage>()
                it.registerPass<TypeHierarchyResolver>()
                it.registerPass<CXXExtraPass>()
                it.registerPass<SymbolResolver>()
                it.registerPass<DFGPass>()
                it.registerPass<EvaluationOrderGraphPass>() // creates EOG
                it.registerPass<TypeResolver>()
                it.registerPass<DynamicInvokeResolver>()
                it.registerPass<ControlFlowSensitiveDFGPass>()
                it.registerPass<FilenameMapper>()
            }

        val target = tu.functions["target"]
        assertNotNull(target)

        val main = tu.functions["main"]
        assertNotNull(main)

        // We do not want any inferred functions
        assertTrue(tu.functions.none { it.isInferred })

        val noParamPointerCall = tu.calls("no_param").firstOrNull { it.callee is UnaryOperator }
        assertInvokes(assertNotNull(noParamPointerCall), target)

        val noParamNoInitPointerCall =
            tu.calls("no_param_uninitialized").firstOrNull { it.callee is UnaryOperator }
        assertInvokes(assertNotNull(noParamNoInitPointerCall), target)

        val noParamCall = tu.calls("no_param").firstOrNull { it.callee is Reference }
        assertInvokes(assertNotNull(noParamCall), target)

        val noParamNoInitCall =
            tu.calls("no_param_uninitialized").firstOrNull { it.callee is Reference }
        assertInvokes(assertNotNull(noParamNoInitCall), target)

        val targetCall = tu.calls["target"]
        assertInvokes(assertNotNull(targetCall), target)
    }

    @Test
    @Throws(Exception::class)
    fun testNamespacedFunction() {
        val file = File("src/test/resources/cxx/namespaced_function.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
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

    @Test
    @Throws(Exception::class)
    fun testLambdas() {
        val file = File("src/test/resources/cxx/lambdas.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)
    }

    @Test
    @Throws(Exception::class)
    fun testCFunctionReturnType() {
        val file = File("src/test/resources/c/types.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        assertLocalName("int", tu.functions["main"]?.returnTypes?.firstOrNull())
    }

    @Test
    @Throws(Exception::class)
    fun testFancyTypes() {
        val file = File("src/test/resources/cxx/fancy_types.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        val ptr = tu.variables["ptr"]
        assertNotNull(ptr)
        assertLocalName("decltype(nullptr)", ptr.type)
    }

    @Test
    fun testRecursiveHeaderFunction() {
        val file = File("src/test/resources/cxx/fix-1226")
        val result =
            analyze(
                listOf(file.resolve("main1.cpp"), file.resolve("main2.cpp")),
                file.toPath(),
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)

        // For now, we have duplicate functions because we include the header twice. This might
        // change in the future. The important thing is that this gets parsed at all because we
        // previously had a loop in our equals method
        val functions = result.functions { it.name.localName == "foo" && it.isDefinition }
        assertEquals(2, functions.size)
    }

    @Test
    fun testUsing() {
        val file = File("src/test/resources/cxx/using.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.inferenceConfiguration(builder().enabled(false).build())
            }
        assertNotNull(result)

        // There should be no type "string" anymore, only "std::string"
        assertFalse(result.finalCtx.typeManager.typeExists("string"))
        assertTrue(result.finalCtx.typeManager.typeExists("std::string"))

        // the same applies to "inner::secret"
        assertFalse(result.finalCtx.typeManager.typeExists("secret"))
        assertFalse(result.finalCtx.typeManager.typeExists("inner::secret"))
        assertTrue(result.finalCtx.typeManager.typeExists("std::inner::secret"))
    }

    @Test
    fun testSymbolResolverFail() {
        val file = File("src/test/resources/c/symbol_resolver_fail.c")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(result)

        val doCall = result.calls["do_call"]
        assertNotNull(doCall)
        assertTrue(doCall.invokes.isNotEmpty())
    }

    @Test
    fun testSwitchEOG() {
        val file = File("src/test/resources/c/switch_eog.c")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(result)

        val printf = result.calls["printf"]
        assertNotNull(printf)
        assertTrue(printf.prevEOG.isNotEmpty())
        assertTrue(printf.invokes.isNotEmpty())
    }

    @Test
    fun testExternC() {
        val file = File("src/test/resources/cxx/extern_c.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)

        val test = result.functions["test"]
        assertNotNull(test)
    }

    @Test
    @Throws(Exception::class)
    fun testCastToInferredType() {
        val file = File("src/test/resources/c/cast_to_inferred.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        val assign = tu.assigns.firstOrNull()
        assertNotNull(assign)

        val cast = assign.rhs.singleOrNull()
        assertIs<CastExpression>(cast)
        assertLocalName("mytype", cast.castType)
    }

    @Test
    fun testGoto() {
        val file = File("src/test/resources/c/goto.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        val labelCName = "LAB_123"

        val goto = tu.allChildren<GotoStatement>().firstOrNull()
        assertIs<GotoStatement>(goto)
        assertEquals(labelCName, goto.labelName)
        assertLocalName(labelCName, goto)

        val label = tu.labels[labelCName]
        assertIs<LabelStatement>(label)
        assertLocalName(labelCName, label)

        assertEquals(label, goto.targetLabel)
    }
}
