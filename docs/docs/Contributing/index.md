---
title: "Contributing"
linkTitle: "Contributing"
no_list: true
weight: 1
date: 2017-01-05
description: >
    Contributing
---

## Prerequisites

* git
* Java 17 to Java 21 (OpenSDK)

## Build and Run

### Getting the source

First, create a fork of this repository and clone the fork:

```
git clone https://github.com/<<<your-github-account>>>/TODO.git
```

Add the upstream repository as a second remote, so you can incorporate upstream changes into your fork:

```
git remote add upstream https://github.com/Fraunhofer-AISEC/cpg.git
```

### Build

Make sure you can build the repository

```
./gradlew clean spotlessApply build publishToMavenLocal
```

This project requires Java 17 to Java 21. If a Java version between 17 and 21 is not your default Java version, make sure to configure gradle to use it by setting its java.home variable:

```
./gradlew -Dorg.gradle.java.home="/usr/lib/jvm/java-17-openjdk-amd64/" build
```

## Copyright Notice

This project has the convention of including a license notice header in all source files:
```java
/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
```

If you are using IntelliJ IDEA, you can import `style/copyright.xml` as a copyright profile to automate the header creation process.
Click [here](https://www.jetbrains.com/help/idea/copyright.html) for further information on copyright profiles.

## Code Guidelines

Most of our code is written in Kotlin and if you develop new nodes, one should follow the following guidelines.

### Property Edges

On some edges, we want to store additional information (e.g., if a `EOG` node is "unreachable"). In this case, a simple list of nodes for a `@Relationship` is not enough and instead a list of `PropertyEdge` objects is needed. To have a consistent naming, the property holding the edges should be named the singular of the property name + "Edges", e.g. `parameterEdges`. To make it more convenient for users to also access the connected nodes without property edges, the Kotlin delegation feature, with a `PropertyEdgeDelegate` can be used. This property should then be named after the property (plural), e.g. `parameters`.

```kotlin
/** The list of function parameters. */
@Relationship(value = "PARAMETERS", direction = Relationship.Direction.OUTGOING)
@field:SubGraph("AST")
var parameterEdges = mutableListOf<PropertyEdge<ParameterDeclaration>>()

/** Virtual property for accessing [parameterEdges] without property edges. */
var parameters by PropertyEdgeDelegate(FunctionDeclaration::parameterEdges)
```

Note: We actually want list property to be immutable so that they can only be modified by the node class itself. However, it is currently not possible to have them immutable on the public getter, but mutable for the class itself. There is a Kotlin issue tracking this feature request. Once https://youtrack.jetbrains.com/issue/KT-14663 is resolved, we should set the public type for all those lists to `List` instead of `MutableList`. Properties delegated by `PropertyEdgeDelegate` are already immutable.

### Required Properties

Properties which can be considered as a required part of an expression, should be non-nullable and be initialized to a `ProblemNode`. In this case we can represent parsing problems in the graph and still avoid too many null checks. For example in the `MemberExpression`:
```kotlin
var base: Expression = newProblemExpression("could not parse base expression")
```

There might be cases, where either one or the other property might be required, e.g., if a property can either be an `Expression` or a `Declaration`. In this case we need to resort of having both properties nullable.

Note: In the future, we might move required properties into the constructor of a node. 

### `equals` and `hashCode`

Because of the special nature of the `PropertyEdge`, one needs to be careful in comparing them in `equals`, to avoid stack overflows. Therefore, the special function `propertyEqualsList` needs to be used:
```kotlin
return super.equals(other) &&
    parameters == other.parameters &&
    propertyEqualsList(parameterEdges, other.parameterEdges)
```

`hashCode` needs to include all properties that are also compared in `equals`. For easier readability, we should use the Kotlin expression body feature:
```kotlin
override fun hashCode() = Objects.hash(super.hashCode(), constructor, arguments)
```

## Pull Requests

Before we can accept a pull request from you, you'll need to sign a Contributor License Agreement (CLA). It is an automated process, and you only need to do it once.

:warning:
We are currently discussing the implementation of a Contributor License Agreement (CLA). Unfortunately, we cannot merge external pull requests until this issue is resolved.
:warning:

To enable us to quickly review and accept your pull requests, always create one pull request per issue and link the issue in the pull request.
Never merge multiple requests in one unless they have the same root cause. Be sure your code is formatted correctly using the respective formatting task.
Keep code changes as small as possible. 
Pull requests should contain tests whenever possible.

### Change-Log
Every PR that changes the graph or interaction with one of the classes that run the analysis has to be documented in the changelog. For this, one should add the appropriated change type (added, changed, removed) under the heading of the thematic change (Graph-changes, Interface-changes). Fixes for specific issues should also be mentioned but their inclusion in the release changelog is optional. An example of a PR-changelog:

#### Graph-changes
##### Added
* New node `A` with edges of name `B` and `C` to its ast-children.
##### Changed
* Property of Node `A` that describes the name changed from `name` to `simple-name`.
#### Interface-changes
##### Added
* function `loadIncludes` which persists nodes to the graph coming from in-file includes.

## Language

Please stick to English for all discussions and comments. This helps to make the project accessible for a larger audience.

## Publishing

To publish a release, push a tag that contains the version number beginning with `v`, i.e. `v2.0.0`. The GitHub Actions workflow will then automatically build a release zip and create a GitHub release. Afterward it would be good to adjust the release text to include a minimal changelog.

### Versioning
The versioning number is split up in major, minor and bugfix releases: `major.minor.bugfix`. Most releases will have the form `major.minor.0`, and bugfixes will be either included in a future version, and the bugfix release number will only be used to ship bug fixes for older versions when necessary.

