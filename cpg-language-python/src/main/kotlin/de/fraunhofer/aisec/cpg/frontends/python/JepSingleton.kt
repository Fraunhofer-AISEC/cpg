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
import java.net.JarURLConnection
import jep.JepConfig
import jep.MainInterpreter
import org.slf4j.LoggerFactory

/**
 * Takes care of configuring Jep according to some well known paths on popular operating systems.
 */
object JepSingleton {
    var config = JepConfig()

    private val LOGGER = LoggerFactory.getLogger(javaClass)

    init {
        val tempFileHolder = PyTempFileHolder()
        val classLoader = javaClass
        val pyInitFile = classLoader.getResource("/CPGPython/__init__.py")

        config.redirectStdErr(System.err)
        config.redirectStdout(System.out)

        if (pyInitFile?.protocol == "file") {
            LOGGER.debug(
                "Found the CPGPython module using a \"file\" resource. Using python code directly."
            )
            // we can point JEP to the folder and get better debug messages with python source code
            // locations

            // we want to have the parent folder of "CPGPython" so that we can do "import CPGPython"
            // in python
            var pyFolder = pyInitFile.file.dropLastWhile { it != File.separatorChar }
            pyFolder = pyFolder.dropLast(1)
            pyFolder = pyFolder.dropLastWhile { it != File.separatorChar }
            config.addIncludePaths(pyFolder)
        } else {
            val targetFolder = tempFileHolder.pyFolder
            config.addIncludePaths(tempFileHolder.pyFolder.toString())

            // otherwise, we are probably running inside a JAR, so we try to extract our files
            // out of the jar into a temporary folder
            val jarURL = pyInitFile?.openConnection() as? JarURLConnection
            val jar = jarURL?.jarFile

            if (jar == null) {
                LOGGER.error(
                    "Could not extract CPGPython out of the jar. The python frontend will probably not work."
                )
            } else {
                LOGGER.info(
                    "Using JAR connection to {} to extract files into {}",
                    jar.name,
                    targetFolder
                )

                // we are only interested in the CPGPython directory
                val entries = jar.entries().asSequence().filter { it.name.contains("CPGPython") }

                entries.forEach { entry ->
                    LOGGER.debug("Extracting entry: {}", entry.name)

                    // resolve target files relatively to our target folder. They are already
                    // prefixed with CPGPython/
                    val targetFile = targetFolder.resolve(entry.name).toFile()

                    // make sure to create directories along the way
                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        // copy the contents into the temp folder
                        jar.getInputStream(entry).use { input ->
                            targetFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
            }
        }

        if (System.getenv("CPG_JEP_LIBRARY") != null) {
            val library = File(System.getenv("CPG_JEP_LIBRARY"))
            if (library.exists()) {
                MainInterpreter.setJepLibraryPath(library.path)
                config.addIncludePaths(
                    library.toPath().parent.parent.toString()
                ) // this assumes that the python code is also at the library's location
            }
        } else {
            var virtualEnv = "cpg"

            if (System.getenv("CPG_PYTHON_VIRTUALENV") != null) {
                virtualEnv = System.getenv("CPG_PYTHON_VIRTUALENV")
            }

            val wellKnownPaths =
                listOf(
                    File(
                        "${System.getProperty("user.home")}/.virtualenvs/${virtualEnv}/lib/python3.11/site-packages/jep/libjep.so"
                    ),
                    File(
                        "${System.getProperty("user.home")}/.virtualenvs/${virtualEnv}/lib/python3.11/site-packages/jep/libjep.jnilib"
                    ),
                    File(
                        "${System.getProperty("user.home")}/.virtualenvs/${virtualEnv}/lib/python3.10/site-packages/jep/libjep.so"
                    ),
                    File(
                        "${System.getProperty("user.home")}/.virtualenvs/${virtualEnv}/lib/python3.10/site-packages/jep/libjep.jnilib"
                    ),
                    File(
                        "${System.getProperty("user.home")}/.virtualenvs/${virtualEnv}/lib/python3.9/site-packages/jep/libjep.so"
                    ),
                    File(
                        "${System.getProperty("user.home")}/.virtualenvs/${virtualEnv}/lib/python3.9/site-packages/jep/libjep.jnilib"
                    ),
                    File("/usr/lib/libjep.so"),
                    File("/Library/Java/Extensions/libjep.jnilib")
                )

            wellKnownPaths.forEach {
                if (it.exists()) {
                    // Jep's configuration must be set before the first instance is created. Later
                    // calls
                    // to setJepLibraryPath and co result in failures.
                    MainInterpreter.setJepLibraryPath(it.path)
                    config.addIncludePaths(
                        it.toPath().parent.parent.toString()
                    ) // this assumes that the python code is also at the library's location
                }
            }
        }
    }
}
