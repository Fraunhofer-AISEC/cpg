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
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newEnumConstantDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newEnumDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newFieldDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMethodDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newMethodParameterIn
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newRecordDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newVariableDeclaration
import de.fraunhofer.aisec.cpg.graph.ProblemNode
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.EnumConstantDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.EnumDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.types.ParameterizedType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.scopes.RecordScope
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.stream.Collectors

open class DeclarationHandler(lang: JavaLanguageFrontend) :
    Handler<Declaration, BodyDeclaration<*>, JavaLanguageFrontend>(
        Supplier { ProblemDeclaration() },
        lang
    ) {

    init {
        map[com.github.javaparser.ast.body.MethodDeclaration::class.java] = HandlerInterface {
            handleMethodDeclaration(it as com.github.javaparser.ast.body.MethodDeclaration)
        }
        map[com.github.javaparser.ast.body.ConstructorDeclaration::class.java] = HandlerInterface {
            handleConstructorDeclaration(
                it as com.github.javaparser.ast.body.ConstructorDeclaration
            )
        }
        map[ClassOrInterfaceDeclaration::class.java] = HandlerInterface {
            handleClassOrInterfaceDeclaration(it as ClassOrInterfaceDeclaration)
        }
        map[com.github.javaparser.ast.body.FieldDeclaration::class.java] = HandlerInterface {
            handleFieldDeclaration(it as com.github.javaparser.ast.body.FieldDeclaration)
        }
        map[com.github.javaparser.ast.body.EnumDeclaration::class.java] = HandlerInterface {
            handleEnumDeclaration(it as com.github.javaparser.ast.body.EnumDeclaration)
        }
        map[com.github.javaparser.ast.body.EnumConstantDeclaration::class.java] = HandlerInterface {
            handleEnumConstantDeclaration(
                it as com.github.javaparser.ast.body.EnumConstantDeclaration
            )
        }
    }

    open fun handleConstructorDeclaration(
        constructorDecl: com.github.javaparser.ast.body.ConstructorDeclaration
    ): ConstructorDeclaration {
        val resolvedConstructor = constructorDecl.resolve()
        val declaration =
            newConstructorDeclaration(
                resolvedConstructor.name,
                constructorDecl.toString(),
                lang.scopeManager.currentRecord
            )
        lang.scopeManager.addDeclaration(declaration)
        lang.scopeManager.enterScope(declaration)
        declaration.addThrowTypes(
            constructorDecl
                .thrownExceptions
                .stream()
                .map { type: ReferenceType -> TypeParser.createFrom(type.asString(), true) }
                .collect(Collectors.toList())
        )
        for (parameter in constructorDecl.parameters) {
            val param =
                newMethodParameterIn(
                    parameter.nameAsString,
                    lang.getTypeAsGoodAsPossible(parameter, parameter.resolve()),
                    parameter.isVarArgs,
                    parameter.toString()
                )
            declaration.addParameter(param)
            lang.setCodeAndRegion(param, parameter)
            lang.scopeManager.addDeclaration(param)
        }
        val type =
            lang.scopeManager
                .firstScopeOrNull { obj -> RecordScope::class.java.isInstance(obj) }
                ?.astNode
                ?.name
                ?.let { TypeParser.createFrom(it, true) }
                ?: UnknownType.getUnknownType()
        declaration.type = type

        // check, if constructor has body (i.e. its not abstract or something)
        val body = constructorDecl.body
        addImplicitReturn(body)
        declaration.body = lang.statementHandler.handle(body)
        lang.processAnnotations(declaration, constructorDecl)
        lang.scopeManager.leaveScope(declaration)
        return declaration
    }

    open fun handleMethodDeclaration(
        methodDecl: com.github.javaparser.ast.body.MethodDeclaration
    ): MethodDeclaration {
        val resolvedMethod = methodDecl.resolve()
        val record = lang.scopeManager.currentRecord
        val functionDeclaration =
            newMethodDeclaration(
                resolvedMethod.name,
                methodDecl.toString(),
                methodDecl.isStatic,
                record
            )

        // create the receiver
        val receiver =
            newVariableDeclaration(
                "this",
                if (record != null) TypeParser.createFrom(record.name, false)
                else UnknownType.getUnknownType(),
                "this",
                false
            )
        functionDeclaration.receiver = receiver
        lang.scopeManager.enterScope(functionDeclaration)
        functionDeclaration.addThrowTypes(
            methodDecl
                .thrownExceptions
                .stream()
                .map { type: ReferenceType -> TypeParser.createFrom(type.asString(), true) }
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
                resolvedType = lang.getTypeAsGoodAsPossible(parameter, parameter.resolve())
            }
            val param =
                newMethodParameterIn(
                    parameter.nameAsString,
                    resolvedType,
                    parameter.isVarArgs,
                    parameter.toString()
                )
            functionDeclaration.addParameter(param)
            lang.setCodeAndRegion(param, parameter)
            lang.processAnnotations(param, parameter)
            lang.scopeManager.addDeclaration(param)
        }
        functionDeclaration.type = lang.getReturnTypeAsGoodAsPossible(methodDecl, resolvedMethod)

        // check, if method has body (i.e. its not abstract or something)
        val o = methodDecl.body
        if (o.isEmpty) {
            lang.scopeManager.leaveScope(functionDeclaration)
            return functionDeclaration
        }
        val body = o.get()
        addImplicitReturn(body)
        functionDeclaration.body = lang.statementHandler.handle(body)
        lang.processAnnotations(functionDeclaration, methodDecl)
        lang.scopeManager.leaveScope(functionDeclaration)
        return functionDeclaration
    }

    open fun handleClassOrInterfaceDeclaration(
        classInterDecl: ClassOrInterfaceDeclaration
    ): RecordDeclaration {
        // TODO: support other kinds, such as interfaces
        var fqn = classInterDecl.nameAsString

        // Todo adapt name using a new type of scope "Namespace/Package scope"
        // if (packageDeclaration != null) {
        //  name = packageDeclaration.getNameAsString() + "." + name;
        // }
        fqn = getAbsoluteName(fqn)

        // add a type declaration
        val recordDeclaration = newRecordDeclaration(fqn, "class", null, true, lang, classInterDecl)
        recordDeclaration.superClasses =
            classInterDecl
                .extendedTypes
                .stream()
                .map { type: ClassOrInterfaceType? -> lang.getTypeAsGoodAsPossible(type!!) }
                .collect(Collectors.toList())
        recordDeclaration.implementedInterfaces =
            classInterDecl
                .implementedTypes
                .stream()
                .map { type: ClassOrInterfaceType? -> lang.getTypeAsGoodAsPossible(type!!) }
                .collect(Collectors.toList())
        TypeManager.getInstance()
            .addTypeParameter(
                recordDeclaration,
                classInterDecl
                    .typeParameters
                    .stream()
                    .map { t: TypeParameter -> ParameterizedType(t.nameAsString) }
                    .collect(Collectors.toList())
            )
        val partitioned =
            lang.context!!
                .imports
                .stream()
                .collect(
                    Collectors.partitioningBy(
                        { it.isStatic },
                        Collectors.mapping(
                            {
                                var iName = it.nameAsString
                                // we need to ensure that x.* imports really preserve the asterisk!
                                if (it.isAsterisk && !iName.endsWith(".*")) {
                                    iName += ".*"
                                }
                                iName
                            },
                            Collectors.toList()
                        )
                    )
                )
        recordDeclaration.staticImportStatements = partitioned[true]
        recordDeclaration.importStatements = partitioned[false]
        lang.scopeManager.enterScope(recordDeclaration)
        lang.scopeManager.addDeclaration(recordDeclaration.getThis())

        // TODO: 'this' identifier for multiple instances?
        for (decl in classInterDecl.members) {
            (decl as? com.github.javaparser.ast.body.FieldDeclaration)?.let {
                handle(it) // will be added via the scope manager
            }
                ?: when (decl) {
                    is com.github.javaparser.ast.body.MethodDeclaration -> {
                        val md = handle(decl) as MethodDeclaration?
                        recordDeclaration.addMethod(md)
                    }
                    is com.github.javaparser.ast.body.ConstructorDeclaration -> {
                        val c = handle(decl) as ConstructorDeclaration?
                        recordDeclaration.addConstructor(c)
                    }
                    is ClassOrInterfaceDeclaration -> {
                        recordDeclaration.addDeclaration(handle(decl)!!)
                    }
                    is InitializerDeclaration -> {
                        val id = decl
                        val initializerBlock = lang.statementHandler.handleBlockStatement(id.body)
                        initializerBlock.isStaticBlock = id.isStatic
                        recordDeclaration.addStatement(initializerBlock)
                    }
                    else -> {
                        log.debug(
                            "Member {} of type {} is something that we do not parse yet: {}",
                            decl,
                            recordDeclaration.name,
                            decl.javaClass.simpleName
                        )
                    }
                }
        }
        if (recordDeclaration.constructors.isEmpty()) {
            val constructorDeclaration =
                newConstructorDeclaration(
                    recordDeclaration.name,
                    recordDeclaration.name,
                    recordDeclaration
                )
            recordDeclaration.addConstructor(constructorDeclaration)
            lang.scopeManager.addDeclaration(constructorDeclaration)
        }
        lang.processAnnotations(recordDeclaration, classInterDecl)
        lang.scopeManager.leaveScope(recordDeclaration)
        return recordDeclaration
    }

    open fun handleFieldDeclaration(
        fieldDecl: com.github.javaparser.ast.body.FieldDeclaration
    ): FieldDeclaration {

        // TODO: can  field have more than one variable?
        val variable = fieldDecl.getVariable(0)
        val modifiers =
            fieldDecl
                .modifiers
                .stream()
                .map { modifier: Modifier -> modifier.keyword.asString() }
                .collect(Collectors.toList())
        val joinedModifiers = java.lang.String.join(" ", modifiers) + " "
        val location = lang.getLocationFromRawNode(fieldDecl)
        val initializer =
            variable
                .initializer
                .map { ctx: Expression -> lang.expressionHandler.handle(ctx) }
                .orElse(null) as?
                de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        var type: Type?
        try {
            // Resolve type first with ParameterizedType
            type =
                TypeManager.getInstance()
                    .getTypeParameter(
                        lang.scopeManager.currentRecord,
                        variable.resolve().type.describe()
                    )
            if (type == null) {
                type =
                    TypeParser.createFrom(
                        joinedModifiers + variable.resolve().type.describe(),
                        true
                    )
            }
        } catch (e: UnsolvedSymbolException) {
            val t = lang.recoverTypeFromUnsolvedException(e)
            if (t == null) {
                log.warn("Could not resolve type for {}", variable)
                type = TypeParser.createFrom(joinedModifiers + variable.type.asString(), true)
            } else {
                type = TypeParser.createFrom(joinedModifiers + t, true)
                type.typeOrigin = Type.Origin.GUESSED
            }
        } catch (e: UnsupportedOperationException) {
            val t = lang.recoverTypeFromUnsolvedException(e)
            if (t == null) {
                log.warn("Could not resolve type for {}", variable)
                type = TypeParser.createFrom(joinedModifiers + variable.type.asString(), true)
            } else {
                type = TypeParser.createFrom(joinedModifiers + t, true)
                type.typeOrigin = Type.Origin.GUESSED
            }
        }
        val fieldDeclaration =
            newFieldDeclaration(
                variable.name.asString(),
                type,
                modifiers,
                variable.toString(),
                location,
                initializer,
                false
            )
        lang.scopeManager.addDeclaration(fieldDeclaration)
        lang.processAnnotations(fieldDeclaration, fieldDecl)
        return fieldDeclaration
    }

    open fun handleEnumDeclaration(
        enumDecl: com.github.javaparser.ast.body.EnumDeclaration
    ): EnumDeclaration {
        val name = getAbsoluteName(enumDecl.nameAsString)
        val location = lang.getLocationFromRawNode(enumDecl)
        val enumDeclaration = newEnumDeclaration(name, enumDecl.toString(), location)
        val entries =
            enumDecl
                .entries
                .stream()
                .map { e: com.github.javaparser.ast.body.EnumConstantDeclaration? ->
                    handle(e) as EnumConstantDeclaration?
                }
                .collect(Collectors.toList())
        entries.forEach(
            Consumer { e: EnumConstantDeclaration? ->
                e!!.type = TypeParser.createFrom(enumDeclaration.name, true)
            }
        )
        enumDeclaration.entries = entries
        val superTypes =
            enumDecl
                .implementedTypes
                .stream()
                .map { type: ClassOrInterfaceType? -> lang.getTypeAsGoodAsPossible(type!!) }
                .collect(Collectors.toList())
        enumDeclaration.superTypes = superTypes
        return enumDeclaration
    }

    /* Not so sure about the place of Annotations in the CPG currently */
    open fun handleEnumConstantDeclaration(
        enumConstDecl: com.github.javaparser.ast.body.EnumConstantDeclaration
    ): EnumConstantDeclaration {
        return newEnumConstantDeclaration(
            enumConstDecl.nameAsString,
            enumConstDecl.toString(),
            lang.getLocationFromRawNode(enumConstDecl)
        )
    }

    open fun /* TODO refine return type*/ handleAnnotationDeclaration(
        annotationConstDecl: AnnotationDeclaration?
    ): Declaration {
        return ProblemDeclaration(
            "AnnotationDeclaration not supported yet",
            ProblemNode.ProblemType.TRANSLATION
        )
    }

    open fun /* TODO refine return type*/ handleAnnotationMemberDeclaration(
        annotationMemberDecl: AnnotationMemberDeclaration?
    ): Declaration {
        return ProblemDeclaration(
            "AnnotationMemberDeclaration not supported yet",
            ProblemNode.ProblemType.TRANSLATION
        )
    }

    private fun getAbsoluteName(name: String): String {
        val prefix = lang.scopeManager.currentNamePrefix
        return (if (prefix.isNotEmpty()) prefix + lang.namespaceDelimiter else "") + name
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
}
