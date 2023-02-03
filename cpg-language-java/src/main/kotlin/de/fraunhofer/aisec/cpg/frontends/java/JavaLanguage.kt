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

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.passes.JavaCallResolverHelper
import kotlin.reflect.KClass

/** The Java language. */
open class JavaLanguage :
    Language<JavaLanguageFrontend>(),
    // HasComplexCallResolution,
    HasClasses,
    HasSuperClasses,
    // HasTemplates,
    HasQualifier,
    HasUnknownType,
    HasShortCircuitOperators {
    override val fileExtensions = listOf("java")
    override val namespaceDelimiter = "."
    override val frontend: KClass<out JavaLanguageFrontend> = JavaLanguageFrontend::class
    override val superClassKeyword = "super"

    override val qualifiers = listOf("final", "volatile")
    override val unknownTypeString = listOf("var")
    override val conjunctiveOperators = listOf("&&")
    override val disjunctiveOperators = listOf("||")

    override fun newFrontend(
        config: TranslationConfiguration,
        scopeManager: ScopeManager
    ): JavaLanguageFrontend {
        return JavaLanguageFrontend(this, config, scopeManager)
    }

    override fun handleSuperCall(
        callee: MemberExpression,
        curClass: RecordDeclaration,
        scopeManager: ScopeManager,
        recordMap: Map<Name, RecordDeclaration>
    ) = JavaCallResolverHelper.handleSuperCall(callee, curClass, scopeManager, recordMap)
}
