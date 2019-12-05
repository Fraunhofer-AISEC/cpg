# Code Property Graph

A simple library to extract a *code property graph* out of source code. It has support for multiple passes that can extend the analysis after the graph is constructed.

It currently supports C/C++ as well as Java.

## Usage

The most recent version is being published to Maven central and can be used as a simple dependency, either using Maven or Gradle. For example, using Gradle's Kotlin syntax:
```
api("de.fraunhofer.aisec", "cpg", "1.1")
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
* Christian Banse <christian.banse@aisec.fraunhofer.de>
* Oliver Braunsdorf <oliver.braunsdorf@aisec.fraunhofer.de>
* Samuel Hopstock <samuel.hopstock@aisec.fraunhofer.de>
* Julian Schütte <julian.schuette@aisec.fraunhofer.de>
* Dennis Titze <dennis.titze@aisec.fraunhofer.de>
* Konrad Weiss <konrad.weiss@aisec.fraunhofer.de>
* Florian Wendland <florian.wendland@aisec.fraunhofer.de>
