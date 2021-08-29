/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.HasDefaultArguments
import de.fraunhofer.aisec.cpg.frontends.HasTemplates
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Annotation
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.lang.reflect.Field
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors
import org.eclipse.cdt.core.dom.ast.*
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage
import org.eclipse.cdt.core.index.IIndexFileLocation
import org.eclipse.cdt.core.model.ILanguage
import org.eclipse.cdt.core.parser.DefaultLogService
import org.eclipse.cdt.core.parser.FileContent
import org.eclipse.cdt.core.parser.IncludeFileContentProvider
import org.eclipse.cdt.core.parser.ScannerInfo
import org.eclipse.cdt.internal.core.dom.parser.ASTNode
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTranslationUnit
import org.eclipse.cdt.internal.core.parser.IMacroDictionary
import org.eclipse.cdt.internal.core.parser.scanner.AbstractCharArray
import org.eclipse.cdt.internal.core.parser.scanner.CharArray
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider
import org.eclipse.core.runtime.CoreException
import org.slf4j.LoggerFactory

/**
 * The language frontend for translating CXX languages into the graph. It uses Eclipse CDT to parse
 * the actual source code into an AST.
 *
 * Frontend for ONE CXX File
 */
class CXXLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, "::"), HasDefaultArguments, HasTemplates {
    private val includeFileContentProvider: IncludeFileContentProvider =
        object : InternalFileContentProvider() {
            private fun getContentUncached(path: String): InternalFileContent? {
                if (!getInclusionExists(path)) {
                    LOGGER.debug("Include file not found: {}", path)
                    return null
                }

                // check, if the file is on the blacklist
                if (absoluteOrRelativePathIsInList(path, config.includeBlacklist)) {
                    LOGGER.debug("Blacklisting include file: {}", path)
                    return null
                }

                // check, if the white-list exists at all
                if (hasIncludeWhitelist() && // and ignore the file if it is not on the whitelist
                    !absoluteOrRelativePathIsInList(path, config.includeWhitelist)
                ) {
                    LOGGER.debug("Include file {} not on the whitelist. Ignoring.", path)
                    return null
                }
                LOGGER.debug("Loading include file {}", path)
                val content = FileContent.createForExternalFileLocation(path)
                return content as InternalFileContent
            }

            private fun hasIncludeWhitelist(): Boolean {
                return config.includeWhitelist != null && !config.includeWhitelist.isEmpty()
            }

            /**
             * This utility function checks, if the specified path is in the included list, either
             * as an absolute path or as a path relative to the translation configurations top level
             * or include paths
             *
             * @param path the absolute path to look for
             * @param list the list of paths to look for, either relative or absolute
             * @return true, if the path is in the list, false otherwise
             */
            private fun absoluteOrRelativePathIsInList(path: String, list: List<String>?): Boolean {
                // path cannot be in the list if its empty or null
                if (list == null || list.isEmpty()) {
                    return false
                }

                // check, if the absolute header path is in the list
                if (list.contains(path)) {
                    return true
                }

                // check for relative path based on the top level and all include paths
                val includeLocations: MutableList<Path> = ArrayList()
                val topLevel = config.topLevel
                if (topLevel != null) {
                    includeLocations.add(topLevel.toPath().toAbsolutePath())
                }
                includeLocations.addAll(
                    Arrays.stream(config.includePaths)
                        .map { s: String? -> Path.of(s).toAbsolutePath() }
                        .collect(Collectors.toList())
                )
                for (includeLocation in includeLocations) {
                    // try to resolve path relatively
                    val includeFile = Path.of(path)
                    val relative = includeLocation.relativize(includeFile)
                    if (list.contains(relative.toString())) {
                        return true
                    }
                }
                return false
            }

            override fun getContentForInclusion(
                path: String,
                macroDictionary: IMacroDictionary
            ): InternalFileContent? {
                return getContentUncached(path)
            }

            override fun getContentForInclusion(
                ifl: IIndexFileLocation,
                astPath: String
            ): InternalFileContent? {
                return getContentUncached(astPath)
            }
        }
    val declarationHandler = DeclarationHandler(this)
    val declaratorHandler = DeclaratorHandler(this)
    val expressionHandler = ExpressionHandler(this)
    val initializerHandler = InitializerHandler(this)
    val parameterDeclarationHandler = ParameterDeclarationHandler(this)
    val statementHandler = StatementHandler(this)
    private val cachedDeclarations = HashMap<IBinding, Declaration?>()
    private val comments = HashMap<Int, String>()
    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        TypeManager.getInstance().setLanguageFrontend(this)
        val content = FileContent.createForExternalFileLocation(file.absolutePath)

        // include paths
        val includePaths: MutableList<String> = ArrayList()
        if (config.topLevel != null) {
            includePaths.add(config.topLevel.toPath().toAbsolutePath().toString())
        }
        includePaths.addAll(Arrays.asList(*config.includePaths))
        val scannerInfo = ScannerInfo(config.symbols, includePaths.toTypedArray())
        val log = DefaultLogService()
        val opts = ILanguage.OPTION_PARSE_INACTIVE_CODE // | ILanguage.OPTION_ADD_COMMENTS;
        return try {
            var bench = Benchmark(this.javaClass, "Parsing sourcefile")
            val translationUnit =
                GPPLanguage.getDefault()
                    .getASTTranslationUnit(
                        content,
                        scannerInfo,
                        includeFileContentProvider,
                        null,
                        opts,
                        log
                    ) as
                    CPPASTTranslationUnit
            val length = translationUnit.length
            LOGGER.info("Parsed {} bytes corresponding roughly to {} LoC", length, length / 50)
            bench.stop()
            bench = Benchmark(this.javaClass, "Transform to CPG")
            if (config.debugParser) {
                explore(translationUnit, 0)
            }
            for (c in translationUnit.comments) {
                if (c.fileLocation == null) {
                    LOGGER.warn("Found comment with null location in {}", translationUnit.filePath)
                    continue
                }
                comments[c.fileLocation.startingLineNumber] = c.rawSignature
            }
            val translationUnitDeclaration =
                declarationHandler.handleTranslationUnit(translationUnit)
            bench.stop()
            translationUnitDeclaration
        } catch (ex: CoreException) {
            throw TranslationException(ex)
        }
    }

    override fun <T> getCodeFromRawNode(astNode: T): String? {
        if (astNode is ASTNode) {
            val node = astNode as ASTNode
            return node.rawSignature
        }
        return null
    }

    override fun <T> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        if (astNode is ASTNode) {
            val node = astNode as ASTNode
            val fLocation = node.fileLocation
            if (fLocation != null) {
                /* Yes, seriously. getRawSignature() is CPU- and heap-costly, because it does an arraycopy. If parent is the whole TranslationUnit and we are doing this repeatedly, we will end up with OOM and waste time.
                 * We thus do a shortcut and directly access the field containing the source code of a node as a CharArray.
                 * This may break in future versions of CDT parser, when fields are renamed (which is unlikely). In this case, we will go the standard route.
                 * Note, the only reason we are doing this is to compute the start and end columns of the current node.
                 */
                var translationUnitRawSignature: AbstractCharArray = CharArray("")
                translationUnitRawSignature =
                    try {
                        val fLoc = getField(fLocation.javaClass, "fLocationCtx")
                        fLoc.trySetAccessible()
                        val locCtx = fLoc[fLocation]
                        val fSource = getField(locCtx.javaClass, "fSource")
                        fSource.trySetAccessible()
                        fSource[locCtx] as AbstractCharArray
                    } catch (e: ReflectiveOperationException) {
                        LOGGER.warn(
                            "Reflective retrieval of AST node source failed. Cannot reliably determine content of the file that contains the node"
                        )
                        return null
                    } catch (e: ClassCastException) {
                        LOGGER.warn(
                            "Reflective retrieval of AST node source failed. Cannot reliably determine content of the file that contains the node"
                        )
                        return null
                    } catch (e: NullPointerException) {
                        LOGGER.warn(
                            "Reflective retrieval of AST node source failed. Cannot reliably determine content of the file that contains the node"
                        )
                        return null
                    }

                // Get start column by stepping backwards from begin of node to first occurrence of
                // '\n'
                var startColumn = 1
                for (i in node.fileLocation.nodeOffset - 1 downTo 2) {
                    if (i >= translationUnitRawSignature.length) {
                        // Fail gracefully, so that we can at least find out why this fails
                        LOGGER.warn(
                            "Requested index {} exceeds length of translation unit code ({})",
                            i,
                            translationUnitRawSignature.length
                        )
                        return null
                    }
                    if (translationUnitRawSignature[i] == '\n') {
                        break
                    }
                    startColumn++
                }
                val endColumn =
                    getEndColumnIndex(
                        translationUnitRawSignature,
                        node.fileLocation.nodeOffset + node.length
                    )
                val region =
                    Region(
                        fLocation.startingLineNumber,
                        startColumn,
                        fLocation.endingLineNumber,
                        endColumn
                    )
                return PhysicalLocation(Path.of(node.containingFilename).toUri(), region)
            }
        }
        return null
    }

    /**
     * Processes C++ attributes into [Annotation] nodes.
     *
     * @param node the node to process
     * @param owner the AST node which holds the attribute
     */
    fun processAttributes(node: Node, owner: IASTAttributeOwner) {
        if (config.processAnnotations) {
            // set attributes
            node.addAnnotations(handleAttributes(owner))
        }
    }

    private fun handleAttributes(owner: IASTAttributeOwner): List<Annotation> {
        val list: MutableList<Annotation> = ArrayList()
        for (attribute in owner.attributes) {
            val annotation =
                NodeBuilder.newAnnotation(String(attribute.name), attribute.rawSignature)

            // go over the parameters
            if (attribute.argumentClause is IASTTokenList) {
                val members = handleTokenList(attribute.argumentClause as IASTTokenList)
                annotation.members = members
            }
            list.add(annotation)
        }
        return list
    }

    private fun handleTokenList(tokenList: IASTTokenList): List<AnnotationMember> {
        val list: MutableList<AnnotationMember> = ArrayList()
        for (token in tokenList.tokens) {
            // skip commas and such
            if (token.tokenType == 6) {
                continue
            }
            list.add(handleToken(token))
        }
        return list
    }

    private fun handleToken(token: IASTToken): AnnotationMember {
        val code = String(token.tokenCharImage)
        val expression: Expression
        expression =
            when (token.tokenType) {
                1 -> // a variable
                NodeBuilder.newDeclaredReferenceExpression(code, UnknownType.getUnknownType(), code)
                2 -> // an integer
                NodeBuilder.newLiteral(code.toInt(), TypeParser.createFrom("int", true), code)
                130 -> // a string
                NodeBuilder.newLiteral(
                        if (code.length >= 2) code.substring(1, code.length - 1) else "",
                        TypeParser.createFrom("const char*", false),
                        code
                    )
                else ->
                    NodeBuilder.newLiteral(code, TypeParser.createFrom("const char*", false), code)
            }
        return NodeBuilder.newAnnotationMember("", expression, code)
    }

    @Throws(NoSuchFieldException::class)
    private fun getField(type: Class<*>, fieldName: String): Field {
        return try {
            type.getDeclaredField(fieldName)
        } catch (e: NoSuchFieldException) {
            if (type.superclass != null) {
                return getField(type.superclass, fieldName)
            }
            throw e
        }
    }

    fun cacheDeclaration(binding: IBinding, declaration: Declaration?): Declaration? {
        return cachedDeclarations.put(binding, declaration)
    }

    fun getCachedDeclaration(binding: IBinding): Declaration? {
        return cachedDeclarations[binding]
    }

    fun getCachedDeclarations(): Map<IBinding, Declaration?> {
        return cachedDeclarations
    }

    override fun <S, T> setComment(s: S, ctx: T) {
        if (ctx is ASTNode && s is Node) {
            val cpgNode = s as Node
            if (comments.containsKey(cpgNode.location!!.region.endLine)
            ) { // only exact match for now
                cpgNode.comment = comments[cpgNode.location!!.region.endLine]
            }
            // TODO: handle orphanComments? i.e. comments which do not correspond to one line
            // todo: what to do with comments which are in a line which contains multiple
            // statements?
        }
    }

    companion object {
        @JvmField val CXX_EXTENSIONS = java.util.List.of(".c", ".cpp", ".cc")
        @JvmField val CXX_HEADER_EXTENSIONS = java.util.List.of(".h", ".hpp")
        private val LOGGER = LoggerFactory.getLogger(CXXLanguageFrontend::class.java)

        /**
         * Searches in posPrefix to the left until first occurrence of line break and returns the
         * number of characters.
         *
         * This corresponds to the column number of "end" within "posPrefix".
         *
         * @param posPrefix
         * - the positional prefix, which is the string before the column and contains the column
         * defining newline.
         */
        private fun getEndColumnIndex(posPrefix: AbstractCharArray, end: Int): Int {
            var end = end
            var column = 1

            // In case the current element goes until EOF, we need to back up "end" by one.
            try {
                if (end - 1 >= posPrefix.length || posPrefix[end - 1] == '\n') {
                    end = Math.min(end - 1, posPrefix.length - 1)
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                log.error("could not update end ", e)
            }
            for (i in end - 1 downTo 2) {
                if (posPrefix[i] == '\n') {
                    break
                }
                column++
            }
            return column
        }

        private fun explore(node: IASTNode, indent: Int) {
            val children = node.children
            val s = StringBuilder()
            s.append(" ".repeat(indent))
            log.trace(
                "{}{} -> {}",
                s,
                node.javaClass.simpleName,
                node.rawSignature.replace('\n', '\\').replace('\t', ' ')
            )
            for (iastNode in children) {
                explore(iastNode, indent + 2)
            }
        }
    }
}
