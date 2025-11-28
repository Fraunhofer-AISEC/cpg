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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.SupportsParallelParsing
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import uniffi.cpgrust.SomeStruct
import uniffi.cpgrust.getSomeStruct
import java.io.File
import java.net.URI
import uniffi.cpgrust.printString

/** The [LanguageFrontend] for Rust. It uses the TreeSitter project to generate a RUST AST. */
@SupportsParallelParsing(true)
class RustLanguageFrontend(ctx: TranslationContext, language: Language<RustLanguageFrontend>) :
    LanguageFrontend<Rust.AST, Rust.Type>(ctx, language) {
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

        printString("Print this rust string")
        val someStruct: SomeStruct = getSomeStruct()

        someStruct.get
        // Todo parsing
        val tud =
            newTranslationUnitDeclaration(file.path, rawNode = null).apply {
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

    override fun typeOf(type: Rust.Type): Type {
        return unknownType()
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

    override fun codeOf(astNode: Rust.AST): String? {
        return astNode.toString() // Todo parse the code itself
    }

    override fun locationOf(astNode: Rust.AST): PhysicalLocation? {
        TODO("Not yet implemented")
    }

    override fun setComment(node: Node, astNode: Rust.AST) {
        TODO("Not yet implemented")
    }

    fun operatorToString(op: Rust.AST) =
        when (op) {
            /*is ... -> "+"
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
            is ... -> "//"*/
            else -> ""
        }

    fun operatorUnaryToString(op: Rust.AST) =
        when (op) {
            else -> ""
        /*          is ... -> "~"
        is ... -> "not"
        is ... -> "+"
        is ... -> "-"*/
        }
}
