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
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDecl
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDecl
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStmt
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStmt
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
                .defaultLanguages()
                .registerLanguage(JavaLanguage())
                .build()
        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()

        assertNotNull(result)

        val foreachArg = result.calls["forEach"]?.arguments?.first()
        assertTrue(foreachArg is LambdaExpr)
        assertNotNull(foreachArg.function)

        val replaceAllArg = result.calls["replaceAll"]?.arguments?.first()
        assertTrue(replaceAllArg is LambdaExpr)
        assertNotNull(replaceAllArg.function)

        val mapArg = result.calls["map"]?.arguments?.first()
        assertTrue(mapArg is LambdaExpr)
        assertNotNull(mapArg.function)

        val mapBody = mapArg.function?.body as? BinaryOp
        assertNotNull(mapBody)
        val outerVar = result.variables["outerVar"]
        assertNotNull(outerVar)
        assertEquals(outerVar, (mapBody.lhs as? Reference)?.refersTo)

        val testfunctionArg =
            result.calls { it.name.localName == "testFunction" }[0].arguments.first()
        assertTrue(testfunctionArg is Reference)
        assertTrue((testfunctionArg.refersTo as? VariableDecl)?.initializer is LambdaExpr)

        val testfunctionBody = mapArg.function?.body as? BinaryOp
        assertNotNull(testfunctionBody)
        assertEquals(outerVar, (testfunctionBody.lhs as? Reference)?.refersTo)

        val lambdaVar = result.variables["lambdaVar"]
        assertNotNull(lambdaVar)
        val constructExpr = (lambdaVar.initializer as? NewExpr)?.initializer as? ConstructExpr
        assertNotNull(constructExpr)
        val anonymousRecord = constructExpr.instantiates as? RecordDecl
        assertNotNull(anonymousRecord)
        assertTrue(anonymousRecord.isImplicit)
        assertEquals(1, anonymousRecord.superClasses.size)
        // TODO: We only get "BiFunction" here.
        // assertEquals("java.util.function.BiFunction",
        // anonymousRecord.superClasses.first().name.toString() )

        val applyMethod = anonymousRecord.methods["apply"]
        assertNotNull(applyMethod)
        val returnStmt =
            (applyMethod.body as? CompoundStmt)?.statements?.firstOrNull() as? ReturnStmt
        assertNotNull(returnStmt)
        assertEquals(
            outerVar,
            (((returnStmt.returnValue as? BinaryOp)?.lhs as? BinaryOp)?.lhs as? Reference)?.refersTo
        )
    }
}
