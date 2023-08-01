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

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TemplateDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TypedefDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.scopes.TemplateScope
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import org.apache.commons.lang3.builder.ToStringBuilder
import org.slf4j.LoggerFactory

class TypeManager {
    val typeCache: MutableMap<HasType, MutableList<Type>> =
        Collections.synchronizedMap(IdentityHashMap())

    private val typeToRecord = Collections.synchronizedMap(HashMap<Type?, RecordDeclaration?>())

    /**
     * Stores the relationship between parameterized RecordDeclarations (e.g. Classes using
     * Generics) to the ParameterizedType to be able to resolve the Type of the fields, since
     * ParameterizedTypes are unique to the RecordDeclaration and are not merged.
     */
    private val recordToTypeParameters =
        Collections.synchronizedMap(mutableMapOf<RecordDeclaration, List<ParameterizedType>>())
    private val templateToTypeParameters =
        Collections.synchronizedMap(
            mutableMapOf<TemplateDeclaration, MutableList<ParameterizedType>>()
        )

    val firstOrderTypes = Collections.synchronizedSet(HashSet<Type>())
    val secondOrderTypes = Collections.synchronizedSet(HashSet<Type>())

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
    fun getTypeParameter(
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

    fun <T : Type?> registerType(t: T): T {
        if (t!!.isFirstOrderType) {
            firstOrderTypes.add(t)
        } else {
            secondOrderTypes.add(t)
            registerType((t as SecondOrderType).elementType)
        }
        return t
    }

    fun typeExists(name: String): Boolean {
        return firstOrderTypes.stream().anyMatch { type: Type -> type.root.name.toString() == name }
    }

    @Synchronized
    fun cacheType(node: HasType, type: Type) {
        if (!isUnknown(type)) {
            val types = typeCache.computeIfAbsent(node) { mutableListOf() }
            if (!types.contains(type)) {
                types.add(type)
            }
        }
    }

    fun isUnknown(type: Type?): Boolean {
        return type is UnknownType
    }

    /**
     * @param generics the list of parameter types
     * @return true if the generics contain parameterized Types
     */
    fun containsParameterizedType(generics: List<Type?>): Boolean {
        for (t in generics) {
            if (t is ParameterizedType) {
                return true
            }
        }
        return false
    }

    /**
     * @param type oldType that we want to replace
     * @param newType newType
     * @return true if an objectType with instantiated generics is replaced by the same objectType
     *   with parameterizedTypes as generics false otherwise
     */
    fun stopPropagation(type: Type, newType: Type): Boolean {
        return if (type is ObjectType && newType is ObjectType && type.name == newType.name) {
            (containsParameterizedType(newType.generics) &&
                !containsParameterizedType(type.generics))
        } else false
    }

    private fun rewrapType(
        type: Type,
        depth: Int,
        pointerOrigins: Array<PointerOrigin?>,
        reference: Boolean,
        referenceType: ReferenceType?
    ): Optional<Type> {
        var type = type
        if (depth > 0) {
            for (i in depth - 1 downTo 0) {
                type = type.reference(pointerOrigins[i])
            }
        }
        if (reference) {
            referenceType!!.elementType = type
            return Optional.of(referenceType)
        }
        return Optional.of(type)
    }

    private fun unwrapTypes(types: Collection<Type>, wrapState: WrapState): Set<Type> {
        // TODO Performance: This method is called very often (for each setType()) and does four
        // iterations over "types". Reduce number of iterations.
        var types = types
        val original: Set<Type> = HashSet(types)
        val unwrappedTypes = mutableSetOf<Type>()
        var pointerOrigins = arrayOfNulls<PointerOrigin>(0)
        var depth = 0
        var counter = 0
        var reference = false
        var referenceType: ReferenceType? = null
        val t1 = types.stream().findAny().orElse(null)
        if (t1 is ReferenceType) {
            for (t in types) {
                referenceType = t as ReferenceType?
                if (!referenceType!!.isSimilar(t)) {
                    return emptySet()
                }
                unwrappedTypes.add(t.elementType)
                reference = true
            }
            types = unwrappedTypes
        }
        val t2 = types.stream().findAny().orElse(null)
        if (t2 is PointerType) {
            for (t in types) {
                if (counter == 0) {
                    depth = t.referenceDepth
                    counter++
                }
                if (t.referenceDepth != depth) {
                    return emptySet()
                }
                unwrappedTypes.add(t.root)
                pointerOrigins = arrayOfNulls(depth)
                var containedType: Type = t2
                var i = 0
                pointerOrigins[i] = (containedType as PointerType).pointerOrigin
                while (containedType is PointerType) {
                    containedType = containedType.elementType
                    if (containedType is PointerType) {
                        pointerOrigins[++i] = containedType.pointerOrigin
                    }
                }
            }
        }
        wrapState.depth = depth
        wrapState.setPointerOrigin(pointerOrigins)
        wrapState.isReference = reference
        wrapState.referenceType = referenceType
        return if (unwrappedTypes.isEmpty() && original.isNotEmpty()) {
            original
        } else {
            unwrappedTypes
        }
    }

    /**
     * This function is a relict from the old ages. It iterates through a collection of types and
     * returns the type they have in *common*. For example, if two types `A` and `B` both derive
     * from the interface `C`` then `C` would be returned. Because this contains some legacy code
     * that does crazy stuff, we need access to scope information, so we can build a map between
     * type information and their record declarations. We want to get rid of that in the future.
     *
     * @param types the types to compare
     * @param ctx a [TranslationContext].
     * @return the common type
     */
    fun getCommonType(types: Collection<Type>, ctx: TranslationContext?): Optional<Type> {
        var types = types
        val provider = ctx!!.scopeManager

        // TODO: Documentation needed.
        val sameType =
            (types
                .stream()
                .map { t: Type -> t.javaClass.canonicalName }
                .collect(Collectors.toSet())
                .size == 1)
        if (!sameType) {
            // No commonType for different Types
            return Optional.empty()
        }
        val wrapState = WrapState()
        types = unwrapTypes(types, wrapState)
        if (types.isEmpty()) {
            return Optional.empty()
        } else if (types.size == 1) {
            return rewrapType(
                types.iterator().next(),
                wrapState.depth,
                wrapState.pointerOrigins,
                wrapState.isReference,
                wrapState.referenceType
            )
        }
        val scope = provider.scope ?: return Optional.empty()

        // We need to find the global scope
        val globalScope = scope.globalScope ?: return Optional.empty()
        for (child in globalScope.children) {
            if (child is RecordScope && child.astNode is RecordDeclaration) {
                typeToRecord[(child.astNode as RecordDeclaration?)!!.toType()] =
                    child.astNode as RecordDeclaration?
            }

            // HACKY HACK HACK
            if (child is NameScope) {
                for (child2 in child.children) {
                    if (child2 is RecordScope && child2.astNode is RecordDeclaration) {
                        typeToRecord[(child2.astNode as RecordDeclaration?)!!.toType()] =
                            child2.astNode as RecordDeclaration?
                    }
                }
            }
        }
        val allAncestors =
            types
                .map { t: Type? -> typeToRecord.getOrDefault(t, null) }
                .filter { obj: RecordDeclaration? -> Objects.nonNull(obj) }
                .map { r: RecordDeclaration? -> getAncestors(r, 0) }

        // normalize/reverse depth: roots start at 0, increasing on each level
        for (ancestors in allAncestors) {
            val farthest = ancestors.stream().max(Comparator.comparingInt(Ancestor::depth))
            if (farthest.isPresent) {
                val maxDepth: Int = farthest.get().depth
                ancestors.forEach(Consumer { a: Ancestor -> a.depth = (maxDepth - a.depth) })
            }
        }
        var commonAncestors: MutableSet<Ancestor> = HashSet()
        for (i in allAncestors.indices) {
            if (i == 0) {
                commonAncestors.addAll(allAncestors[i])
            } else {
                val others = allAncestors[i]
                val newCommonAncestors = mutableSetOf<Ancestor>()
                // like Collection#retainAll but swaps relevant items out if the other set's
                // matching ancestor has a higher depth
                for (curr in commonAncestors) {
                    val toRetain =
                        others
                            .filter { a: Ancestor -> a == curr }
                            .map { a: Ancestor -> if (curr.depth >= a.depth) curr else a }
                            .firstOrNull()
                    toRetain?.let { newCommonAncestors.add(it) }
                }
                commonAncestors = newCommonAncestors
            }
        }
        val lca = commonAncestors.stream().max(Comparator.comparingInt(Ancestor::depth))
        val commonType = lca.map { it.record?.toType() ?: it.record.unknownType() }
        val finalType: Type
        finalType =
            if (commonType.isPresent) {
                commonType.get()
            } else {
                return commonType
            }
        return rewrapType(
            finalType,
            wrapState.depth,
            wrapState.pointerOrigins,
            wrapState.isReference,
            wrapState.referenceType
        )
    }

    private fun getAncestors(recordDeclaration: RecordDeclaration?, depth: Int): Set<Ancestor> {
        if (recordDeclaration!!.superTypes.isEmpty()) {
            val ret = HashSet<Ancestor>()
            ret.add(Ancestor(recordDeclaration, depth))
            return ret
        }
        val ancestors =
            recordDeclaration.superTypes
                .stream()
                .map { s: Type? -> typeToRecord.getOrDefault(s, null) }
                .filter { obj: RecordDeclaration? -> Objects.nonNull(obj) }
                .map { s: RecordDeclaration? -> getAncestors(s, depth + 1) }
                .flatMap { obj: Set<Ancestor> -> obj.stream() }
                .collect(Collectors.toSet())
        ancestors.add(Ancestor(recordDeclaration, depth))
        return ancestors
    }

    fun isSupertypeOf(superType: Type, subType: Type, provider: MetadataProvider): Boolean {
        var language: Language<*>? = null
        val ctx: TranslationContext?
        if (superType is UnknownType && subType is UnknownType) return true
        if (superType.referenceDepth != subType.referenceDepth) {
            return false
        }
        if (provider is LanguageProvider) {
            language = provider.language
        }
        ctx =
            if (provider is ContextProvider) {
                provider.ctx
            } else {
                log.error("Missing context provider")
                return false
            }

        // arrays and pointers match in C/C++
        // TODO: Make this independent from the specific language
        if (isCXX(language) && checkArrayAndPointer(superType, subType)) {
            return true
        }

        // ObjectTypes can be passed as ReferenceTypes
        if (superType is ReferenceType) {
            return isSupertypeOf(superType.elementType, subType, provider)
        }

        // We cannot proceed without a scope provider
        if (provider !is ScopeProvider) {
            return false
        }
        val commonType = getCommonType(HashSet(java.util.List.of(superType, subType)), ctx)
        return if (commonType.isPresent) {
            commonType.get() == superType
        } else {
            // If array depth matches: check whether these are types from the standard library
            try {
                val superCls = Class.forName(superType.typeName)
                val subCls = Class.forName(subType.typeName)
                superCls.isAssignableFrom(subCls)
            } catch (e: ClassNotFoundException) {
                // Not in the class path or other linkage exception, can't help here
                false
            } catch (e: NoClassDefFoundError) {
                false
            }
        }
    }

    private fun isCXX(language: Language<*>?): Boolean {
        return (language != null &&
            (language.javaClass.simpleName == "CLanguage" ||
                language.javaClass.simpleName == "CPPLanguage"))
    }

    fun checkArrayAndPointer(first: Type, second: Type): Boolean {
        val firstDepth = first.referenceDepth
        val secondDepth = second.referenceDepth
        return if (firstDepth == secondDepth) {
            (first.root.name == second.root.name && first.isSimilar(second))
        } else {
            false
        }
    }

    fun cleanup() {
        typeToRecord.clear()
    }

    /**
     * Creates a typedef / type alias in the form of a [TypedefDeclaration] to the scope manager and
     * returns it.
     *
     * @param frontend the language frontend
     * @param rawCode the raw code
     * @param target the target type
     * @param alias the alias type
     * @return the typedef declaration
     */
    fun createTypeAlias(
        frontend: LanguageFrontend<*, *>,
        rawCode: String?,
        target: Type,
        alias: Type,
    ): Declaration {
        var currTarget = target
        var currAlias = alias
        if (alias is SecondOrderType) {
            // TODO: I have NO clue what the following lines do and why they are necessary
            val chain = alias.duplicate()
            chain.root = currTarget
            currTarget = chain
            currTarget.refreshNames()
            currAlias = alias.root
        }
        val typedef = frontend.newTypedefDeclaration(currTarget, currAlias, rawCode)
        frontend.scopeManager.addTypedef(typedef)
        return typedef
    }

    fun resolvePossibleTypedef(alias: Type, scopeManager: ScopeManager): Type {
        val finalToCheck = alias.root
        val applicable =
            scopeManager.currentTypedefs
                .firstOrNull { t: TypedefDeclaration -> t.alias.root == finalToCheck }
                ?.type
        return if (applicable == null) {
            alias
        } else {
            reWrapType(alias, applicable)
        }
    }

    /**
     * Reconstructs the type chain when the root node is modified e.g. when swapping with alias
     * (typedef)
     *
     * @param oldChain containing all types until the root
     * @param newRoot root the chain is swapped with
     * @return oldchain but root replaced with newRoot
     */
    private fun reWrapType(oldChain: Type, newRoot: Type): Type {
        if (oldChain.isFirstOrderType) {
            newRoot.typeOrigin = oldChain.typeOrigin
        }
        if (!newRoot.isFirstOrderType) {
            return newRoot
        }
        return when {
            oldChain is ObjectType && newRoot is ObjectType -> {
                (newRoot.root as ObjectType).generics = oldChain.generics
                newRoot
            }
            oldChain is ReferenceType -> {
                val reference = reWrapType(oldChain.elementType, newRoot)
                val newChain = oldChain.duplicate() as ReferenceType
                newChain.elementType = reference
                newChain.refreshName()
                newChain
            }
            oldChain is PointerType -> {
                val newChain = oldChain.duplicate() as PointerType
                newChain.root = reWrapType(oldChain.root, newRoot)
                newChain.refreshNames()
                newChain
            }
            else -> newRoot
        }
    }

    private class Ancestor(val record: RecordDeclaration?, var depth: Int) {

        override fun hashCode(): Int {
            return Objects.hash(record)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is Ancestor) {
                return false
            }
            return record == other.record
        }

        override fun toString(): String {
            return ToStringBuilder(this, Node.TO_STRING_STYLE)
                .append("record", record!!.name)
                .append("depth", depth)
                .toString()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TypeManager::class.java)

        var isTypeSystemActive = true
    }
}
