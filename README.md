# Code Property Graph 
[![Actions Status](https://github.com/Fraunhofer-AISEC/cpg/workflows/build/badge.svg)](https://github.com/Fraunhofer-AISEC/cpg/actions)
 [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Fraunhofer-AISEC_cpg&metric=alert_status)](https://sonarcloud.io/dashboard?id=Fraunhofer-AISEC_cpg) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Fraunhofer-AISEC_cpg&metric=security_rating)](https://sonarcloud.io/dashboard?id=Fraunhofer-AISEC_cpg) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Fraunhofer-AISEC_cpg&metric=coverage)](https://sonarcloud.io/dashboard?id=Fraunhofer-AISEC_cpg)

A simple library to extract a *code property graph* out of source code. It has support for multiple passes that can extend the analysis after the graph is constructed.

It currently supports C/C++ as well as Java.

## Usage

### As Library

The most recent version is being published to Maven central and can be used as a simple dependency, either using Maven or Gradle. For example, using Gradle's Kotlin syntax:
```
api("de.fraunhofer.aisec", "cpg", "1.1")
```

### On Command Line

The library can be used on the command line using `jshell`, the Java shell to try out some basic queries.

First, a jar consisting all the necessary dependencies should be created with `./gradlew shadowJar`. Afterwards, the shell can be launched using `jshell --class-path build/libs/cpg-*-all.jar`.

The following snippet creates a basic `TranslationManager` with default settings to analyze a sample file in `src/test/resources/openssl/client.cpp`:

```java
import de.fraunhofer.aisec.cpg.TranslationConfiguration;
import de.fraunhofer.aisec.cpg.TranslationManager;
import de.fraunhofer.aisec.cpg.graph.FunctionDeclaration;

var path = Paths.get("src/test/resources/openssl/client.cpp");
var config = TranslationConfiguration.builder().sourceFiles(path.toFile()).defaultPasses().debugParser(true).build();
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
* [aisec-fw](https://github.com/aisec-fw)
* [JulianSchuette](https://github.com/JulianSchuette)
* [konradweiss](https://github.com/konradweiss)
* [Masrepus](https://github.com/Masrepus)
* [obraunsdorf](https://github.com/obraunsdorf)
* [oxisto](https://github.com/oxisto)
* [titze](https://github.com/titze)
