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
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.*
import com.github.javaparser.resolution.UnsolvedSymbolException
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HandlerInterface
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newArrayCreationExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newArraySubscriptionExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newBinaryOperator
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCallExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCastExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newConditionalExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newConstructExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newDeclarationStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newDeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newInitializerListExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newLiteral
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMemberCallExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMemberExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newNewExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newStaticCallExpression
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newUnaryOperator
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newVariableDeclaration
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import java.util.function.Supplier
import java.util.stream.Collectors
import org.slf4j.LoggerFactory

class ExpressionHandler(lang: JavaLanguageFrontend) :
    Handler<Statement, com.github.javaparser.ast.expr.Expression, JavaLanguageFrontend>(
        Supplier { ProblemExpression() },
        lang
    ) {
    private fun handleCastExpr(expr: com.github.javaparser.ast.expr.Expression): Statement {
        val castExpr = expr.asCastExpr()
        val castExpression = newCastExpression(expr.toString())
        val expression = handle(castExpr.expression) as Expression?
        castExpression.expression = expression
        castExpression.setCastOperator(2)
        val t = lang.getTypeAsGoodAsPossible(castExpr.type)
        castExpression.castType = t
        if (castExpr.type.isPrimitiveType) {
            // Set Type based on the Casting type as it will result in a conversion for primitive
            // types
            castExpression.type =
                TypeParser.createFrom(castExpr.type.resolve().asPrimitive().describe(), true)
        } else {
            // Get Runtime type from cast expression for complex types;
            castExpression.expression.registerTypeListener(castExpression)
        }
        return castExpression
    }

    /**
     * Creates a new [ArrayCreationExpression], which is usually used as an initializer of a
     * [VariableDeclaration].
     *
     * @param expr the expression
     * @return the [ArrayCreationExpression]
     */
    private fun handleArrayCreationExpr(
        expr: com.github.javaparser.ast.expr.Expression
    ): Statement {
        val arrayCreationExpr = expr as ArrayCreationExpr
        val creationExpression = newArrayCreationExpression(expr.toString())

        // in Java, an array creation expression either specifies an initializer or dimensions

        // parse initializer, if present
        arrayCreationExpr.initializer.ifPresent { init: ArrayInitializerExpr? ->
            creationExpression.initializer = handle(init) as InitializerListExpression?
        }

        // dimensions are only present if you specify them explicitly, such as new int[1]
        for (lvl in arrayCreationExpr.levels) {
            lvl.dimension.ifPresent { expression: com.github.javaparser.ast.expr.Expression? ->
                creationExpression.addDimension(handle(expression) as Expression?)
            }
        }
        return creationExpression
    }

    private fun handleArrayInitializerExpr(
        expr: com.github.javaparser.ast.expr.Expression
    ): Statement {
        val arrayInitializerExpr = expr as ArrayInitializerExpr
        // ArrayInitializerExpressions are converted into InitializerListExpressions to reduce the
        // syntactic distance a CPP and JAVA CPG
        val initList = newInitializerListExpression(expr.toString())
        val initializers =
            arrayInitializerExpr
                .values
                .stream()
                .map { ctx: com.github.javaparser.ast.expr.Expression? -> handle(ctx) }
                .map { obj: Statement? -> Expression::class.java.cast(obj) }
                .collect(Collectors.toList())
        initList.initializers = initializers
        return initList
    }

    private fun handleArrayAccessExpr(
        expr: com.github.javaparser.ast.expr.Expression
    ): ArraySubscriptionExpression {
        val arrayAccessExpr = expr as ArrayAccessExpr
        val arraySubsExpression = newArraySubscriptionExpression(expr.toString())
        arraySubsExpression.arrayExpression = handle(arrayAccessExpr.name) as Expression?
        arraySubsExpression.subscriptExpression = handle(arrayAccessExpr.index) as Expression?
        return arraySubsExpression
    }

    private fun handleEnclosedExpression(
        expr: com.github.javaparser.ast.expr.Expression
    ): Statement? {
        return handle((expr as EnclosedExpr).inner)
    }

    private fun handleConditionalExpression(
        expr: com.github.javaparser.ast.expr.Expression
    ): ConditionalExpression {
        val conditionalExpr = expr.asConditionalExpr()
        val superType: Type?
        superType =
            try {
                TypeParser.createFrom(conditionalExpr.calculateResolvedType().describe(), true)
            } catch (e: RuntimeException) {
                val s = lang.recoverTypeFromUnsolvedException(e)
                if (s != null) {
                    TypeParser.createFrom(s, true)
                } else {
                    null
                }
            } catch (e: NoClassDefFoundError) {
                val s = lang.recoverTypeFromUnsolvedException(e)
                if (s != null) {
                    TypeParser.createFrom(s, true)
                } else {
                    null
                }
            }
        val condition = handle(conditionalExpr.condition) as Expression?
        val thenExpr = handle(conditionalExpr.thenExpr) as Expression?
        val elseExpr = handle(conditionalExpr.elseExpr) as Expression?
        return newConditionalExpression(condition, thenExpr, elseExpr, superType)
    }

    private fun handleAssignmentExpression(
        expr: com.github.javaparser.ast.expr.Expression
    ): BinaryOperator {
        val assignExpr = expr.asAssignExpr()

        // first, handle the target. this is the first argument of the operator call
        val lhs = handle(assignExpr.target) as Expression?

        // second, handle the value. this is the second argument of the operator call
        val rhs = handle(assignExpr.value) as Expression?
        val binaryOperator =
            newBinaryOperator(assignExpr.operator.asString(), assignExpr.toString())
        binaryOperator.lhs = lhs
        binaryOperator.rhs = rhs
        return binaryOperator
    }

    // Not sure how to handle this exactly yet
    private fun handleVariableDeclarationExpr(
        expr: com.github.javaparser.ast.expr.Expression
    ): DeclarationStatement {
        val variableDeclarationExpr = expr.asVariableDeclarationExpr()
        val declarationStatement = newDeclarationStatement(variableDeclarationExpr.toString())
        for (variable in variableDeclarationExpr.variables) {
            val resolved = variable.resolve()
            val declarationType = lang.getTypeAsGoodAsPossible(variable, resolved)
            declarationType.setAdditionalTypeKeywords(
                variableDeclarationExpr
                    .modifiers
                    .stream()
                    .map { m: Modifier -> m.keyword.asString() }
                    .collect(Collectors.joining(" "))
            )
            val declaration =
                newVariableDeclaration(
                    resolved.name,
                    declarationType,
                    variable.toString(),
                    false,
                    lang,
                    variable
                )
            if (declarationType is PointerType && declarationType.isArray) {
                declaration.setIsArray(true)
            }
            val oInitializer = variable.initializer
            if (oInitializer.isPresent) {
                val initializer = handle(oInitializer.get()) as Expression?
                if (initializer is ArrayCreationExpression) {
                    declaration.setIsArray(true)
                }
                declaration.initializer = initializer
            } else {
                val uninitialzedInitializer: Expression = UninitializedValue()
                declaration.initializer = uninitialzedInitializer
            }
            lang.setCodeAndRegion(declaration, variable)
            declarationStatement.addToPropertyEdgeDeclaration(declaration)
            lang.processAnnotations(declaration, variableDeclarationExpr)
            lang.scopeManager.addDeclaration(declaration)
        }
        return declarationStatement
    }

    private fun handleFieldAccessExpression(
        expr: com.github.javaparser.ast.expr.Expression
    ): Expression {
        val fieldAccessExpr = expr.asFieldAccessExpr()
        var base: Expression
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
                baseType =
                    TypeParser.createFrom(resolve.asField().declaringType().qualifiedName, true)
            } catch (ex: RuntimeException) {
                isStaticAccess = true
                val typeString = lang.recoverTypeFromUnsolvedException(ex)
                if (typeString != null) {
                    baseType = TypeParser.createFrom(typeString, true)
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
                    val qualifiedNameFromImports = lang.getQualifiedNameFromImports(name)
                    baseType =
                        if (qualifiedNameFromImports != null) {
                            TypeParser.createFrom(qualifiedNameFromImports, true)
                        } else {
                            log.info("Unknown base type 1 for {}", fieldAccessExpr)
                            UnknownType.getUnknownType()
                        }
                }
            } catch (ex: NoClassDefFoundError) {
                isStaticAccess = true
                val typeString = lang.recoverTypeFromUnsolvedException(ex)
                if (typeString != null) {
                    baseType = TypeParser.createFrom(typeString, true)
                } else {
                    val name: String
                    val tokenRange = scope.asNameExpr().tokenRange
                    name =
                        if (tokenRange.isPresent) {
                            tokenRange.get().toString()
                        } else {
                            scope.asNameExpr().nameAsString
                        }
                    val qualifiedNameFromImports = lang.getQualifiedNameFromImports(name)
                    baseType =
                        if (qualifiedNameFromImports != null) {
                            TypeParser.createFrom(qualifiedNameFromImports, true)
                        } else {
                            log.info("Unknown base type 1 for {}", fieldAccessExpr)
                            UnknownType.getUnknownType()
                        }
                }
            }
            base =
                newDeclaredReferenceExpression(
                    scope.asNameExpr().nameAsString,
                    baseType,
                    scope.toString()
                )
            base.isStaticAccess = isStaticAccess
            lang.setCodeAndRegion<Expression, com.github.javaparser.ast.expr.Expression>(
                base,
                fieldAccessExpr.scope
            )
        } else if (scope.isFieldAccessExpr) {
            base = handle(scope) as Expression
            var tester = base
            while (tester is MemberExpression) {
                // we need to check if any base is only a static access, otherwise, this is a member
                // access
                // to this base
                tester = tester.base
            }
            if (tester is DeclaredReferenceExpression && tester.isStaticAccess) {
                // try to get the name
                val name: String
                val tokenRange = scope.asFieldAccessExpr().tokenRange
                name =
                    if (tokenRange.isPresent) {
                        tokenRange.get().toString()
                    } else {
                        scope.asFieldAccessExpr().nameAsString
                    }
                val qualifiedNameFromImports = lang.getQualifiedNameFromImports(name)
                val baseType: Type =
                    if (qualifiedNameFromImports != null) {
                        TypeParser.createFrom(qualifiedNameFromImports, true)
                    } else {
                        log.info("Unknown base type 2 for {}", fieldAccessExpr)
                        UnknownType.getUnknownType()
                    }
                base =
                    newDeclaredReferenceExpression(
                        scope.asFieldAccessExpr().nameAsString,
                        baseType,
                        scope.toString()
                    )
                base.isStaticAccess = true
            }
            lang.setCodeAndRegion(base, fieldAccessExpr.scope)
        } else {
            base = handle(scope) as Expression
        }
        var fieldType: Type?
        try {
            val symbol = fieldAccessExpr.resolve()
            fieldType =
                TypeManager.getInstance()
                    .getTypeParameter(
                        lang.scopeManager.currentRecord,
                        symbol.asField().type.describe()
                    )
            if (fieldType == null) {
                fieldType = TypeParser.createFrom(symbol.asField().type.describe(), true)
            }
        } catch (ex: RuntimeException) {
            val typeString = lang.recoverTypeFromUnsolvedException(ex)
            fieldType =
                if (typeString != null) {
                    TypeParser.createFrom(typeString, true)
                } else if (fieldAccessExpr.toString().endsWith(".length")) {
                    TypeParser.createFrom("int", true)
                } else {
                    log.info("Unknown field type for {}", fieldAccessExpr)
                    UnknownType.getUnknownType()
                }
            val memberExpression =
                newMemberExpression(
                    base,
                    fieldType,
                    fieldAccessExpr.name.identifier,
                    ".", // there is only "." in java
                    fieldAccessExpr.toString()
                )
            memberExpression.isStaticAccess = true
            return memberExpression
        } catch (ex: NoClassDefFoundError) {
            val typeString = lang.recoverTypeFromUnsolvedException(ex)
            fieldType =
                if (typeString != null) {
                    TypeParser.createFrom(typeString, true)
                } else if (fieldAccessExpr.toString().endsWith(".length")) {
                    TypeParser.createFrom("int", true)
                } else {
                    log.info("Unknown field type for {}", fieldAccessExpr)
                    UnknownType.getUnknownType()
                }
            val memberExpression =
                newMemberExpression(
                    base,
                    fieldType,
                    fieldAccessExpr.name.identifier,
                    ".",
                    fieldAccessExpr.toString()
                )
            memberExpression.isStaticAccess = true
            return memberExpression
        }
        if (base!!.location == null) {
            base.location = lang.getLocationFromRawNode(fieldAccessExpr)
        }
        return newMemberExpression(
            base,
            fieldType,
            fieldAccessExpr.name.identifier,
            ".",
            fieldAccessExpr.toString()
        )
    }

    private fun handleLiteralExpression(
        expr: com.github.javaparser.ast.expr.Expression
    ): Literal<*>? {
        val literalExpr = expr.asLiteralExpr()
        val value = literalExpr.toString()
        if (literalExpr is IntegerLiteralExpr) {
            return newLiteral(
                literalExpr.asIntegerLiteralExpr().asNumber(),
                TypeParser.createFrom("int", true),
                value
            )
        } else if (literalExpr is StringLiteralExpr) {
            return newLiteral(
                literalExpr.asStringLiteralExpr().asString(),
                TypeParser.createFrom("java.lang.String", true),
                value
            )
        } else if (literalExpr is BooleanLiteralExpr) {
            return newLiteral(
                literalExpr.asBooleanLiteralExpr().value,
                TypeParser.createFrom("boolean", true),
                value
            )
        } else if (literalExpr is CharLiteralExpr) {
            return newLiteral(
                literalExpr.asCharLiteralExpr().asChar(),
                TypeParser.createFrom("char", true),
                value
            )
        } else if (literalExpr is DoubleLiteralExpr) {
            return newLiteral(
                literalExpr.asDoubleLiteralExpr().asDouble(),
                TypeParser.createFrom("double", true),
                value
            )
        } else if (literalExpr is LongLiteralExpr) {
            return newLiteral(
                literalExpr.asLongLiteralExpr().asNumber(),
                TypeParser.createFrom("long", true),
                value
            )
        } else if (literalExpr is NullLiteralExpr) {
            return newLiteral<Any?>(null, TypeParser.createFrom("null", true), value)
        }
        return null
    }

    private fun handleClassExpression(
        expr: com.github.javaparser.ast.expr.Expression
    ): DeclaredReferenceExpression {
        val classExpr = expr.asClassExpr()
        val type = TypeParser.createFrom(classExpr.type.asString(), true)
        val thisExpression =
            newDeclaredReferenceExpression(
                classExpr.toString().substring(classExpr.toString().lastIndexOf('.') + 1),
                type,
                classExpr.toString()
            )
        thisExpression.isStaticAccess = true
        lang.setCodeAndRegion(thisExpression, classExpr)
        return thisExpression
    }

    private fun handleThisExpression(
        expr: com.github.javaparser.ast.expr.Expression
    ): DeclaredReferenceExpression {
        // TODO: use a separate ThisExpression (issue #8)
        val thisExpr = expr.asThisExpr()
        val resolvedValueDeclaration = thisExpr.resolve()
        val type = TypeParser.createFrom(resolvedValueDeclaration.qualifiedName, true)
        val thisExpression =
            newDeclaredReferenceExpression(thisExpr.toString(), type, thisExpr.toString())
        lang.setCodeAndRegion(thisExpression, thisExpr)
        return thisExpression
    }

    private fun handleSuperExpression(
        expr: com.github.javaparser.ast.expr.Expression
    ): DeclaredReferenceExpression {
        // The actual type is hard to determine at this point, as we may not have full information
        // about the inheritance structure. Thus we delay the resolving to the variable resolving
        // process
        val superExpression =
            newDeclaredReferenceExpression(
                expr.toString(),
                UnknownType.getUnknownType(),
                expr.toString()
            )
        lang.setCodeAndRegion(superExpression, expr)
        return superExpression
    }

    // TODO: this function needs a MAJOR overhaul!
    private fun handleNameExpression(expr: com.github.javaparser.ast.expr.Expression): Expression? {
        val nameExpr = expr.asNameExpr()

        // TODO this commented code breaks field accesses to fields that don't have a primitive
        // type.
        //  How should this be handled correctly?
        //    try {
        //      ResolvedType resolvedType = nameExpr.calculateResolvedType();
        //      if (resolvedType.isReferenceType()) {
        //        return NodeBuilder.newDeclaredReferenceExpression(
        //            nameExpr.getNameAsString(),
        //            new Type(((ReferenceTypeImpl) resolvedType).getQualifiedName()),
        //            nameExpr.toString());
        //      }
        //    } catch (
        //        UnsolvedSymbolException
        //            e) { // this might throw, e.g. if the type is simply not defined (i.e., syntax
        // error)
        //      return NodeBuilder.newDeclaredReferenceExpression(
        //          nameExpr.getNameAsString(), new Type(UNKNOWN_TYPE), nameExpr.toString());
        //    }
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
                    handle(fieldAccessExpr) as Expression?
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
                    handle(fieldAccessExpr) as Expression?
                }
            } else {
                // Resolve type first with ParameterizedType
                var type: Type? =
                    TypeManager.getInstance()
                        .getTypeParameter(lang.scopeManager.currentRecord, symbol.type.describe())
                if (type == null) {
                    type = TypeParser.createFrom(symbol.type.describe(), true)
                }
                newDeclaredReferenceExpression(symbol.name, type, nameExpr.toString())
            }
        } catch (ex: UnsolvedSymbolException) {
            val typeString: String?
            typeString =
                if (ex.name.startsWith(
                        "We are unable to find the value declaration corresponding to"
                    )
                ) {
                    nameExpr.nameAsString
                } else {
                    lang.recoverTypeFromUnsolvedException(ex)
                }
            val t: Type
            if (typeString == null) {
                t = TypeParser.createFrom("UNKNOWN3", true)
                log.info("Unresolved symbol: {}", nameExpr.nameAsString)
            } else {
                t = TypeParser.createFrom(typeString, true)
                t.typeOrigin = Type.Origin.GUESSED
            }
            val name = nameExpr.nameAsString
            val declaredReferenceExpression =
                newDeclaredReferenceExpression(name, t, nameExpr.toString())
            val recordDeclaration = lang.scopeManager.currentRecord
            if (recordDeclaration != null && recordDeclaration.name == name) {
                declaredReferenceExpression.refersTo = recordDeclaration
            }
            declaredReferenceExpression
        } catch (ex: RuntimeException) {
            val t = TypeParser.createFrom("UNKNOWN4", true)
            log.info("Unresolved symbol: {}", nameExpr.nameAsString)
            newDeclaredReferenceExpression(nameExpr.nameAsString, t, nameExpr.toString())
        } catch (ex: NoClassDefFoundError) {
            val t = TypeParser.createFrom("UNKNOWN4", true)
            log.info("Unresolved symbol: {}", nameExpr.nameAsString)
            newDeclaredReferenceExpression(nameExpr.nameAsString, t, nameExpr.toString())
        }
    }

    private fun handleInstanceOfExpression(
        expr: com.github.javaparser.ast.expr.Expression
    ): BinaryOperator {
        val binaryExpr = expr.asInstanceOfExpr()

        // first, handle the target. this is the first argument of the operator callUnresolved
        // symbol
        val lhs = handle(binaryExpr.expression) as Expression?
        val typeAsGoodAsPossible = lang.getTypeAsGoodAsPossible(binaryExpr.type)

        // second, handle the value. this is the second argument of the operator call
        val rhs: Expression =
            newLiteral(
                typeAsGoodAsPossible.typeName,
                TypeParser.createFrom("class", true),
                binaryExpr.typeAsString
            )
        val binaryOperator = newBinaryOperator("instanceof", binaryExpr.toString())
        binaryOperator.lhs = lhs
        binaryOperator.rhs = rhs
        return binaryOperator
    }

    private fun handleUnaryExpression(
        expr: com.github.javaparser.ast.expr.Expression
    ): UnaryOperator {
        val unaryExpr = expr.asUnaryExpr()

        // handle the 'inner' expression, which is affected by the unary expression
        val expression = handle(unaryExpr.expression) as Expression?
        val unaryOperator =
            newUnaryOperator(
                unaryExpr.operator.asString(),
                unaryExpr.isPostfix,
                unaryExpr.isPrefix,
                unaryExpr.toString()
            )
        unaryOperator.input = expression
        return unaryOperator
    }

    private fun handleBinaryExpression(
        expr: com.github.javaparser.ast.expr.Expression
    ): BinaryOperator {
        val binaryExpr = expr.asBinaryExpr()

        // first, handle the target. this is the first argument of the operator call
        val lhs = handle(binaryExpr.left) as Expression?

        // second, handle the value. this is the second argument of the operator call
        val rhs = handle(binaryExpr.right) as Expression?
        val binaryOperator =
            newBinaryOperator(binaryExpr.operator.asString(), binaryExpr.toString())
        binaryOperator.lhs = lhs
        binaryOperator.rhs = rhs
        return binaryOperator
    }

    private fun handleMethodCallExpression(
        expr: com.github.javaparser.ast.expr.Expression
    ): CallExpression {
        val methodCallExpr = expr.asMethodCallExpr()
        val callExpression: CallExpression
        val o = methodCallExpr.scope
        val qualifiedName = lang.getQualifiedMethodNameAsGoodAsPossible(methodCallExpr)
        var name = qualifiedName
        if (name.contains(".")) {
            name = name.substring(name.lastIndexOf('.') + 1)
        }
        var typeString = UnknownType.UNKNOWN_TYPE_STRING
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

        // the scope could either be a variable or also the class name (static call!)
        // thus, only because the scope is present, this is not automatically a member call
        if (o.isPresent) {
            val scope = o.get()
            var scopeName: String? = null
            if (scope is NameExpr) {
                scopeName = scope.nameAsString
            } else if (scope is SuperExpr) {
                scopeName = scope.toString()
            }
            val base = handle(scope) as Expression?

            // If the base directly refers to a record, then this is a static call
            if (base is DeclaredReferenceExpression && base.refersTo is RecordDeclaration) {
                isStatic = true
            }

            // Or if the base is a reference to an import
            if (base is DeclaredReferenceExpression &&
                    lang.getQualifiedNameFromImports(base.name) != null
            ) {
                isStatic = true
            }
            if (!isStatic) {
                val member = newDeclaredReferenceExpression(name, UnknownType.getUnknownType(), "")
                lang.setCodeAndRegion(
                    member,
                    methodCallExpr.name
                ) // This will also overwrite the code set to the empty string set above
                callExpression =
                    newMemberCallExpression(
                        name,
                        qualifiedName,
                        base,
                        member,
                        ".",
                        methodCallExpr.toString()
                    )
            } else {
                var targetClass: String?
                targetClass =
                    if (resolved != null) {
                        resolved.declaringType().qualifiedName
                    } else {
                        lang.getQualifiedNameFromImports(scopeName)
                    }
                if (targetClass == null) {
                    targetClass = scopeName
                }
                callExpression =
                    newStaticCallExpression(
                        name,
                        qualifiedName,
                        methodCallExpr.toString(),
                        targetClass
                    )
            }
        } else {
            callExpression =
                newCallExpression(name, qualifiedName, methodCallExpr.toString(), false)
        }
        callExpression.type = TypeParser.createFrom(typeString, true)
        val arguments = methodCallExpr.arguments

        // handle the arguments
        for (i in arguments.indices) {
            val argument = handle(arguments[i]) as Expression?
            argument!!.argumentIndex = i
            callExpression.addArgument(argument)
        }
        return callExpression
    }

    private fun handleObjectCreationExpr(
        expr: com.github.javaparser.ast.expr.Expression
    ): NewExpression {
        val objectCreationExpr = expr.asObjectCreationExpr()

        // scope refers to the constructor arguments
        val o = objectCreationExpr.scope
        if (o.isPresent) {
            // TODO: what to do with it?
            log.warn("Scope {}", o)
        }

        // todo can we merge newNewExpression and newConstructExpression?
        val t = lang.getTypeAsGoodAsPossible(objectCreationExpr.type)
        val newExpression = newNewExpression(expr.toString(), t)
        val arguments = objectCreationExpr.arguments
        var code = expr.toString()
        if (code.length > 4) {
            code = code.substring(4) // remove "new "
        }
        val ctor = newConstructExpression(code)
        ctor.type = t
        lang.setCodeAndRegion(ctor, expr)

        // handle the arguments
        for (i in arguments.indices) {
            val argument = handle(arguments[i]) as Expression?
            argument!!.argumentIndex = i
            ctor.addArgument(argument)
        }
        newExpression.initializer = ctor
        return newExpression
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExpressionHandler::class.java)
    }

    init {
        map[AssignExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleAssignmentExpression(expr)
            }
        map[FieldAccessExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleFieldAccessExpression(expr)
            }
        map[LiteralExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleLiteralExpression(expr)
            }
        map[ThisExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleThisExpression(expr)
            }
        map[SuperExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleSuperExpression(expr)
            }
        map[ClassExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleClassExpression(expr)
            }
        map[NameExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleNameExpression(expr)
            }
        map[InstanceOfExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleInstanceOfExpression(expr)
            }
        map[UnaryExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleUnaryExpression(expr)
            }
        map[BinaryExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleBinaryExpression(expr)
            }
        map[VariableDeclarationExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleVariableDeclarationExpr(expr)
            }
        map[MethodCallExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleMethodCallExpression(expr)
            }
        map[ObjectCreationExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleObjectCreationExpr(expr)
            }
        map[ConditionalExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleConditionalExpression(expr)
            }
        map[EnclosedExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleEnclosedExpression(expr)
            }
        map[ArrayAccessExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleArrayAccessExpr(expr)
            }
        map[ArrayCreationExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleArrayCreationExpr(expr)
            }
        map[ArrayInitializerExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleArrayInitializerExpr(expr)
            }
        map[CastExpr::class.java] =
            HandlerInterface { expr: com.github.javaparser.ast.expr.Expression ->
                handleCastExpr(expr)
            }
    }
}
