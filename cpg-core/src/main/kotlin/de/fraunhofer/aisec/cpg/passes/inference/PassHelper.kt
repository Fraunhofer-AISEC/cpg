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
import de.fraunhofer.aisec.cpg.ancestors
import de.fraunhofer.aisec.cpg.frontends.HasGlobalFunctions
import de.fraunhofer.aisec.cpg.frontends.HasGlobalVariables
import de.fraunhofer.aisec.cpg.frontends.HasImplicitReceiver
import de.fraunhofer.aisec.cpg.frontends.HasStructs
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.dCalls
import de.fraunhofer.aisec.cpg.graph.dMethods
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.graph.pTranslationUnit
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.recordDeclaration
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import de.fraunhofer.aisec.cpg.passes.getPossibleContainingTypes
import kotlin.collections.forEach

/**
 * Tries to infer a [NamespaceDeclaration] from a [Name]. This will return `null`, if inference was
 * not possible, or if it was turned off in the [InferenceConfiguration].
 */
fun Pass<*>.tryNamespaceInference(name: Name, source: Node): NamespaceDeclaration? {
    // Determine the scope where we want to start our inference
    val extractedScope =
        scopeManager.extractScope(name, language = source.language, location = source.location)
    var scope = extractedScope?.scope

    if (scope !is NameScope) {
        scope = null
    }

    var holder = scope?.astNode

    // If we could not find a scope, but we have an FQN, we can try to infer a namespace (or a
    // parent record)
    var parentName = name.parent
    if (scope == null && parentName != null) {
        holder = tryScopeInference(parentName, source)
    }

    return (holder ?: scopeManager.translationUnitForInference<NamespaceDeclaration>(source))
        ?.startInference(ctx)
        ?.inferNamespaceDeclaration(name, null, source)
}

/**
 * Tries to infer a [RecordDeclaration] from an unresolved [Type]. This will return `null`, if
 * inference was not possible, or if it was turned off in the [InferenceConfiguration].
 */
internal fun Pass<*>.tryRecordInference(type: Type, source: Node): RecordDeclaration? {
    val kind =
        if (type.language is HasStructs) {
            "struct"
        } else {
            "class"
        }
    // Determine the scope where we want to start our inference
    val extractedScope =
        scopeManager.extractScope(
            type.name,
            language = source.language,
            location = source.location,
            scope = type.scope,
        )
    var scope = extractedScope?.scope

    if (scope !is NameScope) {
        scope = null
    } else if (scope is RecordScope) {
        // We are asked to infer a record inside another record. While this is not unusual
        // per-se, it is far more likely that the "correct" way to place our record is in a
        // parent namespace or even the global scope. This is especially true if we did NOT
        // infer the parent record, because in this case we can somewhat assume that the
        // parent's records declaration (e.g. in a C++ header file) is somewhat complete.
        if (scope.astNode?.isInferred == false) {
            // It is therefore a better choice to infer it in the parent namespace instead
            scope = scopeManager.firstScopeOrNull(scope) { it is NameScope && it !is RecordScope }
        }
    }

    var holder = scope?.astNode

    // If we could not find a scope, but we have an FQN, we can try to infer a namespace (or a
    // parent record)
    var parentName = type.name.parent
    if (scope == null && parentName != null) {
        holder = tryScopeInference(parentName, source)
    }

    val record =
        (holder ?: scopeManager.translationUnitForInference<RecordDeclaration>(source))
            .startInference(ctx)
            ?.inferRecordDeclaration(type, kind, source)

    // Update the type's record. Because types are only unique per scope, we potentially need to
    // update multiple type nodes, i.e., all type nodes whose FQN match the inferred record. We only
    // need to do this if we are NOT in the type resolver
    if (this !is TypeResolver && record != null) {
        typeManager.resolvedTypes
            .filter { it.name == record.name }
            .forEach { it.recordDeclaration = record }
    }

    return record
}

/**
 * Tries to infer a [VariableDeclaration] (or [FieldDeclaration]) out of a [Reference]. This will
 * return `null`, if inference was not possible, or if it was turned off in the
 * [InferenceConfiguration].
 *
 * We mainly try to infer global variables and fields here, since these are possibly parts of the
 * code we do not "see". We do not try to infer local variables, because we are under the assumption
 * that even with incomplete code, we at least have the complete current function code. We can
 * therefore differentiate between four scenarios:
 * - Inference of a [FieldDeclaration] if we have a language that allows implicit receivers, are
 *   inside a function and the ref is not qualified. This is then forwarded to [tryFieldInference].
 * - Inference of a top-level [VariableDeclaration] on a namespace level (this is not yet
 *   implemented)
 * - Inference of a global [VariableDeclaration] in the [GlobalScope].
 * - No inference, in any other cases since this would mean that we would infer a local variable.
 *   This is something we do not want to do see (see above).
 */
internal fun Pass<*>.tryVariableInference(ref: Reference): VariableDeclaration? {
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
        val extractedScope = scopeManager.extractScope(ref, ref.language)
        when (val scope = extractedScope?.scope) {
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
        scopeManager
            .translationUnitForInference<VariableDeclaration>(ref)
            ?.startInference(this.ctx)
            ?.inferVariableDeclaration(ref)
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
internal fun Pass<*>.tryFieldInference(
    ref: Reference,
    targetType: ObjectType,
): VariableDeclaration? {
    // We only want to infer fields here, this can either happen if we have a reference with an
    // implicit receiver or if we have a scoped reference and the scope points to a record
    val extractedScope = scopeManager.extractScope(ref)
    val scope = extractedScope?.scope
    if (scope != null && scope !is RecordScope) {
        return null
    }

    var record = targetType.recordDeclaration
    // We access an unknown field of an unknown record. so we need to handle that along the
    // way as well.
    if (record == null) {
        record = tryRecordInference(targetType, source = ref)
    }

    if (record == null) {
        log.error(
            "There is no matching record in the record map. Can't identify which field is used."
        )
        return null
    }

    return record.startInference(ctx)?.inferFieldDeclaration(ref)
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
 * - a [MethodDeclaration] in a record using [tryMethodInference]
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
    val callee = call.callee
    val (suitableBases, bestGuess) =
        if (
            callee is MemberExpression ||
                callee is Reference &&
                    !call.callee.name.isQualified() &&
                    call.language is HasImplicitReceiver
        ) {
            getPossibleContainingTypes(callee)
        } else {
            Pair(setOf(), null)
        }

    return if (suitableBases.isEmpty()) {
        // While this is definitely a function, it could still be a function
        // inside a namespace. We therefore have two possible start points, a namespace
        // declaration or a translation unit. Nothing else is allowed (for now). We can
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

/** This function tries to infer a missing [FunctionDeclaration] from a function pointer usage. */
internal fun Pass<*>.tryFunctionInferenceFromFunctionPointer(
    ref: Reference,
    type: FunctionPointerType,
): ValueDeclaration? {
    // Determine the scope where we want to start our inference
    var extracted = scopeManager.extractScope(ref)
    val scope =
        if (extracted?.scope !is NameScope) {
            null
        } else {
            extracted.scope
        }

    return (scope?.astNode ?: ref.pTranslationUnit)
        ?.startInference(ctx)
        ?.inferFunctionDeclaration(ref.name, null, false, type.parameters, type.returnType)
}

/**
 * Creates an inferred [FunctionDeclaration] for each suitable [Type] (which points to a
 * [RecordDeclaration]).
 *
 * There is a big challenge in this inference: We can not be 100 % sure, whether we really need to
 * infer a [MethodDeclaration] inside the [RecordDeclaration] or if this is a call to a global
 * function (if [call] is a simple [CallExpression] and not a [MemberCallExpression]). The reason
 * behind that is that most languages allow to omit `this` when calling methods in the current
 * class. So a call to `foo()` inside record `Bar` could either be a call to a global function `foo`
 * or a call to `Bar::foo`.
 *
 * We need to decide whether we want to infer a global function or not; the heuristic is based on a
 * multitude of factors such as:
 * - Whether the language even allows for [HasGlobalFunctions].
 * - Whether we have multiple calls to the same function `func()` from multiple locations, everytime
 *   without an explicit receiver.
 */
internal fun Pass<*>.tryMethodInference(
    call: CallExpression,
    possibleContainingTypes: Set<Type>,
    bestGuess: Type?,
): List<FunctionDeclaration> {
    // We need to decide whether we want to infer a global function or not. We do this with a
    // simple heuristic. This will of course not be 100 % error-free, but this is the burden of
    // inference.
    // 1a) If the language does not even support functions at a global level, it's easy
    // 1b) If this is a member call expression, it's also easy
    var inferGlobalFunction =
        if (call.language !is HasGlobalFunctions || call is MemberCallExpression) {
            false
        } else if (bestGuess is ObjectType && methodExists(bestGuess, call.name.localName)) {
            // 2) We do a quick check, whether we would have a method with our name in the "best
            // guess" class. Because if we do, we most likely ended up here because of an
            // argument type mismatch. Once we use the new call resolution also for member
            // calls, we have this information more easily available
            false
        } else {
            // 3) Lastly, if we are still undecided, we do a quick check on the current
            // component,
            // if we have multiple calls to the same function from across different locations.
            // This is a bit more expensive, so we leave this as a last resort.
            // If we encounter "others", there is a high chance this is a global function. Of
            // course, we could run into a scenario where we have multiple calls to `init()` in
            // several classes and in all occasions the `this` was left out; but this seems
            // unlikely
            var others =
                ctx.currentComponent.dCalls {
                    it != call && it.name == call.name && call !is MemberCallExpression
                }
            others.isNotEmpty()
        }

    if (inferGlobalFunction) {
        var currentTU =
            scopeManager.currentScope.globalScope?.astNode as? TranslationUnitDeclaration
        return listOfNotNull(currentTU?.inferFunction(call, ctx = ctx))
    }

    var records =
        possibleContainingTypes.mapNotNull {
            val root = it.root as? ObjectType
            root?.recordDeclaration
        }

    // We access an unknown method of an unknown record. so we need to handle that along the way as
    // well. We prefer the base type. This should only happen on types that are "built-in", as all
    // other type declarations are already inferred by the type resolver at this stage.
    if (records.isEmpty()) {
        records =
            listOfNotNull(tryRecordInference(bestGuess?.root ?: call.unknownType(), source = call))
    }
    records = records.distinct()

    return records.mapNotNull { record -> record.inferMethod(call, ctx = ctx) }
}

/**
 * This functions tries to infer a "scope" that should exist under [scopeName], but does not.
 *
 * A common use-case for this is the creation of nested namespaces, e.g., when inferring classes
 * such as `java.lang.System`. At first, we check whether the scope `java` exists, if not, this
 * function makes sure that a [NamespaceDeclaration] `java` will be created. Afterward, the same
 * check will be repeated for `java.lang`, until we are finally ready to infer the
 * [RecordDeclaration] `java.lang.System`.
 */
internal fun Pass<*>.tryScopeInference(scopeName: Name, source: Node): Declaration? {
    // At this point, we need to check whether we have any type reference to our scope
    // name. If we have (e.g. it is used in a function parameter, variable, etc.), then we
    // have a high chance that this is actually a parent record and not a namespace
    var parentType = typeManager.lookupResolvedType(scopeName)
    return if (parentType != null) {
        tryRecordInference(parentType, source = source)
    } else {
        tryNamespaceInference(scopeName, source = source)
    }
}

/**
 * This function is a necessary evil until we completely switch over member call resolution to the
 * new call resolver. We need a way to find out if a method with a given name (independently of
 * their arguments) exists in [type] or in one of their [Type.superTypes]. Because in the new call
 * resolver we will get a [CallResolutionResult], which contains all candidate and not just the
 * matching ones.
 *
 * This function should solely be used in [tryMethodInference].
 */
private fun methodExists(type: ObjectType, name: String): Boolean {
    var types = type.ancestors.map { it.type }
    var methods = types.map { it.recordDeclaration }.flatMap { it.dMethods }
    return methods.any { it.name.localName == name }
}
