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

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.ReferenceType
import com.github.javaparser.ast.type.TypeParameter
import com.github.javaparser.resolution.UnsolvedSymbolException
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HandlerInterface
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.EnumConstantDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.EnumDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import java.util.function.Supplier
import java.util.stream.Collectors
import kotlin.collections.set

open class DeclarationHandler(lang: JavaLanguageFrontend) :
    Handler<Declaration, BodyDeclaration<*>, JavaLanguageFrontend>(
        Supplier { ProblemDeclaration() },
        lang
    ) {
    fun handleConstructorDeclaration(
        constructorDecl: ConstructorDeclaration
    ): de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration {
        val resolvedConstructor = constructorDecl.resolve()
        val currentRecordDecl = frontend.scopeManager.currentRecord
        val declaration =
            this.newConstructorDeclaration(
                resolvedConstructor.name,
                constructorDecl.toString(),
                currentRecordDecl
            )
        frontend.scopeManager.addDeclaration(declaration)
        frontend.scopeManager.enterScope(declaration)
        createMethodReceiver(currentRecordDecl, declaration)
        declaration.addThrowTypes(
            constructorDecl.thrownExceptions
                .stream()
                .map { type: ReferenceType -> this.parseType(type.asString()) }
                .collect(Collectors.toList())
        )
        for (parameter in constructorDecl.parameters) {
            val param =
                this.newParamVariableDeclaration(
                    parameter.nameAsString,
                    frontend.getTypeAsGoodAsPossible(parameter, parameter.resolve()),
                    parameter.isVarArgs
                )
            declaration.addParameter(param)
            frontend.setCodeAndLocation(param, parameter)
            frontend.scopeManager.addDeclaration(param)
        }

        val name =
            frontend.scopeManager
                .firstScopeOrNull { RecordScope::class.java.isInstance(it) }
                ?.astNode
                ?.name
        if (name != null) {
            val type = this.parseType(name)
            declaration.type = type
        }

        // check, if constructor has body (i.e. it's not abstract or something)
        val body = constructorDecl.body
        addImplicitReturn(body)
        declaration.body = frontend.statementHandler.handle(body)
        frontend.processAnnotations(declaration, constructorDecl)
        frontend.scopeManager.leaveScope(declaration)
        return declaration
    }

    fun handleMethodDeclaration(
        methodDecl: MethodDeclaration
    ): de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration {
        val resolvedMethod = methodDecl.resolve()
        val currentRecordDecl = frontend.scopeManager.currentRecord
        val functionDeclaration =
            this.newMethodDeclaration(
                resolvedMethod.name,
                methodDecl.toString(),
                methodDecl.isStatic,
                currentRecordDecl
            )
        frontend.scopeManager.enterScope(functionDeclaration)
        createMethodReceiver(currentRecordDecl, functionDeclaration)
        functionDeclaration.addThrowTypes(
            methodDecl.thrownExceptions
                .stream()
                .map { type: ReferenceType -> this.parseType(type.asString()) }
                .collect(Collectors.toList())
        )
        for (parameter in methodDecl.parameters) {
            var resolvedType: Type? =
                TypeManager.getInstance()
                    .getTypeParameter(
                        functionDeclaration.recordDeclaration,
                        parameter.type.toString()
                    )
            if (resolvedType == null) {
                resolvedType = frontend.getTypeAsGoodAsPossible(parameter, parameter.resolve())
            }
            val param =
                this.newParamVariableDeclaration(
                    parameter.nameAsString,
                    resolvedType,
                    parameter.isVarArgs
                )
            functionDeclaration.addParameter(param)
            frontend.setCodeAndLocation(param, parameter)
            frontend.processAnnotations(param, parameter)
            frontend.scopeManager.addDeclaration(param)
        }
        val returnTypes =
            java.util.List.of(frontend.getReturnTypeAsGoodAsPossible(methodDecl, resolvedMethod))
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

    private fun createMethodReceiver(
        recordDecl: RecordDeclaration?,
        functionDeclaration: de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
    ) {
        // create the receiver
        val receiver =
            this.newVariableDeclaration(
                "this",
                if (recordDecl != null) this.parseType(recordDecl.name)
                else UnknownType.getUnknownType(language),
                "this",
                false
            )
        functionDeclaration.receiver = receiver
        frontend.scopeManager.addDeclaration(receiver)
    }

    open fun handleClassOrInterfaceDeclaration(
        classInterDecl: ClassOrInterfaceDeclaration
    ): RecordDeclaration {
        // TODO: support other kinds, such as interfaces
        val fqn = classInterDecl.nameAsString

        // Todo adapt name using a new type of scope "Namespace/Package scope"

        // add a type declaration
        val recordDeclaration = this.newRecordDeclaration(fqn, "class", null, classInterDecl)
        recordDeclaration.superClasses =
            classInterDecl.extendedTypes
                .stream()
                .map { type: ClassOrInterfaceType? -> frontend.getTypeAsGoodAsPossible(type!!) }
                .collect(Collectors.toList())
        recordDeclaration.implementedInterfaces =
            classInterDecl.implementedTypes
                .stream()
                .map { type: ClassOrInterfaceType? -> frontend.getTypeAsGoodAsPossible(type!!) }
                .collect(Collectors.toList())
        TypeManager.getInstance()
            .addTypeParameter(
                recordDeclaration,
                classInterDecl.typeParameters
                    .stream()
                    .map { t: TypeParameter -> ParameterizedType(t.nameAsString, language) }
                    .collect(Collectors.toList())
            )

        // TODO: I cannot replicate the old partionedBy logic
        val staticImports =
            frontend.context
                ?.imports
                ?.filter { it.isStatic }
                ?.map {
                    var iName: String = it.nameAsString
                    // we need to ensure that x.* imports really preserve the asterisk!
                    if (it.isAsterisk && !iName.endsWith(".*")) {
                        iName += ".*"
                    }
                    iName
                }
        val imports =
            frontend.context
                ?.imports
                ?.filter { !it.isStatic }
                ?.map {
                    var iName: String = it.nameAsString
                    // we need to ensure that x.* imports really preserve the asterisk!
                    if (it.isAsterisk && !iName.endsWith(".*")) {
                        iName += ".*"
                    }
                    iName
                }

        recordDeclaration.staticImportStatements = staticImports ?: listOf()
        recordDeclaration.importStatements = imports ?: listOf()
        frontend.scopeManager.enterScope(recordDeclaration)

        // TODO: 'this' identifier for multiple instances?
        for (decl in classInterDecl.members) {
            (decl as? com.github.javaparser.ast.body.FieldDeclaration)?.let {
                handle(it) // will be added via the scopemanager
            }
                ?: if (decl is MethodDeclaration) {
                    val md =
                        handle(decl)
                            as de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration?
                    frontend.scopeManager.addDeclaration(md)
                } else if (decl is ConstructorDeclaration) {
                    val c =
                        handle(decl)
                            as de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration?
                    frontend.scopeManager.addDeclaration(c)
                } else if (decl is ClassOrInterfaceDeclaration) {
                    frontend.scopeManager.addDeclaration(handle(decl))
                } else if (decl is InitializerDeclaration) {
                    val id = decl
                    val initializerBlock = frontend.statementHandler.handleBlockStatement(id.body)
                    initializerBlock.isStaticBlock = id.isStatic
                    recordDeclaration.addStatement(initializerBlock)
                } else {
                    log.debug(
                        "Member {} of type {} is something that we do not parse yet: {}",
                        decl,
                        recordDeclaration.name,
                        decl.javaClass.simpleName
                    )
                }
        }
        if (recordDeclaration.constructors.isEmpty()) {
            val constructorDeclaration =
                this.newConstructorDeclaration(
                    recordDeclaration.name.localName,
                    recordDeclaration.name.localName,
                    recordDeclaration
                )
            recordDeclaration.addConstructor(constructorDeclaration)
            frontend.scopeManager.addDeclaration(constructorDeclaration)
        }
        frontend.processAnnotations(recordDeclaration, classInterDecl)
        frontend.scopeManager.leaveScope(recordDeclaration)

        // We need special handling if this is a so called "inner class". In this case we need to
        // store
        // a "this" reference to the outer class, so methods can use a "qualified this"
        // (OuterClass.this.someFunction()). This is the same as the java compiler does. The
        // reference
        // is stored as an implicit field.
        if (frontend.scopeManager.currentScope is RecordScope) {
            // Get all the information of the outer class (its name and the respective type). We
            // need this
            // to generate the field.
            val scope = frontend.scopeManager.currentScope as RecordScope?
            if (scope!!.name != null) {
                val fieldType = this.parseType(scope.name!!)

                // Enter the scope of the inner class because the new field belongs there.
                frontend.scopeManager.enterScope(recordDeclaration)
                val field =
                    this.newFieldDeclaration(
                        "this$" + scope.name!!.localName,
                        fieldType,
                        listOf<String>(),
                        null,
                        null,
                        null
                    )
                field.isImplicit = true
                frontend.scopeManager.addDeclaration(field)
                frontend.scopeManager.leaveScope(recordDeclaration)
            }
        }
        return recordDeclaration
    }

    fun handleFieldDeclaration(
        fieldDecl: com.github.javaparser.ast.body.FieldDeclaration
    ): FieldDeclaration {

        // TODO: can  field have more than one variable?
        val variable = fieldDecl.getVariable(0)
        val modifiers =
            fieldDecl.modifiers
                .stream()
                .map { modifier: Modifier -> modifier.keyword.asString() }
                .collect(Collectors.toList())
        val joinedModifiers = java.lang.String.join(" ", modifiers) + " "
        val location = frontend.getLocationFromRawNode(fieldDecl)
        val initializer =
            variable.initializer
                .map { ctx: Expression -> frontend.expressionHandler.handle(ctx) }
                .orElse(null) as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        var type: Type?
        try {
            // Resolve type first with ParameterizedType
            type =
                TypeManager.getInstance()
                    .getTypeParameter(
                        frontend.scopeManager.currentRecord,
                        variable.resolve().type.describe()
                    )
            if (type == null) {
                type = this.parseType(joinedModifiers + variable.resolve().type.describe())
            }
        } catch (e: UnsolvedSymbolException) {
            val t = frontend.recoverTypeFromUnsolvedException(e)
            if (t == null) {
                log.warn("Could not resolve type for {}", variable)
                type = this.parseType(joinedModifiers + variable.type.asString())
            } else {
                type = this.parseType(joinedModifiers + t)
                type.typeOrigin = Type.Origin.GUESSED
            }
        } catch (e: UnsupportedOperationException) {
            val t = frontend.recoverTypeFromUnsolvedException(e)
            if (t == null) {
                log.warn("Could not resolve type for {}", variable)
                type = this.parseType(joinedModifiers + variable.type.asString())
            } else {
                type = this.parseType(joinedModifiers + t)
                type.typeOrigin = Type.Origin.GUESSED
            }
        }
        val fieldDeclaration =
            this.newFieldDeclaration(
                variable.name.asString(),
                type,
                modifiers,
                variable.toString(),
                location,
                initializer
            )
        frontend.scopeManager.addDeclaration(fieldDeclaration)
        frontend.processAnnotations(fieldDeclaration, fieldDecl)
        return fieldDeclaration
    }

    fun handleEnumDeclaration(
        enumDecl: com.github.javaparser.ast.body.EnumDeclaration
    ): EnumDeclaration {
        val name = enumDecl.nameAsString
        val location = frontend.getLocationFromRawNode(enumDecl)
        val enumDeclaration = this.newEnumDeclaration(name, enumDecl.toString(), location)
        val entries = enumDecl.entries.mapNotNull { handle(it) as EnumConstantDeclaration? }

        entries.forEach { it.type = this.parseType(enumDeclaration.name) }
        enumDeclaration.entries = entries
        val superTypes = enumDecl.implementedTypes.map { frontend.getTypeAsGoodAsPossible(it) }
        enumDeclaration.superTypes = superTypes
        return enumDeclaration
    }

    /* Not so sure about the place of Annotations in the CPG currently */
    fun handleEnumConstantDeclaration(
        enumConstDecl: com.github.javaparser.ast.body.EnumConstantDeclaration
    ): EnumConstantDeclaration {
        return this.newEnumConstantDeclaration(
            enumConstDecl.nameAsString,
            enumConstDecl.toString(),
            frontend.getLocationFromRawNode(enumConstDecl)
        )
    }

    fun /* TODO refine return type*/ handleAnnotationDeclaration(
        annotationConstDecl: AnnotationDeclaration?
    ): Declaration {
        return ProblemDeclaration(
            "AnnotationDeclaration not supported yet",
            ProblemNode.ProblemType.TRANSLATION
        )
    }

    fun /* TODO refine return type*/ handleAnnotationMemberDeclaration(
        annotationMemberDecl: AnnotationMemberDeclaration?
    ): Declaration {
        return ProblemDeclaration(
            "AnnotationMemberDeclaration not supported yet",
            ProblemNode.ProblemType.TRANSLATION
        )
    }

    companion object {
        private fun addImplicitReturn(body: BlockStmt) {
            val statements = body.statements

            // get the last statement
            var lastStatement: Statement? = null
            if (!statements.isEmpty()) {
                lastStatement = statements[statements.size - 1]
            }
            // make sure, method contains a return statement
            if (lastStatement == null || !lastStatement.isReturnStmt) {
                body.addStatement(ReturnStmt())
            }
        }
    }

    init {
        map[MethodDeclaration::class.java] = HandlerInterface { decl: BodyDeclaration<*> ->
            handleMethodDeclaration(decl as MethodDeclaration)
        }
        map[ConstructorDeclaration::class.java] = HandlerInterface { decl: BodyDeclaration<*> ->
            handleConstructorDeclaration(decl as ConstructorDeclaration)
        }
        map[ClassOrInterfaceDeclaration::class.java] =
            HandlerInterface { decl: BodyDeclaration<*> ->
                handleClassOrInterfaceDeclaration(decl as ClassOrInterfaceDeclaration)
            }
        map[com.github.javaparser.ast.body.FieldDeclaration::class.java] =
            HandlerInterface { decl: BodyDeclaration<*> ->
                handleFieldDeclaration(decl as com.github.javaparser.ast.body.FieldDeclaration)
            }
        map[com.github.javaparser.ast.body.EnumDeclaration::class.java] =
            HandlerInterface { decl: BodyDeclaration<*> ->
                handleEnumDeclaration(decl as com.github.javaparser.ast.body.EnumDeclaration)
            }
        map[com.github.javaparser.ast.body.EnumConstantDeclaration::class.java] =
            HandlerInterface { decl: BodyDeclaration<*> ->
                handleEnumConstantDeclaration(
                    decl as com.github.javaparser.ast.body.EnumConstantDeclaration
                )
            }
    }
}
