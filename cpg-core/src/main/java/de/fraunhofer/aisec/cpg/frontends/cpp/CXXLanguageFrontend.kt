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
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Annotation
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.nio.file.Path
import org.eclipse.cdt.core.dom.ast.*
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTReferenceOperator
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage
import org.eclipse.cdt.core.dom.parser.AbstractCLikeLanguage
import org.eclipse.cdt.core.index.IIndexFileLocation
import org.eclipse.cdt.core.model.ILanguage
import org.eclipse.cdt.core.parser.DefaultLogService
import org.eclipse.cdt.core.parser.FileContent
import org.eclipse.cdt.core.parser.IncludeFileContentProvider
import org.eclipse.cdt.core.parser.ScannerInfo
import org.eclipse.cdt.internal.core.dom.parser.ASTNode
import org.eclipse.cdt.internal.core.dom.parser.ASTTranslationUnit
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName
import org.eclipse.cdt.internal.core.model.ASTStringUtil
import org.eclipse.cdt.internal.core.parser.IMacroDictionary
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContent
import org.eclipse.cdt.internal.core.parser.scanner.InternalFileContentProvider
import org.eclipse.core.runtime.CoreException
import org.slf4j.LoggerFactory

/**
 * The language frontend for translating C/C++ languages into the graph. It uses Eclipse CDT to
 * parse the actual source code into an AST.
 *
 * Based on the file ending (.c or .cpp) different dialects of Eclipse CDT are used ([GCCLanguage]
 * ad [GPPLanguage]). This enables us (to some degree) to deal with the finer difference between C
 * and C++ code.
 */
class CXXLanguageFrontend(
    language: Language<CXXLanguageFrontend>,
    config: TranslationConfiguration,
    scopeManager: ScopeManager
) : LanguageFrontend(language, config, scopeManager) {

    /**
     * The dialect used by this language frontend, either [GCCLanguage] for C or [GPPLanguage] for
     * C++.
     */
    var dialect: AbstractCLikeLanguage? = null

    /**
     * Implements an [IncludeFileContentProvider] which features an inclusion/exclusion list for
     * header files.
     */
    private val includeFileContentProvider: IncludeFileContentProvider =
        object : InternalFileContentProvider() {
            /**
             * Returns the content of this path, without any cache.
             *
             * @return the content of the path of null if it is to be excluded
             */
            private fun getContentUncached(path: String): InternalFileContent? {
                if (!getInclusionExists(path)) {
                    return null
                }

                // check, if the file is on the blacklist
                if (absoluteOrRelativePathIsInList(Path.of(path), config.includeBlocklist)) {
                    LOGGER.debug("Blacklisting include file: {}", path)
                    return null
                }

                // check, if the white-list exists at all
                if (
                    hasIncludeWhitelist() && // and ignore the file if it is not on the whitelist
                    !absoluteOrRelativePathIsInList(Path.of(path), config.includeWhitelist)
                ) {
                    LOGGER.debug("Include file {} not on the whitelist. Ignoring.", path)
                    return null
                }
                LOGGER.debug("Loading include file {}", path)
                val content = FileContent.createForExternalFileLocation(path)
                return content as? InternalFileContent
            }

            private fun hasIncludeWhitelist(): Boolean {
                return config.includeWhitelist.isNotEmpty()
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
            private fun absoluteOrRelativePathIsInList(path: Path, list: List<Path>?): Boolean {
                // Path cannot be in the list if its empty or null
                if (list.isNullOrEmpty()) {
                    return false
                }

                // Check, if the absolute header path is in the list
                if (list.contains(path)) {
                    return true
                }

                // Check for relative path based on the top level and all include paths
                val includeLocations: MutableList<Path> = ArrayList()
                val topLevel = config.topLevel
                if (topLevel != null) {
                    includeLocations.add(topLevel.toPath().toAbsolutePath())
                }
                includeLocations.addAll(config.includePaths)

                // We need to find the proper include location for our relative path. Any location
                // is valid, if we can
                // find that the include-location + the path is contained in the list of paths we
                // are looking for.
                return includeLocations.any { include ->
                    // try to resolve path relatively
                    try {
                        val relative = include.relativize(path)
                        return list.contains(relative)
                    } catch (e: IllegalArgumentException) {
                        return false
                    }
                }
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

    private val comments = HashMap<Pair<String, Int>, String>()

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        val content = FileContent.createForExternalFileLocation(file.absolutePath)

        // include paths
        val includePaths: MutableList<String> = ArrayList()
        if (config.topLevel != null) {
            includePaths.add(config.topLevel.toPath().toAbsolutePath().toString())
        }

        val symbols: HashMap<String, String> = HashMap()
        symbols.putAll(config.symbols)
        includePaths.addAll(config.includePaths.map { it.toAbsolutePath().toString() })

        config.compilationDatabase?.getIncludePaths(file)?.let { includePaths.addAll(it) }
        config.compilationDatabase?.getSymbols(file)?.let { symbols.putAll(it) }

        val scannerInfo = ScannerInfo(symbols, includePaths.toTypedArray())
        val log = DefaultLogService()
        val opts = ILanguage.OPTION_PARSE_INACTIVE_CODE // | ILanguage.OPTION_ADD_COMMENTS;
        return try {
            var bench = Benchmark(this.javaClass, "Parsing sourcefile ${file.name}")

            // Set parser language, based on file extension
            this.dialect =
                if (file.extension == "c") {
                    GCCLanguage.getDefault()
                } else {
                    GPPLanguage.getDefault()
                }

            val translationUnit =
                this.dialect?.getASTTranslationUnit(
                    content,
                    scannerInfo,
                    includeFileContentProvider,
                    null,
                    opts,
                    log
                ) as ASTTranslationUnit
            val length = translationUnit.length
            LOGGER.info(
                "Parsed {} bytes in ${file.name} corresponding roughly to {} LoC",
                length,
                length / 50
            )
            bench.stop()
            bench = Benchmark(this.javaClass, "Transforming ${file.name} to CPG")
            if (config.debugParser) {
                explore(translationUnit, 0)
            }

            for (c in translationUnit.comments) {
                if (c.rawSignature.isEmpty()) {
                    continue
                }

                if (c.fileLocation == null) {
                    LOGGER.warn("Found comment with null location in {}", translationUnit.filePath)
                    continue
                }

                comments[Pair(c.fileLocation.fileName, c.fileLocation.startingLineNumber)] =
                    c.rawSignature
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
        if (astNode !is ASTNode) {
            return null
        }
        val node = astNode as ASTNode
        val fLocation = node.fileLocation ?: return null
        val lineBreaks: IntArray =
            try {
                val fLoc = getField(fLocation.javaClass, "fLocationCtx")
                fLoc.trySetAccessible()
                val locCtx = fLoc[fLocation]
                val fLineOffsets = getField(locCtx.javaClass, "fLineOffsets")
                val getLineNumber = getMethod(locCtx.javaClass, "getLineNumber", Int::class.java)
                fLineOffsets.trySetAccessible()

                // force to cache line numbers, this calls computeLineOffsets internally
                getLineNumber.trySetAccessible()
                getLineNumber.invoke(locCtx, 0)

                fLineOffsets[locCtx] as IntArray
            } catch (e: ReflectiveOperationException) {
                LOGGER.warn(
                    "Reflective retrieval of AST node source failed. Falling back to getRawSignature()"
                )
                IntArray(0)
            } catch (e: ClassCastException) {
                LOGGER.warn(
                    "Reflective retrieval of AST node source failed. Falling back to getRawSignature()"
                )
                IntArray(0)
            } catch (e: NullPointerException) {
                LOGGER.warn(
                    "Reflective retrieval of AST node source failed. Cannot reliably determine content of the file that contains the node"
                )
                return null
            }

        // our start line, indexed by 0
        val startLine = node.fileLocation.startingLineNumber - 1

        // our end line, indexed by 0
        val endLine = node.fileLocation.endingLineNumber - 1

        // our start column, index by 0
        val startColumn =
            if (startLine == 0) {
                // if we are in the first line, the start column is just the node offset
                node.fileLocation.nodeOffset
            } else {
                // otherwise, we need to calculate the difference to the previous line break
                node.fileLocation.nodeOffset -
                    lineBreaks[startLine - 1] -
                    1 // additional -1 because of the '\n' itself
            }

        // our end column, index by 0
        val endColumn =
            if (endLine == 0) {
                // if we are in the first line, the end column is just the node offset
                node.fileLocation.nodeOffset + node.fileLocation.nodeLength
            } else {
                // otherwise, we need to calculate the difference to the previous line break
                (node.fileLocation.nodeOffset + node.fileLocation.nodeLength) -
                    lineBreaks[endLine - 1] -
                    1 // additional -1 because of the '\n' itself
            }

        // for a SARIF compliant format, we need to add +1, since its index begins at 1 and
        // not 0
        val region = Region(startLine + 1, startColumn + 1, endLine + 1, endColumn + 1)
        return PhysicalLocation(Path.of(node.containingFilename).toUri(), region)
    }

    /**
     * Processes C++ [attributes](https://en.cppreference.com/w/cpp/language/attributes) into
     * [Annotation] nodes.
     *
     * @param node the node to process
     * @param owner the AST node which holds the attribute
     */
    fun processAttributes(node: Node, owner: IASTNode) {
        if (config.processAnnotations && owner is IASTAttributeOwner) { // set attributes
            node.addAnnotations(handleAttributes(owner))
        }
    }

    private fun handleAttributes(owner: IASTAttributeOwner): List<Annotation> {
        val list: MutableList<Annotation> = ArrayList()
        for (attribute in owner.attributes) {
            val annotation = newAnnotation(String(attribute.name), attribute.rawSignature)

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
        val expression: Expression =
            when (token.tokenType) {
                1 -> // a variable
                newDeclaredReferenceExpression(code, UnknownType.getUnknownType(language), code)
                2 -> // an integer
                newLiteral(code.toInt(), newPrimitiveType("int"), code)
                130 -> // a string
                newLiteral(
                        if (code.length >= 2) code.substring(1, code.length - 1) else "",
                        newPrimitiveType("char").const().reference(),
                        code
                    )
                else -> newLiteral(code, newPrimitiveType("char").const().reference(), code)
            }
        return newAnnotationMember("", expression, code)
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

    @Throws(NoSuchMethodException::class)
    private fun getMethod(
        type: Class<*>,
        methodName: String,
        vararg parameterTypes: Class<*>
    ): Method {
        return try {
            type.getDeclaredMethod(methodName, *parameterTypes)
        } catch (e: NoSuchMethodException) {
            if (type.superclass != null) {
                return getMethod(type.superclass, methodName, *parameterTypes)
            }
            throw e
        }
    }

    override fun <S, T> setComment(s: S, ctx: T) {
        if (ctx is ASTNode && s is Node) {
            val cpgNode = s as Node
            val location = cpgNode.location ?: return

            // No location, no comment

            val loc: Pair<String, Int> =
                Pair(location.artifactLocation.uri.path, location.region.endLine)
            comments[loc]?.let {
                // only exact match for now}
                cpgNode.comment = it
            }
            // TODO: handle orphanComments? i.e. comments which do not correspond to one line
            // todo: what to do with comments which are in a line which contains multiple
            // statements?
        }
    }

    /**
     * Returns the [Type] that is represented by the [declarator] and [specifier]. This tries to
     * resolve as much information about the type on its own using by analyzing the AST of the
     * supplied declarator and specifier. Finally, [TypeParser.createFrom] is invoked on the
     * innermost type, but all other type adjustments, such as creating a [PointerType] is done
     * within this method.
     *
     * Optionally, a [hint] in the form of an existing [Declaration] can be provided. The idea
     * behind this, is that in some scenarios we create the [Declaration] before the type and in
     * some, we derive the declaration from the type. In the first one, we might get some necessary
     * information from the declaration, that influences the type parsing. One such example is that
     * we check, whether a declaration is a [ConstructorDeclaration] and return an [ObjectType] that
     * corresponds with the record name it instantiates.
     *
     * @param hint an optional [Declaration], which serves as a parsing hint.
     */
    fun typeOf(
        declarator: IASTDeclarator,
        specifier: IASTDeclSpecifier,
        hint: Declaration? = null
    ): Type {
        // Retrieve the "name" of this type, including qualifiers.
        // TODO: In the future, we should parse the qualifiers, such as const here, instead of in
        //  the TypeParser
        var name = ASTStringUtil.getSignatureString(specifier, null)

        var type =
            when (specifier) {
                is IASTSimpleDeclSpecifier -> {
                    if (hint is ConstructorDeclaration) {
                        parseType(hint.name)
                    } else {
                        // A primitive type
                        parseType(name)
                    }
                }
                is IASTNamedTypeSpecifier -> {
                    val nameDecl = specifier.name
                    name =
                        if (nameDecl is CPPASTQualifiedName) {
                            // For some reason the legacy type system does not keep the language
                            // specific namespace delimiters, and for backwards compatibility, we
                            // are keeping this behaviour (for now).
                            name.replace("::", ".")
                        } else {
                            name
                        }

                    TypeParser.createFrom(name, true, this)
                }
                is IASTCompositeTypeSpecifier -> {
                    // A class. This actually also declares the class. At the moment, we handle this
                    // in handleSimpleDeclaration, but we might want to move it here
                    TypeParser.createFrom(name, true, this)
                }
                is IASTElaboratedTypeSpecifier -> {
                    // A class or struct
                    TypeParser.createFrom(name, true, this)
                }
                else -> {
                    UnknownType.getUnknownType(language)
                }
            }

        type = TypeManager.getInstance().registerType(type)
        type = this.adjustType(declarator, type)

        return type
    }

    /**
     * This is a little helper function, primarily used by [typeOf]. It's primary purpose is to
     * "adjust" the [incoming] type based on the [declarator]. This is needed because the type
     * information in C/C++ are split into a declarator and declaration specifiers.
     */
    private fun adjustType(declarator: IASTDeclarator, incoming: Type): Type {
        var type = incoming

        // First, look at the declarator's pointer operator, to see whether, we need to wrap the
        // type into a pointer or similar
        for (op in declarator.pointerOperators) {
            type =
                when (op) {
                    is IASTPointer -> {
                        type.reference(PointerType.PointerOrigin.POINTER)
                    }
                    is ICPPASTReferenceOperator -> {
                        ReferenceType(type.storage, type.qualifier, type)
                    }
                    else -> {
                        type
                    }
                }
        }

        // Check, if we are an array type
        if (declarator is IASTArrayDeclarator) {
            for (mod in declarator.arrayModifiers) {
                type = type.reference(PointerType.PointerOrigin.ARRAY)
            }
        } else if (declarator is IASTStandardFunctionDeclarator) {
            // Loop through the parameters
            var paramTypes = declarator.parameters.map { typeOf(it.declarator, it.declSpecifier) }

            var i = 0
            // Filter out void
            paramTypes =
                paramTypes.filter {
                    if (it is IncompleteType) {
                        i++
                        return@filter false
                    }

                    return@filter true
                }

            if (i > 1) {
                // TODO: We should actually report this as a "problem" somehow
                LOGGER.error(
                    "Type $type contains more than one void parameter. This is not allowed"
                )
            }

            // We need to construct a function type here. The existing type
            // so far is the return value. We then add the parameters and give it a name.
            val name =
                paramTypes.joinToString(
                    FunctionDeclaration.COMMA + FunctionDeclaration.WHITESPACE,
                    FunctionDeclaration.BRACKET_LEFT,
                    FunctionDeclaration.BRACKET_RIGHT
                ) { it.typeName } + type.typeName
            type =
                FunctionType(name, paramTypes, listOf(type), language, type.qualifier, type.storage)
        }

        // Lastly, there might be further nested declarators that adjust the type further.
        // However, if the type is already a function pointer type, we can ignore it. In the future,
        // this will probably actually make the difference between a function type and a function
        // pointer type.
        if (declarator.nestedDeclarator != null && type !is FunctionPointerType) {
            type = adjustType(declarator.nestedDeclarator, type)
        }

        // Make sure, the type manager knows about this type
        return TypeManager.getInstance().registerType(type)
    }

    companion object {
        @JvmField val CXX_EXTENSIONS = mutableListOf(".c", ".cpp", ".cc")
        @JvmField val CXX_HEADER_EXTENSIONS = mutableListOf(".h", ".hpp")
        private val LOGGER = LoggerFactory.getLogger(CXXLanguageFrontend::class.java)

        private fun explore(node: IASTNode, indent: Int) {
            val children = node.children
            val s = StringBuilder()

            s.append(" ".repeat(indent))
            if (log.isTraceEnabled) {
                log.trace(
                    "{}{} -> {}",
                    s,
                    node.javaClass.simpleName,
                    node.rawSignature.replace('\n', '\\').replace('\t', ' ')
                )
            }

            for (astNode in children) {
                explore(astNode, indent + 2)
            }
        }
    }
}
