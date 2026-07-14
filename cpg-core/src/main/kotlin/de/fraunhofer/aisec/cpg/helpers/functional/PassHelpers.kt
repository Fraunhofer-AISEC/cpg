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
package de.fraunhofer.aisec.cpg.helpers.functional

import de.fraunhofer.aisec.cpg.IncompatibleSignature
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.pointer
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.ProblemType
import de.fraunhofer.aisec.cpg.matchesSignature
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log

/*
harmonize all types to the FunctionPointerType. In the future, we want to get rid of FunctionPointerType and
only deal with FunctionTypes.
 */
fun getFunctionPointerType(expr: Expression): FunctionPointerType? {
    return when (val type = expr.type) {
        is FunctionType -> {
            when (val pointerType = type.pointer()) {
                is FunctionPointerType -> pointerType
                is ProblemType -> {
                    log.warn("Function has unexpected type: ProblemType; ignore call")
                    null
                }
                else -> {
                    log.warn("Unexpected function type: ${pointerType}; ignore call")
                    null
                }
            }
        }
        is FunctionPointerType -> type
        else -> {
            // some languages allow other types to derive from a function type, in this case
            // we need to look for a super type
            val superType = type.superTypes.singleOrNull()
            if (superType is FunctionType) {
                superType.pointer() as FunctionPointerType
            } else {
                null
            }
        }
    }
}

/*
Checks if a given function could be called with a function pointer
returns: false if the function does not match the signature of the callee, or true if it does match
*/
fun matchInvokesCandidateSignature(
    currentFunction: Function,
    pointerType: FunctionPointerType,
    isLambda: Boolean,
): Boolean {
    // Even if it is a function declaration, the dataflow might just come from a
    // situation where the target of a fptr is passed through via a return value. Keep
    // searching if return type or signature don't match
    val functionPointerType = currentFunction.type.pointer()
    return if (
        isLambda &&
            currentFunction.returnTypes.isEmpty() &&
            currentFunction.matchesSignature(pointerType.parameters) != IncompatibleSignature
    ) {
        true
    } else if (functionPointerType == pointerType) {
        true
    } else false
}
