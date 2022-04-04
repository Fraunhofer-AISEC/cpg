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

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HandlerInterface
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newRecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.types.IncompleteType
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.scopes.RecordScope
import de.fraunhofer.aisec.cpg.passes.scopes.TemplateScope
import java.util.*
import java.util.function.Supplier
import java.util.regex.Pattern
import java.util.stream.Collectors
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier
import org.eclipse.cdt.core.dom.ast.IASTDeclarator
import org.eclipse.cdt.core.dom.ast.IASTNameOwner
import org.eclipse.cdt.core.dom.ast.IASTNode
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier
import org.eclipse.cdt.internal.core.dom.parser.cpp.*

class DeclaratorHandler(lang: CXXLanguageFrontend) :
    Handler<Declaration?, IASTNameOwner, CXXLanguageFrontend?>(Supplier { Declaration() }, lang) {

    init {
        map[CPPASTDeclarator::class.java] = HandlerInterface {
            handleDeclarator(it as CPPASTDeclarator)
        }
        map[CPPASTArrayDeclarator::class.java] = HandlerInterface {
            handleDeclarator(it as CPPASTDeclarator)
        }
        map[CPPASTFieldDeclarator::class.java] = HandlerInterface {
            handleFieldDeclarator(it as CPPASTDeclarator)
        }
        map[CPPASTFunctionDeclarator::class.java] = HandlerInterface {
            handleFunctionDeclarator(it as CPPASTFunctionDeclarator)
        }
        map[CPPASTCompositeTypeSpecifier::class.java] = HandlerInterface {
            handleCompositeTypeSpecifier(it as CPPASTCompositeTypeSpecifier)
        }
        map[CPPASTSimpleTypeTemplateParameter::class.java] = HandlerInterface {
            handleTemplateTypeParameter(it as CPPASTSimpleTypeTemplateParameter)
        }
    }

    private fun handleDeclarator(ctx: CPPASTDeclarator): Declaration? {
        // this is just a nested declarator, i.e. () wrapping the real declarator
        if (ctx.initializer == null && ctx.nestedDeclarator is CPPASTDeclarator) {
            return handle(ctx.nestedDeclarator)
        }
        val name = ctx.name.toString()

        return if (lang.scopeManager.currentScope is RecordScope ||
                name.contains(lang.namespaceDelimiter)
        ) {
            // forward it to handleFieldDeclarator
            this.handleFieldDeclarator(ctx)
        } else {
            // type will be filled out later
            val declaration =
                NodeBuilder.newVariableDeclaration(
                    ctx.name.toString(),
                    UnknownType.getUnknownType(),
                    ctx.rawSignature,
                    true
                )
            val init = ctx.initializer
            if (init != null) {
                declaration.initializer = lang.initializerHandler.handle(init)
            }
            lang.scopeManager.addDeclaration(declaration)
            declaration
        }
    }

    private fun handleFieldDeclarator(ctx: CPPASTDeclarator): FieldDeclaration {
        val initializer = ctx.initializer?.let { lang.initializerHandler.handle(it) }

        val name = ctx.name.toString()

        val declaration =
            if (name.contains(lang.namespaceDelimiter)) {
                val rr = name.split(lang.namespaceDelimiter).toTypedArray()
                val fieldName = rr[rr.size - 1]
                NodeBuilder.newFieldDeclaration(
                    fieldName,
                    UnknownType.getUnknownType(),
                    emptyList(),
                    ctx.rawSignature,
                    lang.getLocationFromRawNode(ctx),
                    initializer,
                    true
                )
            } else {
                NodeBuilder.newFieldDeclaration(
                    name,
                    UnknownType.getUnknownType(),
                    emptyList(),
                    ctx.rawSignature,
                    lang.getLocationFromRawNode(ctx),
                    initializer,
                    true
                )
            }

        lang.scopeManager.addDeclaration(declaration)

        return declaration
    }

    private fun createMethodOrConstructor(
        name: String,
        recordDeclaration: RecordDeclaration?,
        lang: LanguageFrontend,
        ctx: IASTNode,
    ): MethodDeclaration {
        // check, if its a constructor
        return if (name == recordDeclaration?.name) {
            NodeBuilder.newConstructorDeclaration(name, null, recordDeclaration, lang, ctx)
        } else NodeBuilder.newMethodDeclaration(name, null, false, recordDeclaration, lang, ctx)
    }

    private fun handleFunctionDeclarator(ctx: CPPASTFunctionDeclarator): ValueDeclaration {
        // Programmers can wrap the function name in as many levels of parentheses as they like. CDT
        // treats these levels as separate declarators, so we need to get to the bottom for the
        // actual name...
        var nameDecl: IASTDeclarator = ctx
        var hasPointer = false
        while (nameDecl.nestedDeclarator != null) {
            nameDecl = nameDecl.nestedDeclarator
            if (nameDecl.pointerOperators.isNotEmpty()) {
                hasPointer = true
            }
        }
        var name = nameDecl.name.toString()

        // Attention! This might actually be a function pointer (requires at least one level of
        // parentheses and a pointer operator)
        if (nameDecl !== ctx && hasPointer) {
            return handleFunctionPointer(ctx, name)
        }

        /*
         * As always, there are some special cases to consider and one of those are C++ operators.
         * They are regarded as functions and eclipse CDT for some reason introduces a whitespace in the function name, which will complicate things later on
         */ if (name.startsWith("operator")) {
            name = name.replace(" ", "")
        }
        val declaration: FunctionDeclaration

        // If this is a method, this is its record declaration
        var recordDeclaration: RecordDeclaration? = null

        // remember, if this is a method declaration outside of the record
        val outsideOfRecord =
            !(lang.scopeManager.currentRecord != null ||
                lang.scopeManager.currentScope is TemplateScope)

        // check for function definitions that are really methods and constructors, i.e. if they
        // contain a scope operator
        if (name.contains(lang.namespaceDelimiter)) {
            val rr = name.split(lang.namespaceDelimiter).toTypedArray()
            val recordName =
                java.lang.String.join(lang.namespaceDelimiter, listOf(*rr).subList(0, rr.size - 1))
            val methodName = rr[rr.size - 1]
            recordDeclaration =
                lang.scopeManager.getRecordForName(lang.scopeManager.currentScope!!, recordName)
            declaration = createMethodOrConstructor(methodName, recordDeclaration, lang, ctx.parent)
        } else if (lang.scopeManager.isInRecord) {
            // if it is inside a record scope, it is a method
            recordDeclaration = lang.scopeManager.currentRecord
            declaration = createMethodOrConstructor(name, recordDeclaration, lang, ctx.parent)
        } else {
            // a plain old function, outside any record scope
            declaration =
                NodeBuilder.newFunctionDeclaration(name, ctx.rawSignature, lang, ctx.parent)
        }
        lang.scopeManager.addDeclaration(declaration)

        // if we know our record declaration, but are outside the actual record, we
        // need to temporary enter the record scope
        if (recordDeclaration != null && outsideOfRecord) {
            // to make sure, that the scope of this function is associated to the record
            lang.scopeManager.enterScope(recordDeclaration)
        }
        lang.scopeManager.enterScope(declaration)
        var i = 0
        for (param in ctx.parameters) {
            val arg = lang.parameterDeclarationHandler.handle(param)

            // check for void type parameters
            if (arg!!.type is IncompleteType) {
                if (arg.name.isNotEmpty()) {
                    Util.warnWithFileLocation(
                        declaration,
                        log,
                        "Named parameter cannot have void type"
                    )
                } else {
                    // specifying void as first parameter is ok and means that the function has no
                    // parameters
                    if (i == 0) {
                        continue
                    } else {
                        Util.warnWithFileLocation(
                            declaration,
                            log,
                            "void parameter must be the first and only parameter"
                        )
                    }
                }
            }

            arg.argumentIndex = i
            // Note that this .addValueDeclaration call already adds arg to the function's
            // parameters.
            // This is why the following line has been commented out by @KW
            lang.scopeManager.addDeclaration(arg)
            // declaration.getParameters().add(arg);
            i++
        }

        // Check for varargs. Note the difference to Java: here, we don't have a named array
        // containing the varargs, but they are rather treated as kind of an invisible arg list that
        // is
        // appended to the original ones. For coherent graph behaviour, we introduce an implicit
        // declaration that
        // wraps this list
        if (ctx.takesVarArgs()) {
            val varargs =
                NodeBuilder.newMethodParameterIn("va_args", UnknownType.getUnknownType(), true, "")
            varargs.isImplicit = true
            varargs.argumentIndex = i
            lang.scopeManager.addDeclaration(varargs)
        }
        lang.scopeManager.leaveScope(declaration)

        // if we know our record declaration, but are outside the actual record, we
        // need to leave the record scope again afterwards
        if (recordDeclaration != null && outsideOfRecord) {
            lang.scopeManager.leaveScope(recordDeclaration)
        }

        // We recognize a ambiguity here, but cannot solve it at the moment
        if (name != "" &&
                ctx.parent is CPPASTDeclarator &&
                declaration.body == null &&
                lang.scopeManager.currentFunction != null
        ) {
            val problem =
                NodeBuilder.newProblemDeclaration(
                    null,
                    "CDT tells us this is a (named) function declaration in parenthesis without a body directly within a block scope, this might be an ambiguity which we cannot solve currently.",
                    null
                )

            Util.warnWithFileLocation(lang, ctx, log, problem.problem)

            return problem
        }

        return declaration
    }

    private fun handleFunctionPointer(
        ctx: CPPASTFunctionDeclarator,
        name: String
    ): ValueDeclaration {
        val initializer =
            if (ctx.initializer == null) null else lang.initializerHandler.handle(ctx.initializer)
        // unfortunately we are not told whether this is a field or not, so we have to find it out
        // ourselves
        val result: ValueDeclaration
        val recordDeclaration = lang.scopeManager.currentRecord
        if (recordDeclaration == null) {
            // variable
            result =
                NodeBuilder.newVariableDeclaration(
                    name,
                    UnknownType.getUnknownType(),
                    ctx.rawSignature,
                    true
                )
            result.initializer = initializer
        } else {
            // field
            val code = ctx.rawSignature
            val namePattern = Pattern.compile("\\((\\*|.+\\*)(?<name>[^)]*)")
            val matcher = namePattern.matcher(code)
            var fieldName: String? = ""
            if (matcher.find()) {
                fieldName = matcher.group("name").trim()
            }
            result =
                NodeBuilder.newFieldDeclaration(
                    fieldName,
                    UnknownType.getUnknownType(),
                    emptyList(),
                    code,
                    lang.getLocationFromRawNode(ctx),
                    initializer,
                    true
                )
        }

        /*
         * Now it gets tricky, because we are looking for the parent declaration to get the full
         * raw signature. However it could be that the declarator is wrapped in nested declarators,
         * so we need to loop.
         *
         * Comment from @oxisto: I think it would still be better to parse the type in the handleSimpleDeclaration
         * and going downwards into the decl-specifiers and declarator and see whether we can re-construct them in
         * the correct order for the function type rather than going upwards from the declarator and use the raw string,
         * but that is the way it is for now.
         */
        var parent = ctx.parent
        while (parent != null && parent !is CPPASTSimpleDeclaration) {
            parent = parent.parent
        }
        if (parent != null) {
            result.type = TypeParser.createFrom(parent.rawSignature, true, lang)
            result.refreshType()
        } else {
            log.warn("Could not find suitable parent ast node for function pointer node: {}", this)
        }
        result.location = lang.getLocationFromRawNode(ctx)
        lang.scopeManager.addDeclaration(result)
        return result
    }

    private fun handleCompositeTypeSpecifier(ctx: CPPASTCompositeTypeSpecifier): RecordDeclaration {
        val kind: String =
            when (ctx.key) {
                IASTCompositeTypeSpecifier.k_struct -> "struct"
                IASTCompositeTypeSpecifier.k_union -> "union"
                ICPPASTCompositeTypeSpecifier.k_class -> "class"
                else -> "struct"
            }

        val recordDeclaration =
            newRecordDeclaration(
                lang.scopeManager.currentNamePrefixWithDelimiter + ctx.name.toString(),
                kind,
                ctx.rawSignature,
                true,
                lang
            )
        recordDeclaration.superClasses =
            Arrays.stream(ctx.baseSpecifiers)
                .map { b: ICPPASTBaseSpecifier ->
                    TypeParser.createFrom(b.nameSpecifier.toString(), true, lang)
                }
                .collect(Collectors.toList())

        lang.scopeManager.addDeclaration(recordDeclaration)

        lang.scopeManager.enterScope(recordDeclaration)

        lang.scopeManager.addDeclaration(recordDeclaration.getThis())

        processMembers(ctx)
        if (recordDeclaration.constructors.isEmpty()) {
            val constructorDeclaration =
                NodeBuilder.newConstructorDeclaration(
                    recordDeclaration.name,
                    recordDeclaration.name,
                    recordDeclaration
                )

            // set this as implicit
            constructorDeclaration.isImplicit = true

            // and set the type, constructors always have implicitly the return type of their class
            constructorDeclaration.type = TypeParser.createFrom(recordDeclaration.name, true, lang)
            recordDeclaration.addConstructor(constructorDeclaration)
            lang.scopeManager.addDeclaration(constructorDeclaration)
        }

        lang.scopeManager.leaveScope(recordDeclaration)

        return recordDeclaration
    }

    /**
     * Handles template parameters that are types
     *
     * @param ctx
     * @return TypeParamDeclaration with its name
     */
    private fun handleTemplateTypeParameter(
        ctx: CPPASTSimpleTypeTemplateParameter
    ): TypeParamDeclaration {
        return NodeBuilder.newTypeParamDeclaration(ctx.rawSignature, ctx.rawSignature)
    }

    private fun processMembers(ctx: CPPASTCompositeTypeSpecifier) {
        for (member in ctx.members) {
            if (member is CPPASTVisibilityLabel) {
                // TODO: parse visibility
                continue
            }

            lang.declarationHandler.handle(member)
        }
    }
}
