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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * This expression denotes the usage of an anonymous / lambda function. It connects the inner
 * anonymous function to the user of a lambda function with an expression.
 */
class LambdaExpression : Expression(), HasType.TypeListener {

    @field:SubGraph("AST")
    var function: FunctionDeclaration? = null
        set(value) {
            if (value != null) {
                removePrevDFG(value)
                value.unregisterTypeListener(this)
                if (value is HasType.TypeListener) {
                    unregisterTypeListener(value as HasType.TypeListener?)
                }
            }
            field = value
            if (value != null) {
                addPrevDFG(value)
                value.registerTypeListener(this)
            }
        }

    override fun typeChanged(src: HasType?, root: Collection<HasType>?, oldType: Type?) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }

        if (!TypeManager.getInstance().isUnknown(type) && src!!.propagationType == oldType) {
            return
        }

        if (src !is FunctionDeclaration) {
            return
        }

        val previous = type

        val parameterTypes = src.parameters.map { it.type }
        val returnType = src.propagationType

        // the incoming "type" is associated to the function and it is only its return type (if it
        // is known). what we really want is to construct a function type, or rather a function
        // pointer type, since this is the closest to what we have
        val functionType =
            FunctionPointerType(
                Type.Qualifier(false, false, false, false),
                Type.Storage.AUTO,
                parameterTypes,
                returnType
            )

        setType(functionType, root)
        if (previous != type) {
            type.typeOrigin = Type.Origin.DATAFLOW
        }
    }

    override fun possibleSubTypesChanged(
        src: HasType?,
        root: Collection<HasType>?,
        oldSubTypes: MutableSet<Type>?
    ) {
        // do not take sub types from the listener
    }
}
