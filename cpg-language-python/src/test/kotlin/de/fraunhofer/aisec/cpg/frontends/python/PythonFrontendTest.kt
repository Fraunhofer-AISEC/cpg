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

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.ancestors
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Annotation
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.*
import de.fraunhofer.aisec.cpg.graph.edges.scopes.ImportStyle
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.DynamicType
import de.fraunhofer.aisec.cpg.graph.types.ListType
import de.fraunhofer.aisec.cpg.graph.types.MapType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.SetType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.sarif.Region
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.*

class PythonFrontendTest : BaseTest() {

    @Test
    fun test1740EndlessCDG() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("1740_endless_cdg_loop.py").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
            }
        assertNotNull(tu)
    }

    @Test
    fun testNestedFunctions() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("nested_functions.py").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)
        // Check that all three functions exist
        val level1 = tu.functions["level1"]
        assertNotNull(level1)
        val level2 = tu.functions["level2"]
        assertNotNull(level2)
        val level3 = tu.functions["level3"]
        assertNotNull(level3)
        // Only level2 and level3 are children of level1
        assertEquals(setOf(level2, level3), level1.body.functions.toSet())
        // Only level3 is child of level2
        assertEquals(setOf(level3), level2.body.functions.toSet())
        // No child for level3
        assertEquals(setOf(), level3.body.functions.toSet())
    }

    @Test
    fun testLiteral() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("literal.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        with(result) {
            val p = namespaces["literal"]
            assertNotNull(p)
            assertLocalName("literal", p)

            val b = p.variables["b"]
            assertNotNull(b)
            assertLocalName("b", b)
            assertEquals(assertResolvedType("bool"), b.type)
            val bFirstAssignment = b.firstAssignment
            assertIs<Literal<*>>(bFirstAssignment)
            assertEquals(true, bFirstAssignment.value)

            val i = p.variables["i"]
            assertNotNull(i)
            assertLocalName("i", i)
            assertEquals(assertResolvedType("int"), i.type)
            val iFirstAssignment = i.firstAssignment
            assertIs<Literal<*>>(iFirstAssignment)
            assertEquals(42L, iFirstAssignment.value)

            val f = p.variables["f"]
            assertNotNull(f)
            assertLocalName("f", f)
            assertEquals(assertResolvedType("float"), f.type)
            val fFirstAssignment = f.firstAssignment
            assertIs<Literal<*>>(fFirstAssignment)
            assertEquals(1.0, fFirstAssignment.value)

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
            val tAssignment = t.firstAssignment
            assertIs<Literal<*>>(tAssignment)
            assertEquals("Hello", tAssignment.value)

            val n = p.variables["n"]
            assertNotNull(n)
            assertLocalName("n", n)
            assertEquals(assertResolvedType("None"), n.type)
            val nAssignment = n.firstAssignment
            assertIs<Literal<*>>(nAssignment)
            assertEquals(null, nAssignment.value)
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
        assertInvokes(callExpression, bar)

        val edge = callExpression.argumentEdges[1]
        assertNotNull(edge)
        assertEquals("s2", edge.name)

        val s = bar.parameters.firstOrNull()
        assertNotNull(s)
        assertLocalName("s", s)
        assertContains(s.assignedTypes, tu.primitiveType("str"))

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

        val lhsValue = lhs.value
        assertIs<Long>(lhsValue)
        assertEquals(1, lhsValue.toInt())

        val rhs = op.rhs
        assertIs<Literal<*>>(rhs)

        val rhsValue = rhs.value
        assertIs<Long>(rhsValue)
        assertEquals(2, rhsValue.toInt())

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

        val bodyFirstStmt = body.statements.firstOrNull()
        assertIs<AssignExpression>(bodyFirstStmt)
        val sel = bodyFirstStmt.declarations.firstOrNull()
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
                true,
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

        assertRefersTo(s2.base, c1)
        assertEquals(1, s2.invokes.size)
        assertInvokes(s2, clsfunc)

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

        val mainBody = main.body
        assertIs<Block>(mainBody)
        val assignExpr = mainBody.statements.firstOrNull()
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
        val thenValue = thenExpr.value
        assertIs<Long>(thenValue)
        assertEquals(21, thenValue.toInt())

        val elseValue = elseExpr.value
        assertIs<Long>(elseValue)
        assertEquals(tu.primitiveType("int"), elseExpr.type)
        assertEquals(42, elseValue.toInt())
    }

    @Test
    fun testFields() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("class_fields.py").toFile()),
                topLevel,
                true,
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

        val methBarBody = methBar.body
        assertIs<Block>(methBarBody)
        val barZ = methBarBody.statements[0]
        assertIs<MemberExpression>(barZ)
        assertRefersTo(barZ, fieldZ)

        val barBaz = methBarBody.statements[1]
        assertIs<AssignExpression>(barBaz)
        val barBazInner = recordFoo.fields("baz").firstOrNull()
        assertIs<FieldDeclaration>(barBazInner)
        assertNotNull(barBazInner.firstAssignment)
    }

    @Test
    fun testSelf() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("class_self.py").toFile()),
                topLevel,
                true,
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
        val barBody = bar.body
        assertIs<Block>(barBody)
        val barBodyFirstStmt = barBody.statements[0]
        assertIs<AssignExpression>(barBodyFirstStmt)
        val someVarDeclaration = recordFoo.variables.firstOrNull()
        assertIs<FieldDeclaration>(someVarDeclaration)
        assertLocalName("somevar", someVarDeclaration)
        assertRefersTo(someVarDeclaration.firstAssignment, i)

        val fooBody = foo.body
        assertIs<Block>(fooBody)
        val fooMemCall = fooBody.statements[0]
        assertIs<MemberCallExpression>(fooMemCall)

        val mem = fooMemCall.callee
        assertIs<MemberExpression>(mem)
        assertLocalName("bar", mem)
        assertEquals(".", fooMemCall.operatorCode)
        assertFullName("class_self.Foo.bar", fooMemCall)
        assertEquals(1, fooMemCall.invokes.size)
        assertInvokes(fooMemCall, bar)
        assertLocalName("self", fooMemCall.base)
    }

    @Test
    fun testClassTypeAnnotations() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("class_type_annotations.py").toFile()),
                topLevel,
                true,
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

        val param = fromOther.parameters.firstOrNull()
        assertNotNull(param)
        assertIs<DynamicType>(param.type)
        assertContains(param.assignedTypes, other.toType())
    }

    @Test
    fun testCtor() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("class_ctor.py").toFile()),
                topLevel,
                true,
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

        assertLocalName(PythonLanguage.IDENTIFIER_INIT, fooCtor)
        assertLocalName("foobar", foobar)

        val bar = p.functions["bar"]
        assertNotNull(bar)
        assertLocalName("bar", bar)

        val barBody = bar.body
        assertIs<Block>(barBody)

        assertEquals(2, barBody.statements.size)
        val line1 = barBody.statements[0]
        assertIs<AssignExpression>(line1)

        val line2 = barBody.statements[1]
        assertIs<MemberCallExpression>(line2)

        assertEquals(1, line1.declarations.size)
        val fooDecl = line1.declarations[0]
        assertNotNull(fooDecl)
        assertLocalName("foo", fooDecl)
        assertFullName("class_ctor.Foo", fooDecl.type)
        val initializer = fooDecl.firstAssignment
        assertIs<ConstructExpression>(initializer)
        assertEquals(fooCtor, initializer.constructor)

        assertRefersTo(line2.base, fooDecl)
        assertInvokes(line2, foobar)
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

        val methCountBody = methCount.body
        assertIs<Block>(methCountBody)

        val countStmt = methCountBody.statements[0]
        assertIs<IfStatement>(countStmt)

        val ifCond = countStmt.condition
        assertIs<BinaryOperator>(ifCond)

        val lhs = ifCond.lhs
        assertIs<MemberCallExpression>(lhs)
        assertRefersTo(lhs.base, countParam)
        assertLocalName("inc", lhs)
        assertEquals(0, lhs.arguments.size)

        val ifThenBody = countStmt.thenStatement
        assertIs<Block>(ifThenBody)
        val ifThenFirstStmt = ifThenBody.statements[0]
        assertIs<CallExpression>(ifThenFirstStmt)
        assertInvokes(ifThenFirstStmt, methCount)
        assertRefersTo(ifThenFirstStmt.arguments.firstOrNull(), countParam)
        assertNull(countStmt.elseStatement)

        // class c1(counter)
        assertLocalName("c1", clsC1)
        val cls1Super = clsC1.superClasses.firstOrNull()
        assertIs<ObjectType>(cls1Super)
        assertEquals(clsCounter, cls1Super.recordDeclaration)
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
        assertRefersTo(assignLhs.base, selfReceiver)
        assertEquals("+", assignRhs.operatorCode)

        val assignRhsLhs = assignRhs.lhs // the second "self.total" in "self.total = self.total + 1"
        assertIs<MemberExpression>(assignRhsLhs)
        assertRefersTo(assignRhsLhs.base, selfReceiver)

        val r = methBody.statements[1]
        assertIs<ReturnStatement>(r)
        val retVal = r.returnValue
        assertIs<MemberExpression>(retVal)
        assertRefersTo(retVal.base, selfReceiver)

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
        assertNull(classFieldNoInitializer.initializer)

        val localClassFieldNoInitializer =
            methBar.variables[
                    { it.name.localName == "classFieldNoInitializer" && it !is FieldDeclaration }]
        assertNotNull(localClassFieldNoInitializer)

        val localClassFieldWithInit =
            methBar.variables[
                    { it.name.localName == "classFieldWithInit" && it !is FieldDeclaration }]
        assertNotNull(localClassFieldNoInitializer)

        val localClassFieldDeclaredInFunction =
            methBar.variables[
                    {
                        it.name.localName == "classFieldDeclaredInFunction" &&
                            it !is FieldDeclaration
                    }]
        assertNotNull(localClassFieldNoInitializer)

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
        val decl0 = clsFoo.fields("classFieldDeclaredInFunction").firstOrNull()
        assertIs<FieldDeclaration>(decl0)
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
        assertRefersTo(barStmt3.lhs<Reference>(), localClassFieldNoInitializer)
        assertLiteralValue("shadowed", barStmt3.rhs<Literal<String>>())

        // classFieldWithInit = "shadowed"
        val barStmt4 = barBody.statements[4]
        assertIs<AssignExpression>(barStmt4)
        assertEquals("=", barStmt4.operatorCode)
        assertRefersTo(barStmt4.lhs<Reference>(), localClassFieldWithInit)
        assertLiteralValue("shadowed", (barStmt4.rhs<Literal<String>>()))

        // classFieldDeclaredInFunction = "shadowed"
        val barStmt5 = barBody.statements[5]
        assertIs<AssignExpression>(barStmt5)
        assertEquals("=", barStmt5.operatorCode)
        assertRefersTo((barStmt5.lhs<Reference>()), localClassFieldDeclaredInFunction)
        assertLiteralValue("shadowed", barStmt5.rhs<Literal<String>>())

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
                true,
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
            p.variables.size,
        ) // including one dummy variable introduced for the loop var
        assertEquals(
            4,
            p.variables.filter { !it.name.localName.contains(PythonHandler.LOOP_VAR_PREFIX) }.size,
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
            forStmt.declarations.firstOrNull {
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
        assertIs<Reference>(ifCond.lhs)
        assertLocalName("minor", ifCond.lhs)

        // phr = {"user_id": user_id} | content
        val ifThenFirstStmt = ifThen.statements.firstOrNull()
        assertIs<AssignExpression>(ifThenFirstStmt)
        val phrDeclaration = ifThenFirstStmt.declarations[0]

        assertNotNull(phrDeclaration)
        assertLocalName("phr", phrDeclaration)
        val phrInitializer = phrDeclaration.firstAssignment
        assertIs<BinaryOperator>(phrInitializer)
        assertEquals("|", phrInitializer.operatorCode)
        val phrInitializerLhs = phrInitializer.lhs
        assertIs<InitializerListExpression>(phrInitializerLhs)

        // z = {"user_id": user_id}
        val elseFirstStmt = ifElse.statements.firstOrNull()
        assertIs<AssignExpression>(elseFirstStmt)
        val elseStmt1 = elseFirstStmt.declarations[0]
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
        assertEquals("# a function", functions.firstOrNull()?.comment)

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
                true,
            ) {
                it.registerLanguage<PythonLanguage>().matchCommentsToNodes(true)
            }
        assertNotNull(tu)

        val annotations = tu.allChildren<Annotation>()
        assertEquals(
            listOf("app.route", "some.otherannotation", "other_func"),
            annotations.map { it.name.toString() },
        )
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

        val bodyFirstStmt = functionBody.statements.firstOrNull()
        assertIs<AssignExpression>(bodyFirstStmt)
        val varDefinedBeforeLoopRef = bodyFirstStmt.lhs.firstOrNull()
        assertIs<Reference>(varDefinedBeforeLoopRef)

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

        val secondLoopVar = secondLoop.variable
        assertIs<Reference>(secondLoopVar)
        assert(secondLoopVar.prevDFG.contains(secondLoopIterable) == true)

        // dataflow from second loop var to bar call
        assertEquals(secondLoopVar, barCall.arguments.firstOrNull()?.prevDFG?.firstOrNull())
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
                val itLhs = it.lhs.singleOrNull()
                assertIs<Reference>(itLhs)
                itLhs.name.localName == "b" && it.location?.region?.startLine == 4
            }
        assertNotNull(bAugAssign)
        assertEquals("*=", bAugAssign.operatorCode)
        assertEquals("b", bAugAssign.lhs.singleOrNull()?.name?.localName)
        val bAugAssignRhs = bAugAssign.rhs.singleOrNull()
        assertIs<Literal<*>>(bAugAssignRhs)
        assertEquals(2L, bAugAssignRhs.value)

        // c = (not True and False) or True
        val cAssign =
            tu.allChildren<AssignExpression>()
                .singleOrNull {
                    val itLhs = it.lhs.singleOrNull()
                    assertIs<Reference>(itLhs)
                    itLhs.name.localName == "c"
                }
                ?.rhs
                ?.singleOrNull()
        assertIs<BinaryOperator>(cAssign)
        assertEquals("or", cAssign.operatorCode)
        val cAssignRhs = cAssign.rhs
        assertIs<Literal<*>>(cAssignRhs)
        assertEquals(true, cAssignRhs.value)
        val cAssignLhs = cAssign.lhs
        assertIs<BinaryOperator>(cAssignLhs)
        assertEquals("and", cAssignLhs.operatorCode)
        val cAssignLhsRhs = cAssignLhs.rhs
        assertIs<Literal<*>>(cAssignLhsRhs)
        assertEquals(false, cAssignLhsRhs.value)
        val cAssignLhsLhs = cAssignLhs.lhs
        assertIs<UnaryOperator>(cAssignLhsLhs)
        assertEquals("not", cAssignLhsLhs.operatorCode)
        val cAssignLhsLhsInput = cAssignLhsLhs.input
        assertIs<Literal<*>>(cAssignLhsLhsInput)
        assertEquals(true, cAssignLhsLhsInput.value)

        // d = ((-5 >> 2) & ~7 | (+4 << 1)) ^ 0xffff
        val dAssign =
            tu.allChildren<AssignExpression>()
                .singleOrNull {
                    val itLhs = it.lhs.singleOrNull()
                    assertIs<Reference>(itLhs)
                    itLhs.name.localName == "d"
                }
                ?.rhs
                ?.singleOrNull()
        assertIs<BinaryOperator>(dAssign)
        assertEquals("^", dAssign.operatorCode)
        val dAssignRhs = dAssign.rhs
        assertIs<Literal<*>>(dAssignRhs)
        assertEquals(0xffffL, dAssignRhs.value)
        val dAssignLhs = dAssign.lhs
        assertIs<BinaryOperator>(dAssignLhs)
        assertEquals("|", dAssignLhs.operatorCode)
        val dAssignLhsRhs = dAssignLhs.rhs
        assertIs<BinaryOperator>(dAssignLhsRhs)
        assertEquals("<<", dAssignLhsRhs.operatorCode)
        val dAssignLhsRhsRhs = dAssignLhsRhs.rhs
        assertIs<Literal<*>>(dAssignLhsRhsRhs)
        assertEquals(1L, dAssignLhsRhsRhs.value)
        val dAssignLhsRhsLhs = dAssignLhsRhs.lhs
        assertIs<UnaryOperator>(dAssignLhsRhsLhs)
        assertEquals("+", dAssignLhsRhsLhs.operatorCode)
        val dAssignLhsRhsLhsInput = dAssignLhsRhsLhs.input
        assertIs<Literal<*>>(dAssignLhsRhsLhsInput)
        assertEquals(4L, dAssignLhsRhsLhsInput.value)
        val dAssignLhsOfOr = dAssignLhs.lhs
        assertIs<BinaryOperator>(dAssignLhsOfOr)
        assertEquals("&", dAssignLhsOfOr.operatorCode)
        val dAssignLhsOfOrRhs = dAssignLhsOfOr.rhs
        assertIs<UnaryOperator>(dAssignLhsOfOrRhs)
        assertEquals("~", dAssignLhsOfOrRhs.operatorCode)
        val dAssignLhsOfOrRhsInput = dAssignLhsOfOrRhs.input
        assertIs<Literal<*>>(dAssignLhsOfOrRhsInput)
        assertEquals(7L, dAssignLhsOfOrRhsInput.value)
        val dAssignLhsOfOrLhs = dAssignLhsOfOr.lhs
        assertIs<BinaryOperator>(dAssignLhsOfOrLhs)
        assertEquals(">>", dAssignLhsOfOrLhs.operatorCode)
        val dAssignLhsOfOrLhsRhs = dAssignLhsOfOrLhs.rhs
        assertIs<Literal<*>>(dAssignLhsOfOrLhsRhs)
        assertEquals(2L, dAssignLhsOfOrLhsRhs.value)
        val dAssignLhsOfOrLhsLhs = dAssignLhsOfOrLhs.lhs
        assertIs<UnaryOperator>(dAssignLhsOfOrLhsLhs)
        assertEquals("-", dAssignLhsOfOrLhsLhs.operatorCode)
        val dAssignLhsOfOrLhsLhsInput = dAssignLhsOfOrLhsLhs.input
        assertIs<Literal<*>>(dAssignLhsOfOrLhsLhsInput)
        assertEquals(5L, dAssignLhsOfOrLhsLhsInput.value)
    }

    @Test
    fun testDataTypes() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("datatypes.py").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)
        val namespace = tu.namespaces.singleOrNull()
        assertNotNull(namespace)

        val aStmt = namespace.statements[0]
        assertIs<AssignExpression>(aStmt)
        val aStmtRhs = aStmt.rhs.singleOrNull()
        assertIs<InitializerListExpression>(aStmtRhs)
        assertIs<ListType>(aStmtRhs.type)

        val bStmt = namespace.statements[1]
        assertIs<AssignExpression>(bStmt)
        val bStmtRhs = bStmt.rhs.singleOrNull()
        assertIs<InitializerListExpression>(bStmtRhs)
        assertIs<SetType>(bStmtRhs.type)

        val cStmt = namespace.statements[2]
        assertIs<AssignExpression>(cStmt)
        val cStmtRhs = cStmt.rhs.singleOrNull()
        assertIs<InitializerListExpression>(cStmtRhs)
        assertIs<ListType>(cStmtRhs.type)

        val dStmt = namespace.statements[3]
        assertIs<AssignExpression>(dStmt)
        val dStmtRhs = dStmt.rhs.singleOrNull()
        assertIs<InitializerListExpression>(dStmtRhs)
        assertIs<MapType>(dStmtRhs.type)

        val fourthStmt = namespace.statements[4]
        assertIs<AssignExpression>(fourthStmt)
        val eStmtRhs = fourthStmt.rhs.singleOrNull()
        assertIs<BinaryOperator>(eStmtRhs)
        val eStmtRhsLhs = eStmtRhs.lhs
        assertIs<Literal<*>>(eStmtRhsLhs)
        assertEquals("Values of a: ", eStmtRhsLhs.value)
        val eStmtRhsRhs = eStmtRhs.rhs
        assertIs<BinaryOperator>(eStmtRhsRhs)
        assertNotNull(eStmtRhsRhs)
        val aRef = eStmtRhsRhs.lhs
        assertEquals("a", aRef.name.localName)
        val eStmtRhsRhsRhs = eStmtRhsRhs.rhs
        assertIs<BinaryOperator>(eStmtRhsRhsRhs)
        val eStmtRhsRhsRhsLhs = eStmtRhsRhsRhs.lhs
        assertIs<Literal<*>>(eStmtRhsRhsRhsLhs)
        assertEquals(" and b: ", eStmtRhsRhsRhsLhs.value)
        val bCall = eStmtRhsRhsRhs.rhs
        assertIs<CallExpression>(bCall)
        assertEquals("str", bCall.name.localName)
        assertEquals("b", bCall.arguments.singleOrNull()?.name?.localName)

        val fifthStmt = namespace.statements[5]
        assertIs<AssignExpression>(fifthStmt)
        val fStmtRhs = fifthStmt.rhs.singleOrNull()

        assertIs<SubscriptExpression>(fStmtRhs)
        assertEquals("a", fStmtRhs.arrayExpression.name.localName)
        val subscriptExpression = fStmtRhs.subscriptExpression
        assertIs<RangeExpression>(subscriptExpression)
        val fStmtRhsFloor = subscriptExpression.floor
        assertIs<Literal<*>>(fStmtRhsFloor)
        assertEquals(1L, fStmtRhsFloor.value)
        val fStmtRhsCeiling = subscriptExpression.ceiling
        assertIs<Literal<*>>(fStmtRhsCeiling)
        assertEquals(3L, fStmtRhsCeiling.value)
        val fStmtRhsThird = subscriptExpression.third
        assertIs<Literal<*>>(fStmtRhsThird)
        assertEquals(2L, fStmtRhsThird.value)
    }

    @Test
    fun testSimpleImport() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("simple_import.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        assertEquals(1, result.variables.size)
        assertEquals(listOf("mypi"), result.variables.map { it.name.localName })
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
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        // import a
        val importA = result.imports["a"]
        assertNotNull(importA)
        assertEquals(ImportStyle.IMPORT_NAMESPACE, importA.style)
        assertContains(
            assertNotNull(importA.scope?.importedScopes),
            assertNotNull(result.finalCtx.scopeManager.lookupScope(Name("a"))),
        )

        // from c import *
        val importC = result.imports["c"]
        assertNotNull(importC)
        assertEquals(ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE, importC.style)
        // assertEquals(result.namespaces["c"], importC.importedFrom)

        val aFunc = result.functions["a.func"]
        assertNotNull(aFunc)

        val bFunc = result.functions["b.func"]
        assertNotNull(bFunc)

        val cCompletelyDifferentFunc = result.functions["c.completely_different_func"]
        assertNotNull(cCompletelyDifferentFunc)

        var calls = result.calls("a.func")
        assertEquals(2, calls.size)

        var call = calls.firstOrNull()
        assertNotNull(call)
        assertInvokes(call, aFunc)

        assertTrue(call.isImported)

        call = result.calls["b.func"]
        assertNotNull(call)
        assertInvokes(call, bFunc)

        call = result.calls["completely_different_func"]
        assertNotNull(call)
        assertInvokes(call, cCompletelyDifferentFunc)

        call = result.calls["c.completely_different_func"]
        assertNotNull(call)
        assertInvokes(call, cCompletelyDifferentFunc)
        assertTrue(call.isImported)
    }

    @Test
    fun testImportsWithoutDependencySource() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("import_no_src.py").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val barCalls = tu.calls("bar")
        assertEquals(2, barCalls.size)
        barCalls.forEach { barCall ->
            assertIs<CallExpression>(barCall)
            assertTrue(barCall.isImported)
        }

        val bazCall = tu.calls["baz"]
        assertNull(
            bazCall,
            "We should not have a baz() call anymore, since it should be harmonized",
        )

        val fooCall = tu.calls["foo"]
        assertIs<CallExpression>(fooCall)
        assertTrue(fooCall.isImported)

        val foo3Call = tu.calls["foo3"]
        assertIs<CallExpression>(foo3Call)
        assertTrue(foo3Call.isImported)
    }

    @Test
    fun testInterfaceStubs() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("complex_class.pyi").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        with(result) {
            val foo = records["Foo"]
            assertNotNull(foo)

            val bar = foo.methods["bar"]
            assertNotNull(bar)

            assertEquals(assertResolvedType("int"), bar.returnTypes.singleOrNull())

            val param = bar.parameters.firstOrNull()
            assertNotNull(param)
            assertContains(param.assignedTypes, assertResolvedType("int"))
            assertEquals(assertResolvedType("complex_class.Foo"), bar.receiver?.type)
        }
    }

    @Test
    fun testNamedExpression() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("named_expressions.py").toFile()), topLevel, true) {
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

    @Test
    fun testParseWithUnicode() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("unicode.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val normalFunc = tu.functions["normal_func"]
        assertNotNull(normalFunc)
        // 11 chars (including whitespace) -> SARIF position = 12
        //     e = "e"
        assertEquals(12, normalFunc.body?.location?.region?.endColumn)

        val unicodeFunc = tu.functions["unicode_func"]
        assertNotNull(unicodeFunc)

        // also 11 chars (including whitespace) -> SARIF position = 12
        // But the python parser somehow sees these as two bytes so the position is 13 :(
        //     e = "é"
        assertEquals(13, unicodeFunc.body?.location?.region?.endColumn)

        // So the code exceeds the line, but we clamp it and avoid a crash
        assertEquals("e = \"é\"", unicodeFunc.body?.code)
    }

    @Test
    fun testPackageResolution() {
        val topLevel = Path.of("src", "test", "resources", "python", "packages")
        var result =
            analyze(listOf(topLevel.resolve("foobar")).map { it.toFile() }, topLevel, true) {
                it.registerLanguage<PythonLanguage>()
                it.useParallelFrontends(false)
                it.failOnError(false)
                it.inferenceConfiguration(
                    InferenceConfiguration.builder().inferFunctions(false).build()
                )
            }
        assertNotNull(result)

        var expected =
            setOf(
                "foobar",
                "foobar.__main__",
                "foobar.module1",
                "foobar.config",
                "foobar.implementation",
                "foobar.implementation.internal_bar",
                "foobar.implementation.internal_foo",
            )
        assertEquals(expected, result.namespaces.map { it.name.toString() }.distinct().toSet())

        var bar = result.functions["bar"]
        assertNotNull(bar)
        assertFullName("foobar.implementation.internal_bar.bar", bar)

        var foo = result.functions["foo"]
        assertNotNull(foo)
        assertFullName("foobar.implementation.internal_foo.foo", foo)

        var barCall = result.calls["bar"]
        assertNotNull(barCall)
        assertInvokes(barCall, bar)

        var fooCalls = result.calls("foo")
        assertEquals(2, fooCalls.size)
        fooCalls.forEach { assertInvokes(it, foo) }

        val refBarString = result.refs("bar_string")
        refBarString.forEach {
            assertNotNull(it)
            assertNotNull(it.refersTo)
        }

        val refFooString = result.refs("foo_string")
        refFooString.forEach {
            assertNotNull(it)
            assertNotNull(it.refersTo)
        }
    }

    @Test
    fun testImportTest() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("import_test.py").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val refs = tu.refs
        refs.forEach { assertIsNot<MemberExpression>(it, "{${it.name}} is a member expression") }
        assertEquals(
            setOf("a", "b", "pkg.module.foo", "pkg.another_module.foo"),
            refs.map { it.name.toString() }.toSet(),
        )

        val imports = tu.imports
        assertEquals(
            setOf("pkg", "pkg.module", "pkg.another_module"),
            imports.map { it.name.toString() }.toSet(),
        )
    }

    @Test
    fun testImportVsMember() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("import_vs_member.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val pkg = result.namespaces["pkg"]
        assertNotNull(pkg)
        assertTrue(pkg.isInferred)

        val pkgThirdModule = result.namespaces["pkg.third_module"]
        assertNotNull(pkgThirdModule)
        assertTrue(pkg.isInferred)

        val pkgFunction = result.functions["pkg.function"]
        assertNotNull(pkgFunction)
        assertTrue(pkg.isInferred)

        val anotherPkg = result.namespaces["another_pkg"]
        assertNotNull(anotherPkg)
        assertTrue(pkg.isInferred)

        val refs = result.refs

        // All reference except the .field access should be reference and not a member expression
        refs.filter { it.name.localName != "field" }.forEach { assertIsNot<MemberExpression>(it) }

        assertEquals(
            listOf("pkg.function", "another_pkg.function", "another_pkg.function", "pkg.function"),
            result.calls.map { it.name.toString() },
        )

        assertEquals(
            listOf(
                // this is the default parameter of foo
                "pkg.some_variable",
                // lhs
                "a",
                // rhs, ME
                "UNKNOWN.field",
                // rhs, base of ME
                "pkg.some_variable",
                // lhs
                "b",
                // rhs
                "pkg.function",
                // lhs
                "c",
                // rhs
                "another_pkg.function",
                // lhs
                "d",
                // rhs
                "another_pkg.function",
                // lhs
                "e",
                // rhs
                "pkg.third_module.variable",
                // lhs
                "f",
                // rhs
                "pkg.function",
            ),
            refs.map { it.name.toString() },
        )
    }

    @Test
    fun testFunctionResolution() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("foobar.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        // ensure, we have four functions and no inferred ones
        val functions = tu.functions
        assertEquals(4, functions.size)

        val inferred = functions.filter { it.isInferred }
        assertTrue(inferred.isEmpty())
    }

    @Test
    fun testMultiComponent() {
        val projectRoot = Path.of("src", "test", "resources", "python", "big-project")

        val config =
            TranslationConfiguration.builder()
                .softwareComponents(
                    mutableMapOf(
                        "component1" to listOf(projectRoot.resolve("component1").toFile()),
                        "component2" to listOf(projectRoot.resolve("component2").toFile()),
                        "stdlib" to listOf(projectRoot.resolve("stdlib").toFile()),
                    )
                )
                .topLevels(
                    mapOf(
                        "component1" to projectRoot.resolve("component1").toFile(),
                        "component2" to projectRoot.resolve("component2").toFile(),
                        "stdlib" to projectRoot.resolve("stdlib").toFile(),
                    )
                )
                .loadIncludes(true)
                .disableCleanup()
                .debugParser(true)
                .failOnError(true)
                .useParallelFrontends(true)
                .defaultPasses()
                .registerLanguage<PythonLanguage>()
                .build()

        val result = TranslationManager.builder().config(config).build().analyze().get()
        assertEquals(3, result.components.size)

        val stdlib = result.components["stdlib"]
        assertNotNull(stdlib)
        assertEquals(listOf("os"), stdlib.namespaces.map { it.name.toString() })
        val osName = stdlib.namespaces["os"].variables["name"]
        assertNotNull(osName)

        val component1 = result.components["component1"]
        assertNotNull(component1)
        assertEquals(
            listOf("mypackage", "mypackage.module"),
            component1.namespaces.map { it.name.toString() },
        )
        val a = component1.variables["a"]
        assertNotNull(a)
        assertRefersTo(a.firstAssignment, osName)

        val component2 = result.components["component2"]
        assertNotNull(component2)
        assertEquals(
            listOf("otherpackage", "otherpackage.module"),
            component2.namespaces.map { it.name.toString() },
        )
        val c = component2.variables["c"]
        assertNotNull(c)
        assertRefersTo(c.firstAssignment, a)

        val fooCall = component2.calls["foo"]
        assertNotNull(fooCall)

        val barArgument = fooCall.argumentEdges["bar"]?.end
        assertNotNull(barArgument)
    }

    @Test
    fun testVariableInference() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("variable_inference.py").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val someClass = tu.records["SomeClass"]
        assertNotNull(someClass)

        val fieldX = someClass.fields["x"]
        assertNotNull(fieldX)

        val method = tu.functions["method"]
        assertNotNull(method)
        // method has a local variables "b".
        val variableB = method.variables["b"]
        assertNotNull(variableB)
        assertIsNot<FieldDeclaration>(variableB)
        assertEquals(1, someClass.fields.size)

        val someClass2 = tu.records["SomeClass2"]
        assertNotNull(someClass2)
        val staticMethod = tu.functions["static_method"]
        assertNotNull(staticMethod)
        // static_method has two local variables which are "b" and "x"
        assertEquals(2, staticMethod.variables.filter { it !is FieldDeclaration }.size)
        assertEquals(setOf("b", "x"), staticMethod.variables.map { it.name.localName }.toSet())
        assertTrue(someClass2.fields.isEmpty())

        // There is no field called "b" in the result.
        assertNull(tu.fields["b"])

        val foo = tu.functions["foo"]
        assertNotNull(foo)
        val refersTo = foo.refs("fooA").map { it.refersTo }
        refersTo.forEach { refersTo -> assertIs<ParameterDeclaration>(refersTo) }
    }

    @Test
    fun testSuperclassImportFullPath() {
        val topLevel = Path.of("src", "test")
        val result =
            analyze(
                listOf(
                    topLevel
                        .resolve("resources/python/superclasses/superclass_import_full_path.py")
                        .toFile(),
                    topLevel.resolve("resources/python/superclasses/superclass.py").toFile(),
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val clsBase = result.records["base"]
        assertNotNull(clsBase)

        val clsSuper = clsBase.superClasses.firstOrNull()
        assertNotNull(clsSuper)
        assertIs<ObjectType>(clsSuper)

        val expectedSuper = result.records["Foobar"]
        assertNotNull(expectedSuper)
        assertEquals(expectedSuper, clsSuper.recordDeclaration)
    }

    @Test
    fun testSuperclassImportModuleAlias() {
        val topLevel = Path.of("src", "test")
        val result =
            analyze(
                listOf(
                    topLevel
                        .resolve("resources/python/superclasses/superclass_import_module_alias.py")
                        .toFile(),
                    topLevel.resolve("resources/python/superclasses/superclass.py").toFile(),
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val clsBase = result.records["base"]
        assertNotNull(clsBase)

        val clsSuper = clsBase.superClasses.firstOrNull()
        assertNotNull(clsSuper)
        assertIs<ObjectType>(clsSuper)

        val expectedSuper = result.records["Foobar"]
        assertNotNull(expectedSuper)
        assertEquals(expectedSuper, clsSuper.recordDeclaration)
    }

    @Test
    fun testSuperclassIncorrect() {
        val topLevel = Path.of("src", "test")
        val result =
            analyze(
                listOf(topLevel.resolve("resources/python/superclasses/incorrect.py").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        var myClass = result.finalCtx.typeManager.resolvedTypes["MyClass"]
        assertNotNull(myClass)
        assertNotNull(myClass.ancestors)
    }

    @Test
    fun testNestedReplace() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("nested_replace.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val functionCall = result.calls["function"]
        assertNotNull(functionCall)

        val anotherFunctionCall = result.mcalls["another_function"]
        assertNotNull(anotherFunctionCall)
        assertNotNull(anotherFunctionCall.astParent)
        assertSame(functionCall, anotherFunctionCall.astParent)
    }
}
