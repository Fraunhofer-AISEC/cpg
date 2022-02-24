plugins {
    `java-library`
    `maven-publish`
    signing
}

publishing {
    publications {
        named<MavenPublication>("cpg") {
            pom {
                artifactId = "cpg" // for legacy reasons (this will be renamed to cpg-core at some point)
                name.set("Code Property Graph")
                description.set("A simple library to extract a code property graph out of source code. It has support for multiple passes that can extend the analysis after the graph is constructed.")
                url.set("https://github.com/Fraunhofer-AISEC/cpg")
            }
        }
    }
}

dependencies {
    // this exposes all of our (published) modules as dependency
    api(project(":cpg-core"))
    api(project(":cpg-analysis"))
    api(project(":cpg-language-llvm"))
    api(project(":cpg-language-python"))
    api(project(":cpg-language-go"))
}