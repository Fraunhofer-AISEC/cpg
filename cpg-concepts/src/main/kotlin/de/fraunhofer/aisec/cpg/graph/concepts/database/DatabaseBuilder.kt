/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.concepts.database

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression

// TODO: move to a better location
fun MetadataProvider.newDatabase(underlyingNode: Node): Database {
    val node = Database(underlyingNode = underlyingNode)
    node.codeAndLocationFrom(underlyingNode)

    node.name = Name("Database[" + underlyingNode.name.toString() + "]")

    NodeBuilder.log(node)
    return node
}

fun MetadataProvider.newDatabaseAdd(
    underlyingNode: Node,
    db: Database,
    what: Node?,
): DatabaseOpAdd {
    val node = DatabaseOpAdd(underlyingNode = underlyingNode, concept = db, what = what)
    node.codeAndLocationFrom(underlyingNode)

    node.name = Name("DatabaseAdd[" + underlyingNode.name.toString() + "]")

    (underlyingNode as? CallExpression)?.let { it.arguments.forEach { arg -> arg.nextDFG += node } }

    NodeBuilder.log(node)
    return node
}
