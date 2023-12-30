/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.array
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.newTranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import sootup.core.inputlocation.AnalysisInputLocation
import sootup.core.types.ArrayType
import sootup.java.core.JavaSootClass
import sootup.jimple.parser.JimpleAnalysisInputLocation
import sootup.jimple.parser.JimpleProject

typealias SootType = sootup.core.types.Type

class JVMLanguageFrontend(
    language: Language<out LanguageFrontend<Any, SootType>>,
    ctx: TranslationContext
) : LanguageFrontend<Any, SootType>(language, ctx) {

    val declarationHandler = DeclarationHandler(this)
    val statementHandler = StatementHandler(this)
    val expressionHandler = ExpressionHandler(this)

    override fun parse(file: File): TranslationUnitDeclaration {
        val inputLocation: AnalysisInputLocation<JavaSootClass> =
            JimpleAnalysisInputLocation(file.toPath().parent)
        val project = JimpleProject(inputLocation)
        val view = project.createView()

        val tu = newTranslationUnitDeclaration(file.name)
        scopeManager.resetToGlobal(tu)

        for (sootClass in view.classes) {
            val decl = declarationHandler.handle(sootClass)
            scopeManager.addDeclaration(decl)
        }

        return tu
    }

    override fun setComment(node: Node, astNode: Any) {}

    override fun locationOf(astNode: Any): PhysicalLocation? {
        // We do not really have a location anyway. maybe in jimple?
        return null
    }

    override fun codeOf(astNode: Any): String? {
        // We do not really have a source anyway. maybe in jimple?
        return ""
    }

    override fun typeOf(type: SootType): Type {
        return when (type) {
            is ArrayType -> {
                typeOf(type.baseType).array()
            }
            else -> {
                // TODO(oxisto): primitive types
                val out = objectType(type.toString())

                out
            }
        }
    }
}
