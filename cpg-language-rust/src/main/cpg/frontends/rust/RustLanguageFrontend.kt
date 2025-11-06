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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.SupportsParallelParsing
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.AutoType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.CommentMatcher
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.net.URI
import java.nio.file.Path
import jep.python.PyObject
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString
import kotlin.math.min

/**
 * The [LanguageFrontend] for Rust. It uses the TreeSitter project to generate a RUST AST.
 *
 * It requires the Python interpreter (and the JEP library) to be installed on the system. The
 * frontend registers two additional passes.
 *
 * ## Adding dynamic variable declarations
 *
 * The [PythonAddDeclarationsPass] adds dynamic declarations to the CPG. Python does not have the
 * concept of a "declaration", but rather values are assigned to variables and internally variable
 * are represented by a dictionary. This pass adds a declaration for each variable that is assigned
 * a value (on the first assignment).
 */

@SupportsParallelParsing(true)
class RustLanguageFrontend(ctx: TranslationContext, language: Language<RustLanguageFrontend>) :
    LanguageFrontend<..., ...>(ctx, language) {
    val lineSeparator = "\n"
    private val tokenTypeIndex = 0


    internal val declarationHandler = DeclarationHandler(this)
    internal var statementHandler = StatementHandler(this)
    internal var expressionHandler = ExpressionHandler(this)

    private lateinit var fileContent: String
    private lateinit var uri: URI
    private var lastLineNumber: Int = -1
    private var lastColumnLength: Int = -1

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        fileContent = file.readText(Charsets.UTF_8)
        uri = file.toURI()

        // Extract the file length for later usage
        val fileAsLines = fileContent.lines()
        lastLineNumber = fileAsLines.size
        lastColumnLength = fileAsLines.lastOrNull()?.length ?: -1

        val tud = ... // Todo parsing
        val tud =
            newTranslationUnitDeclaration(path.toString(), rawNode = ...).apply {
                this.location =
                    PhysicalLocation(
                        uri = uri,
                        region =
                            Region(
                                startLine = 1,
                                startColumn = 1,
                                endLine = lastLineNumber,
                                endColumn = lastColumnLength,
                            ),
                    )
            }

            return tud
        }
    }




    override fun typeOf(type: ...NodeType): Type {
        return when (type) {

            is Name -> {
                this.typeOf(type.id)
            }

            is ... ->

            else -> {
                // The AST supplied us with some kind of type information, but we could not parse
                // it, so we need to return the unknown type.
                unknownType()
            }
        }
    }

    /** Resolves a [Type] based on its string identifier. */
    fun typeOf(typeId: String): Type {
        // Check if the typeId contains a namespace delimiter for qualified types
        val name =
            if (language.namespaceDelimiter in typeId) {
                parseName(typeId)
            } else {
                // Unqualified name, resolved by the type resolver
                typeId
            }

        return objectType(name)
    }


    override fun codeOf(astNode: ...NodeType): String? {
        return ...
    }



    fun operatorToString(op: ...) =
        when (op) {
            is ... -> "+"
            is ... -> "-"
            is ... -> "*"
            is ... -> "*"
            is ... -> "/"
            is ... -> "%"
            is ... -> "**"
            is ... -> "<<"
            is ... -> ">>"
            is ... -> "|"
            is ... -> "^"
            is ... -> "&"
            is ... -> "//"
        }

    fun operatorUnaryToString(op: ...) =
        when (op) {
            is ... -> "~"
            is ... -> "not"
            is ... -> "+"
            is ... -> "-"
        }
}

