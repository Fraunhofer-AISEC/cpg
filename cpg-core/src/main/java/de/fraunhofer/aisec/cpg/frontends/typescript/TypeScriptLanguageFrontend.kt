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
import de.fraunhofer.aisec.cpg.frontends.FrontendUtils
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.Annotation
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.io.File.createTempFile
import java.io.FileReader
import java.io.LineNumberReader
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.checkerframework.checker.nullness.qual.NonNull

/**
 * This language frontend adds experimental support for TypeScript. It is definitely not feature
 * complete, but can be used to parse simple typescript snippets through the official typescript
 * parser written in TypeScript / nodejs. It includes a simple nodejs script that invokes this
 * parser in `src/main/nodejs`. It basically dumps the AST in a JSON structure on stdout and this
 * input is parsed by this frontend.
 *
 * Because TypeScript is a strict super-set of JavaScript, this frontend can also be used to parse
 * JavaScript. However, this is not properly tested. Furthermore, the official TypeScript parser
 * also has built-in support for React dialects TSX and JSX.
 */
@ExperimentalTypeScript
class TypeScriptLanguageFrontend(
    config: @NonNull TranslationConfiguration,
    scopeManager: ScopeManager?
) : LanguageFrontend(config, scopeManager, ".") {

    val declarationHandler = DeclarationHandler(this)
    val statementHandler = StatementHandler(this)
    val expressionHandler = ExpressionHandler(this)
    val typeHandler = TypeHandler(this)

    var currentFileContent: String? = null

    val mapper = jacksonObjectMapper()

    companion object {
        @kotlin.jvm.JvmField var TYPESCRIPT_EXTENSIONS: List<String> = listOf(".ts", ".tsx")

        @kotlin.jvm.JvmField var JAVASCRIPT_EXTENSIONS: List<String> = listOf(".js", ".jsx")

        private val parserFile: File = createTempFile("parser", ".js")

        init {
            val link = this::class.java.getResourceAsStream("/nodejs/parser.js")
            link?.use {
                log.info(
                    "Extracting parser.js out of resources to {}",
                    parserFile.absoluteFile.toPath()
                )
                Files.copy(
                    it,
                    parserFile.absoluteFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }

    override fun parse(file: File): TranslationUnitDeclaration {
        // Necessary to not read file contents several times
        currentFileContent = file.readText()
        if (!parserFile.exists()) {
            throw TranslationException("parser.js not found @ ${parserFile.absolutePath}")
        }

        val p =
            Runtime.getRuntime().exec(arrayOf("node", parserFile.absolutePath, file.absolutePath))

        val node = mapper.readValue(p.inputStream, TypeScriptNode::class.java)

        TypeManager.getInstance().setLanguageFrontend(this)

        val translationUnit = this.declarationHandler.handle(node) as TranslationUnitDeclaration

        handleComments(file, translationUnit)

        return translationUnit
    }

    /**
     * Extracts comments from the file with a regular expression and calls a best effort approach
     * function that matches them to the closes ast node in the cpg.
     * @param file The source of comments
     * @param translationUnit the ast root node which children get the comments associated to
     */
    fun handleComments(file: File, translationUnit: TranslationUnitDeclaration) {
        // Extracting comments with regex, not ideal, as you need a context sensitive parser. but
        // the parser does not support comments so we
        // use a regex as best effort approach. We may recognize something as a comment, which is
        // acceptable.
        val matches: Sequence<MatchResult> =
            Regex("(?:/\\*((?:[^*]|(?:\\*+[^*/]))*)\\*+/)|(?://(.*))").findAll(currentFileContent!!)
        matches.toList().forEach {
            val groups = it.groups
            groups[0]?.let {
                var comment = it.value

                val commentRegion = getRegionFromStartEnd(file, it.range.first, it.range.last)

                // We only want the acutal comment text and therefore take the value we captured in
                // the first, or second group.
                // Only as a last resoort we take the entire match, although this should never ocure
                comment = groups[1]?.value ?: (groups[2]?.value ?: it.value)

                comment = comment.trim()

                comment = comment.trim('\n')

                FrontendUtils.matchCommentToNode(
                    comment,
                    commentRegion ?: translationUnit.location!!.region,
                    translationUnit
                )
            }
        }
    }

    override fun <T : Any?> getCodeFromRawNode(astNode: T): String? {
        return if (astNode is TypeScriptNode) {
            return astNode.code
        } else {
            null
        }
    }

    override fun <T : Any?> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        return if (astNode is TypeScriptNode) {

            var position = astNode.location.pos

            // Correcting node positions as we have noticed that the parser computes wrong
            // positions, it is apparent when
            // a files starts with a comment
            astNode.code?.let {
                val code = it
                currentFileContent?.let { position = it.indexOf(code, position) }
            }

            // From here on the invariant 'astNode.location.end - position != astNode.code!!.length'
            // should hold, only exceptions
            // are mispositioned empty ast elements

            val region =
                getRegionFromStartEnd(File(astNode.location.file), position, astNode.location.end)
            return PhysicalLocation(File(astNode.location.file).toURI(), region ?: Region())
        } else {
            null
        }
    }

    fun getRegionFromStartEnd(file: File, start: Int, end: Int): Region? {
        val lineNumberReader: LineNumberReader = LineNumberReader(FileReader(file))

        // Start and end position given by the parser are sometimes including spaces in front of the
        // code ans
        // loc.end - loc.pos > code.length. This is cause by the parser and results in unexpected
        // but correct regions
        // if the start and end positions are assumed to be correct.

        lineNumberReader.skip(start.toLong())
        val startLine = lineNumberReader.lineNumber + 1
        lineNumberReader.skip((end - start).toLong())
        val endLine = lineNumberReader.lineNumber + 1

        val translationUnitSignature = currentFileContent!!
        val region: Region? =
            FrontendUtils.parseColumnPositionsFromFile(
                translationUnitSignature,
                end - start,
                start,
                startLine,
                endLine
            )
        return region
    }

    override fun <S : Any?, T : Any?> setComment(s: S, ctx: T) {
        // not implemented
    }

    internal fun getIdentifierName(node: TypeScriptNode) =
        this.getCodeFromRawNode(node.firstChild("Identifier")) ?: ""

    fun processAnnotations(node: Node, astNode: TypeScriptNode) {
        // filter for decorators
        astNode.children
            ?.filter { it.type == "Decorator" }
            ?.map { handleDecorator(it) }
            ?.let { node.addAnnotations(it) }
    }

    private fun handleDecorator(node: TypeScriptNode): Annotation {
        // a decorator can contain a call expression with additional arguments
        val call = node.firstChild("CallExpression")
        if (call != null) {
            val call = this.expressionHandler.handle(call) as CallExpression

            val annotation =
                NodeBuilder.newAnnotation(call.name, this.getCodeFromRawNode(node) ?: "")

            annotation.members =
                call.arguments.map { NodeBuilder.newAnnotationMember("", it, it.code ?: "") }

            call.disconnectFromGraph()

            return annotation
        } else {
            // or a decorator just has a simple identifier
            val name = this.getIdentifierName(node)

            val annotation = NodeBuilder.newAnnotation(name, this.getCodeFromRawNode(node) ?: "")

            return annotation
        }
    }
}

class Location(var file: String, var pos: Int, var end: Int)

class TypeScriptNode(
    var type: String,
    var children: List<TypeScriptNode>?,
    var location: Location,
    var code: String?
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
