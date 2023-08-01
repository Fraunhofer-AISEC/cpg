---
title: "Usage as library"
linkTitle: "Usage as library"
no_list: true
weight: 1
date: 2017-01-05
description: >
    Usage as library
---

You can use the CPG library in your kotlin project.

## 1. Add the CPG library to your dependencies

First, get the required dependencies, e.g. by installing either the whole
project or selected submodules from mavencentral.
Here's an excerpt from a `build.gradle.kts` file:
```kotlin
...
repositories {
    mavenCentral()
    ...
}

dependencies {
    implementation("de.fraunhofer.aisec:cpg:6.2.1") // Install everything
    // OR
    implementation("de.fraunhofer.aisec:cpg-core:6.2.1") // Only cpg-core
    implementation("de.fraunhofer.aisec:cpg-language-java:6.2.1") // Only the java language frontend
    ...
}
```

## 2. Configuring the translation

Before constructing the CPG, you have to configure how you want to translate the
code to the CPG. You have to use the `TranslationConfiguration` and the
`InferenceConfiguration`. It allows you to specify which frontends, and passes
you want to use and can steer some analyses. 

The following lines give you a small example:
```kotlin
val inferenceConfig = InferenceConfiguration
    .builder()
    .guessCastExpressions(true)
    .inferRecords(true)
    .inferDfgForUnresolvedCalls(true)
    .build()

val translationConfig = TranslationConfiguration
    .builder()
    .inferenceConfiguration(inferenceConfig)
    .defaultPasses()
    .registerPass<MyCustomPass>()
    .registerFrontend<MyFrontend>()
    .sourceLocations(filePaths)
    .build()
```

For a complete list of available methods, please check the KDoc.

## 3. Running the analysis

Now it's time to get the CPG. All you have to do is to run the analysis with the
given configuration.
```kotlin
val translationResult = TranslationManager
    .builder()
    .config(translationConfig)
    .build()
    .analyze()
    .get()
```

The CPG is available in the `translationResult`. You can now run analyses or
explore the graph.

