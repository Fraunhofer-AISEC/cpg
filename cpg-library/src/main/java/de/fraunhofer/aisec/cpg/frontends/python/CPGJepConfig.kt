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
import java.nio.file.Path
import java.util.zip.ZipFile
import jep.JepConfig
import jep.MainInterpreter

/**
 * Takes care of configuring Jep according to some well known paths on popular operating systems.
 */
class CPGJepConfig {
    var config = JepConfig()
    private var pyZipOnDisk: Path? = null
    private var pyFolder: Path? = null

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
            // TODO security: an attacker could easily change our code before we execute it :(

            try {
                // create temporary file and folder
                pyZipOnDisk = createTempFile("cpg_python", ".zip")

                if (pyZipOnDisk == null) {
                    throw Exception("Failed to create temp files.")
                }
                pyFolder = createTempDirectory("cpg_python")
                if (pyFolder == null) {
                    pyZipOnDisk?.toFile()?.delete()
                    throw Exception("Failed to create temp folder.")
                }

                // get the python src code resource
                val pyResourcesZip = classLoader.getResourceAsStream("/CPGPythonSrc.zip")

                File(pyFolder.toString() + File.separatorChar + "CPGPython").mkdir()

                pyZipOnDisk!!.toFile().outputStream().use { pyResourcesZip?.copyTo(it) }

                // Extract the zip file. Note: this expects the zip to only contain files and no
                // folders...
                ZipFile(pyZipOnDisk!!.toFile()).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        zip.getInputStream(entry).use { input ->
                            val targetFile =
                                pyFolder.toString() +
                                    File.separatorChar +
                                    "CPGPython" +
                                    File.separatorChar +
                                    entry
                                        .name // we have to store the files in a "CPGPython" folder,
                            // so that "import CPGPython" works in Python
                            File(targetFile).outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
                config.addIncludePaths(pyFolder.toString())
            } catch (e: Exception) {
                cleanTempFiles()

                throw e
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

    fun cleanTempFiles() {
        pyZipOnDisk?.toFile()?.delete()
        pyFolder?.toFile()?.deleteRecursively()
    }
}
