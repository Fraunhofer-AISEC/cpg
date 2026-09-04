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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.expressions.Call

/**
 * Language-specific modelling of string-relevant calls (`str.format`, `StringBuilder.append`,
 * `strcat`, ...), consulted by [StringEvaluator.handleCall] before it falls back to the generic
 * predecessor-following behaviour.
 *
 * This is the extension point for Phase 3 of the design doc; no concrete handler is implemented
 * yet.
 */
interface StringOperationHandler {
    /**
     * Tries to model [call]. [evaluate] is a callback into the owning [StringEvaluator] so that a
     * handler can recursively evaluate an argument or the base of the call.
     *
     * Returns `null` if this handler does not know [call], so that the next handler in the list is
     * tried, and eventually the generic fallback.
     */
    fun handleCall(call: Call, evaluate: (Node) -> StringPattern): StringPattern?
}
