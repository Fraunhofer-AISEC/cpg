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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.SupportsParallelParsing
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.frontends.golang.GoStandardLibrary.Modfile
import de.fraunhofer.aisec.cpg.frontends.golang.GoStandardLibrary.Parser
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.DeclarationSequence
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.newNamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.GoEvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.GoExtraPass
import de.fraunhofer.aisec.cpg.passes.configuration.RegisterExtraPass
import de.fraunhofer.aisec.cpg.passes.configuration.ReplacePass
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import java.net.URI

/**
 * A language frontend for the [GoLanguage]. It makes use the internal
 * [go/ast](https://pkg.go.dev/go/ast) package of the Go runtime to parse the AST of a Go program.
 * We make use of JNA to call a dynamic library which exports C function wrappers around the Go API.
 * This is needed because we cannot directly export Go structs and pointers to C.
 */
@RegisterExtraPass(GoExtraPass::class)
@ReplacePass(
    lang = GoLanguage::class,
    old = EvaluationOrderGraphPass::class,
    with = GoEvaluationOrderGraphPass::class,
)
@SupportsParallelParsing(false)
class GoLanguageFrontend(ctx: TranslationContext, language: Language<GoLanguageFrontend>) :
    LanguageFrontend<GoStandardLibrary.Ast.Node, GoStandardLibrary.Ast.Expr>(ctx, language) {

    private var currentFileSet: GoStandardLibrary.Ast.FileSet? = null
    private var currentModule: GoStandardLibrary.Modfile.File? = null
    private var commentMap: GoStandardLibrary.Ast.CommentMap? = null
    var currentFile: GoStandardLibrary.Ast.File? = null

    var isDependency: Boolean = false

    val declarationHandler = DeclarationHandler(this)
    val specificationHandler = SpecificationHandler(this)
    var statementHandler = StatementHandler(this)
    var expressionHandler = ExpressionHandler(this)

    /**
     * This helper class contains values needed to properly decide in which state const declaration
     * / specifications are in.
     */
    class DeclarationContext {
        /**
         * The current value of `iota`. This needs to be reset for each
         * [GoStandardLibrary.Ast.GenDecl] and incremented for each observed
         * [GoStandardLibrary.Ast.ValueSpec].
         */
        var iotaValue = -1

        /**
         * The current initializers in a list representing the different "columns". For example in
         * the following code:
         * ```go
         * const (
         *   a, b = 1, 2
         *   c, d
         *   e, f = 4, 5
         * )
         * ```
         *
         * The current list of initializers would first be (`1`,`2`) until a new set of initializers
         * is declared in the last spec. The key corresponds to the "column" of the variable
         * (a=0,b=1).
         */
        var constInitializers = mutableMapOf<Int, GoStandardLibrary.Ast.Expr>()

        /** The current const type, which is valid until a new initializer is present */
        var constType: Type? = null

        /** The current [GoStandardLibrary.Ast.GenDecl] that is being processed. */
        var currentDecl: GoStandardLibrary.Ast.GenDecl? = null
    }

    /**
     * The current [DeclarationContext]. This is somewhat of a workaround since we cannot properly
     * communicate state between different handlers. However, because *within* a [LanguageFrontend],
     * everything is parsed sequentially according to AST order, we can safely use this context
     * here.
     */
    var declCtx = DeclarationContext()

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        if (!shouldBeBuild(file, ctx.config.symbols)) {
            log.debug(
                "Ignoring the contents of {} because of missing build tags or different GOOS/GOARCH.",
                file,
            )
            return newTranslationUnitDeclaration(file.name)
        }

        val dependency =
            ctx.config.includePaths.firstOrNull {
                file.absolutePath.contains(it.toAbsolutePath().toString())
            }

        // Make sure, that our top level is set either way
        val topLevel =
            // If this file is part of an include, we set the top level to the root of the include
            when {
                dependency != null -> {
                    isDependency = true
                    dependency.toFile()
                }
                ctx.currentComponent?.topLevel != null -> ctx.currentComponent?.topLevel
                else -> file.parentFile
            }!!

        val std = GoStandardLibrary.INSTANCE

        // Try to parse a possible go.mod
        val goModFile = topLevel.resolve("go.mod")
        if (goModFile.exists()) {
            currentModule = Modfile.parse(goModFile.absolutePath, goModFile.readText())
        }

        val fset = std.NewFileSet()
        val f = Parser.parseFile(fset, file.absolutePath)

        this.commentMap = std.NewCommentMap(fset, f, f.comments)

        currentFile = f
        currentFileSet = fset

        val tu = newTranslationUnitDeclaration(file.absolutePath, rawNode = f)
        resetToGlobal(tu)
        currentTU = tu

        // We need to keep imports on a special file scope. We can simulate this by "entering" the
        // translation unit
        enterScope(tu)

        // We parse the imports specifically and not as part of the handler later
        for (spec in f.imports) {
            val import = specificationHandler.handle(spec)
            if (import is ImportDeclaration) {
                declareSymbol(import)
                tu.addDeclaration(import)
            }
        }

        val p = newNamespaceDeclaration(f.name.name)
        enterScope(p)

        try {
            // we need to construct the package "path" (e.g. "encoding/json") out of the
            // module path as well as the current directory in relation to the topLevel
            var packagePath = file.parentFile.relativeTo(topLevel)

            // If we are in a module, we need to prepend the module path to it. There is an
            // exception if we are in the "std" module, which represents the standard library
            val modulePath = currentModule?.module?.mod?.path
            if (modulePath != null && modulePath != "std") {
                packagePath = File(modulePath).resolve(packagePath)
            }

            p.path = packagePath.path
        } catch (ex: IllegalArgumentException) {
            log.error(
                "Could not relativize package path to top level. Cannot set package path.",
                ex,
            )
        }

        for (decl in f.decls) {
            // Retrieve all top level declarations. One "Decl" could potentially
            // contain multiple CPG declarations.
            val declaration = declarationHandler.handle(decl)
            if (declaration is DeclarationSequence) {
                declaration.declarations.forEach {
                    declareSymbol(it)
                    p.addDeclaration(it)
                }
            } else {
                // We need to be careful with method declarations. We need to put them in the
                // respective name scope of the record and NOT on the global scope / namespace scope
                // TODO: this is broken if we see the declaration of the method before the class :(
                if (declaration is MethodDeclaration) {
                    declaration.recordDeclaration?.let {
                        enterScope(it)
                        declareSymbol(declaration)
                        leaveScope(it)
                        // But still add it to the AST of the namespace so our AST walker can find
                        // it
                        p.declarations += declaration
                    }
                } else if (declaration != null) {
                    declareSymbol(declaration)
                    p.addDeclaration(declaration)
                }
            }
        }

        leaveScope(p)
        leaveScope(tu)

        resetToGlobal(tu)

        declareSymbol(p)
        tu.addDeclaration(p)

        return tu
    }

    override fun typeOf(type: GoStandardLibrary.Ast.Expr): Type {
        val cpgType =
            when (type) {
                is GoStandardLibrary.Ast.Ident -> {
                    val name: String =
                        if (isBuiltinType(type.name)) {
                            // Definitely not an FQN type
                            type.name
                        } else {
                            // FQN'ize this name (with the current file)
                            "${currentFile?.name?.name}.${type.name}" // this.File.Name.Name
                        }

                    objectType(name)
                }
                is GoStandardLibrary.Ast.SelectorExpr -> {
                    // This is a FQN type
                    val baseName = (type.x as? GoStandardLibrary.Ast.Ident)?.name?.let { Name(it) }

                    return objectType(Name(type.sel.name, baseName))
                }
                is GoStandardLibrary.Ast.ArrayType -> {
                    return typeOf(type.elt).array()
                }
                is GoStandardLibrary.Ast.ChanType -> {
                    // Handle them similar to a map type (see below)
                    return objectType("chan", listOf(typeOf(type.value)))
                }
                is GoStandardLibrary.Ast.FuncType -> {
                    val paramTypes =
                        type.params.list
                            .flatMap { field ->
                                // Because we can have unnamed parameters or multiple parameters
                                // declared at once, we need to expand the list of types according
                                // to the list of names
                                if (field.names.isEmpty()) {
                                    listOf(field.type)
                                } else {
                                    field.names.map { field.type }
                                }
                            }
                            .map { fieldTypeOf(it).first }
                    val returnTypes = type.results?.list?.map { typeOf(it.type) } ?: listOf()
                    val name = funcTypeName(paramTypes, returnTypes)

                    FunctionType(name, paramTypes, returnTypes, this.language)
                }
                is GoStandardLibrary.Ast.IndexExpr -> {
                    // A go type constraint, aka generic
                    val baseType = typeOf(type.x)
                    val generics = listOf(typeOf(type.index))
                    objectType(baseType.name, generics)
                }
                is GoStandardLibrary.Ast.IndexListExpr -> {
                    // A go type constraint, aka generic with multiple types
                    val baseType = typeOf(type.x)
                    val generics = type.indices.map { typeOf(it) }
                    objectType(baseType.name, generics)
                }
                is GoStandardLibrary.Ast.StructType -> {
                    // Go allows to use anonymous structs as type. This is something we cannot model
                    // properly in the CPG yet. In order to at least deal with this partially, we
                    // construct a ObjectType and put the fields and their types into the type.
                    // This will result in something like `struct{name string; args util.args; want
                    // string}`
                    val parts =
                        type.fields.list.map { field ->
                            var desc = ""
                            // Name can be optional, if its embedded
                            field.names.getOrNull(0)?.let { desc += it }
                            desc += " "
                            desc += fieldTypeOf(field.type).first.name
                            desc
                        }

                    val name = parts.joinToString("; ", "struct{", "}")

                    // Create an anonymous struct, this will add it to the scope manager. This is
                    // somewhat duplicate, but the easiest for now. We need to create it in the
                    // global scope to avoid namespace issues
                    var record =
                        withScope(globalScope) {
                            var record = specificationHandler.buildRecordDeclaration(type, name)
                            declareSymbol(record)
                            currentTU?.declarations += record
                            record
                        }

                    record.toType()
                }
                is GoStandardLibrary.Ast.InterfaceType -> {
                    // Go allows to use anonymous interface as type. This is something we cannot
                    // model
                    // properly in the CPG yet. In order to at least deal with this partially, we
                    // construct a ObjectType and put the methods and their types into the type.

                    // In the easiest case this is the empty interface `interface{}`, which we then
                    // consider to be the "any" type. `any` is actually a type alias for
                    // `interface{}`,
                    // but in modern Go `any` is preferred.
                    if (type.methods.list.isEmpty()) {
                        return primitiveType("any")
                    }

                    val parts =
                        type.methods.list.map { method ->
                            var desc = ""
                            // Name can be optional, if its embedded
                            method.names.getOrNull(0)?.let { desc += it }
                            // the function type has a weird "func" prefix, which we do not want
                            desc += typeOf(method.type).name.toString().removePrefix("func")
                            desc
                        }

                    objectType(parts.joinToString("; ", "interface{", "}"))
                }
                is GoStandardLibrary.Ast.MapType -> {
                    // We cannot properly represent Go's built-in map types, yet so we have
                    // to make a shortcut here and represent it as a Java-like map<K, V> type.
                    return objectType("map", listOf(typeOf(type.key), typeOf(type.value)))
                }
                is GoStandardLibrary.Ast.StarExpr -> {
                    typeOf(type.x).pointer()
                }
                else -> {
                    Util.warnWithFileLocation(
                        this,
                        type,
                        log,
                        "Not parsing type of type ${type.goType} yet",
                    )
                    unknownType()
                }
            }

        return typeManager.registerType(typeManager.resolvePossibleTypedef(cpgType, this))
    }

    /**
     * A quick helper function to retrieve the type of a field, to check for possible variadic
     * arguments.
     */
    internal fun fieldTypeOf(paramType: GoStandardLibrary.Ast.Expr): Pair<Type, Boolean> {
        var variadic = false
        val type =
            if (paramType is GoStandardLibrary.Ast.Ellipsis) {
                variadic = true
                typeOf(paramType.elt).array()
            } else {
                typeOf(paramType)
            }
        return Pair(type, variadic)
    }

    private fun isBuiltinType(name: String): Boolean {
        return language.primitiveTypeNames.contains(name)
    }

    override fun codeOf(astNode: GoStandardLibrary.Ast.Node): String? {
        return currentFileSet?.code(astNode)
    }

    override fun locationOf(astNode: GoStandardLibrary.Ast.Node): PhysicalLocation? {
        val start = currentFileSet?.position(astNode.pos) ?: return null
        val end = currentFileSet?.position(astNode.end) ?: return null
        val url = currentFileSet?.fileName(astNode.pos)?.let { URI(it) } ?: return null

        return PhysicalLocation(url, Region(start.line, start.column, end.line, end.column))
    }

    override fun setComment(node: Node, astNode: GoStandardLibrary.Ast.Node) {
        // Since we are potentially calling this function more than once on a node because of the
        // way go is structured (one decl can contain multiple specs), we need to make sure, that we
        // are not "overriding" more specific comments with more global ones.
        if (node.comment == null) {
            val comment = this.commentMap?.comment(astNode)
            node.comment = comment
        }
    }

    companion object {
        /**
         * All possible goos values. See
         * https://github.com/golang/go/blob/release-branch.go1.21/src/go/build/syslist.go#L11
         */
        val goosValues =
            listOf(
                "aix",
                "android",
                "darwin",
                "dragonfly",
                "freebsd",
                "hurd",
                "illumos",
                "ios",
                "js",
                "linux",
                "nacl",
                "netbsd",
                "openbsd",
                "plan9",
                "solaris",
                "wasip1",
                "windows",
                "zos",
            )

        /**
         * All possible architecture values. See
         * https://github.com/golang/go/blob/release-branch.go1.21/src/go/build/syslist.go#L54
         */
        val goarchValues =
            listOf(
                "386",
                "amd64",
                "arm",
                "arm64",
                "loong64",
                "mips",
                "mips64",
                "mips64le",
                "mipsle",
                "ppc64",
                "ppc64le",
                "riscv64",
                "s390x",
            )
    }
}

/**
 * Go has the concept of the [underlying type](https://go.dev/ref/spec#Underlying_types), in which
 * new named types can be created on top of existing core types (such as function types, slices,
 * etc.). The named types then derive certain properties of their underlying type.
 *
 * For type literals, e.g., a directly specified function type, the underlying type is the type
 * itself.
 */
val Type?.underlyingType: Type?
    get() {
        return if (namedType) {
            this?.superTypes?.singleOrNull()
        } else {
            this
        }
    }

/**
 * In Go, types can be constructed based on existing types (see [underlyingType]) and if given a
 * name, they are considered a [named type](https://go.dev/ref/spec#Types).
 *
 * Since these named types can also be augmented with methods (see
 * https://go.dev/ref/spec#Method_sets), we need to model them as an [ObjectType] with an associated
 * [RecordDeclaration] (of kind "type").
 */
val Type?.namedType: Boolean
    get() {
        return this is ObjectType && this.recordDeclaration?.kind == "type"
    }

val Type.isInterface: Boolean
    get() {
        return underlyingType is ObjectType && this.recordDeclaration?.kind == "interface"
    }

val Type.isMap: Boolean
    get() {
        return underlyingType is ObjectType && this.name.localName == "map"
    }

val Type.isChannel: Boolean
    get() {
        return underlyingType is ObjectType && this.name.localName == "chan"
    }

val HasType?.isNil: Boolean
    get() {
        return this is Literal<*> && this.name.localName == "nil"
    }

/**
 * This function produces a Go-style function type name such as `func(int, string) string` or
 * `func(int) (error, string)`
 */
fun funcTypeName(paramTypes: List<Type>, returnTypes: List<Type>): String {
    val rn = mutableListOf<String>()
    val pn = mutableListOf<String>()

    for (t in paramTypes) {
        pn += t.name.toString()
    }

    for (t in returnTypes) {
        rn += t.name.toString()
    }

    val rs =
        if (returnTypes.size > 1) {
            rn.joinToString(", ", prefix = " (", postfix = ")")
        } else if (returnTypes.isNotEmpty()) {
            rn.joinToString(", ", prefix = " ")
        } else {
            ""
        }

    return pn.joinToString(", ", prefix = "func(", postfix = ")$rs")
}

val RecordDeclaration.embeddedStructs: List<RecordDeclaration>
    get() {
        return this.fields
            .filter { "embedded" in it.modifiers }
            .mapNotNull { it.type.root.recordDeclaration }
    }
