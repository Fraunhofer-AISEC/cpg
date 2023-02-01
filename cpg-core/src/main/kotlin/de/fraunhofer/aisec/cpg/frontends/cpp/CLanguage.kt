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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.*
import kotlin.reflect.KClass

/** The C language. */
open class CLanguage :
    Language<CXXLanguageFrontend>(),
    HasStructs,
    HasFunctionPointers,
    HasQualifier,
    HasElaboratedTypeSpecifier,
    HasShortCircuitOperators {
    override val fileExtensions = listOf("c", "h")
    override val namespaceDelimiter = "::"
    override val frontend: KClass<out CXXLanguageFrontend> = CXXLanguageFrontend::class
    override val qualifiers = listOf("const", "volatile", "restrict", "atomic")
    override val elaboratedTypeSpecifier = listOf("struct", "union", "enum")
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")

    override fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager
    ): CXXLanguageFrontend {
        return CXXLanguageFrontend(this, config, scopeManager)
    }
}
