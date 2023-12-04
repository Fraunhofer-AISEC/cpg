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
package de.fraunhofer.aisec.cpg.frontends.java

import com.github.javaparser.Range
import com.github.javaparser.TokenRange
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.ArrayCreationExpr
import com.github.javaparser.ast.expr.ArrayInitializerExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.CharLiteralExpr
import com.github.javaparser.ast.expr.ClassExpr
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.InstanceOfExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.NullLiteralExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.expr.SuperExpr
import com.github.javaparser.ast.expr.ThisExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.resolution.UnsolvedSymbolException
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HandlerInterface
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ConditionalExpression
import de.fraunhofer.aisec.cpg.graph.types.*
import java.util.function.Supplier
import kotlin.collections.set
import kotlin.jvm.optionals.getOrNull
import org.slf4j.LoggerFactory

class ExpressionHandler(lang: JavaLanguageFrontend) :
    Handler<Statement, Expression, JavaLanguageFrontend>(Supplier { ProblemExpression() }, lang) {

    private fun handleLambdaExpr(expr: Expression): Statement {
        val lambdaExpr = expr.asLambdaExpr()
        val lambda = newLambdaExpression(rawNode = lambdaExpr)
        val anonymousFunction = newFunctionDeclaration("", rawNode = lambdaExpr)
        frontend.scopeManager.enterScope(anonymousFunction)
        for (parameter in lambdaExpr.parameters) {
            val resolvedType = frontend.getTypeAsGoodAsPossible(parameter.type)
            val param =
                newParameterDeclaration(parameter.nameAsString, resolvedType, parameter.isVarArgs)
            anonymousFunction.addParameter(param)
            frontend.setCodeAndLocation(param, parameter)
            frontend.processAnnotations(param, parameter)
            frontend.scopeManager.addDeclaration(param)
        }

        // TODO: We cannot easily identify the signature of the lambda
        // val type = lambdaExpr.calculateResolvedType()
        val functionType = FunctionType.computeType(anonymousFunction)
        anonymousFunction.type = functionType
        anonymousFunction.body = frontend.statementHandler.handle(lambdaExpr.body)
        frontend.scopeManager.leaveScope(anonymousFunction)

        lambda.function = anonymousFunction

        return lambda
    }

    private fun handleCastExpr(expr: Expression): Statement {
        val castExpr = expr.asCastExpr()
        val castExpression = newCastExpression(rawNode = expr)
        val expression =
            handle(castExpr.expression)
                as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
                ?: newProblemExpression("could not parse expression")
        castExpression.expression = expression
        castExpression.setCastOperator(2)
        val t = frontend.getTypeAsGoodAsPossible(castExpr.type)
        castExpression.castType = t
        if (castExpr.type.isPrimitiveType) {
            // Set Type based on the Casting type as it will result in a conversion for primitive
            // types
            castExpression.type = frontend.typeOf(castExpr.type.resolve().asPrimitive())
        } else {
            // Get Runtime type from cast expression for complex types;
            // castExpression.expression.registerTypeListener(castExpression)
        }
        return castExpression
    }

    /**
     * Creates a new [NewArrayExpression], which is usually used as an initializer of a
     * [VariableDeclaration].
     *
     * @param expr the expression
     * @return the [NewArrayExpression]
     */
    private fun handleArrayCreationExpr(expr: Expression): Statement {
        val arrayCreationExpr = expr as ArrayCreationExpr
        val creationExpression = newNewArrayExpression(rawNode = expr)

        // in Java, an array creation expression either specifies an initializer or dimensions

        // parse initializer, if present
        arrayCreationExpr.initializer.ifPresent {
            creationExpression.initializer = handle(it) as? InitializerListExpression
        }

        // dimensions are only present if you specify them explicitly, such as new int[1]
        for (lvl in arrayCreationExpr.levels) {
            lvl.dimension.ifPresent {
                (handle(it) as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?)
                    ?.let { creationExpression.addDimension(it) }
            }
        }
        return creationExpression
    }

    private fun handleArrayInitializerExpr(expr: Expression): Statement {
        val arrayInitializerExpr = expr as ArrayInitializerExpr

        // We need to go back to the parent to get the array type
        val arrayType =
            when (val parent = expr.parentNode.getOrNull()) {
                is ArrayCreationExpr -> frontend.typeOf(parent.elementType).array()
                is VariableDeclarator -> frontend.typeOf(parent.type)
                else -> unknownType()
            }

        // ArrayInitializerExpressions are converted into InitializerListExpressions to reduce the
        // syntactic distance a CPP and JAVA CPG
        val initList = newInitializerListExpression(arrayType, rawNode = expr)
        val initializers =
            arrayInitializerExpr.values
                .map { handle(it) }
                .map {
                    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression::class
                        .java
                        .cast(it)
                }
        initList.initializers = initializers
        return initList
    }

    private fun handleArrayAccessExpr(expr: Expression): SubscriptExpression {
        val arrayAccessExpr = expr as ArrayAccessExpr
        val arraySubsExpression = newSubscriptExpression(rawNode = expr)
        (handle(arrayAccessExpr.name)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?)
            ?.let { arraySubsExpression.arrayExpression = it }

        (handle(arrayAccessExpr.index)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?)
            ?.let { arraySubsExpression.subscriptExpression = it }

        return arraySubsExpression
    }

    private fun handleEnclosedExpression(expr: Expression): Statement? {
        return handle((expr as EnclosedExpr).inner)
    }

    private fun handleConditionalExpression(expr: Expression): ConditionalExpression {
        val conditionalExpr = expr.asConditionalExpr()
        val superType: Type =
            try {
                frontend.typeOf(conditionalExpr.calculateResolvedType())
            } catch (e: RuntimeException) {
                val s = frontend.recoverTypeFromUnsolvedException(e)
                if (s != null) {
                    this.objectType(s)
                } else {
                    unknownType()
                }
            } catch (e: NoClassDefFoundError) {
                val s = frontend.recoverTypeFromUnsolvedException(e)
                if (s != null) {
                    this.objectType(s)
                } else {
                    unknownType()
                }
            }
        val condition =
            handle(conditionalExpr.condition)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
                ?: newProblemExpression("Could not parse condition")
        val thenExpr =
            handle(conditionalExpr.thenExpr)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
        val elseExpr =
            handle(conditionalExpr.elseExpr)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
        return newConditionalExpression(condition, thenExpr, elseExpr, superType)
    }

    private fun handleAssignmentExpression(expr: Expression): AssignExpression {
        val assignExpr = expr.asAssignExpr()

        // first, handle the target. this is the first argument of the operator call
        val lhs =
            handle(assignExpr.target)
                as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
                ?: newProblemExpression("could not parse lhs")

        // second, handle the value. this is the second argument of the operator call
        val rhs =
            handle(assignExpr.value)
                as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
                ?: newProblemExpression("could not parse lhs")
        val assign =
            newAssignExpression(
                assignExpr.operator.asString(),
                listOf(lhs),
                listOf(rhs),
                rawNode = assignExpr
            )

        return assign
    }

    // Not sure how to handle this exactly yet
    private fun handleVariableDeclarationExpr(expr: Expression): DeclarationStatement {
        val variableDeclarationExpr = expr.asVariableDeclarationExpr()
        val declarationStatement = newDeclarationStatement()
        for (variable in variableDeclarationExpr.variables) {
            val declaration = frontend.declarationHandler.handleVariableDeclarator(variable)
            frontend.setCodeAndLocation(declaration, variable)
            declarationStatement.addToPropertyEdgeDeclaration(declaration)
            frontend.processAnnotations(declaration, variableDeclarationExpr)
            frontend.scopeManager.addDeclaration(declaration)
        }
        return declarationStatement
    }

    private fun handleFieldAccessExpression(
        expr: Expression
    ): de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression {
        val fieldAccessExpr = expr.asFieldAccessExpr()
        var base: de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        // first, resolve the scope. this adds the necessary nodes, such as IDENTIFIER for the
        // scope.
        // it also acts as the first argument of the operator call
        val scope = fieldAccessExpr.scope
        if (scope.isNameExpr) {
            var isStaticAccess = false
            var baseType: Type
            try {
                val resolve = fieldAccessExpr.resolve()
                if (resolve.asField().isStatic) {
                    isStaticAccess = true
                }
                baseType = this.objectType(resolve.asField().declaringType().qualifiedName)
            } catch (ex: RuntimeException) {
                isStaticAccess = true
                val typeString = frontend.recoverTypeFromUnsolvedException(ex)
                if (typeString != null) {
                    baseType = this.objectType(typeString)
                } else {
                    // try to get the name
                    val name: String
                    val tokenRange = scope.asNameExpr().tokenRange
                    name =
                        if (tokenRange.isPresent) {
                            tokenRange.get().toString()
                        } else {
                            scope.asNameExpr().nameAsString
                        }
                    val qualifiedNameFromImports = frontend.getQualifiedNameFromImports(name)
                    baseType =
                        if (qualifiedNameFromImports != null) {
                            this.objectType(qualifiedNameFromImports)
                        } else {
                            log.info("Unknown base type 1 for {}", fieldAccessExpr)
                            unknownType()
                        }
                }
            } catch (ex: NoClassDefFoundError) {
                isStaticAccess = true
                val typeString = frontend.recoverTypeFromUnsolvedException(ex)
                if (typeString != null) {
                    baseType = this.objectType(typeString)
                } else {
                    val name: String
                    val tokenRange = scope.asNameExpr().tokenRange
                    name =
                        if (tokenRange.isPresent) {
                            tokenRange.get().toString()
                        } else {
                            scope.asNameExpr().nameAsString
                        }
                    val qualifiedNameFromImports = frontend.getQualifiedNameFromImports(name)
                    baseType =
                        if (qualifiedNameFromImports != null) {
                            this.objectType(qualifiedNameFromImports)
                        } else {
                            log.info("Unknown base type 1 for {}", fieldAccessExpr)
                            unknownType()
                        }
                }
            }
            base = newReference(scope.asNameExpr().nameAsString, baseType, rawNode = scope)
            base.isStaticAccess = isStaticAccess
            frontend.setCodeAndLocation(base, fieldAccessExpr.scope)
        } else if (scope.isFieldAccessExpr) {
            base =
                handle(scope) as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
                    ?: newProblemExpression("Could not parse base")
            var tester = base
            while (tester is MemberExpression) {
                // we need to check if any base is only a static access, otherwise, this is a member
                // access
                // to this base
                tester = tester.base
            }
            if (tester is Reference && tester.isStaticAccess) {
                // try to get the name
                val name: String
                val tokenRange = scope.asFieldAccessExpr().tokenRange
                name =
                    if (tokenRange.isPresent) {
                        tokenRange.get().toString()
                    } else {
                        scope.asFieldAccessExpr().nameAsString
                    }
                val qualifiedNameFromImports = frontend.getQualifiedNameFromImports(name)
                val baseType =
                    if (qualifiedNameFromImports != null) {
                        this.objectType(qualifiedNameFromImports)
                    } else {
                        log.info("Unknown base type 2 for {}", fieldAccessExpr)
                        unknownType()
                    }
                base =
                    newReference(scope.asFieldAccessExpr().nameAsString, baseType, rawNode = scope)
                base.isStaticAccess = true
            }
            frontend.setCodeAndLocation(base, fieldAccessExpr.scope)
        } else {
            base =
                handle(scope) as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
                    ?: newProblemExpression("Could not parse base")
        }
        var fieldType: Type?
        try {
            val symbol = fieldAccessExpr.resolve()
            fieldType =
                frontend.typeManager.getTypeParameter(
                    frontend.scopeManager.currentRecord,
                    symbol.asField().type.describe()
                )
            if (fieldType == null) {
                fieldType = frontend.typeOf(symbol.asField().type)
            }
        } catch (ex: RuntimeException) {
            val typeString = frontend.recoverTypeFromUnsolvedException(ex)
            fieldType =
                if (typeString != null) {
                    this.objectType(typeString)
                } else if (fieldAccessExpr.toString().endsWith(".length")) {
                    this.primitiveType("int")
                } else {
                    log.info("Unknown field type for {}", fieldAccessExpr)
                    unknownType()
                }
            val memberExpression =
                newMemberExpression(
                    fieldAccessExpr.name.identifier,
                    base,
                    fieldType,
                    "." // there is only "." in java
                )
            memberExpression.isStaticAccess = true
            return memberExpression
        } catch (ex: NoClassDefFoundError) {
            val typeString = frontend.recoverTypeFromUnsolvedException(ex)
            fieldType =
                if (typeString != null) {
                    this.objectType(typeString)
                } else if (fieldAccessExpr.toString().endsWith(".length")) {
                    this.primitiveType("int")
                } else {
                    log.info("Unknown field type for {}", fieldAccessExpr)
                    unknownType()
                }
            val memberExpression =
                newMemberExpression(fieldAccessExpr.name.identifier, base, fieldType, ".")
            memberExpression.isStaticAccess = true
            return memberExpression
        }
        if (base.location == null) {
            base.location = frontend.locationOf(fieldAccessExpr)
        }
        return newMemberExpression(fieldAccessExpr.name.identifier, base, fieldType, ".")
    }

    private fun handleLiteralExpression(expr: Expression): Literal<*>? {
        val literalExpr = expr.asLiteralExpr()
        val value = literalExpr.toString()
        return when (literalExpr) {
            is IntegerLiteralExpr ->
                newLiteral(
                    literalExpr.asIntegerLiteralExpr().asNumber(),
                    this.primitiveType("int"),
                    rawNode = expr
                )
            is StringLiteralExpr ->
                newLiteral(
                    literalExpr.asStringLiteralExpr().asString(),
                    this.primitiveType("java.lang.String"),
                    rawNode = expr
                )
            is BooleanLiteralExpr ->
                newLiteral(
                    literalExpr.asBooleanLiteralExpr().value,
                    this.primitiveType("boolean"),
                    rawNode = expr
                )
            is CharLiteralExpr ->
                newLiteral(
                    literalExpr.asCharLiteralExpr().asChar(),
                    this.primitiveType("char"),
                    rawNode = expr
                )
            is DoubleLiteralExpr ->
                newLiteral(
                    literalExpr.asDoubleLiteralExpr().asDouble(),
                    this.primitiveType("double"),
                    rawNode = expr
                )
            is LongLiteralExpr ->
                newLiteral(
                    literalExpr.asLongLiteralExpr().asNumber(),
                    this.primitiveType("long"),
                    rawNode = expr
                )
            is NullLiteralExpr -> newLiteral(null, this.objectType("null"), rawNode = expr)
            else -> null
        }
    }

    private fun handleClassExpression(expr: Expression): Reference {
        val classExpr = expr.asClassExpr()
        val type = frontend.typeOf(classExpr.type)
        val thisExpression =
            newReference(
                classExpr.toString().substring(classExpr.toString().lastIndexOf('.') + 1),
                type,
                rawNode = expr
            )
        thisExpression.isStaticAccess = true
        frontend.setCodeAndLocation(thisExpression, classExpr)
        return thisExpression
    }

    private fun handleThisExpression(expr: Expression): Reference {
        val thisExpr = expr.asThisExpr()
        val resolvedValueDeclaration = thisExpr.resolve()
        val type = this.objectType(resolvedValueDeclaration.qualifiedName)
        var name = thisExpr.toString()

        // If the typeName is specified, then this a "qualified this" and we need to handle it
        // carefully. Basically, we are simulating the behaviour of the java compiler, in which the
        // qualified this refers to a hidden field called "this$n", where n is the n'th enclosing
        // outer
        // class. Since we do not want to count, we replace the number with the simple class name.
        val typeName = thisExpr.typeName
        if (typeName.isPresent) {
            name = "this$" + typeName.get().identifier
        }
        val thisExpression = newReference(name, type, rawNode = expr)
        frontend.setCodeAndLocation(thisExpression, thisExpr)
        return thisExpression
    }

    private fun handleSuperExpression(expr: Expression): Reference {
        // The actual type is hard to determine at this point, as we may not have full information
        // about the inheritance structure. Thus, we delay the resolving to the variable resolving
        // process
        val superExpression = newReference(expr.toString(), unknownType(), rawNode = expr)
        frontend.setCodeAndLocation(superExpression, expr)
        return superExpression
    }

    // TODO: this function needs a MAJOR overhaul!
    private fun handleNameExpression(
        expr: Expression
    ): de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression? {
        val nameExpr = expr.asNameExpr()

        // TODO this commented code breaks field accesses to fields that don't have a primitive
        // type.
        //  How should this be handled correctly?
        //    try {
        //      ResolvedType resolvedType = nameExpr.calculateResolvedType();
        //      if (resolvedType.isReferenceType()) {
        //        return newReference(
        //            nameExpr.getNameAsString(),
        //            new Type(((ReferenceTypeImpl) resolvedType).getQualifiedName()),
        //            nameExpr.toString());
        //      }
        //    } catch (
        //        UnsolvedSymbolException
        //            e) { // this might throw, e.g. if the type is simply not defined (i.e., syntax
        // error)
        //      return newReference(
        //          nameExpr.getNameAsString(), new Type(UNKNOWN_TYPE), nameExpr.toString());
        //    }
        val name = this.parseName(nameExpr.nameAsString)
        return try {
            val symbol = nameExpr.resolve()
            if (symbol.isField) {
                val field = symbol.asField()
                if (!field.isStatic) {
                    // convert to FieldAccessExpr
                    val fieldAccessExpr = FieldAccessExpr(ThisExpr(), field.name)
                    expr.range.ifPresent { range: Range? -> fieldAccessExpr.setRange(range) }
                    expr.tokenRange.ifPresent { tokenRange: TokenRange? ->
                        fieldAccessExpr.setTokenRange(tokenRange)
                    }
                    expr.parentNode.ifPresent { newParentNode: Node? ->
                        fieldAccessExpr.setParentNode(newParentNode)
                    }
                    expr.replace(fieldAccessExpr)
                    fieldAccessExpr.parentNode.ifPresent { newParentNode: Node? ->
                        expr.setParentNode(newParentNode)
                    }

                    // handle it as a field expression
                    handle(fieldAccessExpr)
                        as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
                } else {
                    val fieldAccessExpr =
                        FieldAccessExpr(NameExpr(field.declaringType().className), field.name)
                    expr.range.ifPresent { range: Range? -> fieldAccessExpr.setRange(range) }
                    expr.tokenRange.ifPresent { tokenRange: TokenRange? ->
                        fieldAccessExpr.setTokenRange(tokenRange)
                    }
                    expr.parentNode.ifPresent { newParentNode: Node? ->
                        fieldAccessExpr.setParentNode(newParentNode)
                    }
                    expr.replace(fieldAccessExpr)
                    fieldAccessExpr.parentNode.ifPresent { newParentNode: Node? ->
                        expr.setParentNode(newParentNode)
                    }

                    // handle it as a field expression
                    handle(fieldAccessExpr)
                        as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
                }
            } else {
                // Resolve type first with ParameterizedType
                var type: Type? =
                    frontend.typeManager.getTypeParameter(
                        frontend.scopeManager.currentRecord,
                        symbol.type.describe()
                    )
                if (type == null) {
                    type = frontend.typeOf(symbol.type)
                }
                newReference(symbol.name, type, rawNode = nameExpr)
            }
        } catch (ex: UnsolvedSymbolException) {
            val typeString: String? =
                if (
                    ex.name.startsWith(
                        "We are unable to find the value declaration corresponding to"
                    )
                ) {
                    nameExpr.nameAsString
                } else {
                    frontend.recoverTypeFromUnsolvedException(ex)
                }
            val t: Type
            if (typeString == null) {
                t = unknownType()
            } else {
                t = this.objectType(typeString)
                t.typeOrigin = Type.Origin.GUESSED
            }
            val declaredReferenceExpression = newReference(name, t, rawNode = nameExpr)
            val recordDeclaration = frontend.scopeManager.currentRecord
            if (recordDeclaration != null && recordDeclaration.name.lastPartsMatch(name)) {
                declaredReferenceExpression.refersTo = recordDeclaration
            }
            declaredReferenceExpression
        } catch (ex: RuntimeException) {
            val t = unknownType()
            log.info("Unresolved symbol: {}", nameExpr.nameAsString)
            newReference(nameExpr.nameAsString, t, rawNode = nameExpr)
        } catch (ex: NoClassDefFoundError) {
            val t = unknownType()
            log.info("Unresolved symbol: {}", nameExpr.nameAsString)
            newReference(nameExpr.nameAsString, t, rawNode = nameExpr)
        }
    }

    private fun handleInstanceOfExpression(expr: Expression): BinaryOperator {
        val binaryExpr = expr.asInstanceOfExpr()

        // first, handle the target. this is the first argument of the operator callUnresolved
        // symbol
        val lhs =
            handle(binaryExpr.expression)
                as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
                ?: newProblemExpression("could not parse lhs")
        val typeAsGoodAsPossible = frontend.getTypeAsGoodAsPossible(binaryExpr.type)

        // second, handle the value. this is the second argument of the operator call
        val rhs: de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression =
            newLiteral(
                typeAsGoodAsPossible.typeName,
                this.objectType("class"),
                rawNode = binaryExpr
            )
        val binaryOperator = newBinaryOperator("instanceof", rawNode = binaryExpr)
        binaryOperator.lhs = lhs
        binaryOperator.rhs = rhs
        return binaryOperator
    }

    private fun handleUnaryExpression(expr: Expression): UnaryOperator {
        val unaryExpr = expr.asUnaryExpr()

        // handle the 'inner' expression, which is affected by the unary expression
        val expression =
            handle(unaryExpr.expression)
                as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
                ?: newProblemExpression("could not parse input")
        val unaryOperator =
            newUnaryOperator(
                unaryExpr.operator.asString(),
                unaryExpr.isPostfix,
                unaryExpr.isPrefix,
                rawNode = unaryExpr
            )
        unaryOperator.input = expression
        return unaryOperator
    }

    private fun handleBinaryExpression(expr: Expression): BinaryOperator {
        val binaryExpr = expr.asBinaryExpr()

        // first, handle the target. this is the first argument of the operator call
        val lhs =
            handle(binaryExpr.left)
                as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
                ?: newProblemExpression("could not parse lhs")

        // second, handle the value. this is the second argument of the operator call
        val rhs =
            handle(binaryExpr.right)
                as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
                ?: newProblemExpression("could not parse rhs")
        val binaryOperator = newBinaryOperator(binaryExpr.operator.asString(), rawNode = binaryExpr)
        binaryOperator.lhs = lhs
        binaryOperator.rhs = rhs
        return binaryOperator
    }

    private fun handleMethodCallExpression(expr: Expression): CallExpression {
        val methodCallExpr = expr.asMethodCallExpr()
        val callExpression: CallExpression
        val o = methodCallExpr.scope
        val qualifiedName = frontend.getQualifiedMethodNameAsGoodAsPossible(methodCallExpr)
        var name = qualifiedName
        if (name.contains(".")) {
            name = name.substring(name.lastIndexOf('.') + 1)
        }
        var typeString: String? = null
        var isStatic = false
        var resolved: ResolvedMethodDeclaration? = null
        try {
            // try resolving the method to learn more about it
            resolved = methodCallExpr.resolve()
            isStatic = resolved.isStatic
            typeString = resolved.returnType.describe()
        } catch (ignored: NoClassDefFoundError) {
            // Unfortunately, JavaParser also throws a simple RuntimeException instead of an
            // UnsolvedSymbolException within resolve() if it fails to resolve it under certain
            // circumstances, we catch all that and continue on our own
            log.debug("Could not resolve method {}", methodCallExpr)
        } catch (ignored: RuntimeException) {
            log.debug("Could not resolve method {}", methodCallExpr)
        }

        // Or if the base is a reference to an import
        if (frontend.getQualifiedNameFromImports(qualifiedName) != null) {
            isStatic = true
        }
        val base: de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        // the scope could either be a variable or also the class name (static call!)
        // thus, only because the scope is present, this is not automatically a member call
        if (o.isPresent) {
            val scope = o.get()
            base =
                handle(scope) as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
                    ?: newProblemExpression("Could not parse base")

            // If the base directly refers to a record, then this is a static call
            if (base is Reference && base.refersTo is RecordDeclaration) {
                isStatic = true
            }
        } else {
            // If the call does not have any base, there are two possibilities:
            // a) The "this" could be omitted, making it a member call to the current class
            // b) It could refer to some method that was statically imported using an asterisks
            // import
            //
            // Luckily, the resolved method already hints whether this is a static call
            if (isStatic) {
                // In case this is a static call, we need some additional information from the
                // resolved
                // method
                val baseType: Type
                val baseName =
                    if (resolved != null) {
                        this.parseName(resolved.declaringType().qualifiedName)
                    } else {
                        this.parseName(qualifiedName).parent
                    }
                baseType = this.objectType(baseName ?: Type.UNKNOWN_TYPE_STRING)
                base = newReference(baseName, baseType)
            } else {
                // Since it is possible to omit the "this" keyword, some methods in java do not have
                // a base.
                // However, they are still scoped to the local class, meaning we should insert an
                // implicit
                // "this" reference, to make the life for our call resolver easier.
                base = createImplicitThis()
            }
        }
        val member = newMemberExpression(name, base, unknownType(), ".")
        frontend.setCodeAndLocation(
            member,
            methodCallExpr.name
        ) // This will also overwrite the code set to the empty string set above
        callExpression = newMemberCallExpression(member, isStatic, rawNode = expr)
        callExpression.type = typeString?.let { this.objectType(it) } ?: unknownType()
        val arguments = methodCallExpr.arguments

        // handle the arguments
        for (i in arguments.indices) {
            val argument =
                handle(arguments[i])
                    as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
            argument?.argumentIndex = i
            callExpression.addArgument(
                argument ?: newProblemExpression("Could not parse the argument")
            )
        }
        return callExpression
    }

    /**
     * Creates an implicit "this" reference and already connects it to the current method receiver.
     *
     * @return the "this" reference expression
     */
    private fun createImplicitThis():
        de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression {
        val base: de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        val thisType =
            (frontend.scopeManager.currentFunction as MethodDeclaration?)?.receiver?.type
                ?: unknownType()
        base = newReference("this", thisType).implicit("this")
        return base
    }

    private fun handleObjectCreationExpr(expr: Expression): NewExpression {
        val objectCreationExpr = expr.asObjectCreationExpr()

        // scope refers to the constructor arguments
        val o = objectCreationExpr.scope
        if (o.isPresent) {
            // TODO: what to do with it?
            log.warn("Scope {}", o)
        }

        val t = frontend.getTypeAsGoodAsPossible(objectCreationExpr.type)
        val constructorName = t.name.localName

        // To be consistent with other languages, we need to create a NewExpression (for the "new X"
        // part) as well as a ConstructExpression (for the constructor call)
        val newExpression = newNewExpression(t, rawNode = expr)
        val arguments = objectCreationExpr.arguments

        val ctor = newConstructExpression()
        ctor.type = t
        frontend.setCodeAndLocation(ctor, expr)

        // handle the arguments
        for (i in arguments.indices) {
            val argument =
                handle(arguments[i])
                    as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
                    ?: continue
            argument.argumentIndex = i
            ctor.addArgument(argument)
        }
        newExpression.initializer = ctor

        if (objectCreationExpr.anonymousClassBody.isPresent) {
            // We have an anonymous class and will create a RecordDeclaration for it and add all the
            // implemented methods.
            val locationHash = frontend.locationOf(objectCreationExpr)?.hashCode()

            // We use the hash of the location to distinguish multiple instances of the anonymous
            // class' superclass
            val anonymousClassName = "$constructorName$locationHash"
            val anonymousRecord = newRecordDeclaration(anonymousClassName, "class")
            anonymousRecord.isImplicit = true

            frontend.scopeManager.enterScope(anonymousRecord)

            anonymousRecord.addSuperClass(objectType(constructorName))
            val anonymousClassBody = objectCreationExpr.anonymousClassBody.get()
            for (classBody in anonymousClassBody) {
                // Whatever is implemented in the anonymous class has to be added to the record
                // declaration
                val classBodyDecl = frontend.declarationHandler.handle(classBody)
                classBodyDecl?.let { anonymousRecord.addDeclaration(it) }
            }

            if (anonymousRecord.constructors.isEmpty()) {
                val constructorDeclaration =
                    newConstructorDeclaration(
                            anonymousRecord.name.localName,
                            anonymousRecord,
                        )
                        .implicit(anonymousRecord.name.localName)

                ctor.arguments.forEachIndexed { i, arg ->
                    constructorDeclaration.addParameter(
                        newParameterDeclaration("arg${i}", arg.type)
                    )
                }
                anonymousRecord.addConstructor(constructorDeclaration)
                ctor.anoymousClass = anonymousRecord

                frontend.scopeManager.addDeclaration(constructorDeclaration)
                frontend.scopeManager.leaveScope(anonymousRecord)
            }
        }

        return newExpression
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExpressionHandler::class.java)
    }

    init {
        map[com.github.javaparser.ast.expr.AssignExpr::class.java] = HandlerInterface {
            handleAssignmentExpression(it)
        }
        map[FieldAccessExpr::class.java] = HandlerInterface { handleFieldAccessExpression(it) }
        map[LiteralExpr::class.java] = HandlerInterface { handleLiteralExpression(it) }
        map[ThisExpr::class.java] = HandlerInterface { handleThisExpression(it) }
        map[SuperExpr::class.java] = HandlerInterface { handleSuperExpression(it) }
        map[ClassExpr::class.java] = HandlerInterface { handleClassExpression(it) }
        map[NameExpr::class.java] = HandlerInterface { handleNameExpression(it) }
        map[InstanceOfExpr::class.java] = HandlerInterface { handleInstanceOfExpression(it) }
        map[UnaryExpr::class.java] = HandlerInterface { handleUnaryExpression(it) }
        map[BinaryExpr::class.java] = HandlerInterface { handleBinaryExpression(it) }
        map[VariableDeclarationExpr::class.java] = HandlerInterface {
            handleVariableDeclarationExpr(it)
        }
        map[MethodCallExpr::class.java] = HandlerInterface { handleMethodCallExpression(it) }
        map[ObjectCreationExpr::class.java] = HandlerInterface { handleObjectCreationExpr(it) }
        map[com.github.javaparser.ast.expr.ConditionalExpr::class.java] = HandlerInterface {
            handleConditionalExpression(it)
        }
        map[EnclosedExpr::class.java] = HandlerInterface { handleEnclosedExpression(it) }
        map[ArrayAccessExpr::class.java] = HandlerInterface { handleArrayAccessExpr(it) }
        map[ArrayCreationExpr::class.java] = HandlerInterface { handleArrayCreationExpr(it) }
        map[ArrayInitializerExpr::class.java] = HandlerInterface { handleArrayInitializerExpr(it) }
        map[com.github.javaparser.ast.expr.CastExpr::class.java] = HandlerInterface {
            handleCastExpr(it)
        }
        map[com.github.javaparser.ast.expr.LambdaExpr::class.java] = HandlerInterface {
            handleLambdaExpr(it)
        }
    }
}
