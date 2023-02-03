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

import de.fraunhofer.aisec.cpg.graph.types.NumericType
import de.fraunhofer.aisec.cpg.graph.types.NumericType.Modifier
import de.fraunhofer.aisec.cpg.graph.types.ObjectType

fun MetadataProvider.newPrimitiveType(
    name: String,
    modifier: Modifier = Modifier.SIGNED,
): ObjectType {
    val type = (this as? LanguageProvider)?.language?.getSimpleTypeOf(name)
    if ((type as? NumericType)?.modifier != modifier) {
        // Try again but explicitly state "signed" or "unsigned" as our best guess.
        val modifierStr =
            when (modifier) {
                Modifier.SIGNED -> "signed "
                Modifier.UNSIGNED -> "unsigned "
                else -> ""
            }
        return (this as? LanguageProvider)?.language?.getSimpleTypeOf(modifierStr + name)
            as ObjectType
    }
    return type!!
}

fun ObjectType.const(): ObjectType {
    return ObjectType(this, listOf(), this.isPrimitive, this.language)
}
