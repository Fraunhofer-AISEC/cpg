/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.example

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TypeManager
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HasClasses
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ProblemDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.functionDeclaration
import de.fraunhofer.aisec.cpg.graph.globalScope
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.parameterDeclaration
import de.fraunhofer.aisec.cpg.graph.plusAssign
import de.fraunhofer.aisec.cpg.graph.translationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.withScope
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.File
import kotlin.reflect.KClass

/**
 * This is an example language, that demonstrate how to construct a [Language]. Our language has the
 * following features:
 * - It uses the `.example` file extension
 * - It has classes and namespaces
 * - Namespaces are separated by a `::` delimiter
 * - It features some built-in types, such as `int`
 */
class ExampleLanguage : Language<ExampleLanguageFrontend>(), HasClasses {
    override val fileExtensions: List<String> = listOf("example")
    override val namespaceDelimiter: String = "::"
    override val frontend: KClass<out ExampleLanguageFrontend> = ExampleLanguageFrontend::class
    override val builtInTypes: Map<String, Type> = mapOf("int" to IntegerType())
    override val compoundAssignmentOperators = setOf<String>()
}

/**
 * This is an example language frontend, that demonstrates how to translate languages into the CPG.
 * In this case, we want to translate our [ExampleLanguage]. It takes nodes that derive from
 * [RawNode] -- these would come from a real parser in a real language -- and translates them into a
 * CPG [Node].
 */
class ExampleLanguageFrontend(
    language: ExampleLanguage = ExampleLanguage(),
    ctx: TranslationContext =
        TranslationContext(
            TranslationConfiguration.builder().build(),
            ScopeManager(),
            TypeManager()
        ),
    var rawFileNode: RawFileNode
) : LanguageFrontend<RawNode, RawTypeNode>(language, ctx) {

    /**
     * We want to store an instance of our [Handler] here, in this case one that takes care of a
     * [Declaration].
     */
    private var declarationHandler = ExampleDeclarationHandler(this)

    override fun parse(file: File): TranslationUnitDeclaration {
        // In a real language frontend, we would read the AST from the contents of the file, but
        // since we are not fully implementing a parser, we are using the rawFileNode from our
        // constructor instead.
        val parsedFile = rawFileNode

        return translationUnitDeclaration(parsedFile.name).globalScope {
            for (child in parsedFile.children) {
                this += declarationHandler.handle(child)
            }
        }
    }

    override fun typeOf(type: RawTypeNode): Type {
        return objectType(type.name)
    }

    override fun codeOf(astNode: RawNode): String? {
        return astNode.code
    }

    override fun locationOf(astNode: RawNode): PhysicalLocation? {
        return null
    }

    override fun setComment(node: Node, astNode: RawNode) {}
}

class ExampleDeclarationHandler(frontend: ExampleLanguageFrontend) :
    Handler<Declaration, RawDeclarationNode, ExampleLanguageFrontend>(
        ::ProblemDeclaration,
        frontend
    ) {
    override fun handle(node: RawDeclarationNode): Declaration {
        return when (node) {
            is RawFunctionNode -> handleFunction(node)
            is RawParameterNode -> handleParameter(node)
            else -> ProblemDeclaration()
        }
    }

    private fun handleFunction(node: RawFunctionNode): FunctionDeclaration {
        return functionDeclaration(node.name, rawNode = node).withScope {
            for (param in node.params) {
                val decl = handle(param)

                this += decl
            }
        }
    }

    private fun handleParameter(node: RawParameterNode): ParameterDeclaration {
        return parameterDeclaration(node.name, type = frontend.typeOf(node.type))
    }
}
