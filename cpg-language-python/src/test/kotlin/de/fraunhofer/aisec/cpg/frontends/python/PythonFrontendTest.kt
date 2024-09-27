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

import de.fraunhofer.aisec.cpg.analysis.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Annotation
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.sarif.Region
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.math.pow
import kotlin.test.*

class PythonFrontendTest : BaseTest() {
    @Test
    fun testLiteral() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("literal.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)
        with(tu) {
            val p = tu.namespaces["literal"]
            assertNotNull(p)
            assertLocalName("literal", p)

            val b = p.variables["b"]
            assertNotNull(b)
            assertLocalName("b", b)
            assertEquals(assertResolvedType("bool"), b.type)
            assertEquals(true, (b.firstAssignment as? Literal<*>)?.value)

            val i = p.variables["i"]
            assertNotNull(i)
            assertLocalName("i", i)
            assertEquals(assertResolvedType("int"), i.type)
            assertEquals(42L, (i.firstAssignment as? Literal<*>)?.value)

            val f = p.variables["f"]
            assertNotNull(f)
            assertLocalName("f", f)
            assertEquals(assertResolvedType("float"), f.type)
            assertEquals(1.0, (f.firstAssignment as? Literal<*>)?.value)

            val c = p.variables["c"]
            assertNotNull(c)
            assertLocalName("c", c)
            // assertEquals(tu.primitiveType("complex"), c.type) TODO: this is currently "UNKNOWN"
            // assertEquals("(3+5j)", (c.firstAssignment as? Literal<*>)?.value) // TODO: this is
            // currently a binary op

            val t = p.variables["t"]
            assertNotNull(t)
            assertLocalName("t", t)
            assertEquals(assertResolvedType("str"), t.type)
            assertEquals("Hello", (t.firstAssignment as? Literal<*>)?.value)

            val n = p.variables["n"]
            assertNotNull(n)
            assertLocalName("n", n)
            assertEquals(assertResolvedType("None"), n.type)
            assertEquals(null, (n.firstAssignment as? Literal<*>)?.value)
        }
    }

    @Test
    fun testFunctionDeclaration() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("function.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["function"]
        assertNotNull(p)

        val foo = p.declarations.firstOrNull()
        assertIs<FunctionDeclaration>(foo)

        val bar = p.declarations[1]
        assertIs<FunctionDeclaration>(bar)
        assertEquals(2, bar.parameters.size)

        val fooBody = foo.body
        assertIs<Block>(fooBody)
        var callExpression = fooBody.statements[0]
        assertIs<CallExpression>(callExpression)

        assertLocalName("bar", callExpression)
        assertEquals(bar, callExpression.invokes.firstOrNull())

        val edge = callExpression.argumentEdges[1]
        assertNotNull(edge)
        assertEquals("s2", edge.name)

        val s = bar.parameters.firstOrNull()
        assertNotNull(s)
        assertLocalName("s", s)
        assertEquals(tu.primitiveType("str"), s.type)

        assertLocalName("bar", bar)

        val compStmt = bar.body
        assertIs<Block>(compStmt)
        assertNotNull(compStmt.statements)

        callExpression = compStmt.statements[0]
        assertIs<CallExpression>(callExpression)

        assertFullName("print", callExpression)

        val literal = callExpression.arguments.firstOrNull()
        assertIs<Literal<*>>(literal)

        assertEquals("bar(s) here: ", literal.value)
        assertEquals(tu.primitiveType("str"), literal.type)

        val ref = callExpression.arguments[1]
        assertIs<Reference>(ref)

        assertLocalName("s", ref)
        assertRefersTo(ref, s)

        val stmt = compStmt.statements[1]
        assertIs<AssignExpression>(stmt)

        val a = stmt.declarations.firstOrNull()
        assertNotNull(a)

        assertLocalName("a", a)

        val op = a.firstAssignment
        assertIs<BinaryOperator>(op)

        assertEquals("+", op.operatorCode)

        val lhs = op.lhs
        assertIs<Literal<*>>(lhs)

        assertEquals(1, (lhs.value as? Long)?.toInt())

        val rhs = op.rhs
        assertIs<Literal<*>>(rhs)

        assertEquals(2, (rhs.value as? Long)?.toInt())

        val r = compStmt.statements[3]
        assertIs<ReturnStatement>(r)

        val s3 = tu.variables["s3"]
        assertNotNull(s3)
        assertLocalName("str", s3.type)

        val baz = tu.functions["baz"]
        assertNotNull(baz)
        assertLocalName("str", baz.returnTypes.singleOrNull())
    }

    @Test
    fun testIf() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("if.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["if"]
        val main = p.functions["foo"]
        assertNotNull(main)

        val body = main.body
        assertIs<Block>(body)

        val sel = (body.statements.firstOrNull() as? AssignExpression)?.declarations?.firstOrNull()
        assertNotNull(sel)
        assertLocalName("sel", sel)
        assertEquals(tu.primitiveType("bool"), sel.type)

        val firstAssignment = sel.firstAssignment
        assertIs<Literal<*>>(firstAssignment)
        assertEquals(tu.primitiveType("bool"), firstAssignment.type)
        assertEquals("True", firstAssignment.code)

        val `if` = body.statements[1]
        assertIs<IfStatement>(`if`)
    }

    @Test
    fun testSimpleClass() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("simple_class.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["simple_class"]
        assertNotNull(p)

        val cls = p.records["SomeClass"]
        assertNotNull(cls)

        val foo = p.functions["foo"]
        assertNotNull(foo)

        assertLocalName("SomeClass", cls)
        assertEquals(1, cls.methods.size)

        val clsfunc = cls.methods.firstOrNull()
        assertLocalName("someFunc", clsfunc)

        assertLocalName("foo", foo)
        val body = foo.body
        assertIs<Block>(body)
        assertNotNull(body.statements)
        assertEquals(2, body.statements.size)

        val s1 = body.statements[0]
        assertIs<AssignExpression>(s1)
        val s2 = body.statements[1]
        assertIs<MemberCallExpression>(s2)

        val c1 = s1.declarations.firstOrNull()
        assertNotNull(c1)
        assertLocalName("c1", c1)
        val ctor = c1.firstAssignment
        assertIs<ConstructExpression>(ctor)
        assertEquals(ctor.constructor, cls.constructors.firstOrNull())
        assertFullName("simple_class.SomeClass", c1.type)

        assertRefersTo((s2.base as? Reference), c1)
        assertEquals(1, s2.invokes.size)
        assertEquals(clsfunc, s2.invokes.firstOrNull())

        // member
    }

    @Test
    fun testIfExpr() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("ifexpr.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["ifexpr"]
        val main = p.functions["foo"]
        assertNotNull(main)

        val assignExpr = (main.body as? Block)?.statements?.firstOrNull()
        assertIs<AssignExpression>(assignExpr)

        val foo = assignExpr.declarations.firstOrNull()
        assertNotNull(foo)
        assertLocalName("foo", foo)
        assertEquals(tu.primitiveType("int"), foo.type)

        val initializer = foo.firstAssignment
        assertIs<ConditionalExpression>(initializer)
        assertEquals(tu.primitiveType("int"), initializer.type)

        val ifCond = initializer.condition
        assertIs<Literal<*>>(ifCond)
        val thenExpr = initializer.thenExpression
        assertIs<Literal<*>>(thenExpr)
        val elseExpr = initializer.elseExpression
        assertIs<Literal<*>>(elseExpr)

        assertEquals(tu.primitiveType("bool"), ifCond.type)
        assertEquals(false, ifCond.value)

        assertEquals(tu.primitiveType("int"), thenExpr.type)
        assertEquals(21, (thenExpr.value as? Long)?.toInt())

        assertEquals(tu.primitiveType("int"), elseExpr.type)
        assertEquals(42, (elseExpr.value as? Long)?.toInt())
    }

    @Test
    fun testFields() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("class_fields.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["class_fields"]
        val recordFoo = p.records["Foo"]
        assertNotNull(recordFoo)
        assertLocalName("Foo", recordFoo)
        assertEquals(4, recordFoo.fields.size)
        assertEquals(1, recordFoo.methods.size)

        // TODO: When developing the new python frontend, remove the type specifier from the field
        //   again and check if the field still occurs. It's absolutely not clear to me who would be
        //   responsible for adding it but IMHO it should be the frontend. This, however, is
        //   currently not the case.
        val fieldX = recordFoo.fields["x"]
        assertNotNull(fieldX)

        val fieldY = recordFoo.fields["y"]
        assertNotNull(fieldY)

        val fieldZ = recordFoo.fields["z"]
        assertNotNull(fieldZ)

        val fieldBaz = recordFoo.fields["baz"]
        assertNotNull(fieldBaz)

        assertLocalName("x", fieldX)
        assertLocalName("y", fieldY)
        assertLocalName("z", fieldZ)
        assertLocalName("baz", fieldBaz)

        assertNull(fieldX.firstAssignment)
        assertNotNull(fieldY.firstAssignment)
        assertNull(fieldZ.firstAssignment)
        assertNotNull(fieldBaz.firstAssignment)

        val methBar = recordFoo.methods[0]
        assertNotNull(methBar)
        assertLocalName("bar", methBar)

        val barZ = (methBar.body as? Block)?.statements?.get(0)
        assertIs<MemberExpression>(barZ)
        assertRefersTo(barZ, fieldZ)

        val barBaz = (methBar.body as? Block)?.statements?.get(1)
        assertIs<AssignExpression>(barBaz)
        val barBazInner = barBaz.declarations[0]
        assertIs<FieldDeclaration>(barBazInner)
        assertLocalName("baz", barBazInner)
        assertNotNull(barBazInner.firstAssignment)
    }

    @Test
    fun testSelf() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("class_self.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val recordFoo = tu.records["class_self.Foo"]
        assertNotNull(recordFoo)
        assertLocalName("Foo", recordFoo)

        assertEquals(1, recordFoo.fields.size)
        val somevar = recordFoo.fields["somevar"]
        assertNotNull(somevar)
        assertLocalName("somevar", somevar)
        // assertEquals(tu.parseType("int", false), somevar.type) TODO fix type deduction

        assertEquals(2, recordFoo.methods.size)
        val bar = recordFoo.methods[0]
        val foo = recordFoo.methods[1]
        assertNotNull(bar)
        assertNotNull(foo)
        assertLocalName("bar", bar)
        assertEquals(recordFoo, bar.recordDeclaration)
        assertLocalName("foo", foo)
        assertEquals(recordFoo, foo.recordDeclaration)

        val recv = bar.receiver
        assertNotNull(recv)
        assertLocalName("self", recv)
        assertFullName("class_self.Foo", recv.type)

        assertEquals(1, bar.parameters.size)
        val i = bar.parameters[0]
        assertNotNull(i)

        assertLocalName("i", i)
        // assertEquals(tu.primitiveType("int"), i.type)

        // self.somevar = i
        val someVarDeclaration =
            ((bar.body as? Block)?.statements?.get(0) as? AssignExpression)
                ?.declarations
                ?.firstOrNull()
        assertIs<FieldDeclaration>(someVarDeclaration)
        assertLocalName("somevar", someVarDeclaration)
        assertRefersTo((someVarDeclaration.firstAssignment as? Reference), i)

        val fooMemCall = (foo.body as? Block)?.statements?.get(0)
        assertIs<MemberCallExpression>(fooMemCall)

        val mem = fooMemCall.callee
        assertIs<MemberExpression>(mem)
        assertLocalName("bar", mem)
        assertEquals(".", fooMemCall.operatorCode)
        assertFullName("class_self.Foo.bar", fooMemCall)
        assertEquals(1, fooMemCall.invokes.size)
        assertEquals(bar, fooMemCall.invokes[0])
        assertLocalName("self", fooMemCall.base)
    }

    @Test
    fun testClassTypeAnnotations() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("class_type_annotations.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val other = tu.records["Other"]
        assertNotNull(other)
        assertFullName("class_type_annotations.Other", other.toType())

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        assertFullName("class_type_annotations.Foo", foo.toType())

        val fromOther = tu.functions["from_other"]
        assertNotNull(fromOther)

        val paramType = fromOther.parameters.firstOrNull()?.type
        assertNotNull(paramType)
        assertEquals(other.toType(), paramType)
    }

    @Test
    fun testCtor() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("class_ctor.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["class_ctor"]
        assertNotNull(p)

        val recordFoo = p.records["Foo"]
        assertNotNull(recordFoo)
        assertLocalName("Foo", recordFoo)

        assertEquals(1, recordFoo.methods.size)
        assertEquals(1, recordFoo.constructors.size)
        val fooCtor = recordFoo.constructors[0]
        assertNotNull(fooCtor)
        val foobar = recordFoo.methods[0]
        assertNotNull(foobar)

        assertLocalName("__init__", fooCtor)
        assertLocalName("foobar", foobar)

        val bar = p.functions["bar"]
        assertNotNull(bar)
        assertLocalName("bar", bar)

        assertEquals(2, (bar.body as? Block)?.statements?.size)
        val line1 = (bar.body as? Block)?.statements?.get(0)
        assertIs<AssignExpression>(line1)
        val line2 = (bar.body as? Block)?.statements?.get(1)
        assertIs<MemberCallExpression>(line2)

        assertEquals(1, line1.declarations.size)
        val fooDecl = line1.declarations[0]
        assertNotNull(fooDecl)
        assertLocalName("foo", fooDecl)
        assertFullName("class_ctor.Foo", fooDecl.type)
        val initializer = fooDecl.firstAssignment as? ConstructExpression
        assertEquals(fooCtor, initializer?.constructor)

        assertRefersTo((line2.base as? Reference), fooDecl)
        assertEquals(foobar, line2.invokes[0])
    }

    @Test
    fun testIssue432() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("issue432.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["issue432"]
        assertNotNull(p)

        val clsCounter = p.records["counter"]
        assertNotNull(clsCounter)

        val methCount = p.functions["count"]
        assertNotNull(methCount)

        val clsC1 = p.records["c1"]
        assertNotNull(clsC1)

        // class counter
        assertLocalName("counter", clsCounter)

        // TODO missing check for "pass"

        // def count(c)
        assertLocalName("count", methCount)
        assertEquals(1, methCount.parameters.size)

        val countParam = methCount.parameters[0]
        assertNotNull(countParam)
        assertLocalName("c", countParam)

        val countStmt = (methCount.body as? Block)?.statements?.get(0)
        assertIs<IfStatement>(countStmt)

        val ifCond = countStmt.condition
        assertIs<BinaryOperator>(ifCond)

        val lhs = ifCond.lhs
        assertIs<MemberCallExpression>(lhs)
        assertRefersTo((lhs.base as? Reference), countParam)
        assertLocalName("inc", lhs)
        assertEquals(0, lhs.arguments.size)

        val ifThen = (countStmt.thenStatement as? Block)?.statements?.get(0)
        assertIs<CallExpression>(ifThen)
        assertEquals(methCount, ifThen.invokes.firstOrNull())
        assertRefersTo((ifThen.arguments.firstOrNull() as? Reference), countParam)
        assertNull(countStmt.elseStatement)

        // class c1(counter)
        assertLocalName("c1", clsC1)
        assertEquals(
            clsCounter,
            (clsC1.superClasses.firstOrNull() as? ObjectType)?.recordDeclaration
        )
        assertEquals(1, clsC1.fields.size)

        val field = clsC1.fields[0]
        assertNotNull(field)
        assertLocalName("total", field)

        // TODO assert initializer "total = 0"

        val meth = clsC1.methods[0]
        assertNotNull(meth)
        assertLocalName("inc", meth)
        assertEquals(clsC1, meth.recordDeclaration)

        val selfReceiver = meth.receiver
        assertNotNull(selfReceiver)
        assertLocalName("self", selfReceiver)
        assertEquals(0, meth.parameters.size) // self is receiver and not a parameter

        val methBody = meth.body
        assertIs<Block>(methBody)

        val assign = methBody.statements[0]
        assertIs<AssignExpression>(assign)

        val assignLhs = assign.lhs<MemberExpression>()
        val assignRhs = assign.rhs<BinaryOperator>()
        assertEquals("=", assign.operatorCode)
        assertNotNull(assignLhs)
        assertNotNull(assignRhs)
        assertRefersTo((assignLhs.base as? Reference), selfReceiver)
        assertEquals("+", assignRhs.operatorCode)

        val assignRhsLhs =
            assignRhs.lhs
                as? MemberExpression // the second "self.total" in "self.total = self.total + 1"
        assertNotNull(assignRhsLhs)
        assertRefersTo((assignRhsLhs.base as? Reference), selfReceiver)

        val r = methBody.statements[1]
        assertIs<ReturnStatement>(r)
        assertRefersTo((r.returnValue as? MemberExpression)?.base as? Reference, selfReceiver)

        // TODO last line "count(c1())"
    }

    @Test
    fun testVarsAndFields() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("vars.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["vars"]
        assertNotNull(p)

        val clsFoo = p.records["Foo"]
        assertNotNull(clsFoo)

        val methBar = clsFoo.methods[0]
        assertNotNull(methBar)

        // val stmtsOutsideCls TODO
        val classFieldNoInitializer = clsFoo.fields["classFieldNoInitializer"]
        assertNotNull(classFieldNoInitializer)

        val classFieldWithInit = clsFoo.fields["classFieldWithInit"]
        assertNotNull(classFieldWithInit)

        val classFieldDeclaredInFunction = clsFoo.fields["classFieldDeclaredInFunction"]
        assertNotNull(classFieldDeclaredInFunction)
        // assertEquals(3, clsFoo.fields.size) // TODO should "self" be considered a field here?

        assertNull(classFieldNoInitializer.initializer)
        assertNotNull(classFieldWithInit)

        // classFieldNoInitializer = classFieldWithInit
        val assignClsFieldOutsideFunc = clsFoo.statements[2]
        assertIs<AssignExpression>(assignClsFieldOutsideFunc)
        assertRefersTo(assignClsFieldOutsideFunc.lhs<Reference>(), classFieldNoInitializer)
        assertRefersTo((assignClsFieldOutsideFunc.rhs<Reference>()), classFieldWithInit)
        assertEquals("=", assignClsFieldOutsideFunc.operatorCode)

        val barBody = methBar.body
        assertIs<Block>(barBody)

        // self.classFieldDeclaredInFunction = 456
        val barStmt0 = barBody.statements[0]
        assertIs<AssignExpression>(barStmt0)
        val decl0 = barStmt0.declarations[0]
        assertIs<FieldDeclaration>(decl0)
        assertLocalName("classFieldDeclaredInFunction", decl0)
        assertNotNull(decl0.firstAssignment)

        // self.classFieldNoInitializer = 789
        val barStmt1 = barBody.statements[1]
        assertIs<AssignExpression>(barStmt1)
        assertRefersTo((barStmt1.lhs<MemberExpression>()), classFieldNoInitializer)

        // self.classFieldWithInit = 12
        val barStmt2 = barBody.statements[2]
        assertIs<AssignExpression>(barStmt2)
        assertRefersTo((barStmt2.lhs<MemberExpression>()), classFieldWithInit)

        // classFieldNoInitializer = "shadowed"
        val barStmt3 = barBody.statements[3]
        assertIs<AssignExpression>(barStmt3)
        assertEquals("=", barStmt3.operatorCode)
        assertRefersTo((barStmt3.lhs<Reference>()), classFieldNoInitializer)
        assertEquals("shadowed", (barStmt3.rhs<Literal<*>>())?.value)

        // classFieldWithInit = "shadowed"
        val barStmt4 = barBody.statements[4]
        assertIs<AssignExpression>(barStmt4)
        assertEquals("=", barStmt4.operatorCode)
        assertRefersTo((barStmt4.lhs<Reference>()), classFieldWithInit)
        assertEquals("shadowed", (barStmt4.rhs<Literal<*>>())?.value)

        // classFieldDeclaredInFunction = "shadowed"
        val barStmt5 = barBody.statements[5]
        assertIs<AssignExpression>(barStmt5)
        assertEquals("=", barStmt5.operatorCode)
        assertRefersTo((barStmt5.lhs<Reference>()), classFieldDeclaredInFunction)
        assertEquals("shadowed", (barStmt5.rhs<Literal<*>>())?.value)

        /* TODO:
        foo = Foo()
        foo.classFieldNoInitializer = 345
        foo.classFieldWithInit = 678
         */
    }

    @Test
    fun testRegionInCPG() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("literal.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["literal"]
        assertNotNull(p)

        assertEquals(Region(1, 1, 1, 9), (p.statements[0]).location?.region)
        assertEquals(Region(1, 5, 1, 9), (p.variables["b"])?.firstAssignment?.location?.region)
        assertEquals(Region(2, 1, 2, 7), (p.statements[1]).location?.region)
        assertEquals(Region(3, 1, 3, 8), (p.statements[2]).location?.region)
        assertEquals(Region(4, 1, 4, 11), (p.statements[3]).location?.region)
        assertEquals(Region(5, 1, 5, 12), (p.statements[4]).location?.region)
        assertEquals(Region(6, 1, 6, 9), (p.statements[5]).location?.region)
    }

    @Test
    fun testMultiLevelMemberCall() { // TODO
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("multi_level_mem_call.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["multi_level_mem_call"]
        assertNotNull(p)

        // foo = bar.baz.zzz("hello")
        val foo = p.variables["foo"]
        assertNotNull(foo)

        val firstAssignment = foo.firstAssignment
        assertIs<MemberCallExpression>(firstAssignment)

        assertLocalName("zzz", firstAssignment)
        val base = firstAssignment.base
        assertIs<MemberExpression>(base)
        assertLocalName("baz", base)
        val baseBase = base.base
        assertIs<Reference>(baseBase)
        assertLocalName("bar", baseBase)

        val memberExpression = firstAssignment.callee
        assertIs<MemberExpression>(memberExpression)
        assertLocalName("zzz", memberExpression)
    }

    @Test
    fun testIssue598() { // test for added functionality: "while" and "break"
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("issue598.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["issue598"]
        assertNotNull(p)

        val main = p.functions["main"]
        assertNotNull(main)

        val mainBody = main.body
        assertIs<Block>(mainBody)

        val whlStmt = mainBody.statements[3]
        assertIs<WhileStatement>(whlStmt)

        val whlBody = whlStmt.statement
        assertIs<Block>(whlBody)

        val xDeclaration = whlBody.statements[0]
        assertIs<AssignExpression>(xDeclaration)

        val ifStatement = whlBody.statements[1]
        assertIs<IfStatement>(ifStatement)

        val brk = ifStatement.elseStatement
        assertIs<Block>(brk)
        assertIs<BreakStatement>(brk.statements[0])
    }

    @Test
    fun testIssue615() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("issue615.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["issue615"]
        assertNotNull(p)

        assertEquals(
            5,
            p.variables.size
        ) // including one dummy variable introduced for the loop var
        assertEquals(
            4,
            p.variables.filter { !it.name.localName.contains(PythonHandler.LOOP_VAR_PREFIX) }.size
        )
        assertEquals(2, p.statements.size)

        // test = [(1, 2, 3)]
        val testDeclaration = p.variables[0]
        assertNotNull(testDeclaration)
        assertLocalName("test", testDeclaration)
        val testDeclStmt = p.statements[0]
        assertIs<AssignExpression>(testDeclStmt)

        /* for loop:
        for t1, t2, t3 in test:
            print("bug ... {} {} {}".format(t1, t2, t3))
         */
        val forStmt = p.statements[1]
        assertIs<ForEachStatement>(forStmt)

        val forVariable = forStmt.variable
        assertIs<Reference>(forVariable)
        val forVarDecl =
            p.declarations.firstOrNull {
                it.name.localName.contains((PythonHandler.LOOP_VAR_PREFIX))
            }
        assertNotNull(forVarDecl)
        assertRefersTo(forVariable, forVarDecl)

        val iter = forStmt.iterable
        assertIs<Reference>(iter)
        assertRefersTo(iter, testDeclaration)

        val forBody = forStmt.statement
        assertIs<Block>(forBody)
        assertEquals(2, forBody.statements.size) // loop var assign and print stmt

        /*
        We model the 3 loop variables

        ```
        for t1, t2, t3 in ...
        ```

        implicitly as follows:

        ```
        for tempVar in ...:
          t1, t2, t3 = tempVar
          rest of the loop
        ```
         */
        val forVariableImplicitStmt = forBody.statements.firstOrNull()
        assertIs<AssignExpression>(forVariableImplicitStmt)
        assertEquals("=", forVariableImplicitStmt.operatorCode)
        assertEquals(forStmt.variable, forVariableImplicitStmt.rhs.firstOrNull())
        val (t1Decl, t2Decl, t3Decl) = forVariableImplicitStmt.declarations
        val (t1RefAssign, t2RefAssign, t3RefAssign) = forVariableImplicitStmt.lhs
        assertNotNull(t1Decl)
        assertNotNull(t2Decl)
        assertNotNull(t3Decl)
        assertIs<Reference>(t1RefAssign)
        assertIs<Reference>(t2RefAssign)
        assertIs<Reference>(t3RefAssign)
        assertRefersTo(t1RefAssign, t1Decl)
        assertRefersTo(t2RefAssign, t2Decl)
        assertRefersTo(t3RefAssign, t3Decl)

        // print("bug ... {} {} {}".format(t1, t2, t3))
        val forBodyStmt = forBody.statements<CallExpression>(1)
        assertNotNull(forBodyStmt)
        assertLocalName("print", forBodyStmt)

        val printArg = forBodyStmt.arguments[0]
        assertIs<MemberCallExpression>(printArg)
        val formatArgT1 = printArg.arguments[0]
        assertIs<Reference>(formatArgT1)
        assertRefersTo(formatArgT1, t1Decl)
        val formatArgT2 = printArg.arguments[1]
        assertIs<Reference>(formatArgT2)
        assertRefersTo(formatArgT2, t2Decl)
        val formatArgT3 = printArg.arguments[2]
        assertIs<Reference>(formatArgT3)
        assertRefersTo(formatArgT3, t3Decl)
    }

    @Test
    fun testIssue473() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("issue473.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["issue473"]
        assertNotNull(p)

        val ifStatement = p.statements[0]
        assertIs<IfStatement>(ifStatement)
        val ifCond = ifStatement.condition
        assertIs<BinaryOperator>(ifCond)
        val ifThen = ifStatement.thenStatement
        assertIs<Block>(ifThen)
        val ifElse = ifStatement.elseStatement
        assertIs<Block>(ifElse)

        // sys.version_info.minor > 9
        assertEquals(">", ifCond.operatorCode)
        assertLocalName("minor", ifCond.lhs as? Reference)

        // phr = {"user_id": user_id} | content
        val phrDeclaration = (ifThen.statements[0] as? AssignExpression)?.declarations?.get(0)

        assertNotNull(phrDeclaration)
        assertLocalName("phr", phrDeclaration)
        val phrInitializer = phrDeclaration.firstAssignment
        assertIs<BinaryOperator>(phrInitializer)
        assertEquals("|", phrInitializer.operatorCode)
        assertEquals(true, phrInitializer.lhs is InitializerListExpression)

        // z = {"user_id": user_id}
        val elseStmt1 = (ifElse.statements[0] as? AssignExpression)?.declarations?.get(0)
        assertNotNull(elseStmt1)
        assertLocalName("z", elseStmt1)

        // phr = {**z, **content}
        val elseStmt2 = ifElse.statements<AssignExpression>(1)
        assertNotNull(elseStmt2)
        assertEquals("=", elseStmt2.operatorCode)
        val elseStmt2Rhs = elseStmt2.rhs<InitializerListExpression>()
        assertNotNull(elseStmt2Rhs)
    }

    @Test
    fun testCommentMatching() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("comments.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>().matchCommentsToNodes(true)
            }
        assertNotNull(tu)

        val commentedNodes = SubgraphWalker.flattenAST(tu).filter { it.comment != null }

        assertEquals(9, commentedNodes.size)

        val functions = commentedNodes.filterIsInstance<FunctionDeclaration>()
        assertEquals(1, functions.size)
        assertEquals(
            "# a function",
            functions.firstOrNull()?.comment,
        )

        val literals = commentedNodes.filterIsInstance<Literal<String>>()
        assertEquals(1, literals.size)
        assertEquals("# comment start", literals.firstOrNull()?.comment)

        val params = commentedNodes.filterIsInstance<ParameterDeclaration>()
        assertEquals(2, params.size)
        assertEquals("# a parameter", params.first { it.name.localName == "i" }.comment)
        assertEquals("# another parameter", params.first { it.name.localName == "j" }.comment)

        val assignment = commentedNodes.filterIsInstance<AssignExpression>()
        assertEquals(2, assignment.size)
        assertEquals("# A comment# a number", assignment.firstOrNull()?.comment)
        assertEquals("# comment end", assignment.last().comment)

        val block = commentedNodes.filterIsInstance<Block>()
        assertEquals(1, block.size)
        assertEquals("# foo", block.firstOrNull()?.comment)

        val kvs = commentedNodes.filterIsInstance<KeyValueExpression>()
        assertEquals(2, kvs.size)
        assertEquals("# a entry", kvs.first { it.code?.contains("a") == true }.comment)
        assertEquals("# b entry", kvs.first { it.code?.contains("b") == true }.comment)
    }

    @Test
    fun testAnnotations() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("annotations.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>().matchCommentsToNodes(true)
            }
        assertNotNull(tu)

        val annotations = tu.allChildren<Annotation>()
        val route = annotations.firstOrNull()
        assertFullName("app.route", route)
    }

    @Test
    fun testForLoop() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("forloop.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val forloopFunc = tu.functions["forloop"]
        assertNotNull(forloopFunc)

        val varDefinedBeforeLoop = forloopFunc.variables["varDefinedBeforeLoop"]
        assertNotNull(varDefinedBeforeLoop)

        val varDefinedInLoop = forloopFunc.variables["varDefinedInLoop"]
        assertNotNull(varDefinedInLoop)

        val functionBody = forloopFunc.body
        assertIs<Block>(functionBody)

        val firstLoop = functionBody.statements[1]
        assertIs<ForEachStatement>(firstLoop)

        val secondLoop = functionBody.statements[2]
        assertIs<ForEachStatement>(secondLoop)

        val fooCall = functionBody.statements[3]
        assertIs<CallExpression>(fooCall)

        val barCall = functionBody.statements[4]
        assertIs<CallExpression>(barCall)

        val varDefinedBeforeLoopRef =
            (functionBody.statements.firstOrNull() as? AssignExpression)?.lhs?.firstOrNull()
                as? Reference ?: TODO()
        // no dataflow from var declaration to loop variable because it's a write access
        assert((firstLoop.variable?.prevDFG?.contains(varDefinedBeforeLoopRef) == false))

        // dataflow from range call to loop variable
        val firstLoopIterable = firstLoop.iterable
        assertIs<CallExpression>(firstLoopIterable)
        assert((firstLoop.variable?.prevDFG?.contains((firstLoopIterable)) == true))

        // dataflow from var declaration to loop iterable call
        assert(
            firstLoopIterable.arguments.firstOrNull()?.prevDFG?.contains(varDefinedBeforeLoopRef) ==
                true
        )

        // dataflow from first loop to foo call
        val loopVar = firstLoop.variable
        assertIs<Reference>(loopVar)
        assertTrue(fooCall.arguments.firstOrNull()?.prevDFG?.contains(loopVar) == true)

        // dataflow from var declaration to foo call (in case for loop is not executed)
        assert(fooCall.arguments.firstOrNull()?.prevDFG?.contains(varDefinedBeforeLoopRef) == true)

        // dataflow from range call to loop variable
        val secondLoopIterable = secondLoop.iterable
        assertIs<CallExpression>(secondLoopIterable)
        assert(
            ((secondLoop.variable as? Reference)?.prevDFG?.contains((secondLoopIterable)) == true)
        )

        // dataflow from second loop var to bar call
        assertEquals(
            (secondLoop.variable as? Reference),
            barCall.arguments.firstOrNull()?.prevDFG?.firstOrNull()
        )
    }

    @Test
    fun testArithmetics() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("calc.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val a = tu.refs["a"]
        assertNotNull(a)

        val result = a.evaluate(PythonValueEvaluator())
        assertEquals(16.0, result)

        val bAugAssign =
            tu.allChildren<AssignExpression>().singleOrNull {
                (it.lhs.singleOrNull() as? Reference)?.name?.localName == "b" &&
                    it.location?.region?.startLine == 4
            }
        assertNotNull(bAugAssign)
        assertEquals("*=", bAugAssign.operatorCode)
        assertEquals("b", bAugAssign.lhs.singleOrNull()?.name?.localName)
        assertEquals(2L, (bAugAssign.rhs.singleOrNull() as? Literal<*>)?.value)

        // c = (not True and False) or True
        val cAssign =
            tu.allChildren<AssignExpression>()
                .singleOrNull { (it.lhs.singleOrNull() as? Reference)?.name?.localName == "c" }
                ?.rhs
                ?.singleOrNull()
        assertIs<BinaryOperator>(cAssign)
        assertEquals("or", cAssign.operatorCode)
        assertEquals(true, (cAssign.rhs as? Literal<*>)?.value)
        assertEquals("and", (cAssign.lhs as? BinaryOperator)?.operatorCode)
        assertEquals(false, ((cAssign.lhs as? BinaryOperator)?.rhs as? Literal<*>)?.value)
        assertEquals("not", ((cAssign.lhs as? BinaryOperator)?.lhs as? UnaryOperator)?.operatorCode)
        assertEquals(
            true,
            (((cAssign.lhs as? BinaryOperator)?.lhs as? UnaryOperator)?.input as? Literal<*>)?.value
        )

        // d = ((-5 >> 2) & ~7 | (+4 << 1)) ^ 0xffff
        val dAssign =
            tu.allChildren<AssignExpression>()
                .singleOrNull { (it.lhs.singleOrNull() as? Reference)?.name?.localName == "d" }
                ?.rhs
                ?.singleOrNull()
        assertIs<BinaryOperator>(dAssign)
        assertEquals("^", dAssign.operatorCode)
        assertEquals(0xffffL, (dAssign.rhs as? Literal<*>)?.value)
        assertEquals("|", (dAssign.lhs as? BinaryOperator)?.operatorCode)
        assertEquals("<<", ((dAssign.lhs as? BinaryOperator)?.rhs as? BinaryOperator)?.operatorCode)
        assertEquals(
            1L,
            (((dAssign.lhs as? BinaryOperator)?.rhs as? BinaryOperator)?.rhs as? Literal<*>)?.value
        )
        assertEquals(
            "+",
            (((dAssign.lhs as? BinaryOperator)?.rhs as? BinaryOperator)?.lhs as? UnaryOperator)
                ?.operatorCode
        )
        assertEquals(
            4L,
            ((((dAssign.lhs as? BinaryOperator)?.rhs as? BinaryOperator)?.lhs as? UnaryOperator)
                    ?.input as? Literal<*>)
                ?.value
        )
        val dAssignLhsOfOr = (dAssign.lhs as? BinaryOperator)?.lhs
        assertIs<BinaryOperator>(dAssignLhsOfOr)
        assertEquals("&", dAssignLhsOfOr.operatorCode)
        assertEquals("~", (dAssignLhsOfOr.rhs as? UnaryOperator)?.operatorCode)
        assertEquals(7L, ((dAssignLhsOfOr.rhs as? UnaryOperator)?.input as? Literal<*>)?.value)
        assertEquals(">>", (dAssignLhsOfOr.lhs as? BinaryOperator)?.operatorCode)
        assertEquals(2L, ((dAssignLhsOfOr.lhs as? BinaryOperator)?.rhs as? Literal<*>)?.value)
        assertEquals(
            "-",
            ((dAssignLhsOfOr.lhs as? BinaryOperator)?.lhs as? UnaryOperator)?.operatorCode
        )
        assertEquals(
            5L,
            (((dAssignLhsOfOr.lhs as? BinaryOperator)?.lhs as? UnaryOperator)?.input as? Literal<*>)
                ?.value
        )
    }

    @Test
    fun testDataTypes() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("datatypes.py").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)
        val namespace = tu.namespaces.singleOrNull()
        assertNotNull(namespace)
        val aStmt = namespace.statements[0]
        assertIs<AssignExpression>(aStmt)
        assertEquals(
            "list",
            (aStmt.rhs.singleOrNull() as? InitializerListExpression)?.type?.name?.localName
        )
        val bStmt = namespace.statements[1]
        assertIs<AssignExpression>(bStmt)
        assertEquals(
            "set",
            (bStmt.rhs.singleOrNull() as? InitializerListExpression)?.type?.name?.localName
        )
        val cStmt = namespace.statements[2]
        assertIs<AssignExpression>(cStmt)
        assertEquals(
            "tuple",
            (cStmt.rhs.singleOrNull() as? InitializerListExpression)?.type?.name?.localName
        )
        val dStmt = namespace.statements[3]
        assertIs<AssignExpression>(dStmt)
        assertEquals(
            "dict",
            (dStmt.rhs.singleOrNull() as? InitializerListExpression)?.type?.name?.localName
        )

        val eStmtRhs = (namespace.statements[4] as? AssignExpression)?.rhs?.singleOrNull()
        assertIs<BinaryOperator>(eStmtRhs)
        assertEquals("Values of a: ", (eStmtRhs.lhs as? Literal<*>)?.value)
        val eStmtRhsRhs = (eStmtRhs.rhs as? BinaryOperator)
        assertNotNull(eStmtRhsRhs)
        val aRef = eStmtRhsRhs.lhs as? Reference
        assertEquals("a", aRef?.name?.localName)
        val eStmtRhsRhsRhs = (eStmtRhsRhs.rhs as? BinaryOperator)
        assertEquals(" and b: ", (eStmtRhsRhsRhs?.lhs as? Literal<*>)?.value)
        val bCall = eStmtRhsRhsRhs?.rhs as? CallExpression
        assertEquals("str", bCall?.name?.localName)
        assertEquals("b", bCall?.arguments?.singleOrNull()?.name?.localName)

        val fStmtRhs = (namespace.statements[5] as? AssignExpression)?.rhs?.singleOrNull()

        assertIs<SubscriptExpression>(fStmtRhs)
        assertEquals("a", fStmtRhs.arrayExpression.name.localName)
        assertTrue(fStmtRhs.subscriptExpression is RangeExpression)
        assertEquals(
            1L,
            ((fStmtRhs.subscriptExpression as RangeExpression).floor as? Literal<*>)?.value
        )
        assertEquals(
            3L,
            ((fStmtRhs.subscriptExpression as RangeExpression).ceiling as? Literal<*>)?.value
        )
        assertEquals(
            2L,
            ((fStmtRhs.subscriptExpression as RangeExpression).third as? Literal<*>)?.value
        )
    }

    @Test
    fun testSimpleImport() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("simple_import.py").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        assertEquals(2, result.variables.size)
        // Note, that "pi" is incorrectly inferred as a field declaration. This is a known bug in
        // the inference system (and not in the python module) and will be handled separately.
        assertEquals(listOf("mypi", "pi"), result.variables.map { it.name.localName })
    }

    @Test
    fun testModules() {
        val topLevel = Path.of("src", "test", "resources", "python", "modules")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("a.py").toFile(),
                    topLevel.resolve("b.py").toFile(),
                    topLevel.resolve("c.py").toFile(),
                    topLevel.resolve("main.py").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val aFunc = result.functions["a.func"]
        assertNotNull(aFunc)

        val bFunc = result.functions["b.func"]
        assertNotNull(bFunc)

        val cCompletelyDifferentFunc = result.functions["c.completely_different_func"]
        assertNotNull(cCompletelyDifferentFunc)

        var call = result.calls["a.func"]
        assertNotNull(call)
        assertInvokes(call, aFunc)

        call = result.calls["a_func"]
        assertNotNull(call)
        assertInvokes(call, aFunc)

        call =
            result.calls[
                    { // we need to do select it this way otherwise we will also match "a.func"
                        it.name.toString() == "func"
                    }]
        assertNotNull(call)
        assertInvokes(call, bFunc)

        call = result.calls["completely_different_func"]
        assertNotNull(call)
        assertInvokes(call, cCompletelyDifferentFunc)

        call = result.calls["different.completely_different_func"]
        assertNotNull(call)
        assertInvokes(call, cCompletelyDifferentFunc)
    }

    @Test
    fun testInterfaceStubs() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("complex_class.pyi").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        with(result) {
            val foo = records["Foo"]
            assertNotNull(foo)

            val bar = foo.methods["bar"]
            assertNotNull(bar)

            assertEquals(assertResolvedType("int"), bar.returnTypes.singleOrNull())
            assertEquals(assertResolvedType("int"), bar.parameters.firstOrNull()?.type)
            assertEquals(assertResolvedType("complex_class.Foo"), bar.receiver?.type)
        }
    }

    @Test
    fun testNamedExpression() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("named_expressions.py").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        val namedExpression = result.functions["named_expression"]
        assertNotNull(namedExpression)

        val assignExpression = result.statements[1]
        assertIs<AssignExpression>(assignExpression)
        assertEquals(":=", assignExpression.operatorCode)
        assertEquals(true, assignExpression.usedAsExpression)

        val lhs = assignExpression.lhs.firstOrNull()
        assertIs<Reference>(lhs)

        val lhsVariable = lhs.refersTo
        assertIs<VariableDeclaration>(lhsVariable)
        assertLocalName("x", lhsVariable)

        val rhs = assignExpression.rhs.firstOrNull()
        assertIs<Literal<*>>(rhs)

        assertEquals(4.toLong(), rhs.evaluate())
    }

    class PythonValueEvaluator : ValueEvaluator() {
        override fun computeBinaryOpEffect(
            lhsValue: Any?,
            rhsValue: Any?,
            has: HasOperatorCode?,
        ): Any? {
            return if (has?.operatorCode == "**") {
                when {
                    lhsValue is Number && rhsValue is Number ->
                        lhsValue.toDouble().pow(rhsValue.toDouble())
                    else -> cannotEvaluate(has as Node, this)
                }
            } else {
                super.computeBinaryOpEffect(lhsValue, rhsValue, has)
            }
        }
    }
}
