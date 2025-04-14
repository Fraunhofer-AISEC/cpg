/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.concepts.logging

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import java.util.Objects

/** Indicates a logging level. */
enum class LogLevel {
    FATAL,
    CRITICAL,
    ERROR,
    WARN,
    INFO,
    DEBUG,
    TRACE,
    UNKNOWN,
}

/**
 * A log write operation e.g. `loggint.warn("...")`.
 *
 * @param underlyingNode The underlying CPG node.
 * @param concept The corresponding [Log] concept note, i.e. the log this node is writing to.
 * @param logLevel The corresponding [LogLevel] used in this write operation.
 * @param logArguments The underlying CPG nodes of the logging arguments, i.e. what is written to
 *   the log.
 */
class LogWrite(
    underlyingNode: Node? = null,
    override val concept: Log,
    val logLevel: LogLevel,
    val logArguments: List<Node>,
) : Operation(underlyingNode = underlyingNode, concept = concept), IsLogging {
    override fun equalWithoutUnderlying(other: OverlayNode): Boolean {
        return other is LogWrite &&
            super.equalWithoutUnderlying(other) &&
            other.logLevel == this.logLevel &&
            other.logArguments == this.logArguments
    }

    override fun hashCode() = Objects.hash(super.hashCode(), logLevel, logArguments)

    override fun setDFG() {
        this.nextDFG += concept
        this.prevDFG += logArguments
    }
}
