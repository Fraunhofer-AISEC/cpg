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
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.Component
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.collections.ArrayList
import kotlin.io.path.absolutePathString

typealias ComponentName = String

private const val DEFAULT_COMPONENT = "application"

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
    /**
     * A cached list of symbols for each combination of a component and file specified in the
     * compilation database
     */
    private val symbols = mutableMapOf<Pair<ComponentName, File>, MutableMap<String, String>>()

    /**
     * A cached list of components and their files. Can be used to supply
     * [TranslationConfiguration.softwareComponents] with the necessary files to parse for each
     * component.
     */
    var components = mutableMapOf<ComponentName, MutableList<File>>()

    val sourceFiles: List<File>
        get() {
            return includePaths.keys.toList()
        }

    fun addIncludePath(srcFile: File, paths: List<String>) {
        includePaths[srcFile] = paths
    }

    /** Returns the include paths for the specified file. */
    fun getIncludePaths(file: File): List<String>? {
        return includePaths[file]
    }

    /** Returns the include paths for all files in compilation database. */
    val allIncludePaths: List<String>
        get() {
            return includePaths.values.flatten()
        }

    /** Returns defined symbols for the specified file and component. */
    fun getSymbols(name: ComponentName, file: File): Map<String, String>? {
        return symbols[Pair(name, file)]
    }

    /** Returns all defined symbols for the specified component. */
    fun getAllSymbols(name: ComponentName): MutableMap<String, String> {
        return mutableMapOf<String, String>().apply {
            for (innerMap in symbols) {
                if (innerMap.key.first == name) {
                    putAll(innerMap.value)
                }
            }
        }
    }

    /** This is the structure of how each object inside compile_commands.json looks like. */
    data class CompilationDatabaseEntry(
        val directory: String?,
        val command: String? = null,
        val arguments: List<String>? = null,
        val file: String,
        val output: String?,
    )

    /**
     * This represents a parsed [CompilationDatabaseEntry] with all necessary information extracted.
     */
    data class ParsedCompilationDatabaseEntry(
        val includes: MutableList<String> = mutableListOf(),
        var component: String = DEFAULT_COMPONENT, // Default to the default component name
        var arch: String? = null,
    )

    companion object {
        @JvmStatic
        /**
         * This function returns a [CompilationDatabase] from the specified file. OOptionally, if
         * [filterComponents] is not-null, it can be used to filter specific components from the
         * compilation database. This is useful if you want to ignore tests or focus on a main
         * library component.
         */
        fun fromFile(file: File, filterComponents: List<String>? = null): CompilationDatabase {
            val jsonStringFile = file.readText()
            val mapper = ObjectMapper().registerKotlinModule()
            val db = mapper.readValue<CompilationDatabase>(jsonStringFile)

            for (entry in db) {
                val fileNameInTheObject = entry.file

                val parsedEntry =
                    if (entry.arguments != null) {
                        parseCommandLineArgs(entry.arguments)
                    } else if (entry.command != null) {
                        parseCommandLineArgs(splitCommand(entry.command))
                    } else {
                        ParsedCompilationDatabaseEntry()
                    }

                // If we did not detect a component yet, for example since we are not using CMake,
                // we
                // can try another heuristic, if the "directory" is set. In this case the
                // compilation
                // database could be generated by bear and the project is most likely following a
                // structure like this:
                // - src/libabc
                // - src/libxyz
                // - src/tool
                if (entry.directory != null && parsedEntry.component == DEFAULT_COMPONENT) {
                    parsedEntry.component = parseComponentFromDirectory(entry.directory)
                }

                val basedir = entry.directory
                val srcFile = File(resolveRelativePath(fileNameInTheObject, basedir))

                if (srcFile.exists()) {
                    db.addIncludePath(
                        srcFile,
                        parsedEntry.includes.map { resolveRelativePath(it, basedir) },
                    )
                }

                val pair = Pair(parsedEntry.component, srcFile)

                db.symbols[pair] =
                    (if (entry.arguments != null) {
                        parseSymbols(entry.arguments)
                    } else if (entry.command != null) {
                        parseSymbols(splitCommand(entry.command))
                    } else {
                        mutableMapOf()
                    })
                db.components.getOrPut(parsedEntry.component) { mutableListOf() } += srcFile

                // Add arch as symbol
                if (parsedEntry.arch != null) {
                    val map = db.symbols[pair]
                    map?.put("__${parsedEntry.arch}__", "")
                }
            }

            // Just filter at the end
            db.components =
                db.components
                    .filter { filterComponents == null || it.key in filterComponents }
                    .toMutableMap()

            return db
        }

        private fun parseComponentFromDirectory(directory: String): String {
            val parts = directory.split("/src/")
            if (parts.size == 2) {
                return parts[1]
            } else {
                return DEFAULT_COMPONENT
            }
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

        /** Try to convert relative path to absolute path by using basedir as root */
        private fun resolveRelativePath(path: String, basedir: String?): String {
            if (
                !File(path).isAbsolute &&
                    basedir != null &&
                    Paths.get(basedir, path).toFile().exists()
            ) {
                return Paths.get(basedir, path).absolutePathString()
            }
            return path
        }

        /**
         * Gets the include directories of the array value provided. Example for a compile command
         * is:
         * ['clang', '-Iinc', '-I', 'include', '-isystem', 'sysroot', 'main.c', '-o', 'main.c.o']
         * This method returns the include-paths in the above command.
         */
        private fun parseCommandLineArgs(command: List<String>): ParsedCompilationDatabaseEntry {
            val entry = ParsedCompilationDatabaseEntry()

            // ['clang', 'main.c', '-o', 'main.c.o'],
            if (command.isEmpty()) {
                return entry
            }

            var i = 0
            while (i < command.size) {
                val word = command[i]
                when {
                    word.startsWith("-I") -> {
                        if (word.length == 2) {
                            if (i + 1 != command.size) {
                                // path is located at the next index
                                entry.includes.add(command[++i])
                            }
                        } else {
                            entry.includes.add(
                                word.substring(2)
                            ) // adds the directory excluding the -I field
                        }
                    }
                    word == "-isystem" -> {
                        if (i + 1 != command.size) {
                            entry.includes.add(command[++i])
                        }
                    }
                    word == "-isysroot" -> {
                        // Append usr/include to sysroot
                        if (i + 1 != command.size) {
                            val sysroot = command[++i]
                            entry.includes.add("$sysroot/usr/include")
                            // entry.includes.add("$sysroot/usr/include/c++/v1")
                        }
                    }
                    word == "-o" -> {
                        if (i + 1 != command.size) {
                            parseOutput(command[++i])?.let { entry.component = it }
                        }
                    }
                    word == "-arch" -> {
                        if (i + 1 != command.size) {
                            entry.arch = command[++i]
                        }
                    }
                }
                i++
            }

            return entry
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
        private fun parseSymbols(command: List<String>): MutableMap<String, String> {
            if (command.isEmpty()) {
                return mutableMapOf()
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
                } else if (word.startsWith("-std")) {
                    val std = word.split("=").lastOrNull()
                    val pair = parseStd(std)
                    if (pair != null) {
                        val (symbol, value) = pair
                        symbols[symbol] = value
                    }
                }
                i++
            }
            return symbols
        }

        private fun parseStd(std: String?): Pair<String, String>? {
            when (std) {
                "gnu++2b",
                "c++2b" -> {
                    return Pair("__cplusplus", "202101L")
                }
                "gnu++20",
                "c++20" -> {
                    return Pair("__cplusplus", "202002L")
                }
                "gnu++17",
                "c++17" -> {
                    return Pair("__cplusplus", "201703L")
                }
                "gnu++14",
                "c++14" -> {
                    return Pair("__cplusplus", "201402L")
                }
                "gnu++11",
                "c++11" -> {
                    return Pair("__cplusplus", "201103L")
                }
                "gnu++98",
                "c++98" -> {
                    return Pair("__cplusplus", "199711L")
                }
                "c23" -> {
                    return Pair("__STDC_VERSION__", "202311L")
                }
                "c2x" -> {
                    return Pair("__STDC_VERSION__", "202000L")
                }
                "c17" -> {
                    return Pair("__STDC_VERSION__", "201710L")
                }
                "c11" -> {
                    return Pair("__STDC_VERSION__", "201112L")
                }
                "c99" -> {
                    return Pair("__STDC_VERSION__", "199901L")
                }
                "c94" -> {
                    return Pair("__STDC_VERSION__", "199409L")
                }
            }

            return null
        }

        /**
         * Parses the -o flag and tries to build a name for a [Component].
         *
         * Common patterns include:
         * - CMakeFiles/testbinary.dir/test.c.o which should result in "testbinary"
         * - examples/c/CMakeFiles/c_example1.dir/example1.c.o which should result in "c_example1"
         * - lib/CMakeFiles/awesome.dir/file.c.o which should result in "libawesome"
         */
        private fun parseOutput(output: String): String? {
            var isLibrary = false

            // We need to have CMakeFiles in there, otherwise this will not work
            val cmakeIdx = output.indexOf("CMakeFiles/")
            if (cmakeIdx < 0) {
                // The compilation database could be generated by some other tool from a makefile,
                // e.g. bear. In this case we might need to look for different patterns
                return null
            }

            return parseCMakeNativeOutput(output, cmakeIdx, isLibrary)
        }

        private fun parseCMakeNativeOutput(
            output: String,
            cmakeIdx: Int,
            isLibrary: Boolean,
        ): String? {
            // If there is any prefix before it, analyze it for some patterns
            var isLibrary = isLibrary
            val prefix = output.substring(0, cmakeIdx)
            if (prefix == "lib/") {
                isLibrary = true
            }

            // Next, have a look for .dir
            val dirIdx = output.indexOf(".dir/")
            if (dirIdx < 0) {
                return null
            }

            // Component name is right in the middle of it
            var name = output.substring(cmakeIdx + "CMakeFiles/".length, dirIdx)
            if (isLibrary) {
                name = "lib${name}"
            }

            return name
        }
    }
}
