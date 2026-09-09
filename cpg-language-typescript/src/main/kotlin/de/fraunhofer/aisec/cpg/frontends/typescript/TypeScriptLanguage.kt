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

import de.fraunhofer.aisec.cpg.graph.Visibility
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.scopes.Scope

/**
 * The TypeScript language.
 *
 * In addition to the JavaScript concepts inherited from [JavaScriptLanguage] (`static` members and
 * `#name` hard-private members), TypeScript has genuine compile-time member access control via the
 * `public`/`protected`/`private` access specifiers, which map onto the corresponding [Visibility].
 */
class TypeScriptLanguage : JavaScriptLanguage() {
    override val fileExtensions = listOf("ts", "tsx")

    /**
     * Projects the TypeScript modifiers onto [declaration]. In addition to the `static` and `#name`
     * hard-private handling inherited from [JavaScriptLanguage.applyModifiers], TypeScript has the
     * compile-time access specifiers `public`/`protected`/`private` (only occurring on record
     * members), which map onto the corresponding [Visibility] and take precedence over the
     * JavaScript default.
     */
    override fun applyModifiers(declaration: Declaration, scope: Scope?) {
        super.applyModifiers(declaration, scope)
        when {
            PUBLIC in declaration.modifiers -> declaration.visibility = Visibility.PUBLIC
            PROTECTED in declaration.modifiers -> declaration.visibility = Visibility.PROTECTED
            PRIVATE in declaration.modifiers -> declaration.visibility = Visibility.PRIVATE
        }
    }
}
