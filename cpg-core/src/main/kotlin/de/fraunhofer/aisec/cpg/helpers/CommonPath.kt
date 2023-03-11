/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.helpers

import java.io.File
import java.util.regex.Pattern

/** Find the common root path for a list of files */
object CommonPath {
    fun commonPath(paths: Collection<File>): File? {
        if (paths.isEmpty()) {
            return null
        }
        val longestPrefix = StringBuilder()
        val splitPaths =
            paths
                .map {
                    it.absolutePath
                        .split(Pattern.quote(File.separator).toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                }
                .sortedBy { it.size }
        val shortest = splitPaths[0]
        for (i in shortest.indices) {
            val part = shortest[i]
            if (splitPaths.all { it[i] == part }) {
                longestPrefix.append(part).append(File.separator)
            } else {
                break
            }
        }
        val result = File(longestPrefix.toString())
        return if (result.exists()) {
            getNearestDirectory(result)
        } else null
    }

    private fun getNearestDirectory(file: File): File {
        return if (file.isDirectory) {
            file
        } else {
            getNearestDirectory(file.parentFile)
        }
    }
}
