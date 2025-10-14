/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ast.declarations.ProblemDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import kotlin.reflect.KClass
import kotlin.test.assertNotNull

/**
 * This is a variant of the test language with `::` as a [namespaceDelimiter] to simulate languages
 * like C++.
 */
open class TestLanguageWithColon() : TestLanguage() {
    override val namespaceDelimiter: String
        get() = "::"
}

/**
 * This is a test language that can be used for unit test, where we need a language but do not have
 * a specific one.
 */
open class TestLanguage : Language<TestLanguageFrontend>(), HasImplicitReceiver {
    override val fileExtensions: List<String> = listOf()
    override val frontend: KClass<out TestLanguageFrontend> = TestLanguageFrontend::class
    override val compoundAssignmentOperators =
        setOf("+=", "-=", "*=", "/=", "%=", "<<=", ">>=", "&=", "|=", "^=")

    override val builtInTypes: Map<String, Type> =
        mapOf(
            "boolean" to BooleanType("boolean", 1, this, NumericType.Modifier.SIGNED),
            "char" to IntegerType("char", 8, this, NumericType.Modifier.NOT_APPLICABLE),
            "byte" to IntegerType("byte", 8, this, NumericType.Modifier.SIGNED),
            "short" to IntegerType("short", 16, this, NumericType.Modifier.SIGNED),
            "int" to IntegerType("int", 32, this, NumericType.Modifier.SIGNED),
            "long" to IntegerType("long", 64, this, NumericType.Modifier.SIGNED),
            "float" to FloatingPointType("float", 32, this, NumericType.Modifier.SIGNED),
            "double" to FloatingPointType("double", 64, this, NumericType.Modifier.SIGNED),
            "string" to StringType("string", this),
        )
    override val receiverName: String
        get() = "this"
}

class ClassTestLanguage() : TestLanguage(), HasClasses, HasDefaultArguments

class StructTestLanguage() : TestLanguageWithColon(), HasStructs, HasClasses, HasDefaultArguments

/**
 * Creates a new [TestLanguageFrontend] with the given configuration [builder].
 *
 * Note: This requires that the user registers at least one [TestLanguage], but it gives him the
 * chance to configure a specific subclass of it, e.g., [TestLanguageWithColon].
 */
fun testFrontend(builder: (TranslationConfiguration.Builder) -> Unit): TestLanguageFrontend {
    var config = TranslationConfiguration.builder().also(builder).build()
    return testFrontend(config)
}

/**
 * Creates a new [TestLanguageFrontend] with the given [config].
 *
 * Note: This requires that the user registers at least one [TestLanguage], but it gives him the
 * chance to configure a specific subclass of it, e.g., [TestLanguageWithColon].
 */
fun testFrontend(config: TranslationConfiguration): TestLanguageFrontend {
    val ctx = TranslationContext(config)
    val language = ctx.availableLanguage<TestLanguage>()
    assertNotNull(language)
    return TestLanguageFrontend(ctx, language)
}

open class TestLanguageFrontend(
    ctx: TranslationContext = TranslationContext(TranslationConfiguration.builder().build()),
    language: Language<TestLanguageFrontend> = TestLanguage(),
) : LanguageFrontend<Any, Any>(ctx, language) {
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
    Handler<Node, Any, TestLanguageFrontend>(::ProblemDeclaration, frontend)
