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
package de.fraunhofer.aisec.cpg

/**
 * This class holds configuration options for the inference of certain constructs and auto-guessing
 * when executing language frontends.
 */
class InferenceConfiguration
private constructor(
    /** Enables or disables the inference system as a whole. */
    val enabled: Boolean,

    /** Enables smart guessing of cast vs. call expressions in the C/C++ language frontend. */
    val guessCastExpressions: Boolean,

    /** Enables the inference of record declarations. */
    val inferRecords: Boolean,

    /** Enables the inference of function declarations. */
    val inferFunctions: Boolean,

    /** Enables the inference of variables, such as global variables. */
    val inferVariables: Boolean,

    /**
     * Uses heuristics to add DFG edges for call expressions to unresolved functions (i.e.,
     * functions not implemented in the given source code).
     */
    val inferDfgForUnresolvedSymbols: Boolean
) {
    class Builder(
        private var enabled: Boolean = true,
        private var guessCastExpressions: Boolean = true,
        private var inferRecords: Boolean = true,
        private var inferFunctions: Boolean = true,
        private var inferVariables: Boolean = true,
        private var inferDfgForUnresolvedCalls: Boolean = true
    ) {
        fun guessCastExpressions(guess: Boolean) = apply { this.guessCastExpressions = guess }

        fun enabled(infer: Boolean) = apply { this.enabled = infer }

        fun inferRecords(infer: Boolean) = apply { this.inferRecords = infer }

        fun inferFunctions(infer: Boolean) = apply { this.inferFunctions = infer }

        fun inferVariables(infer: Boolean) = apply { this.inferVariables = infer }

        fun inferDfgForUnresolvedCalls(infer: Boolean) = apply {
            this.inferDfgForUnresolvedCalls = infer
        }

        fun build() =
            InferenceConfiguration(
                enabled,
                guessCastExpressions,
                inferRecords,
                inferFunctions,
                inferVariables,
                inferDfgForUnresolvedCalls
            )
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}
