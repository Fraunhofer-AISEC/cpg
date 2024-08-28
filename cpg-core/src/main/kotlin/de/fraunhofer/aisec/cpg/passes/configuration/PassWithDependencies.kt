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
 * A simple helper class to match a pass with dependencies. [softDependenciesRemaining] and
 * [hardDependenciesRemaining] show the currently remaining / unsatisfied dependencies. These values
 * are updated during the ordering procedure.
 */
data class PassWithDependencies(
    val pass: KClass<out Pass<*>>,
    val softDependenciesRemaining: MutableSet<KClass<out Pass<*>>>,
    val hardDependenciesRemaining: MutableSet<KClass<out Pass<*>>>,
    val executeBeforeRemaining: MutableSet<KClass<out Pass<*>>>,
    val executeBeforeDependenciesRemaining: MutableSet<KClass<out Pass<*>>>
) {
    constructor(
        pass: KClass<out Pass<*>>
    ) : this(
        pass,
        mutableSetOf<KClass<out Pass<*>>>(),
        mutableSetOf<KClass<out Pass<*>>>(),
        mutableSetOf<KClass<out Pass<*>>>(),
        mutableSetOf<KClass<out Pass<*>>>()
    ) {
        for (d in pass.findAnnotations<DependsOn>()) {
            if (d.softDependency) {
                softDependenciesRemaining += d.value
            } else {
                hardDependenciesRemaining += d.value
            }
        }

        for (eb in pass.findAnnotations<ExecuteBefore>()) {
            executeBeforeRemaining += eb.other
        }
    }

    val dependenciesRemaining: Set<KClass<out Pass<*>>>
        get() {
            return softDependenciesRemaining +
                hardDependenciesRemaining +
                executeBeforeDependenciesRemaining
        }

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

    override fun toString(): String {
        val builder = ToStringBuilder(this, Node.TO_STRING_STYLE).append("pass", pass.simpleName)

        if (softDependenciesRemaining.isNotEmpty()) {
            builder.append("softDependencies", softDependenciesRemaining.map { it.simpleName })
        }

        if (hardDependenciesRemaining.isNotEmpty()) {
            builder.append("hardDependencies", hardDependenciesRemaining.map { it.simpleName })
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

    /**
     * Checks whether the [dependenciesRemaining] of this pass are met. The list of
     * [softDependenciesRemaining] and [hardDependenciesRemaining] is removed step-by-step in
     * [PassOrderingHelper.getAndRemoveFirstPassWithoutUnsatisfiedDependencies].
     */
    fun dependenciesMet(workingList: MutableList<PassWithDependencies>): Boolean {
        // In the simplest case all our dependencies are empty since they were already removed by
        // the selecting algorithm.
        if (this.dependenciesRemaining.isEmpty() && (!this.isLastPass || workingList.size == 1)) {
            return true
        }

        // We also need to check, whether we still "soft" depend on passes that are just not
        // there (after all hard dependencies are met), in this case we can select the pass
        // as well
        val remainingClasses = workingList.map { it.pass }
        if (
            this.hardDependenciesRemaining.isEmpty() &&
                this.executeBeforeDependenciesRemaining.isEmpty() &&
                this.softDependenciesRemaining.all { !remainingClasses.contains(it) } &&
                !this.isLastPass
        ) {
            return true
        }

        // Otherwise, we still depend on an unselected pass
        return false
    }
}
