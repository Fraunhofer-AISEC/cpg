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
package de.fraunhofer.aisec.cpg.frontends.java

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.variables
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JavaLambdaTest {
    @Test
    fun testLambda() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/Lambda.java"))
                .defaultPasses()
                .registerLanguage<JavaLanguage>()
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
        assertEquals(outerVar, (mapBody.lhs as? Reference)?.refersTo)

        val testfunctionArg =
            result.calls { it.name.localName == "testFunction" }[0].arguments.first()
        assertTrue(testfunctionArg is Reference)
        assertTrue(
            (testfunctionArg.refersTo as? Variable)?.initializer is LambdaExpression
        )

        val testfunctionBody = mapArg.function?.body as? BinaryOperator
        assertNotNull(testfunctionBody)
        assertEquals(outerVar, (testfunctionBody.lhs as? Reference)?.refersTo)

        val lambdaVar = result.variables["lambdaVar"]
        assertNotNull(lambdaVar)
        val constructExpr =
            (lambdaVar.initializer as? NewExpression)?.initializer as? ConstructExpression
        assertNotNull(constructExpr)
        val anonymousRecord = constructExpr.instantiates as? Record
        assertNotNull(anonymousRecord)
        assertTrue(anonymousRecord.isImplicit)
        assertEquals(1, anonymousRecord.superClasses.size)
        // TODO: We only get "BiFunction" here.
        // assertEquals("java.util.function.BiFunction",
        // anonymousRecord.superClasses.first().name.toString() )

        val applyMethod = anonymousRecord.methods["apply"]
        assertNotNull(applyMethod)
        val returnStatement =
            (applyMethod.body as? Block)?.statements?.firstOrNull() as? ReturnStatement
        assertNotNull(returnStatement)
        assertEquals(
            outerVar,
            (((returnStatement.returnValue as? BinaryOperator)?.lhs as? BinaryOperator)?.lhs
                    as? Reference)
                ?.refersTo,
        )
    }
}
