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
package de.fraunhofer.aisec.cpg.analysis.string

import de.fraunhofer.aisec.cpg.graph.AnalysisScope
import de.fraunhofer.aisec.cpg.graph.Interprocedural

/** Configures a [StringEvaluator]. See the design doc for the rationale behind the defaults. */
data class StringEvaluatorConfig(
    /** Interprocedural by default (D6). */
    val scope: AnalysisScope = Interprocedural(maxCallDepth = 10, maxSteps = 5_000),
    val maxTermSize: Int = DEFAULT_MAX_TERM_SIZE,
    val maxTermDepth: Int = DEFAULT_MAX_TERM_DEPTH,
    val maxUnionSize: Int = DEFAULT_MAX_UNION_SIZE,
    /** Follow calls into inferred functions? Off by default: nothing useful to be learned there. */
    val enterInferredFunctions: Boolean = false,
)
