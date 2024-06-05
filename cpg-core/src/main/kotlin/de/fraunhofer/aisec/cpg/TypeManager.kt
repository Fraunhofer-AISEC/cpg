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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.CastNotPossible
import de.fraunhofer.aisec.cpg.frontends.CastResult
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.scopes.TemplateScope
import de.fraunhofer.aisec.cpg.graph.types.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TypeManager {
    companion object {
        val log: Logger = LoggerFactory.getLogger(TypeManager::class.java)
    }

    /**
     * Stores the relationship between parameterized RecordDeclarations (e.g. Classes using
     * Generics) to the ParameterizedType to be able to resolve the Type of the fields, since
     * ParameterizedTypes are unique to the RecordDeclaration and are not merged.
     */
    private val recordToTypeParameters: MutableMap<RecordDeclaration, List<ParameterizedType>> =
        ConcurrentHashMap()
    private val templateToTypeParameters:
        MutableMap<TemplateDeclaration, MutableList<ParameterizedType>> =
        ConcurrentHashMap()

    val firstOrderTypes: MutableSet<Type> = ConcurrentHashMap.newKeySet()
    val secondOrderTypes: MutableSet<Type> = ConcurrentHashMap.newKeySet()

    /**
     * @param recordDeclaration that is instantiated by a template containing parameterizedtypes
     * @param name of the ParameterizedType we want to get
     * @return ParameterizedType if there is a parameterized type defined in the recordDeclaration
     *   with matching name, null instead
     */
    fun getTypeParameter(recordDeclaration: RecordDeclaration?, name: String): ParameterizedType? {
        if (recordToTypeParameters.containsKey(recordDeclaration)) {
            for (parameterizedType in recordToTypeParameters[recordDeclaration] ?: listOf()) {
                if (parameterizedType.name.toString() == name) {
                    return parameterizedType
                }
            }
        }
        return null
    }

    /**
     * Adds a List of ParameterizedType to [TypeManager.recordToTypeParameters]
     *
     * @param recordDeclaration will be stored as key for the map
     * @param typeParameters List containing all ParameterizedTypes used by the recordDeclaration
     *   and will be stored as value in the map
     */
    fun addTypeParameter(
        recordDeclaration: RecordDeclaration,
        typeParameters: List<ParameterizedType>
    ) {
        recordToTypeParameters[recordDeclaration] = typeParameters
    }

    /**
     * Searches [TypeManager.templateToTypeParameters] for ParameterizedTypes that were defined in a
     * template matching the provided name
     *
     * @param templateDeclaration that includes the ParameterizedType we are looking for
     * @param name name of the ParameterizedType we are looking for
     * @return
     */
    private fun getTypeParameter(
        templateDeclaration: TemplateDeclaration,
        name: String
    ): ParameterizedType? {
        if (templateToTypeParameters.containsKey(templateDeclaration)) {
            for (parameterizedType in templateToTypeParameters[templateDeclaration] ?: listOf()) {
                if (parameterizedType.name.toString() == name) {
                    return parameterizedType
                }
            }
        }
        return null
    }

    /**
     * @param templateDeclaration
     * @return List containing all ParameterizedTypes the templateDeclaration defines. If the
     *   templateDeclaration is not registered, an empty list is returned.
     */
    fun getAllParameterizedType(templateDeclaration: TemplateDeclaration): List<ParameterizedType> {
        return if (templateToTypeParameters.containsKey(templateDeclaration)) {
            templateToTypeParameters[templateDeclaration] ?: listOf()
        } else ArrayList()
    }

    /**
     * Searches for ParameterizedType if the scope is a TemplateScope. If not we search the parent
     * scope until we reach the top.
     *
     * @param scope in which we are searching for the defined ParameterizedTypes
     * @param name of the ParameterizedType
     * @return ParameterizedType that is found within the scope (or any parent scope) and matches
     *   the provided name. Null if we reach the top of the scope without finding a matching
     *   ParameterizedType
     */
    fun searchTemplateScopeForDefinedParameterizedTypes(
        scope: Scope?,
        name: String
    ): ParameterizedType? {
        if (scope is TemplateScope) {
            val node = scope.astNode

            // We need an additional check here, because of parsing or other errors, the AST node
            // might
            // not necessarily be a template declaration.
            if (node is TemplateDeclaration) {
                val parameterizedType = getTypeParameter(node, name)
                if (parameterizedType != null) {
                    return parameterizedType
                }
            }
        }
        return if (scope!!.parent != null)
            searchTemplateScopeForDefinedParameterizedTypes(scope.parent, name)
        else null
    }

    /**
     * Adds ParameterizedType to the [TypeManager.templateToTypeParameters] to be able to resolve
     * this type when it is used
     *
     * @param templateDeclaration key for [TypeManager.templateToTypeParameters]
     * @param typeParameter ParameterizedType we want to register
     */
    fun addTypeParameter(
        templateDeclaration: TemplateDeclaration,
        typeParameter: ParameterizedType
    ) {
        val parameters =
            templateToTypeParameters.computeIfAbsent(templateDeclaration) { mutableListOf() }

        parameters += typeParameter
    }

    /**
     * Check if a ParameterizedType with name typeName is already registered. If so we return the
     * already created ParameterizedType. If not, we create and return a new ParameterizedType
     *
     * @param templateDeclaration in which the ParameterizedType is defined
     * @param typeName name of the ParameterizedType
     * @return
     */
    fun createOrGetTypeParameter(
        templateDeclaration: TemplateDeclaration,
        typeName: String,
        language: Language<*>?
    ): ParameterizedType {
        var parameterizedType = getTypeParameter(templateDeclaration, typeName)
        if (parameterizedType == null) {
            parameterizedType = ParameterizedType(typeName, language)
            addTypeParameter(templateDeclaration, parameterizedType)
        }
        return parameterizedType
    }

    inline fun <reified T : Type> registerType(t: T): T {
        // Skip as they should be unique to each class and not globally unique
        if (t is ParameterizedType) {
            return t
        }

        if (t.isFirstOrderType) {
            // Make sure we only ever return one unique object per type
            if (!firstOrderTypes.add(t)) {
                return firstOrderTypes.first { it == t && it is T } as T
            } else {
                log.trace(
                    "Registering unique first order type {}{}",
                    t.name,
                    if ((t as? ObjectType)?.generics?.isNotEmpty() == true) {
                        " with generics ${t.generics.joinToString(",", "[", "]") { it.name.toString() }}"
                    } else {
                        ""
                    }
                )
            }
        } else if (t is SecondOrderType) {
            if (!secondOrderTypes.add(t)) {
                return secondOrderTypes.first { it == t && it is T } as T
            } else {
                log.trace("Registering unique second order type {}", t.name)
            }
        }

        return t
    }

    fun typeExists(name: String): Boolean {
        return firstOrderTypes.any { type: Type -> type.root.name.toString() == name }
    }

    fun resolvePossibleTypedef(alias: Type, scopeManager: ScopeManager): Type {
        val finalToCheck = alias.root
        val applicable = scopeManager.typedefFor(finalToCheck)
        return applicable ?: alias
    }
}

val Type.ancestors: Set<Type.Ancestor>
    get() {
        return this.getAncestors(0)
    }

internal fun Type.getAncestors(depth: Int): Set<Type.Ancestor> {
    val types = mutableSetOf<Type.Ancestor>()

    // Recursively call ourselves on our super types. There is a little hack here that we need to do
    // for object types created from RecordDeclaration::toType() because their supertypes might not
    // be set correctly. This would be better, if we change a RecordDeclaration to a
    // ValueDeclaration and set the corresponding object type to its type.
    val superTypes =
        if (this is ObjectType) {
            this.recordDeclaration?.superTypes ?: setOf()
        } else {
            superTypes
        }

    types += superTypes.flatMap { it.getAncestors(depth + 1) }

    // Since the chain starts with our type, we add ourselves to it
    types += Type.Ancestor(this, depth)

    return types
}

/**
 * This function checks, if this [Type] can be cast into [targetType]. Note, this also takes the
 * [WrapState] of the type into account, which means that pointer types of derived types will not
 * match with a non-pointer type of its base type. But, if both are pointer types, they will match.
 *
 * Optionally, the nodes that hold the respective type can be supplied as [hint] and [targetHint].
 */
fun Type.tryCast(targetType: Type, hint: HasType? = null, targetHint: HasType? = null): CastResult {
    return this.language?.tryCast(this, targetType, hint, targetHint) ?: CastNotPossible
}

/**
 * This computed property returns the common type in a [Collection] of [Type] objects. For example,
 * if two types `A` and `B` both derive from the interface `C`` then `C` would be returned.
 *
 * More specifically, the lowest common ancestors (LCA) in a tree containing all ancestors of all
 * types in the set is returned.
 */
val Collection<Type>.commonType: Type?
    get() {
        // If we only have one type, we can just directly return it
        val single = this.singleOrNull()
        if (single != null) {
            return single
        }

        // Make sure, we only compare types of the same "kind" of type (e.g. ObjectType vs.
        // NumericType)
        val sameKind = this.map { it::class.simpleName }.toSet().size == 1
        if (!sameKind) {
            return null
        }

        // We also need to make sure that we compare the same reference depth and wrap state
        // (which contains the pointer origins), because otherwise we need to re-create the
        // equivalent wrap state at the end. Make sure we only have one wrap state before we
        // proceed.
        val wrapStates = this.map { it.wrapState }.toSet()
        val wrapState = wrapStates.singleOrNull() ?: return null

        // Build all ancestors out of the root types. This way we compare the most inner type,
        // regardless of the wrap state.
        val allAncestors = this.map { it.root.ancestors }

        // Find the lowest common ancestor (LCA) by maintaining a list of common ancestors, filling
        // them with the ancestors of the first type and then eliminate the list of common ancestors
        // step-by-step by looping over the ancestor list of all other types.
        var commonAncestors = allAncestors.first().toList()
        for (others in allAncestors.subList(1, allAncestors.size)) {
            // In the remaining loop, we are trying to eliminate potential candidates from the
            // list, or more specifically, we are doing an intersect of both lists. If both have an
            // ancestor in common, but on a different depth, the item which has a higher depth is
            // chosen.
            commonAncestors =
                commonAncestors.mapNotNull { ancestor ->
                    val other =
                        others.find {
                            // The equals/hashcode method of an Ancestor will ignore its depth, but
                            // only look at its type. Therefore, ancestors with the same type but
                            // different depths will match here.
                            it == ancestor
                        } ?: return@mapNotNull null

                    // We then need to select one of both, depending on the depth
                    if (ancestor.depth >= other.depth) {
                        ancestor
                    } else {
                        other
                    }
                }
        }

        // Find the one with the largest depth (which is closest to the original type, since the
        // root node is 0) and re-wrap the final common type back into the original wrap state
        return commonAncestors.minByOrNull(Type.Ancestor::depth)?.type?.wrap(wrapState)
    }

/**
 * Calculates and returns the [WrapState] of the current type. A [WrapState] can be used to compute
 * a "wrapped" type, for example a [PointerType] back from its [Type.root].
 */
val Type.wrapState: WrapState
    get() {
        if (this !is SecondOrderType) {
            return WrapState()
        }

        // We already know the depth, so we can just set this and allocate the pointer origins array
        val wrapState = WrapState(this.referenceDepth)

        var type: Type = this as Type
        // Let's unwrap
        var i = 0
        while (type is SecondOrderType) {
            var wrapType =
                if (type is ReferenceType) {
                    WrapState.Wrap.REFERENCE
                } else if (type is PointerType && type.isArray) {
                    WrapState.Wrap.ARRAY
                } else {
                    WrapState.Wrap.POINTER
                }

            wrapState.wraps[i++] = wrapType

            type = type.elementType
        }

        return wrapState
    }

/**
 * Wraps the given [Type] into a chain of [PointerType]s and [ReferenceType]s, given the
 * instructions in [WrapState].
 */
fun Type.wrap(wrapState: WrapState): Type {
    var type = this
    if (wrapState.wraps.isNotEmpty()) {
        for (i in wrapState.wraps.size - 1 downTo 0) {
            var wrap = wrapState.wraps[i]
            if (wrap == WrapState.Wrap.REFERENCE) {
                type = ReferenceType(type)
            } else if (wrap == WrapState.Wrap.ARRAY) {
                type = type.reference(PointerType.PointerOrigin.ARRAY)
            } else if (wrap == WrapState.Wrap.POINTER) {
                type = type.reference(PointerType.PointerOrigin.POINTER)
            }
        }
    }

    return type
}
