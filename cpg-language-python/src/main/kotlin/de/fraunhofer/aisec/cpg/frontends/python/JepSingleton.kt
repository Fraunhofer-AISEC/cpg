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
package de.fraunhofer.aisec.cpg.frontends.python

import java.io.File
import java.lang.RuntimeException
import java.nio.file.Path
import java.nio.file.Paths
import jep.JepConfig
import jep.MainInterpreter
import jep.SharedInterpreter
import kotlin.io.path.exists
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Takes care of configuring Jep according to some well known paths on popular operating systems.
 */
object JepSingleton {
    val log: Logger = LoggerFactory.getLogger(JepSingleton::class.java)

    init {
        // TODO logging
        val config = JepConfig()

        config.redirectStdErr(System.err)
        config.redirectStdout(System.out)

        System.getenv("CPG_JEP_LIBRARY")?.let {
            val library = File(it)
            if (library.exists()) {
                MainInterpreter.setJepLibraryPath(library.path)
                config.addIncludePaths(library.path)
            } else {
                throw RuntimeException(
                    "CPG_JEP_LIBRARY environment variable defined as '${library}' but it does not exist."
                )
            }
        }

        val virtualEnvName = System.getenv("CPG_PYTHON_VIRTUALENV") ?: "cpg"
        val virtualEnvPath =
            Paths.get(System.getProperty("user.home"), ".virtualenvs", virtualEnvName)
        val pythonVersions = listOf("3.8", "3.9", "3.10", "3.11", "3.12", "3.13")
        val wellKnownPaths = mutableListOf<Path>()
        pythonVersions.forEach { version ->
            // Linux
            wellKnownPaths.add(
                Paths.get(
                    "$virtualEnvPath",
                    "lib",
                    "python${version}",
                    "site-packages",
                    "jep",
                    "libjep.so",
                )
            )
            // Mac OS
            wellKnownPaths.add(
                Paths.get(
                    "$virtualEnvPath",
                    "lib",
                    "python${version}",
                    "site-packages",
                    "jep",
                    "libjep.jnilib",
                )
            )
            wellKnownPaths.add(
                Paths.get(
                    "$virtualEnvPath",
                    "lib",
                    "python${version}",
                    "site-packages",
                    "jep",
                    "libjep.dll",
                )
            )
        }
        // try system-wide paths, too
        // TODO: is this still needed?
        wellKnownPaths.add(Paths.get("/", "usr", "lib", "libjep.so"))
        wellKnownPaths.add(Paths.get("/", "Library", "Java", "Extensions", "libjep.jnilib"))

        for (path in wellKnownPaths) {
            if (path.exists()) {
                // Jep's configuration must be set before the first instance is created. Later
                // calls to setJepLibraryPath and co result in failures.
                MainInterpreter.setJepLibraryPath(path.toString())

                log.info("Using Jep native library in {}", path.toString())

                // also add include path so that Python can find jep in case of virtual environment
                // fixes: jep.JepException: <class 'ModuleNotFoundError'>: No module named 'jep'
                if (
                    path.parent.fileName.toString() == "jep" &&
                        (Paths.get(path.parent.toString(), PythonLanguage.IDENTIFIER_INIT + ".py")
                            .exists())
                ) {
                    config.addIncludePaths(path.parent.parent.toString())
                }
                break
            }
        }

        SharedInterpreter.setConfig(config)
    }

    /** Setup and configure (load the Python code and trigger the debug script) an interpreter. */
    fun getInterp(): SharedInterpreter {
        return SharedInterpreter()
    }
}
