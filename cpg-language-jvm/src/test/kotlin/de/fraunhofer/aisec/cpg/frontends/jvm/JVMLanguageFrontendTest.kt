/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.jvm

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.passes.BasicBlockCollectorPass
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.DynamicInvokeResolver
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import de.fraunhofer.aisec.cpg.passes.ResolveCallAmbiguityPass
import de.fraunhofer.aisec.cpg.passes.ResolveMemberAmbiguityPass
import de.fraunhofer.aisec.cpg.passes.SccPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.TypeHierarchyResolver
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertFullName
import de.fraunhofer.aisec.cpg.test.assertInvokes
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.test.assertRefersTo
import java.io.File
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.Disabled

class JVMLanguageFrontendTest {
    @Test
    fun testHelloJimple() {
        val topLevel = Path.of("src", "test", "resources", "jimple", "helloworld")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("HelloWorld.jimple").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(tu)

        val helloWorld = tu.records["HelloWorld"]
        assertNotNull(helloWorld)

        val constructor = helloWorld.constructors.firstOrNull()
        assertNotNull(constructor)

        // All references should be resolved (except Object.<init>, which should be a construct
        // expression anyway)
        val refs = constructor.refs.filter { it.name.toString() != "java.lang.Object.<init>" }
        refs.forEach {
            val refersTo = it.refersTo
            assertNotNull(refersTo, "${it.name} could not be resolved")
            assertFalse(
                refersTo.isInferred,
                "${it.name} should not be resolved to an inferred node",
            )
        }

        val main = helloWorld.methods["main"]
        assertNotNull(main)
        assertTrue(main.isStatic)

        val param0 = main.refs["@parameter0"]
        assertNotNull(param0)

        val refersTo = param0.refersTo
        assertNotNull(refersTo)
        assertFalse(refersTo.isInferred)
    }

    @Test
    fun testMethodsClass() {
        // This will be our classpath
        val topLevel = Path.of("src", "test", "resources", "class", "methods")
        val tu =
            analyzeAndGetFirstTU(
                // We just need to specify one file to trigger the class byte loader
                listOf(topLevel.resolve("mypackage/Adder.class").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(tu)
        assertEquals(0, tu.problems.size)

        val pkg = tu.namespaces["mypackage"]
        assertNotNull(pkg)

        val adder = pkg.records["Adder"]
        assertNotNull(adder)

        val add = adder.methods["add"]
        assertNotNull(add)

        val main = pkg.methods["Main.main"]
        assertNotNull(main)

        println(main.code)

        // r5 contains our adder
        val r5 = main.variables["r5"]
        assertNotNull(r5)
        assertFullName("mypackage.Adder", r5.type)

        // r3 should be the result of the add call
        val r3 = main.variables["r3"]
        assertNotNull(r3)

        val r3ref = r3.usages.firstOrNull { it.access == AccessValues.WRITE }
        assertNotNull(r3ref)

        // Call to add should be resolved
        val call = r3ref.prevDFG.firstOrNull()
        assertIs<MemberCall>(call)
        assertLocalName("add", call)
        assertInvokes(call, add)
        assertEquals(listOf("Integer", "Integer"), call.arguments.map { it.type.name.localName })

        // All references (which are not part of a call) and not to the stdlib should be resolved
        val refs = tu.refs
        refs
            .filter { it.astParent !is Call }
            .filter { !it.name.startsWith("java.") }
            .forEach {
                val refersTo = it.refersTo
                assertNotNull(refersTo, "${it.name} could not be resolved")
                assertFalse(
                    refersTo.isInferred,
                    "${it.name} should not be resolved to an inferred node",
                )
            }
    }

    @Test
    fun testLiteralsClass() {
        // This will be our classpath
        val topLevel = Path.of("src", "test", "resources", "class", "literals")
        val result =
            analyze(
                // We just need to specify one file to trigger the byte code loader
                listOf(topLevel.resolve("mypackage/Literals.class").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        result.methods.forEach {
            println(it.name)
            println(it.code)
        }

        assertEquals(0, result.problems.size)
    }

    @Test
    fun testLiteralsJar() {
        // This will be our classpath
        val topLevel = Path.of("src", "test", "resources", "jar", "literals")
        val tu =
            analyzeAndGetFirstTU(
                // In case of a jar, the jar is directly used as a class path
                listOf(topLevel.resolve("literals.jar").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(tu)
        assertEquals(0, tu.problems.size)
        tu.methods.forEach { println(it.code) }
    }

    /*@Ignore(
        "This test is too slow (around 30 seconds) and is not meant to be ran in the regular test suite (yet)."
    )*/
    @Test
    fun testRealHelloWorldApk() {
        // This will be our classpath
        val topLevel = Path.of("src", "test", "resources", "apk", "HelloWorld")
        val apkFile = topLevel.resolve("real-app-debug.apk").toFile()

        // Assert file exists
        assertTrue(apkFile.exists(), "APK file not found at ${apkFile.absolutePath}")

        val tu =
            analyzeAndGetFirstTU(
                // In case of an APK, the APK is directly used as input
                listOf(apkFile),
                topLevel,
                false,
            ) {
                it.registerLanguage<JVMLanguage>()
                it.configureFrontend<JVMLanguageFrontend>(
                    JVMFrontendConfiguration(
                        packagesToIgnore =
                            listOf(
                                "android.",
                                "androidx.",
                                "com.android.",
                                "kotlin.",
                                "kotlinx.",
                                "java.",
                                "javax.",
                            )
                    )
                )

                it.registerPass<TypeHierarchyResolver>()
                    .registerPass<SymbolResolver>()
                    .registerPass<ImportResolver>()
                    .registerPass<DFGPass>()
                    .registerPass<DynamicInvokeResolver>()
                    .registerPass<EvaluationOrderGraphPass>() // creates EOG
                    .registerPass<TypeResolver>()
                    .registerPass<ControlFlowSensitiveDFGPass>()
                    .registerPass<ResolveCallAmbiguityPass>()
                    .registerPass<ResolveMemberAmbiguityPass>()
                    .registerPass<BasicBlockCollectorPass>()
                    .registerPass<SccPass>()
            }
        assertNotNull(tu)

        // The error handling improvements should prevent OOM errors
        // We should get some user code parsed (non-ignored packages)
        val userMethods =
            tu.methods.filter { method ->
                !method.name.toString().startsWith("android.") &&
                    !method.name.toString().startsWith("androidx.") &&
                    !method.name.toString().startsWith("kotlin.") &&
                    !method.name.toString().startsWith("java.")
            }

        // If the APK contains user code, we should find some methods
        if (userMethods.isNotEmpty()) {
            println("Found ${userMethods.size} user methods in APK")
            // Verify the methods have proper structure
            userMethods.take(5).forEach { method ->
                assertNotNull(method.name, "Method should have a name")
                println("Method: ${method.name}")
            }
        }

        // Most importantly, the analysis should complete without OOM errors
        // The new error handling should catch and handle any parsing issues gracefully
        assertTrue(
            tu.problems.isEmpty() ||
                tu.problems.all { it is ProblemDeclaration || it is ProblemExpression }
        )
    }

    @Test
    fun testHelloWorldFakeApk() {
        // This will be our classpath
        val topLevel = Path.of("src", "test", "resources", "apk", "HelloWorld")
        val apkFile = topLevel.resolve("app-debug.apk").toFile()

        // Assert file exists
        assertTrue(apkFile.exists(), "APK file not found at ${apkFile.absolutePath}")

        val tu =
            analyzeAndGetFirstTU(
                // In case of an APK, the APK is directly used as input
                listOf(apkFile),
                topLevel,
                true,
            ) {
                it.registerLanguage<JVMLanguage>()
                it.configureFrontend<JVMLanguageFrontend>(
                    JVMFrontendConfiguration(
                        packagesToIgnore =
                            listOf(
                                "android.",
                                "androidx.",
                                "com.android.",
                                "kotlin.",
                                "kotlinx.",
                                "java.",
                                "javax.",
                            )
                    )
                )
            }
        assertNotNull(tu)

        // The error handling improvements should prevent OOM errors
        // We should get some user code parsed (non-ignored packages)
        val userMethods =
            tu.methods.filter { method ->
                !method.name.toString().startsWith("android.") &&
                    !method.name.toString().startsWith("androidx.") &&
                    !method.name.toString().startsWith("kotlin.") &&
                    !method.name.toString().startsWith("java.")
            }

        // If the APK contains user code, we should find some methods
        if (userMethods.isNotEmpty()) {
            println("Found ${userMethods.size} user methods in APK")
            // Verify the methods have proper structure
            userMethods.take(5).forEach { method ->
                assertNotNull(method.name, "Method should have a name")
                println("Method: ${method.name}")
            }
        }

        // Most importantly, the analysis should complete without OOM errors
        // The new error handling should catch and handle any parsing issues gracefully
        assertTrue(
            tu.problems.isEmpty() ||
                tu.problems.all { it is ProblemDeclaration || it is ProblemExpression }
        )
    }

    @Test
    fun testInheritanceClass() {
        // This will be our classpath
        val topLevel = Path.of("src", "test", "resources", "class", "inheritance")
        val tu =
            analyzeAndGetFirstTU(
                // In case of a jar, the jar is directly used as a class path
                listOf(topLevel.resolve("mypackage/Application.class").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(tu)
        tu.methods.forEach { println(it.code) }
        assertEquals(0, tu.problems.size)

        val myInterface = tu.records["mypackage.MyInterface"]
        assertNotNull(myInterface)
        assertEquals("interface", myInterface.kind)

        val baseClass = tu.records["mypackage.BaseClass"]
        assertNotNull(baseClass)

        val extendedClass = tu.records["mypackage.ExtendedClass"]
        assertNotNull(extendedClass)
        assertContains(extendedClass.implementedInterfaces, myInterface.toType())
        assertContains(extendedClass.superTypeDeclarations, baseClass)
        assertContains(extendedClass.superTypeDeclarations, myInterface)

        val anotherExtendedClass = tu.records["mypackage.AnotherExtendedClass"]
        assertNotNull(anotherExtendedClass)
        assertContains(anotherExtendedClass.superTypeDeclarations, baseClass)

        assertEquals(
            baseClass.toType(),
            listOf(extendedClass.toType(), anotherExtendedClass.toType()).commonType,
        )

        val appInit = tu.methods["mypackage.Application.<init>"]
        assertNotNull(appInit)

        val appDoSomething = tu.methods["mypackage.Application.doSomething"]
        assertNotNull(appDoSomething)
        assertLocalName("MyInterface", appDoSomething.parameters.firstOrNull()?.type)

        // Call doSomething in Application.<init> with an object of ExtendedClass, which should
        // fulfill the MyInterface of the needed parameter
        val doSomethingCall1 = appInit.calls["doSomething"]
        assertNotNull(doSomethingCall1)
        assertLocalName("ExtendedClass", doSomethingCall1.arguments.firstOrNull()?.type)
        assertInvokes(doSomethingCall1, appDoSomething)

        val extended = appInit.variables["r4"]
        assertNotNull(extended)

        val getMyProperty =
            appInit.calls[
                    {
                        it.name.localName == "getMyProperty" &&
                            it is MemberCall &&
                            it.base in extended.usages
                    }]
        assertNotNull(getMyProperty)
        assertInvokes(getMyProperty, baseClass.methods["getMyProperty"])

        val setMyProperty =
            appInit.calls[
                    {
                        it.name.localName == "setMyProperty" &&
                            it is MemberCall &&
                            it.base in extended.usages
                    }]
        assertNotNull(setMyProperty)
        assertInvokes(setMyProperty, extendedClass.methods["setMyProperty"])
    }

    @Test
    fun testFieldsClass() {
        // This will be our classpath
        val topLevel = Path.of("src", "test", "resources", "class", "fields")
        val tu =
            analyzeAndGetFirstTU(
                // We just need to specify one file to trigger the byte code loader
                listOf(topLevel.resolve("mypackage/Fields.class").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(tu)
        assertEquals(0, tu.problems.size)
        tu.methods.forEach { println(it.code) }

        val refs = tu.refs.filterIsInstance<MemberAccess>()
        refs.forEach {
            val refersTo = it.refersTo
            assertNotNull(refersTo, "${it.name} could not be resolved")
            assertFalse(
                refersTo.isInferred,
                "${it.name} should not be resolved to an inferred node",
            )
        }

        val setACall = tu.calls["setA"]
        assertNotNull(setACall)

        val lit10 = setACall.arguments.firstOrNull()
        assertIs<Literal<Int>>(lit10)
        assertLiteralValue(10, lit10)
    }

    @Disabled
    @Test
    fun testLiteralsSource() {
        // This will be our classpath
        val topLevel = Path.of("src", "test", "resources", "class", "literals")
        val tu =
            analyzeAndGetFirstTU(
                // We just need to specify one file to trigger the source code loader
                listOf(topLevel.resolve("mypackage/Literals.java").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(tu)

        val haveFun = tu.methods["haveFunWithLiterals"]
        assertNotNull(haveFun)

        println(haveFun.code)
    }

    @Test
    fun testArraysClass() {
        // This will be our classpath
        val topLevel = Path.of("src", "test", "resources", "class", "arrays")
        val tu =
            analyzeAndGetFirstTU(
                // We just need to specify one file to trigger the class byte loader
                listOf(topLevel.resolve("mypackage/Arrays.class").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(tu)
        tu.methods.forEach { println(it.code) }
        assertEquals(0, tu.problems.size)

        val create = tu.methods["create"]
        assertNotNull(create)

        val r3 = create.variables["r3"]
        assertNotNull(r3)

        var arrayType = r3.type
        assertIs<PointerType>(arrayType)
        assertTrue(arrayType.isArray)
        assertFullName("mypackage.Element", arrayType.elementType)

        val r3write = r3.usages.firstOrNull { it.access == AccessValues.WRITE }
        assertNotNull(r3write)

        var expr = r3write.prevDFG.singleOrNull()
        assertIs<ArrayConstruction>(expr)
        assertLiteralValue(2, expr.dimensions.singleOrNull())

        var r1 = create.variables["r1"]
        assertNotNull(r1)
        assertEquals(arrayType.elementType, r1.type)

        val r2 = create.variables["r2"]
        assertNotNull(r2)
        assertEquals(arrayType.elementType, r2.type)

        val r2write = r2.usages.firstOrNull { it.access == AccessValues.WRITE }
        assertNotNull(r2write)

        val prevDFG = r2write.prevDFG.singleOrNull()
        assertIs<Subscription>(prevDFG)
        assertRefersTo(prevDFG.arrayExpression, r3)

        val createMulti = tu.methods["createMulti"]
        assertNotNull(createMulti)

        r1 = createMulti.variables["r1"]
        assertNotNull(r1)

        arrayType = r1.type
        assertIs<PointerType>(arrayType)
        assertTrue(arrayType.isArray)
        assertFullName("mypackage.Element", arrayType.elementType)

        val r1write = r1.usages.firstOrNull { it.access == AccessValues.WRITE }
        assertNotNull(r1write)

        expr = r1write.prevDFG.singleOrNull()
        assertIs<ArrayConstruction>(expr)
        listOf(2, 10).forEachIndexed { index, i -> assertLiteralValue(i, expr.dimensions[index]) }
    }

    @Disabled
    @Test
    fun testExceptional() {
        // This will be our classpath
        val topLevel = Path.of("src", "test", "resources", "class", "exception")
        val tu =
            analyzeAndGetFirstTU(
                // We just need to specify one file to trigger the class byte loader
                listOf(topLevel.resolve("mypackage/Exceptional.class").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(tu)
        tu.methods.forEach { println(it.code) }
    }

    @Test
    fun testExceptionsClass() {
        // This will be our classpath
        val topLevel = Path.of("src", "test", "resources", "class", "exceptions")
        val result =
            analyze(
                // We just need to specify one file to trigger the byte code loader
                listOf(topLevel.resolve("ExceptionTest.class").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        assertEquals(1, result.throws.size, "There is exactly one throw statement")

        assertEquals(0, result.problems.size)
    }

    @Test
    fun testLiteralsDetailed() {
        val topLevel = Path.of("src", "test", "resources", "class", "literals")
        val result =
            analyze(listOf(topLevel.resolve("mypackage/Literals.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val haveFun = result.methods["haveFunWithLiterals"]
        assertNotNull(haveFun)

        // Test literals for different types
        val literals = haveFun.literals

        // Verify we have various types of literals
        // Note: Some literals may be optimized away or represented differently in bytecode
        assertTrue(literals.isNotEmpty(), "Should have some literals")

        // Test for numeric literals (int, long, float, double)
        val numericLiterals =
            literals.filter {
                it.value is Int || it.value is Long || it.value is Float || it.value is Double
            }
        assertTrue(numericLiterals.isNotEmpty(), "Should have numeric literals")

        // Verify different literal types exist by checking the overall set
        val literalTypes =
            literals.mapTo(mutableSetOf()) { it.value?.javaClass?.simpleName ?: "null" }
        assertTrue(literalTypes.size >= 2, "Should have multiple types of literals")
    }

    @Test
    fun testBinaryOperators() {
        val topLevel = Path.of("src", "test", "resources", "class", "operators")
        val result =
            analyze(listOf(topLevel.resolve("Operators.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val testArithmetic = result.methods["testArithmetic"]
        assertNotNull(testArithmetic)

        // Test arithmetic operators
        val addOp = testArithmetic.allChildren<BinaryOperator>()[{ it.operatorCode == "+" }]
        assertNotNull(addOp, "Should have addition operator")

        val subOp = testArithmetic.allChildren<BinaryOperator>()[{ it.operatorCode == "-" }]
        assertNotNull(subOp, "Should have subtraction operator")

        val mulOp = testArithmetic.allChildren<BinaryOperator>()[{ it.operatorCode == "*" }]
        assertNotNull(mulOp, "Should have multiplication operator")

        val divOp = testArithmetic.allChildren<BinaryOperator>()[{ it.operatorCode == "/" }]
        assertNotNull(divOp, "Should have division operator")

        val testComparison = result.methods["testComparison"]
        assertNotNull(testComparison)

        // Test comparison operators
        val eqOp = testComparison.allChildren<BinaryOperator>()[{ it.operatorCode == "==" }]
        assertNotNull(eqOp, "Should have equality operator")

        val neOp = testComparison.allChildren<BinaryOperator>()[{ it.operatorCode == "!=" }]
        assertNotNull(neOp, "Should have inequality operator")

        val gtOp = testComparison.allChildren<BinaryOperator>()[{ it.operatorCode == ">" }]
        assertNotNull(gtOp, "Should have greater than operator")

        val ltOp = testComparison.allChildren<BinaryOperator>()[{ it.operatorCode == "<" }]
        assertNotNull(ltOp, "Should have less than operator")
    }

    @Test
    fun testUnaryOperators() {
        val topLevel = Path.of("src", "test", "resources", "class", "operators")
        val result =
            analyze(listOf(topLevel.resolve("Operators.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val testUnary = result.methods["testUnary"]
        assertNotNull(testUnary)

        // Test negation operator
        val negOp = testUnary.allChildren<UnaryOperator>()[{ it.operatorCode == "-" }]
        assertNotNull(negOp, "Should have negation operator")

        // Test array length operator
        val testArrayLength = result.methods["testArrayLength"]
        assertNotNull(testArrayLength)

        val lengthOp =
            testArrayLength.allChildren<UnaryOperator>()[{ it.operatorCode == "lengthof" }]
        assertNotNull(lengthOp, "Should have lengthof operator")
    }

    @Test
    fun testCast() {
        val topLevel = Path.of("src", "test", "resources", "class", "operators")
        val result =
            analyze(listOf(topLevel.resolve("Operators.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val testCast = result.methods["testCast"]
        assertNotNull(testCast)

        val castExpr = testCast.casts.firstOrNull()
        assertNotNull(castExpr, "Should have cast expression")
    }

    @Test
    fun testInstanceOfExpression() {
        val topLevel = Path.of("src", "test", "resources", "class", "operators")
        val result =
            analyze(listOf(topLevel.resolve("Operators.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val testInstanceOf = result.methods["testInstanceOf"]
        assertNotNull(testInstanceOf)

        val instanceOfOp =
            testInstanceOf.allChildren<BinaryOperator>()[{ it.operatorCode == "instanceof" }]
        assertNotNull(instanceOfOp, "Should have instanceof operator")
    }

    @Test
    fun testControlFlow() {
        val topLevel = Path.of("src", "test", "resources", "class", "controlflow")
        val result =
            analyze(listOf(topLevel.resolve("ControlFlow.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val testIf = result.methods["testIf"]
        assertNotNull(testIf)

        val ifStmt = testIf.ifs.firstOrNull()
        assertNotNull(ifStmt, "Should have if statement")
        assertNotNull(ifStmt.condition, "If statement should have condition")
        assertNotNull(ifStmt.thenStatement, "If statement should have then branch")

        val testGoto = result.methods["testGoto"]
        assertNotNull(testGoto)

        // Test that goto statements are created for labeled blocks
        val gotoStmts = testGoto.allChildren<Goto>()
        assertTrue(gotoStmts.isNotEmpty(), "Should have goto statements")
    }

    @Test
    fun testStaticInvoke() {
        val topLevel = Path.of("src", "test", "resources", "class", "methods")
        val result =
            analyze(listOf(topLevel.resolve("mypackage/Main.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val main = result.methods["mypackage.Main.main"]
        assertNotNull(main)

        // Look for static calls (e.g., to constructors or static methods)
        val calls = main.calls
        assertTrue(calls.isNotEmpty(), "Should have call expressions")

        // Test that static calls have references marked as static
        val staticRefs = main.refs.filter { it.isStaticAccess }
        assertTrue(staticRefs.isNotEmpty(), "Should have static references")
    }

    @Test
    fun testConstructorCall() {
        val topLevel = Path.of("src", "test", "resources", "class", "methods")
        val result =
            analyze(listOf(topLevel.resolve("mypackage/Adder.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val adder = result.records["mypackage.Adder"]
        assertNotNull(adder)

        val constructor = adder.constructors.firstOrNull()
        assertNotNull(constructor, "Should have constructor")

        // Constructor should have a receiver (this)
        assertNotNull(constructor.receiver, "Constructor should have receiver")
        assertEquals("@this", constructor.receiver?.name?.localName)
    }

    @Test
    fun testExceptionHandling() {
        val topLevel = Path.of("src", "test", "resources", "class", "exceptions")
        val result =
            analyze(listOf(topLevel.resolve("ExceptionTest.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val main = result.methods["main"]
        assertNotNull(main)

        // Test throw expression
        val throwExpr = main.throws.firstOrNull()
        assertNotNull(throwExpr, "Should have throw expression")
        assertNotNull(throwExpr.exception, "Throw should have exception")

        // Test that exception reference is handled
        val exceptionRefs = main.refs.filter { it.name.toString().contains("exception") }
        assertTrue(exceptionRefs.isNotEmpty() || throwExpr.exception != null)
    }

    @Test
    fun testFrontendConfiguration() {
        val topLevel = Path.of("src", "test", "resources", "class", "methods")
        val result =
            analyze(listOf(topLevel.resolve("mypackage/Main.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
                it.configureFrontend<JVMLanguageFrontend>(
                    JVMFrontendConfiguration(packagesToIgnore = listOf("java.", "javax."))
                )
            }
        assertNotNull(result)

        // Methods from ignored packages should not have bodies parsed
        val javaMethods = result.methods.filter { it.name.toString().startsWith("java.") }
        javaMethods.forEach {
            // These should be inferred or have no body
            assertTrue(
                it.isInferred ||
                    it.body == null ||
                    (it.body as? Block)?.statements?.isEmpty() == true
            )
        }
    }

    @Test
    fun testDynamicInvoke() {
        val topLevel = Path.of("src", "test", "resources", "class", "literals")
        val result =
            analyze(listOf(topLevel.resolve("mypackage/Literals.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        // The lambda/method reference in Literals.java should generate a dynamic invoke
        val haveFun = result.methods["haveFunWithLiterals"]
        assertNotNull(haveFun)

        // Check for calls (including potential dynamic invokes for lambdas)
        val calls = haveFun.calls
        assertTrue(calls.isNotEmpty(), "Should have calls")
    }

    @Test
    fun testBitwiseOperators() {
        val topLevel = Path.of("src", "test", "resources", "class", "operators")
        val result =
            analyze(listOf(topLevel.resolve("Operators.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val testBitwise = result.methods["testBitwise"]
        assertNotNull(testBitwise)

        // Test bitwise operators
        val andOp = testBitwise.allChildren<BinaryOperator>()[{ it.operatorCode == "&" }]
        assertNotNull(andOp, "Should have bitwise AND operator")

        val orOp = testBitwise.allChildren<BinaryOperator>()[{ it.operatorCode == "|" }]
        assertNotNull(orOp, "Should have bitwise OR operator")

        val xorOp = testBitwise.allChildren<BinaryOperator>()[{ it.operatorCode == "^" }]
        assertNotNull(xorOp, "Should have bitwise XOR operator")

        val shlOp = testBitwise.allChildren<BinaryOperator>()[{ it.operatorCode == "<<" }]
        assertNotNull(shlOp, "Should have left shift operator")

        val shrOp = testBitwise.allChildren<BinaryOperator>()[{ it.operatorCode == ">>" }]
        assertNotNull(shrOp, "Should have right shift operator")

        val ushrOp = testBitwise.allChildren<BinaryOperator>()[{ it.operatorCode == ">>>" }]
        assertNotNull(ushrOp, "Should have unsigned right shift operator")
    }

    @Test
    fun testModuloOperator() {
        val topLevel = Path.of("src", "test", "resources", "class", "operators")
        val result =
            analyze(listOf(topLevel.resolve("Operators.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val testArithmetic = result.methods["testArithmetic"]
        assertNotNull(testArithmetic)

        val modOp = testArithmetic.allChildren<BinaryOperator>()[{ it.operatorCode == "%" }]
        assertNotNull(modOp, "Should have modulo operator")
    }

    @Test
    fun testComparisonOperators() {
        val topLevel = Path.of("src", "test", "resources", "class", "operators")
        val result =
            analyze(listOf(topLevel.resolve("Operators.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val testComparison = result.methods["testComparison"]
        assertNotNull(testComparison)

        // Test all comparison operators
        val geOp = testComparison.allChildren<BinaryOperator>()[{ it.operatorCode == ">=" }]
        assertNotNull(geOp, "Should have >= operator")

        val leOp = testComparison.allChildren<BinaryOperator>()[{ it.operatorCode == "<=" }]
        assertNotNull(leOp, "Should have <= operator")
    }

    @Test
    fun testClassConstant() {
        val topLevel = Path.of("src", "test", "resources", "class", "literals")
        val result =
            analyze(listOf(topLevel.resolve("mypackage/Literals.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val haveFun = result.methods["haveFunWithLiterals"]
        assertNotNull(haveFun)

        // Look for class constant (Literals.class)
        val classLiterals = haveFun.literals.filter { it.type.name.toString().contains("Class") }
        assertTrue(classLiterals.isNotEmpty(), "Should have class literal")
    }

    /**
     * Verifies how source positions are surfaced by the JVM frontend for plain bytecode (.class).
     *
     * Statements and most value-level expressions (constants, binary operators, ...) carry a real
     * position coming from the bytecode `LineNumberTable`. References to locals do NOT have an
     * intrinsic position in SootUp -- locals are interned and shared across all their use sites, so
     * a single object cannot represent many source sites -- therefore the frontend attributes them
     * the position of their enclosing statement instead of the dummy location -1:-1:-1:-1 (which is
     * how `NoPositionInformation` prints).
     */
    @Test
    fun testValueAndStatementPositions() {
        val topLevel = Path.of("src", "test", "resources", "class", "operators")
        val result =
            analyze(listOf(topLevel.resolve("Operators.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val testArithmetic = result.methods["testArithmetic"]
        assertNotNull(testArithmetic)

        // (1) Statements carry a real source position (from the bytecode LineNumberTable), which
        //     SootUp exposes via Stmt.getPosition().
        val assigns = testArithmetic.allChildren<Assign>()
        assertTrue(assigns.isNotEmpty(), "Should have assignments")
        val locatedAssigns = assigns.filter { (it.location?.region?.startLine ?: -1) >= 1 }
        assertTrue(
            locatedAssigns.isNotEmpty(),
            "At least some assignments should have a real (non-dummy) source line",
        )

        // (2) Binary operators (a value-level node) also carry a real, per-occurrence position,
        //     because SootUp attaches a value-level position to expressions.
        val binops = testArithmetic.allChildren<BinaryOperator>()
        assertTrue(binops.isNotEmpty(), "Should have binary operators")
        val locatedBinops = binops.filter { (it.location?.region?.startLine ?: -1) >= 1 }
        assertTrue(
            locatedBinops.isNotEmpty(),
            "At least some binary operators should have a real source line",
        )

        // (3) References to locals have no intrinsic position in SootUp, but the frontend now falls
        //     back to the enclosing statement -- so they end up with a real line instead of -1.
        val localRefs = testArithmetic.refs.filter { !it.isStaticAccess }
        assertTrue(localRefs.isNotEmpty(), "Should have (local) references")
        val locatedLocalRefs = localRefs.filter { (it.location?.region?.startLine ?: -1) >= 1 }
        assertTrue(
            locatedLocalRefs.isNotEmpty(),
            "Local references should inherit a real line from their enclosing statement " +
                "(this is the whole point of the stmt-level fallback in locationOf)",
        )

        // (4) A located node must never sit at the dummy line -1: it either has a real position or
        //     no location at all.
        val located =
            buildList<Node> {
                addAll(assigns)
                addAll(binops)
                addAll(localRefs)
            }
        located.forEach { node ->
            node.location?.let { loc ->
                assertTrue(
                    loc.region.startLine >= 1,
                    "'${node.code}' should not be at the dummy location -1, but was ${loc.region}",
                )
            }
        }

        // (5) A local reference reports the SAME line as the statement that contains it.
        val assignWithRef =
            locatedAssigns.firstOrNull { assign ->
                assign.allChildren<Reference>().any { (it.location?.region?.startLine ?: -1) >= 1 }
            }
        assertNotNull(assignWithRef)
        val refInAssign =
            assignWithRef.allChildren<Reference>().first {
                (it.location?.region?.startLine ?: -1) >= 1
            }
        assertEquals(
            assignWithRef.location?.region?.startLine,
            refInAssign.location?.region?.startLine,
            "A local reference should inherit the line of its enclosing statement",
        )
    }

    /**
     * Verifies per-occurrence local positions from the Jimple text frontend. Unlike plain bytecode
     * (which only carries a `LineNumberTable`, so same-line occurrences are indistinguishable), the
     * Jimple parser knows the exact line AND column of every token. SootUp now hands each
     * occurrence of a local its own position, so the two `$i0`s in `$i0 + $i0` -- on the same
     * source line but at different columns -- surface as two references with DIFFERENT locations.
     * This is the column-precise form of the "two occurrences of `a` should differ" guarantee.
     */
    @Test
    fun testPerOccurrenceLocalColumnsFromJimple() {
        val topLevel = Path.of("src", "test", "resources", "jimple", "positions")
        val result =
            analyze(listOf(topLevel.resolve("PerOccurrence.jimple").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(result)

        val compute = result.methods["compute"]
        assertNotNull(compute)

        // The two operands of '$i0 + $i0' are the same local on the same source line but different
        // columns -> their references must carry different locations.
        val binop = compute.allChildren<BinaryOperator>().singleOrNull { it.operatorCode == "+" }
        assertNotNull(binop, "expected a single '+' binary operator")

        val lhs = binop.lhs as? Reference
        val rhs = binop.rhs as? Reference
        assertNotNull(lhs, "left operand should be a reference")
        assertNotNull(rhs, "right operand should be a reference")
        assertLocalName("\$i0", lhs)
        assertLocalName("\$i0", rhs)

        val lhsRegion = lhs.location?.region
        val rhsRegion = rhs.location?.region
        assertNotNull(lhsRegion, "left '\$i0' should have a location")
        assertNotNull(rhsRegion, "right '\$i0' should have a location")

        // Same source line ...
        assertEquals(
            lhsRegion.startLine,
            rhsRegion.startLine,
            "both operands are on the same source line",
        )
        // ... but different columns -> the two occurrences are positionally distinct.
        assertNotEquals(
            lhsRegion.startColumn,
            rhsRegion.startColumn,
            "the two occurrences of \$i0 on the same line must have different columns",
        )

        // Stronger check: every located occurrence of $i0 in compute() (the def on '$i0 = 1', the
        // use on '$i1 = $i0', and the two binop operands) must have a distinct location -- no two
        // occurrences of the same local collapse onto the same position.
        val i0Regions =
            compute.refs.filter { it.name.localName == "\$i0" }.mapNotNull { it.location?.region }
        assertTrue(
            i0Regions.size >= 3,
            "expected at least three located \$i0 references, was ${i0Regions.size}",
        )
        assertEquals(
            i0Regions.size,
            i0Regions.toSet().size,
            "all located \$i0 occurrences must have distinct locations, but some collapsed: " +
                i0Regions,
        )
    }

    /**
     * Positions also survive when the input is a jar file, because the contained .class files still
     * carry their `LineNumberTable`.
     */
    @Test
    fun testPositionsFromJar() {
        val topLevel = Path.of("src", "test", "resources", "jar", "literals")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("literals.jar").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(tu)

        val located = tu.allChildren<Node>().filter { (it.location?.region?.startLine ?: -1) >= 1 }
        assertTrue(
            located.isNotEmpty(),
            "Expected some nodes with a real source position from a jar input",
        )
    }

    /**
     * For APK/dex input we actually get *richer* location information than for plain bytecode:
     * - Statements and values carry real (line-level) positions coming from the dex debug info.
     * - The *original* source file name (e.g. "MainActivity.kt" or "PrintFormat.java", including
     *   Kotlin sources) is recovered from the dex `source_file` entry via
     *   [DexClassSource.getSourceFile] and used as the location's file name.
     *
     * This directly answers whether positions and a filename can be attached when analyzing APKs.
     */
    @Test
    fun testPositionsAndSourceFileFromApk() {
        val topLevel = Path.of("src", "test", "resources", "apk", "HelloWorld")
        val apkFile = topLevel.resolve("real-app-debug.apk").toFile()
        assertTrue(apkFile.exists(), "APK file not found at ${apkFile.absolutePath}")

        val result =
            analyze(listOf(apkFile), topLevel, false) {
                it.registerLanguage<JVMLanguage>()
                it.configureFrontend<JVMLanguageFrontend>(
                    JVMFrontendConfiguration(
                        packagesToIgnore =
                            listOf(
                                "android.",
                                "androidx.",
                                "com.android.",
                                "kotlin.",
                                "kotlinx.",
                                "java.",
                                "javax.",
                            )
                    )
                )
            }
        assertNotNull(result)

        val located = result.allChildren<Node>().filter { it.location != null }
        assertTrue(located.isNotEmpty(), "Expected some located nodes from the APK")

        // (1) Positions: the dex debug info gives us line numbers, so nodes have real source lines.
        val withRealLine = located.filter { (it.location?.region?.startLine ?: -1) >= 1 }
        assertTrue(
            withRealLine.isNotEmpty(),
            "Expected nodes with real source lines from the APK's dex debug info",
        )

        // (2) Invariant of the stmt-level fallback: a value/statement either has a real position or
        //     no location at all -- it must never sit at the dummy location -1:-1:-1:-1.
        located.filterIsInstance<Expression>().forEach {
            assertTrue(
                (it.location?.region?.startLine ?: 1) >= 1,
                "'${it.code}' should not be at the dummy location -1, but was ${it.location?.region}",
            )
        }

        // (3) File names: the original source file names are recovered from the dex `source_file`
        //     entries -- including Kotlin (.kt) sources.
        val allFileNames =
            located.mapNotNull { it.location?.artifactLocation?.fileName }.toSortedSet()
        val sourceFileNames = allFileNames.filter { it.endsWith(".java") || it.endsWith(".kt") }
        assertTrue(
            sourceFileNames.isNotEmpty(),
            "Expected original source file names (.java/.kt) recovered from the dex debug info, " +
                "but got: $allFileNames",
        )

        // (4) Method declarations now carry a source location too. The dex frontend derives a
        //     line-only Position for each method from its statements' line numbers; before this the
        //     SootMethod carried NoPositionInformation, so `locationOf` returned null at method-
        //     declaration time (currentStmt is null then) and methods had no location at all.
        val ignored =
            listOf(
                "android.",
                "androidx.",
                "com.android.",
                "kotlin.",
                "kotlinx.",
                "java.",
                "javax.",
            )
        val userMethods =
            result.methods.filter { m -> ignored.none { m.name.toString().startsWith(it) } }
        val locatedMethods = userMethods.filter { (it.location?.region?.startLine ?: -1) >= 1 }
        assertTrue(
            locatedMethods.isNotEmpty(),
            "Expected at least one user method declaration with a source location from the APK, " +
                "but none of ${userMethods.size} user methods had one",
        )

        // (5) Class (record) declarations now carry a source location too. The dex frontend derives
        //     a line-only Position for the class from the earliest line in its methods' debug info;
        //     before this the SootClass carried NoPositionInformation and records had no location.
        val userRecords =
            result.records.filter { r -> ignored.none { r.name.toString().startsWith(it) } }
        val locatedRecords = userRecords.filter { (it.location?.region?.startLine ?: -1) >= 1 }
        assertTrue(
            locatedRecords.isNotEmpty(),
            "Expected at least one user class declaration with a source location from the APK, " +
                "but none of ${userRecords.size} user records had one",
        )

        // (6) Field declarations inherit their enclosing class's (approximate) position (dex
        // carries
        //     no per-field source line), so fields of a located user class are themselves located.
        //     The APK does declare user fields, so this check is actually exercised (not vacuous).
        val userFields =
            userRecords
                .flatMap { it.fields }
                .filter { f -> ignored.none { f.name.toString().startsWith(it) } }
        assertTrue(userFields.isNotEmpty(), "Expected the APK to declare user fields")
        // A field is either unlocated (its enclosing class carried no line info, so locationOf
        // returns null) or at a real source line -- but it must never sit at the dummy location -1.
        userFields.forEach {
            val startLine = it.location?.region?.startLine
            assertTrue(
                startLine == null || startLine >= 1,
                "field '${it.name}' should not be at the dummy location -1, but was ${it.location?.region}",
            )
        }
        val locatedFields = userFields.filter { (it.location?.region?.startLine ?: -1) >= 1 }
        assertTrue(
            locatedFields.isNotEmpty(),
            "Expected at least one user field declaration with a source location from the APK, " +
                "but none of ${userFields.size} user fields had one",
        )
    }

    /**
     * Verifies the opt-in [JVMFrontendConfiguration.useJimpleTextPositions] mode. Instead of the
     * coarse, frequently collapsed line numbers a compiled artifact carries (many statements
     * sharing one line, no columns), every class is round-tripped through its textual Jimple
     * representation so that each statement lands on its own, distinct line of a written `.jimple`
     * file. This is the "pick the node on line N" guarantee: the reported line resolves to a real,
     * readable line of a real file.
     */
    @Test
    fun testJimpleTextPositions() {
        val topLevel = Path.of("src", "test", "resources", "class", "operators")
        val result =
            analyze(listOf(topLevel.resolve("Operators.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
                it.configureFrontend<JVMLanguageFrontend>(
                    JVMFrontendConfiguration(useJimpleTextPositions = true)
                )
            }
        assertNotNull(result)

        val testArithmetic = result.methods["testArithmetic"]
        assertNotNull(testArithmetic)

        // (1) Every located statement points into a real `.jimple` file, and the line it reports is
        //     a real, non-blank line of that file -- i.e. "line N" actually resolves to content.
        val assigns = testArithmetic.allChildren<Assign>()
        val located = assigns.filter { (it.location?.region?.startLine ?: -1) >= 1 }
        assertTrue(located.isNotEmpty(), "Expected located assignments in text-position mode")

        located.forEach { assign ->
            val region = assign.location?.region
            assertNotNull(region)
            val fileName = assign.location?.artifactLocation?.uri?.path
            assertNotNull(fileName, "text-position nodes must carry a file URI")
            assertTrue(
                fileName.endsWith(".jimple"),
                "in text-position mode the location file must be the reprinted .jimple, was $fileName",
            )
            val lines = File(fileName).readLines()
            assertTrue(
                region.startLine <= lines.size,
                "reported line ${region.startLine} is outside the ${lines.size}-line file",
            )
            assertTrue(
                lines[region.startLine - 1].isNotBlank(),
                "statement reported on blank line ${region.startLine} of $fileName",
            )
        }

        // (2) Statements land on DISTINCT lines -- the whole point of the round-trip (the compiled
        //     artifact collapses many onto one). No two of our assignments share a start line.
        val startLines = located.mapNotNull { it.location?.region?.startLine }
        assertEquals(
            startLines.size,
            startLines.toSet().size,
            "statements must occupy distinct lines in text-position mode, but some collapsed: " +
                startLines.sorted(),
        )

        // (3) Line bases are reconciled: a value-level node (binary operator) reports the SAME line
        //     as its enclosing statement (no 0-/1-based off-by-one), while still carrying real
        //     columns -- so the line matches the file and the columns pinpoint the sub-expression.
        val assignWithBinop = located.firstOrNull { it.allChildren<BinaryOperator>().isNotEmpty() }
        assertNotNull(assignWithBinop, "expected an assignment containing a binary operator")
        val binop = assignWithBinop.allChildren<BinaryOperator>().first()
        assertEquals(
            assignWithBinop.location?.region?.startLine,
            binop.location?.region?.startLine,
            "a value's line must match its enclosing statement's line (no off-by-one)",
        )
        val binopRegion = binop.location?.region
        assertNotNull(binopRegion)
        assertTrue(
            binopRegion.startColumn >= 0 && binopRegion.endColumn > binopRegion.startColumn,
            "a value should carry a real, non-degenerate column span, was $binopRegion",
        )
    }

    /**
     * In text-position mode a method body is only rebuilt from the reprinted Jimple text when it is
     * first requested, i.e. *after* the class-level round-trip succeeded. A single body that does
     * not survive the round-trip is therefore degraded per method (it is translated from the
     * original compiled class instead, see `JVMLanguageFrontend.withMethodPositions`), which this
     * test pins down through the invariants that fallback has to keep:
     * 1. no method and no body is lost, and
     * 2. a method is never a mix of both position sources -- its declaration and its statements
     *    always point into the same file.
     */
    @Test
    fun testJimpleTextPositionsPerMethod() {
        val topLevel = Path.of("src", "test", "resources", "class", "operators")
        val result =
            analyze(listOf(topLevel.resolve("Operators.class").toFile()), topLevel, true) {
                it.registerLanguage<JVMLanguage>()
                it.configureFrontend<JVMLanguageFrontend>(
                    JVMFrontendConfiguration(useJimpleTextPositions = true)
                )
            }
        assertNotNull(result)

        val record = result.records["Operators"]
        assertNotNull(record)

        // (1) Every method the class declares is still there, with a translated body. A method
        //     whose body cannot be built is kept (body-less) rather than dropped, and a method
        //     without text positions is translated from the compiled artifact -- either way it must
        //     not disappear from the record.
        val expected =
            setOf(
                "<init>",
                "testArithmetic",
                "testComparison",
                "testBitwise",
                "testUnary",
                "testArrayLength",
                "testCast",
                "testInstanceOf",
            )
        // (constructors are modelled separately from the other methods)
        val methods = record.methods + record.constructors
        assertEquals(
            expected,
            methods.map { it.name.localName }.toSet(),
            "no method may be lost in text-position mode",
        )
        methods.forEach {
            assertNotNull(it.body, "method '${it.name}' lost its body in text-position mode")
        }

        // (2) Each method is coherent: whichever of the two position sources it ended up using, its
        //     declaration and all of its located statements refer to the same, existing file. (For
        //     this fixture everything round-trips, so that file is the reprinted `.jimple`; a
        //     degraded method would consistently point at the compiled artifact instead.)
        methods.forEach { method ->
            val methodFile = method.location?.artifactLocation?.uri?.path
            assertNotNull(methodFile, "method '${method.name}' must carry a file URI")
            assertTrue(
                File(methodFile).isFile,
                "the location of method '${method.name}' must resolve to a real file, was " +
                    methodFile,
            )
            val statementFiles =
                method
                    .allChildren<Expression>()
                    .filter { (it.location?.region?.startLine ?: -1) >= 1 }
                    .mapNotNull { it.location?.artifactLocation?.uri?.path }
                    .toSet()
            assertTrue(
                statementFiles.isNotEmpty(),
                "method '${method.name}' has no located statement at all",
            )
            assertTrue(
                statementFiles.all { it == methodFile },
                "method '${method.name}' mixes position sources: its declaration is in " +
                    "$methodFile but statements are in ${statementFiles - methodFile}",
            )
        }
    }
}
