/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes.inference

import de.fraunhofer.aisec.cpg.CallResolutionResult
import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.frontends.HasGlobalVariables
import de.fraunhofer.aisec.cpg.frontends.HasImplicitReceiver
import de.fraunhofer.aisec.cpg.frontends.HasStructs
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.newFieldDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.recordDeclaration
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import de.fraunhofer.aisec.cpg.passes.getPossibleContainingTypes
import de.fraunhofer.aisec.cpg.passes.inference.Inference.TypeInferenceObserver
import kotlin.collections.forEach

/**
 * Tries to infer a [NamespaceDeclaration] from a [Name]. This will return `null`, if inference was
 * not possible, or if it was turned off in the [InferenceConfiguration].
 */
internal fun Pass<*>.tryNamespaceInference(name: Name, locationHint: Node?): NamespaceDeclaration? {
    return scopeManager.globalScope
        ?.astNode
        ?.startInference(this.ctx)
        ?.inferNamespaceDeclaration(name, null, locationHint)
}

/**
 * Tries to infer a [RecordDeclaration] from an unresolved [Type]. This will return `null`, if
 * inference was not possible, or if it was turned off in the [InferenceConfiguration].
 */
internal fun Pass<*>.tryRecordInference(
    type: Type,
    locationHint: Node? = null,
): RecordDeclaration? {
    val kind =
        if (type.language is HasStructs) {
            "struct"
        } else {
            "class"
        }
    // Determine the scope where we want to start our inference
    var (scope, _) = scopeManager.extractScope(type)

    if (scope !is NameScope) {
        scope = null
    }

    var holder = scope?.astNode

    // If we could not find a scope, but we have an FQN, we can try to infer a namespace (or a
    // parent record)
    var parentName = type.name.parent
    if (scope == null && parentName != null) {
        // At this point, we need to check whether we have any type reference to our parent
        // name. If we have (e.g. it is used in a function parameter, variable, etc.), then we
        // have a high chance that this is actually a parent record and not a namespace
        var parentType = typeManager.lookupResolvedType(parentName)
        holder =
            if (parentType != null) {
                tryRecordInference(parentType, locationHint = locationHint)
            } else {
                tryNamespaceInference(parentName, locationHint = locationHint)
            }
    }

    val record =
        (holder ?: this.scopeManager.globalScope?.astNode)
            ?.startInference(this.ctx)
            ?.inferRecordDeclaration(type, kind, locationHint)

    // Update the type's record. Because types are only unique per scope, we potentially need to
    // update multiple type nodes, i.e., all type nodes whose FQN match the inferred record. We only
    // need to do this if we are NOT in the type resolver
    if (this !is TypeResolver && record != null) {
        typeManager.firstOrderTypes
            .filter { it.name == record.name }
            .forEach { it.recordDeclaration = record }
    }

    return record
}

/**
 * Tries to infer a [VariableDeclaration] out of a [Reference]. This will return `null`, if
 * inference was not possible, or if it was turned off in the [InferenceConfiguration].
 *
 * We mainly try to infer global variables and fields here, since these are possibly parts of the
 * code we do not "see". We do not try to infer local variables, because we are under the assumption
 * that even with incomplete code, we at least have the complete current function code.
 */
internal fun Pass<*>.tryVariableInference(
    ref: Reference,
): VariableDeclaration? {
    var currentRecordType = scopeManager.currentRecord?.toType() as? ObjectType
    return if (
        ref.language is HasImplicitReceiver &&
            !ref.name.isQualified() &&
            !ref.isStaticAccess &&
            currentRecordType != null
    ) {
        // This could potentially be a reference to a field with an implicit receiver call
        tryFieldInference(ref, currentRecordType)
    } else if (ref.name.isQualified()) {
        // For now, we only infer globals at the top-most global level, i.e., no globals in
        // namespaces
        val (scope, _) = scopeManager.extractScope(ref, null)
        when (scope) {
            is NameScope -> {
                log.warn(
                    "We should infer a namespace variable ${ref.name} at this point, but this is not yet implemented."
                )
                null
            }
            else -> {
                log.warn(
                    "We should infer a variable ${ref.name} in ${scope}, but this is not yet implemented."
                )
                null
            }
        }
    } else if (ref.language is HasGlobalVariables) {
        // We can try to infer a possible global variable (at top-level), if the language
        // supports this
        scopeManager.globalScope?.astNode?.startInference(this.ctx)?.inferVariableDeclaration(ref)
    } else {
        // Nothing to infer
        null
    }
}

/**
 * Tries to infer a [FieldDeclaration] from an unresolved [MemberExpression] or [Reference] (if the
 * language has [HasImplicitReceiver]). This will return `null`, if inference was not possible, or
 * if it was turned off in the [InferenceConfiguration].
 *
 * It will also try to infer a [RecordDeclaration], if [targetType] does not have a declaration.
 * However, this is a very special corner-case that will most likely not be triggered, since the
 * majority of types will have their declaration inferred in the [TypeResolver] already before we
 * reach this step here. This should actually only happen in one case: If we try to infer a field of
 * a type that is registered in [Language.builtInTypes] (e.g. `std::string` for C++). In this case,
 * the record for this type is NOT inferred in the type resolver, because we intentionally wait
 * until the symbol resolver, in case we really "see" the record, e.g., if we parse the std headers.
 * If we did not "see" its declaration, we can infer it now.
 */
internal fun Pass<*>.tryFieldInference(ref: Reference, targetType: ObjectType): VariableDeclaration? {
    // We only want to infer fields here, this can either happen if we have a reference with an
    // implicit receiver or if we have a scoped reference and the scope points to a record
    val (scope, _) = scopeManager.extractScope(ref)
    if (scope != null && scope !is RecordScope) {
        return null
    }

    var record = targetType.recordDeclaration
    // We access an unknown field of an unknown record. so we need to handle that along the
    // way as well.
    if (record == null) {
        record = tryRecordInference(targetType, locationHint = ref)
    }

    if (record == null) {
        log.error(
            "There is no matching record in the record map. Can't identify which field is used."
        )
        return null
    }

    val declaration =
        ref.newFieldDeclaration(
            ref.name.localName,
            // we will set the type later through the type inference observer
            record.unknownType(),
            listOf(),
            null,
            false,
        )
    record.addField(declaration)
    declaration.language = record.language
    declaration.isInferred = true

    // We might be able to resolve the type later (or better), if a type is
    // assigned to our reference later
    ref.registerTypeObserver(TypeInferenceObserver(declaration))

    return declaration
}

/**
 * Tries to infer a [FunctionDeclaration] or a [MethodDeclaration] from a [CallExpression]. This
 * will return an empty list, if inference was not possible, or if it was turned off in the
 * [InferenceConfiguration].
 *
 * Depending on several factors, e.g., whether the callee has an FQN, was a [MemberExpression] or
 * whether the language supports [HasImplicitReceiver] we either infer
 * - a global [FunctionDeclaration]
 * - a [FunctionDeclaration] in a namespace
 * - a [MethodDeclaration] in a record
 *
 * Since potentially multiple suitable bases exist for the inference of methods (derived by
 * [getPossibleContainingTypes]), we infer a method for all of them and return a list.
 */
internal fun Pass<*>.tryFunctionInference(
    call: CallExpression,
    result: CallResolutionResult,
): List<FunctionDeclaration> {
    // We need to see, whether we have any suitable base (e.g. a class) or not; There are two
    // main cases
    // a) we have a member expression -> easy
    // b) we have a call expression -> not so easy. This could be a member call with an implicit
    //    this (in which case we want to explore the base type). But that is only possible if
    //    the callee is not qualified, because otherwise we are in a static call like
    //    MyClass::doSomething() or in a namespace call (in case we do not want to explore the
    //    base type here yet). This will change in a future PR.
    val (suitableBases, bestGuess) =
        if (
            call.callee is MemberExpression ||
                !call.callee.name.isQualified() && call.language is HasImplicitReceiver
        ) {
            getPossibleContainingTypes(call)
        } else {
            Pair(setOf(), null)
        }

    return if (suitableBases.isEmpty()) {
        // While this is definitely a function, it could still be a function
        // inside a namespace. We therefore have two possible start points, a namespace
        // declaration or a translation unit. Nothing else is allowed (fow now). We can
        // re-use the information in the ResolutionResult, since this already contains the
        // actual start scope (e.g. in case the callee has an FQN).
        var scope = result.actualStartScope
        if (scope !is NameScope) {
            scope = scopeManager.globalScope
        }
        val func =
            when (val start = scope?.astNode) {
                is TranslationUnitDeclaration -> start.inferFunction(call, ctx = this.ctx)
                is NamespaceDeclaration -> start.inferFunction(call, ctx = this.ctx)
                else -> null
            }
        listOfNotNull(func)
    } else {
        tryMethodInference(call, suitableBases, bestGuess)
    }
}

/**
 * Creates an inferred element for each RecordDeclaration
 *
 * @param call
 * @param possibleContainingTypes
 */
internal fun Pass<*>.tryMethodInference(
    call: CallExpression,
    possibleContainingTypes: Set<Type>,
    bestGuess: Type?,
): List<FunctionDeclaration> {
    var records =
        possibleContainingTypes.mapNotNull {
            val root = it.root as? ObjectType
            root?.recordDeclaration
        }

    // We access an unknown method of an unknown record. so we need to handle that
    // along the way as well. We prefer the base type
    if (records.isEmpty()) {
        records =
            listOfNotNull(
                tryRecordInference(bestGuess?.root ?: call.unknownType(), locationHint = call)
            )
    }
    records = records.distinct()

    return records.mapNotNull { record -> record.inferMethod(call, ctx = this.ctx) }
}
