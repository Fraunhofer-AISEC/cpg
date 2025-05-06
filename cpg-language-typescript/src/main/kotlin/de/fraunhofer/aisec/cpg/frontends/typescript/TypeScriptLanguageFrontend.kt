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
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.FrontendUtils
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException as CpgTranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Annotation
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.io.File.createTempFile
import java.io.FileReader
import java.io.InputStreamReader
import java.io.LineNumberReader
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * This language frontend adds experimental support for TypeScript. It is definitely not feature
 * complete, but can be used to parse simple typescript snippets through the official typescript
 * parser written in TypeScript. It includes a simple binary (built by deno) that invokes this
 * parser. It basically dumps the AST in a JSON structure on stdout and this input is parsed by this
 * frontend.
 *
 * Because TypeScript is a strict super-set of JavaScript, this frontend can also be used to parse
 * JavaScript. However, this is not properly tested. Furthermore, the official TypeScript parser
 * also has built-in support for React dialects TSX and JSX.
 */
class TypeScriptLanguageFrontend(
    ctx: TranslationContext,
    language: Language<TypeScriptLanguageFrontend>,
) : LanguageFrontend<TypeScriptNode, TypeScriptNode>(ctx, language) {

    val declarationHandler = DeclarationHandler(this)
    val statementHandler = StatementHandler(this)
    val expressionHandler = ExpressionHandler(this)
    val typeHandler = TypeHandler(this)

    private var currentFileContent: String? = null

    internal val mapper = jacksonObjectMapper()

    companion object {
        internal val parserFile: File = createTempFile("parser", "")

        init {
            val arch = System.getProperty("os.arch")
            val os: String =
                when {
                    System.getProperty("os.name").startsWith("Mac") -> {
                        "macos"
                    }
                    System.getProperty("os.name").startsWith("Linux") -> {
                        "linux"
                    }
                    else -> {
                        "windows"
                    }
                }

            // Note: Path adjusted to always look in /typescript/
            val resourcePath = "/typescript/parser-$os-$arch"
            val link = this::class.java.getResourceAsStream(resourcePath)

            if (link == null) {
                // Handle case where parser binary is not found
                log.error("TypeScript parser binary not found at resource path: {}", resourcePath)
                // Optionally attempt fallback or throw an exception
                val fallbackResourcePath = "/typescript/parser-$os-x86_64"
                val fallbackStream = this::class.java.getResourceAsStream(fallbackResourcePath)
                if (fallbackStream != null) {
                    log.warn("Could not find parser for specific arch $arch, using fallback x86_64")
                    Files.copy(
                        fallbackStream,
                        parserFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                    parserFile.setExecutable(true)
                    fallbackStream.close()
                } else {
                    log.error(
                        "TypeScript parser binary also not found at fallback path: {}",
                        fallbackResourcePath,
                    )
                    throw CpgTranslationException("TypeScript parser binary not found")
                }
            } else {
                log.info(
                    "Extracting TypeScript parser from resources ({}) to {}",
                    resourcePath,
                    parserFile.absoluteFile.toPath(),
                )
                Files.copy(
                    link,
                    parserFile.absoluteFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
                parserFile.setExecutable(true)
                link.close() // Close the stream
            }
        }
    }

    override fun parse(file: File): TranslationUnitDeclaration {
        currentFileContent = file.readText()
        if (!parserFile.exists()) {
            throw CpgTranslationException(
                "TypeScript parser not found @ ${parserFile.absolutePath}"
            )
        }

        // Always use the typescript language flag for this frontend
        val languageFlag = "--language=typescript"

        log.info(
            "Executing TypeScript parser for {} with flag: {}",
            file.absolutePath,
            languageFlag,
        )
        val process =
            try {
                Runtime.getRuntime()
                    .exec(arrayOf(parserFile.absolutePath, languageFlag, file.absolutePath))
            } catch (e: Exception) {
                throw CpgTranslationException("Error executing TypeScript parser: ${e.message}", e)
            }

        val stdInput = InputStreamReader(process.inputStream)
        val stdError = InputStreamReader(process.errorStream)
        val jsonResult = stdInput.readText()
        val errors = stdError.readText()
        stdInput.close()
        stdError.close()

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            log.error("TypeScript parser exited with code {}: {}", exitCode, errors)
            throw CpgTranslationException("TypeScript parser failed: $errors")
        }
        if (errors.isNotEmpty()) {
            log.warn("TypeScript parser reported errors/warnings: {}", errors)
        }

        // Deserialize directly into TypeScriptNode for existing TS handling
        val node = mapper.readValue(jsonResult, TypeScriptNode::class.java)

        // Use existing TypeScript handlers
        val translationUnit = this.declarationHandler.handle(node) as TranslationUnitDeclaration
        handleComments(file, translationUnit) // Handle TS comments

        return translationUnit
    }

    override fun typeOf(type: TypeScriptNode): Type {
        return typeHandler.handleNode(type)
    }

    /**
     * Extracts comments from the file with a regular expression and calls a best effort approach
     * function that matches them to the closes ast node in the cpg.
     *
     * @param file The source of comments
     * @param translationUnit the ast root node which children get the comments associated to
     */
    fun handleComments(file: File, translationUnit: TranslationUnitDeclaration) {
        // Extracting comments with regex, not ideal, as you need a context sensitive parser. but
        // the parser does not support comments so we
        // use a regex as best effort approach. We may recognize something as a comment, which is
        // acceptable.
        val matches: Sequence<MatchResult>? =
            currentFileContent?.let {
                Regex("(?:/\\*((?:[^*]|(?:\\*+[^*/]))*)\\*+/)|(?://(.*))").findAll(it)
            }
        matches?.toList()?.forEach { result ->
            val groups = result.groups
            groups[0]?.let {
                val commentRegion = getRegionFromStartEnd(file, it.range.first, it.range.last)

                // We only want the actual comment text and therefore take the value we captured in
                // the first, or second group.
                // Only as a last resort we take the entire match, although this should never occurs
                var comment = groups[1]?.value ?: (groups[2]?.value ?: it.value)

                comment = comment.trim()

                comment = comment.trim('\n')

                FrontendUtils.matchCommentToNode(
                    comment,
                    commentRegion ?: translationUnit.location?.region ?: Region(),
                    translationUnit,
                )
            }
        }
    }

    override fun codeOf(astNode: TypeScriptNode): String? {
        return astNode.code
    }

    override fun locationOf(astNode: TypeScriptNode): PhysicalLocation {
        var position = astNode.location.pos

        // Correcting node positions as we have noticed that the parser computes wrong
        // positions, it is apparent when a file starts with a comment
        astNode.code?.let { code ->
            currentFileContent?.let { position = it.indexOf(code, position) }
        }

        // From here on the invariant 'astNode.location.end - position != astNode.code!!.length'
        // should hold, only exceptions are mispositioned empty ast elements
        val region =
            getRegionFromStartEnd(File(astNode.location.file), position, astNode.location.end)
        return PhysicalLocation(File(astNode.location.file).toURI(), region ?: Region())
    }

    fun getRegionFromStartEnd(file: File, start: Int, end: Int): Region? {
        val lineNumberReader = LineNumberReader(FileReader(file))

        // Start and end position given by the parser are sometimes including spaces in front of the
        // code and loc.end - loc.pos > code.length. This is caused by the parser and results in
        // unexpected
        // but correct regions if the start and end positions are assumed to be correct.
        lineNumberReader.skip(start.toLong())
        val startLine = lineNumberReader.lineNumber + 1
        lineNumberReader.skip((end - start).toLong())
        val endLine = lineNumberReader.lineNumber + 1

        val translationUnitSignature = currentFileContent
        val region =
            translationUnitSignature?.let {
                FrontendUtils.parseColumnPositionsFromFile(
                    it,
                    end - start,
                    start,
                    startLine,
                    endLine,
                )
            }
        return region
    }

    override fun setComment(node: Node, astNode: TypeScriptNode) {
        // not implemented
    }

    internal fun getIdentifierName(node: TypeScriptNode) =
        node.firstChild("Identifier")?.let { this.codeOf(it) } ?: ""

    fun processAnnotations(node: Node, astNode: TypeScriptNode) {
        // filter for decorators
        astNode.children
            ?.filter { it.type == "Decorator" }
            ?.map { handleDecorator(it) }
            ?.let { node.annotations += it }
    }

    private fun handleDecorator(node: TypeScriptNode): Annotation {
        // a decorator can contain a call expression with additional arguments
        val callExpr = node.firstChild("CallExpression")
        return if (callExpr != null) {
            val call = this.expressionHandler.handle(callExpr) as CallExpression

            val annotation = newAnnotation(call.name.localName, rawNode = node)

            annotation.members =
                call.arguments
                    .map { newAnnotationMember("", it).codeAndLocationFrom(it) }
                    .toMutableList()

            call.disconnectFromGraph()

            annotation
        } else {
            // or a decorator just has a simple identifier
            val name = this.getIdentifierName(node)

            newAnnotation(name, rawNode = node)
        }
    }
}

class Location(var file: String, var pos: Int, var end: Int)

class TypeScriptNode(
    var type: String,
    var children: List<TypeScriptNode>?,
    var location: Location,
    var code: String?,
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
