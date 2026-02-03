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
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration.Companion.BRACKET_LEFT
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration.Companion.BRACKET_RIGHT
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration.Companion.COMMA
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration.Companion.WHITESPACE
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
    language: Language<*>,
) : Type(typeName, language), HasSecondaryTypeEdge {

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

    override val secondaryTypes: List<Type>
        get() = parameters + returnTypes

    companion object {
        /**
         * This helper function computes a [FunctionType] out of an existing [FunctionDeclaration].
         */
        @JvmStatic
        fun computeType(
            func: FunctionDeclaration,
            returnTypes: List<Type> = func.returnTypes.toList(),
        ): FunctionType {
            val type =
                FunctionType(
                    buildSignature(func, returnTypes),
                    func.parameters.map { it.type },
                    returnTypes,
                    func.language,
                )

            return type
        }

        /**
         * This helper function builds a function signature out of an existing [FunctionDeclaration]
         * and potential return types.
         *
         * Its main use-case is to have a human-readable representation of the function type. For
         * example `foo(Bar)string` for a function `foo` with parameter types `Bar` and return type
         * `string`.
         */
        fun buildSignature(func: FunctionDeclaration, returnTypes: List<Type>): String =
            func.name.localName +
                func.parameters.joinToString(COMMA + WHITESPACE, BRACKET_LEFT, BRACKET_RIGHT) {
                    it.type.typeName
                } +
                (if (returnTypes.size == 1) {
                    returnTypes.first().typeName
                } else {
                    returnTypes.joinToString(COMMA + WHITESPACE, BRACKET_LEFT, BRACKET_RIGHT) {
                        it.typeName
                    }
                })
    }
}
