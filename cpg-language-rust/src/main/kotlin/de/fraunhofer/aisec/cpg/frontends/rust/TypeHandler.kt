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
            "function_type" -> handleFunctionType(node)
            "abstract_type" -> handleAbstractType(node)
            "dynamic_type" -> handleDynamicType(node)
            "scoped_type_identifier" -> handleScopedTypeIdentifier(node)
            "pointer_type" -> handlePointerType(node)
            "unit_type" -> objectType("()")
            "never_type" -> objectType("!")
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
        // reference_type children: &, optional lifetime, optional mutable_specifier, type
        var typeNode = node.getChildByFieldName("type")
        if (typeNode == null || typeNode.isNull) {
            // Fallback: find the last named child that isn't a lifetime or mutable_specifier
            for (i in node.childCount - 1 downTo 0) {
                val child = node.getChild(i)
                if (
                    child.isNamed && child.type != "lifetime" && child.type != "mutable_specifier"
                ) {
                    typeNode = child
                    break
                }
            }
        }
        val type = if (typeNode != null && !typeNode.isNull) handle(typeNode) else unknownType()
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

    private fun handleFunctionType(node: TSNode): Type {
        // fn(i32, i32) -> i32
        // Model as ObjectType with the full code representation
        return objectType(frontend.codeOf(node) ?: "fn")
    }

    private fun handleAbstractType(node: TSNode): Type {
        // impl Trait
        val traitNode = node.getNamedChild(0)
        val name =
            if (traitNode != null && !traitNode.isNull) frontend.codeOf(traitNode) ?: "" else ""
        return objectType("impl $name")
    }

    private fun handleDynamicType(node: TSNode): Type {
        // dyn Trait
        val traitNode = node.getNamedChild(0)
        val name =
            if (traitNode != null && !traitNode.isNull) frontend.codeOf(traitNode) ?: "" else ""
        return objectType("dyn $name")
    }

    private fun handleScopedTypeIdentifier(node: TSNode): Type {
        // std::vec::Vec
        return objectType(frontend.codeOf(node) ?: "")
    }

    private fun handlePointerType(node: TSNode): Type {
        // *const T or *mut T
        // Find the inner type child (skip mutable_specifier)
        var typeNode: TSNode? = null
        for (i in node.childCount - 1 downTo 0) {
            val child = node.getChild(i)
            if (child.isNamed && child.type != "mutable_specifier") {
                typeNode = child
                break
            }
        }
        val type = if (typeNode != null && !typeNode.isNull) handle(typeNode) else unknownType()
        return type.pointer()
    }
}
