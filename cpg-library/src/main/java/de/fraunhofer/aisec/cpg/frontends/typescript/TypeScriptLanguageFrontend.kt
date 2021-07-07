/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.fraunhofer.aisec.cpg.ExperimentalTypeScript
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.*
import org.apache.commons.io.input.TeeInputStream
import org.checkerframework.checker.nullness.qual.NonNull

@ExperimentalTypeScript
class TypeScriptLanguageFrontend(
    config: @NonNull TranslationConfiguration,
    scopeManager: ScopeManager?
) : LanguageFrontend(config, scopeManager, ".") {

    val declarationHandler = DeclarationHandler(this)
    val statementHandler = StatementHandler(this)
    val expressionHandler = ExpressionHandler(this)
    val typeHandler = TypeHandler(this)

    companion object {
        @kotlin.jvm.JvmField var TYPESCRIPT_EXTENSIONS: List<String> = listOf(".ts", ".tsx")
    }

    var cachedSource: String? = null

    override fun parse(file: File): TranslationUnitDeclaration {
        cachedSource = file.readText()

        val p =
            Runtime.getRuntime()
                .exec(arrayOf("node", "src/main/nodejs/parser.js", file.absolutePath))

        val teeOutput = TeeInputStream(p.inputStream, System.out)

        val mapper = jacksonObjectMapper()
        val node = mapper.readValue(teeOutput, TypeScriptNode::class.java)

        return this.declarationHandler.handle(node) as TranslationUnitDeclaration
    }

    override fun <T : Any?> getCodeFromRawNode(astNode: T): String? {
        return if (astNode is TypeScriptNode) {
            // TODO: position seems to be off by +1
            return cachedSource?.substring(astNode.location.pos, astNode.location.end)
        } else {
            null
        }
    }

    override fun <T : Any?> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        return if (astNode is TypeScriptNode) {
            // TODO: LSP region
            var location = PhysicalLocation(File(astNode.location.file).toURI(), Region())

            return location
        } else {
            null
        }
    }

    override fun <S : Any?, T : Any?> setComment(s: S, ctx: T) {}

    internal fun getIdentifierName(node: TypeScriptNode) =
        this.getCodeFromRawNode(node.firstChild("Identifier"))?.trim()
}

class Location(var file: String, var pos: Int, var end: Int)

class TypeScriptNode(
    var type: String,
    var children: List<TypeScriptNode>?,
    var location: Location
) {
    /** Returns the first child node, that represent a type, if it exists. */
    val typeChildNode: TypeScriptNode?
        get() {
            return this.children?.firstOrNull {
                it.type == "TypeReference" ||
                    it.type == "AnyKeyword" ||
                    it.type == "StringKeyword" ||
                    it.type == "NumberKeyword" ||
                    it.type == "ArrayType" ||
                    it.type == "TypeLiteral"
            }
        }

    fun firstChild(type: String): TypeScriptNode? {
        return this.children?.firstOrNull { it.type == type }
    }
}
