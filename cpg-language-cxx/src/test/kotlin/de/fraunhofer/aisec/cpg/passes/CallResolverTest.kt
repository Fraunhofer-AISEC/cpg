/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import java.nio.file.Path
import java.util.function.Predicate
import kotlin.test.*

class CallResolverTest : BaseTest() {
    private fun testMethods(records: List<RecordDeclaration>, intType: Type, stringType: Type) {
        val callsRecord = findByUniqueName(records, "Calls")
        val externalRecord = findByUniqueName(records, "External")
        val superClassRecord = findByUniqueName(records, "SuperClass")
        val innerMethods = findByName(callsRecord.innerMethods, "innerTarget")
        val innerCalls = findByName(callsRecord.calls, "innerTarget")
        checkCalls(intType, stringType, innerMethods, innerCalls)
        val superMethods = findByName(superClassRecord.innerMethods, "superTarget").toMutableList()
        // We can't infer that a call to superTarget(int, int, int) is intended to be part of the
        // superclass. It looks like a call to a member of Calls.java, thus we need to add these
        // methods to the lookup
        superMethods.addAll(findByName(callsRecord.innerMethods, "superTarget"))
        val superCalls = findByName(callsRecord.calls, "superTarget")
        checkCalls(intType, stringType, superMethods, superCalls)
        val externalMethods = findByName(externalRecord.innerMethods, "externalTarget")
        val externalCalls = findByName(callsRecord.calls, "externalTarget")
        checkCalls(intType, stringType, externalMethods, externalCalls)
    }

    private fun ensureNoUnknownClassDummies(records: List<RecordDeclaration>) {
        val callsRecord = findByUniqueName(records, "Calls")
        assertTrue(records.stream().noneMatch { it.name.localName == "Unknown" })

        val unknownCall = findByUniqueName(callsRecord.calls, "unknownTarget")
        assertEquals(listOf<Any>(), unknownCall.invokes)
    }

    /**
     * Checks that method calls from a function outside a class are correctly resolved to the
     * MethodDeclaration
     *
     * @param result
     */
    private fun ensureInvocationOfMethodsInFunction(result: TranslationResult) {
        assertEquals(1, result.components.flatMap { it.translationUnits }.size)
        val tu = result.components.flatMap { it.translationUnits }[0]
        for (declaration in tu.declarations) {
            assertNotEquals("invoke", declaration.name.localName)
        }
        val callExpressions = result.calls
        val invoke = findByUniqueName(callExpressions, "invoke")
        assertEquals(1, invoke.invokes.size)
        assertTrue(invoke.invokes[0] is MethodDeclaration)
    }

    private fun checkCalls(
        intType: Type,
        stringType: Type,
        methods: Collection<FunctionDeclaration>,
        calls: Collection<CallExpression>,
    ) {
        val signatures = listOf(listOf(), listOf(intType, intType), listOf(intType, stringType))
        for (signature in signatures) {
            for (call in calls.filter { it.signature == signature }) {
                val target =
                    findByUniquePredicate(methods) { m: FunctionDeclaration ->
                        m.matchesSignature(signature) != IncompatibleSignature
                    }
                assertEquals(listOf(target), call.invokes)
            }
        }

        // Check for inferred nodes
        val inferenceSignature = listOf(intType, intType, intType)
        for (inferredCall in
            calls.filter { c: CallExpression -> c.signature == inferenceSignature }) {

            val inferredTarget =
                findByUniquePredicate(methods) { m: FunctionDeclaration ->
                    m.matchesSignature(inferenceSignature) != IncompatibleSignature
                }
            assertEquals(listOf(inferredTarget), inferredCall.invokes)
            assertTrue(inferredTarget.isInferred)
        }
    }

    private fun testOverriding(records: List<RecordDeclaration>) {
        val callsRecord = findByUniqueName(records, "Calls")
        val externalRecord = findByUniqueName(records, "External")
        val superClassRecord = findByUniqueName(records, "SuperClass")
        val originalMethod = findByUniqueName(superClassRecord.innerMethods, "overridingTarget")
        val overridingMethod = findByUniqueName(externalRecord.innerMethods, "overridingTarget")
        val call = findByUniqueName(callsRecord.calls, "overridingTarget")

        // TODO related to #204: Currently we have both the original and the overriding method in
        //  the invokes list. This check needs to be adjusted to the choice we make on solving #204
        assertTrue(call.invokes.contains(overridingMethod))
        assertEquals<List<FunctionDeclaration>>(listOf(originalMethod), overridingMethod.overrides)
        assertEquals<List<FunctionDeclaration>>(
            listOf(overridingMethod),
            originalMethod.overriddenBy,
        )
    }

    @Test
    @Throws(Exception::class)
    fun testCpp() {
        val result =
            analyze(listOf(Path.of(topLevel.toString(), "calls.cpp").toFile()), topLevel, true) {
                it.registerLanguage<CPPLanguage>()
                it.inferenceConfiguration(
                    InferenceConfiguration.builder().inferRecords(false).build()
                )
            }
        val tu = result.components.flatMap { it.translationUnits }.firstOrNull()
        assertNotNull(tu)

        val records = result.records

        val intType = tu.primitiveType("int")
        val stringType = tu.primitiveType("char").reference(PointerType.PointerOrigin.POINTER)
        testMethods(records, intType, stringType)
        testOverriding(records)

        // Test functions (not methods!)
        val functions =
            result.functions { it.name.localName == "functionTarget" && it !is MethodDeclaration }
        val calls = findByName(result.calls, "functionTarget")
        checkCalls(intType, stringType, functions, calls)
        ensureNoUnknownClassDummies(records)
        ensureInvocationOfMethodsInFunction(result)
    }

    @Test
    @Throws(Exception::class)
    fun testImplicitCastMethodCallResolution() {
        val result =
            analyze(
                listOf(
                    Path.of(topLevel.toString(), "implicitcast", "implicitCastInMethod.cpp")
                        .toFile()
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        val functionDeclarations = result.functions
        val callExpressions = result.calls

        // Check resolution of calc
        val calc = findByUniqueName(callExpressions, "calc")
        val calcFunctionDeclaration =
            findByUniquePredicate(functionDeclarations) { f: FunctionDeclaration ->
                f.name.localName == "calc" && !f.isInferred
            }
        assertEquals(1, calc.invokes.size)
        assertEquals(calcFunctionDeclaration, calc.invokes[0])
        assertLiteralValue(2.0, calc.arguments[0])

        // Check resolution of doSmth
        val doSmth = findByUniqueName(callExpressions, "doSmth")
        val doSmthFunctionDeclaration =
            findByUniquePredicate(functionDeclarations) { f: FunctionDeclaration ->
                f.name.localName == "doSmth" && !f.isInferred
            }
        assertEquals(1, doSmth.invokes.size)
        assertEquals(doSmthFunctionDeclaration, doSmth.invokes[0])
        assertLiteralValue(10.0, doSmth.arguments[0])
    }

    @Test
    @Throws(Exception::class)
    fun testImplicitCastCallResolution() {
        val result =
            analyze(
                listOf(
                    Path.of(topLevel.toString(), "implicitcast", "ambiguouscall.cpp").toFile(),
                    Path.of(topLevel.toString(), "implicitcast", "implicitcast.cpp").toFile(),
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        val callExpressions = result.calls

        // Check resolution of implicit cast
        val multiply = findByUniqueName(callExpressions, "multiply")
        assertEquals(1, multiply.invokes.size)

        val functionDeclaration = multiply.invokes[0]
        assertFalse(functionDeclaration.isInferred)
        assertEquals("int", functionDeclaration.signatureTypes[0].typeName)

        var arg = multiply.arguments.firstOrNull()
        assertIs<Literal<*>>(arg)
        assertLiteralValue(10.0, arg)

        // Check implicit cast in case of ambiguous call
        val ambiguousCall = findByUniqueName(callExpressions, "ambiguous_multiply")

        // Check invokes
        val functionDeclarations = ambiguousCall.invokes
        assertEquals(2, functionDeclarations.size)

        for (func in functionDeclarations) {
            assertFalse(func.isImplicit)
            assertTrue(
                func.parameters[0].type.name.localName == "int" ||
                    func.parameters[0].type.name.localName == "float"
            )
        }
        arg = ambiguousCall.arguments.firstOrNull()
        assertIs<Literal<*>>(arg)
        assertLiteralValue(10.0, arg)
    }

    @Test
    @Throws(Exception::class)
    fun testDefaultArgumentsInDeclaration() {
        val result =
            analyze(
                listOf(
                    Path.of(topLevel.toString(), "defaultargs", "defaultInDeclaration.cpp").toFile()
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        val calls = result.calls
        val functionDeclarations = result.functions
        val displayDeclaration =
            findByUniquePredicate(functionDeclarations) { f: FunctionDeclaration ->
                f.name.localName == "display" && !f.isDefinition && !f.isImplicit
            }
        val displayDefinition =
            findByUniquePredicate(functionDeclarations) { f: FunctionDeclaration ->
                f.name.localName == "display" && f.isDefinition && !f.isImplicit
            }

        // Check defines edge
        assertEquals(displayDefinition, displayDeclaration.definition)

        // Check defaults edge of ParameterDeclaration
        assertEquals(displayDeclaration.defaultParameters, displayDefinition.defaultParameters)

        // Check call display(1);
        val display1 =
            findByUniquePredicate(calls) { c: CallExpression ->
                assert(c.code != null)
                c.code == "display(1);"
            }

        assertEquals(1, display1.invokes.size)
        assertTrue(display1.invokes.contains(displayDefinition))
        assertEquals("1", display1.arguments[0].code)
        assertTrue(displayDeclaration.nextEOG.contains(displayDeclaration.defaultParameters[1]!!))
        assertTrue(displayDeclaration.nextEOG.contains(displayDeclaration.defaultParameters[0]!!))
        assertTrue(
            displayDeclaration.defaultParameters[0]
                ?.nextEOG
                ?.contains(displayDeclaration.defaultParameters[1]!!) == true
        )
        for (node in displayDeclaration.nextEOG) {
            assertTrue(
                node == displayDeclaration.defaultParameters[0] ||
                    node == displayDeclaration.defaultParameters[1] ||
                    displayDeclaration.defaultParameters[1]?.nextEOG?.contains(node) == true
            )
        }
        val display =
            findByUniquePredicate(calls) { c: CallExpression ->
                assert(c.code != null)
                c.code == "display();"
            }
        assertEquals(1, display.invokes.size)
        assertTrue(display.invokes.contains(displayDefinition))
        assertEquals(0, display.arguments.size)

        val displayCount =
            findByUniquePredicate(calls) { c: CallExpression ->
                assert(c.code != null)
                c.code == "display(count, '$');"
            }
        assertEquals(1, display.invokes.size)
        assertTrue(display.invokes.contains(displayDefinition))
        assertLocalName("count", displayCount.arguments[0])
        assertEquals("'$'", displayCount.arguments[1].code)

        val display10 =
            findByUniquePredicate(calls) { c: CallExpression ->
                assert(c.code != null)
                c.code == "display(10.0);"
            }
        assertEquals(1, display10.invokes.size)
        assertTrue(display.invokes.contains(displayDefinition))
        assertEquals(1, display10.arguments.size)
        assertLiteralValue(10.0, display10.arguments[0])
    }

    @Test
    @Throws(Exception::class)
    fun testDefaultArgumentsInDefinition() {
        val result =
            analyze(
                listOf(
                    Path.of(topLevel.toString(), "defaultargs", "defaultInDefinition.cpp").toFile()
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        val calls = result.calls
        val functionDeclarations = result.functions
        val displayFunction =
            findByUniquePredicate(functionDeclarations) { f: FunctionDeclaration ->
                f.name.localName == "display" && !f.isImplicit
            }
        val literalStar = findByUniquePredicate(result.literals) { it.value == '*' }
        val literal3 = findByUniquePredicate(result.literals) { it.value == 3 }
        // Check defaults edge of ParameterDeclaration
        assertTrue(displayFunction.defaultParameters[0] is Literal<*>)
        assertTrue(displayFunction.defaultParameters[1] is Literal<*>)
        assertEquals('*', (displayFunction.defaultParameters[0] as Literal<*>).value)
        assertEquals(3, (displayFunction.defaultParameters[1] as Literal<*>).value)

        // Check call display();
        val display =
            findByUniquePredicate(calls) { c: CallExpression ->
                assert(c.code != null)
                c.code == "display();"
            }
        assertEquals(1, display.invokes.size)
        assertEquals(displayFunction, display.invokes[0])
        assertEquals(0, display.arguments.size)
        assertTrue(displayFunction.nextEOG.contains(literalStar))
        assertTrue(displayFunction.nextEOG.contains(literal3))
        assertTrue(literalStar.nextEOG.contains(literal3))
        for (node in displayFunction.nextEOG) {
            assertTrue(node == literal3 || node == literalStar || literal3.nextEOG.contains(node))
        }

        // Check call display('#');
        val displayHash =
            findByUniquePredicate(calls) { c: CallExpression ->
                assert(c.code != null)
                c.code == "display('#');"
            }
        assertEquals(1, displayHash.invokes.size)
        assertEquals(displayFunction, displayHash.invokes[0])
        assertEquals(1, displayHash.arguments.size)
        assertTrue(displayHash.arguments[0] is Literal<*>)
        assertEquals('#', (displayHash.arguments[0] as Literal<*>).value)

        // Check call display('#');
        val displayCount =
            findByUniquePredicate(calls) { c: CallExpression ->
                assert(c.code != null)
                c.code == "display('$', count);"
            }
        assertEquals(1, displayCount.invokes.size)
        assertEquals(displayFunction, displayCount.invokes[0])
        assertTrue(displayCount.arguments[0] is Literal<*>)
        assertEquals('$', (displayCount.arguments[0] as Literal<*>).value)
        assertLocalName("count", displayCount.arguments[1])
    }

    @Test
    @Throws(Exception::class)
    fun testPartialDefaultArguments() {
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "defaultargs", "partialDefaults.cpp").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        val calls = result.calls
        val functionDeclarations = result.functions
        val addFunction =
            findByUniquePredicate(functionDeclarations) { f: FunctionDeclaration ->
                f.name.localName == "add" && !f.isInferred
            }
        val addFunctionInferred =
            findByUniquePredicate(functionDeclarations) { f: FunctionDeclaration ->
                f.name.localName == "add" && f.isInferred
            }

        // Check call add();
        val add =
            findByUniquePredicate(calls) { c: CallExpression ->
                assert(c.code != null)
                c.code == "add();"
            }
        assertEquals(1, add.invokes.size)
        assertEquals(addFunctionInferred, add.invokes[0])

        // Check call add(1, 2);
        val add12 =
            findByUniquePredicate(calls) { c: CallExpression ->
                assert(c.code != null)
                c.code == "add(1,2);"
            }
        assertEquals(1, add12.invokes.size)
        assertEquals(addFunction, add12.invokes[0])
        assertEquals(2, add12.arguments.size)
        assertEquals("1", add12.arguments[0].code)
        assertEquals("2", add12.arguments[1].code)
        assertTrue(addFunction.nextEOG.contains(addFunction.defaultParameters[2]!!))
        assertTrue(addFunction.nextEOG.contains(addFunction.defaultParameters[3]!!))
        assertTrue(
            addFunction.defaultParameters[2]
                ?.nextEOG
                ?.contains(addFunction.defaultParameters[3]!!) == true
        )
        for (node in addFunction.nextEOG) {
            assertTrue(
                node == addFunction.defaultParameters[2] ||
                    node == addFunction.defaultParameters[3] ||
                    addFunction.defaultParameters[3]?.nextEOG?.contains(node) == true
            )
        }

        // Check call add(1, 2, 5, 6);
        val add1256 =
            findByUniquePredicate(calls) { c: CallExpression ->
                assert(c.code != null)
                c.code == "add(1,2,5,6);"
            }
        assertEquals(1, add1256.invokes.size)
        assertEquals(addFunction, add1256.invokes[0])
        assertEquals(4, add1256.arguments.size)
        assertEquals("1", add1256.arguments[0].code)
        assertEquals("2", add1256.arguments[1].code)
        assertEquals("5", add1256.arguments[2].code)
        assertEquals("6", add1256.arguments[3].code)
    }

    @Test
    @Throws(Exception::class)
    fun testDefaultArgumentsMethodResolution() {
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "defaultargs", "defaultInMethod.cpp").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        val calls = result.calls
        val functionDeclarations = result.functions
        val declaredReferenceExpressions = result.refs

        // Check calc call
        val calc =
            findByUniquePredicate(functionDeclarations) {
                it.name.localName == "calc" && !it.isImplicit
            }
        val callCalc = findByUniquePredicate(calls) { it.name.localName == "calc" }
        val x = findByUniquePredicate(declaredReferenceExpressions) { it.name.localName == "x" }
        val literal5 = findByUniquePredicate(result.literals) { it.value == 5 }
        assertEquals(1, callCalc.invokes.size)
        assertEquals(calc, callCalc.invokes[0])
        assertEquals(x, callCalc.arguments[0])
        assertTrue(calc.nextEOG.contains(literal5))

        // Check doSmth call
        val doSmth =
            findByUniquePredicate(functionDeclarations) { f: FunctionDeclaration ->
                f.name.localName == "doSmth" && !f.isImplicit
            }
        val callDoSmth =
            findByUniquePredicate(calls) { f: CallExpression -> f.name.localName == "doSmth" }
        val literal1 = findByUniquePredicate(result.literals) { it.value == 1 }
        val literal2 = findByUniquePredicate(result.literals) { it.value == 2 }
        assertEquals(1, callDoSmth.invokes.size)
        assertEquals(doSmth, callDoSmth.invokes[0])
        assertTrue(doSmth.nextEOG.contains(literal1))
        assertTrue(doSmth.nextEOG.contains(literal2))
        assertTrue(literal1.nextEOG.contains(literal2))
    }

    @Test
    @Throws(Exception::class)
    fun testScopedFunctionResolutionUndefined() {
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "cxxprioresolution", "undefined.cpp").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        val calls = result.calls
        assertEquals(1, calls.size)

        val functionDeclarations = result.functions
        assertEquals(2, functionDeclarations.size)
        assertEquals(1, calls[0].invokes.size)
        assertLocalName("f", calls[0].invokes[0])
    }

    @Test
    @Throws(Exception::class)
    fun testScopedFunctionResolutionDefined() {
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "cxxprioresolution", "defined.cpp").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        val calls = result.calls
        assertEquals(1, calls.size)

        val functionDeclarations = result.functions
        assertEquals(2, functionDeclarations.size)
        assertEquals(1, calls[0].invokes.size)
        assertFalse(calls[0].invokes[0].isImplicit)
        assertLocalName("g", calls[0].invokes[0])
    }

    private fun testScopedFunctionResolutionFunctionGlobal(
        result: TranslationResult,
        calls: List<CallExpression>,
    ) {
        val fh =
            findByUniquePredicate(
                calls,
                Predicate { c: CallExpression -> c.location!!.region.startLine == 4 },
            )
        val literal7 = findByUniquePredicate(result.literals) { it.value == 7 }
        assertEquals(1, fh.invokes.size)
        assertFalse(fh.invokes[0].isImplicit)
        assertEquals(2, fh.invokes[0].location!!.region.startLine)
        assertEquals(1, fh.arguments.size)
        assertEquals(3, (fh.arguments[0] as Literal<*>).value)
        assertTrue(fh.invokes[0].nextEOG.contains(literal7))
        for (node in fh.invokes[0].nextEOG) {
            assertTrue(node == literal7 || literal7.nextEOG.contains(node))
        }
    }

    private fun testScopedFunctionResolutionRedeclaration(
        result: TranslationResult,
        calls: List<CallExpression>,
    ) {
        val fm1 =
            findByUniquePredicate(
                calls,
                Predicate { c: CallExpression -> c.location!!.region.startLine == 8 },
            )
        assertEquals(1, fm1.invokes.size)
        assertEquals(1, fm1.arguments.size)
        assertEquals(8, (fm1.arguments[0] as Literal<*>).value)

        val fm2 =
            findByUniquePredicate(
                calls,
                Predicate { c: CallExpression -> c.location!!.region.startLine == 10 },
            )
        val literal5 = findByUniquePredicate(result.literals) { it.value == 5 }
        assertEquals(1, fm2.invokes.size)
        assertEquals(9, fm2.invokes[0].location!!.region.startLine)
        assertEquals(1, fm2.arguments.size)
        assertEquals(4, (fm2.arguments[0] as Literal<*>).value)
        assertTrue(fm2.invokes[0].nextEOG.contains(literal5))
        for (node in fm2.invokes[0].nextEOG) {
            assertTrue(node == literal5 || literal5.nextEOG.contains(node))
        }
    }

    private fun testScopedFunctionResolutionAfterRedeclaration(
        result: TranslationResult,
        calls: List<CallExpression>,
    ) {
        val fn = findByUniquePredicate(calls, Predicate { it.location?.region?.startLine == 13 })
        val literal7 = findByUniquePredicate(result.literals) { it.value == 7 }
        assertEquals(1, fn.invokes.size)
        assertFalse(fn.invokes[0].isImplicit)
        assertEquals(2, fn.invokes[0].location!!.region.startLine)
        assertEquals(1, fn.arguments.size)
        assertEquals(6, (fn.arguments[0] as Literal<*>).value)
        assertTrue(fn.invokes[0].nextEOG.contains(literal7))
        for (node in fn.invokes[0].nextEOG) {
            assertTrue(node == literal7 || literal7.nextEOG.contains(node))
        }
    }

    @Test
    @Throws(Exception::class)
    fun testScopedFunctionResolutionWithDefaults() {
        val result =
            analyze(
                listOf(
                    Path.of(
                            topLevel.toString(),
                            "cxxprioresolution",
                            "scopedResolutionWithDefaults.cpp",
                        )
                        .toFile()
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        val calls = result.calls
        testScopedFunctionResolutionFunctionGlobal(result, calls)
        testScopedFunctionResolutionRedeclaration(result, calls)
        testScopedFunctionResolutionAfterRedeclaration(result, calls)
    }

    @Test
    @Throws(Exception::class)
    fun testCxxPrioResolutionWithMethods() {
        val result =
            analyze(
                listOf(
                    Path.of(
                            topLevel.toString(),
                            "cxxprioresolution",
                            "methodresolution",
                            "overloadedresolution.cpp",
                        )
                        .toFile()
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
            }
        val calls = result.calls
        val methodDeclarations = result.methods
        val calcOverload: FunctionDeclaration =
            findByUniquePredicate(methodDeclarations) { c: MethodDeclaration ->
                c.recordDeclaration!!.name.localName == "Overload" && c !is ConstructorDeclaration
            }

        // This call must resolve to implicit cast of the overloaded class and not to the base class
        val calcInt =
            findByUniquePredicate(calls) { c: CallExpression ->
                if (c.location != null) {
                    return@findByUniquePredicate c.location!!.region.startLine == 24
                }
                false
            }
        assertEquals(1, calcInt.invokes.size)
        assertEquals(calcOverload, calcInt.invokes[0])
        val calcDouble =
            findByUniquePredicate(calls) { c: CallExpression ->
                if (c.location != null) {
                    return@findByUniquePredicate c.location!!.region.startLine == 25
                }
                false
            }
        assertEquals(1, calcDouble.invokes.size)
        assertEquals(calcOverload, calcDouble.invokes[0])
        assertEquals(1.1, (calcDouble.arguments[0] as Literal<*>).value)
    }

    @Test
    @Throws(Exception::class)
    fun testCXXMethodResolutionDoNotStopOnFirstOccurrence() {
        val result =
            analyze(
                listOf(
                    Path.of(
                            topLevel.toString(),
                            "cxxprioresolution",
                            "methodresolution",
                            "overloadnoresolution.cpp",
                        )
                        .toFile()
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<CPPLanguage>()
                it.inferenceConfiguration(InferenceConfiguration.builder().enabled(false).build())
            }
        val calls = result.calls

        val calcCall =
            findByUniquePredicate(calls) {
                if (it.location != null) {
                    return@findByUniquePredicate it.location!!.region.startLine == 22
                }
                false
            }
        assertEquals(0, calcCall.invokes.size)
    }

    @Test
    @Throws(Exception::class)
    fun testCallWithIgnoredResult() {
        val file = File("src/test/resources/calls/ignore-return.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }

        // check for function declarations, we only want two: main and someFunction
        // we do NOT want any inferred/implicit function declarations that could exist, if
        // the call resolver would incorrectly assume that the call to someFunction is to another
        // function because of the missing return assignment
        val declarations = tu.declarations.filterIsInstance<FunctionDeclaration>()
        assertNotNull(declarations)
        assertEquals(2, declarations.size)
    }

    @Test
    @Throws(Exception::class)
    fun testEOGSymbolResolver() {
        val result =
            analyze(
                listOf(Path.of(topLevel.toString(), "symbols.cpp").toFile()),
                topLevel,
                usePasses = true,
            ) {
                it.configurePass<SymbolResolver>(
                    SymbolResolver.Configuration(experimentalEOGWorklist = true)
                )
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(result)

        result.refs.forEach { assertNotNull(it.refersTo) }
        result.calls.forEach { assertTrue(it.invokes.isNotEmpty()) }
    }

    companion object {
        private val topLevel = Path.of("src", "test", "resources", "calls")
    }
}
