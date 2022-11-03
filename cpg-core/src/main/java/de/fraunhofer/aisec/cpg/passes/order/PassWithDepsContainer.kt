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
package de.fraunhofer.aisec.cpg.passes.order

import de.fraunhofer.aisec.cpg.ConfigurationException
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log
import java.lang.reflect.InvocationTargetException
import java.util.*

/**
 * A simple helper class for keeping track of passes and their (currently not satisfied)
 * dependencies during ordering.
 */
class PassWithDepsContainer {
    private val workingList: MutableList<PassWithDependencies>

    init {
        workingList = ArrayList()
    }

    fun getWorkingList(): List<PassWithDependencies> {
        return workingList
    }

    fun addToWorkingList(newElement: PassWithDependencies) {
        workingList.add(newElement)
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
    private fun removeDependencyByClass(cls: Class<out Pass>) {
        for ((_, value) in workingList) {
            value.remove(cls)
        }
    }

    override fun toString(): String {
        return workingList.toString()
    }

    fun getFirstPasses(): List<PassWithDependencies> {
        return workingList.filter { it.pass.isFirstPass }
    }

    fun getLastPasses(): List<PassWithDependencies> {
        return workingList.filter { it.pass.isLastPass }
    }

    private fun dependencyPresent(dep: Class<out Pass>): Boolean {
        var result = false
        for (currentElement in workingList) {
            if (dep == currentElement.pass.javaClass) {
                result = true
                break
            }
        }

        return result
    }

    private fun createNewPassWithDependency(cls: Class<out Pass>): PassWithDependencies {
        val newPass =
            try {
                cls.getConstructor().newInstance()
            } catch (e: InstantiationException) {
                throw ConfigurationException(e)
            } catch (e: InvocationTargetException) {
                throw ConfigurationException(e)
            } catch (e: IllegalAccessException) {
                throw ConfigurationException(e)
            } catch (e: NoSuchMethodException) {
                throw ConfigurationException(e)
            }

        val deps: MutableSet<Class<out Pass>> = HashSet()
        deps.addAll(newPass.hardDependencies)
        deps.addAll(newPass.softDependencies)
        return PassWithDependencies(newPass, deps)
    }

    /**
     * Recursively iterates the workingList and adds all hard dependencies [DependsOn] and their
     * dependencies to the workingList.
     */
    fun addMissingDependencies() {
        val it = workingList.listIterator()
        while (it.hasNext()) {
            val current = it.next()
            for (dependency in current.pass.hardDependencies) {
                if (!dependencyPresent(dependency)) {
                    log.info(
                        "Registering a required hard dependency which was not registered explicitly: {}",
                        dependency
                    )
                    it.add(createNewPassWithDependency(dependency))
                }
            }
        }

        // add required dependencies to the working list
        val missingPasses: MutableList<Class<out Pass>> = ArrayList()

        // initially populate the missing dependencies list given the current passes
        for (currentElement in workingList) {
            for (dependency in currentElement.pass.hardDependencies) {
                if (!dependencyPresent(dependency)) {
                    missingPasses.add(dependency)
                }
            }
        }
    }

    /**
     * Finds the first pass that has all its dependencies satisfied. This pass is then removed from
     * the other passes dependencies and returned.
     *
     * @return The first pass that has no active dependencies on success. null otherwise.
     */
    fun getAndRemoveFirstPassWithoutDependencies(): Pass? {
        var result: Pass? = null

        for (currentElement in workingList) {
            if (workingList.size > 1 && currentElement.pass.isLastPass) {
                // last pass can only be added at the end
                continue
            }

            if (currentElement.dependencies.isEmpty()) { // no unsatisfied dependencies
                result = currentElement.pass

                // remove the pass from the other pass's dependencies
                removeDependencyByClass(result.javaClass)
                workingList.remove(currentElement)
                break
            }
        }
        return result
    }

    /**
     * Checks for passes marked as first pass by [ExecuteFirst]
     *
     * If found, this pass is returned and removed from the working list.
     *
     * @return The first pass if present. Otherwise, null.
     */
    fun getAndRemoveFirstPass(): Pass? {
        val firstPasses = getFirstPasses()
        if (firstPasses.size > 1) {
            throw ConfigurationException(
                "More than one pass requires to be run as first pass: {}".format(firstPasses)
            )
        }
        return if (firstPasses.isNotEmpty()) {
            val firstPass = firstPasses.first()
            if (firstPass.pass.hardDependencies.isNotEmpty()) {
                throw ConfigurationException("The first pass has a hard dependency.")
            } else {
                removeDependencyByClass(firstPass.pass.javaClass)
                workingList.remove(firstPass)
                firstPass.pass
            }
        } else {
            null
        }
    }
}
