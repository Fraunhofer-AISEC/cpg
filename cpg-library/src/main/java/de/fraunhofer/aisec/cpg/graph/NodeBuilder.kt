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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import org.slf4j.LoggerFactory

/** Builder for construction code property graph nodes. */
object NodeBuilder {
    private val LOGGER = LoggerFactory.getLogger(NodeBuilder::class.java)

    @JvmStatic
    fun newUsingDirective(code: String?, qualifiedName: String?): UsingDirective {
        val using = UsingDirective()
        using.qualifiedName = qualifiedName
        using.code = code
        log(using)
        return using
    }

    @JvmStatic
    fun newCallExpression(
        name: String?,
        fqn: String?,
        code: String?,
        template: Boolean
    ): CallExpression {
        val node = CallExpression()
        node.name = name!!
        node.code = code
        node.fqn = fqn
        node.setTemplate(template)
        log(node)
        return node
    }

    @JvmStatic
    fun newStaticCallExpression(
        name: String?,
        fqn: String?,
        code: String?,
        targetRecord: String?
    ): StaticCallExpression {
        val node = StaticCallExpression()
        node.setName(name!!)
        node.code = code
        node.fqn = fqn
        node.targetRecord = targetRecord
        log(node)
        return node
    }

    @JvmStatic
    fun newCastExpression(code: String?): CastExpression {
        val node = CastExpression()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newTypeIdExpression(
        operatorCode: String?,
        type: Type?,
        referencedType: Type?,
        code: String?
    ): TypeIdExpression {
        val node = TypeIdExpression()
        node.code = code
        node.operatorCode = operatorCode
        node.name = operatorCode!!
        node.type = type
        node.referencedType = referencedType
        log(node)
        return node
    }

    @JvmStatic
    fun newTypedefDeclaration(targetType: Type?, alias: Type?, code: String?): TypedefDeclaration {
        val node = TypedefDeclaration()
        node.type = targetType
        node.alias = alias
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newArraySubscriptionExpression(code: String?): ArraySubscriptionExpression {
        val node = ArraySubscriptionExpression()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun <T> newLiteral(value: T, type: Type?, code: String?): Literal<T> {
        val node = Literal<T>()
        node.value = value
        node.type = type
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun <T> duplicateLiteral(original: Literal<T>, implicit: Boolean): Literal<T> {
        val duplicate = newLiteral(original.value, original.type, original.code)
        duplicate.location = original.location
        duplicate.locals = original.locals
        duplicate.possibleSubTypes = original.possibleSubTypes
        duplicate.argumentIndex = original.argumentIndex
        duplicate.annotations = original.annotations
        duplicate.comment = original.comment
        duplicate.file = original.file
        duplicate.name = original.name
        duplicate.nextDFG = original.nextDFG
        duplicate.prevDFG = original.prevDFG
        duplicate.nextEOG = original.nextEOG
        duplicate.prevEOG = original.prevEOG
        duplicate.isImplicit = implicit
        return duplicate
    }

    @JvmStatic
    fun newDeclaredReferenceExpression(
        name: String?,
        typeFullName: Type?,
        code: String?
    ): DeclaredReferenceExpression {
        val node = DeclaredReferenceExpression()
        node.name = name!!
        node.type = typeFullName
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newArrayRangeExpression(
        floor: Expression?,
        ceil: Expression?,
        code: String?
    ): ArrayRangeExpression {
        val node = ArrayRangeExpression()
        node.code = code
        node.floor = floor
        node.ceiling = ceil
        log(node)
        return node
    }

    @JvmStatic
    fun newFunctionDeclaration(name: String, code: String?): FunctionDeclaration {
        val node = FunctionDeclaration()
        node.name = name
        node.code = code
        log(node)
        return node
    }

    fun log(node: Node?) {
        LOGGER.trace("Creating {}", node)
    }

    @JvmStatic
    fun newReturnStatement(code: String?): ReturnStatement {
        val node = ReturnStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newSynchronizedStatement(code: String?): SynchronizedStatement {
        val node = SynchronizedStatement()
        node.code = code
        log(node)
        return node
    }

    fun newDeleteExpression(code: String?): DeleteExpression {
        val node = DeleteExpression()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newEmptyStatement(code: String?): EmptyStatement {
        val node = EmptyStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newMethodParameterIn(
        name: String?,
        type: Type?,
        variadic: Boolean,
        code: String?
    ): ParamVariableDeclaration {
        val node = ParamVariableDeclaration()
        node.name = name!!
        node.type = type
        node.code = code
        node.isVariadic = variadic
        return node
    }

    @JvmStatic
    fun newTypeParamDeclaration(name: String?, code: String?): TypeParamDeclaration {
        val node = TypeParamDeclaration()
        node.name = name!!
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newCompoundStatement(code: String?): CompoundStatement {
        val node = CompoundStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newExpressionList(code: String?): ExpressionList {
        val node = ExpressionList()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newMemberCallExpression(
        name: String?,
        fqn: String?,
        base: Expression?,
        member: Node?,
        operatorCode: String?,
        code: String?
    ): CallExpression {
        val node = MemberCallExpression()
        node.name = name!!
        node.setBase(base)
        node.member = member
        node.operatorCode = operatorCode
        node.code = code
        node.fqn = fqn
        log(node)
        return node
    }

    @JvmStatic
    fun newTypeExpression(name: String?, type: Type?): TypeExpression {
        val node = TypeExpression()
        node.name = name!!
        node.type = type
        log(node)
        return node
    }

    @JvmStatic
    fun duplicateTypeExpression(original: TypeExpression, implicit: Boolean): TypeExpression {
        val duplicate = newTypeExpression(original.name, original.type)
        duplicate.isImplicit = implicit
        return duplicate
    }

    @JvmStatic
    fun newUnaryOperator(
        operatorType: String?,
        postfix: Boolean,
        prefix: Boolean,
        code: String?
    ): UnaryOperator {
        val node = UnaryOperator()
        node.operatorCode = operatorType
        node.name = operatorType!!
        node.isPostfix = postfix
        node.isPrefix = prefix
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newVariableDeclaration(
        name: String?,
        type: Type?,
        code: String?,
        implicitInitializerAllowed: Boolean
    ): VariableDeclaration {
        val node = VariableDeclaration()
        node.name = name!!
        node.type = type
        node.code = code
        node.isImplicitInitializerAllowed = implicitInitializerAllowed
        log(node)
        return node
    }

    @JvmStatic
    fun newDeclarationStatement(code: String?): DeclarationStatement {
        val node = DeclarationStatement()
        node.code = code
        return node
    }

    @JvmStatic
    fun newIfStatement(code: String?): IfStatement {
        val node = IfStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newLabelStatement(code: String?): LabelStatement {
        val node = LabelStatement()
        node.code = code
        log(node)
        return node
    }

    fun newGotoStatement(code: String?): GotoStatement {
        val node = GotoStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newWhileStatement(code: String?): WhileStatement {
        val node = WhileStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newDoStatement(code: String?): DoStatement {
        val node = DoStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newForEachStatement(code: String?): ForEachStatement {
        val node = ForEachStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newForStatement(code: String?): ForStatement {
        val node = ForStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newContinueStatement(code: String?): ContinueStatement {
        val node = ContinueStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newBreakStatement(code: String?): BreakStatement {
        val node = BreakStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newBinaryOperator(operatorCode: String, code: String?): BinaryOperator {
        val node = BinaryOperator()
        node.operatorCode = operatorCode
        node.name = operatorCode
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newTranslationUnitDeclaration(name: String?, code: String?): TranslationUnitDeclaration {
        val node = TranslationUnitDeclaration()
        node.name = name!!
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newRecordDeclaration(
        fqn: String,
        kind: String,
        code: String?,
        createThis: Boolean = true
    ): RecordDeclaration {
        val node = RecordDeclaration()
        node.name = fqn
        node.kind = kind
        node.code = code

        if (kind == "class" && createThis) {
            val thisDeclaration =
                newFieldDeclaration(
                    "this",
                    TypeParser.createFrom(fqn, true),
                    listOf(),
                    "this",
                    null,
                    null,
                    true
                )
            node.addField(thisDeclaration)
        }

        log(node)

        return node
    }

    @JvmStatic
    fun newEnumDeclaration(
        name: String?,
        code: String?,
        location: PhysicalLocation?
    ): EnumDeclaration {
        val node = EnumDeclaration()
        node.name = name!!
        node.code = code
        node.location = location
        log(node)
        return node
    }

    @JvmStatic
    fun newFunctionTemplateDeclaration(name: String?, code: String?): FunctionTemplateDeclaration {
        val node = FunctionTemplateDeclaration()
        node.name = name!!
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newClassTemplateDeclaration(name: String?, code: String?): ClassTemplateDeclaration {
        val node = ClassTemplateDeclaration()
        node.name = name!!
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newEnumConstantDeclaration(
        name: String?,
        code: String?,
        location: PhysicalLocation?
    ): EnumConstantDeclaration {
        val node = EnumConstantDeclaration()
        node.name = name!!
        node.code = code
        node.location = location
        log(node)
        return node
    }

    @JvmStatic
    fun newFieldDeclaration(
        name: String?,
        type: Type?,
        modifiers: List<String?>?,
        code: String?,
        location: PhysicalLocation?,
        initializer: Expression?,
        implicitInitializerAllowed: Boolean
    ): FieldDeclaration {
        val node = FieldDeclaration()
        node.name = name!!
        node.type = type
        node.modifiers = modifiers
        node.code = code
        node.location = location
        node.isImplicitInitializerAllowed = implicitInitializerAllowed
        if (initializer != null) {
            if (initializer is ArrayCreationExpression) {
                node.setIsArray(true)
            }
            node.initializer = initializer
        }
        log(node)
        return node
    }

    @JvmStatic
    fun newMemberExpression(
        base: Expression?,
        memberType: Type?,
        name: String?,
        operatorCode: String?,
        code: String?
    ): MemberExpression {
        val node = MemberExpression()
        node.setBase(base!!)
        node.operatorCode = operatorCode
        node.code = code
        node.name = name!!
        node.type = memberType
        log(node)
        return node
    }

    @JvmStatic
    fun newStatement(code: String?): Statement {
        val node = Statement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newExpression(code: String?): Expression {
        val node = Expression()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newInitializerListExpression(code: String?): InitializerListExpression {
        val node = InitializerListExpression()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newDesignatedInitializerExpression(code: String?): DesignatedInitializerExpression {
        val node = DesignatedInitializerExpression()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newArrayCreationExpression(code: String?): ArrayCreationExpression {
        val node = ArrayCreationExpression()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newConstructExpression(code: String?): ConstructExpression {
        val node = ConstructExpression()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newMethodDeclaration(
        name: String?,
        code: String?,
        isStatic: Boolean,
        recordDeclaration: RecordDeclaration?
    ): MethodDeclaration {
        val node = MethodDeclaration()
        node.name = name!!
        node.code = code
        node.isStatic = isStatic
        node.recordDeclaration = recordDeclaration
        log(node)
        return node
    }

    @JvmStatic
    fun newConstructorDeclaration(
        name: String?,
        code: String?,
        recordDeclaration: RecordDeclaration?
    ): ConstructorDeclaration {
        val node = ConstructorDeclaration()
        node.name = name!!
        node.code = code
        node.recordDeclaration = recordDeclaration
        log(node)
        return node
    }

    @JvmStatic
    fun newProblemDeclaration(
        filename: String?,
        problem: String?,
        problemLocation: String?
    ): ProblemDeclaration {
        val node = ProblemDeclaration()
        node.filename = filename
        node.problem = problem
        node.problemLocation = problemLocation
        log(node)
        return node
    }

    @JvmStatic
    fun newIncludeDeclaration(includeFilename: String): IncludeDeclaration {
        val node = IncludeDeclaration()
        val name = includeFilename.substring(includeFilename.lastIndexOf('/') + 1)
        node.name = name
        node.filename = includeFilename
        log(node)
        return node
    }

    @JvmStatic
    fun newNewExpression(code: String?, type: Type?): NewExpression {
        val node = NewExpression()
        node.code = code
        node.type = type
        log(node)
        return node
    }

    @JvmStatic
    fun newSwitchStatement(code: String?): SwitchStatement {
        val node = SwitchStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newCaseStatement(code: String?): CaseStatement {
        val node = CaseStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newDefaultStatement(code: String?): DefaultStatement {
        val node = DefaultStatement()
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newConditionalExpression(
        condition: Expression?,
        thenExpr: Expression?,
        elseExpr: Expression?,
        type: Type?
    ): ConditionalExpression {
        val node = ConditionalExpression()
        node.condition = condition
        node.thenExpr = thenExpr
        node.elseExpr = elseExpr
        node.type = type
        log(node)
        return node
    }

    @JvmStatic
    fun newExplicitConstructorInvocation(
        containingClass: String?,
        code: String?
    ): ExplicitConstructorInvocation {
        val node = ExplicitConstructorInvocation()
        node.containingClass = containingClass
        node.code = code
        log(node)
        return node
    }

    /**
     * Creates a new namespace declaration.
     *
     * @param fqn the FQN
     * @param code
     * @return
     */
    @JvmStatic
    fun newNamespaceDeclaration(fqn: String, code: String?): NamespaceDeclaration {
        val node = NamespaceDeclaration()
        node.name = fqn
        node.code = code
        log(node)
        return node
    }

    @JvmStatic
    fun newCatchClause(code: String): CatchClause {
        val catchClause = CatchClause()
        catchClause.code = code
        return catchClause
    }

    @JvmStatic
    fun newTryStatement(code: String): TryStatement {
        val tryStatement = TryStatement()
        tryStatement.code = code
        return tryStatement
    }

    @JvmStatic
    fun newAssertStatement(code: String): AssertStatement {
        val assertStatement = AssertStatement()
        assertStatement.code = code
        return assertStatement
    }

    @JvmStatic
    fun newASMDeclarationStatement(code: String): ASMDeclarationStatement {
        val asmStatement = ASMDeclarationStatement()
        asmStatement.code = code
        return asmStatement
    }

    @JvmStatic
    fun newCompoundStatementExpression(code: String): CompoundStatementExpression {
        val cse = CompoundStatementExpression()
        cse.code = code
        return cse
    }

    @JvmStatic
    fun newAnnotation(name: String?, code: String): Annotation {
        val annotation = Annotation()
        annotation.name = name!!
        annotation.code = code
        return annotation
    }

    @JvmStatic
    fun newAnnotationMember(name: String?, value: Expression?, code: String): AnnotationMember {
        val member = AnnotationMember()
        member.name = name!!
        member.value = value
        member.code = code
        return member
    }

    @JvmStatic
    fun newKeyValueExpression(
        key: Expression?,
        value: Expression?,
        code: String?
    ): KeyValueExpression {
        val keyValue = KeyValueExpression()
        keyValue.key = key
        keyValue.value = value
        keyValue.code = code
        return keyValue
    }

    fun newLambdaExpression(code: String?): LambdaExpression {
        val lambda = LambdaExpression()
        lambda.code = code
        return lambda
    }
}
