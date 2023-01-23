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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.concurrent.ConcurrentHashMap

/** The type cache will be a replacement for the [LegacyTypeManager]. */
class TypeCache {

    var types = ConcurrentHashMap<Name, Type>()

    fun getOrPut(name: Name, defaultValue: () -> Type): Type {
        return types.getOrPut(name, defaultValue)
    }
}

interface TypeCacheProvider : LanguageProvider {

    val typeManager: TypeCache
}

fun TypeCacheProvider.newObjectType(name: CharSequence): Type {
    val namespace = (this as? NamespaceProvider)?.namespace // TODO: Maybe remove this again?
    val fqn = this.newName(name, namespace = namespace)

    return this.typeManager.getOrPut(fqn) {
        val type = ObjectType()
        type.applyMetadata(this, name)

        return@getOrPut type
    }
}
