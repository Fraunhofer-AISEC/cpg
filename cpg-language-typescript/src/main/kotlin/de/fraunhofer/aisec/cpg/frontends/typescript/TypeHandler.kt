/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.typescript

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.array
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.primitiveType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.graph.unknownType

class TypeHandler(frontend: TypeScriptLanguageFrontend) :
    Handler<Type, TypeScriptNode, TypeScriptLanguageFrontend>(frontend) {

    init {
        map.put(TypeScriptNode::class.java, ::handleNode)
    }

    fun handleNode(node: TypeScriptNode): Type {
        when (node.type) {
            "TypeReference" -> return handleTypeReference(node)
            "AnyKeyword" -> return handleAnyKeyword()
            "NumberKeyword" -> return handleNumberKeyword()
            "StringKeyword" -> return handleStringKeyword()
            "ArrayType" -> return handleArrayType(node)
        }

        return unknownType()
    }

    private fun handleArrayType(node: TypeScriptNode): Type {
        val type = node.firstChild("TypeReference")?.let { this.handle(it) } ?: unknownType()

        return type.array()
    }

    private fun handleStringKeyword(): Type {
        return primitiveType("string")
    }

    private fun handleNumberKeyword(): Type {
        return primitiveType("number")
    }

    private fun handleAnyKeyword(): Type {
        return objectType("any")
    }

    private fun handleTypeReference(node: TypeScriptNode): Type {
        node.firstChild("Identifier")?.let {
            return objectType(this.frontend.getIdentifierName(node))
        }

        return unknownType()
    }

    override val problemConstructor: (String, TypeScriptNode?) -> Type
        get() = { problem, rawNode -> UnknownType.getUnknownType(frontend.language) }
}
