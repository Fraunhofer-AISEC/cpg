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
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.LambdaExpression
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

        val testfunctionArg = result.calls["testFunction"]?.arguments?.first()
        assertTrue(testfunctionArg is DeclaredReferenceExpression)
        assertTrue(
            (testfunctionArg.refersTo as? VariableDeclaration)?.initializer is LambdaExpression
        )

        val testfunctionBody = mapArg.function?.body as? BinaryOperator
        assertNotNull(testfunctionBody)
        assertEquals(outerVar, (testfunctionBody.lhs as? DeclaredReferenceExpression)?.refersTo)
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
}
