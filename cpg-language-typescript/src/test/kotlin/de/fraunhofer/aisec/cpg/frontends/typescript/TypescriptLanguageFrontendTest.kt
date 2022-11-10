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
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import org.junit.jupiter.api.Tag

@Tag("experimentalTypeScript")
@ExperimentalTypeScript
class TypeScriptLanguageFrontendTest {

    @Test
    fun testFunction() {
        val topLevel = Path.of("src", "test", "resources", "typescript")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("function.ts").toFile()),
                topLevel,
                true
            ) { it.registerLanguage<TypeScriptLanguage>() }

        assertNotNull(tu)

        val functions = tu.declarations.filterIsInstance<FunctionDeclaration>()
        assertNotNull(functions)

        assertEquals(2, functions.size)

        val someFunction = functions.first()
        assertLocalName("someFunction", someFunction)
        assertEquals(TypeParser.createFrom("Number", TypeScriptLanguage()), someFunction.type)

        val someOtherFunction = functions.last()
        assertLocalName("someOtherFunction", someOtherFunction)
        assertEquals(TypeParser.createFrom("Number", TypeScriptLanguage()), someOtherFunction.type)

        val parameters = someOtherFunction.parameters
        assertNotNull(parameters)

        assertEquals(1, parameters.size)

        val parameter = parameters.first()
        assertLocalName("s", parameter)
        assertEquals(TypeParser.createFrom("String", TypeScriptLanguage()), parameter.type)
    }

    @Test
    fun testJSFunction() {
        val topLevel = Path.of("src", "test", "resources", "typescript")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("function.js").toFile()),
                topLevel,
                true
            ) { it.registerLanguage<JavaScriptLanguage>() }

        assertNotNull(tu)

        // actually, our frontend returns 3 functions (1 inferred), because our function inference
        // cannot handle non-typed languages very well
        val functions =
            tu.declarations.filterIsInstance<FunctionDeclaration>().filter { !it.isInferred }
        assertNotNull(functions)

        assertEquals(2, functions.size)

        val someFunction = functions.first()
        assertLocalName("someFunction", someFunction)

        val someOtherFunction = functions.last()
        assertLocalName("someOtherFunction", someOtherFunction)

        val parameters = someOtherFunction.parameters
        assertNotNull(parameters)

        assertEquals(1, parameters.size)

        val parameter = parameters.first()
        assertLocalName("s", parameter)
    }

    @Test
    fun testJSX() {
        val topLevel = Path.of("src", "test", "resources", "typescript")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("simple.jsx").toFile()),
                topLevel,
                true
            ) { it.registerLanguage<JavaScriptLanguage>() }

        assertNotNull(tu)

        val doJsx =
            tu.getDeclarationsByName("doJsx", FunctionDeclaration::class.java).iterator().next()
        assertNotNull(doJsx)

        val returnStmt = doJsx.getBodyStatementAs(0, ReturnStatement::class.java)
        assertNotNull(returnStmt)

        // check the return statement for the TSX statements
        val jsx = returnStmt.returnValue as? ExpressionList
        assertNotNull(jsx)

        val tag = jsx.expressions.firstOrNull()
        assertNotNull(tag)
        assertLocalName("<div>", tag)
    }

    @Test
    fun testComplexCall() {
        val topLevel = Path.of("src", "test", "resources", "typescript")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("fetch.ts").toFile()),
                topLevel,
                true
            ) { it.registerLanguage<TypeScriptLanguage>() }

        assertNotNull(tu)

        val function =
            tu.getDeclarationsByName("handleSubmit", FunctionDeclaration::class.java)
                .iterator()
                .next()
        assertNotNull(function)

        val preventDefault = function.getBodyStatementAs(0, MemberCallExpression::class.java)
        assertNotNull(preventDefault)

        assertLocalName("preventDefault", preventDefault)
        assertLocalName("event", preventDefault.base)

        val apiUrl =
            function.getBodyStatementAs(1, DeclarationStatement::class.java)?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(apiUrl)

        assertLocalName("apiUrl", apiUrl)

        val literalInitializer = apiUrl.initializer as? Literal<*>
        assertNotNull(literalInitializer)

        assertEquals("/api/v1/groups", literalInitializer.value)

        val token =
            function.getBodyStatementAs(2, DeclarationStatement::class.java)?.singleDeclaration
                as? VariableDeclaration
        assertNotNull(token)

        assertLocalName("token", token)

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

        assertLocalName("apiUrl", refArg)
        assertSame(apiUrl, refArg.refersTo)

        var objectArg = fetch.arguments.last() as? InitializerListExpression
        assertNotNull(objectArg)

        assertEquals(3, objectArg.initializers.size)

        var keyValue = objectArg.initializers.first() as? KeyValueExpression
        assertNotNull(keyValue)

        assertLocalName("method", keyValue.key as? DeclaredReferenceExpression)
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

        assertLocalName("then", chainedCall)

        val funcArg = chainedCall.arguments.firstOrNull() as? LambdaExpression
        assertNotNull(funcArg)

        val arrowFunction = funcArg.function
        assertNotNull(arrowFunction)
        assertNotNull(arrowFunction.body)

        val param = arrowFunction.parameters.firstOrNull()
        assertNotNull(param)
        assertLocalName("res", param)
    }

    @Test
    fun testReact() {
        val topLevel = Path.of("src", "test", "resources", "typescript")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("component.tsx").toFile()),
                topLevel,
                true
            ) { it.registerLanguage<TypeScriptLanguage>() }

        assertNotNull(tu)

        val user = tu.getDeclarationsByName("User", RecordDeclaration::class.java).iterator().next()
        assertNotNull(user)
        assertEquals("interface", user.kind)
        assertLocalName("User", user)

        assertEquals(4, user.fields.size)

        val lastName = user.fields.lastOrNull()
        assertNotNull(lastName)
        assertLocalName("lastName", lastName)
        assertEquals(TypeParser.createFrom("string", TypeScriptLanguage()), lastName.type)

        val usersState =
            tu.getDeclarationsByName("UsersState", RecordDeclaration::class.java).iterator().next()
        assertNotNull(user)
        assertEquals("interface", usersState.kind)
        assertLocalName("UsersState", usersState)

        assertEquals(1, usersState.fields.size)

        val users = usersState.fields.firstOrNull()
        assertNotNull(users)
        assertLocalName("users", users)
        assertEquals(TypeParser.createFrom("User[]", TypeScriptLanguage()), users.type)

        val usersComponent =
            tu.getDeclarationsByName("Users", RecordDeclaration::class.java).iterator().next()
        assertNotNull(usersComponent)
        assertLocalName("Users", usersComponent)
        assertEquals(1, usersComponent.constructors.size)
        assertEquals(/*2*/ 3 /* because of a dummy node */, usersComponent.methods.size)
        assertEquals(/*0*/ 2 /* because of dummy nodes */, usersComponent.fields.size)

        val render = usersComponent.methods["render"]
        assertNotNull(render)

        val returnStmt = render.getBodyStatementAs(1, ReturnStatement::class.java)
        assertNotNull(returnStmt)

        // check the return statement for the TSX statements
        val jsx = returnStmt.returnValue as? ExpressionList
        assertNotNull(jsx)

        val tag = jsx.expressions.firstOrNull()
        assertNotNull(tag)
        assertLocalName("<div>", tag)
    }

    @Test
    fun testReactFunctionComponent() {
        val topLevel = Path.of("src", "test", "resources", "typescript")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("function-component.tsx").toFile()),
                topLevel,
                true
            ) { it.registerLanguage<TypeScriptLanguage>() }

        assertNotNull(tu)

        val loginForm =
            tu.getDeclarationsByName("LoginForm", VariableDeclaration::class.java).iterator().next()
        assertNotNull(loginForm)

        val lambdaFunction = (loginForm.initializer as? LambdaExpression)?.function
        assertNotNull(lambdaFunction)

        val declStatement = lambdaFunction.getBodyStatementAs(3, DeclarationStatement::class.java)
        assertNotNull(declStatement)

        val validateForm = declStatement.singleDeclaration as? FunctionDeclaration
        assertNotNull(validateForm)
        assertLocalName("validateForm", validateForm)
    }

    @Test
    fun testDecorators() {
        val topLevel = Path.of("src", "test", "resources", "typescript")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("decorator.ts").toFile()),
                topLevel,
                true
            ) { it.registerLanguage<TypeScriptLanguage>() }

        assertNotNull(tu)

        val myClass =
            tu.getDeclarationsByName("MyClass", RecordDeclaration::class.java).iterator().next()
        assertNotNull(myClass)
        assertLocalName("awesome", myClass.annotations.firstOrNull())

        val method = myClass.methods.firstOrNull()
        assertNotNull(method)
        assertLocalName("dontcall", method.annotations.firstOrNull())

        val field = myClass.fields["something"]
        assertNotNull(field)

        val annotation = field.annotations.firstOrNull()
        assertNotNull(annotation)
        assertLocalName("sensitive", annotation)

        val member = annotation.members.firstOrNull()
        assertNotNull(member)
        assertEquals("very", (member.value as? Literal<*>)?.value)
    }

    @Test
    fun testLambda() {
        val topLevel = Path.of("src", "test", "resources", "typescript")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("lambda.js").toFile()),
                topLevel,
                true
            ) { it.registerLanguage<JavaScriptLanguage>() }

        assertNotNull(tu)

        val onPost =
            tu.statements.firstOrNull {
                it is MemberCallExpression && it.fullName.localName == "onPost"
            } as? MemberCallExpression
        assertNotNull(onPost)

        val lambda = onPost.arguments.drop(1).firstOrNull() as? LambdaExpression
        assertNotNull(lambda)

        val func = lambda.function
        assertNotNull(func)

        assertEquals(2, func.parameters.size)
        assertLocalName("req", func.parameters.firstOrNull())
        assertLocalName("res", func.parameters[1])
    }

    @Test
    fun testComments() {
        val topLevel = Path.of("src", "test", "resources", "typescript")
        val componentTU =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("component.tsx").toFile()),
                topLevel,
                true
            ) { it.registerLanguage<TypeScriptLanguage>() }
        val functionTu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("function.ts").toFile()),
                topLevel,
                true
            ) { it.registerLanguage<TypeScriptLanguage>() }

        assertNotNull(componentTU)
        assertNotNull(functionTu)

        val users = componentTU.byNameOrNull<RecordDeclaration>("Users")
        assertNotNull(users)
        assertEquals("Comment on a record", users.comment)

        val i = users.constructors.first()
        assertNotNull(i)
        assertEquals("Comment on constructor", i.comment)

        val j = users.methods["componentDidMount"]
        assertNotNull(j)
        assertEquals("Multiline comment inside of a file", j.comment)

        var function = functionTu.byNameOrNull<FunctionDeclaration>("someFunction")
        assertNotNull(function)
        assertEquals("Block comment on a function", function.comment)

        val variableDeclaration =
            SubgraphWalker.flattenAST(function).filterIsInstance<DeclarationStatement>().first()
        assertNotNull(variableDeclaration)
        assertEquals("Comment on a variable", variableDeclaration.comment)

        function = functionTu.byNameOrNull("someOtherFunction")
        assertNotNull(function)
        assertEquals("Comment on a Function", function.comment)
    }
}
