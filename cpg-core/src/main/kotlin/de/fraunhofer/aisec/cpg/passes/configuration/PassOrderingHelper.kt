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

/**
 * A simple helper class for keeping track of passes and their (currently not satisfied)
 * dependencies during ordering.
 */
class PassOrderingHelper {
    private val workingList: MutableList<PassWithDependencies> = ArrayList()

    fun addToWorkingList(newElement: KClass<out Pass<*>>) {
        if (workingList.filter { it.pass == newElement }.isNotEmpty()) {
            return
        }
        workingList.add(createNewPassWithDependency(newElement))
        sanityCheck()
    }

    val isEmpty: Boolean
        get() = workingList.isEmpty()

    fun size(): Int {
        return workingList.size
    }

    /**
     * Iterate through all elements and remove the provided dependency [cls] from all passes in the
     * working list.
     */
    private fun removeDependencyByClass(cls: KClass<out Pass<*>>) {
        for (pass in workingList) {
            pass.softDependenciesRemaining.remove(cls)
            pass.hardDependenciesRemaining.remove(cls)
        }
    }

    override fun toString(): String {
        return workingList.toString()
    }

    private fun dependencyPresent(dep: KClass<out Pass<*>>): Boolean {
        var result = false
        for (currentElement in workingList) {
            if (dep == currentElement.pass) {
                result = true
                break
            }
        }

        return result
    }

    private fun createNewPassWithDependency(cls: KClass<out Pass<*>>): PassWithDependencies {
        val softDependencies = mutableSetOf<KClass<out Pass<*>>>()
        val hardDependencies = mutableSetOf<KClass<out Pass<*>>>()

        val dependencies = cls.findAnnotations<DependsOn>()
        for (d in dependencies) {
            if (d.softDependency) {
                softDependencies += d.value
            } else {
                hardDependencies += d.value
            }
        }

        return PassWithDependencies(cls, softDependencies, hardDependencies)
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
    fun getAndRemoveFirstPassWithoutUnsatisfiedDependencies(): List<KClass<out Pass<*>>> {
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
    fun getAndRemoveFirstPass(): List<KClass<out Pass<*>>>? {
        val firstPasses = workingList.filter { it.isFirstPass }

        return if (firstPasses.isNotEmpty()) {
            firstPasses.map { removeDependencyByClass(it.pass) }
            firstPasses.map { it.pass }
        } else {
            getAndRemoveFirstPassWithoutUnsatisfiedDependencies()
        }
    }

    /**
     * Perform a sanity check on the configured [workingList]. Currently, this only checks that
     * there is at most one [ExecuteFirst] and at most one [ExecuteLast] pass configured. This does
     * not check, whether the requested ordering can be satisfied.
     */
    fun sanityCheck() {
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
