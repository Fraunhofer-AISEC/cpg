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

import java.nio.file.FileSystems
import jep.JepConfig
import jep.MainInterpreter
import jep.SharedInterpreter
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.exists

/**
 * Takes care of configuring Jep according to some well known paths on popular operating systems.
 */
object JepSingleton {
    init {
        // TODO: add proper logging
        val config = JepConfig()
        config.redirectStdErr(System.err)
        config.redirectStdout(System.out)

        // To understand how JEP is installed under the hood, check `installJep` task in
        // build.gradle.kts. But the main idea is that it is always copied to `build/jep` directory.
        val jepLocation = FileSystems.getDefault().getPath("build", "jep")
        // Based on the host OS we will determine the extension for the JEP binary
        val os = System.getProperty("os.name")
        val jepBinaryPath =
            jepLocation /
                ("libjep." +
                    when {
                        os.contains("Mac") -> "jnilib"
                        os.contains("Linux") -> "so"
                        os.contains("Windows") -> "dll"
                        else ->
                            throw IllegalStateException(
                                "Cannot setup JEP for this operating system: [$os]"
                            )
                    })
        if (jepBinaryPath.exists()) {
            // Jep's configuration must be set before the first instance is created. Later
            // calls to setJepLibraryPath and co result in failures.
            MainInterpreter.setJepLibraryPath(jepBinaryPath.absolutePathString())

            // also add include path so that Python can find jep in case of virtual environment
            // fixes: jep.JepException: <class 'ModuleNotFoundError'>: No module named 'jep'
            if ((jepLocation / "__init__.py").exists()) {
                config.addIncludePaths(jepLocation.parent.absolutePathString())
            }
        }

        SharedInterpreter.setConfig(config)
    }

    /** Setup and configure (load the Python code and trigger the debug script) an interpreter. */
    fun getInterp(): SharedInterpreter {
        return SharedInterpreter()
    }
}
