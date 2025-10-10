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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

/**
 * This class holds configuration options for the inference of certain constructs and auto-guessing
 * when executing language frontends.
 */
@JsonDeserialize(builder = InferenceConfiguration.Builder::class)
class InferenceConfiguration
private constructor(
    /** Enables or disables the inference system as a whole. */
    val enabled: Boolean,

    /** Enables the inference of namespace declarations. */
    val inferNamespaces: Boolean,

    /** Enables the inference of record declarations. */
    val inferRecords: Boolean,

    /** Enables the inference of function declarations. */
    val inferFunctions: Boolean,

    /** Enables the inference of field declarations. */
    val inferFields: Boolean,

    /** Enables the inference of variables, such as global variables. */
    val inferVariables: Boolean,

    /**
     * A very EXPERIMENTAL feature. If this is enabled, we will try to infer return types of
     * functions based on the context of the call it originated out of. This is disabled by default.
     */
    val inferReturnTypes: Boolean,

    /**
     * Uses heuristics to add DFG edges for call expressions to unresolved functions (i.e.,
     * functions not implemented in the given source code).
     */
    val inferDfgForUnresolvedSymbols: Boolean,
) {
    class Builder(
        private var enabled: Boolean = true,
        private var inferNamespaces: Boolean = true,
        private var inferRecords: Boolean = true,
        private var inferFields: Boolean = true,
        private var inferFunctions: Boolean = true,
        private var inferVariables: Boolean = true,
        private var inferReturnTypes: Boolean = false,
        private var inferDfgForUnresolvedSymbols: Boolean = true,
    ) {
        fun enabled(infer: Boolean) = apply { this.enabled = infer }

        fun inferNamespaces(infer: Boolean) = apply { this.inferNamespaces = infer }

        fun inferRecords(infer: Boolean) = apply { this.inferRecords = infer }

        fun inferFunctions(infer: Boolean) = apply { this.inferFunctions = infer }

        fun inferFields(infer: Boolean) = apply { this.inferFields = infer }

        fun inferVariables(infer: Boolean) = apply { this.inferVariables = infer }

        fun inferReturnTypes(infer: Boolean) = apply { this.inferReturnTypes = infer }

        fun inferDfgForUnresolvedCalls(infer: Boolean) = apply {
            this.inferDfgForUnresolvedSymbols = infer
        }

        fun build() =
            InferenceConfiguration(
                enabled,
                inferNamespaces,
                inferRecords,
                inferFunctions,
                inferFields,
                inferVariables,
                inferReturnTypes,
                inferDfgForUnresolvedSymbols,
            )
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    override fun toString(): String {
        return ToStringBuilder(this, ToStringStyle.JSON_STYLE)
            .append("inferRecords", inferRecords)
            .append("inferDfgForUnresolvedSymbols", inferDfgForUnresolvedSymbols)
            .toString()
    }
}
