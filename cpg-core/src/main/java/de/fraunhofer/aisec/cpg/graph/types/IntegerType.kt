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

/** Instances of this class represent integer types. */
class IntegerType : NumericType {
    constructor() : super()

    constructor(
        typeName: String,
        bitWidth: Int?,
        language: Language<out LanguageFrontend>?,
        modifier: Modifier = Modifier.SIGNED
    ) : super(typeName, bitWidth, language, modifier)
    constructor(
        typeName: String,
        modifier: Modifier,
        language: Language<out LanguageFrontend>?,
        bitWidth: Int?
    ) : super(typeName, modifier, language, bitWidth)

    constructor(
        name: Name,
        modifier: Modifier,
        language: Language<out LanguageFrontend>?,
        bitWidth: Int?
    ) : super(name, modifier, language, bitWidth)

    override fun duplicate(): Type {
        return IntegerType(this.name, modifier, language, bitWidth)
    }
}
