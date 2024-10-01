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
import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.HasGlobalVariables
import de.fraunhofer.aisec.cpg.frontends.HasImplicitReceiver
import de.fraunhofer.aisec.cpg.frontends.HasStructs
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.newFieldDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.recordDeclaration
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import de.fraunhofer.aisec.cpg.passes.inference.Inference.TypeInferenceObserver
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import kotlin.collections.forEach

internal fun TranslationContext.tryNamespaceInference(
    name: Name,
    locationHint: Node?
): NamespaceDeclaration? {
    return scopeManager.globalScope
        ?.astNode
        ?.startInference(this)
        ?.inferNamespaceDeclaration(name, null, locationHint)
}

/**
 * Tries to infer a [RecordDeclaration] from an unresolved [Type]. This will return `null`, if
 * inference was not possible, or if it was turned off in the [InferenceConfiguration].
 *
 * If [updateType] is set to true, also the [ObjectType.recordDeclaration] is adjusted. This is only
 * needed if we call this function in the [SymbolResolver] (and not in the [TypeResolver]).
 */
internal fun TranslationContext.tryRecordInference(
    type: Type,
    locationHint: Node? = null,
    updateType: Boolean = false,
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
            ?.startInference(this)
            ?.inferRecordDeclaration(type, kind, locationHint)

    // update the type's record. Because types are only unique per scope, we potentially need to
    // update multiple type nodes, i.e., all type nodes whose FQN match the inferred record
    if (updateType && record != null) {
        typeManager.firstOrderTypesMap[record.name.toString()]?.forEach {
            it.recordDeclaration = record
        }
    }

    return record
}

/**
 * Tries to infer a global variable from an unresolved [Reference]. This will return `null`, if
 * inference was not possible, or if it was turned off in the [InferenceConfiguration].
 *
 * Currently, this can only infer variables in the [GlobalScope], but not in a namespace.
 */
internal fun TranslationContext.tryGlobalVariableInference(ref: Reference): Declaration? {
    // For now, we only infer globals at the top-most global level, i.e., no globals in
    // namespaces
    if (ref.name.isQualified()) {
        val (scope, _) = scopeManager.extractScope(ref, null)
        when (scope) {
            is NameScope -> {
                log.warn(
                    "We should infer a namespace variable ${ref.name} at this point, but this is not yet implemented."
                )
                return null
            }
            else -> {
                log.warn(
                    "We should infer a variable ${ref.name} in ${scope}, but this is not yet implemented."
                )
                return null
            }
        }
    }

    if (ref.language !is HasGlobalVariables) {
        return null
    }

    // Forward this to our inference system. This will also check whether and how inference is
    // configured.
    return scopeManager.globalScope?.astNode?.startInference(this)?.inferVariableDeclaration(ref)
}

/**
 * Tries to infer a [FieldDeclaration] from an unresolved [MemberExpression] or [Reference] (if the
 * language has [HasImplicitReceiver]). This will return `null`, if inference was not possible, or
 * if it was turned off in the [InferenceConfiguration].
 */
internal fun TranslationContext.tryFieldInference(
    ref: Reference,
    containingClass: ObjectType
): ValueDeclaration? {
    // This is a little bit of a workaround, but at least this makes sure we are not inferring a
    // record, where a namespace already exist
    val (scope, _) = scopeManager.extractScope(ref, null)
    val shouldInfer =
        if (scope == null) {
            true
        } else {
            // Workaround needed for Java?
            when (scope) {
                is RecordScope -> true
                else -> false
            }
        }
    if (!shouldInfer) {
        return null
    }

    val name = ref.name

    var record = containingClass.recordDeclaration
    if (record == null) {
        // We access an unknown field of an unknown record. so we need to handle that along the
        // way as well
        record = tryRecordInference(containingClass, locationHint = ref, updateType = true)
    }

    if (record == null) {
        log.error(
            "There is no matching record in the record map. Can't identify which field is used."
        )
        return null
    }

    val target = record.fields.firstOrNull { it.name.lastPartsMatch(name) }

    return if (target != null) {
        target
    } else {
        val declaration =
            ref.newFieldDeclaration(
                name.localName,
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

        declaration
    }
}
