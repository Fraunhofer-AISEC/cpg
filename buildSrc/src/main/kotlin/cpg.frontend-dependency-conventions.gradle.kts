plugins {
    kotlin("jvm")
    `java-library`
}

val enableJavaFrontend: Boolean by rootProject.extra
val enableCXXFrontend: Boolean by rootProject.extra
val enableGoFrontend: Boolean by rootProject.extra
val enablePythonFrontend: Boolean by rootProject.extra
val enablePythonQiskitFrontend: Boolean by rootProject.extra
val enableLLVMFrontend: Boolean by rootProject.extra
val enableTypeScriptFrontend: Boolean by rootProject.extra
val enableOpenQasmFrontend: Boolean by rootProject.extra
val enableRubyFrontend: Boolean by rootProject.extra
val enableJVMFrontend: Boolean by rootProject.extra
val enableINIFrontend: Boolean by rootProject.extra

dependencies {
    if (enableJavaFrontend) {
        implementation(project(":cpg-language-java"))
    }
    if (enableJVMFrontend) {
        implementation(project(":cpg-language-jvm"))
    }
    if (enableCXXFrontend) {
        implementation(project(":cpg-language-cxx"))
    }
    if (enableGoFrontend) {
        implementation(project(":cpg-language-go"))
    }
    if (enablePythonFrontend) {
        implementation(project(":cpg-language-python"))
    }
    if (enablePythonQiskitFrontend) {
        implementation(project(":cpg-language-python-qiskit"))
    }
    if (enableLLVMFrontend) {
        implementation(project(":cpg-language-llvm"))
    }
    if (enableTypeScriptFrontend) {
        implementation(project(":cpg-language-typescript"))
    }
    if (enableOpenQasmFrontend) {
        implementation(project(":cpg-language-openqasm"))
    }
    if (enableRubyFrontend) {
        implementation(project(":cpg-language-ruby"))
    }
    if (enableINIFrontend) {
        implementation(project(":cpg-language-ini"))
    }
}
