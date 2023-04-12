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
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.assertFullName
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import java.nio.file.Path
import kotlin.test.*

class GoLanguageFrontendTest : BaseTest() {

    @Test
    fun testArrayCompositeLiteral() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("values.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["p"]
        assertNotNull(p)

        val main = p.functions["main"]
        assertNotNull(main)

        val message = main.variables["message"]
        assertNotNull(message)

        val map =
            assertIs<InitializerListExpression>(
                assertIs<ConstructExpression>(message.firstAssignment).arguments.firstOrNull()
            )
        assertNotNull(map)

        val nameEntry = map.initializers.firstOrNull() as? KeyValueExpression
        assertNotNull(nameEntry)

        assertLocalName("string[]", (nameEntry.value as? ConstructExpression)?.type)
    }

    @Test
    fun testDFG() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("dfg.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["p"]
        assertNotNull(p)

        val main = p.functions["main"]
        assertNotNull(main)

        val data = main.variables["data"]
        assertNotNull(data)

        // We should be able to follow the DFG backwards from the declaration to the individual
        // key/value expressions
        val path = data.firstAssignment?.followPrevDFG { it is KeyValueExpression }

        assertNotNull(path)
        assertEquals(3, path.size)
    }

    @Test
    fun testConstruct() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("construct.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["p"]
        assertNotNull(p)

        val myStruct = p.records["p.MyStruct"]
        assertNotNull(myStruct)

        val main = p.functions["main"]
        assertNotNull(main)

        val body = main.body as? CompoundStatement
        assertNotNull(body)

        var decl = main.variables["o"]
        assertNotNull(decl)

        val new = assertIs<NewExpression>(decl.firstAssignment)
        assertEquals(TypeParser.createFrom("p.MyStruct*", GoLanguage()), new.type)

        val construct = new.initializer as? ConstructExpression
        assertNotNull(construct)
        assertEquals(myStruct, construct.instantiates)

        // make array

        decl = main.variables["a"]
        assertNotNull(decl)

        var make = assertIs<Expression>(decl.firstAssignment)
        assertNotNull(make)
        assertEquals(TypeParser.createFrom("int[]", GoLanguage()), make.type)

        assertTrue(make is ArrayCreationExpression)

        val dimension = make.dimensions.first() as? Literal<*>
        assertNotNull(dimension)
        assertEquals(5, dimension.value)

        // make map

        decl = main.variables["m"]
        assertNotNull(decl)

        make = assertIs(decl.firstAssignment)
        assertNotNull(make)
        assertTrue(make is ConstructExpression)
        // TODO: Maps can have dedicated types and parsing them as a generic here is only a
        //  temporary solution. This should be fixed in the future.
        assertEquals(TypeParser.createFrom("map[string,string]", GoLanguage()), make.type)

        // make channel

        decl = main.variables["ch"]
        assertNotNull(decl)

        make = assertIs(decl.firstAssignment)
        assertNotNull(make)
        assertTrue(make is ConstructExpression)
        assertEquals(TypeParser.createFrom("chan[int]", GoLanguage()), make.type)
    }

    @Test
    fun testLiteral() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("literal.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }

        assertNotNull(tu)

        val p = tu.namespaces["p"]
        assertNotNull(p)

        val a = p.variables["a"]
        assertNotNull(a)
        assertNotNull(a.location)

        assertLocalName("a", a)
        assertEquals(TypeParser.createFrom("int", GoLanguage()), a.type)

        val s = p.variables["s"]
        assertNotNull(s)
        assertLocalName("s", s)
        assertEquals(TypeParser.createFrom("string", GoLanguage()), s.type)

        val f = p.variables["f"]
        assertNotNull(f)
        assertLocalName("f", f)
        assertEquals(TypeParser.createFrom("float64", GoLanguage()), f.type)

        val f32 = p.variables["f32"]
        assertNotNull(f32)
        assertLocalName("f32", f32)
        assertEquals(TypeParser.createFrom("float32", GoLanguage()), f32.type)

        val n = p.variables["n"]
        assertNotNull(n)
        assertEquals(TypeParser.createFrom("int*", GoLanguage()), n.type)

        val nil = n.initializer as? Literal<*>
        assertNotNull(nil)
        assertLocalName("nil", nil)
        assertEquals(null, nil.value)

        val fn = p.variables["fn"]
        assertNotNull(fn)

        val lambda = assertIs<LambdaExpression>(fn.initializer)
        assertNotNull(lambda)

        val func = lambda.function
        assertNotNull(func)
        assertFullName("", func)
        assertEquals(1, func.parameters.size)
        assertEquals(1, func.returnTypes.size)
    }

    @Test
    fun testFunctionDeclaration() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("function.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }

        assertNotNull(tu)

        val p = tu.namespaces["p"]
        assertNotNull(p)

        val main = p.functions["main"]
        assertNotNull(main)

        var type = main.type as? FunctionType
        assertNotNull(type)
        assertLocalName("func()", type)
        assertEquals(0, type.parameters.size)
        assertEquals(0, type.returnTypes.size)

        val myTest = p.functions["myTest"]
        assertNotNull(myTest)
        assertEquals(1, myTest.parameters.size)
        assertEquals(2, myTest.returnTypes.size)

        type = myTest.type as? FunctionType
        assertNotNull(type)
        assertLocalName("func(string) (int, error)", type)
        assertEquals(myTest.parameters.size, type.parameters.size)
        assertEquals(myTest.returnTypes.size, type.returnTypes.size)
        assertEquals(listOf("int", "error"), type.returnTypes.map { it.name.localName })

        var body = main.body as? CompoundStatement
        assertNotNull(body)

        var callExpression = body.calls.firstOrNull()
        assertNotNull(callExpression)

        assertLocalName("myTest", callExpression)
        assertEquals(myTest, callExpression.invokes.iterator().next())

        val s = myTest.parameters.first()
        assertNotNull(s)
        assertLocalName("s", s)
        assertEquals(TypeParser.createFrom("string", GoLanguage()), s.type)

        assertLocalName("myTest", myTest)

        body = myTest.body as? CompoundStatement
        assertNotNull(body)

        callExpression = body.statements.first() as? CallExpression
        assertNotNull(callExpression)

        assertFullName("fmt.Printf", callExpression)
        assertLocalName("Printf", callExpression)

        val literal = callExpression.arguments.first() as? Literal<*>
        assertNotNull(literal)

        assertEquals("%s", literal.value)
        assertEquals(TypeParser.createFrom("string", GoLanguage()), literal.type)

        val ref = callExpression.arguments[1] as? DeclaredReferenceExpression
        assertNotNull(ref)

        assertLocalName("s", ref)
        assertEquals(s, ref.refersTo)

        val stmt = body.statements[1] as? AssignExpression
        assertNotNull(stmt)

        val a = stmt.lhs.firstOrNull() as? DeclaredReferenceExpression
        assertNotNull(a)

        assertLocalName("a", a)

        val op = assertIs<BinaryOperator>(stmt.rhs.firstOrNull())
        assertEquals("+", op.operatorCode)

        val lhs = op.lhs as? Literal<*>
        assertNotNull(lhs)

        assertEquals(1, lhs.value)

        val rhs = op.rhs as? Literal<*>
        assertNotNull(rhs)

        assertEquals(2, rhs.value)

        val binOp = assertIs<AssignExpression>(body.statements[2])
        val err = binOp.lhs.firstOrNull()

        assertNotNull(err)
        assertLocalName("error", err.type)
    }

    @Test
    fun testStruct() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("struct.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
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

        assertLocalName("MyFunc", myFunc)

        val myField = fields.first()

        assertLocalName("MyField", myField)
        assertEquals(TypeParser.createFrom("int", GoLanguage()), myField.type)

        val myInterface =
            p.getDeclarationsByName("p.MyInterface", RecordDeclaration::class.java)
                .iterator()
                .next()

        assertNotNull(myInterface)
        assertEquals("interface", myInterface.kind)

        methods = myInterface.methods

        assertEquals(1, methods.size)

        myFunc = methods.first()

        assertLocalName("MyFunc", myFunc)
        assertLocalName("func() string", myFunc.type)

        val newMyStruct = p.functions["NewMyStruct"]
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
            analyzeAndGetFirstTU(listOf(topLevel.resolve("struct.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }

        assertNotNull(tu)

        val p = tu.namespaces["p"]
        assertNotNull(p)

        val myStruct = p.records["MyStruct"]
        assertNotNull(myStruct)

        val methods = myStruct.methods
        val myFunc = methods.firstOrNull()
        assertNotNull(myFunc)
        assertLocalName("MyFunc", myFunc)

        val body = myFunc.body as? CompoundStatement

        assertNotNull(body)

        val printf = body.statements.first() as? CallExpression

        assertNotNull(printf)
        assertLocalName("Printf", printf)
        assertFullName("fmt.Printf", printf)

        val arg1 = printf.arguments[0] as? MemberCallExpression

        assertNotNull(arg1)
        assertLocalName("myOtherFunc", arg1)
        assertFullName("p.MyStruct.myOtherFunc", arg1)

        assertEquals(myFunc.receiver, (arg1.base as? DeclaredReferenceExpression)?.refersTo)
    }

    @Test
    fun testField() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("field.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }

        assertNotNull(tu)

        val p = tu.namespaces["p"]
        assertNotNull(p)

        val myFunc = p.methods["myFunc"]
        assertNotNull(myFunc)

        val body = myFunc.body as? CompoundStatement
        assertNotNull(body)

        val assign = body.statements.first() as? AssignExpression
        assertNotNull(assign)

        val lhs = assign.lhs.firstOrNull() as? MemberExpression
        assertNotNull(lhs)
        assertEquals(myFunc.receiver, (lhs.base as? DeclaredReferenceExpression)?.refersTo)
        assertLocalName("Field", lhs)
        assertEquals(TypeParser.createFrom("int", GoLanguage()), lhs.type)

        val rhs = assign.rhs.firstOrNull() as? DeclaredReferenceExpression
        assertNotNull(rhs)
        assertFullName("otherPackage.OtherField", rhs)
    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("if.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val main = tu.functions["p.main"]
        assertNotNull(main)

        val body = main.body as? CompoundStatement
        assertNotNull(body)

        val b =
            (body.statements.first() as? DeclarationStatement)?.singleDeclaration
                as? VariableDeclaration

        assertNotNull(b)
        assertLocalName("b", b)
        assertEquals(TypeParser.createFrom("bool", GoLanguage()), b.type)

        // true, false are builtin variables, NOT literals in Golang
        // we might need to parse this special case differently
        val initializer = b.initializer as? DeclaredReferenceExpression

        assertNotNull(initializer)
        assertLocalName("true", initializer)

        val `if` = body.statements[1] as? IfStatement

        assertNotNull(`if`)
    }

    @Test
    fun testSwitch() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("switch.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }

        assertNotNull(tu)

        val myFunc = tu.functions["p.myFunc"]
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
        assertLocalName("first", first)

        val case2 = list.statements[2] as? CaseStatement

        assertNotNull(case2)
        assertEquals(2, (case2.caseExpression as? Literal<*>)?.value)

        val second = list.statements[3] as? CallExpression

        assertNotNull(second)
        assertLocalName("second", second)

        val case3 = list.statements[4] as? CaseStatement

        assertNotNull(case3)
        assertEquals(3, (case3.caseExpression as? Literal<*>)?.value)

        val third = list.statements[5] as? CallExpression

        assertNotNull(third)
        assertLocalName("third", third)
    }

    @Test
    fun testMemberCall() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("call.go").toFile(),
                    topLevel.resolve("struct.go").toFile()
                ),
                topLevel,
                true
            ) {
                it.registerLanguage<GoLanguage>()
            }

        assertNotNull(result)
        val tus = result.translationUnits
        val tu = tus[0]

        val p = tu.namespaces["p"]
        assertNotNull(p)

        val main = p.functions["main"]
        assertNotNull(main)

        val body = main.body as? CompoundStatement
        assertNotNull(body)

        val c = body.variables["c"]

        assertNotNull(c)
        // type will be inferred from the function declaration
        assertEquals(TypeParser.createFrom("p.MyStruct*", GoLanguage()), c.type)

        val newMyStruct = assertIs<CallExpression>(c.firstAssignment)

        // fetch the function declaration from the other TU
        val tu2 = tus[1]

        val p2 = tu2.namespaces["p"]
        assertNotNull(p2)

        val newMyStructDef = p2.functions["NewMyStruct"]
        assertTrue(newMyStruct.invokes.contains(newMyStructDef))

        val call = body.statements[1] as? MemberCallExpression
        assertNotNull(call)

        val base = call.base as? DeclaredReferenceExpression
        assertNotNull(base)
        assertEquals(c, base.refersTo)

        val go = main.calls["go"]
        assertNotNull(go)
    }

    @Test
    fun testFor() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(
                listOf(
                    topLevel.resolve("for.go").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage<GoLanguage>()
            }

        val main = tu.functions["main.main"]
        assertNotNull(main)

        val f = main.bodyOrNull<ForStatement>()
        assertNotNull(f)
        assertTrue(f.condition is BinaryOperator)
        assertTrue(f.statement is CompoundStatement)
        assertTrue(f.initializerStatement is AssignExpression)
        assertTrue(f.iterationStatement is UnaryOperator)

        val each = main.bodyOrNull<ForEachStatement>()
        assertNotNull(each)

        val bytes = assertIs<DeclaredReferenceExpression>(each.iterable)
        assertLocalName("bytes", bytes)
        assertNotNull(bytes.refersTo)

        val idx = assertIs<DeclarationStatement>(each.variable).variables["idx"]
        assertNotNull(idx)
        assertLocalName("int", idx.type)

        val b = assertIs<DeclarationStatement>(each.variable).variables["b"]
        assertNotNull(b)
        assertLocalName("uint8", b.type)
    }

    @Test
    fun testModules() {
        val topLevel = Path.of("src", "test", "resources", "golang-modules")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("awesome.go").toFile(),
                    topLevel.resolve("cmd/awesome/main.go").toFile(),
                    topLevel.resolve("util/stuff.go").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage<GoLanguage>()
            }

        assertNotNull(result)
        val tus = result.translationUnits

        val tu0 = tus[0]
        assertNotNull(tu0)

        val newAwesome = tu0.functions["awesome.NewAwesome"]
        assertNotNull(newAwesome)

        val tu1 = tus[1]
        assertNotNull(tu1)

        val main = tu1.functions["main.main"]
        assertNotNull(main)

        val a = main.variables["a"]
        assertNotNull(a)

        val call = a.firstAssignment as? CallExpression
        assertNotNull(call)
        assertTrue(call.invokes.contains(newAwesome))

        val util = result.namespaces["util"]
        assertNotNull(util)

        // Check, if we correctly inferred this function in the namespace
        val doSomethingElse = util.functions["DoSomethingElse"]
        assertNotNull(doSomethingElse)
        assertTrue(doSomethingElse.isInferred)
        assertSame(util, doSomethingElse.scope?.astNode)
    }

    @Test
    fun testComments() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("comment.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }

        assertNotNull(tu)

        val mainNamespace = tu.namespaces["main"]
        assertNotNull(mainNamespace)

        val main = mainNamespace.functions["main"]
        assertNotNull(main)
        assertEquals("comment before function", main.comment)

        val i = main.parameters.firstOrNull { it.name.localName == "i" }
        assertNotNull(i)
        assertEquals("comment before parameter1", i.comment)

        val j = main.parameters.firstOrNull { it.name.localName == "j" }
        assertNotNull(j)
        assertEquals("comment before parameter2", j.comment)

        val assign = main.bodyOrNull<AssignExpression>()
        assertNotNull(assign)
        assertEquals("comment before assignment", assign.comment)

        val declStmt = main.bodyOrNull<DeclarationStatement>()
        assertNotNull(declStmt)
        assertEquals("comment before declaration", declStmt.comment)

        val s = mainNamespace.byNameOrNull<RecordDeclaration>("main.s")
        assertNotNull(s)
        assertEquals("comment before struct", s.comment)

        val myField = s.fields["myField"]
        assertNotNull(myField)
        assertNotNull("comment before field", myField.comment)
    }

    @Test
    fun testRef() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("ref.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }

        val mainPackage = tu.byNameOrNull<NamespaceDeclaration>("main")
        assertNotNull(mainPackage)

        val main = mainPackage.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val assign = main.bodyOrNull<AssignExpression>()
        assertNotNull(assign)
        assertEquals(1, assign.rhs.size)

        assertNotNull(tu)
    }

    @Test
    fun testAssign() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("function.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val i = tu.variables["i"]

        val assign =
            tu.functions["main"].assignments.firstOrNull {
                (it.target as? DeclaredReferenceExpression)?.refersTo == i
            }
        assertNotNull(assign)

        val call = assertIs<CallExpression>(assign.value)
        assertLocalName("myTest", call)
    }
}
