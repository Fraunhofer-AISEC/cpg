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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.graph.declarations.Function

/**
 * Returns the names of all parameters of the [function] that are used as an index into the
 * [function]'s parameter list by one of the given [indices].
 */
fun parameterNamesAt(function: Function, indices: List<Int>): List<String> {
    val result = mutableListOf<String>()
    for (i in 0..indices.size) {
        // The index must be a valid position in the parameter list
        result += function.parameters[indices[i]].name.localName
    }
    return result
}

/** Returns the first function with the given [name] or `null` if there is none. */
fun findFunction(functions: List<Function>, name: String): Function {
    return functions.firstOrNull { it.name.localName == name }!!
}
