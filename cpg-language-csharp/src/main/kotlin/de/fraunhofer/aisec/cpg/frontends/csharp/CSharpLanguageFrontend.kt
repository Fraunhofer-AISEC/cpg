/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.csharp

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.newTranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File

class CSharpLanguageFrontend(ctx: TranslationContext, language: Language<CSharpLanguageFrontend>) :
    LanguageFrontend<String, String>(ctx, language) {

    override fun parse(file: File): TranslationUnitDeclaration {
        val source = file.readText()
        val ptr = Csharp.INSTANCE.parseCsharp(source)
        val rootKind = ptr.getString(0, "UTF-8")
        if (rootKind == "CompilationUnit") {
            newTranslationUnitDeclaration(file.name)
        }
        log.info("C# root node kind: $rootKind")

        val tu = newTranslationUnitDeclaration(file.name)
        return tu
    }

    override fun typeOf(type: String): Type = unknownType()

    override fun codeOf(astNode: String): String = astNode

    override fun locationOf(astNode: String): PhysicalLocation? = null

    override fun setComment(node: Node, astNode: String) {}
}
