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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration

class MemoryAddress(override var name: Name) : Declaration() {
    /*
     * When the node represents the MemoryAddress of a struct or an array, we use the fieldAddresses map to store the MemoryAddresses of the different fields.
     * Therefore, for structs the key should be a FieldDeclaration.
     * For arrays, it may also be a literal if the MemoryAddress is accesses with something like `array[0]`
     */
    // FIXME: The FieldDeclarations don't seem to be unique. Also, for arrays, the literals in
    // different lines won't be the same, so we try a string as index
    val fieldAddresses = mutableMapOf<String, Set<MemoryAddress>>()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MemoryAddress) {
            return false
        }
        // TODO: What else do we need to compare?
        return name == other.name && fieldAddresses == other.fieldAddresses
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
