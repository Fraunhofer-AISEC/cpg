---
title: "Installing the CPG library"
linkTitle: "Installing the CPG library"
no_list: true
weight: 1
date: 2017-01-05
description: >
    Installing the CPG as a library
---


You can install the library from pre-built releases or build it from the source
code.

## Get Pre-Built Releases

You can find the releases in our [github
repository](https://github.com/Fraunhofer-AISEC/cpg/releases) or on
[maven](https://mvnrepository.com/artifact/de.fraunhofer.aisec/cpg).

## Building from Source

1. Clone the repository from GitHub with `git clone git@github.com:Fraunhofer-AISEC/cpg.git`.
2. Generate a `gradle.properties` file locally. We provide a sample file
   [here](https://github.com/Fraunhofer-AISEC/cpg/blob/main/gradle.properties.example)
   or you can use the `configure_frontends.sh` scripts to generate the file.
3. Build the project using `./gradlew build` or install it with
   `./gradlew installDist`. You could also build selected submodules.

