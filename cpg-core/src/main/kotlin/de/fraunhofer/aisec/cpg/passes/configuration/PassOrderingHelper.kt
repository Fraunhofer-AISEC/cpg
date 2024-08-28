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
 * A simple helper class for keeping track of passes and their (currently not satisfied)
 * dependencies during ordering.
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
    }

    /**
     * Add a pass to the internal [workingList], iff it does not exist.
     *
     * Also, add
     * * hard dependencies
     * * "execute before" dependencies
     *
     * Performs a [sanityCheck] after completion.
     */
    private fun addToWorkingList(newElement: KClass<out Pass<*>>) {
        if (workingList.filter { it.pass == newElement }.isNotEmpty()) {
            return
        }
        workingList.add(PassWithDependencies(newElement))

        // Take care of dependencies
        val dependsOnPasses = newElement.findAnnotations<DependsOn>()
        for (dep in dependsOnPasses) {
            if (!dep.softDependency) { // only hard dependencies
                addToWorkingList(dep.value)
            }
        }
        val executeBeforePasses =
            newElement.findAnnotations<ExecuteBefore>() // treated as hard dependencies
        for (dep in executeBeforePasses) {
            addToWorkingList(dep.other)
        }

        // finally, run a sanity check
        sanityCheck()
    }

    /**
     * This function reorders passes in order to meet their dependency requirements. soft
     * dependencies [DependsOn] with `softDependency == true`: all passes registered as soft
     * dependency will be executed before the current pass if they are registered
     * * hard dependencies [DependsOn] with `softDependency == false (default)`: all passes
     *   registered as hard dependency will be executed before the current pass (hard dependencies
     *   will be registered even if the user did not register them)
     * * first pass [ExecuteFirst]: a pass registered as first pass will be executed in the
     *   beginning
     * * last pass [ExecuteLast]: a pass registered as last pass will be executed at the end
     * * late pass [ExecuteLate]: a pass that is executed as late as possible without violating any
     *   of the other constraints
     *
     * This function uses a very simple (and inefficient) logic to meet the requirements above:
     * 1. A list of all registered passes and their dependencies is build
     *    [PassWithDepsContainer.workingList]
     * 1. All missing hard dependencies [DependsOn] are added to the
     *    [PassWithDepsContainer.workingList]
     * 1. The first pass [ExecuteFirst] is added to the result and removed from the other passes
     *    dependencies
     * 1. A list of passes in the workingList without dependencies are added to the result, and
     *    removed from the other passes dependencies
     * 1. The above step is repeated until all passes have been added to the result
     *
     * @return a sorted list of passes, with passes that can be run in parallel together in a nested
     *   list.
     */
    fun order(): List<List<KClass<out Pass<*>>>> {
        // translate "A `executeBefore` B" to "B `dependsOn` A"
        populateExecuteBeforeDependencies()

        val result = mutableListOf<List<KClass<out Pass<*>>>>()

        val firstPass = getAndRemoveFirstPasses()
        if (firstPass != null) {
            result.add(firstPass)
        }
        while (!workingList.isEmpty()) {
            val p = getAndRemoveFirstPassWithoutUnsatisfiedDependencies()
            if (p.isNotEmpty()) {
                result.add(p)
            } else {
                // failed to find a pass that can be added to the result -> deadlock :(
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
     * A pass annotated with [executeBefore] implies that the other pass depends on it. We populate
     * the [executeBeforeDependciesRemaining] field in the other pass to make the anaylsis simpler.
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
    private fun getAndRemoveFirstPassWithoutUnsatisfiedDependencies(): List<KClass<out Pass<*>>> {
        val results = mutableListOf<PassWithDependencies>()
        val it = workingList.listIterator()

        while (it.hasNext()) {
            val currentElement = it.next()
            if (results.isEmpty() && currentElement.isLastPass && workingList.size == 1) {
                it.remove()
                return listOf(currentElement.pass)
            }

            // Keep going until our dependencies are met, this will collect passes that can run in
            // parallel in results
            if (currentElement.dependenciesMet(workingList)) {
                // no unsatisfied dependencies
                results.add(currentElement)
            } else {
                continue
            }
        }

        val nonLatePassesAvailable =
            workingList
                .filter { !it.isLatePass && !it.isLastPass && !it.dependenciesMet(workingList) }
                .isNotEmpty()
        if (nonLatePassesAvailable) {
            results.removeAll { it.isLatePass }
        }

        // clean up the workingList
        results.forEach { workingList.remove(it) }

        // remove the selected passes from the other pass's dependencies
        results.forEach { removeDependencyByClass(it.pass) }

        return results.map { it.pass }
    }

    /**
     * Checks for passes marked as first pass by [ExecuteFirst]
     *
     * If found, this pass is returned and removed from the working list.
     *
     * @return The first pass if present. Otherwise, null.
     */
    private fun getAndRemoveFirstPasses(): List<KClass<out Pass<*>>>? {
        val firstPasses = workingList.filter { it.isFirstPass }

        return if (
            firstPasses.isNotEmpty()
        ) { // no need to worry about [ExecuteBefore] as there can only be one [ExecuteFirst] pass
            firstPasses.map { removeDependencyByClass(it.pass) }
            firstPasses.map { firstPass ->
                workingList.removeAll { removePass -> firstPass == removePass }
            }
            firstPasses.map { it.pass }
        } else {
            getAndRemoveFirstPassWithoutUnsatisfiedDependencies()
        }
    }

    /**
     * Perform a sanity check on the configured [workingList]. Currently, this only checks that
     * * there is at most one [ExecuteFirst] and
     * * at most one [ExecuteLast] pass configured and
     * * the first pass does not have a hard dependency.
     *
     * This does not check, whether the requested ordering can be satisfied.
     */
    private fun sanityCheck() {
        if (workingList.count { it.isFirstPass } > 1) {
            throw ConfigurationException("More than one pass registered as first pass.")
        }
        if (workingList.count { it.isLastPass } > 1) {
            throw ConfigurationException("More than one pass registered as last pass.")
        }
        workingList
            .filter { it.isFirstPass }
            .firstOrNull()
            ?.let { firstPass ->
                firstPass.hardDependenciesRemaining.isNotEmpty().let {
                    throw ConfigurationException("The first pass has a hard dependency.")
                }
            }
    }
}
