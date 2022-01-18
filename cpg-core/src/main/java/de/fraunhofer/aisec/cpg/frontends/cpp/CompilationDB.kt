/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cpp

import org.slf4j.LoggerFactory
import java.util.*

/** This class is used to extract the details inside a compilation database. */
object CompilationDB {
    private val log = LoggerFactory.getLogger(CompilationDB::class.java)

    /**
     * Gets the include directories the from the string value provided. Example for a compile
     * commdand is : "/usr/local/bin/g++-7 -I/Users/me/prj/Calendar/calendars -g -std=c++11 -o
     * calendar_run.dir/main.cpp.o -c /Users/me/prj/Calendar/main.cpp"
     *
     * This method returns the include-paths in the above the command.
     */
    fun parseIncludeDirectories(stringVal: String): List<String>? {
        if (stringVal == null || stringVal.isEmpty()) {
            return null
        }
        // get all the -I flag files
        val words = java.util.List.of(*stringVal.split(" ").toTypedArray())
        val includeFilesDirectories: MutableList<String> = LinkedList()
        for (word in words) {
            if (word.startsWith("-I")) {
                includeFilesDirectories.add(
                    word.substring(2)
                ) // adds the directory excluding the -I field
            }
        }
        return includeFilesDirectories
    }

    /**
     * Gets the include directories the from the array value provided. Example for a compile
     * commdand is : ['clang', 'main.c', '-o', 'main.c.o'] This method returns the include-paths in
     * the above command.
     */
    fun parseIncludeDirectories(commandVals: List<String>): List<String>? {
        //     ['clang', 'main.c', '-o', 'main.c.o'],
        // The I vals come after -I
        if (commandVals == null || commandVals.isEmpty()) {
            return null
        }
        val includeFilesDirectories: MutableList<String> = LinkedList()
        for (i in commandVals.indices) {
            if (commandVals[i] != null && commandVals[i]!!.startsWith("-I")) {
                if (i + 1 != commandVals.size) {
                    includeFilesDirectories.add(commandVals[i + 1])
                }
            }
        }
        return includeFilesDirectories
    }
}
