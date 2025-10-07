# Code Property Graph 
[![Actions Status](https://github.com/Fraunhofer-AISEC/cpg/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/Fraunhofer-AISEC/cpg/actions)
 [![codecov](https://codecov.io/gh/Fraunhofer-AISEC/cpg/graph/badge.svg?token=XBXZZOQIID)](https://codecov.io/gh/Fraunhofer-AISEC/cpg)

A simple library to extract a *code property graph* out of source code. It has support for multiple passes that can extend the analysis after the graph is constructed. It currently supports C/C++ (C17), Java (Java 13) and has experimental support for Golang, Python and TypeScript. Furthermore, it has support for the [LLVM IR](http://llvm.org/docs/LangRef.html) and thus, theoretically support for all languages that compile using LLVM. 

## What is this?

A code property graph (CPG) is a representation of source code in form of a labelled directed multi-graph. Think of it as directed a graph where each node and edge is assigned a (possibly empty) set of key-value pairs (_properties_). This representation is supported by a range of graph databases such as Neptune, Cosmos, Neo4j, Titan, and Apache Tinkergraph and can be used to store source code of a program in a searchable data structure. Thus, the code property graph allows to use existing graph query languages such as Cypher, NQL, SQL, or Gremlin in order to either manually navigate through interesting parts of the source code or to automatically find "interesting" patterns.

This library uses [Eclipse CDT](https://www.eclipse.org/cdt/) for parsing C/C++ source code [JavaParser](https://javaparser.org/) for parsing Java. In contrast to compiler AST generators, both are "forgiving" parsers that can cope with incomplete or even semantically incorrect source code. That makes it possible to analyze source code even without being able to compile it (due to missing dependencies or minor syntax errors). Furthermore, it uses [LLVM](https://llvm.org) through the [javacpp](https://github.com/bytedeco/javacpp) project to parse LLVM IR. Note that the LLVM IR parser is *not* forgiving, i.e., the LLVM IR code needs to be at least considered valid by LLVM. The necessary native libraries are shipped by the javacpp project for most platforms.

## Specifications

In order to improve some formal aspects of our library, we created several specifications of our core concepts. Currently, the following specifications exist:
* [Dataflow Graph](https://fraunhofer-aisec.github.io/cpg/CPG/specs/dfg/)
* [Evaluation Order Graph](https://fraunhofer-aisec.github.io/cpg/CPG/specs/eog/)
* [Graph Model in neo4j](https://fraunhofer-aisec.github.io/cpg/CPG/specs/graph/)
* [Language and Language Frontend](https://fraunhofer-aisec.github.io/cpg/CPG/impl/language/)

We aim to provide more specifications over time.

## Usage

To build the project from source, you have to generate a `gradle.properties` file locally.
This file also enables and disables the supported programming languages.
We provide a sample file [here](./gradle.properties.example) - simply copy it to `gradle.properties` in the directory of the cpg-project.
Instead of manually generating or editing the `gradle.properties` file, you can also use the `configure_frontends.sh` script, which edits the properties setting the supported programming languages for you.

### For Visualization Purposes

In order to get familiar with the graph itself, you can use the subproject [cpg-neo4j](./cpg-neo4j). It uses this library to generate the CPG for a set of user-provided code files. The graph is then persisted to a [Neo4j](https://neo4j.com/) graph database. The advantage this has for the user, is that Neo4j's visualization software [Neo4j Browser](https://neo4j.com/developer/neo4j-browser/) can be used to graphically look at the CPG nodes and edges, instead of their Java representations.

Please make sure, that the [APOC](https://neo4j.com/labs/apoc/) plugin is enabled on your neo4j server. It is used in mass-creating nodes and relationships.

For example using docker:
```
docker run -p 7474:7474 -p 7687:7687 -d -e NEO4J_AUTH=neo4j/password -e NEO4JLABS_PLUGINS='["apoc"]' neo4j:5
```

### As Library

The most recent version is being published to Maven central and can be used as a simple dependency, either using Maven or Gradle.

```kotlin
dependencies {
    val cpgVersion = "9.0.2"

    // use the 'cpg-core' module
    implementation("de.fraunhofer.aisec", "cpg-core", cpgVersion)

    // and then add the needed extra modules, such as Go and Python
    implementation("de.fraunhofer.aisec", "cpg-language-go", cpgVersion)
    implementation("de.fraunhofer.aisec", "cpg-language-python", cpgVersion)
}
```

There are some extra steps necessary for the `cpg-language-cxx` module. Since Eclipse CDT is not published on maven central, it is necessary to add a repository with a custom layout to find the released CDT files. For example, using Gradle's Kotlin syntax:
```kotlin
repositories {
    // This is only needed for the C++ language frontend
    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/")
        metadataSources {
            artifact()
        }

        patternLayout {
            artifact("[organisation].[module]_[revision].[ext]")
        }
    }
}
```

Beware, that the `cpg` module includes all optional features and might potentially be HUGE (especially because of the LLVM support). If you do not need LLVM, we suggest just using the `cpg-core` module with the needed extra modules like `cpg-language-go`. In the future we are working on extracting more optional modules into separate modules.

#### Development Builds

For all builds on the `main` branch, an artefact is published in the [GitHub Packages](https://github.com/orgs/Fraunhofer-AISEC/packages?repo_name=cpg) under the version `main-SNAPSHOT`. Additionally, selected PRs that have the `publish-to-github-packages` label will also be published there. This is useful if an important feature is not yet in main, but you want to test it. The version refers to the PR number, e.g. `1954-SNAPSHOT`.  

To use the GitHub Gradle Registry, please refer to https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package

### Configuration

The behavior of the library can be configured in several ways. Most of this is done through the `TranslationConfiguration`
and the `InferenceConfiguration`.

#### TranslationConfiguration

The `TranslationConfiguration` configures various aspects of the translation. E.g., it determines which languages/language
frontends and passes will be used, which information should be inferred, which files will be included, among others. The
configuration is set through a builder pattern.

#### InferenceConfiguration

The class `InferenceConfiguration` can be used to affect the behavior or the passes if they identify missing nodes.
Currently, there are three flags which can be enabled:

* `guessCastExpression` enables guessing if a CPP expression is a cast or a call expression if it is not clear.
* `inferRecords` enables the inference of missing record declarations (i.e., classes and structs)
* `inferDfgForUnresolvedSymbols` adds DFG edges to method calls represent all potential data flows if the called function
  is not present in the source code under analysis.

Only `inferDfgForUnresolvedSymbols` is turned on by default.

The configuration can be made through a builder pattern and is set in the `TranslationConfiguration` as follows:
```kt
val inferenceConfig = InferenceConfiguration
    .builder()
    .guessCastExpression(true)
    .inferRecords(true)
    .inferDfgForUnresolvedSymbols(true)
    .build()

val translationConfig = TranslationConfiguration
    .builder()
    .inferenceConfiguration(inferenceConfig)
    .build()
```

## Development
This section describes languages, how well they are supported, and how to use and develop them yourself.

### Language Support
Languages are maintained to different degrees, and are noted in the table below with:
- `maintained`: if they are mostly feature complete and bugs have priority of being fixed.
- `incubating`: if the language is currently being worked on to reach a state of feature completeness.
- `experimental`: if a first working prototype was implemented, e.g., to support research topics, and its future development is unclear.
- `discontinued`: if the language is no longer actively developed or maintained but is kept for everyone to fork and adapt.
  
The current state of languages is:

| Language                 | Module                                | Branch                                                                  | State          |
|--------------------------|---------------------------------------|-------------------------------------------------------------------------|----------------|
| Java (Source)            | cpg-language-java                     | [main](https://github.com/Fraunhofer-AISEC/cpg)                         | `maintained`   |
| C++                      | cpg-language-cxx                      | [main](https://github.com/Fraunhofer-AISEC/cpg)                         | `maintained`   |
| Python                   | cpg-language-python                   | [main](https://github.com/Fraunhofer-AISEC/cpg)                         | `maintained`   |
| Go                       | cpg-language-go                       | [main](https://github.com/Fraunhofer-AISEC/cpg)                         | `maintained`   |
| INI                      | cpg-language-ini                      | [main](https://github.com/Fraunhofer-AISEC/cpg)                         | `maintained`   |
| JVM (Bytecode)           | cpg-language-jvm                      | [main](https://github.com/Fraunhofer-AISEC/cpg)                         | `incubating`   |
| LLVM                     | cpg-language-llvm                     | [main](https://github.com/Fraunhofer-AISEC/cpg)                         | `incubating`   |
| TypeScript/JavaScript    | cpg-language-typescript               | [main](https://github.com/Fraunhofer-AISEC/cpg)                         | `experimental` |
| Ruby                     | cpg-language-ruby                     | [main](https://github.com/Fraunhofer-AISEC/cpg)                         | `experimental` |
| {OpenQASM,Python-Qiskit} | cpg-language-{openqasm,python-qiskit} | [quantum-cpg](https://github.com/Fraunhofer-AISEC/cpg/tree/quantum-cpg) | `experimental` |

Note that several languages can be compiled to LLVM IR and thus, can be analyzed using the `cpg-language-llvm` module (see [7]). This includes, but is not limited to, Rust, Swift, Objective-C, and Haskell (see https://llvm.org/ for more information). 

### Languages and Configuration
`cpg-core` contains the graph nodes, language-independent passes that add semantics to the cpg-AST. Languages are developed in separate gradle submodules. 
To include the desired language submodules, simply toggle them on in your local `gradle.properties` file by setting the properties to `true`, e.g., (`enableGoFrontend=true`).
We provide a sample file with all languages switched on [here](./gradle.properties.example).
Instead of manually editing the `gradle.properties` file, you can also use the `configure_frontends.sh` script, which edits the properties for you. Some languages need additional installation of software to run and will be listed below.

#### Golang

In the case of Golang, additional native code, [libgoast](https://github.com/Fraunhofer-AISEC/libgoast), is used to access the Go `ast` packages. Gradle should automatically download the latest version of this library during the build process. This currently only works for Linux and macOS.

#### Python

You need to install [jep](https://github.com/ninia/jep/). This can either be system-wide or in a virtual environment. Your jep version has to match the version used by the CPG (see [version catalog](./gradle/libs.versions.toml)).

Currently, only Python 3.{9,10,11,12,13} is supported.

##### System Wide

Follow the instructions at https://github.com/ninia/jep/wiki/Getting-Started#installing-jep.

##### Virtual Env

- `python3 -m venv ~/.virtualenvs/cpg`
- `source ~/.virtualenvs/cpg/bin/activate`
- `pip3 install jep`

Through the `JepSingleton`, the CPG library will look for well known paths on Linux and OS X. `JepSingleton` will prefer a virtualenv with the name `cpg`, this can be adjusted with the environment variable `CPG_PYTHON_VIRTUALENV`.

#### TypeScript

For parsing TypeScript, the necessary TypeScript-based code can be found in the `src/main/nodejs` directory of the `cpg-language-typescript` submodule. Gradle should build the script automatically. The bundles script will be placed inside the jar's resources and should work out of the box.

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

## Contributors

The following authors have contributed to this project:

<a href="https://github.com/Fraunhofer-AISEC/cpg/graphs/contributors"><img src="https://contrib.rocks/image?repo=Fraunhofer-AISEC/cpg" /></a>

## Contributing

Before accepting external contributions, you need to sign our [CLA](https://cla-assistant.io/Fraunhofer-AISEC/cpg). Our CLA assistent will check, whether you already signed the CLA when you open your first pull request.

## Further reading

You can find a complete list of papers [here](https://fraunhofer-aisec.github.io/cpg/#publications)

A quick write-up of our CPG has been published on arXiv:

[1] Konrad Weiss, Christian Banse. A Language-Independent Analysis Platform for Source Code. https://arxiv.org/abs/2203.08424

A preliminary version of this cpg has been used to analyze ARM binaries of iOS apps:

[2] Julian Schütte, Dennis Titze. _liOS: Lifting iOS Apps for Fun and Profit._ Proceedings of the ESORICS International Workshop on Secure Internet of Things (SIoT), Luxembourg, 2019. https://arxiv.org/abs/2003.12901

An initial publication on the concept of using code property graphs for static analysis:

[3] Yamaguchi et al. - Modeling and Discovering Vulnerabilities with Code Property Graphs. https://www.sec.cs.tu-bs.de/pubs/2014-ieeesp.pdf

[4] is an unrelated, yet similar project by the authors of the above publication, that is used by the open source software Joern [5] for analysing C/C++ code. While [4] is a specification and implementation of the data structure, this project here includes various _Language frontends_ (currently C/C++ and Java, Python to com) and allows creating custom graphs by configuring _Passes_ which extend the graph as necessary for a specific analysis:

[4] https://github.com/ShiftLeftSecurity/codepropertygraph

[5] https://github.com/ShiftLeftSecurity/joern/

Additional extensions of the CPG to support further use-cases:

[6] Christian Banse, Immanuel Kunz, Angelika Schneider and Konrad Weiss. Cloud Property Graph: Connecting Cloud Security Assessments with Static Code Analysis.  IEEE CLOUD 2021. https://doi.org/10.1109/CLOUD53861.2021.00014

[7] Alexander Küchler, Christian Banse. Representing LLVM-IR in a Code Property Graph. 25th Information Security Conference (ISC). Bali, Indonesia. 2022

[8] Maximilian Kaul, Alexander Küchler, Christian Banse. A Uniform Representation of Classical and Quantum Source Code for Static Code Analysis. IEEE International Conference on Quantum Computing and Engineering (QCE). Bellevue, WA, USA. 2023

