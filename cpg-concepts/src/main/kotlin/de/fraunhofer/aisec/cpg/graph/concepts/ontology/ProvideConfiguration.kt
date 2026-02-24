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
package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int

/**
 * Represents an operation to provide a [Configuration], e.g., in the form of a configuration file
 * (through a [ConfigurationSource]). When the configuration file is loaded, a [LoadConfiguration]
 * operation would be found in the code component (matching the configuration file's name in
 * [LoadConfiguration.fileExpression]) and the [ProvideConfiguration] operation would be found in
 * the configuration component. But also other sources of configuration could be represented by a
 * [ProvideConfiguration] operation, such as environment variables or command-line arguments. Note:
 * The [ProvideConfiguration] operation is part of the [ConfigurationSource.ops] and not of the
 * [Configuration.ops] as it's an operation of the source, not the target.
 */
public open class ProvideConfiguration(
    public val configurationSource: ConfigurationSource?,
    operatesOn: Configuration,
    underlyingNode: Node? = null,
) : ConfigurationOperation(operatesOn, underlyingNode) {
    override fun equals(other: Any?): Boolean =
        other is ProvideConfiguration &&
            super.equals(other) &&
            other.configurationSource == this.configurationSource

    override fun hashCode(): Int = Objects.hash(super.hashCode(), configurationSource)
}
