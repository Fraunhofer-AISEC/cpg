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
package de.fraunhofer.aisec.cpg.frontends

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.collections.ArrayList

/**
 * A compilation database contains necessary information about the include paths and possible
 * compiler flags that should be used for an individual source file. It follows the JSON Compilation
 * Database Format Specification (see https://clang.llvm.org/docs/JSONCompilationDatabase.html).
 *
 * It is basically a list of [CompilationDatabaseEntry] entries. For now, we are primarily
 * interested in the include paths, but in the future, we might extend this to other compiler flags.
 */
class CompilationDatabase : ArrayList<CompilationDatabase.CompilationDatabaseEntry>() {
    /** A cached list of include paths for each source file specified in the compilation database */
    private val includePaths = mutableMapOf<File, List<String>>()

    val sourceFiles: List<File>
        get() {
            return includePaths.keys.toList()
        }

    /** Returns the include paths for the specified file. */
    operator fun get(file: File): List<String>? {
        return includePaths[file]
    }

    /** This is the structure of how each object inside compile_commands.json looks like. */
    data class CompilationDatabaseEntry(
        val directory: String?,
        val command: String? = null,
        val arguments: List<String>? = null,
        val file: String,
        val output: String?
    )

    companion object {
        @JvmStatic
        /** This function returns a [CompilationDatabase] from the specified file. */
        fun fromFile(file: File): CompilationDatabase {
            val jsonStringFile = file.readText()
            val mapper = ObjectMapper().registerKotlinModule()
            val db = mapper.readValue<CompilationDatabase>(jsonStringFile)

            for (entry in db) {
                val fileNameInTheObject = entry.file

                val includes: List<String>? =
                    if (entry.arguments != null) {
                        parseIncludeDirectories(entry.arguments)
                    } else if (entry.command != null) {
                        parseIncludeDirectories(entry.command)
                    } else {
                        null
                    }
                val basedir = entry.directory
                val file = File(fileNameInTheObject)

                if (includes != null) {
                    if (file.isAbsolute) {
                        if (file.exists()) {
                            db.includePaths[file] = includes
                        }
                    } else {
                        if (basedir != null) {
                            if (Paths.get(basedir, fileNameInTheObject).toFile().exists()) {
                                db.includePaths[Paths.get(basedir, fileNameInTheObject).toFile()] =
                                    includes
                            }
                        }
                    }
                }
            }

            return db
        }
        /**
         * Gets the include directories the from the string value provided. Example for a compile
         * commdand is : "/usr/local/bin/g++-7 -I/Users/me/prj/Calendar/calendars -g -std=c++11 -o
         * calendar_run.dir/main.cpp.o -c /Users/me/prj/Calendar/main.cpp"
         *
         * This method returns the include-paths in the above the command.
         */
        private fun parseIncludeDirectories(command: String): List<String>? {
            if (command.isEmpty()) {
                return null
            }
            // get all the -I flag files
            val words = listOf(*command.split(" ").toTypedArray())
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
         * commdand is : ['clang', 'main.c', '-o', 'main.c.o'] This method returns the include-paths
         * in the above command.
         */
        private fun parseIncludeDirectories(command: List<String>): List<String>? {
            //     ['clang', 'main.c', '-o', 'main.c.o'],
            // The I vals come after -I
            if (command.isEmpty()) {
                return null
            }
            val includeFilesDirectories: MutableList<String> = LinkedList()
            for (i in command.indices) {
                if (command[i].startsWith("-I")) {
                    if (i + 1 != command.size) {
                        includeFilesDirectories.add(command[i + 1])
                    }
                }
            }

            return includeFilesDirectories
        }
    }
}
