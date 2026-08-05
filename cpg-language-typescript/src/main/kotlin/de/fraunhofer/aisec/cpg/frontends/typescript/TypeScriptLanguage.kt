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

import de.fraunhofer.aisec.cpg.frontends.DeclarationContext
import de.fraunhofer.aisec.cpg.frontends.KeywordSemantics
import de.fraunhofer.aisec.cpg.graph.Visibility

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
     * Interprets a TypeScript declaration keyword into its canonical [KeywordSemantics]. In
     * addition to the keywords handled by [JavaScriptLanguage.interpretKeyword], TypeScript has the
     * access specifiers `public`/`protected`/`private` (only occurring on record members), which
     * map onto the corresponding [Visibility]. Every other keyword is delegated to the JavaScript
     * interpretation.
     */
    override fun interpretKeyword(keyword: String, context: DeclarationContext): KeywordSemantics {
        return when (keyword) {
            PUBLIC -> KeywordSemantics(visibility = Visibility.PUBLIC)
            PROTECTED -> KeywordSemantics(visibility = Visibility.PROTECTED)
            PRIVATE -> KeywordSemantics(visibility = Visibility.PRIVATE)
            else -> super.interpretKeyword(keyword, context)
        }
    }
}
