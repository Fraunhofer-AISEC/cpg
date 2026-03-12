plugins {
    kotlin("jvm")
    `java-library`
}

val enableJavaFrontend: Boolean by rootProject.extra
val enableCXXFrontend: Boolean by rootProject.extra
val enableGoFrontend: Boolean by rootProject.extra
val enablePythonFrontend: Boolean by rootProject.extra
val enableLLVMFrontend: Boolean by rootProject.extra
val enableTypeScriptFrontend: Boolean by rootProject.extra
val enableRubyFrontend: Boolean by rootProject.extra
val enableJVMFrontend: Boolean by rootProject.extra
val enableINIFrontend: Boolean by rootProject.extra

dependencies {
    if (enableJavaFrontend) {
        runtimeOnly(project(":cpg-language-java"))
    }
    if (enableJVMFrontend) {
        runtimeOnly(project(":cpg-language-jvm"))
    }
    if (enableCXXFrontend) {
        runtimeOnly(project(":cpg-language-cxx"))
    }
    if (enableGoFrontend) {
        runtimeOnly(project(":cpg-language-go"))
    }
    if (enablePythonFrontend) {
        runtimeOnly(project(":cpg-language-python"))
    }
    if (enableLLVMFrontend) {
        runtimeOnly(project(":cpg-language-llvm"))
    }
    if (enableTypeScriptFrontend) {
        runtimeOnly(project(":cpg-language-typescript"))
    }
    if (enableRubyFrontend) {
        runtimeOnly(project(":cpg-language-ruby"))
    }
    if (enableINIFrontend) {
        runtimeOnly(project(":cpg-language-ini"))
    }
}
