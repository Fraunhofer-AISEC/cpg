# Code Property Graph 
[![Actions Status](https://github.com/Fraunhofer-AISEC/cpg/workflows/build/badge.svg)](https://github.com/Fraunhofer-AISEC/cpg/actions)
 [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Fraunhofer-AISEC_cpg&metric=alert_status)](https://sonarcloud.io/dashboard?id=Fraunhofer-AISEC_cpg) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Fraunhofer-AISEC_cpg&metric=security_rating)](https://sonarcloud.io/dashboard?id=Fraunhofer-AISEC_cpg) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Fraunhofer-AISEC_cpg&metric=coverage)](https://sonarcloud.io/dashboard?id=Fraunhofer-AISEC_cpg)

A simple library to extract a *code property graph* out of source code. It has support for multiple passes that can extend the analysis after the graph is constructed. It currently supports C/C++ (C17) and Java (Java 13).

## What is this?

A code property graph (CPG) is a representation of source code in form of a labelled directed multigraph. Think of it as directed a graph where each node and edge is assigned a (possibly empty) set of key-value pairs (_properties_). This representation is supported by a range of graph databases such as Neptune, Cosmos, Neo4j, Titan, and Apache Tinkergraph and can be used to store source code of a program in a searchable data structure. Thus, the code property graph allows to use existing graph query languages such as Cypher, NQL, SQL, or Gremlin in order to either manually navigate through interesting parts of the source code or to automatically find "interesting" patterns.

This library uses [Eclipse CDT](https://www.eclipse.org/cdt/) for parsing C/C++ source code and [JavaParser](https://javaparser.org/) for parsing Java. In contrast to compiler AST generators, both are "forgiving" parsers that can cope with incomplete or even syntactically incorrect source code. That makes it possible to analyze source code even without being able to compile it (due to missing dependencies or minor syntax errors). 


## Usage

### For Visualization Purposes

In order to get familiar with the graph itself, you can take a look at our companion project [cpg-vis-neo4j](https://github.com/Fraunhofer-AISEC/cpg-vis-neo4j). It uses this library to generate the CPG for a set of user-provided code files. The graph is then persisted to a [Neo4j](https://neo4j.com/) graph database. The advantage this has for the user, is that Neo4j's visualization software [Neo4j Browser](https://neo4j.com/developer/neo4j-browser/) can be used to graphically look at the CPG nodes and edges, instead of their Java representations.

### As Library

The most recent version is being published to Maven central and can be used as a simple dependency, either using Maven or Gradle. Since Eclipse CDT is not published on maven central, it is necessary to add a repository with a custom layout to find the released CDT files. For example, using Gradle's Kotlin syntax:
```
repositories {
    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/9.11/cdt-9.11.1/plugins")
        metadataSources {
            artifact()
        }
        patternLayout {
            artifact("/[organisation].[module]_[revision].[ext]")
        }
    }
}

dependencies {
    api("de.fraunhofer.aisec", "cpg", "2.1.1")
}
```

### On Command Line

The library can be used on the command line using `jshell`, the Java shell to try out some basic queries.

First, a jar consisting all the necessary dependencies should be created with `./gradlew shadowJar`. Afterwards, the shell can be launched using `jshell --class-path build/libs/cpg-*-all.jar`.

The following snippet creates a basic `TranslationManager` with default settings to analyze a sample file in `src/test/resources/openssl/client.cpp`:

```java
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration;

var path = Paths.get("src/test/resources/openssl/client.cpp");
var config = TranslationConfiguration.builder().sourceLocations(path.toFile()).defaultPasses().debugParser(true).build();
var analyzer = TranslationManager.builder().config(config).build();
var result = analyzer.analyze().get();
var tu = result.getTranslationUnits().get(0);
```

Afterwards, a list of function declarations can be obtained like this:

```java
var functions = tu.getDeclarations().stream().filter(decl -> decl instanceof FunctionDeclaration).map(FunctionDeclaration.class::cast).collect(Collectors.toList());
```

Information about specific functions can be obtained using the property getters:

```java
var func = functions.get(0);
func.getName();
func.getSignature();
func.getParameters();
```

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
* [Masrepus](https://github.com/Masrepus)
* [obraunsdorf](https://github.com/obraunsdorf)
* [oxisto](https://github.com/oxisto)
* [titze](https://github.com/titze)
* [vfsrfs](https://github.com/vfsrfs)

## Further reading

A preliminary version of this cpg has been used to analyze ARM binaries of iOS apps:

[1] Julian Schütte, Dennis Titze. _liOS: Lifting iOS Apps for Fun and Profit._ Proceedings of the ESORICS International Workshop on Secure Internet of Things (SIoT), Luxembourg, 2019


An initial publication on the concept of using code property graphs for static analysis:

[2] Yamaguchi et al. - Modeling and Discovering Vulnerabilities with Code Property Graphs https://www.sec.cs.tu-bs.de/pubs/2014-ieeesp.pdf


[3] is an unrelated, yet similar project by the authors of the above publication, that is used by the open source software Joern [4] for analysing C/C++ code. While [3] is a specification and implementation of the data structure, this project here includes various _Language frontends_ (currently C/C++ and Java, Python to com) and allows creating custom graphs by configuring _Passes_ which extend the graph as necessary for a specific analysis:

[3] https://github.com/ShiftLeftSecurity/codepropertygraph

[4] https://github.com/ShiftLeftSecurity/joern/
