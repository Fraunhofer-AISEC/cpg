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
package de.fraunhofer.aisec.cpg.passes.configuration

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.passes.Pass
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.hasAnnotation
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * A simple helper class to match a pass with its dependencies. [dependenciesRemaining] shows the
 * currently remaining / unsatisfied dependencies. These values are updated during the ordering
 * procedure.
 */
data class PassWithDependencies(
    /** the pass itself */
    val pass: KClass<out Pass<*>>,
    /** currently unsatisfied dependencies (soft / hard / [ExecuteBefore] from other passes) */
    val dependenciesRemaining: MutableSet<KClass<out Pass<*>>>
) {
    val isFirstPass: Boolean
        get() {
            return pass.hasAnnotation<ExecuteFirst>()
        }

    val isLastPass: Boolean
        get() {
            return pass.hasAnnotation<ExecuteLast>()
        }

    val isLatePass: Boolean
        get() {
            return pass.hasAnnotation<ExecuteLate>()
        }

    val softDependencies: Set<KClass<out Pass<*>>>
        get() {
            return pass
                .findAnnotations<DependsOn>()
                .filter { it.softDependency == true }
                .map { it.value }
                .toSet()
        }

    val hardDependencies: Set<KClass<out Pass<*>>>
        get() {
            return pass
                .findAnnotations<DependsOn>()
                .filter { it.softDependency == false }
                .map { it.value }
                .toSet()
        }

    val softExecuteBefore: Set<KClass<out Pass<*>>>
        get() {
            return pass
                .findAnnotations<ExecuteBefore>()
                .filter { it.soft == true }
                .map { it.other }
                .toSet()
        }

    val hardExecuteBefore: Set<KClass<out Pass<*>>>
        get() {
            return pass
                .findAnnotations<ExecuteBefore>()
                .filter { it.soft == false }
                .map { it.other }
                .toSet()
        }

    override fun toString(): String {
        val builder = ToStringBuilder(this, Node.TO_STRING_STYLE).append("pass", pass.simpleName)

        if (softDependencies.isNotEmpty()) {
            builder.append("soft dependencies:", softDependencies.map { it.simpleName })
        }

        if (hardDependencies.isNotEmpty()) {
            builder.append("hard dependencies:", hardDependencies.map { it.simpleName })
        }

        if (softExecuteBefore.isNotEmpty()) {
            builder.append("execute before (soft): ", softExecuteBefore.map { it.simpleName })
        }

        if (hardExecuteBefore.isNotEmpty()) {
            builder.append("execute before (hard): ", hardExecuteBefore.map { it.simpleName })
        }

        if (isFirstPass) {
            builder.append("firstPass")
        }

        if (isLastPass) {
            builder.append("lastPass")
        }

        if (isLatePass) {
            builder.append("latePass")
        }

        return builder.toString()
    }
}
