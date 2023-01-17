plugins {
    kotlin("jvm")
    `java-library`
}

val enableGoFrontend: Boolean by rootProject.extra
val enablePythonFrontend: Boolean by rootProject.extra
val enableLLVMFrontend: Boolean by rootProject.extra
val enableTypeScriptFrontend: Boolean by rootProject.extra

dependencies {
    if (enableGoFrontend) api(project(":cpg-language-go"))
    if (enablePythonFrontend) api(project(":cpg-language-python"))
    if (enableLLVMFrontend) api(project(":cpg-language-llvm"))
    if (enableTypeScriptFrontend) api(project(":cpg-language-typescript"))
}
