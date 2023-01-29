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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.CallResolver
import java.math.BigInteger
import java.util.*
import java.util.function.Supplier
import kotlin.math.max
import org.eclipse.cdt.core.dom.ast.*
import org.eclipse.cdt.internal.core.dom.parser.CStringValue
import org.eclipse.cdt.internal.core.dom.parser.ProblemBinding
import org.eclipse.cdt.internal.core.dom.parser.ProblemType
import org.eclipse.cdt.internal.core.dom.parser.c.CASTDesignatedInitializer
import org.eclipse.cdt.internal.core.dom.parser.cpp.*
import org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.TypeOfDependentExpression

/**
 * Note: CDT expresses hierarchies in Interfaces to allow to have multi-inheritance in java. Because
 * some Expressions have sub elements of type IASTInitializerClause and in the hierarchy
 * IASTExpression extends IASTInitializerClause. The later is the appropriate Interface type for the
 * handler.
 */
class ExpressionHandler(lang: CXXLanguageFrontend) :
    CXXHandler<Expression, IASTInitializerClause>(Supplier(::ProblemExpression), lang) {

    override fun handleNode(node: IASTInitializerClause): Expression {
        return when (node) {
            is IASTLiteralExpression -> handleLiteralExpression(node)
            is IASTBinaryExpression -> handleBinaryExpression(node)
            is IASTUnaryExpression -> handleUnaryExpression(node)
            is IASTConditionalExpression -> handleConditionalExpression(node)
            is IASTIdExpression -> handleIdExpression(node)
            is IASTFieldReference -> handleFieldReference(node)
            is IASTFunctionCallExpression -> handleFunctionCallExpression(node)
            is IASTCastExpression -> handleCastExpression(node)
            is IASTInitializerList -> handleInitializerList(node)
            is IASTExpressionList -> handleExpressionList(node)
            is IASTArraySubscriptExpression -> handleArraySubscriptExpression(node)
            is IASTTypeIdExpression -> handleTypeIdExpression(node)
            is CPPASTSimpleTypeConstructorExpression -> handleSimpleTypeConstructorExpression(node)
            is CPPASTNewExpression -> handleNewExpression(node)
            is CPPASTDesignatedInitializer -> handleCXXDesignatedInitializer(node)
            is CASTDesignatedInitializer -> handleCDesignatedInitializer(node)
            is CPPASTDeleteExpression -> handleDeleteExpression(node)
            is CPPASTCompoundStatementExpression -> handleCompoundStatementExpression(node)
            else -> {
                return handleNotSupported(node, node.javaClass.name)
            }
        }
    }

    /**
     * Tries to return the [IType] for a given AST expression. In case this fails, the constant type
     * [ProblemType.UNKNOWN_FOR_EXPRESSION] is returned.
     *
     * @param expression the ast expression
     * @return a CDT type
     */
    private fun expressionTypeProxy(expression: IASTExpression): IType {
        var expressionType = ProblemType.UNKNOWN_FOR_EXPRESSION
        try {
            expressionType = expression.expressionType
        } catch (e: AssertionError) {
            val codeFromRawNode = frontend.getCodeFromRawNode(expression)
            Util.warnWithFileLocation(
                frontend,
                expression,
                log,
                "Unknown Expression Type: {}",
                codeFromRawNode
            )
        }
        return expressionType
    }

    private fun handleCompoundStatementExpression(
        ctx: CPPASTCompoundStatementExpression
    ): Expression {
        val cse = newCompoundStatementExpression(ctx.rawSignature)
        cse.statement = frontend.statementHandler.handle(ctx.compoundStatement)
        return cse
    }

    private fun handleTypeIdExpression(ctx: IASTTypeIdExpression): TypeIdExpression {
        // Eclipse CDT seems to support the following operators
        // 0 sizeof
        // 1 typeid
        // 2 alignof
        // 3 typeof
        // 22 sizeof... (however does not really work)
        // there are a lot of other constants defined for type traits, but they are not really
        // parsed as
        // type id expressions
        var operatorCode = ""
        var type: Type = UnknownType.getUnknownType(language)
        when (ctx.operator) {
            IASTTypeIdExpression.op_sizeof -> {
                operatorCode = "sizeof"
                type = parseType("std::size_t")
            }
            IASTTypeIdExpression.op_typeid -> {
                operatorCode = "typeid"
                type = parseType("const std::type_info&")
            }
            IASTTypeIdExpression.op_alignof -> {
                operatorCode = "alignof"
                type = parseType("std::size_t")
            }
            IASTTypeIdExpression
                .op_typeof -> // typeof is not an official c++ keyword - not sure why eclipse
                // supports it
                operatorCode = "typeof"
            else -> log.debug("Unknown typeid operator code: {}", ctx.operator)
        }

        // TODO: proper type resolve
        val referencedType =
            TypeParser.createFrom(ctx.typeId.declSpecifier.toString(), true, frontend)
        return newTypeIdExpression(operatorCode, type, referencedType, ctx.rawSignature)
    }

    private fun handleArraySubscriptExpression(ctx: IASTArraySubscriptExpression): Expression {
        val arraySubsExpression = newArraySubscriptionExpression(ctx.rawSignature)
        handle(ctx.arrayExpression)?.let { arraySubsExpression.arrayExpression = it }
        handle(ctx.argument)?.let { arraySubsExpression.subscriptExpression = it }
        return arraySubsExpression
    }

    private fun handleNewExpression(ctx: CPPASTNewExpression): Expression {
        val name = ctx.typeId.declSpecifier.toString()
        val code = ctx.rawSignature
        val t = TypeParser.createFrom(name, true, frontend)
        val init = ctx.initializer

        // we need to check, whether this is an array initialization or a single new expression
        return if (ctx.isArrayAllocation) {
            t.reference(PointerOrigin.ARRAY)
            val arrayMods = (ctx.typeId.abstractDeclarator as IASTArrayDeclarator).arrayModifiers
            val arrayCreate = newArrayCreationExpression(code)
            arrayCreate.type = t
            for (arrayMod in arrayMods) {
                val constant = handle(arrayMod.constantExpression)
                constant?.let { arrayCreate.addDimension(it) }
            }
            if (init != null) {
                arrayCreate.initializer = frontend.initializerHandler.handle(init)
            }
            arrayCreate
        } else {
            // Resolve possible templates
            var templateParameters: List<Node> = emptyList()
            val declSpecifier = ctx.typeId.declSpecifier as? IASTNamedTypeSpecifier
            if (declSpecifier?.name is CPPASTTemplateId) {
                templateParameters = getTemplateArguments(declSpecifier.name as CPPASTTemplateId)
                assert(t.root is ObjectType)
                val objectType = t.root as? ObjectType
                val generics = templateParameters.filterIsInstance<TypeExpression>().map { it.type }
                objectType?.generics = generics
            }

            // new returns a pointer, so we need to reference the type by pointer
            val newExpression =
                newNewExpression(code, t.reference(PointerOrigin.POINTER), frontend.language)
            newExpression.templateParameters = templateParameters
            val initializer: Expression?
            if (init != null) {
                initializer = frontend.initializerHandler.handle(init)
            } else {
                // in C++, it is possible to omit the `()` part, when creating an object, such as
                // `new A`.
                // Therefore, CDT does not have an explicit construct expression, so we need create
                // an implicit one
                initializer = newConstructExpression(t.name.localName, "${t.name.localName}()")
                initializer.isImplicit = true
            }

            // we also need to "forward" our template parameters (if we have any) to the construct
            // expression since the construct expression will do the actual template instantiation
            if (
                newExpression.templateParameters != null &&
                    newExpression.templateParameters!!.isNotEmpty()
            ) {
                CallResolver.addImplicitTemplateParametersToCall(
                    newExpression.templateParameters!!,
                    initializer as ConstructExpression
                )
            }

            // our initializer, such as a construct expression, will have the non-pointer type
            initializer!!.type = t
            newExpression.initializer = initializer
            newExpression
        }
    }

    /**
     * Retrieves all arguments a template was instantiated with. Note, that the arguments can either
     * be Expressions referring to a value ot TypeExpressions referring to a type.
     *
     * @param template
     * @return List of Nodes containing the all the arguments the template was instantiated with.
     */
    private fun getTemplateArguments(template: CPPASTTemplateId): List<Node> {
        val templateArguments: MutableList<Node> = ArrayList()
        for (argument in template.templateArguments) {
            if (argument is IASTTypeId) {
                val type = parseType(argument.declSpecifier.toString())
                templateArguments.add(newTypeExpression(type.name, type))
            } else if (argument is IASTLiteralExpression) {
                frontend.expressionHandler.handle(argument as IASTInitializerClause)?.let {
                    templateArguments.add(it)
                }
            } else if (argument is IASTIdExpression) {
                // add to templateArguments
                frontend.expressionHandler.handle(argument)?.let { templateArguments.add(it) }
            }
        }
        return templateArguments
    }

    private fun handleConditionalExpression(ctx: IASTConditionalExpression): ConditionalExpression {
        val condition =
            handle(ctx.logicalConditionExpression)
                ?: ProblemExpression("could not parse condition expression")
        return newConditionalExpression(
            condition,
            if (ctx.positiveResultExpression != null) handle(ctx.positiveResultExpression)
            else condition,
            handle(ctx.negativeResultExpression),
            TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, frontend),
        )
    }

    private fun handleDeleteExpression(ctx: CPPASTDeleteExpression): DeleteExpression {
        val deleteExpression = newDeleteExpression(ctx.rawSignature)
        for (name in ctx.implicitDestructorNames) {
            log.debug("Implicit constructor name {}", name)
        }
        deleteExpression.operand = handle(ctx.operand)
        return deleteExpression
    }

    private fun handleCastExpression(ctx: IASTCastExpression): Expression {
        val castExpression = newCastExpression(ctx.rawSignature)
        castExpression.expression = handle(ctx.operand)
        castExpression.setCastOperator(ctx.operator)
        val castType: Type
        val iType = expressionTypeProxy(ctx)
        castType =
            if (iType is CPPPointerType) {
                if (iType.type is IProblemType) {
                    // fall back to fTypeId
                    TypeParser.createFrom(ctx.typeId.declSpecifier.toString() + "*", true, frontend)
                } else {
                    TypeParser.createFrom(iType.type.toString() + "*", true, frontend)
                }
            } else if (iType is IProblemType) {
                // fall back to fTypeId
                TypeParser.createFrom(ctx.typeId.declSpecifier.toString(), true, frontend)
                // TODO: try to actually resolve the type (similar to NewExpression) using
                // ((IASTNamedTypeSpecifier) declSpecifier).getName().resolveBinding()
            } else {
                TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, frontend)
            }
        castExpression.castType = castType
        if (TypeManager.isPrimitive(castExpression.castType, language) || ctx.operator == 4) {
            castExpression.type = castExpression.castType
        } else {
            castExpression.expression.registerTypeListener(castExpression)
        }
        return castExpression
    }

    private fun handleSimpleTypeConstructorExpression(
        ctx: CPPASTSimpleTypeConstructorExpression
    ): Expression {
        val castExpression = newCastExpression(ctx.rawSignature)
        castExpression.expression = frontend.initializerHandler.handle(ctx.initializer)
        castExpression.setCastOperator(0) // cast

        val castType: Type =
            if (expressionTypeProxy(ctx) is CPPPointerType) {
                val pointerType = expressionTypeProxy(ctx) as CPPPointerType
                TypeParser.createFrom(pointerType.type.toString() + "*", true, frontend)
            } else {
                TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, frontend)
            }

        castExpression.castType = castType
        if (TypeManager.isPrimitive(castExpression.castType, language)) {
            castExpression.type = castExpression.castType
        } else {
            castExpression.expression.registerTypeListener(castExpression)
        }
        return castExpression
    }

    /**
     * Translates a C/C++
     * [member access](https://en.cppreference.com/w/cpp/language/operator_member_access) into a
     * [MemberExpression].
     */
    private fun handleFieldReference(ctx: IASTFieldReference): Expression {
        var base = handle(ctx.fieldOwner)

        // Replace Literal this with a reference pointing to the receiver, which also called an
        // implicit object parameter in C++ (see
        // https://en.cppreference.com/w/cpp/language/overload_resolution#Implicit_object_parameter). It is sufficient to have the refers, it will be connected by the resolver later.
        if (base is Literal<*> && base.value == "this") {
            val location = base.location
            base = newDeclaredReferenceExpression("this", base.type, base.code)
            base.location = location
        }

        if (base == null) {
            return newProblemExpression("base of field is null")
        }

        // We need some special handling for templates (of course). Since we only want the basic
        // name without any arguments as a name
        val name =
            if (ctx.fieldName is CPPASTTemplateId) {
                (ctx.fieldName as CPPASTTemplateId).templateName.toString()
            } else {
                ctx.fieldName.toString()
            }

        return newMemberExpression(
            name,
            base,
            UnknownType.getUnknownType(language),
            if (ctx.isPointerDereference) "->" else ".",
            ctx.rawSignature
        )
    }

    private fun handleUnaryExpression(ctx: IASTUnaryExpression): Expression {
        var input: Expression? = null
        if (ctx.operand != null) { // can be null e.g. for "throw;"
            input = handle(ctx.operand)
        }
        var operatorCode = ""
        when (ctx.operator) {
            IASTUnaryExpression.op_prefixIncr,
            IASTUnaryExpression.op_postFixIncr -> operatorCode = "++"
            IASTUnaryExpression.op_prefixDecr,
            IASTUnaryExpression.op_postFixDecr -> operatorCode = "--"
            IASTUnaryExpression.op_plus -> operatorCode = "+"
            IASTUnaryExpression.op_minus -> operatorCode = "-"
            IASTUnaryExpression.op_star -> operatorCode = "*"
            IASTUnaryExpression.op_amper -> operatorCode = "&"
            IASTUnaryExpression.op_tilde -> operatorCode = "~"
            IASTUnaryExpression.op_not -> operatorCode = "!"
            IASTUnaryExpression.op_sizeof -> operatorCode = "sizeof"
            IASTUnaryExpression.op_bracketedPrimary -> {
                if (
                    frontend.config.inferenceConfiguration.guessCastExpressions &&
                        ctx.operand is IASTIdExpression
                ) {
                    // this can either be just a meaningless bracket or it can be a cast expression
                    val typeName = (ctx.operand as IASTIdExpression).name.toString()
                    if (TypeManager.getInstance().typeExists(typeName)) {
                        val cast = newCastExpression(frontend.getCodeFromRawNode<Any>(ctx))
                        cast.castType = parseType(typeName)
                        cast.expression = input
                        cast.location = frontend.getLocationFromRawNode<Any>(ctx)
                        return cast
                    }
                }

                // otherwise, ignore this kind of expression and return the input directly
                return input as Expression
            }
            IASTUnaryExpression.op_throw -> operatorCode = "throw"
            IASTUnaryExpression.op_typeid -> operatorCode = "typeid"
            IASTUnaryExpression.op_alignOf -> operatorCode = "alignof"
            IASTUnaryExpression.op_sizeofParameterPack -> operatorCode = "sizeof..."
            IASTUnaryExpression.op_noexcept -> operatorCode = "noexcept"
            IASTUnaryExpression.op_labelReference -> operatorCode = ""
            else ->
                Util.errorWithFileLocation(frontend, ctx, log, "unknown operator {}", ctx.operator)
        }
        val unaryOperator =
            newUnaryOperator(
                operatorCode,
                ctx.isPostfixOperator,
                !ctx.isPostfixOperator,
                ctx.rawSignature
            )
        if (input != null) {
            unaryOperator.input = input
        }
        return unaryOperator
    }

    private fun handleFunctionCallExpression(ctx: IASTFunctionCallExpression): Expression {
        val reference = handle(ctx.functionNameExpression)
        val callExpression: CallExpression
        if (reference is MemberExpression) {
            val baseType: Type = reference.base.type.root
            assert(baseType !is SecondOrderType)
            callExpression = newMemberCallExpression(reference, code = ctx.rawSignature)
            if (
                (ctx.functionNameExpression as? IASTFieldReference)?.fieldName is CPPASTTemplateId
            ) {
                // Make necessary adjustments if we are handling a function template
                val name =
                    ((ctx.functionNameExpression as IASTFieldReference).fieldName
                            as CPPASTTemplateId)
                        .templateName
                        .toString()
                callExpression.name = language.parseName(name)
                getTemplateArguments(
                        (ctx.functionNameExpression as IASTFieldReference).fieldName
                            as CPPASTTemplateId
                    )
                    .forEach { callExpression.addTemplateParameter(it) }
            }
        } else if (
            reference is BinaryOperator &&
                (reference.operatorCode == ".*" || reference.operatorCode == "->*")
        ) {
            // This is a function pointer call to a class method. We keep this as a binary operator
            // with the .* or ->* operator code, so that we can resolve this later in the
            // FunctionPointerCallResolver
            callExpression = newMemberCallExpression(reference, code = ctx.rawSignature)
        } else if (reference is UnaryOperator && reference.operatorCode == "*") {
            // Classic C-style function pointer call -> let's extract the target
            callExpression = newCallExpression(reference, "", reference.code, false)
        } else if (
            ctx.functionNameExpression is IASTIdExpression &&
                (ctx.functionNameExpression as IASTIdExpression).name is CPPASTTemplateId
        ) {
            val name =
                ((ctx.functionNameExpression as IASTIdExpression).name as CPPASTTemplateId)
                    .templateName
                    .toString()
            val ref = newDeclaredReferenceExpression(name)
            callExpression = newCallExpression(ref, name, ctx.rawSignature, true)
            getTemplateArguments(
                    (ctx.functionNameExpression as IASTIdExpression).name as CPPASTTemplateId
                )
                .forEach { callExpression.addTemplateParameter(it) }
        } else if (reference is CastExpression) {
            // this really is a cast expression in disguise
            return reference
        } else {
            callExpression = newCallExpression(reference, reference?.name, ctx.rawSignature, false)
        }

        for ((i, argument) in ctx.arguments.withIndex()) {
            val arg = handle(argument)
            arg!!.argumentIndex = i
            callExpression.addArgument(arg)
        }

        // Important: we don't really need the reference node, but even its temporary creation might
        // leave unwanted artifacts behind in the final graph!
        reference!!.disconnectFromGraph()
        return callExpression
    }

    private fun handleIdExpression(ctx: IASTIdExpression): DeclaredReferenceExpression {
        val declaredReferenceExpression =
            newDeclaredReferenceExpression(
                ctx.name.toString(),
                UnknownType.getUnknownType(language),
                ctx.rawSignature
            )
        val proxy = expressionTypeProxy(ctx)
        if (proxy is CPPClassInstance) {
            // Handle Template Types separately
            handleTemplateTypeOfDeclaredReferenceExpression(proxy, declaredReferenceExpression)
        } else if (proxy !is TypeOfDependentExpression) {
            declaredReferenceExpression.type =
                TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, frontend)
        }

        /* this expression could actually be a field / member expression, but somehow CDT only recognizes them as a member expression if it has an explicit 'this'
         */
        // TODO: handle this? convert the declared reference expression into a member expression?
        return declaredReferenceExpression
    }

    /**
     * Sets type of DeclaredReferenceExpression if type represents a template
     *
     * @param proxy
     * @param declaredReferenceExpression
     */
    private fun handleTemplateTypeOfDeclaredReferenceExpression(
        proxy: IType,
        declaredReferenceExpression: DeclaredReferenceExpression
    ) {
        val type =
            parseType((proxy as CPPClassInstance).templateDefinition.toString()) as? ObjectType
        for (templateArgument in proxy.templateArguments) {
            if (templateArgument is CPPTemplateTypeArgument) {
                type?.addGeneric(parseType(templateArgument.toString()))
            }
        }
        type?.let { declaredReferenceExpression.type = it }
    }

    private fun handleExpressionList(exprList: IASTExpressionList): ExpressionList {
        val expressionList = newExpressionList(exprList.rawSignature)
        for (expr in exprList.expressions) {
            expressionList.addExpression(handle(expr))
        }
        return expressionList
    }

    private fun handleBinaryExpression(ctx: IASTBinaryExpression): BinaryOperator {
        var operatorCode = ""
        when (ctx.operator) {
            IASTBinaryExpression.op_multiply -> operatorCode = "*"
            IASTBinaryExpression.op_divide -> operatorCode = "/"
            IASTBinaryExpression.op_modulo -> operatorCode = "%"
            IASTBinaryExpression.op_plus -> operatorCode = "+"
            IASTBinaryExpression.op_minus -> operatorCode = "-"
            IASTBinaryExpression.op_shiftLeft -> operatorCode = "<<"
            IASTBinaryExpression.op_shiftRight -> operatorCode = ">>"
            IASTBinaryExpression.op_lessThan -> operatorCode = "<"
            IASTBinaryExpression.op_greaterThan -> operatorCode = ">"
            IASTBinaryExpression.op_lessEqual -> operatorCode = "<="
            IASTBinaryExpression.op_greaterEqual -> operatorCode = ">="
            IASTBinaryExpression.op_binaryAnd -> operatorCode = "&"
            IASTBinaryExpression.op_binaryXor -> operatorCode = "^"
            IASTBinaryExpression.op_binaryOr -> operatorCode = "|"
            IASTBinaryExpression.op_logicalAnd -> operatorCode = "&&"
            IASTBinaryExpression.op_logicalOr -> operatorCode = "||"
            IASTBinaryExpression.op_assign -> operatorCode = "="
            IASTBinaryExpression.op_multiplyAssign -> operatorCode = "*="
            IASTBinaryExpression.op_divideAssign -> operatorCode = "/="
            IASTBinaryExpression.op_moduloAssign -> operatorCode = "%="
            IASTBinaryExpression.op_plusAssign -> operatorCode = "+="
            IASTBinaryExpression.op_minusAssign -> operatorCode = "-="
            IASTBinaryExpression.op_shiftLeftAssign -> operatorCode = "<<="
            IASTBinaryExpression.op_shiftRightAssign -> operatorCode = ">>="
            IASTBinaryExpression.op_binaryAndAssign -> operatorCode = "&="
            IASTBinaryExpression.op_binaryXorAssign -> operatorCode = "^="
            IASTBinaryExpression.op_binaryOrAssign -> operatorCode = "|="
            IASTBinaryExpression.op_equals -> operatorCode = "=="
            IASTBinaryExpression.op_notequals -> operatorCode = "!="
            IASTBinaryExpression.op_pmdot -> operatorCode = ".*"
            IASTBinaryExpression.op_pmarrow -> operatorCode = "->*"
            IASTBinaryExpression.op_max -> operatorCode = ">?"
            IASTBinaryExpression.op_min -> operatorCode = "?<"
            IASTBinaryExpression.op_ellipses -> operatorCode = "..."
            else ->
                Util.errorWithFileLocation(frontend, ctx, log, "unknown operator {}", ctx.operator)
        }
        val binaryOperator = newBinaryOperator(operatorCode, ctx.rawSignature)
        val lhs = handle(ctx.operand1)
        val rhs =
            if (ctx.operand2 != null) {
                handle(ctx.operand2)
            } else {
                handle(ctx.initOperand2)
            }
        binaryOperator.lhs = lhs
        binaryOperator.rhs = rhs
        when (expressionTypeProxy(ctx)) {
            is ProblemType,
            is ProblemBinding -> {
                log.trace("CDT could not deduce type. Type is set to null")
            }
            is TypeOfDependentExpression -> {
                log.debug("Type of Expression depends on the type the template is initialized with")
                binaryOperator.type = UnknownType.getUnknownType(language)
            }
            else -> {
                binaryOperator.type =
                    TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, frontend)
            }
        }

        return binaryOperator
    }

    private fun handleLiteralExpression(ctx: IASTLiteralExpression): Literal<*> {
        val type = expressionTypeProxy(ctx)
        // TODO: parse C literal
        val value =
            if (ctx is CPPASTLiteralExpression) {
                ctx.evaluation.value
            } else {
                CPPASTLiteralExpression(ctx.kind, ctx.value).evaluation.value
            }

        val generatedType = TypeParser.createFrom(type.toString(), true, frontend)
        if (
            value.numberValue() == null // e.g. for 0x1p-52
            && value !is CStringValue
        ) {
            return newLiteral(value.toString(), generatedType, ctx.rawSignature)
        }
        if (type is IBasicType && type.kind == IBasicType.Kind.eInt) {
            return handleIntegerLiteral(ctx)
        } else if (type.isSameType(CPPBasicType.BOOLEAN)) {
            return newLiteral(value.numberValue().toInt() == 1, generatedType, ctx.rawSignature)
        } else if (value is CStringValue) {
            return newLiteral(value.cStringValue(), generatedType, ctx.rawSignature)
        } else if (type is CPPBasicType && type.kind == IBasicType.Kind.eFloat) {
            return newLiteral(value.numberValue().toFloat(), generatedType, ctx.rawSignature)
        } else if (type is CPPBasicType && type.kind == IBasicType.Kind.eDouble) {
            return newLiteral(value.numberValue().toDouble(), generatedType, ctx.rawSignature)
        } else if (type is CPPBasicType && type.kind == IBasicType.Kind.eChar) {
            return newLiteral(value.numberValue().toInt().toChar(), generatedType, ctx.rawSignature)
        }
        return newLiteral(value.toString(), generatedType, ctx.rawSignature)
    }

    private fun handleInitializerList(ctx: IASTInitializerList): InitializerListExpression {
        val expression = newInitializerListExpression(ctx.rawSignature)

        for (clause in ctx.clauses) {
            handle(clause)?.let {
                val edge = PropertyEdge(expression, it)
                edge.addProperty(Properties.INDEX, expression.initializerEdges.size)

                expression.initializerEdges.add(edge)
            }
        }

        return expression
    }

    private fun handleCXXDesignatedInitializer(
        ctx: CPPASTDesignatedInitializer
    ): DesignatedInitializerExpression {
        val rhs = handle(ctx.operand)
        val lhs = ArrayList<Expression>()
        if (ctx.designators.isEmpty()) {
            Util.errorWithFileLocation(frontend, ctx, log, "no designator found")
        } else {
            for (des in ctx.designators) {
                var oneLhs: Expression? = null
                when (des) {
                    is CPPASTArrayDesignator -> {
                        oneLhs = handle(des.subscriptExpression)
                    }
                    is CPPASTFieldDesignator -> {
                        oneLhs =
                            newDeclaredReferenceExpression(
                                des.name.toString(),
                                UnknownType.getUnknownType(language),
                                des.getRawSignature()
                            )
                    }
                    is CPPASTArrayRangeDesignator -> {
                        oneLhs =
                            newArrayRangeExpression(
                                handle(des.rangeFloor),
                                handle(des.rangeCeiling),
                                des.getRawSignature()
                            )
                    }
                    else -> {
                        Util.errorWithFileLocation(
                            frontend,
                            ctx,
                            log,
                            "Unknown designated lhs {}",
                            des.javaClass.toGenericString()
                        )
                    }
                }
                if (oneLhs != null) {
                    lhs.add(oneLhs)
                }
            }
        }

        val die = newDesignatedInitializerExpression(ctx.rawSignature)
        die.lhs = lhs
        die.rhs = rhs

        return die
    }

    private fun handleCDesignatedInitializer(
        ctx: CASTDesignatedInitializer
    ): DesignatedInitializerExpression {
        val rhs = handle(ctx.operand)
        val lhs = ArrayList<Expression>()
        if (ctx.designators.isEmpty()) {
            Util.errorWithFileLocation(frontend, ctx, log, "no designator found")
        } else {
            for (des in ctx.designators) {
                var oneLhs: Expression? = null
                when (des) {
                    is CPPASTArrayDesignator -> {
                        oneLhs = handle(des.subscriptExpression)
                    }
                    is CPPASTFieldDesignator -> {
                        oneLhs =
                            newDeclaredReferenceExpression(
                                des.name.toString(),
                                UnknownType.getUnknownType(language),
                                des.getRawSignature()
                            )
                    }
                    is CPPASTArrayRangeDesignator -> {
                        oneLhs =
                            newArrayRangeExpression(
                                handle(des.rangeFloor),
                                handle(des.rangeCeiling),
                                des.getRawSignature()
                            )
                    }
                    else -> {
                        Util.errorWithFileLocation(
                            frontend,
                            ctx,
                            log,
                            "Unknown designated lhs {}",
                            des.javaClass.toGenericString()
                        )
                    }
                }
                if (oneLhs != null) {
                    lhs.add(oneLhs)
                }
            }
        }

        val die = newDesignatedInitializerExpression(ctx.rawSignature)
        die.lhs = lhs
        die.rhs = rhs

        return die
    }

    private fun handleIntegerLiteral(ctx: IASTLiteralExpression): Literal<*> {
        val value = String(ctx.value).lowercase(Locale.getDefault())
        val bigValue: BigInteger
        val suffix = value.suffix

        // first, strip the suffix from the value
        var strippedValue = value.substring(0, value.length - suffix.length)

        // next, check for possible prefixes
        var radix = 10
        var offset = 0
        if (value.startsWith("0b")) {
            radix = 2 // binary
            offset = 2 // len("0b")
        } else if (value.startsWith("0x")) {
            radix = 16 // hex
            offset = 2 // len("0x")
        } else if (value.startsWith("0") && strippedValue.length > 1) {
            radix = 8 // octal
            offset = 1 // len("0")
        }

        // strip the prefix
        strippedValue = strippedValue.substring(offset)

        // remove single quotes inside integer literal, used as a separator (C++14)
        strippedValue = strippedValue.replace("'", "")

        // basically we parse everything as BigInteger and then decide what to do
        bigValue = BigInteger(strippedValue, radix)
        val numberValue: Number
        if ("ull" == suffix || "ul" == suffix) {
            // unsigned long (long) will always be represented as BigInteger
            numberValue = bigValue
        } else if ("ll" == suffix || "l" == suffix) {
            // both long and long long can be represented in Java long, but only within
            // Long.MAX_VALUE
            if (bigValue > BigInteger.valueOf(Long.MAX_VALUE)) {
                // keep it as BigInteger
                numberValue = bigValue
                Util.warnWithFileLocation(
                    frontend,
                    ctx,
                    log,
                    "Integer literal {} is too large to represented in a signed type, interpreting it as unsigned.",
                    ctx
                )
            } else {
                numberValue = bigValue.toLong()
            }
        } else if (bigValue > BigInteger.valueOf(Long.MAX_VALUE)) {
            // No suffix, we just cast it to the appropriate signed type that is required, but only
            // within Long.MAX_VALUE

            // keep it as BigInteger
            numberValue = bigValue
            Util.warnWithFileLocation(
                frontend,
                ctx,
                log,
                "Integer literal {} is too large to represented in a signed type, interpreting it as unsigned.",
                ctx
            )
        } else if (bigValue.toLong() > Int.MAX_VALUE) {
            numberValue = bigValue.toLong()
        } else {
            numberValue = bigValue.toInt()
        }

        // retrieve type based on stored Java number
        val type =
            if (numberValue is BigInteger && "ul" == suffix) {
                // we follow the way clang/llvm handles this and this seems to always
                // be an unsigned long long, except if it is explicitly specified as ul
                parseType("unsigned long")
            } else if (numberValue is BigInteger) {
                parseType("unsigned long long")
            } else if (numberValue is Long && "ll" == suffix) {
                // differentiate between long and long long
                parseType("long long")
            } else if (numberValue is Long) {
                parseType("long")
            } else {
                parseType("int")
            }

        return newLiteral(numberValue, type, ctx.rawSignature)
    }

    private val String.suffix: String
        get() {
            var suffix = ""

            // maximum suffix length is 3
            for (i in 1..3) {
                val digit = this.substring(max(0, this.length - i))
                suffix =
                    if (
                        digit.chars().allMatch { character: Int ->
                            character == 'u'.code || character == 'l'.code
                        }
                    ) {
                        digit
                    } else {
                        break
                    }
            }

            return suffix
        }
}

private val IASTUnaryExpression.isPostfixOperator: Boolean
    get() {
        return this.operator == IASTUnaryExpression.op_postFixDecr ||
            this.operator == IASTUnaryExpression.op_postFixIncr
    }
