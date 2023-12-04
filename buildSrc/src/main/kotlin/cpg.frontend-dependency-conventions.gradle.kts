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

dependencies {
    if (enableJavaFrontend) api(project(":cpg-language-java"))
    if (enableCXXFrontend) api(project(":cpg-language-cxx"))
    if (enableGoFrontend) api(project(":cpg-language-go"))
    if (enablePythonFrontend) api(project(":cpg-language-python"))
    if (enablePythonQiskitFrontend) api(project(":cpg-language-python-qiskit"))
    if (enableLLVMFrontend) api(project(":cpg-language-llvm"))
    if (enableTypeScriptFrontend) api(project(":cpg-language-typescript"))
    if (enableOpenQasmFrontend) api(project(":cpg-language-openqasm"))

    if (enableRubyFrontend) api(project(":cpg-language-ruby"))
}
