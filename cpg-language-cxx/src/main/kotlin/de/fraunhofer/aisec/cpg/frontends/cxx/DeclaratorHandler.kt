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
import de.fraunhofer.aisec.cpg.frontends.isKnownOperatorName
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.helpers.Util
import java.util.function.Supplier
import org.eclipse.cdt.core.dom.ast.*
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage
import org.eclipse.cdt.internal.core.dom.parser.cpp.*

/**
 * Takes care of translating a
 * [declarator](https://en.cppreference.com/w/cpp/language/declarations#Declarators) into a
 * [Declaration].
 *
 * See [DeclarationHandler] for a detailed explanation, why this is split into a dedicated handler.
 */
class DeclaratorHandler(lang: CXXLanguageFrontend) :
    CXXHandler<Declaration, IASTNode>(Supplier(::ProblemDeclaration), lang) {

    override fun handleNode(node: IASTNode): Declaration {
        return when (node) {
            is CPPASTFunctionDeclarator -> handleCPPFunctionDeclarator(node)
            is IASTStandardFunctionDeclarator -> handleFunctionDeclarator(node)
            is IASTFieldDeclarator -> handleFieldDeclarator(node)
            is IASTDeclarator -> handleDeclarator(node)
            is IASTCompositeTypeSpecifier -> handleCompositeTypeSpecifier(node)
            is CPPASTSimpleTypeTemplateParameter -> handleTemplateTypeParameter(node)
            else -> {
                return handleNotSupported(node, node.javaClass.name)
            }
        }
    }

    /**
     * The [CPPASTFunctionDeclarator] extends the [IASTStandardFunctionDeclarator] and has some more
     * attributes which we want to consider. Currently, this is the
     * [CPPASTFunctionDeclarator.trailingReturnType] which will be added to the FunctionDeclaration.
     * This represents the return-type of a lambda function.
     */
    private fun handleCPPFunctionDeclarator(node: CPPASTFunctionDeclarator): Declaration {
        // Handle it as a regular C function first
        val function = handleFunctionDeclarator(node)

        // If we have a trailing return type, we specify the return type of the (lambda) function
        if (function is FunctionDeclaration && node.trailingReturnType != null) {
            function.returnTypes = listOf(frontend.typeOf(node.trailingReturnType))
        }

        return function
    }

    /**
     * This is sort of a catch-all function, if none of the previous specialized declarators match.
     * It can be one of three things:
     * - a wrapper around a nested declarator, in which case we delegate the handling to the nested
     *   one,
     * - a field declaration, if this declaration occurs within a class or has a qualified name, or
     * - a variable declaration in all the other cases.
     */
    private fun handleDeclarator(ctx: IASTDeclarator): Declaration {
        // This is just a nested declarator, i.e. () wrapping the real declarator
        if (ctx.initializer == null && ctx.nestedDeclarator is IASTDeclarator) {
            return handle(ctx.nestedDeclarator)
                ?: ProblemDeclaration("could not parse nested declaration")
        }

        val name = ctx.name.toString()

        // Check, if the name is qualified or if we are within a record scope
        return if (
            frontend.scopeManager.currentScope is RecordScope ||
                language.namespaceDelimiter.let { name.contains(it) } == true
        ) {
            // If yes, treat this like a field declaration
            this.handleFieldDeclarator(ctx)
        } else {
            // If not, this is a normal variable declaration. First, we need to check if this
            // declaration allows to have an implicit constructor initializer. Only C++ has
            // constructors and thus implicit (constructor) initialization calls
            val implicitInitializerAllowed = frontend.dialect is GPPLanguage

            val declaration =
                newVariableDeclaration(
                    ctx.name.toString(),
                    unknownType(), // Type will be filled out later by
                    // handleSimpleDeclaration
                    implicitInitializerAllowed = implicitInitializerAllowed,
                    rawNode = ctx,
                )

            declaration
        }
    }

    /**
     * Translates (data members)[https://en.cppreference.com/w/cpp/language/data_members] of a C++
     * class or C/C++ struct into a [FieldDeclaration].
     */
    private fun handleFieldDeclarator(ctx: IASTDeclarator): FieldDeclaration {
        val initializer = ctx.initializer?.let { frontend.initializerHandler.handle(it) }

        val name = parseName(ctx.name.toString())

        val declaration =
            newFieldDeclaration(
                name.localName,
                unknownType(),
                emptyList(),
                initializer = initializer,
                implicitInitializerAllowed = true,
                rawNode = ctx,
            )

        return declaration
    }

    /**
     * A small utility function that creates a [ConstructorDeclaration], [MethodDeclaration] or
     * [FunctionDeclaration] depending on which scope the function should live in. This basically
     * checks if the scope is a namespace or a record and if the name matches to the record (in case
     * of a constructor).
     */
    private fun createAppropriateFunction(
        name: Name,
        scope: Scope?,
        ctx: IASTNode,
    ): FunctionDeclaration {
        // Retrieve the AST node for the scope we need to put the function in
        val holder = scope?.astNode

        val func =
            when {
                // Check if it's an operator
                name.isKnownOperatorName -> {
                    // retrieve the operator code
                    val operatorCode = name.localName.drop("operator".length)
                    newOperatorDeclaration(name, operatorCode, rawNode = ctx)
                }
                // Check, if it's a constructor. This is the case if the local names of the function
                // and the record declaration match
                holder is RecordDeclaration && name.localName == holder.name.localName -> {
                    newConstructorDeclaration(name, holder, rawNode = ctx)
                }
                // It's also a constructor, if the name is in the form A::A, and it has no type
                // specifier
                name.localName == name.parent.toString() &&
                    ((ctx as? IASTFunctionDefinition)?.declSpecifier as? IASTSimpleDeclSpecifier)
                        ?.type == IASTSimpleDeclSpecifier.t_unspecified -> {
                    newConstructorDeclaration(name, null, rawNode = ctx)
                }
                // It could also be a scoped function declaration.
                scope?.astNode is NamespaceDeclaration -> {
                    newFunctionDeclaration(name, rawNode = ctx)
                }
                // Otherwise, it's a method to a known or unknown record
                else -> {
                    newMethodDeclaration(name, false, holder as? RecordDeclaration, rawNode = ctx)
                }
            }

        // Also make sure to correctly set the scope of the function, regardless where we are in the
        // AST currently
        func.scope = scope

        return func
    }

    @ResolveInFrontend("lookupScope")
    private fun handleFunctionDeclarator(ctx: IASTStandardFunctionDeclarator): ValueDeclaration {
        // Programmers can wrap the function name in as many levels of parentheses as they like. CDT
        // treats these levels as separate declarators, so we need to get to the bottom for the
        // actual name using the realName extension function.
        val (nameDecl: IASTDeclarator, hasPointer) = ctx.realName()
        var name =
            if (nameDecl.name == null) {
                Name("")
            } else {
                parseName(nameDecl.name.toString())
            }

        // Attention! This might actually be a function pointer (requires at least one level of
        // parentheses and a pointer operator)
        if (nameDecl !== ctx && hasPointer) {
            return handleFunctionPointer(ctx, name.toString())
        }

        /*
         * As always, there are some special cases to consider and one of those are C++ operators.
         * They are regarded as functions and eclipse CDT for some reason introduces a whitespace
         * in the function name, which will complicate things later on. But we only want to replace
         * the whitespace for "standard" operators.
         */
        if (nameDecl.name is CPPASTOperatorName && name.replace(" ", "").isKnownOperatorName) {
            name = name.replace(" ", "")
        }
        val declaration: FunctionDeclaration

        // We need to check if this function is actually part of a named declaration, such as a
        // record or a namespace, but defined externally.
        var parentScope: NameScope? = null

        // Check for function definitions that really belong to a named scoped, i.e. if they
        // contain a scope operator. This could either be a namespace or a record.
        val parent = name.parent
        if (parent != null) {
            // In this case, the name contains a qualifier, and we can try to check, if we have a
            // matching name scope for the parent name. We also need to take the current namespace
            // into account
            parentScope =
                frontend.scopeManager.lookupScope(
                    parseName(
                        frontend.scopeManager.currentNamespace
                            .fqn(parent.toString(), delimiter = parent.delimiter)
                            .toString()
                    )
                )

            declaration = createAppropriateFunction(name, parentScope, ctx.parent)
        } else if (frontend.scopeManager.isInRecord) {
            // If the current scope is already a record, it's a method
            declaration =
                createAppropriateFunction(name, frontend.scopeManager.currentScope, ctx.parent)
        } else {
            // a plain old function, outside any named scope
            declaration = newFunctionDeclaration(name, rawNode = ctx.parent)
        }

        // We want to determine, whether we are currently outside a named scope on the AST
        val outsideOfScope = frontend.scopeManager.currentScope != declaration.scope

        // If we know our parent scope, but are outside the actual scope on the AST, we
        // need to temporarily enter the scope. This way, we can do a little trick
        // and (manually) add the declaration to the AST element of the current scope
        // (probably the global scope), but associate it to the named scope.
        if (parentScope != null && outsideOfScope) {
            // Bypass the scope manager and manually add it to the AST parent
            val scopeParent = frontend.scopeManager.currentScope?.astNode
            if (scopeParent != null && scopeParent is DeclarationHolder) {
                scopeParent.addDeclaration(declaration)
            }

            // Enter the name scope
            parentScope.astNode?.let {
                frontend.scopeManager.enterScope(it)
                frontend.scopeManager.addDeclaration(declaration)
            }

            // We also need to by-pass the scope manager for this, because it will
            // otherwise add the declaration to the AST element of the named scope (the record
            // or namespace declaration); in the case of a record declaration to the `methods`
            // fields. However, since `methods` is an  AST field, (for now) we only want those
            // methods in there, that were actual AST
            // parents. This is also something that we need to figure out how we want to handle
            // this.
            parentScope.addSymbol(declaration.symbol, declaration)
        }

        // Enter the scope of the function itself
        frontend.scopeManager.enterScope(declaration)

        // Create the method receiver (if this is a method)
        if (declaration is MethodDeclaration) {
            createMethodReceiver(declaration)
        }

        var i = 0
        for (param in ctx.parameters) {
            val arg = frontend.parameterDeclarationHandler.handle(param)

            if (arg is ParameterDeclaration) {
                // check for void type parameters
                if (arg.type is IncompleteType) {
                    if (arg.name.isNotEmpty()) {
                        Util.warnWithFileLocation(
                            declaration,
                            log,
                            "Named parameter cannot have void type",
                        )
                    } else {
                        // specifying void as first parameter is ok and means that the function has
                        // no parameters
                        if (i == 0) {
                            continue
                        } else {
                            Util.warnWithFileLocation(
                                declaration,
                                log,
                                "void parameter must be the first and only parameter",
                            )
                        }
                    }
                }

                arg.argumentIndex = i
            }

            if (arg is ParameterDeclaration) {
                frontend.scopeManager.addDeclaration(arg)
                declaration.parameters += arg
            }
            i++
        }

        // Check for varargs. Note the difference to Java: here, we don't have a named array
        // containing the varargs, but they are rather treated as kind of an invisible arg list that
        // is appended to the original ones. For coherent graph behaviour, we introduce an implicit
        // declaration that wraps this list
        if (ctx.takesVarArgs()) {
            val varargs = newParameterDeclaration("va_args", unknownType(), true)
            varargs.isImplicit = true
            varargs.argumentIndex = i
            frontend.scopeManager.addDeclaration(varargs)
            declaration.parameters += varargs
        }
        frontend.scopeManager.leaveScope(declaration)

        // if we know our record declaration, but are outside the actual record, we
        // need to leave the record scope again afterwards
        if (parentScope != null && outsideOfScope) {
            parentScope.astNode?.let { frontend.scopeManager.leaveScope(it) }
        }

        // We recognize an ambiguity here, but cannot solve it at the moment
        if (
            name.isNotEmpty() &&
                ctx.parent is CPPASTDeclarator &&
                declaration.body == null &&
                frontend.scopeManager.currentFunction != null
        ) {
            val problem =
                newProblemDeclaration(
                    "CDT tells us this is a (named) function declaration in parenthesis without a body directly within a block scope, this might be an ambiguity which we cannot solve currently."
                )

            Util.warnWithFileLocation(frontend, ctx, log, problem.problem)

            return problem
        }

        return declaration
    }

    /**
     * This function takes cares of creating a receiver and setting it to the supplied
     * [MethodDeclaration]. In C++ this is called the
     * [implicit object parameter](https://en.cppreference.com/w/cpp/language/overload_resolution#Implicit_object_parameter)
     * .
     */
    private fun createMethodReceiver(declaration: MethodDeclaration) {
        val recordDeclaration = declaration.recordDeclaration

        // Create a pointer to the class type (if we know it)
        val type = recordDeclaration?.toType()?.pointer() ?: unknownType()

        // Create the receiver. implicitInitializerAllowed must be false, otherwise fixInitializers
        // will create another implicit constructexpression for this variable, and we don't want
        // this.
        val thisDeclaration =
            newVariableDeclaration("this", type = type, implicitInitializerAllowed = false)
        // Yes, this is implicit
        thisDeclaration.isImplicit = true

        // Add it to the scope of the method
        frontend.scopeManager.addDeclaration(thisDeclaration)

        // We need to manually set the receiver, since the scope manager cannot figure this out
        declaration.receiver = thisDeclaration
    }

    private fun handleFunctionPointer(ctx: IASTFunctionDeclarator, name: String): ValueDeclaration {
        // unfortunately we are not told whether this is a field or not, so we have to find it out
        // ourselves
        val result: ValueDeclaration
        val recordDeclaration = frontend.scopeManager.currentRecord
        if (recordDeclaration == null) {
            // variable
            result =
                newVariableDeclaration(
                    name,
                    unknownType(),
                    implicitInitializerAllowed = true,
                    rawNode = ctx,
                )
        } else {
            // field
            result =
                newFieldDeclaration(
                    name,
                    unknownType(),
                    emptyList(),
                    initializer = null,
                    implicitInitializerAllowed = false,
                    rawNode = ctx,
                )
        }

        result.location = frontend.locationOf(ctx)

        return result
    }

    private fun handleCompositeTypeSpecifier(ctx: IASTCompositeTypeSpecifier): RecordDeclaration {
        val kind: String =
            when (ctx.key) {
                IASTCompositeTypeSpecifier.k_struct -> "struct"
                IASTCompositeTypeSpecifier.k_union -> "union"
                ICPPASTCompositeTypeSpecifier.k_class -> "class"
                else -> "struct"
            }

        val recordDeclaration = newRecordDeclaration(ctx.name.toString(), kind, rawNode = ctx)

        // Handle C++ classes
        if (ctx is CPPASTCompositeTypeSpecifier) {
            recordDeclaration.superClasses =
                ctx.baseSpecifiers
                    .map { objectType(it.nameSpecifier.toString(), rawNode = it) }
                    .toMutableList()
        }

        frontend.scopeManager.enterScope(recordDeclaration)

        processMembers(recordDeclaration, ctx)

        if (recordDeclaration.innerConstructors.isEmpty()) {
            // create an implicit constructor declaration with the same name as the record
            val constructorDeclaration =
                newConstructorDeclaration(recordDeclaration.name.localName, recordDeclaration)
                    .implicit(code = recordDeclaration.name.localName)

            createMethodReceiver(constructorDeclaration)

            // and set the type, constructors always have implicitly the return type of their class
            constructorDeclaration.type = computeType(constructorDeclaration)
            frontend.scopeManager.addDeclaration(constructorDeclaration)
            recordDeclaration.innerConstructors += constructorDeclaration
        }

        frontend.scopeManager.leaveScope(recordDeclaration)

        return recordDeclaration
    }

    /**
     * Handles template parameters that are types
     *
     * @param ctx
     * @return TypeParameterDeclaration with its name
     */
    private fun handleTemplateTypeParameter(
        ctx: CPPASTSimpleTypeTemplateParameter
    ): TypeParameterDeclaration {
        return newTypeParameterDeclaration(ctx.rawSignature, rawNode = ctx)
    }

    private fun processMembers(
        recordDeclaration: RecordDeclaration,
        ctx: IASTCompositeTypeSpecifier,
    ) {
        for (member in ctx.members) {
            if (member is CPPASTVisibilityLabel) {
                // TODO: parse visibility
                continue
            }

            val declaration = frontend.declarationHandler.handle(member)
            if (declaration != null) {
                frontend.scopeManager.addDeclaration(declaration)
                recordDeclaration.addDeclaration(declaration)
            }
        }
    }
}

/**
 * This function returns the real name (declarator) of this [IASTDeclarator]. The name itself can be
 * wrapped in many layers of nested declarators, e.g., if the name is wrapped in ().
 */
fun IASTDeclarator.realName(): Pair<IASTDeclarator, Boolean> {
    var nameDecl: IASTDeclarator = this
    var hasPointer = false
    while (nameDecl.nestedDeclarator != null) {
        nameDecl = nameDecl.nestedDeclarator
        if (nameDecl.pointerOperators.isNotEmpty()) {
            hasPointer = true
        }
    }
    return Pair(nameDecl, hasPointer)
}
