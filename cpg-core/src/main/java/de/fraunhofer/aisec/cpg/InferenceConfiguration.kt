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

import de.fraunhofer.aisec.cpg.frontends.cpp.CXXLanguageFrontend
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

/**
 * This class holds configuration options for the inference of certain constructs and auto-guessing
 * when executing language frontends.
 */
class InferenceConfiguration
private constructor(
    /** Enables smart guessing of cast vs. call expressions in the [CXXLanguageFrontend] */
    val guessCastExpressions: Boolean,

    /** Enables the inference of record declarations */
    val inferRecords: Boolean
) {
    class Builder(var guessCastExpressions: Boolean = false, var inferRecords: Boolean = false) {
        fun guessCastExpressions(guess: Boolean) = apply { this.guessCastExpressions = guess }
        fun inferRecords(infer: Boolean) = apply { this.inferRecords = infer }
        fun build() = InferenceConfiguration(guessCastExpressions, inferRecords)
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    override fun toString(): String {
        return ToStringBuilder(this, ToStringStyle.JSON_STYLE)
            .append("guessCastExpressions", guessCastExpressions)
            .append("inferRecords", inferRecords)
            .toString()
    }
}
