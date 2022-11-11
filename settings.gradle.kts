rootProject.name = "cpg"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

plugins {
    id("com.gradle.enterprise") version("3.11.3")
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}

include(":cpg-all")
include(":cpg-core")
include(":cpg-analysis")
include(":cpg-neo4j")
include(":cpg-console")

// this code block also exists in the root build.gradle.kts
val enableGoFrontend by extra {
    val enableGoFrontend: String by settings
    enableGoFrontend.toBoolean()
}
val enablePythonFrontend by extra {
    val enablePythonFrontend: String by settings
    enablePythonFrontend.toBoolean()
}
val enableLLVMFrontend by extra {
    val enableLLVMFrontend: String by settings
    enableLLVMFrontend.toBoolean()
}
val enableTypeScriptFrontend by extra {
    val enableTypeScriptFrontend: String by settings
    enableTypeScriptFrontend.toBoolean()
}

if (enableGoFrontend) include(":cpg-language-go")
if (enableLLVMFrontend) include(":cpg-language-llvm")
if (enablePythonFrontend) include(":cpg-language-python")
if (enableTypeScriptFrontend) include(":cpg-language-typescript")
