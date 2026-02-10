/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import org.treesitter.TSNode

/**
 * A [Handler] that translates Rust type nodes into CPG [Type] nodes. It supports primitive types,
 * references, arrays, and type aliases.
 */
class TypeHandler(frontend: RustLanguageFrontend) :
    RustHandler<Type, TSNode>({ UnknownType.getUnknownType(null) }, frontend) {

    override fun handleNode(node: TSNode): Type {
        return when (node.type) {
            "primitive_type" -> handlePrimitiveType(node)
            "type_identifier" -> handleTypeIdentifier(node)
            "reference_type" -> handleReferenceType(node)
            "tuple_type" -> handleTupleType(node)
            "array_type" -> handleArrayType(node)
            "generic_type" -> handleGenericType(node)
            else -> {
                objectType(frontend.codeOf(node) ?: "")
            }
        }
    }

    private fun handlePrimitiveType(node: TSNode): Type {
        return objectType(frontend.codeOf(node) ?: "")
    }

    private fun handleTypeIdentifier(node: TSNode): Type {
        return objectType(frontend.codeOf(node) ?: "")
    }

    private fun handleReferenceType(node: TSNode): Type {
        val typeNode = node.getChildByFieldName("type") ?: node.getNamedChild(0)
        val type = if (typeNode != null) handle(typeNode) else unknownType()
        return type.ref()
    }

    private fun handleTupleType(node: TSNode): Type {
        // Rust tuples are complex, but for now we'll just treat them as ObjectType
        return objectType(frontend.codeOf(node) ?: "")
    }

    private fun handleArrayType(node: TSNode): Type {
        val typeNode = node.getChildByFieldName("type") ?: node.getNamedChild(0)
        val type = if (typeNode != null) handle(typeNode) else unknownType()
        return type.array()
    }

    private fun handleGenericType(node: TSNode): Type {
        val typeNode = node.getChildByFieldName("type")
        val typeName = typeNode?.let { frontend.codeOf(it) } ?: ""

        // TODO: Handle generics (e.g. Vec<i32>)
        return objectType(typeName)
    }
}
