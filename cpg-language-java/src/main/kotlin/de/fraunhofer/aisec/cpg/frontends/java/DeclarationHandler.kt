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

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.ReferenceType
import com.github.javaparser.resolution.UnsolvedSymbolException
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HandlerInterface
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.Constructor
import de.fraunhofer.aisec.cpg.graph.declarations.EnumConstant
import de.fraunhofer.aisec.cpg.graph.declarations.Enumeration
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewArray
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.matchesSignature
import java.util.function.Supplier

open class DeclarationHandler(lang: JavaLanguageFrontend) :
    Handler<Declaration, Node, JavaLanguageFrontend>(Supplier { ProblemDeclaration() }, lang) {
    fun handleConstructor(constructorDeclaration: ConstructorDeclaration): Constructor {
        val resolvedConstructor = constructorDeclaration.resolve()
        val currentRecordDecl = frontend.scopeManager.currentRecord
        val declaration =
            this.newConstructor(
                resolvedConstructor.name,
                currentRecordDecl,
                rawNode = constructorDeclaration,
            )
        frontend.scopeManager.enterScope(declaration)
        createMethodReceiver(currentRecordDecl, declaration)
        declaration.addThrowTypes(
            constructorDeclaration.thrownExceptions.map { type: ReferenceType ->
                frontend.typeOf(type)
            }
        )
        for (parameter in constructorDeclaration.parameters) {
            val param =
                this.newParameter(
                    parameter.nameAsString,
                    frontend.getTypeAsGoodAsPossible(parameter, parameter.resolve()),
                    parameter.isVarArgs,
                    rawNode = parameter,
                )
            frontend.scopeManager.addDeclaration(param)
            declaration.parameters += param
        }

        val record =
            frontend.scopeManager.firstScopeOrNull { it is RecordScope }?.astNode as? Record
        if (record != null) {
            val type = record.toType()
            declaration.type = type
        }

        // check, if constructor has body (i.e. it's not abstract or something)
        val body = constructorDeclaration.body
        addImplicitReturn(body)
        declaration.body = frontend.statementHandler.handle(body)
        frontend.processAnnotations(declaration, constructorDeclaration)
        frontend.scopeManager.leaveScope(declaration)
        return declaration
    }

    fun handleMethod(methodDecl: MethodDeclaration): Method {
        val resolvedMethod = methodDecl.resolve()
        val currentRecordDecl = frontend.scopeManager.currentRecord

        val functionDeclaration =
            this.newMethod(
                resolvedMethod.name,
                methodDecl.isStatic,
                currentRecordDecl,
                rawNode = methodDecl,
            )
        functionDeclaration.modifiers =
            methodDecl.modifiers.map { modifier -> modifier.keyword.asString() }.toSet()

        frontend.scopeManager.enterScope(functionDeclaration)
        createMethodReceiver(currentRecordDecl, functionDeclaration)
        functionDeclaration.addThrowTypes(
            methodDecl.thrownExceptions.map { type: ReferenceType -> frontend.typeOf(type) }
        )
        for (parameter in methodDecl.parameters) {
            var resolvedType: Type? =
                frontend.typeManager.getTypeParameter(
                    functionDeclaration.recordDeclaration,
                    parameter.type.toString(),
                )
            if (resolvedType == null) {
                resolvedType = frontend.getTypeAsGoodAsPossible(parameter, parameter.resolve())
            }
            val param =
                this.newParameter(
                    parameter.nameAsString,
                    resolvedType,
                    parameter.isVarArgs,
                    rawNode = parameter,
                )
            frontend.processAnnotations(param, parameter)
            frontend.scopeManager.addDeclaration(param)
            functionDeclaration.parameters += param
        }
        val returnTypes = listOf(frontend.getReturnTypeAsGoodAsPossible(methodDecl, resolvedMethod))
        functionDeclaration.returnTypes = returnTypes
        val type = computeType(functionDeclaration)
        functionDeclaration.type = type

        // check, if method has body (i.e., it's not abstract or something)
        val o = methodDecl.body
        if (o.isEmpty) {
            frontend.scopeManager.leaveScope(functionDeclaration)
            return functionDeclaration
        }
        val body = o.get()
        addImplicitReturn(body)
        functionDeclaration.body = frontend.statementHandler.handle(body)
        frontend.processAnnotations(functionDeclaration, methodDecl)
        frontend.scopeManager.leaveScope(functionDeclaration)
        return functionDeclaration
    }

    private fun createMethodReceiver(recordDeclaration: Record?, functionDeclaration: Method) {
        // create the receiver
        val receiver =
            newVariable("this", recordDeclaration?.toType() ?: unknownType(), false)
                .implicit("this")
        frontend.scopeManager.addDeclaration(receiver)
        functionDeclaration.receiver = receiver
    }

    open fun handleClassOrInterfaceDeclaration(
        classInterDecl: ClassOrInterfaceDeclaration
    ): Record {
        // TODO: support other kinds, such as interfaces
        val fqn = classInterDecl.fullyQualifiedName.orElse(classInterDecl.nameAsString)

        // Todo adapt name using a new type of scope "Namespace/Package scope"

        // add a type declaration
        val recordDeclaration = this.newRecord(fqn, "class", rawNode = classInterDecl)
        recordDeclaration.superClasses =
            classInterDecl.extendedTypes
                .map { type -> frontend.getTypeAsGoodAsPossible(type) }
                .toMutableList()
        recordDeclaration.implementedInterfaces =
            classInterDecl.implementedTypes
                .map { type -> frontend.getTypeAsGoodAsPossible(type) }
                .toMutableList()
        recordDeclaration.modifiers =
            classInterDecl.modifiers.map { modifier -> modifier.keyword.asString() }.toSet()

        frontend.typeManager.addTypeParameter(
            recordDeclaration,
            classInterDecl.typeParameters.map { ParameterizedType(it.nameAsString, language) },
        )

        processImportDeclarations(recordDeclaration)

        frontend.scopeManager.enterScope(recordDeclaration)
        processRecordMembers(classInterDecl, recordDeclaration)
        frontend.scopeManager.leaveScope(recordDeclaration)

        if (frontend.scopeManager.currentScope is RecordScope) {
            // We need special handling if this is a so called "inner class". In this case we need
            // to store
            // a "this" reference to the outer class, so methods can use a "qualified this"
            // (OuterClass.this.someFunction()). This is the same as the java compiler does. The
            // reference is stored as an implicit field.
            processInnerRecord(recordDeclaration)
        }
        return recordDeclaration
    }

    private fun processInnerRecord(recordDeclaration: Record) {
        // Get all the information of the outer class (its name and the respective type). We
        // need this to generate the field.
        val scope = frontend.scopeManager.currentScope as RecordScope?
        if (scope?.name != null) {
            val fieldType = scope.name.let { this.objectType(it) }

            // Enter the scope of the inner class because the new field belongs there.
            frontend.scopeManager.enterScope(recordDeclaration)
            val field =
                this.newField("this$" + scope.name.localName, fieldType, setOf())
                    .implicit("this$" + scope.name.localName)
            frontend.scopeManager.addDeclaration(field)
            recordDeclaration.fields += field

            frontend.scopeManager.leaveScope(recordDeclaration)
        }
    }

    fun handleField(fieldDecl: FieldDeclaration): DeclarationSequence {
        val declarationSequence = DeclarationSequence()

        for (variable in fieldDecl.variables) {
            val initializer =
                variable.initializer
                    .map { ctx: Expression -> frontend.expressionHandler.handle(ctx) }
                    .orElse(null)
                    as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
            var type: Type
            try {
                // Resolve type first with ParameterizedType
                type =
                    frontend.typeManager.getTypeParameter(
                        frontend.scopeManager.currentRecord,
                        variable.resolve().type.describe(),
                    ) ?: frontend.typeOf(variable.resolve().type)
            } catch (e: UnsolvedSymbolException) {
                val t = frontend.recoverTypeFromUnsolvedException(e)
                if (t == null) {
                    log.warn("Could not resolve type for {}", variable)
                    type = frontend.typeOf(variable.type)
                } else {
                    type = this.objectType(t)
                    type.typeOrigin = Type.Origin.GUESSED
                }
            } catch (e: UnsupportedOperationException) {
                val t = frontend.recoverTypeFromUnsolvedException(e)
                if (t == null) {
                    log.warn("Could not resolve type for {}", variable)
                    type = frontend.typeOf(variable.type)
                } else {
                    type = this.objectType(t)
                    type.typeOrigin = Type.Origin.GUESSED
                }
            } catch (e: IllegalArgumentException) {
                val t = frontend.recoverTypeFromUnsolvedException(e)
                if (t == null) {
                    log.warn("Could not resolve type for {}", variable)
                    type = frontend.typeOf(variable.type)
                } else {
                    type = this.objectType(t)
                    type.typeOrigin = Type.Origin.GUESSED
                }
            }
            val fieldDeclaration =
                this.newField(
                    variable.name.asString(),
                    type,
                    fieldDecl.modifiers.map { modifier -> modifier.keyword.asString() }.toSet(),
                    initializer,
                    rawNode = fieldDecl,
                )
            frontend.processAnnotations(fieldDeclaration, fieldDecl)
            declarationSequence.addDeclaration(fieldDeclaration)
        }
        return declarationSequence
    }

    fun handleEnumeration(enumDecl: EnumDeclaration): Enumeration {
        val name = enumDecl.nameAsString
        val enumDeclaration = this.newEnumeration(name, rawNode = enumDecl)

        val superTypes = enumDecl.implementedTypes.map { frontend.getTypeAsGoodAsPossible(it) }
        enumDeclaration.superClasses.addAll(superTypes)

        processImportDeclarations(enumDeclaration)

        frontend.scopeManager.enterScope(enumDeclaration)

        processRecordMembers(enumDecl, enumDeclaration)

        val entries = enumDecl.entries.mapNotNull { handle(it) as EnumConstant? }
        entries.forEach { it.type = this.objectType(enumDeclaration.name) }
        enumDeclaration.entries = entries.toMutableList()

        frontend.scopeManager.leaveScope(enumDeclaration)

        if (frontend.scopeManager.currentScope is RecordScope) {
            // We need special handling if this is a so called "inner class". In this case we need
            // to store
            // a "this" reference to the outer class, so methods can use a "qualified this"
            // (OuterClass.this.someFunction()). This is the same as the java compiler does. The
            // reference is stored as an implicit field.
            processInnerRecord(enumDeclaration)
        }
        return enumDeclaration
    }

    private fun <T : TypeDeclaration<T>> processRecordMembers(
        typeDecl: T,
        recordDeclaration: Record,
    ) {
        for (decl in typeDecl.members) {
            when (decl) {
                is MethodDeclaration -> {
                    val md = handle(decl) as Method
                    frontend.scopeManager.addDeclaration(md)
                    recordDeclaration.methods += md
                }
                is FieldDeclaration -> {
                    val seq = handle(decl) as DeclarationSequence
                    seq.declarations.filterIsInstance<Field>().forEach {
                        frontend.scopeManager.addDeclaration(it)
                        recordDeclaration.fields += it
                    }
                }
                is ConstructorDeclaration -> {
                    val c = handle(decl) as Constructor
                    frontend.scopeManager.addDeclaration(c)
                    recordDeclaration.constructors += c
                }
                is ClassOrInterfaceDeclaration -> {
                    val cls = handle(decl) as Record
                    frontend.scopeManager.addDeclaration(cls)
                    recordDeclaration.records += cls
                }
                is EnumDeclaration -> {
                    val cls = handle(decl) as Record
                    frontend.scopeManager.addDeclaration(cls)
                    recordDeclaration.records += cls
                }
                is InitializerDeclaration -> {
                    val initializerBlock = frontend.statementHandler.handleBlockStatement(decl.body)
                    initializerBlock.isStaticBlock = decl.isStatic
                    recordDeclaration.statements += initializerBlock
                }
                else -> {
                    log.debug(
                        "Member {} of type {} is something that we do not parse yet: {}",
                        decl,
                        recordDeclaration.name,
                        decl.javaClass.simpleName,
                    )
                }
            }
        }
        if (recordDeclaration.constructors.isEmpty()) {
            val constructorDeclaration =
                this.newConstructor(recordDeclaration.name.localName, recordDeclaration)
                    .implicit(recordDeclaration.name.localName)
            frontend.scopeManager.addDeclaration(constructorDeclaration)
            recordDeclaration.constructors += constructorDeclaration
        }
        frontend.processAnnotations(recordDeclaration, typeDecl)
    }

    private fun processImportDeclarations(recordDeclaration: Record) {
        val allImports =
            frontend.context
                ?.imports
                ?.map {
                    var iName: String = it.nameAsString
                    // we need to ensure that x.* imports really preserve the asterisk!
                    if (it.isAsterisk && !iName.endsWith(".*")) {
                        iName += ".*"
                    }
                    Pair(it, iName)
                }
                ?.groupBy({ it.first.isStatic }, { it.second })

        recordDeclaration.staticImportStatements = allImports?.get(true) ?: listOf()
        recordDeclaration.importStatements = allImports?.get(false) ?: listOf()
    }

    /* Not so sure about the place of Annotations in the CPG currently */
    fun handleEnumConstant(enumConstDecl: EnumConstantDeclaration): EnumConstant {
        val currentEnum = frontend.scopeManager.currentRecord
        val result = this.newEnumConstant(enumConstDecl.nameAsString, rawNode = enumConstDecl)
        if (enumConstDecl.arguments.isNotEmpty()) {
            val arguments =
                enumConstDecl.arguments.mapNotNull {
                    frontend.expressionHandler.handle(it)
                        as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
                }
            // TODO: This call resolution in the frontend might fail, in particular if we haven't
            // processed the constructor yet. Should be cleaned up in the future but requires
            // changes to the starting points of call/symbol resolution.
            val matchingConstructor =
                currentEnum?.constructors?.singleOrNull {
                    it.matchesSignature(arguments.map { it.type }).isDirectMatch
                }

            val constructExpr = newConstruct(matchingConstructor?.name ?: currentEnum?.name)
            arguments.forEach { constructExpr.addArgument(it) }
            matchingConstructor?.let { constructExpr.constructor = matchingConstructor }
            result.initializer = constructExpr
        }
        return result
    }

    fun /* TODO refine return type*/ handleAnnotationDeclaration(
        annotationConstDecl: AnnotationDeclaration?
    ): Declaration {
        return ProblemDeclaration(
            "AnnotationDeclaration not supported yet",
            ProblemNode.ProblemType.TRANSLATION,
        )
    }

    fun /* TODO refine return type*/ handleAnnotationMemberDeclaration(
        annotationMemberDecl: AnnotationMemberDeclaration?
    ): Declaration {
        return ProblemDeclaration(
            "AnnotationMemberDeclaration not supported yet",
            ProblemNode.ProblemType.TRANSLATION,
        )
    }

    fun handleVariableDeclarator(variable: VariableDeclarator): Variable {
        val resolved = variable.resolve()
        val declarationType = frontend.getTypeAsGoodAsPossible(variable, resolved)
        val declaration = newVariable(resolved.name, declarationType, false, rawNode = variable)
        if (declarationType is PointerType && declarationType.isArray) {
            declaration.isArray = true
        }
        val oInitializer = variable.initializer
        if (oInitializer.isPresent) {
            val initializer =
                frontend.expressionHandler.handle(oInitializer.get())
                    as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression?
            if (initializer is NewArray) {
                declaration.isArray = true
            }
            declaration.initializer = initializer
        }

        return declaration
    }

    companion object {
        private fun addImplicitReturn(body: BlockStmt) {
            val statements = body.statements

            // get the last statement
            var lastStatement: Statement? = null
            if (statements.isNotEmpty()) {
                lastStatement = statements[statements.size - 1]
            }
            // make sure, method contains a return statement
            if (lastStatement == null || !lastStatement.isReturnStmt) {
                body.addStatement(ReturnStmt())
            }
        }
    }

    init {
        map[MethodDeclaration::class.java] = HandlerInterface { decl ->
            handleMethod(decl as MethodDeclaration)
        }
        map[ConstructorDeclaration::class.java] = HandlerInterface { decl ->
            handleConstructor(decl as ConstructorDeclaration)
        }
        map[ClassOrInterfaceDeclaration::class.java] = HandlerInterface { decl ->
            handleClassOrInterfaceDeclaration(decl as ClassOrInterfaceDeclaration)
        }
        map[FieldDeclaration::class.java] = HandlerInterface { decl ->
            handleField(decl as FieldDeclaration)
        }
        map[EnumDeclaration::class.java] = HandlerInterface { decl ->
            handleEnumeration(decl as EnumDeclaration)
        }
        map[EnumConstantDeclaration::class.java] = HandlerInterface { decl ->
            handleEnumConstant(decl as EnumConstantDeclaration)
        }
    }
}
