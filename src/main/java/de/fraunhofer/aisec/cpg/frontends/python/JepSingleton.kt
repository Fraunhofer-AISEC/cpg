package de.fraunhofer.aisec.cpg.frontends.python

import jep.MainInterpreter

// Jep's configuration must be set before the first instance is created. Later calls to setJepLibraryPath and co result in failures.
object JepSingleton {
    init {
        // do not set it for CI
        // TODO: check different paths or get it via environment variable on non-CI
        //MainInterpreter.setJepLibraryPath(System.getProperty("user.home") + "/.virtualenvs/jep/lib/python3.9/site-packages/jep/libjep.so")
    }
}