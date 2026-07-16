/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.types.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Creates a new [UnknownType] and sets the appropriate language, if this [MetadataProvider]
 * includes a [LanguageProvider].
 */
fun MetadataProvider?.unknownType(): Type {
    return if (this is LanguageProvider) {
        UnknownType.getUnknownType(language)
    } else {
        UnknownType.getUnknownType(null)
    }
}

fun LanguageProvider.autoType(): Type {
    return AutoType(this.language)
}

fun LanguageProvider.incompleteType(): Type {
    return IncompleteType(this.language)
}

fun LanguageProvider.dynamicType(): Type {
    return DynamicType(this.language)
}

/** Returns a [PointerType] that describes an array reference to the current type. */
fun Type.array(): Type {
    val type = this.reference(PointerType.PointerOrigin.ARRAY)

    return type
}

/** Returns a [PointerType] that describes a pointer reference to the current type. */
fun Type.pointer(): Type {
    val type = this.reference(PointerType.PointerOrigin.POINTER)

    return type
}

fun Type.ref(): Type {
    val type = ReferenceType(this)

    return type
}

/**
 * This function returns an [ObjectType] with the given [name]. If a respective [Type] does not yet
 * exist (in the current scope), it will be created. In order to avoid unnecessary allocation of
 * simple types, we do a pre-check within this function, whether a built-in type exist with the
 * particular name.
 */
@JvmOverloads
fun LanguageProvider.objectType(
    name: CharSequence,
    generics: List<Type> = listOf(),
    rawNode: Any? = null,
): Type {
    // First, we check, whether this is a built-in type, to avoid necessary allocations of simple
    // types
    val builtIn = language.getSimpleTypeOf(name.toString())
    if (builtIn != null) {
        return builtIn
    }

    // Conservative interning: for a non-generic named type we reuse a single ObjectType instance
    // per
    // (scope, name) within the current translation context (see
    // TranslationContext.objectTypeCache).
    // Every reference to the same named type in the same scope resolves identically, so sharing one
    // instance avoids allocating a fresh, redundant ObjectType for each use. Types with generics,
    // or
    // created without a scope/context, keep their previous fresh-per-call behavior.
    val scope = (this as? ScopeProvider)?.scope
    val ctx = (this as? ContextProvider)?.ctx
    if (generics.isEmpty() && scope != null && ctx != null) {
        return ctx.objectTypeCache
            .computeIfAbsent(scope) { ConcurrentHashMap() }
            .computeIfAbsent(name.toString()) { createObjectType(name, generics, rawNode) }
    }

    return createObjectType(name, generics, rawNode)
}

/**
 * Creates a fresh [ObjectType] with the given [name] and [generics] and applies the usual metadata
 * (scope, code, location). We only refer to the type by its local name, because we treat types as
 * sort of references when creating them and resolve them later. Callers should prefer [objectType],
 * which additionally interns simple named types.
 */
private fun LanguageProvider.createObjectType(
    name: CharSequence,
    generics: List<Type>,
    rawNode: Any?,
): ObjectType {
    val type =
        ObjectType(
            typeName = name,
            generics = generics,
            primitive = false,
            mutable = true,
            language = language,
        )
    type.applyMetadata(this, name, rawNode = rawNode, doNotPrependNamespace = true)
    return type
}

/**
 * This function constructs a new primitive [Type]. Primitive or built-in types are defined in
 * [Language.builtInTypes]. This function will look up the type by its name, if it fails to find an
 * appropriate build-in type, a [TranslationException] is thrown. Therefore, this function should
 * primarily be called by language frontends if they are sure that this type is a built-in type,
 * e.g., when constructing literals. It can be useful, if frontends want to check, whether all
 * literal types are correctly registered as built-in types.
 *
 * If the frontend is not sure, what kind of type it is, it should call [objectType], which also
 * does a check, whether it is a known built-in type.
 */
fun LanguageProvider.primitiveType(name: CharSequence): Type {
    return language.getSimpleTypeOf(name.toString())
        ?: throw TranslationException(
            "Cannot find primitive type $name in language ${language.name}. This is either an error in the language frontend or the language definition is missing a type definition."
        )
}

/**
 * Checks, whether the given [Type] is a primitive in the language specified in the
 * [LanguageProvider].
 */
fun LanguageProvider.isPrimitive(type: Type): Boolean {
    return language.primitiveTypeNames.contains(type.typeName)
}
