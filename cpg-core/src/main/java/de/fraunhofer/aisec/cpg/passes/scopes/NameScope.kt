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
package de.fraunhofer.aisec.cpg.passes.scopes

import de.fraunhofer.aisec.cpg.graph.Node

open class NameScope(node: Node, currentPrefix: String, var delimiter: String) :
    StructureDeclarationScope(node) {
    var namePrefix: String = ""

    init {
        if (currentPrefix.isNotEmpty()) {
            var nodeName = node.name
            // If the name already contains some form of prefix we have to remove it.
            nodeName =
                if (nodeName.contains(delimiter))
                    nodeName.substring(nodeName.lastIndexOf(delimiter) + delimiter.length)
                else nodeName
            namePrefix = currentPrefix + delimiter + nodeName
        } else {
            namePrefix = node.name
        }
        astNode = node
    }

    // Split scoped named by delimiter
    val simpleName: String?
        get() = scopedName?.split(delimiter)?.lastOrNull { it.isNotEmpty() }
}
