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

import de.fraunhofer.aisec.cpg.evaluation.MultiValueEvaluator
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
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

        val map = assertIs<InitializerListExpression>(message.firstAssignment)
        assertNotNull(map)

        val nameEntry = map.initializers.firstOrNull() as? KeyValueExpression
        assertNotNull(nameEntry)

        assertLocalName("string[]", (nameEntry.value as? InitializerListExpression)?.type)
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
        assertEquals(3, path.nodes.size)
    }

    @Test
    fun testConstruct() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val result =
            analyze(listOf(topLevel.resolve("construct.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(result)

        with(result) {
            val p = namespaces["p"]
            assertNotNull(p)

            val myStruct = p.records["p.MyStruct"]
            assertNotNull(myStruct)

            val main = p.functions["main"]
            assertNotNull(main)

            val body = main.body as? Block
            assertNotNull(body)

            var decl = main.variables["o"]
            assertNotNull(decl)

            val new = assertIs<NewExpression>(decl.firstAssignment)
            with(result) { assertEquals(assertResolvedType("p.MyStruct").pointer(), new.type) }

            val construct = new.initializer as? ConstructExpression
            assertNotNull(construct)
            assertEquals(myStruct, construct.instantiates)

            // make array

            decl = main.variables["a"]
            assertNotNull(decl)

            var make = assertIs<Expression>(decl.firstAssignment)
            assertNotNull(make)
            assertEquals(primitiveType("int").array(), make.type)

            assertTrue(make is NewArrayExpression)

            val dimension = make.dimensions.firstOrNull() as? Literal<*>
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
            assertEquals(
                objectType("map", listOf(primitiveType("string"), primitiveType("string"))).also {
                    it.scope = finalCtx.scopeManager.globalScope
                },
                make.type,
            )

            // make channel

            decl = main.variables["ch"]
            assertNotNull(decl)

            make = assertIs(decl.firstAssignment)
            assertNotNull(make)
            assertTrue(make is ConstructExpression)
            assertEquals(
                objectType("chan", listOf(primitiveType("int"))).also {
                    it.scope = finalCtx.scopeManager.globalScope
                },
                make.type,
            )
        }
    }

    @Test
    fun testLiteral() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("literal.go").toFile(),
                    topLevel.resolve("submodule/const.go").toFile(),
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(result)

        val tu = result.components["application"]?.translationUnits?.firstOrNull()
        assertNotNull(tu)

        val p = tu.namespaces["p"]
        assertNotNull(p)

        val a = p.variables["a"]
        assertNotNull(a)
        assertNotNull(a.location)

        assertLocalName("a", a)
        assertEquals(tu.primitiveType("int"), a.type)

        val s = p.variables["s"]
        assertNotNull(s)
        assertLocalName("s", s)
        assertEquals(tu.primitiveType("string"), s.type)

        val f = p.variables["f"]
        assertNotNull(f)
        assertLocalName("f", f)
        assertEquals(tu.primitiveType("float64"), f.type)

        val f32 = p.variables["f32"]
        assertNotNull(f32)
        assertLocalName("f32", f32)
        assertEquals(tu.primitiveType("float32"), f32.type)

        val n = p.variables["n"]
        assertNotNull(n)
        with(tu) { assertEquals(tu.primitiveType("int").pointer(), n.type) }

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

        val o = p.variables["o"]
        assertNotNull(o)
        assertLocalName("MyStruct[]", o.type)

        val myStruct = tu.records["MyStruct"]
        assertNotNull(myStruct)

        val field = myStruct.fields["Field"]
        assertNotNull(field)

        var composite =
            (o.initializer as? InitializerListExpression)?.initializers<InitializerListExpression>(
                0
            )
        assertNotNull(composite)
        assertIs<InitializerListExpression>(composite)
        assertIs<ObjectType>(composite.type)
        assertLocalName("MyStruct", composite.type)

        var keyValue = composite.initializers<KeyValueExpression>(0)
        assertNotNull(keyValue)
        assertLocalName("Field", keyValue.key)
        assertRefersTo(keyValue.key, field)
        assertLiteralValue(10, keyValue.value)

        val o3 = p.variables["o3"]
        assertNotNull(o3)
        assertLocalName("MyStruct[]", o.type)

        composite =
            (o.initializer as? InitializerListExpression)?.initializers<InitializerListExpression>(
                0
            )
        assertNotNull(composite)
        assertIs<InitializerListExpression>(composite)
        assertIs<ObjectType>(composite.type)
        assertLocalName("MyStruct", composite.type)

        keyValue = composite.initializers<KeyValueExpression>(0)
        assertNotNull(keyValue)
        assertLocalName("Field", keyValue.key)
        assertRefersTo(keyValue.key, field)
        assertLiteralValue(10, keyValue.value)

        val rr = tu.variables["rr"]
        assertNotNull(rr)

        var init = rr.initializer
        assertIs<InitializerListExpression>(init)

        keyValue = init.initializers<KeyValueExpression>(0)
        assertNotNull(keyValue)

        var key = keyValue.key
        assertNotNull(key)

        var zero = result.variables["submodule.Zero"]
        assertNotNull(zero)
        assertRefersTo(key, zero)

        val mapr = tu.variables["mapr"]
        assertNotNull(mapr)

        init = mapr.initializer
        assertIs<InitializerListExpression>(init)

        keyValue = init.initializers<KeyValueExpression>(0)
        assertNotNull(keyValue)

        key = keyValue.key
        assertNotNull(key)

        zero = result.variables["submodule.Zero"]
        assertNotNull(zero)
        assertRefersTo(key, zero)
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

        var type = main.type
        assertIs<FunctionType>(type)
        assertNotNull(type)
        assertLocalName("func()", type)
        assertEquals(0, type.parameters.size)
        assertEquals(0, type.returnTypes.size)

        val funcA = p.functions["funcA"]
        assertNotNull(funcA)
        assertEquals(1, funcA.parameters.size)
        assertEquals(2, funcA.returnTypes.size)

        type = funcA.type
        assertIs<FunctionType>(type)
        assertNotNull(type)
        assertLocalName("func(string) (int, error)", type)
        assertEquals(funcA.parameters.size, type.parameters.size)
        assertEquals(funcA.returnTypes.size, type.returnTypes.size)
        assertEquals(listOf("int", "error"), type.returnTypes.map { it.name.localName })

        var body = main.body as? Block
        assertNotNull(body)

        var callExpression = body.calls.firstOrNull()
        assertNotNull(callExpression)

        assertLocalName("funcA", callExpression)
        assertEquals(funcA, callExpression.invokes.iterator().next())

        val s = funcA.parameters.first()
        assertNotNull(s)
        assertLocalName("s", s)
        assertEquals(tu.primitiveType("string"), s.type)

        assertLocalName("funcA", funcA)

        body = funcA.body as? Block
        assertNotNull(body)

        callExpression = body.statements.first() as? CallExpression
        assertNotNull(callExpression)

        assertFullName("fmt.Printf", callExpression)
        assertLocalName("Printf", callExpression)

        val literal = callExpression.arguments.firstOrNull() as? Literal<*>
        assertNotNull(literal)

        assertEquals("%s", literal.value)
        assertEquals(tu.primitiveType("string"), literal.type)

        val ref = callExpression.arguments[1] as? Reference
        assertNotNull(ref)

        assertLocalName("s", ref)
        assertEquals(s, ref.refersTo)

        val stmt = body.statements[1] as? AssignExpression
        assertNotNull(stmt)

        val a = stmt.lhs.firstOrNull() as? Reference
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

        val funcB = tu.functions["funcB"]
        assertNotNull(funcB)
        assertEquals(3, funcB.parameters.size)
        assertEquals(
            listOf("uint8[]", "uint8[]", "int"),
            funcB.parameters.map { it.type.name.toString() },
        )

        type = funcB.type
        assertIs<FunctionType>(type)
        assertNotNull(type)
        assertEquals(3, type.parameters.size)
        assertEquals(
            listOf("uint8[]", "uint8[]", "int"),
            type.parameters.map { it.name.toString() },
        )

        val funcC = tu.functions["funcC"]
        assertNotNull(funcC)
        assertEquals(1, funcC.parameters.size)
        assertEquals(listOf("string"), funcC.parameters.map { it.type.name.toString() })

        type = funcC.type
        assertIs<FunctionType>(type)
        assertNotNull(type)
        assertEquals(1, type.parameters.size)
        assertEquals(listOf("string"), type.parameters.map { it.name.toString() })
    }

    @Test
    fun testMemberCalls() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val result =
            analyze(listOf(topLevel.resolve("struct.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(result)

        val p = result.namespaces["p"]
        assertNotNull(p)

        val myStruct = p.records["MyStruct"]
        assertNotNull(myStruct)

        val methods = myStruct.methods
        val myFunc = methods.firstOrNull()
        assertNotNull(myFunc)
        assertLocalName("MyFunc", myFunc)

        val body = myFunc.body as? Block
        assertNotNull(body)

        val printfCall = body.statements.first() as? CallExpression
        assertNotNull(printfCall)
        assertLocalName("Printf", printfCall)
        assertFullName("fmt.Printf", printfCall)

        val arg1 = printfCall.arguments[0] as? MemberCallExpression

        assertNotNull(arg1)
        assertLocalName("myOtherFunc", arg1)
        assertFullName("p.MyStruct.myOtherFunc", arg1)

        assertEquals(myFunc.receiver, (arg1.base as? Reference)?.refersTo)
    }

    @Test
    fun testCorrectInference() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val result =
            analyze(listOf(topLevel.resolve("struct.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(result)

        // Make sure, that we inferred the Printf function at the correct namespace
        val fmt = result.namespaces["fmt"]
        assertNotNull(fmt)

        val printf = fmt.functions["Printf"]
        assertNotNull(printf)
        assertTrue(printf.isInferred)

        val printfCall = result.calls["fmt.Printf"]
        assertNotNull(printfCall)
        assertLocalName("Printf", printfCall)
        assertFullName("fmt.Printf", printfCall)
        assertInvokes(printfCall, printf)
    }

    @Test
    fun testPointerTypeInference() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val result =
            analyze(listOf(topLevel.resolve("inference.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(result)

        // There should be only a single one inferred method with that name
        val queryDecl = result.methods("Query").singleOrNull()
        assertNotNull(queryDecl)
        assertTrue(queryDecl.isInferred)

        val query = result.mcalls["Query"]
        assertInvokes(query, queryDecl)
    }

    @Test
    fun testQualifiedCallInMethod() {
        val stdLib = Path.of("src", "test", "resources", "golang-std")
        val topLevel = Path.of("src", "test", "resources", "golang")
        val result =
            analyze(
                listOf(topLevel.resolve("struct.go").toFile(), stdLib.resolve("fmt").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
                it.includePath(stdLib)
            }
        assertNotNull(result)

        val fmt = result.namespaces["fmt"]
        assertNotNull(fmt)

        val printf = fmt.functions["Printf"]
        assertNotNull(printf)
        assertFalse(printf.isInferred)

        val printfCall = result.calls["fmt.Printf"]
        assertNotNull(printfCall)
        assertInvokes(printfCall, printf)
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

        val body = myFunc.body as? Block
        assertNotNull(body)

        val assign = body.statements.first() as? AssignExpression
        assertNotNull(assign)

        val lhs = assign.lhs.firstOrNull() as? MemberExpression
        assertNotNull(lhs)
        assertEquals(myFunc.receiver, (lhs.base as? Reference)?.refersTo)
        assertLocalName("Field", lhs)
        assertEquals(tu.primitiveType("int"), lhs.type)

        val rhs = assign.rhs.firstOrNull() as? Reference
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

        val body = main.body as? Block
        assertNotNull(body)

        val b =
            (body.statements.first() as? DeclarationStatement)?.singleDeclaration
                as? VariableDeclaration

        assertNotNull(b)
        assertLocalName("b", b)
        assertEquals(tu.primitiveType("bool"), b.type)

        // Technically, "true" and "false" are builtin variables, NOT literals in Golang,
        // however, we parse them as literals to have compatibility with other languages
        // also enable all features, such as value resolution based on literal values.
        assertLiteralValue(true, b.initializer)

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

        val body = myFunc.body as? Block
        assertNotNull(body)

        val switch = body.statements.first() as? SwitchStatement
        assertNotNull(switch)

        val list = switch.statement as? Block
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
                    topLevel.resolve("struct.go").toFile(),
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
            }

        assertNotNull(result)
        with(result) {
            val app = result.components["application"]
            assertNotNull(app)

            val tus = app.translationUnits

            // fetch the function declaration from the struct TU
            val tu2 = tus[1]

            val p2 = tu2.namespaces["p"]
            assertNotNull(p2)

            val myOtherFunc = p2.methods["myOtherFunc"]
            assertNotNull(myOtherFunc)
            assertFalse(myOtherFunc.isImplicit)

            val newMyStruct = p2.functions["NewMyStruct"]
            assertNotNull(newMyStruct)

            // and compare it with the call TU
            val tu = tus[0]

            val p = tu.namespaces["p"]
            assertNotNull(p)

            val main = p.functions["main"]
            assertNotNull(main)

            val body = main.body as? Block
            assertNotNull(body)

            val c = body.variables["c"]

            assertNotNull(c)
            // type will be inferred from the function declaration
            assertEquals(assertResolvedType("p.MyStruct").pointer(), c.type)

            val newMyStructCall = assertIs<CallExpression>(c.firstAssignment)
            assertInvokes(newMyStructCall, newMyStruct)

            val call = tu.calls["myOtherFunc"] as? MemberCallExpression
            assertNotNull(call)

            val base = call.base as? Reference
            assertNotNull(base)
            assertRefersTo(base, c)

            val myOtherFuncCall = tu.calls["myOtherFunc"]
            assertNotNull(myOtherFuncCall)
            assertInvokes(myOtherFuncCall, myOtherFunc)

            val go = main.calls["go"]
            assertNotNull(go)
        }
    }

    @Test
    fun testFor() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("for.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }

        val main = tu.functions["main.main"]
        assertNotNull(main)

        val f = main.forLoops.firstOrNull()
        assertNotNull(f)

        assertTrue(f.condition is BinaryOperator)
        assertTrue(f.statement is Block)
        assertTrue(f.initializerStatement is AssignExpression)
        assertTrue(f.iterationStatement is UnaryOperator)

        val each = main.forEachLoops.firstOrNull()
        assertNotNull(each)

        val bytes = assertIs<Reference>(each.iterable)
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
                // the order does not matter anymore now, so we intentionally keep it in the reverse
                // order
                listOf(
                    topLevel.resolve("cmd/awesome/main.go").toFile(),
                    topLevel.resolve("util/stuff.go").toFile(),
                    topLevel.resolve("awesome.go").toFile(),
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
            }

        val app = result.components["application"]
        assertNotNull(app)

        val tus = app.translationUnits
        val tuAwesome = tus.firstOrNull { it.name.endsWith("awesome.go") }
        assertNotNull(tuAwesome)

        val newAwesome = tuAwesome.functions["awesome.NewAwesome"]
        assertNotNull(newAwesome)

        val tuMain = tus.firstOrNull { it.name.endsWith("main.go") }
        assertNotNull(tuMain)

        val import = tuMain.imports["awesome"]
        assertNotNull(import)
        assertEquals("example.io/awesome", import.importURL)

        val main = tuMain.functions["main.main"]
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

        val assign = main.assigns.firstOrNull()
        assertNotNull(assign)
        assertEquals("comment before assignment", assign.comment)

        val declStmt = main.allChildren<DeclarationStatement>().firstOrNull()
        assertNotNull(declStmt)
        assertEquals("comment before declaration", declStmt.comment)

        val s = mainNamespace.records["main.s"]
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

        val mainPackage = tu.namespaces["main"]
        assertNotNull(mainPackage)

        val main = mainPackage.functions["main"]
        assertNotNull(main)

        val assign = main.assigns.firstOrNull()
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
                (it.target as? Reference)?.refersTo == i
            }
        assertNotNull(assign)

        val call = assertIs<CallExpression>(assign.value)
        assertLocalName("funcA", call)
    }

    @Test
    fun testTypes() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("types.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val s = tu.variables["s"]
        assertNotNull(s)
        assertIs<ObjectType>(s.type)
        assertLocalName("struct{Field int}", s.type)

        val i = tu.variables["i"]
        assertNotNull(i)
        assertIs<ObjectType>(i.type)
        assertLocalName("interface{MyMethod(int) error}", i.type)

        val m = tu.variables["m"]
        assertNotNull(m)
        var type = m.type
        assertIs<ObjectType>(type)
        assertEquals(listOf("int", "string"), type.generics.map { it.name.toString() })

        val a = tu.variables["a"]
        assertNotNull(a)
        type = a.type
        assertIs<PointerType>(type)
        assertEquals(PointerType.PointerOrigin.ARRAY, type.pointerOrigin)
        assertLocalName("int", type.elementType)

        val f = tu.variables["f"]
        assertNotNull(f)
        assertIs<FunctionType>(f.type.underlyingType)

        val g = tu.variables["g"]
        assertNotNull(g)
        assertLocalName("string", g.type)

        val h = tu.variables["h"]
        assertNotNull(h)

        type = h.type
        assertIs<ObjectType>(type)
        assertLocalName("newType", type)

        assertEquals(1, type.recordDeclaration?.methods?.size)
    }

    @Test
    fun testResolveStdLibImport() {
        val stdLib = Path.of("src", "test", "resources", "golang-std")
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("function.go").toFile(), stdLib.resolve("fmt").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
                it.includePath(stdLib)
            }

        assertNotNull(tu)

        val p = tu.namespaces["p"]
        assertNotNull(p)

        val main = p.functions["main"]
        assertNotNull(main)

        val printfCall = main.calls["fmt.Printf"]
        assertNotNull(printfCall)

        val printf = printfCall.invokes.firstOrNull()
        assertNotNull(printf)
        assertEquals("print.go", File(printf.location?.artifactLocation?.uri?.path.toString()).name)
    }

    @Test
    fun testBuildTags() {
        val stdLib = Path.of("src", "test", "resources", "golang-std")
        val topLevel = Path.of("src", "test", "resources", "golang", "integration")

        // make sure we parse main.go as the last
        val files =
            listOf(
                    "func_darwin.go",
                    "func_darwin_arm64.go",
                    "func_ios.go",
                    "func_linux_arm64.go",
                    "cmd/buildtags/main.go",
                )
                .map { topLevel.resolve(it).toFile() }
                .toMutableList()

        // add the std lib
        files += stdLib.resolve("fmt").toFile()

        val result =
            analyze(files, topLevel, true) {
                it.registerLanguage<GoLanguage>()
                it.includePath(stdLib)
                it.useParallelFrontends(false)
                it.symbols(mapOf("GOOS" to "darwin", "GOARCH" to "arm64"))
            }

        assertNotNull(result)

        val funcOS = result.functions("OS")
        assertEquals(1, funcOS.size)

        val specific = result.functions["someSpecific"]
        assertNotNull(specific)
    }

    @Test
    fun testInterfaceDeriveFrom() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("struct.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val doInterface = tu.functions["DoInterface"]
        assertNotNull(doInterface)

        val call = tu.calls["DoInterface"]
        assertNotNull(call)
        assertInvokes(call, doInterface)
    }

    @Test
    fun testFuncOptions() {
        val topLevel = Path.of("src", "test", "resources", "golang", "options")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("srv.go").toFile(),
                    topLevel.resolve("srv_option.go").toFile(),
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(result)

        val inner = result.records["inner"]
        assertNotNull(inner)

        val field = inner.fields["field"]
        assertNotNull(field)

        val assign = result.assignments.firstOrNull()
        assertNotNull(assign)

        val mce = assign.target
        assertIs<MemberExpression>(mce)
        assertRefersTo(mce, field)
    }

    @Ignore
    @Test
    fun testChainedCall() {
        val topLevel = Path.of("src", "test", "resources", "golang", "chained")
        val tu =
            analyze(listOf(topLevel.resolve("chained.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val type = tu.records["Type"]
        assertNotNull(type)

        val elem = type.methods["Elem"]
        assertNotNull(elem)

        val call = tu.calls["Elem"]
        assertInvokes(call, elem)
    }

    @Test
    fun testChainedCast() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyze(listOf(topLevel.resolve("cast.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        assertEquals(0, tu.calls.size)
        assertEquals(
            listOf("string", "error", "p.myError"),
            tu.casts.map { it.castType.name.toString() },
        )
    }

    @Ignore
    @Test
    fun testComplexResolution() {
        val topLevel = Path.of("src", "test", "resources", "golang", "complex_resolution")
        val result =
            analyze(
                listOf(
                    // We need to keep them in this particular order, otherwise we will not resolve
                    // cross-package correctly yet
                    topLevel.resolve("util/util.go").toFile(),
                    topLevel.resolve("calls/calls.go").toFile(),
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(result)

        val language = result.finalCtx.availableLanguage<GoLanguage>()
        assertNotNull(language)

        val meter = result.variables["util.Meter"]
        assertNotNull(meter)
        assertLocalName("Length", meter.type)

        // All calls including the one to "funcy" (which is a dynamic invoke) should be resolved to
        // non-inferred functions
        val calls = result.calls
        calls.forEach {
            assertTrue(it.invokes.isNotEmpty(), "${it.name}'s invokes should not be empty")
            it.invokes.forEach { func ->
                assertFalse(func.isInferred, "${func.name}'s should not be inferred")
            }
        }

        val funcy = result.calls["funcy"]
        assertNotNull(funcy)
        funcy.invokeEdges.all { it.dynamicInvoke == true }

        // We should be able to resolve the call from our stored "do" function to funcy
        assertInvokes(funcy, result.functions["do"])

        val refs = result.refs.filter { it.name.localName != language.anonymousIdentifier }
        refs.forEach { assertNotNull(it.refersTo, "${it.name}'s referTo is empty") }
    }

    @Test
    fun testPackages() {
        val topLevel = Path.of("src", "test", "resources", "golang", "packages")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("api/apiv1.go").toFile(),
                    topLevel.resolve("packages.go").toFile(),
                    topLevel.resolve("cmd/packages/packages.go").toFile(),
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(result)
    }

    @Ignore
    @Test
    fun testMultiValueEvaluate() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("eval.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val f = tu.refs("f").lastOrNull()
        assertNotNull(f)

        val values = f.evaluate(MultiValueEvaluator())
        assertEquals(setOf("GPT", "GTP"), values)
        println(f.printDFG())
    }
}
