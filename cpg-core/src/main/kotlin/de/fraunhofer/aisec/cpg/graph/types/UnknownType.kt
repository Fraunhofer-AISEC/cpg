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
package de.fraunhofer.aisec.cpg.graph.types

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import java.util.*

/**
 * UnknownType describe the case in which it is not possible for the CPG to determine which Type is
 * used. E.g.: This occurs when the type is inferred by the compiler automatically when using
 * keywords such as auto in cpp
 */
class UnknownType : Type {
    private constructor() : super() {
        name = Name(UNKNOWN_TYPE_STRING, null, language)
    }

    /**
     * This is only intended to be used by [TypeParser] for edge cases like distinct unknown types,
     * such as "UNKNOWN1", thus the package-private visibility. Other users should see
     * [getUnknownType] instead
     *
     * @param typeName The name of this unknown type, usually a variation of UNKNOWN
     */
    internal constructor(typeName: String?) : super(typeName)

    /**
     * @return Same UnknownType, as it is makes no sense to obtain a pointer/reference to an
     *   UnknownType
     */
    override fun reference(pointer: PointerOrigin?): Type {
        return this
    }

    /** @return Same UnknownType, */
    override fun dereference(): Type {
        return this
    }

    override fun duplicate(): Type {
        // We don't duplicate because we cannot change any properties.
        return this
    }

    override fun hashCode() = Objects.hash(super.hashCode())

    override fun equals(other: Any?): Boolean {
        return other is UnknownType
    }

    override fun toString(): String {
        return "UNKNOWN"
    }

    override var typeOrigin: Origin? = null

    companion object {
        /** A map of [UnknownType] and their respective [Language]. */
        private val unknownTypes = mutableMapOf<Language<*>?, UnknownType>()

        /** Use this function to obtain an [UnknownType] for the particular [language]. */
        @JvmStatic
        fun getUnknownType(language: Language<out LanguageFrontend>?): UnknownType {
            return unknownTypes.computeIfAbsent(language) {
                val unknownType = UnknownType()
                unknownType.language = language
                unknownType
            }
        }
    }
}
