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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.HasSuperclasses
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.newFieldDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

/**
 * Creates new connections between the place where a variable is declared and where it is used.
 *
 * A field access is modeled with a [MemberExpression]. After AST building, its base and member
 * references are set to [DeclaredReferenceExpression] stubs. This pass resolves those references
 * and makes the member point to the appropriate [FieldDeclaration] and the base to the "this"
 * [FieldDeclaration] of the containing class. It is also capable of resolving references to fields
 * that are inherited from a superclass and thus not declared in the actual base class. When base or
 * member declarations are not found in the graph, a new "inferred" [FieldDeclaration] is being
 * created that is then used to collect all usages to the same unknown declaration.
 * [DeclaredReferenceExpression] stubs are removed from the graph after being resolved.
 *
 * Accessing a local variable is modeled directly with a [DeclaredReferenceExpression]. This step of
 * the pass doesn't remove the [DeclaredReferenceExpression] nodes like in the field usage case but
 * rather makes their "refersTo" point to the appropriate [ValueDeclaration].
 */
@DependsOn(TypeHierarchyResolver::class)
open class VariableUsageResolver : SymbolResolverPass() {

    override fun accept(result: TranslationResult) {
        scopeManager = result.scopeManager
        config = result.config

        walker = ScopedWalker(scopeManager)
        for (tu in result.translationUnits) {
            currentTU = tu
            walker.clearCallbacks()
            walker.registerHandler { _, _, currNode -> walker.collectDeclarations(currNode) }
            walker.registerHandler { node, _ -> findRecords(node) }
            walker.registerHandler { node, _ -> findEnums(node) }
            walker.iterate(currentTU)
        }

        collectSupertypes()

        for (tu in result.translationUnits) {
            walker.clearCallbacks()
            walker.registerHandler { curClass, _, node -> resolveFieldUsages(curClass, node) }
            walker.iterate(tu)
        }
        for (tu in result.translationUnits) {
            walker.clearCallbacks()
            walker.registerHandler(::resolveLocalVarUsage)
            walker.iterate(tu)
        }
    }

    private fun resolveFunctionPtr(
        containingClassArg: Type?,
        reference: DeclaredReferenceExpression
    ): ValueDeclaration? {
        var containingClass = containingClassArg
        // Without FunctionPointerType, we cannot resolve function pointers
        val fptrType = reference.type as? FunctionPointerType ?: return null

        var functionName = reference.name
        val matcher =
            Pattern.compile("(?:(?<class>.*)(?:\\.|::))?(?<function>.*)").matcher(reference.name)
        if (matcher.matches()) {
            val cls = matcher.group("class")
            functionName = matcher.group("function")
            if (cls == null) {
                log.error(
                    "Resolution of pointers to functions inside the current scope should have been done by the ScopeManager"
                )
            } else {
                containingClass = TypeParser.createFrom(cls, true, reference.language)
            }
        }

        return handleUnknownFunction(
            if (containingClass != null) {
                recordMap[containingClass.typeName]
            } else {
                null
            },
            functionName,
            fptrType
        )
    }

    private fun resolveLocalVarUsage(
        currentClass: RecordDeclaration?,
        parent: Node?,
        current: Node
    ) {
        var language = current.language

        if (current !is DeclaredReferenceExpression || current is MemberExpression) return
        if (
            parent is MemberCallExpression &&
                current === parent.member &&
                current.type !is FunctionPointerType
        ) {
            // members of a MemberCallExpression are no variables to be resolved, unless we have
            // a function pointer call
            return
        }

        // For now, we need to ignore reference expressions that are directly embedded into call
        // expressions, because they are the "callee" property. In the future, we will use this
        // property to actually resolve the function call.
        if (parent is CallExpression && parent.callee === current) {
            return
        }

        // only consider resolving, if the language frontend did not specify a resolution
        var refersTo = current.refersTo ?: scopeManager?.resolveReference(current)
        // if (current.refersTo == null) scopeManager?.resolveReference(current)
        // else current.refersTo!!
        var recordDeclType: Type? = null
        if (currentClass != null) {
            recordDeclType = TypeParser.createFrom(currentClass.name, true, currentClass.language)
        }
        if (current.type is FunctionPointerType && refersTo == null) {
            refersTo = resolveFunctionPtr(recordDeclType, current)
        }

        // only add new nodes for non-static unknown
        if (
            refersTo == null &&
                !current.isStaticAccess &&
                recordDeclType != null &&
                recordDeclType.typeName in recordMap
        ) {
            // Maybe we are referring to a field instead of a local var
            if (language != null && language.namespaceDelimiter in current.name) {
                recordDeclType = getEnclosingTypeOf(current)
            }
            val field = resolveMember(recordDeclType, current)
            if (field != null) {
                refersTo = field
            }
        }

        // TODO: we need to do proper scoping (and merge it with the code above), but for now
        // this just enables CXX static fields
        if (refersTo == null && language != null && language.namespaceDelimiter in current.name) {
            recordDeclType = getEnclosingTypeOf(current)
            val field = resolveMember(recordDeclType, current)
            if (field != null) {
                refersTo = field
            }
        }
        if (refersTo != null) {
            current.refersTo = refersTo
        } else {
            Util.warnWithFileLocation(
                current,
                log,
                "Did not find a declaration for ${current.name}"
            )
        }
    }

    /**
     * We get the type of the "scope" this node is in. (e.g. for a field, we drop the field's name
     * and have the class)
     */
    private fun getEnclosingTypeOf(current: Node): Type {
        val language = current.language

        // TODO(oxisto): This should use our new name system instead
        if (language != null && language.namespaceDelimiter.isNotEmpty()) {
            /*val path =
                listOf(
                    *current.name
                        .split(Pattern.quote(language.namespaceDelimiter).toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                )
            return TypeParser.createFrom(
                java.lang.String.join(language.namespaceDelimiter, path.subList(0, path.size - 1)),
                true
            )*/
            val parentName = Util.getParentName(language, current.name)
            return TypeParser.createFrom(parentName, true, language)
        } else {
            return UnknownType.getUnknownType()
        }
    }

    private fun resolveFieldUsages(curClass: RecordDeclaration?, current: Node) {
        if (current !is MemberExpression) return

        var baseTarget: Declaration? = null
        if (current.base is DeclaredReferenceExpression) {
            val base = current.base as DeclaredReferenceExpression
            if (
                current.language is HasSuperclasses &&
                    base.name == (current.language as HasSuperclasses).superclassKeyword
            ) {
                if (curClass != null && curClass.superClasses.isNotEmpty()) {
                    val superType = curClass.superClasses[0]
                    val superRecord = recordMap[superType.typeName]
                    if (superRecord == null) {
                        log.error(
                            "Could not find referring super type ${superType.typeName} for ${curClass.name} in the record map. Will set the super type to java.lang.Object"
                        )
                        // TODO: Should be more generic!
                        base.type =
                            TypeParser.createFrom(Any::class.java.name, true, current.language)
                    } else {
                        // We need to connect this super reference to the receiver of this
                        // method
                        val func = scopeManager?.currentFunction
                        if (func is MethodDeclaration) {
                            baseTarget = func.receiver
                        }
                        if (baseTarget != null) {
                            base.refersTo = baseTarget
                            // Explicitly set the type of the call's base to the super type
                            base.type = superType
                            // And set the possible subtypes, to ensure, that really only our
                            // super type is in there
                            base.updatePossibleSubtypes(listOf(superType))
                        }
                    }
                } else {
                    // no explicit super type -> java.lang.Object
                    // TODO: Should be more generic
                    val objectType =
                        TypeParser.createFrom(Any::class.java.name, true, current.language)
                    base.type = objectType
                }
            } else {
                baseTarget = resolveBase(current.base as DeclaredReferenceExpression)
                base.refersTo = baseTarget
            }
            if (baseTarget is EnumDeclaration) {
                val name = current.name
                val memberTarget = baseTarget.entries.firstOrNull { it.name == name }
                if (memberTarget != null) {
                    current.refersTo = memberTarget
                    return
                }
            } else if (baseTarget is RecordDeclaration) {
                var baseType = TypeParser.createFrom(baseTarget.name, true, baseTarget.language)
                if (baseType.typeName !in recordMap) {
                    val containingT = baseType
                    val fqnResolvedType =
                        recordMap.keys.firstOrNull {
                            it.endsWith("." + containingT.name)
                        } // TODO: Is the "." correct here for all languages?
                    if (fqnResolvedType != null) {
                        baseType = TypeParser.createFrom(fqnResolvedType, true, baseTarget.language)
                    }
                }
                current.refersTo = resolveMember(baseType, current)
                return
            }
        }
        var baseType = current.base.type
        if (baseType.typeName !in recordMap) {
            val fqnResolvedType = recordMap.keys.firstOrNull { it.endsWith("." + baseType.name) }
            if (fqnResolvedType != null) {
                baseType = TypeParser.createFrom(fqnResolvedType, true, baseType.language)
            }
        }
        current.refersTo = resolveMember(baseType, current)
    }

    private fun resolveBase(reference: DeclaredReferenceExpression): Declaration? {
        val declaration = scopeManager?.resolveReference(reference)
        if (declaration != null) {
            return declaration
        }

        // check if this refers to an enum
        return if (reference.type in enumMap) {
            enumMap[reference.type]
        } else if (reference.type.typeName in recordMap) {
            recordMap[reference.type.typeName]
        } else {
            null
        }
    }

    private fun resolveMember(
        containingClass: Type,
        reference: DeclaredReferenceExpression
    ): ValueDeclaration? {
        if (isSuperclassReference(reference)) {
            // if we have a "super" on the member side, this is a member call. We need to resolve
            // this in the call resolver instead
            return null
        }
        val simpleName = Util.getSimpleName(reference.language, reference.name)
        var member: FieldDeclaration? = null
        if (containingClass !is UnknownType && containingClass.typeName in recordMap) {
            member =
                recordMap[containingClass.typeName]!!
                    .fields
                    .filter { it.name == simpleName }
                    .map { it.definition }
                    .firstOrNull()
        }
        if (member == null) {
            member =
                superTypesMap
                    .getOrDefault(containingClass.typeName, listOf())
                    .mapNotNull { recordMap[it.typeName] }
                    .flatMap { it.fields }
                    .filter { it.name == simpleName }
                    .map { it.definition }
                    .firstOrNull()
        }
        // Attention: using orElse instead of orElseGet will always invoke unknown declaration
        // handling!
        return member ?: handleUnknownField(containingClass, reference.name, reference.type)
    }

    // TODO(oxisto): Move to inference class
    private fun handleUnknownField(base: Type, name: String, type: Type): FieldDeclaration? {
        // unwrap a potential pointer-type
        if (base is PointerType) {
            return handleUnknownField(base.elementType, name, type)
        }

        if (base.typeName !in recordMap) {
            // No matching record in the map? If we should infer it, we do so, otherwise we stop.
            if (config?.inferenceConfiguration?.inferRecords != true) return null

            // We access an unknown field of an unknown record. so we need to handle that
            inferRecordDeclaration(base, base.typeName, "struct")
        }

        val recordDeclaration = recordMap[base.typeName]
        if (recordDeclaration == null) {
            log.error(
                "There is no matching record in the record map. Can't identify which field is used."
            )
            return null
        }

        val target = recordDeclaration.fields.firstOrNull { it.name == name }

        return if (target != null) {
            target
        } else {
            val declaration =
                recordDeclaration.newFieldDeclaration(
                    name,
                    type,
                    listOf<String>(),
                    "",
                    null,
                    null,
                    false,
                )
            recordDeclaration.addField(declaration)
            declaration.isInferred = true
            declaration
        }
    }

    /**
     * Generates a [MethodDeclaration] if the [declarationHolder] is a [RecordDeclaration] or a
     * [FunctionDeclaration] if the [declarationHolder] is a [TranslationUnitDeclaration]. The
     * resulting function/method has the signature and return type specified in [fctPtrType] and the
     * specified [name].
     */
    private fun handleUnknownFunction(
        declarationHolder: RecordDeclaration?,
        name: String,
        fctPtrType: FunctionPointerType
    ): FunctionDeclaration {
        // Try to find the function or method in the list of existing functions.
        val target =
            if (declarationHolder != null) {
                declarationHolder.methods.firstOrNull { f ->
                    f.matches(name, fctPtrType.returnType, fctPtrType.parameters)
                }
            } else {
                currentTU.functions.firstOrNull { f ->
                    f.matches(name, fctPtrType.returnType, fctPtrType.parameters)
                }
            }
        // If we didn't find anything, we create a new function or method declaration
        return target
            ?: (declarationHolder ?: currentTU)
                .startInference()
                .createInferredFunctionDeclaration(
                    name,
                    null,
                    false,
                    fctPtrType.parameters,
                    fctPtrType.returnType
                )
    }

    companion object {
        private val log = LoggerFactory.getLogger(VariableUsageResolver::class.java)
    }
}
