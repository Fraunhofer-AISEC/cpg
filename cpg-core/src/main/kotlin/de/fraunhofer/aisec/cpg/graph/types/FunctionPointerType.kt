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
package de.fraunhofer.aisec.cpg.graph.types

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * FunctionPointerType represents function pointers containing a list of parameters and a return
 * type.
 *
 * This class is currently only used in the C++ language frontend.
 *
 * TODO(oxisto): We want to replace this dedicated type with a simple [PointerType] to a
 *   [FunctionType] in the future
 */
class FunctionPointerType : Type {
    @Relationship(value = "PARAMETERS", direction = Relationship.Direction.OUTGOING)
    var parameters: List<Type>

    var returnType: Type

    constructor(
        ctx: TranslationContext?,
        parameters: List<Type> = listOf(),
        language: Language<*>,
        returnType: Type = UnknownType.getUnknownType(language),
    ) : super(ctx, Name(EMPTY_NAME), language) {
        this.parameters = parameters
        this.returnType = returnType
    }

    override fun reference(pointer: PointerOrigin?): PointerType {
        return PointerType(ctx, this, pointer)
    }

    override fun dereference(): Type {
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionPointerType) return false
        return super.equals(other) &&
            parameters == other.parameters &&
            returnType == other.returnType
    }

    override fun hashCode() = Objects.hash(super.hashCode(), parameters, returnType)

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("parameters", parameters)
            .append("returnType", returnType)
            .append("typeOrigin", typeOrigin)
            .toString()
    }
}
