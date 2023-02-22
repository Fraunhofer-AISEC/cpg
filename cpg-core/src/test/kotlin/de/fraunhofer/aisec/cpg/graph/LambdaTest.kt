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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import java.io.File
import kotlin.test.*

class LambdaTest {
    @Test
    fun testJavaLambda() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/Lambda.java"))
                .defaultPasses()
                .defaultLanguages()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)

        val foreachArg = result.calls["forEach"]?.arguments?.first()
        assertTrue(foreachArg is LambdaExpression)
        assertNotNull(foreachArg.function)

        val replaceAllArg = result.calls["replaceAll"]?.arguments?.first()
        assertTrue(replaceAllArg is LambdaExpression)
        assertNotNull(replaceAllArg.function)

        val mapArg = result.calls["map"]?.arguments?.first()
        assertTrue(mapArg is LambdaExpression)
        assertNotNull(mapArg.function)

        val mapBody = mapArg.function?.body as? BinaryOperator
        assertNotNull(mapBody)
        val outerVar = result.variables["outerVar"]
        assertNotNull(outerVar)
        assertEquals(outerVar, (mapBody.lhs as? DeclaredReferenceExpression)?.refersTo)

        val testfunctionArg =
            result.calls { it.name.localName == "testFunction" }[0].arguments.first()
        assertTrue(testfunctionArg is DeclaredReferenceExpression)
        assertTrue(
            (testfunctionArg.refersTo as? VariableDeclaration)?.initializer is LambdaExpression
        )

        val testfunctionBody = mapArg.function?.body as? BinaryOperator
        assertNotNull(testfunctionBody)
        assertEquals(outerVar, (testfunctionBody.lhs as? DeclaredReferenceExpression)?.refersTo)

        val lambdaVar = result.variables["lambdaVar"]
        assertNotNull(lambdaVar)
        val constructExpression =
            (lambdaVar.initializer as? NewExpression)?.initializer as? ConstructExpression
        assertNotNull(constructExpression)
        val anonymousRecord = constructExpression.instantiates as? RecordDeclaration
        assertNotNull(anonymousRecord)
        assertTrue(anonymousRecord.isImplicit)
        assertEquals(1, anonymousRecord.superClasses.size)
        // TODO: We only get "BiFunction" here.
        // assertEquals("java.util.function.BiFunction",
        // anonymousRecord.superClasses.first().name.toString() )

        val applyMethod = anonymousRecord.methods["apply"]
        assertNotNull(applyMethod)
        val returnStmt =
            (applyMethod.body as? CompoundStatement)?.statements?.firstOrNull() as? ReturnStatement
        assertNotNull(returnStmt)
        assertEquals(
            outerVar,
            (((returnStmt.returnValue as? BinaryOperator)?.lhs as? BinaryOperator)?.lhs
                    as? DeclaredReferenceExpression)
                ?.refersTo
        )
    }

    @Test
    fun testCPPLambda() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/lambda.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)

        val verifyArg = result.calls["SSL_CTX_set_verify"]?.arguments?.last()
        assertTrue(verifyArg is LambdaExpression)
        assertNotNull(verifyArg.function)
        assertTrue(
            (verifyArg.function?.body as? CompoundStatement)?.statements?.firstOrNull()
                is ReturnStatement
        )
    }

    @Test
    fun testCPPLambda1() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda1"]
        assertNotNull(function)

        val lambdaVar = function.variables["this_is_a_lambda"]
        assertNotNull(lambdaVar)
        val lambda = lambdaVar.initializer as? LambdaExpression
        assertNotNull(lambda)

        assertTrue(lambda in lambdaVar.prevEOG)
        val printFunctionCall = function.calls["print_function"]
        assertNotNull(printFunctionCall)
        assertTrue(printFunctionCall in lambda.prevEOG)

        val lambdaCall = function.calls["this_is_a_lambda"]
        assertEquals(1, lambdaCall?.invokes?.size)
        assertEquals(lambda.function, lambdaCall?.invokes?.firstOrNull())
    }

    @Test
    fun testCPPLambda2() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda2"]
        assertNotNull(function)

        val lambdaVar = function.variables["this_is_a_lambda"]
        assertNotNull(lambdaVar)
        val lambda = lambdaVar.initializer as? LambdaExpression
        assertNotNull(lambda)

        // Check the type of the parameter
        assertEquals(1, lambda.function.parameters.size)
        assertEquals("uint64_t", lambda.function?.parameters?.first()?.type?.name?.localName)
        // Check that the ref is resolved to the param.
        val numberRef = lambda.function?.body?.refs?.get("number")
        assertNotNull(numberRef)
        assertEquals(lambda.function?.parameters?.firstOrNull(), numberRef.refersTo)

        assertTrue(lambda in lambdaVar.prevEOG)
        val printFunctionCall = function.calls["print_function"]
        assertNotNull(printFunctionCall)
        assertTrue(printFunctionCall in lambda.prevEOG)
    }

    @Test
    fun testLambdaEOG() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda2"]
        assertNotNull(function)

        val lambdaVar = function.variables["this_is_a_lambda"]
        assertNotNull(lambdaVar)
        val lambda = lambdaVar.initializer as? LambdaExpression
        assertNotNull(lambda)

        // The "outer" EOG is assembled correctly.
        assertTrue(lambda in lambdaVar.prevEOG)
        val printFunctionCall = function.calls["print_function"]
        assertNotNull(printFunctionCall)
        assertTrue(printFunctionCall in lambda.prevEOG)

        // The "inner" EOG is assembled correctly.
        val body = (lambda.function?.body as? CompoundStatement)
        assertNotNull(body)
        assertEquals(1, lambda.function?.nextEOG?.size)
        assertEquals(
            "std::cout",
            (lambda.function?.nextEOG?.get(0) as? DeclaredReferenceExpression)?.name.toString()
        )

        val cout = lambda.function?.nextEOG?.get(0) as? DeclaredReferenceExpression
        assertEquals(1, cout?.nextEOG?.size)
        assertEquals("Hello ", (cout?.nextEOG?.get(0) as? Literal<*>)?.value.toString())

        val hello = cout?.nextEOG?.get(0) as? Literal<*>
        assertEquals(1, hello?.nextEOG?.size)
        assertEquals("<<", (hello?.nextEOG?.get(0) as? BinaryOperator)?.operatorCode)

        val binOpLeft = (hello?.nextEOG?.get(0) as? BinaryOperator)
        assertEquals(1, binOpLeft?.nextEOG?.size)
        assertEquals(
            "number",
            (binOpLeft?.nextEOG?.get(0) as? DeclaredReferenceExpression)?.name.toString()
        )

        val number = binOpLeft?.nextEOG?.get(0) as? DeclaredReferenceExpression
        assertEquals(1, number?.nextEOG?.size)
        assertEquals("<<", (number?.nextEOG?.get(0) as? BinaryOperator)?.operatorCode)

        val binOpCenter = (number?.nextEOG?.get(0) as? BinaryOperator)
        assertEquals(1, binOpCenter?.nextEOG?.size)
        assertEquals(
            "std::endl",
            (binOpCenter?.nextEOG?.get(0) as? DeclaredReferenceExpression)?.name.toString()
        )

        val endl = (binOpCenter?.nextEOG?.get(0) as? DeclaredReferenceExpression)
        assertEquals(1, endl?.nextEOG?.size)
        assertEquals("<<", (endl?.nextEOG?.get(0) as? BinaryOperator)?.operatorCode)

        val binOpRight = (endl?.nextEOG?.get(0) as? BinaryOperator)
        assertEquals(1, binOpRight?.nextEOG?.size)
        assertTrue(binOpRight?.nextEOG?.firstOrNull() is CompoundStatement)

        assertEquals(0, (binOpRight?.nextEOG?.firstOrNull() as? CompoundStatement)?.nextEOG?.size)
    }

    @Test
    fun testCPPLambda3() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda3"]
        assertNotNull(function)

        val lambdaVar = function.variables["this_is_a_lambda"]
        assertNotNull(lambdaVar)
        val lambda = lambdaVar.initializer as? LambdaExpression
        assertNotNull(lambda)

        assertEquals(1, lambda.function.parameters.size)
        // Check the param type
        assertEquals("bool", lambda.function?.parameters?.first()?.type?.name?.localName)
        // Check the return type
        assertEquals("float", lambda.function?.returnTypes?.firstOrNull()?.name?.localName)
    }

    @Test
    fun testCPPLambda4() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda4"]
        assertNotNull(function)

        val aNumberDecl = function.variables["a_number"]
        assertNotNull(aNumberDecl)

        val lambdaVar = function.variables["this_is_a_lambda"]
        assertNotNull(lambdaVar)
        val lambda = lambdaVar.initializer as? LambdaExpression
        assertNotNull(lambda)

        // Check that the ref is resolved to the decl outside the lambda
        val numberRef = lambda.function?.body?.refs?.filter { it.name.localName == "a_number" }
        assertNotNull(numberRef)
        assertEquals(2, numberRef.size)
        for (ref in numberRef) {
            assertEquals(aNumberDecl, ref.refersTo)
        }

        // By default, the lambda is marked as not mutable
        assertFalse(lambda.areVariablesMutable)
        assertTrue(lambda.mutableVariables.isEmpty())
    }

    @Test
    fun testCPPLambda5() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda5"]
        assertNotNull(function)

        val aNumberDecl = function.variables["a_number"]
        assertNotNull(aNumberDecl)

        val lambdaVar = function.variables["this_is_a_lambda"]
        assertNotNull(lambdaVar)
        val lambda = lambdaVar.initializer as? LambdaExpression
        assertNotNull(lambda)

        // Check that the ref is resolved to the decl outside the lambda
        val numberRef = lambda.function?.body?.refs?.filter { it.name.localName == "a_number" }
        assertNotNull(numberRef)
        assertEquals(2, numberRef.size)
        for (ref in numberRef) {
            assertEquals(aNumberDecl, ref.refersTo)
        }

        // By default, the lambda is marked as not mutable
        assertFalse(lambda.areVariablesMutable)
        assertEquals(1, lambda.mutableVariables.size)
        assertEquals(aNumberDecl, lambda.mutableVariables.first())
    }

    @Test
    fun testCPPLambda6() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda6"]
        assertNotNull(function)

        val aNumberDecl = function.variables["a_number"]
        assertNotNull(aNumberDecl)

        val lambdaVar = function.variables["this_is_a_lambda"]
        assertNotNull(lambdaVar)
        val lambda = lambdaVar.initializer as? LambdaExpression
        assertNotNull(lambda)

        // Check that the ref is resolved to the decl outside the lambda
        val numberRef = lambda.function?.body?.refs?.filter { it.name.localName == "a_number" }
        assertNotNull(numberRef)
        assertEquals(2, numberRef.size)
        for (ref in numberRef) {
            assertEquals(aNumberDecl, ref.refersTo)
        }

        // By default, the lambda is marked as not mutable
        assertTrue(lambda.areVariablesMutable)
        assertTrue(lambda.mutableVariables.isEmpty())
    }

    @Test
    fun testCPPLambda7() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda7"]
        assertNotNull(function)

        val aNumberDecl = function.variables["a_number"]
        assertNotNull(aNumberDecl)

        val lambdaVar = function.variables["this_is_a_lambda"]
        assertNotNull(lambdaVar)
        val lambda = lambdaVar.initializer as? LambdaExpression
        assertNotNull(lambda)

        // Check that the ref is resolved to the decl outside the lambda
        val numberRef = lambda.function?.body?.refs?.filter { it.name.localName == "a_number" }
        assertNotNull(numberRef)
        assertEquals(1, numberRef.size)
        assertEquals(aNumberDecl, numberRef.first().refersTo)

        // By default, the lambda is marked as not mutable
        assertFalse(lambda.areVariablesMutable)
        assertTrue(lambda.mutableVariables.isEmpty())
    }

    @Test
    fun testCPPLambda8() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda8"]
        assertNotNull(function)

        val aNumberDecl = function.variables["a_number"]
        assertNotNull(aNumberDecl)

        val lambdaVar = function.variables["this_is_a_lambda"]
        assertNotNull(lambdaVar)
        val lambda = lambdaVar.initializer as? LambdaExpression
        assertNotNull(lambda)

        // Check that the ref is resolved to the decl outside the lambda
        val numberRef = lambda.function?.body?.refs?.filter { it.name.localName == "a_number" }
        assertNotNull(numberRef)
        assertEquals(2, numberRef.size)
        for (ref in numberRef) {
            assertEquals(aNumberDecl, ref.refersTo)
        }

        // By default, the lambda is marked as not mutable
        assertTrue(lambda.areVariablesMutable)
        assertTrue(lambda.mutableVariables.isEmpty())
    }

    @Test
    fun testCPPLambda9() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda9"]
        assertNotNull(function)

        val lambda = function.calls["for_each"]?.arguments?.get(2) as? LambdaExpression
        assertNotNull(lambda)

        // Check the type of the parameter
        assertEquals(1, lambda.function.parameters.size)
        assertEquals("std::string", lambda.function?.parameters?.first()?.type?.name.toString())
        // Check that the ref is resolved to the param.
        val numberRef = lambda.function?.body?.refs?.get("it")
        assertNotNull(numberRef)
        assertEquals(lambda.function?.parameters?.firstOrNull(), numberRef.refersTo)
    }

    @Test
    fun testCPPLambda10() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .defaultLanguages()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda10"]
        assertNotNull(function)

        val lambdaVar = function.variables["this_is_a_lambda"]
        assertNotNull(lambdaVar)
        val lambda = (lambdaVar.initializer as? CallExpression)?.callee as? LambdaExpression
        assertNotNull(lambda)
    }
}
