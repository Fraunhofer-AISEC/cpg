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

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
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
    @JvmOverloads
    fun newUsingDirective(
        code: String? = null,
        qualifiedName: String?,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): UsingDirective {
        val using = UsingDirective()
        using.qualifiedName = qualifiedName

        lang?.setCodeAndRegion(using, rawNode)
        if (code != null) {
            using.code = code
        }
        log(using)
        return using
    }

    @JvmStatic
    @JvmOverloads
    fun newCallExpression(
        name: String?,
        fqn: String?,
        code: String? = null,
        template: Boolean,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CallExpression {
        val node = CallExpression()
        node.name = name!!
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        node.fqn = fqn
        node.setTemplate(template)
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newStaticCallExpression(
        name: String?,
        fqn: String?,
        code: String? = null,
        targetRecord: String?,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): StaticCallExpression {
        val node = StaticCallExpression()
        node.setName(name!!)
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        node.fqn = fqn
        node.targetRecord = targetRecord
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newCastExpression(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CastExpression {
        val node = CastExpression()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newTypeIdExpression(
        operatorCode: String?,
        type: Type?,
        referencedType: Type?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): TypeIdExpression {
        val node = TypeIdExpression()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        node.operatorCode = operatorCode
        node.name = operatorCode!!
        node.type = type
        node.referencedType = referencedType
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newTypedefDeclaration(
        targetType: Type?,
        alias: Type?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): TypedefDeclaration {
        val node = TypedefDeclaration()
        node.type = targetType
        node.alias = alias
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newArraySubscriptionExpression(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ArraySubscriptionExpression {
        val node = ArraySubscriptionExpression()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun <T> newLiteral(
        value: T,
        type: Type?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): Literal<T> {
        val node = Literal<T>()
        node.value = value
        node.type = type
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
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
    @JvmOverloads
    fun newDeclaredReferenceExpression(
        name: String?,
        typeFullName: Type?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): DeclaredReferenceExpression {
        val node = DeclaredReferenceExpression()
        node.name = name!!
        node.type = typeFullName
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newArrayRangeExpression(
        floor: Expression?,
        ceil: Expression?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ArrayRangeExpression {
        val node = ArrayRangeExpression()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        node.floor = floor
        node.ceiling = ceil
        log(node)
        return node
    }

    @JvmOverloads
    @JvmStatic
    fun newFunctionDeclaration(
        name: String,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): FunctionDeclaration {
        val node = FunctionDeclaration()
        node.name = name

        lang?.setCodeAndRegion(node, rawNode)

        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)

        return node
    }

    fun log(node: Node?) {
        LOGGER.trace("Creating {}", node)
    }

    @JvmStatic
    @JvmOverloads
    fun newReturnStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ReturnStatement {
        val node = ReturnStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newSynchronizedStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): SynchronizedStatement {
        val node = SynchronizedStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newDeleteExpression(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): DeleteExpression {
        val node = DeleteExpression()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newEmptyStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): EmptyStatement {
        val node = EmptyStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newMethodParameterIn(
        name: String?,
        type: Type?,
        variadic: Boolean,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ParamVariableDeclaration {
        val node = ParamVariableDeclaration()
        node.name = name!!
        node.type = type
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        node.isVariadic = variadic
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newTypeParamDeclaration(
        name: String?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): TypeParamDeclaration {
        val node = TypeParamDeclaration()
        node.name = name!!
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newCompoundStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CompoundStatement {
        val node = CompoundStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newExpressionList(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ExpressionList {
        val node = ExpressionList()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newMemberCallExpression(
        name: String?,
        fqn: String?,
        base: Expression?,
        member: Node?,
        operatorCode: String?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CallExpression {
        val node = MemberCallExpression()
        node.name = name!!
        node.setBase(base)
        node.member = member
        node.operatorCode = operatorCode
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        node.fqn = fqn
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newTypeExpression(
        name: String?,
        type: Type?,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): TypeExpression {
        val node = TypeExpression()
        node.name = name!!
        node.type = type
        lang?.setCodeAndRegion(node, rawNode)
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun duplicateTypeExpression(original: TypeExpression, implicit: Boolean): TypeExpression {
        val duplicate = newTypeExpression(original.name, original.type)
        duplicate.isImplicit = implicit
        return duplicate
    }

    @JvmStatic
    @JvmOverloads
    fun newUnaryOperator(
        operatorType: String?,
        postfix: Boolean,
        prefix: Boolean,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): UnaryOperator {
        val node = UnaryOperator()
        node.operatorCode = operatorType
        node.name = operatorType!!
        node.isPostfix = postfix
        node.isPrefix = prefix
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newVariableDeclaration(
        name: String?,
        type: Type?,
        code: String? = null,
        implicitInitializerAllowed: Boolean,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): VariableDeclaration {
        val node = VariableDeclaration()
        node.name = name!!
        node.type = type

        lang?.setCodeAndRegion(node, rawNode)

        if (code != null) {
            node.code = code
        }

        node.isImplicitInitializerAllowed = implicitInitializerAllowed
        log(node)

        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newDeclarationStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): DeclarationStatement {
        val node = DeclarationStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newIfStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): IfStatement {
        val node = IfStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newLabelStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): LabelStatement {
        val node = LabelStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newGotoStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): GotoStatement {
        val node = GotoStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newWhileStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): WhileStatement {
        val node = WhileStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newDoStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): DoStatement {
        val node = DoStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newForEachStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ForEachStatement {
        val node = ForEachStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newForStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ForStatement {
        val node = ForStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newContinueStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ContinueStatement {
        val node = ContinueStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newBreakStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): BreakStatement {
        val node = BreakStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newBinaryOperator(
        operatorCode: String,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): BinaryOperator {
        val node = BinaryOperator()
        node.operatorCode = operatorCode
        node.name = operatorCode
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newTranslationUnitDeclaration(
        name: String?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): TranslationUnitDeclaration {
        val node = TranslationUnitDeclaration()
        node.name = name!!
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newRecordDeclaration(
        fqn: String,
        kind: String,
        code: String? = null,
        createThis: Boolean = true,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): RecordDeclaration {
        val node = RecordDeclaration()
        node.name = fqn
        node.kind = kind

        lang?.setCodeAndRegion(node, rawNode)

        if (code != null) {
            node.code = code
        }

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
    @JvmOverloads
    fun newEnumDeclaration(
        name: String?,
        code: String? = null,
        location: PhysicalLocation?,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): EnumDeclaration {
        val node = EnumDeclaration()
        node.name = name!!
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        node.location = location
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newFunctionTemplateDeclaration(
        name: String?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): FunctionTemplateDeclaration {
        val node = FunctionTemplateDeclaration()
        node.name = name!!
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newClassTemplateDeclaration(
        name: String?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ClassTemplateDeclaration {
        val node = ClassTemplateDeclaration()
        node.name = name!!
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newEnumConstantDeclaration(
        name: String?,
        code: String? = null,
        location: PhysicalLocation?,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): EnumConstantDeclaration {
        val node = EnumConstantDeclaration()
        node.name = name!!
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        node.location = location
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newFieldDeclaration(
        name: String?,
        type: Type?,
        modifiers: List<String?>?,
        code: String? = null,
        location: PhysicalLocation?,
        initializer: Expression?,
        implicitInitializerAllowed: Boolean,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): FieldDeclaration {
        val node = FieldDeclaration()
        node.name = name!!
        node.type = type
        node.modifiers = modifiers
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
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
    @JvmOverloads
    fun newMemberExpression(
        base: Expression?,
        memberType: Type?,
        name: String?,
        operatorCode: String?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): MemberExpression {
        val node = MemberExpression()
        node.setBase(base!!)
        node.operatorCode = operatorCode
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        node.name = name!!
        node.type = memberType
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): Statement {
        val node = Statement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newExpression(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): Expression {
        val node = Expression()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newInitializerListExpression(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): InitializerListExpression {
        val node = InitializerListExpression()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newDesignatedInitializerExpression(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): DesignatedInitializerExpression {
        val node = DesignatedInitializerExpression()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newArrayCreationExpression(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ArrayCreationExpression {
        val node = ArrayCreationExpression()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newConstructExpression(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ConstructExpression {
        val node = ConstructExpression()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmOverloads
    @JvmStatic
    fun newMethodDeclaration(
        name: String?,
        code: String? = null,
        isStatic: Boolean,
        recordDeclaration: RecordDeclaration?,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): MethodDeclaration {
        val node = MethodDeclaration()
        node.name = name!!
        node.isStatic = isStatic
        node.recordDeclaration = recordDeclaration

        lang?.setCodeAndRegion(node, rawNode)

        if (code != null) {
            node.code = code
        }

        log(node)
        return node
    }

    @JvmOverloads
    @JvmStatic
    fun newConstructorDeclaration(
        name: String?,
        code: String? = null,
        recordDeclaration: RecordDeclaration?,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ConstructorDeclaration {
        val node = ConstructorDeclaration()
        node.name = name!!
        node.recordDeclaration = recordDeclaration

        lang?.setCodeAndRegion(node, rawNode)

        if (code != null) {
            node.code = code
        }

        log(node)

        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newProblemDeclaration(
        filename: String?,
        problem: String?,
        problemLocation: String?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ProblemDeclaration {
        val node = ProblemDeclaration()
        node.filename = filename
        node.problem = problem
        node.problemLocation = problemLocation
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newIncludeDeclaration(
        includeFilename: String,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): IncludeDeclaration {
        val node = IncludeDeclaration()
        val name = includeFilename.substring(includeFilename.lastIndexOf('/') + 1)
        node.name = name
        node.filename = includeFilename
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newNewExpression(
        code: String? = null,
        type: Type?,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): NewExpression {
        val node = NewExpression()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        node.type = type
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newSwitchStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): SwitchStatement {
        val node = SwitchStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newCaseStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CaseStatement {
        val node = CaseStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newDefaultStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): DefaultStatement {
        val node = DefaultStatement()
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newConditionalExpression(
        condition: Expression?,
        thenExpr: Expression?,
        elseExpr: Expression?,
        type: Type?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ConditionalExpression {
        val node = ConditionalExpression()
        node.condition = condition
        node.thenExpr = thenExpr
        node.elseExpr = elseExpr
        node.type = type
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newExplicitConstructorInvocation(
        containingClass: String?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ExplicitConstructorInvocation {
        val node = ExplicitConstructorInvocation()
        node.containingClass = containingClass
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
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
    @JvmOverloads
    fun newNamespaceDeclaration(
        fqn: String,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): NamespaceDeclaration {
        val node = NamespaceDeclaration()
        node.name = fqn
        lang?.setCodeAndRegion(node, rawNode)
        if (code != null) {
            node.code = code
        }
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newCatchClause(
        code: String,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CatchClause {
        val catchClause = CatchClause()
        lang?.setCodeAndRegion(catchClause, rawNode)
        if (code != null) {
            catchClause.code = code
        }
        return catchClause
    }

    @JvmStatic
    @JvmOverloads
    fun newTryStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): TryStatement {
        val tryStatement = TryStatement()
        lang?.setCodeAndRegion(tryStatement, rawNode)
        if (code != null) {
            tryStatement.code = code
        }
        return tryStatement
    }

    @JvmStatic
    @JvmOverloads
    fun newAssertStatement(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): AssertStatement {
        val assertStatement = AssertStatement()
        lang?.setCodeAndRegion(assertStatement, rawNode)
        if (code != null) {
            assertStatement.code = code
        }
        return assertStatement
    }

    @JvmStatic
    @JvmOverloads
    fun newASMDeclarationStatement(
        code: String,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ASMDeclarationStatement {
        val asmStatement = ASMDeclarationStatement()
        lang?.setCodeAndRegion(asmStatement, rawNode)
        if (code != null) {
            asmStatement.code = code
        }
        return asmStatement
    }

    @JvmStatic
    @JvmOverloads
    fun newCompoundStatementExpression(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CompoundStatementExpression {
        val cse = CompoundStatementExpression()
        lang?.setCodeAndRegion(cse, rawNode)
        if (code != null) {
            cse.code = code
        }
        return cse
    }

    @JvmStatic
    @JvmOverloads
    fun newAnnotation(
        name: String?,
        code: String,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): Annotation {
        val annotation = Annotation()
        annotation.name = name!!
        lang?.setCodeAndRegion(annotation, rawNode)
        if (code != null) {
            annotation.code = code
        }
        return annotation
    }

    @JvmStatic
    @JvmOverloads
    fun newAnnotationMember(
        name: String?,
        value: Expression?,
        code: String,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): AnnotationMember {
        val member = AnnotationMember()
        member.name = name!!
        member.value = value
        lang?.setCodeAndRegion(member, rawNode)
        if (code != null) {
            member.code = code
        }
        return member
    }

    @JvmStatic
    @JvmOverloads
    fun newKeyValueExpression(
        key: Expression?,
        value: Expression?,
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): KeyValueExpression {
        val keyValue = KeyValueExpression()
        keyValue.key = key
        keyValue.value = value
        lang?.setCodeAndRegion(keyValue, rawNode)
        if (code != null) {
            keyValue.code = code
        }
        return keyValue
    }

    @JvmStatic
    @JvmOverloads
    fun newLambdaExpression(
        code: String? = null,
        lang: LanguageFrontend? = null,
        rawNode: Any? = null
    ): LambdaExpression {
        val lambda = LambdaExpression()
        lang?.setCodeAndRegion(lambda, rawNode)
        if (code != null) {
            lambda.code = code
        }
        return lambda
    }
}
