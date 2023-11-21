rootProject.name = "cpg"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":cpg-all")
include(":cpg-core")
include(":cpg-analysis")
include(":cpg-neo4j")
include(":cpg-console")

// this code block also exists in the root build.gradle.kts
val enableJavaFrontend: Boolean by extra {
    val enableJavaFrontend: String? by settings
    enableJavaFrontend.toBoolean()
}
val enableCXXFrontend: Boolean by extra {
    val enableCXXFrontend: String? by settings
    enableCXXFrontend.toBoolean()
}
val enableGoFrontend: Boolean by extra {
    val enableGoFrontend: String? by settings
    enableGoFrontend.toBoolean()
}
val enablePythonFrontend: Boolean by extra {
    val enablePythonFrontend: String? by settings
    enablePythonFrontend.toBoolean()
}
val enableLLVMFrontend: Boolean by extra {
    val enableLLVMFrontend: String? by settings
    enableLLVMFrontend.toBoolean()
}
val enableTypeScriptFrontend: Boolean by extra {
    val enableTypeScriptFrontend: String? by settings
    enableTypeScriptFrontend.toBoolean()
}
val enableRubyFrontend: Boolean by extra {
    val enableRubyFrontend: String? by settings
    enableRubyFrontend.toBoolean()
}

if (enableJavaFrontend) include(":cpg-language-java")
if (enableCXXFrontend) include(":cpg-language-cxx")
if (enableGoFrontend) include(":cpg-language-go")
if (enableLLVMFrontend) include(":cpg-language-llvm")
if (enablePythonFrontend) include(":cpg-language-python")
if (enableTypeScriptFrontend) include(":cpg-language-typescript")
if (enableRubyFrontend) include(":cpg-language-ruby")