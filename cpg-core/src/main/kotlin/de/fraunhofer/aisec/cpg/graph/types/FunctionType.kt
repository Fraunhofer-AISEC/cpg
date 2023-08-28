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
package de.fraunhofer.aisec.cpg.graph.types

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDecl
import de.fraunhofer.aisec.cpg.graph.unknownType

/**
 * A type representing a function. It contains a list of parameters and one or more return types.
 *
 * It can be referenced into a [FunctionPointerType]. In the future, we will probably change this
 * and remove the [FunctionPointerType] and directly use a [PointerType].
 */
class FunctionType
@JvmOverloads
constructor(
    typeName: String = "",
    var parameters: List<Type> = listOf(),
    var returnTypes: List<Type> = listOf(),
    language: Language<*>? = null
) : Type(typeName, language) {

    override fun reference(pointer: PointerType.PointerOrigin?): Type {
        // TODO(oxisto): In the future, we actually could just remove the FunctionPointerType
        //  and just have a regular PointerType here
        return FunctionPointerType(
            parameters.toList(),
            language,
            returnTypes.firstOrNull() ?: unknownType(),
        )
    }

    override fun dereference(): Type {
        return unknownType()
    }

    override fun duplicate(): Type {
        return FunctionType(typeName, parameters.toList(), returnTypes.toList(), language)
    }

    companion object {
        /** This helper function computes a [FunctionType] out of an existing [FunctionDecl]. */
        @JvmStatic
        fun computeType(
            func: FunctionDecl,
        ): FunctionType {
            val type =
                FunctionType(
                    func.signature,
                    func.parameters.map { it.type },
                    func.returnTypes.toList(),
                    func.language
                )

            return func.registerType(type)
        }
    }
}
