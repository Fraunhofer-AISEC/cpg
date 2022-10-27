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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.HasComplexCallResolution
import de.fraunhofer.aisec.cpg.frontends.HasDefaultArguments
import de.fraunhofer.aisec.cpg.frontends.HasTemplates
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager

/**
 * The C++ language.
 */
class CPPLanguage :
    Language<CXXLanguageFrontend>, HasDefaultArguments, HasTemplates, HasComplexCallResolution {
    override val fileExtensions: List<String>
        get() = listOf(".cpp", ".cc")
    override val namespaceDelimiter: String
        get() = "::"

    override fun newFrontend(config: TranslationConfiguration): CXXLanguageFrontend {
        return CXXLanguageFrontend(config, ScopeManager())
    }

    override fun doBetterCallResolution() {
        println("i know it better")
    }
}
