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

import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.TypeManager
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
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.math.max
import org.eclipse.cdt.core.dom.ast.*
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTExpression
import org.eclipse.cdt.internal.core.dom.parser.CStringValue
import org.eclipse.cdt.internal.core.dom.parser.ProblemBinding
import org.eclipse.cdt.internal.core.dom.parser.ProblemType
import org.eclipse.cdt.internal.core.dom.parser.cpp.*
import org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.TypeOfDependentExpression

class ExpressionHandler(lang: CXXLanguageFrontend) :
    Handler<Expression?, IASTInitializerClause, CXXLanguageFrontend>(
        Supplier { Expression() },
        lang
    ) {

    /*
    Note: CDT expresses hierarchies in Interfaces to allow to have multi-inheritance in java. Because some Expressions
    have sub elements of type IASTInitializerClause and in the hierarchy IASTExpression extends IASTInitializerClause.
    The later is the appropriate Interface type for the handler.
    */
    init {
        map[CPPASTLiteralExpression::class.java] = HandlerInterface { ctx: IASTInitializerClause ->
            handleLiteralExpression(ctx as CPPASTLiteralExpression)
        }
        map[CPPASTBinaryExpression::class.java] = HandlerInterface { ctx: IASTInitializerClause ->
            handleBinaryExpression(ctx as CPPASTBinaryExpression)
        }
        map[CPPASTUnaryExpression::class.java] = HandlerInterface { ctx: IASTInitializerClause ->
            handleUnaryExpression(ctx as CPPASTUnaryExpression)
        }
        map[CPPASTConditionalExpression::class.java] =
            HandlerInterface { ctx: IASTInitializerClause ->
                handleConditionalExpression(ctx as CPPASTConditionalExpression)
            }
        map[CPPASTIdExpression::class.java] = HandlerInterface { ctx: IASTInitializerClause ->
            handleIdExpression(ctx as CPPASTIdExpression)
        }
        map[CPPASTFieldReference::class.java] = HandlerInterface { ctx: IASTInitializerClause ->
            handleFieldReference(ctx as CPPASTFieldReference)
        }
        map[CPPASTFunctionCallExpression::class.java] =
            HandlerInterface { ctx: IASTInitializerClause ->
                handleFunctionCallExpression(ctx as CPPASTFunctionCallExpression)
            }
        map[CPPASTCastExpression::class.java] = HandlerInterface { ctx: IASTInitializerClause ->
            handleCastExpression(ctx as CPPASTCastExpression)
        }
        map[CPPASTSimpleTypeConstructorExpression::class.java] =
            HandlerInterface { ctx: IASTInitializerClause ->
                handleSimpleTypeConstructorExpression(ctx as CPPASTSimpleTypeConstructorExpression)
            }
        map[CPPASTNewExpression::class.java] = HandlerInterface { ctx: IASTInitializerClause ->
            handleNewExpression(ctx as CPPASTNewExpression)
        }
        map[CPPASTInitializerList::class.java] = HandlerInterface { ctx: IASTInitializerClause ->
            handleInitializerList(ctx as CPPASTInitializerList)
        }
        map[CPPASTDesignatedInitializer::class.java] =
            HandlerInterface { ctx: IASTInitializerClause ->
                handleDesignatedInitializer(ctx as CPPASTDesignatedInitializer)
            }
        map[CPPASTExpressionList::class.java] = HandlerInterface { ctx: IASTInitializerClause ->
            handleExpressionList(ctx as CPPASTExpressionList)
        }
        map[CPPASTDeleteExpression::class.java] = HandlerInterface { ctx: IASTInitializerClause ->
            handleDeleteExpression(ctx as CPPASTDeleteExpression)
        }
        map[CPPASTArraySubscriptExpression::class.java] =
            HandlerInterface { ctx: IASTInitializerClause ->
                handleArraySubscriptExpression(ctx as CPPASTArraySubscriptExpression)
            }
        map[CPPASTTypeIdExpression::class.java] = HandlerInterface { ctx: IASTInitializerClause ->
            handleTypeIdExpression(ctx as CPPASTTypeIdExpression)
        }
        map[CPPASTCompoundStatementExpression::class.java] =
            HandlerInterface { ctx: IASTInitializerClause ->
                handleCompoundStatementExpression(ctx as CPPASTCompoundStatementExpression)
            }
    }

    /**
     * Tries to return the [IType] for a given AST expression. In case this fails, the constant type
     * [ProblemType.UNKNOWN_FOR_EXPRESSION] is returned.
     *
     * @param expression the ast expression
     * @return a CDT type
     */
    private fun expressionTypeProxy(expression: ICPPASTExpression): IType {
        var expressionType = ProblemType.UNKNOWN_FOR_EXPRESSION
        try {
            expressionType = expression.expressionType
        } catch (e: AssertionError) {
            val codeFromRawNode = lang.getCodeFromRawNode(expression)
            Util.warnWithFileLocation(
                lang,
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
        val cse = NodeBuilder.newCompoundStatementExpression(ctx.rawSignature)
        cse.statement = lang.statementHandler.handle(ctx.compoundStatement)
        return cse
    }

    private fun handleTypeIdExpression(ctx: CPPASTTypeIdExpression): TypeIdExpression {
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
        var type: Type = UnknownType.getUnknownType()
        when (ctx.operator) {
            IASTTypeIdExpression.op_sizeof -> {
                operatorCode = "sizeof"
                type = TypeParser.createFrom("std::size_t", true)
            }
            IASTTypeIdExpression.op_typeid -> {
                operatorCode = "typeid"
                type = TypeParser.createFrom("const std::type_info&", true)
            }
            IASTTypeIdExpression.op_alignof -> {
                operatorCode = "alignof"
                type = TypeParser.createFrom("std::size_t", true)
            }
            IASTTypeIdExpression
                .op_typeof -> // typeof is not an official c++ keyword - not sure why eclipse
                // supports it
                operatorCode = "typeof"
            else -> log.debug("Unknown typeid operator code: {}", ctx.operator)
        }

        // TODO: proper type resolve
        val referencedType = TypeParser.createFrom(ctx.typeId.declSpecifier.toString(), true, lang)
        return NodeBuilder.newTypeIdExpression(operatorCode, type, referencedType, ctx.rawSignature)
    }

    private fun handleArraySubscriptExpression(ctx: CPPASTArraySubscriptExpression): Expression {
        val arraySubsExpression = NodeBuilder.newArraySubscriptionExpression(ctx.rawSignature)
        arraySubsExpression.arrayExpression = handle(ctx.arrayExpression)
        arraySubsExpression.subscriptExpression = handle(ctx.argument)
        return arraySubsExpression
    }

    private fun handleNewExpression(ctx: CPPASTNewExpression): Expression {
        val name = ctx.typeId.declSpecifier.toString()
        val code = ctx.rawSignature
        val t = TypeParser.createFrom(name, true, lang)
        val init = ctx.initializer

        // we need to check, whether this is an array initialization or a single new expression
        return if (ctx.isArrayAllocation) {
            t.reference(PointerOrigin.ARRAY)
            val arrayMods = (ctx.typeId.abstractDeclarator as IASTArrayDeclarator).arrayModifiers
            val arrayCreate = NodeBuilder.newArrayCreationExpression(code)
            arrayCreate.type = t
            for (arrayMod in arrayMods) {
                arrayCreate.addDimension(handle(arrayMod.constantExpression))
            }
            if (init != null) {
                arrayCreate.initializer = lang.initializerHandler.handle(init)
            }
            arrayCreate
        } else {
            // Resolve possible templates
            var templateParameters: List<Node?> = emptyList<Node>()
            val declSpecifier = ctx.typeId.declSpecifier
            if ((declSpecifier as CPPASTNamedTypeSpecifier).name is CPPASTTemplateId) {
                templateParameters = getTemplateArguments(declSpecifier.name as CPPASTTemplateId)
                assert(t.root is ObjectType)
                val objectType = t.root as ObjectType
                val generics =
                    templateParameters
                        .stream()
                        .filter { obj: Node? -> TypeExpression::class.java.isInstance(obj) }
                        .map { e: Node? -> (e as TypeExpression?)!!.type }
                        .collect(Collectors.toList())
                objectType.generics = generics
            }

            // new returns a pointer, so we need to reference the type by pointer
            val newExpression =
                NodeBuilder.newNewExpression(code, t.reference(PointerOrigin.POINTER))
            newExpression.templateParameters = templateParameters
            val initializer: Expression?
            if (init != null) {
                initializer = lang.initializerHandler.handle(init)
            } else {
                // in C++, it is possible to omit the `()` part, when creating an object, such as
                // `new A`.
                // Therefore CDT does not have an explicit construct expression, so we need create
                // an
                // implicit one
                initializer = NodeBuilder.newConstructExpression("()")
                initializer.isImplicit = true
            }

            // we also need to "forward" our template parameters (if we have any) to the construct
            // expression since the construct expression will do the actual template instantiation
            if (newExpression.templateParameters != null &&
                    newExpression.templateParameters.isNotEmpty()
            ) {
                CallResolver.addImplicitTemplateParametersToCall(
                    newExpression.templateParameters,
                    initializer as ConstructExpression?
                )
            }

            // our initializer, such as a construct expression, will have the non-pointer type
            initializer!!.type = t
            newExpression.initializer = initializer
            newExpression
        }
    }

    /**
     * Gets all arguments a template was instantiated with. Note, that the arguments can either be
     * Expressions referring to a value ot TypeExpressions referring to a type.
     *
     * @param template
     * @return List of Nodes containing the all the arguments the template was instantiated with.
     */
    private fun getTemplateArguments(template: CPPASTTemplateId): List<Node?> {
        val templateArguments: MutableList<Node?> = ArrayList()
        for (argument in template.templateArguments) {
            if (argument is CPPASTTypeId) {
                val type = TypeParser.createFrom(argument.declSpecifier.toString(), true)
                templateArguments.add(NodeBuilder.newTypeExpression(type.name, type))
            } else if (argument is CPPASTLiteralExpression) {
                templateArguments.add(
                    lang.expressionHandler.handle(argument as IASTInitializerClause)
                )
            }
        }
        return templateArguments
    }

    private fun handleConditionalExpression(
        ctx: CPPASTConditionalExpression
    ): ConditionalExpression {
        val condition = handle(ctx.logicalConditionExpression)
        return NodeBuilder.newConditionalExpression(
            condition,
            if (ctx.positiveResultExpression != null) handle(ctx.positiveResultExpression)
            else condition,
            handle(ctx.negativeResultExpression),
            TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, lang)
        )
    }

    private fun handleDeleteExpression(ctx: CPPASTDeleteExpression): DeleteExpression {
        val deleteExpression = NodeBuilder.newDeleteExpression(ctx.rawSignature)
        for (name in ctx.implicitDestructorNames) {
            log.debug("Implicit constructor name {}", name)
        }
        deleteExpression.operand = handle(ctx.operand)
        return deleteExpression
    }

    private fun handleCastExpression(ctx: CPPASTCastExpression): Expression {
        val castExpression = NodeBuilder.newCastExpression(ctx.rawSignature)
        castExpression.expression = handle(ctx.operand)
        castExpression.setCastOperator(ctx.operator)
        val castType: Type
        val iType = expressionTypeProxy(ctx)
        castType =
            if (iType is CPPPointerType) {
                if (iType.type is IProblemType) {
                    // fall back to fTypeId
                    TypeParser.createFrom(ctx.typeId.declSpecifier.toString() + "*", true, lang)
                } else {
                    TypeParser.createFrom(iType.type.toString() + "*", true, lang)
                }
            } else if (iType is IProblemType) {
                // fall back to fTypeId
                TypeParser.createFrom(ctx.typeId.declSpecifier.toString(), true, lang)
                // TODO: try to actually resolve the type (similar to NewExpression) using
                // ((CPPASTNamedTypeSpecifier) declSpecifier).getName().resolveBinding()
            } else {
                TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, lang)
            }
        castExpression.castType = castType
        if (TypeManager.getInstance().isPrimitive(castExpression.castType) || ctx.operator == 4) {
            castExpression.type = castExpression.castType
        } else {
            castExpression.expression.registerTypeListener(castExpression)
        }
        return castExpression
    }

    private fun handleSimpleTypeConstructorExpression(
        ctx: CPPASTSimpleTypeConstructorExpression
    ): Expression {
        val castExpression = NodeBuilder.newCastExpression(ctx.rawSignature)
        castExpression.expression = lang.initializerHandler.handle(ctx.initializer)
        castExpression.setCastOperator(0) // cast

        val castType: Type =
            if (expressionTypeProxy(ctx) is CPPPointerType) {
                val pointerType = expressionTypeProxy(ctx) as CPPPointerType
                TypeParser.createFrom(pointerType.type.toString() + "*", true, lang)
            } else {
                TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, lang)
            }

        castExpression.castType = castType
        if (TypeManager.getInstance().isPrimitive(castExpression.castType)) {
            castExpression.type = castExpression.castType
        } else {
            castExpression.expression.registerTypeListener(castExpression)
        }
        return castExpression
    }

    private fun handleFieldReference(ctx: CPPASTFieldReference): Expression {
        var base = handle(ctx.fieldOwner)
        // Replace Literal this with a reference pointing to this
        if (base is Literal<*> && base.value == "this") {
            val location = base.location
            val recordDeclaration = lang.scopeManager.currentRecord
            base =
                NodeBuilder.newDeclaredReferenceExpression(
                    "this",
                    if (recordDeclaration != null) recordDeclaration.getThis().type
                    else UnknownType.getUnknownType(),
                    base.code
                )
            base.location = location
        }
        return NodeBuilder.newMemberExpression(
            base,
            UnknownType.getUnknownType(),
            ctx.fieldName.toString(),
            if (ctx.isPointerDereference) "->" else ".",
            ctx.rawSignature
        )
    }

    private fun handleUnaryExpression(ctx: CPPASTUnaryExpression): Expression? {
        var input: Expression? = null
        if (ctx.operand != null) { // can be null e.g. for "throw;"
            input = handle(ctx.operand)
        }
        var operatorCode = ""
        when (ctx.operator) {
            IASTUnaryExpression.op_prefixIncr, IASTUnaryExpression.op_postFixIncr ->
                operatorCode = "++"
            IASTUnaryExpression.op_prefixDecr, IASTUnaryExpression.op_postFixDecr ->
                operatorCode = "--"
            IASTUnaryExpression.op_plus -> operatorCode = "+"
            IASTUnaryExpression.op_minus -> operatorCode = "-"
            IASTUnaryExpression.op_star -> operatorCode = "*"
            IASTUnaryExpression.op_amper -> operatorCode = "&"
            IASTUnaryExpression.op_tilde -> operatorCode = "~"
            IASTUnaryExpression.op_not -> operatorCode = "!"
            IASTUnaryExpression.op_sizeof -> operatorCode = "sizeof"
            IASTUnaryExpression.op_bracketedPrimary -> {
                if (lang.config.inferenceConfiguration.guessCastExpressions) {
                    // this can either be just a meaningless bracket or it can be a cast expression
                    if (ctx.operand is CPPASTIdExpression) {
                        val typeName = (ctx.operand as CPPASTIdExpression).name.toString()
                        if (TypeManager.getInstance().typeExists(typeName)) {
                            val cast =
                                NodeBuilder.newCastExpression(lang.getCodeFromRawNode<Any>(ctx))
                            cast.castType = TypeParser.createFrom(typeName, false)
                            cast.expression = input
                            cast.location = lang.getLocationFromRawNode<Any>(ctx)
                            return cast
                        }
                    }
                }

                // otherwise, ignore this kind of expression and return the input directly
                return input
            }
            IASTUnaryExpression.op_throw -> operatorCode = "throw"
            IASTUnaryExpression.op_typeid -> operatorCode = "typeid"
            IASTUnaryExpression.op_alignOf -> operatorCode = "alignof"
            IASTUnaryExpression.op_sizeofParameterPack -> operatorCode = "sizeof..."
            IASTUnaryExpression.op_noexcept -> operatorCode = "noexcept"
            IASTUnaryExpression.op_labelReference -> operatorCode = ""
            else -> Util.errorWithFileLocation(lang, ctx, log, "unknown operator {}", ctx.operator)
        }
        val unaryOperator =
            NodeBuilder.newUnaryOperator(
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

    private fun handleFunctionCallExpression(ctx: CPPASTFunctionCallExpression): Expression {
        val reference = handle(ctx.functionNameExpression)
        val callExpression: CallExpression
        if (reference is MemberExpression) {
            val baseTypename: String
            // Pointer types contain * or []. We do not want that here.
            val baseType: Type = reference.base.type.root
            assert(baseType !is SecondOrderType)
            baseTypename = baseType.typeName
            val member =
                NodeBuilder.newDeclaredReferenceExpression(
                    reference.name,
                    UnknownType.getUnknownType(),
                    reference.name
                )
            member.location = lang.getLocationFromRawNode<Expression>(reference)
            callExpression =
                NodeBuilder.newMemberCallExpression(
                    member.name,
                    baseTypename + "." + member.name,
                    reference.base,
                    member,
                    reference.operatorCode,
                    ctx.rawSignature
                )
            if ((ctx.functionNameExpression as? CPPASTFieldReference)?.fieldName is CPPASTTemplateId
            ) {
                // Make necessary adjustments if we are handling a function template
                val name =
                    ((ctx.functionNameExpression as CPPASTFieldReference).fieldName as
                            CPPASTTemplateId)
                        .templateName.toString()
                callExpression.name = name
                callExpression.addExplicitTemplateParameters(
                    getTemplateArguments(
                        (ctx.functionNameExpression as CPPASTFieldReference).fieldName as
                            CPPASTTemplateId
                    )
                )
            }
        } else if (reference is BinaryOperator && reference.operatorCode == ".") {
            // We have a dot operator that was not classified as a member expression. This happens
            // when dealing with function pointer calls that happen on an explicit object
            callExpression =
                NodeBuilder.newMemberCallExpression(
                    ctx.functionNameExpression.rawSignature,
                    "",
                    reference.lhs,
                    reference.rhs,
                    reference.operatorCode,
                    ctx.rawSignature
                )
        } else if (reference is UnaryOperator && reference.operatorCode == "*") {
            // Classic C-style function pointer call -> let's extract the target
            callExpression =
                NodeBuilder.newCallExpression(reference.input.name, "", reference.code, false)
        } else if (ctx.functionNameExpression is CPPASTIdExpression &&
                (ctx.functionNameExpression as CPPASTIdExpression).name is CPPASTTemplateId
        ) {
            val name =
                ((ctx.functionNameExpression as CPPASTIdExpression).name as CPPASTTemplateId)
                    .templateName.toString()
            callExpression = NodeBuilder.newCallExpression(name, name, ctx.rawSignature, true)
            callExpression.addExplicitTemplateParameters(
                getTemplateArguments(
                    (ctx.functionNameExpression as CPPASTIdExpression).name as CPPASTTemplateId
                )
            )
        } else if (reference is CastExpression) {
            // this really is a cast expression in disguise
            return reference
        } else {
            var fqn = reference!!.name
            var name = fqn
            if (name.contains("::")) {
                name = name.substring(name.lastIndexOf("::") + 2)
            }
            if (name.contains("<")) {
                // The characters < and > are not allowed in identifier names, as they denote the
                // usage of a
                // template
                name = name.substring(0, name.indexOf("<"))
            }
            fqn = fqn.replace("::", ".")
            // FIXME this is only true if we are in a namespace! If we are in a class, this is
            // wrong!
            //  happens again in l367
            // String fullNamePrefix = lang.getScopeManager().getFullNamePrefix();
            // if (!fullNamePrefix.isEmpty()) {
            //  fqn = fullNamePrefix + "." + fqn;
            // }
            callExpression = NodeBuilder.newCallExpression(name, fqn, ctx.rawSignature, false)
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

    private fun handleIdExpression(ctx: CPPASTIdExpression): DeclaredReferenceExpression {
        val declaredReferenceExpression =
            NodeBuilder.newDeclaredReferenceExpression(
                ctx.name.toString(),
                UnknownType.getUnknownType(),
                ctx.rawSignature
            )
        val proxy = expressionTypeProxy(ctx)
        if (proxy is CPPClassInstance) {
            // Handle Template Types separately
            handleTemplateTypeOfDeclaredReferenceExpression(proxy, declaredReferenceExpression)
        } else if (proxy !is TypeOfDependentExpression) {
            declaredReferenceExpression.type =
                TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, lang)
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
            TypeParser.createFrom(
                (proxy as CPPClassInstance).templateDefinition.toString(),
                true
            ) as
                ObjectType
        for (templateArgument in proxy.templateArguments) {
            if (templateArgument is CPPTemplateTypeArgument) {
                type.addGeneric(TypeParser.createFrom(templateArgument.toString(), true))
            }
        }
        declaredReferenceExpression.type = type
    }

    private fun handleExpressionList(exprList: CPPASTExpressionList): ExpressionList {
        val expressionList = NodeBuilder.newExpressionList(exprList.rawSignature)
        for (expr in exprList.expressions) {
            expressionList.addExpression(handle(expr))
        }
        return expressionList
    }

    private fun handleBinaryExpression(ctx: CPPASTBinaryExpression): BinaryOperator {
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
            IASTBinaryExpression.op_pmdot -> operatorCode = "."
            IASTBinaryExpression.op_pmarrow -> operatorCode = "->"
            IASTBinaryExpression.op_max -> operatorCode = ">?"
            IASTBinaryExpression.op_min -> operatorCode = "?<"
            IASTBinaryExpression.op_ellipses -> operatorCode = "..."
            else -> Util.errorWithFileLocation(lang, ctx, log, "unknown operator {}", ctx.operator)
        }
        val binaryOperator = NodeBuilder.newBinaryOperator(operatorCode, ctx.rawSignature)
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
            is ProblemType, is ProblemBinding -> {
                log.trace("CDT could not deduce type. Type is set to null")
            }
            is TypeOfDependentExpression -> {
                log.debug("Type of Expression depends on the type the template is initialized with")
                binaryOperator.type = UnknownType.getUnknownType()
            }
            else -> {
                binaryOperator.type =
                    TypeParser.createFrom(expressionTypeProxy(ctx).toString(), true, lang)
            }
        }

        return binaryOperator
    }

    private fun handleLiteralExpression(ctx: CPPASTLiteralExpression): Literal<*> {
        val type = expressionTypeProxy(ctx)
        val value = ctx.evaluation.value
        val generatedType = TypeParser.createFrom(type.toString(), true, lang)
        if (value.numberValue() == null // e.g. for 0x1p-52
            && value !is CStringValue
        ) {
            return NodeBuilder.newLiteral(value.toString(), generatedType, ctx.rawSignature)
        }
        if (type is CPPBasicType && type.kind == IBasicType.Kind.eInt) {
            return handleIntegerLiteral(ctx)
        } else if (type.isSameType(CPPBasicType.BOOLEAN)) {
            return NodeBuilder.newLiteral(
                value.numberValue().toInt() == 1,
                generatedType,
                ctx.rawSignature
            )
        } else if (value is CStringValue) {
            return NodeBuilder.newLiteral(value.cStringValue(), generatedType, ctx.rawSignature)
        } else if (type is CPPBasicType && type.kind == IBasicType.Kind.eFloat) {
            return NodeBuilder.newLiteral(
                value.numberValue().toFloat(),
                generatedType,
                ctx.rawSignature
            )
        } else if (type is CPPBasicType && type.kind == IBasicType.Kind.eDouble) {
            return NodeBuilder.newLiteral(
                value.numberValue().toDouble(),
                generatedType,
                ctx.rawSignature
            )
        } else if (type is CPPBasicType && type.kind == IBasicType.Kind.eChar) {
            return NodeBuilder.newLiteral(
                value.numberValue().toInt().toChar(),
                generatedType,
                ctx.rawSignature
            )
        }
        return NodeBuilder.newLiteral(value.toString(), generatedType, ctx.rawSignature)
    }

    private fun handleInitializerList(ctx: CPPASTInitializerList): InitializerListExpression {
        val expression = NodeBuilder.newInitializerListExpression(ctx.rawSignature)

        for (clause in ctx.clauses) {
            handle(clause)?.let {
                val edge = PropertyEdge(expression, it)
                edge.addProperty(Properties.INDEX, expression.initializersPropertyEdge.size)

                expression.initializersPropertyEdge.add(edge)
            }
        }

        return expression
    }

    private fun handleDesignatedInitializer(
        ctx: CPPASTDesignatedInitializer
    ): DesignatedInitializerExpression {
        val rhs = handle(ctx.operand)
        val lhs = ArrayList<Expression>()
        if (ctx.designators.isEmpty()) {
            Util.errorWithFileLocation(lang, ctx, log, "no designator found")
        } else {
            for (des in ctx.designators) {
                var oneLhs: Expression? = null
                when (des) {
                    is CPPASTArrayDesignator -> {
                        oneLhs = handle(des.subscriptExpression)
                    }
                    is CPPASTFieldDesignator -> {
                        oneLhs =
                            NodeBuilder.newDeclaredReferenceExpression(
                                des.name.toString(),
                                UnknownType.getUnknownType(),
                                des.getRawSignature()
                            )
                    }
                    is CPPASTArrayRangeDesignator -> {
                        oneLhs =
                            NodeBuilder.newArrayRangeExpression(
                                handle(des.rangeFloor),
                                handle(des.rangeCeiling),
                                des.getRawSignature()
                            )
                    }
                    else -> {
                        Util.errorWithFileLocation(
                            lang,
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

        val die = NodeBuilder.newDesignatedInitializerExpression(ctx.rawSignature)
        die.lhs = lhs
        die.rhs = rhs

        return die
    }

    private fun handleIntegerLiteral(ctx: CPPASTLiteralExpression): Literal<*> {
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
                    lang,
                    ctx,
                    log,
                    "Integer literal {} is too large to represented in a signed type, interpreting it as unsigned.",
                    ctx
                )
            } else {
                numberValue = bigValue.toLong()
            }
        } else {
            // No suffix, we just cast it to the appropriate signed type that is required, but only
            // within Long.MAX_VALUE
            if (bigValue > BigInteger.valueOf(Long.MAX_VALUE)) {
                // keep it as BigInteger
                numberValue = bigValue
                Util.warnWithFileLocation(
                    lang,
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
        }

        // retrieve type based on stored Java number
        val type =
            if (numberValue is BigInteger) {
                // we follow the way clang/llvm handles this and this seems to always
                // be an unsigned long long, except if it is explicitly specified as ul
                if ("ul" == suffix) TypeParser.createFrom("unsigned long", true)
                else TypeParser.createFrom("unsigned long long", true)
            } else if (numberValue is Long) {
                // differentiate between long and long long
                if ("ll" == suffix) TypeParser.createFrom("long long", true)
                else TypeParser.createFrom("long", true)
            } else {
                TypeParser.createFrom("int", true)
            }

        return NodeBuilder.newLiteral(numberValue, type, ctx.rawSignature)
    }

    private val String.suffix: String
        get() {
            var suffix = ""

            // maximum suffix length is 3
            for (i in 1..3) {
                val digit = this.substring(max(0, this.length - i))
                suffix =
                    if (digit.chars().allMatch { character: Int ->
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
