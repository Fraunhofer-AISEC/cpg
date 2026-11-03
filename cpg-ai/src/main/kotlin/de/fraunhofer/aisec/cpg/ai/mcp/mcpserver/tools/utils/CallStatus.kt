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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import de.fraunhofer.aisec.cpg.graph.expressions.Call

/** How well the analysis knows what a [Call] does. */
enum class CallStatus {
    /** At least one target has a body, i.e., is part of the analysed code. */
    RESOLVED,
    /**
     * The targets are known, but none of them has a body, e.g. functions that are only declared in
     * a header or a library, or that were inferred by the analysis.
     */
    EXTERNAL,
    /** No target could be determined at all. */
    UNRESOLVED,
}

/**
 * How well the analysis knows the targets of this call, see [CallStatus].
 *
 * A single target with a body is enough for [CallStatus.RESOLVED], since the targets of a virtual
 * call can include declarations without a body (e.g. a pure virtual method) next to its overrides.
 */
val Call.status: CallStatus
    get() =
        when {
            invokes.isEmpty() -> CallStatus.UNRESOLVED
            invokes.none { it.body != null } -> CallStatus.EXTERNAL
            else -> CallStatus.RESOLVED
        }
