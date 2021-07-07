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

import de.fraunhofer.aisec.cpg.ExperimentalTypeScript
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.graph.types.UnknownType

@ExperimentalTypeScript
class TypeHandler(lang: TypeScriptLanguageFrontend?) :
    Handler<Type, TypeScriptNode, TypeScriptLanguageFrontend>(
        { UnknownType.getUnknownType() },
        lang,
    ) {

    init {
        map.put(TypeScriptNode::class.java, ::handleNode)
    }

    fun handleNode(node: TypeScriptNode): Type {
        when (node.type) {
            "TypeReference" -> return handleTypeReference(node)
            "AnyKeyword" -> return handleAnyKeyword()
            "NumberKeyword" -> return handleNumberKeyword()
            "StringKeyword" -> return handleStringKeyword()
        }

        return UnknownType.getUnknownType()
    }

    private fun handleStringKeyword(): Type {
        return TypeParser.createFrom("string", false)
    }

    private fun handleNumberKeyword(): Type {
        return TypeParser.createFrom("number", false)
    }

    private fun handleAnyKeyword(): Type {
        return TypeParser.createFrom("any", false)
    }

    private fun handleTypeReference(node: TypeScriptNode): Type {
        this.lang.getIdentifierName(node)?.let {
            return TypeParser.createFrom(it, false)
        }

        return UnknownType.getUnknownType()
    }
}
