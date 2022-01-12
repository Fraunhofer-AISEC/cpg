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
package de.fraunhofer.aisec.cpg.frontends.cpp2

import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.TestUtils.analyzeWithBuilder
import de.fraunhofer.aisec.cpg.TestUtils.assertRefersTo
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.sarif.Region
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import kotlin.test.*

class CXXLanguageFrontend2Test {
    @Test
    fun testBinaryOperator() {
        val file = File("src/test/resources/binaryoperator.cpp")

        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        val statements: List<Statement> =
            (tu.getDeclarationAs(0, FunctionDeclaration::class.java)?.body as CompoundStatement)
                .statements

        // first two statements are just declarations
        // a = b * 2
        var operator = statements[2] as BinaryOperator

        assertEquals("a", operator.lhs.name)
        assertTrue(operator.rhs is BinaryOperator)

        var rhs = operator.rhs as BinaryOperator

        assertTrue(rhs.lhs is DeclaredReferenceExpression)
        assertEquals("b", rhs.lhs.name)
        assertTrue(rhs.rhs is Literal<*>)
        assertEquals(2, (rhs.rhs as Literal<*>).value)

        // a = 1 * 1
        operator = statements[3] as BinaryOperator

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

        operator = decl.initializer as BinaryOperator

        assertTrue(operator.lhs is Literal<*>)
        assertEquals(0, (operator.lhs as Literal<*>).value)

        assertTrue(operator.rhs is Literal<*>)
        assertEquals(0, (operator.rhs as Literal<*>).value)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testUnaryOperator() {
        val file = File("src/test/resources/unaryoperator.cpp")

        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        val statements: List<Statement> =
            (tu.getDeclarationAs(0, FunctionDeclaration::class.java)?.body as CompoundStatement)
                .statements

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
        val sizeof = declaration.initializer as UnaryOperator
        input = sizeof.input
        assertEquals("a", input.name)
        assertEquals("sizeof", sizeof.operatorCode)
        assertTrue(sizeof.isPrefix)

        // bool b = !false;
        statement = statements[++line] as DeclarationStatement
        declaration = statement.singleDeclaration as VariableDeclaration
        val negation = declaration.initializer as UnaryOperator
        input = negation.input
        assertTrue(input is Literal<*>)
        assertEquals(false, (input as Literal<*>).value)
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
    @Throws(java.lang.Exception::class)
    fun testShiftExpression() {
        val file = File("src/test/resources/shiftexpression.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }
        val functionDeclaration = tu.getDeclarationAs(0, FunctionDeclaration::class.java)
        val statements: List<Statement> =
            (functionDeclaration?.body as CompoundStatement).statements
        assertTrue(statements[1] is BinaryOperator)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testPostfixExpression() {
        val file = File("src/test/resources/postfixexpression.cpp")

        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        val statements: List<Statement> =
            (tu.getDeclarationAs(0, FunctionDeclaration::class.java)?.body as CompoundStatement)
                .statements

        assertEquals(6, statements.size)
        val callExpression = statements[0] as CallExpression
        assertEquals("printf", callExpression.name)
        val arg = callExpression.arguments[0]
        assertTrue(arg is Literal<*>)
        assertEquals("text", (arg as Literal<*>).value)
        val unaryOperatorPlus = statements[1] as UnaryOperator
        assertEquals(UnaryOperator.OPERATOR_POSTFIX_INCREMENT, unaryOperatorPlus.operatorCode)
        assertTrue(unaryOperatorPlus.isPostfix)
        val unaryOperatorMinus = statements[2] as UnaryOperator
        assertEquals(UnaryOperator.OPERATOR_POSTFIX_DECREMENT, unaryOperatorMinus.operatorCode)
        assertTrue(unaryOperatorMinus.isPostfix)

        // 4th statement is not yet parsed correctly
        val memberCallExpression = statements[4] as MemberCallExpression
        assertEquals("test", memberCallExpression.base.name)
        assertEquals("c_str", memberCallExpression.name)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testLiterals() {
        val file = File("src/test/resources/literals.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        val s = tu.getDeclarationAs(0, VariableDeclaration::class.java)
        assertEquals(TypeParser.createFrom("char[]", true), s!!.type)
        assertEquals("s", s.name)
        var initializer: Expression? = s.initializer
        assertEquals("string", (initializer as Literal<*>).value)

        val i = tu.getDeclarationAs(1, VariableDeclaration::class.java)
        assertEquals(TypeParser.createFrom("int", true), i!!.type)
        assertEquals("i", i.name)
        initializer = i.initializer
        assertEquals(1, (initializer as Literal<*>).value)

        val f = tu.getDeclarationAs(2, VariableDeclaration::class.java)
        assertEquals(TypeParser.createFrom("float", true), f!!.type)
        assertEquals("f", f.name)
        initializer = f.initializer
        assertEquals(0.2f, (initializer as Literal<*>).value)

        val d = tu.getDeclarationAs(3, VariableDeclaration::class.java)
        assertEquals(TypeParser.createFrom("double", true), d!!.type)
        assertEquals("d", d.name)
        initializer = d.initializer
        assertEquals(0.2, (initializer as Literal<*>).value)

        val b = tu.getDeclarationAs(4, VariableDeclaration::class.java)
        assertEquals(TypeParser.createFrom("bool", true), b!!.type)
        assertEquals("b", b.name)
        initializer = b.initializer
        assertEquals(false, (initializer as Literal<*>).value)

        val c = tu.getDeclarationAs(5, VariableDeclaration::class.java)
        assertEquals(TypeParser.createFrom("char", true), c!!.type)
        assertEquals("c", c.name)
        initializer = c.initializer
        assertEquals('c', (initializer as Literal<*>).value)
    }

    @Test
    @Throws(Exception::class)
    fun testDeclarationStatement() {
        val file = File("src/test/resources/declstmt.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        val statements: List<Statement> =
            (tu.getDeclarationAs(0, FunctionDeclaration::class.java)?.body as? CompoundStatement)
                ?.statements
                ?: listOf()

        statements.forEach(
            Consumer { node: Statement ->
                assertTrue(
                    node is DeclarationStatement ||
                        statements.indexOf(node) == statements.size - 1 && node is ReturnStatement
                )
            }
        )
        val declfromMultiplyExpression =
            (statements[0] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        assertEquals(TypeParser.createFrom("SSL_CTX*", true), declfromMultiplyExpression.type)
        assertEquals("ptr", declfromMultiplyExpression.name)

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
        assertEquals(TypeParser.createFrom("int*", false), c.type)

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
        assertEquals("some text", (qualifiedType.initializer as Literal<*>).value)

        val pointerWithAssign =
            (statements[5] as DeclarationStatement).getSingleDeclarationAs(
                VariableDeclaration::class.java
            )
        assertEquals(TypeParser.createFrom("void*", true), pointerWithAssign.type)
        assertEquals("ptr2", pointerWithAssign.name)
        assertTrue((pointerWithAssign.initializer as? Literal<*>)?.value == null)

        val classWithVariable = statements[6].declarations
        assertEquals(2, classWithVariable.size)

        val classA = classWithVariable[0] as RecordDeclaration
        assertNotNull(classA)
        assertEquals("A", classA.name)

        val field = classA.getField("myField")
        assertNotNull(field)
        assertEquals("myField", field.name)
        assertEquals("int", field.type.typeName)

        val myA = classWithVariable[1] as VariableDeclaration
        assertNotNull(myA)
        assertEquals("myA", myA.name)
        assertEquals(classA, (myA.type as ObjectType).recordDeclaration)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testRecordDeclaration() {
        val file = File("src/test/resources/recordstmt.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        val recordDeclaration = tu.getDeclarationAs(0, RecordDeclaration::class.java)
        assertNotNull(recordDeclaration)
        assertEquals("SomeClass", recordDeclaration.name)
        assertEquals("class", recordDeclaration.kind)
        assertEquals(3, recordDeclaration.fields.size)

        val field = recordDeclaration.getField("field")
        assertNotNull(field)

        val initializer = field.initializer
        assertNotNull(initializer)

        val constant = recordDeclaration.getField("CONSTANT")
        assertNotNull(constant)
        assertEquals(TypeParser.createFrom("void*", true), field.type)
        assertEquals(3, recordDeclaration.methods.size)

        val method = recordDeclaration.methods[0]
        assertEquals("method", method.name)
        assertEquals(0, method.parameters.size)
        assertEquals(TypeParser.createFrom("void*", true), method.type)
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
        assertEquals(TypeParser.createFrom("void*", true), methodWithParam.type)
        assertFalse(methodWithParam.hasBody())
        definition = methodWithParam.definition as MethodDeclaration
        assertNotNull(definition)
        assertEquals("method", definition.name)
        assertEquals(1, definition.parameters.size)
        assertTrue(definition.isDefinition)

        val inlineMethod = recordDeclaration.methods[2]
        assertEquals("inlineMethod", inlineMethod.name)
        assertEquals(TypeParser.createFrom("void*", true), inlineMethod.type)
        assertTrue(inlineMethod.hasBody())

        val inlineConstructor = recordDeclaration.constructors[0]
        assertEquals(recordDeclaration.name, inlineConstructor.name)
        assertEquals(TypeParser.createFrom("SomeClass", true), inlineConstructor.type)
        assertTrue(inlineConstructor.hasBody())

        val constructorDefinition = tu.getDeclarationAs(3, ConstructorDeclaration::class.java)
        assertNotNull(constructorDefinition)
        assertEquals(1, constructorDefinition.parameters.size)
        assertEquals(TypeParser.createFrom("int", true), constructorDefinition.parameters[0].type)
        assertEquals(TypeParser.createFrom("SomeClass", true), constructorDefinition.type)
        assertTrue(constructorDefinition.hasBody())

        val constructorDeclaration = recordDeclaration.constructors[1]
        assertNotNull(constructorDeclaration)
        assertFalse(constructorDeclaration.isDefinition)
        assertEquals(constructorDefinition, constructorDeclaration.definition)

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)
        ""
        val methodCallWithConstant = main.getBodyStatementAs(2, CallExpression::class.java)
        assertNotNull(methodCallWithConstant)

        val arg = methodCallWithConstant.arguments[0]
        assertSame(constant, (arg as DeclaredReferenceExpression).refersTo)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testUnityBuild() {
        val file = File("src/test/resources/unity")
        val builder =
            TranslationConfiguration.builder()
                .sourceLocations(java.util.List.of(file))
                .topLevel(file.parentFile)
                .useUnityBuild(true)
                .loadIncludes(true)
                .defaultPasses()
                .defaultLanguages()

        builder.unregisterLanguage(CXXLanguageFrontend::class.java)
        builder.registerLanguage(
            CXXLanguageFrontend2::class.java,
            CXXLanguageFrontend.CXX_EXTENSIONS
        )

        val declarations = analyzeWithBuilder(builder)
        Assertions.assertEquals(1, declarations.size)

        // should contain 3 declarations (2 include and 1 function decl from the include)
        Assertions.assertEquals(3, declarations[0].declarations.size)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testLocation() {
        val file = File("src/test/resources/components/foreachstmt.cpp")
        val tu =
            analyzeAndGetFirstTU(java.util.List.of(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }
        val main = tu.getDeclarationsByName("main", FunctionDeclaration::class.java)
        Assertions.assertFalse(main.isEmpty())
        val location = main.iterator().next().location
        Assertions.assertNotNull(location)
        val path = Path.of(location!!.artifactLocation.uri)
        Assertions.assertEquals("foreachstmt.cpp", path.fileName.toString())
        Assertions.assertEquals(Region(4, 1, 8, 2), location.region)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testCompoundStatement() {
        val file = File("src/test/resources/compoundstmt.cpp")
        val declaration = analyzeAndGetFirstTU(java.util.List.of(file), file.parentFile.toPath(), true){
            it.unregisterLanguage(CXXLanguageFrontend::class.java)
            it.registerLanguage(
                CXXLanguageFrontend2::class.java,
                CXXLanguageFrontend.CXX_EXTENSIONS
            )
        }
        val function = declaration.getDeclarationAs(
            0,
            FunctionDeclaration::class.java
        )
        Assertions.assertNotNull(function)
        val functionBody = function!!.body
        Assertions.assertNotNull(functionBody)
        val statements = (functionBody as CompoundStatement).statements
        Assertions.assertEquals(1, statements.size)
        val returnStatement = statements[0] as ReturnStatement
        Assertions.assertNotNull(returnStatement)
        val returnValue = returnStatement.returnValue
        Assertions.assertTrue(returnValue is Literal<*>)
        Assertions.assertEquals(1, (returnValue as Literal<*>).value)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testCast() {
        val file = File("src/test/resources/components/castexpr.cpp")
        val tu = analyzeAndGetFirstTU(java.util.List.of(file), file.parentFile.toPath(), true){
            it.unregisterLanguage(CXXLanguageFrontend::class.java)
            it.registerLanguage(
                CXXLanguageFrontend2::class.java,
                CXXLanguageFrontend.CXX_EXTENSIONS
            )
        }
        val main = tu.getDeclarationAs(
            0,
            FunctionDeclaration::class.java
        )
        val e = Objects.requireNonNull(
            main!!.getBodyStatementAs(
                0,
                DeclarationStatement::class.java
            )
        )!!.singleDeclaration as VariableDeclaration
        Assertions.assertNotNull(e)
        Assertions.assertEquals(TypeParser.createFrom("ExtendedClass*", true), e.type)
        val b = Objects.requireNonNull(
            main.getBodyStatementAs(
                1,
                DeclarationStatement::class.java
            )
        )!!.singleDeclaration as VariableDeclaration
        Assertions.assertNotNull(b)
        Assertions.assertEquals(TypeParser.createFrom("BaseClass*", true), b.type)

        // initializer
        var cast = b.initializer as CastExpression
        Assertions.assertNotNull(cast)
        Assertions.assertEquals(TypeParser.createFrom("BaseClass*", true), cast.castType)
        val staticCast = main.getBodyStatementAs(
            2,
            BinaryOperator::class.java
        )
        Assertions.assertNotNull(staticCast)
        cast = staticCast!!.rhs as CastExpression
        Assertions.assertNotNull(cast)
        Assertions.assertEquals("static_cast", cast.name)
        val reinterpretCast = main.getBodyStatementAs(
            3,
            BinaryOperator::class.java
        )
        Assertions.assertNotNull(reinterpretCast)
        cast = reinterpretCast!!.rhs as CastExpression
        Assertions.assertNotNull(cast)
        Assertions.assertEquals("reinterpret_cast", cast.name)
        val d = Objects.requireNonNull(
            main.getBodyStatementAs(
                4,
                DeclarationStatement::class.java
            )
        )!!.singleDeclaration as VariableDeclaration
        Assertions.assertNotNull(d)
        cast = d.initializer as CastExpression
        Assertions.assertNotNull(cast)
        Assertions.assertEquals(TypeParser.createFrom("int", true), cast.castType)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testIf() {
        val file = File("src/test/resources/if.cpp")
        val declaration = analyzeAndGetFirstTU(java.util.List.of(file), file.parentFile.toPath(), true){
            it.unregisterLanguage(CXXLanguageFrontend::class.java)
            it.registerLanguage(
                CXXLanguageFrontend2::class.java,
                CXXLanguageFrontend.CXX_EXTENSIONS
            )
        }
        val statements: List<Statement> = (declaration.getDeclarationAs(0, FunctionDeclaration::class.java)!!.body as CompoundStatement).statements

        val ifStatement = statements[0] as IfStatement
        Assertions.assertNotNull(ifStatement)
        Assertions.assertNotNull(ifStatement.condition)
        Assertions.assertEquals("bool", ifStatement.condition.type.typeName)
        Assertions.assertEquals(true, (ifStatement.condition as Literal<*>).value)
        Assertions.assertTrue(
            (ifStatement.thenStatement as CompoundStatement).statements[0] is ReturnStatement
        )
        Assertions.assertTrue(
            (ifStatement.elseStatement as CompoundStatement).statements[0] is ReturnStatement
        )
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testForEach() {
        val file = File("src/test/resources/components/foreachstmt.cpp")
        val tu =
            analyzeAndGetFirstTU(java.util.List.of(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }
        val main = tu.getDeclarationsByName("main", FunctionDeclaration::class.java)
        Assertions.assertFalse(main.isEmpty())
        val decl = main.iterator().next()
        val ls = decl.getVariableDeclarationByName("ls").orElse(null)
        Assertions.assertNotNull(ls)
        Assertions.assertEquals(TypeParser.createFrom("std::vector<int>", true), ls.type)
        Assertions.assertEquals("ls", ls.name)
        val forEachStatement = decl.getBodyStatementAs(1, ForEachStatement::class.java)
        Assertions.assertNotNull(forEachStatement)

        // should loop over ls
        Assertions.assertEquals(
            ls,
            (forEachStatement!!.iterable as DeclaredReferenceExpression).refersTo
        )

        // should declare auto i (so far no concrete type inferrable)
        val stmt = forEachStatement.variable
        Assertions.assertNotNull(stmt)
        Assertions.assertTrue(stmt is DeclarationStatement)
        Assertions.assertTrue((stmt as DeclarationStatement).isSingleDeclaration)
        val i = stmt.singleDeclaration as VariableDeclaration
        Assertions.assertNotNull(i)
        Assertions.assertEquals("i", i.name)
        Assertions.assertEquals(UnknownType.getUnknownType(), i.type)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testAssignmentExpression() {
        val file = File("src/test/resources/assignmentexpression.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.unregisterLanguage(CXXLanguageFrontend::class.java)
                it.registerLanguage(
                    CXXLanguageFrontend2::class.java,
                    CXXLanguageFrontend.CXX_EXTENSIONS
                )
            }

        val statements =
            (tu.byNameOrNull<FunctionDeclaration>("main")?.body as? CompoundStatement)?.statements
                ?: listOf()

        val declareA = statements[0]
        val a = (declareA as DeclarationStatement).singleDeclaration
        val assignA = statements[1]
        assertTrue(assignA is BinaryOperator)

        var lhs = assignA.lhs
        var rhs = assignA.rhs
        assertEquals("a", lhs.name)
        assertEquals(2, (rhs as Literal<*>).value)
        assertRefersTo(lhs, a)

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
}
