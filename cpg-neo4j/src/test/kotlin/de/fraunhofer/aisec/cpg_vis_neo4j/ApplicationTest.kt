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
package de.fraunhofer.aisec.cpg_vis_neo4j

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.net.URI
import java.nio.file.Paths
import java.util.function.Supplier
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import picocli.CommandLine

// @Tag("integration")
class ApplicationTest {
    @Test
    @Throws(InterruptedException::class)
    fun testPush() {
        val topLevel = Paths.get("src").resolve("test").resolve("resources").toAbsolutePath()
        val path = topLevel.resolve("client.cpp").toAbsolutePath()

        val cmd = CommandLine(Application::class.java)
        cmd.parseArgs(path.toString())
        val application = cmd.getCommand<Application>()

        val translationConfiguration = application.setupTranslationConfiguration()
        val translationResult =
            TranslationManager.builder().config(translationConfiguration).build().analyze().get()

        assertEquals(31, translationResult.functions.size)

        application.pushToNeo4j(translationResult)

        val sessionAndSessionFactoryPair = application.connect()

        val session = sessionAndSessionFactoryPair.first
        session.beginTransaction().use { transaction ->
            val functions = session.loadAll(FunctionDeclaration::class.java)
            assertNotNull(functions)

            assertEquals(31, functions.size)

            transaction.commit()
        }

        session.clear()
        sessionAndSessionFactoryPair.second.close()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testPush2() {

        val cmd = CommandLine(Application::class.java)
        val application = cmd.getCommand<Application>()

        val translationResult = getConditionalExpression()

        application.pushToNeo4j(translationResult)

        val sessionAndSessionFactoryPair = application.connect()

        val session = sessionAndSessionFactoryPair.first
        session.beginTransaction().use { transaction ->
            val functions = session.loadAll(FunctionDeclaration::class.java)

            transaction.commit()
        }

        session.clear()
        sessionAndSessionFactoryPair.second.close()
    }
}

fun getConditionalExpression(
    config: TranslationConfiguration =
        TranslationConfiguration.builder()
            .defaultPasses()
            .registerLanguage(TestLanguage("."))
            .build()
) =
    testFrontend(config).build {
        translationResult {
            translationUnit("conditional_expression.cpp") {
                // The main method
                function("main", t("int")) {
                    body {
                        declare { variable("a", t("int")) { literal(0, t("int")) } }
                        declare { variable("b", t("int")) { literal(1, t("int")) } }

                        ref("a") {
                            location =
                                PhysicalLocation(
                                    URI("conditional_expression.cpp"),
                                    Region(5, 3, 5, 4)
                                )
                        } assign
                            {
                                conditional(
                                    ref("a") {
                                        location =
                                            PhysicalLocation(
                                                URI("conditional_expression.cpp"),
                                                Region(5, 7, 5, 8)
                                            )
                                    } eq
                                        ref("b") {
                                            location =
                                                PhysicalLocation(
                                                    URI("conditional_expression.cpp"),
                                                    Region(5, 12, 5, 13)
                                                )
                                        },
                                    ref("b") {
                                        location =
                                            PhysicalLocation(
                                                URI("conditional_expression.cpp"),
                                                Region(5, 16, 5, 17)
                                            )
                                    } assignAsExpr { literal(2, t("int")) },
                                    ref("b") {
                                        location =
                                            PhysicalLocation(
                                                URI("conditional_expression.cpp"),
                                                Region(5, 23, 5, 24)
                                            )
                                    } assignAsExpr { literal(3, t("int")) }
                                )
                            }
                        ref("a") {
                            location =
                                PhysicalLocation(
                                    URI("conditional_expression.cpp"),
                                    Region(6, 3, 6, 4)
                                )
                        } assign
                            ref("b") {
                                location =
                                    PhysicalLocation(
                                        URI("conditional_expression.cpp"),
                                        Region(6, 7, 6, 8)
                                    )
                            }
                        returnStmt { isImplicit = true }
                    }
                }
            }
        }
    }

fun testFrontend(config: TranslationConfiguration): TestLanguageFrontend {
    val ctx = TranslationContext(config, ScopeManager(), TypeManager())
    val language = config.languages.filterIsInstance<TestLanguage>().first()
    return TestLanguageFrontend(language.namespaceDelimiter, language, ctx)
}

open class TestLanguage(namespaceDelimiter: String = "::") : Language<TestLanguageFrontend>() {
    override val fileExtensions: List<String> = listOf()
    final override val namespaceDelimiter: String
    override val frontend: KClass<out TestLanguageFrontend> = TestLanguageFrontend::class
    override val compoundAssignmentOperators =
        setOf("+=", "-=", "*=", "/=", "%=", "<<=", ">>=", "&=", "|=", "^=")

    override val builtInTypes: Map<String, Type> =
        mapOf(
            "boolean" to IntegerType("boolean", 1, this, NumericType.Modifier.SIGNED),
            "char" to IntegerType("char", 8, this, NumericType.Modifier.NOT_APPLICABLE),
            "byte" to IntegerType("byte", 8, this, NumericType.Modifier.SIGNED),
            "short" to IntegerType("short", 16, this, NumericType.Modifier.SIGNED),
            "int" to IntegerType("int", 32, this, NumericType.Modifier.SIGNED),
            "long" to IntegerType("long", 64, this, NumericType.Modifier.SIGNED),
            "float" to FloatingPointType("float", 32, this, NumericType.Modifier.SIGNED),
            "double" to FloatingPointType("double", 64, this, NumericType.Modifier.SIGNED),
            "string" to StringType("string", this),
        )

    init {
        this.namespaceDelimiter = namespaceDelimiter
    }

    override fun newFrontend(ctx: TranslationContext): TestLanguageFrontend {
        return TestLanguageFrontend(language = this, ctx = ctx)
    }
}

open class TestLanguageFrontend(
    namespaceDelimiter: String = "::",
    language: Language<TestLanguageFrontend> = TestLanguage(namespaceDelimiter),
    ctx: TranslationContext =
        TranslationContext(
            TranslationConfiguration.builder().build(),
            ScopeManager(),
            TypeManager()
        ),
) : LanguageFrontend<Any, Any>(language, ctx) {
    override fun parse(file: File): TranslationUnitDeclaration {
        TODO("Not yet implemented")
    }

    override fun typeOf(type: Any): Type {
        // reserved for future use
        return unknownType()
    }

    override fun codeOf(astNode: Any): String? {
        TODO("Not yet implemented")
    }

    override fun locationOf(astNode: Any): PhysicalLocation? {
        TODO("Not yet implemented")
    }

    override fun setComment(node: Node, astNode: Any) {
        TODO("Not yet implemented")
    }
}

class TestHandler(frontend: TestLanguageFrontend) :
    Handler<Node, Any, TestLanguageFrontend>(Supplier { ProblemExpression() }, frontend)
