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
package de.fraunhofer.aisec.cpg.frontends.typescript

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import kotlin.reflect.KClass

/** The JavaScript language. */
open class JavaScriptLanguage : Language<TypeScriptLanguageFrontend>(), HasShortCircuitOperators {
    override val fileExtensions = listOf("js", "jsx")
    override val namespaceDelimiter = "."
    override val frontend: KClass<out TypeScriptLanguageFrontend> =
        TypeScriptLanguageFrontend::class
    override val conjunctiveOperators = listOf("&&", "&&=", "??", "??=")
    override val disjunctiveOperators = listOf("||", "||=")
    override val stringTypes = setOf("String")

    override fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager
    ): TypeScriptLanguageFrontend {
        return TypeScriptLanguageFrontend(this, config, scopeManager)
    }
}
