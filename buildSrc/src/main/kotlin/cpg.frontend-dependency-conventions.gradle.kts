plugins {
    kotlin("jvm")
    `java-library`
    id("org.jetbrains.kotlinx.kover")
}

val enableJavaFrontend: Boolean by rootProject.extra
val enableCXXFrontend: Boolean by rootProject.extra
val enableGoFrontend: Boolean by rootProject.extra
val enablePythonFrontend: Boolean by rootProject.extra
val enableLLVMFrontend: Boolean by rootProject.extra
val enableTypeScriptFrontend: Boolean by rootProject.extra
val enableRubyFrontend: Boolean by rootProject.extra
val enableJVMFrontend: Boolean by rootProject.extra
val enableConfigfilesFrontend: Boolean by rootProject.extra

dependencies {
    if (enableJavaFrontend) {
        api(project(":cpg-language-java"))
        kover(project(":cpg-language-java"))
    }
    if (enableJVMFrontend) {
        api(project(":cpg-language-jvm"))
        kover(project(":cpg-language-jvm"))
    }
    if (enableCXXFrontend) {
        api(project(":cpg-language-cxx"))
        kover(project(":cpg-language-cxx"))
    }
    if (enableGoFrontend) {
        api(project(":cpg-language-go"))
        kover(project(":cpg-language-go"))
    }
    if (enablePythonFrontend) {
        api(project(":cpg-language-python"))
        kover(project(":cpg-language-python"))
    }
    if (enableLLVMFrontend) {
        api(project(":cpg-language-llvm"))
        kover(project(":cpg-language-llvm"))
    }
    if (enableTypeScriptFrontend) {
        api(project(":cpg-language-typescript"))
        kover(project(":cpg-language-typescript"))
    }
    if (enableRubyFrontend) {
        api(project(":cpg-language-ruby"))
        kover(project(":cpg-language-ruby"))
    }
    if (enableConfigfilesFrontend) {
        api(project(":cpg-language-configfiles"))
        kover(project(":cpg-language-configfiles"))
    }
}
