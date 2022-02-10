plugins {
    id("com.gradle.enterprise") version("3.6.4")
}

include(":cpg")
include(":cpg-core")
include(":cpg-analysis")
include(":cpg-neo4j")
include(":cpg-language-llvm")
include(":cpg-language-python")
include(":cpg-language-go")
include(":cpg-console")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}