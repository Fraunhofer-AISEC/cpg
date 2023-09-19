/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.TestUtils.assertInvokes
import de.fraunhofer.aisec.cpg.TestUtils.assertRefersTo
import de.fraunhofer.aisec.cpg.assertFullName
import de.fraunhofer.aisec.cpg.assertLiteralValue
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.variables
import java.nio.file.Path
import kotlin.test.*

class DeclarationTest {
    @Test
    fun testUnnamedReceiver() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("unnamed.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val main = tu.byNameOrNull<NamespaceDeclaration>("main")
        assertNotNull(main)

        val myStruct = main.byNameOrNull<RecordDeclaration>("main.MyStruct")
        assertNotNull(myStruct)

        // Receiver should be null since its unnamed
        val myFunc = myStruct.byNameOrNull<MethodDeclaration>("MyFunc")
        assertNotNull(myFunc)
        assertNull(myFunc.receiver)
    }

    @Test
    fun testUnnamedParameter() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("unnamed.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val main = tu.byNameOrNull<NamespaceDeclaration>("main")
        assertNotNull(main)

        // Parameter should be there but not have a name
        val myGlobalFunc = main.byNameOrNull<FunctionDeclaration>("MyGlobalFunc")
        assertNotNull(myGlobalFunc)

        val param = myGlobalFunc.parameters.firstOrNull()
        assertNotNull(param)
        assertFullName("", param)
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
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["p"]
        assertNotNull(p)

        val myStruct = p.records["MyStruct"]
        assertNotNull(myStruct)
        assertEquals("struct", myStruct.kind)

        val fields = myStruct.fields
        assertEquals(
            listOf("MyField", "OtherStruct", "EvenAnotherStruct"),
            fields.map { it.name.localName }
        )

        var methods = myStruct.methods

        var myFunc = methods.firstOrNull()
        assertNotNull(myFunc)
        assertFullName("p.MyStruct.MyFunc", myFunc)

        val myField = fields.firstOrNull()
        assertNotNull(myField)

        assertLocalName("MyField", myField)
        assertEquals(tu.primitiveType("int"), myField.type)

        val myInterface = p.records["p.MyInterface"]
        assertNotNull(myInterface)
        assertEquals("interface", myInterface.kind)

        methods = myInterface.methods

        assertEquals(1, methods.size)

        myFunc = methods.first()

        assertLocalName("MyFunc", myFunc)
        assertLocalName("func() string", myFunc.type)

        val newMyStruct = p.functions["NewMyStruct"]
        assertNotNull(newMyStruct)

        val body = newMyStruct.body as? Block
        assertNotNull(body)

        val `return` = body.statements.first() as? ReturnStatement
        assertNotNull(`return`)

        val returnValue = `return`.returnValue as? UnaryOperator
        assertNotNull(returnValue)

        val s = p.variables["p.s"]
        assertNotNull(s)

        val type = s.type
        assertIs<ObjectType>(type)

        val record = type.recordDeclaration
        assertNotNull(record)

        val init = s.initializer
        assertIs<InitializerListExpression>(init)

        val keyValue = init.initializers<KeyValueExpression>(0)
        assertNotNull(keyValue)

        val key = keyValue.key
        assertNotNull(key)
        assertRefersTo(key, record.fields["field"])
    }

    @Test
    fun testEmbeddedInterface() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("embed.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val main = tu.byNameOrNull<NamespaceDeclaration>("main")
        assertNotNull(main)

        val myInterface = main.byNameOrNull<RecordDeclaration>("main.MyInterface")
        assertNotNull(myInterface)

        val myOtherInterface = main.byNameOrNull<RecordDeclaration>("main.MyOtherInterface")
        assertNotNull(myOtherInterface)

        // MyOtherInterface should be in the superClasses and superTypeDeclarations of MyInterface,
        // since it is embedded and thus MyInterface "extends" it
        assertContains(myInterface.superTypeDeclarations, myOtherInterface)
        assertTrue(myInterface.superClasses.any { it.name == myOtherInterface.name })
    }

    @Test
    fun testMultipleDeclarations() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("declare.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val main = tu.functions["main.main"]
        assertNotNull(main)

        // We should have 10 variables (a, b, c, d, (e,f), e, f, g, h, i)
        assertEquals(10, main.variables.size)

        // Four should have (literal) initializers
        val a = main.variables["a"]
        assertLiteralValue(1, a?.initializer)

        val b = main.variables["b"]
        assertLiteralValue(2, b?.initializer)

        val c = main.variables["c"]
        assertLiteralValue(3, c?.initializer)

        val d = main.variables["d"]
        assertLiteralValue(4, d?.initializer)

        val e = main.variables["e"]
        assertNotNull(e)
        // e does not have a direct initializer, since it is initialized through the tuple
        // declaration (e,f)
        assertNull(e.initializer)

        // The tuple (e,f) does have an initializer
        val ef = main.allChildren<TupleDeclaration> { it.name.toString() == "(e,f)" }.firstOrNull()
        assertNotNull(ef)
        assertIs<CallExpression>(ef.initializer)

        // The next two variables are using a short assignment, therefore they do not have an
        // initializer, but we can use the firstAssignment function
        val g = main.variables["g"]
        assertLiteralValue(5, g?.firstAssignment)

        val h = main.variables["h"]
        assertLiteralValue(6, h?.firstAssignment)

        // And they should all be connected to the arguments of the Printf call
        val printf = main.calls["Printf"]
        assertNotNull(printf)

        printf.arguments.drop(1).forEach {
            val ref = assertIs<Reference>(it)
            assertNotNull(ref.refersTo)
        }

        // We have eight assignments in total (7 initializers + 2 assign expressions)
        assertEquals(9, tu.assignments.size)
    }

    @Test
    fun testTypeConstraints() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("type_constraints.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val myStruct = tu.records["MyStruct"]
        assertNotNull(myStruct)

        val myInterface = tu.records["MyInterface"]
        assertNotNull(myInterface)
    }

    @Test
    fun testConst() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("const.go").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        with(tu) {
            val values =
                mapOf(
                    "one" to Pair(1, objectType("p.custom")),
                    "oneAsWell" to Pair(1, objectType("p.custom")),
                    "two" to Pair(2, primitiveType("int")),
                    "three" to Pair(3, primitiveType("int")),
                    "four" to Pair(4, primitiveType("int")),
                    "tenAsWell" to Pair(10, primitiveType("int")),
                    "five" to Pair(5, primitiveType("int")),
                    "fiveAsWell" to Pair(5, primitiveType("int")),
                    "six" to Pair(6, primitiveType("int")),
                    "fivehundred" to Pair(500, primitiveType("int")),
                    "sixhundred" to Pair(600, primitiveType("int")),
                    "onehundredandfive" to Pair(105, primitiveType("int")),
                )
            values.forEach {
                val variable = tu.variables[it.key]
                assertNotNull(variable, "variable \"${it.key}\" not found")
                assertEquals(it.value.first, variable.evaluate(), "${it.key} does not match")
                assertEquals(it.value.second, variable.type, "${it.key} has the wrong type")
            }
        }
    }

    @Test
    fun testImportAlias() {
        val stdLib = Path.of("src", "test", "resources", "golang-std")
        val topLevel = Path.of("src", "test", "resources", "golang")
        val result =
            TestUtils.analyze(
                listOf(
                    topLevel.resolve("importalias.go").toFile(),
                    stdLib.resolve("fmt").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage<GoLanguage>()
                it.includePath(stdLib)
            }
        assertNotNull(result)

        val printf = result.functions["fmt.Printf"]
        assertNotNull(printf)

        val callPrintf = result.calls["fmtother.Printf"]
        assertNotNull(callPrintf)
        assertInvokes(callPrintf, printf)

        val expr = result.allChildren<MemberExpression>().firstOrNull()
        assertNotNull(expr)

        val fmt = result.variables["fmt"]
        assertNotNull(fmt)

        val base = expr.base
        assertIs<Reference>(base)
        assertRefersTo(base, fmt)
    }

    @Test
    fun testFuncOptions() {
        val topLevel = Path.of("src", "test", "resources", "golang", "options")
        val result =
            TestUtils.analyze(
                listOf(
                    topLevel.resolve("srv_option.go").toFile(),
                    topLevel.resolve("srv.go").toFile(),
                ),
                topLevel,
                true
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
        assertNotNull(mce)
        assertIs<MemberExpression>(mce)
        assertRefersTo(mce, field)
    }
}
