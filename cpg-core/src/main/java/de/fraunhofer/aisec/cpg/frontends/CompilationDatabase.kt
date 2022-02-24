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
    /** A cached list of symbols for each source file specified in the compilation database */
    private val symbols = mutableMapOf<File, Map<String, String>>()

    val sourceFiles: List<File>
        get() {
            return includePaths.keys.toList()
        }

    /** Returns the include paths for the specified file. */
    fun getIncludePaths(file: File): List<String>? {
        return includePaths[file]
    }
    /** Returns defined symbols for the specified file. */
    fun getSymbols(file: File): Map<String, String>? {
        return symbols[file]
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
                        parseIncludeDirectories(splitCommand(entry.command))
                    } else {
                        null
                    }
                val basedir = entry.directory
                var srcFile = File(fileNameInTheObject)
                if (!srcFile.isAbsolute) {
                    if (basedir != null) {
                        if (Paths.get(basedir, fileNameInTheObject).toFile().exists()) {
                            srcFile = Paths.get(basedir, fileNameInTheObject).toFile()
                        }
                    }
                }
                if (includes != null) {
                    if (srcFile.exists()) {
                        db.includePaths[srcFile] = includes
                    }
                }
                db.symbols[srcFile] =
                    (if (entry.arguments != null) {
                        parseSymbols(entry.arguments)
                    } else if (entry.command != null) {
                        parseSymbols(splitCommand(entry.command))
                    } else {
                        mapOf()
                    })
            }

            return db
        }

        /**
         * Split a command into its separate arguments. The current implementation uses the naive
         * approach to split by " ". This will fail if escaping is used.
         *
         * TODO: Use escaping aware split
         */
        private fun splitCommand(command: String): List<String> {
            if (command.isEmpty()) {
                return listOf()
            }
            return listOf(*command.split(" ").toTypedArray())
        }

        /**
         * Gets the include directories of the array value provided. Example for a compile command
         * is: ['clang', '-Iinc', '-I', 'include', '-isystem', 'sysroot', 'main.c', '-o',
         * 'main.c.o'] This method returns the include-paths in the above command.
         */
        private fun parseIncludeDirectories(command: List<String>): List<String>? {
            //     ['clang', 'main.c', '-o', 'main.c.o'],
            if (command.isEmpty()) {
                return null
            }
            val includeFilesDirectories: MutableList<String> = LinkedList()
            var i = 0
            while (i < command.size) {
                val word = command[i]
                if (word.startsWith("-I")) {
                    if (word.length == 2) {
                        if (i + 1 != command.size) {
                            // path is located at the next index
                            includeFilesDirectories.add(command[++i])
                        }
                    } else {
                        includeFilesDirectories.add(
                            word.substring(2)
                        ) // adds the directory excluding the -I field
                    }
                } else if (word == "-isystem") {
                    if (i + 1 != command.size) {
                        includeFilesDirectories.add(command[++i])
                    }
                }
                i++
            }

            return includeFilesDirectories
        }

        /** Split the symbol into key and value. Value is optional. */
        private fun splitSymbol(sym: String): Pair<String, String> {
            if (sym.contains("=")) {
                val pair = sym.split("=", limit = 2)
                return Pair(pair[0], pair[1])
            }
            return Pair(sym, "")
        }

        /**
         * Gets the symbols (-D) from the array value provided. Example for a compile command is:
         * ['clang', '-DVERSION=1', '-D', 'DEBUG' 'main.c', '-o', 'main.c.o'] This method returns
         * the symbols as Map in the above command.
         */
        private fun parseSymbols(command: List<String>): Map<String, String> {
            if (command.isEmpty()) {
                return mapOf()
            }
            val symbols: LinkedHashMap<String, String> = LinkedHashMap()
            var i = 0
            while (i < command.size) {
                val word = command[i]
                if (word.startsWith("-D")) {
                    if (word.length == 2) {
                        if (i + 1 != command.size) {
                            // symbol is located at the next index
                            val sym = splitSymbol(command[++i])
                            symbols[sym.first] = sym.second
                        }
                    } else {
                        val sym =
                            splitSymbol(word.substring(2)) // adds the symbol excluding the -D field
                        symbols[sym.first] = sym.second
                    }
                }
                i++
            }
            return symbols
        }
    }
}
