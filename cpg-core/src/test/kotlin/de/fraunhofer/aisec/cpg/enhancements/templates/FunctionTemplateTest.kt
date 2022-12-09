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
package de.fraunhofer.aisec.cpg.enhancements.templates

import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.TestUtils.findByUniquePredicate
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.frontends.cpp.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.*
import java.nio.file.Path
import java.util.function.Predicate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class FunctionTemplateTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "templates", "functiontemplates")

    @Test
    @Throws(Exception::class)
    fun testDependentType() {
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "functionTemplate.cpp").toFile()),
                topLevel,
                true
            )
        val variableDeclarations = result.variables
        val x = findByUniqueName(variableDeclarations, "x")
        assertEquals(UnknownType.getUnknownType(), x.type)

        val declaredReferenceExpressions = result.refs
        val xDeclaredReferenceExpression = findByUniqueName(declaredReferenceExpressions, "x")
        assertEquals(UnknownType.getUnknownType(), xDeclaredReferenceExpression.type)

        val binaryOperators = result.allChildren<BinaryOperator>()
        val dependentOperation =
            findByUniquePredicate(binaryOperators) { b: BinaryOperator -> b.code == "val * N" }
        assertEquals(UnknownType.getUnknownType(), dependentOperation.type)
    }

    private fun testFunctionTemplateArguments(
        callFloat3: CallExpression,
        floatType: ObjectType,
        int3: Literal<*>
    ) {
        assertEquals(2, callFloat3.templateParameters.size)
        assertEquals(floatType, (callFloat3.templateParameters[0] as TypeExpression).type)
        assertEquals(0, callFloat3.templateParametersEdges!![0].getProperty(Properties.INDEX))
        assertEquals(
            TemplateDeclaration.TemplateInitialization.EXPLICIT,
            callFloat3.templateParametersEdges!![0].getProperty(Properties.INSTANTIATION)
        )
        assertEquals(int3, callFloat3.templateParameters[1])
        assertEquals(1, callFloat3.templateParametersEdges!![1].getProperty(Properties.INDEX))
        assertEquals(
            TemplateDeclaration.TemplateInitialization.EXPLICIT,
            callFloat3.templateParametersEdges!![1].getProperty(Properties.INSTANTIATION)
        )
    }

    @Test
    @Throws(Exception::class)
    fun testFunctionTemplateStructure() {
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "functionTemplate.cpp").toFile()),
                topLevel,
                true
            )
        // This test checks the structure of FunctionTemplates without the TemplateExpansionPass
        val functionTemplateDeclaration = result.allChildren<FunctionTemplateDeclaration>()[0]

        // Check FunctionTemplate Parameters
        val typeParamDeclarations = result.allChildren<TypeParamDeclaration>()
        assertEquals(1, typeParamDeclarations.size)

        val typeParamDeclaration = typeParamDeclarations[0]
        assertEquals(typeParamDeclaration, functionTemplateDeclaration.parameters[0])

        val typeT = ParameterizedType("T", CPPLanguage())
        val intType =
            ObjectType(
                "int",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.SIGNED,
                true,
                CPPLanguage()
            )
        val floatType =
            ObjectType(
                "float",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.SIGNED,
                true,
                CPPLanguage()
            )
        assertEquals(typeT, typeParamDeclaration.type)
        assertEquals(intType, typeParamDeclaration.default)

        val N = findByUniqueName(result.parameters, "N")
        val int2 = findByUniquePredicate(result.literals { it.value == 2 }) { it.value == 2 }
        val int3 = findByUniquePredicate(result.literals) { it.value == 3 }
        val int5 = findByUniquePredicate(result.literals) { it.value == 5 }
        assertEquals(N, functionTemplateDeclaration.parameters[1])
        assertEquals(intType, N.type)
        assertEquals(5, (N.default as Literal<*>).value)
        assertTrue(N.prevDFG.contains(int5))
        assertTrue(N.prevDFG.contains(int3))
        assertTrue(N.prevDFG.contains(int2))

        // Check the realization
        assertEquals(1, functionTemplateDeclaration.realization.size)

        val fixedMultiply = functionTemplateDeclaration.realization[0]
        val funcType = fixedMultiply.type as? FunctionType
        assertNotNull(funcType)
        assertEquals(typeT, funcType.returnTypes.firstOrNull())

        val `val` = fixedMultiply.parameters[0]
        assertEquals(typeT, `val`.type)

        // Check the invokes
        val callInt2 = findByUniquePredicate(result.calls) { it.location!!.region.startLine == 12 }
        assertEquals(1, callInt2.invokes.size)
        assertEquals(fixedMultiply, callInt2.invokes[0])

        val callFloat3 =
            findByUniquePredicate(result.calls) { c: CallExpression ->
                c.location!!.region.startLine == 13
            }
        assertEquals(1, callFloat3.invokes.size)
        assertEquals(fixedMultiply, callFloat3.invokes[0])

        // Check return values
        assertEquals(intType, callInt2.type)
        assertEquals(floatType, callFloat3.type)

        // Check template arguments
        testFunctionTemplateArguments(callFloat3, floatType, int3)
    }

    @Test
    @Throws(Exception::class)
    fun testInvocationWithCallTarget() {
        // Check invocation target with specialized function alongside template with same name
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "functionTemplateInvocation1.cpp").toFile()),
                topLevel,
                true
            )
        val doubleFixedMultiply =
            findByUniquePredicate(result.functions) { f: FunctionDeclaration ->
                f.name.localName == "fixed_multiply" &&
                    f.returnTypes.firstOrNull()?.name?.localName == "double"
            }
        val call =
            findByUniquePredicate(result.calls) { c: CallExpression ->
                c.name.localName == "fixed_multiply"
            }

        // Check invocation
        assertEquals(1, call.invokes.size)
        assertEquals(doubleFixedMultiply, call.invokes[0])
        // Check return value
        assertLocalName("double", call.type)
    }

    @Test
    @Throws(Exception::class)
    fun testInvocationWithoutCallTarget() {
        // Check if a CallExpression is converted to a TemplateCallExpression if a compatible target
        // exists
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "functionTemplateInvocation2.cpp").toFile()),
                topLevel,
                true
            )
        val templateDeclaration =
            findByUniquePredicate(result.allChildren<FunctionTemplateDeclaration>()) {
                t: FunctionTemplateDeclaration ->
                t.name.localName == "fixed_multiply"
            }
        val fixedMultiply =
            findByUniquePredicate(result.functions) { f: FunctionDeclaration ->
                f.name.localName == "fixed_multiply" &&
                    f.returnTypes.firstOrNull()?.name?.localName == "T"
            }

        // Check realization of template maps to our target function
        assertEquals(1, templateDeclaration.realization.size)
        assertEquals(fixedMultiply, templateDeclaration.realization[0])

        val call =
            findByUniquePredicate(result.calls) { c: CallExpression ->
                c.name.localName == "fixed_multiply"
            }
        // Check invocation target
        assertEquals(1, call.invokes.size)
        assertEquals(fixedMultiply, call.invokes[0])

        // Check template parameters
        val doubleType =
            ObjectType(
                "double",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.SIGNED,
                true,
                CPPLanguage()
            )
        val literal5 = findByUniquePredicate(result.literals) { l: Literal<*> -> l.value == 5 }
        assertEquals(2, call.templateParameters.size)
        assertEquals(doubleType, (call.templateParameters[0] as TypeExpression).type)
        assertEquals(literal5, call.templateParameters[1])

        // Check return value
        assertEquals(doubleType, call.type)
    }

    @Test
    @Throws(Exception::class)
    fun testInvocationWithAutoDeduction() {
        // Check if a TemplateCallExpression without template parameters performs autodeduction
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "functionTemplateInvocation3.cpp").toFile()),
                topLevel,
                true
            )
        val templateDeclaration =
            findByUniquePredicate(result.allChildren<FunctionTemplateDeclaration>()) {
                t: FunctionTemplateDeclaration ->
                t.name.localName == "fixed_multiply"
            }
        val fixedMultiply =
            findByUniquePredicate(result.functions) { f: FunctionDeclaration ->
                f.name.localName == "fixed_multiply" &&
                    f.returnTypes.firstOrNull()?.name?.localName == "T"
            }

        // Check realization of template maps to our target function
        assertEquals(1, templateDeclaration.realization.size)
        assertEquals(fixedMultiply, templateDeclaration.realization[0])
        val call = findByUniquePredicate(result.calls) { it.name.localName == "fixed_multiply" }

        // Check invocation target
        assertEquals(1, call.invokes.size)
        assertEquals(fixedMultiply, call.invokes[0])

        // Check template parameters
        val literal5 = findByUniquePredicate(result.literals) { l: Literal<*> -> l.value == 5 }
        assertEquals(2, call.templateParameters.size)
        assertLocalName("double", call.templateParameters[0])
        assertEquals(literal5, call.templateParameters[1])

        // Check return value
        assertLocalName("double", call.type)
    }

    @Test
    @Throws(Exception::class)
    fun testInvocationWithDefaults() {
        // test invocation target when no autodeduction is possible, but defaults are provided
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "functionTemplateInvocation4.cpp").toFile()),
                topLevel,
                true
            )
        val templateDeclaration =
            findByUniquePredicate(result.allChildren<FunctionTemplateDeclaration>()) {
                it.name.localName == "fixed_multiply"
            }
        val fixedMultiply =
            findByUniquePredicate(result.functions) {
                it.name.localName == "fixed_multiply" &&
                    it.returnTypes.firstOrNull()?.name?.localName == "T"
            }

        // Check realization of template maps to our target function
        assertEquals(1, templateDeclaration.realization.size)
        assertEquals(fixedMultiply, templateDeclaration.realization[0])
        val call =
            findByUniquePredicate(result.calls) { c: CallExpression ->
                c.name.localName == "fixed_multiply"
            }

        // Check invocation target
        assertEquals(1, call.invokes.size)
        assertEquals(fixedMultiply, call.invokes[0])

        // Check template parameters
        val intType =
            findByUniquePredicate(result.allChildren()) { t: ObjectType ->
                t.name.localName == "int"
            }
        val literal5 =
            findByUniquePredicate<Literal<*>>(result.literals) { l: Literal<*> -> l.value == 5 }
        assertEquals(2, call.templateParameters.size)
        assertEquals(intType, (call.templateParameters[0] as TypeExpression).type)
        assertEquals(literal5, call.templateParameters[1])

        // Check return value
        assertEquals(intType, call.type)
    }

    @Test
    @Throws(Exception::class)
    fun testInvocationWithPartialDefaults() {
        // test invocation target when no autodeduction is possible, but defaults are partially used
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "functionTemplateInvocation5.cpp").toFile()),
                topLevel,
                true
            )
        val templateDeclaration =
            findByUniquePredicate(result.allChildren<FunctionTemplateDeclaration>()) {
                t: FunctionTemplateDeclaration ->
                t.name.localName == "fixed_multiply"
            }
        val fixedMultiply =
            findByUniquePredicate(result.functions) { f: FunctionDeclaration ->
                f.name.localName == "fixed_multiply" &&
                    f.returnTypes.firstOrNull()?.name?.localName == "T"
            }

        // Check realization of template maps to our target function
        assertEquals(1, templateDeclaration.realization.size)
        assertEquals(fixedMultiply, templateDeclaration.realization[0])
        val call =
            findByUniquePredicate(result.calls) { c: CallExpression ->
                c.name.localName == "fixed_multiply"
            }

        // Check invocation target
        assertEquals(1, call.invokes.size)
        assertEquals(fixedMultiply, call.invokes[0])

        // Check template parameters
        val doubleType =
            ObjectType(
                "double",
                Type.Storage.AUTO,
                Type.Qualifier(),
                ArrayList(),
                ObjectType.Modifier.SIGNED,
                true,
                CPPLanguage()
            )
        val literal5 = findByUniquePredicate(result.literals) { l: Literal<*> -> l.value == 5 }
        assertEquals(2, call.templateParameters.size)
        assertEquals(doubleType, (call.templateParameters[0] as TypeExpression).type)
        assertEquals(literal5, call.templateParameters[1])

        // Check return value
        assertEquals(doubleType, call.type)
    }

    @Test
    @Throws(Exception::class)
    fun testInvocationWithImplicitCastToOverridenTemplateParameter() {
        // test invocation target when template parameter produces a cast in an argument
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "functionTemplateInvocation6.cpp").toFile()),
                topLevel,
                true
            )
        val templateDeclaration =
            findByUniquePredicate(result.allChildren<FunctionTemplateDeclaration>()) {
                t: FunctionTemplateDeclaration ->
                t.name.localName == "fixed_multiply"
            }
        val fixedMultiply =
            findByUniquePredicate(result.functions) { f: FunctionDeclaration ->
                f.name.localName == "fixed_multiply" &&
                    f.returnTypes.firstOrNull()?.name?.localName == "T"
            }

        // Check realization of template maps to our target function
        assertEquals(1, templateDeclaration.realization.size)
        assertEquals(fixedMultiply, templateDeclaration.realization[0])
        val call =
            findByUniquePredicate(result.calls) { c: CallExpression ->
                c.name.localName == "fixed_multiply"
            }

        // Check invocation target
        assertEquals(1, call.invokes.size)
        assertEquals(fixedMultiply, call.invokes[0])

        // Check template parameters
        val intType =
            findByUniquePredicate(result.allChildren<ObjectType>()) { t: ObjectType ->
                t.name.localName == "int"
            }
        val literal5 = findByUniquePredicate(result.literals) { l: Literal<*> -> l.value == 5 }
        assertEquals(2, call.templateParameters.size)
        assertEquals(intType, (call.templateParameters[0] as TypeExpression).type)
        assertEquals(literal5, call.templateParameters[1])

        // Check return value
        assertEquals(intType, call.type)

        // Check cast
        assertEquals(1, call.arguments.size)
        assertTrue(call.arguments[0] is CastExpression)
        val arg = call.arguments[0] as CastExpression
        assertEquals(intType, arg.castType)
        assertEquals(20.3, (arg.expression as Literal<*>).value)
    }

    @Test
    @Throws(Exception::class)
    fun testInvocationWithImplicitCast() {
        // test invocation target when signature does not match but implicitcast can be applied
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "functionTemplateInvocation7.cpp").toFile()),
                topLevel,
                true
            )
        val templateDeclaration =
            findByUniquePredicate(result.allChildren<FunctionTemplateDeclaration>()) {
                t: FunctionTemplateDeclaration ->
                t.name.localName == "f" && !t.isInferred
            }
        val f =
            findByUniquePredicate(result.functions) { func: FunctionDeclaration ->
                (func.name.localName == "f" &&
                    !templateDeclaration.realization.contains(func) &&
                    !func.isInferred)
            }
        val f1 =
            findByUniquePredicate(result.calls) { c: CallExpression ->
                c.location!!.region.startLine == 9
            }
        val f2 =
            findByUniquePredicate(result.calls) { c: CallExpression ->
                c.location!!.region.startLine == 10
            }
        val f3 =
            findByUniquePredicate(result.calls) { c: CallExpression ->
                c.location!!.region.startLine == 11
            }
        val f4 =
            findByUniquePredicate(result.calls) { c: CallExpression ->
                c.location!!.region.startLine == 12
            }
        assertEquals(1, f1.invokes.size)
        assertEquals(f, f1.invokes[0])
        assertEquals(1, f2.invokes.size)
        assertEquals(templateDeclaration.realization[0], f2.invokes[0])
        assertEquals(1, f3.invokes.size)
        assertEquals(f, f3.invokes[0])
        assertEquals(2, f3.arguments.size)
        assertLocalName("int", f3.arguments[0].type)
        assertLocalName("int", f3.arguments[1].type)
        assertTrue(f3.arguments[1] is CastExpression)
        val castExpression = f3.arguments[1] as CastExpression
        assertEquals('b', (castExpression.expression as Literal<*>).value)
        assertEquals(1, f4.invokes.size)
        assertTrue(f4.invokes[0].isInferred)
    }

    @Test
    @Throws(Exception::class)
    fun testFunctionTemplateInMethod() {
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "functionTemplateMethod.cpp").toFile()),
                topLevel,
                true
            )
        val recordDeclaration =
            findByUniquePredicate(result.records) { c: RecordDeclaration ->
                c.name.localName == "MyClass"
            }
        val templateDeclaration =
            findByUniquePredicate(result.allChildren<FunctionTemplateDeclaration>()) {
                t: FunctionTemplateDeclaration ->
                t.name.localName == "fixed_multiply" && !t.isImplicit
            }
        assertEquals(2, templateDeclaration.parameters.size)
        assertEquals(1, recordDeclaration.templates.size)
        assertTrue(recordDeclaration.templates.contains(templateDeclaration))
        val methodDeclaration =
            findByUniquePredicate(result.methods) { m: MethodDeclaration ->
                !m.isImplicit && m.name.localName == "fixed_multiply"
            }
        assertEquals(1, templateDeclaration.realization.size)
        assertTrue(templateDeclaration.realization.contains(methodDeclaration))

        // Test callexpression to invoke the realization
        val callExpression =
            findByUniquePredicate(result.calls) { c: CallExpression ->
                c.code != null && c.code == "myObj.fixed_multiply<int>(3);"
            }
        assertEquals(1, callExpression.invokes.size)
        assertEquals(methodDeclaration, callExpression.invokes[0])
        assertEquals(templateDeclaration, callExpression.templateInstantiation)
        assertEquals(2, callExpression.templateParameters.size)
        assertLocalName("int", callExpression.templateParameters[0])
        assertEquals(
            TemplateDeclaration.TemplateInitialization.EXPLICIT,
            callExpression.templateParametersEdges?.get(0)?.getProperty(Properties.INSTANTIATION)
        )
        assertEquals(0, callExpression.templateParametersEdges!![0].getProperty(Properties.INDEX))
        val int5 =
            findByUniquePredicate(result.literals, Predicate { l: Literal<*> -> l.value == 5 })
        assertEquals(int5, callExpression.templateParameters[1])
        assertEquals(1, callExpression.templateParametersEdges!![1].getProperty(Properties.INDEX))
        assertEquals(
            TemplateDeclaration.TemplateInitialization.DEFAULT,
            callExpression.templateParametersEdges!![1].getProperty(Properties.INSTANTIATION)
        )
    }

    @Test
    @Throws(Exception::class)
    fun testCreateInferred() {
        // test invocation target when template parameter produces a cast in an argument
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "functionTemplateInvocation8.cpp").toFile()),
                topLevel,
                true
            )

        // Check inferred for first fixed_division call
        var templateDeclaration =
            findByUniquePredicate(result.allChildren<FunctionTemplateDeclaration>()) {
                t: FunctionTemplateDeclaration ->
                t.code == "fixed_division<int,2>(10)"
            }
        var fixedDivision =
            findByUniquePredicate(result.functions) { f: FunctionDeclaration ->
                f.code == "fixed_division<int,2>(10)" && f.isInferred
            }
        assertEquals(1, templateDeclaration.realization.size)
        assertEquals(fixedDivision, templateDeclaration.realization[0])
        assertEquals(2, templateDeclaration.parameters.size)
        assertTrue(templateDeclaration.parameters[0] is TypeParamDeclaration)
        assertTrue(templateDeclaration.parameters[1] is ParamVariableDeclaration)
        assertEquals(1, fixedDivision.parameters.size)
        val callInt2 =
            findByUniquePredicate(result.calls) { c: CallExpression ->
                c.location!!.region.startLine == 12
            }
        assertEquals(1, callInt2.invokes.size)
        assertEquals(fixedDivision, callInt2.invokes[0])
        assertTrue(
            callInt2.templateParameters[1].nextDFG.contains(templateDeclaration.parameters[1])
        )

        // Check inferred for second fixed_division call
        templateDeclaration =
            findByUniquePredicate(result.allChildren<FunctionTemplateDeclaration>()) {
                t: FunctionTemplateDeclaration ->
                t.code == "fixed_division<double,3>(10.0)"
            }
        fixedDivision =
            findByUniquePredicate(result.functions) { f: FunctionDeclaration ->
                f.code == "fixed_division<double,3>(10.0)" && f.isInferred
            }
        assertEquals(1, templateDeclaration.realization.size)
        assertEquals(fixedDivision, templateDeclaration.realization[0])
        assertEquals(2, templateDeclaration.parameters.size)
        assertTrue(templateDeclaration.parameters[0] is TypeParamDeclaration)
        assertTrue(templateDeclaration.parameters[1] is ParamVariableDeclaration)
        assertEquals(1, fixedDivision.parameters.size)
        val callDouble3 =
            findByUniquePredicate(result.calls) { c: CallExpression ->
                c.location!!.region.startLine == 13
            }
        assertEquals(1, callDouble3.invokes.size)
        assertEquals(fixedDivision, callDouble3.invokes[0])
        assertTrue(
            callDouble3.templateParameters[1].nextDFG.contains(templateDeclaration.parameters[1])
        )

        // Check return values
        assertEquals(UnknownType.getUnknownType(), callInt2.type)
        assertEquals(UnknownType.getUnknownType(), callDouble3.type)
    }
}
