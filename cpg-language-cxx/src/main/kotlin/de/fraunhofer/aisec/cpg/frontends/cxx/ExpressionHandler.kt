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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.CallResolver
import java.math.BigInteger
import java.util.*
import java.util.function.Supplier
import kotlin.math.max
import org.eclipse.cdt.core.dom.ast.*
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.*
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression.*
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLambdaExpression
import org.eclipse.cdt.internal.core.dom.parser.c.CASTDesignatedInitializer
import org.eclipse.cdt.internal.core.dom.parser.cpp.*
import org.eclipse.cdt.internal.core.model.ASTStringUtil

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
            is IASTExpressionList -> handleExpressionList(node)
            is IASTInitializerList -> frontend.initializerHandler.handle(node)
                    ?: ProblemExpression("could not parse initializer list")
            is IASTArraySubscriptExpression -> handleArraySubscriptExpression(node)
            is IASTTypeIdExpression -> handleTypeIdExpression(node)
            is CPPASTNewExpression -> handleNewExpression(node)
            is CPPASTDesignatedInitializer -> handleCXXDesignatedInitializer(node)
            is CASTDesignatedInitializer -> handleCDesignatedInitializer(node)
            is CPPASTDeleteExpression -> handleDeleteExpression(node)
            is CPPASTCompoundStatementExpression -> handleCompoundStatementExpression(node)
            is CPPASTLambdaExpression -> handleLambdaExpression(node)
            else -> {
                return handleNotSupported(node, node.javaClass.name)
            }
        }
    }

    private fun handleLambdaExpression(node: CPPASTLambdaExpression): Expression {
        val lambda = newLambdaExpression(frontend.codeOf(node))

        // Variables passed by reference are mutable. If we have initializers, we have to model the
        // variable explicitly.
        for (capture in node.captures) {
            if (capture is CPPASTInitCapture) {
                // TODO: The scope manager isn't able to resolve this correctly.
                frontend.declaratorHandler.handle(capture.declarator)?.let {
                    it.isImplicit = true
                    lambda.addDeclaration(it)
                }
            } else {
                if (capture.isByReference) {
                    val valueDeclaration =
                        frontend.scopeManager.resolveReference(
                            newDeclaredReferenceExpression(capture?.identifier?.toString())
                        )
                    valueDeclaration?.let { lambda.mutableVariables.add(it) }
                }
            }
        }

        // By default, the outer variables passed by value to the lambda are immutable. But we can
        // either make the function "mutable" or pass everything by reference.
        lambda.areVariablesMutable =
            (node.declarator as? CPPASTFunctionDeclarator)?.isMutable == true ||
                node.captureDefault == ICPPASTLambdaExpression.CaptureDefault.BY_REFERENCE

        val anonymousFunction =
            node.declarator?.let { frontend.declaratorHandler.handle(it) as? FunctionDeclaration }
                ?: newFunctionDeclaration("lambda${lambda.hashCode()}")
        anonymousFunction.type = FunctionType.computeType(anonymousFunction)

        frontend.scopeManager.enterScope(anonymousFunction)
        anonymousFunction.body = frontend.statementHandler.handle(node.body)
        frontend.scopeManager.leaveScope(anonymousFunction)

        lambda.function = anonymousFunction

        return lambda
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
        // * 0 sizeof
        // * 1 typeid
        // * 2 alignof
        // * 3 typeof
        // * 22 sizeof... (however does not really work)
        // there are a lot of other constants defined for type traits, but they are not really
        // parsed as type id expressions
        var operatorCode = ""
        var type: Type = unknownType()
        when (ctx.operator) {
            IASTTypeIdExpression.op_sizeof -> {
                operatorCode = "sizeof"
                type = objectType("std::size_t")
            }
            IASTTypeIdExpression.op_typeid -> {
                operatorCode = "typeid"
                type = objectType("std::type_info").ref()
            }
            IASTTypeIdExpression.op_alignof -> {
                operatorCode = "alignof"
                type = objectType("std::size_t")
            }
            IASTTypeIdExpression
                .op_typeof -> // typeof is not an official c++ keyword - not sure why eclipse
                // supports it
                operatorCode = "typeof"
            else -> log.debug("Unknown typeid operator code: {}", ctx.operator)
        }

        val referencedType = frontend.typeOf(ctx.typeId)
        return newTypeIdExpression(operatorCode, type, referencedType, ctx.rawSignature)
    }

    private fun handleArraySubscriptExpression(ctx: IASTArraySubscriptExpression): Expression {
        val arraySubsExpression = newArraySubscriptionExpression(ctx.rawSignature)
        handle(ctx.arrayExpression)?.let { arraySubsExpression.arrayExpression = it }
        handle(ctx.argument)?.let { arraySubsExpression.subscriptExpression = it }
        return arraySubsExpression
    }

    private fun handleNewExpression(ctx: CPPASTNewExpression): Expression {
        val code = ctx.rawSignature
        val t = frontend.typeOf(ctx.typeId)
        val init = ctx.initializer

        // we need to check, whether this is an array initialization or a single new expression
        return if (ctx.isArrayAllocation) {
            t.array()
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
            // new returns a pointer, so we need to reference the type by pointer
            val newExpression = newNewExpression(code, t.pointer(), ctx)
            val declSpecifier = ctx.typeId.declSpecifier as? IASTNamedTypeSpecifier
            // Resolve possible templates
            if (declSpecifier?.name is CPPASTTemplateId) {
                newExpression.templateParameters =
                    getTemplateArguments(declSpecifier.name as CPPASTTemplateId)
            }

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
                initializer.type = t
            }

            // we also need to "forward" our template parameters (if we have any) to the construct
            // expression since the construct expression will do the actual template instantiation
            if (newExpression.templateParameters?.isNotEmpty() == true) {
                newExpression.templateParameters?.let {
                    CallResolver.addImplicitTemplateParametersToCall(
                        it,
                        initializer as ConstructExpression
                    )
                }
            }

            // our initializer, such as a construct expression, will have the non-pointer type
            initializer?.type = t
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
            when (argument) {
                is IASTTypeId -> {
                    val type = frontend.typeOf(argument)
                    templateArguments.add(newTypeExpression(type.name, type))
                }
                is IASTLiteralExpression -> {
                    frontend.expressionHandler.handle(argument as IASTInitializerClause)?.let {
                        templateArguments.add(it)
                    }
                }
                is IASTIdExpression -> {
                    // add to templateArguments
                    frontend.expressionHandler.handle(argument)?.let { templateArguments.add(it) }
                }
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
            handle(ctx.negativeResultExpression)
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
        castExpression.expression =
            handle(ctx.operand) ?: ProblemExpression("could not parse inner expression")
        castExpression.setCastOperator(ctx.operator)
        castExpression.castType = frontend.typeOf(ctx.typeId)

        if (isPrimitive(castExpression.castType) || ctx.operator == 4) {
            castExpression.type = castExpression.castType
        }

        return castExpression
    }

    /**
     * Translates a C/C++
     * [member access](https://en.cppreference.com/w/cpp/language/operator_member_access) into a
     * [MemberExpression].
     */
    private fun handleFieldReference(ctx: IASTFieldReference): Expression {
        val base = handle(ctx.fieldOwner) ?: return newProblemExpression("base of field is null")

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
            unknownType(),
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
                    if (frontend.typeManager.typeExists(typeName)) {
                        val cast = newCastExpression(frontend.codeOf(ctx))
                        cast.castType = frontend.typeOf((ctx.operand as IASTIdExpression).name)
                        // The expression member can only be filled by the parent call
                        // (handleFunctionCallExpression)
                        cast.location = frontend.locationOf(ctx)
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
        when {
            reference is MemberExpression -> {
                val baseType = reference.base.type.root
                assert(baseType !is SecondOrderType)
                callExpression = newMemberCallExpression(reference, code = ctx.rawSignature)
                if (
                    (ctx.functionNameExpression as? IASTFieldReference)?.fieldName
                        is CPPASTTemplateId
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
            }
            reference is BinaryOperator &&
                (reference.operatorCode == ".*" || reference.operatorCode == "->*") -> {
                // This is a function pointer call to a class method. We keep this as a binary
                // operator
                // with the .* or ->* operator code, so that we can resolve this later in the
                // FunctionPointerCallResolver
                callExpression = newMemberCallExpression(reference, code = ctx.rawSignature)
            }
            reference is UnaryOperator && reference.operatorCode == "*" -> {
                // Classic C-style function pointer call -> let's extract the target
                callExpression = newCallExpression(reference, "", reference.code, false)
            }
            ctx.functionNameExpression is IASTIdExpression &&
                (ctx.functionNameExpression as IASTIdExpression).name is CPPASTTemplateId -> {
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
            }
            reference is CastExpression -> {
                // this really is a cast expression in disguise
                reference.expression =
                    handle(ctx.arguments.first())
                        ?: ProblemExpression("could not parse argument for cast")
                return reference
            }
            else -> {
                callExpression =
                    newCallExpression(reference, reference?.name, ctx.rawSignature, false)
            }
        }

        for ((i, argument) in ctx.arguments.withIndex()) {
            val arg = handle(argument)
            arg?.let {
                it.argumentIndex = i
                callExpression.addArgument(it)
            }
        }

        // Important: we don't really need the reference node, but even its temporary creation might
        // leave unwanted artifacts behind in the final graph!
        reference?.disconnectFromGraph()
        return callExpression
    }

    private fun handleIdExpression(ctx: IASTIdExpression): DeclaredReferenceExpression {
        // this expression could actually be a field / member expression, but somehow CDT only
        // recognizes them as a member expression if it has an explicit 'this'
        // TODO: handle this? convert the declared reference expression into a member expression?
        return newDeclaredReferenceExpression(ctx.name.toString(), unknownType(), ctx.rawSignature)
    }

    private fun handleExpressionList(exprList: IASTExpressionList): ExpressionList {
        val expressionList = newExpressionList(exprList.rawSignature)
        for (expr in exprList.expressions) {
            handle(expr)?.let { expressionList.addExpression(it) }
        }
        return expressionList
    }

    private fun handleBinaryExpression(ctx: IASTBinaryExpression): Expression {
        val operatorCode =
            when (ctx.operator) {
                op_assign,
                op_multiplyAssign,
                op_divideAssign,
                op_moduloAssign,
                op_plusAssign,
                op_minusAssign,
                op_shiftLeftAssign,
                op_shiftRightAssign,
                op_binaryAndAssign,
                op_binaryXorAssign,
                op_binaryOrAssign -> {
                    return handleAssignment(ctx)
                }
                op_pmdot -> ".*"
                op_pmarrow -> "->*"
                else -> String(ASTStringUtil.getBinaryOperatorString(ctx))
            }

        val binaryOperator = newBinaryOperator(operatorCode, ctx.rawSignature)
        val lhs = handle(ctx.operand1) ?: newProblemExpression("could not parse lhs")
        val rhs =
            if (ctx.operand2 != null) {
                handle(ctx.operand2)
            } else {
                handle(ctx.initOperand2)
            }
                ?: newProblemExpression("could not parse rhs")
        binaryOperator.lhs = lhs
        binaryOperator.rhs = rhs

        return binaryOperator
    }

    private fun handleAssignment(ctx: IASTBinaryExpression): Expression {
        val lhs = handle(ctx.operand1) ?: newProblemExpression("missing LHS")
        val rhs =
            if (ctx.operand2 != null) {
                handle(ctx.operand2)
            } else {
                handle(ctx.initOperand2)
            }
                ?: newProblemExpression("missing RHS")

        val operatorCode = String(ASTStringUtil.getBinaryOperatorString(ctx))
        val assign = newAssignExpression(operatorCode, listOf(lhs), listOf(rhs), rawNode = ctx)
        if (rhs is UnaryOperator && rhs.input is DeclaredReferenceExpression) {
            (rhs.input as DeclaredReferenceExpression).resolutionHelper = lhs
        }

        return assign
    }

    private fun handleLiteralExpression(ctx: IASTLiteralExpression): Expression {
        return when (ctx.kind) {
            lk_integer_constant -> handleIntegerLiteral(ctx)
            lk_float_constant -> handleFloatLiteral(ctx)
            lk_char_constant -> newLiteral(ctx.value[1], primitiveType("char"), ctx.rawSignature)
            lk_string_literal ->
                newLiteral(
                    String(ctx.value.slice(IntRange(1, ctx.value.size - 2)).toCharArray()),
                    primitiveType("char").array(),
                    ctx.rawSignature
                )
            lk_this -> handleThisLiteral(ctx)
            lk_true -> newLiteral(true, primitiveType("bool"), ctx.rawSignature)
            lk_false -> newLiteral(false, primitiveType("bool"), ctx.rawSignature)
            lk_nullptr -> newLiteral(null, objectType("nullptr_t"), ctx.rawSignature)
            else -> newLiteral(String(ctx.value), unknownType(), ctx.rawSignature)
        }
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
                                unknownType(),
                                des.getRawSignature()
                            )
                    }
                    is CPPASTArrayRangeDesignator -> {
                        oneLhs =
                            newRangeExpression(
                                handle(des.rangeFloor),
                                handle(des.rangeCeiling),
                                des.getRawSignature()
                            )
                        oneLhs.operatorCode = "..."
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
                                unknownType(),
                                des.getRawSignature()
                            )
                    }
                    is CPPASTArrayRangeDesignator -> {
                        oneLhs =
                            newRangeExpression(
                                handle(des.rangeFloor),
                                handle(des.rangeCeiling),
                                des.getRawSignature()
                            )
                        oneLhs.operatorCode = "..."
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

    private fun handleIntegerLiteral(ctx: IASTLiteralExpression): Expression {
        var (strippedValue, suffix) = ctx.valueWithSuffix
        val bigValue: BigInteger

        // next, check for possible prefixes
        var radix = 10
        var offset = 0
        when {
            strippedValue.startsWith("0b") -> {
                radix = 2 // binary
                offset = 2 // len("0b")
            }
            strippedValue.startsWith("0x") -> {
                radix = 16 // hex
                offset = 2 // len("0x")
            }
            strippedValue.startsWith("0") && strippedValue.length > 1 -> {
                radix = 8 // octal
                offset = 1 // len("0")
            }
        }

        // strip the prefix
        strippedValue = strippedValue.substring(offset)

        // remove single quotes inside integer literal, used as a separator (C++14)
        strippedValue = strippedValue.replace("'", "")

        // basically we parse everything as BigInteger and then decide what to do
        try {
            bigValue = BigInteger(strippedValue, radix)
            val numberValue: Number =
                when {
                    "ull" == suffix || "ul" == suffix -> {
                        // unsigned long (long) will always be represented as BigInteger
                        bigValue
                    }
                    "ll" == suffix || "l" == suffix -> {
                        // both long and long long can be represented in Java long, but only within
                        // Long.MAX_VALUE
                        if (bigValue > BigInteger.valueOf(Long.MAX_VALUE)) {
                            Util.warnWithFileLocation(
                                frontend,
                                ctx,
                                log,
                                "Integer literal {} is too large to be represented in a signed type, interpreting it as unsigned.",
                                ctx
                            )
                            // keep it as BigInteger
                            bigValue
                        } else {
                            bigValue.toLong()
                        }
                    }
                    bigValue > BigInteger.valueOf(Long.MAX_VALUE) -> {
                        // No suffix, we just cast it to the appropriate signed type that is
                        // required, but
                        // only within Long.MAX_VALUE
                        Util.warnWithFileLocation(
                            frontend,
                            ctx,
                            log,
                            "Integer literal {} is too large to be represented in a signed type, interpreting it as unsigned.",
                            ctx
                        )
                        // keep it as BigInteger
                        bigValue
                    }
                    bigValue.toLong() > Int.MAX_VALUE -> {
                        bigValue.toLong()
                    }
                    else -> {
                        bigValue.toInt()
                    }
                }

            // retrieve type based on stored Java number
            val type =
                primitiveType(
                    when {
                        // we follow the way clang/llvm handles this and this seems to always
                        // be an unsigned long long, except if it is explicitly specified as ul
                        // differentiate between long and long long
                        numberValue is BigInteger && "ul" == suffix -> "unsigned long int"
                        numberValue is BigInteger -> "unsigned long long int"
                        numberValue is Long && "ll" == suffix -> "long long int"
                        numberValue is Long -> "long int"
                        else -> "int"
                    }
                )

            return newLiteral(numberValue, type, ctx.rawSignature)
        } catch (ex: NumberFormatException) {
            // It could be that we cannot parse the literal, in this case we return an error
            return ProblemExpression("could not parse literal: ${ex.message}")
        }
    }

    private fun handleFloatLiteral(ctx: IASTLiteralExpression): Expression {
        val (strippedValue, suffix) = ctx.valueWithSuffix

        return try {
            when (suffix) {
                "f" -> newLiteral(strippedValue.toFloat(), primitiveType("float"), ctx.rawSignature)
                "l" ->
                    newLiteral(
                        strippedValue.toBigDecimal(),
                        primitiveType("long double"),
                        ctx.rawSignature
                    )
                else ->
                    newLiteral(strippedValue.toDouble(), primitiveType("double"), ctx.rawSignature)
            }
        } catch (ex: NumberFormatException) {
            // It could be that we cannot parse the literal, in this case we return an error
            ProblemExpression("could not parse literal: ${ex.message}")
        }
    }

    /**
     * In C++, the "this" expression is also modelled as a literal. In our case however, we want to
     * return a [DeclaredReferenceExpression], which is then later connected to the current method's
     * [MethodDeclaration.receiver].
     */
    private fun handleThisLiteral(ctx: IASTLiteralExpression): DeclaredReferenceExpression {
        // We should be in a record here. However since we are a fuzzy parser, maybe things went
        // wrong, so we might have an unknown type.
        val recordType = frontend.scopeManager.currentRecord?.toType() ?: unknownType()
        // We do want to make sure that the type of the expression is at least a pointer.
        val pointerType = recordType.pointer()

        return newDeclaredReferenceExpression("this", pointerType, ctx.rawSignature, ctx)
    }

    private val IASTLiteralExpression.valueWithSuffix: Pair<String, String>
        get() {
            val value = String(this.value).lowercase(Locale.getDefault())

            var suffix = ""

            // Maximum suffix length is 3
            for (i in 1..3) {
                val digit = value.substring(max(0, value.length - i))
                suffix =
                    if (
                        digit.chars().allMatch {
                            // We need to match on different suffixes, based on the kind of literal
                            when (this.kind) {
                                // See https://en.cppreference.com/w/cpp/language/floating_literal
                                lk_float_constant -> it == 'f'.code || it == 'l'.code
                                lk_integer_constant -> it == 'u'.code || it == 'l'.code
                                else -> false
                            }
                        }
                    ) {
                        digit
                    } else {
                        break
                    }
            }

            // Supply the value with the suffix stripped, as well as the suffix
            return Pair(value.substring(0, value.length - suffix.length), suffix)
        }
}

private val IASTUnaryExpression.isPostfixOperator: Boolean
    get() {
        return this.operator == IASTUnaryExpression.op_postFixDecr ||
            this.operator == IASTUnaryExpression.op_postFixIncr
    }
