# Code Property Graph 
[![Actions Status](https://github.com/Fraunhofer-AISEC/cpg/workflows/build/badge.svg)](https://github.com/Fraunhofer-AISEC/cpg/actions)
 [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Fraunhofer-AISEC_cpg&metric=alert_status)](https://sonarcloud.io/dashboard?id=Fraunhofer-AISEC_cpg) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Fraunhofer-AISEC_cpg&metric=security_rating)](https://sonarcloud.io/dashboard?id=Fraunhofer-AISEC_cpg) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Fraunhofer-AISEC_cpg&metric=coverage)](https://sonarcloud.io/dashboard?id=Fraunhofer-AISEC_cpg) [![](https://jitpack.io/v/Fraunhofer-AISEC/cpg.svg)](https://jitpack.io/#Fraunhofer-AISEC/cpg)

A simple library to extract a *code property graph* out of source code. It has support for multiple passes that can extend the analysis after the graph is constructed. It currently supports C/C++ (C17), Java (Java 13) and has experimental support for Golang, Python and TypeScript. Furthermore, it has support for the [LLVM IR](http://llvm.org/docs/LangRef.html) and thus, theoretically support for all languages that compile using LLVM. 

## What is this?

A code property graph (CPG) is a representation of source code in form of a labelled directed multi-graph. Think of it as directed a graph where each node and edge is assigned a (possibly empty) set of key-value pairs (_properties_). This representation is supported by a range of graph databases such as Neptune, Cosmos, Neo4j, Titan, and Apache Tinkergraph and can be used to store source code of a program in a searchable data structure. Thus, the code property graph allows to use existing graph query languages such as Cypher, NQL, SQL, or Gremlin in order to either manually navigate through interesting parts of the source code or to automatically find "interesting" patterns.

This library uses [Eclipse CDT](https://www.eclipse.org/cdt/) for parsing C/C++ source code [JavaParser](https://javaparser.org/) for parsing Java. In contrast to compiler AST generators, both are "forgiving" parsers that can cope with incomplete or even semantically incorrect source code. That makes it possible to analyze source code even without being able to compile it (due to missing dependencies or minor syntax errors). Furthermore, it uses [LLVM](https://llvm.org) through the [javacpp](https://github.com/bytedeco/javacpp) project to parse LLVM IR. Note that the LLVM IR parser is *not* forgiving, i.e., the LLVM IR code needs to be at least considered valid by LLVM. The necessary native libraries are shipped by the javacpp project for most platforms.


## Usage

### For Visualization Purposes

In order to get familiar with the graph itself, you can use the subproject [cpg-neo4j](https://github.com/Fraunhofer-AISEC/cpg/tree/master/cpg-neo4j). It uses this library to generate the CPG for a set of user-provided code files. The graph is then persisted to a [Neo4j](https://neo4j.com/) graph database. The advantage this has for the user, is that Neo4j's visualization software [Neo4j Browser](https://neo4j.com/developer/neo4j-browser/) can be used to graphically look at the CPG nodes and edges, instead of their Java representations.

### As Library

The most recent version is being published to Maven central and can be used as a simple dependency, either using Maven or Gradle. Since Eclipse CDT is not published on maven central, it is necessary to add a repository with a custom layout to find the released CDT files. For example, using Gradle's Kotlin syntax:
```
repositories {
    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/10.3/cdt-10.3.2/plugins")
        metadataSources {
            artifact()
        }
        patternLayout {
            artifact("/[organisation].[module]_[revision].[ext]")
        }
    }
}

dependencies {
    var cpgVersion = "4.3.0" 
    
    // if you want to include all published cpg modules
    api("de.fraunhofer.aisec", "cpg", cpgVersion)
    
    // if you only want to include the core CPG without extra modules
    api("de.fraunhofer.aisec", "cpg-core", cpgVersion)
    
    // or just a particular extra module, such as LLVM or Python
    api("de.fraunhofer.aisec", "cpg-language-llvm", cpgVersion)
    api("de.fraunhofer.aisec", "cpg-language-python", cpgVersion)    
}
```

Beware, that the `cpg` module includes all optional features and might potentially be HUGE (especially because of the LLVM support). If you do not need LLVM, we suggest just using the `cpg-core` module. In the future we are working on extracting more optional modules into separate modules.

#### Development Builds

A published artifact of every commit can be requested through [JitPack](https://jitpack.io/#Fraunhofer-AISEC/cpg). This is especially useful, if your external project makes use of a specific feature that is not yet merged in yet or not published as a version yet. Please follow the instructions on the JitPack page. Please be aware, that similar to release builds, the CDT repository needs to be added as well (see above).

### On Command Line

The library can be used on the command line using the `cpg-console` subproject. Please refer to the [README.md](./cpg-console/README.md) of the `cpg-console` as well as our small [tutorial](./tutorial.md) for further details.

### Usage of Experimental Languages

Some languages, such as Golang are marked as experimental and depend on other native libraries. These are NOT YET bundled in the release jars (with exception of TypeScript), so you need to build them manually using the property `-Pexperimental` when using tasks such as `build` or `test`. For typescript, please use `-PexperimentalTypeScript`. Use the `cpg-language-python` module for Python support.

#### Golang

In the case of Golang, the necessary native code can be found in the `src/main/golang` folder. Gradle should automatically find JNI headers and stores the finished library in the `src/main/golang` folder. This currently only works for Linux and macOS. In order to use it in an external project, the resulting library needs to be placed somewhere in `java.library.path`. 

#### Python

You need to install [jep](https://github.com/ninia/jep/). This can either be system wide or in a virtual environment. Your jep version hast to match the version used by CPG (see [build.gradle.kts](./cpg-language-python/build.gradle.kts)).

Currently, only Python 3.10 is supported.

##### System Wide

Follow the instructions at https://github.com/ninia/jep/wiki/Getting-Started#installing-jep.

##### Virtual Env

- `python3 -m venv ~/.virtualenvs/cpg`
- `source ~/.virtualenvs/cpg/bin/activate`
- `pip3 install jep`

Through the `JepSingleton`, the CPG library will look for well known paths on Linux and OS X. `JepSingleton` will prefer a virtualenv with the name `cpg`, this can be adjusted with the environment variable `CPG_PYTHON_VIRTUALENV`.

#### TypeScript

For parsing TypeScript, the necessary NodeJS-based code can be found in the `src/main/nodejs` directory of the `cpg-library` folder. Gradle should build the script automatically, provided NodeJS (>=16) is installed. The bundles script will be placed inside the jar's resources and should work out of the box.

## Development Setup

### Code Style

We use [Google Java Style](https://github.com/google/google-java-format) as a formatting. Please install the appropriate plugin for your IDE, such as the [google-java-format IntelliJ plugin](https://plugins.jetbrains.com/plugin/8527-google-java-format) or [google-java-format Eclipse plugin](https://github.com/google/google-java-format/releases/download/google-java-format-1.6/google-java-format-eclipse-plugin_1.6.0.jar).

### Integration into IntelliJ

Straightforward, however three things are recommended

* Enable gradle "auto-import"
* Enable google-java-format
* Hook gradle spotlessApply into "before build" (might be obsolete with IDEA 2019.1)

### Git Hooks

You can use the hook in `style/pre-commit` to check for formatting errors:
```
cp style/pre-commit .git/hooks
```  

### How to build

This project requires Java 11. If Java 11 is not your default Java version, make sure to configure gradle to use it by setting its `java.home` variable:

```
./gradlew -Dorg.gradle.java.home="/usr/lib/jvm/java-11-openjdk-amd64/" build
```

## Contributors

The following authors have contributed to this project (in alphabetical order):
* [fwendland](https://github.com/fwendland)
* [JulianSchuette](https://github.com/JulianSchuette)
* [konradweiss](https://github.com/konradweiss)
* [KuechA](https://github.com/KuechA)
* [Masrepus](https://github.com/Masrepus)
* [maximiliankaul](https://github.com/maximiliankaul)
* [obraunsdorf](https://github.com/obraunsdorf)
* [oxisto](https://github.com/oxisto)
* [peckto](https://github.com/peckto)
* [titze](https://github.com/titze)
* [vfsrfs](https://github.com/vfsrfs)

## Further reading

A quick write-up of our CPG has been published on arXiv:

[1] Konrad Weiss, Christian Banse. A Language-Independent Analysis Platform for Source Code. https://arxiv.org/abs/2203.08424

A preliminary version of this cpg has been used to analyze ARM binaries of iOS apps:

[2] Julian Schütte, Dennis Titze. _liOS: Lifting iOS Apps for Fun and Profit._ Proceedings of the ESORICS International Workshop on Secure Internet of Things (SIoT), Luxembourg, 2019. https://arxiv.org/abs/2003.12901

An initial publication on the concept of using code property graphs for static analysis:

[3] Yamaguchi et al. - Modeling and Discovering Vulnerabilities with Code Property Graphs. https://www.sec.cs.tu-bs.de/pubs/2014-ieeesp.pdf

[4] is an unrelated, yet similar project by the authors of the above publication, that is used by the open source software Joern [5] for analysing C/C++ code. While [4] is a specification and implementation of the data structure, this project here includes various _Language frontends_ (currently C/C++ and Java, Python to com) and allows creating custom graphs by configuring _Passes_ which extend the graph as necessary for a specific analysis:

[4] https://github.com/ShiftLeftSecurity/codepropertygraph

[5] https://github.com/ShiftLeftSecurity/joern/

Additional extensions of the CPG into the field of Cloud security:

[6] Christian Banse, Immanuel Kunz, Angelika Schneider and Konrad Weiss. Cloud Property Graph: Connecting Cloud Security Assessments with Static Code Analysis.  IEEE CLOUD 2021. https://doi.org/10.1109/CLOUD53861.2021.00014
