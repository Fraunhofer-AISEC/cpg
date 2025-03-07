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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.ResolveInFrontend
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Annotation
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import de.fraunhofer.aisec.cpg.helpers.CommentMatcher
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.CXXExtraPass
import de.fraunhofer.aisec.cpg.passes.DynamicInvokeResolver
import de.fraunhofer.aisec.cpg.passes.configuration.RegisterExtraPass
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
import org.eclipse.cdt.internal.core.dom.parser.c.CASTSimpleDeclSpecifier
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTLiteralExpression
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTemplateId
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTTypeId
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
@RegisterExtraPass(DynamicInvokeResolver::class)
@RegisterExtraPass(CXXExtraPass::class)
open class CXXLanguageFrontend(ctx: TranslationContext, language: Language<CXXLanguageFrontend>) :
    LanguageFrontend<IASTNode, IASTTypeId>(ctx, language) {

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
            var cache = mutableMapOf<String, FileContent>()

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

                return cache.computeIfAbsent(path) {
                    LOGGER.debug("Loading include file {}", path)
                    val content = FileContent.createForExternalFileLocation(path)
                    content
                } as? InternalFileContent
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
                val topLevel = ctx.currentComponent?.topLevel
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
                macroDictionary: IMacroDictionary,
            ): InternalFileContent? {
                return getContentUncached(path)
            }

            override fun getContentForInclusion(
                ifl: IIndexFileLocation,
                astPath: String,
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

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        val content = FileContent.createForExternalFileLocation(file.absolutePath)

        // include paths
        val includePaths = mutableSetOf<String>()
        ctx.currentComponent?.topLevel?.let {
            includePaths.add(it.toPath().toAbsolutePath().toString())
        }

        val symbols: HashMap<String, String> = HashMap()
        symbols.putAll(config.symbols)

        // We aim to behave like clang
        symbols.put("__clang__", "")

        includePaths.addAll(config.includePaths.map { it.toAbsolutePath().toString() })

        config.compilationDatabase?.getIncludePaths(file)?.let { includePaths.addAll(it) }
        if (config.useUnityBuild) {
            // For a unity build, we cannot access the individual symbols per file, but rather only
            // for the whole component
            symbols.putAll(
                config.compilationDatabase?.getAllSymbols(
                    ctx.currentComponent?.name?.localName ?: ""
                ) ?: mutableMapOf()
            )
        } else {
            config.compilationDatabase
                ?.getSymbols(ctx.currentComponent?.name?.localName ?: "", file)
                ?.let { symbols.putAll(it) }
        }

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
                    GPPLanguage()
                }

            val translationUnit =
                this.dialect?.getASTTranslationUnit(
                    content,
                    scannerInfo,
                    includeFileContentProvider,
                    null,
                    opts,
                    log,
                ) as ASTTranslationUnit
            val length = translationUnit.length
            LOGGER.info(
                "Parsed {} bytes in ${file.name} corresponding roughly to {} LoC",
                length,
                length / 50,
            )
            bench.stop()
            bench = Benchmark(this.javaClass, "Transforming ${file.name} to CPG")
            if (config.debugParser) {
                explore(translationUnit, 0)
            }

            val translationUnitDeclaration =
                declarationHandler.handleTranslationUnit(translationUnit)

            for (c in translationUnit.comments) {
                if (c.rawSignature.isNotEmpty()) {
                    locationOf(c)?.let {
                        CommentMatcher()
                            .matchCommentToNode(
                                c.rawSignature,
                                it.region,
                                translationUnitDeclaration,
                                it.artifactLocation,
                            )
                    }
                }
            }

            bench.stop()
            translationUnitDeclaration
        } catch (ex: CoreException) {
            throw TranslationException(ex)
        }
    }

    override fun codeOf(astNode: IASTNode): String? {
        val node = astNode as ASTNode
        return node.rawSignature
    }

    override fun locationOf(astNode: IASTNode): PhysicalLocation? {
        return regionOf(astNode.fileLocation)?.let {
            PhysicalLocation(Path.of(astNode.containingFilename).toUri(), it)
        }
    }

    private fun regionOf(fLocation: IASTFileLocation?): Region? {
        if (fLocation == null) return null
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
        val startLine = fLocation.startingLineNumber - 1

        // our end line, indexed by 0
        val endLine = fLocation.endingLineNumber - 1

        // our start column, index by 0
        val startColumn =
            if (startLine == 0) {
                // if we are in the first line, the start column is just the node offset
                fLocation.nodeOffset
            } else {
                // otherwise, we need to calculate the difference to the previous line break
                fLocation.nodeOffset -
                    lineBreaks[startLine - 1] -
                    1 // additional -1 because of the '\n' itself
            }

        // our end column, index by 0
        val endColumn =
            if (endLine == 0) {
                // if we are in the first line, the end column is just the node offset
                fLocation.nodeOffset + fLocation.nodeLength
            } else {
                // otherwise, we need to calculate the difference to the previous line break
                (fLocation.nodeOffset + fLocation.nodeLength) -
                    lineBreaks[endLine - 1] -
                    1 // additional -1 because of the '\n' itself
            }

        // for a SARIF compliant format, we need to add +1, since its index begins at 1 and
        // not 0
        return Region(startLine + 1, startColumn + 1, endLine + 1, endColumn + 1)
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
            node.annotations += handleAttributes(owner)
        }
    }

    private fun handleAttributes(owner: IASTAttributeOwner): List<Annotation> {
        val list: MutableList<Annotation> = ArrayList()
        for (attribute in owner.attributes) {
            val annotation = newAnnotation(String(attribute.name), rawNode = owner)

            // go over the parameters
            if (attribute.argumentClause is IASTTokenList) {
                val members = handleTokenList(attribute.argumentClause as IASTTokenList)
                annotation.members = members
            }
            list.add(annotation)
        }
        return list
    }

    private fun handleTokenList(tokenList: IASTTokenList): MutableList<AnnotationMember> {
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
                newReference(code, unknownType(), rawNode = token)
                2 -> // an integer
                newLiteral(code.toInt(), primitiveType("int"), rawNode = token)
                130 -> // a string
                newLiteral(
                        if (code.length >= 2) code.substring(1, code.length - 1) else "",
                        primitiveType("char").pointer(),
                        rawNode = token,
                    )
                else -> newLiteral(code, primitiveType("char").pointer(), rawNode = token)
            }
        return newAnnotationMember("", expression, rawNode = token)
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
        vararg parameterTypes: Class<*>,
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

    override fun setComment(node: Node, astNode: IASTNode) {
        // Nothing to do. We use the CommentMatcher instead.
    }

    /** Returns the [Type] that is represented by an [IASTTypeId]. */
    override fun typeOf(type: IASTTypeId): Type {
        return typeOf(type.abstractDeclarator, type.declSpecifier)
    }

    /**
     * Returns te [Type] that is represented by the [declarator] and [specifier]. This tries to
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
    @ResolveInFrontend("getRecordForName")
    fun typeOf(
        declarator: IASTDeclarator,
        specifier: IASTDeclSpecifier,
        hint: Declaration? = null,
    ): Type {
        var type = typeOf(specifier, hint)

        type = this.adjustType(declarator, type)

        return type
    }

    fun typeOf(specifier: IASTDeclSpecifier, hint: Declaration? = null): Type {
        var resolveTypeDef = false

        var type =
            when (specifier) {
                is IASTSimpleDeclSpecifier -> typeOf(specifier, hint)
                is IASTNamedTypeSpecifier -> {
                    // A reference to an object type. We need to differentiate between two cases:
                    // a) the type name is already an FQN. In this case, we can just parse it as
                    //    such.
                    // b) the type is a local name. In this case, we can peek whether the local name
                    //    refers to a symbol in our current namespace. This means that we are doing
                    //    some resolving in the frontend, which we actually want to avoid since it
                    //    has limited view.
                    if (
                        specifier.name is CPPASTQualifiedName || specifier.name is CPPASTTemplateId
                    ) {
                        // Case a: FQN or template
                        resolveTypeDef = true
                        typeOf(specifier.name)
                    } else {
                        resolveTypeDef = true

                        // It could be, that this is a parameterized type
                        val paramType =
                            typeManager.searchTemplateScopeForDefinedParameterizedTypes(
                                scopeManager.currentScope,
                                specifier.name.toString(),
                            )
                        if (paramType != null) {
                            paramType
                        } else {
                            // Otherwise, we keep it as a local name and the type normalizer will
                            // take care of it
                            typeOf(specifier.name)
                        }
                    }
                }
                is IASTCompositeTypeSpecifier -> {
                    // A class. This actually also declares the class. At the moment, we handle this
                    // in handleSimpleDeclaration, but we might want to move it here
                    resolveTypeDef = true

                    objectType(specifier.name.toString(), rawNode = specifier)
                }
                is IASTElaboratedTypeSpecifier -> {
                    resolveTypeDef = true

                    // A class or struct
                    objectType(specifier.name.toString(), rawNode = specifier)
                }
                else -> {
                    unknownType()
                }
            }

        type =
            if (resolveTypeDef) {
                typeManager.registerType(typeManager.resolvePossibleTypedef(type, scopeManager))
            } else {
                typeManager.registerType(type)
            }
        return type
    }

    private fun typeOf(specifier: IASTSimpleDeclSpecifier, hint: Declaration? = null): Type {
        val name = specifier.rawSignature

        return when {
            // auto type
            specifier.type == IASTSimpleDeclSpecifier.t_auto -> {
                autoType()
            }
            // void type
            specifier.type == IASTSimpleDeclSpecifier.t_void -> {
                incompleteType()
            }
            // __typeof__ type
            specifier.type == IASTSimpleDeclSpecifier.t_typeof -> {
                objectType(
                    "typeof(${specifier.declTypeExpression.rawSignature})",
                    rawNode = specifier,
                )
            }
            // A decl type
            specifier.type == IASTSimpleDeclSpecifier.t_decltype -> {
                objectType(
                    "decltype(${specifier.declTypeExpression.rawSignature})",
                    rawNode = specifier,
                )
            }
            // The type of constructor declaration is always the declaration itself
            specifier.type == IASTSimpleDeclSpecifier.t_unspecified &&
                hint is ConstructorDeclaration -> {
                hint.name.parent?.let { objectType(it, rawNode = specifier) } ?: unknownType()
            }
            // The type of conversion operator is also always the declaration itself
            specifier.type == IASTSimpleDeclSpecifier.t_unspecified &&
                hint is MethodDeclaration &&
                hint.name.localName == "operator#0" -> {
                hint.name.parent?.let { objectType(it, rawNode = specifier) } ?: unknownType()
            }
            // The type of conversion operator is also always the declaration itself
            specifier.type == IASTSimpleDeclSpecifier.t_unspecified &&
                hint is MethodDeclaration &&
                hint.name.localName == "operator#0*" -> {
                hint.name.parent?.let { objectType(it, rawNode = specifier).pointer() }
                    ?: unknownType()
            }
            // The type of destructor is unspecified, but we model it as a void type to make it
            // compatible with other methods.
            specifier.type == IASTSimpleDeclSpecifier.t_unspecified &&
                hint is MethodDeclaration &&
                hint.isDestructor -> {
                incompleteType()
            }
            // C (not C++) allows unspecified types in function declarations, they
            // default to int and usually produce a warning
            name == "" && language !is CPPLanguage -> {
                Util.warnWithFileLocation(
                    this,
                    specifier,
                    log,
                    "Type specifier missing, defaulting to 'int'",
                )
                primitiveType("int")
            }
            name == "" && language is CPPLanguage -> {
                Util.errorWithFileLocation(
                    this,
                    specifier,
                    log,
                    "C++ does not allow unspecified type specifiers",
                )
                newProblemType()
            }
            // In all other cases, this must be a primitive type, otherwise it's an error
            else -> {
                // We need to remove qualifiers such as "const" from the name here, because
                // we model them as part of the variable declaration and not the type, so use
                // the "canonical" name
                val canonicalName = specifier.canonicalName
                if (canonicalName == "") {
                    Util.errorWithFileLocation(
                        this,
                        specifier,
                        log,
                        "Could not determine canonical name for potential primitive type $name",
                    )
                    newProblemType()
                } else {
                    primitiveType(canonicalName)
                }
            }
        }
    }

    fun typeOf(name: IASTName, prefix: String? = null, doFqn: Boolean = false): Type {
        if (name is CPPASTQualifiedName) {
            val last = name.lastName
            if (last is CPPASTTemplateId) {
                return typeOf(last, name.qualifier.joinToString("::", postfix = "::"))
            }
        } else if (name is CPPASTTemplateId) {
            // Build fqn
            val fqn =
                if (prefix != null) {
                    prefix + name.templateName.toString()
                } else {
                    name.templateName.toString()
                }
            val generics = mutableListOf<Type>()

            // Loop through template arguments
            for (arg in name.templateArguments) {
                if (arg is CPPASTTypeId) {
                    generics += typeOf(arg)
                } else if (arg is CPPASTLiteralExpression) {
                    // This is most likely a constant in a template class definition, but we need to
                    // model this somehow, but it seems the old code just ignored this, so we do as
                    // well!
                }
            }

            return objectType(fqn, generics, rawNode = name)
        }

        var typeName =
            if (doFqn) {
                scopeManager.currentNamespace.fqn(name.toString())
            } else {
                parseName(name.toString())
            }

        return objectType(typeName, rawNode = name)
    }

    /**
     * This is a little helper function, primarily used by [typeOf]. It's primary purpose is to
     * "adjust" the [incoming] type based on the [declarator]. This is needed because the type
     * information in C/C++ are split into a declarator and declaration specifiers.
     */
    private fun adjustType(declarator: IASTDeclarator, incoming: Type): Type {
        var type = incoming

        // First, look at the declarator's pointer operator, to see whether we need to wrap the
        // type into a pointer or similar
        for (op in declarator.pointerOperators) {
            type =
                when {
                    op is IASTPointer -> type.pointer()
                    op is ICPPASTReferenceOperator && !op.isRValueReference -> ReferenceType(type)
                    // this is a little bit of a workaround until we re-design reference types, this
                    // is a && r-value reference used by move semantics in C++. This is actually
                    // just one level of reference (with a different operator), but for now we just
                    // make a double reference out of it to at least differentiate it from a &
                    // reference.
                    op is ICPPASTReferenceOperator && op.isRValueReference ->
                        ReferenceType(ReferenceType(type))
                    else -> type
                }
        }

        // Check, if we are an array type
        if (declarator is IASTArrayDeclarator) {
            for (mod in declarator.arrayModifiers) {
                type = type.array()
            }
        } else if (declarator is IASTStandardFunctionDeclarator) {
            // Loop through the parameters
            var paramTypes =
                declarator.parameters.map {
                    val specifier = it.declSpecifier
                    // If we are running into the situation where the declSpecifier is "unspecified"
                    // and the name is not, then this is an unnamed parameter of an unknown type and
                    // CDT is not able to handle this correctly
                    if (
                        specifier is CASTSimpleDeclSpecifier &&
                            specifier.type == IASTDeclSpecifier.sc_unspecified &&
                            it.declarator.name.toString() != ""
                    ) {
                        typeOf(it.declarator.name)
                    } else {
                        typeOf(it.declarator, it.declSpecifier)
                    }
                }

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
                    FunctionDeclaration.BRACKET_RIGHT,
                ) {
                    it.typeName
                } + type.typeName
            type = FunctionType(name, paramTypes, listOf(type), language)
        }

        // Lastly, there might be further nested declarators that adjust the type further.
        // However, if the type is already a function pointer type, we can ignore it. In the future,
        // this will probably actually make the difference between a function type and a function
        // pointer type.
        if (declarator.nestedDeclarator != null && type !is FunctionPointerType) {
            type = adjustType(declarator.nestedDeclarator, type)
        }

        type = typeManager.registerType(type)

        // Check for parameterized types
        if (type is SecondOrderType) {
            val templateType =
                typeManager.searchTemplateScopeForDefinedParameterizedTypes(
                    scopeManager.currentScope,
                    type.root.name.toString(),
                )
            if (templateType != null) {
                type.root = templateType
            }
        }

        // Make sure, the type manager knows about this type
        return type
    }

    companion object {
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
                    node.rawSignature.replace('\n', '\\').replace('\t', ' '),
                )
            }

            for (astNode in children) {
                explore(astNode, indent + 2)
            }
        }
    }
}

/**
 * Returns the type specified in the [IASTSimpleDeclSpecifier] in a "canonical" form and without any
 * other specifiers or keywords such as "typedef" or "const".
 */
private val IASTSimpleDeclSpecifier.canonicalName: CharSequence
    get() {
        var type = this.type
        var parts = mutableListOf<String>()
        // First, we specify whether it is signed or unsigned. We only need "signed" for chars
        if (this.isUnsigned) {
            parts += "unsigned"
        } else if (this.isSigned && this.type == IASTSimpleDeclSpecifier.t_char) {
            parts += "signed"
        }

        // Next, we analyze the size (long, long long, ...)
        if (this.isShort || this.isLong || this.isLongLong) {
            parts +=
                if (this.isShort) {
                    "short"
                } else if (this.isLong) {
                    "long"
                } else {
                    "long long"
                }

            // Also make this an int, if it is omitted
            if (type == IASTSimpleDeclSpecifier.t_unspecified) {
                type = IASTSimpleDeclSpecifier.t_int
            }
        }

        // Last part is the actual type (int, float, ...)
        when (type) {
            IASTSimpleDeclSpecifier.t_char -> parts += "char"
            IASTSimpleDeclSpecifier.t_wchar_t -> parts += "wchar_t"
            IASTSimpleDeclSpecifier.t_char16_t -> parts += "char16_t"
            IASTSimpleDeclSpecifier.t_char32_t -> parts += "char32_t"
            IASTSimpleDeclSpecifier.t_int -> parts += "int"
            IASTSimpleDeclSpecifier.t_float -> parts += "float"
            IASTSimpleDeclSpecifier.t_double -> parts += "double"
            IASTSimpleDeclSpecifier.t_bool -> parts += "bool"
            IASTSimpleDeclSpecifier.t_unspecified -> {
                if (isSigned || isUnsigned) {
                    parts += "int"
                }
            }
            IASTSimpleDeclSpecifier.t_auto -> parts = mutableListOf("auto")
            else -> {
                LanguageFrontend.Companion.log.error("Unknown C/C++ simple type: {}", type)
            }
        }

        return parts.joinToString(" ")
    }

/**
 * Returns whether this method is a
 * [Destructor](https://en.cppreference.com/w/cpp/language/destructor).
 */
val MethodDeclaration.isDestructor: Boolean
    get() {
        return "~" + this.name.parent?.localName == this.name.localName
    }
