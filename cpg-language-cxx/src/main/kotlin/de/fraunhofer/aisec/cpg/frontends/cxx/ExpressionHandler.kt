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
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.SymbolResolver.Companion.addImplicitTemplateParametersToCall
import java.math.BigInteger
import java.util.*
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.pow
import org.eclipse.cdt.core.dom.ast.*
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression.*
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression.*
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLambda
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTCompoundStatementExpression
import org.eclipse.cdt.internal.core.dom.parser.c.CASTArrayDesignator
import org.eclipse.cdt.internal.core.dom.parser.c.CASTArrayRangeDesignator
import org.eclipse.cdt.internal.core.dom.parser.c.CASTDesignatedInitializer
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFieldDesignator
import org.eclipse.cdt.internal.core.dom.parser.c.CASTTypeIdInitializerExpression
import org.eclipse.cdt.internal.core.dom.parser.cpp.*
import org.eclipse.cdt.internal.core.model.ASTStringUtil

/**
 * Note: CDT expresses hierarchies in Interfaces to allow to have multi-inheritance in java. Because
 * some Expressions have sub elements of type IASTInitializerClause and in the hierarchy
 * IASTExpression extends IASTInitializerClause. The later is the appropriate Interface type for the
 * handler.
 */
class ExpressionHandler(lang: CXXLanguageFrontend) :
    CXXHandler<Expression, IASTNode>(Supplier(::ProblemExpression), lang) {

    override fun handleNode(node: IASTNode): Expression {
        return when (node) {
            is IASTLiteralExpression -> handleLiteralExpression(node)
            is IASTBinaryExpression -> handleBinaryExpression(node)
            is IASTUnaryExpression -> handleUnaryExpression(node)
            is IASTConditional -> handleConditional(node)
            is IASTIdExpression -> handleIdExpression(node)
            is IASTFieldReference -> handleFieldReference(node)
            is IASTFunctionCall -> handleFunctionCall(node)
            is IASTCast -> handleCast(node)
            is IASTExpressionList -> handleExpressionList(node)
            is IASTInitializerList ->
                frontend.initializerHandler.handle(node)
                    ?: ProblemExpression("could not parse initializer list")
            is IASTArraySubscript -> handleArraySubscript(node)
            is IASTTypeId -> handleTypeReference(node)
            is IGNUASTCompoundStatementExpression -> handleCompoundStatementExpression(node)
            is CPPASTNew -> handleNew(node)
            is CPPASTDesignatedInitializer -> handleCXXDesignatedInitializer(node)
            is CASTDesignatedInitializer -> handleCDesignatedInitializer(node)
            is CASTTypeIdInitializerExpression -> handleTypeIdInitializerExpression(node)
            is CPPASTDelete -> handleDelete(node)
            is CPPASTLambda -> handleLambda(node)
            is CPPASTSimpleTypeConstructorExpression -> handleSimpleTypeConstructorExpression(node)
            else -> {
                handleNotSupported(node, node.javaClass.name)
            }
        }
    }

    /**
     * This handles a [CPPASTSimpleTypeConstructorExpression], which handles all cases of
     * [Explicit type conversion](https://en.cppreference.com/w/cpp/language/explicit_cast).
     * Depending on the case, we either handle this as a [Cast] or a [Construction].
     */
    private fun handleSimpleTypeConstructorExpression(
        node: CPPASTSimpleTypeConstructorExpression
    ): Expression {
        return if (node.declSpecifier is IASTSimpleDeclSpecifier) {
            val cast = newCast(rawNode = node)
            cast.castType = frontend.typeOf(node.declSpecifier)

            // The actual expression that is cast is nested in an initializer. We could forward
            // this to our initializer handler, but this would create a lot of construct expressions
            // just for simple type casts, which we want to avoid, so we take a shortcut and do a
            // direct unwrapping here.
            val single =
                (node.initializer as? ICPPASTConstructorInitializer)?.arguments?.singleOrNull()
            cast.expression =
                single?.let { handle(it) } ?: newProblemExpression("could not parse initializer")
            cast
        } else {
            // Otherwise, we try to parse it as an initializer, which must either be an initializer
            // list expression or a constructor initializer
            val initializer = frontend.initializerHandler.handle(node.initializer)
            if (initializer is InitializerList) {
                val construct = newConstruction(rawNode = node)
                construct.arguments = initializer.initializers
                construct
            } else initializer ?: newProblemExpression("could not parse initializer")
        }
    }

    private fun handleLambda(node: CPPASTLambda): Expression {
        val lambda = newLambda(rawNode = node)

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
                        frontend.scopeManager
                            .lookupSymbolByName(
                                newName(capture.identifier?.toString() ?: ""),
                                language = language,
                            ) {
                                it is ValueDeclaration
                            }
                            .singleOrNull() as? ValueDeclaration
                    valueDeclaration?.let { lambda.mutableVariables.add(it) }
                }
            }
        }

        // By default, the outer variables passed by value to the lambda are immutable. But we can
        // either make the function "mutable" or pass everything by reference.
        lambda.areVariablesMutable =
            (node.declarator as? CPPASTFunctionDeclarator)?.isMutable == true ||
                node.captureDefault == ICPPASTLambda.CaptureDefault.BY_REFERENCE

        val anonymousFunction =
            node.declarator?.let { frontend.declaratorHandler.handle(it) as? Function }
                ?: newFunction("lambda${lambda.hashCode()}")
        anonymousFunction.type = computeType(anonymousFunction)

        frontend.scopeManager.enterScope(anonymousFunction)
        anonymousFunction.body = frontend.statementHandler.handle(node.body)
        frontend.scopeManager.leaveScope(anonymousFunction)

        lambda.function = anonymousFunction

        return lambda
    }

    private fun handleCompoundStatementExpression(
        ctx: IGNUASTCompoundStatementExpression
    ): Expression {
        return frontend.statementHandler.handle(ctx.compoundStatement) as Expression
    }

    private fun handleTypeReference(ctx: IASTTypeId): TypeReference {
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
            IASTTypeId.op_sizeof -> {
                operatorCode = "sizeof"
                type = objectType("std::size_t")
            }
            IASTTypeId.op_typeid -> {
                operatorCode = "typeid"
                type = objectType("std::type_info").ref()
            }
            IASTTypeId.op_alignof -> {
                operatorCode = "alignof"
                type = objectType("std::size_t")
            }
            IASTTypeId.op_typeof -> // typeof is not an official c++ keyword - not sure why eclipse
                // supports it
                operatorCode = "typeof"
            else -> log.debug("Unknown typeid operator code: {}", ctx.operator)
        }

        val referencedType = frontend.typeOf(ctx.typeId)
        return newTypeReference(operatorCode, type, referencedType, rawNode = ctx)
    }

    private fun handleArraySubscript(ctx: IASTArraySubscript): Expression {
        val arraySubsExpression = newSubscription(rawNode = ctx)
        handle(ctx.arrayExpression)?.let { arraySubsExpression.arrayExpression = it }
        handle(ctx.argument)?.let { arraySubsExpression.subscriptExpression = it }
        return arraySubsExpression
    }

    private fun handleNew(ctx: CPPASTNew): Expression {
        val t = frontend.typeOf(ctx.typeId)
        val init = ctx.initializer

        // we need to check, whether this is an array initialization or a single new expression
        return if (ctx.isArrayAllocation) {
            t.array()
            val arrayMods = (ctx.typeId.abstractDeclarator as IASTArrayDeclarator).arrayModifiers
            val arrayCreate = newArrayConstruction(rawNode = ctx)
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
            val newExpression = newNew(type = t.pointer(), rawNode = ctx)
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
                initializer =
                    newConstruction(t.name.localName).implicit(code = "${t.name.localName}()")
                initializer.type = t
            }

            // we also need to "forward" our template parameters (if we have any) to the construct
            // expression since the construct expression will do the actual template instantiation
            if (newExpression.templateParameters.isNotEmpty()) {
                addImplicitTemplateParametersToCall(
                    newExpression.templateParameters,
                    initializer as Construction,
                )
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
    private fun getTemplateArguments(template: CPPASTTemplateId): MutableList<AstNode> {
        val templateArguments = mutableListOf<AstNode>()
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

    private fun handleConditional(ctx: IASTConditional): Conditional {
        val condition =
            handle(ctx.logicalConditionExpression)
                ?: ProblemExpression("could not parse condition expression")
        return newConditional(
            condition,
            if (ctx.positiveResultExpression != null) handle(ctx.positiveResultExpression)
            else condition,
            handle(ctx.negativeResultExpression),
        )
    }

    private fun handleDelete(ctx: CPPASTDelete): Delete {
        val deleteExpression = newDelete(rawNode = ctx)
        for (name in ctx.implicitDestructorNames) {
            log.debug("Implicit constructor name {}", name)
        }
        handle(ctx.operand)?.let { deleteExpression.operands.add(it) }
        return deleteExpression
    }

    private fun handleCast(ctx: IASTCast): Expression {
        val castExpression = newCast(rawNode = ctx)
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
     * [MemberAccess].
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

        return newMemberAccess(
            name,
            base,
            unknownType(),
            if (ctx.isPointerDereference) "->" else ".",
            rawNode = ctx,
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
                // If this expression is NOT part of a call expression and contains a "name" or
                // something similar, we want to keep the information that this is an expression
                // wrapped in parentheses. The best way to do this is to create a unary expression
                if (ctx.operand is IASTIdExpression && ctx.parent !is IASTFunctionCall) {
                    val op = newUnaryOperator("()", postfix = true, prefix = true, rawNode = ctx)
                    if (input != null) {
                        op.input = input
                    }
                    return op
                }

                // In all other cases, e.g., if the parenthesis is nested or part of a function
                // call, we just return the unwrapped expression, because in this case we do not
                // need to information about the parenthesis.
                return input as Expression
            }
            IASTUnaryExpression.op_throw ->
                return newThrow(rawNode = ctx).apply { this.exception = input }
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
                rawNode = ctx,
            )
        if (input != null) {
            unaryOperator.input = input
        }
        return unaryOperator
    }

    private fun handleFunctionCall(ctx: IASTFunctionCall): Expression {
        val reference = handle(ctx.functionNameExpression)
        val callExpression: Call
        when {
            reference is MemberAccess -> {
                callExpression = newMemberCall(reference, rawNode = ctx)
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
                // operator with the .* or ->* operator code, so that we can resolve this later in
                // the
                // FunctionPointerCallResolver
                callExpression = newMemberCall(reference, rawNode = ctx)
            }
            reference is UnaryOperator && reference.operatorCode == "*" -> {
                // Classic C-style function pointer call -> let's extract the target
                callExpression = newCall(reference, "", false, rawNode = ctx)
            }
            ctx.functionNameExpression is IASTIdExpression &&
                (ctx.functionNameExpression as IASTIdExpression).name is CPPASTTemplateId -> {
                val name =
                    ((ctx.functionNameExpression as IASTIdExpression).name as CPPASTTemplateId)
                        .templateName
                        .toString()
                val ref = newReference(name)
                callExpression = newCall(ref, name, template = true, rawNode = ctx)
                getTemplateArguments(
                        (ctx.functionNameExpression as IASTIdExpression).name as CPPASTTemplateId
                    )
                    .forEach { callExpression.addTemplateParameter(it) }
            }
            else -> {
                callExpression =
                    newCall(reference, reference?.name, template = false, rawNode = ctx)
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

    private fun handleIdExpression(ctx: IASTIdExpression): Expression {
        val name = ctx.name.toString()

        // In order to avoid many inferred/unresolved symbols, we want to make sure that we convert
        // NULL into a proper null-literal, regardless whether headers are included or not.
        if (name == "NULL") {
            return if (language is CPPLanguage) {
                newLiteral(null, objectType("std::nullptr_t"), rawNode = ctx)
            } else {
                newLiteral(0, unknownType(), rawNode = ctx)
            }
        }

        // Handle pre-defined expressions
        if (name == "__FUNCTION__" || name == "__func__") {
            return newLiteral(
                frontend.scopeManager.currentFunction?.name?.localName ?: "",
                primitiveType("char").pointer(),
                rawNode = ctx,
            )
        } else if (name == "__PRETTY_FUNCTION__") {
            // This is not 100 % compatible with CLANG, but this is ok for now
            return newLiteral(
                frontend.scopeManager.currentFunction?.signature ?: "",
                primitiveType("char").pointer(),
                rawNode = ctx,
            )
        }

        // this expression could actually be a field / member expression, but somehow CDT only
        // recognizes them as a member expression if it has an explicit 'this'
        // TODO: handle this? convert the declared reference expression into a member expression?
        return newReference(name, unknownType(), rawNode = ctx)
    }

    private fun handleExpressionList(exprList: IASTExpressionList): ExpressionList {
        val expressionList = newExpressionList(rawNode = exprList)
        for (expr in exprList.expressions) {
            handle(expr)?.let { expressionList.expressions += it }
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

        val binaryOperator = newBinaryOperator(operatorCode, rawNode = ctx)
        val lhs = handle(ctx.operand1) ?: newProblemExpression("could not parse lhs")
        val rhs =
            if (ctx.operand2 != null) {
                handle(ctx.operand2)
            } else {
                handle(ctx.initOperand2)
            } ?: newProblemExpression("could not parse rhs")

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
            } ?: newProblemExpression("missing RHS")

        val operatorCode = String(ASTStringUtil.getBinaryOperatorString(ctx))
        val assign = newAssign(operatorCode, listOf(lhs), listOf(rhs), rawNode = ctx)
        if (rhs is UnaryOperator && rhs.input is Reference) {
            (rhs.input as Reference).resolutionHelper = lhs
        }

        return assign
    }

    private fun handleLiteralExpression(ctx: IASTLiteralExpression): Expression {
        return when (ctx.kind) {
            lk_integer_constant -> handleIntegerLiteral(ctx)
            lk_float_constant -> handleFloatLiteral(ctx)
            lk_char_constant -> handleCharLiteral(ctx)
            lk_string_literal ->
                newLiteral(
                    String(ctx.value.slice(IntRange(1, ctx.value.size - 2)).toCharArray()),
                    primitiveType("char").array(),
                    rawNode = ctx,
                )
            lk_this -> handleThisLiteral(ctx)
            lk_true -> newLiteral(true, primitiveType("bool"), rawNode = ctx)
            lk_false -> newLiteral(false, primitiveType("bool"), rawNode = ctx)
            lk_nullptr -> newLiteral(null, objectType("nullptr_t"), rawNode = ctx)
            else -> newLiteral(String(ctx.value), unknownType(), rawNode = ctx)
        }
    }

    var escapeMap =
        mapOf(
            'a' to Char(0x07),
            'b' to Char(0x08),
            'f' to Char(0x0c),
            'n' to Char(0x0a),
            'r' to Char(0x0d),
            't' to Char(0x09),
            'v' to Char(0x0b),
            '\\' to Char(0x5c),
            '\'' to Char(0x27),
            '\"' to Char(0x22),
            '?' to Char(0x3f),
        )

    private fun handleCharLiteral(ctx: IASTLiteralExpression): Expression {
        var raw = String(ctx.value)
        if (!raw.startsWith("'") || !raw.endsWith("'")) {
            return newProblemExpression(
                "character literal does not start or end with '",
                rawNode = ctx,
            )
        }

        raw = raw.trim('\'')

        // Since C/C++ for some reason allows multi-character, we need to parse character by
        // character and then see what the final type is
        var i = 0
        val chars = mutableListOf<Char>()
        var escapeChars = ""
        var radix = 10
        var inEscape = false
        var maxChars: Int? = null
        while (i < raw.length) {
            // Check, if we are in escape mode, then we need to gather the escape chars
            if (inEscape) {
                // Check for radix specifier
                if (escapeChars.isEmpty() && raw[i] == 'x') {
                    radix = 16
                    maxChars =
                        2 // it seems like most compilers only allow two hex digits here, so do we
                    i++
                    continue
                }

                // Check, if new escape char. Then finish this one and start a new one
                if (raw[i] == '\\') {
                    try {
                        chars += Char(escapeChars.toInt(radix))
                        // Restart, assuming its octal and wait for a new radix specifier
                        escapeChars = ""
                        inEscape = true
                        maxChars = 3
                        radix = 8

                        // Go to the next digit
                        i++
                        continue
                    } catch (ex: NumberFormatException) {
                        return newProblemExpression("invalid number: ${ex.message}", rawNode = ctx)
                    }
                }

                // Check for special escape (they are only one digit and we NOT in hex mode)
                val specialEscape = escapeMap[raw[i]]
                if (escapeChars.isEmpty() && radix != 16 && specialEscape != null) {
                    chars += specialEscape
                    escapeChars = ""
                    inEscape = false
                    maxChars = null
                } else {
                    // Otherwise, we need to collect digits (up to a certain max)
                    escapeChars += raw[i]

                    // There are multiple ways to end the sequence:
                    // - If this is the last char of the whole string
                    // - If max digits are reached
                    // - If another escape character is there
                    if (
                        i == raw.length - 1 || (maxChars != null && escapeChars.length >= maxChars)
                    ) {
                        try {
                            chars += Char(escapeChars.toInt(radix))
                            escapeChars = ""
                            inEscape = false
                            maxChars = 0
                        } catch (ex: NumberFormatException) {
                            return newProblemExpression(
                                "invalid number: ${ex.message}",
                                rawNode = ctx,
                            )
                        }
                    }
                }
            } else {
                if (raw[i] == '\\') {
                    // Switch into escape mode
                    inEscape = true

                    // If no other specifier is there, we can have a maximum of three digits
                    maxChars = 3

                    // Radix is octal
                    radix = 8
                } else {
                    // Handle regular character
                    chars += raw[i]
                }
            }
            i++
        }

        val single = chars.singleOrNull()
        if (single != null) {
            return newLiteral(single, primitiveType("char"), rawNode = ctx)
        } else {
            // Somehow make an int out of, this is "implementation" specific. We follow the way
            // clang does it
            var intValue = 0
            for ((n, c) in chars.reversed().withIndex()) {
                intValue += (c.code * 256.0f.pow(n)).toInt()
            }
            return newLiteral(intValue, primitiveType("int"), rawNode = ctx)
        }
    }

    private fun handleCXXDesignatedInitializer(ctx: CPPASTDesignatedInitializer): Expression {
        val rhs = handle(ctx.operand)

        // We need to check the first designator first
        val des = ctx.designators.firstOrNull()
        if (des == null) {
            Util.errorWithFileLocation(frontend, ctx, log, "no designator found")
            return newProblemExpression("no designator found")
        }

        // We need to start with our target (which we need to find in a hacky way) as
        // first ref
        val baseName =
            (((ctx.parent as? IASTInitializerList)?.parent as? IASTInitializer)?.parent
                    as? IASTDeclarator)
                ?.name
                .toString()
        var ref = newReference(baseName)

        val lhs =
            when (des) {
                is CPPASTArrayDesignator -> {
                    val sub = newSubscription()
                    sub.arrayExpression = ref
                    handle(des.subscriptExpression)?.let { sub.subscriptExpression = it }
                    sub
                }
                is CPPASTFieldDesignator -> {
                    // Then we loop through all designators and chain them. Only field designators
                    // can be chained in this way
                    for (field in
                        ctx.designators.toList().filterIsInstance<CPPASTFieldDesignator>()) {
                        // the old ref is our new base
                        ref = newMemberAccess(field.name.toString(), ref, rawNode = field)
                    }
                    ref
                }
                else -> {
                    Util.errorWithFileLocation(
                        frontend,
                        ctx,
                        log,
                        "Unknown designated lhs {}",
                        des.javaClass.toGenericString(),
                    )
                    null
                }
            }

        return newAssign(lhs = listOfNotNull(lhs), rhs = listOfNotNull(rhs), rawNode = ctx)
    }

    private fun handleCDesignatedInitializer(ctx: CASTDesignatedInitializer): Expression {
        val rhs = handle(ctx.operand)

        // We need to check the first designator first
        val des = ctx.designators.firstOrNull()
        if (des == null) {
            Util.errorWithFileLocation(frontend, ctx, log, "no designator found")
            return newProblemExpression("no designator found")
        }

        // We need to start with our target (which we need to find in a hacky way) as
        // first ref
        val baseName =
            (((ctx.parent as? IASTInitializerList)?.parent as? IASTInitializer)?.parent
                    as? IASTDeclarator)
                ?.name
                .toString()
        var ref = newReference(baseName)

        val lhs =
            when (des) {
                is CASTArrayDesignator -> {
                    val sub = newSubscription(rawNode = des)
                    sub.arrayExpression = ref
                    handle(des.subscriptExpression)?.let { sub.subscriptExpression = it }
                    sub
                }
                is CASTArrayRangeDesignator -> {
                    val sub = newSubscription(rawNode = des)
                    sub.arrayExpression = ref

                    val range = newRange(rawNode = des)
                    des.rangeFloor?.let { range.floor = handle(it) }
                    des.rangeCeiling?.let { range.ceiling = handle(it) }
                    range.operatorCode = "..."
                    sub.subscriptExpression = range
                    sub
                }
                is CASTFieldDesignator -> {
                    // Then we loop through all designators and chain them. Only field designators
                    // can be chained in this way
                    for (field in
                        ctx.designators.toList().filterIsInstance<CASTFieldDesignator>()) {
                        // the old ref is our new base
                        ref = newMemberAccess(field.name.toString(), ref, rawNode = field)
                    }
                    ref
                }
                else -> {
                    Util.errorWithFileLocation(
                        frontend,
                        ctx,
                        log,
                        "Unknown designated lhs {}",
                        des.javaClass.toGenericString(),
                    )
                    null
                }
            }

        return newAssign(lhs = listOfNotNull(lhs), rhs = listOfNotNull(rhs), rawNode = ctx)
    }

    private fun handleTypeIdInitializerExpression(
        ctx: CASTTypeIdInitializerExpression
    ): Construction {
        val type = frontend.typeOf(ctx.typeId)

        val construct = newConstruction(type.name, rawNode = ctx)

        // The only supported initializer is an initializer list
        (ctx.initializer as? IASTInitializerList)?.let {
            construct.arguments =
                it.clauses
                    .map { handle(it) ?: newProblemExpression("could not parse argument") }
                    .toMutableList()
        }

        return construct
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
                                ctx,
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
                            ctx,
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
                    when (numberValue) {
                        // we follow the way clang/llvm handles this and this seems to always
                        // be an unsigned long long, except if it is explicitly specified as ul
                        // differentiate between long and long long
                        is BigInteger if "ul" == suffix -> "unsigned long int"
                        is BigInteger -> "unsigned long long int"
                        is Long if "ll" == suffix -> "long long int"
                        is Long -> "long int"
                        else -> "int"
                    }
                )

            return newLiteral(numberValue, type, rawNode = ctx)
        } catch (ex: NumberFormatException) {
            // It could be that we cannot parse the literal, in this case we return an error
            return ProblemExpression("could not parse literal: ${ex.message}")
        }
    }

    private fun handleFloatLiteral(ctx: IASTLiteralExpression): Expression {
        val (strippedValue, suffix) = ctx.valueWithSuffix

        return try {
            when (suffix) {
                "f" -> newLiteral(strippedValue.toFloat(), primitiveType("float"), rawNode = ctx)
                "l" ->
                    newLiteral(
                        strippedValue.toBigDecimal(),
                        primitiveType("long double"),
                        rawNode = ctx,
                    )
                else -> newLiteral(strippedValue.toDouble(), primitiveType("double"), rawNode = ctx)
            }
        } catch (ex: NumberFormatException) {
            // It could be that we cannot parse the literal, in this case we return an error
            ProblemExpression("could not parse literal: ${ex.message}")
        }
    }

    /**
     * In C++, the "this" expression is also modeled as a literal. In our case however, we want to
     * return a [Reference], which is then later connected to the current method's
     * [Method.receiver].
     */
    private fun handleThisLiteral(ctx: IASTLiteralExpression): Reference {
        // We should be in a record here. However since we are a fuzzy parser, maybe things went
        // wrong, so we might have an unknown type.
        val recordType = frontend.scopeManager.currentRecord?.toType() ?: unknownType()
        // We do want to make sure that the type of the expression is at least a pointer.
        val pointerType = recordType.pointer()

        return newReference("this", pointerType, rawNode = ctx)
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
