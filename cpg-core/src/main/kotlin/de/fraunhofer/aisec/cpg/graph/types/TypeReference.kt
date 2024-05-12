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
package de.fraunhofer.aisec.cpg.graph.types

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.DeclaresType

/**
 * This class represents a usage of a [Type]. This class should primarily be used in all nodes that
 * implement [HasType] because usually during the translation phase, we do not know the exact type
 * (i.e., its FQN). Therefore, this class should be in the [HasType.type], with one of the very few
 * exceptions being types in a [Language.builtInTypes].
 */
class TypeReference(name: CharSequence, generics: List<Type>, language: Language<*>?) :
    Type(name, generics, language) {

    var refersTo: DeclaresType? = null

    val referringType: Type?
        get() {
            return refersTo?.declaringType
        }

    override var typeOrigin: Origin?
        get() {
            return if (refersTo == null) {
                Origin.UNRESOLVED
            } else {
                Origin.RESOLVED
            }
        }
        set(_) {}

    override fun reference(pointer: PointerType.PointerOrigin?): Type {
        TODO("Not yet implemented")
    }

    override fun dereference(): Type {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Type) return false

        // If this reference is unresolved, we can compare it directly to other type references.  If
        // it is resolved, we need to delegate the equals to our referredType
        return referringType?.equals(other) ?: super.equals(other)
    }

    override fun hashCode(): Int {
        return referringType?.hashCode() ?: super.hashCode()
    }
}
