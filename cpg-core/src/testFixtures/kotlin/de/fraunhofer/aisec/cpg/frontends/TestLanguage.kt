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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import kotlin.reflect.KClass

/**
 * This is a test language that can be used for unit test, where we need a language but do not have
 * a specific one.
 */
class TestLanguage : Language<TestLanguageFrontend>() {
    override val fileExtensions: List<String> = listOf()
    override val namespaceDelimiter: String = "."
    override val frontend: KClass<out TestLanguageFrontend> = TestLanguageFrontend::class

    override fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager
    ): TestLanguageFrontend {
        return TestLanguageFrontend()
    }
}

class TestLanguageFrontend :
    LanguageFrontend(
        TestLanguage(),
        TranslationConfiguration.builder().build(),
        ScopeManager(),
    ) {
    override fun parse(file: File): TranslationUnitDeclaration {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> getCodeFromRawNode(astNode: T): String? {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        TODO("Not yet implemented")
    }

    override fun <S : Any?, T : Any?> setComment(s: S, ctx: T) {
        TODO("Not yet implemented")
    }
}

class TestHandler : Handler<Node, Any, TestLanguageFrontend>(null, TestLanguageFrontend()) {}
