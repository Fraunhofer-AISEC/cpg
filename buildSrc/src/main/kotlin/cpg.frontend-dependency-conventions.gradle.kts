plugins {
    kotlin("jvm")
    `java-library`
}

val enableJavaFrontend: Boolean by rootProject.extra
val enableGoFrontend: Boolean by rootProject.extra
val enablePythonFrontend: Boolean by rootProject.extra
val enableLLVMFrontend: Boolean by rootProject.extra
val enableTypeScriptFrontend: Boolean by rootProject.extra

dependencies {
    if (enableJavaFrontend) api(project(":cpg-language-java"))
    if (enableGoFrontend) api(project(":cpg-language-go"))
    if (enablePythonFrontend) api(project(":cpg-language-python"))
    if (enableLLVMFrontend) api(project(":cpg-language-llvm"))
    if (enableTypeScriptFrontend) api(project(":cpg-language-typescript"))
}
