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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.passes.Pass.log
import java.lang.reflect.InvocationTargetException
import java.util.*

/**
 * A simple helper class for keeping track of passes and their (currently not satisfied)
 * dependencies during ordering.
 */
class PassOrderingWorkingList {
    private val workingList: MutableList<PassOrderingPassWithDependencies>

    init {
        workingList = ArrayList()
    }

    fun getWorkingList(): List<PassOrderingPassWithDependencies> {
        return workingList
    }

    fun addToWorkingList(newElement: PassOrderingPassWithDependencies) {
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
    fun removeDependencyByClass(cls: Class<out Pass>) {
        for ((_, value) in workingList) {
            value.remove(cls)
        }
    }

    override fun toString(): String {
        return workingList.toString()
    }

    fun getFirstPasses(): List<PassOrderingPassWithDependencies> {
        return workingList.filter { it.pass.isFirstPass }
    }

    fun getLastPasses(): List<PassOrderingPassWithDependencies> {
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
    fun addMissingDependencies() {
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

        // adding missing passes to the local working list
        while (missingPasses.isNotEmpty()) {
            val cls = missingPasses.removeAt(0)
            log.info(
                "Registering a required hard dependency which was not registered explicitly: {}",
                cls
            )
            var newPass: Pass =
                try {
                    cls.getConstructor().newInstance()
                } catch (e: InstantiationException) {
                    throw RuntimeException(e)
                } catch (e: InvocationTargetException) {
                    throw RuntimeException(e)
                } catch (e: IllegalAccessException) {
                    throw RuntimeException(e)
                } catch (e: NoSuchMethodException) {
                    throw RuntimeException(e)
                }

            val deps: MutableSet<Class<out Pass>> = HashSet()
            deps.addAll(newPass.hardDependencies)
            deps.addAll(newPass.softDependencies)
            workingList.add(PassOrderingPassWithDependencies(newPass, deps))

            // check the dependencies of the new pass
            for (dependency in newPass.hardDependencies) {
                var dependencyFound = dependencyPresent(dependency)
                if (!dependencyFound) {
                    for (innerElem in missingPasses) {
                        if (dependency == innerElem) {
                            dependencyFound = true
                            break
                        }
                    }
                }

                // it is really missing -> add it to missing passes
                if (!dependencyFound) {
                    missingPasses.add(dependency)
                }
            }
        }
    }
}
