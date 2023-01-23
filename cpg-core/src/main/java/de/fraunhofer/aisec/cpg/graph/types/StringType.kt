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

class StringType : ObjectType {

    constructor() : super()

    constructor(
        typeName: String,
        qualifier: Qualifier,
        generics: List<Type>,
        language: Language<out LanguageFrontend>?
    ) : super(typeName, qualifier, generics, Modifier.NOT_APPLICABLE, false, language)

    constructor(
        typeName: String,
        language: Language<out LanguageFrontend>?
    ) : super(typeName, Qualifier(), listOf(), Modifier.NOT_APPLICABLE, false, language)
    constructor(
        name: Name,
        qualifier: Qualifier,
        generics: List<Type>,
        language: Language<out LanguageFrontend>?
    ) : super(name, qualifier, generics, Modifier.NOT_APPLICABLE, false, language)

    override fun duplicate(): Type {
        return StringType(name, qualifier, generics, language)
    }
}
