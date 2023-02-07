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
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.resolution.UnsolvedSymbolException
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HandlerInterface
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import java.util.function.Supplier
import java.util.stream.Collectors
import kotlin.collections.set
import org.slf4j.LoggerFactory

class ExpressionHandler(lang: JavaLanguageFrontend) :
    Handler<Statement, Expression, JavaLanguageFrontend>(Supplier { ProblemExpression() }, lang) {
    private fun handleCastExpr(expr: Expression): Statement {
        val castExpr = expr.asCastExpr()
        val castExpression = this.newCastExpression(expr.toString())
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
            castExpression.type = this.parseType(castExpr.type.resolve().asPrimitive().describe())
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
    private fun handleArrayCreationExpr(expr: Expression): Statement {
        val arrayCreationExpr = expr as ArrayCreationExpr
        val creationExpression = this.newArrayCreationExpression(expr.toString())

        // in Java, an array creation expression either specifies an initializer or dimensions

        // parse initializer, if present
        arrayCreationExpr.initializer.ifPresent {
            creationExpression.initializer = handle(it) as? InitializerListExpression
        }

        // dimensions are only present if you specify them explicitly, such as new int[1]
        for (lvl in arrayCreationExpr.levels) {
            lvl.dimension.ifPresent {
                creationExpression.addDimension(
                    (handle(it)
                        as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?)!!
                )
            }
        }
        return creationExpression
    }

    private fun handleArrayInitializerExpr(expr: Expression): Statement {
        val arrayInitializerExpr = expr as ArrayInitializerExpr
        // ArrayInitializerExpressions are converted into InitializerListExpressions to reduce the
        // syntactic distance a CPP and JAVA CPG
        val initList = this.newInitializerListExpression(expr.toString())
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

    private fun handleArrayAccessExpr(expr: Expression): ArraySubscriptionExpression {
        val arrayAccessExpr = expr as ArrayAccessExpr
        val arraySubsExpression = this.newArraySubscriptionExpression(expr.toString())
        arraySubsExpression.arrayExpression =
            (handle(arrayAccessExpr.name)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?)!!
        arraySubsExpression.subscriptExpression =
            (handle(arrayAccessExpr.index)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?)!!
        return arraySubsExpression
    }

    private fun handleEnclosedExpression(expr: Expression): Statement? {
        return handle((expr as EnclosedExpr).inner)
    }

    private fun handleConditionalExpression(expr: Expression): ConditionalExpression {
        val conditionalExpr = expr.asConditionalExpr()
        val superType: Type =
            try {
                this.parseType(conditionalExpr.calculateResolvedType().describe())
            } catch (e: RuntimeException) {
                val s = frontend.recoverTypeFromUnsolvedException(e)
                if (s != null) {
                    this.parseType(s)
                } else {
                    UnknownType.getUnknownType(this.language)
                }
            } catch (e: NoClassDefFoundError) {
                val s = frontend.recoverTypeFromUnsolvedException(e)
                if (s != null) {
                    this.parseType(s)
                } else {
                    UnknownType.getUnknownType(this.language)
                }
            }
        val condition =
            handle(conditionalExpr.condition)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
        val thenExpr =
            handle(conditionalExpr.thenExpr)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
        val elseExpr =
            handle(conditionalExpr.elseExpr)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
        return this.newConditionalExpression(condition!!, thenExpr, elseExpr, superType)
    }

    private fun handleAssignmentExpression(expr: Expression): BinaryOperator {
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
        val binaryOperator =
            this.newBinaryOperator(assignExpr.operator.asString(), assignExpr.toString())
        binaryOperator.lhs = lhs
        binaryOperator.rhs = rhs
        return binaryOperator
    }

    // Not sure how to handle this exactly yet
    private fun handleVariableDeclarationExpr(expr: Expression): DeclarationStatement {
        val variableDeclarationExpr = expr.asVariableDeclarationExpr()
        val declarationStatement = this.newDeclarationStatement(variableDeclarationExpr.toString())
        for (variable in variableDeclarationExpr.variables) {
            val resolved = variable.resolve()
            val declarationType = frontend.getTypeAsGoodAsPossible(variable, resolved)
            declarationType.setAdditionalTypeKeywords(
                variableDeclarationExpr.modifiers
                    .stream()
                    .map { m: Modifier -> m.keyword.asString() }
                    .collect(Collectors.joining(" "))
            )
            val declaration =
                this.newVariableDeclaration(
                    resolved.name,
                    declarationType,
                    variable.toString(),
                    false,
                    variable
                )
            if (declarationType is PointerType && declarationType.isArray) {
                declaration.isArray = true
            }
            val oInitializer = variable.initializer
            if (oInitializer.isPresent) {
                val initializer =
                    handle(oInitializer.get())
                        as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
                if (initializer is ArrayCreationExpression) {
                    declaration.isArray = true
                }
                declaration.initializer = initializer
            }
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
        var base: de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
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
                baseType = this.parseType(resolve.asField().declaringType().qualifiedName)
            } catch (ex: RuntimeException) {
                isStaticAccess = true
                val typeString = frontend.recoverTypeFromUnsolvedException(ex)
                if (typeString != null) {
                    baseType = this.parseType(typeString)
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
                            this.parseType(qualifiedNameFromImports)
                        } else {
                            log.info("Unknown base type 1 for {}", fieldAccessExpr)
                            UnknownType.getUnknownType(language)
                        }
                }
            } catch (ex: NoClassDefFoundError) {
                isStaticAccess = true
                val typeString = frontend.recoverTypeFromUnsolvedException(ex)
                if (typeString != null) {
                    baseType = this.parseType(typeString)
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
                            this.parseType(qualifiedNameFromImports)
                        } else {
                            log.info("Unknown base type 1 for {}", fieldAccessExpr)
                            UnknownType.getUnknownType(language)
                        }
                }
            }
            base =
                this.newDeclaredReferenceExpression(
                    scope.asNameExpr().nameAsString,
                    baseType,
                    scope.toString()
                )
            base.isStaticAccess = isStaticAccess
            frontend.setCodeAndLocation<
                de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression, Expression
            >(
                base,
                fieldAccessExpr.scope
            )
        } else if (scope.isFieldAccessExpr) {
            base = handle(scope) as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
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
                val qualifiedNameFromImports = frontend.getQualifiedNameFromImports(name)
                val baseType: Type
                baseType =
                    if (qualifiedNameFromImports != null) {
                        this.parseType(qualifiedNameFromImports)
                    } else {
                        log.info("Unknown base type 2 for {}", fieldAccessExpr)
                        UnknownType.getUnknownType(language)
                    }
                base =
                    this.newDeclaredReferenceExpression(
                        scope.asFieldAccessExpr().nameAsString,
                        baseType,
                        scope.toString()
                    )
                base.isStaticAccess = true
            }
            frontend.setCodeAndLocation(base, fieldAccessExpr.scope)
        } else {
            base = handle(scope) as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
        }
        var fieldType: Type?
        try {
            val symbol = fieldAccessExpr.resolve()
            fieldType =
                TypeManager.getInstance()
                    .getTypeParameter(
                        frontend.scopeManager.currentRecord,
                        symbol.asField().type.describe()
                    )
            if (fieldType == null) {
                fieldType = this.parseType(symbol.asField().type.describe())
            }
        } catch (ex: RuntimeException) {
            val typeString = frontend.recoverTypeFromUnsolvedException(ex)
            fieldType =
                if (typeString != null) {
                    this.parseType(typeString)
                } else if (fieldAccessExpr.toString().endsWith(".length")) {
                    this.parseType("int")
                } else {
                    log.info("Unknown field type for {}", fieldAccessExpr)
                    UnknownType.getUnknownType(language)
                }
            val memberExpression =
                this.newMemberExpression(
                    fieldAccessExpr.name.identifier,
                    base!!,
                    fieldType,
                    "." // there is only "." in java
                )
            memberExpression.isStaticAccess = true
            return memberExpression
        } catch (ex: NoClassDefFoundError) {
            val typeString = frontend.recoverTypeFromUnsolvedException(ex)
            fieldType =
                if (typeString != null) {
                    this.parseType(typeString)
                } else if (fieldAccessExpr.toString().endsWith(".length")) {
                    this.parseType("int")
                } else {
                    log.info("Unknown field type for {}", fieldAccessExpr)
                    UnknownType.getUnknownType(language)
                }
            val memberExpression =
                this.newMemberExpression(fieldAccessExpr.name.identifier, base!!, fieldType, ".")
            memberExpression.isStaticAccess = true
            return memberExpression
        }
        if (base!!.location == null) {
            base.location = frontend.getLocationFromRawNode(fieldAccessExpr)
        }
        return this.newMemberExpression(fieldAccessExpr.name.identifier, base, fieldType, ".")
    }

    private fun handleLiteralExpression(expr: Expression): Literal<*>? {
        val literalExpr = expr.asLiteralExpr()
        val value = literalExpr.toString()
        if (literalExpr is IntegerLiteralExpr) {
            return this.newLiteral(
                literalExpr.asIntegerLiteralExpr().asNumber(),
                this.parseType("int"),
                value
            )
        } else if (literalExpr is StringLiteralExpr) {
            return this.newLiteral(
                literalExpr.asStringLiteralExpr().asString(),
                this.parseType("java.lang.String"),
                value
            )
        } else if (literalExpr is BooleanLiteralExpr) {
            return this.newLiteral(
                literalExpr.asBooleanLiteralExpr().value,
                this.parseType("boolean"),
                value
            )
        } else if (literalExpr is CharLiteralExpr) {
            return this.newLiteral(
                literalExpr.asCharLiteralExpr().asChar(),
                this.parseType("char"),
                value
            )
        } else if (literalExpr is DoubleLiteralExpr) {
            return this.newLiteral(
                literalExpr.asDoubleLiteralExpr().asDouble(),
                this.parseType("double"),
                value
            )
        } else if (literalExpr is LongLiteralExpr) {
            return this.newLiteral(
                literalExpr.asLongLiteralExpr().asNumber(),
                this.parseType("long"),
                value
            )
        } else if (literalExpr is NullLiteralExpr) {
            return this.newLiteral<Any?>(null, this.parseType("null"), value)
        }
        return null
    }

    private fun handleClassExpression(expr: Expression): DeclaredReferenceExpression {
        val classExpr = expr.asClassExpr()
        val type = this.parseType(classExpr.type.asString())
        val thisExpression =
            this.newDeclaredReferenceExpression(
                classExpr.toString().substring(classExpr.toString().lastIndexOf('.') + 1),
                type,
                classExpr.toString()
            )
        thisExpression.isStaticAccess = true
        frontend.setCodeAndLocation(thisExpression, classExpr)
        return thisExpression
    }

    private fun handleThisExpression(expr: Expression): DeclaredReferenceExpression {
        val thisExpr = expr.asThisExpr()
        val resolvedValueDeclaration = thisExpr.resolve()
        val type = this.parseType(resolvedValueDeclaration.qualifiedName)
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
        val thisExpression = this.newDeclaredReferenceExpression(name, type, thisExpr.toString())
        frontend.setCodeAndLocation(thisExpression, thisExpr)
        return thisExpression
    }

    private fun handleSuperExpression(expr: Expression): DeclaredReferenceExpression {
        // The actual type is hard to determine at this point, as we may not have full information
        // about the inheritance structure. Thus, we delay the resolving to the variable resolving
        // process
        val superExpression =
            this.newDeclaredReferenceExpression(
                expr.toString(),
                UnknownType.getUnknownType(language),
                expr.toString()
            )
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
        //        return newDeclaredReferenceExpression(
        //            nameExpr.getNameAsString(),
        //            new Type(((ReferenceTypeImpl) resolvedType).getQualifiedName()),
        //            nameExpr.toString());
        //      }
        //    } catch (
        //        UnsolvedSymbolException
        //            e) { // this might throw, e.g. if the type is simply not defined (i.e., syntax
        // error)
        //      return newDeclaredReferenceExpression(
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
                    TypeManager.getInstance()
                        .getTypeParameter(
                            frontend.scopeManager.currentRecord,
                            symbol.type.describe()
                        )
                if (type == null) {
                    type = this.parseType(symbol.type.describe())
                }
                this.newDeclaredReferenceExpression(symbol.name, type, nameExpr.toString())
            }
        } catch (ex: UnsolvedSymbolException) {
            val typeString: String?
            typeString =
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
                t = UnknownType.getUnknownType(language)
            } else {
                t = this.parseType(typeString)
                t.typeOrigin = Type.Origin.GUESSED
            }
            val declaredReferenceExpression =
                this.newDeclaredReferenceExpression(name, t, nameExpr.toString())
            val recordDeclaration = frontend.scopeManager.currentRecord
            if (recordDeclaration != null && recordDeclaration.name.lastPartsMatch(name)) {
                declaredReferenceExpression.refersTo = recordDeclaration
            }
            declaredReferenceExpression
        } catch (ex: RuntimeException) {
            val t = this.parseType("UNKNOWN4") // TODO: What's this? UNKNOWN4??
            log.info("Unresolved symbol: {}", nameExpr.nameAsString)
            this.newDeclaredReferenceExpression(nameExpr.nameAsString, t, nameExpr.toString())
        } catch (ex: NoClassDefFoundError) {
            val t = this.parseType("UNKNOWN4")
            log.info("Unresolved symbol: {}", nameExpr.nameAsString)
            this.newDeclaredReferenceExpression(nameExpr.nameAsString, t, nameExpr.toString())
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
            this.newLiteral(
                typeAsGoodAsPossible.typeName,
                this.parseType("class"),
                binaryExpr.typeAsString
            )
        val binaryOperator = this.newBinaryOperator("instanceof", binaryExpr.toString())
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
            this.newUnaryOperator(
                unaryExpr.operator.asString(),
                unaryExpr.isPostfix,
                unaryExpr.isPrefix,
                unaryExpr.toString()
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
        val binaryOperator =
            this.newBinaryOperator(binaryExpr.operator.asString(), binaryExpr.toString())
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

        // Or if the base is a reference to an import
        if (frontend.getQualifiedNameFromImports(qualifiedName) != null) {
            isStatic = true
        }
        val base: de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
        // the scope could either be a variable or also the class name (static call!)
        // thus, only because the scope is present, this is not automatically a member call
        if (o.isPresent) {
            val scope = o.get()
            base = handle(scope) as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?

            // If the base directly refers to a record, then this is a static call
            if (base is DeclaredReferenceExpression && base.refersTo is RecordDeclaration) {
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
                val baseName: Name?
                baseName =
                    if (resolved != null) {
                        this.parseName(resolved.declaringType().qualifiedName)
                    } else {
                        this.parseName(qualifiedName).parent
                    }
                baseType = this.parseType(baseName!!)
                base = this.newDeclaredReferenceExpression(baseName, baseType)
            } else {
                // Since it is possible to omit the "this" keyword, some methods in java do not have
                // a base.
                // However, they are still scoped to the local class, meaning we should insert an
                // implicit
                // "this" reference, to make the life for our call resolver easier.
                base = createImplicitThis()
            }
        }
        val member =
            this.newMemberExpression(name, base!!, UnknownType.getUnknownType(language), ".")
        frontend.setCodeAndLocation(
            member,
            methodCallExpr.name
        ) // This will also overwrite the code set to the empty string set above
        callExpression =
            this.newMemberCallExpression(member, isStatic, methodCallExpr.toString(), expr)
        callExpression.type = this.parseType(typeString)
        val arguments = methodCallExpr.arguments

        // handle the arguments
        for (i in arguments.indices) {
            val argument =
                handle(arguments[i])
                    as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
            argument!!.argumentIndex = i
            callExpression.addArgument(argument)
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
                ?: UnknownType.getUnknownType(language)
        base = this.newDeclaredReferenceExpression("this", thisType, "this")
        base.isImplicit = true
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

        // todo can we merge newNewExpression and newConstructExpression?
        val t = frontend.getTypeAsGoodAsPossible(objectCreationExpr.type)
        val newExpression = this.newNewExpression(expr.toString(), t)
        val arguments = objectCreationExpr.arguments
        var code = expr.toString()
        if (code.length > 4) {
            code = code.substring(4) // remove "new "
        }
        val ctor = this.newConstructExpression(code)
        ctor.type = t
        frontend.setCodeAndLocation(ctor, expr)

        // handle the arguments
        for (i in arguments.indices) {
            val argument =
                handle(arguments[i])
                    as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
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
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleAssignmentExpression(expr)
            }
        map[FieldAccessExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleFieldAccessExpression(expr)
            }
        map[LiteralExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleLiteralExpression(expr)
            }
        map[ThisExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleThisExpression(expr)
            }
        map[SuperExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleSuperExpression(expr)
            }
        map[ClassExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleClassExpression(expr)
            }
        map[NameExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleNameExpression(expr)
            }
        map[InstanceOfExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleInstanceOfExpression(expr)
            }
        map[UnaryExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleUnaryExpression(expr)
            }
        map[BinaryExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleBinaryExpression(expr)
            }
        map[VariableDeclarationExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleVariableDeclarationExpr(expr)
            }
        map[MethodCallExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleMethodCallExpression(expr)
            }
        map[ObjectCreationExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleObjectCreationExpr(expr)
            }
        map[ConditionalExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleConditionalExpression(expr)
            }
        map[EnclosedExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleEnclosedExpression(expr)
            }
        map[ArrayAccessExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleArrayAccessExpr(expr)
            }
        map[ArrayCreationExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleArrayCreationExpr(expr)
            }
        map[ArrayInitializerExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression ->
                handleArrayInitializerExpr(expr)
            }
        map[CastExpr::class.java] =
            HandlerInterface<Statement, Expression> { expr: Expression -> handleCastExpr(expr) }
    }
}
