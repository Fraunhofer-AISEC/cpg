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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.HasAnonymousIdentifier
import de.fraunhofer.aisec.cpg.frontends.HasStructs
import de.fraunhofer.aisec.cpg.frontends.HasSuperClasses
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.inference.Inference.TypeInferenceObserver
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import org.slf4j.LoggerFactory

/**
 * Creates new connections between the place where a variable is declared and where it is used.
 *
 * A field access is modeled with a [MemberExpression]. After AST building, its base and member
 * references are set to [Reference] stubs. This pass resolves those references and makes the member
 * point to the appropriate [FieldDeclaration] and the base to the "this" [FieldDeclaration] of the
 * containing class. It is also capable of resolving references to fields that are inherited from a
 * superclass and thus not declared in the actual base class. When base or member declarations are
 * not found in the graph, a new "inferred" [FieldDeclaration] is being created that is then used to
 * collect all usages to the same unknown declaration. [Reference] stubs are removed from the graph
 * after being resolved.
 *
 * Accessing a local variable is modeled directly with a [Reference]. This step of the pass doesn't
 * remove the [Reference] nodes like in the field usage case but rather makes their "refersTo" point
 * to the appropriate [ValueDeclaration].
 */
@DependsOn(TypeHierarchyResolver::class)
open class VariableUsageResolver(ctx: TranslationContext) : SymbolResolverPass(ctx) {

    override fun accept(component: Component) {
        walker = ScopedWalker(scopeManager)
        for (tu in component.translationUnits) {
            currentTU = tu
            walker.clearCallbacks()
            walker.registerHandler { node, _ -> findRecords(node) }
            walker.registerHandler { node, _ -> findEnums(node) }
            walker.iterate(currentTU)
        }

        collectSupertypes()

        for (tu in component.translationUnits) {
            walker.clearCallbacks()
            walker.registerHandler { curClass, parent, node ->
                resolveFieldUsages(curClass, parent, node)
            }
            walker.iterate(tu)
        }
        for (tu in component.translationUnits) {
            walker.clearCallbacks()
            walker.registerHandler(::resolveLocalVarUsage)
            walker.iterate(tu)
        }
    }

    /** This function seems to resolve function pointers pointing to a [MethodDeclaration]. */
    protected fun resolveMethodFunctionPointer(
        reference: Reference,
        type: FunctionPointerType
    ): ValueDeclaration {
        val parent = reference.name.parent

        return handleUnknownFunction(
            if (parent != null) {
                recordMap[parent]
            } else {
                null
            },
            reference.name,
            type
        )
    }

    protected fun resolveLocalVarUsage(
        currentClass: RecordDeclaration?,
        parent: Node?,
        current: Node?
    ) {
        val language = current?.language

        if (current !is Reference || current is MemberExpression) return

        // We can safely ignore references to the anonymous identifier, e.g. _ in Go.
        if (
            language is HasAnonymousIdentifier &&
                current.name.localName == language.anonymousIdentifier
        ) {
            return
        }

        // For now, we need to ignore reference expressions that are directly embedded into call
        // expressions, because they are the "callee" property. In the future, we will use this
        // property to actually resolve the function call. However, there is a special case that
        // we want to catch already, that is if we are "calling" a reference to a variable. This
        // can be done in several languages, e.g., in C/C++ as function pointers or in Go as
        // function references. In this case, we want to resolve the declared reference expression
        // of this call expression back to its original variable declaration. In the future, we want
        // to extend this particular code to resolve all callee references to their declarations,
        // i.e., their function definitions and get rid of the separate CallResolver.
        var wouldResolveTo: Declaration? = null
        if (parent is CallExpression && parent.callee === current) {
            // Peek into the declaration, and if it is a variable, we can proceed normally, as we
            // are running into the special case explained above. Otherwise, we abort here (for
            // now).
            wouldResolveTo = scopeManager.resolveReference(current, current.scope)
            if (wouldResolveTo !is VariableDeclaration) {
                return
            }
        }

        // Only consider resolving, if the language frontend did not specify a resolution. If we
        // already have populated the wouldResolveTo variable, we can re-use this instead of
        // resolving again
        var refersTo =
            current.refersTo
                ?: wouldResolveTo ?: scopeManager.resolveReference(current, current.scope)

        var recordDeclType: Type? = null
        if (currentClass != null) {
            recordDeclType = currentClass.toType()
        }

        val helperType = current.resolutionHelper?.type
        if (helperType is FunctionPointerType && refersTo == null) {
            refersTo = resolveMethodFunctionPointer(current, helperType)
        }

        // only add new nodes for non-static unknown
        if (
            refersTo == null &&
                !current.isStaticAccess &&
                recordDeclType != null &&
                recordDeclType.name in recordMap
        ) {
            // Maybe we are referring to a field instead of a local var
            if (language != null && language.namespaceDelimiter in current.name.toString()) {
                recordDeclType = getEnclosingTypeOf(current)
            }
            val field = resolveMember(recordDeclType, current)
            if (field != null) {
                refersTo = field
            }
        }

        // TODO: we need to do proper scoping (and merge it with the code above), but for now
        //  this just enables CXX static fields
        if (
            refersTo == null &&
                language != null &&
                language.namespaceDelimiter in current.name.toString()
        ) {
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
    protected fun getEnclosingTypeOf(current: Node): Type {
        val language = current.language

        return if (language != null && language.namespaceDelimiter.isNotEmpty()) {
            val parentName = (current.name.parent ?: current.name).toString()
            current.objectType(parentName)
        } else {
            current.unknownType()
        }
    }

    protected fun resolveFieldUsages(curClass: RecordDeclaration?, parent: Node?, current: Node?) {
        if (current !is MemberExpression) {
            return
        }

        // For legacy reasons, method and field resolving is split between the VariableUsageResolver
        // and the CallResolver. Since we are trying to merge these two, the first step was to have
        // the callee/member field of a MemberCallExpression set to a MemberExpression. This means
        // however, that these will show up in this callback function. To not mess with legacy code
        // (yet), we are ignoring all MemberExpressions whose parents are MemberCallExpressions in
        // this function for now.
        if (parent is MemberCallExpression && parent.callee == current) {
            return
        }

        var baseTarget: Declaration? = null
        if (current.base is Reference) {
            val base = current.base as Reference
            if (
                current.language is HasSuperClasses &&
                    base.name.toString() == (current.language as HasSuperClasses).superClassKeyword
            ) {
                if (curClass != null && curClass.superClasses.isNotEmpty()) {
                    val superType = curClass.superClasses[0]
                    val superRecord = recordMap[superType.name]
                    if (superRecord == null) {
                        log.error(
                            "Could not find referring super type ${superType.typeName} for ${curClass.name} in the record map. Will set the super type to java.lang.Object"
                        )
                        // TODO: Should be more generic!
                        base.type = current.objectType(Any::class.java.name)
                    } else {
                        // We need to connect this super reference to the receiver of this
                        // method
                        val func = scopeManager.currentFunction
                        if (func is MethodDeclaration) {
                            baseTarget = func.receiver
                        }
                        if (baseTarget != null) {
                            base.refersTo = baseTarget
                            // Explicitly set the type of the call's base to the super type
                            base.type = superType
                            (base.refersTo as? HasType)?.type = superType
                            // And set the assigned subtypes, to ensure, that really only our
                            // super type is in there
                            base.assignedTypes = mutableSetOf(superType)
                            (base.refersTo as? ValueDeclaration)?.assignedTypes =
                                mutableSetOf(superType)
                        }
                    }
                } else {
                    // no explicit super type -> java.lang.Object
                    // TODO: Should be more generic
                    val objectType = current.objectType(Any::class.java.name)
                    base.type = objectType
                }
            } else {
                baseTarget = resolveBase(current.base as Reference)
                base.refersTo = baseTarget
            }
            if (baseTarget is EnumDeclaration) {
                val memberTarget =
                    baseTarget.entries.firstOrNull { it.name.lastPartsMatch(current.name) }
                if (memberTarget != null) {
                    current.refersTo = memberTarget
                    return
                }
            } else if (baseTarget is RecordDeclaration) {
                var baseType = baseTarget.toType()
                if (baseType.name !in recordMap) {
                    val containingT = baseType
                    val fqnResolvedType =
                        recordMap.keys.firstOrNull { it.lastPartsMatch(containingT.name) }
                    if (fqnResolvedType != null) {
                        baseType = baseTarget.objectType(fqnResolvedType)
                    }
                }
                current.refersTo = resolveMember(baseType, current)
                return
            }
        }
        var baseType = current.base.type
        if (baseType.name !in recordMap) {
            val fqnResolvedType = recordMap.keys.firstOrNull { it.lastPartsMatch(baseType.name) }
            if (fqnResolvedType != null) {
                baseType = current.base.objectType(fqnResolvedType)
            }
        }
        current.refersTo = resolveMember(baseType, current)
    }

    protected fun resolveBase(reference: Reference): Declaration? {
        val declaration = scopeManager.resolveReference(reference)
        if (declaration != null) {
            return declaration
        }

        // check if this refers to an enum
        return if (reference.type in enumMap) {
            enumMap[reference.type]
        } else if (reference.type.name in recordMap) {
            recordMap[reference.type.name]
        } else {
            null
        }
    }

    protected fun resolveMember(containingClass: Type, reference: Reference): ValueDeclaration? {
        if (isSuperclassReference(reference)) {
            // if we have a "super" on the member side, this is a member call. We need to resolve
            // this in the call resolver instead
            return null
        }
        var member: FieldDeclaration? = null
        if (containingClass !is UnknownType && containingClass.name in recordMap) {
            member =
                recordMap[containingClass.name]
                    .fields
                    .filter { it.name.lastPartsMatch(reference.name) }
                    .map { it.definition }
                    .firstOrNull()
        }
        if (member == null) {
            member =
                superTypesMap
                    .getOrDefault(containingClass.name, listOf())
                    .mapNotNull { recordMap[it.name] }
                    .flatMap { it.fields }
                    .filter { it.name.lastPartsMatch(reference.name) }
                    .map { it.definition }
                    .firstOrNull()
        }
        return member ?: handleUnknownField(containingClass, reference)
    }

    // TODO(oxisto): Move to inference class
    protected fun handleUnknownField(base: Type, ref: Reference): FieldDeclaration? {
        val name = ref.name

        // unwrap a potential pointer-type
        if (base is PointerType) {
            return handleUnknownField(base.elementType, ref)
        }

        if (base.name !in recordMap) {
            // No matching record in the map? If we should infer it, we do so, otherwise we stop.
            if (!config.inferenceConfiguration.inferRecords) return null

            // We access an unknown field of an unknown record. so we need to handle that
            val kind =
                if (base.language is HasStructs) {
                    "struct"
                } else {
                    "class"
                }
            val record = base.startInference(ctx).inferRecordDeclaration(base, currentTU, kind)
            // update the record map
            if (record != null) recordMap[base.name] = record
        }

        val recordDeclaration = recordMap[base.name]
        if (recordDeclaration == null) {
            log.error(
                "There is no matching record in the record map. Can't identify which field is used."
            )
            return null
        }

        val target = recordDeclaration.fields.firstOrNull { it.name.lastPartsMatch(name) }

        return if (target != null) {
            target
        } else {
            val declaration =
                recordDeclaration.newFieldDeclaration(
                    name.localName,
                    // we will set the type later through the type inference observer
                    unknownType(),
                    listOf(),
                    "",
                    null,
                    null,
                    false,
                )
            recordDeclaration.addField(declaration)
            declaration.isInferred = true

            // We might be able to resolve the type later (or better), if a type is
            // assigned to our reference later
            ref.registerTypeObserver(TypeInferenceObserver(declaration))

            declaration
        }
    }

    /**
     * Generates a [MethodDeclaration] if the [declarationHolder] is a [RecordDeclaration] or a
     * [FunctionDeclaration] if the [declarationHolder] is a [TranslationUnitDeclaration]. The
     * resulting function/method has the signature and return type specified in [fctPtrType] and the
     * specified [name].
     */
    protected fun handleUnknownFunction(
        declarationHolder: RecordDeclaration?,
        name: Name,
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
                .startInference(ctx)
                .createInferredFunctionDeclaration(
                    name,
                    null,
                    false,
                    fctPtrType.parameters,
                    fctPtrType.returnType
                )
    }

    companion object {
        protected val log = LoggerFactory.getLogger(VariableUsageResolver::class.java)
    }
}
