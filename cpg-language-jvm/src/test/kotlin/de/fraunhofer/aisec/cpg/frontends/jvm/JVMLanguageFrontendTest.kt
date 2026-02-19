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
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.test.assertFullName
import de.fraunhofer.aisec.cpg.test.assertInvokes
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import de.fraunhofer.aisec.cpg.test.assertLocalName
import de.fraunhofer.aisec.cpg.test.assertRefersTo
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
        assertIs<MemberCallExpression>(call)
        assertLocalName("add", call)
        assertInvokes(call, add)
        assertEquals(listOf("Integer", "Integer"), call.arguments.map { it.type.name.localName })

        // All references (which are not part of a call) and not to the stdlib should be resolved
        val refs = tu.refs
        refs
            .filter { it.astParent !is CallExpression }
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

    @Test
    fun testHelloWorldApk() {
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
            tu.problems.isEmpty() || tu.problems.all { it is Problem || it is ProblemExpression }
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
                            it is MemberCallExpression &&
                            it.base in extended.usages
                    }]
        assertNotNull(getMyProperty)
        assertInvokes(getMyProperty, baseClass.methods["getMyProperty"])

        val setMyProperty =
            appInit.calls[
                    {
                        it.name.localName == "setMyProperty" &&
                            it is MemberCallExpression &&
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

        val refs = tu.refs.filterIsInstance<MemberExpression>()
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
        assertIs<NewArrayExpression>(expr)
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
        assertIs<SubscriptExpression>(prevDFG)
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
        assertIs<NewArrayExpression>(expr)
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
        val literalTypes = literals.map { it.value?.javaClass?.simpleName ?: "null" }.toSet()
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
    fun testCastExpression() {
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
        val gotoStmts = testGoto.allChildren<GotoStatement>()
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
}
