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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.ExperimentalGolang
import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.body
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalGolang
class GoLanguageFrontendTest : BaseTest() {

    @Test
    fun testConstruct() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("construct.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    GoLanguageFrontend::class.java,
                    GoLanguageFrontend.GOLANG_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p = tu.byNameOrNull<NamespaceDeclaration>("p")
        assertNotNull(p)

        val myStruct = p.byNameOrNull<RecordDeclaration>("p.MyStruct")
        assertNotNull(myStruct)

        val main = p.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val body = main.body as? CompoundStatement
        assertNotNull(body)

        var stmt = main.body<DeclarationStatement>(0)
        assertNotNull(stmt)

        var decl = stmt.singleDeclaration as? VariableDeclaration
        assertNotNull(decl)

        val new = decl.initializer as? NewExpression
        assertNotNull(new)
        assertEquals(TypeParser.createFrom("p.MyStruct*", false), new.type)

        val construct = new.initializer as? ConstructExpression
        assertNotNull(construct)
        assertEquals(myStruct, construct.instantiates)

        // make array

        stmt = main.body(1)
        assertNotNull(stmt)

        decl = stmt.singleDeclaration as? VariableDeclaration
        assertNotNull(decl)

        var make = decl.initializer
        assertNotNull(make)
        assertEquals(TypeParser.createFrom("int[]", false), make.type)

        assertTrue(make is ArrayCreationExpression)

        val dimension = make.dimensions.first() as? Literal<*>
        assertNotNull(dimension)
        assertEquals(5, dimension.value)

        // make map

        stmt = main.body(2)
        assertNotNull(stmt)

        decl = stmt.singleDeclaration as? VariableDeclaration
        assertNotNull(decl)

        make = decl.initializer
        assertNotNull(make)
        assertTrue(make is ConstructExpression)
        assertEquals(TypeParser.createFrom("map<string,string>", false), make.type)

        // make channel

        stmt = main.body(3)
        assertNotNull(stmt)

        decl = stmt.singleDeclaration as? VariableDeclaration
        assertNotNull(decl)

        make = decl.initializer
        assertNotNull(make)
        assertTrue(make is ConstructExpression)
        assertEquals(TypeParser.createFrom("chan<int>", false), make.type)
    }

    @Test
    fun testLiteral() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("literal.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    GoLanguageFrontend::class.java,
                    GoLanguageFrontend.GOLANG_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p = tu.byNameOrNull<NamespaceDeclaration>("p")
        assertNotNull(p)

        val a = p.byNameOrNull<VariableDeclaration>("a")
        assertNotNull(a)
        assertNotNull(a.location)

        assertEquals("a", a.name)
        assertEquals(TypeParser.createFrom("int", false), a.type)

        val s = p.byNameOrNull<VariableDeclaration>("s")
        assertNotNull(s)
        assertEquals("s", s.name)
        assertEquals(TypeParser.createFrom("string", false), s.type)

        val f = p.byNameOrNull<VariableDeclaration>("f")
        assertNotNull(f)
        assertEquals("f", f.name)
        assertEquals(TypeParser.createFrom("float64", false), f.type)

        val f32 = p.byNameOrNull<VariableDeclaration>("f32")
        assertNotNull(f32)
        assertEquals("f32", f32.name)
        assertEquals(TypeParser.createFrom("float32", false), f32.type)
    }

    @Test
    fun testFunctionDeclaration() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("function.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    GoLanguageFrontend::class.java,
                    GoLanguageFrontend.GOLANG_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        val main = p.declarations.first() as? FunctionDeclaration
        assertNotNull(main)

        val myTest = p.declarations[1] as? FunctionDeclaration
        assertNotNull(myTest)
        assertEquals(1, myTest.parameters.size)

        var body = main.body as? CompoundStatement
        assertNotNull(body)

        var callExpression = body.statements.first() as? CallExpression
        assertNotNull(callExpression)

        assertEquals("myTest", callExpression.name)
        assertEquals(myTest, callExpression.invokes.iterator().next())

        val s = myTest.parameters.first()
        assertNotNull(s)
        assertEquals("s", s.name)
        assertEquals(TypeParser.createFrom("string", false), s.type)

        assertEquals("myTest", myTest.name)

        body = myTest.body as? CompoundStatement
        assertNotNull(body)

        callExpression = body.statements.first() as? CallExpression
        assertNotNull(callExpression)

        assertEquals("fmt.Printf", callExpression.fqn)
        assertEquals("Printf", callExpression.name)

        val literal = callExpression.arguments.first() as? Literal<*>
        assertNotNull(literal)

        assertEquals("%s", literal.value)
        assertEquals(TypeParser.createFrom("string", false), literal.type)

        val ref = callExpression.arguments[1] as? DeclaredReferenceExpression
        assertNotNull(ref)

        assertEquals("s", ref.name)
        assertEquals(s, ref.refersTo)

        val stmt = body.statements[1] as? DeclarationStatement
        assertNotNull(stmt)

        val a = stmt.singleDeclaration as? VariableDeclaration
        assertNotNull(a)

        assertEquals("a", a.name)

        val op = a.initializer as? BinaryOperator
        assertNotNull(op)

        assertEquals("+", op.operatorCode)

        val lhs = op.lhs as? Literal<*>
        assertNotNull(lhs)

        assertEquals(1, lhs.value)

        val rhs = op.rhs as? Literal<*>
        assertNotNull(rhs)

        assertEquals(2, rhs.value)

        val binOp = body.statements[2] as? BinaryOperator

        assertNotNull(binOp)

        val err = binOp.lhs

        assertNotNull(err)
        assertEquals(TypeParser.createFrom("error", false), err.type)
    }

    @Test
    fun testStruct() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("struct.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    GoLanguageFrontend::class.java,
                    GoLanguageFrontend.GOLANG_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()

        val myStruct =
            p.getDeclarationsByName("p.MyStruct", RecordDeclaration::class.java).iterator().next()

        assertNotNull(myStruct)
        assertEquals("struct", myStruct.kind)

        val fields = myStruct.fields

        assertEquals(1, fields.size)

        var methods = myStruct.methods

        var myFunc = methods.first()

        assertEquals("MyFunc", myFunc.name)

        val myField = fields.first()

        assertEquals("MyField", myField.name)
        assertEquals(TypeParser.createFrom("int", false), myField.type)

        val myInterface =
            p.getDeclarationsByName("p.MyInterface", RecordDeclaration::class.java)
                .iterator()
                .next()

        assertNotNull(myInterface)
        assertEquals("interface", myInterface.kind)

        methods = myInterface.methods

        assertEquals(1, methods.size)

        myFunc = methods.first()

        assertEquals("MyFunc", myFunc.name)
        assertEquals(TypeParser.createFrom("string", false), myFunc.type)

        val newMyStruct =
            p.getDeclarationsByName("NewMyStruct", FunctionDeclaration::class.java)
                .iterator()
                .next()

        assertNotNull(newMyStruct)

        val body = newMyStruct.body as? CompoundStatement

        assertNotNull(body)

        val `return` = body.statements.first() as? ReturnStatement

        assertNotNull(`return`)

        val returnValue = `return`.returnValue as? UnaryOperator

        assertNotNull(returnValue)
    }

    @Test
    fun testMemberCalls() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("struct.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    GoLanguageFrontend::class.java,
                    GoLanguageFrontend.GOLANG_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()

        val myStruct =
            p.getDeclarationsByName("p.MyStruct", RecordDeclaration::class.java).iterator().next()

        val methods = myStruct.methods

        val myFunc = methods.first()

        assertEquals("MyFunc", myFunc.name)

        val body = myFunc.body as? CompoundStatement

        assertNotNull(body)

        val printf = body.statements.first() as? CallExpression

        assertNotNull(printf)
        assertEquals("Printf", printf.name)
        assertEquals("fmt.Printf", printf.fqn)

        val arg1 = printf.arguments[0] as? MemberCallExpression

        assertNotNull(arg1)
        assertEquals("myOtherFunc", arg1.name)
        assertEquals("s.myOtherFunc", arg1.fqn)

        assertEquals(myFunc.receiver, (arg1.base as? DeclaredReferenceExpression)?.refersTo)
    }

    @Test
    fun testField() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("field.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    GoLanguageFrontend::class.java,
                    GoLanguageFrontend.GOLANG_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        val myFunc =
            p.getDeclarationsByName("myFunc", MethodDeclaration::class.java).iterator().next()

        val body = myFunc.body as? CompoundStatement

        assertNotNull(body)

        val binOp = body.statements.first() as? BinaryOperator

        assertNotNull(binOp)

        val lhs = binOp.lhs as? MemberExpression

        assertNotNull(lhs)
        assertEquals(myFunc.receiver, (lhs.base as? DeclaredReferenceExpression)?.refersTo)
        assertEquals("Field", lhs.name)
        assertEquals(TypeParser.createFrom("int", false), lhs.type)

        val rhs = binOp.rhs as? DeclaredReferenceExpression

        assertNotNull(rhs)
        assertEquals("otherPackage.OtherField", rhs.name)
    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("if.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    GoLanguageFrontend::class.java,
                    GoLanguageFrontend.GOLANG_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()

        val main =
            p.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(main)

        val body = main.body as? CompoundStatement

        assertNotNull(body)

        val b =
            (body.statements.first() as? DeclarationStatement)?.singleDeclaration as?
                VariableDeclaration

        assertNotNull(b)
        assertEquals("b", b.name)
        assertEquals(TypeParser.createFrom("bool", false), b.type)

        // true, false are builtin variables, NOT literals in Golang
        // we might need to parse this special case differently
        val initializer = b.initializer as? DeclaredReferenceExpression

        assertNotNull(initializer)
        assertEquals("true", initializer.name)

        val `if` = body.statements[1] as? IfStatement

        assertNotNull(`if`)
    }

    @Test
    fun testSwitch() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("switch.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    GoLanguageFrontend::class.java,
                    GoLanguageFrontend.GOLANG_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()

        val myFunc =
            p.getDeclarationsByName("myFunc", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(myFunc)

        val body = myFunc.body as? CompoundStatement

        assertNotNull(body)

        val switch = body.statements.first() as? SwitchStatement

        assertNotNull(switch)

        val list = switch.statement as? CompoundStatement

        assertNotNull(list)

        val case1 = list.statements[0] as? CaseStatement

        assertNotNull(case1)
        assertEquals(1, (case1.caseExpression as? Literal<*>)?.value)

        val first = list.statements[1] as? CallExpression

        assertNotNull(first)
        assertEquals("first", first.name)

        val case2 = list.statements[2] as? CaseStatement

        assertNotNull(case2)
        assertEquals(2, (case2.caseExpression as? Literal<*>)?.value)

        val second = list.statements[3] as? CallExpression

        assertNotNull(second)
        assertEquals("second", second.name)

        val case3 = list.statements[4] as? CaseStatement

        assertNotNull(case3)
        assertEquals(3, (case3.caseExpression as? Literal<*>)?.value)

        val third = list.statements[5] as? CallExpression

        assertNotNull(third)
        assertEquals("third", third.name)
    }

    @Test
    fun testMemberCall() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tus =
            TestUtils.analyze(
                listOf(
                    topLevel.resolve("call.go").toFile(),
                    topLevel.resolve("struct.go").toFile()
                ),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    GoLanguageFrontend::class.java,
                    GoLanguageFrontend.GOLANG_EXTENSIONS
                )
            }

        assertNotNull(tus)

        val tu = tus.first()

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()

        val main =
            p.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(main)

        val body = main.body as? CompoundStatement

        assertNotNull(body)

        val c =
            (body.statements[0] as? DeclarationStatement)?.singleDeclaration as? VariableDeclaration

        assertNotNull(c)
        // type will be inferred from the function declaration
        assertEquals(TypeParser.createFrom("p.MyStruct*", false), c.type)

        val newMyStruct = c.initializer as? CallExpression

        // fetch the function declaration from the other TU
        val tu2 = tus[1]

        val p2 = tu2.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()
        val newMyStructDef =
            p2.getDeclarationsByName("NewMyStruct", FunctionDeclaration::class.java)
                .iterator()
                .next()

        assertNotNull(newMyStruct)
        assertTrue(newMyStruct.invokes.contains(newMyStructDef))

        val call = body.statements[1] as? MemberCallExpression

        assertNotNull(call)

        val base = call.base as? DeclaredReferenceExpression

        assertNotNull(base)
        assertEquals(c, base.refersTo)
    }

    @Test
    fun testFor() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tus =
            TestUtils.analyze(
                listOf(
                    topLevel.resolve("for.go").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    GoLanguageFrontend::class.java,
                    GoLanguageFrontend.GOLANG_EXTENSIONS
                )
            }

        assertNotNull(tus)

        val tu = tus.first()

        val p = tu.getDeclarationsByName("p", NamespaceDeclaration::class.java).iterator().next()

        val main =
            p.getDeclarationsByName("main", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(main)

        val f = main.getBodyStatementAs(0, ForStatement::class.java)

        assertNotNull(f)
        assertTrue(f.condition is BinaryOperator)
        assertTrue(f.statement is CompoundStatement)
        assertTrue(f.initializerStatement is DeclarationStatement)
        assertTrue(f.iterationStatement is UnaryOperator)
    }

    @Test
    fun testModules() {
        val topLevel = Path.of("src", "test", "resources", "golang-modules")
        val tus =
            TestUtils.analyze(
                listOf(
                    topLevel.resolve("awesome.go").toFile(),
                    topLevel.resolve("cmd/awesome/main.go").toFile(),
                    topLevel.resolve("util/stuff.go").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    GoLanguageFrontend::class.java,
                    GoLanguageFrontend.GOLANG_EXTENSIONS
                )
            }

        assertNotNull(tus)

        val tu0 = tus[0]
        assertNotNull(tu0)

        val awesome =
            tu0.getDeclarationsByName("awesome", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(awesome)

        val newAwesome =
            awesome
                .getDeclarationsByName("NewAwesome", FunctionDeclaration::class.java)
                .iterator()
                .next()
        assertNotNull(newAwesome)

        val tu1 = tus[1]
        assertNotNull(tu1)

        val mainNamespace =
            tu1.getDeclarationsByName("main", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(mainNamespace)

        val main =
            mainNamespace
                .getDeclarationsByName("main", FunctionDeclaration::class.java)
                .iterator()
                .next()
        assertNotNull(main)

        val a = main.getBodyStatementAs(0, DeclarationStatement::class.java)
        assertNotNull(a)

        val call = (a.singleDeclaration as? VariableDeclaration)?.initializer as? CallExpression
        assertNotNull(call)
        assertTrue(call.invokes.contains(newAwesome))
    }

    @Test
    fun testComments() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("comment.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    GoLanguageFrontend::class.java,
                    GoLanguageFrontend.GOLANG_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val mainNamespace = tu.byNameOrNull<NamespaceDeclaration>("main")
        assertNotNull(mainNamespace)

        val main = mainNamespace.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)
        assertEquals("comment before function", main.comment)

        val i = main.parameters.firstOrNull { it.name == "i" }
        assertNotNull(i)
        assertEquals("comment before parameter1", i.comment)

        val j = main.parameters.firstOrNull { it.name == "j" }
        assertNotNull(j)
        assertEquals("comment before parameter2", j.comment)

        var declStmt = main.bodyOrNull<DeclarationStatement>()
        assertNotNull(declStmt)
        assertEquals("comment before assignment", declStmt.comment)

        declStmt = main.bodyOrNull(1)
        assertNotNull(declStmt)
        assertEquals("comment before declaration", declStmt.comment)

        val s = mainNamespace.byNameOrNull<RecordDeclaration>("main.s")
        assertNotNull(s)
        assertEquals("comment before struct", s.comment)

        val myField = s.getField("myField")
        assertNotNull(myField)
        assertNotNull("comment before field", myField.comment)
    }

    @Test
    fun testRef() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ref.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    GoLanguageFrontend::class.java,
                    GoLanguageFrontend.GOLANG_EXTENSIONS
                )
            }

        val mainPackage = tu.byNameOrNull<NamespaceDeclaration>("main")
        assertNotNull(mainPackage)

        val main = mainPackage.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val binOp = main.bodyOrNull<BinaryOperator>()
        assertNotNull(binOp)

        assertNotNull(tu)
    }
}
