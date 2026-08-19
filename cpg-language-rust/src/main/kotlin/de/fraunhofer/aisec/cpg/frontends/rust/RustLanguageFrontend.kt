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
import de.fraunhofer.aisec.cpg.graph.declarations.DeclarationSequence
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.types.AutoType
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.TupleType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.net.URI
import kotlin.math.min
import uniffi.rustast.RsAst
import uniffi.rustast.RsPath
import uniffi.rustast.RsType
import uniffi.rustast.parseRustCode

/** The [LanguageFrontend] for Rust. It uses the TreeSitter project to generate a RUST AST. */
@SupportsParallelParsing(true)
class RustLanguageFrontend(ctx: TranslationContext, language: Language<RustLanguageFrontend>) :
    LanguageFrontend<RsAst, RsType>(ctx, language) {
    val lineSeparator = "\n"
    private val tokenTypeIndex = 0

    internal val declarationHandler = DeclarationHandler(this)
    internal var statementHandler = StatementHandler(this)
    internal var expressionHandler = ExpressionHandler(this)
    internal var patternHandler = PatternHandler(this)

    private lateinit var fileContent: String
    private lateinit var uri: URI
    private var lastLineNumber: Int = -1
    private var lastColumnLength: Int = -1

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnit {
        fileContent = file.readText(Charsets.UTF_8)
        uri = file.toURI()

        // Extract the file length for later usage
        val fileAsLines = fileContent.lines()
        lastLineNumber = fileAsLines.size
        lastColumnLength = fileAsLines.lastOrNull()?.length ?: -1

        val rsRustFile = parseRustCode(file.absolutePath)
        val tud =
            newTranslationUnit(file.path, rawNode = null).apply {
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
                    ((decl as? DeclarationSequence)?.declarations ?: listOf(decl)).forEach {
                        declItem ->
                        scopeManager.addDeclaration(declItem)
                        tud.addDeclaration(declItem)
                    }
                }
                else -> log.warn("Not handling ${rsItem.javaClass.simpleName}.")
            }
        }

        return tud
    }

    override fun typeOf(type: RsType): Type {
        return when (type) {
            is RsType.ArrayType -> typeOf(type.v1.ty.first()).array()
            is RsType.TupleType -> TupleType(type.v1.fields.map { t -> typeOf(t) })
            is RsType.ParenType -> typeOf(type.v1.ty.first())
            is RsType.PathType -> typeFromPath(type)
            is RsType.PtrType -> typeOf(type.v1.ty.first()).pointer()
            is RsType.RefType -> typeOf(type.v1.ty.first()).ref()
            is RsType.SliceType -> typeOf(type.v1.ty.first()).array()
            is RsType.FnPtrType -> {
                val params =
                    type.v1.paramList.firstOrNull()?.params?.map {
                        it.ty?.let { t -> typeOf(t) } ?: unknownType()
                    } ?: listOf()
                val ret = type.v1.retType.firstOrNull()?.let { typeOf(it) } ?: unknownType()
                FunctionType(
                    parameters = params,
                    returnTypes = listOf(ret),
                    language = this.language,
                )
            }

            is RsType.InferType -> AutoType(language)
            // Cannot handle the type of a macro before expansion
            is RsType.MacroType -> newProblemType(RsAst.RustType(type))
            // Currently we only handle one of the contained types, in the future we need to solve
            // this by introducing
            // Type lists or type bounds
            is RsType.DynTraitType ->
                type.v1.typeBoundList.firstOrNull()?.ty?.let { typeOf(it) } ?: unknownType()
            is RsType.ImplTraitType ->
                type.v1.typeBoundList.firstOrNull()?.ty?.let { typeOf(it) } ?: unknownType()
            // Just unwrapping the type here, if we need to handle lifetime bounds in the type we
            // have to change this in the future
            is RsType.ForType -> type.v1.ty.firstOrNull()?.let { typeOf(it) } ?: unknownType()
            is RsType.NeverType -> language.builtInTypes["!"] ?: unknownType()
        }
    }

    fun typeFromPath(typePath: RsType.PathType): Type {
        typePath.v1.path?.segment?.nameRef?.let {
            language.builtInTypes[it.text]?.let {
                return it
            }
            return objectType(parseName(it.text))
        }
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

    override fun codeOf(astNode: RsAst): String? {
        return astNode.astNode().text
    }

    override fun locationOf(astNode: RsAst): PhysicalLocation? {
        val metaAstNode = astNode.astNode()
        val contentBefore =
            fileContent.substring(
                0,
                if (metaAstNode.startOffset.toInt() < fileContent.length)
                    metaAstNode.startOffset.toInt()
                else fileContent.length,
            )
        val upTo = contentBefore.split(lineSeparator)
        val contentBeforeAndIn =
            fileContent.substring(0, min(metaAstNode.endOffset.toInt(), fileContent.length))
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

    fun handlePathForRef(rsPath: RsPath): Name? {

        val qualifierName =
            rsPath.qualifier.firstOrNull()?.let { qualifier -> handlePathForRef(qualifier) }

        return rsPath.segment?.nameRef?.text?.let { text ->
            newName(text, namespace = qualifierName)
        }
    }

    /**
     * This function replaces the keyword prefixes of a path in a name with the concrete value that
     * is defined by the current scope.
     */
    fun handleKeywordsInNames(name: Name): Name? {
        name.parent?.let { parent ->
            return when (name.localName) {
                "super" -> handleKeywordsInNames(parent)?.parent
                else -> newName(name.localName, namespace = handleKeywordsInNames(parent))
            }
        }

        val current = scopeManager.currentNamespace

        return when (name.localName) {
            "self" -> current
            "super" -> current?.parent
            "crate" -> null
            else -> name
        }
    }

    override fun setComment(node: Node, astNode: RsAst) {
        node.comment = astNode.astNode().comments
    }
}
