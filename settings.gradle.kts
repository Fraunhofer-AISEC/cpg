plugins {
    id("com.gradle.enterprise") version("3.6.4")
}

include(":cpg-library")
include(":cpg-neo4j")
include(":cpg-llvm")
include(":cpg-console")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}