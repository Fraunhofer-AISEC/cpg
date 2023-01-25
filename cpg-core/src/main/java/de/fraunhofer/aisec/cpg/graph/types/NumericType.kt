/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
import java.util.*

/** This type collects all kind of numeric types. */
open class NumericType : ObjectType {
    val bitWidth: Int?
    val modifier: Modifier

    constructor() : super() {
        bitWidth = null
        modifier = Modifier.SIGNED
    }

    constructor(
        typeName: String,
        bitWidth: Int?,
        language: Language<out LanguageFrontend>?,
        modifier: Modifier = Modifier.SIGNED
    ) : super(typeName, listOf(), true, language) {
        this.bitWidth = bitWidth
        this.modifier = modifier
    }

    constructor(
        typeName: String,
        modifier: Modifier,
        language: Language<out LanguageFrontend>?,
        bitWidth: Int?
    ) : super(typeName, listOf(), true, language) {
        this.bitWidth = bitWidth
        this.modifier = modifier
    }

    constructor(
        typeName: Name,
        modifier: Modifier,
        language: Language<out LanguageFrontend>?,
        bitWidth: Int?
    ) : super(typeName, listOf(), true, language) {
        this.bitWidth = bitWidth
        this.modifier = modifier
    }

    override fun duplicate(): Type {
        return NumericType(this.name, modifier, language, bitWidth)
    }

    /**
     * NumericTypes can have a modifier. The default is signed. Some types (e.g. char in C) may be
     * neither of the signed/unsigned option. TODO: maybe replace with a flag "signed" or
     * "unsigned"?
     */
    enum class Modifier {
        SIGNED,
        UNSIGNED,
        NOT_APPLICABLE
    }

    override fun equals(other: Any?) =
        super.equals(other) && this.modifier == (other as? NumericType)?.modifier

    override fun hashCode() = Objects.hash(super.hashCode(), generics, modifier, primitive)
}
