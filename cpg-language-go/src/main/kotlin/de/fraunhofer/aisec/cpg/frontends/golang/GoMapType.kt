/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * Go's built-in map type, represented as a Java-like `map<K, V>`.
 *
 * Both type arguments are kept as [generics], because iterating a map with `range` yields the key
 * as well as the value. What a plain [ObjectType] cannot express is the result of subscripting:
 * [ObjectType.dereference] returns an unknown type, so `m[k]` had no type at all. That is not a
 * cosmetic loss -- an untyped value flows on, and a call on it or with it as an argument can no
 * longer be matched against a declaration. The comma-ok lookup `flag, ok := f.formal[name]` in
 * spf13/pflag is the typical case: `flag` ends up untyped, so `flag.Value.Set(value)` cannot be
 * dispatched at all.
 */
class GoMapType(
    typeName: CharSequence,
    val keyType: Type,
    val valueType: Type,
    language: Language<*>,
) : ObjectType(typeName, listOf(keyType, valueType), false, true, language) {

    /** Subscripting a map yields its value type. */
    override fun dereference(): Type = valueType

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GoMapType) return false
        return super.equals(other) && keyType == other.keyType && valueType == other.valueType
    }

    override fun hashCode(): Int = super.hashCode() * 31 + valueType.hashCode()
}
