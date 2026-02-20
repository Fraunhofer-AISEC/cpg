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
package de.fraunhofer.aisec.cpg.frontends.experimental.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.TupleType
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
            "generic_type_with_turbofish" -> handleGenericType(node)
            "bounded_type" -> {
                val firstType = node.getNamedChild(0)
                if (firstType != null && !firstType.isNull) handle(firstType)
                else objectType(node.text())
            }
            "qualified_type" -> objectType(node.text())
            else -> {
                objectType(node.text())
            }
        }
    }

    /** Translates a Rust `primitive_type` (e.g., `i32`, `bool`, `str`) into an object [Type]. */
    private fun handlePrimitiveType(node: TSNode): Type {
        return objectType(node.text())
    }

    /** Translates a Rust `type_identifier` (e.g., a named type like `MyStruct`) into a [Type]. */
    private fun handleTypeIdentifier(node: TSNode): Type {
        return objectType(node.text())
    }

    /**
     * Translates a Rust `reference_type` (e.g., `&T`, `&mut T`, `&'a T`) into a reference [Type].
     */
    private fun handleReferenceType(node: TSNode): Type {
        // reference_type children: &, optional lifetime, optional mutable_specifier, type
        val typeNode = node["type"]
        val type = if (typeNode != null && !typeNode.isNull) handle(typeNode) else unknownType()
        return type.ref()
    }

    /** Translates a Rust `tuple_type` (e.g., `(i32, String)`) into a [TupleType]. */
    private fun handleTupleType(node: TSNode): Type {
        val elementTypes = mutableListOf<Type>()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child.isNamed) {
                elementTypes += handle(child)
            }
        }
        return TupleType(elementTypes)
    }

    /** Translates a Rust `array_type` (e.g., `[i32; 5]`) into an array [Type]. */
    private fun handleArrayType(node: TSNode): Type {
        val typeNode = node["type"]
        val type = if (typeNode != null && !typeNode.isNull) handle(typeNode) else unknownType()
        return type.array()
    }

    /**
     * Translates a Rust `generic_type` (e.g., `Vec<i32>`, `HashMap<K, V>`) or
     * `generic_type_with_turbofish` into an object [Type] with generic parameters.
     */
    private fun handleGenericType(node: TSNode): Type {
        val typeNode = node["type"]
        val typeName = typeNode.text().ifEmpty { node.text() }
        val typeArgs = node.getChildByFieldName("type_arguments")
        val generics = mutableListOf<Type>()
        if (typeArgs != null && !typeArgs.isNull) {
            for (i in 0 until typeArgs.childCount) {
                val arg = typeArgs.getChild(i)
                if (arg.isNamed) {
                    generics += handle(arg)
                }
            }
        }
        return objectType(typeName, generics)
    }

    /**
     * Translates a Rust `function_type` (e.g., `fn(i32) -> bool`) into a [FunctionType] with
     * parameter types and return type.
     */
    private fun handleFunctionType(node: TSNode): Type {
        val paramTypes = mutableListOf<Type>()
        val params = node.getChildByFieldName("parameters")
        if (params != null && !params.isNull) {
            for (i in 0 until params.childCount) {
                val child = params.getChild(i)
                if (child.isNamed) paramTypes += handle(child)
            }
        }
        val returnNode = node.getChildByFieldName("return_type")
        val returnTypes =
            if (returnNode != null && !returnNode.isNull) {
                listOf(handle(returnNode))
            } else {
                listOf(objectType("()"))
            }
        return FunctionType(
            node.text().ifEmpty { "fn" },
            paramTypes,
            returnTypes,
            frontend.language,
        )
    }

    /** Translates a Rust `abstract_type` (e.g., `impl Trait`) into an object [Type]. */
    private fun handleAbstractType(node: TSNode): Type {
        // impl Trait
        val traitNode = node.getNamedChild(0)
        val name = traitNode.text()
        return objectType("impl $name")
    }

    /** Translates a Rust `dynamic_type` (e.g., `dyn Trait`) into an object [Type]. */
    private fun handleDynamicType(node: TSNode): Type {
        // dyn Trait
        val traitNode = node.getNamedChild(0)
        val name = traitNode.text()
        return objectType("dyn $name")
    }

    /** Translates a Rust `scoped_type_identifier` (e.g., `std::vec::Vec`) into an object [Type]. */
    private fun handleScopedTypeIdentifier(node: TSNode): Type {
        // std::vec::Vec
        return objectType(node.text())
    }

    /** Translates a Rust `pointer_type` (e.g., `*const T`, `*mut T`) into a pointer [Type]. */
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
