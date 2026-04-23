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
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import java.util.Objects

/**
 * An [AliasType] represents a type alias (e.g., from C/C++ typedef or C++ using declarations). It
 * holds a reference to the underlying type and acts as a first-order type.
 *
 * Example: `typedef struct { int A; } S;` creates an AliasType("S") that wraps the anonymous
 * ObjectType representing the struct.
 *
 * Note: Since [Type] properties are final, this class delegates to the underlying type by providing
 * access via [underlyingType] and [underlyingTypeRoot]. Code that needs to access properties like
 * [Record] declarations, [superTypes], etc. should use [underlyingTypeRoot] or check if the type is
 * an [AliasType] and access [underlyingType] accordingly.
 */
class AliasType(aliasName: CharSequence, val underlyingType: Type, language: Language<*>) :
    Type(aliasName, language) {

    constructor(
        aliasName: Name,
        underlyingType: Type,
        language: Language<*>,
    ) : this(aliasName.toString(), underlyingType, language) {
        this.name = aliasName
    }

    override fun reference(pointer: PointerOrigin?): Type {
        return underlyingType.reference(pointer)
    }

    override fun dereference(): Type {
        return underlyingType.dereference()
    }

    override fun refreshNames() {
        super.refreshNames()
        underlyingType.refreshNames()
    }

    val underlyingTypeRoot: Type
        get() = underlyingType.root

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        // For backward compatibility: AliasType equals its underlying type when names match
        if (other is Type) {
            // If names match, consider them equal for typedef compatibility
            if (name == other.name && language == other.language) {
                return true
            }
        }
        return false
    }

    override fun hashCode() = Objects.hash(super.hashCode(), underlyingType)

    override fun toString(): String {
        return "${super.toString()} -> $underlyingType"
    }
}
