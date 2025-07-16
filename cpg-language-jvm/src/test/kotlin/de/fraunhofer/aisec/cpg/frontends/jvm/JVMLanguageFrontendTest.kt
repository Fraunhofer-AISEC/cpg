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

        val helloWorld = tu.dRecords["HelloWorld"]
        assertNotNull(helloWorld)

        val constructor = helloWorld.constructors.firstOrNull()
        assertNotNull(constructor)

        // All references should be resolved (except Object.<init>, which should be a construct
        // expression anyway)
        val refs = constructor.dRefs.filter { it.name.toString() != "java.lang.Object.<init>" }
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

        val param0 = main.dRefs["@parameter0"]
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
        assertEquals(0, tu.dProblems.size)

        val pkg = tu.namespaces["mypackage"]
        assertNotNull(pkg)

        val adder = pkg.dRecords["Adder"]
        assertNotNull(adder)

        val add = adder.methods["add"]
        assertNotNull(add)

        val main = pkg.dMethods["Main.main"]
        assertNotNull(main)

        println(main.code)

        // r5 contains our adder
        val r5 = main.dVariables["r5"]
        assertNotNull(r5)
        assertFullName("mypackage.Adder", r5.type)

        // r3 should be the result of the add call
        val r3 = main.dVariables["r3"]
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
        val refs = tu.dRefs
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

        result.dMethods.forEach {
            println(it.name)
            println(it.code)
        }

        assertEquals(0, result.dProblems.size)
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
        assertEquals(0, tu.dProblems.size)
        tu.dMethods.forEach { println(it.code) }
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
        tu.dMethods.forEach { println(it.code) }
        assertEquals(0, tu.dProblems.size)

        val myInterface = tu.dRecords["mypackage.MyInterface"]
        assertNotNull(myInterface)
        assertEquals("interface", myInterface.kind)

        val baseClass = tu.dRecords["mypackage.BaseClass"]
        assertNotNull(baseClass)

        val extendedClass = tu.dRecords["mypackage.ExtendedClass"]
        assertNotNull(extendedClass)
        assertContains(extendedClass.implementedInterfaces, myInterface.toType())
        assertContains(extendedClass.superTypeDeclarations, baseClass)
        assertContains(extendedClass.superTypeDeclarations, myInterface)

        val anotherExtendedClass = tu.dRecords["mypackage.AnotherExtendedClass"]
        assertNotNull(anotherExtendedClass)
        assertContains(anotherExtendedClass.superTypeDeclarations, baseClass)

        assertEquals(
            baseClass.toType(),
            listOf(extendedClass.toType(), anotherExtendedClass.toType()).commonType,
        )

        val appInit = tu.dMethods["mypackage.Application.<init>"]
        assertNotNull(appInit)

        val appDoSomething = tu.dMethods["mypackage.Application.doSomething"]
        assertNotNull(appDoSomething)
        assertLocalName("MyInterface", appDoSomething.parameters.firstOrNull()?.type)

        // Call doSomething in Application.<init> with an object of ExtendedClass, which should
        // fulfill the MyInterface of the needed parameter
        val doSomethingCall1 = appInit.dCalls["doSomething"]
        assertNotNull(doSomethingCall1)
        assertLocalName("ExtendedClass", doSomethingCall1.arguments.firstOrNull()?.type)
        assertInvokes(doSomethingCall1, appDoSomething)

        val extended = appInit.dVariables["r4"]
        assertNotNull(extended)

        val getMyProperty =
            appInit.dCalls[
                    {
                        it.name.localName == "getMyProperty" &&
                            it is MemberCallExpression &&
                            it.base in extended.usages
                    }]
        assertNotNull(getMyProperty)
        assertInvokes(getMyProperty, baseClass.methods["getMyProperty"])

        val setMyProperty =
            appInit.dCalls[
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
        assertEquals(0, tu.dProblems.size)
        tu.dMethods.forEach { println(it.code) }

        val refs = tu.dRefs.filterIsInstance<MemberExpression>()
        refs.forEach {
            val refersTo = it.refersTo
            assertNotNull(refersTo, "${it.name} could not be resolved")
            assertFalse(
                refersTo.isInferred,
                "${it.name} should not be resolved to an inferred node",
            )
        }

        val setACall = tu.dCalls["setA"]
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

        val haveFun = tu.dMethods["haveFunWithLiterals"]
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
        tu.dMethods.forEach { println(it.code) }
        assertEquals(0, tu.dProblems.size)

        val create = tu.dMethods["create"]
        assertNotNull(create)

        val r3 = create.dVariables["r3"]
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

        var r1 = create.dVariables["r1"]
        assertNotNull(r1)
        assertEquals(arrayType.elementType, r1.type)

        val r2 = create.dVariables["r2"]
        assertNotNull(r2)
        assertEquals(arrayType.elementType, r2.type)

        val r2write = r2.usages.firstOrNull { it.access == AccessValues.WRITE }
        assertNotNull(r2write)

        val prevDFG = r2write.prevDFG.singleOrNull()
        assertIs<SubscriptExpression>(prevDFG)
        assertRefersTo(prevDFG.arrayExpression, r3)

        val createMulti = tu.dMethods["createMulti"]
        assertNotNull(createMulti)

        r1 = createMulti.dVariables["r1"]
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
        tu.dMethods.forEach { println(it.code) }
    }
}
