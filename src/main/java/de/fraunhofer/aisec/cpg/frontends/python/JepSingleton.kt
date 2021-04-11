package de.fraunhofer.aisec.cpg.frontends.python

import java.io.File
import jep.MainInterpreter

/**
 * Takes care of configuring Jep according to some well known paths on popular operating systems.
 */
object JepSingleton {
    init {
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
                // Jep's configuration must be set before the first instance is created. Later calls
                // to setJepLibraryPath and co result in failures.
                MainInterpreter.setJepLibraryPath(it.path)
            }
        }
    }
}
