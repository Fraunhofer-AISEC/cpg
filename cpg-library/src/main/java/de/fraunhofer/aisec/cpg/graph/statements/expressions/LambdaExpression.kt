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
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * This expression denotes the usage of an anonymous / lambda function. It connects the inner
 * anonymous function to the user of a lambda function with an expression.
 */
class LambdaExpression : Expression(), HasType.TypeListener {

    @field:SubGraph("AST")
    var function: FunctionDeclaration? = null
        set(value: FunctionDeclaration?) {
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

                // if the initializer implements a type listener, inform it about our type changes
                // since the type is tied to the declaration, but it is convenient to have the type
                // information in the initializer, i.e. in a ConstructExpression.
                if (value is HasType.TypeListener) {
                    registerTypeListener(value as HasType.TypeListener?)
                }
            }
        }

    override fun typeChanged(src: HasType?, root: HasType?, oldType: Type?) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }

        if (!TypeManager.getInstance().isUnknown(type) && src!!.propagationType == oldType) {
            return
        }

        val previous = type
        val newType: Type = src!!.propagationType

        setType(newType, root)
        if (previous != type) {
            type.typeOrigin = Type.Origin.DATAFLOW
        }
    }

    override fun possibleSubTypesChanged(
        src: HasType?,
        root: HasType?,
        oldSubTypes: MutableSet<Type>?
    ) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        val subTypes: MutableSet<Type> = HashSet(possibleSubTypes)
        subTypes.addAll(src!!.possibleSubTypes)
        setPossibleSubTypes(subTypes, root)
    }
}
