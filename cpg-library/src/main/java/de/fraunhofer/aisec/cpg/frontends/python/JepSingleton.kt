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
import jep.MainInterpreter

/**
 * Takes care of configuring Jep according to some well known paths on popular operating systems.
 */
object JepSingleton {
    init {
        if (System.getenv("CPG_JEP_LIBRARY") != null) {
            val library = File(System.getenv("CPG_JEP_LIBRARY"))
            if (library.exists()) {
                MainInterpreter.setJepLibraryPath(library.path)
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
                }
            }
        }
    }
}
