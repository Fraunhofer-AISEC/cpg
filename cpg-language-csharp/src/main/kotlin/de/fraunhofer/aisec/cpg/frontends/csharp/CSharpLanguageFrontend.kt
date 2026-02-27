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
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.newTranslationUnit
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File

class CSharpLanguageFrontend(ctx: TranslationContext, language: Language<CSharpLanguageFrontend>) :
    LanguageFrontend<Csharp.Ast.Node, Csharp.Ast.Node>(ctx, language) {

    override fun parse(file: File): TranslationUnit {
        val source = file.readText()
        val root = Csharp.CSharpSyntaxTree.parseText(source)
        val tu = newTranslationUnit(file.name, rawNode = root)
        return tu
    }

    override fun typeOf(type: Csharp.Ast.Node): Type = unknownType()

    override fun codeOf(astNode: Csharp.Ast.Node): String? = null

    override fun locationOf(astNode: Csharp.Ast.Node): PhysicalLocation? = null

    override fun setComment(node: Node, astNode: Csharp.Ast.Node) {}
}
