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

import de.fraunhofer.aisec.cpg.ConfigurationException
import de.fraunhofer.aisec.cpg.passes.Pass
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations
import org.slf4j.LoggerFactory

/**
 * A simple helper class for keeping track of passes and their (initially not satisfied)
 * dependencies during the ordering process. The goal of this class is to provide ordered passes
 * when invoking the [order] function.
 * * soft dependencies ([DependsOn] with `softDependency == true`): all passes registered as soft
 *   dependency will be executed before the current pass, if they are registered
 * * hard dependencies ([DependsOn] with `softDependency == false (default)`): all passes registered
 *   as hard dependency will be executed before the current pass (hard dependencies will be
 *   registered even if the user did not specifically register them)
 * * first pass [ExecuteFirst]: a pass registered as first pass will be executed in the beginning
 * * last pass [ExecuteLast]: a pass registered as last pass will be executed at the end
 * * late pass [ExecuteLate]: a pass that is executed as late as possible without violating any of
 *   the other constraints
 *
 * This class works by computing an initial [workingList] consisting of yet to schedule [Pass]es in
 * form of [PassWithDependencies] elements (they keep track of unsatisfied dependencies).
 * 1. All passes or collected in the [workingList]
 *     1. Hard dependencies and [ExecuteBefore] dependencies are automatically added to the
 *        [workingList], too.
 *     2. A [sanityCheck] is performed.
 * 2. If there is a [ExecuteFirst] pass, then it is selected for the result.
 * 3. While the [workingList] contains more than one element ([ExecuteLast]):
 *     1. All passes with no unsatisfied dependencies AND no [ExecuteLast] AND no [ExecuteLate] are
 *        selected.
 *     2. If the above fails: All passes with no unsatisfied dependencies AND no [ExecuteLast] are
 *        selected.
 *     3. If the above also fails: -> exception.
 * 4. The [ExecuteLast] pass is chosen.
 *
 * Whenever a pass is selected: all other passes in the [workingList] are updated (this pass is
 * removed from their dependencies). This allows the passes to be chosen in the next round because
 * all dependencies are then met.
 */
class PassOrderingHelper {
    private val workingList: MutableList<PassWithDependencies> = ArrayList()

    companion object {
        private val log = LoggerFactory.getLogger(PassOrderingHelper::class.java)
    }

    /**
     * Collects the requested passes provided as [passes] and populates the internal [workingList]
     * consisting of pairs of passes and their dependencies. Also, this function adds all
     * `hardDependencies`
     */
    constructor(passes: List<KClass<out Pass<*>>>) {
        for (pass in passes) {
            addToWorkingList(pass)
        }

        // translate "A `executeBefore` B" to "B `dependsOn` A"
        populateExecuteBeforeDependencies()

        // finally, run a sanity check
        sanityCheck()
    }

    /**
     * Add a pass to the internal [workingList], iff it does not exist.
     *
     * Also, add
     * * hard dependencies
     * * [ExecuteBefore] dependencies
     */
    private fun addToWorkingList(newElement: KClass<out Pass<*>>) {
        if (workingList.filter { it.pass == newElement }.isNotEmpty()) {
            return
        }
        workingList.add(PassWithDependencies(newElement))

        // Take care of dependencies
        for (dep in newElement.findAnnotations<DependsOn>()) {
            if (!dep.softDependency) { // only hard dependencies
                addToWorkingList(dep.value)
            }
        }

        // take care of [ExecuteBefore] dependencies (treated similar to hard dependencies)
        for (dep in newElement.findAnnotations<ExecuteBefore>()) {
            addToWorkingList(dep.other)
        }
    }

    /**
     * Order the passes.
     *
     * @return a sorted list of passes, with passes that can be run in parallel together in a nested
     *   list.
     */
    fun order(): List<List<KClass<out Pass<*>>>> {
        val result = mutableListOf<List<KClass<out Pass<*>>>>()

        val firstPass = getAndRemoveFirstPasses()
        if (firstPass.isNotEmpty()) {
            result.add(firstPass)
        }
        while (
            workingList.size >= 2 || (workingList.size == 1 && !workingList.first().isLastPass)
        ) {
            val noLatePassesAllowed =
                getAndRemoveFirstPassWithoutUnsatisfiedDependencies(allowLatePasses = false)
            if (noLatePassesAllowed.isNotEmpty()) {
                result.add(noLatePassesAllowed.map { selectPass(it) })
            } else {
                val latePassesAllowed =
                    getAndRemoveFirstPassWithoutUnsatisfiedDependencies(allowLatePasses = true)
                if (latePassesAllowed.isNotEmpty()) {
                    result.add(latePassesAllowed.map { selectPass(it) })
                } else {
                    throw ConfigurationException("Failed to satisfy ordering requirements.")
                }
            }
        }

        // [ExecuteLast]
        if (workingList.size == 1) {
            if (workingList.first().isLastPass) {
                result.add(workingList.toList().map { selectPass(it) })
            } else {
                throw ConfigurationException("Failed to satisfy ordering requirements.")
            }
        }

        log.info(
            "Passes after enforcing order: {}",
            result.map { list -> list.map { it.simpleName } }
        )
        return result
    }

    /**
     * A pass annotated with [ExecuteBefore] implies that the other pass depends on it. We populate
     * the
     * [de.fraunhofer.aisec.cpg.passes.configuration.PassWithDependencies.executeBeforeDependenciesRemaining]
     * field in the other pass to make the analysis simpler.
     */
    private fun populateExecuteBeforeDependencies() {
        for (pass in workingList) { // iterate over entire workingList
            for (executeBeforePass in
                pass.executeBeforeRemaining) { // iterate over all executeBefore passes
                workingList
                    .map { it }
                    .filter { it.pass == executeBeforePass } // find the executeBeforePass
                    .forEach {
                        it.executeBeforeDependenciesRemaining += pass.pass
                    } // add the original pass to the dependency list
            }
        }
    }

    /**
     * Iterate through all elements and remove the provided dependency [cls] from all passes in the
     * working list.
     */
    private fun removeDependencyByClass(cls: KClass<out Pass<*>>) {
        for (pass in workingList) {
            pass.softDependenciesRemaining.remove(cls)
            pass.hardDependenciesRemaining.remove(cls)
            pass.executeBeforeDependenciesRemaining.remove(cls)
        }
    }

    override fun toString(): String {
        return workingList.toString()
    }

    /**
     * Finds the first pass that has all its dependencies satisfied. This pass is then removed from
     * the other passes dependencies and returned.
     *
     * This functions also honors the [ExecuteLate] annotation and only returns these, if there are
     * no more non-[ExecuteLate] available.
     *
     * @return The first pass that has no active dependencies on success. null otherwise.
     */
    private fun getAndRemoveFirstPassWithoutUnsatisfiedDependencies(
        allowLatePasses: Boolean
    ): List<PassWithDependencies> {
        return workingList.filter {
            it.dependenciesRemaining.isEmpty() && !it.isLastPass && it.isLatePass == allowLatePasses
        }
    }

    /**
     * Checks for passes marked as first pass by [ExecuteFirst]
     *
     * If found, this pass is returned and removed from the working list.
     *
     * @return The first pass if present. Otherwise, null.
     */
    private fun getAndRemoveFirstPasses(): List<KClass<out Pass<*>>> {
        return workingList.filter { it.isFirstPass }.map { selectPass(it) }
    }

    private fun selectPass(pass: PassWithDependencies): KClass<out Pass<*>> {
        // remove it from the other passes dependencies
        removeDependencyByClass(pass.pass)

        // remove it from the workingList
        workingList.remove(pass)

        // return the pass (not the [PassWithDependencies] container
        return pass.pass
    }

    /**
     * Perform a sanity check on the configured [workingList]. Currently, this only checks that
     * * there is at most one [ExecuteFirst] and
     * * at most one [ExecuteLast] pass configured and
     * * the first pass does not have a hard dependency and
     * * the last pass is not to be executed before other passes.
     *
     * This does not check, whether the requested ordering can be satisfied.
     */
    private fun sanityCheck() {
        if (workingList.count { it.isFirstPass } > 1) {
            throw ConfigurationException(
                "More than one pass registered as first pass: \"${workingList.filter { it.isFirstPass }.map { it.pass }}\"."
            )
        }
        if (workingList.count { it.isLastPass } > 1) {
            throw ConfigurationException(
                "More than one pass registered as last pass: \"${workingList.filter { it.isLastPass }.map { it.pass }}\"."
            )
        }
        workingList
            .filter { it.isFirstPass }
            .firstOrNull()
            ?.let { firstPass ->
                if (firstPass.hardDependenciesRemaining.isNotEmpty()) {
                    throw ConfigurationException(
                        "The first pass \"${firstPass.pass}\" has a hard dependency: \"${firstPass.hardDependenciesRemaining}\"."
                    )
                }
            }
        workingList
            .filter { it.isLastPass }
            .firstOrNull()
            ?.let { lastPass ->
                if (lastPass.executeBeforeRemaining.isNotEmpty()) {
                    throw ConfigurationException(
                        "The last pass \"${lastPass.pass}\" is supposed to be executed before another pass: \"${lastPass.executeBeforeRemaining}\"."
                    )
                }
            }
    }
}
