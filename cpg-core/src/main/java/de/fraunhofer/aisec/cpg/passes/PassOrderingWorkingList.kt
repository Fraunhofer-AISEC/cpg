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

import java.util.*

class PassOrderingWorkingList {
    private val workingList: MutableList<Map.Entry<Pass, MutableSet<Class<out Pass>>>>

    init {
        workingList = ArrayList()
    }

    fun getWorkingList(): List<Map.Entry<Pass, MutableSet<Class<out Pass>>>> {
        return workingList
    }

    fun addToWorkingList(newElement: AbstractMap.SimpleEntry<Pass, MutableSet<Class<out Pass>>>) {
        workingList.add(newElement)
    }

    val isEmpty: Boolean
        get() = workingList.isEmpty()

    fun size(): Int {
        return workingList.size
    }

    fun removeDependencyByClass(cls: Class<out Pass>) {
        for ((_, value) in workingList) {
            value.remove(cls)
        }
    }

    override fun toString(): String {
        return workingList.toString()
    }
}
