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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.ExperimentalPython
import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

@ExperimentalPython
class PythonFrontendTest : BaseTest() {
    // TODO ensure gradle doesn't remove those classes
    private val dummyRegion = Region()
    private val dummyPhysicalLocation = PhysicalLocation(URI(""), dummyRegion)

    @Test
    fun testLiteral() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("literal.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("literal", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)
        assertEquals("literal", p.name)

        val b = p.getDeclarationsByName("b", VariableDeclaration::class.java).iterator().next()
        assertNotNull(b)
        assertEquals("b", b.name)
        assertEquals(TypeParser.createFrom("bool", false), b.type)

        val i = p.getDeclarationsByName("i", VariableDeclaration::class.java).iterator().next()
        assertNotNull(i)
        assertEquals("i", i.name)
        assertEquals(TypeParser.createFrom("int", false), i.type)

        val f = p.getDeclarationsByName("f", VariableDeclaration::class.java).iterator().next()
        assertNotNull(f)
        assertEquals("f", f.name)
        assertEquals(TypeParser.createFrom("float", false), f.type)

        /*
        val c = p.getDeclarationsByName("c", VariableDeclaration::class.java).iterator().next()
        assertNotNull(c)
        assertEquals("c", c.name)
        assertEquals(TypeParser.createFrom("complex", false), c.type)
        */

        val t = p.getDeclarationsByName("t", VariableDeclaration::class.java).iterator().next()
        assertNotNull(t)
        assertEquals("t", t.name)
        assertEquals(TypeParser.createFrom("str", false), t.type)

        val n = p.getDeclarationsByName("n", VariableDeclaration::class.java).iterator().next()
        assertNotNull(n)
        assertEquals("n", n.name)
        assertEquals(TypeParser.createFrom("None", false), n.type)
    }

    @Test
    fun testFunctionDeclaration() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("function.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("function", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        val foo = p.declarations.first() as? FunctionDeclaration
        assertNotNull(foo)

        val bar = p.declarations[1] as? FunctionDeclaration
        assertNotNull(bar)
        assertEquals(2, bar.parameters.size)

        var callExpression = (foo.body as? CompoundStatement)?.statements?.get(0) as? CallExpression
        assertNotNull(callExpression)

        assertEquals("bar", callExpression.name)
        assertEquals(bar, callExpression.invokes.iterator().next())

        val edge = callExpression.argumentsPropertyEdge[1]

        assertEquals("s2", edge.getProperty(Properties.NAME))

        val s = bar.parameters.first()
        assertNotNull(s)
        assertEquals("s", s.name)
        assertEquals(TypeParser.createFrom("str", false), s.type)

        assertEquals("bar", bar.name)

        val compStmt = bar.body as? CompoundStatement
        assertNotNull(compStmt)
        assertNotNull(compStmt.statements)

        callExpression = compStmt.statements[0] as? CallExpression
        assertNotNull(callExpression)

        assertEquals("print", callExpression.fqn)

        val literal = callExpression.arguments.first() as? Literal<*>
        assertNotNull(literal)

        assertEquals("bar(s) here: ", literal.value)
        assertEquals(TypeParser.createFrom("str", false), literal.type)

        val ref = callExpression.arguments[1] as? DeclaredReferenceExpression
        assertNotNull(ref)

        assertEquals("s", ref.name)
        assertEquals(s, ref.refersTo)

        val stmt = compStmt.statements[1] as? DeclarationStatement
        assertNotNull(stmt)

        val a = stmt.singleDeclaration as? VariableDeclaration
        assertNotNull(a)

        assertEquals("a", a.name)

        val op = a.initializer as? BinaryOperator
        assertNotNull(op)

        assertEquals("+", op.operatorCode)

        val lhs = op.lhs as? Literal<*>
        assertNotNull(lhs)

        assertEquals(1, (lhs.value as? Long)?.toInt())

        val rhs = op.rhs as? Literal<*>
        assertNotNull(rhs)

        assertEquals(2, (rhs.value as? Long)?.toInt())

        val r = compStmt.statements[2] as? ReturnStatement
        assertNotNull(r)
    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("if.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("if", NamespaceDeclaration::class.java).iterator().next()

        val main = p.getDeclarationsByName("foo", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(main)

        val body = main.body as? CompoundStatement

        assertNotNull(body)

        val sel =
            (body.statements.first() as? DeclarationStatement)?.singleDeclaration as?
                VariableDeclaration
        assertNotNull(sel)
        assertEquals("sel", sel.name)
        assertEquals(TypeParser.createFrom("bool", false), sel.type)

        val initializer = sel.initializer as? Literal<*>

        assertNotNull(initializer)
        assertEquals(TypeParser.createFrom("bool", false), initializer.type)
        assertEquals("True", initializer.name)

        val `if` = body.statements[1] as? IfStatement
        assertNotNull(`if`)
    }

    @Test
    fun testSimpleClass() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("simple_class.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)
        val p =
            tu.getDeclarationsByName("simple_class", NamespaceDeclaration::class.java)
                .iterator()
                .next()
        assertNotNull(p)

        val cls =
            p.getDeclarationsByName("SomeClass", RecordDeclaration::class.java).iterator().next()
        assertNotNull(cls)
        val foo = p.getDeclarationsByName("foo", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(foo)

        assertEquals("SomeClass", cls.name)
        assertEquals(1, cls.methods.size)
        assertEquals(1, cls.constructors.size) // auto generated by cpg
        assertEquals(true, cls.constructors.first().isInferred)

        val clsfunc = cls.methods.first()
        assertEquals("someFunc", clsfunc.name)

        assertEquals("foo", foo.name)
        val body = foo.body as? CompoundStatement
        assertNotNull(body)
        assertNotNull(body.statements)
        assertEquals(2, body.statements.size)

        val s1 = body.statements[0] as? DeclarationStatement
        assertNotNull(s1)
        val s2 = body.statements[1] as? MemberCallExpression
        assertNotNull(s2)

        val c1 = s1.declarations[0] as? VariableDeclaration
        assertNotNull(c1)
        assertEquals("c1", c1.name)
        val ctor = (c1.initializer as? ConstructExpression)?.constructor
        assertEquals(ctor, cls.constructors.first())
        assertEquals(TypeParser.createFrom("SomeClass", false), c1.type)

        assertEquals(c1, (s2.base as? DeclaredReferenceExpression)?.refersTo)
        assertEquals(1, s2.invokes.size)
        assertEquals(clsfunc, s2.invokes.first())

        // member
    }

    @Test
    fun testIfExpr() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ifexpr.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("ifexpr", NamespaceDeclaration::class.java).iterator().next()

        val main = p.getDeclarationsByName("foo", FunctionDeclaration::class.java).iterator().next()

        assertNotNull(main)

        val body = (main.body as? CompoundStatement)?.statements?.get(0) as? DeclarationStatement

        assertNotNull(body)

        val foo = body.singleDeclaration as? VariableDeclaration
        assertNotNull(foo)
        assertEquals("foo", foo.name)
        assertEquals(TypeParser.createFrom("int", false), foo.type)

        val initializer = foo.initializer as? ConditionalExpression

        assertNotNull(initializer)
        assertEquals(TypeParser.createFrom("int", false), initializer.type)

        val ifCond = initializer.condition as? Literal<*>
        assertNotNull(ifCond)
        val thenExpr = initializer.thenExpr as? Literal<*>
        assertNotNull(thenExpr)
        val elseExpr = initializer.elseExpr as? Literal<*>
        assertNotNull(elseExpr)

        assertEquals(TypeParser.createFrom("bool", false), ifCond.type)
        assertEquals(false, ifCond.value)

        assertEquals(TypeParser.createFrom("int", false), thenExpr.type)
        assertEquals(21, (thenExpr.value as? Long)?.toInt())

        assertEquals(TypeParser.createFrom("int", false), elseExpr.type)
        assertEquals(42, (elseExpr.value as? Long)?.toInt())
    }

    @Test
    fun testFields() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("class_fields.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("class_fields", NamespaceDeclaration::class.java)
                .iterator()
                .next()

        val recordFoo =
            p.getDeclarationsByName("Foo", RecordDeclaration::class.java).iterator().next()

        assertNotNull(recordFoo)
        assertEquals("Foo", recordFoo.name)
        assertEquals(recordFoo.fields.size, 4)
        assertEquals(recordFoo.methods.size, 1)

        val fieldX = recordFoo.getField("x")
        assertNotNull(fieldX)
        val fieldY = recordFoo.getField("y")
        assertNotNull(fieldY)
        val fieldZ = recordFoo.getField("z")
        assertNotNull(fieldZ)
        val fieldBaz = recordFoo.getField("baz")
        assertNotNull(fieldBaz)

        assertEquals(fieldX.name, "x")
        assertEquals(fieldY.name, "y")
        assertEquals(fieldZ.name, "z")
        assertEquals(fieldBaz.name, "baz")

        assertNull(fieldX.initializer)
        assertNotNull(fieldY.initializer)
        assertNull(fieldZ.initializer)
        assertNotNull(fieldBaz.initializer)

        val methBar = recordFoo.methods[0]
        assertNotNull(methBar)
        assertEquals(methBar.name, "bar")

        val barZ = (methBar.body as? CompoundStatement)?.statements?.get(0) as? MemberExpression
        assertNotNull(barZ)
        assertEquals(barZ.refersTo, fieldZ)

        val barBaz =
            (methBar.body as? CompoundStatement)?.statements?.get(1) as? DeclarationStatement
        assertNotNull(barBaz)
        val barBazInner = barBaz.declarations[0] as? FieldDeclaration
        assertNotNull(barBazInner)
        assertEquals("baz", barBazInner.name)
        assertNotNull(barBazInner.initializer)
    }

    @Test
    fun testSelf() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("class_self.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("class_self", NamespaceDeclaration::class.java)
                .iterator()
                .next()

        val recordFoo =
            p.getDeclarationsByName("Foo", RecordDeclaration::class.java).iterator().next()

        assertNotNull(recordFoo)
        assertEquals("Foo", recordFoo.name)

        assertEquals(1, recordFoo.fields.size)
        val somevar = recordFoo.fields[0]
        assertNotNull(somevar)
        assertEquals("somevar", somevar.name)
        // assertEquals(TypeParser.createFrom("int", false), somevar.type) TODO fix type deduction

        assertEquals(2, recordFoo.methods.size)
        val bar = recordFoo.methods[0]
        val foo = recordFoo.methods[1]
        assertNotNull(bar)
        assertNotNull(foo)
        assertEquals("bar", bar.name)
        assertEquals(recordFoo, bar.recordDeclaration)
        assertEquals("foo", foo.name)
        assertEquals(recordFoo, foo.recordDeclaration)

        val recv = bar.receiver
        assertNotNull(recv)
        assertEquals("self", recv.name)
        assertEquals(TypeParser.createFrom("Foo", false), recv.type)

        assertEquals(1, bar.parameters?.size)
        val i = bar.parameters?.get(0)
        assertNotNull(i)

        assertEquals("i", i.name)
        assertEquals(TypeParser.createFrom("int", false), i.type)

        // self.somevar = i
        val someVarDeclaration =
            ((bar.body as? CompoundStatement)?.statements?.get(0) as? DeclarationStatement)
                ?.declarations?.first() as?
                FieldDeclaration
        assertNotNull(someVarDeclaration)
        assertEquals("somevar", someVarDeclaration.name)
        assertEquals(i, (someVarDeclaration.initializer as? DeclaredReferenceExpression)?.refersTo)

        val fooMemCall =
            (foo.body as? CompoundStatement)?.statements?.get(0) as? MemberCallExpression
        assertNotNull(fooMemCall)
        val mem = fooMemCall.member as? DeclaredReferenceExpression
        assertNotNull(mem)
        assertEquals("bar", mem.name)
        assertEquals(".", fooMemCall.operatorCode)
        assertEquals("Foo.bar", fooMemCall.fqn)
        assertEquals(1, fooMemCall.invokes.size)
        assertEquals(bar, fooMemCall.invokes[0])
        assertEquals("self", fooMemCall.base.name)
    }

    @Test
    fun testCtor() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("class_ctor.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("class_ctor", NamespaceDeclaration::class.java)
                .iterator()
                .next()

        val recordFoo =
            p.getDeclarationsByName("Foo", RecordDeclaration::class.java).iterator().next()

        assertNotNull(recordFoo)
        assertEquals("Foo", recordFoo.name)

        assertEquals(2, recordFoo.methods.size)
        val fooCtor = recordFoo.methods[0] as? ConstructorDeclaration
        assertNotNull(fooCtor)
        val foobar = recordFoo.methods[1]
        assertNotNull(foobar)

        assertEquals("__init__", fooCtor.name)
        assertEquals("foobar", foobar.name)

        val bar = p.getDeclarationsByName("bar", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(bar)
        assertEquals("bar", bar.name)

        assertEquals(2, (bar.body as? CompoundStatement)?.statements?.size)
        val line1 = (bar.body as? CompoundStatement)?.statements?.get(0) as? DeclarationStatement
        assertNotNull(line1)
        val line2 = (bar.body as? CompoundStatement)?.statements?.get(1) as? MemberCallExpression
        assertNotNull(line2)

        assertEquals(1, line1.declarations.size)
        val fooDecl = line1.declarations[0] as? VariableDeclaration
        assertNotNull(fooDecl)
        assertEquals("foo", fooDecl.name)
        assertEquals(TypeParser.createFrom("Foo", false), fooDecl.type)
        val initializer = fooDecl.initializer as? ConstructExpression
        assertEquals(fooCtor, initializer?.constructor)

        assertEquals(fooDecl, (line2.base as? DeclaredReferenceExpression)?.refersTo)
        assertEquals(foobar, line2.invokes[0])
    }

    @Test
    fun testIssue432() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("issue432.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("issue432", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        val clsCounter =
            p.getDeclarationsByName("counter", RecordDeclaration::class.java).iterator().next()
        assertNotNull(clsCounter)

        val methCount =
            p.getDeclarationsByName("count", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(methCount)

        val clsC1 = p.getDeclarationsByName("c1", RecordDeclaration::class.java).iterator().next()
        assertNotNull(clsC1)

        // class counter
        assertEquals(clsCounter.name, "counter")

        // TODO missing check for "pass"

        // def count(c)
        assertEquals(methCount.name, "count")
        assertEquals(methCount.parameters.size, 1)

        val countParam = methCount.parameters[0]
        assertNotNull(countParam)
        assertEquals(countParam.name, "c")

        val countStmt = (methCount.body as? CompoundStatement)?.statements?.get(0) as? IfStatement
        assertNotNull(countStmt)

        val ifCond = countStmt.condition as? BinaryOperator
        assertNotNull(ifCond)

        val lhs = ifCond.lhs as? MemberCallExpression
        assertNotNull(lhs)
        assertEquals((lhs.base as? DeclaredReferenceExpression)?.refersTo, countParam)
        assertEquals(lhs.name, "inc")
        assertEquals(lhs.arguments.size, 0)

        val ifThen =
            (countStmt.thenStatement as? CompoundStatement)?.statements?.get(0) as? CallExpression
        assertNotNull(ifThen)
        assertEquals(ifThen.invokes.first(), methCount)
        assertEquals(
            (ifThen.arguments.first() as? DeclaredReferenceExpression)?.refersTo,
            countParam
        )
        assertNull(countStmt.elseStatement)

        // class c1(counter)
        assertEquals(clsC1.name, "c1")
        assertEquals((clsC1.superClasses.first() as? ObjectType)?.recordDeclaration, clsCounter)
        assertEquals(clsC1.fields.size, 1)

        val field = clsC1.fields[0]
        assertNotNull(field)
        assertEquals(field.name, "total")

        // TODO assert initializer "total = 0"

        val meth = clsC1.methods[0]
        assertNotNull(meth)
        assertEquals(meth.name, "inc")
        assertEquals(meth.recordDeclaration, clsC1)

        val selfReceiver = meth.receiver
        assertNotNull(selfReceiver)
        assertEquals(selfReceiver.name, "self")
        assertEquals(meth.parameters.size, 0) // self is receiver and not a parameter

        val methBody = meth.body as? CompoundStatement
        assertNotNull(methBody)

        val assign = methBody.statements[0] as? BinaryOperator
        assertNotNull(assign)

        val assignLhs = assign.lhs as? MemberExpression
        val assignRhs = assign.rhs as? BinaryOperator
        assertEquals(assign.operatorCode, "=")
        assertNotNull(assignLhs)
        assertNotNull(assignRhs)
        assertEquals((assignLhs.base as? DeclaredReferenceExpression)?.refersTo, selfReceiver)
        assertEquals(assignRhs.operatorCode, "+")

        val assignRhsLhs =
            assignRhs.lhs as?
                MemberExpression // the second "self.total" in "self.total = self.total + 1"
        assertNotNull(assignRhsLhs)
        assertEquals((assignRhsLhs.base as? DeclaredReferenceExpression)?.refersTo, selfReceiver)

        val r = methBody.statements[1] as? ReturnStatement
        assertNotNull(r)
        assertEquals(
            ((r.returnValue as? MemberExpression)?.base as? DeclaredReferenceExpression)?.refersTo,
            selfReceiver
        )

        // TODO last line "count(c1())"
    }

    @Test
    fun testVarsAndFields() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("vars.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p = tu.getDeclarationsByName("vars", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        val clsFoo = p.getDeclarationsByName("Foo", RecordDeclaration::class.java).iterator().next()
        assertNotNull(clsFoo)

        val methBar = clsFoo.methods[0]
        assertNotNull(methBar)

        // val stmtsOutsideCls TODO
        val classFieldNoInitializer = clsFoo.getField("classFieldNoInitializer")
        assertNotNull(classFieldNoInitializer)
        val classFieldWithInit = clsFoo.getField("classFieldWithInit")
        assertNotNull(classFieldWithInit)
        val classFieldDeclaredInFunction = clsFoo.getField("classFieldDeclaredInFunction")
        assertNotNull(classFieldDeclaredInFunction)
        // assertEquals(3, clsFoo.fields.size) // TODO should "self" be considered a field here?

        assertNull(classFieldNoInitializer.initializer)
        assertNotNull(classFieldWithInit)

        // classFieldNoInitializer = classFieldWithInit
        val assignClsFieldOutsideFunc = clsFoo.statements[2] as? BinaryOperator
        assertNotNull(assignClsFieldOutsideFunc)
        assertEquals(
            (assignClsFieldOutsideFunc.lhs as? DeclaredReferenceExpression)?.refersTo,
            classFieldNoInitializer
        )
        assertEquals(
            (assignClsFieldOutsideFunc.rhs as? DeclaredReferenceExpression)?.refersTo,
            classFieldWithInit
        )
        assertEquals(assignClsFieldOutsideFunc.operatorCode, "=")

        val barBody = methBar.body as? CompoundStatement
        assertNotNull(barBody)

        // self.classFieldDeclaredInFunction = 456
        val barStmt0 = barBody.statements[0] as? DeclarationStatement
        val decl0 = barStmt0?.declarations?.get(0) as? FieldDeclaration
        assertNotNull(decl0)
        assertEquals("classFieldDeclaredInFunction", decl0.name)
        assertNotNull(decl0.initializer)

        // self.classFieldNoInitializer = 789
        val barStmt1 = barBody.statements[1] as? BinaryOperator
        assertNotNull(barStmt1)
        assertEquals((barStmt1.lhs as? MemberExpression)?.refersTo, classFieldNoInitializer)

        // self.classFieldWithInit = 12
        val barStmt2 = barBody.statements[2] as? BinaryOperator
        assertNotNull(barStmt2)
        assertEquals((barStmt2.lhs as? MemberExpression)?.refersTo, classFieldWithInit)

        // classFieldNoInitializer = "shadowed"
        val barStmt3 = barBody.statements[3] as? BinaryOperator
        assertNotNull(barStmt3)
        assertEquals("=", barStmt3.operatorCode)
        assertEquals(
            classFieldNoInitializer,
            (barStmt3.lhs as? DeclaredReferenceExpression)?.refersTo
        )
        assertEquals("shadowed", (barStmt3.rhs as? Literal<*>)?.value)

        // classFieldWithInit = "shadowed"
        val barStmt4 = barBody.statements[4] as? BinaryOperator
        assertNotNull(barStmt4)
        assertEquals("=", barStmt4.operatorCode)
        assertEquals(classFieldWithInit, (barStmt4.lhs as? DeclaredReferenceExpression)?.refersTo)
        assertEquals("shadowed", (barStmt4.rhs as? Literal<*>)?.value)

        // classFieldDeclaredInFunction = "shadowed"
        val barStmt5 = barBody.statements[5] as? BinaryOperator
        assertNotNull(barStmt5)
        assertEquals("=", barStmt5.operatorCode)
        assertEquals(
            classFieldDeclaredInFunction,
            (barStmt5.lhs as? DeclaredReferenceExpression)?.refersTo
        )
        assertEquals("shadowed", (barStmt5.rhs as? Literal<*>)?.value)

        /* TODO:
        foo = Foo()
        foo.classFieldNoInitializer = 345
        foo.classFieldWithInit = 678
         */
    }

    @Test
    fun testLiterals() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("literal.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("literal", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        assertEquals("b", (p.declarations[0] as? VariableDeclaration)?.name)
        assertEquals("True", (p.declarations[0] as? VariableDeclaration)?.initializer?.code)
        assertEquals("i", (p.declarations[1] as? VariableDeclaration)?.name)
        assertEquals("42", (p.declarations[1] as? VariableDeclaration)?.initializer?.code)
        assertEquals("f", (p.declarations[2] as? VariableDeclaration)?.name)
        assertEquals("1.0", (p.declarations[2] as? VariableDeclaration)?.initializer?.code)
        assertEquals("t", (p.declarations[3] as? VariableDeclaration)?.name)
        assertEquals("\"Hello\"", (p.declarations[3] as? VariableDeclaration)?.initializer?.code)
        assertEquals("n", (p.declarations[4] as? VariableDeclaration)?.name)
        assertEquals("None", (p.declarations[4] as? VariableDeclaration)?.initializer?.code)
    }

    @Test
    fun testRegionInCPG() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("literal.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("literal", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        assertEquals(
            Region(1, 0, 1, 8),
            (p.declarations[0] as? VariableDeclaration)?.location?.region
        )
        assertEquals(
            Region(1, 4, 1, 8),
            (p.declarations[0] as? VariableDeclaration)?.initializer?.location?.region
        )
        assertEquals(
            Region(2, 0, 2, 6),
            (p.declarations[1] as? VariableDeclaration)?.location?.region
        )
        assertEquals(
            Region(3, 0, 3, 7),
            (p.declarations[2] as? VariableDeclaration)?.location?.region
        )
        assertEquals(
            Region(5, 0, 5, 11),
            (p.declarations[3] as? VariableDeclaration)?.location?.region
        )
        assertEquals(
            Region(6, 0, 6, 8),
            (p.declarations[4] as? VariableDeclaration)?.location?.region
        )
    }

    @Test
    fun testMultiLevelMemberCall() { // TODO
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("multi_level_mem_call.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("multi_level_mem_call", NamespaceDeclaration::class.java)
                .iterator()
                .next()
        assertNotNull(p)

        // foo = bar.baz.zzz("hello")
        val foo = p.getDeclarationsByName("foo", VariableDeclaration::class.java)
        assertNotNull(foo)

        val initializer = foo.first()?.initializer as? MemberCallExpression
        assertNotNull(initializer)

        assertEquals("zzz", initializer.name)
        val base = initializer.base as? MemberExpression
        assertNotNull(base)
        assertEquals("baz", base.name)
        val baseBase = base.base as? DeclaredReferenceExpression
        assertNotNull(baseBase)
        assertEquals("bar", baseBase.name)

        val member = initializer.member as? DeclaredReferenceExpression
        assertNotNull(member)
        assertEquals("zzz", member.name)
    }

    @Test
    fun testIssue598() { // test for added functionality: "while" and "break"
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("issue598.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("issue598", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        val main = p.getDeclarationsByName("main", FunctionDeclaration::class.java).first()
        assertNotNull(main)

        val mainBody = (main as? FunctionDeclaration)?.body as? CompoundStatement
        assertNotNull(mainBody)

        val whlStmt = mainBody.statements[3] as? WhileStatement
        assertNotNull(whlStmt)

        val whlBody = whlStmt.statement as? CompoundStatement
        assertNotNull(whlBody)

        val xDeclaration = whlBody.statements[0] as? DeclarationStatement
        assertNotNull(xDeclaration)

        val ifStmt = whlBody.statements[1] as? IfStatement
        assertNotNull(ifStmt)

        val brk = ifStmt.elseStatement as? CompoundStatement
        assertNotNull(brk)
        brk.statements[0] as? BreakStatement
    }

    @Test
    fun testIssue615() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("issue615.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("issue615", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        assertEquals(1, p.declarations.size)
        assertEquals(2, p.statements.size)

        // test = [(1, 2, 3)]
        val testDeclaration = p.declarations[0] as? VariableDeclaration
        assertNotNull(testDeclaration)
        assertEquals("test", testDeclaration.name)
        val testDeclStmt = p.statements[0] as? DeclarationStatement
        assertNotNull(testDeclStmt)
        assertEquals(1, testDeclStmt.declarations.size)
        assertEquals(testDeclaration, testDeclStmt.declarations[0] as? VariableDeclaration)

        /* for loop:
        for t1, t2, t3 in test:
            print("bug ... {} {} {}".format(t1, t2, t3))
         */
        val forStmt = p.statements[1] as? ForEachStatement
        assertNotNull(forStmt)

        val forVariable = forStmt.variable as? InitializerListExpression
        assertNotNull(forVariable)
        assertEquals(3, forVariable.initializers.size)
        val t1Decl = forVariable.initializers[0] as? DeclaredReferenceExpression
        val t2Decl = forVariable.initializers[1] as? DeclaredReferenceExpression
        val t3Decl = forVariable.initializers[2] as? DeclaredReferenceExpression
        assertNotNull(t1Decl)
        assertNotNull(t2Decl)
        assertNotNull(t3Decl)
        // TODO no refersTo

        val iter = forStmt.iterable as? DeclaredReferenceExpression
        assertNotNull(iter)
        assertEquals(testDeclaration, iter.refersTo)

        val forBody = forStmt.statement as? CompoundStatement
        assertNotNull(forBody)
        assertEquals(1, forBody.statements.size)

        // print("bug ... {} {} {}".format(t1, t2, t3))
        val forBodyStmt = forBody.statements[0] as? CallExpression
        assertNotNull(forBodyStmt)
        assertEquals("print", forBodyStmt.name)

        val printArg = forBodyStmt.arguments[0] as? MemberCallExpression
        assertNotNull(printArg)
        val formatArgT1 = printArg.arguments[0] as? DeclaredReferenceExpression
        assertNotNull(formatArgT1)
        val formatArgT2 = printArg.arguments[1] as? DeclaredReferenceExpression
        assertNotNull(formatArgT2)
        val formatArgT3 = printArg.arguments[2] as? DeclaredReferenceExpression
        assertNotNull(formatArgT3)
        // TODO check refersTo
    }
    @Test
    fun testIssue473() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("issue473.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    PythonLanguageFrontend::class.java,
                    PythonLanguageFrontend.PY_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val p =
            tu.getDeclarationsByName("issue473", NamespaceDeclaration::class.java).iterator().next()
        assertNotNull(p)

        val ifStmt = p.statements.get(0) as? IfStatement
        assertNotNull(ifStmt)
        val ifCond = ifStmt.condition as? BinaryOperator
        assertNotNull(ifCond)
        val ifThen = ifStmt.thenStatement as? CompoundStatement
        assertNotNull(ifThen)
        val ifElse = ifStmt.elseStatement as? CompoundStatement
        assertNotNull(ifElse)

        // sys.version_info.minor > 9
        assertEquals(">", ifCond.operatorCode)
        assertEquals("minor", (ifCond.lhs as? DeclaredReferenceExpression)?.name)

        // phr = {"user_id": user_id} | content
        val phrDeclaration =
            (ifThen.statements.get(0) as? DeclarationStatement)?.declarations?.get(0) as?
                VariableDeclaration
        assertNotNull(phrDeclaration)
        assertEquals("phr", phrDeclaration.name)
        val phrInintializer = phrDeclaration.initializer as? BinaryOperator
        assertNotNull(phrInintializer)
        assertEquals("|", phrInintializer.operatorCode)
        assertEquals(true, phrInintializer.lhs is InitializerListExpression)

        // z = {"user_id": user_id}
        val elseStmt1 =
            (ifElse.statements.get(0) as? DeclarationStatement)?.declarations?.get(0) as?
                VariableDeclaration
        assertNotNull(elseStmt1)
        assertEquals("z", elseStmt1.name)

        // phr = {**z, **content}
        val elseStmt2 = ifElse.statements.get(1) as? BinaryOperator
        assertNotNull(elseStmt2)
        assertEquals("=", elseStmt2.operatorCode)
        val elseStmt2Rhs = elseStmt2.rhs as? InitializerListExpression
        assertNotNull(elseStmt2Rhs)
    }
}
