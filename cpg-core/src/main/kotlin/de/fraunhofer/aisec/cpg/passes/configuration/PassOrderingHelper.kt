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
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.Pass
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations
import org.slf4j.LoggerFactory

/**
 * The goal of this class is to provide ordered passes when invoking the [order] function.
 * * soft dependencies ([DependsOn] with `softDependency == true`): all passes registered as soft
 *   dependency will be executed before the current pass, if they are registered independently
 * * hard dependencies ([DependsOn] with `softDependency == false (default)`): all passes registered
 *   as hard dependency will be executed before the current pass (hard dependencies will be
 *   registered even if the user did not specifically register them)
 * * first pass [ExecuteFirst]: a pass registered as first pass will be executed in the beginning
 * * last pass [ExecuteLast]: a pass registered as last pass will be executed at the end
 * * late pass [ExecuteLate]: a pass that is executed as late as possible without violating any of
 *   the other constraints
 * * [ExecuteBefore] (with soft and hard dependencies): the pass is to be executed before the other
 *   pass (soft: if the other pass is also configured)
 *
 * This class works by
 * 1. Setup
 *     1. Iterating through the configured passes and registering them (plus all `hard == true`
 *        dependencies) as [PassWithDependencies] containers:
 *         1. [ExecuteFirst] and [ExecuteLast] passes are stored in separate lists to keep the
 *            ordering logic simple.
 *         2. Normal passes are stored in the [workingList]
 *     2. [ExecuteBefore] passes: "`A` execute before `B`" implies that "`B` depends on `A`" -> the
 *        dependency is stored in the [PassWithDependencies.dependenciesRemaining] of "`B`". This
 *        logic is implemented in [populateExecuteBeforeDependencies].
 *     3. [DependsOn] passes: [PassWithDependencies.dependenciesRemaining] keeps track of these
 *        dependencies. [populateNormalDependencies] implements this logic.
 *     4. A [sanityCheck] is performed.
 * 2. Ordering
 *     1. [firstPassesList] passes are moved to the result.
 *     2. While the [workingList] is not empty:
 *         1. All passes ready to be scheduled are moved to the result excluding late passes. If
 *            this found at least one pass, then the loop starts again.
 *         2. Late passes are considered for scheduling as well. If this found at least one pass,
 *            then the loop starts again. Otherwise, the dependencies cannot be satisfied.
 *     3. [lastPassesList] passes are moved to the result.
 *
 * Note: whenever a pass is moved to the result:
 * - it is removed from the [workingList] (and [firstPassesList] / [lastPassesList])
 * - the other pass's [PassWithDependencies.dependenciesRemaining] are updated.
 */
class PassOrderingHelper {
    /** This list stores all non-{first,last} passes which have not yet been ordered. */
    private val workingList: MutableList<PassWithDependencies> = ArrayList()

    /** This list stores all first passes. Stored separately to keep the sorting logic simpler. */
    private val firstPassesList: MutableList<PassWithDependencies> = ArrayList()

    /** This list stores all last passes. Stored separately to keep the sorting logic simpler. */
    private val lastPassesList: MutableList<PassWithDependencies> = ArrayList()

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

        // clean up soft dependencies which are not registered in the workingList
        populateNormalDependencies()

        // finally, run a sanity check
        sanityCheck()
    }

    /** Register all (soft and hard) dependencies. */
    private fun populateNormalDependencies() {
        for (pass in workingList) {
            pass.passClass.hardDependencies.forEach { pass.dependenciesRemaining += it }
            pass.passClass.softDependencies
                .filter { workingList.map { it.passClass }.contains(it) }
                .forEach { pass.dependenciesRemaining += it }
        }
    }

    /**
     * Add a pass to the internal [workingList], iff it does not exist.
     *
     * Also, add
     * * hard dependencies
     * * [ExecuteBefore] dependencies
     */
    private fun addToWorkingList(newElement: KClass<out Pass<*>>) {
        if (
            (workingList + firstPassesList + lastPassesList)
                .filter { it.passClass == newElement }
                .isNotEmpty()
        ) {
            // we already know about this pass
            return
        }

        var firstOrLastPass = false
        if (newElement.findAnnotations<ExecuteFirst>().isNotEmpty()) {
            firstOrLastPass = true
            firstPassesList.add(PassWithDependencies(newElement, mutableSetOf()))
        }
        if (newElement.findAnnotations<ExecuteLast>().isNotEmpty()) {
            firstOrLastPass = true
            lastPassesList.add(PassWithDependencies(newElement, mutableSetOf()))
        }
        if (!firstOrLastPass) {
            workingList.add(PassWithDependencies(newElement, mutableSetOf()))
        }

        // take care of hard dependencies
        for (dep in newElement.findAnnotations<DependsOn>()) {
            if (!dep.softDependency) { // only hard dependencies
                addToWorkingList(dep.value)
            }
        }

        // take care of [ExecuteBefore] dependencies
        for (dep in newElement.findAnnotations<ExecuteBefore>()) {
            if (!dep.softDependency) {
                addToWorkingList(dep.other)
            }
        }
    }

    /**
     * Order the passes. This function honors
     * - [DependsOn] with soft and hard dependencies
     * - [ExecuteFirst]
     * - [ExecuteLast]
     * - [ExecuteLate]
     * - [ExecuteBefore] with soft and hard dependencies
     *
     * @return a sorted list of passes, with passes that can be run in parallel together in a nested
     *   list.
     * @throws [ConfigurationException] if the passes cannot be ordered.
     */
    fun order(): List<List<KClass<out Pass<*>>>> {
        val result = mutableListOf<List<KClass<out Pass<*>>>>()

        // [ExecuteFirst]
        getAndRemoveFirstPasses()?.let {
            result.add(listOf(it))
        } // there can only be one because of [sanityCheck]

        // "normal / middle" passes
        while (workingList.isNotEmpty()) {
            val noLatePassesAllowed = getAndRemoveNextPasses(allowLatePasses = false)
            if (noLatePassesAllowed.isNotEmpty()) {
                result.add(noLatePassesAllowed)
            } else {
                val latePassesAllowed = getAndRemoveNextPasses(allowLatePasses = true)
                if (latePassesAllowed.isNotEmpty()) {
                    result.add(latePassesAllowed)
                } else {
                    throw ConfigurationException("Failed to satisfy ordering requirements.")
                }
            }
        }

        // [ExecuteLast]
        lastPassesList.firstOrNull()?.let {
            result.add(listOf(selectPass(it)))
        } // there can only be one because of [sanityCheck]

        log.info(
            "Passes after enforcing order: {}",
            result.map { list -> list.map { it.simpleName } },
        )
        return result
    }

    /**
     * A pass annotated with [ExecuteBefore] implies that the other pass depends on it. We populate
     * the [de.fraunhofer.aisec.cpg.passes.configuration.PassWithDependencies.dependenciesRemaining]
     * field in the other pass to make the analysis simpler.
     */
    private fun populateExecuteBeforeDependencies() {
        for (pass in
            (workingList + firstPassesList + lastPassesList)) { // iterate over entire workingList
            for (executeBeforePass in
                (pass.passClass.softExecuteBefore +
                    pass.passClass.hardExecuteBefore)) { // iterate over all executeBefore passes
                (workingList + firstPassesList + lastPassesList)
                    .map { it }
                    .filter { it.passClass == executeBeforePass } // find the executeBeforePass
                    .forEach {
                        it.dependenciesRemaining += pass.passClass
                    } // add the original pass to the dependency list
            }
        }
    }

    /**
     * Iterate through all elements and remove the provided dependency [cls] from all passes in the
     * working lists.
     */
    private fun removeDependencyByClass(cls: KClass<out Pass<*>>) {
        for (pass in workingList) {
            pass.dependenciesRemaining.remove(cls)
        }
        for (pass in firstPassesList) {
            pass.dependenciesRemaining.remove(cls)
        }
        for (pass in lastPassesList) {
            pass.dependenciesRemaining.remove(cls)
        }
    }

    /**
     * Finds the first passes which have all their dependencies satisfied. These passes are then
     * returned.
     *
     * @return The first passes which have no active dependencies on success. An empty list
     *   otherwise.
     */
    private fun getAndRemoveNextPasses(allowLatePasses: Boolean): List<KClass<out Pass<*>>> {
        return workingList
            .filter {
                it.dependenciesRemaining.isEmpty() && it.passClass.isLatePass == allowLatePasses
            }
            .map { selectPass(it) }
    }

    /**
     * Checks for passes marked as first pass by [ExecuteFirst]
     *
     * If found, this pass is returned and removed from the working list.
     *
     * @return The first pass if present. Otherwise, null.
     */
    private fun getAndRemoveFirstPasses(): KClass<out Pass<*>>? {
        return when (firstPassesList.isEmpty()) {
            true -> null
            false -> selectPass(firstPassesList.first())
        }
    }

    /**
     * Removes a pass from the other passes dependencies and the workingList.
     *
     * @return the (unpacked) pass
     */
    private fun selectPass(pass: PassWithDependencies): KClass<out Pass<*>> {
        // remove it from the other passes dependencies
        removeDependencyByClass(pass.passClass)

        // remove it from the workingList
        workingList.remove(pass)
        firstPassesList.remove(pass)
        lastPassesList.remove(pass)

        // return the pass (not the [PassWithDependencies] container)
        return pass.passClass
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
        if (firstPassesList.size > 1) {
            throw ConfigurationException(
                "More than one pass registered as first pass: \"${firstPassesList.map { it.passClass }}\"."
            )
        }
        if (lastPassesList.size > 1) {
            throw ConfigurationException(
                "More than one pass registered as last pass: \"${lastPassesList.map { it.passClass }}\"."
            )
        }

        firstPassesList.map { firstPass ->
            if (firstPass.passClass.hardDependencies.isNotEmpty()) {
                throw ConfigurationException(
                    "The first pass \"${firstPass.passClass}\" has a hard dependency: \"${firstPass.passClass.hardDependencies}\"."
                )
            }
        }

        lastPassesList.map { lastPass ->
            if (lastPass.passClass.softExecuteBefore.isNotEmpty()) {
                throw ConfigurationException(
                    "The last pass \"${lastPass.passClass}\" is supposed to be executed before another pass: \"${lastPass.passClass.softExecuteBefore}\"."
                )
            }
            if (lastPass.passClass.hardExecuteBefore.isNotEmpty()) {
                throw ConfigurationException(
                    "The last pass \"${lastPass.passClass}\" is supposed to be executed before another pass: \"${lastPass.passClass.hardExecuteBefore}\"."
                )
            }
        }
    }
}
