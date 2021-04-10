package de.fraunhofer.aisec.cpg.frontends.python

import jep.MainInterpreter
import java.io.File

/**
 * Takes care of configuring Jep according to some well known paths on popular operating systems.
 */
object JepSingleton {
    init {
        val wellKnownPaths = listOf(
            File("/usr/lib/libjep.so"),
            File("/Library/Java/Extensions/libjep.jnilib")
        )

        wellKnownPaths.forEach {
            if(it.exists()) {
                // Jep's configuration must be set before the first instance is created. Later calls to setJepLibraryPath and co result in failures.
                MainInterpreter.setJepLibraryPath(it.path)
            }
        }

    }
}