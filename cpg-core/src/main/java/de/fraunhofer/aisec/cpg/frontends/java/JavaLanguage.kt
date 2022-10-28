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
package de.fraunhofer.aisec.cpg.frontends.java

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager

/** The Java language TODO: Shouldn't it have HasTemplates as well? */
open class JavaLanguage :
    Language<JavaLanguageFrontend>, HasComplexCallResolution, HasClasses, HasSuperclasses {
    override val fileExtensions: List<String>
        get() = listOf("java")
    override val namespaceDelimiter: String
        get() = "."
    override val frontend: Class<JavaLanguageFrontend>
        get() = JavaLanguageFrontend::class.java
    override val superclassKeyword: String
        get() = "super"

    override fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager
    ): JavaLanguageFrontend {
        return JavaLanguageFrontend(config, scopeManager)
    }

    // TODO: Remove if not needed.
    override fun doBetterCallResolution() {
        println("i know it better")
    }
}
