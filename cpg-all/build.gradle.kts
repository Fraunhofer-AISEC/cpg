plugins {
    id("cpg.library-conventions")
}

publishing {
    publications {
        named<MavenPublication>("cpg-all") {
            pom {
                artifactId = "cpg" // for legacy reasons (this will be renamed to cpg-core at some point)
                name.set("Code Property Graph")
                description.set("A simple library to extract a code property graph out of source code. It has support for multiple passes that can extend the analysis after the graph is constructed.")
                url.set("https://github.com/Fraunhofer-AISEC/cpg")
            }
        }
    }
}

val enableGoFrontend: Boolean by rootProject.extra
val enablePythonFrontend: Boolean by rootProject.extra
val enableLLVMFrontend: Boolean by rootProject.extra
val enableTypeScriptFrontend: Boolean by rootProject.extra

dependencies {
    // this exposes all of our (published) modules as dependency
    api(projects.cpgCore)
    api(projects.cpgAnalysis)

    if (enableGoFrontend) api(project(":cpg-language-go"))
    if (enablePythonFrontend) api(project(":cpg-language-python"))
    if (enableLLVMFrontend) api(project(":cpg-language-llvm"))
    if (enableTypeScriptFrontend) api(project(":cpg-language-typescript"))
}