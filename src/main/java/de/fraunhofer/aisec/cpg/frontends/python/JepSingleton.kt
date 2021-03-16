package de.fraunhofer.aisec.cpg.frontends.python

import jep.MainInterpreter

// Jep's configuration must be set before the first instance is created. Later calls to setJepLibraryPath and co result in failures.
object JepSingleton {
    init {
        // TODO
        MainInterpreter.setJepLibraryPath("/home/maximilian/.virtualenvs/jep/lib/python3.9/site-packages/jep/libjep.so")
        // val pyConfig = PyConfig().setPythonHome("/home/maximilian/.virtualenvs/jep/")
        // MainInterpreter.setInitParams(pyConfig)
    }
}