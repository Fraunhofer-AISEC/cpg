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

import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.log
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.inference.IsInferredProvider
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import org.slf4j.LoggerFactory

/** Builder for construction code property graph nodes. */
object NodeBuilder {
    private val LOGGER = LoggerFactory.getLogger(NodeBuilder::class.java)

    @JvmStatic
    @JvmOverloads
    fun newUsingDirective(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        qualifiedName: String?,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): UsingDirective {
        val node = UsingDirective()
        node.qualifiedName = qualifiedName
        node.applyMetadata(frontend, rawNode, code)
        node.language = language

        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newCallExpression(
        callee: Expression?,
        fqn: String?,
        language: Language<out LanguageFrontend>,
        code: String? = null,
        template: Boolean,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CallExpression {
        val node = CallExpression()
        node.callee = callee
        node.applyMetadata(frontend, rawNode, code)
        node.fqn = fqn
        node.template = template
        node.language = language
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
        language: Language<out LanguageFrontend>,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): StaticCallExpression {
        val node = StaticCallExpression()
        node.setName(name!!)
        node.applyMetadata(frontend, rawNode, code)
        node.fqn = fqn
        node.targetRecord = targetRecord
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newCastExpression(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CastExpression {
        val node = CastExpression()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newTypeIdExpression(
        operatorCode: String?,
        type: Type?,
        referencedType: Type?,
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): TypeIdExpression {
        val node = TypeIdExpression()
        node.applyMetadata(frontend, rawNode, code)
        node.operatorCode = operatorCode
        node.name = operatorCode!!
        node.type = type
        node.referencedType = referencedType
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newArraySubscriptionExpression(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ArraySubscriptionExpression {
        val node = ArraySubscriptionExpression()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language

        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newDeclaredReferenceExpression(
        name: String?,
        language: Language<out LanguageFrontend>,
        type: Type? = UnknownType.getUnknownType(),
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): DeclaredReferenceExpression {
        val node = DeclaredReferenceExpression()
        node.name = name!!
        node.type = type
        node.applyMetadata(frontend, rawNode, code)
        node.language = language

        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newArrayRangeExpression(
        floor: Expression?,
        ceil: Expression?,
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ArrayRangeExpression {
        val node = ArrayRangeExpression()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language

        node.floor = floor
        node.ceiling = ceil
        log(node)
        return node
    }

    fun log(node: Node?) {
        LOGGER.trace("Creating {}", node)
    }

    @JvmStatic
    @JvmOverloads
    fun newSynchronizedStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): SynchronizedStatement {
        val node = SynchronizedStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language

        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newDeleteExpression(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): DeleteExpression {
        val node = DeleteExpression()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language

        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newEmptyStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): EmptyStatement {
        val node = EmptyStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newCompoundStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CompoundStatement {
        val node = CompoundStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language

        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newExpressionList(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ExpressionList {
        val node = ExpressionList()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language

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
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CallExpression {
        val node = MemberCallExpression()
        node.name = name!!
        node.base = base
        node.member = member
        node.operatorCode = operatorCode
        node.applyMetadata(frontend, rawNode, code)
        node.language = language

        node.fqn = fqn
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newTypeExpression(
        name: String?,
        type: Type?,
        language: Language<out LanguageFrontend>,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): TypeExpression {
        val node = TypeExpression()
        node.name = name!!
        node.type = type
        node.applyMetadata(frontend, rawNode, null)
        node.language = language

        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newDeclarationStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): DeclarationStatement {
        val node = DeclarationStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language

        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newIfStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): IfStatement {
        val node = IfStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language

        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newLabelStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): LabelStatement {
        val node = LabelStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language

        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newGotoStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): GotoStatement {
        val node = GotoStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newWhileStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): WhileStatement {
        val node = WhileStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newDoStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): DoStatement {
        val node = DoStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newForEachStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ForEachStatement {
        val node = ForEachStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newForStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ForStatement {
        val node = ForStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newContinueStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ContinueStatement {
        val node = ContinueStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newBreakStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): BreakStatement {
        val node = BreakStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newRecordDeclaration(
        fqn: String,
        kind: String,
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): RecordDeclaration {
        val node = RecordDeclaration()
        node.name = fqn
        node.kind = kind
        node.language = language

        node.applyMetadata(frontend, rawNode, code)

        if (code != null) {
            node.code = code
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
        language: Language<out LanguageFrontend>,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): EnumDeclaration {
        val node = EnumDeclaration()
        node.name = name!!
        node.applyMetadata(frontend, rawNode, code)
        node.location = location
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newFunctionTemplateDeclaration(
        name: String?,
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): FunctionTemplateDeclaration {
        val node = FunctionTemplateDeclaration()
        node.name = name!!
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newClassTemplateDeclaration(
        name: String?,
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ClassTemplateDeclaration {
        val node = ClassTemplateDeclaration()
        node.name = name!!
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newEnumConstantDeclaration(
        name: String?,
        code: String? = null,
        location: PhysicalLocation?,
        language: Language<out LanguageFrontend>,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): EnumConstantDeclaration {
        val node = EnumConstantDeclaration()
        node.name = name!!
        node.applyMetadata(frontend, rawNode, code)
        node.location = location
        node.language = language
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
        language: Language<out LanguageFrontend>,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): FieldDeclaration {
        val node = FieldDeclaration()
        node.name = name!!
        node.type = type
        node.modifiers = modifiers
        node.applyMetadata(frontend, rawNode, code)
        node.location = location
        node.isImplicitInitializerAllowed = implicitInitializerAllowed
        if (initializer != null) {
            if (initializer is ArrayCreationExpression) {
                node.setIsArray(true)
            }
            node.initializer = initializer
        }
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newMemberExpression(
        base: Expression?,
        memberType: Type?,
        name: String?,
        language: Language<out LanguageFrontend>,
        operatorCode: String?,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): MemberExpression {
        val node = MemberExpression()
        node.setBase(base!!)
        node.operatorCode = operatorCode
        node.applyMetadata(frontend, rawNode, code)
        node.name = name!!
        node.type = memberType
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): Statement {
        val node = ProblemExpression()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newExpression(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): Expression {
        val node = ProblemExpression()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newInitializerListExpression(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): InitializerListExpression {
        val node = InitializerListExpression()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newDesignatedInitializerExpression(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): DesignatedInitializerExpression {
        val node = DesignatedInitializerExpression()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newArrayCreationExpression(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ArrayCreationExpression {
        val node = ArrayCreationExpression()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newConstructExpression(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ConstructExpression {
        val node = ConstructExpression()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newProblemDeclaration(
        language: Language<out LanguageFrontend>,
        problem: String = "",
        type: ProblemNode.ProblemType = ProblemNode.ProblemType.PARSING,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ProblemDeclaration {
        val node = ProblemDeclaration()
        node.problem = problem
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newProblemExpression(
        language: Language<out LanguageFrontend>,
        problem: String = "",
        type: ProblemNode.ProblemType = ProblemNode.ProblemType.PARSING,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ProblemExpression {
        val node = ProblemExpression(problem, type)
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newIncludeDeclaration(
        includeFilename: String,
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): IncludeDeclaration {
        val node = IncludeDeclaration()
        val name = includeFilename.substring(includeFilename.lastIndexOf('/') + 1)
        node.name = name
        node.filename = includeFilename
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newNewExpression(
        code: String? = null,
        type: Type?,
        language: Language<out LanguageFrontend>,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): NewExpression {
        val node = NewExpression()
        node.applyMetadata(frontend, rawNode, code)
        node.type = type
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newSwitchStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): SwitchStatement {
        val node = SwitchStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newCaseStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CaseStatement {
        val node = CaseStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newDefaultStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): DefaultStatement {
        val node = DefaultStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
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
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ConditionalExpression {
        val node = ConditionalExpression()
        node.condition = condition
        node.thenExpr = thenExpr
        node.elseExpr = elseExpr
        node.type = type
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newExplicitConstructorInvocation(
        containingClass: String?,
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ExplicitConstructorInvocation {
        val node = ExplicitConstructorInvocation()
        node.containingClass = containingClass
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    /** Creates a new namespace declaration. */
    @JvmStatic
    @JvmOverloads
    fun newNamespaceDeclaration(
        fqn: String,
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): NamespaceDeclaration {
        val node = NamespaceDeclaration()
        node.name = fqn
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        log(node)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newCatchClause(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CatchClause {
        val node = CatchClause()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newTryStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): TryStatement {
        val node = TryStatement()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newAssertStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): AssertStatement {
        val node = AssertStatement()
        node.applyMetadata(frontend, rawNode, code)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newASMDeclarationStatement(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): ASMDeclarationStatement {
        val node = ASMDeclarationStatement()
        node.applyMetadata(frontend, rawNode, code)
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newCompoundStatementExpression(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): CompoundStatementExpression {
        val cse = CompoundStatementExpression()
        frontend?.setCodeAndLocation(cse, rawNode)
        if (code != null) {
            cse.code = code
        }
        cse.language = language
        return cse
    }

    @JvmStatic
    @JvmOverloads
    fun newAnnotation(
        name: String?,
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): Annotation {
        val node = Annotation()
        node.name = name!!
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newAnnotationMember(
        name: String?,
        value: Expression?,
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): AnnotationMember {
        val node = AnnotationMember()
        node.name = name!!
        node.value = value
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newKeyValueExpression(
        key: Expression?,
        value: Expression?,
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): KeyValueExpression {
        val node = KeyValueExpression()
        node.key = key
        node.value = value
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        return node
    }

    @JvmStatic
    @JvmOverloads
    fun newLambdaExpression(
        language: Language<out LanguageFrontend>,
        code: String? = null,
        frontend: LanguageFrontend? = null,
        rawNode: Any? = null
    ): LambdaExpression {
        val node = LambdaExpression()
        node.applyMetadata(frontend, rawNode, code)
        node.language = language
        return node
    }
}

/**
 * Applies various metadata on this [Node], based on the kind of provider in [provider]. This can
 * include:
 * - Setting [Node.code] and [Node.location], if a [CodeAndLocationProvider] is given
 * - Setting [Node.location], if a [LanguageProvider] is given
 * - Setting [Node.isInferred], if an [IsInferredProvider] is given
 *
 * Note, that one provider can implement multiple provider interfaces. Additionally, if
 * [codeOverride] is specified, the supplied source code is used to override anything from the
 * provider.
 */
fun Node.applyMetadata(provider: MetadataProvider?, rawNode: Any?, codeOverride: String?) {
    if (provider is CodeAndLocationProvider) {
        provider.setCodeAndLocation(this, rawNode)
    }

    if (provider is LanguageProvider) {
        this.language = provider.language
    }

    if (provider is IsInferredProvider) {
        this.isInferred = provider.isInferred
    }

    if (codeOverride != null) {
        this.code = codeOverride
    }
}

@JvmOverloads
fun MetadataProvider.newBinaryOperator(
    operatorCode: String,
    code: String? = null,
    rawNode: Any? = null
): BinaryOperator {
    val node = BinaryOperator()
    node.applyMetadata(this, rawNode, code)

    node.name = operatorCode
    node.operatorCode = operatorCode

    log(node)

    return node
}

@JvmOverloads
fun MetadataProvider.newUnaryOperator(
    operatorType: String,
    postfix: Boolean,
    prefix: Boolean,
    code: String? = null,
    rawNode: Any? = null
): UnaryOperator {
    val node = UnaryOperator()
    node.applyMetadata(this, rawNode, code)

    node.operatorCode = operatorType
    node.name = operatorType
    node.isPostfix = postfix
    node.isPrefix = prefix

    log(node)

    return node
}

@JvmOverloads
fun Handler<*, *, *>.newReturnStatement(
    code: String? = null,
    rawNode: Any? = null
): ReturnStatement {
    val node = ReturnStatement()
    node.language = this.frontend.language
    node.applyMetadata(this.frontend, rawNode, code)

    log(node)
    return node
}

@JvmOverloads
fun <T> Handler<*, *, *>.newLiteral(
    value: T,
    type: Type?,
    code: String? = null,
    rawNode: Any? = null
): Literal<T> {
    return this.frontend.newLiteral(value, type, code, rawNode)
}

fun <T> LanguageFrontend.newLiteral(
    value: T,
    type: Type?,
    code: String? = null,
    rawNode: Any? = null,
): Literal<T> {
    val node = Literal<T>()
    node.value = value
    node.type = type
    node.applyMetadata(this, rawNode, code)
    node.language = this.language

    log(node)
    return node
}

fun <T> Literal<T>.duplicate(implicit: Boolean): Literal<T> {
    val duplicate = Literal<T>()
    duplicate.language = this.language
    duplicate.value = this.value
    duplicate.type = this.type
    duplicate.code = this.code
    duplicate.location = this.location
    duplicate.locals = this.locals
    duplicate.possibleSubTypes = this.possibleSubTypes
    duplicate.argumentIndex = this.argumentIndex
    duplicate.annotations = this.annotations
    duplicate.comment = this.comment
    duplicate.file = this.file
    duplicate.name = this.name
    duplicate.nextDFG = this.nextDFG
    duplicate.prevDFG = this.prevDFG
    duplicate.nextEOG = this.nextEOG
    duplicate.prevEOG = this.prevEOG
    duplicate.isImplicit = implicit
    return duplicate
}

fun TypeExpression.duplicate(implicit: Boolean): TypeExpression {
    val duplicate = TypeExpression()
    duplicate.name = this.name
    duplicate.type = this.type
    duplicate.language = this.language
    duplicate.isImplicit = implicit
    return duplicate
}

/**
 * Creates a new [TranslationUnitDeclaration]. This is the top-most [Node] that a [LanguageFrontend]
 * or [Handler] should create. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
fun MetadataProvider.newTranslationUnitDeclaration(
    name: String,
    code: String? = null,
    rawNode: Any? = null
): TranslationUnitDeclaration {
    val node = TranslationUnitDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name

    log(node)
    return node
}

/**
 * Creates a new [FunctionDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newFunctionDeclaration(
    name: String,
    code: String? = null,
    rawNode: Any? = null
): FunctionDeclaration {
    val node = FunctionDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name

    log(node)
    return node
}

/**
 * Creates a new [MethodDeclaration]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newMethodDeclaration(
    name: String,
    code: String? = null,
    isStatic: Boolean,
    recordDeclaration: RecordDeclaration?,
    rawNode: Any? = null
): MethodDeclaration {
    val node = MethodDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name
    node.isStatic = isStatic
    node.recordDeclaration = recordDeclaration

    log(node)
    return node
}

/**
 * Creates a new [ConstructorDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newConstructorDeclaration(
    name: String,
    code: String? = null,
    recordDeclaration: RecordDeclaration?,
    rawNode: Any? = null
): ConstructorDeclaration {
    val node = ConstructorDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name
    node.recordDeclaration = recordDeclaration

    log(node)
    return node
}

/**
 * Creates a new [MethodDeclaration]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newParamVariableDeclaration(
    name: String,
    type: Type?,
    variadic: Boolean,
    code: String? = null,
    frontend: LanguageFrontend? = null,
    rawNode: Any? = null
): ParamVariableDeclaration {
    val node = ParamVariableDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name
    node.type = type
    node.isVariadic = variadic

    log(node)
    return node
}

/**
 * Creates a new [VariableDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newVariableDeclaration(
    name: String,
    type: Type?,
    code: String? = null,
    implicitInitializerAllowed: Boolean,
    rawNode: Any? = null
): VariableDeclaration {
    val node = VariableDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name
    node.type = type
    node.isImplicitInitializerAllowed = implicitInitializerAllowed

    log(node)
    return node
}

/**
 * Creates a new [TypedefDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newTypedefDeclaration(
    targetType: Type?,
    alias: Type,
    code: String? = null,
    rawNode: Any? = null
): TypedefDeclaration {
    val node = TypedefDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = alias.typeName
    node.type = targetType
    node.alias = alias

    log(node)
    return node
}

/**
 * Creates a new [TypeParamDeclaration]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newTypeParamDeclaration(
    name: String,
    code: String? = null,
    rawNode: Any? = null
): TypeParamDeclaration {
    val node = TypeParamDeclaration()
    node.applyMetadata(this, rawNode, code)

    node.name = name

    log(node)
    return node
}
