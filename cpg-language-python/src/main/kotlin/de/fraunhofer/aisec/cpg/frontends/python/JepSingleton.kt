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

import de.fraunhofer.aisec.cpg.frontends.TranslationException
import java.io.File
import java.net.JarURLConnection
import java.nio.file.Path
import jep.JepConfig
import jep.JepException
import jep.MainInterpreter
import jep.SubInterpreter
import org.slf4j.LoggerFactory

/**
 * Takes care of configuring Jep according to some well known paths on popular operating systems.
 */
object JepSingleton {
    private var config = JepConfig()
    private val classLoader = javaClass

    private val LOGGER = LoggerFactory.getLogger(javaClass)

    init {
        val tempFileHolder = PyTempFileHolder()
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

            val virtualEnvPath = "${System.getProperty("user.home")}/.virtualenvs/${virtualEnv}/"
            val pythonVersions = listOf("3.9", "3.10", "3.11", "3.12")
            val wellKnownPaths = mutableListOf<File>()
            pythonVersions.forEach { version ->
                wellKnownPaths.add(
                    File("${virtualEnvPath}/lib/python${version}/site-packages/jep/libjep.so")
                )
                wellKnownPaths.add(
                    File("${virtualEnvPath}/lib/python${version}/site-packages/jep/libjep.jnilib")
                )
            }
            wellKnownPaths.add(File("/usr/lib/libjep.so"))
            wellKnownPaths.add(File("/Library/Java/Extensions/libjep.jnilib"))

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

    /** Setup and configure (load the Python code and trigger the debug script) an interpreter. */
    fun getInterp(): SubInterpreter {
        val interp = SubInterpreter(config)
        var found = false
        // load the python code
        // check, if the cpg.py is either directly available in the current directory or in the
        // src/main/python folder
        val modulePath = Path.of("cpg.py")

        val possibleLocations =
            listOf(
                Path.of(".").resolve(modulePath),
                Path.of("src/main/python").resolve(modulePath),
                Path.of("cpg-library/src/main/python").resolve(modulePath)
            )

        var entryScript: Path? = null
        possibleLocations.forEach {
            if (it.toFile().exists()) {
                found = true
                entryScript = it.toAbsolutePath()
            }
        }

        try {

            val debugEgg = System.getenv("DEBUG_PYTHON_EGG")
            val debugHost = System.getenv("DEBUG_PYTHON_HOST") ?: "localhost"
            val debugPort = System.getenv("DEBUG_PYTHON_PORT") ?: 52190

            // load script
            if (found) {
                interp.runScript(entryScript.toString())
            } else {
                // fall back to the cpg.py in the class's resources
                interp.exec(classLoader.getResource("/cpg.py")?.readText())
            }

            if (debugEgg != null) {
                interp.invoke("enable_debugger", debugEgg, debugHost, debugPort)
            }
        } catch (e: JepException) {
            e.printStackTrace()
            throw TranslationException("Initializing Python failed with message: $e")
        } catch (e: Exception) {
            throw e
        }

        return interp
    }
}
