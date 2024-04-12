/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes.executionConfiguration

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.passes.Pass
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import org.apache.commons.lang3.builder.ToStringBuilder

/** A simple helper class to match a pass with dependencies. */
data class PassWithDependencies(
    val pass: KClass<out Pass<*>>,
    val softDependencies: MutableSet<KClass<out Pass<*>>>,
    val hardDependencies: MutableSet<KClass<out Pass<*>>>
) {
    val dependencies: Set<KClass<out Pass<*>>>
        get() {
            return softDependencies + hardDependencies
        }

    val isFirstPass: Boolean
        get() {
            return pass.hasAnnotation<ExecuteFirst>()
        }

    val isLastPass: Boolean
        get() {
            return pass.hasAnnotation<ExecuteLast>()
        }

    override fun toString(): String {
        val builder = ToStringBuilder(this, Node.TO_STRING_STYLE).append("pass", pass.simpleName)

        if (softDependencies.isNotEmpty()) {
            builder.append("softDependencies", softDependencies.map { it.simpleName })
        }

        if (hardDependencies.isNotEmpty()) {
            builder.append("hardDependencies", hardDependencies.map { it.simpleName })
        }
        return builder.toString()
    }
}
