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
package de.fraunhofer.aisec.cpg.frontends.typescript

import de.fraunhofer.aisec.cpg.ExperimentalTypeScript
import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("experimental")
@ExperimentalTypeScript
class TypescriptLanguageFrontendTest {

    @Test
    fun testFunction() {
        val topLevel = Path.of("src", "test", "resources", "typescript")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("function.ts").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    TypeScriptLanguageFrontend::class.java,
                    TypeScriptLanguageFrontend.TYPESCRIPT_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val functions = tu.declarations.filterIsInstance<FunctionDeclaration>()
        assertNotNull(functions)

        assertEquals(2, functions.size)

        val someFunction = functions.first()
        assertEquals("someFunction", someFunction.name)
        assertEquals(TypeParser.createFrom("Number", false), someFunction.type)

        val someOtherFunction = functions.last()
        assertEquals("someOtherFunction", someOtherFunction.name)
        assertEquals(TypeParser.createFrom("Number", false), someOtherFunction.type)

        val parameters = someOtherFunction.parameters
        assertNotNull(parameters)

        assertEquals(1, parameters.size)

        val parameter = parameters.first()
        assertEquals("s", parameter.name)
        assertEquals(TypeParser.createFrom("String", false), parameter.type)
    }

    @Test
    fun testComplexCall() {
        val topLevel = Path.of("src", "test", "resources", "typescript")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("fetch.ts").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    TypeScriptLanguageFrontend::class.java,
                    TypeScriptLanguageFrontend.TYPESCRIPT_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val function =
            tu.getDeclarationsByName("handleSubmit", FunctionDeclaration::class.java)
                .iterator()
                .next()
        assertNotNull(function)

        val preventDefault = function.getBodyStatementAs(0, MemberCallExpression::class.java)
        assertNotNull(preventDefault)

        assertEquals("preventDefault", preventDefault.name)
        assertEquals("event", preventDefault.base.name)

        val apiUrl =
            function.getBodyStatementAs(1, DeclarationStatement::class.java)?.singleDeclaration as?
                VariableDeclaration
        assertNotNull(apiUrl)

        assertNotNull("apiUrl", apiUrl.name)

        val literalInitializer = apiUrl.initializer as? Literal<*>
        assertNotNull(literalInitializer)

        assertEquals("/api/v1/groups", literalInitializer.value)

        val token =
            function.getBodyStatementAs(2, DeclarationStatement::class.java)?.singleDeclaration as?
                VariableDeclaration
        assertNotNull(token)

        assertNotNull("token", token.name)

        val callInitializer = token.initializer as? CallExpression
        assertNotNull(callInitializer)

        val stringArg = callInitializer.arguments.first() as? Literal<*>
        assertNotNull(stringArg)

        assertEquals("access_token", stringArg.value)

        val chainedCall = function.getBodyStatementAs(3, MemberCallExpression::class.java)
        assertNotNull(chainedCall)

        val fetch = chainedCall.base as? CallExpression
        assertNotNull(fetch)

        val refArg = fetch.arguments.first() as? DeclaredReferenceExpression
        assertNotNull(refArg)

        assertEquals("apiUrl", refArg.name)
        assertSame(apiUrl, refArg.refersTo)

        var objectArg = fetch.arguments.last() as? InitializerListExpression
        assertNotNull(objectArg)

        assertEquals(3, objectArg.initializers.size)

        var keyValue = objectArg.initializers.first() as? KeyValueExpression
        assertNotNull(keyValue)

        assertEquals("method", (keyValue.key as? DeclaredReferenceExpression)?.name)
        assertEquals("POST", (keyValue.value as? Literal<*>)?.value)

        keyValue = objectArg.initializers.last() as? KeyValueExpression
        assertNotNull(keyValue)

        // nested object creation
        objectArg = keyValue.value as? InitializerListExpression
        assertNotNull(objectArg)

        assertEquals(2, objectArg.initializers.size)

        keyValue = objectArg.initializers.first() as? KeyValueExpression
        assertNotNull(keyValue)

        assertEquals("Authorization", (keyValue.key as? Literal<*>)?.value)
        assertEquals("Bearer \${token}", (keyValue.value as? Literal<*>)?.value)

        assertEquals("then", chainedCall.name)

        val funcArg = chainedCall.arguments.firstOrNull() as? DeclaredReferenceExpression
        assertNotNull(funcArg)

        val arrowFunction = funcArg.refersTo as? FunctionDeclaration
        assertNotNull(arrowFunction)
        assertNotNull(arrowFunction.body)

        val param = arrowFunction.parameters.firstOrNull()
        assertNotNull(param)
        assertNotNull("res", param.name)
    }

    @Test
    fun testReact() {
        val topLevel = Path.of("src", "test", "resources", "typescript")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("component.tsx").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                    TypeScriptLanguageFrontend::class.java,
                    TypeScriptLanguageFrontend.TYPESCRIPT_EXTENSIONS
                )
            }

        assertNotNull(tu)

        val user = tu.getDeclarationsByName("User", RecordDeclaration::class.java).iterator().next()
        assertNotNull(user)
        assertEquals("interface", user.kind)
        assertEquals("User", user.name)

        assertEquals(4, user.fields.size)

        val lastName = user.fields.lastOrNull()
        assertNotNull(lastName)
        assertEquals("lastName", lastName.name)
        assertEquals(TypeParser.createFrom("string", false), lastName.type)
    }
}
