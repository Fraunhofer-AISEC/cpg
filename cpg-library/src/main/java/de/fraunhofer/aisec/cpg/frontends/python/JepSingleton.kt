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
import java.nio.file.Files.createTempDirectory
import java.nio.file.Files.createTempFile
import java.util.zip.ZipFile
import jep.JepConfig
import jep.MainInterpreter

/**
 * Takes care of configuring Jep according to some well known paths on popular operating systems.
 */
object JepSingleton {
    var config = JepConfig()

    init {
        val classLoader = javaClass
        val pyInitFile = classLoader.getResource("/CPGPython/__init__.py")

        config.redirectStdErr(System.err)
        config.redirectStdout(System.out)
        if (pyInitFile?.protocol == "file") {
            // we can point JEP to the folder and get better debug messages with python source code
            // locations

            // we want to have the parent folder of "CPGPython" so that we can do "import CPGPython"
            // in python
            var pyFolder = pyInitFile.file.dropLastWhile { it != File.separatorChar }
            pyFolder = pyFolder.dropLast(1)
            pyFolder = pyFolder.dropLastWhile { it != File.separatorChar }
            config.addIncludePaths(pyFolder)
        } else {
            // Extract files to a temp folder
            // TODO clean up after cpg finishes
            // TODO security: an attacker could easily change our code before we execute it :(

            val pyZipOnDisk = createTempFile("cpg_python", ".zip")
            val pyFolder = createTempDirectory("cpg_python")
            try {
                val pyZipFile = classLoader.getResourceAsStream("/CPGPythonSrc.zip")

                File(pyFolder.toString() + File.separatorChar + "CPGPython").mkdir()

                pyZipOnDisk.toFile().outputStream().use { pyZipFile.copyTo(it) }

                ZipFile(pyZipOnDisk.toFile()).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        zip.getInputStream(entry).use { input ->
                            val targetFile =
                                pyFolder.toString() +
                                    File.separatorChar +
                                    "CPGPython" +
                                    File.separatorChar +
                                    entry.name
                            File(targetFile).outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
                pyZipOnDisk.toFile().delete()
                config.addIncludePaths(pyFolder.toString())
            } catch (e: Exception) {
                pyZipOnDisk.toFile().delete()
                pyFolder.toFile().deleteRecursively()

                throw e
            }
        }

        if (System.getenv("CPG_JEP_LIBRARY") != null) {
            val library = File(System.getenv("CPG_JEP_LIBRARY"))
            if (library.exists()) {
                MainInterpreter.setJepLibraryPath(library.path)
                config.addIncludePaths(library.toPath().parent.parent.toString())
            }
        } else {
            var virtualEnv = "cpg"

            if (System.getenv("CPG_PYTHON_VIRTUALENV") != null) {
                virtualEnv = System.getenv("CPG_PYTHON_VIRTUALENV")
            }

            val wellKnownPaths =
                listOf(
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
                    config.addIncludePaths(it.toPath().parent.parent.toString())
                }
            }
        }
    }
}
