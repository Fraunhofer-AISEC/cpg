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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.LambdaExpression
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import java.io.File
import kotlin.test.*

class CPPLambdaTest {

    @Test
    fun testLambdaSimple() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .registerLanguage<CPPLanguage>()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda1"]
        assertNotNull(function)

        val lambdaVar = function.variables["this_is_a_lambda"]
        assertNotNull(lambdaVar)
        assertIs<FunctionPointerType>(lambdaVar.type)

        val lambda = lambdaVar.initializer as? LambdaExpression
        assertNotNull(lambda)
        assertTrue(lambda in lambdaVar.nextEOG)

        val printFunctionCall = function.calls["print_function"]
        assertNotNull(printFunctionCall)
        assertTrue(printFunctionCall in lambdaVar.prevEOG)

        val lambdaCall = function.calls["this_is_a_lambda"]
        assertEquals(1, lambdaCall?.invokes?.size)
        assertEquals(lambda.function, lambdaCall?.invokes?.firstOrNull())
    }

    @Test
    fun testLambdaArgument() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .registerLanguage<CPPLanguage>()
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

        assertTrue(lambda in lambdaVar.nextEOG)
        val printFunctionCall = function.calls["print_function"]
        assertNotNull(printFunctionCall)
        assertTrue(printFunctionCall in lambdaVar.prevEOG)
    }

    @Test
    fun testLambdaSignature() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .registerLanguage<CPPLanguage>()
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
    fun testLambdaCaptureByValue() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .registerLanguage<CPPLanguage>()
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
    fun testLambdaCaptureByReference() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .registerLanguage<CPPLanguage>()
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
    fun testLambdaMutable() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .registerLanguage<CPPLanguage>()
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
    fun testLambdaCaptureAllByValue() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .registerLanguage<CPPLanguage>()
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
    fun testLambdaCaptureAllByReference() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .registerLanguage<CPPLanguage>()
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
    fun testLambdaForeach() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .registerLanguage<CPPLanguage>()
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
    fun testLambdaInitializedCaptures() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .registerLanguage<CPPLanguage>()
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

    @Test
    fun testLambdaFunctionNull() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/cxx/lambdas.cpp"))
                .defaultPasses()
                .registerLanguage<CPPLanguage>()
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)
        val function = result.functions["lambda11"]
        assertNotNull(function)

        val lambdaVar = function.variables["this_is_a_lambda"]
        assertNotNull(lambdaVar)
        assertNotNull(lambdaVar.initializer as? LambdaExpression)
        assertNotNull((lambdaVar.initializer as? LambdaExpression)?.function)
    }
}
