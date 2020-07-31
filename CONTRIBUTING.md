# Prerequsites

* git
* Java 11 (OpenSDK)

# Build and Run

## Getting the source

First, create a fork of this repository and clone the fork:

```
git clone https://github.com/<<<your-github-account>>>/TODO.git
```

Add the upstream repository as a second remote, so you can incorporate upstream changes into your fork:

```
git remote add upstream https://github.com/FraunhoferAISEC/TODO.git
```

## Build

Make sure you can build the repository

```
./gradlew clean spotlessApply build publishToMavenLocal
```

This project requires Java 11. If Java 11 is not your default Java version, make sure to configure gradle to use it by setting its java.home variable:

```
./gradlew -Dorg.gradle.java.home="/usr/lib/jvm/java-11-openjdk-amd64/" build
```

# Pull Requests

Before we can accept a pull request from you, you'll need to sign a Contributor License Agreement (CLA). It is an automated process and you only need to do it once.

To enable us to quickly review and accept your pull requests, always create one pull request per issue and link the issue in the pull request.
Never merge multiple requests in one unless they have the same root cause. Be sure your code is formatted correctly using the respective formatting task.
Keep code changes as small as possible. 
Pull requests should contain tests whenever possible.

# Language

Please stick to English for all discussions and comments. This helps to make the project accessible for a larger audience.

# Publishing

To publish a release, push a tag that contains the version number beginning with `v`, i.e. `v2.0.0`. The GitHub Actions workflow will then automatically build a release zip and create a GitHub release. Afterwards it would be good to adjust the release text to include a minimal changelog.
