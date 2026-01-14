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
import java.io.File
import java.net.URI
import kotlin.collections.plusAssign
import uniffi.cpgrust.RsAst
import uniffi.cpgrust.RsItem
import uniffi.cpgrust.RsType
import uniffi.cpgrust.parseRustCode

/** The [LanguageFrontend] for Rust. It uses the TreeSitter project to generate a RUST AST. */
@SupportsParallelParsing(true)
class RustLanguageFrontend(ctx: TranslationContext, language: Language<RustLanguageFrontend>) :
    LanguageFrontend<RsAst, RsType>(ctx, language) {
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

        val rsRustFile = parseRustCode(file.absolutePath)
        println(rsRustFile?.astNode?.text)
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

        for (rsItem in rsRustFile?.items ?: listOf()) {
            when (rsItem) {
                is RsAst.RustItem -> {
                    val decl = declarationHandler.handle(rsItem)
                    scopeManager.addDeclaration(decl)
                    tud.addDeclaration(decl)
                }
                else -> log.warn("Not handling ${rsItem.javaClass.simpleName}.")
            }
        }

        rsRustFile?.let {
            it.items.forEach { it is RsItem }
            it.items.forEach { item -> println("Item: $item type: ${item}") }
        }

        return tud
    }

    override fun typeOf(type: RsType): Type {
        return when (type) {
            is RsType.ArrayType -> unknownType()
            is RsType.TupleType -> unknownType()
            is RsType.FnPtrType -> unknownType()
            is RsType.InferType -> unknownType()
            is RsType.MacroType -> unknownType()
            is RsType.DynTraitType -> unknownType()
            is RsType.ForType -> unknownType()
            is RsType.ImplTraitType -> unknownType()
            is RsType.NeverType -> unknownType()
            is RsType.ParenType -> unknownType()
            is RsType.PathType -> unknownType()
            is RsType.PtrType -> unknownType()
            is RsType.RefType -> unknownType()
            is RsType.SliceType -> unknownType()
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

    override fun codeOf(astNode: RsAst): String? {
        return astNode.astNode().text
    }

    override fun locationOf(astNode: RsAst): PhysicalLocation? {
        val metaAstNode = astNode.astNode()
        val contentBefore = fileContent.substring(0, metaAstNode.startOffset.toInt())
        val upTo = contentBefore.split(lineSeparator)
        val contentBeforeAndIn = fileContent.substring(0, metaAstNode.endOffset.toInt())
        val upToIncluding = contentBeforeAndIn.split(lineSeparator)
        return PhysicalLocation(
            uri,
            Region(
                upTo.size,
                upTo.last().length + 1,
                upToIncluding.size,
                upToIncluding.last().length + 1,
            ),
        )
    }

    override fun setComment(node: Node, astNode: RsAst) {
        val metaAstNode = astNode.astNode()

        node.comment = metaAstNode.comments
    }

    fun operatorToString(op: RsAst) =
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

    fun operatorUnaryToString(op: RsAst) =
        when (op) {
            else -> ""
        /*          is ... -> "~"
        is ... -> "not"
        is ... -> "+"
        is ... -> "-"*/
        }
}
